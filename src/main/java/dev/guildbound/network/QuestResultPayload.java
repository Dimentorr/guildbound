package dev.guildbound.network;
import dev.guildbound.Guildbound;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record QuestResultPayload(String message) implements CustomPacketPayload {
    public static final Type<QuestResultPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Guildbound.ID,"quest_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf,QuestResultPayload> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.message,160),b->new QuestResultPayload(b.readUtf(160)));
    @Override public Type<QuestResultPayload> type(){return TYPE;}
}
