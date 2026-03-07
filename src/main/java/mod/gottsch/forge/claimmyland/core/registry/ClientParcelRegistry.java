/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * All rights reserved.
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
 */
package mod.gottsch.forge.claimmyland.core.registry;

import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side registry of all known parcels, populated by
 * {@link mod.gottsch.forge.claimmyland.core.network.SyncParcelPacket} and
 * {@link mod.gottsch.forge.claimmyland.core.network.SyncAllParcelsPacket}.
 *
 * <p>Used by the HUD overlay for look-based parcel queries. Intentionally
 * simple — flat list with linear scan. The client holds far fewer parcels
 * than the server and queries are infrequent (once per render frame at most),
 * so a BST would add complexity without meaningful benefit.</p>
 *
 * <p><b>Thread safety:</b> uses {@link CopyOnWriteArrayList} so the render
 * thread can iterate safely while the network thread writes.</p>
 *
 * @author Mark Gottschling on 3/3/2026
 */
public class ClientParcelRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    // CopyOnWriteArrayList — reads (render thread) never block,
    // writes (network/main thread) are infrequent.
    private static final CopyOnWriteArrayList<ClientParcel> PARCELS = new CopyOnWriteArrayList<>();

    // Singleton — no instances
    private ClientParcelRegistry() {}

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Adds or replaces a parcel. If a parcel with the same ID already exists
     * it is replaced — handles re-sync after a parcel is modified.
     */
    public static void register(ClientParcel parcel) {
        // Remove existing entry with same ID (re-sync / update case)
        PARCELS.removeIf(p -> p.parcelId().equals(parcel.parcelId()));
        PARCELS.add(parcel);
        LOGGER.debug("ClientParcelRegistry: registered parcel '{}' [{}]",
                parcel.parcelName(), parcel.parcelId());
    }

    /**
     * Registers a list of parcels in bulk. Used by SyncAllParcelsPacket on login.
     * Replaces any existing entries with matching IDs.
     */
    public static void registerAll(List<ClientParcel> parcels) {
        parcels.forEach(ClientParcelRegistry::register);
        LOGGER.debug("ClientParcelRegistry: bulk registered {} parcel(s)", parcels.size());
    }

    /**
     * Removes a parcel by ID. Called when the server sends a RemoveParcelPacket.
     */
    public static void unregister(UUID parcelId) {
        boolean removed = PARCELS.removeIf(p -> p.parcelId().equals(parcelId));
        if (removed) {
            LOGGER.debug("ClientParcelRegistry: unregistered parcel [{}]", parcelId);
        }
    }

    /**
     * Clears all parcels. Call on dimension change or disconnect so stale
     * data is not shown on the HUD after a world transition.
     */
    public static void clear() {
        int size = PARCELS.size();
        PARCELS.clear();
        LOGGER.debug("ClientParcelRegistry: cleared ({} parcel(s) removed)", size);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns the parcel at the given block coords in the given dimension,
     * or empty if the coords are in wilderness.
     *
     * <p>Linear scan — acceptable given the expected client-side parcel count
     * and the infrequency of HUD look-based queries.</p>
     *
     * <p>When multiple parcels overlap (e.g. a citizen parcel inside a nation),
     * returns the smallest by volume — matching the server-side
     * {@code findLeastSignificant()} behaviour.</p>
     */
    public static Optional<ClientParcel> findAt(int x, int y, int z, String dimension) {
        ClientParcel result = null;
        long smallestVolume = Long.MAX_VALUE;

        for (ClientParcel parcel : PARCELS) {
            if (parcel.contains(x, y, z, dimension)) {
                long volume = (long)(parcel.maxX() - parcel.minX() + 1)
                        * (parcel.maxY() - parcel.minY() + 1)
                        * (parcel.maxZ() - parcel.minZ() + 1);
                if (volume < smallestVolume) {
                    smallestVolume = volume;
                    result = parcel;
                }
            }
        }
        return Optional.ofNullable(result);
    }

    /**
     * Returns all parcels that contain the given coords in the given dimension.
     * Useful for HUD detail views showing the full containment hierarchy
     * (e.g. citizen parcel inside a nation).
     */
    public static List<ClientParcel> findAllAt(int x, int y, int z, String dimension) {
        List<ClientParcel> results = new ArrayList<>();
        for (ClientParcel parcel : PARCELS) {
            if (parcel.contains(x, y, z, dimension)) {
                results.add(parcel);
            }
        }
        return results;
    }

    /**
     * Returns a parcel by ID, or empty if not found.
     */
    public static Optional<ClientParcel> findById(UUID parcelId) {
        for (ClientParcel parcel : PARCELS) {
            if (parcel.parcelId().equals(parcelId)) {
                return Optional.of(parcel);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns an unmodifiable snapshot of all registered parcels.
     * Safe to iterate from any thread.
     */
    public static List<ClientParcel> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(PARCELS));
    }

    /** Returns the number of registered parcels. */
    public static int size() {
        return PARCELS.size();
    }
}