package de.nr137.playertitles.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.display.TitleDisplays;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The name in the player list with the worn title, and titles again in a new language (TitleDisplays). */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
	@Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
	private void playerTitles$listName(CallbackInfoReturnable<Component> cir) {
		Component name = TitleDisplays.playerListName((ServerPlayer) (Object) this, PlayerTitles.sharedLanguage());
		if (name != null) {
			cir.setReturnValue(name);
		}
	}

	@Inject(method = "updateOptions", at = @At("TAIL"))
	private void playerTitles$language(ClientInformation information, CallbackInfo ci) {
		TitleDisplays.onOptionsChanged((ServerPlayer) (Object) this);
	}

	/** Chat in the receiver's language (TitleDisplays.chatTypeFor). */
	@ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), argsOnly = true)
	private ChatType.Bound playerTitles$chatLanguage(ChatType.Bound bound) {
		return TitleDisplays.chatTypeFor(bound, (ServerPlayer) (Object) this);
	}
}
