package com.petchancetracker;

import com.petchancetracker.skills.*;
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

        for (PetRollTrackable skill : skills) {
            JPanel container = new JPanel(new GridLayout(0, 2, 5, 5));
            JScrollPane scrollPane = new JScrollPane(container);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            cardContainer.add(scrollPane, skill.getSkillType().name());

            countsContainers.put(skill.getSkillType(), container);
        }

        add(skillSelector, BorderLayout.NORTH);
        add(cardContainer, BorderLayout.CENTER);
    }

    public void refresh() {
        for (PetRollTrackable skill : skills) {
            JPanel container = countsContainers.get(skill.getSkillType());
            if (container == null) { continue; }

            Map<String, Integer> formatted = skill.getFormattedTotalCounts();
            log.debug("refresh: skill={}, entries={}", skill.getSkillType(), formatted);

            container.removeAll();

            for (Map.Entry<String, Integer> entry : formatted.entrySet()) {
                container.add(new JLabel(entry.getKey()));
                container.add(new JLabel(String.valueOf(entry.getValue())));
            }

            container.revalidate();
            container.repaint();
        }
    }
}