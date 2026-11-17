package de.nr137.playertitles.mixin;

import java.util.List;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import de.nr137.playertitles.loot.TitleTags;

/** A catch: name tags fished up may become title name tags (TitleTags). */
@Mixin(FishingHook.class)
abstract class FishingHookMixin {
	@Redirect(method = "retrieve", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
	private it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> playerTitles$catch(LootTable table, LootParams params) {
		it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> items = table.getRandomItems(params);
		TitleTags.onFished(items, params.getLevel().getRandom());
		return items;
	}
}
