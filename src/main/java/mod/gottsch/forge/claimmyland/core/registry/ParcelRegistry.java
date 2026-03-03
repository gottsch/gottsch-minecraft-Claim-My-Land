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
import mod.gottsch.forge.claimmyland.core.estate.*;
import mod.gottsch.forge.claimmyland.core.parcel.*;
import mod.gottsch.forge.claimmyland.core.util.TagHelper;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.bst.CoordsInterval;
import mod.gottsch.forge.gottschcore.bst.CoordsIntervalTree;
import mod.gottsch.forge.gottschcore.bst.IInterval;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

    /*
     * nation caches
     */
    @Deprecated
    private static final Multimap<UUID, Parcel> NATIONS_BY_ID = ArrayListMultimap.create();


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
        NATIONS_BY_ID.clear();
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

    public static String toJson() {
        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper(gson);
        return mapper.writeValueAsString(PARCELS_BY_COORDS);
    }

    /**
     *
     * @param tag
     */
    public static void convertV1ToV2(CompoundTag tag) {
        Map<ICoords, Parcel> byCoords = new HashMap<>();
        ClaimMyLand.LOGGER.debug("converting registry from v1 to v2...");

        if (tag.contains(PARCELS_KEY)) {
            // 1. load parcels into local map
            ListTag list = tag.getList(PARCELS_KEY, Tag.TAG_COMPOUND);

            list.stream()
                    .map(element -> (CompoundTag) element)
                    .peek(e -> ClaimMyLand.LOGGER.debug("processing v1 parcel element..."))
                    .forEach(e -> {
                        ParcelType type = e.contains(Parcel.TYPE)
                                ? ParcelType.fromString(e.getString(Parcel.TYPE))
                                : ParcelType.NONE;

                        ParcelTypeRegistry.create(type).ifPresent(parcel -> {
                            // load the legacy parcel
                            // NOTE the new parcel structure is used, so method calls like setWhitelist()
                            //  are actually updating the estate.
                            loadV1(parcel, e);

                            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                                ClaimMyLand.LOGGER.debug("loaded v1 parcel -> {}", parcel);
                            }

                            // add parcel to the local map
                            byCoords.put(parcel.getCoords(), parcel);
                        });
                    });


            // 2. walk the parcel map, building the nation-level parcels
            byCoords.values().stream()
                    .filter(p -> p instanceof NationParcel)
                    .map(p -> (NationParcel)p)
                    .forEach(np -> {
                        NationEstateContext estate = (NationEstateContext) np.getEstate();
                        // copy properties
//                                        estate.setName(np.getName());
//                                        estate.setOwnerId(np.getOwnerId());
//                                        estate.setBlockWhitelist(np.getBlockWhitelist());
//                                        estate.setBlockTagWhitelist(np.getBlockWhitelist());
//                                        estate.setItemWhitelist(np.getBlockWhitelist());
//                                        estate.setItemTagWhitelist(np.getItemTagWhitelist());
//                                        estate.setPlayerWhitelist(np.getPlayerWhitelist());
                        estate.setAccessType(NationAccessType.fromString(np.getBorderType().name()));
                        estate.setPlayerBlacklist(new HashSet<>(np.getBlacklist()));
//                        EstateRegistry.register(estate);
                        if (estate.getOwnerId() != null) {
                            ParcelRegistry.register(np);
                        }
                    });

            // walk he parcel map again, building the rest
            byCoords.values().stream()
                    .filter(p -> !(p instanceof NationParcel))
                    .forEach(p -> {
                        Estate estate = p.getEstate();
                        // setup default estate
//                                        estate.setOwnerId(p.getOwnerId());
//                                        estate.setBlockWhitelist(p.getBlockWhitelist());
//                                        estate.setBlockTagWhitelist(p.getBlockWhitelist());
//                                        estate.setItemWhitelist(p.getBlockWhitelist());
//                                        estate.setItemTagWhitelist(p.getItemTagWhitelist());
//                                        estate.setPlayerWhitelist(p.getPlayerWhitelist());

                        // setup the nation estate if a nation id exists
                        if (p instanceof NationalizedParcel nationalizedParcel) {
                            // NOTE load nationId from tag since new parceling loading doesn't load nation id ????
                            if (nationalizedParcel.getNationId() == null) {
                                // TODO log warning
                                return;
                            }

                            Optional<Estate> nationEstate = EstateRegistry.get(nationalizedParcel.getNationId());
                            if (nationEstate.isEmpty()) {
                                // TODO log warning
                                return;
                            }

                            if (estate.getOwnerId() == null) {
                                estate.setRelinquished(true);
                                estate.setOwnerId(nationEstate.get().getOwnerId());
                            }
                            nationalizedParcel.setNationEstate((NationEstate) nationEstate.get());
                        }
                        ParcelRegistry.register(p);
                    });
        }
    }

    public static synchronized void loadV1(Parcel parcel, CompoundTag tag) {
        // standard parcel
        if (tag.contains(AbstractParcel.ID_KEY)) {
            parcel.setId(tag.getUUID(AbstractParcel.ID_KEY));
        } else {
            parcel.setId(UUID.randomUUID());
        }
        if (tag.contains(AbstractParcel.NATION_ID_KEY)) {
            parcel.setNationId(tag.getUUID(AbstractParcel.NATION_ID_KEY));
        }
        if (tag.contains(AbstractParcel.NAME_KEY)) {
            parcel.setName(tag.getString(AbstractParcel.NAME_KEY));
        }
        if (tag.contains(AbstractParcel.OWNER_KEY)) {
            parcel.setOwnerId(tag.getUUID(AbstractParcel.OWNER_KEY));
        }
        if (tag.contains(AbstractParcel.DEED_KEY)) {
            parcel.setDeedId(tag.getUUID(AbstractParcel.DEED_KEY));
        }
        if (tag.contains(AbstractParcel.TYPE)) {
            parcel.setType(ParcelType.valueOf(tag.getString(AbstractParcel.TYPE)));
        }
        if (tag.contains(AbstractParcel.COORDS_KEY)) {
            parcel.setCoords(Coords.EMPTY.load(tag.getCompound(AbstractParcel.COORDS_KEY)));
        }
        if (tag.contains(AbstractParcel.SIZE_KEY)) {
            parcel.setSize(Box.load(tag.getCompound(AbstractParcel.SIZE_KEY)));
        }
        if (tag.contains(AbstractParcel.WHITELIST_KEY)) {
            ListTag list = tag.getList(AbstractParcel.WHITELIST_KEY, Tag.TAG_COMPOUND);
            list.forEach(element -> {
                CompoundTag uuidTag = ((CompoundTag)element);
                if (uuidTag.contains(AbstractParcel.ID_KEY)) {
                    ClaimMyLand.LOGGER.debug("loading {} to whitelist", uuidTag.getUUID(AbstractParcel.ID_KEY));
                    parcel.getWhitelist().add(uuidTag.getUUID(AbstractParcel.ID_KEY));
                }
            });
        }

        if (tag.contains(AbstractParcel.BLOCK_TAG_WHITELIST_KEY)) {
            ListTag list = tag.getList(AbstractParcel.BLOCK_TAG_WHITELIST_KEY, Tag.TAG_STRING);
            list.forEach(element -> {
                String blockTag = element.getAsString();
                parcel.getBlockTagWhitelist().add(blockTag);
            });
        }

        if (tag.contains(AbstractParcel.BLOCK_WHITELIST_KEY)) {
            ListTag list = tag.getList(AbstractParcel.BLOCK_WHITELIST_KEY, Tag.TAG_STRING);
            list.forEach(element -> {
                String block = element.getAsString();
                parcel.getBlockWhitelist().add(block);
            });
        }

        if (tag.contains(AbstractParcel.ITEM_TAG_WHITELIST_KEY)) {
            ListTag list = tag.getList(AbstractParcel.ITEM_TAG_WHITELIST_KEY, Tag.TAG_STRING);
            list.forEach(element -> {
                String itemTag = element.getAsString();
                parcel.getItemTagWhitelist().add(itemTag);
            });
        }

        if (tag.contains(AbstractParcel.ITEM_WHITELIST_KEY)) {
            ListTag list = tag.getList(AbstractParcel.ITEM_WHITELIST_KEY, Tag.TAG_STRING);
            list.forEach(element -> {
                String item = element.getAsString();
                parcel.getItemWhitelist().add(item);
            });
        }

        if (tag.contains("foundedTime")) {
            parcel.setFoundedTime(tag.getLong("foundedTime"));
        }
        if (tag.contains("ownerTime")) {
            parcel.setOwnerTime(tag.getLong("ownerTime"));
        }
        if (tag.contains("abandonedTime")) {
            parcel.setRelinquishedTime(tag.getLong("abandonedTime"));
        }

        // parcel specific properties
        if (parcel instanceof NationParcel nationParcel) {
            if (tag.contains("borderType")) {
                try {
                    nationParcel.setBorderType(NationBorderType.valueOf(tag.getString("borderType")));
                } catch(Exception e) {
                    ClaimMyLand.LOGGER.warn("unable to parse and load borderType - using default CLOSED");
                    nationParcel.setBorderType(NationBorderType.CLOSED);
                }
            }

            if (tag.contains("blacklist")) {
                ListTag list = tag.getList("blacklist", Tag.TAG_STRING);
                list.forEach(element -> {
                    StringTag uuidTag = ((StringTag)element);
                    nationParcel.getBlacklist().add(UUID.fromString(uuidTag.getAsString()));
                });
            }
        }
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

            // add to nations map
            registerNation(parcel);

            //register estate
            EstateRegistry.register(parcel.getEstate());

        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("error attempting to register parcel", parcel.getId());
            return Optional.empty();
        }
        return Optional.of(parcel);
    }

    public static synchronized void registerNation(Parcel parcel) {
        if (parcel.getType() == ParcelType.NATION) {
            NATIONS_BY_ID.put(((NationParcel)parcel).getEstate().getId(), parcel);
        }
    }

    public static  synchronized void registerTree(Parcel parcel) {
        IInterval<UUID> interval = TREE.insert(
                new CoordsInterval<>(parcel.getMinCoords(), parcel.getMaxCoords(), parcel.getOwnerId())
        );
    }

    public static synchronized void registerCoords(Parcel parcel) {
        PARCELS_BY_COORDS.put(parcel.getMinCoords(), parcel);
    }

    /**
     * removes a parcel from the registries/maps
     * @param parcel
     */
    public static synchronized void unregisterParcel(Parcel parcel) {
        // remove from the TREE
//        TREE.delete(new CoordsInterval<>(new CoordsInterval<UUID>(parcel.getMinCoords(), parcel.getMaxCoords(), parcel.getOwnerId())));
        unregisterTree(parcel);
        // remove from PARCELS registries
//        List<Parcel> parcels = PARCELS_BY_OWNER.get(parcel.getOwnerId());
//        if (!parcels.isEmpty()) {
//            parcels.removeIf(p -> p.getId().equals(parcel.getId()));
//        }
        unregisterOwner(parcel);

        // remove from friends
//        parcel.getWhitelist().forEach(friend -> {
//            List<Parcel> parcelList = PARCELS_BY_FRIENDS.get(friend);
//            if (!parcelList.isEmpty()) {
//                parcelList.removeIf(p -> p.getId().equals(parcel.getId()));
//            }
//        });
        unregisterFriends(parcel);

        // remove from coords (also removes ability of Estate finding parcel ie Estate.getParcels() )
        unregisterCoords(parcel);
//        PARCELS_BY_COORDS.remove(parcel.getMinCoords());

        // remove from buffer map
        unregisterBuffer(parcel);
//        Box inflatedBox = inflateParcelBox(parcel);
//        BUFFER_PARCELS_BY_COORDS.remove(inflatedBox.getMinCoords());
//        // TODO test if this only deletes the one, or everything in this area.
//        // remove from buffer tree
//        BUFFER_TREE.delete(new CoordsInterval<>(new CoordsInterval<UUID>(inflatedBox.getMinCoords(), inflatedBox.getMaxCoords(), parcel.getOwnerId())));

        // if nation remove from special map/registry
        unregisterNation(parcel);

        //remove the estate if no more parcels
        if (parcel.getEstate().findParcels().isEmpty()) {
            EstateRegistry.unregister(parcel.getEstate());
        }
    }

    public static synchronized void unregisterBuffer(Parcel parcel) {
        Box inflatedBox = inflateParcelBox(parcel);
        BUFFER_PARCELS_BY_COORDS.remove(inflatedBox.getMinCoords());
        // TODO test if this only deletes the one, or everything in this area.
        // remove from buffer tree
        BUFFER_TREE.delete(new CoordsInterval<>(new CoordsInterval<UUID>(inflatedBox.getMinCoords(), inflatedBox.getMaxCoords(), parcel.getOwnerId())));
    }

    public static synchronized void unregisterCoords(Parcel parcel) {
        PARCELS_BY_COORDS.remove(parcel.getMinCoords());
    }

    public static synchronized void unregisterTree(Parcel parcel) {
        TREE.delete(new CoordsInterval<>(new CoordsInterval<UUID>(parcel.getMinCoords(), parcel.getMaxCoords(), parcel.getOwnerId())));
    }

    public static synchronized void unregisterNation(Parcel parcel) {
        if (parcel.getType() == ParcelType.NATION) {
            NATIONS_BY_ID.remove(((NationParcel)parcel).getNationId(), parcel);

            ParcelRegistry.findChildrenByNationId(parcel.getNationId())
                    .forEach(p -> {
                        // TODO calling this may cause Concurrent operation exceptions.
                        if (p.getType() == ParcelType.ZONE) {
                            ParcelRegistry.unregisterParcel(p);
                        } else {
                            p.setType(ParcelType.PLAYER);
                            p.setNationId(null);
                            // TODO remove borders if any this would have to be able to call the Border Entity Block - how?!
                        }
                    });
        }
    }

    /**
     * removes all parcels by player
     * @param ownerId
     */
    public static synchronized void removeParcel(Level level, UUID ownerId) {

        // get all parcels excluding zones as they will be handled when handling nations
        List<Parcel> parcels = Optional.ofNullable(PARCELS_BY_OWNER.get(ownerId))
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(p -> p.getType() != ParcelType.ZONE).toList();

        if (!parcels.isEmpty()) {
            for (Parcel parcel : parcels) {
                unregisterParcel(parcel);
            }
            // TODO can this not be replace with removeParcel(parcel) ?
//                // remove the border
//                BlockEntity blockEntity = level.getBlockEntity(p.getCoords().toPos());
//                if (blockEntity instanceof FoundationStoneBlockEntity) {
//                    ((FoundationStoneBlockEntity)blockEntity).removeParcelBorder(level, p.getCoords());
//                }
//
//                // remove nations parcels (and zones)
//                removeFromNationsRegistry(p);
//
//                // remove from friends
//                p.getWhitelist().forEach(friend -> {
//                    List<Parcel> parcelList = PARCELS_BY_FRIENDS.get(friend);
//                    if (!parcelList.isEmpty()) {
//                        parcelList.removeIf(fp -> fp.getId().equals(p.getId()));
//                    }
//                });
//
//                // remove from coords
//                PARCELS_BY_COORDS.remove(p.getMinCoords());
//                // remove from buffer map
//                Box inflatedBox = inflateParcelBox(p);
//                BUFFER_PARCELS_BY_COORDS.remove(inflatedBox.getMinCoords());
//            }
        }
//        PARCELS_BY_OWNER.remove(ownerId);

        // TODO needs to remove the estate if the estate only has the one parcel
    }

    @Deprecated
    public static boolean abandonParcel(UUID parcelId) {
        Optional<Parcel> abandonedParcel = findByParcelId(parcelId);
        return abandonedParcel.filter(ParcelRegistry::abandonParcel).isPresent();
    }

    @Deprecated
    public static boolean abandonParcel(Parcel parcel) {
        if (parcel == null) return false;
        if (parcel.getType() != ParcelType.CITIZEN) return false;

        // unregister parcel
        ParcelRegistry.unregisterParcel(parcel);

        // NOTE this will change if a "relinquish" flag is added instead of clearing ownership
        // create new estate
        parcel.setEstate(new EstateContext()); // NOTE estate will not have an owner assigned

        // re-register parcel without owner/friends
        registerTree(parcel);
        registerCoords(parcel);
        registerBuffer(parcel);
        EstateRegistry.register(parcel.getEstate());

//        if (PARCELS_BY_OWNER.containsKey(parcel.getOwnerId())) {
//            List<Parcel> parcels = PARCELS_BY_OWNER.get(parcel.getOwnerId());
//            if (!parcels.isEmpty()) {
//                parcels.removeIf(p -> p.getId().equals(parcel.getId()));
//            }
//            // remove from friends
//            parcel.getWhitelist().forEach(friend -> {
//                if (PARCELS_BY_FRIENDS.containsKey(friend)) {
//                    List<Parcel> friendsParcels = PARCELS_BY_FRIENDS.get(friend);
//                    if (!friendsParcels.isEmpty()) {
//                        friendsParcels.removeIf(p -> p.getId().equals(parcel.getId()));
//                    }
//                }
//            });
//            // remove owner
//            parcel.setOwnerId(null);

        return true;
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
    @Deprecated
    public static List<Parcel> findByNationId(UUID nationId) {
        List<Parcel> parcels = new ArrayList<>(1);
        for (Parcel parcel : PARCELS_BY_COORDS.values()) {
            if ((parcel instanceof NationParcel) && ((NationParcel) parcel).getNationId().equals(nationId)) {
                parcels.add(parcel);
                break;
            }
        }
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

    public static Set<Parcel> findAllByEstateId(UUID estateId) {
        return PARCELS_BY_COORDS.values().stream()
                .filter(parcel -> parcel.getEstate().getId().equals(estateId))
                .collect(Collectors.toSet());
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

    public static boolean hasInteractAccess(ICoords coords, UUID entityId, ItemStack stack) {
        return hasInteractAccess(coords, coords, entityId, stack);
    }

    @Deprecated
    // TODO keep for now, might refactor
    public static boolean hasInteractAccess(ICoords coords, UUID entityId, BlockState state) {
        return hasInteractAccess(coords, entityId, state, ItemStack.EMPTY);
    }

    public static boolean hasInteractAccess(ICoords coords, UUID entityId, BlockState state, ItemStack heldItem ) {
        return hasInteractAccess(coords, coords, entityId, state, heldItem);
    }

    public static boolean hasAccess(ICoords coords1, ICoords coords2, UUID entityId, ItemStack itemStack) {
        // this is the fastest lookup
        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, false, true );
        if (!intervals.isEmpty()) {
            Parcel parcel;
            // convert to parcels
            List<Parcel> parcels = getAsParcels(intervals);

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
            return (itemStack !=null && !itemStack.isEmpty()) ? parcel.grantsAccess(entityId, itemStack) : parcel.grantsAccess(entityId);
        }
        return true;
    }

    /*
     *
     */
    public static boolean hasInteractAccess(ICoords coords1, ICoords coords2, UUID entityId, ItemStack itemStack) {
        // this is the fastest lookup
        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, false, true );
        if (!intervals.isEmpty()) {
            Parcel parcel;
            // convert to parcels
            List<Parcel> parcels = getAsParcels(intervals);

            if (parcels.isEmpty()) {
                return true;
            }
            if (intervals.size() > 1) {
                // find the least significant parcel
                Optional<Parcel> parcelOptional = findLeastSignificant(parcels);
                if (parcelOptional.isPresent()) {
                    parcel = parcelOptional.get();
                } else {
                    // this is a case where the interval still exists but the parcel has been removed
                    TREE.delete(intervals.get(0));
                    return true;
                }
            } else {
                parcel = parcels.get(0);
            }

            // if you have an item in your hand, do whitelist short-circuit tests
            if (itemStack != null && !itemStack.isEmpty()) {
                ClaimMyLand.LOGGER.debug("trying to use item {} in parcel -> {}", itemStack.getDisplayName().getString(), parcel);
                // test the item against the whitelisted item tags for the parcel
                for (String tagName : parcel.getItemTagWhitelist()) {
                    ResourceLocation location = new ResourceLocation(tagName);
                    ClaimMyLand.LOGGER.debug("creating tag for parcel item tag -> {}", location.toString());
                    // get the tag from the resource key
                    if (TagHelper.doesItemBelongToTag(itemStack.getItem(), location)) {
                        return true;
                    }
                }

                ClaimMyLand.LOGGER.debug("value of item white list -> {}", parcel.getItemWhitelist());
                for (String itemName : parcel.getItemWhitelist()) {
                    ResourceLocation location = new ResourceLocation(itemName);
                    ClaimMyLand.LOGGER.debug("comparing item locations for held item -> {}", itemName);
                    if (ModUtil.getName(itemStack.getItem()).equals(location)) {
                        return true;
                    }
                }
            }

            // check player's access
            return (itemStack !=null && !itemStack.isEmpty()) ? parcel.grantsAccess(entityId, itemStack) : parcel.grantsAccess(entityId);
        }
        return true;
    }

    public static boolean hasAccess(ICoords coords1, ICoords coords2, UUID entityId, BlockState state, ItemStack heldItem) {
        // this is the fastest lookup
        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, false, true );
        if (!intervals.isEmpty()) {
            Parcel parcel;
            // convert to parcels
            List<Parcel> parcels = getAsParcels(intervals);

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
            return parcel.grantsAccess(entityId, heldItem);
        }
        return true;
    }

    public static boolean hasInteractAccess(ICoords coords1, ICoords coords2, UUID entityId, BlockState state, ItemStack heldItem) {
        // TODO all this code getting the parcel is the same and can be extracted to its own method

        // this is the fastest lookup
        List<IInterval<UUID>> intervals = findRaw(coords1, coords2, false, true );
        if (!intervals.isEmpty()) {
            Parcel parcel;
            // convert to parcels
            List<Parcel> parcels = getAsParcels(intervals);

            if (parcels.isEmpty()) {
                return true;
            }
            if (intervals.size() > 1) {
                // find the least significant parcel
                Optional<Parcel> parcelOptional = findLeastSignificant(parcels);
                if (parcelOptional.isPresent()) {
                    parcel = parcelOptional.get();
                } else {
                    // this is a case where the interval still exists but the parcel has been removed
                    TREE.delete(intervals.get(0));
                    return true;
                }
            } else {
                parcel = parcels.get(0);
            }

            // TODO move Item and Tag whitelist back here from Parcel - can't control interact permission from Parcels

            ClaimMyLand.LOGGER.debug("trying to use block {} in parcel -> {}", state.getBlock().getName().getString(), parcel);
            // TODO block tag and block could be merged into one list, where tags are prefixed with # and would have to be removed before checking
            // test the block against the whitelisted block tags for the parcel
            for (String tagName : parcel.getBlockTagWhitelist()) {
                ResourceLocation location = new ResourceLocation(tagName);
                ClaimMyLand.LOGGER.debug("creating tag for parcel block tag -> {}", location.toString());
                // get the tag from the resource key
                if (TagHelper.doesBlockBelongToTag(state.getBlock(), location)) {
                    return true;
                }
            }

            // check BlockWhitelist
            ClaimMyLand.LOGGER.debug("value of block white list -> {}", parcel.getBlockWhitelist());
            for (String blockName : parcel.getBlockWhitelist()) {
                ResourceLocation location = new ResourceLocation(blockName);
                ClaimMyLand.LOGGER.debug("comparing block locations for parcel block -> {}", blockName);
                if (ModUtil.getName(state.getBlock()).equals(location)) {
                    return true;
                }
            }

            // if you have an item in your hand, do whitelist short-circuit tests
            if (heldItem != null && !heldItem.isEmpty()) {
                ClaimMyLand.LOGGER.debug("trying to use item {} in parcel -> {}", heldItem.getDisplayName().getString(), parcel);
                // test the item against the whitelisted item tags for the parcel
                for (String tagName : parcel.getItemTagWhitelist()) {
                    ResourceLocation location = new ResourceLocation(tagName);
                    ClaimMyLand.LOGGER.debug("creating tag for parcel item tag -> {}", location.toString());
                    // get the tag from the resource key
                    if (TagHelper.doesItemBelongToTag(heldItem.getItem(), location)) {
                        return true;
                    }
                }

                ClaimMyLand.LOGGER.debug("value of item white list -> {}", parcel.getItemWhitelist());
                for (String itemName : parcel.getItemWhitelist()) {
                    ResourceLocation location = new ResourceLocation(itemName);
                    ClaimMyLand.LOGGER.debug("comparing item locations for held item -> {}", itemName);
                    if (ModUtil.getName(heldItem.getItem()).equals(location)) {
                        return true;
                    }
                }
            }

            return parcel.grantsAccess(entityId, heldItem);
        }
        return true;
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
     * returns the most significant (largest area) parcel at coords,
     * optionally filtered by type. used to find the containing nation/zone parcel.
     */
    public static Optional<Parcel> findMostSignificant(ICoords coords) {
        return findMostSignificant(ParcelRegistry.find(coords));
    }

    public static Optional<Parcel> findMostSignificant(List<Parcel> parcels, ParcelType... types) {
        Set<ParcelType> typeSet = Set.of(types);
        return parcels.stream()
                .filter(p -> typeSet.isEmpty() || typeSet.contains(p.getType()))
                .max(Comparator.comparingLong(Parcel::getArea));
    }

    public static List<UUID> getOwnerIds() {
        return PARCELS_BY_OWNER.keySet().stream().toList();
    }

    public static List<Parcel> getNations() {
        List<Parcel> parcels = new ArrayList<>();
        NATIONS_BY_ID.entries().forEach(e -> parcels.add(e.getValue()));
        return parcels;
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
        // unregister parcel
        ParcelRegistry.unregisterParcel(parcel);

        // create new estate and update parcel
        // TODO use factory (might be a nation)

        // save old estate
        Estate oldEstate = parcel.getEstate();

        Estate estate = EstateTypeRegistry.create(parcel.isNation() ? EstateTypeRegistry.NATION_ESTATE_TYPE : EstateTypeRegistry.ESTATE_TYPE);
        estate.setOwnerId(newOwnerUuid);
        estate.setName(oldEstate.getName());
        estate.setParcelType(oldEstate.getParcelType());

        parcel.setEstate(estate);
        parcel.setOwnerTime(level.getGameTime());

        // re-register parcel
        ParcelRegistry.register(parcel);
    }

    @Deprecated
    public static boolean updateOwner(UUID parcelId, UUID ownerId) {
        Optional<Parcel> parcel = findByParcelId(parcelId);
        if (parcel.isPresent()) {
            // unregister owner for parcel
            if (!ObjectUtils.isEmpty(parcel.get().getOwnerId())) {
                ParcelRegistry.unregisterOwner(parcel.get());
            }
            // unregister estate if it only has the one parcel
            Estate estate = parcel.get().getEstate();
            if (estate.findParcels().size() <= 1) {
                EstateRegistry.unregister(estate);
            }
            // create a new estate
            estate = new EstateContext();
            // update estate
            estate.setOwnerId(ownerId);
            // update parcel
            parcel.get().setEstate(estate);
            // register parcel owner
            ParcelRegistry.registerOwner(parcel.get());
            // register estate
            EstateRegistry.register(estate);

//            parcel.get().setOwnerId(ownerId);
//            if (!PARCELS_BY_OWNER.containsKey(ownerId)) {
//                // create new list entry
//                PARCELS_BY_OWNER.put(ownerId, new ArrayList<>());
//            }
//            List<Parcel> parcels = PARCELS_BY_OWNER.get(ownerId);
//            parcels.add(parcel.get());
            return true;
        } else {
            return false;
        }


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
        if (ObjectUtils.isNotEmpty(parcel.getWhitelist())) {
            // add to parcels_by_friends
            parcel.getWhitelist().forEach(friend ->
                    PARCELS_BY_FRIENDS.computeIfAbsent(friend, k -> new ArrayList<>())
                            .add(parcel)
            );
        }
    }

    public static void unregisterFriends(Parcel parcel) {
        parcel.getWhitelist().forEach(friend -> {
            List<Parcel> parcelList = PARCELS_BY_FRIENDS.get(friend);
            if (!parcelList.isEmpty()) {
                parcelList.removeIf(p -> p.getId().equals(parcel.getId()));
            }
        });
    }

    // expose a detached parcel list
    public static List<Parcel> getParcels() {
        return new ArrayList<>(PARCELS_BY_COORDS.values());
    }
}
