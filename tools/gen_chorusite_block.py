"""Block of Chorusite: art (original, CC0) with an animated glowing overlay, block data, recipes, tags, advancement and names.
usage: python gen_chorusite_block.py <resources dir> [preview.png]"""
import json
import math
import os
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
TEX = f"{A}/textures/block"
E = "endrealms:"
M = "minecraft:"
NAME = "chorusite_block"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def hexc(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


OUTLINE, HIGH, SHADOW = hexc("#2A0F38"), hexc("#B94DC6"), hexc("#3A1248")
BANDS = [hexc("#EE86E1"), hexc("#DD69D3"), hexc("#A642BB"), hexc("#7D2F99"), hexc("#5A2172")]
FACET, RIVET, RIVET_SHADE = hexc("#C25AD0"), hexc("#FCBEF1"), hexc("#7D2F99")
RIVETS = [(2, 2), (13, 2), (2, 13), (13, 13)]

# ---------------------------------------------------------------- base: a bevelled frame around a cut gem face
base = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        d = abs(x - 7.5) + abs(y - 7.5)
        if x in (0, 15) or y in (0, 15):
            c = OUTLINE
        elif x == 1 or y == 1:
            c = HIGH
        elif x == 14 or y == 14:
            c = SHADOW
        elif x == y or x + y == 15:
            c = FACET
        else:
            c = BANDS[min(4, int(d / 2.0))]
        base.putpixel((x, y), c)
for x, y in RIVETS:
    base.putpixel((x, y), RIVET)
    base.putpixel((x + 1 if x < 8 else x - 1, y + 1 if y < 8 else y - 1), RIVET_SHADE)
base.save(f"{TEX}/{NAME}.png")

# ---------------------------------------------------------------- glow: veins pulsing outward from the core, shown full bright
FRAMES = 16
LOW, MID, HOT = hexc("#A642BB"), hexc("#EE86E1"), hexc("#FFF0FC")
lit = set()
for i in range(3, 13):
    lit.add((i, i))
    lit.add((i, 15 - i))
for i in (7, 8):
    for j in range(4, 12):
        if abs(j - 7.5) > 1.5:
            lit.add((i, j))
            lit.add((j, i))
lit |= {(7, 7), (8, 7), (7, 8), (8, 8)}
lit |= set(RIVETS)
strip = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
for f in range(FRAMES):
    phase = f / FRAMES * 2 * math.pi
    for x, y in lit:
        if (x, y) in RIVETS:
            t = 0.5 + 0.5 * math.sin(phase * 2 + RIVETS.index((x, y)) * 1.7)
            c = mix(MID, HOT, t)
        elif 7 <= x <= 8 and 7 <= y <= 8:
            c = HOT
        else:
            d = math.hypot(x - 7.5, y - 7.5)
            t = 0.5 + 0.5 * math.sin(phase - d * 0.9)
            c = mix(LOW, MID, t * 2) if t < 0.5 else mix(MID, HOT, (t - 0.5) * 2)
        strip.putpixel((x, y + 16 * f), c)
strip.save(f"{TEX}/{NAME}_glow.png")
w(f"{TEX}/{NAME}_glow.png.mcmeta", {"animation": {"frametime": 2, "interpolate": True}})

FACES = ("north", "south", "east", "west", "up", "down")
w(f"{A}/models/block/{NAME}.json", {
    "parent": "minecraft:block/block",
    "textures": {"particle": f"endrealms:block/{NAME}", "all": f"endrealms:block/{NAME}", "glow": f"endrealms:block/{NAME}_glow"},
    "elements": [
        {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#all", "cullface": f} for f in FACES}},
        {"from": [0, 0, 0], "to": [16, 16, 16], "shade": False, "light_emission": 15,
         "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#glow", "cullface": f} for f in FACES}}]})
w(f"{A}/blockstates/{NAME}.json", {"variants": {"": {"model": f"endrealms:block/{NAME}"}}})
w(f"{A}/items/{NAME}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:block/{NAME}"}})
w(f"{D}/loot_table/blocks/{NAME}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + NAME}], "condition": {"type": "minecraft:survives_explosion"}}],
                                         "random_sequence": f"endrealms:blocks/{NAME}"})
w(f"{D}/recipe/{NAME}.json", {"type": "minecraft:crafting_shaped", "category": "building", "key": {"#": E + "chorusite_ingot"}, "pattern": ["###", "###", "###"], "result": {"id": E + NAME}})
w(f"{D}/recipe/chorusite_ingot_from_block.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [E + NAME], "result": {"id": E + "chorusite_ingot", "count": 9}})


def add_tag(path, values):
    p = f"{RES}/data/minecraft/tags/{path}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    t["values"] += [v for v in values if v not in t["values"]]
    w(p, t)


for tag in ("block/mineable/pickaxe", "block/needs_diamond_tool", "block/beacon_base_blocks", "block/blocks_motion_no_leaves"):
    add_tag(tag, [E + NAME])
add_tag("item/beacon_payment_items", [E + "chorusite_ingot"])

w(f"{D}/advancement/{NAME}.json", {"parent": "endrealms:chorusite_armor", "display": {"icon": {"id": E + NAME}, "title": {"translate": f"advancements.endrealms.{NAME}.title"},
                                                                                    "description": {"translate": f"advancements.endrealms.{NAME}.description"}, "frame": "challenge"},
                                   "criteria": {"has": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [E + NAME]}]}}}, "requirements": [["has"]]})
T = "advancements.endrealms."
NAMES = {"en_us": {f"block.endrealms.{NAME}": "Block of Chorusite", f"{T}{NAME}.title": "Monument of the End", f"{T}{NAME}.description": "Craft a Block of Chorusite from nine ingots"},
         "hu_hu": {f"block.endrealms.{NAME}": "Chorusit-tömb", f"{T}{NAME}.title": "Az End emlékműve", f"{T}{NAME}.description": "Készíts Chorusit-tömböt kilenc rúdból"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)

if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (8 + 5 * 136, 144), (24, 18, 40, 255))
    for i, f in enumerate((0, 4, 8, 12)):
        img = base.copy()
        img.alpha_composite(strip.crop((0, 16 * f, 16, 16 * f + 16)))
        big = img.resize((128, 128), Image.NEAREST)
        sheet.paste(big, (8 + i * 136, 8))
    big = base.resize((128, 128), Image.NEAREST)
    sheet.paste(big, (8 + 4 * 136, 8))
    sheet.save(sys.argv[2])
print("ok")
