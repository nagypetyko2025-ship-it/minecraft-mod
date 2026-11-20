package hu.endrealms.mixin;

import java.util.function.Predicate;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Golems accept an End Realms carved pumpkin as their head, like a vanilla one. */
@Mixin(CarvedPumpkinBlock.class)
public abstract class GolemPumpkinMixin {
	@Shadow
	@Final
	@Mutable
	private static Predicate<BlockState> PUMPKINS_PREDICATE;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void endrealms$acceptCarvedPumpkins(CallbackInfo ci) {
		// checks the class, not ModBlocks: this runs while vanilla blocks are being created, before the mod registers its own
		PUMPKINS_PREDICATE = PUMPKINS_PREDICATE.or(state -> state.getBlock() instanceof hu.endrealms.block.CarvedPumpkinBlock);
	}
}
