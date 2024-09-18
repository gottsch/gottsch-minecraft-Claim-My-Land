
package mod.gottsch.forge.claimmyland.datagen;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 
 * @author Mark Gottschling on Sep 16, 2024
 *
 */
public class BlockStates extends BlockStateProvider {

	public BlockStates(PackOutput gen, ExistingFileHelper helper) {
        super(gen, ClaimMyLand.MOD_ID, helper);
    }
	
	@Override
	protected void registerStatesAndModels() {

//		simpleBlock(ModBlocks.FOUNDATION_STONE.get());
		simpleBlock(ModBlocks.PERSONAL_FOUNDATION_STONE.get());
//		simpleBlock(ProtectItBlocks.GOOD_BORDER.get());
	}

}
