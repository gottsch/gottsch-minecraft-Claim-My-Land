/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * Claim My Land is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Claim My Land is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Claim My Land.  If not, see <http://www.gnu.org/licenses/lgpl>.
 *
 */

package mod.gottsch.forge.claimmyland.core.cache;

import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client-side cache of the local player's current parcel.
 * Populated by {@link mod.gottsch.forge.claimmyland.core.network.CacheSyncPacket}
 * sent from the server after each BST hit or invalidation.
 *
 * <p>Contains only the fields needed for:</p>
 * <ul>
 *   <li>Instant client-side block protection checks (cancel before server round-trip)</li>
 *   <li>HUD display (parcel name, estate name, owner, type)</li>
 * </ul>
 *
 * <p>All methods are static — there is exactly one local player per client.</p>
 *
 * <p><b>Thread safety:</b> all writes happen on the client main thread via
 * {@code enqueueWork()} in the packet handler. Reads from the render thread
 * are safe because entries are replaced atomically (volatile reference).</p>
 *
 * @author by Mark Gottschling on 3/3/2026
 */
public class ClientParcelCache {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Immutable snapshot of the client's current parcel state.
     * Replaced atomically on each server sync.
     */
    public static class Entry {
        private final UUID parcelId;
        private final UUID estateId;
        private final String parcelName;
        private final String estateName;
        private final String ownerName;
        private final UUID ownerId;
        private final ParcelType parcelType;
        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;
        private final String dimension;

        private Entry(
                UUID parcelId, UUID estateId,
                String parcelName, String estateName, String ownerName,
                UUID ownerId, ParcelType parcelType,
                int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ,
                String dimension) {
            this.parcelId   = parcelId;
            this.estateId   = estateId;
            this.parcelName = parcelName;
            this.estateName = estateName;
            this.ownerName  = ownerName;
            this.ownerId = ownerId;
            this.parcelType = parcelType;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.dimension  = dimension;
        }

        /**
         * Returns true if the given block coords fall within this cached parcel
         * in the given dimension. This is the fast path for client-side protection
         * checks — a simple bounds test, no BST involved.
         */
        public boolean contains(int x, int y, int z, String dim) {
            return dimension.equals(dim)
                    && x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }

        public UUID getParcelId()       { return parcelId; }
        public UUID getEstateId()       { return estateId; }
        public String getParcelName()   { return parcelName; }
        public String getEstateName()   { return estateName; }
        public String getOwnerName()    { return ownerName; }
        public UUID getOwnerId() { return ownerId; }
        public ParcelType getParcelType() { return parcelType; }
        public int getMinX()            { return minX; }
        public int getMinY()            { return minY; }
        public int getMinZ()            { return minZ; }
        public int getMaxX()            { return maxX; }
        public int getMaxY()            { return maxY; }
        public int getMaxZ()            { return maxZ; }
        public String getDimension()    { return dimension; }

        @Override
        public String toString() {
            return String.format("ClientParcelCache.Entry{parcel='%s' [%s], estate='%s', owner='%s', type=%s, dim=%s}",
                    parcelName, parcelId, estateName, ownerName, parcelType, dimension);
        }
    }

    // Volatile so the render thread always sees the latest write from the
    // main thread without needing synchronization.
    @Nullable
    private static volatile Entry current = null;

    // Singleton — no instances
    private ClientParcelCache() {}

    // -------------------------------------------------------------------------
    // Write (called from CacheSyncPacket.handle() on the client main thread)
    // -------------------------------------------------------------------------

    /**
     * Updates the cache with a new parcel entry received from the server.
     */
    public static void update(
            UUID parcelId, UUID estateId,
            String parcelName, String estateName, String ownerName,
            UUID ownerId, ParcelType parcelType,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            String dimension) {
        current = new Entry(
                parcelId, estateId,
                parcelName, estateName, ownerName,
                ownerId, parcelType,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                dimension
        );
        LOGGER.debug("ClientParcelCache updated -> {}", current);
    }

    /**
     * Clears the cache — the player is now in wilderness.
     * Called when the server sends a null-parcel sync packet.
     */
    public static void setWilderness() {
        current = null;
        LOGGER.debug("ClientParcelCache cleared (wilderness)");
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns the current cache entry, or null if the player is in wilderness
     * or the cache has not yet been populated.
     */
    @Nullable
    public static Entry get() {
        return current;
    }

    /**
     * Returns true if the player is currently inside a claimed parcel
     * according to the last server sync.
     */
    public static boolean isInParcel() {
        return current != null;
    }

    /**
     * Fast protection check — returns true if the given block coords are
     * inside the cached parcel in the given dimension.
     *
     * Returns false if the cache is empty (wilderness) or the coords are
     * outside the cached parcel bounds. The caller should forward to the
     * server for an authoritative check on false.
     */
    public static boolean isProtected(int x, int y, int z, String dimension) {
        Entry entry = current; // local copy — safe across volatile read
        return entry != null && entry.contains(x, y, z, dimension);
    }

    /**
     * Returns true if the cache is empty (wilderness or not yet synced).
     */
    public static boolean isWilderness() {
        return current == null;
    }
}
