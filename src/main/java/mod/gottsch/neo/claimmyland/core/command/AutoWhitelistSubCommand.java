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

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.*;

/**
 * Toggle the auto-whitelist feature for an estate.
 * When enabled, any player who logs in is automatically added to this estate's
 * player whitelist.
 *
 * @author Mark Gottschling on 2026-05-02
 */
public class AutoWhitelistSubCommand implements SubCommand {

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(AUTO)
                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .then(Commands.argument(AUTO, BoolArgumentType.bool())
                                .executes(source -> {
                                    return autoWhitelist(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME),
                                            BoolArgumentType.getBool(source, AUTO));
                                })
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(AUTO)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_ESTATE_NAMES)
                                .then(Commands.argument(AUTO, BoolArgumentType.bool())
                                        .executes(source -> {
                                            return autoWhitelist(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    BoolArgumentType.getBool(source, AUTO));
                                        })
                                )
                        )
                );
    }

    // player version — delegates to the ops version using the calling player's name
    public int autoWhitelist(CommandSourceStack source, String estateName, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        return autoWhitelist(source, player.getScoreboardName(), estateName, enabled);
    }

    // ops version
    public int autoWhitelist(CommandSourceStack source, String ownerName, String estateName, boolean enabled) {
        try {
            Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
            if (ownerUuid.isEmpty()) {
                sendUnableToLocatePlayerMessage(source, ownerName);
                return -1;
            }

            Optional<Estate> optionalEstate = CommandHelper.getEstateByOwner(source, ownerUuid.get(), estateName);
            if (optionalEstate.isEmpty()) {
                failure(source, "estate.unable_to_locate");
                return -1;
            }

            Estate estate = optionalEstate.get();

            // validate ownership
            if (!estate.getOwnerId().equals(ownerUuid.get())) {
                failure(source, "estate.not_owner");
                return -1;
            }

            // idempotency check
            if (estate.isAutoWhitelist() == enabled) {
                sendSuccess(source, "estate.whitelist.auto.no_change", "estate.whitelist.auto.no_change.body",
                        estate.getName(), Boolean.toString(enabled));
                return 1;
            }

            estate.setAutoWhitelist(enabled);
            save(source.getLevel());

            sendSuccess(source, "estate.whitelist.auto.success", "estate.whitelist.auto.success.body",
                    estate.getName(), Boolean.toString(enabled));

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred changing estate autoWhitelist:", e);
            unexpectedError(source);
        }
        return 1;
    }
}
