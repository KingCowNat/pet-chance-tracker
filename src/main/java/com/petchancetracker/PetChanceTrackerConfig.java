package com.petchancetracker;

import com.petchancetracker.utils.SkillType;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("petchancetracker")
public interface PetChanceTrackerConfig extends Config
{
    enum OverlaySkillMode {
        AUTO,
        MANUAL
    }

    @ConfigItem(
            keyName = "overlaySkillMode",
            name = "Overlay skill mode",
            description = "Auto: shows whichever skill you last trained. Manual: always shows the skill selected below.",
            position = 0
    )
    default OverlaySkillMode overlaySkillMode() {
        return OverlaySkillMode.AUTO;
    }

    @ConfigItem(
            keyName = "manualOverlaySkill",
            name = "Manual overlay skill",
            description = "Which skill to display when the mode above is set to Manual.",
            position = 1
    )
    default SkillType manualOverlaySkill() {
        return SkillType.FISHING;
    }
}
