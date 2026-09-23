package dev.guildbound.progression;

/** Stable order is part of the version 1 journal format. */
public enum GuildContract {
    PATROL(5, 100, 2), RESTLESS_DEAD(6, 150, 3), BONE_HUNT(4, 200, 4);
    public final int target, experience, emeralds;
    GuildContract(int target, int experience, int emeralds) {
        this.target = target; this.experience = experience; this.emeralds = emeralds;
    }
    public String key() { return "quest.guildbound." + name().toLowerCase(java.util.Locale.ROOT); }
}

