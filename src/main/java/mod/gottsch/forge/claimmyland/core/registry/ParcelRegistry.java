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


import com.google.gson.Gson;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.cache.ParcelRegionCache;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.EstateTypeRegistry;
import mod.gottsch.forge.claimmyland.core.network.CMLNetwork;
import mod.gottsch.forge.claimmyland.core.parcel.*;
import mod.gottsch.forge.claimmyland.core.tags.ModTags;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.claimmyland.core.util.TagHelper;
import mod.gottsch.forge.gottschcore.bst.CoordsInterval;
import mod.gottsch.forge.gottschcore.bst.CoordsIntervalTree;
import mod.gottsch.forge.gottschcore.bst.IInterval;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
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
@SuppressWarnings("removal")
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
     * inflates the parcel and adds to the buffer tree, which is a duplicate of the main tree,
     * but uses the inflated coords. the buffer tree is used when determining if 2 parcels
     * meet the buffer criteria.
     * NOTE even if buffer size == 0, the parcel must be added to teh buffer tree.
     * @param parcel
     */
    public static synchronized void registerBuffer(Parcel parcel) {
        Box inflatedBox;
        // inflate if buffer size is > 0
        if (parcel.getBufferSize() > 0) {
            inflatedBox = inflateParcelBox(parcel);
        } else {
            inflatedBox = parcel.getBox();
        }

        BUFFER_TREE.insert(new CoordsInterval<>(inflatedBox.getMinCoords(), inflatedBox.getMaxCoords(), parcel.getOwnerId()));
        // add buffered parcel to byCoords map
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

//    /**
//     *
//     * @param tag
//     */
//    public static void convertV1ToV2(CompoundTag tag) {
//        Map<ICoords, Parcel> byCoords = new HashMap<>();
//        ClaimMyLand.LOGGER.debug("converting registry from v1 to v2...");
//
//        if (tag.contains(PARCELS_KEY)) {
//            // 1. load parcels into local map
//            ListTag list = tag.getList(PARCELS_KEY, Tag.TAG_COMPOUND);
//
//            list.stream()
//                    .map(element -> (CompoundTag) element)
//                    .peek(e -> ClaimMyLand.LOGGER.debug("processing v1 parcel element..."))
//                    .forEach(e -> {
//                        ParcelType type = e.contains(Parcel.TYPE)
//                                ? ParcelType.fromString(e.getString(Parcel.TYPE))
//                                : ParcelType.NONE;
//
//                        ParcelTypeRegistry.create(type).ifPresent(parcel -> {
//                            // load the legacy parcel
//                            // NOTE the new parcel structure is used, so method calls like setWhitelist()
//                            //  are actually updating the estate.
//                            loadV1(parcel, e);
//
//                            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
//                                ClaimMyLand.LOGGER.debug("loaded v1 parcel -> {}", parcel);
//                            }
//
//                            // add parcel to the local map
//                            byCoords.put(parcel.getCoords(), parcel);
//                        });
//                    });
//
//
//            // 2. walk the parcel map, building the nation-level parcels
//            byCoords.values().stream()
//                    .filter(p -> p instanceof NationParcel)
//                    .map(p -> (NationParcel)p)
//                    .forEach(np -> {
//                        NationEstateContext estate = (NationEstateContext) np.getEstate();
//                        // copy properties
//                        estate.setPlayerBlacklist(new HashSet<>(np.getBlacklist()));
//                        if (estate.getOwnerId() != null) {
//                            ParcelRegistry.register(np);
//                        }
//                    });
//
//            // walk he parcel map again, building the rest
//            byCoords.values().stream()
//                    .filter(p -> !(p instanceof NationParcel))
//                    .forEach(p -> {
//                        Estate estate = p.getEstate();
//                        // setup default estate
//
//
//                        // setup the nation estate if a nation id exists
//                        if (p instanceof NationalizedParcel nationalizedParcel) {
//
//                            Optional<Estate> nationEstate = EstateRegistry.get(nationalizedParcel.getNationEstate().getId());
//                            if (nationEstate.isEmpty()) {
//                                // TODO log warning
//                                return;
//                            }
//
//                            if (estate.getOwnerId() == null) {
//                                estate.setRelinquished(true);
//                                estate.setOwnerId(nationEstate.get().getOwnerId());
//                            }
//                            nationalizedParcel.setNationEstate((NationEstate) nationEstate.get());
//                        }
//                        ParcelRegistry.register(p);
//                    });
//        }
//    }

//    public static synchronized void loadV1(Parcel parcel, CompoundTag tag) {
//        // standard parcel
//        if (tag.contains(AbstractParcel.ID_KEY)) {
//            parcel.setId(tag.getUUID(AbstractParcel.ID_KEY));
//        } else {
//            parcel.setId(UUID.randomUUID());
//        }
//        if (tag.contains(AbstractParcel.NATION_ID_KEY)) {
//            parcel.setNationId(tag.getUUID(AbstractParcel.NATION_ID_KEY));
//        }
//        if (tag.contains(AbstractParcel.NAME_KEY)) {
//            parcel.setName(tag.getString(AbstractParcel.NAME_KEY));
//        }
//        if (tag.contains(AbstractParcel.OWNER_KEY)) {
//            parcel.setOwnerId(tag.getUUID(AbstractParcel.OWNER_KEY));
//        }
//        if (tag.contains(AbstractParcel.DEED_KEY)) {
//            parcel.setDeedId(tag.getUUID(AbstractParcel.DEED_KEY));
//        }
//        if (tag.contains(AbstractParcel.TYPE)) {
//            parcel.setType(ParcelType.valueOf(tag.getString(AbstractParcel.TYPE)));
//        }
//        if (tag.contains(AbstractParcel.COORDS_KEY)) {
//            parcel.setCoords(Coords.EMPTY.load(tag.getCompound(AbstractParcel.COORDS_KEY)));
//        }
//        if (tag.contains(AbstractParcel.SIZE_KEY)) {
//            parcel.setSize(Box.load(tag.getCompound(AbstractParcel.SIZE_KEY)));
//        }
//        if (tag.contains(AbstractParcel.WHITELIST_KEY)) {
//            ListTag list = tag.getList(AbstractParcel.WHITELIST_KEY, Tag.TAG_COMPOUND);
//            list.forEach(element -> {
//                CompoundTag uuidTag = ((CompoundTag)element);
//                if (uuidTag.contains(AbstractParcel.ID_KEY)) {
//                    ClaimMyLand.LOGGER.debug("loading {} to whitelist", uuidTag.getUUID(AbstractParcel.ID_KEY));
//                    parcel.getWhitelist().add(uuidTag.getUUID(AbstractParcel.ID_KEY));
//                }
//            });
//        }
//
//        if (tag.contains(AbstractParcel.BLOCK_TAG_WHITELIST_KEY)) {
//            ListTag list = tag.getList(AbstractParcel.BLOCK_TAG_WHITELIST_KEY, Tag.TAG_STRING);
//            list.forEach(element -> {
//                String blockTag = element.getAsString();
//                parcel.getBlockTagWhitelist().add(blockTag);
//            });
//        }
//
//        if (tag.contains(AbstractParcel.BLOCK_WHITELIST_KEY)) {
//            ListTag list = tag.getList(AbstractParcel.BLOCK_WHITELIST_KEY, Tag.TAG_STRING);
//            list.forEach(element -> {
//                String block = element.getAsString();
//                parcel.getBlockWhitelist().add(block);
//            });
//        }
//
//        if (tag.contains(AbstractParcel.ITEM_TAG_WHITELIST_KEY)) {
//            ListTag list = tag.getList(AbstractParcel.ITEM_TAG_WHITELIST_KEY, Tag.TAG_STRING);
//            list.forEach(element -> {
//                String itemTag = element.getAsString();
//                parcel.getItemTagWhitelist().add(itemTag);
//            });
//        }
//
//        if (tag.contains(AbstractParcel.ITEM_WHITELIST_KEY)) {
//            ListTag list = tag.getList(AbstractParcel.ITEM_WHITELIST_KEY, Tag.TAG_STRING);
//            list.forEach(element -> {
//                String item = element.getAsString();
//                parcel.getItemWhitelist().add(item);
//            });
//        }
//
//        // parcel specific properties
//        if (parcel instanceof NationParcel nationParcel) {
//            NationEstateContext estate = (NationEstateContext) nationParcel.getEstate();
//            if (tag.contains("borderType")) {
//                try {
//                    estate.setAccessType(NationAccessType.valueOf(tag.getString("borderType")));
//                } catch(Exception e) {
//                    ClaimMyLand.LOGGER.warn("unable to parse and load borderType - using default CLOSED");
//                    estate.setAccessType(NationAccessType.CLOSED);
//                }
//            }
//
//            if (tag.contains("blacklist")) {
//                ListTag list = tag.getList("blacklist", Tag.TAG_STRING);
//                list.forEach(element -> {
//                    StringTag uuidTag = ((StringTag)element);
//                    nationParcel.getBlacklist().add(UUID.fromString(uuidTag.getAsString()));
//                });
//            }
//        }
//    }

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

    public static  synchronized void registerTree(Parcel parcel) {
        IInterval<UUID> interval = TREE.insert(
                new CoordsInterval<>(parcel.getMinCoords(), parcel.getMaxCoords(), parcel.getOwnerId())
        );
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
        // TODO test if this only deletes the one, or everything in this area.
        // remove from buffer tree
        BUFFER_TREE.delete(new CoordsInterval<>(new CoordsInterval<UUID>(inflatedBox.getMinCoords(), inflatedBox.getMaxCoords(), parcel.getOwnerId())));
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
        TREE.delete(new CoordsInterval<>(new CoordsInterval<UUID>(parcel.getMinCoords(), parcel.getMaxCoords(), parcel.getOwnerId())));
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

    public static boolean hasInteractAccess(ICoords coords, UUID entityId, BlockState state, ItemStack heldItem ) {
        return hasInteractAccess(coords, coords, entityId, state, heldItem);
    }

    public static boolean hasAccess(ICoords coords1, ICoords coords2, UUID entityId, ItemStack itemStack) {
        return resolveParcelAt(coords1, coords2)
                .map(parcel -> (itemStack != null && !itemStack.isEmpty())
                        ? parcel.grantsAccess(entityId, itemStack)
                        : parcel.grantsAccess(entityId))
                .orElse(true);
    }

    public static boolean hasAccess(ServerPlayer player, ICoords coords, String dimension) {
        return resolveParcelCached(player, coords, dimension)
                .map(parcel -> parcel.grantsAccess(player.getUUID()))
                .orElse(true);
    }

    public static boolean hasAccess(ServerPlayer player, ICoords coords, String dimension, ItemStack itemStack) {
        return resolveParcelCached(player, coords, dimension)
                .map(parcel -> (itemStack != null && !itemStack.isEmpty())
                        ? parcel.grantsAccess(player.getUUID(), itemStack)
                        : parcel.grantsAccess(player.getUUID()))
                .orElse(true);
    }

//    public static boolean hasAccess(ICoords coords1, ICoords coords2, UUID entityId, ItemStack itemStack) {
//        // this is the fastest lookup
//        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, false, true );
//        if (!intervals.isEmpty()) {
//            Parcel parcel;
//            // convert to parcels
//            List<Parcel> parcels = getAsParcels(intervals);
//
//            if (parcels.isEmpty()) {
//                return true;
//            }
//            if (intervals.size() > 1) {
//                // find the least significant parcel
//                Optional<Parcel> parcelOptional = findLeastSignificant(parcels);
//                if (parcelOptional.isPresent()) {
//                    parcel = parcelOptional.get();
//                } else {
//                    // TODO add chat warning
//                    // TODO add log warning
//                    // TODO maybe do something like labelling as abandoned and has a timer before it is removed from registry.
//                    // this is a case where the interval still exists but the parcel has been removed
//                    TREE.delete(intervals.get(0));
//                    return true;
//                }
//            } else {
//                parcel = parcels.get(0);
//            }
//
//            // check player's access
//            return (itemStack !=null && !itemStack.isEmpty()) ? parcel.grantsAccess(entityId, itemStack) : parcel.grantsAccess(entityId);
//        }
//        return true;
//    }

    /*
     *
     */
//    public static boolean hasInteractAccess(ICoords coords1, ICoords coords2, UUID entityId, ItemStack itemStack) {
//        // this is the fastest lookup
//        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, false, true );
//        if (!intervals.isEmpty()) {
//            Parcel parcel;
//            // convert to parcels
//            List<Parcel> parcels = getAsParcels(intervals);
//
//            if (parcels.isEmpty()) {
//                return true;
//            }
//            if (intervals.size() > 1) {
//                // find the least significant parcel
//                Optional<Parcel> parcelOptional = findLeastSignificant(parcels);
//                if (parcelOptional.isPresent()) {
//                    parcel = parcelOptional.get();
//                } else {
//                    // this is a case where the interval still exists but the parcel has been removed
//                    TREE.delete(intervals.get(0));
//                    return true;
//                }
//            } else {
//                parcel = parcels.get(0);
//            }
//
//            // if you have an item in your hand, do whitelist short-circuit tests
//            if (itemStack != null && !itemStack.isEmpty()) {
//                ClaimMyLand.LOGGER.debug("trying to use item {} in parcel -> {}", itemStack.getDisplayName().getString(), parcel);
//                // test the item against the whitelisted item tags for the parcel
//                for (String tagName : parcel.getItemTagWhitelist()) {
//                    ResourceLocation location = new ResourceLocation(tagName);
//                    ClaimMyLand.LOGGER.debug("creating tag for parcel item tag -> {}", location.toString());
//                    // get the tag from the resource key
//                    if (TagHelper.doesItemBelongToTag(itemStack.getItem(), location)) {
//                        return true;
//                    }
//                }
//
//                ClaimMyLand.LOGGER.debug("value of item white list -> {}", parcel.getItemWhitelist());
//                for (String itemName : parcel.getItemWhitelist()) {
//                    ResourceLocation location = new ResourceLocation(itemName);
//                    ClaimMyLand.LOGGER.debug("comparing item locations for held item -> {}", itemName);
//                    if (ModUtil.getName(itemStack.getItem()).equals(location)) {
//                        return true;
//                    }
//                }
//            }
//
//            // check player's access
//            return (itemStack !=null && !itemStack.isEmpty()) ? parcel.grantsAccess(entityId, itemStack) : parcel.grantsAccess(entityId);
//        }
//        return true;
//    }

    public static boolean hasInteractAccess(ServerPlayer player, ICoords coords, String dimension, ItemStack itemStack) {
        return resolveParcelCached(player, coords, dimension)
                .map(parcel -> {
                    if (itemStack != null && !itemStack.isEmpty()) {
                        for (String tagName : parcel.getEstate().getItemTagWhitelist()) {
                            if (TagHelper.doesItemBelongToTag(itemStack.getItem(), new ResourceLocation(tagName))) return true;
                        }
                        for (String itemName : parcel.getEstate().getItemWhitelist()) {
                            if (ModUtil.getName(itemStack.getItem()).equals(new ResourceLocation(itemName))) return true;
                        }
                    }
                    return (itemStack != null && !itemStack.isEmpty())
                            ? parcel.grantsAccess(player.getUUID(), itemStack)
                            : parcel.grantsAccess(player.getUUID());
                })
                .orElse(true);
    }

    public static boolean hasInteractAccess(ICoords coords1, ICoords coords2, UUID entityId, BlockState state, ItemStack heldItem) {
        return resolveParcelAt(coords1, coords2)
                .map(parcel -> {
                    ClaimMyLand.LOGGER.debug("trying to use block {} in parcel -> {}", state.getBlock().getName().getString(), parcel);

                    for (String tagName : parcel.getEstate().getBlockTagWhitelist()) {
                        ResourceLocation location = new ResourceLocation(tagName);
                        ClaimMyLand.LOGGER.debug("creating tag for parcel block tag -> {}", location);
                        if (TagHelper.doesBlockBelongToTag(state.getBlock(), location)) {
                            return true;
                        }
                    }

                    ClaimMyLand.LOGGER.debug("value of block white list -> {}", parcel.getEstate().getBlockWhitelist());
                    for (String blockName : parcel.getEstate().getBlockWhitelist()) {
                        ResourceLocation location = new ResourceLocation(blockName);
                        ClaimMyLand.LOGGER.debug("comparing block locations for parcel block -> {}", blockName);
                        if (ModUtil.getName(state.getBlock()).equals(location)) {
                            return true;
                        }
                    }

                    if (heldItem != null && !heldItem.isEmpty()) {
                        ClaimMyLand.LOGGER.debug("trying to use item {} in parcel -> {}", heldItem.getDisplayName().getString(), parcel);

                        for (String tagName : parcel.getEstate().getItemTagWhitelist()) {
                            ResourceLocation location = new ResourceLocation(tagName);
                            ClaimMyLand.LOGGER.debug("creating tag for parcel item tag -> {}", location);
                            if (TagHelper.doesItemBelongToTag(heldItem.getItem(), location)) {
                                return true;
                            }
                        }

                        ClaimMyLand.LOGGER.debug("value of item white list -> {}", parcel.getEstate().getItemWhitelist());
                        for (String itemName : parcel.getEstate().getItemWhitelist()) {
                            ResourceLocation location = new ResourceLocation(itemName);
                            ClaimMyLand.LOGGER.debug("comparing item locations for held item -> {}", itemName);
                            if (ModUtil.getName(heldItem.getItem()).equals(location)) {
                                return true;
                            }
                        }
                    }

                    return parcel.grantsAccess(entityId, heldItem);
                })
                .orElse(true);
    }

    public static boolean hasInteractAccess(UUID playerId, ICoords coords, String dimension, ItemStack itemStack) {
        return resolveParcelCached(playerId, coords, dimension)
                .map(parcel -> {
                    if (itemStack != null && !itemStack.isEmpty()) {
                        for (String tagName : parcel.getEstate().getItemTagWhitelist()) {
                            ResourceLocation location = new ResourceLocation(tagName);
                            if (TagHelper.doesItemBelongToTag(itemStack.getItem(), location)) return true;
                        }
                        for (String itemName : parcel.getEstate().getItemWhitelist()) {
                            if (ModUtil.getName(itemStack.getItem()).equals(new ResourceLocation(itemName))) return true;
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
                    ClaimMyLand.LOGGER.debug("trying to use block {} in parcel -> {}",
                            state.getBlock().getName().getString(), parcel);

                    for (String tagName : parcel.getEstate().getBlockTagWhitelist()) {
                        if (TagHelper.doesBlockBelongToTag(state.getBlock(), new ResourceLocation(tagName))) return true;
                    }
                    for (String blockName : parcel.getEstate().getBlockWhitelist()) {
                        if (ModUtil.getName(state.getBlock()).equals(new ResourceLocation(blockName))) return true;
                    }
                    if (heldItem != null && !heldItem.isEmpty()) {
                        for (String tagName : parcel.getEstate().getItemTagWhitelist()) {
                            if (TagHelper.doesItemBelongToTag(heldItem.getItem(), new ResourceLocation(tagName))) return true;
                        }
                        for (String itemName : parcel.getEstate().getItemWhitelist()) {
                            if (ModUtil.getName(heldItem.getItem()).equals(new ResourceLocation(itemName))) return true;
                        }
                    }
                    return parcel.grantsAccess(player.getUUID(), heldItem);
                })
                .orElse(true);
    }

    /**
     * UUID-only variant. Checks cache, falls through to BST, updates server cache.
     * Does NOT send a network packet — use the ServerPlayer overload from event
     * handlers where the player object is available.
     */
    public static Optional<Parcel> resolveParcelCached(UUID playerId, ICoords coords, String dimension) {
        Optional<ParcelRegionCache.CacheEntry> cached = REGION_CACHE.getIfContains(playerId, coords, dimension);
        if (cached.isPresent()) {
            return Optional.of(cached.get().getParcel());
        }

        Optional<Parcel> resolved = resolveParcelAt(coords, coords);

        if (resolved.isPresent()) {
            REGION_CACHE.update(playerId, resolved.get());
        } else {
            REGION_CACHE.invalidatePlayer(playerId);
        }

        return resolved;
    }


    /**
     * ServerPlayer variant. Checks cache, falls through to BST, updates server
     * cache, and syncs the result to the client via CacheSyncPacket.
     * Use this from ModEvents where ServerPlayer is available.
     */
    public static Optional<Parcel> resolveParcelCached(ServerPlayer player, ICoords coords, String dimension) {
        UUID playerId = player.getUUID();

        Optional<ParcelRegionCache.CacheEntry> cached = REGION_CACHE.getIfContains(playerId, coords, dimension);
        if (cached.isPresent()) {
            // Cache hit — no BST, no packet needed (client already has this entry)
            return Optional.of(cached.get().getParcel());
        }

        // Cache miss — query BST
        Optional<Parcel> resolved = resolveParcelAt(coords, coords);

        if (resolved.isPresent()) {
            REGION_CACHE.update(playerId, resolved.get());
            // notify client so it can do instant protection checks
            CMLNetwork.syncCacheToPlayer(player, resolved.get());
        } else {
            REGION_CACHE.invalidatePlayer(playerId);
            // tell client they are in wilderness
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
    public static void syncParcelToClient(ServerPlayer player, ICoords coords, String dimension) {
        UUID playerId = player.getUUID();
        Optional<ParcelRegionCache.CacheEntry> cached = REGION_CACHE.get(playerId);

        // Early-exit only for true leaf parcel types — types that cannot contain
        // child parcels. CITIZEN and PLAYER are both leaves:
        //   PLAYER  — standalone parcel, cannot exist inside NATION or ZONE
        //   CITIZEN — innermost parcel in the NATION → ZONE → CITIZEN hierarchy
        // NATION and ZONE can both contain children, so the BST must run every tick
        // while inside either of them to detect when the player enters a child parcel.
        if (cached.isPresent() && cached.get().contains(coords, dimension)) {
            ParcelType cachedType = cached.get().getParcel().getType();
            if (cachedType == ParcelType.CITIZEN || cachedType == ParcelType.PLAYER) {
                return;
            }
        }

        // Cache miss, player outside cached bounds, or cached parcel is NATION/ZONE —
        // query BST to find the innermost parcel at this position.
        Optional<Parcel> resolved = resolveParcelAt(coords, coords);

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
     * Returns the parcel with the least area of all parcels at the given coords,
     * filtered to the specified dimension.
     * @author Mark Gottschling on Mar 09, 2026
     */
    public static Optional<Parcel> findLeastSignificant(ICoords coords, String dimension) {
        List<Parcel> parcels = ParcelRegistry.find(coords).stream()
                .filter(p -> dimension.equals(p.getDimension()))
                .toList();
        return findLeastSignificant(parcels);
    }

    /**
     * returns the parcel with the least area of all parcels at the given coords
     * @param coords
     * @return
     */
    private static Optional<Parcel> findLeastSignificant(ICoords coords) {
        return findLeastSignificant(ParcelRegistry.find(coords));
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
    public static Optional<Parcel> findMostSignificant(ICoords coords) {
        return findMostSignificant(ParcelRegistry.find(coords));
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
    public static boolean isFireSpreadPrevented(ICoords coords, BlockState state) {
        if (!Config.SERVER.protection.preventFireSpread.get()) {
            return false;
        }
        if (!state.is(ModTags.Blocks.FIRE_BLOCKS)) {
            return false;
        }
        return findLeastSignificant(coords)
                .map(parcel -> parcel.getEstate().isPreventFireSpread())
                .orElse(false);
    }

//    /*
//     * TODO updateOwner() methods are no longer called.
//     */
//    /**
//     * updates the owner of a parcel by creating a new estate and re-registering.
//     * broadcasts SyncParcelPacket to nearby clients.
//     */
//    public static synchronized void updateOwner(ServerLevel level, UUID parcelId, UUID newOwnerId) {
//        findByParcelId(parcelId).ifPresent(parcel -> updateOwner(level, parcel, newOwnerId));
//    }
//
//    public static synchronized void updateOwner(ServerLevel level, Parcel parcel, UUID newOwnerId) {
//        // unregister current owner
//        if (ObjectUtils.isNotEmpty(parcel.getOwnerId())) {
//            unregisterOwner(parcel);
//        }
//
//        // unregister estate if it only has this one parcel
//        Estate oldEstate = parcel.getEstate();
//        if (oldEstate.findParcels().size() <= 1) {
//            EstateRegistry.unregister(oldEstate);
//        }
//
//        // create new estate with the new owner
//        Estate newEstate = new EstateContext();
//        newEstate.setOwnerId(newOwnerId);
//
//        // update parcel
//        parcel.setEstate(newEstate);
//
//        // re-register owner and estate
//        registerOwner(parcel);
//        EstateRegistry.register(newEstate);
//
//        // broadcast updated parcel to nearby clients
//        CMLNetwork.syncParcelToTrackingPlayers(level, parcel);
//    }

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

    //    private static Optional<Parcel> resolveParcelAt(ICoords coords1, ICoords coords2) {
//        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, false, true);
//        if (intervals.isEmpty()) return Optional.empty();
//        List<Parcel> parcels = getAsParcels(intervals);
//        if (parcels.isEmpty()) {
//            TREE.delete(intervals.get(0)); // stale interval cleanup
//            return Optional.empty();
//        }
//        return intervals.size() > 1 ? findLeastSignificant(parcels) : Optional.of(parcels.get(0));
//    }
    private static Optional<Parcel> resolveParcelAt(ICoords coords1, ICoords coords2) {
        // NOTE: resolveParcelAt() is called from hasAccess() / hasInteractAccess(),
        // which do not currently receive a player UUID. The cache lookup by player
        // is wired at the event-handler level (ModEvents) in a later step, where
        // the UUID is available. This method remains the fallback BST path.
        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, false, true);
        if (intervals.isEmpty()) return Optional.empty();
        List<Parcel> parcels = getAsParcels(intervals);
        if (parcels.isEmpty()) {
            TREE.delete(intervals.get(0)); // stale interval cleanup
            return Optional.empty();
        }
        return intervals.size() > 1 ? findLeastSignificant(parcels) : Optional.of(parcels.get(0));
    }

    /**
     * Determines the conflict state for a proposed parcel box and its owner.
     * Returns 1 if:
     *   - the proposed border overlaps any existing parcel border (any owner), OR
     *   - the proposed border overlaps another owner's buffer zone.
     * Returns 0 if no conflict exists.
     */
    public static int resolveConflictState(Box proposedBox, UUID ownerId, UUID excludeParcelId, ParcelType placingType) {
        // rule 1: border-vs-border — any overlap is a conflict, excluding self and allowed ancestors
        List<Parcel> borderOverlaps = find(proposedBox).stream()
                .filter(p -> !p.getId().equals(excludeParcelId))
                .filter(p -> !isAllowedAncestor(p.getType(), placingType))
                .filter(p -> !isAllowedDescendant(p.getType(), placingType))
                .filter(p -> !isSameOwnerSibling(p, ownerId, placingType))
                .toList();
        if (!borderOverlaps.isEmpty()) {
            return 1;
        }

        // rule 2: border-vs-buffer — conflict only if different owner, excluding self and allowed ancestors
        List<Parcel> bufferOverlaps = findBuffer(proposedBox).stream()
                .filter(p -> !p.getId().equals(excludeParcelId))
                .filter(p -> !isAllowedAncestor(p.getType(), placingType))
                .filter(p -> !isAllowedDescendant(p.getType(), placingType))
                .toList();
        boolean foreignBufferConflict = bufferOverlaps.stream()
                .anyMatch(p -> !ownerId.equals(p.getOwnerId()));
        return foreignBufferConflict ? 1 : 0;
    }

    /**
     * Returns true if the enclosing parcel type is a permitted ancestor of the placing
     * parcel type under the NATION > ZONE > CITIZEN hierarchy. Permitted ancestors
     * are never conflicts.
     */
    private static boolean isAllowedAncestor(ParcelType enclosingType, ParcelType placingType) {
        return switch (placingType) {
            case ZONE    -> enclosingType == ParcelType.NATION;
            case CITIZEN -> enclosingType == ParcelType.NATION || enclosingType == ParcelType.ZONE;
            default      -> false; // NATION, PLAYER — no allowed ancestors
        };
    }

    private static boolean isAllowedDescendant(ParcelType enclosedType, ParcelType placingType) {
        return switch (placingType) {
            case NATION -> enclosedType == ParcelType.ZONE || enclosedType == ParcelType.CITIZEN;
            case ZONE   -> enclosedType == ParcelType.CITIZEN;
            default     -> false; // CITIZEN, PLAYER — no allowed descendants
        };
    }

//    private static boolean isSameOwnerSibling(Parcel p, UUID ownerId, ParcelType placingType) {
//        return p.getType() == placingType && p.getOwnerId().equals(ownerId);
//    }

    private static boolean isSameOwnerSibling(Parcel p, UUID ownerId, ParcelType placingType) {
        return p.getOwnerId().equals(ownerId);
    }

    // expose a detached parcel list
    public static List<Parcel> getParcels() {
        return new ArrayList<>(PARCELS_BY_COORDS.values());
    }
}
