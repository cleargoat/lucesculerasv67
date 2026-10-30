package com.lightutils.client;

import com.lightutils.ClientLights;
import com.lightutils.LightData;
import com.lightutils.LightUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Luz de color ligera (estilo "deferred"): por cada luz cercana se hace un pase que lee la
 * profundidad de la pantalla, calcula la posicion en el mundo y multiplica/suma el color del pixel.
 * Sin normales, sin especular y sin sombras => no hay reflejos ni sombras en jugador/suelo.
 */
@Mod.EventBusSubscriber(modid = LightUtils.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ColorLightRenderer {

    private static final int MAX_LIGHTS = 6;           // luces mas cercanas por frame
    private static final double MAX_DIST = 160.0;
    private static TextureTarget depthCopy;

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        ShaderInstance sh = ClientSetup.coloredLight;
        if (mc.level == null || sh == null) return;

        List<LightData> all = ClientLights.lights;
        if (all.isEmpty()) return;

        String dim = mc.level.dimension().location().toString();
        Vec3 cp = e.getCamera().getPosition();

        List<LightData> near = new ArrayList<>();
        for (LightData l : all) {
            if (!l.dim().equals(dim)) continue;
            double dx = l.pos().getX() + 0.5 - cp.x, dy = l.pos().getY() + 0.5 - cp.y, dz = l.pos().getZ() + 0.5 - cp.z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d - l.radius() > MAX_DIST) continue;
            near.add(l);
        }
        if (near.isEmpty()) return;
        near.sort(Comparator.comparingDouble(l -> l.pos().distToCenterSqr(cp)));
        if (near.size() > MAX_LIGHTS) near = near.subList(0, MAX_LIGHTS);

        // copia de la profundidad (para poder leerla mientras se dibuja encima)
        RenderTarget main = mc.getMainRenderTarget();
        if (depthCopy == null || depthCopy.width != main.width || depthCopy.height != main.height) {
            if (depthCopy != null) depthCopy.destroyBuffers();
            depthCopy = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        }
        depthCopy.copyDepthFrom(main);
        main.bindWrite(false);

        Matrix4f invProj = new Matrix4f(e.getProjectionMatrix()).invert();
        Matrix4f invView = new Matrix4f(e.getPoseStack().last().pose()).invert();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> sh);
        sh.setSampler("DepthSampler", depthCopy.getDepthTextureId());
        sh.getUniform("InvProj").set(invProj);
        sh.getUniform("InvView").set(invView);

        for (LightData l : near) {
            float strength = 0.25f + 1.1f * (l.intensity() / 15f);
            float r = ((l.color() >> 16) & 255) / 255f * strength;
            float g = ((l.color() >> 8) & 255) / 255f * strength;
            float b = (l.color() & 255) / 255f * strength;

            sh.getUniform("LightPos").set((float) (l.pos().getX() + 0.5 - cp.x),
                    (float) (l.pos().getY() + 0.5 - cp.y), (float) (l.pos().getZ() + 0.5 - cp.z));
            sh.getUniform("LightColor").set(r, g, b, (float) l.radius());

            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bb.vertex(-1, -1, 0).endVertex();
            bb.vertex(1, -1, 0).endVertex();
            bb.vertex(1, 1, 0).endVertex();
            bb.vertex(-1, 1, 0).endVertex();
            BufferUploader.drawWithShader(bb.end());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
