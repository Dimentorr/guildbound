package dev.guildbound.server;

import dev.guildbound.Guildbound;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Guildbound.ID)
public final class DailyService {
    private DailyService() {}
    public static void refresh(ServerPlayer player) {
        var p = player.getData(ProgressAttachments.PROGRESS);
        if (!p.registered()) return;
        var previous = player.getData(ProgressAttachments.DAILY);
        var log=player.getData(ProgressAttachments.QUEST_LOG).migrate(previous).expire(player.server.overworld().getDayTime());
        player.setData(ProgressAttachments.QUEST_LOG,log);
        var next = previous.refresh(Math.max(0, player.server.overworld().getDayTime() / 24000), p.totalLevel());
        // Preserve progress of a previously accepted introductory quest in a matching slot.
        var legacy = player.getData(ProgressAttachments.QUESTS);
        if (previous.day() == -1 && legacy.active()) {
            int slot = legacy.completed();
            var counts = new java.util.ArrayList<>(next.kills());
            counts.set(slot, legacy.kills() * next.target(slot) / legacy.contract().target);
            next = new dev.guildbound.progression.DailyJournal(next.day(), next.tier(), 1 << slot, 0, counts);
            player.setData(ProgressAttachments.QUEST_LOG,log.add(dev.guildbound.progression.AcceptedQuest.legacy(next,slot)));
            player.setData(ProgressAttachments.QUESTS, new dev.guildbound.progression.QuestState(legacy.completed(), false, 0));
        }
        if (!next.equals(previous)) player.setData(ProgressAttachments.DAILY, next);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        renewSession(player);
        var previous = player.getData(ProgressAttachments.DAILY);
        var log=player.getData(ProgressAttachments.QUEST_LOG);
        refresh(player);
        if(!previous.equals(player.getData(ProgressAttachments.DAILY))||!log.equals(player.getData(ProgressAttachments.QUEST_LOG)))sync(player);
        else if(!log.entries().isEmpty())PacketDistributor.sendToPlayer(player,new QuestLogPayload(null,player.server.overworld().getDayTime()));
    }
    public static void renewSession(ServerPlayer player) {
        var session=player.getData(ProgressAttachments.GUILD_SESSION);
        long now=player.serverLevel().getGameTime();
        if(player.isAlive() && !player.isSpectator() && session.matches(session.token(),session.desk(),player.level().dimension(),now) && RpgNetwork.canUseDesk(player,session.desk()))
            player.setData(ProgressAttachments.GUILD_SESSION,new GuildSession(session.token(),session.desk(),session.dimension(),now+1200));
    }
    public static void sync(ServerPlayer p) { PacketDistributor.sendToPlayer(p, new DailyPayload(p.getData(ProgressAttachments.DAILY))); PacketDistributor.sendToPlayer(p,new QuestLogPayload(p.getData(ProgressAttachments.QUEST_LOG),p.server.overworld().getDayTime())); }
    public static boolean action(ServerPlayer p, DailyActionPayload request) {
        refresh(p);
        var state = p.getData(ProgressAttachments.DAILY);
        if (request.slot() < 0 || request.slot() >= 5
                || !p.isAlive() || p.isSpectator() || !p.getData(ProgressAttachments.PROGRESS).registered()
                || !p.getData(ProgressAttachments.GUILD_SESSION).matches(request.token(), request.desk(), p.level().dimension(), p.serverLevel().getGameTime())
                || !RpgNetwork.canUseDesk(p, request.desk())) return false;
        var log=p.getData(ProgressAttachments.QUEST_LOG);
        if (request.claim()) {
            var quest=log.find(request.day(),request.slot()).orElse(null);
            if(quest==null||!log.activeEntries().contains(quest)||!quest.ready()||quest.expired(p.server.overworld().getDayTime()))return false;
            p.setData(ProgressAttachments.QUEST_LOG,log.remove(quest.day(),quest.slot()));
            if(request.day()==state.day()) {
                var counts=new java.util.ArrayList<>(state.kills());counts.set(request.slot(),quest.kills());
                p.setData(ProgressAttachments.DAILY,new dev.guildbound.progression.DailyJournal(state.day(),state.tier(),state.accepted(),state.claimed(),counts).claim(request.slot()));
            }
            ProgressEvents.award(p,quest.experience());
            var reward=new ItemStack(Items.EMERALD,quest.emeralds());
            if(!p.getInventory().add(reward))p.drop(reward,false);
        } else {
            if(request.day()!=state.day()||!log.offerSlots(state).contains(request.slot()))return false;
            var next=state.accept(request.slot());if(next.equals(state))return false;
            var quest=dev.guildbound.progression.AcceptedQuest.offer(state,request.slot());
            if(quest.expired(p.server.overworld().getDayTime()))return false;
            p.setData(ProgressAttachments.QUEST_LOG,log.add(quest));
            p.setData(ProgressAttachments.DAILY,next);
        }
        return true;
    }
    public static void kill(ServerPlayer player, net.minecraft.world.entity.LivingEntity target) {
        if (player.isCreative() || player.isSpectator() || !player.getData(ProgressAttachments.PROGRESS).registered() || SummonService.owned(target)) return;
        refresh(player);
        String kind = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
        var old = player.getData(ProgressAttachments.DAILY);
        var next = old.kill(kind, target instanceof net.minecraft.world.entity.monster.Enemy);
        var log=player.getData(ProgressAttachments.QUEST_LOG);
        var updatedLog=log.kill(kind,target instanceof net.minecraft.world.entity.monster.Enemy,player.server.overworld().getDayTime());
        player.setData(ProgressAttachments.QUEST_LOG,updatedLog);
        if (!old.equals(next)||!log.equals(updatedLog)) { player.setData(ProgressAttachments.DAILY, next); sync(player); }
    }
}
