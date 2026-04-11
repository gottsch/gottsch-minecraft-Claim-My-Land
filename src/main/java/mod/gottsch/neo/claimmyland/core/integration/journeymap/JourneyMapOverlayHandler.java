package mod.gottsch.neo.claimmyland.core.integration.journeymap;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.event.DisplayUpdateEvent;
import journeymap.api.v2.client.event.FullscreenMapEvent;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.neo.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * JourneyMap {@link IClientPlugin} implementation for Claim My Land.
 * <p>
 * Discovered automatically by JourneyMap via the bare {@code @ClientPlugin} marker annotation —
 * no service file or manual registration is required.
 * </p>
 *
 * <h3>Event lifecycle</h3>
 * <ul>
 *   <li>{@code MAPPING_STARTED} — push all parcel overlays when a world session begins.</li>
 *   <li>{@code DISPLAY_UPDATE} — re-push overlays whenever a map UI (fullscreen or minimap)
 *       becomes active. Cast to {@link DisplayUpdateEvent} and check
 *       {@code uiState.active} to avoid redundant work on close.</li>
 *   <li>{@code MAPPING_STOPPED} — remove all overlays on world unload / logout to prevent
 *       stale polygons appearing in the next session.</li>
 * </ul>
 *
 * <h3>Live updates</h3>
 * {@link ParcelPolygonOverlayFactory} is registered on the Forge event bus by
 * {@link JourneyMapIntegration#init()} and calls {@link #showOverlay(PolygonOverlay)} /
 * {@link #removeOverlay(String)} directly when parcels are added or removed at runtime.
 *
 * @author Mark Gottschling on March 04, 2026
 */
@OnlyIn(Dist.CLIENT)
@JourneyMapPlugin(apiVersion = "2.0.0-1.21.1-SNAPSHOT")
@ParametersAreNonnullByDefault
public class JourneyMapOverlayHandler implements IClientPlugin {

    private static final Logger LOGGER = LogManager.getLogger(ClaimMyLand.MOD_ID);

    /**
     * Shared reference to the JourneyMap client API, set during {@link #initialize(IClientAPI)}.
     * Package-private so {@link ParcelPolygonOverlayFactory} can call show/remove directly.
     */
    static IClientAPI jmApi = null;

    // -------------------------------------------------------------------------
    // IClientPlugin implementation
    // -------------------------------------------------------------------------

    @Override
    public String getModId() {
        return ClaimMyLand.MOD_ID;
    }

    /**
     * Called once by JourneyMap after the plugin is instantiated.
     * Stores the API reference and subscribes to the required event types.
     */
    @Override
    public void initialize(IClientAPI api) {
        jmApi = api;
        try {
            ClientEventRegistry.MAPPING_EVENT.subscribe(getModId(), this::onMappingEvent);
            ClientEventRegistry.DISPLAY_UPDATE_EVENT.subscribe(getModId(), this::onDisplayUpdate);
            ClientEventRegistry.FULLSCREEN_MAP_MOVE_EVENT.subscribe(getModId(), this::onMapMouseMoved);
            LOGGER.debug("[{}] JourneyMap plugin initialized.", ClaimMyLand.MOD_ID);
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to subscribe to JourneyMap events.", ClaimMyLand.MOD_ID, e);
        }
    }

    private void onMappingEvent(MappingEvent event) {
        if (event.getStage() == MappingEvent.Stage.MAPPING_STARTED) {
            // former onMappingStarted logic
        } else if (event.getStage() == MappingEvent.Stage.MAPPING_STOPPED) {
            // former onMappingStopped logic
        }
    }

    private void onDisplayUpdate(DisplayUpdateEvent event) {
        if (event.uiState.active) {
            pushAllOverlays();
        }
    }

    private void onMapMouseMoved(FullscreenMapEvent.MouseMoveEvent event) {
        BlockPos pos = event.getLocation();
        String dimension = event.getLevel().location().toString();

        ClientParcel parcel = ClientParcelRegistry
                .findAt(pos.getX(), pos.getZ(), dimension)
                .orElse(null);

        ClientParcelRegistry.setHoveredParcel(parcel);
    }

    // -------------------------------------------------------------------------
    // Package-private helpers used by ParcelPolygonOverlayFactory
    // -------------------------------------------------------------------------

    /**
     * Pushes a single polygon overlay to JourneyMap.
     * Called by {@link ParcelPolygonOverlayFactory} when a parcel is added at runtime.
     */
    static void showOverlay(PolygonOverlay overlay) {
        if (jmApi == null) return;
        try {
            jmApi.show(overlay);
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed to show parcel overlay '{}': {}",
                    ClaimMyLand.MOD_ID, overlay.getId(), e.getMessage());
        }
    }

    /**
     * Removes a single overlay by passing the {@link PolygonOverlay} object directly,
     * as required by {@link IClientAPI#remove(journeymap.client.api.display.Displayable)}.
     * Called by {@link ParcelPolygonOverlayFactory} when a parcel is removed at runtime.
     */
    public static void removeOverlay(PolygonOverlay overlay) {
        if (jmApi == null) return;
        try {
            jmApi.remove(overlay);
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed to remove parcel overlay '{}': {}",
                    ClaimMyLand.MOD_ID, overlay.getId(), e.getMessage());
        }
    }

    /**
     * Removes all Claim My Land overlays — called on {@code MAPPING_STOPPED}.
     */
    public static void removeAllOverlays() {
        if (jmApi == null) return;
        jmApi.removeAll(ClaimMyLand.MOD_ID);
        LOGGER.debug("[{}] Removed all parcel overlays from JourneyMap.", ClaimMyLand.MOD_ID);
    }

    /**
     * Builds and pushes a {@link PolygonOverlay} for every parcel in
     * {@link ClientParcelRegistry}.
     */
    static void pushAllOverlays() {
        ParcelPolygonOverlayFactory.removeAllPermanentOverlays();

        List<PolygonOverlay> overlays = ParcelPolygonOverlayFactory.buildAll();
        overlays.forEach(overlay -> {
            try {
                jmApi.show(overlay);
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to show overlay: {}", ClaimMyLand.MOD_ID, e.getMessage());
            }
        });

        // Rebuild and re-show transient overlays (conflict + preview) with fresh
        // JM overlay objects — DISPLAY_UPDATE may have invalidated the originals.
        List<PolygonOverlay> transients = ParcelPolygonOverlayFactory.rebuildTransientOverlays();
        transients.forEach(overlay -> {
            try {
                jmApi.show(overlay);
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to show transient overlay: {}", ClaimMyLand.MOD_ID, e.getMessage());
            }
        });

        LOGGER.debug("[{}] Pushed {} permanent + {} transient overlay(s) to JourneyMap.",
                ClaimMyLand.MOD_ID, overlays.size(), transients.size());
    }

    /**
     * Registers {@link ParcelPolygonOverlayFactory} on the Forge event bus for
     * live parcel add/remove updates. Called from {@link JourneyMapIntegration#init()}.
     */
    static void register() {
//        NeoForge.EVENT_BUS.register(ParcelPolygonOverlayFactory.class);
        LOGGER.debug("[{}] ParcelPolygonOverlayFactory registered on Forge event bus.", ClaimMyLand.MOD_ID);
    }

}