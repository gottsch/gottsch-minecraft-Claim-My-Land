/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
 * 
 * All rights reserved.
 *
 * Protect It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Protect It is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Protect It.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.claimmyland.core.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.core.parcel.NationBorderType;
import mod.gottsch.forge.claimmyland.core.persistence.PersistedData;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.Arrays;

/**
 * 
 * @author Mark Gottschling Sep 16, 2024
 *
 */
public class CommandHelper {

	public static final String CML_OPS = "cml-ops";
	public static final String DEED = "deed";
	public static final String PARCEL = "parcel";
	public static final String ADD = "add";
	public static final String REMOVE = "remove";
	public static final String LIST = "list";
	public static final String RENAME = "rename";
	public static final String TRANSFER = "transfer";
	public static final String CLEAR = "clear";
	public static final String GENERATE = "generate";
	public static final String NEW = "new";
	public static final String DEED_TYPE = "deed_type";
	public static final String POS = "pos";
	public static final String X_SIZE = "x_size";
	public static final String Y_SIZE_UP = "y_size_up";
	public static final String Y_SIZE_DOWN = "y_size_down";
	public static final String Z_SIZE = "z_size";
	public static final String OWNER_NAME = "owner_name";
	public static final String NEW_OWNER_NAME = "new_owner_name";
	public static final String PARCEL_NAME = "parcel_name";
	public static final String NEW_NAME = "new_name";
	public static final String BACKUP = "backup";
	public static final String RESTORE = "restore";
	public static final String WHITELIST = "whitelist";
	public static final String BY_OWNER = "by_owner";
	public static final String BY_NATION = "by_nation";
	public static final String NATION_NAME = "nation_name";
	public static final String ABANDON = "abandon";
	public static final String BY_ABANDONED = "by_abandoned";
	public static final String FROM_PARCEL = "from_parcel";
	public static final String DEMOLISH = "demolish";
    public static final String BORDER_TYPE ="border_type" ;

	public static final SuggestionProvider<CommandSourceStack> BORDER_TYPES = (source, builder) -> {
		return SharedSuggestionProvider.suggest(Arrays.stream(NationBorderType.values()).map(NationBorderType::getSerializedName), builder);
	};
	public static final String GIVE = "give";
	public static final String GIVE_ITEM = "give_item";
	public static final String CLAIMED_BY = "claimed_by";

	/**
	 * marks persistent data as dirty so that minecraft will auto save it.
	 * @param level
	 */
	public static void save(Level level) {
		PersistedData savedData = PersistedData.get(level);
		// mark data as dirty
		if (savedData != null) {
			savedData.setDirty();
		}
	}

	public static void sendNewLineMessage(CommandSourceStack source) {
		source.sendSuccess(() -> Component.translatable(LangUtil.NEWLINE), false);
	}

	public static void sendUnableToLocatePlayerMessage(CommandSourceStack source, String name) {
		source.sendSuccess(() -> Component.translatable(LangUtil.chat("unable_locate_player"), name).withStyle(ChatFormatting.RED), false);
	}

	public static void sendUnableToGenerateDeedMessage(CommandSourceStack source, String nationName) {
		source.sendSuccess(() -> Component.translatable(LangUtil.chat(" deed.generate.failure")).withStyle(ChatFormatting.RED), false);
	}

	/**
	 * convenience chat method
	 * @param source
	 */
	public static void unexceptedError(CommandSourceStack source) {
		failure(source, "unexpected_error");
	}

	public static void failure(CommandSourceStack source, String key) {
		source.sendFailure(Component.translatable(LangUtil.chat(key)).withStyle(ChatFormatting.RED));
	}

	///// SUGGESTIONS /////
//	static final SuggestionProvider<CommandSourceStack> SUGGEST_UUID = (source, builder) -> {
//		// NOTE use to find the player's name by UUID
//		//		source.getSource().getServer().getPlayerList()
//
//
//		return SharedSuggestionProvider.suggest(ProtectionRegistries.block().findByClaim(p -> !p.getOwner().getUuid().isEmpty()).stream()
//				.map(i -> String.format("%s [%s]",
//						(i.getOwner().getName() == null) ? "" : i.getOwner().getName(),
//								(i.getOwner().getUuid() == null) ? "" : i.getOwner().getUuid())), builder);
//	};
//
//	static final SuggestionProvider<CommandSourceStack> GIVABLE_ITEMS = (source, builder) -> {
//		List<String> items = Arrays.asList(
//				"Property Lever",
//				"Remove Claim Stake"
//				);
//		return SharedSuggestionProvider.suggest(items, builder);
//	};
//
//	/**
//	 *
//	 * @param source
//	 * @return
//	 */
//	static int unavailable(CommandSourceStack source) {
//		source.sendSuccess(Component.translatable(LangUtil.message("option_unavailable")), false);
//		return 1;
//	}
//
//
//
//	/**
//	 *
//	 * @param source
//	 * @param oldName
//	 * @param newName
//	 * @return
//	 */
//	public static int rename(CommandSourceStack source, String oldName, String newName) {
//		ServerPlayer player = null;
//		try {
//			player = source.getPlayerOrException();
//		}
//		catch(CommandSyntaxException 	e) {
//			source.sendFailure(Component.translatable(LangUtil.message("unable_locate_player")));
//			return 1;
//		}
//		List<Property> claims = ProtectionRegistries.block().getProtections(player.getStringUUID());
//		List<Property> namedClaims = claims.stream().filter(claim -> claim.getName().equalsIgnoreCase(oldName)).collect(Collectors.toList());
//		if (namedClaims.isEmpty()) {
//			source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
//					.append(Component.translatable(oldName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
//			return 1;
//		}
//		namedClaims.get(0).setName(newName.toUpperCase());
//
//		source.sendSuccess(Component.translatable(LangUtil.message("property.rename.success"))
//				.append(Component.translatable(newName.toUpperCase()).withStyle(ChatFormatting.AQUA)), false);
//
//		saveData(source.getLevel());
//		// NOTE it is not necessary to send message to client as rename() method is called from server
//		// and server registry is used to lookup rename() etc.
//
//		return 1;
//	}
//
//	/**
//	 *
//	 * @param source
//	 * @return
//	 */
//	public static int list(CommandSourceStack source) {
//		ServerPlayer player;
//		try {
//			player = source.getPlayerOrException();
//			return list(source, player);
//		}
//		catch(CommandSyntaxException 	e) {
//			source.sendFailure(Component.translatable(LangUtil.message("unable_locate_player")));
//		}
//		return 1;
//	}
//
//	/**
//	 *
//	 * @param source
//	 * @param player
//	 * @return
//	 */
//	public static int list(CommandSourceStack source, ServerPlayer player) {
//		List<Component> messages = new ArrayList<>();
//		messages.add(Component.literal(""));
//		messages.add(Component.translatable(LangUtil.message("property.list"), player.getName().getString()).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
//		messages.add(Component.literal(""));
//
//		List<Component> components = formatList(messages, ProtectionRegistries.block().getProtections(player.getStringUUID()));
//		components.forEach(component -> {
//			source.sendSuccess(component, false);
//		});
//		return 1;
//	}
//
//	/**
//	 *
//	 * @param messages
//	 * @param list
//	 * @return
//	 */
//	static List<Component> formatList(List<Component> messages, List<Property> list) {
//
//		if (list.isEmpty()) {
//			messages.add(Component.translatable(LangUtil.message("property.list.empty")).withStyle(ChatFormatting.AQUA));
//		}
//		else {
//			list.forEach(claim -> {
//				messages.add(Component.translatable(claim.getName().toUpperCase() + ": ").withStyle(ChatFormatting.AQUA)
//						.append(Component.translatable(String.format("(%s) to (%s)",
//								formatCoords(claim.getBox().getMinCoords()),
//								formatCoords(claim.getBox().getMaxCoords()))).withStyle(ChatFormatting.GREEN)
//						)
//						.append(Component.translatable(", size: (" + formatCoords(claim.getBox().getSize()) + ")").withStyle(ChatFormatting.WHITE))
//				);
//
////				[STYLE].withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockpos.getX() + " " + s1 + " " + blockpos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))
//			});
//		}
//		return messages;
//	}
//
//	/**
//	 *
//	 * @param source
//	 * @param registryName
//	 * @return
//	 */
//	// TODO update to take in a Player param
//	static int give(CommandSourceStack source, String name) {
//		try {
//			Item givableItem = null;
//			switch (name.toLowerCase()) {
//			case "property lever":
//				givableItem = ProtectItItems.PROPERTY_LEVER.get();
//				break;
//			case "remove claim stake":
//				givableItem = ProtectItItems.REMOVE_CLAIM.get();
//				break;
//			}
//			if (givableItem == null) {
//				source.sendSuccess(Component.translatable(LangUtil.message("non_givable_item")), false);
//				return 1;
//			}
//			source.getPlayerOrException().getInventory().add(new ItemStack(givableItem));
//		}
//		catch(Exception e) {
//			ProtectIt.LOGGER.error("error on give -> ", e);
//		}
//		return 1;
//	}
//
//	/**
//	 *
//	 * @param source
//	 */
//	static void saveData(ServerLevel level) {
//		PersistedData savedData = PersistedData.get(level);
//		if (savedData != null) {
//			savedData.setDirty();
//		}
//	}
//
//	public static String formatCoords(ICoords coords) {
//		return String.format("%s, %s, %s", coords.getX(), coords.getY(), coords.getZ());
//	}
//
//	/**
//	 *
//	 * @param uuid
//	 * @return
//	 */
//	static String parseNameUuid(String uuid) {
//		String output = "";
//		// parse out the uuid
//		if (uuid.contains("[")) {
//			// find between square brackets
//			Pattern p = Pattern.compile("\\[([^\"]*)\\]");
//			Matcher m = p.matcher(uuid);
//			// get first occurence
//			if (m.find()) {
//				output = m.group(1);
//			}
//		}
//		return output;
//	}
//
//	/**
//	 *
//	 * @param owner
//	 * @param property
//	 * @return
//	 */
//	public static Optional<Property> getProperty(UUID owner, UUID property) {
//		// get the owner's properties
//		List<Property> claims = ProtectionRegistries.block().getProtections(owner.toString());
//		// get the named property
//		List<Property> namedClaims = claims.stream().filter(claim -> claim.getUuid().equals(property)).collect(Collectors.toList());
//		if (namedClaims.isEmpty()) {
//			return Optional.empty();
//		}
//		return Optional.ofNullable(namedClaims.get(0));
//	}
//
//	/**
//	 *
//	 * @param c1
//	 * @param c2
//	 * @return
//	 */
//	public static Optional<Tuple<ICoords, ICoords>> validateCoords(ICoords c1, ICoords c2) {
//		Optional<Tuple<ICoords, ICoords>> coords = Optional.of (new Tuple<ICoords, ICoords>(c1, c2));
//		if (!isDownField(c1, c2)) {
//			// attempt to flip coords and test again
//			if (isDownField(c2, c1)) {
//				coords = Optional.of(new Tuple<ICoords, ICoords>(c2, c1));
//			}
//			else {
//				coords = Optional.empty();
//			}
//		}
//		return coords;
//	}
//
//	/**
//	 * TODO When updating to allow Y values, update this method to include Y check
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public static boolean isDownField(ICoords from, ICoords to) {
//		if (to.getX() >= from.getX() && to.getZ() >= from.getZ()) {
//			return true;
//		}
//		return false;
//	}
}
