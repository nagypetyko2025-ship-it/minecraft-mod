"""Draws void sand + the void sandstone family and the kelp-like starweed (original pixel art, CC0).
usage: python draw_void_sand.py <assets/endrealms/textures/block dir> [preview.png]"""
import random
import sys

from PIL import Image

OUT = sys.argv[1]
rnd = random.Random(9)


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


SAND = [hexc(c) for c in ("#C7C98B", "#D6D89C", "#E2E4AB", "#ECEEBB", "#F5F6CC")]
SPECK = [hexc("#C98BDC"), hexc("#E8A6E6"), hexc("#B477CF")]


def grain(lo=0, hi=4, speck=0.0):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = SAND[rnd.randint(lo, hi)]
            if rnd.random() < speck:
                c = SPECK[rnd.randrange(len(SPECK))]
            img.putpixel((x, y), c)
    return img


def void_sand():
    img = grain(1, 4, 0.06)  # end-stone coloured grains with lilac/pink specks
    for _ in range(3):        # a few small pink clusters
        x, y = rnd.randrange(15), rnd.randrange(15)
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            img.putpixel((x + dx, y + dy), SPECK[1])
    return img


def sandstone_side():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y < 3:
                v = rnd.randint(3, 4)            # weathered light top band
            elif y >= 12:
                v = rnd.randint(0, 1)            # darker base band
            else:
                v = 2 + (1 if (y % 3 == 0) else 0) - (1 if rnd.random() < 0.15 else 0)
            img.putpixel((x, y), SAND[max(0, min(4, v))])
    for x in range(16):                          # thin pink strata line
        if rnd.random() < 0.8:
            img.putpixel((x, 7), SPECK[1] if rnd.random() < 0.5 else SPECK[0])
    return img


def sandstone_top():
    return grain(2, 4, 0.02)


def sandstone_bottom():
    img = grain(0, 2, 0.03)
    for _ in range(4):                           # little cracks
        x, y = rnd.randrange(14), rnd.randrange(14)
        img.putpixel((x, y), SAND[0]); img.putpixel((x + 1, y + 1), SAND[0])
    return img


def cut_sandstone():
    img = sandstone_top()
    for i in range(16):
        img.putpixel((i, 0), SAND[4]); img.putpixel((0, i), SAND[4])
        img.putpixel((i, 15), SAND[0]); img.putpixel((15, i), SAND[0])
    for x in range(1, 15):                       # two carved grooves
        img.putpixel((x, 5), SAND[1]); img.putpixel((x, 10), SAND[1])
        img.putpixel((x, 6), SAND[4]); img.putpixel((x, 11), SAND[4])
    return img


EYE = [
    "................",
    "................",
    "................",
    "................",
    ".....PPPPPP.....",
    "...PPppppppPP...",
    "..PppwwVVwwppP..",
    "..PpwVVVVVVwpP..",
    "..PppwwVVwwppP..",
    "...PPppppppPP...",
    ".....PPPPPP.....",
    "................",
    "................",
    "................",
    "................",
    "................",
]


def chiseled():
    img = cut_sandstone()
    pal = {"P": hexc("#8A4DBA"), "p": hexc("#C98BDC"), "w": hexc("#FFE9FB"), "V": hexc("#3B2157")}
    for y, row in enumerate(EYE):               # carved ender-eye motif
        for x, ch in enumerate(row):
            if ch in pal and 0 < x < 15:
                img.putpixel((x, y), pal[ch])
    return img


# --- starweed: kelp-like, broad wavy lilac fronds around a stem; tip has star-shaped glowing buds
WEED = {"S": hexc("#6E4C9E"), "s": hexc("#8E68BE"), "L": hexc("#B793DE"), "l": hexc("#D6B8F2"),
        "h": hexc("#ECDDFB"), "*": hexc("#FFF6C8")}
BODY = [
    "LL.....Ss.....ll",
    "lLL....Ss....lLL",
    ".lLL...Ss...lLL.",
    "..lLL..Ss..LLl..",
    "...hLLsSsLLh....",
    "......sSs.......",
    "ll....sS....lL..",
    "LLl...Ss...lLLl.",
    ".lLL..Ss..LLLl..",
    "..lLLLSsLLLl....",
    "....hlSsl.......",
    "......Ss......ll",
    ".Ll...sS....lLL.",
    "lLLl..sS...lLLl.",
    ".hLLL.Ss.LLLh...",
    "...lLLSsLLl.....",
]
TOP = [
    "....*.....*.....",
    "...hlh...hlh....",
    "....l..*..l.....",
    "....Ll.l.lL.....",
    ".*...Ll.lL...*..",
    ".hl...LsL...lh..",
    "..lL..sSs..Ll...",
    "...lLLsSsLLl....",
    "......Ss........",
    "ll....Ss....lL..",
    "LLl...sS...lLLl.",
    ".lLL..sS..LLLl..",
    "..lLLLSsLLLl....",
    "....hlSsl.......",
    "......Ss......ll",
    ".Ll...sS....lLL.",
]


def plant(rows):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch in WEED:
                img.putpixel((x, y), WEED[ch])
    return img


made = {"void_sand": void_sand(), "void_sandstone": sandstone_side(), "void_sandstone_top": sandstone_top(),
        "void_sandstone_bottom": sandstone_bottom(), "cut_void_sandstone": cut_sandstone(),
        "chiseled_void_sandstone": chiseled(), "starweed": plant(BODY), "starweed_top": plant(TOP)}
for n, im in made.items():
    im.save(f"{OUT}/{n}.png")
if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (len(made) * 104 + 8, 112), (40, 40, 48, 255))
    for i, im in enumerate(made.values()):
        big = im.resize((96, 96), Image.NEAREST)
        sheet.paste(big, (8 + i * 104, 8), big)
    sheet.save(sys.argv[2])
print("ok")
