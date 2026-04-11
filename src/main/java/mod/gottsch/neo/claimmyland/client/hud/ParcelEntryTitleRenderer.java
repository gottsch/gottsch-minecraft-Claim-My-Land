package mod.gottsch.neo.claimmyland.client.hud;

import mod.gottsch.neo.claimmyland.core.config.ClientServerConfig;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Displays a vanilla title/subtitle overlay when the player enters a parcel.
 * <p>
 * Called from {@code CacheSyncPacket.handle()} whenever the client cache transitions
 * to a new parcel or to wilderness.
 * </p>
 *
 * <h3>Display rules</h3>
 * <ul>
 *   <li>Only fires for {@code NATION}, {@code CITIZEN}, and {@code PLAYER} parcel types.</li>
 *   <li>{@code ZONE} and wilderness transitions are silent.</li>
 *   <li>{@code NATION} — estate name as the large <b>title</b>, type + owner as the small subtitle.</li>
 *   <li>{@code CITIZEN} / {@code PLAYER} — estate name as the small <b>subtitle</b>, title slot empty,
 *       giving a visually quieter display than a Nation announcement.</li>
 *   <li>A per-parcel cooldown prevents re-firing when the player briefly crosses
 *       and re-crosses the same boundary.</li>
 * </ul>
 *
 * <h3>Server / client config</h3>
 * The server config {@code enableParcelEntryTitle} is the master switch — if false the
 * packet is never sent, so no client ever sees titles regardless of client config.
 * The client config {@code enableParcelEntryTitle} is a secondary opt-out for individual
 * players on servers that have the feature enabled.
 *
 * @author Mark Gottschling on March 25, 2026
 */
public class ParcelEntryTitleRenderer {

    private static final int MAX_COOLDOWN_ENTRIES = 64;

    /**
     * Per-parcel cooldown map. Key = parcel UUID, value = last display time (ms).
     * Bounded LRU — evicts the least-recently-accessed entry once capacity is exceeded.
     */
    private static final Map<UUID, Long> COOLDOWN_MAP =
            Collections.synchronizedMap(
                    new LinkedHashMap<>(MAX_COOLDOWN_ENTRIES, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) {
                            return size() > MAX_COOLDOWN_ENTRIES;
                        }
                    });

    private ParcelEntryTitleRenderer() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Called from {@code CacheSyncPacket.handle()} when the client cache is updated.
     *
     * @param parcel the newly entered parcel, or {@code null} if the player has
     *               entered the wilderness
     */
    public static void onParcelChanged(ClientParcel parcel) {
        // Server master switch — synced at login via ServerConfigSyncPacket
        if (!ClientServerConfig.isParcelEntryTitleEnabled()) return;

        // Client config opt-out
        if (!Config.CLIENT.gui.enableParcelEntryTitle.get()) return;

        // Wilderness — silent
        if (parcel == null) return;

        // Only show for NATION, CITIZEN, PLAYER
        ParcelType type = parcel.parcelType();
        if (type != ParcelType.NATION && type != ParcelType.CITIZEN && type != ParcelType.PLAYER) {
            return;
        }

        // Cooldown check
        UUID key = parcel.parcelId();
        long now = System.currentTimeMillis();
        long cooldownMs = Config.CLIENT.gui.parcelEntryCooldownSeconds.get() * 1000L;
        Long lastShown = COOLDOWN_MAP.get(key);
        if (lastShown != null && (now - lastShown) < cooldownMs) return;
        COOLDOWN_MAP.put(key, now);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Component title;
        Component subtitle;

        if (type == ParcelType.NATION) {
            // Nation — large title + small subtitle
            title    = buildNationTitle(parcel);
            subtitle = buildSubtitle(parcel);
        } else {
            // Citizen / Player — small subtitle only, empty title
            title    = Component.empty();
            subtitle = buildCitizenTitle(parcel);
        }

        mc.gui.setTimes(
                Config.CLIENT.gui.parcelEntryFadeInTicks.get(),
                Config.CLIENT.gui.parcelEntryStayTicks.get(),
                Config.CLIENT.gui.parcelEntryFadeOutTicks.get()
        );
        mc.gui.setTitle(title);
        mc.gui.setSubtitle(subtitle);
    }

    /**
     * Clears the cooldown map — call on dimension change or player logout so the
     * title fires fresh when entering a familiar parcel after a context change.
     */
    public static void clearCooldowns() {
        COOLDOWN_MAP.clear();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Large title component for Nation parcels. */
    private static Component buildNationTitle(ClientParcel parcel) {
        return Component.literal(parcel.estateName())
                .withStyle(s -> s.withBold(true).withColor(0x00AAFF));
    }

    /** Small subtitle component showing type bullet owner, used for all types. */
    private static Component buildSubtitle(ClientParcel parcel) {
        String typeName = capitalize(parcel.parcelType().name().toLowerCase());
        return Component.literal(typeName + " \u2022 " + parcel.ownerName())
                .withStyle(s -> s.withColor(0xAAAAAA));
    }

    /**
     * Subtitle-only component for Citizen / Player parcels.
     * Shown in the subtitle slot (small font) with the estate name and owner.
     */
    private static Component buildCitizenTitle(ClientParcel parcel) {
        String typeName = capitalize(parcel.parcelType().name().toLowerCase());
        return Component.literal(parcel.estateName() + " \u2022 " + typeName + " \u2022 " + parcel.ownerName())
                .withStyle(s -> s.withColor(0xAAAAAA));
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}