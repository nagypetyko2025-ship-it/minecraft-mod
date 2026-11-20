"""Gem system: art (original, CC0), block and item data, recipes, tags, advancement and names.
usage: python gen_gems.py <resources dir> [preview.png]"""
import json
import os
import sys

from PIL import Image

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
TEX = f"{A}/textures"
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


SHAPE = [
    "................",
    "................",
    ".....oooooo.....",
    "....oWLLLMDo....",
    "...oWLLLMMMDo...",
    "..oLLLLMMMMDDo..",
    "..oLWLMMMMDDDo..",
    "..oMLLMMMDDVVo..",
    "...oMLMMDDVVo...",
    "....oMMDDVVo....",
    ".....oMDVVo.....",
    "......oDVo......",
    ".......oo.......",
    "................",
    "................",
    "................",
]
GEMS = {
    "ember_garnet": {"o": "#3A0A24", "W": "#FFD8C8", "L": "#FF7A5C", "M": "#E0344E", "D": "#A0163E", "V": "#6A1048"},
    "star_sapphire": {"o": "#0E1A52", "W": "#E6F4FF", "L": "#7FC4FF", "M": "#3A78E8", "D": "#2348B8", "V": "#1A2C80"},
    "void_onyx": {"o": "#0A0614", "W": "#C8B8FF", "L": "#6A4FA8", "M": "#35256B", "D": "#20163F", "V": "#120C28"},
    "moon_opal": {"o": "#3A4A5C", "W": "#FFFFFF", "L": "#E4F2F0", "M": "#BFD8E8", "D": "#A8B8E0", "V": "#C8A8E0"},
    "ash_quartz": {"o": "#2A2530", "W": "#F0E8D8", "L": "#B5AFBA", "M": "#8E8894", "D": "#D66BF2", "V": "#5B1E8C"},
    "deep_aquamarine": {"o": "#0E3A44", "W": "#D8FFF4", "L": "#7FF0E0", "M": "#3BC8C0", "D": "#1F7A80", "V": "#16505C"},
    "bloodstone": {"o": "#1A0C10", "W": "#FF9A9A", "L": "#B02A30", "M": "#3C6E48", "D": "#2A5038", "V": "#7A1420"},
    "crownstone": {"o": "#4A2A08", "W": "#FFF6D8", "L": "#FFE08A", "M": "#E2B23C", "D": "#B07A1E", "V": "#7A2A3A"},
}
sheet = []
for name, pal in GEMS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(SHAPE):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), hexc(pal[ch]))
    img.save(f"{TEX}/item/{name}.png")
    sheet.append(img)
    w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})

BASE = Image.open(f"{TEX}/block/polished_voidstone.png").convert("RGBA")
DARK, DEEP, GOLD, RED, BLUE, VIOLET = map(hexc, ("#1C1238", "#35256B", "#C8A24A", "#E0344E", "#3A78E8", "#8A6AD8"))


def framed():
    img = BASE.copy()
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), DARK)
    return img


top = framed()
for x in range(3, 13):
    for y in range(3, 13):
        top.putpixel((x, y), DEEP)
for i in range(3, 13):
    for x, y in ((i, 3), (i, 12), (3, i), (12, i)):
        top.putpixel((x, y), GOLD)
for (cx, cy), colour in (((7, 5), RED), ((5, 9), BLUE), ((10, 9), VIOLET)):
    for x, y in ((cx, cy), (cx + 1, cy), (cx, cy + 1), (cx + 1, cy + 1)):
        top.putpixel((x, y), colour)
top.save(f"{TEX}/block/gem_table_top.png")
side = framed()
for x in range(1, 15):
    side.putpixel((x, 3), GOLD)
for x, colour in ((4, RED), (7, BLUE), (10, VIOLET)):
    for dx in range(2):
        for y in (7, 8):
            side.putpixel((x + dx, y), colour)
side.save(f"{TEX}/block/gem_table_side.png")
sheet += [top, side]

w(f"{A}/blockstates/gem_table.json", {"variants": {"": {"model": "endrealms:block/gem_table"}}})
w(f"{A}/models/block/gem_table.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "bottom": "endrealms:block/polished_voidstone", "side": "endrealms:block/gem_table_side", "top": "endrealms:block/gem_table_top"}})
w(f"{A}/items/gem_table.json", {"model": {"type": "minecraft:model", "model": "endrealms:block/gem_table"}})
w(f"{D}/loot_table/blocks/gem_table.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + "gem_table"}],
                                                                                  "condition": {"type": "minecraft:survives_explosion"}}],
                                            "random_sequence": "endrealms:blocks/gem_table"})
PHASITE = [E + "phasite_ore", E + "phasite_shard"]
RECIPES = {
    "ember_garnet": [E + "chorusite_shard", "minecraft:redstone", "minecraft:redstone", "minecraft:raw_gold", "minecraft:raw_gold", E + "ember_rock"],
    "star_sapphire": [PHASITE, "minecraft:lapis_lazuli", "minecraft:lapis_lazuli", "minecraft:diamond", E + "prism_shard", E + "prism_shard"],
    "void_onyx": [E + "chorusite_shard", "minecraft:raw_iron", "minecraft:raw_iron", "minecraft:obsidian", E + "voidstone", E + "voidstone"],
    "moon_opal": [PHASITE, "minecraft:quartz", "minecraft:quartz", "minecraft:amethyst_shard", "minecraft:amethyst_shard", E + "moonbell"],
    "ash_quartz": [E + "chorusite_shard", "minecraft:quartz", "minecraft:quartz", "minecraft:coal", E + "ash_dust", E + "ash_dust"],
    "deep_aquamarine": [E + "deepglow_shard", E + "deepglow_shard", E + "deepglow_shard", E + "prism_shard", E + "prism_shard", PHASITE],
    "bloodstone": [E + "chorusite_shard", "minecraft:raw_copper", "minecraft:raw_copper", "minecraft:redstone", "minecraft:redstone", "minecraft:nether_wart"],
}
for name, ingredients in RECIPES.items():
    w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients, "result": {"id": E + name}})
w(f"{D}/recipe/gem_table.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["RGR", "VSV", "VVV"],
                                 "key": {"R": E + "prism_shard", "G": "minecraft:diamond", "V": E + "polished_voidstone", "S": "minecraft:smithing_table"},
                                 "result": {"id": E + "gem_table"}})


def add_tag(path, values):
    p = f"{RES}/data/minecraft/tags/{path}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    t["values"] += [v for v in values if v not in t["values"]]
    w(p, t)


add_tag("block/mineable/pickaxe", [E + "gem_table"])
add_tag("block/blocks_motion_no_leaves", [E + "gem_table"])
add_tag("item/swords", [E + "riftblade"])

w(f"{D}/advancement/gem.json", {"parent": "endrealms:root", "display": {"icon": {"id": E + "ember_garnet"}, "title": {"translate": "advancements.endrealms.gem.title"},
                                                                        "description": {"translate": "advancements.endrealms.gem.description"}, "frame": "task"},
                                "criteria": {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [E + g for g in GEMS]}]}}},
                                "requirements": [["done"]]})

G = "gem.endrealms."
NAMES = {"en_us": {"item.endrealms.ember_garnet": "Ember Garnet", "item.endrealms.star_sapphire": "Star Sapphire", "item.endrealms.void_onyx": "Void Onyx",
                   "item.endrealms.moon_opal": "Moon Opal", "item.endrealms.ash_quartz": "Ash Quartz", "item.endrealms.bloodstone": "Bloodstone", "item.endrealms.deep_aquamarine": "Deep Aquamarine", "item.endrealms.crownstone": "Crownstone",
                   f"{G}ignite": "Sets the target on fire for %s s", f"{G}lifesteal": "+%s%% Lifesteal", f"{G}socketed": "Gem: %s", f"{G}remove": "Take the result: the gem comes out and is destroyed",
                   "block.endrealms.gem_table": "Gem Table", "container.endrealms.gem_table": "Gem Table",
                   f"{G}durability": "+%s Durability", f"{G}already": "This gem is already set in it", f"{G}full": "It already holds a gem: take it out first",
                   f"{G}hint": "Add an item and a gem, or an item alone to take its gem out", f"{G}category.weapon": "Weapon", f"{G}category.axe": "Axe", f"{G}category.pickaxe": "Pickaxe", f"{G}category.shovel": "Shovel",
                   f"{G}category.hoe": "Hoe", f"{G}category.helmet": "Helmet", f"{G}category.chestplate": "Chestplate", f"{G}category.leggings": "Leggings", f"{G}category.boots": "Boots",
                   "advancements.endrealms.gem.title": "Cut and Polished", "advancements.endrealms.gem.description": "Craft your first gem"},
         "hu_hu": {"item.endrealms.ember_garnet": "Parázsgránát", "item.endrealms.star_sapphire": "Csillagzafír", "item.endrealms.void_onyx": "Űrónix",
                   "item.endrealms.moon_opal": "Holdopál", "item.endrealms.ash_quartz": "Hamukvarc", "item.endrealms.bloodstone": "Vérkő", "item.endrealms.deep_aquamarine": "Mélyfény-akvamarin", "item.endrealms.crownstone": "Koronakő",
                   f"{G}ignite": "%s mp-re felgyújtja a célt", f"{G}lifesteal": "+%s%% életlopás", f"{G}socketed": "Ékkő: %s", f"{G}remove": "Vedd ki az eredményt: az ékkő kikerül, és megsemmisül",
                   "block.endrealms.gem_table": "Ékkőasztal", "container.endrealms.gem_table": "Ékkőasztal",
                   f"{G}durability": "+%s tartósság", f"{G}already": "Ez az ékkő már benne van", f"{G}full": "Már van benne ékkő: előbb vedd ki",
                   f"{G}hint": "Tegyél be egy tárgyat és egy ékkövet, vagy csak egy tárgyat az ékkő kivételéhez", f"{G}category.weapon": "Fegyver", f"{G}category.axe": "Fejsze", f"{G}category.pickaxe": "Csákány", f"{G}category.shovel": "Ásó",
                   f"{G}category.hoe": "Kapa", f"{G}category.helmet": "Sisak", f"{G}category.chestplate": "Mellvért", f"{G}category.leggings": "Lábvért", f"{G}category.boots": "Csizma",
                   "advancements.endrealms.gem.title": "Csiszolt kincs", "advancements.endrealms.gem.description": "Készítsd el az első ékkövedet"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)

if len(sys.argv) > 2:
    s = Image.new("RGBA", (len(sheet) * 136 + 8, 144), (45, 42, 66, 255))
    for i, im in enumerate(sheet):
        big = im.resize((128, 128), Image.NEAREST)
        s.paste(big, (8 + i * 136, 8), big)
    s.save(sys.argv[2])
print("ok")
