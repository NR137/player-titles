package de.nr137.playertitles.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The anvil's slots (AnvilMenuMixin). */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
	@Accessor("inputSlots")
	Container playerTitles$inputSlots();

	@Accessor("resultSlots")
	ResultContainer playerTitles$resultSlots();
}
