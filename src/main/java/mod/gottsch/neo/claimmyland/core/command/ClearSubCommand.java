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
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.save;
import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.sendSuccess;

/**
 * @author by Mark Gottschling on 3/4/2026
 */
public class ClearSubCommand implements SubCommand {
    public static final String CLEAR = "clear";

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return null;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(CLEAR)
                .executes(source -> {
                    return clearAllParcels(source.getSource());
                })
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NAMES)
                        .executes(source -> {
                            return clearOwnerParcels(source.getSource(), StringArgumentType.getString(source, OWNER_NAME));
                        })
                );

    }

    public static int clearOwnerParcels (CommandSourceStack source, String ownerName){
        Optional<UUID> player = CommandHelper.getPlayerUuid(source, ownerName);

        if (player.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        // remove all properties from player
        ParcelRegistry.removeParcel(source.getLevel(), player.get());
        sendSuccess(source, "parcel.clear.success");
        save(source.getLevel());
        return 1;
    }

    public static int clearAllParcels (CommandSourceStack source){
        // TODO add an undo/restore command
        ParcelRegistry.clear();
        sendSuccess(source, "parcel.clear.success");
        save(source.getLevel());
        return 1;
    }
}
