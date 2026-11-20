package hu.endrealms.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Endwalker's Guide: opens the guided route through the End. The screen itself only exists on the client. */
public class EndGuideItem extends Item {
	/** Set by the client initializer; stays a no-op on dedicated servers. */
	public static Runnable opener = () -> {
	};

	public EndGuideItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			opener.run();
		}
		return InteractionResult.SUCCESS;
	}
}
