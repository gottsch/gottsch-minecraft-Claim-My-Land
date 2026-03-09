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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.WhitelistFormatter;
import mod.gottsch.forge.claimmyland.core.command.helper.WhitelistType;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.*;

import static mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper.*;

/**
 * @author by Mark Gottschling on 2/20/2026
 */
public abstract class WhitelistSubCommand implements SubCommand {

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return null;
    };
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return null;
    }

    static final SuggestionProvider<CommandSourceStack> CURRENT_BLOCK_TAGS = (source, builder) -> {
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Set<String>> list = Optional.empty();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
        if (ownerUuid.isPresent()) {
            list = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), estateName, WhitelistType.BLOCK_TAG);
        }
        return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
    };

    static final SuggestionProvider<CommandSourceStack> CURRENT_BLOCKS = (source, builder) -> {
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Set<String>> list = Optional.empty();

        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
        if (ownerUuid.isPresent()) {
            list = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), estateName, WhitelistType.BLOCK);
        }
        return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
    };

//    static final SuggestionProvider<CommandSourceStack> CURRENT_ITEM_TAGS = (source, builder) -> {
//        String parcelName = StringArgumentType.getString(source, ESTATE_NAME);
//        Optional<Set<String>> list = Optional.empty();
//        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
//        if (ownerUuid.isPresent()) {
//            list = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, WhitelistType.ITEM_TAG);
//        }
//        return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
//    };

    static final SuggestionProvider<CommandSourceStack> CURRENT_ITEMS = (source, builder) -> {
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Set<String>> list = Optional.empty();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
        if (ownerUuid.isPresent()) {
            list = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), estateName, WhitelistType.ITEM);
        }
        return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
    };

    static final SuggestionProvider<CommandSourceStack> CURRENT_ENTITIES = (source, builder) -> {
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Set<String>> list = Optional.empty();

        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
        if (ownerUuid.isPresent()) {
            list = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), estateName, WhitelistType.ENTITY);
        }
        return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
    };

    static final SuggestionProvider<CommandSourceStack> OPS_CURRENT_BLOCKS = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Set<String>> whitelist = Optional.empty();

        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            whitelist = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), estateName, WhitelistType.BLOCK);
        }
        return SharedSuggestionProvider.suggest(whitelist.orElseGet(Collections::emptySet), builder);
    };

    static final SuggestionProvider<CommandSourceStack> OPS_CURRENT_ITEMS = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Set<String>> whitelist = Optional.empty();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            whitelist = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), estateName, WhitelistType.ITEM);
        }
        return SharedSuggestionProvider.suggest(whitelist.orElseGet(Collections::emptySet), builder);
    };

    static Optional<Set<String>> getEstateWhitelistByType(CommandSourceStack source, UUID ownerUuid, String estateName, WhitelistType type) {
        return switch(type) {
            case BLOCK -> getEstateBlockWhitelist(source, ownerUuid, estateName);
            case BLOCK_TAG -> getEstateBlockTagWhitelist(source, ownerUuid, estateName);
            case ITEM -> getEstateItemWhitelist(source, ownerUuid, estateName);
            case ITEM_TAG -> getEstateItemTagWhitelist(source, ownerUuid, estateName);
            case ENTITY -> getEstateEntityWhitelist(source, ownerUuid, estateName);
            case ENTITY_TAG -> getEstateEntityTagWhitelist(source, ownerUuid, estateName);
            default -> Optional.empty();
        };
    }

    static Optional<Set<String>> getEstateBlockTagWhitelist(CommandSourceStack source, UUID ownerUuid, String estateName) {
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

    public static Optional<Set<String>> getEstateEntityTagWhitelist(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        return estate.map(Estate::getEntitySpawnTagWhitelist);
    }

    public static Optional<Set<String>> getEstateEntityWhitelist(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        return estate.map(Estate::getEntitySpawnWhitelist);
    }

    /**
     * estate player version
     */
    public int listFromEstate(CommandSourceStack source, String estateName, WhitelistType type) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return listFromEstate(source, playerUuid.get(), estateName, type);
        } else {
            sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /*
     * estate ops version
     */
    public int listFromEstate(CommandSourceStack source, String ownerName, String estateName, WhitelistType type) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return listFromEstate(source, ownerUuid.get(), estateName, type);
        } else {
            sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate common version
     */
    public int listFromEstate(CommandSourceStack source, UUID ownerUuid, String estateName, WhitelistType type) {

        Optional<Set<String>> whitelist = WhitelistSubCommand.getEstateWhitelistByType(source, ownerUuid, estateName, type);

        whitelist.ifPresentOrElse(data -> {
                    List<Component> messages = WhitelistFormatter
                            .formatStandAloneGenericWhitelist(data, type, type.getTitle() + " - " + estateName, null);

                    CommandHelper.sendLines(source, messages);
                },
                () -> failure(source, "estate." + type.name().toLowerCase() + ".list.failure")
        );
        return 1;
    }

    /**
     * estate ops version
     */
    public int addToEstate(CommandSourceStack source, String ownerName, String estateName, ItemInput itemInput, WhitelistType type) {
        try {
            ItemStack itemStack = itemInput.createItemStack(1, false);
            ItemLike item = itemStack.getItem();
            if (type == WhitelistType.BLOCK && item instanceof BlockItem blockItem) {
                return addToEstate(source, ownerName, estateName, ModUtil.getName(blockItem.getBlock()), WhitelistType.BLOCK);
            } else {
                return addToEstate(source, ownerName, estateName, ModUtil.getName(item.asItem()), WhitelistType.ITEM);
            }
        } catch(Exception e) {
            unexpectedError(source);
        }
        return 1;
    }

    /**
     * estate ops version
     */
    public int addToEstate(CommandSourceStack source, String ownerName, String estateName, ResourceLocation value, WhitelistType type) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return addToEstate(source, ownerUuid.get(), estateName, value, type);
        } else {
            sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate player versions
     */
    public int addToEstate(CommandSourceStack source, String estateName, BlockInput blockInput, WhitelistType type) {
        try {
            return addToEstate(source, estateName, ModUtil.getName(blockInput.getState().getBlock()), WhitelistType.BLOCK);
        } catch(Exception e) {
            unexpectedError(source);
        }
        return 1;
    }

    public int addToEstate(CommandSourceStack source, String estateName,  ItemInput itemInput, WhitelistType type) {
        try {
            ItemStack itemStack = itemInput.createItemStack(1, false);
            ItemLike item = itemStack.getItem();
            if (type == WhitelistType.BLOCK && item instanceof BlockItem blockItem) {
                return addToEstate(source,estateName, ModUtil.getName(blockItem.getBlock()), WhitelistType.BLOCK);
            } else {
                return addToEstate(source, estateName, ModUtil.getName(item.asItem()), WhitelistType.ITEM);
            }
        } catch(Exception e) {
            unexpectedError(source);
        }
        return 1;
    }

    public int addToEstate(CommandSourceStack source, String estateName, Entity entity, WhitelistType type) {
        try {
            return addToEstate(source, estateName, ModUtil.getName(entity.getType()), WhitelistType.ENTITY);
        } catch(Exception e) {
            unexpectedError(source);
        }
        return 1;
    }

    /**
     * estate player version
     */
    public int addToEstate(CommandSourceStack source, String estateName, ResourceLocation value, WhitelistType type) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return addToEstate(source, playerUuid.get(), estateName, value, type);
        } else {
            sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     * estate common version
     */
    public int addToEstate(CommandSourceStack source, UUID ownerUuid, String estateName, ResourceLocation value, WhitelistType type) {
        Optional<Set<String>> whitelist = WhitelistSubCommand.getEstateWhitelistByType(source, ownerUuid, estateName, type);

        whitelist.ifPresentOrElse(action -> {
                    action.add(value.toString());
                    CommandHelper.save(source.getLevel());
//                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".add.success")).withStyle(ChatFormatting.GREEN), false);
                    sendSuccess(source, "estate." + type.name().toLowerCase() + ".add.success");
//                }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".add.failure")).withStyle(ChatFormatting.RED), false)
                }, () -> failure(source, "estate." + type.name().toLowerCase() + ".add.failure")
        );
        return 1;
    }

    /**
     * estate player version
     */
    public int removeFromEstate(CommandSourceStack source, String estateName, ResourceLocation tagName, WhitelistType type) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return removeFromEstate(source, playerUuid.get(), estateName, tagName, type);
        } else {
            sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     * estate ops version
     */
    public int removeFromEstate(CommandSourceStack source, String ownerName, String estateName, ResourceLocation tagName, WhitelistType type) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return removeFromEstate(source, ownerUuid.get(), estateName, tagName, type);
        } else {
            sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate common version
     */
    public int removeFromEstate(CommandSourceStack source, UUID ownerUuid, String estateName, ResourceLocation tagName, WhitelistType type) {
        Optional<Set<String>> whitelist = WhitelistSubCommand.getEstateWhitelistByType(source, ownerUuid, estateName, type);

        whitelist.ifPresentOrElse(action -> {
                    if (action.remove(tagName.toString())) {
                        CommandHelper.save(source.getLevel());
//                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".remove.success")).withStyle(ChatFormatting.GREEN), false);
                        sendSuccess(source, "estate." + type.name().toLowerCase() + ".remove.success");
                    } else {
                        // TODO could be specific that it didn't match
//                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".remove.failure")).withStyle(ChatFormatting.RED), false);
                        failure(source, "estate." + type.name().toLowerCase() + ".add.failure");
                    }
                },
//                () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate." + type.name().toLowerCase() + ".remove.failure")).withStyle(ChatFormatting.RED), false)
                () -> failure(source, "estate." + type.name().toLowerCase() + ".add.failure")
        );
        return 1;
    }

}
