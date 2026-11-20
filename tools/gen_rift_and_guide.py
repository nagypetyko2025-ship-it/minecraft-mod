"""Rift Sanctum, Rift Lord loot, Riftblade and the Endwalker's Guide: art (original, CC0), data files, names and guide content.
The guide's recipes are read from the mod's real recipe files, so re-run this after changing one of them.
usage: python gen_rift_and_guide.py <resources dir> [preview.png]"""
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


# ---------------------------------------------------------------- item art
PAL = {"o": hexc("#1A1238"), "d": hexc("#35256B"), "p": hexc("#6A4FA8"), "l": hexc("#A58AE0"), "c": hexc("#7FE0E8"), "W": hexc("#EEFFFF"),
       "b": hexc("#5B3A22"), "B": hexc("#8A5F3A"), "s": hexc("#D9D2B4"), "t": hexc("#B4DCF2"), "m": hexc("#4C6EA8")}
ICONS = {
    "rift_heart": [
        "................",
        "...ooo....ooo...",
        "..oplpo..opppo..",
        ".oplllpooppcdpo.",
        ".oplWlpcpcWcddo.",
        ".opllpcWcWcdddo.",
        ".oppppcWWWcdddo.",
        "..opppdcWcddpo..",
        "...opppdcdddo...",
        "....opppddpo....",
        ".....opppdo.....",
        "......opdo......",
        ".......oo.......",
        "................",
        "................",
        "................",
    ],
    "riftblade": [
        "................",
        ".............oo.",
        "............oWo.",
        "...........oWco.",
        "..........oWcpo.",
        ".........oWcpo..",
        "........oWcpo...",
        ".......oWcpo....",
        "..oo..oWcpo.....",
        "..oco.oWcpo.....",
        "...oco.opo......",
        "....ocoo........",
        "....opoco.......",
        "...odo.oco......",
        "..odo...oo......",
        "..oo............",
    ],
    "end_guide": [
        "................",
        "..oooooooooooo..",
        ".odpppppppppppo.",
        ".odppplllllpppo.",
        ".odpplccccclppo.",
        ".odpplcWWWclppo.",
        ".odpplcWWWclppo.",
        ".odpplcccccclpo.",
        ".odppplllllpppo.",
        ".odpppppppppppo.",
        ".odsssssssssssso",
        ".odtttttttttttto",
        ".odsssssssssssso",
        ".oooooooooooooo.",
        "................",
        "................",
    ],
    "rift_lord_spawn_egg": [
        "................",
        "......oooo......",
        ".....oppppo.....",
        "....oppcpppo....",
        "....opcWcppo....",
        "...opppcppppo...",
        "...opppppcppo...",
        "..oppcppcWcppo..",
        "..opcWcppcpppo..",
        "..oppcppppppdo..",
        "..opppppcppddo..",
        "...oppppppddo...",
        "...oopppddoo....",
        ".....oooo.......",
        "................",
        "................",
    ],
}
sheet = []
for name, rows in ICONS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate((row + "................")[:16]):
            if ch in PAL:
                img.putpixel((x, y), PAL[ch])
    img.save(f"{TEX}/item/{name}.png")
    sheet.append(img)
    parent = "minecraft:item/handheld" if name == "riftblade" else "minecraft:item/generated"
    w(f"{A}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})

# ---------------------------------------------------------------- Rift Lord skin (64x64 humanoid layout)
rnd = random.Random(4471)
ROBE = [hexc(c) for c in ("#33256B", "#3B2B7A", "#443288")]
ACC, BRIGHT, WHITE = hexc("#7FE0E8"), hexc("#B4F0F2"), hexc("#EEFFFF")
skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def rect(img, x0, y0, x1, y1, colour):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), rnd.choice(colour) if isinstance(colour, list) else colour)


rect(skin, 0, 0, 32, 16, ROBE)            # head; the hat layer (32..64) stays transparent
rect(skin, 8, 8, 16, 16, hexc("#07040F"))  # shadow under the hood
for x in (10, 13):
    for img in (skin, glow):
        img.putpixel((x, 11), WHITE)
        img.putpixel((x, 12), ACC)
rect(skin, 0, 16, 56, 32, ROBE)           # legs, body, arms
for y in range(20, 32):                   # the rift through the chest
    x = 23 + (y % 3 == 0) + (y % 5 == 0)
    for img in (skin, glow):
        img.putpixel((x, y), BRIGHT)
        img.putpixel((x + 1, y), ACC)
for x in range(40, 56):                   # bands on the sleeves
    for y in (26, 29):
        skin.putpixel((x, y), hexc("#5B3FA0"))
for img in (skin, glow):
    rect(img, 0, 32, 4, 37, [BRIGHT, ACC, WHITE])    # crown spikes
    rect(img, 0, 40, 8, 48, [ACC, BRIGHT, hexc("#4C6EA8")])    # orbiting shards
rect(skin, 24, 34, 56, 50, ROBE)          # robe
for i in range(5):                        # cracks in the hem
    x, y = rnd.randrange(30, 54), 49
    for _ in range(rnd.randrange(3, 8)):
        for img in (skin, glow):
            img.putpixel((x, y), ACC)
        x = min(55, max(24, x + rnd.choice((-1, 0, 1))))
        y -= 1
os.makedirs(f"{TEX}/entity/rift_lord", exist_ok=True)
skin.save(f"{TEX}/entity/rift_lord/rift_lord.png")
glow.save(f"{TEX}/entity/rift_lord/rift_lord_glow.png")

# ---------------------------------------------------------------- worldgen, loot, recipes, advancements
w(f"{D}/worldgen/structure/rift_sanctum.json", {"type": "endrealms:rift_sanctum", "biomes": "#endrealms:has_structure/rift_sanctum", "step": "surface_structures", "spawn_overrides": {}})
w(f"{D}/worldgen/structure_set/rift_sanctums.json", {"placement": {"type": "minecraft:random_spread", "salt": 26410933, "separation": 24, "spacing": 56,
                                                                   "spread_type": "triangular", "locate_offset": [8, 0, 8]},
                                                     "structures": [{"structure": "endrealms:rift_sanctum", "weight": 1}]})
w(f"{D}/tags/worldgen/biome/has_structure/rift_sanctum.json", {"values": ["minecraft:end_highlands", "minecraft:end_midlands"]})


def item(name, weight, lo, hi):
    return {"type": "minecraft:item", "name": name, "weight": weight,
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}


w(f"{D}/loot_table/chests/rift_sanctum.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
        item(E + "phasite_shard", 6, 1, 3), item("minecraft:diamond", 10, 2, 5), item(E + "chorusite_shard", 12, 3, 8), item("minecraft:gold_ingot", 10, 4, 10),
        item(E + "chorusite_upgrade_smithing_template", 3, 1, 1), item("minecraft:enchanted_golden_apple", 2, 1, 1), item("minecraft:ender_pearl", 8, 2, 6)]}],
    "random_sequence": "endrealms:chests/rift_sanctum"})
w(f"{D}/loot_table/entities/rift_lord.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + "rift_heart"}]}],
                                              "random_sequence": "endrealms:entities/rift_lord"})
w(f"{D}/loot_table/gameplay/end_guide.json", {"type": "minecraft:advancement_reward", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + "end_guide"}]}],
                                              "random_sequence": "endrealms:gameplay/end_guide"})
w(f"{D}/recipe/end_guide.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ["minecraft:book", "minecraft:end_stone"], "result": {"id": E + "end_guide"}})
w(f"{D}/recipe/riftblade.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["GHG", " S "],
                                 "key": {"G": E + "phasite_gem", "H": E + "rift_heart", "S": E + "chorusite_sword"}, "result": {"id": E + "riftblade"}})


def display(key, icon, frame="task"):
    return {"icon": {"id": icon}, "title": {"translate": f"advancements.endrealms.{key}.title"},
            "description": {"translate": f"advancements.endrealms.{key}.description"}, "frame": frame}


def visited(structure):
    return {"trigger": "minecraft:location", "conditions": {"player": {"type": "minecraft:entity_properties", "entity": "this",
                                                                        "predicate": {"minecraft:location": {"structures": E + structure}}}}}


for key, icon, criterion in (("end_village", E + "end_villager_spawn_egg", visited("end_village")), ("void_bastion", E + "voidstone_bricks", visited("void_bastion")),
                             ("phasite", E + "phasite_shard", {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [E + "phasite_ore", E + "phasite_shard"]}]}})):
    w(f"{D}/advancement/{key}.json", {"parent": "endrealms:root", "display": display(key, icon), "criteria": {"done": criterion}, "requirements": [["done"]]})
w(f"{D}/advancement/rift_lord.json", {"parent": "endrealms:rift_sanctum", "display": display("rift_lord", E + "rift_heart", "challenge"),
                                      "criteria": {"killed": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": "minecraft:entity_properties", "entity": "this",
                                                                                                                                    "predicate": {"minecraft:entity_type": E + "rift_lord"}}}}},
                                      "requirements": [["killed"]]})
# no display: invisible, and granted once per player, also in worlds that already exist
w(f"{D}/advancement/guide_given.json", {"criteria": {"tick": {"trigger": "minecraft:tick"}}, "requirements": [["tick"]], "rewards": {"loot": [E + "gameplay/end_guide"]}})

A_ = "advancements.endrealms."


EN = {"entity.endrealms.rift_lord": "Rift Lord", "entity.endrealms.rift_lord.shield": "The Void Sentinels shield the Rift Lord: strike them down first",
      "entity.endrealms.rift_lord.enraged": "The Rift Lord tears the arena apart: stay close, or be dragged in",
      "item.endrealms.rift_lord_spawn_egg": "Rift Lord Spawn Egg", "item.endrealms.rift_heart": "Rift Heart", "item.endrealms.riftblade": "Riftblade",
      "item.endrealms.end_guide": "Endwalker's Guide", "structure.endrealms.rift_sanctum": "Rift Sanctum",
      f"{A_}end_village.title": "Foreign Market", f"{A_}end_village.description": "Find an End Village",
      f"{A_}void_bastion.title": "In the Tower's Shadow", f"{A_}void_bastion.description": "Enter a Void Bastion",
      f"{A_}phasite.title": "Out of Phase", f"{A_}phasite.description": "Obtain Phasite",
      f"{A_}rift_lord.title": "The Rift Closes", f"{A_}rift_lord.description": "Defeat the Rift Lord in its underground arena"}

HU = {"entity.endrealms.rift_lord": "Hasadékúr", "entity.endrealms.rift_lord.shield": "Az Űrőrzők védik a Hasadékurat: előbb őket győzd le",
      "entity.endrealms.rift_lord.enraged": "A Hasadékúr felszaggatja a küzdőteret: maradj közel, vagy magához ránt",
      "item.endrealms.rift_lord_spawn_egg": "Hasadékúr idéző tojás", "item.endrealms.rift_heart": "Hasadékszív", "item.endrealms.riftblade": "Hasadékpenge",
      "item.endrealms.end_guide": "Endjáró kalauz", "structure.endrealms.rift_sanctum": "Hasadékszentély",
      f"{A_}end_village.title": "Idegen piac", f"{A_}end_village.description": "Találj rá egy Endfalura",
      f"{A_}void_bastion.title": "A torony árnyékában", f"{A_}void_bastion.description": "Lépj be egy Űrbástyába",
      f"{A_}phasite.title": "Fázison kívül", f"{A_}phasite.description": "Szerezz fázisércet",
      f"{A_}rift_lord.title": "A hasadék bezárul", f"{A_}rift_lord.description": "Győzd le a Hasadékurat a föld alatti küzdőterén"}

for lang, vals in (("en_us", EN), ("hu_hu", HU)):
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)

if len(sys.argv) > 2:
    s = Image.new("RGBA", (len(sheet) * 136 + 8 + 264, 272), (45, 42, 66, 255))
    for i, im in enumerate(sheet):
        big = im.resize((128, 128), Image.NEAREST)
        s.paste(big, (8 + i * 136, 8), big)
    big = skin.resize((256, 256), Image.NEAREST)
    s.paste(big, (len(sheet) * 136 + 8, 8), big)
    s.save(sys.argv[2])
print("ok")
