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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * server-side per-player cache of the last accessed parcel and its owning estate.
 * <p>
 * sits in front of the BST in hasAccess() / hasInteractAccess(). A cache hit
 * (player queries a block inside their cached parcel) costs a simple bounds check
 * (~2-5ns) instead of a full BST traversal (~20-50ns).
 * </p>
 *
 * <p><b>Invalidation triggers:</b></p>
 * <ul>
 *   <li>Player logs out → {@link #invalidatePlayer(UUID)}</li>
 *   <li>Player changes dimension → {@link #invalidatePlayer(UUID)}</li>
 *   <li>Parcel demolished → {@link #invalidateByParcel(UUID)}</li>
 *   <li>Parcel bounds modified → {@link #invalidateByParcel(UUID)}</li>
 *   <li>Parcel owner changed → {@link #invalidateByParcel(UUID)}</li>
 *   <li>Estate deleted → {@link #invalidateByEstate(UUID)}</li>
 * </ul>
 *
 * @author by Mark Gottschling on 3/3/2026
 */
public class ParcelRegionCache {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * a single player's cached region state.
     */
    public static class CacheEntry {
        private final Parcel parcel;
        private final long timestamp;

        public CacheEntry(Parcel parcel) {
            this.parcel = parcel;
            this.timestamp = System.currentTimeMillis();
        }

        public Parcel getParcel() { return parcel; }
        /** Convenience — estate is already on the parcel. */
        public Estate getEstate() { return parcel.getEstate(); }
        public long getTimestamp() { return timestamp; }

        /**
         * Returns true if the given coords fall within this entry's parcel bounds.
         * Also validates the dimension string so a player teleporting dimensions
         * never gets a false hit.
         */
        public boolean contains(ICoords coords, String dimension) {
            if (!parcel.getDimension().equals(dimension)) {
                return false;
            }
            ICoords min = parcel.getMinCoords();
            ICoords max = parcel.getMaxCoords();
            int x = coords.getX(), y = coords.getY(), z = coords.getZ();
            return x >= min.getX() && x <= max.getX()
                    && y >= min.getY() && y <= max.getY()
                    && z >= min.getZ() && z <= max.getZ();
        }

        @Override
        public String toString() {
            return String.format("CacheEntry{parcel=%s, estate=%s, age=%dms}",
                    parcel.getId(), parcel.getEstate().getId(), System.currentTimeMillis() - timestamp);
        }
    }

    // ConcurrentHashMap so reads from the server tick thread and writes from
    // event handlers don't need explicit locking for the map itself.
    // Individual CacheEntry objects are immutable (replaced, never mutated).
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------------------
    // read
    // ---------------------------------------------------------------------------

    /**
     * Returns the cached entry for this player, or empty if no entry exists.
     * Callers should follow up with {@link CacheEntry#contains(ICoords, String)}
     * to confirm the queried coords are actually inside the cached parcel.
     */
    public Optional<CacheEntry> get(UUID playerId) {
        return Optional.ofNullable(cache.get(playerId));
    }

    /**
     * Convenience: returns the cached entry only if it contains the given coords
     * in the given dimension. Returns empty on any miss (no entry, wrong dimension,
     * or coords outside parcel bounds).
     *
     * <p>This is the method to call at the top of hasAccess() / hasInteractAccess().</p>
     */
    public Optional<CacheEntry> getIfContains(UUID playerId, ICoords coords, String dimension) {
        CacheEntry entry = cache.get(playerId);
        if (entry == null) return Optional.empty();
        return entry.contains(coords, dimension) ? Optional.of(entry) : Optional.empty();
    }

    // ---------------------------------------------------------------------------
    // write
    // ---------------------------------------------------------------------------

    /**
     * Updates (or creates) the cache entry for a player after a successful BST lookup.
     *
     * @param playerId the player whose cache to update
     * @param parcel   the parcel returned by the BST query
     */
    public void update(UUID playerId, Parcel parcel) {
        cache.put(playerId, new CacheEntry(parcel));
        LOGGER.debug("Cache updated for player {}: parcel={}, estate={}",
                playerId, parcel.getId(), parcel.getEstate().getId());
    }

    // ---------------------------------------------------------------------------
    // invalidation
    // ---------------------------------------------------------------------------

    /**
     * Removes a single player's cache entry.
     * Call on: player logout, player dimension change.
     */
    public void invalidatePlayer(UUID playerId) {
        CacheEntry removed = cache.remove(playerId);
        if (removed != null) {
            LOGGER.debug("Cache invalidated for player {}", playerId);
        }
    }

    /**
     * Removes all player entries whose cached parcel matches the given parcel ID.
     * Call on: parcel demolished, parcel bounds modified, parcel owner changed.
     *
     * @param parcelId the ID of the parcel that changed
     * @return the set of player UUIDs whose entries were invalidated
     */
    public Set<UUID> invalidateByParcel(UUID parcelId) {
        Set<UUID> affected = new HashSet<>();
        cache.entrySet().removeIf(e -> {
            if (e.getValue().getParcel().getId().equals(parcelId)) {
                affected.add(e.getKey());
                return true;
            }
            return false;
        });
        if (!affected.isEmpty()) {
            LOGGER.debug("Cache invalidated {} player(s) for parcel {}", affected.size(), parcelId);
        }
        return Collections.unmodifiableSet(affected);
    }

    /**
     * Removes all player entries whose cached estate matches the given estate ID.
     * Call on: estate deleted (all parcels in the estate are implicitly gone).
     *
     * @param estateId the ID of the estate that was deleted
     * @return the set of player UUIDs whose entries were invalidated
     */
    public Set<UUID> invalidateByEstate(UUID estateId) {
        Set<UUID> affected = new HashSet<>();
        cache.entrySet().removeIf(e -> {
            if (e.getValue().getEstate().getId().equals(estateId)) {
                affected.add(e.getKey());
                return true;
            }
            return false;
        });
        if (!affected.isEmpty()) {
            LOGGER.debug("Cache invalidated {} player(s) for estate {}", affected.size(), estateId);
        }
        return Collections.unmodifiableSet(affected);
    }

    /**
     * Clears all entries. Call on world unload / server stop.
     */
    public void invalidateAll() {
        int size = cache.size();
        cache.clear();
        LOGGER.debug("Cache fully cleared ({} entries removed)", size);
    }

    // ---------------------------------------------------------------------------
    // Diagnostics
    // ---------------------------------------------------------------------------

    /** Returns the number of currently cached player entries. */
    public int size() {
        return cache.size();
    }

    /** Returns true if the given player has a cache entry (regardless of validity). */
    public boolean isCached(UUID playerId) {
        return cache.containsKey(playerId);
    }

    /**
     * Returns a snapshot of all current entries for diagnostics / admin commands.
     * The returned map is a copy — mutations do not affect the cache.
     */
    public Map<UUID, CacheEntry> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(cache));
    }
}