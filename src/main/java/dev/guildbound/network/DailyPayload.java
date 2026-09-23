package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.DailyJournal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DailyPayload(DailyJournal state) implements CustomPacketPayload {
    public static final Type<DailyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "daily"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DailyPayload> CODEC = StreamCodec.of((b, p) -> {
        b.writeLong(p.state.day()); b.writeVarInt(p.state.tier()); b.writeVarInt(p.state.accepted()); b.writeVarInt(p.state.claimed());
        p.state.kills().forEach(b::writeVarInt);
    }, b -> {
        long day = b.readLong(); int tier = b.readVarInt(), accepted = b.readVarInt(), claimed = b.readVarInt();
        var kills = new java.util.ArrayList<Integer>(); for (int i = 0; i < 5; i++) kills.add(b.readVarInt());
        return new DailyPayload(new DailyJournal(day, tier, accepted, claimed, kills));
    });
    @Override public Type<DailyPayload> type() { return TYPE; }
}
