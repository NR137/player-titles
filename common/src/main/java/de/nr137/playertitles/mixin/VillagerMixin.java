package de.nr137.playertitles.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.nr137.playertitles.PlayerTitles;
import de.nr137.playertitles.merchant.MerchantScreen;
import de.nr137.playertitles.merchant.TitleMerchants;

/**
 * Title Merchants (TitleMerchants): picking up a name tag, talking to one, losing one; and the merchant options for
 * villagers: nitwits from breeding, nitwits without bad gossip.
 */
@Mixin(Villager.class)
abstract class VillagerMixin {
	@Inject(method = "wantsToPickUp", at = @At("RETURN"), cancellable = true)
	private void playerTitles$wantsNameTag(ServerLevel level, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && stack.is(Items.NAME_TAG) && TitleMerchants.canBecome((Villager) (Object) this)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "pickUpItem", at = @At("HEAD"), cancellable = true)
	private void playerTitles$becomeMerchant(ServerLevel level, ItemEntity entity, CallbackInfo ci) {
		if (TitleMerchants.pickUp(level, (Villager) (Object) this, entity)) {
			ci.cancel();
		} else if (entity.getItem().is(Items.NAME_TAG)) {
			ci.cancel(); // a name tag it may not take (it only wanted it to become a merchant)
		}
	}

	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void playerTitles$talk(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		Villager villager = (Villager) (Object) this;
		if (player instanceof ServerPlayer serverPlayer && villager.isAlive() && !player.getItemInHand(hand).is(Items.VILLAGER_SPAWN_EGG)
				&& TitleMerchants.isMerchant(villager)) {
			if (hand == InteractionHand.MAIN_HAND && PlayerTitles.MERCHANT.check(serverPlayer)) {
				if (TitleMerchants.config().tradingScreen()) {
					de.nr137.playertitles.merchant.MerchantTrades.open(serverPlayer, villager);
				} else {
					MerchantScreen.open(serverPlayer, villager);
				}
			}
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}

	/**
	 * An unemployed Title Merchant doesn't take a job: when the game gives it one (it found a workstation), it stays
	 * unemployed and the workstation is free again for others.
	 */
	@org.spongepowered.asm.mixin.injection.ModifyVariable(method = "setVillagerData", at = @At("HEAD"), argsOnly = true)
	private net.minecraft.world.entity.npc.villager.VillagerData playerTitles$noJob(net.minecraft.world.entity.npc.villager.VillagerData data) {
		Villager villager = (Villager) (Object) this;
		if (villager.level() instanceof ServerLevel level && villager.getVillagerData() != null
				&& villager.getVillagerData().profession().is(VillagerProfession.NONE) && !data.profession().is(VillagerProfession.NONE)
				&& TitleMerchants.isMerchant(villager)) {
			VillagerAccessor accessor = (VillagerAccessor) villager;
			accessor.playerTitles$releasePoi(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE);
			accessor.playerTitles$releasePoi(net.minecraft.world.entity.ai.memory.MemoryModuleType.POTENTIAL_JOB_SITE);
			return data.withProfession(level.registryAccess(), VillagerProfession.NONE);
		}
		return data;
	}

	@Inject(method = "die", at = @At("HEAD"))
	private void playerTitles$dying(DamageSource source, CallbackInfo ci) {
		TitleMerchants.onDying((Villager) (Object) this, source);
	}

	@Inject(method = "die", at = @At("TAIL"))
	private void playerTitles$died(DamageSource source, CallbackInfo ci) {
		TitleMerchants.onDeath((Villager) (Object) this);
	}

	/** Bred villagers become nitwits with breedNitwitChance (vanilla: never). */
	@Inject(method = "getBreedOffspring", at = @At("RETURN"))
	private void playerTitles$nitwit(ServerLevel level, AgeableMob partner, CallbackInfoReturnable<Villager> cir) {
		Villager baby = cir.getReturnValue();
		if (baby != null && level.getRandom().nextDouble() < TitleMerchants.config().breedNitwitChance()) {
			baby.setVillagerData(baby.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NITWIT));
		}
	}

	/** Hurting a nitwit: no bad gossip unless nitwitKillsAffectReputation. */
	@Redirect(method = "setLastHurtByMob", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;onReputationEvent(Lnet/minecraft/world/entity/ai/village/ReputationEventType;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/ReputationEventHandler;)V"))
	private void playerTitles$hurtGossip(ServerLevel level, ReputationEventType type, Entity source, ReputationEventHandler handler) {
		if (!playerTitles$quietNitwit()) {
			level.onReputationEvent(type, source, handler);
		}
	}

	/** Killing a nitwit: the witnesses aren't told unless nitwitKillsAffectReputation. */
	@Inject(method = "tellWitnessesThatIWasMurdered", at = @At("HEAD"), cancellable = true)
	private void playerTitles$killGossip(Entity murderer, CallbackInfo ci) {
		if (playerTitles$quietNitwit()) {
			ci.cancel();
		}
	}

	/** A nitwit without the usual bad gossip; Title Merchants aren't counted: they are worth something. */
	private boolean playerTitles$quietNitwit() {
		Villager villager = (Villager) (Object) this;
		return !TitleMerchants.config().nitwitKillsAffectReputation() && villager.getVillagerData().profession().is(VillagerProfession.NITWIT)
			&& !TitleMerchants.isMerchant(villager);
	}
}
