"""Sculk Soil and Sculk Stone (CC0, drawn for End Realms), plus their models, item definitions and loot tables.
Sculk Soil: the dark blue-black of sculk, but crumbly and earthy - grains in two darker tones, a few dull teal flecks
and no glowing pulse. Sculk Stone: deepslate-dark rock in rough horizontal layers, with thin teal veins of sculk
running through the cracks. Writes a preview next to this script."""
import json
import os
import random
from PIL import Image

ASSETS = r"D:\minecraft mod\endrealms\src\main\resources\assets\endrealms"
DATA = r"D:\minecraft mod\endrealms\src\main\resources\data\endrealms"


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def soil():
    rnd = random.Random(41)
    img = Image.new("RGBA", (16, 16))
    base = [(10, 22, 30), (13, 28, 37), (8, 17, 24), (16, 31, 38)]
    for x in range(16):
        for y in range(16):
            c = base[rnd.randrange(len(base))]
            c = tuple(v + rnd.randint(-2, 2) for v in c)
            img.putpixel((x, y), clamp(c) + (255,))
    for _ in range(14):                                   # darker clods
        x, y = rnd.randrange(16), rnd.randrange(16)
        img.putpixel((x, y), (5, 11, 15, 255))
        if x < 15:
            img.putpixel((x + 1, y), (6, 13, 18, 255))
    for _ in range(7):                                    # dull teal flecks of old sculk
        x, y = rnd.randrange(16), rnd.randrange(16)
        img.putpixel((x, y), (28, 86, 92, 255) if rnd.random() < 0.6 else (40, 112, 118, 255))
    return img


def stone():
    rnd = random.Random(77)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        band = 36 + (6 if (y // 3) % 2 else 0)            # rough layers like deepslate
        for x in range(16):
            v = band + rnd.randint(-5, 5)
            img.putpixel((x, y), clamp((v - 4, v, v + 3)) + (255,))
        if y % 3 == 2:
            for x in range(16):
                if rnd.random() < 0.7:
                    img.putpixel((x, y), (24, 27, 30, 255))
    x, y = 5 + rnd.randrange(6), 0                        # a vein of sculk wandering down through the cracks
    for step in range(22):
        img.putpixel((x % 16, y % 16), (22, 96, 104, 255) if step % 4 else (54, 150, 156, 255))
        x += rnd.choice((-1, 0, 1))
        y += 1 if rnd.random() < 0.75 else 0
    return img


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


textures = {"sculk_soil": soil(), "sculk_stone": stone()}
preview = Image.new("RGBA", (16 * 2 * 8, 16 * 8))
for i, (name, img) in enumerate(textures.items()):
    img.save(os.path.join(ASSETS, "textures", "block", name + ".png"))
    preview.paste(img.resize((128, 128), Image.NEAREST), (128 * i, 0))
    write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "endrealms:block/" + name}}})
    write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "endrealms:block/" + name}})
    write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "endrealms:block/" + name}})
    write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": "endrealms:" + name}]}],
        "random_sequence": "endrealms:blocks/" + name})
preview.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "preview_sculk_ground.png"))
print("sculk ground written")
