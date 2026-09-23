package dev.guildbound.server;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Ephemeral desk authorization; path changes rotate it, quest actions validate journal state. */
public record GuildSession(UUID token, BlockPos desk, ResourceKey<Level> dimension, long expires) {
    public static GuildSession empty() { return new GuildSession(new UUID(0, 0), BlockPos.ZERO, Level.OVERWORLD, -1); }
    public boolean matches(UUID candidate, BlockPos position, ResourceKey<Level> currentDimension, long time) {
        return expires >= time && token.equals(candidate) && desk.equals(position) && dimension.equals(currentDimension);
    }
}
