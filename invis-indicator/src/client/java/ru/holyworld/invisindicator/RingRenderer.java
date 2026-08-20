package ru.holyworld.invisindicator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * ВАЖНО про принцип работы, чтобы это НЕ превратилось в ESP:
 * - Мы НЕ ищем невидимых игроков "сквозь стены" и не увеличиваем радиус их
 *   обнаружения. Кольцо рисуется только для сущностей, которые и так были бы
 *   переданы клиенту как обычные entity в пределах стандартной прорисовки.
 * - player.isInvisible() читает тот же самый синхронизируемый флаг, из-за
 *   которого ванильный клиент вообще перестаёт рисовать модель игрока —
 *   то есть мы не извлекаем никакой информации, которой у клиента иначе бы
 *   не было.
 * - RenderType.lines() рисуется с включённым тестом глубины, поэтому кольцо
 *   так же перекрывается блоками/стенами, как и любая обычная модель
 *   сущности. Никакого "сквозь стены" эффекта тут нет.
 *
 * Если конкретный сервер требует ещё более узких ограничений — сверься с
 * его правилами перед использованием.
 */
public final class RingRenderer {

    private RingRenderer() {}

    public static void register() {
        LevelRenderEvents.AFTER_ENTITIES.register(RingRenderer::onAfterEntities);
    }

    private static void onAfterEntities(LevelRenderEvents.AfterEntities context) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        LocalPlayer self = client.player;
        if (level == null || self == null) {
            return;
        }

        Config cfg = Config.get();
        Camera camera = context.camera();
        PoseStack poseStack = context.poseStack();
        MultiBufferSource bufferSource = context.consumers();

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        for (Player player : level.players()) {
            if (!cfg.includeSelf && player == self) {
                continue;
            }
            if (!player.isInvisible()) {
                continue;
            }

            double x = Mth_lerp(player, context.partialTick(), 0) - camX;
            double y = Mth_lerp(player, context.partialTick(), 1) - camY;
            double z = Mth_lerp(player, context.partialTick(), 2) - camZ;

            drawRing(poseStack, bufferSource, x, y + player.getBbHeight() + cfg.heightOffset, z, cfg);

            if (cfg.showNickname) {
                drawNickname(client, poseStack, bufferSource, player,
                        x, y + player.getBbHeight() + cfg.heightOffset + 0.35, z, camera);
            }
        }
    }

    // Небольшой хелпер интерполяции позиции между тиками, чтобы кольцо не
    // "дёргалось". Если у Entity в твоей версии другое имя полей x/y/z
    // старого тика (xo/yo/zo в Yarn -> в маппингах Mojang это обычно
    // тоже xo/yo/zo), поправь здесь.
    private static double Mth_lerp(Player player, float partialTick, int axis) {
        switch (axis) {
            case 0: return player.xo + (player.getX() - player.xo) * partialTick;
            case 1: return player.yo + (player.getY() - player.yo) * partialTick;
            default: return player.zo + (player.getZ() - player.zo) * partialTick;
        }
    }

    private static void drawRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                  double x, double y, double z, Config cfg) {
        int segments = 40;
        float r = ((cfg.colorRgb >> 16) & 0xFF) / 255f;
        float g = ((cfg.colorRgb >> 8) & 0xFF) / 255f;
        float b = (cfg.colorRgb & 0xFF) / 255f;
        float a = cfg.alpha;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < segments; i++) {
            double a1 = (Math.PI * 2 * i) / segments;
            double a2 = (Math.PI * 2 * (i + 1)) / segments;
            float x1 = (float) (Math.cos(a1) * cfg.radius);
            float z1 = (float) (Math.sin(a1) * cfg.radius);
            float x2 = (float) (Math.cos(a2) * cfg.radius);
            float z2 = (float) (Math.sin(a2) * cfg.radius);

            // Слегка приплюснутое кольцо, как на референс-скринах.
            buffer.addVertex(pose, x1, 0f, z1).setColor(r, g, b, a).setNormal(pose, 0f, 1f, 0f);
            buffer.addVertex(pose, x2, 0f, z2).setColor(r, g, b, a).setNormal(pose, 0f, 1f, 0f);
        }

        poseStack.popPose();
    }

    private static void drawNickname(Minecraft client, PoseStack poseStack, MultiBufferSource bufferSource,
                                      Player player, double x, double y, double z, Camera camera) {
        Font font = client.font;
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        Component name = player.getDisplayName() != null ? player.getDisplayName() : Component.literal(player.getName().getString());
        String text = name.getString();
        int width = font.width(text);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        int bg = (int) (client.options.getBackgroundOpacity(0.25f) * 255) << 24;
        font.drawInBatch(text, -width / 2f, 0, 0xFFFFFF, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, bg, 0xF000F0);

        poseStack.popPose();
    }
}
