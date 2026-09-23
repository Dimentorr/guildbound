package dev.guildbound.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.guildbound.progression.Talent;
import dev.guildbound.progression.TalentState;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TalentCodecsTest {
    @Test void emptyAndPurchasedStatesRoundTrip() {
        for (var state : new TalentState[]{TalentState.empty(), new TalentState(Map.of(Talent.VITALITY, 2, Talent.BULWARK, 1))}) {
            var json = TalentCodecs.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            assertEquals(state, TalentCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
    }

    @Test void invalidRanksAndUnknownVersionsOrTalentsReturnErrors() {
        for (String json : new String[]{"{\"version\":2,\"ranks\":{}}", "{\"version\":1,\"ranks\":{\"UNKNOWN\":1}}",
                "{\"version\":1,\"ranks\":{\"VITALITY\":3}}", "{\"version\":1,\"ranks\":{\"ENDURANCE\":-1}}"}) {
            assertTrue(TalentCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent());
        }
    }

    @Test void oldProgressStillLoadsAndGetsUnspentTalentPoints() {
        var progress = ProgressCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"version\":1,\"classes\":[{\"subclass\":\"HUNTER\",\"level\":5}],\"experience\":0}")).getOrThrow();
        assertEquals(9, TalentState.empty().available(progress));
    }
}
