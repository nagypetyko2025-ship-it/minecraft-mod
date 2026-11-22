package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import hu.endrealms.item.MoonbellNectarItem;
import hu.endrealms.item.AshMealItem;
import hu.endrealms.item.EndGuideItem;
import hu.endrealms.item.PhasePearlItem;
import hu.endrealms.item.RiftbladeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModItems {
	/** Netherite: 2031 durability, 9.0 speed, +4 damage, 15 enchantability. Chorusite beats it on every stat. */
	public static final ToolMaterial CHORUSITE_MATERIAL = new ToolMaterial(
			ModTags.INCORRECT_FOR_CHORUSITE_TOOL, 2600, 12.0F, 5.0F, 18, ModTags.CHORUSITE_TOOL_MATERIALS);

	/** Netherite: 37 durability factor, armor 3/8/6/3, 3 toughness, 0.1 knockback resistance, 15 enchantability. */
	public static final ArmorMaterial CHORUSITE_ARMOR_MATERIAL = new ArmorMaterial(45,
			Map.of(ArmorType.BOOTS, 4, ArmorType.LEGGINGS, 7, ArmorType.CHESTPLATE, 9, ArmorType.HELMET, 4, ArmorType.BODY, 13),
			18, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.15F, ModTags.CHORUSITE_TOOL_MATERIALS,
			ResourceKey.create(EquipmentAssets.ROOT_ID, EndRealms.id("chorusite")));

	// The netherite chain: ore -> shard; 4 shards + 4 diamonds -> pure shard; pure shard -> (furnace) -> ingot
	public static final Item CHORUSITE_SHARD = register("chorusite_shard", Item::new, new Item.Properties());
	public static final Item PURE_CHORUSITE_SHARD = register("pure_chorusite_shard", Item::new, new Item.Properties().fireResistant());
	public static final Item PHASITE_SHARD = register("phasite_shard", Item::new, new Item.Properties());
	/** Armor trim material, cut from phasite with prism shards. */
	public static final Item PHASITE_GEM = register("phasite_gem", Item::new, new Item.Properties()
			.trimMaterial(ResourceKey.create(Registries.TRIM_MATERIAL, EndRealms.id("phasite"))));
	public static final Item PHASE_PEARL = register("phase_pearl", PhasePearlItem::new, new Item.Properties()
			.durability(9).repairable(PHASITE_SHARD).rarity(Rarity.RARE));
	public static final Item CHORUSITE_INGOT = register("chorusite_ingot", Item::new,
			new Item.Properties().fireResistant());

	public static final Item CHORUSITE_UPGRADE_SMITHING_TEMPLATE = register("chorusite_upgrade_smithing_template",
			ModItems::createUpgradeTemplate, new Item.Properties());

	// Same per-tool damage/speed modifiers as the vanilla netherite tools; the material adds the extra power.
	public static final Item CHORUSITE_SWORD = register("chorusite_sword", Item::new,
			new Item.Properties().sword(CHORUSITE_MATERIAL, 3.0F, -2.4F).fireResistant());
	/** Dropped once by each Rift Lord; the only way to a Riftblade. */
	public static final Item RIFT_HEART = register("rift_heart", Item::new, new Item.Properties().rarity(Rarity.EPIC).fireResistant());
	public static final Item RIFTBLADE = register("riftblade", RiftbladeItem::new,
			new Item.Properties().sword(CHORUSITE_MATERIAL, 5.0F, -2.4F).fireResistant().rarity(Rarity.EPIC));
	public static final Item END_GUIDE = register("end_guide", EndGuideItem::new, new Item.Properties().stacksTo(1));
	public static final Item CHORUSITE_SHOVEL = register("chorusite_shovel", Item::new,
			new Item.Properties().shovel(CHORUSITE_MATERIAL, 1.5F, -3.0F).fireResistant());
	public static final Item CHORUSITE_PICKAXE = register("chorusite_pickaxe", Item::new,
			new Item.Properties().pickaxe(CHORUSITE_MATERIAL, 1.0F, -2.8F).fireResistant());
	public static final Item CHORUSITE_AXE = register("chorusite_axe", Item::new,
			new Item.Properties().axe(CHORUSITE_MATERIAL, 5.0F, -3.0F).fireResistant());
	public static final Item CHORUSITE_HOE = register("chorusite_hoe", Item::new,
			new Item.Properties().hoe(CHORUSITE_MATERIAL, -5.0F, 0.0F).fireResistant());

	public static final Item CHORUSITE_HELMET = armor("chorusite_helmet", ArmorType.HELMET);
	public static final Item CHORUSITE_CHESTPLATE = armor("chorusite_chestplate", ArmorType.CHESTPLATE);
	public static final Item CHORUSITE_LEGGINGS = armor("chorusite_leggings", ArmorType.LEGGINGS);
	public static final Item CHORUSITE_BOOTS = armor("chorusite_boots", ArmorType.BOOTS);

	// Glimmerhoof meat: exactly as filling as beef / steak
	public static final Item RAW_GLIMMERHOOF = register("raw_glimmerhoof", Item::new,
			new Item.Properties().food(Foods.BEEF));
	public static final Item COOKED_GLIMMERHOOF = register("cooked_glimmerhoof", Item::new,
			new Item.Properties().food(Foods.COOKED_BEEF));
	public static final Item GLIMMERHOOF_SPAWN_EGG = register("glimmerhoof_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.GLIMMERHOOF));

	/** Eaten in under a second; slows your fall and gives a short burst of speed for hopping between islands. */
	public static final Item STARBERRY = register("starberry", hu.endrealms.item.StarberryItem::new, new Item.Properties().food(new FoodProperties(2, 0.2F, true),
			Consumables.defaultFood().consumeSeconds(0.8F).onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
					new MobEffectInstance(MobEffects.SLOW_FALLING, 300), new MobEffectInstance(MobEffects.SPEED, 160)))).build()));
	public static final Item MOONBELL_NECTAR = register("moonbell_nectar", MoonbellNectarItem::new, new Item.Properties().stacksTo(16)
			.food(new FoodProperties(3, 0.3F, true), Consumables.defaultDrink().onConsume(
					new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400))).build()));

	// Astral Basin fish: Starfin ~ cod, Void Eel ~ salmon, Glowgill a bit better than both when cooked
	public static final Item RAW_STARFIN = register("raw_starfin", Item::new, new Item.Properties().food(Foods.COD));
	public static final Item COOKED_STARFIN = register("cooked_starfin", Item::new, new Item.Properties().food(Foods.COOKED_COD,
			Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 300))).build()));
	public static final Item RAW_VOID_EEL = register("raw_void_eel", Item::new, new Item.Properties().food(Foods.SALMON));
	public static final Item COOKED_VOID_EEL = register("cooked_void_eel", Item::new, new Item.Properties().food(Foods.COOKED_SALMON,
			Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 900))).build()));
	public static final Item RAW_GLOWGILL = register("raw_glowgill", Item::new, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.1F).build()));
	public static final Item COOKED_GLOWGILL = register("cooked_glowgill", Item::new, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.7F).build(),
					Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 600))).build()));
	public static final Item STARFIN_SPAWN_EGG = register("starfin_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.STARFIN));
	public static final Item VOID_EEL_SPAWN_EGG = register("void_eel_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.VOID_EEL));
	public static final Item GLOWGILL_SPAWN_EGG = register("glowgill_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.GLOWGILL));

	public static final Item VOID_SENTINEL_SPAWN_EGG = register("void_sentinel_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.VOID_SENTINEL));

	public static final Item PRISM_SHARD = register("prism_shard", Item::new, new Item.Properties());
	// --- Brandies from the Fermentation Station: each its own buff, plus nausea and Drunkenness (see BrandyItem) ---
	public static final Item APPLE_BRANDY = brandy("apple_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.STRENGTH, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.REGENERATION, 20 * 10)));
	public static final Item GLOW_BERRY_BRANDY = brandy("glow_berry_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.NIGHT_VISION, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.HASTE, hu.endrealms.brew.Brewing.DURATION)));
	public static final Item SWEET_BERRY_BRANDY = brandy("sweet_berry_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.SPEED, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.SATURATION, 10)));
	public static final Item NECTAR_BRANDY = brandy("moonbell_nectar_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.RESISTANCE, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.FIRE_RESISTANCE, hu.endrealms.brew.Brewing.DURATION)));
	public static final Item STARBERRY_BRANDY = brandy("starberry_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.JUMP_BOOST, hu.endrealms.brew.Brewing.DURATION, 1), new MobEffectInstance(MobEffects.SLOW_FALLING, hu.endrealms.brew.Brewing.DURATION)));

	// --- Mixed brandies (two or three ingredients) ---
	public static final Item FRUIT_MEDLEY_BRANDY = brandy("fruit_medley_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.STRENGTH, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.SPEED, hu.endrealms.brew.Brewing.DURATION)));
	public static final Item HONEYED_APPLE_BRANDY = brandy("honeyed_apple_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.REGENERATION, 20 * 45), new MobEffectInstance(MobEffects.ABSORPTION, hu.endrealms.brew.Brewing.DURATION, 1)));
	public static final Item GLOWING_NECTAR_BRANDY = register("glowing_nectar_brandy", props -> new hu.endrealms.brew.BrandyItem(props, () -> List.of(
			new MobEffectInstance(MobEffects.NIGHT_VISION, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.RESISTANCE, hu.endrealms.brew.Brewing.DURATION)),
			true), brandyProperties());
	public static final Item CHORUS_BERRY_BRANDY = brandy("chorus_berry_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.SLOW_FALLING, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.JUMP_BOOST, hu.endrealms.brew.Brewing.DURATION, 1)));
	public static final Item MELON_REFRESHER = brandy("melon_refresher", () -> List.of(
			new MobEffectInstance(MobEffects.FIRE_RESISTANCE, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.WATER_BREATHING, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 1)));
	public static final Item ENDLESS_BRANDY = brandy("endless_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.LEVITATION, 20 * 6), new MobEffectInstance(MobEffects.SLOW_FALLING, hu.endrealms.brew.Brewing.DURATION),
			new MobEffectInstance(MobEffects.JUMP_BOOST, hu.endrealms.brew.Brewing.DURATION, 1), new MobEffectInstance(MobEffects.SPEED, hu.endrealms.brew.Brewing.DURATION)));
	public static final Item MINERS_BRANDY = brandy("miners_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.HASTE, hu.endrealms.brew.Brewing.DURATION, 1), new MobEffectInstance(MobEffects.NIGHT_VISION, hu.endrealms.brew.Brewing.DURATION)));
	public static final Item DRAGON_BLOOD_BRANDY = brandy("dragon_blood_brandy", () -> List.of(
			new MobEffectInstance(MobEffects.STRENGTH, hu.endrealms.brew.Brewing.DURATION, 1), new MobEffectInstance(MobEffects.FIRE_RESISTANCE, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.RESISTANCE, hu.endrealms.brew.Brewing.DURATION)));
	public static final Item FOREST_ELIXIR = brandy("forest_elixir", () -> List.of(
			new MobEffectInstance(MobEffects.SPEED, hu.endrealms.brew.Brewing.DURATION, 1), new MobEffectInstance(MobEffects.SATURATION, 10)));
	public static final Item VOID_ESSENCE = brandy("void_essence", () -> List.of(
			new MobEffectInstance(MobEffects.INVISIBILITY, hu.endrealms.brew.Brewing.DURATION), new MobEffectInstance(MobEffects.SLOW_FALLING, hu.endrealms.brew.Brewing.DURATION)));

	private static Item.Properties brandyProperties() {
		return new Item.Properties().stacksTo(16).usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
				.food(new FoodProperties(4, 0.4F, true), Consumables.defaultDrink().build());
	}

	private static Item brandy(String name, java.util.function.Supplier<List<MobEffectInstance>> effects) {
		return register(name, props -> new hu.endrealms.brew.BrandyItem(props, effects), new Item.Properties().stacksTo(16)
				.usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
				.food(new FoodProperties(4, 0.4F, true), Consumables.defaultDrink().build()));
	}

	public static final Item BACKPACK = register("backpack", Item::new, new Item.Properties().stacksTo(1));

	public static final Item DRAGON_BONE = register("dragon_bone", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	/** From the Echo Flower. */
	public static final Item ECHO_DUST = register("echo_dust", Item::new, new Item.Properties());
	/** Lights the portal frame in the heart of an Ancient City: the way into the Sculk dimension. */
	public static final Item ECHO_KEY = register("echo_key", hu.endrealms.sculk.EchoKeyItem::new,
			new Item.Properties().durability(8).rarity(Rarity.RARE));
	/** From the Bone Stag. */
	public static final Item SCULK_ANTLER = register("sculk_antler", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item BONE_STAG_SPAWN_EGG = register("bone_stag_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.BONE_STAG));

	public static final Item SPECTRAL_WHELP_SPAWN_EGG = register("spectral_whelp_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.SPECTRAL_WHELP));

	public static final Item BONE_DRAKE_SPAWN_EGG = register("bone_drake_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.BONE_DRAKE));

	public static final Item VOID_KING_SPAWN_EGG = register("void_king_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.VOID_KING));

	public static final Item RIFT_LORD_SPAWN_EGG = register("rift_lord_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.RIFT_LORD));
	public static final Item EMBER_GARNET = register("ember_garnet", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item STAR_SAPPHIRE = register("star_sapphire", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item VOID_ONYX = register("void_onyx", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item MOON_OPAL = register("moon_opal", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item ASH_QUARTZ = register("ash_quartz", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item BLOODSTONE = register("bloodstone", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item CROWNSTONE = register("crownstone", Item::new, new Item.Properties().rarity(Rarity.EPIC).fireResistant());
	public static final Item DEEPGLOW_SHARD = register("deepglow_shard", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item DEEP_AQUAMARINE = register("deep_aquamarine", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item DEEP_LURKER_SPAWN_EGG = register("deep_lurker_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.DEEP_LURKER));
	public static final Item ASH_MEAL = register("ash_meal", AshMealItem::new, new Item.Properties());
	public static final Item ASH_DUST = register("ash_dust", Item::new, new Item.Properties());
	public static final Item ASH_WRAITH_SPAWN_EGG = register("ash_wraith_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.ASH_WRAITH));
	public static final Item SHARDCRAB_SPAWN_EGG = register("shardcrab_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.SHARDCRAB));

	public static final Item END_VILLAGER_SPAWN_EGG = register("end_villager_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.END_VILLAGER));

	private ModItems() {
	}

	private static SmithingTemplateItem createUpgradeTemplate(Item.Properties props) {
		String base = "item." + EndRealms.MOD_ID + ".smithing_template.chorusite_upgrade.";
		return new SmithingTemplateItem(
				Component.translatable(base + "applies_to").withStyle(ChatFormatting.BLUE),
				Component.translatable(base + "ingredients").withStyle(ChatFormatting.BLUE),
				Component.translatable(base + "base_slot_description"),
				Component.translatable(base + "additions_slot_description"),
				List.of(slot("helmet"), slot("sword"), slot("chestplate"), slot("pickaxe"), slot("leggings"), slot("axe"), slot("boots"), slot("hoe"), slot("shovel")),
				List.of(slot("ingot")),
				props);
	}

	private static Item armor(String name, ArmorType type) {
		return register(name, Item::new, new Item.Properties().humanoidArmor(CHORUSITE_ARMOR_MATERIAL, type).fireResistant());
	}

	private static Identifier slot(String name) {
		return Identifier.withDefaultNamespace("container/slot/" + name);
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory,
			Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, EndRealms.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
	}

	public static void init() {
	}
}
