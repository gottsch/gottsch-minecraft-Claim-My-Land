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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.NationEstate;
import mod.gottsch.forge.claimmyland.core.parcel.*;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
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
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class CitizenDeed extends Deed {
    @Deprecated
//    public static final String NATION_NAME = "nation_name";

    public CitizenDeed(Properties properties) {
        super(properties);
        setParcelType(ParcelType.CITIZEN);
    }

    @Override
    public Optional<Parcel> createParcel(ItemStack deedStack, ICoords coords, Player player) {
        Optional<Parcel> optionalParcel = super.createParcel(deedStack, coords, player);

        if (optionalParcel.isEmpty()) {
            player.sendSystemMessage(Component.translatable(LangUtil.chat("citizen_deed.unable_create")).withStyle(ChatFormatting.RED));
            return optionalParcel;
        }

        // unwrap
        CitizenParcel parcel = (CitizenParcel) optionalParcel.get();

        CompoundTag tag = deedStack.getOrCreateTag();

        if (!tag.contains(Deed.NATION_ESTATE_ID)) {
            player.sendSystemMessage(Component.translatable(LangUtil.chat("citizen_deed.invalid")).withStyle(ChatFormatting.RED));
            return null;
        }

        Optional<Estate> nationEstate = EstateRegistry.get(tag.getUUID(Deed.NATION_ESTATE_ID));
        if (nationEstate.isEmpty()) {
            player.sendSystemMessage(Component.translatable(LangUtil.chat("citizen_deed.invalid")).withStyle(ChatFormatting.RED));
            return null;
        }

        parcel.setNationEstate((NationEstate) nationEstate.get());

        return optionalParcel;
    }

    @Override
    protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
        super.populateFoundationStone(blockEntity, deed, pos, player);

        CompoundTag tag = deed.getOrCreateTag();

        // check if parcel is within another existing parcel
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(Coords.of(pos));

        // override some properties if within another parcel
        // if claiming an existing citizen parcel
        if (registryParcel.isPresent()) {
            if (registryParcel.get().getType() == ParcelType.CITIZEN) {
                // unwrap
                CitizenParcel citizenParcel = (CitizenParcel) registryParcel.get();
                // update block entity with properties of that of the existing citizen parcel
                blockEntity.setParcelId(registryParcel.get().getId());
//                blockEntity.setNationId(((CitizenParcel) registryParcel.get()).getNationId());
//                blockEntity.setNationId(citizenParcel.getNationEstate().getId());
                blockEntity.setNationEstateId(citizenParcel.getNationEstate().getId());
                blockEntity.setRelativeBox(registryParcel.get().getSize());
                blockEntity.setCoords(registryParcel.get().getCoords());
            }
        }
    }

    @Override
    protected boolean validateWorldPlacement(Level level, BlockPos pos, Box size, Player player) {
        boolean result = super.validateWorldPlacement(level, pos, size, player);
        if (!result) {
            return false;
        }

        // TODO ensure the citizen parcel is wholly within a nation parcel

        return true;
    }

    @Override
    public Block getFoundationStone() {
        return ModBlocks.CITIZEN_FOUNDATION_STONE.get();
    }

    @Override
    public void appendDetailsHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // TODO update to use getParcelType()
//        if (stack.getTag() != null && stack.getTag().contains(Deed.PARCEL_TYPE)) {
        tooltip.add(Component.translatable(LangUtil.tooltip("deed.type"), ChatFormatting.BLUE + getParcelType().name())); //stack.getTag().getString(Deed.PARCEL_TYPE)));
//        }
        // TODO update to get from Estate.getId() / Estage.getName()
        if (stack.getTag() != null && stack.getTag().contains(Deed.NATION_ESTATE_ID)) {
            UUID nationEstateId = stack.getTag().getUUID(Deed.NATION_ESTATE_ID);
            EstateRegistry.get(nationEstateId).ifPresent(estate ->
                tooltip.add(Component.translatable(LangUtil.tooltip("deed.nation_id"), ChatFormatting.BLUE + estate.getName()))
            );
        } else {
            ClaimMyLand.LOGGER.debug("Citizen Deed doesn't have a Nation ID");
        }
        if (stack.getTag() != null && stack.getTag().contains(Deed.SIZE)) {
            appendSizeHoverText(stack, level, tooltip, flag);
        }
    }
}
