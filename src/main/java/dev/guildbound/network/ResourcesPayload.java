package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.combat.ResourceState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ResourcesPayload(ResourceState state, int fireCooldown) implements CustomPacketPayload {
    public static final Type<ResourcesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "resources"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourcesPayload> CODEC = StreamCodec.of((buffer, payload) -> {
        var value = payload.state;
        buffer.writeVarInt(value.stamina());
        buffer.writeVarInt(value.mana());
        buffer.writeVarInt(value.recoveryDelay());
        buffer.writeVarInt(value.dodgeCooldown());
        buffer.writeVarInt(value.healCooldown());
        buffer.writeBoolean(value.exhausted());
        buffer.writeVarInt(payload.fireCooldown);
    }, buffer -> new ResourcesPayload(new ResourceState(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()), buffer.readVarInt()));
    @Override public Type<ResourcesPayload> type() { return TYPE; }
}
