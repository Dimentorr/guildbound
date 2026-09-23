package dev.guildbound.client;

import dev.guildbound.network.UseAbilityPayload;
public enum MagicSpell {
    TRIPWIRE(dev.guildbound.progression.HeroAbility.TRIPWIRE), SWAP_BEAST(dev.guildbound.progression.HeroAbility.SWAP_BEAST),
    HEAL("heal_title", UseAbilityPayload.Ability.HEAL, 0),
    EMBER("fire_title", UseAbilityPayload.Ability.FIRE, 0),
    ARCANE_BURST(dev.guildbound.progression.HeroAbility.ARCANE_BURST),
    ARCANE_WARD(dev.guildbound.progression.HeroAbility.ARCANE_WARD),
    MAGIC_MISSILE(dev.guildbound.progression.HeroAbility.MAGIC_MISSILE),
    FROST_NOVA(dev.guildbound.progression.HeroAbility.FROST_NOVA),
    SONG_STRENGTH(dev.guildbound.progression.HeroAbility.SONG_STRENGTH),
    SONG_SPEED(dev.guildbound.progression.HeroAbility.SONG_SPEED),
    SONG_RESISTANCE(dev.guildbound.progression.HeroAbility.SONG_RESISTANCE),
    HEALING_FIELD(dev.guildbound.progression.HeroAbility.HEALING_FIELD),
    HOLY_FIELD(dev.guildbound.progression.HeroAbility.HOLY_FIELD),
    GREAT_HEAL(dev.guildbound.progression.HeroAbility.GREAT_HEAL),
    RAISE_ZOMBIE(dev.guildbound.progression.HeroAbility.RAISE_ZOMBIE),
    RAISE_SKELETON(dev.guildbound.progression.HeroAbility.RAISE_SKELETON),
    RAISE_WITHER(dev.guildbound.progression.HeroAbility.RAISE_WITHER),
    TAUNT(dev.guildbound.progression.HeroAbility.TAUNT),
    RAGE(dev.guildbound.progression.HeroAbility.RAGE),
    RUNE_BLADE(dev.guildbound.progression.HeroAbility.RUNE_BLADE),
    SHADOW_STEP(dev.guildbound.progression.HeroAbility.SHADOW_STEP),
    VANISH(dev.guildbound.progression.HeroAbility.VANISH),
    TRAP(dev.guildbound.progression.HeroAbility.TRAP),
    SMOKE(dev.guildbound.progression.HeroAbility.SMOKE),
    TOXIC_BLADE(dev.guildbound.progression.HeroAbility.TOXIC_BLADE),
    CALL_BEAST(dev.guildbound.progression.HeroAbility.CALL_BEAST),
    CALL_PACK(dev.guildbound.progression.HeroAbility.CALL_PACK),
    CALL_MONSTER(dev.guildbound.progression.HeroAbility.CALL_MONSTER),
    WILD_COMPANION(dev.guildbound.progression.HeroAbility.WILD_COMPANION),
    WILD_LEAP(dev.guildbound.progression.HeroAbility.WILD_LEAP),
    HUNTER_MARK(dev.guildbound.progression.HeroAbility.HUNTER_MARK),
    DUELIST_RIPOSTE(dev.guildbound.progression.HeroAbility.DUELIST_RIPOSTE);
    public final dev.guildbound.progression.HeroAbility skill;
    public final String title;
    public final UseAbilityPayload.Ability ability;
    /** Zero means cantrip; positive values are reserved for future spell circles. */
    public final int circle;
    MagicSpell(String title, UseAbilityPayload.Ability ability, int circle) {
        this.skill = null; this.title = title; this.ability = ability; this.circle = circle;
    }
    MagicSpell(dev.guildbound.progression.HeroAbility skill) { this.skill = skill; this.title = skill.key(); this.ability = null; this.circle = skill.circle(); }
    public static MagicSpell[] available(dev.guildbound.progression.CharacterProgress p) {
        return java.util.Arrays.stream(values()).filter(s -> s.skill != null ? s.skill.available(p) : p.tracks().stream().anyMatch(t -> t.heroClass() == dev.guildbound.progression.HeroClass.MAGE)).toArray(MagicSpell[]::new);
    }
    public net.minecraft.network.chat.Component label() {
        return net.minecraft.network.chat.Component.translatable(skill == null ? "screen.guildbound." + title : skill.key());
    }
}
