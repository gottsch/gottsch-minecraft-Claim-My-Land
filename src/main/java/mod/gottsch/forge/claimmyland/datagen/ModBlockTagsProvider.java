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
package mod.gottsch.forge.claimmyland.datagen;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.tags.ModTags;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.treasure2.core.block.TreasureBlocks;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

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

        // populate integration tags
//        String T2_ID = "treasure2";
        // TODO complete
        tag(ModTags.Blocks.TREASURE2_CHEST_WHITELIST)
                .addOptional(ModUtil.getName(TreasureBlocks.WOOD_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.CRATE_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.MOLDY_CRATE_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.IRONBOUND_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.CARDBOARD_BOX.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.MILK_CRATE.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.PIRATE_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.IRON_STRONGBOX.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.GOLD_STRONGBOX.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.VIKING_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.SAFE.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.DREAD_PIRATE_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.COMPRESSOR_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.SKULL_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.GOLD_SKULL_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.CRYSTAL_SKULL_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.SPIDER_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.WITHER_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.CAULDRON_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.BARREL_CHEST.get()))
                .addOptional(ModUtil.getName(TreasureBlocks.VANILLA_CHEST.get()));
        String LV = "legacyvault";
        tag(ModTags.Blocks.LEGACY_VAULT_WHITELIST)
                .addOptional(new ResourceLocation(LV, "community_vault"));

        // TODO add some more common tags
    }

}
