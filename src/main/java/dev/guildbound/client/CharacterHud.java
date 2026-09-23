package dev.guildbound.client;

import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.data.ResourceAttachments;
import dev.guildbound.progression.HeroClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class CharacterHud {
    private CharacterHud() {}

    private static boolean visible() {
        var mc = Minecraft.getInstance();
        return mc.player != null && !mc.options.hideGui && mc.screen == null && !mc.player.isSpectator()
                && !mc.getDebugOverlay().showDebugScreen() && mc.player.getData(ProgressAttachments.PROGRESS).registered();
    }
    public static void hideVanilla(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        var mc=Minecraft.getInstance();
        if (mc.player != null && mc.player.getData(ProgressAttachments.PROGRESS).registered() && !mc.player.isSpectator() && (event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.PLAYER_HEALTH)
                || event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.FOOD_LEVEL)
                || event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_BAR)
                || event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_LEVEL))) event.setCanceled(true);
    }
    public static void render(RenderGuiEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (!visible()) return;
        var progress = minecraft.player.getData(ProgressAttachments.PROGRESS);
        if (!progress.registered()) return;
        var resources = minecraft.player.getData(ResourceAttachments.RESOURCES);
        var graphics = event.getGuiGraphics();
        // Physical size stays stable when the user changes Minecraft GUI scale.
        float scale = (float) (Math.clamp(minecraft.getWindow().getWidth() / 960F, 1F, 2F)
                / minecraft.getWindow().getGuiScale() * ClientPreferences.HUD_SCALE.get());
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(6, 6, 100);
        graphics.pose().scale(scale, scale, 1);
        CompactHud.draw(graphics,minecraft);
        var entries=minecraft.player.getData(ProgressAttachments.QUEST_LOG).activeEntries();
        if(ClientPreferences.QUEST_TRACKER.get()&&!entries.isEmpty()){
            int rows=Math.max(1,(int)((graphics.guiHeight()-65)/scale-CompactHud.HEIGHT-10)/14);
            int columns=Math.max(1,(int)((graphics.guiWidth()-12)/scale)/220);
            int pageSize=rows*columns,pages=(entries.size()+pageSize-1)/pageSize;
            int page=(int)(minecraft.level.getGameTime()/160%pages);
            for(int n=page*pageSize;n<Math.min(entries.size(),(page+1)*pageSize);n++){
                var q=entries.get(n);int offset=n-page*pageSize,x=(offset/rows)*220,y=CompactHud.HEIGHT+7+(offset%rows)*14;
                String title=net.minecraft.network.chat.Component.translatable("daily.guildbound."+q.targetType()).getString();
                String label=(q.timed()?"★ ":"")+minecraft.font.plainSubstrByWidth(title,q.timed()?90:130)+" "+q.kills()+"/"+q.target()+(q.timed()?" · "+GuildboundClient.timeLeft(q.deadline()):"");
                graphics.fill(x,y-2,x+214,y+11,HudShapes.background());
                HudText.draw(graphics,minecraft.font,label,x+4,y,q.expired(GuildboundClient.questTime())?0xFFE05B4B:q.ready()?0xFF9AE05A:AincradStyle.GOLD);
            }
            if(pages>1) HudText.draw(graphics,minecraft.font,(page+1)+" / "+pages,0,CompactHud.HEIGHT-5,AincradStyle.MUTED);
        }
        graphics.flush();graphics.pose().popPose();

        CompactHud.bottom(graphics,minecraft);
        int[] circles=java.util.Arrays.stream(MagicSpell.available(progress)).mapToInt(s->s.circle).distinct().sorted().toArray();
        graphics.pose().pushPose();
        graphics.pose().translate(graphics.guiWidth()-(34*(1+circles.length))*scale-6,graphics.guiHeight()-38*scale-6,100);
        graphics.pose().scale(scale,scale,1);
        slot(graphics,false,false,0,resources.dodgeCooldown(),resources.canDodge()&&!minecraft.player.isCreative());
        for(int i=0;i<circles.length;i++) circleSlot(graphics,circles[i],34*(i+1));
        graphics.flush();graphics.pose().popPose();
    }

    private static void circleSlot(GuiGraphics g,int circle,int x){
        var mc=Minecraft.getInstance();var spell=MagicControls.selected(circle);int cooldown=MagicControls.cooldown(circle);
        HudShapes.polygon(g,HudShapes.background(),x,0,x+30,0,x+30,34,x,34);
        HudShapes.outline(g,cooldown>0?0xFF818A8D:AincradStyle.GOLD,.65F,x,0,x+30,0,x+30,34,x,34);
        if(spell==MagicSpell.EMBER)g.renderItem(net.minecraft.world.item.Items.FIRE_CHARGE.getDefaultInstance(),x+7,3);
        else if(spell==MagicSpell.HEAL)AincradStyle.abilityIcon(g,true,x+8,5,AincradStyle.MANA);
        else {HudShapes.ring(g,x+15,11,6,6,AincradStyle.MANA);HudShapes.line(g,x+11,11,x+19,11,.75F,AincradStyle.TEXT);HudShapes.line(g,x+15,7,x+15,15,.75F,AincradStyle.TEXT);}
        if(spell==MagicSpell.HEAL){
            var target=dev.guildbound.combat.HealingTarget.find(mc.player);
            String name=target==mc.player?net.minecraft.network.chat.Component.translatable("magic.guildbound.self").getString():target.getDisplayName().getString();
            String label=mc.font.plainSubstrByWidth(name,60)+" "+Math.round(target.getHealth())+"/"+Math.round(target.getMaxHealth());
            g.fill(x+30-HudText.width(mc.font,label)-3,-16,x+33,-2,HudShapes.background());
            HudText.draw(g,mc.font,label,x+30-HudText.width(mc.font,label),-13,AincradStyle.MANA);
        }
        var binding=(circle==0?GuildboundClient.MAGIC:GuildboundClient.FIRST_CIRCLE).getTranslatedKeyMessage();
        String key=mc.font.plainSubstrByWidth(binding.getString(),26);
        HudText.draw(g,mc.font,key,x+(30-HudText.width(mc.font,key))/2,23,AincradStyle.GOLD);
        if(circle>0) {g.pose().pushPose();g.pose().translate(x+2,1,200);g.pose().scale(.65F,.65F,1);HudText.draw(g,mc.font,Integer.toString(circle),0,0,AincradStyle.TEXT);g.pose().popPose();}
        if(cooldown>0){g.flush();g.pose().pushPose();g.pose().translate(0,0,220);g.fill(x+2,2,x+28,20,0xD022292C);
            String seconds=String.format(java.util.Locale.ROOT,"%.1f",cooldown/20F);HudText.draw(g,mc.font,seconds,x+(30-HudText.width(mc.font,seconds))/2,6,AincradStyle.TEXT);g.flush();g.pose().popPose();}
    }

    private static void slot(GuiGraphics graphics, boolean healing, boolean fire, int x, int cooldown, boolean ready) {
        var font = Minecraft.getInstance().font;
        graphics.fill(x, 0, x + 30, 34, 0, 0xB02C3030);
        int color = ready ? (healing ? AincradStyle.MANA : AincradStyle.GOLD) : 0xFF818A8D;
        graphics.renderOutline(x, 0, 30, 34, 0x9CB7B2A1);
        if (fire) graphics.renderItem(net.minecraft.world.item.Items.FIRE_CHARGE.getDefaultInstance(), x + 7, 3);
        else AincradStyle.abilityIcon(graphics, healing, x + 8, 5, color);
        var key = (fire || healing ? GuildboundClient.MAGIC : GuildboundClient.DODGE).getTranslatedKeyMessage();
        String binding = font.plainSubstrByWidth(key.getString(), 26);
        HudText.draw(graphics, font, binding, x + (30 - HudText.width(font,binding)) / 2, 23, color);
        if (cooldown > 0) {
            // Item icons render at GUI z + 150; keep the overlay above that layer.
            graphics.flush();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.fill(x + 2, 2, x + 28, 20, 0xD022292C);
            String seconds = fire || healing ? String.format(java.util.Locale.ROOT, "%.1f", cooldown / 20F)
                    : Integer.toString((cooldown + 19) / 20);
            HudText.draw(graphics, font, seconds, x + (30 - HudText.width(font,seconds)) / 2, 6, AincradStyle.TEXT);
            graphics.flush();
            graphics.pose().popPose();
        }
    }
}
