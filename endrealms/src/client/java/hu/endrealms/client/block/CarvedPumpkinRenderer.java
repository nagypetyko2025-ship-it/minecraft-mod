package hu.endrealms.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import hu.endrealms.EndRealms;
import hu.endrealms.block.CarvedPumpkinBlock;
import hu.endrealms.block.CarvedPumpkinBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Draws the cut pixels as dark squares just above the pumpkin's faces, and marks the pixel the shears are aimed at. */
public class CarvedPumpkinRenderer implements BlockEntityRenderer<CarvedPumpkinBlockEntity, CarvedPumpkinRenderer.State> {
	private static final Identifier WHITE = EndRealms.id("textures/block/carving_pixel.png");
	private static final int CARVED = 0xFF241204;
	private static final int GLOWING = 0xFFFFC23C;
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final int AIM = 0xFFFFE4A0;
	private static final float LIFT = 0.0015F;

	public static class State extends BlockEntityRenderState {
		long[] pixels;
		boolean lit;
		Direction aimFace;
		int aimA;
		int aimB;
	}

	public CarvedPumpkinRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CarvedPumpkinBlockEntity entity, State state, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
		BlockEntityRenderer.super.extractRenderState(entity, state, partial, camera, overlay);
		state.pixels = entity.pixels().clone();
		state.lit = entity.getBlockState().getValue(CarvedPumpkinBlock.LIT);
		state.aimFace = null;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(entity.getBlockPos())
				&& (mc.player.getMainHandItem().is(Items.SHEARS) || mc.player.getOffhandItem().is(Items.SHEARS))) {
			int[] pixel = CarvedPumpkinBlock.pixelAt(entity.getBlockPos(), hit.getDirection(), hit.getLocation());
			state.aimFace = hit.getDirection();
			state.aimA = pixel[0];
			state.aimB = pixel[1];
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(WHITE), (pose, consumer) -> {
			for (Direction face : Direction.values()) {
				for (int b = 0; b < 16; b++) {
					for (int a = 0; a < 16; a++) {
						if (CarvedPumpkinBlockEntity.isCarved(state.pixels, face, a, b)) {
							quad(pose, consumer, face, a, b, state.lit ? GLOWING : CARVED, state.lit ? FULL_BRIGHT : state.lightCoords, LIFT);
						}
					}
				}
			}
			if (state.aimFace != null) {
				quad(pose, consumer, state.aimFace, state.aimA, state.aimB, AIM, state.lightCoords, LIFT * 2.0F);
			}
		});
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float[] p, Direction face, int color, int light) {
		consumer.addVertex(pose, p[0], p[1], p[2]).setColor(color).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
				.setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
	}

	private static float[] point(Direction face, int[] axes, float a, float b, float lift) {
		float[] p = new float[3];
		p[axes[0]] = a / 16.0F;
		p[axes[1]] = b / 16.0F;
		int normal = face.getAxis().ordinal();
		p[normal] = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F + lift : -lift;
		return p;
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer consumer, Direction face, int a, int b, int color, int light, float lift) {
		int[] axes = CarvedPumpkinBlock.axes(face);
		float[] p0 = point(face, axes, a, b, lift);
		float[] p1 = point(face, axes, a + 1, b, lift);
		float[] p2 = point(face, axes, a + 1, b + 1, lift);
		float[] p3 = point(face, axes, a, b + 1, lift);
		boolean counterClockwise = face == Direction.WEST || face == Direction.DOWN || face == Direction.SOUTH;
		float[][] order = counterClockwise ? new float[][] {p0, p1, p2, p3} : new float[][] {p3, p2, p1, p0};
		for (float[] p : order) {
			vertex(pose, consumer, p, face, color, light);
		}
	}
}
