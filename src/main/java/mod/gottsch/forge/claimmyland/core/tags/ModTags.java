package mod.gottsch.forge.claimmyland.core.tags;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

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

		// TODO add an Api class that you can register tags to
		public static final List<TagKey<Block>> BLOCK_TAG_WHITELISTS =
				Arrays.asList(COMMON_NATION_WHITELIST,
						CHEST_BARREL_WHITELIST,
						CRAFTING_WHITELIST,
						DOOR_GATE_WHITELIST,
						TREASURE2_CHEST_WHITELIST,
						LEGACY_VAULT_WHITELIST);

		public static TagKey<Block> mod(String domain, String path) {
			return BlockTags.create(new ResourceLocation(domain, path));
		}
	}
}
