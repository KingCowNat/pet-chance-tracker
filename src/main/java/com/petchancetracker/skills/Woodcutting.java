package com.petchancetracker.skills;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Woodcutting {
    public interface CountsChangedListener {
        void onCountsChanged();
    }

    private static final String CONFIG_GROUP = "petchancetracker";
    private static final String CONFIG_KEY = "woodcuttingCountsByLevelV3";

    private static final Pattern LOG_CUT_PATTERN = Pattern.compile("You get (?:some|an) ([\\w ]+?)\\.");

    public enum LogType {
        NORMAL_LOGS,
        OAK_LOGS,
        WILLOW_LOGS
    }

    private static final Map<LogType, Integer> DROP_RATES = Map.of(
            LogType.NORMAL_LOGS, 317647,
            LogType.OAK_LOGS, 361146,
            LogType.WILLOW_LOGS, 289286
    );

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Gson gson;

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event) {
        log.debug("onRuneScapeProfileChanged fired, profile={}", configManager.getRSProfileKey());
        loadCounts();
        notifyListeners();
    }

    private final List<CountsChangedListener> listeners = new ArrayList<>();

    public void addCountsChangedListener(CountsChangedListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (CountsChangedListener listener : listeners) {
            listener.onCountsChanged();
        }
    }

    private final Map<Integer, Map<LogType, Integer>> countsByLevel = new TreeMap<>();

    private static final Map<String, LogType> ITEM_LOOKUP = Map.of(
            "logs", LogType.NORMAL_LOGS,
            "oak logs", LogType.OAK_LOGS,
            "willow logs", LogType.WILLOW_LOGS
    );

    public void loadCounts() {
        countsByLevel.clear();

        String json = configManager.getRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY);
        log.debug("Raw stored value for {}: {}", CONFIG_KEY, json);

        if (json == null || json.isEmpty()) {
            return;
        }

        Type type = new TypeToken<Map<Integer, Map<LogType, Integer>>>() {}.getType();

        try {
            Map<Integer, Map<LogType, Integer>> saved = gson.fromJson(json, type);
            log.debug("loadCounts: profile={}, saved={}", configManager.getRSProfileKey(), saved);
            if (saved != null) {
                countsByLevel.putAll(saved);
            }
        } catch (ClassCastException e) {
            log.warn("Failed to load woodcutting counts, resetting", e);
        }
    }

    public void saveCounts() {
        String json = gson.toJson(countsByLevel);
        configManager.setRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY, json);
        log.debug("saveCounts: profile={}, json={}", configManager.getRSProfileKey(), json);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM
                && event.getType() != ChatMessageType.GAMEMESSAGE
                && event.getType() != ChatMessageType.MESBOX) {
            return;
        }

        Matcher matcher = LOG_CUT_PATTERN.matcher(event.getMessage());
        if (!matcher.find()) {
            return;
        }

        String item = matcher.group(1).trim().toLowerCase();
        LogType logType = ITEM_LOOKUP.get(item);

        if (logType == null) {
            return;
        }

        int level = client.getRealSkillLevel(Skill.WOODCUTTING);

        Map<LogType, Integer> levelCounts = countsByLevel.computeIfAbsent(level, k -> new EnumMap<>(LogType.class));
        levelCounts.merge(logType, 1, Integer::sum);

        saveCounts();
        notifyListeners();
    }

    public Map<LogType, Integer> getTotalCounts() {
        Map<LogType, Integer> totals = new EnumMap<>(LogType.class);

        for (Map<LogType, Integer> logCounts : countsByLevel.values()) {
            for (Map.Entry<LogType, Integer> entry : logCounts.entrySet()) {
                totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return totals;
    }

    public int getTotalCount(LogType type) {
        return getTotalCounts().getOrDefault(type, 0);
    }

    public double getProbability() {
        double probablity = 0;

        for (Map.Entry<Integer, Map<LogType, Integer>> levelEntry : countsByLevel.entrySet()) {
            int level = levelEntry.getKey();
            Map<LogType, Integer> logCounts = levelEntry.getValue();

            for (Map.Entry<LogType, Integer> logEntry : logCounts.entrySet()) {
                LogType type = logEntry.getKey();
                int count = logEntry.getValue();
                int baseDropRate = DROP_RATES.get(type);

                double perLogChance = 1.0 / (baseDropRate - level * 25);
                double chanceAtLevel = 1 - Math.pow(1 - perLogChance, count);

                probablity = 1 - (1 - probablity) * (1 - chanceAtLevel);
            }
        }

        return probablity;
    }

    public void reset() {
        countsByLevel.clear();
        saveCounts();
        notifyListeners();
    }
}
