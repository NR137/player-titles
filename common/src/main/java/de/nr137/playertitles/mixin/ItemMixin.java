package de.nr137.playertitles.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.nr137.playertitles.loot.TitleTags;

/** Using a title name tag into the air collects a title (TitleTags); on a mob it names the mob as usual. */
@Mixin(Item.class)
abstract class ItemMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void playerTitles$titleTag(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (player instanceof ServerPlayer serverPlayer && TitleTags.isTitleTag(player.getItemInHand(hand))) {
			cir.setReturnValue(TitleTags.use(serverPlayer, player.getItemInHand(hand)) ? InteractionResult.SUCCESS : InteractionResult.FAIL);
		}
	}
}
