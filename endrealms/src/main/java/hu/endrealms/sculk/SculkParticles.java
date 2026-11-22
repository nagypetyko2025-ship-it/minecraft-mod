package hu.endrealms.sculk;

import hu.endrealms.EndRealms;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Particles of the Sculk dimension. */
public final class SculkParticles {
	/** The will-moths fluttering around lights (only ever spawned on the client). */
	public static final SimpleParticleType WILL_MOTH = Registry.register(BuiltInRegistries.PARTICLE_TYPE, EndRealms.id("will_moth"), FabricParticleTypes.simple(true));

	private SculkParticles() {
	}

	public static void init() {
	}
}
