"""Draws the End Realms chorusite textures (original pixel art, CC0)."""
import random, sys, os
from PIL import Image

OUT = sys.argv[1]  # path to src/main/resources/assets/endrealms/textures

def hexc(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2],16), int(h[2:4],16), int(h[4:6],16), a)

# --- end-stone-like base (own design: porous pale yellow stone) ---
BASE = [hexc(c) for c in ("#F3F4C4", "#E4E6AB", "#D6D899", "#C6C788", "#B2B377")]
def end_stone_base(seed):
    rnd = random.Random(seed)
    v = [[rnd.random() for _ in range(16)] for _ in range(16)]
    # light smoothing (wrapping, so the texture tiles)
    s = [[(v[y][x]*2 + v[y][(x+1)%16] + v[(y+1)%16][x] + v[y][(x-1)%16] + v[(y-1)%16][x]) / 6
          for x in range(16)] for y in range(16)]
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = s[y][x]
            i = 0 if t > 0.66 else 1 if t > 0.52 else 2 if t > 0.40 else 3
            img.putpixel((x, y), BASE[i])
    # small pores: a dark pixel with a lit rim above-left
    for _ in range(7):
        x, y = rnd.randrange(16), rnd.randrange(16)
        img.putpixel((x, y), BASE[4])
        img.putpixel(((x-1) % 16, (y-1) % 16), BASE[0])
    return img

PAL = {
    'D': hexc("#4B1A5E"),   # deep outline
    'M': hexc("#8E3AAE"),   # purple
    'P': hexc("#D861CF"),   # pink-magenta
    'L': hexc("#FFB8F0"),   # light pink
    'W': hexc("#FFF0FC"),   # sparkle
}

ORE = [
    "................",
    "..DD............",
    ".DWPD......DD...",
    ".DPMD.....DLPD..",
    "..DMD.....DPPMD.",
    "...D.......DMD..",
    "............D...",
    "......DD........",
    ".....DLPD.......",
    "....DWLPMD......",
    "....DPPMMD...DD.",
    ".....DMMD...DLPD",
    "..DD..DD....DPMD",
    ".DLPD........DD.",
    ".DPMD...........",
    "..DD............",
]

SHARD = [
    "................",
    "........DD......",
    ".......DWLD.....",
    "......DLLPPD....",
    "......DLPPMD....",
    ".....DLPPPMMD...",
    ".....DLPPPMMD...",
    "....DLPPPMMMD...",
    "....DLPPPMMD..D.",
    "...DLPPPMMMD.DLD",
    "...DLPPMMMD.DLPD",
    "...DPPPMMMD.DPMD",
    "....DPMMMD..DMD.",
    "....DMMMD....D..",
    ".....DDD........",
    "................",
]

def paint(img, rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), "rows must be 16x16"
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in PAL:
                img.putpixel((x, y), PAL[ch])
    return img

os.makedirs(f"{OUT}/block", exist_ok=True)
os.makedirs(f"{OUT}/item", exist_ok=True)
ore = paint(end_stone_base(1337), ORE)
ore.save(f"{OUT}/block/chorusite_ore.png")
# voidstone variant: same crystals on the voidstone texture (run draw_voidstone.py first), darker outline for contrast
void_base = Image.open(f"{OUT}/block/voidstone.png").convert("RGBA")
PAL_DARK = dict(PAL, D=hexc("#24082F"))
def paint_with(img, rows, pal):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img
paint_with(void_base, ORE, PAL_DARK).save(f"{OUT}/block/voidstone_chorusite_ore.png")
shard = paint(Image.new("RGBA", (16, 16), (0, 0, 0, 0)), SHARD)
shard.save(f"{OUT}/item/chorusite_shard.png")

if len(sys.argv) > 2:  # preview sheet, scaled up
    prev = Image.new("RGBA", (16*16*3 + 32, 16*16 + 16), (40, 40, 48, 255))
    tiled = Image.new("RGBA", (32, 32))
    for i in range(2):
        for j in range(2):
            tiled.paste(ore, (i*16, j*16))
    prev.paste(ore.resize((256, 256), Image.NEAREST), (8, 8))
    prev.paste(tiled.resize((256, 256), Image.NEAREST), (272, 8))
    prev.paste(shard.resize((256, 256), Image.NEAREST), (536, 8), shard.resize((256, 256), Image.NEAREST))
    prev.save(sys.argv[2])
