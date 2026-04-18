/*
 * This file is part of  Protect It.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.claimmyland.core.config;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.util.StructurePolicyFactory;
import mod.gottsch.forge.gottschcore.config.AbstractConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
@EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config extends AbstractConfig {
	public static final String GENERAL_CATEGORY = "general";
	public static final String PROTECTION_CATEGORY = "protection";
	public static final String UNDERLINE_DIV = "------------------------------";

	public static final ForgeConfigSpec COMMON_SPEC;
	public static final CommonConfig COMMON;

	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final ClientConfig CLIENT;

	public static final ForgeConfigSpec SERVER_SPEC;
	public static final ServerConfig SERVER;

	// setup as a singleton
	public static Config instance = new Config();
	
	static {
		final Pair<CommonConfig, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder()
				.configure(CommonConfig::new);
		COMMON_SPEC = commonSpecPair.getRight();
		COMMON = commonSpecPair.getLeft();

		final Pair<ClientConfig, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder()
				.configure(ClientConfig::new);
		CLIENT_SPEC = clientSpecPair.getRight();
		CLIENT = clientSpecPair.getLeft();

		final Pair<ServerConfig, ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder()
				.configure(ServerConfig::new);
		SERVER_SPEC = serverSpecPair.getRight();
		SERVER = serverSpecPair.getLeft();
	}

	/**
	 *
	 */
	public static void register() {
		registerCommonConfig();
		registerClientConfig();
		registerServerConfig();
	}

	private static void registerCommonConfig() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
	}

	private static void registerClientConfig() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
	}

	private static void registerServerConfig() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
	}

	@SubscribeEvent
	public static void onConfigReload(ModConfigEvent.Reloading event) {
		if (event.getConfig().getSpec() == SERVER_SPEC) {
			StructurePolicyFactory.invalidate();
			ClaimMyLand.reinitBackup();
		}
	}

	/*
	 *
	 */
	public static class CommonConfig {
		public Logging logging;
		public CommonConfig(ForgeConfigSpec.Builder builder) {
			logging = new Logging(builder);
		}
	}

	public static class ClientConfig {
		public Gui gui;
		public Rendering rendering;

		public ClientConfig(ForgeConfigSpec.Builder builder) {

			gui = new Gui(builder);
			rendering = new Rendering(builder);
		}
	}

	public static class ServerConfig {
		public General general;
		public Borders borders;
		public Protection protection;
		public Backup backup;
		public StructureProtection structureProtection;
		public Dimensions dimensions;
		public CelebrationConfig celebration;

		public BooleanValue preventFireSpread;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
			general = new General(builder);
			borders = new Borders(builder);
			protection = new Protection(builder);
			backup = new Backup(builder);
			structureProtection = new StructureProtection(builder);
			dimensions = new Dimensions(builder);
			celebration = new CelebrationConfig(builder);
		}
	}

	@Override
	public String getLogsFolder() {
		return COMMON.logging.folder.get();
	}

	@Override
	public String getLogSize() {
		return COMMON.logging.size.get();
	}

	@Override
	public String getLoggingLevel() {
		return COMMON.logging.level.get();
	}

	/*
	 * 
	 */
	public static class Gui {
		public BooleanValue enableProtectionChatMessages;
		public ForgeConfigSpec.BooleanValue enableParcelEntryTitle;
		public ForgeConfigSpec.IntValue parcelEntryCooldownSeconds;
		public ForgeConfigSpec.IntValue parcelEntryFadeInTicks;
		public ForgeConfigSpec.IntValue parcelEntryStayTicks;
		public ForgeConfigSpec.IntValue parcelEntryFadeOutTicks;
		public ForgeConfigSpec.BooleanValue showParcelHud;

		public Gui(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, " Client GUI properties for Protect It  mod.", CATEGORY_DIV).push("GUI");
			enableProtectionChatMessages = builder
					.comment(" Enables protection messages in chat. If enabled, when protection is triggered, a message will display in the chat.")
					.define("enableProtectionChatMessages", false);

			enableParcelEntryTitle = builder
					.comment("If true, a title overlay is shown when entering a Nation, Citizen,",
							"or Player parcel. The server config enableParcelEntryTitle must also",
							"be true for this to have any effect.",
							"Default: true.")
					.define("enableParcelEntryTitle", true);

			parcelEntryCooldownSeconds = builder
					.comment("Seconds before the parcel entry title re-fires for the same parcel.",
							"Default: 30.")
					.defineInRange("parcelEntryCooldownSeconds", 30, 5, 300);

			parcelEntryFadeInTicks = builder
					.comment("Fade-in duration in ticks for the parcel entry title.",
							"Default: 10.")
					.defineInRange("parcelEntryFadeInTicks", 10, 0, 40);

			parcelEntryStayTicks = builder
					.comment("How long the parcel entry title stays on screen, in ticks.",
							"Default: 60.")
					.defineInRange("parcelEntryStayTicks", 60, 10, 200);

			parcelEntryFadeOutTicks = builder
					.comment("Fade-out duration in ticks for the parcel entry title.",
							"Default: 20.")
					.defineInRange("parcelEntryFadeOutTicks", 20, 0, 40);

			showParcelHud = builder
					.comment("Show the in-world parcel HUD overlay when standing inside a claimed parcel.",
							"Disable to hide the HUD without affecting gameplay or server behavior.")
					.translation("config.claimmyland.client.show_parcel_hud")
					.define("showParcelHud", true);

			builder.pop();
		}		
	}
	
	/**
	 * 
	 * @author Mark Gottschling on Nov 3, 2021
	 *
	 */
	public static class General {
		public IntValue parcelsPerPlayer;
		public IntValue opsPermissionLevel;
		public IntValue parcelBufferRadius;
		public IntValue nationParcelBufferRadius;
		public BooleanValue allowMojangNameCalls;
		public BooleanValue enableParcelEntryTitle;
		public BooleanValue foundationStoneCentered;
		public BooleanValue enableDeedLoot;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> opsList;

		General(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, " General properties for Protect It  mod.", CATEGORY_DIV).push(GENERAL_CATEGORY);

			parcelsPerPlayer = builder
					.comment(" The number of parcels each player can own per world.")
					.defineInRange("parcelsPerPlayer", 5, 1, 100);
			opsPermissionLevel = builder
					.comment(" The permission level required to be Ops within Claim My Land.","This is not the op-permission-level that is set in the server.propeties.",
							" This allows players who are not server-level ops, to have Claim My Land Ops permissions. ie protections don't protect against Ops.",
							" Ex. server-level ops = 4, but Claim My Land ops = 3 - a player with permission 3 would be considered an Ops within Claim My Land.")
					.defineInRange("opsPermissionLevel", 4, 0, 4);

			parcelBufferRadius = builder
					.comment(" A buffer between parcels. Another parcel cannot be built within this area.",
							" This is a radius beyond (or in addition to) the parcel border.",
							" Ex. parcel size = 10x10x10, with a buffer radius = 3. The total size = 13x13x13",
							" that another parcel cannot build within.",
							" Note that the buffer is between parcel borders, not other buffers, meaning if 2 parcels",
							" both have a buffer = 3, there is a buffer of 3 between the parcels, not 6.")
							.defineInRange("parcelBufferRadius", 3, 1, 10);

			nationParcelBufferRadius = builder
					.comment(" Like 'parcelBufferRadius', but for Nation parcels.")
							.defineInRange("nationParcelBufferRadius", 10, 1, 50);

			allowMojangNameCalls = builder
					.comment(" Allows external HTTP calls to Mojang's web API (ex. https://api.mojang.com/users/profiles/minecraft/)",
							" This is used to find offline player's UUID by name, or visa versa.")
					.define("allowMojangNameCalls", true);

			enableParcelEntryTitle = builder
					.comment("If true, clients receive a title overlay when crossing into a",
							"Nation, Citizen, or Player parcel. Server master switch —",
							"disabling this prevents all clients from seeing titles regardless",
							"of their individual client settings.",
							"Default: true.")
					.define("enableParcelEntryTitle", true);

			foundationStoneCentered = builder
					.comment("If true, the Foundation Stone is placed at the XZ centre of the parcel.",
							"If false, the stone is placed at the min corner (legacy behaviour).",
							"Default: true.")
					.define("foundationStoneCentered", true);

			opsList = builder
					.comment("List of player UUIDs with CML ops-level permission.",
							"These players can use /cml-ops commands without being server operators.",
							"Format: UUID strings e.g. [\"550e8400-e29b-41d4-a716-446655440000\"]",
							"Default: empty list.")
					.defineListAllowEmpty("opsList", Collections.emptyList(), o -> o instanceof String);

			enableDeedLoot = builder
					.comment("If true, deeds can appear in loot tables (chests, mob drops, fishing, etc.).",
							"Disable to prevent deeds from appearing in loot if you prefer admin-only distribution.",
							"Default: true.")
					.define("enableDeedLoot", true);

			builder.pop();
		}
	}

	public static class Borders {
		public ForgeConfigSpec.IntValue borderStoneLifeSpan;
		public ForgeConfigSpec.IntValue foundationStoneLifeSpan;
		public ForgeConfigSpec.IntValue nationBorderHeight;
		// default 20, min 1, max 256

		Borders(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, " General properties for Protect It  mod.", CATEGORY_DIV).push(GENERAL_CATEGORY);

			borderStoneLifeSpan = builder
					.comment(" The life span of a border stone in ticks.")
					.defineInRange("borderStoneLifeSpan", 6000, 1200, Integer.MAX_VALUE);

			foundationStoneLifeSpan = builder
					.comment(" The life span of a foundation stone in ticks.")
					.defineInRange("foundationStoneLifeSpan", 6000, 1200, Integer.MAX_VALUE);

			nationBorderHeight = builder
					.comment(" Height in blocks of the visual border and buffer wall for Nation parcels.")
					.defineInRange("nationBorderHeight", 20, 1, 256);

			builder.pop();
		}
	}

	public static class Protection {
		public BooleanValue enableBlockBreakEvent;
		public BooleanValue enableEntityPlaceEvent;
		public BooleanValue enableEntityMultiPlaceEvent;
		public BooleanValue enableBlockToolInteractEvent;
		public BooleanValue enableRightClickBlockEvent;
		public BooleanValue enableRightClickItemEvent;
		public BooleanValue enableLivingDestroyBlockEvent;
		public BooleanValue enablePistionEvent;
		public BooleanValue enableExplosionDetonateEvent;
		public BooleanValue preventFireSpread;
		public BooleanValue enableFarmlandTrampleEvent;
		public BooleanValue enableChorusFruitTeleport;

		Protection(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, 
					" Protection properties for Claim My Land mod.",
					" Note: these config settings are for enabling the protections of the parcel, not for enabling the actions.",
					" ex. Block break protection = true, enables the PROTECTION AGAINST breaking blocks in the property,",
					" it does NOT enable the player TO BREAK a block.",
					CATEGORY_DIV).push(PROTECTION_CATEGORY);
			
			enableBlockBreakEvent = builder
					.comment(" Enables block break protection. If enabled, blocks in property are protected from being broken by others.")
					.define("enableBlockBreakProtection", true);
			
			enableEntityPlaceEvent = builder
					.comment(" Enables block placement protection. If enabled, blocks are not allowed to be placed in the property by others.")
					.define("enableBlockPlacementProtection", true);
			
			enableEntityMultiPlaceEvent = builder
					.comment(" Enables multi-block placement protection. If enabled, multi-blocks are not allowed to be placed in the property by others.")
					.define("enableMultiBlockPlacementProtection", true);

			enableBlockToolInteractEvent = builder					
					.comment(" Enables block tool interaction protection. If enabled, blocks in the property will not change state when right-clicked with tool. Ex. axe will not strip a log.")
					.define("enableBlockToolInteractProtection", true);
			
			enableRightClickBlockEvent = builder					
					.comment(" Enables right click block protection. If enabled, blocks in the property will not perform any action if right-clicked. Ex. chests will not open for others.")
					.define("enableRightClickBlockProtection", true);

			enableRightClickItemEvent = builder
					.comment(" Enables right click item protection. If enabled, item use in the property will not perform any action if right-clicked.")
					.define("enableRightClickItemProtection", true);

			enableLivingDestroyBlockEvent = builder
					.comment(" Enables block break protection from living entities. If enabled, blocks in the property  are protected from being broken for living entities (mobs).")
					.define("enableLivingDestroyBlockProtection", true);
			
			enablePistionEvent = builder
					.comment(" Enables piston movement protection. If enabled, pistons outside the property will not fire if their movement would move protected blocks.")
					.define("enablePistonMovementProtection", true);
			
			enableExplosionDetonateEvent = builder
					.comment(" Enables explosion protection. If enabled, explosions will not destory protected blocks.")
					.define("enableExplosionProtection", true);

			preventFireSpread = builder
					.comment(" Prevents fire from spreading within claimed parcels.",
							" Can be overridden per-estate.",
							" Default: true.")
					.define("preventFireSpread", true);

			enableFarmlandTrampleEvent = builder
					.comment("If true, prevents players and entities from trampling farmland inside protected parcels.",
							"Default: true.")
					.define("enableFarmlandTrampleEvent", true);

			enableChorusFruitTeleport = builder
					.comment("If true, prevents players from teleporting into protected parcels via chorus fruit.",
							"Default: true.")
					.define("enableChorusFruitTeleport", true);

			builder.pop();
		}
	}

	public static class Backup {
		public final BooleanValue enabled;
		public final IntValue intervalMinutes;
		public final IntValue maxFiles;

		Backup(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV,
					" Backup properties for Claim My Land mod.",
					CATEGORY_DIV).push("backup");

			enabled = builder
					.comment(" Enable the rolling JSON backup system.",
							" Backups are written to world/data/claimmyland/backups/")
					.define("enabled", true);

			intervalMinutes = builder
					.comment(" How often (in minutes) a backup is written.",
							" Minimum: 1. Default: 5.")
					.defineInRange("intervalMinutes", 5, 1, 1440);

			maxFiles = builder
					.comment(" Maximum number of backup files to keep.",
							" Oldest files are deleted automatically.",
							" Minimum: 1. Default: 20.")
					.defineInRange("maxFiles", 20, 1, 500);

			builder.pop();
		}
	}

	public static class StructureProtection {
		public final BooleanValue enabled;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> denyStructures;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> warnStructures;

		StructureProtection(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV,
					" Structure protection properties for Claim My Land mod.",
					CATEGORY_DIV).push("structureProtection");

			enabled = builder
					.comment(" When true, players cannot claim land that overlaps certain structures.",
							" See denyStructures and warnStructures to customise which ones.")
					.define("enabled", true);

			denyStructures = builder
					.comment(" Structures that cannot be claimed over. Claim is hard-rejected.",
							" Use Minecraft resource location format.",
							" Prefix with # for a structure tag.",
							" Default: Stronghold, Nether Fortress, Bastion Remnant, End City.",
							" Example: [\"#minecraft:eye_of_ender_located\", \"minecraft:fortress\"]")
					.defineListAllowEmpty("denyStructures",
							List.of(
									"#minecraft:eye_of_ender_located",
									"minecraft:fortress",
									"minecraft:bastion_remnant",
									"minecraft:end_city"
							),
							String.class::isInstance);

			warnStructures = builder
					.comment(" Structures that trigger a warning but still allow the claim.",
							" Default: Village, Woodland Mansion, Ocean Monument.",
							" Example: [\"#minecraft:village\", \"minecraft:mansion\"]")
					.defineListAllowEmpty("warnStructures",
							List.of(
									"#minecraft:village",
									"#minecraft:on_woodland_explorer_maps",
									"#minecraft:on_ocean_explorer_maps"
							),
							String.class::isInstance);

			builder.pop();
		}
	}

	public static class Dimensions {
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> excludedDimensions;

		Dimensions(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV,
					" Dimension properties for Claim My Land mod.",
					CATEGORY_DIV).push("dimensions");

			excludedDimensions = builder
					.comment(" Dimensions where land claiming is completely disabled.",
							" Use Minecraft resource location format.",
							" Default is empty — all dimensions are claimable.",
							" Example: [\"minecraft:the_nether\", \"minecraft:the_end\"]")
					.defineListAllowEmpty("excludedDimensions",
							List.of(),
							String.class::isInstance);

			builder.pop();
		}
	}

	public static class CelebrationConfig {
		public final ForgeConfigSpec.BooleanValue fireworksEnabled;

		CelebrationConfig(ForgeConfigSpec.Builder builder) {
			builder.push("celebration");
			fireworksEnabled = builder
					.comment("If true, fireworks are launched at the parcel centre on claim commit.",
							"Default: true.")
					.define("fireworksEnabled", true);
			builder.pop();
		}
	}

	public static class Rendering {
		public IntValue borderRenderRadius;
		public final ForgeConfigSpec.IntValue conflictHighlightTimeoutSeconds;

		Rendering(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV,
					" Rendering properties for Claim My Land mod.",
					CATEGORY_DIV).push("rendering");

			borderRenderRadius = builder
					.comment(" Maximum radius (in blocks) within which parcel borders are rendered.",
							" Parcels whose buffer boundary is entirely outside this radius are skipped.",
							" Default: 256.")
					.defineInRange("borderRenderRadius", 256, 64, 2048);

			conflictHighlightTimeoutSeconds = builder
					.comment(" How long (in seconds) conflict highlights remain visible after a Foundation Stone",
							" preview clears. Highlights also clear if the player moves 32+ blocks away.",
							" Range: 10–300. Default: 60.")
					.defineInRange("conflictHighlightTimeoutSeconds", 60, 10, 300);

			builder.pop();
		}
	}
}
