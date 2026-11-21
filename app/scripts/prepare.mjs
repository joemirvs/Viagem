// Monta a pasta www do app: copia o mapa e os dados do site e troca as bibliotecas da internet por cópias locais (funciona sem internet).
import { cpSync, existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname } from "node:path";
const copiar = (de, para) => { if (!existsSync(de)) { console.error("ERRO: não achei", de); process.exit(1); } mkdirSync(dirname(para), { recursive: true }); cpSync(de, para); console.log("ok:", para); };
for (const f of ["index.html", "land.json", "holyland.json", "greece.json", "icon-192.png"]) copiar(`../${f}`, `www/${f}`);
copiar("node_modules/@capacitor/core/dist/capacitor.js", "www/vendor/capacitor.js");
copiar("node_modules/three/build/three.min.js", "www/vendor/three.min.js");
copiar("node_modules/three/examples/js/controls/OrbitControls.js", "www/vendor/OrbitControls.js");
copiar("node_modules/exifr/dist/full.umd.js", "www/vendor/exifr.umd.js");
copiar("node_modules/heic2any/dist/heic2any.min.js", "www/vendor/heic2any.min.js");
const fontes = [["eb-garamond", "eb-garamond-latin-400-normal", 400, "normal", "EB Garamond"], ["eb-garamond", "eb-garamond-latin-600-normal", 600, "normal", "EB Garamond"], ["eb-garamond", "eb-garamond-latin-400-italic", 400, "italic", "EB Garamond"], ["pirata-one", "pirata-one-latin-400-normal", 400, "normal", "Pirata One"]];
let css = "";
for (const [pkg, file, w, st, fam] of fontes) { copiar(`node_modules/@fontsource/${pkg}/files/${file}.woff2`, `www/fonts/${file}.woff2`); css += `@font-face{font-family:'${fam}';font-style:${st};font-weight:${w};font-display:swap;src:url(${file}.woff2) format('woff2')}\n`; }
writeFileSync("www/fonts/fonts.css", css);
let h = readFileSync("www/index.html", "utf8");
const trocas = [
  [/<link rel="preconnect"[^>]*>\s*/g, ""],
  [/<link href="https:\/\/fonts\.googleapis\.com[^>]*>/, '<link rel="stylesheet" href="fonts/fonts.css">'],
  ["https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js", "vendor/three.min.js"],
  ["https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js", "vendor/OrbitControls.js"],
  ["https://cdn.jsdelivr.net/npm/exifr@7.1.3/dist/full.umd.js", "vendor/exifr.umd.js"],
  ["https://cdn.jsdelivr.net/npm/heic2any@0.0.4/dist/heic2any.min.js", "vendor/heic2any.min.js"],
  [/<link rel="manifest"[^>]*>\s*/, ""],
  ["</head>", '<script src="vendor/capacitor.js"></script>\n</head>']
];
for (const [a, b] of trocas) { const antes = h; h = h.replace(a, b); if (h === antes) console.warn("AVISO: não troquei", a); }
writeFileSync("www/index.html", h);
console.log("www pronta");
