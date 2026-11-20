"""Draws the End villager skins (one robe per profession), the shared glow layer and the spawn egg (original pixel art, CC0).
usage: python draw_end_villager.py <assets/endrealms/textures dir> [preview.png]"""
import os
import random
import sys

from PIL import Image

OUT = sys.argv[1]


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


SKIN = [hexc(c) for c in ("#B9ACC9", "#AB9DBE", "#C4B8D2")]
SKIN_DARK = hexc("#8F80A6")
HORN = [hexc("#E7E4B8"), hexc("#CFCB98")]
EYE = hexc("#7DF2E4")
EYE_DARK = hexc("#20142E")
TRIM_GLOW = hexc("#F2A8EE")

# profession -> (robe shades, accent, hood?)
ROBES = {
    "toolsmith": (("#3A3F52", "#2F3445", "#454B60"), "#E08A3C", False),
    "mason": (("#8E8A78", "#7E7A69", "#9C9886"), "#6A4FA8", False),
    "butcher": (("#D8D2C8", "#C8C1B5", "#E2DDD4"), "#A8324E", False),
    "fisherman": (("#2F6F86", "#265D71", "#3A8199"), "#E7E4B8", False),
    "cleric": (("#6A2C8E", "#5A2379", "#7A38A0"), "#F2D16B", True),
    "librarian": (("#2E2A6B", "#25215A", "#39347D"), "#F2F0DE", True),
    "forester": (("#7C3F96", "#6B3383", "#8C4CA8"), "#46C9C6", False),
    "jeweler": (("#1A8290", "#13606F", "#27A6AC"), "#D6FFF7", False),
}


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def draw(name, robe_hex, accent_hex, hood):
    rnd = random.Random(name)
    robe = [hexc(c) for c in robe_hex]
    accent = hexc(accent_hex)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def box(u, v, w, h, d, pal):
        f = faces(u, v, w, h, d)
        for x0, y0, fw, fh in f.values():
            for y in range(y0, y0 + fh):
                for x in range(x0, x0 + fw):
                    img.putpixel((x, y), pal[rnd.randrange(len(pal))])
        return f

    head = box(0, 0, 8, 10, 8, SKIN)
    fx, fy, _, _ = head["front"]
    for ex in (fx + 1, fx + 5):
        for dx in (0, 1):
            img.putpixel((ex + dx, fy + 4), EYE)
            img.putpixel((ex + dx, fy + 5), EYE_DARK)
    for x in range(fx + 1, fx + 7):
        img.putpixel((x, fy + 3), SKIN_DARK)        # brow
    box(24, 0, 2, 4, 2, [SKIN_DARK, SKIN[1]])       # nose

    box(16, 20, 8, 12, 6, robe)                     # torso
    jacket = box(0, 38, 8, 20, 6, robe)
    for side in ("front", "back", "right", "left"):
        x0, y0, fw, fh = jacket[side]
        for x in range(x0, x0 + fw):
            img.putpixel((x, y0 + fh - 1), accent)
            img.putpixel((x, y0 + fh - 2), robe[1])
    x0, y0, fw, fh = jacket["front"]
    for y in range(y0, y0 + fh):
        img.putpixel((x0 + 3, y), accent)
        img.putpixel((x0 + 4, y), accent)

    arm = box(44, 22, 4, 8, 4, robe)
    for side in ("front", "back", "right", "left"):
        x0, y0, fw, fh = arm[side]
        for x in range(x0, x0 + fw):
            img.putpixel((x, y0 + fh - 1), accent)
    box(40, 38, 8, 4, 4, SKIN)                      # folded forearms / hands
    box(0, 22, 4, 12, 4, [robe[1], robe[2]])        # legs

    horn = box(28, 38, 1, 4, 1, HORN)
    for side in ("front", "back", "right", "left"):
        x0, y0, _, _ = horn[side]
        img.putpixel((x0, y0), TRIM_GLOW)
    img.putpixel(horn["top"][:2], TRIM_GLOW)

    if hood:
        hat = box(32, 0, 8, 10, 8, robe)
        x0, y0, fw, fh = hat["front"]
        for y in range(y0 + 2, y0 + fh):            # open face in the hood
            for x in range(x0 + 1, x0 + fw - 1):
                img.putpixel((x, y), (0, 0, 0, 0))
        for x in range(x0, x0 + fw):
            img.putpixel((x, y0 + 1), accent)

    os.makedirs(f"{OUT}/entity/end_villager", exist_ok=True)
    img.save(f"{OUT}/entity/end_villager/{name}.png")
    return img


sheets = [draw(n, r, a, h) for n, (r, a, h) in ROBES.items()]

glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
fx, fy, _, _ = faces(0, 0, 8, 10, 8)["front"]
for ex in (fx + 1, fx + 5):
    for dx in (0, 1):
        glow.putpixel((ex + dx, fy + 4), EYE)
hf = faces(28, 38, 1, 4, 1)
for side in ("front", "back", "right", "left"):
    glow.putpixel(hf[side][:2], TRIM_GLOW)
glow.putpixel(hf["top"][:2], TRIM_GLOW)
glow.save(f"{OUT}/entity/end_villager/glow.png")

EGG = [
    "................",
    "......oooo......",
    ".....oCCCCo.....",
    "....oCCPCCCo....",
    "....oCPPCCCo....",
    "...oCCCCCPPCo...",
    "...oCCCCCPPCo...",
    "...oCTCCCCCCo...",
    "..oCTTCCCPCCCo..",
    "..oCCCCCCPPCCo..",
    "..oCCCPCCCCCCo..",
    "..odCCPPCCCCdo..",
    "...odCCCCCCdo...",
    "....oddddddo....",
    ".....oooooo.....",
    "................",
]
EP = {"o": hexc("#20142E"), "C": SKIN[0], "d": SKIN_DARK, "P": hexc("#6A2C8E"), "T": EYE}
egg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, r in enumerate(EGG):
    for x, ch in enumerate(r):
        if ch in EP:
            egg.putpixel((x, y), EP[ch])
egg.save(f"{OUT}/item/end_villager_spawn_egg.png")

if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (4 * 208 + 8, 2 * 208 + 8), (45, 42, 66, 255))
    for i, s in enumerate(sheets):
        big = s.resize((200, 200), Image.NEAREST)
        sheet.paste(big, (8 + (i % 4) * 208, 8 + (i // 4) * 208), big)
    sheet.save(sys.argv[2])
print("ok")
