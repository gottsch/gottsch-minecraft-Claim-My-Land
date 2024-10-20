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
package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ObjectUtils;

import java.util.UUID;

/**
 * TODO currently there aren't any usages of this class outside this class.
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class DeedFactory {

    private DeedFactory() {}

    public static ItemStack createDeed(Class clazz, Box size) {
        if (ModItems.PLAYER_DEED.get().getClass().equals(clazz)) {
            return createPlayerDeed(size);
        }
        // TODO add other types
        else {
            return createPlayerDeed(size);
        }
    }

    public static ItemStack createPlayerDeed(Box size) {
        ItemStack deed = createItemStack(ParcelType.PLAYER);
        return createPlayerDeed(deed, size);
    }

    public static ItemStack createPlayerDeed(ItemStack deed, Box size) {
        if (deed == ItemStack.EMPTY || deed == null) {
            return deed;
        }

        CompoundTag tag = deed.getOrCreateTag();
        // add the ids
        // TODO this is probably going away in favor of deed id for the access checks
        tag.putUUID(Deed.PARCEL_ID, UUID.randomUUID());
        tag.putUUID(Deed.DEED_ID, UUID.randomUUID());
        // add the type
        tag.putString(Deed.PARCEL_TYPE, ParcelType.PLAYER.name());
        // add the size
        CompoundTag sizeTag = new CompoundTag();
        size.save(sizeTag);
        tag.put(Deed.SIZE, sizeTag);

        return deed;
    }

    public static ItemStack createPlayerDeed(UUID ownerId, Box size) {
        ItemStack deed = createPlayerDeed(size);
        CompoundTag tag = deed.getTag();
        if (ObjectUtils.isNotEmpty(ownerId) && tag != null) {
            tag.putUUID(Deed.OWNER_ID, ownerId);
        }
        return deed;
    }

    public static ItemStack createNationDeed(Level level, Box size) {
        ItemStack deed = createItemStack(ParcelType.NATION);
        return createNationDeed(level, deed, size);
    }

    public static ItemStack createNationDeed(Level level, ItemStack deed, Box size) {
        if (deed == ItemStack.EMPTY || deed == null) {
            return deed;
        }

        CompoundTag tag = deed.getOrCreateTag();
        tag.putUUID(NationDeed.NATION_ID, UUID.randomUUID());
        // TODO this should be refactored out
        tag.putUUID(Deed.PARCEL_ID, UUID.randomUUID());
        tag.putUUID(Deed.DEED_ID, UUID.randomUUID());
        tag.putString(Deed.PARCEL_TYPE, ParcelType.NATION.name());
        CompoundTag sizeTag = new CompoundTag();
        // modify size to max y limits
        size.setMinCoords(size.getMinCoords().withY(level.getMinBuildHeight()));
        size.setMaxCoords(size.getMaxCoords().withY(level.getMaxBuildHeight()-1));
        size.save(sizeTag);
        tag.put(Deed.SIZE, sizeTag);

        return deed;
    }

    public static ItemStack createCitizenDeed(Box size, UUID nationId) {
        ItemStack deed = createItemStack(ParcelType.CITIZEN);
        CompoundTag tag = deed.getOrCreateTag();
        // add the ids
        tag.putUUID(NationDeed.NATION_ID, nationId);
        // TODO this should be refactored out
        tag.putUUID(Deed.PARCEL_ID, UUID.randomUUID());
        tag.putUUID(Deed.DEED_ID, UUID.randomUUID());
        // add the type
        tag.putString(Deed.PARCEL_TYPE, ParcelType.CITIZEN.name());
        // add the size
        CompoundTag sizeTag = new CompoundTag();
        size.save(sizeTag);
        tag.put(Deed.SIZE, sizeTag);

        return deed;
    }

    private static ItemStack createItemStack(ParcelType type) {
        return switch(type) {
            case PLAYER -> new ItemStack(ModItems.PLAYER_DEED.get());
            case NATION -> new ItemStack(ModItems.NATION_DEED.get());
            case CITIZEN -> new ItemStack(ModItems.CITIZEN_DEED.get());
            case ZONE -> null; //new ItemStack((ModItems.CITIZEN_DEED.get()));
        };
    }

}
