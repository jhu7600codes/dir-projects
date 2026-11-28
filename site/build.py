#!/usr/bin/env python3
"""builds the static data for the dir-projects site.

reads every branch of the repo (refs/remotes/origin/* or local heads), merges
the top-level project folders into one list, finds downloads (dist/ folders,
loose apk/deb/vpk/... files, github release assets) and writes json for the
git ui: commits, per-commit diffs, trees.

usage: python3 site/build.py [--repo owner/name] [--out _site]
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.request
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
DL_EXT = re.compile(r"\.(apk|apks|xapk|aab|deb|ipa|vpk|zip|exe|msi|dmg|appimage|tar\.gz|tgz|7z|gmd|jar|whl)$", re.I)
SKIP_DL = re.compile(r"(gradle-wrapper\.jar$|/build/|/node_modules/|/\.gradle/)")
SKIP_TOP = {".github", ".gitignore", ".gitattributes"}
MAX_DIFF_FILE = 60_000
MAX_DIFF_TOTAL = 400_000


def git(*args, binary=False):
    out = subprocess.run(["git", *args], check=True, capture_output=True).stdout
    return out if binary else out.decode("utf-8", "replace")


def branches():
    refs = git("for-each-ref", "--format=%(refname)\t%(objectname)", "refs/remotes/origin").split("\n")
    res = {}
    for line in refs:
        if not line.strip():
            continue
        ref, sha = line.split("\t")
        name = ref[len("refs/remotes/origin/"):]
        if name == "HEAD":
            continue
        res[name] = sha
    if not res:  # fallback: local heads
        for line in git("for-each-ref", "--format=%(refname:short)\t%(objectname)", "refs/heads").split("\n"):
            if line.strip():
                n, s = line.split("\t")
                res[n] = s
    return res


def default_branch(names, forced=None):
    if forced and forced in names:
        return forced
    try:
        out = git("ls-remote", "--symref", "origin", "HEAD")
        m = re.search(r"ref: refs/heads/(\S+)\s+HEAD", out)
        if m and m.group(1) in names:
            return m.group(1)
    except Exception:
        pass
    try:
        ref = git("symbolic-ref", "refs/remotes/origin/HEAD").strip()
        return ref.split("refs/remotes/origin/", 1)[1]
    except Exception:
        pass
    for n in ("main", "master", "project.main"):
        if n in names:
            return n
    return sorted(names)[0]


def tags():
    out = git("for-each-ref", "--format=%(refname:short)\t%(*objectname)%(objectname)\t%(creatordate:unix)", "refs/tags")
    res = []
    for line in out.split("\n"):
        if not line.strip():
            continue
        name, sha, ts = line.split("\t")
        res.append({"name": name, "sha": sha[-40:], "date": int(ts or 0)})
    return res


def all_commits():
    fmt = "%H%x1f%P%x1f%an%x1f%ae%x1f%at%x1f%cn%x1f%ct%x1f%s%x1f%b%x1e"
    out = git("log", "--all", "--remotes", "--date-order", f"--format={fmt}")
    commits = []
    for rec in out.split("\x1e"):
        rec = rec.strip("\n")
        if not rec:
            continue
        h, p, an, ae, at, cn, ct, s, b = rec.split("\x1f")
        commits.append({
            "h": h, "p": p.split() if p else [], "an": an, "at": int(at),
            "cn": cn, "ct": int(ct), "s": s, "b": b.strip(),
        })
    return commits


def commit_detail(c):
    h = c["h"]
    parent = c["p"][0] if c["p"] else None
    base = [parent, h] if parent else ["--root", h]
    numstat = git("diff-tree", "-r", "-M", "--numstat", "--no-commit-id", *base)
    status = git("diff-tree", "-r", "-M", "--name-status", "--no-commit-id", *base)
    files = []
    st_lines = [l for l in status.split("\n") if l.strip()]
    ns_lines = [l for l in numstat.split("\n") if l.strip()]
    for st, ns in zip(st_lines, ns_lines):
        parts = st.split("\t")
        code = parts[0][0]
        path = parts[-1]
        old = parts[1] if len(parts) > 2 else None
        a, d = ns.split("\t")[:2]
        binary = a == "-"
        files.append({"path": path, "old": old, "st": code, "add": 0 if binary else int(a),
                      "del": 0 if binary else int(d), "bin": binary})
    total = 0
    for f in files:
        if f["bin"]:
            continue
        if total > MAX_DIFF_TOTAL:
            f["skip"] = "diff too big for the site, open it on github"
            continue
        args = ["diff", "-M", "--no-color", "-U3"]
        if parent:
            args += [parent, h]
        else:
            args += ["4b825dc642cb6eb9a060e54bf8d69288fbee4904", h]
        paths = [f["path"]] + ([f["old"]] if f["old"] else [])
        try:
            d = git(*args, "--", *paths)
        except subprocess.CalledProcessError:
            continue
        # drop header lines up to first hunk
        i = d.find("\n@@")
        d = d[i + 1:] if i >= 0 else ""
        if len(d) > MAX_DIFF_FILE:
            f["skip"] = f"big diff ({len(d)//1000} kb), open it on github"
            continue
        total += len(d)
        f["diff"] = d
    return {"files": files}


def tree_entries(tree_sha):
    out = git("ls-tree", "-l", tree_sha)
    ents = []
    for line in out.split("\n"):
        if not line.strip():
            continue
        meta, name = line.split("\t", 1)
        mode, typ, sha, size = meta.split()
        ents.append({"n": name, "t": typ[0], "h": sha, "s": None if size == "-" else int(size), "m": mode})
    ents.sort(key=lambda e: (e["t"] != "t", e["n"].lower()))
    return ents


def clean(t):
    t = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", t)
    t = re.sub(r"<[^>]+>", "", t)
    return re.sub(r"[`*_]", "", t).strip()


def readme_info(sha, folder):
    for name in ("README.md", "readme.md", "README.MD", "README", "README.txt"):
        try:
            txt = git("show", f"{sha}:{folder}/{name}")
        except subprocess.CalledProcessError:
            continue
        title, desc = None, None
        para = []
        for line in txt.split("\n"):
            s = line.strip()
            if not title and s.startswith("#"):
                title = s.lstrip("#").strip()
                continue
            if title is not None or not s.startswith("#"):
                if s.startswith(("#", "```", "|", "![", "<", "[!", ">")):
                    if para and len(clean(" ".join(para))) >= 40:
                        break
                    para = []
                    continue
                if not s:
                    if para and len(clean(" ".join(para))) >= 40:
                        break
                    para = []
                    continue
                if s.startswith(("- ", "* ")) and not para:
                    continue
                para.append(s)
        desc = clean(" ".join(para))
        return {"readme": f"{folder}/{name}", "title": title, "desc": desc[:400]}
    return {}


def detect_kind(files, name=""):
    names = set(files)
    j = " ".join(files)
    kinds = []
    if "AndroidManifest.xml" in j or re.search(r"\.apk\b", j):
        kinds.append("android")
    if re.search(r"\.deb\b", j) and ("iphoneos" in j or "Tweak" in j or "theos" in j.lower() or "ios" in j.lower()):
        kinds.append("ios")
    elif re.search(r"\.deb\b", j):
        kinds.append("linux")
    if ".vpk" in j or "sce_sys" in j:
        kinds.append("vita")
    if "ios" not in kinds and any(f.endswith((".xm", ".x", ".m", ".plist")) for f in files) and ("theos" in j.lower() or "Tweak" in j or ".xm" in j):
        kinds.append("ios")
    if any(f.endswith(".py") for f in files) and not kinds:
        kinds.append("python")
    if any(f.endswith((".html",)) for f in files) and not kinds:
        kinds.append("web")
    if any(f.endswith(".gmd") for f in files):
        kinds.append("gd level")
    if "vita" in name and "vita" not in kinds:
        kinds.append("vita")
    if any(f.endswith((".xm", ".m")) for f in files) and "ios" not in kinds:
        kinds.insert(0, "ios")
        if "python" in kinds:
            kinds.remove("python")
    return kinds or ["code"]


def version_of(sha, folder, files):
    for f in files:
        if f.endswith(("build.gradle.kts", "build.gradle")) and f.count("/") <= 2:
            try:
                txt = git("show", f"{sha}:{folder}/{f}")
            except subprocess.CalledProcessError:
                continue
            m = re.search(r"versionName\s*=?\s*\"([^\"]+)\"", txt)
            if m and "@" not in m.group(1) and "$" not in m.group(1):
                return m.group(1)
    for f in files:
        if f.endswith("control") and "DEBIAN" in f or f == "control":
            try:
                txt = git("show", f"{sha}:{folder}/{f}")
            except subprocess.CalledProcessError:
                continue
            m = re.search(r"^Version:\s*(\S+)", txt, re.M)
            if m and "@" not in m.group(1) and "$" not in m.group(1):
                return m.group(1)
    return None


def fetch_releases(repo, token):
    if not repo:
        return []
    url = f"https://api.github.com/repos/{repo}/releases?per_page=100"
    req = urllib.request.Request(url, headers={"Accept": "application/vnd.github+json", "User-Agent": "dir-projects-site"})
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            data = json.load(r)
    except Exception as e:
        print("releases: skipped,", e, file=sys.stderr)
        return []
    res = []
    for rel in data:
        res.append({
            "tag": rel.get("tag_name"), "name": rel.get("name") or rel.get("tag_name"),
            "date": rel.get("published_at") or rel.get("created_at"), "url": rel.get("html_url"),
            "pre": rel.get("prerelease"), "body": (rel.get("body") or "")[:2000],
            "assets": [{"name": a["name"], "size": a["size"], "url": a["browser_download_url"],
                        "count": a.get("download_count", 0)} for a in rel.get("assets", [])],
        })
    return res


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", "jhu7600codes/dir-projects"))
    ap.add_argument("--out", default="_site")
    ap.add_argument("--default", default=os.environ.get("DEFAULT_BRANCH"))
    ap.add_argument("--no-releases", action="store_true")
    a = ap.parse_args()
    out = a.out
    if os.path.exists(out):
        shutil.rmtree(out)
    os.makedirs(f"{out}/data/c")
    os.makedirs(f"{out}/data/t")
    for f in ("index.html", "app.js", "style.css"):
        shutil.copy(os.path.join(HERE, f), os.path.join(out, f))
    open(f"{out}/.nojekyll", "w").close()

    br = branches()
    default = default_branch(br, a.default)
    print(f"{len(br)} branches, default {default}", file=sys.stderr)

    commits = all_commits()
    by_sha = {c["h"]: c for c in commits}

    # branch info
    branch_list = []
    for name, sha in br.items():
        c = by_sha.get(sha)
        try:
            ahead, behind = git("rev-list", "--left-right", "--count", f"{sha}...{br[default]}").split()
        except Exception:
            ahead, behind = 0, 0
        top = [e for e in tree_entries(f"{sha}^{{tree}}")]
        branch_list.append({
            "name": name, "sha": sha, "tree": git("rev-parse", f"{sha}^{{tree}}").strip(),
            "date": c["ct"] if c else 0, "subject": c["s"] if c else "",
            "ahead": int(ahead), "behind": int(behind),
            "projects": [e["n"] for e in top if e["t"] == "t" and e["n"] not in SKIP_TOP],
        })
    branch_list.sort(key=lambda b: -b["date"])

    # projects: merge the same top-level folder across every branch
    proj = defaultdict(list)
    for b in branch_list:
        for p in b["projects"]:
            tree = git("rev-parse", f"{b['sha']}:{p}").strip()
            last = git("log", "-1", "--format=%H%x1f%ct%x1f%s", b["sha"], "--", p).strip()
            lh, lt, ls = last.split("\x1f") if last else (b["sha"], b["date"], b["subject"])
            proj[p].append({"branch": b["name"], "sha": b["sha"], "tree": tree,
                            "last": lh, "date": int(lt), "subject": ls})

    releases = [] if a.no_releases else fetch_releases(a.repo, os.environ.get("GITHUB_TOKEN"))

    projects = []
    for name, vers in proj.items():
        vers.sort(key=lambda v: (-v["date"], v["branch"] != default))
        best = vers[0]
        files = [l for l in git("ls-tree", "-r", "--name-only", best["tree"]).split("\n") if l]
        info = readme_info(best["sha"], name)
        # downloads: newest copy of every distinct file name across all branches
        dl = {}
        seen_tree = set()
        for v in vers:
            if v["tree"] in seen_tree:
                continue
            seen_tree.add(v["tree"])
            for line in git("ls-tree", "-r", "-l", v["tree"]).split("\n"):
                if not line.strip():
                    continue
                meta, path = line.split("\t", 1)
                _, _, blob, size = meta.split()
                full = f"{name}/{path}"
                if SKIP_DL.search(full):
                    continue
                if not ("dist/" in f"/{path}" or DL_EXT.search(path)):
                    continue
                if path.endswith((".md", ".txt", ".gitkeep")):
                    continue
                fname = os.path.basename(path)
                if fname in dl:
                    continue
                flast = git("log", "-1", "--format=%H%x1f%ct", v["sha"], "--", full).strip().split("\x1f")
                dl[fname] = {"name": fname, "path": full, "size": int(size), "blob": blob,
                             "sha": v["sha"], "branch": v["branch"], "date": int(flast[1]) if len(flast) > 1 else v["date"],
                             "url": f"https://github.com/{a.repo}/raw/{v['sha']}/{full}"}
        norm = lambda t: re.sub(r"[^a-z0-9]", "", t.lower())
        rels = [r for r in releases if r["tag"] and (norm(r["tag"]) == norm(name)
                or re.match(re.escape(norm(name)) + r"(latest|v?\d|$)", norm(r["tag"])))]
        uniq = []
        seen = set()
        for v in vers:
            key = v["tree"]
            uniq.append({**v, "same": key in seen})
            seen.add(key)
        hist = [l for l in git("log", "--all", "--remotes", "--format=%H", "--", name).split("\n") if l]
        projects.append({
            "name": name, "title": info.get("title") or name, "desc": info.get("desc") or "",
            "readme": info.get("readme"), "kinds": detect_kind(files, name), "version": version_of(best["sha"], name, files),
            "branch": best["branch"], "sha": best["sha"], "tree": best["tree"], "date": best["date"],
            "last": best["last"], "subject": best["subject"], "files": len(files),
            "versions": uniq, "downloads": sorted(dl.values(), key=lambda d: -d["date"]), "releases": rels,
            "commits": hist,
        })
    projects.sort(key=lambda p: -p["date"])

    # per-commit diffs
    for i, c in enumerate(commits):
        with open(f"{out}/data/c/{c['h']}.json", "w") as fh:
            json.dump(commit_detail(c), fh, separators=(",", ":"))
        if i % 100 == 0:
            print(f"diffs {i}/{len(commits)}", file=sys.stderr)

    # trees, deduped by hash, written lazily per directory
    todo = list({b["tree"] for b in branch_list})
    done = set()
    while todo:
        t = todo.pop()
        if t in done:
            continue
        done.add(t)
        ents = tree_entries(t)
        with open(f"{out}/data/t/{t}.json", "w") as fh:
            json.dump(ents, fh, separators=(",", ":"))
        todo.extend(e["h"] for e in ents if e["t"] == "t" and e["h"] not in done)
    print(f"{len(done)} trees", file=sys.stderr)

    meta = {"repo": a.repo, "default": default, "built": int(__import__("time").time())}
    with open(f"{out}/data/index.json", "w") as fh:
        json.dump({"meta": meta, "branches": branch_list, "projects": projects, "tags": tags(),
                   "releases": releases}, fh, separators=(",", ":"))
    with open(f"{out}/data/commits.json", "w") as fh:
        json.dump(commits, fh, separators=(",", ":"))
    print(f"{len(projects)} projects, {len(commits)} commits", file=sys.stderr)


if __name__ == "__main__":
    main()
