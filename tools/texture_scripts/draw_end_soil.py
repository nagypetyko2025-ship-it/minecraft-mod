"""Draws the End Soil texture (original pixel art, CC0)."""
import random, sys
from PIL import Image

def hexc(h):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)

SOIL = [hexc(c) for c in ("#2F2238", "#3E2D4A", "#4F3B5D", "#634B72", "#7A5F8A")]
PEBBLE = hexc("#A893B8")
GLINT = hexc("#C46AD6")

rnd = random.Random(77)
v = [[rnd.random() for _ in range(16)] for _ in range(16)]
img = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        t = (v[y][x] * 2 + v[y][(x + 1) % 16] + v[(y + 1) % 16][x] + v[y][(x - 1) % 16] + v[(y - 1) % 16][x]) / 6
        i = 0 if t < 0.33 else 1 if t < 0.45 else 2 if t < 0.56 else 3 if t < 0.66 else 4
        img.putpixel((x, y), SOIL[i])
for _ in range(6):  # pale pebbles with a dark shadow underneath
    x, y = rnd.randrange(16), rnd.randrange(16)
    img.putpixel((x, y), PEBBLE)
    img.putpixel((x, (y + 1) % 16), SOIL[0])
for _ in range(3):  # tiny chorus-pink glints
    img.putpixel((rnd.randrange(16), rnd.randrange(16)), GLINT)
img.save(sys.argv[1])
