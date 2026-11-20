package hu.endrealms.structure;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Void Bastion placement: like End Cities it needs a big enough, high enough island - every corner of the
 * footprint must be solid ground at y >= 55, so towers never hang over the void.
 */
public class VoidBastionStructure extends Structure {
	public static final MapCodec<VoidBastionStructure> CODEC = simpleCodec(VoidBastionStructure::new);
	private static final int MIN_GROUND = 55;
	private static final int FOOTPRINT = 8;

	public VoidBastionStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		ChunkGenerator generator = context.chunkGenerator();
		int lowest = Integer.MAX_VALUE;
		int center = 0;
		for (int[] o : new int[][] {{0, 0}, {-FOOTPRINT, -FOOTPRINT}, {FOOTPRINT, -FOOTPRINT}, {-FOOTPRINT, FOOTPRINT}, {FOOTPRINT, FOOTPRINT}}) {
			int h = generator.getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG,
					context.heightAccessor(), context.randomState());
			lowest = Math.min(lowest, h);
			if (o[0] == 0 && o[1] == 0) {
				center = h;
			}
		}
		if (lowest < MIN_GROUND) {
			return Optional.empty();
		}
		BlockPos base = new BlockPos(x, center, z);
		return Optional.of(new GenerationStub(base, builder -> builder.addPiece(new VoidBastionPiece(context.random(), base))));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.VOID_BASTION;
	}
}
