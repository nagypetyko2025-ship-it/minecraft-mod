package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import hu.endrealms.entity.AshWraith;
import hu.endrealms.entity.AstralFish;
import hu.endrealms.entity.DeepLurker;
import hu.endrealms.entity.EndVillager;
import hu.endrealms.entity.Glimmerhoof;
import hu.endrealms.entity.RiftLord;
import hu.endrealms.entity.Shardcrab;
import hu.endrealms.entity.VoidSentinel;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	private static final ResourceKey<EntityType<?>> GLIMMERHOOF_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("glimmerhoof"));

	public static final EntityType<Glimmerhoof> GLIMMERHOOF = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			GLIMMERHOOF_KEY,
			FabricEntityType.Builder.createMob(Glimmerhoof::new, MobCategory.CREATURE, mob -> mob
							.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
									Glimmerhoof::checkGlimmerhoofSpawnRules)
							.defaultAttributes(Glimmerhoof::createAttributes))
					.sized(0.8F, 1.6F)
					.eyeHeight(1.35F)
					.clientTrackingRange(10)
					.build(GLIMMERHOOF_KEY));

	private static final ResourceKey<EntityType<?>> BONE_STAG_KEY = ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("bone_stag"));
	public static final EntityType<hu.endrealms.entity.BoneStag> BONE_STAG = Registry.register(BuiltInRegistries.ENTITY_TYPE, BONE_STAG_KEY,
			FabricEntityType.Builder.createMob(hu.endrealms.entity.BoneStag::new, MobCategory.CREATURE, mob -> mob
							.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hu.endrealms.entity.BoneStag::checkSpawnRules)
							.defaultAttributes(hu.endrealms.entity.BoneStag::createAttributes))
					.sized(0.9F, 1.8F)
					.eyeHeight(1.6F)
					.clientTrackingRange(10)
					.build(BONE_STAG_KEY));

	private static final ResourceKey<EntityType<?>> VOID_SENTINEL_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("void_sentinel"));
	public static final EntityType<VoidSentinel> VOID_SENTINEL = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			VOID_SENTINEL_KEY,
			FabricEntityType.Builder.createMob(VoidSentinel::new, MobCategory.MONSTER, mob -> mob
							.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
									VoidSentinel::checkSentinelSpawnRules)
							.defaultAttributes(VoidSentinel::createAttributes))
					.sized(0.85F, 2.55F)
					.eyeHeight(2.25F)
					.clientTrackingRange(8)
					.build(VOID_SENTINEL_KEY));

	private static final ResourceKey<EntityType<?>> SHARDCRAB_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("shardcrab"));
	public static final EntityType<Shardcrab> SHARDCRAB = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			SHARDCRAB_KEY,
			FabricEntityType.Builder.createMob(Shardcrab::new, MobCategory.MONSTER, mob -> mob
							.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
									Shardcrab::checkShardcrabSpawnRules)
							.defaultAttributes(Shardcrab::createAttributes))
					.sized(1.2F, 0.9F)
					.eyeHeight(0.6F)
					.clientTrackingRange(8)
					.build(SHARDCRAB_KEY));

	private static final ResourceKey<EntityType<?>> RIFT_LORD_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("rift_lord"));
	public static final EntityType<RiftLord> RIFT_LORD = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			RIFT_LORD_KEY,
			FabricEntityType.Builder.createMob(RiftLord::new, MobCategory.MONSTER, mob -> mob.defaultAttributes(RiftLord::createAttributes))
					.sized(1.1F, 3.4F)
					.eyeHeight(3.0F)
					.fireImmune()
					.clientTrackingRange(10)
					.build(RIFT_LORD_KEY));

	private static final ResourceKey<EntityType<?>> SPECTRAL_WHELP_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("spectral_whelp"));
	public static final EntityType<hu.endrealms.entity.SpectralWhelp> SPECTRAL_WHELP = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			SPECTRAL_WHELP_KEY,
			FabricEntityType.Builder.createMob(hu.endrealms.entity.SpectralWhelp::new, MobCategory.MONSTER, mob -> mob.defaultAttributes(hu.endrealms.entity.SpectralWhelp::createAttributes))
					.sized(0.9F, 0.5F)
					.eyeHeight(0.175F)
					.clientTrackingRange(8)
					.build(SPECTRAL_WHELP_KEY));

	private static final ResourceKey<EntityType<?>> BONE_DRAKE_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("bone_drake"));
	public static final EntityType<hu.endrealms.entity.BoneDrake> BONE_DRAKE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			BONE_DRAKE_KEY,
			FabricEntityType.Builder.createMob(hu.endrealms.entity.BoneDrake::new, MobCategory.CREATURE, mob -> mob.defaultAttributes(hu.endrealms.entity.BoneDrake::createAttributes))
					.sized(0.9F, 0.9F)
					.eyeHeight(0.75F)
					.clientTrackingRange(10)
					.build(BONE_DRAKE_KEY));

	private static final ResourceKey<EntityType<?>> VOID_KING_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("void_king"));
	public static final EntityType<hu.endrealms.entity.VoidKing> VOID_KING = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			VOID_KING_KEY,
			FabricEntityType.Builder.createMob(hu.endrealms.entity.VoidKing::new, MobCategory.MONSTER, mob -> mob.defaultAttributes(hu.endrealms.entity.VoidKing::createAttributes))
					.sized(1.3F, 4.1F)
					.eyeHeight(3.6F)
					.fireImmune()
					.clientTrackingRange(10)
					.build(VOID_KING_KEY));

	private static final ResourceKey<EntityType<?>> ASH_WRAITH_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("ash_wraith"));
	public static final EntityType<AshWraith> ASH_WRAITH = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ASH_WRAITH_KEY,
			FabricEntityType.Builder.createMob(AshWraith::new, MobCategory.MONSTER, mob -> mob
							.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
									AshWraith::checkAshWraithSpawnRules)
							.defaultAttributes(AshWraith::createAttributes))
					.sized(0.6F, 2.0F)
					.eyeHeight(1.75F)
					.fireImmune()
					.clientTrackingRange(8)
					.build(ASH_WRAITH_KEY));

	private static final ResourceKey<EntityType<?>> DEEP_LURKER_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("deep_lurker"));
	public static final EntityType<DeepLurker> DEEP_LURKER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DEEP_LURKER_KEY,
			FabricEntityType.Builder.createMob(DeepLurker::new, MobCategory.MONSTER, mob -> mob.defaultAttributes(DeepLurker::createAttributes))
					.sized(1.0F, 0.8F)
					.eyeHeight(0.5F)
					.clientTrackingRange(8)
					.build(DEEP_LURKER_KEY));

	private static final ResourceKey<EntityType<?>> END_VILLAGER_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id("end_villager"));
	public static final EntityType<EndVillager> END_VILLAGER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			END_VILLAGER_KEY,
			FabricEntityType.Builder.createMob(EndVillager::new, MobCategory.MISC, mob -> mob.defaultAttributes(EndVillager::createAttributes))
					.sized(0.6F, 2.1F)
					.eyeHeight(1.75F)
					.clientTrackingRange(10)
					.build(END_VILLAGER_KEY));

	public static final EntityType<AstralFish> STARFIN = fish("starfin", 0.5F, 0.35F);
	public static final EntityType<AstralFish> VOID_EEL = fish("void_eel", 0.6F, 0.3F);
	public static final EntityType<AstralFish> GLOWGILL = fish("glowgill", 0.5F, 0.5F);

	private static EntityType<AstralFish> fish(String name, float width, float height) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, EndRealms.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
				FabricEntityType.Builder.createMob(AstralFish::new, MobCategory.WATER_AMBIENT, mob -> mob
								.spawnPlacement(SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
										AstralFish::checkAstralFishSpawnRules)
								.defaultAttributes(AbstractFish::createAttributes))
						.sized(width, height)
						.eyeHeight(height * 0.65F)
						.clientTrackingRange(4)
						.build(key));
	}

	private ModEntities() {
	}

	public static void init() {
	}
}
