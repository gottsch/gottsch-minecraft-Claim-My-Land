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
import mod.gottsch.forge.claimmyland.core.config.Config;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.ObjectUtils;

import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class NationParcel extends AbstractParcel {

    private static final String BORDER_TYPE_KEY = "border_type";

    private NationBorderType borderType;

    /**
     *
     */
    public NationParcel() {
        setType(ParcelType.NATION);
    }

    @Override
    public String randomName() {
        return super.randomName().replace("Parcel", "Nation");
    }

    @Override
    public boolean grantsAccess(Parcel parcel) {
        // TODO grants access to Citizen and Citizen Zone parcel
        return false;
    }

    /**
     * nation deed/parcel do not have access to any other parcel
     * @param parcel
     * @return
     */
    @Override
    public boolean hasAccessTo(Parcel parcel) {
        return false;
    }

    /**
     * this parcel is derived from a deed in this case ie when accessing a Foundation stone.
     * @param blockEntity
     * @return
     */
    @Override
    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return getDeedId().equals(blockEntity.getDeedId())
                && (ObjectUtils.isEmpty(getNationId())) || getNationId().equals(blockEntity.getNationId())
                && (ObjectUtils.isEmpty(getId()) || getId().equals(blockEntity.getParcelId()));
    }

    /*
     * cannot use deed to place foundation stone that is embedded in another parcel
     */
    @Override
    public boolean handleEmbeddedPlacementRules(Parcel registryParcel) {
        return false;
    }

    @Override
    public void save(CompoundTag tag) {
        super.save(tag);
//        tag.putUUID(NATION_ID_KEY, getNationId());
        ClaimMyLand.LOGGER.debug("saved parcel -> {}", this);
    }

    @Override
    public Parcel load(CompoundTag tag) {
        super.load(tag);
//        if (tag.contains(NATION_ID_KEY)) {
//            setNationId(tag.getUUID(NATION_ID_KEY));
//        }
        return this;
    }

    @Override
    public int getBufferSize() {
        return Config.SERVER.general.nationParcelBufferRadius.get();
    }

    @Override
    public String toString() {
        return "NationParcel{" +
                "} " + super.toString();
    }
}
