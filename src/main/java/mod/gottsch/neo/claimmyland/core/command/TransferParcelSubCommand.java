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
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.*;

/**
 * @author by Mark Gottschling on 2/22/2026
 */
public class TransferParcelSubCommand implements SubCommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(TRANSFER)

                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_PARCEL_NAMES)
                                .then(Commands.argument(NEW_OWNER_NAME, StringArgumentType.string())
                                        .suggests(PLAYER_NAMES)
                                        .executes(source -> {
                                            return transferParcel(source.getSource(),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    StringArgumentType.getString(source, PARCEL_NAME),
                                                    StringArgumentType.getString(source, NEW_OWNER_NAME));
                                        })
                                )
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(TRANSFER)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                                .suggests(OPS_OWNER_ESTATE_PARCEL_NAMES)
                                                .then(Commands.argument(NEW_OWNER_NAME, StringArgumentType.string())
                                                        .suggests(PLAYER_NAMES)
                                                        .executes(source -> {
                                                            return transferParcel(source.getSource(),
                                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                                    StringArgumentType.getString(source, PARCEL_NAME),
                                                                    StringArgumentType.getString(source, NEW_OWNER_NAME));
                                                        })
                                                )
                                        )
                                )
                        )
                );
    }

    public static int transferParcel(CommandSourceStack source, String estateName, String parcelName, String newOwnerName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return transferParcel(source, player.getScoreboardName(), estateName, parcelName, newOwnerName);
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred transferring parcel:", e);
            unexpectedError(source);
            return 0;
        }
    }

    public static int transferParcel(CommandSourceStack source, String ownerName, String estateName, String parcelName, String newOwnerName) {
        Optional<UUID> owner = CommandHelper.getPlayerUuid(source, ownerName);
        if (owner.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        Optional<Parcel> parcel = CommandHelper.findParcelByOwnerEstate(source, owner.get(), estateName, parcelName);
        if (parcel.isEmpty()) {
            failure(source, "parcel.unable_to_locate");
            return -1;
        }

        if (StringUtils.isBlank(newOwnerName)) {
            sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        Optional<UUID> newOwner = CommandHelper.getPlayerUuid(source, newOwnerName);
        if (newOwner.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, newOwnerName);
            return -1;
        }

        try {
            ParcelRegistry.transferParcelOwnership(source.getLevel(), parcel.get(), newOwner.get());
        } catch(Exception e) {
            failure(source, "parcel.transfer.failure");
            return -1;
        }

        sendLines(source,
                CommandResponseFormatter.formatParcelOwnershipTransferred(
                        estateName,
                        parcelName,
                        parcel.get().getId(),
                        ownerName,
                        newOwnerName));

        CommandHelper.save(source.getLevel());
        return 1;
    }
}
