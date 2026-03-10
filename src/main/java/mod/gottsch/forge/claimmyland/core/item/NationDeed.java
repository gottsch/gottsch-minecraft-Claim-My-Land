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
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.command.helper.PlayerMessageHelper;
import mod.gottsch.forge.claimmyland.core.parcel.*;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Mar 27, 2024
 *
 */
public class NationDeed extends Deed {
//    @Deprecated
//    public static final String NATION_ID = "nation_id";

    public NationDeed(Properties properties) {
        super(properties);
        setParcelType(ParcelType.NATION);
    }

    @Override
    public Optional<Parcel> createParcel(ItemStack deedStack, ICoords coords, Player player) {
        Optional<Parcel> optionalParcel = super.createParcel(deedStack, coords, player);

        if (optionalParcel.isEmpty()) {
            PlayerMessageHelper.sendFailure(player, "nation_deed.unable_create");
            return optionalParcel;
        }

        NationParcel parcel = (NationParcel) optionalParcel.get();
        CompoundTag tag = deedStack.getOrCreateTag();

        // add nation id
        if (tag.contains(Deed.ESTATE_ID)) {
            parcel.getEstate().setId(tag.getUUID(Deed.ESTATE_ID));
        }

        // override coords
        parcel.setCoords(parcel.getCoords().withY(0));

        return optionalParcel;
    }

    @Override
    public Block getFoundationStone() {
        return ModBlocks.NATION_FOUNDATION_STONE.get();
    }

    @Override
    protected boolean validateWorldPlacement(Level level, BlockPos pos, Box size, Player player) {
        if (level.isOutsideBuildHeight(size.getMinCoords().getY())
            || level.isOutsideBuildHeight(size.getMaxCoords().getY())) {

            PlayerMessageHelper.sendFailure(player, "parcel.outside_world_boundaries");
            return false;
        }
        return true;
    }

    @Override
    protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
        super.populateFoundationStone(blockEntity, deed, pos, player);

        // default behaviour
        CompoundTag tag = deed.getOrCreateTag();
        Box size = getSize(tag);

//        blockEntity.setNationId(tag.contains(NATION_ID) ? tag.getUUID(NATION_ID) : null);

        // check if parcel is within another nation parcel ie it was abandoned
        String dimension = blockEntity.getLevel().dimension().location().toString();
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(Coords.of(pos), dimension);

        // override some properties if within another parcel
        if (registryParcel.isPresent()) {
            if (registryParcel.get().getType() == ParcelType.NATION) {
                // update block entity with properties of that of the existing citizen parcel
                blockEntity.setParcelId(registryParcel.get().getId());
//                blockEntity.setDeedId(registryParcel.get().getDeedId());
//                blockEntity.setNationId(((NationParcel) registryParcel.get()).getNationId());
                blockEntity.setRelativeBox(registryParcel.get().getSize());
                blockEntity.setCoords(registryParcel.get().getCoords());
            }
        }
    }

    @Override
    public void appendUsageHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(LangUtil.tooltip("nation_deed.usage")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(LangUtil.NEWLINE));
    }

    @Override
    public void appendDetailsHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
//        if (stack.getTag() != null && stack.getTag().contains(Deed.PARCEL_TYPE)) {
            tooltip.add(Component.translatable(LangUtil.tooltip("deed.type"), ChatFormatting.BLUE + getParcelType().name())); // stack.getTag().getString(Deed.PARCEL_TYPE)));
//        }

        // NOTE nation DEED does NOT have a nationId nor nationName as
        // a deed is a net new parcel to be used by anyone. the name would not be known
        // and also this avoids duplicate names floating around in the deeds.
//        if (stack.getTag() != null && stack.getTag().contains(NATION_ID)) {
//            String id = stack.getTag().getUUID(NationDeed.NATION_ID).toString();
//            tooltip.add(Component.translatable(LangUtil.tooltip("deed.nation_id"), ChatFormatting.BLUE + id));
//        }

        if (stack.getTag() != null && stack.getTag().contains(Deed.SIZE)) {
            appendSizeHoverText(stack, level, tooltip, flag);
        }
    }
}
