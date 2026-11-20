"""Phasite ore assets (flickering overlay + shard) and the "End Realms" advancement tab. Original art, CC0.
ADD EVERY NEW BIOME TO `BIOMES` below, then re-run: the "every biome" advancement is generated from that list.
usage: python gen_phasite_and_advancements.py <resources dir>"""
import json
import math
import os
import random
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
TEX = f"{A}/textures"

BIOMES = ["astral_forest", "astral_basin", "prism_wastes", "scorched_barrens", "dragon_graveyard"]


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


# ---------------------------------------------------------------- textures
rnd = random.Random(909)
FRAMES = 12
PAL = [(0x4C, 0x6E, 0xA8), (0x7F, 0xA8, 0xD6), (0xB4, 0xDC, 0xF2), (0xEE, 0xFF, 0xFF)]
veins = []
for _ in range(6):                          # short diagonal slivers, each flickering on its own
    x, y = rnd.randrange(1, 14), rnd.randrange(1, 14)
    phase, speed = rnd.random() * math.tau, rnd.choice((1, 2, 3))
    for _ in range(rnd.randrange(3, 6)):
        veins.append((x, y, phase, speed))
        x = min(15, max(0, x + rnd.choice((-1, 0, 1))))
        y = min(15, y + 1)
strip = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
for f in range(FRAMES):
    for x, y, phase, speed in veins:
        t = 0.5 + 0.5 * math.sin(phase + speed * f / FRAMES * math.tau)
        strip.putpixel((x, y + 16 * f), PAL[min(3, int(t * 4))] + (255,))
os.makedirs(f"{TEX}/block", exist_ok=True)
strip.save(f"{TEX}/block/phasite_ore_veins.png")
with open(f"{TEX}/block/phasite_ore_veins.png.mcmeta", "w", encoding="utf-8") as f:
    json.dump({"animation": {"frametime": 3, "interpolate": True}}, f)

SHARD = [
    "................",
    "........oo......",
    ".......oWLo.....",
    "......oWLMo.....",
    ".....oWLMMo.....",
    "....oLLMMDo.....",
    "....oLMMDo......",
    "...oLMMDDoo.....",
    "...oLMDDooLo....",
    "..oLMMDo.oLMo...",
    "..oMMDo..oMDo...",
    "..oMDDo...oo....",
    "...oDo..........",
    "....o...........",
    "................",
    "................",
]
SP = {"o": (0x24, 0x33, 0x5C, 255), "W": (0xEE, 0xFF, 0xFF, 255), "L": PAL[2] + (255,), "M": PAL[1] + (255,), "D": PAL[0] + (255,)}
shard = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(SHARD):
    for x, ch in enumerate(row):
        if ch in SP:
            shard.putpixel((x, y), SP[ch])
shard.save(f"{TEX}/item/phasite_shard.png")

# ---------------------------------------------------------------- block, item, loot, worldgen
SIDES = ("down", "up", "north", "south", "west", "east")


def cube(texture, **extra):
    return dict(extra, **{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {s: {"texture": texture, "cullface": s} for s in SIDES}})


w(f"{A}/blockstates/phasite_ore.json", {"variants": {"": {"model": "endrealms:block/phasite_ore"}}})
w(f"{A}/models/block/phasite_ore.json", {"parent": "minecraft:block/block",
                                         "textures": {"particle": "endrealms:block/voidstone", "base": "endrealms:block/voidstone", "veins": "endrealms:block/phasite_ore_veins"},
                                         "elements": [cube("#base"), cube("#veins", light_emission=15, shade=False)]})
w(f"{A}/items/phasite_ore.json", {"model": {"type": "minecraft:model", "model": "endrealms:block/phasite_ore"}})
w(f"{A}/models/item/phasite_shard.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "endrealms:item/phasite_shard"}})
w(f"{A}/items/phasite_shard.json", {"model": {"type": "minecraft:model", "model": "endrealms:item/phasite_shard"}})
w(f"{D}/loot_table/blocks/phasite_ore.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
    {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": "endrealms:phasite_ore"},
    {"type": "minecraft:item", "name": "endrealms:phasite_shard", "modifier": [
        {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"}, {"type": "minecraft:explosion_decay"}]}]}]}],
    "random_sequence": "endrealms:blocks/phasite_ore"})
w(f"{D}/worldgen/feature/phasite_vein.json", {"type": "endrealms:phasite_vein"})
w(f"{D}/worldgen/placed_feature/phasite_vein.json", {"feature": "endrealms:phasite_vein", "placement": [
    {"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})


def add_tag(path, value):
    p = f"{RES}/data/minecraft/tags/block/{path}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    if value not in t["values"]:
        t["values"].append(value)
    w(p, t)


for tag in ("mineable/pickaxe", "needs_diamond_tool", "blocks_motion_no_leaves"):
    add_tag(tag, "endrealms:phasite_ore")

# ---------------------------------------------------------------- advancements


def display(key, icon, frame="task", **extra):
    return dict({"icon": {"id": icon}, "title": {"translate": f"advancements.endrealms.{key}.title"},
                 "description": {"translate": f"advancements.endrealms.{key}.description"}, "frame": frame}, **extra)


# The tab must be there from the start, so its root is granted on the first tick, silently.
w(f"{D}/advancement/root.json", {"display": display("root", "endrealms:chorusite_ore", background="minecraft:gui/advancements/backgrounds/end", show_toast=False, announce_to_chat=False),
                                 "criteria": {"tick": {"trigger": "minecraft:tick"}}, "requirements": [["tick"]]})
w(f"{D}/advancement/chorusite_ingot.json", {"parent": "endrealms:root", "display": display("chorusite_ingot", "endrealms:chorusite_ingot"),
                                            "criteria": {"has_ingot": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "endrealms:chorusite_ingot"}]}}},
                                            "requirements": [["has_ingot"]]})
ARMOR = [f"endrealms:chorusite_{p}" for p in ("helmet", "chestplate", "leggings", "boots")]
w(f"{D}/advancement/chorusite_pickaxe.json", {"parent": "endrealms:chorusite_ingot", "display": display("chorusite_pickaxe", "endrealms:chorusite_pickaxe"),
                                              "criteria": {"has_pickaxe": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "endrealms:chorusite_pickaxe"}]}}},
                                              "requirements": [["has_pickaxe"]]})
w(f"{D}/advancement/chorusite_armor.json", {"parent": "endrealms:chorusite_ingot", "display": display("chorusite_armor", "endrealms:chorusite_chestplate", frame="goal"),
                                            "criteria": {"full_set": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": a} for a in ARMOR]}}},
                                            "requirements": [["full_set"]]})
w(f"{D}/advancement/all_biomes.json", {"parent": "endrealms:root", "display": display("all_biomes", "endrealms:astral_sapling", frame="challenge"),
                                       "criteria": {b: {"trigger": "minecraft:location", "conditions": {"player": {"type": "minecraft:entity_properties", "entity": "this",
                                                        "predicate": {"minecraft:location": {"biomes": f"endrealms:{b}"}}}}} for b in BIOMES},
                                       "requirements": [[b] for b in BIOMES]})

NAMES = {"en_us": {"block.endrealms.phasite_ore": "Phasite Ore", "item.endrealms.phasite_shard": "Phasite Shard",
                   "advancements.endrealms.root.title": "End Realms", "advancements.endrealms.root.description": "Beyond the dragon, a whole new End awaits",
                   "advancements.endrealms.chorusite_ingot.title": "Age of Chorusite", "advancements.endrealms.chorusite_ingot.description": "Obtain your first Chorusite Ingot",
                   "advancements.endrealms.chorusite_pickaxe.title": "Sharper Than Netherite", "advancements.endrealms.chorusite_pickaxe.description": "Obtain a Chorusite Pickaxe",
                   "advancements.endrealms.chorusite_armor.title": "Clad in the End", "advancements.endrealms.chorusite_armor.description": "Obtain a full set of Chorusite armor",
                   "advancements.endrealms.all_biomes.title": "Realm Walker", "advancements.endrealms.all_biomes.description": "Set foot in every End Realms biome"},
         "hu_hu": {"block.endrealms.phasite_ore": "Fázisérc", "item.endrealms.phasite_shard": "Fázisszilánk",
                   "advancements.endrealms.root.title": "End Realms", "advancements.endrealms.root.description": "A sárkányon túl egy egész új End vár",
                   "advancements.endrealms.chorusite_ingot.title": "A chorusit kora", "advancements.endrealms.chorusite_ingot.description": "Szerezd meg az első chorusitrudadat",
                   "advancements.endrealms.chorusite_pickaxe.title": "Élesebb a netheritnél", "advancements.endrealms.chorusite_pickaxe.description": "Szerezz egy chorusit csákányt",
                   "advancements.endrealms.chorusite_armor.title": "Az End páncélja", "advancements.endrealms.chorusite_armor.description": "Szerezz egy teljes chorusit páncélszettet",
                   "advancements.endrealms.all_biomes.title": "Birodalomjáró", "advancements.endrealms.all_biomes.description": "Járj az End Realms minden biomjában"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
print("ok")
