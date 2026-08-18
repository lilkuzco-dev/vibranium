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

---

# Vibranium 1.7.0 verification — armor

Date: 2026-08-17. Same target: Minecraft 26.2, Fabric Loader 0.19.3, Fabric API
0.157.0+26.2, Loom 1.17, JDK 25.

## Build and package

- `./gradlew build` — PASS, no warnings with `-Xlint:deprecation` on.
- All 123 resource JSON files parse; the four armor recipe patterns are byte-identical
  to vanilla's `diamond_helmet`/`chestplate`/`leggings`/`boots`.
- `node tools/check-equipment-assets.js --jar build/libs/vibranium-1.7.0.jar` — PASS:
  all three worn-armor layers resolve to real textures at the right sheet sizes
  (humanoid 64x32, humanoid_baby 64x64, humanoid_leggings 64x32).
- Generated armor textures read visually against their vanilla diamond sources: same
  pixel structure, purple hue, correct dimensions per source (the baby sheet is 64x64,
  not 64x32 — the generator sizes from each source's IHDR, so no truncation).

## Live dev-server battery

Carpet fake player `WardBot` on a flat platform; test mobs `NoAI` at measured distances.

- `/vibranium_gear_selftest` — PASS. Compares all 10 vibranium gear items against their
  diamond counterparts: identical enchantability value and identical membership across
  all 21 `#minecraft:enchantable/*` tags.
- Armor attributes, read live off the wearer and A/B'd against netherite on the same
  entity — vibranium **23 armor / 16.0 toughness / 0.48 knockback**, netherite
  **20 / 12.0 / 0.40**.
- Kinetic ward, 7-hit cycle — PASS. Hits 1–6 raised the shared charge 1→6 with all
  three test mobs untouched at 20.0 HP; the 7th detonated. Measured damage matched the
  tuning constants to four decimals: a mob 2.0 blocks out took 5.5104 (falloff 0.70),
  one 1.5 blocks out took 6.1008 (falloff 0.775), and the control at 6.5 blocks — past
  the 4-block radius — took exactly 0.
- Burst knockback — PASS. Velocity vectors point away from the wearer on the correct
  axis, and the vertical component is exactly `WARD_VERTICAL x falloff` (0.49 and
  0.5425 for the two mobs).
- Set-wide sharing — PASS. All four worn pieces carried the same count, were cleared
  together by the burst, and restarted together at 1 on the next hit.
- Environmental exclusion (`WARD_ATTACKS_ONLY`) — PASS. `minecraft:fall` and
  `minecraft:on_fire` damage left the charge at 1; an attacker-sourced hit raised it.
- Decay — PASS. A single charge survived +12 s and +24 s and was gone at +36 s, past
  the 600-tick window. This also confirms `inventoryTick` reaches worn armor.
- All four armor recipes load (`/recipe give` unlocked each); 1608 recipes total, zero
  errors, exceptions or warnings in the server log.

Not covered here: the worn-armor look on a real client. The equipment-asset gate proves
every layer resolves to a correctly-sized texture, and the item textures were read
directly, but nobody has seen the set on a player model.
