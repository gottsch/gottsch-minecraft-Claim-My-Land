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
package mod.gottsch.forge.claimmyland.core.parcel;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Mark Gottschling on Sep 14, 2024
 */
public class CitizenParcel extends AbstractParcel {

    public CitizenParcel() {
        setType(ParcelType.CITIZEN);
    }

    /**
     * called by the existing parcel
     * @param virtualParcel
     * @return
     */
    @Override
    public boolean grantsAccess(Parcel virtualParcel) {
        return switch (virtualParcel.getType()) {
            case PLAYER -> {
                // TODO check parent Nation is open
//                NationParcel nation = ParcelRegistry.findByNationId(getNationId());

                yield getOwnerId() == null && virtualParcel.getArea() >= getArea();
            }
            case CITIZEN -> {
                // TODO add lots of debugging logs here
                ClaimMyLand.LOGGER.debug("this.nationID ->{}, virtual -> {}", getNationId(), virtualParcel.getNationId());
                ClaimMyLand.LOGGER.debug("this.ownerID -> {}", getOwnerId());
                ClaimMyLand.LOGGER.debug("this.area ->{}, virtual -> {}", getArea(), virtualParcel.getArea());

                yield getNationId() != null && getNationId().equals(virtualParcel.getNationId())
                        && getOwnerId() == null
                        && virtualParcel.getArea() >= getArea();

                // TODO check parent nation is open then dont' need nation id matching
            }
            default -> false;
        };
    }

    @Override
    public boolean hasAccessTo(Parcel existingParcel) {
        return switch (existingParcel.getType()) {
            case CITIZEN, ZONE -> { yield true; }
            // TODO move to ZONE
            //yield getNationId() != null && getNationId().equals(otherParcel.getNationId());
            default -> false;
        };
    }

    @Override
    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return getDeedId().equals(blockEntity.getDeedId());
    }

    @Override
    public boolean canPlaceAt(Level level, ICoords coords) {
        // test if a parcel already exists for the deed id
        boolean canPlace = false;
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);

        /*
         * inside a parcel.
         */
        if (registryParcel.isPresent()) {
            if (hasAccessTo(registryParcel.get()) && registryParcel.get().grantsAccess(this)) {
                canPlace = true;
            }
        }
        return canPlace;
    }

    @Override
    public ClaimResult handleEmbeddedClaim(Level level, Parcel parentParcel, Box parcelBox) {
        ClaimResult result = ClaimResult.FAILURE;

        // an existing citizen parcel to be claimed ie not owner
        if (parentParcel.getType() == ParcelType.CITIZEN
                // if the owner id is empty, it is claimable
                && ObjectUtils.isEmpty(parentParcel.getOwnerId())) {
                // check that this parcel is bigger than the existing parcel
            if (ModUtil.getArea(parcelBox) >= parentParcel.getArea()) {
//                parentParcel.setOwnerId(getOwnerId());
                ParcelRegistry.updateOwner(parentParcel.getId(), getOwnerId());
                result = ClaimResult.SUCCESS;
            } else {
                result = ClaimResult.INSUFFICIENT_SIZE;
            }
        }
        // a zone parcel
        else if (parentParcel.getType() == ParcelType.ZONE
                || parentParcel.getType() == ParcelType.NATION) {

            // ensure the citizen parcel is completed contained within the zone
            if (!ModUtil.contains(parentParcel.getBox(), parcelBox)) {
                return result;
            }

            // find overlaps of the parcel with buffered registry parcels.
            // this ensure that the parcel boundaries are not overlapping the buffer area of another parcel
            // NOTE filter out the zone and nation parcels
            List<Parcel> overlaps = ParcelRegistry.findBuffer(parcelBox).stream()
                    .filter(p -> !p.getId().equals(parentParcel.getId()))
                    .filter(p -> parentParcel.getType() != ParcelType.ZONE && parentParcel.getType() != ParcelType.NATION)
                    .toList();

            if(Parcel.hasBoxToBufferedIntersections(parcelBox, getOwnerId(), overlaps)) {
                return result;
            }

            // add to the registry
            ParcelRegistry.add(this);

        }
        return ClaimResult.SUCCESS;
    }

    @Override
    public int getBufferSize() {
        return Config.SERVER.general.parcelBufferRadius.get();
    }
}
