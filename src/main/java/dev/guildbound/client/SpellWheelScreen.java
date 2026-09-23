package dev.guildbound.client;

import dev.guildbound.combat.RadialLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SpellWheelScreen extends Screen {
    private int hovered = -1;
    public SpellWheelScreen() { super(Component.translatable("magic.guildbound.cantrips")); }
    private float scale() { return Math.min(1F, Math.min(width / 400F, height / 380F)); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float partial) {}
    @Override public void tick() {
        if (!MagicControls.hasAbilities() || minecraft.player == null || !minecraft.player.isAlive() || !minecraft.isWindowActive()) { onClose(); return; }
        if (MagicControls.binding().getKey().getType() != com.mojang.blaze3d.platform.InputConstants.Type.SCANCODE
                && !MagicControls.physicalHeld()) confirm();
    }
    @Override public void onClose() { MagicControls.wheelClosed(); super.onClose(); }
    @Override public void mouseMoved(double x, double y) {
        hovered = RadialLayout.pick((x - width / 2F) / scale(), (y - height / 2F) / scale(), MagicControls.available().length);
    }
    private void confirm() {
        if (hovered >= 0 && hovered < MagicControls.available().length && MagicControls.hasAbilities()) MagicControls.select(MagicControls.available()[hovered]);
        onClose();
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        if (MagicControls.binding().matches(key, scan)) { confirm(); return true; }
        return super.keyReleased(key, scan, modifiers);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (MagicControls.binding().matchesMouse(button)) { confirm(); return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseClicked(double x, double y, int button) { return true; }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        g.fill(0, 0, width, height, 0x8020292D);
        g.pose().pushPose();
        g.pose().translate(width / 2F, height / 2F, 200);
        g.pose().scale(scale(), scale(), 1);
        centered(g, font, title, 0, -155, AincradStyle.GOLD);
        centered(g, font, Component.translatable(MagicControls.circle()==1 ? "magic.guildbound.first_circle" : MagicControls.isMage() ? "magic.guildbound.cantrips" : "magic.guildbound.active_skill"), 0, -137, AincradStyle.MUTED);
        drawChoices(g, font, hovered < 0 ? java.util.Arrays.asList(MagicControls.available()).indexOf(MagicControls.selected()) : hovered, 0, 0, 104, MagicControls.cooldown());
        var options = MagicControls.available();
        var preview = hovered >= 0 && hovered < options.length ? options[hovered] : MagicControls.selected();
        int lineY = -32;
        for (var line : font.split(preview.label(), 140)) { g.drawString(font, line, -font.width(line)/2, lineY, AincradStyle.GOLD, false); lineY += font.lineHeight; }
        if (preview.skill != null) centered(g, font, Component.literal(preview.skill == dev.guildbound.progression.HeroAbility.ARCANE_BURST ? "10 MP/s · 10 s" : preview.skill.cost + (preview.skill.magical() ? " MP" : " SP")), 0, 39, AincradStyle.MUTED);
        centered(g, font, Component.translatable(preview.circle == 1 ? "magic.guildbound.first_circle" : MagicControls.isMage() ? "magic.guildbound.cantrips" : "magic.guildbound.active_skill"), 0, 18, AincradStyle.MUTED);
        centered(g, font, Component.translatable("magic.guildbound.release", MagicControls.binding().getTranslatedKeyMessage()), 0, 166, AincradStyle.TEXT);
        centered(g, font, Component.translatable("magic.guildbound.cancel"), 0, 182, AincradStyle.MUTED);
        g.flush();
        g.pose().popPose();
    }
    static void centered(GuiGraphics g, net.minecraft.client.gui.Font font, Component value, int x, int y, int color) {
        AincradStyle.text(g, font, value, x - font.width(value) / 2, y, color);
    }
    static void drawChoices(GuiGraphics g, net.minecraft.client.gui.Font font, int hovered, int cx, int cy, int radius) {
        drawChoices(g, font, hovered, cx, cy, radius, 0, java.util.Arrays.copyOf(MagicControls.available(), Math.min(2, MagicControls.available().length)));
    }
    private static void drawChoices(GuiGraphics g, net.minecraft.client.gui.Font font, int hovered, int cx, int cy, int radius, int cooldown) {
        drawChoices(g, font, hovered, cx, cy, radius, cooldown, MagicControls.available());
    }
    private static void drawChoices(GuiGraphics g, net.minecraft.client.gui.Font font, int hovered, int cx, int cy, int radius, int cooldown, MagicSpell[] spells) {
        for (int i = 0; i < 72; i++) {
            double angle = i * Math.PI * 2 / 72;
            int px = cx + (int)(Math.cos(angle) * radius), py = cy + (int)(Math.sin(angle) * radius);
            g.fill(px, py, px + 1, py + 1, 0x707F999A);
        }
        for (int i = 0; i < spells.length; i++) {
            int px = cx + (int)(Math.cos(RadialLayout.angle(i, spells.length)) * radius);
            int py = cy + (int)(Math.sin(RadialLayout.angle(i, spells.length)) * radius);
            boolean selected = hovered == i;
            g.fill(px - 24, py - 25, px + 24, py + 22, selected ? 0xFF876733 : 0xF02C383C);
            g.renderOutline(px - 24, py - 25, 48, 47, selected ? AincradStyle.GOLD : AincradStyle.MUTED);
            if (spells[i] == MagicSpell.HEAL) AincradStyle.abilityIcon(g, true, px - 6, py - 10, AincradStyle.MANA);
            else g.renderItem((spells[i].skill == null ? net.minecraft.world.item.Items.FIRE_CHARGE : spells[i].skill.magical() ? net.minecraft.world.item.Items.AMETHYST_SHARD : net.minecraft.world.item.Items.IRON_SWORD).getDefaultInstance(), px - 8, py - 11);
            int remaining = spells[i].skill == null ? dev.guildbound.combat.CantripCooldown.remaining(net.minecraft.client.Minecraft.getInstance().player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES), net.minecraft.client.Minecraft.getInstance().player.getData(dev.guildbound.data.ProgressAttachments.FIRE_COOLDOWN)) : net.minecraft.client.Minecraft.getInstance().player.getData(dev.guildbound.data.ProgressAttachments.ABILITIES).cooldown(spells[i].skill);
            if (spells[i].skill == dev.guildbound.progression.HeroAbility.ARCANE_BURST) remaining = 0;
            if (remaining > 0) {
                g.flush();
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(px - 20, py - 20, px + 20, py + 15, 0xDC22292C);
                centered(g, font, Component.literal(String.format(java.util.Locale.ROOT, "%.1f", remaining / 20F)), px, py - 5, AincradStyle.TEXT);
                g.flush();
                g.pose().popPose();
            }
            int labelY=py+28;
            for(var line:font.split(spells[i].label(),100)){g.drawString(font,line,px-font.width(line)/2,labelY,AincradStyle.TEXT,false);labelY+=font.lineHeight;}
        }
        centered(g, font, Component.literal("+"), cx, cy - 4, AincradStyle.GOLD);
    }
}

