package com.petchancetracker.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class PersistentIntegerCounts {
    private final ConfigManager configManager;
    private final Gson gson;
    private final String configGroup;
    private final String configKey;

    // Count of eligible rolls for a given skilling pet per bucket
    @Getter
    private final Map<Integer, Integer> countsByBucket = new TreeMap<>();


    /**
     * Initialise final variables to keep them constant throughout the lifecycle of reading and writing counts
     * @param configManager {@link ConfigManager}
     * @param gson {@link Gson}
     * @param configGroup The name of the plugin in slug form
     * @param configKey The key to tag all the relevant information with in order to persist between sessions
     */
    public PersistentIntegerCounts(ConfigManager configManager, Gson gson, String configGroup, String configKey) {
        this.configManager = configManager;
        this.gson = gson;
        this.configGroup = configGroup;
        this.configKey = configKey;
    }

    /**
     * Loads a count of all eligible rolls for a specific skilling pet per bucket.
     */
    public void load() {
        countsByBucket.clear();
        String json = configManager.getRSProfileConfiguration(configGroup, configKey);
        log.debug("load: key={}, values={}", configKey, json);

        if (json == null || json.isEmpty()) {
            return;
        }

        Type type = TypeToken.getParameterized(Map.class, Integer.class, Integer.class).getType();

        try {
            Map<Integer, Integer> saved = gson.fromJson(json, type);
            if (saved != null) {
                saved.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
                countsByBucket.putAll(saved);
            }
        } catch (Exception e) {
            log.warn("Failed to load counts for key {}, resetting", configKey, e);
        }
    }

    /**
     * Saves a count of all eligible rolls for a specific skilling pet per bucket.
     */
    public void save() {
        String json = gson.toJson(countsByBucket);
        configManager.setRSProfileConfiguration(configGroup, configKey, json);
        log.debug("save: key={}, values={}", configKey, json);
    }

    /**
     * Increments and saves the count of eligible rolls for a specific skilling pet in a specific bucket.
     * @param bucket The bucket to count on.
     */
    public void increment(int bucket) {
        countsByBucket.merge(bucket, 1, Integer::sum);
        save();
    }

    /**
     * Gets a count of all eligible rolls for a specific skilling pet from all buckets.
     * @return A count of all eligible rolls for a specific skilling pet from all buckets.
     */
    public int getTotalCount() {
        return countsByBucket.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Resets the count of all eligible rolls for a specific skilling pet from all buckets.
     * <p>
     * WARNING: This is a permanent and irreversible process. Only use this if you absolutely mean to.
     * </p>
     */
    public void reset() {
        countsByBucket.clear();
        save();
    }

}
