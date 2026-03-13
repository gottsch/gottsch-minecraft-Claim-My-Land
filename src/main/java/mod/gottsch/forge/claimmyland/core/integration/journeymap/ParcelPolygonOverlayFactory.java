package mod.gottsch.forge.claimmyland.core.integration.journeymap;

import journeymap.client.api.display.Context;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.model.TextProperties;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Builds PolygonOverlay objects for JourneyMap and maintains the ACTIVE_OVERLAYS
 * cache. Also manages the Foundation Stone preview overlay separately.
 *
 * Each claimed parcel gets two overlays (full-screen/webmap + minimap).
 * The preview overlay is a single overlay stored in previewOverlay and
 * identified by previewParcelId — never mixed with ACTIVE_OVERLAYS.
 *
 * @author Mark Gottschling on Mar 11, 2026
 */
@OnlyIn(Dist.CLIENT)
public class ParcelPolygonOverlayFactory {

    private static final Logger LOGGER = LogManager.getLogger(ClaimMyLand.MOD_ID);

    // --- Parcel type colors (vivid) ---
    private static final Color NATION_FILL    = new Color(0x3300AAFF, true);
    private static final Color NATION_STROKE  = new Color(0xCC00AAFF, true);
    private static final Color CITIZEN_FILL   = new Color(0x33AA55FF, true);
    private static final Color CITIZEN_STROKE = new Color(0xCCAA55FF, true);
    private static final Color ZONE_FILL      = new Color(0x33FFFF55, true);
    private static final Color ZONE_STROKE    = new Color(0xCCFFFF55, true);
    private static final Color PLAYER_FILL    = new Color(0x3355FF55, true);
    private static final Color PLAYER_STROKE  = new Color(0xCC55FF55, true);

    // --- Preview overlay colors ---
    private static final Color PREVIEW_CLEAR_FILL   = new Color(0x3300FF00, true);
    private static final Color PREVIEW_CLEAR_STROKE = new Color(0xCC00FF00, true);
    private static final Color PREVIEW_CONFLICT_FILL   = new Color(0x33FF0000, true);
    private static final Color PREVIEW_CONFLICT_STROKE = new Color(0xCCFF0000, true);

    /** Cache of active parcel overlays keyed by parcel UUID. Each entry holds [fullOverlay, miniOverlay]. */
    private static final Map<UUID, PolygonOverlay[]> ACTIVE_OVERLAYS = new HashMap<>();

    /** The Foundation Stone preview overlay — stored separately from ACTIVE_OVERLAYS. */
    private static PolygonOverlay previewOverlay = null;

    /** The parcel ID associated with the current preview overlay, used for cleanup in notifyParcelRemoved(). */
    private static UUID previewParcelId = null;

    // -------------------------------------------------------------------------
    // Claimed parcel overlays
    // -------------------------------------------------------------------------

    /**
     * Clears the ACTIVE_OVERLAYS cache and builds fresh overlays for every parcel
     * in ClientParcelRegistry. Called by JourneyMapOverlayHandler on MAPPING_STARTED
     * and DISPLAY_UPDATE (active=true).
     */
    public static List<PolygonOverlay> buildAll() {
        ACTIVE_OVERLAYS.clear();

        List<PolygonOverlay> result = new ArrayList<>();
        UUID localPlayerId = localPlayerId();

        for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
            PolygonOverlay[] overlays = buildOverlay(parcel, localPlayerId);
            if (overlays == null) continue;

            ACTIVE_OVERLAYS.put(parcel.parcelId(), overlays);
            result.add(overlays[0]);
            result.add(overlays[1]);
        }

        return result;
    }

    /**
     * Builds a fullscreen+webmap overlay and a minimap overlay for the given parcel.
     * Returns null if the dimension cannot be resolved.
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
                new BlockPos(parcel.minX(), y, parcel.minZ()), // NW
                new BlockPos(parcel.maxX(), y, parcel.minZ()), // NE
                new BlockPos(parcel.maxX(), y, parcel.maxZ()), // SE
                new BlockPos(parcel.minX(), y, parcel.maxZ())  // SW
        );
        MapPolygon polygon = new MapPolygon(corners);

        boolean isOwner = localPlayerId != null && localPlayerId.equals(parcel.ownerId());
        Color[] colors = isOwner
                ? activeColorsForType(parcel.parcelType())
                : mutedColorsForType(parcel.parcelType());

        ShapeProperties shapeProps = new ShapeProperties()
                .setFillColor(colors[0].getRGB())
                .setFillOpacity(colors[0].getAlpha() / 255f)
                .setStrokeColor(colors[1].getRGB())
                .setStrokeOpacity(colors[1].getAlpha() / 255f)
                .setStrokeWidth(2f);

        // Full-screen + webmap overlay (with multi-line label)
        TextProperties fullscreenTextProps = new TextProperties()
                .setColor(colors[1].getRGB())
                .setFontShadow(true)
                .setActiveUIs(EnumSet.of(Context.UI.Fullscreen, Context.UI.Webmap));
        String fullDisplayId = ClaimMyLand.MOD_ID + ":parcel:full:" + parcel.parcelId();
        PolygonOverlay fullOverlay = new PolygonOverlay(ClaimMyLand.MOD_ID, fullDisplayId, dimKey, shapeProps, polygon);
        fullOverlay.setLabel(buildFullLabel(parcel))
                .setTextProperties(fullscreenTextProps);

        // Minimap overlay (estate name only)
        TextProperties minimapTextProps = new TextProperties()
                .setColor(colors[1].getRGB())
                .setFontShadow(true)
                .setActiveUIs(EnumSet.of(Context.UI.Minimap));
        String miniDisplayId = ClaimMyLand.MOD_ID + ":parcel:mini:" + parcel.parcelId();
        PolygonOverlay miniOverlay = new PolygonOverlay(ClaimMyLand.MOD_ID, miniDisplayId, dimKey, shapeProps, polygon);
        miniOverlay.setLabel(parcel.estateName())
                .setTextProperties(minimapTextProps);

        return new PolygonOverlay[] { fullOverlay, miniOverlay };
    }

    /**
     * Called from SyncParcelPacket / CacheSyncPacket handlers when a parcel is
     * added or re-synced. Removes any existing overlay for that ID first to avoid
     * duplicates, then builds and shows fresh overlays.
     */
    public static void notifyParcelAdded(ClientParcel parcel) {
        notifyParcelRemoved(parcel.parcelId());

        PolygonOverlay[] overlays = buildOverlay(parcel, localPlayerId());
        if (overlays == null) return;

        ACTIVE_OVERLAYS.put(parcel.parcelId(), overlays);
        JourneyMapOverlayHandler.showOverlay(overlays[0]);
        JourneyMapOverlayHandler.showOverlay(overlays[1]);
    }

    /**
     * Called from RemoveParcelPacket handler when a parcel is demolished or removed.
     * Also handles cleanup of the preview overlay if this parcel ID matches the
     * current preview (i.e. the Foundation Stone was broken / claim was completed).
     */
    public static void notifyParcelRemoved(UUID parcelId) {
        // Clear preview overlay if this is the preview parcel being removed
        if (parcelId.equals(previewParcelId)) {
            clearPreviewOverlay();
        }

        PolygonOverlay[] existing = ACTIVE_OVERLAYS.remove(parcelId);
        if (existing != null) {
            JourneyMapOverlayHandler.removeOverlay(existing[0]);
            JourneyMapOverlayHandler.removeOverlay(existing[1]);
        }
    }

    /**
     * Clears the ACTIVE_OVERLAYS cache without touching JourneyMap.
     * Called implicitly by buildAll().
     */
    public static void clearCache() {
        ACTIVE_OVERLAYS.clear();
    }

    // -------------------------------------------------------------------------
    // Foundation Stone preview overlay
    // -------------------------------------------------------------------------

    /**
     * Shows (or refreshes) a temporary Foundation Stone preview overlay on JourneyMap.
     * Green = no intersection with existing parcels; red = intersects one or more parcels.
     * Any existing preview overlay is cleared first.
     *
     * @param parcelId   the preview parcel UUID (from FoundationStoneBlockEntity)
     * @param minX       proposed parcel min X (absolute world coords)
     * @param minY       proposed parcel min Y
     * @param minZ       proposed parcel min Z
     * @param maxX       proposed parcel max X
     * @param maxY       proposed parcel max Y (unused by JM 2D display, kept for consistency)
     * @param maxZ       proposed parcel max Z
     * @param intersects true if the proposed bounds overlap an existing parcel
     * @param dimension  the dimension ResourceKey for the overlay
     *
     * @author Mark Gottschling on Mar 11, 2026
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
                new BlockPos(minX, minY, minZ), // NW
                new BlockPos(maxX, minY, minZ), // NE
                new BlockPos(maxX, minY, maxZ), // SE
                new BlockPos(minX, minY, maxZ)  // SW
        );
        MapPolygon polygon = new MapPolygon(corners);

        String displayId = ClaimMyLand.MOD_ID + ":preview:" + parcelId;
        previewOverlay = new PolygonOverlay(ClaimMyLand.MOD_ID, displayId, dimension, shapeProps, polygon);
        previewParcelId = parcelId;

        JourneyMapOverlayHandler.showOverlay(previewOverlay);
    }

    /**
     * Removes the Foundation Stone preview overlay from JourneyMap and clears
     * the stored reference. Safe to call when no preview is active.
     *
     * @author Mark Gottschling on Mar 11, 2026
     */
    public static void clearPreviewOverlay() {
        if (previewOverlay != null) {
            JourneyMapOverlayHandler.removeOverlay(previewOverlay);
            previewOverlay = null;
            previewParcelId = null;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
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
        // Halve saturation, reduce brightness slightly
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
        if (dimension == null || dimension.isBlank()) return Level.OVERWORLD;
        try {
            return ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new ResourceLocation(dimension));
        } catch (Exception e) {
            return null;
        }
    }
}