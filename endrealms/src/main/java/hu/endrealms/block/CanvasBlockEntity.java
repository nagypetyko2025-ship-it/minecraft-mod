package hu.endrealms.block;

import hu.endrealms.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The paint on a canvas: one character per pixel, row by row from the bottom, left to right as seen from the front.
 * '0' is bare canvas, '1'..'g' are the sixteen dye colours (dye id + 1). A large canvas keeps both halves here, in its left block.
 */
public class CanvasBlockEntity extends BlockEntity {
	private char[] pixels;

	public CanvasBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CANVAS, pos, state);
		this.pixels = blank(CanvasBlock.widthOf(state));
	}

	private static char[] blank(int width) {
		char[] blank = new char[width * 16];
		java.util.Arrays.fill(blank, '0');
		return blank;
	}

	public int width() {
		return pixels.length / 16;
	}

	/** 0 for bare canvas, otherwise dye id + 1. */
	public int colour(int u, int v) {
		return Character.digit(pixels[v * width() + u], 17);
	}

	public void paint(int u, int v, int colour) {
		char value = Character.forDigit(colour, 17);
		if (pixels[v * width() + u] == value) {
			return;
		}
		pixels[v * width() + u] = value;
		changed();
	}

	public String snapshot() {
		return new String(pixels);
	}

	public boolean isBlank() {
		for (char c : pixels) {
			if (c != '0') {
				return false;
			}
		}
		return true;
	}

	public void restore(String data) {
		if (data != null && data.length() == pixels.length) {
			pixels = data.toCharArray();
			changed();
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("Paint", snapshot());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String data = input.getStringOr("Paint", "");
		if (data.length() == pixels.length) {
			pixels = data.toCharArray();
		}
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
