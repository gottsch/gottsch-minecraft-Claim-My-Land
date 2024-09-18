/*
 * This file is part of  Protect It.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
 *
 * All rights reserved.
 *
 * Protect It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Protect It is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Protect It.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.claimmyland.core.parcel;

import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 16, 2024
 *
 */
public interface Parcel {
    public static final String PARCEL_ID = "parcel_id";
    public static final String DEED_ID = "deed_id";
    public static final String OWNER_ID = "owner_id";
    public static final String PARCEL_TYPE = "parcel_type";
    public static final String SIZE = "size";

    void save(CompoundTag parcelTag);
    Parcel load(CompoundTag tag);

    default String randomName() {
        return StringUtils.capitalize(RandomStringUtils.random(8, true, false));
    }

    /**
     * determine if this parcel grants access to the given parcel
     * this is called when attempting to place a foundation stone,
     * ie using a deed on a non-foundation stone block.
     * @param parcel
     * @return
     */
    boolean grantsAccess(Parcel parcel);

    /**
     * determine if the this parcel has access to the given parcel.
     * this is called when attempting to place a foundation stone,
     * @param parcel
     * @return
     */
    boolean hasAccessTo(Parcel parcel);

    /**
     * this is called when attempting to claim a parcel represented by the foundation stone.
     * ie. using a deed on a foundation stone block.
     * @param blockEntity
     * @return
     */
    boolean hasAccessTo(FoundationStoneBlockEntity blockEntity);

    boolean isOwner(UUID id);

    /**
     * does the entity have access to the parcel
     * @param entityId
     * @return
     */
    boolean grantsAccess(UUID entityId);

    /**
     * does the entity and itemStack have access to the parcel
     * @param entityId
     * @param stack
     * @return
     */
    boolean grantsAccess(UUID entityId, ItemStack stack);

    void populateBlockEntity(FoundationStoneBlockEntity entity);

    Box getBox();

    ICoords getMinCoords();
    ICoords getMaxCoords();

    UUID getId();

    void setId(UUID id);

    UUID getOwnerId();

    void setOwnerId(UUID ownerId);

    UUID getDeedId();

    void setDeedId(UUID deedId);

    String getName();

    void setName(String name);

    ICoords getCoords();

    void setCoords(ICoords coords);

    Box getSize();

    void setSize(Box size);

    int getArea();

    List<UUID> getWhitelist();

    void setWhitelist(List<UUID> whitelist);

    int getBufferSize();

    ParcelType getType();

    void setType(ParcelType type);

}
