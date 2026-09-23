package dev.guildbound.client;

import dev.guildbound.combat.LevelStats;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.data.ResourceAttachments;
import dev.guildbound.progression.HeroClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Compact heraldic HUD; survival warnings appear only when supplies fall. */
final class CompactHud {
    static final int WIDTH=260, HEIGHT=110;
    private CompactHud() {}
    static void draw(GuiGraphics g,Minecraft mc) {
        var p=mc.player;
        if(p==null)return;
        var progress=p.getData(ProgressAttachments.PROGRESS);
        if(!progress.registered())return;
        var resources=p.getData(ResourceAttachments.RESOURCES);
        var track=progress.tracks().getFirst();
        AincradStyle.classIcon(g,track.heroClass(),track.subclass(),12,0,30);
        if(progress.totalLevel()>=5) {
            GuildRankBanner.drawHanging(g,mc.font,progress,11,37);
            HudShapes.ring(g,18,33,2,5,0xFFD5CDAF);
            HudShapes.ring(g,36,33,2,5,0xFFD5CDAF);
        }
        HudShapes.polygon(g,HudShapes.background(),52,3,WIDTH-18,3,WIDTH-12,16,52,16);
        HudShapes.line(g,52,16,WIDTH-12,16,.5F,0x85C7CEC8);
        HudText.draw(g,mc.font,mc.font.plainSubstrByWidth(p.getGameProfile().getName(),139),55,5,AincradStyle.TEXT);
        String level="Lv. "+progress.totalLevel();HudText.draw(g,mc.font,level,WIDTH-22-HudText.width(mc.font,level),5,AincradStyle.TEXT);
        boolean hpFirst=ClientPreferences.BAR_ORDER.get().y("HP")<ClientPreferences.BAR_ORDER.get().y("MP");
        String hp=Math.round(p.getHealth())+" / "+Math.round(p.getMaxHealth())+(p.getAbsorptionAmount()>0?" +"+Math.round(p.getAbsorptionAmount()):"");
        gauge(g,mc,"HP",hp,hpFirst?20:39,p.getHealth()/p.getMaxHealth(),AincradStyle.HEALTH);
        if(progress.tracks().stream().anyMatch(t->t.heroClass()==HeroClass.MAGE))
            gauge(g,mc,"MP",resources.mana()/10+" / "+LevelStats.mana(progress.totalLevel())/10,hpFirst?39:20,resources.mana()/(float)LevelStats.mana(progress.totalLevel()),AincradStyle.MANA);
        // RPG experience remains distinct from Minecraft enchanting experience.
        float xp=progress.totalLevel()>=100?1:progress.experience()/(float)progress.nextLevelCost();
        g.fill(55,60,WIDTH-12,63,0xCC27353D);g.fill(55,60,55+(int)((WIDTH-67)*xp),63,AincradStyle.EXPERIENCE);
        int food=p.getFoodData().getFoodLevel();
        if(food<=17) warning(g,55,68,food,false);
        int water=20; // Placeholder only, no thirst simulation or saved resource yet.
        if(water<=17) warning(g,food<=17?75:55,68,water,true);
    }
    private static void gauge(GuiGraphics g,Minecraft mc,String label,String value,int y,float ratio,int color){
        HudShapes.polygon(g,HudShapes.background(),52,y-2,WIDTH-8,y-2,WIDTH-2,y+6.5F,WIDTH-8,y+15,52,y+15);
        HudText.draw(g,mc.font,label,55,y+3,AincradStyle.TEXT);
        HudShapes.bar(g,73,y,177,13,ratio,color);
        g.pose().pushPose();g.pose().translate(0,0,20);
        HudText.right(g,mc.font,value,246,y+5,AincradStyle.TEXT,.78F);
        g.pose().popPose();
    }
    private static void warning(GuiGraphics g,int x,int y,int food,boolean water){
        int color=food==0?0xFF101010:food<=6?0xFFE05B4B:food<=12?0xFFE5BD50:0xFF80C766;
        HudShapes.polygon(g,0x60172024,x-2,y-2,x+16,y-2,x+16,y+17,x-2,y+17);
        if(water){
            HudShapes.polygon(g,color,x+7,y,x+13,y+9,x+12,y+13,x+7,y+15,x+2,y+13,x+1,y+9);
            HudShapes.outline(g,0xE0D4D5C9,.6F,x+7,y,x+13,y+9,x+12,y+13,x+7,y+15,x+2,y+13,x+1,y+9);
        }else{
            // Esophagus, stomach sac, and three intestinal bends form an anatomical silhouette.
            HudShapes.line(g,x+8,y-1,x+8,y+3,1.6F,color);
            HudShapes.polygon(g,color,x+8,y+2,x+11,y+1,x+14,y+3,x+14,y+7,x+11,y+9,x+5,y+9,x+3,y+6,x+5,y+4,x+8,y+5);
            HudShapes.outline(g,0xD8E5DFCA,.6F,x+8,y+2,x+11,y+1,x+14,y+3,x+14,y+7,x+11,y+9,x+5,y+9,x+3,y+6,x+5,y+4,x+8,y+5);
            float[] bends={x+3,y+9,x+1,y+10,x+1,y+14,x+4,y+15,x+12,y+15,x+14,y+13,x+13,y+11,x+5,y+11,x+4,y+13,x+10,y+13};
            for(int i=0;i<bends.length-2;i+=2)HudShapes.line(g,bends[i],bends[i+1],bends[i+2],bends[i+3],1.6F,color);
        }
        if(food==0){HudShapes.line(g,x+5,y+3,x+8,y+7,1.3F,0xFFCBC5B4);HudShapes.line(g,x+8,y+7,x+5,y+11,1.3F,0xFFCBC5B4);}
    }
    static void bottom(GuiGraphics g,Minecraft mc){
        var p=mc.player;var r=p.getData(ResourceAttachments.RESOURCES);
        int max=LevelStats.stamina(p.getData(ProgressAttachments.PROGRESS).totalLevel());
        int left=g.guiWidth()/2-91,y=g.guiHeight()-26;
        if(!p.isCreative()){
            g.fill(left,y,left+182,y+2,0xCC182522);
            g.fill(left,y,left+(int)(182*p.experienceProgress),y+2,0xFF8CCB57);
            if(p.experienceLevel>0){String level=Integer.toString(p.experienceLevel);HudText.draw(g,mc.font,level,left+91-HudText.width(mc.font,level)/2,y-10,0xFF9AE05A);}
        }
        if(r.stamina()<max || r.exhausted()){
            int sy=y-23;
            HudShapes.bar(g,left+2,sy,178,6,r.stamina()/(float)max,r.exhausted()?0xFFE05B4B:AincradStyle.STAMINA);

        }
    }
}
