package hu.endrealms.worldgen;

import net.minecraft.util.Mth;

/**
 * Where Astral Basins are. The outer End is divided into a grid of cells; some cells hold one basin island at a
 * pseudo-random spot. Both the biome source (via mixin) and {@link AstralBasinFeature} use this, so the biome
 * covers exactly the generated island and {@code /locate biome endrealms:astral_basin} finds it.
 */
public final class BasinSites {
	private static final int CELL = 640;
	private static final int CELL_MARGIN = 128;     // keeps every island fully inside its own cell
	private static final float CHANCE = 0.45F;
	private static final double MIN_DIST_FROM_CENTER = 1200.0;  // never near the dragon island

	public record Site(int x, int z, float radius) {
		/** Island radius (mountain ring outer edge) at a column, with a wobbly outline. */
		public float ringRadius(float noise) {
			return radius + 6.0F * noise;
		}

		public float lakeRadius(float noise) {
			return radius * 0.6F + 4.0F * noise;
		}
	}

	private BasinSites() {
	}

	/** The basin whose cell contains (x, z), or null. */
	public static Site siteFor(int x, int z) {
		int cx = Math.floorDiv(x, CELL);
		int cz = Math.floorDiv(z, CELL);
		if (hash(cx, cz, 1) >= CHANCE) {
			return null;
		}
		int span = CELL - 2 * CELL_MARGIN;
		int sx = cx * CELL + CELL_MARGIN + (int) (hash(cx, cz, 2) * span);
		int sz = cz * CELL + CELL_MARGIN + (int) (hash(cx, cz, 3) * span);
		if ((double) sx * sx + (double) sz * sz < MIN_DIST_FROM_CENTER * MIN_DIST_FROM_CENTER) {
			return null;
		}
		return new Site(sx, sz, 64.0F + 12.0F * hash(cx, cz, 4));
	}

	/** True if the biome at this block column should be Astral Basin (the island plus a small margin). */
	public static boolean isBasinBiome(int x, int z) {
		Site site = siteFor(x, z);
		if (site == null) {
			return false;
		}
		float dx = x - site.x();
		float dz = z - site.z();
		return dx * dx + dz * dz <= Mth.square(site.radius() + 10.0F);
	}

	/** Smooth deterministic value noise in [-1, 1] with the given cell size; salt picks an independent layer. */
	public static float noise(int x, int z, int cell, int salt) {
		float fx = x / (float) cell;
		float fz = z / (float) cell;
		int ix = Mth.floor(fx);
		int iz = Mth.floor(fz);
		float tx = fx - ix;
		float tz = fz - iz;
		tx = tx * tx * (3 - 2 * tx);
		tz = tz * tz * (3 - 2 * tz);
		float a = hash(ix, iz, salt);
		float b = hash(ix + 1, iz, salt);
		float c = hash(ix, iz + 1, salt);
		float d = hash(ix + 1, iz + 1, salt);
		return Mth.lerp(tz, Mth.lerp(tx, a, b), Mth.lerp(tx, c, d)) * 2.0F - 1.0F;
	}

	/** Deterministic hash of (x, z, salt) in [0, 1). */
	public static float hash(int x, int z, int salt) {
		long h = x * 341873128712L + z * 132897987541L + salt * 1640531527L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / (float) 0x1000000;
	}
}
