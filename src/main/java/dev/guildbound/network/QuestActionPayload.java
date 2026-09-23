package dev.guildbound.network;

import dev.guildbound.Guildbound;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestActionPayload(BlockPos desk, UUID token, int contract, boolean claim) implements CustomPacketPayload {
    public static final Type<QuestActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "quest_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestActionPayload> CODEC = StreamCodec.of((b, p) -> {
        b.writeBlockPos(p.desk); b.writeUUID(p.token); b.writeVarInt(p.contract); b.writeBoolean(p.claim);
    }, b -> new QuestActionPayload(b.readBlockPos(), b.readUUID(), b.readVarInt(), b.readBoolean()));
    @Override public Type<QuestActionPayload> type() { return TYPE; }
}

