"""Phasite Gem (armor trim material) and Phase Pearl: art (original, CC0), recipes, trim data and names.
usage: python gen_phasite_items.py <resources dir> [preview.png]"""
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


PAL = {"o": hexc("#1C2A52"), "D": hexc("#3A5590"), "M": hexc("#7FA8D6"), "L": hexc("#B4DCF2"), "W": hexc("#EEFFFF"), "V": hexc("#6A4FA8"), "v": hexc("#4A3580")}
ICONS = {
    "phasite_gem": [
        "................",
        "................",
        ".....oooooo.....",
        "....oWLLLMDo....",
        "...oWLLLMMMDo...",
        "..oLLLLMMMMDDo..",
        "..ooooooooooooo.",
        "..oLWLMMVMMDDo..",
        "...oLLMVVVMDo...",
        "....oLMVVMDo....",
        ".....oMMVDo.....",
        "......oMDo......",
        ".......oo.......",
        "................",
        "................",
        "................",
    ],
    "phase_pearl": [
        "................",
        ".....oooooo.....",
        "....oLLMMMDo....",
        "...oLWWLMMMDo...",
        "..oLWWLLVMMDDo..",
        "..oLWLLVvVMDDo..",
        "..oLLLVvvvVDDo..",
        "..oMLLVvWvVDDo..",
        "..oMLLVvvvVDDo..",
        "..oMMLLVvVMDDo..",
        "..oMMMLLVMMDDo..",
        "...oDMMMMMDDo...",
        "....oDDDDDDo....",
        ".....oooooo.....",
        "................",
        "................",
    ],
}
sheet = []
for name, rows in ICONS.items():
    rows = [(row + "................")[:16] for row in rows]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in PAL:
                img.putpixel((x, y), PAL[ch])
    img.save(f"{TEX}/item/{name}.png")
    sheet.append(img)
    w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})

# the game recolors the trim pattern through this 8-step palette, brightest first
palette = Image.new("RGBA", (8, 1))
for x, c in enumerate(("#EEFFFF", "#B4DCF2", "#7FA8D6", "#5F8AC4", "#4C6EA8", "#3A5590", "#2B3F73", "#1C2A52")):
    palette.putpixel((x, 0), hexc(c))
os.makedirs(f"{TEX}/palettes/trim", exist_ok=True)
palette.save(f"{TEX}/palettes/trim/phasite.png")
w(f"{D}/trim_material/phasite.json", {"description": {"color": "#B4DCF2", "translate": "trim_material.endrealms.phasite"}, "palette_id": "endrealms:trim/phasite"})

p = f"{RES}/data/minecraft/tags/item/trim_materials.json"
t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
if "endrealms:phasite_gem" not in t["values"]:
    t["values"].append("endrealms:phasite_gem")
w(p, t)

PHASITE = ["endrealms:phasite_ore", "endrealms:phasite_shard"]
w(f"{D}/recipe/phasite_gem.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                   "ingredients": [PHASITE, "endrealms:prism_shard", "endrealms:prism_shard", "minecraft:popped_chorus_fruit"],
                                   "result": {"id": "endrealms:phasite_gem"}})
w(f"{D}/recipe/phase_pearl.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["PRP", "CEC", "PRP"],
                                   "key": {"P": PHASITE, "R": "endrealms:prism_shard", "C": "minecraft:popped_chorus_fruit", "E": "minecraft:ender_eye"},
                                   "result": {"id": "endrealms:phase_pearl"}})

K = "item.endrealms.phase_pearl"
NAMES = {"en_us": {"item.endrealms.phasite_gem": "Phasite Gem", K: "Phase Pearl", "trim_material.endrealms.phasite": "Phasite Material",
                   f"{K}.unmarked": "Right-click a Phase Anchor to bind it", f"{K}.target": "Bound anchor: %s %s %s (%s)", f"{K}.charges": "Charges: %s / %s",
                   f"{K}.marked": "Bound to this Phase Anchor", f"{K}.no_target": "Right-click a Phase Anchor with it first", f"{K}.anchor_missing": "The bound Phase Anchor is gone: bind it to another one",
                   f"{K}.empty": "Out of charges: recharge it on an anvil with phasite shards", f"{K}.blocked": "There is no room on top of the Phase Anchor",
                   f"{K}.dimension.overworld": "Overworld", f"{K}.dimension.the_nether": "Nether", f"{K}.dimension.the_end": "End"},
         "hu_hu": {"item.endrealms.phasite_gem": "Fázisékkő", K: "Fázisgyöngy", "trim_material.endrealms.phasite": "Fázisanyag",
                   f"{K}.unmarked": "Jobb klikkelj vele egy Fázishorgonyra", f"{K}.target": "Kötött horgony: %s %s %s (%s)", f"{K}.charges": "Töltés: %s / %s",
                   f"{K}.marked": "Hozzákötve ehhez a Fázishorgonyhoz", f"{K}.no_target": "Előbb klikkelj vele egy Fázishorgonyra", f"{K}.anchor_missing": "A kötött Fázishorgony eltűnt: köss a gyöngyöt egy másikhoz",
                   f"{K}.empty": "Elfogyott a töltés: üllőn, fázisszilánkkal töltheted újra", f"{K}.blocked": "Nincs elég hely a Fázishorgony tetején",
                   f"{K}.dimension.overworld": "Felszín", f"{K}.dimension.the_nether": "Nether", f"{K}.dimension.the_end": "End"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.pop(f"{K}.need_ground", None)
    d.update(vals)
    w(p, d)

if len(sys.argv) > 2:
    s = Image.new("RGBA", (2 * 136 + 8, 144), (45, 42, 66, 255))
    for i, im in enumerate(sheet):
        big = im.resize((128, 128), Image.NEAREST)
        s.paste(big, (8 + i * 136, 8), big)
    s.save(sys.argv[2])
print("ok")
