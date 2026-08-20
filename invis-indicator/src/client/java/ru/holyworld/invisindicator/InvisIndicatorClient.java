package ru.holyworld.invisindicator;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvisIndicatorClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("invisindicator");

    @Override
    public void onInitializeClient() {
        RingRenderer.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("invisindicator")
                        .then(ClientCommandManager.literal("color")
                                .then(ClientCommandManager.argument("hex", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String hex = StringArgumentType.getString(ctx, "hex").replace("#", "");
                                            try {
                                                Config cfg = Config.get();
                                                cfg.colorRgb = Integer.parseInt(hex, 16) & 0xFFFFFF;
                                                cfg.save();
                                                ctx.getSource().sendFeedback(Component.literal("Цвет кольца обновлён: #" + hex));
                                            } catch (NumberFormatException e) {
                                                ctx.getSource().sendError(Component.literal("Формат: /invisindicator color RRGGBB"));
                                            }
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("radius")
                                .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 3f))
                                        .executes(ctx -> {
                                            Config cfg = Config.get();
                                            cfg.radius = FloatArgumentType.getFloat(ctx, "value");
                                            cfg.save();
                                            ctx.getSource().sendFeedback(Component.literal("Радиус кольца: " + cfg.radius));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("alpha")
                                .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.05f, 1f))
                                        .executes(ctx -> {
                                            Config cfg = Config.get();
                                            cfg.alpha = FloatArgumentType.getFloat(ctx, "value");
                                            cfg.save();
                                            ctx.getSource().sendFeedback(Component.literal("Прозрачность кольца: " + cfg.alpha));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("nickname")
                                .then(ClientCommandManager.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            Config cfg = Config.get();
                                            cfg.showNickname = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value");
                                            cfg.save();
                                            ctx.getSource().sendFeedback(Component.literal("Показ ника: " + cfg.showNickname));
                                            return 1;
                                        })))
        ));
    }
}
