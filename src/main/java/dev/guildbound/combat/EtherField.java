package dev.guildbound.combat;

/** Per-tick channel accounting: 0.5 mana/tick, one pulse every 12 paid ticks. */
public record EtherField(int elapsed) {
    public static final int DURATION = 200, COST = 5, INTERVAL = 12;
    public boolean canTick(int mana) { return elapsed < DURATION && mana >= COST; }
    public EtherField tick() { return new EtherField(elapsed + 1); }
    public boolean pulse() { return elapsed > 0 && elapsed % INTERVAL == 0; }
    public static float damage(int ranks) { return 6 + ranks; }
}
