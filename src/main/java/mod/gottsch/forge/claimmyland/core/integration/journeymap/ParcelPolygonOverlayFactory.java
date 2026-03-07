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
 * Builds and manages {@link PolygonOverlay} instances for the JourneyMap display,
 * one overlay per parcel stored in {@link ClientParcelRegistry}.
 *
 * <h3>Overlay cache</h3>
 * Because {@link journeymap.client.api.IClientAPI#remove(journeymap.client.api.display.Displayable)}
 * requires the original {@link PolygonOverlay} object (not a string ID), this class maintains
 * an internal {@code Map<UUID, PolygonOverlay>} of all overlays it has shown. The cache is
 * cleared and rebuilt whenever {@link #buildAll()} is called (i.e. on {@code MAPPING_STARTED}
 * and {@code DISPLAY_UPDATE}).
 *
 * <h3>Color scheme</h3>
 * <table>
 *   <tr><th>Parcel type</th><th>Fill</th><th>Stroke</th></tr>
 *   <tr><td>NATION</td><td>Blue 20% alpha</td><td>Blue 80% alpha</td></tr>
 *   <tr><td>CITIZEN</td><td>Light-purple 20% alpha</td><td>Light-purple 80% alpha</td></tr>
 *   <tr><td>ZONE</td><td>Yellow 20% alpha</td><td>Yellow 80% alpha</td></tr>
 *   <tr><td>PLAYER (default)</td><td>Green 20% alpha</td><td>Green 80% alpha</td></tr>
 * </table>
 *
 * <h3>Live updates</h3>
 * {@link #notifyParcelAdded(ClientParcel)} and {@link #notifyParcelRemoved(UUID)} are called
 * directly from the network packet handlers (inside {@code ctx.get().enqueueWork()}) after
 * {@link ClientParcelRegistry} is mutated. No custom Forge event layer is needed.
 *
 * @author Mark Gottschling on 4/4/2026
 */
@OnlyIn(Dist.CLIENT)
public class ParcelPolygonOverlayFactory {

    private static final Logger LOGGER = LogManager.getLogger(ClaimMyLand.MOD_ID);

    /**
     * cache of every overlay currently shown, keyed by parcel UUID.
     * rebuilt from scratch on each {@link #buildAll()} call.
     */
    private static final Map<UUID, PolygonOverlay[]> ACTIVE_OVERLAYS = new HashMap<>();

    // -------------------------------------------------------------------------
    // Owned parcel colours  (ARGB — ~20% fill alpha, ~80% stroke alpha)
    // -------------------------------------------------------------------------

    private static final Color NATION_FILL    = new Color(0x3300AAFF, true);
    private static final Color NATION_STROKE  = new Color(0xCC00AAFF, true);

    private static final Color CITIZEN_FILL   = new Color(0x33AA55FF, true);
    private static final Color CITIZEN_STROKE = new Color(0xCCAA55FF, true);

    private static final Color ZONE_FILL      = new Color(0x33FFFF55, true);
    private static final Color ZONE_STROKE    = new Color(0xCCFFFF55, true);

    private static final Color PLAYER_FILL    = new Color(0x3355FF55, true);
    private static final Color PLAYER_STROKE  = new Color(0xCC55FF55, true);

    private ParcelPolygonOverlayFactory() {}

    // -------------------------------------------------------------------------
    // bulk build — called on MAPPING_STARTED / DISPLAY_UPDATE
    // -------------------------------------------------------------------------

    /**
     * builds a {@link PolygonOverlay} for every parcel currently held in
     * {@link ClientParcelRegistry}, replacing {@link #ACTIVE_OVERLAYS}.
     *
     * @return list of overlays ready to be shown — may be empty
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
     * builds a single {@link PolygonOverlay} from a {@link ClientParcel}.
     * <p>
     * parcels not owned by the local player receive a desaturated version of the
     * type colour. Labels show the estate name and are hidden on the minimap.
     * </p>
     *
     * @param parcel        the parcel to render
     * @param localPlayerId the local player's UUID — used to determine ownership colouring
     * @return a configured overlay, or {@code null} if the dimension cannot be resolved
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
                new BlockPos(parcel.minX(), y, parcel.minZ()),
                new BlockPos(parcel.maxX(), y, parcel.minZ()),
                new BlockPos(parcel.maxX(), y, parcel.maxZ()),
                new BlockPos(parcel.minX(), y, parcel.maxZ())
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

        // Fullscreen + webmap overlay — full label
        TextProperties fullscreenTextProps = new TextProperties()
                .setColor(colors[1].getRGB())
                .setFontShadow(true)
                .setActiveUIs(EnumSet.of(Context.UI.Fullscreen, Context.UI.Webmap));

        String fullDisplayId = ClaimMyLand.MOD_ID + ":parcel:full:" + parcel.parcelId();
        PolygonOverlay fullOverlay = new PolygonOverlay(ClaimMyLand.MOD_ID, fullDisplayId, dimKey, shapeProps, polygon);
        fullOverlay.setLabel(buildFullLabel(parcel))
                .setTextProperties(fullscreenTextProps);

        // Minimap overlay — estate name only
        TextProperties minimapTextProps = new TextProperties()
                .setColor(colors[1].getRGB())
                .setFontShadow(true)
                .setActiveUIs(EnumSet.of(Context.UI.Minimap));

        String miniDisplayId = ClaimMyLand.MOD_ID + ":parcel:mini:" + parcel.parcelId();
        PolygonOverlay miniOverlay = new PolygonOverlay(ClaimMyLand.MOD_ID, miniDisplayId, dimKey, shapeProps, polygon);
        miniOverlay.setLabel(parcel.estateName())
                .setTextProperties(minimapTextProps);

        return new PolygonOverlay[]{fullOverlay, miniOverlay};
    }
    // -------------------------------------------------------------------------
    // live-update entry points — called directly from packet handlers
    // -------------------------------------------------------------------------

    /**
     * called after a {@link ClientParcel} is added to (or updated in)
     * {@link ClientParcelRegistry}, inside {@code ctx.get().enqueueWork()}.
     *
     * <pre>{@code
     * if (ModList.get().isLoaded("journeymap")) {
     *     ParcelPolygonOverlayFactory.notifyParcelAdded(clientParcel);
     * }
     * }</pre>
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
     * called after a parcel is removed from {@link ClientParcelRegistry},
     * inside {@code ctx.get().enqueueWork()}.
     *
     * <pre>{@code
     * if (ModList.get().isLoaded("journeymap")) {
     *     ParcelPolygonOverlayFactory.notifyParcelRemoved(parcelId);
     * }
     * }</pre>
     */
    public static void notifyParcelRemoved(UUID parcelId) {
        PolygonOverlay[] existing = ACTIVE_OVERLAYS.remove(parcelId);
        if (existing == null) return;
        JourneyMapOverlayHandler.removeOverlay(existing[0]);
        JourneyMapOverlayHandler.removeOverlay(existing[1]);
    }

    /**
     * Clears the overlay cache. Called implicitly by {@link #buildAll()}.
     */
    public static void clearCache() {
        ACTIVE_OVERLAYS.clear();
    }

    // -------------------------------------------------------------------------
    // colour helpers
    // -------------------------------------------------------------------------

    /**
     * returns {@code [fill, stroke]} vivid colours for a parcel owned by the local player.
     */
    private static Color[] activeColorsForType(ParcelType type) {
        if (type == null) return new Color[]{PLAYER_FILL, PLAYER_STROKE};
        return switch (type) {
            case NATION  -> new Color[]{NATION_FILL,  NATION_STROKE};
            case CITIZEN -> new Color[]{CITIZEN_FILL, CITIZEN_STROKE};
            case ZONE    -> new Color[]{ZONE_FILL,    ZONE_STROKE};
            default      -> new Color[]{PLAYER_FILL,  PLAYER_STROKE};
        };
    }

    /**
     * returns {@code [fill, stroke]} muted colours for a parcel owned by another player.
     * the hue of the type colour is preserved but saturation is reduced to 30% and
     * brightness to 60%, giving a clearly faded appearance while remaining identifiable
     * by type.
     */
    private static Color[] mutedColorsForType(ParcelType type) {
        Color[] active = activeColorsForType(type);
        return new Color[]{
                desaturate(active[0]),
                desaturate(active[1])
        };
    }

    /**
     * desaturates a colour by converting to HSB, reducing saturation to 30% and
     * brightness to 60%, then converting back to RGB while preserving the original alpha.
     */
    private static Color desaturate(Color color) {
        int alpha = color.getAlpha();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hsb[1] = hsb[1] * 0.30f;  // saturation → 30% of original
        hsb[2] = hsb[2] * 0.60f;  // brightness  → 60% of original
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return new Color((rgb & 0x00FFFFFF) | (alpha << 24), true);
    }

    // -------------------------------------------------------------------------
    // private utilities
    // -------------------------------------------------------------------------

    /**
     * returns the local player's UUID, or {@code null} if no player is loaded
     * (should not happen in normal gameplay but guards against edge cases during
     * world load/unload).
     */
    private static UUID localPlayerId() {
        Minecraft mc = Minecraft.getInstance();
        return (mc != null && mc.player != null) ? mc.player.getUUID() : null;
    }

    /**
     * builds the label shown on the fullscreen map.
     * Format: {@code "<estate name> (<TYPE>)\nOwner: <ownerName>"}
     */
    private static String buildFullLabel(ClientParcel parcel) {
        String typeName = parcel.parcelType() != null ? parcel.parcelType().name() : "PARCEL";
        return parcel.estateName() + " (" + typeName + ")\nOwner: " + parcel.ownerName();
    }

    /**
     * resolves a dimension string to a {@link ResourceKey}{@code <Level>}.
     * falls back to {@link Level#OVERWORLD} if blank or unresolvable.
     */
    private static ResourceKey<Level> dimensionKey(String dimension) {
        if (dimension == null || dimension.isBlank()) return Level.OVERWORLD;
        return switch (dimension) {
            case "minecraft:overworld"  -> Level.OVERWORLD;
            case "minecraft:the_nether" -> Level.NETHER;
            case "minecraft:the_end"    -> Level.END;
            default -> {
                try {
                    yield ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            new ResourceLocation(dimension));
                } catch (Exception e) {
                    LOGGER.warn("[{}] invalid dimension '{}' — defaulting to overworld.",
                            ClaimMyLand.MOD_ID, dimension);
                    yield Level.OVERWORLD;
                }
            }
        };
    }
}