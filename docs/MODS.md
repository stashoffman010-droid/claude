# Mod reference

Minecraft 1.21.11 / fabric. Generated from `optimization-mods.json` by `tools/gen_mods_doc.py` — edit the manifest, not this file.

Every mod here was checked against Modrinth for a 1.21.11 Fabric build on **2026-09-25**. Three candidates were dropped because no such build exists, and four resolved to maintained forks — see the bottom of this file.

## Optimization mods added by the build script (15)

### Lithium  <sub>client + server</sub>

Verified: **0.21.1 (1.21.11 / fabric)**

Rewrites game logic: mob AI, collisions, pathfinding and explosion raycasts. The single biggest win for crystal PvP, because popping a crystal is explosion math and Lithium makes it dramatically cheaper.

### FerriteCore  <sub>client + server</sub>

Verified: **8.2.0-fabric (1.21.11 / fabric)**

Cuts blockstate and model memory by roughly 30-50%. Important on 16GB, where you can only spare ~6GB of heap.

### ModernFix-mVUS  <sub>client + server</sub>

Verified: **5.21.0 (1.21.11 / fabric)**

Large bundle of memory, startup and render fixes; dynamic resource loading cuts RAM further. Upstream ModernFix stops at 1.21.1; mVUS is the maintained fork that carries it to 1.21.11.

### Entity Culling  <sub>client only</sub>

Verified: **1.10.2 (1.21.11 / fabric)**

Async raycasting on spare 3700X cores to skip rendering and ticking entities you cannot actually see.

### More Culling  <sub>client only</sub>

Verified: **1.6.1 (1.21.11 / fabric)**

Extends culling to block entities, item frames and leaves that Sodium alone still draws.

### BadOptimizations  <sub>client only</sub>

Verified: **2.4.1 (1.21.11 / fabric)**

Dozens of small, safe render and tick optimizations vanilla leaves on the table.

### ThreadTweak  <sub>client + server</sub>

Verified: **0.1.8 (1.21.11 / fabric)**

Tunes worker thread counts and priorities. Your 8c/16t 3700X has cores to spare; this stops them fighting the render thread.

### Dynamic FPS  <sub>client only</sub>

Verified: **3.11.2 (1.21.11 / fabric)**

Throttles the game when alt-tabbed, so background CPU/GPU goes back to the foreground.

### Cull Fewer Leaves  <sub>client only</sub>

Verified: **1.1.1 (1.21.11 / fabric)**

Culls interior leaf faces. Large win in any forest. Cull Less Leaves stopped at 1.21.1; Cull Fewer Leaves is its continuation.

### Better Block Entities  <sub>client only</sub>

Verified: **1.3.0-rc.1+1.21.11 (1.21.11 / fabric)**

Renders chests, signs and beds as fast static models instead of per-frame block entities. Enhanced Block Entities stopped at 1.21.4; Better Block Entities is the active equivalent. Its 1.21.11 builds are release candidates, so this is the least battle-tested mod in the pack - disable it first if chests or signs render oddly.

### ScalableLux  <sub>client + server</sub>

Verified: **0.1.6 (1.21.11 / fabric)**

Multithreaded lighting engine. Smoother chunk loading, fewer light-update hitches.

### C2ME  <sub>client + server</sub>

Verified: **0.3.6.0.0 (1.21.11 / fabric)**

Parallel chunk loading and generation. Singleplayer only - no effect when you are on someone else's server.

### NoisiumForked  <sub>client + server</sub>

Verified: **2.8.3+mc1.21.11 (1.21.11 / fabric)**

Faster worldgen. Singleplayer only. Upstream Noisium has no 1.21.11 build; NoisiumForked does.

### Alternate Current  <sub>client + server</sub>

Verified: **mc1.21.11-1.9.0 (1.21.11 / fabric)**

Much cheaper redstone implementation. Singleplayer only.

### Language Reload  <sub>client only</sub>

Verified: **1.7.7+1.21.11 (1.21.11 / fabric)**

Faster startup and less RAM held by language data.

## Shipped but disabled (3)

Present in `mods/` with a `.disabled` suffix. Rename to remove the suffix to turn one on.

### spark  <sub>client + server</sub>

Verified: **1.10.156 (1.21.11 / fabric)**

Profiler. Ships disabled. Enable it and run /spark profiler when you want to know what is actually costing you frames instead of guessing.

### Nvidium  <sub>client only</sub>

Verified: **supports 1.21.6-1.21.11 (1.21.11 / fabric)**

NVIDIA-only terrain renderer that can be a very large win on an RTX 2060 Super. Ships DISABLED because it is reported broken against Sodium 0.8.x on 1.21.11. Try it last, and disable it again if you crash or see missing chunks.

### Exordium  <sub>client only</sub>

Verified: **supports 1.21.10-1.21.11 (1.21.11 / fabric)**

Draws the HUD at a lower framerate than the world. Free FPS, but it adds visible HUD latency, which is bad for PvP. Ships disabled; enable only if you are still short on frames.

## Carried over from your original pack (33)

Your PvP and quality-of-life mods, kept at the exact versions you were already running — the hashes come straight out of your own `FPS_Modpack_1.0.0.mrpack`.

- AnchorOptimizer
- appleskin fabric mc
- ClientSideCrystals
- cloth config
- Clumps fabric
- collective
- crafter presets
- crosshairindicator
- dynamiccrosshair
- fabric api
- fast ip ping
- fastquit
- freelook
- herosanchoroptimizer
- ImmediatelyFast Fabric
- inventorytotem
- krypton
- Marlow Crystal Optimizer
- modmenu
- MouseTweaks fabric mc
- reeses sodium options fabric
- shulkerboxtooltip fabric
- sodium extra fabric
- sodium fabric
- tiers
- totemtweaks
- ukulib
- ukus armor hud
- voicechat fabric
- whoami
- yet_another_config_lib

Left disabled, as they were in your pack:

- Heart Indicator
- iris fabric

## Resource packs

- Crystal PvP LT3 Essentials (1M Edition).zip
- Flaming Swords.zip
- PvP Essentials.zip

None are enabled by default — this pack deliberately ships no `options.txt`, so your keybinds, sensitivity and resource pack order stay exactly as they are. Turn them on in Options → Resource Packs.

## Dropped: no 1.21.11 Fabric build

- **Memory Leak Fix** — nothing past 1.20.4. ModernFix-mVUS covers most of the same ground.
- **Faster Random** — project archived at 1.21.1.
- **Let Me Despawn** — nothing past 1.21.9.

## Resolved to a maintained fork

- **ModernFix** → **ModernFix-mVUS**: upstream stops at 1.21.1.
- **Cull Less Leaves** → **Cull Fewer Leaves**: upstream stops at 1.21.1.
- **Enhanced Block Entities** → **Better Block Entities**: upstream stops at 1.21.4.
- **Noisium** → **NoisiumForked**: upstream has no 1.21.11 build.

## Removed

- **ClickCrystals** — an automation client, not a performance mod, and a ban risk on any server that checks. It was already disabled in your pack.
- **Meteor, Xenon, Prestige, Nova, Marlowww, zerio, glazed, 4e Client+** — cheat clients that were sitting disabled in your `overrides/mods/`. None of them affect FPS, so none were carried over.
- **`config/imnotcheatingyouare/`, `config/ExploitPreventer.json`** — leftover config from the above.
