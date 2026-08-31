package com.petchancetracker;

import com.petchancetracker.utils.PetRollTrackable;
import com.petchancetracker.utils.SkillType;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PetChanceTrackerPanel extends PluginPanel {

    private final List<PetRollTrackable> skills;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContainer = new JPanel(cardLayout);

    private final Map<SkillType, JPanel> countsContainers = new LinkedHashMap<>();

    public PetChanceTrackerPanel(List<PetRollTrackable> skills) {
        this.skills = skills;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        SkillType[] skillTypes = skills.stream()
                .map(PetRollTrackable::getSkillType)
                .toArray(SkillType[]::new);

        JComboBox<SkillType> skillSelector = new JComboBox<>(skillTypes);
        skillSelector.addActionListener(e ->
                cardLayout.show(cardContainer, ((SkillType) skillSelector.getSelectedItem()).name()));

        // Adds a small bottom margin below the dropdown to create a gap between the selector and the source counts
        JPanel selectorWrapper = new JPanel(new BorderLayout());
        selectorWrapper.add(skillSelector, BorderLayout.CENTER);
        selectorWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        for (PetRollTrackable skill : skills) {
            JPanel grid = new JPanel(new GridBagLayout());

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.add(grid, BorderLayout.NORTH);

            cardContainer.add(wrapper, skill.getSkillType().name());
            countsContainers.put(skill.getSkillType(), grid);
        }

        add(selectorWrapper, BorderLayout.NORTH);
        add(cardContainer, BorderLayout.CENTER);
    }

    public void refresh() {
        for (PetRollTrackable skill : skills) {
            JPanel container = countsContainers.get(skill.getSkillType());
            if (container == null) { continue; }

            Map<String, Integer> formatted = skill.getFormattedTotalCounts();
            log.debug("refresh: skill={}, entries={}", skill.getSkillType(), formatted);

            container.removeAll();

            int row = 0;
            for (Map.Entry<String, Integer> entry : formatted.entrySet()) {
                GridBagConstraints nameConstraints = new GridBagConstraints();
                nameConstraints.gridx = 0;                              // Left column
                nameConstraints.gridy = row;                            // Row number
                nameConstraints.weightx = 1.0;                          // Name column takes priority on spacing
                nameConstraints.fill = GridBagConstraints.HORIZONTAL;   // Name column expands to fill horizontal space
                nameConstraints.anchor = GridBagConstraints.WEST;       // Left aligns the name column
                // Adds padding between each row and between name and count column
                nameConstraints.insets = new Insets(2, 0, 2, 8);

                GridBagConstraints countConstraints = new GridBagConstraints();
                countConstraints.gridx = 1;                             // Right column
                countConstraints.gridy = row;                           // Row number
                countConstraints.weightx = 0.0;                         // Count column only takes as much space as it needs
                countConstraints.anchor = GridBagConstraints.EAST;      // Right aligns the count column
                countConstraints.insets = new Insets(2, 0, 2, 0);   // Adds padding between each row

                JLabel nameLabel = new JLabel(entry.getKey());
                JLabel countLabel = new JLabel(String.valueOf(entry.getValue()));
                countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

                container.add(nameLabel, nameConstraints);
                container.add(countLabel, countConstraints);

                row++;
            }

            container.revalidate();
            container.repaint();
        }
    }
}