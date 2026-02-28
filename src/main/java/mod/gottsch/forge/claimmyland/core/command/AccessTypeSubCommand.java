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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.NationEstateContext;
import mod.gottsch.forge.claimmyland.core.parcel.NationAccessType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 2/25/2026
 */
public class AccessTypeSubCommand implements SubCommand {
    public static final SuggestionProvider<CommandSourceStack> ACCESS_TYPES = (source, builder) -> {
        return SharedSuggestionProvider.suggest(Arrays.stream(NationAccessType.values()).map(NationAccessType::getSerializedName), builder);
    };

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("access_type")
                .then(Commands.argument(NATION_NAME, StringArgumentType.string())
                        .suggests(OWNER_NATION_ESTATE_NAMES)
                        .then(Commands.argument("access_type", StringArgumentType.string())
                                .suggests(ACCESS_TYPES)
                                .executes(source -> {
                                    return accessType(source.getSource(),
                                            StringArgumentType.getString(source, NATION_NAME),
                                            StringArgumentType.getString(source, "access_type"));
                                })
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal("access_type")
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NAMES)
                .then(Commands.argument(NATION_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NATION_ESTATE_NAMES)
                        .then(Commands.argument("access_type", StringArgumentType.string())
                                .suggests(ACCESS_TYPES)
                                .executes(source -> {
                                    return accessType(source.getSource(),
                                            StringArgumentType.getString(source, OWNER_NAME),
                                            StringArgumentType.getString(source, NATION_NAME),
                                            StringArgumentType.getString(source, "access_type"));
                                })
                        )
                )
                );
    }

    // player version
    public  int accessType(CommandSourceStack source, String nationName, String accessType) {
        ServerPlayer player = source.getPlayer();
        return accessType(source, player.getScoreboardName(), nationName, accessType);
    }

    public int accessType(CommandSourceStack source, String ownerName, String nationName, String accessTypeName) {
        try {
            Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
            if (ownerUuid.isEmpty()) {
                CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
                return -1;
            }

            Optional<Estate> optionalEstate = CommandHelper.getEstateByOwner(source, ownerUuid.get(), nationName);
            if (optionalEstate.isEmpty()) {
                CommandHelper.failure(source,"parcel.nation.unable_to_locate");
                return -1;
            }

            // get the border type
            NationAccessType accessType = NationAccessType.fromString(accessTypeName.toUpperCase());

            // players version needs to validate that the player owns the nation
            if (!optionalEstate.get().getOwnerId().equals(ownerUuid.get())) {
                CommandHelper.failure(source, "parcel.nation.not_owner");
                return -1;
            }

            ((NationEstateContext)optionalEstate.get()).setAccessType(accessType);;
            CommandHelper.save(source.getLevel());

        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred changing nation border type:", e);
            CommandHelper.unexceptedError(source);
        }
        return 1;
    }
}
