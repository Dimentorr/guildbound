package dev.guildbound.progression;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExtendedTalentsTest {
    @Test void allSorcererAndCommonBranchesCanBePurchasedAndAccountForTheirActualCost() {
        var p = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.SORCERER, 100)), 0);
        var state = TalentState.empty(); int expected = 0, bought = 0;
        for (int pass=0;pass<10;pass++) for (var t : Talent.values()) {
            if (t.heroClass()!=null && t.heroClass()!=HeroClass.MAGE || t.subclass()!=null && t.subclass()!=HeroSubclass.SORCERER) continue;
            if (state.canBuy(p,t,state.spent())) {
                expected += t.cost(state.rank(t)+1); state = state.buy(p,t,state.spent()); bought++;
            }
        }
        assertEquals(expected,state.spent()); assertTrue(bought>=60);
        assertEquals(8,state.effectiveRank(Talent.VITALITY));
        assertEquals(14,state.effectiveRank(Talent.MEDITATION));
        assertEquals(0,state.rank(Talent.SPELL_WEAVING));
    }
    @Test void healingBranchRequiresRestorationNotAnotherSubclass() {
        var p = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.SORCERER, 5)), 0);
        var state = TalentState.empty();
        assertFalse(state.canBuy(p,Talent.SORCERER_TRAINING,0));
        state = state.buy(p,Talent.RESTORATION,0);
        assertTrue(state.canBuy(p,Talent.SORCERER_TRAINING,state.spent()));
    }
}
