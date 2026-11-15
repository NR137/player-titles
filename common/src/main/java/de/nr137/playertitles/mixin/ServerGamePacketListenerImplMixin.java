package de.nr137.playertitles.mixin;

import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.display.TitleDisplays;

/** Chat: a player's message gets the configured format and their title (TitleDisplays.chatType). */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Redirect(method = "broadcastChatMessage", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/network/chat/ChatType;bind(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/ChatType$Bound;"))
	private ChatType.Bound playerTitles$chatType(ResourceKey<ChatType> type, Entity entity) {
		if (type != ChatType.CHAT || !(entity instanceof ServerPlayer sender)) {
			return ChatType.bind(type, entity);
		}
		// Each receiver may get it again in their language (ServerPlayerMixin), until the message is sent.
		TitleDisplays.chatSending(sender);
		return TitleDisplays.chatType(sender, PlayerTitles.sharedLanguage());
	}

	@Inject(method = "broadcastChatMessage", at = @At("RETURN"))
	private void playerTitles$sent(CallbackInfo ci) {
		TitleDisplays.chatSending(null);
	}
}
