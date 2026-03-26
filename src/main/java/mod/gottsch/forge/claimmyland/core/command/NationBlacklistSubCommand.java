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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.EstateDisplayFormatter;
import mod.gottsch.forge.claimmyland.core.command.helper.WhitelistFormatter;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.NationEstate;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.*;

import static mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper.*;

/**
 * @author by Mark Gottschling on 2/20/2026
 */
public class NationBlacklistSubCommand implements SubCommand {
    private static final String BLACKLIST = "blacklist";
    private static final String PLAYER_NAME = "player_name";

    static final SuggestionProvider<CommandSourceStack> CURRENT_BLACKLIST_NAMES = (source, builder) -> {
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
        List<String> list = new ArrayList<>();

        if (ownerUuid.isPresent()) {
            Optional<Set<UUID>> friendsUuids = getNationBlacklist(source.getSource(), ownerUuid.get(), estateName);
            friendsUuids.ifPresent(uuids -> uuids.forEach(uuid -> {
                Optional<String> name = CommandHelper.getPlayerName(source.getSource(), uuid);
                name.ifPresent(list::add);
            }));
        }
        return SharedSuggestionProvider.suggest(list, builder);
    };

    static Optional<Set<UUID>> getNationBlacklist(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        if (estate.isPresent() && estate.get() instanceof NationEstate nationEstate) {
            return Optional.of(new HashSet<>(nationEstate.getPlayerBlacklist()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return
                Commands.literal(BLACKLIST)
                        ///// BLACKLIST ADD /////
                        .then(Commands.literal(ADD)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OWNER_NATION_ESTATE_NAMES)
                                        .then(Commands.argument(PLAYER_NAME, StringArgumentType.string())
                                                .suggests(PLAYER_NAMES)
                                                .executes(source -> {
                                                    return addToEstate(source.getSource(),
                                                            StringArgumentType.getString(source, ESTATE_NAME),
                                                            StringArgumentType.getString(source, PLAYER_NAME));
                                                })
                                        )
                                )
                        )
                        ///// BLACKLIST REMOVE /////
                        .then(Commands.literal(REMOVE)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OWNER_NATION_ESTATE_NAMES)
                                        .then(Commands.argument(PLAYER_NAME, StringArgumentType.string())
                                                .suggests(CURRENT_BLACKLIST_NAMES)
                                                .executes(source -> {
                                                    return removeFromEstate(source.getSource(),
                                                            StringArgumentType.getString(source, ESTATE_NAME),
                                                            StringArgumentType.getString(source, PLAYER_NAME));
                                                })
                                        )
                                )
                        )
                        ///// BLACKLIST LIST /////
                        .then(Commands.literal(LIST)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OWNER_NATION_ESTATE_NAMES)
                                        .executes(source -> {
                                            return listFromEstate(source.getSource(),
                                                    StringArgumentType.getString(source, ESTATE_NAME));
                                        })
                                )
                        );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(BLACKLIST)
                ///// BLACKLIST WHITELIST ADD /////
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
                ///// BLACKLIST WHITELIST REMOVE /////
                .then(Commands.literal(REMOVE)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(FRIEND_NAME, StringArgumentType.string())
                                                .suggests(CURRENT_BLACKLIST_NAMES)
                                                .executes(source -> {
                                                    return removeFromEstate(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, ESTATE_NAME), StringArgumentType.getString(source, FRIEND_NAME));
                                                })
                                        )
                                )
                        )
                )
                ///// BLACKLIST WHITELIST LIST /////
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
    public static int addToEstate(CommandSourceStack source, String estateName, String playerName) {
        // refactored way
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source);
        if (ownerUuid.isEmpty()) {
            sendUnableToLocatePlayerMessage(source);
            return -1;
        }

        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, playerName);
        if (friendUuid.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, playerName);
            return -1;
        }

        return addToEstate(source, ownerUuid.get(), estateName, friendUuid.get());
    }

    /*
     * ops version
     */
    public static int addToEstate(CommandSourceStack source, String ownerName, String estateName, String playerName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isEmpty()) {
            sendUnableToLocatePlayerMessage(source);
            return -1;
        }

        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, playerName);
        if (friendUuid.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, playerName);
            return -1;
        }

        return addToEstate(source, ownerUuid.get(), estateName, friendUuid.get());
    }

    /**
     * estate common version
     */
    private static int addToEstate(CommandSourceStack source, UUID ownerUuid, String estateName, UUID playerUuid) {
        if (ownerUuid.equals(playerUuid)) {
            failure(source, "estate.blacklist.add.same_name.failure");
            return -1;
        }
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        estate.ifPresentOrElse(estate1 -> {
                    if (!(estate1 instanceof NationEstate nationEstate)) {
                        failure(source, "estate.blacklist.unable_to_locate");
                        return;
                    }
                    nationEstate.getPlayerBlacklist().add(playerUuid);
                    CommandHelper.save(source.getLevel());
                    sendSuccess(source, "estate.blacklist.add.success");
                },
                () -> failure(source, "estate.blacklist.add.failure")
        );
        return 1;
    }

    /**
     * estate player version
     */
    public static int removeFromEstate(CommandSourceStack source, String estateName, String BLACKLISTName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source);
        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, BLACKLISTName);

        if (ownerUuid.isPresent()) {
            if (friendUuid.isPresent()) {
                return removeFromEstate(source,ownerUuid.get(), estateName, friendUuid.get());
            } else {
                sendUnableToLocatePlayerMessage(source, BLACKLISTName);
            }
        } else {
            sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     * estate ops version
     */
    public static int removeFromEstate(CommandSourceStack source, String ownerName, String estateName, String playerName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        Optional<UUID> BLACKLISTUuid = CommandHelper.getPlayerUuid(source, playerName);
        if (ownerUuid.isPresent()) {
            if (BLACKLISTUuid.isPresent()) {
                return removeFromEstate(source, ownerUuid.get(), estateName, BLACKLISTUuid.get());
            } else {
                sendUnableToLocatePlayerMessage(source, playerName);
            }
        } else {
            sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * estate common version
     */
    public static int removeFromEstate(CommandSourceStack source, UUID ownerUuid, String estateName, UUID playerUuid) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        estate.ifPresentOrElse(action -> {
                    if (!(action instanceof NationEstate nationEstate)) {
                        failure(source, "estate.blacklist.unable_to_locate");
                        return;
                    }

                    if (nationEstate.getPlayerBlacklist().remove(playerUuid)) {
                        save(source.getLevel());
                        sendSuccess(source, "estate.blacklist.remove.success");
                    } else {
                        failure(source, "estate.blacklist.remove.failure");
                    }
                },
                () -> failure(source, "estate.blacklist.remove.failure")
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
                    if (!(action instanceof NationEstate nationEstate)) {
                        failure(source, "estate.blacklist.unable_to_locate");
                        return;
                    }

                    List<Component> messages = WhitelistFormatter
                            .formatStandAlonePlayerWhitelist(source.getLevel(), nationEstate.getPlayerBlacklist(), "PLAYER BLACKLIST - " + estateName, action.getId(), estateName);

                    sendLines(source, messages);
                },
                () -> failure(source, "estate.blacklist.list.failure")
        );
        return 1;
    }
}
