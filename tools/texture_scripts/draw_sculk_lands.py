"""The stranger lands of the Sculk dimension (CC0, drawn for End Realms): Echo Glass, Sculk Stone Bricks, Dangle Berry,
Thorn Vine, Ash Flower, the Bell Core, their items, the five creatures (Dangler, Shade, Bellringer, Pillar Crawler,
Spire Nester) and their spawn eggs. Also writes block states, models, item definitions, loot tables, the biome
advancements and the names. A preview lands next to this script."""
import json
import math
import os
import random
from PIL import Image

RES = r"D:\minecraft mod\endrealms\src\main\resources"
A = os.path.join(RES, "assets", "endrealms")
D = os.path.join(RES, "data", "endrealms")
T = os.path.join(A, "textures")
E = "endrealms:"


def new(w=16, h=16):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def put(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((int(x), int(y)), tuple(int(max(0, min(255, v))) for v in c[:3]) + (int(a),))


def fill(img, x0, y0, x1, y1, colours, rnd, a=255):
    for x in range(x0, x1):
        for y in range(y0, y1):
            put(img, x, y, rnd.choice(colours), a)


def save(img, *path):
    p = os.path.join(T, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


# ------------------------------------------------------------------------------------------------ blocks
def echo_glass():
    rnd = random.Random(3)
    img = new()
    for x in range(16):
        for y in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                put(img, x, y, (70, 96, 104), 220)
            else:
                v = rnd.randint(-6, 6)
                put(img, x, y, (52 + v, 74 + v, 84 + v), 96)
    for i in range(3, 12):                                          # faint ghostly streaks, as if someone stood behind it
        put(img, i, 14 - i, (150, 190, 196), 120)
        put(img, i + 2, 15 - i, (110, 150, 158), 90)
    return img


def sculk_stone_bricks():
    rnd = random.Random(8)
    img = new()
    for y in range(16):
        row = y // 4
        for x in range(16):
            mortar = y % 4 == 3 or (x + (4 if row % 2 else 0)) % 8 == 7
            v = rnd.randint(-5, 5)
            put(img, x, y, (24, 28, 31) if mortar else (40 + v, 44 + v, 48 + v))
    for _ in range(6):
        x, y = rnd.randrange(16), rnd.choice([3, 7, 11, 15])
        put(img, x, y, (34, 120, 126))
    return img


def dangle_berry():
    rnd = random.Random(4)
    img = new()
    for sx in (5, 8, 11):
        for y in range(0, 9 + rnd.randint(0, 3)):
            put(img, sx + (1 if y > 6 and sx == 11 else 0), y, (40, 46, 44))
    for cx, cy in ((5, 11), (8, 13), (11, 11)):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if abs(dx) + abs(dy) < 2:
                    put(img, cx + dx, cy + dy, (214, 220, 212) if dy < 1 else (170, 176, 170))
        put(img, cx, cy, (236, 240, 230))
    return img


def thorn_vine(berries):
    rnd = random.Random(6)
    img = new()
    x = 7
    for y in range(16):
        x = max(3, min(12, x + rnd.choice((-1, 0, 0, 1))))
        put(img, x, y, (14, 16, 18))
        put(img, x + 1, y, (24, 26, 28))
        if rnd.random() < 0.35:                                   # thorns
            put(img, x - 1, y, (40, 44, 46))
        if rnd.random() < 0.3:
            put(img, x + 2, y - 1, (40, 44, 46))
    for y in (3, 9, 13):                                           # side shoots
        for k in range(3):
            put(img, 3 + k if y != 9 else 12 - k, y + k // 2, (18, 20, 22))
    if berries:
        for bx, by in ((5, 4), (11, 8), (6, 12), (10, 14), (4, 9)):
            put(img, bx, by, (60, 180, 240))
            put(img, bx + 1, by, (40, 130, 200))
            put(img, bx, by + 1, (30, 100, 170))
    return img


def ash_flower():
    rnd = random.Random(9)
    img = new()
    for sx, top in ((5, 6), (9, 4), (12, 8)):
        for y in range(top + 2, 16):
            put(img, sx, y, (70, 72, 70))
        for dx in range(-2, 3):
            for dy in range(-2, 2):
                if dx * dx + dy * dy <= 4:
                    put(img, sx + dx, top + dy, rnd.choice([(196, 196, 192), (170, 170, 166), (214, 214, 210)]))
    return img


def bell_core():
    rnd = random.Random(12)
    img = new()
    fill(img, 0, 0, 16, 16, [(64, 130, 112), (56, 118, 100), (72, 142, 122)], rnd)
    for i in range(16):
        put(img, i, 7 + int(2 * math.sin(i / 2.0)), (120, 240, 226))
        put(img, 7 + int(2 * math.sin(i / 2.5)), i, (90, 210, 200))
    return img


# ------------------------------------------------------------------------------------------------ items
def berry_item(colour, glow):
    img = new()
    for cx, cy in ((6, 9), (10, 8), (8, 12)):
        for dx in (-2, -1, 0, 1, 2):
            for dy in (-2, -1, 0, 1, 2):
                if dx * dx + dy * dy <= 4:
                    put(img, cx + dx, cy + dy, colour if dy < 1 else tuple(v * 0.75 for v in colour))
        put(img, cx - 1, cy - 1, glow)
    for y in range(2, 7):
        put(img, 8, y, (40, 46, 44))
    return img


def egg(base, spots):
    img = new()
    for y in range(2, 15):
        for x in range(3, 13):
            if ((x - 7.5) / 4.6) ** 2 + ((y - 8.8) / (6.6 if y > 8 else 6.9)) ** 2 <= 1:
                put(img, x, y, base if x < 9 else tuple(v * 0.85 for v in base))
    for x, y in [(5, 6), (9, 4), (8, 10), (6, 12), (10, 12), (11, 8)]:
        put(img, x, y, spots)
    return img


# ------------------------------------------------------------------------------------------------ creatures
def skeleton_layout(skin, dark, face, chest=None, seed=1):
    """64x32 in the thin humanoid layout: head 0,0 (8x8x8), body 16,16 (8x12x4), arms 40,16 and legs 0,16 (2x12x2)."""
    rnd = random.Random(seed)
    img = new(64, 32)
    fill(img, 0, 0, 32, 16, skin, rnd)
    fill(img, 0, 16, 56, 32, skin, rnd)
    for x in range(0, 56, 7):                                      # shadowed joints and creases
        for y in range(16, 32):
            if (x + y) % 6 == 0:
                put(img, x, y, dark)
    face(img)
    if chest:
        chest(img)
    return img


def dangler_face(img):
    for x, y in ((9, 10), (10, 10), (13, 10), (14, 10), (9, 11), (14, 11)):   # hollow eye pits
        put(img, x, y, (8, 8, 10))
    for x in range(10, 14):                                        # a long thin mouth
        put(img, x, 14, (20, 14, 16))
    put(img, 11, 15, (20, 14, 16))


def bellringer_face(img):
    for x, y in ((9, 11), (10, 11), (13, 11), (14, 11)):
        put(img, x, y, (6, 8, 10))
    for x in range(10, 14):
        put(img, x, 14, (40, 36, 30))


def bellringer_chest(img):
    for y in range(21, 31, 2):                                     # ribs across the front of the body
        for x in range(20, 28):
            put(img, x, y, (230, 226, 210))
        for x in range(21, 27):
            put(img, x, y + 1, (30, 30, 34))
    for dx, dy in ((0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (2, 1), (-1, 2), (0, 2), (1, 2), (2, 2)):  # the small bell inside
        put(img, 23 + dx, 24 + dy, (90, 150, 128))


def shade_texture():
    rnd = random.Random(5)
    img = new(64, 64)
    for x in range(64):
        for y in range(64):
            v = rnd.randint(-4, 4)
            put(img, x, y, (12 + v, 14 + v, 18 + v), 150 + rnd.randint(-20, 20))
    for x, y in ((10, 12), (13, 12)):                              # the faintest hint of eyes
        put(img, x, y, (120, 140, 150), 120)
    return img


def crawler_texture():
    rnd = random.Random(7)
    img = new(64, 32)
    fill(img, 0, 0, 64, 32, [(196, 192, 180), (176, 172, 160), (210, 206, 194)], rnd)
    for x in range(18, 50):                                        # dark rings along the long legs
        for y in range(0, 4):
            if x % 5 == 0:
                put(img, x, y, (60, 58, 54))
    fill(img, 32, 4, 64, 20, [(60, 58, 56), (74, 72, 68)], rnd)    # the head darker
    return img


def nester_texture():
    rnd = random.Random(11)
    img = new(64, 64)
    fill(img, 0, 0, 64, 64, [(40, 42, 46), (34, 36, 40), (48, 50, 54)], rnd)    # membrane
    fill(img, 0, 0, 32, 22, [(170, 166, 154), (150, 146, 136)], rnd)             # the gaunt bony body and head
    for x in range(23, 64, 4):                                     # finger bones through the wings
        for y in range(12, 34):
            put(img, x, y, (120, 116, 108))
    return img


# ------------------------------------------------------------------------------------------------ write
blocks = {"echo_glass": echo_glass(), "sculk_stone_bricks": sculk_stone_bricks(), "dangle_berry_bush": dangle_berry(),
          "thorn_vine": thorn_vine(False), "thorn_vine_berries": thorn_vine(True), "ash_flower": ash_flower(), "bell_core": bell_core()}
for name, img in blocks.items():
    save(img, "block", name + ".png")
items = {"dangle_berry": berry_item((218, 222, 212), (250, 250, 246)), "thorn_berry": berry_item((50, 150, 220), (150, 230, 255)),
         "dangler_spawn_egg": egg((208, 206, 196), (20, 18, 20)), "shade_spawn_egg": egg((30, 34, 40), (120, 140, 150)),
         "bellringer_spawn_egg": egg((214, 210, 196), (80, 150, 128)), "pillar_crawler_spawn_egg": egg((196, 192, 180), (60, 58, 54)),
         "spire_nester_spawn_egg": egg((44, 46, 50), (170, 166, 154))}
for name, img in items.items():
    save(img, "item", name + ".png")
    write(os.path.join(A, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": E + "item/" + name}})
    write(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": E + "item/" + name}})
creatures = {
    "dangler": skeleton_layout([(206, 204, 196), (190, 188, 180), (218, 216, 208)], (120, 118, 112), dangler_face, seed=2),
    "bellringer": skeleton_layout([(214, 208, 190), (196, 190, 172), (226, 220, 204)], (110, 104, 90), bellringer_face, bellringer_chest, seed=3),
    "shade": shade_texture(), "pillar_crawler": crawler_texture(), "spire_nester": nester_texture()}
for name, img in creatures.items():
    save(img, "entity", name, name + ".png")
glow = new(64, 32)
for x, y in ((9, 11), (14, 11)):
    put(glow, x, y, (230, 236, 240))
save(glow, "entity", "dangler", "dangler_glow.png")
glow = new(64, 32)
for x, y in ((10, 11), (13, 11)):
    put(glow, x, y, (110, 250, 230))
for dx, dy in ((0, 1), (1, 1)):
    put(glow, 23 + dx, 24 + dy, (60, 200, 170))
save(glow, "entity", "bellringer", "bellringer_glow.png")

# block states and models
def simple(name, model):
    write(os.path.join(A, "blockstates", name + ".json"), {"variants": {"": {"model": E + "block/" + name}}})
    write(os.path.join(A, "models", "block", name + ".json"), model)
    write(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": E + "block/" + name}})


simple("echo_glass", {"parent": "minecraft:block/cube_all", "textures": {"all": {"force_translucent": True, "sprite": E + "block/echo_glass"}}})
simple("sculk_stone_bricks", {"parent": "minecraft:block/cube_all", "textures": {"all": E + "block/sculk_stone_bricks"}})
simple("bell_core", {"parent": "minecraft:block/cube_all", "textures": {"all": E + "block/bell_core"}})
for name in ("dangle_berry_bush", "ash_flower"):
    write(os.path.join(A, "blockstates", name + ".json"), {"variants": {"": {"model": E + "block/" + name}}})
    write(os.path.join(A, "models", "block", name + ".json"), {"parent": "minecraft:block/cross", "textures": {"cross": E + "block/" + name}})
    write(os.path.join(A, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": E + "block/" + name}})
    write(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": E + "item/" + name}})
for suffix in ("", "_berries"):
    write(os.path.join(A, "models", "block", "thorn_vine" + suffix + ".json"),
          {"parent": "minecraft:block/ladder", "textures": {"particle": E + "block/thorn_vine" + suffix, "texture": E + "block/thorn_vine" + suffix}})
variants = {}
for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for berries in ("false", "true"):
        v = {"model": E + "block/thorn_vine" + ("_berries" if berries == "true" else "")}
        if y:
            v["y"] = y
        variants[f"berries={berries},facing={facing}"] = v
write(os.path.join(A, "blockstates", "thorn_vine.json"), {"variants": variants})
write(os.path.join(A, "models", "item", "thorn_vine.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": E + "block/thorn_vine"}})
write(os.path.join(A, "items", "thorn_vine.json"), {"model": {"type": "minecraft:model", "model": E + "item/thorn_vine"}})

# loot tables: blocks
LT = os.path.join(D, "loot_table")


def drops_self(name):
    write(os.path.join(LT, "blocks", name + ".json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
          "entries": [{"type": "minecraft:item", "name": E + name}]}], "random_sequence": E + "blocks/" + name})


for name in ("sculk_stone_bricks", "thorn_vine", "ash_flower", "bell_core"):
    drops_self(name)
write(os.path.join(LT, "blocks", "echo_glass.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": "minecraft:tool/can_silk_touch",
      "entries": [{"type": "minecraft:item", "name": E + "echo_glass"}]}], "random_sequence": E + "blocks/echo_glass"})
write(os.path.join(LT, "blocks", "dangle_berry_bush.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": E + "dangle_berry",
      "modifier": {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}}]}], "random_sequence": E + "blocks/dangle_berry_bush"})


def item(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name if ":" in name else "minecraft:" + name, "weight": weight}
    if hi > 1:
        e["modifier"] = {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}
    return e


def chest(name, rolls, entries):
    write(os.path.join(LT, "chests", name + ".json"), {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
          "entries": entries}], "random_sequence": E + "chests/" + name})


# no Chorusite Ingots anywhere in chests (house rule)
chest("sculk_ruin", (3, 6), [item("bone", 10, 2, 6), item("iron_ingot", 8, 1, 4), item("gold_ingot", 6, 1, 3), item("echo_shard", 3, 1, 2),
      item(E + "echo_dust", 8, 1, 4), item(E + "soul_mushroom", 6, 1, 4), item("experience_bottle", 5, 1, 4), item("diamond", 2, 1, 2),
      item("golden_apple", 2), item("book", 5, 1, 3), item("candle", 6, 1, 3), item(E + "sculk_antler", 2)])
chest("shadow_city", (4, 7), [item(E + "echo_glass", 8, 2, 8), item("echo_shard", 4, 1, 3), item("emerald", 6, 1, 4), item("gold_ingot", 6, 1, 4),
      item("diamond", 3, 1, 2), item("amethyst_shard", 6, 2, 6), item("recovery_compass", 1), item("music_disc_5", 1), item("experience_bottle", 6, 2, 5),
      item("golden_apple", 3), item("name_tag", 2), item(E + "echo_dust", 6, 2, 5)])
chest("spire_nest", (2, 5), [item("feather", 10, 2, 8), item("bone", 8, 2, 6), item("gold_nugget", 8, 3, 9), item("emerald", 4, 1, 3),
      item("diamond", 2, 1, 1), item(E + "thorn_berry", 6, 2, 5), item("ender_pearl", 3, 1, 2), item("experience_bottle", 4, 1, 3)])
write(os.path.join(LT, "chests", "hermit_pillar.json"), {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [item("diamond", 4, 1, 3), item("emerald", 6, 2, 6), item("echo_shard", 5, 2, 4),
     item("enchanted_golden_apple", 1), item("golden_apple", 5, 1, 2), item("experience_bottle", 6, 3, 8), item(E + "thorn_berry", 5, 2, 6),
     item(E + "sculk_antler", 3), item("recovery_compass", 2), item("spyglass", 3)]},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:written_book", "modifier": [
        {"type": "minecraft:set_book_cover", "title": "Napló", "author": "Ismeretlen", "generation": 3},
        {"type": "minecraft:set_written_book_pages", "mode": "replace_all", "pages": [
            "Harmincadik nap. Az oszlopok ma este is énekeltek. A szél nem fúj, mégis szólnak.",
            "Valami jár odakint az oszlopok között. Nem a földön. Fent, a köveken. Hallom a karmait.",
            "A madarak nem esznek. Nem isznak. Csak köröznek. Ma megszámoltam őket: eggyel többen vannak, mint tegnap.",
            "Ha valaki ezt olvassa: ne nézz fel éjjel. Az égen van valami. Akkor is figyel, amikor nem látod.",
            "Lezártam az ajtót. A lépcsőn léptek jönnek fel. Az én lépteim. Pedig én itt fent ülök."]}]}]}],
    "random_sequence": E + "chests/hermit_pillar"})


def mob_loot(name, entries):
    write(os.path.join(LT, "entities", name + ".json"), {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [e]} for e in entries],
          "random_sequence": E + "entities/" + name})


mob_loot("dangler", [item("bone", 1, 1, 3), item("string", 1, 1, 3), item(E + "echo_dust", 1, 0, 2)])
mob_loot("bellringer", [item("bone", 1, 2, 4), item("copper_ingot", 1, 1, 3), item("echo_shard", 1, 0, 1)])
mob_loot("pillar_crawler", [item("string", 1, 2, 4), item("bone", 1, 1, 2), item("spider_eye", 1, 0, 1)])
mob_loot("spire_nester", [item("feather", 1, 1, 3), item("phantom_membrane", 1, 0, 1)])
write(os.path.join(LT, "entities", "shade.json"), {"type": "minecraft:entity", "pools": [], "random_sequence": E + "entities/shade"})

# biome advancements: the Sculk tab and the End Realms all-biomes challenge
NEW = [("hanging_woods", "minecraft:pale_hanging_moss", "Fordított erdő", "Hanging Woods", "Ahol a fák lefelé nőnek", "Where Trees Grow Down",
        "Járj a Fordított erdő lebegő fái alatt", "Walk beneath the floating trees of the Hanging Woods"),
       ("shadow_city", E + "echo_glass", "Árnyváros", "Shadow City", "Lakatlan", "Uninhabited", "Járj az Árnyváros üvegromjai között", "Walk among the glass ruins of the Shadow City"),
       ("bell_valley", "minecraft:bell", "Harangvölgy", "Bell Valley", "Kinek szól a harang", "For Whom the Bell Tolls", "Ereszkedj le a Harangvölgybe", "Go down into the Bell Valley"),
       ("listening_pillars", E + "sculk_stone", "Hallgató oszlopok", "Listening Pillars", "Hallgatózók", "The Listeners", "Járj a Hallgató oszlopok között", "Walk among the Listening Pillars"),
       ("scattered_ruins", E + "sculk_stone_bricks", "Szétszórt romok", "Scattered Ruins", "Semmi sem illik össze", "Nothing Fits", "Járd be a Szétszórt romok vidékét", "Wander the land of the Scattered Ruins")]


def loc(b):
    return {"trigger": "minecraft:location", "conditions": {"player": {"type": "minecraft:entity_properties", "entity": "this",
            "predicate": {"minecraft:location": {"biomes": E + b}}}}}


ADV = os.path.join(D, "advancement")
for b, icon, *_ in NEW:
    write(os.path.join(ADV, "sculk", b + ".json"), {"parent": E + "sculk/root", "display": {"icon": {"id": icon},
          "title": {"translate": f"advancements.endrealms.sculk.{b}.title"}, "description": {"translate": f"advancements.endrealms.sculk.{b}.description"},
          "frame": "task", "show_toast": True, "announce_to_chat": True}, "criteria": {"visit": loc(b)}, "requirements": [["visit"]]})
for path in (os.path.join(ADV, "sculk", "all_biomes.json"), os.path.join(ADV, "all_biomes.json")):
    with open(path, encoding="utf-8") as f:
        d = json.load(f)
    for b, *_ in NEW:
        d["criteria"][b] = loc(b)
        if [b] not in d["requirements"]:
            d["requirements"].append([b])
    write(path, d)

# names
NAMES = {"hu_hu": {"block.endrealms.echo_glass": "Visszhangüveg", "block.endrealms.sculk_stone_bricks": "Sculk-kőtégla", "block.endrealms.dangle_berry_bush": "Csüngő bogyó",
                   "block.endrealms.thorn_vine": "Tüskeinda", "block.endrealms.ash_flower": "Hamuvirág", "block.endrealms.bell_core": "Harangmag",
                   "item.endrealms.dangle_berry": "Csüngő bogyó", "item.endrealms.thorn_berry": "Tüskebogyó",
                   "entity.endrealms.dangler": "Lógó", "entity.endrealms.shade": "Árnyalak", "entity.endrealms.bellringer": "Harangozó",
                   "entity.endrealms.pillar_crawler": "Oszlopjáró", "entity.endrealms.spire_nester": "Csúcsfészkelő",
                   "item.endrealms.dangler_spawn_egg": "Lógó idéző tojás", "item.endrealms.shade_spawn_egg": "Árnyalak idéző tojás",
                   "item.endrealms.bellringer_spawn_egg": "Harangozó idéző tojás", "item.endrealms.pillar_crawler_spawn_egg": "Oszlopjáró idéző tojás",
                   "item.endrealms.spire_nester_spawn_egg": "Csúcsfészkelő idéző tojás"},
         "en_us": {"block.endrealms.echo_glass": "Echo Glass", "block.endrealms.sculk_stone_bricks": "Sculk Stone Bricks", "block.endrealms.dangle_berry_bush": "Dangle Berries",
                   "block.endrealms.thorn_vine": "Thorn Vine", "block.endrealms.ash_flower": "Ash Flower", "block.endrealms.bell_core": "Bell Core",
                   "item.endrealms.dangle_berry": "Dangle Berry", "item.endrealms.thorn_berry": "Thorn Berry",
                   "entity.endrealms.dangler": "Dangler", "entity.endrealms.shade": "Shade", "entity.endrealms.bellringer": "Bellringer",
                   "entity.endrealms.pillar_crawler": "Pillar Crawler", "entity.endrealms.spire_nester": "Spire Nester",
                   "item.endrealms.dangler_spawn_egg": "Dangler Spawn Egg", "item.endrealms.shade_spawn_egg": "Shade Spawn Egg",
                   "item.endrealms.bellringer_spawn_egg": "Bellringer Spawn Egg", "item.endrealms.pillar_crawler_spawn_egg": "Pillar Crawler Spawn Egg",
                   "item.endrealms.spire_nester_spawn_egg": "Spire Nester Spawn Egg"}}
for b, icon, hu, en, hu_t, en_t, hu_d, en_d in NEW:
    NAMES["hu_hu"][f"biome.endrealms.{b}"] = hu
    NAMES["en_us"][f"biome.endrealms.{b}"] = en
    NAMES["hu_hu"][f"advancements.endrealms.sculk.{b}.title"] = hu_t
    NAMES["en_us"][f"advancements.endrealms.sculk.{b}.title"] = en_t
    NAMES["hu_hu"][f"advancements.endrealms.sculk.{b}.description"] = hu_d
    NAMES["en_us"][f"advancements.endrealms.sculk.{b}.description"] = en_d
for lang, kv in NAMES.items():
    p = os.path.join(A, "lang", lang + ".json")
    with open(p, encoding="utf-8") as f:
        d = json.load(f)
    d.update(kv)
    write(p, d)

preview = Image.new("RGBA", (64 * 8, 64 * 2), (30, 34, 36, 255))
for i, img in enumerate(list(blocks.values())[:8]):
    big = img.resize((64, 64), Image.NEAREST)
    preview.paste(big, (64 * i, 0), big)
for i, img in enumerate(creatures.values()):
    big = img.resize((96, 48 if img.height == 32 else 96), Image.NEAREST)
    preview.paste(big, (100 * i, 64), big)
preview.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "preview_sculk_lands.png"))
print("sculk lands written")
