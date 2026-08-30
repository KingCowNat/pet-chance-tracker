package com.petchancetracker.skills;

import com.google.gson.Gson;
import com.petchancetracker.utils.*;
import lombok.Getter;
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
    private static final Pattern FISH_CAUGHT_PATTERN = Pattern.compile("^You catch (?:an?|some|\\d*) ([a-zA-Z ]+)[.!]");

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
        FISHING_TRAWLER_REGULAR,
        FISHING_TRAWLER_MAX_CONTRIBUTION,
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
        SHARK_LURE_1,
        SHARK_LURE_3,
        SHARK_LURE_5,
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
            entry(FishType.FISHING_TRAWLER_REGULAR, 5000),              // Static rate
            entry(FishType.FISHING_TRAWLER_MAX_CONTRIBUTION, 2500),     // Static rate
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
            entry(FishType.SHARK_LURE_1, 328972),
            entry(FishType.SHARK_LURE_3, 411215),
            entry(FishType.SHARK_LURE_5, 493458),
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
            //entry("", FishType.BLUEFIN),                      // Sailing
            //entry("", FishType.BREAM),                        // Moons of Peril
            entry("raw catfish", FishType.CATFISH),
            entry("raw cave eel", FishType.CAVE_EEL),
            entry("raw cavefish", FishType.CAVEFISH),
            entry("raw cod", FishType.COD),
            entry("dark crab", FishType.DARK_CRAB),
            //entry("raw ", FishType.GIANT_KRILL),              // Sailing
            entry("raw guppy", FishType.GUPPY),
            //entry("raw ", FishType.HADDOCK),                  // Sailing
            //entry("raw ", FishType.HALIBUT),                  // Sailing
            entry("raw herring", FishType.HERRING),
            entry("infernal eel", FishType.INFERNAL_EEL),
            //entry("raw ", FishType.JUMBO_SQUID),              // Sailing
            entry("karambwan", FishType.KARAMBWAN),
            entry("karambwanji", FishType.KARAMBWANJI),
            entry("leaping salmon", FishType.LEAPING_SALMON),
            entry("leaping sturgeon", FishType.LEAPING_STURGEON),
            entry("leaping trout", FishType.LEAPING_TROUT),
            //entry("", FishType.LEECHFIN),                     // Vampyrium
            entry("lobster", FishType.LOBSTER),
            entry("raw mackerel", FishType.MACKEREL),
            //entry("raw ", FishType.MARLIN),                   // Sailing
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
            //entry("raw ", FishType.SWORDTIP_SQUID),           // Sailing
            entry("raw tetra", FishType.TETRA),
            entry("raw trout", FishType.TROUT),
            entry("raw tuna", FishType.TUNA)
            //entry("raw ", FishType.YELLOWFIN)                // Sailing
    );

    private static final Map<Integer, FishType> SHARK_LURE_LOOKUP = Map.of(
            0, FishType.SHARK,
            1, FishType.SHARK_LURE_1,
            3, FishType.SHARK_LURE_3,
            5, FishType.SHARK_LURE_5
    );

    // Used to exclude counts from level scaling probability calculator
    private static final Set<FishType> STATIC_DROP_RATES = Set.of(
            FishType.FISHING_TRAWLER_REGULAR,
            FishType.FISHING_TRAWLER_MAX_CONTRIBUTION,
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
    private static final int MAX_TRAWLER_POINTS = 255;
    private boolean maxTrawlerContribution = false;

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

    @Inject
    private void initialise() {
        counts = new PersistentCounts<>(configManager, gson, CONFIG_GROUP, CONFIG_KEY, FishType.class);
        fishTracker = new ChatMessageTracker<>(client, FISH_CAUGHT_PATTERN, FISH_LOOKUP, INELIGIBLE_REGIONS, INELIGIBLE_AREAS);
        sharkLureTracker = new ItemConsumptionTracker(client, ItemID.SHARK_LURE);
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
            return;
        }

        // Handle drop rate changes based on shark lures
        if (fishType == FishType.SHARK) {
            // Check if the user has shark lures configured
            int sharkLureSetting = client.getVarbitValue(VarbitID.SHARK_LURE_USE_QUANTITY);

            // If shark lures configured, check consumed count
            if (sharkLureSetting > 0) {
                int consumed = sharkLureTracker.getConsumedSinceLastCheck();

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

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (event.getVarbitId() != VarbitID.TRAWLER_ACTIVITY) {
            return;
        }

        if (event.getValue() >= MAX_TRAWLER_POINTS) {
            maxTrawlerContribution = true;
        }
    }

    // Whenever the player's inventory is updated, refresh lure count
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.INV) {
            return;
        }
        sharkLureTracker.getConsumedSinceLastCheck();
    }

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

        Matcher matcher = TRAWLER_CATCH_PATTERN.matcher(event.getMessage());
        if (!matcher.find()) {
            return;
        }

        int level = client.getRealSkillLevel(Skill.FISHING);

        if (maxTrawlerContribution) {
            lastFishType = FishType.FISHING_TRAWLER_MAX_CONTRIBUTION;
            counts.increment(level, FishType.FISHING_TRAWLER_MAX_CONTRIBUTION);
        } else {
            lastFishType = FishType.FISHING_TRAWLER_REGULAR;
            counts.increment(level, FishType.FISHING_TRAWLER_REGULAR);
        }

        activeSkillTracker.setActiveSkill(getSkillType());

        // Reset for next round
        maxTrawlerContribution = false;
    }

    // NEEDS UPDATING - DROP RATE SCALES LINEARLY BETWEEN MINIMUM AND MAXIMUM CONTRIBUTION INSTEAD OF A JUMP
    public double getTrawlerProbability() {
        double chanceFromNormal = 1 - Math.pow(1 - 1.0 / DROP_RATES.get(FishType.FISHING_TRAWLER_REGULAR),
                counts.getTotalCount(FishType.FISHING_TRAWLER_REGULAR));
        double chanceFromMax = 1 - Math.pow(1 - 1.0 / DROP_RATES.get(FishType.FISHING_TRAWLER_MAX_CONTRIBUTION),
                counts.getTotalCount(FishType.FISHING_TRAWLER_MAX_CONTRIBUTION));

        return 1 - (1 - chanceFromNormal) * (1 - chanceFromMax);
    }

    public double getMinnowProbability() {
        return 1 - Math.pow(1 - 1.0 / DROP_RATES.get(FishType.MINNOWS), counts.getTotalCount(FishType.MINNOWS));
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.FISHING;
    }

    @Override
    public void loadCounts() { counts.load(); }

    @Override
    public Map<String, Integer> getFormattedTotalCounts() {
        Map<Fishing.FishType, Integer> totals = counts.getTotalCounts();
        Map<String, Integer> formatted = new LinkedHashMap<>();
        for (Fishing.FishType type : Fishing.FishType.values()) {
            formatted.put(DisplayNames.format(type), totals.getOrDefault(type, 0));
        }
        return formatted;
    }

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

    private void getFishingXp() {
        prevXp = client.getSkillExperience(Skill.FISHING);
    }

    private void notifyListeners() {
        for (CountsChangedListener listener : listeners) {
            listener.onCountsChanged();
        }
    }
}
