package com.petchancetracker.utils;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.Set;

public class PetRollEligibility {
    /**
     * Checks to see if the player is located in an eligible place to receive a pet roll.
     * @return boolean
     */
    public static boolean isEligible(WorldPoint playerLocation, Set<Integer> ineligibleRegions, WorldArea[] ineligibleAreas) {
        int regionId = playerLocation.getRegionID();

        if (ineligibleRegions.contains(regionId)) {
            return false;
        }

        for (WorldArea area : ineligibleAreas) {
            if (playerLocation.isInArea(area)) {
                return false;
            }
        }

        return true;
    }
}
