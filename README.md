# Vibranium

A Fabric mod for Minecraft 26.2 that adds two metals to the deep underground: **vibranium**, a rare purple
metal a tier above netherite, and **godite**, a rainbow metal five times rarer again and a tier above that.

- `vibranium_ore` / `deepslate_vibranium_ore` — generate below Y16 with diamond's exact distribution shape, but **5× rarer** (every diamond batch, gated to 1-in-5 chunks; measured 5.3:1 over 450 chunks). **Diamond pickaxe or better** (iron breaks slowly and drops nothing); drops 1 **raw vibranium** with Fortune scaling like raw iron, Silk Touch drops the ore block, 3–7 XP on mine.
- **Vibranium pits** — rare geode-style hollow deposits at deepslate depths (1 per ~240 chunks, Y −55..−20): a smooth-basalt casing around a **vibranium veinstone** shell, a dense ore lining studded with raw vibranium blocks, and a crystal-lit hollow center (**vibranium crystal clusters**, light 7, drop raw vibranium — or themselves with Silk Touch). A pit holds ~40–80 vibranium (measured ~59 average).
- `vibranium_ingot` — smelt or blast the ore or raw vibranium (0.7 XP, like iron).
- `block_of_vibranium` (9 ingots ⇄ 1) and `raw_vibranium_block` (9 raw ⇄ 1), matching vanilla's metal storage blocks.
- **Weapons** — `vibranium_sword` (9 damage / 1.6 speed), `vibranium_axe` (11 / 1.0), and `vibranium_spear` (8 / 1.2 with **5.5-block survival thrust reach**, 7.5 in creative — vanilla spear mechanics, longer): a tier above netherite, crafted directly from ingots + sticks (no smithing), 2600 durability, diamond enchantability, anvil-repaired with ingots, fire-resistant as dropped items.
- **Tools** — `vibranium_pickaxe`, `vibranium_shovel`, `vibranium_hoe` complete the set: mining speed 11 (netherite is 9), 2600 durability, harvests everything netherite can, same enchant/repair/fireproof treatment. Vanilla recipe patterns; the spear is a diagonal of 2 ingots with a 2-stick haft.
- **Armor** — `vibranium_helmet` / `vibranium_chestplate` / `vibranium_leggings` / `vibranium_boots`, crafted from ingots in the vanilla armor patterns. A tier above netherite and slightly more protective on every axis: **23 armor points** (netherite 20), **16 armor toughness** (12), **0.48 knockback resistance** (0.40), and 495/720/675/585 durability (407/592/555/481). Same diamond enchantability, ingot anvil repair, trimmable, and fire-resistant as dropped items.
- **Kinetic strike cycle** — every melee hit you land with a vibranium sword, axe, or spear adds a charge (cap 6, shown in the tooltip; purple shimmer builds from 4 charges, unmistakable when primed). The 7th hit detonates: an AoE burst centered on the struck target (3-block radius) that launches everything up/away with ~4 hearts of falloff bonus damage — then the cycle restarts. One swing = one charge (sweeps don't double-count); charges reset after 30 s out of combat; each weapon tracks its own count and it survives relogs.
- **Kinetic ward cycle** — the strike cycle in reverse, on the armor: every hit you *take* while wearing vibranium adds a charge (cap 6, shown in the tooltip, purple shimmer from 4). The 7th hit received detonates a purple energy burst centered on **you** — a 4-block radius that damages and launches everything around you (~4 hearts at the center, falling off to the edge; you are never caught in your own burst) — then the cycle restarts. Any number of pieces participate and they share one count, so a full set is one cycle, not four. Only hits with an attacker behind them count (fall and fire damage do not); charges reset after 30 s without being hit, and a ward burst never chains into another one.
- **Kinetic energy ball** — a throwable with ender-pearl flight and a power-20 purple explosion on impact (TNT is 4; yes, it craters — and the thrower is not immune). Crafted like TNT with ingots instead of sand (yields 8), stacks to 16.
- **Vibranium Fabricator** — an advanced crafting station with a **5×5 grid** and its own data-driven recipe type (`vibranium:fabricating`, JSON like vanilla shaped recipes but up to 5×5 — see `data/vibranium/recipe/vibranium_extractor.json`). Crafted on a regular crafting table: 4 ingots in the corners, veinstone left/right, obsidian top/bottom, a crafting table in the middle. Placed and used like a crafting table — no fuel, grid contents return on close. Fabricating recipes only work here; the vanilla table can't see them.
- **Vibranium Extractor** — the first automated machine, craftable **only at the fabricator**. Burns standard furnace fuels to mine a 3×3 column straight down (1 block / 2 s; a **vibranium ingot as fuel runs 4× speed** for its burn), collecting normal diamond-tier drops (no silk/fortune, mines vibranium ore) into an 18-slot buffer. Auto-pushes into an adjacent chest/barrel; hoppers can insert fuel from above/sides and extract output from below. Halts with the reason shown in its GUI: lava or water sources in the path (resumes if cleared), unbreakable blocks/bedrock (stops), inventory full (waits). The animated drill face glows while running.

## Godite

A tier above vibranium, and deliberately a small one — every offensive and defensive number is
**exactly 10% higher**, derived from vibranium's rather than retyped, so the gap cannot drift.
What you actually buy godite for is the launch.

- `godite_ore` / `deepslate_godite_ore` — the same distribution shape as vibranium's, gated **5× harder
  again**: 1-in-25 chunks against diamond's every chunk. Measured over 1,323 fresh chunks: 0.879
  godite/chunk against 4.60 vibranium and 22.4 diamond — **5.2 : 1 vibranium-to-godite** and 25.5 : 1
  diamond-to-godite (per-region 4.4–5.7 : 1; ore is clumpy, so a single region is not the number).
  Diamond pickaxe or better, drops **raw godite** with Fortune scaling, 3–7 XP. No pits — those stay
  vibranium's landmark.
- `godite_ingot`, `block_of_godite` (9 ingots ⇄ 1) and `raw_godite_block`, all mirroring the vibranium set.
- **Weapons** — `godite_sword` (**9.9** damage / 1.6 speed), `godite_axe` (**12.1** / 1.0), `godite_spear`
  (**8.8** / 1.2, same 5.5-block reach as vibranium's). Attack speed and reach are *unchanged*: those are
  not strength, and scaling them would be a far bigger buff than 10%.
- **Tools** — `godite_pickaxe`, `godite_shovel`, `godite_hoe`: mining speed **12.1** (vibranium 11),
  **2860** durability, everything vibranium can harvest.
- **Armor** — the four pieces at **25 armor points** (vibranium 23), **17.6 toughness** (16.0), **0.528
  knockback resistance** (0.48), 550/800/750/650 durability. Armor points and durability are integer
  knobs, so they land on 25 (from 25.3) and +11.1% (45→50 multiplier) — the closest the scale allows,
  and `/godite_selftest` asserts exactly those values rather than waving them through.
- **The launch** — godite's own mechanic, and the reason to want it. Both kinetic cycles fire on **every
  5th hit** instead of every 7th, and instead of vibranium's shove they launch for `11.0` horizontally
  with `1.6` of lift. On the sword that is **every 5th hit you land**; on the armor, **every 5th hit you
  take**. Measured on a dev server: a zombie at the center of the burst travels **124.6 blocks** before it
  comes down (one caught 1.5 blocks off-center still went 98.8). Minecraft's air drag is 0.91/tick, so
  total travel converges to `v0 × 11.11` — three digits is simply not reachable below `v0 ≈ 9`, and the
  lift exists to buy the ~45 ticks of air needed to spend it. Bonus burst damage is 8.8 at the center,
  falling off to the edge, and you are never caught in your own.
- **Mixed sets ward as the weakest piece worn.** A godite chestplate over vibranium leggings wards like
  vibranium — 7 hits, vibranium's shove. One godite boot cannot buy a godite ward.
- **Rainbow** — godite is vibranium's texture pipeline with the hue swept across each piece instead of
  held at 270°: one full turn of the wheel from one end of a blade, ingot or armor piece to the other,
  equalized so every band of the spectrum gets the same share of pixels. Ore specks come out individually
  colored; the tooltip's charge line is drawn in the same sweep.

Requires [Fabric Loader](https://fabricmc.net) 0.19.3+ and [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2. Textures are programmatic recolors of vanilla textures (purple for vibranium, a swept rainbow for godite) and (for the machines) of MIT-licensed [TechReborn](https://github.com/TechReborn/TechReborn) art — see `tools/gen-textures.js` and [CREDITS.md](CREDITS.md); derivative art, fine inside a Minecraft mod, but not original artwork.

## Install

Grab the jar from the [releases page](https://github.com/lilkuzco-dev/vibranium/releases) and drop it into your `mods/` folder — or, if you're in the friend group, just run the [mod-installer](https://github.com/lilkuzco-dev/mod-installer) as usual: the shared manifest includes vibranium via its `extra_mods` list.

## Development

Standard Fabric Loom project (Mojang mappings). Requires JDK 25.

```sh
./gradlew build          # jar lands in build/libs/
./gradlew runClient      # dev client
```

Handy knobs and tools:

- **Ore rarity**: constants at the top of `tools/gen-worldgen.js` (`DENSITY_DIVISOR` + per-batch table). Run `node tools/gen-worldgen.js`, then rebuild.
- **Godite tuning**: `GoditeCombat.java` — one `STRENGTH_MULTIPLIER` and the launch constants; everything
  else is derived from `VibraniumCombat`, so retuning vibranium retunes godite and keeps the gap at 10%.
- **Godite ore rarity**: `GODITE_DIVISOR` in `tools/gen-worldgen.js` (godite is that many times rarer than
  vibranium, which is `DENSITY_DIVISOR` times rarer than diamond). Re-run the script, rebuild.
- **Godite self-test**: in-game `/godite_selftest` asserts, headlessly, that every godite stat is exactly
  10% above its vibranium counterpart (read off the registered items, not the constants), that attack
  speed did *not* move, that both cycles detonate on the 5th hit while vibranium's still detonate on the
  7th, and that the launch clears 100 blocks under vanilla's own drag model.
- **Combat + armor + ward + energy ball tuning**: every number (weapon stats, armor defense/toughness/knockback/durability, strike and ward charge caps, burst damage/radius/knockback/decay, explosion power, terrain-damage flag, stack size) lives in `VibraniumCombat.java`'s commented stat blocks.
- **Extractor tuning**: mine rate, column size, vibranium fuel duration + speed multiplier, chest-push cadence — the commented block at the top of `VibraniumMachines.java`.
- **Gear self-test**: in-game `/vibranium_gear_selftest` asserts, headlessly, that every vibranium tool, weapon and armor piece is enchantable *exactly* like its diamond counterpart (enchantability value **and** membership in every `#minecraft:enchantable/*` tag — the silent failure mode for custom gear), that the armor set out-protects netherite, and that the ward is wired to all four pieces.
- **Equipment-asset gate**: `node tools/check-equipment-assets.js [--jar <path>]` asserts every worn-armor layer resolves to a real texture of the right sheet size. Worn armor pointing at a missing texture renders as nothing, with no crash and no server-side symptom.
- **Recipe self-test**: in-game `/vibranium_fabricator_selftest` asserts the fabricating recipe wiring (exact/perturbed/vanilla-isolation/table recipe) headlessly.
- **Ore tool tier**: `src/main/resources/data/minecraft/tags/block/needs_diamond_tool.json`
- **Drops / XP**: `src/main/resources/data/vibranium/loot_table/blocks/` and `UniformInt.of(3, 7)` in `VibraniumBlocks.java`
- **Textures**: regenerate with `node tools/gen-textures.js` (zero-dependency PNG generator)
- **Spawn-rate measurement**: in-game `/vibranium_census <chunkRadius>` counts vibranium and diamond (as control) around the command source; `tools/rcon.js` drives a headless dev server for testing.
