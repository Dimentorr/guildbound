package dev.guildbound.server;

import dev.guildbound.Guildbound;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.*;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Guildbound.ID)
public final class SummonService {
    public static final String OWNER = "guildbound_owner", EXPIRES = "guildbound_expires";
    private SummonService() {}
    public static boolean owned(Entity e) { return e.getPersistentData().hasUUID(OWNER); }
    public static ServerPlayer owner(Entity e) {
        return owned(e) && e.level() instanceof ServerLevel level ? level.getServer().getPlayerList().getPlayer(e.getPersistentData().getUUID(OWNER)) : null;
    }
    public static boolean allied(ServerPlayer p, Entity e) {
        return p == e || p.isAlliedTo(e) || owned(e) && p.getUUID().equals(e.getPersistentData().getUUID(OWNER));
    }
    public static List<Mob> nearby(ServerPlayer p) {
        return p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64), m -> owned(m)
                && p.getUUID().equals(m.getPersistentData().getUUID(OWNER)) && m.isAlive());
    }
    public static boolean summon(ServerPlayer p, HeroAbility skill, int rank) {
        int cap = skill == HeroAbility.CALL_PACK ? 3 : skill.subclass == HeroSubclass.NECROMANCER ? 3 : 1;
        // One bounded active retinue per owner; replacing it prevents accumulation across ability choices.
        var existing = nearby(p);
        int count = skill == HeroAbility.CALL_PACK ? 3 : 1;
        if (skill.subclass == HeroSubclass.NECROMANCER && existing.size() >= cap) return false;
        var spawned = new ArrayList<Mob>();
        for (int i = 0; i < count; i++) {
            EntityType<? extends Mob> type = switch (skill) {
                case RAISE_ZOMBIE -> EntityType.ZOMBIE;
                case RAISE_SKELETON -> EntityType.SKELETON;
                case RAISE_WITHER -> EntityType.WITHER_SKELETON;
                default -> EntityType.WOLF;
            };
            Mob mob = type.create(p.serverLevel());
            if (mob == null) continue;
            boolean placed = false;
            for (int attempt = 0; attempt < 8; attempt++) {
                double angle = (i + attempt) * Math.PI / 4;
                mob.moveTo(p.getX() + Math.cos(angle) * 2, p.getY(), p.getZ() + Math.sin(angle) * 2, p.getYRot(), 0);
                if (p.serverLevel().hasChunkAt(mob.blockPosition()) && p.serverLevel().noCollision(mob) && !p.serverLevel().containsAnyLiquid(mob.getBoundingBox())) { placed = true; break; }
            }
            if (!placed) { mob.discard(); continue; }
            mob.getPersistentData().putUUID(OWNER, p.getUUID());
            mob.getPersistentData().putString("guildbound_skill", skill.name());
            mob.getPersistentData().putLong(EXPIRES, p.serverLevel().getGameTime() + 2400 + rank * 600);
            mob.setPersistenceRequired(); mob.setCanPickUpLoot(false);
            mob.targetSelector.removeAllGoals(g -> true);
            mob.goalSelector.removeAllGoals(g -> g instanceof net.minecraft.world.entity.ai.goal.FleeSunGoal
                    || g instanceof net.minecraft.world.entity.ai.goal.RestrictSunGoal);
            float health = (skill == HeroAbility.CALL_PACK ? 12 : skill == HeroAbility.CALL_MONSTER ? 45 : skill == HeroAbility.CALL_BEAST ? 36 : 24) * skill.power(rank);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health); mob.setHealth(health);
            if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null)
                mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((skill == HeroAbility.CALL_PACK ? 2 : skill == HeroAbility.CALL_MONSTER ? 7 : 4) * skill.power(rank));
            if (mob instanceof Wolf wolf) { wolf.setTame(true, false); wolf.setOwnerUUID(p.getUUID()); wolf.setOrderedToSit(false); }
            else equipUndead(mob, p.getData(ProgressAttachments.PROGRESS).tracks().stream()
                    .filter(t -> t.subclass() == HeroSubclass.NECROMANCER).mapToInt(ClassTrack::level).findFirst().orElse(0));
            mob.setCustomName(Component.translatable(skill == HeroAbility.CALL_MONSTER ? "entity.guildbound.elemental_beast" : "entity.guildbound.companion", p.getGameProfile().getName()));
            for (var slot : EquipmentSlot.values()) mob.setDropChance(slot, 0);
            if (p.serverLevel().addFreshEntity(mob)) spawned.add(mob);
        }
        if (spawned.isEmpty()) return false;
        if (skill.subclass != HeroSubclass.NECROMANCER) existing.forEach(Entity::discard);
        return true;
    }
    private static void equipUndead(Mob mob, int level) {
        int tier = UndeadEquipment.tier(level);
        Item[] armor = tier == 0 ? null : tier < 3 ? new Item[]{Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE, Items.LEATHER_HELMET}
                : tier < 5 ? new Item[]{Items.IRON_BOOTS, Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE, Items.IRON_HELMET}
                : tier < 7 ? new Item[]{Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET}
                : new Item[]{Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
        var slots = new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        if (armor != null) for (int i = 0; i < 4; i++) {
            var stack = new ItemStack(armor[i]);
            if (tier == 8) stack.enchant(mob.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PROTECTION), 4);
            mob.setItemSlot(slots[i], stack);
        }
        var weapon = new ItemStack(mob instanceof Skeleton ? Items.BOW : tier >= 7 ? Items.NETHERITE_SWORD : tier >= 4 ? Items.DIAMOND_SWORD : Items.IRON_SWORD);
        if (tier == 8) weapon.enchant(mob.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(mob instanceof Skeleton ? Enchantments.POWER : Enchantments.SHARPNESS), 4);
        mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide() || !owned(mob)) return;
        var p = owner(mob);
        if (p == null || !p.isAlive() || p.level() != mob.level() || mob.level().getGameTime() >= mob.getPersistentData().getLong(EXPIRES)
                || mob.distanceToSqr(p) > 4096) { mob.discard(); return; }
        HeroAbility skill;
        try { skill = HeroAbility.valueOf(mob.getPersistentData().getString("guildbound_skill")); }
        catch (IllegalArgumentException e) { mob.discard(); return; }
        if (!skill.available(p.getData(ProgressAttachments.PROGRESS))) { mob.discard(); return; }
        mob.clearFire();
        if (mob.tickCount % 10 != 0) return;
        mob.targetSelector.removeAllGoals(g -> true);
        var enemies = p.serverLevel().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(14), e -> e instanceof Enemy && !owned(e) && e.isAlive() && !allied(p, e));
        var target = enemies.stream().filter(mob::hasLineOfSight).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        mob.setTarget(target);
        if (target == null && mob.distanceToSqr(p) > 16) mob.getNavigation().moveTo(p, 1.2);
        if (skill == HeroAbility.CALL_MONSTER) p.serverLevel().sendParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + .5, mob.getZ(), 3, .3, .3, .3, 0);
    }
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event) {
        var source = event.getSource().getEntity();
        if (source != null && owned(source)) {
            var p = owner(source);
            if (p == null || allied(p, event.getEntity()) || event.getEntity() instanceof net.minecraft.world.entity.player.Player) { event.setCanceled(true); return; }
            if (source.getPersistentData().getString("guildbound_skill").equals(HeroAbility.CALL_PACK.name())
                    && nearby(p).stream().filter(m -> m.distanceToSqr(event.getEntity()) < 25).count() >= 2)
                event.getEntity().addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
        if (owned(event.getEntity()) && (event.getSource().is(DamageTypeTags.IS_FIRE) || event.getSource().getEntity() == owner(event.getEntity()))) event.setCanceled(true);
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) { if (owned(event.getEntity())) event.getDrops().clear(); }
    @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
        if (owned(event.getEntity()) && event.getNewAboutToBeSetTarget() instanceof net.minecraft.world.entity.player.Player) event.setCanceled(true);
    }
    @SubscribeEvent public static void experience(LivingExperienceDropEvent event) { if (owned(event.getEntity())) event.setDroppedExperience(0); }
}
