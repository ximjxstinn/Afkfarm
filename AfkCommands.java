package de.ximjxstinn.afkfarm;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** Client-Befehl /afkfarm zum Ein-/Ausschalten und Einstellen. */
public final class AfkCommands {
    private AfkCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, AfkFarmController controller) {
        dispatcher.register(literal("afkfarm")
                .executes(ctx -> {
                    controller.toggle(MinecraftClient.getInstance());
                    return 1;
                })
                .then(literal("status").executes(ctx -> {
                    AfkConfig c = AfkFarmClient.CONFIG;
                    ctx.getSource().sendFeedback(Text.literal(
                            "§6AFK-Farm §7| " + (controller.isEnabled() ? "§aAN" : "§cAUS") +
                            "\n§7Modus: §f" + (c.autoAim ? "Auto-Aim (" + c.range + " Blöcke)" : "Fadenkreuz") +
                            "\n§7Auto-Essen: §f" + (c.autoEat ? "an, unter " + c.eatBelow : "aus") +
                            "\n§7Min. Haltbarkeit: §f" + c.minDurability + (c.switchWeapon ? " (Waffenwechsel an)" : " (Waffenwechsel aus)") +
                            "\n§7Extra-Verzögerung: §f0–" + c.extraDelayMax + " Ticks" +
                            "\n§7Benannte Mobs ignorieren: §f" + (c.ignoreNamed ? "ja" : "nein")));
                    return 1;
                }))
                .then(literal("aim").then(argument("an", BoolArgumentType.bool()).executes(ctx -> {
                    AfkFarmClient.CONFIG.autoAim = BoolArgumentType.getBool(ctx, "an");
                    return saved(ctx.getSource(), "Auto-Aim", AfkFarmClient.CONFIG.autoAim ? "an" : "aus (nur Fadenkreuz)");
                })))
                .then(literal("range").then(argument("bloecke", DoubleArgumentType.doubleArg(1.0, 3.0)).executes(ctx -> {
                    AfkFarmClient.CONFIG.range = DoubleArgumentType.getDouble(ctx, "bloecke");
                    return saved(ctx.getSource(), "Reichweite", AfkFarmClient.CONFIG.range + " Blöcke");
                })))
                .then(literal("autoeat").then(argument("an", BoolArgumentType.bool()).executes(ctx -> {
                    AfkFarmClient.CONFIG.autoEat = BoolArgumentType.getBool(ctx, "an");
                    return saved(ctx.getSource(), "Auto-Essen", AfkFarmClient.CONFIG.autoEat ? "an" : "aus");
                })))
                .then(literal("eatbelow").then(argument("hunger", IntegerArgumentType.integer(1, 19)).executes(ctx -> {
                    AfkFarmClient.CONFIG.eatBelow = IntegerArgumentType.getInteger(ctx, "hunger");
                    return saved(ctx.getSource(), "Essen unter Hunger", String.valueOf(AfkFarmClient.CONFIG.eatBelow));
                })))
                .then(literal("durability").then(argument("min", IntegerArgumentType.integer(0, 500)).executes(ctx -> {
                    AfkFarmClient.CONFIG.minDurability = IntegerArgumentType.getInteger(ctx, "min");
                    return saved(ctx.getSource(), "Min. Haltbarkeit", String.valueOf(AfkFarmClient.CONFIG.minDurability));
                })))
                .then(literal("switchweapon").then(argument("an", BoolArgumentType.bool()).executes(ctx -> {
                    AfkFarmClient.CONFIG.switchWeapon = BoolArgumentType.getBool(ctx, "an");
                    return saved(ctx.getSource(), "Waffenwechsel", AfkFarmClient.CONFIG.switchWeapon ? "an" : "aus");
                })))
                .then(literal("delay").then(argument("maxTicks", IntegerArgumentType.integer(0, 10)).executes(ctx -> {
                    AfkFarmClient.CONFIG.extraDelayMax = IntegerArgumentType.getInteger(ctx, "maxTicks");
                    return saved(ctx.getSource(), "Extra-Verzögerung", "0–" + AfkFarmClient.CONFIG.extraDelayMax + " Ticks");
                })))
                .then(literal("ignorenamed").then(argument("an", BoolArgumentType.bool()).executes(ctx -> {
                    AfkFarmClient.CONFIG.ignoreNamed = BoolArgumentType.getBool(ctx, "an");
                    return saved(ctx.getSource(), "Benannte Mobs ignorieren", AfkFarmClient.CONFIG.ignoreNamed ? "ja" : "nein");
                })))
        );
    }

    private static int saved(FabricClientCommandSource source, String name, String value) {
        AfkFarmClient.CONFIG.save();
        source.sendFeedback(Text.literal("AFK-Farm: " + name + " → " + value).formatted(Formatting.GREEN));
        return 1;
    }
}
