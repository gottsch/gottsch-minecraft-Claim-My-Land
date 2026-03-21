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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.*;

/**
 * Client-side renderer that draws parcel boundaries in world space using
 * {@code RenderLevelStageEvent}.
 *
 * <p>Also renders orange conflict highlights for parcels that overlap a
 * Foundation Stone's proposed bounds. Highlights persist after the preview
 * clears until the player moves 32+ blocks from the stone position OR the
 * configurable timeout elapses, whichever comes first.</p>
 *
 * @author Mark Gottschling on March 20, 2026
 */
@OnlyIn(Dist.CLIENT)
public class ParcelBorderRenderer {

    // --- Conflict highlight constants ---

    /** Distance in blocks beyond which conflict highlights are cleared. */
    private static final int CONFLICT_CLEAR_DISTANCE = 32;

    /** Orange wireframe / buffer color for conflicting parcels. */
    private static final float CONFLICT_R = 1.0f;
    private static final float CONFLICT_G = 0.5f;
    private static final float CONFLICT_B = 0.0f;

    // --- Conflict highlight state ---

    /**
     * Set of parcel UUIDs to render in orange regardless of isBorderVisible.
     * Populated by FoundationStoneEvents on each right-click of a Foundation Stone.
     * Cleared when the distance or time condition is met.
     */
    private static final Set<UUID> CONFLICT_HIGHLIGHT_IDS = new HashSet<>();

    /**
     * World position of the Foundation Stone that triggered the current conflict
     * highlights. Used for distance-based clearing.
     */
    private static BlockPos conflictOrigin = null;

    /**
     * System time (ms) when conflict highlights were last set.
     * Used for time-based clearing.
     */
    private static long conflictSetTime = 0L;

    // --- Sprites (resolved by ParcelBorderRendererSetup on TextureStitchEvent.Post) ---

    static TextureAtlasSprite bufferSprite;
    static TextureAtlasSprite bufferSpriteConflict;
    static final Map<ParcelType, TextureAtlasSprite> AREA_SPRITES = new EnumMap<>(ParcelType.class);

    // -------------------------------------------------------------------------
    // Public API — called by FoundationStoneEvents
    // -------------------------------------------------------------------------

    /**
     * Sets the conflict highlight set to the given parcels and records the
     * origin position and current time for the clear-condition check.
     *
     * Passing an empty list clears all highlights immediately.
     *
     * @param conflicting list of committed parcels that conflict with the preview
     * @param stonePos    block position of the Foundation Stone (for distance check)
     */
    public static void setConflictHighlights(List<ClientParcel> conflicting, BlockPos stonePos) {
        CONFLICT_HIGHLIGHT_IDS.clear();
        if (conflicting.isEmpty()) {
            conflictOrigin  = null;
            conflictSetTime = 0L;
            return;
        }
        for (ClientParcel parcel : conflicting) {
            CONFLICT_HIGHLIGHT_IDS.add(parcel.parcelId());
        }
        conflictOrigin  = stonePos;
        conflictSetTime = System.currentTimeMillis();
    }

    /** Clears all conflict highlights immediately. */
    public static void clearConflictHighlights() {
        CONFLICT_HIGHLIGHT_IDS.clear();
        conflictOrigin  = null;
        conflictSetTime = 0L;
    }

    // -------------------------------------------------------------------------
    // Render loop
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        render(event.getPoseStack(), event.getCamera());
    }

    private static void render(PoseStack poseStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Player player = mc.player;
        UUID localPlayerId = player.getUUID();

        // --- Conflict highlight clear check ---
        if (!CONFLICT_HIGHLIGHT_IDS.isEmpty() && conflictOrigin != null) {
            boolean tooFar = player.blockPosition().distSqr(conflictOrigin)
                    > (double) CONFLICT_CLEAR_DISTANCE * CONFLICT_CLEAR_DISTANCE;
            long timeoutMs = (long) Config.CLIENT.rendering.conflictHighlightTimeoutSeconds.get() * 1000L;
            boolean timedOut = (System.currentTimeMillis() - conflictSetTime) >= timeoutMs;
            if (tooFar || timedOut) {
                clearConflictHighlights();
            }
        }

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(2.0f);

        // --- Normal parcel borders ---
        for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
            if (!parcel.isBorderVisible()) continue;
            if (!isWithinRenderRadius(parcel, player)) continue;
            if (!isVisibleToLocalPlayer(parcel, localPlayerId)) continue;

            int color = resolveOwnershipColor(parcel, localPlayerId);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8)  & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            renderBorderWireframe(poseStack, camera, lineConsumer, parcel, r, g, b);

            TextureAtlasSprite bufSprite = parcel.isConflict() ? bufferSpriteConflict : bufferSprite;
            if (bufSprite != null) {
                VertexConsumer quadConsumer = bufferSource.getBuffer(
                        RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
                renderBufferBrackets(poseStack, camera, quadConsumer, parcel, bufSprite);
            }

            TextureAtlasSprite areaSprite = AREA_SPRITES.get(parcel.parcelType());
            if (areaSprite != null) {
                VertexConsumer quadConsumer = bufferSource.getBuffer(
                        RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
                if (parcel.isConflict()) {
                    renderHorizontalPlane(poseStack, camera, quadConsumer, parcel, bufferSpriteConflict, 1f, 1f, 1f);
                } else {
                    renderHorizontalPlane(poseStack, camera, quadConsumer, parcel, areaSprite, r, g, b);
                }
            }
        }

        // --- Conflict highlights (orange) ---
        // Rendered for any parcel in CONFLICT_HIGHLIGHT_IDS regardless of isBorderVisible,
        // so parcels the local player doesn't own still show the orange highlight.
        if (!CONFLICT_HIGHLIGHT_IDS.isEmpty()) {
            for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
                if (!CONFLICT_HIGHLIGHT_IDS.contains(parcel.parcelId())) continue;
                if (!isWithinRenderRadius(parcel, player)) continue;

                VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
                renderBorderWireframe(poseStack, camera, lineConsumer, parcel,
                        CONFLICT_R, CONFLICT_G, CONFLICT_B);

                if (bufferSpriteConflict != null) {
                    VertexConsumer quadConsumer = bufferSource.getBuffer(
                            RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
                    renderBufferBracketsColored(poseStack, camera, quadConsumer, parcel,
                            bufferSpriteConflict, CONFLICT_R, CONFLICT_G, CONFLICT_B);
                }
            }
        }

        bufferSource.endBatch(RenderType.lines());
        bufferSource.endBatch(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    }

    // -------------------------------------------------------------------------
    // Wireframe
    // -------------------------------------------------------------------------

    private static void renderBorderWireframe(PoseStack poseStack,
                                              Camera camera,
                                              VertexConsumer consumer,
                                              ClientParcel parcel,
                                              float r, float g, float b) {
        // Conflict state on the parcel's own border overrides ownership color with red.
        // (Orange conflict highlights from FoundationStoneEvents pass their own r/g/b.)
        if (parcel.conflictState() == 1) {
            r = 1.0f; g = 0.0f; b = 0.0f;
        }

        float a;
        float lineWidth = 2.0f;
        if (parcel.isPreview()) {
            float pulseT = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
            a = 0.05f + 0.75f * pulseT;
            lineWidth = 0.5f + 1.5f * pulseT;
        } else {
            a = 0.8f;
        }

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        double x0 = parcel.minX() - camX;
        double z0 = parcel.minZ() - camZ;
        double x1 = parcel.maxX() + 1 - camX;
        double z1 = parcel.maxZ() + 1 - camZ;
        double y0 = parcel.parcelType() == ParcelType.NATION
                ? parcel.borderStoneY() - camY
                : parcel.minY() - camY;
        double y1 = parcel.parcelType() == ParcelType.NATION
                ? parcel.borderStoneY() + Config.SERVER.borders.nationBorderHeight.get() + 1 - camY
                : parcel.maxY() + 1 - camY;

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
        // Verticals
        line(consumer, matrix, pose, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(consumer, matrix, pose, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(consumer, matrix, pose, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(consumer, matrix, pose, x0, y0, z1, x0, y1, z1, r, g, b, a);

        RenderSystem.lineWidth(2.0f);
    }

    // -------------------------------------------------------------------------
    // Buffer brackets — white (normal) and colored (conflict highlight)
    // -------------------------------------------------------------------------

    private static void renderBufferBrackets(PoseStack poseStack,
                                             Camera camera,
                                             VertexConsumer consumer,
                                             ClientParcel parcel,
                                             TextureAtlasSprite sprite) {
        renderBufferBracketsColored(poseStack, camera, consumer, parcel, sprite, 1f, 1f, 1f);
    }

    /**
     * Renders buffer brackets with an explicit color tint.
     * Normal parcels use white (1,1,1). Conflict highlights pass orange.
     */
    private static void renderBufferBracketsColored(PoseStack poseStack,
                                                    Camera camera,
                                                    VertexConsumer consumer,
                                                    ClientParcel parcel,
                                                    TextureAtlasSprite sprite,
                                                    float r, float g, float b) {
        int buf = parcel.parcelType() == ParcelType.NATION
                ? Config.SERVER.general.nationParcelBufferRadius.get()
                : Config.SERVER.general.parcelBufferRadius.get();

        float a = 0.6f;
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

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

        // NW corner
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bx0 + 1, bz0, yBot + 1, yTop - 1);
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz0, bz0 + 1, yBot + 1, yTop - 1);
        // NE corner
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx1, bx1 + 1, bz0, yBot + 1, yTop - 1);
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx1 + 1, bz0, bz0 + 1, yBot + 1, yTop - 1);
        // SW corner
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bx0 + 1, bz1 + 1, yBot + 1, yTop - 1);
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz1, bz1 + 1, yBot + 1, yTop - 1);
        // SE corner
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx1, bx1 + 1, bz1 + 1, yBot + 1, yTop - 1);
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx1 + 1, bz1, bz1 + 1, yBot + 1, yTop - 1);
        // North top
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bx1 + 1, bz0, yTop - 1, yTop);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz0, bx1 + 1, bz0 + 1, yTop);
        // South top
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bx1 + 1, bz1 + 1, yTop - 1, yTop);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz1, bx1 + 1, bz1 + 1, yTop);
        // West top
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz0, bz1 + 1, yTop - 1, yTop);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz0, bx0 + 1, bz1 + 1, yTop);
        // East top
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx1 + 1, bz0, bz1 + 1, yTop - 1, yTop);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx1, bz0, bx1 + 1, bz1 + 1, yTop);
        // North bottom
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bx1 + 1, bz0, yBot, yBot + 1);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz0, bx1 + 1, bz0 + 1, yBot);
        // South bottom
        tiledVerticalFaceAlongX(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bx1 + 1, bz1 + 1, yBot, yBot + 1);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz1, bx1 + 1, bz1 + 1, yBot);
        // West bottom
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz0, bz1 + 1, yBot, yBot + 1);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx0, bz0, bx0 + 1, bz1 + 1, yBot);
        // East bottom
        tiledVerticalFaceAlongZ(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx1 + 1, bz0, bz1 + 1, yBot, yBot + 1);
        tiledHorizontalFace(consumer, matrix, camX, camY, camZ, sprite, r, g, b, a, bx1, bz0, bx1 + 1, bz1 + 1, yBot);
    }

    // -------------------------------------------------------------------------
    // Tiled face helpers
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Horizontal area plane
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Primitive helpers
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Culling helpers
    // -------------------------------------------------------------------------

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

    private static boolean isVisibleToLocalPlayer(ClientParcel parcel, UUID localPlayerId) {
        return (parcel.ownerId() != null && parcel.ownerId().equals(localPlayerId))
                || (parcel.placingPlayer() != null && parcel.placingPlayer().equals(localPlayerId));
    }

    private static int resolveOwnershipColor(ClientParcel parcel, UUID localPlayerId) {
        boolean owned = parcel.ownerId() != null && parcel.ownerId().equals(localPlayerId);
        int vivid = switch (parcel.parcelType()) {
            case NATION  -> 0x00AAFF;
            case CITIZEN -> 0xAA55FF;
            case ZONE    -> 0xFFFF55;
            default      -> 0x55FF55;
        };
        if (owned) return vivid;
        float[] hsb = java.awt.Color.RGBtoHSB(
                (vivid >> 16) & 0xFF, (vivid >> 8) & 0xFF, vivid & 0xFF, null);
        hsb[1] *= 0.30f;
        hsb[2] *= 0.60f;
        return java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0x00FFFFFF;
    }

    public static void resolveSprites(TextureAtlas atlas) {
        bufferSprite         = atlas.getSprite(new ResourceLocation("claimmyland", "block/buffer_block"));
        bufferSpriteConflict = atlas.getSprite(new ResourceLocation("claimmyland", "block/buffer_block_conflict"));
        AREA_SPRITES.put(ParcelType.NATION,  atlas.getSprite(new ResourceLocation("claimmyland", "block/blue_horizontal")));
        AREA_SPRITES.put(ParcelType.CITIZEN, atlas.getSprite(new ResourceLocation("claimmyland", "block/purple_horizontal")));
        AREA_SPRITES.put(ParcelType.ZONE,    atlas.getSprite(new ResourceLocation("claimmyland", "block/yellow_horizontal")));
        AREA_SPRITES.put(ParcelType.PLAYER,  atlas.getSprite(new ResourceLocation("claimmyland", "block/green_horizontal")));
    }
}