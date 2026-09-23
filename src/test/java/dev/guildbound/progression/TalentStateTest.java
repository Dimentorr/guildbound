package dev.guildbound.progression;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TalentStateTest {
    private static CharacterProgress profile(HeroSubclass subclass, int level) {
        return new CharacterProgress(List.of(new ClassTrack(subclass, level)), 0);
    }

    @Test void studyPathRemainsAvailableWithoutPointsButPurchaseDoesNot() {
        var p=profile(HeroSubclass.SORCERER,1);
        var state=TalentState.empty().buy(p,Talent.ENDURANCE,0);
        state=state.buy(p,Talent.ENDURANCE,1).buy(p,Talent.MEDITATION,3).buy(p,Talent.VITALITY,4);
        assertEquals(0,state.available(p));
        assertTrue(state.canStudy(p,Talent.VITALITY,state.spent()));
        assertFalse(state.canBuy(p,Talent.VITALITY,state.spent()));
        assertFalse(state.canStudy(p,Talent.ENDURANCE,state.spent()));
        assertFalse(state.canStudy(p,Talent.RESTORATION,state.spent()));
    }

    @Test void existingCharactersReceiveLevelBasedPointsWithoutReplayingLevelUps() {
        assertEquals(0, TalentState.empty().available(CharacterProgress.empty()));
        assertEquals(5, TalentState.empty().available(profile(HeroSubclass.GUARDIAN, 1)));
        assertEquals(9, TalentState.empty().available(profile(HeroSubclass.GUARDIAN, 5)));
    }

    @Test void rejectsUnregisteredAndWrongClassPurchases() {
        var state = TalentState.empty();
        assertSame(state, state.buy(CharacterProgress.empty(), Talent.VITALITY, 0));
        var warrior = profile(HeroSubclass.GUARDIAN, 5);
        state = state.buy(warrior, Talent.VITALITY, 0);
        assertFalse(state.canBuy(warrior, Talent.MARKSMAN, 1));
        assertFalse(state.canBuy(warrior, Talent.RESTORATION, 1));
        assertTrue(state.canBuy(warrior, Talent.BULWARK, 1));
    }

    @Test void replayCannotSpendTwiceAndDifferentRequestWithSameSnapshotAlsoFails() {
        var profile = profile(HeroSubclass.DUELIST, 5);
        var state = TalentState.empty().buy(profile, Talent.VITALITY, 0);
        assertSame(state, state.buy(profile, Talent.VITALITY, 0));
        assertSame(state, state.buy(profile, Talent.ENDURANCE, 0));
        assertSame(state, state.buy(profile, Talent.ENDURANCE, -1));
        assertEquals(8, state.available(profile));
        assertEquals(2, state.buy(profile, Talent.ENDURANCE, 1).spent());
    }

    @Test void allFiveStartingPointsCanBeUsedInGeneralTree() {
        var p = profile(HeroSubclass.SORCERER, 1);
        var state = TalentState.empty().buy(p, Talent.ENDURANCE, 0);
        assertTrue(state.canBuy(p, Talent.ENDURANCE, 1));
        state = state.buy(p, Talent.ENDURANCE, 1).buy(p, Talent.MEDITATION, 3).buy(p, Talent.VITALITY, 4);
        assertEquals(5, state.spent());
        assertEquals(0, state.available(p));
    }

    @Test void classBranchRequiresOwnClassLevelButNotCommonNode() {
        var p = profile(HeroSubclass.HUNTER, 5);
        assertTrue(TalentState.empty().canBuy(p, Talent.MARKSMAN, 0));
        var state = TalentState.empty().buy(p, Talent.ENDURANCE, 0).buy(p, Talent.MARKSMAN, 1);
        assertFalse(state.canBuy(profile(HeroSubclass.HUNTER, 4), Talent.MARKSMAN, 2));
        assertTrue(state.canBuy(p, Talent.MARKSMAN, 2));
        var mixed = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.HUNTER, 1),
                new ClassTrack(HeroSubclass.SORCERER, 4)), 0);
        assertFalse(state.canBuy(mixed, Talent.MARKSMAN, 2));
    }

    @Test void firstClassRankCanBeFirstPurchaseAtLevelThreeForEveryClass() {
        for (var subclass : HeroSubclass.values()) {
            var p = profile(subclass, 3);
            var talent = Talent.specialization(subclass.parent());
            var state = TalentState.empty().buy(p, talent, 0);
            assertEquals(1, state.rank(talent));
            assertEquals(6, state.available(p));
            assertEquals(0, state.rank(Talent.VITALITY));
            assertEquals(0, state.rank(Talent.ENDURANCE));
            assertSame(state, state.buy(p, talent, 0));
            assertSame(state, state.buy(p, talent, 1));
        }
    }

    @Test void levelTwoAllowsEitherCommonTalentButNotClassTalent() {
        for (var subclass : HeroSubclass.values()) {
            var p = profile(subclass, 2);
            for (var common : List.of(Talent.VITALITY, Talent.ENDURANCE)) {
                var state = TalentState.empty().buy(p, common, 0);
                assertEquals(1, state.rank(common));
                assertEquals(5, state.available(p));
            }
            assertFalse(TalentState.empty().canBuy(p, Talent.specialization(subclass.parent()), 0));
        }
    }

    @Test void rankCostsIncreaseAndCannotOverspend() {
        var p = profile(HeroSubclass.GUARDIAN, 3);
        var state = TalentState.empty().buy(p, Talent.VITALITY, 0).buy(p, Talent.VITALITY, 1)
                .buy(p, Talent.ENDURANCE, 3).buy(p, Talent.ENDURANCE, 4).buy(p, Talent.RESILIENCE, 6);
        assertEquals(7, state.spent());
        assertEquals(0, state.available(p));
        for (var talent : Talent.values()) assertSame(state, state.buy(p, talent, 7));
    }

    @Test void storedRanksAreImmutableAndValidated() {
        assertThrows(IllegalArgumentException.class, () -> new TalentState(Map.of(Talent.VITALITY, 3)));
        assertThrows(IllegalArgumentException.class, () -> new TalentState(Map.of(Talent.VITALITY, -1)));
        assertThrows(UnsupportedOperationException.class, () -> TalentState.empty().ranks().put(Talent.VITALITY, 1));
    }
}
