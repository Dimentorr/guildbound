package dev.guildbound.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.guildbound.combat.DaggerState;

public final class DaggerCodecs {
    private DaggerCodecs() {}
    public static final Codec<DaggerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 200).fieldOf("main").forGetter(DaggerState::main),
            Codec.intRange(0, 200).fieldOf("off").forGetter(DaggerState::off),
            Codec.intRange(0, 200).fieldOf("shared").forGetter(DaggerState::shared),
            Codec.BOOL.fieldOf("next_off").forGetter(DaggerState::nextOff)
    ).apply(instance, DaggerState::new));
}
