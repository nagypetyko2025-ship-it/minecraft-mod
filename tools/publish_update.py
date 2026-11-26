"""Publishes the built End Realms jar to GitHub, where the game's auto-updater fetches it from.

Usage: python tools/publish_update.py
Copies endrealms/build/libs/endrealms-<version>.jar (the version in gradle.properties) and server/jatekos-csomag.zip
(which the players' desktop updater installs) to updates/, removes the older jars there, and commits and pushes it together with the source. Run it before the server restarts onto the new version:
a joining player's game downloads https://raw.githubusercontent.com/nagypetyko2025-ship-it/minecraft-mod/main/updates/endrealms-<version>.jar
"""
import glob
import os
import re
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
props = open(os.path.join(ROOT, "endrealms", "gradle.properties"), encoding="utf-8").read()
version = re.search(r"^version=(\S+)", props, re.M).group(1)
jar = os.path.join(ROOT, "endrealms", "build", "libs", f"endrealms-{version}.jar")
if not os.path.isfile(jar):
    sys.exit(f"missing {jar}: build it first")

updates = os.path.join(ROOT, "updates")
os.makedirs(updates, exist_ok=True)
for old in glob.glob(os.path.join(updates, "endrealms-*.jar")):
    if os.path.basename(old) != os.path.basename(jar):
        os.remove(old)
shutil.copy2(jar, updates)
pack = os.path.join(ROOT, "server", "jatekos-csomag.zip")      # the players' desktop updater installs this whole pack
if not os.path.isfile(pack):
    sys.exit("missing server/jatekos-csomag.zip: run tools/make_player_pack.py first")
shutil.copy2(pack, updates)
# The server machine's agent (Szerverugynok.exe) reads this every 10 minutes and fetches the jar when it is newer.
with open(os.path.join(updates, "latest.txt"), "w", encoding="utf-8", newline="
") as f:
    f.write(version + "
")


def git(*args):
    print("git", " ".join(args))
    subprocess.run(["git", *args], cwd=ROOT, check=True)


git("add", "-A")
if subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=ROOT).returncode != 0:
    git("commit", "-q", "-m", f"End Realms {version}")
git("push", "-u", "origin", "main")
print(f"published {version}")
