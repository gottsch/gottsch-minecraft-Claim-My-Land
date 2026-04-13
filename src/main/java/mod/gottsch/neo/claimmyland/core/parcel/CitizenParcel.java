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
package mod.gottsch.neo.claimmyland.core.parcel;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.estate.NationEstate;
import mod.gottsch.neo.claimmyland.core.estate.NationEstateContext;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.util.ModUtil;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * @author Mark Gottschling on Sep 14, 2024
 */
public class CitizenParcel extends AbstractClaimableParcel implements NationalizedParcel {

    // nation-ownership token
    private NationEstate nationEstate;

    /**
     * no-arg constructor
     */
    private CitizenParcel() {
        super();
        setType(ParcelType.CITIZEN);
        getEstate().setParcelType(getType());
        this.nationEstate = new NationEstateContext();
    }

    private CitizenParcel(NationEstate nationEstate) {
        super();
        setType(ParcelType.CITIZEN);
        setNationEstate(nationEstate);

        Estate estate = getEstate();
        estate.setParcelType(getType());
        estate.setName(estate.defaultName(nationEstate));
        estate.setBlockWhitelist(nationEstate.getBlockWhitelist());
        estate.setBlockTagWhitelist(nationEstate.getBlockTagWhitelist());
        estate.setItemWhitelist(nationEstate.getItemWhitelist());
        estate.setItemTagWhitelist(nationEstate.getItemTagWhitelist());
        estate.setPlayerWhitelist(nationEstate.getPlayerWhitelist());
        estate.setEntitySpawnTagWhitelist(nationEstate.getEntitySpawnTagWhitelist());
        estate.setEntitySpawnWhitelist(nationEstate.getEntitySpawnWhitelist());
    }

    /**
     * public factories
     */
    public static CitizenParcel create() {
        return new CitizenParcel();
    }

    public static CitizenParcel create(NationEstate nationEstate) {
        return new CitizenParcel(nationEstate);
    }

    public static CitizenParcel create(NationParcel nation) {
        return create((NationEstate) nation.getEstate());
    }

    /**
     * called by the existing parcel
     * @param virtualParcel
     * @return
     */
    public boolean grantsAccess(Parcel virtualParcel) {
        if (!getEstate().isRelinquished() || virtualParcel.getArea() < getArea()) {
            return false;
        }

        /*
         // IF using Java 21+
        return switch (virtualParcel) {
            case PlayerParcel p -> accessType == NationAccessType.OPEN;
            case CitizenParcel cp -> accessType == NationAccessType.OPEN
                    || isSameNation(cp);
            default -> false;
        };
         */
        if (virtualParcel.isPlayer()) {
            return getAccessType() == NationAccessType.OPEN;
        }

        if (virtualParcel.isCitizen()) {
            return getAccessType() == NationAccessType.OPEN || isSameNation((CitizenParcel)virtualParcel);
        }

        return false;
    }

    @Override
    public boolean hasAccessTo(Parcel existingParcel) {
        return switch (existingParcel.getType()) {
            case CITIZEN, ZONE -> true;
            default -> false;
        };
    }

    @Override
    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return getDeedId().equals(blockEntity.getDeedId());
    }

    /**
     * @author Mark Gottschling — PlacementResult refactor on Apr 12, 2026
     */
    /**
     * @author Mark Gottschling — PlacementResult refactor on Apr 12, 2026
     */
    @Override
    public PlacementResult canPlaceAt(Level level, ICoords coords) {
        String dimension = level.dimension().location().toString();
        Optional<Parcel> found = ParcelRegistry.findLeastSignificant(coords, dimension);

        if (found.isEmpty()) {
            // wilderness — Citizens require a Zone host
            return PlacementResult.INVALID_PARENT_TYPE;
        }

        Parcel parent = found.get();

        if (parent.isNation()) {
            // inside a Nation directly — must be inside a Zone instead.
            // Surface closed/blacklist as the more specific reason if applicable,
            // since those would also block any subsequent attempt via a Zone.
            NationParcel nation = (NationParcel) parent;
            if (nation.getAccessType() == NationAccessType.CLOSED) {
                return PlacementResult.NATION_CLOSED;
            }
            if (nation.getBlacklist() != null && nation.getBlacklist().contains(getOwnerId())) {
                return PlacementResult.NATION_BLACKLISTED;
            }
            return PlacementResult.OUTSIDE_VALID_PARENT;
        }

        if (parent.isZone()) {
            // inside a Zone — check the enclosing Nation's policy via the
            // NationalizedParcel contract on the Zone itself.
            NationalizedParcel nationalized = (NationalizedParcel) parent;
            if (nationalized.getAccessType() == NationAccessType.CLOSED) {
                return PlacementResult.NATION_CLOSED;
            }
            // Nation blacklist check requires walking from Zone → Nation. The
            // Zone's NationEstate carries the Nation's ID; resolve via the
            // multi-result findAllByEstateId and pick the first match. There
            // should only be one Nation per Nation estate, but the registry
            // method is multi-valued for general-purpose use.
            NationEstate ne = nationalized.getNationEstate();
            if (ne != null) {
                Optional<NationParcel> nationOpt = ParcelRegistry.findAllByEstateId(ne.getId()).stream()
                        .filter(NationParcel.class::isInstance)
                        .map(NationParcel.class::cast)
                        .findFirst();
                if (nationOpt.isPresent()) {
                    NationParcel nation = nationOpt.get();
                    if (nation.getBlacklist() != null
                            && nation.getBlacklist().contains(getOwnerId())) {
                        return PlacementResult.NATION_BLACKLISTED;
                    }
                }
            }
            // Defer to the existing handshake for any other access policy.
            if (!hasAccessTo(parent) || !parent.grantsAccess(this)) {
                return PlacementResult.ACCESS_DENIED;
            }
            return PlacementResult.SUCCESS;
        }

        // enclosing parcel is a Citizen or Player
        return PlacementResult.INVALID_PARENT_TYPE;
    }

    @Override
    protected ClaimResult claimWithinZone(Level level, Parcel parentParcel, Box parcelBox) {
        // Rule 1: parcel must be fully within the parent
        if (!ModUtil.contains(parentParcel.getBox(), parcelBox)) {
            return ClaimResult.NOT_IN_PARENT;
        }

        String dimension = level.dimension().location().toString();

        // Rule 1b: direct sibling overlap
        Optional<ClaimResult> siblingConflict = ParcelHelper.checkDirectSiblingOverlap(this, parcelBox, parentParcel, dimension);
        if (siblingConflict.isPresent()) return siblingConflict.get();

//        // Rule 1b: direct box overlap with siblings
//        List<Parcel> directOverlaps = ParcelRegistry.find(parcelBox, dimension).stream()
//                .filter(p -> !p.getId().equals(parentParcel.getId()))
//                .filter(p -> !p.isNation())
//                .filter(p -> !p.isZone())
//                .toList();
//
//        for (Parcel existing : directOverlaps) {
//            boolean hierarchical = ParcelType.isAllowedAncestor(existing.getType(), getType())
//                    || ParcelType.isAllowedDescendant(existing.getType(), getType());
//            if (hierarchical) {
//                boolean ancestorContains = ParcelType.isAllowedAncestor(existing.getType(), getType())
//                        && ModUtil.contains(existing.getBox(), parcelBox);
//                boolean placingContains = ParcelType.isAllowedDescendant(existing.getType(), getType())
//                        && ModUtil.contains(parcelBox, existing.getBox());
//                if (!ancestorContains && !placingContains) return ClaimResult.NOT_IN_PARENT;
//                continue;
//            }
//            if (ParcelConflictResolver.isConflict(getType(), existing.getType(),
//                    getOwnerId(), existing.getOwnerId())) {
//                return ClaimResult.INTERSECTS;
//            }
//            // same-owner same-type sibling: touching ok, direct overlap is a conflict
//            if (ModUtil.overlaps(parcelBox, existing.getBox())) {
//                return ClaimResult.INTERSECTS;
//            }
//        }

        // Rule 2a: existing parcels whose buffer zones reach into this parcel's box
        List<Parcel> bufferOverlaps = ParcelRegistry.findBuffer(parcelBox, dimension).stream()
                .filter(p -> !p.getId().equals(parentParcel.getId()))
                .filter(p -> !p.isNation())
                .toList();

        // Rule 2b: this parcel's own buffer zone reaches into existing parcel boxes
        int bufferSize = getBufferSize();
        List<Parcel> inflatedOverlaps = bufferSize > 0
                ? ParcelRegistry.find(ModUtil.inflate(parcelBox, bufferSize), dimension).stream()
                  .filter(p -> !p.getId().equals(parentParcel.getId()))
                  .filter(p -> !p.isNation())
                  .toList()
                : List.of();

        boolean bufferConflict =
                bufferOverlaps.stream().anyMatch(p ->
                        ParcelConflictResolver.isConflict(getType(), p.getType(),
                                getOwnerId(), p.getOwnerId()))
                        || inflatedOverlaps.stream().anyMatch(p ->
                        ParcelConflictResolver.isConflict(getType(), p.getType(),
                                getOwnerId(), p.getOwnerId()));

        if (bufferConflict) {
            return ClaimResult.INTERSECTS;
        }

        // inherit nation estate from parent
        if (parentParcel.isZone()) {
            setNationEstate(((NationalizedParcel) parentParcel).getNationEstate());
        } else {
            setNationEstate((NationEstate) parentParcel.getEstate());
        }

        return nameAndRegister(level);
    }

    @Override
    public ClaimResult handleEmbeddedClaim(Level level, Parcel parentParcel) { //}, Box parcelBox) {
        ClaimResult result = ClaimResult.FAILURE;

        // claiming an existing and relinquished citizen parcel
        if (isRelinquishedCitizenClaim(parentParcel, getBox())) {
            return claimRelinquishedCitizenParcel(level, parentParcel, getBox());
        }

        // placing a parcel within a zone
        if (isValidParentParcel(parentParcel)) {
            return claimWithinZone(level, parentParcel, getBox());
        }
        return ClaimResult.FAILURE;
    }

    protected boolean isValidParentParcel(Parcel parcel) {
        return parcel.isZone() || parcel.isNation();
    }

    // NOTE due to Java's singular inheritance, Citizen and Zone parcels have to define
    //  their own save() and load() methods even though they are duplicated code.
    @Override
    public void save(CompoundTag tag) {
//        ClaimMyLand.LOGGER.debug("saving nationalized parcel -> {}", this);

        if (nationEstate == null || nationEstate.getId() == null) {
            ClaimMyLand.LOGGER.warn("Unable to save parcel {} - missing a valid Nation Estate. This is an issue!", getId());
            return;
        }
        saveNationEstate(tag);
        super.save(tag);
    }

    @Override
    public Parcel load(CompoundTag tag) {
        if (!loadNationEstate(tag)) {
            ClaimMyLand.LOGGER.warn("skipping parcel {} - invalid nation estate.", getId());
            return this;
        }
        return super.load(tag);
    }

    @Override
    public int getBufferSize() {
        return Config.SERVER.general.parcelBufferRadius.get();
    }

    @Override
    public NationEstate getNationEstate() {
        return nationEstate;
    }

    @Override
    public void setNationEstate(NationEstate nationEstate) {
        this.nationEstate = nationEstate;
    }
}
