/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.gottschcore.bst.CoordsInterval;
import mod.gottsch.forge.gottschcore.bst.CoordsIntervalTree;
import mod.gottsch.forge.gottschcore.bst.IInterval;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;

import java.util.*;
import java.util.function.Predicate;

/**
 * Parcel Registry that resides on the client side.
 * WHY DID I WANT THIS? Probably for something of a cache for the HUD.
 * @author by Mark Gottschling on 3/11/2025
 */
public class ClientParcelRegistry {
    /*
     * interval binary spanning tree. main data structure for searchable areas in 3 dimensions.
     * note - this bst does not contain parcels but only area (min coords -> max coords) of the parcel and the id of the owner.
     */
    private static final CoordsIntervalTree<UUID> TREE = new CoordsIntervalTree<UUID>();

    /*
     * map of parcels by coords. main storage of parcels.
     * note - the min coords are used as the key.
     */
    private static final Map<ICoords, Parcel> PARCELS_BY_COORDS = new HashMap<>();

    /*
     * the game time was the registry was last updated
     */
    private long lastGameTime;

    private ClientParcelRegistry() {}

    public static synchronized void clear() {
        TREE.clear();
        PARCELS_BY_COORDS.clear();
    }

    /**
     * add the parcel to the registries/maps
     * @param parcel
     * @return
     */
    public static Optional<Parcel> add(Parcel parcel) {
        ClaimMyLand.LOGGER.debug("adding parcel to client registry -> {}", parcel);

        // add to parcels by coords
        PARCELS_BY_COORDS.put(parcel.getMinCoords(), parcel);

        // add to BST
        Box box = new Box(parcel.getMinCoords(), parcel.getMaxCoords());
        IInterval<UUID> interval = TREE.insert(new CoordsInterval<UUID>(parcel.getMinCoords(), parcel.getMaxCoords(), parcel.getOwnerId()));

        return interval != null ? Optional.of(parcel) : Optional.empty();
    }

    /**
     * removes a parcel from the registries/maps
     * @param parcel
     */
    public static void removeParcel(Parcel parcel) {
        // remove from the TREE
        TREE.delete(new CoordsInterval<>(new CoordsInterval<UUID>(parcel.getMinCoords(), parcel.getMaxCoords(), parcel.getOwnerId())));

        // remove from coords
        PARCELS_BY_COORDS.remove(parcel.getMinCoords());
    }

    /**
     * returns a parcel by id
     * @param id
     * @return
     */
    public static Optional<Parcel> findByParcelId(UUID id) {
        List<Parcel> parcels = new ArrayList<>(1);
        for (Parcel parcel : PARCELS_BY_COORDS.values()) {
            if (parcel.getId().equals(id)) {
                parcels.add(parcel);
                break;
            }
        }
        return parcels.isEmpty() ? Optional.empty() : Optional.of(parcels.get(0));
    }

    /**
     * returns a parcel by a predicate
     */
    public static List<Parcel> findByPredicate(Predicate<Parcel> predicate) {
        List<Parcel> parcels = new ArrayList<>();
        PARCELS_BY_COORDS.values().forEach(parcel -> {
            if (predicate.test(parcel)) {
                parcels.add(parcel);
            }
        });
        return parcels;
    }

    /**
     * find()/findBuffer() variants are slower versions of findRaw() since it requires looking up the parcel from the internal map.
     * @param coords
     * @return
     */
    public static List<Parcel> find(ICoords coords) {
        return find(coords, coords);
    }

    public static List<Parcel> find(Box box) {
        return find(box.getMinCoords(), box.getMaxCoords());
    }

    public static List<Parcel> find(ICoords coords1, ICoords coords2) {
        return find(coords1, coords2, false);
    }

    public static List<Parcel> find(ICoords coords1, ICoords coords2, boolean findFast) {
        return find(coords1, coords2, findFast, true);
    }

    public static List<Parcel> find(ICoords coords1, ICoords coords2, boolean findFast, boolean includeBorder) {
        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, findFast, includeBorder);
        return getAsParcels(intervals);
    }

    /**
     * returns a parcel list from the given interval list
     * @param intervals
     * @return
     */
    private static List<Parcel> getAsParcels(List<IInterval<UUID>> intervals) {
        List<Parcel> parcels = new ArrayList<>();
        intervals.forEach(i -> {
            // find the parcel from the map
            Parcel p = PARCELS_BY_COORDS.get(((CoordsInterval<UUID>)i).getCoords1());
            if (p != null) {
                parcels.add(p);
            }
        });
        return parcels;
    }

    public static List<Box> findBoxes(ICoords coords) {
        return findBoxes(coords, coords);
    }

    public static List<Box> findBoxes(Box box) {
        return findBoxes(box.getMinCoords(), box.getMaxCoords());
    }

    public static List<Box> findBoxes(ICoords coords1, ICoords coords2) {
        return findBoxes(coords1, coords2, false);
    }

    public static List<Box> findBoxes(ICoords coords1, ICoords coords2, boolean findFast) {
        return findBoxes(coords1, coords2, findFast, true);
    }

    /**
     * returns a box list within the given coords
     * @param coords1
     * @param coords2
     * @param findFast
     * @param includeBorder
     * @return
     */
    public static List<Box> findBoxes(ICoords coords1, ICoords coords2, boolean findFast, boolean includeBorder) {
        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, findFast, includeBorder);
        List<Box> boxes = new ArrayList<>();

        // need to check against the PARCELS_BY_COORDS map to ensure it hasn't been deleted.
        intervals.forEach(i -> {
            // find the parcel from the map
            Parcel p = PARCELS_BY_COORDS.get(((CoordsInterval<UUID>)i).getCoords1());
            if (p != null) {
                boxes.add(new Box(((CoordsInterval<UUID>)i).getCoords1(), ((CoordsInterval<UUID>)i).getCoords2()));
            }
        });

        return boxes;
    }

    /**
     * the findRaw() interrogates the interval tree directly.
     * this is the fastest search as it does not have to convert to any other object.
     * @param coords1
     * @param coords2
     * @param findFast
     * @param includeBorder
     * @return
     */
    private static List<IInterval<UUID>> findRaw(ICoords coords1, ICoords coords2, boolean findFast, boolean includeBorder) {
        return TREE.getOverlapping(TREE.getRoot(), new CoordsInterval<UUID>(coords1, coords2), findFast, includeBorder);
    }

    public static boolean intersectsParcel(ICoords coords) {
        return intersectsParcel(coords, coords);
    }

    public static boolean intersectsParcel(ICoords coords1, ICoords coords2) {
        return intersectsParcel(coords1, coords2, true);
    }

    /**
     * Used to determine if the provided area intersects with a parcel
     * @param coords1
     * @param coords2
     * @param includeBorders
     * @return
     */
    public static boolean intersectsParcel(ICoords coords1, ICoords coords2, boolean includeBorders) {
        List<Box> parcels = findBoxes(coords1, coords2, true, includeBorders);
        return !parcels.isEmpty();
    }

    /**
     * returns the parcel with the least area of all parcels at the given coords
     * @param coords
     * @return
     */
    public static Optional<Parcel> findLeastSignificant(ICoords coords) {
        return findLeastSignificant(ParcelRegistry.find(coords));
    }

    public static Optional<Parcel> findLeastSignificant(List<Parcel> parcels) {
        Parcel parcel = null;
        if (parcels.isEmpty()) {
            return Optional.empty();
        }
        else if (parcels.size() == 1) {
            parcel = parcels.get(0);
        } else {
            parcel = parcels.get(0);
            for (Parcel p : parcels) {
                if (p != parcel) {
                    if (p.getArea() < parcel.getArea()) {
                        parcel = p;
                    }
                }
            }
        }
        return Optional.ofNullable(parcel);
    }

    /**
     * not case-sensitive
     * @param name
     * @return
     */
    public static Optional<Parcel> findByName(String name) {
        return PARCELS_BY_COORDS.values().stream().filter(p -> p.getName().equalsIgnoreCase(name)).findFirst();
    }
}
