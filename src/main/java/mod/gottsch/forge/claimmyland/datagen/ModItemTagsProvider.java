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
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * @author Mark Gottschling on Feb 26, 2025
 */
public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider,
                               CompletableFuture<TagLookup<Block>> blockTagProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTagProvider, ClaimMyLand.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(Provider provider) {
        tag(ModTags.Items.COMMON_NATION_WHITELIST)
                .addTags(

                )
                .add(
                        Items.BOOK,
                        Items.ENCHANTED_BOOK,
                        Items.MAP
                );


        // populate integration tags
        String MF = "mageflame";
        // TODO complete
        tag(ModTags.Items.MAGEFLAME_SCROLLS_WHITELIST)
                .addOptional(new ResourceLocation(MF, "mage_flame"))
                .addOptional(new ResourceLocation(MF, "lesser_revelation"))
                .addOptional(new ResourceLocation(MF, "greater_revelation"))
                .addOptional(new ResourceLocation(MF, "bubble_flame"))
                .addOptional(new ResourceLocation(MF, "winged_torch"))
                .addOptional(new ResourceLocation(MF, "ember_hound"))
        ;

        // TODO add some more common tags
    }

}
