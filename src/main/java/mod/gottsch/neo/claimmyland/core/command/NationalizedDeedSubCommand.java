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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.estate.NationEstate;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.claimmyland.core.registry.EstateRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * @author by Mark Gottschling on 3/2/2026
 */
public class NationalizedDeedSubCommand extends DeedsSubCommand {
    static final String NATION_ESTATE_NAME = "nation_estate_name";

    private static final SuggestionProvider<CommandSourceStack> OPS_NATION_ESTATE_NAMES = (source, builder) -> {
        List<String> names = EstateRegistry.getAll().stream()
                .filter(Estate::isNation)
                .map((Estate::getName))
                .map(StringArgumentType::escapeIfRequired)
                .toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    // NOTE this is duplicated from SubCommand.
    static final SuggestionProvider<CommandSourceStack>
            OWNER_NATION_ESTATE_NAMES = (source, builder) -> {
        ServerPlayer owner = source.getSource().getPlayerOrException();
        List<String> names = EstateRegistry.findByOwner(owner.getUUID()).stream()
                .filter(estate -> estate instanceof NationEstate)
                .map((Estate::getName))
                .map(StringArgumentType::escapeIfRequired)
                .toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, String> build(ParcelType parcelType) {
        return
                Commands.argument(NATION_ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_NATION_ESTATE_NAMES)
                        .then(Commands.argument(X_SIZE, IntegerArgumentType.integer())
                                .then(Commands.argument(Y_SIZE_UP, IntegerArgumentType.integer())
                                        .then(Commands.argument(Y_SIZE_DOWN, IntegerArgumentType.integer())
                                                .then(Commands.argument(Z_SIZE, IntegerArgumentType.integer())
                                                        .executes(source -> {
                                                            return generateDeed(source.getSource(),
                                                                    parcelType,
                                                                    IntegerArgumentType.getInteger(source, X_SIZE),
                                                                    IntegerArgumentType.getInteger(source, Y_SIZE_UP),
                                                                    IntegerArgumentType.getInteger(source, Y_SIZE_DOWN),
                                                                    IntegerArgumentType.getInteger(source, Z_SIZE),
                                                                    StringArgumentType.getString(source, NATION_ESTATE_NAME)
                                                            );
                                                        })
                                                )
                                        )
                                )
                        );
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, String> buildOps(ParcelType parcelType) {
        return
                Commands.argument(NATION_ESTATE_NAME, StringArgumentType.string())
                        .suggests(OPS_NATION_ESTATE_NAMES)
                        .then(Commands.argument(X_SIZE, IntegerArgumentType.integer())
                                .then(Commands.argument(Y_SIZE_UP, IntegerArgumentType.integer())
                                        .then(Commands.argument(Y_SIZE_DOWN, IntegerArgumentType.integer())
                                                .then(Commands.argument(Z_SIZE, IntegerArgumentType.integer())
                                                        .executes(source -> {
                                                            return generateDeed(source.getSource(),
                                                                    parcelType,
                                                                    IntegerArgumentType.getInteger(source, X_SIZE),
                                                                    IntegerArgumentType.getInteger(source, Y_SIZE_UP),
                                                                    IntegerArgumentType.getInteger(source, Y_SIZE_DOWN),
                                                                    IntegerArgumentType.getInteger(source, Z_SIZE),
                                                                    StringArgumentType.getString(source, NATION_ESTATE_NAME)
                                                            );
                                                        })
                                                )
                                        )
                                )
                        );
    }
}
