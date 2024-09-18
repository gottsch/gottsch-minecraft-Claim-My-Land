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
import mod.gottsch.forge.claimmyland.core.config.Config;
import net.minecraft.nbt.CompoundTag;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class PersonalParcel extends AbstractParcel {

    /**
     *
     */
    public PersonalParcel() {
        setType(ParcelType.PERSONAL);
    }

    @Override
    public boolean hasAccessTo(Parcel otherParcel) {
        return switch (otherParcel.getType()) {
            case CITIZEN -> {yield true;}
            case CITIZEN_CLAIM_ZONE -> {yield true;}
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
       return getId().equals(blockEntity.getParcelId());
    }

    @Override
    public void save(CompoundTag tag) {
        super.save(tag);
        tag.putString(TYPE, "personal");
    }

    @Override
    public Parcel load(CompoundTag tag) {
        super.load(tag);
        return this;
    }

    @Override
    public int getBufferSize() {
        return Config.SERVER.general.parcelBufferRadius.get();
    }

    @Override
    public String toString() {
        return "PersonalParcel{} " + super.toString();
    }
}
