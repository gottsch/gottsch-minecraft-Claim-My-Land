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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.item.CitizenDeed;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import mod.gottsch.forge.claimmyland.core.parcel.NationBorderType;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author Mark Gottschling on Oct 11, 2021
 *
 */
public class PlayersCommand {
	private static final String CML = "cml";
	private static final String CURRENT_NAME = "current_name";
	private static final String NEW_NAME = "new_name";


//	private static final SuggestionProvider<CommandSourceStack> WHITELIST_NAMES = (source, builder) -> {
//		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(source.getSource().getPlayerOrException().getUUID());
//		List<String> names = properties.stream().flatMap(x -> x.getWhitelist().stream().map(y -> y.getName() )).collect(Collectors.toList());
//		return SharedSuggestionProvider.suggest(names, builder);
//	};
//
//	private static final SuggestionProvider<CommandSourceStack> PERMISSIONS = (source, builder) -> {
//		return SharedSuggestionProvider.suggest(Permission.getNames(), builder);
//	};

	private static final SuggestionProvider<CommandSourceStack> DEED_TYPES = (source, builder) -> {
		return SharedSuggestionProvider.suggest(Stream.of(ParcelType.CITIZEN)
				.map(ParcelType::getSerializedName), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> OWNER_NATION_PARCEL_NAMES = (source, builder) -> {
		ServerPlayer owner = source.getSource().getPlayerOrException();
		List<String> names = ParcelRegistry.getNations().stream()
				.filter(p -> p.getOwnerId().equals(owner.getUUID()))
				.map((Parcel::getName)).toList();
		return SharedSuggestionProvider.suggest(names, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_NAMES = (source, builder) -> {
		ServerPlayer owner = source.getSource().getPlayerOrException();
		List<String> names = EstateRegistry.getByOwner(owner.getUUID()).stream()
				.map((Estate::getName)).toList();
		return SharedSuggestionProvider.suggest(names, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_NAMES_MINUS_SELF = (source, builder) -> {
		ServerPlayer owner = source.getSource().getPlayerOrException();
		String primaryEstateName = StringArgumentType.getString(source, CommandHelper.ESTATE_NAME);
		List<String> estates = new ArrayList<>();

		estates = EstateRegistry.getByOwner(owner.getUUID()).stream()
				.map(Estate::getName)
				.filter(name -> !name.equalsIgnoreCase(primaryEstateName)).toList();

		return SharedSuggestionProvider.suggest(estates, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_PARCEL_NAMES = (source, builder) -> {
		ServerPlayer owner = source.getSource().getPlayerOrException();
		String estateName = StringArgumentType.getString(source, CommandHelper.ESTATE_NAME);
		Optional<Estate> estate = CommandHelper.getEstateByOwner(source.getSource(), owner.getUUID(), estateName);
		List<String> names = new ArrayList<>();
		if (estate.isPresent()) {
			Set<Parcel> parcels = ParcelRegistry.findAllByEstateId(estate.get().getId());
			names = parcels.stream().map((Parcel::getName)).toList();
		}
		return SharedSuggestionProvider.suggest(names, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_PARCEL_NAMES_MORE_THAN_ONE = (source, builder) -> {
		ServerPlayer owner = source.getSource().getPlayerOrException();
		String estateName = StringArgumentType.getString(source, CommandHelper.ESTATE_NAME);
		Optional<Estate> estate = CommandHelper.getEstateByOwner(source.getSource(), owner.getUUID(), estateName);

		List<String> names = new ArrayList<>();
		if (estate.isPresent()) {
			Set<Parcel> parcels = ParcelRegistry.findAllByEstateId(estate.get().getId());
			if (parcels.size() == 1) {
				names.add("[cannot split an estate with a single parcel]");
			} else {
				names = parcels.stream().map((Parcel::getName)).toList();
			}
		}
		return SharedSuggestionProvider.suggest(names, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> OWNER_PARCEL_NAMES = (source, builder) -> {
		ServerPlayer owner = source.getSource().getPlayerOrException();
		List<String> names = ParcelRegistry.findByOwner(owner.getUUID()).stream()
				.map((Parcel::getName)).toList();
		return SharedSuggestionProvider.suggest(names, builder);
	};

	static final SuggestionProvider<CommandSourceStack> GIVABLE_ITEMS = (source, builder) -> {
		return SharedSuggestionProvider.suggest(Stream.of("border_stone", "citizen_tool", "zoning_tool"), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> CURRENT_BLOCK_TAGS = (source, builder) -> {
		String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
		Optional<Set<String>> list = Optional.empty();
		Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
		if (ownerUuid.isPresent()) {
			list = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
		}
		return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> CURRENT_BLOCKS = (source, builder) -> {
		String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
		Optional<Set<String>> list = Optional.empty();

		Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
		if (ownerUuid.isPresent()) {
			list = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
		}
		return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> CURRENT_ITEM_TAGS = (source, builder) -> {
		String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
		Optional<Set<String>> list = Optional.empty();
		Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
		if (ownerUuid.isPresent()) {
			list = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
		}
		return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> CURRENT_ITEMS = (source, builder) -> {
		String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
		Optional<Set<String>> list = Optional.empty();
		Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
		if (ownerUuid.isPresent()) {
			list = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
		}
		return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
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
								.then(Commands.literal(CommandHelper.DEED)
//								.requires(source -> {
//											return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
//										})
												.then(Commands.literal(CommandHelper.NEW)
														.then(Commands.argument(CommandHelper.DEED_TYPE, StringArgumentType.string())
																.suggests(DEED_TYPES)
																.then(Commands.argument(CommandHelper.X_SIZE, IntegerArgumentType.integer())
																		.then(Commands.argument(CommandHelper.Y_SIZE_UP, IntegerArgumentType.integer())
																				.then(Commands.argument(CommandHelper.Y_SIZE_DOWN, IntegerArgumentType.integer())
																						.then(Commands.argument(CommandHelper.Z_SIZE, IntegerArgumentType.integer())
																								.then(Commands.argument(CommandHelper.NATION_NAME, StringArgumentType.string())
																										.suggests(OWNER_NATION_PARCEL_NAMES)
																										.executes(source -> {
																											return generateDeed(source.getSource(),
																													StringArgumentType.getString(source, CommandHelper.DEED_TYPE),
																													IntegerArgumentType.getInteger(source, CommandHelper.X_SIZE),
																													IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_UP),
																													IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_DOWN),
																													IntegerArgumentType.getInteger(source, CommandHelper.Z_SIZE),
																													StringArgumentType.getString(source, CommandHelper.NATION_NAME)
																											);
																											// TODO need to supply the owner name
																										})
																								)
																						)

																				)
																		)
																)
														)
												)
								)
								///// PARCEL TOP-LEVEL OPTION /////
								.then(Commands.literal(CommandHelper.PARCEL)
//										.requires(source -> {
//											return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
//										})
												///// LIST OPTION /////
												.then(Commands.literal(CommandHelper.LIST)
														.executes(source -> {
															return listParcels(source.getSource());
														})
												)
												///// ABANDON OPTION /////
												.then(Commands.literal(CommandHelper.ABANDON)
														.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
																.suggests(OWNER_PARCEL_NAMES)
																.executes(source -> {
																	return abandonParcel(source.getSource(), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
																})
														)
												)
												///// BORDER TYPE /////
												.then(Commands.literal(CommandHelper.BORDER_TYPE)
														.then(Commands.argument(CommandHelper.NATION_NAME, StringArgumentType.string())
																.suggests(OWNER_NATION_PARCEL_NAMES)
																.then(Commands.argument(CommandHelper.BORDER_TYPE, StringArgumentType.string())
																		.suggests(CommandHelper.BORDER_TYPES)
																		.executes(source -> {
																			return borderType(source.getSource(), StringArgumentType.getString(source, CommandHelper.NATION_NAME), StringArgumentType.getString(source, CommandHelper.BORDER_TYPE));
																		})
																)
														)
												)
												///// DEMOLISH /////
												.then(Commands.literal(CommandHelper.DEMOLISH)
														.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
																.suggests(OWNER_PARCEL_NAMES)
																.executes(source -> {
																	return demolishParcel(source.getSource(), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
																})
														)

												)
												///// RENAME PARCEL /////
												.then(Commands.literal(CommandHelper.RENAME)
														.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
																.suggests(OWNER_PARCEL_NAMES)
																.then(Commands.argument(CommandHelper.NEW_NAME, StringArgumentType.string())
																		.executes(source -> {
																			return renameParcel(source.getSource(),
																					StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
																					StringArgumentType.getString(source, CommandHelper.NEW_NAME));
																		})
																)
														)


												)
												///// TRANSFER /////
												.then(Commands.literal(CommandHelper.TRANSFER)
														.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
																.suggests(OWNER_PARCEL_NAMES)
																.then(Commands.argument(CommandHelper.NEW_OWNER_NAME, StringArgumentType.string())
																		.suggests(CommandHelper.PLAYER_NAMES)
																		.executes(source -> {
																			return transferParcel(source.getSource(),
																					StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
																					StringArgumentType.getString(source, CommandHelper.NEW_OWNER_NAME));
																		})
																)
														)

												)
												///// WHITELIST OPTION /////
//												.then(Commands.literal(CommandHelper.WHITELIST)
//																///// BLOCK TAG WHITELIST OPTION /////
//																.then(Commands.literal(CommandHelper.BLOCK_TAG)
//																				///// WHITELIST ADD /////
//																				.then(Commands.literal(CommandHelper.ADD)
//																						.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																								.suggests(OWNER_PARCEL_NAMES)
//																								.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
//																										.suggests(CommandHelper.BLOCK_TAGS)
//																										.executes(source -> {
//																											return InteractWhitelistCommandsDelegate.add(source.getSource(),
//																													StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																													ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME),
//																													InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
//																										})
//																								)
//																						)
//																				)
//
//																				///// BLOCK TAGS WHITELIST LIST /////
//																				.then(Commands.literal(CommandHelper.LIST)
//																						.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																								.suggests(OWNER_PARCEL_NAMES)
//																								.executes(source -> {
//																									return InteractWhitelistCommandsDelegate.list(source.getSource(),
//																											StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																											InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
//																								})
//																						)
//																				)
//
////																				///// BLOCK TAGS WHITELIST REMOVE /////
//																				.then(Commands.literal(CommandHelper.REMOVE)
//																						.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																								.suggests(OWNER_PARCEL_NAMES)
//																								.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
//																										.suggests(CURRENT_BLOCK_TAGS)
//																										.executes(source -> {
//																											return InteractWhitelistCommandsDelegate.remove(source.getSource(),
//																													StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																													ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
//																										})
//																								)
//																						)
//																				)
//																)
//																.then(Commands.literal(CommandHelper.BLOCKS)
//																		///// WHITELIST ADD /////
//																		.then(Commands.literal(CommandHelper.ADD)
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.then(Commands.argument(CommandHelper.ITEM, ItemArgument.item(buildContext))
//																								.executes(source -> {
//																									return InteractWhitelistCommandsDelegate.add(source.getSource(),
//																											StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																											ItemArgument.getItem(source, CommandHelper.ITEM),
//																											InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
//																								})
//																						)
//																				)
//																		)
//
//																		///// BLOCK WHITELIST REMOVE /////
//																		.then(Commands.literal(CommandHelper.REMOVE)
//
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
//																								.suggests(CURRENT_BLOCKS)
//																								.executes(source -> {
//																									return InteractWhitelistCommandsDelegate.remove(source.getSource(),
//																											StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																											ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
//																								})
//																						)
//
//																				)
//																		)
//																		///// BLOCK WHITELIST LIST /////
//																		.then(Commands.literal(CommandHelper.LIST)
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.executes(source -> {
//																							return InteractWhitelistCommandsDelegate.list(source.getSource(),
//																									StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																									InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
//																						})
//																				)
//																		)
//																)
//
//																///// ITEM TAG WHITELIST OPTION /////
//																.then(Commands.literal(CommandHelper.ITEM_TAG)
//																		///// WHITELIST ADD /////
//																		.then(Commands.literal(CommandHelper.ADD)
//
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
//																								.suggests(CommandHelper.ITEM_TAGS)
//																								.executes(source -> {
//																									return InteractWhitelistCommandsDelegate.add(source.getSource(),
//																											StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																											ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
//																								})
//																						)
//
//																				)
//																		)
//																		///// ITEM TAGS WHITELIST LIST /////
//																		.then(Commands.literal(CommandHelper.LIST)
//
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.executes(source -> {
//																							return InteractWhitelistCommandsDelegate.list(source.getSource(),
//																									StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																									InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
//																						})
//
//																				)
//																		)
//																		///// ITEM TAGS WHITELIST REMOVE /////
//																		.then(Commands.literal(CommandHelper.REMOVE)
//
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
//																								.suggests(CURRENT_ITEM_TAGS)
//																								.executes(source -> {
//																									return InteractWhitelistCommandsDelegate.remove(source.getSource(),
//																											StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																											ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
//																								})
//																						)
//																				)
//
//																		)
//																)
//																///// ITEM WHITELIST OPTION /////
//																.then(Commands.literal(CommandHelper.ITEMS)
//																		///// ITEM WHITELIST ADD /////
//																		.then(Commands.literal(CommandHelper.ADD)
//
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.then(Commands.argument(CommandHelper.ITEM, ItemArgument.item(buildContext))
//																								.executes(source -> {
//																									return InteractWhitelistCommandsDelegate.add(source.getSource(),
//																											StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																											ItemArgument.getItem(source, CommandHelper.ITEM),
//																											InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
//																								})
//																						)
//																				)
//
//																		)
//																		///// ITEM WHITELIST REMOVE /////
//																		.then(Commands.literal(CommandHelper.REMOVE)
//
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
//																								.suggests(CURRENT_ITEMS)
//																								.executes(source -> {
//																									return InteractWhitelistCommandsDelegate.remove(source.getSource(),
//																											StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																											ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
//																								})
//																						)
//
//																				)
//																		)
//																		///// ITEM WHITELIST LIST /////
//																		.then(Commands.literal(CommandHelper.LIST)
//
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.executes(source -> {
//																							return InteractWhitelistCommandsDelegate.list(source.getSource(),
//																									StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																									InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
//																						})
//																				)
//																		)
//																)
//																.then(Commands.literal(CommandHelper.FRIENDS)
//																		///// FRIENDS WHITELIST ADD /////
//																		.then(Commands.literal(CommandHelper.ADD)
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.then(Commands.argument(CommandHelper.FRIEND_NAME, StringArgumentType.string())
//																								.suggests(CommandHelper.PLAYER_NAMES)
//																								.executes(source -> {
//																									return FriendsWhitelistCommandsDelegate.add(source.getSource(),
//																											StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																											StringArgumentType.getString(source, CommandHelper.FRIEND_NAME));
//																								})
//																						)
//																				)
//																		)
//																		///// FRIENDS WHITELIST REMOVE /////
//																		.then(Commands.literal(CommandHelper.REMOVE)
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.then(Commands.argument(CommandHelper.FRIEND_NAME, StringArgumentType.string())
//																								.suggests(CommandHelper.CURRENT_FRIENDS_NAMES)
//																								.executes(source -> {
//																									return FriendsWhitelistCommandsDelegate.remove(source.getSource(),
//																											StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//																											StringArgumentType.getString(source, CommandHelper.FRIEND_NAME));
//																								})
//																						)
//																				)
//																		)
//																		///// FRIENDS WHITELIST LIST /////
//																		.then(Commands.literal(CommandHelper.LIST)
//																				.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//																						.suggests(OWNER_PARCEL_NAMES)
//																						.executes(source -> {
//																							return FriendsWhitelistCommandsDelegate.list(source.getSource(),
//																									StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
//																						})
//																				)
//																		)
//																)

//												)
								) // end of parcel

								///// ESTATE TOP-LEVEL OPTION /////
								.then(Commands.literal(CommandHelper.ESTATE)
												///// LIST OPTION /////
												.then(Commands.literal(CommandHelper.LIST)
														.executes(source -> {
															return listEstates(source.getSource());
														})
												)
												///// INFO OPTION /////
												.then(Commands.literal(CommandHelper.INFO)
														.executes(source -> {
															return listEstates(source.getSource());
														})
												)
												///// JOIN (ANNEX) OPTION /////
												.then(Commands.literal(CommandHelper.JOIN)
														.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																.suggests(OWNER_ESTATE_NAMES)
																.then(Commands.argument(CommandHelper.OTHER_ESTATE_NAME, StringArgumentType.string())
																		.suggests(OWNER_ESTATE_NAMES_MINUS_SELF)
																		.executes(source -> {
																			return joinEstates(source.getSource(),
																					StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																					StringArgumentType.getString(source, CommandHelper.OTHER_ESTATE_NAME));
																		})
																)
														)
												)
												///// SPLIT (CEDE) OPTION /////
												.then(Commands.literal(CommandHelper.SPLIT)
														.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																.suggests(OWNER_ESTATE_NAMES)
																.then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
																		.suggests(OWNER_ESTATE_PARCEL_NAMES_MORE_THAN_ONE)
																		.executes(source -> {
																			return splitEstate(source.getSource(),
																					StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																					StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
																		})
																)
														)
												)
												///// RENAME OPTION /////
												.then(Commands.literal(CommandHelper.RENAME)
														.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																.suggests(OWNER_ESTATE_NAMES)
																.then(Commands.argument(CommandHelper.NEW_NAME, StringArgumentType.string())
																		.executes(source -> {
																			return renameEstate(source.getSource(),
																					StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																					StringArgumentType.getString(source, CommandHelper.NEW_NAME));
																		})
																)
														)
												)
												///// TRANSFER /////
												.then(Commands.literal(CommandHelper.TRANSFER)
														.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																.suggests(OWNER_ESTATE_NAMES)
																.then(Commands.argument(CommandHelper.NEW_OWNER_NAME, StringArgumentType.string())
																		.suggests(CommandHelper.PLAYER_NAMES)
																		.executes(source -> {
																			return EstateCommandDelegate.transfer(source.getSource(),
																					StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																					StringArgumentType.getString(source, CommandHelper.NEW_OWNER_NAME));
																		})
																)
														)

												)
												///// WHITELIST OPTION /////
												.then(Commands.literal(CommandHelper.WHITELIST)
																///// BLOCK TAG WHITELIST OPTION /////
																.then(Commands.literal(CommandHelper.BLOCK_TAG)
																				///// WHITELIST ADD /////
																				.then(Commands.literal(CommandHelper.ADD)
																						.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																								.suggests(OWNER_ESTATE_NAMES)
																								.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
																										.suggests(CommandHelper.BLOCK_TAGS)
																										.executes(source -> {
																											return InteractWhitelistCommandsDelegate.addToEstate(source.getSource(),
																													StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																													ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME),
																													InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
																										})
																								)
																						)
																				)

																				///// BLOCK TAGS WHITELIST LIST /////
																				.then(Commands.literal(CommandHelper.LIST)
																						.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																								.suggests(OWNER_ESTATE_NAMES)
																								.executes(source -> {
																									return InteractWhitelistCommandsDelegate.listFromEstate(source.getSource(),
																											StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																											InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);

																								})
																						)
																				)

//																				///// BLOCK TAGS WHITELIST REMOVE /////
																				.then(Commands.literal(CommandHelper.REMOVE)
																						.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																								.suggests(OWNER_ESTATE_NAMES)
																								.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
																										.suggests(CURRENT_BLOCK_TAGS)
																										.executes(source -> {
																											return InteractWhitelistCommandsDelegate.removeFromEstate(source.getSource(),
																													StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																													ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME),
																													InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
																										})
																								)
																						)
																				)
																)
																.then(Commands.literal(CommandHelper.BLOCKS)
																		///// WHITELIST ADD /////
																		.then(Commands.literal(CommandHelper.ADD)
																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.then(Commands.argument(CommandHelper.ITEM, ItemArgument.item(buildContext))
																								.executes(source -> {
																									return InteractWhitelistCommandsDelegate.addToEstate(source.getSource(),
																											StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																											ItemArgument.getItem(source, CommandHelper.ITEM),
																											InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
																								})
																						)
																				)
																		)

																		///// BLOCK WHITELIST REMOVE /////
																		.then(Commands.literal(CommandHelper.REMOVE)

																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
																								.suggests(CURRENT_BLOCKS)
																								.executes(source -> {
																									return InteractWhitelistCommandsDelegate.removeFromEstate(source.getSource(),
																											StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																											ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME),
																											InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
																								})
																						)

																				)
																		)
																		///// BLOCK WHITELIST LIST /////
																		.then(Commands.literal(CommandHelper.LIST)
																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.executes(source -> {
																							return InteractWhitelistCommandsDelegate.listFromEstate(source.getSource(),
																									StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																									InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
																						})
																				)
																		)
																)

																///// ITEM TAG WHITELIST OPTION /////
																.then(Commands.literal(CommandHelper.ITEM_TAG)
																		///// WHITELIST ADD /////
																		.then(Commands.literal(CommandHelper.ADD)

																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
																								.suggests(CommandHelper.ITEM_TAGS)
																								.executes(source -> {
																									return InteractWhitelistCommandsDelegate.addToEstate(source.getSource(),
																											StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																											ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME),
																											InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
																								})
																						)

																				)
																		)
																		///// ITEM TAGS WHITELIST LIST /////
																		.then(Commands.literal(CommandHelper.LIST)

																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.executes(source -> {
																							return InteractWhitelistCommandsDelegate.listFromEstate(source.getSource(),
																									StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																									InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
																						})

																				)
																		)
																		///// ITEM TAGS WHITELIST REMOVE /////
																		.then(Commands.literal(CommandHelper.REMOVE)

																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
																								.suggests(CURRENT_ITEM_TAGS)
																								.executes(source -> {
																									return InteractWhitelistCommandsDelegate.removeFromEstate(source.getSource(),
																											StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																											ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME),
																											InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
																								})
																						)
																				)

																		)
																)
																///// ITEM WHITELIST OPTION /////
																.then(Commands.literal(CommandHelper.ITEMS)
																		///// ITEM WHITELIST ADD /////
																		.then(Commands.literal(CommandHelper.ADD)
																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.then(Commands.argument(CommandHelper.ITEM, ItemArgument.item(buildContext))
																								.executes(source -> {
																									return InteractWhitelistCommandsDelegate.addToEstate(source.getSource(),
																											StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																											ItemArgument.getItem(source, CommandHelper.ITEM),
																											InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
																								})
																						)
																				)

																		)
																		///// ITEM WHITELIST REMOVE /////
																		.then(Commands.literal(CommandHelper.REMOVE)

																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
																								.suggests(CURRENT_ITEMS)
																								.executes(source -> {
																									return InteractWhitelistCommandsDelegate.removeFromEstate(source.getSource(),
																											StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																											ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME),
																											InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
																								})
																						)
																				)
																		)
																		///// ITEM WHITELIST LIST /////
																		.then(Commands.literal(CommandHelper.LIST)

																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.executes(source -> {
																							return InteractWhitelistCommandsDelegate.listFromEstate(source.getSource(),
																									StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																									InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
																						})
																				)
																		)
																)
																.then(Commands.literal(CommandHelper.FRIENDS)
																		///// FRIENDS WHITELIST ADD /////
																		.then(Commands.literal(CommandHelper.ADD)
																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.then(Commands.argument(CommandHelper.FRIEND_NAME, StringArgumentType.string())
																								.suggests(CommandHelper.PLAYER_NAMES)
																								.executes(source -> {
																									return FriendsWhitelistCommandsDelegate.addToEstate(source.getSource(),
																											StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																											StringArgumentType.getString(source, CommandHelper.FRIEND_NAME));
																								})
																						)
																				)
																		)
																		///// FRIENDS WHITELIST REMOVE /////
																		.then(Commands.literal(CommandHelper.REMOVE)
																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.then(Commands.argument(CommandHelper.FRIEND_NAME, StringArgumentType.string())
																								.suggests(CommandHelper.CURRENT_FRIENDS_NAMES)
																								.executes(source -> {
																									return FriendsWhitelistCommandsDelegate.removeFromEstate(source.getSource(),
																											StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
																											StringArgumentType.getString(source, CommandHelper.FRIEND_NAME));
																								})
																						)
																				)
																		)
																		///// FRIENDS WHITELIST LIST /////
																		.then(Commands.literal(CommandHelper.LIST)
																				.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																						.suggests(OWNER_ESTATE_NAMES)
																						.executes(source -> {
																							return FriendsWhitelistCommandsDelegate.listFromEstate(source.getSource(),
																									StringArgumentType.getString(source, CommandHelper.ESTATE_NAME));
																						})
																				)
																		)
																)
												) // end of whitelist
												///// DEMOLISH /////
												.then(Commands.literal(CommandHelper.DEMOLISH)
														.then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
																.suggests(OWNER_ESTATE_NAMES)
																.executes(source -> {
																	return demolishEstate(source.getSource(), StringArgumentType.getString(source, CommandHelper.ESTATE_NAME));
																})
														)

												)
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

	/**
	 *
	 * @param source
	 * @return
	 */
	public static int listParcels(CommandSourceStack source) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			return ParcelCommandDelegate.listParcelsByOwner(source, player);
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred listing parcels:", e);
			CommandHelper.failure(source, "unexpected_error");
		}
		return 1;
	}

	public static int abandonParcel(CommandSourceStack source, String parcelName) {
		ServerPlayer player = source.getPlayer();
		return ParcelCommandDelegate.abandonParcel(source, player.getScoreboardName(), parcelName);
	}

	public static int borderType(CommandSourceStack source, String nationName, String borderType) {
		try {
			ServerPlayer player = source.getPlayerOrException();

			// get the border type
			NationBorderType type = NationBorderType.valueOf(borderType.toUpperCase());

			// find the nation by name
			Optional<Parcel> nation = ParcelRegistry.getNations().stream()
					.filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getName()))
					.findFirst();

			if (nation.isEmpty()) {
				CommandHelper.failure(source,"parcel.nation.unable_to_locate");
				return 0;
			}

			// players version needs to validate that the player owns the nation
			if (!nation.get().getOwnerId().equals(player.getUUID())) {
				CommandHelper.failure(source, "parcel.nation.not_owner");
				return 0;
			}

			((NationParcel)nation.get()).setBorderType(type);
			CommandHelper.save(source.getLevel());

		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred changing nation border type:", e);
			CommandHelper.unexceptedError(source);
		}
		return 1;
	}

	public static int demolishParcel(CommandSourceStack source, String parcelName) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			return ParcelCommandDelegate.demolishParcel(source, player.getScoreboardName(), parcelName);
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred demonishing parcel:", e);
			CommandHelper.unexceptedError(source);
			return 0;
		}
	}

	public static int renameParcel(CommandSourceStack source, String parcelName, String newName) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			return ParcelCommandDelegate.renameParcel(source, player.getScoreboardName(), parcelName, newName);
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred renaming parcel:", e);
			CommandHelper.unexceptedError(source);
			return 0;
		}
	}

	public static int transferParcel(CommandSourceStack source, String parcelName, String newOwnerName) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			return ParcelCommandDelegate.transferParcel(source, player.getScoreboardName(), parcelName, newOwnerName);
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred transferring parcel:", e);
			CommandHelper.unexceptedError(source);
			return 0;
		}
	}

	public static int listEstates(CommandSourceStack source) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			return EstateCommandDelegate.listEstatesByOwner(source, player);
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred listing estates:", e);
			CommandHelper.failure(source, "unexpected_error");
		}
		return 1;
	}

	public static int renameEstate(CommandSourceStack source, String estateName, String newName) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			return EstateCommandDelegate.rename(source, player.getScoreboardName(), estateName, newName);
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred renaming estate:", e);
			CommandHelper.unexceptedError(source);
			return 0;
		}
	}

	public static int joinEstates(CommandSourceStack source, String mainEstateName, String satelliteEstateName) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			return EstateCommandDelegate.join(source, player.getScoreboardName(), mainEstateName, satelliteEstateName);
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred joing estates:", e);
			CommandHelper.unexceptedError(source);
			return 0;
		}
	}

	public static int splitEstate(CommandSourceStack source, String estateName, String parcelName) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			return EstateCommandDelegate.split(source, player.getScoreboardName(), estateName, parcelName);
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred joing estates:", e);
			CommandHelper.unexceptedError(source);
			return 0;
		}
	}

	public static int demolishEstate(CommandSourceStack source, String estateName) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			return EstateCommandDelegate.demolish(source, player.getScoreboardName(), estateName);
		} catch(Exception e) {
			ClaimMyLand.LOGGER.error("an error occurred demonishing parcel:", e);
			CommandHelper.unexceptedError(source);
			return 0;
		}
	}

//	public static int transferEstate(CommandSourceStack source, String parcelName, String newOwnerName) {
//		try {
//			ServerPlayer player = source.getPlayerOrException();
//			return EstateCommandDelegate.transfer(source, player.getScoreboardName(), parcelName, newOwnerName);
//		} catch(Exception e) {
//			ClaimMyLand.LOGGER.error("an error occurred transferring parcel:", e);
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
	 *
	 * @param source
	 * @param deedType
	 * @param xSize
	 * @param ySizeUp
	 * @param ySizeDown
	 * @param zSize
	 * @param nationName
	 * @return
	 */
	private static int generateDeed(CommandSourceStack source, String deedType, int xSize, int ySizeUp, int ySizeDown, int zSize, String nationName) {
		try {
			ServerPlayer player = source.getPlayerOrException();

			// get the type
			ParcelType type = ParcelType.valueOf(deedType);

			// find the nation by name
			Optional<Parcel> nation = ParcelRegistry.getNations().stream()
					.filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getName()))
					.findFirst();

			// validations
			if (nation.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.chat("parcel.nation.unable_to_locate")).withStyle(ChatFormatting.RED));
				return 0;
			}
			if (!nation.get().getOwnerId().equals(player.getUUID())) {
				source.sendFailure(Component.translatable(LangUtil.chat("parcel.nation.not_owner")).withStyle(ChatFormatting.RED));
				return 0;
			}

			// create a relative sized Box
			Box size = new Box(Coords.of(0, -ySizeDown, 0), Coords.of(xSize-1, ySizeUp-1, zSize-1));

			// create a deed item
			ItemStack deed = DeedFactory.createCitizenDeed(size, nation.get().getNationId());
			deed.getOrCreateTag().putString(CitizenDeed.NATION_NAME, nationName);

			// attempt to add the deed item to the player inventory
			if (deed != ItemStack.EMPTY) {
				source.getPlayerOrException().getInventory().add(deed);
			}
		} catch (Exception e) {
			ClaimMyLand.LOGGER.error("error while generating deed:", e);
			CommandHelper.failure(source, " deed.generate.failure");
		}
		return 1;
	}
}
