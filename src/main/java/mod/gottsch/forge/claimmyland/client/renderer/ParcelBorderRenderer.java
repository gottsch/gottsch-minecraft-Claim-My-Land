/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
 *
 * Claim My Land is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Claim My Land is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Claim My Land. If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.claimmyland.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side renderer that draws parcel boundaries in world space using
 * {@link RenderLevelStageEvent}. Renders Tier 2 parcels (area exceeds
 * {@code largeParcelsThreshold}) with a full 3D box wireframe, buffer boundary
 * quad strips, and a horizontal area plane. Driven entirely by data in
 * {@link ClientParcelRegistry}; does not reference any in-world block or block entity.
 *
 * <p>Registration: {@code MinecraftForge.EVENT_BUS.register(ParcelBorderRenderer.class)}
 * in {@code ClientSetup.init()} for render events.
 * Sprite resolution is handled by {@link ParcelBorderRendererSetup} on the mod bus.</p>
 *
 * @author Mark Gottschling on March 10, 2026
 */
@OnlyIn(Dist.CLIENT)
public class ParcelBorderRenderer {

    // ---------------------------------------------------------------------------
    // Sprite cache — populated by ParcelBorderRendererSetup.onTextureStitchPost()
    // ---------------------------------------------------------------------------

    static TextureAtlasSprite bufferSprite;
    static TextureAtlasSprite bufferSpriteConflict;
    static final Map<ParcelType, TextureAtlasSprite> AREA_SPRITES = new EnumMap<>(ParcelType.class);

    // ---------------------------------------------------------------------------
    // Event hooks
    // ---------------------------------------------------------------------------

    /**
     * Main render entry point. Fires after translucent blocks are drawn so
     * the wireframe and translucent quads composite correctly over the world.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        render(event.getPoseStack(), event.getCamera());
    }

    // ---------------------------------------------------------------------------
    // Render loop
    // ---------------------------------------------------------------------------
//    private static int debugFrameCount = 0;
    private static void render(PoseStack poseStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Player player = mc.player;
        UUID localPlayerId = player.getUUID();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(2.0f);

        for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
            if (!parcel.isBorderVisible()) continue;
            if (!isWithinRenderRadius(parcel, player)) continue;
            if (!isVisibleToLocalPlayer(parcel, localPlayerId)) continue;

            int color = resolveOwnershipColor(parcel, localPlayerId);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            // --- Wireframe (with preview pulse) ---
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            renderBorderWireframe(poseStack, camera, lineConsumer, parcel, r, g, b);

            // --- buffer brackets ---
            TextureAtlasSprite bufSprite = parcel.isConflict()
                    ? bufferSpriteConflict : bufferSprite;
            if (bufSprite != null) {
                VertexConsumer quadConsumer = bufferSource.getBuffer(
                        RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
//                renderBufferBrackets(poseStack, camera, quadConsumer, parcel, bufSprite, r, g, b);
                renderBufferBrackets(poseStack, camera, quadConsumer, parcel, bufSprite);
            }

            // --- horizontal area plane ---
            TextureAtlasSprite areaSprite = AREA_SPRITES.get(parcel.parcelType());

            if (areaSprite != null) {
                VertexConsumer quadConsumer = bufferSource.getBuffer(
                        RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
//                renderHorizontalPlane(poseStack, camera, quadConsumer, parcel, areaSprite, r, g, b);
                if (parcel.isConflict()) {
                    renderHorizontalPlane(poseStack, camera, quadConsumer, parcel, bufferSpriteConflict, 1f, 1f, 1f);
                } else {
                    renderHorizontalPlane(poseStack, camera, quadConsumer, parcel, areaSprite, r, g, b);
                }
            }
        }

        bufferSource.endBatch(RenderType.lines());
        bufferSource.endBatch(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    }

    // ---------------------------------------------------------------------------
    // Render sub-methods
    // ---------------------------------------------------------------------------

    /**
     * Renders all 12 edges of the parcel bounding box as a wireframe.
     * Nation parcels are capped at {@code nationBorderHeight} blocks tall.
     * Alpha pulses between 25%-75% for preview parcels; otherwise 0.8f.
     */
    private static void renderBorderWireframe(PoseStack poseStack,
                                              Camera camera,
                                              VertexConsumer consumer,
                                              ClientParcel parcel,
                                              float r, float g, float b) {

        if (parcel.conflictState() == 1) {
            r = 1.0f; g = 0.0f; b = 0.0f;
        }

        float a;
        float lineWidth = 2.0f;
        if (parcel.isPreview()) {
//            float pulse = (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.5 + 0.5);
//            a = 0.25f + pulse * 0.50f;

            float pulseT = (float)((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0); // 0.0 → 1.0
            a = 0.05f + 0.75f * pulseT;   // alpha: 0.05 → 0.80
            lineWidth = 0.5f + 1.5f * pulseT;     // strokeWidth: 0.5 → 2.0
        } else {
            a = 0.8f;
        }

//        ClaimMyLand.LOGGER.debug("borderStoneY={} parcelType={}",
//                parcel.borderStoneY(), parcel.parcelType());

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        double x0 = parcel.minX() - camX;
        double z0 = parcel.minZ() - camZ;
        // NOTE need to +1 to the max values to render aournd the outer edge of the block
        double x1 = parcel.maxX() +1 - camX;
        double z1 = parcel.maxZ() +1 - camZ;
        double y0 = parcel.parcelType() == ParcelType.NATION
                ? parcel.borderStoneY() - camY
                : parcel.minY() - camY;
        double y1 = parcel.parcelType() == ParcelType.NATION
                ? parcel.borderStoneY() + Config.SERVER.borders.nationBorderHeight.get() +1 - camY
                : parcel.maxY() +1 - camY;

        Matrix4f matrix = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();

        RenderSystem.lineWidth(lineWidth);

        // Bottom face
        line(consumer, matrix, pose, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(consumer, matrix, pose, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(consumer, matrix, pose, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(consumer, matrix, pose, x0, y0, z1, x0, y0, z0, r, g, b, a);

        // Top face
        line(consumer, matrix, pose, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(consumer, matrix, pose, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(consumer, matrix, pose, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(consumer, matrix, pose, x0, y1, z1, x0, y1, z0, r, g, b, a);

        // Vertical edges
        line(consumer, matrix, pose, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(consumer, matrix, pose, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(consumer, matrix, pose, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(consumer, matrix, pose, x0, y0, z1, x0, y1, z1, r, g, b, a);

        RenderSystem.lineWidth(2.0f);
    }

    private static void renderBufferBrackets(PoseStack poseStack,
                                             Camera camera,
                                             VertexConsumer consumer,
                                             ClientParcel parcel,
                                             TextureAtlasSprite sprite) {
//                                             float r, float g, float b) {
        int buf = parcel.parcelType() == ParcelType.NATION
                ? Config.SERVER.general.nationParcelBufferRadius.get()
                : Config.SERVER.general.parcelBufferRadius.get();

        float a = 0.6f;
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        // buffer boundary in world space (not camera-relative yet)
        int bx0 = parcel.minX() - buf;
        int bx1 = parcel.maxX() + buf;
        int bz0 = parcel.minZ() - buf;
        int bz1 = parcel.maxZ() + buf;

        int yBot = parcel.parcelType() == ParcelType.NATION
                ? parcel.borderStoneY()
                : parcel.minY() - buf;
        int yTop = parcel.parcelType() == ParcelType.NATION
                ? parcel.borderStoneY() + Config.SERVER.borders.nationBorderHeight.get()
                : parcel.maxY() + buf;

        Matrix4f matrix = poseStack.last().pose();

        // NW corner: North face + West face (full height minus top and bottom 1 block)
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bx0 + 1, bz0, yBot + 1, yTop - 1);
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz0, bz0 + 1, yBot + 1, yTop - 1);

        // NE corner: North face + East face
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx1, bx1 + 1, bz0, yBot + 1, yTop - 1);
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx1 + 1, bz0, bz0 + 1, yBot + 1, yTop - 1);

        // SW corner: South face + West face
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bx0 + 1, bz1 + 1, yBot + 1, yTop - 1);
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz1, bz1 + 1, yBot + 1, yTop - 1);

        // SE corner: South face + East face
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx1, bx1 + 1, bz1 + 1, yBot + 1, yTop - 1);
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx1 + 1, bz1, bz1 + 1, yBot + 1, yTop - 1);

        // North top edge: outward vertical (fixed z=bz0-1) + top cap
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bx1 + 1, bz0, yTop - 1, yTop);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz0, bx1 + 1, bz0 + 1, yTop);

        // South top edge: outward vertical (fixed z=bz1) + top cap
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bx1 + 1, bz1 + 1, yTop - 1, yTop);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz1, bx1 + 1, bz1 + 1, yTop);

        // West top edge: outward vertical (fixed x=bx0-1) + top cap
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz0, bz1 + 1, yTop - 1, yTop);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz0, bx0 + 1, bz1 + 1, yTop);

        // East top edge: outward vertical (fixed x=bx1) + top cap
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx1 + 1, bz0, bz1 + 1, yTop - 1, yTop);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx1, bz0, bx1 + 1, bz1 + 1, yTop);

        // North bottom edge: outward vertical (fixed z=bz0-1) + bottom cap
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bx1 + 1, bz0, yBot, yBot + 1);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz0, bx1 + 1, bz0 + 1, yBot);

        // South bottom edge: outward vertical (fixed z=bz1) + bottom cap
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bx1 + 1, bz1 + 1, yBot, yBot + 1);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz1, bx1 + 1, bz1 + 1, yBot);

        // West bottom edge: outward vertical (fixed x=bx0-1) + bottom cap
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz0, bz1 + 1, yBot, yBot + 1);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx0, bz0, bx0 + 1, bz1 + 1, yBot);

        // East bottom edge: outward vertical (fixed x=bx1) + bottom cap
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx1 + 1, bz0, bz1 + 1, yBot, yBot + 1);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, 1f, 1f, 1f, a, bx1, bz0, bx1 + 1, bz1 + 1, yBot);
    }


    // North/South walls — fixed Z, runs along X axis
    private static void tiledVerticalFaceAlongX(VertexConsumer consumer, Matrix4f matrix,
                                                double camX, double camY, double camZ,
                                                TextureAtlasSprite sprite,
                                                float r, float g, float b, float a,
                                                int x0, int x1, int z,
                                                int yBot, int yTop) {
        float su0 = sprite.getU0(), su1 = sprite.getU1();
        float sv0 = sprite.getV0(), sv1 = sprite.getV1();
        float qz = (float) (z - camZ);
        for (int ix = x0; ix < x1; ix++) {
            for (int iy = yBot; iy < yTop; iy++) {
                float qx0 = (float) (ix - camX);
                float qx1 = (float) (ix + 1 - camX);
                float qy0 = (float) (iy - camY);
                float qy1 = (float) (iy + 1 - camY);
                quad(consumer, matrix,
                        qx0, qy0, qz,
                        qx1, qy0, qz,
                        qx1, qy1, qz,
                        qx0, qy1, qz,
                        su0, sv0, su1, sv1, r, g, b, a);
            }
        }
    }

    // West/East walls — fixed X, runs along Z axis
    private static void tiledVerticalFaceAlongZ(VertexConsumer consumer, Matrix4f matrix,
                                                double camX, double camY, double camZ,
                                                TextureAtlasSprite sprite,
                                                float r, float g, float b, float a,
                                                int x, int z0, int z1,
                                                int yBot, int yTop) {
        float su0 = sprite.getU0(), su1 = sprite.getU1();
        float sv0 = sprite.getV0(), sv1 = sprite.getV1();
        float qx = (float) (x - camX);
        for (int iz = z0; iz < z1; iz++) {
            for (int iy = yBot; iy < yTop; iy++) {
                float qz0 = (float) (iz - camZ);
                float qz1 = (float) (iz + 1 - camZ);
                float qy0 = (float) (iy - camY);
                float qy1 = (float) (iy + 1 - camY);
                quad(consumer, matrix,
                        qx, qy0, qz0,
                        qx, qy0, qz1,
                        qx, qy1, qz1,
                        qx, qy1, qz0,
                        su0, sv0, su1, sv1, r, g, b, a);
            }
        }
    }

    private static void tiledHorizontalFace(VertexConsumer consumer, Matrix4f matrix,
                                            double camX, double camY, double camZ,
                                            TextureAtlasSprite sprite,
                                            float r, float g, float b, float a,
                                            int x0, int z0, int x1, int z1,
                                            int y) {
        float su0 = sprite.getU0(), su1 = sprite.getU1();
        float sv0 = sprite.getV0(), sv1 = sprite.getV1();
        float qy = (float) (y - camY);

        for (int ix = x0; ix < x1; ix++) {
            for (int iz = z0; iz < z1; iz++) {
                float qx0 = (float) (ix - camX);
                float qx1 = (float) (ix + 1 - camX);
                float qz0 = (float) (iz - camZ);
                float qz1 = (float) (iz + 1 - camZ);
                quad(consumer, matrix,
                        qx0, qy, qz0,
                        qx1, qy, qz0,
                        qx1, qy, qz1,
                        qx0, qy, qz1,
                        su0, sv0, su1, sv1, r, g, b, a);
            }
        }
    }

    /**
     * Renders a single horizontal quad covering the full XZ parcel footprint at
     * {@code borderStoneY}. Alpha = 0.4f. Ownership color applied as vertex tint.
     */
    private static void renderHorizontalPlane(PoseStack poseStack,
                                              Camera camera,
                                              VertexConsumer consumer,
                                              ClientParcel parcel,
                                              TextureAtlasSprite sprite,
                                              float r, float g, float b) {
        float a = 0.4f;

        double camX = camera.getPosition().x;
        double camZ = camera.getPosition().z;
        float y = (float) (parcel.borderStoneY() + 0.002 - camera.getPosition().y);

        Matrix4f matrix = poseStack.last().pose();

        for (int ix = parcel.minX(); ix <= parcel.maxX(); ix++) {
            for (int iz = parcel.minZ(); iz <= parcel.maxZ(); iz++) {
                float qx0 = (float) (ix - camX);
                float qx1 = (float) (ix + 1 - camX);
                float qz0 = (float) (iz - camZ);
                float qz1 = (float) (iz + 1 - camZ);

                quad(consumer, matrix,
                        qx0, y, qz0,
                        qx1, y, qz0,
                        qx1, y, qz1,
                        qx0, y, qz1,
                        sprite.getU0(), sprite.getV0(),
                        sprite.getU1(), sprite.getV1(),
                        r, g, b, a);
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Primitive helpers
    // ---------------------------------------------------------------------------

    /**
     * Emits a single line segment (two vertices) into a {@link RenderType#lines()} buffer.
     */
    private static void line(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             float r, float g, float b, float a) {
        float dx = (float) (x1 - x0);
        float dy = (float) (y1 - y0);
        float dz = (float) (z1 - z0);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        consumer.vertex(matrix, (float) x0, (float) y0, (float) z0)
                .color(r, g, b, a).normal(pose.normal(), dx / len, dy / len, dz / len).endVertex();
        consumer.vertex(matrix, (float) x1, (float) y1, (float) z1)
                .color(r, g, b, a).normal(pose.normal(), dx / len, dy / len, dz / len).endVertex();
    }

    /**
     * Emits a single quad (four vertices, clockwise winding) into a translucent buffer.
     * Uses {@link OverlayTexture#NO_OVERLAY} and full-brightness lightmap (0xF000F0).
     */
    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float u0, float v0, float u1, float v1,
                             float r, float g, float b, float a) {
        consumer.vertex(matrix, x0, y0, z0).color(r, g, b, a).uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(u1, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(r, g, b, a).uv(u0, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 1, 0).endVertex();
    }

    // ---------------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------------

    /**
     * Returns {@code true} if any part of the parcel's buffer-expanded bounding box
     * falls within {@code borderRenderRadius} blocks of the player.
     */
    private static boolean isWithinRenderRadius(ClientParcel parcel, Player player) {
        int radius = Config.CLIENT.rendering.borderRenderRadius.get();
        int buf = Config.SERVER.general.parcelBufferRadius.get();
        double px = player.getX();
        double pz = player.getZ();
        double bx0 = parcel.minX() - buf;
        double bx1 = parcel.maxX() + buf;
        double bz0 = parcel.minZ() - buf;
        double bz1 = parcel.maxZ() + buf;
        double cx = Math.max(bx0, Math.min(px, bx1));
        double cz = Math.max(bz0, Math.min(pz, bz1));
        double distSq = (px - cx) * (px - cx) + (pz - cz) * (pz - cz);
        return distSq <= (double) radius * radius;
    }

    /**
     * Returns the vivid ownership color if owned by the local player,
     * or a desaturated version if not. Returns an RGB int (no alpha).
     */
    private static int resolveOwnershipColor(ClientParcel parcel, UUID localPlayerId) {
        boolean owned = parcel.ownerId() != null && parcel.ownerId().equals(localPlayerId);
        int vivid = switch (parcel.parcelType()) {
            case NATION -> 0x00AAFF;
            case CITIZEN -> 0xAA55FF;
            case ZONE -> 0xFFFF55;
            default -> 0x55FF55;
        };
        if (owned) return vivid;
        float[] hsb = java.awt.Color.RGBtoHSB(
                (vivid >> 16) & 0xFF, (vivid >> 8) & 0xFF, vivid & 0xFF, null);
        hsb[1] *= 0.30f;
        hsb[2] *= 0.60f;
        return java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0x00FFFFFF;
    }

    public static void resolveSprites(TextureAtlas atlas) {
        bufferSprite = atlas.getSprite(new ResourceLocation("claimmyland", "block/buffer_block"));
        bufferSpriteConflict = atlas.getSprite(new ResourceLocation("claimmyland", "block/buffer_block_conflict"));
        AREA_SPRITES.put(ParcelType.NATION, atlas.getSprite(new ResourceLocation("claimmyland", "block/blue_horizontal")));
        AREA_SPRITES.put(ParcelType.CITIZEN, atlas.getSprite(new ResourceLocation("claimmyland", "block/purple_horizontal")));
        AREA_SPRITES.put(ParcelType.ZONE, atlas.getSprite(new ResourceLocation("claimmyland", "block/yellow_horizontal")));
        AREA_SPRITES.put(ParcelType.PLAYER, atlas.getSprite(new ResourceLocation("claimmyland", "block/green_horizontal")));

    }

    /**
     * returns true if the local player should see this parcel's border.
     * only the owner sees their own borders — whitelisted players and
     * bystanders do not, to avoid visual pollution on busy servers.
     * preview parcels follow the same rule: only the placing player sees theirs.
     * the placing player sees they parcel - this is for claiming a relinquished parcel
     *
     * @author Mark Gottschling on Mar 11, 2026
     */
    private static boolean isVisibleToLocalPlayer(ClientParcel parcel, UUID localPlayerId) {
        return
                (parcel.ownerId() != null && parcel.ownerId().equals(localPlayerId))
                || (parcel.placingPlayer() != null && parcel.placingPlayer().equals(localPlayerId));
    }
}