package dev.guildbound.network;

import dev.guildbound.Guildbound;
import dev.guildbound.client.GuildboundClient;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.data.ResourceAttachments;
import dev.guildbound.server.CombatEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RpgNetwork {
    private RpgNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("13");
        registrar.playToClient(SkillsPayload.TYPE, SkillsPayload.CODEC, (p, c) -> c.player().setData(ProgressAttachments.ABILITIES, p.state()));
        registrar.playToServer(SkillPayload.TYPE, SkillPayload.CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer player) {
                if (!dev.guildbound.server.SkillService.cast(player, p.ability()))
                    player.displayClientMessage(Component.translatable("message.guildbound.skill_denied"), true);
                CombatEvents.sync(player);
            }
        });
        registrar.playToClient(QuestLogPayload.TYPE,QuestLogPayload.CODEC,(p,c)-> { if(p.log()!=null)c.player().setData(ProgressAttachments.QUEST_LOG,p.log()); GuildboundClient.questClock(p.time()); });
        registrar.playToClient(DailyPayload.TYPE, DailyPayload.CODEC, (p, c) -> c.player().setData(ProgressAttachments.DAILY, p.state()));
        registrar.playToClient(QuestResultPayload.TYPE,QuestResultPayload.CODEC,(p,c)->GuildboundClient.questResult(p.message()));
        registrar.playToServer(DailyActionPayload.TYPE, DailyActionPayload.CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer player) {
                boolean accepted=dev.guildbound.server.DailyService.action(player,p);
                String message=accepted ? (p.claim()?"quest.guildbound.reward_received":"quest.guildbound.accepted")
                        : !canUseDesk(player,p.desk())?"quest.guildbound.too_far"
                        : p.day()!=player.getData(ProgressAttachments.DAILY).day()?"quest.guildbound.new_day"
                        : !player.getData(ProgressAttachments.GUILD_SESSION).matches(p.token(),p.desk(),player.level().dimension(),player.serverLevel().getGameTime())?"quest.guildbound.session_expired":"message.guildbound.quest_denied";
                PacketDistributor.sendToPlayer(player,new QuestResultPayload(message));
                sync(player);
            }
        });
        registrar.playToServer(ChooseSubclassPayload.TYPE, ChooseSubclassPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                if (!tryChooseSubclass(player, payload))
                    player.displayClientMessage(Component.translatable("message.guildbound.specialization_denied"), true);
                sync(player);
            }
        });
        registrar.playToClient(QuestsPayload.TYPE, QuestsPayload.CODEC,
                (payload, context) -> context.player().setData(ProgressAttachments.QUESTS, payload.state()));
        registrar.playToServer(RebuildPayload.TYPE, RebuildPayload.CODEC, RpgNetwork::rebuild);
        registrar.playToClient(TalentsPayload.TYPE, TalentsPayload.CODEC,
                (payload, context) -> context.player().setData(ProgressAttachments.TALENTS, payload.state()));
        registrar.playToServer(BuyTalentPayload.TYPE, BuyTalentPayload.CODEC, RpgNetwork::buyTalent);
        registrar.playToClient(ProgressPayload.TYPE, ProgressPayload.CODEC,
                (payload, context) -> context.player().setData(ProgressAttachments.PROGRESS, payload.progress()));
        registrar.playToClient(OpenGuildPayload.TYPE, OpenGuildPayload.CODEC,
                (payload, context) -> GuildboundClient.openGuild(payload.desk(), payload.token(), payload.notice()));
        registrar.playToServer(RegisterClassPayload.TYPE, RegisterClassPayload.CODEC, RpgNetwork::registerClass);
        registrar.playToClient(ResourcesPayload.TYPE, ResourcesPayload.CODEC,
                (payload, context) -> {
                    context.player().setData(ResourceAttachments.RESOURCES, payload.state());
                    context.player().setData(ProgressAttachments.FIRE_COOLDOWN, payload.fireCooldown());
                });
        registrar.playToServer(UseAbilityPayload.TYPE, UseAbilityPayload.CODEC, CombatEvents::useAbility);
    }

    public static void sync(ServerPlayer player) {
        dev.guildbound.server.DailyService.refresh(player);
        dev.guildbound.server.DailyService.sync(player);
        PacketDistributor.sendToPlayer(player, new QuestsPayload(player.getData(ProgressAttachments.QUESTS)));
        PacketDistributor.sendToPlayer(player, new ProgressPayload(player.getData(ProgressAttachments.PROGRESS)));
        CombatEvents.sync(player);
        PacketDistributor.sendToPlayer(player, new TalentsPayload(player.getData(ProgressAttachments.TALENTS)));
    }

    private static void buyTalent(BuyTalentPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
        var previous = player.getData(ProgressAttachments.TALENTS);
        var next = previous.buy(player.getData(ProgressAttachments.PROGRESS), payload.talent(), payload.expectedSpent());
        if (!next.equals(previous)) player.setData(ProgressAttachments.TALENTS, next);
        PacketDistributor.sendToPlayer(player, new TalentsPayload(next));
    }

    public static void openRegistration(ServerPlayer player, BlockPos position) { openRegistration(player, position, ""); }
    private static void openRegistration(ServerPlayer player, BlockPos position, String notice) {
        sync(player);
        var session = new dev.guildbound.server.GuildSession(java.util.UUID.randomUUID(), position.immutable(),
                player.level().dimension(), player.serverLevel().getGameTime() + 1200);
        player.setData(ProgressAttachments.GUILD_SESSION, session);
        PacketDistributor.sendToPlayer(player, new OpenGuildPayload(position, session.token(), notice));
    }

    private static void rebuild(RebuildPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!tryRebuild(player, payload)) {
            sync(player);
            if (canUseDesk(player, payload.desk())) openRegistration(player, payload.desk(), "message.guildbound.rebuild_denied");
            else PacketDistributor.sendToPlayer(player, new OpenGuildPayload(payload.desk(), java.util.UUID.randomUUID(), "message.guildbound.rebuild_denied"));
        }
    }

    public static boolean tryRebuild(ServerPlayer player, RebuildPayload payload) {
        var progress = player.getData(ProgressAttachments.PROGRESS);
        var session = player.getData(ProgressAttachments.GUILD_SESSION);
        if (!player.isAlive() || player.isSpectator() || progress.tracks().size() != 1
                || progress.totalLevel() != payload.expectedLevel() || progress.experience() != payload.expectedXp()
                || !session.matches(payload.token(), payload.desk(), player.level().dimension(), player.serverLevel().getGameTime())
                || !canUseDesk(player, payload.desk())) return false;
        var replacement = progress.rebuild(payload.heroClass());
        player.setData(ProgressAttachments.GUILD_SESSION, dev.guildbound.server.GuildSession.empty());
        player.setData(ProgressAttachments.PROGRESS, replacement);
        player.setData(ProgressAttachments.TALENTS, dev.guildbound.progression.TalentState.empty());
        player.stopUsingItem();
        player.setSprinting(false);
        dev.guildbound.server.ClassBonusEvents.refresh(player);
        openRegistration(player, payload.desk(), "message.guildbound.rebuilt");
        return true;
    }

    public static boolean tryQuestAction(ServerPlayer player, QuestActionPayload payload) {
        var state = player.getData(ProgressAttachments.QUESTS);
        var session = player.getData(ProgressAttachments.GUILD_SESSION);
        if (!player.isAlive() || player.isSpectator() || !player.getData(ProgressAttachments.PROGRESS).registered()
                || state.finished() || payload.contract() != state.completed()
                || !session.matches(payload.token(), payload.desk(), player.level().dimension(), player.serverLevel().getGameTime())
                || !canUseDesk(player, payload.desk())) return false;
        if (payload.claim()) {
            if (!state.ready()) return false;
            // Mark claimed before awarding; a replay cannot grant rewards twice.
            player.setData(ProgressAttachments.QUESTS, state.claim());
            var contract = state.contract();
            dev.guildbound.server.ProgressEvents.award(player, contract.experience);
            var reward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, contract.emeralds);
            if (!player.getInventory().add(reward)) player.drop(reward, false);
            player.displayClientMessage(Component.translatable("message.guildbound.quest_reward", contract.experience, contract.emeralds), false);
        } else {
            if (state.active()) return false;
            player.setData(ProgressAttachments.QUESTS, state.accept());
            player.displayClientMessage(Component.translatable("message.guildbound.quest_accepted"), true);
        }
        return true;
    }

    public static boolean tryChooseSubclass(ServerPlayer player, ChooseSubclassPayload payload) {
        var progress = player.getData(ProgressAttachments.PROGRESS);
        var session = player.getData(ProgressAttachments.GUILD_SESSION);
        if (!player.isAlive() || player.isSpectator()
                || !session.matches(payload.token(), payload.desk(), player.level().dimension(), player.serverLevel().getGameTime())
                || !canUseDesk(player, payload.desk())) return false;
        var next = progress.specialize(payload.subclass());
        if (next.equals(progress)) return false;
        player.setData(ProgressAttachments.PROGRESS, next);
        dev.guildbound.server.SkillService.guildGift(player, payload.subclass());
        player.displayClientMessage(Component.translatable("message.guildbound.specialized",
                Component.translatable(payload.subclass().key())), false);
        return true;
    }

    public static boolean canUseDesk(ServerPlayer player, BlockPos position) {
        var level = player.serverLevel();
        if (!player.canInteractWithBlock(position, 0)
                || !level.getChunkSource().hasChunk(position.getX() >> 4, position.getZ() >> 4)
                || !level.getBlockState(position).is(Guildbound.GUILD_DESK.get())) return false;
        var hit = level.clip(new ClipContext(player.getEyePosition(), Vec3.atCenterOf(position),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(position);
    }

    private static void registerClass(RegisterClassPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        var progress = player.getData(ProgressAttachments.PROGRESS);
        var level = player.serverLevel();
        // Check distance before touching the chunk: arbitrary packets must not load terrain.
        if (!player.isAlive() || player.isSpectator() || progress.registered()
                || !player.canInteractWithBlock(payload.desk(), 0)
                || !level.getChunkSource().hasChunk(payload.desk().getX() >> 4, payload.desk().getZ() >> 4)
                || !level.getBlockState(payload.desk()).is(Guildbound.GUILD_DESK.get())) {
            player.displayClientMessage(Component.translatable("message.guildbound.registration_denied"), true);
            return;
        }
        var hit = level.clip(new ClipContext(player.getEyePosition(), Vec3.atCenterOf(payload.desk()),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(payload.desk())) {
            player.displayClientMessage(Component.translatable("message.guildbound.registration_denied"), true);
            return;
        }
        player.setData(ProgressAttachments.PROGRESS, progress.register(payload.heroClass()));
        sync(player);
        player.displayClientMessage(Component.translatable("message.guildbound.registered"), false);
    }
}
