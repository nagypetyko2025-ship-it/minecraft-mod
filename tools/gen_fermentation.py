"""Textures for the Fermentation Station and Apple Brandy (CC0, drawn for End Realms).
The station: an oak cask of vertical staves with two copper hoops; the front has a small round glass window above a copper
tap, empty when idle and amber with bubbles while fermenting. The top is the lid with a copper rim and a glass airlock.
Apple Brandy: a slim clear bottle with a cork, golden-amber brandy and a small red apple on a cream label."""
import random
from PIL import Image

BLOCK = r"D:\minecraft mod\endrealms\src\main\resources\assets\endrealms\textures\block"
ITEM = r"D:\minecraft mod\endrealms\src\main\resources\assets\endrealms\textures\item"
EFFECT = r"D:\minecraft mod\endrealms\src\main\resources\assets\endrealms\textures\mob_effect"

WOOD = [(150, 104, 62), (139, 95, 56), (160, 112, 68), (128, 88, 52)]
WOOD_DARK = (92, 62, 36)
GAP = (70, 46, 26)
COPPER = (196, 112, 76)
COPPER_LIGHT = (231, 160, 122)
COPPER_DARK = (128, 70, 46)
GLASS = (176, 214, 222)
GLASS_DARK = (96, 132, 142)
AMBER = (232, 163, 58)
AMBER_DARK = (176, 104, 30)
AMBER_LIGHT = (252, 214, 120)


def staves(seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        stave = x // 4
        for y in range(16):
            base = WOOD[(stave + seed) % len(WOOD)]
            jitter = rnd.randint(-6, 6)
            img.putpixel((x, y), tuple(max(0, min(255, c + jitter)) for c in base) + (255,))
        if x % 4 == 3:
            for y in range(16):
                img.putpixel((x, y), GAP + (255,))
    for y in (3, 12):                                   # copper hoops
        for x in range(16):
            img.putpixel((x, y), COPPER + (255,))
            img.putpixel((x, y - 1), COPPER_LIGHT + (255,) if x % 5 else COPPER + (255,))
            img.putpixel((x, y + 1), COPPER_DARK + (255,))
    return img


def front(on):
    img = staves(1)
    cx, cy = 7.5, 7.0
    for x in range(16):
        for y in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= 3.6:
                if not on:
                    img.putpixel((x, y), (GLASS if y < cy else GLASS_DARK) + (255,))
                else:
                    level = AMBER_LIGHT if y < cy - 1 else AMBER if y < cy + 1.5 else AMBER_DARK
                    img.putpixel((x, y), level + (255,))
            elif d <= 4.5:
                img.putpixel((x, y), (COPPER_DARK if y > cy else COPPER) + (255,))
    if on:
        for x, y in [(6, 8), (8, 6), (9, 9), (7, 5)]:   # bubbles
            img.putpixel((x, y), (255, 240, 200, 255))
    else:
        img.putpixel((6, 5), (235, 250, 255, 255))      # glint
    for x, y in [(7, 12), (8, 12), (7, 13), (8, 13), (7, 14), (8, 14), (9, 14)]:   # the tap
        img.putpixel((x, y), (COPPER_LIGHT if y == 12 else COPPER) + (255,))
    img.putpixel((9, 15), COPPER_DARK + (255,))
    return img


def top():
    rnd = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            base = WOOD[(y // 4) % len(WOOD)]
            img.putpixel((x, y), tuple(max(0, min(255, c + rnd.randint(-6, 6))) for c in base) + (255,))
        img.putpixel((x, 3), GAP + (255,))
        img.putpixel((x, 7), GAP + (255,))
        img.putpixel((x, 11), GAP + (255,))
    for i in range(16):                                 # copper rim
        for edge in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(edge, COPPER + (255,))
    for x, y in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        img.putpixel((x, y), COPPER_DARK + (255,))
    for x in range(6, 10):                              # glass airlock in the middle
        for y in range(6, 10):
            img.putpixel((x, y), (GLASS if (x + y) % 3 else GLASS_DARK) + (255,))
    for x, y in [(5, 6), (5, 9), (10, 6), (10, 9), (6, 5), (9, 5), (6, 10), (9, 10)]:
        img.putpixel((x, y), COPPER_DARK + (255,))
    img.putpixel((7, 7), (240, 252, 255, 255))
    return img


def bottom():
    rnd = random.Random(3)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            base = WOOD[(x // 4) % len(WOOD)]
            img.putpixel((x, y), tuple(max(0, min(255, c - 18 + rnd.randint(-5, 5))) for c in base) + (255,))
    return img


LIQUIDS = {
    "apple_brandy": ((252, 214, 120), (232, 163, 58), (176, 104, 30)),
    "glow_berry_brandy": ((255, 236, 140), (255, 196, 64), (214, 140, 30)),
    "sweet_berry_brandy": ((232, 96, 104), (196, 44, 60), (128, 18, 34)),
    "moonbell_nectar_brandy": ((214, 255, 247), (139, 239, 226), (47, 159, 165)),
    "starberry_brandy": ((206, 168, 244), (150, 95, 210), (98, 56, 150)),
    "fruit_medley_brandy": ((250, 170, 120), (226, 110, 70), (160, 60, 40)),
    "honeyed_apple_brandy": ((255, 222, 110), (240, 180, 40), (190, 120, 20)),
    "glowing_nectar_brandy": ((236, 255, 190), (180, 236, 120), (96, 170, 80)),
    "chorus_berry_brandy": ((230, 190, 240), (180, 120, 200), (110, 70, 140)),
    "melon_refresher": ((255, 170, 170), (236, 92, 96), (170, 40, 52)),
    "endless_brandy": ((200, 236, 255), (120, 170, 250), (70, 90, 200)),
    "miners_brandy": ((255, 214, 150), (214, 150, 80), (130, 84, 40)),
    "dragon_blood_brandy": ((220, 70, 70), (150, 20, 30), (80, 6, 16)),
    "forest_elixir": ((180, 220, 130), (110, 160, 70), (60, 100, 40)),
    "void_essence": ((120, 100, 150), (60, 40, 90), (24, 14, 40)),
}
LABELS = {   # the little picture on the cream label: (pixels, colour)
    "apple_brandy": ([(7, 10), (8, 10), (7, 11), (8, 11)], (196, 40, 36)),
    "glow_berry_brandy": ([(7, 10), (8, 11), (7, 11)], (255, 170, 40)),
    "sweet_berry_brandy": ([(7, 10), (8, 10), (8, 11), (7, 11)], (176, 22, 40)),
    "moonbell_nectar_brandy": ([(7, 10), (8, 10), (6, 11), (7, 11), (8, 11), (9, 11)], (47, 159, 165)),
    "starberry_brandy": ([(7, 9), (6, 10), (7, 10), (8, 10), (7, 11)], (150, 95, 210)),
    "fruit_medley_brandy": ([(6, 10), (7, 10), (8, 11), (9, 11)], (196, 40, 36)),
    "honeyed_apple_brandy": ([(7, 10), (8, 10), (7, 11), (8, 11)], (230, 160, 20)),
    "glowing_nectar_brandy": ([(7, 9), (6, 10), (8, 10), (7, 11)], (110, 200, 90)),
    "chorus_berry_brandy": ([(6, 10), (7, 10), (8, 10), (7, 11), (8, 9)], (150, 90, 170)),
    "melon_refresher": ([(6, 11), (7, 10), (8, 10), (9, 11), (7, 11), (8, 11)], (80, 160, 60)),
    "endless_brandy": ([(6, 10), (7, 11), (8, 10), (9, 11)], (70, 100, 210)),
    "miners_brandy": ([(6, 9), (7, 10), (8, 11), (9, 12), (7, 9), (6, 10)], (110, 110, 120)),
    "dragon_blood_brandy": ([(7, 9), (7, 10), (6, 11), (8, 11)], (150, 20, 30)),
    "forest_elixir": ([(7, 9), (6, 10), (7, 10), (8, 10), (7, 11), (7, 12)], (60, 120, 50)),
    "void_essence": ([(7, 10), (8, 10), (7, 11), (8, 11), (6, 10), (9, 11)], (40, 20, 70)),
}


def brandy(name):
    light, mid, dark = LIQUIDS[name]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    outline = (60, 74, 80, 255)
    for y in range(1, 4):                               # cork
        for x in (7, 8):
            img.putpixel((x, y), (150, 108, 70, 255) if y > 1 else (120, 84, 52, 255))
    for y in range(4, 6):                               # neck
        img.putpixel((6, y), outline)
        img.putpixel((9, y), outline)
        img.putpixel((7, y), GLASS + (200,))
        img.putpixel((8, y), GLASS + (160,))
    body = [(5, 6, 10), (4, 7, 11), (4, 8, 11), (4, 9, 11), (4, 10, 11), (4, 11, 11), (4, 12, 11), (4, 13, 11), (5, 14, 10)]
    for x0, y, x1 in body:
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y == 14
            if edge:
                img.putpixel((x, y), outline)
            elif y <= 7:
                img.putpixel((x, y), GLASS + (150,))
            else:
                img.putpixel((x, y), (light if x == x0 + 1 else mid if y < 12 else dark) + (255,))
    for x in range(6, 10):                              # cream label with the fruit on it
        for y in range(9, 13):
            img.putpixel((x, y), (238, 226, 196, 255))
    pixels, colour = LABELS[name]
    for x, y in pixels:
        img.putpixel((x, y), colour + (255,))
    if name in ("apple_brandy", "glow_berry_brandy", "sweet_berry_brandy"):
        img.putpixel((8, 9), (70, 140, 50, 255))        # leaf
    img.putpixel((6, 8), (255, 250, 230, 255))          # glint
    return img


def drunk_icon():
    """18x18 effect icon: a tipped-over bottle with a few bubbles drifting off."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    bottle = brandy("apple_brandy").rotate(-35, resample=Image.NEAREST, expand=False)
    img.paste(bottle, (1, 2), bottle)
    for x, y, c in [(13, 3, (255, 240, 200)), (15, 6, (255, 226, 150)), (12, 1, (255, 250, 230)), (16, 2, (255, 214, 120))]:
        img.putpixel((x, y), c + (255,))
    return img


staves(0).save(f"{BLOCK}\\fermentation_station_side.png")
front(False).save(f"{BLOCK}\\fermentation_station_front.png")
front(True).save(f"{BLOCK}\\fermentation_station_front_on.png")
top().save(f"{BLOCK}\\fermentation_station_top.png")
bottom().save(f"{BLOCK}\\fermentation_station_bottom.png")
for brandy_name in LIQUIDS:
    brandy(brandy_name).save(f"{ITEM}\\{brandy_name}.png")
import os
os.makedirs(EFFECT, exist_ok=True)
drunk_icon().save(f"{EFFECT}\\drunkenness.png")
print("ok")
