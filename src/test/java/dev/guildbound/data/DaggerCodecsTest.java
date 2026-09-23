package dev.guildbound.data;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import dev.guildbound.combat.DaggerState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DaggerCodecsTest {
    @Test void relogPreservesBothCooldownsAndNextHand() {
        var state = new DaggerState(5, 8, 2, true);
        var json = DaggerCodecs.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        assertEquals(state, DaggerCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
    @Test void rejectsInvalidCooldownFromSave() {
        assertTrue(DaggerCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"main\":-1,\"off\":0,\"shared\":0,\"next_off\":false}")).error().isPresent());
    }
}
