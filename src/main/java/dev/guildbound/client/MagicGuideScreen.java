package dev.guildbound.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class MagicGuideScreen extends Screen {
    private final Screen previous;
    public MagicGuideScreen(Screen previous) { super(Component.translatable("magic.guildbound.guide_title")); this.previous = previous; }
    @Override protected void init() {
        addRenderableWidget(GuildButton.from(Button.builder(Component.translatable("magic.guildbound.understood"), b -> onClose())
                .bounds(width / 2 - 65, height - 28, 130, 20).build()));
    }
    @Override public void onClose() { minecraft.setScreen(previous); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float partial) {}
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        g.fill(0, 0, width, height, 0xED182227);
        float scale = Math.min(1F, Math.min(width / 430F, (height - 36) / 262F));
        g.pose().pushPose();
        g.pose().translate(width / 2F, 8, 200);
        g.pose().scale(scale, scale, 1);
        SpellWheelScreen.centered(g, font, title, 0, 0, AincradStyle.GOLD);
        SpellWheelScreen.centered(g, font, Component.translatable("magic.guildbound.tap", GuildboundClient.MAGIC.getTranslatedKeyMessage()), 0, 23, AincradStyle.TEXT);
        SpellWheelScreen.centered(g, font, Component.translatable("magic.guildbound.hold", GuildboundClient.MAGIC.getTranslatedKeyMessage()), 0, 42, AincradStyle.TEXT);
        int side = (int)((System.nanoTime() / 1_000_000 / 1800) % Math.max(1, Math.min(2, MagicControls.available().length)));
        SpellWheelScreen.drawChoices(g, font, side, 0, 135, 73);
        // A mouse diagram and moving pointer demonstrate direction-based selection.
        g.renderOutline(-9, 116, 18, 28, AincradStyle.TEXT);
        g.fill(-1, 117, 1, 127, AincradStyle.GOLD);
        int direction = side == 0 ? 1 : -1;
        for (int i = 18; i < 42; i += 4) g.fill(direction * i, 130, direction * i + 2, 132, AincradStyle.GOLD);
        SpellWheelScreen.centered(g, font, Component.translatable("magic.guildbound.guide_move"), 0, 222, AincradStyle.GOLD);
        SpellWheelScreen.centered(g, font, Component.translatable("magic.guildbound.circle_keys", GuildboundClient.MAGIC.getTranslatedKeyMessage(), GuildboundClient.FIRST_CIRCLE.getTranslatedKeyMessage()), 0, 243, AincradStyle.MUTED);
        g.flush(); g.pose().popPose();
        super.render(g, x, y, partial);
    }
}

