package de.nr137.playertitles.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import de.nr137.playertitles.merchant.MerchantBadges;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import de.nr137.playertitles.display.TitleDisplays;

/**
 * Packets as each player gets them: the player list with titles in their language (TitleDisplays.forViewer), and what
 * Title Merchants hold (MerchantBadges.equipmentFor).
 */
@Mixin(ServerCommonPacketListenerImpl.class)
abstract class ServerCommonPacketListenerImplMixin {
	@ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"),
		argsOnly = true)
	private Packet<?> playerTitles$forViewer(Packet<?> packet, Packet<?> original, @Nullable ChannelFutureListener listener) {
		if (packet instanceof ClientboundPlayerInfoUpdatePacket info && (Object) this instanceof ServerGamePacketListenerImpl game
				&& game.player != null) {
			return TitleDisplays.forViewer(info, game.player);
		}
		if (packet instanceof ClientboundSetEquipmentPacket equipment && (Object) this instanceof ServerGamePacketListenerImpl game
				&& game.player != null) {
			return MerchantBadges.equipmentFor(equipment, game.player);
		}
		return packet;
	}
}
