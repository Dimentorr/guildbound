package dev.guildbound.client;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
/** Local HUD typography; does not replace Minecraft menus or user resource-pack fonts. */
final class HudText {
    private static final ResourceLocation FONT=ResourceLocation.fromNamespaceAndPath("guildbound","hud");
    private static final float SCALE=.85F;
    private HudText(){}
    static void smallRight(GuiGraphics g,Font font,String text,int right,int y,int color){
        right(g,font,text,right,y,color,.66F);
    }
    static void right(GuiGraphics g,Font font,String text,int right,int y,int color,float scale){
        g.pose().pushPose();g.pose().translate(right-font.width(styled(Component.literal(text)))*scale,y,10);g.pose().scale(scale,scale,1);
        g.drawString(font,styled(Component.literal(text)),0,0,color,false);g.pose().popPose();
    }
    static void centered(GuiGraphics g,Font font,Component text,int x,int y,int available,int color){
        float scale=Math.min(SCALE,available/(float)Math.max(1,font.width(styled(text))));
        g.pose().pushPose();g.pose().translate(x+(available-font.width(styled(text))*scale)/2,y,10);g.pose().scale(scale,scale,1);
        g.drawString(font,styled(text),0,0,color,false);g.pose().popPose();
    }
    static void wrap(GuiGraphics g,Font font,Component text,int x,int y,int width,int color){
        g.pose().pushPose();g.pose().translate(x,y,10);g.pose().scale(SCALE,SCALE,1);
        g.drawWordWrap(font,styled(text),0,0,(int)(width/SCALE),color);g.pose().popPose();
    }
    static Component styled(Component text){return text.copy().withStyle(s->s.withFont(FONT));}
    static int width(Font font,String text){return width(font,Component.literal(text));}
    static int width(Font font,Component text){return Math.round(font.width(styled(text))*SCALE);}
    static void draw(GuiGraphics g,Font font,String text,int x,int y,int color){draw(g,font,Component.literal(text),x,y,color);}
    static void draw(GuiGraphics g,Font font,Component text,int x,int y,int color){
        g.pose().pushPose();g.pose().translate(x,y,10);g.pose().scale(SCALE,SCALE,1);
        g.drawString(font,styled(text),0,0,color,false);g.pose().popPose();
    }
}
