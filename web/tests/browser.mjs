import { chromium } from "@playwright/test";
import fs from "node:fs";
import { spawn } from "node:child_process";
import path from "node:path";
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
    "8795",
    "--strictPort",
  ],
  { stdio: "ignore" },
);
let browser;
const checks = [];
function ok(value, label) {
  if (!value) throw Error(label);
  checks.push(label);
}
try {
  for (let i = 0; i < 50; i++) {
    try {
      await fetch("http://127.0.0.1:8795");
      break;
    } catch {
      await new Promise((r) => setTimeout(r, 100));
    }
  }
  browser = await chromium.launch({
    ...(process.env.ROUTINE_CHROME_PATH
        ? { executablePath: process.env.ROUTINE_CHROME_PATH }
        : { channel: "chrome" }),
    headless: true,
  });
  for (const language of ["en", "de"]) {
    const de = language === "de";
    const context = await browser.newContext({
      locale: language,
      acceptDownloads: true,
      viewport: { width: 1100, height: 950 },
    });
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.goto("http://127.0.0.1:8795");
    await page.locator("#age").check();
    await page.locator("#start").click();
    await page
      .getByRole("button", {
        name: de ? "Selbst schreiben" : "Write my own",
        exact: true,
      })
      .click();
    await page.locator("#title").fill(de ? "Mein Morgen" : "My morning");
    const tasks = de
      ? ["Zähne putzen", "Schultasche packen", "Schuhe anziehen"]
      : ["Brush teeth", "Pack schoolbag", "Put on shoes"];
    for (let i = 0; i < 3; i++)
      await page
        .getByRole("textbox", {
          name: (de ? "Aufgabe" : "Task") + " " + (i + 1),
          exact: true,
        })
        .fill(tasks[i]);
    await page
      .getByRole("textbox", { name: de ? "Belohnung" : "Reward", exact: true })
      .fill(de ? "Gemeinsam lesen" : "Read together");
    await page
      .getByRole("textbox", {
        name: de ? "Aufgabe 1 Sterne" : "Task 1 Stars",
        exact: true,
      })
      .fill("7");
    await page
      .getByRole("button", {
        name: de ? "Routine speichern" : "Save routine",
        exact: true,
      })
      .click();
    ok(
      (await page.locator("#status").innerText()).includes(
        de ? "gespeichert" : "saved",
      ),
      language + " saved",
    );
    const dp = page.waitForEvent("download");
    await page
      .getByRole("button", {
        name: de ? "Für StarJar exportieren" : "Export to StarJar",
        exact: true,
      })
      .click();
    await (await dp).saveAs(path.join(out, language + "-export.json"));
    const exported = JSON.parse(
      fs.readFileSync(path.join(out, language + "-export.json")),
    );
    ok(
      exported.tasks[0].pointAmount === 7 &&
        exported.format === "starjar-templates" &&
        exported.tasks.length === 3,
      language + " real JSON download",
    );
    await page.screenshot({
      path: path.join(out, language + "-desktop-editor.png"),
      fullPage: true,
    });
    await page.setViewportSize({ width: 320, height: 640 });
    ok(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      language + " narrow layout",
    );
    await page.setViewportSize({
      width: 320,
      height: 900,
    });
    await page.locator("#title").scrollIntoViewIfNeeded();
    await page.waitForTimeout(200);
    await page.screenshot({
      path: path.join(out, language + "-phone-editor.png"),
    });
    await page.emulateMedia({ colorScheme: "dark" });
    await page.screenshot({
      path: path.join(out, language + "-phone-dark.png"),
    });
    await page.addStyleTag({ content: "body{font-size:32px}" });
    ok(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      language + " enlarged text no horizontal overflow",
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
      (await page.locator("#title").inputValue()) ===
        (de ? "Mein Morgen" : "My morning"),
      language + " offline reload restores draft",
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
        .getByRole("textbox", {
          name: (de ? "Aufgabe" : "Task") + " 1",
          exact: true,
        })
        .inputValue()) === tasks[0],
      language + " saved reopen offline",
    );
    ok(errors.length === 0, language + " no browser errors");
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
