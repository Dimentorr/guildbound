package dev.guildbound.world;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/** Material stats are base values, before player effects and class training. */
public final class DaggerItem extends SwordItem {
    public DaggerItem(Tiers tier, int damage, float speed) {
        super(tier, properties(tier, damage, speed));
    }

    private static Item.Properties properties(Tiers tier, int damage, float speed) {
        var properties = new Item.Properties().attributes(SwordItem.createAttributes(tier,
                damage - 1 - tier.getAttackDamageBonus(), speed - 4));
        return tier == Tiers.NETHERITE ? properties.fireResistant() : properties;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target,
                              net.minecraft.world.entity.LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, stack == attacker.getOffhandItem() ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ability != ItemAbilities.SWORD_SWEEP && super.canPerformAction(stack, ability);
    }
}
