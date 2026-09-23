package dev.guildbound.world;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** A cosmetic flame with ordinary projectile damage: never ignites or explodes. */
public final class EmberBolt extends Snowball {
    private float damage = 5;
    public EmberBolt(EntityType<? extends Snowball> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }
    public void setDamage(float damage) { this.damage = damage; }
    @Override protected Item getDefaultItem() { return Items.FIRE_CHARGE; }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide || !(getOwner() instanceof ServerPlayer owner) || !owner.isAlive()) return;
        if (hit.getEntity() instanceof ServerPlayer victim
                && (!owner.server.isPvpAllowed() || !owner.canHarmPlayer(victim))) return;
        hit.getEntity().hurt(damageSources().thrown(this, owner), damage);
    }
    @Override public void handleEntityEvent(byte event) {
        if (event == 3) for (int i = 0; i < 8; i++)
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), (random.nextDouble() - .5) * .1, .03, (random.nextDouble() - .5) * .1);
        else super.handleEntityEvent(event);
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= 24) discard();
        if (level().isClientSide) level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0, 0);
    }
}
