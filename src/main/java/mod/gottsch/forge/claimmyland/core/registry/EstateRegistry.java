package mod.gottsch.forge.claimmyland.core.registry;

import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.EstateContext;
import mod.gottsch.forge.claimmyland.core.estate.NationEstate;
import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;

import java.util.*;
import java.util.stream.Collectors;

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

    public static synchronized void register(Estate estate) {
        ESTATES_BY_ID.put(estate.getId(), estate);
        if (estate.getOwnerId() != null) {
            ESTATES_BY_OWNER.computeIfAbsent(estate.getOwnerId(), mapper -> new HashSet<>()).add(estate);
        }
        estate.getPlayerWhitelist().forEach(friend -> {
            ESTATES_BY_FRIENDS.computeIfAbsent(friend, mapper -> new HashSet<>()).add(estate);
        });
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

    public static Optional<Estate> get(UUID estateId) {
        return Optional.ofNullable(ESTATES_BY_ID.get(estateId));
    }

    /** returns a cloned set ie changes to the set will not update the registry. */
    public static Set<Estate> getAll() {
        return new HashSet<>(ESTATES_BY_ID.values());
    }

    public static Set<Estate> findByOwner(UUID ownerId) {
        return ESTATES_BY_OWNER.getOrDefault(ownerId, Collections.emptySet());
    }

    /**
     * retrieves a set of all estates by friend
     */
    public static Set<Estate> findByFriend(UUID id) {
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

    public static boolean hasName(String name) {
        return ESTATES_BY_ID.values().stream()
                .anyMatch(estate -> estate.getName().equalsIgnoreCase(name));
    }

    public static Optional<Estate> findByName(String nationName) {
        return ESTATES_BY_ID.values().stream()
                .filter(estate -> estate.getName().equalsIgnoreCase(nationName))
                .findFirst();
    }
}