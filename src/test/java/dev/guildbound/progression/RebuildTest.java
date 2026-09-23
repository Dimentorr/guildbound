package dev.guildbound.progression;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RebuildTest {
    @Test void changesClassLosesOneLevelAndClearsLevelExperience() {
        var old = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.GUARDIAN, 4)), 99);
        var result = old.rebuild(HeroSubclass.SORCERER);
        assertEquals(List.of(new ClassTrack(HeroSubclass.SORCERER, 3)), result.tracks());
        assertEquals(0, result.experience());
        assertEquals(7, TalentState.empty().available(result));
        assertEquals(99, old.experience());
    }
    @Test void sameClassRebuildHasTheSameCostAndLevelFloor() {
        var p = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.DUELIST, 5)), 0);
        for (int i = 0; i < 8; i++) p = p.rebuild(HeroSubclass.DUELIST);
        assertEquals(1, p.totalLevel());
        assertEquals(5, TalentState.empty().available(p));
        assertTrue(p.registered());
    }
    @Test void refusesUnregisteredAndUnsupportedMulticlassRebuilds() {
        assertThrows(IllegalStateException.class, () -> CharacterProgress.empty().rebuild(HeroSubclass.HUNTER));
        var p = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.DUELIST, 2), new ClassTrack(HeroSubclass.HUNTER, 2)), 0);
        assertThrows(IllegalStateException.class, () -> p.rebuild(HeroSubclass.HUNTER));
    }
}
