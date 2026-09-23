package dev.guildbound.client;

import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.HeroSubclass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

final class SubclassScreen extends Screen {
    private final Screen parent;
    private final BlockPos desk;
    private final java.util.UUID token;
    private HeroSubclass selected;
    private Button confirm;
    private boolean pending;
    private long sentAt;
    private final java.util.List<GuildButton> cards = new java.util.ArrayList<>();
    SubclassScreen(Screen parent, BlockPos desk, java.util.UUID token) {
        super(Component.translatable("screen.guildbound.specialize")); this.parent = parent; this.desk = desk; this.token = token;
    }
    @Override protected void init() {
        cards.clear();
        var hero = minecraft.player.getData(ProgressAttachments.PROGRESS).tracks().getFirst().heroClass();
        int i = 0, w = Math.min(190, (width - 48) / 2), left = width / 2 - w - 4;
        for (var sub : HeroSubclass.values()) if (sub.parent() == hero) {
            var card = addRenderableWidget(GuildButton.from(Button.builder(Component.translatable(sub.key()), b -> selected = sub)
                    .bounds(left + (i % 2) * (w + 8), 42 + i / 2 * 29, w, 26).build())); card.emblem = sub; cards.add(card); i++;
        }
        confirm = addRenderableWidget(GuildButton.from(Button.builder(Component.translatable("screen.guildbound.specialize"), b -> {
            if (selected != null && !pending && eligible()) { pending = true; sentAt = System.currentTimeMillis(); PacketDistributor.sendToServer(new dev.guildbound.network.ChooseSubclassPayload(desk, token, selected)); }
        }).bounds(width / 2 - 135, height - 33, 175, 20).build()));
        addRenderableWidget(GuildButton.from(Button.builder(Component.translatable("screen.guildbound.close"), b -> onClose()).bounds(width / 2 + 45, height - 33, 90, 20).build()));
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        g.fill(0, 0, width, height, 0xBB111D25);
        AincradStyle.panel(g, 14, 10, width-28, height-20);
        g.drawString(font, title, 24, 20, AincradStyle.GOLD, false);
        if (!eligible()) { onClose(); return; }
        if (pending && System.currentTimeMillis()-sentAt > 3000) { onClose(); return; }
        confirm.active = selected != null && !pending;
        for (var card : cards) { card.selected = card.emblem == selected; card.active = !pending; }
        if (selected != null) {
            g.drawString(font, Component.translatable(selected.key()), 24, 136, AincradStyle.GOLD, false);
            g.drawWordWrap(font, Component.translatable(selected.key() + ".description"), 24, 151, width - 48, AincradStyle.TEXT);
        }
        super.render(g, x, y, partial);
    }
    private boolean eligible() { var p = minecraft.player.getData(ProgressAttachments.PROGRESS); return p.registered() && p.tracks().getFirst().subclass() == null && p.tracks().getFirst().level() >= 5; }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float tick) {}
}
