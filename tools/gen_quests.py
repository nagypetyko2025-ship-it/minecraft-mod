"""The Endwalker's Guide quest tree. ADD NEW CONTENT HERE: one Q(...) line per quest, then re-run. Layout is automatic.
Objectives are read from the player's advancements, inventory and statistics, so finished things count immediately.
usage: python gen_quests.py <resources dir>"""
import json
import os
import sys

RES = sys.argv[1]
D, A = f"{RES}/data/endrealms", f"{RES}/assets/endrealms"
E = "endrealms:"
M = "minecraft:"


def r(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(o, f, indent=2, ensure_ascii=False)


TAGS = {"#endrealms:chorusite_tool_materials": [E + "chorusite_ingot"],
        "#minecraft:logs": [M + w + "_log" for w in ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak")]
        + [M + "crimson_stem", M + "warped_stem"]}


def options(ingredient):
    if isinstance(ingredient, list):
        return ingredient
    return TAGS[ingredient] if ingredient.startswith("#") else [ingredient]


def recipe(name):
    data = r(f"{D}/recipe/{name}.json")
    kind = data["type"].split(":")[1]
    result = data["result"]["id"]
    if kind == "crafting_shaped":
        pattern = [(row + "   ")[:3] for row in data["pattern"]] + ["   "] * (3 - len(data["pattern"]))
        return {"kind": "crafting", "slots": [options(data["key"][c]) if c != " " else [] for row in pattern for c in row], "result": result}
    if kind == "end_crafting":   # the End Crafting Table's 5x5 grid, centred like the table places it
        rows = data["pattern"]
        h, wd = len(rows), max(len(row) for row in rows)
        top, left = (5 - h) // 2, (5 - wd) // 2
        grid = [[] for _ in range(25)]
        for y, row in enumerate(rows):
            for x, c in enumerate(row):
                if c != " ":
                    grid[(top + y) * 5 + left + x] = options(data["key"][c])
        return {"kind": "end_crafting", "slots": grid, "result": result}
    if kind == "crafting_shapeless":
        slots = [options(i) for i in data["ingredients"]]
        return {"kind": "crafting", "slots": slots + [[]] * (9 - len(slots)), "result": result}
    if kind == "smelting":
        return {"kind": "furnace", "slots": [options(data["ingredient"])], "result": result}
    if kind == "smithing_transform":
        return {"kind": "smithing", "slots": [options(data["template"]), options(data["base"]), options(data["addition"])], "result": result}
    raise ValueError(f"{name}: {kind}")


def adv(i, criterion=None):
    o = {"type": "advancement", "id": i if ":" in i else E + i}
    if criterion:
        o["criterion"] = criterion
    return o


def biome(b):
    return adv("all_biomes", b)


def item(i, n=1):
    return {"type": "item", "id": i if ":" in i else E + i, "count": n}


def kill(e, n=1):
    return {"type": "kill", "id": e if ":" in e else E + e, "count": n}


def give(i, n=1):
    return {"item": i if ":" in i else E + i, "count": n}


CHAPTERS = [("path", E + "end_guide", "Whole Path", "Teljes út"), ("start", M + "ender_eye", "Start", "Kezdet"), ("isles", E + "astral_sapling", "Islands", "Szigetek"),
            ("ruins", E + "voidstone_bricks", "Ruins", "Romok"), ("chorusite", E + "chorusite_ingot", "Chorusite", "Chorusit"),
            ("jewels", E + "ember_garnet", "Gems", "Ékkövek"), ("rift", E + "rift_heart", "Rift", "Hasadék"), ("brew", E + "fermentation_station", "Brewing", "Pálinkafőzés"), ("misc", E + "backpack", "Other", "Egyéb")]
QUESTS = []


def Q(cid, chapter, icon, requires, objectives, rewards, xp, recipes, en, hu):
    """en and hu are (title, description[, goal]); the goal is the objective line for advancement objectives."""
    QUESTS.append({"id": cid, "chapter": chapter, "icon": icon if ":" in icon else E + icon, "requires": requires, "objectives": objectives, "rewards": rewards, "xp": xp,
                   "recipes": [recipe(n) for n in recipes], "en": en, "hu": hu})


# ---- First Steps
Q("enter_end", "start", M + "ender_eye", [], [adv(M + "end/root")], [give(M + "ender_pearl", 16), give(M + "golden_apple", 4)], 100, ["end_guide"],
  ("The Gate", "Find a stronghold and step through the End Portal. Lost this guide? A book and a piece of end stone make a new one.", "Enter the End"),
  ("A kapu", "Keress erődöt, és lépj át az End-kapun. Ha elveszíted ezt a kalauzt, egy könyvből és egy endkőből újat készíthetsz.", "Lépj be az Endbe"))
Q("dragon", "start", M + "dragon_head", ["enter_end"], [adv(M + "end/kill_dragon")], [give(M + "diamond", 16), give(M + "golden_apple", 8), give(M + "enchanted_golden_apple", 1), give(M + "netherite_ingot", 1)], 1000, [],
  ("The Old Ruler", "The End Realms only open up once the dragon is gone.", "Defeat the Ender Dragon"),
  ("A régi uralkodó", "Az End Realms csak a sárkány bukása után nyílik meg igazán.", "Győzd le az Endersárkányt"))
Q("gateway", "start", M + "ender_pearl", ["dragon"], [adv(M + "end/enter_end_gateway")], [give(M + "ender_pearl", 16), give("cooked_glimmerhoof", 16)], 150, [],
  ("Beyond the Gateway", "Throw an ender pearl into the gateway at the edge of the main island. The outer islands wait behind it.", "Pass through an End Gateway"),
  ("Az átjárón túl", "Dobj ender gyöngyöt a főszigeten kinyíló átjáróba. Mögötte várnak a külső szigetek.", "Haladj át egy End-átjárón"))
Q("end_city", "start", M + "purpur_block", ["gateway"], [adv(M + "end/find_end_city")], [give(M + "shulker_shell", 4), give(M + "firework_rocket", 32)], 200, [],
  ("Towers of Purpur", "The old End Cities still stand among the islands.", "Find an End City"),
  ("Lila tornyok", "A régi End-városok még mindig állnak a szigetek között.", "Találj egy End-várost"))
Q("elytra", "start", M + "elytra", ["end_city"], [adv(M + "end/elytra")], [give(M + "firework_rocket", 64), give(M + "phantom_membrane", 8), give(M + "diamond", 8)], 500, [],
  ("Wings", "An elytra hangs on the ships of the End Cities. Nothing crosses the void faster.", "Obtain an Elytra"),
  ("Szárnyak", "Az End-városok hajóin elytra lóg. Semmivel nem jutsz át gyorsabban a semmin.", "Szerezz elytrát"))

# ---- The Islands
Q("forest", "isles", "astral_sapling", [], [biome("astral_forest")], [give("astral_sapling", 8), give("end_soil", 32), give("voidbloom", 8), give("cooked_glimmerhoof", 16)], 150, [],
  ("Astral Forest", "Tall thin trees on purple soil, the gentlest place in the End.", "Set foot in an Astral Forest"),
  ("Asztrál erdő", "Magas, vékony fák lila földön, az End legbarátságosabb helye.", "Lépj be egy Asztrál erdőbe"))
Q("astral_wood", "isles", "astral_log", ["forest"], [item("astral_log", 16)], [give("astral_planks", 64), give("polished_voidstone", 32)], 100, [],
  ("Timber", "Astral wood builds nearly everything the End's villagers live in.", ""), ("Faanyag", "Az asztrál fából készül szinte minden, amiben az Endfalusiak élnek.", ""))
Q("glimmerhoof", "isles", "cooked_glimmerhoof", ["forest"], [kill("glimmerhoof", 3)], [give("cooked_glimmerhoof", 32), give(M + "leather", 16)], 150, [],
  ("Glimmerhoof Hunt", "Glimmerhoofs graze under the canopy. Their meat keeps well.", ""), ("Csillámpata-vadászat", "A Csillámpaták a lombok alatt legelnek, a húsuk tápláló.", ""))
Q("glimmer_ride", "isles", M + "saddle", ["glimmerhoof"], [adv("glimmerhoof_ride")], [give(M + "chorus_fruit", 32), give(M + "golden_apple", 4), give(M + "diamond", 4)], 200, [],
  ("Leap of Faith", "Put a saddle on a grown Glimmerhoof and ride it. Hold jump and let go: it leaps far higher than a horse, and its long legs take the fall.", "Ride a saddled Glimmerhoof"),
  ("Hitugrás", "Tegyél nyerget egy felnőtt Csillámpatára, és ülj fel rá. Tartsd nyomva, majd engedd el az ugrást: sokkal magasabbra ugrik egy lónál, és hosszú lábai felfogják az esést.", "Lovagolj meg egy felnyergelt Csillámpatát"))
Q("starberry", "isles", "starberry", ["forest"], [adv("starberry")], [give("starberry", 16), give("moonbell_nectar", 4)], 100, [],
  ("Falling Star", "Starreed grows only here. Its berries slow your fall and quicken your step. Planted on End Soil a berry grows a new cane; shears take the cane itself.", "Eat a Starberry"),
  ("Hullócsillag", "A Csillagnád csak itt nő. A bogyója lassítja az esést és gyorsít. End-földbe ültetve új nád nő belőle; ollóval magát a nádat szedheted le.", "Egyél Csillagbogyót"))
Q("nectar", "isles", "moonbell_nectar", ["starberry"], [adv("moonbell_nectar")], [give("moonbell_nectar", 8), give(M + "golden_carrot", 16)], 100, [],
  ("Moonbell", "The nectar of the Moonbell lets you see in the dark. Planted on End Soil it grows a new Moonbell; shears take the whole plant.", "Drink Moonbell Nectar"),
  ("Holdharang", "A Holdharang nektárjával a sötétben is látsz. End-földbe ültetve új Holdharang nő belőle; ollóval az egész növényt leszedheted.", "Igyál Harangnektárt"))
Q("basin", "isles", "raw_starfin", [], [biome("astral_basin")], [give(M + "fishing_rod", 1), give("cooked_glowgill", 16), give("astral_water_bucket", 1), give("prism_shard", 16), give(M + "diamond", 4)], 250, [],
  ("Astral Basin", "A ring of mountains around a glowing lake.", "Set foot in an Astral Basin"),
  ("Asztrál medence", "Hegygyűrű egy világító tó körül.", "Lépj be egy Asztrál medencébe"))
Q("fishing", "isles", "raw_glowgill", ["basin"], [adv("astral_fish")], [give("cooked_starfin", 32), give("cooked_void_eel", 32), give("cooked_glowgill", 8)], 200, ["cooked_starfin"],
  ("Astral Angler", "Astral water only gives End fish. Cooked, they help you dive: Starfin to swim, Void Eel to breathe, Glowgill for both.", "Catch a fish in astral water"),
  ("Asztrál horgász", "Az asztrál vízből csak End-hal jön. Sütve a merülést segítik: a Csillaguszony gyorsít, az Űrangolna levegőt ad, a Fénykopoltyú mindkettőben segít.", "Fogj halat asztrál vízben"))
Q("lurker", "isles", "deep_lurker_spawn_egg", ["fishing"], [adv("deep_lurker")], [give("cooked_glowgill", 16), give(M + "golden_apple", 8), give(M + "diamond", 8), give("deepglow_shard", 2)], 400, [],
  ("Something Below", "Deep Lurkers hunt anything swimming in the deep. Every bite takes your breath.", "Defeat a Deep Lurker"),
  ("Valami odalent", "A Mélylesők mindenre vadásznak, ami a mélyben úszik, és minden harapásukkal levegőt lopnak.", "Győzz le egy Mélylesőt"))
Q("deepglow", "isles", "deepglow_shard", ["lurker"], [item("deepglow_shard", 3)], [give("deepglow_shard", 4), give(M + "diamond", 16), give("prism_shard", 32)], 500, ["deep_aquamarine"],
  ("Light of the Deep", "Deepglow Ore shines only on the deep lake bed.", ""), ("A mélység fénye", "A Mélyfény-érc csak a tó mély fenekén világít.", ""))
Q("prism", "isles", "prism_shard", [], [biome("prism_wastes")], [give("prism_shard", 32), give("prism_lamp", 8), give(M + "diamond", 4)], 200, [],
  ("Prism Wastes", "Turquoise crystal spires on pale ground.", "Set foot in the Prism Wastes"),
  ("Kristályrengeteg", "Türkizkék kristálytornyok halvány talajon.", "Lépj be a Kristályrengetegbe"))
Q("prism_shards", "isles", "prism_cluster", ["prism"], [item("prism_shard", 16)], [give("prism_lamp", 16), give("prism_shard", 16)], 150, ["prism_lamp"],
  ("Crystal Harvest", "Most later recipes want Prism Shards.", ""), ("Kristályszüret", "A későbbi receptek többségéhez kell Prizmaszilánk.", ""))
Q("shardcrab", "isles", "shardcrab_spawn_egg", ["prism"], [adv("shardcrab")], [give("prism_shard", 32), give("prism_crystal_block", 8)], 250, [],
  ("Not a Crystal", "Some of those crystals have legs.", "Defeat a Shardcrab"), ("Mégsem kristály", "Némelyik kristálynak lába van.", "Győzz le egy Szilánkrákot"))
Q("scorched", "isles", "ember_rock", [], [biome("scorched_barrens")], [give("ember_rock", 16), give("ash_block", 16), give("ash_meal", 4), give(M + "diamond", 4)], 200, [],
  ("Scorched Barrens", "Hilly islands under ash that piles up like snow, and small volcanoes that never stop smoking.", "Set foot in the Scorched Barrens"),
  ("Perzselt puszta", "Dombos szigetek hamuval, ami hó módjára áll, és kis vulkánok, amelyek sosem alszanak el.", "Lépj be a Perzselt pusztába"))
Q("wraith", "isles", "ash_wraith_spawn_egg", ["scorched"], [adv("ash_wraith")], [give("ash_dust", 27), give("ash_meal", 8), give(M + "blaze_rod", 8)], 300, [],
  ("Ashes to Ashes", "Ash Wraiths spit embers and leave a blinding cloud when they die.", "Defeat an Ash Wraith"),
  ("Hamuból hamuba", "A Hamulidércek parazsat köpnek, és haláluk után vakító felhőt hagynak.", "Győzz le egy Hamulidércet"))
Q("ash_meal", "isles", "ash_meal", ["wraith"], [item("ash_meal", 1)], [give("ash_meal", 16), give(M + "bone_block", 16)], 200, ["ash_meal"],
  ("From Ashes", "Nine piles of Ash Dust make Ash Meal, far stronger than bone meal.", ""), ("Hamvaiból", "Kilenc hamuporból készül a Hamuliszt, ami sokkal erősebb a csontlisztnél.", ""))
Q("vent", "isles", "volcanic_vent", ["wraith"], [item("volcanic_vent", 1)], [give("volcanic_vent", 2), give(M + "blaze_rod", 16), give(M + "magma_block", 16)], 300, ["volcanic_vent"],
  ("Bottled Eruption", "A Volcanic Vent burns whatever stands near it, a fine trap. Redstone shuts it off.", ""), ("Palackozott kitörés", "A Vulkánblokk megéget mindent a közelében, jó csapda. Vöröskővel leállítható.", ""))

# ---- Ruins and Settlements
Q("graveyard", "isles", "bone_soil", [], [biome("dragon_graveyard")], [give("bone_soil", 32), give(M + "bone_block", 16), give(M + "diamond", 4)], 200, [],
  ("Dragon Graveyard", "Pale islands where old dragons came to die. Their ribcages still stand, and some skulls hide a chest.", "Set foot in a Dragon Graveyard"),
  ("Sárkánytemető", "Sápadt szigetek, ahová a régi sárkányok meghalni jártak. A bordáik még állnak, és némelyik koponyában láda rejtőzik.", "Lépj be egy Sárkánytemetőbe"))
Q("whelp", "isles", "spectral_whelp_spawn_egg", ["graveyard"], [adv("spectral_whelp")], [give(M + "phantom_membrane", 8), give(M + "golden_apple", 4), give(M + "diamond", 8)], 300, [],
  ("Laid to Rest", "Spectral Whelps circle above the bones, then swoop at anything that walks among them.", "Defeat a Spectral Whelp"),
  ("Örök nyugalom", "A Sárkányszellemek a csontok felett köröznek, aztán lecsapnak bárkire, aki közöttük jár.", "Győzz le egy Sárkányszellemet"))
Q("dragon_bones", "isles", "dragon_bone", ["whelp"], [item("dragon_bone", 9)], [give("dragon_bone_block", 4), give(M + "diamond", 8), give(M + "experience_bottle", 16)], 400, ["dragon_bone_block"],
  ("Old Bones", "Whelps and graveyard chests give Dragon Bone. Nine make a block, one makes a lot of bone meal.", ""),
  ("Régi csontok", "A Sárkányszellemek és a temető ládái Sárkánycsontot adnak. Kilencből tömb lesz, egyből rengeteg csontliszt.", ""))
Q("drake", "isles", "bone_drake_spawn_egg", ["dragon_bones"], [adv("bone_drake")], [give("dragon_bone", 8), give(M + "golden_apple", 4), give(M + "diamond", 8)], 500, [],
  ("Dragon Tamer", "A small Bone Drake sits in a nest of bone at the heart of every graveyard, and nothing can hurt it. Feed it Dragon Bones until it takes to you; then it follows you through the air like a dog and fights at your side. Right-click it with an empty hand to make it sit or stand, and Dragon Bones heal it.", "Tame a Bone Drake"),
  ("Sárkányszelídítő", "Minden temető közepén, egy csontfészekben ül egy kis Csontsárkány, amit semmi sem sebezhet meg. Etesd Sárkánycsonttal, amíg meg nem barátkozik veled; onnantól repülve követ, mint egy kutya, és melletted harcol. Üres kézzel jobb klikkelve leül vagy feláll, a Sárkánycsont pedig gyógyítja.", "Szelídíts meg egy Csontsárkányt"))

Q("village", "ruins", "end_villager_spawn_egg", [], [adv("end_village")], [give("chorusite_shard", 4), give("prism_shard", 16)], 200, [],
  ("Foreign Market", "End Villagers trade for Chorusite Shards, and do not care for emeralds.", "Find an End Village"),
  ("Idegen piac", "Az Endfalusiak Chorusit-szilánkért kereskednek, a smaragd nem érdekli őket.", "Találj rá egy Endfalura"))
Q("trade", "ruins", "chorusite_shard", ["village"], [adv("end_trade")], [give("chorusite_shard", 6), give(M + "golden_apple", 4)], 200, [],
  ("A Fair Deal", "Their prices are honest, if not cheap.", "Trade with an End Villager"), ("Tisztes üzlet", "Az áraik tisztességesek, ha nem is olcsók.", "Kereskedj egy Endfalusival"))
Q("bastion", "ruins", "voidstone_bricks", [], [adv("void_bastion")], [give("voidstone_bricks", 64), give("polished_voidstone", 64), give(M + "diamond", 4), give(M + "golden_apple", 4)], 250, [],
  ("Void Bastion", "Towers of voidstone full of guards and chests.", "Enter a Void Bastion"), ("Űrbástya", "Űrkőből épült tornyok, tele őrökkel és ládákkal.", "Lépj be egy Űrbástyába"))
Q("sentinel", "ruins", "void_sentinel_spawn_egg", ["bastion"], [kill("void_sentinel", 3)], [give(M + "diamond", 16), give(M + "golden_apple", 8), give(M + "ender_pearl", 16)], 500, [],
  ("Sentry Duty Over", "Void Sentinels hit hard and come in numbers.", ""), ("Őrségváltás", "Az Űrőrzők nagyot ütnek, és sokan vannak.", ""))
Q("template", "ruins", "chorusite_upgrade_smithing_template", ["sentinel"], [item("chorusite_upgrade_smithing_template")], [give("chorusite_upgrade_smithing_template", 1), give(M + "diamond", 16), give("chorusite_shard", 4)], 500, ["chorusite_upgrade_smithing_template_duplicate"],
  ("The Upgrade Template", "Bastion chests hold the template that turns netherite into Chorusite.", ""), ("A fejlesztősablon", "A bástyák ládáiban van a sablon, amellyel a netherit chorusittá alakítható.", ""))
Q("observatory", "ruins", "prism_lamp", [], [adv("observatory")], [give(M + "book", 16), give(M + "lapis_lazuli", 32), give(M + "amethyst_shard", 16), give("star_sapphire", 1)], 300, [],
  ("Stargazer", "Ruined towers with great telescopes, libraries full of gem materials.", "Find an Observatory"),
  ("Csillagnéző", "Romos, távcsöves tornyok, könyvtáraikban ékkő-alapanyagokkal.", "Találj rá egy Csillagvizsgálóra"))
Q("wreck", "ruins", "crumbling_planks", [], [adv("sky_wreck")], [give(M + "ender_pearl", 16), give(M + "shulker_shell", 4), give(M + "gold_block", 4), give("chorusite_shard", 2)], 350, [],
  ("Adrift", "Ships float in the void. Their decks are rotten, so keep moving. A Castaway may sell rare goods.", "Set foot on a Sky Wreck"),
  ("Sodródva", "A semmiben hajók lebegnek. A fedélzetük korhadt, ne állj meg rajta. A Hajótörött ritka árut kínálhat.", "Lépj egy Lebegő roncsra"))
Q("crypt", "ruins", "volcanic_vent", ["scorched"], [adv("crypt")], [give(M + "bone_block", 16), give("ash_meal", 8), give(M + "gold_block", 4), give(M + "diamond", 4)], 350, [],
  ("Under the Ash", "A spiral stair leads beneath every tomb on the Scorched Barrens.", "Enter a Crypt"),
  ("A hamu alatt", "A Perzselt puszta minden sírja alá csigalépcső vezet.", "Lépj be egy Kriptába"))
Q("crypt_guards", "ruins", "ash_dust", ["crypt"], [kill("ash_wraith", 8)], [give("ash_quartz", 2), give(M + "diamond", 16), give("ash_meal", 16)], 600, [],
  ("Tomb Sweeper", "The wraiths guarding the burial chambers will not leave on their own.", ""), ("Sírtisztító", "A sírkamrákat őrző lidércek maguktól nem mennek el.", ""))

# ---- Chorusite
Q("shards", "chorusite", "chorusite_shard", [], [item("chorusite_shard", 4)], [give(M + "diamond", 12), give("chorusite_shard", 2), give(M + "golden_apple", 8)], 500, [],
  ("Chorusite", "The ore hides inside the islands, and only a netherite pickaxe can break it.", ""), ("Chorusit", "Az érc a szigetek belsejében rejtőzik, és csak netherit csákánnyal üthető ki.", ""))
Q("pure", "chorusite", "pure_chorusite_shard", ["shards", "phasite"], [item("pure_chorusite_shard")], [give(M + "diamond", 12), give("chorusite_shard", 4)], 400, ["pure_chorusite_shard"],
  ("Refining", "Four shards, four diamonds and one piece of phasite.", ""), ("Finomítás", "Négy szilánk, négy gyémánt és egy fázisérc kell hozzá.", ""))
Q("ingot", "chorusite", "chorusite_ingot", ["pure"], [adv("chorusite_ingot")], [give("chorusite_shard", 6), give(M + "diamond", 16), give(M + "golden_apple", 8), give(M + "netherite_ingot", 1)], 800, ["chorusite_ingot_from_smelting"],
  ("The Chorusite Age", "Smelt the refined shard into an ingot.", "Make a Chorusite Ingot"), ("A chorusit kora", "Égesd ki a finomított szilánkot rúddá.", "Készíts Chorusitrudat"))
Q("pickaxe", "chorusite", "chorusite_pickaxe", ["ingot"], [adv("chorusite_pickaxe")], [give(M + "netherite_ingot", 1), give(M + "diamond", 16)], 600, ["chorusite_pickaxe_smithing"],
  ("Sharper than Netherite", "The template and an ingot turn a netherite pickaxe into a Chorusite one.", "Make a Chorusite Pickaxe"),
  ("Élesebb a netheritnél", "A sablon és egy rúd chorusit csákánnyá alakítja a netherit csákányt.", "Készíts chorusit csákányt"))
Q("armor", "chorusite", "chorusite_chestplate", ["ingot", "template"], [adv("chorusite_armor")], [give(M + "enchanted_golden_apple", 4), give(M + "netherite_ingot", 4), give(M + "diamond", 32)], 2000, ["chorusite_chestplate_smithing"],
  ("Clad in the End", "Stronger than netherite, and you will need it at the end of this road.", "Obtain a full Chorusite armor set"),
  ("Az End páncélja", "Erősebb a netheritnél, és az út végén szükséged lesz rá.", "Szerezz teljes chorusit páncélszettet"))

Q("chorusite_block", "chorusite", "chorusite_block", ["armor"], [adv("chorusite_block")], [give(M + "diamond_block", 16), give(M + "netherite_ingot", 4), give(M + "enchanted_golden_apple", 4)], 5000, ["chorusite_block"],
  ("Monument of the End", "Nine ingots pressed into one glowing block. Worth more than anything else you can build, and a beacon accepts it.", "Craft a Block of Chorusite"),
  ("Az End emlékműve", "Kilenc rúd egyetlen ragyogó tömbbe préselve. Ennél értékesebbet nem építhetsz, és a jelzőfény is elfogadja.", "Készíts Chorusit-tömböt"))

# ---- Phasite and Gems
Q("end_table", "jewels", "end_crafting_table", [], [item("end_crafting_table")], [give("polished_voidstone", 32), give("prism_shard", 16), give(M + "ender_pearl", 8)], 150, ["end_crafting_table"],
  ("Five by Five", "The End's greater items are crafted on a 5x5 grid. Its window lists every such recipe: click one to see where everything goes, and the table fills the grid from your inventory when you have it all.", ""),
  ("Ötször öt", "Az End nagyobb tárgyai 5x5-ös rácson készülnek. Az asztal ablaka felsorolja az összes ilyen receptet: kattints egyre, és megmutatja, mi hová kerül, ha pedig minden megvan nálad, be is rakja a rácsba.", ""))
Q("phasite", "jewels", "phasite_ore", [], [adv("phasite")], [give("phasite_shard", 2), give(M + "lapis_lazuli", 32), give(M + "diamond", 8)], 600, [],
  ("Out of Phase", "A rare ore on the undersides of the islands.", "Obtain Phasite"), ("Fázison kívül", "Ritka érc a szigetek alján.", "Szerezz fázisércet"))
Q("pearl", "jewels", "phase_pearl", ["phasite", "end_table"], [adv("phase_pearl")], [give("phasite_shard", 4), give(M + "ender_pearl", 16)], 600, ["phase_pearl"],
  ("Pocket Rift", "A pearl that remembers one place.", "Craft a Phase Pearl"), ("Zsebhasadék", "Egy gyöngy, amely egyetlen helyre emlékszik.", "Készíts Fázisgyöngyöt"))
Q("anchor", "jewels", "phase_anchor", ["pearl"], [adv("phase_anchor")], [give("phase_anchor", 2), give(M + "ender_pearl", 16)], 400, ["phase_anchor"],
  ("Anchored", "Right-click an anchor with the pearl to bind them, then jump to it from anywhere.", "Craft a Phase Anchor"),
  ("Lehorgonyozva", "A gyönggyel jobb klikkelj a horgonyra, és bárhonnan odaugorhatsz.", "Készíts Fázishorgonyt"))
Q("table", "jewels", "gem_table", ["phasite", "end_table"], [item("gem_table")], [give(M + "diamond", 8), give(M + "quartz", 32), give(M + "redstone", 32), give(M + "lapis_lazuli", 32)], 300, ["gem_table"],
  ("The Gem Table", "Tools, weapons and armor can each hold one gem.", ""), ("Az Ékkőasztal", "A szerszámokba, fegyverekbe és páncélokba egy-egy ékkő foglalható.", ""))
Q("gem", "jewels", "ember_garnet", ["table"], [adv("gem")], [give("chorusite_shard", 4), give(M + "diamond", 8), give(M + "redstone", 32), give(M + "quartz", 32)], 400, ["ember_garnet", "star_sapphire", "void_onyx"],
  ("Cut and Polished", "Each gem gives something else to each kind of item. Hover over one to see.", "Craft your first gem"),
  ("Csiszolt kincs", "Minden ékkő más-mást ad az egyes tárgytípusoknak. Húzd fölé az egeret.", "Készítsd el az első ékkövedet"))
Q("socket", "jewels", "gem_table", ["gem"], [adv("gem_socket")], [give(M + "experience_bottle", 32), give("void_onyx", 1)], 500, [],
  ("Set in Stone", "Put an item and a gem in the table. Taking a gem out destroys it.", "Set a gem into an item"),
  ("Kőbe foglalva", "Tegyél tárgyat és ékkövet az asztalba. Ha kiveszed az ékkövet, az megsemmisül.", "Foglalj ékkövet egy tárgyba"))
Q("all_gems", "jewels", "bloodstone", ["socket"], [adv("all_gems")], [give(M + "netherite_ingot", 4), give(M + "enchanted_golden_apple", 4), give("phasite_shard", 8), give(M + "diamond_block", 4)], 3000, ["moon_opal", "ash_quartz", "bloodstone"],
  ("Master Jeweler", "Own every kind of gem at least once.", "Obtain every kind of gem"), ("Mesterékszerész", "Legyen meg minden ékkőfajta legalább egyszer.", "Szerezd meg az összes ékkőfajtát"))

# ---- Other: small things outside the End's story, with smaller rewards; not part of the Whole Path
Q("canvas", "misc", "canvas", [], [item("canvas")], [give(M + "red_dye", 4), give(M + "yellow_dye", 4), give(M + "blue_dye", 4), give(M + "green_dye", 4)], 20, ["canvas"],
  ("Blank Canvas", "Nine white wool make a Canvas. Right-click its front with any dye to paint the pixel you look at; sneak with an empty hand to scrape one off. Broken, it keeps the picture.", ""),
  ("Üres vászon", "Kilenc fehér gyapjúból Vászon készül. Bármelyik festékkel jobb klikkelj az elejére, és kifested a pixelt, amire nézel; guggolva, üres kézzel visszakapard. Kiütve megmarad rajta a kép.", ""))
Q("large_canvas", "misc", "large_canvas", ["canvas"], [item("large_canvas")], [give(M + "white_wool", 9), give(M + "black_dye", 4), give(M + "white_dye", 4)], 20, ["large_canvas"],
  ("Bigger Picture", "Two canvases make one twice as wide. It needs a free spot to the right of where you place it.", ""),
  ("Nagyobb kép", "Két vászonból kétszer olyan széles lesz. Lerakáskor a jobb oldalán is kell egy szabad hely.", ""))
Q("backpack", "misc", "backpack", [], [item("backpack")], [give(M + "leather", 4), give(M + "string", 4)], 25, ["backpack"],
  ("More Room", "Five leather and a string make a Backpack. Put it in the slot above your off hand for nine more slots under your inventory.", ""),
  ("Több hely", "Öt bőrből és egy fonalból Táska készül. Tedd a bal kezed feletti helyre, és kilenc új tárgyhely nyílik a leltárad alatt.", ""))
Q("fermentation_station", "brew", "fermentation_station", [], [item("fermentation_station")], [give(M + "apple", 16), give(M + "glass_bottle", 4), give(M + "water_bucket", 1)], 25, ["fermentation_station"],
  ("Slow and Steady", "Copper, iron, glass and a barrel make a Fermentation Station. Fruit goes on top, glass bottles below, and pour water in with a bucket (up to ten); every bottle takes one bucket. Hoppers can fill it from above and the sides and empty it from below.", ""),
  ("Lassan járj", "Rézből, vasból, üvegből és egy hordóból Fermentáló állomás készül. Felülre gyümölcs, alá üvegpalack kerül, vizet pedig vödörrel önthetsz bele (legfeljebb tízet); minden üveghez egy vödör kell. Tölcsér felülről és oldalról töltheti, alulról ürítheti.", ""))
Q("apple_brandy", "brew", "apple_brandy", ["fermentation_station"], [item("apple_brandy")], [give(M + "apple", 32), give(M + "golden_apple", 2)], 40, [],
  ("Apple Brandy", "Six apples, a bottle and a bucket of water ferment into Apple Brandy in five minutes. It makes you strong, but the world sways just as long, and it makes you drunk. Another glass before that wears off makes it worse, up to level four.", ""),
  ("Almapálinka", "Hat almából, egy üvegből és egy vödör vízből öt perc alatt Almapálinka erjed. Erőt ad, de ugyanannyi ideig imbolyog tőle a világ, és berúgsz tőle. Ha mielőtt elmúlna iszol még egyet, rosszabb lesz, egészen négyes szintig.", ""))
Q("brandy_master", "brew", "starberry_brandy", ["apple_brandy"],
  [item("apple_brandy"), item("glow_berry_brandy"), item("sweet_berry_brandy"), item("moonbell_nectar_brandy"), item("starberry_brandy")],
  [give(M + "golden_apple", 4), give(M + "emerald", 16)], 100, [],
  ("Master Distiller", "Twelve glow berries make Glow Berry Brandy (night vision, haste), twelve sweet berries Sweet Berry Brandy (speed), four Moonbell Nectars a Nectar Brandy (resistance, fire resistance), eight Starberries a Starberry Brandy (jump boost, slow falling).", ""),
  ("Pálinkamester", "Tizenkét fényszederből Fényszederpálinka (éjjellátás, sietség), tizenkét édes bogyóból Édesbogyó-pálinka (gyorsaság), négy Harangnektárból Harangnektár-pálinka (ellenállás, tűzállóság), nyolc Csillagbogyóból Csillagbogyó-pálinka (ugrás, lassú esés) lesz.", ""))

Q("aging_barrel", "brew", "aging_barrel", ["apple_brandy"], [item("aging_barrel")], [give(M + "dark_oak_log", 16), give(M + "iron_ingot", 4)], 40, ["aging_barrel"],
  ("Patience", "Dark oak logs, iron and a barrel make an Aging Barrel. It holds one bottle of brandy in each of its three slots. After a Minecraft day (20 minutes) a bottle becomes Aged, after a day and a half more Fine Old: longer effects and higher levels. It only ages while its chunk is loaded; taking a bottle out starts its time over. Hoppers fill it from above and the sides and take only finished bottles from below.", ""),
  ("Türelem", "Sötét tölgy rönkökből, vasból és egy hordóból Érlelő hordó készül. Mindhárom helyére egy-egy üveg pálinka fér. Egy Minecraft-nap (20 perc) múlva Érlelt lesz belőle, további másfél nap után Nemes: tovább tartó és erősebb hatásokkal. Csak akkor érik, ha a chunkja be van töltve; ha kiveszed, az ideje elölről kezdődik. Tölcsér felülről és oldalról töltheti, alulról csak a kész üvegeket viszi el.", ""))
Q("fine_brandy", "brew", "apple_brandy", ["aging_barrel"], [adv("fine_brandy")], [give(M + "golden_apple", 6), give(M + "emerald", 24)], 150, [],
  ("Well Aged", "Age any brandy to the finest quality (three stars).", "Age a brandy to three stars"),
  ("Jól megérett", "Érlelj bármelyik pálinkát a legjobb minőségűre (három csillag).", "Érlelj egy pálinkát három csillagosra"))

Q("fruit_medley_brandy", "brew", "fruit_medley_brandy", ["apple_brandy"], [item("fruit_medley_brandy")], [give(M + "apple", 8), give(M + "sweet_berries", 12)], 40, [],
  ("Fruit Medley", "Four apples and six sweet berries: strength and speed. Plus a bottle and a bucket of water, as always.", ""),
  ("Gyümölcsös házi", "Négy alma és hat édes bogyó: erő és gyorsaság. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("honeyed_apple_brandy", "brew", "honeyed_apple_brandy", ["apple_brandy"], [item("honeyed_apple_brandy")], [give(M + "apple", 8), give(M + "honey_bottle", 4)], 40, [],
  ("Honey and Apples", "Four apples and two honey bottles: regeneration and a thick layer of absorption. Plus a bottle and a bucket of water, as always.", ""),
  ("Méz és alma", "Négy alma és két mézes üveg: regeneráció és vastag elnyelés. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("glowing_nectar_brandy", "brew", "glowing_nectar_brandy", ["apple_brandy"], [item("glowing_nectar_brandy")], [give(M + "glow_berries", 16), give("moonbell_nectar", 4)], 40, [],
  ("Glowing Nectar", "Eight glow berries and two Moonbell Nectars: night vision, resistance, and every monster nearby glows through the walls for half a minute. Plus a bottle and a bucket of water, as always.", ""),
  ("Fénylő nektár", "Nyolc fényszeder és két Harangnektár: éjjellátás, ellenállás, és fél percig minden közeli szörny átlátszik a falon. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("chorus_berry_brandy", "brew", "chorus_berry_brandy", ["apple_brandy"], [item("chorus_berry_brandy")], [give(M + "chorus_fruit", 8), give("starberry", 8)], 40, [],
  ("Chorus Berry", "Four chorus fruits and four Starberries: slow falling and high jumps for hopping between islands. Plus a bottle and a bucket of water, as always.", ""),
  ("Kórusbogyó", "Négy kórusgyümölcs és négy Csillagbogyó: lassú esés és magas ugrás a szigetek közti ugráláshoz. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("melon_refresher", "brew", "melon_refresher", ["apple_brandy"], [item("melon_refresher")], [give(M + "melon_slice", 16), give(M + "sweet_berries", 12)], 40, [],
  ("Melon Refresher", "Eight melon slices and six sweet berries: fire resistance, water breathing and four hearts at once. Plus a bottle and a bucket of water, as always.", ""),
  ("Dinnyés frissítő", "Nyolc dinnyeszelet és hat édes bogyó: tűzállóság, vízlégzés és azonnal négy szív. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("endless_brandy", "brew", "endless_brandy", ["brandy_master"], [item("endless_brandy")], [give("moonbell_nectar", 6), give("starberry", 8)], 80, [],
  ("Endless", "Three Moonbell Nectars, four Starberries and six glow berries: you float up for a few seconds, then drift down with high jumps and speed. Plus a bottle and a bucket of water, as always.", ""),
  ("Végtelen", "Három Harangnektár, négy Csillagbogyó és hat fényszeder: pár másodpercig felemelkedsz, utána lassan ereszkedsz, magasra ugrasz és gyorsabb vagy. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("miners_brandy", "brew", "miners_brandy", ["brandy_master"], [item("miners_brandy")], [give(M + "apple", 8), give(M + "glow_berries", 16)], 80, [],
  ("Miner's Friend", "Four apples, eight glow berries and a golden carrot: Haste II and night vision. Plus a bottle and a bucket of water, as always.", ""),
  ("A bányász barátja", "Négy alma, nyolc fényszeder és egy aranyrépa: Sietség II és éjjellátás. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("dragon_blood_brandy", "brew", "dragon_blood_brandy", ["brandy_master"], [item("dragon_blood_brandy")], [give("dragon_bone", 2), give(M + "apple", 8)], 80, [],
  ("Dragon's Blood", "A Dragon Bone, four apples and three Moonbell Nectars: Strength II, fire resistance and resistance. Plus a bottle and a bucket of water, as always.", ""),
  ("Sárkányvér", "Egy Sárkánycsont, négy alma és három Harangnektár: Erő II, tűzállóság és ellenállás. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("forest_elixir", "brew", "forest_elixir", ["brandy_master"], [item("forest_elixir")], [give(M + "sweet_berries", 12), give(M + "honey_bottle", 4)], 80, [],
  ("Forest Elixir", "Six sweet berries, two honey bottles and two cocoa beans: Speed II and a full stomach. Plus a bottle and a bucket of water, as always.", ""),
  ("Erdei elixír", "Hat édes bogyó, két mézes üveg és két kakaóbab: Gyorsaság II és tele has. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("void_essence", "brew", "void_essence", ["brandy_master"], [item("void_essence")], [give(M + "chorus_fruit", 8), give(M + "ender_eye", 2)], 80, [],
  ("Void Essence", "Four chorus fruits, an eye of ender and three Starberries: invisibility and slow falling. Plus a bottle and a bucket of water, as always.", ""),
  ("Üresség-esszencia", "Négy kórusgyümölcs, egy enderszem és három Csillagbogyó: láthatatlanság és lassú esés. Mellé, mint mindig, egy üveg és egy vödör víz.", ""))
Q("mix_master", "brew", "endless_brandy", ["endless_brandy", "miners_brandy", "dragon_blood_brandy", "forest_elixir", "void_essence"],
  [item("fruit_medley_brandy"), item("honeyed_apple_brandy"), item("glowing_nectar_brandy"), item("chorus_berry_brandy"), item("melon_refresher"),
   item("endless_brandy"), item("miners_brandy"), item("dragon_blood_brandy"), item("forest_elixir"), item("void_essence")],
  [give(M + "enchanted_golden_apple", 1), give(M + "emerald", 32), give(M + "diamond", 8)], 300, [],
  ("Master of Blends", "Brew every one of the ten mixed brandies.", ""),
  ("A keverékek mestere", "Főzd le mind a tíz kevert pálinkát.", ""))

Q("mailbox", "jewels", "phase_mailbox", ["anchor"], [item("phase_mailbox")], [give("phasite_shard", 8), give(M + "ender_eye", 16), give(M + "diamond_block", 4)], 3000, ["phase_mailbox"],
  ("Return to Sender", "A Phase Anchor at its heart sends items to any named Phase Mailbox on the server, even in unloaded lands. Name yours in its window; hoppers can empty it.", ""),
  ("Feladó: én", "A szívében lévő Fázishorgony bármelyik elnevezett Fázispostába elküldi a tárgyakat a szerveren, akár be nem töltött területre is. Az ablakában nevezheted el a sajátodat, és tölcsér is kiürítheti.", ""))

Q("gravity_stone", "jewels", "gravity_stone", ["deepglow", "anchor"], [item("gravity_stone")], [give("phasite_shard", 6), give(M + "diamond_block", 2), give(M + "redstone_block", 8)], 2500, ["gravity_stone"],
  ("Feather Fall", "Powered by redstone, it makes everything within six blocks fall slowly and jump high. Build lifts, jump courses and safe landings with it.", ""),
  ("Pehelykönnyű", "Vöröskővel bekapcsolva hat blokkon belül mindent lassan esővé és nagyot ugróvá tesz. Liftet, ugrópályát és biztonságos leszállóhelyet építhetsz vele.", ""))

# ---- The Rift
Q("sanctum", "rift", "rift_heart", ["armor"], [adv("rift_sanctum")], [give(M + "golden_apple", 32), give(M + "ender_pearl", 16), give(M + "diamond", 16)], 800, [],
  ("Stairs into the Dark", "A rare small shrine hides a stair down to an arena.", "Find a Rift Sanctum"), ("Lépcső a sötétbe", "Egy ritka kis szentély lépcsőt rejt, amely egy küzdőtérre vezet.", "Találj Hasadékszentélyt"))
Q("rift_lord", "rift", "rift_lord_spawn_egg", ["sanctum"], [adv("rift_lord")], [give(M + "netherite_ingot", 8), give(M + "enchanted_golden_apple", 8), give("phasite_shard", 8), give(M + "diamond_block", 8)], 5000, [],
  ("The Rift Closes", "It steps behind you and opens rifts underfoot. At 60% Void Sentinels shield it, at 25% it drags you in.", "Defeat the Rift Lord"),
  ("A hasadék bezárul", "Mögéd teleportál, és hasadékokat nyit a lábad alatt. 60%-nál Űrőrzők védik, 25%-nál magához ránt.", "Győzd le a Hasadékurat"))
Q("riftblade", "rift", "riftblade", ["rift_lord"], [adv("riftblade")], [give("phasite_shard", 8), give(M + "diamond_block", 8), give(M + "experience_bottle", 64)], 2000, ["riftblade"],
  ("Rift in Hand", "Forged from the Rift Heart, it carries you nine blocks along your line of sight.", "Forge the Riftblade"),
  ("Hasadék a kézben", "A Hasadékszívből kovácsolva 9 blokkot repít a nézésed irányába.", "Kovácsold meg a Hasadékpengét"))
Q("citadel", "rift", "voidstone_bricks", ["rift_lord"], [adv("void_citadel")], [give(M + "golden_apple", 32), give(M + "ender_pearl", 32), give(M + "diamond", 32), give("phasite_shard", 4)], 1500, [],
  ("Castle in the Void", "Far out in the End a castle stands alone on its own island, with smaller islands chained around it. Each of those holds guards and chests.", "Set foot on a Void Citadel's island"),
  ("Vár a semmiben", "Az End távoli részein egy vár áll magában a saját szigetén, köré láncolt kisebb szigetekkel. Azokon is őrök és ládák várnak.", "Lépj egy Fellegvár szigetére"))
Q("citadel_guards", "rift", "void_sentinel_spawn_egg", ["citadel"], [kill("void_sentinel", 25)], [give(M + "enchanted_golden_apple", 4), give(M + "netherite_ingot", 2), give(M + "diamond", 32)], 2000, [],
  ("Storming the Walls", "Sentinels hold the courtyard and halls, and Ash Wraiths shoot down from the walls.", ""),
  ("Falak ostroma", "Az udvart és a csarnokokat Űrőrzők tartják, a falakról Hamulidércek lőnek.", ""))
Q("void_king", "rift", "void_king_spawn_egg", ["citadel_guards"], [adv("void_king")], [give(M + "netherite_ingot", 12), give(M + "enchanted_golden_apple", 12), give("phasite_shard", 16), give(M + "diamond_block", 16)], 8000, [],
  ("Regicide", "Fights like the Rift Lord and hits harder. Jump over his shockwaves. His death opens the sealed treasury under the throne room.", "Defeat the Void King"),
  ("Trónfosztás", "Úgy harcol, mint a Hasadékúr, csak erősebb. Ugord át a lökéshullámait. Halála megnyitja a trónterem alatti lepecsételt kincstárat.", "Győzd le az Űrkirályt"))
Q("crownstone", "rift", "crownstone", ["void_king"], [adv("crownstone")], [give(M + "experience_bottle", 64), give("phasite_shard", 8), give(M + "diamond_block", 8)], 3000, [],
  ("Heavy Is the Crown", "Only the Void King carries Crownstones. Set in the Gem Table, one does the work of two ordinary gems.", "Obtain a Crownstone"),
  ("A korona súlya", "Koronakő csak az Űrkirálynál van. Az Ékkőasztalon befoglalva két közönséges ékkő munkáját végzi el.", "Szerezz Koronakövet"))

# ---------------------------------------------------------------- automatic layout: depth to the right, children stacked
by_id = {q["id"]: q for q in QUESTS}
for q in QUESTS:
    for p in q["requires"]:
        assert p in by_id, f"{q['id']} requires unknown {p}"
for chapter, *_ in CHAPTERS:
    members = [q for q in QUESTS if q["chapter"] == chapter]
    local = lambda q: [p for p in q["requires"] if by_id[p]["chapter"] == chapter]
    children = {q["id"]: [] for q in members}
    roots = []
    for q in members:
        parents = local(q)
        (children[parents[0]].append(q) if parents else roots.append(q))
    row = [0.0]

    def place(q, depth):
        q["x"] = float(depth)
        kids = children[q["id"]]
        if not kids:
            q["y"] = row[0]
            row[0] += 1.0
        else:
            for k in kids:
                place(k, depth + 1)
            q["y"] = (kids[0]["y"] + kids[-1]["y"]) / 2.0


    for q in roots:
        place(q, 0)

# ---------------------------------------------------------------- the Whole Path tab: one tree in five tiers; new quests must be added to a tier
OFF_PATH = ("misc", "brew")   # chapters that stay off the Whole Path tab
TIERS = [(("First Steps", "Első lépések"), "enter_end dragon gateway end_city elytra end_table forest astral_wood glimmerhoof glimmer_ride starberry nectar village trade prism prism_shards"),
         (("Exploration", "Felfedezés"), "basin fishing shardcrab scorched wraith ash_meal vent graveyard whelp dragon_bones drake bastion sentinel observatory wreck"),
         (("Treasure Hunt", "Kincsek nyomában"), "shards phasite pearl anchor table gem socket crypt crypt_guards lurker template"),
         (("Mastery", "Mesterfok"), "deepglow pure ingot pickaxe armor all_gems"),
         (("Endgame", "Végjáték"), "chorusite_block mailbox gravity_stone sanctum rift_lord riftblade citadel citadel_guards void_king crownstone")]
tier_of = {i: t for t, (_, ids) in enumerate(TIERS, 1) for i in ids.split()}
assert set(tier_of) == {i for i, q in by_id.items() if q["chapter"] not in OFF_PATH}, sorted(set(tier_of) ^ {i for i, q in by_id.items() if q["chapter"] not in OFF_PATH})
# only drawn on the Whole Path tab, so every quest joins the tree; these lock nothing
AFTER = {"forest": "gateway", "prism": "gateway", "graveyard": "gateway", "village": "gateway", "basin": "gateway", "scorched": "gateway", "bastion": "gateway", "observatory": "gateway", "wreck": "gateway",
         "shards": "bastion", "phasite": "observatory", "end_table": "gateway"}
for q in QUESTS:
    if q["chapter"] in OFF_PATH:
        q.update(tier=0, parents=[], path_x=0.0, path_y=0.0)
        continue
    q["tier"] = tier_of[q["id"]]
    parents = q["requires"] + ([AFTER[q["id"]]] if q["id"] in AFTER else [])
    assert all(tier_of[p] <= q["tier"] for p in parents), q["id"]
    q["parents"] = parents
assert [q["id"] for q in QUESTS if not q["parents"] and q["tier"]] == ["enter_end"]


def tier_depth(q):
    inside = [by_id[p] for p in q["parents"] if tier_of[p] == q["tier"]]
    return 1 + max(map(tier_depth, inside)) if inside else 0


columns, start = [], 0
for t in range(1, len(TIERS) + 1):
    columns.append(start)
    start += max(max(tier_depth(q) for q in QUESTS if q["tier"] == t) + 1, 5)   # wide enough for the tier's name


# every tier is its own compact block centred on row 0, so the whole path stays low enough to read while scrolling sideways
def place_tier(t):
    members = [q for q in QUESTS if q["tier"] == t]
    kids = {q["id"]: [] for q in members}
    roots = []
    for q in members:
        inside = [p for p in q["parents"] if tier_of[p] == t]
        (kids[inside[0]].append(q) if inside else roots.append(q))
    roots.sort(key=lambda q: by_id[q["parents"][0]]["path_y"] if q["parents"] else 0.0)  # in the order of the quests leading into them
    row = [0.0]

    def place(q):
        q["path_x"] = float(columns[t - 1] + tier_depth(q))
        if not kids[q["id"]]:
            q["path_y"] = row[0]
            row[0] += 1.0
        else:
            for k in kids[q["id"]]:
                place(k)
            q["path_y"] = (kids[q["id"]][0]["path_y"] + kids[q["id"]][-1]["path_y"]) / 2.0

    for q in roots:
        place(q)
    for q in members:
        q["path_y"] -= (row[0] - 1.0) / 2.0


for t in range(1, len(TIERS) + 1):
    place_tier(t)
assert all("path_x" in q for q in QUESTS)


def exported(q):
    o = {k: q[k] for k in ("id", "chapter", "icon", "x", "y", "requires", "objectives", "rewards", "xp", "recipes", "path_x", "path_y", "tier")}
    if q["id"] in AFTER:
        o["after"] = AFTER[q["id"]]
    return o


w(f"{D}/quests.json", {"chapters": [{"id": c[0], "icon": c[1]} for c in CHAPTERS], "tiers": [{"id": t + 1, "x": columns[t]} for t in range(len(TIERS))], "quests": [exported(q) for q in QUESTS]})

KEY = "quest.endrealms."
COMMON = {"en_us": {"title": "Endwalker's Guide", "progress": "%s / %s quests", "claim": "Claim reward", "claimed": "Claimed", "goals": "Goals", "rewards": "Rewards", "requires": "Finish first:", "xp": "+%s XP",
                    "kill": "Defeat: %s", "mine": "Mine: %s", "obtain": "Obtain: %s", "status.locked": "Locked", "status.open": "In progress", "status.ready": "Reward ready!", "status.claimed": "Done"},
          "hu_hu": {"title": "Endjáró kalauz", "progress": "%s / %s küldetés", "claim": "Jutalom átvétele", "claimed": "Átvéve", "goals": "Célok", "rewards": "Jutalmak", "requires": "Előbb teljesítsd:", "xp": "+%s XP",
                    "kill": "Győzd le: %s", "mine": "Bányászd: %s", "obtain": "Szerezz: %s", "status.locked": "Zárolva", "status.open": "Folyamatban", "status.ready": "Jutalom átvehető!", "status.claimed": "Teljesítve"}}
for lang, slot, chapter_slot in (("en_us", "en", 2), ("hu_hu", "hu", 3)):
    p = f"{A}/lang/{lang}.json"
    d = {k: v for k, v in r(p).items() if not k.startswith("guide.endrealms.") and not k.startswith(KEY)}
    d.update({KEY + k: v for k, v in COMMON[lang].items()})
    for c in CHAPTERS:
        d[f"{KEY}chapter.{c[0]}"] = c[chapter_slot]
    for t, (names, _) in enumerate(TIERS, 1):
        d[f"{KEY}tier.{t}"] = f"Tier {t}: {names[0]}" if lang == "en_us" else f"{t}. szint: {names[1]}"
    for q in QUESTS:
        title, desc, goal = q[slot]
        d[f"{KEY}{q['id']}.title"] = title
        d[f"{KEY}{q['id']}.desc"] = desc
        if goal:
            d[f"{KEY}{q['id']}.goal"] = goal
    w(p, d)
print("ok", len(QUESTS), "quests")
