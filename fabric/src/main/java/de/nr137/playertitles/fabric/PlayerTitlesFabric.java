package de.nr137.playertitles.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import de.nr137.playertitles.PlayerTitles;

public class PlayerTitlesFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		PlayerTitles.init();
		ServerLifecycleEvents.SERVER_STARTING.register(PlayerTitles::onServerStarting);
		ServerLifecycleEvents.SERVER_STARTED.register(PlayerTitles::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> PlayerTitles.onServerStopping());
		ServerTickEvents.END_SERVER_TICK.register(PlayerTitles::onServerTick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PlayerTitles.onJoin(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PlayerTitles.onLeave(handler.getPlayer()));
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> PlayerTitles.registerCommands(dispatcher));
	}
}
