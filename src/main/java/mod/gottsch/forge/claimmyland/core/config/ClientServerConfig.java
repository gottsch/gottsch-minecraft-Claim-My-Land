package mod.gottsch.forge.claimmyland.core.config;

/**
 * Client-side holder for server config values that the client needs but cannot
 * read directly from {@code Config.SERVER} (which is only populated on the server).
 *
 * <p>Values are populated by {@code ServerConfigSyncPacket} on player login.
 * Defaults match the server config defaults so the client behaves correctly
 * even before the sync packet arrives (e.g. during the brief login window).</p>
 *
 * @author Mark Gottschling on March 20, 2026
 */
public class ClientServerConfig {

    /** Default matches {@code Config.SERVER.general.parcelBufferRadius} default (3). */
    private static int parcelBufferRadius = 3;

    /** Default matches {@code Config.SERVER.general.nationParcelBufferRadius} default (10). */
    private static int nationParcelBufferRadius = 10;

    private ClientServerConfig() {}

    public static int getParcelBufferRadius() {
        return parcelBufferRadius;
    }

    public static int getNationParcelBufferRadius() {
        return nationParcelBufferRadius;
    }

    /**
     * Called by {@code ServerConfigSyncPacket.handle()} to populate values
     * received from the server.
     */
    public static void update(int parcelBufferRadius, int nationParcelBufferRadius) {
        ClientServerConfig.parcelBufferRadius = parcelBufferRadius;
        ClientServerConfig.nationParcelBufferRadius = nationParcelBufferRadius;
    }
}