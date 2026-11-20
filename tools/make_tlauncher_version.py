"""Builds a standalone (merged) TLauncher version: vanilla 26.3 + Fabric loader + mods from tlauncher-mods."""
import hashlib, json, os, shutil, sys, urllib.request

MC = os.path.join(os.environ["APPDATA"], ".minecraft")
VANILLA = "26.3"
NAME = "End Realms 26.3"
MODS_DIR = r"D:\minecraft mod\tlauncher-mods"
fabric = json.load(open(sys.argv[1], encoding="utf-8"))
van = json.load(open(os.path.join(MC, "versions", VANILLA, VANILLA + ".json"), encoding="utf-8"))

def ga(name):  # group:artifact
    return ":".join(name.split(":")[:2])

libs = []
for lib in fabric["libraries"]:
    g, a, v = lib["name"].split(":")
    path = f"{g.replace('.', '/')}/{a}/{v}/{a}-{v}.jar"
    url = lib["url"].rstrip("/") + "/" + path
    dest = os.path.join(MC, "libraries", *path.split("/"))
    if not os.path.exists(dest):
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        urllib.request.urlretrieve(url, dest)
    data = open(dest, "rb").read()
    sha1 = hashlib.sha1(data).hexdigest()
    if "sha1" in lib and lib["sha1"] != sha1:
        raise SystemExit(f"sha1 mismatch for {lib['name']}")
    libs.append({"name": lib["name"], "downloads": {"artifact": {
        "path": path, "url": url, "sha1": sha1, "size": len(data)}}})

fab_ga = {ga(l["name"]) for l in libs}
dropped = [l["name"] for l in van["libraries"] if ga(l["name"]) in fab_ga]
merged = dict(van)
merged["id"] = NAME
merged["type"] = "modified"
merged["mainClass"] = fabric["mainClass"]
merged["libraries"] = libs + [l for l in van["libraries"] if ga(l["name"]) not in fab_ga]
merged["arguments"] = dict(van["arguments"])
merged["arguments"]["jvm"] = list(van["arguments"]["jvm"]) + fabric["arguments"]["jvm"] + [
    "-Dfabric.addMods=" + MODS_DIR]

vdir = os.path.join(MC, "versions", NAME)
os.makedirs(vdir, exist_ok=True)
json.dump(merged, open(os.path.join(vdir, NAME + ".json"), "w", encoding="utf-8"), indent=2)
shutil.copyfile(os.path.join(MC, "versions", VANILLA, VANILLA + ".jar"), os.path.join(vdir, NAME + ".jar"))
print("written", vdir, "| fabric libs:", len(libs), "| vanilla duplicates dropped:", dropped)
