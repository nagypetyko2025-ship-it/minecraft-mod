"""Astral Basin depths: Deepglow Ore, Deep Lurker, art (original, CC0; the ore is painted over the mod's void sand), data and names.
usage: python gen_basin_depths.py <client jar> <resources dir>"""
import json
import os
import random
import sys
import zipfile

from PIL import Image

jar, RES = zipfile.ZipFile(sys.argv[1]), sys.argv[2]
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


rnd = random.Random(3307)
GLOW = [hexc(c) for c in ("#3BC8C0", "#7FF0E0", "#D8FFF4")]
ore = Image.open(f"{TEX}/block/void_sand.png").convert("RGBA")
for _ in range(5):
    x, y = rnd.randrange(1, 14), rnd.randrange(1, 14)
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        if rnd.random() < 0.85:
            ore.putpixel((x + dx, y + dy), rnd.choice(GLOW))
ore.save(f"{TEX}/block/deepglow_ore.png")

PAL = {"o": hexc("#0E3A44"), "d": hexc("#1F7A80"), "m": GLOW[0], "l": GLOW[1], "W": GLOW[2], "p": hexc("#16323C"), "q": hexc("#245864")}
ICONS = {
    "deepglow_shard": [
        "................",
        ".........oo.....",
        "........olWo....",
        ".......olWlo....",
        "......olWlmo....",
        ".....olllmdo....",
        "....olllmdo.....",
        "...ollmmdo......",
        "..olmmddoo......",
        "..ommddo.oo.....",
        "..oomdo.olmo....",
        "...oodo.omdo....",
        ".....oo..oo.....",
        "................",
        "................",
        "................",
    ],
    "deep_lurker_spawn_egg": [
        "................",
        "......oooo......",
        ".....oppppo.....",
        "....oppqpppo....",
        "....opqWqppo....",
        "...opppqppppo...",
        "...oppppqpppo...",
        "..oppqppmWqppo..",
        "..opqWqppqpppo..",
        "..oppqpppppppo..",
        "..opppppqppppo..",
        "...oppppppppo...",
        "...oopppppoo....",
        ".....oooo.......",
        "................",
        "................",
    ],
}
for name, rows in ICONS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate((row + "................")[:16]):
            if ch in PAL:
                img.putpixel((x, y), PAL[ch])
    img.save(f"{TEX}/item/{name}.png")
    w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})

BODY = [hexc(c) for c in ("#102A34", "#16323C", "#1C3C48", "#245864")]
skin = Image.new("RGBA", (64, 64))
glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
for y in range(64):
    for x in range(64):
        skin.putpixel((x, y), rnd.choice(BODY))
for x in (8, 12):                         # eyes on the front of the head
    for img in (skin, glow):
        img.putpixel((x, 9), GLOW[2])
        img.putpixel((x, 10), GLOW[1])
for x in range(36, 52):                   # teeth along the jaw's top edge
    if x % 2 == 0:
        skin.putpixel((x, 5), hexc("#E8F4E8"))
for img in (skin, glow):
    for x0, y0, x1, y1 in ((54, 8, 62, 12),):      # the lure's bulb
        for y in range(y0, y1):
            for x in range(x0, x1):
                img.putpixel((x, y), rnd.choice(GLOW))
    for x, y0, y1 in ((10, 0, 7), (11, 14, 22), (10, 28, 36), (34, 28, 34)):    # the line along the back
        for y in range(y0, y1):
            img.putpixel((x, y), GLOW[0])
for y in range(14, 29):                   # translucent tail fin
    for x in range(44, 58):
        skin.putpixel((x, y), hexc("#1F7A80"))
os.makedirs(f"{TEX}/entity/deep_lurker", exist_ok=True)
skin.save(f"{TEX}/entity/deep_lurker/deep_lurker.png")
glow.save(f"{TEX}/entity/deep_lurker/deep_lurker_glow.png")

NAME = "deepglow_ore"
w(f"{A}/blockstates/{NAME}.json", {"variants": {"": {"model": f"endrealms:block/{NAME}"}}})
w(f"{A}/models/block/{NAME}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"endrealms:block/{NAME}"}})
w(f"{A}/items/{NAME}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:block/{NAME}"}})
loot = jar.read("data/minecraft/loot_table/blocks/diamond_ore.json").decode("utf-8")
loot = loot.replace("minecraft:diamond_ore", E + NAME).replace("minecraft:diamond", E + "deepglow_shard").replace("minecraft:blocks/diamond_ore", f"endrealms:blocks/{NAME}")
w(f"{D}/loot_table/blocks/{NAME}.json", json.loads(loot))
w(f"{D}/loot_table/entities/deep_lurker.json", {"type": "minecraft:entity", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + "raw_void_eel", "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]}]},
    {"rolls": 1, "condition": {"type": "minecraft:random_chance", "chance": 0.35}, "entries": [{"type": "minecraft:item", "name": E + "deepglow_shard"}]}],
    "random_sequence": "endrealms:entities/deep_lurker"})

for tag, values in (("block/mineable/pickaxe", [E + NAME]), ("block/needs_iron_tool", [E + NAME]), ("block/blocks_motion_no_leaves", [E + NAME])):
    p = f"{RES}/data/minecraft/tags/{tag}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    t["values"] += [v for v in values if v not in t["values"]]
    w(p, t)

w(f"{D}/worldgen/feature/deepglow_patch.json", {"type": "endrealms:deepglow_patch"})
w(f"{D}/worldgen/placed_feature/deepglow_patch.json", {"feature": "endrealms:deepglow_patch", "placement": [
    {"type": "minecraft:count", "count": 5}, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}]})
biome_path = f"{D}/worldgen/biome/astral_basin.json"
biome = r(biome_path)
if E + "deepglow_patch" not in biome["features"][9]:
    biome["features"][9].insert(0, E + "deepglow_patch")
    w(biome_path, biome)

T = "advancements.endrealms."
NAMES = {"en_us": {f"block.endrealms.{NAME}": "Deepglow Ore", "item.endrealms.deepglow_shard": "Deepglow Shard", "entity.endrealms.deep_lurker": "Deep Lurker", "item.endrealms.deep_lurker_spawn_egg": "Deep Lurker Spawn Egg",
                   f"{T}deep_lurker.title": "Something Below", f"{T}deep_lurker.description": "Defeat a Deep Lurker", f"{T}deepglow.title": "Light of the Deep", f"{T}deepglow.description": "Obtain a Deepglow Shard"},
         "hu_hu": {f"block.endrealms.{NAME}": "Mélyfény-érc", "item.endrealms.deepglow_shard": "Mélyfény-szilánk", "entity.endrealms.deep_lurker": "Mélyleső", "item.endrealms.deep_lurker_spawn_egg": "Mélyleső idéző tojás",
                   f"{T}deep_lurker.title": "Valami odalent", f"{T}deep_lurker.description": "Győzz le egy Mélylesőt", f"{T}deepglow.title": "A mélység fénye", f"{T}deepglow.description": "Szerezz Mélyfény-szilánkot"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
print("ok")
