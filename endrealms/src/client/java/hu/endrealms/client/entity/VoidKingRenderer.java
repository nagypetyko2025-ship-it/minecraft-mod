package hu.endrealms.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import hu.endrealms.EndRealms;
import hu.endrealms.entity.VoidKing;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Void King: the Rift Lord's body at a larger size, in black and gold with a burning crown. */
public class VoidKingRenderer extends HumanoidMobRenderer<VoidKing, HumanoidRenderState, RiftLordRenderer.Model> {
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/void_king/void_king.png");
	private static final Identifier GLOW = EndRealms.id("textures/entity/void_king/void_king_glow.png");
	private static final float SCALE = 2.05F;

	public VoidKingRenderer(EntityRendererProvider.Context context) {
		super(context, new RiftLordRenderer.Model(context.bakeLayer(RiftLordRenderer.LAYER)), 1.2F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
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
