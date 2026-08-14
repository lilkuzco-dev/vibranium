# Vibranium

A Fabric mod for Minecraft 26.2 that adds **vibranium**, a rare purple metal, to the deep underground.

- `vibranium_ore` / `deepslate_vibranium_ore` — generate below Y16 with diamond's exact distribution shape, but **5× rarer** (every diamond batch, gated to 1-in-5 chunks; measured 5.3:1 over 450 chunks). Iron pickaxe or better; drops 1 **raw vibranium** with Fortune scaling like raw iron, Silk Touch drops the ore block, 3–7 XP on mine.
- `vibranium_ingot` — smelt or blast the ore or raw vibranium (0.7 XP, like iron). Future tools/weapons will craft from ingots.
- `block_of_vibranium` (9 ingots ⇄ 1) and `raw_vibranium_block` (9 raw ⇄ 1), matching vanilla's metal storage blocks.

Requires [Fabric Loader](https://fabricmc.net) 0.19.3+ and [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2. All textures are original, generated pixel art.

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
- **Tool tier**: `src/main/resources/data/minecraft/tags/block/needs_iron_tool.json`
- **Drops / XP**: `src/main/resources/data/vibranium/loot_table/blocks/` and `UniformInt.of(3, 7)` in `VibraniumBlocks.java`
- **Textures**: regenerate with `node tools/gen-textures.js` (zero-dependency PNG generator)
- **Spawn-rate measurement**: in-game `/vibranium_census <chunkRadius>` counts vibranium and diamond (as control) around the command source; `tools/rcon.js` drives a headless dev server for testing.
