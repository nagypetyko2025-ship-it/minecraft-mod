package hu.endrealms.client.entity;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Bone Stag: four long thin bone legs; instead of a body a spine with an open cage of ribs you can look through,
 * a solid shoulder and pelvis, sculk hanging in strands from the ribs and clinging to the spine; a long neck, a
 * bare skull with glowing eyes, and branching antlers. Texture 64x64: bone in the top-left, sculk on the right,
 * the skull below.
 */
public class BoneStagModel extends QuadrupedModel<LivingEntityRenderState> {
	public BoneStagModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 16.0F)          // spine
				.texOffs(20, 28).addBox(-3.5F, -1.0F, -8.0F, 7.0F, 5.0F, 3.0F)         // shoulders
				.texOffs(20, 20).addBox(-3.0F, -1.0F, 5.0F, 6.0F, 4.0F, 4.0F),          // pelvis
				PartPose.offset(0.0F, 8.0F, 0.0F));
		for (int i = 0; i < 4; i++) {                                                   // the open rib cage
			float z = -4.5F + i * 2.5F;
			float drop = i == 1 || i == 2 ? 8.0F : 7.0F;
			CubeListBuilder rib = CubeListBuilder.create()
					.texOffs(12, 20).addBox(-4.5F, 0.0F, z, 9.0F, 1.0F, 1.0F)              // over the top
					.texOffs(12, 20).addBox(3.5F, 1.0F, z, 1.0F, drop - 1.0F, 1.0F)        // down the left side
					.texOffs(12, 20).addBox(-4.5F, 1.0F, z, 1.0F, drop - 1.0F, 1.0F);      // down the right side
			if (i != 3) {
				rib.texOffs(12, 20).addBox(-3.5F, drop - 1.0F, z, 7.0F, 1.0F, 1.0F);       // closing underneath, but for the last
			}
			body.addOrReplaceChild("rib" + i, rib, PartPose.ZERO);
		}
		// sculk hanging from the ribs in strands, and a crust of it on the spine
		body.addOrReplaceChild("sculk", CubeListBuilder.create()
				.texOffs(40, 0).addBox(2.5F, 7.0F, -4.5F, 1.0F, 4.0F, 1.0F)
				.texOffs(40, 0).addBox(-2.0F, 8.0F, -2.0F, 1.0F, 3.0F, 1.0F)
				.texOffs(40, 0).addBox(0.5F, 8.0F, 0.5F, 1.0F, 5.0F, 1.0F)
				.texOffs(40, 0).addBox(-4.0F, 6.0F, 2.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(40, 8).addBox(-1.5F, -2.0F, -5.0F, 3.0F, 1.0F, 4.0F)
				.texOffs(40, 8).addBox(-1.5F, -2.0F, 3.0F, 3.0F, 1.0F, 3.0F),
				PartPose.ZERO);

		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(24, 40).addBox(-1.0F, -6.0F, -2.0F, 2.0F, 7.0F, 2.0F)          // neck
				.texOffs(0, 40).addBox(-2.5F, -10.0F, -7.0F, 5.0F, 4.0F, 7.0F),          // skull, snout forward
				PartPose.offsetAndRotation(0.0F, 7.0F, -8.0F, 0.25F, 0.0F, 0.0F));
		for (int side = -1; side <= 1; side += 2) {                                     // branching antlers
			PartDefinition beam = head.addOrReplaceChild(side < 0 ? "right_antler" : "left_antler",
					CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F),
					PartPose.offsetAndRotation(side * 1.8F, -10.0F, -1.5F, -0.35F, 0.0F, side * 0.45F));
			beam.addOrReplaceChild("tine0", CubeListBuilder.create().texOffs(36, 40).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
					PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.9F, 0.0F, 0.0F));
			beam.addOrReplaceChild("tine1", CubeListBuilder.create().texOffs(36, 40).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
					PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, side * 0.8F));
			beam.addOrReplaceChild("tine2", CubeListBuilder.create().texOffs(36, 40).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
					PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.6F, 0.0F, 0.0F));
		}

		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 15.0F, 2.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5F, 9.0F, 6.5F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2.5F, 9.0F, 6.5F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5F, 9.0F, -6.5F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2.5F, 9.0F, -6.5F));
		return LayerDefinition.create(mesh, 64, 64);
	}
}
