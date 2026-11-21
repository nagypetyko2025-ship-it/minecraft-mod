"""Builds server/jatekos-csomag.zip: everything a player needs, with a double-click installer for TLauncher.
Run it after a new mod version has been built.
usage: python make_player_pack.py"""
import glob
import json
import os
import re
import struct
import zipfile

ROOT = r"D:\minecraft mod"
HOST = "impairment-wimp.tun.ply.gg:20574"
SERVER_NAME = "End Realms szerver"
VERSION = "End Realms 26.3"
SOURCE = os.path.join(os.environ["APPDATA"], ".minecraft", "versions", VERSION)
PACK_URL = "https://raw.githubusercontent.com/nagypetyko2025-ship-it/minecraft-mod/main/updates/jatekos-csomag.zip"


def nbt_string(value):
    data = value.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def server_entry():
    return b"\x08" + nbt_string("ip") + nbt_string(HOST) + b"\x08" + nbt_string("name") + nbt_string(SERVER_NAME) + b"\x00"


def servers_dat():
    return b"\x0a" + nbt_string("") + b"\x09" + nbt_string("servers") + b"\x0a" + struct.pack(">i", 1) + server_entry() + b"\x00"


with open(os.path.join(SOURCE, VERSION + ".json"), encoding="utf-8") as f:
    version = json.load(f)
replaced = 0


def swap(node):
    global replaced
    if isinstance(node, str) and "fabric.addMods=" in node:
        replaced += 1
        return "-Dfabric.addMods=__MODS__"
    if isinstance(node, list):
        return [swap(n) for n in node]
    if isinstance(node, dict):
        return {k: swap(v) for k, v in node.items()}
    return node


version = swap(version)
assert replaced == 1, f"expected one fabric.addMods argument, found {replaced}"
client = version["downloads"]["client"]

INSTALL = r'''param([string]$MinecraftDir = (Join-Path $env:APPDATA '.minecraft'), [string]$Desktop = [Environment]::GetFolderPath('Desktop'))
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $PSScriptRoot
$versionName = '__VERSION__'
$hostAddress = '__HOST__'
$serverName = '__SERVER__'
$clientUrl = '__URL__'
$clientSha1 = '__SHA1__'

function Done($text) { Write-Host ''; Write-Host $text -ForegroundColor Green }

Write-Host '=== End Realms telepito ===' -ForegroundColor Cyan
if (-not (Test-Path (Join-Path $here 'files'))) {
    throw 'Elobb csomagold ki a teljes zip fajlt egy mappaba, es onnan inditsd a TELEPITES.bat-ot.'
}
if (-not (Test-Path $MinecraftDir)) {
    throw "Nem talalom a Minecraft mappat ($MinecraftDir). Inditsd el egyszer a TLaunchert, aztan probald ujra."
}

$target = Join-Path (Join-Path $MinecraftDir 'versions') $versionName
$mods = Join-Path $target 'mods'
New-Item -ItemType Directory -Force $mods | Out-Null

foreach ($jar in Get-ChildItem $mods -Filter '*.jar' -ErrorAction SilentlyContinue) {
    try { $stream = [IO.File]::Open($jar.FullName, 'Open', 'ReadWrite', 'None'); $stream.Close() }
    catch { throw 'A Minecraft most fut, es fogja a modokat. Zard be a jatekot (es a TLaunchert), aztan probald ujra.' }
}

Write-Host 'Modok masolasa...'
Get-ChildItem $mods -Filter '*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
Copy-Item (Join-Path $here 'files\mods\*.jar') $mods -Force

Write-Host 'Jatekverzio beallitasa...'
$json = [IO.File]::ReadAllText((Join-Path $here 'files\version.json'), [Text.Encoding]::UTF8)
$json = $json.Replace('__MODS__', $mods.Replace('\', '\\'))
[IO.File]::WriteAllText((Join-Path $target "$versionName.json"), $json, (New-Object Text.UTF8Encoding($false)))
Copy-Item (Join-Path $here 'files\TLauncherAdditional.json') $target -Force

$jar = Join-Path $target "$versionName.jar"
function Sha1Of($path) { (Get-FileHash $path -Algorithm SHA1).Hash.ToLower() }
if (-not (Test-Path $jar) -or (Sha1Of $jar) -ne $clientSha1) {
    Write-Host 'Minecraft 26.3 letoltese a Mojangtol (kb. 40 MB)...'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $clientUrl -OutFile $jar -UseBasicParsing
    if ((Sha1Of $jar) -ne $clientSha1) { Remove-Item $jar -Force; throw 'A letoltott jatekfajl serult. Probald ujra.' }
}

Write-Host 'Szerver hozzaadasa a listahoz...'
$servers = Join-Path $MinecraftDir 'servers.dat'
$added = $false
if (-not (Test-Path $servers)) {
    Copy-Item (Join-Path $here 'files\servers.dat') $servers
    $added = $true
} else {
    $bytes = [IO.File]::ReadAllBytes($servers)
    $text = [Text.Encoding]::UTF8.GetString($bytes)
    $name = [Text.Encoding]::ASCII.GetString($bytes, [Math]::Min(6, $bytes.Length - 1), [Math]::Min(7, $bytes.Length - 6))
    if ($text.Contains($hostAddress)) {
        $added = $true
    } elseif ($bytes.Length -gt 17 -and $bytes[0] -eq 10 -and $bytes[3] -eq 9 -and $name -eq 'servers' -and ($bytes[13] -eq 10 -or $bytes[13] -eq 0) -and $bytes[$bytes.Length - 1] -eq 0) {
        Copy-Item $servers "$servers.endrealms-bak" -Force
        $entry = [IO.File]::ReadAllBytes((Join-Path $here 'files\servers.dat'))
        $count = ([int]$bytes[14] -shl 24) + ([int]$bytes[15] -shl 16) + ([int]$bytes[16] -shl 8) + [int]$bytes[17] + 1
        $body = $entry[18..($entry.Length - 2)]
        $out = New-Object IO.MemoryStream
        $out.Write($bytes, 0, 13)
        $out.WriteByte(10)
        $out.WriteByte(($count -shr 24) -band 255); $out.WriteByte(($count -shr 16) -band 255); $out.WriteByte(($count -shr 8) -band 255); $out.WriteByte($count -band 255)
        $out.Write($bytes, 18, $bytes.Length - 19)
        $out.Write($body, 0, $body.Length)
        $out.WriteByte(0)
        [IO.File]::WriteAllBytes($servers, $out.ToArray())
        $added = $true
    }
}

Write-Host 'Frissito elhelyezese az asztalon...'
Copy-Item (Join-Path $here 'files\frissites.ps1') (Join-Path $target 'frissites.ps1') -Force
if ($Desktop -and (Test-Path $Desktop)) {
    $bat = "@echo off`r`ntitle End Realms frissito`r`npowershell -NoProfile -ExecutionPolicy Bypass -File `"$target\frissites.ps1`"`r`nif errorlevel 1 echo. & echo HIBA: a frissites nem sikerult, olvasd el a fenti uzenetet.`r`necho.`r`npause`r`n"
    [IO.File]::WriteAllText((Join-Path $Desktop 'End Realms frissites.bat'), $bat, [Text.Encoding]::ASCII)
}

Done 'Kesz! A telepites sikerult.'
Write-Host ''
Write-Host "1. A TLauncherben valaszd ki ezt a verziot: $versionName"
Write-Host '   (Ha nem latod, kapcsold be a modositott verziok mutatasat a beallitasokban, vagy inditsd ujra a TLaunchert.)'
Write-Host '2. Inditsd el a jatekot. Az elso inditasnal a TLauncher letolti a hianyzo fajlokat, ez par percig tarthat.'
Write-Host '   Kesobb, ha a szerver uj modokat kap: inditsd el az asztalon az "End Realms frissites" fajlt, az mindent letolt.'
if ($added) { Write-Host "3. Tobbjatekos menu: ott talalod a szervert ($serverName)." } else { Write-Host "3. Tobbjatekos menu: add hozza kezzel ezt a cimet: $hostAddress" }
Write-Host '4. Elso belepeskor irj jelszot:  /register jelszo jelszo     kesobb:  /login jelszo'
'''.replace("__VERSION__", VERSION).replace("__HOST__", HOST).replace("__SERVER__", SERVER_NAME).replace("__URL__", client["url"]).replace("__SHA1__", client["sha1"])

UPDATE = r'''# End Realms frissito: letolti a legujabb jatekos-csomagot a GitHubrol, es lefuttatja a telepitojet.
$ErrorActionPreference = 'Stop'
$url = '__PACK_URL__'
Write-Host '=== End Realms frissito ===' -ForegroundColor Cyan
$tmp = Join-Path $env:TEMP 'endrealms-frissites'
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
New-Item -ItemType Directory -Force $tmp | Out-Null
Write-Host 'A legujabb csomag letoltese...'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue'
Invoke-WebRequest -Uri $url -OutFile (Join-Path $tmp 'csomag.zip') -UseBasicParsing
Expand-Archive -LiteralPath (Join-Path $tmp 'csomag.zip') -DestinationPath (Join-Path $tmp 'csomag') -Force
& (Join-Path $tmp 'csomag\files\install.ps1')
Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
'''.replace("__PACK_URL__", PACK_URL)

BAT = '\r\n'.join(['@echo off', 'title End Realms telepito', 'powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0files\\install.ps1"',
                   'if errorlevel 1 echo. & echo HIBA: a telepites nem sikerult, olvasd el a fenti uzenetet.', 'echo.', 'pause', ''])
README = f"""End Realms szerver

Telepites:
1. Csomagold ki ezt a zip fajlt egy mappaba.
2. Inditsd el a TELEPITES.bat fajlt (dupla kattintas).
   Ha a Windows figyelmeztet, valaszd a "Tovabbi informacio", majd a "Futtatas mindenkeppen" lehetoseget.
3. Kovesd a kiirt lepeseket.

Szerver cime, ha kezzel kellene megadni: {HOST}

Uj modverzio eseten: az End Realms mod magatol frissul a jatekban. Ha a szerver mas modokat is kap,
inditsd el az asztalon az "End Realms frissites" fajlt (a telepito tette oda), az mindent letolt.
A szerver csak akkor elerheto, ha a gazda gepe be van kapcsolva es a szerver fut.
"""

out = os.path.join(ROOT, "server", "jatekos-csomag.zip")
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("TELEPITES.bat", BAT.encode("ascii"))
    z.writestr("OLVASS_EL.txt", README.encode("utf-8"))
    z.writestr("files/install.ps1", b"\xef\xbb\xbf" + INSTALL.encode("utf-8"))
    z.writestr("files/frissites.ps1", b"\xef\xbb\xbf" + UPDATE.encode("utf-8"))
    z.writestr("files/version.json", json.dumps(version, ensure_ascii=False).encode("utf-8"))
    z.write(os.path.join(SOURCE, "TLauncherAdditional.json"), "files/TLauncherAdditional.json")
    z.writestr("files/servers.dat", servers_dat())
    # the End Realms jar comes from the build: tlauncher-mods stays out of date while the game keeps its jar locked
    with open(os.path.join(ROOT, "endrealms", "gradle.properties"), encoding="utf-8") as f:
        mod_version = re.search(r"^version=(.+)$", f.read(), re.M).group(1).strip()
    mod_jar = os.path.join(ROOT, "endrealms", "build", "libs", f"endrealms-{mod_version}.jar")
    assert os.path.exists(mod_jar), mod_jar
    others = [j for j in glob.glob(os.path.join(ROOT, "tlauncher-mods", "*.jar")) if "endrealms-" not in os.path.basename(j)]
    assert len(others) == 1, others
    for jar in [mod_jar] + others:
        z.write(jar, "files/mods/" + os.path.basename(jar))
print("ok", os.path.getsize(out) // 1024, "KB")
