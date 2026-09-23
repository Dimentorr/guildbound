package dev.guildbound.combat;

/** One shared lock derived from existing persisted timers; selection cannot change it. */
public final class CantripCooldown {
    private CantripCooldown() {}
    public static int remaining(ResourceState resources, int fireCooldown) {
        return Math.max(resources.healCooldown(), fireCooldown);
    }
}

