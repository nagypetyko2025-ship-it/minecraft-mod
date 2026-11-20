"""The later End Realms advancements. AdvancementLayout.java decides which side of the tab each branch grows towards.
usage: python gen_more_advancements.py <resources dir>"""
import json
import os
import sys

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
E = "endrealms:"


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def has(*items):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": list(items)}]}}


def killed(entity):
    return {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": "minecraft:entity_properties", "entity": "this",
                                                                                   "predicate": {"minecraft:entity_type": E + entity}}}}


def eaten(item):
    return {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": E + item}}}


FISH = [E + "raw_starfin", E + "raw_void_eel", E + "raw_glowgill"]
GEMS = ["ember_garnet", "star_sapphire", "void_onyx", "moon_opal", "ash_quartz", "bloodstone", "deep_aquamarine"]
SANCTUM = {"trigger": "minecraft:location", "conditions": {"player": {"type": "minecraft:entity_properties", "entity": "this",
                                                                      "predicate": {"minecraft:location": {"structures": E + "rift_sanctum"}}}}}
TRADE = {"trigger": "minecraft:villager_trade", "conditions": {"villager": {"type": "minecraft:entity_properties", "entity": "this",
                                                                            "predicate": {"minecraft:entity_type": E + "end_villager"}}}}
# id, parent, icon, frame, criterion
SIMPLE = [
    ("end_trade", "end_village", E + "chorusite_shard", "task", TRADE),
    ("void_sentinel", "void_bastion", E + "void_sentinel_spawn_egg", "task", killed("void_sentinel")),
    ("shardcrab", "root", E + "prism_shard", "task", killed("shardcrab")),
    ("ash_wraith", "shardcrab", E + "ash_dust", "task", killed("ash_wraith")),
    ("astral_fish", "root", E + "raw_starfin", "task", {"trigger": "minecraft:fishing_rod_hooked", "conditions": {"item": {"items": FISH}}}),
    ("deep_lurker", "astral_fish", E + "deep_lurker_spawn_egg", "task", killed("deep_lurker")),
    ("deepglow", "deep_lurker", E + "deepglow_shard", "task", has(E + "deepglow_shard")),
    ("starberry", "root", E + "starberry", "task", eaten("starberry")),
    ("moonbell_nectar", "starberry", E + "moonbell_nectar", "task", eaten("moonbell_nectar")),
    ("rift_sanctum", "chorusite_armor", E + "gem_table", "task", SANCTUM),
    ("riftblade", "rift_lord", E + "riftblade", "challenge", has(E + "riftblade")),
    ("phase_pearl", "phasite", E + "phase_pearl", "task", has(E + "phase_pearl")),
    ("phase_anchor", "phase_pearl", E + "phase_anchor", "task", has(E + "phase_anchor")),
    ("gem_socket", "gem", E + "gem_table", "task", {"trigger": "minecraft:impossible"}),
]


def display(key, icon, frame):
    return {"icon": {"id": icon}, "title": {"translate": f"advancements.endrealms.{key}.title"},
            "description": {"translate": f"advancements.endrealms.{key}.description"}, "frame": frame}


for key, parent, icon, frame, criterion in SIMPLE:
    w(f"{D}/advancement/{key}.json", {"parent": E + parent, "display": display(key, icon, frame), "criteria": {"done": criterion}, "requirements": [["done"]]})
w(f"{D}/advancement/all_gems.json", {"parent": E + "gem_socket", "display": display("all_gems", E + "bloodstone", "challenge"),
                                     "criteria": {g: has(E + g) for g in GEMS}, "requirements": [[g] for g in GEMS]})
adv = r(f"{D}/advancement/gem.json")
adv["criteria"]["done"] = has(*[E + g for g in GEMS])
w(f"{D}/advancement/gem.json", adv)

T = "advancements.endrealms."
NAMES = {
    "en_us": {"end_trade": ("A Fair Deal", "Trade with an End Villager"), "void_sentinel": ("Sentry Duty Over", "Defeat a Void Sentinel"),
              "shardcrab": ("Not a Crystal", "Defeat a Shardcrab"), "ash_wraith": ("Ashes to Ashes", "Defeat an Ash Wraith"),
              "astral_fish": ("Astral Angler", "Catch a fish in astral water"), "starberry": ("Falling Star", "Eat a Starberry"),
              "moonbell_nectar": ("Clear as Moonlight", "Drink Moonbell Nectar"), "rift_sanctum": ("Stairs into the Dark", "Find a Rift Sanctum"),
              "riftblade": ("Rift in Hand", "Forge the Riftblade"), "phase_pearl": ("Pocket Rift", "Craft a Phase Pearl"),
              "phase_anchor": ("Anchored", "Craft a Phase Anchor"), "gem_socket": ("Set in Stone", "Set a gem into an item at a Gem Table"),
              "all_gems": ("Master Jeweler", "Obtain every kind of gem")},
    "hu_hu": {"end_trade": ("Tisztes üzlet", "Kereskedj egy Endfalusival"), "void_sentinel": ("Őrségváltás", "Győzz le egy Űrőrzőt"),
              "shardcrab": ("Mégsem kristály", "Győzz le egy Szilánkrákot"), "ash_wraith": ("Hamuból hamuba", "Győzz le egy Hamulidércet"),
              "astral_fish": ("Asztrál horgász", "Fogj halat asztrál vízben"), "starberry": ("Hullócsillag", "Egyél Csillagbogyót"),
              "moonbell_nectar": ("Tiszta, mint a holdfény", "Igyál Harangnektárt"), "rift_sanctum": ("Lépcső a sötétbe", "Találj rá egy Hasadékszentélyre"),
              "riftblade": ("Hasadék a kézben", "Kovácsold meg a Hasadékpengét"), "phase_pearl": ("Zsebhasadék", "Készíts Fázisgyöngyöt"),
              "phase_anchor": ("Lehorgonyozva", "Készíts Fázishorgonyt"), "gem_socket": ("Kőbe foglalva", "Foglalj ékkövet egy tárgyba az Ékkőasztalon"),
              "all_gems": ("Mesterékszerész", "Szerezd meg az összes ékkőfajtát")}}
for lang, vals in NAMES.items():
    p = f"{A}/lang/{lang}.json"
    d = r(p)
    for key, (title, description) in vals.items():
        d[f"{T}{key}.title"] = title
        d[f"{T}{key}.description"] = description
    w(p, d)
print("ok")
