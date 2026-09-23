package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.CharacterProgress;
import dev.guildbound.progression.ClassTrack;
import dev.guildbound.progression.HeroSubclass;
import dev.guildbound.progression.HeroClass;
import java.util.ArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ProgressPayload(CharacterProgress progress) implements CustomPacketPayload {
    public static final Type<ProgressPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressPayload> CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeVarInt(payload.progress.tracks().size());
        for (var track : payload.progress.tracks()) {
            buffer.writeEnum(track.heroClass());
            buffer.writeBoolean(track.subclass() != null);
            if (track.subclass() != null) buffer.writeEnum(track.subclass());
            buffer.writeVarInt(track.level());
        }
        buffer.writeVarInt(payload.progress.experience());
    }, buffer -> {
        int count = buffer.readVarInt();
        if (count < 0 || count > 4) throw new IllegalArgumentException("Invalid class count");
        var tracks = new ArrayList<ClassTrack>(count);
        for (int index = 0; index < count; index++) {
            var heroClass = buffer.readEnum(HeroClass.class);
            var subclass = buffer.readBoolean() ? buffer.readEnum(HeroSubclass.class) : null;
            tracks.add(new ClassTrack(heroClass, subclass, buffer.readVarInt()));
        }
        return new ProgressPayload(new CharacterProgress(tracks, buffer.readVarInt()));
    });

    @Override public Type<ProgressPayload> type() { return TYPE; }
}
