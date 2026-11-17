import { chromium } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";
import { spawn } from "node:child_process";
const out = process.env.ROUTINE_EVIDENCE || "/tmp/routine-parent-browser";
fs.mkdirSync(out, { recursive: true });
const server = spawn(
  process.execPath,
  [
    "node_modules/vite/bin/vite.js",
    "preview",
    "--host",
    "127.0.0.1",
    "--port",
    "8795",
    "--strictPort",
  ],
  { stdio: "ignore" },
);
let browser;
const checks = [];
const ok = (v, label) => {
  if (!v) throw Error(label);
  checks.push(label);
};
try {
  await new Promise((r) => setTimeout(r, 500));
  browser = await chromium.launch({ channel: "chrome", headless: true });
  for (const lang of ["en", "de"]) {
    const de = lang === "de";
    const context = await browser.newContext({
      locale: lang,
      acceptDownloads: true,
      viewport: { width: 1100, height: 950 },
    });
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.goto("http://127.0.0.1:8795");
    await page.locator("#age").check();
    await page.locator("#start").click();
    ok(
      (await page.locator("#count").count()) === 0,
      lang + " no task count selector",
    );
    await page.screenshot({
      path: path.join(out, lang + "-create.png"),
      fullPage: true,
      animations: "disabled",
    });
    await page
      .getByRole("button", {
        name: de ? "Selbst schreiben" : "Write my own",
        exact: true,
      })
      .click();
    await page.locator("#title").fill(de ? "Unser Morgen" : "Our morning");
    const task = page.locator('[data-kind="task"]').first();
    await task.locator("summary").first().click();
    await task
      .getByLabel("Name", { exact: true })
      .fill(de ? "Kleidung auswählen" : "Choose clothes");
    await task
      .getByLabel(de ? "Beschreibung" : "Description", { exact: true })
      .fill(
        de
          ? "Wähle gemeinsam eines von zwei Outfits."
          : "Choose between two outfits together.",
      );
    await task.getByLabel(de ? "Sterne" : "Stars", { exact: true }).fill("7");
    await task.locator('select[id^="frequency-"]').selectOption("DAILY");
    await page
      .getByRole("button", {
        name: de ? "+ Eigene Belohnung hinzufügen" : "+ Add my own reward",
        exact: true,
      })
      .click();
    const reward = page.locator('[data-kind="reward"]').first();
    await reward.locator("summary").first().click();
    await reward
      .getByLabel("Name", { exact: true })
      .fill(de ? "Spiel auswählen" : "Choose a game");
    await reward
      .getByLabel(de ? "Beschreibung" : "Description", { exact: true })
      .fill(
        de
          ? "Wähle ein Spiel für die gemeinsame Spielzeit."
          : "Choose a game for our shared playtime.",
      );
    await reward
      .getByLabel(de ? "Sterne" : "Stars", { exact: true })
      .fill("20");
    await reward.locator('[data-action="keep"]').click();
    await page
      .getByRole("button", {
        name: de ? "Ideen speichern" : "Save ideas",
        exact: true,
      })
      .click();
    ok(
      (await page.locator("#status").innerText()).includes(
        de ? "gespeichert" : "saved",
      ),
      lang + " saved",
    );
    let dp = page.waitForEvent("download");
    await page.locator("#export").click();
    await (await dp).saveAs(path.join(out, lang + "-task-only.json"));
    let json = JSON.parse(
      fs.readFileSync(path.join(out, lang + "-task-only.json")),
    );
    ok(
      json.tasks.length === 1 && json.rewards.length === 0,
      lang + " only kept exported",
    );
    await reward.locator('[data-action="keep"]').click();
    dp = page.waitForEvent("download");
    await page.locator("#export").click();
    await (await dp).saveAs(path.join(out, lang + "-rich-export.json"));
    json = JSON.parse(
      fs.readFileSync(path.join(out, lang + "-rich-export.json")),
    );
    ok(
      json.tasks[0].pointAmount === 7 &&
        json.tasks[0].limitType === "DAILY" &&
        json.tasks[0].description.length > 0 &&
        json.tasks[0].requireApproval === true &&
        json.rewards.length === 1,
      lang + " rich actual JSON download",
    );
    await page
      .getByRole("button", {
        name: de ? "Ideen speichern" : "Save ideas",
        exact: true,
      })
      .click();
    while (await page.locator(".idea-card > details[open]").count())
      await page
        .locator(".idea-card > details[open]")
        .first()
        .locator("summary")
        .first()
        .click();
    await page.screenshot({
      path: path.join(out, lang + "-selection.png"),
      fullPage: true,
      animations: "disabled",
    });
    await page.setViewportSize({ width: 320, height: 900 });
    ok(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      lang + " narrow layout",
    );
    await page.waitForTimeout(100);
    await page.screenshot({ path: path.join(out, lang + "-phone.png") });
    await page.emulateMedia({ colorScheme: "dark" });
    await page.screenshot({ path: path.join(out, lang + "-dark.png") });
    await page.addStyleTag({ content: "body{font-size:32px}" });
    ok(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      lang + " 200 percent text",
    );
    await page.evaluate(async () => {
      await navigator.serviceWorker.ready;
      await new Promise((r) => {
        if (navigator.serviceWorker.controller) r();
        else
          navigator.serviceWorker.addEventListener("controllerchange", r, {
            once: true,
          });
      });
    });
    await context.setOffline(true);
    await page.reload();
    ok(
      (await page
        .locator('[data-kind="task"] [data-action="keep"][aria-pressed="true"]')
        .count()) === 1,
      lang + " kept state restored offline",
    );
    await page
      .getByRole("button", {
        name: de ? "Gespeicherte Routinen" : "Saved routines",
        exact: true,
      })
      .click();
    await page
      .getByRole("button", { name: de ? "Öffnen" : "Open", exact: true })
      .click();
    ok(
      (await page
        .locator(
          '[data-kind="reward"] [data-action="keep"][aria-pressed="true"]',
        )
        .count()) === 1,
      lang + " saved reward restored",
    );
    await page.locator('[data-kind="reward"] [data-action="discard"]').click();
    await page.reload();
    ok(
      (await page.locator('[data-kind="reward"]').count()) === 0,
      lang + " discarded stays removed",
    );
    ok(errors.length === 0, lang + " no browser errors");
    await context.close();
  }
  fs.writeFileSync(
    path.join(out, "browser-results.json"),
    JSON.stringify({ at: new Date().toISOString(), checks }, null, 2),
  );
  console.log(JSON.stringify({ passed: checks.length, out }));
} finally {
  await browser?.close();
  server.kill();
}
