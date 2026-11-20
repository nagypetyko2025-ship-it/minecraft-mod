"""Draws every Prism Wastes texture: turquoise crystal blocks with violet End shadows, crystal buds, the pale
crystalline voidstone, the lamp, the shard, and the Shardcrab skin + glow + spawn egg (original pixel art, CC0).
usage: python draw_prism.py <assets/endrealms/textures dir> [preview.png]"""
import os
import random
import sys

from PIL import Image

OUT = sys.argv[1]
rnd = random.Random(4242)


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


DEEP = hexc("#0D3F52")
SHADES = [hexc(c) for c in ("#13606F", "#1A8290", "#27A6AC", "#46C9C6")]
LIGHT = hexc("#8BEFE2")
WHITE = hexc("#D6FFF7")
VIOLET = [hexc("#4A3580"), hexc("#6A4FA8"), hexc("#8F74CC")]
OUTLINE = hexc("#0A2233")
PALE = [hexc(c) for c in ("#BCCFC6", "#C8D9CE", "#D3E2D6", "#AFC6C0")]


def new(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def save(img, rel):
    p = f"{OUT}/{rel}.png"
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    return img


def facets(n, violet_every=5, size=16, seed=0):
    """Tileable Voronoi facets: each one a flat crystal face, edges catch the light, some sink into End violet."""
    r = random.Random(seed)
    pts = []
    for i in range(n):
        pal = VIOLET[r.randrange(2)] if (i + 1) % violet_every == 0 else SHADES[r.randrange(len(SHADES))]
        pts.append((r.randrange(size), r.randrange(size), pal))
    img = new(size)

    def nearest(x, y):
        best = sorted(((min(abs(x - px), size - abs(x - px)) ** 2 + min(abs(y - py), size - abs(y - py)) ** 2), i)
                      for i, (px, py, _) in enumerate(pts))
        return best[0][1], best[1][0] - best[0][0]

    for y in range(size):
        for x in range(size):
            i, gap = nearest(x, y)
            c = pts[i][2]
            if gap <= 2:
                c = LIGHT if r.random() < 0.55 else SHADES[3]
            img.putpixel((x, y), c)
    return img


def spike(img, cx, base, h, w, violet_base=True):
    """Upright crystal: lit left side, dark right side, near-white tip, violet where it meets the rock."""
    for i in range(h):
        y = base - i
        if y < 0:
            break
        half = w * (1 - i / h) + 0.35
        for x in range(int(cx - half + 0.5), int(cx + half + 0.5) + 1):
            if not 0 <= x < img.width:
                continue
            f = (x - (cx - half)) / (2 * half + 0.01)
            c = LIGHT if f < 0.3 else SHADES[3] if f < 0.55 else SHADES[2] if f < 0.8 else SHADES[0]
            if i >= h - 2:
                c = WHITE
            elif i < 2 and violet_base:
                c = VIOLET[1] if f < 0.6 else VIOLET[0]
            img.putpixel((x, y), c)


block = save(facets(9, seed=11), "block/prism_crystal_block")

budding = facets(9, violet_every=2, seed=23)
for (cx, cy) in ((4, 4), (11, 6), (6, 11), (12, 12)):
    for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
        budding.putpixel((cx + dx, cy + dy), DEEP)
    budding.putpixel((cx, cy), WHITE)
save(budding, "block/budding_prism_crystal")

small = new()
spike(small, 7.5, 15, 4, 1.4)
spike(small, 4.5, 15, 2, 0.8)
spike(small, 11, 15, 3, 0.8)
save(small, "block/small_prism_bud")
medium = new()
spike(medium, 7.5, 15, 7, 2.0)
spike(medium, 3.5, 15, 4, 1.0)
spike(medium, 12, 15, 5, 1.2)
save(medium, "block/medium_prism_bud")
large = new()
spike(large, 7.5, 15, 11, 2.4)
spike(large, 3, 15, 6, 1.2)
spike(large, 12.5, 15, 8, 1.4)
save(large, "block/large_prism_bud")
cluster = new()
spike(cluster, 7.5, 15, 15, 2.8)
spike(cluster, 2.5, 15, 8, 1.3)
spike(cluster, 13, 15, 11, 1.6)
spike(cluster, 5, 15, 5, 1.0)
spike(cluster, 10.5, 15, 6, 1.0)
save(cluster, "block/prism_cluster")

ground = new()
for y in range(16):
    for x in range(16):
        ground.putpixel((x, y), PALE[rnd.randrange(len(PALE))])
for _ in range(7):                      # embedded turquoise slivers
    x, y = rnd.randrange(15), rnd.randrange(15)
    ground.putpixel((x, y), SHADES[2])
    ground.putpixel((x + 1, y), LIGHT)
    if rnd.random() < 0.6:
        ground.putpixel((x, y + 1), SHADES[1])
for _ in range(9):                      # End-violet flecks
    ground.putpixel((rnd.randrange(16), rnd.randrange(16)), VIOLET[2])
save(ground, "block/crystalline_voidstone")

lamp = facets(6, violet_every=99, seed=5)
for y in range(16):
    for x in range(16):
        edge = min(x, y, 15 - x, 15 - y)
        if edge == 0:
            lamp.putpixel((x, y), VIOLET[0])
        elif edge == 1:
            lamp.putpixel((x, y), VIOLET[1] if (x + y) % 2 else OUTLINE)
        else:
            lamp.putpixel((x, y), WHITE if lamp.getpixel((x, y)) in (LIGHT, SHADES[3]) else LIGHT)
for p in ((1, 1), (14, 1), (1, 14), (14, 14)):
    lamp.putpixel(p, LIGHT)
save(lamp, "block/prism_lamp")

SHARD = [
    "................",
    "...........oo...",
    "..........oWLo..",
    ".........oWLSo..",
    "........oWLSSo..",
    ".......oLLSSo...",
    "......oLLSDo....",
    ".....oLLSSDo....",
    "....oLSSDDo.....",
    "....oSSDVo......",
    "...oSSDVo.......",
    "...oSDVo........",
    "..oDVVo.........",
    "..oVVo..........",
    "...oo...........",
    "................",
]
SP = {"o": OUTLINE, "W": WHITE, "L": LIGHT, "S": SHADES[2], "D": SHADES[0], "V": VIOLET[1]}
shard = new()
for y, r in enumerate(SHARD):
    for x, ch in enumerate(r):
        if ch in SP:
            shard.putpixel((x, y), SP[ch])
save(shard, "item/prism_shard")

# --- Shardcrab: 64x64 skin, see ShardcrabModel for the UV layout ---
skin = new(64)
glow = new(64)
CARAPACE = [DEEP, SHADES[0], hexc("#2C4A68")]
JOINT = [VIOLET[0], hexc("#37285F")]


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def box(u, v, w, h, d, pal):
    f = faces(u, v, w, h, d)
    for x0, y0, fw, fh in f.values():
        for y in range(y0, y0 + fh):
            for x in range(x0, x0 + fw):
                skin.putpixel((x, y), pal[rnd.randrange(len(pal))])
    return f


body = box(0, 0, 12, 4, 10, CARAPACE)
tx, ty, tw, th = body["top"]
for _ in range(14):                     # crystal crust on the shell
    skin.putpixel((rnd.randrange(tx, tx + tw), rnd.randrange(ty, ty + th)), SHADES[rnd.randrange(1, 4)])


def crystal(u, v, w, h, d):
    f = box(u, v, w, h, d, [SHADES[1], SHADES[2], SHADES[3]])
    for name in ("front", "back", "right", "left"):
        x0, y0, fw, fh = f[name]
        for y in range(y0, y0 + fh):
            t = (y - y0) / fh                   # 0 = tip end of the box
            for x in range(x0, x0 + fw):
                c = WHITE if t < 0.2 else LIGHT if t < 0.45 else skin.getpixel((x, y)) if t < 0.85 else VIOLET[1]
                skin.putpixel((x, y), c)
                if t < 0.45:
                    glow.putpixel((x, y), c)
    x0, y0, fw, fh = f["top"]
    for y in range(y0, y0 + fh):
        for x in range(x0, x0 + fw):
            skin.putpixel((x, y), WHITE)
            glow.putpixel((x, y), WHITE)


crystal(0, 16, 3, 10, 3)
crystal(14, 16, 2, 8, 2)
crystal(24, 16, 2, 6, 2)
crystal(34, 16, 3, 7, 3)

for u in (0, 20):
    claw = box(u, 32, 5, 4, 4, CARAPACE)
    x0, y0, fw, fh = claw["front"]
    for x in range(x0, x0 + fw):
        skin.putpixel((x, y0 + 2), OUTLINE)     # the claw's mouth
        skin.putpixel((x, y0), LIGHT)
box(40, 32, 7, 2, 2, JOINT)                     # legs
for u in (0, 6):
    stalk = box(u, 44, 1, 3, 1, JOINT)
    x0, y0, _, _ = stalk["top"]
    skin.putpixel((x0, y0), WHITE)
    glow.putpixel((x0, y0), WHITE)
    for name in ("front", "back", "right", "left"):
        x1, y1, _, _ = stalk[name]
        skin.putpixel((x1, y1), WHITE)
        glow.putpixel((x1, y1), WHITE)

save(skin, "entity/shardcrab/shardcrab")
save(glow, "entity/shardcrab/shardcrab_glow")

EGG = [
    "................",
    "......oooo......",
    ".....oCCCCo.....",
    "....oCCPCCCo....",
    "....oCPPCCCo....",
    "...oCCCCCPPCo...",
    "...oCCCCCPPCo...",
    "...oCVCCCCCCo...",
    "..oCVVCCCPCCCo..",
    "..oCCCCCCPPCCo..",
    "..oCCCPCCCCCCo..",
    "..odCCPPCCCCdo..",
    "...odCCCCCCdo...",
    "....oddddddo....",
    ".....oooooo.....",
    "................",
]
EP = {"o": OUTLINE, "C": SHADES[1], "d": SHADES[0], "P": LIGHT, "V": VIOLET[2]}
egg = new()
for y, r in enumerate(EGG):
    for x, ch in enumerate(r):
        if ch in EP:
            egg.putpixel((x, y), EP[ch])
save(egg, "item/shardcrab_spawn_egg")

if len(sys.argv) > 2:
    tiles = [block, budding, ground, lamp, small, medium, large, cluster, shard, egg]
    sheet = Image.new("RGBA", (5 * 104 + 8 + 330, 2 * 104 + 8 + 130), (45, 42, 66, 255))
    for i, t in enumerate(tiles):
        big = t.resize((96, 96), Image.NEAREST)
        sheet.paste(big, (8 + (i % 5) * 104, 8 + (i // 5) * 104), big)
    # 3x3 of the ground and of the crystal block, to judge how they tile
    for j, t in enumerate((ground, block)):
        for a in range(3):
            for b in range(3):
                sheet.paste(t.resize((40, 40), Image.NEAREST), (8 + j * 130 + a * 40, 226 + b * 40 - 3))
    s = skin.resize((320, 320), Image.NEAREST)
    sheet.paste(s, (530, 8), s)
    sheet.save(sys.argv[2])
print("ok")
