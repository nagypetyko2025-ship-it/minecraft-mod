"""Draws the Astral wood textures (original pixel art, CC0): log side/top, planks, leaves, sapling."""
import random, sys
from PIL import Image

OUT = sys.argv[1]  # .../assets/endrealms/textures/block

def hexc(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

BARK = [hexc(c) for c in ("#2A1739", "#3A2250", "#4D2E68", "#623D82", "#7A4F9C")]
VEIN = hexc("#C46AD6")
WOOD = [hexc(c) for c in ("#6E4C8A", "#8A64A6", "#A27BBE", "#B993CF", "#CBA9DD")]
PLANK = [hexc(c) for c in ("#5A3C74", "#7F5C9E", "#9373B4", "#A586C3", "#B79AD1")]
LEAF = [hexc(c) for c in ("#5E1F6C", "#7E2C8C", "#A23DAE", "#C65AC8", "#E585E0")]

def log_side(rnd):
    img = Image.new("RGBA", (16, 16))
    col = [rnd.choice([1, 2, 2, 3]) for _ in range(16)]
    for x in range(16):  # grooves: some columns are dark crevices
        if rnd.random() < 0.3:
            col[x] = 0
    for x in range(16):
        for y in range(16):
            v = col[x] + (1 if rnd.random() < 0.18 else 0) - (1 if rnd.random() < 0.12 else 0)
            img.putpixel((x, y), BARK[max(0, min(4, v))])
    for _ in range(3):  # short glowing veins running along the grain
        x, y = rnd.randrange(16), rnd.randrange(16)
        for k in range(rnd.randint(2, 3)):
            img.putpixel((x, (y + k) % 16), VEIN)
    return img

def log_top(rnd):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                c = BARK[rnd.choice([0, 1, 2])]
            else:
                ring = max(abs(x - 7.5), abs(y - 7.5))  # squarish rings like real end-grain pixel art
                r = int(ring)
                c = WOOD[1] if r in (2, 5) else WOOD[3] if r in (3, 6) else WOOD[2]
                if r <= 1:
                    c = WOOD[0] if r == 0 else WOOD[1]
                if rnd.random() < 0.08:
                    c = WOOD[4]
            img.putpixel((x, y), c)
    return img

def planks(rnd):
    img = Image.new("RGBA", (16, 16))
    joints = [rnd.randrange(3, 13) for _ in range(4)]
    for b in range(4):  # four boards, 4px each, last row of each board is a seam
        tone = rnd.choice([2, 2, 3])
        for y in range(b * 4, b * 4 + 4):
            for x in range(16):
                if y == b * 4 + 3:
                    c = PLANK[0]
                elif x == joints[b]:
                    c = PLANK[0] if y != b * 4 else PLANK[1]
                else:
                    v = tone
                    if y == b * 4:
                        v += 1  # top edge of a board catches light
                    if rnd.random() < 0.15:
                        v -= 1  # grain
                    c = PLANK[max(1, min(4, v))]
                img.putpixel((x, y), c)
    return img

def leaves(rnd):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    v = [[rnd.random() for _ in range(16)] for _ in range(16)]
    sm = [[(v[y][x] * 2 + v[y][(x + 1) % 16] + v[(y + 1) % 16][x] + v[y][(x - 1) % 16] + v[(y - 1) % 16][x]) / 6
           for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            t = sm[y][x]
            if t < 0.30:
                continue  # small holes between leaf clusters (visible with fancy graphics)
            i = 0 if t < 0.36 else 1 if t < 0.45 else 2 if t < 0.55 else 3 if t < 0.66 else 4
            # light from the top-left: brighten a pixel if the one up-left is darker cluster edge
            img.putpixel((x, y), LEAF[i])
    return img

SAP = {'D': hexc("#2A1739"), 'B': hexc("#4D2E68"), 'b': hexc("#7A4F9C"),
       'l': LEAF[1], 'L': LEAF[2], 'P': LEAF[3], 'H': LEAF[4]}
SAPLING = [
    "................",
    "......lPPl......",
    ".....lPHHPl.....",
    "....lLPPPPLl....",
    "...lPPLHPLPPl...",
    "...lLPPLLPPLl...",
    "....lLLbBLLl....",
    "..lPl.lbBl.lPl..",
    ".lPHPl.bB.lPHPl.",
    ".lLPL..bB..LPLl.",
    "..ll.bbbB...ll..",
    "......bB........",
    ".......bB.......",
    ".......bB.......",
    "......DbBD......",
    "................",
]

def sapling():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(SAPLING):
        assert len(row) == 16
        for x, ch in enumerate(row):
            if ch in SAP:
                img.putpixel((x, y), SAP[ch])
    return img

def stripped_side(rnd):
    img = Image.new("RGBA", (16, 16))
    col = [rnd.choice([2, 2, 3, 3, 4]) for _ in range(16)]
    for x in range(16):
        for y in range(16):
            v = col[x] - (1 if rnd.random() < 0.15 else 0)
            img.putpixel((x, y), WOOD[max(1, v)])
    return img

def stripped_top(rnd):
    img = log_top(rnd)
    for i in range(16):  # no bark: thin darker wood rim instead
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), WOOD[1])
    return img

CLEAR = (0, 0, 0, 0)
def door_half(rnd, top):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or (top and y == 0) or (not top and y == 15):
                c = PLANK[0]                                   # frame
            elif top and 2 <= y <= 7 and (2 <= x <= 6 or 9 <= x <= 13):
                c = CLEAR                                      # two window panes
            elif x in (7, 8) and top and 2 <= y <= 7:
                c = PLANK[1]                                   # window mullion
            elif (top and y in (1, 8)) or (not top and y in (7, 8)):
                c = PLANK[1]                                   # cross rails
            else:
                c = PLANK[3] if (x // 3) % 2 == 0 else PLANK[2]  # vertical boards
                if rnd.random() < 0.1:
                    c = PLANK[1]
            img.putpixel((x, y), c)
    if not top:
        img.putpixel((12, 1), VEIN); img.putpixel((12, 2), PLANK[0])  # glowing handle
    return img

def trapdoor(rnd):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = PLANK[0]
            elif x in (7, 8) or y in (7, 8):
                c = PLANK[2]
            elif 2 <= x <= 5 or 10 <= x <= 13:
                c = CLEAR if (2 <= y <= 5 or 10 <= y <= 13) else PLANK[3]
            else:
                c = PLANK[3]
            img.putpixel((x, y), c)
    return img

DOOR_ITEM = [
    "....FFFFFFFF....",
    "....FwwFwwwF....",
    "....F..F...F....",
    "....F..F...F....",
    "....FwwFwwwF....",
    "....FFFFFFFF....",
    "....FbBbBbBF....",
    "....FbBbBbBF....",
    "....FbBbBbBF....",
    "....FFFFFFFF....",
    "....FbBbBhBF....",
    "....FbBbBbBF....",
    "....FbBbBbBF....",
    "....FbBbBbBF....",
    "....FbBbBbBF....",
    "....FFFFFFFF....",
]
def door_item():
    pal = {"F": PLANK[0], "w": PLANK[1], "b": PLANK[2], "B": PLANK[3], "h": VEIN}
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(DOOR_ITEM):
        assert len(row) == 16
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img

rnd = random.Random(2026)
made = {"astral_log": log_side(rnd), "astral_log_top": log_top(rnd), "astral_planks": planks(rnd),
        "astral_leaves": leaves(rnd), "astral_sapling": sapling()}
r2 = random.Random(99)
made.update({"stripped_astral_log": stripped_side(r2), "stripped_astral_log_top": stripped_top(r2),
             "astral_door_top": door_half(r2, True), "astral_door_bottom": door_half(r2, False),
             "astral_trapdoor": trapdoor(r2)})
for n, im in made.items():
    im.save(f"{OUT}/{n}.png")
door_item().save(OUT.replace("textures/block", "textures/item") + "/astral_door.png")
made["astral_door_item"] = door_item()
if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (len(made) * 168 + 8, 2 * 168 + 8), (40, 40, 48, 255))
    for i, im in enumerate(made.values()):
        big = im.resize((160, 160), Image.NEAREST)
        sheet.paste(big, (8 + i * 168, 8), big)
        tile = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        for a in (0, 16):
            for b in (0, 16):
                tile.paste(im, (a, b))
        t = tile.resize((160, 160), Image.NEAREST)
        sheet.paste(t, (8 + i * 168, 176), t)
    sheet.save(sys.argv[2])
print("ok")
