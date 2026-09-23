package dev.guildbound;

import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.data.ResourceAttachments;
import dev.guildbound.network.RpgNetwork;
import dev.guildbound.world.GuildDeskBlock;
import dev.guildbound.world.DaggerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

@Mod(Guildbound.ID)
public final class Guildbound {
    public static final String ID = "guildbound";
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    public static final Supplier<net.minecraft.world.entity.EntityType<dev.guildbound.world.EmberBolt>> EMBER_BOLT = ENTITIES.register("ember_bolt", () ->
            net.minecraft.world.entity.EntityType.Builder.<dev.guildbound.world.EmberBolt>of(dev.guildbound.world.EmberBolt::new,
                    net.minecraft.world.entity.MobCategory.MISC).sized(.25F, .25F).clientTrackingRange(4).updateInterval(1).noSave().build("guildbound:ember_bolt"));
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final Supplier<Block> GUILD_DESK = BLOCKS.register("guild_desk", () ->
            new GuildDeskBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD)));
    public static final Supplier<Item> GUILD_DESK_ITEM = ITEMS.register("guild_desk", () ->
            new BlockItem(GUILD_DESK.get(), new Item.Properties()));
    public static final Supplier<Item> GOLDEN_DAGGER = ITEMS.register("golden_dagger", () -> new DaggerItem(Tiers.GOLD, 2, 3.6F));
    public static final Supplier<Item> IRON_DAGGER = ITEMS.register("iron_dagger", () -> new DaggerItem(Tiers.IRON, 4, 2.4F));
    public static final Supplier<Item> DIAMOND_DAGGER = ITEMS.register("diamond_dagger", () -> new DaggerItem(Tiers.DIAMOND, 5, 3F));
    public static final Supplier<Item> NETHERITE_DAGGER = ITEMS.register("netherite_dagger", () -> new DaggerItem(Tiers.NETHERITE, 6, 3.2F));
    public static final Supplier<CreativeModeTab> CREATIVE_TAB = TABS.register("guildbound", () ->
            CreativeModeTab.builder().title(Component.translatable("itemGroup.guildbound"))
                    .icon(() -> GUILD_DESK_ITEM.get().getDefaultInstance())
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .displayItems((parameters, output) -> {
                        output.accept(GUILD_DESK_ITEM.get());
                        output.accept(GOLDEN_DAGGER.get());
                        output.accept(IRON_DAGGER.get());
                        output.accept(DIAMOND_DAGGER.get());
                        output.accept(NETHERITE_DAGGER.get());
                        dev.guildbound.world.ClassEquipment.ITEMS.values().forEach(item -> output.accept(item.get()));
                    }).build());

    public Guildbound(IEventBus modBus) {
        dev.guildbound.world.ClassEquipment.bootstrap();
        BLOCKS.register(modBus);
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        ProgressAttachments.TYPES.register(modBus);
        ResourceAttachments.register(modBus);
        modBus.addListener(RpgNetwork::register);
        modBus.addListener(this::creativeTab);
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(GUILD_DESK_ITEM.get());
    }
}
