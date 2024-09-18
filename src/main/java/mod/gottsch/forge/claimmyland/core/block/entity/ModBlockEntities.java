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
package mod.gottsch.forge.claimmyland.core.block.entity;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.setup.Registration;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

/**
 * Created by Mark Gottschling on Sep 14, 2024
 */
public class ModBlockEntities {

//    public static final RegistryObject<BlockEntityType<FoundationStoneBlockEntity>> FOUNDATION_STONE_ENTITY_TYPE =
//            Registration.BLOCK_ENTITIES.register("foundation_stone_block_entity",
//                    () -> BlockEntityType.Builder.of(FoundationStoneBlockEntity::new, ModBlocks.FOUNDATION_STONE.get()).build(null));

    public static final RegistryObject<BlockEntityType<PersonalFoundationStoneBlockEntity>> PERSONAL_FOUNDATION_STONE_ENTITY_TYPE =
            Registration.BLOCK_ENTITIES.register("personal_foundation_stone_block_entity",
                    () -> BlockEntityType.Builder.of(PersonalFoundationStoneBlockEntity::new, ModBlocks.PERSONAL_FOUNDATION_STONE.get()).build(null));

    public static void register(IEventBus bus) {
        Registration.registerBlockEntities(bus);
    }
}
