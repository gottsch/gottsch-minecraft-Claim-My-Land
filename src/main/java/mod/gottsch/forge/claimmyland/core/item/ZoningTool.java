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
import mod.gottsch.forge.claimmyland.core.block.entity.ZonePlacementBlockEntity;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.parcel.*;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Created by Mark Gottschling on Oct 7, 2024
 */
public class ZoningTool extends BlockItem {
    private static final String COORDS1 = "coords1";
    private static final String COORDS2 = "coords2";

    public ZoningTool(Block block, Properties properties) {
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

        Optional<Parcel> nationParcel = ParcelRegistry.findLeastSignificant(Coords.of(context.getClickedPos()));
        if (nationParcel.isEmpty() || !nationParcel.get().isNation()) {
            sendError(context.getPlayer(), "zone_placement.not_nation");
            return InteractionResult.FAIL;
        }

        CompoundTag tag = context.getItemInHand().getOrCreateTag();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);

        if (isClickingZonePlacementBlock(context)) {
            return handleZoneCreation(context, placeContext, tag, nationParcel.get());
        } else {
            return handleZonePlacement(context, placeContext, tag);
        }
    }

    // -------------------------------------------------------------------------
    // Zone Creation (clicking an existing ZonePlacementBlock to finalize)
    // -------------------------------------------------------------------------

    private InteractionResult handleZoneCreation(UseOnContext context, BlockPlaceContext placeContext,
                                                 CompoundTag tag, Parcel nationParcel) {
        if (!tag.contains(COORDS1) || !tag.contains(COORDS2)) {
            sendError(context.getPlayer(), "zone_placement.begin_end_required");
            return InteractionResult.SUCCESS;
        }

        ICoords coords1 = loadCoords(tag, COORDS1);
        ICoords coords2 = loadCoords(tag, COORDS2);
        Box box = new Box(coords1, coords2);

        if (isBoxTooSmall(box)) {
            sendError(context.getPlayer(), "parcel.add.failure_too_small");
            return InteractionResult.SUCCESS;
        }

        InteractionResult result = tryCreateZoneParcel(context, placeContext, nationParcel, box, coords1, coords2);

        clear(placeContext, coords1, coords2);
        tag.remove(COORDS1);
        tag.remove(COORDS2);

        return result;
    }

    private InteractionResult tryCreateZoneParcel(UseOnContext context, BlockPlaceContext placeContext,
                                                  Parcel nationParcel, Box box,
                                                  ICoords coords1, ICoords coords2) {
        Optional<Parcel> created = ParcelTypeRegistry.create(ParcelType.ZONE, (NationParcel) nationParcel);
        if (created.isEmpty()) {
            sendError(context.getPlayer(), "unexpected_error");
            return InteractionResult.SUCCESS;
        }

        Parcel zone = created.get();
        zone.setOwnerId(nationParcel.getOwnerId());
        zone.setCoords(box.getMinCoords());
        zone.setSize(new Box(Coords.of(0, 0, 0), box.getSize()));

        ClaimResult claimResult = zone.handleEmbeddedClaim(context.getLevel(), nationParcel, zone.getBox());
        if (claimResult == ClaimResult.SUCCESS) {
            sendSuccess(context.getPlayer(), "parcel.add.success");
            CommandHelper.save(context.getLevel());
        } else {
            sendError(context.getPlayer(), "parcel.add.failure_with_overlaps");
        }

        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Zone Placement (placing ZonePlacementBlocks to define corners)
    // -------------------------------------------------------------------------

    private InteractionResult handleZonePlacement(UseOnContext context, BlockPlaceContext placeContext,
                                                  CompoundTag tag) {
        Level level = context.getLevel();
        ICoords clickedCoords = Coords.of(placeContext.getClickedPos());

        level.setBlock(placeContext.getClickedPos(), ModBlocks.ZONE_PLACEMENT_BLOCK.get().defaultBlockState(), 3);

        boolean hasValidCoords1 = tag.contains(COORDS1)
                && level.getBlockEntity(loadCoords(tag, COORDS1).toPos()) instanceof ZonePlacementBlockEntity;

        if (!hasValidCoords1) {
            // First corner: just record coords1
            tag.put(COORDS1, clickedCoords.save(new CompoundTag()));
            return InteractionResult.SUCCESS;
        }

        boolean hasValidCoords2 = tag.contains(COORDS2)
                && level.getBlockEntity(loadCoords(tag, COORDS2).toPos()) instanceof ZonePlacementBlockEntity;

        if (hasValidCoords2) {
            // both corners already set: reset and start fresh from clicked position
            clear(placeContext, tag);
            tag.remove(COORDS2);
            tag.put(COORDS1, clickedCoords.save(new CompoundTag()));
            return InteractionResult.SUCCESS;
        }

        // Second corner: validate and set coords2
        return placeSecondCorner(context, placeContext, tag, clickedCoords);
    }

    private InteractionResult placeSecondCorner(UseOnContext context, BlockPlaceContext placeContext,
                                                CompoundTag tag, ICoords coords2) {
        ICoords coords1 = loadCoords(tag, COORDS1);

        Optional<Parcel> parentAtCoords1 = ParcelRegistry.findLeastSignificant(coords1);
        Optional<Parcel> parentAtCoords2 = ParcelRegistry.findLeastSignificant(coords2);

        if (parentAtCoords1.isEmpty() || parentAtCoords2.isEmpty()) {
            sendError(context.getPlayer(), "unexpected_error");
            return InteractionResult.SUCCESS;
        }

        if (!parentAtCoords1.get().getId().equals(parentAtCoords2.get().getId())) {
            sendError(context.getPlayer(), "zone_placement.not_same_nation");
            return InteractionResult.SUCCESS;
        }

        tag.put(COORDS2, coords2.save(new CompoundTag()));

        linkZonePlacementBlocks(context.getLevel(), placeContext, coords1, coords2, context.getPlayer());

        return InteractionResult.SUCCESS;
    }

    private void linkZonePlacementBlocks(Level level, BlockPlaceContext placeContext,
                                         ICoords coords1, ICoords coords2, Player player) {
        ZonePlacementBlockEntity blockEntity2 = (ZonePlacementBlockEntity) level.getBlockEntity(placeContext.getClickedPos());
        blockEntity2.setCoords1(coords1);
        blockEntity2.setCoords2(coords2);
        blockEntity2.setOwnerId(player.getUUID());

        ZonePlacementBlockEntity blockEntity1 = (ZonePlacementBlockEntity) level.getBlockEntity(coords1.toPos());
        blockEntity1.setCoords1(coords1);
        blockEntity1.setCoords2(coords2);
        blockEntity1.setOwnerId(player.getUUID());

        blockEntity2.placeParcelBorder();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isClickingZonePlacementBlock(UseOnContext context) {
        return context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get());
    }

    private boolean isBoxTooSmall(Box box) {
        return box.getSize().getX() < 2
                || box.getSize().getY() < 2
                || box.getSize().getZ() < 2;
    }

    private ICoords loadCoords(CompoundTag tag, String key) {
        return Coords.EMPTY.load(tag.getCompound(key));
    }

    private void sendError(Player player, String langKey) {
        player.sendSystemMessage(Component.translatable(LangUtil.chat(langKey)).withStyle(ChatFormatting.RED));
    }

    private void sendSuccess(Player player, String langKey) {
        player.sendSystemMessage(Component.translatable(LangUtil.chat(langKey)).withStyle(ChatFormatting.GREEN));
    }

    private void clear(BlockPlaceContext context, CompoundTag tag) {
        clear(context, loadCoords(tag, COORDS1), loadCoords(tag, COORDS2));
    }

    private void clear(BlockPlaceContext context, ICoords coords1, ICoords coords2) {
        clearBlockIfZonePlacement(context.getLevel(), coords1);
        clearBlockIfZonePlacement(context.getLevel(), coords2);
        ZonePlacementBlockEntity.removeParcelBorder(context.getLevel(), new Box(coords1, coords2), ModBlocks.ZONE_BORDER.get(), 0);
    }

    private void clearBlockIfZonePlacement(Level level, ICoords coords) {
        if (level.getBlockState(coords.toPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
            level.setBlock(coords.toPos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

//        Optional<Parcel> nationParcel = ParcelRegistry.findLeastSignificant(Coords.of(context.getClickedPos()));
//        if (nationParcel.isEmpty() || !nationParcel.get().isNation()) {
//            context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("zone_placement.not_nation")).withStyle(ChatFormatting.RED));
//            return InteractionResult.FAIL;
//        }
//
//        // NOTE don't have to check if nation ID matches or
//        // player is the owner as that is already checked by events.
//        // ie. can't use the Zoning Tool if don't have access.
//
//        // get the tag
//        CompoundTag tag = context.getItemInHand().getOrCreateTag();
//
//        // convert UseOnContext to BlockPlaceContext
//        BlockPlaceContext placeContext = new BlockPlaceContext(context);
//
//        // if using the tool on zoning block
//        if (context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
//            // test if there are two blocks on record
//            if (!tag.contains(COORDS1) || !tag.contains(COORDS2)) {
//                context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("zone_placement.begin_end_required")).withStyle(ChatFormatting.RED));
//                return InteractionResult.SUCCESS;
//            }
//
//            // create zone parcel using the Box defined by the 2 zoning blocks coords
//            ICoords coords1 = Coords.EMPTY.load(tag.getCompound(COORDS1));
//            ICoords coords2 = Coords.EMPTY.load(tag.getCompound(COORDS2));
//
//            Box box = new Box(coords1, coords2);
//            if (box.getSize().getX() < 2
//                    || box.getSize().getY() < 2
//                    || box.getSize().getZ() < 2) {
//                context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("parcel.add.failure_too_small")).withStyle(ChatFormatting.RED));
//                return InteractionResult.SUCCESS;
//            }
//
//            ParcelTypeRegistry.create(ParcelType.ZONE, (NationParcel) nationParcel.get())
//                    .ifPresentOrElse(p -> {
//                            p.setOwnerId(nationParcel.get().getOwnerId());
//                            // ensure to have to use the min coords of the box
//                            p.setCoords(box.getMinCoords());
//
//                            // build a relative 0-based size of the box.
//                            // NOTE since it is a 0-based new box, use box.getSize() instead of ModUtil.getSize(),
//                            // because we are introducing a bigger size by starting at 0 0 0.
//                            p.setSize(new Box(Coords.of(0, 0, 0), box.getSize()));
//
//                            ClaimResult claimResult = p.handleEmbeddedClaim(context.getLevel(), nationParcel.get(), p.getBox());
//                            if (claimResult == ClaimResult.SUCCESS) {
//                                context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("parcel.add.success")).withStyle(ChatFormatting.GREEN));
//                                CommandHelper.save(context.getLevel());
//                            } else {
//                                // TODO examine the claim result to determine the correct message.
//                                // handleError(context.getLevel(), context.getPlayer(), successfulClaim);
//                                context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("parcel.add.failure_with_overlaps")).withStyle(ChatFormatting.RED));
//                            }
//                        },
//                    () -> {
//                        context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
//                    });
//
//            // clear the zone placement blocks and border
//            clear(placeContext, coords1, coords2);
//            // remove the coords tags
//            tag.remove(COORDS1);
//            tag.remove(COORDS2);
//
//            return InteractionResult.SUCCESS;
//        } else {
//            // TODO cannot place if not within the same containing parcel
//            /*
//             * placing a zone placement block
//             */
//            context.getLevel().setBlock(placeContext.getClickedPos(), ModBlocks.ZONE_PLACEMENT_BLOCK.get().defaultBlockState(), 3);
//
//            if (tag.contains(COORDS1) && context.getLevel().getBlockEntity(Coords.EMPTY.load(tag.getCompound(COORDS1)).toPos()) instanceof ZonePlacementBlockEntity) {
//                if (tag.contains(COORDS2) && context.getLevel().getBlockEntity(Coords.EMPTY.load(tag.getCompound(COORDS2)).toPos()) instanceof ZonePlacementBlockEntity) {
//                    // clear zone placement blocks and borders
//                    clear(placeContext, tag);
//
//                    // remove the coords2 tag
//                    tag.remove(COORDS2);
//
//                    // update coords1
//                    tag.put(COORDS1, Coords.of(placeContext.getClickedPos()).save(new CompoundTag()));
//                } else {
//
//                    // placing block2
//                    ICoords coords1 = Coords.EMPTY.load(tag.getCompound(COORDS1));
//                    ICoords coords2 = Coords.of(placeContext.getClickedPos());
//
//                    /* test if the parent parcel at both coords is the same */
//                    Optional<Parcel> parentParcelAtCoords1 = ParcelRegistry.findLeastSignificant(coords1);
//                    Optional<Parcel> parentParcelAtCoords2 = ParcelRegistry.findLeastSignificant(coords2);
//                    if (parentParcelAtCoords1.isEmpty() || parentParcelAtCoords2.isEmpty()) {
//                        context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
//                        return InteractionResult.SUCCESS;
//                    }
//                    if (parentParcelAtCoords1.get().getId().equals(parentParcelAtCoords2.get().getId())) {
//                        context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("parcel.add.not_same_parent")).withStyle(ChatFormatting.RED));
//                        return InteractionResult.SUCCESS;
//                    }
//
//                    // update coords2
//                    tag.put(COORDS2, coords2.save(new CompoundTag()));
//                    // place borders
//
//                    ZonePlacementBlockEntity blockEntity = (ZonePlacementBlockEntity) context.getLevel().getBlockEntity(placeContext.getClickedPos());
//                    blockEntity.setCoords1(coords1);
//                    blockEntity.setCoords2(coords2);
//                    blockEntity.setOwnerId(context.getPlayer().getUUID());
//
//                    // update the first block with both coords as well
//                    ZonePlacementBlockEntity blockEntity1 = (ZonePlacementBlockEntity) context.getLevel().getBlockEntity(coords1.toPos());
//                    blockEntity1.setCoords1(coords1);
//                    blockEntity1.setCoords2(coords2);
//                    blockEntity1.setOwnerId(context.getPlayer().getUUID());
//
//                    // display the border
//                    blockEntity.placeParcelBorder();
//                }
//            } else {
//                tag.put(COORDS1, Coords.of(placeContext.getClickedPos()).save(new CompoundTag()));
//            }
//
//            return InteractionResult.SUCCESS;
//        }
//    }
//
//    private void clear(BlockPlaceContext context, CompoundTag tag) {
//        // load the coords from the tag
//        ICoords coords1 = Coords.EMPTY.load(tag.getCompound(COORDS1));
//        ICoords coords2 = Coords.EMPTY.load(tag.getCompound(COORDS2));
//        clear(context, coords1, coords2);
//    }
//
//    private void clear(BlockPlaceContext context, ICoords coords1, ICoords coords2) {
//
//        // clear blocks at 1 & 2
//        if (context.getLevel().getBlockState(coords1.toPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
//            context.getLevel().setBlock(coords1.toPos(), Blocks.AIR.defaultBlockState(), 3);
//        }
//        if (context.getLevel().getBlockState(coords2.toPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
//            context.getLevel().setBlock(coords2.toPos(), Blocks.AIR.defaultBlockState(), 3);
//        }
//
//        // clear the borders
//       ZonePlacementBlockEntity.removeParcelBorder(context.getLevel(), new Box(coords1, coords2), ModBlocks.ZONE_BORDER.get(), 0);
//    }
}
