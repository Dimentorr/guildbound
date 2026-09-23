package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.QuestState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestsPayload(QuestState state) implements CustomPacketPayload {
    public static final Type<QuestsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "quests"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestsPayload> CODEC = StreamCodec.of((b, p) -> {
        b.writeVarInt(p.state.completed()); b.writeBoolean(p.state.active()); b.writeVarInt(p.state.kills());
    }, b -> new QuestsPayload(new QuestState(b.readVarInt(), b.readBoolean(), b.readVarInt())));
    @Override public Type<QuestsPayload> type() { return TYPE; }
}

