package hu.endrealms.client.entity;

import hu.endrealms.EndRealms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Spectral Whelp: a phantom's body, pale and see-through. */
public class SpectralWhelpRenderer extends PhantomRenderer {
	private static final Identifier TEXTURE = EndRealms.id("textures/entity/spectral_whelp.png");

	public SpectralWhelpRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(PhantomRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(PhantomRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return RenderTypes.entityTranslucent(TEXTURE);
	}
}
