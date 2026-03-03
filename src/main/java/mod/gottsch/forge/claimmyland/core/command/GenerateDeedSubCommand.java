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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author by Mark Gottschling on 3/2/2026
 */
public class GenerateDeedSubCommand implements SubCommand {
    public static final String DEED = "deed";

    private static final SuggestionProvider<CommandSourceStack> DEED_TYPES = (source, builder) -> {
        return SharedSuggestionProvider.suggest(Stream.of(ParcelType.CITIZEN)
                .map(ParcelType::getSerializedName), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> OPS_DEED_TYPES = (source, builder) -> {
        return SharedSuggestionProvider.suggest(Arrays.stream(ParcelType.values()).filter(p -> p != ParcelType.ZONE).map(ParcelType::getSerializedName), builder);
    };

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(DEED)
                .then(Commands.literal(NEW)
                                .then(Commands.literal(ParcelType.CITIZEN.toString())
                                        .then(new NationalizedDeedSubCommand().build(ParcelType.CITIZEN))
                                )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {

        return Commands.literal(DEED).requires(source -> {
                    return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
                })

                .then(Commands.literal(NEW)
                        .then(Commands.literal(ParcelType.PLAYER.toString())
                                .then(new NationlessDeedSubCommand().buildOps(ParcelType.PLAYER))
                        )

                        .then(Commands.literal(ParcelType.CITIZEN.toString())
                                .then(new NationalizedDeedSubCommand().buildOps(ParcelType.CITIZEN))
                        )

                        .then(Commands.literal(ParcelType.NATION.toString())
                                .then(new NationlessDeedSubCommand().buildOps(ParcelType.NATION))
                        )

                );
    }
}
