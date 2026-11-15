package de.nr137.playertitles.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.nr137.playertitles.advancement.AdvancementTitles;

/** Advancement titles follow the advancements (AdvancementTitles): given on completing, taken when lost. */
@Mixin(PlayerAdvancements.class)
abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	@Inject(method = {"award", "revoke"}, at = @At("RETURN"))
	private void playerTitles$progress(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && player != null) {
			AdvancementTitles.onProgress(player, advancement, ((PlayerAdvancements) (Object) this).getOrStartProgress(advancement).isDone());
		}
	}
}
