package dev.guildbound.progression;

public enum HeroClass {
    RANGER, ROGUE, WARRIOR, MAGE;

    public String key() { return "class.guildbound." + name().toLowerCase(java.util.Locale.ROOT); }
}
