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

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper.*;

/**
 * @author Mark Gottschling on <date>
 */
public class PreventFireSpreadSubCommand implements SubCommand {
    private String PREVENT_FIRE_SPREAD = "preventFireSpread";

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(PREVENT_FIRE_SPREAD)
                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .then(Commands.argument(PREVENT_FIRE_SPREAD, BoolArgumentType.bool())
                                .executes(source -> {
                                    return preventFireSpread(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME),
                                            BoolArgumentType.getBool(source, PREVENT_FIRE_SPREAD));
                                })
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(PREVENT_FIRE_SPREAD)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_ESTATE_NAMES)
                                .then(Commands.argument(PREVENT_FIRE_SPREAD, BoolArgumentType.bool())
                                        .executes(source -> {
                                            return preventFireSpread(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    BoolArgumentType.getBool(source, PREVENT_FIRE_SPREAD));
                                        })
                                )
                        )
                );
    }

    // player version
    public int preventFireSpread(CommandSourceStack source, String estateName, boolean preventFireSpread) {
        ServerPlayer player = source.getPlayer();
        return preventFireSpread(source, player.getScoreboardName(), estateName, preventFireSpread);
    }

    public int preventFireSpread(CommandSourceStack source, String ownerName, String estateName, boolean preventFireSpread) {
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

            estate.setPreventFireSpread(preventFireSpread);
            save(source.getLevel());

            sendSuccess(source, "estate.prevent_fire_spread.success");

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred changing estate preventFireSpread:", e);
            unexpectedError(source);
        }
        return 1;
    }
}