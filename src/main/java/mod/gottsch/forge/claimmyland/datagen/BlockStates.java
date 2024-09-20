
package mod.gottsch.forge.claimmyland.datagen;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.*;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.*;
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

//		simpleBlock(ModBlocks.FOUNDATION_STONE.get());
		simpleBlock(ModBlocks.BORDER_STONE.get(), models().cubeAll(name(ModBlocks.BORDER_STONE.get()), blockTexture(ModBlocks.BORDER_STONE.get())).renderType("cutout"));
		simpleBlock(ModBlocks.PLAYER_FOUNDATION_STONE.get());
		simpleBlock(ModBlocks.NATION_FOUNDATION_STONE.get());

		borderBlock(ModBlocks.PLAYER_BORDER, modLoc("block/green"), modLoc("block/red"));
		borderBlock(ModBlocks.NATION_BORDER, modLoc("block/blue"), modLoc("block/red"));

		bufferBlock(ModBlocks.BUFFER, modLoc("block/buffer_block"), modLoc("block/bad_buffer_block"));
//		sewerBlock(ModBlocks.WEATHERED_COPPER_SEWER, modLoc("block/weathered_copper_pipe"), mcLoc("block/weathered_copper"));
//		sewerBlock(ModBlocks.TERRACOTTA_SEWER, mcLoc("block/terracotta"), mcLoc("block/terracotta"));

	}

	public void borderBlock(RegistryObject<Block> block, ResourceLocation goodTexture, ResourceLocation badTexture) {
		String name = block.getId().getPath();
//		ModelFile goodTop = models().withExistingParent("good_top_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/top_border_block"))
//				.texture("0", goodTexture);
//
//		ModelFile bottom = models().withExistingParent("good_bottom_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/bottom_border_block"))
//				.texture("0", goodTexture);

		myBorderBlock(name, (BorderBlock)block.get(), "border", goodTexture, badTexture, "minecraft:cutout");
	}

	public void bufferBlock(RegistryObject<Block> block, ResourceLocation goodTexture, ResourceLocation badTexture) {
		String name = block.getId().getPath();

		myBorderBlock(name, (BufferBlock)block.get(), "buffer", goodTexture, badTexture, "minecraft:translucent");
	}

	private void myBorderBlock(String name, BorderBlock block, String blockKey, ResourceLocation goodTexture, ResourceLocation badTexture, String renderType) {
		ModelFile goodTop = models().withExistingParent("good_top_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/top_"+ blockKey +"_block")).texture("0", goodTexture).renderType(renderType);
		ModelFile goodBottom = models().withExistingParent("good_bottom_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/bottom_"+ blockKey +"_block")).texture("0", goodTexture).renderType(renderType);
		ModelFile goodLeft = models().withExistingParent("good_left_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/left_" + blockKey + "_block")).texture("0", goodTexture).renderType(renderType);
		ModelFile goodRight = models().withExistingParent("good_right_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/right_" + blockKey + "_block")).texture("0", goodTexture).renderType(renderType);
		ModelFile goodTopLeft = models().withExistingParent("good_top_left_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/top_left_" + blockKey + "_block")).texture("0", goodTexture).renderType(renderType);
		ModelFile goodTopRight = models().withExistingParent("good_top_right_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/top_right_" + blockKey + "_block")).texture("0", goodTexture).renderType(renderType);
		ModelFile goodBottomLeft = models().withExistingParent("good_bottom_left_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/bottom_left_" + blockKey + "_block")).texture("0", goodTexture).renderType(renderType);
		ModelFile goodBottomRight = models().withExistingParent("good_bottom_right_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/bottom_right_" + blockKey + "_block")).texture("0", goodTexture).renderType(renderType);

		ModelFile badTop = models().withExistingParent("bad_top_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/top_" + blockKey + "_block")).texture("0", badTexture).renderType(renderType);
		ModelFile badBottom = models().withExistingParent("bad_bottom_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/bottom_" + blockKey + "_block")).texture("0", badTexture).renderType(renderType);
		ModelFile badLeft = models().withExistingParent("bad_left_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/left_" + blockKey + "_block")).texture("0", badTexture).renderType(renderType);
		ModelFile badRight = models().withExistingParent("bad_right_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/right_" + blockKey + "_block")).texture("0", badTexture).renderType(renderType);
		ModelFile badTopLeft = models().withExistingParent("bad_top_left_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/top_left_" + blockKey + "_block")).texture("0", badTexture).renderType(renderType);
		ModelFile badTopRight = models().withExistingParent("bad_top_right_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/top_right_" + blockKey + "_block")).texture("0", badTexture).renderType(renderType);
		ModelFile badBottomLeft = models().withExistingParent("bad_bottom_left_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/bottom_left_" + blockKey + "_block")).texture("0", badTexture).renderType(renderType);
		ModelFile badBottomRight = models().withExistingParent("bad_bottom_right_" + name, modLoc(ModelProvider.BLOCK_FOLDER + "/bottom_right_" + blockKey + "_block")).texture("0", badTexture).renderType(renderType);

		getVariantBuilder(block).forAllStatesExcept(state -> {
			ModelFile model = goodTop;
			BorderStatus intersects = state.getValue(BorderBlock.INTERSECTS);
			BorderPosition position = state.getValue(BorderBlock.POSITION);
			Direction facing = state.getValue(BorderBlock.FACING);

			if (intersects == BorderStatus.GOOD) {
				if (position == BorderPosition.BOTTOM) {
					model = goodBottom;
				} else if (position == BorderPosition.LEFT) {
					model = goodLeft;
				} else if (position == BorderPosition.RIGHT) {
					model = goodRight;
				} else if (position == BorderPosition.TOP_LEFT) {
					model = goodTopLeft;
				} else if (position == BorderPosition.TOP_RIGHT) {
					model = goodTopRight;
				} else if (position == BorderPosition.BOTTOM_LEFT) {
					model = goodBottomLeft;
				} else if (position == BorderPosition.BOTTOM_RIGHT) {
					model = goodBottomRight;
				}
			} else {
				// bad models
				if (position == BorderPosition.TOP) {
					model = badTop;
				} else if (position == BorderPosition.BOTTOM) {
					model = badBottom;
				} else if (position == BorderPosition.LEFT) {
					model = badLeft;
				} else if (position == BorderPosition.RIGHT) {
					model = badRight;
				} else if (position == BorderPosition.TOP_LEFT) {
					model = badTopLeft;
				} else if (position == BorderPosition.TOP_RIGHT) {
					model = badTopRight;
				} else if (position == BorderPosition.BOTTOM_LEFT) {
					model = badBottomLeft;
				} else if (position == BorderPosition.BOTTOM_RIGHT) {
					model = badBottomRight;
				}
			}

			int yRot = 0;
			Direction dir = state.getValue(BorderBlock.FACING);
			if (dir == Direction.DOWN) {
//				model = ringOpen;
//				xRot = 90;
			}
			else if (dir == Direction.UP) {
//				xRot = -90;
			} else {
				yRot = ((int) state.getValue(BorderBlock.FACING).toYRot() + 180) % 360;
			}
			return ConfiguredModel.builder()
					.modelFile(model)
					.rotationY(yRot)// (int) facing.getOpposite().toYRot())
					.uvLock(true)
					.build();
		}, BorderBlock.WATERLOGGED);
	}

//	public BlockModelBuilder twoTextures(String name, ResourceLocation parent,
//										 String textureKey1, ResourceLocation texture1,
//										 String textureKey2, ResourceLocation texture2) {
//		return models().withExistingParent(name, parent)
//				.texture(textureKey1, texture1)
//				.texture(textureKey2, texture2);
//	}

//	public void sewerBlock(RegistryObject<Block> block, ResourceLocation texture, ResourceLocation texture1) {
//		String name = block.getId().getPath();
//		ModelFile model = twoTextures(name, modLoc(ModelProvider.BLOCK_FOLDER + "/template_sewer_block"), "0", texture, "1", texture1);
//		ModelFile corner = twoTextures(name + "_corner", modLoc(ModelProvider.BLOCK_FOLDER + "/template_sewer_block_corner"), "0", texture, "1", texture1);
//
//		sewerBlock(block.get(), model, corner);
//	}
//
//	public void sewerBlock(Block block, ModelFile sewer, ModelFile corner) {
//		getVariantBuilder(block)
//				.forAllStates(state -> {
//					Direction facing = state.getValue(LedgeBlock.FACING);
//					SewerBlock.SewerShape shape = state.getValue(SewerBlock.SHAPE);
//					int yRot = ((int) state.getValue(FACING).toYRot() + DEFAULT_ANGLE_OFFSET) % 360;
//					yRot = switch(shape) {
//						case STRAIGHT -> yRot;
//						case TOP_LEFT -> 180;
//						case BOTTOM_LEFT -> 90;
//						case TOP_RIGHT -> 270;
//						case BOTTOM_RIGHT -> 0;
//					};
//
//					return ConfiguredModel.builder()
//							.modelFile(shape == SewerBlock.SewerShape.STRAIGHT ? sewer : corner)
//							.rotationY(yRot)
//							.uvLock(true)
//							.build();
//				});
//	}

	private ResourceLocation key(Block block) {
		return ForgeRegistries.BLOCKS.getKey(block);
	}

	private String name(Block block) {
		return key(block).getPath();
	}
}
