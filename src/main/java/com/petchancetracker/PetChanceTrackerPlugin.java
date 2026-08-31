package com.petchancetracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;

import com.petchancetracker.skills.*;
import com.petchancetracker.utils.PetRollTrackable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "Pet Chance Tracker"
)
public class PetChanceTrackerPlugin extends Plugin
{
	@Inject
	private EventBus eventBus;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PetChanceTrackerOverlay overlay;

	@Inject
	private ClientToolbar clientToolbar;

    @Inject
	private Woodcutting woodcutting;

	@Inject
	private Fishing fishing;

	@Inject
	private ConfigManager configManager;

	private PetChanceTrackerPanel panel;
	private NavigationButton navButton;

	private List<PetRollTrackable> skills;

	/* A list of everything that needs registering/unregistering with the event bus. This includes each skill class
	   and their standalone source trackers (each is a separate object with its own @Subscribe methods that isn't
	   picked up by registering the skill class alone)
	 */
	private List<Object> eventSubscribers;

    @Override
	protected void startUp() throws Exception
	{
		log.debug("Pet Chance Tracker started!");

		skills = List.of(fishing, woodcutting);

		eventSubscribers = new ArrayList<>();
		for (PetRollTrackable skill : skills) {
			eventSubscribers.add(skill);
			eventSubscribers.add(skill.getSourceTracker());
		}

		// Register everything in need of registering
		eventSubscribers.forEach(eventBus::register);

		overlayManager.add(overlay);

		if (configManager.getRSProfileKey() != null)
		{
			skills.forEach(PetRollTrackable::loadCounts);
		}

		// Add side panel
		panel = new PetChanceTrackerPanel(skills);
		panel.refresh();

		skills.forEach(skill ->
				skill.addCountsChangedListener(() -> SwingUtilities.invokeLater(panel::refresh)));

		navButton = createNavButton();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Pet Chance Tracker stopped!");

		// Unregister everything in need of unregistering
		eventSubscribers.forEach(eventBus::unregister);

		// Remove overlay and side panel
		overlayManager.remove(overlay);

		if (navButton != null) {
			clientToolbar.removeNavigation(navButton);
		}
	}

	private NavigationButton createNavButton() throws java.io.IOException
	{
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		return NavigationButton.builder()
				.tooltip("Pet Chance Tracker")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();
	}

	@Provides
	PetChanceTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PetChanceTrackerConfig.class);
	}
}
