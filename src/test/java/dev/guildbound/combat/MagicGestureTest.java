package dev.guildbound.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagicGestureTest {
    @Test void tapCastsOnlyOnReleaseIncludingVeryShortTaps() {
        var gesture = new MagicGesture();
        assertEquals(MagicGesture.Action.NONE, gesture.update(true, true, true, 0));
        assertEquals(MagicGesture.Action.CAST, gesture.update(false, false, true, 100));
        assertEquals(MagicGesture.Action.NONE, gesture.update(false, false, true, 150));
        assertEquals(MagicGesture.Action.CAST, gesture.update(true, false, true, 200));
    }
    @Test void holdOpensWheelAndNeverCastsOnRelease() {
        var gesture = new MagicGesture();
        gesture.update(true, true, true, 0);
        assertEquals(MagicGesture.Action.NONE, gesture.update(true, true, true, 249));
        assertEquals(MagicGesture.Action.OPEN, gesture.update(false, true, true, 250));
        assertEquals(MagicGesture.Action.NONE, gesture.update(false, false, true, 300));
    }
    @Test void losingFocusOrOpeningAnotherScreenCancelsPendingCast() {
        var gesture = new MagicGesture();
        gesture.update(true, true, true, 0);
        assertEquals(MagicGesture.Action.NONE, gesture.update(false, true, false, 50));
        assertEquals(MagicGesture.Action.NONE, gesture.update(false, false, true, 100));
    }
    @Test void releaseAfterHoldThresholdCannotCastEvenWhenClientTicksAreDelayed() {
        var gesture = new MagicGesture();
        gesture.update(true, true, true, 0);
        assertEquals(MagicGesture.Action.OPEN, gesture.update(false, false, true, 400));
    }
    @Test void wheelLayoutsSelectEveryVertexAndKeepCenterUnselected() {
        for (int count = 2; count <= 12; count++) {
            assertEquals(-1, RadialLayout.pick(0, 0, count));
            for (int i = 0; i < count; i++) {
                double angle = RadialLayout.angle(i, count);
                assertEquals(i, RadialLayout.pick(Math.cos(angle) * 100, Math.sin(angle) * 100, count));
            }
        }
        assertEquals(0, RadialLayout.pick(100, 0, 2));
        assertEquals(1, RadialLayout.pick(-100, 0, 2));
    }
}
