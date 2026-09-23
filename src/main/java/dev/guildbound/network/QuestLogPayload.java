package dev.guildbound.network;
import dev.guildbound.Guildbound;
import dev.guildbound.progression.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record QuestLogPayload(QuestLog log,long time) implements CustomPacketPayload {
    public static final Type<QuestLogPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID,"quest_log"));
    public static final StreamCodec<RegistryFriendlyByteBuf,QuestLogPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeLong(p.time);b.writeBoolean(p.log!=null);if(p.log==null)return;b.writeVarInt(p.log.entries().size());
        for(var q:p.log.entries()){b.writeLong(q.day());b.writeVarInt(q.tier());b.writeVarInt(q.slot());b.writeVarInt(q.kills());b.writeLong(q.deadline());}
    },b->{long time=b.readLong();if(!b.readBoolean())return new QuestLogPayload(null,time);int count=b.readVarInt();if(count<0||count>65536)throw new IllegalArgumentException("Quest count");
        var entries=new java.util.ArrayList<AcceptedQuest>();for(int i=0;i<count;i++)entries.add(new AcceptedQuest(b.readLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readLong()));
        return new QuestLogPayload(new QuestLog(true,entries),time);});
    @Override public Type<QuestLogPayload> type(){return TYPE;}
}
