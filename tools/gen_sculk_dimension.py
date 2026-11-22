"""Data for the Sculk dimension (phase 1): the dimension and its type, the terrain (an Overworld-like landscape, but dead
and dark), its two biomes and their features.
The terrain shape is the vanilla Overworld's, read from the Minecraft jar in the project's Loom cache; only the blocks
differ, so the noise settings are copied from there with the surface rule swapped.
usage: python tools/gen_sculk_dimension.py"""
import glob
import json
import os
import zipfile

ROOT = r"D:\minecraft mod\endrealms"
DATA = os.path.join(ROOT, r"src\main\resources\data\endrealms")
E = "endrealms:"
M = "minecraft:"


def w(path, obj):
    path = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


jar = glob.glob(os.path.join(ROOT, r".gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-*\26.3\minecraft-common-*-26.3.jar"))[0]
with zipfile.ZipFile(jar) as z:
    noise = json.loads(z.read("data/minecraft/worldgen/noise_settings/overworld.json"))

# --- dimension type: always night-dark, no sun, no music, thick dark fog for a horizon -------------------------------
w("dimension_type/sculk.json", {
    "ambient_light": 0.0,
    "attributes": {
        "minecraft:audio/ambient_sounds": {
            "loop": "minecraft:ambient.soul_sand_valley.loop",
            "additions": {"sound": "minecraft:ambient.soul_sand_valley.additions", "tick_chance": 0.0111},
            "mood": {"block_search_extent": 8, "offset": 2.0, "sound": "minecraft:ambient.soul_sand_valley.mood", "tick_delay": 2400},
        },
        "minecraft:audio/background_music": {},
        "minecraft:audio/music_volume": 0.0,
        "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never", "destroy_on_use": True},
        "minecraft:gameplay/straw_bed_rule": {"can_set_spawn": "never", "can_sleep": "never", "destroy_on_use": True},
        "minecraft:gameplay/respawn_anchor_works": False,
        "minecraft:visual/ambient_light_color": "#0b1214",
        "minecraft:visual/fog_color": "#15191a",
        "minecraft:visual/sky_color": "#060708",
        "minecraft:visual/sky_light_color": "#3c5a63",
        "minecraft:visual/sky_light_factor": 0.22,
        "minecraft:visual/fog_start_distance": 24.0,
        "minecraft:visual/fog_end_distance": 160.0,
    },
    "coordinate_scale": 1.0,
    "has_ceiling": False,
    "has_ender_dragon_fight": False,
    "has_fixed_time": True,
    "has_skylight": True,
    "height": 384,
    "infiniburn": "#minecraft:infiniburn_overworld",
    "logical_height": 384,
    "min_y": -64,
    "monster_spawn_block_light_limit": 0,
    "monster_spawn_light_level": {"type": "minecraft:uniform", "max_inclusive": 7, "min_inclusive": 0},
    "skybox": "none",
    "timelines": "#endrealms:in_sculk",
})
w("tags/timeline/in_sculk.json", {"values": ["#minecraft:universal"]})

# --- the dimension: Overworld-shaped terrain, two biomes split by the vegetation noise --------------------------------
FULL = [-1.0, 1.0]


def point(biome, humidity=FULL, continentalness=(-1.2, 1.0), erosion=FULL, weirdness=FULL, offset=0.0):
    return {"biome": E + biome, "parameters": {"temperature": FULL, "humidity": list(humidity), "continentalness": list(continentalness),
                                              "erosion": list(erosion), "weirdness": list(weirdness), "depth": 0.0, "offset": offset}}


w("dimension/sculk.json", {
    "type": E + "sculk",
    "generator": {
        "type": "minecraft:noise",
        "settings": E + "sculk",
        # the Overworld's own biome map, each vanilla biome standing in for one of ours (SculkBiomeSource): the peaks land
        # on the real mountains, the woods where forests grow, the canyons on badlands and deserts, the barrens elsewhere
        "biome_source": {"type": E + "sculk", "barrens": E + "sculk_barrens", "woods": E + "withered_woods",
                         "peaks": E + "sculk_peaks", "canyons": E + "sculk_canyons"},
    },
})

noise["material_rule"] = E + "sculk"
noise["default_block"] = E + "sculk_stone"
noise["default_fluid"] = "minecraft:air"         # no water anywhere: the seas are dry, deep basins
noise.pop("aquifers", None)                       # nor underground lakes of water or lava
noise["disable_mob_generation"] = True
w("worldgen/noise_settings/sculk.json", noise)


def block(name):
    return {"type": "minecraft:block", "result_state": name}


def noisy(min_t, max_t, then):
    return {"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:surface",
                                                       "min_threshold": min_t, "max_threshold": max_t}, "then_run": then}


def in_biome(biome, then):
    return {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [E + biome]}, "then_run": then}


def depth(add_surface_depth, then):
    return {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "add_surface_depth": add_surface_depth, "offset": 0,
                                                       "secondary_depth_range": 0, "surface_type": "floor"}, "then_run": then}


# sculk over nearly everything, the bare Sculk Soil showing through in patches (more of it on the barrens)
top = {"type": "minecraft:sequence", "sequence": [
    # bare dark rock on the heights, sculk clinging in the hollows
    in_biome("sculk_peaks", {"type": "minecraft:sequence", "sequence": [noisy(0.0, 10.0, block(M + "sculk")), block(E + "sculk_stone")]}),
    in_biome("withered_woods", {"type": "minecraft:sequence", "sequence": [noisy(-0.55, 10.0, block(M + "sculk")), block(E + "sculk_soil")]}),
    {"type": "minecraft:sequence", "sequence": [noisy(-0.2, 10.0, block(M + "sculk")), noisy(-10.0, -0.65, block(M + "soul_soil")), block(E + "sculk_soil")]},
]}
under = {"type": "minecraft:sequence", "sequence": [in_biome("sculk_peaks", block(E + "sculk_stone")), block(E + "sculk_soil")]}
w("worldgen/material_rule/sculk.json", {"type": "minecraft:sequence", "sequence": [
    "minecraft:bedrock_floor",
    {"type": "minecraft:condition", "if_true": {"type": "minecraft:above_preliminary_surface"},
     "then_run": {"type": "minecraft:sequence", "sequence": [depth(False, top), depth(True, under)]}},
    # bands of deepslate through the Sculk Stone, like calcite in stony peaks
    {"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:calcite",
                                                "min_threshold": -0.03, "max_threshold": 0.03}, "then_run": block(M + "deepslate")},
]})

# --- features ----------------------------------------------------------------------------------------------------------
w("worldgen/feature/dead_tree.json", {"type": E + "dead_tree"})
w("worldgen/feature/sculk_patch_surface.json", {"type": "minecraft:sculk_patch", "amount_per_charge": 24, "charge_count": 8,
                                                 "growth_rounds": 0, "spread_attempts": 48, "spread_rounds": 1})
SURFACE = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
w("worldgen/placed_feature/dead_tree_sparse.json", {"feature": E + "dead_tree", "placement": [{"type": "minecraft:rarity_filter", "chance": 3}] + SURFACE})
w("worldgen/placed_feature/dead_tree_dense.json", {"feature": E + "dead_tree", "placement": [
    {"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 6}}] + SURFACE})
w("worldgen/placed_feature/sculk_patch_surface.json", {"feature": E + "sculk_patch_surface", "placement": [{"type": "minecraft:rarity_filter", "chance": 2}] + SURFACE})
w("worldgen/placed_feature/sculk_patch_surface_dense.json", {"feature": E + "sculk_patch_surface", "placement": [{"type": "minecraft:count", "count": 3}] + SURFACE})

CARVERS = ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]

# deep canyons: the vanilla canyon, but far more often, starting near the surface and cut much deeper and wider
w("worldgen/carver/deep_canyon.json", {
    "type": "minecraft:canyon",
    "probability": 0.22,
    "shape": {
        "distance_factor": {"type": "minecraft:uniform", "min_inclusive": 0.9, "max_exclusive": 1.2},
        "horizontal_radius_factor": {"type": "minecraft:uniform", "min_inclusive": 1.1, "max_exclusive": 1.6},
        "thickness": {"type": "minecraft:trapezoid", "min": 2.0, "max": 9.0, "plateau": 4.0},
        "vertical_radius_center_factor": 0.0,
        "vertical_radius_default_factor": 1.0,
        "width_smoothness": 3,
        "y_scale": 5.0,
    },
    "vertical_rotation": {"type": "minecraft:uniform", "min_inclusive": -0.08, "max_exclusive": 0.08},
    "y": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 30}, "max_inclusive": {"absolute": 72}},
})


def biome(name, fog, fog_end, particle, chance, vegetation, carvers=CARVERS):
    w(f"worldgen/biome/{name}.json", {
        "attributes": {
            "minecraft:visual/fog_color": fog,
            "minecraft:visual/sky_color": "#060708",
            "minecraft:visual/water_fog_color": "#0a1214",
            "minecraft:visual/fog_end_distance": fog_end,
            "minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": particle}, "probability": chance}], "modifier": "append"},
        },
        "carvers": carvers,
        "downfall": 0.0,
        "effects": {"water_color": "#1c2a2e", "grass_color": "#4a4f45", "foliage_color": "#3d4239", "dry_foliage_color": "#4b4b44"},
        "features": [[], [], [], [], [], [], [], [], [], vegetation, []],
        "has_precipitation": False,
        "temperature": 0.5,
    })


biome("sculk_barrens", "#1b1f20", 176.0, "minecraft:white_ash", 0.006,
      [E + "dead_tree_sparse", E + "sculk_patch_surface", "minecraft:patch_dead_bush"])
biome("withered_woods", "#101415", 112.0, "minecraft:ash", 0.012,
      [E + "dead_tree_dense", E + "sculk_patch_surface_dense", "minecraft:pale_moss_patch"])
biome("sculk_peaks", "#181c1e", 192.0, "minecraft:white_ash", 0.004,
      [E + "dead_tree_sparse", E + "sculk_patch_surface"])
biome("sculk_canyons", "#16191a", 160.0, "minecraft:ash", 0.006,
      [E + "dead_tree_sparse", E + "sculk_patch_surface_dense", "minecraft:patch_dead_bush"], CARVERS + [E + "deep_canyon"])
print("sculk dimension data written")
