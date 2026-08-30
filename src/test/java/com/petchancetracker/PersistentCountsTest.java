package com.petchancetracker;

import com.google.gson.Gson;
import com.petchancetracker.utils.PersistentCounts;
import net.runelite.client.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class PersistentCountsTest {

    enum TestType { X, Y }

    @Mock
    private ConfigManager configManager;

    private final Gson gson = new Gson();
    private PersistentCounts<TestType> counts;

    @BeforeEach
    void setUp() {
        counts = new PersistentCounts<>(configManager, gson, "group", "key", TestType.class);
    }

    @Test
    void incrementThenTotalReflectsTheIncrement() {
        counts.increment(75, TestType.X);
        counts.increment(75, TestType.X);
        counts.increment(80, TestType.X);

        assertEquals(3, counts.getTotalCount(TestType.X));
        assertEquals(0, counts.getTotalCount(TestType.Y));
    }

    @Test
    void loadWithMalformedJsonDoesNotThrow() {
        when(configManager.getRSProfileConfiguration("group", "key")).thenReturn("not valid json{{{");

        assertDoesNotThrow(() -> counts.load());
        assertEquals(0, counts.getTotalCount(TestType.X));
    }

    @Test
    void loadWithNullEntryFromRemovedEnumConstantIsFilteredOut() {
        // Simulates old JSON referencing an enum constant that no longer exists
        when(configManager.getRSProfileConfiguration("group", "key"))
                .thenReturn("{\"75\":{\"X\":2,\"REMOVED_TYPE\":5}}");

        counts.load();

        assertEquals(2, counts.getTotalCount(TestType.X)); // the null-keyed entry shouldn't corrupt the rest
    }
}
