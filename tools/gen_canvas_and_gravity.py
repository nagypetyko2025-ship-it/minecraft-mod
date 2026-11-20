"""Canvas, Large Canvas and Gravity Stone: art (original, CC0), block and item data, recipes, tags, advancement and names.
usage: python gen_canvas_and_gravity.py <resources dir> [preview.png]"""
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


rnd = random.Random(8821)
LINEN = [hexc(c) for c in ("#EFE8D8", "#E9E1CF", "#F3EDDF", "#E4DCC8")]
WOOD = [hexc(c) for c in ("#8A5F3A", "#7C5433", "#93663F")]
WOOD_DARK = hexc("#5B3A22")


def linen(border):
    """border: which edges get the wooden frame, from 'ltrb'."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = (x == 0 and "l" in border) or (x == 15 and "r" in border) or (y == 0 and "t" in border) or (y == 15 and "b" in border)
            img.putpixel((x, y), rnd.choice(WOOD) if edge else rnd.choice(LINEN))
    return img


fronts = {"canvas": linen("ltrb"), "large_canvas_left": linen("ltb"), "large_canvas_right": linen("rtb")}
for name, img in fronts.items():
    img.save(f"{TEX}/block/{name}.png")
frame = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        frame.putpixel((x, y), WOOD_DARK if (x + y * 3) % 7 == 0 else rnd.choice(WOOD))
frame.save(f"{TEX}/block/canvas_frame.png")

FACES = ("north", "south", "east", "west", "up", "down")


def panel(front):
    faces = {}
    for f in FACES:
        tex = "#front" if f == "north" else "#frame"
        uv = [0, 0, 16, 16] if f in ("north", "south") else [0, 0, 1, 16] if f in ("east", "west") else [0, 15, 16, 16]
        faces[f] = {"uv": uv, "texture": tex}
    return {"parent": "minecraft:block/block", "textures": {"particle": f"endrealms:block/{front}", "front": f"endrealms:block/{front}", "frame": "endrealms:block/canvas_frame"},
            "elements": [{"from": [0, 0, 15], "to": [16, 16, 16], "faces": faces}]}


ROT = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
for name in fronts:
    w(f"{A}/models/block/{name}.json", panel(name))
w(f"{A}/blockstates/canvas.json", {"variants": {f"facing={f}": {"model": "endrealms:block/canvas", "y": y} for f, y in ROT}})
w(f"{A}/blockstates/large_canvas.json", {"variants": {f"extension={'true' if ext else 'false'},facing={f}": {"model": f"endrealms:block/large_canvas_{'right' if ext else 'left'}", "y": y}
                                                      for f, y in ROT for ext in (False, True)}})

ICONS = {"canvas": ["................", "................", "..bbbbbbbbbbbb..", "..bLLLLLLLLLLb..", "..bLLRRLLLLLLb..", "..bLRRRLLLYYLb..", "..bLLRLLLLYYLb..", "..bLLLLLLLLLLb..",
                    "..bLLLLGGLLLLb..", "..bLGGGGGGGLLb..", "..bLLLLLLLLLLb..", "..bbbbbbbbbbbb..", "......b..b......", ".....b....b.....", "................", "................"],
         "large_canvas": ["................", "................", "bbbbbbbbbbbbbbbb", "bLLLLLLLLLLLLLLb", "bLRRLLLLLLLYYLLb", "bRRRRLLLLLLYYLLb", "bLRRLLLLLLLLLLLb", "bLLLLLLLBBLLLLLb",
                          "bLLLLLBBBBBLLLLb", "bGGGGGGGGGGGGGGb", "bLLLLLLLLLLLLLLb", "bbbbbbbbbbbbbbbb", "...b........b...", "..b..........b..", "................", "................"]}
PAL = {"b": hexc("#5B3A22"), "L": hexc("#EFE8D8"), "R": hexc("#C8322E"), "Y": hexc("#F2C23C"), "G": hexc("#4E9A3A"), "B": hexc("#3A6AC8")}
for name, rows in ICONS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in PAL:
                img.putpixel((x, y), PAL[ch])
    img.save(f"{TEX}/item/{name}.png")
    w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})

# ---------------------------------------------------------------- gravity stone
STONE = [hexc(c) for c in ("#1E1A33", "#231E3C", "#1A162C")]
RING_OFF, RING_ON, CORE_ON = hexc("#4A3A78"), hexc("#7FE0E8"), hexc("#E8FFFB")
for lit in (False, True):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            ring = d in (6.5, 3.5) or (d < 1.0)
            if x in (0, 15) or y in (0, 15):
                c = hexc("#120E20")
            elif ring:
                c = (CORE_ON if d < 1.0 else RING_ON) if lit else RING_OFF
            else:
                c = rnd.choice(STONE)
            img.putpixel((x, y), c)
    name = "gravity_stone_on" if lit else "gravity_stone"
    img.save(f"{TEX}/block/{name}.png")
    w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"endrealms:block/{name}"}})
w(f"{A}/blockstates/gravity_stone.json", {"variants": {"powered=false": {"model": "endrealms:block/gravity_stone"}, "powered=true": {"model": "endrealms:block/gravity_stone_on"}}})
w(f"{A}/items/gravity_stone.json", {"model": {"type": "minecraft:model", "model": "endrealms:block/gravity_stone"}})
w(f"{D}/loot_table/blocks/gravity_stone.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + "gravity_stone"}], "condition": {"type": "minecraft:survives_explosion"}}],
                                                "random_sequence": "endrealms:blocks/gravity_stone"})

# ---------------------------------------------------------------- recipes, tags, advancement, names
w(f"{D}/recipe/canvas.json", {"type": "minecraft:crafting_shaped", "category": "misc", "key": {"#": M + "white_wool"}, "pattern": ["###", "###", "###"], "result": {"id": E + "canvas"}})
w(f"{D}/recipe/large_canvas.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [E + "canvas", E + "canvas"], "result": {"id": E + "large_canvas"}})
w(f"{D}/recipe/gravity_stone.json", {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": ["PDP", "DCD", "PDP"],
                                     "key": {"P": E + "phasite_shard", "D": E + "deepglow_shard", "C": E + "crystalline_voidstone"}, "result": {"id": E + "gravity_stone"}})


def add_tag(path, values):
    p = f"{RES}/data/minecraft/tags/{path}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    t["values"] += [v for v in values if v not in t["values"]]
    w(p, t)


add_tag("block/mineable/axe", [E + "canvas", E + "large_canvas"])
add_tag("block/mineable/pickaxe", [E + "gravity_stone"])
add_tag("block/blocks_motion_no_leaves", [E + "gravity_stone"])
w(f"{D}/advancement/gravity_stone.json", {"parent": "endrealms:phase_anchor", "display": {"icon": {"id": E + "gravity_stone"}, "title": {"translate": "advancements.endrealms.gravity_stone.title"},
                                                                                         "description": {"translate": "advancements.endrealms.gravity_stone.description"}, "frame": "goal"},
                                          "criteria": {"has": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [E + "gravity_stone"]}]}}}, "requirements": [["has"]]})
T = "advancements.endrealms."
NAMES = {"en_us": {"block.endrealms.canvas": "Canvas", "block.endrealms.large_canvas": "Large Canvas", "block.endrealms.gravity_stone": "Gravity Stone",
                   f"{T}gravity_stone.title": "Feather Fall", f"{T}gravity_stone.description": "Craft a Gravity Stone"},
         "hu_hu": {"block.endrealms.canvas": "Vászon", "block.endrealms.large_canvas": "Nagy vászon", "block.endrealms.gravity_stone": "Gravitációs kő",
                   f"{T}gravity_stone.title": "Pehelykönnyű", f"{T}gravity_stone.description": "Készíts Gravitációs követ"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
if len(sys.argv) > 2:
    imgs = list(fronts.values()) + [Image.open(f"{TEX}/block/gravity_stone.png"), Image.open(f"{TEX}/block/gravity_stone_on.png"), Image.open(f"{TEX}/item/canvas.png"), Image.open(f"{TEX}/item/large_canvas.png")]
    sheet = Image.new("RGBA", (8 + len(imgs) * 104, 112), (24, 18, 40, 255))
    for i, im in enumerate(imgs):
        big = im.convert("RGBA").resize((96, 96), Image.NEAREST)
        sheet.paste(big, (8 + i * 104, 8), big)
    sheet.save(sys.argv[2])
print("ok")
