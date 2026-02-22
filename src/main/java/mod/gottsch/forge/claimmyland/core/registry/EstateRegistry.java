package mod.gottsch.forge.claimmyland.core.registry;

import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.EstateContext;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;

import java.util.*;

/**
 * @author by Mark Gottschling on 1/28/2026
 */
public class EstateRegistry {
    /*
     * map of estates by id
     */
    private static final Map<UUID, Estate> ESTATES_BY_ID = new HashMap<>();
    /*
     * map of estates by owner id. convenience map
     */
    private static final Map<UUID, Set<Estate>> ESTATES_BY_OWNER = new HashMap<>();
    /*
     * map of estates by friend's id. a friend is a player who is listed in a estate's friends (player) whitelist.
     */
    private static final Map<UUID, Set<Estate>> ESTATES_BY_FRIENDS = new HashMap<>();

    public EstateRegistry() {

    }

    public static synchronized void clear() {
        ESTATES_BY_ID.clear();
        ESTATES_BY_OWNER.clear();
        ESTATES_BY_FRIENDS.clear();
    }

//    public static synchronized CompoundTag save(CompoundTag tag) {
//        return tag;
//    }
//
//    public static synchronized void load(CompoundTag tag) {
//        ClaimMyLand.LOGGER.debug("loading registry...");
//        clear();
//    }

    public static synchronized void register(Estate estate) {
        ESTATES_BY_ID.put(estate.getId(), estate);
        if (estate.getOwnerId() != null) {
            ESTATES_BY_OWNER.computeIfAbsent(estate.getOwnerId(), mapper -> new HashSet<>()).add(estate);
        }
        estate.getPlayerWhitelist().forEach(friend -> {
            ESTATES_BY_FRIENDS.computeIfAbsent(friend, mapper -> new HashSet<>()).add(estate);
        });
    }

    public static Set<Estate> getByOwner(UUID ownerId) {
        return ESTATES_BY_OWNER.getOrDefault(ownerId, Collections.emptySet());
    }

    public static Optional<Estate> get(UUID claimId) {
        return Optional.ofNullable(ESTATES_BY_ID.get(claimId));
    }

    public static void unregister(Estate estate) {
        // remove estate from all registries
        Set<Estate> estates = ESTATES_BY_OWNER.get(estate.getOwnerId());
        if (!estates.isEmpty()) {
            estates.removeIf(e -> e.getId().equals(estate.getId()));
        }

        estate.getPlayerWhitelist().forEach(friend -> {
            Set<Estate> friendsEstates = ESTATES_BY_FRIENDS.get(friend);
            if (!friendsEstates.isEmpty()) {
                friendsEstates.removeIf(e -> e.getId().equals(estate.getId()));
            }
        });

        ESTATES_BY_ID.remove(estate.getId());
    }

    /**
     * retrieves a set of all estates by friend
     */
    public static Set<Estate> getByFriend(UUID id) {
        Set<Estate> estates = ESTATES_BY_FRIENDS.get(id);
        return estates == null ? Collections.emptySet() : estates;
    }

    public static List<UUID> getOwnerIds() {
        return ESTATES_BY_OWNER.keySet().stream().toList();
    }

    public static Estate transfer(Estate estate, UUID newOwnerUuid) {
        // remove estate from registry
        EstateRegistry.unregister(estate);

        // save the parcels
        Set<Parcel> parcels =  estate.findParcels();

        // remove parcels from registry
        parcels.forEach(ParcelRegistry::unregisterParcel);

        // create a new estate
        EstateContext newEstate = new EstateContext(newOwnerUuid);

        parcels.forEach(parcel -> {
            // update parcels' estate
            parcel.setEstate(newEstate);
            //  add parcels to registry
            ParcelRegistry.register(parcel);
        });

        // add estate to registry
        EstateRegistry.register(newEstate);

        return newEstate;
    }

    public static boolean hasName(String newName) {
        return ESTATES_BY_ID.values().stream()
                .anyMatch(estate -> estate.getName().equalsIgnoreCase(newName));
    }

}