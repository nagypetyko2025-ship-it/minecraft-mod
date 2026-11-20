"""Chorusite armor textures and the pure shard icon. The armor is the vanilla diamond armor recolored by brightness, so
(like the tools) it derives from Mojang's textures: fine for this mod, not CC0.
usage: python recolor_chorusite_armor.py <client jar> <assets/endrealms/textures dir> [preview.png]"""
import io
import os
import sys
import zipfile

from PIL import Image

zf, OUT = zipfile.ZipFile(sys.argv[1]), sys.argv[2]


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


STOPS = [(p, hexc(c)) for p, c in [(0.0, "#2C0A39"), (0.2, "#4B1A5E"), (0.4, "#6E2A8A"), (0.6, "#9437B0"), (0.75, "#B94DC6"), (0.88, "#D861CF"), (1.0, "#FFCDF5")]]


def gradient(t):
    t = max(0.0, min(1.0, t))
    for (p0, c0), (p1, c1) in zip(STOPS, STOPS[1:]):
        if p0 <= t <= p1:
            k = (t - p0) / (p1 - p0)
            return tuple(round(a + (b - a) * k) for a, b in zip(c0, c1))


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def recolor(path):
    img = Image.open(io.BytesIO(zf.read(path))).convert("RGBA")
    values = [lum(p) for p in img.getdata() if p[3]]
    lo, hi = min(values), max(values)
    res = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p[3]:
                res.putpixel((x, y), gradient((lum(p) - lo) / (hi - lo)) + (p[3],))
    return res


def save(img, rel):
    p = f"{OUT}/{rel}.png"
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    return img


made = []
for piece in ("helmet", "chestplate", "leggings", "boots"):
    made.append(save(recolor(f"assets/minecraft/textures/item/diamond_{piece}.png"), f"item/chorusite_{piece}"))
for layer in ("humanoid", "humanoid_leggings", "humanoid_baby"):
    src = f"assets/minecraft/textures/entity/equipment/{layer}/diamond.png"
    if src in zf.namelist():
        made.append(save(recolor(src), f"entity/equipment/{layer}/chorusite"))

# pure shard: the ordinary shard refined - paler crystal, with the diamonds it was made from glinting inside
shard = Image.open(f"{OUT}/item/chorusite_shard.png").convert("RGBA")
values = [lum(p) for p in shard.getdata() if p[3]]
lo, hi = min(values), max(values)
pure = shard.copy()
GLINT = [(0xC9, 0xFF, 0xF4), (0x6F, 0xE8, 0xE0)]
bright = []
for y in range(16):
    for x in range(16):
        p = shard.getpixel((x, y))
        if p[3]:
            t = (lum(p) - lo) / (hi - lo)
            pure.putpixel((x, y), gradient(0.2 + 0.8 * t ** 0.7) + (p[3],))
            if 0.35 < t < 0.8:
                bright.append((x, y))
for i, pos in enumerate(bright[::max(1, len(bright) // 5)][:5]):
    pure.putpixel(pos, GLINT[i % 2] + (255,))
made.append(save(pure, "item/pure_chorusite_shard"))

if len(sys.argv) > 3:
    sheet = Image.new("RGBA", (980, 340), (45, 42, 66, 255))
    x = 8
    for im in [i for i in made if i.width == 16] + [shard]:
        big = im.resize((128, 128), Image.NEAREST)
        sheet.paste(big, (x, 8), big)
        x += 136
    x = 8
    for im in [i for i in made if i.width != 16][:3]:
        big = im.resize((384, 192), Image.NEAREST) if im.height * 2 == im.width else im.resize((192, 192), Image.NEAREST)
        sheet.paste(big, (x, 144), big)
        x += big.width + 8
    sheet.save(sys.argv[3])
print("ok", len(made))
