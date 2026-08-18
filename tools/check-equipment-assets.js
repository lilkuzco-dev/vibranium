#!/usr/bin/env node
// Equipment-asset gate: worn armor that resolves to a missing texture renders as
// NOTHING — no crash, no log line, no server-side symptom. Every check the server
// can run passes while the armor is invisible on every client. This asserts, from
// the shipped resources, that every layer in every assets/<ns>/equipment/*.json
// resolves to a real PNG of the right size.
//
// Path rule (EquipmentClientInfo$Layer.getTextureLocation, 26.2): a layer's
//   "texture": "<ns>:<name>"  under layer type <type>
// resolves to
//   assets/<ns>/textures/entity/equipment/<type>/<name>.png
//
// Usage: node tools/check-equipment-assets.js [--jar <path>]   (default: source tree)

const fs = require("node:fs");
const path = require("node:path");
const { execFileSync } = require("node:child_process");

// Layer types that vanilla's humanoid armor renders, and the sheet size each expects.
// A 16x16 sheet where a 64x32 one belongs is the cosmos truncation bug wearing a hat.
const EXPECTED_SIZE = {
	humanoid: [64, 32],
	humanoid_leggings: [64, 32],
	humanoid_baby: [64, 64],
	wings: [64, 32],
};

const jarIndex = process.argv.indexOf("--jar");
const jar = jarIndex === -1 ? null : process.argv[jarIndex + 1];
const ROOT = path.join(__dirname, "..", "src/main/resources");

const listing = jar
	? execFileSync("unzip", ["-Z1", jar], { encoding: "utf8" }).split("\n")
	: null;
const exists = (rel) => (jar ? listing.includes(rel) : fs.existsSync(path.join(ROOT, rel)));
const read = (rel) =>
	jar ? execFileSync("unzip", ["-p", jar, rel], { maxBuffer: 1 << 24 }) : fs.readFileSync(path.join(ROOT, rel));
const pngSize = (buf) => [buf.readUInt32BE(16), buf.readUInt32BE(20)];

const assetFiles = [];
if (jar) {
	assetFiles.push(...listing.filter((e) => /^assets\/[^/]+\/equipment\/[^/]+\.json$/.test(e)));
} else {
	for (const ns of fs.readdirSync(path.join(ROOT, "assets"))) {
		const dir = path.join(ROOT, "assets", ns, "equipment");
		if (!fs.existsSync(dir)) continue;
		for (const f of fs.readdirSync(dir)) assetFiles.push(`assets/${ns}/equipment/${f}`);
	}
}

const failures = [];
let checked = 0;
for (const rel of assetFiles) {
	const asset = JSON.parse(read(rel).toString("utf8"));
	for (const [layerType, layers] of Object.entries(asset.layers ?? {})) {
		for (const layer of layers) {
			const [ns, name] = layer.texture.includes(":") ? layer.texture.split(":") : ["minecraft", layer.texture];
			const texture = `assets/${ns}/textures/entity/equipment/${layerType}/${name}.png`;
			checked++;
			if (!exists(texture)) {
				failures.push(`${rel} [${layerType}] -> ${texture} MISSING (armor would render invisible)`);
				continue;
			}
			const expected = EXPECTED_SIZE[layerType];
			if (expected) {
				const [w, h] = pngSize(read(texture));
				if (w !== expected[0] || h !== expected[1]) {
					failures.push(`${texture} is ${w}x${h}, ${layerType} expects ${expected[0]}x${expected[1]}`);
				} else {
					console.log(`ok  ${rel} [${layerType}] -> ${texture} (${w}x${h})`);
				}
			} else {
				console.log(`ok  ${rel} [${layerType}] -> ${texture}`);
			}
		}
	}
}

if (failures.length) {
	for (const f of failures) console.error(`FAIL ${f}`);
	console.error(`equipment assets: ${failures.length} failure(s) across ${checked} layer(s)`);
	process.exit(1);
}
console.log(`equipment assets: PASS — ${checked} layer(s) across ${assetFiles.length} asset file(s)`);
