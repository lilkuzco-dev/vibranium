# Vibranium 1.6.1 verification

Date: 2026-08-16. Target: Minecraft 26.2, Fabric Loader 0.19.3,
Fabric API 0.157.0+26.2, Loom 1.17, Gradle 9.5.1, JDK 25.

## Build and launch

- `./gradlew clean build` — PASS with deprecation lint enabled and zero warnings.
- Dedicated server — PASS on a fresh world; registries, 1,604 recipes, 1,696
  advancements, and 275 biome modifications loaded without a Vibranium error.
- Client — PASS through resource reload and title-screen startup.
- Resource and package inspection — PASS: JSON parses, model and texture references
  resolve, and the remapped JAR passes ZIP integrity inspection.

## Live-world battery

Carpet fake players were used only as test drivers; Carpet is not a production
dependency.

- Fabricator recipe self-test — PASS 4/4: exact 5x5 recipe, perturbed recipe
  rejection, vanilla-table isolation, and a normal 3x3 recipe in the fabricator.
- World generation — PASS over 441 chunks: 2,275 vibranium ore and 11,302 diamond
  ore, exactly a 5.0:1 diamond-to-vibranium ratio; pits produced veinstone, ore, and
  raw-vibranium blocks.
- Harvest tier — PASS: diamond pickaxe drops raw vibranium; iron is too weak and
  drops nothing.
- Extractor mining — PASS: normal drops enter its buffer, source water halts with
  the correct status, clearing the obstruction resumes work, and adjacent barrels
  receive auto-pushed output.
- Extractor full-buffer behavior — PASS: the target block remains untouched while
  all output slots are full, then mining resumes without loss after one slot clears.
- Extractor save/restart — PASS: fuel, progress, mining depth, inventory, and active
  work all persisted and resumed after a full server restart.
- Kinetic strike cycle — PASS: six hits store charge, the seventh resets it and deals
  the configured bonus damage and radial knockback; charge state survives restart
  and expires after its 30-second inactivity window.
- Spear — PASS for base damage and kinetic charge integration. Its item uses the
  vanilla 26.2 kinetic/piercing spear components with 5.5 survival reach and 7.5
  creative reach; an audit corrected the previous documentation that mislabeled
  creative reach as a separate lunge range.
- Kinetic energy ball — PASS: projectile impact destroys terrain and completes
  without a crash.

## Reliability fixes made by this audit

- Replaced a deprecated entity damage call with the supported server-level damage
  API and enabled compiler deprecation lint.
- Added the missing translated name for the Vibranium repair-material item tag.
- Corrected stale tool-tier references to the actual diamond-tier mining tag.
- Corrected the spear reach constant and documentation to match Minecraft's
  `AttackRange` semantics.

No known launch, progression, persistence, crafting, world-generation, or machine
blocker remains after this battery. As with any game mod, this is a tested result,
not a guarantee that no edge-case defect can ever exist.
