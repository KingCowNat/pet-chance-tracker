package com.petchancetracker.utils;

import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.petchancetracker.utils.PetRollEligibility.isEligible;

public class TargetInteractionTracker<T extends Enum<T>> implements PetRollSourceTracker<T> {
    private final Client client;
    private final Pattern targetItemPattern;
    private final Map<String, T> itemLookup;
    private final Set<Integer> ineligibleRegions;
    private final WorldArea[] ineligibleAreas;
    private final Set<String> interactOptions;

    private T currentTarget;    // current target of the action, set by onMenuOptionClicked

    public TargetInteractionTracker(Client client, Pattern targetItemPattern, Map<String, T> itemLookup,
                                    Set<Integer> ineligibleRegions, WorldArea[] ineligibleAreas,
                                    Set<String> interactOptions) {
        this.client = client;
        this.targetItemPattern = targetItemPattern;
        this.itemLookup = itemLookup;
        this.ineligibleRegions = ineligibleRegions;
        this.ineligibleAreas = ineligibleAreas;
        this.interactOptions = interactOptions;
    }

    /**
     * Uses interactions with an object to determine the current pet source.
     * @param event Any left click interaction.
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (!isEligible(playerLocation, ineligibleRegions, ineligibleAreas)) {
            return;
        }

        if (!interactOptions.contains(event.getMenuOption())) {
            return;
        }

        Matcher matcher = targetItemPattern.matcher(event.getMenuTarget());
        if (!matcher.find()) {
            return;
        }

        String target = matcher.group(1).trim().toLowerCase();
        currentTarget = itemLookup.get(target);
    }

    @Override
    public T getCurrentSource() {
        return currentTarget;
    }
}
