package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.*;
import java.util.EnumMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkillsPayload(AbilityState state) implements CustomPacketPayload {
    public static final Type<SkillsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, "skills"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillsPayload> CODEC = StreamCodec.of((b, p) -> {
        for (var a : HeroAbility.values()) b.writeVarInt(p.state.cooldown(a));
    }, b -> {
        var map = new EnumMap<HeroAbility, Integer>(HeroAbility.class);
        for (var a : HeroAbility.values()) { int ticks = b.readVarInt(); if (ticks < 0 || ticks > 72000) throw new IllegalArgumentException("Cooldown"); if (ticks > 0) map.put(a, ticks); }
        return new SkillsPayload(new AbilityState(map));
    });
    @Override public Type<SkillsPayload> type() { return TYPE; }
}
