package dev.guildbound.client;

import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.GuildRank;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only rank information reached from the guild desk. */
final class GuildRankScreen extends Screen {
    private final Screen parent;
    GuildRankScreen(Screen parent) {
        super(Component.translatable("screen.guildbound.rank_details"));
        this.parent = parent;
    }
    @Override protected void init() {
        addRenderableWidget(GuildButton.from(Button.builder(Component.translatable("screen.guildbound.close"), b -> onClose())
                .bounds(width / 2 - 60, height - 36, 120, 20).build()));
    }
    @Override public void render(GuiGraphics g, int x, int y, float partialTick) {
        g.fill(0, 0, width, height, 0xEC1B282F);
        if (minecraft.player != null) {
            var progress = minecraft.player.getData(ProgressAttachments.PROGRESS);
            var rank = GuildRank.atLevel(progress.totalLevel());
            int left = Math.max(20, (width - 400) / 2);
            int available = width - left * 2;
            g.drawString(font, title, left, 24, AincradStyle.GOLD, false);
            var name = rank == null ? Component.translatable("screen.guildbound.rank_none") : Component.translatable(rank.key());
            g.drawWordWrap(font, name, left, 48, available, rank == null ? AincradStyle.MUTED : rank.color);
            g.drawString(font, Component.translatable("screen.guildbound.rank_level", progress.totalLevel()), left, 77, AincradStyle.TEXT, false);
            var next = java.util.Arrays.stream(GuildRank.values()).filter(r -> r.level > progress.totalLevel()).findFirst();
            var threshold = next.map(r -> Component.translatable("screen.guildbound.rank_next", Component.translatable(r.key()), r.level))
                    .orElseGet(() -> Component.translatable("screen.guildbound.rank_highest"));
            g.drawWordWrap(font, threshold, left, 97, available, AincradStyle.MUTED);
            g.drawWordWrap(font, Component.translatable("screen.guildbound.rank_perks"), left, 128, available, AincradStyle.TEXT);
        }
        super.render(g, x, y, partialTick);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float tick) {}
}
