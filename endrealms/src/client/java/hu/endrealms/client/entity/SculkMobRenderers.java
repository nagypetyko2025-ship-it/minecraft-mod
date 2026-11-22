package hu.endrealms.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import hu.endrealms.EndRealms;
import hu.endrealms.entity.Bellringer;
import hu.endrealms.entity.Dangler;
import hu.endrealms.entity.PillarCrawler;
import hu.endrealms.entity.Shade;
import hu.endrealms.entity.SpireNester;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Renderers of the creatures of the Sculk dimension's stranger lands. */
public final class SculkMobRenderers {
	public static final ModelLayerLocation DANGLER = new ModelLayerLocation(EndRealms.id("dangler"), "main");
	public static final ModelLayerLocation BELLRINGER = new ModelLayerLocation(EndRealms.id("bellringer"), "main");
	public static final ModelLayerLocation SHADE = new ModelLayerLocation(EndRealms.id("shade"), "main");

	private SculkMobRenderers() {
	}

	private static Identifier texture(String name) {
		return EndRealms.id("textures/entity/" + name + "/" + name + ".png");
	}

	private static Identifier glow(String name) {
		return EndRealms.id("textures/entity/" + name + "/" + name + "_glow.png");
	}

	/** Skeleton-thin body: arms and legs like sticks. */
	public static LayerDefinition thinLayer() {
		return SkeletonModel.createBodyLayer();
	}

	/** A plain human shape. */
	public static LayerDefinition humanLayer() {
		return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64);
	}

	// ---------------------------------------------------------------- Dangler: stretched thin and tall, upside down while it hangs

	public static class DanglerState extends HumanoidRenderState {
		boolean hanging;
	}

	public static class DanglerRenderer extends HumanoidMobRenderer<Dangler, DanglerState, HumanoidModel<DanglerState>> {
		public DanglerRenderer(EntityRendererProvider.Context context) {
			super(context, new HumanoidModel<>(context.bakeLayer(DANGLER)), 0.4F);
			addLayer(new EyesLayer<>(this) {
				@Override
				public RenderType renderType() {
					return RenderTypes.eyes(glow("dangler"));
				}
			});
		}

		@Override
		public DanglerState createRenderState() {
			return new DanglerState();
		}

		@Override
		public void extractRenderState(Dangler entity, DanglerState state, float partialTick) {
			super.extractRenderState(entity, state, partialTick);
			state.hanging = entity.isHanging();
			state.isUpsideDown = state.hanging;
			if (state.hanging) {
				state.walkAnimationSpeed = 0.0F;
			}
		}

		@Override
		protected void scale(DanglerState state, PoseStack poseStack) {
			poseStack.scale(0.95F, 1.32F, 0.95F);
		}

		@Override
		public Identifier getTextureLocation(DanglerState state) {
			return texture("dangler");
		}
	}

	// ---------------------------------------------------------------- Shade: a see-through dark human shape

	public static class ShadeRenderer extends HumanoidMobRenderer<Shade, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
		public ShadeRenderer(EntityRendererProvider.Context context) {
			super(context, new HumanoidModel<>(context.bakeLayer(SHADE)), 0.0F);
		}

		@Override
		public HumanoidRenderState createRenderState() {
			return new HumanoidRenderState();
		}

		@Override
		protected RenderType getRenderType(HumanoidRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
			return RenderTypes.entityTranslucent(getTextureLocation(state));
		}

		@Override
		public Identifier getTextureLocation(HumanoidRenderState state) {
			return texture("shade");
		}
	}

	// ---------------------------------------------------------------- Bellringer: a tall bone figure

	public static class BellringerRenderer extends HumanoidMobRenderer<Bellringer, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
		public BellringerRenderer(EntityRendererProvider.Context context) {
			super(context, new HumanoidModel<>(context.bakeLayer(BELLRINGER)), 0.5F);
			addLayer(new EyesLayer<>(this) {
				@Override
				public RenderType renderType() {
					return RenderTypes.eyes(glow("bellringer"));
				}
			});
		}

		@Override
		public HumanoidRenderState createRenderState() {
			return new HumanoidRenderState();
		}

		@Override
		protected void scale(HumanoidRenderState state, PoseStack poseStack) {
			poseStack.scale(1.25F, 1.25F, 1.25F);
		}

		@Override
		public Identifier getTextureLocation(HumanoidRenderState state) {
			return texture("bellringer");
		}
	}

	// ---------------------------------------------------------------- Pillar Crawler: a spider's frame, pale and stretched long

	public static class PillarCrawlerRenderer extends SpiderRenderer<PillarCrawler> {
		public PillarCrawlerRenderer(EntityRendererProvider.Context context) {
			super(context, ModelLayers.SPIDER);
		}

		@Override
		protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
			poseStack.scale(1.15F, 1.7F, 1.35F);
		}

		@Override
		public Identifier getTextureLocation(LivingEntityRenderState state) {
			return texture("pillar_crawler");
		}
	}

	// ---------------------------------------------------------------- Spire Nester: a big, eyeless, slow-winged flier

	public static class SpireNesterRenderer extends MobRenderer<SpireNester, PhantomRenderState, PhantomModel> {
		public SpireNesterRenderer(EntityRendererProvider.Context context) {
			super(context, new PhantomModel(context.bakeLayer(ModelLayers.PHANTOM)), 0.6F);
		}

		@Override
		public PhantomRenderState createRenderState() {
			return new PhantomRenderState();
		}

		@Override
		public void extractRenderState(SpireNester entity, PhantomRenderState state, float partialTick) {
			super.extractRenderState(entity, state, partialTick);
			state.flapTime = (entity.tickCount + partialTick) * 0.55F + entity.getId() * 7;
			state.size = 0;
		}

		@Override
		protected void scale(PhantomRenderState state, PoseStack poseStack) {
			poseStack.scale(1.6F, 1.6F, 1.6F);
		}

		@Override
		public Identifier getTextureLocation(PhantomRenderState state) {
			return texture("spire_nester");
		}
	}
}
