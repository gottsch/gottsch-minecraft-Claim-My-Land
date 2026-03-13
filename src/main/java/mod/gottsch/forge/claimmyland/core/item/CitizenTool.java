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
import mod.gottsch.forge.claimmyland.core.block.entity.CitizenPlacementBlockEntity;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.PlayerMessageHelper;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.parcel.*;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.List;
import java.util.Optional;

/**
 * @author Mark Gottschling on Oct 7, 2024
 */
public class CitizenTool extends BlockItem {

    private static final String COORDS1 = "coords1";
    private static final String COORDS2 = "coords2";

    public CitizenTool(Block block, Properties properties) {
        super(block, properties);
    }

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

        Optional<Parcel> parentParcel = ParcelRegistry.findLeastSignificant(Coords.of(context.getClickedPos()), dimId.toString());
        if (parentParcel.isEmpty() || !isValidParent(parentParcel.get())) {
            PlayerMessageHelper.sendFailure(context.getPlayer(), "citizen_placement.not_valid_parent");
            return InteractionResult.FAIL;
        }

        CompoundTag tag = context.getItemInHand().getOrCreateTag();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);

        if (isClickingCitizenPlacementBlock(context)) {
            return handleParcelCreation(context, placeContext, tag, parentParcel.get());
        } else {
            return handleBlockPlacement(context, placeContext, tag);
        }
    }

    // -------------------------------------------------------------------------
    // Parcel Creation (clicking an existing CitizenPlacementBlock to finalize)
    // -------------------------------------------------------------------------

    private InteractionResult handleParcelCreation(UseOnContext context, BlockPlaceContext placeContext,
                                                   CompoundTag tag, Parcel parentParcel) {
        if (!tag.contains(COORDS1) || !tag.contains(COORDS2)) {
            PlayerMessageHelper.sendFailure(context.getPlayer(), "citizen_placement.begin_end_required");
            return InteractionResult.SUCCESS;
        }

        ICoords coords1 = loadCoords(tag, COORDS1);
        ICoords coords2 = loadCoords(tag, COORDS2);
        Box box = new Box(coords1, coords2);

        if (isBoxTooSmall(box)) {
            PlayerMessageHelper.sendFailure(context.getPlayer(), "parcel.add.failure_too_small");
            return InteractionResult.SUCCESS;
        }

        tryCreateCitizenParcel(context, parentParcel, box);

        clear(placeContext, coords1, coords2);
        tag.remove(COORDS1);
        tag.remove(COORDS2);

        return InteractionResult.SUCCESS;
    }

    private void tryCreateCitizenParcel(UseOnContext context, Parcel parentParcel, Box box) {
        Optional<Parcel> created = ParcelTypeRegistry.create(ParcelType.CITIZEN,
                parentParcel.isNation()
                        ? parentParcel.getEstate()
                        : ((NationalizedParcel)parentParcel).getNationEstate()
                );
        if (created.isEmpty()) {
            PlayerMessageHelper.sendFailure(context.getPlayer(), "unexpected_error");
            return;
        }

        Parcel citizen = created.get();
        citizen.setOwnerId(parentParcel.getOwnerId());
        citizen.setCoords(box.getMinCoords());
        citizen.setSize(new Box(Coords.of(0, 0, 0), box.getSize()));

        ClaimResult claimResult = citizen.handleEmbeddedClaim(context.getLevel(), parentParcel, citizen.getBox());
//        if (claimResult == ClaimResult.SUCCESS) {
//            PlayerMessageHelper.sendSuccess(context.getPlayer(), "parcel.add.success");
//            CommandHelper.save(context.getLevel());
//        } else {
//            PlayerMessageHelper.sendFailure(context.getPlayer(), "parcel.add.failure_with_overlaps");
//        }
        if (claimResult.isSuccess()) {
            if (claimResult == ClaimResult.SUCCESS_WITH_WARNINGS) {
                PlayerMessageHelper.sendWarning(context.getPlayer(), "parcel.add.structure_warning");
            } else {
                PlayerMessageHelper.sendSuccess(context.getPlayer(), "parcel.add.success");
            }
            CommandHelper.save(context.getLevel());
        } else {
            switch (claimResult) {
                case STRUCTURE_DENIED -> PlayerMessageHelper.sendFailure(context.getPlayer(), "parcel.add.structure_denied");
                default -> PlayerMessageHelper.sendFailure(context.getPlayer(), "parcel.add.failure_with_overlaps");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Block Placement (placing CitizenPlacementBlocks to define corners)
    // -------------------------------------------------------------------------

    private InteractionResult handleBlockPlacement(UseOnContext context, BlockPlaceContext placeContext,
                                                   CompoundTag tag) {
        Level level = context.getLevel();
        ICoords clickedCoords = Coords.of(placeContext.getClickedPos());

        level.setBlock(placeContext.getClickedPos(), ModBlocks.CITIZEN_PLACEMENT_BLOCK.get().defaultBlockState(), 3);

        boolean hasValidCoords1 = tag.contains(COORDS1)
                && level.getBlockEntity(loadCoords(tag, COORDS1).toPos()) instanceof CitizenPlacementBlockEntity;

        if (!hasValidCoords1) {
            // First corner: just record coords1
            tag.put(COORDS1, clickedCoords.save(new CompoundTag()));
            return InteractionResult.SUCCESS;
        }

        boolean hasValidCoords2 = tag.contains(COORDS2)
                && level.getBlockEntity(loadCoords(tag, COORDS2).toPos()) instanceof CitizenPlacementBlockEntity;

        if (hasValidCoords2) {
            // Both corners already set: reset and start fresh from clicked position
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

        String dimension = context.getLevel().dimension().location().toString();
        Optional<Parcel> parentAtCoords1 = ParcelRegistry.findLeastSignificant(coords1, dimension);
        Optional<Parcel> parentAtCoords2 = ParcelRegistry.findLeastSignificant(coords2, dimension);

        if (parentAtCoords1.isEmpty() || parentAtCoords2.isEmpty()) {
            PlayerMessageHelper.sendFailure(context.getPlayer(), "unexpected_error");
            return InteractionResult.SUCCESS;
        }

        if (!parentAtCoords1.get().getId().equals(parentAtCoords2.get().getId())) {
            PlayerMessageHelper.sendFailure(context.getPlayer(), "citizen_placement.not_same_zone");
            return InteractionResult.SUCCESS;
        }

        tag.put(COORDS2, coords2.save(new CompoundTag()));

        linkCitizenPlacementBlocks(context.getLevel(), placeContext, coords1, coords2, context.getPlayer());

        return InteractionResult.SUCCESS;
    }

    private void linkCitizenPlacementBlocks(Level level, BlockPlaceContext placeContext,
                                            ICoords coords1, ICoords coords2, Player player) {
        CitizenPlacementBlockEntity blockEntity2 = (CitizenPlacementBlockEntity) level.getBlockEntity(placeContext.getClickedPos());
        blockEntity2.setCoords1(coords1);
        blockEntity2.setCoords2(coords2);
        blockEntity2.setOwnerId(player.getUUID());

        CitizenPlacementBlockEntity blockEntity1 = (CitizenPlacementBlockEntity) level.getBlockEntity(coords1.toPos());
        blockEntity1.setCoords1(coords1);
        blockEntity1.setCoords2(coords2);
        blockEntity1.setOwnerId(player.getUUID());

        blockEntity2.placeParcelBorder((ServerPlayer) player);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isValidParent(Parcel parcel) {
        return
                parcel.isNation() ||
                parcel.isZone();
    }

    private boolean isClickingCitizenPlacementBlock(UseOnContext context) {
        return context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.CITIZEN_PLACEMENT_BLOCK.get());
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
        clearBlockIfCitizenPlacement(context.getLevel(), coords1);
        clearBlockIfCitizenPlacement(context.getLevel(), coords2);
//        CitizenPlacementBlockEntity.removeParcelBorder(context.getLevel(), new Box(coords1, coords2), ModBlocks.CITIZEN_BORDER.get(), 0);
    }

    private void clearBlockIfCitizenPlacement(Level level, ICoords coords) {
        if (level.getBlockState(coords.toPos()).is(ModBlocks.CITIZEN_PLACEMENT_BLOCK.get())) {
            level.setBlock(coords.toPos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}