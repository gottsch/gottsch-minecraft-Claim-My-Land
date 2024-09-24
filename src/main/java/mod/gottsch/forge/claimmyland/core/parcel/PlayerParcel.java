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
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class PlayerParcel extends AbstractParcel {

    /**
     *
     */
    public PlayerParcel() {
        setType(ParcelType.PLAYER);
    }

    @Override
    public boolean hasAccessTo(Parcel otherParcel) {
        return switch (otherParcel.getType()) {
            case PLAYER -> { yield true;}
            case CITIZEN -> {yield true;}
            case ZONE -> {yield true;}
            default -> false;
        };

        // check against another personal parcel
//        if (parcel.getId().equals(getId())) {
//            return parcel.getOwnerId() == null || parcel.getOwnerId().equals(getOwnerId());
//        }
    }

    @Override
    public boolean grantsAccess(Parcel otherParcel) {
        // TODO only a transfer deed/parcel has access to me
        return false;
    }

    @Override

    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return getDeedId().equals(blockEntity.getDeedId());
    }

    @Override
    public boolean handleEmbeddedClaim(Level level, Parcel parentParcel, Box parcelBox) {
        boolean result = false; //ClaimResult.FAILURE;

        if (parentParcel.getType() == ParcelType.CITIZEN
                && ObjectUtils.isEmpty(parentParcel.getOwnerId())) {

            if (ModUtil.getArea(parcelBox) >= parentParcel.getArea()) {
                ParcelRegistry.updateOwner(parentParcel.getId(), getOwnerId());
//                parentParcel.setOwnerId(getOwnerId());
                result = true; // ClaimResult.SUCCESS;
            } else {
                result = false; // ClaimResult.INSUFFICIENT_SIZE;
            }
        }
        else if (parentParcel.getType() == ParcelType.ZONE) {

            // find overlaps of the parcel with buffered registry parcels.
            // this ensure that the parcel boundaries are not overlapping the buffer area of another parcel
            // NOTE filter out the multicitizen and nation parcels
            List<Parcel> overlaps = ParcelRegistry.findBuffer(parcelBox).stream()
                    .filter(p -> !p.getId().equals(parentParcel.getId()))
                    .filter(p -> !p.getId().equals(((ZoneParcel)parentParcel).getNationId()))
                    .toList();

            // short-circuit if the parcel overlaps/intersects any parcels
            if (!overlaps.isEmpty()) {
                for (Parcel overlapParcel : overlaps) {
                    // if parcel in hand equals parcel in world then fail
                    /*
                     * NOTE this should be moot as the deed shouldn't exist at this point anymore (survival)
                     * as this can potentially only happen in creative.
                     */
                    if (getId().equals(overlapParcel.getId())) {
                        return false;
                    }

                    /*
                     * if parcel in hand has same owner as parcel in world, ignore buffers,
                     * but check border overlaps. parcels owned by the same player can be touching.
                     */
                    if (getOwnerId().equals(overlapParcel.getOwnerId())) {
                        // get the existing owned parcel
                        Optional<Parcel> optionalOwnedParcel = ParcelRegistry.findByParcelId(overlapParcel.getId());

                        // test if the non-buffered parcels intersect
                        if (optionalOwnedParcel.isPresent() && ModUtil.touching(getBox(), optionalOwnedParcel.get().getBox())) {
                            return false; // ClaimResult.INTERSECTS
                        }
                    } else {
                        return false; // ClaimResult.INTERSECTS
                    }
                }
            }

            // validate placement. transform personal into citizen parcel
            Optional<Parcel> optionalCitizenParcel = ParcelFactory.create(ParcelType.CITIZEN);
            if (optionalCitizenParcel.isPresent()) {
                CitizenParcel citizenParcel = (CitizenParcel) optionalCitizenParcel.get();
                citizenParcel.setNationId(((ZoneParcel) parentParcel).getNationId());
                citizenParcel.setId(getId());
                citizenParcel.setSize(getSize());
                citizenParcel.setCoords(parcelBox.getMinCoords());
                citizenParcel.setOwnerId(getOwnerId());

                // add to the registry
                ParcelRegistry.add(citizenParcel);
                // register the player
                PlayerRegistry.register(level, getOwnerId());
                result = true; // ClaimResult.SUCCESS;
            }
        }
        return result;
    }

    @Override
    public void save(CompoundTag tag) {
        super.save(tag);
        tag.putString(TYPE, getType().getSerializedName());
        ClaimMyLand.LOGGER.debug("saved parcel -> {}", this);
    }

    @Override
    public Parcel load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TYPE)) {
            setType(ParcelType.valueOf(tag.getString(TYPE)));
        }
        return this;
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
