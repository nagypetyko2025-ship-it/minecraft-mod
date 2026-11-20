package hu.endrealms.registry;

import hu.endrealms.EndRealms;
import hu.endrealms.structure.CryptPiece;
import hu.endrealms.structure.CryptStructure;
import hu.endrealms.structure.EndVillagePiece;
import hu.endrealms.structure.EndVillageStructure;
import hu.endrealms.structure.ObservatoryPiece;
import hu.endrealms.structure.ObservatoryStructure;
import hu.endrealms.structure.RiftSanctumPiece;
import hu.endrealms.structure.SkyWreckPiece;
import hu.endrealms.structure.SkyWreckStructure;
import hu.endrealms.structure.RiftSanctumStructure;
import hu.endrealms.structure.VoidBastionPiece;
import hu.endrealms.structure.VoidBastionStructure;
import hu.endrealms.structure.VoidCitadelPiece;
import hu.endrealms.structure.VoidCitadelStructure;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class ModStructures {
	public static final StructureType<VoidBastionStructure> VOID_BASTION = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			EndRealms.id("void_bastion"), () -> VoidBastionStructure.CODEC);
	public static final StructurePieceType VOID_BASTION_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			EndRealms.id("void_bastion"), (StructurePieceType.ContextlessType) VoidBastionPiece::new);

	public static final StructureType<EndVillageStructure> END_VILLAGE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			EndRealms.id("end_village"), () -> EndVillageStructure.CODEC);
	public static final StructurePieceType END_VILLAGE_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			EndRealms.id("end_village"), (StructurePieceType.ContextlessType) EndVillagePiece::new);

	public static final StructureType<RiftSanctumStructure> RIFT_SANCTUM = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			EndRealms.id("rift_sanctum"), () -> RiftSanctumStructure.CODEC);
	public static final StructurePieceType RIFT_SANCTUM_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			EndRealms.id("rift_sanctum"), (StructurePieceType.ContextlessType) RiftSanctumPiece::new);

	public static final StructureType<SkyWreckStructure> SKY_WRECK = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			EndRealms.id("sky_wreck"), () -> SkyWreckStructure.CODEC);
	public static final StructurePieceType SKY_WRECK_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			EndRealms.id("sky_wreck"), (StructurePieceType.ContextlessType) SkyWreckPiece::new);

	public static final StructureType<ObservatoryStructure> OBSERVATORY = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			EndRealms.id("observatory"), () -> ObservatoryStructure.CODEC);
	public static final StructurePieceType OBSERVATORY_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			EndRealms.id("observatory"), (StructurePieceType.ContextlessType) ObservatoryPiece::new);

	public static final StructureType<CryptStructure> CRYPT = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			EndRealms.id("crypt"), () -> CryptStructure.CODEC);
	public static final StructurePieceType CRYPT_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			EndRealms.id("crypt"), (StructurePieceType.ContextlessType) CryptPiece::new);

	public static final StructureType<VoidCitadelStructure> VOID_CITADEL = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			EndRealms.id("void_citadel"), () -> VoidCitadelStructure.CODEC);
	public static final StructurePieceType VOID_CITADEL_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			EndRealms.id("void_citadel"), (StructurePieceType.ContextlessType) VoidCitadelPiece::new);

	/** Structures findable with /astra locate &lt;name&gt; (name = the id path). */
	public static final ResourceKey<Structure> CRYPT_KEY = ResourceKey.create(Registries.STRUCTURE, EndRealms.id("crypt"));
	public static final ResourceKey<Structure> VOID_BASTION_KEY = ResourceKey.create(Registries.STRUCTURE, EndRealms.id("void_bastion"));

	private ModStructures() {
	}

	public static void init() {
	}
}
