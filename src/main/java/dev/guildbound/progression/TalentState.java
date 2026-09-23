package dev.guildbound.progression;

import java.util.EnumMap;
import java.util.Map;

/** Immutable purchases; unspent points derive from progress, never from a client counter. */
public record TalentState(Map<Talent, Integer> ranks) {
    public TalentState {
        ranks = Map.copyOf(ranks);
        for (int rank : ranks.values()) {
            if (rank < 1 || rank > 2) throw new IllegalArgumentException("Invalid talent rank");
        }
    }
    public static TalentState empty() { return new TalentState(Map.of()); }
    public int rank(Talent talent) { return ranks.getOrDefault(talent, 0); }
    public int spent() { return ranks.entrySet().stream().mapToInt(e -> java.util.stream.IntStream.rangeClosed(1, e.getValue()).map(e.getKey()::cost).sum()).sum(); }
    public int available(CharacterProgress progress) { return Math.max(0, TalentPoints.total(progress.totalLevel()) - spent()); }
    public int effectiveRank(Talent talent) {
        return rank(talent) + switch (talent) {
            case MARKSMAN -> rank(Talent.HUNTER_TRAINING);
            case KNIFEWORK -> rank(Talent.DUELIST_TRAINING);
            case BULWARK -> rank(Talent.GUARDIAN_TRAINING);
            case RESTORATION -> rank(Talent.SORCERER_TRAINING) + rank(Talent.LIFE_II) + rank(Talent.LIFE_III) + rank(Talent.LIFE_IV);
case VITALITY -> rank(Talent.HEART_II) + rank(Talent.HEART_III) + rank(Talent.HEART_IV);
case RESILIENCE -> rank(Talent.GUARD_II) + rank(Talent.GUARD_III) + rank(Talent.GUARD_IV);
case ENDURANCE -> rank(Talent.BREATH_II) + rank(Talent.BREATH_III) + rank(Talent.BREATH_IV);
case MEDITATION -> rank(Talent.MIND_II) + rank(Talent.MIND_III) + rank(Talent.MIND_IV) + rank(Talent.SORC_MIND) + rank(Talent.SORC_MIND_II) + rank(Talent.SORC_MIND_III);
            default -> 0;
        };
    }

    public boolean canBuy(CharacterProgress progress, Talent talent, int expectedSpent) {
        if (available(progress) < talent.cost(Math.min(2,rank(talent)+1))) return false;
        return canStudy(progress,talent,expectedSpent);
    }

    public boolean canStudy(CharacterProgress progress, Talent talent, int expectedSpent) {
        if (talent == Talent.SPELL_WEAVING) return false; // Reserved branch, never charges points for unfinished content.
        if (!progress.registered() || expectedSpent != spent() || rank(talent) == 2) return false;
        if (talent.prerequisite() != null && rank(talent.prerequisite()) == 0) return false;
        if (talent.subclass() != null && progress.tracks().stream().noneMatch(t -> t.subclass() == talent.subclass())) return false;
        int level = talent.heroClass() == null ? progress.totalLevel() : progress.tracks().stream()
                .filter(t -> t.heroClass() == talent.heroClass()).mapToInt(ClassTrack::level).findFirst().orElse(0);
        return level >= talent.requiredLevel(rank(talent) + 1);
    }

    /** A replay of a request from an older snapshot cannot purchase a second rank. */
    public TalentState buy(CharacterProgress progress, Talent talent, int expectedSpent) {
        if (!canBuy(progress, talent, expectedSpent)) return this;
        var next = new EnumMap<Talent, Integer>(Talent.class);
        next.putAll(ranks);
        next.put(talent, rank(talent) + 1);
        return new TalentState(next);
    }
}
