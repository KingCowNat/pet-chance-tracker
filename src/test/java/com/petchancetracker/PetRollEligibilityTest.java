package com.petchancetracker;

import com.petchancetracker.utils.PetRollEligibility;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;


import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class PetRollEligibilityTest {

    @Test
    void ineligibleRegionBlocksRoll() {
        WorldPoint point = new WorldPoint(3100, 3100, 0);
        Set<Integer> ineligibleRegions = Set.of(point.getRegionID());

        assertFalse(PetRollEligibility.isEligible(point, ineligibleRegions, new WorldArea[0]));
    }

    @Test
    void pointInsideIneligibleAreaBlocksRoll() {
        WorldArea area = new WorldArea(100, 100, 10, 10, 0);
        WorldPoint insidePoint = new WorldPoint(105, 105, 0);

        assertFalse(PetRollEligibility.isEligible(insidePoint, Set.of(), new WorldArea[]{area}));
    }

    @Test
    void pointOutsideEverythingIsEligible() {
        WorldPoint point = new WorldPoint(9999, 9999, 0);
        assertTrue(PetRollEligibility.isEligible(point, Set.of(), new WorldArea[0]));
    }
}