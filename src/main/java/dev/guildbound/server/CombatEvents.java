package dev.guildbound.server;

import dev.guildbound.Guildbound;
import dev.guildbound.combat.ResourceState;
import dev.guildbound.combat.ShieldCombat;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.data.ResourceAttachments;
import dev.guildbound.network.ResourcesPayload;
import dev.guildbound.network.UseAbilityPayload;
import dev.guildbound.progression.Talent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = Guildbound.ID)
public final class CombatEvents {
    private CombatEvents() {}

    private static boolean active(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative()
                && player.getData(ProgressAttachments.PROGRESS).registered();
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new dev.guildbound.network.SkillsPayload(player.getData(ProgressAttachments.ABILITIES)));
        PacketDistributor.sendToPlayer(player, new ResourcesPayload(player.getData(ResourceAttachments.RESOURCES), player.getData(ProgressAttachments.FIRE_COOLDOWN)));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player)) return;
        int level = player.getData(ProgressAttachments.PROGRESS).totalLevel();
        int staminaMax=dev.guildbound.combat.LevelStats.stamina(level), manaMax=dev.guildbound.combat.LevelStats.mana(level);
        var previous = player.getData(ResourceAttachments.RESOURCES);
        boolean shield = ShieldCombat.raisedShield(player);
        if (!previous.canSprint(shield) || (shield && !ShieldCombat.mobileGuard(player))) player.setSprinting(false);
        var updated = previous.tick(player.isSprinting() && !player.isPassenger(), shield,
                player.getData(ProgressAttachments.TALENTS).effectiveRank(Talent.ENDURANCE),
                player.getData(ProgressAttachments.TALENTS).effectiveRank(Talent.MEDITATION), staminaMax, manaMax);
        if (dev.guildbound.world.ClassEquipment.fullSet(player, dev.guildbound.progression.HeroClass.MAGE))
            updated = new ResourceState(updated.stamina(), Math.min(manaMax, updated.mana() + 1),
                    updated.recoveryDelay(), updated.dodgeCooldown(), updated.healCooldown(), updated.exhausted());
        if (SkillService.fieldActive(player)) updated = new ResourceState(updated.stamina(), Math.min(manaMax,previous.mana()), updated.recoveryDelay(), updated.dodgeCooldown(), updated.healCooldown(), updated.exhausted());
        if (!updated.canSprint(shield)) player.setSprinting(false);
        if (!updated.equals(previous)) {
            player.setData(ResourceAttachments.RESOURCES, updated);
            // At most four regular snapshots per second, plus important boundary transitions.
            if (player.tickCount % 5 == 0 || updated.cooldownFinishedSince(previous) || updated.exhausted() != previous.exhausted()
                    || updated.canSprint(shield) != previous.canSprint(shield)
                    || updated.stamina() == staminaMax && previous.stamina() != staminaMax
                    || updated.mana() == manaMax && previous.mana() != manaMax) sync(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void channelTick(PlayerTickEvent.Post event) { if (event.getEntity() instanceof ServerPlayer p) SkillService.tickField(p); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer daggerUser
                && (daggerUser.getMainHandItem().getItem() instanceof dev.guildbound.world.DaggerItem
                    || daggerUser.getOffhandItem().getItem() instanceof dev.guildbound.world.DaggerItem)
                && event.getTarget() instanceof net.minecraft.world.entity.LivingEntity target) {
            event.setCanceled(true);
            DaggerCombat.attack(daggerUser, target);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player)) return;
        var state = player.getData(ResourceAttachments.RESOURCES);
        if (!state.canAttack()) { event.setCanceled(true); return; }
        player.setData(ResourceAttachments.RESOURCES, state.spendStamina(ResourceState.ATTACK_COST));
        sync(player);
    }

    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player)) return;
        // Jumping remains possible at zero so exhaustion cannot trap the player in terrain.
        player.setData(ResourceAttachments.RESOURCES,
                player.getData(ResourceAttachments.RESOURCES).spendStamina(ResourceState.JUMP_COST));
        sync(player);
    }

    public static void useAbility(UseAbilityPayload payload, IPayloadContext context) {
        if (payload.ability() == UseAbilityPayload.Ability.FIRE) {
            if (context.player() instanceof ServerPlayer player && !CantripEvents.cast(player)) sync(player);
            return;
        }
        if (payload.ability() == UseAbilityPayload.Ability.HEAL) {
            if (context.player() instanceof ServerPlayer player && !HealingService.cast(player)) sync(player);
            return;
        }
        if (!(context.player() instanceof ServerPlayer player) || !active(player)
                || player.isPassenger() || player.isSleeping()) return;
        var state = player.getData(ResourceAttachments.RESOURCES);
        if (payload.ability() == UseAbilityPayload.Ability.DODGE) {
            if (!state.canDodge() || !dev.guildbound.combat.ClassBonuses.canDodgeFrom(
                    player.getData(ProgressAttachments.PROGRESS), player.onGround())
                    || player.isInWaterOrBubble() || player.isInLava() || player.isFallFlying()) return;
            Vec3 direction = new Vec3(-Math.sin(Math.toRadians(player.getYRot())), 0,
                    Math.cos(Math.toRadians(player.getYRot())));
            player.setData(ResourceAttachments.RESOURCES, state.dodge());
            player.setDeltaMovement(direction.scale(0.9).add(0, player.getDeltaMovement().y, 0));
            player.hurtMarked = true;
        }
        sync(player);
    }
}

