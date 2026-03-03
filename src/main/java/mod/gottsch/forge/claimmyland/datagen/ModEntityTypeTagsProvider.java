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
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.tags.ModTags;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.treasure2.core.block.TreasureBlocks;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * @author Mark Gottschling on Feb 7, 2026
 */
public class ModEntityTypeTagsProvider extends EntityTypeTagsProvider {

    public ModEntityTypeTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider,
                                     ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ClaimMyLand.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(Provider provider) {

        // farm entities
        tag(ModTags.Entities.FARM_ENTITIES)
                .add(EntityType.CHICKEN)
                .add(EntityType.CAMEL)
                .add(EntityType.COW)
                .add(EntityType.DONKEY)
                .add(EntityType.GOAT)
                .add(EntityType.HORSE)
                .add(EntityType.PIG)
                .add(EntityType.SHEEP);

        // neutral entities
        tag(ModTags.Entities.NEUTRAL_ENTITIES)
                .add(EntityType.AXOLOTL)
                .add(EntityType.BAT)
                .add(EntityType.BEE)
                .add(EntityType.CAT)
                .add(EntityType.COD)
                .add(EntityType.DOLPHIN)
                .add(EntityType.FOX)
                .add(EntityType.FROG)
                .add(EntityType.GLOW_SQUID)
                .add(EntityType.IRON_GOLEM)
                .add(EntityType.LLAMA)
                .add(EntityType.MULE)
                .add(EntityType.MOOSHROOM)
                .add(EntityType.OCELOT)
                .add(EntityType.PANDA)
                .add(EntityType.PARROT)
                .add(EntityType.POLAR_BEAR)
                .add(EntityType.PUFFERFISH)
                .add(EntityType.RABBIT)
                .add(EntityType.SALMON)
                .add(EntityType.SQUID)
                .add(EntityType.TROPICAL_FISH)
                .add(EntityType.WOLF);

        tag(ModTags.Entities.ENTITY_SPAWN_WHITELIST)
                .addTag(ModTags.Entities.FARM_ENTITIES)
                .addTag(ModTags.Entities.NEUTRAL_ENTITIES)
                .add(EntityType.BOAT)
                .add(EntityType.CHEST_BOAT)
                .add(EntityType.CHEST_MINECART)
                .add(EntityType.COMMAND_BLOCK_MINECART)
                .add(EntityType.END_CRYSTAL)
                .add(EntityType.ENDER_PEARL)
                .add(EntityType.EXPERIENCE_BOTTLE)
                .add(EntityType.EXPERIENCE_ORB)
                .add(EntityType.EYE_OF_ENDER)
                .add(EntityType.FALLING_BLOCK)
                .add(EntityType.FIREBALL)
                .add(EntityType.FIREWORK_ROCKET)
                .add(EntityType.FISHING_BOBBER)
                .add(EntityType.FURNACE_MINECART)
                .add(EntityType.GLOW_ITEM_FRAME)
                .add(EntityType.ITEM_DISPLAY)
                .add(EntityType.ITEM_FRAME)
                .add(EntityType.ITEM)
                .add(EntityType.LLAMA_SPIT)
                .add(EntityType.LEASH_KNOT)
                .add(EntityType.MINECART)
                .add(EntityType.PAINTING)
                .add(EntityType.PLAYER)
                .add(EntityType.POTION)
                .add(EntityType.SPAWNER_MINECART)
                .add(EntityType.SPECTRAL_ARROW);
    }

}
