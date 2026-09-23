package dev.guildbound.progression;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
public record QuestLog(boolean initialized,List<AcceptedQuest> entries) {
    public static final Codec<QuestLog> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.BOOL.fieldOf("initialized").forGetter(QuestLog::initialized),AcceptedQuest.CODEC.listOf().fieldOf("entries").forGetter(QuestLog::entries)).apply(i,QuestLog::new));
    public QuestLog { entries=List.copyOf(entries);var ids=new HashSet<String>();for(var q:entries)if(!ids.add(q.day()+":"+q.slot()))throw new IllegalArgumentException("Duplicate quest"); }
    public static QuestLog empty(){return new QuestLog(false,List.of());}
    public QuestLog migrate(DailyJournal old){
        if(initialized)return this;
        var next=new ArrayList<AcceptedQuest>(entries);
        if(old.day()>=0)for(int i=0;i<5;i++)if(old.active(i))next.add(AcceptedQuest.legacy(old,i));
        return new QuestLog(true,next);
    }
    public static final int MAX_ACTIVE=5;
    public List<AcceptedQuest> activeEntries(){return entries.subList(0,Math.min(MAX_ACTIVE,entries.size()));}
    /** Carried contracts occupy places before new offers. Accepted offers never regenerate the same day. */
    public List<Integer> offerSlots(DailyJournal board){
        int free=Math.max(0,MAX_ACTIVE-entries.size());var slots=new ArrayList<Integer>();
        for(int i=0;i<5&&slots.size()<free;i++)if((board.accepted()&(1<<i))==0)slots.add(i);
        return List.copyOf(slots);
    }
    public Optional<AcceptedQuest> find(long day,int slot){return entries.stream().filter(q->q.same(day,slot)).findFirst();}
    public QuestLog add(AcceptedQuest q){if(find(q.day(),q.slot()).isPresent())return this;var next=new ArrayList<>(entries);next.add(q);return new QuestLog(true,next);}
    public QuestLog remove(long day,int slot){return new QuestLog(true,entries.stream().filter(q->!q.same(day,slot)).toList());}
    public QuestLog expire(long time){return new QuestLog(initialized,entries.stream().filter(q->!q.expired(time)).toList());}
    public QuestLog kill(String type,boolean hostile,long time){var next=new ArrayList<>(entries);for(int i=0;i<Math.min(MAX_ACTIVE,next.size());i++)next.set(i,next.get(i).kill(type,hostile,time));return new QuestLog(initialized,next);}
}
