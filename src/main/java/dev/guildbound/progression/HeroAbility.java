package dev.guildbound.progression;

/** Costs are whole resource points; cooldowns are ticks. No client-supplied balance values. */
public enum HeroAbility {
    ARCANE_BURST(HeroSubclass.SORCERER, 5, 0, 0), ARCANE_WARD(HeroSubclass.SORCERER, 5, 30, 400),
    MAGIC_MISSILE(HeroSubclass.WIZARD, 5, 20, 100), FROST_NOVA(HeroSubclass.WIZARD, 5, 35, 300),
    SONG_STRENGTH(HeroSubclass.BARD, 5, 25, 300), SONG_SPEED(HeroSubclass.BARD, 5, 20, 240),
    SONG_RESISTANCE(HeroSubclass.BARD, 5, 30, 400), HEALING_FIELD(HeroSubclass.BARD, 5, 40, 500),
    HOLY_FIELD(HeroSubclass.PRIEST, 5, 40, 500), GREAT_HEAL(HeroSubclass.PRIEST, 5, 35, 180),
    RAISE_ZOMBIE(HeroSubclass.NECROMANCER, 5, 35, 400), RAISE_SKELETON(HeroSubclass.NECROMANCER, 5, 40, 500),
    RAISE_WITHER(HeroSubclass.NECROMANCER, 25, 60, 800),
    TAUNT(HeroSubclass.GUARDIAN, 5, 25, 240), RAGE(HeroSubclass.BERSERKER, 5, 30, 500),
    RUNE_BLADE(HeroSubclass.RUNE_WARRIOR, 5, 25, 320),
    SHADOW_STEP(HeroSubclass.SHADOW, 5, 20, 160), VANISH(HeroSubclass.SHADOW, 5, 30, 400),
    TRAP(HeroSubclass.BURGLAR, 5, 20, 240), SMOKE(HeroSubclass.BURGLAR, 5, 25, 300), TRIPWIRE(HeroSubclass.BURGLAR, 10, 25, 300),
    TOXIC_BLADE(HeroSubclass.POISONER, 5, 25, 320),
    CALL_BEAST(HeroSubclass.BEASTMASTER, 5, 30, 500), CALL_PACK(HeroSubclass.PACK_LEADER, 5, 40, 600),
    CALL_MONSTER(HeroSubclass.MONSTER_TAMER, 5, 50, 700),
    WILD_COMPANION(HeroSubclass.WILD_HUNTER, 5, 30, 500), WILD_LEAP(HeroSubclass.WILD_HUNTER, 5, 20, 160), SWAP_BEAST(HeroSubclass.WILD_HUNTER, 10, 25, 240),
    HUNTER_MARK(HeroSubclass.HUNTER, 5, 20, 160), DUELIST_RIPOSTE(HeroSubclass.DUELIST, 5, 20, 240);

    public final HeroSubclass subclass;
    public final int level, cost, cooldown;
    HeroAbility(HeroSubclass subclass, int level, int cost, int cooldown) {
        this.subclass = subclass; this.level = level; this.cost = cost; this.cooldown = cooldown;
    }
    public boolean available(CharacterProgress p) {
        return p.tracks().stream().anyMatch(t -> t.subclass() == subclass && t.level() >= level);
    }
    public boolean magical() { return subclass.parent() == HeroClass.MAGE; }
    public int circle() { return magical() ? 1 : 0; }
    public String key() { return "ability.guildbound." + name().toLowerCase(java.util.Locale.ROOT); }
    public Talent talent() { return Talent.valueOf(name()); }
    public int cooldown(int rank) { return cooldown * (10 - rank) / 10; }
    public float power(int rank) { return 1 + .2F * rank; }
}
