"""Draws the Glimmerhoof entity skin + glow layer and its items (original pixel art, CC0).
usage: python draw_glimmerhoof.py <assets/endrealms/textures dir> [preview.png]"""
import os, random, sys
from PIL import Image

OUT = sys.argv[1]
def hexc(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

CREAM = [hexc(c) for c in ("#B9BB80", "#D3D59A", "#E4E6AE", "#F1F2C6")]
PURP = [hexc(c) for c in ("#3A2150", "#55307A", "#7446A0", "#9364C0")]
PINK = [hexc(c) for c in ("#C77FE0", "#F2A8EE", "#FFD6F8", "#FFF4FD")]
rnd = random.Random(5)
skin = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

def faces(u, v, w, h, d):
    """Standard Minecraft box UV layout -> dict of face rectangles (x, y, width, height)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}

def fill(rect, pal, lo=1, hi=None, spots=None, spot_chance=0.0):
    x0, y0, w, h = rect
    hi = len(pal) - 1 if hi is None else hi
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            c = pal[rnd.randint(lo, hi)]
            if spots and rnd.random() < spot_chance:
                c = spots[rnd.randint(1, 2)]
            skin.putpixel((x, y), c)

def box(u, v, w, h, d, pal, lo=1, hi=None, **kw):
    hi = len(pal) - 1 if hi is None else hi
    f = faces(u, v, w, h, d)
    for name, rect in f.items():
        if name == "top":
            fill(rect, pal, lo=min(hi, lo + 1), hi=hi, **kw)   # top lit
        elif name == "bottom":
            fill(rect, pal, lo=max(0, lo - 1), hi=lo)          # underside shaded
        else:
            fill(rect, pal, lo=lo, hi=hi, **kw)
    return f

# body: pale end-stone hide with violet spots
body = box(0, 0, 8, 6, 12, CREAM, spots=PURP, spot_chance=0.12)
# head
head = box(0, 20, 6, 6, 5, CREAM, spots=PURP, spot_chance=0.06)
fx, fy, fw, fh = head["front"]
for x in range(fx, fx + fw):             # dark violet mask across the face
    for y in (fy + 1, fy + 2, fy + 3):
        skin.putpixel((x, y), PURP[0] if y == fy + 2 else PURP[1])
for ex in (fx + 1, fx + 4):              # eyes (also in glow layer)
    skin.putpixel((ex, fy + 2), PINK[2]); glow.putpixel((ex, fy + 2), PINK[3])
    glow.putpixel((ex, fy + 1), PINK[1] [:3] + (140,))
# legs: dark violet with glowing hooves
legs = box(40, 0, 2, 12, 2, PURP, lo=1, hi=2)
for name in ("right", "front", "left", "back"):
    x0, y0, w, h = legs[name]
    for x in range(x0, x0 + w):
        for y in (y0 + h - 2, y0 + h - 1):
            skin.putpixel((x, y), PINK[1]); glow.putpixel((x, y), PINK[1])
bx, by, bw, bh = legs["bottom"]
for x in range(bx, bx + bw):
    for y in range(by, by + bh):
        skin.putpixel((x, y), PINK[1]); glow.putpixel((x, y), PINK[1])
# antenna stalks, orbs, spines, tail
box(48, 0, 1, 6, 1, PURP, lo=2, hi=3)
for rect in box(52, 0, 2, 2, 2, PINK, lo=1, hi=3).values():
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            glow.putpixel((x, y), skin.getpixel((x, y)))
for rect in box(48, 8, 1, 3, 1, PINK, lo=0, hi=2).values():
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            glow.putpixel((x, y), skin.getpixel((x, y))[:3] + (200,))
tail = box(24, 20, 2, 2, 6, PURP, lo=1, hi=3)

os.makedirs(f"{OUT}/entity/glimmerhoof", exist_ok=True)
skin.save(f"{OUT}/entity/glimmerhoof/glimmerhoof.png")
glow.save(f"{OUT}/entity/glimmerhoof/glimmerhoof_glow.png")

# --- items ---
def draw(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img

MEAT = [
    "................",
    "................",
    "................",
    "......ooooooo...",
    "....ooMMMMMMMo..",
    "...oMMmmmmMMMMo.",
    "..oMmmFFFmmMMMo.",
    "..oMmFFFFFmMMMo.",
    "WBBoMmFFFmmMMMo.",
    "WBBoMMmmmmMMMMo.",
    "...oMMMMMMMMMo..",
    "...ooMMMMMMMoo..",
    ".....ooooooo....",
    "................",
    "................",
    "................",
]
raw = draw(MEAT, {"o": hexc("#5A1F6E"), "M": hexc("#B24FC8"), "m": hexc("#D27AE0"),
                  "F": hexc("#F7C8F2"), "B": hexc("#E9EBB4"), "W": hexc("#FFFFF0")})
cooked = draw(MEAT, {"o": hexc("#3A1630"), "M": hexc("#7E3A6E"), "m": hexc("#A0507E"),
                     "F": hexc("#D49AB8"), "B": hexc("#C9B98A"), "W": hexc("#E8DDB8")})
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
egg = draw(EGG, {"o": hexc("#3A2150"), "C": hexc("#E4E6AE"), "d": hexc("#B9BB80"), "P": hexc("#C77FE0")})
raw.save(f"{OUT}/item/raw_glimmerhoof.png")
cooked.save(f"{OUT}/item/cooked_glimmerhoof.png")
egg.save(f"{OUT}/item/glimmerhoof_spawn_egg.png")

if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (64 * 6 + 3 * 104 + 40, 200), (40, 40, 48, 255))
    sheet.paste(skin.resize((384, 192), Image.NEAREST), (4, 4), skin.resize((384, 192), Image.NEAREST))
    for i, im in enumerate((raw, cooked, egg)):
        big = im.resize((96, 96), Image.NEAREST)
        sheet.paste(big, (400 + i * 104, 50), big)
    sheet.save(sys.argv[2])
print("ok")
