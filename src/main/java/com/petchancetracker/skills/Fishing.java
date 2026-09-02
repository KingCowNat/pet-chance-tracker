package com.petchancetracker.skills;

import com.google.gson.Gson;
import com.petchancetracker.utils.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;

@Singleton
@Slf4j
public class Fishing implements PetRollTrackable {
    private static final String CONFIG_GROUP = "petchancetracker";
    private static final String CONFIG_KEY = "fishingCountsByLevel";

    // Regex to find the fish caught from the ChatMessage event
    private static final Pattern FISH_CAUGHT_PATTERN =
            Pattern.compile("^You catch (?:an?|some|two|three|four|five|six|seven|\\d*) ([a-zA-Z ]+)[.!]");
    // Fishing Trawler regex
    private static final Pattern TRAWLER_CATCH_PATTERN =
            Pattern.compile("^You caught (\\d+) fish(?:, but the .*? diary boosted the catch to (\\d+))?.*$");

    // All Heron sources
    public enum FishType {
        AERIAL_FISH,
        ANCHOVY,
        ANGLERFISH,
        BASS,
        BLUEFIN,
        BREAM,
        CATFISH,
        CAVE_EEL,
        CAVEFISH,
        COD,
        DARK_CRAB,
        FISHING_TRAWLER,
        GIANT_KRILL,
        GUPPY,
        HADDOCK,
        HALIBUT,
        HERRING,
        INFERNAL_EEL,
        JUMBO_SQUID,
        KARAMBWAN,
        KARAMBWANJI,
        LEAPING_SALMON,
        LEAPING_STURGEON,
        LEAPING_TROUT,
        LEECHFIN,
        LOBSTER,
        MACKEREL,
        MARLIN,
        MINNOWS,
        MONKFISH,
        PIKE,
        RAINBOW_FISH,
        SACRED_EEL,
        SALMON,
        SARDINE,
        SHARK,
        SHARK_WITH_1_LURE,
        SHARK_WITH_3_LURES,
        SHARK_WITH_5_LURES,
        SHRIMP,
        SWORDFISH,
        SWORDTIP_SQUID,
        TETRA,
        TROUT,
        TUNA,
        YELLOWFIN
    }

    // All Heron base drop rates
    private static final Map<FishType, Integer> DROP_RATES = Map.ofEntries(
            entry(FishType.AERIAL_FISH, 636833),
            entry(FishType.ANCHOVY, 870330),
            entry(FishType.ANGLERFISH, 78649),
            entry(FishType.BASS, 1147827),              // Big net rate
            entry(FishType.BLUEFIN, 217770),
            entry(FishType.BREAM, 1147827),             // Big net rate
            entry(FishType.CATFISH, 152120),
            entry(FishType.CAVE_EEL, 257770),
            entry(FishType.CAVEFISH, 300792),
            entry(FishType.COD, 1147827),               // Big net rate
            entry(FishType.DARK_CRAB, 149434),
            entry(FishType.GIANT_KRILL, 257770),
            entry(FishType.GUPPY, 820330),
            entry(FishType.HADDOCK, 247770),
            entry(FishType.HALIBUT, 227770),
            entry(FishType.HERRING, 1056000),
            entry(FishType.INFERNAL_EEL, 165000),
            entry(FishType.JUMBO_SQUID, 257770),
            entry(FishType.KARAMBWAN, 170874),
            entry(FishType.KARAMBWANJI, 443697),
            entry(FishType.LEAPING_SALMON, 1280862),
            entry(FishType.LEAPING_STURGEON, 1280862),
            entry(FishType.LEAPING_TROUT, 1280862),
            entry(FishType.LEECHFIN, 1847827),
            entry(FishType.LOBSTER, 116129),
            entry(FishType.MACKEREL, 1147827),          // Big net rate
            entry(FishType.MARLIN, 207770),
            entry(FishType.MINNOWS, 977778),            // Static rate
            entry(FishType.MONKFISH, 138583),
            entry(FishType.PIKE, 305792),
            entry(FishType.RAINBOW_FISH, 137739),
            entry(FishType.SACRED_EEL, 99000),
            entry(FishType.SALMON, 923616),
            entry(FishType.SARDINE, 1056000),
            entry(FishType.SHARK, 82243),
            entry(FishType.SHARK_WITH_1_LURE, 328972),
            entry(FishType.SHARK_WITH_3_LURES, 411215),
            entry(FishType.SHARK_WITH_5_LURES, 493458),
            entry(FishType.SHRIMP, 870330),
            entry(FishType.SWORDFISH, 257770),
            entry(FishType.SWORDTIP_SQUID, 257770),
            entry(FishType.TETRA, 257770),
            entry(FishType.TROUT, 923616),
            entry(FishType.TUNA, 257770),
            entry(FishType.YELLOWFIN, 237770)
    );

    // Lookup map to match ChatMessage to source enum
    private static final Map<String, FishType> FISH_LOOKUP = Map.ofEntries(
            entry("raw anchovies", FishType.ANCHOVY),
            entry("anglerfish", FishType.ANGLERFISH),
            entry("raw bass", FishType.BASS),
            entry("bluefin", FishType.BLUEFIN),                      // Sailing
            entry("bream", FishType.BREAM),                        // Moons of Peril
            entry("raw catfish", FishType.CATFISH),
            entry("raw cave eel", FishType.CAVE_EEL),
            entry("raw cavefish", FishType.CAVEFISH),
            entry("raw cod", FishType.COD),
            entry("dark crab", FishType.DARK_CRAB),
            entry("giant krill", FishType.GIANT_KRILL),              // Sailing
            entry("raw guppy", FishType.GUPPY),
            entry("haddock", FishType.HADDOCK),                  // Sailing
            entry("halibut", FishType.HALIBUT),                  // Sailing
            entry("raw herring", FishType.HERRING),
            entry("infernal eel", FishType.INFERNAL_EEL),
            entry("raw jumbo squid", FishType.JUMBO_SQUID),              // Sailing
            entry("karambwan", FishType.KARAMBWAN),
            entry("karambwanji", FishType.KARAMBWANJI),
            entry("leaping salmon", FishType.LEAPING_SALMON),
            entry("leaping sturgeon", FishType.LEAPING_STURGEON),
            entry("leaping trout", FishType.LEAPING_TROUT),
            entry("leechfin", FishType.LEECHFIN),                     // Vampyrium
            entry("lobster", FishType.LOBSTER),
            entry("raw mackerel", FishType.MACKEREL),
            entry("marlin", FishType.MARLIN),                   // Sailing
            entry("minnows", FishType.MINNOWS),
            entry("monkfish", FishType.MONKFISH),
            entry("pike", FishType.PIKE),
            entry("rainbow fish", FishType.RAINBOW_FISH),
            entry("sacred eel", FishType.SACRED_EEL),
            entry("raw salmon", FishType.SALMON),
            entry("raw sardine", FishType.SARDINE),
            entry("shark", FishType.SHARK),
            entry("raw shrimps", FishType.SHRIMP),
            entry("raw swordfish", FishType.SWORDFISH),
            entry("raw swordtip squid", FishType.SWORDTIP_SQUID),           // Sailing
            entry("raw tetra", FishType.TETRA),
            entry("raw trout", FishType.TROUT),
            entry("raw tuna", FishType.TUNA),
            entry("yellowfin", FishType.YELLOWFIN)                // Sailing
    );

    private static final Map<Integer, FishType> SHARK_LURE_LOOKUP = Map.of(
            0, FishType.SHARK,
            1, FishType.SHARK_WITH_1_LURE,
            3, FishType.SHARK_WITH_3_LURES,
            5, FishType.SHARK_WITH_5_LURES
    );

    // Used to exclude counts from level scaling probability calculator
    private static final Set<FishType> STATIC_DROP_RATES = Set.of(
            FishType.MINNOWS
    );

    // Ineligible regions
    private static final Set<Integer> INELIGIBLE_REGIONS = Set.of(
            12076   // Tempoross
    );

    private static final WorldArea[] INELIGIBLE_AREAS = {};

    private final List<CountsChangedListener> listeners = new ArrayList<>();

    // Aerial fishing
    private boolean isAerialFishing = false;
    private static final int MOLCH_ISLAND_REGION_ID = 5432;

    // Fishing Trawler
    private static final int MIN_TRAWLER_POINTS = 50;
    private static final int MIN_TRAWLER_POINTS_RATE = 5000;
    private static final int MAX_TRAWLER_POINTS = 255;
    private static final int MAX_TRAWLER_POINTS_RATE = 2500;
    private static final double TRAWLER_RATE_SCALING = (double) (MAX_TRAWLER_POINTS_RATE - MIN_TRAWLER_POINTS_RATE) /
            (MAX_TRAWLER_POINTS - MIN_TRAWLER_POINTS);

    private int currentTrawlerPoints = 0;
    private PersistentIntegerCounts trawlerCounts;

    private int prevXp;      // xp before the current action, used to detect stat changes

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Gson gson;

    @Inject
    private ActiveSkillTracker activeSkillTracker;
    private FishType lastFishType;

    private PetRollSourceTracker<FishType> fishTracker;
    private PersistentCounts<FishType> counts;
    private ItemConsumptionTracker sharkLureTracker;
    private ItemConsumptionTracker leechfinTracker;
    private ItemConsumptionTracker breamTracker;

    @Inject
    private void initialise() {
        counts = new PersistentCounts<>(configManager, gson, CONFIG_GROUP, CONFIG_KEY, FishType.class);
        fishTracker = new ChatMessageTracker<>(client, FISH_CAUGHT_PATTERN, FISH_LOOKUP, INELIGIBLE_REGIONS, INELIGIBLE_AREAS);
        sharkLureTracker = new ItemConsumptionTracker(client, ItemID.SHARK_LURE);
        leechfinTracker = new ItemConsumptionTracker(client, ItemID.LEECHFIN);
        breamTracker = new ItemConsumptionTracker(client, ItemID.BREAM_FISH_RAW);
        trawlerCounts = new PersistentIntegerCounts(configManager, gson, CONFIG_GROUP, "fishingTrawlerCountsByPoints");
    }

    /**
     * Uses the {@link RuneScapeProfileChanged} event to load the player's experience and eligible roll counts that
     * persist between sessions
     * @param event An event when the user switches to a different RuneScape save profile. This might be because
     *              they logged into a different account, or hopped to/from a Beta/Tournament/DMM/Leagues world.
     */
    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event) {
        log.debug("onRuneScapeProfileChanged fired, profile={}", configManager.getRSProfileKey());
        getFishingXp();
        loadCounts();
        notifyListeners();
    }

    /**
     * Uses the {@link StatChanged} event to detect when experience in a given skill has been gained and if the
     * player caught a fish to determine if they are eligible to receive a pet roll.
     * @param event An event where the experience, level, or boosted level of a {@link Skill} has been modified.
     */
    @Subscribe
    public void onStatChanged(StatChanged event) {
        Skill trainedSkill = event.getSkill();
        if (trainedSkill != Skill.FISHING) {
            return;
        }

        int newXp = event.getXp();

        log.debug("fishing xp: prev={} new={}", prevXp, newXp);

        if (prevXp == newXp) {
            return;
        }

        // Set previous xp equal to the latest xp so we always have a fresh previous xp to compare to when a stat change happens
        prevXp = newXp;

        FishType fishType;

        if (isAerialFishing) {
            fishType = FishType.AERIAL_FISH;
        } else {
            fishType = fishTracker.getCurrentSource();
        }

        if (fishType == null) {
            // Check if Leechfin or Bream counts increased. If neither, skip;
            int leechfinCountChange = leechfinTracker.getChangeSinceLastCheck();

            if (leechfinCountChange > 0) {
                fishType = FishType.LEECHFIN;
            } else {
                int breamCountChange = breamTracker.getChangeSinceLastCheck();
                if (breamCountChange > 0) {
                    fishType = FishType.BREAM;
                } else {
                    return;
                }
            }
        }

        // Handle drop rate changes based on shark lures
        if (fishType == FishType.SHARK) {
            // Check if the user has shark lures configured
            int sharkLureSetting = client.getVarbitValue(VarbitID.SHARK_LURE_USE_QUANTITY);

            // If shark lures configured, check consumed count
            if (sharkLureSetting > 0) {
                int consumed = -(sharkLureTracker.getChangeSinceLastCheck());

                if (consumed >= 0) {
                    fishType = SHARK_LURE_LOOKUP.getOrDefault(consumed, FishType.SHARK);
                }
            }
        }

        // Register last skill and pet source
        lastFishType = fishType;
        activeSkillTracker.setActiveSkill(getSkillType());

        // Increment counts and update overlay/side panel
        int level = client.getRealSkillLevel(Skill.FISHING);
        counts.increment(level, fishType);
        notifyListeners();
    }

    /**
     * Uses the {@link VarbitChanged} event to track Fishing Trawler points contribution
     * @param event An event fired when the Varbit for fishing trawler points changes
     */
    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (event.getVarbitId() != VarbitID.TRAWLER_ACTIVITY) {
            return;
        }

        currentTrawlerPoints = event.getValue();
    }

    /**
     * Uses the {@link ItemContainerChanged} event to update the counts of shark lure, leechfin, and bream whenever the
     * player's inventory changes (eg. banking or dropping items) so that a stale count isn't used for detecting changes
     * @param event An event fired whenever the stack size of an item in the player's inventory changes
     */
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.INV) {
            return;
        }
        sharkLureTracker.syncBaseline();
        leechfinTracker.syncBaseline();
        breamTracker.syncBaseline();
    }

    /**
     * Uses the {@link ChatMessage} event to track if the user is aerial fishing or playing the Fishing Trawler minigame
     * @param event Any game or spam chat message
     */
    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM
                && event.getType() != ChatMessageType.GAMEMESSAGE
                && event.getType() != ChatMessageType.MESBOX
        ) {
            return;
        }

        // Aerial fishing check
        if (event.getType() == ChatMessageType.SPAM
                && event.getMessage().equals("You send your cormorant to try to catch a fish from out at sea.")) {
            isAerialFishing = true;
        }

        // Disable aerial fishing flag when the player leaves the island
        var localPlayer = client.getLocalPlayer();
        if (localPlayer != null && localPlayer.getWorldLocation().getRegionID() != MOLCH_ISLAND_REGION_ID) {
            isAerialFishing = false;
        }

        // Fishing Trawler check
        Matcher matcher = TRAWLER_CATCH_PATTERN.matcher(event.getMessage());
        if (!matcher.find()) {
            return;
        }

        lastFishType = FishType.FISHING_TRAWLER;
        trawlerCounts.increment(currentTrawlerPoints);
        activeSkillTracker.setActiveSkill(getSkillType());
    }

    /**
     * Calculates the total chance of rolling the fishing pet from all games of Fishing Trawler based on the player's
     * contribution points
     * @return The probability of rolling the fishing pet from all games of Fishing Trawler that the player has played
     */
    public double getTrawlerProbability() {
        double probability = 0;

        for (Map.Entry<Integer, Integer> entry : trawlerCounts.getCountsByBucket().entrySet()) {
            int points = entry.getKey();
            int gamesAtThisPointsCount = entry.getValue();

            int rate = getTrawlerRateForPoints(points);
            double chanceAtThisPoints = 1 - Math.pow(1 - 1.0 / rate, gamesAtThisPointsCount);

            probability = 1 - (1 - probability) * (1 - chanceAtThisPoints);
        }

        return probability;
    }

    /**
     * Calculates the total chance of the player rolling the fishing pet from minnows
     * @return The probability of rolling the fishing pet from minnows
     */
    public double getMinnowProbability() {
        return 1 - Math.pow(1 - 1.0 / DROP_RATES.get(FishType.MINNOWS), counts.getTotalCount(FishType.MINNOWS));
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.FISHING;
    }

    @Override
    public void loadCounts() {
        counts.load();
        trawlerCounts.load();
    }

    @Override
    public Map<String, Integer> getFormattedTotalCounts() {
        Map<Fishing.FishType, Integer> totals = counts.getTotalCounts();
        Map<String, Integer> formatted = new LinkedHashMap<>();
        for (Fishing.FishType type : Fishing.FishType.values()) {
            if (type == FishType.FISHING_TRAWLER) {
                formatted.put(DisplayNames.format(type), trawlerCounts.getTotalCount());
            } else {
                formatted.put(DisplayNames.format(type), totals.getOrDefault(type, 0));
            }
        }
        return formatted;
    }

    /**
     * Calculates the total chance of the player receiving the fishing pet from all sources, including Fishing Trawler
     * and minnows
     * @return The probability of rolling the fishing pet from all sources
     */
    @Override
    public double getOverallProbability() {
        double mainProbability = PetRollProbabilityCalculator.calculateProbability(counts.getCountsByLevel(),
                DROP_RATES, STATIC_DROP_RATES);
        double combined = 1 - (1 - mainProbability) * (1 - getTrawlerProbability());
        combined = 1 - (1 - combined) * (1 - getMinnowProbability());
        return combined;
    }

    @Override
    public String getLastSourceName() {
        return DisplayNames.format(lastFishType);
    }

    @Override
    public int getLastSourceCount() {
        return lastFishType == null ? 0 : counts.getTotalCount(lastFishType);
    }

    @Override
    public Object getSourceTracker() {
        return fishTracker;
    }

    @Override
    public void addCountsChangedListener(CountsChangedListener listener) {
        listeners.add(listener);
    }

    /**
     * Calculates the drop rate of the fishing pet for a specific value of Fishing Trawler contribution points
     * @param points Fishing Trawler contribution points for a specific game
     * @return The drop rate of the fishing pet for the given contribution points
     */
    private int getTrawlerRateForPoints(int points) {
        if (points == MIN_TRAWLER_POINTS) { return MIN_TRAWLER_POINTS_RATE; }
        if (points == MAX_TRAWLER_POINTS) { return MAX_TRAWLER_POINTS_RATE; }

        return Math.toIntExact(Math.round(MIN_TRAWLER_POINTS + (points - MIN_TRAWLER_POINTS) * TRAWLER_RATE_SCALING));
    }

    private void getFishingXp() {
        prevXp = client.getSkillExperience(Skill.FISHING);
    }

    private void notifyListeners() {
        for (CountsChangedListener listener : listeners) {
            listener.onCountsChanged();
        }
    }
}
