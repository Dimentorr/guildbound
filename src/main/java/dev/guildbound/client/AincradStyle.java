package dev.guildbound.client;

import dev.guildbound.progression.HeroClass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Original vector-like GUI shapes. All surfaces are parallel to the screen to avoid depth fighting. */
public final class AincradStyle {
    public static final int TEXT = 0xFFF2EEE2;
    public static final int MUTED = 0xFFB9BCB9;
    public static final int GOLD = 0xFFE9BF72;
    public static final int HEALTH = 0xFF91CE25;
    public static final int MANA = 0xFF65B9D5;
    public static final int STAMINA = 0xFFE4BF57;
    public static final int EXPERIENCE = 0xFFADA1DC;

    private AincradStyle() {}

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 3, y + 4, x + width + 3, y + height + 4, -2, 0x60000000);
        graphics.fill(x, y, x + width, y + height, 0, 0xE022292C);
        graphics.renderOutline(x, y, width, height, 0xB8B7B2A1);
        graphics.fill(x, y, x + width, y + 2, 1, GOLD);
    }

    public static void text(GuiGraphics graphics, Font font, Component value, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 10);
        graphics.drawString(font, value, x, y, color, false);
        graphics.pose().popPose();
    }

    public static void text(GuiGraphics graphics, Font font, String value, int x, int y, int color) {
        text(graphics, font, Component.literal(value), x, y, color);
    }

    /** Sloping tip and stepped lower edge echo the reference without rotating depth-writing quads. */
    public static void bar(GuiGraphics graphics, int x, int y, int width, float ratio, int color) {
        int filled = Math.round((width - 4) * Math.clamp(ratio, 0F, 1F));
        for (int row = 0; row < 9; row++) {
            int end = x + width - row / 2;
            int start = x + (row >= 6 ? 3 : 0);
            graphics.fill(start, y + row, end, y + row + 1, 1, 0xFFABA998);
            if (row > 0 && row < 8) {
                graphics.fill(start + 1, y + row, end - 1, y + row + 1, 2, 0xFF283F50);
                int fillEnd = start + 1 + Math.round((end - start - 2) * Math.clamp(ratio, 0F, 1F));
                if (filled > 0 && fillEnd > start + 1) graphics.fill(start + 1, y + row, fillEnd, y + row + 1,
                        3, row == 1 ? brighten(color) : color);
            }
        }
    }

    private static int brighten(int color) {
        return 0xFF000000 | Math.min(255, ((color >> 16) & 255) + 25) << 16
                | Math.min(255, ((color >> 8) & 255) + 25) << 8 | Math.min(255, (color & 255) + 25);
    }

    public static void classIcon(GuiGraphics graphics, HeroClass heroClass, int x, int y, int size) {
        classIcon(graphics, heroClass, null, x, y, size);
    }

    public static void classIcon(GuiGraphics graphics, HeroClass heroClass,
                                 dev.guildbound.progression.HeroSubclass subclass, int x, int y, int size) {
        String[] pixels = subclass == null ? switch (heroClass) {
            case WARRIOR -> new String[]{"..########..", "..#..##..#..", "..#..##..#..", "..#..##..#..",
                    "..#..##..#..", "..########..", "...#.##.#...", "...#.##.#...", "....####....", ".....##....."};
            case RANGER -> new String[]{"...##.......", "...#.##.....", "...#...#....", "...#....#...",
                    ".##########.", "...#....#...", "...#...#....", "...#.##.....", "...##.......", "............"};
            case ROGUE -> new String[]{".##......##.", "..##....##..", "...##..##...", "....####....",
                    ".....##.....", "....####....", "..###..###..", "...#....#...", "..##....##..", ".##......##."};
            case MAGE -> new String[]{".....##.....", "....####....", "...######...", "....####....",
                    ".....##.....", ".....##.....", ".....##.....", "....####....", ".....##.....", ".....##....."};
        } : switch (subclass) {
            case HUNTER, BEASTMASTER, PACK_LEADER, MONSTER_TAMER, WILD_HUNTER -> new String[]{".....##.....", "...######...", "..##.##.##..", ".##..##..##.",
                    "############", ".##..##..##.", "..##.##.##..", "...######...", ".....##.....", ".....##....."};
            case DUELIST, SHADOW, BURGLAR, POISONER -> new String[]{".........##.", "........##..", ".......##...", "......##....",
                    ".....##.....", "..#.##......", "...##.......", "..####......", ".##...#.....", "##.........."};
            case GUARDIAN, BERSERKER, RUNE_WARRIOR -> new String[]{".##########.", ".#...##...#.", ".#...##...#.", ".##########.",
                    ".#...##...#.", "..#..##..#..", "..#..##..#..", "...#.##.#...", "....####....", ".....##....."};
            case WIZARD -> new String[]{".####..####.",".#..#..#..#.",".#..####..#.",".#...##...#.",".#...##...#.",".#...##...#.",".####..####.",".....##....."};
            case BARD -> new String[]{".....######.",".....##..##.",".....##..##.",".....##..##.",".....##..##.","..#####.###.",".######.###.","..####......"};
            case PRIEST -> new String[]{".....##.....",".....##.....","..########..","..########..",".....##.....",".....##.....",".....##.....","....####...."};
            case NECROMANCER -> new String[]{"...######...","..########..","..#..##..#..","..########..","...#.##.#...","....####....","...#....#...","..#......#.."};
            case SORCERER -> new String[]{".....##.....", "...##..##...", "..#..##..#..", ".#..####..#.",
                    "#..######..#", ".#..####..#.", "..#..##..#..", "...##..##...", ".....##.....", "............"};
        };
        float cut=size*.25F;
        float[] octagon={x+cut,y,x+size-cut,y,x+size,y+cut,x+size,y+size-cut,x+size-cut,y+size,x+cut,y+size,x,y+size-cut,x,y+cut};
        HudShapes.polygon(graphics,0xD84C4B36,octagon);
        HudShapes.outline(graphics,0xFFCCD0C7,Math.max(.65F,size*.025F),octagon);
        float inset=size*.07F;
        HudShapes.outline(graphics,GOLD,.5F,x+cut,y+inset,x+size-cut,y+inset,x+size-inset,y+cut,x+size-inset,y+size-cut,x+size-cut,y+size-inset,x+cut,y+size-inset,x+inset,y+size-cut,x+inset,y+cut);
        graphics.pose().pushPose();
        graphics.pose().translate(x + size * 0.12F, y + size * 0.18F, 5);
        graphics.pose().scale(size / 16F, size / 16F, 1);
        for (int row = 0; row < pixels.length; row++) {
            for (int column = 0; column < pixels[row].length(); column++) {
                if (pixels[row].charAt(column) == '#') graphics.fill(column, row, column + 1, row + 1, TEXT);
            }
        }
        graphics.pose().popPose();
    }

    public static void abilityIcon(GuiGraphics graphics, boolean healing, int x, int y, int color) {
        if (healing) {
            graphics.fill(x + 5, y, x + 8, y + 13, 4, color);
            graphics.fill(x, y + 5, x + 13, y + 8, 4, color);
        } else {
            for (int row = 0; row < 11; row++) {
                int offset = 5 - Math.abs(row - 5);
                graphics.fill(x + offset, y + row, x + offset + 3, y + row + 1, 4, color);
                graphics.fill(x + offset + 6, y + row, x + offset + 9, y + row + 1, 4, color);
            }
        }
    }
}
