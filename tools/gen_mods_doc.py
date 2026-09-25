#!/usr/bin/env python3
"""Regenerate docs/MODS.md from optimization-mods.json + pack/modrinth.index.json."""
import json, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
man = json.load(open(os.path.join(ROOT, 'optimization-mods.json')))
idx = json.load(open(os.path.join(ROOT, 'pack', 'modrinth.index.json')))

SIDE = {'both': 'client + server', 'client': 'client only'}
on  = [m for m in man['mods'] if m.get('enabled', True)]
off = [m for m in man['mods'] if not m.get('enabled', True)]

def pretty(path):
    n = os.path.basename(path)
    dis = n.endswith('.disabled')
    if dis:
        n = n[:-len('.disabled')]
    n = re.sub(r'[-_ ]?v?\d[\d.]*(\+|-)?.*\.jar$', '', n) or n
    return n.replace('.jar', '').replace('-', ' ').strip(), dis

lines = [
    '# Mod reference',
    '',
    f"Minecraft {man['minecraft']} / {man['loader']}. "
    'Generated from `optimization-mods.json` by `tools/gen_mods_doc.py` — edit the '
    'manifest, not this file.',
    '',
    f"Every mod here was checked against Modrinth for a 1.21.11 Fabric build on "
    f"**{man.get('verified_on','(unrecorded)')}**. Three candidates were dropped because no "
    'such build exists, and four resolved to maintained forks — see the bottom of this file.',
    '',
    f'## Optimization mods added by the build script ({len(on)})',
    '',
]
def block(m):
    head = f"### {m['name']}  <sub>{SIDE[m.get('side','client')]}</sub>"
    body = [head, '']
    if m.get('verified'):
        body += [f"Verified: **{m['verified']}**", '']
    return body + [m['reason'], '']

for m in on:
    lines += block(m)

lines += [f'## Shipped but disabled ({len(off)})', '',
          'Present in `mods/` with a `.disabled` suffix. Rename to remove the suffix to turn one on.', '']
for m in off:
    lines += block(m)

carried, disabled_base = [], []
for f in idx['files']:
    if not f['path'].startswith('mods/'):
        continue
    name, dis = pretty(f['path'])
    (disabled_base if dis else carried).append(name)

lines += [
    f'## Carried over from your original pack ({len(carried) + len(disabled_base)})',
    '',
    'Your PvP and quality-of-life mods, kept at the exact versions you were '
    'already running — the hashes come straight out of your own `FPS_Modpack_1.0.0.mrpack`.',
    '',
]
lines += [f'- {n}' for n in sorted(carried, key=str.lower)]
if disabled_base:
    lines += ['', 'Left disabled, as they were in your pack:', '']
    lines += [f'- {n}' for n in sorted(disabled_base, key=str.lower)]

rp = [f['path'].split('/', 1)[1] for f in idx['files'] if f['path'].startswith('resourcepacks/')]
if rp:
    lines += ['', '## Resource packs', '']
    lines += [f'- {n}' for n in sorted(rp, key=str.lower)]
    lines += ['', 'None are enabled by default — this pack deliberately ships no `options.txt`, '
              'so your keybinds, sensitivity and resource pack order stay exactly as they are. '
              'Turn them on in Options → Resource Packs.']

lines += ['', '## Dropped: no 1.21.11 Fabric build', '',
          '- **Memory Leak Fix** — nothing past 1.20.4. ModernFix-mVUS covers most of the same ground.',
          '- **Faster Random** — project archived at 1.21.1.',
          '- **Let Me Despawn** — nothing past 1.21.9.', '']
lines += ['## Resolved to a maintained fork', '',
          '- **ModernFix** → **ModernFix-mVUS**: upstream stops at 1.21.1.',
          '- **Cull Less Leaves** → **Cull Fewer Leaves**: upstream stops at 1.21.1.',
          '- **Enhanced Block Entities** → **Better Block Entities**: upstream stops at 1.21.4.',
          '- **Noisium** → **NoisiumForked**: upstream has no 1.21.11 build.', '']
lines += ['## Removed', '',
          '- **ClickCrystals** — an automation client, not a performance mod, and a ban risk '
          'on any server that checks. It was already disabled in your pack.',
          '- **Meteor, Xenon, Prestige, Nova, Marlowww, zerio, glazed, 4e Client+** — cheat '
          'clients that were sitting disabled in your `overrides/mods/`. None of them affect '
          'FPS, so none were carried over.',
          '- **`config/imnotcheatingyouare/`, `config/ExploitPreventer.json`** — leftover '
          'config from the above.', '']

out = os.path.join(ROOT, 'docs', 'MODS.md')
open(out, 'w').write('\n'.join(lines))
print(f'wrote {out} ({len(on)} enabled, {len(off)} disabled, {len(carried) + len(disabled_base)} carried over)')
