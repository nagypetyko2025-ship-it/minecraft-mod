"""Recolors vanilla textures (read from the Minecraft client jar) into chorusite versions.
NOTE: outputs are derived from Mojang's textures -> not CC0, fine for personal use / mods,
but should be redrawn before any public release under CC0.
usage: python recolor_vanilla.py <client jar> <out textures/item dir> [preview.png]"""
import colorsys, io, sys, zipfile
from PIL import Image

jar, out = sys.argv[1], sys.argv[2]
zf = zipfile.ZipFile(jar)

def load(name):
    return Image.open(io.BytesIO(zf.read(f"assets/minecraft/textures/item/{name}.png"))).convert("RGBA")

def hexc(h):
    h = h.lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))

def gradient(stops):
    stops = [(p, hexc(c)) for p, c in stops]
    def f(t):
        t = max(stops[0][0], min(stops[-1][0], t))
        for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
            if p0 <= t <= p1:
                k = 0 if p1 == p0 else (t - p0) / (p1 - p0)
                return tuple(round(a + (b - a) * k) for a, b in zip(c0, c1))
    return f

# chorusite crystal colors, dark outline -> light highlight
CHORUSITE = gradient([(0.15, "#2C0A39"), (0.25, "#4B1A5E"), (0.40, "#6E2A8A"), (0.55, "#9437B0"),
                      (0.70, "#B94DC6"), (0.80, "#D861CF"), (0.92, "#F18BE4"), (1.00, "#FFCDF5")])
END_STONE = gradient([(0.20, "#5F5E38"), (0.32, "#8E8D58"), (0.40, "#ACAD71"), (0.46, "#C3C487"),
                      (0.52, "#D9DA9C")])

def hsv(p):
    return colorsys.rgb_to_hsv(*(x / 255 for x in p[:3]))

def recolor(img, rule):
    res = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p[3]:
                c = rule(p, *hsv(p))
                if c:
                    res.putpixel((x, y), c + (p[3],))
    return res

def tool_rule(p, h, s, v):
    if 150 / 360 <= h <= 200 / 360:          # diamond head (cyan/teal); brown handle stays
        if s < 0.5:                           # pale highlight pixels
            v = 1.0
        return CHORUSITE(v)
    return None

def template_rule(p, h, s, v):
    if h < 20 / 360 or h > 340 / 360:         # red netherite body -> end stone body
        return END_STONE(v)
    if 170 / 360 <= h <= 200 / 360:           # cyan diamond inlay -> chorusite
        return CHORUSITE(min(1.0, v + 0.08))
    return None

gold = load("gold_ingot")
lums = [0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2] for p in gold.getdata() if p[3]]
lo, hi = min(lums), max(lums)
def ingot_rule(p, h, s, v):
    l = (0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2] - lo) / (hi - lo)
    return CHORUSITE(0.25 + 0.75 * l)

made = {}
for tool in ("sword", "shovel", "pickaxe", "axe", "hoe"):
    made[f"chorusite_{tool}"] = recolor(load(f"diamond_{tool}"), tool_rule)
made["chorusite_ingot"] = recolor(gold, ingot_rule)
made["chorusite_upgrade_smithing_template"] = recolor(load("netherite_upgrade_smithing_template"), template_rule)
WATER = gradient([(0.20, "#3A1452"), (0.45, "#6A2A92"), (0.70, "#9442C8"), (0.85, "#A64BDB"), (1.00, "#D7A2F2")])
def bucket_rule(p, h, s, v):
    if 190 / 360 <= h <= 250 / 360 and s > 0.25:  # blue water inside the bucket
        return WATER(v)
    return None
made["astral_water_bucket"] = recolor(load("water_bucket"), bucket_rule)

# particle texture for the fluid block: first frame of the greyscale water_still, tinted like in-game
still = Image.open(io.BytesIO(zf.read("assets/minecraft/textures/block/water_still.png"))).convert("RGBA").crop((0, 0, 16, 16))
tint = hexc("#A64BDB")
particle = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        r, g, b, a = still.getpixel((x, y))
        particle.putpixel((x, y), (r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255, 255))
particle.save(out.replace("textures/item", "textures/block") + "/astral_water_particle.png")

for name, img in made.items():
    img.save(f"{out}/{name}.png")

if len(sys.argv) > 3:
    imgs = list(made.values())
    sheet = Image.new("RGBA", (len(imgs) * 136 + 8, 144), (40, 40, 48, 255))
    for i, im in enumerate(imgs):
        big = im.resize((128, 128), Image.NEAREST)
        sheet.paste(big, (8 + i * 136, 8), big)
    sheet.save(sys.argv[3])
print("written:", ", ".join(made))
