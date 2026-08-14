#!/usr/bin/env node
// Generates all Vibranium textures programmatically (original pixel art, no
// copied assets): ore blocks (stone/deepslate base + purple gem flecks in the
// style of diamond ore), the gem item, the storage block, and the mod icon.
// Zero dependencies — includes a minimal PNG encoder. Deterministic output.
//
// Usage: node tools/gen-textures.js [--previews <dir>]  (previews = 8x upscales)

const zlib = require("node:zlib");
const fs = require("node:fs");
const path = require("node:path");

// ---------- minimal PNG encoder (RGBA, 8-bit) ----------
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
function encodePng(width, height, rgba) {
	const ihdr = Buffer.alloc(13);
	ihdr.writeUInt32BE(width, 0);
	ihdr.writeUInt32BE(height, 4);
	ihdr[8] = 8; // bit depth
	ihdr[9] = 6; // color type RGBA
	const raw = Buffer.alloc(height * (1 + width * 4));
	for (let y = 0; y < height; y++) {
		raw[y * (1 + width * 4)] = 0; // filter: none
		rgba.copy(raw, y * (1 + width * 4) + 1, y * width * 4, (y + 1) * width * 4);
	}
	return Buffer.concat([
		Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
		pngChunk("IHDR", ihdr),
		pngChunk("IDAT", zlib.deflateSync(raw, { level: 9 })),
		pngChunk("IEND", Buffer.alloc(0)),
	]);
}

// ---------- tiny image helper ----------
class Img {
	constructor(w, h) {
		this.w = w;
		this.h = h;
		this.d = Buffer.alloc(w * h * 4); // transparent
	}
	set(x, y, [r, g, b, a = 255]) {
		if (x < 0 || y < 0 || x >= this.w || y >= this.h) return;
		const i = (y * this.w + x) * 4;
		this.d[i] = r;
		this.d[i + 1] = g;
		this.d[i + 2] = b;
		this.d[i + 3] = a;
	}
	get(x, y) {
		const i = (y * this.w + x) * 4;
		return [this.d[i], this.d[i + 1], this.d[i + 2], this.d[i + 3]];
	}
	save(file) {
		fs.mkdirSync(path.dirname(file), { recursive: true });
		fs.writeFileSync(file, encodePng(this.w, this.h, this.d));
		console.log(`wrote ${file}`);
	}
	upscale(factor) {
		const out = new Img(this.w * factor, this.h * factor);
		for (let y = 0; y < out.h; y++)
			for (let x = 0; x < out.w; x++) out.set(x, y, this.get(Math.floor(x / factor), Math.floor(y / factor)));
		return out;
	}
}

// deterministic PRNG
function mulberry32(seed) {
	let a = seed >>> 0;
	return () => {
		a |= 0;
		a = (a + 0x6d2b79f5) | 0;
		let t = Math.imul(a ^ (a >>> 15), 1 | a);
		t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
		return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
	};
}

const gray = (v) => [v, v, v, 255];

// ---------- stone-style base (original, vanilla-inspired look) ----------
function rockBase(seed, base, spread, blotchShades) {
	const rng = mulberry32(seed);
	const img = new Img(16, 16);
	const shade = Array.from({ length: 16 }, () => new Array(16).fill(base));
	// soft patches of lighter/darker rock
	for (let i = 0; i < 15; i++) {
		const cx = Math.floor(rng() * 16);
		const cy = Math.floor(rng() * 16);
		const dv = Math.round((rng() - 0.5) * 2 * spread);
		const rad = 1 + Math.floor(rng() * 2);
		for (let dy = -rad; dy <= rad; dy++)
			for (let dx = -rad; dx <= rad; dx++) {
				if (Math.abs(dx) + Math.abs(dy) > rad) continue;
				const x = (cx + dx + 16) % 16; // wrap so the texture tiles
				const y = (cy + dy + 16) % 16;
				shade[y][x] += dv;
			}
	}
	// small hard blotches for grit
	for (let i = 0; i < 10; i++) {
		const x = Math.floor(rng() * 16);
		const y = Math.floor(rng() * 16);
		const v = blotchShades[Math.floor(rng() * blotchShades.length)];
		shade[y][x] = v;
		if (rng() < 0.5) shade[y][(x + 1) % 16] = v;
	}
	// per-pixel jitter
	for (let y = 0; y < 16; y++)
		for (let x = 0; x < 16; x++) {
			const v = Math.max(0, Math.min(255, shade[y][x] + Math.round((rng() - 0.5) * 10)));
			img.set(x, y, gray(v));
		}
	return img;
}

// ---------- purple gem palette ----------
const P = {
	spark: [243, 232, 255, 255], // near-white glint
	hi: [199, 125, 255, 255], // bright violet
	mid: [157, 78, 221, 255], // core purple
	lo: [106, 44, 165, 255], // deep purple
	edge: [63, 21, 102, 255], // darkest outline purple
};

// Diamond-ore-style fleck clusters: [x, y, role] per pixel, hand-placed.
const ORE_CLUSTERS = [
	// small square cluster, top-left
	[[3, 2, "hi"], [4, 2, "spark"], [3, 3, "mid"], [4, 3, "lo"]],
	// plus-shaped cluster, top-right
	[[11, 3, "spark"], [10, 4, "mid"], [11, 4, "hi"], [12, 4, "mid"], [11, 5, "lo"]],
	// triangle, mid-left
	[[2, 9, "hi"], [3, 9, "mid"], [3, 10, "lo"]],
	// square-ish, center-bottom
	[[8, 8, "mid"], [9, 8, "hi"], [8, 9, "lo"], [9, 9, "mid"], [10, 9, "lo"]],
	// pair, bottom-right
	[[13, 12, "hi"], [14, 13, "mid"]],
	// pair, bottom-left
	[[5, 13, "mid"], [5, 14, "lo"]],
];
function addFlecks(img) {
	for (const cluster of ORE_CLUSTERS) for (const [x, y, role] of cluster) img.set(x, y, P[role]);
	return img;
}

// ---------- textures ----------
function vibraniumOre() {
	return addFlecks(rockBase(1337, 128, 22, [96, 104, 150, 158]));
}
function deepslateVibraniumOre() {
	const img = rockBase(4242, 74, 16, [52, 58, 92]);
	// subtle vertical grain, like deepslate's directional look
	const rng = mulberry32(99);
	for (let x = 0; x < 16; x++) {
		const bias = Math.round((rng() - 0.5) * 8);
		for (let y = 0; y < 16; y++) {
			const [r] = img.get(x, y);
			img.set(x, y, gray(Math.max(0, Math.min(255, r + bias))));
		}
	}
	return addFlecks(img);
}

function vibraniumGem() {
	const img = new Img(16, 16);
	// faceted gem silhouette: widest at y=7/8, pointed top and bottom
	const rows = {
		3: [6, 9], 4: [5, 10], 5: [4, 11], 6: [3, 12], 7: [3, 12],
		8: [4, 11], 9: [5, 10], 10: [6, 9], 11: [7, 8],
	};
	for (const [yStr, [x0, x1]] of Object.entries(rows)) {
		const y = Number(yStr);
		for (let x = x0; x <= x1; x++) {
			const onEdge = x === x0 || x === x1 || y === 3 || y === 11 ||
				(y <= 6 && (x === x0 || x === x1)) === undefined; // edge = outline
			let color;
			if (x === x0 || x === x1 || y === 3 || y === 11) color = P.edge;
			else if (x + y < 12) color = P.hi; // top-left facets catch light
			else if (x + y > 17) color = P.lo; // bottom-right in shadow
			else color = P.mid;
			img.set(x, y, color);
		}
	}
	// table facet line + sparkle
	for (let x = 6; x <= 9; x++) img.set(x, 5, P.hi);
	img.set(6, 4, P.spark);
	img.set(7, 4, P.spark);
	img.set(5, 6, P.spark);
	return img;
}

function blockOfVibranium() {
	const img = new Img(16, 16);
	for (let y = 0; y < 16; y++)
		for (let x = 0; x < 16; x++) {
			let color;
			if (x === 0 || y === 0 || x === 15 || y === 15) color = P.edge; // outer frame
			else if (x === 1 || y === 1) color = x === 1 && y > 1 ? P.mid : P.hi; // bevel: lit top, mid left
			else if (x === 14 || y === 14) color = P.lo; // bevel shadow
			else {
				// inner face: diagonal facet bands
				const band = (x + y) % 6;
				color = band === 0 ? P.hi : band === 3 ? P.lo : P.mid;
			}
			img.set(x, y, color);
		}
	// corner glints on the inner face
	img.set(3, 3, P.spark);
	img.set(4, 3, P.hi);
	img.set(3, 4, P.hi);
	img.set(12, 11, P.spark);
	return img;
}

function icon() {
	// mod icon: the gem, upscaled onto a dark rounded card
	const card = new Img(16, 16);
	for (let y = 0; y < 16; y++)
		for (let x = 0; x < 16; x++) {
			const corner = (x < 2 || x > 13) && (y < 2 || y > 13) && (x === 0 || x === 15) && (y === 0 || y === 15);
			if (!corner) card.set(x, y, [26, 16, 38, 255]);
		}
	const gem = vibraniumGem();
	for (let y = 0; y < 16; y++)
		for (let x = 0; x < 16; x++) {
			const px = gem.get(x, y);
			if (px[3] > 0) card.set(x, y, px);
		}
	return card.upscale(8); // 128x128
}

// ---------- main ----------
const ROOT = path.join(__dirname, "..");
const ASSETS = path.join(ROOT, "src/main/resources/assets/vibranium");
const outputs = {
	[path.join(ASSETS, "textures/block/vibranium_ore.png")]: vibraniumOre(),
	[path.join(ASSETS, "textures/block/deepslate_vibranium_ore.png")]: deepslateVibraniumOre(),
	[path.join(ASSETS, "textures/block/block_of_vibranium.png")]: blockOfVibranium(),
	[path.join(ASSETS, "textures/item/vibranium_gem.png")]: vibraniumGem(),
	[path.join(ASSETS, "icon.png")]: icon(),
};
for (const [file, img] of Object.entries(outputs)) img.save(file);

const previewIdx = process.argv.indexOf("--previews");
if (previewIdx !== -1) {
	const dir = process.argv[previewIdx + 1];
	for (const [file, img] of Object.entries(outputs)) {
		if (file.endsWith("icon.png")) continue;
		img.upscale(8).save(path.join(dir, path.basename(file)));
	}
}
