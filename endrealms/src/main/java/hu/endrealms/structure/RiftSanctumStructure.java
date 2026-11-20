package hu.endrealms.structure;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Rift Sanctum placement: needs high ground over the whole arena and a level spot for the shrine next to it. */
public class RiftSanctumStructure extends Structure {
	public static final MapCodec<RiftSanctumStructure> CODEC = simpleCodec(RiftSanctumStructure::new);
	private static final int MIN_GROUND = 54;
	private static final int MAX_SHRINE_SLOPE = 5;
	private static final int ARENA_DEPTH = 24;

	public RiftSanctumStructure(StructureSettings settings) {
		super(settings);
	}

	/** {lowest, highest, centre} ground height of five samples around the point. */
	private static int[] survey(GenerationContext context, int x, int z, int reach) {
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		int center = 0;
		for (int[] o : new int[][] {{0, 0}, {-reach, -reach}, {reach, -reach}, {-reach, reach}, {reach, reach}}) {
			int h = context.chunkGenerator().getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			low = Math.min(low, h);
			high = Math.max(high, h);
			if (o[0] == 0 && o[1] == 0) {
				center = h;
			}
		}
		return new int[] {low, high, center};
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		int[] arena = survey(context, x, z, 14);
		int[] shrine = survey(context, x, z - RiftSanctumPiece.SHAFT_OFFSET, 5);
		if (arena[0] < MIN_GROUND || shrine[0] < MIN_GROUND || shrine[1] - shrine[0] > MAX_SHRINE_SLOPE) {
			return Optional.empty();
		}
		int floorY = Math.min(arena[0], shrine[2]) - ARENA_DEPTH;
		return Optional.of(new GenerationStub(new BlockPos(x, shrine[2], z - RiftSanctumPiece.SHAFT_OFFSET),
				builder -> builder.addPiece(new RiftSanctumPiece(x, shrine[2], floorY, z))));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.RIFT_SANCTUM;
	}
}
