package hu.endrealms.client.entity;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.BoneDrake;
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

/** Bone Drake: a pale little dragon with a long neck and tail, horns, and membrane wings that fold along its back at rest. */
public class BoneDrakeRenderer extends MobRenderer<BoneDrake, BoneDrakeRenderer.State, BoneDrakeRenderer.Model> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(EndRealms.id("bone_drake"), "main");
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/bone_drake/bone_drake.png");
	private static final Identifier GLOW = EndRealms.id("textures/entity/bone_drake/bone_drake_glow.png");

	public BoneDrakeRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(LAYER)), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BoneDrake entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.flying = entity.isFlying();
		state.sitting = entity.isInSittingPose();
		state.flap = entity.flap + entity.flapSpeed * 0.45F * partialTick;
		state.flapSpeed = entity.flapSpeed;
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}

	public static class State extends LivingEntityRenderState {
		public boolean flying;
		public boolean sitting;
		public float flap;
		public float flapSpeed;
	}

	public static class Model extends EntityModel<State> {
		private final ModelPart body;
		private final ModelPart neck;
		private final ModelPart head;
		private final ModelPart tail;
		private final ModelPart tailTip;
		private final ModelPart rightWing;
		private final ModelPart leftWing;
		private final ModelPart[] legs;

		public Model(ModelPart root) {
			super(root);
			this.body = root.getChild("body");
			this.neck = body.getChild("neck");
			this.head = neck.getChild("head");
			this.tail = body.getChild("tail");
			this.tailTip = tail.getChild("tail_tip");
			this.rightWing = body.getChild("right_wing");
			this.leftWing = body.getChild("left_wing");
			this.legs = new ModelPart[] {root.getChild("right_front_leg"), root.getChild("left_front_leg"), root.getChild("right_hind_leg"), root.getChild("left_hind_leg")};
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -5.0F, 6.0F, 5.0F, 10.0F),
					PartPose.offset(0.0F, 16.0F, 0.0F));
			for (int i = 0; i < 3; i++) {     // ridge of bony spikes along the back
				body.addOrReplaceChild("spike" + i, CubeListBuilder.create().texOffs(54, 10).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
						PartPose.offsetAndRotation(0.0F, -3.0F, -3.0F + i * 3.0F, -0.4F, 0.0F, 0.0F));
			}
			PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, -1.5F, -5.0F, 3.0F, 3.0F, 5.0F),
					PartPose.offsetAndRotation(0.0F, -1.0F, -4.5F, -0.55F, 0.0F, 0.0F));
			PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0).addBox(-2.5F, -2.5F, -4.0F, 5.0F, 4.0F, 5.0F),
					PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, 0.55F, 0.0F, 0.0F));
			head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(32, 10).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 3.0F),
					PartPose.offset(0.0F, 0.0F, -4.0F));
			head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(44, 10).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F),
					PartPose.offsetAndRotation(-1.8F, -2.2F, -0.5F, 0.45F, -0.3F, 0.0F));
			head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(44, 10).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F),
					PartPose.offsetAndRotation(1.8F, -2.2F, -0.5F, 0.45F, 0.3F, 0.0F));
			PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 16).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 6.0F),
					PartPose.offsetAndRotation(0.0F, -1.0F, 5.0F, -0.25F, 0.0F, 0.0F));
			tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(34, 16).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F),
					PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 0.2F, 0.0F, 0.0F));
			PartDefinition right = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(8, 26).addBox(-9.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
					PartPose.offset(-3.0F, -2.5F, -2.0F));
			right.addOrReplaceChild("membrane", CubeListBuilder.create().texOffs(0, 36).addBox(-9.0F, 0.0F, 0.0F, 9.0F, 0.0F, 8.0F),
					PartPose.offset(0.0F, 0.0F, 0.5F));
			PartDefinition left = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(8, 26).mirror().addBox(0.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
					PartPose.offset(3.0F, -2.5F, -2.0F));
			left.addOrReplaceChild("membrane", CubeListBuilder.create().texOffs(0, 36).mirror().addBox(0.0F, 0.0F, 0.0F, 9.0F, 0.0F, 8.0F),
					PartPose.offset(0.0F, 0.0F, 0.5F));
			CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F);
			root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.0F, 18.0F, -3.5F));
			root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2.0F, 18.0F, -3.5F));
			root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.0F, 18.0F, 3.5F));
			root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2.0F, 18.0F, 3.5F));
			return LayerDefinition.create(mesh, 64, 64);
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			float age = state.ageInTicks;
			neck.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
			head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
			head.xRot += state.xRot * Mth.DEG_TO_RAD;
			neck.xRot += Mth.sin(age * 0.08F) * 0.04F;
			tail.yRot = Mth.sin(age * 0.07F) * 0.25F;
			tailTip.yRot = Mth.sin(age * 0.07F - 0.8F) * 0.3F;

			if (state.flying) {
				float beat = Mth.sin(state.flap) * (0.35F + 0.5F * state.flapSpeed);
				rightWing.zRot = 0.1F + beat;
				leftWing.zRot = -0.1F - beat;
				rightWing.yRot = 0.0F;
				leftWing.yRot = 0.0F;
				for (ModelPart leg : legs) {
					leg.xRot = 0.9F;                     // tucked back
				}
				tail.xRot += 0.2F;
				return;
			}
			// at rest the wings fold back along the body, the membrane hanging down over the flanks
			rightWing.yRot = 1.35F;
			leftWing.yRot = -1.35F;
			rightWing.zRot = -0.25F + Mth.sin(age * 0.05F) * 0.03F;
			leftWing.zRot = 0.25F - Mth.sin(age * 0.05F) * 0.03F;
			if (state.sitting) {
				body.y += 3.0F;
				for (ModelPart leg : legs) {
					leg.y += 3.5F;
				}
				legs[0].xRot = -1.2F;
				legs[1].xRot = -1.2F;
				legs[2].xRot = -1.5F;
				legs[3].xRot = -1.5F;
				tail.xRot += 0.25F;
				return;
			}
			float swing = state.walkAnimationPos * 0.9F;
			float amount = state.walkAnimationSpeed;
			legs[0].xRot = Mth.cos(swing) * amount;
			legs[1].xRot = Mth.cos(swing + Mth.PI) * amount;
			legs[2].xRot = Mth.cos(swing + Mth.PI) * amount;
			legs[3].xRot = Mth.cos(swing) * amount;
		}
	}
}
