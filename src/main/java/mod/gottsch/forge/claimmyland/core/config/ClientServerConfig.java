package mod.gottsch.forge.claimmyland.core.config;

/**
 * Client-side holder for server config values that the client needs but cannot
 * read directly from {@code Config.SERVER} (which is only populated on the server).
 *
 * <p>Values are populated by {@code ServerConfigSyncPacket} on player login
 * and dimension change.
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

    /** Default matches {@code Config.SERVER.general.enableParcelEntryTitle} default (true). */
    private static boolean enableParcelEntryTitle = true;

    private ClientServerConfig() {}

    public static int getParcelBufferRadius() {
        return parcelBufferRadius;
    }

    public static int getNationParcelBufferRadius() {
        return nationParcelBufferRadius;
    }

    public static boolean isParcelEntryTitleEnabled() {
        return enableParcelEntryTitle;
    }

    /**
     * Called by {@code ServerConfigSyncPacket.handle()} to populate values
     * received from the server.
     */
    public static void update(int parcelBufferRadius, int nationParcelBufferRadius,
                              boolean enableParcelEntryTitle) {
        ClientServerConfig.parcelBufferRadius       = parcelBufferRadius;
        ClientServerConfig.nationParcelBufferRadius = nationParcelBufferRadius;
        ClientServerConfig.enableParcelEntryTitle   = enableParcelEntryTitle;
    }
}