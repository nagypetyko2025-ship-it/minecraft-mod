"""Draws Voidstone, Polished Voidstone and Voidstone Bricks (original pixel art, CC0)."""
import random, sys
from PIL import Image

OUT = sys.argv[1]
def hexc(h):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)

V = [hexc(c) for c in ("#3E2F55", "#584477", "#6B5690", "#7E68A3", "#9682B9", "#B3A0D0")]

def smooth_noise(rnd):
    v = [[rnd.random() for _ in range(16)] for _ in range(16)]
    return [[(v[y][x] * 2 + v[y][(x + 1) % 16] + v[(y + 1) % 16][x] + v[y][(x - 1) % 16] + v[(y - 1) % 16][x]) / 6
             for x in range(16)] for y in range(16)]

def voidstone(rnd):
    n = smooth_noise(rnd)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = n[y][x]
            img.putpixel((x, y), V[1 if t < 0.36 else 2 if t < 0.47 else 3 if t < 0.58 else 4])
    for _ in range(8):  # porous pits with a lit rim, like end stone but purple
        x, y = rnd.randrange(16), rnd.randrange(16)
        img.putpixel((x, y), V[0])
        img.putpixel(((x - 1) % 16, (y - 1) % 16), V[5])
    return img

def polished(rnd):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x == 0 or y == 0:
                c = V[5]                       # bevel highlight
            elif x == 15 or y == 15:
                c = V[1]                       # bevel shadow
            elif x == 1 or y == 1:
                c = V[4]
            elif x == 14 or y == 14:
                c = V[2]
            else:
                c = V[3] if rnd.random() > 0.12 else V[4]
            img.putpixel((x, y), c)
    return img

def bricks(rnd):
    img = Image.new("RGBA", (16, 16))
    for row in range(4):                       # 4 rows of 8x4 bricks, every other row offset by 4
        off = 0 if row % 2 == 0 else 4
        for y in range(row * 4, row * 4 + 4):
            for x in range(16):
                lx = (x + off) % 8
                if y == row * 4 + 3 or lx == 7:
                    c = V[0]                   # mortar
                elif y == row * 4 or lx == 0:
                    c = V[4]                   # lit top/left edge of each brick
                else:
                    c = V[3] if rnd.random() > 0.2 else V[2]
                img.putpixel((x, y), c)
    return img

rnd = random.Random(31)
made = {"voidstone": voidstone(rnd), "polished_voidstone": polished(rnd), "voidstone_bricks": bricks(rnd)}
for n, im in made.items():
    im.save(f"{OUT}/{n}.png")
if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (3 * 168 + 8, 2 * 168 + 8), (40, 40, 48, 255))
    for i, im in enumerate(made.values()):
        sheet.paste(im.resize((160, 160), Image.NEAREST), (8 + i * 168, 8))
        tile = Image.new("RGBA", (32, 32))
        for a in (0, 16):
            for b in (0, 16):
                tile.paste(im, (a, b))
        sheet.paste(tile.resize((160, 160), Image.NEAREST), (8 + i * 168, 176))
    sheet.save(sys.argv[2])
print("ok")
