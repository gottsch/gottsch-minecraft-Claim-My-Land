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

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class ParcelFactory {

    public static Optional<Parcel> create(CompoundTag tag) {
        if (tag.contains(AbstractParcel.TYPE)) {
            String type = tag.getString(AbstractParcel.TYPE);

            if (type.equalsIgnoreCase(ParcelType.PLAYER.getSerializedName())) {
                return Optional.of(new PlayerParcel());
            }
            else if (type.equalsIgnoreCase(ParcelType.CITIZEN.getSerializedName())) {
                return Optional.of(new CitizenParcel());
            }
            // TODO add citizen zone
            else if (type.equalsIgnoreCase(ParcelType.NATION.getSerializedName())) {
                return Optional.of(new NationParcel());
            }
        }
        return Optional.empty();
    }

    /**
     *
     * @param type
     * @return
     */
    public static Optional<Parcel> create(ParcelType type) {
        return switch (type) {
            case PLAYER -> Optional.of(createPlayerParcel());
            case NATION -> Optional.of(createNationParcel());
            case CITIZEN -> Optional.of(createCitizenParcel());
            default -> Optional.empty();
        };
    }

    private static Parcel createPlayerParcel() {
        Parcel parcel = new PlayerParcel();
        parcel.setId(UUID.randomUUID());
        parcel.setName(parcel.randomName());
        return parcel;
    }

    private static Parcel createCitizenParcel() {
        Parcel parcel = new CitizenParcel();
        parcel.setId(UUID.randomUUID());
        parcel.setName(parcel.randomName());
        return parcel;
    }

    private static Parcel createNationParcel() {
        NationParcel parcel = new NationParcel();
        parcel.setId(UUID.randomUUID());
        parcel.setNationId(UUID.randomUUID());
        parcel.setName(parcel.randomName());
        return parcel;
    }
}
