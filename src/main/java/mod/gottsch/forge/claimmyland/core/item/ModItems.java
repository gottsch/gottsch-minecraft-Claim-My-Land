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

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.setup.Registration;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;
import java.util.function.Supplier;

/**
 *
 */
public class ModItems {
    public static RegistryObject<Item> PLAYER_DEED = Registration.ITEMS.register("player_deed", () -> new PlayerDeed(new Item.Properties()));
    public static RegistryObject<Item> NATION_DEED = Registration.ITEMS.register("nation_deed", () -> new NationDeed(new Item.Properties()));
    public static RegistryObject<Item> CITIZEN_DEED = Registration.ITEMS.register("citizen_deed", () -> new CitizenDeed(new Item.Properties()));

    // pre-set 10x20x10 player parcel
    public static RegistryObject<Item> PLAYER_DEED_10 = Registration.ITEMS.register("player_deed_10", () -> new PlayerDeed(new Item.Properties()) {
            @Override
            protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
                if (deed.getTag() != null && !deed.getTag().contains(Deed.SIZE)) {
                    DeedFactory.createPlayerDeed(deed, new Box(Coords.of(0, -10, 0), Coords.of(9, 9, 9)));
                }
                super.populateFoundationStone(blockEntity, deed, pos, player);
            }
        }
    );

    // pre-set 16x32x16 player parcel
    public static RegistryObject<Item> PLAYER_DEED_16 = Registration.ITEMS.register("player_deed_16", () -> new PlayerDeed(new Item.Properties()) {
        @Override
        protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
            if (deed.getTag() != null && !deed.getTag().contains(Deed.SIZE)) {
                DeedFactory.createPlayerDeed(deed, new Box(Coords.of(0, -16, 0), Coords.of(15, 15, 15)));
            }
            super.populateFoundationStone(blockEntity, deed, pos, player);
        }
    });

    // pre-set 32x64x32 player parcel
    public static RegistryObject<Item> PLAYER_DEED_32 = Registration.ITEMS.register("player_deed_32", () -> new PlayerDeed(new Item.Properties()) {
        @Override
        protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
            if (deed.getTag() != null && !deed.getTag().contains(Deed.SIZE)) {
                DeedFactory.createPlayerDeed(deed, new Box(Coords.of(0, -32, 0), Coords.of(31, 31, 31)));
            }
            super.populateFoundationStone(blockEntity, deed, pos, player);
        }
    });

    // pre-set 100x100 nation parcel
    public static RegistryObject<Item> NATION_DEED_100 = Registration.ITEMS.register("nation_deed_100", () -> new NationDeed(new Item.Properties()) {
        @Override
        protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
            if (deed.getTag() != null && !deed.getTag().contains(Deed.SIZE)) {
                DeedFactory.createNationDeed(blockEntity.getLevel(), deed, new Box(Coords.of(0, 0, 0), Coords.of(99, 0, 99)));
            }
            super.populateFoundationStone(blockEntity, deed, pos, player);
        }
    });

    public static RegistryObject<Item> BORDER_STONE = fromBorderStone(ModBlocks.BORDER_STONE, Item.Properties::new);
    public static RegistryObject<Item> CITIZEN_PLACEMENT_TOOL = fromCitizenPlacement(ModBlocks.CITIZEN_PLACEMENT_BLOCK, Item.Properties::new);
    public static RegistryObject<Item> ZONING_PLACEMENT_TOOL = fromZonePlacement(ModBlocks.ZONE_PLACEMENT_BLOCK, Item.Properties::new);


    // tools
//    public static RegistryObject<Item> ZONING_TOOL = Registration.ITEMS.register("zoning_tool", () -> new ZoningTool(new Item.Properties()));

    /**
     *
     * @param bus
     */
    public static void register(IEventBus bus) {
        // cycle through all block and create items
        Registration.registerItems(bus);
    }

    public static void createPlayerParcel(ItemStack stack, Box size) {
        CompoundTag tag = stack.getOrCreateTag();
        // create a relative sized Box
        tag.putUUID(Deed.PARCEL_ID, UUID.randomUUID());
        tag.putUUID(Deed.DEED_ID, UUID.randomUUID());
        tag.putString(Deed.PARCEL_TYPE, ParcelType.PLAYER.name());
        CompoundTag sizeTag = new CompoundTag();
        size.save(sizeTag);
        tag.put(Deed.SIZE, sizeTag);
    }

    // convenience method: take a RegistryObject<Block> and make a corresponding RegistryObject<Item> from it
    public static <B extends Block> RegistryObject<Item> fromBlock(RegistryObject<B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), itemProperties.get()));
    }

    public static <B extends Block> RegistryObject<Item> fromBorderStone(RegistryObject<B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new BorderStoneBlockItem(block.get(), itemProperties.get()));
    }

    public static <B extends Block> RegistryObject<Item> fromCitizenPlacement(RegistryObject<B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new CitizenTool(block.get(), itemProperties.get()));
    }
    public static <B extends Block> RegistryObject<Item> fromZonePlacement(RegistryObject<B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new ZoningTool(block.get(), itemProperties.get()));
    }
}
