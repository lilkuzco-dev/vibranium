# Third-party art credits

## TechReborn (MIT)

Several machine textures in this mod are hue-shifted derivatives of artwork
from [TechReborn](https://github.com/TechReborn/TechReborn), used under the
MIT license (Copyright (c) 2020 TechReborn). The transformation is performed
by `tools/gen-textures.js`, which pulls the source PNGs from a TechReborn
checkout and remaps their hues to purple; no TechReborn code is copied.

Derived textures (source → shipped file, all under
`assets/vibranium/textures/block/`):

| TechReborn source | Vibranium texture |
| --- | --- |
| `block/machines/structure/advanced_machine_casing.png` | `vibranium_fabricator_side.png` |
| `block/machines/tier1_machines/auto_crafting_table_top.png` | `vibranium_fabricator_top.png` |
| `block/machines/tier0_machines/machine_bottom.png` | `vibranium_fabricator_bottom.png` |
| `block/machines/structure/basic_machine_casing.png` | `vibranium_extractor_side.png` |
| `block/machines/tier0_machines/machine_top.png` | `vibranium_extractor_top.png` |
| `block/machines/tier1_machines/grinder_front_off.png` | `vibranium_extractor_drill.png` |
| `block/machines/tier1_machines/grinder_front_on.png` (+ its animation `.mcmeta`) | `vibranium_extractor_drill_on.png` |

MIT license text: https://github.com/TechReborn/TechReborn/blob/26.2/LICENSE.md

## Mojang (vanilla Minecraft)

All other textures are hue-shifted or recomposed derivatives of vanilla
Minecraft textures (see the job list in `tools/gen-textures.js`), which is
normal for Minecraft mods but means they are not original artwork. GUI
textures are composed from regions of `crafting_table.png`, `furnace.png`,
and the furnace progress sprites.

TechReborn's machine code (block entities, screen handlers) was studied as a
reference for Fabric 26.2 idioms; the Java in this mod is written fresh
against vanilla + Fabric API, with no TechReborn source files copied.
