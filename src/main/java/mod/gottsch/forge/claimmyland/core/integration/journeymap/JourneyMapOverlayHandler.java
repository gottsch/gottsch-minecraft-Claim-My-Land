package mod.gottsch.forge.claimmyland.core.integration.journeymap;

import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.DisplayUpdateEvent;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
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
@journeymap.client.api.ClientPlugin
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
            api.subscribe(getModId(), EnumSet.of(
                    ClientEvent.Type.MAPPING_STARTED,
                    ClientEvent.Type.MAPPING_STOPPED,
                    ClientEvent.Type.DISPLAY_UPDATE
            ));
            LOGGER.debug("[{}] JourneyMap plugin initialized.", ClaimMyLand.MOD_ID);
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to subscribe to JourneyMap events.", ClaimMyLand.MOD_ID, e);
        }
    }

    /**
     * Called by JourneyMap on each subscribed event.
     */
    @Override
    public void onEvent(ClientEvent event) {
        if (jmApi == null) return;

        switch (event.type) {
            case MAPPING_STARTED:
                // World session started — push all known parcel overlays.
                pushAllOverlays();
                break;

            case DISPLAY_UPDATE:
                // Fires when any map UI (fullscreen map, minimap) changes active state.
                // Only push when the UI is becoming active; skip the closing event.
                DisplayUpdateEvent due = (DisplayUpdateEvent) event;
                if (due.uiState.active) {
                    pushAllOverlays();
                }
                break;

            case MAPPING_STOPPED:
                // World unloaded or player logged out — clean up to avoid stale polygons.
                removeAllOverlays();
                break;

            default:
                break;
        }
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
    static void removeOverlay(PolygonOverlay overlay) {
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
    static void removeAllOverlays() {
        if (jmApi == null) return;
        jmApi.removeAll(ClaimMyLand.MOD_ID);
        LOGGER.debug("[{}] Removed all parcel overlays from JourneyMap.", ClaimMyLand.MOD_ID);
    }

    /**
     * Builds and pushes a {@link PolygonOverlay} for every parcel in
     * {@link mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry}.
     */
    static void pushAllOverlays() {
//        if (jmApi == null) return;
//        List<PolygonOverlay> overlays = ParcelPolygonOverlayFactory.buildAll();
//        for (PolygonOverlay overlay : overlays) {
//            showOverlay(overlay);
//        }
        if (jmApi == null) return;  // JM not yet initialized or not loaded
        jmApi.removeAll(ClaimMyLand.MOD_ID);
        List<PolygonOverlay> overlays = ParcelPolygonOverlayFactory.buildAll();
        overlays.forEach(overlay -> {
            try {
                jmApi.show(overlay);
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to show overlay: {}", ClaimMyLand.MOD_ID, e.getMessage());
            }
        });
        LOGGER.debug("[{}] Pushed {} parcel overlay(s) to JourneyMap.", ClaimMyLand.MOD_ID, overlays.size());
    }

    /**
     * Registers {@link ParcelPolygonOverlayFactory} on the Forge event bus for
     * live parcel add/remove updates. Called from {@link JourneyMapIntegration#init()}.
     */
    static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ParcelPolygonOverlayFactory.class);
        LOGGER.debug("[{}] ParcelPolygonOverlayFactory registered on Forge event bus.", ClaimMyLand.MOD_ID);
    }
}