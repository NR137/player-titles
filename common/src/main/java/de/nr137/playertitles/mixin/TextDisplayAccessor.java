package de.nr137.playertitles.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** See DisplayAccessor; the text accessor lets each viewer get the title in their own language. */
@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
	@Accessor("DATA_TEXT_ID")
	static EntityDataAccessor<Component> playerTitles$textId() {
		throw new AssertionError();
	}

	@Accessor("DATA_STYLE_FLAGS_ID")
	static EntityDataAccessor<Byte> playerTitles$flagsId() {
		throw new AssertionError();
	}

	@Invoker("setText")
	void playerTitles$setText(Component text);
}
