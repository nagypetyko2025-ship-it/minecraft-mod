"""Starreed and Moonbell: textures (original pixel art, CC0), models, loot, world generation and names.
usage: python gen_astral_plants.py <resources dir> [preview.png]"""
import json
import os
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
TEX = f"{A}/textures"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def hexc(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


PAL = {"s": hexc("#5B3F8C"), "S": hexc("#7E5FB5"), "d": hexc("#3A2763"), "l": hexc("#9B7FD0"), "t": hexc("#46C9C6"),
       "B": hexc("#D6FFF7"), "b": hexc("#8BEFE2"), "c": hexc("#2F9FA5"), "o": hexc("#22163F"), "W": hexc("#FFFFFF"), "p": hexc("#F2A8EE")}
GLOWING = set("BbW")


def art(rows):
    img, glow = Image.new("RGBA", (16, 16), (0, 0, 0, 0)), Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in PAL:
                img.putpixel((x, y), PAL[ch])
                if ch in GLOWING:
                    glow.putpixel((x, y), PAL[ch])
    return img, glow


TEXTURES = {
    "starreed_stalk": [
        "...Ss.....Ss....",
        "...Ss....bSs....",
        "...SsB...BSs....",
        "...Ssb....dd....",
        "...dd.....Ss....",
        "..bSs.....Ss....",
        "..BSs.....SsB...",
        "...Ss.....Ssb...",
        "...Ss.....dd....",
        "...SsB....Ss....",
        "...Ssb...bSs....",
        "...dd....BSs....",
        "...Ss.....Ss....",
        "..bSs.....Ss....",
        "..BSs.....SsB...",
        "...Ss.....Ssb...",
    ],
    "starreed_top": [
        "................",
        "...b......B.....",
        "...B......b.....",
        "...Sl....lS.....",
        "..lSs....Ss.....",
        "..tSs....Sst....",
        "...Ss.....Ss....",
        "...SsB....Ss....",
        "...Ssb...bSs....",
        "...dd....BSs....",
        "...Ss.....Ss....",
        "..bSs.....Ss....",
        "..BSs.....SsB...",
        "...Ss.....Ssb...",
        "...dd.....dd....",
        "...Ss.....Ss....",
    ],
    "moonbell_bottom": [
        ".......Ss.......",
        ".......Ss.......",
        ".......Ss.......",
        ".......Ss..l....",
        ".......Ss.lS....",
        ".......SsSs.....",
        ".......Ssd......",
        "...l...Ss.......",
        "...Sl..Ss.......",
        "....sSSss.......",
        ".....dSs........",
        ".......Ss.......",
        ".......Ss.......",
        "......SSsl......",
        ".....lSsdSl.....",
        "....lS.Ss.Sl....",
    ],
    "moonbell_middle": [
        "..........Ss....",
        "..........Ss....",
        ".........SSs....",
        ".........Ss.....",
        ".........Ss.....",
        ".....l..SSs.....",
        ".....Sl.Ss......",
        "......sSSs......",
        ".......dSs......",
        "........Ss......",
        ".......SSs......",
        ".......Ss.......",
        ".......Ss.......",
        ".......Ss.......",
        ".......Ss.......",
        ".......Ss.......",
    ],
    "moonbell_top": [
        "................",
        "....SSSSSSSs....",
        "...SSssssssSs...",
        "..Ss.......SSs..",
        "..Ss..S....Ss...",
        "..d...s....Ss...",
        ".oBo..d...SSs...",
        "oBWbo.S...Ss....",
        "oBbbo.s...Ss....",
        "bBbbcoBo..Ss....",
        ".c.c.oWbo.Ss....",
        ".....oBbc.Ss....",
        ".....bbbc.Ss....",
        "......c.c.Ss....",
        "..........Ss....",
        "..........Ss....",
    ],
}
ITEMS = {
    "starreed": [
        "................",
        ".......b........",
        ".......B........",
        "......lS........",
        "......Ss........",
        "......SsB.......",
        "......Ssb.......",
        "......dd........",
        ".....bSs........",
        ".....BSs........",
        "......Ss........",
        "......SsB.......",
        "......Ssb.......",
        "......dd........",
        "......Ss........",
        "................",
    ],
    "moonbell": [
        "................",
        "......SSSSs.....",
        ".....Ss...Ss....",
        ".....Ss...Ss....",
        "....oBo...Ss....",
        "...oBWbo..Ss....",
        "...oBbbo..Ss....",
        "..bBbbbco.Ss....",
        "...c...c..Ss....",
        "..........Ss....",
        ".....l...SSs....",
        ".....Sl..Ss.....",
        "......sSSs......",
        ".......dSs......",
        ".......Ss.......",
        "................",
    ],
    "starberry": [
        "................",
        "................",
        ".......lS.......",
        "......lSs.......",
        ".....oobbo......",
        "....obBWBbo.....",
        "...obBWWBbco....",
        "...obBWBbbco....",
        "...obBBbbbco....",
        "...obbbbbcco....",
        "....obbbcco.....",
        ".....occco......",
        "......ooo.......",
        "................",
        "................",
        "................",
    ],
    "moonbell_nectar": [
        "................",
        "......SSs.......",
        ".......Ss.......",
        "......oddo......",
        ".....oBWbo......",
        ".....oBWbo......",
        "....oBWbbco.....",
        "....oBWbbco.....",
        "...oBbbbbbco....",
        "...oBbpbbbco....",
        "...obbppbbco....",
        "...obbbpbcco....",
        "....ocbbbco.....",
        ".....ooooo......",
        "................",
        "................",
    ],
}
sheet_items = []
os.makedirs(f"{TEX}/block", exist_ok=True)
for name, rows in TEXTURES.items():
    assert len(rows) == 16 and all(len(x) == 16 for x in rows), name
    img, glow = art(rows)
    img.save(f"{TEX}/block/{name}.png")
    glow.save(f"{TEX}/block/{name}_glow.png")
    sheet_items.append(img)
    w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cross_emissive",
                                        "textures": {"cross": f"endrealms:block/{name}", "cross_emissive": f"endrealms:block/{name}_glow"}})
for name, rows in ITEMS.items():
    assert len(rows) == 16 and all(len(x) == 16 for x in rows), name
    img, _ = art(rows)
    img.save(f"{TEX}/item/{name}.png")
    sheet_items.append(img)
    w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})

w(f"{A}/blockstates/starreed.json", {"variants": {"top=false": {"model": "endrealms:block/starreed_stalk"}, "top=true": {"model": "endrealms:block/starreed_top"}}})
w(f"{A}/blockstates/moonbell.json", {"variants": {"part=0": {"model": "endrealms:block/moonbell_bottom"}, "part=1": {"model": "endrealms:block/moonbell_middle"},
                                                  "part=2": {"model": "endrealms:block/moonbell_top"}}})


def pool(item, lo, hi, chance=None):
    p = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": item, "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}]}
    if chance:
        p["condition"] = {"type": "minecraft:random_chance", "chance": chance}
    return p


w(f"{D}/loot_table/blocks/starreed.json", {"type": "minecraft:block", "pools": [pool("endrealms:starreed", 1, 1), pool("endrealms:starberry", 1, 2, 0.6)],
                                           "random_sequence": "endrealms:blocks/starreed"})
# only the part the player actually breaks produces loot; the rest of the plant vanishes without any
w(f"{D}/loot_table/blocks/moonbell.json", {"type": "minecraft:block", "pools": [pool("endrealms:moonbell_nectar", 1, 2), pool("endrealms:moonbell", 1, 1, 0.33)],
                                           "random_sequence": "endrealms:blocks/moonbell"})

GROUND = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}]
for name, chance in (("starreed", 3), ("moonbell", 2)):
    w(f"{D}/worldgen/feature/{name}_patch.json", {"type": f"endrealms:{name}_patch"})
    w(f"{D}/worldgen/placed_feature/{name}_patch.json", {"feature": f"endrealms:{name}_patch", "placement": [{"type": "minecraft:rarity_filter", "chance": chance}] + GROUND})

p = f"{D}/worldgen/biome/astral_forest.json"
b = r(p)
for name in ("endrealms:starreed_patch", "endrealms:moonbell_patch"):
    if name not in b["features"][9]:
        b["features"][9].append(name)
w(p, b)

NAMES = {"en_us": {"block.endrealms.starreed": "Starreed", "block.endrealms.moonbell": "Moonbell", "item.endrealms.starberry": "Starberry", "item.endrealms.moonbell_nectar": "Moonbell Nectar"},
         "hu_hu": {"block.endrealms.starreed": "Csillagnád", "block.endrealms.moonbell": "Holdharang", "item.endrealms.starberry": "Csillagbogyó", "item.endrealms.moonbell_nectar": "Harangnektár"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)

if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (9 * 104 + 8, 112), (45, 42, 66, 255))
    for i, im in enumerate(sheet_items):
        big = im.resize((96, 96), Image.NEAREST)
        sheet.paste(big, (8 + i * 104, 8), big)
    sheet.save(sys.argv[2])
print("ok")
