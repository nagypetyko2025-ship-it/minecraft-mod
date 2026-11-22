package hu.endrealms.client.sculk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import hu.endrealms.EndRealms;
import hu.endrealms.sculk.SculkDimension;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/**
 * The night sky of the Sculk dimension. The dimension keeps a day of its own, as long as the Overworld's: by day the
 * sky stays empty and dark; when night falls, a vast head of sculk slowly comes out of the dark high in the north,
 * with glowing eyes - something enormous up there, watching you - and fades away again before morning.
 */
public final class SculkSky {
	private static final Identifier HEAD = EndRealms.id("textures/environment/sculk_head.png");
	private static final Identifier HEAD_GLOW = EndRealms.id("textures/environment/sculk_head_glow.png");
	private static final int DAY = 24000;
	private static final int NIGHT_START = 13000;
	private static final int NIGHT_END = 23000;
	private static final int FADE = 1500;
	/** How far off and how big: far enough that mountains can stand in front of it, inside the far clipping plane. */
	private static final float DISTANCE = 300.0F;
	private static final float HALF_SIZE = 110.0F;
	/** Up in the north, about a third of the way to the top of the sky. */
	private static final Vector3f DIRECTION = new Vector3f(0.0F, 0.55F, -1.0F).normalize();

	private SculkSky() {
	}

	public static void init() {
		LevelRenderEvents.COLLECT_SUBMITS.register(SculkSky::render);
	}

	/** 0 by day, 1 deep in the night, fading in and out at dusk and dawn. */
	static float night(ClientLevel level, float partialTick) {
		float time = (level.getGameTime() % DAY) + partialTick;
		if (time < NIGHT_START || time > NIGHT_END) {
			return 0.0F;
		}
		float in = Math.min(1.0F, (time - NIGHT_START) / FADE);
		float out = Math.min(1.0F, (NIGHT_END - time) / FADE);
		float f = Math.min(in, out);
		return f * f * (3.0F - 2.0F * f);
	}

	private static void render(LevelRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || level.dimension() != SculkDimension.LEVEL) {
			return;
		}
		float strength = night(level, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
		if (strength <= 0.01F) {
			return;
		}
		// the eyes drift a little, slowly, as if it were looking about
		float t = level.getGameTime() / 400.0F;
		Vector3f facing = new Vector3f(DIRECTION).add((float) Math.sin(t) * 0.03F, (float) Math.sin(t * 0.7F) * 0.015F, 0.0F).normalize();
		Vector3f right = new Vector3f(facing).cross(0.0F, 1.0F, 0.0F).normalize();
		Vector3f up = new Vector3f(right).cross(facing).normalize();
		Vector3f center = new Vector3f(facing).mul(DISTANCE);
		PoseStack pose = context.poseStack();
		int alpha = (int) (strength * 235.0F);
		context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.entityTranslucent(HEAD),
				(matrix, consumer) -> quad(matrix, consumer, center, right, up, facing, (alpha << 24) | 0xFFFFFF));
		int glow = (int) (strength * 255.0F);
		context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.eyes(HEAD_GLOW),
				(matrix, consumer) -> quad(matrix, consumer, center, right, up, facing, 0xFF000000 | (glow << 16) | (glow << 8) | glow));
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer consumer, Vector3f center, Vector3f right, Vector3f up, Vector3f facing, int colour) {
		float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
		for (float[] c : corners) {
			float x = center.x + (right.x * c[0] + up.x * c[1]) * HALF_SIZE;
			float y = center.y + (right.y * c[0] + up.y * c[1]) * HALF_SIZE;
			float z = center.z + (right.z * c[0] + up.z * c[1]) * HALF_SIZE;
			consumer.addVertex(pose, x, y, z).setColor(colour).setUv(c[2], c[3]).setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(0xF000F0).setNormal(pose, -facing.x, -facing.y, -facing.z);
		}
	}
}
