package dev.guildbound.combat;

/** Fixed-point resources: ten units equal one point on the HUD. All costs are server-owned. */
public record ResourceState(int stamina, int mana, int recoveryDelay, int dodgeCooldown, int healCooldown, boolean exhausted) {
    public static final int MAX = 1000;
    public static final int STORAGE_LIMIT = 10000;
    public static final int ATTACK_COST = 40;
    public static final int JUMP_COST = 20;
    public static final int DODGE_COST = 120;
    public static final int HEAL_COST = 250;
    public static final int SPRINT_DRAIN = 2;
    public static final int SHIELD_SPRINT_DRAIN = 8;
    public static final int STAMINA_REGEN = 10;
    public static final int RECOVERY_DELAY = 8;
    public static final int SPRINT_RECOVERY_THRESHOLD = 150;

    public ResourceState {
        if (stamina < 0 || stamina > STORAGE_LIMIT || mana < 0 || mana > STORAGE_LIMIT
                || recoveryDelay < 0 || recoveryDelay > 40 || dodgeCooldown < 0 || dodgeCooldown > 20
                || healCooldown < 0 || healCooldown > 100) throw new IllegalArgumentException("Invalid resources");
    }

    public static ResourceState full() { return new ResourceState(MAX, MAX, 0, 0, 0, false); }
    public boolean canSprint() { return !exhausted && stamina >= SPRINT_DRAIN; }
    public boolean canSprint(boolean shieldRaised) {
        return !exhausted && stamina >= (shieldRaised ? SHIELD_SPRINT_DRAIN : SPRINT_DRAIN);
    }
    public boolean canAttack() { return stamina >= ATTACK_COST; }
    public boolean canDodge() { return !exhausted && stamina >= DODGE_COST && dodgeCooldown == 0; }
    public boolean cooldownFinishedSince(ResourceState previous) {
        return healCooldown == 0 && previous.healCooldown > 0 || dodgeCooldown == 0 && previous.dodgeCooldown > 0;
    }
    public boolean canHeal() { return mana >= HEAL_COST && healCooldown == 0; }

    public ResourceState spendStamina(int cost) {
        if (cost < 0 || cost > MAX) throw new IllegalArgumentException("Invalid stamina cost");
        int next = Math.max(0, stamina - cost);
        return new ResourceState(next, mana, RECOVERY_DELAY, dodgeCooldown, healCooldown, exhausted || next < SPRINT_DRAIN);
    }

    public ResourceState dodge() {
        if (!canDodge()) return this;
        var spent = spendStamina(DODGE_COST);
        return new ResourceState(spent.stamina, mana, spent.recoveryDelay, 20, healCooldown, spent.exhausted);
    }

    public ResourceState heal() {
        if (!canHeal()) return this;
        return new ResourceState(stamina, mana - HEAL_COST, recoveryDelay, dodgeCooldown, 100, exhausted);
    }

    public ResourceState tick(boolean sprinting) {
        return tick(sprinting, false);
    }

    public ResourceState tick(boolean sprinting, boolean shieldRaised) {
        return tick(sprinting, shieldRaised, 0);
    }

    public ResourceState tick(boolean sprinting, boolean shieldRaised, int enduranceRank) {
        return tick(sprinting, shieldRaised, enduranceRank, 0);
    }

    public ResourceState tick(boolean sprinting, boolean shieldRaised, int enduranceRank, int meditationRank) {
        return tick(sprinting,shieldRaised,enduranceRank,meditationRank,MAX,MAX);
    }
    public ResourceState tick(boolean sprinting, boolean shieldRaised, int enduranceRank, int meditationRank, int staminaMax, int manaMax) {
        if(staminaMax<MAX||manaMax<MAX||staminaMax>STORAGE_LIMIT||manaMax>STORAGE_LIMIT) throw new IllegalArgumentException("Invalid resource cap");
        if (enduranceRank < 0 || enduranceRank > 20) throw new IllegalArgumentException("Invalid endurance rank");
        if (meditationRank < 0 || meditationRank > 20) throw new IllegalArgumentException("Invalid meditation rank");
        int drain = shieldRaised ? SHIELD_SPRINT_DRAIN : SPRINT_DRAIN;
        int nextStamina = stamina;
        int delay = Math.max(0, recoveryDelay - 1);
        if (sprinting && canSprint(shieldRaised)) {
            nextStamina = Math.max(0, stamina - drain);
            delay = RECOVERY_DELAY;
        } else if (recoveryDelay == 0) nextStamina = Math.min(staminaMax, stamina + STAMINA_REGEN + 2 * enduranceRank);
        boolean tired = exhausted || nextStamina < SPRINT_DRAIN
                || (sprinting && nextStamina < drain);
        if (nextStamina >= SPRINT_RECOVERY_THRESHOLD) tired = false;
        return new ResourceState(Math.min(staminaMax,nextStamina), Math.min(manaMax, mana + 1 + meditationRank), delay,
                Math.max(0, dodgeCooldown - 1), Math.max(0, healCooldown - 1), tired);
    }
}
