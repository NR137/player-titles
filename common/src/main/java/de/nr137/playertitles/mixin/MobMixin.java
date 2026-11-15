package de.nr137.playertitles.mixin;

import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.nr137.playertitles.merchant.TitleMerchants;

/** A Title Merchant stays one through zombie villager and cure, as a villager keeps its job (TitleMerchants). */
@Mixin(Mob.class)
abstract class MobMixin {
	@Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",
		at = @At("RETURN"))
	private <T extends Mob> void playerTitles$converted(EntityType<T> type, ConversionParams params, EntitySpawnReason reason,
			ConversionParams.AfterConversion<T> after, CallbackInfoReturnable<T> cir) {
		TitleMerchants.onConverted((Mob) (Object) this, cir.getReturnValue());
	}
}
