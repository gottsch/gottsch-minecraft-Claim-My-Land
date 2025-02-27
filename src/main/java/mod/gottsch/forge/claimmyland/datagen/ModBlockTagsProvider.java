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
        tag(ModTags.Blocks.COMMON_NATION_WHITELIST)
                .addTags(
                        BlockTags.WOODEN_DOORS,
                        BlockTags.WOODEN_TRAPDOORS,
                        BlockTags.FENCE_GATES,

                        BlockTags.WOODEN_BUTTONS,
                        BlockTags.WOODEN_PRESSURE_PLATES,

                        BlockTags.STONE_BUTTONS,
                        BlockTags.STONE_PRESSURE_PLATES,

                        BlockTags.ANVIL,
                        BlockTags.BEDS,
                        BlockTags.CAULDRONS,
                        Tags.Blocks.CHESTS,
                        Tags.Blocks.BARRELS,
                        Tags.Blocks.BOOKSHELVES
                )
                .add(
                        Blocks.CRAFTING_TABLE,
                        Blocks.FURNACE,
                        Blocks.BLAST_FURNACE,
                        Blocks.LOOM,
                        Blocks.STONECUTTER,
                        Blocks.SMOKER,
                        Blocks.JUKEBOX,
						Blocks.IRON_DOOR,
                        Blocks.IRON_TRAPDOOR
                );

        tag(ModTags.Blocks.CHEST_BARREL_WHITELIST)
                .addTags(
                        Tags.Blocks.CHESTS,
                        Tags.Blocks.BARRELS);

        tag(ModTags.Blocks.DOOR_GATE_WHITELIST)
                .addTags(
                        BlockTags.WOODEN_DOORS,
                        BlockTags.WOODEN_TRAPDOORS,
                        BlockTags.FENCE_GATES
                ).add(
                        Blocks.IRON_DOOR,
                        Blocks.IRON_TRAPDOOR
                );

        // populate integration tags
        String T2_ID = "treasure2";
        // TODO complete
        tag(ModTags.Blocks.TREASURE2_CHEST_WHITELIST)
                .addOptional(new ResourceLocation(T2_ID, "wooden_chest"))
                .addOptional(new ResourceLocation(T2_ID, "cardboard_box"));

        String LV = "legacyvault";
        tag(ModTags.Blocks.LEGACY_VAULT_WHITELIST)
                .addOptional(new ResourceLocation(LV, "community_vault"));

        // TODO add some more common tags
    }

}
