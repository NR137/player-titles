package de.nr137.playertitles.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import de.nr137.playertitles.PlayerTitles;

@Mod(PlayerTitles.MOD_ID)
public class PlayerTitlesNeoForge {
	public PlayerTitlesNeoForge() {
		PlayerTitles.init();
		NeoForge.EVENT_BUS.addListener(ServerStartingEvent.class, event -> PlayerTitles.onServerStarting(event.getServer()));
		NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> PlayerTitles.onServerStarted(event.getServer()));
		NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> PlayerTitles.onServerStopping());
		NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> PlayerTitles.onServerTick(event.getServer()));
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				PlayerTitles.onJoin(player);
			}
		});
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				PlayerTitles.onLeave(player);
			}
		});
		NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> PlayerTitles.registerCommands(event.getDispatcher()));
	}
}
