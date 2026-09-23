package dev.guildbound.data;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.*;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ProgressAttachments {
    private ProgressAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Guildbound.ID);
    public static final Supplier<AttachmentType<CharacterProgress>> PROGRESS = TYPES.register("progress",
            () -> AttachmentType.builder(CharacterProgress::empty).serialize(ProgressCodecs.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<QuestState>> QUESTS = TYPES.register("quests",
            () -> AttachmentType.builder(QuestState::empty).serialize(QuestCodecs.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Integer>> FIRE_COOLDOWN = TYPES.register("fire_cooldown",
            () -> AttachmentType.builder(() -> 0).serialize(com.mojang.serialization.Codec.intRange(0, 50)).build());
    public static final Supplier<AttachmentType<TalentState>> TALENTS = TYPES.register("talents",
            () -> AttachmentType.builder(TalentState::empty).serialize(TalentCodecs.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<AbilityState>> ABILITIES = TYPES.register("abilities",
            () -> AttachmentType.builder(AbilityState::ready).serialize(AbilityState.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Integer>> GIFTS = TYPES.register("guild_gifts",
            () -> AttachmentType.builder(() -> 0).serialize(com.mojang.serialization.Codec.intRange(0, 3)).copyOnDeath().build());
    public static final Supplier<AttachmentType<QuestLog>> QUEST_LOG = TYPES.register("quest_log",
            () -> AttachmentType.builder(QuestLog::empty).serialize(QuestLog.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<DailyJournal>> DAILY = TYPES.register("daily",
            () -> AttachmentType.builder(DailyJournal::empty).serialize(DailyJournal.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<dev.guildbound.combat.DaggerState>> DAGGER_STATE = TYPES.register("dagger_state",
            () -> AttachmentType.builder(dev.guildbound.combat.DaggerState::ready).serialize(DaggerCodecs.CODEC).build());
    public static final Supplier<AttachmentType<dev.guildbound.server.GuildSession>> GUILD_SESSION = TYPES.register("guild_session",
            () -> AttachmentType.builder(dev.guildbound.server.GuildSession::empty).build());
}
