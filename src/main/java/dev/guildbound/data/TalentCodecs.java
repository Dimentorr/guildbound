package dev.guildbound.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.guildbound.progression.Talent;
import dev.guildbound.progression.TalentState;
import java.util.Map;

public final class TalentCodecs {
    private TalentCodecs() {}
    private static final Codec<Talent> TALENT = Codec.STRING.comapFlatMap(value -> {
        try { return DataResult.success(Talent.valueOf(value)); }
        catch (IllegalArgumentException ex) { return DataResult.error(() -> "Unknown talent: " + value); }
    }, Enum::name);
    private record Stored(int version, Map<Talent, Integer> ranks) {}
    public static final Codec<TalentState> CODEC = RecordCodecBuilder.<Stored>create(instance -> instance.group(
            Codec.intRange(1, 1).fieldOf("version").forGetter(Stored::version),
            Codec.unboundedMap(TALENT, Codec.intRange(1, 2)).fieldOf("ranks").forGetter(Stored::ranks)
    ).apply(instance, Stored::new)).xmap(stored -> new TalentState(stored.ranks()), state -> new Stored(1, state.ranks()));
}
