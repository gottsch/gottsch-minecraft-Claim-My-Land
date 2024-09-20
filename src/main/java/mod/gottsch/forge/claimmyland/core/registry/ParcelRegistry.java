/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
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


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.bst.CoordsInterval;
import mod.gottsch.forge.gottschcore.bst.CoordsIntervalTree;
import mod.gottsch.forge.gottschcore.bst.IInterval;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.function.Predicate;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class ParcelRegistry {
    private static final String PARCELS_KEY = "parcels";

    /*
     * interval binary spanning tree. main data structure for searchable areas in 3 dimensions.
     * note - this bst does not contain parcels but only area (min coords -> max coords) of the parcel and the id of the owner.
     */
    private static final CoordsIntervalTree<UUID> TREE = new CoordsIntervalTree<UUID>();
    /*
     * supporting data structure for buffered parcels ie areas with a "buffer" zone around them.
     */
    private static final CoordsIntervalTree<UUID> BUFFER_TREE = new CoordsIntervalTree<UUID>();
    /*
     * map of parcels by owner id. convenience map
     */
    private static final Map<UUID, List<Parcel>> PARCELS_BY_OWNER = new HashMap<>();
    /*
     * map of parcels by coords. main storage of parcels.
     * note - the min coords are used as the key.
     */
    private static final Map<ICoords, Parcel> PARCELS_BY_COORDS = new HashMap<>();
    /*
     * supporting map of buffered parcels.
     */
    private static final Map<ICoords, Parcel> BUFFER_PARCELS_BY_COORDS = new HashMap<>();

    /*
     * nation caches
     */
    private static final Multimap<UUID, Parcel> NATIONS_BY_ID = ArrayListMultimap.create();


    // singleton
    private ParcelRegistry() {}

//    /**
//     * this is a helper method until added to GottschCore CoordsIntervalTree.
//     * this is not really needed by Protect It. if wanting to make a backup/dump, just use BY_COORDS map.
//     * @param interval
//     * @param intervals
//     */
//    public synchronized void list(IInterval<UUID> interval, List<IInterval<UUID>> intervals) {
//        if (interval == null) {
//            return;
//        }
//
//        if (interval.getLeft() != null) {
//            list(interval.getLeft(), intervals);
//        }
//
//        intervals.add(interval);
//
//        if (interval.getRight() != null) {
//            list(interval.getRight(), intervals);
//        }
//    }
//
//    public synchronized List<IInterval<UUID>> list(IInterval<UUID> interval) {
//        List<IInterval<UUID>> intervals = new ArrayList<>();
//        list(TREE.getRoot(), intervals);
//        return intervals;
//    }

    /**
     *
     */
    public static synchronized void clear() {
        PARCELS_BY_OWNER.clear();
        PARCELS_BY_COORDS.clear();
        BUFFER_PARCELS_BY_COORDS.clear();
        TREE.clear();
        BUFFER_TREE.clear();
        NATIONS_BY_ID.clear();
    }

    /**
     *
     * @param tag
     * @return
     */
    public static synchronized CompoundTag save(CompoundTag tag) {
        ClaimMyLand.LOGGER.debug("saving parcel registry...");

        ListTag list = new ListTag();
        PARCELS_BY_COORDS.forEach((coords, parcel) -> {
            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                ClaimMyLand.LOGGER.debug("registry saving parcel -> {}", parcel);
            }
            CompoundTag parcelTag = new CompoundTag();
            parcel.save(parcelTag);
            list.add(parcelTag);
        });
        ClaimMyLand.LOGGER.debug("saving registry, size -> {}", list.size());
        tag.put(PARCELS_KEY, list);

        return tag;
    }

    /**
     *
     * @param tag
     */
    public static synchronized void load(CompoundTag tag) {
        ClaimMyLand.LOGGER.debug("loading registry...");
        clear();


        if (tag.contains(PARCELS_KEY)) {
            ListTag list = tag.getList(PARCELS_KEY, Tag.TAG_COMPOUND);
            ClaimMyLand.LOGGER.debug("loading registry, size -> {}", list.size());

            list.forEach(element -> {
                ClaimMyLand.LOGGER.debug("processing parcel element...");
                Optional<Parcel> optionalParcel = ParcelFactory.create((CompoundTag)element);
                optionalParcel.ifPresent(parcel -> {
                    // load the parcel
                    parcel.load((CompoundTag) element);

                    if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                        ClaimMyLand.LOGGER.debug("loaded parcel -> {}", parcel);
                    }

                    // add to byCoords map
                    PARCELS_BY_COORDS.put(parcel.getMinCoords(), parcel);

                    // add to byOwner map
                    if (ObjectUtils.isNotEmpty(parcel.getOwnerId())) {
                        List<Parcel> parcelsByOwner = new ArrayList<>();
                        if (!PARCELS_BY_OWNER.containsKey(parcel.getOwnerId())) {
                            // create new list entry
                            PARCELS_BY_OWNER.put(parcel.getOwnerId(), parcelsByOwner);
                        } else {
                            parcelsByOwner = PARCELS_BY_OWNER.get(parcel.getOwnerId());
                        }
                        parcelsByOwner.add(parcel);
                    }

                    // add to tree
                    Box box = new Box(parcel.getMinCoords(), parcel.getMaxCoords());
                    TREE.insert(new CoordsInterval<>(box.getMinCoords(), box.getMaxCoords(), parcel.getOwnerId()));

                    // add to the buffer tree
                    addParcelToBufferTree(parcel);

                    // if nation add to special map
                    if (parcel.getType() == ParcelType.NATION) {
                        NATIONS_BY_ID.put(((NationParcel)parcel).getNationId(), parcel);
                    }
                });
            });
        }
    }

    /**
     * inflates the parcel and adds to the buffer tree, which is a duplicate of the main tree,
     * but uses the inflated coords. the buffer tree is used when determining if 2 parcels
     * meet the buffer criteria.
     * @param parcel
     */
    public static synchronized void addParcelToBufferTree(Parcel parcel) {
        Box inflatedBox = inflateParcelBox(parcel);
        BUFFER_TREE.insert(new CoordsInterval<>(inflatedBox.getMinCoords(), inflatedBox.getMaxCoords(), parcel.getOwnerId()));
        // add buffered parcel to byCoords map
        BUFFER_PARCELS_BY_COORDS.put(inflatedBox.getMinCoords(), parcel);

    }

    public static String toJson() {
        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper(gson);
        return mapper.writeValueAsString(PARCELS_BY_COORDS);
    }

    // TODO
    public List<Parcel> fromJson() {
        return null;
    }

    /**
     * add the parcel to the registries/maps
     * @param parcel
     * @return
     */
    public static Optional<Parcel> add(Parcel parcel) {
        ClaimMyLand.LOGGER.debug("adding parcel to registry -> {}", parcel);

        // add to parcels by owner
        List<Parcel> parcels = null;

        if (!PARCELS_BY_OWNER.containsKey(parcel.getOwnerId())) {
            // create new list entry
            PARCELS_BY_OWNER.put(parcel.getOwnerId(), new ArrayList<>());
        }
        parcels = PARCELS_BY_OWNER.get(parcel.getOwnerId());
        parcels.add(parcel);

        // add to parcels by coords
        PARCELS_BY_COORDS.put(parcel.getMinCoords(), parcel);

        // add to BST
        Box box = new Box(parcel.getMinCoords(), parcel.getMaxCoords());
        IInterval<UUID> interval = TREE.insert(new CoordsInterval<UUID>(parcel.getMinCoords(), parcel.getMaxCoords(), parcel.getOwnerId()));

        // add to the buffer tree
//        Box inflatedBox = inflateParcelBox(parcel);
//        BUFFER_TREE.insert(new CoordsInterval<>(inflatedBox.getMinCoords(), inflatedBox.getMaxCoords(), parcel.getOwnerId()));
//        BUFFER_PARCELS_BY_COORDS.put(inflatedBox.getMinCoords(), parcel);
        addParcelToBufferTree(parcel);

        if (parcel.getType() == ParcelType.NATION) {
            NATIONS_BY_ID.put(((NationParcel)parcel).getNationId(), parcel);
        }

        return interval != null ? Optional.of(parcel) : Optional.empty();
    }

    /**
     * removes a parcel from the registries/maps
     * @param parcel
     */
    public static void removeParcel(Parcel parcel) {
        // delete from PARCELS registries
        List<Parcel> parcels = PARCELS_BY_OWNER.get(parcel.getOwnerId());
        if (!parcels.isEmpty()) {
            parcels.removeIf(p -> p.getId().equals(parcel.getId()));
        }
        PARCELS_BY_COORDS.remove(parcel.getMinCoords());

        // remove from buffer map
        Box inflatedBox = inflateParcelBox(parcel);
        BUFFER_PARCELS_BY_COORDS.remove(inflatedBox.getMinCoords());

        // if nation remove from special map/registry
        removeFromNationsRegistry(parcel);
    }

    private static void removeFromNationsRegistry(Parcel parcel) {
        if (parcel.getType() == ParcelType.NATION) {
            NATIONS_BY_ID.remove(((NationParcel)parcel).getNationId(), parcel);
        }
    }

    /**
     * removes all parcels by player
     * @param ownerId
     */
    public static void removeParcel(UUID ownerId) {
        //delete from PARCELS registries
        List<Parcel> parcels = PARCELS_BY_OWNER.get(ownerId);
        if (!parcels.isEmpty()) {
            for (Parcel p : parcels) {
                PARCELS_BY_COORDS.remove(p.getMinCoords());
                // remove from buffer map
                Box inflatedBox = inflateParcelBox(p);
                BUFFER_PARCELS_BY_COORDS.remove(inflatedBox.getMinCoords());

                removeFromNationsRegistry(p);
//                if (p.getType() == ParcelType.NATION) {
//                    NATIONS_BY_ID.remove(((NationParcel)p).getNationId(), p);
//                }
            }
        }
        PARCELS_BY_OWNER.remove(ownerId);
    }

    /**
     * inflates the parcels dimensions by the config buffer radius setting
     * @param parcel
     * @return
     */
    public static Box inflateParcelBox(final Parcel parcel) {
        return switch(parcel.getType()) {
            case PLAYER, CITIZEN, CITIZEN_ZONE -> ModUtil.inflate(parcel.getBox(), Config.SERVER.general.parcelBufferRadius.get());
            case NATION -> ModUtil.inflate(parcel.getBox(), Config.SERVER.general.nationParcelBufferRadius.get());
          };
    }

    /**
     * retrieves a list of all parcels by player/owner
     * @param id
     * @return
     */
    public static List<Parcel> findByOwner(UUID id) {
        List<Parcel> parcels = PARCELS_BY_OWNER.get(id);
        if (parcels == null) {
            parcels = new ArrayList<>();
        }
        return parcels;
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
    public static List<Parcel> findByParcel(Predicate<Parcel> predicate) {
        List<Parcel> parcels = new ArrayList<>();
        PARCELS_BY_COORDS.values().forEach(parcel -> {
            if (predicate.test(parcel)) {
                parcels.add(parcel);
            }
        });
        return parcels;
    }

    /**
     * returns a nation parcel by nation id
     * @param nationId
     * @return
     */
    public static List<Parcel> findByNationId(UUID nationId) {
        List<Parcel> parcels = new ArrayList<>(1);
        for (Parcel parcel : PARCELS_BY_COORDS.values()) {
            if (parcel.getId().equals(nationId)) {
                parcels.add(parcel);
                break;
            }
        }
        return parcels;
    }

    /**
     * returns a nation parcel by the player/owner
     * @param id
     * @return
     */
    public static List<Parcel> findNationsByOwner(UUID id) {
        return findByOwner(id).stream().filter(p -> (p instanceof NationParcel)).toList();
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
        return getParcels(intervals);
    }

    public static List<Parcel> findBuffer(ICoords coords) {
        return findBuffer(coords, coords);
    }

    public static List<Parcel> findBuffer(Box box) {
        return findBuffer(box.getMinCoords(), box.getMaxCoords());
    }

    public static List<Parcel> findBuffer(ICoords coords1, ICoords coords2) {
        return findBuffer(coords1, coords2, false);
    }

    public static List<Parcel> findBuffer(ICoords coords1, ICoords coords2, boolean findFast) {
        return findBuffer(coords1, coords2, findFast, true);
    }

    public static List<Parcel> findBuffer(ICoords coords1, ICoords coords2, boolean findFast, boolean includeBorder) {
        List<IInterval<UUID>> intervals = findBufferRaw(coords1, coords2, findFast, includeBorder);
        return getBufferParcels(intervals);
    }

    /**
     * returns a parcel list from the given interval list
     * @param intervals
     * @return
     */
    private static List<Parcel> getParcels(List<IInterval<UUID>> intervals) {
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

    /**
     * returns a buffer parcel list from the given interval list
     * @param intervals
     * @return
     */
    public static List<Parcel> getBufferParcels(List<IInterval<UUID>> intervals) {
        List<Parcel> parcels = new ArrayList<>();
        intervals.forEach(i -> {
            // find the parcel from the map
            Parcel p = BUFFER_PARCELS_BY_COORDS.get(((CoordsInterval<UUID>)i).getCoords1());
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

    private static List<IInterval<UUID>> findBufferRaw(ICoords coords1, ICoords coords2, boolean findFast, boolean includeBorder) {
        return BUFFER_TREE.getOverlapping(BUFFER_TREE.getRoot(), new CoordsInterval<UUID>(coords1, coords2), findFast, includeBorder);
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
     * determines if a player has access/permission to execute event at coords ex break block, place block etc
     * @param coords
     * @param entityId
     * @return
     */
    public static boolean hasAccess(ICoords coords, UUID entityId) {
        return hasAccess(coords, coords, entityId, ItemStack.EMPTY);
    }

    public static boolean hasAccess(ICoords coords, UUID entityId, ItemStack stack) {
        return hasAccess(coords, coords, entityId, stack);
    }

    public static boolean hasAccess(ICoords coords1, ICoords coords2, UUID entityId, ItemStack itemStack) {
        // this is the fastest lookup
        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, false, true );
        if (!intervals.isEmpty()) {
            Parcel parcel;
            // convert to parcels
            List<Parcel> parcels = getParcels(intervals);

            if (parcels.isEmpty()) {
                return true;
            }
            if (intervals.size() > 1) {
                // find the least significant parcel
                Optional<Parcel> parcelOptional = findLeastSignificant(parcels);
                if (parcelOptional.isPresent()) {
                    parcel = parcelOptional.get();
                } else {
                    // TODO add chat warning
                    // TODO add log warning
                    // TODO maybe do something like labelling as abandoned and has a timer before it is removed from registry.
                    // this is a case where the interval still exists but the parcel has been removed
                    TREE.delete(intervals.get(0));
                    return true;
                }
            } else {
                parcel = parcels.get(0);
            }

            // check player's access
            return itemStack != ItemStack.EMPTY ? parcel.grantsAccess(entityId, itemStack) : parcel.grantsAccess(entityId);
        }
        return true;
    }

    /**
     * returns the parcel with the least area all parcels at the given coords
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

//    @Deprecated
//    public static boolean isProtectedAgainst(ICoords coords, UUID entityId) {
//        return isProtectedAgainst(coords, coords, entityId);
//    }

//    @Deprecated
//    public static boolean isProtectedAgainst(ICoords coords1, ICoords coords2, UUID entityId) {
//        List<IInterval<UUID>> intervals = TREE.getOverlapping(TREE.getRoot(), new CoordsInterval<>(coords1, coords2));
//        if (intervals.isEmpty()) {
//            return false;
//        }
//        else {
//
//            // interrogate each interval to determine if the uuid is the owner
//            for (IInterval<UUID> interval : intervals) {
//                // short circuit if owner or no owner
//                ClaimMyLand.LOGGER.debug("isProtectedAgainst interval data -> {}", interval.getData());
//                if (interval.getData() == null || interval.getData().equals(entityId)) {
//                    break;
//                }
////                if (p.getData() == null) {
////                    break; // was true. but if no owner, it is not protected against you? how does this work with CitizenParcels
////                }
//
//                // get the parcel
//                CoordsInterval<UUID> coordsInterval = (CoordsInterval<UUID>)interval;
//                Parcel parcel = PARCELS_BY_COORDS.get(coordsInterval.getCoords1());
//                ClaimMyLand.LOGGER.debug("isProtectedAgainst.parcelsByCoords -> {}, parcel -> {}", parcel.getMinCoords(), parcel);
//
//
//                // cycle through whitelist
////                if (!parcel.getWhitelist().isEmpty()) {
////                    ClaimMyLand.LOGGER.debug("isProtectedAgainst whitelist is not null");
////
////                    for (PlayerData id : parcel.getWhitelist()) {
////                        ClaimMyLand.LOGGER.debug("isProtectedAgainst compare whitelist id -> {} to uuid -> {}", id.getUuid(), uuid);
////                        if (id.getUuid().equalsIgnoreCase(uuid)) {
////                            return false;
////                        }
////                    }
////                }
//                return true;
//            }
//        }
//        return false;
//    }

    public static List<UUID> getOwnerIds() {
        return PARCELS_BY_OWNER.keySet().stream().toList();
    }

    public static List<Parcel> getNations() {
        List<Parcel> parcels = new ArrayList<>();
        NATIONS_BY_ID.entries().forEach(e -> parcels.add(e.getValue()));
        return parcels;
    }
}
