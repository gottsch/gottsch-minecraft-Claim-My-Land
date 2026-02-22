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
import mod.gottsch.forge.claimmyland.core.command.helper.WhitelistType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.item.ItemArgument;

/**
 * @author by Mark Gottschling on 2/20/2026
 */
public class BlockWhitelistSubCommand extends WhitelistSubCommand {

    public LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext buildContext, WhitelistType type) {
        return Commands.literal(type.getCommand())
                ///// WHITELIST ADD /////
                .then(Commands.literal(ADD)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(BLOCK, BlockStateArgument.block(buildContext))
                                        .executes(source -> {
                                            return addToEstate(source.getSource(),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    BlockStateArgument.getBlock(source, BLOCK),
                                                    WhitelistType.BLOCK);
                                        })
                                )
                        )
                )

                ///// WHITELIST REMOVE /////
                .then(Commands.literal(REMOVE)

                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(BLOCK_NAME, ResourceLocationArgument.id())
                                        .suggests(CURRENT_BLOCKS)
                                        .executes(source -> {
                                            return removeFromEstate(source.getSource(),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    ResourceLocationArgument.getId(source, BLOCK_NAME),
                                                    WhitelistType.BLOCK);
                                        })
                                )

                        )
                )
                ///// WHITELIST LIST /////
                .then(Commands.literal(LIST)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .executes(source -> {
                                    return listFromEstate(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME),
                                            WhitelistType.BLOCK);
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildOps(CommandBuildContext buildContext, WhitelistType type) {
        return Commands.literal(type.getCommand())
                ///// BLOCK WHITELIST ADD /////
                .then(Commands.literal(ADD)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(BLOCK, BlockStateArgument.block(buildContext))
                                                .executes(source -> {
                                                    return addToEstate(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, ESTATE_NAME),
                                                            ItemArgument.getItem(source, BLOCK),
                                                            type);
                                                })
                                        )
                                )
                        )
                )
                ///// BLOCK WHITELIST REMOVE /////
                .then(Commands.literal(REMOVE)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(TAG_NAME, ResourceLocationArgument.id())
                                                .suggests(OPS_CURRENT_BLOCKS)
                                                .executes(source -> {
                                                    return removeFromEstate(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, ESTATE_NAME),
                                                            ResourceLocationArgument.getId(source, TAG_NAME),
                                                            WhitelistType.BLOCK);
                                                })
                                        )
                                )
                        )
                )
                ///// BLOCK WHITELIST LIST /////
                .then(Commands.literal(LIST)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .executes(source -> {
                                            return listFromEstate(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    WhitelistType.BLOCK);
                                        })
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
