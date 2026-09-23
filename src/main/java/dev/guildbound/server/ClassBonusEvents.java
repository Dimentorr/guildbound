package dev.guildbound.server;

import dev.guildbound.Guildbound;
import dev.guildbound.combat.ClassBonuses;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.Talent;
import dev.guildbound.progression.HeroClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Guildbound.ID)
public final class ClassBonusEvents {
    public static final TagKey<Item> HEAVY_ARMOR = TagKey.create(Registries.ITEM, id("heavy_armor"));
    public static final TagKey<Item> DAGGERS = TagKey.create(Registries.ITEM, id("daggers"));
    private static final ResourceLocation SPEED = id("class_speed");
    private static final ResourceLocation ARMOR = id("heavy_armor_speed");
    private static final ResourceLocation ATTACK = id("class_attack_speed");
    private static final ResourceLocation HEALTH = id("talent_health");

    private ClassBonusEvents() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Guildbound.ID, path); }
    private static boolean active(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator()
                && player.getData(ProgressAttachments.PROGRESS).registered();
    }

    /** Stable transient IDs: no stacking after reconnect, respawn or equipment changes. */
    private static void modifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        modifier(attribute, id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void modifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        var previous = attribute.getModifier(id);
        if (amount == 0) {
            if (previous != null) attribute.removeModifier(id);
        } else if (previous == null || previous.amount() != amount) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount,
                    operation));
        }
    }

    private static void updateHealth(ServerPlayer player) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        double amount = active(player) ? 2 * player.getData(ProgressAttachments.TALENTS).effectiveRank(Talent.VITALITY) + dev.guildbound.combat.LevelStats.healthBonus(player.getData(ProgressAttachments.PROGRESS).totalLevel()) : 0;
        var previous = attribute.getModifier(HEALTH);
        if (amount == 0) {
            if (previous != null) attribute.removeModifier(HEALTH);
        } else if (previous == null || previous.amount() != amount) {
            // Persist the maximum too: vanilla loads/clamps saved health before login events.
            attribute.addOrReplacePermanentModifier(new AttributeModifier(HEALTH, amount, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updateHealth(player);
            if (!event.isEndConquered()) player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        refresh(player);
    }

    public static void refresh(ServerPlayer player) {
        var progress = player.getData(ProgressAttachments.PROGRESS);
        boolean enabled = active(player);
        var talents = player.getData(ProgressAttachments.TALENTS);
        updateHealth(player);
        int heavy = 0;
        for (var stack : player.getArmorSlots()) if (stack.is(HEAVY_ARMOR)) heavy++;
        var weapon = player.getMainHandItem();
        boolean trainedWeapon = weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES) || weapon.is(Items.MACE);
        double setSpeed = dev.guildbound.world.ClassEquipment.fullSet(player, HeroClass.RANGER)
                || dev.guildbound.world.ClassEquipment.fullSet(player, HeroClass.ROGUE) ? .05 : 0;
        modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, enabled ? ClassBonuses.speed(progress) + setSpeed : 0);
        modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), ARMOR, enabled ? -ClassBonuses.armorPenalty(progress, heavy) : 0);
        double knifeBonus = weapon.is(DAGGERS) && ClassBonuses.level(progress, HeroClass.ROGUE) > 0
                ? .05 * talents.effectiveRank(Talent.KNIFEWORK) : 0;
        modifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK,
                enabled ? ClassBonuses.attackSpeed(progress, weapon.is(DAGGERS), trainedWeapon) + knifeBonus : 0);
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        float damage = event.getAmount();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && active(attacker)
                && event.getSource().getDirectEntity() instanceof AbstractArrow arrow
                && arrow.getWeaponItem() != null && arrow.getWeaponItem().getItem() instanceof BowItem) {
            var progress = attacker.getData(ProgressAttachments.PROGRESS);
            double talentBonus = ClassBonuses.level(progress, HeroClass.RANGER) > 0
                    ? .05 * attacker.getData(ProgressAttachments.TALENTS).effectiveRank(Talent.MARKSMAN) : 0;
            damage *= (float) (1 + ClassBonuses.bowDamage(progress) + talentBonus
                    + (arrow.getWeaponItem().is(dev.guildbound.world.ClassEquipment.RANGER_BOW.get()) ? .1 : 0));
        }
        // Combat protection does not reduce falls, drowning, void damage or /kill.
        if (event.getEntity() instanceof ServerPlayer target && active(target)
                && event.getSource().getEntity() != null && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            var progress = target.getData(ProgressAttachments.PROGRESS);
            double talentBonus = ClassBonuses.level(progress, HeroClass.WARRIOR) > 0
                    ? .03 * target.getData(ProgressAttachments.TALENTS).effectiveRank(Talent.BULWARK) : 0;
            damage *= (float) (1 - ClassBonuses.resistance(progress) - talentBonus
                    - .02 * target.getData(ProgressAttachments.TALENTS).effectiveRank(Talent.RESILIENCE)
                    - (dev.guildbound.world.ClassEquipment.fullSet(target, HeroClass.WARRIOR) ? .05 : 0));
        }
        event.setAmount(damage);
    }

    @SubscribeEvent
    public static void visibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player) && player.isShiftKeyDown()) {
            event.modifyVisibility(1 - ClassBonuses.concealment(player.getData(ProgressAttachments.PROGRESS)));
        }
    }
}
