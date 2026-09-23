package dev.guildbound.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceStateTest {
    @Test void finalCooldownSnapshotArrivesInEveryTickPhaseWithFullResources() {
        for(int phase=0;phase<5;phase++) {
            var server=new ResourceState(1000,1000,0,0,100,false);
            var client=server;
            for(int tick=1;tick<=105;tick++) {
                var next=server.tick(false);
                if(!next.equals(server) && ((tick+phase)%5==0 || next.cooldownFinishedSince(server))) client=next;
                server=next;
            }
            assertEquals(0,client.healCooldown(),"phase "+phase);
            assertTrue(client.canHeal());
        }
        var previous=new ResourceState(1000,1000,0,1,0,false);
        assertTrue(previous.tick(false).cooldownFinishedSince(previous));
        assertFalse(ResourceState.full().cooldownFinishedSince(ResourceState.full()));
    }
    @Test void enduranceOnlyImprovesIdleRecoveryAfterDelay() {
        var state = ResourceState.full().spendStamina(100);
        assertEquals(900, state.tick(false, false, 2).stamina());
        for (int tick = 0; tick < 8; tick++) state = state.tick(false, false, 2);
        assertEquals(914, state.tick(false, false, 2).stamina());
        assertEquals(912, state.tick(false, false, 1).stamina());
        assertEquals(898, state.tick(true, false, 2).stamina());
        assertEquals(892, state.tick(true, true, 2).stamina());
        assertEquals(1000, new ResourceState(999, 1000, 0, 0, 0, false).tick(false, false, 2).stamina());
        assertThrows(IllegalArgumentException.class, () -> ResourceState.full().tick(false, false, 21));
    }
    @Test void spendingAndRegenerationNeverLeaveBounds() {
        var state = ResourceState.full().spendStamina(1000);
        for (int tick = 0; tick < 20000; tick++) {
            state = state.tick(tick % 200 < 100);
            assertTrue(state.stamina() >= 0 && state.stamina() <= ResourceState.MAX);
            assertTrue(state.mana() >= 0 && state.mana() <= ResourceState.MAX);
        }
    }

    @Test void attackRequiresEnoughResourceAndJumpCanDrainRemainder() {
        var state = ResourceState.full().spendStamina(985);
        assertFalse(state.canAttack());
        var jumped = state.spendStamina(ResourceState.JUMP_COST);
        assertEquals(0, jumped.stamina());
        assertTrue(jumped.exhausted());
    }

    @Test void cannotSpamDodgeAndCooldownIsTwentyTicks() {
        var state = ResourceState.full().dodge();
        assertEquals(880, state.stamina());
        assertEquals(state, state.dodge());
        for (int tick = 0; tick < 19; tick++) state = state.tick(false);
        assertFalse(state.canDodge());
        assertTrue(state.tick(false).canDodge());
    }

    @Test void healingConsumesManaAndCannotRepeatDuringCooldown() {
        var state = ResourceState.full().heal();
        assertEquals(750, state.mana());
        assertEquals(state, state.heal());
        for (int tick = 0; tick < 100; tick++) state = state.tick(false);
        assertTrue(state.canHeal());
        assertEquals(850, state.mana());
        assertFalse(new ResourceState(1000, 249, 0, 0, 0, false).canHeal());
    }

    @Test void exhaustedSprintRecoversAfterShortRest() {
        var state = ResourceState.full().spendStamina(1000);
        for (int tick = 0; tick < 22; tick++) state = state.tick(false);
        assertEquals(140, state.stamina());
        assertFalse(state.canSprint());
        state = state.tick(false);
        assertEquals(150, state.stamina());
        assertTrue(state.canSprint());
    }

    @Test void recoveryDelayPreventsImmediateRefunds() {
        var state = ResourceState.full().spendStamina(100);
        for (int tick = 0; tick < 8; tick++) state = state.tick(false);
        assertEquals(900, state.stamina());
        assertEquals(910, state.tick(false).stamina());
    }

    @Test void negativeCostsCannotGenerateResources() {
        assertThrows(IllegalArgumentException.class, () -> ResourceState.full().spendStamina(-100));
        assertThrows(IllegalArgumentException.class, () -> new ResourceState(ResourceState.STORAGE_LIMIT+1, 1000, 0, 0, 0, false));
    }

    @Test void tinyRemainderAlsoLocksSprintToPreventFlickering() {
        var state = new ResourceState(3, 1000, 0, 0, 0, false).tick(true);
        assertEquals(1, state.stamina());
        assertTrue(state.exhausted());
        assertFalse(state.canSprint());
    }

    @Test void fullBarAllowsTwentyFiveSecondsOfContinuousSprint() {
        var state = ResourceState.full();
        for (int tick = 0; tick < 499; tick++) state = state.tick(true);
        assertTrue(state.canSprint());
        assertFalse(state.tick(true).canSprint());
    }

    @Test void runningWithJumpsAndOneDodgeStillAllowsAnAttackAfterTenSeconds() {
        var state = ResourceState.full().dodge();
        for (int tick = 0; tick < 200; tick++) {
            if (tick % 20 == 0) state = state.spendStamina(ResourceState.JUMP_COST);
            state = state.tick(true);
        }
        assertTrue(state.canAttack());
        assertTrue(state.canDodge());
    }

    @Test void shieldSprintConsumesFullBarInSixAndQuarterSeconds() {
        var state = ResourceState.full();
        for (int tick = 0; tick < 124; tick++) state = state.tick(true, true);
        assertTrue(state.canSprint(true));
        state = state.tick(true, true);
        assertEquals(0, state.stamina());
        assertFalse(state.canSprint(true));
        assertTrue(state.exhausted());
    }

    @Test void shieldRemainderLocksSprintUntilRecoveryThreshold() {
        var state = new ResourceState(15, 1000, 0, 0, 0, false).tick(true, true);
        assertEquals(7, state.stamina());
        assertTrue(state.exhausted());
        for (int tick = 0; tick < 22; tick++) state = state.tick(false, true);
        assertFalse(state.canSprint(true));
        assertTrue(state.tick(false, true).canSprint(true));
    }

    @Test void HoldingShieldAtRestRegeneratesNormally() {
        var state = ResourceState.full().spendStamina(400);
        assertEquals(state.tick(false), state.tick(false, true));
        for (int tick = 0; tick < 60; tick++) state = state.tick(false, true);
        assertEquals(ResourceState.full(), state);
    }

    @Test void loweringShieldRestoresNormalSprintCostWithoutResettingCooldowns() {
        var state = ResourceState.full().dodge().heal().tick(true, true);
        var next = state.tick(true, false);
        assertEquals(state.stamina() - ResourceState.SPRINT_DRAIN, next.stamina());
        assertEquals(state.dodgeCooldown() - 1, next.dodgeCooldown());
        assertEquals(state.healCooldown() - 1, next.healCooldown());
        assertEquals(state.mana() + 1, next.mana());
    }

    @Test void shieldSprintCannotStartWithOnlyEnoughForNormalSprint() {
        var state = new ResourceState(7, 1000, 0, 0, 0, false);
        assertTrue(state.canSprint());
        assertFalse(state.canSprint(true));
        for (int tick = 0; tick < 20000; tick++) {
            state = state.tick(tick % 200 < 100, tick % 300 < 150);
            assertTrue(state.stamina() >= 0 && state.stamina() <= ResourceState.MAX);
        }
    }
}
