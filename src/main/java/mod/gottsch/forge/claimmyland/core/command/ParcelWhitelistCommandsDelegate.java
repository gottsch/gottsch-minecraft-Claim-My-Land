/*
 * This file is part of Legacy Vault.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
 *
 * All rights reserved.
 *
 * Legacy Vault is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Legacy Vault is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Legacy Vault.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.claimmyland.core.command;

import mod.gottsch.forge.claimmyland.core.exception.PlayerNotFoundException;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

/**
 * TODO merge with Player whitelist
 * @author Mark Gottschling on Feb 24, 2025
 */
public class ParcelWhitelistCommandsDelegate {

    public enum WhitelistType {
        BLOCK,
        BLOCK_TAG,
        ITEM,
        ITEM_TAG,
        PLAYER;
    }

    public static List<Parcel> getParcelsByOwner(CommandSourceStack source, String ownerName) throws PlayerNotFoundException {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            return ParcelRegistry.findByOwner(player.getUUID());
        }
        throw new PlayerNotFoundException();
    }

    public static Optional<Parcel> getParcelByOwner(CommandSourceStack source, String ownerName, String parcelName) throws PlayerNotFoundException {
        return getParcelsByOwner(source, ownerName).stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
    }

    public static Optional<List<String>> getBlockTagWhitelist(CommandSourceStack source, String ownerName, String parcelName) throws PlayerNotFoundException {
        Optional<Parcel> parcel = getParcelByOwner(source, ownerName, parcelName);
        return parcel.map(Parcel::getBlockTagWhitelist);
    }

    public static Optional<List<String>> getBlockWhitelist(CommandSourceStack source, String ownerName, String parcelName) throws PlayerNotFoundException {
        Optional<Parcel> parcel = getParcelByOwner(source, ownerName, parcelName);
        return parcel.map(Parcel::getBlockWhitelist);
    }

    public static Optional<List<String>> getItemTagWhitelist(CommandSourceStack source, String ownerName, String parcelName) throws PlayerNotFoundException {
        Optional<Parcel> parcel = getParcelByOwner(source, ownerName, parcelName);
        return parcel.map(Parcel::getItemTagWhitelist);
    }

    public static Optional<List<String>> getItemWhitelist(CommandSourceStack source, String ownerName, String parcelName) throws PlayerNotFoundException {
        Optional<Parcel> parcel = getParcelByOwner(source, ownerName, parcelName);
        return parcel.map(Parcel::getItemWhitelist);
    }

    public static Optional<List<String>> getWhitelistByType(CommandSourceStack source, String ownerName, String parcelName, WhitelistType type) throws PlayerNotFoundException {
        return switch(type) {
            case BLOCK -> getBlockWhitelist(source, ownerName, parcelName);
            case BLOCK_TAG -> getBlockTagWhitelist(source, ownerName, parcelName);
            case ITEM -> getItemWhitelist(source, ownerName, parcelName);
            case ITEM_TAG -> getItemTagWhitelist(source, ownerName, parcelName);
            default -> Optional.empty();
        };
    }

    public static int add(CommandSourceStack source, String ownerName, String parcelName, ItemInput itemInput, WhitelistType type) {
        try {
            ItemStack itemStack = itemInput.createItemStack(1, false);
            ItemLike item = itemStack.getItem();
            if (type == WhitelistType.BLOCK && item instanceof BlockItem blockItem) {
                return add(source, ownerName, parcelName, ModUtil.getName(blockItem.getBlock()), WhitelistType.BLOCK);
            } else {
                return add(source, ownerName, parcelName, ModUtil.getName(item.asItem()), WhitelistType.ITEM);
            }
        } catch(Exception e) {
            CommandHelper.unexceptedError(source);
        }
        return 1;
    }

    public static int add(CommandSourceStack source, String ownerName, String parcelName, ResourceLocation value, WhitelistType type) {
        try {
            Optional<List<String>> list = getWhitelistByType(source, ownerName, parcelName, type);

            list.ifPresentOrElse(action -> {
                        action.add(value.toString());
                        CommandHelper.save(source.getLevel());
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".add.success")).withStyle(ChatFormatting.GREEN), false);
                    }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".add.failure")).withStyle(ChatFormatting.RED), false)
            );
        } catch (PlayerNotFoundException e) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }
        return 1;
    }

    public static int remove(CommandSourceStack source, String ownerName, String parcelName, ResourceLocation tagName, WhitelistType type) {
        try {
            Optional<List<String>> list = getWhitelistByType(source, ownerName, parcelName, type);

            list.ifPresentOrElse(action -> {
                        if (action.remove(tagName.toString())) {
                            CommandHelper.save(source.getLevel());
                            source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".remove.success")).withStyle(ChatFormatting.GREEN), false);
                        } else {
                            // TODO could be specific that it didn't match
                            source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".remove.failure")).withStyle(ChatFormatting.RED), false);
                        }
                    }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".remove.failure")).withStyle(ChatFormatting.RED), false)
            );
        } catch (PlayerNotFoundException e) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int list(CommandSourceStack source, String ownerName, String parcelName, WhitelistType type) {
        try {
            Optional<List<String>> list = getWhitelistByType(source, ownerName, parcelName, type);

            list.ifPresentOrElse(action -> {

                        CommandHelper.sendNewLineMessage(source);
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".list"))
                                .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
                                .append(Component.translatable(parcelName)
                                        .withStyle(ChatFormatting.AQUA)), false);
                        CommandHelper.sendNewLineMessage(source);
                        action.forEach(blockTag -> {
                            if (blockTag != null) {
                                source.sendSuccess(() -> Component.literal(blockTag).withStyle(ChatFormatting.GREEN), false);
                            }
                        });
                    }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".list.failure")).withStyle(ChatFormatting.RED), false)
            );
        } catch (PlayerNotFoundException e) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

}
