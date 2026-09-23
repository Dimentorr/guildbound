package dev.guildbound.combat;

import dev.guildbound.progression.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CantripBalanceTest {
    @Test void ranksImproveDamageAndCooldownIndependentlyOfLevelBonus() {
        for (int rank = 0; rank <= 2; rank++) {
            assertEquals(5F + rank, CantripBalance.damage(1, rank));
            assertEquals(5F + rank, CantripBalance.damage(4, rank));
            assertEquals(6.5F + rank, CantripBalance.damage(5, rank));
            assertEquals(50 - 10 * rank, CantripBalance.cooldown(rank));
        }
    }
    @Test void emberRanksRequireMageLevelsThreeAndFiveAndSpendNormalPoints() {
        var level3 = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.SORCERER, 3)), 0);
        var state = TalentState.empty().buy(level3, Talent.EMBER, 0);
        assertEquals(1, state.rank(Talent.EMBER));
        assertEquals(6, state.available(level3));
        assertFalse(state.canBuy(level3, Talent.EMBER, 1));
        var level5 = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.SORCERER, 5)), 0);
        state = state.buy(level5, Talent.EMBER, 1);
        assertEquals(2, state.rank(Talent.EMBER));
        assertEquals(6, state.available(level5));
        assertFalse(state.canBuy(level5, Talent.EMBER, 2));
        assertFalse(TalentState.empty().canBuy(new CharacterProgress(List.of(new ClassTrack(HeroSubclass.HUNTER, 5)), 0), Talent.EMBER, 0));
    }
}
