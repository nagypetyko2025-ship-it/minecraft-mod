"""Textures for the Aging Barrel (CC0, drawn for End Realms).
A dark oak cask of vertical staves with two black iron hoops. The front is the round barrel head (horizontal boards in a
ring of iron) with a small wooden tap; the top is the same head without the tap, with a bung; the bottom is plain boards."""
import random
from PIL import Image

BLOCK = r"D:\minecraft mod\endrealms\src\main\resources\assets\endrealms\textures\block"

WOOD = [(92, 62, 38), (84, 56, 34), (100, 68, 42), (78, 52, 32)]
HEAD = [(118, 82, 50), (108, 74, 46), (126, 88, 54)]
GAP = (48, 32, 20)
IRON = (74, 74, 80)
IRON_LIGHT = (128, 128, 136)
IRON_DARK = (40, 40, 44)
TAP = (214, 170, 84)
TAP_DARK = (140, 104, 44)


def px(img, x, y, c):
    img.putpixel((x, y), tuple(c) + (255,))


def jitter(rnd, c, n=5):
    return tuple(max(0, min(255, v + rnd.randint(-n, n))) for v in c)


def staves(seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            px(img, x, y, jitter(rnd, WOOD[(x // 4 + seed) % len(WOOD)]))
        if x % 4 == 3:
            for y in range(16):
                px(img, x, y, GAP)
    for y in (2, 13):                                  # iron hoops with rivets
        for x in range(16):
            px(img, x, y, IRON)
            px(img, x, y - 1, IRON_LIGHT if x % 6 else IRON)
            px(img, x, y + 1, IRON_DARK)
        for x in (2, 9):
            px(img, x, y, IRON_LIGHT)
    return img


def head(seed, tap):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    cx = cy = 7.5
    for x in range(16):
        for y in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= 6.2:
                c = HEAD[(y // 3) % len(HEAD)]
                px(img, x, y, GAP if y % 3 == 2 else jitter(rnd, c, 4))
            elif d <= 7.6:
                px(img, x, y, IRON_LIGHT if (x + y) % 7 == 0 else IRON if y < cy else IRON_DARK)
            else:
                px(img, x, y, jitter(rnd, WOOD[(x // 4) % len(WOOD)]))
    if tap:
        for x in range(6, 10):                         # the tap: a short spout with a handle on top
            px(img, x, 10, TAP)
            px(img, x, 11, TAP_DARK)
        px(img, 6, 12, TAP_DARK)
        px(img, 7, 12, TAP)
        px(img, 8, 9, TAP)
        px(img, 8, 8, TAP_DARK)
        px(img, 7, 8, TAP)
        px(img, 9, 8, TAP)
    else:
        for x, y in [(7, 7), (8, 7), (7, 8), (8, 8)]:  # the bung
            px(img, x, y, TAP_DARK)
        px(img, 7, 7, TAP)
    return img


def bottom():
    rnd = random.Random(9)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            px(img, x, y, GAP if y % 4 == 3 else jitter(rnd, WOOD[(y // 4) % len(WOOD)]))
    return img


staves(2).save(BLOCK + r"\aging_barrel_side.png")
head(3, True).save(BLOCK + r"\aging_barrel_front.png")
head(4, False).save(BLOCK + r"\aging_barrel_top.png")
bottom().save(BLOCK + r"\aging_barrel_bottom.png")
print("ok")
