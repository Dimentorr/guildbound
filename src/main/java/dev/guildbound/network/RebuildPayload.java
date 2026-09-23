package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.HeroClass;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RebuildPayload(BlockPos desk, UUID token, HeroClass heroClass, int expectedLevel, int expectedXp) implements CustomPacketPayload {
    public static final Type<RebuildPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "rebuild"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RebuildPayload> CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeBlockPos(payload.desk); buffer.writeUUID(payload.token); buffer.writeEnum(payload.heroClass);
        buffer.writeVarInt(payload.expectedLevel); buffer.writeVarInt(payload.expectedXp);
    }, buffer -> new RebuildPayload(buffer.readBlockPos(), buffer.readUUID(), buffer.readEnum(HeroClass.class), buffer.readVarInt(), buffer.readVarInt()));
    @Override public Type<RebuildPayload> type() { return TYPE; }
}
