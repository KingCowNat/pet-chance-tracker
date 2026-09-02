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

    /**
     * Gets the current count of a specific ItemID in the player's inventory
     * @return Count of a specific ItemID
     */
    public int getCurrentCount() {
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory == null) {
            return 0;
        }

        int count = 0;
        for (Item item : inventory.getItems()) {
            if (item.getId() == itemId) {
                count+= item.getQuantity();
            }
        }

        log.debug("itemCount: item={} count={}", itemId, count);
        return count;
    }

    /**
     * Sync the baseline count to the current count without reporting a change. Useful for whenever the inventory
     * changes for a reason unrelated to the skill being tracked (eg. banking or dropping items) so that a stale
     * count isn't used when a legitimate change related to the skill being tracked occurs.
     */
    public void syncBaseline() {
        previousCount = getCurrentCount();
    }

    /**
     * Gets the change in count of a specific ItemID in the player's inventory since the last count was computed
     * @return Change in count of a specific ItemID
     */
    public int getChangeSinceLastCheck() {
        int currentCount = getCurrentCount();
        int change = previousCount < 0 ? Integer.MIN_VALUE : currentCount - previousCount;
        previousCount = currentCount;
        return change;
    }
}
