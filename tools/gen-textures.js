#!/usr/bin/env node
// Vibranium textures v4 — hue-shifted derivatives from TWO sources:
//   1. the vanilla Minecraft client jar in the Fabric Loom cache (`src` jobs)
//   2. a local TechReborn checkout (`trSrc` jobs) — MIT-licensed art, see
//      CREDITS.md. Auto-cloned to .techreborn-src (branch 26.2) if missing;
//      override the location with the TECHREBORN_SRC env var.
// Every texture the mod ships flows through this script: no from-scratch
// pixel art is generated. GUI textures are COMPOSED from vanilla GUI parts
// (crafting_table.png / furnace.png regions rearranged into 176x206 layouts)
// and then trimmed purple; block/item textures are straight hue remaps that
// preserve pixel structure and luminance.
// NOTE: output is derivative of Mojang's and TechReborn's art (normal for
// Minecraft mods, but not original artwork). Requires `unzip` + `git` on
// PATH and a populated Loom cache (run any gradle task once first). Usage:
//   node tools/gen-textures.js [--previews <dir>]

const zlib = require("node:zlib");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { execFileSync } = require("node:child_process");

const PURPLE_HUE = 270 / 360;
// Godite's whole look: the same vanilla art, the same recolor, but the hue SWEPT across the
// metal instead of held at one value — one full turn of the wheel from one end of the piece
// to the other, so a godite ingot is a rainbow and a godite blade grades through the spectrum.
// Every other knob (which pixels move, how saturation is synthesized on grays, luminance) is
// untouched: godite is literally vibranium's texture pipeline with a hue function in place of
// a constant. See recolor() for why the sweep is measured over the moving pixels.
const RAINBOW_HUE = (t) => t;

// ---------- locate the vanilla client jar ----------
function findClientJar() {
	const cacheRoot = path.join(os.homedir(), ".gradle/caches/fabric-loom");
	for (const version of fs.existsSync(cacheRoot) ? fs.readdirSync(cacheRoot) : []) {
		for (const name of ["minecraft-client-only.jar", "minecraft-client.jar"]) {
			const jar = path.join(cacheRoot, version, name);
			if (fs.existsSync(jar)) {
				try {
					execFileSync("unzip", ["-l", jar, "assets/minecraft/textures/block/diamond_ore.png"], { stdio: "pipe" });
					return jar;
				} catch { /* texture not in this jar; keep looking */ }
			}
		}
	}
	throw new Error("Could not find a Loom-cached Minecraft client jar with textures. Run ./gradlew build once first.");
}
const readFromJar = (jar, entry) => execFileSync("unzip", ["-p", jar, entry], { maxBuffer: 1 << 24 });

// ---------- locate (or clone) the TechReborn asset tree ----------
// TechReborn is MIT (Copyright (c) 2020 TechReborn); its textures may be
// modified and redistributed with attribution — see CREDITS.md. We never
// invent pixel art here: if a needed source file is absent, the script fails
// loudly instead of generating something.
function findTechRebornDir() {
	const dir = process.env.TECHREBORN_SRC || path.join(__dirname, "..", ".techreborn-src");
	const marker = path.join(dir, "src/main/resources/assets/techreborn/textures");
	if (!fs.existsSync(marker)) {
		console.log(`TechReborn checkout not found; cloning branch 26.2 into ${dir} ...`);
		execFileSync("git", ["clone", "--depth", "1", "-b", "26.2", "https://github.com/TechReborn/TechReborn", dir], { stdio: "inherit" });
	}
	if (!fs.existsSync(marker)) throw new Error(`TechReborn assets missing at ${marker}`);
	return dir;
}
const readFromTechReborn = (trDir, rel) =>
	fs.readFileSync(path.join(trDir, "src/main/resources/assets/techreborn/textures", rel));

// ---------- PNG decode (8/4/2/1-bit; color types 0,2,3,4,6; no interlace) ----------
function decodePng(buf) {
	let off = 8;
	let w, h, bitDepth, colorType;
	const palette = [], trns = [], idat = [];
	while (off < buf.length) {
		const len = buf.readUInt32BE(off);
		const type = buf.toString("ascii", off + 4, off + 8);
		const data = buf.subarray(off + 8, off + 8 + len);
		if (type === "IHDR") {
			w = data.readUInt32BE(0); h = data.readUInt32BE(4);
			bitDepth = data[8]; colorType = data[9];
			if (data[12] !== 0) throw new Error("interlaced png not supported");
		} else if (type === "PLTE") for (let i = 0; i < data.length; i += 3) palette.push([data[i], data[i + 1], data[i + 2]]);
		else if (type === "tRNS") trns.push(...data);
		else if (type === "IDAT") idat.push(data);
		off += 12 + len;
	}
	const raw = zlib.inflateSync(Buffer.concat(idat));
	const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType];
	const bpp = Math.max(1, (channels * bitDepth) / 8);
	const stride = Math.ceil((w * channels * bitDepth) / 8);
	const out = Buffer.alloc(h * stride);
	let prev = Buffer.alloc(stride);
	for (let y = 0; y < h; y++) {
		const filter = raw[y * (stride + 1)];
		const line = Buffer.from(raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1)));
		for (let x = 0; x < stride; x++) {
			const a = x >= bpp ? line[x - bpp] : 0;
			const b = prev[x];
			const c = x >= bpp ? prev[x - bpp] : 0;
			if (filter === 1) line[x] = (line[x] + a) & 0xff;
			else if (filter === 2) line[x] = (line[x] + b) & 0xff;
			else if (filter === 3) line[x] = (line[x] + ((a + b) >> 1)) & 0xff;
			else if (filter === 4) {
				const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
				line[x] = (line[x] + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 0xff;
			}
			out[y * stride + x] = line[x];
		}
		prev = line;
	}
	const bitAt = (row, i) => {
		const bitPos = i * bitDepth;
		return (out[row * stride + (bitPos >> 3)] >> (8 - bitDepth - (bitPos & 7))) & ((1 << bitDepth) - 1);
	};
	const px = Buffer.alloc(w * h * 4);
	for (let y = 0; y < h; y++)
		for (let x = 0; x < w; x++) {
			const i = (y * w + x) * 4;
			if (colorType === 6) out.copy(px, i, y * stride + x * 4, y * stride + x * 4 + 4);
			else if (colorType === 2) { out.copy(px, i, y * stride + x * 3, y * stride + x * 3 + 3); px[i + 3] = 255; }
			else if (colorType === 3) {
				const idx = bitAt(y, x);
				const [r, g, b] = palette[idx] ?? [0, 0, 0];
				px[i] = r; px[i + 1] = g; px[i + 2] = b; px[i + 3] = trns[idx] ?? 255;
			} else if (colorType === 0) {
				const v = Math.round(bitAt(y, x) * (255 / ((1 << bitDepth) - 1)));
				px[i] = px[i + 1] = px[i + 2] = v; px[i + 3] = 255;
			} else if (colorType === 4) {
				const v = out[y * stride + x * 2];
				px[i] = px[i + 1] = px[i + 2] = v; px[i + 3] = out[y * stride + x * 2 + 1];
			}
		}
	return { w, h, px };
}

// ---------- PNG encode (RGBA) ----------
const CRC_TABLE = (() => {
	const t = new Int32Array(256);
	for (let n = 0; n < 256; n++) {
		let c = n;
		for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
		t[n] = c;
	}
	return t;
})();
const crc32 = (buf) => {
	let c = 0xffffffff;
	for (const b of buf) c = CRC_TABLE[(c ^ b) & 0xff] ^ (c >>> 8);
	return (c ^ 0xffffffff) >>> 0;
};
function pngChunk(type, data) {
	const len = Buffer.alloc(4);
	len.writeUInt32BE(data.length);
	const body = Buffer.concat([Buffer.from(type, "ascii"), data]);
	const crc = Buffer.alloc(4);
	crc.writeUInt32BE(crc32(body));
	return Buffer.concat([len, body, crc]);
}
function encodePng(w, h, rgba) {
	const ihdr = Buffer.alloc(13);
	ihdr.writeUInt32BE(w, 0);
	ihdr.writeUInt32BE(h, 4);
	ihdr[8] = 8;
	ihdr[9] = 6;
	const raw = Buffer.alloc(h * (1 + w * 4));
	for (let y = 0; y < h; y++) {
		raw[y * (1 + w * 4)] = 0;
		rgba.copy(raw, y * (1 + w * 4) + 1, y * w * 4, (y + 1) * w * 4);
	}
	return Buffer.concat([
		Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
		pngChunk("IHDR", ihdr),
		pngChunk("IDAT", zlib.deflateSync(raw, { level: 9 })),
		pngChunk("IEND", Buffer.alloc(0)),
	]);
}

// ---------- HSL color remapping ----------
function rgbToHsl(r, g, b) {
	r /= 255; g /= 255; b /= 255;
	const mx = Math.max(r, g, b), mn = Math.min(r, g, b), l = (mx + mn) / 2;
	if (mx === mn) return [0, 0, l];
	const d = mx - mn;
	const s = l > 0.5 ? d / (2 - mx - mn) : d / (mx + mn);
	let h;
	if (mx === r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
	else if (mx === g) h = ((b - r) / d + 2) / 6;
	else h = ((r - g) / d + 4) / 6;
	return [h, s, l];
}
function hslToRgb(h, s, l) {
	if (s === 0) {
		const v = Math.round(l * 255);
		return [v, v, v];
	}
	const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
	const p = 2 * l - q;
	const f = (t) => {
		t = ((t % 1) + 1) % 1;
		if (t < 1 / 6) return p + (q - p) * 6 * t;
		if (t < 1 / 2) return q;
		if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
		return p;
	};
	return [Math.round(f(h + 1 / 3) * 255), Math.round(f(h) * 255), Math.round(f(h - 1 / 3) * 255)];
}

// mode "saturated":  recolor only clearly-colored pixels (ore: gems yes, stone no)
// mode "all":        recolor every opaque pixel; grays gain saturation
// mode "cyan-only":  recolor only cyan/teal-hued pixels (weapon blades/heads),
//                    leaving wooden handles and other materials untouched
// opts.brighten:     push bright pixels further toward white (glowing-energy look)
// opts.hue:          a constant hue (default: vibranium's purple), or a function of the
//                    sweep position t in [0,1] — which is how godite gets its rainbow

// Which pixels a mode moves. Split out because the rainbow has to MEASURE the moving
// pixels before it can recolor them, so the rule is needed in two places.
function movesUnder(mode, h, s) {
	if (mode === "saturated") return s >= 0.14;      // stone stays stone
	if (mode === "cyan-only") return s >= 0.14 && h >= 0.35 && h <= 0.62; // only the cyan family
	return true;
}

function recolor(img, mode, opts = {}) {
	const hue = opts.hue ?? PURPLE_HUE;
	// A positional hue has to sweep across THE PIXELS THAT MOVE, not across the sprite's
	// bounding box. Item sprites are mostly empty and mostly diagonal: a sword blade lies
	// along one diagonal, so an x+y gradient hands the whole blade a single colour (it did,
	// on the first attempt). The sweep therefore runs along the anti-diagonal x-y, and is
	// EQUALISED — each step of the wheel gets the same number of pixels — because a raw
	// min/max stretch piles most of a chubby sprite like the ingot into the middle third of
	// the spectrum and never reaches the reds at either end.
	const sweep = typeof hue === "function" ? equalisedSweep(img, mode) : null;
	const out = Buffer.from(img.px);
	for (let i = 0; i < out.length; i += 4) {
		if (out[i + 3] < 8) continue;
		const [h, s, l] = rgbToHsl(out[i], out[i + 1], out[i + 2]);
		if (!movesUnder(mode, h, s)) continue;
		let ns = s;
		let nl = l;
		if (mode !== "saturated" && mode !== "cyan-only" && s < 0.1) {
			// gray metal: synthesize saturation, gentler at the extremes
			ns = l > 0.9 || l < 0.12 ? 0.22 : 0.42;
		}
		if (opts.brighten) {
			// glowing-energy look: lift the whole body, then push the glints toward white
			nl = Math.min(0.92, 0.15 + l * 1.4);
			if (l > 0.35) nl = Math.min(0.97, nl + 0.25);
		}
		const [r, g, b] = hslToRgb(sweep === null ? hue : hue(sweep(i / 4)), ns, nl);
		out[i] = r;
		out[i + 1] = g;
		out[i + 2] = b;
	}
	return { w: img.w, h: img.h, px: out };
}

// Maps a pixel index to its position 0..1 along the sweep, with every band of the wheel
// covering an equal share of the moving pixels (a cumulative histogram over x-y).
function equalisedSweep(img, mode) {
	const counts = new Map();
	const diagonal = (pixel) => (pixel % img.w) - Math.floor(pixel / img.w);
	let total = 0;
	for (let i = 0; i < img.px.length; i += 4) {
		if (img.px[i + 3] < 8) continue;
		const [h, s] = rgbToHsl(img.px[i], img.px[i + 1], img.px[i + 2]);
		if (!movesUnder(mode, h, s)) continue;
		const d = diagonal(i / 4);
		counts.set(d, (counts.get(d) ?? 0) + 1);
		total++;
	}
	const position = new Map();
	let seen = 0;
	for (const d of [...counts.keys()].sort((a, b) => a - b)) {
		const count = counts.get(d);
		position.set(d, total > 0 ? (seen + count / 2) / total : 0); // midpoint of this step's band
		seen += count;
	}
	return (pixel) => position.get(diagonal(pixel)) ?? 0;
}

function upscale(img, factor) {
	const out = Buffer.alloc(img.w * factor * img.h * factor * 4);
	for (let y = 0; y < img.h * factor; y++)
		for (let x = 0; x < img.w * factor; x++) {
			const src = ((Math.floor(y / factor) * img.w) + Math.floor(x / factor)) * 4;
			img.px.copy(out, (y * img.w * factor + x) * 4, src, src + 4);
		}
	return { w: img.w * factor, h: img.h * factor, px: out };
}

// icon: the recolored ingot on a dark rounded card, upscaled to 128
function makeIcon(ingot) {
	const card = { w: 16, h: 16, px: Buffer.alloc(16 * 16 * 4) };
	for (let y = 0; y < 16; y++)
		for (let x = 0; x < 16; x++) {
			const corner = (x === 0 || x === 15) && (y === 0 || y === 15);
			if (corner) continue;
			const i = (y * 16 + x) * 4;
			card.px[i] = 26; card.px[i + 1] = 16; card.px[i + 2] = 38; card.px[i + 3] = 255;
		}
	for (let i = 0; i < ingot.px.length; i += 4) {
		if (ingot.px[i + 3] > 8) ingot.px.copy(card.px, i, i, i + 4);
	}
	return upscale(card, 8);
}

// ---------- main ----------
const ROOT = path.join(__dirname, "..");
const ASSETS = path.join(ROOT, "src/main/resources/assets/vibranium");
const jar = findClientJar();
console.log(`vanilla jar: ${jar}`);

const JOBS = [
	{ src: "block/diamond_ore", out: "textures/block/vibranium_ore.png", mode: "saturated" },
	{ src: "block/deepslate_diamond_ore", out: "textures/block/deepslate_vibranium_ore.png", mode: "saturated" },
	{ src: "block/diamond_block", out: "textures/block/block_of_vibranium.png", mode: "all" },
	{ src: "item/iron_ingot", out: "textures/item/vibranium_ingot.png", mode: "all" },
	{ src: "item/raw_iron", out: "textures/item/raw_vibranium.png", mode: "all" },
	{ src: "block/raw_iron_block", out: "textures/block/raw_vibranium_block.png", mode: "all" },
	// weapons: only the cyan blade/head pixels shift to purple; handles stay wooden
	{ src: "item/diamond_sword", out: "textures/item/vibranium_sword.png", mode: "cyan-only" },
	{ src: "item/diamond_axe", out: "textures/item/vibranium_axe.png", mode: "cyan-only" },
	// energy ball: pearl -> purple with the central highlight glowing toward white
	{ src: "item/ender_pearl", out: "textures/item/vibranium_energy_ball.png", mode: "all", brighten: true },
	// armor: the whole piece is the metal (no wooden handle to preserve), so it
	// recolors like the ingot — every opaque pixel goes purple, grays gain saturation
	{ src: "item/diamond_helmet", out: "textures/item/vibranium_helmet.png", mode: "all" },
	{ src: "item/diamond_chestplate", out: "textures/item/vibranium_chestplate.png", mode: "all" },
	{ src: "item/diamond_leggings", out: "textures/item/vibranium_leggings.png", mode: "all" },
	{ src: "item/diamond_boots", out: "textures/item/vibranium_boots.png", mode: "all" },
	// worn-armor layers (26.x equipment assets; NOT 16x16 — the decoder reads each
	// source's real IHDR size, so these stay 64x32 / 64x32 / 64x32 on the way through)
	{ src: "entity/equipment/humanoid/diamond", out: "textures/entity/equipment/humanoid/vibranium.png", mode: "all" },
	{ src: "entity/equipment/humanoid_baby/diamond", out: "textures/entity/equipment/humanoid_baby/vibranium.png", mode: "all" },
	{ src: "entity/equipment/humanoid_leggings/diamond", out: "textures/entity/equipment/humanoid_leggings/vibranium.png", mode: "all" },
	// tools + spear: cyan heads -> purple, wooden handles untouched
	{ src: "item/diamond_pickaxe", out: "textures/item/vibranium_pickaxe.png", mode: "cyan-only" },
	{ src: "item/diamond_shovel", out: "textures/item/vibranium_shovel.png", mode: "cyan-only" },
	{ src: "item/diamond_hoe", out: "textures/item/vibranium_hoe.png", mode: "cyan-only" },
	{ src: "item/diamond_spear", out: "textures/item/vibranium_spear.png", mode: "cyan-only" },
	{ src: "item/diamond_spear_in_hand", out: "textures/item/vibranium_spear_in_hand.png", mode: "cyan-only" },
	// pit blocks: veinstone = deepslate tinted deep purple; crystal = amethyst cluster hue-shifted
	{ src: "block/deepslate", out: "textures/block/vibranium_veinstone.png", mode: "all" },
	{ src: "block/amethyst_cluster", out: "textures/block/vibranium_crystal_cluster.png", mode: "saturated" },
	// ---- machines (v1.6.0) — TechReborn art, hue-shifted purple (MIT, see CREDITS.md) ----
	// fabricator: advanced casing sides, auto-crafting-table grid top, plain machine bottom
	{ trSrc: "block/machines/structure/advanced_machine_casing.png", out: "textures/block/vibranium_fabricator_side.png", mode: "all" },
	{ trSrc: "block/machines/tier1_machines/auto_crafting_table_top.png", out: "textures/block/vibranium_fabricator_top.png", mode: "all" },
	{ trSrc: "block/machines/tier0_machines/machine_bottom.png", out: "textures/block/vibranium_fabricator_bottom.png", mode: "all" },
	// extractor: basic casing sides, machine top, grinder wheel as the down-facing
	// drill head (grinder_front_on is a 3-frame animation; its .mcmeta ships too)
	{ trSrc: "block/machines/structure/basic_machine_casing.png", out: "textures/block/vibranium_extractor_side.png", mode: "all" },
	{ trSrc: "block/machines/tier0_machines/machine_top.png", out: "textures/block/vibranium_extractor_top.png", mode: "all" },
	{ trSrc: "block/machines/tier1_machines/grinder_front_off.png", out: "textures/block/vibranium_extractor_drill.png", mode: "all" },
	{ trSrc: "block/machines/tier1_machines/grinder_front_on.png", out: "textures/block/vibranium_extractor_drill_on.png", mode: "all",
		mcmeta: "block/machines/tier1_machines/grinder_front_on.png.mcmeta" },
];

// ---- godite (v1.8.0): every vibranium gear/metal job again, rainbow instead of purple ----
// Deliberately the SAME vanilla sources as the vibranium jobs above: godite is a rehue of
// vibranium, so anything else here would make the two metals different art, not different hue.
const GODITE_JOBS = [
	{ src: "block/diamond_ore", out: "textures/block/godite_ore.png", mode: "saturated" },
	{ src: "block/deepslate_diamond_ore", out: "textures/block/deepslate_godite_ore.png", mode: "saturated" },
	{ src: "block/diamond_block", out: "textures/block/block_of_godite.png", mode: "all" },
	{ src: "item/iron_ingot", out: "textures/item/godite_ingot.png", mode: "all" },
	{ src: "item/raw_iron", out: "textures/item/raw_godite.png", mode: "all" },
	{ src: "block/raw_iron_block", out: "textures/block/raw_godite_block.png", mode: "all" },
	{ src: "item/diamond_sword", out: "textures/item/godite_sword.png", mode: "cyan-only" },
	{ src: "item/diamond_axe", out: "textures/item/godite_axe.png", mode: "cyan-only" },
	{ src: "item/diamond_helmet", out: "textures/item/godite_helmet.png", mode: "all" },
	{ src: "item/diamond_chestplate", out: "textures/item/godite_chestplate.png", mode: "all" },
	{ src: "item/diamond_leggings", out: "textures/item/godite_leggings.png", mode: "all" },
	{ src: "item/diamond_boots", out: "textures/item/godite_boots.png", mode: "all" },
	{ src: "entity/equipment/humanoid/diamond", out: "textures/entity/equipment/humanoid/godite.png", mode: "all" },
	{ src: "entity/equipment/humanoid_baby/diamond", out: "textures/entity/equipment/humanoid_baby/godite.png", mode: "all" },
	{ src: "entity/equipment/humanoid_leggings/diamond", out: "textures/entity/equipment/humanoid_leggings/godite.png", mode: "all" },
	{ src: "item/diamond_pickaxe", out: "textures/item/godite_pickaxe.png", mode: "cyan-only" },
	{ src: "item/diamond_shovel", out: "textures/item/godite_shovel.png", mode: "cyan-only" },
	{ src: "item/diamond_hoe", out: "textures/item/godite_hoe.png", mode: "cyan-only" },
	{ src: "item/diamond_spear", out: "textures/item/godite_spear.png", mode: "cyan-only" },
	{ src: "item/diamond_spear_in_hand", out: "textures/item/godite_spear_in_hand.png", mode: "cyan-only" },
].map((job) => ({ ...job, hue: RAINBOW_HUE }));
JOBS.push(...GODITE_JOBS);

const trDir = findTechRebornDir();
console.log(`techreborn src: ${trDir}`);
const results = {};
for (const job of JOBS) {
	const source = job.trSrc ? readFromTechReborn(trDir, job.trSrc) : readFromJar(jar, `assets/minecraft/textures/${job.src}.png`);
	const img = recolor(decodePng(source), job.mode, { brighten: job.brighten, hue: job.hue });
	results[job.out] = img;
	const file = path.join(ASSETS, job.out);
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, encodePng(img.w, img.h, img.px));
	if (job.mcmeta) fs.writeFileSync(file + ".mcmeta", readFromTechReborn(trDir, job.mcmeta));
	console.log(`wrote ${job.out}  (recolored ${job.trSrc ? "techreborn:" + job.trSrc : job.src}${job.mcmeta ? " + animation mcmeta" : ""})`);
}

// ============================================================================
//  GUI COMPOSITION — 176x206 container screens assembled from vanilla parts.
//  Layout constants here MUST match FabricatorMenu/FabricatorScreen and
//  ExtractorMenu/ExtractorScreen in the Java source (coordinates are the
//  slot/widget positions minus 1 for the 18x18 slot borders).
// ============================================================================
function blitRegion(dst, dstW, src, srcW, sx, sy, w, h, dx, dy) {
	for (let y = 0; y < h; y++)
		for (let x = 0; x < w; x++) {
			const si = ((sy + y) * srcW + sx + x) * 4;
			const di = ((dy + y) * dstW + dx + x) * 4;
			src.px.copy(dst, di, si, si + 4);
		}
}
// purple-tint a rectangle in place (the "dark/purple trim"): grays gain a
// deep purple cast, anything already colored is hue-shifted
function tintRect(px, w, x0, y0, rw, rh) {
	for (let y = y0; y < y0 + rh; y++)
		for (let x = x0; x < x0 + rw; x++) {
			const i = (y * w + x) * 4;
			if (px[i + 3] < 8) continue;
			const [, s, l] = rgbToHsl(px[i], px[i + 1], px[i + 2]);
			const [r, g, b] = hslToRgb(PURPLE_HUE, Math.max(s, 0.34), Math.max(0.06, l * 0.82));
			px[i] = r; px[i + 1] = g; px[i + 2] = b;
		}
}
function composeGuis() {
	const crafting = decodePng(readFromJar(jar, "assets/minecraft/textures/gui/container/crafting_table.png"));
	const furnace = decodePng(readFromJar(jar, "assets/minecraft/textures/gui/container/furnace.png"));
	const litSprite = decodePng(readFromJar(jar, "assets/minecraft/textures/gui/sprites/container/furnace/lit_progress.png"));
	const burnSprite = decodePng(readFromJar(jar, "assets/minecraft/textures/gui/sprites/container/furnace/burn_progress.png"));

	// shared scaffold: vanilla top border rows, tiled clean panel body, and the
	// whole player-inventory half (vanilla rows 82..165) moved down to y=122
	function scaffold(base) {
		const px = Buffer.alloc(256 * 256 * 4);
		const cv = { w: 256, h: 256, px };
		blitRegion(px, 256, base, 256, 0, 0, 176, 4, 0, 0); // top border
		for (let y = 4; y < 122; y++) blitRegion(px, 256, base, 256, 0, 8, 176, 1, 0, y); // clean body row tiled
		blitRegion(px, 256, base, 256, 0, 82, 176, 84, 0, 122); // player inventory half (incl. bottom border)
		return cv;
	}
	const slotCell = { img: crafting, x: 29, y: 16 }; // one 18x18 slot cell w/ border

	// ---------- fabricator: 5x5 grid (8,16), arrow (102,54), result (130,52) ----------
	const fab = scaffold(crafting);
	for (let r = 0; r < 5; r++)
		for (let c = 0; c < 5; c++)
			blitRegion(fab.px, 256, slotCell.img, 256, slotCell.x, slotCell.y, 18, 18, 7 + 18 * c, 15 + 18 * r);
	blitRegion(fab.px, 256, crafting, 256, 89, 34, 22, 15, 102, 54); // arrow
	blitRegion(fab.px, 256, crafting, 256, 119, 30, 26, 26, 125, 47); // bordered result slot (item at 130,52)
	fabricatorTrim(fab.px);
	fs.mkdirSync(path.join(ASSETS, "textures/gui/container"), { recursive: true });
	fs.writeFileSync(path.join(ASSETS, "textures/gui/container/fabricator.png"), encodePng(256, 256, fab.px));
	results["textures/gui/container/fabricator.png"] = fab;
	console.log("wrote textures/gui/container/fabricator.png  (composed from crafting_table.png)");

	// ---------- extractor: fuel (8,53)+flame (9,36), status text zone, arrow (110,49),
	// ---------- 9x2 output grid (8,76)/(8,94); sprites stashed at (180,0)/(180,16)
	const ext = scaffold(furnace);
	blitRegion(ext.px, 256, furnace, 256, 55, 52, 18, 18, 7, 52); // fuel slot cell
	blitRegion(ext.px, 256, furnace, 256, 56, 36, 14, 14, 9, 36); // empty flame outline
	blitRegion(ext.px, 256, furnace, 256, 79, 34, 24, 17, 110, 49); // empty progress arrow
	for (let r = 0; r < 2; r++)
		for (let c = 0; c < 9; c++)
			blitRegion(ext.px, 256, furnace, 256, 55, 52, 18, 18, 7 + 18 * c, 75 + 18 * r);
	blitRegion(ext.px, 256, litSprite, litSprite.w, 0, 0, 14, 14, 180, 0); // lit flame fill -> u180,v0
	blitRegion(ext.px, 256, burnSprite, burnSprite.w, 0, 0, 24, 16, 180, 16); // arrow fill -> u180,v16
	tintRect(ext.px, 256, 180, 0, 14, 14); // the flame + arrow fills go purple too
	tintRect(ext.px, 256, 180, 16, 24, 16);
	fabricatorTrim(ext.px);
	fs.writeFileSync(path.join(ASSETS, "textures/gui/container/extractor.png"), encodePng(256, 256, ext.px));
	results["textures/gui/container/extractor.png"] = ext;
	console.log("wrote textures/gui/container/extractor.png  (composed from furnace.png + furnace sprites)");

	function fabricatorTrim(px) {
		tintRect(px, 256, 0, 0, 176, 4);       // top edge
		tintRect(px, 256, 0, 202, 176, 4);     // bottom edge
		tintRect(px, 256, 0, 4, 4, 198);       // left edge
		tintRect(px, 256, 172, 4, 4, 198);     // right edge
	}
}
composeGuis();
const icon = makeIcon(results["textures/item/vibranium_ingot.png"]);
fs.writeFileSync(path.join(ASSETS, "icon.png"), encodePng(icon.w, icon.h, icon.px));
console.log("wrote icon.png");

const previewIdx = process.argv.indexOf("--previews");
if (previewIdx !== -1) {
	const dir = process.argv[previewIdx + 1];
	fs.mkdirSync(dir, { recursive: true });
	for (const [rel, img] of Object.entries(results)) {
		const big = upscale(img, 8);
		fs.writeFileSync(path.join(dir, path.basename(rel)), encodePng(big.w, big.h, big.px));
	}
}
