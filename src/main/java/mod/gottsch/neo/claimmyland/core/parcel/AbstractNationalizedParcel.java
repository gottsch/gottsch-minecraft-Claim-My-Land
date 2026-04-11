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

package mod.gottsch.neo.claimmyland.core.parcel;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.estate.NationEstate;
import mod.gottsch.neo.claimmyland.core.estate.NationEstateContext;
import mod.gottsch.neo.claimmyland.core.registry.EstateRegistry;
import net.minecraft.nbt.CompoundTag;

/**
 * @author by Mark Gottschling on 2/9/2026
 */
@Deprecated
public abstract class AbstractNationalizedParcel extends AbstractParcel implements NationalizedParcel {
    public static final String NATION_ESTATE_KEY = "nation_estate";

    // nation-ownership token
    private NationEstate nationEstate;

    public AbstractNationalizedParcel() {
        super();
        this.nationEstate = new NationEstateContext();
    }

    public AbstractNationalizedParcel(NationEstate nationEstate) {
        super();
        setNationEstate(nationEstate);
    }

    @Override
    public boolean isSameNation(NationalizedParcel citizenParcel) {
        NationEstate myNation = getNationEstate();
        NationEstate theirNation = citizenParcel.getNationEstate();
        return myNation != null && theirNation != null
                && myNation.getId().equals(theirNation.getId());
    }

    @Override
    public void save(CompoundTag tag) {
//        ClaimMyLand.LOGGER.debug("saving nationalized parcel -> {}", this);

        if (nationEstate == null || nationEstate.getId() == null) {
//            ClaimMyLand.LOGGER.warn("Unable to save parcel {} - missing a valid Nation Estate. This is an issue!", getId());
            return;
        }
        tag.put(NATION_ESTATE_KEY, nationEstate.save(new CompoundTag()));

        super.save(tag);
    }

    @Override
    public Parcel load(CompoundTag tag) {
        if (tag.contains(NATION_ESTATE_KEY)) {
            CompoundTag nationEstateTag = tag.getCompound(NATION_ESTATE_KEY);

            EstateRegistry.get(nationEstateTag.getUUID(Estate.ID_KEY))
                    .ifPresentOrElse(estate -> setNationEstate((NationEstate) estate),
                            () -> getNationEstate().load(nationEstateTag));

        } else {
            ClaimMyLand.LOGGER.warn("Unable to load parcel - missing nation estate data.");
//            return null;
//            if (loadNationEstateFromLegacy(tag) == null) { // TODO look into more
                return null;
//            }
        }

        return super.load(tag);
    }

//    private Estate loadNationEstateFromLegacy(CompoundTag tag) {
//        NationEstate estate = getNationEstate();
//
//        // TODO load nation info by the nationID guuid
//        // NOTE legacy nationId is a UUID separate from the NationParcel id/parcelId
//        //  used to identify and link parcels by nation.
//        // TODO check estate registry for nation by nationId
//        // TODO if not found create a new nation estate using the nationId and registry it.
//        // TODO update NationParcel load() because it is not a NationalizedParcel and does not
//        //  check for nation parcels by nationId by default.
//
//        // get the nationId
//        UUID nationId;
//        if (tag.contains(NATION_ID_KEY)) {
//            nationId = tag.getUUID(NATION_ID_KEY);
//        } else {
//            return null;
//        }
//
//        EstateRegistry.get(nationId)
//                .ifPresentOrElse(e -> setNationEstate((NationEstate) e),
//                        () -> {
//                    // TODO legacy load
//                            // TODO find the nation in the nation registry
//                            // TODO not found, populate nation estate with nation id
//                            //
//                            List<Parcel> nations = ParcelRegistry.findByNationId(nationId);
//                            if (!nations.isEmpty()) {
//
//                            } else {
//
//                            }
//                        });
//
//
//        return estate;
//    }

    @Override
    public NationEstate getNationEstate() {
        return nationEstate;
    }

    @Override
    public void setNationEstate(NationEstate nationEstate) {
        this.nationEstate = nationEstate;
    }
}
