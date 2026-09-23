package dev.guildbound.client;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.DailyJournal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
final class DailyQuestScreen extends Screen {
    private final Screen parent; private final BlockPos desk; private final java.util.UUID token;
    private final java.util.List<ScrollButton> cards=new java.util.ArrayList<>();
    private int bodyScroll, maxBodyScroll;
    private String notice="";
    private java.util.List<String> visibleIds=java.util.List.of();
    void result(String key) { notice=key; if(key.equals("quest.guildbound.reward_received")){selected=-1;requested=-1;opening=0;} }
    private GuildButton action; private int selected=-1,requested=-1; private float opening;
    private long lastFrame,day=Long.MIN_VALUE;
    DailyQuestScreen(Screen parent,BlockPos desk,java.util.UUID token){super(Component.translatable(desk==null?"screen.guildbound.quest_journal":"screen.guildbound.quest_board"));this.parent=parent;this.desk=desk;this.token=token;}
    private DailyJournal state(){return minecraft.player.getData(ProgressAttachments.DAILY);}
    private record Row(dev.guildbound.progression.AcceptedQuest quest,boolean active,boolean claimed){}
    private final class View {
        private final java.util.List<Row> rows=new java.util.ArrayList<>();
        View(){
            var board=state();var log=minecraft.player.getData(ProgressAttachments.QUEST_LOG);
            for(var q:log.activeEntries())rows.add(new Row(q,true,false));
            if(desk!=null){
                for(int slot:log.offerSlots(board))rows.add(new Row(dev.guildbound.progression.AcceptedQuest.offer(board,slot),false,false));
                for(int slot=0;slot<5&&rows.size()<5;slot++)if(board.claimed(slot))rows.add(new Row(dev.guildbound.progression.AcceptedQuest.legacy(board,slot),false,true));
            }
        }
        long day(){return state().day();}
        int size(){return rows.size();}
        dev.guildbound.progression.AcceptedQuest quest(int i){return rows.get(i).quest();}
        boolean active(int i){return rows.get(i).active();}
        boolean claimed(int i){return rows.get(i).claimed();}
        boolean ready(int i){return active(i)&&quest(i).ready()&&!quest(i).expired(GuildboundClient.questTime());}
        String targetType(int i){return quest(i).targetType();}
        int target(int i){return quest(i).target();}
        int experience(int i){return quest(i).experience();}
        int emeralds(int i){return quest(i).emeralds();}
    }
    private int rackWidth(){return Math.min(152,Math.max(100,width/3));}
    @Override protected void init(){
        clearWidgets();cards.clear();
        var tracker=new GuildButton(width-135,12,115,20,Component.empty(),b->{ClientPreferences.QUEST_TRACKER.set(!ClientPreferences.QUEST_TRACKER.get());ClientPreferences.SPEC.save();b.setMessage(Component.translatable(ClientPreferences.QUEST_TRACKER.get()?"screen.guildbound.tracker_on":"screen.guildbound.tracker_off"));});
        tracker.setMessage(Component.translatable(ClientPreferences.QUEST_TRACKER.get()?"screen.guildbound.tracker_on":"screen.guildbound.tracker_off"));addRenderableWidget(tracker);
        for(int i=0;i<5;i++){final int index=i;cards.add(addRenderableWidget(new ScrollButton(24,48+i*Math.max(29,(height-97)/5),rackWidth()-14,Math.max(26,(height-107)/5),b->{notice="";int chosen=index;requested=requested==chosen?-1:chosen;},index)));}
        action=addRenderableWidget(new GuildButton(rackWidth()+28,height-34,Math.max(90,width-rackWidth()-145),20,Component.empty(),b->{var view=new View();if(!view.rows.stream().map(r->r.quest().day()+":"+r.quest().slot()).toList().equals(visibleIds))return;if(selected>=0&&selected<view.size()&&desk!=null&&token!=null){var q=view.quest(selected);PacketDistributor.sendToServer(new dev.guildbound.network.DailyActionPayload(desk,token,q.day(),q.slot(),view.ready(selected)));}}));
        addRenderableWidget(new GuildButton(width-105,height-34,85,20,Component.translatable("screen.guildbound.close"),b->onClose()));
        addRenderableWidget(new GuildButton(24,height-34,rackWidth()-14,20,Component.translatable("quest.guildbound.roll_up"),b->requested=-1));
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        var s=new View();var ids=s.rows.stream().map(r->r.quest().day()+":"+r.quest().slot()).toList();if(!ids.equals(visibleIds)){selected=-1;requested=-1;opening=0;visibleIds=ids;}if(day!=s.day()){day=s.day();selected=-1;requested=-1;opening=0;}
        if(requested>=s.size()||selected>=s.size()){selected=-1;requested=-1;opening=0;}

        long now=System.nanoTime();float step=lastFrame==0?1:Math.min(.2F,(now-lastFrame)/1_000_000_000F)*5;lastFrame=now;
        if(!ClientPreferences.ANIMATIONS.get()){selected=requested;opening=selected<0?0:1;}
        else if(selected!=requested){opening=Math.max(0,opening-step);if(opening==0){selected=requested;bodyScroll=0;}}
        else if(selected>=0)opening=Math.min(1,opening+step);
        g.fill(0,0,width,height,0xC010181D);g.fill(16,39,width-16,height-42,0xFF493322);
        for(int y=42;y<height-42;y+=24){g.fill(18,y,width-18,y+2,0xFF302316);g.fill(24,y+8,width-24,y+9,0xFF59402A);}
        AincradStyle.text(g,font,title,20,18,AincradStyle.GOLD);
        for(int i=0;i<5;i++)cards.get(i).visible=i<s.size();
        int x=rackWidth()+28,w=width-x-26,top=51,bottom=height-52;
        if(selected>=0&&opening>0){
            int openBottom=top+(int)((bottom-top)*opening);g.enableScissor(x-5,top-5,x+w+5,openBottom+5);
            g.fill(x,top,x+w,bottom,0xFFBC955F);g.fill(x+5,top+5,x+w-5,bottom-5,0xFFEEDAA9);g.fill(x-3,top-3,x+w+3,top+5,0xFFD8B57C);
            Component name=questName(s.quest(selected));int y=top+14-bodyScroll;
            for(var line:font.split(name,w-24)){g.drawString(font,line,x+12,y,0xFF21170C,false);y+=font.lineHeight;}y+=8;
            if(s.active(selected)&&!s.ready(selected)){for(var line:font.split(Component.translatable("quest.guildbound.accepted"),w-24)){g.drawString(font,line,x+12,y,0xFF33562A,false);y+=font.lineHeight;}y+=8;}
            if(s.quest(selected).timed()) { y+=10;for(var line:font.split(Component.translatable("quest.guildbound.time_left",GuildboundClient.timeLeft(s.quest(selected).deadline())),w-24)){g.drawString(font,line,x+12,y,0xFFA03828,false);y+=font.lineHeight;} }
            y+=6;g.drawString(font,Component.translatable("daily.guildbound.progress",s.quest(selected).kills(),s.target(selected)),x+12,y,0xFF21170C,false);y+=20;
            for(var line:font.split(Component.translatable("screen.guildbound.quest_reward",s.experience(selected),s.emeralds(selected)),w-24)){g.drawString(font,line,x+12,y,0xFF21170C,false);y+=font.lineHeight;}y+=16;
            for(var line:font.split(Component.translatable(s.quest(selected).timed()?"quest.guildbound.timed_terms":"quest.guildbound.persistent_terms"),w-24)){g.drawString(font,line,x+12,y,0xFF21170C,false);y+=font.lineHeight;}


            maxBodyScroll=Math.max(0,y+bodyScroll+38-bottom);
            if(s.ready(selected)||s.claimed(selected)){var stamp=Component.translatable("screen.guildbound.quest_stamp");int sy=Math.max(y+9,bottom-28),sw=Math.min(w-20,font.width(stamp)+10);g.renderOutline(x+10,sy,sw,17,0xFFA03828);g.drawString(font,stamp,x+15,sy+5,0xFFA03828,false);}
            g.disableScissor();
        }else g.drawWordWrap(font,Component.translatable("quest.guildbound.select_scroll"),x+12,top+30,Math.max(60,w-24),AincradStyle.TEXT);
        action.visible=desk!=null&&selected>=0&&selected==requested&&opening>.99;
        action.active=action.visible&&token!=null&&!s.quest(selected).expired(GuildboundClient.questTime())&&!s.claimed(selected)&&(!s.active(selected)||s.ready(selected));
        if(selected>=0)action.setMessage(Component.translatable(s.ready(selected)?"screen.guildbound.quest_claim":s.claimed(selected)?"screen.guildbound.quest_done":s.active(selected)?"screen.guildbound.quest_active":"screen.guildbound.quest_accept"));
        if(!notice.isEmpty()){g.pose().pushPose();g.pose().translate(0,0,300);g.pose().translate(20,height-61,0);g.pose().scale(.8F,.8F,1);var lines=font.split(Component.translatable(notice),(int)((width-40)/.8F));g.fill(-5,-3,(int)((width-40)/.8F)+5,lines.size()*font.lineHeight+3,0xEE172127);int ny=0;for(var line:lines){g.drawString(font,line,0,ny,AincradStyle.GOLD,false);ny+=font.lineHeight;}g.pose().popPose();}
        super.render(g,mx,my,delta);
    }
    private final class ScrollButton extends net.minecraft.client.gui.components.Button{
        private final int slot;
        ScrollButton(int x,int y,int w,int h,OnPress press,int slot){super(x,y,w,h,Component.empty(),press,DEFAULT_NARRATION);this.slot=slot;}
        @Override protected void renderWidget(GuiGraphics g,int x,int y,float tick){
            var s=new View();int index=slot;if(index>=s.size())return;int color=index==requested?0xFFFFE2A6:0xFFE4C18A;
            g.fill(getX()+4,getY()+2,getX()+width-4,getY()+height-2,color);g.fill(getX(),getY(),getX()+6,getY()+height,0xFFB28B56);g.fill(getX()+width-6,getY(),getX()+width,getY()+height,0xFFB28B56);
            g.renderOutline(getX()+8,getY()+5,width-16,height-10,isHoveredOrFocused()?0xFF895723:0xFFC29A62);
            var name=questName(s.quest(index));setMessage(name);int ly=getY()+7;
            for(var line:font.split(name,width-22)){if(ly+8>getY()+height-3)break;g.drawString(font,line,getX()+11,ly,0xFF21170C,false);ly+=font.lineHeight;}
            if(s.active(index)&&!s.ready(index))g.fill(getX()+width-12,getY()+height-10,getX()+width-4,getY()+height-2,0xFF287037);
            if(s.claimed(index)||s.ready(index))g.fill(getX()+width-12,getY()+height-10,getX()+width-4,getY()+height-2,0xFFA03828);
        }
    }
    private Component questName(dev.guildbound.progression.AcceptedQuest q) {
        return Component.literal(q.timed()?"★ ":"").append(Component.translatable("daily.guildbound.hunt",Component.translatable("daily.guildbound."+q.targetType())));
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){ if(x>rackWidth()+20){bodyScroll=Math.clamp(bodyScroll-(int)(dy*16),0,maxBodyScroll);return true;}return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick){}
}
