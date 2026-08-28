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

    public enum TreeType {
        NORMAL_LOGS,
        OAK_LOGS,
        WILLOW_LOGS
    }

    private static final Map<TreeType, Integer> DROP_RATES = Map.of(
            TreeType.NORMAL_LOGS, 317647,
            TreeType.OAK_LOGS, 361146,
            TreeType.WILLOW_LOGS, 289286
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

    private final Map<Integer, Map<TreeType, Integer>> countsByLevel = new TreeMap<>();

    private static final Map<String, TreeType> ITEM_LOOKUP = Map.of(
            "logs", TreeType.NORMAL_LOGS,
            "oak logs", TreeType.OAK_LOGS,
            "willow logs", TreeType.WILLOW_LOGS
    );

    public void loadCounts() {
        countsByLevel.clear();

        String json = configManager.getRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY);
        log.debug("Raw stored value for {}: {}", CONFIG_KEY, json);

        if (json == null || json.isEmpty()) {
            return;
        }

        Type type = new TypeToken<Map<Integer, Map<TreeType, Integer>>>() {}.getType();

        try {
            Map<Integer, Map<TreeType, Integer>> saved = gson.fromJson(json, type);
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
        TreeType TreeType = ITEM_LOOKUP.get(item);

        if (TreeType == null) {
            return;
        }

        int level = client.getRealSkillLevel(Skill.WOODCUTTING);

        Map<TreeType, Integer> levelCounts = countsByLevel.computeIfAbsent(level, k -> new EnumMap<>(TreeType.class));
        levelCounts.merge(TreeType, 1, Integer::sum);

        saveCounts();
        notifyListeners();
    }

    public Map<TreeType, Integer> getTotalCounts() {
        Map<TreeType, Integer> totals = new EnumMap<>(TreeType.class);

        for (Map<TreeType, Integer> logCounts : countsByLevel.values()) {
            for (Map.Entry<TreeType, Integer> entry : logCounts.entrySet()) {
                totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return totals;
    }

    public int getTotalCount(TreeType type) {
        return getTotalCounts().getOrDefault(type, 0);
    }

    public double getProbability() {
        double probability = 0;

        for (Map.Entry<Integer, Map<TreeType, Integer>> levelEntry : countsByLevel.entrySet()) {
            int level = levelEntry.getKey();
            Map<TreeType, Integer> logCounts = levelEntry.getValue();

            for (Map.Entry<TreeType, Integer> logEntry : logCounts.entrySet()) {
                TreeType type = logEntry.getKey();
                int count = logEntry.getValue();
                int baseDropRate = DROP_RATES.get(type);

                double perLogChance = 1.0 / (baseDropRate - level * 25);
                double chanceAtLevel = 1 - Math.pow(1 - perLogChance, count);

                probability = 1 - (1 - probability) * (1 - chanceAtLevel);
            }
        }

        return probability;
    }

    public void reset() {
        countsByLevel.clear();
        saveCounts();
        notifyListeners();
    }
}
