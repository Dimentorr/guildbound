package dev.guildbound.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientPreferences {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ANIMATIONS;
    public static final ModConfigSpec.BooleanValue QUEST_TRACKER;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.DoubleValue HUD_OPACITY;
    public static final ModConfigSpec.EnumValue<BarOrder> BAR_ORDER;
    public enum BarOrder { HP_MP_SP, HP_SP_MP, MP_HP_SP, MP_SP_HP, SP_HP_MP, SP_MP_HP;
        public int y(String resource) { return 20 + java.util.Arrays.asList(name().split("_")).indexOf(resource) * 17; }
    }
    public static final ModConfigSpec.EnumValue<MagicSpell> SELECTED_CANTRIP;
    public static final ModConfigSpec.EnumValue<MagicSpell> SELECTED_FIRST_CIRCLE;
    public static final ModConfigSpec.BooleanValue MAGIC_GUIDE_SEEN;

    static {
        var builder = new ModConfigSpec.Builder();
        SELECTED_CANTRIP = builder.defineEnum("selectedCantrip", MagicSpell.EMBER);
        SELECTED_FIRST_CIRCLE = builder.defineEnum("selectedFirstCircle", MagicSpell.ARCANE_BURST);
        MAGIC_GUIDE_SEEN = builder.define("magicGuideSeen", false);
        ANIMATIONS = builder.comment("Animate floating menu transitions.").define("animations", true);
        QUEST_TRACKER = builder.comment("Show the compact active guild contract below the status panel.").define("questTracker", true);
        HUD_SCALE = builder.comment("Scale of the compact status panel and ability reminders.")
                .defineInRange("hudScale", 1.0, 0.75, 1.75);
        HUD_OPACITY = builder.defineInRange("hudOpacity", 0.65, 0.0, 1.0);
        BAR_ORDER = builder.defineEnum("resourceBarOrder", BarOrder.HP_MP_SP);
        SPEC = builder.build();
    }

    private ClientPreferences() {}
}
