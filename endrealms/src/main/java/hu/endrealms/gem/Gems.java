package hu.endrealms.gem;

import com.mojang.serialization.Codec;
import hu.endrealms.EndRealms;
import hu.endrealms.registry.ModItems;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Gem sockets: every tool, weapon and armor piece holds {@link #MAX_SOCKETS} gem, and the same gem
 * gives a different bonus depending on the kind of item. The socketed gems are stored on the item; its attributes and
 * durability are always derived from them, so changing a bonus here is all it takes to rebalance a gem.
 */
public final class Gems {
	public static final int MAX_SOCKETS = 1;
	public static final DataComponentType<List<String>> SOCKETED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EndRealms.id("gems"),
			DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

	private Gems() {
	}

	public enum Category {
		WEAPON(EquipmentSlotGroup.MAINHAND), AXE(EquipmentSlotGroup.MAINHAND), PICKAXE(EquipmentSlotGroup.MAINHAND), SHOVEL(EquipmentSlotGroup.MAINHAND),
		HOE(EquipmentSlotGroup.MAINHAND), HELMET(EquipmentSlotGroup.HEAD), CHESTPLATE(EquipmentSlotGroup.CHEST), LEGGINGS(EquipmentSlotGroup.LEGS), BOOTS(EquipmentSlotGroup.FEET);

		final EquipmentSlotGroup group;

		Category(EquipmentSlotGroup group) {
			this.group = group;
		}

		public Component label() {
			return Component.translatable("gem.endrealms.category." + name().toLowerCase(java.util.Locale.ROOT));
		}
	}

	/** Either an attribute change or, when {@code attribute} is null, one of the special effects named by {@code special}. */
	public record Bonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, String special) {
		private static final String DURABILITY = "durability";
		private static final String IGNITE = "ignite";
		private static final String LIFESTEAL = "lifesteal";

		private static Bonus add(Holder<Attribute> attribute, double amount) {
			return new Bonus(attribute, amount, AttributeModifier.Operation.ADD_VALUE, null);
		}

		private static Bonus percent(Holder<Attribute> attribute, double amount) {
			return new Bonus(attribute, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, null);
		}

		private static Bonus special(String kind, double amount) {
			return new Bonus(null, amount, AttributeModifier.Operation.ADD_VALUE, kind);
		}

		private static Bonus durability(int amount) {
			return special(DURABILITY, amount);
		}

		private static String number(double value) {
			return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
		}

		public Component describe() {
			if (attribute == null) {
				return Component.translatable("gem.endrealms." + special, number(LIFESTEAL.equals(special) ? amount * 100.0 : amount));
			}
			Component name = Component.translatable(attribute.value().getDescriptionId());
			String sign = amount < 0.0 ? "take" : "plus";
			return operation == AttributeModifier.Operation.ADD_VALUE
					? Component.translatable("attribute.modifier." + sign + ".0", number(Math.abs(amount)), name)
					: Component.translatable("attribute.modifier." + sign + ".1", number(Math.abs(amount) * 100.0), name);
		}
	}

	public enum Gem {
		EMBER_GARNET("ember_garnet"), STAR_SAPPHIRE("star_sapphire"), VOID_ONYX("void_onyx"),
		MOON_OPAL("moon_opal"), ASH_QUARTZ("ash_quartz"), BLOODSTONE("bloodstone"), DEEP_AQUAMARINE("deep_aquamarine"), CROWNSTONE("crownstone");

		public final String id;
		private final Map<Category, List<Bonus>> bonuses = new EnumMap<>(Category.class);

		Gem(String id) {
			this.id = id;
		}

		public List<Bonus> bonuses(Category category) {
			return bonuses.get(category);
		}

		public Item item() {
			return switch (this) {
				case EMBER_GARNET -> ModItems.EMBER_GARNET;
				case STAR_SAPPHIRE -> ModItems.STAR_SAPPHIRE;
				case VOID_ONYX -> ModItems.VOID_ONYX;
				case MOON_OPAL -> ModItems.MOON_OPAL;
				case ASH_QUARTZ -> ModItems.ASH_QUARTZ;
				case BLOODSTONE -> ModItems.BLOODSTONE;
				case DEEP_AQUAMARINE -> ModItems.DEEP_AQUAMARINE;
				case CROWNSTONE -> ModItems.CROWNSTONE;
			};
		}

		public Component displayName() {
			return Component.translatable("item.endrealms." + id);
		}
	}

	static {
		Map<Category, List<Bonus>> garnet = Gem.EMBER_GARNET.bonuses;
		garnet.put(Category.WEAPON, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 3.0)));
		garnet.put(Category.AXE, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 2.0), Bonus.add(Attributes.MINING_EFFICIENCY, 15.0)));
		garnet.put(Category.PICKAXE, List.of(Bonus.add(Attributes.MINING_EFFICIENCY, 33.0)));
		garnet.put(Category.SHOVEL, List.of(Bonus.add(Attributes.MINING_EFFICIENCY, 33.0)));
		garnet.put(Category.HOE, List.of(Bonus.add(Attributes.MINING_EFFICIENCY, 33.0)));
		garnet.put(Category.HELMET, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 1.0)));
		garnet.put(Category.CHESTPLATE, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 2.0)));
		garnet.put(Category.LEGGINGS, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 1.5)));
		garnet.put(Category.BOOTS, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 1.0)));

		Map<Category, List<Bonus>> sapphire = Gem.STAR_SAPPHIRE.bonuses;
		sapphire.put(Category.WEAPON, List.of(Bonus.add(Attributes.ATTACK_SPEED, 0.5), Bonus.add(Attributes.ENTITY_INTERACTION_RANGE, 1.0)));
		sapphire.put(Category.AXE, List.of(Bonus.add(Attributes.ATTACK_SPEED, 0.4), Bonus.add(Attributes.ENTITY_INTERACTION_RANGE, 0.5)));
		sapphire.put(Category.PICKAXE, List.of(Bonus.add(Attributes.BLOCK_INTERACTION_RANGE, 2.0)));
		sapphire.put(Category.SHOVEL, List.of(Bonus.add(Attributes.BLOCK_INTERACTION_RANGE, 2.0)));
		sapphire.put(Category.HOE, List.of(Bonus.add(Attributes.BLOCK_INTERACTION_RANGE, 2.0)));
		sapphire.put(Category.HELMET, List.of(Bonus.add(Attributes.OXYGEN_BONUS, 2.0)));
		sapphire.put(Category.CHESTPLATE, List.of(Bonus.percent(Attributes.MOVEMENT_SPEED, 0.06)));
		sapphire.put(Category.LEGGINGS, List.of(Bonus.add(Attributes.STEP_HEIGHT, 0.5)));
		sapphire.put(Category.BOOTS, List.of(Bonus.percent(Attributes.MOVEMENT_SPEED, 0.12), Bonus.add(Attributes.JUMP_STRENGTH, 0.1)));

		Map<Category, List<Bonus>> onyx = Gem.VOID_ONYX.bonuses;
		onyx.put(Category.WEAPON, List.of(Bonus.add(Attributes.ATTACK_KNOCKBACK, 1.0), Bonus.durability(300)));
		onyx.put(Category.AXE, List.of(Bonus.durability(500)));
		onyx.put(Category.PICKAXE, List.of(Bonus.durability(800)));
		onyx.put(Category.SHOVEL, List.of(Bonus.durability(800)));
		onyx.put(Category.HOE, List.of(Bonus.durability(800)));
		onyx.put(Category.HELMET, List.of(Bonus.add(Attributes.ARMOR, 2.0), Bonus.add(Attributes.ARMOR_TOUGHNESS, 1.0)));
		onyx.put(Category.CHESTPLATE, List.of(Bonus.add(Attributes.MAX_HEALTH, 4.0)));
		onyx.put(Category.LEGGINGS, List.of(Bonus.add(Attributes.ARMOR, 2.0), Bonus.add(Attributes.ARMOR_TOUGHNESS, 1.0)));
		onyx.put(Category.BOOTS, List.of(Bonus.add(Attributes.ARMOR, 2.0), Bonus.add(Attributes.KNOCKBACK_RESISTANCE, 0.1)));

		Map<Category, List<Bonus>> opal = Gem.MOON_OPAL.bonuses;
		opal.put(Category.WEAPON, List.of(Bonus.add(Attributes.SWEEPING_DAMAGE_RATIO, 0.5)));
		opal.put(Category.AXE, List.of(Bonus.add(Attributes.ATTACK_KNOCKBACK, 1.0)));
		opal.put(Category.PICKAXE, List.of(Bonus.add(Attributes.SUBMERGED_MINING_SPEED, 0.8)));
		opal.put(Category.SHOVEL, List.of(Bonus.add(Attributes.SUBMERGED_MINING_SPEED, 0.8)));
		opal.put(Category.HOE, List.of(Bonus.add(Attributes.SUBMERGED_MINING_SPEED, 0.8)));
		opal.put(Category.HELMET, List.of(Bonus.add(Attributes.LUCK, 2.0)));
		opal.put(Category.CHESTPLATE, List.of(Bonus.percent(Attributes.GRAVITY, -0.15)));
		opal.put(Category.LEGGINGS, List.of(Bonus.add(Attributes.SNEAKING_SPEED, 0.3)));
		opal.put(Category.BOOTS, List.of(Bonus.add(Attributes.SAFE_FALL_DISTANCE, 5.0)));

		Map<Category, List<Bonus>> quartz = Gem.ASH_QUARTZ.bonuses;
		quartz.put(Category.WEAPON, List.of(Bonus.special(Bonus.IGNITE, 5.0)));
		quartz.put(Category.AXE, List.of(Bonus.special(Bonus.IGNITE, 3.0)));
		quartz.put(Category.PICKAXE, List.of(Bonus.percent(Attributes.BLOCK_BREAK_SPEED, 0.3)));
		quartz.put(Category.SHOVEL, List.of(Bonus.percent(Attributes.BLOCK_BREAK_SPEED, 0.3)));
		quartz.put(Category.HOE, List.of(Bonus.percent(Attributes.BLOCK_BREAK_SPEED, 0.3)));
		quartz.put(Category.HELMET, List.of(Bonus.percent(Attributes.BURNING_TIME, -0.25)));
		quartz.put(Category.CHESTPLATE, List.of(Bonus.percent(Attributes.BURNING_TIME, -0.25), Bonus.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.5)));
		quartz.put(Category.LEGGINGS, List.of(Bonus.percent(Attributes.BURNING_TIME, -0.25)));
		quartz.put(Category.BOOTS, List.of(Bonus.percent(Attributes.BURNING_TIME, -0.25), Bonus.add(Attributes.MOVEMENT_EFFICIENCY, 0.5)));

		Map<Category, List<Bonus>> blood = Gem.BLOODSTONE.bonuses;
		blood.put(Category.WEAPON, List.of(Bonus.special(Bonus.LIFESTEAL, 0.2)));
		blood.put(Category.AXE, List.of(Bonus.special(Bonus.LIFESTEAL, 0.15)));
		blood.put(Category.PICKAXE, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 2.0)));
		blood.put(Category.SHOVEL, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 2.0)));
		blood.put(Category.HOE, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 2.0)));
		blood.put(Category.HELMET, List.of(Bonus.add(Attributes.MAX_HEALTH, 2.0)));
		blood.put(Category.CHESTPLATE, List.of(Bonus.add(Attributes.MAX_HEALTH, 6.0)));
		blood.put(Category.LEGGINGS, List.of(Bonus.add(Attributes.MAX_HEALTH, 4.0)));
		blood.put(Category.BOOTS, List.of(Bonus.add(Attributes.MAX_HEALTH, 2.0)));

		Map<Category, List<Bonus>> aqua = Gem.DEEP_AQUAMARINE.bonuses;
		aqua.put(Category.WEAPON, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 1.5), Bonus.add(Attributes.ENTITY_INTERACTION_RANGE, 0.5)));
		aqua.put(Category.AXE, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 1.0), Bonus.add(Attributes.ATTACK_KNOCKBACK, 0.5)));
		aqua.put(Category.PICKAXE, List.of(Bonus.add(Attributes.SUBMERGED_MINING_SPEED, 0.4), Bonus.add(Attributes.MINING_EFFICIENCY, 12.0)));
		aqua.put(Category.SHOVEL, List.of(Bonus.add(Attributes.SUBMERGED_MINING_SPEED, 0.4), Bonus.add(Attributes.MINING_EFFICIENCY, 12.0)));
		aqua.put(Category.HOE, List.of(Bonus.add(Attributes.SUBMERGED_MINING_SPEED, 0.4), Bonus.add(Attributes.MINING_EFFICIENCY, 12.0)));
		aqua.put(Category.HELMET, List.of(Bonus.add(Attributes.OXYGEN_BONUS, 4.0)));
		aqua.put(Category.CHESTPLATE, List.of(Bonus.add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.5)));
		aqua.put(Category.LEGGINGS, List.of(Bonus.add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.3)));
		aqua.put(Category.BOOTS, List.of(Bonus.add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.5), Bonus.add(Attributes.SAFE_FALL_DISTANCE, 2.0)));

		// dropped only by the Void King, so it combines what the craftable gems give one at a time
		Map<Category, List<Bonus>> crown = Gem.CROWNSTONE.bonuses;
		crown.put(Category.WEAPON, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 4.0), Bonus.special(Bonus.LIFESTEAL, 0.1)));
		crown.put(Category.AXE, List.of(Bonus.add(Attributes.ATTACK_DAMAGE, 3.0), Bonus.special(Bonus.LIFESTEAL, 0.08)));
		crown.put(Category.PICKAXE, List.of(Bonus.add(Attributes.MINING_EFFICIENCY, 33.0), Bonus.percent(Attributes.BLOCK_BREAK_SPEED, 0.2), Bonus.durability(500)));
		crown.put(Category.SHOVEL, List.of(Bonus.add(Attributes.MINING_EFFICIENCY, 33.0), Bonus.percent(Attributes.BLOCK_BREAK_SPEED, 0.2), Bonus.durability(500)));
		crown.put(Category.HOE, List.of(Bonus.add(Attributes.MINING_EFFICIENCY, 33.0), Bonus.percent(Attributes.BLOCK_BREAK_SPEED, 0.2), Bonus.durability(500)));
		crown.put(Category.HELMET, List.of(Bonus.add(Attributes.MAX_HEALTH, 2.0), Bonus.add(Attributes.ARMOR, 2.0)));
		crown.put(Category.CHESTPLATE, List.of(Bonus.add(Attributes.MAX_HEALTH, 8.0), Bonus.add(Attributes.ARMOR_TOUGHNESS, 1.0)));
		crown.put(Category.LEGGINGS, List.of(Bonus.add(Attributes.MAX_HEALTH, 4.0), Bonus.add(Attributes.ARMOR, 2.0)));
		crown.put(Category.BOOTS, List.of(Bonus.add(Attributes.MAX_HEALTH, 2.0), Bonus.percent(Attributes.MOVEMENT_SPEED, 0.08)));
	}

	/** The kind of item for gem purposes, or null if gems cannot go into it. */
	public static Category categoryOf(ItemStack stack) {
		if (stack.is(ItemTags.AXES)) {
			return Category.AXE;
		}
		if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.SPEARS) || stack.is(Items.MACE) || stack.is(Items.TRIDENT)) {
			return Category.WEAPON;
		}
		if (stack.is(ItemTags.PICKAXES)) {
			return Category.PICKAXE;
		}
		if (stack.is(ItemTags.SHOVELS)) {
			return Category.SHOVEL;
		}
		if (stack.is(ItemTags.HOES)) {
			return Category.HOE;
		}
		if (stack.is(ItemTags.HEAD_ARMOR)) {
			return Category.HELMET;
		}
		if (stack.is(ItemTags.CHEST_ARMOR)) {
			return Category.CHESTPLATE;
		}
		if (stack.is(ItemTags.LEG_ARMOR)) {
			return Category.LEGGINGS;
		}
		return stack.is(ItemTags.FOOT_ARMOR) ? Category.BOOTS : null;
	}

	public static Gem gemOf(ItemStack stack) {
		for (Gem gem : Gem.values()) {
			if (stack.is(gem.item())) {
				return gem;
			}
		}
		return null;
	}

	private static Gem byId(String id) {
		for (Gem gem : Gem.values()) {
			if (gem.id.equals(id)) {
				return gem;
			}
		}
		return null;
	}

	public static List<String> socketed(ItemStack stack) {
		return stack.getOrDefault(SOCKETED, List.of());
	}

	/** Why this gem cannot go into this item, or null if it can. */
	public static Component problem(ItemStack item, Gem gem) {
		List<String> gems = socketed(item);
		if (gems.contains(gem.id)) {
			return Component.translatable("gem.endrealms.already");
		}
		return gems.size() >= MAX_SOCKETS ? Component.translatable("gem.endrealms.full") : null;
	}

	public static void socket(ItemStack item, Gem gem) {
		List<String> gems = new ArrayList<>(socketed(item));
		gems.add(gem.id);
		item.set(SOCKETED, List.copyOf(gems));
		rebuild(item);
	}

	/** Takes the gem out of the item, which destroys it. */
	public static void clear(ItemStack item) {
		item.remove(SOCKETED);
		rebuild(item);
	}

	/** Melee hits only: the special effects of the gem in the attacker's weapon. */
	public static void afterDamage(LivingEntity target, DamageSource source, float damage) {
		if (damage <= 0.0F || !(source.getDirectEntity() instanceof Player player)) {
			return;
		}
		ItemStack weapon = player.getMainHandItem();
		Category category = categoryOf(weapon);
		if (category == null) {
			return;
		}
		for (String id : socketed(weapon)) {
			Gem gem = byId(id);
			if (gem == null) {
				continue;
			}
			for (Bonus bonus : gem.bonuses(category)) {
				if (Bonus.IGNITE.equals(bonus.special())) {
					target.igniteForSeconds((float) bonus.amount());
				} else if (Bonus.LIFESTEAL.equals(bonus.special())) {
					player.heal(damage * (float) bonus.amount());
				}
			}
		}
	}

	/** Recomputes the item's attributes and durability from its own defaults plus its gems. */
	public static void rebuild(ItemStack item) {
		Category category = categoryOf(item);
		if (category == null) {
			return;
		}
		ItemAttributeModifiers modifiers = item.getPrototype().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		int durability = 0;
		for (String id : socketed(item)) {
			Gem gem = byId(id);
			if (gem == null) {
				continue;
			}
			int index = 0;
			for (Bonus bonus : gem.bonuses(category)) {
				if (bonus.attribute() == null) {
					durability += Bonus.DURABILITY.equals(bonus.special()) ? (int) bonus.amount() : 0;
				} else {
					modifiers = modifiers.withModifierAdded(bonus.attribute(),
							new AttributeModifier(EndRealms.id("gem/" + gem.id + "/" + index++), bonus.amount(), bonus.operation()), category.group);
				}
			}
		}
		item.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
		Integer baseDurability = item.getPrototype().get(DataComponents.MAX_DAMAGE);
		if (baseDurability != null) {
			item.set(DataComponents.MAX_DAMAGE, baseDurability + durability);
			if (item.getDamageValue() >= baseDurability + durability) {
				item.setDamageValue(baseDurability + durability - 1);       // removing durability never breaks the item outright
			}
		}
	}

	/** Tooltip lines for an item with gems. */
	public static List<Component> socketLines(ItemStack item) {
		List<Component> lines = new ArrayList<>();
		List<String> gems = socketed(item);
		if (gems.isEmpty()) {
			return lines;
		}
		for (String id : gems) {
			Gem gem = byId(id);
			if (gem != null) {
				lines.add(Component.translatable("gem.endrealms.socketed", gem.displayName()).withStyle(ChatFormatting.AQUA));
			}
		}
		return lines;
	}

	/** Tooltip lines for a gem: what it gives on every kind of item. */
	public static List<Component> gemLines(Gem gem) {
		List<Component> lines = new ArrayList<>();
		for (Category category : Category.values()) {
			MutableComponent line = Component.empty().append(category.label()).append(": ").withStyle(ChatFormatting.GRAY);
			boolean first = true;
			for (Bonus bonus : gem.bonuses(category)) {
				if (!first) {
					line.append(", ");
				}
				line.append(Component.empty().append(bonus.describe()).withStyle(ChatFormatting.BLUE));
				first = false;
			}
			lines.add(line);
		}
		return lines;
	}
}
