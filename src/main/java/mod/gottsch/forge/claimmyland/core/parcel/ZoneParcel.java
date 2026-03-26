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
import mod.gottsch.forge.claimmyland.core.estate.NationEstateContext;
import mod.gottsch.forge.claimmyland.core.item.CitizenDeed;
import mod.gottsch.forge.claimmyland.core.item.PlayerDeed;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * @author Mark Gottschling on Sep 30, 2024
 */
public class ZoneParcel extends AbstractParcel implements NationalizedParcel {
    // nation-ownership token
    private NationEstate nationEstate;

    private ZoneParcel() {
        super();
        setType(ParcelType.ZONE);
        getEstate().setParcelType(getType());
        this.nationEstate = new NationEstateContext();
    }

    private ZoneParcel(NationEstate nationEstate) {
//        super(nationEstate);
        setType(ParcelType.ZONE);
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

    public static ZoneParcel create() {
        return new ZoneParcel();
    }


    public static ZoneParcel create(NationParcel nation) {
        return create((NationEstate) nation.getEstate());
    }

    public static ZoneParcel create(NationEstate estate) {
        return new ZoneParcel(estate);
    }

    @Override
    public String randomName() {
        return super.randomName().replace("Parcel", "Zone");
    }

    @Override
    public boolean grantsAccess(Parcel virtualParcel) {
        if (virtualParcel.isPlayer()) {
            return getAccessType() == NationAccessType.OPEN;
        }
        if (virtualParcel.isCitizen()) {
            return getAccessType() == NationAccessType.OPEN || isSameNation((CitizenParcel) virtualParcel);
        }
        return false;
    }

    @Override
    public boolean grantsAccess(UUID entityId, ItemStack stack) {
        ClaimMyLand.LOGGER.debug("checking Zone parcel grantsAccess for player -> 0{} with item -> {}", entityId.toString(), stack.getDisplayName().getString());
        if (grantsAccess(entityId)) {
            return true;
        }
        ClaimMyLand.LOGGER.debug("player does not have uuid access, check item...");

        // check what stack the player is holding
        boolean hasItemAccess = stack.getItem() instanceof PlayerDeed || stack.getItem() instanceof CitizenDeed;
        ClaimMyLand.LOGGER.debug("player {} item access", hasItemAccess ? "has" : "does NOT have");
        return hasItemAccess;
    }

    @Override
    public boolean hasAccessTo(Parcel otherParcel) {
        return otherParcel.isNation();
    }

    @Override
    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return blockEntity.getParcelType().equalsIgnoreCase(ParcelType.NATION.getSerializedName());
    }

    @Override
    public ClaimResult handleEmbeddedClaim(Level level, Parcel parentParcel, Box parcelBox) {
        if (!parentParcel.isNation() || !getOwnerId().equals(parentParcel.getOwnerId())) {
            return ClaimResult.FAILURE;
        }

        // TODO parcelBox appears 1 block larger on x-axis (and possibly y-axis) — investigate
        if (!ModUtil.contains(parentParcel.getBox(), parcelBox)) {
            return ClaimResult.FAILURE;
        }

        List<Parcel> overlaps = ParcelRegistry.findBuffer(parcelBox, level.dimension().location().toString()).stream()
                .filter(p -> !p.getId().equals(parentParcel.getId()) && !p.isNation())
                .filter(p -> !p.getOwnerId().equals(getOwnerId()))
                .toList();

        if (Parcel.hasBoxToBufferedIntersections(parcelBox, getOwnerId(), overlaps)) {
            return ClaimResult.INTERSECTS;
        }

        // TODO isValidParcel()
        // update nation estate - inherit from parent parcel
        setNationEstate((NationEstate) parentParcel.getEstate());

        // register parcel
//        ParcelRegistry.register((ServerLevel)level, this);
//        CommandHelper.save(level);
//        return ClaimResult.SUCCESS;
        return nameAndRegister(level);
    }

    // NOTE due to Java's singular inheritance, Citizen and Zone parcels have to define
    //  their own save() and load() methods even though they are duplicated code.
    @Override
    public void save(CompoundTag tag) {
        ClaimMyLand.LOGGER.debug("saving nationalized parcel -> {}", this);

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
        return 0;
    }

    @Override
    public NationEstate getNationEstate() {
        return nationEstate;
    }

    @Override
    public void setNationEstate(NationEstate nationEstate) {
        this.nationEstate = nationEstate;
    }

    @Override
    public String toString() {
        return "ZoneParcel{} " + super.toString();
    }
}
