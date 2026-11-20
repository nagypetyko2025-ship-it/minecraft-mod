package hu.endrealms;

import hu.endrealms.command.AstraCommand;
import hu.endrealms.registry.ModBlocks;
import hu.endrealms.registry.ModCreativeTab;
import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModFluids;
import hu.endrealms.registry.ModItems;
import hu.endrealms.registry.ModStructures;
import hu.endrealms.registry.ModWorldgen;
import hu.endrealms.entity.AshWraithSpawner;
import hu.endrealms.entity.DeepLurkerSpawner;
import hu.endrealms.gem.Gems;
import hu.endrealms.block.CarvedPumpkinBlock;
import hu.endrealms.quest.Quests;
import hu.endrealms.registry.ModBlockEntities;
import hu.endrealms.registry.ModMenus;
import hu.endrealms.registry.ModRecipeBook;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndRealms implements ModInitializer {
	public static final String MOD_ID = "endrealms";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.init();
		hu.endrealms.net.VersionCheck.init();
		hu.endrealms.brew.Brewing.init();
		hu.endrealms.brew.Blackout.init();
		ModItems.init();
		ModBlocks.init();
		ModFluids.init();
		ModBlockEntities.init();
		ModRecipeBook.init();
		CarvedPumpkinBlock.register();
		hu.endrealms.entity.BoneDrake.registerFollowing();
		hu.endrealms.block.CanvasBlock.register();
		Quests.init();
		hu.endrealms.mail.PhaseMail.init();
		ModMenus.init();
		hu.endrealms.craft.EndCrafting.init();
		Gems.socketed(net.minecraft.world.item.ItemStack.EMPTY);   // registers the socket component during startup
		ModCreativeTab.init();
		ModWorldgen.init();
		ModStructures.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> AstraCommand.register(dispatcher));
		// Every End Realms recipe goes straight into the recipe book; recipes added in later versions follow on the next join.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handler.player.awardRecipes(
				server.getRecipeManager().getLearnableRecipes().stream()
						.filter(recipe -> recipe.id().identifier().getNamespace().equals(MOD_ID)).toList()));
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> hu.endrealms.command.TeleportRequests.register(dispatcher));
		hu.endrealms.command.TeleportRequests.init();
		ServerTickEvents.END_SERVER_TICK.register(AshWraithSpawner::tick);
		ServerTickEvents.END_SERVER_TICK.register(DeepLurkerSpawner::tick);
		ServerTickEvents.END_SERVER_TICK.register(hu.endrealms.entity.SpectralWhelpSpawner::tick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> Gems.afterDamage(entity, source, damage));
		LOGGER.info("End Realms loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
