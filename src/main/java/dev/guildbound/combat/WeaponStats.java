package dev.guildbound.combat;

import dev.guildbound.Guildbound;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.HeroClass;
import dev.guildbound.progression.Talent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Same calculation for hovered weapons and actual attacks, without mutating equipment. */
public final class WeaponStats {
    public static final ResourceLocation CLASS_SPEED = ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "class_attack_speed");
    public static final TagKey<Item> DAGGERS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "daggers"));
    private WeaponStats() {}

    public static double classSpeed(Player player, ItemStack stack) {
        if (player.isCreative() || player.isSpectator()) return 0;
        var progress = player.getData(ProgressAttachments.PROGRESS);
        boolean dagger = stack.is(DAGGERS);
        double bonus = ClassBonuses.attackSpeed(progress, dagger,
                stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.MACE));
        if (dagger && ClassBonuses.level(progress, HeroClass.ROGUE) > 0)
            bonus += .05 * player.getData(ProgressAttachments.TALENTS).effectiveRank(Talent.KNIFEWORK);
        return bonus;
    }

    public static double value(Player player, ItemStack stack, Holder<Attribute> attribute) {
        var current = player.getAttribute(attribute);
        if (current == null) return 0;
        var preview = new AttributeInstance(attribute, ignored -> {});
        preview.setBaseValue(current.getBaseValue());
        for (var modifier : current.getModifiers()) preview.addTransientModifier(modifier);
        player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (type, modifier) -> {
            if (type.equals(attribute)) preview.removeModifier(modifier.id());
        });
        if (attribute.equals(Attributes.ATTACK_SPEED)) preview.removeModifier(CLASS_SPEED);
        stack.forEachModifier(EquipmentSlot.MAINHAND, (type, modifier) -> {
            if (type.equals(attribute)) preview.addOrUpdateTransientModifier(modifier);
        });
        if (attribute.equals(Attributes.ATTACK_SPEED)) preview.addOrUpdateTransientModifier(
                new AttributeModifier(CLASS_SPEED, classSpeed(player, stack), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return preview.getValue();
    }
}
