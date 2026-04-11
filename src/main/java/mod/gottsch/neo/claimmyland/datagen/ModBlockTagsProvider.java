/*
 * This file is part of Legacy Vault.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
 *
 * All rights reserved.
 *
 * Legacy Vault is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Legacy Vault is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Legacy Vault.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.neo.claimmyland.datagen;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.block.ModBlocks;
import mod.gottsch.neo.claimmyland.core.tags.ModTags;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;


import java.util.concurrent.CompletableFuture;

/**
 * @author Mark Gottschling on Feb 24, 2025
 */
public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider,
                                ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ClaimMyLand.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(Provider provider) {

        // add to minecraft tags
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(
                        ModBlocks.BORDER_STONE.get(),
                        ModBlocks.PLAYER_FOUNDATION_STONE.get(),
                        ModBlocks.CITIZEN_FOUNDATION_STONE.get(),
                        ModBlocks.NATION_FOUNDATION_STONE.get()
                        );
        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(
                        ModBlocks.BORDER_STONE.get(),
                        ModBlocks.PLAYER_FOUNDATION_STONE.get(),
                        ModBlocks.CITIZEN_FOUNDATION_STONE.get(),
                        ModBlocks.NATION_FOUNDATION_STONE.get()
                );

        // custom tags
        tag(ModTags.Blocks.FIRE_BLOCKS)
                .add(
                        Blocks.FIRE,
                        Blocks.SOUL_FIRE
                );

        tag(ModTags.Blocks.DOOR_GATE_WHITELIST)
                .addTags(
                        BlockTags.WOODEN_DOORS,
                        BlockTags.WOODEN_TRAPDOORS,
                        BlockTags.FENCE_GATES
                ).add(
                        Blocks.IRON_DOOR,
                        Blocks.IRON_TRAPDOOR
                );

        tag(ModTags.Blocks.CHEST_BARREL_WHITELIST)
                .addTags(
                        Tags.Blocks.CHESTS,
                        Tags.Blocks.BARRELS);

        tag(ModTags.Blocks.CRAFTING_WHITELIST)
                .add(
                        Blocks.CRAFTING_TABLE,
                        Blocks.FURNACE,
                        Blocks.BLAST_FURNACE,
                        Blocks.LOOM,
                        Blocks.STONECUTTER,
                        Blocks.SMOKER
                );

        tag(ModTags.Blocks.COMMON_NATION_WHITELIST)
                .addTags(
                        ModTags.Blocks.DOOR_GATE_WHITELIST,
                        ModTags.Blocks.CHEST_BARREL_WHITELIST,
                        ModTags.Blocks.CRAFTING_WHITELIST,

                        BlockTags.WOODEN_BUTTONS,
                        BlockTags.WOODEN_PRESSURE_PLATES,

                        BlockTags.STONE_BUTTONS,
                        BlockTags.STONE_PRESSURE_PLATES,

                        BlockTags.ANVIL,
                        BlockTags.BEDS,

                        BlockTags.CAULDRONS,
                        Tags.Blocks.BOOKSHELVES
                )
                .add(
                        Blocks.JUKEBOX
	                );

        // Treasure2 isn't available yet for Neoforge 1.21.1
        // populate integration tags
//        tag(ModTags.Blocks.TREASURE2_CHEST_WHITELIST)
//                .addOptional(ModUtil.getName(TreasureBlocks.WOOD_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.CRATE_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.MOLDY_CRATE_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.IRONBOUND_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.CARDBOARD_BOX.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.MILK_CRATE.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.PIRATE_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.IRON_STRONGBOX.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.GOLD_STRONGBOX.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.VIKING_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.SAFE.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.DREAD_PIRATE_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.COMPRESSOR_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.SKULL_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.GOLD_SKULL_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.CRYSTAL_SKULL_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.SPIDER_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.WITHER_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.CAULDRON_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.BARREL_CHEST.get()))
//                .addOptional(ModUtil.getName(TreasureBlocks.VANILLA_CHEST.get()));

        String LV = "legacyvault";
        tag(ModTags.Blocks.LEGACY_VAULT_WHITELIST)
                .addOptional(ResourceLocation.fromNamespaceAndPath(LV, "community_vault"));

        String MCWF = "mcwfurnitures";
        tag(ModTags.Blocks.MACAWS_FURNITURE_WHITELIST)
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "bookshelf"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "bookshelf_cupboard"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "bookshelf_drawer"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "cabinet"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "chair"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "cupboard_counter"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "double_drawer"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "double_drawer_counter"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "large_drawer"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "lower_bookshelf_drawer"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "lower_triple_drawer"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "modern_chair"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "modern_wardrobe"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "stool_chair"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "striped_chair"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "triple_drawer"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath(MCWF, "wardrobe"))
        ;
    }

}
