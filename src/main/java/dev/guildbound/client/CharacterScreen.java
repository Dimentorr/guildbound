package dev.guildbound.client;

import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.data.ResourceAttachments;
import dev.guildbound.network.RegisterClassPayload;
import dev.guildbound.progression.Talent;
import dev.guildbound.progression.TalentState;
import dev.guildbound.progression.CharacterProgress;
import dev.guildbound.progression.HeroClass;
import dev.guildbound.progression.HeroSubclass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CharacterScreen extends Screen {
    private static final int DESIGN_WIDTH = 492;
    private static final int DESIGN_HEIGHT = 280;
    private final BlockPos desk;
    private final java.util.UUID guildToken;
    private final String notice;
    private boolean rebuilding;
    private boolean rebuildPending;
    private HeroClass rebuildChoice;
    private CharacterProgress rebuildSnapshot;
    private Button rebuildButton;
    private Button confirmRebuild;
    private Button questAction;
    private Button questTracker;
    private Button magicGuide;
    private Button specializationButton;
    private Button rankDetailsButton;
    private int selectedQuest = -1;
    private final List<QuestCard> questCards = new ArrayList<>();
    private final long openedAt = System.nanoTime();
    private final List<Button> registrationButtons = new ArrayList<>();
    private final List<PanelButton> navigation = new ArrayList<>();
    private int tab;

    public CharacterScreen(BlockPos desk) {
        this(desk, null);
    }

    public CharacterScreen(BlockPos desk, java.util.UUID guildToken) {
        this(desk, guildToken, "");
    }

    public CharacterScreen(BlockPos desk, java.util.UUID guildToken, String notice) {
        super(Component.translatable(desk == null ? "screen.guildbound.title" : "screen.guildbound.registration"));
        this.desk = desk;
        this.guildToken = guildToken;
        this.notice = notice;
    }

    private CharacterProgress progress() {
        return minecraft.player == null ? CharacterProgress.empty() : minecraft.player.getData(ProgressAttachments.PROGRESS);
    }

    @Override
    protected void init() {
        clearWidgets();
        registrationButtons.clear();
        navigation.clear();
        questCards.clear();
        for (var contract : dev.guildbound.progression.GuildContract.values()) {
            questCards.add(addRenderableWidget(new QuestCard(166 + contract.ordinal() * 104, contract,
                    button -> { selectedQuest = contract.ordinal(); updateButtons(); })));
        }
        String[] sections = {"profile", "abilities", "talents", "quests"};
        for (int index = 0; index < sections.length; index++) {
            final int selected = index;
            navigation.add(addRenderableWidget(new PanelButton(10, 62 + index * 34, 118, 28,
                    text(sections[index]), button -> { if (selected == 3 && progress().registered()) { minecraft.setScreen(new DailyQuestScreen(this, desk, guildToken)); return; } if (selected == 1 && progress().registered()) { minecraft.setScreen(new AbilityCatalogScreen(this)); return; } if (selected == 2 && progress().registered()) { minecraft.setScreen(new TalentTreeScreen(this)); return; } tab = selected; rebuilding = false; rebuildChoice = null; updateButtons(); })));
        }
        addRenderableWidget(new PanelButton(10, 240, 118, 27, text("close"), button -> onClose()));
        int index = 0;
        for (HeroClass subclass : HeroClass.values()) {
            var card = new ClassCard(162 + (index % 2) * 164, 111 + (index / 2) * 67, subclass,
                    button -> {
                        if (rebuilding) { rebuildChoice = subclass; rebuildSnapshot = progress(); rebuildPending = false; }
                        else {
                            MagicControls.classChosen(subclass);
                            PacketDistributor.sendToServer(new RegisterClassPayload(desk, subclass));
                        }
                        updateButtons();
                    });
            registrationButtons.add(addRenderableWidget(card));
            index++;
        }
        rebuildButton = addRenderableWidget(new PanelButton(382, 12, 96, 20, text("rebuild"), button -> {
            rebuilding = !rebuilding;
            rebuildChoice = null;
            rebuildPending = false;
            tab = 0;
            updateButtons();
        }));
        confirmRebuild = addRenderableWidget(new PanelButton(166, 219, 310, 26, text("rebuild_confirm"), button -> {
            if (rebuildChoice == null || rebuildSnapshot == null || guildToken == null || rebuildPending) return;
            rebuildPending = true;
            MagicControls.classChosen(rebuildChoice);
            PacketDistributor.sendToServer(new dev.guildbound.network.RebuildPayload(desk, guildToken, rebuildChoice,
                    rebuildSnapshot.totalLevel(), rebuildSnapshot.experience()));
            updateButtons();
        }));
        questAction = addRenderableWidget(new PanelButton(166, 233, 148, 23, text("quest_accept"), button -> {
            var state = minecraft.player.getData(ProgressAttachments.QUESTS);
            if (desk != null && guildToken != null && !state.finished() && selectedQuest == state.completed())
                PacketDistributor.sendToServer(new dev.guildbound.network.QuestActionPayload(desk, guildToken, state.completed(), state.ready()));
        }));
        questTracker = addRenderableWidget(new PanelButton(322, 233, 154, 23, text("tracker_on"), button -> {
            ClientPreferences.QUEST_TRACKER.set(!ClientPreferences.QUEST_TRACKER.get());
            ClientPreferences.SPEC.save();
            updateButtons();
        }));
        magicGuide = addRenderableWidget(new PanelButton(337, 41, 139, 14,
                Component.translatable("magic.guildbound.help"), button -> minecraft.setScreen(new MagicGuideScreen(this))));
        specializationButton = addRenderableWidget(new PanelButton(166, 237, 310, 19, text("specialize"), button -> {
            if (desk != null && guildToken != null && progress().registered() && progress().tracks().getFirst().subclass() == null) {
                minecraft.setScreen(new SubclassScreen(this, desk, guildToken));
            }
        }));
        rankDetailsButton = addRenderableWidget(new PanelButton(322, 40, 154, 13, text("rank_details"),
                button -> minecraft.setScreen(new GuildRankScreen(this))));
        updateButtons();
    }

    private void updateButtons() {
        if (rankDetailsButton != null) rankDetailsButton.visible = desk != null && tab == 0 && !rebuilding && progress().registered();
        if (specializationButton != null) {
            var p = progress();
            specializationButton.visible = tab == 0 && !rebuilding && p.registered()
                    && p.tracks().getFirst().subclass() == null;
            specializationButton.active = specializationButton.visible && desk != null && guildToken != null
                    && p.tracks().getFirst().level() >= 5;
            if (p.registered()) {
                HeroSubclass option = java.util.Arrays.stream(HeroSubclass.values())
                        .filter(s -> s.parent() == p.tracks().getFirst().heroClass()).findFirst().orElseThrow();
                specializationButton.setMessage(p.tracks().getFirst().level() < 5 ? text("specialize_level")
                        : desk == null ? text("specialize_guild")
                        : text("specialize"));
            }
        }
        if (magicGuide != null) magicGuide.visible = tab == 1 && MagicControls.hasAbilities();
        boolean choosing = tab == 0 && desk != null && (!progress().registered() || rebuilding && rebuildChoice == null);
        for (var button : registrationButtons) { button.visible = choosing; button.active = choosing; }
        for (int index = 0; index < navigation.size(); index++) navigation.get(index).selected = tab == index;
        if (rebuildButton != null) {
            rebuildButton.visible = desk != null && guildToken != null && progress().registered();
            rebuildButton.setMessage(text(rebuilding ? "rebuild_cancel" : "rebuild"));
        }
        if (confirmRebuild != null) {
            confirmRebuild.visible = tab == 0 && rebuilding && rebuildChoice != null;
            confirmRebuild.active = confirmRebuild.visible && !rebuildPending;
        }
        if (questAction != null && minecraft.player != null) {
            var state = minecraft.player.getData(ProgressAttachments.QUESTS);
            // The journal contains accepted work; the desk also offers the next available contract.
            selectedQuest = !state.finished() && (desk != null || state.active()) ? state.completed() : -1;
            int visibleIndex = 0;
            for (var card : questCards) {
                card.visible = tab == 3 && progress().registered() && card.contract.ordinal() == selectedQuest;
                card.active = card.visible;
                if (card.visible) card.setX(166 + visibleIndex++ * 104);
                card.selected = card.contract.ordinal() == selectedQuest;
                card.completed = card.contract.ordinal() < state.completed()
                        || card.contract.ordinal() == state.completed() && state.ready();
            }
            questAction.visible = tab == 3 && desk != null && progress().registered() && !state.finished() && selectedQuest == state.completed();
            questAction.active = questAction.visible && desk != null && guildToken != null && (!state.active() || state.ready());
            questAction.setMessage(text(state.ready() ? "quest_claim" : state.active() ? "quest_active" : "quest_accept"));
            questTracker.visible = tab == 3 && progress().registered();
            questTracker.setMessage(text(ClientPreferences.QUEST_TRACKER.get() ? "tracker_on" : "tracker_off"));
        }
    }

    private TalentState talents() { return minecraft.player.getData(ProgressAttachments.TALENTS); }
    private Component text(String suffix) { return Component.translatable("screen.guildbound." + suffix); }
    private Component animationLabel() { return text(ClientPreferences.ANIMATIONS.get() ? "animation_on" : "animation_off"); }
    private int canvasWidth() { return dev.guildbound.progression.GuildRank.atLevel(progress().totalLevel()) == null ? DESIGN_WIDTH : 554; }
    private float scale() { return .82F * Math.min(1F, Math.min((width - 28F) / canvasWidth(), (height - 26F) / DESIGN_HEIGHT)); }
    private float originX() {
        float reveal = ClientPreferences.ANIMATIONS.get() ? Math.min(1F, (System.nanoTime() - openedAt) / 180_000_000F) : 1;
        return Math.round((width - canvasWidth() * scale()) / 2F + 12 * (1 - reveal) * (1 - reveal));
    }
    private float originY() { return Math.round((height - DESIGN_HEIGHT * scale()) / 2F); }
    private double localX(double x) { return (x - originX()) / scale(); }
    private double localY(double y) { return (y - originY()) / scale(); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        // Flush prior GUI batches. Parallel depth layers avoid the former tilted-plane artifacts.
        graphics.flush();
        graphics.fill(0, 0, width, height, 0x350B1014);
        graphics.pose().pushPose();
        float origin = originX();
        graphics.pose().translate(origin, originY(), 200);
        graphics.pose().scale(scale(), scale(), 1);
        AincradStyle.panel(graphics, 0, 0, 138, DESIGN_HEIGHT);
        AincradStyle.panel(graphics, 150, 0, 342, DESIGN_HEIGHT);
        GuildRankBanner.draw(graphics, font, progress(), 506, 12);
        line(graphics, Component.literal("GUILDBOUND"), 12, 16, AincradStyle.GOLD);
        line(graphics, text("subtitle"), 12, 34, AincradStyle.MUTED);
        line(graphics, title, 166, 17, AincradStyle.TEXT);
        graphics.fill(166, 37, 476, 38, 1, 0x607F8788);

        if (tab == 0) renderProfile(graphics);
        else if (tab == 1) renderAbilities(graphics);
        else if (tab == 2) paragraph(graphics, text("find_guild"), 166, 58, 306);
        else renderQuests(graphics);
        line(graphics, text("world_live"), 166, 262, AincradStyle.MUTED);
        super.render(graphics, (int) ((mouseX - origin) / scale()), (int) localY(mouseY), partialTick);
        if (!notice.isEmpty()) {
            graphics.pose().pushPose(); graphics.pose().translate(0, 0, 500);
            graphics.fill(155, 199, 486, 233, 0xF522292C);
            HudText.wrap(graphics,font,Component.translatable(notice),163,203,315,notice.endsWith("denied") ? 0xFFFF9988 : AincradStyle.GOLD);
            graphics.pose().popPose();
        }
        graphics.flush();
        graphics.pose().popPose();
    }

    private void renderProfile(GuiGraphics graphics) {
        var profile = progress();
        if (rebuilding) {
            line(graphics, text("rebuild"), 166, 55, AincradStyle.GOLD);
            if (rebuildChoice == null) paragraph(graphics, text("rebuild_choose"), 166, 77, 306);
            else {
                line(graphics, Component.translatable(rebuildChoice.key()), 166, 80, AincradStyle.TEXT);
                paragraph(graphics, Component.translatable("screen.guildbound.rebuild_cost",
                        rebuildSnapshot.totalLevel(), Math.max(1, rebuildSnapshot.totalLevel() - 1),
                        dev.guildbound.progression.TalentPoints.total(Math.max(1, rebuildSnapshot.totalLevel() - 1))).withStyle(net.minecraft.ChatFormatting.RED), 166, 107, 306);
                paragraph(graphics, text("rebuild_kept"), 166, 164, 306);
            }
            return;
        }
        if (!profile.registered()) {
            line(graphics, text("unregistered"), 166, 56, AincradStyle.GOLD);
            paragraph(graphics, text(desk == null ? "find_guild" : "choose_class"), 166, 78, 306);
            return;
        }
        var track = profile.tracks().getFirst();
        line(graphics, trackTitle(track), 166, 55, AincradStyle.GOLD);
        graphics.pose().pushPose();
        graphics.pose().translate(166, 77, 0);
        graphics.pose().scale(.85F, .85F, 1);
        StatusPanel.draw(graphics, font, minecraft.player.getGameProfile().getName(), minecraft.player.getHealth(),
                minecraft.player.getMaxHealth(), profile, minecraft.player.getData(ResourceAttachments.RESOURCES));
        graphics.pose().popPose();
        paragraph(graphics, Component.translatable("passive.guildbound." + track.heroClass().name()
                .toLowerCase(java.util.Locale.ROOT) + (track.level() >= 3 ? ".advanced" : ".base")), 166, 199, 306);
    }

    private Component trackTitle(dev.guildbound.progression.ClassTrack track) {
        var title = Component.translatable(track.heroClass().key());
        return track.subclass() == null ? title : title.append(" / ").append(Component.translatable(track.subclass().key()));
    }

    private void renderQuests(GuiGraphics graphics) {
        if (!progress().registered()) { paragraph(graphics, text("find_guild"), 166, 58, 306); return; }
        var state = minecraft.player.getData(ProgressAttachments.QUESTS);
        line(graphics, text(desk == null ? "quest_journal" : "quest_board"), 166, 55, AincradStyle.GOLD);
        if (selectedQuest < 0) {
            parchment(graphics, 166, 110, 310, 116);
            paragraph(graphics, text(desk == null ? "quest_journal_empty" : "quest_board_empty").copy()
                    .withStyle(style -> style.withColor(0x000000)), 177, 128, 287);
            return;
        }
        var contract = dev.guildbound.progression.GuildContract.values()[selectedQuest];
        parchment(graphics, 166, 110, 310, 116);
        line(graphics, Component.translatable(contract.key()), 177, 119, 0xFF000000);
        if (selectedQuest < state.completed() || selectedQuest == state.completed() && state.ready()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 10);
            completionStamp(graphics, font, 379, 115, 87, 19);
            graphics.pose().popPose();
        }
        paragraph(graphics, Component.translatable(contract.key() + ".description").withStyle(style -> style.withColor(0x000000)), 177, 138, 287);
        Component status = selectedQuest < state.completed() ? text("quest_done")
                : selectedQuest > state.completed() ? text("quest_locked")
                : state.ready() ? text("quest_return")
                : state.active() ? Component.translatable("screen.guildbound.quest_progress", state.kills(), contract.target)
                : text("quest_available");
        line(graphics, status, 177, 177, 0xFF000000);
        line(graphics, Component.translatable("screen.guildbound.quest_reward", contract.experience, contract.emeralds), 177, 194, 0xFF000000);
        line(graphics, text("quest_desk_hint"), 177, 211, 0xFF000000);
    }

    private void renderAbilities(GuiGraphics graphics) {
        if (!progress().registered()) {
            paragraph(graphics, text("find_guild"), 166, 58, 306);
            return;
        }
        ability(graphics, false, 59, GuildboundClient.DODGE.getTranslatedKeyMessage());
        boolean mage = progress().tracks().stream().anyMatch(track -> track.heroClass() == HeroClass.MAGE);
        if (mage) {
            ability(graphics, true, 137, GuildboundClient.MAGIC.getTranslatedKeyMessage());
            line(graphics, text("fire_title").copy().append(" [").append(GuildboundClient.MAGIC.getTranslatedKeyMessage()).append("]"), 174, 214, AincradStyle.GOLD);
            paragraph(graphics, Component.translatable("screen.guildbound.fire_detail",
                    dev.guildbound.combat.CantripBalance.damage(dev.guildbound.combat.ClassBonuses.level(progress(), HeroClass.MAGE), talents().rank(Talent.EMBER)) * (dev.guildbound.world.ClassEquipment.subclass(minecraft.player) == HeroSubclass.WIZARD ? 1.15F : 1),
                    dev.guildbound.combat.CantripBalance.cooldown(talents().rank(Talent.EMBER)) / 20F), 174, 231, 287);
        }
        else if (dev.guildbound.combat.ClassBonuses.level(progress(), HeroClass.WARRIOR) > 0) {
            line(graphics, text("shield_title"), 174, 139, AincradStyle.GOLD);
            paragraph(graphics, Component.translatable("screen.guildbound.shield_detail",
                    minecraft.options.keyUse.getTranslatedKeyMessage(),
                    minecraft.options.keySprint.getTranslatedKeyMessage()), 174, 160, 287);
        }
    }

    private void ability(GuiGraphics graphics, boolean healing, int y, Component key) {
        graphics.fill(164, y - 3, 478, y + 65, 1, 0x803E494B);
        AincradStyle.abilityIcon(graphics, healing, 174, y + 6, healing ? AincradStyle.MANA : AincradStyle.GOLD);
        line(graphics, text(healing ? "heal_title" : "dodge_title"), 200, y + 2, AincradStyle.TEXT);
        line(graphics, Component.literal("[").append(key).append("]"), 438, y + 2, AincradStyle.GOLD);
        Component detail = healing ? Component.translatable("screen.guildbound.heal_detail_value",
                (dev.guildbound.combat.ClassBonuses.healing(progress()) + talents().effectiveRank(Talent.RESTORATION)) * dev.guildbound.world.ClassEquipment.healingMultiplier(minecraft.player) / 2F)
                : text(dev.guildbound.combat.ClassBonuses.canDodgeFrom(progress(), false) ? "dodge_detail_air" : "dodge_detail");
        paragraph(graphics, detail, 174, y + 26, 287);
    }

    private void line(GuiGraphics graphics, Component value, int x, int y, int color) {
        HudText.draw(graphics, font, value, x, y, color);
    }
    private void paragraph(GuiGraphics graphics, Component value, int x, int y, int width) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 10);
        HudText.wrap(graphics,font,value,x,y,width,AincradStyle.MUTED);
        graphics.pose().popPose();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean mouseClicked(double x, double y, int button) { updateButtons(); return super.mouseClicked(localX(x), localY(y), button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(localX(x), localY(y), button); }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(localX(x), localY(y)); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(localX(x), localY(y), button, dx / scale(), dy / scale());
    }

    private static void parchment(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 2, 0xFFB48B52);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 3, 0xFFE6CF98);
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 3, 0xFFEEDAA9);
        for (int i = 0; i < width / 9; i++) {
            int px = x + 5 + (i * 37) % (width - 10);
            int py = y + 4 + (i * 19) % (height - 8);
            graphics.fill(px, py, px + 2, py + 1, 4, 0x40936A35);
        }
    }

    private static final class QuestCard extends Button {
        private final dev.guildbound.progression.GuildContract contract;
        private boolean selected;
        private boolean completed;
        private QuestCard(int x, dev.guildbound.progression.GuildContract contract, OnPress action) {
            super(x, 76, 102, 28, Component.translatable(contract.key()), action, DEFAULT_NARRATION);
            this.contract = contract;
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            parchment(graphics, getX(), getY(), width, height);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 10);
            if (selected || isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), width, height, 0xFF5C3414);
            if (selected) graphics.fill(getX() + 4, getY() + height - 4, getX() + width - 4, getY() + height - 2, 0xFF5C3414);
            var font = net.minecraft.client.Minecraft.getInstance().font;
            // Vanilla scrolling button labels draw a shadow: black-on-black smears this font.
            float textScale = Math.min(1F, (width - 10F) / Math.max(1, font.width(getMessage())));
            graphics.pose().pushPose();
            graphics.pose().translate(getX() + width / 2F, getY() + (completed ? 3 : 9), 1);
            graphics.pose().scale(textScale, textScale, 1);
            graphics.drawString(font, getMessage(), -font.width(getMessage()) / 2, 0, 0xFF000000, false);
            graphics.pose().popPose();
            if (completed) completionStamp(graphics, font, getX() + 24, getY() + 14, width - 48, 10);
            graphics.pose().popPose();
        }
    }

    private static void completionStamp(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                                        int x, int y, int width, int height) {
        int ink = 0xFFA03828;
        graphics.renderOutline(x, y, width, height, ink);
        if (height > 12) graphics.renderOutline(x + 2, y + 2, width - 4, height - 4, ink);
        var label = Component.translatable("screen.guildbound.quest_stamp");
        float scale = Math.min(Math.min(1F, (width - 8F) / Math.max(1, font.width(label))), (height - 4F) / font.lineHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(x + width / 2F, y + (height - font.lineHeight * scale) / 2F, 1);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, label, -font.width(label) / 2, 0, ink, false);
        graphics.pose().popPose();
    }

    private static class PanelButton extends Button {
        private boolean selected;
        private PanelButton(int x, int y, int width, int height, Component message, OnPress action) {
            super(x, y, width, height, message, action, DEFAULT_NARRATION);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean highlighted = selected || isHoveredOrFocused();
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 2,
                    !active ? 0x80404749 : highlighted ? 0xE6B78C47 : 0x80515A5C);
            graphics.fill(getX(), getY(), getX() + 2, getY() + height, 3,
                    highlighted ? AincradStyle.GOLD : 0xFF919594);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 10);
            HudText.centered(graphics,net.minecraft.client.Minecraft.getInstance().font,getMessage(),getX()+7,getY()+(height-8)/2,width-14,AincradStyle.TEXT);
            graphics.pose().popPose();
        }
    }

    private static final class ClassCard extends Button {
        private final HeroClass subclass;
        private ClassCard(int x, int y, HeroClass subclass, OnPress action) {
            super(x, y, 154, 58, Component.translatable(subclass.key()), action, DEFAULT_NARRATION);
            this.subclass = subclass;
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 2,
                    isHoveredOrFocused() ? 0xE08A713F : 0xC0394549);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 4);
            graphics.renderOutline(getX(), getY(), width, height, isHoveredOrFocused() ? AincradStyle.GOLD : 0xFF78817E);
            AincradStyle.classIcon(graphics, subclass, getX() + 8, getY() + 15, 27);
            HudText.draw(graphics, font, Component.translatable(subclass.key()), getX() + 43, getY() + 17, AincradStyle.TEXT);
            HudText.draw(graphics, font, Component.translatable("screen.guildbound.base_class"), getX() + 43, getY() + 32, AincradStyle.MUTED);
            graphics.pose().popPose();
        }
    }
}
