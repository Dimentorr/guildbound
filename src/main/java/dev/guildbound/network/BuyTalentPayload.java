package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.Talent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BuyTalentPayload(Talent talent, int expectedSpent) implements CustomPacketPayload {
    public static final Type<BuyTalentPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "buy_talent"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuyTalentPayload> CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeEnum(payload.talent);
        buffer.writeVarInt(payload.expectedSpent);
    }, buffer -> new BuyTalentPayload(buffer.readEnum(Talent.class), buffer.readVarInt()));
    @Override public Type<BuyTalentPayload> type() { return TYPE; }
}
