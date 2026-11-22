"""Textures for the Sculk Portal (CC0, drawn for End Realms).
A pool of living darkness: near-black, slowly turning swirl with faint teal veins, and sculk-cyan specks that pulse like
the sculk catalyst's souls. Animated, 16 frames; the side texture is the same dark with veins running up.
Also writes the blockstate, the model (12 px high, like the block's shape) and the item definition."""
import json
import math
import os
from PIL import Image

ASSETS = r"D:\minecraft mod\endrealms\src\main\resources\assets\endrealms"
FRAMES = 16
DARK = (6, 10, 12)
VEIN = (16, 58, 64)
GLOW = (60, 200, 210)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def swirl_frame(f, top):
    img = Image.new("RGBA", (16, 16))
    phase = f / FRAMES * 2 * math.pi
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            if top:
                v = math.sin(a * 3 + r * 0.9 - phase) * 0.5 + 0.5          # three arms turning around the middle
            else:
                v = math.sin(x * 0.9 + math.sin(y * 0.6) * 1.5 - phase) * 0.5 + 0.5   # veins rising up the side
            c = mix(DARK, VEIN, (v ** 3) * 0.9)
            img.putpixel((x, y), c + (255,))
    # pulsing specks of sculk light
    specks = [(3, 4, 0), (11, 3, 5), (6, 11, 9), (12, 12, 13), (8, 7, 3), (2, 13, 11)]
    for sx, sy, off in specks:
        glow = (math.sin((f + off) / FRAMES * 2 * math.pi) + 1) / 2
        if glow > 0.35:
            img.putpixel((sx, sy), mix(VEIN, GLOW, glow) + (255,))
    return img


def strip(top):
    sheet = Image.new("RGBA", (16, 16 * FRAMES))
    for f in range(FRAMES):
        sheet.paste(swirl_frame(f, top), (0, 16 * f))
    return sheet


block_dir = os.path.join(ASSETS, "textures", "block")
for name, top in (("sculk_portal_top", True), ("sculk_portal_side", False)):
    strip(top).save(os.path.join(block_dir, name + ".png"))
    with open(os.path.join(block_dir, name + ".png.mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, f)


def write(path, obj):
    path = os.path.join(ASSETS, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


write("blockstates/sculk_portal.json", {"variants": {"": {"model": "endrealms:block/sculk_portal"}}})
write("models/block/sculk_portal.json", {
    "parent": "minecraft:block/block",
    "textures": {"particle": "endrealms:block/sculk_portal_top", "top": "endrealms:block/sculk_portal_top", "side": "endrealms:block/sculk_portal_side"},
    "elements": [{
        "from": [0, 0, 0], "to": [16, 12, 16],
        "faces": {
            "up": {"texture": "#top", "cullface": "up"},
            "down": {"texture": "#top", "cullface": "down"},
            "north": {"uv": [0, 4, 16, 16], "texture": "#side", "cullface": "north"},
            "south": {"uv": [0, 4, 16, 16], "texture": "#side", "cullface": "south"},
            "west": {"uv": [0, 4, 16, 16], "texture": "#side", "cullface": "west"},
            "east": {"uv": [0, 4, 16, 16], "texture": "#side", "cullface": "east"},
        },
    }],
})
write("items/sculk_portal.json", {"model": {"type": "minecraft:model", "model": "endrealms:block/sculk_portal"}})
print("sculk portal assets written")
