package dev.guildbound.combat;

import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.HeroClass;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;

/** Shared eligibility for client movement and server stamina accounting. */
public final class ShieldCombat {
    private ShieldCombat() {}

    public static boolean raisedShield(Player player) {
        return player.isUsingItem() && player.getUseItem().getUseAnimation() == UseAnim.BLOCK;
    }

    public static boolean mobileGuard(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator()
                && !player.isPassenger() && !player.isSleeping() && raisedShield(player)
                && ClassBonuses.level(player.getData(ProgressAttachments.PROGRESS), HeroClass.WARRIOR) > 0;
    }
}
