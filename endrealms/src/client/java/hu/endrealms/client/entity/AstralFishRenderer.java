package hu.endrealms.client.entity;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.AstralFish;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Shared renderer for the Astral Basin fish: base skin plus a full-bright glow layer. */
public class AstralFishRenderer extends MobRenderer<AstralFish, LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {
	private final Identifier texture;

	public AstralFishRenderer(EntityRendererProvider.Context context, EntityModel<LivingEntityRenderState> model, String name) {
		super(context, model, 0.3F);
		this.texture = EndRealms.id("textures/entity/fish/" + name + ".png");
		Identifier glow = EndRealms.id("textures/entity/fish/" + name + "_glow.png");
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(glow);
			}
		});
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return texture;
	}
}
