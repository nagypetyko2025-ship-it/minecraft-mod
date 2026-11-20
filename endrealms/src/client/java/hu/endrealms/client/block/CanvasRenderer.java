package hu.endrealms.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import hu.endrealms.EndRealms;
import hu.endrealms.block.CanvasBlock;
import hu.endrealms.block.CanvasBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Draws the paint as coloured squares just in front of the canvas, and outlines the pixel a held dye would paint. */
public class CanvasRenderer implements BlockEntityRenderer<CanvasBlockEntity, CanvasRenderer.State> {
	private static final Identifier WHITE = EndRealms.id("textures/block/carving_pixel.png");
	private static final float LIFT = 0.0015F;

	public static class State extends BlockEntityRenderState {
		int[] colours;
		int width;
		Direction facing;
		int aimU = -1;
		int aimV;
		int aimColour;
	}

	public CanvasRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CanvasBlockEntity entity, State state, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
		BlockEntityRenderer.super.extractRenderState(entity, state, partial, camera, overlay);
		BlockState block = entity.getBlockState();
		state.width = entity.width();
		state.facing = block.getValue(CanvasBlock.FACING);
		state.colours = new int[state.width * 16];
		for (int v = 0; v < 16; v++) {
			for (int u = 0; u < state.width; u++) {
				int colour = entity.colour(u, v);
				state.colours[v * state.width + u] = colour == 0 ? 0 : 0xFF000000 | DyeColor.byId(colour - 1).getTextureDiffuseColor();
			}
		}
		state.aimU = -1;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.hitResult instanceof BlockHitResult hit && hit.getDirection() == state.facing) {
			BlockPos hitPos = hit.getBlockPos();
			BlockState hitState = entity.getLevel().getBlockState(hitPos);
			DyeColor dye = mc.player.getMainHandItem().get(DataComponents.DYE);
			if (dye != null && hitState.getBlock() instanceof CanvasBlock && CanvasBlock.mainPos(hitState, hitPos).equals(entity.getBlockPos())) {
				int[] pixel = CanvasBlock.pixelAt(block, entity.getBlockPos(), hit.getLocation());
				state.aimU = pixel[0];
				state.aimV = pixel[1];
				state.aimColour = 0xFF000000 | dye.getTextureDiffuseColor();
			}
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(WHITE), (pose, consumer) -> {
			for (int v = 0; v < 16; v++) {
				for (int u = 0; u < state.width; u++) {
					int colour = state.colours[v * state.width + u];
					if (colour != 0) {
						quad(pose, consumer, state.facing, u, v, 1.0F, colour, state.lightCoords, LIFT);
					}
				}
			}
			if (state.aimU >= 0) {
				quad(pose, consumer, state.facing, state.aimU, state.aimV, 1.0F, state.aimColour, state.lightCoords, LIFT * 2.0F);
			}
		});
	}

	/** A point on the canvas front: u to the viewer's right, v up, both in pixels from the left block's bottom-left corner. */
	private static float[] point(Direction facing, float u, float v, float lift) {
		Direction right = CanvasBlock.right(facing);
		float along = right.getAxisDirection() == Direction.AxisDirection.POSITIVE ? u / 16.0F : 1.0F - u / 16.0F;
		boolean facingPositive = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
		float depth = facingPositive ? 1.0F / 16.0F + lift : 15.0F / 16.0F - lift;
		return right.getAxis() == Direction.Axis.X ? new float[] {along, v / 16.0F, depth} : new float[] {depth, v / 16.0F, along};
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer consumer, Direction facing, int u, int v, float size, int colour, int light, float lift) {
		float[][] corners = {point(facing, u, v, lift), point(facing, u + size, v, lift), point(facing, u + size, v + size, lift), point(facing, u, v + size, lift)};
		for (float[] p : corners) {
			consumer.addVertex(pose, p[0], p[1], p[2]).setColor(colour).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
					.setNormal(pose, facing.getStepX(), facing.getStepY(), facing.getStepZ());
		}
	}
}
