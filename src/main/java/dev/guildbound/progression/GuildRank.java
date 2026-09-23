package dev.guildbound.progression;

import java.util.Locale;

/** Derived from total RPG level: cannot become stale after a rebuild or migration. */
public enum GuildRank {
    RECRUIT(5, 0xFFB8AA8A), EXPERIENCED(10, 0xFF72997A),
    BRONZE(15, 0xFFCD8D52), IRON(25, 0xFF9DA6AD), SILVER(35, 0xFFD5E3E8),
    GOLD(50, 0xFFF0C866), PLATINUM(65, 0xFF9DE1DD),
    LEGEND(80, 0xFFC094F0), GRANDMASTER(100, 0xFFFF886C);

    public final int level;
    public final int color;
    GuildRank(int level, int color) { this.level = level; this.color = color; }
    public String key() { return "rank.guildbound." + name().toLowerCase(Locale.ROOT); }
    public static GuildRank atLevel(int level) {
        GuildRank result = null;
        for (var rank : values()) if (level >= rank.level) result = rank;
        return result;
    }
}
