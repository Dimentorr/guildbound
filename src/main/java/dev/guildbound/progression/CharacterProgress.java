package dev.guildbound.progression;

import java.util.List;
import java.util.HashSet;

/** Immutable, engine-independent progression. Experience is progress toward the next level. */
public record CharacterProgress(List<ClassTrack> tracks, int experience) {
    public static final int MAX_LEVEL = 100;

    public CharacterProgress {
        tracks = List.copyOf(tracks);
        var seen = new HashSet<HeroClass>();
        int total = 0;
        for (var track : tracks) {
            if (!seen.add(track.heroClass())) {
                throw new IllegalArgumentException("Duplicate class");
            }
            total += track.level();
        }
        if (total > MAX_LEVEL || experience < 0 || (tracks.isEmpty() && experience != 0)) {
            throw new IllegalArgumentException("Invalid progression");
        }
    }

    public static CharacterProgress empty() { return new CharacterProgress(List.of(), 0); }
    public boolean registered() { return !tracks.isEmpty(); }
    public int totalLevel() { return tracks.stream().mapToInt(ClassTrack::level).sum(); }
    public int nextLevelCost() { return costAfter(totalLevel()); }
    private static int costAfter(int level) {
        if (level < 1 || level >= MAX_LEVEL) return 0;
        return switch (level) {
            case 1 -> 100;
            case 2 -> 225;
            case 3 -> 338;
            case 4 -> 525;
            default -> 21 * (level + 1) * (level + 1);
        };
    }

    public CharacterProgress register(HeroSubclass subclass) {
        return register(subclass.parent());
    }

    public CharacterProgress register(HeroClass heroClass) {
        if (registered()) throw new IllegalStateException("Already registered");
        return new CharacterProgress(List.of(new ClassTrack(heroClass, 1)), 0);
    }

    /** Prototype rebuild: lose one level (floor 1), discard progress toward the next. */
    public CharacterProgress rebuild(HeroSubclass subclass) {
        return rebuild(subclass.parent());
    }

    public CharacterProgress rebuild(HeroClass heroClass) {
        if (tracks.size() != 1) throw new IllegalStateException("Rebuild requires one registered class");
        return new CharacterProgress(List.of(new ClassTrack(heroClass, Math.max(1, totalLevel() - 1))), 0);
    }

    public CharacterProgress specialize(HeroSubclass subclass) {
        var next = new java.util.ArrayList<>(tracks);
        for (int i = 0; i < next.size(); i++) {
            var track = next.get(i);
            if (track.heroClass() == subclass.parent() && track.level() >= 5 && track.subclass() == null) {
                next.set(i, new ClassTrack(track.heroClass(), subclass, track.level()));
                return new CharacterProgress(next, experience);
            }
        }
        return this;
    }

    /** Prototype XP advances the existing class. Multiclass allocation is deliberately not exposed yet. */
    public CharacterProgress awardExperience(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative experience");
        if (!registered() || totalLevel() >= MAX_LEVEL) return this;
        if (tracks.size() != 1) throw new IllegalStateException("Multiclass allocation requires explicit selection");
        long remaining = (long) experience + amount;
        int level = totalLevel();
        while (level < MAX_LEVEL && remaining >= costAfter(level)) {
            remaining -= costAfter(level);
            level++;
        }
        var track = tracks.getFirst();
        return new CharacterProgress(List.of(new ClassTrack(track.heroClass(), track.subclass(), level)),
                level == MAX_LEVEL ? 0 : (int) remaining);
    }
}
