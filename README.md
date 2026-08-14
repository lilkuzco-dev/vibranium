# Vibranium

A Fabric mod for Minecraft 26.2 that adds **vibranium**, a rare purple metal, to the deep underground.

- `vibranium_ore` / `deepslate_vibranium_ore` — generate below Y16 with diamond's exact distribution shape, but **5× rarer** (every diamond batch, gated to 1-in-5 chunks; measured 5.3:1 over 450 chunks). **Diamond pickaxe or better** (iron breaks slowly and drops nothing); drops 1 **raw vibranium** with Fortune scaling like raw iron, Silk Touch drops the ore block, 3–7 XP on mine.
- **Vibranium pits** — rare geode-style hollow deposits at deepslate depths (1 per ~240 chunks, Y −55..−20): a smooth-basalt casing around a **vibranium veinstone** shell, a dense ore lining studded with raw vibranium blocks, and a crystal-lit hollow center (**vibranium crystal clusters**, light 7, drop raw vibranium — or themselves with Silk Touch). A pit holds ~40–80 vibranium (measured ~59 average).
- `vibranium_ingot` — smelt or blast the ore or raw vibranium (0.7 XP, like iron).
- `block_of_vibranium` (9 ingots ⇄ 1) and `raw_vibranium_block` (9 raw ⇄ 1), matching vanilla's metal storage blocks.
- **Weapons** — `vibranium_sword` (9 damage / 1.6 speed), `vibranium_axe` (11 / 1.0), and `vibranium_spear` (8 / 1.2 with **5.5-block thrust reach**, 7.5 lunging — vanilla spear mechanics, longer): a tier above netherite, crafted directly from ingots + sticks (no smithing), 2600 durability, diamond enchantability, anvil-repaired with ingots, fire-resistant as dropped items.
- **Tools** — `vibranium_pickaxe`, `vibranium_shovel`, `vibranium_hoe` complete the set: mining speed 11 (netherite is 9), 2600 durability, harvests everything netherite can, same enchant/repair/fireproof treatment. Vanilla recipe patterns; the spear is a diagonal of 2 ingots with a 2-stick haft.
- **Kinetic strike cycle** — every melee hit you land with a vibranium sword, axe, or spear adds a charge (cap 6, shown in the tooltip; purple shimmer builds from 4 charges, unmistakable when primed). The 7th hit detonates: an AoE burst centered on the struck target (3-block radius) that launches everything up/away with ~4 hearts of falloff bonus damage — then the cycle restarts. One swing = one charge (sweeps don't double-count); charges reset after 30 s out of combat; each weapon tracks its own count and it survives relogs.
- **Kinetic energy ball** — a throwable with ender-pearl flight and a power-20 purple explosion on impact (TNT is 4; yes, it craters — and the thrower is not immune). Crafted like TNT with ingots instead of sand (yields 8), stacks to 16.

Requires [Fabric Loader](https://fabricmc.net) 0.19.3+ and [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2. Textures are programmatic purple recolors of the corresponding vanilla textures (see tools/gen-textures.js), i.e. derivative of Mojang's art — fine inside a Minecraft mod, but not original artwork.

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
- **Combat + perk + energy ball tuning**: every number (weapon stats, charge cap/ratio, burst damage/radius/knockback/cooldown, explosion power, terrain-damage flag, stack size) lives in `VibraniumCombat.java`'s commented stat blocks.
- **Tool tier**: `src/main/resources/data/minecraft/tags/block/needs_iron_tool.json`
- **Drops / XP**: `src/main/resources/data/vibranium/loot_table/blocks/` and `UniformInt.of(3, 7)` in `VibraniumBlocks.java`
- **Textures**: regenerate with `node tools/gen-textures.js` (zero-dependency PNG generator)
- **Spawn-rate measurement**: in-game `/vibranium_census <chunkRadius>` counts vibranium and diamond (as control) around the command source; `tools/rcon.js` drives a headless dev server for testing.
