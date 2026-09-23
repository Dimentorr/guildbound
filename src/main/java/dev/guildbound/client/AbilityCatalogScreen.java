package dev.guildbound.client;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
final class AbilityCatalogScreen extends Screen {
    private final Screen parent;
    private int scroll,contentHeight,category=0,spellCircle=0;
    AbilityCatalogScreen(Screen parent){super(Component.translatable("screen.guildbound.abilities"));this.parent=parent;}
    private Component text(String k){return Component.translatable("catalog.guildbound."+k);}
    @Override protected void init(){
        clearWidgets();addRenderableWidget(new GuildButton(width-100,12,80,20,Component.translatable("screen.guildbound.close"),b->onClose()));
        String[] sections={"passive","active","spells"};
        for(int i=0;i<3;i++){final int c=i;addRenderableWidget(new GuildButton(14,51+i*25,104,21,text(sections[i]),b->{category=c;scroll=0;rebuildWidgets();}));}
        if(category==2){
            addRenderableWidget(new GuildButton(20,133,98,20,Component.translatable("magic.guildbound.cantrips"),b->{spellCircle=0;scroll=0;}));
            addRenderableWidget(new GuildButton(20,158,98,20,Component.translatable("magic.guildbound.first_circle"),b->{spellCircle=1;scroll=0;}));
        }
    }
    private record Entry(Component name,Component description,Component stats){}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        g.fill(0,0,width,height,0xE51B282F);AincradStyle.text(g,font,title,20,20,AincradStyle.GOLD);
        var p=minecraft.player.getData(ProgressAttachments.PROGRESS);var talents=minecraft.player.getData(ProgressAttachments.TALENTS);
        var entries=new java.util.ArrayList<Entry>();
        if(category==0){
            entries.add(new Entry(text("growth"),Component.translatable("catalog.guildbound.growth_detail",dev.guildbound.combat.LevelStats.stamina(p.totalLevel())/10,dev.guildbound.combat.LevelStats.mana(p.totalLevel())/10,dev.guildbound.combat.LevelStats.healthBonus(p.totalLevel())/2),Component.empty()));
            for(var t:Talent.values())if(talents.rank(t)>0)entries.add(new Entry(Component.translatable(t.key()),Component.translatable(t.key()+".description"),Component.literal(talents.rank(t)+" / 2")));
        }
        if(category==1){
            entries.add(new Entry(Component.translatable("screen.guildbound.dodge_title"),Component.translatable(dev.guildbound.combat.ClassBonuses.canDodgeFrom(p,false)?"screen.guildbound.dodge_detail_air":"screen.guildbound.dodge_detail"),GuildboundClient.DODGE.getTranslatedKeyMessage()));
            if(dev.guildbound.combat.ClassBonuses.level(p,HeroClass.WARRIOR)>0)entries.add(new Entry(Component.translatable("screen.guildbound.shield_title"),Component.translatable("screen.guildbound.shield_detail",minecraft.options.keyUse.getTranslatedKeyMessage(),minecraft.options.keySprint.getTranslatedKeyMessage()),Component.empty()));
        }
        for(var spell:MagicSpell.available(p)){
            boolean magical=spell.skill==null||spell.skill.magical();
            if(category==0||category==1&&magical||category==2&&(!magical||spell.circle!=spellCircle))continue;
            Component description=Component.translatable(spell.skill==null?(spell==MagicSpell.HEAL?"catalog.guildbound.heal":"catalog.guildbound.ember"):spell.skill.key()+".description");
            String stats="";
            if(spell.skill!=null){int rank=talents.rank(spell.skill.talent());stats=spell.skill==HeroAbility.ARCANE_BURST?"10 MP/s · 10 s · 6 m · 0.6 s":spell.skill.cost+(magical?" MP":" SP")+" · "+String.format(java.util.Locale.ROOT,"%.1f s",spell.skill.cooldown(rank)/20F);}
            stats+=" ["+(spell.circle==1?GuildboundClient.FIRST_CIRCLE:GuildboundClient.MAGIC).getTranslatedKeyMessage().getString()+"]";
            entries.add(new Entry(spell.label(),description,Component.literal(stats)));
        }
        g.enableScissor(126,46,width-12,height-12);g.pose().pushPose();g.pose().translate(128,51,10);g.pose().scale(.8F,.8F,1);
        int w=(int)((width-146)/.8F),y=-scroll;
        if(entries.isEmpty())g.drawWordWrap(font,text("empty"),8,4,w-16,AincradStyle.MUTED);
        for(var e:entries){var lines=font.split(e.description,w-24);var statLines=font.split(e.stats,w-24);int h=32+(lines.size()+statLines.size())*font.lineHeight;
            g.fill(0,y,w,y+h,0xDD2D3E46);g.fill(0,y,2,y+h,AincradStyle.GOLD);int ly=y+8;
            for(var line:font.split(e.name,w-24)){g.drawString(font,line,10,ly,AincradStyle.GOLD,false);ly+=font.lineHeight;}ly+=5;
            for(var line:lines){g.drawString(font,line,10,ly,AincradStyle.TEXT,false);ly+=font.lineHeight;}ly+=6;
            for(var line:statLines){g.drawString(font,line,10,ly,AincradStyle.MANA,false);ly+=font.lineHeight;}
            y=Math.max(y+h,ly+8)+6;
        }
        contentHeight=y+scroll;g.pose().popPose();g.disableScissor();super.render(g,mx,my,delta);
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll=Math.clamp(scroll-(int)(dy*30),0,Math.max(0,contentHeight-(int)((height-70)/.8F)));return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick){}
}
