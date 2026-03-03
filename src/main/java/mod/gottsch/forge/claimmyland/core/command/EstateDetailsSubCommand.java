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
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.EstateDisplayFormatter;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 2/18/2026
 */
public class EstateDetailsSubCommand implements SubCommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(DETAILS)
                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .executes(source -> {
                            return details(source.getSource(),
                                    StringArgumentType.getString(source, ESTATE_NAME));
                        })
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(DETAILS)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_ESTATE_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_ESTATE_NAMES)
                                .executes(source -> {
                                    return details(source.getSource(),
                                            StringArgumentType.getString(source, OWNER_NAME),
                                            StringArgumentType.getString(source, ESTATE_NAME));
                                })
                        )
                );
    }

    public static int details(CommandSourceStack source, String estateName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return details(source, player.getUUID(), estateName);
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred detailing estate:", e);
            CommandHelper.failure(source, "unexpected_error");
        }
        return 1;
    }

    // ops version
    public static int details(CommandSourceStack source, String ownerName, String estateName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }
        return details(source, ownerUuid.get(), estateName);
    }

    // common version
    public static int details(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> optionalEstate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        if (optionalEstate.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.details.failure"))
                    .withStyle(ChatFormatting.RED), false);
            return -1;
        }
        List<Component> messages = EstateDisplayFormatter.formatEstateDetails(source.getLevel(), optionalEstate.get());
        messages.forEach(component -> {
            source.sendSuccess(() -> component, false);
        });
        return 1;
    }
}
