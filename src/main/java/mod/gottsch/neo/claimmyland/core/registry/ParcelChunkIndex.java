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

package mod.gottsch.neo.claimmyland.core.registry;

import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.gottschcore.spatial.ICoords;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk-based pre-filter index for {@link ParcelRegistry}.
 *
 * Sits in front of the CoordsIntervalTree (BST) and answers the question
 * "is any parcel registered in this chunk?" in O(1) time via a hash lookup.
 * Event handlers call {@link #isChunkClaimed(int, int)} first; if it returns
 * false the BST is never touched, eliminating the vast majority of queries
 * in a world where most chunks are unclaimed.
 *
 * MEGA-PARCEL HANDLING
 * Parcels that span more than MEGA_PARCEL_CHUNK_THRESHOLD total chunks in the
 * XZ plane (e.g. large Nation parcels) are stored in MEGA_PARCELS rather than
 * being indexed per-chunk. This prevents "chunk explosion" — a 512x512 Nation
 * would otherwise produce 1,024 map entries. isChunkClaimed() checks the chunk
 * map first, then falls back to a linear scan of MEGA_PARCELS (acceptable
 * because mega-parcels are rare and the scan terminates on the first hit).
 *
 * THREAD SAFETY
 * CHUNK_TO_PARCEL_IDS uses ConcurrentHashMap with newKeySet() inner sets for
 * lock-free reads during event handling (which may occur off the main server
 * thread). MEGA_PARCELS is a synchronized LinkedHashSet.
 *
 * DIMENSION SUPPORT
 * v2.1 defaults to "minecraft:overworld" — the same scope as the rest of the
 * event system. ChunkKey is dimension-aware, so extending to multi-dimension
 * in v2.2 requires only passing the actual dimension string through call sites
 * rather than changing any data structures here.
 *
 * @since 2.1.0
 */

/**
 * @author by Mark Gottschling on 3/3/2026
 */
public class ParcelChunkIndex {

    // -------------------------------------------------------------------------
    // constants
    // -------------------------------------------------------------------------

    /**
     * Parcels whose XZ footprint spans more total chunks than this threshold are
     * stored in MEGA_PARCELS rather than the per-chunk index.
     * 50 chunks ≈ an 800x800 block region — well above any typical citizen or
     * zone parcel, but within the range of large Nation parcels.
     */
    public static final int MEGA_PARCEL_CHUNK_THRESHOLD = 50;

    // -------------------------------------------------------------------------
    // internal state
    // -------------------------------------------------------------------------

    /**
     * Primary index: ChunkKey -> set of parcel IDs overlapping that chunk.
     * ConcurrentHashMap with newKeySet() inner sets for lock-free reads.
     */
    private static final ConcurrentHashMap<ChunkKey, Set<UUID>> CHUNK_TO_PARCEL_IDS =
            new ConcurrentHashMap<>();

    /**
     * Mega-parcel store: parcels too large to index per-chunk.
     * Scanned linearly on every isChunkClaimed() call, but expected to contain
     * at most a handful of entries on any typical server.
     */
    private static final Set<Parcel> MEGA_PARCELS =
            Collections.synchronizedSet(new LinkedHashSet<>());

    // -------------------------------------------------------------------------
    // registration
    // -------------------------------------------------------------------------

    /**
     * Registers parcel in the appropriate index structure.
     * Called from ParcelRegistry.registerChunk(Parcel).
     *
     * Parcels whose XZ footprint exceeds MEGA_PARCEL_CHUNK_THRESHOLD total
     * chunks go into MEGA_PARCELS; all others are indexed per-chunk.
     */
    public static void index(Parcel parcel, String dimension) {
        Objects.requireNonNull(parcel, "parcel must not be null");

        ICoords min = parcel.getMinCoords();
        ICoords max = parcel.getMaxCoords();

        int minCX = min.getX() >> 4;
        int maxCX = max.getX() >> 4;
        int minCZ = min.getZ() >> 4;
        int maxCZ = max.getZ() >> 4;

        // Use long to avoid int overflow on extreme coordinates
        long chunkSpan = (long)(maxCX - minCX + 1) * (maxCZ - minCZ + 1);

        if (chunkSpan > MEGA_PARCEL_CHUNK_THRESHOLD) {
            MEGA_PARCELS.add(parcel);
        } else {
            UUID id = parcel.getId();
            for (int cx = minCX; cx <= maxCX; cx++) {
                for (int cz = minCZ; cz <= maxCZ; cz++) {
                    ChunkKey key = new ChunkKey(dimension, cx, cz);
                    CHUNK_TO_PARCEL_IDS
                            .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                            .add(id);
                }
            }
        }
    }

    /**
     * Removes parcel from whichever index structure it was added to.
     * Called from ParcelRegistry during parcel demolition / unregistration.
     */
    public static void unindex(Parcel parcel, String dimension) {
        Objects.requireNonNull(parcel, "parcel must not be null");

        // Fast path: try mega-parcel set first (O(1) average via equals/hashCode)
        if (MEGA_PARCELS.remove(parcel)) {
            return;
        }

        ICoords min = parcel.getMinCoords();
        ICoords max = parcel.getMaxCoords();

        int minCX = min.getX() >> 4;
        int maxCX = max.getX() >> 4;
        int minCZ = min.getZ() >> 4;
        int maxCZ = max.getZ() >> 4;

        UUID id = parcel.getId();
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                ChunkKey key = new ChunkKey(dimension, cx, cz);
                Set<UUID> ids = CHUNK_TO_PARCEL_IDS.get(key);
                if (ids != null) {
                    ids.remove(id);
                    // Prune empty sets to keep the map lean
                    if (ids.isEmpty()) {
                        CHUNK_TO_PARCEL_IDS.remove(key, ids);
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // querying
    // -------------------------------------------------------------------------

    /**
     * Fast O(1) check: is any parcel registered in the chunk containing
     * block (blockX, blockZ)?
     *
     * This is the primary hot-path method called from every block-interaction
     * event handler. It performs one hash lookup against the chunk map, then —
     * only on a miss — does a linear scan over the (tiny) mega-parcel set.
     */
    public static boolean isChunkClaimed(int blockX, int blockZ, String dimension) {
        ChunkKey key = new ChunkKey(dimension, blockX >> 4, blockZ >> 4);
        Set<UUID> ids = CHUNK_TO_PARCEL_IDS.get(key);
        if (ids != null && !ids.isEmpty()) {
            return true;
        }
        return isCoveredByMegaParcel(blockX, blockZ);
    }

    /**
     * Returns an immutable snapshot of the IDs of all parcels registered in the
     * chunk containing block (blockX, blockZ), including any mega-parcels that
     * cover this block. Intended for debugging; not for the hot path.
     */
    public static Set<UUID> getParcelIdsInChunk(int blockX, int blockZ, String dimension) {
        Set<UUID> result = new HashSet<>();

        ChunkKey key = new ChunkKey(dimension, blockX >> 4, blockZ >> 4);
        Set<UUID> ids = CHUNK_TO_PARCEL_IDS.get(key);
        if (ids != null) {
            result.addAll(ids);
        }

        synchronized (MEGA_PARCELS) {
            for (Parcel mp : MEGA_PARCELS) {
                ICoords min = mp.getMinCoords();
                ICoords max = mp.getMaxCoords();
                if (blockX >= min.getX() && blockX <= max.getX()
                        && blockZ >= min.getZ() && blockZ <= max.getZ()) {
                    result.add(mp.getId());
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    // -------------------------------------------------------------------------
    // maintenance
    // -------------------------------------------------------------------------

    /**
     * Clears both the chunk index and the mega-parcel set.
     * Must be called from ParcelRegistry.clear() on world unload.
     */
    public static void clear() {
        CHUNK_TO_PARCEL_IDS.clear();
        MEGA_PARCELS.clear();
    }

    /**
     * Returns a diagnostic summary. Safe to call from admin commands.
     */
    public static String debugSummary() {
        return String.format(
                "ParcelChunkIndex[chunksIndexed=%d, megaParcels=%d]",
                CHUNK_TO_PARCEL_IDS.size(),
                MEGA_PARCELS.size()
        );
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    private static boolean isCoveredByMegaParcel(int blockX, int blockZ) {
        synchronized (MEGA_PARCELS) {
            for (Parcel mp : MEGA_PARCELS) {
                ICoords min = mp.getMinCoords();
                ICoords max = mp.getMaxCoords();
                if (blockX >= min.getX() && blockX <= max.getX()
                        && blockZ >= min.getZ() && blockZ <= max.getZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // ChunkKey record
    // -------------------------------------------------------------------------

    /**
     * Immutable, dimension-aware chunk coordinate key.
     *
     * Using a record instead of a formatted String eliminates per-lookup string
     * allocation and provides compiler-generated hashCode/equals. The dimension
     * field is included so this key is already structured for multi-dimension
     * support in v2.2 — call sites only need to supply the real dimension string
     * instead of DEFAULT_DIMENSION.
     *
     * @param dimension Minecraft dimension resource location (e.g. "minecraft:overworld")
     * @param chunkX    chunk X coordinate (blockX >> 4)
     * @param chunkZ    chunk Z coordinate (blockZ >> 4)
     */
    public record ChunkKey(String dimension, int chunkX, int chunkZ) {
        public ChunkKey {
            Objects.requireNonNull(dimension, "dimension must not be null");
        }
    }
}