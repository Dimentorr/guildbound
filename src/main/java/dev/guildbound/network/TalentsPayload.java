package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.Talent;
import dev.guildbound.progression.TalentState;
import java.util.EnumMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TalentsPayload(TalentState state) implements CustomPacketPayload {
    public static final Type<TalentsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "talents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TalentsPayload> CODEC = StreamCodec.of((buffer, payload) -> {
        for (var talent : Talent.values()) buffer.writeVarInt(payload.state.rank(talent));
    }, buffer -> {
        var ranks = new EnumMap<Talent, Integer>(Talent.class);
        for (var talent : Talent.values()) {
            int rank = buffer.readVarInt();
            if (rank < 0 || rank > 2) throw new IllegalArgumentException("Invalid talent rank");
            if (rank > 0) ranks.put(talent, rank);
        }
        return new TalentsPayload(new TalentState(ranks));
    });
    @Override public Type<TalentsPayload> type() { return TYPE; }
}
