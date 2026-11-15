package de.nr137.playertitles.mixin;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.nr137.playertitles.display.TitleDisplays;
import de.nr137.playertitles.merchant.MerchantBadges;
import net.minecraft.world.entity.npc.villager.Villager;

/** Who sees which player or villager: titles above the head are sent along (TitleDisplays, MerchantBadges). */
@Mixin(ServerEntity.class)
abstract class ServerEntityMixin {
	@Shadow
	@Final
	private Entity entity;

	@Inject(method = "addPairing", at = @At("TAIL"))
	private void playerTitles$seen(ServerPlayer viewer, CallbackInfo ci) {
		if (entity instanceof ServerPlayer wearer) {
			TitleDisplays.onSeen(wearer, viewer);
		} else if (entity instanceof Villager villager) {
			MerchantBadges.onSeen(villager, viewer);
		}
	}

	@Inject(method = "removePairing", at = @At("HEAD"))
	private void playerTitles$unseen(ServerPlayer viewer, CallbackInfo ci) {
		if (entity instanceof ServerPlayer wearer) {
			TitleDisplays.onUnseen(wearer, viewer);
		} else if (entity instanceof Villager villager) {
			MerchantBadges.onUnseen(villager, viewer);
		}
	}
}
