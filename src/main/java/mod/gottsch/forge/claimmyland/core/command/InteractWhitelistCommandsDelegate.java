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

import mod.gottsch.forge.claimmyland.core.estate.Estate;
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
import java.util.Set;
import java.util.UUID;

/**
 * @author Mark Gottschling on Feb 24, 2025
 */
public class InteractWhitelistCommandsDelegate {

    public enum WhitelistType {
        BLOCK,
        BLOCK_TAG,
        ITEM,
        ITEM_TAG,
        FRIENDS,
        ENTITY,
        ENTITY_TAG;
    }

    /*
     * NOTE methods are return Optional<Set<String>> because the parcel may not exist and therefor the Set
     *  will not exist.
     */

    public static Optional<Set<String>> getBlockTagWhitelist(CommandSourceStack source, UUID ownerUuid, String parcelName) {
        Optional<Parcel> parcel = CommandHelper.getParcelByOwner(source, ownerUuid, parcelName);
        return parcel.map(Parcel::getBlockTagWhitelist);
    }

    public static Optional<Set<String>> getBlockWhitelist(CommandSourceStack source, UUID ownerUuid, String parcelName) {
        Optional<Parcel> parcel = CommandHelper.getParcelByOwner(source, ownerUuid, parcelName);
        return parcel.map(Parcel::getBlockWhitelist);
    }

    public static Optional<Set<String>> getItemTagWhitelist(CommandSourceStack source, UUID ownerUuid, String parcelName) {
        Optional<Parcel> parcel = CommandHelper.getParcelByOwner(source, ownerUuid, parcelName);
        return parcel.map(Parcel::getItemTagWhitelist);
    }

    public static Optional<Set<String>> getItemWhitelist(CommandSourceStack source, UUID ownerUuid, String parcelName) {
        Optional<Parcel> parcel = CommandHelper.getParcelByOwner(source, ownerUuid, parcelName);
        return parcel.map(Parcel::getItemWhitelist);
    }

    public static Optional<Set<String>> getWhitelistByType(CommandSourceStack source, UUID ownerUuid, String parcelName, WhitelistType type) {
        return switch(type) {
            case BLOCK -> getBlockWhitelist(source, ownerUuid, parcelName);
            case BLOCK_TAG -> getBlockTagWhitelist(source, ownerUuid, parcelName);
            case ITEM -> getItemWhitelist(source, ownerUuid, parcelName);
            case ITEM_TAG -> getItemTagWhitelist(source, ownerUuid, parcelName);
            default -> Optional.empty();
        };
    }

    public static Optional<Set<String>> getEstateBlockTagWhitelist(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        return estate.map(Estate::getBlockTagWhitelist);
    }

    public static Optional<Set<String>> getEstateBlockWhitelist(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        return estate.map(Estate::getBlockWhitelist);
    }

    public static Optional<Set<String>> getEstateItemTagWhitelist(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        return estate.map(Estate::getItemTagWhitelist);
    }

    public static Optional<Set<String>> getEstateItemWhitelist(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        return estate.map(Estate::getItemWhitelist);
    }
    
    public static Optional<Set<String>> getEstateWhitelistByType(CommandSourceStack source, UUID ownerUuid, String estateName, WhitelistType type) {
        return switch(type) {
            case BLOCK -> getEstateBlockWhitelist(source, ownerUuid, estateName);
            case BLOCK_TAG -> getEstateBlockTagWhitelist(source, ownerUuid, estateName);
            case ITEM -> getEstateItemWhitelist(source, ownerUuid, estateName);
            case ITEM_TAG -> getEstateItemTagWhitelist(source, ownerUuid, estateName);
            default -> Optional.empty();
        };
    }

    /**
     * ops version
     */
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

    /**
     * player version
     */
    public static int add(CommandSourceStack source, String parcelName,  ItemInput itemInput, WhitelistType type) {
        try {
            ItemStack itemStack = itemInput.createItemStack(1, false);
            ItemLike item = itemStack.getItem();
            if (type == WhitelistType.BLOCK && item instanceof BlockItem blockItem) {
                return add(source,parcelName, ModUtil.getName(blockItem.getBlock()), WhitelistType.BLOCK);
            } else {
                return add(source, parcelName, ModUtil.getName(item.asItem()), WhitelistType.ITEM);
            }
        } catch(Exception e) {
            CommandHelper.unexceptedError(source);
        }
        return 1;
    }

    /**
     * player version
     */
    public static int add(CommandSourceStack source, String parcelName, ResourceLocation value, WhitelistType type) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return add(source, playerUuid.get(), parcelName, value, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /*
     * ops version
     */
    public static int add(CommandSourceStack source, String ownerName, String parcelName, ResourceLocation value, WhitelistType type) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return add(source, ownerUuid.get(), parcelName, value, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * common version
     */
    private static int add(CommandSourceStack source, UUID ownerUuid, String parcelName, ResourceLocation value, WhitelistType type) {
        Optional<Set<String>> whitelist = getWhitelistByType(source, ownerUuid, parcelName, type);

        whitelist.ifPresentOrElse(action -> {
                    action.add(value.toString());
                    CommandHelper.save(source.getLevel());
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".add.success")).withStyle(ChatFormatting.GREEN), false);
                }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".add.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }

    /**
     * player version
     */
    public static int remove(CommandSourceStack source, String parcelName, ResourceLocation tagName, WhitelistType type) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return remove(source, playerUuid.get(), parcelName, tagName, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     * ops version
     */
    public static int remove(CommandSourceStack source, String ownerName, String parcelName, ResourceLocation tagName, WhitelistType type) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return remove(source, ownerUuid.get(), parcelName, tagName, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * common version
     */
    public static int remove(CommandSourceStack source, UUID ownerUuid, String parcelName, ResourceLocation tagName, WhitelistType type) {
        Optional<Set<String>> whitelist = getWhitelistByType(source, ownerUuid, parcelName, type);

        whitelist.ifPresentOrElse(action -> {
                    if (action.remove(tagName.toString())) {
                        CommandHelper.save(source.getLevel());
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".remove.success")).withStyle(ChatFormatting.GREEN), false);
                    } else {
                        // TODO could be specific that it didn't match
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".remove.failure")).withStyle(ChatFormatting.RED), false);
                    }
                }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel." + type.name().toLowerCase() + ".remove.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }

    /**
     * player version
     */
    public static int list(CommandSourceStack source, String parcelName, WhitelistType type) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return list(source, playerUuid.get(), parcelName, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /*
     * ops version
     */
    public static int list(CommandSourceStack source, String ownerName, String parcelName, WhitelistType type) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return list(source, ownerUuid.get(), parcelName, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * common version
     */
    public static int list(CommandSourceStack source, UUID ownerUuid, String parcelName, WhitelistType type) {

        Optional<Set<String>> whitelist = getWhitelistByType(source, ownerUuid, parcelName, type);

        whitelist.ifPresentOrElse(action -> {

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
        return 1;
    }

    /**
     * estate ops version
     */
    public static int addToEstate(CommandSourceStack source, String ownerName, String estateName, ItemInput itemInput, WhitelistType type) {
        try {
            ItemStack itemStack = itemInput.createItemStack(1, false);
            ItemLike item = itemStack.getItem();
            if (type == WhitelistType.BLOCK && item instanceof BlockItem blockItem) {
                return addToEstate(source, ownerName, estateName, ModUtil.getName(blockItem.getBlock()), WhitelistType.BLOCK);
            } else {
                return addToEstate(source, ownerName, estateName, ModUtil.getName(item.asItem()), WhitelistType.ITEM);
            }
        } catch(Exception e) {
            CommandHelper.unexceptedError(source);
        }
        return 1;
    }

    /**
     * estate ops version
     */
    public static int addToEstate(CommandSourceStack source, String ownerName, String estateName, ResourceLocation value, WhitelistType type) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return addToEstate(source, ownerUuid.get(), estateName, value, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate player version
     */
    public static int addToEstate(CommandSourceStack source, String estateName,  ItemInput itemInput, WhitelistType type) {
        try {
            ItemStack itemStack = itemInput.createItemStack(1, false);
            ItemLike item = itemStack.getItem();
            if (type == WhitelistType.BLOCK && item instanceof BlockItem blockItem) {
                return addToEstate(source,estateName, ModUtil.getName(blockItem.getBlock()), WhitelistType.BLOCK);
            } else {
                return addToEstate(source, estateName, ModUtil.getName(item.asItem()), WhitelistType.ITEM);
            }
        } catch(Exception e) {
            CommandHelper.unexceptedError(source);
        }
        return 1;
    }

    /**
     * estate player version
     */
    public static int addToEstate(CommandSourceStack source, String estateName, ResourceLocation value, WhitelistType type) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return addToEstate(source, playerUuid.get(), estateName, value, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     * estate common version
     */
    private static int addToEstate(CommandSourceStack source, UUID ownerUuid, String estateName, ResourceLocation value, WhitelistType type) {
        Optional<Set<String>> whitelist = getEstateWhitelistByType(source, ownerUuid, estateName, type);

        whitelist.ifPresentOrElse(action -> {
                    action.add(value.toString());
                    CommandHelper.save(source.getLevel());
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".add.success")).withStyle(ChatFormatting.GREEN), false);
                }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".add.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }

    /**
     * estate player version
     */
    public static int removeFromEstate(CommandSourceStack source, String estateName, ResourceLocation tagName, WhitelistType type) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return removeFromEstate(source, playerUuid.get(), estateName, tagName, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     * estate ops version
     */
    public static int removeFromEstate(CommandSourceStack source, String ownerName, String estateName, ResourceLocation tagName, WhitelistType type) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return removeFromEstate(source, ownerUuid.get(), estateName, tagName, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate common version
     */
    public static int removeFromEstate(CommandSourceStack source, UUID ownerUuid, String estateName, ResourceLocation tagName, WhitelistType type) {
        Optional<Set<String>> whitelist = getEstateWhitelistByType(source, ownerUuid, estateName, type);

        whitelist.ifPresentOrElse(action -> {
                    if (action.remove(tagName.toString())) {
                        CommandHelper.save(source.getLevel());
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".remove.success")).withStyle(ChatFormatting.GREEN), false);
                    } else {
                        // TODO could be specific that it didn't match
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".remove.failure")).withStyle(ChatFormatting.RED), false);
                    }
                }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".remove.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }
    
    /**
     * estate player version
     */
    public static int listFromEstate(CommandSourceStack source, String estateName, WhitelistType type) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return listFromEstate(source, playerUuid.get(), estateName, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /*
     * estate ops version
     */
    public static int listFromEstate(CommandSourceStack source, String ownerName, String estateName, WhitelistType type) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return listFromEstate(source, ownerUuid.get(), estateName, type);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate common version
     */
    public static int listFromEstate(CommandSourceStack source, UUID ownerUuid, String estateName, WhitelistType type) {

        Optional<Set<String>> whitelist = getEstateWhitelistByType(source, ownerUuid, estateName, type);

        whitelist.ifPresentOrElse(action -> {

                    CommandHelper.sendNewLineMessage(source);
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".list"))
                            .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
                            .append(Component.translatable(estateName)
                                    .withStyle(ChatFormatting.AQUA)), false);
                    CommandHelper.sendNewLineMessage(source);
                    action.forEach(blockTag -> {
                        if (blockTag != null) {
                            source.sendSuccess(() -> Component.literal(blockTag).withStyle(ChatFormatting.GREEN), false);
                        }
                    });
                }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".list.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }
}
