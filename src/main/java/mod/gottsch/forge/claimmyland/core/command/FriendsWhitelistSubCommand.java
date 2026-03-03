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
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.EstateDisplayFormatter;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 2/20/2026
 */
public class FriendsWhitelistSubCommand implements SubCommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(FRIENDS)
                ///// FRIENDS WHITELIST ADD /////
                .then(Commands.literal(ADD)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(FRIEND_NAME, StringArgumentType.string())
                                        .suggests(PLAYER_NAMES)
                                        .executes(source -> {
                                            return addToEstate(source.getSource(),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    StringArgumentType.getString(source, FRIEND_NAME));
                                        })
                                )
                        )
                )
                ///// FRIENDS WHITELIST REMOVE /////
                .then(Commands.literal(REMOVE)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(FRIEND_NAME, StringArgumentType.string())
                                        .suggests(CURRENT_FRIENDS_NAMES)
                                        .executes(source -> {
                                            return removeFromEstate(source.getSource(),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    StringArgumentType.getString(source, FRIEND_NAME));
                                        })
                                )
                        )
                )
                ///// FRIENDS WHITELIST LIST /////
                .then(Commands.literal(LIST)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .executes(source -> {
                                    return listFromEstate(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME));
                                })
                        )
                );

    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(FRIENDS)
                ///// FRIENDS WHITELIST ADD /////
                .then(Commands.literal(ADD)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(FRIEND_NAME, StringArgumentType.string())
                                                .suggests(PLAYER_NAMES)
                                                .executes(source -> {
                                                    return addToEstate(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, ESTATE_NAME),
                                                            StringArgumentType.getString(source, FRIEND_NAME));
                                                })
                                        )
                                )
                        )
                )
                ///// FRIENDS WHITELIST REMOVE /////
                .then(Commands.literal(REMOVE)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(FRIEND_NAME, StringArgumentType.string())
                                                .suggests(CURRENT_FRIENDS_NAMES)
                                                .executes(source -> {
                                                    return removeFromEstate(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, ESTATE_NAME), StringArgumentType.getString(source, CommandHelper.FRIEND_NAME));
                                                })
                                        )
                                )
                        )
                )
                ///// FRIENDS WHITELIST LIST /////
                .then(Commands.literal(LIST)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .executes(source -> {
                                            return listFromEstate(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, PARCEL_NAME));
                                        })
                                )
                        )

                );
    }

    /*
     * estate player version
     */
    public static int addToEstate(CommandSourceStack source, String estateName, String friendsName) {
        // refactored way
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source);
        if (ownerUuid.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
            return -1;
        }

        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, friendsName);
        if (friendUuid.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, friendsName);
            return -1;
        }

        return addToEstate(source, ownerUuid.get(), estateName, friendUuid.get());
    }

    /*
     * ops version
     */
    public static int addToEstate(CommandSourceStack source, String ownerName, String estateName, String friendsName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
            return -1;
        }

        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, friendsName);
        if (friendUuid.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, friendsName);
            return -1;
        }

        return addToEstate(source, ownerUuid.get(), estateName, friendUuid.get());
    }

    /**
     * estate common version
     */
    private static int addToEstate(CommandSourceStack source, UUID ownerUuid, String estateName, UUID friendUuid) {
        if (ownerUuid.equals(friendUuid)) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.add.same_name.failure")).withStyle(ChatFormatting.RED), false);
            return -1;
        }
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        estate.ifPresentOrElse(action -> {
                    action.getPlayerWhitelist().add(friendUuid);
                    CommandHelper.save(source.getLevel());
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.add.success")).withStyle(ChatFormatting.GREEN), false);
                }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.add.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }

    /**
     * estate player version
     */
    public static int removeFromEstate(CommandSourceStack source, String estateName, String friendsName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source);
        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, friendsName);

        if (ownerUuid.isPresent()) {
            if (friendUuid.isPresent()) {
                return removeFromEstate(source,ownerUuid.get(), estateName, friendUuid.get());
            } else {
                CommandHelper.sendUnableToLocatePlayerMessage(source, friendsName);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     * estate ops version
     */
    public static int removeFromEstate(CommandSourceStack source, String ownerName, String estateName, String friendsName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        Optional<UUID> friendsUuid = CommandHelper.getPlayerUuid(source, friendsName);
        if (ownerUuid.isPresent()) {
            if (friendsUuid.isPresent()) {
                return removeFromEstate(source, ownerUuid.get(), estateName, friendsUuid.get());
            } else {
                CommandHelper.sendUnableToLocatePlayerMessage(source, friendsName);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate common version
     */
    public static int removeFromEstate(CommandSourceStack source, UUID ownerUuid, String estateName, UUID friendsUuid) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        estate.ifPresentOrElse(action -> {
                    if (action.getPlayerWhitelist().remove(friendsUuid)) {
                        CommandHelper.save(source.getLevel());
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.remove.success")).withStyle(ChatFormatting.GREEN), false);
                    } else {
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.remove.failure")).withStyle(ChatFormatting.RED), false);
                    }
                },
                () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.remove.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }

    /**
     * estate player version
     */
    public static int listFromEstate(CommandSourceStack source, String estateName) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return listFromEstate(source, playerUuid.get(), estateName);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /*
     * estate ops version
     */
    public static int listFromEstate(CommandSourceStack source, String ownerName, String estateName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return listFromEstate(source, ownerUuid.get(), estateName);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate common version
     */
    public static int listFromEstate(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        estate.ifPresentOrElse(action -> {
                    List<Component> messages = EstateDisplayFormatter
                            .formatStandAlonePlayerWhitelist(source.getLevel(), action.getPlayerWhitelist(), "PLAYER WHITELIST - " + estateName, action.getId());

                    messages.forEach(component -> {
                        source.sendSuccess(() -> component, false);
                    });
                },
//                {
//                    CommandHelper.sendNewLineMessage(source);
//                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.list"))
//                            .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
//                            .append(Component.translatable(estateName)
//                                    .withStyle(ChatFormatting.AQUA)), false);
//                    CommandHelper.sendNewLineMessage(source);
//                    action.getPlayerWhitelist().forEach(uuid -> {
//                        Optional<String> friendsName = CommandHelper.getPlayerName(source, uuid);
//                        friendsName.ifPresent(s -> source.sendSuccess(() -> Component.literal(s).withStyle(ChatFormatting.GREEN), false));
//                    });
//                },
                () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.list.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }
}
