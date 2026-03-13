
package mod.gottsch.forge.claimmyland.datagen;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.BorderStone;
import mod.gottsch.forge.claimmyland.core.block.FoundationStone;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.ModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 *
 * @author Mark Gottschling on Sep 16, 2024
 *
 */
public class BlockStates extends BlockStateProvider {

	public BlockStates(PackOutput gen, ExistingFileHelper helper) {
		super(gen, ClaimMyLand.MOD_ID, helper);
	}
	private static final int DEFAULT_ANGLE_OFFSET = 180;

	@Override
	protected void registerStatesAndModels() {

		foundationStone(ModBlocks.PLAYER_FOUNDATION_STONE, mcLoc("block/polished_andesite"), mcLoc("block/andesite"));
		foundationStone(ModBlocks.CITIZEN_FOUNDATION_STONE, mcLoc("block/polished_diorite"), mcLoc("block/diorite"));
		foundationStone(ModBlocks.NATION_FOUNDATION_STONE, mcLoc("block/polished_granite"), mcLoc("block/granite"));
	}

	public void foundationStone(RegistryObject<Block> block, ResourceLocation polished, ResourceLocation stone) {
		String name = block.getId().getPath();
		myFoundationStone(name, (FoundationStone)block.get(), polished, stone, "minecraft:cutout");
	}

	private void myFoundationStone(String name, FoundationStone block, ResourceLocation polished, ResourceLocation stone, String renderType) {
		ModelFile model = models().withExistingParent(name, modLoc(ModelProvider.BLOCK_FOLDER + "/foundation_stone")).texture("2", polished).texture("9", stone).renderType(renderType);

		getVariantBuilder(block).forAllStates(state -> {
			Direction facing = state.getValue(FoundationStone.FACING);
			int yRot = 0;
			Direction dir = state.getValue(BorderStone.FACING);
			if (dir == Direction.DOWN) {
//				model = ringOpen;
//				xRot = 90;
			}
			else if (dir == Direction.UP) {
//				xRot = -90;
			} else {
				yRot = ((int) state.getValue(BorderStone.FACING).toYRot() + 180) % 360;
			}
			return ConfiguredModel.builder()
					.modelFile(model)
					.rotationY(yRot)// (int) facing.getOpposite().toYRot())
					.uvLock(true)
					.build();
		});
	}

	private ResourceLocation key(Block block) {
		return ForgeRegistries.BLOCKS.getKey(block);
	}

	private String name(Block block) {
		return key(block).getPath();
	}
}
