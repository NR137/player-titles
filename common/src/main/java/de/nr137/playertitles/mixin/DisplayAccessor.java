package de.nr137.playertitles.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Display entities' setters are private; the title above a player is one that is never added to a world. */
@Mixin(Display.class)
public interface DisplayAccessor {
	@Invoker("setTransformation")
	void playerTitles$setTransformation(Transformation transformation);

	@Invoker("setBillboardConstraints")
	void playerTitles$setBillboardConstraints(Display.BillboardConstraints constraints);
}
