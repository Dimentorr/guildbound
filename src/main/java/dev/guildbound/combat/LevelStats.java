package dev.guildbound.combat;
/** Fixed-point resource caps; level one retains the original base statistics. */
public final class LevelStats {
 private LevelStats() {}
 private static int level(int l){return Math.clamp(l,1,100);}
 public static int stamina(int l){return 1000+20*(level(l)-1);}
 public static int mana(int l){return 1000+30*(level(l)-1);}
 public static int healthBonus(int l){return 2*(level(l)/10);}
}
