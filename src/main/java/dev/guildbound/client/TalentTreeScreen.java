package dev.guildbound.client;

import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.network.BuyTalentPayload;
import dev.guildbound.progression.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Two independent graph views. All purchase decisions remain on the server. */
public final class TalentTreeScreen extends Screen {
    private record Node(Talent talent, int x, int y) {}
    private final Screen parent;
    private boolean classTree;
    private Talent selected = Talent.VITALITY;
    private final TreeViewport viewport = new TreeViewport();
    private Button buy;
    private boolean dragging;
    private List<Node> nodes = List.of();

    public TalentTreeScreen(Screen parent) {
        super(Component.translatable("screen.guildbound.talents"));
        this.parent = parent;
    }
    private CharacterProgress progress() { return minecraft.player.getData(ProgressAttachments.PROGRESS); }
    private TalentState talents() { return minecraft.player.getData(ProgressAttachments.TALENTS); }
    private Component text(String key) { return Component.translatable("screen.guildbound." + key); }
    @Override protected void init() {
        addRenderableWidget(GuildButton.from(Button.builder(text("tree_common"), b -> switchTree(false)).bounds(20, 15, 106, 20).build()));
        addRenderableWidget(GuildButton.from(Button.builder(text("tree_class"), b -> switchTree(true)).bounds(132, 15, 106, 20).build()));
        addRenderableWidget(GuildButton.from(Button.builder(Component.literal("×"), b -> onClose()).bounds(width - 40, 15, 20, 20).build()));
        buy = addRenderableWidget(GuildButton.from(Button.builder(text("learn"), b -> {
            if (selected != null) PacketDistributor.sendToServer(new BuyTalentPayload(selected, talents().spent()));
        }).bounds(width - 125, height - 30, 105, 20).build()));
        addRenderableWidget(GuildButton.from(Button.builder(text("tree_center"), b -> resetCamera()).bounds(20, height - 30, 105, 20).build()));
        addRenderableWidget(new GuildButton(133, height-30, 80, 20, text("tree_overview"), b -> {
            double w = nodes.stream().mapToInt(n -> Math.abs(n.x())+70).max().orElse(200)*2;
            double h = nodes.stream().mapToInt(n -> Math.abs(n.y())+25).max().orElse(80)*2;
            viewport.fitBounds(sideX()-40, bottom()-65, w, h);
        }));
        rebuildNodes();
        resetCamera();
    }
    private void resetCamera() { viewport.fitBounds(sideX()-40,bottom()-65,(sideX()-40)/.55,(bottom()-65)/.55); }
    private void switchTree(boolean value) {
        classTree = value;
        resetCamera();
        rebuildNodes();
        selected = nodes.isEmpty() ? null : nodes.getFirst().talent();
    }
    private void rebuildNodes() {
        var result = new ArrayList<Node>();
        var hero = progress().registered() ? progress().tracks().getFirst().heroClass() : null;
        var chosen = progress().registered() ? progress().tracks().getFirst().subclass() : null;
        var visible = java.util.Arrays.stream(Talent.values()).filter(t -> classTree
                ? t.heroClass() == hero && (t.subclass() == null || t.subclass() == chosen) && (t != Talent.SPELL_WEAVING || progress().totalLevel() >= 15)
                : t.heroClass() == null).toList();
        var roots = visible.stream().filter(t -> t.prerequisite() == null || !visible.contains(t.prerequisite())).toList();
        for (int i=0;i<roots.size();i++) {
            int side = i%2==0 ? -1 : 1;
            int row = i/2;
            int rows = (roots.size()+1)/2;
            int y = row == 0 ? 0 : (row%2 == 1 ? -1 : 1)*((row+1)/2)*110;
            addBranch(result, visible, roots.get(i), side, 0, y);
        }
        nodes = List.copyOf(result);
    }
    private void addBranch(List<Node> result, List<Talent> visible, Talent t, int side, int depth, int y) {
        result.add(new Node(t, side*(115+depth*170), y));
        var children = visible.stream().filter(n -> n.prerequisite()==t).toList();
        for (int i=0;i<children.size();i++) addBranch(result, visible, children.get(i), side, depth+1, y + (int)((i-(children.size()-1)/2.0)*75));
    }
    private int descriptionHeight() { return selected == null ? 18 : font.split(Component.translatable(selected.key() + ".description"), Math.max(100, width - 40)).size() * font.lineHeight; }
    private int sideX() { return width-Math.min(160,Math.max(120,width/3)); }
    private int bottom() { return height-42; }
    private boolean inside(double x, double y) { return x >= 20 && x < sideX()-8 && y >= 61 && y < bottom(); }
    private double centerX() { return (20+sideX()-8) / 2.0; }
    private double centerY() { return (61 + bottom()) / 2.0; }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (minecraft.player == null) return;
        buy.active = selected != null && talents().canBuy(progress(), selected, talents().spent());
        g.fill(0, 0, width, height, 0xD518242B);
        g.drawString(font, text(classTree ? "tree_class" : "tree_common"), 20, 43, AincradStyle.GOLD, false);
        var points = Component.translatable("screen.guildbound.talent_points", talents().available(progress()));
        g.drawString(font, points, width - 20 - font.width(points), 43, AincradStyle.GOLD, false);
        g.fill(20, 61, sideX()-8, bottom(), 0xA0182027);
        g.enableScissor(20, 61, sideX()-8, bottom());
        g.pose().pushPose();
        g.pose().translate(centerX() + viewport.panX(), centerY() + viewport.panY(), 5);
        g.pose().scale((float) viewport.zoom(), (float) viewport.zoom(), 1);
        for (var node : nodes) {
            Node from = nodes.stream().filter(n -> n.talent() == node.talent().prerequisite()).findFirst().orElse(null);
            int direction = node.x() < 0 ? -1 : 1;
            int sx = from == null ? direction * 13 : from.x() + direction * 61;
            int sy = from == null ? 0 : from.y();
            int color = talents().rank(node.talent()) == 2 ? 0xFF85CB72
                    : talents().canStudy(progress(), node.talent(), talents().spent()) ? AincradStyle.GOLD : 0xFF677578;
            connector(g, sx, sy, node.x() - direction * 61, node.y(), color);
        }
        if (progress().registered()) {
            var track = progress().tracks().getFirst();
            AincradStyle.classIcon(g, track.heroClass(), track.subclass(), -13, -13, 26);
        }
        double wx = viewport.worldX(mouseX - centerX()), wy = viewport.worldY(mouseY - centerY());
        for (var node : nodes) {
            boolean hover = inside(mouseX, mouseY) && hit(node, wx, wy);
            int rank = talents().rank(node.talent());
            boolean available = talents().canBuy(progress(), node.talent(), talents().spent());
            int border = node.talent() == selected || hover ? AincradStyle.GOLD
                    : rank > 0 ? AincradStyle.MANA : available ? 0xFFB8C7C7 : 0xFF576167;
            g.fill(node.x() - 61, node.y() - 18, node.x() + 61, node.y() + 18, 0xFF303E46);
            g.renderOutline(node.x() - 61, node.y() - 18, 122, 36, border);
            g.fill(node.x()-59, node.y()-15, node.x()-57, node.y()+15, border);
            for(int pip=0;pip<2;pip++) g.fill(node.x()+43+pip*7,node.y()+9,node.x()+47+pip*7,node.y()+13,pip<rank ? AincradStyle.GOLD : 0xFF60747A);
            var label = Component.translatable(node.talent().key());
            float scale = Math.min(1F, 112F / Math.max(1, font.width(label)));
            g.pose().pushPose();
            g.pose().translate(node.x(), node.y() - 10, 10);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, label, -font.width(label) / 2, 0, available || rank > 0 ? AincradStyle.TEXT : AincradStyle.MUTED, false);
            g.pose().popPose();
            g.drawString(font, rank + " / 2", node.x() - 45, node.y() + 3, border, false);
            g.drawString(font, rank==2 ? "✓" : Component.translatable("talent.guildbound.cost_short",node.talent().cost(rank+1)).getString(),node.x()-8,node.y()+3,border,false);
        }
        g.flush();
        g.pose().popPose();
        g.disableScissor();
        int sx=sideX()+5;
        g.fill(sideX(),61,width-12,bottom(),0xE5223038);
        g.pose().pushPose();g.pose().translate(sx,68,10);g.pose().scale(.8F,.8F,1);
        int wrap=(int)((width-sx-20)/.8F), y=0;
        var details=new java.util.ArrayList<Component>();
        details.add(text("tree_controls"));
        if(selected!=null){
            details.add(Component.translatable(selected.key()).withStyle(net.minecraft.ChatFormatting.GOLD));
            details.add(Component.translatable(selected.key()+".description"));
            int next=Math.min(2,talents().rank(selected)+1);
            int level = selected.heroClass() == null ? progress().totalLevel() : progress().tracks().stream()
                    .filter(t -> t.heroClass() == selected.heroClass()).mapToInt(ClassTrack::level).findFirst().orElse(0);
            details.add(talents().rank(selected)==2 ? text("tree_max") : Component.translatable("screen.guildbound.tree_cost",
                    Component.literal(Integer.toString(selected.cost(next))).withStyle(talents().available(progress()) < selected.cost(next) ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.WHITE),
                    Component.literal(Integer.toString(selected.requiredLevel(next))).withStyle(level < selected.requiredLevel(next) ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.WHITE)));
            if(selected.subclass()!=null && progress().tracks().stream().noneMatch(t -> t.subclass()==selected.subclass()))
                details.add(Component.translatable(selected.subclass().key()).withStyle(net.minecraft.ChatFormatting.RED));
            if(selected.prerequisite()!=null&&talents().rank(selected.prerequisite())==0) details.add(Component.translatable(selected.prerequisite().key()).append(" I").withStyle(net.minecraft.ChatFormatting.RED));
            if(selected==Talent.SPELL_WEAVING)details.add(text("tree_future"));
        }
        for(var detail:details){for(var line:font.split(detail,wrap)){g.drawString(font,line,0,y,AincradStyle.TEXT,false);y+=font.lineHeight;}y+=10;}
        g.pose().popPose();
        super.render(g, mouseX, mouseY, partialTick);
    }
    private static void connector(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int middle = (x1 + x2) / 2;
        g.fill(Math.min(x1, middle), y1 - 1, Math.max(x1, middle) + 1, y1 + 1, color);
        g.fill(middle - 1, Math.min(y1, y2) - 1, middle + 1, Math.max(y1, y2) + 1, color);
        g.fill(Math.min(middle, x2), y2 - 1, Math.max(middle, x2) + 1, y2 + 1, color);
    }
    private static boolean hit(Node node, double x, double y) {
        return Math.abs(x - node.x()) <= 61 && Math.abs(y - node.y()) <= 18;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (inside(x, y) && (button == 0 || button == 1)) {
            if (button == 0) for (var node : nodes)
                if (hit(node, viewport.worldX(x - centerX()), viewport.worldY(y - centerY()))) { selected = node.talent(); return true; }
            dragging = true;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging) { viewport.pan(dx, dy); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { dragging = false; return super.mouseReleased(x, y, button); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (inside(x, y)) { viewport.zoomAt(x - centerX(), y - centerY(), vertical); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float tick) {}
}
