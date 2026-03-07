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
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.EstateTypeRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    protected ClaimResult claimWithinZone(Level level, Parcel parentParcel, Box parcelBox) {
        if (!ModUtil.contains(parentParcel.getBox(), parcelBox)) {
            return ClaimResult.FAILURE;
        }

        List<Parcel> overlaps = ParcelRegistry.findBuffer(parcelBox).stream()
                .filter(p -> !p.getId().equals(parentParcel.getId()))
                .filter(p -> !p.isNation())
                .toList();

        if (Parcel.hasBoxToBufferedIntersections(parcelBox, getOwnerId(), overlaps)) {
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

        // register the player before the parcel to save a network call to Mojang API.
        PlayerRegistry.register(level, getOwnerId());

        return citizenParcel.nameAndRegister(level);
//        ParcelRegistry.register((ServerLevel)level, citizenParcel);
//        CommandHelper.save(level);

//        return ClaimResult.SUCCESS;
    }

    @Override
    public void save(CompoundTag tag) {
        super.save(tag);
        tag.putString(TYPE, getType().getSerializedName());
        ClaimMyLand.LOGGER.debug("saved parcel -> {}", this);
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
