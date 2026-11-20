"""Generates blockstates, models, item definitions, loot tables, recipes and tag entries for the
voidstone and astral block families by copying the matching vanilla (cherry / end stone brick) files
and renaming their ids/texture references. usage: python gen_block_families.py <common jar> <client jar> <resources dir>"""
import json, os, re, sys, zipfile

common, client, RES = zipfile.ZipFile(sys.argv[1]), zipfile.ZipFile(sys.argv[2]), sys.argv[3]

def astral_repl(text):
    for a, b in [("#minecraft:cherry_logs", "#endrealms:astral_logs"),
                 ("minecraft:block/stripped_cherry_", "endrealms:block/stripped_astral_"),
                 ("minecraft:block/cherry_", "endrealms:block/astral_"),
                 ("minecraft:item/cherry_", "endrealms:item/astral_"),
                 ("minecraft:stripped_cherry_", "endrealms:stripped_astral_"),
                 ("minecraft:cherry_", "endrealms:astral_"),
                 ("stripped_cherry_", "stripped_astral_"), ("cherry_", "astral_")]:
        text = text.replace(a, b)
    return text

def stone_repl(full, prefix):
    def f(text):
        for a, b in [("minecraft:block/end_stone_bricks", f"endrealms:block/{full}"),
                     ("minecraft:block/end_stone_brick_", f"endrealms:block/{prefix}_"),
                     ("minecraft:end_stone_bricks", f"endrealms:{full}"),
                     ("minecraft:end_stone_brick_", f"endrealms:{prefix}_"),
                     ("minecraft:end_stone", "endrealms:voidstone"),
                     ("end_stone_bricks", full), ("end_stone_brick_", f"{prefix}_"), ("end_stone", "voidstone")]:
            text = text.replace(a, b)
        return text
    return f

def sandstone_repl(text):
    # sandstone family -> void sandstone family; vanilla sand -> void sand
    text = re.sub(r"(?<![a-z_])(smooth_|cut_|chiseled_)?sandstone", lambda m: (m.group(1) or "") + "void_sandstone", text)
    text = re.sub(r"minecraft:((?:block/|item/)?(?:smooth_|cut_|chiseled_)?void_sandstone)", lambda m: "endrealms:" + m.group(1), text)
    return text.replace('"minecraft:sand"', '"endrealms:void_sand"')


FAMILIES = [(["cherry_fence", "cherry_fence_gate", "cherry_slab", "cherry_stairs", "cherry_door", "cherry_trapdoor",
              "stripped_cherry_log", "cherry_wood", "stripped_cherry_wood"], astral_repl)]
FAMILIES.append((["sandstone", "sandstone_slab", "sandstone_stairs", "sandstone_wall", "smooth_sandstone",
                  "smooth_sandstone_slab", "smooth_sandstone_stairs", "cut_sandstone", "cut_sandstone_slab",
                  "chiseled_sandstone"], sandstone_repl))
for full, prefix in (("voidstone", "voidstone"), ("polished_voidstone", "polished_voidstone"), ("voidstone_bricks", "voidstone_brick")):
    FAMILIES.append((["end_stone_brick_slab", "end_stone_brick_stairs", "end_stone_brick_wall"], stone_repl(full, prefix)))

def read(zf, path):
    return zf.read(path).decode("utf-8")

def write(rel, text):
    p = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "w", encoding="utf-8").write(text)

def model_refs(text):
    return set(re.findall(r'"model": "minecraft:((?:block|item)/[a-z0-9_]+)"', text))

made_ids, recipe_outputs = [], set()
common_names = common.namelist()
for sources, repl in FAMILIES:
    for src in sources:
        dst = repl(src)
        made_ids.append(("minecraft:" + src, "endrealms:" + dst))
        refs = set()
        for kind in ("blockstates", "items"):
            t = read(client, f"assets/minecraft/{kind}/{src}.json")
            refs |= model_refs(t)
            write(f"assets/endrealms/{kind}/{dst}.json", repl(t))
        for ref in refs:
            # template parents (e.g. block/template_wall_post) stay vanilla; only copy models named after the block
            if src in ref:
                write(f"assets/endrealms/models/{repl(ref)}.json", repl(read(client, f"assets/minecraft/models/{ref}.json")))
        write(f"data/endrealms/loot_table/blocks/{dst}.json", repl(read(common, f"data/minecraft/loot_table/blocks/{src}.json")))
        for n in common_names:
            m = re.fullmatch(rf"data/minecraft/recipe/({re.escape(src)}(_from_[a-z_]+_stonecutting)?)\.json", n)
            if m:
                t = repl(read(common, n))
                d = json.loads(t)
                key = json.dumps({k: v for k, v in d.items() if k != "group"}, sort_keys=True)
                if key not in recipe_outputs:  # end_stone & end_stone_bricks both map to voidstone -> dedupe
                    recipe_outputs.add(key)
                    write(f"data/endrealms/recipe/{repl(m.group(1))}.json", t)

# tag membership: wherever vanilla lists a source block/item directly, list ours too
for n in common_names:
    m = re.fullmatch(r"data/minecraft/tags/(block|item)/(.+)\.json", n)
    if not m or "cherry" in m.group(2):
        continue
    vals = json.loads(read(common, n)).get("values", [])
    ours = [dst for v in vals if isinstance(v, str) for src, dst in made_ids if src == v]
    if ours:
        p = os.path.join(RES, "data/minecraft/tags", m.group(1), m.group(2) + ".json")
        d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
        d["values"] += [o for o in ours if o not in d["values"]]
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump(d, open(p, "w"), indent=2)
        print("tag", m.group(1), m.group(2), len(ours))

# all astral logs/wood share one tag (like #minecraft:cherry_logs) which counts as #minecraft:logs
logs = ["endrealms:astral_log", "endrealms:astral_wood", "endrealms:stripped_astral_log", "endrealms:stripped_astral_wood"]
for kind in ("block", "item"):
    write(f"data/endrealms/tags/{kind}/astral_logs.json", json.dumps({"values": logs}, indent=2))
    p = os.path.join(RES, f"data/minecraft/tags/{kind}/logs.json")
    d = json.load(open(p))
    d["values"] = [v for v in d["values"] if v != "endrealms:astral_log"]
    if "#endrealms:astral_logs" not in d["values"]:
        d["values"].append("#endrealms:astral_logs")
    json.dump(d, open(p, "w"), indent=2)

# axe stripping: vanilla block_transformer/axe.json + our two rules
axe = json.loads(read(common, "data/minecraft/block_transformer/axe.json"))
rules = axe[0]["block_state_provider"]["rules"]
for frm, to in (("endrealms:astral_wood", "endrealms:stripped_astral_wood"), ("endrealms:astral_log", "endrealms:stripped_astral_log")):
    rules.insert(0, {"if_true": {"type": "minecraft:matching_blocks", "blocks": frm},
                     "then": {"type": "minecraft:copy_properties", "source": {"id": to, "properties": {"axis": "y"}}}})
write("data/minecraft/block_transformer/axe.json", json.dumps(axe, indent=2))
print("blocks:", len(made_ids))
