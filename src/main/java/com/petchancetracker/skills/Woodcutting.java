package com.petchancetracker.skills;

import com.google.gson.Gson;
import com.petchancetracker.utils.PersistentCounts;
import com.petchancetracker.utils.PetRollProbabilityCalculator;
import com.petchancetracker.utils.PetRollSourceTracker;
import com.petchancetracker.utils.TargetInteractionTracker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;

import javax.inject.Inject;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Map.entry;

@Slf4j
public class Woodcutting {
    private static final String CONFIG_GROUP = "petchancetracker";
    private static final String CONFIG_KEY = "woodcuttingCountsByLevel";

    // Regex to find the target of the MenuOptionClicked event
    private static final Pattern TARGET_TREE_PATTERN = Pattern.compile("^<col=[a-fA-F0-9]+>(.*?)$");

    // All Beaver sources
    public enum TreeType {
        ACHEY_TREE,
        ARCTIC_PINE_TREE,
        BLISTERWOOD_TREE,
        BLOODWOOD_TREE,
        CAMPHOR_TREE,
        ENGORGED_BLOODWOOD_TREE,
        HOLLOW_TREE,
        IRONWOOD_TREE,
        JATOBA_TREE,
        JUNIPER_TREE,
        MAGIC_TREE,
        MAHOGANY_TREE,
        MAPLE_TREE,
        NORMAL_TREE,
        OAK_TREE,
        REDWOOD_TREE,
        ROSEWOOD_TREE,
        SULLIUSCEP,
        TEAK_TREE,
        WILLOW_TREE,
        YEW_TREE
    }

    // All Beaver base drop rates
    private static final Map<TreeType, Integer> DROP_RATES = Map.ofEntries(
            entry(TreeType.ACHEY_TREE, 317647),
            entry(TreeType.ARCTIC_PINE_TREE, 145758),
            entry(TreeType.BLISTERWOOD_TREE, 289286),
            entry(TreeType.BLOODWOOD_TREE, 969283),
            entry(TreeType.CAMPHOR_TREE, 145013),
            entry(TreeType.ENGORGED_BLOODWOOD_TREE, 319283),
            entry(TreeType.HOLLOW_TREE, 214367),
            entry(TreeType.IRONWOOD_TREE, 72321),
            entry(TreeType.JATOBA_TREE, 264336),
            entry(TreeType.JUNIPER_TREE, 360000),
            entry(TreeType.MAGIC_TREE, 72321),
            entry(TreeType.MAHOGANY_TREE, 220623),
            entry(TreeType.MAPLE_TREE, 221918),
            entry(TreeType.NORMAL_TREE, 317647),
            entry(TreeType.OAK_TREE, 361146),
            entry(TreeType.REDWOOD_TREE, 72321),
            entry(TreeType.ROSEWOOD_TREE, 72321),
            entry(TreeType.SULLIUSCEP, 343000),
            entry(TreeType.TEAK_TREE, 264336),
            entry(TreeType.WILLOW_TREE, 289286),
            entry(TreeType.YEW_TREE, 145013)
    );

    // Lookup map to match target object to source enum
    private static final Map<String, TreeType> TREE_LOOKUP = Map.ofEntries(
            entry("achey tree", TreeType.ACHEY_TREE),
            entry("arctic pine tree", TreeType.ARCTIC_PINE_TREE),
            entry("blisterwood tree", TreeType.BLISTERWOOD_TREE),
            entry("bloodwood tree", TreeType.BLOODWOOD_TREE),
            entry("camphor tree", TreeType.CAMPHOR_TREE),
            entry("engorged bloodwood tree", TreeType.ENGORGED_BLOODWOOD_TREE),
            entry("hollow tree", TreeType.HOLLOW_TREE),
            entry("ironwood tree", TreeType.IRONWOOD_TREE),
            entry("jatoba tree", TreeType.JATOBA_TREE),
            entry("juniper tree", TreeType.JUNIPER_TREE),
            entry("magic tree", TreeType.MAGIC_TREE),
            entry("mahogany tree", TreeType.MAHOGANY_TREE),
            entry("maple tree", TreeType.MAPLE_TREE),
            entry("tree", TreeType.NORMAL_TREE),
            entry("oak tree", TreeType.OAK_TREE),
            entry("redwood tree", TreeType.REDWOOD_TREE),
            entry("rosewood tree", TreeType.ROSEWOOD_TREE),
            entry("sulliuscep", TreeType.SULLIUSCEP),
            entry("teak tree", TreeType.TEAK_TREE),
            entry("willow tree", TreeType.WILLOW_TREE),
            entry("yew tree", TreeType.YEW_TREE)
    );

    // Tree interact options
    private static final Set<String> INTERACT_OPTIONS = Set.of(
            "Chop",
            "Chop down",
            "Cut"
    );

    // Ineligible regions
    private static final Set<Integer> INELIGIBLE_REGIONS = Set.of(
            6462,   // Wintertodt
            15150,  // Trouble Brewing
            12080, 12336, 12592, 12335, // Tutorial Island
            10044,  // Miscellania
            10300,  // Etceteria
            10536,  // Pest Control
            5268    // Ruins of Mokhaiotl
    );

    private static final WorldArea[] INELIGIBLE_AREAS = {
            new WorldArea(2752, 2933, 204, 11, 0)   // Kharazi Jungle entrance
    };

    // Count listener to update side panel
    public interface CountsChangedListener {
        void onCountsChanged();
    }
    private final List<CountsChangedListener> listeners = new ArrayList<>();

    private int prevXp;         // xp before the current action, used to detect stat changes

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Gson gson;

    @Getter
    private PetRollSourceTracker<TreeType> treeTracker;
    private PersistentCounts<TreeType> counts;

    @Inject
    private void initialise() {
        counts = new PersistentCounts<>(configManager, gson, CONFIG_GROUP, CONFIG_KEY, TreeType.class);
        treeTracker = new TargetInteractionTracker<>(client, TARGET_TREE_PATTERN, TREE_LOOKUP, INELIGIBLE_REGIONS,
                INELIGIBLE_AREAS, INTERACT_OPTIONS);
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
        getWoodcuttingXp();
        loadTreeCounts();
        notifyListeners();
    }

    /**
     * Uses the {@link StatChanged} event to detect when experience in a given skill has been gained and if the
     * player was performing an action where they are eligible to receive a pet roll.
     * @param event An event where the experience, level, or boosted level of a {@link Skill} has been modified.
     */
    @Subscribe
    public void onStatChanged(StatChanged event) {
        Skill skill = event.getSkill();
        if (skill != Skill.WOODCUTTING) {
            return;
        }

        int newXp = event.getXp();

        log.debug("woodcutting xp: prev={} new={}", prevXp, newXp);

        if (prevXp == newXp) {
            return;
        }

        // Set initial xp equal to the latest xp so we always have a fresh previous xp to compare to when a stat change happens
        prevXp = newXp;

        TreeType treeType = treeTracker.getCurrentSource();
        if (treeType == null) {
            return;
        }

        int level = client.getRealSkillLevel(Skill.WOODCUTTING);
        counts.increment(level, treeType);
        notifyListeners();
    }

    public void loadTreeCounts() { counts.load(); }
    public Map<TreeType, Integer> getTotalTreeCounts() { return counts.getTotalCounts(); }

    public double getOverallProbability() {
        return PetRollProbabilityCalculator.calculateProbability(counts.getCountsByLevel(), DROP_RATES);
    }

    public void addCountsChangedListener(CountsChangedListener listener) {
        listeners.add(listener);
    }

    private void getWoodcuttingXp() {
        prevXp = client.getSkillExperience(Skill.WOODCUTTING);
    }

    private void notifyListeners() {
        for (CountsChangedListener listener : listeners) {
            listener.onCountsChanged();
        }
    }
}
