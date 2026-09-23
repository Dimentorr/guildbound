package dev.guildbound.progression;

import java.util.Locale;

public enum Talent {
    VITALITY(null), ENDURANCE(null), MARKSMAN(HeroClass.RANGER),
    KNIFEWORK(HeroClass.ROGUE), BULWARK(HeroClass.WARRIOR), RESTORATION(HeroClass.MAGE), EMBER(HeroClass.MAGE),
    RESILIENCE(null), MEDITATION(null),
    HUNTER_TRAINING(HeroClass.RANGER), DUELIST_TRAINING(HeroClass.ROGUE),
    GUARDIAN_TRAINING(HeroClass.WARRIOR), SORCERER_TRAINING(HeroClass.MAGE),
    ARCANE_BURST(HeroSubclass.SORCERER, 5),
    ARCANE_WARD(HeroSubclass.SORCERER, 5),
    MAGIC_MISSILE(HeroSubclass.WIZARD, 5),
    FROST_NOVA(HeroSubclass.WIZARD, 5),
    SONG_STRENGTH(HeroSubclass.BARD, 5),
    SONG_SPEED(HeroSubclass.BARD, 5),
    SONG_RESISTANCE(HeroSubclass.BARD, 5),
    HEALING_FIELD(HeroSubclass.BARD, 5),
    HOLY_FIELD(HeroSubclass.PRIEST, 5),
    GREAT_HEAL(HeroSubclass.PRIEST, 5),
    RAISE_ZOMBIE(HeroSubclass.NECROMANCER, 5),
    RAISE_SKELETON(HeroSubclass.NECROMANCER, 5),
    RAISE_WITHER(HeroSubclass.NECROMANCER, 25),
    TAUNT(HeroSubclass.GUARDIAN, 5),
    RAGE(HeroSubclass.BERSERKER, 5),
    RUNE_BLADE(HeroSubclass.RUNE_WARRIOR, 5),
    SHADOW_STEP(HeroSubclass.SHADOW, 5),
    VANISH(HeroSubclass.SHADOW, 5),
    TRAP(HeroSubclass.BURGLAR, 5),
    SMOKE(HeroSubclass.BURGLAR, 5),
    TOXIC_BLADE(HeroSubclass.POISONER, 5),
    CALL_BEAST(HeroSubclass.BEASTMASTER, 5),
    CALL_PACK(HeroSubclass.PACK_LEADER, 5),
    CALL_MONSTER(HeroSubclass.MONSTER_TAMER, 5),
    WILD_COMPANION(HeroSubclass.WILD_HUNTER, 5),
    WILD_LEAP(HeroSubclass.WILD_HUNTER, 5),
    HUNTER_MARK(HeroSubclass.HUNTER, 5),
    DUELIST_RIPOSTE(HeroSubclass.DUELIST, 5),
    TRIPWIRE(HeroSubclass.BURGLAR, 10), SWAP_BEAST(HeroSubclass.WILD_HUNTER, 10),
    SPELL_WEAVING(HeroSubclass.SORCERER, 15),
HEART_II(null, 10),
HEART_III(null, 25),
HEART_IV(null, 50),
GUARD_II(null, 10),
GUARD_III(null, 25),
GUARD_IV(null, 50),
BREATH_II(null, 10),
BREATH_III(null, 25),
BREATH_IV(null, 50),
MIND_II(null, 10),
MIND_III(null, 25),
MIND_IV(null, 50),
FIELD_POWER(HeroSubclass.SORCERER, 10),
FIELD_POWER_II(HeroSubclass.SORCERER, 25),
FIELD_POWER_III(HeroSubclass.SORCERER, 50),
WARD_TIME(HeroSubclass.SORCERER, 10),
WARD_TIME_II(HeroSubclass.SORCERER, 25),
WARD_TIME_III(HeroSubclass.SORCERER, 50),
LIFE_II(HeroSubclass.SORCERER, 10),
LIFE_III(HeroSubclass.SORCERER, 25),
LIFE_IV(HeroSubclass.SORCERER, 50),
SORC_MIND(HeroSubclass.SORCERER, 10),
SORC_MIND_II(HeroSubclass.SORCERER, 25),
SORC_MIND_III(HeroSubclass.SORCERER, 50);

    private HeroSubclass specialization;
    private int unlockLevel;
    private final HeroClass heroClass;
    Talent(HeroClass heroClass) { this.heroClass = heroClass; }
    Talent(HeroSubclass subclass, int level) { this.heroClass = subclass == null ? null : subclass.parent(); this.specialization = subclass; this.unlockLevel = level; }
    public HeroClass heroClass() { return heroClass; }
    public String key() { return "talent.guildbound." + name().toLowerCase(Locale.ROOT); }
    public int requiredLevel(int nextRank) { return unlockLevel > 0 ? unlockLevel + (nextRank - 1) * 5 : heroClass == null ? 1 : (subclass() != null ? 5 : 3) + (nextRank - 1) * 2; }
    public int cost(int nextRank) { return nextRank * (ordinal() > SPELL_WEAVING.ordinal() ? Math.max(2, unlockLevel / 5) : 1); }
    public HeroSubclass subclass() {
        return switch (this) {
            case HUNTER_TRAINING -> HeroSubclass.HUNTER;
            case DUELIST_TRAINING -> HeroSubclass.DUELIST;
            case GUARDIAN_TRAINING -> HeroSubclass.GUARDIAN;
            case SORCERER_TRAINING -> HeroSubclass.SORCERER;
            default -> specialization;
        };
    }
    public Talent prerequisite() {
        return switch (this) {
            case HEART_II -> VITALITY;
            case HEART_III -> HEART_II;
            case HEART_IV -> HEART_III;
            case GUARD_II -> RESILIENCE;
            case GUARD_III -> GUARD_II;
            case GUARD_IV -> GUARD_III;
            case BREATH_II -> ENDURANCE;
            case BREATH_III -> BREATH_II;
            case BREATH_IV -> BREATH_III;
            case MIND_II -> MEDITATION;
            case MIND_III -> MIND_II;
            case MIND_IV -> MIND_III;
            case FIELD_POWER -> ARCANE_BURST;
            case FIELD_POWER_II -> FIELD_POWER;
            case FIELD_POWER_III -> FIELD_POWER_II;
            case WARD_TIME -> ARCANE_WARD;
            case WARD_TIME_II -> WARD_TIME;
            case WARD_TIME_III -> WARD_TIME_II;
            case LIFE_II -> SORCERER_TRAINING;
            case LIFE_III -> LIFE_II;
            case LIFE_IV -> LIFE_III;
            case SORC_MIND -> ARCANE_WARD;
            case SORC_MIND_II -> SORC_MIND;
            case SORC_MIND_III -> SORC_MIND_II;
            case RESILIENCE -> VITALITY;
            case MEDITATION -> ENDURANCE;
            case HUNTER_TRAINING -> MARKSMAN;
            case DUELIST_TRAINING -> KNIFEWORK;
            case GUARDIAN_TRAINING -> BULWARK;
            case SORCERER_TRAINING -> RESTORATION;
            default -> null;
        };
    }
    public static Talent specialization(HeroClass heroClass) {
        return switch (heroClass) {
            case RANGER -> MARKSMAN;
            case ROGUE -> KNIFEWORK;
            case WARRIOR -> BULWARK;
            case MAGE -> RESTORATION;
        };
    }
}
