package de.nr137.playertitles.mixin;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** To free a workstation an unemployed Title Merchant claimed (VillagerMixin): it doesn't take the job. */
@Mixin(Villager.class)
public interface VillagerAccessor {
	@Invoker("releasePoi")
	void playerTitles$releasePoi(MemoryModuleType<GlobalPos> memory);
}
