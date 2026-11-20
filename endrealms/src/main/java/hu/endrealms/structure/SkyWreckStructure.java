package hu.endrealms.structure;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Sky Wreck placement: only out in the open void, with nothing at all underneath the ship. */
public class SkyWreckStructure extends Structure {
	public static final MapCodec<SkyWreckStructure> CODEC = simpleCodec(SkyWreckStructure::new);
	private static final double MIN_DIST_FROM_CENTER = 800.0;
	private static final int GROUND_LIMIT = 4;           // open void: nothing at all under the ship
	private static final int REACH = 26;

	public SkyWreckStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		if ((double) x * x + (double) z * z < MIN_DIST_FROM_CENTER * MIN_DIST_FROM_CENTER) {
			return Optional.empty();
		}
		RandomSource random = context.random();
		int y = 70 + random.nextInt(34);
		for (int[] o : new int[][] {{0, 0}, {-REACH, 0}, {REACH, 0}, {0, -REACH}, {0, REACH}}) {
			int ground = context.chunkGenerator().getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (ground > GROUND_LIMIT) {
				return Optional.empty();
			}
		}
		BlockPos base = new BlockPos(x, y, z);
		return Optional.of(new GenerationStub(base, builder -> builder.addPiece(new SkyWreckPiece(random, x, y, z))));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.SKY_WRECK;
	}
}
