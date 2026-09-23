package dev.guildbound.combat;

/** Shared by server casting and client descriptions. One rank costs one talent point. */
public final class CantripBalance {
    private CantripBalance() {}
    public static float damage(int mageLevel, int rank) {
        validate(rank);
        return 5 + rank + (mageLevel >= 5 ? 1.5F : 0);
    }
    public static int cooldown(int rank) {
        validate(rank);
        return 50 - rank * 10;
    }
    private static void validate(int rank) {
        if (rank < 0 || rank > 2) throw new IllegalArgumentException("Invalid cantrip rank");
    }
}
