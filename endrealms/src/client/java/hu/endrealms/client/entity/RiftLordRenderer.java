package hu.endrealms.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import hu.endrealms.EndRealms;
import hu.endrealms.entity.RiftLord;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
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
import net.minecraft.util.Mth;

/** Rift Lord: a hooded giant in a long robe, with a crown of crystal spikes and four shards circling it. */
public class RiftLordRenderer extends HumanoidMobRenderer<RiftLord, HumanoidRenderState, RiftLordRenderer.Model> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(EndRealms.id("rift_lord"), "main");
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/rift_lord/rift_lord.png");
	private static final Identifier GLOW = EndRealms.id("textures/entity/rift_lord/rift_lord_glow.png");
	private static final float SCALE = 1.7F;
	private static final int SHARDS = 4;

	public static class Model extends HumanoidModel<HumanoidRenderState> {
		private final ModelPart[] shards = new ModelPart[SHARDS];

		public Model(ModelPart root) {
			super(root);
			for (int i = 0; i < SHARDS; i++) {
				shards[i] = root.getChild("shard_" + i);
			}
		}

		@Override
		public void setupAnim(HumanoidRenderState state) {
			super.setupAnim(state);
			for (int i = 0; i < SHARDS; i++) {
				float angle = state.ageInTicks * 0.05F + i * Mth.HALF_PI;
				shards[i].x = Mth.cos(angle) * 15.0F;
				shards[i].z = Mth.sin(angle) * 15.0F;
				shards[i].y = 4.0F + Mth.sin(state.ageInTicks * 0.08F + i) * 3.0F;
				shards[i].yRot = -angle;
			}
		}
	}

	public RiftLordRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(LAYER)), 1.0F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
	}

	/** Vanilla humanoid (64x64) plus crown, robe and orbiting shards in the free lower half of the texture. */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.getChild("head");
		float[][] spikes = {{-3.5F, -0.35F}, {-1.75F, -0.15F}, {0.0F, 0.0F}, {1.75F, 0.15F}, {3.5F, 0.35F}};
		for (int i = 0; i < spikes.length; i++) {
			head.addOrReplaceChild("spike_" + i, CubeListBuilder.create().texOffs(0, 32).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
					PartPose.offsetAndRotation(spikes[i][0], -7.5F, -3.5F, 0.0F, 0.0F, spikes[i][1]));
		}
		root.getChild("body").addOrReplaceChild("robe", CubeListBuilder.create().texOffs(24, 34).addBox(-5.0F, 11.0F, -3.0F, 10.0F, 10.0F, 6.0F),
				PartPose.ZERO);
		for (int i = 0; i < SHARDS; i++) {
			root.addOrReplaceChild("shard_" + i, CubeListBuilder.create().texOffs(0, 40).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.ZERO);
		}
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
