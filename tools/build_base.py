#!/usr/bin/env python3
"""Zip pack/ into dist/FPS_Modpack_Base_<version>.mrpack.

This needs no network: it only packages the entries whose hashes are already
verified, plus the tuned configs. Use it to get a working instance immediately;
use build_pack.py to add the optimization mods on top.
"""
import json, os, sys, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
pack = os.path.join(ROOT, 'pack')
idx = json.load(open(os.path.join(pack, 'modrinth.index.json')))
out = os.path.join(ROOT, 'dist')
os.makedirs(out, exist_ok=True)
dest = os.path.join(out, f"FPS_Modpack_Base_{idx['versionId']}.mrpack")

with zipfile.ZipFile(dest, 'w', zipfile.ZIP_DEFLATED) as z:
    z.write(os.path.join(pack, 'modrinth.index.json'), 'modrinth.index.json')
    for dirpath, _, filenames in os.walk(os.path.join(pack, 'overrides')):
        for fn in sorted(filenames):
            full = os.path.join(dirpath, fn)
            z.write(full, os.path.relpath(full, pack).replace(os.sep, '/'))

mb = sum(f['fileSize'] for f in idx['files']) / 1048576
print(f"Wrote {dest}\n{len(idx['files'])} files, ~{mb:.0f} MB to download on import")
