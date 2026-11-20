"""Phase Mailbox: art (original, CC0), block data, recipe, tags, advancement and names.
usage: python gen_phase_mailbox.py <resources dir> [preview.png]"""
import json
import os
import random
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
TEX = f"{A}/textures/block"
E = "endrealms:"
M = "minecraft:"
NAME = "phase_mailbox"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def hexc(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


rnd = random.Random(5521)
STONE = [hexc(c) for c in ("#2B2140", "#30254A", "#262038")]
EDGE, TRIM, GLOW, GLOW_HI, DARK = hexc("#16101F"), hexc("#6A4FA8"), hexc("#7FE0E8"), hexc("#D8FFF4"), hexc("#0B0812")


def base():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), EDGE if x in (0, 15) or y in (0, 15) else TRIM if x in (1, 14) or y in (1, 14) else rnd.choice(STONE))
    return img


side = base()
for y in (5, 10):
    for x in range(2, 14):
        side.putpixel((x, y), EDGE)
side.save(f"{TEX}/{NAME}_side.png")

front = base()
for x in range(3, 13):                     # the letter slot, glowing from within
    for y in range(5, 8):
        front.putpixel((x, y), DARK if y != 6 else GLOW)
    front.putpixel((x, 6), GLOW_HI if 6 <= x <= 9 else GLOW)
for x, y in ((7, 10), (8, 10), (6, 11), (9, 11), (7, 12), (8, 12)):   # a small phase rune under it
    front.putpixel((x, y), GLOW)
front.save(f"{TEX}/{NAME}_front.png")

top = base()
for i in range(4, 12):
    top.putpixel((i, i), TRIM)
    top.putpixel((15 - i, i), TRIM)
for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
    top.putpixel((x, y), GLOW_HI)
top.save(f"{TEX}/{NAME}_top.png")

w(f"{A}/models/block/{NAME}.json", {"parent": "minecraft:block/orientable", "textures": {"front": f"endrealms:block/{NAME}_front", "side": f"endrealms:block/{NAME}_side", "top": f"endrealms:block/{NAME}_top"}})
w(f"{A}/blockstates/{NAME}.json", {"variants": {f"facing={f}": ({"model": f"endrealms:block/{NAME}", "y": y} if y else {"model": f"endrealms:block/{NAME}"})
                                                for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
w(f"{A}/items/{NAME}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:block/{NAME}"}})
w(f"{D}/loot_table/blocks/{NAME}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + NAME}], "condition": {"type": "minecraft:survives_explosion"}}],
                                         "random_sequence": f"endrealms:blocks/{NAME}"})
w(f"{D}/recipe/{NAME}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PDP", "EAE", "VVV"],
                              "key": {"P": E + "phasite_shard", "D": M + "diamond", "E": M + "ender_eye", "A": E + "phase_anchor", "V": E + "polished_voidstone"}, "result": {"id": E + NAME}})
for tag in ("block/mineable/pickaxe", "block/blocks_motion_no_leaves"):
    p = f"{RES}/data/minecraft/tags/{tag}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    if E + NAME not in t["values"]:
        t["values"].append(E + NAME)
    w(p, t)
w(f"{D}/advancement/{NAME}.json", {"parent": "endrealms:phase_anchor", "display": {"icon": {"id": E + NAME}, "title": {"translate": f"advancements.endrealms.{NAME}.title"},
                                                                                  "description": {"translate": f"advancements.endrealms.{NAME}.description"}, "frame": "goal"},
                                   "criteria": {"has": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [E + NAME]}]}}}, "requirements": [["has"]]})
T, K = "advancements.endrealms.", "mail.endrealms."
NAMES = {"en_us": {f"block.endrealms.{NAME}": "Phase Mailbox", "container.endrealms.phase_mailbox": "Phase Mailbox", f"{T}{NAME}.title": "Return to Sender", f"{T}{NAME}.description": "Craft a Phase Mailbox",
                   f"{K}default_name": "%s's Mailbox", f"{K}name": "Name", f"{K}recipients": "Mailboxes (%s)", f"{K}pick": "Pick a recipient above", f"{K}send": "Send",
                   f"{K}not_owner": "Only its owner can rename it", f"{K}bad_name": "Names are 1-24 characters", f"{K}name_taken": "That name is taken", f"{K}renamed": "Renamed",
                   f"{K}nothing": "Put something in the send slot", f"{K}no_such": "That mailbox no longer exists", f"{K}self": "That is this mailbox", f"{K}full": "That mailbox is full",
                   f"{K}sent": "Sent!", f"{K}partly": "Only part fit, the rest stays here", f"{K}arrived": "Phase Mail: %s sent %s x %s to %s"},
         "hu_hu": {f"block.endrealms.{NAME}": "Fázisposta", "container.endrealms.phase_mailbox": "Fázisposta", f"{T}{NAME}.title": "Feladó: én", f"{T}{NAME}.description": "Készíts Fázispostát",
                   f"{K}default_name": "%s postája", f"{K}name": "Név", f"{K}recipients": "Posták (%s)", f"{K}pick": "Válassz címzettet fent", f"{K}send": "Küldés",
                   f"{K}not_owner": "Csak a tulajdonosa nevezheti át", f"{K}bad_name": "A név 1-24 karakter legyen", f"{K}name_taken": "Ez a név már foglalt", f"{K}renamed": "Átnevezve",
                   f"{K}nothing": "Tegyél tárgyat a küldőhelyre", f"{K}no_such": "Ilyen posta már nincs", f"{K}self": "Ez maga ez a posta", f"{K}full": "A címzett postája tele van",
                   f"{K}sent": "Elküldve!", f"{K}partly": "Csak egy része fért el, a többi itt maradt", f"{K}arrived": "Fázisposta: %s küldött %s db %s tárgyat ide: %s"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (8 + 3 * 136, 144), (24, 18, 40, 255))
    for i, im in enumerate((front, side, top)):
        sheet.paste(im.resize((128, 128), Image.NEAREST), (8 + i * 136, 8))
    sheet.save(sys.argv[2])
print("ok")
