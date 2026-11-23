package hu.endrealms.sculk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import hu.endrealms.EndRealms;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Which Overworld portal leads to which Sculk portal, so that going back takes you to where you came from - say the
 * Ancient City's frame - and not to wherever the Silent Barrens put you. A portal is found by any of its blocks: a link
 * counts for every portal block within {@link #REACH} of the one it was made from.
 */
public final class SculkLinks extends SavedData {
	public record Link(long overworld, long sculk) {
		static final Codec<Link> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.LONG.fieldOf("overworld").forGetter(Link::overworld),
				Codec.LONG.fieldOf("sculk").forGetter(Link::sculk)).apply(i, Link::new));
	}

	private static final int REACH = 16;
	private static final Codec<SculkLinks> CODEC = Link.CODEC.listOf().fieldOf("links").codec().xmap(SculkLinks::new, links -> links.links);
	private static final SavedDataType<SculkLinks> TYPE = new SavedDataType<>(EndRealms.id("sculk_links"), SculkLinks::new, CODEC, null);

	private final List<Link> links;

	private SculkLinks() {
		this.links = new ArrayList<>();
	}

	private SculkLinks(List<Link> links) {
		this.links = new ArrayList<>(links);
	}

	public static SculkLinks get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private static Link nearest(List<Link> links, BlockPos pos, boolean fromOverworld) {
		Link best = null;
		double bestDistance = REACH * REACH;
		for (Link link : links) {
			BlockPos end = BlockPos.of(fromOverworld ? link.overworld() : link.sculk());
			double d = end.distSqr(pos);
			if (d <= bestDistance) {
				best = link;
				bestDistance = d;
			}
		}
		return best;
	}

	/** Where the Overworld portal at {@code pos} leads in the Sculk dimension, or null if it was never used. */
	public BlockPos sculkFor(BlockPos pos) {
		Link link = nearest(links, pos, true);
		return link == null ? null : BlockPos.of(link.sculk());
	}

	/** Where the Sculk portal at {@code pos} leads back to in the Overworld, or null if it was not made from there. */
	public BlockPos overworldFor(BlockPos pos) {
		Link link = nearest(links, pos, false);
		return link == null ? null : BlockPos.of(link.overworld());
	}

	public void link(BlockPos overworld, BlockPos sculk) {
		links.removeIf(l -> BlockPos.of(l.overworld()).distSqr(overworld) <= REACH * REACH);
		links.add(new Link(overworld.asLong(), sculk.asLong()));
		setDirty();
	}
}
