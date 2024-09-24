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

import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
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

    /**
     *
     * @return
     */
    default public String randomName() {
        String name;
        int size = ParcelRegistry.size() + 1;

        int iterations = 0;
        boolean nameNotFound = false;
        do {
            name = "Parcel" + String.valueOf(size);
            // check the registry
            Optional<Parcel> namedParcel = ParcelRegistry.findByName(name);
            if (namedParcel.isEmpty()) nameNotFound = true;
        } while (iterations++ < 3 && !nameNotFound);

        if (!nameNotFound) {
            name = StringUtils.capitalize(RandomStringUtils.random(8, true, false));
        }
        return name;
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

     /**
     * determines whether this deed can place a Foundation stone in the world.
     * @param level
     * @param coords
     * @return
     */
    default boolean canPlaceAt(Level level, ICoords coords) {
        /*
         * check if parcel is within another existing parcel
         */
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);
        return registryParcel.map(this::handleEmbeddedPlacementRules).orElseGet(this::handlePlacementRules);
    }

    /**
     * handles situations where this deed DOES NOT overlap with any other parcels
     * @return
     */
    default public boolean handlePlacementRules() {
        return true;
    }

    /**
     * handles situations where this does DOES overlap with other parcels.
     * ie check whitelist, nation permissions etc
     * @return
     */
    default public boolean handleEmbeddedPlacementRules(Parcel registryParcel) {
        return hasAccessTo(registryParcel) && registryParcel.grantsAccess(this);
    }

    // TODO have 2 variants of this, one that takes the parcelBox (static) and one that doesn't
    public boolean handleEmbeddedClaim(Level level, Parcel parentParcel, Box parcelBox);

    /**
     * default behaviour to claim parcel
     * @param level
     * @return
     */
    default public boolean handleClaim(Level level, Box parcelBox) {

//        Box parcelBox = blockEntity.getAbsoluteBox();

        // TODO all this can be replace with hasBoxToBufferedBoxIntersections
        // find overlaps of the parcel with buffered registry parcels.
        // this ensure that the parcel boundaries are not overlapping the buffer area of another parcel
        List<Parcel> overlaps = ParcelRegistry.findBuffer(parcelBox);
        if (!overlaps.isEmpty()) {
            for (Parcel overlapParcel : overlaps) {
                // if parcel in hand equals parcel in world then fail
                /*
                 * NOTE this should be moot as the deed shouldn't exist at this point anymore (survival)
                 * as this can potentially only happen in creative.
                 */
                if (getId().equals(overlapParcel.getId())) {
                    return false; // ClaimResult.FAILURE
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
                    return false; // ClaimResult.INTERSECTS;
                }
            }
        }

        // add to the registry
        ParcelRegistry.add(this);

        return true; // ClaimResult.SUCCESS;
    }

    default public boolean hasBufferedIntersections(Parcel parcel, List<Parcel> bufferedParcels) {
        return hasBoxToBufferedIntersections(parcel.getBox(), parcel.getOwnerId(), bufferedParcels);
    }

    /**
     * the intent to to take a non-buffered parcel box and test against the buffered list
     * if overlaps with a buffered parcel and not owner by the same owner, then fail
     *
     * @param bufferedParcels a list of buffered parcels to test against
     */
    public static boolean hasBoxToBufferedIntersections(Box box, UUID ownerId, List<Parcel> bufferedParcels) {
        for (Parcel overlapParcel : bufferedParcels) {
            /*
             * if parcel of foundation stone has same owner as parcel in world, ignore buffers,
             * but check border overlaps. parcels owned by the same player can be touching.
             */
            if (ownerId.equals(overlapParcel.getOwnerId())) {
                // get the existing owned parcel
                Optional<Parcel> optionalOwnedParcel = ParcelRegistry.findByParcelId(overlapParcel.getId());

                // test if the parcels intersect
                if (optionalOwnedParcel.isPresent() && ModUtil.touching(box, optionalOwnedParcel.get().getBox())) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

//    void populateBlockEntity(FoundationStoneBlockEntity entity);

    Box getAbsoluteBox();

    Box getBox();

    ICoords getMinCoords();
    ICoords getMaxCoords();

    UUID getId();

    void setId(UUID id);

    UUID getNationId();

    void setNationId(UUID nationId);

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
