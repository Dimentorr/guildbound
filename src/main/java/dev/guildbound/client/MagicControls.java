package dev.guildbound.client;

import dev.guildbound.combat.MagicGesture;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.HeroClass;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.guildbound.network.UseAbilityPayload;

public final class MagicControls {
    private static final MagicGesture[] GESTURES = {new MagicGesture(), new MagicGesture()};
    private static int circle;
    public static int circle() { return circle; }
    public static net.minecraft.client.KeyMapping binding() { return circle == 0 ? GuildboundClient.MAGIC : GuildboundClient.FIRST_CIRCLE; }
    private static java.util.UUID lastPlayer;
    private static boolean lastMage, lastRegistered, pendingGuide;
    private static long chosenUntil;
    private static final boolean[] waitForRelease = new boolean[2];
    private MagicControls() {}
    public static boolean isMage() {
        var p = Minecraft.getInstance().player;
        return p != null && p.getData(ProgressAttachments.PROGRESS).tracks().stream().anyMatch(t -> t.heroClass() == HeroClass.MAGE);
    }
    public static MagicSpell[] available() { return available(circle); }
    public static MagicSpell[] available(int spellCircle) {
        var player = Minecraft.getInstance().player;
        return player == null ? new MagicSpell[0] : java.util.Arrays.stream(MagicSpell.available(player.getData(ProgressAttachments.PROGRESS))).filter(s -> s.circle == spellCircle).toArray(MagicSpell[]::new);
    }
    public static boolean hasAbilities() { var p=Minecraft.getInstance().player; return p != null && MagicSpell.available(p.getData(ProgressAttachments.PROGRESS)).length > 0; }
    public static MagicSpell selected() { return selected(circle); }
    public static MagicSpell selected(int spellCircle) {
        var choice = (spellCircle == 0 ? ClientPreferences.SELECTED_CANTRIP : ClientPreferences.SELECTED_FIRST_CIRCLE).get();
        var options = available(spellCircle);
        return java.util.Arrays.asList(options).contains(choice) ? choice : options.length > 0 ? options[0] : MagicSpell.EMBER;
    }
    public static int cooldown() { return cooldown(circle); }
    public static int cooldown(int spellCircle) {
        var player = Minecraft.getInstance().player;
        if (selected(spellCircle).skill == dev.guildbound.progression.HeroAbility.ARCANE_BURST) return 0;
        if (player != null && selected(spellCircle).skill != null) return player.getData(ProgressAttachments.ABILITIES).cooldown(selected(spellCircle).skill);
        return player == null ? 0 : dev.guildbound.combat.CantripCooldown.remaining(
                player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES), player.getData(ProgressAttachments.FIRE_COOLDOWN));
    }
    public static void select(MagicSpell spell) {
        (circle == 0 ? ClientPreferences.SELECTED_CANTRIP : ClientPreferences.SELECTED_FIRST_CIRCLE).set(spell);
        ClientPreferences.SPEC.save();
    }
    public static void classChosen(HeroClass heroClass) {
        if (heroClass == HeroClass.MAGE) chosenUntil = System.nanoTime() / 1_000_000 + 10000;
    }
    public static boolean physicalHeld() {
        return physicalHeld(binding());
    }
    private static boolean physicalHeld(net.minecraft.client.KeyMapping mapping) {
        var key = mapping.getKey();
        if (key.getValue() < 0) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE)
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM)
            return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, key.getValue());
        return mapping.isDown();
    }
    public static void wheelClosed() { waitForRelease[circle] = true; }
    public static void interaction(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (hasAbilities() && (GuildboundClient.MAGIC.getKey().equals(event.getKeyMapping().getKey()) || GuildboundClient.FIRST_CIRCLE.getKey().equals(event.getKeyMapping().getKey()))) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        boolean[] started = new boolean[2];
        while (GuildboundClient.MAGIC.consumeClick()) started[0]=true;
        while (GuildboundClient.FIRST_CIRCLE.consumeClick()) started[1]=true;
        boolean mage = isMage();
        if (mc.player == null) { lastPlayer = null; lastMage = false; lastRegistered = false; pendingGuide = false; chosenUntil = 0; }
        else {
            boolean samePlayer = mc.player.getUUID().equals(lastPlayer);
            if (mage && ((!lastMage && lastRegistered && samePlayer) || !ClientPreferences.MAGIC_GUIDE_SEEN.get()
                    || chosenUntil > System.nanoTime() / 1_000_000)) { pendingGuide = true; chosenUntil = 0; }
            lastPlayer = mc.player.getUUID(); lastMage = mage;
            lastRegistered = mc.player.getData(ProgressAttachments.PROGRESS).registered();
        }
        if (!mage) pendingGuide = false;
        if (pendingGuide && mc.isWindowActive() && (mc.screen == null || mc.screen instanceof CharacterScreen)) {
            pendingGuide = false;
            ClientPreferences.MAGIC_GUIDE_SEEN.set(true);
            ClientPreferences.SPEC.save();
            mc.setScreen(new MagicGuideScreen(mc.screen));
        }
        if(mc.screen != null) { for(var gesture:GESTURES) gesture.update(false,false,false,0); return; }
        int shown = circle;
        for(int i=0;i<2;i++) {
            circle=i;
            boolean held=physicalHeld(binding());
            if(waitForRelease[i]) { GESTURES[i].update(false,false,false,0); if(!held) waitForRelease[i]=false; continue; }
            var action=GESTURES[i].update(started[i],held,available().length>0 && mc.player!=null && mc.player.isAlive() && !mc.player.isSpectator() && mc.isWindowActive(),System.nanoTime()/1_000_000);
            if(started[i] && available().length>0) shown=i;
            if(action==MagicGesture.Action.CAST && cooldown()==0) {
                if(selected().skill!=null) PacketDistributor.sendToServer(new dev.guildbound.network.SkillPayload(selected().skill));
                else PacketDistributor.sendToServer(new UseAbilityPayload(selected().ability));
            } else if(action==MagicGesture.Action.OPEN) { mc.setScreen(new SpellWheelScreen()); return; }
        }
        circle=shown;
    }
}
