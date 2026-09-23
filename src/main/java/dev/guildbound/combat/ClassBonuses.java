package dev.guildbound.combat;

import dev.guildbound.progression.CharacterProgress;
import dev.guildbound.progression.HeroClass;

/** Initial class balance. Uses the level of each class, never the combined level. */
public final class ClassBonuses {
    private ClassBonuses() {}

    public static int level(CharacterProgress progress, HeroClass heroClass) {
        return progress.tracks().stream().filter(t -> t.heroClass() == heroClass)
                .mapToInt(t -> t.level()).findFirst().orElse(0);
    }

    private static double bonus(int level, double base, double advanced) {
        return level == 0 ? 0 : level >= 3 ? advanced : base;
    }

    public static double speed(CharacterProgress progress) {
        return Math.max(bonus(level(progress, HeroClass.RANGER), .10, .15),
                bonus(level(progress, HeroClass.ROGUE), .15, .20));
    }

    public static double bowDamage(CharacterProgress progress) {
        return bonus(level(progress, HeroClass.RANGER), .15, .25);
    }

    public static double resistance(CharacterProgress progress) {
        return bonus(level(progress, HeroClass.WARRIOR), .10, .15);
    }

    public static double weaponSpeed(CharacterProgress progress) {
        return bonus(level(progress, HeroClass.WARRIOR), .15, .20);
    }

    public static double attackSpeed(CharacterProgress progress, boolean dagger, boolean warriorWeapon) {
        // A dagger tagged as a sword still uses only the rogue specialization.
        if (dagger) return bonus(level(progress, HeroClass.ROGUE), .20, .30);
        return warriorWeapon ? weaponSpeed(progress) : 0;
    }

    public static double concealment(CharacterProgress progress) {
        return Math.max(bonus(level(progress, HeroClass.RANGER), .15, .25),
                bonus(level(progress, HeroClass.ROGUE), .25, .35));
    }

    public static double armorPenalty(CharacterProgress progress, int heavyPieces) {
        if (!progress.registered() || level(progress, HeroClass.WARRIOR) > 0) return 0;
        return .04 * Math.clamp(heavyPieces, 0, 4);
    }

    public static float healing(CharacterProgress progress) {
        return level(progress, HeroClass.MAGE) >= 3 ? 5 : 4;
    }

    public static boolean canDodgeFrom(CharacterProgress progress, boolean onGround) {
        return progress.registered() && (onGround || level(progress, HeroClass.ROGUE) > 0 || level(progress, HeroClass.RANGER) > 0);
    }
}
