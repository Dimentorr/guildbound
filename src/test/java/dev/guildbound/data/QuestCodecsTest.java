package dev.guildbound.data;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.guildbound.progression.QuestState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestCodecsTest {
    @Test void journalsRoundTrip() {
        for (var state : new QuestState[]{QuestState.empty(), new QuestState(0, true, 3),
                new QuestState(1, true, 6), new QuestState(3, false, 0)}) {
            var json = QuestCodecs.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            assertEquals(state, QuestCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
    }
    @Test void corruptJournalAndUnknownVersionReturnErrors() {
        for (String json : new String[]{
            "{\"version\":2,\"completed\":0,\"active\":false,\"kills\":0}",
            "{\"version\":1,\"completed\":100000,\"active\":true,\"kills\":0}",
            "{\"version\":1,\"completed\":0,\"active\":false,\"kills\":1}",
            "{\"version\":1,\"completed\":1,\"active\":true,\"kills\":7}"})
            assertTrue(QuestCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent());
    }
}

