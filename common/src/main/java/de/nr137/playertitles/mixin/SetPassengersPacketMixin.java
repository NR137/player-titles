package de.nr137.playertitles.mixin;

import java.util.Arrays;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.nr137.playertitles.display.TitleDisplays;
import de.nr137.playertitles.merchant.MerchantBadges;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * A player's passengers as sent to clients include the title display (TitleDisplays), so it stays on the player
 * also when vanilla sends the passengers again. Clients that don't know the display skip it.
 */
@Mixin(ClientboundSetPassengersPacket.class)
abstract class SetPassengersPacketMixin {
	@Shadow
	@Final
	@Mutable
	private int[] passengers;

	@Inject(method = "<init>(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
	private void playerTitles$withTitle(Entity vehicle, CallbackInfo ci) {
		if (vehicle instanceof Villager villager) {
			for (int display : new int[] {MerchantBadges.displayId(villager), MerchantBadges.nameId(villager)}) {
				if (display != -1) {
					passengers = Arrays.copyOf(passengers, passengers.length + 1);
					passengers[passengers.length - 1] = display;
				}
			}
		}
		if (vehicle instanceof ServerPlayer player) {
			// The title, and the name (for the player themselves always, for others with the title; a client skips an id
			// it doesn't know).
			for (int display : new int[] {TitleDisplays.displayId(player), TitleDisplays.ownNameId(player)}) {
				if (display != -1) {
					passengers = Arrays.copyOf(passengers, passengers.length + 1);
					passengers[passengers.length - 1] = display;
				}
			}
		}
	}
}
