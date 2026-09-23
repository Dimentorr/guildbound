package dev.guildbound.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CharacterProgressTest {
    @Test void cannotRegisterTwice() {
        var profile = CharacterProgress.empty().register(HeroSubclass.GUARDIAN);
        assertEquals(1, profile.totalLevel());
        assertThrows(IllegalStateException.class, () -> profile.register(HeroSubclass.SORCERER));
    }

    @Test void unregisteredPlayersCannotAccumulateExperience() {
        assertEquals(CharacterProgress.empty(), CharacterProgress.empty().awardExperience(500));
    }

    @Test void exactThresholdAndRemainderCarryAcrossLevels() {
        var profile = CharacterProgress.empty().register(HeroSubclass.HUNTER);
        assertEquals(1, profile.awardExperience(99).totalLevel());
        assertEquals(2, profile.awardExperience(100).totalLevel());
        var advanced = profile.awardExperience(350);
        assertEquals(3, advanced.totalLevel());
        assertEquals(25, advanced.experience());
        assertEquals(0, profile.experience());
    }

    @Test void hugeExperienceCannotOverflowOrBypassLevelCap() {
        var profile = CharacterProgress.empty().register(HeroSubclass.SORCERER).awardExperience(99);
        var capped = profile.awardExperience(Integer.MAX_VALUE);
        assertEquals(100, capped.totalLevel());
        assertEquals(0, capped.experience());
        assertEquals(capped, capped.awardExperience(1000));
    }
    @Test void newCurveRequires1188ExperienceAndPreservesExistingProgress() {
        var profile = CharacterProgress.empty().register(HeroSubclass.SORCERER);
        int[] costs = {100, 225, 338, 525};
        for (int cost : costs) {
            assertEquals(cost, profile.nextLevelCost());
            assertEquals(profile.totalLevel(), profile.awardExperience(cost - 1).totalLevel());
            profile = profile.awardExperience(cost);
            assertEquals(0, profile.experience());
        }
        assertEquals(5, profile.totalLevel());
        assertEquals(4, CharacterProgress.empty().register(HeroSubclass.HUNTER).awardExperience(1187).totalLevel());
        assertEquals(5, CharacterProgress.empty().register(HeroSubclass.HUNTER).awardExperience(1188).totalLevel());
        var existing = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.HUNTER, 3)), 250);
        assertEquals(3, existing.awardExperience(87).totalLevel());
        assertEquals(4, existing.awardExperience(88).totalLevel());
    }

    @Test void summedMulticlassLevelCannotExceedOneHundred() {
        var legal = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.GUARDIAN, 60),
                new ClassTrack(HeroSubclass.SORCERER, 40)), 0);
        assertEquals(100, legal.totalLevel());
        assertThrows(IllegalArgumentException.class, () -> new CharacterProgress(List.of(
                new ClassTrack(HeroSubclass.GUARDIAN, 61), new ClassTrack(HeroSubclass.SORCERER, 40)), 0));
    }

    @Test void rejectsDuplicateClassesAndInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new CharacterProgress(List.of(
                new ClassTrack(HeroSubclass.HUNTER, 1), new ClassTrack(HeroSubclass.HUNTER, 1)), 0));
        assertThrows(IllegalArgumentException.class, () -> new ClassTrack(HeroSubclass.HUNTER, 0));
        assertThrows(IllegalArgumentException.class, () -> new CharacterProgress(List.of(), 1));
        assertThrows(IllegalArgumentException.class, () -> CharacterProgress.empty().awardExperience(-1));
    }

    @Test void callerCannotMutateStoredTracks() {
        var tracks = new ArrayList<>(List.of(new ClassTrack(HeroSubclass.DUELIST, 1)));
        var profile = new CharacterProgress(tracks, 0);
        tracks.clear();
        assertEquals(1, profile.totalLevel());
        assertThrows(UnsupportedOperationException.class, () -> profile.tracks().clear());
    }

    @Test void unfinishedMulticlassCannotSilentlyAssignExperienceToWrongClass() {
        var profile = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.GUARDIAN, 1),
                new ClassTrack(HeroSubclass.SORCERER, 1)), 0);
        assertThrows(IllegalStateException.class, () -> profile.awardExperience(25));
    }
}
