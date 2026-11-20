package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import hu.endrealms.block.CarvedPumpkinBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<CarvedPumpkinBlockEntity> CARVED_PUMPKIN = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EndRealms.id("carved_pumpkin"),
			FabricBlockEntityTypeBuilder.create(CarvedPumpkinBlockEntity::new, ModBlocks.CARVED_PUMPKIN).build());

	public static final BlockEntityType<hu.endrealms.mail.PhaseMailboxBlockEntity> PHASE_MAILBOX = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EndRealms.id("phase_mailbox"),
			FabricBlockEntityTypeBuilder.create(hu.endrealms.mail.PhaseMailboxBlockEntity::new, ModBlocks.PHASE_MAILBOX).build());

	public static final BlockEntityType<hu.endrealms.block.CanvasBlockEntity> CANVAS = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EndRealms.id("canvas"),
			FabricBlockEntityTypeBuilder.create(hu.endrealms.block.CanvasBlockEntity::new, ModBlocks.CANVAS, ModBlocks.LARGE_CANVAS).build());

	public static final BlockEntityType<hu.endrealms.brew.AgingBarrelBlockEntity> AGING_BARREL = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EndRealms.id("aging_barrel"),
			FabricBlockEntityTypeBuilder.create(hu.endrealms.brew.AgingBarrelBlockEntity::new, ModBlocks.AGING_BARREL).build());

	public static final BlockEntityType<hu.endrealms.brew.FermentationStationBlockEntity> FERMENTATION_STATION = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EndRealms.id("fermentation_station"),
			FabricBlockEntityTypeBuilder.create(hu.endrealms.brew.FermentationStationBlockEntity::new, ModBlocks.FERMENTATION_STATION).build());

	private ModBlockEntities() {
	}

	public static void init() {
	}
}
