package hu.endrealms.client.entity;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.DeepLurker;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Deep Lurker: a heavy-jawed eel with a glowing lure over its head and a glowing line down its back. */
public class DeepLurkerRenderer extends MobRenderer<DeepLurker, LivingEntityRenderState, DeepLurkerRenderer.Model> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(EndRealms.id("deep_lurker"), "main");
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/deep_lurker/deep_lurker.png");
	private static final Identifier GLOW = EndRealms.id("textures/entity/deep_lurker/deep_lurker_glow.png");

	public static class Model extends EntityModel<LivingEntityRenderState> {
		private final ModelPart head;
		private final ModelPart jaw;
		private final ModelPart front;
		private final ModelPart rear;
		private final ModelPart tail;

		public Model(ModelPart root) {
			super(root, RenderTypes::entityCutoutCull);
			this.head = root.getChild("head");
			this.jaw = head.getChild("jaw");
			this.front = head.getChild("front");
			this.rear = front.getChild("rear");
			this.tail = rear.getChild("tail");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.0F, -7.0F, 7.0F, 6.0F, 7.0F),
					PartPose.offset(0.0F, 19.0F, -5.0F));
			head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(30, 0).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 2.0F, 5.0F), PartPose.offset(0.0F, 3.0F, -1.0F));
			PartDefinition lure = head.addOrReplaceChild("lure", CubeListBuilder.create().texOffs(54, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
					PartPose.offsetAndRotation(0.0F, -3.0F, -3.0F, -0.7F, 0.0F, 0.0F));
			lure.addOrReplaceChild("bulb", CubeListBuilder.create().texOffs(54, 8).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, -5.0F, 0.0F));
			PartDefinition front = head.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 14).addBox(-3.0F, -2.5F, 0.0F, 6.0F, 5.0F, 8.0F), PartPose.ZERO);
			PartDefinition rear = front.addOrReplaceChild("rear", CubeListBuilder.create().texOffs(0, 28).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 8.0F));
			PartDefinition tail = rear.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(26, 28).addBox(-1.0F, -1.5F, 0.0F, 2.0F, 3.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 8.0F));
			tail.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(44, 14).addBox(0.0F, -4.0F, 0.0F, 0.0F, 8.0F, 7.0F), PartPose.offset(0.0F, 0.0F, 6.0F));
			return LayerDefinition.create(mesh, 64, 64);
		}

		@Override
		public void setupAnim(LivingEntityRenderState state) {
			super.setupAnim(state);
			float t = state.ageInTicks * 0.35F;
			head.xRot = state.xRot * Mth.DEG_TO_RAD;
			jaw.xRot = 0.15F + 0.12F * Mth.sin(t * 0.7F);
			front.yRot = 0.14F * Mth.sin(t);
			rear.yRot = 0.26F * Mth.sin(t - 0.9F);
			tail.yRot = 0.4F * Mth.sin(t - 1.8F);
		}
	}

	public DeepLurkerRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(LAYER)), 0.4F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
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
