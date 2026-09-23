package dev.guildbound.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

/** Five rank-locked contracts per overworld day. Older clocks cannot reroll a claimed board. */
public record DailyJournal(long day, int tier, int accepted, int claimed, List<Integer> kills) {
    public static final Codec<DailyJournal> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("day").forGetter(DailyJournal::day), Codec.INT.fieldOf("tier").forGetter(DailyJournal::tier),
            Codec.INT.fieldOf("accepted").forGetter(DailyJournal::accepted), Codec.INT.fieldOf("claimed").forGetter(DailyJournal::claimed),
            Codec.INT.listOf().fieldOf("kills").forGetter(DailyJournal::kills)).apply(i, DailyJournal::new));
    public DailyJournal {
        kills = List.copyOf(kills);
        if (day < -1 || tier < 0 || tier > 9 || accepted < 0 || accepted > 31 || claimed < 0 || claimed > 31
                || (claimed & ~accepted) != 0 || kills.size() != 5 || kills.stream().anyMatch(n -> n < 0 || n > 1000))
            throw new IllegalArgumentException("Invalid daily journal");
    }
    public static DailyJournal empty() { return new DailyJournal(-1, 0, 0, 0, List.of(0, 0, 0, 0, 0)); }
    public DailyJournal refresh(long currentDay, int level) {
        if (currentDay <= day) return this;
        var rank = GuildRank.atLevel(level);
        return new DailyJournal(currentDay, rank == null ? 0 : rank.ordinal() + 1, 0, 0, List.of(0, 0, 0, 0, 0));
    }
    public String targetType(int slot) {
        check(slot);
        return switch (slot) {
            case 0 -> "hostile";
            case 1 -> tier >= 5 ? "blaze" : "zombie";
            case 2 -> tier >= 4 ? "pillager" : "skeleton";
            case 3 -> tier >= 3 ? "witch" : "spider";
            default -> tier >= 6 ? "wither_skeleton" : "creeper";
        };
    }
    public int target(int slot) { check(slot); return 4 + tier * 2 + slot; }
    public int experience(int slot) {
        check(slot);
        int level = tier == 0 ? 1 : GuildRank.values()[tier - 1].level;
        return (int) Math.round((100 + 12.0 * level * level) * (1 + slot * .15));
    }
    public int emeralds(int slot) { check(slot); return 2 + tier * 2 + slot; }
    public boolean active(int slot) { check(slot); return (accepted & 1 << slot) != 0 && !claimed(slot); }
    public boolean claimed(int slot) { check(slot); return (claimed & 1 << slot) != 0; }
    public boolean ready(int slot) { return active(slot) && kills.get(slot) >= target(slot); }
    public DailyJournal accept(int slot) {
        check(slot); if (day < 0 || (accepted & 1 << slot) != 0) return this;
        return new DailyJournal(day, tier, accepted | 1 << slot, claimed, kills);
    }
    public DailyJournal kill(String type, boolean hostile) {
        var next = new ArrayList<>(kills);
        for (int slot = 0; slot < 5; slot++) if (active(slot) && (targetType(slot).equals(type) || targetType(slot).equals("hostile") && hostile))
            next.set(slot, Math.min(target(slot), next.get(slot) + 1));
        return new DailyJournal(day, tier, accepted, claimed, next);
    }
    public DailyJournal claim(int slot) {
        return ready(slot) ? new DailyJournal(day, tier, accepted, claimed | 1 << slot, kills) : this;
    }
    private static void check(int slot) { if (slot < 0 || slot >= 5) throw new IllegalArgumentException("Invalid daily slot"); }
}
