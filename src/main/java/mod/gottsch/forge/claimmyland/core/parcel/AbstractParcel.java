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
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public abstract class AbstractParcel implements Parcel {
    public static final String NAME_KEY = "name";
    public static final String ID_KEY = "id";
    public static final String NATION_ID_KEY = "nation_id";
    public static final String OWNER_KEY = "owner";
    public static final String DEED_KEY = "deed";

    public static final String COORDS_KEY = "coords";
    public static final String SIZE_KEY = "size";
    public static final String WHITELIST_KEY = "whitelist";

    // TODO this probably can be moved into Parcel (replace PARCEL_TYPE)
    public static final String TYPE = "type";

    // the unique id of the parcel
    private UUID id;

    // TODO will need to create a NationRegistry that associates a nation id
    // to a name. that way instead of one parcel being a nation you can have
    // muiltiple sharing the same name and id.
    // the unique id of a nation
    private UUID nationId;

    private UUID ownerId;
    private UUID deedId;
    private String name;
    private ICoords coords;
    // TODO rename to getPlacement or getBox()
    private Box size;
    private List<UUID> whitelist;
    private ParcelType type;

    private long foundedTime;
    private long ownerTime;
    private long abandonedTime;

    public AbstractParcel() {
        setId(UUID.randomUUID());
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
        // if a parcel has no owner, anyone has access to modify
        if (getOwnerId() == null) {
            return true;
        }
        // if a parcel has a owner, only the owner has access
        else if (getOwnerId().equals(entityId)) {
            return true;
        } else {
            // or the owener's whitelist has access
            return getWhitelist().stream().anyMatch(uuid -> uuid.equals(entityId));
        }
    }

    /**
     * variation of hasAccess(entityId) that takes into account the item the player is holding.
     * ie certain items grant the player access, like a deed, which under normal circumstances they
     * would not have access.
     * @param entityId
     * @param stack
     * @return
     */
    @Override
    public boolean grantsAccess(UUID entityId, ItemStack stack) {
        return grantsAccess(entityId);
    }

    // TODO will have to be overridden by concrete parcel
//    @Override
//    public void populateBlockEntity(FoundationStoneBlockEntity entity) {
//        entity.setParcelId(getId());
//        entity.setDeedId(getDeedId());
//        entity.setOwnerId(getOwnerId());
//        entity.setCoords(getCoords());
//        entity.setRelativeBox(getSize());
//    }

    @Override
    public ClaimResult handleEmbeddedClaim(Level level, Parcel parentParcel, Box parcelBox) {
        return ClaimResult.FAILURE;
    }

    @Override
    public void save(CompoundTag tag) {
        ClaimMyLand.LOGGER.debug("saving parcel -> {}", this);

        if (ObjectUtils.isNotEmpty(getId())) {
            tag.putUUID(ID_KEY, getId());
        } else {
            // TODO warn and skip save
        }
        if (ObjectUtils.isNotEmpty(getNationId())) {
            tag.putUUID(NATION_ID_KEY, getNationId());
        }

        if (StringUtils.isNotBlank(getName())) {
            tag.putString(NAME_KEY, getName());
        }
        if (ObjectUtils.isNotEmpty(getOwnerId())) {
            tag.putUUID(OWNER_KEY, getOwnerId());
        }
        if (ObjectUtils.isNotEmpty(getDeedId())) {
            tag.putUUID(DEED_KEY, getDeedId());
        }

        tag.putString(TYPE, getType().getSerializedName());

        CompoundTag coordsTag = new CompoundTag();
        getCoords().save(coordsTag);
        tag.put(COORDS_KEY, coordsTag);

        CompoundTag sizeTag = new CompoundTag();
        getSize().save(sizeTag);
        tag.put(SIZE_KEY, sizeTag);

        if (getWhitelist() != null) {
            ListTag list = new ListTag();
            getWhitelist().forEach(data -> {
                CompoundTag uuidTag = new CompoundTag();
                uuidTag.putUUID(ID_KEY, data);
                list.add(uuidTag);
            });
            tag.put(WHITELIST_KEY, list);
        }

        tag.putLong("foundedTime", getFoundedTime());
        tag.putLong("ownerTime", getOnwerTime());
        tag.putLong("abandonedTime", getAbandonedTime());
    }

    @Override
    public Parcel load(CompoundTag tag) {
        if (tag.contains(ID_KEY)) {
            setId(tag.getUUID(ID_KEY));
        } else if (this.getId() == null) {
            setId(UUID.randomUUID());
        }
        if (tag.contains(NATION_ID_KEY)) {
            setNationId(tag.getUUID(NATION_ID_KEY));
        }
        if (tag.contains(NAME_KEY)) {
            setName(tag.getString(NAME_KEY));
        }
        if (tag.contains(OWNER_KEY)) {
            setOwnerId(tag.getUUID(OWNER_KEY));
        }
        if (tag.contains(DEED_KEY)) {
            setDeedId(tag.getUUID(DEED_KEY));
        }
        if (tag.contains(TYPE)) {
            setType(ParcelType.valueOf(tag.getString(TYPE)));
        }
        if (tag.contains(COORDS_KEY)) {
            setCoords(Coords.EMPTY.load(tag.getCompound(COORDS_KEY)));
        }
        if (tag.contains(SIZE_KEY)) {
            setSize(Box.load(tag.getCompound(SIZE_KEY)));
        }
        if (tag.contains(WHITELIST_KEY)) {
            ListTag list = tag.getList(WHITELIST_KEY, Tag.TAG_COMPOUND);
            list.forEach(element -> {
                CompoundTag uuidTag = ((CompoundTag)element);
                if (uuidTag.contains(ID_KEY)) {
                    getWhitelist().add(uuidTag.getUUID(ID_KEY));
                }
            });
        }

        if (tag.contains("foundedTime")) {
            setFoundedTime(tag.getLong("foundedTime"));
        }
        if (tag.contains("ownerTime")) {
            setOwnerTime(tag.getLong("ownerTime"));
        }
        if (tag.contains("abandonedTime")) {
            setAbandonedTime(tag.getLong("abandonedTime"));
        }

        return this;
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
        return ModUtil.getArea(getSize());
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
    public UUID getNationId() {
        return nationId;
    }
    @Override
    public void setNationId(UUID nationId) {
        this.nationId = nationId;
    }

    @Override
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
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
    public List<UUID> getWhitelist() {
        if (whitelist == null) {
            whitelist = new ArrayList<>();
        }
        return whitelist;
    }

    @Override
    public void setWhitelist(List<UUID> whitelist) {
        this.whitelist = whitelist;
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
    public Long getFoundedTime() {
        return foundedTime;
    }

    @Override
    public void setFoundedTime(Long foundedTime) {
        this.foundedTime = foundedTime;
    }

    @Override
    public Long getOnwerTime() {
        return ownerTime;
    }

    @Override
    public void setOwnerTime(Long ownerTime) {
        this.ownerTime = ownerTime;
    }

    @Override
    public Long getAbandonedTime() {
        return abandonedTime;
    }

    @Override
    public void setAbandonedTime(Long abandonedTime) {
        this.abandonedTime = abandonedTime;
    }

    @Override
    public String toString() {
        return "AbstractParcel{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", deedId=" + deedId +
                ", name='" + name + '\'' +
                ", coords=" + coords +
                ", size=" + size +
                ", whitelist=" + whitelist +
                ", type=" + type +
                '}';
    }
}
