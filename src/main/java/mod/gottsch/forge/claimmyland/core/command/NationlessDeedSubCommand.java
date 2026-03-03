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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * @author by Mark Gottschling on 3/2/2026
 */
public class NationlessDeedSubCommand extends DeedsSubCommand {

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(ParcelType parcelType) {
        // NOTE there isn't a player nationless deed command
        return null;
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, Integer> buildOps(ParcelType parcelType) {
        return Commands.argument(X_SIZE, IntegerArgumentType.integer())
                .then(Commands.argument(Y_SIZE_UP, IntegerArgumentType.integer())
                        .then(Commands.argument(Y_SIZE_DOWN, IntegerArgumentType.integer())
                                .then(Commands.argument(Z_SIZE, IntegerArgumentType.integer())
                                        .executes(source -> {
                                            return generateDeed(source.getSource(),
                                                    parcelType,
                                                    IntegerArgumentType.getInteger(source, X_SIZE),
                                                    IntegerArgumentType.getInteger(source, Y_SIZE_UP),
                                                    IntegerArgumentType.getInteger(source, Y_SIZE_DOWN),
                                                    IntegerArgumentType.getInteger(source, Z_SIZE)
                                            );
                                        })
                                )

                        )
                );
    }
}
