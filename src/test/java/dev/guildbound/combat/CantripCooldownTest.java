package dev.guildbound.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CantripCooldownTest {
    @Test void eitherSpellLocksBothUntilItsTimerExpires() {
        var resources = ResourceState.full();
        assertEquals(50, CantripCooldown.remaining(resources, 50));
        assertEquals(100, CantripCooldown.remaining(resources.heal(), 0));
        assertEquals(100, CantripCooldown.remaining(resources.heal(), 30));
    }
    @Test void existingSavedTimersUseTheLongestRemainderWithoutRestartingIt() {
        var resources = new ResourceState(1000, 750, 0, 0, 75, false);
        int fire = 30;
        for (int tick = 0; tick < 75; tick++) {
            assertEquals(75 - tick, CantripCooldown.remaining(resources, fire));
            resources = resources.tick(false);
            fire = Math.max(0, fire - 1);
        }
        assertEquals(0, CantripCooldown.remaining(resources, fire));
    }
}
