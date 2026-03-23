package mod.gottsch.forge.claimmyland.core.integration.journeymap;

import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.model.TextProperties;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.config.ClientServerConfig;
import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.DimensionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Color;
import java.util.*;

/**
 * Builds and manages JourneyMap polygon overlays for claimed parcels.
 *
 * <p>Each committed parcel produces two overlays: a full-screen overlay with
 * estate/owner label, and a minimap overlay with the estate name only.
 * Preview overlays (Foundation Stone placements) are stored separately and
 * never mixed with committed parcel overlays.</p>
 *
 * <p>Conflict state: when a parcel's {@code conflictState > 0} its overlay
 * renders in red regardless of ownership, matching the in-world
 * {@code ParcelBorderRenderer} behaviour.</p>
 *
 * @author Mark Gottschling on March 19, 2026
 */
public class ParcelPolygonOverlayFactory {

    private static final Logger LOGGER = LogManager.getLogger(ClaimMyLand.MOD_ID);

    // -------------------------------------------------------------------------
    // Committed parcel colors — ownership
    // -------------------------------------------------------------------------

    private static final Color NATION_FILL    = new Color(0x0300AAFF, true);
    private static final Color NATION_STROKE  = new Color(0xCC00AAFF, true);
    private static final Color CITIZEN_FILL   = new Color(0x33AA55FF, true);
    private static final Color CITIZEN_STROKE = new Color(0xCCAA55FF, true);
    private static final Color ZONE_FILL      = new Color(0x03FFFF55, true);
    private static final Color ZONE_STROKE    = new Color(0xCCFFFF55, true);
    private static final Color PLAYER_FILL    = new Color(0x3355FF55, true);
    private static final Color PLAYER_STROKE  = new Color(0xCC55FF55, true);

    // -------------------------------------------------------------------------
    // Committed parcel colors — conflict (conflictState > 0)
    // Matches the red wireframe used by ParcelBorderRenderer in-world.
    // -------------------------------------------------------------------------

    private static final Color CONFLICT_FILL   = new Color(0x33FF0000, true);  // red, 20% alpha
    private static final Color CONFLICT_STROKE = new Color(0xCCFF0000, true);  // red, 80% alpha

    // -------------------------------------------------------------------------
    // Preview overlay colors (Foundation Stone — not yet committed)
    // -------------------------------------------------------------------------

    private static final Color PREVIEW_CLEAR_FILL      = new Color(0x3300FF00, true);
    private static final Color PREVIEW_CLEAR_STROKE    = new Color(0xCC00FF00, true);
    private static final Color PREVIEW_CONFLICT_FILL   = new Color(0x33FF0000, true);
    private static final Color PREVIEW_CONFLICT_STROKE = new Color(0xCCFF0000, true);

    // -------------------------------------------------------------------------
    // JM conflict highlight colors (orange — shown on parcels conflicting with a preview)
    // -------------------------------------------------------------------------

    private static final Color CONFLICT_TARGET_FILL          = new Color(0x33FF8000, true); // orange, 20% alpha
    private static final Color CONFLICT_TARGET_STROKE        = new Color(0xCCFF8000, true); // orange, 80% alpha
    private static final Color CONFLICT_TARGET_BUFFER_STROKE = new Color(0x80FF8000, true); // orange, 50% alpha

    // -------------------------------------------------------------------------
    // Buffer zone overlay colors
    // -------------------------------------------------------------------------

    /** No fill — buffer overlay is stroke-only to avoid visual clutter. */
    private static final Color BUFFER_STROKE         = new Color(0x80FFFFFF, true);  // white, 50% alpha
    private static final Color BUFFER_CONFLICT_STROKE = new Color(0x80FF0000, true); // red,   50% alpha

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Committed parcel overlays keyed by parcel UUID. Each entry holds [fullOverlay, miniOverlay]. */
    private static final Map<UUID, PolygonOverlay[]> ACTIVE_OVERLAYS = new HashMap<>();

    /** Buffer zone overlays keyed by parcel UUID. One entry per parcel (no minimap variant needed). */
    private static final Map<UUID, PolygonOverlay> BUFFER_OVERLAYS = new HashMap<>();

    /** Orange conflict highlight overlays — shown on parcels that conflict with the current preview. */
    private static final Map<UUID, PolygonOverlay> CONFLICT_OVERLAYS = new HashMap<>();
    private static final Map<UUID, PolygonOverlay> CONFLICT_BUFFER_OVERLAYS = new HashMap<>();

    /** Current Foundation Stone preview overlay — stored separately from ACTIVE_OVERLAYS. */
    private static PolygonOverlay previewOverlay = null;
    private static PolygonOverlay previewBufferOverlay = null;
    private static UUID previewParcelId = null;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Rebuilds all committed parcel overlays from the current ClientParcelRegistry.
     * Clears ACTIVE_OVERLAYS first. Called by JourneyMapOverlayHandler on
     * MAPPING_STARTED and DISPLAY_UPDATE events.
     */
    public static List<PolygonOverlay> buildAll() {
        ACTIVE_OVERLAYS.clear();
        BUFFER_OVERLAYS.clear();
        clearConflictOverlays();
        List<PolygonOverlay> result = new ArrayList<>();
        UUID localPlayerId = localPlayerId();

        for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
            PolygonOverlay[] overlays = buildOverlay(parcel, localPlayerId);
            if (overlays == null) continue;
            ACTIVE_OVERLAYS.put(parcel.parcelId(), overlays);
            result.add(overlays[0]);
            result.add(overlays[1]);

            PolygonOverlay bufferOverlay = buildBufferOverlay(parcel);
            if (bufferOverlay != null) {
                BUFFER_OVERLAYS.put(parcel.parcelId(), bufferOverlay);
                result.add(bufferOverlay);
            }
        }
        return result;
    }

    /**
     * Builds the full-screen and minimap overlays for a single committed parcel.
     *
     * <p>Color priority:
     * <ol>
     *   <li>If {@code conflictState > 0} — red conflict colors, regardless of ownership.</li>
     *   <li>If owned by the local player — vivid ownership colors.</li>
     *   <li>Otherwise — muted/desaturated ownership colors.</li>
     * </ol>
     * </p>
     *
     * Returns {@code null} if the parcel's dimension cannot be resolved.
     */
    public static PolygonOverlay[] buildOverlay(ClientParcel parcel, UUID localPlayerId) {
        ResourceKey<Level> dimKey = dimensionKey(parcel.dimension());
        if (dimKey == null) {
            LOGGER.warn("[{}] Cannot resolve dimension '{}' for parcel '{}' — skipping overlay.",
                    ClaimMyLand.MOD_ID, parcel.dimension(), parcel.parcelId());
            return null;
        }

        int y = parcel.minY();
        List<BlockPos> corners = List.of(
                new BlockPos(parcel.minX(),     y, parcel.minZ()),
                new BlockPos(parcel.maxX() + 1, y, parcel.minZ()),
                new BlockPos(parcel.maxX() + 1, y, parcel.maxZ() + 1),
                new BlockPos(parcel.minX(),     y, parcel.maxZ() + 1)
        );
        MapPolygon polygon = new MapPolygon(corners);

        // --- Color resolution ---
        Color[] colors;
        boolean isPreviewStroke;
        if (parcel.isPreview()) {
            isPreviewStroke = true;
            if (parcel.conflictState() > 0) {
                // Conflict — red regardless of type
                colors = new Color[]{ PREVIEW_CONFLICT_FILL, PREVIEW_CONFLICT_STROKE };
            } else {
                // Clear — use parcel type color at reduced opacity to show unconfirmed state
                Color[] typeColors = activeColorsForType(parcel.parcelType());
                int fillArgb   = (typeColors[0].getRGB() & 0x00FFFFFF) | 0x1A000000;  // ~10% alpha fill
                int strokeArgb = (typeColors[1].getRGB() & 0x00FFFFFF) | 0x66000000;  // ~40% alpha stroke
                colors = new Color[]{ new Color(fillArgb, true), new Color(strokeArgb, true) };
            }
        } else if (parcel.conflictState() > 0) {
            isPreviewStroke = false;
            // Conflict overrides ownership color — always red, matching in-world renderer.
            colors = new Color[]{ CONFLICT_FILL, CONFLICT_STROKE };
        } else {
            isPreviewStroke = false;
            boolean isOwner = localPlayerId != null && localPlayerId.equals(parcel.ownerId());
            colors = isOwner
                    ? activeColorsForType(parcel.parcelType())
                    : mutedColorsForType(parcel.parcelType());
        }

        // Preview uses strokeWidth=1f (thinner) to visually distinguish from committed.
        // Committed nation parcels use 4f; others use 2f.
        float strokeWidth = isPreviewStroke ? 1f
                : (parcel.parcelType() == ParcelType.NATION ? 4f : 2f);

        ShapeProperties shapeProps = new ShapeProperties()
                .setFillColor(colors[0].getRGB())
                .setFillOpacity(colors[0].getAlpha() / 255f)
                .setStrokeColor(colors[1].getRGB())
                .setStrokeOpacity(colors[1].getAlpha() / 255f)
                .setStrokeWidth(strokeWidth);

        TextProperties fullscreenTextProps = new TextProperties()
                .setColor(colors[1].getRGB())
                .setFontShadow(true)
                .setActiveUIs(EnumSet.of(Context.UI.Fullscreen, Context.UI.Webmap));

        String fullDisplayId = ClaimMyLand.MOD_ID + ":parcel:full:" + parcel.parcelId();
        PolygonOverlay fullOverlay = new PolygonOverlay(ClaimMyLand.MOD_ID, fullDisplayId, dimKey, shapeProps, polygon);
        fullOverlay.setLabel(buildFullLabel(parcel))
                .setTextProperties(fullscreenTextProps);

        TextProperties minimapTextProps = new TextProperties()
                .setOpacity(0f)
                .setActiveUIs(EnumSet.of(Context.UI.Minimap));

        String miniDisplayId = ClaimMyLand.MOD_ID + ":parcel:mini:" + parcel.parcelId();
        PolygonOverlay miniOverlay = new PolygonOverlay(ClaimMyLand.MOD_ID, miniDisplayId, dimKey, shapeProps, polygon);
        miniOverlay.setLabel(parcel.estateName())
                .setTextProperties(minimapTextProps);

        return new PolygonOverlay[]{ fullOverlay, miniOverlay };
    }

    /**
     * Builds a buffer zone polygon overlay for a parcel — an outer ring showing
     * the no-build buffer area around the parcel bounds.
     *
     * <p>The buffer is stroke-only (no fill) to keep the map readable. Conflict
     * state is reflected in the stroke colour matching {@code buildOverlay()}.</p>
     *
     * <p>Buffer radius is sourced from {@code ClientServerConfig} which is
     * populated by {@code ServerConfigSyncPacket} on login.</p>
     *
     * Returns {@code null} if the parcel's dimension cannot be resolved.
     */
    public static PolygonOverlay buildBufferOverlay(ClientParcel parcel) {
        ResourceKey<Level> dimKey = dimensionKey(parcel.dimension());
        if (dimKey == null) return null;

        int buf = parcel.parcelType() == ParcelType.NATION
                ? ClientServerConfig.getNationParcelBufferRadius()
                : ClientServerConfig.getParcelBufferRadius();

        int y = parcel.minY();
        List<BlockPos> corners = List.of(
                new BlockPos(parcel.minX() - buf,     y, parcel.minZ() - buf),
                new BlockPos(parcel.maxX() + buf + 1, y, parcel.minZ() - buf),
                new BlockPos(parcel.maxX() + buf + 1, y, parcel.maxZ() + buf + 1),
                new BlockPos(parcel.minX() - buf,     y, parcel.maxZ() + buf + 1)
        );
        MapPolygon polygon = new MapPolygon(corners);

        Color strokeColor = (parcel.conflictState() > 0)
                ? BUFFER_CONFLICT_STROKE
                : BUFFER_STROKE;

        ShapeProperties shapeProps = new ShapeProperties()
                .setFillColor(0xFFFFFF).setFillOpacity(0f)          // no fill
                .setStrokeColor(strokeColor.getRGB())
                .setStrokeOpacity(strokeColor.getAlpha() / 255f)
                .setStrokeWidth(1f);

        String displayId = ClaimMyLand.MOD_ID + ":parcel:buffer:" + parcel.parcelId();
        return new PolygonOverlay(ClaimMyLand.MOD_ID, displayId, dimKey, shapeProps, polygon);
    }

    /**
     * Removes any existing overlay for the parcel ID first, then builds and shows fresh overlays.
     */
    public static void notifyParcelAdded(ClientParcel parcel) {
        notifyParcelRemoved(parcel.parcelId());
        PolygonOverlay[] overlays = buildOverlay(parcel, localPlayerId());
        if (overlays == null) return;
        ACTIVE_OVERLAYS.put(parcel.parcelId(), overlays);
        JourneyMapOverlayHandler.showOverlay(overlays[0]);
        JourneyMapOverlayHandler.showOverlay(overlays[1]);

        PolygonOverlay bufferOverlay = buildBufferOverlay(parcel);
        if (bufferOverlay != null) {
            BUFFER_OVERLAYS.put(parcel.parcelId(), bufferOverlay);
            JourneyMapOverlayHandler.showOverlay(bufferOverlay);
        }
    }

    /**
     * Called from packet handlers when a parcel is removed.
     * Also clears the preview overlay if the removed parcel ID matches the current preview.
     */
    public static void notifyParcelRemoved(UUID parcelId) {
        if (parcelId.equals(previewParcelId)) {
            clearPreviewOverlay();
        }
        PolygonOverlay[] existing = ACTIVE_OVERLAYS.remove(parcelId);
        if (existing != null) {
            JourneyMapOverlayHandler.removeOverlay(existing[0]);
            JourneyMapOverlayHandler.removeOverlay(existing[1]);
        }
        PolygonOverlay existingBuffer = BUFFER_OVERLAYS.remove(parcelId);
        if (existingBuffer != null) {
            JourneyMapOverlayHandler.removeOverlay(existingBuffer);
        }
    }

    /** Clears the ACTIVE_OVERLAYS and BUFFER_OVERLAYS caches without removing overlays from JourneyMap. */
    public static void clearCache() {
        ACTIVE_OVERLAYS.clear();
        BUFFER_OVERLAYS.clear();
    }

    /**
     * Shows or updates the Foundation Stone preview overlay on the JourneyMap.
     *
     * @param parcelId     the preview parcel's UUID
     * @param minX         proposed parcel min X
     * @param minY         proposed parcel min Y
     * @param minZ         proposed parcel min Z
     * @param maxX         proposed parcel max X
     * @param maxY         proposed parcel max Y
     * @param maxZ         proposed parcel max Z
     * @param intersects   true if the proposed bounds conflict with a committed parcel — renders red
     * @param dimension    the dimension resource key for the overlay
     */
    public static void showPreviewOverlay(UUID parcelId,
                                          int minX, int minY, int minZ,
                                          int maxX, int maxY, int maxZ,
                                          boolean intersects,
                                          ResourceKey<Level> dimension) {
        clearPreviewOverlay();

        Color fillColor   = intersects ? PREVIEW_CONFLICT_FILL   : PREVIEW_CLEAR_FILL;
        Color strokeColor = intersects ? PREVIEW_CONFLICT_STROKE : PREVIEW_CLEAR_STROKE;

        ShapeProperties shapeProps = new ShapeProperties()
                .setFillColor(fillColor.getRGB())
                .setFillOpacity(fillColor.getAlpha() / 255f)
                .setStrokeColor(strokeColor.getRGB())
                .setStrokeOpacity(strokeColor.getAlpha() / 255f)
                .setStrokeWidth(2f);

        List<BlockPos> corners = List.of(
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, minY, minZ),
                new BlockPos(maxX, minY, maxZ),
                new BlockPos(minX, minY, maxZ)
        );
        MapPolygon polygon = new MapPolygon(corners);

        String displayId = ClaimMyLand.MOD_ID + ":preview:" + parcelId;
        previewOverlay  = new PolygonOverlay(ClaimMyLand.MOD_ID, displayId, dimension, shapeProps, polygon);
        previewParcelId = parcelId;
        JourneyMapOverlayHandler.showOverlay(previewOverlay);

        // Buffer zone overlay for the preview
        int buf = ClientServerConfig.getParcelBufferRadius();
        Color bufStroke = intersects ? BUFFER_CONFLICT_STROKE : BUFFER_STROKE;
        ShapeProperties bufferShape = new ShapeProperties()
                .setFillColor(0xFFFFFF).setFillOpacity(0f)
                .setStrokeColor(bufStroke.getRGB())
                .setStrokeOpacity(bufStroke.getAlpha() / 255f)
                .setStrokeWidth(1f);
        List<BlockPos> bufCorners = List.of(
                new BlockPos(minX - buf,     minY, minZ - buf),
                new BlockPos(maxX + buf + 1, minY, minZ - buf),
                new BlockPos(maxX + buf + 1, minY, maxZ + buf + 1),
                new BlockPos(minX - buf,     minY, maxZ + buf + 1)
        );
        String bufDisplayId = ClaimMyLand.MOD_ID + ":preview:buffer:" + parcelId;
        previewBufferOverlay = new PolygonOverlay(ClaimMyLand.MOD_ID, bufDisplayId, dimension, bufferShape, new MapPolygon(bufCorners));
        JourneyMapOverlayHandler.showOverlay(previewBufferOverlay);
    }

    /** Removes the current Foundation Stone preview overlay from the JourneyMap. */
    public static void clearPreviewOverlay() {
        if (previewOverlay != null) {
            JourneyMapOverlayHandler.removeOverlay(previewOverlay);
            previewOverlay  = null;
        }
        if (previewBufferOverlay != null) {
            JourneyMapOverlayHandler.removeOverlay(previewBufferOverlay);
            previewBufferOverlay = null;
        }
        previewParcelId = null;
        // Note: conflict overlays are NOT cleared here — they persist until
        // the distance/time condition is met (checked by ParcelBorderRenderer)
        // and cleared explicitly via clearConflictOverlays().
    }

    /**
     * Shows orange conflict highlight overlays (inner + buffer) for all parcels
     * that overlap the current Foundation Stone preview.
     *
     * <p>Clears any existing conflict overlays first so stale highlights from a
     * previous right-click are always replaced.</p>
     *
     * @param conflicting list of committed parcels overlapping the preview bounds
     * @param dimension   the dimension resource key for the overlays
     */
    public static void showConflictOverlays(List<ClientParcel> conflicting,
                                            ResourceKey<Level> dimension) {
        clearConflictOverlays();
        if (conflicting == null || conflicting.isEmpty()) return;

        for (ClientParcel parcel : conflicting) {
            ResourceKey<Level> dimKey = dimensionKey(parcel.dimension());
            if (dimKey == null) continue;

            // Inner conflict overlay
            int y = parcel.minY();
            List<BlockPos> corners = List.of(
                    new BlockPos(parcel.minX(),     y, parcel.minZ()),
                    new BlockPos(parcel.maxX() + 1, y, parcel.minZ()),
                    new BlockPos(parcel.maxX() + 1, y, parcel.maxZ() + 1),
                    new BlockPos(parcel.minX(),     y, parcel.maxZ() + 1)
            );
            ShapeProperties innerShape = new ShapeProperties()
                    .setFillColor(CONFLICT_TARGET_FILL.getRGB())
                    .setFillOpacity(CONFLICT_TARGET_FILL.getAlpha() / 255f)
                    .setStrokeColor(CONFLICT_TARGET_STROKE.getRGB())
                    .setStrokeOpacity(CONFLICT_TARGET_STROKE.getAlpha() / 255f)
                    .setStrokeWidth(2f);
            String innerDisplayId = ClaimMyLand.MOD_ID + ":conflict:" + parcel.parcelId();
            PolygonOverlay innerOverlay = new PolygonOverlay(
                    ClaimMyLand.MOD_ID, innerDisplayId, dimKey, innerShape, new MapPolygon(corners));
            CONFLICT_OVERLAYS.put(parcel.parcelId(), innerOverlay);
            JourneyMapOverlayHandler.showOverlay(innerOverlay);

            // Buffer conflict overlay
            int buf = parcel.parcelType() == ParcelType.NATION
                    ? ClientServerConfig.getNationParcelBufferRadius()
                    : ClientServerConfig.getParcelBufferRadius();
            List<BlockPos> bufCorners = List.of(
                    new BlockPos(parcel.minX() - buf,     y, parcel.minZ() - buf),
                    new BlockPos(parcel.maxX() + buf + 1, y, parcel.minZ() - buf),
                    new BlockPos(parcel.maxX() + buf + 1, y, parcel.maxZ() + buf + 1),
                    new BlockPos(parcel.minX() - buf,     y, parcel.maxZ() + buf + 1)
            );
            ShapeProperties bufShape = new ShapeProperties()
                    .setFillColor(0xFFFFFF).setFillOpacity(0f)
                    .setStrokeColor(CONFLICT_TARGET_BUFFER_STROKE.getRGB())
                    .setStrokeOpacity(CONFLICT_TARGET_BUFFER_STROKE.getAlpha() / 255f)
                    .setStrokeWidth(1f);
            String bufDisplayId = ClaimMyLand.MOD_ID + ":conflict:buffer:" + parcel.parcelId();
            PolygonOverlay bufOverlay = new PolygonOverlay(
                    ClaimMyLand.MOD_ID, bufDisplayId, dimKey, bufShape, new MapPolygon(bufCorners));
            CONFLICT_BUFFER_OVERLAYS.put(parcel.parcelId(), bufOverlay);
            JourneyMapOverlayHandler.showOverlay(bufOverlay);
        }
    }

    /** Removes all orange conflict highlight overlays from the JourneyMap and clears the caches. */
    public static void clearConflictOverlays() {
        CONFLICT_OVERLAYS.values().forEach(JourneyMapOverlayHandler::removeOverlay);
        CONFLICT_OVERLAYS.clear();
        CONFLICT_BUFFER_OVERLAYS.values().forEach(JourneyMapOverlayHandler::removeOverlay);
        CONFLICT_BUFFER_OVERLAYS.clear();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Color[] activeColorsForType(ParcelType type) {
        return switch (type) {
            case NATION  -> new Color[]{ NATION_FILL,  NATION_STROKE  };
            case CITIZEN -> new Color[]{ CITIZEN_FILL, CITIZEN_STROKE };
            case ZONE    -> new Color[]{ ZONE_FILL,    ZONE_STROKE    };
            default      -> new Color[]{ PLAYER_FILL,  PLAYER_STROKE  };
        };
    }

    private static Color[] mutedColorsForType(ParcelType type) {
        Color[] active = activeColorsForType(type);
        return new Color[]{ desaturate(active[0]), desaturate(active[1]) };
    }

    private static Color desaturate(Color color) {
        int alpha = color.getAlpha();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1] * 0.5f, hsb[2] * 0.8f);
        return new Color((rgb & 0x00FFFFFF) | (alpha << 24), true);
    }

    private static UUID localPlayerId() {
        Minecraft mc = Minecraft.getInstance();
        return (mc != null && mc.player != null) ? mc.player.getUUID() : null;
    }

    private static String buildFullLabel(ClientParcel parcel) {
        String typeName = parcel.parcelType() != null ? parcel.parcelType().name() : "PARCEL";
        return parcel.estateName() + " (" + typeName + ")\nOwner: " + parcel.ownerName();
    }

    private static ResourceKey<Level> dimensionKey(String dimension) {
        return DimensionHelper.dimensionKey(dimension);
    }
}