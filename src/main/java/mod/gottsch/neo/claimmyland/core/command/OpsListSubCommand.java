/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
package mod.gottsch.neo.claimmyland.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.neo.claimmyland.core.command.helper.OpsListFormatter;
import mod.gottsch.neo.claimmyland.core.config.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.neo.claimmyland.core.command.SubCommand.*;
import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.*;

/**
 * /cml-ops opslist management subcommand. Adds, removes, and lists CML ops
 * stored in {@link Config.General#opsList}. Replaces direct editing of the
 * {@code claimmyland-server.toml} file for ops list management.
 *
 * <p>The config value is a {@code List<? extends String>} of player UUIDs
 * (as strings). Display is name-first with a UUID fallback when the Mojang
 * lookup fails.</p>
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code /cml-ops opslist list} — show current ops with interactive icons</li>
 *   <li>{@code /cml-ops opslist add <player>} — add a player by name</li>
 *   <li>{@code /cml-ops opslist remove <player>} — remove a player by name</li>
 * </ul>
 * </p>
 *
 * <p>Self-removal is permitted (matches vanilla {@code /deop} semantics).</p>
 *
 * @author Mark Gottschling on Apr 19, 2026
 */
public class OpsListSubCommand {
    public static final String OPSLIST = "ops_list";
    public static final String PLAYER_NAME = "player_name";

    public static final SuggestionProvider<CommandSourceStack> OPS_LIST_NAMES =
            (context, builder) -> {
                ServerLevel level = context.getSource().getLevel();
                Config.SERVER.general.opsList.get().forEach(uuidString -> {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        getPlayerName(context.getSource(), uuid).ifPresent(builder::suggest);
                    } catch (IllegalArgumentException ignored) {
                        // malformed UUID in config — skip
                    }
                });
                return builder.buildFuture();
            };

    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(OPSLIST)
                ///// OPSLIST ADD /////
                .then(Commands.literal(ADD)
                        .then(Commands.argument(PLAYER_NAME, StringArgumentType.string())
                                .suggests(PLAYER_NAMES)
                                .executes(source -> addOp(source.getSource(),
                                        StringArgumentType.getString(source, PLAYER_NAME)))
                        )
                )
                ///// OPSLIST REMOVE /////
                .then(Commands.literal(REMOVE)
                        .then(Commands.argument(PLAYER_NAME, StringArgumentType.string())
                                .suggests(OPS_LIST_NAMES)
                                .executes(source -> removeOp(source.getSource(),
                                        StringArgumentType.getString(source, PLAYER_NAME)))
                        )
                )
                ///// OPSLIST LIST /////
                .then(Commands.literal(LIST)
                        .executes(source -> listOps(source.getSource()))
                );
    }

    /**
     * Adds the resolved UUID of {@code playerName} to {@link Config.General#opsList}
     * if not already present. Resolution uses the extended CommandHelper lookup
     * (online → PlayerRegistry → Mojang cache).
     */
    public static int addOp(CommandSourceStack source, String playerName) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source, playerName);
        if (playerUuid.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, playerName);
            return -1;
        }

        String uuidString = playerUuid.get().toString();
        List<? extends String> current = Config.SERVER.general.opsList.get();

        if (current.contains(uuidString)) {
            sendFailure(source,
                    "opslist.add.already_op",
                    "opslist.add.already_op.body",
                    playerName);
            return -1;
        }

        /*
         * Build a fresh mutable List<String> — the config getter returns
         * List<? extends String>, which is read-only at the generic level.
         * The constructor copy is safe because each element is a String.
         */
        List<String> updated = new ArrayList<>(current);
        updated.add(uuidString);
        Config.SERVER.general.opsList.set(updated);
        saveServerConfig();

        sendSuccess(source,
                "opslist.add.success",
                "opslist.add.success.body",
                playerName);

        return 1;
    }

    /**
     * Removes the resolved UUID of {@code playerName} from
     * {@link Config.General#opsList} if present. Self-removal is allowed.
     */
    public static int removeOp(CommandSourceStack source, String playerName) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source, playerName);
        if (playerUuid.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, playerName);
            return -1;
        }

        String uuidString = playerUuid.get().toString();
        List<? extends String> current = Config.SERVER.general.opsList.get();

        if (!current.contains(uuidString)) {
            sendFailure(source,
                    "opslist.remove.not_op",
                    "opslist.remove.not_op.body",
                    playerName);
            return -1;
        }

        List<String> updated = new ArrayList<>(current);
        updated.remove(uuidString);
        Config.SERVER.general.opsList.set(updated);
        saveServerConfig();

        sendSuccess(source,
                "opslist.remove.success",
                "opslist.remove.success.body",
                playerName);

        return 1;
    }

    /**
     * Formats the current ops list and sends it to {@code source}. Invalid
     * UUID strings in the config are skipped with a warning log.
     */
    public static int listOps(CommandSourceStack source) {
        List<? extends String> current = Config.SERVER.general.opsList.get();

        List<UUID> uuids = current.stream()
                .map(OpsListSubCommand::parseUuidOrNull)
                .filter(Objects::nonNull)
                .toList();

        List<Component> messages = OpsListFormatter.formatOpsList(source.getLevel(), uuids);
        sendLines(source, messages);
        return 1;
    }

    private static UUID parseUuidOrNull(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            ClaimMyLand.LOGGER.warn("invalid UUID in opsList config (skipped): {}", s);
            return null;
        }
    }
}