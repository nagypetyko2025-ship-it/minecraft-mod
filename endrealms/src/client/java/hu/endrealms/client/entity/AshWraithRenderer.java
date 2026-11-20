package hu.endrealms.client.entity;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.AshWraith;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Ash Wraith: a gaunt charred figure with very long arms and two back-swept horns; its eyes and cracks glow violet. */
public class AshWraithRenderer extends HumanoidMobRenderer<AshWraith, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(EndRealms.id("ash_wraith"), "main");
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/ash_wraith/ash_wraith.png");
	private static final Identifier GLOW = EndRealms.id("textures/entity/ash_wraith/ash_wraith_glow.png");

	public AshWraithRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(LAYER)), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 12.0F, 3.0F), PartPose.ZERO);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -1.0F, 2.0F, 18.0F, 2.0F), PartPose.offset(-3.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(0.0F, -2.0F, -1.0F, 2.0F, 18.0F, 2.0F), PartPose.offset(3.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-1.6F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(1.6F, 12.0F, 0.0F));
		PartDefinition head = root.getChild("head");
		head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F),
				PartPose.offsetAndRotation(-3.0F, -7.0F, 1.0F, -0.6F, 0.0F, -0.3F));
		head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F),
				PartPose.offsetAndRotation(3.0F, -7.0F, 1.0F, -0.6F, 0.0F, 0.3F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}
}
