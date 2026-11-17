import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
const root = "dist";
const files = fs
  .readdirSync(root, { recursive: true })
  .filter((f) => fs.statSync(path.join(root, f)).isFile() && f !== "sw.js")
  .map((f) => "./" + f);
const version = crypto
  .createHash("sha256")
  .update(files.map((f) => fs.readFileSync(path.join(root, f))).join(""))
  .digest("hex")
  .slice(0, 16);
fs.writeFileSync(
  path.join(root, "sw.js"),
  `const CACHE='routine-web-${version}';const FILES=${JSON.stringify(["./", ...files])};
self.addEventListener('install',e=>e.waitUntil(caches.open(CACHE).then(c=>c.addAll(FILES))));
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('routine-web-')&&k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{const url=new URL(e.request.url);if(e.request.method!=='GET'||url.origin!==self.location.origin)return;const permitted=FILES.some(f=>new URL(f,self.registration.scope).href===url.href);if(!permitted)return;e.respondWith(caches.open(CACHE).then(async c=>(await c.match(e.request))||fetch(e.request)));});`,
);
