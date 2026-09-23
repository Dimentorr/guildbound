package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.HeroClass;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RegisterClassPayload(BlockPos desk, HeroClass heroClass) implements CustomPacketPayload {
    public static final Type<RegisterClassPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "register_class"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RegisterClassPayload> CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeBlockPos(payload.desk);
        buffer.writeEnum(payload.heroClass);
    }, buffer -> new RegisterClassPayload(buffer.readBlockPos(), buffer.readEnum(HeroClass.class)));
    @Override public Type<RegisterClassPayload> type() { return TYPE; }
}
