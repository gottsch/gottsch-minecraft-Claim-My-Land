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
import mod.gottsch.forge.treasure2.core.item.TreasureItems;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
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
                        ItemTags.BOATS
                )
                .add(
                        Items.BOOK,
                        Items.ENCHANTED_BOOK,
                        Items.MAP,
                        Items.POTION
                );


        // populate integration tags
        String MF = "mageflame";
        // TODO complete
        tag(ModTags.Items.MAGEFLAME_SCROLLS_WHITELIST)
                .addOptional(new ResourceLocation(MF, "mage_flame_scroll"))
                .addOptional(new ResourceLocation(MF, "lesser_revelation_scroll"))
                .addOptional(new ResourceLocation(MF, "greater_revelation_scroll"))
                .addOptional(new ResourceLocation(MF, "bubble_flame_scroll"))
                .addOptional(new ResourceLocation(MF, "winged_torch_scroll"))
                .addOptional(new ResourceLocation(MF, "ember_hound_scroll"))
        ;

        tag(ModTags.Items.TREASURE2_KEYS_WHITELIST)
                .addOptional(TreasureItems.WOOD_KEY.getId())
                .addOptional(TreasureItems.STONE_KEY.getId())
                .addOptional(TreasureItems.LEAF_KEY.getId())
                .addOptional(TreasureItems.EMBER_KEY.getId())
                .addOptional(TreasureItems.LIGHTNING_KEY.getId())
                .addOptional(TreasureItems.IRON_KEY.getId())
                .addOptional(TreasureItems.GOLD_KEY.getId())
                .addOptional(TreasureItems.METALLURGISTS_KEY.getId())
                .addOptional(TreasureItems.ONYX_KEY.getId())
                .addOptional(TreasureItems.TOPAZ_KEY.getId())
                .addOptional(TreasureItems.RUBY_KEY.getId())
                .addOptional(TreasureItems.SAPPHIRE_KEY.getId())
                .addOptional(TreasureItems.DIAMOND_KEY.getId())
                .addOptional(TreasureItems.EMERALD_KEY.getId())
                .addOptional(TreasureItems.JEWELLED_KEY.getId())
                .addOptional(TreasureItems.SKELETON_KEY.getId())
                .addOptional(TreasureItems.SPIDER_KEY.getId())
                .addOptional(TreasureItems.WITHER_KEY.getId())
                .addOptional(TreasureItems.ONE_KEY.getId())
                .addOptional(TreasureItems.PILFERERS_LOCK_PICK.getId())
                .addOptional(TreasureItems.THIEFS_LOCK_PICK.getId())
                ;

        // TODO add some more common tags
    }

}
