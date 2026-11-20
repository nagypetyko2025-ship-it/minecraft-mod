"""Draws the Void Sentinel skin (64x64 humanoid layout) + glow layer and its spawn egg (original pixel art, CC0).
usage: python draw_void_sentinel.py <assets/endrealms/textures dir> [preview.png]"""
import os
import random
import sys

from PIL import Image

OUT = sys.argv[1]
rnd = random.Random(77)


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


ARMOR = [hexc(c) for c in ("#1E1529", "#2B1F3B", "#3A2A50", "#4B3866")]
TRIM = [hexc(c) for c in ("#7E68A3", "#9682B9", "#B3A0D0")]
GLOW = [hexc("#F2A8EE"), hexc("#FFD6F8")]
VISOR = hexc("#0C0812")
HORN = [hexc("#E4E6AE"), hexc("#C4C68A")]

skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def fill(rect, pal):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            skin.putpixel((x, y), pal[rnd.randrange(len(pal))])


def px(x, y, c, lit=False):
    skin.putpixel((x, y), c)
    if lit:
        glow.putpixel((x, y), c)


def box(u, v, w, h, d, pal):
    f = faces(u, v, w, h, d)
    for rect in f.values():
        if rect[2] > 0 and rect[3] > 0:
            fill(rect, pal)
    return f


def row(rect, ry, c, lit=False):
    x0, y0, w, h = rect
    for x in range(x0, x0 + w):
        px(x, y0 + ry, c, lit)


# head: dark helmet, black visor band with glowing eyes, trim ridge on top
head = box(0, 0, 8, 8, 8, ARMOR)
for name in ("front", "right", "left", "back"):
    row(head[name], 7, TRIM[0])
fx, fy, fw, fh = head["front"]
for x in range(fx, fx + fw):
    px(x, fy + 3, VISOR)
    px(x, fy + 4, VISOR)
for ex in (fx + 1, fx + 2, fx + 5, fx + 6):
    px(ex, fy + 3, GLOW[1], True)
for y in range(fy + 5, fy + 8):
    px(fx + 3, y, TRIM[1]); px(fx + 4, y, TRIM[1])      # nose guard
tx, ty, tw, th = head["top"]
for y in range(ty, ty + th):
    px(tx + 3, y, TRIM[2]); px(tx + 4, y, TRIM[2])      # crest

# body: dark plates, trim belt and centre line, glowing chest crystal
body = box(16, 16, 8, 12, 4, ARMOR)
bx, by_, bw, bh = body["front"]
for y in range(by_, by_ + bh):
    px(bx + 3, y, TRIM[0]); px(bx + 4, y, TRIM[0])
row(body["front"], 0, TRIM[1]); row(body["back"], 0, TRIM[1])
for name in ("front", "back", "right", "left"):
    row(body[name], 9, TRIM[2])
for dx in (3, 4):
    for dy in (3, 4):
        px(bx + dx, by_ + dy, GLOW[(dx + dy) % 2], True)
px(bx + 3, by_ + 2, GLOW[0], True); px(bx + 4, by_ + 5, GLOW[0], True)

# arms: dark with lighter gauntlets
arm = box(40, 16, 4, 12, 4, ARMOR)
for name in ("front", "back", "right", "left"):
    for ry in (9, 10, 11):
        row(arm[name], ry, TRIM[1] if ry != 9 else TRIM[2])
fill(arm["bottom"], [TRIM[0]])

# legs: dark with knee guard and trimmed boots
leg = box(0, 16, 4, 12, 4, ARMOR)
for name in ("front", "back", "right", "left"):
    row(leg[name], 4, TRIM[0])
    row(leg[name], 10, TRIM[1]); row(leg[name], 11, TRIM[0])
fill(leg["bottom"], [ARMOR[0]])

# horns: pale end-stone colour with glowing tips
for u in (0, 4):
    hf = box(u, 32, 1, 5, 1, HORN)
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = hf[name]
        px(x0, y0, GLOW[0], True)
    fill(hf["top"], [GLOW[1]])
    glow.putpixel(hf["top"][:2], GLOW[1])

# shoulder plates: purple trim plates with dark rims
pad = box(0, 40, 5, 3, 6, [TRIM[0], TRIM[1]])
for name in ("front", "back", "right", "left"):
    row(pad[name], 2, ARMOR[1])

os.makedirs(f"{OUT}/entity/void_sentinel", exist_ok=True)
skin.save(f"{OUT}/entity/void_sentinel/void_sentinel.png")
glow.save(f"{OUT}/entity/void_sentinel/void_sentinel_glow.png")

EGG = [
    "................",
    "......oooo......",
    ".....oCCCCo.....",
    "....oCCPCCCo....",
    "....oCPPCCCo....",
    "...oCCCCCPPCo...",
    "...oCCCCCPPCo...",
    "...oCPCCCCCCo...",
    "..oCPPCCCPCCCo..",
    "..oCCCCCCPPCCo..",
    "..oCCCPCCCCCCo..",
    "..odCCPPCCCCdo..",
    "...odCCCCCCdo...",
    "....oddddddo....",
    ".....oooooo.....",
    "................",
]
egg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
pal = {"o": hexc("#0C0812"), "C": ARMOR[2], "d": ARMOR[1], "P": GLOW[0]}
for y, r in enumerate(EGG):
    for x, ch in enumerate(r):
        if ch in pal:
            egg.putpixel((x, y), pal[ch])
egg.save(f"{OUT}/item/void_sentinel_spawn_egg.png")

if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (64 * 5 + 96 + 24, 64 * 5 + 16), (60, 60, 70, 255))
    big = skin.resize((320, 320), Image.NEAREST)
    sheet.paste(big, (8, 8), big)
    e = egg.resize((96, 96), Image.NEAREST)
    sheet.paste(e, (336, 8), e)
    sheet.save(sys.argv[2])
print("ok")
