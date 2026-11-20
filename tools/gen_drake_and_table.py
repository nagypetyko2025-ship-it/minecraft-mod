"""Bone Drake and End Crafting Table: art (original, CC0), block/item data, the 5x5 recipes, advancement and names.
usage: python gen_drake_and_table.py <resources dir>"""
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


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def hexc(h, alpha=255):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), alpha)


rnd = random.Random(52817)

# ---------------------------------------------------------------- Bone Drake, 64x64 (layout in BoneDrakeRenderer)
BONE = [hexc(c) for c in ("#DAD4BE", "#D2CCB4", "#E2DCC8", "#CAC3AA")]
DARK = [hexc(c) for c in ("#A49D84", "#9A937A", "#AEA78E")]
HORN = [hexc(c) for c in ("#5E584A", "#6A6454", "#544F42")]
MEMBRANE = [hexc(c) for c in ("#6F8C86", "#678680", "#76938C")]
drake = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def box(u, v, wd, h, d, palette, belly=None):
    """Paints every face of a model box; the bottom face (and lower half of the sides) in the belly palette."""
    for y in range(v, v + d + h):
        for x in range(u, u + 2 * (d + wd)):
            if y < v + d and not (u + d <= x < u + d + 2 * wd):
                continue
            under = belly and ((y < v + d and x >= u + d + wd) or (y >= v + d and y >= v + d + h * 0.6))
            drake.putpixel((x, y), rnd.choice(belly if under else palette))


box(0, 0, 6, 5, 10, BONE, DARK)       # body
for x in range(10, 16):                # bony ridge stripe along the back (top face centre)
    for y in range(0, 10, 3):
        drake.putpixel((x, y), rnd.choice(DARK))
box(0, 16, 3, 3, 5, BONE, DARK)       # neck
box(32, 0, 5, 4, 5, BONE, DARK)       # head
box(32, 10, 3, 2, 3, BONE, DARK)      # snout
box(44, 10, 1, 1, 4, HORN)            # horns
box(54, 10, 1, 2, 1, HORN)            # back spikes
box(16, 16, 3, 3, 6, BONE, DARK)      # tail
box(34, 16, 2, 2, 6, BONE, DARK)      # tail tip
box(0, 26, 2, 6, 2, DARK)             # legs
for x in range(0, 8):
    drake.putpixel((x, 33), hexc("#4A463A"))       # claws
box(8, 26, 9, 2, 2, BONE, DARK)       # wing bone
for y in range(36, 44):               # membrane, top and bottom faces, with finger bones running out from the arm
    for x in range(8, 26):
        lx = (x - 8) % 9
        vein = (y - 36) in (0,) or lx in (0, 4, 8) and (y - 36) < 7 - (lx // 3)
        drake.putpixel((x, y), rnd.choice(DARK) if vein else rnd.choice(MEMBRANE))
# eyes on the head's front face (37..41, 5..8), glowing
for ex in (37, 41):
    drake.putpixel((ex, 6), hexc("#7FE0E8"))
    glow.putpixel((ex, 6), hexc("#9FF4FA"))
for x in range(32, 37):              # nostrils / mouth line on the snout front (35..37, 13..14)
    pass
drake.putpixel((35, 13), hexc("#4A463A"))
drake.putpixel((37, 13), hexc("#4A463A"))
os.makedirs(f"{TEX}/entity/bone_drake", exist_ok=True)
drake.save(f"{TEX}/entity/bone_drake/bone_drake.png")
glow.save(f"{TEX}/entity/bone_drake/bone_drake_glow.png")

# ---------------------------------------------------------------- icons and the table's faces
EGG = ["................", "......oooo......", ".....oWWWWo.....", "....oWWBWWWo....", "....oWBWWWWo....", "...oWWWWWDWWo...", "...oWWWWDDWWo...", "..oWDWWWWWWBWo..",
       "..oWDDWWWWWWWo..", "..oWWWWWBWWWWo..", "..oWWWWWWWWDWo..", "..oWWBWWWWDDWo..", "...oWWWWWWWWo...", "...oDWWWWWWDo...", "....ooDDDDoo....", "......oooo......"]
COL = {"o": hexc("#3B372D"), "W": hexc("#DAD4BE"), "D": hexc("#A49D84"), "B": hexc("#6F8C86")}
egg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(EGG):
    for x, c in enumerate(row):
        if c in COL:
            egg.putpixel((x, y), COL[c])
egg.save(f"{TEX}/item/bone_drake_spawn_egg.png")

VOID = [hexc(c) for c in ("#2E2545", "#33294D", "#2A2140", "#382D52")]
top = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        edge = x in (0, 15) or y in (0, 15)
        grid = x % 3 == 0 or y % 3 == 0
        top.putpixel((x, y), hexc("#1E1830") if edge else hexc("#7FE0E8") if grid and 0 < x < 15 and 0 < y < 15 else rnd.choice(VOID))
top.save(f"{TEX}/block/end_crafting_table_top.png")
side = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        side.putpixel((x, y), hexc("#1E1830") if y in (0, 4) or x in (0, 15) else hexc("#4A3A78") if y < 4 else rnd.choice(VOID))
for (x, y) in ((7, 7), (8, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9), (7, 10), (8, 10), (7, 11), (8, 11)):   # a prism crystal on each side
    side.putpixel((x, y), hexc("#7FE0E8") if y < 9 else hexc("#4FB3BC"))
for (x, y) in ((3, 9), (12, 9), (3, 10), (12, 10)):
    side.putpixel((x, y), hexc("#B98CF0"))
side.save(f"{TEX}/block/end_crafting_table_side.png")

# ---------------------------------------------------------------- block and item data
w(f"{A}/blockstates/end_crafting_table.json", {"variants": {"": {"model": E + "block/end_crafting_table"}}})
w(f"{A}/models/block/end_crafting_table.json", {"parent": M + "block/cube_bottom_top", "textures": {
    "bottom": E + "block/polished_voidstone", "side": E + "block/end_crafting_table_side", "top": E + "block/end_crafting_table_top"}})
w(f"{A}/items/end_crafting_table.json", {"model": {"type": M + "model", "model": E + "block/end_crafting_table"}})
w(f"{A}/models/item/bone_drake_spawn_egg.json", {"parent": M + "item/generated", "textures": {"layer0": E + "item/bone_drake_spawn_egg"}})
w(f"{A}/items/bone_drake_spawn_egg.json", {"model": {"type": M + "model", "model": E + "item/bone_drake_spawn_egg"}})
w(f"{D}/loot_table/blocks/end_crafting_table.json", {"type": M + "block", "pools": [{"rolls": 1, "entries": [{"type": M + "item", "name": E + "end_crafting_table"}],
                                                                                     "condition": {"type": M + "survives_explosion"}}],
                                                       "random_sequence": E + "blocks/end_crafting_table"})

# ---------------------------------------------------------------- recipes: the table in 3x3, the End's greater items in 5x5
w(f"{D}/recipe/end_crafting_table.json", {"type": M + "crafting_shaped", "category": "misc", "pattern": ["CEC", "VTV", "VVV"],
                                          "key": {"C": M + "popped_chorus_fruit", "E": M + "ender_pearl", "T": M + "crafting_table", "V": E + "polished_voidstone"},
                                          "result": {"id": E + "end_crafting_table"}})
PHASITE = [E + "phasite_ore", E + "phasite_shard"]
FIVE = {
    "phase_pearl": ([" CRC ", "CPEPC", "RE*ER", "CPEPC", " CRC "],
                    {"C": M + "popped_chorus_fruit", "R": E + "prism_shard", "P": PHASITE, "E": M + "ender_pearl", "*": M + "ender_eye"}),
    "phase_anchor": (["VVVVV", "VRORV", "VOPOV", "VRORV", "VVVVV"],
                     {"V": E + "polished_voidstone", "R": E + "prism_shard", "O": M + "obsidian", "P": PHASITE}),
    "riftblade": (["  G  ", " RHR ", "G S G", " R R "],
                  {"G": E + "phasite_gem", "H": E + "rift_heart", "S": E + "chorusite_sword", "R": E + "prism_shard"}),
    "gem_table": ([" RGR ", "RAGAR", "VVSVV", "V   V", "V   V"],
                  {"R": E + "prism_shard", "G": M + "diamond", "A": M + "amethyst_shard", "V": E + "polished_voidstone", "S": M + "smithing_table"}),
    "volcanic_vent": (["E S E", "ERMRE", "EMLME", "ERMRE", "EEEEE"],
                      {"E": E + "ember_rock", "S": E + "scorched_end_stone", "R": M + "blaze_powder", "M": M + "magma_block", "L": M + "lava_bucket"}),
    "phase_mailbox": ([" RDR ", "PEAEP", "VVCVV", "VVVVV"],
                      {"R": E + "prism_shard", "D": M + "diamond", "P": E + "phasite_shard", "E": M + "ender_eye", "A": E + "phase_anchor", "C": M + "chest", "V": E + "polished_voidstone"}),
    "gravity_stone": (["F D F", " PCP ", "DCSCD", " PCP ", "F D F"],
                      {"F": M + "feather", "D": E + "deepglow_shard", "P": E + "phasite_shard", "C": E + "crystalline_voidstone", "S": M + "slime_block"}),
}
for name, (pattern, key) in FIVE.items():
    assert all(c == " " or c in key for row in pattern for c in row), name
    w(f"{D}/recipe/{name}.json", {"type": E + "end_crafting", "pattern": pattern, "key": key, "result": {"id": E + name}})

# ---------------------------------------------------------------- advancement and names
w(f"{D}/advancement/bone_drake.json", {"parent": "endrealms:root", "display": {"icon": {"id": E + "bone_drake_spawn_egg"},
                                                                               "title": {"translate": "advancements.endrealms.bone_drake.title"},
                                                                               "description": {"translate": "advancements.endrealms.bone_drake.description"}, "frame": "goal"},
                                       "criteria": {"tamed": {"trigger": M + "tame_animal", "conditions": {"entity": {"type": M + "entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": E + "bone_drake"}}}}},
                                       "requirements": [["tamed"]]})
NAMES = {"en_us": {"entity.endrealms.bone_drake": "Bone Drake", "item.endrealms.bone_drake_spawn_egg": "Bone Drake Spawn Egg",
                   "block.endrealms.end_crafting_table": "End Crafting Table", "container.endrealms.end_crafting_table": "End Crafting Table",
                   "container.endrealms.end_crafting_table.recipes": "Recipes", "container.endrealms.end_crafting_table.none": "No recipes",
                   "container.endrealms.end_crafting_table.hint": "Click: show and fill",
                   "advancements.endrealms.bone_drake.title": "Dragon Tamer", "advancements.endrealms.bone_drake.description": "Tame the Bone Drake of a Dragon Graveyard"},
         "hu_hu": {"entity.endrealms.bone_drake": "Csontsárkány", "item.endrealms.bone_drake_spawn_egg": "Csontsárkány idéző tojás",
                   "block.endrealms.end_crafting_table": "End-barkácsasztal", "container.endrealms.end_crafting_table": "End-barkácsasztal",
                   "container.endrealms.end_crafting_table.recipes": "Receptek", "container.endrealms.end_crafting_table.none": "Nincs recept",
                   "container.endrealms.end_crafting_table.hint": "Kattints: mutat, berak",
                   "advancements.endrealms.bone_drake.title": "Sárkányszelídítő", "advancements.endrealms.bone_drake.description": "Szelídítsd meg egy Sárkánytemető Csontsárkányát"}}
for lang, names in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(names)
    w(p, d)
print("ok")
