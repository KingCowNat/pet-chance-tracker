package com.petchancetracker;

import com.petchancetracker.utils.ItemConsumptionTracker;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemConsumptionTrackerTest {

    @Mock
    private Client client;

    @Mock
    private ItemContainer inventory;

    private ItemConsumptionTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new ItemConsumptionTracker(client, 100); // arbitrary test item ID
    }

    private void mockInventory(int quantity) {
        Item item = new Item(100, quantity);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
        when(inventory.getItems()).thenReturn(new Item[]{item});
    }

    @Test
    void firstCallReturnsMinusOneRegardlessOfCount() {
        mockInventory(11);
        assertEquals(-1, tracker.getChangeSinceLastCheck());
    }

    @Test
    void secondCallComputesRealDiff() {
        mockInventory(11);
        tracker.getChangeSinceLastCheck(); // establishes baseline

        mockInventory(6);
        assertEquals(5, tracker.getChangeSinceLastCheck());
    }

    @Test
    void repeatedCallsEachDiffAgainstTheLastOne() {
        mockInventory(10);
        tracker.getChangeSinceLastCheck();

        mockInventory(5);
        assertEquals(5, tracker.getChangeSinceLastCheck());

        mockInventory(0);
        assertEquals(5, tracker.getChangeSinceLastCheck()); // exactly the "ran out on the last batch" case that broke earlier
    }

    @Test
    void noChangeReturnsZero() {
        mockInventory(10);
        tracker.getChangeSinceLastCheck();

        mockInventory(10);
        assertEquals(0, tracker.getChangeSinceLastCheck());
    }
}