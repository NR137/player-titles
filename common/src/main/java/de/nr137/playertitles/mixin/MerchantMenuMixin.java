package de.nr137.playertitles.mixin;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.nr137.playertitles.merchant.MerchantTrades;

/**
 * The trading screen's sound after a shift-click: the game takes the trader for an entity, but a Title Merchant's
 * trades (MerchantTrades) aren't one; they play it at the villager.
 */
@Mixin(MerchantMenu.class)
abstract class MerchantMenuMixin {
	@Shadow
	@Final
	private Merchant trader;

	@Inject(method = "playTradeSound", at = @At("HEAD"), cancellable = true)
	private void playerTitles$sound(CallbackInfo ci) {
		if (trader instanceof MerchantTrades trades) {
			trades.playTradeSound();
			ci.cancel();
		}
	}
}
