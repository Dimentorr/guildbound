package dev.guildbound.smoke;

import com.mojang.authlib.GameProfile;
import dev.guildbound.Guildbound;
import dev.guildbound.combat.DaggerState;
import dev.guildbound.combat.WeaponStats;
import dev.guildbound.data.ProgressAttachments;
import dev.guildbound.progression.*;
import dev.guildbound.server.ClassBonusEvents;
import dev.guildbound.server.DaggerCombat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Opt-in server-only verification. Not packaged into the distributed mod JAR. */
@EventBusSubscriber(modid = Guildbound.ID)
public final class CombatSmoke {
    private CombatSmoke() {}
    private static void close(double actual, double expected, String name) {
        if (Math.abs(actual - expected) > .001) throw new IllegalStateException(name + ": expected " + expected + ", got " + actual);
    }
    @SubscribeEvent
    public static void run(ServerStartedEvent event) {
        var level = event.getServer().overworld();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("4237d9b1-8d21-4b31-9a9b-e4139a0ab100"), "GuildboundSmoke"));
        var spawn = level.getSharedSpawnPos();
        player.setPos(spawn.getX() + .5, 250, spawn.getZ() + .5);
        player.setData(ProgressAttachments.PROGRESS, new CharacterProgress(List.of(new ClassTrack(HeroSubclass.DUELIST, 5)), 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Guildbound.IRON_DAGGER.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Guildbound.GOLDEN_DAGGER.get()));
        close(WeaponStats.value(player, player.getMainHandItem(), Attributes.ATTACK_SPEED), 3.12, "rogue iron speed");
        close(WeaponStats.value(player, player.getOffhandItem(), Attributes.ATTACK_SPEED), 4.68, "rogue gold speed");
        int[] damages = {2, 4, 5, 6};
        int[] durability = {32, 250, 1561, 2031};
        var items = List.of(Guildbound.GOLDEN_DAGGER.get(), Guildbound.IRON_DAGGER.get(), Guildbound.DIAMOND_DAGGER.get(), Guildbound.NETHERITE_DAGGER.get());
        for (int index = 0; index < items.size(); index++) {
            var stack = new ItemStack(items.get(index));
            close(WeaponStats.value(player, stack, Attributes.ATTACK_DAMAGE), damages[index], "material damage");
            close(stack.getMaxDamage(), durability[index], "material durability");
        }
        var target = EntityType.COW.create(level);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.setHealth(100);
        target.setPos(player.getX(), player.getY(), player.getZ() + 2);
        DaggerCombat.attack(player, target);
        close(target.getHealth(), 96, "main-hand hit");
        DaggerCombat.attack(player, target);
        close(target.getHealth(), 96, "same-tick spam rejected");
        var state = player.getData(ProgressAttachments.DAGGER_STATE);
        int wait = state.shared();
        for (int tick = 0; tick < wait; tick++) state = state.tick();
        player.setData(ProgressAttachments.DAGGER_STATE, state);
        // Do not tick target: vanilla hurt immunity is still active here.
        DaggerCombat.attack(player, target);
        close(target.getHealth(), 94, "off-hand hit during vanilla immunity");
        close(player.getMainHandItem().getDamageValue(), 1, "main durability");
        close(player.getOffhandItem().getDamageValue(), 1, "off durability");
        close(player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES).stamina(), 920, "exactly two stamina costs");
        player.setData(ProgressAttachments.TALENTS, new TalentState(Map.of(Talent.VITALITY, 1, Talent.KNIFEWORK, 1)));
        ClassBonusEvents.tick(new PlayerTickEvent.Pre(player));
        close(player.getMaxHealth(), 22, "talent maximum health");
        close(WeaponStats.value(player, player.getMainHandItem(), Attributes.ATTACK_SPEED), 3.24, "talent speed no double count");
        player.setHealth(21);
        var saved = player.saveWithoutId(new CompoundTag());
        var restored = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("4237d9b1-8d21-4b31-9a9b-e4139a0ab101"), "GuildboundReload"));
        restored.load(saved);
        close(restored.getMaxHealth(), 22, "persisted maximum health");
        close(restored.getHealth(), 21, "health survives reload");
        close(restored.getData(ProgressAttachments.TALENTS).spent(), 2, "persisted talents");
        for (String material : List.of("golden", "iron", "diamond", "netherite")) {
            if (level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Guildbound.ID, material + "_dagger")).isEmpty())
                throw new IllegalStateException("Missing recipe " + material);
        }
        var deskPos = net.minecraft.core.BlockPos.containing(player.getX(), player.getY(), player.getZ() + 1);
        var oldBlock = level.getBlockState(deskPos);
        var beforeResources = player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES);
        var beforeCooldowns = player.getData(ProgressAttachments.DAGGER_STATE);
        dev.guildbound.server.DailyService.refresh(player);
        var beforeQuests = new dev.guildbound.progression.QuestState(0, true, 2);
        player.setData(ProgressAttachments.QUESTS, beforeQuests);
        try {
            level.setBlock(deskPos, Guildbound.GUILD_DESK.get().defaultBlockState(), 3);
            var token = UUID.randomUUID();
            player.setData(ProgressAttachments.GUILD_SESSION, new dev.guildbound.server.GuildSession(token, deskPos, level.dimension(), level.getGameTime() + 100));
            var request = new dev.guildbound.network.RebuildPayload(deskPos, token, dev.guildbound.progression.HeroClass.MAGE, 5, 0);
            player.setPos(player.getX(), player.getY(), player.getZ() - 30);
            if (dev.guildbound.network.RpgNetwork.tryRebuild(player, request)) throw new IllegalStateException("Remote rebuild allowed");
            player.setPos(player.getX(), player.getY(), player.getZ() + 30);
            if (!dev.guildbound.network.RpgNetwork.tryRebuild(player, request)) throw new IllegalStateException("Valid rebuild denied");
            close(player.getData(ProgressAttachments.PROGRESS).totalLevel(), 4, "rebuild level cost");
            close(player.getData(ProgressAttachments.TALENTS).spent(), 0, "reset talents");
            close(player.getMaxHealth(), 20, "remove vitality immediately");
            close(player.getHealth(), 20, "clamp health without healing");
            if (!player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES).equals(beforeResources)
                    || !player.getData(ProgressAttachments.DAGGER_STATE).equals(beforeCooldowns))
                throw new IllegalStateException("Rebuild refreshed resources or cooldowns");
            if (dev.guildbound.network.RpgNetwork.tryRebuild(player, request)) throw new IllegalStateException("Rebuild replay applied");
            close(player.getMainHandItem().getDamageValue(), 1, "rebuild kept inventory");
            if (!player.getData(ProgressAttachments.QUESTS).equals(beforeQuests)) throw new IllegalStateException("Path change reset quests");
            var specializeToken = player.getData(ProgressAttachments.GUILD_SESSION).token();
            var specialize = new dev.guildbound.network.ChooseSubclassPayload(deskPos, specializeToken, HeroSubclass.SORCERER);
            if (dev.guildbound.network.RpgNetwork.tryChooseSubclass(player, specialize)) throw new IllegalStateException("Level four specialization allowed");
            dev.guildbound.server.ProgressEvents.award(player, 525);
            close(player.getData(ProgressAttachments.PROGRESS).totalLevel(), 5, "specialization threshold");
            close(player.getData(ProgressAttachments.TALENTS).available(player.getData(ProgressAttachments.PROGRESS)), 9, "new talent budget");
            if (dev.guildbound.network.RpgNetwork.tryChooseSubclass(player,
                    new dev.guildbound.network.ChooseSubclassPayload(deskPos, UUID.randomUUID(), HeroSubclass.SORCERER)))
                throw new IllegalStateException("Forged specialization token accepted");
            if (dev.guildbound.network.RpgNetwork.tryChooseSubclass(player,
                    new dev.guildbound.network.ChooseSubclassPayload(deskPos, specializeToken, HeroSubclass.HUNTER)))
                throw new IllegalStateException("Wrong class specialization accepted");
            player.setPos(player.getX(), player.getY(), player.getZ() - 30);
            if (dev.guildbound.network.RpgNetwork.tryChooseSubclass(player, specialize)) throw new IllegalStateException("Remote specialization accepted");
            player.setPos(player.getX(), player.getY(), player.getZ() + 30);
            if (!dev.guildbound.network.RpgNetwork.tryChooseSubclass(player, specialize)) throw new IllegalStateException("Valid specialization denied");
            if (dev.guildbound.network.RpgNetwork.tryChooseSubclass(player, specialize)) throw new IllegalStateException("Specialization replay accepted");
            restored.load(player.saveWithoutId(new CompoundTag()));
            if (restored.getData(ProgressAttachments.PROGRESS).tracks().getFirst().subclass() != HeroSubclass.SORCERER)
                throw new IllegalStateException("Specialization not saved");
            System.out.println("GUILDBOUND_PROGRESSION_V2_PASS: level gate, point budget, subclass token, distance, class, replay, persistence");
            player.setData(ProgressAttachments.QUESTS, dev.guildbound.progression.QuestState.empty());
            player.setData(ProgressAttachments.PROGRESS, CharacterProgress.empty().register(HeroSubclass.SORCERER));
            var questToken = player.getData(ProgressAttachments.GUILD_SESSION).token();
            var accept = new dev.guildbound.network.QuestActionPayload(deskPos, questToken, 0, false);
            var claim = new dev.guildbound.network.QuestActionPayload(deskPos, questToken, 0, true);
            if (dev.guildbound.network.RpgNetwork.tryQuestAction(player, claim)) throw new IllegalStateException("Unaccepted reward granted");
            player.setPos(player.getX(), player.getY(), player.getZ() - 30);
            if (dev.guildbound.network.RpgNetwork.tryQuestAction(player, accept)) throw new IllegalStateException("Remote quest accepted");
            player.setPos(player.getX(), player.getY(), player.getZ() + 30);
            if (!dev.guildbound.network.RpgNetwork.tryQuestAction(player, accept)) throw new IllegalStateException("Quest accept failed");
            if (dev.guildbound.network.RpgNetwork.tryQuestAction(player, accept)) throw new IllegalStateException("Duplicate acceptance applied");
            if (dev.guildbound.network.RpgNetwork.tryQuestAction(player, claim)) throw new IllegalStateException("Incomplete reward granted");
            for (int i = 0; i < 5; i++) {
                var zombie = EntityType.ZOMBIE.create(level);
                dev.guildbound.server.ProgressEvents.kill(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(zombie, player.damageSources().playerAttack(player)));
                zombie.discard();
            }
            restored.load(player.saveWithoutId(new CompoundTag()));
            if (!restored.getData(ProgressAttachments.QUESTS).ready()) throw new IllegalStateException("Quest progress lost on reload");
            int emeralds = player.getInventory().countItem(net.minecraft.world.item.Items.EMERALD);
            if (!dev.guildbound.network.RpgNetwork.tryQuestAction(player, claim)) throw new IllegalStateException("Ready reward denied");
            close(player.getInventory().countItem(net.minecraft.world.item.Items.EMERALD), emeralds + 2, "quest emerald reward");
            close(player.getData(ProgressAttachments.PROGRESS).totalLevel(), 2, "quest level reward");
            close(player.getData(ProgressAttachments.PROGRESS).experience(), 125, "kill plus quest XP");
            if (dev.guildbound.network.RpgNetwork.tryQuestAction(player, claim)) throw new IllegalStateException("Reward replay applied");
            for (int index = 1; index < 3; index++) {
                if (!dev.guildbound.network.RpgNetwork.tryQuestAction(player,
                        new dev.guildbound.network.QuestActionPayload(deskPos, questToken, index, false))) throw new IllegalStateException("Next quest denied");
                var kind = index == 1 ? EntityType.ZOMBIE : EntityType.SKELETON;
                dev.guildbound.server.ProgressEvents.recordQuestKill(player, EntityType.CREEPER);
                close(player.getData(ProgressAttachments.QUESTS).kills(), 0, "wrong target ignored");
                int required = player.getData(ProgressAttachments.QUESTS).contract().target;
                for (int i = 0; i < required; i++) dev.guildbound.server.ProgressEvents.recordQuestKill(player, kind);
                if (!dev.guildbound.network.RpgNetwork.tryQuestAction(player,
                        new dev.guildbound.network.QuestActionPayload(deskPos, questToken, index, true))) throw new IllegalStateException("Next reward denied");
            }
            if (!player.getData(ProgressAttachments.QUESTS).finished()) throw new IllegalStateException("Chain not completed");
            close(player.getInventory().countItem(net.minecraft.world.item.Items.EMERALD), emeralds + 9, "all contract rewards");
        } finally { level.setBlock(deskPos, oldBlock, 3); }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Guildbound.IRON_DAGGER.get()));
        player.setData(ProgressAttachments.DAGGER_STATE, dev.guildbound.combat.DaggerState.ready());
        target.setHealth(100);
        DaggerCombat.attack(player, target);
        close(target.getHealth(), 96, "offhand-only dagger damage");
        close(player.getOffhandItem().getDamageValue(), 1, "offhand-only durability");
        DaggerCombat.attack(player, target);
        close(target.getHealth(), 96, "offhand-only spam denied");

        player.setData(ProgressAttachments.PROGRESS, CharacterProgress.empty().register(HeroSubclass.SORCERER));
        player.setYRot(0); player.setXRot(0);
        var spellTarget = EntityType.COW.create(level);
        spellTarget.setNoAi(true);
        spellTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        spellTarget.setHealth(100);
        spellTarget.setPos(player.getX(), player.getY() + 1, player.getZ() + 4);
        level.addFreshEntity(spellTarget);
        var manaBefore = player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES).mana();
        if (!dev.guildbound.server.CantripEvents.cast(player)) throw new IllegalStateException("Cantrip denied");
        if (dev.guildbound.server.CantripEvents.cast(player)) throw new IllegalStateException("Cantrip spam allowed");
        player.setHealth(10);
        if (dev.guildbound.server.HealingService.cast(player)) throw new IllegalStateException("Heal bypassed fire cooldown");
        close(player.getHealth(), 10, "shared lock preserves health");
        var bolts = level.getEntitiesOfClass(dev.guildbound.world.EmberBolt.class, player.getBoundingBox().inflate(40));
        if (bolts.size() != 1) throw new IllegalStateException("Expected exactly one projectile");
        var bolt = bolts.getFirst();
        for (int i = 0; i < 12 && !bolt.isRemoved(); i++) bolt.tick();
        close(spellTarget.getHealth(), 95, "cantrip projectile damage");
        if (spellTarget.isOnFire() || !bolt.isRemoved()) throw new IllegalStateException("Cantrip ignited target or survived hit");
        close(player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES).mana(), manaBefore, "cantrip no mana cost");
        restored.load(player.saveWithoutId(new CompoundTag()));
        close(restored.getData(ProgressAttachments.FIRE_COOLDOWN), 50, "cantrip cooldown persists");
        player.setData(ProgressAttachments.PROGRESS, new CharacterProgress(List.of(new ClassTrack(HeroSubclass.SORCERER, 5)), 0));
        player.setData(ProgressAttachments.TALENTS, new TalentState(Map.of(Talent.EMBER, 2)));
        player.setData(ProgressAttachments.FIRE_COOLDOWN, 0);
        spellTarget.setHealth(100);
        spellTarget.invulnerableTime = 0;
        if (!dev.guildbound.server.CantripEvents.cast(player)) throw new IllegalStateException("Upgraded cantrip denied");
        close(player.getData(ProgressAttachments.FIRE_COOLDOWN), 30, "upgraded cooldown");
        var upgraded = level.getEntitiesOfClass(dev.guildbound.world.EmberBolt.class, player.getBoundingBox().inflate(40)).getFirst();
        for (int i = 0; i < 12 && !upgraded.isRemoved(); i++) upgraded.tick();
        close(spellTarget.getHealth(), 91.5, "level five plus two talent ranks damage");
        restored.load(player.saveWithoutId(new CompoundTag()));
        close(restored.getData(ProgressAttachments.TALENTS).rank(Talent.EMBER), 2, "cantrip talent persists");
        spellTarget.discard();
        var wallPos = net.minecraft.core.BlockPos.containing(player.getX(), player.getEyeY(), player.getZ() + 2);
        var oldWall = level.getBlockState(wallPos);
        try {
            level.setBlock(wallPos, net.minecraft.world.level.block.Blocks.WHITE_WOOL.defaultBlockState(), 3);
            player.setData(ProgressAttachments.FIRE_COOLDOWN, 0);
            if (!dev.guildbound.server.CantripEvents.cast(player)) throw new IllegalStateException("Second cantrip denied");
            var wallBolt = level.getEntitiesOfClass(dev.guildbound.world.EmberBolt.class, player.getBoundingBox().inflate(40)).getFirst();
            for (int i = 0; i < 12 && !wallBolt.isRemoved(); i++) wallBolt.tick();
            if (!wallBolt.isRemoved() || !level.getBlockState(wallPos).is(net.minecraft.world.level.block.Blocks.WHITE_WOOL)
                    || level.getBlockState(wallPos.above()).is(net.minecraft.world.level.block.Blocks.FIRE))
                throw new IllegalStateException("Cantrip damaged or ignited terrain");
        } finally { level.setBlock(wallPos, oldWall, 3); }
        player.setData(ProgressAttachments.FIRE_COOLDOWN, 0);
        player.setData(ProgressAttachments.PROGRESS, CharacterProgress.empty().register(HeroSubclass.HUNTER));
        if (dev.guildbound.server.CantripEvents.cast(player)) throw new IllegalStateException("Non-mage cast allowed");
        player.setData(ProgressAttachments.PROGRESS, new CharacterProgress(List.of(new ClassTrack(HeroSubclass.SORCERER, 3)), 0));
        player.setData(ProgressAttachments.TALENTS, new TalentState(Map.of(Talent.RESTORATION, 1)));
        player.setData(dev.guildbound.data.ResourceAttachments.RESOURCES, dev.guildbound.combat.ResourceState.full());
        var ally = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("4237d9b1-8d21-4b31-9a9b-e4139a0ab102"), "GuildboundAlly"));
        ally.setPos(player.getX(), player.getY(), player.getZ() + 6);
        ally.setHealth(10);
        level.addNewPlayer(ally);
        try {
            if (dev.guildbound.combat.HealingTarget.find(player) != ally) throw new IllegalStateException("Ally not targeted");
            if (!dev.guildbound.server.HealingService.cast(player)) throw new IllegalStateException("Ally heal denied");
            close(ally.getHealth(), 16, "ranged healing with talent");
            close(player.getHealth(), 10, "ranged healing leaves caster unchanged");
            close(player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES).mana(), 750, "healing spends caster mana once");
            if (dev.guildbound.server.CantripEvents.cast(player)) throw new IllegalStateException("Fire bypassed heal cooldown");
            if (dev.guildbound.server.HealingService.cast(player)) throw new IllegalStateException("Repeated heal allowed");
            player.setData(dev.guildbound.data.ResourceAttachments.RESOURCES, dev.guildbound.combat.ResourceState.full());
            player.setShiftKeyDown(true);
            if (!dev.guildbound.server.HealingService.cast(player)) throw new IllegalStateException("Forced self heal denied");
            close(player.getHealth(), 16, "forced self healing");
            close(ally.getHealth(), 16, "forced self leaves ally unchanged");
            player.setShiftKeyDown(false);
            player.setData(dev.guildbound.data.ResourceAttachments.RESOURCES, dev.guildbound.combat.ResourceState.full());
            ally.setHealth(ally.getMaxHealth());
            if (dev.guildbound.server.HealingService.cast(player)) throw new IllegalStateException("Full ally consumed healing");
            close(player.getData(dev.guildbound.data.ResourceAttachments.RESOURCES).mana(), 1000, "full target costs nothing");
            ally.setHealth(10);
            var barrierPos = net.minecraft.core.BlockPos.containing(player.getX(), player.getEyeY(), player.getZ() + 3);
            var barrierBefore = level.getBlockState(barrierPos);
            try {
                level.setBlock(barrierPos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                if (dev.guildbound.combat.HealingTarget.find(player) != player) throw new IllegalStateException("Healing targeted through wall");
                player.setHealth(player.getMaxHealth());
                if (dev.guildbound.server.HealingService.cast(player)) throw new IllegalStateException("Healing passed through wall");
                close(ally.getHealth(), 10, "wall protects target selection");
            } finally { level.setBlock(barrierPos, barrierBefore, 3); }
            ally.setPos(player.getX(), player.getY(), player.getZ() + 20);
            if (dev.guildbound.combat.HealingTarget.find(player) != player) throw new IllegalStateException("Healing range exceeded");
        } finally {
            player.setShiftKeyDown(false);
            level.removePlayerImmediately(ally, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        ExpansionSmoke.run(player);
        target.discard();
        player.discard();
        restored.discard();
        System.out.println("GUILDBOUND_COMBAT_SMOKE_PASS: prior regression, shared cantrip lock in both directions, ally healing, self override, mana, wall and range");
    }
}
