package hu.endrealms.client.entity;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Glimmerhoof: slim body high up on four long thin legs, a small head with two antennae ending in glowing orbs,
 * a row of crystal spines along the back and a short tail. Texture layout: 64x32.
 */
public class GlimmerhoofModel extends QuadrupedModel<GlimmerhoofRenderState> {
	private final ModelPart[] spines = new ModelPart[3];

	public GlimmerhoofModel(ModelPart root) {
		super(root);
		for (int i = 0; i < spines.length; i++) {
			spines[i] = root.getChild("body").getChild("spine" + i);
		}
	}

	/** The crystal spines would poke through a saddle. */
	@Override
	public void setupAnim(GlimmerhoofRenderState state) {
		super.setupAnim(state);
		for (ModelPart spine : spines) {
			spine.visible = !state.saddled;
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.0F, -6.0F, 8.0F, 6.0F, 12.0F),
				PartPose.offset(0.0F, 9.0F, 0.0F));
		for (int i = 0; i < 3; i++) { // crystal spines on the back
			body.addOrReplaceChild("spine" + i,
					CubeListBuilder.create().texOffs(48, 8).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
					PartPose.offsetAndRotation(0.0F, -3.0F, -3.0F + i * 3.5F, -0.3F, 0.0F, 0.0F));
		}
		body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(24, 20).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F),
				PartPose.offsetAndRotation(0.0F, -1.5F, 6.0F, -0.6F, 0.0F, 0.0F));

		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, -5.0F, -5.0F, 6.0F, 6.0F, 5.0F),
				PartPose.offset(0.0F, 7.0F, -6.0F));
		for (int side = -1; side <= 1; side += 2) { // antennae with glowing orbs
			PartDefinition antenna = head.addOrReplaceChild(side < 0 ? "right_antenna" : "left_antenna",
					CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F),
					PartPose.offsetAndRotation(side * 2.0F, -5.0F, -2.0F, -0.2F, 0.0F, side * 0.35F));
			antenna.addOrReplaceChild("orb",
					CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F),
					PartPose.offset(0.0F, -6.0F, 0.0F));
		}

		CubeListBuilder leg = CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5F, 12.0F, 4.5F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2.5F, 12.0F, 4.5F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5F, 12.0F, -4.5F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2.5F, 12.0F, -4.5F));
		return LayerDefinition.create(mesh, 64, 32);
	}
}
