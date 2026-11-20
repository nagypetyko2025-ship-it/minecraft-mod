package hu.endrealms.structure;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Observatory placement: on high, fairly level ground of the Astral Forest and the Prism Wastes. */
public class ObservatoryStructure extends Structure {
	public static final MapCodec<ObservatoryStructure> CODEC = simpleCodec(ObservatoryStructure::new);
	private static final int MIN_GROUND = 54;
	private static final int MAX_SLOPE = 8;
	private static final int REACH = 11;

	public ObservatoryStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		int center = 0;
		for (int[] o : new int[][] {{0, 0}, {-REACH, -REACH}, {REACH, -REACH}, {-REACH, REACH}, {REACH, REACH}}) {
			int h = context.chunkGenerator().getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			low = Math.min(low, h);
			high = Math.max(high, h);
			if (o[0] == 0 && o[1] == 0) {
				center = h;
			}
		}
		if (low < MIN_GROUND || high - low > MAX_SLOPE) {
			return Optional.empty();
		}
		int ground = center - context.random().nextInt(4);      // sunk a little into the island
		BlockPos base = new BlockPos(x, ground, z);
		return Optional.of(new GenerationStub(base, builder -> builder.addPiece(new ObservatoryPiece(context.random(), x, ground, z))));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.OBSERVATORY;
	}
}
