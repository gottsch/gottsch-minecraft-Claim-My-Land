package mod.gottsch.neo.claimmyland.datagen;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.loot.DeedLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

/**
 * DataGen provider for CML deed loot modifier JSON files.
 * Generates one file per tier under data/neoforge/loot_modifiers/.
 *
 * @author Mark Gottschling on Apr 22, 2026
 */
public class LootModifierProvider extends GlobalLootModifierProvider {

    public LootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ClaimMyLand.MOD_ID);
    }

    @Override
    protected void start() {

        // common (15%) — early-game chests
        add("deed_loot_common", new DeedLootModifier(
                conditions(
                        "minecraft:chests/simple_dungeon",
                        "minecraft:chests/nether_bridge",
                        "minecraft:chests/village/village_weaponsmith",
                        "minecraft:gameplay/fishing_treasure",
                        // YUNG's Better Dungeons
                        "betterdungeons:small_dungeon/chests/loot_piles",
                        "betterdungeons:skeleton_dungeon/chests/common",
                        "betterdungeons:zombie_dungeon/chests/common"
                ),
                0.15f, "common"
        ));

        // uncommon (12%) — mid-game chests
        add("deed_loot_uncommon", new DeedLootModifier(
                conditions(
                        "minecraft:chests/stronghold_corridor",
                        "minecraft:chests/bastion_treasure",
                        // YUNG's Better Dungeons
                        "betterdungeons:small_nether_dungeon/chests/common",
                        "betterdungeons:skeleton_dungeon/chests/middle",
                        "betterdungeons:spider_dungeon/chests/egg_room",
                        "betterdungeons:zombie_dungeon/chests/tombstone",
                        // Twilight Forest
                        "twilightforest:chests/basement",
                        "twilightforest:chests/tower_enchanting",
                        "twilightforest:chests/tower_foyer",
                        "twilightforest:chests/tower_grave_lower",
                        "twilightforest:chests/tower_grave_upper",
                        "twilightforest:chests/tower_jars",
                        "twilightforest:chests/tower_potion"
                ),
                0.12f, "uncommon"
        ));

        // rare (8%) — late-game chests
        add("deed_loot_rare", new DeedLootModifier(
                conditions(
                        "minecraft:chests/stronghold_library",
                        "minecraft:chests/woodland_mansion",
                        // YUNG's Better Dungeons
                        "betterdungeons:zombie_dungeon/chests/special",
                        // Twilight Forest
                        "twilightforest:chests/tower_library",
                        "twilightforest:chests/tower_room"
                ),
                0.08f, "rare"
        ));

        // epic (5%) — end-game chests
        add("deed_loot_epic", new DeedLootModifier(
                conditions(
                        "minecraft:chests/end_city_treasure",
                        "minecraft:chests/ancient_city"
                ),
                0.05f, "epic"
        ));
    }

    /**
     * Builds a single-element conditions array whose sole condition is an
     * {@code any_of} wrapping one {@code neoforge:loot_table_id} check per
     * supplied table path.
     */
    private static LootItemCondition[] conditions(String... tableIds) {
        LootItemCondition.Builder[] builders = new LootItemCondition.Builder[tableIds.length];
        for (int i = 0; i < tableIds.length; i++) {
            builders[i] = LootTableIdCondition.builder(ResourceLocation.parse(tableIds[i]));
        }
        return new LootItemCondition[]{AnyOfCondition.anyOf(builders).build()};
    }
}