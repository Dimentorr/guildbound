package dev.guildbound.progression;

public record ClassTrack(HeroClass heroClass, HeroSubclass subclass, int level) {
    public ClassTrack {
        if (heroClass == null || subclass != null && subclass.parent() != heroClass
                || level < 1 || level > CharacterProgress.MAX_LEVEL) {
            throw new IllegalArgumentException("Invalid class track");
        }
        // Legacy characters below level five retain all base abilities, but choose a specialization later.
        if (level < 5) subclass = null;
    }
    public ClassTrack(HeroSubclass subclass, int level) { this(subclass.parent(), subclass, level); }
    public ClassTrack(HeroClass heroClass, int level) { this(heroClass, null, level); }
}
