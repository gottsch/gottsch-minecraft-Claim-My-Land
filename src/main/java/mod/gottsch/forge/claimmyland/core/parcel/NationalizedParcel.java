/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
 *
 */

package mod.gottsch.forge.claimmyland.core.parcel;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.NationEstate;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

/**
 * @author by Mark Gottschling on 2/9/2026
 */
public interface NationalizedParcel extends Parcel {
    public static final String NATION_ESTATE_KEY = "nation_estate";
//    boolean isSameNation(NationalizedParcel citizenParcel);

    default public boolean isSameNation(NationalizedParcel citizenParcel) {
        NationEstate myNation = getNationEstate();
        NationEstate theirNation = citizenParcel.getNationEstate();
        return myNation != null && theirNation != null
                && myNation.getId().equals(theirNation.getId());
    }

    default NationAccessType getAccessType() {
        return Optional.ofNullable(getNationEstate())
                .map(NationEstate::getAccessType)
                .orElse(NationAccessType.CLOSED);
    }

    default void saveNationEstate(CompoundTag tag) {
        if (getNationEstate() != null) {
            tag.put(NATION_ESTATE_KEY, getNationEstate().save(new CompoundTag()));
        }
    }

    default boolean loadNationEstate(CompoundTag tag) {
        if (!tag.contains(NATION_ESTATE_KEY)) {
            ClaimMyLand.LOGGER.warn("unable to load parcel - missing nation estate data.");
            return false;
        }

        CompoundTag nationEstateTag = tag.getCompound(NATION_ESTATE_KEY);
        EstateRegistry.get(nationEstateTag.getUUID(Estate.ID_KEY))
                .ifPresentOrElse(
                        estate -> setNationEstate((NationEstate) estate),
                        () -> {
                            getNationEstate().load(nationEstateTag);
                            EstateRegistry.register(getNationEstate());
                        });
        return true;
    }

    NationEstate getNationEstate();

    void setNationEstate(NationEstate nationEstate);
}
