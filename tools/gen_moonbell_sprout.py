"""Draws the two young Moonbell stages (CC0, made for End Realms): a sprout, then a stem with a closed bell bud.
Colours are taken from the grown plant's own textures so the stages match it."""
from collections import Counter
from PIL import Image

TEX = r"D:\minecraft mod\endrealms\src\main\resources\assets\endrealms\textures\block"


def palette(name):
    img = Image.open(f"{TEX}\\{name}.png").convert("RGBA")
    colours = Counter(p for p in img.getdata() if p[3] > 0)
    return [c for c, _ in colours.most_common()]


stem = palette("moonbell_bottom")
bell = palette("moonbell_top")
STEM, STEM_DARK = stem[0], stem[min(1, len(stem) - 1)]
petals = [c for c in bell if c[1] > c[0] + 40 and sum(c[:3]) < 690] or bell     # the glowing cyan of the bells
BELL = max(petals, key=lambda c: sum(c[:3]))
BELL_DARK = min(petals, key=lambda c: sum(c[:3]))


def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, pts, colour):
    for x, y in pts:
        img.putpixel((x, y), colour)


def sprout():
    img, glow = blank(), blank()
    put(img, [(8, y) for y in range(10, 16)], STEM)
    put(img, [(7, 13), (6, 12), (5, 12)], STEM_DARK)          # left leaf
    put(img, [(9, 12), (10, 11), (11, 11)], STEM_DARK)         # right leaf
    put(img, [(8, 9)], BELL)
    put(glow, [(8, 9)], BELL)
    return img, glow


def budding():
    img, glow = blank(), blank()
    put(img, [(8, y) for y in range(6, 16)], STEM)
    put(img, [(7, 14), (6, 13), (5, 13), (4, 12)], STEM_DARK)
    put(img, [(9, 12), (10, 11), (11, 11), (12, 10)], STEM_DARK)
    put(img, [(9, 7), (10, 7)], STEM)                           # bud stalk bending over
    bud = [(10, 8), (11, 8), (10, 9), (11, 9), (10, 10), (11, 10)]
    put(img, bud, BELL)
    put(img, [(10, 10), (11, 10)], BELL_DARK)
    put(glow, bud[:4], BELL)
    return img, glow


for stage, (img, glow) in enumerate((sprout(), budding())):
    img.save(f"{TEX}\\moonbell_sprout_{stage}.png")
    glow.save(f"{TEX}\\moonbell_sprout_{stage}_glow.png")
print("ok", STEM, BELL)
