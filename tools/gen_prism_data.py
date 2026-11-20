"""Generates all JSON of the Prism Wastes content: copies the vanilla amethyst bud/cluster files under new names and
writes the biome, features, loot, recipes, tags and translations.
usage: python gen_prism_data.py <client jar> <resources dir>"""
import json
import os
import sys
import zipfile

jar, RES = zipfile.ZipFile(sys.argv[1]), sys.argv[2]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


RENAMES = [("amethyst_cluster", "prism_cluster"), ("small_amethyst_bud", "small_prism_bud"),
           ("medium_amethyst_bud", "medium_prism_bud"), ("large_amethyst_bud", "large_prism_bud"), ("amethyst_shard", "prism_shard")]


def rename(text):
    for a, b in RENAMES:
        text = text.replace(f"minecraft:block/{a}", f"endrealms:block/{b}").replace(f"minecraft:item/{a}", f"endrealms:item/{b}")
        text = text.replace(f"minecraft:{a}", f"endrealms:{b}").replace(a, b)
    return text


BUDS = ["amethyst_cluster", "small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud"]
for name in BUDS:
    new = rename(name)
    for vanilla, ours in [(f"assets/minecraft/blockstates/{name}.json", f"{A}/blockstates/{new}.json"),
                          (f"assets/minecraft/models/block/{name}.json", f"{A}/models/block/{new}.json"),
                          (f"assets/minecraft/models/item/{name}.json", f"{A}/models/item/{new}.json"),
                          (f"assets/minecraft/items/{name}.json", f"{A}/items/{new}.json"),
                          (f"data/minecraft/loot_table/blocks/{name}.json", f"{D}/loot_table/blocks/{new}.json")]:
        w(ours, json.loads(rename(jar.read(vanilla).decode("utf-8"))))

FULL = ["prism_crystal_block", "budding_prism_crystal", "crystalline_voidstone", "prism_lamp"]
for name in FULL:
    w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"endrealms:block/{name}"}}})
    w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"endrealms:block/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:block/{name}"}})
    if name != "budding_prism_crystal":     # like budding amethyst, it can never be picked up
        w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"endrealms:{name}"}], "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": f"endrealms:blocks/{name}"})

for name in ("prism_shard", "shardcrab_spawn_egg"):
    w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"endrealms:item/{name}"}})
    w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"endrealms:item/{name}"}})

w(f"{D}/recipe/prism_crystal_block.json", {"type": "minecraft:crafting_shaped", "category": "building",
                                           "key": {"#": "endrealms:prism_shard"}, "pattern": ["##", "##"],
                                           "result": {"id": "endrealms:prism_crystal_block"}})
w(f"{D}/recipe/prism_lamp.json", {"type": "minecraft:crafting_shaped", "category": "building",
                                  "key": {"#": "endrealms:prism_shard", "R": "minecraft:end_rod"},
                                  "pattern": [" # ", "#R#", " # "], "result": {"id": "endrealms:prism_lamp"}})
w(f"{D}/recipe/crystalline_voidstone.json", {"type": "minecraft:crafting_shapeless", "category": "building",
                                             "ingredients": ["endrealms:voidstone", "endrealms:prism_shard"],
                                             "result": {"id": "endrealms:crystalline_voidstone"}})


def add_tag(path, values):
    p = f"{RES}/data/minecraft/tags/{path}.json"
    t = {"replace": False, "values": []}
    if os.path.exists(p):
        with open(p, encoding="utf-8") as f:
            t = json.load(f)
    t["values"] += [v for v in values if v not in t["values"]]
    w(p, t)


NEW_BUDS = [f"endrealms:{rename(n)}" for n in BUDS]
add_tag("block/mineable/pickaxe", [f"endrealms:{n}" for n in FULL] + NEW_BUDS)
# heightmaps are tag-driven in 26.x: a solid block missing from this tag is invisible to OCEAN_FLOOR/MOTION_BLOCKING_NO_LEAVES
add_tag("block/blocks_motion_no_leaves", NEW_BUDS + [f"endrealms:{n}" for n in FULL] +
        ["endrealms:voidstone", "endrealms:polished_voidstone", "endrealms:voidstone_bricks"])
add_tag("block/crystal_sound_blocks", ["endrealms:prism_crystal_block", "endrealms:budding_prism_crystal"])

w(f"{D}/worldgen/feature/prism_spire.json", {"type": "endrealms:prism_spire"})
w(f"{D}/worldgen/feature/prism_patch.json", {"type": "endrealms:prism_patch"})
GROUND = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}]
w(f"{D}/worldgen/placed_feature/prism_spire.json", {"feature": "endrealms:prism_spire",
                                                    "placement": [{"type": "minecraft:rarity_filter", "chance": 2}] + GROUND})
w(f"{D}/worldgen/placed_feature/prism_patch.json", {"feature": "endrealms:prism_patch",
                                                    "placement": [{"type": "minecraft:count", "count": 3}] + GROUND})

w(f"{D}/worldgen/biome/prism_wastes.json", {
    "attributes": {
        "minecraft:gameplay/natural_mob_spawns": {"argument": {"spawn_costs": {}, "spawns_by_category": {"monster": [
            {"type": "minecraft:enderman", "count": 4, "weight": 6},
            {"type": "endrealms:shardcrab", "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 3}, "weight": 10}]}},
            "modifier": "overlay"},
        "minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:glow"}, "probability": 0.012}], "modifier": "append"},
        "minecraft:visual/fog_color": "#1F5F6B"},
    "carvers": [], "downfall": 0.5, "effects": {"water_color": "#3f76e4"},
    "features": [[], [], [], [], ["minecraft:end_gateway_return"], [], [], [], [],
                 ["endrealms:prism_patch", "endrealms:prism_spire"]],
    "has_precipitation": False, "temperature": 0.5})

w(f"{D}/loot_table/entities/shardcrab.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [{
    "type": "minecraft:item", "name": "endrealms:prism_shard", "modifier": [
        {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
        {"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0}, "enchantment": "minecraft:looting"}]}]}],
    "random_sequence": "endrealms:entities/shardcrab"})

rule_path = f"{RES}/data/minecraft/worldgen/material_rule/end.json"
with open(rule_path, encoding="utf-8") as f:
    rules = json.load(f)
if "prism_wastes" not in json.dumps(rules):
    rules["sequence"].insert(1, {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": "endrealms:prism_wastes"},
                                 "then_run": {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "add_surface_depth": False, "offset": 2,
                                                                                         "secondary_depth_range": 0, "surface_type": "floor"},
                                              "then_run": {"type": "minecraft:block", "result_state": "endrealms:crystalline_voidstone"}}})
    w(rule_path, rules)

NAMES = {
    "en_us": {"biome.endrealms.prism_wastes": "Prism Wastes", "block.endrealms.prism_crystal_block": "Block of Prism Crystal",
              "block.endrealms.budding_prism_crystal": "Budding Prism Crystal", "block.endrealms.small_prism_bud": "Small Prism Bud",
              "block.endrealms.medium_prism_bud": "Medium Prism Bud", "block.endrealms.large_prism_bud": "Large Prism Bud",
              "block.endrealms.prism_cluster": "Prism Cluster", "block.endrealms.prism_lamp": "Prism Lamp",
              "block.endrealms.crystalline_voidstone": "Crystalline Voidstone", "item.endrealms.prism_shard": "Prism Shard",
              "entity.endrealms.shardcrab": "Shardcrab", "item.endrealms.shardcrab_spawn_egg": "Shardcrab Spawn Egg"},
    "hu_hu": {"biome.endrealms.prism_wastes": "Kristályrengeteg", "block.endrealms.prism_crystal_block": "Prizmakristály blokk",
              "block.endrealms.budding_prism_crystal": "Bimbózó prizmakristály", "block.endrealms.small_prism_bud": "Kis prizmabimbó",
              "block.endrealms.medium_prism_bud": "Közepes prizmabimbó", "block.endrealms.large_prism_bud": "Nagy prizmabimbó",
              "block.endrealms.prism_cluster": "Prizmacsokor", "block.endrealms.prism_lamp": "Kristálylámpa",
              "block.endrealms.crystalline_voidstone": "Kristályos űrkő", "item.endrealms.prism_shard": "Prizmaszilánk",
              "entity.endrealms.shardcrab": "Szilánkrák", "item.endrealms.shardcrab_spawn_egg": "Szilánkrák idéző tojás"}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    with open(p, encoding="utf-8") as f:
        d = json.load(f)
    d.update(vals)
    w(p, d)
print("ok")
