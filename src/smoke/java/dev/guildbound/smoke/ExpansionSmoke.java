package dev.guildbound.smoke;

import dev.guildbound.data.*;
import dev.guildbound.progression.*;
import dev.guildbound.server.*;
import dev.guildbound.world.ClassEquipment;
import dev.guildbound.combat.ResourceState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import java.util.*;

final class ExpansionSmoke {
    private static void require(boolean condition, String label) { if (!condition) throw new IllegalStateException("Expansion: " + label); }
    private static void setup(ServerPlayer p, HeroSubclass subclass, int level) {
        p.setData(ProgressAttachments.PROGRESS, new CharacterProgress(List.of(new ClassTrack(subclass, level)), 0));
        p.setData(ProgressAttachments.ABILITIES, AbilityState.ready());
        p.setData(ProgressAttachments.TALENTS, TalentState.empty());
        p.setData(ProgressAttachments.FIRE_COOLDOWN, 0);
        p.setData(ResourceAttachments.RESOURCES, ResourceState.full());
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }
    static void run(ServerPlayer p) {
        setup(p, HeroSubclass.WIZARD, 5);
        require(!CantripEvents.cast(p), "wizard without grimoire");
        p.setData(ProgressAttachments.GIFTS, 0);
        int before = p.getInventory().countItem(ClassEquipment.GRIMOIRE.get());
        SkillService.guildGift(p, HeroSubclass.WIZARD); SkillService.guildGift(p, HeroSubclass.WIZARD);
        require(p.getInventory().countItem(ClassEquipment.GRIMOIRE.get()) == before + 1, "one-time grimoire");
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ClassEquipment.GRIMOIRE.get()));
        require(CantripEvents.cast(p), "grimoire off hand enables magic");
        setup(p, HeroSubclass.BARD, 5);
        require(!SkillService.cast(p, HeroAbility.HEALING_FIELD), "bard without flute");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ClassEquipment.FLUTE.get()));
        require(SkillService.cast(p, HeroAbility.HEALING_FIELD), "bard field");
        require(p.getData(ResourceAttachments.RESOURCES).mana() == 600, "field cost");
        require(!SkillService.cast(p, HeroAbility.HEALING_FIELD), "field replay");
        require(!SkillService.cast(p, HeroAbility.HOLY_FIELD), "foreign subclass skill");
        var saved = p.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        p.load(saved);
        require(p.getData(ProgressAttachments.ABILITIES).cooldown(HeroAbility.HEALING_FIELD) > 0, "persisted cooldown");
        setup(p, HeroSubclass.PRIEST, 5); p.setHealth(5); p.setShiftKeyDown(true);
        require(HealingService.cast(p), "priest cantrip");
        require(Math.abs(p.getHealth() - 11.5F) < .01, "priest heal +30 percent");
        p.setShiftKeyDown(false);
        setup(p, HeroSubclass.NECROMANCER, 100);
        try {
            require(SkillService.cast(p, HeroAbility.RAISE_ZOMBIE), "zombie summon");
            var minions = SummonService.nearby(p);
            require(minions.size() == 1, "one zombie");
            var mob = minions.getFirst();
            require(mob.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE), "grandmaster armor");
            require(mob.getItemBySlot(EquipmentSlot.CHEST).isEnchanted(), "grandmaster enchantment");
            require(SummonService.allied(p, mob), "owner relation");
            require(mob.getTarget() != p, "summon target");
            setup(p, HeroSubclass.NECROMANCER, 5);
            require(!SkillService.cast(p, HeroAbility.RAISE_WITHER), "wither level gate");
        } finally { SummonService.nearby(p).forEach(Entity::discard); }
        setup(p, HeroSubclass.PACK_LEADER, 5);
        try { require(SkillService.cast(p, HeroAbility.CALL_PACK), "pack summon"); require(SummonService.nearby(p).size() == 3, "pack size"); }
        finally { SummonService.nearby(p).forEach(Entity::discard); }
        var desk = p.blockPosition().south(); var original = p.level().getBlockState(desk);
        try {
            p.serverLevel().setBlock(desk, dev.guildbound.Guildbound.GUILD_DESK.get().defaultBlockState(), 3);
            var token = UUID.randomUUID();
            p.setData(ProgressAttachments.GUILD_SESSION, new GuildSession(token, desk, p.level().dimension(), p.level().getGameTime() + 100));
            long oldExpiry=p.getData(ProgressAttachments.GUILD_SESSION).expires();
            DailyService.renewSession(p);
            require(p.getData(ProgressAttachments.GUILD_SESSION).expires()>oldExpiry,"desk session extended while reading");
            require(p.getData(ProgressAttachments.GUILD_SESSION).token().equals(token),"renewal keeps UI token");
            var journal = DailyJournal.empty().refresh(Math.max(0, p.server.overworld().getDayTime() / 24000), 5);
            p.setData(ProgressAttachments.DAILY, journal);
            var savedLog=p.getData(ProgressAttachments.QUEST_LOG);
            var cappedLog=QuestLog.empty();
            for(int slot=0;slot<3;slot++)cappedLog=cappedLog.add(new AcceptedQuest(journal.day()+1,0,slot,0,0));
            p.setData(ProgressAttachments.QUEST_LOG,cappedLog);
            require(!DailyService.action(p,new dev.guildbound.network.DailyActionPayload(desk,token,journal.day(),4,false)),"hidden offer cannot bypass five visible contracts");
            for(int slot=3;slot<5;slot++)cappedLog=cappedLog.add(new AcceptedQuest(journal.day()+1,0,slot,0,0));
            p.setData(ProgressAttachments.QUEST_LOG,cappedLog);
            require(!DailyService.action(p,new dev.guildbound.network.DailyActionPayload(desk,token,journal.day(),0,false)),"sixth accepted contract rejected by server");
            require(p.getData(ProgressAttachments.QUEST_LOG).entries().size()==5,"rejected acceptance preserves journal");
            p.setData(ProgressAttachments.QUEST_LOG,savedLog);
            var accept = new dev.guildbound.network.DailyActionPayload(desk, token, journal.day(), 0, false);
            require(DailyService.action(p, accept), "daily acceptance");
            require(!DailyService.action(p, accept), "daily acceptance replay");
            var claim = new dev.guildbound.network.DailyActionPayload(desk, token, journal.day(), 0, true);
            require(!DailyService.action(p, claim), "unfinished daily reward");
            journal = p.getData(ProgressAttachments.DAILY);
            for (int i = 0; i < journal.target(0); i++) journal = journal.kill("zombie", true);
            p.setData(ProgressAttachments.DAILY, journal);
            var activeLog=p.getData(ProgressAttachments.QUEST_LOG);
            for(int i=0;i<journal.target(0);i++)activeLog=activeLog.kill("zombie",true,p.server.overworld().getDayTime());
            p.setData(ProgressAttachments.QUEST_LOG,activeLog);
            int emeralds = p.getInventory().countItem(Items.EMERALD);
            require(DailyService.action(p, claim), "daily reward");
            require(!DailyService.action(p, claim), "daily reward replay");
            require(p.getInventory().countItem(Items.EMERALD) == emeralds + journal.emeralds(0), "daily emeralds");
            require(!DailyService.action(p, new dev.guildbound.network.DailyActionPayload(desk, token, journal.day() - 1, 1, false)), "expired board");
            for(int slot=1;slot<5;slot++) require(DailyService.action(p,new dev.guildbound.network.DailyActionPayload(desk,token,journal.day(),slot,false)),"all five daily slots can be accepted");
            var carried=new AcceptedQuest(Math.max(0,journal.day()-1),0,2,6,0);
            if(journal.day()>0){
                p.setData(ProgressAttachments.QUEST_LOG,p.getData(ProgressAttachments.QUEST_LOG).add(carried));
                var oldClaim=new dev.guildbound.network.DailyActionPayload(desk,token,carried.day(),carried.slot(),true);
                require(DailyService.action(p,oldClaim),"claim carried contract at desk");
                require(!DailyService.action(p,oldClaim),"carried reward cannot replay");
            }
            p.setData(ProgressAttachments.GUILD_SESSION,new GuildSession(token,desk,p.level().dimension(),p.level().getGameTime()-1));
            DailyService.renewSession(p);
            require(p.getData(ProgressAttachments.GUILD_SESSION).expires()<p.level().getGameTime(),"expired authorization is not revived");
        } finally { p.serverLevel().setBlock(desk, original, 3); }
        for (String name : ClassEquipment.ITEMS.keySet())
            require(p.serverLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("guildbound", name)).isPresent(), "recipe " + name);
        require(SkillService.compareGuard(20, 5, 10, 100) > 0 && SkillService.compareGuard(20, 10, 20, 5) > 0, "guard priority");
        var origin = p.position();
        for (var ability : HeroAbility.values()) {
            setup(p, ability.subclass, 100);
            p.setPos(origin.x, origin.y, origin.z); p.setYRot(0); p.setXRot(0); p.setHealth(5);
            p.setShiftKeyDown(true);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            if (ability.subclass == HeroSubclass.WIZARD) p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ClassEquipment.GRIMOIRE.get()));
            if (ability.subclass == HeroSubclass.BARD) p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ClassEquipment.FLUTE.get()));
            var enemy = EntityType.ZOMBIE.create(p.serverLevel());
            enemy.setPos(origin.x, origin.y, origin.z + 3);
            p.serverLevel().addFreshEntity(enemy);
            try {
                if (ability == HeroAbility.WILD_LEAP || ability == HeroAbility.SWAP_BEAST)
                    require(SummonService.summon(p, HeroAbility.WILD_COMPANION, 0), "prepare pet " + ability);
                require(SkillService.cast(p, ability), "cast " + ability);
                if (ability == HeroAbility.ARCANE_BURST) {
                    float hp = enemy.getHealth();
                    for (int i=0;i<11;i++) SkillService.tickField(p);
                    require(enemy.getHealth()==hp,"field waits 0.6s");
                    SkillService.tickField(p);
                    require(enemy.getHealth()<hp,"field damage");
                    require(p.getData(ResourceAttachments.RESOURCES).mana()==940,"field exact drain");
                    require(SkillService.cast(p,ability) && !SkillService.fieldActive(p),"field toggle off");
                    require(SkillService.cast(p,ability),"field no cooldown");
                    for(int i=0;i<200;i++) SkillService.tickField(p);
                    require(!SkillService.fieldActive(p) && p.getData(ResourceAttachments.RESOURCES).mana()==0,"field stops on empty mana");
                } else {
                    require(p.getData(ProgressAttachments.ABILITIES).cooldown(ability) > 0, "cooldown " + ability);
                    require(!SkillService.cast(p, ability), "spam " + ability);
                }
                if (ability == HeroAbility.TAUNT) require(enemy.getTarget() == p, "taunt target");
            } finally { enemy.discard(); SummonService.nearby(p).forEach(Entity::discard); }
        }
        p.setShiftKeyDown(false);
        System.out.println("GUILDBOUND_ALL_SKILLS_PASS: " + HeroAbility.values().length + " active abilities cast and reject replays");
        System.out.println("GUILDBOUND_EXPANSION_PASS: focus items, gifts, cooldowns, priest healing, summons, armor, daily replay, recipes");
    }
}
