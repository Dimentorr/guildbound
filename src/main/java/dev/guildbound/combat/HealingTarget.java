package dev.guildbound.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;

/** Same preview and authoritative server targeting; no client-supplied entity id. */
public final class HealingTarget {
    public static final double RANGE = 12;
    private HealingTarget() {}
    public static Player find(Player caster) {
        if (caster.isShiftKeyDown()) return caster;
        var start = caster.getEyePosition();
        var direction = caster.getLookAngle().scale(RANGE);
        var end = caster.level().clip(new ClipContext(start, start.add(direction),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(caster, start, end,
                caster.getBoundingBox().expandTowards(direction).inflate(1),
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity.isPickable() && !entity.isSpectator(),
                start.distanceToSqr(end));
        return hit != null && hit.getEntity() instanceof Player target ? target : caster;
    }
}

