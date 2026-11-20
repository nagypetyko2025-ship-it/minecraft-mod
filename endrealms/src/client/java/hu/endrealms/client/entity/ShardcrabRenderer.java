package hu.endrealms.client.entity;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.Shardcrab;
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

/** Shardcrab: flat shell crowned with crystals. Disguised it sits on the ground with claws, legs and eyes hidden. */
public class ShardcrabRenderer extends MobRenderer<Shardcrab, ShardcrabRenderer.State, ShardcrabRenderer.Model> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(EndRealms.id("shardcrab"), "main");
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/shardcrab/shardcrab.png");
	private static final Identifier GLOW = EndRealms.id("textures/entity/shardcrab/shardcrab_glow.png");

	public ShardcrabRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(LAYER)), 0.6F);
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
	public void extractRenderState(Shardcrab entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.emerge = entity.getEmerge(partialTick);
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}

	public static class State extends LivingEntityRenderState {
		public float emerge;
	}

	public static class Model extends EntityModel<State> {
		private static final float RISE = 5.0F;
		private final ModelPart body;
		private final ModelPart[] limbs;
		private final ModelPart[] rightLegs;
		private final ModelPart[] leftLegs;

		public Model(ModelPart root) {
			super(root);
			this.body = root.getChild("body");
			this.rightLegs = new ModelPart[] {root.getChild("right_leg0"), root.getChild("right_leg1"), root.getChild("right_leg2")};
			this.leftLegs = new ModelPart[] {root.getChild("left_leg0"), root.getChild("left_leg1"), root.getChild("left_leg2")};
			this.limbs = new ModelPart[] {this.body.getChild("right_claw"), this.body.getChild("left_claw"),
					this.body.getChild("right_eye"), this.body.getChild("left_eye")};
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			PartDefinition body = root.addOrReplaceChild("body",
					CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -4.0F, -5.0F, 12.0F, 4.0F, 10.0F),
					PartPose.offset(0.0F, 24.0F, 0.0F));
			spike(body, "spike_main", 0, 16, 3, 10, 3, -0.5F, -3.5F, -0.5F, 0.08F, -0.1F);
			spike(body, "spike_a", 14, 16, 2, 8, 2, -3.5F, -3.5F, 1.5F, 0.3F, 0.35F);
			spike(body, "spike_b", 24, 16, 2, 6, 2, 3.5F, -3.5F, -2.0F, -0.3F, -0.4F);
			spike(body, "spike_c", 34, 16, 3, 7, 3, 2.5F, -3.5F, 2.5F, 0.4F, -0.3F);
			spike(body, "spike_d", 24, 16, 2, 6, 2, -3.0F, -3.5F, -2.5F, -0.35F, 0.3F);

			body.addOrReplaceChild("right_claw", CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, -2.0F, -4.0F, 5.0F, 4.0F, 4.0F),
					PartPose.offsetAndRotation(-5.0F, -1.5F, -5.0F, 0.0F, 0.25F, 0.0F));
			body.addOrReplaceChild("left_claw", CubeListBuilder.create().texOffs(20, 32).addBox(-2.5F, -2.0F, -4.0F, 5.0F, 4.0F, 4.0F),
					PartPose.offsetAndRotation(5.0F, -1.5F, -5.0F, 0.0F, -0.25F, 0.0F));
			body.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(0, 44).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
					PartPose.offset(-2.0F, -4.0F, -4.0F));
			body.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(6, 44).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
					PartPose.offset(2.0F, -4.0F, -4.0F));

			for (int i = 0; i < 3; i++) {
				float z = -3.0F + i * 3.0F;
				root.addOrReplaceChild("right_leg" + i, CubeListBuilder.create().texOffs(40, 32).addBox(-7.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F),
						PartPose.offsetAndRotation(-5.5F, 19.0F, z, 0.0F, 0.0F, -0.7F));
				root.addOrReplaceChild("left_leg" + i, CubeListBuilder.create().texOffs(40, 32).mirror().addBox(0.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F),
						PartPose.offsetAndRotation(5.5F, 19.0F, z, 0.0F, 0.0F, 0.7F));
			}
			return LayerDefinition.create(mesh, 64, 64);
		}

		private static void spike(PartDefinition body, String name, int u, int v, int w, int h, int d,
				float x, float y, float z, float rotX, float rotZ) {
			body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(-w / 2.0F, -h, -d / 2.0F, w, h, d),
					PartPose.offsetAndRotation(x, y, z, rotX, 0.0F, rotZ));
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			this.body.y -= RISE * state.emerge;
			boolean out = state.emerge > 0.35F;
			for (ModelPart limb : this.limbs) {
				limb.visible = out;
			}
			float swing = state.walkAnimationPos * 1.2F;
			float amount = state.walkAnimationSpeed * 0.6F;
			for (int i = 0; i < 3; i++) {
				float wave = Mth.sin(swing + i * 2.1F) * amount;
				setLeg(this.rightLegs[i], out, wave, state.emerge);
				setLeg(this.leftLegs[i], out, -wave, state.emerge);
			}
		}

		private static void setLeg(ModelPart leg, boolean out, float wave, float emerge) {
			leg.visible = out;
			leg.yRot += wave;
			leg.y += RISE * (1.0F - emerge);    // drawn in with the body while it stands up or settles
		}
	}
}
