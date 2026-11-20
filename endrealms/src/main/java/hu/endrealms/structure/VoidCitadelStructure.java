package hu.endrealms.structure;

import com.mojang.serialization.MapCodec;
import hu.endrealms.registry.ModStructures;
import hu.endrealms.worldgen.BasinSites;
import hu.endrealms.worldgen.GraveyardIslandFeature;
import hu.endrealms.worldgen.ScorchedIslandFeature;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Void Citadel placement: far from the main island and clear of the islands that the Basin and Scorched features build. */
public class VoidCitadelStructure extends Structure {
	public static final MapCodec<VoidCitadelStructure> CODEC = simpleCodec(VoidCitadelStructure::new);
	private static final double MIN_DIST_FROM_CENTER = 1500.0;
	private static final int FLOOR_Y = 70;
	private static final int ISLAND_GRID = 640;
	private static final int CLEARANCE = 30;

	public VoidCitadelStructure(StructureSettings settings) {
		super(settings);
	}

	private static boolean tooClose(int x, int z, int siteX, int siteZ, float radius) {
		double reach = VoidCitadelPiece.REACH + radius + CLEARANCE;
		double dx = x - siteX;
		double dz = z - siteZ;
		return dx * dx + dz * dz < reach * reach;
	}

	private static boolean overlapsFeatureIsland(int x, int z) {
		for (int gx = -1; gx <= 1; gx++) {
			for (int gz = -1; gz <= 1; gz++) {
				int px = x + gx * ISLAND_GRID;
				int pz = z + gz * ISLAND_GRID;
				BasinSites.Site basin = BasinSites.siteFor(px, pz);
				if (basin != null && tooClose(x, z, basin.x(), basin.z(), basin.radius())) {
					return true;
				}
				ScorchedIslandFeature.Sites.Site scorched = ScorchedIslandFeature.Sites.siteFor(px, pz);
				if (scorched != null && tooClose(x, z, scorched.x(), scorched.z(), scorched.radius())) {
					return true;
				}
			}
		}
		for (int gx = -1; gx <= 1; gx++) {
			for (int gz = -1; gz <= 1; gz++) {
				GraveyardIslandFeature.Sites.Site graveyard = GraveyardIslandFeature.Sites.siteFor(x + gx * GraveyardIslandFeature.Sites.CELL, z + gz * GraveyardIslandFeature.Sites.CELL);
				if (graveyard != null && tooClose(x, z, graveyard.x(), graveyard.z(), graveyard.radius())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		if ((double) x * x + (double) z * z < MIN_DIST_FROM_CENTER * MIN_DIST_FROM_CENTER || overlapsFeatureIsland(x, z)) {
			return Optional.empty();
		}
		int seed = context.random().nextInt();
		return Optional.of(new GenerationStub(new BlockPos(x, FLOOR_Y, z), builder -> builder.addPiece(new VoidCitadelPiece(x, FLOOR_Y, z, seed))));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.VOID_CITADEL;
	}
}
