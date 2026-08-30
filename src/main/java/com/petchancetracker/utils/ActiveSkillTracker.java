package com.petchancetracker.utils;

import lombok.Getter;

import javax.inject.Singleton;

@Singleton
public class ActiveSkillTracker {
    @Getter
    private SkillType lastActiveSkill;

    public void setActiveSkill(SkillType skillType) {
        this.lastActiveSkill = skillType;
    }
}