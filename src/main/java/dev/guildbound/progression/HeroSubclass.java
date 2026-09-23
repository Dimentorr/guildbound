package dev.guildbound.progression;

public enum HeroSubclass {
    HUNTER(HeroClass.RANGER), DUELIST(HeroClass.ROGUE),
    GUARDIAN(HeroClass.WARRIOR), SORCERER(HeroClass.MAGE),
    WIZARD(HeroClass.MAGE), BARD(HeroClass.MAGE), PRIEST(HeroClass.MAGE), NECROMANCER(HeroClass.MAGE),
    BERSERKER(HeroClass.WARRIOR), RUNE_WARRIOR(HeroClass.WARRIOR),
    SHADOW(HeroClass.ROGUE), BURGLAR(HeroClass.ROGUE), POISONER(HeroClass.ROGUE),
    BEASTMASTER(HeroClass.RANGER), PACK_LEADER(HeroClass.RANGER), MONSTER_TAMER(HeroClass.RANGER), WILD_HUNTER(HeroClass.RANGER);

    private final HeroClass parent;

    HeroSubclass(HeroClass parent) { this.parent = parent; }
    public HeroClass parent() { return parent; }
    public String key() { return "subclass.guildbound." + name().toLowerCase(java.util.Locale.ROOT); }
}
