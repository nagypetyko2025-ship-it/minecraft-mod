"""Dragon Graveyard biome: art (original, CC0), block and item data, worldgen files, loot, advancement and names.
usage: python gen_graveyard.py <resources dir>"""
import json
import os
import random
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
TEX = f"{A}/textures"
E = "endrealms:"
M = "minecraft:"
BIOME = "dragon_graveyard"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def hexc(h, alpha=255):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), alpha)


# ---------------------------------------------------------------- art
rnd = random.Random(71239)
PALE = [hexc(c) for c in ("#CFC8AE", "#C6BFA4", "#D7D0B6", "#BDB69C")]
soil = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        soil.putpixel((x, y), rnd.choice(PALE))
for _ in range(7):                                     # fragments of bone in the soil
    x, y = rnd.randrange(14), rnd.randrange(15)
    for dx in range(rnd.randrange(2, 4)):
        soil.putpixel((min(15, x + dx), y), hexc("#EFEBDC"))
    soil.putpixel((x, min(15, y + 1)), hexc("#9C957C"))
soil.save(f"{TEX}/block/bone_soil.png")

BONE = [hexc(c) for c in ("#B9C2B4", "#AEB8AA", "#C4CDBF")]
side = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        side.putpixel((x, y), rnd.choice(BONE))
for x in (0, 5, 10, 15):
    for y in range(16):
        side.putpixel((x, y), hexc("#8A9587"))
side.save(f"{TEX}/block/dragon_bone_block.png")
top = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        d = max(abs(x - 7.5), abs(y - 7.5))
        top.putpixel((x, y), hexc("#8A9587") if d > 6.6 or 2.6 < d < 3.6 else rnd.choice(BONE) if d >= 3.6 else hexc("#5F8C80"))
top.save(f"{TEX}/block/dragon_bone_block_top.png")

ICONS = {"dragon_bone": ["................", ".............oo.", "...........ooWLo", "..........oWLLo.", ".........oWLDo..", "........oWLDo...", ".......oWLDo....", "......oWLDo.....",
                         ".....oWLDo......", "....oWLDo.......", "...oLLDo........", ".oooLDo.........", "oWLLoo..........", ".oDo............", "..o.............", "................"],
         "spectral_whelp_spawn_egg": ["................", "......oooo......", ".....oWWWLo.....", "....oWLcLLLo....", "....oWcCcLLo....", "...oLLLcLLLLo...", "...oLLLLLLcLo...", "..oLLcLLcCcLLo..",
                                      "..oLcCcLLcLLLo..", "..oLLcLLLLLLDo..", "..oLLLLLcLLDDo..", "...oLLLLLLDDo...", "...ooLLLDDoo....", ".....oooo.......", "................", "................"]}
PAL = {"o": hexc("#3A4A44"), "W": hexc("#F2F6EC"), "L": hexc("#C4CDBF"), "D": hexc("#8A9587"), "c": hexc("#7FE0D0"), "C": hexc("#D8FFF4")}
for name, rows in ICONS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate((row + "................")[:16]):
            if ch in PAL:
                img.putpixel((x, y), PAL[ch])
    img.save(f"{TEX}/item/{name}.png")
    w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})

ghost = Image.new("RGBA", (64, 64))
for y in range(64):
    for x in range(64):
        c = rnd.choice(("#BFE8E0", "#CFEFE8", "#AEDFD6", "#E2F7F2"))
        ghost.putpixel((x, y), hexc(c, 175 if (x + y) % 7 else 215))
for y in range(0, 64, 4):                              # ribs showing through
    for x in range(64):
        px = ghost.getpixel((x, y))
        ghost.putpixel((x, y), (px[0] - 40, px[1] - 30, px[2] - 34, 225))
ghost.save(f"{TEX}/entity/spectral_whelp.png")

# ---------------------------------------------------------------- blocks and items


def drop_self(name):
    w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + name}], "condition": {"type": "minecraft:survives_explosion"}}],
                                             "random_sequence": f"endrealms:blocks/{name}"})


w(f"{A}/blockstates/bone_soil.json", {"variants": {"": {"model": "endrealms:block/bone_soil"}}})
w(f"{A}/models/block/bone_soil.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "endrealms:block/bone_soil"}})
w(f"{A}/items/bone_soil.json", {"model": {"type": "minecraft:model", "model": "endrealms:block/bone_soil"}})
drop_self("bone_soil")
w(f"{A}/blockstates/dragon_bone_block.json", {"variants": {"axis=x": {"model": "endrealms:block/dragon_bone_block", "x": 90, "y": 90}, "axis=y": {"model": "endrealms:block/dragon_bone_block"},
                                                           "axis=z": {"model": "endrealms:block/dragon_bone_block", "x": 90}}})
w(f"{A}/models/block/dragon_bone_block.json", {"parent": "minecraft:block/cube_column", "textures": {"end": "endrealms:block/dragon_bone_block_top", "side": "endrealms:block/dragon_bone_block"}})
w(f"{A}/items/dragon_bone_block.json", {"model": {"type": "minecraft:model", "model": "endrealms:block/dragon_bone_block"}})
drop_self("dragon_bone_block")
w(f"{D}/recipe/dragon_bone_block.json", {"type": "minecraft:crafting_shaped", "category": "building", "key": {"#": E + "dragon_bone"}, "pattern": ["###", "###", "###"], "result": {"id": E + "dragon_bone_block"}})
w(f"{D}/recipe/dragon_bone.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [E + "dragon_bone_block"], "result": {"id": E + "dragon_bone", "count": 9}})
w(f"{D}/recipe/bone_meal_from_dragon_bone.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [E + "dragon_bone"], "result": {"id": M + "bone_meal", "count": 9}})


def add_tag(path, values):
    p = f"{RES}/data/minecraft/tags/{path}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    t["values"] += [v for v in values if v not in t["values"]]
    w(p, t)


add_tag("block/mineable/pickaxe", [E + "bone_soil", E + "dragon_bone_block"])
add_tag("block/blocks_motion_no_leaves", [E + "bone_soil", E + "dragon_bone_block"])

# ---------------------------------------------------------------- worldgen
PLACED = {"graveyard_island": [], "bone_soil_blanket": [], "dragon_ribcage": [{"type": "minecraft:rarity_filter", "chance": 7}, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}],
          "dragon_skull": [{"type": "minecraft:rarity_filter", "chance": 14}, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}],
          "bone_tree": [{"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}]}
for name, placement in PLACED.items():
    w(f"{D}/worldgen/feature/{name}.json", {"type": E + name})
    w(f"{D}/worldgen/placed_feature/{name}.json", {"feature": E + name, "placement": placement})
w(f"{D}/worldgen/biome/{BIOME}.json", {
    "attributes": {"minecraft:gameplay/natural_mob_spawns": {"argument": {"spawn_costs": {}, "spawns_by_category": {"monster": [{"type": M + "enderman", "count": 4, "weight": 10}]}}, "modifier": "overlay"},
                   "minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": M + "white_ash"}, "probability": 0.02}], "modifier": "append"}, "minecraft:visual/fog_color": "#5E6459"},
    "carvers": [], "downfall": 0.0, "effects": {"water_color": "#3f76e4"},
    "features": [[E + "graveyard_island"], [], [], [], [M + "end_gateway_return", E + "bone_soil_blanket"], [], [], [], [], [E + "dragon_ribcage", E + "dragon_skull", E + "bone_tree"]],
    "has_precipitation": False, "temperature": 0.5})


# ---------------------------------------------------------------- loot; chorusite ingots stay out of chests on purpose
def item(name, weight, lo=1, hi=1):
    entry = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1 or lo != 1:
        entry["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return entry


GEMS = ("ember_garnet", "star_sapphire", "void_onyx", "moon_opal", "ash_quartz", "bloodstone", "deep_aquamarine")
w(f"{D}/loot_table/chests/dragon_graveyard.json", {"type": M + "chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 4, "max": 6}, "entries": [
    item(E + "dragon_bone", 12, 2, 5), item(M + "bone_block", 8, 2, 6), item(M + "diamond", 8, 1, 4), item(M + "gold_ingot", 8, 3, 9), item(M + "ender_pearl", 7, 2, 5),
    item(M + "experience_bottle", 6, 4, 10), item(M + "golden_apple", 5, 1, 3), item(E + "phasite_shard", 3), item(E + "chorusite_shard", 3, 1, 2), *[item(E + g, 1) for g in GEMS]]}],
    "random_sequence": "endrealms:chests/dragon_graveyard"})
w(f"{D}/loot_table/entities/spectral_whelp.json", {"type": M + "entity", "pools": [{"rolls": 1, "entries": [item(E + "dragon_bone", 1, 0, 2)]}, {"rolls": 1, "entries": [item(M + "phantom_membrane", 1, 0, 1)]}],
                                                   "random_sequence": "endrealms:entities/spectral_whelp"})

w(f"{D}/advancement/spectral_whelp.json", {"parent": "endrealms:root", "display": {"icon": {"id": E + "spectral_whelp_spawn_egg"}, "title": {"translate": "advancements.endrealms.spectral_whelp.title"},
                                                                                   "description": {"translate": "advancements.endrealms.spectral_whelp.description"}, "frame": "task"},
                                           "criteria": {"killed": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": E + "spectral_whelp"}}}}},
                                           "requirements": [["killed"]]})

T = "advancements.endrealms."
NAMES = {"en_us": {f"biome.endrealms.{BIOME}": "Dragon Graveyard", "block.endrealms.bone_soil": "Bone Soil", "block.endrealms.dragon_bone_block": "Block of Dragon Bone", "item.endrealms.dragon_bone": "Dragon Bone",
                   "entity.endrealms.spectral_whelp": "Spectral Whelp", "item.endrealms.spectral_whelp_spawn_egg": "Spectral Whelp Spawn Egg",
                   f"{T}spectral_whelp.title": "Laid to Rest", f"{T}spectral_whelp.description": "Defeat a Spectral Whelp"},
         "hu_hu": {f"biome.endrealms.{BIOME}": "Sárkánytemető", "block.endrealms.bone_soil": "Csonttalaj", "block.endrealms.dragon_bone_block": "Sárkánycsont-tömb", "item.endrealms.dragon_bone": "Sárkánycsont",
                   "entity.endrealms.spectral_whelp": "Sárkányszellem", "item.endrealms.spectral_whelp_spawn_egg": "Sárkányszellem idéző tojás",
                   f"{T}spectral_whelp.title": "Örök nyugalom", f"{T}spectral_whelp.description": "Győzz le egy Sárkányszellemet"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
print("ok")
