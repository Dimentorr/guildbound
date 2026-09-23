package dev.guildbound.combat;

/** Per-hand cooldowns in server ticks. Equipment swaps never reset this state. */
public record DaggerState(int main, int off, int shared, boolean nextOff) {
    public DaggerState {
        if (main < 0 || main > 200 || off < 0 || off > 200 || shared < 0 || shared > 200)
            throw new IllegalArgumentException("Invalid dagger cooldown");
    }
    public static DaggerState ready() { return new DaggerState(0, 0, 0, false); }
    public boolean useOff(boolean dual) { return dual && nextOff; }
    public boolean canAttack(boolean dual) { return shared == 0 && (useOff(dual) ? off : main) == 0; }
    public boolean canAttack(boolean dual, boolean offOnly) { return shared == 0 && ((offOnly || useOff(dual)) ? off : main) == 0; }
    public DaggerState tick() { return new DaggerState(Math.max(0, main - 1), Math.max(0, off - 1), Math.max(0, shared - 1), nextOff); }
    public static int duration(double speed) { return (int) Math.clamp(Math.ceil(20 / Math.max(.1, speed)), 1, 200); }
    public DaggerState attack(boolean dual, int mainDuration, int offDuration, boolean landed) {
        return attack(dual, false, mainDuration, offDuration, landed);
    }
    public DaggerState attack(boolean dual, boolean offOnly, int mainDuration, int offDuration, boolean landed) {
        if (mainDuration < 1 || mainDuration > 200 || offDuration < 1 || offDuration > 200)
            throw new IllegalArgumentException("Invalid duration");
        if (!canAttack(dual, offOnly)) return this;
        boolean offHand = offOnly || useOff(dual);
        int nextMain = offHand ? main : mainDuration;
        int nextOffTime = offHand ? offDuration : off;
        if (dual && landed) {
            if (offHand) nextMain = Math.max(0, nextMain - (int) Math.ceil(mainDuration * .25));
            else nextOffTime = Math.max(0, nextOffTime - (int) Math.ceil(offDuration * .25));
        }
        int gap = dual ? Math.max(1, Math.min(mainDuration, offDuration) / 2) : offOnly ? offDuration : mainDuration;
        return new DaggerState(nextMain, nextOffTime, gap, dual && !offHand);
    }
}
