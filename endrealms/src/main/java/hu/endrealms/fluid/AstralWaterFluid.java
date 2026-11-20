package hu.endrealms.fluid;

import hu.endrealms.registry.ModFluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;

/** Purple water: inherits every vanilla water behaviour (flow, swimming, infinite sources, lava mixing). */
public abstract class AstralWaterFluid extends WaterFluid {
	@Override
	public Fluid getFlowing() {
		return ModFluids.FLOWING_ASTRAL_WATER;
	}

	@Override
	public Fluid getSource() {
		return ModFluids.ASTRAL_WATER;
	}

	@Override
	public Item getBucket() {
		return ModFluids.ASTRAL_WATER_BUCKET;
	}

	@Override
	public BlockState createLegacyBlock(FluidState state) {
		return ModFluids.ASTRAL_WATER_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
	}

	@Override
	public boolean isSame(Fluid fluid) {
		return fluid == ModFluids.ASTRAL_WATER || fluid == ModFluids.FLOWING_ASTRAL_WATER;
	}

	public static class Source extends AstralWaterFluid {
		@Override
		public int getAmount(FluidState state) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends AstralWaterFluid {
		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState state) {
			return false;
		}
	}
}
