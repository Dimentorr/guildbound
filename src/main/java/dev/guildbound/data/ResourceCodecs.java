package dev.guildbound.data;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.guildbound.combat.ResourceState;
public final class ResourceCodecs {
 private ResourceCodecs() {}
    public static final Codec<ResourceState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, ResourceState.STORAGE_LIMIT).fieldOf("stamina").forGetter(ResourceState::stamina),
            Codec.intRange(0, ResourceState.STORAGE_LIMIT).fieldOf("mana").forGetter(ResourceState::mana),
            Codec.intRange(0, 40).fieldOf("recovery_delay").forGetter(ResourceState::recoveryDelay),
            Codec.intRange(0, 20).fieldOf("dodge_cooldown").forGetter(ResourceState::dodgeCooldown),
            Codec.intRange(0, 100).fieldOf("heal_cooldown").forGetter(ResourceState::healCooldown),
            Codec.BOOL.fieldOf("exhausted").forGetter(ResourceState::exhausted)
    ).apply(instance, ResourceState::new));
}
