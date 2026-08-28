package com.petchancetracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;

import com.petchancetracker.skills.Woodcutting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Pet Chance Tracker"
)
public class PetChanceTrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PetChanceTrackerConfig config;

	@Inject
	private EventBus eventBus;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PetChanceTrackerOverlay overlay;

	@Inject
	private ClientToolbar clientToolbar;

	@Getter
    @Inject
	private Woodcutting woodcutting;

	@Inject
	private ConfigManager configManager;

	private PetChanceTrackerPanel panel;
	private NavigationButton navButton;

    @Override
	protected void startUp() throws Exception
	{
		log.debug("Pet Chance Tracker started!");
		woodcutting.loadCounts();
		eventBus.register(woodcutting);
		overlayManager.add(overlay);
		clientToolbar.addNavigation(navButton);

		if (configManager.getRSProfileKey() != null)
		{
			woodcutting.loadCounts();
		}

		panel = new PetChanceTrackerPanel(woodcutting);
		panel.refresh();

		woodcutting.addCountsChangedListener(() -> SwingUtilities.invokeLater(panel::refresh));

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Pet Chance Tracker")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Pet Chance Tracker stopped!");
		eventBus.unregister(woodcutting);
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
		}
	}

	@Provides
	PetChanceTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PetChanceTrackerConfig.class);
	}
}
