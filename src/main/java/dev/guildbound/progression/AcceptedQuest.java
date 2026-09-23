package dev.guildbound.progression;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/** Accepted contract identity and reward tier remain fixed across daily board refreshes. */
public record AcceptedQuest(long day,int tier,int slot,int kills,long deadline) {
    public static final Codec<AcceptedQuest> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.LONG.fieldOf("day").forGetter(AcceptedQuest::day),Codec.INT.fieldOf("tier").forGetter(AcceptedQuest::tier),
        Codec.INT.fieldOf("slot").forGetter(AcceptedQuest::slot),Codec.INT.fieldOf("kills").forGetter(AcceptedQuest::kills),
        Codec.LONG.fieldOf("deadline").forGetter(AcceptedQuest::deadline)).apply(i,AcceptedQuest::new));
    public AcceptedQuest { if(day<0||tier<0||tier>9||slot<0||slot>4||kills<0||kills>1000||deadline<0)throw new IllegalArgumentException("Invalid accepted quest"); }
    public static AcceptedQuest offer(DailyJournal board,int slot){
        return new AcceptedQuest(board.day(),board.tier(),slot,0,slot==4&&board.day()%3==2?(board.day()+1)*24000:0);
    }
    public static AcceptedQuest legacy(DailyJournal board,int slot){return new AcceptedQuest(board.day(),board.tier(),slot,board.kills().get(slot),0);}
    private DailyJournal rules(){return new DailyJournal(day,tier,0,0,List.of(0,0,0,0,0));}
    public String targetType(){return rules().targetType(slot);}
    public int target(){return rules().target(slot);}
    public boolean timed(){return deadline>0;}
    public boolean expired(long time){return timed()&&time>=deadline;}
    public boolean ready(){return kills>=target();}
    public int experience(){return rules().experience(slot)*(timed()?2:1);}
    public int emeralds(){return rules().emeralds(slot)*(timed()?2:1);}
    public boolean same(long day,int slot){return this.day==day&&this.slot==slot;}
    public AcceptedQuest kill(String type,boolean hostile,long time){
        return !expired(time)&&!ready()&&(targetType().equals(type)||targetType().equals("hostile")&&hostile)
            ? new AcceptedQuest(day,tier,slot,kills+1,deadline):this;
    }
}
