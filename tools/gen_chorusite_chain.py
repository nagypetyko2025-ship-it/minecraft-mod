"""JSON for the netherite-style chorusite chain (ore -> shard, 4 shards + 4 diamonds -> pure shard, pure shard -> ingot)
and for the chorusite armor. usage: python gen_chorusite_chain.py <resources dir>"""
import json
import os
import sys

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


w(f"{D}/recipe/pure_chorusite_shard.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                            "ingredients": ["endrealms:chorusite_shard"] * 4 + ["minecraft:diamond"] * 4 + [["endrealms:phasite_ore", "endrealms:phasite_shard"]],
                                            "result": {"id": "endrealms:pure_chorusite_shard"}})
for kind in ("smelting", "blasting"):
    p = f"{D}/recipe/chorusite_ingot_from_{kind}.json"
    d = r(p)
    d["ingredient"] = "endrealms:pure_chorusite_shard"
    w(p, d)

# one shard per ore, whatever the pickaxe: like ancient debris, Fortune must not multiply it
for ore in ("chorusite_ore", "voidstone_chorusite_ore"):
    p = f"{D}/loot_table/blocks/{ore}.json"
    d = r(p)
    for child in d["pools"][0]["entries"][0]["children"]:
        if "modifier" in child:
            child["modifier"] = [m for m in child["modifier"] if m["type"] != "minecraft:apply_bonus"]
    w(p, d)

PIECES = {"helmet": "head_armor", "chestplate": "chest_armor", "leggings": "leg_armor", "boots": "foot_armor"}
for piece, tag in PIECES.items():
    name = f"chorusite_{piece}"
    w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})
    w(f"{D}/recipe/{name}_smithing.json", {"type": "minecraft:smithing_transform", "template": "endrealms:chorusite_upgrade_smithing_template",
                                           "base": f"minecraft:netherite_{piece}", "addition": "#endrealms:chorusite_tool_materials",
                                           "result": {"id": f"endrealms:{name}"}})
    p = f"{RES}/data/minecraft/tags/item/{tag}.json"
    t = r(p) if os.path.exists(p) else {"replace": False, "values": []}
    if f"endrealms:{name}" not in t["values"]:
        t["values"].append(f"endrealms:{name}")
    w(p, t)

w(f"{A}/models/item/pure_chorusite_shard.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "endrealms:item/pure_chorusite_shard"}})
w(f"{A}/items/pure_chorusite_shard.json", {"model": {"type": "minecraft:model", "model": "endrealms:item/pure_chorusite_shard"}})
LAYER = [{"texture": "endrealms:chorusite"}]
w(f"{A}/equipment/chorusite.json", {"layers": {"humanoid": LAYER, "humanoid_baby": LAYER, "humanoid_leggings": LAYER}})

NAMES = {"en_us": {"item.endrealms.pure_chorusite_shard": "Pure Chorusite Shard", "item.endrealms.chorusite_helmet": "Chorusite Helmet",
                   "item.endrealms.chorusite_chestplate": "Chorusite Chestplate", "item.endrealms.chorusite_leggings": "Chorusite Leggings",
                   "item.endrealms.chorusite_boots": "Chorusite Boots"},
         "hu_hu": {"item.endrealms.pure_chorusite_shard": "Nyers tiszta chorusit-szilánk", "item.endrealms.chorusite_helmet": "Chorusit sisak",
                   "item.endrealms.chorusite_chestplate": "Chorusit mellvért", "item.endrealms.chorusite_leggings": "Chorusit lábvért",
                   "item.endrealms.chorusite_boots": "Chorusit csizma"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    d.update(vals)
    w(p, d)
print("ok")
