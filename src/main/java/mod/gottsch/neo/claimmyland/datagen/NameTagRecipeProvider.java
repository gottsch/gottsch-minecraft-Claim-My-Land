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
package mod.gottsch.neo.claimmyland.datagen;

import mod.gottsch.neo.claimmyland.core.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

/**
 * DataGen recipe provider for CML name tag items.
 *
 * <p>Registered in your {@link net.neoforged.neoforge.data.event.GatherDataEvent}
 * handler alongside other recipe providers.
 *
 * <p>Recipes:
 * <ul>
 *   <li>Iron Name Tag — vanilla name tag + iron nugget (shapeless)</li>
 *   <li>Gold Name Tag — vanilla name tag + gold nugget (shapeless)</li>
 * </ul>
 *
 * @author Mark Gottschling on Apr 19, 2026
 */
public class NameTagRecipeProvider extends RecipeProvider {

    public NameTagRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Iron Name Tag — name tag + iron nugget
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.IRON_NAME_TAG.get())
                .requires(Items.NAME_TAG)
                .requires(Items.IRON_NUGGET)
                .unlockedBy("has_name_tag", has(Items.NAME_TAG))
                .save(output);

        // Gold Name Tag — name tag + gold nugget
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.GOLD_NAME_TAG.get())
                .requires(Items.NAME_TAG)
                .requires(Items.GOLD_NUGGET)
                .unlockedBy("has_name_tag", has(Items.NAME_TAG))
                .save(output);
    }
}