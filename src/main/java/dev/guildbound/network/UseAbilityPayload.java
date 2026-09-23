package dev.guildbound.network;

import dev.guildbound.Guildbound;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UseAbilityPayload(Ability ability) implements CustomPacketPayload {
    public enum Ability { DODGE, HEAL, FIRE }
    public static final Type<UseAbilityPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UseAbilityPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeEnum(payload.ability), buffer -> new UseAbilityPayload(buffer.readEnum(Ability.class)));
    @Override public Type<UseAbilityPayload> type() { return TYPE; }
}
