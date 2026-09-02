package com.petchancetracker.utils;

import lombok.Getter;

import javax.inject.Singleton;

@Singleton
public class ActiveSkillTracker {
    @Getter
    private SkillType lastActiveSkill;

    /**
     * Sets the active skill being trained for the overlay tracking
     * @param skillType The SkillType for the skill currently being trained
     */
    public void setActiveSkill(SkillType skillType) {
        this.lastActiveSkill = skillType;
    }
}