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
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.NationEstate;
import mod.gottsch.forge.claimmyland.core.item.CitizenDeed;
import mod.gottsch.forge.claimmyland.core.item.PlayerDeed;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Mark Gottschling on Sep 30, 2024
 */
public class ZoneParcel extends AbstractNationalizedParcel {

    public ZoneParcel() {
        super();
        setType(ParcelType.ZONE);
        getEstate().setParcelType(getType());
    }

    public ZoneParcel(NationEstate nationEstate) {
        super(nationEstate);
        setType(ParcelType.ZONE);

        Estate estate = getEstate();
        estate.setParcelType(getType());
        estate.setName(estate.defaultName(getOwnerId()));
        estate.setBlockWhitelist(nationEstate.getBlockWhitelist());
        estate.setBlockTagWhitelist(nationEstate.getBlockTagWhitelist());
        estate.setItemWhitelist(nationEstate.getItemWhitelist());
        estate.setItemTagWhitelist(nationEstate.getItemTagWhitelist());
        estate.setPlayerWhitelist(nationEstate.getPlayerWhitelist());
        estate.setEntitySpawnTagWhitelist(nationEstate.getEntitySpawnTagWhitelist());
        estate.setEntitySpawnWhitelist(nationEstate.getEntitySpawnWhitelist());
    }

    @Deprecated
    public ZoneParcel(UUID nationId) {
        this();
        setNationId(nationId);
    }

    @Deprecated
    public ZoneParcel(NationParcel nation) {
        setNationEstate((NationEstate)nation.getEstate());
        setBlockTagWhitelist(nation.getBlockTagWhitelist());
        setBlockWhitelist(nation.getBlockWhitelist());
        setItemTagWhitelist(nation.getItemTagWhitelist());
        setItemWhitelist(nation.getItemWhitelist());
        setPlayerWhitelist(nation.getPlayerWhitelist());
        getEstate().setEntitySpawnTagWhitelist(nation.getEstate().getEntitySpawnTagWhitelist());
        getEstate().setEntitySpawnWhitelist(nation.getEstate().getEntitySpawnWhitelist());
    }

    public static ZoneParcel create() {
        return new ZoneParcel();
    }

    @Deprecated
    public static ZoneParcel create(UUID nationId) {
        return new ZoneParcel(nationId);
    }

    public static ZoneParcel create(NationParcel nation) {
        return new ZoneParcel((NationEstate) nation.getEstate());
    }

    @Override
    public String randomName() {
        return super.randomName().replace("Parcel", "Zone");
    }

    @Override
    public boolean grantsAccess(Parcel virtualParcel) {
        NationAccessType accessType = Optional.ofNullable(getNationEstate())
                .map(NationEstate::getAccessType)
                .orElse(NationAccessType.CLOSED);

        if (virtualParcel instanceof PlayerParcel) {
            return accessType == NationAccessType.OPEN;
        }

        if (virtualParcel instanceof CitizenParcel citizenParcel) {
            return accessType == NationAccessType.OPEN || isSameNation(citizenParcel);
        }

        return false;
    }
//    public boolean grantsAccess(Parcel virtualParcel) {
//        // get the nation this belongs to
//        List<Parcel> nations = ParcelRegistry.findByNationId(getNationId());
//        if (!nations.isEmpty()) {
//            NationParcel nation;
//            if (nations.size() > 1) {
//                Optional<Parcel> optionalNation = nations.stream().filter(n -> ModUtil.contains(n.getAbsoluteBox(), virtualParcel.getAbsoluteBox())).findFirst();
//                if (optionalNation.isEmpty()) {return false;}
//                else {
//                    nation = (NationParcel) optionalNation.get();
//                }
//            } else {
//                nation = (NationParcel) nations.get(0);
//            }
//
//            // a personal deed cannot be used in a closed-border nation
//            if (virtualParcel.getType() == ParcelType.PLAYER
//                    && nation.getBorderType() == NationBorderType.OPEN) {
//                return true;
//            } else {
//                if (virtualParcel.getType() == ParcelType.CITIZEN
//                        && virtualParcel.getNationId().equals(getNationId())) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    @Override
    public boolean grantsAccess(UUID entityId, ItemStack stack) {
        ClaimMyLand.LOGGER.debug("checking Zone parcel grantsAccess for player -> 0{} with item -> {}", entityId.toString(), stack.getDisplayName().getString());
        if (grantsAccess(entityId)) {
            return true;
        }
        ClaimMyLand.LOGGER.debug("player does not have uuid access, check item...");

        // check what stack the player is holding
        if (stack.getItem() instanceof PlayerDeed || stack.getItem() instanceof CitizenDeed) {
            ClaimMyLand.LOGGER.debug("player DOES have item access...");
            return true;
        }
        ClaimMyLand.LOGGER.debug("player does not have item access...");
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

            // TODO somehow parcelBox is being 1 bigger on the x-axis and probably on the y-axis as well.
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
            ParcelRegistry.register(this);
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
