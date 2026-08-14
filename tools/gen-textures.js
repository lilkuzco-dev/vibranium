#!/usr/bin/env node
// Vibranium textures v3 — vanilla recolors ("literally the vanilla texture,
// except purple"). Decodes the real textures from the Minecraft client jar in
// the Fabric Loom cache and remaps their hues to purple, preserving every
// pixel's structure and luminance:
//   diamond_ore / deepslate_diamond_ore -> vibranium ores   (stone untouched)
//   diamond_block                        -> block_of_vibranium
//   iron_ingot                           -> vibranium_ingot
//   raw_iron                             -> raw_vibranium
//   raw_iron_block                       -> raw_vibranium_block
// NOTE: output is derivative of Mojang's art (normal for Minecraft mods, but
// not original artwork). Requires `unzip` on PATH and a populated Loom cache
// (run any gradle task once first). Usage:
//   node tools/gen-textures.js [--previews <dir>]

const zlib = require("node:zlib");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { execFileSync } = require("node:child_process");

const PURPLE_HUE = 270 / 360;

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
// mode "all":        recolor every opaque pixel; grays gain purple saturation
// mode "cyan-only":  recolor only cyan/teal-hued pixels (weapon blades/heads),
//                    leaving wooden handles and other materials untouched
// opts.brighten:     push bright pixels further toward white (glowing-energy look)
function recolor(img, mode, opts = {}) {
	const out = Buffer.from(img.px);
	for (let i = 0; i < out.length; i += 4) {
		if (out[i + 3] < 8) continue;
		const [h, s, l] = rgbToHsl(out[i], out[i + 1], out[i + 2]);
		let ns = s;
		let nl = l;
		if (mode === "saturated") {
			if (s < 0.14) continue; // stone stays stone
		} else if (mode === "cyan-only") {
			if (s < 0.14 || h < 0.35 || h > 0.62) continue; // only the cyan family moves
		} else if (s < 0.1) {
			// gray metal: synthesize purple saturation, gentler at the extremes
			ns = l > 0.9 || l < 0.12 ? 0.22 : 0.42;
		}
		if (opts.brighten) {
			// glowing-energy look: lift the whole body, then push the glints toward white
			nl = Math.min(0.92, 0.15 + l * 1.4);
			if (l > 0.35) nl = Math.min(0.97, nl + 0.25);
		}
		const [r, g, b] = hslToRgb(PURPLE_HUE, ns, nl);
		out[i] = r;
		out[i + 1] = g;
		out[i + 2] = b;
	}
	return { w: img.w, h: img.h, px: out };
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
	// tools + spear: cyan heads -> purple, wooden handles untouched
	{ src: "item/diamond_pickaxe", out: "textures/item/vibranium_pickaxe.png", mode: "cyan-only" },
	{ src: "item/diamond_shovel", out: "textures/item/vibranium_shovel.png", mode: "cyan-only" },
	{ src: "item/diamond_hoe", out: "textures/item/vibranium_hoe.png", mode: "cyan-only" },
	{ src: "item/diamond_spear", out: "textures/item/vibranium_spear.png", mode: "cyan-only" },
	{ src: "item/diamond_spear_in_hand", out: "textures/item/vibranium_spear_in_hand.png", mode: "cyan-only" },
];
const results = {};
for (const job of JOBS) {
	const img = recolor(decodePng(readFromJar(jar, `assets/minecraft/textures/${job.src}.png`)), job.mode, { brighten: job.brighten });
	results[job.out] = img;
	const file = path.join(ASSETS, job.out);
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, encodePng(img.w, img.h, img.px));
	console.log(`wrote ${job.out}  (recolored ${job.src})`);
}
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
