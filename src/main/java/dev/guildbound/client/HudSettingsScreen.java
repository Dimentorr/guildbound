package dev.guildbound.client;

import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.data.ResourceAttachments;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudSettingsScreen extends Screen {
    private final Screen parent;

    public HudSettingsScreen(Screen parent) {
        super(Component.translatable("hud.guildbound.settings"));
        this.parent = parent;
    }

    private Component text(String key, Object... args) {
        return Component.translatable("hud.guildbound." + key, args);
    }

    @Override protected void init() {
        int x = width / 2 - 135;
        addRenderableWidget(new GuildButton(x, 40, 35, 20, Component.literal("−"), b -> changeScale(-.1)));
        addRenderableWidget(new GuildButton(x+235, 40, 35, 20, Component.literal("+"), b -> changeScale(.1)));
        addRenderableWidget(new GuildButton(x, 67, 35, 20, Component.literal("−"), b -> changeOpacity(-.1)));
        addRenderableWidget(new GuildButton(x+235, 67, 35, 20, Component.literal("+"), b -> changeOpacity(.1)));
        addRenderableWidget(new GuildButton(x, 94, 270, 20, orderLabel(), b -> {
            ClientPreferences.BAR_ORDER.set(ClientPreferences.BAR_ORDER.get().y("HP") < ClientPreferences.BAR_ORDER.get().y("MP")
                    ? ClientPreferences.BarOrder.MP_HP_SP : ClientPreferences.BarOrder.HP_MP_SP);
            ClientPreferences.SPEC.save();
            b.setMessage(orderLabel());
        }));
        addRenderableWidget(new GuildButton(x, 120, 270, 20, animationLabel(), b -> {
            ClientPreferences.ANIMATIONS.set(!ClientPreferences.ANIMATIONS.get()); ClientPreferences.SPEC.save(); b.setMessage(animationLabel());
        }));
        addRenderableWidget(new GuildButton(x, height-28, 130, 20, text("reset"), b -> {
            ClientPreferences.ANIMATIONS.set(true);
            ClientPreferences.HUD_SCALE.set(1.0);
            ClientPreferences.HUD_OPACITY.set(.65);
            ClientPreferences.BAR_ORDER.set(ClientPreferences.BarOrder.HP_MP_SP);
            ClientPreferences.SPEC.save();
            rebuildWidgets();
        }));
        addRenderableWidget(new GuildButton(x+140, height-28, 130, 20,
                Component.translatable("screen.guildbound.close"), b -> onClose()));
    }

    private Component animationLabel() { return text(ClientPreferences.ANIMATIONS.get()?"animation_on":"animation_off"); }
    private Component orderLabel() { return text("order", ClientPreferences.BAR_ORDER.get().y("HP") < ClientPreferences.BAR_ORDER.get().y("MP") ? "HP / MP" : "MP / HP"); }
    private void changeScale(double delta) {
        ClientPreferences.HUD_SCALE.set(Math.clamp(Math.round((ClientPreferences.HUD_SCALE.get()+delta)*100)/100.0,.75,1.75));
        ClientPreferences.SPEC.save();
    }
    private void changeOpacity(double delta) {
        ClientPreferences.HUD_OPACITY.set(Math.clamp(Math.round((ClientPreferences.HUD_OPACITY.get()+delta)*100)/100.0,0,1));
        ClientPreferences.SPEC.save();
    }

    @Override public void render(GuiGraphics g, int mx, int my, float tick) {
        g.fill(0,0,width,height,0xB018242B);
        g.drawCenteredString(font,title,width/2,16,AincradStyle.GOLD);
        g.drawCenteredString(font,text("scale",Math.round(ClientPreferences.HUD_SCALE.get()*100)),width/2,46,AincradStyle.TEXT);
        g.drawCenteredString(font,text("opacity",Math.round(ClientPreferences.HUD_OPACITY.get()*100)),width/2,73,AincradStyle.TEXT);
        int y=147;
        for(var line:font.split(text("future"),width-40)) {
            g.drawString(font,line,20,y,AincradStyle.MUTED,false);
            y+=font.lineHeight;
        }
        if(minecraft.player!=null && minecraft.player.getData(ProgressAttachments.PROGRESS).registered()) {
            float fit=Math.min((width-40F)/CompactHud.WIDTH,Math.max(0,height-y-45F)/CompactHud.HEIGHT);
            float scale=(float)Math.min(fit,ClientPreferences.HUD_SCALE.get());
            g.pose().pushPose();
            g.pose().translate((width-CompactHud.WIDTH*scale)/2,y+10,0);
            g.pose().scale(scale,scale,1);
            CompactHud.draw(g, minecraft);
            g.pose().popPose();
        }
        super.render(g,mx,my,tick);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick) {}
}
