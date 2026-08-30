package com.petchancetracker.utils;

public enum SkillType {
    FISHING("Fishing"),
    WOODCUTTING("Woodcutting");

    private final String displayName;

    SkillType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
