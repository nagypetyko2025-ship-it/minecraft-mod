package hu.endrealms.structure;

import com.mojang.serialization.MapCodec;
import hu.endrealms.entity.EndVillager;
import hu.endrealms.registry.ModStructures;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * End Village: a paved plaza with an astral water fountain, surrounded by one or two rings of houses joined to it
 * by paths. Each house slot is tried separately against the terrain, so villages adapt to the shape of the island;
 * a site that fits fewer than {@link #MIN_HOUSES} houses is rejected.
 */
public class EndVillageStructure extends Structure {
	public static final MapCodec<EndVillageStructure> CODEC = simpleCodec(EndVillageStructure::new);
	private static final int MIN_GROUND = 50;
	private static final int MIN_HOUSES = 4;
	private static final int MAX_SLOPE = 7;
	private static final int PLAZA_REACH = 8;

	public EndVillageStructure(StructureSettings settings) {
		super(settings);
	}

	private record Spot(int x, int y, int z) {
	}

	/** Ground height at the centre, or null if any sample of the footprint is void or too far from level. */
	private static Spot survey(GenerationContext context, int x, int z, int reach) {
		ChunkGenerator generator = context.chunkGenerator();
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		int center = 0;
		for (int[] o : new int[][] {{0, 0}, {-reach, -reach}, {reach, -reach}, {-reach, reach}, {reach, reach}}) {
			int h = generator.getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			low = Math.min(low, h);
			high = Math.max(high, h);
			if (o[0] == 0 && o[1] == 0) {
				center = h;
			}
		}
		return low < MIN_GROUND || high - low > MAX_SLOPE ? null : new Spot(x, center, z);
	}

	/** The side of a house at this offset from the plaza that faces back towards it. */
	private static Direction facingPlaza(int dx, int dz) {
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? Direction.WEST : Direction.EAST;
		}
		return dz > 0 ? Direction.NORTH : Direction.SOUTH;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		Spot plaza = survey(context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), PLAZA_REACH);
		if (plaza == null) {
			return Optional.empty();
		}
		RandomSource random = context.random();
		List<EndVillagePiece> paths = new ArrayList<>();
		List<EndVillagePiece> houses = new ArrayList<>();
		List<EndVillager.Profession> professions = new ArrayList<>(List.of(EndVillager.Profession.values()));
		professions.remove(EndVillager.Profession.CASTAWAY);
		for (int i = professions.size() - 1; i > 0; i--) {
			professions.set(i, professions.set(random.nextInt(i + 1), professions.get(i)));
		}

		float start = random.nextFloat() * Mth.TWO_PI;
		int[][] rings = {{8, 19}, {10, 35}};    // {slots, distance}: spaced so that neighbouring houses cannot touch
		for (int[] ring : rings) {
			for (int slot = 0; slot < ring[0]; slot++) {
				float angle = start + slot * Mth.TWO_PI / ring[0];
				int distance = ring[1] + random.nextInt(3);
				int dx = Math.round(Mth.cos(angle) * distance);
				int dz = Math.round(Mth.sin(angle) * distance);
				Spot spot = survey(context, plaza.x() + dx, plaza.z() + dz, 5);
				if (spot == null || Math.abs(spot.y() - plaza.y()) > 16) {
					continue;
				}
				Direction facing = facingPlaza(dx, dz);
				EndVillager.Profession profession = professions.get(houses.size() % professions.size());
				EndVillagePiece house = EndVillagePiece.house(random, spot.x(), spot.y(), spot.z(), facing, profession);
				houses.add(house);
				BlockPos door = house.frontOfDoor();
				paths.add(EndVillagePiece.path(door.getX(), door.getY(), door.getZ(), plaza.x(), plaza.y(), plaza.z()));
			}
		}
		if (houses.size() < MIN_HOUSES) {
			return Optional.empty();
		}
		BlockPos base = new BlockPos(plaza.x(), plaza.y(), plaza.z());
		return Optional.of(new GenerationStub(base, builder -> {
			paths.forEach(builder::addPiece);       // paths first: plaza and houses are built over their ends
			builder.addPiece(EndVillagePiece.plaza(random, plaza.x(), plaza.y(), plaza.z()));
			houses.forEach(builder::addPiece);
		}));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.END_VILLAGE;
	}
}
