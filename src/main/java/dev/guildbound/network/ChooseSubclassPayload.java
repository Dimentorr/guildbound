package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.HeroSubclass;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChooseSubclassPayload(BlockPos desk, UUID token, HeroSubclass subclass) implements CustomPacketPayload {
    public static final Type<ChooseSubclassPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "choose_subclass"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseSubclassPayload> CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeBlockPos(payload.desk); buffer.writeUUID(payload.token); buffer.writeEnum(payload.subclass);
    }, buffer -> new ChooseSubclassPayload(buffer.readBlockPos(), buffer.readUUID(), buffer.readEnum(HeroSubclass.class)));
    @Override public Type<ChooseSubclassPayload> type() { return TYPE; }
}
