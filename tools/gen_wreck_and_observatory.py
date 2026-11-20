"""Sky Wreck and Observatory: rotten plank art (derived from the mod's own astral planks), structure JSON, chest loot, advancements and names.
usage: python gen_wreck_and_observatory.py <resources dir>"""
import json
import os
import random
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


rnd = random.Random(5512)
img = Image.open(f"{A}/textures/block/astral_planks.png").convert("RGBA")
for y in range(16):
    for x in range(16):
        c = img.getpixel((x, y))
        grey = (c[0] + c[1] + c[2]) // 3
        f = 0.55 + rnd.random() * 0.15
        img.putpixel((x, y), (int((c[0] + grey) / 2 * f), int((c[1] + grey) / 2 * f), int((c[2] + grey) / 2 * f), 255))
for _ in range(4):
    x, y = rnd.randrange(16), rnd.randrange(16)
    for _ in range(rnd.randrange(4, 9)):
        img.putpixel((x, y), (14, 10, 22, 255))
        x = (x + rnd.choice((-1, 0, 1))) % 16
        y = (y + 1) % 16
img.save(f"{A}/textures/block/crumbling_planks.png")

# the Castaway's robe: the librarian's, faded and patched
robe = Image.open(f"{A}/textures/entity/end_villager/librarian.png").convert("RGBA")
for y in range(robe.height):
    for x in range(robe.width):
        c = robe.getpixel((x, y))
        if c[3] and y >= 19:
            grey = (c[0] + c[1] + c[2]) // 3
            f = 0.72 + rnd.random() * 0.2
            robe.putpixel((x, y), (int((c[0] + grey * 2) / 3 * f + 14), int((c[1] + grey * 2) / 3 * f + 8), int((c[2] + grey * 2) / 3 * f), c[3]))
robe.save(f"{A}/textures/entity/end_villager/castaway.png")

NAME = "crumbling_planks"
w(f"{A}/blockstates/{NAME}.json", {"variants": {"triggered=false": {"model": f"endrealms:block/{NAME}"}, "triggered=true": {"model": f"endrealms:block/{NAME}"}}})
w(f"{A}/models/block/{NAME}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"endrealms:block/{NAME}"}})
w(f"{A}/items/{NAME}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:block/{NAME}"}})
w(f"{D}/loot_table/blocks/{NAME}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + NAME}],
                                                                                "condition": {"type": "minecraft:survives_explosion"}}],
                                         "random_sequence": f"endrealms:blocks/{NAME}"})
for tag in ("mineable/axe", "blocks_motion_no_leaves"):
    p = f"{RES}/data/minecraft/tags/block/{tag}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    if E + NAME not in t["values"]:
        t["values"].append(E + NAME)
    w(p, t)

w(f"{D}/worldgen/structure/sky_wreck.json", {"type": "endrealms:sky_wreck", "biomes": "#endrealms:has_structure/sky_wreck", "step": "surface_structures", "spawn_overrides": {}})
w(f"{D}/worldgen/structure_set/sky_wrecks.json", {"placement": {"type": "minecraft:random_spread", "salt": 61920447, "separation": 8, "spacing": 18, "spread_type": "triangular", "locate_offset": [8, 0, 8]},
                                                  "structures": [{"structure": "endrealms:sky_wreck", "weight": 1}]})
w(f"{D}/tags/worldgen/biome/has_structure/sky_wreck.json", {"values": ["minecraft:small_end_islands", "minecraft:end_barrens", "minecraft:end_midlands", "minecraft:end_highlands", E + "astral_forest", E + "prism_wastes"]})
w(f"{D}/worldgen/structure/observatory.json", {"type": "endrealms:observatory", "biomes": "#endrealms:has_structure/observatory", "step": "surface_structures", "spawn_overrides": {}})
w(f"{D}/worldgen/structure_set/observatories.json", {"placement": {"type": "minecraft:random_spread", "salt": 19283746, "separation": 10, "spacing": 24, "spread_type": "triangular", "locate_offset": [8, 0, 8]},
                                                     "structures": [{"structure": "endrealms:observatory", "weight": 1}]})
w(f"{D}/tags/worldgen/biome/has_structure/observatory.json", {"values": [E + "astral_forest", E + "prism_wastes"]})


def item(name, weight, lo, hi):
    return {"type": "minecraft:item", "name": name, "weight": weight,
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}


w(f"{D}/loot_table/chests/sky_wreck.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 8}, "entries": [
        item(E + "chorusite_shard", 8, 1, 4), item(E + "prism_shard", 10, 2, 6), item("minecraft:ender_pearl", 8, 1, 3), item("minecraft:gold_ingot", 8, 2, 6),
        item("minecraft:iron_ingot", 10, 3, 8), item(E + "cooked_starfin", 8, 2, 5), item("minecraft:string", 8, 3, 9), item("minecraft:shulker_shell", 3, 1, 2),
        item(E + "phasite_shard", 2, 1, 1)]}], "random_sequence": "endrealms:chests/sky_wreck"})
GEM_STUFF = [item("minecraft:diamond", 8, 1, 3), item("minecraft:lapis_lazuli", 12, 4, 10), item("minecraft:quartz", 12, 4, 10), item("minecraft:amethyst_shard", 12, 3, 8),
             item(E + "prism_shard", 12, 3, 7), item("minecraft:raw_gold", 8, 2, 5), item("minecraft:raw_copper", 8, 3, 7), item("minecraft:book", 10, 2, 6), item(E + "phasite_shard", 3, 1, 2),
             item(E + "star_sapphire", 1, 1, 1), item(E + "moon_opal", 1, 1, 1)]
w(f"{D}/loot_table/chests/observatory.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": GEM_STUFF}],
                                              "random_sequence": "endrealms:chests/observatory"})


def visited(structure):
    return {"trigger": "minecraft:location", "conditions": {"player": {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:location": {"structures": E + structure}}}}}


for key, icon in (("sky_wreck", E + "crumbling_planks"), ("observatory", E + "prism_lamp")):
    w(f"{D}/advancement/{key}.json", {"parent": E + "root", "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.endrealms.{key}.title"},
                                                                        "description": {"translate": f"advancements.endrealms.{key}.description"}, "frame": "task"},
                                      "criteria": {"done": visited(key)}, "requirements": [["done"]]})

T = "advancements.endrealms."
NAMES = {"en_us": {f"block.endrealms.{NAME}": "Crumbling Planks", "structure.endrealms.sky_wreck": "Sky Wreck", "structure.endrealms.observatory": "Observatory", "entity.endrealms.end_villager.castaway": "Castaway",
                   f"{T}sky_wreck.title": "Adrift", f"{T}sky_wreck.description": "Set foot on a Sky Wreck", f"{T}observatory.title": "Stargazer", f"{T}observatory.description": "Find an Observatory"},
         "hu_hu": {f"block.endrealms.{NAME}": "Korhadó deszka", "structure.endrealms.sky_wreck": "Lebegő roncs", "structure.endrealms.observatory": "Csillagvizsgáló", "entity.endrealms.end_villager.castaway": "Hajótörött",
                   f"{T}sky_wreck.title": "Sodródva", f"{T}sky_wreck.description": "Lépj egy Lebegő roncsra", f"{T}observatory.title": "Csillagnéző", f"{T}observatory.description": "Találj rá egy Csillagvizsgálóra"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
print("ok")
