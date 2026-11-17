package de.nr137.playertitles.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.nr137.playertitles.loot.TitleTags;

/** A title name tag can't be worked in an anvil (no renaming, no combining): no result. */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
	@Inject(method = "createResult", at = @At("TAIL"))
	private void playerTitles$noTitleTags(CallbackInfo ci) {
		var slots = (ItemCombinerMenuAccessor) this;
		if (TitleTags.isTitleTag(slots.playerTitles$inputSlots().getItem(0)) || TitleTags.isTitleTag(slots.playerTitles$inputSlots().getItem(1))) {
			slots.playerTitles$resultSlots().setItem(0, ItemStack.EMPTY);
		}
	}
}
