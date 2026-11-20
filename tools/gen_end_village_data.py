"""Writes the JSON of the End Village: structure, placement, biome tag, chest loot, spawn egg model and translations.
usage: python gen_end_village_data.py <resources dir>"""
import json
import os
import sys

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


w(f"{D}/worldgen/structure/end_village.json", {"type": "endrealms:end_village", "biomes": "#endrealms:has_structure/end_village",
                                               "step": "surface_structures", "spawn_overrides": {}})
w(f"{D}/worldgen/structure_set/end_villages.json", {"placement": {"type": "minecraft:random_spread", "salt": 51820377, "separation": 12, "spacing": 28,
                                                                  "spread_type": "triangular", "locate_offset": [8, 0, 8]},
                                                    "structures": [{"structure": "endrealms:end_village", "weight": 1}]})
w(f"{D}/tags/worldgen/biome/has_structure/end_village.json", {"values": ["minecraft:end_highlands", "minecraft:end_midlands"]})


def item(name, weight, lo, hi):
    return {"type": "minecraft:item", "name": name, "weight": weight,
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}


w(f"{D}/loot_table/chests/end_village.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
        item("minecraft:chorus_fruit", 10, 2, 6), item("minecraft:popped_chorus_fruit", 6, 1, 4), item("endrealms:cooked_glimmerhoof", 8, 1, 4),
        item("endrealms:cooked_starfin", 6, 1, 3), item("endrealms:prism_shard", 6, 1, 5), item("minecraft:end_rod", 5, 1, 4),
        item("minecraft:ender_pearl", 3, 1, 2), item("endrealms:astral_sapling", 3, 1, 2), item("endrealms:voidbloom", 4, 1, 3)]},
    # a little starting money, so the first trades are possible right away
    {"rolls": 1, "condition": {"type": "minecraft:random_chance", "chance": 0.5}, "entries": [item("endrealms:chorusite_shard", 1, 1, 3)]}],
    "random_sequence": "endrealms:chests/end_village"})

w(f"{A}/models/item/end_villager_spawn_egg.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "endrealms:item/end_villager_spawn_egg"}})
w(f"{A}/items/end_villager_spawn_egg.json", {"model": {"type": "minecraft:model", "model": "endrealms:item/end_villager_spawn_egg"}})

JOBS = {
    "en_us": {"toolsmith": "Chorusite Smith", "mason": "Voidstone Mason", "butcher": "Glimmerhoof Butcher", "fisherman": "Basin Fisher",
              "cleric": "Chorus Cleric", "librarian": "Star Archivist", "forester": "Astral Forester", "jeweler": "Prism Jeweler"},
    "hu_hu": {"toolsmith": "Chorusit-kovács", "mason": "Űrkő-kőműves", "butcher": "Csillámpata-mészáros", "fisherman": "Medencei halász",
              "cleric": "Chorus-pap", "librarian": "Csillaglevéltáros", "forester": "Asztrál erdész", "jeweler": "Prizmaékszerész"}}
BASE = {"en_us": {"entity.endrealms.end_villager": "End Villager", "item.endrealms.end_villager_spawn_egg": "End Villager Spawn Egg",
                  "structure.endrealms.end_village": "End Village"},
        "hu_hu": {"entity.endrealms.end_villager": "Endfalusi", "item.endrealms.end_villager_spawn_egg": "Endfalusi idéző tojás",
                  "structure.endrealms.end_village": "Endfalu"}}
for lang in JOBS:
    p = f"{A}/lang/{lang}.json"
    with open(p, encoding="utf-8") as f:
        d = json.load(f)
    d.update(BASE[lang])
    d.update({f"entity.endrealms.end_villager.{k}": v for k, v in JOBS[lang].items()})
    w(p, d)
print("ok")
