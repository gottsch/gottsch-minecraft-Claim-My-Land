/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
 *
 * All rights reserved.
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
 */
package mod.gottsch.forge.claimmyland.core.command;

import com.mojang.brigadier.CommandDispatcher;
import mod.gottsch.forge.claimmyland.core.command.helper.WhitelistType;
import mod.gottsch.forge.claimmyland.core.config.Config;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 *
 * @author Mark Gottschling on Mar 19, 2024
 *
 */
public class OpsCommand {
    private static final String CML_OPS = "cml-ops";
    private static final String PARCEL = "parcel";
    private static final String ESTATE = "estate";
    private static final String WHITELIST = "whitelist";

    /*
     * cml-ops
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher
                .register(Commands.literal(CML_OPS).requires(source -> {
                                    return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get()); // only ops can use command
                                })
                                ///// BACKUP /////
                                .then(new BackupSubCommand().buildOps())

                                ///// DEED TOP-LEVEL OPTION /////
                                ///// NEW DEED /////
                                .then(new GenerateDeedSubCommand().buildOps())

                                ///// PARCEL TOP-LEVEL OPTION /////
                                .then(Commands.literal(PARCEL).requires(source -> source.hasPermission(Config.SERVER.general.opsPermissionLevel.get()))
                                        ///// LIST OPTION /////
                                        .then(new ListParcelsSubCommand().buildOps())

                                        ///// RELINQUISH OPTION /////
                                        .then(new RelinquishParcelSubCommand().buildOps())

                                        ///// DEMOLISH /////
                                        .then(new DemolishParcelSubCommand().buildOps())

                                        ///// RENAME PARCEL /////
                                        .then(new RenameParcelSubCommand().buildOps())

                                        ///// TRANSFER /////
                                        .then(new TransferParcelSubCommand().buildOps())

                                        ///// CLEAR /////
                                        .then(new ClearSubCommand().buildOps())
                                )
                                ///// ESTATE TOP-LEVEL OPTION /////
                                .then(Commands.literal(ESTATE).requires(source -> source.hasPermission(Config.SERVER.general.opsPermissionLevel.get()))
                                        ///// ACCESS TYPE /////
                                        .then(new AccessTypeSubCommand().buildOps())

                                        /// // LIST ESTATES /////
                                        .then(new ListEstateSubCommand().buildOps())
                                        ///// DETAILS OPTION /////
                                        .then(new EstateDetailsSubCommand().buildOps())
                                        ///// RENAME ESTATE /////
                                        .then(new RenameEstateSubCommand().buildOps())

                                        ///// REMOVE ESTATE /////
                                        // removes Estate from the world without returning Deeds
                                        .then(new RemoveEstateSubCommand().buildOps())

                                        ///// RELINQUISH /////
                                        .then(new RelinquishEstateSubCommand().buildOps())

                                        ///// TRANSFER /////
                                        .then(new TransferEstateSubCommand().buildOps())

                                        ///// PREVENT FIRE SPREAD /////
                                        .then(new PreventFireSpreadSubCommand().buildOps())

                                        ///// JOIN (ANNEX) OPTION /////
                                        .then(new JoinSubCommand().buildOps())

                                        ///// SPLIT (CEDE) OPTION /////
                                        .then(new SplitSubCommand().buildOps())
                                        ///// WHITELIST OPTION /////
                                        .then(Commands.literal(WHITELIST)
                                                ///// BLOCK TAG WHITELIST OPTION /////
                                                .then(new TagWhitelistSubCommand().buildOps(buildContext, WhitelistType.BLOCK_TAG))
                                                ///// BLOCK WHITELIST OPTION /////
                                                .then(new BlockWhitelistSubCommand().buildOps(buildContext, WhitelistType.BLOCK))
                                                ///// ITEM TAG WHITELIST OPTION /////
                                                .then(new TagWhitelistSubCommand().buildOps(buildContext, WhitelistType.ITEM_TAG))
                                                ///// ITEM WHITELIST OPTION /////
                                                .then(new ItemWhitelistSubCommand().buildOps(buildContext, WhitelistType.ITEM))
                                                .then(new TagWhitelistSubCommand().build(buildContext, WhitelistType.ENTITY_TAG))
                                                .then(new EntitySpawnWhitelistSubCommand().build(buildContext, WhitelistType.ENTITY))
                                                .then(new FriendsWhitelistSubCommand().build())
                                        )
                                        /// // NATION BLACKLIST /////
                                        .then(new NationBlacklistSubCommand().buildOps())
                                        ///// CLEAR /////
                                        // performs same action as parcel > clear
                                        .then(new ClearSubCommand().buildOps())
                                        ///// CLAIMED_BY TOP-LEVEL OPTION /////
                                        .then(new ClaimedBySubCommand().build())
                                )
                );
    } // end of method

}