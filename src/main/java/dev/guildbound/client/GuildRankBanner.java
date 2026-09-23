package dev.guildbound.client;

import dev.guildbound.progression.CharacterProgress;
import dev.guildbound.progression.GuildRank;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Narrow heraldic ribbon; the full rank description belongs at the guild desk. */
final class GuildRankBanner {
    private GuildRankBanner() {}
    static void drawCompact(GuiGraphics g, Font font, CharacterProgress progress, int x, int y) {
        draw(g, font, progress, x, y);
    }
    static void draw(GuiGraphics g, Font font, CharacterProgress progress, int x, int y) {
        var rank = GuildRank.atLevel(progress.totalLevel());
        if (rank == null) return;
        int width = 40;
        g.fill(x - 3, y, x + width + 3, y + 3, 3, rank.color);
        g.fill(x, y + 4, x + width, y + 72, 2, 0xDD263139);
        g.fill(x + 2, y + 5, x + 3, y + 71, 3, rank.color);
        g.fill(x + width - 3, y + 5, x + width - 2, y + 71, 3, rank.color);
        for (int row = 0; row < 18; row++)
            g.fill(x + row, y + 72 + row, x + width - row, y + 73 + row, 2, 0xDD263139);
        g.pose().pushPose();
        g.pose().translate(0, 0, 10);
        emblem(g, rank, x + width / 2, y + 28);
        var level = Component.literal("Lv." + progress.totalLevel());
        HudText.centered(g,font,level,x,y+59,width,AincradStyle.TEXT);
        g.pose().popPose();
    }
    static void drawHanging(GuiGraphics g,Font font,CharacterProgress progress,int x,int y) {
        var rank=GuildRank.atLevel(progress.totalLevel());if(rank==null)return;
        HudShapes.polygon(g,0xB5233039,x,y,x+32,y,x+32,y+53,x+16,y+65,x,y+53);
        HudShapes.outline(g,rank.color,.75F,x,y,x+32,y,x+32,y+53,x+16,y+65,x,y+53);
        HudShapes.line(g,x+3,y+3,x+3,y+51,.45F,0x758F9C9B);
        HudShapes.line(g,x+29,y+3,x+29,y+51,.45F,0x758F9C9B);
        g.pose().pushPose();g.pose().translate(x+16,y+23,0);g.pose().scale(.72F,.72F,1);
        emblem(g,rank,0,0);g.pose().popPose();
        var level=Component.literal(Integer.toString(progress.totalLevel()));
        HudText.draw(g,font,level,x+(32-HudText.width(font,level))/2,y+46,AincradStyle.TEXT);
    }
    private static void emblem(GuiGraphics g, GuildRank rank, int x, int y) {
        int color = rank.color;
        // Shield outline, central star, and increasingly elaborate heraldic ornaments.
        g.fill(x - 11, y - 13, x + 12, y - 11, color);
        g.fill(x - 11, y - 11, x - 9, y + 5, color);
        g.fill(x + 10, y - 11, x + 12, y + 5, color);
        for (int row = 0; row < 10; row++) {
            g.fill(x - 10 + row, y + 5 + row, x - 8 + row, y + 6 + row, color);
            g.fill(x + 9 - row, y + 5 + row, x + 11 - row, y + 6 + row, color);
        }
        for (int row = -6; row <= 6; row++) {
            int half = Math.max(0, 3 - Math.abs(row));
            g.fill(x - half, y + row, x + half + 1, y + row + 1, color);
        }
        int tier = rank.ordinal();
        if (tier >= 1) {
            g.fill(x - 6, y - 8, x - 4, y - 6, color);
            g.fill(x + 5, y - 8, x + 7, y - 6, color);
        }
        for (int stripe = 0; stripe < Math.min(5, Math.max(0, tier - 1)); stripe++)
            g.fill(x - 7 + stripe * 3, y + 20, x - 5 + stripe * 3, y + 23, color);
        if (tier >= 7) {
            g.fill(x - 9, y - 18, x + 10, y - 16, color);
            for (int crown = -8; crown <= 8; crown += 8)
                g.fill(x + crown - 1, y - 23, x + crown + 2, y - 17, color);
        }
        if (tier == 8) {
            g.fill(x - 16, y - 4, x - 14, y + 12, color);
            g.fill(x + 15, y - 4, x + 17, y + 12, color);
        }
    }
}
