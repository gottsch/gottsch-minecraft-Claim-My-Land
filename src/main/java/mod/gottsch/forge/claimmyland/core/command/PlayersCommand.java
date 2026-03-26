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
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 *
 * @author Mark Gottschling on Oct 11, 2021
 *
 */
public class PlayersCommand {
	private static final String CML = "cml";
	private static final String PARCEL = "parcel";
	private static final String ESTATE = "estate";
	private static final String WHITELIST = "whitelist";

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
		dispatcher
				.register(Commands.literal(CML)
						.requires(source -> {
							return source.hasPermission(0);
						})
						///// DEED TOP-LEVEL OPTION /////
						.then(new GenerateDeedSubCommand().build())

						///// PARCEL TOP-LEVEL OPTION /////
						.then(Commands.literal(PARCEL)
								///// LIST OPTION /////
								.then(new ListParcelsSubCommand().build())

								///// RELINQUISH OPTION /////
								.then(new RelinquishParcelSubCommand().build())

								///// DEMOLISH /////
								.then(new DemolishParcelSubCommand().build())

								///// RENAME PARCEL /////
								.then(new RenameParcelSubCommand().build())

								///// TRANSFER /////
								.then(new TransferParcelSubCommand().build())
						) // end of parcel

						///// ESTATE TOP-LEVEL OPTION /////
						.then(Commands.literal(ESTATE)
								///// ACCESS TYPE /////
								.then(new AccessTypeSubCommand().build())
								///// LIST OPTION /////
								.then(new ListEstateSubCommand().build())
								///// DETAILS OPTION /////
								.then(new EstateDetailsSubCommand().build())
								///// RELINQUISH OPTION /////
								.then(new RelinquishEstateSubCommand().build())
								///// DEMOLISH /////
								.then(new DemolishEstateSubCommand().build())
								///// JOIN (ANNEX) OPTION /////
								.then(new JoinSubCommand().build())
								///// SPLIT (CEDE) OPTION /////
								.then(new SplitSubCommand().build())
								///// RENAME OPTION /////
								.then(new RenameEstateSubCommand().build())
								///// REMOVE ZONE ESTATE /////
								// removes Estate from the world without returning Deeds
								.then(new RemoveEstateSubCommand().build())
								///// TRANSFER /////
								.then(new TransferEstateSubCommand().build())
								///// PREVENT FIRE SPREAD /////
								.then(new PreventFireSpreadSubCommand().build())
								///// WHITELIST OPTION /////
								.then(Commands.literal(WHITELIST)
										///// BLOCK WHITELIST OPTION /////
										.then(new BlockWhitelistSubCommand().build(buildContext, WhitelistType.BLOCK))
										///// BLOCK TAG WHITELIST OPTION /////
										.then(new TagWhitelistSubCommand().build(buildContext, WhitelistType.BLOCK_TAG))
										///// ITEM WHITELIST REMOVE /////
										.then(new ItemWhitelistSubCommand().build(buildContext, WhitelistType.ITEM))
										///// ITEM TAG WHITELIST OPTION /////
										.then(new TagWhitelistSubCommand().build(buildContext, WhitelistType.ITEM_TAG))
										/// // ENTITY SPAWN /////
										.then(new EntitySpawnWhitelistSubCommand().build(buildContext, WhitelistType.ENTITY))
										/// // ENTITY SPAWN TAG /////
										.then(new TagWhitelistSubCommand().build(buildContext, WhitelistType.ENTITY_TAG))
										///// FRIENDS WHITELIST /////
										.then(new FriendsWhitelistSubCommand().build())

								) // end of whitelist
								/// // BLACK LIST /////
								.then(new NationBlacklistSubCommand().build())

						) // end of estate

						///// GIVE TOP-LEVEL OPTION /////
						.then(new GiveSubCommand().build())
						///// CLAIMED_BY TOP-LEVEL OPTION /////
						.then(new ClaimedBySubCommand().build())

				); // end of register
	}
}
