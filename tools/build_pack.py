#!/usr/bin/env python3
"""Build FPS_Modpack_Optimized.mrpack for Minecraft 1.21.11 / Fabric.

Resolves every mod in optimization-mods.json against the Modrinth API, pulls in
their required dependencies, merges them with the verified base index in pack/,
and writes a finished .mrpack.

Run this on your own PC - it needs to reach api.modrinth.com.

    python tools/build_pack.py

Options:
    --out DIR        where to write the .mrpack (default: dist/)
    --mc VERSION     Minecraft version (default: from optimization-mods.json)
    --allow-missing  keep going even if a mod has no build for this version
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from typing import Any

API = "https://api.modrinth.com/v2"
UA = "fps-modpack-builder/2.0 (github.com/stashoffman010-droid/claude)"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def get(path: str, **params: Any) -> Any:
    url = f"{API}{path}"
    if params:
        url += "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def resolve_project(slug: str, name: str) -> str | None:
    """Return a real project id/slug. Falls back to search if the slug is stale."""
    try:
        return get(f"/project/{urllib.parse.quote(slug)}")["slug"]
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise
    facets = json.dumps([["project_type:mod"], ["categories:fabric"]])
    hits = get("/search", query=name, facets=facets, limit=3).get("hits", [])
    if not hits:
        return None
    found = hits[0]["slug"]
    print(f"    slug '{slug}' is stale -> resolved by search to '{found}'")
    return found


def pick_version(project: str, mc: str, loader: str) -> dict | None:
    """Newest release for this MC version, preferring release over beta/alpha."""
    versions = get(
        f"/project/{urllib.parse.quote(project)}/version",
        loaders=json.dumps([loader]),
        game_versions=json.dumps([mc]),
    )
    if not versions:
        return None
    rank = {"release": 0, "beta": 1, "alpha": 2}
    versions.sort(key=lambda v: (rank.get(v.get("version_type"), 3), -_epoch(v)))
    return versions[0]


def _epoch(v: dict) -> int:
    # date_published is ISO-8601; string order is chronological, so hash to int order
    return int("".join(c for c in v.get("date_published", "") if c.isdigit())[:14] or 0)


def primary_file(version: dict) -> dict:
    files = version["files"]
    return next((f for f in files if f.get("primary")), files[0])


def to_entry(version: dict, side: str, enabled: bool) -> dict:
    f = primary_file(version)
    path = f"mods/{f['filename']}" + ("" if enabled else ".disabled")
    env = {
        "client": "required",
        "server": "required" if side == "both" else "unsupported",
    }
    return {
        "path": path,
        "hashes": {"sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"]},
        "env": env,
        "downloads": [f["url"]],
        "fileSize": f["size"],
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "dist"))
    ap.add_argument("--mc", default=None)
    ap.add_argument("--allow-missing", action="store_true")
    args = ap.parse_args()

    manifest = json.load(open(os.path.join(ROOT, "optimization-mods.json")))
    mc = args.mc or manifest["minecraft"]
    loader = manifest["loader"]

    base_path = os.path.join(ROOT, "pack", "modrinth.index.json")
    base = json.load(open(base_path))

    # Filenames already in the base pack, so we never ship a mod twice.
    def key(path: str) -> str:
        return os.path.basename(path).lower().removesuffix(".disabled")

    have = {key(f["path"]) for f in base["files"]}
    have_sha = {f["hashes"]["sha1"] for f in base["files"]}

    print(f"Building for Minecraft {mc} / {loader}")
    print(f"Base pack: {len(base['files'])} verified files\n")

    added: list[dict] = []
    missing: list[str] = []
    queue = [(m["slug"], m["name"], m.get("side", "client"), m.get("enabled", True), False)
             for m in manifest["mods"]]
    seen_projects: set[str] = set()

    while queue:
        slug, name, side, enabled, is_dep = queue.pop(0)
        label = f"  {'dep ' if is_dep else ''}{name}"
        project = resolve_project(slug, name)
        if project is None:
            print(f"{label}: NOT FOUND on Modrinth")
            missing.append(name)
            continue
        if project in seen_projects:
            continue
        seen_projects.add(project)

        version = pick_version(project, mc, loader)
        if version is None:
            print(f"{label}: no {loader} build for {mc}")
            missing.append(name)
            continue

        entry = to_entry(version, side, enabled)
        if key(entry["path"]) in have or entry["hashes"]["sha1"] in have_sha:
            print(f"{label}: already in base pack, skipping")
            continue

        added.append(entry)
        have.add(key(entry["path"]))
        have_sha.add(entry["hashes"]["sha1"])
        flag = "" if enabled else "  [shipped disabled]"
        print(f"{label}: {version['version_number']}{flag}")

        # Pull in required dependencies we do not already have.
        for dep in version.get("dependencies", []):
            if dep.get("dependency_type") != "required":
                continue
            dep_id = dep.get("project_id")
            if not dep_id or dep_id in seen_projects:
                continue
            queue.append((dep_id, dep_id, side, True, True))

    out = dict(base)
    out["files"] = sorted(base["files"] + added, key=lambda f: f["path"].lower())

    os.makedirs(args.out, exist_ok=True)
    dest = os.path.join(args.out, f"FPS_Modpack_Optimized_{out['versionId']}.mrpack")
    overrides = os.path.join(ROOT, "pack", "overrides")

    with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("modrinth.index.json", json.dumps(out, indent=2) + "\n")
        for dirpath, _, filenames in os.walk(overrides):
            for fn in sorted(filenames):
                full = os.path.join(dirpath, fn)
                rel = os.path.relpath(full, os.path.join(ROOT, "pack"))
                z.write(full, rel.replace(os.sep, "/"))

    total = sum(f["fileSize"] for f in out["files"])
    print(f"\nAdded {len(added)} optimization mods "
          f"({len(out['files'])} files total, ~{total / 1048576:.0f} MB to download)")
    if missing:
        print("\nCould not resolve for this version:")
        for m in missing:
            print(f"  - {m}")
        if not args.allow_missing:
            print("(pack was still written; pass --allow-missing to silence this)")
    print(f"\nWrote {dest}")
    print("Import it in the Modrinth App or Prism Launcher: it will download every mod itself.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
