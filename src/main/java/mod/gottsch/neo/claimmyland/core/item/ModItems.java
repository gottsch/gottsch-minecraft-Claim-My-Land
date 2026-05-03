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

import mod.gottsch.neo.claimmyland.core.block.ModBlocks;
import mod.gottsch.neo.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.neo.claimmyland.core.setup.Registration;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 *
 */
public class ModItems {
    public static DeferredHolder<Item, PlayerDeed> PLAYER_DEED = Registration.ITEMS.register("player_deed", () -> new PlayerDeed(new Item.Properties()));
    public static DeferredHolder<Item, NationDeed> NATION_DEED = Registration.ITEMS.register("nation_deed", () -> new NationDeed(new Item.Properties()));
    public static DeferredHolder<Item, CitizenDeed> CITIZEN_DEED = Registration.ITEMS.register("citizen_deed", () -> new CitizenDeed(new Item.Properties()));

    // pre-set 10x20x10 player parcel
    public static DeferredHolder<Item, PlayerDeed> PLAYER_DEED_10 = Registration.ITEMS.register("player_deed_10", () -> new PlayerDeed(new Item.Properties()) {
            @Override
            protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
                CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                if (!tag.contains(Deed.SIZE)) {
                    DeedFactory.createPlayerDeed(deed, new Box(Coords.of(0, -10, 0), Coords.of(9, 9, 9)));
                }
                super.populateFoundationStone(blockEntity, deed, pos, player);
            }
        }
    );

    // pre-set 16x32x16 player parcel
    public static DeferredHolder<Item, PlayerDeed> PLAYER_DEED_16 = Registration.ITEMS.register("player_deed_16", () -> new PlayerDeed(new Item.Properties()) {
        @Override
        protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
            CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!tag.contains(Deed.SIZE)) {
                DeedFactory.createPlayerDeed(deed, new Box(Coords.of(0, -16, 0), Coords.of(15, 15, 15)));
            }
            super.populateFoundationStone(blockEntity, deed, pos, player);
        }
    });

    // pre-set 32x64x32 player parcel
    public static DeferredHolder<Item, PlayerDeed> PLAYER_DEED_32 = Registration.ITEMS.register("player_deed_32", () -> new PlayerDeed(new Item.Properties()) {
        @Override
        protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
            CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!tag.contains(Deed.SIZE)) {
                DeedFactory.createPlayerDeed(deed, new Box(Coords.of(0, -32, 0), Coords.of(31, 31, 31)));
            }
            super.populateFoundationStone(blockEntity, deed, pos, player);
        }
    });

    // pre-set 100x100 nation parcel
    public static DeferredHolder<Item, NationDeed> NATION_DEED_100 = Registration.ITEMS.register("nation_deed_100", () -> new NationDeed(new Item.Properties()) {
        @Override
        protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
            CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!tag.contains(Deed.SIZE)) {
                DeedFactory.createNationDeed(blockEntity.getLevel(), deed, new Box(Coords.of(0, 0, 0), Coords.of(99, 0, 99)));
            }
            super.populateFoundationStone(blockEntity, deed, pos, player);
        }
    });

    // NOTE these exist as item blocks, for the sole reason of displaying in a Patchouli book.
    public static DeferredHolder<Item, BlockItem> PLAYER_FOUNDATION_STONE = fromBlock(ModBlocks.PLAYER_FOUNDATION_STONE, Item.Properties::new);
    public static DeferredHolder<Item, BlockItem> CITIZEN_FOUNDATION_STONE = fromBlock(ModBlocks.CITIZEN_FOUNDATION_STONE, Item.Properties::new);
    public static DeferredHolder<Item, BlockItem> NATION_FOUNDATION_STONE = fromBlock(ModBlocks.NATION_FOUNDATION_STONE, Item.Properties::new);

    public static DeferredHolder<Item, BlockItem> BORDER_STONE = fromBorderStone(ModBlocks.BORDER_STONE, Item.Properties::new);
    public static DeferredHolder<Item, BlockItem> CITIZEN_PLACEMENT_TOOL = fromCitizenPlacement(ModBlocks.CITIZEN_PLACEMENT_BLOCK, Item.Properties::new);
    public static DeferredHolder<Item, BlockItem> ZONING_PLACEMENT_TOOL = fromZonePlacement(ModBlocks.ZONE_PLACEMENT_BLOCK, Item.Properties::new);


    // tools
//    public static DeferredHolder<Item> ZONING_TOOL = Registration.ITEMS.register("zoning_tool", () -> new ZoningTool(new Item.Properties()));
    public static final DeferredHolder<Item, IronNameTagItem> IRON_NAME_TAG =
            Registration.ITEMS.register("iron_name_tag",
                    () -> new IronNameTagItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, GoldNameTagItem> GOLD_NAME_TAG =
            Registration.ITEMS.register("gold_name_tag",
                    () -> new GoldNameTagItem(new Item.Properties().stacksTo(16)));

    /**
     *
     * @param bus
     */
    public static void register(IEventBus bus) {
        // cycle through all block and create items
        Registration.registerItems(bus);
    }

//    public static void createPlayerParcel(ItemStack stack, Box size) {
//        CompoundTag tag = stack.getOrCreateTag();
//        // create a relative sized Box
//        tag.putUUID(Deed.PARCEL_ID, UUID.randomUUID());
//        tag.putUUID(Deed.DEED_ID, UUID.randomUUID());
//        tag.putString(Deed.PARCEL_TYPE, ParcelType.PLAYER.name());
//        CompoundTag sizeTag = new CompoundTag();
//        size.save(sizeTag);
//        tag.put(Deed.SIZE, sizeTag);
//    }

    // convenience method: take a DeferredHolder<Block> and make a corresponding DeferredHolder<Item> from it
    public static <B extends Block> DeferredHolder<Item, BlockItem> fromBlock(DeferredHolder<Block, B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), itemProperties.get()));
    }

    public static <B extends Block> DeferredHolder<Item, BlockItem> fromFoundationStone(DeferredHolder<Block, B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), itemProperties.get()));
    }

    public static <B extends Block> DeferredHolder<Item, BlockItem> fromBorderStone(DeferredHolder<Block, B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new BorderStoneBlockItem(block.get(), itemProperties.get()));
    }

    public static <B extends Block> DeferredHolder<Item, BlockItem> fromCitizenPlacement(DeferredHolder<Block, B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new CitizenTool(block.get(), itemProperties.get()));
    }
    public static <B extends Block> DeferredHolder<Item, BlockItem> fromZonePlacement(DeferredHolder<Block, B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new ZoningTool(block.get(), itemProperties.get()));
    }
}
