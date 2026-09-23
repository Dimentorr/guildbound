package dev.guildbound.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.guildbound.Guildbound;
import dev.guildbound.data.ResourceAttachments;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.network.UseAbilityPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(value = Guildbound.ID, dist = Dist.CLIENT)
public final class GuildboundClient {
    private static final KeyMapping MENU = new KeyMapping("key.guildbound.menu", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.guildbound");
    public static final KeyMapping DODGE = new KeyMapping("key.guildbound.dodge", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.guildbound");
    public static final KeyMapping MAGIC = new KeyMapping("key.guildbound.magic", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, "key.categories.guildbound");

    public static final KeyMapping HUD_SETTINGS = new KeyMapping("key.guildbound.hud_settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.guildbound");
    public static final KeyMapping FIRST_CIRCLE = new KeyMapping("key.guildbound.first_circle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.guildbound");

    public GuildboundClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientPreferences.SPEC);
        modBus.addListener(GuildboundClient::keys);
        modBus.addListener(GuildboundClient::renderers);
        modBus.addListener(GuildboundClient::setupEquipment);
        NeoForge.EVENT_BUS.addListener(GuildboundClient::tick);
        NeoForge.EVENT_BUS.addListener(CharacterHud::render);
        NeoForge.EVENT_BUS.addListener(CharacterHud::hideVanilla);
        NeoForge.EVENT_BUS.addListener(ShieldMovement::update);
        NeoForge.EVENT_BUS.addListener(WeaponTooltips::tooltip);
        NeoForge.EVENT_BUS.addListener(WeaponTooltips::input);
        NeoForge.EVENT_BUS.addListener(MagicControls::interaction);
    }

    private static void setupEquipment(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            var bow = dev.guildbound.world.ClassEquipment.RANGER_BOW.get();
            net.minecraft.client.renderer.item.ItemProperties.register(bow, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0 : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20F);
            net.minecraft.client.renderer.item.ItemProperties.register(bow, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0);
        });
    }

    private static void keys(RegisterKeyMappingsEvent event) {
        event.register(HUD_SETTINGS);
        event.register(MAGIC);
        event.register(FIRST_CIRCLE);
        event.register(MENU);
        event.register(DODGE);
    }

    private static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        MagicControls.tick();
        while (HUD_SETTINGS.consumeClick()) if(minecraft.player!=null && minecraft.screen==null) minecraft.setScreen(new HudSettingsScreen(null));
        while (MENU.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) minecraft.setScreen(new CharacterScreen(null));
        }
        while (DODGE.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                PacketDistributor.sendToServer(new UseAbilityPayload(UseAbilityPayload.Ability.DODGE));
            }
        }
        if (minecraft.player != null && !minecraft.player.isCreative()
                && minecraft.player.getData(ProgressAttachments.PROGRESS).registered()
                && !minecraft.player.getData(ResourceAttachments.RESOURCES).canSprint()) {
            minecraft.player.setSprinting(false);
        }
    }

    private static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Guildbound.EMBER_BOLT.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, .7F, true));
    }

    private static long questTime,questTick;
    public static void questClock(long time) { questTime=time;var mc=Minecraft.getInstance();questTick=mc.level==null?0:mc.level.getGameTime(); }
    public static long questTime() { var mc=Minecraft.getInstance();return questTime+(mc.level==null?0:Math.max(0,mc.level.getGameTime()-questTick)); }
    public static String timeLeft(long deadline) { long seconds=Math.max(0,(deadline-questTime()+19)/20);return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60); }
    public static void questResult(String key) { if(Minecraft.getInstance().screen instanceof DailyQuestScreen screen) screen.result(key); }
    public static void openGuild(BlockPos position, java.util.UUID token, String notice) {
        if (Minecraft.getInstance().player != null) Minecraft.getInstance().setScreen(new CharacterScreen(position, token, notice));
    }
}
