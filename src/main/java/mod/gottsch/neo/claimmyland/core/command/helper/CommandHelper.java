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
package mod.gottsch.neo.claimmyland.core.command.helper;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.persistence.PersistedData;
import mod.gottsch.neo.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * coordination utilities for command implementations: player/estate lookup,
 * persistence, and the single gateway for sending formatted output to the player.
 *
 * <p><b>sending contract:</b> all command output must go through
 * {@link #sendLines}, {@link #sendSuccess}, {@link #sendFailure}, or
 * {@link #sendWarning}. Commands must never call
 * {@code source.sendSuccess()} / {@code source.sendFailure()} directly.</p>
 *
 * <p><b>future direction (v2.x):</b> the {@code send*} methods here will migrate
 * to a default method on the {@code SubCommand} interface, at which point
 * {@code CommandHelper} will become a pure lookup/coordination class.</p>
 *
 * @author Mark Gottschling 9/16/2024
 */
public class CommandHelper {

	/**
	 * Removes all tenant parcels (Citizens, Zones) belonging to a Nation estate.
	 * Removes borders, unregisters parcels from ParcelRegistry, and unregisters
	 * their estates from EstateRegistry.
	 * No deeds are returned — tenants are destroyed when the Nation is demolished.
	 *
	 * @author Mark Gottschling on Apr 6, 2026
	 */
	public static void cleanupNationTenants(ServerLevel level, Estate nationEstate) {
		if (!nationEstate.isNation()) return;

		Set<Parcel> tenantParcels = ParcelRegistry.findAllByNationEstateId(nationEstate.getId());
		// collect tenant estates before unregistering parcels
		Set<Estate> tenantEstates = tenantParcels.stream()
				.map(Parcel::getEstate)
				.collect(Collectors.toSet());

		tenantParcels.forEach(parcel -> {
			ParcelRegistry.unregisterParcel(level, parcel);
		});

		// unregister tenant estates
		tenantEstates.forEach(EstateRegistry::unregister);
	}

	// =====================================================================
	// SENDING GATEWAY
	// all command output flows through these methods. direct calls to
	// source.sendSuccess() / source.sendFailure() in command classes are
	// prohibited — use these helpers instead.
	// =====================================================================

	/**
	 * sends every line in the supplied list to the player via
	 * {@code source.sendSuccess()}. This is the single gateway through which
	 * all formatted command output is delivered.
	 *
	 * @param source the command source
	 * @param lines  the pre-built lines from any formatter
	 */
	public static void sendLines(CommandSourceStack source, List<Component> lines) {
		for (Component line : lines) {
			source.sendSuccess(() -> line, false);
		}
	}

	/**
	 * sends error lines: the first line goes through {@code source.sendFailure()}
	 * so it is flagged in the operator feedback log; all subsequent lines go
	 * through {@code source.sendSuccess()} so they are visible to the player.
	 *
	 * <p>this is intentional — visual colouring (red bold) is already embedded
	 * in the Component styles produced by {@link CommandResponseFormatter}.</p>
	 *
	 * @param source the command source
	 * @param lines  the pre-built lines from {@link CommandResponseFormatter#formatError}
	 */
	private static void sendErrorLines(CommandSourceStack source, List<Component> lines) {
		if (lines.isEmpty()) return;
		source.sendFailure(lines.get(0));
		lines.subList(1, lines.size()).forEach(line -> source.sendSuccess(() -> line, false));
	}

	/**
	 * sends a boxed green success response (shorthand — title line only, no body).
	 * use for simple outcomes where the title is self-explanatory.
	 *
	 * @param source   the command source
	 * @param titleKey lang key for the title (e.g. "parcel.demolish.success")
	 */
	public static void sendSuccess(CommandSourceStack source, String titleKey) {
		sendLines(source, CommandResponseFormatter.formatSuccess(titleKey));
	}

	/**
	 * sends a boxed green success response with a body line.
	 *
	 * @param source   the command source
	 * @param titleKey lang key for the bold white title
	 * @param bodyKey  lang key for the grey body message
	 * @param bodyArgs optional format arguments for the body translation
	 */
	public static void sendSuccess(CommandSourceStack source, String titleKey, String bodyKey, Object... bodyArgs) {
		sendLines(source, CommandResponseFormatter.formatSuccess(titleKey, bodyKey, bodyArgs));
	}

	/**
	 * sends a boxed red error response (shorthand — title line only, no body).
	 * Use for simple failures where the title is self-explanatory.
	 *
	 * <p>routes through {@code source.sendFailure()} for the first (header) line
	 * so it is flagged correctly in the operator feedback log, then
	 * {@code source.sendSuccess()} for the body lines so they reach the player.</p>
	 *
	 * @param source   the command source
	 * @param titleKey lang key for the title (e.g. "estate.demolish.failure")
	 */
	public static void sendFailure(CommandSourceStack source, String titleKey) {
		sendErrorLines(source, CommandResponseFormatter.formatFailure(titleKey));
	}

	/**
	 * sends a boxed red error response with a body line.
	 *
	 * @param source   the command source
	 * @param titleKey lang key for the bold white title
	 * @param bodyKey  lang key for the grey body message
	 * @param bodyArgs optional format arguments for the body translation
	 */
	public static void sendFailure(CommandSourceStack source, String titleKey, String bodyKey, Object... bodyArgs) {
		sendErrorLines(source, CommandResponseFormatter.formatFailure(titleKey, bodyKey, bodyArgs));
	}

	/**
	 * sends a boxed yellow warning response (shorthand — title line only, no body).
	 *
	 * @param source   the command source
	 * @param titleKey lang key for the title
	 */
	public static void sendWarning(CommandSourceStack source, String titleKey) {
		sendLines(source, CommandResponseFormatter.formatWarning(titleKey));
	}

	/**
	 * sends a boxed yellow warning response with a body line.
	 *
	 * @param source   the command source
	 * @param titleKey lang key for the bold white title
	 * @param bodyKey  lang key for the grey body message
	 * @param bodyArgs optional format arguments for the body translation
	 */
	public static void sendWarning(CommandSourceStack source, String titleKey, String bodyKey, Object... bodyArgs) {
		sendLines(source, CommandResponseFormatter.formatWarning(titleKey, bodyKey, bodyArgs));
	}

	// =====================================================================
	// CONVENIENCE MESSAGE WRAPPERS
	// these cover the common cases that appear across many commands. all
	// delegate to sendFailure / sendLines above — never to source directly.
	// =====================================================================

	/**
	 * sends the standard "unexpected error" failure message.
	 * Use in catch blocks where no more specific message is available.
	 */
	public static void unexpectedError(CommandSourceStack source) {
		sendFailure(source, "unexpected_error");
	}

	/**
	 * sends a boxed red error response for the given lang key.
	 * prefer the typed {@link #sendFailure(CommandSourceStack, String)} overload;
	 * this exists for backward compatibility with existing call sites.
	 *
	 * @param source the command source
	 * @param key    the lang key passed to {@link LangUtil#chat(String)}
	 */
	public static void failure(CommandSourceStack source, String key) {
		sendFailure(source, key);
	}

	/**
	 * sends the standard "unable to locate player" failure message with the
	 * player name embedded.
	 *
	 * @param source the command source
	 * @param name   the player name that could not be found
	 */
	public static void sendUnableToLocatePlayerMessage(CommandSourceStack source, String name) {
		sendFailure(source, "unable_locate_player", "unable_locate_player.body", name);
	}

	/**
	 * sends the standard "unable to locate player" failure message without a
	 * specific name (used when the name is not available at the call site).
	 *
	 * @param source the command source
	 */
	public static void sendUnableToLocatePlayerMessage(CommandSourceStack source) {
		sendFailure(source, "unable_locate_player");
	}

	/**
	 * sends the standard "deed generate failure" failure message.
	 *
	 * @param source     the command source
	 * @param nationName the nation name involved (currently unused in the message
	 *                   body but retained for future use)
	 */
	public static void sendUnableToGenerateDeedMessage(CommandSourceStack source, String nationName) {
		sendFailure(source, "deed.generate.failure");
	}

	// =====================================================================
	// LOOKUP / COORDINATION UTILITIES
	// =====================================================================

	/**
	 * Marks persistent data as dirty so Minecraft will auto-save it.
	 *
	 * @param level the current server level
	 */
	public static void save(Level level) {
		// PersistedData uses DimensionDataStorage which is per-dimension.
		// ParcelRegistry is a global singleton stored in the Overworld — always
		// resolve to the Overworld ServerLevel regardless of the caller's dimension.
		Level overworld = level instanceof ServerLevel serverLevel
				? serverLevel.getServer().overworld()
				: level;
		PersistedData savedData = PersistedData.get(overworld);
		if (savedData != null) {
			savedData.setDirty();
		}
	}

	public static Optional<Parcel> findParcelByOwnerEstate(CommandSourceStack source,
														   UUID ownerUuid,
														   String estateName,
														   String parcelName) {
		Optional<Estate> optionalEstate = getEstateByOwner(source, ownerUuid, estateName);
		return optionalEstate.flatMap(estate -> estate.findParcels().stream()
				.filter(p -> p.getName().equalsIgnoreCase(parcelName))
				.findFirst());
	}

	public static Optional<Estate> getEstateByOwner(CommandSourceStack source,
													UUID ownerUuid,
													String estateName) {

		return getEstatesByOwner(source, ownerUuid).stream()
				.filter(e -> e.getName().equalsIgnoreCase(estateName))
				.findFirst();
	}

	public static Set<Estate> getEstatesByOwner(CommandSourceStack source, UUID playerUuid) {
		return EstateRegistry.findByOwner(playerUuid);
	}

	/**
	 * Returns the calling player, throwing {@link CommandSyntaxException} if the
	 * source is not a player entity.
	 */
	public static ServerPlayer getPlayer(CommandSourceStack source) throws CommandSyntaxException {
		return source.getPlayerOrException();
	}

	public static Optional<UUID> getPlayerUuid(CommandSourceStack source) {
		try {
			return Optional.of(getPlayer(source).getUUID());
		} catch (CommandSyntaxException e) {
			return Optional.empty();
		}
	}

	public static Optional<UUID> getPlayerUuid(CommandSourceStack source, String playerName) {
		return PlayerRegistry.getPlayerUuid(source.getLevel(), playerName);
	}

	public static Optional<String> getPlayerName(CommandSourceStack source, UUID playerUuid) {
		return PlayerRegistry.getPlayerName(source.getLevel(), playerUuid);
	}

	/**
	 * Force an immediate write of the server config to disk. Call after
	 * {@code ConfigValue.set(...)} when durability cannot wait for the
	 * NeoForge config system's natural save cycle.
	 *
	 * @author Mark Gottschling on Apr 19, 2026
	 */
	public static void saveServerConfig() {
		Config.SERVER_SPEC.save();
	}
}