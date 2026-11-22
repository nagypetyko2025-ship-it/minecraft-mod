"""The life of the Sculk dimension (CC0, drawn for End Realms): Echo Flower, Watcher Grass, Soul Mushroom, the Bone
Stag, the will-moths and the sculk head in the night sky. Also writes the block states, models, item definitions,
loot tables and the particle definition, and a preview next to this script."""
import json
import math
import os
import random
from PIL import Image, ImageDraw

RES = r"D:\minecraft mod\endrealms\src\main\resources"
A = os.path.join(RES, "assets", "endrealms")
D = os.path.join(RES, "data", "endrealms")
T = os.path.join(A, "textures")


def new(w=16, h=16):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def put(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(int(v) for v in c[:3]) + (a,))


def save(img, *path):
    p = os.path.join(T, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


# ---------------------------------------------------------------------------------------------------------- plants
def echo_flower():
    img = new()
    stem = [(18, 52, 56), (24, 66, 70)]
    for y in range(15, 6, -1):                                   # stem rising, then bowing over to the right
        x = 7 + (1 if y < 9 else 0)
        put(img, x, y, stem[y % 2])
    for x, y in [(9, 6), (10, 6), (10, 7)]:
        put(img, x, y, stem[0])
    for x, y in [(5, 13), (6, 12), (9, 12), (10, 11)]:           # two thin leaves
        put(img, x, y, stem[1])
    bell = [(214, 222, 220), (188, 200, 200), (150, 166, 168)]   # the pale bell hanging down
    for dy, (x0, x1) in enumerate([(10, 12), (9, 13), (9, 13), (9, 13), (8, 14)]):
        for x in range(x0, x1 + 1):
            put(img, x, 7 + dy, bell[0 if x < 12 else 1 if x < 13 else 2])
    for x in (9, 11, 13):
        put(img, x, 12, bell[2])
    put(img, 11, 12, (90, 200, 205))                             # faint glow inside
    return img


def grass_blades(seed, top):
    rnd = random.Random(seed)
    img = new()
    dark = [(8, 18, 22), (12, 28, 32), (16, 38, 42), (20, 48, 52)]
    for blade in range(9):
        x = 1 + blade * 1.7 + rnd.random()
        lean = rnd.uniform(-0.25, 0.25)
        start = 15
        end = rnd.randint(1, 6) if top else 0
        for y in range(start, end - 1, -1):
            px = int(round(x + lean * (start - y) * (0.6 if top else 0.25)))
            put(img, px, y, dark[(blade + y // 4) % len(dark)])
            if not top or y > end + 3:
                put(img, px + 1, y, dark[(blade + 1) % len(dark)])
    if top:
        for _ in range(4):                                       # a few sculk-cyan tips
            put(img, rnd.randrange(16), rnd.randint(1, 5), (40, 120, 126))
    return img


def eyes(closed):
    img = new()
    for cx in (4, 11):                                           # two eyes side by side, upper part of the texture
        if closed:
            for x in range(cx - 2, cx + 3):
                put(img, x, 3, (30, 60, 64))
            continue
        for x in range(cx - 2, cx + 3):
            for y in range(2, 5):
                if abs(x - cx) + abs(y - 3) * 2 <= 3:
                    put(img, x, y, (206, 232, 226))
        put(img, cx, 3, (60, 225, 230))
        put(img, cx - 1, 3, (40, 170, 178))
        put(img, cx, 2, (90, 235, 238))
    return img


def soul_mushroom():
    img = new()
    for y in range(9, 16):                                       # pale stems, a big one and a small one
        put(img, 7, y, (176, 184, 178))
        put(img, 8, y, (150, 158, 154))
    for y in range(12, 16):
        put(img, 11, y, (160, 168, 164))
    cap = [(64, 214, 214), (40, 170, 176), (24, 110, 120)]
    for y, (x0, x1) in enumerate([(5, 10), (4, 11), (3, 12), (3, 12)]):
        for x in range(x0, x1 + 1):
            put(img, x, 5 + y, cap[0 if y < 2 else 1] if 4 < x < 11 else cap[2])
    for x, y in [(6, 6), (9, 7), (5, 8)]:
        put(img, x, y, (190, 250, 248))
    for x in range(10, 13):                                      # small cap
        put(img, x, 11, cap[1])
    put(img, 11, 10, cap[0])
    return img


# ---------------------------------------------------------------------------------------------------------- items
def echo_dust():
    rnd = random.Random(5)
    img = new()
    for _ in range(46):
        x, y = int(rnd.gauss(8, 2.6)), int(rnd.gauss(10, 1.8))
        if 3 <= y <= 14:
            put(img, x, y, rnd.choice([(200, 214, 214), (160, 180, 182), (120, 210, 214)]))
    for x, y in [(6, 6), (10, 5), (8, 4)]:
        put(img, x, y, (200, 250, 250))
    return img


def sculk_antler():
    img = new()
    bone = [(226, 220, 204), (196, 188, 170), (150, 142, 126)]
    pts = [(3, 14), (4, 13), (5, 12), (6, 11), (7, 10), (8, 9), (9, 8), (10, 7), (11, 6), (12, 5), (13, 4), (13, 3)]
    for i, (x, y) in enumerate(pts):
        put(img, x, y, bone[0])
        put(img, x + 1, y + 1, bone[2])
    for x, y in [(6, 10), (6, 9), (5, 8), (5, 7), (9, 7), (9, 6), (8, 5), (12, 4), (11, 3), (11, 2)]:
        put(img, x, y, bone[1])
    for x, y in [(3, 15), (4, 14), (2, 14), (3, 13)]:            # sculk at the base
        put(img, x, y, (20, 70, 76))
    put(img, 3, 14, (60, 200, 206))
    return img


def spawn_egg():
    img = new()
    for y in range(2, 15):
        for x in range(3, 13):
            if ((x - 7.5) / 4.6) ** 2 + ((y - 8.8) / (6.6 if y > 8 else 6.9)) ** 2 <= 1:
                put(img, x, y, (222, 216, 200) if x < 9 else (196, 190, 174))
    for x, y in [(5, 6), (9, 4), (8, 10), (6, 12), (10, 12), (11, 8)]:
        put(img, x, y, (24, 80, 88))
        put(img, x + 1, y, (60, 190, 196))
    return img


# ---------------------------------------------------------------------------------------------------------- the stag
def bone_stag():
    rnd = random.Random(12)
    img = new(64, 64)
    for x in range(40):                                          # bone, top-left
        for y in range(40):
            v = rnd.randint(-8, 6)
            put(img, x, y, (214 + v, 208 + v, 190 + v))
            if (x + y * 3) % 11 == 0:
                put(img, x, y, (176, 168, 150))
    for x in range(40, 64):                                      # sculk, top-right
        for y in range(0, 20):
            c = rnd.choice([(10, 32, 38), (14, 42, 48), (8, 24, 30)])
            put(img, x, y, c)
            if rnd.random() < 0.08:
                put(img, x, y, (50, 170, 176))
    for x in range(0, 40):                                       # the skull and antlers, a little darker bone
        for y in range(40, 56):
            v = rnd.randint(-8, 6)
            put(img, x, y, (198 + v, 190 + v, 170 + v))
    # eye sockets on the skull's front (box at 0,40, 5 wide, 4 high, 7 deep: the front face spans u 7..12, v 47..51)
    for x, y in [(8, 48), (11, 48), (8, 49), (11, 49)]:
        put(img, x, y, (18, 20, 22))
    for x in range(7, 12):                                       # the nose holes and the teeth line
        put(img, x, 50, (120, 112, 98))
    glow = new(64, 64)
    for x, y in [(8, 48), (11, 48)]:
        put(glow, x, y, (120, 255, 250))
        put(glow, x, y + 1, (40, 200, 206))
    for x in range(40, 64):                                      # the sculk's specks glow a little
        for y in range(0, 20):
            if img.getpixel((x, y))[1] > 150:
                put(glow, x, y, (40, 150, 156))
    return img, glow


# ---------------------------------------------------------------------------------------------------------- moths
def moth(open_wings):
    img = new(8, 8)
    body = (230, 245, 250)
    for y in range(2, 6):
        put(img, 3, y, body)
        put(img, 4, y, body)
    wing = (170, 225, 235)
    if open_wings:
        for x, y in [(1, 2), (2, 2), (0, 3), (1, 3), (2, 3), (1, 4), (2, 4), (5, 2), (6, 2), (5, 3), (6, 3), (7, 3), (5, 4), (6, 4)]:
            put(img, x, y, wing, 210)
    else:
        for x, y in [(2, 2), (2, 3), (5, 2), (5, 3)]:
            put(img, x, y, wing, 230)
    return img


# ---------------------------------------------------------------------------------------------------------- the sky
def sculk_head():
    """A vast head of sculk filling most of a 128 square: a long face narrowing to the chin, crowned with crooked
    shrieker-like spines, tendrils dripping from under the jaw, the edges fading into the dark. Two great hollow eyes."""
    S = 128
    rnd = random.Random(99)
    img = new(S, S)
    glow = new(S, S)
    cx, cy = 64, 62

    def inside(x, y):
        dy = (y - cy) / 46.0
        half = 36 * math.sqrt(max(0.0, 1 - dy * dy)) * (1.0 - max(0.0, dy) * 0.38)
        return abs(x - cx) <= half and abs(dy) <= 1
    for x in range(S):
        for y in range(S):
            if inside(x, y):
                edge = min(abs(x - cx) / 36.0, 1.0)
                d = math.hypot((x - cx) / 36.0, (y - cy) / 46.0)
                a = int(255 * max(0.0, min(1.0, (1.08 - d) * 3.5)))
                c = rnd.choice([(8, 22, 28), (10, 30, 36), (6, 16, 22), (14, 38, 44)])
                put(img, x, y, c, a)
                if rnd.random() < 0.025:                         # sculk specks
                    put(img, x, y, (40, 140, 148), a)
                    put(glow, x, y, (20, 90, 96))
    for i in range(7):                                           # crooked spines on the crown
        bx = 34 + i * 10
        h = 14 + rnd.randint(0, 12)
        lean = rnd.uniform(-0.35, 0.35)
        for s in range(h):
            x = int(bx + lean * s)
            y = 22 - s + abs(bx - 64) // 6
            for w in range(-1, 2 if s < h - 4 else 1):
                put(img, x + w, y, (12, 34, 40), 230)
        put(glow, int(bx + lean * (h - 1)), 22 - h + abs(bx - 64) // 6, (60, 200, 206))
    for i in range(16):                                          # tendrils dripping from under the jaw
        x = 40 + i * 3 + rnd.randint(-1, 1)
        top = 96 + abs(x - 64) // 5 - 8
        length = rnd.randint(10, 26)
        for s in range(length):
            put(img, x + (1 if s % 9 > 6 else 0), top + s, (10, 28, 34), int(230 * (1 - s / length)))
        if rnd.random() < 0.5:
            put(glow, x, top + length - 2, (30, 140, 150))
    for ex in (46, 82):                                          # the eyes: hollow, glowing, without pupils
        for x in range(ex - 9, ex + 10):
            for y in range(48, 64):
                d = ((x - ex) / 9.0) ** 2 + ((y - 56) / 6.0) ** 2
                if d <= 1:
                    put(img, x, y, (170, 245, 240), 255)
                    v = 1 - d
                    put(glow, x, y, (int(120 + 135 * v), 255, int(240 + 15 * v)))
                elif d <= 1.6:
                    put(img, x, y, (4, 10, 12), 255)             # dark rim around each eye
    for x in range(50, 79):                                      # a thin crack of a mouth
        y = 86 + int(2 * math.sin((x - 50) / 4.0))
        put(img, x, y, (2, 6, 8), 255)
        if x % 5 == 0:
            put(glow, x, y, (30, 120, 126))
    return img, glow


# ---------------------------------------------------------------------------------------------------------- write it all
save(echo_flower(), "block", "echo_flower.png")
save(grass_blades(1, False), "block", "watcher_grass_bottom.png")
save(grass_blades(2, False), "block", "watcher_grass_middle.png")
save(grass_blades(3, True), "block", "watcher_grass_top.png")
save(eyes(False), "block", "watcher_eyes.png")
save(eyes(True), "block", "watcher_eyes_closed.png")
save(soul_mushroom(), "block", "soul_mushroom.png")
save(echo_dust(), "item", "echo_dust.png")
save(sculk_antler(), "item", "sculk_antler.png")
save(spawn_egg(), "item", "bone_stag_spawn_egg.png")
stag, stag_glow = bone_stag()
save(stag, "entity", "bone_stag", "bone_stag.png")
save(stag_glow, "entity", "bone_stag", "bone_stag_glow.png")
save(moth(True), "particle", "will_moth_0.png")
save(moth(False), "particle", "will_moth_1.png")
head, head_glow = sculk_head()
save(head, "environment", "sculk_head.png")
save(head_glow, "environment", "sculk_head_glow.png")
write(os.path.join(A, "particles", "will_moth.json"), {"textures": ["endrealms:will_moth_0", "endrealms:will_moth_1"]})

# block states and models
for name in ("echo_flower", "soul_mushroom"):
    write(os.path.join(A, "blockstates", name + ".json"), {"variants": {"": {"model": "endrealms:block/" + name}}})
    write(os.path.join(A, "models", "block", name + ".json"), {"parent": "minecraft:block/cross", "textures": {"cross": "endrealms:block/" + name}})
    write(os.path.join(A, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "endrealms:block/" + name}})
    write(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "endrealms:item/" + name}})

CROSS = [
    {"from": [0.8, 0, 8], "to": [15.2, 16, 8], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}, "shade": False,
     "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#cross"}, "south": {"uv": [0, 0, 16, 16], "texture": "#cross"}}},
    {"from": [8, 0, 0.8], "to": [8, 16, 15.2], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}, "shade": False,
     "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#cross"}, "east": {"uv": [0, 0, 16, 16], "texture": "#cross"}}},
]
for part in ("bottom", "middle"):
    write(os.path.join(A, "models", "block", f"watcher_grass_{part}.json"),
          {"parent": "minecraft:block/cross", "textures": {"cross": f"endrealms:block/watcher_grass_{part}"}})
for shut in (False, True):
    eye = {"from": [5, 9, 7.3], "to": [11, 11.5, 7.4], "shade": False,
           "faces": {"north": {"uv": [2, 1.5, 14, 4.5], "texture": "#eyes"}, "south": {"uv": [14, 1.5, 2, 4.5], "texture": "#eyes"}}}
    if not shut:
        eye["light_emission"] = 12
    write(os.path.join(A, "models", "block", "watcher_grass_top" + ("_closed" if shut else "") + ".json"), {
        "ambientocclusion": False,
        "textures": {"particle": "endrealms:block/watcher_grass_top", "cross": "endrealms:block/watcher_grass_top",
                     "eyes": "endrealms:block/watcher_eyes" + ("_closed" if shut else "")},
        "elements": CROSS + [eye]})
variants = {}
for part in ("bottom", "middle", "top"):
    for e, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270), ("closed", 0)):
        if part != "top":
            variants[f"eyes={e},part={part}"] = {"model": f"endrealms:block/watcher_grass_{part}"}
        else:
            v = {"model": "endrealms:block/watcher_grass_top" + ("_closed" if e == "closed" else "")}
            if rot:
                v["y"] = rot
            variants[f"eyes={e},part={part}"] = v
write(os.path.join(A, "blockstates", "watcher_grass.json"), {"variants": variants})
write(os.path.join(A, "models", "item", "watcher_grass.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "endrealms:block/watcher_grass_top"}})
write(os.path.join(A, "items", "watcher_grass.json"), {"model": {"type": "minecraft:model", "model": "endrealms:item/watcher_grass"}})
for name in ("echo_dust", "sculk_antler", "bone_stag_spawn_egg"):
    write(os.path.join(A, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "endrealms:item/" + name}})
    write(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "endrealms:item/" + name}})

# loot tables
LT = os.path.join(D, "loot_table")
write(os.path.join(LT, "blocks", "soul_mushroom.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
      "entries": [{"type": "minecraft:item", "name": "endrealms:soul_mushroom"}]}], "random_sequence": "endrealms:blocks/soul_mushroom"})
write(os.path.join(LT, "blocks", "echo_flower.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
    {"type": "minecraft:item", "condition": "minecraft:tool/can_shear", "name": "endrealms:echo_flower"},
    {"type": "minecraft:item", "name": "endrealms:echo_dust", "modifier": {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}}]}]}],
    "random_sequence": "endrealms:blocks/echo_flower"})
write(os.path.join(LT, "blocks", "watcher_grass.json"), {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:match_block", "blocks": "endrealms:watcher_grass", "state": {"part": "bottom"}},
    "entries": [{"type": "minecraft:item", "condition": "minecraft:tool/can_shear", "name": "endrealms:watcher_grass"}]}],
    "random_sequence": "endrealms:blocks/watcher_grass"})
write(os.path.join(LT, "entities", "bone_stag.json"), {"type": "minecraft:entity", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "endrealms:sculk_antler"}]},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:bone",
                              "modifier": {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}}]}],
    "random_sequence": "endrealms:entities/bone_stag"})

preview = Image.new("RGBA", (128 * 4, 128 * 2), (30, 34, 36, 255))
for i, im in enumerate([echo_flower(), grass_blades(3, True), soul_mushroom(), sculk_antler()]):
    preview.paste(im.resize((128, 128), Image.NEAREST), (128 * i, 0), im.resize((128, 128), Image.NEAREST))
preview.paste(head.resize((128, 128)), (0, 128), head.resize((128, 128)))
preview.paste(stag.resize((128, 128), Image.NEAREST), (128, 128))
preview.paste(eyes(False).resize((128, 128), Image.NEAREST), (256, 128), eyes(False).resize((128, 128), Image.NEAREST))
preview.paste(moth(True).resize((128, 128), Image.NEAREST), (384, 128), moth(True).resize((128, 128), Image.NEAREST))
preview.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "preview_sculk_life.png"))
print("sculk life written")
