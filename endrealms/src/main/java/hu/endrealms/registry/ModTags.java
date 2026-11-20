package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
	public static final TagKey<Block> INCORRECT_FOR_CHORUSITE_TOOL =
			TagKey.create(Registries.BLOCK, EndRealms.id("incorrect_for_chorusite_tool"));
	public static final TagKey<Item> CHORUSITE_TOOL_MATERIALS =
			TagKey.create(Registries.ITEM, EndRealms.id("chorusite_tool_materials"));

	private ModTags() {
	}
}
