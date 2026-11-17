// Opt-in real Firebase on-device test. Dedicated profile; no personal Chrome data.
import { chromium } from "@playwright/test";
import fs from "node:fs";
import { spawn } from "node:child_process";
const out = process.env.ROUTINE_EVIDENCE || "/tmp/routine-web-evidence";
fs.mkdirSync(out, { recursive: true });
const server = spawn(
  process.execPath,
  [
    "node_modules/vite/bin/vite.js",
    "preview",
    "--host",
    "127.0.0.1",
    "--port",
    "8796",
    "--strictPort",
  ],
  { stdio: "ignore" },
);
let context;
try {
  await new Promise((r) => setTimeout(r, 700));
  context = await chromium.launchPersistentContext(
    process.env.ROUTINE_CHROME_PROFILE || "/tmp/routine-web-model-profile",
    {
      ...(process.env.ROUTINE_CHROME_PATH
        ? { executablePath: process.env.ROUTINE_CHROME_PATH }
        : { channel: "chrome" }),
      headless: true,
      locale: "en-GB",
      ignoreDefaultArgs: [
        "--disable-component-update",
        "--disable-background-networking",
      ],
      args: [
        "--enable-features=OnDeviceModelConversationBackend,OptimizationGuideManifestBroker,AIApiFoundationalModel:model_version/v4",
      ],
      viewport: { width: 1100, height: 950 },
    },
  );
  const page = await context.newPage();
  page.on("console", (m) => {
    if (m.text().startsWith("Routine generation code:")) console.log(m.text());
  });
  await page.goto("http://127.0.0.1:8796");
  // Refresh the previous task-only service worker to the build under test.
  await page.evaluate(async () => {
    for (const r of await navigator.serviceWorker.getRegistrations())
      await r.unregister();
    for (const k of await caches.keys()) await caches.delete(k);
  });
  await page.reload();
  console.log(
    "capability",
    await page.evaluate(async () => ({
      agent: navigator.userAgent,
      exposed: typeof LanguageModel,
      availability:
        typeof LanguageModel === "undefined"
          ? "unavailable"
          : await LanguageModel.availability(),
    })),
  );
  if (await page.locator("#age").count()) {
    await page.locator("#age").check();
    await page.locator("#start").click();
  }
  page.on("dialog", (d) => d.accept());
  await page
    .getByRole("button", { name: "Create a routine", exact: true })
    .click();
  await page
    .locator("#goal")
    .fill("Morning: brush teeth, pack schoolbag, put on shoes");
  await page
    .getByRole("button", { name: "Suggest a routine", exact: true })
    .click();
  for (let i = 0; i < 120; i++) {
    await page.waitForTimeout(5000);
    if (await page.locator("#receipt").count()) break;
    const status = await page.locator("#status").innerText();
    if (i % 6 === 0) console.log("local preparation", status);
    if (
      !(await page
        .getByRole("button", { name: "Creating suggestions…", exact: true })
        .count())
    )
      throw Error(status);
  }
  await page.locator("#receipt").waitFor({ timeout: 1000 });
  const first = await page.locator("#receipt").innerText();
  if (!first.startsWith("Generated on this device")) throw Error(first);
  console.log("local", first);
  await page.screenshot({
    path: out + "/en-local-generated.png",
    fullPage: true,
  });
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
  });
  await context.setOffline(true);
  await page.reload();
  await page
    .getByRole("button", { name: "Create a routine", exact: true })
    .click();
  await page.locator("#language").selectOption("de");
  await page
    .locator("#goal")
    .fill("Abends: Spielzeug aufräumen, Schlafanzug anziehen, Buch lesen");
  await page
    .getByRole("button", { name: "Routine vorschlagen", exact: true })
    .click();
  await page.locator("#receipt").waitFor({ timeout: 120000 });
  const second = await page.locator("#receipt").innerText();
  if (!second.startsWith("Auf diesem Gerät erstellt")) throw Error(second);
  await page.screenshot({
    path: out + "/de-offline-local-generated.png",
    fullPage: true,
  });
  const values = await page
    .locator(".item input")
    .evaluateAll((nodes) => nodes.map((n) => n.value));
  const savedReceipt = await page.evaluate(
    () => JSON.parse(localStorage.getItem("routine-draft-v1")).receipt,
  );
  await page
    .getByRole("button", { name: "Routine erstellen", exact: true })
    .click();
  await page
    .locator("#goal")
    .fill("Zähne putzen, Tasche packen, Schuhe anziehen");
  await page
    .getByRole("button", { name: "Routine vorschlagen", exact: true })
    .click();
  await page.getByRole("button", { name: "Abbrechen", exact: true }).click();
  await page
    .getByRole("button", { name: "Selbst schreiben", exact: true })
    .click();
  await page.locator("#title").fill("Nach Abbruch behalten");
  await page.waitForTimeout(5000);
  if ((await page.locator("#title").inputValue()) !== "Nach Abbruch behalten")
    throw Error("Late result replaced edits");
  fs.writeFileSync(
    out + "/local-live.json",
    JSON.stringify(
      {
        at: new Date().toISOString(),
        first,
        second,
        germanOfflineValues: values,
        savedReceipt,
        cancelPreservedNewEdits: true,
        offlineReload: true,
      },
      null,
      2,
    ),
  );
  console.log("offline local passed", second);
} finally {
  await context?.close();
  server.kill();
}
