package hu.endrealms.client;

import hu.endrealms.client.entity.AshWraithRenderer;
import hu.endrealms.client.gem.GemTableScreen;
import hu.endrealms.gem.Gems;
import hu.endrealms.quest.Quests;
import hu.endrealms.registry.ModBlockEntities;
import hu.endrealms.registry.ModMenus;
import hu.endrealms.client.entity.AstralFishModels;
import hu.endrealms.client.entity.DeepLurkerRenderer;
import hu.endrealms.client.entity.AstralFishRenderer;
import hu.endrealms.client.entity.EndVillagerRenderer;
import hu.endrealms.client.entity.GlimmerhoofModel;
import hu.endrealms.client.entity.GlimmerhoofRenderer;
import hu.endrealms.client.entity.RiftLordRenderer;
import hu.endrealms.client.entity.ShardcrabRenderer;
import hu.endrealms.client.block.CarvedPumpkinRenderer;
import hu.endrealms.client.guide.QuestScreen;
import hu.endrealms.item.EndGuideItem;
import hu.endrealms.client.entity.VoidSentinelRenderer;
import hu.endrealms.registry.ModEntities;
import hu.endrealms.registry.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

public class EndRealmsClient implements ClientModInitializer {
	/** Vanilla water textures are greyscale and get tinted; we tint them purple instead of biome blue. */
	private static final int ASTRAL_WATER_COLOR = 0xFFA64BDB;

	@Override
	public void onInitializeClient() {
		// Answers the server's version question while logging in, and fetches a newer End Realms (see VersionCheck, AutoUpdate).
		hu.endrealms.client.update.ClientUpdate.init();
		hu.endrealms.client.sculk.SculkSky.init();
		hu.endrealms.client.sculk.WillMoths.init();
		// Passing out at Drunkenness IV: the whole screen goes black, fading in over half a second (see Blackout).
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(hu.endrealms.EndRealms.id("blackout"), (g, delta) -> {
			net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
			net.minecraft.world.effect.MobEffectInstance out = player == null ? null : player.getEffect(hu.endrealms.brew.Blackout.EFFECT);
			if (out != null) {
				int elapsed = hu.endrealms.brew.Blackout.TICKS + 10 - out.getDuration();
				int alpha = Math.max(0, Math.min(255, elapsed * 255 / 10));
				g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha << 24);
			}
		});
		FluidRenderingRegistry.register(ModFluids.ASTRAL_WATER, ModFluids.FLOWING_ASTRAL_WATER, new FluidModel.Unbaked(
				new Material(Identifier.withDefaultNamespace("block/water_still")),
				new Material(Identifier.withDefaultNamespace("block/water_flow")),
				new Material(Identifier.withDefaultNamespace("block/water_overlay")),
				state -> ASTRAL_WATER_COLOR));

		ModelLayerRegistry.registerModelLayer(GlimmerhoofRenderer.LAYER, GlimmerhoofModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(GlimmerhoofRenderer.BABY_LAYER, GlimmerhoofRenderer::createBabyLayer);
		EntityRendererRegistry.register(ModEntities.GLIMMERHOOF, GlimmerhoofRenderer::new);
		ModelLayerRegistry.registerModelLayer(hu.endrealms.client.entity.BoneStagRenderer.LAYER, hu.endrealms.client.entity.BoneStagModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(hu.endrealms.client.entity.BoneStagRenderer.BABY_LAYER, hu.endrealms.client.entity.BoneStagRenderer::createBabyLayer);
		EntityRendererRegistry.register(ModEntities.BONE_STAG, hu.endrealms.client.entity.BoneStagRenderer::new);

		ModelLayerRegistry.registerModelLayer(VoidSentinelRenderer.LAYER, VoidSentinelRenderer::createLayer);
		EntityRendererRegistry.register(ModEntities.VOID_SENTINEL, VoidSentinelRenderer::new);

		ModelLayerRegistry.registerModelLayer(RiftLordRenderer.LAYER, RiftLordRenderer::createLayer);
		EntityRendererRegistry.register(ModEntities.RIFT_LORD, RiftLordRenderer::new);
		EntityRendererRegistry.register(ModEntities.VOID_KING, hu.endrealms.client.entity.VoidKingRenderer::new);
		hu.endrealms.bag.Bags.hidden = () -> net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
		EntityRendererRegistry.register(ModEntities.SPECTRAL_WHELP, hu.endrealms.client.entity.SpectralWhelpRenderer::new);
		EndGuideItem.opener = () -> Minecraft.getInstance().gui.setScreen(new QuestScreen());
		ClientPlayNetworking.registerGlobalReceiver(Quests.State.TYPE, (payload, context) -> QuestScreen.accept(payload));
		BlockEntityRendererRegistry.register(ModBlockEntities.CARVED_PUMPKIN, CarvedPumpkinRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.CANVAS, hu.endrealms.client.block.CanvasRenderer::new);

		MenuScreens.register(ModMenus.GEM_TABLE, GemTableScreen::new);
		MenuScreens.register(ModMenus.FERMENTATION_STATION, hu.endrealms.client.brew.FermentationStationScreen::new);
		MenuScreens.register(ModMenus.AGING_BARREL, hu.endrealms.client.brew.AgingBarrelScreen::new);
		MenuScreens.register(ModMenus.END_CRAFTING_TABLE, hu.endrealms.client.craft.EndCraftingScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(hu.endrealms.craft.EndCrafting.Recipes.TYPE, (payload, context) -> hu.endrealms.client.craft.EndCraftingScreen.accept(payload));
		ModelLayerRegistry.registerModelLayer(hu.endrealms.client.entity.BoneDrakeRenderer.LAYER, hu.endrealms.client.entity.BoneDrakeRenderer.Model::createLayer);
		EntityRendererRegistry.register(ModEntities.BONE_DRAKE, hu.endrealms.client.entity.BoneDrakeRenderer::new);
		MenuScreens.register(ModMenus.PHASE_MAILBOX, hu.endrealms.client.mail.PhaseMailboxScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(hu.endrealms.mail.PhaseMail.State.TYPE, (payload, context) -> hu.endrealms.client.mail.PhaseMailboxScreen.accept(payload));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			lines.addAll(Gems.socketLines(stack));
			Gems.Gem gem = Gems.gemOf(stack);
			if (gem != null) {
				lines.addAll(Gems.gemLines(gem));
			}
		});

		ModelLayerRegistry.registerModelLayer(AshWraithRenderer.LAYER, AshWraithRenderer::createLayer);
		EntityRendererRegistry.register(ModEntities.ASH_WRAITH, AshWraithRenderer::new);

		ModelLayerRegistry.registerModelLayer(DeepLurkerRenderer.LAYER, DeepLurkerRenderer.Model::createLayer);
		EntityRendererRegistry.register(ModEntities.DEEP_LURKER, DeepLurkerRenderer::new);

		ModelLayerRegistry.registerModelLayer(ShardcrabRenderer.LAYER, ShardcrabRenderer.Model::createLayer);
		EntityRendererRegistry.register(ModEntities.SHARDCRAB, ShardcrabRenderer::new);

		ModelLayerRegistry.registerModelLayer(EndVillagerRenderer.LAYER, EndVillagerRenderer::createLayer);
		EntityRendererRegistry.register(ModEntities.END_VILLAGER, EndVillagerRenderer::new);

		ModelLayerRegistry.registerModelLayer(AstralFishModels.STARFIN, AstralFishModels.Starfin::createLayer);
		ModelLayerRegistry.registerModelLayer(AstralFishModels.VOID_EEL, AstralFishModels.VoidEel::createLayer);
		ModelLayerRegistry.registerModelLayer(AstralFishModels.GLOWGILL, AstralFishModels.Glowgill::createLayer);
		EntityRendererRegistry.register(ModEntities.STARFIN, ctx -> new AstralFishRenderer(ctx,
				new AstralFishModels.Starfin(ctx.bakeLayer(AstralFishModels.STARFIN)), "starfin"));
		EntityRendererRegistry.register(ModEntities.VOID_EEL, ctx -> new AstralFishRenderer(ctx,
				new AstralFishModels.VoidEel(ctx.bakeLayer(AstralFishModels.VOID_EEL)), "void_eel"));
		EntityRendererRegistry.register(ModEntities.GLOWGILL, ctx -> new AstralFishRenderer(ctx,
				new AstralFishModels.Glowgill(ctx.bakeLayer(AstralFishModels.GLOWGILL)), "glowgill"));
	}
}
