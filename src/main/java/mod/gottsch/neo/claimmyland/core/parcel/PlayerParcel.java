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
import mod.gottsch.neo.claimmyland.core.estate.EstateTypeRegistry;
import mod.gottsch.neo.claimmyland.core.estate.NationEstate;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.neo.claimmyland.core.util.ModUtil;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class PlayerParcel extends AbstractClaimableParcel {

    /**
     *
     */
    private PlayerParcel() {
        super();
        setType(ParcelType.PLAYER);
        getEstate().setParcelType(getType());
    }

    /** public factory method */
    public static PlayerParcel create() {
        return new PlayerParcel();
    }

    /** copy factory method. */
    public static PlayerParcel create(Parcel source) {
        PlayerParcel playerParcel = new PlayerParcel();
        playerParcel.setId(source.getId());
        playerParcel.setName(source.getName());
        playerParcel.setCoords(source.getCoords());
        playerParcel.setSize(source.getSize());

        // TODO update estate factory to allow copy constructor
        // TODO EstateTypeRegistry should take in the getType() from the other.getEstate(). Have to ensure that estates are setting/saving/loading their types.
        Estate estate = EstateTypeRegistry.create(EstateTypeRegistry.ESTATE_TYPE);
        estate.setOwnerId(source.getEstate().getOwnerId());
        estate.setParcelType(ParcelType.PLAYER);
        estate.setPlayerWhitelist(source.getEstate().getPlayerWhitelist());
        estate.setBlockWhitelist(source.getEstate().getBlockWhitelist());
        estate.setBlockTagWhitelist(source.getEstate().getBlockTagWhitelist());
        estate.setItemWhitelist(source.getEstate().getItemWhitelist());
        estate.setItemTagWhitelist(source.getEstate().getItemTagWhitelist());
        estate.setEntitySpawnWhitelist(source.getEstate().getEntitySpawnWhitelist());
        estate.setEntitySpawnTagWhitelist(source.getEstate().getEntitySpawnTagWhitelist());
        playerParcel.setEstate(estate);
        return playerParcel;
    }

    @Override
    public boolean hasAccessTo(Parcel otherParcel) {
        return switch (otherParcel.getType()) {
            case PLAYER, CITIZEN, ZONE -> true;
            default -> false;
        };
    }

    @Override
    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return getDeedId().equals(blockEntity.getDeedId());
    }

    /**
     * version 2+: player parcels cannot be relinquished and thus cannot be claimed
     * by another deed. ie can be created by a deed or demolished/removed.
     */
    @Override
    public boolean grantsAccess(Parcel otherParcel) {
        return false;
    }

    /**
     * @author Mark Gottschling — PlacementResult refactor on Apr 12, 2026
     */
    @Override
    public PlacementResult canPlaceAt(Level level, ICoords coords) {
        String dimension = level.dimension().location().toString();
        Optional<Parcel> found = ParcelRegistry.findLeastSignificant(coords, dimension);

        if (found.isEmpty()) {
            // wilderness — fine
            return PlacementResult.SUCCESS;
        }

        Parcel parent = found.get();

        if (parent.isNation()) {
            NationParcel nation = (NationParcel) parent;
            if (nation.getAccessType() == NationAccessType.CLOSED) {
                return PlacementResult.NATION_CLOSED;
            }
            if (nation.getBlacklist() != null && nation.getBlacklist().contains(getOwnerId())) {
                return PlacementResult.NATION_BLACKLISTED;
            }
            // Player deeds cannot be placed directly in a Nation under any policy
            return PlacementResult.OUTSIDE_VALID_PARENT;
        }

        if (parent.isZone()) {
            NationalizedParcel nationalized = (NationalizedParcel) parent;
            if (nationalized.getAccessType() == NationAccessType.CLOSED) {
                return PlacementResult.NATION_CLOSED;
            }
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
            if (!hasAccessTo(parent) || !parent.grantsAccess(this)) {
                return PlacementResult.ACCESS_DENIED;
            }
            return PlacementResult.SUCCESS;
        }

        // Reclaiming a relinquished Citizen — placement is allowed at this gate.
        // The actual reclaim eligibility (geometry match, owner rules) is checked
        // downstream in claimRelinquishedCitizenParcel / transferParcelOwnership.
        if (parent.isCitizen() && parent.getEstate().isRelinquished()) {
            return PlacementResult.SUCCESS;
        }

        // enclosing parcel is a Citizen or Player — invalid host
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

        Optional<Parcel> optionalCitizenParcel = ParcelTypeRegistry.create(ParcelType.CITIZEN);
        if (optionalCitizenParcel.isEmpty()) {
            return ClaimResult.FAILURE;
        }

        CitizenParcel citizenParcel = (CitizenParcel) optionalCitizenParcel.get();
        citizenParcel.setEstate(getEstate());
        citizenParcel.getEstate().setParcelType(ParcelType.CITIZEN);
        citizenParcel.setNationEstate(((NationalizedParcel) parentParcel).getNationEstate());
        citizenParcel.setId(getId());
        citizenParcel.setSize(getSize());
        citizenParcel.setCoords(getCoords());
        citizenParcel.setOwnerId(getOwnerId());

        // register the player before the parcel to save a network call to Mojang API
        PlayerRegistry.register(level, getOwnerId());

        return citizenParcel.nameAndRegister(level);
    }

    @Override
    public void save(CompoundTag tag) {
        super.save(tag);
        tag.putString(TYPE, getType().getSerializedName());
//        ClaimMyLand.LOGGER.debug("saved parcel -> {}", this);
    }

    @Override
    public int getBufferSize() {
        return Config.SERVER.general.parcelBufferRadius.get();
    }

    @Override
    public String toString() {
        return "PlayerParcel{} " + super.toString();
    }
}
