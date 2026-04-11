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
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.neo.claimmyland.core.command.helper.CommandResponseFormatter;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.network.CMLNetwork;
import mod.gottsch.neo.claimmyland.core.registry.EstateRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.*;

/**
 * @author by Mark Gottschling on 2/19/2026
 */
public class RenameEstateSubCommand implements SubCommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(RENAME)
                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .then(Commands.argument(NEW_NAME, StringArgumentType.string())
                                .executes(source -> {
                                    return rename(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME),
                                            StringArgumentType.getString(source, NEW_NAME));
                                })
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(RENAME)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_ESTATE_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(NEW_NAME, StringArgumentType.string())
                                        .executes(source -> {
                                            return rename(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    StringArgumentType.getString(source, NEW_NAME));
                                        })
                                )
                        )
                );
    }

    public static int rename(CommandSourceStack source, String estateName, String newName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return rename(source, player.getScoreboardName(), estateName, newName);
        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred renaming estate:", e);
            unexpectedError(source);
            return 0;
        }
    }

    public static int rename(CommandSourceStack source, String ownerName, String estateName, String newName) {
        Optional<UUID> player = CommandHelper.getPlayerUuid(source, ownerName);
        if (player.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        Set<Estate> estates = EstateRegistry.findByOwner(player.get());
        Optional<Estate> estate = estates.stream().filter(est -> est.getName().equalsIgnoreCase(estateName)).findFirst();

        if (estate.isEmpty()) {
            failure(source, "estate.rename.failure");
            return -1;
        }

        if (EstateRegistry.hasName(newName)) {
            failure(source, "estate.rename.exists.failure");
            return -1;
        }

        String oldName = estate.get().getName();
        UUID estateId = estate.get().getId();

        estate.get().setName(newName);

        estate.get().findParcels().forEach(parcel ->
                CMLNetwork.syncParcelToTrackingPlayers(source.getLevel(), parcel));

        // TRACKING_CHUNK excludes the executing player — send directly to them.
        ServerPlayer serverPlayer = source.getLevel().getServer().getPlayerList().getPlayer(player.get());
        if (serverPlayer != null) {
            estate.get().findParcels().forEach(parcel ->
                    CMLNetwork.syncParcelToPlayer(source.getLevel(), serverPlayer, parcel));
        }

        sendLines(source,
                CommandResponseFormatter.formatEstateRenamed(oldName, newName, estateId));

        save(source.getLevel());

        return 1;
    }
}
