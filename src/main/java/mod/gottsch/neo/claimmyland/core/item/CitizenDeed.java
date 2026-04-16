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
package mod.gottsch.neo.claimmyland.core.item;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.block.ModBlocks;
import mod.gottsch.neo.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.neo.claimmyland.core.command.helper.PlayerMessageHelper;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.estate.NationEstate;
import mod.gottsch.neo.claimmyland.core.parcel.CitizenParcel;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.util.LangUtil;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
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

    public CitizenDeed(Properties properties) {
        super(properties);
        setParcelType(ParcelType.CITIZEN);
    }

    @Override
    public Optional<Parcel> createParcel(ItemStack deedStack, ICoords coords, Player player) {
        CompoundTag debugTag = deedStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ClaimMyLand.LOGGER.debug("CitizenDeed.createParcel() tag = {}", debugTag);

        Optional<Parcel> optionalParcel = super.createParcel(deedStack, coords, player);

        if (optionalParcel.isEmpty()) {
            PlayerMessageHelper.sendFailure(player, "citizen_deed.unable_create");
            return optionalParcel;
        }

        // unwrap
        CitizenParcel parcel = (CitizenParcel) optionalParcel.get();

        CompoundTag tag = deedStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(Deed.NATION_ESTATE_ID)) {
            PlayerMessageHelper.sendFailure(player,"citizen_deed.invalid");
            return Optional.empty();
        }

        UUID nationId = tag.getUUID(Deed.NATION_ESTATE_ID);
        ClaimMyLand.LOGGER.debug("CitizenDeed looking up nation estate id={}", nationId);
        Optional<Estate> nationEstate = EstateRegistry.get(nationId);
        ClaimMyLand.LOGGER.debug("CitizenDeed nation estate found={}", nationEstate.isPresent());


//        Optional<Estate> nationEstate = EstateRegistry.get(tag.getUUID(Deed.NATION_ESTATE_ID));
        if (nationEstate.isEmpty()) {
            PlayerMessageHelper.sendFailure(player,"citizen_deed.invalid");
            return Optional.empty();
        }

        parcel.setNationEstate((NationEstate) nationEstate.get());

        return optionalParcel;
    }

    @Override
    protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
        super.populateFoundationStone(blockEntity, deed, pos, player);

//        CompoundTag tag = deed.getOrCreateTag();

        // check if parcel is within another existing parcel
        String dimension = blockEntity.getLevel().dimension().location().toString();
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(Coords.of(pos), dimension);

        // override some properties if within another parcel
        // if claiming an existing citizen parcel
        if (registryParcel.isPresent()) {
            if (registryParcel.get().getType() == ParcelType.CITIZEN) {
//                ClaimMyLand.LOGGER.debug("populating citizen foundation stone; placing player -> {}", String.valueOf(player));
                // unwrap
                CitizenParcel citizenParcel = (CitizenParcel) registryParcel.get();
                // update block entity with properties of that of the existing citizen parcel
                blockEntity.setParcelId(registryParcel.get().getId());
                blockEntity.setNationEstateId(citizenParcel.getNationEstate().getId());
                blockEntity.setRelativeBox(registryParcel.get().getSize());
                blockEntity.setCoords(registryParcel.get().getCoords());
                blockEntity.setPlacingPlayerId(player.getUUID());
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
        tooltip.add(Component.translatable(LangUtil.tooltip("deed.type"), ChatFormatting.LIGHT_PURPLE + getParcelType().name()));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(Deed.NATION_ESTATE_NAME)) {
            String nationName = tag.getString(Deed.NATION_ESTATE_NAME);
            tooltip.add(Component.translatable(LangUtil.tooltip("deed.nation"), ChatFormatting.BLUE + nationName));
        }
        if (tag.contains(Deed.NATION_ESTATE_ID)) {
            UUID nationEstateId = tag.getUUID(Deed.NATION_ESTATE_ID);
            EstateRegistry.get(nationEstateId).ifPresent(estate ->
                tooltip.add(Component.translatable(LangUtil.tooltip("deed.nation_id"), estate.getId()))
            );
        } else {
//            ClaimMyLand.LOGGER.debug("citizen deed doesn't have a nation ID");
        }
        if (tag.contains(Deed.SIZE)) {
            appendSizeHoverText(stack, level, tooltip, flag);
        }
    }
}
