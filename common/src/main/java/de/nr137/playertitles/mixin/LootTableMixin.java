package de.nr137.playertitles.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.nr137.playertitles.loot.TitleTags;

/** Loot chests (and chest minecarts) as they are filled: their name tags may become title name tags (TitleTags). */
@Mixin(LootTable.class)
abstract class LootTableMixin {
	@Inject(method = "fill", at = @At("TAIL"))
	private void playerTitles$titleTags(Container container, LootParams params, long seed, CallbackInfo ci) {
		TitleTags.onLootFilled(container, (LootTable) (Object) this, params);
	}
}
