package com.petchancetracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

import com.petchancetracker.skills.*;
import com.petchancetracker.utils.ActiveSkillTracker;
import com.petchancetracker.utils.PetRollTrackable;
import com.petchancetracker.utils.SkillType;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class PetChanceTrackerOverlay extends OverlayPanel {
    private static final NumberFormat PROBABILITY_FORMAT = new DecimalFormat("#0.00000");

    private final PetChanceTrackerConfig config;
    private final ActiveSkillTracker activeSkillTracker;
    private final Map<SkillType, PetRollTrackable> trackablesLookup;

    @Inject
    private PetChanceTrackerOverlay(PetChanceTrackerPlugin plugin, PetChanceTrackerConfig config,
                                    Woodcutting woodcutting, Fishing fishing, ActiveSkillTracker activeSkillTracker)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.config = config;
        this.activeSkillTracker = activeSkillTracker;

        List<PetRollTrackable> skills = List.of(woodcutting, fishing);
        this.trackablesLookup = skills.stream()
                .collect(Collectors.toMap(PetRollTrackable::getSkillType, Function.identity()));

        addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Pet chance tracker");
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();

        SkillType skillType = resolveDisplayedSkills();

        if (skillType == null) {
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Pet Chance Tracker")
                    .color(Color.ORANGE)
                    .build());

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Waiting for activity...")
                    .color(Color.GRAY)
                    .build());

            return super.render(graphics);
        }

        PetRollTrackable trackable = trackablesLookup.get(skillType);

        String lastSourceName = trackable.getLastSourceName();
        int lastSourceCount = trackable.getLastSourceCount();
        double probability = trackable.getOverallProbability();

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(skillType.toString())
                .color(Color.ORANGE)
                .build());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(PROBABILITY_FORMAT.format(probability * 100) + "%")
                .color(Color.YELLOW)
                .build());

        if (lastSourceName != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(lastSourceName)
                    .right(String.valueOf(lastSourceCount))
                    .build());
        }

        return super.render(graphics);
    }

    private SkillType resolveDisplayedSkills() {
        if (config.overlaySkillMode() == PetChanceTrackerConfig.OverlaySkillMode.MANUAL) {
            return config.manualOverlaySkill();
        }

        return activeSkillTracker.getLastActiveSkill();
    }
}
