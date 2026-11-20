package hu.endrealms.structure;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModStructures;
import java.util.Optional;
import hu.endrealms.worldgen.ScorchedIslandFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Crypt placement: a level spot for the surface building on a Scorched Barrens island, with solid ground over the vaults. */
public class CryptStructure extends Structure {
	public static final MapCodec<CryptStructure> CODEC = simpleCodec(CryptStructure::new);
	private static final int EDGE_MARGIN = 34;
	private static final int MAX_SLOPE = 12;
	private static final int VAULT_DEPTH = 16;

	public CryptStructure(StructureSettings settings) {
		super(settings);
	}

	private static int[] survey(ScorchedIslandFeature.Sites.Site site, int x, int z, int reach) {
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		for (int[] o : new int[][] {{0, 0}, {-reach, -reach}, {reach, -reach}, {-reach, reach}, {reach, reach}}) {
			int h = ScorchedIslandFeature.surfaceAt(site, x + o[0], z + o[1]);
			low = Math.min(low, h);
			high = Math.max(high, h);
		}
		return new int[] {low, high};
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		ScorchedIslandFeature.Sites.Site site = ScorchedIslandFeature.Sites.siteFor(x, z);
		if (site == null || Mth.sqrt(Mth.square(x - site.x()) + Mth.square(z - site.z())) > site.radius() - EDGE_MARGIN) {
			return Optional.empty();
		}
		int[] building = survey(site, x, z, 7);
		if (building[1] - building[0] > MAX_SLOPE) {
			return Optional.empty();
		}
		int ground = ScorchedIslandFeature.surfaceAt(site, x, z);
		int floor = Math.min(survey(site, x, z, 26)[0], ground) - VAULT_DEPTH;
		return Optional.of(new GenerationStub(new BlockPos(x, ground, z), builder -> builder.addPiece(new CryptPiece(context.random(), x, ground, floor, z))));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.CRYPT;
	}
}
