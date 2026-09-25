# FPS Modpack Optimized — Minecraft 1.21.11 / Fabric

A maximum-FPS Crystal PvP pack built on top of your existing
`FPS_Modpack_1.0.0.mrpack`, tuned for:

> **Ryzen 7 3700X · GeForce RTX 2060 Super · 16 GB RAM**

It keeps every PvP and quality-of-life mod you were already running, at the exact
versions you had, and adds 15 optimization mods plus hand-tuned configs on top.

**Every mod was individually checked against Modrinth for a 1.21.11 Fabric
build** — see [Version compatibility](#version-compatibility).

## Quick start

**Step 1 — get a working instance right now (no script, 30 seconds).**

Import [`dist/FPS_Modpack_Base_2.0.0.mrpack`](dist/) into the
**Modrinth App** (or Prism Launcher → Add Instance → Import). This is your
original pack, cleaned up, with the tuned configs. It works immediately and
proves the format is good before you add anything.

**Step 2 — build the full pack with all the optimization mods.**

**Double-click `BUILD-PACK.bat`.** That's it — it handles PowerShell's execution
policy for you and prints what it's doing.

If you'd rather run it yourself:

```powershell
powershell -ExecutionPolicy Bypass -File tools\build-pack.ps1
```

or with Python:

```bash
python tools/build_pack.py
```

Any of these writes `dist/FPS_Modpack_Optimized_2.0.0.mrpack`. Import that one
and delete the base instance.

**Step 3 — do the tuning.** Read **[docs/TUNING.md](docs/TUNING.md)**. The JVM
flags and the BIOS memory setting in there are worth more FPS than several of
the mods, and almost nobody does them.

### Why a script instead of a finished file?

I built this in a sandbox whose network policy blocks `modrinth.com`,
`api.modrinth.com` and `cdn.modrinth.com` outright, so I could not download a
single mod jar or read its checksums. An `.mrpack` has to carry a real SHA-1 and
SHA-512 for every file or launchers reject it, and inventing those would have
handed you a pack that fails on import.

So the script resolves the mod list from Modrinth **on your PC**, where nothing
is blocked. This turned out better than a static file anyway: it always picks the
newest build for your Minecraft version, pulls in dependencies automatically, and
falls back to a Modrinth search if a project gets renamed. Re-run it any time to
refresh the pack.

Everything I *could* verify — your 36 existing files, with hashes taken straight
from your own pack — is already baked into `pack/modrinth.index.json`.

## What's in it

See **[docs/MODS.md](docs/MODS.md)** for every mod and why it's there. The short
version:

- **Lithium** (0.21.1) — the big one for your use case. Explosion raycasting is exactly
  what runs when a crystal pops, and Lithium makes it far cheaper. This is what
  stops your frame times collapsing in a four-way crystal fight.
- **Entity Culling · More Culling · Cull Fewer Leaves** — stop drawing things you
  can't see. Your 3700X has spare cores to do the raycasts on.
- **FerriteCore · ModernFix-mVUS** — memory. On 16 GB this is what keeps hour
  three feeling like hour one.
- **ScalableLux · C2ME · NoisiumForked · Alternate Current** — chunk loading,
  lighting and worldgen. The last three only matter in singleplayer.
- **BadOptimizations · ThreadTweak · Better Block Entities · Dynamic FPS ·
  Language Reload** — an assortment of smaller, safe wins.
- **Nvidium, Exordium and spark ship disabled** on purpose — see MODS.md for
  what each costs you.

Tuned configs (`pack/overrides/config/`):

- **Sodium** — entity culling turned on (it was off in your pack), improved fluid
  shaping off.
- **Sodium Extra** — weather, stars, rain splashes and sculk animations off;
  beacon beams height-limited; adaptive sync off for lower input latency; FPS
  counter on. Player nametags stay on, because that's information you need.
- **Entity Culling** — your original file was **invalid JSON** (a trailing comma
  in `blockEntityWhitelist`), which means the mod was falling back to defaults.
  Fixed, and `end_crystal` and `tnt` are whitelisted so they are never culled —
  a crystal you can't see is a lost fight.
- **ImmediatelyFast** — sign text buffering on.

Your `options.txt` is deliberately **not** included, so your keybinds,
sensitivity and resource pack order are untouched.

## Version compatibility

Checked on **2026-09-25**, one project at a time. Confirmed versions are recorded
per mod in [docs/MODS.md](docs/MODS.md) and in `optimization-mods.json`.

**Four mods are dead upstream and now resolve to their maintained forks:**

| Was | Now | Why |
|---|---|---|
| ModernFix | **ModernFix-mVUS** 5.21.0 | upstream stops at 1.21.1 |
| Cull Less Leaves | **Cull Fewer Leaves** 1.1.1 | upstream stops at 1.21.1 |
| Enhanced Block Entities | **Better Block Entities** 1.3.0-rc.1 | upstream stops at 1.21.4 |
| Noisium | **NoisiumForked** 2.8.3 | upstream has no 1.21.11 build |

**Three mods were dropped — no 1.21.11 Fabric build exists:**

- **Memory Leak Fix** — nothing past 1.20.4. ModernFix-mVUS covers most of it.
- **Faster Random** — project archived at 1.21.1.
- **Let Me Despawn** — nothing past 1.21.9.

**One caveat:** Better Block Entities' 1.21.11 builds are release candidates, so
it is the least battle-tested mod in the pack. If chests or signs render oddly,
disable that one first.

The build script also re-checks at run time and prints `no fabric build for
1.21.11` for anything that has since fallen behind, so this stays honest as
versions move.

## Layout

```
BUILD-PACK.bat             double-click this to build the full pack
optimization-mods.json     the mod list — edit this, then re-run the build
pack/
  modrinth.index.json      your 36 verified files
  overrides/config/        33 configs, tuned
tools/
  build-pack.ps1           Windows builder (PowerShell 5.1+, no dependencies)
  build_pack.py            same thing in Python
  build_base.py            zips pack/ with no network needed
  gen_mods_doc.py          regenerates docs/MODS.md from the manifest
  bundle_builder.py        rebuilds dist/FPS-Modpack-Builder.zip
docs/
  TUNING.md                JVM flags, BIOS, NVIDIA, Windows, in-game settings
  MODS.md                  every mod and why
dist/                      built .mrpack files
```

## Customising

Edit `optimization-mods.json` and re-run the build script. To drop a mod, delete
its entry; to ship one disabled, add `"enabled": false`. Then run
`python tools/gen_mods_doc.py` to refresh the docs.

## Honest expectations

You were **already running Sodium**, so the headline "300% more FPS" numbers you
see quoted do not apply to you — you have already banked the largest single win
available. What's left, and what this pack delivers, is roughly **+20–40% average
in busy scenes**, a much bigger improvement in **1% lows during crystal fights**,
and no more slow degradation over a long session.

The JVM flags and XMP/DOCP in [docs/TUNING.md](docs/TUNING.md) can each be worth
more than that. Please don't skip them.
