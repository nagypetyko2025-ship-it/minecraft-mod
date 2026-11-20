package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import hu.endrealms.gem.GemTableMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static final MenuType<GemTableMenu> GEM_TABLE = Registry.register(BuiltInRegistries.MENU, EndRealms.id("gem_table"),
			new MenuType<>(GemTableMenu::new, FeatureFlags.VANILLA_SET));

	public static final MenuType<hu.endrealms.mail.PhaseMailboxMenu> PHASE_MAILBOX = Registry.register(BuiltInRegistries.MENU, EndRealms.id("phase_mailbox"),
			new MenuType<>(hu.endrealms.mail.PhaseMailboxMenu::new, FeatureFlags.VANILLA_SET));

	public static final MenuType<hu.endrealms.craft.EndCraftingMenu> END_CRAFTING_TABLE = Registry.register(BuiltInRegistries.MENU, EndRealms.id("end_crafting_table"),
			new MenuType<>(hu.endrealms.craft.EndCraftingMenu::new, FeatureFlags.VANILLA_SET));

	public static final MenuType<hu.endrealms.brew.FermentationStationMenu> FERMENTATION_STATION = Registry.register(BuiltInRegistries.MENU, EndRealms.id("fermentation_station"),
			new MenuType<>(hu.endrealms.brew.FermentationStationMenu::new, FeatureFlags.VANILLA_SET));

	public static final MenuType<hu.endrealms.brew.AgingBarrelMenu> AGING_BARREL = Registry.register(BuiltInRegistries.MENU, EndRealms.id("aging_barrel"),
			new MenuType<>(hu.endrealms.brew.AgingBarrelMenu::new, FeatureFlags.VANILLA_SET));

	private ModMenus() {
	}

	public static void init() {
	}
}
