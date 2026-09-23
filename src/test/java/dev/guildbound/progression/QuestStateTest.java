package dev.guildbound.progression;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestStateTest {
    @Test void killsBeforeAcceptAndWrongTargetsDoNotCount() {
        var empty = QuestState.empty();
        assertEquals(empty, empty.kill(true));
        assertEquals(empty, empty.claim());
        var accepted = empty.accept();
        assertEquals(accepted, accepted.kill(false));
        assertEquals(accepted, accepted.accept());
        assertEquals(accepted, accepted.claim());
    }
    @Test void completesEveryContractOnceAndCapsProgress() {
        var state = QuestState.empty();
        int xp = 0, emeralds = 0;
        for (var contract : GuildContract.values()) {
            assertEquals(contract, state.contract());
            state = state.accept();
            for (int i = 0; i < contract.target + 10; i++) state = state.kill(true);
            assertEquals(contract.target, state.kills());
            assertTrue(state.ready());
            xp += contract.experience; emeralds += contract.emeralds;
            state = state.claim();
            assertEquals(state, state.claim());
            assertFalse(state.active());
        }
        assertEquals(450, xp); assertEquals(9, emeralds);
        assertTrue(state.finished()); assertNull(state.contract());
        assertEquals(state, state.accept()); assertEquals(state, state.kill(true));
        assertEquals(state, state.claim());
    }
    @Test void rejectsImpossibleAndUnboundedStates() {
        assertThrows(IllegalArgumentException.class, () -> new QuestState(-1, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new QuestState(4, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new QuestState(3, true, 0));
        assertThrows(IllegalArgumentException.class, () -> new QuestState(0, false, 1));
        assertThrows(IllegalArgumentException.class, () -> new QuestState(0, true, 6));
        assertThrows(IllegalArgumentException.class, () -> new QuestState(0, true, -1));
    }
}

