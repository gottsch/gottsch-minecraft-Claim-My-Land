package mod.gottsch.forge.claimmyland.core.tags;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import javax.swing.text.html.parser.Entity;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author Mark Gottschling Feb 23, 2025
 *
 */
public class ModTags {
	
	public static class Items {
		public static final TagKey<Item> COMMON_NATION_WHITELIST = mod(ClaimMyLand.MOD_ID, "common_nation_item_whitelist");
		public static final TagKey<Item> MAGEFLAME_SCROLLS_WHITELIST = mod(ClaimMyLand.MOD_ID, "mageflame_scrolls_whitelist");
		public static final TagKey<Item> TREASURE2_KEYS_WHITELIST = mod(ClaimMyLand.MOD_ID, "treasure2_keys_whitelist");

		public static final List<TagKey<Item>> ITEM_TAG_WHITELISTS =
				Arrays.asList(COMMON_NATION_WHITELIST,
						MAGEFLAME_SCROLLS_WHITELIST,
						TREASURE2_KEYS_WHITELIST);

		public static TagKey<Item> mod(String domain, String path) {
			return ItemTags.create(new ResourceLocation(domain, path));
		}
	}

	/*
	 *
	 */
	public static class Blocks {
		public static final TagKey<Block> COMMON_NATION_WHITELIST = mod(ClaimMyLand.MOD_ID, "common_nation_block_whitelist");
		public static final TagKey<Block> CHEST_BARREL_WHITELIST = mod(ClaimMyLand.MOD_ID, "chest_barrel_whitelist");
		public static final TagKey<Block> CRAFTING_WHITELIST = mod(ClaimMyLand.MOD_ID, "crafting_whitelist");
		public static final TagKey<Block> DOOR_GATE_WHITELIST = mod(ClaimMyLand.MOD_ID, "door_gate_whitelist");

		// integration tags
		public static final TagKey<Block> TREASURE2_CHEST_WHITELIST = mod(ClaimMyLand.MOD_ID, "treasure2_chest_whitelist");
		public static final TagKey<Block> LEGACY_VAULT_WHITELIST = mod(ClaimMyLand.MOD_ID, "legacy_vault_whitelist");
		public static final TagKey<Block> MACAWS_FURNITURE_WHITELIST = mod(ClaimMyLand.MOD_ID, "macaws_furniture_whitelist");


		// TODO add an Api class that you can register tags to
		/**
		 * a convenience list for suggestions in commands
		 */
		public static final List<TagKey<Block>> BLOCK_TAG_WHITELISTS =
				Arrays.asList(COMMON_NATION_WHITELIST,
						CHEST_BARREL_WHITELIST,
						CRAFTING_WHITELIST,
						DOOR_GATE_WHITELIST,
						TREASURE2_CHEST_WHITELIST,
						LEGACY_VAULT_WHITELIST,
						MACAWS_FURNITURE_WHITELIST);

		public static TagKey<Block> mod(String domain, String path) {
			return BlockTags.create(new ResourceLocation(domain, path));
		}
	}

	public static class Entities {
		public static final TagKey<EntityType<?>> FARM_ENTITIES = mod(ClaimMyLand.MOD_ID, "farm_entities");
		public static final TagKey<EntityType<?>> NEUTRAL_ENTITIES = mod(ClaimMyLand.MOD_ID, "neutral_entities");
		public static final TagKey<EntityType<?>> ENTITY_SPAWN_WHITELIST = mod(ClaimMyLand.MOD_ID, "entity_spawn_whitelist");

		public  static final List<TagKey<EntityType<?>>> ENTITY_SPAWN_TAG_WHITELISTS =
				Arrays.asList(FARM_ENTITIES,
						NEUTRAL_ENTITIES,
						ENTITY_SPAWN_WHITELIST);

		public static TagKey<EntityType<?>> mod(String domain, String path) {
			return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(domain, path));
		}
	}
}
