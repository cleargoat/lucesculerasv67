package com.lightutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class LightCommand {

    private static final Map<String, Integer> COLORS = new LinkedHashMap<>();
    static {
        COLORS.put("rojo", 0xFF0000);     COLORS.put("red", 0xFF0000);
        COLORS.put("verde", 0x00FF00);    COLORS.put("green", 0x00FF00);
        COLORS.put("azul", 0x0055FF);     COLORS.put("blue", 0x0055FF);
        COLORS.put("amarillo", 0xFFFF00); COLORS.put("yellow", 0xFFFF00);
        COLORS.put("naranja", 0xFF8800);  COLORS.put("orange", 0xFF8800);
        COLORS.put("morado", 0xAA00FF);   COLORS.put("purple", 0xAA00FF);
        COLORS.put("rosa", 0xFF55AA);     COLORS.put("pink", 0xFF55AA);
        COLORS.put("cian", 0x00FFFF);     COLORS.put("cyan", 0x00FFFF);
        COLORS.put("blanco", 0xFFFFFF);   COLORS.put("white", 0xFFFFFF);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("lightutils")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(COLORS.keySet(), b))
                                .then(Commands.argument("intensidad", IntegerArgumentType.integer(1, 15))
                                        .then(Commands.argument("radio", IntegerArgumentType.integer(1, 64))
                                                .then(Commands.argument("tag", StringArgumentType.word())
                                                        .executes(LightCommand::add))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("tag", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(tags(c.getSource()), b))
                                .executes(LightCommand::clear))));
    }

    private static int parseColor(String s) {
        String key = s.toLowerCase(Locale.ROOT);
        if (COLORS.containsKey(key)) return COLORS.get(key);
        if (key.startsWith("#")) key = key.substring(1);
        if (key.length() != 6) return -1;
        try { return Integer.parseInt(key, 16); } catch (NumberFormatException e) { return -1; }
    }

    private static int add(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        int color = parseColor(StringArgumentType.getString(ctx, "color"));
        int intensity = IntegerArgumentType.getInteger(ctx, "intensidad");
        int radius = IntegerArgumentType.getInteger(ctx, "radio");
        String tag = StringArgumentType.getString(ctx, "tag");

        if (color < 0) {
            src.sendFailure(Component.literal("Color invalido. Usa un nombre (rojo, azul...) o hex de 6 digitos (ej: ff8800)."));
            return 0;
        }
        BlockPos pos = BlockPos.containing(src.getPosition());
        String dim = src.getLevel().dimension().location().toString();

        LightSavedData data = LightSavedData.get(src.getServer());
        data.lights.add(new LightData(tag, color, intensity, radius, dim, pos));
        data.setDirty();
        LightNet.syncAll(src.getServer());

        src.sendSuccess(() -> Component.literal("Luz '" + tag + "' creada en " + pos.toShortString()), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String tag = StringArgumentType.getString(ctx, "tag");
        LightSavedData data = LightSavedData.get(src.getServer());

        int before = data.lights.size();
        data.lights.removeIf(l -> l.tag().equals(tag));
        int removed = before - data.lights.size();

        if (removed == 0) {
            src.sendFailure(Component.literal("No se encontro ninguna luz con el tag '" + tag + "'."));
            return 0;
        }
        data.setDirty();
        LightNet.syncAll(src.getServer());
        src.sendSuccess(() -> Component.literal("Se eliminaron " + removed + " luz/luces con el tag '" + tag + "'."), true);
        return removed;
    }

    private static Set<String> tags(CommandSourceStack src) {
        Set<String> t = new TreeSet<>();
        for (LightData l : LightSavedData.get(src.getServer()).lights) t.add(l.tag());
        return t;
    }
}
