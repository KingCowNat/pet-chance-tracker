package com.petchancetracker.utils;

import java.util.Map;

public interface PetRollTrackable {
    SkillType getSkillType();
    double getOverallProbability();
    String getLastSourceName();
    int getLastSourceCount();
    void loadCounts();
    void addCountsChangedListener(CountsChangedListener listener);

    /**
     * Standalone object that detects this skill's roll source and has its own @Subscriber methods that need
     * registering with the event bus separately from the skill class itself.
     * @return A generic Object as that is all the event bus needs to register differently typed trackers
     */
    Object getSourceTracker();

    /**
     * Every source type for this skill, formatted as display name -> total count in order declared in the enum
     * (this is almost always just alphabetical). This lets the side panel render a skill's source breakdown
     * without needing to know its enum type.
     * @return A Map of formatted total counts by source for a skill
     */
    Map<String, Integer> getFormattedTotalCounts();
}
