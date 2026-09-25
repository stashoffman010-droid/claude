# Tuning for Ryzen 7 3700X / RTX 2060 Super / 16GB RAM

Minecraft 1.21.11, Fabric. Do these in order — 1 and 2 are the biggest wins
outside the mods themselves, and most people never touch them.

## 1. RAM allocation and JVM flags

**Java 21 is required** for anything past 1.20.5, so make sure your launcher is
using a Java 21+ runtime (Modrinth App and Prism both download one for you).

The pack pins Fabric Loader `0.18.4`, the version your original pack was built
against, so nothing changes underneath you. If your launcher offers a newer
loader for 1.21.11, taking it is fine and usually slightly faster.

Allocate **6 GB, not more.** This is the single most common mistake. Your 16 GB
has to also cover Windows (~3–4 GB), your browser, and Discord. More heap does
not mean more FPS — it means *longer garbage-collection pauses*, which you feel
as stutter in the middle of a fight. 6 GB is comfortable for this pack.

Paste these into your launcher's JVM arguments (Modrinth App: Options →
Java; Prism: Instance → Settings → Java):

```
-Xms6G -Xmx6G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=37 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1
```

`-Xms6G` equal to `-Xmx6G` plus `-XX:+AlwaysPreTouch` claims the heap up front,
so the game never pauses to grow it mid-fight. Startup takes a second or two
longer; that is the whole cost.

**Optional, if you still feel GC stutter:** swap G1 for the generational
low-pause collector. Slightly less raw throughput, near-zero pauses:

```
-Xms6G -Xmx6G -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch -XX:+DisableExplicitGC
```

Use one set or the other, never both.

## 2. Turn on XMP/DOCP in your BIOS

Minecraft is unusually sensitive to memory latency, and the Ryzen 7 3700X's
Infinity Fabric scales directly with RAM speed. If your RAM is running at its
stock 2133 MHz because XMP was never enabled, you are leaving **10–20% FPS** on
the table — more than most of the mods in this pack will give you.

Check it: Task Manager → Performance → Memory → Speed. If it says 2133 MHz and
your kit is rated 3200 or 3600, reboot into BIOS and enable **XMP** (Intel
branding) or **DOCP** (ASUS) or **EXPO/AMP** and pick your kit's rated profile.
3600 MHz CL16 is the sweet spot for a 3700X. Save, reboot, re-check.

## 3. NVIDIA Control Panel

Right-click desktop → NVIDIA Control Panel → **Manage 3D settings** →
**Program Settings** → add `javaw.exe` (your launcher's Java 21 runtime — the
launcher's settings show the path).

| Setting | Value | Why |
|---|---|---|
| Power management mode | **Prefer maximum performance** | Stops the 2060 Super downclocking between frames |
| Low Latency Mode | **Ultra** | Cuts a frame or two of input lag. Worth a lot in crystal PvP |
| Vertical sync | **Off** | VSync adds 1–2 frames of latency |
| Shader Cache Size | **10 GB** or Unlimited | Big one for stutter — stops shader recompiles |
| Texture filtering – Quality | **High performance** | Free frames, invisible in Minecraft |
| Antialiasing – Mode | **Off / application-controlled** | Minecraft does not use it |
| Threaded optimization | **On** | Lets the driver use your spare 3700X cores |
| Max Frame Rate | **Off** | Unless you run G-Sync (then cap 3 below refresh) |

Then in **Change resolution**, confirm your monitor is actually running at its
full refresh rate — Windows silently resets this to 60 Hz after some driver
updates.

## 4. Windows

- **Game Mode**: Settings → Gaming → Game Mode → **On**
- **Hardware-accelerated GPU scheduling**: Settings → Display → Graphics →
  Advanced → **On**
- **Per-app GPU preference**: Settings → Display → Graphics → add `javaw.exe` →
  **High performance**
- **Power plan**: Control Panel → Power Options → **High performance**
- **Xbox Game Bar / background recording**: **Off**. Game Bar's capture costs
  real frames.
- **Close Chrome before you play.** On 16 GB with 6 GB going to Java, browser
  tabs are what push you into swapping, and swapping is what causes the
  multi-second freezes people blame on the game.
- Keep your NVIDIA driver current, but if a new driver tanks your FPS, roll back —
  it happens.

## 5. In-game video settings

After the pack is installed. **Options → Video Settings**:

| Setting | Value | Note |
|---|---|---|
| Render distance | **8** on servers, **12** singleplayer | Above 12 costs a lot for almost nothing |
| Simulation distance | **5** | Ignored on servers anyway |
| Max framerate | **Unlimited** | With VSync off, this is the lowest-latency option |
| VSync | **Off** | |
| Graphics | **Fast** | |
| Clouds | **Off** | Free frames |
| Particles | **Minimal** | Big during crystal fights |
| Smooth lighting | **Off** | Noticeable win, mild visual cost |
| Entity shadows | **Off** | |
| Entity distance | **50%** | Large win in crowded fights |
| Biome blend | **Off** | Makes chunk rebuilds much cheaper |
| Mipmap levels | **4** | No real cost on a 2060 Super |
| Fullscreen | **On** | Better frame pacing than windowed |

**Options → Video Settings → Sodium** is already configured by this pack, so you
should not need to touch it. If you want to look: entity culling and fog
occlusion are on, and Reese's Sodium Options gives you the nicer menu.

## 6. Two things in your old pack that cost you frames

- **`Crystal PvP LT3 Essentials (1M Edition)`** is a 75 MB resource pack. High
  resolution textures eat VRAM and texture bandwidth. Your 8 GB of VRAM can take
  it, but if you want the last few percent, use the small `PvP Essentials` pack
  instead and keep the big one off.
- **Entity Model Features** re-implements entity model loading to allow custom
  models. It is a cosmetic mod with a real per-entity cost. If you are not
  actually using custom entity models, remove it.

## 7. What to actually expect

Be a little skeptical of "300% FPS" claims — those are measured against *vanilla*.
You were already running Sodium, Sodium Extra and ImmediatelyFast, so you have
already collected the single largest chunk of the available gain.

What this pack adds on top of where you were:

- **Average FPS**: roughly **+20–40%** in busy scenes, mostly from the culling
  mods (Entity Culling, More Culling, Cull Less Leaves) and ScalableLux.
- **Crystal fights specifically**: the biggest change. Lithium rewrites
  explosion raycasting, which is exactly the code that runs when a crystal pops.
  This is a frame-*time* fix — your 1% lows stop collapsing when four people
  are crystalling at once.
- **Long sessions**: FerriteCore and Memory Leak Fix mean hour-three feels like
  hour-one instead of degrading into GC stutter.
- **Singleplayer world loading**: much faster, from C2ME and Noisium. These do
  nothing on someone else's server.

## 8. If something breaks

Everything here is reversible.

1. **Crash on startup** → check `logs/latest.log` for the mod named in the
   stack trace, rename that jar to `<name>.jar.disabled`, launch again.
2. **Missing or flickering chunks** → you enabled Nvidium. Disable it. It is
   shipped disabled for exactly this reason.
3. **HUD feels laggy** → you enabled Exordium. Disable it.
4. **Want to know what is actually slow** → enable `spark`, then in game run
   `/spark profiler --timeout 60` and open the link it gives you. Measure
   before you change anything else.
5. **Reset a config** → delete the file in `config/` and relaunch; the mod
   writes a fresh default.
