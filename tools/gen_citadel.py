"""Void Citadel and Void King: art (original, CC0), worldgen files, loot, advancements and names.
usage: python gen_citadel.py <resources dir>"""
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


def hexc(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


# ---------------------------------------------------------------- Void King skin: same UV layout as the Rift Lord's
rnd = random.Random(90517)
ROBE = [hexc(c) for c in ("#15101F", "#1B1428", "#221932")]
GOLD, BRIGHT, WHITE, EMBER = hexc("#E2B23C"), hexc("#FFE08A"), hexc("#FFF6D8"), hexc("#E0553A")
skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def rect(img, x0, y0, x1, y1, colour):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), rnd.choice(colour) if isinstance(colour, list) else colour)


rect(skin, 0, 0, 32, 16, ROBE)
rect(skin, 8, 8, 16, 16, hexc("#050308"))
for x in range(8, 16):                    # gold circlet across the brow
    skin.putpixel((x, 8), GOLD)
for x in (10, 13):
    for img in (skin, glow):
        img.putpixel((x, 11), WHITE)
        img.putpixel((x, 12), EMBER)
rect(skin, 0, 16, 56, 32, ROBE)
for y in range(20, 32):                   # the burning seam down the chest
    x = 23 + (y % 3 == 0) + (y % 5 == 0)
    for img in (skin, glow):
        img.putpixel((x, y), BRIGHT)
        img.putpixel((x + 1, y), EMBER)
for x in range(40, 56):
    for y in (26, 29):
        skin.putpixel((x, y), GOLD)
for x in range(20, 28):
    skin.putpixel((x, 20), GOLD)
    skin.putpixel((x, 31), GOLD)
for img in (skin, glow):
    rect(img, 0, 32, 4, 37, [BRIGHT, GOLD, WHITE])
    rect(img, 0, 40, 8, 48, [GOLD, BRIGHT, EMBER])
rect(skin, 24, 34, 56, 50, ROBE)
for x in range(24, 56):
    skin.putpixel((x, 49), GOLD)
for _ in range(5):
    x, y = rnd.randrange(30, 54), 48
    for _ in range(rnd.randrange(3, 8)):
        for img in (skin, glow):
            img.putpixel((x, y), EMBER)
        x = min(55, max(24, x + rnd.choice((-1, 0, 1))))
        y -= 1
os.makedirs(f"{TEX}/entity/void_king", exist_ok=True)
skin.save(f"{TEX}/entity/void_king/void_king.png")
glow.save(f"{TEX}/entity/void_king/void_king_glow.png")

EGG = ["................", "......oooo......", ".....oddddo.....", "....oddgddo.....", "....odgWgddo....", "...odddgddddo...", "...oddddddgdo...", "..oddgddgWgddo..",
       "..odgWgddgdddo..", "..oddgddddddso..", "..odddddgddsso..", "...oddddddsso...", "...oodddssoo....", ".....oooo.......", "................", "................"]
PAL = {"o": hexc("#0B0812"), "d": hexc("#241A36"), "s": hexc("#16101F"), "g": GOLD, "W": WHITE}
egg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(EGG):
    for x, ch in enumerate((row + "................")[:16]):
        if ch in PAL:
            egg.putpixel((x, y), PAL[ch])
egg.save(f"{TEX}/item/void_king_spawn_egg.png")
w(f"{A}/models/item/void_king_spawn_egg.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "endrealms:item/void_king_spawn_egg"}})
w(f"{A}/items/void_king_spawn_egg.json", {"model": {"type": "minecraft:model", "model": "endrealms:item/void_king_spawn_egg"}})

# ---------------------------------------------------------------- worldgen
w(f"{D}/worldgen/structure/void_citadel.json", {"type": "endrealms:void_citadel", "biomes": "#endrealms:has_structure/void_citadel", "step": "surface_structures", "spawn_overrides": {}})
w(f"{D}/worldgen/structure_set/void_citadels.json", {"placement": {"type": "minecraft:random_spread", "salt": 73815529, "separation": 32, "spacing": 72, "spread_type": "triangular", "locate_offset": [8, 0, 8]},
                                                     "structures": [{"structure": "endrealms:void_citadel", "weight": 1}]})
w(f"{D}/tags/worldgen/biome/has_structure/void_citadel.json", {"values": [M + "end_highlands", M + "end_midlands", M + "end_barrens", M + "small_end_islands"]})


# ---------------------------------------------------------------- loot; chorusite ingots stay out of chests on purpose
def item(name, weight, lo=1, hi=1):
    entry = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1:
        entry["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return entry


GEMS = ("ember_garnet", "star_sapphire", "void_onyx", "moon_opal", "ash_quartz", "bloodstone", "deep_aquamarine")


def table(name, kind, pools):
    w(f"{D}/loot_table/{name}.json", {"type": f"minecraft:{kind}", "pools": pools, "random_sequence": f"endrealms:{name}"})


table("chests/void_citadel", "chest", [{"rolls": {"type": "minecraft:uniform", "min": 5, "max": 8}, "entries": [
    item(M + "diamond", 10, 2, 6), item(M + "gold_ingot", 10, 6, 14), item(M + "ender_pearl", 8, 3, 8), item(M + "golden_apple", 8, 2, 5), item(M + "experience_bottle", 8, 4, 12),
    item(M + "netherite_scrap", 4, 1, 2), item(E + "phasite_shard", 5, 1, 2), item(E + "prism_shard", 8, 6, 16), item(E + "chorusite_shard", 3, 1, 2),
    item(E + "chorusite_upgrade_smithing_template", 1), *[item(E + g, 2) for g in GEMS]]}])
table("chests/void_citadel_treasure", "chest", [{"rolls": {"type": "minecraft:uniform", "min": 6, "max": 9}, "entries": [
    item(M + "diamond_block", 8, 1, 3), item(M + "netherite_ingot", 6, 1, 2), item(M + "enchanted_golden_apple", 5, 1, 2), item(E + "phasite_shard", 10, 2, 5), item(E + "chorusite_shard", 8, 2, 5),
    item(E + "chorusite_upgrade_smithing_template", 4), item(M + "experience_bottle", 6, 16, 32), *[item(E + g, 4) for g in GEMS]]}])
table("chests/void_king", "chest", [
    {"rolls": 1, "entries": [item(M + "netherite_ingot", 1, 3, 5)]},
    {"rolls": 1, "entries": [item(E + "crownstone", 1)]},
    {"rolls": {"type": "minecraft:uniform", "min": 5, "max": 7}, "entries": [
        item(M + "diamond_block", 8, 2, 4), item(M + "enchanted_golden_apple", 6, 2, 3), item(E + "phasite_shard", 10, 4, 8), item(E + "chorusite_shard", 8, 3, 6), item(E + "chorusite_upgrade_smithing_template", 5),
        *[item(E + g, 5) for g in GEMS]]}])
table("entities/void_king", "entity", [{"rolls": 1, "entries": [item(E + "phasite_shard", 1, 4, 8)]}, {"rolls": 1, "entries": [item(M + "netherite_ingot", 1, 2, 2)]}, {"rolls": 1, "entries": [item(E + "crownstone", 1)]}])


# ---------------------------------------------------------------- advancements and names
def display(key, icon, frame):
    return {"icon": {"id": icon}, "title": {"translate": f"advancements.endrealms.{key}.title"}, "description": {"translate": f"advancements.endrealms.{key}.description"}, "frame": frame}


w(f"{D}/advancement/void_citadel.json", {"parent": "endrealms:rift_lord", "display": display("void_citadel", E + "voidstone_bricks", "task"),
                                         "criteria": {"done": {"trigger": "minecraft:location", "conditions": {"player": {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:location": {"structures": E + "void_citadel"}}}}}},
                                         "requirements": [["done"]]})
w(f"{D}/advancement/void_king.json", {"parent": "endrealms:void_citadel", "display": display("void_king", E + "void_king_spawn_egg", "challenge"),
                                      "criteria": {"killed": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": E + "void_king"}}}}},
                                      "requirements": [["killed"]]})
w(f"{D}/advancement/crownstone.json", {"parent": "endrealms:void_king", "display": display("crownstone", E + "crownstone", "challenge"),
                                       "criteria": {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [E + "crownstone"]}]}}},
                                       "requirements": [["done"]]})

T = "advancements.endrealms."
NAMES = {"en_us": {"entity.endrealms.void_king": "Void King", "entity.endrealms.void_king.shield": "The Void Sentinels shield the Void King: strike them down first",
                   "entity.endrealms.void_king.enraged": "The Void King drags his court to him: stay close, or be pulled in", "item.endrealms.void_king_spawn_egg": "Void King Spawn Egg",
                   "structure.endrealms.void_citadel": "Void Citadel", f"{T}void_citadel.title": "Castle in the Void", f"{T}void_citadel.description": "Set foot on a Void Citadel's island",
                   f"{T}void_king.title": "Regicide", f"{T}void_king.description": "Defeat the Void King in his throne room",
                   f"{T}crownstone.title": "Heavy Is the Crown", f"{T}crownstone.description": "Obtain a Crownstone from the Void King"},
         "hu_hu": {"entity.endrealms.void_king": "Űrkirály", "entity.endrealms.void_king.shield": "Az Űrőrzők védik az Űrkirályt: előbb őket győzd le",
                   "entity.endrealms.void_king.enraged": "Az Űrkirály mindenkit magához ránt: maradj közel", "item.endrealms.void_king_spawn_egg": "Űrkirály idéző tojás",
                   "structure.endrealms.void_citadel": "Fellegvár", f"{T}void_citadel.title": "Vár a semmiben", f"{T}void_citadel.description": "Lépj egy Fellegvár szigetére",
                   f"{T}void_king.title": "Trónfosztás", f"{T}void_king.description": "Győzd le az Űrkirályt a tróntermében",
                   f"{T}crownstone.title": "A korona súlya", f"{T}crownstone.description": "Szerezz Koronakövet az Űrkirálytól"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
print("ok")
