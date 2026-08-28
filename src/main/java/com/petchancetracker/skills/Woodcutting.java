package com.petchancetracker.skills;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;

@Slf4j
public class Woodcutting {
    // Count listener to update side panel
    public interface CountsChangedListener {
        void onCountsChanged();
    }

    private final List<CountsChangedListener> listeners = new ArrayList<>();

    public void addCountsChangedListener(CountsChangedListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (CountsChangedListener listener : listeners) {
            listener.onCountsChanged();
        }
    }

    private static final String CONFIG_GROUP = "petchancetracker";
    private static final String CONFIG_KEY = "woodcuttingCountsByLevel";

    private static final Pattern LOG_CUT_PATTERN = Pattern.compile("You get (?:some|an) ([\\w ]+?)\\.");

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

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Gson gson;

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event) {
        log.debug("onRuneScapeProfileChanged fired, profile={}", configManager.getRSProfileKey());
        getWoodcuttingXp();
        loadCounts();
        notifyListeners();
    }

    // Count of eligible rolls for each tree type per level
    private final Map<Integer, Map<TreeType, Integer>> countsByLevel = new TreeMap<>();

    // Lookup map to match target object to source enum
    private static final Map<String, TreeType> ITEM_LOOKUP = Map.ofEntries(
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

    private boolean isAcheyTree = false;

    /**
     * Loads a count of all eligible rolls for each tree type per level
     */
    public void loadCounts() {
        countsByLevel.clear();

        String json = configManager.getRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY);
        log.debug("Raw stored value for {}: {}", CONFIG_KEY, json);

        if (json == null || json.isEmpty()) {
            return;
        }

        Type type = new TypeToken<Map<Integer, Map<TreeType, Integer>>>() {}.getType();

        try {
            Map<Integer, Map<TreeType, Integer>> saved = gson.fromJson(json, type);
            log.debug("loadCounts: profile={}, saved={}", configManager.getRSProfileKey(), saved);
            if (saved != null) {
                countsByLevel.putAll(saved);
            }
        } catch (ClassCastException e) {
            log.warn("Failed to load woodcutting counts, resetting", e);
        }
    }

    /**
     * Saves a count of all eligible rolls for each tree type per level
     */
    public void saveCounts() {
        String json = gson.toJson(countsByLevel);
        configManager.setRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY, json);
        log.debug("saveCounts: profile={}, json={}", configManager.getRSProfileKey(), json);
    }

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

    // Kharazi Jungle entrance
    WorldArea ineligibleArea = new WorldArea(2752, 2933, 204, 11, 0);

    /**
     * Checks to see if the tree is located in an eligible place to receive a pet roll.
     * @return {@link boolean}
     */
    public boolean isEligibleForPetRoll() {
        // Player location
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        int regionId = playerLocation.getRegionID();

        return !(INELIGIBLE_REGIONS.contains(regionId) || playerLocation.isInArea(ineligibleArea));
    }

    // Initial woodcutting xp before action
    int initialXp;

    private void getWoodcuttingXp() {
        initialXp = client.getSkillExperience(Skill.WOODCUTTING);
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

        log.debug("Initial xp: {} Stat change xp: {}",initialXp, newXp);

        if (initialXp == newXp) {
            return;
        }

        // Set initial xp equal to the latest xp so we always have a fresh previous xp to compare to when a stat change happens
        initialXp = newXp;

        if (treeType == null) {
            return;
        }

        int level = client.getRealSkillLevel(Skill.WOODCUTTING);

        Map<TreeType, Integer> levelCounts = countsByLevel.computeIfAbsent(level, k -> new EnumMap<>(TreeType.class));
        levelCounts.merge(treeType, 1, Integer::sum);


        initialXp = newXp;
        saveCounts();
        notifyListeners();
    }

    TreeType treeType;

    /**
     * Uses interactions with an object to determine what tree type is being chopped.
     * @param event Any left click interaction.
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!isEligibleForPetRoll()) {
            return;
        }

        if (!"Chop".equals(event.getMenuOption()) && !"Chop down".equals(event.getMenuOption()) && !"Cut".equals(event.getMenuOption())) {
            return;
        }

        String objectString = event.getMenuTarget();
        log.debug("Menu target: {}", objectString);

        Matcher matcher = TARGET_TREE_PATTERN.matcher(event.getMenuTarget());
        if (!matcher.find()) {
            return;
        }

        String target = matcher.group(1).trim().toLowerCase();
        treeType = ITEM_LOOKUP.get(target);

        /*
        if (treeType == null) {
            return;
        }

        log.debug("{} Poggers", treeType);



        int level = client.getRealSkillLevel(Skill.WOODCUTTING);

        Map<TreeType, Integer> levelCounts = countsByLevel.computeIfAbsent(level, k -> new EnumMap<>(TreeType.class));
        levelCounts.merge(treeType, 1, Integer::sum);

        saveCounts();
        notifyListeners();

        if () {//objectId == 2023) {
            isAcheyTree = true;
        }*/
    }

    // Bloodwood trees
    //Regular bloodwood on animation, engorged on chop option

    /**
     * Uses the chat message of successfully chopping a tree to track eligible pet rolls
     *
     * @param event Chat message event
     */
    /*@Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM
                && event.getType() != ChatMessageType.GAMEMESSAGE
                && event.getType() != ChatMessageType.MESBOX) {
            return;
        }

        if (!isEligibleForPetRoll()) {
            return;
        }

        // Regex matching to determine which tree tier was rolled against
        Matcher matcher = LOG_CUT_PATTERN.matcher(event.getMessage());
        if (!matcher.find()) {
            return;
        }

        String item = matcher.group(1).trim().toLowerCase();
        TreeType treeType;
        if (item.equals("logs") && isAcheyTree) {
            treeType = TreeType.ACHEY_TREE;
        } else {
            treeType = ITEM_LOOKUP.get(item);
        }

        if (treeType == null) {
            return;
        }

        int level = client.getRealSkillLevel(Skill.WOODCUTTING);

        Map<TreeType, Integer> levelCounts = countsByLevel.computeIfAbsent(level, k -> new EnumMap<>(TreeType.class));
        levelCounts.merge(treeType, 1, Integer::sum);

        saveCounts();
        notifyListeners();
    }*/

    /**
     * Gets a total count of eligible pet rolls for each tree type.
     * @return A map of counts for each type of tree.
     */
    public Map<TreeType, Integer> getTotalCounts() {
        Map<TreeType, Integer> totals = new EnumMap<>(TreeType.class);

        for (Map<TreeType, Integer> treeCounts : countsByLevel.values()) {
            for (Map.Entry<TreeType, Integer> entry : treeCounts.entrySet()) {
                totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return totals;
    }

    public int getTotalCount(TreeType type) {
        return getTotalCounts().getOrDefault(type, 0);
    }

    /**
     * Calculates the probability of successfully rolling the Beaver pet at least once.
     * @return Probability of successfully rolling the Beaver pet at least once.
     */
    public double getProbability() {
        double probability = 0;

        for (Map.Entry<Integer, Map<TreeType, Integer>> levelEntry : countsByLevel.entrySet()) {
            int level = levelEntry.getKey();
            Map<TreeType, Integer> treeCounts = levelEntry.getValue();

            for (Map.Entry<TreeType, Integer> treeEntry : treeCounts.entrySet()) {
                TreeType type = treeEntry.getKey();
                int count = treeEntry.getValue();
                int baseDropRate = DROP_RATES.get(type);

                double perRollChance = 1.0 / (baseDropRate - level * 25);
                double chanceAtLevel = 1 - Math.pow(1 - perRollChance, count);

                probability = 1 - (1 - probability) * (1 - chanceAtLevel);
            }
        }

        return probability;
    }

    public void reset() {
        countsByLevel.clear();
        saveCounts();
        notifyListeners();
    }
}
