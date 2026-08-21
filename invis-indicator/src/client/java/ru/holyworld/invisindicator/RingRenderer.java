package ru.holyworld.invisindicator;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public final class RingRenderer {

    private RingRenderer() {}

    private static final int SPAWN_INTERVAL_TICKS = 4;
    private static final int SEGMENTS = 20;

    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(RingRenderer::onEndClientTick);
    }

    private static void onEndClientTick(Minecraft client) {
        ClientLevel level = client.level;
        LocalPlayer self = client.player;
        if (level == null || self == null) {
            return;
        }

        tickCounter++;
        if (tickCounter < SPAWN_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        Config cfg = Config.get();

        for (Player player : level.players()) {
            if (!cfg.includeSelf && player == self) {
                continue;
            }
            if (!player.isInvisible()) {
                continue;
            }

            spawnRing(level, player, cfg);
        }
    }

    private static void spawnRing(ClientLevel level, Player player, Config cfg) {
        float r = ((cfg.colorRgb >> 16) & 0xFF) / 255f;
        float g = ((cfg.colorRgb >> 8) & 0xFF) / 255f;
        float b = (cfg.colorRgb & 0xFF) / 255f;

        DustParticleOptions dust = new DustParticleOptions(new Vector3f(r, g, b), 1.3f);

        double centerX = player.getX();
        double centerY = player.getY() + player.getBbHeight() + cfg.heightOffset;
        double centerZ = player.getZ();

        for (int i = 0; i < SEGMENTS; i++) {
            double angle = (Math.PI * 2 * i) / SEGMENTS;
            double x = centerX + Math.cos(angle) * cfg.radius;
            double z = centerZ + Math.sin(angle) * cfg.radius;
            level.addParticle(dust, x, centerY, z, 0.0, 0.0, 0.0);
        }
    }
}
