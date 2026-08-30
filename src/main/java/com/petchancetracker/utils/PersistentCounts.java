package com.petchancetracker.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class PersistentCounts<T extends Enum<T>> {
    private final ConfigManager configManager;
    private final Gson gson;
    private final String configGroup;
    private final String configKey;
    private final Class<T> enumClass;

    // Count of eligible rolls for a given skilling pet per source per level
    @Getter
    private final Map<Integer, Map<T, Integer>> countsByLevel = new TreeMap<>();

    /**
     * Initialise final variables to keep them constant throughout the lifecycle of reading and writing counts
     * @param configManager {@link ConfigManager}
     * @param gson {@link Gson}
     * @param configGroup The name of the plugin in slug form
     * @param configKey The key to tag all the relevant information with in order to persist between sessions
     * @param enumClass The class for the source types enum
     */
    public PersistentCounts(ConfigManager configManager, Gson gson, String configGroup, String configKey, Class<T> enumClass) {
        this.configManager = configManager;
        this.gson = gson;
        this.configGroup = configGroup;
        this.configKey = configKey;
        this.enumClass = enumClass;
    }

    /**
     * Loads a count of all eligible rolls for a specific skilling pet per source type per level.
     */
    public void load() {
        countsByLevel.clear();
        String json = configManager.getRSProfileConfiguration(configGroup, configKey);
        log.debug("load: key={}, values={}", configKey, json);

        if (json == null || json.isEmpty()) {
            return;
        }

        Type type = TypeToken.getParameterized(Map.class, Integer.class,
                TypeToken.getParameterized(Map.class, enumClass, Integer.class).getType()).getType();

        try {
            Map<Integer, Map<T, Integer>> saved = gson.fromJson(json, type);
            if (saved != null) {
                // Guard against null keys sneaking into the persistent counts
                for (Map.Entry<Integer, Map<T, Integer>> levelEntry : saved.entrySet()) {
                    Map<T, Integer> levelCounts = levelEntry.getValue();
                    levelCounts.entrySet().removeIf(e -> e.getKey() == null);
                }
                countsByLevel.putAll(saved);
            }
        } catch (Exception e) {
            log.warn("Failed to load counts for key {}, resetting", configKey, e);
        }
    }

    /**
     * Saves a count of all eligible rolls for a specific skilling pet per source per level.
     */
    public void save() {
        String json = gson.toJson(countsByLevel);
        configManager.setRSProfileConfiguration(configGroup, configKey, json);
        log.debug("save: key={}, values={}", configKey, json);
    }

    /**
     * Increments and saves the count of eligible rolls for a specific skilling pet from a specific source
     * at the player's current level in that skill.
     * @param level The player's current level in a specific skill.
     * @param type A specific source for the skilling pet of a specific skill.
     */
    public void increment(int level, T type) {
        countsByLevel.computeIfAbsent(level, k -> new EnumMap<>(enumClass))
                .merge(type, 1, Integer::sum);
        save();
    }

    /**
     * Gets a count of all eligible rolls for a specific skilling pet per source.
     * @return A map of counts for each type of pet source.
     */
    public Map<T, Integer> getTotalCounts() {
        Map<T, Integer> totals = new EnumMap<>(enumClass);

        for (Map<T, Integer> levelCounts : countsByLevel.values()) {
            for (Map.Entry<T, Integer> entry : levelCounts.entrySet()) {
                totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return totals;
    }

    /**
     * Gets a total count of eligible rolls for a specific skilling pet from a specific source
     * @param type A specific source for the skilling pet of a specific skill.
     * @return A total count of eligible rolls for the skilling pet from a specific source.
     */
    public int getTotalCount(T type) {
        int total = 0;

        for (Map<T, Integer> levelCounts : countsByLevel.values()) {
            total += levelCounts.getOrDefault(type, 0);
        }

        return total;
    }

    /**
     * Resets the count of all eligible rolls for a specific skilling pet.
     * <p>
     * WARNING: This is a permanent and irreversible process. Only use this if you absolutely mean to.
     * </p>
     */
    public void reset() {
        countsByLevel.clear();
        save();
    }


}
