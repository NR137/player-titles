package de.nr137.playertitles.mixin;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** For a copy of a player list packet with other names (TitleDisplays.forViewer). */
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerInfoPacketAccessor {
	@Mutable
	@Accessor("entries")
	void playerTitles$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
