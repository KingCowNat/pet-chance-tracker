package com.petchancetracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import javax.inject.Inject;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

import com.petchancetracker.skills.Woodcutting;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class PetChanceTrackerOverlay extends OverlayPanel {
    private final PetChanceTrackerPlugin plugin;
    private final PetChanceTrackerConfig config;

    @Inject
    private PetChanceTrackerOverlay(PetChanceTrackerPlugin plugin, PetChanceTrackerConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.plugin = plugin;
        this.config = config;
        addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Pet chance tracker");
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Pet Chance Tracker")
                .color(Color.ORANGE)
                .build());

        Woodcutting woodcutting = plugin.getWoodcutting();
        double probability = woodcutting.getProbability();
        NumberFormat formatter = new DecimalFormat("#0.00000");

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(String.valueOf(formatter.format(Math.round(probability * 100000.0) / 100000.0)) + "%")
                .color(Color.YELLOW)
                .build());

        Map<Woodcutting.TreeType, Integer> totals = woodcutting.getTotalCounts();

        for (Woodcutting.TreeType type: Woodcutting.TreeType.values()) {
            int count = totals.getOrDefault(type, 0);

            if (count == 0) {
                continue;
            }

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(formatTreeName(type))
                    .right(String.valueOf(count))
                    .build());
        }

        return super.render(graphics);
    }

    private String formatTreeName(Woodcutting.TreeType type) {
        String name = type.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
