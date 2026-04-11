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
package mod.gottsch.neo.claimmyland.core.registry;


import com.google.gson.Gson;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.cache.ParcelRegionCache;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.estate.EstateTypeRegistry;
import mod.gottsch.neo.claimmyland.core.estate.NationEstate;
import mod.gottsch.neo.claimmyland.core.network.CMLNetwork;
import mod.gottsch.neo.claimmyland.core.parcel.*;
import mod.gottsch.neo.claimmyland.core.tags.ModTags;
import mod.gottsch.neo.claimmyland.core.util.ModUtil;
import mod.gottsch.neo.claimmyland.core.util.TagHelper;
import mod.gottsch.neo.gottschcore.bst.CoordsInterval;
import mod.gottsch.neo.gottschcore.bst.CoordsIntervalTree;
import mod.gottsch.neo.gottschcore.bst.IInterval;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    private static final CoordsIntervalTree<UUID> TREE = new CoordsIntervalTree<>();
    /*
     * supporting data structure for buffered parcels ie areas with a "buffer" zone around them.
     */
    private static final CoordsIntervalTree<UUID> BUFFER_TREE = new CoordsIntervalTree<>();
    /*
     * map of parcels by owner id. convenience map
     */
    private static final Map<UUID, List<Parcel>> PARCELS_BY_OWNER = new HashMap<>();
    /*
     * map of parcels by friend's id. a friend is a player who is listed in a parcel's friends whitelist.
     */
    private static final Map<UUID, List<Parcel>> PARCELS_BY_FRIENDS = new HashMap<>();
    /*
     * map of parcels by coords. main storage of parcels.
     * note - the min coords are used as the key.
     */
    private static final Map<ICoords, Parcel> PARCELS_BY_COORDS = new HashMap<>();
    /*
     * supporting map of buffered parcels.
     */
    private static final Map<ICoords, Parcel> BUFFER_PARCELS_BY_COORDS = new HashMap<>();

    private static final Map<UUID, Parcel> PARCELS_BY_ID = new HashMap<>();

    private static final Map<UUID, Set<Parcel>> PARCELS_BY_ESTATE_ID = new HashMap<>();

    public static final ParcelRegionCache REGION_CACHE = new ParcelRegionCache();

    // singleton
    private ParcelRegistry() {}

    /**
     *
     */
    public static synchronized void clear() {
        PARCELS_BY_OWNER.clear();
        PARCELS_BY_FRIENDS.clear();
        PARCELS_BY_COORDS.clear();
        BUFFER_PARCELS_BY_COORDS.clear();
        TREE.clear();
        BUFFER_TREE.clear();
        ParcelChunkIndex.clear();
        REGION_CACHE.invalidateAll();
    }

    /**
     * registers a parcel and notifies nearby clients.
     * Use this overload from command and block entity code where ServerLevel
     * and the claiming player are available.
     */
    public static synchronized Optional<Parcel> register(ServerLevel level, Parcel parcel, String ownerName) {
        Optional<Parcel> result = register(parcel);
        result.ifPresent(p -> CMLNetwork.syncParcelToTrackingPlayers(level, p, ownerName));
        return result;
    }

    public static synchronized Optional<Parcel> register(ServerLevel level, Parcel parcel) {
        Optional<Parcel> result = register(parcel);
        result.ifPresent(p -> CMLNetwork.syncParcelToTrackingPlayers(level, p));
        return result;
    }


    /**
     *
     * @param tag
     * @return
     */
    public static synchronized CompoundTag save(CompoundTag tag) {
//        ClaimMyLand.LOGGER.debug("saving parcel registry...");

        ListTag list = new ListTag();
        PARCELS_BY_COORDS.forEach((coords, parcel) -> {
            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                ClaimMyLand.LOGGER.debug("registry saving parcel -> {}", parcel);
            }
            CompoundTag parcelTag = new CompoundTag();
            parcel.save(parcelTag);
            list.add(parcelTag);
        });
//        ClaimMyLand.LOGGER.debug("saving registry, size -> {}", list.size());
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
//            ClaimMyLand.LOGGER.debug("loading registry, size -> {}", list.size());

            list.stream()
                    .map(element -> (CompoundTag) element)
                    .peek(e -> ClaimMyLand.LOGGER.debug("processing parcel element..."))
                    .forEach(e -> {
                        ParcelType type = e.contains(Parcel.TYPE)
                                ? ParcelType.fromString(e.getString(Parcel.TYPE))
                                : ParcelType.NONE;

                        ParcelTypeRegistry.create(type).ifPresent(parcel -> {
                            // load the parcel
                            parcel.load(e);
                            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                                ClaimMyLand.LOGGER.debug("loaded parcel -> {}", parcel);
                            }
                            ParcelRegistry.register(parcel);
                        });
                    });
        }
    }

    /**
     * inserts the inflated buffer box into the buffer BST with the parcel's dimension.
     * NOTE even if buffer size == 0, the parcel must be added to teh buffer tree.
     */
    public static synchronized void registerBuffer(Parcel parcel) {
        Box inflatedBox = parcel.getBufferSize() > 0
                ? inflateParcelBox(parcel)
                : parcel.getBox();

        String dim = effectiveDimension(parcel.getDimension());
        BUFFER_TREE.insert(new CoordsInterval<>(
                inflatedBox.getMinCoords(), inflatedBox.getMaxCoords(),
                parcel.getOwnerId(), dim));
        BUFFER_PARCELS_BY_COORDS.put(inflatedBox.getMinCoords(), parcel);
    }

    /**
     * registers parcel in the chunk pre-filter index.
     * called as part of the full registration sequence in {@link #register}.
     */
    public static synchronized void registerChunk(Parcel parcel) {
        String dimension = parcel.getDimension() != null
                ? parcel.getDimension()
                : "minecraft:overworld";
        ParcelChunkIndex.index(parcel, dimension);
    }

    public static String toJson() {
        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper(gson);
        return mapper.writeValueAsString(PARCELS_BY_COORDS);
    }

    /*
     * determines if parcel name exists within an estate.
     */
    public static boolean hasName(Parcel parcel, String newName) {
        return hasName(parcel.getEstate().findParcels(), parcel, newName);
    }

    public static boolean hasName(Set<Parcel> parcels, Parcel parcel, String newName) {
        return parcels.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(newName));
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
    public static synchronized Optional<Parcel> register(Parcel parcel) {
        ClaimMyLand.LOGGER.debug("adding parcel to registry -> {}", parcel);

        try {
            // add to parcels by coords
            registerCoords(parcel);

            registerOwner(parcel);

            registerFriends(parcel);

            // add to tree
            registerTree(parcel);

            // add to the buffer tree
            registerBuffer(parcel);

            registerChunk(parcel);
            // add to nations map
//            registerNation(parcel);

            //register estate
            EstateRegistry.register(parcel.getEstate());

        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("error attempting to register parcel", parcel.getId());
            return Optional.empty();
        }
        return Optional.of(parcel);
    }

//    public static synchronized void registerNation(Parcel parcel) {
//        if (parcel.getType() == ParcelType.NATION) {
//            NATIONS_BY_ID.put(((NationParcel)parcel).getEstate().getId(), parcel);
//        }
//    }

    /**
     * inserts the parcel into the primary BST with its dimension.
     */
    public static synchronized void registerTree(Parcel parcel) {
        String dim = effectiveDimension(parcel.getDimension());
        TREE.insert(new CoordsInterval<>(
                parcel.getMinCoords(), parcel.getMaxCoords(),
                parcel.getOwnerId(), dim));
    }

    public static synchronized void registerCoords(Parcel parcel) {
        PARCELS_BY_ID.put(parcel.getId(), parcel);
        PARCELS_BY_ESTATE_ID.computeIfAbsent(parcel.getEstate().getId(), k -> new HashSet<>())
                .add(parcel);
        PARCELS_BY_COORDS.put(parcel.getMinCoords(), parcel);
    }

    /**
     * removes a parcel from the registries/maps
     * @param parcel
     */
    public static synchronized void unregisterParcel(Parcel parcel) {
        // remove from the TREE
        unregisterTree(parcel);

        unregisterOwner(parcel);

        // remove from friends
        unregisterFriends(parcel);

        // remove from coords (also removes ability of Estate finding parcel ie Estate.getParcels() )
        unregisterCoords(parcel);

        // remove from buffer map
        unregisterBuffer(parcel);

        // unindex the parcel
        String dimension = parcel.getDimension() != null
                ? parcel.getDimension()
                : "minecraft:overworld";
        ParcelChunkIndex.unindex(parcel, dimension);

        REGION_CACHE.invalidateByParcel(parcel.getId());

        // if nation remove from special map/registry
//        unregisterNation(parcel);

        //remove the estate if no more parcels
        if (parcel.getEstate().findParcels().isEmpty()) {
            EstateRegistry.unregister(parcel.getEstate());
        }
    }

    /**
     * Unregisters a parcel and notifies nearby clients.
     * Use this overload from command and block entity code where ServerLevel
     * is available.
     */
    public static synchronized void unregisterParcel(ServerLevel level, Parcel parcel) {
        unregisterParcel(parcel);
        CMLNetwork.removeParcelFromTracking(level, parcel);
    }

    public static synchronized void unregisterBuffer(Parcel parcel) {
        Box inflatedBox = inflateParcelBox(parcel);
        BUFFER_PARCELS_BY_COORDS.remove(inflatedBox.getMinCoords());
        String dim = effectiveDimension(parcel.getDimension());
        BUFFER_TREE.delete(new CoordsInterval<>(
                inflatedBox.getMinCoords(), inflatedBox.getMaxCoords(),
                parcel.getOwnerId(), dim));
    }

    public static synchronized void unregisterCoords(Parcel parcel) {
        PARCELS_BY_ID.remove(parcel.getId());
        Set<Parcel> estateSet = PARCELS_BY_ESTATE_ID.get(parcel.getEstate().getId());
        if (estateSet != null) {
            estateSet.remove(parcel);
            if (estateSet.isEmpty()) PARCELS_BY_ESTATE_ID.remove(parcel.getEstate().getId());
        }
        PARCELS_BY_COORDS.remove(parcel.getMinCoords());
    }

    public static synchronized void unregisterTree(Parcel parcel) {
        String dim = effectiveDimension(parcel.getDimension());
        TREE.delete(new CoordsInterval<>(
                parcel.getMinCoords(), parcel.getMaxCoords(),
                parcel.getOwnerId(), dim));
    }

//    public static synchronized void unregisterNation(Parcel parcel) {
//        if (parcel.getType() == ParcelType.NATION) {
//            NATIONS_BY_ID.remove(((NationParcel)parcel).getNationId(), parcel);
//
//            ParcelRegistry.findChildrenByNationId(parcel.getNationId())
//                    .forEach(p -> {
//                        // TODO calling this may cause Concurrent operation exceptions.
//                        if (p.getType() == ParcelType.ZONE) {
//                            ParcelRegistry.unregisterParcel(p);
//                        } else {
//                            p.setType(ParcelType.PLAYER);
//                            p.setNationId(null);
//                            // TODO remove borders if any this would have to be able to call the Border Entity Block - how?!
//                        }
//                    });
//        }
//    }

    /**
     * removes all parcels by player
     * @param ownerId
     */
    public static synchronized void removeParcel(ServerLevel level, UUID ownerId) {

        // get all parcels excluding zones as they will be handled when handling nations
        List<Parcel> parcels = Optional.ofNullable(PARCELS_BY_OWNER.get(ownerId))
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(p -> p.getType() != ParcelType.ZONE).toList();

        if (!parcels.isEmpty()) {
            for (Parcel parcel : parcels) {
                unregisterParcel(level, parcel);
            }
        }
    }

    /**
     * inflates the parcels dimensions by the config buffer radius setting
     */
    public static Box inflateParcelBox(final Parcel parcel) {
        return switch(parcel.getType()) {
            case PLAYER, CITIZEN -> ModUtil.inflate(parcel.getBox(), Config.SERVER.general.parcelBufferRadius.get());
            case NATION -> ModUtil.inflate(parcel.getBox(), Config.SERVER.general.nationParcelBufferRadius.get());
            case ZONE -> parcel.getBox();
            default -> parcel.getBox();
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
     * retrieves a list of all parcels by friend
     */
    public static List<Parcel> findByFriend(UUID id) {
        List<Parcel> parcels = PARCELS_BY_FRIENDS.get(id);
        return parcels == null ? new ArrayList<>() : parcels;
    }

    /**
     * returns a parcel by id
     */
//    public static Optional<Parcel> findByParcelId(UUID id) {
//        List<Parcel> parcels = new ArrayList<>(1);
//        for (Parcel parcel : PARCELS_BY_COORDS.values()) {
//            if (parcel.getId().equals(id)) {
//                parcels.add(parcel);
//                break;
//            }
//        }
//        return parcels.isEmpty() ? Optional.empty() : Optional.of(parcels.get(0));
//    }
    // old findByParcelId() — O(n), new findByParcelId() → O(1):
    public static Optional<Parcel> findByParcelId(UUID id) {
        return Optional.ofNullable(PARCELS_BY_ID.get(id));
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

    public static List<Parcel> findByNationName(String nationName) {
        return PARCELS_BY_COORDS.values().stream()
                .filter(p -> (p instanceof NationParcel))
                .filter(p -> (nationName.equals((p.getName()))))
                .toList();
    }

    public static List<Parcel> findChildrenByNationId(UUID nationId) {
        return PARCELS_BY_COORDS.values().stream()
                .filter(p -> ((p instanceof CitizenParcel || p instanceof ZoneParcel)))
                .map(p -> (NationalizedParcel) p)
                .filter(nationalizedParcel -> nationalizedParcel.getNationEstate().getId().equals(nationId))
                .collect(Collectors.toList());
    }

//    public static Set<Parcel> findAllByEstateId(UUID estateId) {
//        return PARCELS_BY_COORDS.values().stream()
//                .filter(parcel -> parcel.getEstate().getId().equals(estateId))
//                .collect(Collectors.toSet());
//    }

    public static Set<Parcel> findAllByEstateId(UUID estateId) {
        return PARCELS_BY_ESTATE_ID.getOrDefault(estateId, Collections.emptySet());
    }

    public static Set<Parcel> findAllByNationEstateId(UUID nationEstateId) {
        return PARCELS_BY_COORDS.values().stream()
                .filter(parcel -> parcel instanceof NationalizedParcel)
                .map(parcel -> (NationalizedParcel)parcel)
                .filter(parcel -> parcel.getNationEstate().getId().equals(nationEstateId))
                .collect(Collectors.toSet());
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
     * returns all parcels that have been abandoned ie ownerId is empty
     * @return
     */
    public static List<Parcel> findAbandoned() {
        return PARCELS_BY_COORDS.values().stream()
                .filter(p -> (p.getOwnerId() == null))
                .toList();
    }

    /**
     * find()/findBuffer() variants are slower versions of findRaw() since it requires looking up the parcel from the internal map.
     * @param coords
     * @return
     */
    public static List<Parcel> find(ICoords coords, String dimension) {
        return find(coords, coords, dimension);
    }

    /** @author Mark Gottschling on March 23, 2026 */
    public static List<Parcel> find(Box box, String dimension) {
        return find(box.getMinCoords(), box.getMaxCoords(), dimension);
    }

    /** @author Mark Gottschling on March 23, 2026 */
    public static List<Parcel> find(ICoords coords1, ICoords coords2, String dimension) {
        return find(coords1, coords2, false, true, dimension);
    }

    /** @author Mark Gottschling on March 23, 2026 */
    public static List<Parcel> find(ICoords coords1, ICoords coords2,
                                    boolean findFast, boolean includeBorder,
                                    String dimension) {
        return getAsParcels(findRaw(coords1, coords2, findFast, includeBorder, dimension));
    }

    public static List<Parcel> findBuffer(ICoords coords, String dimension) {
        return findBuffer(coords, coords, dimension);
    }

    /** @author Mark Gottschling on March 23, 2026 */
    public static List<Parcel> findBuffer(Box box, String dimension) {
        return findBuffer(box.getMinCoords(), box.getMaxCoords(), dimension);
    }

    /** @author Mark Gottschling on March 23, 2026 */
    public static List<Parcel> findBuffer(ICoords coords1, ICoords coords2, String dimension) {
        return findBuffer(coords1, coords2, false, true, dimension);
    }

    /** @author Mark Gottschling on March 23, 2026 */
    public static List<Parcel> findBuffer(ICoords coords1, ICoords coords2,
                                          boolean findFast, boolean includeBorder,
                                          String dimension) {
        return getBufferParcels(
                findBufferRaw(coords1, coords2, findFast, includeBorder, dimension));
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

    public static List<Box> findBoxes(ICoords coords, String dimension) {
        return findBoxes(coords, coords, dimension);
    }

    public static List<Box> findBoxes(Box box, String dimension) {
        return findBoxes(box.getMinCoords(), box.getMaxCoords(), dimension);
    }

    public static List<Box> findBoxes(ICoords coords1, ICoords coords2, String dimension) {
        return findBoxes(coords1, coords2, false, true, dimension);
    }

    public static List<Box> findBoxes(ICoords coords1, ICoords coords2,
                                      boolean findFast, boolean includeBorder,
                                      String dimension) {
        List<IInterval<UUID>> intervals =
                findRaw(coords1, coords2, findFast, includeBorder, dimension);
        List<Box> boxes = new ArrayList<>();
        intervals.forEach(i -> {
            Parcel p = PARCELS_BY_COORDS.get(((CoordsInterval<UUID>) i).getCoords1());
            if (p != null) {
                boxes.add(new Box(
                        ((CoordsInterval<UUID>) i).getCoords1(),
                        ((CoordsInterval<UUID>) i).getCoords2()));
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
    private static List<IInterval<UUID>> findRaw(ICoords coords1, ICoords coords2,
                                                 boolean findFast, boolean includeBorder,
                                                 String dimension) {
        CoordsInterval<UUID> test = new CoordsInterval<>(coords1, coords2);
        test.setDimension(effectiveDimension(dimension));
        return TREE.getOverlapping(TREE.getRoot(), test, findFast, includeBorder);
    }

    private static List<IInterval<UUID>> findBufferRaw(ICoords coords1, ICoords coords2,
                                                       boolean findFast, boolean includeBorder,
                                                       String dimension) {
        CoordsInterval<UUID> test = new CoordsInterval<>(coords1, coords2);
        test.setDimension(effectiveDimension(dimension));
        return BUFFER_TREE.getOverlapping(BUFFER_TREE.getRoot(), test, findFast, includeBorder);
    }

    public static boolean intersectsParcel(ICoords coords, String dimension) {
        return intersectsParcel(coords, coords, true, dimension);
    }

    /** @author Mark Gottschling on March 23, 2026 */
    public static boolean intersectsParcel(ICoords coords1, ICoords coords2, String dimension) {
        return intersectsParcel(coords1, coords2, true, dimension);
    }

    /** @author Mark Gottschling on March 23, 2026 */
    public static boolean intersectsParcel(ICoords coords1, ICoords coords2,
                                           boolean includeBorders, String dimension) {
        return !findBoxes(coords1, coords2, true, includeBorders, dimension).isEmpty();
    }

    public static boolean hasAccess(ServerPlayer player, ICoords coords, String dimension) {
        return resolveParcelCached(player, coords, dimension)
                .map(parcel -> parcel.grantsAccess(player.getUUID()))
                .orElse(true);
    }

    // main access check
    public static boolean hasAccess(ServerPlayer player, ICoords coords, String dimension, ItemStack itemStack) {
//        ClaimMyLand.LOGGER.debug("player checking access...");
        return resolveParcelCached(player, coords, dimension)
                .map(parcel -> (itemStack != null && !itemStack.isEmpty())
                        ? parcel.grantsAccess(player.getUUID(), itemStack)
                        : parcel.grantsAccess(player.getUUID()))
                .orElse(true);
    }

    public static boolean hasInteractAccess(ServerPlayer player, ICoords coords, String dimension, ItemStack itemStack) {
        return resolveParcelCached(player, coords, dimension)
                .map(parcel -> {
                    if (itemStack != null && !itemStack.isEmpty()) {
                        for (String tagName : parcel.getEstate().getItemTagWhitelist()) {
                            if (TagHelper.doesItemBelongToTag(itemStack.getItem(), ResourceLocation.parse(tagName))) return true;
                        }
                        for (String itemName : parcel.getEstate().getItemWhitelist()) {
                            if (ModUtil.getName(itemStack.getItem()).equals(ResourceLocation.parse(itemName))) return true;
                        }
                    }
                    return (itemStack != null && !itemStack.isEmpty())
                            ? parcel.grantsAccess(player.getUUID(), itemStack)
                            : parcel.grantsAccess(player.getUUID());
                })
                .orElse(true);
    }

    public static boolean hasInteractAccess(UUID playerId, ICoords coords, String dimension, ItemStack itemStack) {
        return resolveParcelCached(playerId, coords, dimension)
                .map(parcel -> {
                    if (itemStack != null && !itemStack.isEmpty()) {
                        for (String tagName : parcel.getEstate().getItemTagWhitelist()) {
                            ResourceLocation location = ResourceLocation.parse(tagName);
                            if (TagHelper.doesItemBelongToTag(itemStack.getItem(), location)) return true;
                        }
                        for (String itemName : parcel.getEstate().getItemWhitelist()) {
                            if (ModUtil.getName(itemStack.getItem()).equals(ResourceLocation.parse(itemName))) return true;
                        }
                    }
                    return (itemStack != null && !itemStack.isEmpty())
                            ? parcel.grantsAccess(playerId, itemStack)
                            : parcel.grantsAccess(playerId);
                })
                .orElse(true);
    }

    public static boolean hasInteractAccess(ServerPlayer player, ICoords coords, String dimension,
                                            BlockState state, ItemStack heldItem) {
        return resolveParcelCached(player, coords, dimension)
                .map(parcel -> {
//                    ClaimMyLand.LOGGER.debug("trying to use block {} in parcel -> {}",
//                            state.getBlock().getName().getString(), parcel);

                    for (String tagName : parcel.getEstate().getBlockTagWhitelist()) {
                        if (TagHelper.doesBlockBelongToTag(state.getBlock(), ResourceLocation.parse(tagName))) return true;
                    }
                    for (String blockName : parcel.getEstate().getBlockWhitelist()) {
                        if (ModUtil.getName(state.getBlock()).equals(ResourceLocation.parse(blockName))) return true;
                    }
                    if (heldItem != null && !heldItem.isEmpty()) {
                        for (String tagName : parcel.getEstate().getItemTagWhitelist()) {
                            if (TagHelper.doesItemBelongToTag(heldItem.getItem(), ResourceLocation.parse(tagName))) return true;
                        }
                        for (String itemName : parcel.getEstate().getItemWhitelist()) {
                            if (ModUtil.getName(heldItem.getItem()).equals(ResourceLocation.parse(itemName))) return true;
                        }
                    }
                    return parcel.grantsAccess(player.getUUID(), heldItem);
                })
                .orElse(true);

    }

    /**
     * Returns true if the given player is blacklisted from claiming land within
     * the nation that encloses the given coordinates.
     * <p>
     * If no enclosing nation exists at the coordinates, returns false — there is
     * nothing to blacklist against.
     * </p>
     *
     * @param coords    the coordinates to check (typically the Foundation Stone position)
     * @param playerId  the claiming player's UUID
     * @param dimension the dimension string
     */
    public static boolean isBlacklistedFromNation(ICoords coords, UUID playerId,
                                                  String dimension) {
        return find(coords, dimension).stream()
                .filter(Parcel::isNation)
                .findFirst()
                .map(nation -> nation.getEstate() instanceof NationEstate ne
                        && ne.isBlacklisted(playerId))
                .orElse(false);
    }

    /**
     * Overload for call sites that already have the enclosing nation parcel resolved.
     * Avoids a redundant registry lookup.
     *
     * @param nationParcel  the enclosing nation parcel
     * @param playerId      the claiming player's UUID
     */
    public static boolean isBlacklistedFromNation(Parcel nationParcel, UUID playerId) {
        if (nationParcel == null || !nationParcel.isNation()) return false;
        return nationParcel.getEstate() instanceof NationEstate ne
                && ne.isBlacklisted(playerId);
    }

    /**
     * UUID-keyed variant — passes dimension through to {@link #resolveParcelAt}.
     *
     * @author Mark Gottschling on March 23, 2026
     */
    public static Optional<Parcel> resolveParcelCached(UUID playerId, ICoords coords,
                                                       String dimension) {
        Optional<ParcelRegionCache.CacheEntry> cached =
                REGION_CACHE.getIfContains(playerId, coords, dimension);
        if (cached.isPresent()) {
            Parcel cachedParcel = cached.get().getParcel();
            if (cachedParcel.getType().isLeaf()) {
                return Optional.of(cachedParcel);
            }
            return resolveParcelAt(coords, coords, dimension);
        }

        Optional<Parcel> resolved = resolveParcelAt(coords, coords, dimension);
        if (resolved.isPresent()) {
            REGION_CACHE.update(playerId, resolved.get());
        } else {
            REGION_CACHE.invalidatePlayer(playerId);
        }
        return resolved;
    }

    /**
     * ServerPlayer-keyed variant — passes dimension through to {@link #resolveParcelAt}
     * and syncs cache/wilderness packets.
     *
     * @author Mark Gottschling on March 23, 2026
     */
    public static Optional<Parcel> resolveParcelCached(ServerPlayer player, ICoords coords,
                                                       String dimension) {
        UUID playerId = player.getUUID();
        Optional<ParcelRegionCache.CacheEntry> cached =
                REGION_CACHE.getIfContains(playerId, coords, dimension);
        if (cached.isPresent()) {
            Parcel cachedParcel = cached.get().getParcel();
            if (cachedParcel.getType().isLeaf()) {
                return Optional.of(cachedParcel);
            }
            return resolveParcelAt(coords, coords, dimension);
        }

        Optional<Parcel> resolved = resolveParcelAt(coords, coords, dimension);
        if (resolved.isPresent()) {
            REGION_CACHE.update(playerId, resolved.get());
            CMLNetwork.syncCacheToPlayer(player, resolved.get());
        } else {
            REGION_CACHE.invalidatePlayer(playerId);
            CMLNetwork.syncWildernessToPlayer(player);
        }
        return resolved;
    }

    /**
     * Tick-path variant used exclusively by the HUD sync in ModEvents.onPlayerTick().
     *
     * Unlike resolveParcelCached(), this always goes to the BST to ensure the
     * least-significant (innermost) parcel is returned when the player is standing
     * inside nested parcels (e.g. a citizen parcel inside a nation).
     *
     * Sends CacheSyncPacket only when the resolved parcel changes, using the
     * parcel ID stored in REGION_CACHE as the change-detector.
     */
    public static void syncParcelToClient(ServerPlayer player, ICoords coords,
                                          String dimension) {
        UUID playerId = player.getUUID();
        Optional<ParcelRegionCache.CacheEntry> cached = REGION_CACHE.get(playerId);

        if (cached.isPresent() && cached.get().contains(coords, dimension)) {
            ParcelType cachedType = cached.get().getParcel().getType();
            if (cachedType == ParcelType.CITIZEN || cachedType == ParcelType.PLAYER) {
                return;
            }
        }

        Optional<Parcel> resolved = resolveParcelAt(coords, coords, dimension);
        UUID newId    = resolved.map(Parcel::getId).orElse(null);
        UUID cachedId = cached.map(e -> e.getParcel().getId()).orElse(null);

        if (!Objects.equals(newId, cachedId)) {
            if (resolved.isPresent()) {
                REGION_CACHE.update(playerId, resolved.get());
                CMLNetwork.syncCacheToPlayer(player, resolved.get());
            } else {
                REGION_CACHE.invalidatePlayer(playerId);
                CMLNetwork.syncWildernessToPlayer(player);
            }
        }
    }

    /**
     * Returns the parcel with the smallest area at the given coords in the given
     * dimension. Uses the BST directly rather than post-filtering.
     */
    public static Optional<Parcel> findLeastSignificant(ICoords coords, String dimension) {
        return findLeastSignificant(find(coords, dimension));
    }

    private static Optional<Parcel> findLeastSignificant(List<Parcel> parcels) {
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
     * returns the most significant (largest area) parcel at coords,
     * optionally filtered by type. used to find the containing nation/zone parcel.
     */
    public static Optional<Parcel> findMostSignificant(ICoords coords, String dimension,
                                                       ParcelType... types) {
        return findMostSignificant(find(coords, dimension), types);
    }

    public static Optional<Parcel> findMostSignificant(List<Parcel> parcels, String dimension, ParcelType... types) {
        List<Parcel> filteredParcels = parcels.stream()
                .filter(p -> dimension.equals(p.getDimension()))
                .toList();
        return findMostSignificant(filteredParcels, types);
    }

    private static Optional<Parcel> findMostSignificant(List<Parcel> parcels, ParcelType... types) {
        Set<ParcelType> typeSet = Set.of(types);
        return parcels.stream()
                .filter(p -> typeSet.isEmpty() || typeSet.contains(p.getType()))
                .max(Comparator.comparingLong(Parcel::getArea));
    }

    public static List<UUID> getOwnerIds() {
        return PARCELS_BY_OWNER.keySet().stream().toList();
    }

    public static int size() {
        return PARCELS_BY_COORDS.size();
    }

    /**
     * not case-sensitive
     * @param name
     * @return
     */
    public static Optional<Parcel> findByName(String name) {
        return PARCELS_BY_COORDS.values().stream().filter(p -> p.getName().equalsIgnoreCase(name)).findFirst();
    }

    public static void transferParcelOwnership(ServerLevel level, Parcel parcel, UUID newOwnerUuid) {
        REGION_CACHE.invalidateByParcel(parcel.getId());

        // unregister parcel
        ParcelRegistry.unregisterParcel(level, parcel);

        // save old estate
        Estate oldEstate = parcel.getEstate();
        // create new estate and update parcel
        Estate estate = EstateTypeRegistry.create(parcel.isNation() ? EstateTypeRegistry.NATION_ESTATE_TYPE : EstateTypeRegistry.ESTATE_TYPE);
        estate.setOwnerId(newOwnerUuid);
        estate.setName(oldEstate.getName());
        estate.setParcelType(oldEstate.getParcelType());
        estate.setRelinquished(false);

        parcel.setEstate(estate);

        // re-register parcel
        ParcelRegistry.register(level, parcel);
    }

    /**
     * returns true if fire spread should be cancelled at the given coords.
     * checks the server-wide config first, then the estate-level property.
     */
    public static boolean isFireSpreadPrevented(ICoords coords, BlockState state,
                                                String dimension) {
        if (!Config.SERVER.protection.preventFireSpread.get()) return false;
        if (!state.is(ModTags.Blocks.FIRE_BLOCKS)) return false;
        return findLeastSignificant(coords, dimension)
                .map(parcel -> parcel.getEstate().isPreventFireSpread())
                .orElse(false);
    }

    // new methods should replace repeated code elsewhere
    public static void registerOwner(Parcel parcel) {
        PARCELS_BY_OWNER.computeIfAbsent(parcel.getOwnerId(), k -> new ArrayList<>())
                .add(parcel);
    }

    public static void unregisterOwner(Parcel parcel) {
        if (ObjectUtils.isNotEmpty(parcel.getOwnerId())) {
            List<Parcel> parcels = PARCELS_BY_OWNER.get(parcel.getOwnerId());
            if (!parcels.isEmpty()) {
                parcels.removeIf(p -> p.getId().equals(parcel.getId()));
            }
        }
    }

    public static void registerFriends(Parcel parcel) {
        if (ObjectUtils.isNotEmpty(parcel.getEstate().getPlayerWhitelist())) {
            // add to parcels_by_friends
            parcel.getEstate().getPlayerWhitelist().forEach(friend ->
                    PARCELS_BY_FRIENDS.computeIfAbsent(friend, k -> new ArrayList<>())
                            .add(parcel)
            );
        }
    }

    public static void unregisterFriends(Parcel parcel) {
        parcel.getEstate().getPlayerWhitelist().forEach(friend -> {
            List<Parcel> parcelList = PARCELS_BY_FRIENDS.get(friend);
            if (!parcelList.isEmpty()) {
                parcelList.removeIf(p -> p.getId().equals(parcel.getId()));
            }
        });
    }

    /**
     * Resolves the least-significant parcel at the given coordinates within the
     * specified dimension.
     */
    private static Optional<Parcel> resolveParcelAt(ICoords coords1, ICoords coords2,
                                                    String dimension) {
        List<IInterval<UUID>> intervals =
                findRaw(coords1, coords2, false, true, dimension);
        if (intervals.isEmpty()) return Optional.empty();
        List<Parcel> parcels = getAsParcels(intervals);
        if (parcels.isEmpty()) {
            TREE.delete(intervals.get(0)); // stale interval cleanup
            return Optional.empty();
        }
        return intervals.size() > 1 ? findLeastSignificant(parcels) : Optional.of(parcels.get(0));
    }

    /**
     * Determines the visual conflict state for a proposed parcel placement.
     * Returns 1 (conflict) or 0 (no conflict). Used client-side for border colour.
     *
     * Mirrors the rules in Parcel.handleClaim() so the preview border always
     * matches the actual claim outcome.
     *
     * Rule 1  — Direct box overlap:
     *   Hierarchical overlaps (ancestor/descendant) are only valid when fully
     *   contained — failing containment counts as a conflict.
     *   Same-owner same-type siblings may touch but not overlap.
     *   All other overlaps are conflicts.
     *
     * Rule 2  — Bidirectional buffer conflict (siblings exempt):
     *   2a. Existing parcel's buffer zone reaches into the proposed box.
     *   2b. Proposed parcel's own buffer zone reaches into existing parcel boxes.
     *
     * @param proposedBox     the box of the parcel being evaluated
     * @param ownerId         the owner of the parcel being evaluated
     * @param excludeParcelId parcel id to exclude from results (the parcel itself)
     * @param placingType     the type of the parcel being evaluated
     * @param dimension       the dimension string
     * @return 1 if in conflict, 0 if clear
     */
    public static int resolveConflictState(Box proposedBox, UUID ownerId,
                                           UUID excludeParcelId, ParcelType placingType,
                                           String dimension) {
        // ── Rule 1: direct box overlap ────────────────────────────────────────
        List<Parcel> directOverlaps = find(proposedBox, dimension).stream()
                .filter(p -> !p.getId().equals(excludeParcelId))
                .toList();

        ClaimMyLand.LOGGER.debug("resolveConflictState: parcelId={} type={} proposedBox={} dimension={} directOverlaps={}",
                excludeParcelId,
                placingType,
                proposedBox,
                dimension,
                directOverlaps.stream()
                        .map(p -> p.getId() + ":" + p.getType() + ":" + p.getOwnerId())
                        .toList());

        for (Parcel existing : directOverlaps) {
            boolean hierarchical = ParcelType.isAllowedAncestor(existing.getType(), placingType)
                    || ParcelType.isAllowedDescendant(existing.getType(), placingType);

//            if (hierarchical) {
//                // valid only when fully contained
//                if (!ModUtil.contains(existing.getBox(), proposedBox)) return 1;
//                continue;
//            }
            if (hierarchical) {
                boolean ancestorContainsPlacing =
                        ParcelType.isAllowedAncestor(existing.getType(), placingType)
                                && ModUtil.contains(existing.getBox(), proposedBox);
                boolean placingContainsDescendant =
                        ParcelType.isAllowedDescendant(existing.getType(), placingType)
                                && ModUtil.contains(proposedBox, existing.getBox());
                if (!ancestorContainsPlacing && !placingContainsDescendant) return 1;
                continue;
            }

            if (ParcelConflictResolver.isConflict(placingType, existing.getType(),
                    ownerId, existing.getOwnerId())) {
                return 1;
            }

            // same-owner same-type sibling: touching ok, overlap is a conflict
            if (ModUtil.overlaps(proposedBox, existing.getBox())) return 1;
        }

        // ── Rule 2: bidirectional buffer conflict ─────────────────────────────
        // Rule 2a: existing parcels whose buffer zones reach into the proposed box
        List<Parcel> bufferOverlaps = findBuffer(proposedBox, dimension).stream()
                .filter(p -> !p.getId().equals(excludeParcelId))
                .toList();

        // Rule 2b: proposed parcel's own buffer zone reaches into existing parcel boxes
        int placingBuffer = getBufferSizeForType(placingType);
        List<Parcel> inflatedOverlaps = placingBuffer > 0
                ? find(ModUtil.inflate(proposedBox, placingBuffer), dimension).stream()
                  .filter(p -> !p.getId().equals(excludeParcelId))
                  .toList()
                : List.of();

        ClaimMyLand.LOGGER.debug("resolveConflictState: bufferOverlaps={} inflatedOverlaps={}",
                bufferOverlaps.stream()
                        .map(p -> p.getId() + ":" + p.getType())
                        .toList(),
                inflatedOverlaps.stream()
                        .map(p -> p.getId() + ":" + p.getType())
                        .toList());

        boolean bufferConflict =
                bufferOverlaps.stream().anyMatch(p ->
                        ParcelConflictResolver.isConflict(placingType, p.getType(),
                                ownerId, p.getOwnerId()))
                        || inflatedOverlaps.stream().anyMatch(p ->
                        ParcelConflictResolver.isConflict(placingType, p.getType(),
                                ownerId, p.getOwnerId()));

        ClaimMyLand.LOGGER.debug("resolveConflictState: parcelId={} bufferConflict={} detail_buffer={} detail_inflated={}",
                excludeParcelId,
                bufferConflict,
                bufferOverlaps.stream()
                        .map(p -> p.getId() + ":" + p.getType() + ":conflict=" +
                                ParcelConflictResolver.isConflict(placingType, p.getType(),
                                        ownerId, p.getOwnerId()))
                        .toList(),
                inflatedOverlaps.stream()
                        .map(p -> p.getId() + ":" + p.getType() + ":conflict=" +
                                ParcelConflictResolver.isConflict(placingType, p.getType(),
                                        ownerId, p.getOwnerId()))
                        .toList());

        return bufferConflict ? 1 : 0;
    }


    private static int getBufferSizeForType(ParcelType type) {
        return switch (type) {
            case NATION -> Config.SERVER.general.nationParcelBufferRadius.get();
            case PLAYER, CITIZEN -> Config.SERVER.general.parcelBufferRadius.get();
            default -> 0;
        };
    }

    // -------------------------------------------------------------------------
    // Private helper — normalise null dimension to DEFAULT_DIMENSION
    // -------------------------------------------------------------------------

    /**
     * Returns {@code dim} if non-null, otherwise {@link CoordsInterval#DEFAULT_DIMENSION}.
     * Mirrors {@link CoordsInterval#effectiveDimension(String)} for use inside
     * ParcelRegistry before constructing intervals.
     */
    private static String effectiveDimension(String dim) {
        return CoordsInterval.effectiveDimension(dim);
    }

    // expose a detached parcel list
    public static List<Parcel> getParcels() {
        return new ArrayList<>(PARCELS_BY_COORDS.values());
    }
}
