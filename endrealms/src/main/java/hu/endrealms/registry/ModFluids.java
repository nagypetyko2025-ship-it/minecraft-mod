package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import hu.endrealms.fluid.AstralWaterFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;

public final class ModFluids {
	public static final FlowingFluid ASTRAL_WATER = Registry.register(BuiltInRegistries.FLUID,
			EndRealms.id("astral_water"), new AstralWaterFluid.Source());
	public static final FlowingFluid FLOWING_ASTRAL_WATER = Registry.register(BuiltInRegistries.FLUID,
			EndRealms.id("flowing_astral_water"), new AstralWaterFluid.Flowing());

	public static final Block ASTRAL_WATER_BLOCK;
	public static final Item ASTRAL_WATER_BUCKET;

	static {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, EndRealms.id("astral_water"));
		ASTRAL_WATER_BLOCK = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				new LiquidBlock(ASTRAL_WATER, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
						.mapColor(MapColor.COLOR_PURPLE).setId(blockKey)) {
				});

		ResourceKey<Item> bucketKey = ResourceKey.create(Registries.ITEM, EndRealms.id("astral_water_bucket"));
		ASTRAL_WATER_BUCKET = Registry.register(BuiltInRegistries.ITEM, bucketKey, new BucketItem(ASTRAL_WATER,
				new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).setId(bucketKey)));
	}

	private ModFluids() {
	}

	public static void init() {
	}
}
