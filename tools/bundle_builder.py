#!/usr/bin/env python3
"""Bundle everything needed to build the pack into dist/FPS-Modpack-Builder.zip.

This is the archive you hand someone: unzip it anywhere and double-click
BUILD-PACK.bat. It carries the manifest, both builders, the tuned configs and
the docs, but no mod jars — the builder downloads those from Modrinth.
"""
import os, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FILES = [
    'BUILD-PACK.bat', 'README.md', 'optimization-mods.json',
    'tools/build-pack.ps1', 'tools/build_pack.py', 'tools/build_base.py',
    'tools/gen_mods_doc.py', 'tools/bundle_builder.py',
    'docs/MODS.md', 'docs/TUNING.md', 'pack/modrinth.index.json',
]

dest = os.path.join(ROOT, 'dist', 'FPS-Modpack-Builder.zip')
os.makedirs(os.path.dirname(dest), exist_ok=True)

with zipfile.ZipFile(dest, 'w', zipfile.ZIP_DEFLATED) as z:
    for rel in FILES:
        full = os.path.join(ROOT, rel)
        if not os.path.exists(full):
            raise SystemExit(f'missing: {rel}')
        z.write(full, rel)
    for dirpath, _, filenames in os.walk(os.path.join(ROOT, 'pack', 'overrides')):
        for fn in sorted(filenames):
            full = os.path.join(dirpath, fn)
            z.write(full, os.path.relpath(full, ROOT).replace(os.sep, '/'))
    count = len(z.namelist())

print(f'Wrote {dest} ({count} files, {os.path.getsize(dest) / 1024:.0f} KB)')
