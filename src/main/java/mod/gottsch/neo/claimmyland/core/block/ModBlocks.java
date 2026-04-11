/*
 * This file is part of  Claim My Land.
 * ofFullCopyright (c) 2024 Mark Gottschling (gottsch)
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
 * You should have received a ofFullCopy of the GNU Lesser General Public License
 * along with Claim My Land.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.neo.claimmyland.core.block;

import mod.gottsch.neo.claimmyland.core.setup.Registration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * @author Mark Gottschling on Sep 14, 2024
 */
public class ModBlocks {
    public static final DeferredHolder<Block, BorderStone> BORDER_STONE = Registration.BLOCKS.register("border_stone",
            () -> new BorderStone(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));

    public static final DeferredHolder<Block, PlayerFoundationStone> PLAYER_FOUNDATION_STONE = Registration.BLOCKS.register("player_foundation_stone",
            () -> new PlayerFoundationStone(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));

    public static final DeferredHolder<Block, NationFoundationStone> NATION_FOUNDATION_STONE = Registration.BLOCKS.register("nation_foundation_stone",
            () -> new NationFoundationStone(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));

    public static final DeferredHolder<Block, CitizenFoundationStone> CITIZEN_FOUNDATION_STONE = Registration.BLOCKS.register("citizen_foundation_stone",
            () -> new CitizenFoundationStone(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));

    public static final DeferredHolder<Block, CitizenPlacementBlock> CITIZEN_PLACEMENT_BLOCK = Registration.BLOCKS.register("citizen_placement",
            () -> new CitizenPlacementBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission().instabreak().pushReaction(PushReaction.IGNORE)));

    public static final DeferredHolder<Block, ZonePlacementBlock> ZONE_PLACEMENT_BLOCK = Registration.BLOCKS.register("zone_placement",
            () -> new ZonePlacementBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak().pushReaction(PushReaction.IGNORE)));

    public static void register(IEventBus bus) {
        // cycle through all block and create items
        Registration.registerBlocks(bus);
    }
}
