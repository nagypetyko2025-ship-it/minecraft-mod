package hu.endrealms.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import hu.endrealms.EndRealms;
import hu.endrealms.entity.EndVillager;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Villager body stretched taller and thinner, with two small horns, glowing eyes and one robe texture per profession. */
public class EndVillagerRenderer extends MobRenderer<EndVillager, VillagerRenderState, VillagerModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(EndRealms.id("end_villager"), "main");
	private static final Identifier GLOW = EndRealms.id("textures/entity/end_villager/glow.png");
	private static final Identifier[] TEXTURES = new Identifier[EndVillager.Profession.values().length];

	static {
		for (EndVillager.Profession profession : EndVillager.Profession.values()) {
			TEXTURES[profession.ordinal()] = EndRealms.id("textures/entity/end_villager/" + profession.id() + ".png");
		}
	}

	public EndVillagerRenderer(EntityRendererProvider.Context context) {
		super(context, new VillagerModel(context.bakeLayer(LAYER)), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = VillagerModel.createBodyModel();
		PartDefinition head = mesh.getRoot().getChild("head");
		head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(28, 38).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(-2.5F, -10.0F, -1.0F, 0.0F, 0.0F, -0.35F));
		head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(28, 38).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(2.5F, -10.0F, -1.0F, 0.0F, 0.0F, 0.35F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public VillagerRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EndVillager entity, VillagerRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		((State) state).profession = entity.getProfession().ordinal();
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return TEXTURES[((State) state).profession];
	}

	@Override
	protected void scale(VillagerRenderState state, PoseStack poseStack) {
		poseStack.scale(0.92F, 1.08F, 0.92F);
	}

	public static class State extends VillagerRenderState {
		public int profession;
	}
}
