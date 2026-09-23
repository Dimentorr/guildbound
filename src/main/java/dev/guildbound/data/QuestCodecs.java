package dev.guildbound.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.guildbound.progression.QuestState;

public final class QuestCodecs {
    private QuestCodecs() {}
    private record Stored(int version, int completed, boolean active, int kills) {}
    public static final Codec<QuestState> CODEC = RecordCodecBuilder.<Stored>create(i -> i.group(
        Codec.intRange(1, 1).fieldOf("version").forGetter(Stored::version),
        Codec.INT.fieldOf("completed").forGetter(Stored::completed),
        Codec.BOOL.fieldOf("active").forGetter(Stored::active),
        Codec.INT.fieldOf("kills").forGetter(Stored::kills)
    ).apply(i, Stored::new)).comapFlatMap(s -> {
        try { return DataResult.success(new QuestState(s.completed(), s.active(), s.kills())); }
        catch (IllegalArgumentException e) { return DataResult.error(() -> "Invalid quest journal"); }
    }, s -> new Stored(1, s.completed(), s.active(), s.kills()));
}

