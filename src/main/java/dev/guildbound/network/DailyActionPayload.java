package dev.guildbound.network;

import dev.guildbound.Guildbound;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DailyActionPayload(BlockPos desk, UUID token, long day, int slot, boolean claim) implements CustomPacketPayload {
    public static final Type<DailyActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "daily_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DailyActionPayload> CODEC = StreamCodec.of((b, p) -> {
        b.writeBlockPos(p.desk); b.writeUUID(p.token); b.writeLong(p.day); b.writeVarInt(p.slot); b.writeBoolean(p.claim);
    }, b -> new DailyActionPayload(b.readBlockPos(), b.readUUID(), b.readLong(), b.readVarInt(), b.readBoolean()));
    @Override public Type<DailyActionPayload> type() { return TYPE; }
}
