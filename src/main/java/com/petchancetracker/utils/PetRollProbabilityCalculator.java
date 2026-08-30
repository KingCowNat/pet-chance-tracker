package com.petchancetracker.utils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class PetRollProbabilityCalculator {
    /**
     * Calculates the probability of successfully rolling a specific skilling pet at least once,
     * with no excluded drop sources.
     * @see #calculateProbability(Map, Map, Set)
     */
    public static <T extends Enum<T>> double calculateProbability(
            Map<Integer, Map<T, Integer>> countsByLevel,
            Map<T, Integer> dropRates
    ) {
        return calculateProbability(countsByLevel, dropRates, Collections.emptySet());
    }

    /**
     * Calculates the probability of successfully rolling a specific skilling pet at least once.
     * @return Probability of successfully rolling a specific skilling pet at least once.
     */
    public static <T extends Enum<T>> double calculateProbability(
            Map<Integer, Map<T, Integer>> countsByLevel,
            Map<T, Integer> dropRates,
            Set<T> excludedRates
    ) {
        // Guard against null excludedRates
        Set<T> safeExcluded = (excludedRates != null) ? excludedRates : Collections.emptySet();
        double probability = 0;

        for (Map.Entry<Integer, Map<T, Integer>> levelEntry : countsByLevel.entrySet()) {
            int level = levelEntry.getKey();
            Map<T, Integer> typeCounts = levelEntry.getValue();
            for (Map.Entry<T, Integer> typeEntry : typeCounts.entrySet()) {
                T sourceType = typeEntry.getKey();

                // Skip if the current sourceType is contained within the excluded rates
                if (safeExcluded.contains(sourceType)) { continue; }

                int count = typeEntry.getValue();
                int baseDropRate = dropRates.get(sourceType);

                double perRollChance = 1.0 / (baseDropRate - level * 25);
                double chanceAtLevel = 1 - Math.pow(1 - perRollChance, count);

                probability = 1 - (1 - probability) * (1 - chanceAtLevel);
            }
        }

        return probability;
    }
}
