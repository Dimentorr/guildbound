package dev.guildbound.progression;

/** Equipment milestones belong to the necromancer, independently of guild ranks. */
public final class UndeadEquipment {
    private UndeadEquipment() {}

    public static int tier(int necromancerLevel) {
        if (necromancerLevel >= 100) return 8;
        if (necromancerLevel >= 80) return 7;
        if (necromancerLevel >= 50) return 5;
        if (necromancerLevel >= 35) return 4;
        if (necromancerLevel >= 25) return 3;
        if (necromancerLevel >= 10) return 1;
        return 0;
    }
}
