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
package mod.gottsch.forge.claimmyland.core.block;

import mod.gottsch.forge.claimmyland.core.setup.Registration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

/**
 * Created by Mark Gottschling on Sep 14, 2024
 */
public class ModBlocks {
    public static final RegistryObject<Block> PERSONAL_BORDER_STONE = Registration.BLOCKS.register("personal_border_stone",
            () -> new BorderStone(BlockBehaviour.Properties.copy(Blocks.STONE)));

    public static final RegistryObject<Block> NATION_BORDER_STONE = Registration.BLOCKS.register("nation_border_stone",
            () -> new BorderStone(BlockBehaviour.Properties.copy(Blocks.STONE)));

//    public static final RegistryObject<Block> FOUNDATION_STONE = Registration.BLOCKS.register("foundation_stone",
//            () -> new FoundationStone(BlockBehaviour.Properties.copy(Blocks.STONE)));

    public static final RegistryObject<Block> PERSONAL_FOUNDATION_STONE = Registration.BLOCKS.register("personal_foundation_stone",
            () -> new PersonalFoundationStone(BlockBehaviour.Properties.copy(Blocks.STONE)));

    public static final RegistryObject<Block> PERSONAL_BORDER = Registration.BLOCKS.register("personal_border",
            () -> new BorderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().randomTicks().noCollission().instabreak().pushReaction(PushReaction.IGNORE)));

    public static final RegistryObject<Block> NATION_BORDER = Registration.BLOCKS.register("nation_border",
            () -> new NationBorderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().randomTicks().noCollission().instabreak().pushReaction(PushReaction.IGNORE)));

    public static final RegistryObject<Block> CITIZEN_BORDER = Registration.BLOCKS.register("citizen_border",
            () -> new BorderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().randomTicks().noCollission().instabreak().pushReaction(PushReaction.IGNORE)));

    public static final RegistryObject<Block> BUFFER = Registration.BLOCKS.register("buffer",
            () -> new BufferBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().randomTicks().noCollission().instabreak().pushReaction(PushReaction.IGNORE)));

    public static void register(IEventBus bus) {
        // cycle through all block and create items
        Registration.registerBlocks(bus);
    }
}
