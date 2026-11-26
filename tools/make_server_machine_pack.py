"""Packs the End Realms server for the server machine, and sets up this computer's remote panel.

usage: python tools/make_server_machine_pack.py [--source <server folder>] [--target <folder>] [--backups]

Run it with the server stopped. It makes:
  <target>/EndRealmsServer   the whole server folder to copy onto the server machine (the world as a real folder, Java in
                             jdk/, start-server.bat pointing there), with Szerverugynok.exe and Telepites.bat in it
  Tavoli-szerverpanel/       next to this repository: Szerverpanel.exe in remote mode, which knows the agent's token
The world backups (backups/, many GB) only go along with --backups.
"""
import argparse
import os
import secrets
import shutil
import socket
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JDK = os.path.join(ROOT, "tools", "jdk-25.0.5+8")
APP = os.path.join(ROOT, "tools", "panel_app")

parser = argparse.ArgumentParser()
parser.add_argument("--source", default=os.path.join(ROOT, "server"))
parser.add_argument("--target", default=os.path.join(ROOT, "szervergep-csomag"))
parser.add_argument("--remote-panel", default=os.path.join(ROOT, "Tavoli-szerverpanel"))
parser.add_argument("--backups", action="store_true")
args = parser.parse_args()
source = os.path.abspath(args.source)
server = os.path.join(os.path.abspath(args.target), "EndRealmsServer")


def port_open(port):
    with socket.socket() as s:
        s.settimeout(0.3)
        return s.connect_ex(("127.0.0.1", port)) == 0


def game_port():
    for line in open(os.path.join(source, "server.properties"), encoding="utf-8", errors="replace"):
        if line.startswith("server-port="):
            return int(line.split("=", 1)[1])
    return 25565


if source == os.path.join(ROOT, "server") and (port_open(25580) or port_open(game_port())):
    sys.exit("A szerver fut. Állítsd le (a panelen: Leállítás), és utána futtasd újra, hogy a világ ép állapotban kerüljön át.")
if os.path.exists(server) and os.listdir(server):
    sys.exit(f"{server} már létezik és nem üres. Töröld vagy adj meg másik --target mappát.")
os.makedirs(server, exist_ok=True)

# robocopy follows the world junction (world -> C:\EndRealmsServer\world), so the world arrives as a real folder.
skip_dirs = ["world-hdd-regi", ".fabric", "crash-reports", "debug"] + ([] if args.backups else ["backups"])
skip_files = ["Szerverpanel.regi.exe", "Szerverpanel.old", "jvm-flags.regi.txt", "*.flag", "playitd.log*", "ugynok.token", "Szerverugynok.exe"]
print("Szerver másolása:", source, "->", server)
result = subprocess.run(["robocopy", source, server, "/E", "/COPY:DAT", "/R:2", "/W:2", "/NFL", "/NDL", "/NP", "/XD", *skip_dirs, "/XF", *skip_files])
if result.returncode >= 8:
    sys.exit(f"robocopy hiba: {result.returncode}")

print("Java másolása:", JDK)
shutil.copytree(JDK, os.path.join(server, "jdk"))

bat = os.path.join(server, "start-server.bat")
text = open(bat, encoding="utf-8").read()
lines = text.split("\n")
java = [i for i, line in enumerate(lines) if line.startswith("set JAVA=")]
assert len(java) == 1, "start-server.bat: nincs pontosan egy 'set JAVA=' sor"
lines[java[0]] = 'set JAVA="%~dp0jdk\\bin\\java.exe"' + ("\r" if lines[java[0]].endswith("\r") else "")
open(bat, "w", encoding="utf-8", newline="").write("\n".join(lines))

# On this computer the world was moved from the slow disk onto C:\ (the SSD). On the server machine the whole folder
# should sit on its fastest disk instead, so the move is left out there.
prepare = os.path.join(server, "prepare-server.ps1")
text = open(prepare, encoding="utf-8").read()
if text.count("\nMove-WorldToSsd") == 1:
    text = text.replace("\nMove-WorldToSsd", "\n# Move-WorldToSsd      (a szervergépen nem kell: a teljes szervermappa legyen a leggyorsabb lemezen)")
    open(prepare, "w", encoding="utf-8", newline="").write(text)

print("Ügynök és helyi Szerverpanel fordítása")
subprocess.run([sys.executable, os.path.join(APP, "build.py"), server, "agent"], check=True)
subprocess.run([sys.executable, os.path.join(APP, "build.py"), server], check=True)
for leftover in ("Szerverugynok.old", "Szerverpanel.old"):
    if os.path.exists(os.path.join(server, leftover)):
        os.remove(os.path.join(server, leftover))
token = secrets.token_hex(24)
with open(os.path.join(server, "ugynok.token"), "w", encoding="ascii") as f:
    f.write(token)
shutil.copy(os.path.join(APP, "Telepites.bat"), server)
shutil.copy(os.path.join(APP, "OLVASS-EL-szervergep.txt"), os.path.join(server, "OLVASS-EL.txt"))

print("Távoli Szerverpanel ezen a gépen:", args.remote_panel)
subprocess.run([sys.executable, os.path.join(APP, "build.py"), args.remote_panel], check=True)
remote = os.path.join(args.remote_panel, "tavoli.txt")
address = ""
if os.path.exists(remote):
    for line in open(remote, encoding="utf-8"):
        if line.startswith("cim="):
            address = line[4:].strip()
with open(remote, "w", encoding="utf-8") as f:
    f.write("# A szervergép címe a házi hálózaton, és a rajta futó ügynök tokenje (a szervergép ugynok.token fájljából).\n")
    f.write(f"cim={address}\ntoken={token}\n")

size = 0
for base, _, files in os.walk(server):
    for name in files:
        try:
            size += os.path.getsize(os.path.join(base, name))
        except OSError:
            pass
print(f"\nKész. A csomag: {server} ({size / 1073741824:.1f} GB)")
print("Másold át a szervergépre (például C:\\EndRealmsServer), és ott futtasd a Telepites.bat-ot.")
print(f"Ezen a gépen ezt nyisd meg: {os.path.join(args.remote_panel, 'Szerverpanel.exe')}")
