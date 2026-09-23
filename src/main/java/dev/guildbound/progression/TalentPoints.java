package dev.guildbound.progression;

public final class TalentPoints {
    private TalentPoints() {}
    public static int total(int level) {
        if (level < 0 || level > CharacterProgress.MAX_LEVEL) throw new IllegalArgumentException("Invalid level");
        if (level == 0) return 0;
        int points = 5;
        for (int earned = 2; earned <= level; earned++)
            points += earned <= 5 ? 1 : earned <= 10 ? 2 : earned <= 20 ? 3 : earned <= 45 ? 5
                    : earned < 75 ? 10 : earned < 90 ? 20 : earned < 100 ? 25 : 50;
        return points;
    }
}
