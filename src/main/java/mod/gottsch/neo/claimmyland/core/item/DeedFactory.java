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
package mod.gottsch.neo.claimmyland.core.item;

import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ObjectUtils;

import java.util.UUID;

/**
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

        CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        // add the size
        CompoundTag sizeTag = new CompoundTag();
        size.save(sizeTag);
        tag.put(Deed.SIZE, sizeTag);
        deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        return deed;
    }

    public static ItemStack createPlayerDeed(UUID ownerId, Box size) {
        ItemStack deed = createPlayerDeed(size);
        CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (ObjectUtils.isNotEmpty(ownerId)) {
            tag.putUUID(Deed.OWNER_ID, ownerId);
            deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
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

        CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag sizeTag = new CompoundTag();
        // create a copy — do not mutate the original size
        Box deedSize = new Box(
                Coords.of(size.getMinCoords().getX(), size.getMinCoords().getY(), size.getMinCoords().getZ()),
                Coords.of(size.getMaxCoords().getX(), size.getMaxCoords().getY(), size.getMaxCoords().getZ())
        );
        deedSize.setMinCoords(deedSize.getMinCoords().withY(level.getMinBuildHeight()));
        deedSize.setMaxCoords(deedSize.getMaxCoords().withY(level.getMaxBuildHeight()-1));
        deedSize.save(sizeTag);
        tag.put(Deed.SIZE, sizeTag);
        deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return deed;
    }

    public static ItemStack createCitizenDeed(Box size, UUID nationId, String nationName) {
        ItemStack deed = createItemStack(ParcelType.CITIZEN);
        CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        // add the ids
        tag.putUUID(NationDeed.NATION_ESTATE_ID, nationId);
        tag.putString(NationDeed.NATION_ESTATE_NAME, nationName);

        // add the size
        CompoundTag sizeTag = new CompoundTag();
        size.save(sizeTag);
        tag.put(Deed.SIZE, sizeTag);
        deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return deed;
    }

    private static ItemStack createItemStack(ParcelType type) {
        return switch(type) {
            case PLAYER -> new ItemStack(ModItems.PLAYER_DEED.get());
            case NATION -> new ItemStack(ModItems.NATION_DEED.get());
            case CITIZEN -> new ItemStack(ModItems.CITIZEN_DEED.get());
            case ZONE -> null; //new ItemStack((ModItems.CITIZEN_DEED.get()));
            default -> null;
        };
    }

    public static ItemStack createTieredDeed(RandomSource random, String tier) {
        int roll = random.nextInt(100);
        return switch (tier) {
            case "uncommon" -> {
                if (roll < 30)       yield createPlayerDeed(new Box(Coords.of(0, -10, 0), Coords.of(9, 9, 9)));
                else if (roll < 80)  yield createPlayerDeed(new Box(Coords.of(0, -16, 0), Coords.of(15, 15, 15)));
                else                 yield createPlayerDeed(new Box(Coords.of(0, -32, 0), Coords.of(31, 31, 31)));
            }
            case "rare" -> {
                if (roll < 30)       yield createPlayerDeed(new Box(Coords.of(0, -16, 0), Coords.of(15, 15, 15)));
                else if (roll < 90)  yield createPlayerDeed(new Box(Coords.of(0, -32, 0), Coords.of(31, 31, 31)));
                else                 yield createNationDeedForLoot();
            }
            case "epic" -> {
                if (roll < 60)       yield createPlayerDeed(new Box(Coords.of(0, -32, 0), Coords.of(31, 31, 31)));
                else                 yield createNationDeedForLoot();
            }
            default -> { // "common"
                if (roll < 60)       yield createPlayerDeed(new Box(Coords.of(0, -10, 0), Coords.of(9, 9, 9)));
                else if (roll < 90)  yield createPlayerDeed(new Box(Coords.of(0, -16, 0), Coords.of(15, 15, 15)));
                else                 yield createPlayerDeed(new Box(Coords.of(0, -32, 0), Coords.of(31, 31, 31)));
            }
        };
    }

    private static ItemStack createNationDeedForLoot() {
        ItemStack deed = createItemStack(ParcelType.NATION);
        CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        Box size = new Box(Coords.of(0, -64, 0), Coords.of(99, 319, 99));
        CompoundTag sizeTag = new CompoundTag();
        size.save(sizeTag);
        tag.put(Deed.SIZE, sizeTag);
        deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return deed;
    }
}
