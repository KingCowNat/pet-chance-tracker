package com.petchancetracker.utils;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.petchancetracker.utils.PetRollEligibility.isEligible;

public class ChatMessageTracker<T extends Enum<T>> implements PetRollSourceTracker<T> {
    private final Client client;
    private final Pattern sourceItemPattern;
    private final Map<String, T> itemLookup;
    private final Set<Integer> ineligibleRegions;
    private final WorldArea[] ineligibleAreas;

    private T currentSource;

    public ChatMessageTracker(Client client, Pattern sourceItemPattern, Map<String, T> itemLookup,
                              Set<Integer> ineligibleRegions, WorldArea[] ineligibleAreas) {
        this.client = client;
        this.sourceItemPattern = sourceItemPattern;
        this.itemLookup = itemLookup;
        this.ineligibleRegions = ineligibleRegions;
        this.ineligibleAreas = ineligibleAreas;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM
                && event.getType() != ChatMessageType.GAMEMESSAGE
                && event.getType() != ChatMessageType.MESBOX
        ) {
            return;
        }

        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (!isEligible(playerLocation, ineligibleRegions, ineligibleAreas)) {
            return;
        }

        Matcher matcher = sourceItemPattern.matcher(event.getMessage());
        if (!matcher.find()) {
            return;
        }

        String item = matcher.group(1).trim().toLowerCase();
        currentSource = itemLookup.get(item);
    }

    @Override
    public T getCurrentSource() {
        return currentSource;
    }
}
