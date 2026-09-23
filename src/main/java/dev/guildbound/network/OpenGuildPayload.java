package dev.guildbound.network;

import dev.guildbound.Guildbound;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenGuildPayload(BlockPos desk, java.util.UUID token, String notice) implements CustomPacketPayload {
    public OpenGuildPayload(BlockPos desk, java.util.UUID token) { this(desk, token, ""); }
    public static final Type<OpenGuildPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "open_guild"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGuildPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeBlockPos(payload.desk); buffer.writeUUID(payload.token); buffer.writeUtf(payload.notice, 160); },
            buffer -> new OpenGuildPayload(buffer.readBlockPos(), buffer.readUUID(), buffer.readUtf(160)));
    @Override public Type<OpenGuildPayload> type() { return TYPE; }
}
