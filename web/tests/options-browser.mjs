import { chromium } from "@playwright/test";
import fs from "node:fs";
import { spawn } from "node:child_process";
import { fullTask } from "./fixtures.js";
import { blankPlan, newItem } from "../src/parent-plan.js";
const out = process.env.ROUTINE_EVIDENCE || "/tmp/routine-options-browser";
fs.mkdirSync(out, { recursive: true });
const server = spawn(
  process.execPath,
  [
    "node_modules/vite/bin/vite.js",
    "preview",
    "--host",
    "127.0.0.1",
    "--port",
    "8809",
    "--strictPort",
  ],
  { stdio: "ignore" },
);
let browser;
const results = [];
try {
  await new Promise((r) => setTimeout(r, 500));
  browser = await chromium.launch({ channel: "chrome", headless: true });
  for (const lang of ["en", "de"]) {
    const c = await browser.newContext({
      locale: lang,
      acceptDownloads: true,
      viewport: { width: 1100, height: 950 },
    });
    const p = await c.newPage();
    const errors = [];
    p.on("pageerror", (e) => errors.push(e.message));
    await p.goto("http://127.0.0.1:8809");
    await p.locator("#age").check();
    await p.locator("#start").click();
    await p.locator("#goal").fill("Getting ready for school");
    await p
      .locator("#badge-count")
      .evaluate((n) => (n.closest("details").open = true));
    await p.locator("#badge-count").fill("10");
    await p.locator("form button[type=submit]").click();
    await p.waitForFunction(
      () =>
        document.getElementById("status").textContent.includes("Cloud") ||
        document.getElementById("status").textContent.includes("cloud"),
    );
    const plan = blankPlan(lang, "StarJar options");
    plan.items = [
      structuredClone(fullTask),
      { ...newItem("reward"), name: "Choose a game" },
    ];
    await p.evaluate((plan) => {
      localStorage.setItem("routine-adult-v1", "yes");
      localStorage.setItem("routine-draft-v2", JSON.stringify({ plan }));
    }, plan);
    await p.reload();
    const card = p.locator("[data-kind=task]");
    await card.locator("summary").first().click();
    await card.locator(".icon-picker summary").click();
    if ((await card.locator(".icon-grid button").count()) !== 167)
      throw Error("Missing icons");
    await p.evaluate(() => document.fonts.ready);
    await p.screenshot({
      path: out + "/" + lang + "-icons.png",
      fullPage: true,
      animations: "disabled",
    });
    await card.locator('.icon-grid button[title="Dishwasher"]').click();
    await card.locator(".badge-editor summary").first().click();
    await card
      .getByLabel(lang === "de" ? "Name des Abzeichens" : "Badge title", {
        exact: true,
      })
      .first()
      .fill(lang === "de" ? "Erster Erfolg" : "First achievement");
    await card
      .getByLabel(
        lang === "de" ? "Uhrzeit (optional)" : "Reminder time (optional)",
        { exact: true },
      )
      .fill("08:15");
    await card
      .getByLabel(
        lang === "de" ? "Uhrzeit (optional)" : "Reminder time (optional)",
        { exact: true },
      )
      .blur();
    const dp = p.waitForEvent("download");
    await p.locator("#export").click();
    await (await dp).saveAs(out + "/" + lang + "-full-export.json");
    const doc = JSON.parse(
      fs.readFileSync(out + "/" + lang + "-full-export.json"),
    );
    if (
      doc.tasks[0].badges.length !== 20 ||
      doc.tasks[0].reminder.time !== "08:15" ||
      doc.tasks[0].icon !== "Dishwasher"
    )
      throw Error("Lost options");
    if ("badges" in doc.rewards[0]) throw Error("Reward leaked task fields");
    await p.setViewportSize({ width: 320, height: 850 });
    await p.screenshot({
      path: out + "/" + lang + "-phone-options.png",
      fullPage: true,
      animations: "disabled",
    });
    if (
      await p.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      )
    )
      throw Error("Horizontal overflow");
    await c.setOffline(true);
    await p.reload();
    const restored = await p.evaluate(
      () => JSON.parse(localStorage.getItem("routine-draft-v2")).plan,
    );
    if (restored.items[0].badges[0].title !== doc.tasks[0].badges[0].title)
      throw Error("Lost saved badge");
    if (errors.length) throw Error(errors.join());
    results.push({
      lang,
      icons: 167,
      badges: 20,
      richExport: true,
      offline: true,
      narrow: true,
    });
    await c.close();
  }
  console.log(JSON.stringify({ pass: true, results }));
  fs.writeFileSync(
    out + "/results.json",
    JSON.stringify({ pass: true, results }, null, 2),
  );
} finally {
  await browser?.close();
  server.kill();
}
