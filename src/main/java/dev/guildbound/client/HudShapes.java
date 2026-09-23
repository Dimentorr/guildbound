package dev.guildbound.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/** Fractional-coordinate HUD geometry: diagonal edges are actual triangles, not GUI-pixel stairs. */
final class HudShapes {
    private HudShapes() {}
    static int background() { return ((int)(ClientPreferences.HUD_OPACITY.get()*110)<<24)|0x172226; }
    static void polygon(GuiGraphics g,int color,float... xy) {
        g.flush();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN,DefaultVertexFormat.POSITION_COLOR);
        var pose=g.pose().last().pose();
        for(int i=0;i<xy.length;i+=2)b.addVertex(pose,xy[i],xy[i+1],0).setColor(color);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.disableBlend();RenderSystem.enableCull();
    }
    static void line(GuiGraphics g,float x1,float y1,float x2,float y2,float width,int color){
        float length=(float)Math.hypot(x2-x1,y2-y1);if(length==0)return;
        float dx=-(y2-y1)/length*width/2,dy=(x2-x1)/length*width/2;
        polygon(g,color,x1+dx,y1+dy,x1-dx,y1-dy,x2-dx,y2-dy,x2+dx,y2+dy);
    }
    static void outline(GuiGraphics g,int color,float width,float... xy){
        for(int i=0;i<xy.length;i+=2){int next=(i+2)%xy.length;line(g,xy[i],xy[i+1],xy[next],xy[next+1],width,color);}
    }
    static void ring(GuiGraphics g,float x,float y,float rx,float ry,int color){
        for(int i=0;i<24;i++){double a=i*Math.PI/12,b=(i+1)*Math.PI/12;
            line(g,x+(float)Math.cos(a)*rx,y+(float)Math.sin(a)*ry,x+(float)Math.cos(b)*rx,y+(float)Math.sin(b)*ry,.7F,color);}
    }
    static void bar(GuiGraphics g,float x,float y,float w,float h,float ratio,int color){
        polygon(g,background(),x,y,x+w-4,y,x+w,y+h/2,x+w-4,y+h,x,y+h);
        outline(g,0xA8C7CEC8,.55F,x,y,x+w-4,y,x+w,y+h/2,x+w-4,y+h,x,y+h);
        float r=Math.clamp(ratio,0,1);
        if(r>0){
            float left=x+1,top=y+1,bottom=y+h-1,tip=x+w-1,shoulder=x+w-4.5F;
            float edge=left+(tip-left)*r;
            if(edge<=shoulder) polygon(g,color,left,top,edge,top,edge,bottom,left,bottom);
            else {
                float fraction=(edge-shoulder)/(tip-shoulder);
                float cutTop=top+(h/2-1)*fraction,cutBottom=bottom-(h/2-1)*fraction;
                polygon(g,color,left,top,shoulder,top,edge,cutTop,edge,cutBottom,shoulder,bottom,left,bottom);
            }
            line(g,left,top+.2F,Math.min(edge,shoulder),top+.2F,.45F,0x65FFFFFF);
        }
    }
}
