/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
 *
 */

package mod.gottsch.forge.claimmyland.core.command;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.BorderStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.EstateContext;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author by Mark Gottschling on 1/30/2026
 */
public class EstateCommandDelegate {

    private static int demolishParcel(CommandSourceStack source, ServerPlayer player, Parcel parcel) {
        ItemStack deed = createDeedForParcel(source.getLevel(), parcel);

        if (deed.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.demolish.zone_cannot_demolish"))
                    .withStyle(ChatFormatting.RED), false);
            return 1;
        }

        // copy props over
        CompoundTag tag = deed.getOrCreateTag();
        tag.putUUID(Deed.PARCEL_ID, parcel.getId());
//        if (parcel.getNationId() != null) {
//            tag.putUUID(NationDeed.NATION_ID, parcel.getNationId());
//        }
        if (parcel instanceof NationalizedParcel nationalizedParcel) {
            tag.putUUID(Deed.NATION_ESTATE_ID, nationalizedParcel.getNationEstate().getId());
        }

        player.getInventory().add(deed);
        ParcelRegistry.unregisterParcel(parcel);
        removeBorderStone(source.getLevel(), parcel);

        source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.demolish.success"))
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static ItemStack createDeedForParcel(ServerLevel level, Parcel parcel) {
        return switch (parcel.getType()) {
            case PLAYER -> DeedFactory.createPlayerDeed(parcel.getSize());
            case NATION -> DeedFactory.createNationDeed(level, parcel.getSize());
            case CITIZEN -> DeedFactory.createCitizenDeed(parcel.getSize(), parcel.getNationId());
            case ZONE, NONE -> ItemStack.EMPTY;
        };
    }

    private static void removeBorderStone(ServerLevel level, Parcel parcel) {
        // TODO this will only work if the border stone is at coords
        ICoords coords = parcel.getCoords();
        BlockEntity be = level.getBlockEntity(coords.toPos());
        if (be instanceof BorderStoneBlockEntity borderStone) {
            borderStone.removeParcelBorder(level, coords);
        }
    }
}
