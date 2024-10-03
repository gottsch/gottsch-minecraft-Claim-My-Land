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
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.parcel.ZoneParcel;
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
 * Create by Mark Gottschling on Sep 23, 2204
 */
public class CitizenFoundationStoneBlockEntity extends FoundationStoneBlockEntity {

    /**
     *
     * @param pos
     * @param state
     */
    public CitizenFoundationStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CITIZEN_FOUNDATION_STONE_ENTITY_TYPE.get(), pos, state);
    }

    @Override
    public int getBufferSize(ParcelType type) {
        return Config.SERVER.general.parcelBufferRadius.get();
    }

    @Override
    public Block getBorderBlock() {
        return ModBlocks.CITIZEN_BORDER.get();
    }

    // TODO this probably needs to be more like PlayerBE
    // TODO this should return Optional<BlockState>
    @Override
    protected BlockState getBorderBlockState(Box box) {
        // get the default block state of the border block
        BlockState blockState = ModBlocks.CITIZEN_BORDER.get().defaultBlockState();

        /*
         * check if parcel is within another existing parcel
         * NOTE this should not happen as it is not allowed by the rules
         */
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(box.getMinCoords());

        if (registryParcel.isEmpty()) {
            // find overlaps of the parcel with buffered registry parcels.
            // this ensure that the parcel boundaries are not overlapping the buffer area of another parcel.
            // filter out parcels without owners
            List<Parcel> overlaps = ParcelRegistry.findBuffer(box);

            if (!overlaps.isEmpty()) {
                boolean hasIntersections = Parcel.hasBoxToBufferedIntersections(box, getOwnerId(), overlaps);
                if (hasIntersections) {
                    blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                }
//                for (Parcel overlapParcel : overlaps) {
//                    /*
//                     * if parcel of foundation stone has same owner as parcel in world, ignore buffers,
//                     * but check border overlaps. parcels owned by the same player can be touching.
//                     */
//                    if (getOwnerId().equals(overlapParcel.getOwnerId())) {
//                        // get the existing owned parcel
//                        Optional<Parcel> optionalOwnedParcel = ParcelRegistry.findByParcelId(overlapParcel.getId());
//
//                        // test if the non-buffered parcels intersect
//                        if (optionalOwnedParcel.isPresent() && ModUtil.touching(box, optionalOwnedParcel.get().getBox())) {
//                            blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
//                            break;
//                        }
//                    } else {
//                        blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
//                        break;
//                    }
//                }
            }
        } else {
            // determine what parcel type is the foundation stone in
            if (registryParcel.get().getType() == ParcelType.CITIZEN) {
                // do nothing - uses good state
                int i = 0;
            } else if (registryParcel.get().getType() == ParcelType.NATION) {
                // NOTE this shouldn't be allowed in the first place due to placement rules.
                blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
            } else if (registryParcel.get().getType() == ParcelType.ZONE) {
                // if any part of the parcel is outside the claim zone, then bad
                if (!ModUtil.contains(registryParcel.get().getBox(), box)) {
                    blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                } else {
                    // proceed with a normal overlaps check, filtering out nation parcels
                    List<Parcel> overlaps = ParcelRegistry.findBuffer(box);
                    overlaps = overlaps.stream()
                            .filter(p -> !(p instanceof NationParcel || p instanceof ZoneParcel))
                            .toList();

                    if (Parcel.hasBoxToBufferedIntersections(box, getOwnerId(), overlaps)) {
                        blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                    }
                }
            }
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
            // determine what parcel type is the foundation stone in
            if (registryParcel.get().getType() == ParcelType.CITIZEN) {
                // do nothing - uses good state
            } else if (registryParcel.get().getType() == ParcelType.NATION) {
                // NOTE this shouldn't be allowed in the first place due to placement rules.
                blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
            } else if (registryParcel.get().getType() == ParcelType.ZONE) {

                // TODO replace with Parcel.hasBoxToBufferedIntersections()...
                // preform a normal overlaps check, filtering out nation and claim zone parcels
                List<Parcel> overlaps = ParcelRegistry.find(bufferedBox).stream()
                        .filter(p -> !p.getId().equals(getParcelId()))
                        .filter(p -> !(p instanceof NationParcel || p instanceof ZoneParcel))
                        .toList();

                if(Parcel.hasBoxToBufferedIntersections(box, getOwnerId(), overlaps)) {
                    blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                }
            }
         }
        return blockState;
    }
}
