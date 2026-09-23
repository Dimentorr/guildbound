package dev.guildbound.client;

import dev.guildbound.combat.WeaponStats;
import dev.guildbound.world.DaggerItem;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BowItem;
import dev.guildbound.combat.ClassBonuses;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.Talent;
import dev.guildbound.progression.HeroClass;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class WeaponTooltips {
    private WeaponTooltips() {}
    public static void tooltip(ItemTooltipEvent event) {
        var player = event.getEntity();
        var stack = event.getItemStack();
        for (var entry : dev.guildbound.world.ClassEquipment.ITEMS.entrySet()) if (stack.is(entry.getValue().get())) {
            event.getToolTip().add(Component.translatable("tooltip.guildbound.equipment." + entry.getKey().split("_")[0]).withStyle(ChatFormatting.GRAY));
            break;
        }
        if (player != null && stack.getItem() instanceof BowItem) {
            var progress = player.getData(ProgressAttachments.PROGRESS);
            double bonus = player.isCreative() || player.isSpectator() ? 0 : ClassBonuses.bowDamage(progress)
                    + (ClassBonuses.level(progress, HeroClass.RANGER) > 0
                    ? .05 * player.getData(ProgressAttachments.TALENTS).effectiveRank(Talent.MARKSMAN) : 0)
                    + (stack.is(dev.guildbound.world.ClassEquipment.RANGER_BOW.get()) ? .1 : 0);
            event.getToolTip().add(Component.translatable("tooltip.guildbound.bow_bonus", Math.round(bonus * 100)).withStyle(ChatFormatting.GOLD));
        }
        if (player == null || !(stack.is(WeaponStats.DAGGERS) || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES) || stack.is(Items.MACE))) return;
        var lines = event.getToolTip();
        lines.add(Component.translatable("tooltip.guildbound.effective_speed",
                String.format(Locale.ROOT, "%.2f", WeaponStats.value(player, stack, Attributes.ATTACK_SPEED))).withStyle(ChatFormatting.GOLD));
        if (stack.getItem() instanceof DaggerItem) {
            lines.add(Component.translatable("tooltip.guildbound.dual").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        var minecraft = Minecraft.getInstance();
        if (event.isAttack() && minecraft.player != null
                && (minecraft.player.getMainHandItem().getItem() instanceof DaggerItem
                    || minecraft.player.getOffhandItem().getItem() instanceof DaggerItem)
                && minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity) {
            // The server broadcasts the hand that really attacked; don't show a fake main-hand swing.
            event.setSwingHand(false);
        }
    }
}
