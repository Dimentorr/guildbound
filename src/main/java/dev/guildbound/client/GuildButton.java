package dev.guildbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import dev.guildbound.progression.HeroSubclass;

/** Shared flat, gold-accented controls for floating guild panels. */
final class GuildButton extends Button {
    HeroSubclass emblem;
    boolean selected;
    GuildButton(int x, int y, int w, int h, Component text, OnPress action) {
        super(x, y, w, h, text, action, DEFAULT_NARRATION);
    }
    static GuildButton from(Button b) {
        return new GuildButton(b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.getMessage(), ignored -> b.onPress());
    }
    @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
        int color = !active ? 0xFF667477 : selected || isHoveredOrFocused() ? AincradStyle.GOLD : 0xFF758C92;
        g.fill(getX(), getY(), getX()+width, getY()+height, selected || isHoveredOrFocused() ? 0xEE45504F : 0xE525363E);
        g.renderOutline(getX(), getY(), width, height, color);
        g.fill(getX(), getY()+3, getX()+2, getY()+height-3, color);
        if (emblem != null) AincradStyle.classIcon(g, emblem.parent(), emblem, getX()+7, getY()+(height-22)/2, 22);
        if (width <= 24) {
            int cx=getX()+width/2,cy=getY()+height/2;
            g.fill(cx-5,cy,cx+6,cy+2,color);
            for(int i=0;i<5;i++) {g.fill(cx-5+i,cy-i,cx-3+i,cy-i+1,color);g.fill(cx-5+i,cy+1+i,cx-3+i,cy+2+i,color);}
            return;
        }
        var font = Minecraft.getInstance().font;
        int left = getX() + (emblem == null ? 8 : 35), available = width - (left-getX()) - 8;
        var label = font.plainSubstrByWidth(getMessage().getString(), available);
        AincradStyle.text(g, font, label, left + Math.max(0, (available-font.width(label))/2), getY()+(height-8)/2,
                active ? AincradStyle.TEXT : AincradStyle.MUTED);
    }
}
