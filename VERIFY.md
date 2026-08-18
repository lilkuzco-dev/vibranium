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

---

# Vibranium 1.8.0 verification — godite

Date: 2026-08-17. Same target: Minecraft 26.2, Fabric Loader 0.19.3, Fabric API
0.157.0+26.2, Loom 1.17, JDK 25.

## Build and package

- `./gradlew clean build` — PASS, no warnings with `-Xlint:deprecation` on.
- All 204 resource JSON files parse.
- `node tools/check-equipment-assets.js --jar build/libs/vibranium-1.8.0.jar` — PASS: both
  equipment assets, six layers, all at the right sheet sizes (humanoid 64x32, humanoid_baby
  64x64, humanoid_leggings 64x32).
- Regenerating textures leaves every vibranium PNG byte-identical: godite is the same
  pipeline with a hue function instead of a constant, and it did not disturb the old art.
- The generated godite textures were read as images, not just checked for existence. The
  first attempt was wrong in a way no existence check could catch: a naive `x+y` gradient
  runs *along* a sword blade's diagonal, so the whole blade came out one flat colour. The
  sweep now runs along `x-y` and is equalised over the pixels that actually move.

## Headless self-test

`/godite_selftest` — PASS. Asserted against the registered items, not the constants:

- Displayed damage exactly x1.10 on all six weapons/tools (sword 9.00 -> 9.90, axe
  11.00 -> 12.10), and attack speed **unchanged** on every one of them.
- Durability: tools exactly x1.10 (sword 2600 -> 2860). Armour durability is
  `multiplier x per-slot base`, so it can only move in whole multiplier steps: 45 x 1.10 =
  49.5 rounds to 50, landing the pieces at +11.1%. The test asserts that value rather than
  waving it through — it is what caught the discrepancy in the first place.
- Full set 25 armor / 17.6 toughness / 0.528 knockback against vibranium's 23 / 16.0 / 0.48.
  Toughness and knockback are the exact 10%; armour points are integers, so 25.3 lands on 25.
- Mining speed 11.0 -> 12.1; enchantability unchanged at 10, and identical membership across
  all 21 `#minecraft:enchantable/*` tags.
- Both godite cycles detonate on the 5th hit, both vibranium cycles still on the 7th.
- All 10 godite items repair with the godite ingot and **not** with the vibranium one.

## Live dev-server battery

Carpet fake players as drivers. Test mobs pinned with `movement_speed 0` (a `NoAI` mob never
integrates velocity, so it cannot be launched at all) and `fall_damage_multiplier 0` so they
survive the landing and can still be read. Launches were run from a platform at Y 250 over
open air, with the flight path force-loaded.

- **Strike cycle** — PASS. The sword's charge read 1, 2, 3, 4 on the first four hits; the 5th
  detonated and cleared the component entirely. Damage on the detonating hit was 18.40 total
  against a 400 HP zombie: 9.7416 weapon + 8.6592 burst, which is exactly the configured 9.9
  and 8.8 after a zombie's 2 points of natural armour (x0.984). Four decimals, both terms.
- **Strike launch** — PASS, and this is the headline number. Measured velocity one tick after
  the burst: **11.16 horizontal, 1.91 vertical**; successive samples fell by exactly 0.910 per
  tick, confirming vanilla's air-drag model. The zombie travelled from x=1007.0 to x=1131.61 —
  **124.6 blocks** — before coming down.
- **Ward cycle** — PASS. Four hits taken raised the shared charge 1->4 across all four worn
  pieces; the 5th detonated and cleared all four together. The attacker, 1.5 blocks out, took
  6.888 damage — exactly `8.8 x falloff(0.79545) x 0.984` — and was launched from x=1007.0 to
  x=1105.80, **98.8 blocks**.
- **Ward radius control** — PASS, and found by accident, which is the best kind. An earlier run
  had the wearer drift to 5.5 blocks from the attacker, past the 4.4-block radius: the burst
  fired, cleared the charge, and the attacker took exactly 0 damage and moved exactly 0 blocks.
- **Mixed sets** — PASS. Three godite pieces under a vibranium helmet warded on **vibranium's**
  profile: charges climbed 1..6 and detonated on the 7th hit, not the 5th, and the attacker
  flew 15.4 blocks rather than a hundred. The weakest piece worn decides.
- **Vibranium regression** — PASS. The shared-machinery refactor did not move vibranium: its
  sword still charges 1..6 and detonates on the 7th.
- **World generation** — PASS over 1,323 fresh chunks in three widely separated regions:
  6,091 vibranium, 1,163 godite, 29,694 diamond. **5.2 : 1 vibranium-to-godite** and 25.5 : 1
  diamond-to-godite against a configured 25. Per-region ratios were 5.7, 4.4 and 5.7 : 1 — ore
  is clumpy and a single region is not the number, the same lesson vibranium's own tuning
  recorded. (Vibranium's count includes pit ore, which godite has no equivalent of, so the
  pooled ratio is if anything slightly flattering to vibranium.)
- Zero errors, exceptions or warnings in the server log across the whole battery.

## Bug found and fixed by this battery

**The strike cycle's launch was being silently halved, and had been since it shipped.**
`Player.attack` applies its own knockback to the entity it struck, *after* `Item.hurtEnemy` —
which is where the strike cycle runs — and `LivingEntity.knockback` halves whatever velocity it
finds before adding its own. Godite's 11.0 reached the zombie as a measured **6.78**, and 124
blocks of flight arrived as **79.8**. Nothing errored; the burst looked and sounded exactly
right and every constant said 11.0. The same bug had been quietly halving vibranium's 1.8 since
1.3.0.

`KineticLaunch` now queues both cycles' impulses and applies them at `END_SERVER_TICK`, after
vanilla has had its say. Both metals now deliver their configured launch; vibranium's strike is
correspondingly a little stronger than it has been, which is the constant it always claimed.

Not covered here: the worn-armour look and the in-hand item art on a real client. The
equipment-asset gate proves every layer resolves to a correctly-sized texture and every
generated texture was read directly as an image, but nobody has seen godite on a player model.
