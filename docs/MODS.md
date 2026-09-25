# Mod reference

Minecraft 1.21.11 / fabric. Generated from `optimization-mods.json` by `tools/gen_mods_doc.py` — edit the manifest, not this file.

## Optimization mods added by the build script (18)

### Lithium  <sub>client + server</sub>

Rewrites game logic: mob AI, collisions, pathfinding and explosion raycasts. The single biggest win for crystal PvP, because popping a crystal is explosion math and Lithium makes it dramatically cheaper.

### FerriteCore  <sub>client + server</sub>

Cuts blockstate and model memory by roughly 30-50%. Important on 16GB, where you can only spare ~6GB of heap.

### ModernFix  <sub>client + server</sub>

Large bundle of memory, startup and render fixes; dynamic resource loading cuts RAM further.

### Entity Culling  <sub>client only</sub>

Async raycasting on spare 3700X cores to skip rendering and ticking entities you cannot actually see.

### More Culling  <sub>client only</sub>

Extends culling to block entities, item frames and leaves that Sodium alone still draws.

### BadOptimizations  <sub>client only</sub>

Dozens of small, safe render and tick optimizations vanilla leaves on the table.

### ThreadTweak  <sub>client + server</sub>

Tunes worker thread counts and priorities. Your 8c/16t 3700X has cores to spare; this stops them fighting the render thread.

### Dynamic FPS  <sub>client only</sub>

Throttles the game when alt-tabbed, so background CPU/GPU goes back to the foreground.

### Memory Leak Fix  <sub>client + server</sub>

Plugs known client memory leaks that turn into GC stutter over a long session.

### Faster Random  <sub>client + server</sub>

Swaps java.util.Random for a faster generator. Touches almost every hot path in the game.

### Cull Less Leaves  <sub>client only</sub>

Culls interior leaf faces. Large win in any forest.

### Enhanced Block Entities  <sub>client only</sub>

Renders chests, signs and beds as fast static models instead of per-frame block entities.

### ScalableLux  <sub>client + server</sub>

Multithreaded lighting engine. Smoother chunk loading, fewer light-update hitches.

### Let Me Despawn  <sub>client + server</sub>

Lets useless mobs despawn sooner, cutting entity counts. Singleplayer / your own server.

### C2ME  <sub>client + server</sub>

Parallel chunk loading and generation. Singleplayer only - no effect when you are on someone else's server.

### Noisium  <sub>client + server</sub>

Faster worldgen. Singleplayer only.

### Alternate Current  <sub>client + server</sub>

Much cheaper redstone implementation. Singleplayer only.

### Language Reload  <sub>client only</sub>

Faster startup and less RAM held by language data.

## Shipped but disabled (3)

Present in `mods/` with a `.disabled` suffix. Rename to remove the suffix to turn one on.

### spark  <sub>client + server</sub>

Profiler. Ships disabled. Enable it and run /spark profiler when you want to know what is actually costing you frames instead of guessing.

### Nvidium  <sub>client only</sub>

NVIDIA-only terrain renderer that can be a very large win on an RTX 2060 Super. Ships DISABLED because it is reported broken against Sodium 0.8.x on 1.21.11. Try it last, and disable it again if you crash or see missing chunks.

### Exordium  <sub>client only</sub>

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

## Removed

- **ClickCrystals** — an automation client, not a performance mod, and a ban risk on any server that checks. It was already disabled in your pack.
- **Meteor, Xenon, Prestige, Nova, Marlowww, zerio, glazed, 4e Client+** — cheat clients that were sitting disabled in your `overrides/mods/`. None of them affect FPS, so none were carried over.
- **`config/imnotcheatingyouare/`, `config/ExploitPreventer.json`** — leftover config from the above.
