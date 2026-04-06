package mod.gottsch.forge.claimmyland.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
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
import net.minecraftforge.fml.ModList;
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

    // -------------------------------------------------------------------------
    // Quad-tube render type — lazy initialization
    //
    // RenderType.create() must run on the render thread after the GL context is
    // ready. A static final field initializer fires at class-load time which can
    // precede GL initialization and cause a freeze on world load.
    // Lazy init via a null-check inside tubeRenderType() is the correct pattern.
    // -------------------------------------------------------------------------

    private static RenderType TUBE_RENDER_TYPE = null;

    private static RenderType tubeRenderType() {
        if (TUBE_RENDER_TYPE == null) {
            TUBE_RENDER_TYPE = RenderType.create(
                    "claimmyland_tube",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(
                                    GameRenderer::getPositionColorShader))
                            .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                                    "translucent_transparency",
                                    () -> {
                                        RenderSystem.enableBlend();
                                        RenderSystem.blendFuncSeparate(
                                                GlStateManager.SourceFactor.SRC_ALPHA,
                                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                                GlStateManager.SourceFactor.ONE,
                                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                                    },
                                    () -> {
                                        RenderSystem.disableBlend();
                                        RenderSystem.defaultBlendFunc();
                                    }))
                            .setDepthTestState(new RenderStateShard.DepthTestStateShard("\u2264", 515))
                            .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                            .setCullState(new RenderStateShard.CullStateShard(false))
                            .createCompositeState(false)
            );
        }
        return TUBE_RENDER_TYPE;
    }

    /** Half-width of the tube cross-section in world units for committed parcels. */
    private static final float TUBE_HALF_WIDTH = 0.015f;  // ~0.03 block wide total

    /** Preview pulse: half-width oscillates between these bounds. */
    private static final float TUBE_HALF_WIDTH_MIN = 0.006f;
    private static final float TUBE_HALF_WIDTH_MAX = 0.025f;



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

    private static int conflictClearDistance = CONFLICT_CLEAR_DISTANCE;

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
            conflictOrigin = null;
            conflictSetTime = 0L;
            conflictClearDistance = CONFLICT_CLEAR_DISTANCE;
            return;
        }
        int maxSpan = 0;
        for (ClientParcel parcel : conflicting) {
            CONFLICT_HIGHLIGHT_IDS.add(parcel.parcelId());
            int spanX = parcel.maxX() - parcel.minX();
            int spanZ = parcel.maxZ() - parcel.minZ();
            maxSpan = Math.max(maxSpan, Math.max(spanX, spanZ));
        }
        conflictOrigin = stonePos;
        conflictSetTime = System.currentTimeMillis();
        conflictClearDistance = Math.max(CONFLICT_CLEAR_DISTANCE, maxSpan + CONFLICT_CLEAR_DISTANCE);
    }

    /** Clears all conflict highlights immediately. */
    public static void clearConflictHighlights() {
        CONFLICT_HIGHLIGHT_IDS.clear();
        conflictOrigin = null;
        conflictSetTime = 0L;
        conflictClearDistance = CONFLICT_CLEAR_DISTANCE;
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
                    > (double) conflictClearDistance * conflictClearDistance;
            long timeoutMs = (long) Config.CLIENT.rendering.conflictHighlightTimeoutSeconds.get() * 1000L;
            boolean timedOut = (System.currentTimeMillis() - conflictSetTime) >= timeoutMs;
            if (tooFar || timedOut) {
                if (ModList.get().isLoaded("journeymap")) {
                    ParcelPolygonOverlayFactory.clearConflictOverlays();
                }
                clearConflictHighlights();
            }
        }

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        RenderSystem.enableDepthTest();

        // --- Normal parcel borders ---
        for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
            if (!parcel.isBorderVisible()) continue;
            if (!isWithinRenderRadius(parcel, player)) continue;
            if (!isVisibleToLocalPlayer(parcel, localPlayerId)) continue;

//            // TEMP DEBUG
//            if (!CONFLICT_HIGHLIGHT_IDS.isEmpty()) {
//                ClaimMyLand.LOGGER.info("normal loop: parcel={} estateid={} id={} inConflictSet={}",
//                        parcel.parcelName(), parcel.estateId(), parcel.parcelId(),
//                        CONFLICT_HIGHLIGHT_IDS.contains(parcel.parcelId()));
//            }

            // Skip entirely if conflict highlight will render this parcel in orange
            if (CONFLICT_HIGHLIGHT_IDS.contains(parcel.parcelId())) continue;

            int color = resolveOwnershipColor(parcel, localPlayerId);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8)  & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            VertexConsumer lineConsumer = bufferSource.getBuffer(tubeRenderType());
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
//            ClaimMyLand.LOGGER.debug("render: CONFLICT_HIGHLIGHT_IDS={}", CONFLICT_HIGHLIGHT_IDS);

            for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
                if (!CONFLICT_HIGHLIGHT_IDS.contains(parcel.parcelId())) continue;
                if (!isWithinRenderRadius(parcel, player)) continue;

                VertexConsumer lineConsumer = bufferSource.getBuffer(tubeRenderType());
                renderBorderWireframe(poseStack, camera, lineConsumer, parcel,
                        CONFLICT_R, CONFLICT_G, CONFLICT_B);

                // Use the neutral (white) sprite so the orange vertex color tint
                // shows cleanly — bufferSpriteConflict has a red texture that would
                // override the orange tint.
                if (bufferSprite != null) {
                    VertexConsumer quadConsumer = bufferSource.getBuffer(
                            RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
                    renderBufferBracketsColored(poseStack, camera, quadConsumer, parcel,
                            bufferSprite, CONFLICT_R, CONFLICT_G, CONFLICT_B);
                }
            }
        }

        bufferSource.endBatch(tubeRenderType());
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
        float halfWidth;
        if (parcel.isPreview()) {
            float pulseT = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
            a = 0.05f + 0.75f * pulseT;
            halfWidth = TUBE_HALF_WIDTH_MIN + (TUBE_HALF_WIDTH_MAX - TUBE_HALF_WIDTH_MIN) * pulseT;
        } else {
            a = 0.8f;
            halfWidth = TUBE_HALF_WIDTH;
        }

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        double x0 = parcel.minX() - camX;
        double z0 = parcel.minZ() - camZ;
        double x1 = parcel.maxX() + 1 - camX;
        double z1 = parcel.maxZ() + 1 - camZ;
        double y0 = parcel.parcelType() == ParcelType.NATION
                ? (parcel.borderStoneY() != 0 ? parcel.borderStoneY()
                   : (conflictOrigin != null ? conflictOrigin.getY() : parcel.minY())) - camY
                : parcel.minY() - camY;
        double y1 = parcel.parcelType() == ParcelType.NATION
                ? (parcel.borderStoneY() != 0
                   ? parcel.borderStoneY() + Config.SERVER.borders.nationBorderHeight.get() + 1
                   : (conflictOrigin != null
                      ? conflictOrigin.getY() + Config.SERVER.borders.nationBorderHeight.get() + 1
                      : parcel.maxY() + 1)) - camY
                : parcel.maxY() + 1 - camY;

        Matrix4f matrix = poseStack.last().pose();

        // Bottom face — 4 edges running along X or Z at y0
        renderTubeEdgeX(consumer, matrix, x0, x1, y0, z0, halfWidth, r, g, b, a);
        renderTubeEdgeX(consumer, matrix, x0, x1, y0, z1, halfWidth, r, g, b, a);
        renderTubeEdgeZ(consumer, matrix, z0, z1, y0, x0, halfWidth, r, g, b, a);
        renderTubeEdgeZ(consumer, matrix, z0, z1, y0, x1, halfWidth, r, g, b, a);

        // Top face — 4 edges running along X or Z at y1
        renderTubeEdgeX(consumer, matrix, x0, x1, y1, z0, halfWidth, r, g, b, a);
        renderTubeEdgeX(consumer, matrix, x0, x1, y1, z1, halfWidth, r, g, b, a);
        renderTubeEdgeZ(consumer, matrix, z0, z1, y1, x0, halfWidth, r, g, b, a);
        renderTubeEdgeZ(consumer, matrix, z0, z1, y1, x1, halfWidth, r, g, b, a);

        // Vertical edges — 4 corners running along Y
        renderTubeEdgeY(consumer, matrix, y0, y1, x0, z0, halfWidth, r, g, b, a);
        renderTubeEdgeY(consumer, matrix, y0, y1, x1, z0, halfWidth, r, g, b, a);
        renderTubeEdgeY(consumer, matrix, y0, y1, x1, z1, halfWidth, r, g, b, a);
        renderTubeEdgeY(consumer, matrix, y0, y1, x0, z1, halfWidth, r, g, b, a);
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
                ? (parcel.borderStoneY() != 0 ? parcel.borderStoneY()
                   : (conflictOrigin != null ? conflictOrigin.getY() : parcel.minY() - buf))
                : parcel.minY() - buf;
        int yTop = parcel.parcelType() == ParcelType.NATION
                ? (parcel.borderStoneY() != 0
                   ? parcel.borderStoneY() + Config.SERVER.borders.nationBorderHeight.get()
                   : (conflictOrigin != null
                      ? conflictOrigin.getY() + Config.SERVER.borders.nationBorderHeight.get()
                      : parcel.maxY() + buf))
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
    // Quad-tube edge helpers
    //
    // Each helper renders a single axis-aligned edge as a square-cross-section
    // tube — four QUADS (top, bottom, left, right face). The tube is centered
    // on the edge line with half-width extending perpendicular to the edge axis.
    //
    // Vertices use POSITION_COLOR format (x, y, z, r, g, b, a) — no UV/normal.
    // -------------------------------------------------------------------------

    /**
     * Tube edge running along the X axis.
     *
     * @param x0        start X (camera-relative)
     * @param x1        end X (camera-relative)
     * @param y         fixed Y (camera-relative) — center of the edge
     * @param z         fixed Z (camera-relative) — center of the edge
     * @param hw        half-width of the tube cross-section
     */
    private static void renderTubeEdgeX(VertexConsumer consumer, Matrix4f matrix,
                                        double x0, double x1, double y, double z,
                                        float hw, float r, float g, float b, float a) {
        float fx0 = (float) x0, fx1 = (float) x1;
        float fy  = (float) y,  fz  = (float) z;
        // Top face (y + hw)
        tubeQuad(consumer, matrix, fx0, fy + hw, fz - hw,  fx1, fy + hw, fz - hw,
                fx1, fy + hw, fz + hw,  fx0, fy + hw, fz + hw,  r, g, b, a);
        // Bottom face (y - hw)
        tubeQuad(consumer, matrix, fx0, fy - hw, fz + hw,  fx1, fy - hw, fz + hw,
                fx1, fy - hw, fz - hw,  fx0, fy - hw, fz - hw,  r, g, b, a);
        // Front face (z + hw)
        tubeQuad(consumer, matrix, fx0, fy - hw, fz + hw,  fx1, fy - hw, fz + hw,
                fx1, fy + hw, fz + hw,  fx0, fy + hw, fz + hw,  r, g, b, a);
        // Back face (z - hw)
        tubeQuad(consumer, matrix, fx0, fy + hw, fz - hw,  fx1, fy + hw, fz - hw,
                fx1, fy - hw, fz - hw,  fx0, fy - hw, fz - hw,  r, g, b, a);
    }

    /**
     * Tube edge running along the Z axis.
     *
     * @param z0        start Z (camera-relative)
     * @param z1        end Z (camera-relative)
     * @param y         fixed Y (camera-relative) — center of the edge
     * @param x         fixed X (camera-relative) — center of the edge
     * @param hw        half-width of the tube cross-section
     */
    private static void renderTubeEdgeZ(VertexConsumer consumer, Matrix4f matrix,
                                        double z0, double z1, double y, double x,
                                        float hw, float r, float g, float b, float a) {
        float fz0 = (float) z0, fz1 = (float) z1;
        float fy  = (float) y,  fx  = (float) x;
        // Top face
        tubeQuad(consumer, matrix, fx - hw, fy + hw, fz0,  fx + hw, fy + hw, fz0,
                fx + hw, fy + hw, fz1,  fx - hw, fy + hw, fz1,  r, g, b, a);
        // Bottom face
        tubeQuad(consumer, matrix, fx + hw, fy - hw, fz0,  fx - hw, fy - hw, fz0,
                fx - hw, fy - hw, fz1,  fx + hw, fy - hw, fz1,  r, g, b, a);
        // Right face (x + hw)
        tubeQuad(consumer, matrix, fx + hw, fy - hw, fz0,  fx + hw, fy + hw, fz0,
                fx + hw, fy + hw, fz1,  fx + hw, fy - hw, fz1,  r, g, b, a);
        // Left face (x - hw)
        tubeQuad(consumer, matrix, fx - hw, fy + hw, fz0,  fx - hw, fy - hw, fz0,
                fx - hw, fy - hw, fz1,  fx - hw, fy + hw, fz1,  r, g, b, a);
    }

    /**
     * Tube edge running along the Y axis (vertical corner edges).
     *
     * @param y0        start Y (camera-relative)
     * @param y1        end Y (camera-relative)
     * @param x         fixed X (camera-relative) — center of the edge
     * @param z         fixed Z (camera-relative) — center of the edge
     * @param hw        half-width of the tube cross-section
     */
    private static void renderTubeEdgeY(VertexConsumer consumer, Matrix4f matrix,
                                        double y0, double y1, double x, double z,
                                        float hw, float r, float g, float b, float a) {
        float fy0 = (float) y0, fy1 = (float) y1;
        float fx  = (float) x,  fz  = (float) z;
        // Front face (z + hw)
        tubeQuad(consumer, matrix, fx - hw, fy0, fz + hw,  fx + hw, fy0, fz + hw,
                fx + hw, fy1, fz + hw,  fx - hw, fy1, fz + hw,  r, g, b, a);
        // Back face (z - hw)
        tubeQuad(consumer, matrix, fx + hw, fy0, fz - hw,  fx - hw, fy0, fz - hw,
                fx - hw, fy1, fz - hw,  fx + hw, fy1, fz - hw,  r, g, b, a);
        // Right face (x + hw)
        tubeQuad(consumer, matrix, fx + hw, fy0, fz + hw,  fx + hw, fy0, fz - hw,
                fx + hw, fy1, fz - hw,  fx + hw, fy1, fz + hw,  r, g, b, a);
        // Left face (x - hw)
        tubeQuad(consumer, matrix, fx - hw, fy0, fz - hw,  fx - hw, fy0, fz + hw,
                fx - hw, fy1, fz + hw,  fx - hw, fy1, fz - hw,  r, g, b, a);
    }

    /**
     * Emits a single quad into a POSITION_COLOR vertex consumer.
     * Vertices are provided in counter-clockwise order (front-face by OpenGL convention).
     */
    private static void tubeQuad(VertexConsumer consumer, Matrix4f matrix,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float r, float g, float b, float a) {
        consumer.vertex(matrix, x0, y0, z0).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(r, g, b, a).endVertex();
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