
package mod.gottsch.neo.claimmyland.datagen;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 
 * @author Mark Gottschling on Sep 16, 2024
 *
 */
@EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

		if (event.includeServer()) {
//			generator.addProvider(event.includeServer(), new Recipes(output));
			ModBlockTagsProvider blockTags = new ModBlockTagsProvider(output, lookupProvider, event.getExistingFileHelper());
			generator.addProvider(true, blockTags);
			generator.addProvider(true, new ModItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), event.getExistingFileHelper()));
			generator.addProvider(true, new ModEntityTypeTagsProvider(output, lookupProvider, event.getExistingFileHelper()));
			generator.addProvider(true, ModLootTableProvider.create(output, lookupProvider));
			event.getGenerator().addProvider(
					event.includeServer(),
					new ModRecipesProvider(output, event.getLookupProvider())
			);
			event.getGenerator().addProvider(
					event.includeServer(),
					new LootModifierProvider(output, event.getLookupProvider())
			);
		}
		if (event.includeClient()) {
			generator.addProvider(event.includeClient(), new BlockStates(output, event.getExistingFileHelper()));
			generator.addProvider(event.includeClient(), new ItemModelsProvider(output, event.getExistingFileHelper()));
			generator.addProvider(event.includeClient(), new LanguageGen(output, "en_us"));
		}
	}
}