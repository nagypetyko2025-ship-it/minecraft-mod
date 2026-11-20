"""Phase Anchor block: art (original, CC0, painted over the mod's polished voidstone), models, loot, recipe, tags, names.
usage: python gen_phase_anchor.py <resources dir> [preview.png]"""
import json
import os
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
TEX = f"{A}/textures/block"
NAME = "phase_anchor"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def hexc(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


DARK, DEEP, MID, LIGHT, WHITE = map(hexc, ("#1C2A52", "#3A5590", "#7FA8D6", "#B4DCF2", "#EEFFFF"))
BASE = Image.open(f"{TEX}/polished_voidstone.png").convert("RGBA")


def frame(img):
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), DARK)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        img.putpixel((x, y), DEEP)


side = BASE.copy()
frame(side)
for y in range(4, 12):             # upright glowing rune, widest in the middle
    half = min(y - 4, 11 - y, 2)
    for x in range(7 - half, 9 + half):
        side.putpixel((x, y), LIGHT if x in (7, 8) else MID)
    side.putpixel((6 - half, y), DARK)
    side.putpixel((9 + half, y), DARK)
side.save(f"{TEX}/{NAME}_side.png")


def top(core, inner):
    img = BASE.copy()
    frame(img)
    for y in range(16):
        for x in range(16):
            dist = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 4.6 <= dist < 5.7:
                img.putpixel((x, y), MID)
            elif 3.6 <= dist < 4.6:
                img.putpixel((x, y), DARK)
            elif 1.6 <= dist < 3.6:
                img.putpixel((x, y), inner)
            elif dist < 1.6:
                img.putpixel((x, y), core)
    return img


frames = [top(c, i) for c, i in ((MID, DEEP), (LIGHT, DEEP), (WHITE, MID), (WHITE, LIGHT))]
strip = Image.new("RGBA", (16, 16 * len(frames)))
for i, f in enumerate(frames):
    strip.paste(f, (0, 16 * i))
strip.save(f"{TEX}/{NAME}_top.png")
w(f"{TEX}/{NAME}_top.png.mcmeta", {"animation": {"frametime": 6, "interpolate": True, "frames": [0, 1, 2, 3, 2, 1]}})

w(f"{A}/blockstates/{NAME}.json", {"variants": {"": {"model": f"endrealms:block/{NAME}"}}})
w(f"{A}/models/block/{NAME}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "bottom": "endrealms:block/polished_voidstone", "side": f"endrealms:block/{NAME}_side", "top": f"endrealms:block/{NAME}_top"}})
w(f"{A}/items/{NAME}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:block/{NAME}"}})

with open(f"{D}/loot_table/blocks/prism_lamp.json", encoding="utf-8") as f:
    loot = f.read().replace("prism_lamp", NAME)
with open(f"{D}/loot_table/blocks/{NAME}.json", "w", encoding="utf-8") as f:
    f.write(loot)

w(f"{D}/recipe/{NAME}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["VRV", "RPR", "VVV"],
                              "key": {"V": "endrealms:polished_voidstone", "R": "endrealms:prism_shard", "P": ["endrealms:phasite_ore", "endrealms:phasite_shard"]},
                              "result": {"id": f"endrealms:{NAME}"}})

for tag in ("blocks_motion_no_leaves", "mineable/pickaxe"):
    p = f"{RES}/data/minecraft/tags/block/{tag}.json"
    t = r(p)
    if f"endrealms:{NAME}" not in t["values"]:
        t["values"].append(f"endrealms:{NAME}")
    w(p, t)

for lang, val in (("en_us", "Phase Anchor"), ("hu_hu", "Fázishorgony")):
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d[f"block.endrealms.{NAME}"] = val
    w(p, d)

if len(sys.argv) > 2:
    s = Image.new("RGBA", (5 * 136 + 8, 144), (45, 42, 66, 255))
    for i, im in enumerate([side] + frames):
        big = im.resize((128, 128), Image.NEAREST)
        s.paste(big, (8 + i * 136, 8), big)
    s.save(sys.argv[2])
print("ok")
