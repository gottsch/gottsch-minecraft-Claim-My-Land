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
package mod.gottsch.neo.claimmyland.core.parcel;

import mod.gottsch.neo.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.estate.EstateTypeRegistry;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class NationParcel extends AbstractParcel implements INationParcel {

    private List<UUID> blacklist;

    /**
     *
     */
    public NationParcel() {
        super();
        setEstate(EstateTypeRegistry.create(EstateTypeRegistry.NATION_ESTATE_TYPE));
        setType(ParcelType.NATION);
        getEstate().setParcelType(getType());
    }

    public static NationParcel create() {
        return new NationParcel();
    }

//    public static NationParcel create(UUID nationId) {
//        return new NationParcel(nationId);
//    }

    @Override
    public String randomName() {
        return super.randomName().replace("Parcel", "Nation");
    }

    @Override
    public boolean grantsAccess(Parcel virtualParcel) {
//        return virtualParcel.isNation()
//                && this.getOwnerId() == null
//                && ModUtil.getVolume(virtualParcel.getBox()) >= ModUtil.getVolume(this.getBox());
        return false;
    }

    /**
     * nation deed/parcel do not have access to any other parcel
     * @param parcel
     * @return
     */
    @Override
    public boolean hasAccessTo(Parcel parcel) {
        return parcel.isNation();
    }

    /**
     * this parcel is derived from a deed in this case ie when accessing a Foundation stone.
     * @param blockEntity
     * @return
     */
    @Override
    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return getDeedId().equals(blockEntity.getDeedId());
    }

    /**
     * Nations may only be placed in wilderness — any enclosing parcel disqualifies.
     *
     * @author Mark Gottschling — PlacementResult refactor on Apr 12, 2026
     */
    /**
     * Nations may only be placed in wilderness — any enclosing parcel disqualifies.
     *
     * @author Mark Gottschling — PlacementResult refactor on Apr 12, 2026
     */
    @Override
    public PlacementResult canPlaceAt(Level level, ICoords coords) {
        String dimension = level.dimension().location().toString();
        Optional<Parcel> enclosing = ParcelRegistry.findLeastSignificant(coords, dimension);
        if (enclosing.isPresent()) {
            return PlacementResult.INVALID_PARENT_TYPE;
        }
        return PlacementResult.SUCCESS;
    }

    @Override
    public ClaimResult handleEmbeddedClaim(Level level, Parcel parentParcel) { //}, Box parcelBox) {
        return ClaimResult.FAILURE;
    }


    @Override
    public int getBufferSize() {
        return Config.SERVER.general.nationParcelBufferRadius.get();
    }

    @Override
    public List<UUID> getBlacklist() {
        if (blacklist == null) {
            blacklist = new ArrayList<>();
        }
        return blacklist;
    }

    @Deprecated(forRemoval = true, since = "2.0")
    @Override
    public void setBlacklist(List<UUID> blacklist) {
        this.blacklist = blacklist;
    }

    @Override
    public String toString() {
        return "NationParcel{" +
                "} " + super.toString();
    }
}
