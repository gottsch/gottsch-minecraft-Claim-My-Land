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
import mod.gottsch.forge.claimmyland.core.command.helper.PlayerMessageHelper;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.network.CMLNetwork;
import mod.gottsch.forge.claimmyland.core.parcel.*;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Mark Gottschling on Oct 7, 2024
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

        ResourceLocation dimId = context.getLevel().dimension().location();
        List<? extends String> excluded = Config.SERVER.dimensions.excludedDimensions.get();
        if (excluded.stream().anyMatch(e -> e.equals(dimId.toString()))) {
            return InteractionResult.FAIL;
        }

        // wrap the context in a BlockPlaceContext to get the position of the placed block,
        // not the position of the block face that was clicked. without this, findLeastSignificant()
        // will be using the wrong coords and could possibly miss the parcel if on an edge.
        BlockPlaceContext placeContext = new BlockPlaceContext(context);

        String dimension = context.getLevel().dimension().location().toString();
        Optional<Parcel> nationParcel = ParcelRegistry.findLeastSignificant(Coords.of(placeContext.getClickedPos()), dimension);
        if (nationParcel.isEmpty() || !nationParcel.get().isNation()) {
            PlayerMessageHelper.sendFailure(context.getPlayer(), "zone_placement.not_nation");
            return InteractionResult.FAIL;
        }

        CompoundTag tag = context.getItemInHand().getOrCreateTag();


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
            PlayerMessageHelper.sendFailure(context.getPlayer(), "zone_placement.begin_end_required");
            return InteractionResult.SUCCESS;
        }

        ICoords coords1 = loadCoords(tag, COORDS1);
        ICoords coords2 = loadCoords(tag, COORDS2);
        Box box = new Box(coords1, coords2);

        if (isBoxTooSmall(box)) {
            PlayerMessageHelper.sendFailure(context.getPlayer(), "parcel.add.failure_too_small");
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
            PlayerMessageHelper.sendFailure(context.getPlayer(), "unexpected_error");
            return InteractionResult.SUCCESS;
        }

        Parcel zone = created.get();
        zone.setOwnerId(nationParcel.getOwnerId());
        zone.setCoords(box.getMinCoords());
        zone.setSize(new Box(Coords.of(0, 0, 0), box.getSize()));

        ClaimResult claimResult = zone.handleEmbeddedClaim(context.getLevel(), nationParcel, zone.getBox());

        if (claimResult.isSuccess()) {
            // clean up preview border
            ZonePlacementBlockEntity be = (ZonePlacementBlockEntity) context.getLevel().getBlockEntity(coords2.toPos());
            if (be != null && context.getLevel() instanceof ServerLevel serverLevel) {
                CMLNetwork.removePreviewParcelFromTracking(serverLevel, be.getParcelId(), coords2.toPos());
            }

            if (claimResult == ClaimResult.SUCCESS_WITH_WARNINGS) {
                PlayerMessageHelper.sendWarning(context.getPlayer(), "parcel.add.structure_warning");
            } else {
                PlayerMessageHelper.sendSuccess(context.getPlayer(), "parcel.add.success");
            }
            CommandHelper.save(context.getLevel());
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
        String dimension = context.getLevel().dimension().location().toString();

        boolean hasValidCoords1 = tag.contains(COORDS1)
                && level.getBlockEntity(loadCoords(tag, COORDS1).toPos()) instanceof ZonePlacementBlockEntity;

        if (hasValidCoords1) {
            boolean hasValidCoords2 = tag.contains(COORDS2)
                    && level.getBlockEntity(loadCoords(tag, COORDS2).toPos()) instanceof ZonePlacementBlockEntity;

            if (hasValidCoords2) {
                // Both corners already set: reset and start fresh from clicked position
                clear(placeContext, tag);
                tag.remove(COORDS2);
                tag.put(COORDS1, clickedCoords.save(new CompoundTag()));
                level.setBlock(placeContext.getClickedPos(), ModBlocks.ZONE_PLACEMENT_BLOCK.get().defaultBlockState(), 3);
                return InteractionResult.SUCCESS;
            }

            // second corner — validate parent matches before placing
            ICoords coords1 = loadCoords(tag, COORDS1);
            Optional<Parcel> parentAtCoords1 = ParcelRegistry.findLeastSignificant(coords1, dimension);
            Optional<Parcel> parentAtCoords2 = ParcelRegistry.findLeastSignificant(clickedCoords, dimension);

            if (parentAtCoords1.isEmpty() || parentAtCoords2.isEmpty()) {
                PlayerMessageHelper.sendFailure(context.getPlayer(), "unexpected_error");
                return InteractionResult.FAIL;
            }

            // reject if either corner is inside a zone
            if (parentAtCoords1.get().isZone() || parentAtCoords2.get().isZone()) {
                PlayerMessageHelper.sendFailure(context.getPlayer(), "zone_placement.not_nation");
                return InteractionResult.FAIL;
            }

            // reject if corner are in two different nations
            if (!parentAtCoords1.get().getId().equals(parentAtCoords2.get().getId())) {
                PlayerMessageHelper.sendFailure(context.getPlayer(), "zone_placement.not_same_nation");
                return InteractionResult.FAIL;
            }

            // validation passed — place block and link
            level.setBlock(placeContext.getClickedPos(), ModBlocks.ZONE_PLACEMENT_BLOCK.get().defaultBlockState(), 3);
            return placeSecondCorner(context, placeContext, tag, clickedCoords);
        }

        // first corner: just place and record coords1
        level.setBlock(placeContext.getClickedPos(), ModBlocks.ZONE_PLACEMENT_BLOCK.get().defaultBlockState(), 3);
        tag.put(COORDS1, clickedCoords.save(new CompoundTag()));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult placeSecondCorner(UseOnContext context, BlockPlaceContext placeContext,
                                                CompoundTag tag, ICoords coords2) {
        ICoords coords1 = loadCoords(tag, COORDS1);

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

    UUID previewId = UUID.randomUUID();
    blockEntity1.setParcelId(previewId);
    blockEntity2.setParcelId(previewId);

    if (level instanceof ServerLevel serverLevel) {
        Box box = new Box(coords1, coords2);
        String dimension = level.dimension().location().toString();
        int conflictState = ParcelRegistry.resolveConflictState(box, player.getUUID(), null, ParcelType.ZONE);
        CMLNetwork.syncPreviewParcelToTrackingPlayersAndSelf(
                serverLevel, (ServerPlayer) player,
                previewId, previewId,
                player.getUUID(), ParcelType.ZONE,
                box, placeContext.getClickedPos().getY(),
                dimension, conflictState);
    }
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

    private void clear(BlockPlaceContext context, CompoundTag tag) {
        clear(context, loadCoords(tag, COORDS1), loadCoords(tag, COORDS2));
    }

    private void clear(BlockPlaceContext context, ICoords coords1, ICoords coords2) {
        clearBlockIfZonePlacement(context.getLevel(), coords1);
        clearBlockIfZonePlacement(context.getLevel(), coords2);
//        ZonePlacementBlockEntity.removeParcelBorder(context.getLevel(), new Box(coords1, coords2), ModBlocks.ZONE_BORDER.get(), 0);
    }

    private void clearBlockIfZonePlacement(Level level, ICoords coords) {
        if (level.getBlockState(coords.toPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
            level.setBlock(coords.toPos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
