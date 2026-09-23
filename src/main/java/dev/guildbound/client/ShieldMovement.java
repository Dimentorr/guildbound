package dev.guildbound.client;

import dev.guildbound.combat.ShieldCombat;
import dev.guildbound.data.ResourceAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

public final class ShieldMovement {
    private ShieldMovement() {}

    public static void update(MovementInputUpdateEvent event) {
        var player = event.getEntity();
        if (!ShieldCombat.mobileGuard(player)) return;
        var input = event.getInput();
        // NeoForge fires this immediately BEFORE LocalPlayer's 0.2 item-use slowdown.
        // Undo only that factor; preserve sneak and any preceding input modifiers.
        boolean wantsSprint = player.isSprinting() || Minecraft.getInstance().options.keySprint.isDown();
        boolean canSprint = input.forwardImpulse >= .8F && !input.shiftKeyDown
                && player.getFoodData().getFoodLevel() > 6 && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isInWaterOrBubble() && !player.isInLava() && !player.isFallFlying()
                && !player.getAbilities().flying && !player.horizontalCollision
                && player.getData(ResourceAttachments.RESOURCES).canSprint(true);
        player.setSprinting(wantsSprint && canSprint);
        input.leftImpulse *= 5;
        input.forwardImpulse *= 5;
    }
}
