package dev.guildbound.combat;

import dev.guildbound.client.TreeViewport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TreeViewportTest {
    @Test void initialViewShowsBothFirstBranchesOnSmallAndLargeWindows() {
        var view = new TreeViewport();
        assertEquals(2, view.zoom());
        for (int width : new int[]{280, 344, 728, 1000}) {
            view.fitInitial(width, 120);
            assertTrue(176 * view.zoom() < width / 2.0);
            assertTrue(view.zoom() <= 2);
            assertEquals(0, view.panX());
        }
    }
    @Test void zoomKeepsPointUnderCursorAndClamps() {
        var view = new TreeViewport();
        view.pan(30, -15);
        double x = view.worldX(77), y = view.worldY(31);
        view.zoomAt(77, 31, 3);
        assertEquals(x, view.worldX(77), 1e-8);
        assertEquals(y, view.worldY(31), 1e-8);
        view.zoomAt(77, 31, 1000);
        assertEquals(2.5, view.zoom());
        view.zoomAt(77, 31, -1000);
        assertEquals(.15, view.zoom());
        view.reset();
        assertEquals(2, view.zoom());
        assertEquals(0, view.panX());
        assertEquals(0, view.panY());
    }
    @Test void meditationRegeneratesManaAndNeverExceedsCap() {
        var state = new ResourceState(1000, 900, 0, 0, 0, false);
        assertEquals(903, state.tick(false, false, 0, 2).mana());
        assertEquals(1000, ResourceState.full().tick(false, false, 0, 2).mana());
    }
}
