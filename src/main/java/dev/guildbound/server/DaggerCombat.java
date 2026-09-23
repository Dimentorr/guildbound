package dev.guildbound.server;

import dev.guildbound.Guildbound;
import dev.guildbound.combat.DaggerState;
import dev.guildbound.combat.ResourceState;
import dev.guildbound.combat.WeaponStats;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.data.ResourceAttachments;
import dev.guildbound.world.DaggerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Guildbound.ID)
public final class DaggerCombat {
    private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "dagger"));
    private DaggerCombat() {}

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasData(ProgressAttachments.DAGGER_STATE)) {
            var previous = player.getData(ProgressAttachments.DAGGER_STATE);
            var next = previous.tick();
            if (!next.equals(previous)) player.setData(ProgressAttachments.DAGGER_STATE, next);
        }
    }

    public static void attack(ServerPlayer player, LivingEntity target) {
        if (!player.isAlive() || player.isSpectator() || player.isUsingItem() || player.isSleeping()
                || !target.isAlive() || target == player || !target.isAttackable()
                || !player.canInteractWithEntity(target, 0) || !player.hasLineOfSight(target)
                || target.skipAttackInteraction(player)) return;
        if (target instanceof ServerPlayer victim && (!player.server.isPvpAllowed() || !player.canHarmPlayer(victim))) return;
        boolean mainDagger = player.getMainHandItem().getItem() instanceof DaggerItem;
        boolean offDagger = player.getOffhandItem().getItem() instanceof DaggerItem;
        if (!mainDagger && !offDagger) return;
        boolean dual = mainDagger && offDagger;
        boolean offOnly = !mainDagger;
        var state = player.getData(ProgressAttachments.DAGGER_STATE);
        if (!state.canAttack(dual, offOnly)) return;
        boolean usesStamina = !player.isCreative() && player.getData(ProgressAttachments.PROGRESS).registered();
        var resources = player.getData(ResourceAttachments.RESOURCES);
        if (usesStamina && !resources.canAttack()) return;
        InteractionHand hand = offOnly || state.useOff(dual) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack weapon = player.getItemInHand(hand);
        int mainTime = DaggerState.duration(WeaponStats.value(player, player.getMainHandItem(), Attributes.ATTACK_SPEED));
        int offTime = offDagger ? DaggerState.duration(WeaponStats.value(player, player.getOffhandItem(), Attributes.ATTACK_SPEED)) : mainTime;
        // Reserve the attack before hurt/enchantment hooks can re-enter combat code.
        player.setData(ProgressAttachments.DAGGER_STATE, state.attack(dual, offOnly, mainTime, offTime, false));
        if (usesStamina) {
            player.setData(ResourceAttachments.RESOURCES, resources.spendStamina(ResourceState.ATTACK_COST));
            CombatEvents.sync(player);
        }
        var holder = player.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE);
        DamageSource source = new DamageSource(holder, player) {
            @Override public ItemStack getWeaponItem() { return weapon; }
        };
        float base = (float) WeaponStats.value(player, weapon, Attributes.ATTACK_DAMAGE);
        float enchanted = EnchantmentHelper.modifyDamage(player.serverLevel(), weapon, target, source, base);
        boolean fallingCrit = player.fallDistance > 0 && !player.onGround() && !player.onClimbable()
                && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger() && !player.isSprinting();
        var crit = net.neoforged.neoforge.common.CommonHooks.fireCriticalHit(player, target, fallingCrit, fallingCrit ? 1.5F : 1);
        float damage = base * (crit.isCriticalHit() ? crit.getDamageMultiplier() : 1) + (enchanted - base);
        float oldHealth = target.getHealth();
        boolean landed = target.hurt(source, damage);
        player.swing(hand, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                landed ? (crit.isCriticalHit() ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG)
                        : SoundEvents.PLAYER_ATTACK_NODAMAGE, player.getSoundSource(), 1, 1);
        if (!landed) return;
        player.setData(ProgressAttachments.DAGGER_STATE, state.attack(dual, offOnly, mainTime, offTime, true));
        float knockback = EnchantmentHelper.modifyKnockback(player.serverLevel(), weapon, target, source,
                (float) player.getAttributeValue(Attributes.ATTACK_KNOCKBACK)) + (player.isSprinting() ? 1 : 0);
        if (knockback > 0) {
            target.knockback(knockback * .5, Math.sin(Math.toRadians(player.getYRot())), -Math.cos(Math.toRadians(player.getYRot())));
            player.setSprinting(false);
        }
        if (crit.isCriticalHit()) player.crit(target);
        if (enchanted > base) player.magicCrit(target);
        player.setLastHurtMob(target);
        boolean wears = weapon.hurtEnemy(target, player);
        EnchantmentHelper.doPostAttackEffectsWithItemSource(player.serverLevel(), target, source, weapon);
        if (wears && !weapon.isEmpty()) weapon.postHurtEnemy(target, player);
        player.awardStat(Stats.DAMAGE_DEALT, Math.round(Math.max(0, oldHealth - target.getHealth()) * 10));
        player.causeFoodExhaustion(.1F);
    }
}
