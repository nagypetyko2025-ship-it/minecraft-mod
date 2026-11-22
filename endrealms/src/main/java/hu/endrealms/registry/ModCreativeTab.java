package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** One creative tab per kind of content. Lists are suppliers because blocks and items must be registered first. */
public final class ModCreativeTab {
	private ModCreativeTab() {
	}

	private static void tab(String name, Supplier<ItemLike> icon, Supplier<ItemLike[]> items) {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceKey.create(Registries.CREATIVE_MODE_TAB, EndRealms.id(name)),
				FabricCreativeModeTab.builder()
						.title(Component.translatable("itemGroup.endrealms." + name))
						.icon(() -> new ItemStack(icon.get()))
						.displayItems((params, output) -> {
							for (ItemLike item : items.get()) {
								output.accept(item);
							}
						})
						.build());
	}

	public static void init() {
		tab("building", () -> ModBlocks.VOIDSTONE_BRICKS, () -> new ItemLike[] {
				ModBlocks.VOIDSTONE, ModBlocks.VOIDSTONE_SLAB, ModBlocks.VOIDSTONE_STAIRS, ModBlocks.VOIDSTONE_WALL,
				ModBlocks.POLISHED_VOIDSTONE, ModBlocks.POLISHED_VOIDSTONE_SLAB, ModBlocks.POLISHED_VOIDSTONE_STAIRS, ModBlocks.POLISHED_VOIDSTONE_WALL,
				ModBlocks.VOIDSTONE_BRICKS, ModBlocks.VOIDSTONE_BRICK_SLAB, ModBlocks.VOIDSTONE_BRICK_STAIRS, ModBlocks.VOIDSTONE_BRICK_WALL,
				ModBlocks.CRYSTALLINE_VOIDSTONE,
				ModBlocks.VOID_SAND,
				ModBlocks.VOID_SANDSTONE, ModBlocks.VOID_SANDSTONE_SLAB, ModBlocks.VOID_SANDSTONE_STAIRS, ModBlocks.VOID_SANDSTONE_WALL,
				ModBlocks.SMOOTH_VOID_SANDSTONE, ModBlocks.SMOOTH_VOID_SANDSTONE_SLAB, ModBlocks.SMOOTH_VOID_SANDSTONE_STAIRS,
				ModBlocks.CUT_VOID_SANDSTONE, ModBlocks.CUT_VOID_SANDSTONE_SLAB, ModBlocks.CHISELED_VOID_SANDSTONE,
				ModBlocks.PRISM_CRYSTAL_BLOCK, ModBlocks.BUDDING_PRISM_CRYSTAL, ModBlocks.SMALL_PRISM_BUD, ModBlocks.MEDIUM_PRISM_BUD,
				ModBlocks.LARGE_PRISM_BUD, ModBlocks.PRISM_CLUSTER, ModBlocks.PRISM_LAMP,
				ModBlocks.CRUMBLING_PLANKS, ModBlocks.SCORCHED_END_STONE, ModBlocks.EMBER_ROCK, ModBlocks.VOLCANIC_VENT, ModBlocks.ASH_BLOCK, ModBlocks.ASH_LAYER,
		});
		tab("nature", () -> ModBlocks.ASTRAL_SAPLING, () -> new ItemLike[] {
				ModBlocks.ASTRAL_LOG, ModBlocks.ASTRAL_WOOD, ModBlocks.STRIPPED_ASTRAL_LOG, ModBlocks.STRIPPED_ASTRAL_WOOD,
				ModBlocks.ASTRAL_PLANKS, ModBlocks.ASTRAL_SLAB, ModBlocks.ASTRAL_STAIRS, ModBlocks.ASTRAL_FENCE, ModBlocks.ASTRAL_FENCE_GATE,
				ModBlocks.ASTRAL_DOOR, ModBlocks.ASTRAL_TRAPDOOR, ModBlocks.ASTRAL_LEAVES, ModBlocks.ASTRAL_SAPLING,
				ModBlocks.END_SOIL, ModBlocks.VOIDBLOOM, ModBlocks.STARREED, ModBlocks.MOONBELL, ModBlocks.STARWEED, ModBlocks.VOID_CORAL, ModFluids.ASTRAL_WATER_BUCKET,
		});
		tab("materials", () -> ModItems.CHORUSITE_INGOT, () -> new ItemLike[] {
				ModBlocks.CHORUSITE_ORE, ModBlocks.VOIDSTONE_CHORUSITE_ORE, ModBlocks.PHASITE_ORE, ModBlocks.DEEPGLOW_ORE, ModItems.DEEPGLOW_SHARD,
				ModItems.CHORUSITE_SHARD, ModItems.PURE_CHORUSITE_SHARD, ModItems.CHORUSITE_INGOT, ModBlocks.CHORUSITE_BLOCK, ModItems.CHORUSITE_UPGRADE_SMITHING_TEMPLATE,
				ModItems.PHASITE_SHARD, ModItems.PHASITE_GEM, ModItems.PRISM_SHARD, ModItems.RIFT_HEART, ModItems.ASH_DUST, ModItems.ASH_MEAL, ModItems.EMBER_GARNET, ModItems.STAR_SAPPHIRE, ModItems.VOID_ONYX, ModItems.MOON_OPAL, ModItems.ASH_QUARTZ, ModItems.BLOODSTONE, ModItems.CROWNSTONE, ModItems.DEEP_AQUAMARINE,
		});
		tab("equipment", () -> ModItems.CHORUSITE_CHESTPLATE, () -> new ItemLike[] {
				ModItems.CHORUSITE_SWORD, ModItems.CHORUSITE_SHOVEL, ModItems.CHORUSITE_PICKAXE, ModItems.CHORUSITE_AXE, ModItems.CHORUSITE_HOE,
				ModItems.CHORUSITE_HELMET, ModItems.CHORUSITE_CHESTPLATE, ModItems.CHORUSITE_LEGGINGS, ModItems.CHORUSITE_BOOTS,
				ModItems.RIFTBLADE, ModItems.PHASE_PEARL, ModBlocks.PHASE_ANCHOR, ModBlocks.PHASE_MAILBOX, ModItems.END_GUIDE, ModBlocks.END_CRAFTING_TABLE, ModBlocks.GEM_TABLE, ModBlocks.GRAVITY_STONE, ModBlocks.CANVAS, ModBlocks.LARGE_CANVAS, ModBlocks.FERMENTATION_STATION, ModBlocks.AGING_BARREL, ModItems.APPLE_BRANDY, ModItems.GLOW_BERRY_BRANDY, ModItems.SWEET_BERRY_BRANDY, ModItems.NECTAR_BRANDY, ModItems.STARBERRY_BRANDY, ModItems.FRUIT_MEDLEY_BRANDY, ModItems.HONEYED_APPLE_BRANDY, ModItems.GLOWING_NECTAR_BRANDY, ModItems.CHORUS_BERRY_BRANDY, ModItems.MELON_REFRESHER, ModItems.ENDLESS_BRANDY, ModItems.MINERS_BRANDY, ModItems.DRAGON_BLOOD_BRANDY, ModItems.FOREST_ELIXIR, ModItems.VOID_ESSENCE, ModBlocks.BONE_SOIL, ModBlocks.DRAGON_BONE_BLOCK, ModItems.DRAGON_BONE, ModItems.BACKPACK,
		});
		tab("food", () -> ModItems.COOKED_GLIMMERHOOF, () -> new ItemLike[] {
				ModItems.RAW_GLIMMERHOOF, ModItems.COOKED_GLIMMERHOOF,
				ModItems.RAW_STARFIN, ModItems.COOKED_STARFIN, ModItems.RAW_VOID_EEL, ModItems.COOKED_VOID_EEL, ModItems.RAW_GLOWGILL, ModItems.COOKED_GLOWGILL,
				ModItems.STARBERRY, ModItems.MOONBELL_NECTAR,
		});
		// everything from the Sculk dimension on a page of its own
		tab("sculk", () -> ModBlocks.SCULK_PORTAL, () -> new ItemLike[] {
				ModItems.ECHO_KEY, ModBlocks.SCULK_PORTAL, ModBlocks.SCULK_SOIL, ModBlocks.SCULK_STONE,
				ModBlocks.WATCHER_GRASS, ModBlocks.ECHO_FLOWER, ModBlocks.SOUL_MUSHROOM,
				ModItems.ECHO_DUST, ModItems.SCULK_ANTLER, ModItems.BONE_STAG_SPAWN_EGG,
		});
		tab("creatures", () -> ModItems.GLIMMERHOOF_SPAWN_EGG, () -> new ItemLike[] {
				ModItems.GLIMMERHOOF_SPAWN_EGG, ModItems.STARFIN_SPAWN_EGG, ModItems.VOID_EEL_SPAWN_EGG, ModItems.GLOWGILL_SPAWN_EGG,
				ModItems.VOID_SENTINEL_SPAWN_EGG, ModItems.RIFT_LORD_SPAWN_EGG, ModItems.VOID_KING_SPAWN_EGG, ModItems.SPECTRAL_WHELP_SPAWN_EGG, ModItems.BONE_DRAKE_SPAWN_EGG, ModItems.SHARDCRAB_SPAWN_EGG, ModItems.ASH_WRAITH_SPAWN_EGG, ModItems.DEEP_LURKER_SPAWN_EGG, ModItems.END_VILLAGER_SPAWN_EGG,
		});
	}
}
