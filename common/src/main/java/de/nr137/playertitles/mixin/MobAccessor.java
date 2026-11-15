package de.nr137.playertitles.mixin;

import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A mob's drop chances, so a dying Title Merchant drops nothing it holds (VillagerMixin). */
@Mixin(Mob.class)
public interface MobAccessor {
	@Accessor("dropChances")
	DropChances playerTitles$dropChances();

	@Accessor("dropChances")
	void playerTitles$setDropChances(DropChances chances);
}
