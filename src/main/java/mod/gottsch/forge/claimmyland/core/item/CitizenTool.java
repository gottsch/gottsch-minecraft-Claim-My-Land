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
package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.BorderStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.block.entity.CitizenPlacementBlockEntity;
import mod.gottsch.forge.claimmyland.core.block.entity.ZonePlacementBlockEntity;
import mod.gottsch.forge.claimmyland.core.command.CommandHelper;
import mod.gottsch.forge.claimmyland.core.parcel.ClaimResult;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Created by Mark Gottschling on Oct 7, 2024
 */
public class CitizenTool extends BlockItem {
    private static final String COORDS1 = "coords1";
    private static final String COORDS2 = "coords2";

    public CitizenTool(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     *
     * @param context
     * @return
     */
    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {

        if (context.getLevel().isClientSide()) {
            return InteractionResult.FAIL;
        }

        if (context.getLevel().dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD) {
            return InteractionResult.FAIL;
        }

        Optional<Parcel> parentParcel = ParcelRegistry.findLeastSignificant(Coords.of(context.getClickedPos()));
        if (parentParcel.isEmpty()
                || (parentParcel.get().getType() != ParcelType.NATION
                && parentParcel.get().getType() != ParcelType.ZONE)) {
            context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("citizen_placement.not_valid_parent")).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        // NOTE don't have to check if nation ID matches or
        // player is the owner as that is already checked by events.
        // ie. can't use the Zoning Tool if don't have access.

        // get the tag
        CompoundTag tag = context.getItemInHand().getOrCreateTag();

        // convert UseOnContext to BlockPlaceContext
        BlockPlaceContext placeContext = new BlockPlaceContext(context);

        // if using the tool on citizen block
        if (context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.CITIZEN_PLACEMENT_BLOCK.get())) {
            // test if there are two blocks on record
            if (!tag.contains(COORDS1) || !tag.contains(COORDS2)) {
                context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("citizen_placement.begin_end_required")).withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }

            // create zone parcel using the Box defined by the 2 zoning blocks coords
            ICoords coords1 = Coords.EMPTY.load(tag.getCompound(COORDS1));
            ICoords coords2 = Coords.EMPTY.load(tag.getCompound(COORDS2));

            Box box = new Box(coords1, coords2);
            if (box.getSize().getX() < 2
                    || box.getSize().getY() < 2
                    || box.getSize().getZ() < 2) {
                context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("parcel.add.failure_too_small")).withStyle(ChatFormatting.RED));
            }

            ParcelFactory.create(ParcelType.CITIZEN, parentParcel.get().getNationId())
                    .ifPresentOrElse(p -> {
                                p.setOwnerId(parentParcel.get().getOwnerId());
                                // ensure to have to use the min coords of the box
                                p.setCoords(box.getMinCoords());
                                // build a relative 0-based size of the box.
                                // NOTE since it is a 0-based new box, use box.getSize() instead of ModUtil.getSize(),
                                // because we are introducing a bigger size by starting at 0 0 0.
                                p.setSize(new Box(Coords.of(0, 0, 0), box.getSize()));

                                ClaimResult claimResult = p.handleEmbeddedClaim(context.getLevel(), parentParcel.get(), p.getBox());
                                if (claimResult == ClaimResult.SUCCESS) {
                                    context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("parcel.add.success")).withStyle(ChatFormatting.GREEN));
                                    CommandHelper.save(context.getLevel());
                                } else {
                                    // TODO examine the claim result to determine the correct message.
                                    // handleError(context.getLevel(), context.getPlayer(), successfulClaim);
                                    context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("parcel.add.failure_with_overlaps")).withStyle(ChatFormatting.RED));
                                }
                            },
                            () -> {
                                context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
                            });

            // clear the zone placement blocks and border
            clear(placeContext, coords1, coords2);
            // remove the coords tags
            tag.remove(COORDS1);
            tag.remove(COORDS2);

            return InteractionResult.SUCCESS;
        } else {
            /*
             * placing a zone placement block
             */
            context.getLevel().setBlock(placeContext.getClickedPos(), ModBlocks.CITIZEN_PLACEMENT_BLOCK.get().defaultBlockState(), 3);

            if (tag.contains(COORDS1) && context.getLevel().getBlockEntity(Coords.EMPTY.load(tag.getCompound(COORDS1)).toPos()) instanceof CitizenPlacementBlockEntity) {
                if (tag.contains(COORDS2) && context.getLevel().getBlockEntity(Coords.EMPTY.load(tag.getCompound(COORDS2)).toPos()) instanceof CitizenPlacementBlockEntity) {
                    // clear zone placement blocks and borders
                    clear(placeContext, tag);

                    // remove the coords2 tag
                    tag.remove(COORDS2);

                    // update coords1
                    tag.put(COORDS1, Coords.of(placeContext.getClickedPos()).save(new CompoundTag()));
                } else {
                    // placing block2
                    ICoords coords1 = Coords.EMPTY.load(tag.getCompound(COORDS1));
                    ICoords coords2 = Coords.of(placeContext.getClickedPos());
                    // update coords2
                    tag.put(COORDS2, coords2.save(new CompoundTag()));
                    // place borders

                    CitizenPlacementBlockEntity blockEntity = (CitizenPlacementBlockEntity) context.getLevel().getBlockEntity(placeContext.getClickedPos());
                    blockEntity.setCoords1(coords1);
                    blockEntity.setCoords2(coords2);
                    blockEntity.setOwnerId(context.getPlayer().getUUID());

                    // update the first block with both coords as well
                    CitizenPlacementBlockEntity blockEntity1 = (CitizenPlacementBlockEntity) context.getLevel().getBlockEntity(coords1.toPos());
                    blockEntity1.setCoords1(coords1);
                    blockEntity1.setCoords2(coords2);
                    blockEntity1.setOwnerId(context.getPlayer().getUUID());

                    // display the border
                    blockEntity.placeParcelBorder();                }
            } else {
                tag.put(COORDS1, Coords.of(placeContext.getClickedPos()).save(new CompoundTag()));
            }
            return InteractionResult.SUCCESS;
        }
    }

    private void clear(BlockPlaceContext context, CompoundTag tag) {
        // load the coords from the tag
        ICoords coords1 = Coords.EMPTY.load(tag.getCompound(COORDS1));
        ICoords coords2 = Coords.EMPTY.load(tag.getCompound(COORDS2));
        clear(context, coords1, coords2);
    }

    private void clear(BlockPlaceContext context, ICoords coords1, ICoords coords2) {

        // clear blocks at 1 & 2
        if (context.getLevel().getBlockState(coords1.toPos()).is(ModBlocks.CITIZEN_PLACEMENT_BLOCK.get())) {
            context.getLevel().setBlock(coords1.toPos(), Blocks.AIR.defaultBlockState(), 3);
        }
        if (context.getLevel().getBlockState(coords2.toPos()).is(ModBlocks.CITIZEN_PLACEMENT_BLOCK.get())) {
            context.getLevel().setBlock(coords2.toPos(), Blocks.AIR.defaultBlockState(), 3);
        }

        // clear the borders
        CitizenPlacementBlockEntity.removeParcelBorder(context.getLevel(), new Box(coords1, coords2), ModBlocks.CITIZEN_BORDER.get(), 0);
    }
}
