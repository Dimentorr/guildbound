package dev.guildbound.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.guildbound.progression.CharacterProgress;
import dev.guildbound.progression.ClassTrack;
import dev.guildbound.progression.HeroSubclass;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressCodecsTest {
    @Test void roundTripPreservesMulticlassAndExperience() {
        var profile = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.GUARDIAN, 3),
                new ClassTrack(HeroSubclass.SORCERER, 2)), 75);
        var json = ProgressCodecs.CODEC.encodeStart(JsonOps.INSTANCE, profile).getOrThrow();
        assertEquals(profile, ProgressCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test void unregisteredProfileRoundTrips() {
        var json = ProgressCodecs.CODEC.encodeStart(JsonOps.INSTANCE, CharacterProgress.empty()).getOrThrow();
        assertEquals(CharacterProgress.empty(), ProgressCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test void legacySubclassMigratesWithoutLosingClassLevelOrXp() {
        for (int level : new int[]{3, 5}) {
            var profile = ProgressCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                    "{\"version\":1,\"classes\":[{\"subclass\":\"SORCERER\",\"level\":" + level
                            + "}],\"experience\":99}")).getOrThrow();
            var track = profile.tracks().getFirst();
            assertEquals(level, track.level());
            assertEquals(dev.guildbound.progression.HeroClass.MAGE, track.heroClass());
            assertEquals(level < 5 ? null : HeroSubclass.SORCERER, track.subclass());
            assertEquals(99, profile.experience());
            var encoded = ProgressCodecs.CODEC.encodeStart(JsonOps.INSTANCE, profile).getOrThrow();
            assertEquals(2, encoded.getAsJsonObject().get("version").getAsInt());
            assertEquals(profile, ProgressCodecs.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        }
    }

    @Test void baseClassAtLevelOneHundredRoundTripsAndMismatchIsRejected() {
        var profile = new CharacterProgress(List.of(new ClassTrack(dev.guildbound.progression.HeroClass.ROGUE, 100)), 0);
        var encoded = ProgressCodecs.CODEC.encodeStart(JsonOps.INSTANCE, profile).getOrThrow();
        assertEquals(profile, ProgressCodecs.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        assertRejected("""
                {"version":2,"classes":[{"class":"MAGE","subclass":"HUNTER","level":5}],"experience":0}
                """);
        assertRejected("""
                {"version":2,"classes":[{"level":5}],"experience":0}
                """);
    }

    @Test void rejectsFutureVersionInsteadOfSilentlyResettingProgress() {
        assertRejected("""
                {"version":3,"classes":[],"experience":0}
                """);
    }

    @Test void invalidAndUnknownClassDataReturnCodecErrors() {
        assertRejected("""
                {"version":1,"classes":[{"subclass":"GUARDIAN","level":60},
                {"subclass":"SORCERER","level":60}],"experience":0}
                """);
        assertRejected("""
                {"version":1,"classes":[{"subclass":"UNKNOWN","level":1}],"experience":0}
                """);
        assertRejected("""
                {"version":1,"classes":[{"subclass":"GUARDIAN","level":0}],"experience":0}
                """);
    }

    private static void assertRejected(String json) {
        assertTrue(ProgressCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent());
    }
}
