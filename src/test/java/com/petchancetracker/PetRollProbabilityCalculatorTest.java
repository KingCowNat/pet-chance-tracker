package com.petchancetracker;

import com.petchancetracker.utils.PetRollProbabilityCalculator;
import org.junit.jupiter.api.Test;


import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class PetRollProbabilityCalculatorTest {
    enum TestType { A, B }

    @Test
    void singleRollAtBaseRate() {
        Map<Integer, Map<TestType, Integer>> counts = Map.of(
                1, Map.of(TestType.A, 1)
        );
        Map<TestType, Integer> rates = Map.of(TestType.A, 1000);

        double result = PetRollProbabilityCalculator.calculateProbability(counts, rates);

        assertEquals(1.0 / (1000 - 25), result, 0.0000001);
    }

    @Test
    void zeroRollsGivesZeroProbability() {
        Map<Integer, Map<TestType, Integer>> counts = Map.of();
        Map<TestType, Integer> rates = Map.of(TestType.A, 1000);

        double result = PetRollProbabilityCalculator.calculateProbability(counts, rates);

        assertEquals(0.0, result, 0.0000001);
    }

    @Test
    void multipleSourcesCombineCorrectly() {
        Map<Integer, Map<TestType, Integer>> counts = Map.of(
                1, Map.of(TestType.A, 1, TestType.B, 1)
        );
        Map<TestType, Integer> rates = Map.of(
                TestType.A, 1000,
                TestType.B, 2000
        );

        double result = PetRollProbabilityCalculator.calculateProbability(counts, rates);

        double chanceA = 1.0 / (1000 - 25);
        double chanceB = 1.0 / (2000 - 25);
        double expected = 1 - (1 - chanceA) * (1 - chanceB);

        assertEquals(expected, result, 0.0000001);

        // Regression guard: naive addition was the original bug this formula fixed —
        // two sources should never simply sum, since that can even exceed 100%.
        double naiveSum = chanceA + chanceB;
        assertNotEquals(naiveSum, result, 0.0000001);
    }
}
