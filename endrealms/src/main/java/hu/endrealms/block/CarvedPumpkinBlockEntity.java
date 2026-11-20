package hu.endrealms.block;

import com.mojang.serialization.Codec;
import hu.endrealms.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Which of the 16x16 pixels on each of the six faces has been cut away. */
public class CarvedPumpkinBlockEntity extends BlockEntity {
	public static final int LONGS = 4 * 6;
	private final long[] pixels = new long[LONGS];

	public CarvedPumpkinBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CARVED_PUMPKIN, pos, state);
	}

	private static int bit(int a, int b) {
		return b * 16 + a;
	}

	public void setCarved(Direction face, int a, int b, boolean carved) {
		int bit = bit(a, b);
		int word = face.ordinal() * 4 + (bit >> 6);
		long mask = 1L << (bit & 63);
		pixels[word] = carved ? pixels[word] | mask : pixels[word] & ~mask;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public boolean isCarved(Direction face, int a, int b) {
		return isCarved(pixels, face, a, b);
	}

	public static boolean isCarved(long[] pixels, Direction face, int a, int b) {
		int bit = bit(a, b);
		return (pixels[face.ordinal() * 4 + (bit >> 6)] >>> (bit & 63) & 1L) != 0;
	}

	/** For the item this pumpkin drops: the pixels and, as the last word, whether it is lit. */
	public List<Long> snapshot(boolean lit) {
		List<Long> list = new ArrayList<>(LONGS + 1);
		for (long word : pixels) {
			list.add(word);
		}
		list.add(lit ? 1L : 0L);
		return list;
	}

	public void restore(List<Long> list) {
		for (int i = 0; i < Math.min(LONGS, list.size()); i++) {
			pixels[i] = list.get(i);
		}
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public long[] pixels() {
		return pixels;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		List<Long> list = new ArrayList<>(LONGS);
		for (long word : pixels) {
			list.add(word);
		}
		output.store("Pixels", Codec.LONG.listOf(), list);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		input.read("Pixels", Codec.LONG.listOf()).ifPresent(list -> {
			for (int i = 0; i < Math.min(LONGS, list.size()); i++) {
				pixels[i] = list.get(i);
			}
		});
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
