"""Draws the Voidbloom plant (original pixel art, CC0): pale end-stone stalk, lilac tendrils, glowing pink orbs."""
import sys
from PIL import Image

def hexc(h):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)

PAL = {'s': hexc("#ECEEBA"), 'S': hexc("#C4C68A"),            # stalk: end stone colours
       'g': hexc("#D9AAF0"), 'G': hexc("#B987DA"),            # tendrils: light lilac
       'r': hexc("#C984E2"), 'p': hexc("#F4ACEF"),            # orb rim / body
       'l': hexc("#FFD9FA"), 'w': hexc("#FFF6FE"),            # orb highlight / glow
       'v': hexc("#8A4DBA")}                                  # odd violet "pupil"
ROWS = [
    "......rr........",
    ".....rplr...rr..",
    ".....rlwr..rplr.",
    ".rr..rvpr..rlwr.",
    "rplr..rr....rr..",
    "rlwr...s.....G..",
    ".rr...sS....g...",
    "...G..sS...g....",
    "....g.sS..g.....",
    ".....gsSgG......",
    "......sS....rr..",
    "..Gg..sS...rplr.",
    "....g.sS..grlwr.",
    ".....gsSgG..rr..",
    "......sS........",
    ".....SsSS.......",
]
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(ROWS):
    assert len(row) == 16, (y, len(row))
    for x, ch in enumerate(row):
        if ch in PAL:
            img.putpixel((x, y), PAL[ch])
img.save(sys.argv[1])
if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (360, 184), (40, 40, 48, 255))
    big = img.resize((160, 160), Image.NEAREST)
    sheet.paste(big, (12, 12), big)
    dark = Image.new("RGBA", (160, 160), (30, 22, 40, 255))
    sheet.paste(dark, (188, 12)); sheet.paste(big, (188, 12), big)
    sheet.save(sys.argv[2])
