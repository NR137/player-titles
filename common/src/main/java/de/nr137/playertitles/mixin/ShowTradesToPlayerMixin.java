package de.nr137.playertitles.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.nr137.playertitles.merchant.TitleMerchants;

/**
 * A Title Merchant doesn't show trades to players who come close: the game's behaviour empties the villager's hand
 * when it stops, which took the merchant's name tag. It has no vanilla trades to show anyway.
 */
@Mixin(ShowTradesToPlayer.class)
abstract class ShowTradesToPlayerMixin {
	@Inject(method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;)Z",
		at = @At("HEAD"), cancellable = true)
	private void playerTitles$notForMerchants(ServerLevel level, Villager villager, CallbackInfoReturnable<Boolean> cir) {
		if (TitleMerchants.isMerchant(villager)) {
			cir.setReturnValue(false);
		}
	}
}
