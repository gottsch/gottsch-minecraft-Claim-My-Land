
package mod.gottsch.forge.claimmyland.datagen;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

/**
 * 
 * @author Mark Gottschling on Sep 16, 2024
 *
 */
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
//			generator.addProvider(true, new ItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), event.getExistingFileHelper()));

		}
		if (event.includeClient()) {
			generator.addProvider(event.includeClient(), new BlockStates(output, event.getExistingFileHelper()));
			generator.addProvider(event.includeClient(), new ItemModelsProvider(output, event.getExistingFileHelper()));
			generator.addProvider(event.includeClient(), new LanguageGen(output, "en_us"));
		}
	}
}