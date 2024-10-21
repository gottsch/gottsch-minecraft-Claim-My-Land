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
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import mod.gottsch.forge.claimmyland.core.parcel.NationBorderType;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.setup.Registration;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author Mark Gottschling on Oct 11, 2021
 *
 */
public class PlayersCommand {
	private static final String PROTECT = "cml";
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

	private static final SuggestionProvider<CommandSourceStack> OWNER_NATION_NAMES = (source, builder) -> {
		ServerPlayer owner = source.getSource().getPlayerOrException();
		List<String> names = ParcelRegistry.getNations().stream()
				.filter(p -> p.getOwnerId().equals(owner.getUUID()))
				.map((Parcel::getName)).toList();
		return SharedSuggestionProvider.suggest(names, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> OWNER_PARCEL_NAMES = (source, builder) -> {
		ServerPlayer owner = source.getSource().getPlayerOrException();
		List<String> names = ParcelRegistry.findByOwner(owner.getUUID()).stream()
				.map((Parcel::getName)).toList();
		return SharedSuggestionProvider.suggest(names, builder);
	};

	static final SuggestionProvider<CommandSourceStack> PLAYER_NAMES = (source, builder) -> {
		List<String> names = source.getSource().getLevel().getServer().getPlayerList().getPlayers().stream().map(p -> p.getName().getString()).toList();
		return SharedSuggestionProvider.suggest(names, builder);
	};

	static final SuggestionProvider<CommandSourceStack> GIVABLE_ITEMS = (source, builder) -> {
		return SharedSuggestionProvider.suggest(Stream.of("border_stone", "citizen_tool", "zoning_tool"), builder);
	};

	/**
	 *
	 * @param dispatcher
	 */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher
				.register(Commands.literal(PROTECT)
						.requires(source -> {
							return source.hasPermission(0);
						})
						///// DEED TOP-LEVEL OPTION /////
						.then(Commands.literal(CommandHelper.DEED).requires(source -> {
											return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
										})
										.then(Commands.literal(CommandHelper.NEW)
												.then(Commands.argument(CommandHelper.DEED_TYPE, StringArgumentType.string())
														.suggests(DEED_TYPES)
														.then(Commands.argument(CommandHelper.X_SIZE, IntegerArgumentType.integer())
																.then(Commands.argument(CommandHelper.Y_SIZE_UP, IntegerArgumentType.integer())
																		.then(Commands.argument(CommandHelper.Y_SIZE_DOWN, IntegerArgumentType.integer())
																				.then(Commands.argument(CommandHelper.Z_SIZE, IntegerArgumentType.integer())
																						.then(Commands.argument(CommandHelper.NATION_NAME, StringArgumentType.string())
																								.suggests(OWNER_NATION_NAMES)
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
						.then(Commands.literal(CommandHelper.PARCEL).requires(source -> {
											return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
										})
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
														.suggests(OWNER_NATION_NAMES)
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
																.suggests(PLAYER_NAMES)
																.executes(source -> {
																	return transferParcel(source.getSource(),
																			StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
																			StringArgumentType.getString(source, CommandHelper.NEW_OWNER_NAME));
																})
														)
												)

										)

						) // end of parcel
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
			ClaimMyLand.LOGGER.error("an error occurred demolishing a parcels:", e);
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

//	/**
//	 *
//	 * @param source
//	 * @param propertyName
//	 * @return
//	 */
//	private static int propertyListPermissions(CommandSourceStack source, String propertyName) {
//		try {
//			// get the owner
//			ServerPlayer owner = source.getPlayerOrException();
//			// get the owner's properties
//			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner.getUUID());
//			// get the named property
//			List<Property> namedProperties = properties.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
//			if (namedProperties.isEmpty()) {
//				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
//						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
//				return 1;
//			}
//			Property property = namedProperties.get(0);
//
//			List<Component> messages = new ArrayList<>();
//			messages.add(Component.literal(""));
//			messages.add(Component.translatable(LangUtil.message("property.permission.list"))
//					.withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE)
//					.append(propertyName).withStyle(ChatFormatting.AQUA));
//			messages.add(Component.literal(""));
//
//			for (int i = 0; i < Permission.values().length; i++) {
//				property.hasPermission(i);
//				Permission permission = Permission.getByValue(i);
//				MutableComponent component = Component.translatable(permission.name()).withStyle(ChatFormatting.AQUA)
//						.append(Component.literal(" = "));
//				if (property.hasPermission(i)) {
//					component.append(Component.translatable(LangUtil.message("permission.state.on")).withStyle(ChatFormatting.GREEN));
//				}
//				else {
//					component.append(Component.translatable(LangUtil.message("permission.state.off")).withStyle(ChatFormatting.RED));
//				}
//				messages.add(component);
//			}
//
//			messages.forEach(component -> {
//				source.sendSuccess(component, false);
//			});
//		} catch (Exception e) {
//			ProtectIt.LOGGER.error("Unable to execute whitelistAddPlayer command:", e);
//			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
//					.withStyle(ChatFormatting.RED));
//		}
//		// TODO print out all the permissions that are on.
//		return 1;
//	}

//	/**
//	 *
//	 * @param source
//	 * @param propertyName
//	 * @param permissionName
//	 * @param value
//	 * @return
//	 */
//	private static int propertyChangePermission(CommandSourceStack source, String propertyName, String permissionName, boolean value) {
//		try {
//			// get the owner
//			ServerPlayer owner = source.getPlayerOrException();
//			// get the owner's properties
//			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner.getUUID());
//			// get the named property
//			List<Property> namedProperties = properties.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
//			if (namedProperties.isEmpty()) {
//				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
//						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
//				return 1;
//			}
//			Property property = namedProperties.get(0);
//
//			// update permission on property
//			property.setPermission(Permission.valueOf(permissionName).value, value);
//			CommandHelper.saveData(source.getLevel());
//
//			//send update to client
//			if(source.getLevel().getServer().isDedicatedServer()) {
//				PermissionChangeS2CPush message = new PermissionChangeS2CPush(
//						owner.getUUID(),
//						property.getUuid(),
//						Permission.valueOf(permissionName).value,
//						value
//				);
//				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
//			}
//
//			source.sendSuccess(Component.translatable(LangUtil.message("property.permission.change_success"))
//					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);
//
//		} catch (Exception e) {
//			ProtectIt.LOGGER.error("Unable to execute whitelistAddPlayer command:", e);
//			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
//					.withStyle(ChatFormatting.RED));
//		}
//
//		return 1;
//	}

//	/**
//	 *
//	 * @param source
//	 * @param propertyName
//	 * @param player
//	 * @return
//	 */
//	public static int whitelistAddPlayer(CommandSourceStack source, String propertyName, @Nullable String player) {
//		ProtectIt.LOGGER.debug("executing whitelist.add() command...");
//
//		try {
//			GameProfileCache cache = source.getLevel().getServer().getProfileCache();
//			Optional<GameProfile> profile = cache.get(player.toLowerCase());
//			if (profile.isEmpty()) {
//				source.sendFailure(Component.translatable(LangUtil.message("unable_locate_player")));
//			}
//
//			// get the owner
//			ServerPlayer owner = source.getPlayerOrException();
//			// TODO replace with CommmandHelper call
//			// get the owner's properties
//			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner.getUUID());
//			// get the named property
//			List<Property> namedProperties = properties.stream().filter(prop -> prop.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
//			if (namedProperties.isEmpty()) {
//				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
//						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
//				return 1;
//			}
//			Property property = namedProperties.get(0);
//			// update property whitelist with player
//			if (property.getWhitelist().stream().noneMatch(data -> data.getName().equalsIgnoreCase(profile.get().getName()))) {
//				property.getWhitelist().add(new PlayerIdentity(profile.get().getId(), profile.get().getName()));
//				CommandHelper.saveData(source.getLevel());
//			}
//			//send update to client
//			if(source.getLevel().getServer().isDedicatedServer()) {
//				WhitelistAddS2CPush message = new WhitelistAddS2CPush(
//						owner.getUUID(),
//						property.getUuid(),
//						profile.get().getName(),
//						profile.get().getId()
//				);
//				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
//			}
//
//			source.sendSuccess(Component.translatable(LangUtil.message("whitelist.add.success"))
//					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);
//
//		} catch (Exception e) {
//			ProtectIt.LOGGER.error("Unable to execute whitelistAddPlayer command:", e);
//			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
//					.withStyle(ChatFormatting.RED));
//		}
//
//		return 1;
//	}
//
//	/**
//	 *
//	 * @param source
//	 * @param propertyName
//	 * @param player
//	 * @return
//	 */
//	public static int whitelistRemovePlayer(CommandSourceStack source, String propertyName, @Nullable String playerName) {
//		ProtectIt.LOGGER.debug("Executing whitelistRemovePlayer() command...");
//
//		try {
//			// get the owner
//			ServerPlayer owner = source.getPlayerOrException();
//			// TODO replace with CommandHelper call
//			// get the owner's properties
//			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner.getUUID());
//			// get the named property
//			List<Property> names = properties.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
//			if (names.isEmpty()) {
//				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
//						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
//				return 1;
//			}
//			Property property = names.get(0);
//			// update property whitelist with player
//			boolean result = property.getWhitelist().removeIf(p -> p.getName().equalsIgnoreCase(playerName));
//			CommandHelper.saveData(source.getLevel());
//
//			// send update to client
//			if(result && source.getLevel().getServer().isDedicatedServer()) {
//				WhitelistRemoveS2CPush message = new WhitelistRemoveS2CPush(
//						owner.getUUID(),
//						property.getUuid(),
//						playerName.toUpperCase(),
//						null
//				);
//				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
//			}
//
//			source.sendSuccess(Component.translatable(LangUtil.message("whitelist.remove.success"))
//					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);
//
//		} catch (Exception e) {
//			ProtectIt.LOGGER.error("Unable to execute whitelistRemovePlayer command:", e);
//			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
//					.withStyle(ChatFormatting.RED));
//		}
//
//
//		return 1;
//	}
//
//	/**
//	 *
//	 * @param source
//	 * @param propertyName
//	 * @param player
//	 * @return
//	 */
//	public static int whitelistClear(CommandSourceStack source, String propertyName) {
//		ProtectIt.LOGGER.debug("Executing whitelistClear() command...");
//
//		try {
//			// get the owner
//			ServerPlayer owner = source.getPlayerOrException();
//			//			// get the owner's properties
//			//			List<Property> properties = ProtectionRegistries.block().getProtections(owner.getStringUUID());
//			//			// get the named property
//			//			List<Property> namedProperties = properties.stream().filter(p -> p.getName().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
//			Optional<Property> property = CommandHelper.getPropertyByName(owner.getUUID(), propertyName);
//			if (property.isEmpty()) {
//				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
//						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
//				return 1;
//			}
//
//			//			Property property = namedProperties.get(0);
//			property.get().getWhitelist().clear();
//			CommandHelper.saveData(source.getLevel());
//
//			//send update to client
//			if(source.getLevel().getServer().isDedicatedServer()) {
//				WhitelistClearS2CPush message = new WhitelistClearS2CPush(
//						owner.getUUID(),
//						property.get().getUuid()
//				);
//				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
//			}
//
//			source.sendSuccess(Component.translatable(LangUtil.message("whitelist.clear.success"))
//					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);
//
//		} catch (Exception e) {
//			ProtectIt.LOGGER.error("Unable to execute whitelistClear command:", e);
//			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
//					.withStyle(ChatFormatting.RED));
//		}
//
//		return 1;
//	}
//
//	/**
//	 *
//	 * @param source
//	 * @param propertyName
//	 * @return
//	 */
//	public static int whitelistListForProperty(CommandSourceStack source, String propertyName) {
//		ServerPlayer player;
//		try {
//			player = source.getPlayerOrException();
//		}
//		catch(CommandSyntaxException 	e) {
//			source.sendFailure(Component.translatable(LangUtil.message("unable_locate_player")));
//			return 1;
//		}
//
//		// TODO replace with CommadnHelper call
//		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(player.getUUID());
//		List<Property> namedProperties = properties.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
//		if (namedProperties.isEmpty()) {
//			source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
//					.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
//			return 1;
//		}
//		source.sendSuccess(Component.translatable(LangUtil.NEWLINE), false);
//		source.sendSuccess(Component.translatable(LangUtil.message("whitelist.property.list")).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
//				.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);
//		source.sendSuccess(Component.translatable(LangUtil.NEWLINE), false);
//
//		namedProperties.get(0).getWhitelist().forEach(data -> {
//			source.sendSuccess(Component.translatable(data.getName()).withStyle(ChatFormatting.GREEN), false);
//		});
//
//		return 1;
//	}
}
