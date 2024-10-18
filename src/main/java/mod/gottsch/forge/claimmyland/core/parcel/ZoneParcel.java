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
import mod.gottsch.forge.claimmyland.core.command.CommandHelper;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Mark Gottschling on Sep 30, 2024
 */
public class ZoneParcel extends AbstractParcel {

    public ZoneParcel() {
        setType(ParcelType.ZONE);
    }

    @Override
    public String randomName() {
        return super.randomName().replace("Parcel", "Zone");
    }

    @Override
    public boolean grantsAccess(Parcel virtualParcel) {
        // get the nation this belongs to
        List<Parcel> nations = ParcelRegistry.findByNationId(getNationId());
        if (!nations.isEmpty()) {
            NationParcel nation;
            if (nations.size() > 1) {
                Optional<Parcel> optionalNation = nations.stream().filter(n -> ModUtil.contains(n.getAbsoluteBox(), virtualParcel.getAbsoluteBox())).findFirst();
                if (optionalNation.isEmpty()) {return false;}
                else {
                    nation = (NationParcel) optionalNation.get();
                }
            } else {
                nation = (NationParcel) nations.get(0);
            }

            // a personal deed cannot be used on an existing parcel
            if (virtualParcel.getType() == ParcelType.PLAYER
                    && nation.getBorderType() == NationBorderType.OPEN) {
                return true;
            } else {
                if (virtualParcel.getType() == ParcelType.CITIZEN
                        && virtualParcel.getNationId().equals(getNationId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasAccessTo(Parcel otherParcel) {
        return otherParcel.getType() == ParcelType.NATION;
    }

    @Override
    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return blockEntity.getParcelType().equalsIgnoreCase(ParcelType.NATION.getSerializedName());
    }

    @Override
    public ClaimResult handleEmbeddedClaim(Level level, Parcel parentParcel, Box parcelBox) {
        ClaimResult result = ClaimResult.FAILURE;

        if (parentParcel.getType() == ParcelType.NATION
            && getOwnerId().equals(parentParcel.getOwnerId())) {

            // ensure zone is completely contained within the nation
            if (!ModUtil.contains(parentParcel.getBox(), parcelBox)) {
                return result;
            }

            // find overlaps of the parcel with buffered registry parcels.
            // this ensure that the parcel boundaries are not overlapping the buffer area of another parcel
            // NOTE filter out the zone and nation parcels
            List<Parcel> overlaps = ParcelRegistry.findBuffer(parcelBox).stream()
                    .filter(p -> !p.getId().equals(parentParcel.getId())
                            && p.getType() != ParcelType.NATION)
                    .toList();

            if(Parcel.hasBoxToBufferedIntersections(parcelBox, getOwnerId(), overlaps)) {
                return result;
            }

            // add to the registry
            ParcelRegistry.add(this);
            CommandHelper.save(level);
            result = ClaimResult.SUCCESS;
        }
        return result;
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public String toString() {
        return "ZoneParcel{} " + super.toString();
    }
}
