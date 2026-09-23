package dev.guildbound.data;

import dev.guildbound.server.GuildSession;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuildSessionTest {
    @Test void tokenPositionDimensionAndExpiryAllMustMatch() {
        var id = UUID.randomUUID();
        var pos = new BlockPos(1, 70, 3);
        var session = new GuildSession(id, pos, Level.OVERWORLD, 100);
        assertTrue(session.matches(id, pos, Level.OVERWORLD, 100));
        assertFalse(session.matches(id, pos, Level.OVERWORLD, 101));
        assertFalse(session.matches(UUID.randomUUID(), pos, Level.OVERWORLD, 50));
        assertFalse(session.matches(id, pos.above(), Level.OVERWORLD, 50));
        assertFalse(session.matches(id, pos, Level.NETHER, 50));
        assertFalse(GuildSession.empty().matches(id, pos, Level.OVERWORLD, 50));
    }
}
