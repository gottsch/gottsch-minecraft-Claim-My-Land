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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.WhitelistType;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

/**
 *
 * @author Mark Gottschling on Oct 11, 2021
 *
 */
public class PlayersCommand {
	private static final String CML = "cml";

	static final SuggestionProvider<CommandSourceStack> GIVABLE_ITEMS = (source, builder) -> {
		return SharedSuggestionProvider.suggest(Stream.of("border_stone", "citizen_tool", "zoning_tool"), builder);
	};

	/**
	 *
	 * @param dispatcher
	 */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
		dispatcher
				.register(Commands.literal(CML)
								.requires(source -> {
									return source.hasPermission(0);
								})
								///// DEED TOP-LEVEL OPTION /////
								.then(new GenerateDeedSubCommand().build())

								///// PARCEL TOP-LEVEL OPTION /////
								.then(Commands.literal(CommandHelper.PARCEL)
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
								.then(Commands.literal(CommandHelper.ESTATE)
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

												///// WHITELIST OPTION /////
												.then(Commands.literal(CommandHelper.WHITELIST)
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
														///// FRIENDS WHITELIST ADD /////
														.then(new FriendsWhitelistSubCommand().build())

												) // end of whitelist

								) // end of estate

								///// GIVE TOP-LEVEL OPTION /////
								.then(Commands.literal(CommandHelper.GIVE)
												.then(Commands.argument(CommandHelper.GIVE_ITEM, StringArgumentType.greedyString())
														.suggests(GIVABLE_ITEMS)
														.executes(source -> {
															return give(source.getSource(), StringArgumentType.getString(source, CommandHelper.GIVE_ITEM));
														})
												) // end of ITEM
										// TODO add ownership
								)
								///// CLAIMED_BY TOP-LEVEL OPTION /////
								.then(Commands.literal(CommandHelper.CLAIMED_BY)
										.executes(source -> {
											return ParcelCommandDelegate.claimedBy(source.getSource(), null);
										})
										.then(Commands.argument(CommandHelper.POS, BlockPosArgument.blockPos())
												.executes(source -> {
													return ParcelCommandDelegate.claimedBy(source.getSource(), BlockPosArgument.getBlockPos(source, CommandHelper.POS));
												})
										)
								) // end of CLAIMED_BY
				); // end of register
	}

//	public static int demolishEstate(CommandSourceStack source, String estateName) {
//		try {
//			ServerPlayer player = source.getPlayerOrException();
//			return EstateCommandDelegate.demolish(source, player.getScoreboardName(), estateName);
//		} catch(Exception e) {
//			ClaimMyLand.LOGGER.error("an error occurred demonishing parcel:", e);
//			CommandHelper.unexceptedError(source);
//			return 0;
//		}
//	}

	public static int give(CommandSourceStack source, String giveItem) {
		try {
			ItemStack itemStack = switch(giveItem.toLowerCase()) {
				case "border_stone" -> new ItemStack(ModItems.BORDER_STONE.get());
				case "citizen_tool" -> new ItemStack(ModItems.CITIZEN_PLACEMENT_TOOL.get()); // TODO test if you are a nation owner
				case "zoning_tool" -> new ItemStack(ModItems.ZONING_PLACEMENT_TOOL.get()); // TODO test if you are a nation owner
				default -> ItemStack.EMPTY;
			};

			if (itemStack != ItemStack.EMPTY) {
				// attempt to add the deed item to the player inventory
				source.getPlayerOrException().getInventory().add(itemStack);
			}
			return 1;
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred giving item:", e);
			CommandHelper.unexceptedError(source);
			return 0;
		}
	}



	/**
	 * Player version can only generate one type of deed -> citizen.
	 * @param source
	 * @param deedType
	 * @param xSize
	 * @param ySizeUp
	 * @param ySizeDown
	 * @param zSize
	 * @param nationName
	 * @return
	 */
//	private static int generateDeed(CommandSourceStack source, String deedType, int xSize, int ySizeUp, int ySizeDown, int zSize, String nationName) {
//		try {
//			ServerPlayer player = source.getPlayerOrException();
//
//			// get the type
////			try {
////			ParcelType type = ParcelType.valueOf(deedType);
////			} catch(Exception e) {
////				source.sendFailure(Component.translatable(LangUtil.chat("parcel.unknown_type")).withStyle(ChatFormatting.RED));
////				return 0;
////			}
//
//			// find the nation by name
////			Optional<Parcel> nation = ParcelRegistry.getNations().stream()
////					.filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getEstate().getName()))
////					.findFirst();
//			Optional<Estate> optionalEstate = CommandHelper.getEstateByOwner(source, player.getUUID(), nationName);
//
//			// validations
//			if (optionalEstate.isEmpty()) {
//				source.sendFailure(Component.translatable(LangUtil.chat("parcel.nation.unable_to_locate")).withStyle(ChatFormatting.RED));
//				return 0;
//			}
////			if (!nation.get().getOwnerId().equals(player.getUUID())) {
////				source.sendFailure(Component.translatable(LangUtil.chat("parcel.nation.not_owner")).withStyle(ChatFormatting.RED));
////				return 0;
////			}
//
//			// create a relative sized Box
//			Box size = new Box(Coords.of(0, -ySizeDown, 0), Coords.of(xSize-1, ySizeUp-1, zSize-1));
//
//			// create a deed item
//			ItemStack deed = DeedFactory.createCitizenDeed(size, optionalEstate.get().getId());
////			deed.getOrCreateTag().putString(CitizenDeed.NATION_NAME, nationName);
//
//			// attempt to add the deed item to the player inventory
//			if (deed != ItemStack.EMPTY) {
//				source.getPlayerOrException().getInventory().add(deed);
//			}
//		} catch (Exception e) {
//			ClaimMyLand.LOGGER.error("error while generating deed:", e);
//			CommandHelper.failure(source, " deed.generate.failure");
//		}
//		return 1;
//	}
}
