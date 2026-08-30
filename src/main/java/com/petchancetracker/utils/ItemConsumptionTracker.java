package com.petchancetracker.utils;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

@Slf4j
public class ItemConsumptionTracker {
    private final Client client;
    private final int itemId;

    private int previousCount = -1;    // -1 until initialised

    public ItemConsumptionTracker(Client client, int itemId) {
        this.client = client;
        this.itemId = itemId;
    }

    public int getCurrentCount() {
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory == null) {
            return 0;
        }

        int count = 0;
        for (Item item : inventory.getItems()) {
            if (item.getId() == itemId) {
                count+= item.getQuantity();
                log.debug("itemCount: count={}", count);
            }
        }

        return count;
    }


    public int getConsumedSinceLastCheck() {
        int currentCount = getCurrentCount();
        int consumed = previousCount < 0 ? -1 : previousCount - currentCount;
        previousCount = currentCount;
        return consumed;
    }
}
