package hu.endrealms.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import hu.endrealms.EndRealms;
import hu.endrealms.entity.VoidSentinel;
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

/**
 * Void Sentinel: a humanoid knight in voidstone armour with horned helmet and shoulder plates, rendered 1.3x
 * larger than a player. Visor eyes and the chest crystal glow (eyes layer).
 */
public class VoidSentinelRenderer extends HumanoidMobRenderer<VoidSentinel, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(EndRealms.id("void_sentinel"), "main");
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/void_sentinel/void_sentinel.png");
	private static final Identifier GLOW = EndRealms.id("textures/entity/void_sentinel/void_sentinel_glow.png");
	private static final float SCALE = 1.3F;

	public VoidSentinelRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(LAYER)), 0.7F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
	}

	/** Vanilla humanoid (64x64 texture) plus helmet horns and shoulder plates in the free lower half of the texture. */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.getChild("head");
		head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(0, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(-3.5F, -7.0F, 0.0F, 0.0F, 0.0F, -0.45F));
		head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(4, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(3.5F, -7.0F, 0.0F, 0.0F, 0.0F, 0.45F));
		root.getChild("right_arm").addOrReplaceChild("right_pad",
				CubeListBuilder.create().texOffs(0, 40).addBox(-3.5F, -3.0F, -3.0F, 5.0F, 3.0F, 6.0F),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		root.getChild("left_arm").addOrReplaceChild("left_pad",
				CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-1.5F, -3.0F, -3.0F, 5.0F, 3.0F, 6.0F),
				PartPose.offset(0.0F, 0.0F, 0.0F));
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

	@Override
	protected void scale(HumanoidRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
