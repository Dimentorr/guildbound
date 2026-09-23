package dev.guildbound.server;

import dev.guildbound.combat.*;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.data.ResourceAttachments;
import dev.guildbound.progression.HeroClass;
import dev.guildbound.progression.Talent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class HealingService {
    private HealingService() {}
    public static boolean cast(ServerPlayer caster) {
        var progress = caster.getData(ProgressAttachments.PROGRESS);
        var resources = caster.getData(ResourceAttachments.RESOURCES);
        if (!dev.guildbound.world.ClassEquipment.magicFocus(caster) || !caster.isAlive() || caster.isSpectator() || caster.isCreative() || caster.isSleeping()
                || caster.isPassenger() || caster.isUsingItem() || ClassBonuses.level(progress, HeroClass.MAGE) == 0
                || !resources.canHeal() || CantripCooldown.remaining(resources, caster.getData(ProgressAttachments.FIRE_COOLDOWN)) > 0)
            return false;
        var target = HealingTarget.find(caster);
        if (!target.isAlive() || target.getHealth() >= target.getMaxHealth()) return false;
        // Reserve resources before healing hooks; a repeat request cannot spend/cast twice.
        caster.setData(ResourceAttachments.RESOURCES, resources.heal());
        target.heal((ClassBonuses.healing(progress) + caster.getData(ProgressAttachments.TALENTS).effectiveRank(Talent.RESTORATION)) * dev.guildbound.world.ClassEquipment.healingMultiplier(caster));
        caster.serverLevel().sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1,
                target.getZ(), 4, .3, .4, .3, 0);
        caster.displayClientMessage(Component.translatable("message.guildbound.healed_target", target.getDisplayName()), true);
        if (target instanceof ServerPlayer recipient && target != caster)
            recipient.displayClientMessage(Component.translatable("message.guildbound.healed_by", caster.getDisplayName()), true);
        CombatEvents.sync(caster);
        return true;
    }
}

