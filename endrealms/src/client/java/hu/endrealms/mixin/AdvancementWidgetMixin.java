package hu.endrealms.mixin;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The vanilla connector always leaves its parent on the right; a child on the left needs the mirror image. */
@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {
	@Shadow
	private AdvancementWidget parent;

	@Shadow
	@Final
	private List<AdvancementWidget> children;

	@Shadow
	@Final
	private int x;

	@Shadow
	@Final
	private int y;

	@Shadow
	public abstract void extractConnectivity(GuiGraphicsExtractor graphics, int xOffset, int yOffset, boolean outline);

	@Inject(method = "extractConnectivity", at = @At("HEAD"), cancellable = true)
	private void endrealms$mirrored(GuiGraphicsExtractor g, int xOffset, int yOffset, boolean outline, CallbackInfo ci) {
		if (parent == null) {
			return;
		}
		AdvancementWidgetMixin other = (AdvancementWidgetMixin) (Object) parent;
		if (this.x >= other.x) {
			return;
		}
		ci.cancel();
		int fromX = xOffset + other.x + 13;
		int bendX = xOffset + other.x - 4;
		int fromY = yOffset + other.y + 13;
		int toX = xOffset + this.x + 13;
		int toY = yOffset + this.y + 13;
		int color = outline ? 0xFF000000 : 0xFFFFFFFF;
		if (outline) {
			g.horizontalLine(bendX, fromX, fromY - 1, color);
			g.horizontalLine(bendX - 1, fromX, fromY, color);
			g.horizontalLine(bendX, fromX, fromY + 1, color);
			g.horizontalLine(toX, bendX + 1, toY - 1, color);
			g.horizontalLine(toX, bendX + 1, toY, color);
			g.horizontalLine(toX, bendX + 1, toY + 1, color);
			g.verticalLine(bendX - 1, toY, fromY, color);
			g.verticalLine(bendX + 1, toY, fromY, color);
		} else {
			g.horizontalLine(bendX, fromX, fromY, color);
			g.horizontalLine(toX, bendX, toY, color);
			g.verticalLine(bendX, toY, fromY, color);
		}
		for (AdvancementWidget child : children) {
			child.extractConnectivity(g, xOffset, yOffset, outline);
		}
	}
}
