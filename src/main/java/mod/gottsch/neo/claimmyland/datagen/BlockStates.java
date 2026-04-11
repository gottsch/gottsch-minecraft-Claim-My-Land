
package mod.gottsch.neo.claimmyland.datagen;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.block.BorderStone;
import mod.gottsch.neo.claimmyland.core.block.FoundationStone;
import mod.gottsch.neo.claimmyland.core.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

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

	public void foundationStone(DeferredHolder<Block, ? extends FoundationStone> block, ResourceLocation polished, ResourceLocation stone) {
		String name = block.getId().getPath();
		myFoundationStone(name, (FoundationStone)block.get(), polished, stone, "minecraft:cutout");
	}

	private void myFoundationStone(String name, FoundationStone block, ResourceLocation polished, ResourceLocation stone, String renderType) {
		ModelFile model = models().withExistingParent(name, modLoc(ModelProvider.BLOCK_FOLDER + "/foundation_stone"))
				.texture("2", polished)
				.texture("9", stone)
				.renderType(renderType);

		getVariantBuilder(block).forAllStates(state -> {
			Direction dir = state.getValue(FoundationStone.FACING);
			int yRot = 0;
			if (dir == Direction.DOWN) {
				// xRot = 90;
			} else if (dir == Direction.UP) {
				// xRot = -90;
			} else {
				yRot = ((int) dir.toYRot() + 180) % 360;
			}
			return ConfiguredModel.builder()
					.modelFile(model)
					.rotationY(yRot)
					.uvLock(true)
					.build();
		});
	}

	private ResourceLocation key(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block);
	}

	private String name(Block block) {
		return key(block).getPath();
	}
}
