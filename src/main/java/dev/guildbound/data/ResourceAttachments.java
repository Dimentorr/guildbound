package dev.guildbound.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.guildbound.combat.ResourceState;
import dev.guildbound.Guildbound;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ResourceAttachments {
    private ResourceAttachments() {}
    public static final Codec<ResourceState> CODEC = ResourceCodecs.CODEC;

    private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Guildbound.ID);
    public static final Supplier<AttachmentType<ResourceState>> RESOURCES = TYPES.register("resources",
            () -> AttachmentType.builder(ResourceState::full).serialize(CODEC).build());

    public static void register(IEventBus bus) { TYPES.register(bus); }
}
