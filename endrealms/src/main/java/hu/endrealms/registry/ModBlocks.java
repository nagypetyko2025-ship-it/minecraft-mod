package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import hu.endrealms.block.AstralWaterPlantBlock;
import hu.endrealms.block.CarvedPumpkinBlock;
import hu.endrealms.block.CrumblingPlanksBlock;
import hu.endrealms.block.EndSaplingBlock;
import hu.endrealms.block.GemTableBlock;
import hu.endrealms.block.MoonbellBlock;
import hu.endrealms.block.StarreedBlock;
import hu.endrealms.block.VolcanicVentBlock;
import hu.endrealms.block.PrismBlocks;
import hu.endrealms.block.ScorchedBlocks;
import hu.endrealms.block.StarweedBlock;
import hu.endrealms.block.VoidbloomBlock;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import java.util.function.BiFunction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	/** Chorusite ore: found in end stone, drops chorusite shards (3-7 XP like diamond ore). */
	public static final Block CHORUSITE_ORE = register("chorusite_ore",
			props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.SAND)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(4.0F, 9.0F)
					.sound(SoundType.STONE));

	/** Phasite ore: on the underside of End islands; its veins flicker and glow (see the block model). No uses yet. */
	public static final Block PHASITE_ORE = register("phasite_ore",
			props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_LIGHT_BLUE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops()
					.strength(4.0F, 9.0F)
					.lightLevel(state -> 3)
					.sound(SoundType.STONE));

	/** End soil: the ground of the Astral Forest, also found in small patches all over the End. */
	public static final Block END_SOIL = register("end_soil", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.COLOR_PURPLE).sound(SoundType.ROOTED_DIRT));

	/** Alien plant of the Astral Forest and end-soil patches; drops itself only when sheared, glows faintly. */
	public static final Block VOIDBLOOM = register("voidbloom", VoidbloomBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.FERN).mapColor(MapColor.COLOR_PINK).lightLevel(state -> 6));

	// --- Prism Wastes: turquoise crystals; the biome's light comes from the blocks themselves ---
	public static final Block CRYSTALLINE_VOIDSTONE = register("crystalline_voidstone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).mapColor(MapColor.COLOR_LIGHT_GRAY));
	public static final Block PRISM_CRYSTAL_BLOCK = register("prism_crystal_block", PrismBlocks.Crystal::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(state -> 6));
	public static final Block BUDDING_PRISM_CRYSTAL = register("budding_prism_crystal", PrismBlocks.Budding::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.BUDDING_AMETHYST).mapColor(MapColor.COLOR_CYAN).lightLevel(state -> 8).randomTicks());
	public static final Block SMALL_PRISM_BUD = register("small_prism_bud", props -> new PrismBlocks.Cluster(3.0F, 4.0F, props),
			BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD).mapColor(MapColor.COLOR_CYAN));
	public static final Block MEDIUM_PRISM_BUD = register("medium_prism_bud", props -> new PrismBlocks.Cluster(4.0F, 3.0F, props),
			BlockBehaviour.Properties.ofFullCopy(Blocks.MEDIUM_AMETHYST_BUD).mapColor(MapColor.COLOR_CYAN));
	public static final Block LARGE_PRISM_BUD = register("large_prism_bud", props -> new PrismBlocks.Cluster(5.0F, 3.0F, props),
			BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_AMETHYST_BUD).mapColor(MapColor.COLOR_CYAN));
	public static final Block PRISM_CLUSTER = register("prism_cluster", props -> new PrismBlocks.Cluster(7.0F, 3.0F, props),
			BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).mapColor(MapColor.COLOR_CYAN));
	public static final Block PRISM_LAMP = register("prism_lamp", PrismBlocks.Crystal::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(state -> 15));

	/** Right-click it with a Phase Pearl to bind the pearl; the pearl then jumps to the top of this block. */
	public static final Block PHASE_ANCHOR = register("phase_anchor", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).mapColor(MapColor.COLOR_CYAN).lightLevel(state -> 9));

	public static final Block GEM_TABLE = register("gem_table", GemTableBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).mapColor(MapColor.COLOR_PURPLE));

	/** A 5x5 crafting grid for the End's greater recipes; see {@link hu.endrealms.craft.EndCrafting}. */
	public static final Block END_CRAFTING_TABLE = register("end_crafting_table", hu.endrealms.craft.EndCraftingTableBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).mapColor(MapColor.COLOR_PURPLE).lightLevel(state -> 4));

	public static final Block CRUMBLING_PLANKS = register("crumbling_planks", CrumblingPlanksBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_GRAY).pushReaction(PushReaction.POPPED));

	/** Only on the deep bed of the Astral Basin's lake. */
	public static final Block DEEPGLOW_ORE = register("deepglow_ore", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).mapColor(MapColor.COLOR_CYAN).lightLevel(state -> 9));

	// --- Canvas and Gravity Stone ---
	public static final Block CANVAS = register("canvas", hu.endrealms.block.CanvasBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.WOOL.white()).noOcclusion().strength(0.8F));
	public static final Block LARGE_CANVAS = register("large_canvas", hu.endrealms.block.LargeCanvasBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.WOOL.white()).noOcclusion().strength(0.8F));
	public static final Block GRAVITY_STONE = register("gravity_stone", hu.endrealms.block.GravityStoneBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_CYAN)
					.lightLevel(state -> state.getValue(hu.endrealms.block.GravityStoneBlock.POWERED) ? 10 : 2));

	// --- Phase Mail ---
	// --- Fermentation Station (an Overworld thing: apples into Apple Brandy) ---
	public static final Block FERMENTATION_STATION = register("fermentation_station", hu.endrealms.brew.FermentationStationBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).mapColor(MapColor.WOOD));

	public static final Block AGING_BARREL = register("aging_barrel", hu.endrealms.brew.AgingBarrelBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).mapColor(MapColor.COLOR_BROWN));

	public static final Block PHASE_MAILBOX = register("phase_mailbox", hu.endrealms.mail.PhaseMailboxBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PURPLE).lightLevel(state -> 6));

	// --- Chorusite ---
	public static final Block CHORUSITE_BLOCK = register("chorusite_block", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).mapColor(MapColor.COLOR_MAGENTA).lightLevel(state -> 9),
			(block, props) -> new BlockItem(block, props.rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

	// --- Dragon Graveyard ---
	public static final Block BONE_SOIL = register("bone_soil", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).mapColor(MapColor.SAND));
	public static final Block DRAGON_BONE_BLOCK = register("dragon_bone_block", RotatedPillarBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.BONE_BLOCK).mapColor(MapColor.COLOR_LIGHT_GREEN));

	// --- Scorched Barrens ---
	public static final Block SCORCHED_END_STONE = register("scorched_end_stone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).mapColor(MapColor.COLOR_GRAY));
	public static final Block ASH_BLOCK = register("ash_block", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW_BLOCK).mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.SAND));
	public static final Block ASH_LAYER = register("ash_layer", ScorchedBlocks.AshLayer::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW).mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.SAND));
	public static final Block EMBER_ROCK = register("ember_rock", ScorchedBlocks.EmberRock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.MAGMA_BLOCK).mapColor(MapColor.COLOR_MAGENTA).lightLevel(state -> 11));

	/** Burns what stands near it and erupts now and then; the crater heart of the volcanoes, and a craftable trap. */
	public static final Block VOLCANIC_VENT = register("volcanic_vent", VolcanicVentBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.MAGMA_BLOCK).mapColor(MapColor.COLOR_MAGENTA).lightLevel(state -> 13).randomTicks());

	/** A pumpkin mid-carving: shears turn a plain pumpkin into this. */
	public static final Block CARVED_PUMPKIN = register("carved_pumpkin", CarvedPumpkinBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.PUMPKIN).lightLevel(state -> state.getValue(CarvedPumpkinBlock.LIT) ? 15 : 0));

	// --- Tall plants of the Astral Forest ---
	public static final Block STARREED = register("starreed", StarreedBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.SUGAR_CANE).mapColor(MapColor.COLOR_PURPLE).lightLevel(state -> 3));
	public static final Block MOONBELL = register("moonbell", MoonbellBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollision().instabreak().sound(SoundType.GRASS)
					.pushReaction(PushReaction.POPPED).lightLevel(state -> state.getValue(MoonbellBlock.PART) == 2 ? 8 : 2));

	// --- Astral Basin underwater plants (only live in astral water) ---
	public static final Block STARWEED = register("starweed", StarweedBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.KELP_PLANT).mapColor(MapColor.COLOR_LIGHT_BLUE));
	public static final Block VOID_CORAL = register("void_coral",
			props -> new AstralWaterPlantBlock(Block.column(12.0, 0.0, 15.0), props),
			BlockBehaviour.Properties.ofFullCopy(Blocks.TUBE_CORAL).mapColor(MapColor.COLOR_PINK).lightLevel(state -> 8));

	// --- Void sand family (lake bed of the Astral Basin), same variants as vanilla sand/sandstone ---
	public static final Block VOID_SAND = register("void_sand",
			props -> new ColoredFallingBlock(new ColorRGBA(0xFFDCD9A8), props),
			BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.SAND));
	public static final Block VOID_SANDSTONE = register("void_sandstone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.SANDSTONE).mapColor(MapColor.SAND));
	public static final Block VOID_SANDSTONE_SLAB = slab("void_sandstone_slab", VOID_SANDSTONE);
	public static final Block VOID_SANDSTONE_STAIRS = stairs("void_sandstone_stairs", VOID_SANDSTONE);
	public static final Block VOID_SANDSTONE_WALL = wall("void_sandstone_wall", VOID_SANDSTONE);
	public static final Block SMOOTH_VOID_SANDSTONE = register("smooth_void_sandstone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_SANDSTONE).mapColor(MapColor.SAND));
	public static final Block SMOOTH_VOID_SANDSTONE_SLAB = slab("smooth_void_sandstone_slab", SMOOTH_VOID_SANDSTONE);
	public static final Block SMOOTH_VOID_SANDSTONE_STAIRS = stairs("smooth_void_sandstone_stairs", SMOOTH_VOID_SANDSTONE);
	public static final Block CUT_VOID_SANDSTONE = register("cut_void_sandstone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CUT_SANDSTONE).mapColor(MapColor.SAND));
	public static final Block CUT_VOID_SANDSTONE_SLAB = slab("cut_void_sandstone_slab", CUT_VOID_SANDSTONE);
	public static final Block CHISELED_VOID_SANDSTONE = register("chiseled_void_sandstone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_SANDSTONE).mapColor(MapColor.SAND));

	// --- Voidstone: purple End stone found below the top 3 layers of the islands ---
	public static final Block VOIDSTONE = register("voidstone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).mapColor(MapColor.COLOR_PURPLE));
	public static final Block POLISHED_VOIDSTONE = register("polished_voidstone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).mapColor(MapColor.COLOR_PURPLE));
	public static final Block VOIDSTONE_BRICKS = register("voidstone_bricks", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICKS).mapColor(MapColor.COLOR_PURPLE));

	public static final Block VOIDSTONE_SLAB = slab("voidstone_slab", VOIDSTONE);
	public static final Block VOIDSTONE_STAIRS = stairs("voidstone_stairs", VOIDSTONE);
	public static final Block VOIDSTONE_WALL = wall("voidstone_wall", VOIDSTONE);
	public static final Block POLISHED_VOIDSTONE_SLAB = slab("polished_voidstone_slab", POLISHED_VOIDSTONE);
	public static final Block POLISHED_VOIDSTONE_STAIRS = stairs("polished_voidstone_stairs", POLISHED_VOIDSTONE);
	public static final Block POLISHED_VOIDSTONE_WALL = wall("polished_voidstone_wall", POLISHED_VOIDSTONE);
	public static final Block VOIDSTONE_BRICK_SLAB = slab("voidstone_brick_slab", VOIDSTONE_BRICKS);
	public static final Block VOIDSTONE_BRICK_STAIRS = stairs("voidstone_brick_stairs", VOIDSTONE_BRICKS);
	public static final Block VOIDSTONE_BRICK_WALL = wall("voidstone_brick_wall", VOIDSTONE_BRICKS);

	// --- Astral wood (Astral Forest biome) ---
	public static final ResourceKey<Feature> ASTRAL_TREE_FEATURE =
			ResourceKey.create(Registries.FEATURE, EndRealms.id("astral_tree"));
	public static final TreeGrower ASTRAL_TREE_GROWER = new TreeGrower(EndRealms.MOD_ID + ":astral",
			WeightedList.of(ASTRAL_TREE_FEATURE), WeightedList.of(), WeightedList.of(), ASTRAL_TREE_FEATURE);

	public static final Block ASTRAL_LOG = register("astral_log", RotatedPillarBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LOG).mapColor(MapColor.COLOR_PURPLE).ignitedByLava());
	public static final Block ASTRAL_PLANKS = register("astral_planks", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS).mapColor(MapColor.TERRACOTTA_PURPLE));
	public static final Block STRIPPED_ASTRAL_LOG = register("stripped_astral_log", RotatedPillarBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_CHERRY_LOG).mapColor(MapColor.TERRACOTTA_PURPLE));
	public static final Block ASTRAL_WOOD = register("astral_wood", RotatedPillarBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_WOOD).mapColor(MapColor.COLOR_PURPLE));
	public static final Block STRIPPED_ASTRAL_WOOD = register("stripped_astral_wood", RotatedPillarBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_CHERRY_WOOD).mapColor(MapColor.TERRACOTTA_PURPLE));
	public static final Block ASTRAL_SLAB = slab("astral_slab", ASTRAL_PLANKS);
	public static final Block ASTRAL_STAIRS = stairs("astral_stairs", ASTRAL_PLANKS);
	public static final Block ASTRAL_FENCE = register("astral_fence", FenceBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_FENCE).mapColor(MapColor.TERRACOTTA_PURPLE));
	public static final Block ASTRAL_FENCE_GATE = register("astral_fence_gate",
			props -> new FenceGateBlock(WoodType.CHERRY, props),
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_FENCE_GATE).mapColor(MapColor.TERRACOTTA_PURPLE));
	public static final Block ASTRAL_DOOR = register("astral_door",
			props -> new DoorBlock(BlockSetType.CHERRY, props) {
			},
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_DOOR).mapColor(MapColor.TERRACOTTA_PURPLE),
			DoubleHighBlockItem::new);
	public static final Block ASTRAL_TRAPDOOR = register("astral_trapdoor",
			props -> new TrapDoorBlock(BlockSetType.CHERRY, props) {
			},
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_TRAPDOOR).mapColor(MapColor.TERRACOTTA_PURPLE));
	public static final Block ASTRAL_LEAVES = register("astral_leaves",
			props -> new UntintedParticleLeavesBlock(0.02F,
					ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFFB44FC4),
					AmbientLeavesBlockSoundPlayer.noAmbientSound(), props),
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.COLOR_MAGENTA));
	public static final Block ASTRAL_SAPLING = register("astral_sapling",
			props -> new EndSaplingBlock(ASTRAL_TREE_GROWER, props),
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING).mapColor(MapColor.COLOR_MAGENTA));

	/** Chorusite ore inside voidstone (like deepslate ores): same drops, a bit harder to mine. */
	public static final Block VOIDSTONE_CHORUSITE_ORE = register("voidstone_chorusite_ore",
			props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
			BlockBehaviour.Properties.ofFullCopy(CHORUSITE_ORE).mapColor(MapColor.COLOR_PURPLE).strength(5.5F, 9.0F));

	/** The way into the Sculk dimension (and back): a pool of living dark in the ground. Unbreakable, drops nothing. */
	public static final Block SCULK_PORTAL = register("sculk_portal", hu.endrealms.sculk.SculkPortalBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noCollision().strength(-1.0F, 3600000.0F).noLootTable()
					.lightLevel(state -> 5).sound(net.minecraft.world.level.block.SoundType.SCULK)
					.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE));

	/** The ground of the Sculk dimension: looks like sculk, but it is crumbly earth, dug with a shovel. */
	public static final Block SCULK_SOIL = register("sculk_soil", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.COLOR_BLACK).sound(SoundType.SCULK));

	/** The rock under the Sculk dimension: deepslate-dark, veined with sculk. */
	public static final Block SCULK_STONE = register("sculk_stone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.COLOR_BLACK));

	/** Pale bell of the Sculk dimension: walk past and it plays your footsteps back to you (SculkLife). */
	public static final Block ECHO_FLOWER = register("echo_flower",
			props -> new hu.endrealms.sculk.SculkPlants.Plant(props, Block.box(4, 0, 4, 12, 11, 12)),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).noCollision().instabreak().sound(SoundType.SCULK_SENSOR)
					.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(net.minecraft.world.level.material.PushReaction.POPPED)
					.lightLevel(state -> 2));

	/** The common grass of the Sculk dimension: three blocks tall, with eyes on top that follow you. */
	public static final Block WATCHER_GRASS = register("watcher_grass", hu.endrealms.sculk.SculkPlants.WatcherGrass::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noCollision().instabreak().sound(SoundType.SCULK_VEIN)
					.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(net.minecraft.world.level.material.PushReaction.POPPED)
					.lightLevel(state -> state.getValue(hu.endrealms.sculk.SculkPlants.WatcherGrass.PART) == hu.endrealms.sculk.SculkPlants.Part.TOP
							&& state.getValue(hu.endrealms.sculk.SculkPlants.WatcherGrass.EYES) != hu.endrealms.sculk.SculkPlants.Eyes.CLOSED ? 3 : 0));

	/** Glowing mushroom of the Sculk dimension, one of its few lights; the Bone Stag's food. */
	public static final Block SOUL_MUSHROOM = register("soul_mushroom",
			props -> new hu.endrealms.sculk.SculkPlants.Plant(props, Block.box(4, 0, 4, 12, 10, 12)),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollision().instabreak().sound(SoundType.FUNGUS)
					.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(net.minecraft.world.level.material.PushReaction.POPPED)
					.lightLevel(state -> 10));

	/** Smoky blue-grey glass the Shadow City is built of; you can almost see the people who lived behind it. */
	public static final Block ECHO_GLASS = register("echo_glass", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).mapColor(MapColor.COLOR_CYAN));

	/** Bricks of Sculk Stone, the walls of the old ruins of the Sculk dimension. */
	public static final Block SCULK_STONE_BRICKS = register("sculk_stone_bricks", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICKS).mapColor(MapColor.COLOR_BLACK));

	/** Pale berries hanging under the upside-down canopies of the Hanging Woods. */
	public static final Block DANGLE_BERRY_BUSH = register("dangle_berry_bush", hu.endrealms.sculk.SculkWilds.HangingBerry::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).noCollision().instabreak().sound(SoundType.CAVE_VINES)
					.pushReaction(net.minecraft.world.level.material.PushReaction.POPPED).lightLevel(state -> 4));

	/** Black climbing vine of the Listening Pillars; carries glowing blue berries now and then. */
	public static final Block THORN_VINE = register("thorn_vine", hu.endrealms.sculk.SculkWilds.ThornVine::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noCollision().strength(0.3F).sound(SoundType.CAVE_VINES)
					.pushReaction(net.minecraft.world.level.material.PushReaction.POPPED).randomTicks()
					.lightLevel(state -> state.getValue(hu.endrealms.sculk.SculkWilds.ThornVine.BERRIES) ? 7 : 0));

	/** Grey flower at the feet of the Listening Pillars that bursts into ash when brushed. */
	public static final Block ASH_FLOWER = register("ash_flower", hu.endrealms.sculk.SculkWilds.AshFlower::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).noCollision().instabreak().sound(SoundType.GRASS)
					.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(net.minecraft.world.level.material.PushReaction.POPPED));

	/** The hidden heart of the great bells of the Bell Valley, which makes them toll. */
	public static final Block BELL_CORE = register("bell_core", hu.endrealms.sculk.SculkWilds.BellCore::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().oxidized()).mapColor(MapColor.WARPED_NYLIUM).randomTicks());

	private ModBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties props) {
		return register(name, factory, props, BlockItem::new);
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties props, BiFunction<Block, Item.Properties, Item> itemFactory) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, EndRealms.id(name));
		Block block = factory.apply(props.setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, EndRealms.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				itemFactory.apply(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	private static Block slab(String name, Block base) {
		return register(name, SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(base));
	}

	private static Block stairs(String name, Block base) {
		return register(name, props -> new StairBlock(base.defaultBlockState(), props) {
		}, BlockBehaviour.Properties.ofFullCopy(base));
	}

	private static Block wall(String name, Block base) {
		return register(name, WallBlock::new, BlockBehaviour.Properties.ofFullCopy(base).forceSolidOn());
	}

	public static void init() {
	}
}
