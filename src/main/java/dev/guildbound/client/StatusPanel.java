package dev.guildbound.client;

import dev.guildbound.combat.ResourceState;
import dev.guildbound.progression.CharacterProgress;
import dev.guildbound.progression.HeroClass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class StatusPanel {
    public static final int WIDTH = 244;
    public static final int HEIGHT = 125;

    private StatusPanel() {}

    public static void draw(GuiGraphics graphics, Font font, String name, float health, float maxHealth,
                            CharacterProgress progress, ResourceState resources) {
        var track = progress.tracks().getFirst();
        boolean mage = progress.tracks().stream().anyMatch(value -> value.heroClass() == HeroClass.MAGE);
        graphics.fill(0, 0, WIDTH, HEIGHT, 0, backgroundColor());
        graphics.fill(0, 0, 2, HEIGHT, 1, AincradStyle.GOLD);
        AincradStyle.classIcon(graphics, track.heroClass(), track.subclass(), 7, 3, 20);
        HudText.draw(graphics, font, font.plainSubstrByWidth(name, 127), 39, 6, AincradStyle.TEXT);
        String level = "Lv. " + track.level();
        HudText.draw(graphics, font, level, WIDTH - 9 - HudText.width(font,level), 6, AincradStyle.GOLD);
        var player = net.minecraft.client.Minecraft.getInstance().player;
        float absorption = player == null ? 0 : player.getAbsorptionAmount();
        String healthValue = Math.round(health) + " / " + Math.round(maxHealth)
                + (absorption > 0 ? " + " + Math.round(absorption) : "");
        row(graphics, font, "HP", healthValue, ClientPreferences.BAR_ORDER.get().y("HP"),
                maxHealth > 0 ? health / maxHealth : 0, health <= maxHealth * 0.25F ? 0xFFE16C55 : AincradStyle.HEALTH);
        row(graphics, font, "MP", mage ? resources.mana() / 10 + " / " + dev.guildbound.combat.LevelStats.mana(progress.totalLevel())/10 : "—", ClientPreferences.BAR_ORDER.get().y("MP"),
                mage ? resources.mana() / (float) dev.guildbound.combat.LevelStats.mana(progress.totalLevel()) : 0, AincradStyle.MANA);
        row(graphics, font, "SP", resources.stamina() / 10 + " / " + dev.guildbound.combat.LevelStats.stamina(progress.totalLevel())/10, ClientPreferences.BAR_ORDER.get().y("SP"),
                resources.stamina() / (float) dev.guildbound.combat.LevelStats.stamina(progress.totalLevel()), resources.exhausted() ? 0xFFE16C55 : AincradStyle.STAMINA);
        boolean capped = progress.totalLevel() >= CharacterProgress.MAX_LEVEL;
        row(graphics, font, "XP", capped ? "MAX" : progress.experience() + " / " + progress.nextLevelCost(), 71,
                capped ? 1 : progress.experience() / (float) progress.nextLevelCost(), AincradStyle.EXPERIENCE);
        if (player != null) {
            int food = player.getFoodData().getFoodLevel();
            row(graphics, font, net.minecraft.network.chat.Component.translatable("hud.guildbound.food").getString(), food + " / 20", 88, food / 20F, 0xFFD59A56);
            row(graphics, font, net.minecraft.network.chat.Component.translatable("hud.guildbound.thirst").getString(), "—", 105, 0, AincradStyle.MANA);
        }
    }

    public static int backgroundColor() { return ((int)Math.round(ClientPreferences.HUD_OPACITY.get()*255) << 24) | 0x2C3030; }

    private static void row(GuiGraphics graphics, Font font, String label, String value, int y, float ratio, int color) {
        HudText.draw(graphics, font, label, 7, y + 6, AincradStyle.MUTED);
        HudText.smallRight(graphics,font,value,234,y,AincradStyle.TEXT);
        AincradStyle.bar(graphics, 39, y + 6, 196, ratio, color);
    }
}
