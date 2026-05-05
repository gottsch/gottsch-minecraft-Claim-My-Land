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

package mod.gottsch.neo.claimmyland.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.neo.claimmyland.core.command.helper.WhitelistFormatter;
import mod.gottsch.neo.claimmyland.core.command.helper.WhitelistType;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.*;

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
                )
                ///// FRIENDS WHITELIST CLEAR /////
                .then(Commands.literal(CLEAR)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .executes(source -> clearFromEstate(source.getSource(),
                                        StringArgumentType.getString(source, ESTATE_NAME)))
                                .then(Commands.literal(CONFIRM)
                                        .executes(source -> clearFromEstateConfirmed(source.getSource(),
                                                StringArgumentType.getString(source, ESTATE_NAME)))
                                )
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
                                                            StringArgumentType.getString(source, ESTATE_NAME), StringArgumentType.getString(source, FRIEND_NAME));
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
                                                    StringArgumentType.getString(source, ESTATE_NAME));
                                        })
                                )
                        )
                )
                ///// FRIENDS WHITELIST CLEAR /////
                .then(Commands.literal(CLEAR)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .executes(source -> clearFromEstate(source.getSource(),
                                                StringArgumentType.getString(source, OWNER_NAME),
                                                StringArgumentType.getString(source, ESTATE_NAME)))
                                        .then(Commands.literal(CONFIRM)
                                                .executes(source -> clearFromEstateConfirmed(source.getSource(),
                                                        StringArgumentType.getString(source, OWNER_NAME),
                                                        StringArgumentType.getString(source, ESTATE_NAME)))
                                        )
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
            sendUnableToLocatePlayerMessage(source);
            return -1;
        }

        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, friendsName);
        if (friendUuid.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, friendsName);
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
            sendUnableToLocatePlayerMessage(source);
            return -1;
        }

        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, friendsName);
        if (friendUuid.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, friendsName);
            return -1;
        }

        return addToEstate(source, ownerUuid.get(), estateName, friendUuid.get());
    }

    /**
     * estate common version
     */
    private static int addToEstate(CommandSourceStack source, UUID ownerUuid, String estateName, UUID friendUuid) {
        if (ownerUuid.equals(friendUuid)) {
            failure(source, "estate.whitelist.add.same_name.failure");
            return -1;
        }
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        estate.ifPresentOrElse(action -> {
                    action.getPlayerWhitelist().add(friendUuid);
                    CommandHelper.save(source.getLevel());
                    sendSuccess(source, "estate.whitelist.add.success");
//                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.add.success")).withStyle(ChatFormatting.GREEN), false);
                },
                () -> failure(source, "estate.whitelist.add.failure")
//                () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.add.failure")).withStyle(ChatFormatting.RED), false)
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
                sendUnableToLocatePlayerMessage(source, friendsName);
            }
        } else {
            sendUnableToLocatePlayerMessage(source);
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
                sendUnableToLocatePlayerMessage(source, friendsName);
            }
        } else {
            sendUnableToLocatePlayerMessage(source, ownerName);
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
                        save(source.getLevel());
                        sendSuccess(source, "estate.whitelist.remove.success");
//                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.remove.success")).withStyle(ChatFormatting.GREEN), false);
                    } else {
                        failure(source, "estate.whitelist.remove.failure");
//                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.remove.failure")).withStyle(ChatFormatting.RED), false);
                    }
                },
                () -> failure(source, "estate.whitelist.remove.failure")
//                () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.remove.failure")).withStyle(ChatFormatting.RED), false)
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
            sendUnableToLocatePlayerMessage(source);
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
            sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate common version
     */
    public static int listFromEstate(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        estate.ifPresentOrElse(action -> {
                    List<Component> messages = WhitelistFormatter
                            .formatStandAlonePlayerWhitelist(source.getLevel(), action.getPlayerWhitelist(), "PLAYER WHITELIST - " + estateName, action.getId(), estateName, null);

                    sendLines(source, messages);
//                    messages.forEach(component -> {
//                        source.sendSuccess(() -> component, false);
//                    });
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
                () -> failure(source, "estate.whitelist.list.failure")
//                () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.whitelist.list.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }

    // ===== CLEAR =====

    /** estate player version — sends confirmation prompt */
    public static int clearFromEstate(CommandSourceStack source, String estateName) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return clearFromEstateInternal(source, playerUuid.get(), estateName, false, null);
        }
        sendUnableToLocatePlayerMessage(source);
        return -1;
    }

    /** estate ops version — sends confirmation prompt */
    public static int clearFromEstate(CommandSourceStack source, String ownerName, String estateName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return clearFromEstateInternal(source, ownerUuid.get(), estateName, false, ownerName);
        }
        sendUnableToLocatePlayerMessage(source, ownerName);
        return -1;
    }

    /** estate player version — actually clears after confirmation */
    public static int clearFromEstateConfirmed(CommandSourceStack source, String estateName) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return clearFromEstateInternal(source, playerUuid.get(), estateName, true, null);
        }
        sendUnableToLocatePlayerMessage(source);
        return -1;
    }

    /** estate ops version — actually clears after confirmation */
    public static int clearFromEstateConfirmed(CommandSourceStack source, String ownerName, String estateName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return clearFromEstateInternal(source, ownerUuid.get(), estateName, true, ownerName);
        }
        sendUnableToLocatePlayerMessage(source, ownerName);
        return -1;
    }

    private static int clearFromEstateInternal(CommandSourceStack source, UUID ownerUuid, String estateName, boolean confirmed, @Nullable String ownerName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        estate.ifPresentOrElse(action -> {
            java.util.Set<UUID> whitelist = action.getPlayerWhitelist();
            if (!confirmed) {
                List<Component> lines = WhitelistFormatter.formatClearConfirmation(estateName, WhitelistType.FRIENDS, whitelist.size(), ownerName);
                sendLines(source, lines);
            } else if (whitelist.isEmpty()) {
                sendSuccess(source, "estate.whitelist.clear.no_change", "estate.whitelist.clear.no_change.body", estateName);
            } else {
                whitelist.clear();
                CommandHelper.save(source.getLevel());
                sendSuccess(source, "estate.friends.clear.success", "estate.friends.clear.success.body", estateName);
            }
        }, () -> failure(source, "estate.whitelist.add.failure"));
        return 1;
    }
}
