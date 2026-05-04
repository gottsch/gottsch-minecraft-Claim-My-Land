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
import mod.gottsch.neo.claimmyland.core.command.helper.WhitelistType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;

/**
 * Whitelist sub-command for entity spawn types (by resource ID, e.g. "create:contraption").
 *
 * ADD accepts a resource-location argument so the player can type any registered entity
 * type ID directly — including modded entities that are never present in the world as
 * selectable targets.
 *
 * @author by Mark Gottschling on 2/20/2026
 */
public class EntitySpawnWhitelistSubCommand extends WhitelistSubCommand {

    public LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext buildContext, WhitelistType type) {
        return Commands.literal(type.getCommand())
                ///// ENTITY WHITELIST ADD /////
                .then(Commands.literal(ADD)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(ENTITY, ResourceLocationArgument.id())
                                        .suggests(ALL_ENTITY_TYPES)
                                        .executes(source -> addToEstate(source.getSource(),
                                                StringArgumentType.getString(source, ESTATE_NAME),
                                                ResourceLocationArgument.getId(source, ENTITY),
                                                WhitelistType.ENTITY))
                                )
                        )
                )
                ///// ENTITY WHITELIST REMOVE /////
                .then(Commands.literal(REMOVE)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(ENTITY, ResourceLocationArgument.id())
                                        .suggests(CURRENT_ENTITIES)
                                        .executes(source -> removeFromEstate(source.getSource(),
                                                StringArgumentType.getString(source, ESTATE_NAME),
                                                ResourceLocationArgument.getId(source, ENTITY),
                                                WhitelistType.ENTITY))
                                )
                        )
                )
                ///// ENTITY WHITELIST LIST /////
                .then(Commands.literal(LIST)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .executes(source -> listFromEstate(source.getSource(),
                                        StringArgumentType.getString(source, ESTATE_NAME),
                                        WhitelistType.ENTITY))
                        )
                )
                ///// ENTITY WHITELIST CLEAR /////
                .then(Commands.literal(CLEAR)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .executes(source -> clearFromEstate(source.getSource(),
                                        StringArgumentType.getString(source, ESTATE_NAME),
                                        WhitelistType.ENTITY))
                                .then(Commands.literal(CONFIRM)
                                        .executes(source -> clearFromEstateConfirmed(source.getSource(),
                                                StringArgumentType.getString(source, ESTATE_NAME),
                                                WhitelistType.ENTITY))
                                )
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildOps(CommandBuildContext buildContext, WhitelistType type) {
        return Commands.literal(type.getCommand())
                ///// ENTITY WHITELIST ADD /////
                .then(Commands.literal(ADD)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(ENTITY, ResourceLocationArgument.id())
                                                .suggests(ALL_ENTITY_TYPES)
                                                .executes(source -> addToEstate(source.getSource(),
                                                        StringArgumentType.getString(source, OWNER_NAME),
                                                        StringArgumentType.getString(source, ESTATE_NAME),
                                                        ResourceLocationArgument.getId(source, ENTITY),
                                                        WhitelistType.ENTITY))
                                        )
                                )
                        )
                )
                ///// ENTITY WHITELIST REMOVE /////
                .then(Commands.literal(REMOVE)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(ENTITY, ResourceLocationArgument.id())
                                                .suggests(OPS_CURRENT_ENTITIES)
                                                .executes(source -> removeFromEstate(source.getSource(),
                                                        StringArgumentType.getString(source, OWNER_NAME),
                                                        StringArgumentType.getString(source, ESTATE_NAME),
                                                        ResourceLocationArgument.getId(source, ENTITY),
                                                        WhitelistType.ENTITY))
                                        )
                                )
                        )
                )
                ///// ENTITY WHITELIST LIST /////
                .then(Commands.literal(LIST)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .executes(source -> listFromEstate(source.getSource(),
                                                StringArgumentType.getString(source, OWNER_NAME),
                                                StringArgumentType.getString(source, ESTATE_NAME),
                                                WhitelistType.ENTITY))
                                )
                        )
                )
                ///// ENTITY WHITELIST CLEAR /////
                .then(Commands.literal(CLEAR)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .executes(source -> clearFromEstate(source.getSource(),
                                                StringArgumentType.getString(source, OWNER_NAME),
                                                StringArgumentType.getString(source, ESTATE_NAME),
                                                WhitelistType.ENTITY))
                                        .then(Commands.literal(CONFIRM)
                                                .executes(source -> clearFromEstateConfirmed(source.getSource(),
                                                        StringArgumentType.getString(source, OWNER_NAME),
                                                        StringArgumentType.getString(source, ESTATE_NAME),
                                                        WhitelistType.ENTITY))
                                        )
                                )
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return null;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return null;
    }
}
