"""Crypt, Volcanic Vent and Ash Meal: art (original, CC0), data files, loot, advancements and names.
usage: python gen_crypt_and_vent.py <resources dir>"""
import json
import os
import random
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
TEX = f"{A}/textures"
E = "endrealms:"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def hexc(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


rnd = random.Random(1187)
CHAR = [hexc(c) for c in ("#2A2530", "#322B3A", "#3A3344")]
EMBER = [hexc(c) for c in ("#5B1E8C", "#9A33D6", "#D66BF2", "#FFD0FF")]
side = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        side.putpixel((x, y), rnd.choice(CHAR))
for _ in range(14):
    side.putpixel((rnd.randrange(16), rnd.randrange(8)), rnd.choice(EMBER[:3]))
side.save(f"{TEX}/block/volcanic_vent_side.png")
FRAMES = 8
strip = Image.new("RGBA", (16, 16 * FRAMES))
for f in range(FRAMES):
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.5:
                c = EMBER[3 if (f + x + y) % 3 == 0 else 2]
            elif d < 4.5:
                c = EMBER[2 if (f + x * y) % 4 == 0 else 1]
            elif d < 6.0:
                c = EMBER[0]
            else:
                c = rnd.choice(CHAR)
            strip.putpixel((x, y + 16 * f), c)
strip.save(f"{TEX}/block/volcanic_vent_top.png")
w(f"{TEX}/block/volcanic_vent_top.png.mcmeta", {"animation": {"frametime": 3, "interpolate": True}})

MEAL = [
    "................",
    "................",
    "................",
    "......ooo.......",
    ".....oLWLo......",
    "....oLMLWLoo....",
    "...oLMDMLLWLo...",
    "..oLMDPDMLWLLo..",
    "..oMDPPDMMLLMo..",
    ".oMDPPDDMMMMDDo.",
    ".oDDDDDDDDDDDDo.",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
    "................",
]
PAL = {"o": hexc("#3E3844"), "W": hexc("#F4F0F6"), "L": hexc("#C8C2CC"), "M": hexc("#A8A2AD"), "D": hexc("#8E8894"), "P": EMBER[1]}
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(MEAL):
    for x, ch in enumerate(row):
        if ch in PAL:
            img.putpixel((x, y), PAL[ch])
img.save(f"{TEX}/item/ash_meal.png")
w(f"{A}/models/item/ash_meal.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "endrealms:item/ash_meal"}})
w(f"{A}/items/ash_meal.json", {"model": {"type": "minecraft:model", "model": "endrealms:item/ash_meal"}})

V = "volcanic_vent"
w(f"{A}/blockstates/{V}.json", {"variants": {"erupting=false": {"model": f"endrealms:block/{V}"}, "erupting=true": {"model": f"endrealms:block/{V}"}}})
w(f"{A}/models/block/{V}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {"bottom": f"endrealms:block/{V}_side", "side": f"endrealms:block/{V}_side", "top": f"endrealms:block/{V}_top"}})
w(f"{A}/items/{V}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:block/{V}"}})
w(f"{D}/loot_table/blocks/{V}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + V}], "condition": {"type": "minecraft:survives_explosion"}}],
                                      "random_sequence": f"endrealms:blocks/{V}"})
for tag in ("mineable/pickaxe", "blocks_motion_no_leaves"):
    p = f"{RES}/data/minecraft/tags/block/{tag}.json"
    t = r(p)
    if E + V not in t["values"]:
        t["values"].append(E + V)
    w(p, t)

w(f"{D}/recipe/{V}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ESE", "RMR", "ERE"],
                           "key": {"E": E + "ember_rock", "S": E + "scorched_end_stone", "R": "minecraft:blaze_powder", "M": "minecraft:magma_block"}, "result": {"id": E + V}})
w(f"{D}/recipe/ash_meal.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [E + "ash_dust"] * 9, "result": {"id": E + "ash_meal"}})

w(f"{D}/worldgen/structure/crypt.json", {"type": "endrealms:crypt", "biomes": "#endrealms:has_structure/crypt", "step": "surface_structures", "spawn_overrides": {}})
w(f"{D}/worldgen/structure_set/crypts.json", {"placement": {"type": "minecraft:random_spread", "salt": 48120977, "separation": 2, "spacing": 6, "spread_type": "triangular", "locate_offset": [8, 0, 8]},
                                              "structures": [{"structure": "endrealms:crypt", "weight": 1}]})
w(f"{D}/tags/worldgen/biome/has_structure/crypt.json", {"values": [E + "scorched_barrens"]})


def item(name, weight, lo, hi):
    return {"type": "minecraft:item", "name": name, "weight": weight, "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}


w(f"{D}/loot_table/chests/crypt.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
    item(E + "ash_dust", 12, 3, 9), item("minecraft:bone", 12, 3, 8), item("minecraft:coal", 10, 3, 8), item("minecraft:gold_ingot", 7, 1, 4), item("minecraft:quartz", 10, 3, 7),
    item(E + "ember_rock", 6, 1, 4), item(E + "ash_meal", 5, 1, 2), item(E + "chorusite_shard", 4, 1, 2), item("minecraft:ender_pearl", 5, 1, 2)]}], "random_sequence": "endrealms:chests/crypt"})
w(f"{D}/loot_table/chests/crypt_treasure.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
    item("minecraft:diamond", 10, 1, 4), item(E + "chorusite_shard", 12, 2, 5), item(E + "phasite_shard", 4, 1, 2), item(E + "ash_quartz", 3, 1, 1), item(E + "ember_garnet", 2, 1, 1),
    item(E + "ash_meal", 8, 2, 4), item("minecraft:gold_block", 3, 1, 1), item("minecraft:golden_apple", 5, 1, 2), item(E + V, 3, 1, 2)]}], "random_sequence": "endrealms:chests/crypt_treasure"})

for key, icon, parent, criterion in (
        ("crypt", E + "volcanic_vent", E + "root", {"trigger": "minecraft:location", "conditions": {"player": {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:location": {"structures": E + "crypt"}}}}}),
        ("ash_meal", E + "ash_meal", E + "ash_wraith", {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": E + "ash_meal"}]}})):
    w(f"{D}/advancement/{key}.json", {"parent": parent, "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.endrealms.{key}.title"},
                                                                    "description": {"translate": f"advancements.endrealms.{key}.description"}, "frame": "task"},
                                      "criteria": {"done": criterion}, "requirements": [["done"]]})
T = "advancements.endrealms."
NAMES = {"en_us": {f"block.endrealms.{V}": "Volcanic Vent", "item.endrealms.ash_meal": "Ash Meal", "structure.endrealms.crypt": "Crypt",
                   f"{T}crypt.title": "Under the Ash", f"{T}crypt.description": "Enter a Crypt", f"{T}ash_meal.title": "From Ashes", f"{T}ash_meal.description": "Make Ash Meal"},
         "hu_hu": {f"block.endrealms.{V}": "Vulkánblokk", "item.endrealms.ash_meal": "Hamuliszt", "structure.endrealms.crypt": "Kripta",
                   f"{T}crypt.title": "A hamu alatt", f"{T}crypt.description": "Lépj be egy Kriptába", f"{T}ash_meal.title": "Hamvaiból", f"{T}ash_meal.description": "Készíts Hamulisztet"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
print("ok")
