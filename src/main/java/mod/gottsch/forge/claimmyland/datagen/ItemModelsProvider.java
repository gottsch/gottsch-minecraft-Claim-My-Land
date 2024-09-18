
package mod.gottsch.forge.claimmyland.datagen;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 
 * @author Mark Gottschling Mar 19, 2024
 *
 */
public class ItemModelsProvider extends ItemModelProvider {

	public ItemModelsProvider(PackOutput generator, ExistingFileHelper existingFileHelper) {
		super(generator, ClaimMyLand.MOD_ID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		// tabs

		// deeds
		singleTexture(
				"personal_deed",
				mcLoc("item/generated"), "layer0", modLoc("item/personal_deed"));
		singleTexture(
				"nation_deed",
				mcLoc("item/generated"), "layer0", modLoc("item/nation_deed"));
		singleTexture(
				"citizen_deed",
				mcLoc("item/generated"), "layer0", modLoc("item/citizen_deed"));

		// blocks
	}
}
