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
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.EstateContext;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.Callable;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public abstract class AbstractParcel implements Parcel {
    public static final String ESTATE_KEY = "estate";
    public static final String NAME_KEY = "name";
    public static final String ID_KEY = "id";
    @Deprecated
    public static final String NATION_ID_KEY = "nation_id";
    public static final String OWNER_KEY = "owner";
    public static final String DEED_KEY = "deed";

    public static final String COORDS_KEY = "coords";
    public static final String SIZE_KEY = "size";
    public static final String WHITELIST_KEY = "whitelist";
    public static final String BLOCK_TAG_WHITELIST_KEY = "blockTagWhitelist";
    public static final String BLOCK_WHITELIST_KEY = "blockWhitelist";
    public static final String ITEM_TAG_WHITELIST_KEY = "itemTagWhitelist";
    public static final String ITEM_WHITELIST_KEY = "itemWhitelist";

    // TODO this probably can be moved into Parcel (replace PARCEL_TYPE)
    public static final String TYPE = "type";

    //
    public static final String DIMENSION_KEY = "dimension";

    // default value — matches current hardcoded Overworld-only behaviour.
    // TODO update this per-parcel when multi-dimension support is added in a future version.
    public static final String DEFAULT_DIMENSION = "minecraft:overworld";

    // the unique id of the parcel
    private UUID id;
    private String name;

    // ownership token
    private Estate estate;

    @Deprecated
    private UUID nationId;

    private UUID deedId;

    private ICoords coords;
    // TODO rename to getPlacement or getBox()
    private Box size;
    private ParcelType type;

    private String dimension = DEFAULT_DIMENSION;

    /*
     * no-arg constructor
     */
    public AbstractParcel() {
        setId(UUID.randomUUID());
        this.estate = new EstateContext();
        setName(randomName());
    }

    @Override
    public String defaultName(Player player) {
        Set<Parcel> parcels = getEstate().findParcels();
        return player.getScoreboardName() + "-parcel-" + (parcels.size() + 1);
    }

    @Override
    public String defaultName(UUID ownerId) {
        Optional<String> name = PlayerRegistry.getNameFromUUIDSynchronized(ownerId);
        Set<Parcel> parcels = getEstate().findParcels();
        return name.orElseGet(this::randomName) + "-parcel-" + (parcels.size() + 1);
    }

    @Override
    public String defaultName(ServerLevel level, UUID ownerId) {
        Optional<String> name = PlayerRegistry.getPlayerName(level, ownerId);
        Set<Parcel> parcels = getEstate().findParcels();
        return name.orElseGet(this::randomName) + "-parcel-" + (parcels.size() + 1);
    }

    @Override
    public abstract boolean grantsAccess(Parcel parcel);

    @Override
    public abstract boolean hasAccessTo(Parcel parcel);

    @Override
    public abstract boolean hasAccessTo(FoundationStoneBlockEntity blockEntity);

    @Override
    public boolean isOwner(UUID entityId) {
        return getOwnerId() == null || getOwnerId().equals(entityId);
    }

    /**
     * default behaviour is to check if the owner id matches, or any of the whitelisted ids.
     * @param entityId
     * @return
     */
    @Override
    public boolean grantsAccess(UUID entityId) {
//        ClaimMyLand.LOGGER.info("this parcel -> {}", this);
//        ClaimMyLand.LOGGER.info("entityId -> {}, parcel.owner -> {}", entityId, this.getOwnerId());

        // if a parcel has no owner, anyone has access to modify
        if (getOwnerId() == null) {
            ClaimMyLand.LOGGER.debug("parcel has no owner");
            return true;
        }
        // if a parcel has a owner, only the owner has access
        else if (getOwnerId().equals(entityId)) {
            ClaimMyLand.LOGGER.debug("ids match -> {} - {}", getOwnerId(), entityId);
            return true;
        } else {
            // or the owner's whitelist has access
            return getEstate().getPlayerWhitelist().stream().anyMatch(uuid -> uuid.equals(entityId));
        }
    }

    /**
     * variation of hasAccess(entityId) that takes into account the item the player is holding.
     * ie certain items grant the player access, like a deed, which under normal circumstances they
     * would not have access.
     * @param entityId
     * @param itemStack
     * @return
     */
    @Override
    public boolean grantsAccess(UUID entityId, ItemStack itemStack) {

        /*
         * NOTE item right-click of a deed is permitted in any parcel,
         * but the actual usage/execution is checked during use().
         */
        if (itemStack.getItem() instanceof Deed) {
            return true;
        }

        // ClaimMyLand.LOGGER.debug("in grantsAccess() for entityId -> {} and item -> {}", entityId, stack.getDisplayName().getString());
        return grantsAccess(entityId);
    }

    @Override
    public ClaimResult handleEmbeddedClaim(Level level, Parcel parentParcel) { //}, Box parcelBox) {
        return ClaimResult.FAILURE;
    }

    @Override
    public void save(CompoundTag tag) {
        ClaimMyLand.LOGGER.debug("saving parcel -> {}", this);

        Estate estate = getEstate();
        // TODO add isValidClaim() method
        if (estate == null || estate.getId() == null || estate.getOwnerId() == null) { // TODO <-- if abandoned is removed, enforce this
            ClaimMyLand.LOGGER.warn("parcel {} is missing a valid Estate.", getName());
            return;
        }
        tag.put(ESTATE_KEY, estate.save(new CompoundTag()));

        if (ObjectUtils.isNotEmpty(getId())) {
            tag.putUUID(ID_KEY, getId());
        } else {
            // TODO warn and skip save
        }
        if (StringUtils.isNotBlank(getName())) {
            tag.putString(NAME_KEY, getName());
        }

        if (ObjectUtils.isNotEmpty(getDeedId())) {
            tag.putUUID(DEED_KEY, getDeedId());
        }

        tag.putString(TYPE, getType().getSerializedName());

        tag.putString(DIMENSION_KEY, getDimension());

        CompoundTag coordsTag = new CompoundTag();
        getCoords().save(coordsTag);
        tag.put(COORDS_KEY, coordsTag);

        CompoundTag sizeTag = new CompoundTag();
        getSize().save(sizeTag);
        tag.put(SIZE_KEY, sizeTag);

    }

    @Override
    public Parcel load(CompoundTag tag) {
        if (tag.contains(ID_KEY)) {
            setId(tag.getUUID(ID_KEY));
        } else if (this.getId() == null) {
            setId(UUID.randomUUID());
        }

        // load estate data
        if (tag.contains(ESTATE_KEY)) {
            CompoundTag estateTag = tag.getCompound(ESTATE_KEY);
            EstateRegistry.get(estateTag.getUUID(Estate.ID_KEY))
                    .ifPresentOrElse(this::setEstate,
                            () -> {
                                getEstate().load(estateTag);
                                EstateRegistry.register(getEstate());
                            });

        }

        if (!isValidClaim(estate)) {
            ClaimMyLand.LOGGER.warn("unable to load parcel {} - invalid claim data.", getId());
            return this;
        }
        if (tag.contains(NAME_KEY)) {
            setName(tag.getString(NAME_KEY));
        }

        if (tag.contains(DEED_KEY)) {
            setDeedId(tag.getUUID(DEED_KEY));
        }
        if (tag.contains(TYPE)) {
            setType(ParcelType.valueOf(tag.getString(TYPE)));
        }
        if (tag.contains(DIMENSION_KEY)) {
            setDimension(tag.getString(DIMENSION_KEY));
        }
        if (tag.contains(COORDS_KEY)) {
            setCoords(Coords.EMPTY.load(tag.getCompound(COORDS_KEY)));
        }
        if (tag.contains(SIZE_KEY)) {
            setSize(Box.load(tag.getCompound(SIZE_KEY)));
        }

        return this;
    }

    @Override
    public boolean isValidClaim(Estate estate) {
        return estate != null && estate.getId() != null
                && StringUtils.isNotBlank(estate.getName());
    }

    @Override
    public boolean isValid() {
        return isValidClaim(getEstate()) && getId() != null;
    }

    /**
     * gets an absolute box at a coords using the block entity
     * @return
     */
    @Deprecated
    // getBox() already does this
    @Override
    public Box getAbsoluteBox() {
        ICoords myCoords = getCoords();
        return new Box(myCoords.add(getBox().getMinCoords()),
                myCoords.add(getBox().getMaxCoords()));
    }

    @Override
    public Box getBox() {
        return new Box(getMinCoords(), getMaxCoords());
    }

    @Override
    public int getArea() {
//        ICoords absoluteSize = ModUtil.getSize(getSize());
//        return absoluteSize.getX() * absoluteSize.getZ() * absoluteSize.getY();
        return ModUtil.getVolume(getSize());
    }

    @Override
    public ICoords getMinCoords() {

        return getCoords().add(getSize().getMinCoords());
    }

    @Override
    public ICoords getMaxCoords() {

        return getCoords().add(getSize().getMaxCoords());
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public Estate getEstate() {
        return estate;
    }

    public void setEstate(Estate estate) {
        this.estate = estate;
    }

    @Override
    public UUID getOwnerId() {
        return getEstate().getOwnerId();
    }

    @Override
    public void setOwnerId(UUID ownerId) {
        getEstate().setOwnerId(ownerId);
    }

    @Override
    public UUID getDeedId() {
        return deedId;
    }

    @Override
    public void setDeedId(UUID deedId) {
        this.deedId = deedId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ICoords getCoords() {
        return coords;
    }

    @Override
    public void setCoords(ICoords coords) {
        this.coords = coords;
    }

    // TODO rename to getPlacement ie it is not getting the size by the relative pos
    @Override
    public Box getSize() {
        return size;
    }

    @Override
    public void setSize(Box size) {
        this.size = size;
    }

    @Override
    public String getDimension() {
        return dimension;
    }

    @Override
    public void setDimension(String dimension) {
        this.dimension = dimension;
    }



    /*
     * convenience method
     */
    @Override
    public Set<UUID> getPlayerWhitelist() {
        return getEstate().getPlayerWhitelist();
    }

    @Override
    public void setPlayerWhitelist(Set<UUID> whitelist) {
        getEstate().setPlayerWhitelist(whitelist);
    }



    @Override
    public ParcelType getType() {
        return type;
    }

    @Override
    public void setType(ParcelType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "AbstractParcel{" +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", estate=" + estate +
                ", nationId=" + nationId +
                ", deedId=" + deedId +
                ", coords=" + coords +
                ", size=" + size +
                ", type=" + type +
                '}';
    }
}
