package hu.endrealms.client.entity;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.BoneStag;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** The Bone Stag, with its eyes (and a few sculk specks) glowing in the dark like an enderman's. */
public class BoneStagRenderer extends AgeableMobRenderer<BoneStag, LivingEntityRenderState, BoneStagModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(EndRealms.id("bone_stag"), "main");
	public static final ModelLayerLocation BABY_LAYER = new ModelLayerLocation(EndRealms.id("bone_stag_baby"), "main");
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/bone_stag/bone_stag.png");
	private static final Identifier GLOW = EndRealms.id("textures/entity/bone_stag/bone_stag_glow.png");

	public BoneStagRenderer(EntityRendererProvider.Context context) {
		super(context, new BoneStagModel(context.bakeLayer(LAYER)), new BoneStagModel(context.bakeLayer(BABY_LAYER)), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
	}

	public static LayerDefinition createBabyLayer() {
		return BoneStagModel.createBodyLayer().apply(MeshTransformer.scaling(0.5F));
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}
