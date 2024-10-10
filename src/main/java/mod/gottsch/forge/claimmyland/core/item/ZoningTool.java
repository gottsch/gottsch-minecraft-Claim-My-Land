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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Created by Mark Gottschling on Oct 7, 2024
 */
public class ZoningTool extends Item {
    private static final String COORDS1 = "coords1";
    private static final String COORDS2 = "coords2";

    public ZoningTool(Properties properties) {
        super(properties);
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
        if (nationParcel.isEmpty() || nationParcel.get().getType() != ParcelType.NATION) {
            context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("zone_placement.not_nation")).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        // NOTE don't have to check if nation ID matches or
        // player is the owner as that is already checked by events.
        // ie. can't use the Zoning Tool if don't have access.

        // get the tag
        CompoundTag tag = context.getItemInHand().getOrCreateTag();

        // convert UseOnContext to BlockPlaceContext
        BlockPlaceContext placeContext = new BlockPlaceContext(context);


        // if using the tool on zoning block
        if (context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
            // test if there are two blocks on record
            if (!tag.contains(COORDS1) || !tag.contains(COORDS2)) {
                context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("zone_placement.begin_end_required")).withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }

            // create zone parcel using the Box defined by the 2 zoning blocks coords
            ICoords coords1 = Coords.EMPTY.load(tag.getCompound(COORDS1));
            ICoords coords2 = Coords.EMPTY.load(tag.getCompound(COORDS2));

            ParcelFactory.create(ParcelType.ZONE, nationParcel.get().getNationId())
                    .ifPresentOrElse(p -> {
                            p.setOwnerId(nationParcel.get().getOwnerId());
                            p.setCoords(coords1);
                            p.setSize(new Box(Coords.of(0, 0, 0), ModUtil.getSize(new Box(coords1, coords2))));

                            ClaimResult claimResult = p.handleEmbeddedClaim(context.getLevel(), nationParcel.get(), p.getBox());
                            if (claimResult == ClaimResult.SUCCESS) {
                                context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("parcel.add.success")).withStyle(ChatFormatting.GREEN));
                                ParcelRegistry.add(p);
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

            return InteractionResult.SUCCESS;
        } else {
            /*
             * placing a zone placement block
             */

            if (tag.contains(COORDS1)) {
                if (tag.contains(COORDS2)) {
                    // clear zone placement blocks and borders
                    clear(placeContext, tag);

                    // remove the coords2 tag
                    tag.remove(COORDS2);
                    // update coords1
                    tag.put(COORDS1, Coords.of(placeContext.getClickedPos()).save(new CompoundTag()));
                } else {
                    // update coords2
                    tag.put(COORDS2, Coords.of(placeContext.getClickedPos()).save(new CompoundTag()));
                    // TODO place borders
                }
            } else {
                tag.put(COORDS1, Coords.of(placeContext.getClickedPos()).save(new CompoundTag()));
            }
            context.getLevel().setBlock(placeContext.getClickedPos(), ModBlocks.ZONE_PLACEMENT_BLOCK.get().defaultBlockState(), 3);
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
        if (context.getLevel().getBlockState(coords1.toPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
            context.getLevel().setBlock(coords1.toPos(), Blocks.AIR.defaultBlockState(), 3);
        }
        if (context.getLevel().getBlockState(coords2.toPos()).is(ModBlocks.ZONE_PLACEMENT_BLOCK.get())) {
            context.getLevel().setBlock(coords2.toPos(), Blocks.AIR.defaultBlockState(), 3);
        }

        // TODO clear the borders
    }
}
