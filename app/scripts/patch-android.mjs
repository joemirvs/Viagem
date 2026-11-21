// Ajustes no projeto Android: permissões (localização, câmera, fotos), ícone e número da versão.
import { readFileSync, writeFileSync, existsSync, cpSync } from "node:fs";
const m = "android/app/src/main/AndroidManifest.xml";
let s = readFileSync(m, "utf8");
const perms = ["ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "CAMERA", "ACCESS_MEDIA_LOCATION", "READ_MEDIA_IMAGES"];
const linhas = perms.filter(p => !s.includes(`android.permission.${p}`)).map(p => `    <uses-permission android:name="android.permission.${p}" />`).join("\n");
s = s.replace(/(<uses-permission\s+android:name="android\.permission\.INTERNET"\s*\/>)/, `$1\n${linhas}\n    <uses-feature android:name="android.hardware.camera" android:required="false" />\n    <uses-feature android:name="android.hardware.location.gps" android:required="false" />`);
writeFileSync(m, s); console.log("ok: permissões");
if (existsSync("resources/android")) { cpSync("resources/android", "android/app/src/main/res", { recursive: true, force: true }); console.log("ok: ícones"); }
const g = "android/app/build.gradle";
const n = parseInt(process.env.GITHUB_RUN_NUMBER || "1", 10) || 1;
writeFileSync(g, readFileSync(g, "utf8").replace(/versionCode\s+\d+/, `versionCode ${n}`).replace(/versionName\s+"[^"]*"/, `versionName "1.0.${n}"`));
console.log(`ok: versão 1.0.${n}`);
