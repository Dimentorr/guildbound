package dev.guildbound.progression;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.EnumMap;

public record AbilityState(Map<HeroAbility, Integer> remaining) {
    private static final Codec<HeroAbility> KEY = Codec.STRING.comapFlatMap(s -> {
        try { return com.mojang.serialization.DataResult.success(HeroAbility.valueOf(s)); }
        catch (IllegalArgumentException e) { return com.mojang.serialization.DataResult.error(() -> "Unknown ability"); }
    }, Enum::name);
    public static final Codec<AbilityState> CODEC = Codec.unboundedMap(KEY, Codec.intRange(1, 72000))
            .xmap(AbilityState::new, AbilityState::remaining);
    public AbilityState {
        remaining = Map.copyOf(remaining);
        for (int time : remaining.values()) if (time < 1 || time > 72000) throw new IllegalArgumentException("Invalid cooldown");
    }
    public static AbilityState ready() { return new AbilityState(Map.of()); }
    public int cooldown(HeroAbility ability) { return ability.magical() ? remaining.entrySet().stream().filter(e -> e.getKey().magical() && e.getKey().circle() == ability.circle() && e.getKey() != HeroAbility.ARCANE_BURST).mapToInt(Map.Entry::getValue).max().orElse(0) : remaining.getOrDefault(ability, 0); }
    public AbilityState start(HeroAbility ability, int ticks) {
        var map = new EnumMap<HeroAbility, Integer>(HeroAbility.class);
        map.putAll(remaining); if (ticks > 0) map.put(ability, ticks); else map.remove(ability);
        return new AbilityState(map);
    }
    public AbilityState tick(int ticks) {
        var map = new EnumMap<HeroAbility, Integer>(HeroAbility.class);
        remaining.forEach((key, value) -> { if (value > ticks) map.put(key, value - ticks); });
        return new AbilityState(map);
    }
}
