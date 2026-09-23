package dev.guildbound.progression;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionV2Test {
    private static CharacterProgress at(int level) {
        return new CharacterProgress(List.of(new ClassTrack(HeroClass.MAGE, level)), 0);
    }
    @Test void baseRegistrationPreservesAbilitiesButHasNoSubclass() {
        for (var hero : HeroClass.values()) {
            var p = CharacterProgress.empty().register(hero);
            assertEquals(hero, p.tracks().getFirst().heroClass());
            assertNull(p.tracks().getFirst().subclass());
            assertEquals(5, TalentState.empty().available(p));
            assertTrue(TalentState.empty().canBuy(p, Talent.VITALITY, 0));
        }
    }
    @Test void specializationRequiresOwnClassLevelFiveAndIsOneTime() {
        assertEquals(at(4), at(4).specialize(HeroSubclass.SORCERER));
        assertEquals(at(5), at(5).specialize(HeroSubclass.HUNTER));
        var chosen = at(5).specialize(HeroSubclass.SORCERER);
        assertEquals(HeroSubclass.SORCERER, chosen.tracks().getFirst().subclass());
        assertSame(chosen, chosen.specialize(HeroSubclass.SORCERER));
        assertEquals(HeroSubclass.SORCERER, chosen.awardExperience(756).tracks().getFirst().subclass());
        assertNull(chosen.rebuild(HeroClass.MAGE).tracks().getFirst().subclass());
    }
    @Test void pointsAtEveryTierBoundaryAndNeverRepeatOnRebuild() {
        int[] levels = {0, 1, 5, 6, 10, 11, 20, 21, 45, 46, 50, 74, 75, 89, 90, 99, 100};
        int[] budgets = {0, 5, 9, 11, 19, 22, 49, 54, 174, 184, 224, 464, 484, 764, 789, 1014, 1064};
        for (int i = 0; i < levels.length; i++) assertEquals(budgets[i], TalentPoints.total(levels[i]), "level " + levels[i]);
        var p = at(10);
        for (int i = 0; i < 20; i++) p = p.rebuild(HeroClass.MAGE);
        assertEquals(5, TalentState.empty().available(p));
    }
    @Test void allRankBoundariesIncludingCap() {
        assertNull(GuildRank.atLevel(4));
        GuildRank previous = null;
        for (var rank : GuildRank.values()) {
            assertEquals(previous, GuildRank.atLevel(rank.level - 1));
            assertEquals(rank, GuildRank.atLevel(rank.level));
            previous = rank;
        }
        assertEquals(GuildRank.GRANDMASTER, GuildRank.atLevel(100));
        assertEquals(GuildRank.LEGEND, GuildRank.atLevel(at(100).rebuild(HeroClass.MAGE).totalLevel()));
    }
    @Test void xpCurveMonotonicAndExactForEveryLevel() {
        int previous = 0;
        for (int level = 1; level < 100; level++) {
            var p = at(level);
            int cost = p.nextLevelCost();
            assertTrue(cost > previous);
            assertEquals(level, p.awardExperience(cost - 1).totalLevel());
            assertEquals(level + 1, p.awardExperience(cost).totalLevel());
            previous = cost;
        }
        assertEquals(0, at(100).nextLevelCost());
        var legacy = new CharacterProgress(at(2).tracks(), 290).awardExperience(0);
        assertEquals(3, legacy.totalLevel());
        assertEquals(65, legacy.experience());
    }
    @Test void connectionsAndSpecializationCannotBeBypassedByPackets() {
        var p = at(7);
        var empty = TalentState.empty();
        assertFalse(empty.canBuy(p, Talent.RESILIENCE, 0));
        var common = empty.buy(p, Talent.VITALITY, 0);
        assertTrue(common.canBuy(p, Talent.RESILIENCE, 1));
        var state = empty.buy(p, Talent.RESTORATION, 0);
        assertFalse(state.canBuy(p, Talent.SORCERER_TRAINING, 1));
        p = p.specialize(HeroSubclass.SORCERER);
        state = state.buy(p, Talent.SORCERER_TRAINING, 1);
        assertEquals(2, state.effectiveRank(Talent.RESTORATION));
        assertSame(state, state.buy(p, Talent.SORCERER_TRAINING, 1));
        assertFalse(state.canBuy(p, Talent.HUNTER_TRAINING, state.spent()));
        assertEquals(3, new TalentState(Map.of(Talent.VITALITY, 2)).spent());
    }
}
