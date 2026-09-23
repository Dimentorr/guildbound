package dev.guildbound.server;

import dev.guildbound.Guildbound;
import dev.guildbound.combat.*;
import dev.guildbound.data.*;
import dev.guildbound.progression.*;
import dev.guildbound.world.ClassEquipment;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.item.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = Guildbound.ID)
public final class SkillService {
    private record Zone(UUID owner, ServerLevel level, Vec3 center, Vec3 heading, HeroAbility ability, float power, long expiry) {}
    private static final List<Zone> ZONES = new ArrayList<>();
    private SkillService() {}
    private record Channel(ServerPlayer owner, ServerLevel level, EtherField state) {}
    private static final Map<UUID, Channel> FIELDS = new HashMap<>();
    public static boolean fieldActive(ServerPlayer p) { return FIELDS.containsKey(p.getUUID()); }
    private static void stopField(ServerPlayer p) { FIELDS.remove(p.getUUID()); p.displayClientMessage(Component.translatable("message.guildbound.field_off"), true); }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) { FIELDS.remove(e.getEntity().getUUID()); }
    public static void tickField(ServerPlayer p) {
        var channel = FIELDS.get(p.getUUID());
        if (channel == null) return;
        var r = p.getData(ResourceAttachments.RESOURCES);
        if (channel.owner != p || channel.level != p.level() || !p.isAlive() || p.isCreative() || p.isSpectator() || !HeroAbility.ARCANE_BURST.available(p.getData(ProgressAttachments.PROGRESS)) || !channel.state.canTick(r.mana())) { stopField(p); return; }
        var next = channel.state.tick();
        p.setData(ResourceAttachments.RESOURCES, new ResourceState(r.stamina(), r.mana()-EtherField.COST, r.recoveryDelay(), r.dodgeCooldown(), r.healCooldown(), r.exhausted()));
        FIELDS.put(p.getUUID(), new Channel(p, channel.level, next));
        if (next.pulse()) {
            var t = p.getData(ProgressAttachments.TALENTS);
            float damage = EtherField.damage(t.rank(Talent.ARCANE_BURST)+t.rank(Talent.FIELD_POWER)+t.rank(Talent.FIELD_POWER_II)+t.rank(Talent.FIELD_POWER_III));
            for (var target : enemies(p, 6)) target.hurt(p.damageSources().indirectMagic(p,p), damage);
        }
        if (next.elapsed()%4 == 0) for (int i=0;i<24;i++) { double a=i*Math.PI/12; p.serverLevel().sendParticles(ParticleTypes.END_ROD,p.getX()+6*Math.cos(a),p.getY()+.2,p.getZ()+6*Math.sin(a),1,0,.1,0,0); }
        if (next.elapsed()%5 == 0) CombatEvents.sync(p);
        if (next.elapsed()>=EtherField.DURATION || r.mana()==EtherField.COST) stopField(p);
    }
    public static void guildGift(ServerPlayer p, HeroSubclass subclass) {
        int bit = subclass == HeroSubclass.WIZARD ? 1 : subclass == HeroSubclass.BARD ? 2 : 0;
        if (bit == 0) return;
        int previous = p.getData(ProgressAttachments.GIFTS);
        if ((previous & bit) == 0) {
            p.setData(ProgressAttachments.GIFTS, previous | bit);
            var stack = new ItemStack(bit == 1 ? ClassEquipment.GRIMOIRE.get() : ClassEquipment.FLUTE.get());
            if (!p.getInventory().add(stack)) p.drop(stack, false);
        }
        p.displayClientMessage(Component.translatable(bit == 1 ? "message.guildbound.grimoire_guide" : "message.guildbound.flute_guide"), false);
    }
    public static boolean cast(ServerPlayer p, HeroAbility skill) {
        if (skill == HeroAbility.ARCANE_BURST && fieldActive(p)) { stopField(p); return true; }
        var profile = p.getData(ProgressAttachments.PROGRESS);
        var resources = p.getData(ResourceAttachments.RESOURCES);
        var timers = p.getData(ProgressAttachments.ABILITIES);
        if (!p.isAlive() || p.isCreative() || p.isSpectator() || p.isSleeping() || p.isPassenger() || p.isUsingItem()
                || !skill.available(profile) || timers.cooldown(skill) > 0 || !ClassEquipment.magicFocus(p)
                || skill.subclass == HeroSubclass.BARD && !ClassEquipment.holding(p, ClassEquipment.FLUTE)) return false;
        if (skill == HeroAbility.ARCANE_BURST) {
            if (resources.mana() < EtherField.COST) return false;
            FIELDS.put(p.getUUID(), new Channel(p, p.serverLevel(), new EtherField(0)));
            p.displayClientMessage(Component.translatable("message.guildbound.field_on"), true);
            return true;
        }
        int rank = p.getData(ProgressAttachments.TALENTS).rank(skill.talent());
        int cost = skill.cost * 10;
        if (skill.magical() && ClassEquipment.holding(p, ClassEquipment.ARCANE_STAFF)) cost = cost * 9 / 10;
        if (skill.magical() ? resources.mana() < cost : resources.stamina() < cost) return false;
        // Reserve cooldown and resource before callbacks; failed placement returns the reservation.
        p.setData(ProgressAttachments.ABILITIES, timers.start(skill, skill.cooldown(rank)));
        p.setData(ResourceAttachments.RESOURCES, skill.magical()
                ? new ResourceState(resources.stamina(), resources.mana() - cost, resources.recoveryDelay(), resources.dodgeCooldown(), resources.healCooldown(), resources.exhausted())
                : resources.spendStamina(cost));
        boolean success = execute(p, skill, rank);
        if (!success) { p.setData(ProgressAttachments.ABILITIES, timers); p.setData(ResourceAttachments.RESOURCES, resources); }
        else p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 12, .5, .5, .5, .1);
        CombatEvents.sync(p);
        return success;
    }
    private static boolean execute(ServerPlayer p, HeroAbility skill, int rank) {
        float power = skill.power(rank);
        long now = p.serverLevel().getGameTime();
        switch (skill) {
            case RAISE_ZOMBIE, RAISE_SKELETON, RAISE_WITHER, CALL_BEAST, CALL_PACK, CALL_MONSTER, WILD_COMPANION -> { return SummonService.summon(p, skill, rank); }
            case ARCANE_BURST -> { return false; }
            case FROST_NOVA -> {
                var targets = enemies(p, 6);
                if (targets.isEmpty()) return false;
                for (var target : targets) {
                    target.hurt(p.damageSources().indirectMagic(p, p), (skill == HeroAbility.ARCANE_BURST ? 7 : 5.75F) * power);
                    if (skill == HeroAbility.FROST_NOVA) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int)(100 * power), 1));
                }
            }
            case MAGIC_MISSILE -> {
                var target = aimedEnemy(p, 20);
                if (target == null) return false;
                target.hurt(p.damageSources().indirectMagic(p, p), 8 * 1.15F * power);
                p.serverLevel().sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1, target.getZ(), 15, .2, .4, .2, .05);
            }
            case ARCANE_WARD -> p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int)(200 * power) + 40 * (p.getData(ProgressAttachments.TALENTS).rank(Talent.WARD_TIME) + p.getData(ProgressAttachments.TALENTS).rank(Talent.WARD_TIME_II) + p.getData(ProgressAttachments.TALENTS).rank(Talent.WARD_TIME_III)), 1));
            case SONG_STRENGTH, SONG_SPEED, SONG_RESISTANCE -> {
                var effect = skill == HeroAbility.SONG_STRENGTH ? MobEffects.DAMAGE_BOOST : skill == HeroAbility.SONG_SPEED ? MobEffects.MOVEMENT_SPEED : MobEffects.DAMAGE_RESISTANCE;
                for (var ally : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, p.getBoundingBox().inflate(7), e -> e.isAlive() && p.distanceToSqr(e) <= 49))
                    ally.addEffect(new MobEffectInstance(effect, (int)(200 * power), 0));
                p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE.value(), p.getSoundSource(), 1, 1);
            }
            case HEALING_FIELD, HOLY_FIELD, TRAP, TRIPWIRE -> {
                ZONES.removeIf(z -> z.owner.equals(p.getUUID()) && z.ability == skill);
                ZONES.add(new Zone(p.getUUID(), p.serverLevel(), p.position(), p.getLookAngle().multiply(1, 0, 1).normalize(), skill, power,
                        now + (skill == HeroAbility.TRAP || skill == HeroAbility.TRIPWIRE ? 400 : 160)));
            }
            case GREAT_HEAL -> {
                var target = HealingTarget.find(p);
                if (target.getHealth() >= target.getMaxHealth()) return false;
                target.heal(9 * power * ClassEquipment.healingMultiplier(p));
            }
            case TAUNT -> {
                buff(p, "taunt_notice", (int)(160 * power));
                var targets = enemies(p, 14);
                if (targets.isEmpty()) return false;
                for (var mob : targets) {
                    var data = mob.getPersistentData();
                    var other = data.hasUUID("guildbound_taunt") ? p.server.getPlayerList().getPlayer(data.getUUID("guildbound_taunt")) : null;
                    if (data.getLong("guildbound_taunt_cast") == now && other != null && compareGuard(p.getArmorValue(), profileLevel(p), other.getArmorValue(), profileLevel(other)) < 0) continue;
                    data.putUUID("guildbound_taunt", p.getUUID()); data.putLong("guildbound_taunt_cast", now);
                    data.putLong("guildbound_taunt_until", now + (long)(160 * power)); mob.setTarget(p);
                }
            }
            case RAGE -> {
                buff(p, "rage", (int)(200 * power)); p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int)(200 * power), 0));
            }
            case RUNE_BLADE, TOXIC_BLADE, DUELIST_RIPOSTE -> {
                if (p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty()) return false;
                buff(p, skill == HeroAbility.RUNE_BLADE ? "rune" : skill == HeroAbility.TOXIC_BLADE ? "toxin" : "riposte", (int)(200 * power));
            }
            case SHADOW_STEP -> { if (!teleportToward(p, p.position().add(p.getLookAngle().scale(6 * power)))) return false; }
            case VANISH, SMOKE -> {
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int)(100 * power), 0));
                buff(p, "vanish", (int)(100 * power));
                for (var mob : enemies(p, skill == HeroAbility.SMOKE ? 8 : 16)) {
                    if (mob.getTarget() == p) mob.setTarget(null);
                    if (skill == HeroAbility.SMOKE) mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                }
                p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + .5, p.getZ(), 60, 2, 1, 2, .05);
            }
            case WILD_LEAP -> {
                var pet = SummonService.nearby(p).stream().filter(m -> p.distanceToSqr(m) <= 256).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
                if (pet == null || !teleportToward(p, pet.position().add(1, 0, 0))) return false;
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int)(100 * power), 0));
            }
            case SWAP_BEAST -> {
                var pet = SummonService.nearby(p).stream().filter(m -> p.distanceToSqr(m) <= 256 && p.hasLineOfSight(m))
                        .min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
                if (pet == null) return false;
                Vec3 destination = pet.position(), origin = p.position();
                var playerBox = p.getBoundingBox().move(destination.subtract(origin));
                var petBox = pet.getBoundingBox().move(origin.subtract(destination));
                if (p.level().getBlockCollisions(p, playerBox).iterator().hasNext() || p.level().getBlockCollisions(pet, petBox).iterator().hasNext()
                        || p.level().containsAnyLiquid(playerBox) || p.level().containsAnyLiquid(petBox)) return false;
                pet.teleportTo(origin.x, origin.y, origin.z); p.teleportTo(destination.x, destination.y, destination.z);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int)(60 * power), 0));
            }
            case HUNTER_MARK -> {
                var target = aimedEnemy(p, 24); if (target == null) return false;
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int)(200 * power), 0));
                target.getPersistentData().putUUID("guildbound_mark", p.getUUID()); target.getPersistentData().putLong("guildbound_mark_until", now + (long)(200 * power));
            }
        }
        return true;
    }
    public static int compareGuard(int armor, int level, int otherArmor, int otherLevel) {
        int result = Integer.compare(armor, otherArmor); return result != 0 ? result : Integer.compare(level, otherLevel);
    }
    private static int profileLevel(ServerPlayer p) { return p.getData(ProgressAttachments.PROGRESS).totalLevel(); }
    private static List<Mob> enemies(ServerPlayer p, double radius) {
        return p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(radius), e -> e instanceof Enemy && e.isAlive()
                && !SummonService.owned(e) && !p.isAlliedTo(e) && p.distanceToSqr(e) <= radius * radius && p.hasLineOfSight(e));
    }
    private static Mob aimedEnemy(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition(), end = start.add(p.getLookAngle().scale(range));
        var wall = p.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = wall.getLocation();
        return enemies(p, range).stream().filter(m -> m.getBoundingBox().inflate(.25).clip(start, stop).isPresent()).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }
    private static boolean teleportToward(ServerPlayer p, Vec3 destination) {
        Vec3 start = p.getEyePosition();
        var hit = p.level().clip(new ClipContext(start, destination.add(0, p.getEyeHeight(), 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = hit.getLocation().subtract(0, p.getEyeHeight(), 0);
        Vec3 direction = end.subtract(p.position());
        double distance = direction.length();
        if (distance < 1) return false;
        direction = direction.normalize();
        for (double d = Math.max(0, distance - .6); d >= 1; d -= .5) {
            Vec3 point = p.position().add(direction.scale(d));
            var box = p.getBoundingBox().move(point.subtract(p.position()));
            if (p.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(point)) && p.level().getWorldBorder().isWithinBounds(box)
                    && p.level().noCollision(p, box) && !p.level().containsAnyLiquid(box)) { p.teleportTo(point.x, point.y, point.z); return true; }
        }
        return false;
    }
    private static void buff(ServerPlayer p, String key, int ticks) { p.getPersistentData().putLong("guildbound_" + key, p.level().getGameTime() + ticks); }
    private static boolean buffed(ServerPlayer p, String key) { return p.getPersistentData().getLong("guildbound_" + key) > p.level().getGameTime(); }
    @SubscribeEvent public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 5 != 0) return;
        var old = p.getData(ProgressAttachments.ABILITIES); var next = old.tick(5);
        if (!old.equals(next)) { p.setData(ProgressAttachments.ABILITIES, next); CombatEvents.sync(p); }
    }
    @SubscribeEvent public static void serverTick(ServerTickEvent.Post event) {
        ZONES.removeIf(z -> z.level.getGameTime() >= z.expiry || z.level.getServer().getPlayerList().getPlayer(z.owner) == null);
        var triggered = new HashSet<Zone>();
        for (var z : ZONES) {
            if (z.level.getGameTime() % 20 != 0) continue;
            var p = z.level.getServer().getPlayerList().getPlayer(z.owner);
            if (p == null || !p.isAlive() || p.level() != z.level || !z.ability.available(p.getData(ProgressAttachments.PROGRESS))) continue;
            double radius = z.ability == HeroAbility.TRAP ? 2 : z.ability == HeroAbility.TRIPWIRE ? 5 : 7;
            z.level.sendParticles(z.ability == HeroAbility.TRAP ? ParticleTypes.CRIT : ParticleTypes.END_ROD, z.center.x, z.center.y + .2, z.center.z, 30, radius / 2, .1, radius / 2, .01);
            for (var target : z.level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(radius), e -> e.isAlive() && e.distanceToSqr(z.center) <= radius * radius)) {
                if (z.ability == HeroAbility.TRIPWIRE) {
                    var delta = target.position().subtract(z.center);
                    double along = delta.dot(z.heading);
                    if (target instanceof Enemy && !SummonService.owned(target) && along >= 0 && along <= 5
                            && delta.subtract(z.heading.scale(along)).lengthSqr() < 1) {
                        target.hurt(p.damageSources().playerAttack(p), 5 * z.power);
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int)(100 * z.power), 2));
                        triggered.add(z);
                    }
                } else if (z.ability == HeroAbility.TRAP) {
                    if (target instanceof Enemy && !SummonService.owned(target)) { target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3)); target.hurt(p.damageSources().playerAttack(p), z.power); }
                } else if (z.ability == HeroAbility.HOLY_FIELD && (target instanceof Enemy || target.isInvertedHealAndHarm())) {
                    if (!SummonService.owned(target)) target.hurt(p.damageSources().indirectMagic(p, p), 2 * z.power);
                } else if (!(target instanceof Enemy) && !target.isInvertedHealAndHarm()) {
                    target.heal((z.ability == HeroAbility.HOLY_FIELD ? 1 : 1.5F) * z.power * ClassEquipment.healingMultiplier(p));
                    target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0));
                }
            }
        }
        ZONES.removeAll(triggered);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { ZONES.clear(); FIELDS.clear(); }
    @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer p && buffed(p, "vanish")) event.setCanceled(true);
    }
    @SubscribeEvent public static void visibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && ClassEquipment.subclass(p) == HeroSubclass.GUARDIAN && buffed(p, "taunt_notice")) event.modifyVisibility(1.5);
    }
    @SubscribeEvent public static void mobTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || mob.tickCount % 5 != 0) return;
        var data = mob.getPersistentData();
        if (data.getLong("guildbound_taunt_until") <= level.getGameTime() || !data.hasUUID("guildbound_taunt")) return;
        var p = level.getServer().getPlayerList().getPlayer(data.getUUID("guildbound_taunt"));
        if (p != null && p.isAlive() && p.level() == level && ClassEquipment.subclass(p) == HeroSubclass.GUARDIAN && mob.distanceToSqr(p) < 900) mob.setTarget(p);
    }
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        float amount = event.getAmount();
        if (event.getSource().getEntity() instanceof ServerPlayer p && p.isAlive()) {
            var sub = ClassEquipment.subclass(p);
            boolean melee = event.getSource().getDirectEntity() == p;
            if (melee && sub == HeroSubclass.BERSERKER && buffed(p, "rage")) {
                int combo = p.getPersistentData().getLong("guildbound_last_hit") + 100 >= p.level().getGameTime() ? Math.min(10, p.getPersistentData().getInt("guildbound_combo") + 1) : 1;
                p.getPersistentData().putInt("guildbound_combo", combo); p.getPersistentData().putLong("guildbound_last_hit", p.level().getGameTime());
                amount *= 1.2F + .3F * (1 - p.getHealth() / p.getMaxHealth()) + combo * .02F;
            }
            if (melee && sub == HeroSubclass.RUNE_WARRIOR && buffed(p, "rune")) amount += 3 * HeroAbility.RUNE_BLADE.power(p.getData(ProgressAttachments.TALENTS).rank(Talent.RUNE_BLADE));
            if (melee && sub == HeroSubclass.POISONER && buffed(p, "toxin")) {
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                amount *= 1 + .1F * HeroAbility.TOXIC_BLADE.power(p.getData(ProgressAttachments.TALENTS).rank(Talent.TOXIC_BLADE));
            }
            if (melee && sub == HeroSubclass.SHADOW && buffed(p, "vanish")) { amount *= 1.5F; p.getPersistentData().remove("guildbound_vanish"); p.removeEffect(MobEffects.INVISIBILITY); }
            if (melee && sub == HeroSubclass.DUELIST && buffed(p, "riposte")) amount *= 1.3F;
            if (sub == HeroSubclass.WILD_HUNTER && SummonService.nearby(p).stream().anyMatch(m -> m.distanceToSqr(event.getEntity()) < 16)) amount *= 1.15F;
            var data = event.getEntity().getPersistentData();
            if (data.hasUUID("guildbound_mark") && data.getUUID("guildbound_mark").equals(p.getUUID()) && data.getLong("guildbound_mark_until") > p.level().getGameTime()) amount *= 1.2F;
        }
        if (event.getEntity() instanceof ServerPlayer p) {
            if (ClassEquipment.subclass(p) == HeroSubclass.BERSERKER && buffed(p, "rage")) amount *= 1.2F;
            if (ClassEquipment.subclass(p) == HeroSubclass.DUELIST && buffed(p, "riposte")) amount *= .8F;
        }
        event.setAmount(amount);
    }
}
