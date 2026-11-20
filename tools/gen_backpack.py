"""The Backpack: art (original, CC0), item data, recipe and names.
usage: python gen_backpack.py <resources dir>"""
import json
import os
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
E = "endrealms:"
M = "minecraft:"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def hexc(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


ROWS = ["................", "......oooo......", ".....oLLLLo.....", ".....o....o.....", "...oooooooooo...", "..oLLLLLLLLDDo..", "..oLLLLLLLLDDo..", "..oMMMMggMMDDo..",
        "..oDDDDggDDDDo..", "..oLLLLLLLLDDo..", "..oLLoooooLDDo..", "..oLLoMMMoLDDo..", "..oLLoooooLDDo..", "..oLLLLLLLLDDo..", "...oooooooooo...", "................"]
PAL = {"o": hexc("#3B2415"), "L": hexc("#A5713F"), "M": hexc("#7E5329"), "D": hexc("#67431F"), "g": hexc("#E2B23C")}
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(ROWS):
    for x, ch in enumerate(row):
        if ch in PAL:
            img.putpixel((x, y), PAL[ch])
img.save(f"{A}/textures/item/backpack.png")
w(f"{A}/models/item/backpack.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "endrealms:item/backpack"}})
w(f"{A}/items/backpack.json", {"model": {"type": "minecraft:model", "model": "endrealms:item/backpack"}})
w(f"{D}/recipe/backpack.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "key": {"L": M + "leather", "S": M + "string"}, "pattern": [" S ", "L L", "LLL"], "result": {"id": E + "backpack"}})
for lang, name in (("en_us", "Backpack"), ("hu_hu", "Táska")):
    p = f"{A}/lang/{lang}.json"
    with open(p, encoding="utf-8") as f:
        d = json.load(f)
    d["item.endrealms.backpack"] = name
    w(p, d)
print("ok")
