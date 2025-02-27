/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.claimmyland.core.util;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * @author Mark Gottschling Feb 24, 2025
 */
public class TagHelper {

    public static Optional<TagKey<Block>> getBlockTagFromResourceLocation(ResourceLocation resourceLocation) {
        try {
            return Optional.of(TagKey.create(Registries.BLOCK, resourceLocation));
        } catch (IllegalArgumentException e) {
            System.err.println("invalid ResourceLocation for Block Tag: " + resourceLocation);
            return Optional.empty();
        }
    }

    public static boolean doesBlockBelongToTag(Block block, ResourceLocation tagLocation) {
        Optional<TagKey<Block>> tagKeyOptional = getBlockTagFromResourceLocation(tagLocation);
        if (tagKeyOptional.isPresent()) {
            TagKey<Block> tagKey = tagKeyOptional.get();
            ClaimMyLand.LOGGER.debug("testing block -> {} .is()", block.getName().getString());
            return block.builtInRegistryHolder().is(tagKey);
        }
        ClaimMyLand.LOGGER.debug("could not find block tag in registry.");
        return false;
    }

    public static boolean doesBlockBelongToTag(Block block, TagKey<Block> tagKey){
        return block.builtInRegistryHolder().is(tagKey);
    }

    public static Optional<TagKey<Item>> getItemTagFromResourceLocation(ResourceLocation location) {
        try {
            return Optional.of(TagKey.create(Registries.ITEM, location));
        } catch (IllegalArgumentException e) {
            System.err.println("invalid ResourceLocation for Item Tag: " + location);
            return Optional.empty();
        }
    }

    public static boolean doesItemBelongToTag(Item item, ResourceLocation tagResourceLocation) {
        Optional<TagKey<Item>> tagKeyOptional = getItemTagFromResourceLocation(tagResourceLocation);
        if (tagKeyOptional.isPresent()) {
            TagKey<Item> tagKey = tagKeyOptional.get();
            return item.builtInRegistryHolder().is(tagKey);
        }
        return false;
    }

    public static boolean doesItemBelongToTag(Item item, TagKey<Item> tagKey){
        return item.builtInRegistryHolder().is(tagKey);
    }
}
