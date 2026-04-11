package mod.gottsch.neo.claimmyland.core.integration.journeymap;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for optional JourneyMap integration.
 * <p>
 * This class must only be loaded when JourneyMap is present on the classpath.
 * All references to JourneyMap API classes are isolated inside this package so
 * that the mod loads and functions normally when JourneyMap is absent.
 * </p>
 *
 * <p>Activation: call {@link #init()} from {@code CommonSetup} guarded by a
 * {@code ModList.get().isLoaded("journeymap")} check.</p>
 *
 * <pre>{@code
 * // In CommonSetup.init():
 * if (ModList.get().isLoaded("journeymap")) {
 *     JourneyMapIntegration.init();
 * }
 * }</pre>
 *
 * @author Mark Gottschling on March 04, 2026
 */
public class JourneyMapIntegration {

    private static final Logger LOGGER = LogManager.getLogger(ClaimMyLand.MOD_ID);

    private JourneyMapIntegration() {}

    /**
     * Initialises the JourneyMap integration.
     * <p>
     * Registers the {@link JourneyMapOverlayHandler} on the Forge event bus so
     * that it can hook the JourneyMap client plugin lifecycle and install the
     * {@link ParcelPolygonOverlayFactory} when JourneyMap signals it is ready.
     * </p>
     *
     * <p>Must only be called when {@code ModList.get().isLoaded("journeymap")} is
     * {@code true}.</p>
     */
    public static void init() {
//        LOGGER.debug("[{}] JourneyMap detected — enabling parcel map overlay.", ClaimMyLand.MOD_ID);
//        JourneyMapOverlayHandler.register();
    }
}