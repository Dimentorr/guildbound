package dev.guildbound.world;

import dev.guildbound.Guildbound;
import dev.guildbound.progression.*;
import dev.guildbound.data.ProgressAttachments;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;

public final class ClassEquipment {
    public static final Map<String, Supplier<Item>> ITEMS = new LinkedHashMap<>();
    public static final Supplier<Item> GRIMOIRE = item("grimoire", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> FLUTE = item("flute", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> WAR_SWORD = item("war_sword", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 4, -2.4F))));
    public static final Supplier<Item> RANGER_BOW = item("ranger_bow", () -> new BowItem(new Item.Properties().durability(600)));
    public static final Supplier<Item> ARCANE_STAFF = item("arcane_staff", () -> new SwordItem(Tiers.WOOD, new Item.Properties().durability(300).attributes(SwordItem.createAttributes(Tiers.WOOD, 2, -2.4F))));
    static {
        for (var hero : HeroClass.values()) for (var type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS)) {
            String name = hero.name().toLowerCase(Locale.ROOT) + "_" + type.getName();
            item(name, () -> new ArmorItem(hero == HeroClass.WARRIOR ? ArmorMaterials.IRON : ArmorMaterials.LEATHER, type,
                    new Item.Properties().durability(type.getDurability(hero == HeroClass.WARRIOR ? 20 : 14))));
        }
    }
    private ClassEquipment() {}
    private static Supplier<Item> item(String name, Supplier<Item> factory) {
        var value = Guildbound.ITEMS.register(name, factory); ITEMS.put(name, value); return value;
    }
    public static void bootstrap() {}
    public static boolean holding(Player player, Supplier<Item> item) {
        return player.getMainHandItem().is(item.get()) || player.getOffhandItem().is(item.get());
    }
    public static HeroSubclass subclass(Player p) {
        return p.getData(ProgressAttachments.PROGRESS).tracks().stream().map(ClassTrack::subclass).filter(Objects::nonNull).findFirst().orElse(null);
    }
    public static boolean magicFocus(Player p) {
        return subclass(p) != HeroSubclass.WIZARD || holding(p, GRIMOIRE);
    }
    public static boolean fullSet(Player p, HeroClass hero) {
        if (p.getData(ProgressAttachments.PROGRESS).tracks().stream().noneMatch(t -> t.heroClass() == hero)) return false;
        for (var type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS))
            if (!p.getItemBySlot(type.getSlot()).is(ITEMS.get(hero.name().toLowerCase(Locale.ROOT) + "_" + type.getName()).get())) return false;
        return true;
    }
    public static float healingMultiplier(Player p) { return subclass(p) == HeroSubclass.PRIEST ? 1.3F : subclass(p) == HeroSubclass.WIZARD ? 1.3F : 1; }
}
