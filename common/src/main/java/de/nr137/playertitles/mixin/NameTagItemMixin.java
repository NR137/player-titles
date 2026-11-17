package de.nr137.playertitles.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.nr137.playertitles.loot.TitleTags;

/** A title name tag doesn't name mobs (a crafted name tag does): TitleTags.blockNaming. */
@Mixin(NameTagItem.class)
abstract class NameTagItemMixin {
	@Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
	private void playerTitles$noNaming(ItemStack stack, Player player, LivingEntity target, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (TitleTags.blockNaming(player, stack)) {
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
