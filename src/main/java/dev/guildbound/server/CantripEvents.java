package dev.guildbound.server;

import dev.guildbound.Guildbound;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.HeroClass;
import dev.guildbound.combat.ClassBonuses;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Guildbound.ID)
public final class CantripEvents {
    private CantripEvents() {}
    public static boolean cast(ServerPlayer player) {
        int level = ClassBonuses.level(player.getData(ProgressAttachments.PROGRESS), HeroClass.MAGE);
        if (!dev.guildbound.world.ClassEquipment.magicFocus(player) || level == 0 || !player.isAlive() || player.isCreative() || player.isSpectator()
                || player.isSleeping() || player.isPassenger() || player.isUsingItem()
                || dev.guildbound.combat.CantripCooldown.remaining(player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES),
                        player.getData(ProgressAttachments.FIRE_COOLDOWN)) > 0) return false;
        var bolt = Guildbound.EMBER_BOLT.get().create(player.serverLevel());
        if (bolt == null) return false;
        bolt.setOwner(player);
        bolt.setPos(player.getX(), player.getEyeY() - .1, player.getZ());
        int rank = player.getData(ProgressAttachments.TALENTS).rank(dev.guildbound.progression.Talent.EMBER);
        bolt.setDamage(dev.guildbound.combat.CantripBalance.damage(level, rank) * (dev.guildbound.world.ClassEquipment.subclass(player) == dev.guildbound.progression.HeroSubclass.WIZARD ? 1.15F : 1));
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5F, 0);
        if (!player.serverLevel().addFreshEntity(bolt)) return false;
        player.setData(ProgressAttachments.FIRE_COOLDOWN, dev.guildbound.combat.CantripBalance.cooldown(rank));
        player.serverLevel().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BLAZE_SHOOT,
                player.getSoundSource(), .5F, 1.4F);
        CombatEvents.sync(player);
        return true;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int remaining = player.getData(ProgressAttachments.FIRE_COOLDOWN);
        if (remaining > 0) {
            player.setData(ProgressAttachments.FIRE_COOLDOWN, remaining - 1);
            if (remaining % 5 == 1) CombatEvents.sync(player);
        }
    }
}
