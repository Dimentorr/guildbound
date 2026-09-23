package dev.guildbound.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EtherFieldTest {
    @Test void fullChannelCostsExactlyOneHundredManaAndPulsesEveryTwelveTicks() {
        var field = new EtherField(0);
        int mana = 1000, pulses = 0;
        while (field.canTick(mana)) {
            mana -= EtherField.COST;
            field = field.tick();
            if (field.pulse()) { pulses++; assertEquals(0, field.elapsed()%12); }
        }
        assertEquals(200, field.elapsed()); assertEquals(0, mana); assertEquals(16, pulses);
        assertFalse(field.canTick(1000)); assertEquals(6, EtherField.damage(0));
    }
    @Test void InsufficientManaNeverPaysOrPulses() {
        assertFalse(new EtherField(0).canTick(4));
        assertTrue(new EtherField(0).canTick(5));
        assertFalse(new EtherField(0).pulse());
    }
}
