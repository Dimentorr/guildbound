package dev.guildbound.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DaggerStateTest {
    @Test void offhandAloneUsesItsOwnCooldownWithoutDualBonus() {
        var state = DaggerState.ready().attack(false, true, 20, 6, true);
        assertEquals(0, state.main());
        assertEquals(6, state.off());
        assertEquals(6, state.shared());
        assertFalse(state.nextOff());
        assertFalse(ticks(state, 5).canAttack(false, true));
        assertTrue(ticks(state, 6).canAttack(false, true));
        assertEquals(state, state.attack(false, true, 20, 6, true));
    }
    @Test void removingMainDaggerDoesNotClearTheOffhandCooldown() {
        var state = new DaggerState(0, 5, 0, false);
        assertFalse(state.canAttack(false, true));
        assertEquals(state, state.attack(false, true, 10, 6, true));
    }
    private static DaggerState ticks(DaggerState state, int count) {
        for (int i = 0; i < count; i++) state = state.tick();
        return state;
    }
    @Test void singleDaggerRequiresFullCooldownAndRejectsPacketSpam() {
        var state = DaggerState.ready().attack(false, 9, 9, true);
        assertFalse(state.canAttack(false));
        assertSame(state, state.attack(false, 9, 9, true));
        assertFalse(ticks(state, 8).canAttack(false));
        assertTrue(ticks(state, 9).canAttack(false));
    }
    @Test void twoHandsAlternateWithIndependentMaterialSpeeds() {
        var state = DaggerState.ready().attack(true, 10, 6, true);
        assertTrue(state.nextOff());
        assertEquals(10, state.main());
        state = ticks(state, 3);
        assertTrue(state.canAttack(true));
        state = state.attack(true, 10, 6, true);
        assertFalse(state.nextOff());
        assertEquals(4, state.main()); // 7 remaining minus ceil(10 / 4)
        assertEquals(6, state.off());
        assertFalse(ticks(state, 3).canAttack(true));
        assertTrue(ticks(state, 4).canAttack(true));
    }
    @Test void blockedHitConsumesCooldownButDoesNotReduceOtherHand() {
        var state = ticks(DaggerState.ready().attack(true, 10, 6, true), 3);
        assertEquals(7, state.attack(true, 10, 6, false).main());
        assertEquals(4, state.attack(true, 10, 6, true).main());
    }
    @Test void removingOffhandCannotSkipSharedOrMainCooldown() {
        var state = DaggerState.ready().attack(true, 10, 6, true);
        assertFalse(state.canAttack(false));
        state = ticks(state, 3);
        assertTrue(state.canAttack(true));
        assertFalse(state.canAttack(false));
        assertFalse(state.useOff(false));
    }
    @Test void longCombinationStaysBoundedAndNeverHitsTwiceInOneTick() {
        var state = DaggerState.ready();
        boolean expectedOff = false;
        for (int tick = 0; tick < 20000; tick++) {
            if (state.canAttack(true)) {
                assertEquals(expectedOff, state.useOff(true));
                state = state.attack(true, 5, 7, tick % 3 != 0);
                expectedOff = !expectedOff;
                assertFalse(state.canAttack(true));
            }
            state = state.tick();
        }
    }
    @Test void periodsAccountForClassSpeedAndHaveSafeBounds() {
        assertEquals(9, DaggerState.duration(2.4));
        assertEquals(7, DaggerState.duration(2.4 * 1.3));
        assertEquals(6, DaggerState.duration(3.6));
        assertEquals(200, DaggerState.duration(0));
        assertEquals(1, DaggerState.duration(1000));
    }
}
