package hu.endrealms.client.entity;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.Glimmerhoof;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class GlimmerhoofRenderer extends AgeableMobRenderer<Glimmerhoof, GlimmerhoofRenderState, GlimmerhoofModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(EndRealms.id("glimmerhoof"), "main");
	public static final ModelLayerLocation BABY_LAYER = new ModelLayerLocation(EndRealms.id("glimmerhoof_baby"), "main");
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/glimmerhoof/glimmerhoof.png");
	/** Eyes, antenna orbs and back spines, drawn full-bright like enderman eyes. */
	private static final Identifier SADDLE = EndRealms.id("textures/entity/glimmerhoof/glimmerhoof_saddle.png");
	private static final Identifier GLOW = EndRealms.id("textures/entity/glimmerhoof/glimmerhoof_glow.png");

	public GlimmerhoofRenderer(EntityRendererProvider.Context context) {
		super(context, new GlimmerhoofModel(context.bakeLayer(LAYER)), new GlimmerhoofModel(context.bakeLayer(BABY_LAYER)), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
		this.addLayer(new RenderLayer<>(this) {
			@Override
			public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, GlimmerhoofRenderState state, float yRot, float xRot) {
				if (state.saddled && !state.isInvisible) {
					renderColoredCutoutModel(getParentModel(), SADDLE, poseStack, collector, light, state, -1, 1);
				}
			}
		});
	}

	public static LayerDefinition createBabyLayer() {
		return GlimmerhoofModel.createBodyLayer().apply(MeshTransformer.scaling(0.5F));
	}

	@Override
	public GlimmerhoofRenderState createRenderState() {
		return new GlimmerhoofRenderState();
	}

	@Override
	public void extractRenderState(Glimmerhoof entity, GlimmerhoofRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.saddled = entity.isSaddled();
	}

	@Override
	public Identifier getTextureLocation(GlimmerhoofRenderState state) {
		return TEXTURE;
	}
}
