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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.BorderBlock;
import mod.gottsch.forge.claimmyland.core.block.BorderStatus;
import mod.gottsch.forge.claimmyland.core.block.BufferBlock;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.parcel.CitizenZoneParcel;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * Create by Mark Gottschling on Sep 20, 2204
 */
public class NationFoundationStoneBlockEntity extends FoundationStoneBlockEntity {

    /**
     *
     * @param pos
     * @param state
     */
    public NationFoundationStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NATION_FOUNDATION_STONE_ENTITY_TYPE.get(), pos, state);
    }

    @Override
    public int getBufferSize() {
      return Config.SERVER.general.nationParcelBufferRadius.get();
    }

    @Override
    public Block getBorderBlock() {
        return ModBlocks.NATION_BORDER.get();
    }

    // TODO this should return Optional<BlockState>
    @Override
    protected BlockState getBorderBlockState(Box box) {
        // get the default block state of the border block
        BlockState blockState = ModBlocks.NATION_BORDER.get().defaultBlockState();

        /*
         * check if parcel is within another existing parcel
         * NOTE this should not happen as it is not allowed by the rules
         */
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(box.getMinCoords());

        if (registryParcel.isEmpty()) {
            // find overlaps of the parcel with buffered registry parcels.
            // this ensure that the parcel boundaries are not overlapping the buffer area of another parcel
            List<Parcel> overlaps = ParcelRegistry.findBuffer(box);
            if (!overlaps.isEmpty()) {
                for (Parcel overlapParcel : overlaps) {
                    /*
                     * if parcel of foundation stone has same owner as parcel in world, ignore buffers,
                     * but check border overlaps. parcels owned by the same player can be touching.
                     */
                    if (getOwnerId().equals(overlapParcel.getOwnerId())) {
                        // get the existing owned parcel
                        Optional<Parcel> optionalOwnedParcel = ParcelRegistry.findByParcelId(overlapParcel.getId());

                        // test if the non-buffered parcels intersect
                        if (optionalOwnedParcel.isPresent() && ModUtil.intersects(box, optionalOwnedParcel.get().getBox())) {
                            blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                            break;
                        }
                    } else {
                        blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                        break;
                    }
                }
            }
        } else {
            // NOTE this should not happen. nations cannot be embedded in another parcel.
            ClaimMyLand.LOGGER.error("unable to display nation border for -> {} as it is within another parcel -> {}", getParcelId(), registryParcel.get().getId());
        }
        return blockState;
    }

    // TODO should return Optional<BlockState>
    @Override
    // determines what state the buffer block is
    protected BlockState getBufferBlockState(Box box, Box bufferedBox) {
        // get the default block state of the border block
        BlockState blockState = ModBlocks.BUFFER.get().defaultBlockState();

        /*
         * check if box/parcel is within another existing parcel
         */
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(box.getMinCoords());

        // not within another parcel
        if (registryParcel.isEmpty()) {

            // find overlaps of the buffered box with unbuffered parcels
            // this ensure that the buffered boundaries are not overlapping the area of another parcel - too close!
            // filter out the parcel that the buffer belongs to
            List<Parcel> overlaps = ParcelRegistry.find(bufferedBox).stream()
                    .filter(p -> !p.getId().equals(getParcelId())).toList();

            if (!overlaps.isEmpty()) {
                for (Parcel overlapParcel : overlaps) {
                    // the parcels are owned by the same person. they can be closer or touching,
                    // ie. ignore buffers, only the parcels themselves can't overlap
                    if (!getOwnerId().equals(overlapParcel.getOwnerId())) {
                        blockState = blockState.setValue(BufferBlock.INTERSECTS, BorderStatus.BAD);
                        break;
                    }
                }
            }
        } else {
            // NOTE this should not happen. nations cannot be embedded in another parcel.
            ClaimMyLand.LOGGER.error("unable to display nation border for -> {} as it is within another parcel -> {}", getParcelId(), registryParcel.get().getId());
        }
        return blockState;
    }

    /**
     * since nation blocks encompass the entire y-range, limit the drawn border
     * to only 20 blocks ie 10 down / 10 up.
     * @param coords
     * @return
     */
    @Override
    public Box getBorderDisplayBox(ICoords coords) {
        return new Box(coords.add(getRelativeBox().getMinCoords().withY(-10)),
                coords.add(getRelativeBox().getMaxCoords().withY(9))); // 10-1
    }

}
