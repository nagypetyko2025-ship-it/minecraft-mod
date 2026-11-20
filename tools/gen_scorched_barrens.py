"""Scorched Barrens biome: art (original, CC0), block/item/entity data, worldgen JSON, loot, recipes, tags and names.
The ash layer's blockstate and models follow vanilla snow's, read from the client jar.
usage: python gen_scorched_barrens.py <client jar> <resources dir> [preview.png]"""
import json
import math
import os
import random
import sys
import zipfile

from PIL import Image

jar, RES = zipfile.ZipFile(sys.argv[1]), sys.argv[2]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
TEX = f"{A}/textures"
E = "endrealms:"
BIOME = "scorched_barrens"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def hexc(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


rnd = random.Random(7741)
CHAR = [hexc(c) for c in ("#2A2530", "#322B3A", "#3A3344", "#453D50")]
ASH = [hexc(c) for c in ("#8E8894", "#9A949F", "#A8A2AD", "#B5AFBA", "#7E7885")]
EMBER = [hexc(c) for c in ("#5B1E8C", "#9A33D6", "#D66BF2", "#FFD0FF")]


def speckle(colors, weights=None):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), rnd.choices(colors, weights)[0])
    return img


stone = speckle(CHAR, (3, 5, 4, 1))
for _ in range(7):                        # pale flecks of ash baked into the stone
    stone.putpixel((rnd.randrange(16), rnd.randrange(16)), hexc("#5C5566"))
stone.save(f"{TEX}/block/scorched_end_stone.png")
ash = speckle(ASH, (3, 4, 4, 2, 2))
ash.save(f"{TEX}/block/ash_block.png")

crack = []
for _ in range(5):                        # wandering cracks, each pulsing on its own
    x, y = rnd.randrange(16), rnd.randrange(16)
    phase = rnd.random() * math.tau
    for _ in range(rnd.randrange(7, 13)):
        crack.append((x, y, phase))
        x = (x + rnd.choice((-1, 0, 1))) % 16
        y = (y + rnd.choice((0, 1))) % 16
FRAMES = 10
base = speckle(CHAR, (3, 5, 4, 1))
strip = Image.new("RGBA", (16, 16 * FRAMES))
for f in range(FRAMES):
    strip.paste(base, (0, 16 * f))
    for x, y, phase in crack:
        t = 0.5 + 0.5 * math.sin(phase + f / FRAMES * math.tau)
        strip.putpixel((x, y + 16 * f), EMBER[min(3, int(1 + t * 3))])
strip.save(f"{TEX}/block/ember_rock.png")
w(f"{TEX}/block/ember_rock.png.mcmeta", {"animation": {"frametime": 3, "interpolate": True}})

ITEM_PAL = {"o": hexc("#3E3844"), "a": ASH[0], "b": ASH[2], "c": ASH[3], "p": EMBER[1], "q": EMBER[2], "W": EMBER[3], "d": CHAR[1]}
ICONS = {
    "ash_dust": [
        "................",
        "................",
        "................",
        "......o.........",
        ".....oao..oo....",
        "....oabbooaao...",
        "...oabcbaabbao..",
        "..oabbccbbbcbo..",
        "..oabbbcbcbbao..",
        ".oaabbbbbbbbaao.",
        ".oooaaaaaaaaooo.",
        "...oooooooooo...",
        "................",
        "................",
        "................",
        "................",
    ],
    "ash_wraith_spawn_egg": [
        "................",
        "......oooo......",
        ".....odddoo.....",
        "....odddqddo....",
        "....oddqWqdo....",
        "...odddqdddo....",
        "...odddddqddo...",
        "..oddqddqWqddo..",
        "..odqWqddqdddo..",
        "..oddqdddddddo..",
        "..odddddqddddo..",
        "...oddddddddo...",
        "...oodddddoo....",
        ".....oooo.......",
        "................",
        "................",
    ],
}
sheet = [stone, ash, strip.crop((0, 0, 16, 16))]
for name, rows in ICONS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate((row + "................")[:16]):
            if ch in ITEM_PAL:
                img.putpixel((x, y), ITEM_PAL[ch])
    img.save(f"{TEX}/item/{name}.png")
    sheet.append(img)
    w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})

# ---------------------------------------------------------------- Ash Wraith skin (64x64)
skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def rect(img, x0, y0, x1, y1, colors):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), rnd.choice(colors))


rect(skin, 0, 0, 32, 16, CHAR)            # head (the hat layer stays transparent)
rect(skin, 8, 8, 16, 16, CHAR[:2])
rect(skin, 0, 16, 8, 30, CHAR)            # thin legs
rect(skin, 16, 16, 34, 31, CHAR)          # body
rect(skin, 40, 16, 48, 36, CHAR)          # long arms
rect(skin, 0, 34, 4, 41, ASH)             # horns
for x in range(0, 64):                    # ash dusted over the shoulders and the head's top
    for y in (0, 1, 16, 17):
        if skin.getpixel((x, y))[3] and rnd.random() < 0.55:
            skin.putpixel((x, y), rnd.choice(ASH))
for x in (10, 13):                        # slit eyes
    for img in (skin, glow):
        img.putpixel((x, 11), EMBER[3])
        img.putpixel((x, 12), EMBER[2])
for sx, sy, ex, ey in ((19, 20, 25, 31), (40, 20, 48, 36), (0, 20, 8, 30)):    # cracks over the body, arms and legs
    for _ in range(2):
        x, y = rnd.randrange(sx, ex), sy
        while y < ey:
            for img in (skin, glow):
                img.putpixel((x, y), EMBER[2])
            x = min(ex - 1, max(sx, x + rnd.choice((-1, 0, 1))))
            y += 1
os.makedirs(f"{TEX}/entity/ash_wraith", exist_ok=True)
skin.save(f"{TEX}/entity/ash_wraith/ash_wraith.png")
glow.save(f"{TEX}/entity/ash_wraith/ash_wraith_glow.png")

# ---------------------------------------------------------------- block data
for name in ("scorched_end_stone", "ash_block", "ember_rock"):
    w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"endrealms:block/{name}"}}})
    w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"endrealms:block/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:block/{name}"}})
    w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + name}],
                                                                                   "condition": {"type": "minecraft:survives_explosion"}}],
                                             "random_sequence": f"endrealms:blocks/{name}"})

states = jar.read("assets/minecraft/blockstates/snow.json").decode("utf-8")
w(f"{A}/blockstates/ash_layer.json", json.loads(states.replace("minecraft:block/snow_height", "endrealms:block/ash_height").replace("minecraft:block/snow_block", "endrealms:block/ash_block")))
for h in range(2, 16, 2):
    model = jar.read(f"assets/minecraft/models/block/snow_height{h}.json").decode("utf-8")
    w(f"{A}/models/block/ash_height{h}.json", json.loads(model.replace('"block/thin_block"', '"minecraft:block/thin_block"').replace('"block/snow"', '"endrealms:block/ash_block"').replace('"minecraft:block/snow"', '"endrealms:block/ash_block"')))
w(f"{A}/items/ash_layer.json", {"model": {"type": "minecraft:model", "model": "endrealms:block/ash_height2"}})
w(f"{D}/loot_table/blocks/ash_layer.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
    {"type": "minecraft:item", "name": E + "ash_layer", "condition": {"type": "minecraft:match_block", "blocks": E + "ash_layer", "state": {"layers": str(n)}},
     "modifier": [{"type": "minecraft:set_count", "count": n}]} for n in range(1, 9)]}]}], "random_sequence": "endrealms:blocks/ash_layer"})
w(f"{D}/loot_table/entities/ash_wraith.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [{
    "type": "minecraft:item", "name": E + "ash_dust", "modifier": [
        {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
        {"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0}, "enchantment": "minecraft:looting"}]}]}],
    "random_sequence": "endrealms:entities/ash_wraith"})

w(f"{D}/recipe/ash_block.json", {"type": "minecraft:crafting_shaped", "category": "building", "key": {"#": E + "ash_dust"}, "pattern": ["##", "##"], "result": {"id": E + "ash_block"}})
w(f"{D}/recipe/ash_dust.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [E + "ash_block"], "result": {"id": E + "ash_dust", "count": 4}})
w(f"{D}/recipe/ash_layer.json", {"type": "minecraft:crafting_shaped", "category": "building", "key": {"#": E + "ash_block"}, "pattern": ["###"], "result": {"id": E + "ash_layer", "count": 6}})


def add_tag(path, values):
    p = f"{RES}/data/minecraft/tags/{path}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    t["values"] += [v for v in values if v not in t["values"]]
    w(p, t)


add_tag("block/mineable/pickaxe", [E + "scorched_end_stone", E + "ember_rock"])
add_tag("block/mineable/shovel", [E + "ash_block", E + "ash_layer"])
add_tag("block/blocks_motion_no_leaves", [E + "scorched_end_stone", E + "ember_rock", E + "ash_block"])

# ---------------------------------------------------------------- worldgen
w(f"{D}/worldgen/feature/ash_volcano.json", {"type": "endrealms:ash_volcano"})
w(f"{D}/worldgen/feature/ash_blanket.json", {"type": "endrealms:ash_blanket"})
w(f"{D}/worldgen/feature/scorched_island.json", {"type": "endrealms:scorched_island"})
w(f"{D}/worldgen/placed_feature/scorched_island.json", {"feature": "endrealms:scorched_island", "placement": []})
w(f"{D}/worldgen/placed_feature/ash_volcano.json", {"feature": "endrealms:ash_volcano", "placement": [
    {"type": "minecraft:rarity_filter", "chance": 4}, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}]})
w(f"{D}/worldgen/placed_feature/ash_blanket.json", {"feature": "endrealms:ash_blanket", "placement": []})
w(f"{D}/worldgen/biome/{BIOME}.json", {
    "attributes": {
        "minecraft:gameplay/natural_mob_spawns": {"argument": {"spawn_costs": {}, "spawns_by_category": {"monster": [
            {"type": "minecraft:enderman", "count": 4, "weight": 4},
            {"type": "endrealms:ash_wraith", "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 3}, "weight": 12}]}},
            "modifier": "overlay"},
        "minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:white_ash"}, "probability": 0.08}], "modifier": "append"},
        "minecraft:visual/fog_color": "#4A3B52"},
    "carvers": [], "downfall": 0.0, "effects": {"water_color": "#3f76e4"},
    "features": [["endrealms:scorched_island"], [], [], [], ["minecraft:end_gateway_return"], [], [], [], [],
                 ["endrealms:ash_volcano"], ["endrealms:ash_blanket"]],
    "has_precipitation": False, "temperature": 2.0})

rule_path = f"{RES}/data/minecraft/worldgen/material_rule/end.json"
rules = r(rule_path)
if BIOME not in json.dumps(rules):
    inner = rules["sequence"][0]["then_run"]["sequence"]
    inner.insert(len(inner) - 1, {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": E + BIOME},
                                  "then_run": {"type": "minecraft:block", "result_state": E + "scorched_end_stone"}})
    w(rule_path, rules)

# the "every biome" advancement
adv_path = f"{D}/advancement/all_biomes.json"
adv = r(adv_path)
if BIOME not in adv["criteria"]:
    adv["criteria"][BIOME] = {"trigger": "minecraft:location", "conditions": {"player": {"type": "minecraft:entity_properties", "entity": "this",
                                                                                         "predicate": {"minecraft:location": {"biomes": E + BIOME}}}}}
    adv["requirements"].append([BIOME])
    w(adv_path, adv)

NAMES = {"en_us": {f"biome.endrealms.{BIOME}": "Scorched Barrens", "block.endrealms.scorched_end_stone": "Scorched End Stone", "block.endrealms.ash_block": "Block of Ash",
                   "block.endrealms.ash_layer": "Ash", "block.endrealms.ember_rock": "Ember Rock", "item.endrealms.ash_dust": "Ash Dust",
                   "entity.endrealms.ash_wraith": "Ash Wraith", "item.endrealms.ash_wraith_spawn_egg": "Ash Wraith Spawn Egg"},
         "hu_hu": {f"biome.endrealms.{BIOME}": "Perzselt puszta", "block.endrealms.scorched_end_stone": "Perzselt endkő", "block.endrealms.ash_block": "Hamu blokk",
                   "block.endrealms.ash_layer": "Hamu", "block.endrealms.ember_rock": "Parázskő", "item.endrealms.ash_dust": "Hamupor",
                   "entity.endrealms.ash_wraith": "Hamulidérc", "item.endrealms.ash_wraith_spawn_egg": "Hamulidérc idéző tojás"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)

if len(sys.argv) > 3:
    s = Image.new("RGBA", (len(sheet) * 136 + 8 + 264, 272), (45, 42, 66, 255))
    for i, im in enumerate(sheet):
        big = im.resize((128, 128), Image.NEAREST)
        s.paste(big, (8 + i * 136, 8), big)
    big = skin.resize((256, 256), Image.NEAREST)
    s.paste(big, (len(sheet) * 136 + 8, 8), big)
    s.save(sys.argv[3])
print("ok")
