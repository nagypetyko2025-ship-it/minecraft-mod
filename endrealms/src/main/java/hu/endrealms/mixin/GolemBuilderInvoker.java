package hu.endrealms.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CarvedPumpkinBlock.class)
public interface GolemBuilderInvoker {
	@Invoker("trySpawnGolem")
	void endrealms$trySpawnGolem(Level level, BlockPos pos);
}
