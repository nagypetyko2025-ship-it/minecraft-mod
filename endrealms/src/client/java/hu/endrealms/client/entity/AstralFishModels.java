package hu.endrealms.client.entity;

import hu.endrealms.EndRealms;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/** Models of the three Astral Basin fish. All textures are 32x32. */
public final class AstralFishModels {
	public static final ModelLayerLocation STARFIN = new ModelLayerLocation(EndRealms.id("starfin"), "main");
	public static final ModelLayerLocation VOID_EEL = new ModelLayerLocation(EndRealms.id("void_eel"), "main");
	public static final ModelLayerLocation GLOWGILL = new ModelLayerLocation(EndRealms.id("glowgill"), "main");

	private AstralFishModels() {
	}

	/** Starfin: slim violet fish with a star-shaped glowing dorsal fin and fan side fins. */
	public static class Starfin extends EntityModel<LivingEntityRenderState> {
		private final ModelPart tail;

		public Starfin(ModelPart root) {
			super(root, RenderTypes::entityCutoutCull); // culled: the zero-thickness fins must not z-fight
			this.tail = root.getChild("tail");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -4.0F, 2.0F, 4.0F, 8.0F),
					PartPose.offset(0.0F, 22.0F, 0.0F));
			root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 0).addBox(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 5.0F),
					PartPose.offset(0.0F, 22.0F, 4.0F));
			root.addOrReplaceChild("top_fin", CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, -3.0F, -2.0F, 0.0F, 3.0F, 5.0F),
					PartPose.offset(0.0F, 20.0F, 0.0F));
			root.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(10, 12).addBox(-3.0F, 0.0F, -1.0F, 3.0F, 0.0F, 3.0F),
					PartPose.offsetAndRotation(-1.0F, 23.0F, -1.0F, 0.0F, 0.0F, -0.6F));
			root.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(10, 15).addBox(0.0F, 0.0F, -1.0F, 3.0F, 0.0F, 3.0F),
					PartPose.offsetAndRotation(1.0F, 23.0F, -1.0F, 0.0F, 0.0F, 0.6F));
			return LayerDefinition.create(mesh, 32, 32);
		}

		@Override
		public void setupAnim(LivingEntityRenderState state) {
			super.setupAnim(state);
			this.tail.yRot = -0.45F * Mth.sin(0.6F * state.ageInTicks);
		}
	}

	/** Void Eel: long dark three-segment eel with a glowing line along its back; the body undulates. */
	public static class VoidEel extends EntityModel<LivingEntityRenderState> {
		private final ModelPart body1;
		private final ModelPart body2;
		private final ModelPart tail;

		public VoidEel(ModelPart root) {
			super(root, RenderTypes::entityCutoutCull); // culled: the zero-thickness fins must not z-fight
			this.body1 = root.getChild("body1");
			this.body2 = this.body1.getChild("body2");
			this.tail = this.body2.getChild("tail");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.5F, -3.0F, 2.0F, 3.0F, 3.0F),
					PartPose.offset(0.0F, 22.5F, -4.0F));
			PartDefinition body1 = root.addOrReplaceChild("body1",
					CubeListBuilder.create().texOffs(0, 6).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F)
							.texOffs(14, 10).addBox(0.0F, -2.0F, 0.0F, 0.0F, 1.0F, 5.0F),
					PartPose.offset(0.0F, 22.5F, -4.0F));
			PartDefinition body2 = body1.addOrReplaceChild("body2",
					CubeListBuilder.create().texOffs(0, 13).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F),
					PartPose.offset(0.0F, 0.0F, 5.0F));
			body2.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(14, 0).addBox(0.0F, -2.5F, 0.0F, 0.0F, 5.0F, 5.0F),
					PartPose.offset(0.0F, 0.0F, 5.0F));
			return LayerDefinition.create(mesh, 32, 32);
		}

		@Override
		public void setupAnim(LivingEntityRenderState state) {
			super.setupAnim(state);
			float t = 0.4F * state.ageInTicks;
			this.body1.yRot = 0.25F * Mth.sin(t);
			this.body2.yRot = 0.35F * Mth.sin(t - 0.8F);
			this.tail.yRot = 0.45F * Mth.sin(t - 1.6F);
		}
	}

	/** Glowgill: round pale-lilac fish with glowing flapping gill plates and a glowing angler lure. */
	public static class Glowgill extends EntityModel<LivingEntityRenderState> {
		private final ModelPart tail;
		private final ModelPart rightGill;
		private final ModelPart leftGill;

		public Glowgill(ModelPart root) {
			super(root, RenderTypes::entityCutoutCull); // culled: the zero-thickness fins must not z-fight
			this.tail = root.getChild("tail");
			this.rightGill = root.getChild("right_gill");
			this.leftGill = root.getChild("left_gill");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F),
					PartPose.offset(0.0F, 24.0F, 0.0F));
			root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 0).addBox(0.0F, -1.5F, 0.0F, 0.0F, 3.0F, 3.0F),
					PartPose.offset(0.0F, 21.5F, 2.5F));
			root.addOrReplaceChild("right_gill", CubeListBuilder.create().texOffs(0, 10).addBox(0.0F, -1.5F, 0.0F, 0.0F, 3.0F, 2.0F),
					PartPose.offset(-2.5F, 21.5F, -1.0F));
			root.addOrReplaceChild("left_gill", CubeListBuilder.create().texOffs(4, 10).addBox(0.0F, -1.5F, 0.0F, 0.0F, 3.0F, 2.0F),
					PartPose.offset(2.5F, 21.5F, -1.0F));
			PartDefinition stalk = root.addOrReplaceChild("lure_stalk",
					CubeListBuilder.create().texOffs(8, 10).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
					PartPose.offsetAndRotation(0.0F, 19.0F, -1.5F, -0.5F, 0.0F, 0.0F));
			stalk.addOrReplaceChild("lure", CubeListBuilder.create().texOffs(12, 10).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F),
					PartPose.offset(0.0F, -3.0F, 0.0F));
			return LayerDefinition.create(mesh, 32, 32);
		}

		@Override
		public void setupAnim(LivingEntityRenderState state) {
			super.setupAnim(state);
			float t = state.ageInTicks;
			this.tail.yRot = -0.4F * Mth.sin(0.5F * t);
			this.rightGill.yRot = -0.4F - 0.35F * Mth.sin(0.3F * t);
			this.leftGill.yRot = 0.4F + 0.35F * Mth.sin(0.3F * t);
		}
	}
}
