"""Draws the young Starreed (CC0, made for End Realms): a short cane shoot with one leaf, in the grown cane's own colours."""
from collections import Counter
from PIL import Image

TEX = r"D:\minecraft mod\endrealms\src\main\resources\assets\endrealms\textures\block"


def palette(name):
    img = Image.open(f"{TEX}\\{name}.png").convert("RGBA")
    return [c for c, _ in Counter(p for p in img.getdata() if p[3] > 0).most_common()]


cane = palette("starreed_stalk")
glow = palette("starreed_top_glow") or cane
CANE = cane[0]
DARK = min(cane[:4], key=lambda c: sum(c[:3]))
LIGHT = max(cane[:4], key=lambda c: sum(c[:3]))
STAR = max(glow, key=lambda c: sum(c[:3]))

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
lit = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y in range(9, 16):                      # the shoot, two pixels wide like the grown cane
    img.putpixel((7, y), DARK)
    img.putpixel((8, y), LIGHT if y % 3 else CANE)
for x, y in [(9, 12), (10, 11), (11, 11), (11, 10)]:   # one leaf
    img.putpixel((x, y), CANE)
for x, y in [(7, 8), (8, 8), (8, 7)]:       # a glowing star bud at the tip
    img.putpixel((x, y), STAR)
    lit.putpixel((x, y), STAR)
img.save(f"{TEX}\\starreed_sprout.png")
lit.save(f"{TEX}\\starreed_sprout_glow.png")
print("ok", CANE, STAR)
