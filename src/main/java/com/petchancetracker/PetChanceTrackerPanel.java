package com.petchancetracker;

import com.petchancetracker.skills.Woodcutting;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class PetChanceTrackerPanel extends PluginPanel {

    private final Woodcutting woodcutting;
    private final JPanel countsContainer = new JPanel();

    public PetChanceTrackerPanel(Woodcutting woodcutting) {
        this.woodcutting = woodcutting;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Log Counts");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setHorizontalAlignment(SwingConstants.CENTER);

        countsContainer.setLayout(new GridLayout(0, 2, 5, 5));

        add(title, BorderLayout.NORTH);
        add(countsContainer, BorderLayout.CENTER);
    }

    public void refresh() {
        countsContainer.removeAll();

        Map<Woodcutting.TreeType, Integer> totals = woodcutting.getTotalCounts();

        for (Woodcutting.TreeType type : Woodcutting.TreeType.values()) {
            int count = totals.getOrDefault(type, 0);

            countsContainer.add(new JLabel(formatLogName(type)));
            countsContainer.add(new JLabel(String.valueOf(count)));
        }

        revalidate();
        repaint();
    }

    private String formatLogName(Woodcutting.TreeType type) {
        String name = type.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}