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
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.EstateDisplayFormatter;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 2/18/2026
 */
public class ListEstateSubCommand implements SubCommand {

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(LIST)
                .executes(source -> {
                    return list(source.getSource());
                });

    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(LIST)
                .then(Commands.literal(BY_OWNER)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_ESTATE_OWNER_NAMES)
                                .executes(source -> {
                                    return list(source.getSource(),
                                            StringArgumentType.getString(source, OWNER_NAME));
                                })
                        )
                );
    }

    private static int list(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return list(source, player.getUUID(), false);
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred listing estates:", e);
            CommandHelper.failure(source, "unexpected_error");
        }
        return 1;
    }

    // ops version
    private static int list(CommandSourceStack source, String ownerName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return list(source, ownerUuid.get(), true);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    private static int list(CommandSourceStack source, UUID ownerUuid, boolean isOps) {
        List<Component> messages = new ArrayList<>();

        EstateDisplayFormatter.formatEstateList(source.getLevel(), messages, EstateRegistry.getByOwner(ownerUuid), isOps);

        messages.forEach(component -> {
            source.sendSuccess(() -> component, false);
        });
        return 1;
    }
}
