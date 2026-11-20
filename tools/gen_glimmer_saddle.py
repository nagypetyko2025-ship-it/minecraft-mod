"""Saddled Glimmerhoof: saddle overlay art (original, CC0) on the body's UV, saddle tag, advancement and names.
usage: python gen_glimmer_saddle.py <resources dir> [preview.png]"""
import json
import os
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
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


LEATHER, LIGHT, DARK, GOLD, CLOTH, CLOTH_EDGE = map(hexc, ("#6B3A1F", "#8A4E2A", "#4A2614", "#E2B23C", "#5A2172", "#A642BB"))
img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
# body box at (0, 0), 8 wide x 6 high x 12 deep: top face u 12..20 v 0..12, sides v 12..18 (u 0..12 and 20..32), bottom u 20..28 v 0..12
for v in range(2, 10):
    for u in range(12, 20):
        edge = v in (2, 9)
        if edge:
            c = GOLD
        elif u in (12, 19):
            c = CLOTH_EDGE
        elif u in (13, 18):
            c = CLOTH
        else:
            c = LIGHT if v in (3, 4) else LEATHER if v < 8 else DARK
        img.putpixel((u, v), c)
for start in (0, 20):                       # blanket hanging down both flanks, with a strap
    for u in range(start + 2, start + 10):
        for v in range(12, 15):
            img.putpixel((u, v), CLOTH_EDGE if v == 14 else CLOTH)
        img.putpixel((u, 12), GOLD)
    for v in range(12, 18):
        for u in (start + 5, start + 6):
            img.putpixel((u, v), DARK)
    img.putpixel((start + 5, 15), GOLD)
for u in range(20, 28):                      # girth strap under the belly
    for v in (5, 6):
        img.putpixel((u, v), DARK)
img.save(f"{A}/textures/entity/glimmerhoof/glimmerhoof_saddle.png")

p = f"{RES}/data/minecraft/tags/entity_type/can_equip_saddle.json"
t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
if E + "glimmerhoof" not in t["values"]:
    t["values"].append(E + "glimmerhoof")
w(p, t)

w(f"{D}/advancement/glimmerhoof_ride.json", {"parent": "endrealms:root", "display": {"icon": {"id": "minecraft:saddle"}, "title": {"translate": "advancements.endrealms.glimmerhoof_ride.title"},
                                                                                    "description": {"translate": "advancements.endrealms.glimmerhoof_ride.description"}, "frame": "task"},
                                             "criteria": {"rode": {"trigger": "minecraft:started_riding", "conditions": {"player": {"type": "minecraft:entity_properties", "entity": "this",
                                                                                                                                       "predicate": {"minecraft:vehicle": {"minecraft:entity_type": E + "glimmerhoof"}}}}}},
                                             "requirements": [["rode"]]})
T = "advancements.endrealms."
for lang, vals in (("en_us", {f"{T}glimmerhoof_ride.title": "Leap of Faith", f"{T}glimmerhoof_ride.description": "Ride a saddled Glimmerhoof"}),
                   ("hu_hu", {f"{T}glimmerhoof_ride.title": "Hitugrás", f"{T}glimmerhoof_ride.description": "Lovagolj meg egy felnyergelt Csillámpatát"})):
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
if len(sys.argv) > 2:
    img.resize((512, 256), Image.NEAREST).save(sys.argv[2])
print("ok")
