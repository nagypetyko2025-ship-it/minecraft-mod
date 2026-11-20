"""Draws the Astral Basin textures (original pixel art, CC0): three fish skins + glow layers, their raw/cooked
items and spawn eggs, and the two underwater plants.
usage: python draw_basin.py <assets/endrealms/textures dir> [preview.png]"""
import os
import random
import sys

from PIL import Image

OUT = sys.argv[1]
rnd = random.Random(42)


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def faces(u, v, w, h, d):
    """Minecraft box UV layout -> face rectangles (x, y, width, height). 'front' is the -Z (head) face."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


class Skin:
    def __init__(self):
        self.img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        self.glow = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    def fill(self, rect, pal, glow=False):
        x0, y0, w, h = rect
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                c = pal[rnd.randrange(len(pal))]
                self.img.putpixel((x, y), c)
                if glow:
                    self.glow.putpixel((x, y), c)

    def box(self, u, v, w, h, d, pal, belly=None, glow=False):
        f = faces(u, v, w, h, d)
        for name, (x, y, fw, fh) in f.items():
            if fw <= 0 or fh <= 0:
                continue
            self.fill((x, y, fw, fh), belly if (belly and name == "bottom") else pal, glow)
        # zero-thickness planes (fins): make both sides mirror images so they look the same from either side
        if w == 0:
            self.mirror(f["right"], f["left"])
        if h == 0:
            self.mirror(f["top"], f["bottom"])
        return f

    def mirror(self, src, dst):
        sx, sy, sw, sh = src
        dx, dy, _, _ = dst
        for y in range(sh):
            for x in range(sw):
                self.img.putpixel((dx + sw - 1 - x, dy + y), self.img.getpixel((sx + x, sy + y)))
                self.glow.putpixel((dx + sw - 1 - x, dy + y), self.glow.getpixel((sx + x, sy + y)))

    def px(self, x, y, c, glow=False):
        self.img.putpixel((x, y), c)
        if glow:
            self.glow.putpixel((x, y), c)

    def save(self, name):
        os.makedirs(f"{OUT}/entity/fish", exist_ok=True)
        self.img.save(f"{OUT}/entity/fish/{name}.png")
        self.glow.save(f"{OUT}/entity/fish/{name}_glow.png")


PINK = [hexc("#F2A8EE"), hexc("#FFD6F8")]
CREAM = [hexc("#E4E6AE"), hexc("#D3D59A")]
EYE = hexc("#FFF6C8")

# --- Starfin: violet body, cream belly, glowing pink star fins
s = Skin()
VIOLET = [hexc(c) for c in ("#3B2157", "#56307D", "#6F44A0", "#8A5DBF")]
body = s.box(0, 0, 2, 4, 8, VIOLET, belly=CREAM)
s.box(20, 0, 0, 4, 5, PINK, glow=True)            # tail
s.box(0, 12, 0, 3, 5, PINK, glow=True)            # star dorsal fin
s.box(10, 12, 3, 0, 3, PINK, glow=True)           # side fins
s.box(10, 15, 3, 0, 3, PINK, glow=True)
fx, fy, fw, fh = body["front"]
s.px(fx, fy + 1, EYE, True)
s.px(fx + 1, fy + 1, EYE, True)
s.save("starfin")

# --- Void Eel: near-black violet body, glowing dorsal line and eyes, violet tail fin
s = Skin()
DARK = [hexc(c) for c in ("#140C1C", "#21152E", "#2F1F42", "#40295A")]
head = s.box(0, 0, 2, 3, 3, DARK)
s.box(0, 6, 2, 2, 5, DARK)
s.box(14, 10, 0, 1, 5, PINK, glow=True)           # glowing dorsal ridge
s.box(0, 13, 2, 2, 5, DARK)
s.box(14, 0, 0, 5, 5, [hexc("#6F44A0"), hexc("#8A5DBF")])
for name in ("top",):                             # dotted glow line on the back segments
    for (u, v) in ((0, 6), (0, 13)):
        x0, y0, w, h = faces(u, v, 2, 2, 5)[name]
        for y in range(y0, y0 + h, 2):
            s.px(x0, y, PINK[1], True)
fx, fy, fw, fh = head["front"]
s.px(fx, fy, EYE, True)
s.px(fx + 1, fy, EYE, True)
s.save("void_eel")

# --- Glowgill: pale lilac round body with dark spots, glowing gills, angler lure
s = Skin()
LILAC = [hexc(c) for c in ("#9B7BC0", "#B395D6", "#C9AEE6", "#DCC6F2")]
body = s.box(0, 0, 5, 5, 5, LILAC)
for _ in range(10):
    s.px(rnd.randrange(0, 20), rnd.randrange(0, 10), hexc("#5A3A80"))
s.box(20, 0, 0, 3, 3, [hexc("#8A5DBF"), hexc("#B395D6")])
s.box(0, 10, 0, 3, 2, PINK, glow=True)            # gills
s.box(4, 10, 0, 3, 2, PINK, glow=True)
s.box(8, 10, 1, 3, 1, [hexc("#56307D")])          # lure stalk
s.box(12, 10, 2, 2, 2, [hexc("#FFF4FD"), hexc("#FFD6F8")], glow=True)  # lure orb
fx, fy, fw, fh = body["front"]
s.px(fx + 1, fy + 1, EYE, True)
s.px(fx + 3, fy + 1, EYE, True)
for x in range(fx + 1, fx + 4):
    s.px(x, fy + 3, hexc("#3B2157"))              # mouth
s.save("glowgill")


# --- items ---
def draw(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


def cooked(pal):
    """Cooked variant: darker, warmer, and the glowing bits turn golden-brown."""
    out = {}
    for k, (r, g, b, a) in pal.items():
        out[k] = (min(255, int(r * 0.62 + 40)), int(g * 0.5 + 20), int(b * 0.42 + 10), a)
    return out


FISH = [
    "................",
    "................",
    "........ff......",
    ".......fBBo.....",
    ".....oBBBBBo..tt",
    "....oBBbBBBBo.tt",
    "...oeBBbbBBBBot.",
    "...oBBBbbBBBBBt.",
    "...oLLBBBBBBBot.",
    "....oLLLLLBBo.tt",
    ".....ooLLLoo..tt",
    ".......ff.......",
    "................",
    "................",
    "................",
    "................",
]
EEL = [
    "................",
    "................",
    "................",
    "...........gg...",
    ".........ooBBo..",
    ".......ooBBBBeo.",
    "..t...oBBBooooo.",
    ".tto.oBBBo......",
    ".tBBoBBBo.......",
    "..oBBBBo........",
    "...oooo.........",
    "................",
    "................",
    "................",
    "................",
    "................",
]
ROUND = [
    "................",
    "......g.........",
    ".......s........",
    "........s.......",
    ".....oooooo.....",
    "....oBBBBBBo.tt.",
    "...oeBBdBBBBott.",
    "...oBBBBBdBBBt..",
    "...omBBdBBBBBt..",
    "...oBBBBBBdBott.",
    "....oBBBBBBo.tt.",
    ".....oooooo.....",
    "................",
    "................",
    "................",
    "................",
]
starfin_pal = {"o": hexc("#2A1640"), "B": hexc("#6F44A0"), "b": hexc("#8A5DBF"), "L": hexc("#E4E6AE"),
               "f": hexc("#F2A8EE"), "t": hexc("#F2A8EE"), "e": EYE}
eel_pal = {"o": hexc("#0C0712"), "B": hexc("#2F1F42"), "g": hexc("#F2A8EE"), "t": hexc("#8A5DBF"), "e": EYE}
glow_pal = {"o": hexc("#5A3A80"), "B": hexc("#C9AEE6"), "d": hexc("#9B7BC0"), "g": hexc("#FFF4FD"),
            "s": hexc("#56307D"), "t": hexc("#F2A8EE"), "e": EYE, "m": hexc("#3B2157")}
items = {}
for name, rows, pal in (("starfin", FISH, starfin_pal), ("void_eel", EEL, eel_pal), ("glowgill", ROUND, glow_pal)):
    items[f"raw_{name}"] = draw(rows, pal)
    items[f"cooked_{name}"] = draw(rows, cooked(pal))

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
for name, base, spot in (("starfin", "#6F44A0", "#F2A8EE"), ("void_eel", "#2F1F42", "#F2A8EE"),
                         ("glowgill", "#C9AEE6", "#FFF4FD")):
    b = hexc(base)
    items[f"{name}_spawn_egg"] = draw(EGG, {"o": hexc("#140C1C"), "C": b, "P": hexc(spot),
                                            "d": (int(b[0] * 0.75), int(b[1] * 0.75), int(b[2] * 0.75), 255)})
os.makedirs(f"{OUT}/item", exist_ok=True)
for name, img in items.items():
    img.save(f"{OUT}/item/{name}.png")

# --- plants (cross models, transparent background) ---
STARWEED = [
    "....*.......*...",
    "....l.......l...",
    "...l....*..l....",
    "...l....l..l....",
    "..L.....l.L..*..",
    "..L....L..L..l..",
    "..L....L.L..l...",
    "...L...L.L..l...",
    "...L..L..L.L....",
    "....L.L.L..L....",
    "....L.L.L.L.....",
    ".....LL.LL......",
    ".....dL.Ld......",
    "......dLd.......",
    "......dLd.......",
    "......ddd.......",
]
starweed = draw(STARWEED, {"*": hexc("#FFF6C8"), "l": hexc("#E9D6FA"), "L": hexc("#B99AE0"), "d": hexc("#7A57A8")})
CORAL = [
    "..w.......w.....",
    "..p...w...p..w..",
    "..p...p..p...p..",
    "...p..p..p..p...",
    "...P..p.p...P.w.",
    "w..P...pP..P..p.",
    "p...P..PP..P.p..",
    ".p..P..P..P..p..",
    "..p.PP.P.P..p...",
    "...PP.PPPP.PP...",
    "....PP.PP.PP....",
    ".....PPPPPP.....",
    "......PPPP......",
    "......DPPD......",
    ".......DD.......",
    ".......DD.......",
]
coral = draw(CORAL, {"w": hexc("#FFF4FD"), "p": hexc("#F7B4EF"), "P": hexc("#D86FD0"), "D": hexc("#8A3A90")})
coral.save(f"{OUT}/block/void_coral.png")

if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (3 * 200 + 16, 200 + 2 * 72 + 16), (40, 40, 48, 255))
    for i, n in enumerate(("starfin", "void_eel", "glowgill")):
        sk = Image.open(f"{OUT}/entity/fish/{n}.png").resize((192, 192), Image.NEAREST)
        sheet.paste(sk, (8 + i * 200, 8), sk)
    allicons = list(items.values()) + [starweed, coral]
    for i, im in enumerate(allicons):
        big = im.resize((64, 64), Image.NEAREST)
        sheet.paste(big, (8 + (i % 9) * 68, 208 + (i // 9) * 72), big)
    sheet.save(sys.argv[2])
print("ok")
