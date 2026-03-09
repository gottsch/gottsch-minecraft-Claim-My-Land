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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.ZonePlacementBlockEntity;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * @author Mark Gottschling on Oct 15, 2024
 */
public class ZonePlacementBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11D, 16D, 11D);

    public ZonePlacementBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        ZonePlacementBlockEntity blockEntity = null;
        try {
            blockEntity = new ZonePlacementBlockEntity(pos, state);
        }
        catch(Exception e) {
            ClaimMyLand.LOGGER.error("error", e);
        }

        return blockEntity;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
                                       FluidState fluid) {

        if (WorldInfo.isClientSide(level)) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ZonePlacementBlockEntity zbe) {
            if (zbe.getCoords1() != null && zbe.getCoords1() != Coords.EMPTY
                && zbe.getCoords2() != null && zbe.getCoords2() != Coords.EMPTY) {
                ICoords coords1 = zbe.getCoords1();
                ICoords coords2 = zbe.getCoords2();

                ZonePlacementBlockEntity.removeParcelBorder(level, new Box(coords1, coords2), ModBlocks.ZONE_BORDER.get());

                // clear blocks at 1 & 2
                if (!coords1.toPos().equals(pos) && level.getBlockState(coords1.toPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
                    level.setBlock(coords1.toPos(), Blocks.AIR.defaultBlockState(), 3);
                }
                if (!coords2.toPos().equals(pos) && level.getBlockState(coords2.toPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
                    level.setBlock(coords2.toPos(), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
}
