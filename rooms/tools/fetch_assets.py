#!/usr/bin/env python3
"""
downloads the entity art, sounds, music and the official rooms icon listed in
assets/manifest.json from the doors / rooms / rooms: revisited fandom wikis into
assets/local/ (which is gitignored).

these files belong to the original games. lsplash and redibles said personal use only,
so keep them on your machine: never commit or publish assets/local/.

usage:  python3 tools/fetch_assets.py            (skips files you already have)
        python3 tools/fetch_assets.py --force    (downloads everything again)
needs only python 3, no extra packages.
"""
import json
import os
import sys
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MANIFEST = os.path.join(ROOT, "assets", "manifest.json")
UA = {"User-Agent": "rooms-the-hallway-asset-fetcher/1.0 (personal fan project)"}


def res_to_disk(path):
    return os.path.join(ROOT, path.replace("res://", "", 1))


def file_url(wiki, name):
    # the mediawiki api gives the real file url (the normal pages block scripts)
    q = urllib.parse.urlencode({
        "action": "query", "titles": "File:" + name, "prop": "imageinfo",
        "iiprop": "url", "format": "json",
    })
    req = urllib.request.Request(f"https://{wiki}.fandom.com/api.php?{q}", headers=UA)
    data = json.load(urllib.request.urlopen(req, timeout=30))
    for page in data.get("query", {}).get("pages", {}).values():
        info = page.get("imageinfo")
        if info:
            return info[0]["url"]
    return None


def entries(manifest):
    if "icon" in manifest:
        yield "icon", manifest["icon"]
    for section in ("textures", "sounds", "music", "models"):
        for key, e in manifest.get(section, {}).items():
            if isinstance(e, dict):
                yield f"{section}/{key}", e


def main():
    force = "--force" in sys.argv
    manifest = json.load(open(MANIFEST))
    got = skipped = failed = 0
    for key, e in entries(manifest):
        src = e.get("source")
        if not src:
            continue
        dest = res_to_disk(e["path"])
        if os.path.exists(dest) and not force:
            skipped += 1
            continue
        try:
            url = file_url(src["wiki"], src["file"])
            if not url:
                print(f"missing on wiki: {key} ({src['wiki']} / {src['file']})")
                failed += 1
                continue
            # the wiki cdn converts images to webp unless you ask for the original
            url += ("&" if "?" in url else "?") + "format=original"
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=60) as r, open(dest, "wb") as f:
                f.write(r.read())
            print(f"ok   {key} <- {src['file']}")
            got += 1
        except Exception as ex:  # keep going, the game uses placeholders for anything missing
            print(f"fail {key}: {ex}")
            failed += 1
    print(f"\ndownloaded {got}, already had {skipped}, failed {failed}")
    print("open the project in godot once so it imports the new files.")
    print("reminder: assets/local/ is for personal use only, don't commit or share it.")


if __name__ == "__main__":
    main()
