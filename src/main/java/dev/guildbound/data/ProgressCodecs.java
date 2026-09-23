package dev.guildbound.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.guildbound.progression.CharacterProgress;
import dev.guildbound.progression.ClassTrack;
import dev.guildbound.progression.HeroSubclass;
import dev.guildbound.progression.HeroClass;
import java.util.Optional;
import java.util.List;

/** Persistence format separated from registry bootstrap so saved data can be tested independently. */
public final class ProgressCodecs {
    private ProgressCodecs() {}

    private static final Codec<HeroSubclass> SUBCLASS = Codec.STRING.comapFlatMap(value -> {
        try { return DataResult.success(HeroSubclass.valueOf(value)); }
        catch (IllegalArgumentException ex) { return DataResult.error(() -> "Unknown subclass: " + value); }
    }, Enum::name);

    private static final Codec<HeroClass> HERO_CLASS = Codec.STRING.comapFlatMap(value -> {
        try { return DataResult.success(HeroClass.valueOf(value)); }
        catch (IllegalArgumentException ex) { return DataResult.error(() -> "Unknown class: " + value); }
    }, Enum::name);
    private record StoredTrack(Optional<HeroClass> heroClass, Optional<HeroSubclass> subclass, int level) {}
    private static final Codec<ClassTrack> TRACK = RecordCodecBuilder.<StoredTrack>create(instance -> instance.group(
            HERO_CLASS.optionalFieldOf("class").forGetter(StoredTrack::heroClass),
            SUBCLASS.optionalFieldOf("subclass").forGetter(StoredTrack::subclass),
            Codec.intRange(1, CharacterProgress.MAX_LEVEL).fieldOf("level").forGetter(StoredTrack::level)
    ).apply(instance, StoredTrack::new)).flatXmap(stored -> {
        try {
            var heroClass = stored.heroClass().orElseGet(() -> stored.subclass().orElseThrow().parent());
            return DataResult.success(new ClassTrack(heroClass, stored.subclass().orElse(null), stored.level()));
        } catch (RuntimeException ex) { return DataResult.error(() -> "Invalid class track"); }
    }, track -> DataResult.success(new StoredTrack(Optional.of(track.heroClass()), Optional.ofNullable(track.subclass()), track.level())));

    private record StoredProgress(int version, List<ClassTrack> tracks, int experience) {}

    public static final Codec<CharacterProgress> CODEC = RecordCodecBuilder.<StoredProgress>create(instance -> instance.group(
            Codec.intRange(1, 2).fieldOf("version").forGetter(StoredProgress::version),
            TRACK.listOf().fieldOf("classes").forGetter(StoredProgress::tracks),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("experience").forGetter(StoredProgress::experience)
    ).apply(instance, StoredProgress::new)).flatXmap(stored -> {
        try { return DataResult.success(new CharacterProgress(stored.tracks(), stored.experience())); }
        catch (IllegalArgumentException ex) { return DataResult.error(ex::getMessage); }
    }, value -> DataResult.success(new StoredProgress(2, value.tracks(), value.experience())));
}
