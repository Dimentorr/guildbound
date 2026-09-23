package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.HeroAbility;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkillPayload(HeroAbility ability) implements CustomPacketPayload {
    public static final Type<SkillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillPayload> CODEC = StreamCodec.of(
            (b, p) -> b.writeEnum(p.ability), b -> new SkillPayload(b.readEnum(HeroAbility.class)));
    @Override public Type<SkillPayload> type() { return TYPE; }
}
