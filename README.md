# End Realms – Fabric mod (Minecraft 26.3)

## Mappák
- `endrealms/` – a mod forráskódja (Fabric, Gradle projekt)
- `endrealms/build/libs/endrealms-<verzió>.jar` – a kész mod (ezt kell a `mods` mappába tenni)
- `tools/jdk-25.0.5+8/` – hordozható Java 25 (Temurin), csak a buildhez kell, nincs telepítve a rendszerbe
- `tools/texture_scripts/draw_chorusite.py` – a textúrákat rajzoló script (`preview.png` = előnézet)

## Verziók
- Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.2+26.3, Java 25

## Build (Git Bash)
```
cd "D:/minecraft mod/endrealms"
export JAVA_HOME="D:/minecraft mod/tools/jdk-25.0.5+8"
./gradlew build          # -> build/libs/endrealms-<verzió>.jar
./gradlew runClient      # elindítja a játékot fejlesztői módban a moddal
./gradlew runServer      # dedikált szerver (run/eula.txt -> eula=true kell)
```

## Telepítés a rendes játékba / szerverre
1. Fabric Loader 0.20.1 a 26.3-hoz (https://fabricmc.net/use/)
2. A `mods` mappába: Fabric API (0.165.2+26.3) + `endrealms-<verzió>.jar`
3. A modnak kliensen ÉS szerveren is fent kell lennie (új blokk/item van benne).

## Tartalom (0.41.0)
- **Ékkövek 5×5-ben**: a 7 barkácsolható ékkő (Koronakő kivételével) mostantól az End-barkácsasztalon, 5×5-ös gyémánt mintában készül, kb. kétszeres alapanyagból
- **Ájulás**: Részegség IV elérésekor 4 másodpercre teljesen elsötétül a kép, és nem tudsz mozogni; utána 100 blokkon belül máshol térsz magadhoz, összekevert táskával, és minden hatás eltűnik rólad
- Javítva: más postaládájának névmezőjébe kattintva beragadt az ablak (se kattintás, se Esc nem működött)
- Az érlelő hordó bármilyen fa rönkjéből elkészíthető (End-fákból is), nem csak sötéttölgyből
- **Automatikus frissítés**: ha a szerveren újabb End Realms fut, a játék belépéskor magától letölti GitHubról; értesítés jön, hogy kész, utána csak újra kell indítani a játékot (a 0.40.0-t még egyszer kézzel kell feltenni)
- Javítva: a fermentáló állomásból az utolsó hozzávalót kivéve összeomlott a játék
- **10 kevert pálinka** 2–3 alapanyagból (Gyümölcsös házi, Mézes alma, Fénylő nektár, Kórusbogyó, Dinnyés frissítő, Végtelen pálinka, Bányászpálinka, Sárkányvér, Erdei elixír, Üresség-esszencia), mind érlelhető; a receptpanel görgethető
- **Endjáró kalauz**: új Pálinkafőzés fejezet 16 küldetéssel (köztük „A keverékek mestere”)
- **Verzióellenőrzés**: régi vagy hiányzó End Realms-szel belépve „Hálózati protokollhiba” helyett érthető magyar üzenet (szerver- és saját verzió, mit kell tenni)
- **Érlelő hordó**: 3 hely, mindegyikbe 1 pálinka; 1 Minecraft-nap (20 perc) után Érlelt (★★), további másfél nap után Nemes (★★★) pálinka – hosszabb és erősebb hatások; tölcsér alulról csak a kész üveget viszi el
- **Javítások**: a Csontsárkány utánad teleportál (repülés közben és tpa/dimenzióváltás után is); a „szerezd meg” küldetések a lerakott/elkészített tárgyat is elfogadják
- **Fermentáló állomás**: nagyobb ablak, 3 alapanyag-hely, Indítás/Leállítás gomb, receptpanel; a Részegség látásvesztése javítva
- **Pálinkafőzés**: víz vödörrel (max. 10), ötféle pálinka (alma, fényszeder, édes bogyó, harangnektár, csillagbogyó) saját hatásokkal; Részegség I–IV
- **Fermentáló állomás**: 6 almából és 1 üvegből 5 perc alatt Almapálinka (erő, regeneráció, kis szédülés); tölcsérrel is tölthető/üríthető
- **Szerverpanel**: a Most játszanak táblázat mutatja, ki melyik világban van
- **Csillagnád**: a Csillagbogyó End-földbe ültethető és kinő; ollóval maga a nád szedhető le (duplikálás javítva)
- **Világváltáskor nem kell újra bejelentkezni; a játékos a saját spawnpontjára érkezik**
- **Világok**: új világ létrehozása a szerverpanelről (saját Netherrel és Enddel), játékos átküldése
- **Holdharang**: a nektár End-földbe ültethető és kinő; ollóval az egész növény leszedhető (duplikálás javítva)
- **Csontsárkány** – minden Sárkánytemető közepén egy csontfészekben ül egy kis sárkány; amíg vad, nem mozdul és nem sebezhető. Sárkánycsonttal etetve (darabonként 1/3 eséllyel) megszelídül, onnantól repülve követ, mint egy kutya (messziről utánad teleportál), és harcol melletted. Üres kézzel jobb klikk: ül/áll; Sárkánycsont: gyógyítja. Előrelépés (*Sárkányszelídítő*) és küldetés a Szigetek fejezetben.
- **End-barkácsasztal** (3x3: 2 pattogtatott chorus + ender gyöngy + barkácsasztal + 5 csiszolt űrkő) – 5x5-ös barkácsrács (`endrealms:end_crafting` recepttípus, akár 5x5-ös mintával). Az ablak bal oldalán az összes 5x5-ös recept listája: kattintásra halványan megmutatja a mintát a rácsban, és ha minden megvan a leltárban, be is rakja. Ide költözött a **Fázisgyöngy, Fázishorgony, Fázisposta, Gravitációs kő, Hasadékpenge, Ékkőasztal és Vulkánblokk** receptje (a küldetéskönyv az 5x5-ös mintát mutatja).
- **Szerverpanel naptár**: a szerver a `history.json`-ba jegyzi, mely napokon futott és ki lépett be; a panel Naptár füle ebből és a régi naplókból színezi a napokat (zöld: játszottak, sárga: futott, piros: nem futott).
- **Vászon** (9 fehér gyapjú) és **Nagy vászon** (2 vászon, 2 blokk széles) – bármelyik festékkel pixelenként festhető (16 szín, egy képen bármennyi); guggolva üres kézzel visszakaparható; kiütve megmarad a kép. A receptek a vanília Egyéb fülön vannak, a küldetések az Egyéb fejezet elején.
- **Gravitációs kő** – vöröskővel bekapcsolva 6 blokkos körzetben lassú esést és nagy ugrást ad. Küldetés és előrelépés a Végjátékban.
- **Fázisposta** – drága blokk (End-barkácsasztalon: Fázishorgony + 2 fázisszilánk + 2 prizmaszilánk + gyémánt + 2 Ender szem + láda + 9 csiszolt űrkő). Lerakáskor a lerakó nevére kerül, az ablakában (csak a tulajdonos) átnevezhető; a listából névre küldhetsz bármelyik postába a szerveren, akár be nem töltött területre is. A beérkezett tárgyakat bárki kiveheti a postánál, tölcsér is kiszedheti, de betenni csak küldéssel lehet. Küldetés és előrelépés a Végjátékban.
- **Nyergelhető Csillámpata** – felnőtt Csillámpatára nyereg tehető, és meglovagolható (nagyjából egy átlagos ló sebességével). Az ugrást nyomva tartva és elengedve kb. 9 blokk magasra ugrik, a 12 blokknál kisebb esés nem sebzi.
- **Chorusit-tömb** – 9 chorusitrúdból, visszabontható. Lüktető, sötétben is világító textúra, fényt ad, jelzőfény alapjának is jó (a rúd pedig fizetségnek). Saját előrelépés (*Az End emlékműve*) és küldetés a Chorusit fejezet végén.
- A **faragott tökből** is lehet hó-, vas- és rézgólemet építeni.
- **Táska** (5 bőr + 1 fonal, az Endtől függetlenül) – a leltárban a bal kéz feletti helyre téve 9 extra tárgyhelyet nyit a leltár alatt; ami nem fér a leltárba, oda kerül.
- **Sárkánytemető** (`endrealms:dragon_graveyard`) – új End-biom sápadt csonttalajjal, óriási bordákkal, koponyákkal (bennük kincsesláda) és csontfákkal. Felette **Sárkányszellemek** köröznek; tőlük és a ládákból **Sárkánycsont** szerezhető.
- **Koronakő** – csak az Űrkirálytól szerezhető ékkő. A kalauz első füle, a **Teljes út** az összes küldetést 5 szintre osztva egyetlen fában mutatja.
- **Fellegvár** (`endrealms:void_citadel`, `/astra locate void_citadel`) – ritka vár a saját nagy szigetén az End távoli részein (legalább 1500 blokkra a középponttól), körülötte 6 kisebb, lánccal hozzákötött szigettel. Sok őr és láda; a trónteremben az **Űrkirály** (600 életerő, a Hasadékúr képességei + átugrandó lökéshullámok) vár, halála megnyitja a terem alatti kincstárat.
- **Teleportkérés** (operátor nélkül): `/tpa <játékos>` kérés küldése, `/tpaccept` elfogadás, `/tpdeny` elutasítás (a chatben kattintható gombokkal), `/tpcancel` visszavonás. A kérés 1 perc múlva lejár, két kérés között 10 másodperc a várakozás, dimenziók között is működik.
- **Chorusit-érc** (`endrealms:chorusite_ore`) – End kőben generálódik az összes End biomban,
  y 0–80 között, chunkonként 10 próbálkozás, max 5 blokkos erekben.
  **Netherit (vagy chorusit) csákány** kell hozzá, gyengébbel kiütve semmit nem dob; 3–7 XP-t ad, Fortune és Silk Touch működik.
- **Chorusit-szilánk** (`endrealms:chorusite_shard`) – az érc dropja (mindig 1 darab, a Szerencse nem növeli), mint a netherit törmeléke.
- **Nyers tiszta chorusit-szilánk** – 4 chorusit-szilánk + 4 gyémánt + 1 fázisérc vagy fázisszilánk összekraftolva.
- **Chorusitrúd** – a nyers tiszta szilánkot kemencében / kohóban kell rúddá égetni (a sima szilánk nem égethető ki).
- **Chorusit-fejlesztés** kovácssablon – 7 gyémánt + 1 chorusitrúd + 1 End kő (rúd felül középen, End kő a közepén);
  másolás: 7 gyémánt + sablon + End kő → 2 sablon.
- **Chorusit eszközök** (kard, csákány, balta, ásó, kapa) – kovácsasztal: sablon + **netherit** eszköz + chorusitrúd.
- **Chorusit páncélzat** – kovácsasztal: sablon + **netherit** páncéldarab + chorusitrúd; páncél 4/9/7/4, szívósság 4, hátralökés-ellenállás 15%, tűzálló.
  Jobbak a netheritnél: 2600 tartósság (netherit 2031), 12 bányászási sebesség (9), +1 sebzés, 18 varázsolhatóság (15), tűzálló.
- **`/astra locate <név>`** – struktúrák és biomok keresése: `void_bastion`, `end_village`, `astral_forest`, `astral_basin`, `prism_wastes`
  (Tab kiegészít; OP/csalás jog kell, az Endben futtasd).
- **Asztrál erdő** biom (`endrealms:astral_forest`) – a nagy külső End-szigeteken (highlands + midlands).
  Keresés: `/execute in minecraft:the_end run locate biome endrealms:astral_forest` (az Endben elég a `/locate biome endrealms:astral_forest`).
  Magas (12–17 blokk), vékony fák sűrű, lila lombkoronával.
- **Asztrál fa család**: hántolt rönk (baltával), asztrálfa blokk (4 rönk → 3), hántolt asztrálfa blokk,
  lap, lépcső, kerítés, kerítéskapu, ajtó, csapóajtó – a vanilla receptekkel (a deszka bármelyik rönk/blokk változatból).
  A lomb kiütéskor / lehulláskor ~5% eséllyel csemetét, néha botot dob.
- **Asztrál rönk, deszka (4 db/rönk), lomb, csemete** – a csemete End kőre (vagy földre) ültethető,
  de **csak az Endben nő** (ott fény nélkül is, csontliszttel is); máshol nem nő és csontliszt sem fogy.
- **Űrkő** (Voidstone) – lilás End kő. A szigetek felső 3 rétege mindig sima End kő, alatta nagy zónákban
  űrkő (a többi helyen End kő marad). Ugyanúgy bányászható, mint az End kő.
  - **Csiszolt űrkő**: 4 űrkő (2×2) → 4 db; **Űrkőtégla**: 4 csiszolt űrkő (2×2) → 4 db; kővágóban 1:1 mindkettő.
  - Mindhárom változatnak van **lapja, lépcsője és fala** (barkácsolás a vanilla mintákkal, vagy kővágóval űrkőből).
  - **Űrköves chorusit-érc**: a chorusit-érc űrkőben (mint a mélypalás ércek), ugyanazt dobja, kicsit keményebb.
- **Űrhomok** (Void Sand) – End-kő színű, helyenként lilás-rózsaszín szemcsés homok (esik, mint a homok,
  üveggé olvasztható). Variánsok a vanilla homokkő mintájára: **űrhomokkő** (+lap, lépcső, fal),
  **sima űrhomokkő** (+lap, lépcső; kiolvasztással), **vágott űrhomokkő** (+lap), **vésett űrhomokkő** (ender-szem mintával).
- **End-föld** – az Asztrál erdő talaja (felső 3 réteg), máshol az Endben kisebb foltokban (kb. minden 3. chunkban).
  Ásóval bányászható, föld-tagben van (csemeték, virágok ültethetők rá).
- **Űrvirág** (Voidbloom) – furcsa, halványan világító (fényszint 6) End-növény: End-sárga szár, lila indák,
  rózsaszín gömbök. Az Asztrál erdőben és az End-föld foltokon nő (csak End-földön/földön él meg).
  Kézzel/szerszámmal kiütve semmit nem dob, **ollóval** megkapod.
- **Csillámpata** (Glimmerhoof) – hosszú lábú, csápos End-állat: halvány End-kő színű bőr lila foltokkal,
  világító szemek, csápgömbök, hátüskék és paták. Csak az Asztrál erdőben spawnol, End-földön (fény nem kell).
  **Chorus gyümölccsel** csalogatható és szaporítható. Idéző tojás a kreatív fülön.
  - **Nyers csillámpata-hús** (1–3 db, Looting növeli; égve megölve sülten esik) – annyit tölt, mint a nyers marhahús.
  - **Sült csillámpata-hús** – kemence / füstölő / tábortűz; annyit tölt, mint a steak.
- **Asztrál medence** biom (`endrealms:astral_basin`) – nagy, saját generálású sziget a külső Enden
  (kb. 130–160 blokk átmérő): körben változó magasságú hegygyűrű (csúcsok ~y 95–125), közepén színültig
  töltött, akár 38 blokk mély asztrál vizes krátertó (víztükör: y=88). A tó medre és a belső part űrhomok.
  Keresés az Endben: `/locate biome endrealms:astral_basin`.
  - Vízinövények (csak asztrál vízben élnek, bennük is asztrál víz van): **Csillagmoszat** – magas (5–23 blokk),
    terebélyes, hínárszerű oszlop (egymásra rakható, kivágva a felette lévő rész is leesik) – és világító **Űrkorall**.
  - Csak még be nem járt End-területen generálódik: a már legenerált chunkok nem változnak, ezért egy régi
    világ bejárt részén a sziget félbevágva jelenhet meg.
  - Halak (csak az asztrál vizes tavakban spawnolnak): **Csillaguszony** (~tőkehal), **Űrangolna** (~lazac),
    **Fénykopoltyú** (nyersen 3, sülve 7 éhségpont). Mindegyiknek nyers és sült húsa van
    (kemence / füstölő / tábortűz), és idéző tojása a kreatív fülön. Vödörrel (még) nem foghatók.
- **Űrbástya** (Void Bastion) struktúra – magas űrkő torony a külső End-szigeteken (az End Cityhez hasonló
  gyakorisággal, csak elég nagy és magas szigeten). Űrkőtéglából, csiszolt űrkőből és ezek lépcsőiből/lapjaiból.
  Változatok: 5–7 sugár, 3–6 szint, négyzetes vagy nyolcszögű alaprajz, hegyes torony vagy bástyafokos tetőterasz.
  Belül csigalépcső a tetőig, lila üvegablakok, szintenként berendezés (könyvtár, asztalok, szőnyeg, End rúd lámpák)
  és ládák; a legfelső szinten kincsesláda.
  - Loot: vas–gyémánt felszerelés és nyersanyag (varázsolt vas/gyémánt eszközök és páncélok), arany, smaragd,
    ender gyöngy, chorus gyümölcs, sült End-húsok; **Chorusit-fejlesztés sablon** (sima láda 25%, kincsesláda 67%).
    Chorusit nincs benne.
  - **Űrőrző** (Void Sentinel) őrzi: 1,3× játékosnyi, űrkő-páncélos lovag szarvas sisakkal, világító szemmel,
    chorusit karddal (nem dobja el). 60 életpont, 10 páncél, erős ütés, alig lökhető hátra; megtámadja a játékost,
    és ha megütöd, a közeli őrök is rád támadnak. Generáláskor szintenként 1 (a kincsesnél 2) áll őrt, utána a
    bástyában újraspawnolnak (fénytől függetlenül). Dob: ender gyöngy, chorus gyümölcs, űrkőtégla,
    ritkán (15%, ha játékos öli meg) chorusit-szilánk; 25 XP.
  - Keresés: **`/astra locate void_bastion`** (vagy `/locate structure endrealms:void_bastion`) – csalás/OP jog kell
    hozzá, mint a vanilla `/locate`-hez, és az Endben kell futtatni.
- **Asztrál víz** + **Asztrálvizes vödör** – lila víz, pontosan úgy viselkedik, mint a víz (úszás, végtelen forrás,
  lávával obszidián/kő). Egyelőre nem generálódik, csak a kreatív menüből vehető elő.
  Megjegyzés: a mod felülírja a `minecraft:end` felszíni szabályt (material_rule) – más, ugyanezt felülíró moddal ütközhet.
- Saját kreatív fül: „End Realms”.

- **Kristályrengeteg** (Prism Wastes, `/astra locate prism_wastes`): sápadt End-biom türkiz kristálytűkkel
  - **Kristályos űrkő** a talaj; **prizmakristály blokk**, **bimbózó prizmakristály** (az ametisztéhez hasonlóan növeszt
    kis/közepes/nagy **prizmabimbót** és **prizmacsokrot**), **kristálylámpa** (4 szilánk + end rúd, 15-ös fény)
  - A csokor **prizmaszilánkot** dob; 4 szilánk = kristályblokk; űrkő + szilánk = kristályos űrkő
  - A kristályok halkan csilingelnek, ha elsétálsz mellettük; türkiz köd és lebegő fényrészecskék
  - **Szilánkrák** (Shardcrab): kristálypáncélos rák, álló helyzetben kristálynak néz ki, 5 blokkon belül ugrik elő
    (30 élet, 12 páncél, 7 sebzés), 1-3 prizmaszilánkot dob

- **Endfalu** (End Village, `/astra locate end_village`): generált falu az End külső szigetein
  - Burkolt tér asztrál vizes szökőkúttal, körülötte 4-18 ház (end kő, end kőtégla, asztrál fa), háromféle alaprajz, utak, szükség esetén deszkahíd
  - Nincs farm és sima víz; a házakban munkaasztal, lámpa, szőnyeg, néha láda
  - **Endfalusi** (End Villager): szarvas, világító szemű falusi 8 foglalkozással: chorusit-kovács, űrkő-kőműves, csillámpata-mészáros, medencei halász, chorus-pap, csillaglevéltáros, asztrál erdész, prizmaékszerész
  - A pénz a **chorusit-szilánk**; a készlet 10 percenként feltöltődik
  - A faluk nem generálódnak bástyák közvetlen közelében

- **Fázisérc** (`endrealms:phasite_ore`): az End szigetek alján, villódzó világító erekkel; gyémánt csákány kell hozzá, **fázisszilánkot** dob; a nyers tiszta chorusit-szilánk receptjéhez kell
- **End Realms achievement fül**: *A chorusit kora* (első chorusitrúd), *Birodalomjáró* (minden End Realms biom; új biomnál a `tools/gen_phasite_and_advancements.py` `BIOMES` listáját kell bővíteni)

- **Csillagnád** (Asztrál erdő): 3-5 blokk magas, magától növő nád end földön; **Csillagbogyó**: gyorsan ehető, 15 mp lassú esés + 8 mp gyorsaság
- **Holdharang** (Asztrál erdő): 3 blokk magas, világító, csilingelő virág; **Harangnektár**: leveszi a káros hatásokat, a hasznosakat meghagyja, 20 mp éjjellátás

- **Horgászat asztrál vízben**: kizárólag a három endes hal fogható ki (csillaguszony, űrangolna, ritkábban fénykopoltyú)

- **Receptkönyv**: belépéskor minden End Realms recept bekerül a játékos receptkönyvébe

- **Fázisgyöngy**: Fázishorgonyra jobb klikkelve hozzákötöd, használva a horgony tetejére ugrik (dimenziók között is), 8 töltés, 30 mp várakozás, üllőn fázisszilánkkal tölthető; recept (End-barkácsasztal, 5x5): 4 fázisérc/szilánk + 4 prizmaszilánk + 8 pattogtatott chorus + 4 ender gyöngy + 1 ender szem
- **Fázishorgony**: a Fázisgyöngy célblokkja; recept (End-barkácsasztal, 5x5): 16 csiszolt űrkő + 4 prizmaszilánk + 4 obszidián + 1 fázisérc/szilánk
- **Fázisékkő**: páncéldísz-anyag halványkék mintával; recept: 1 fázisérc/szilánk + 2 prizmaszilánk + 1 pattogtatott chorus

- **Hasadékszentély**: ritka építmény, felszíni szentéllyel és föld alatti küzdőtérrel; `/astra locate rift_sanctum`
- **Hasadékúr**: háromfázisú boss, a Hasadékszívből és egy chorusit kardból Hasadékpenge készül (+ 2 Fázisékkő)
- **Endjáró kalauz**: belépéskor egyszer megkapod, könyv + endkő a recept; saját, állomásokra bontott útvonal-képernyőt nyit
- **Perzselt puszta**: saját, dombos-völgyes szigeteken álló hamus End-biom kis vulkánokkal, Parázskővel, hóhoz hasonló hamuréteggel és Hamulidércekkel; `/astra locate scorched_barrens`
- **Ékkövek**: Parázsgránát, Csillagzafír, Űrónix, Holdopál, Hamukvarc, Vérkő; az Ékkőasztalon tárgyanként egy foglalható be, kivéve megsemmisül, minden tárgytípusnak mást adnak
- **Lebegő roncs**: négyféle hajó a semmiben, korhadó fedélzettel és néha Hajótörött kereskedővel; `/astra locate sky_wreck`
- **Csillagvizsgáló**: változatos romos tornyok díszítő távcsővel és könyvtárral; `/astra locate observatory`
- **Asztrál medence mélye**: Mélyfény-érc a tó fenekén, Mélyleső ragadozó, Mélyfény-akvamarin ékkő, búvárkodást segítő sült halak
- **Kripta**: négyféle felszíni épület, alatta többféle szoba; `/astra locate crypt`
- **Vulkánblokk**: éget és időnként kitör, vöröskővel leállítható; **Hamuliszt**: 9 hamupor, erősebb csontliszt
- **Küldetésfa**: az Endjáró kalauz fejezetekre bontott, jutalmat adó küldetéseket mutat (`tools/gen_quests.py`)
- **Tökfaragás**: ollóval pixelenként, guggolva visszatehető

## Fejlesztői scriptek
- `tools/gen_drake_and_table.py` – a Csontsárkány és az End-barkácsasztal saját rajzú (CC0) textúrái, adatai és az 5x5-ös receptek.
- `tools/gen_block_families.py` – a lap/lépcső/fal/kerítés/ajtó stb. JSON-jait a vanilla cseresznye / End kőtégla fájlokból
  generálja átnevezéssel, és a baltás hántolást (`data/minecraft/block_transformer/axe.json`) is kiegészíti.
  Ez a fájl is egy vanilla fájl felülírása, mint a `material_rule/end.json`.
- A medence-szigetek helyét a `worldgen/BasinSites.java` határozza meg; a biomot egy mixin
  (`mixin/TheEndBiomeSourceMixin.java`) teszi pontosan a szigetre.

## Licenc
A kód CC0-1.0. Az érc, a szilánk, az asztrál fa, az End-föld, az űrkő, az űrvirág és a csillámpata, a halak és a vízinövények textúrái saját rajzok (CC0, `draw_chorusite.py`, `draw_astral.py`, `draw_end_soil.py`, `draw_voidstone.py`, `draw_voidbloom.py`, `draw_glimmerhoof.py`, `draw_basin.py`, `draw_void_sand.py`, `draw_void_sentinel.py`).
Az eszközök, a rúd, a sablon és az asztrálvizes vödör textúrája a vanilla textúrák átszínezése (`tools/texture_scripts/recolor_vanilla.py`),
ezek Mojang-asset származékok: saját használatra rendben, nyilvános kiadás előtt érdemes újrarajzolni őket.

## TLauncher
- A TLauncherben válaszd az **„End Realms 26.3”** verziót, és indítsd el.
- A mod és a Fabric API innen töltődik be: `D:\minecraft mod\tlauncher-mods` (a sima `.minecraft\mods` mappához nem nyúl).
- Minden `./gradlew build` automatikusan bemásolja ide a legújabb mod-jart (a régit törli).
- Ha a Minecraft/Fabric verzió változik: `python tools/make_tlauncher_version.py <fabric profile json>` újragenerálja a verziót.
