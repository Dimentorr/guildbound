package dev.guildbound.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.guildbound.Guildbound;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.network.RpgNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Guildbound.ID)
public final class ProgressEvents {
    private ProgressEvents() {}

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync(event); }
    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync(event); }
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync(event); }

    private static void sync(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var progress = player.getData(ProgressAttachments.PROGRESS);
            // A lower XP threshold must not discard already earned progress on upgrade.
            if (progress.tracks().size() == 1) player.setData(ProgressAttachments.PROGRESS, progress.awardExperience(0));
            RpgNetwork.sync(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void kill(LivingDeathEvent event) {
        if (SummonService.owned(event.getEntity())) return;
        var source = event.getSource().getEntity();
        ServerPlayer player = source instanceof ServerPlayer direct ? direct : source == null ? null : SummonService.owner(source);
        if (event.getEntity() instanceof Enemy && player != null) {
            DailyService.kill(player, event.getEntity());
            award(player, 25);
            recordQuestKill(player, event.getEntity().getType());
        }
    }

    public static void recordQuestKill(ServerPlayer player, net.minecraft.world.entity.EntityType<?> type) {
        if (player.isCreative() || player.isSpectator() || !player.getData(ProgressAttachments.PROGRESS).registered()) return;
        var state = player.getData(ProgressAttachments.QUESTS);
        if (!state.active()) return;
        boolean matches = switch (state.contract()) {
            case PATROL -> true;
            case RESTLESS_DEAD -> type == net.minecraft.world.entity.EntityType.ZOMBIE;
            case BONE_HUNT -> type == net.minecraft.world.entity.EntityType.SKELETON;
        };
        var updated = state.kill(matches);
        if (!updated.equals(state)) {
            player.setData(ProgressAttachments.QUESTS, updated);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new dev.guildbound.network.QuestsPayload(updated));
            if (updated.ready()) player.displayClientMessage(Component.translatable("message.guildbound.quest_ready"), false);
        }
    }

    public static void award(ServerPlayer player, int amount) {
        var old = player.getData(ProgressAttachments.PROGRESS);
        var updated = old.awardExperience(amount);
        if (!old.equals(updated)) {
            player.setData(ProgressAttachments.PROGRESS, updated);
            RpgNetwork.sync(player);
            if (updated.totalLevel() > old.totalLevel()) {
                player.displayClientMessage(Component.translatable("message.guildbound.level_up", updated.totalLevel()), false);
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guildbound")
                .then(Commands.literal("status").executes(context -> {
                    var profile = context.getSource().getPlayerOrException().getData(ProgressAttachments.PROGRESS);
                    context.getSource().sendSuccess(() -> Component.translatable("message.guildbound.status",
                            profile.totalLevel(), profile.experience()), false);
                    return 1;
                }))
                .then(Commands.literal("xp").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000))
                                .executes(context -> {
                                    award(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "amount"));
                                    return 1;
                                }))));
    }
}
