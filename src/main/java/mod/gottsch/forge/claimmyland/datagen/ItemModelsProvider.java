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
package mod.gottsch.forge.claimmyland.datagen;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 
 * @author Mark Gottschling Mar 19, 2024
 *
 */
public class ItemModelsProvider extends ItemModelProvider {

	public ItemModelsProvider(PackOutput generator, ExistingFileHelper existingFileHelper) {
		super(generator, ClaimMyLand.MOD_ID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		// tabs

		// deeds
		singleTexture("player_deed",
				mcLoc("item/generated"), "layer0", modLoc("item/player_deed"));
		singleTexture("nation_deed",
				mcLoc("item/generated"), "layer0", modLoc("item/nation_deed"));
		singleTexture("citizen_deed",
				mcLoc("item/generated"), "layer0", modLoc("item/citizen_deed"));

		// premade deeds
		singleTexture("player_deed_10",
				mcLoc("item/generated"), "layer0", modLoc("item/player_deed_10"));

		singleTexture("player_deed_16",
				mcLoc("item/generated"), "layer0", modLoc("item/player_deed_16"));

		singleTexture("player_deed_32",
				mcLoc("item/generated"), "layer0", modLoc("item/player_deed_32"));

		singleTexture(ModItems.NATION_DEED_100.getId().getPath(),
				mcLoc("item/generated"), "layer0", modLoc("item/nation_deed_100"));

		// block items
		withExistingParent(ModItems.BORDER_STONE.getId().getPath(), modLoc("block/border_stone"));

		// tools
//		singleTexture("zoning_tool",
//				mcLoc("item/handheld"), "layer0", modLoc("item/zoning_tool"));
		withExistingParent(ModItems.ZONING_PLACEMENT_TOOL.getId().getPath(), modLoc("block/zone_placement"));
		withExistingParent(ModItems.CITIZEN_PLACEMENT_TOOL.getId().getPath(), modLoc("block/citizen_placement"));

	}
}
