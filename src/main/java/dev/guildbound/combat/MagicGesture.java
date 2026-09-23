package dev.guildbound.combat;

/** Tap casts on release; holding only opens selection. Losing input focus cancels. */
public final class MagicGesture {
    public enum Action { NONE, CAST, OPEN }
    private long pressedAt = -1;
    public Action update(boolean started, boolean held, boolean allowed, long nowMillis) {
        if (!allowed) { pressedAt = -1; return Action.NONE; }
        if (started && pressedAt < 0) {
            if (!held) return Action.CAST;
            pressedAt = nowMillis;
        }
        if (pressedAt < 0) return Action.NONE;
        if (!held) {
            boolean longPress = nowMillis - pressedAt >= 250;
            pressedAt = -1;
            return longPress ? Action.OPEN : Action.CAST;
        }
        if (nowMillis - pressedAt >= 250) { pressedAt = -1; return Action.OPEN; }
        return Action.NONE;
    }
}
