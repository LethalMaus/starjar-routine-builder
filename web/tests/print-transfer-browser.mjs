import { chromium } from "@playwright/test";
import { spawn } from "node:child_process";
import fs from "node:fs";
import { fullTask } from "./fixtures.js";
import { blankPlan, newItem } from "../src/parent-plan.js";
import { exportEditablePlan } from "../src/transfer.js";
const out = process.env.ROUTINE_EVIDENCE || "/tmp/routine-print-transfer";
fs.mkdirSync(out, { recursive: true });
const server = spawn(
  process.execPath,
  [
    "node_modules/vite/bin/vite.js",
    "preview",
    "--host",
    "127.0.0.1",
    "--port",
    "8814",
    "--strictPort",
  ],
  { stdio: "ignore" },
);
let browser;
const results = [];
try {
  await new Promise((r) => setTimeout(r, 600));
  browser = await chromium.launch({ channel: "chrome", headless: true });
  for (const language of ["en", "de"]) {
    const c = await browser.newContext({
      locale: language,
      acceptDownloads: true,
    });
    const p = await c.newPage();
    await p.goto("http://127.0.0.1:8814");
    await p.locator("#age").check();
    await p.locator("#start").click();
    const plan = blankPlan(
      language,
      language === "de" ? "Unsere Woche" : "Our week",
    );
    plan.items = [
      {
        ...fullTask,
        name: language === "de" ? "Kleidung auswählen" : "Choose clothes",
        description:
          language === "de"
            ? "Wähle eines von zwei Outfits."
            : "Choose one of two outfits.",
      },
      {
        ...newItem("reward"),
        name: language === "de" ? "Ein Spiel auswählen" : "Choose a game",
      },
      { ...newItem("task"), name: "DO NOT PRINT", state: "suggested" },
    ];
    const fc = p.waitForEvent("filechooser");
    await p.locator("#open-plan").click();
    await (
      await fc
    ).setFiles({
      name: "plan.json",
      mimeType: "application/json",
      buffer: Buffer.from(exportEditablePlan(plan)),
    });
    await p.locator("#accept-import").waitFor();
    if (await p.evaluate(() => localStorage.getItem("routine-draft-v2")))
      throw Error("Imported without review");
    await p.locator("#accept-import").click();
    await p.evaluate(() => (window.print = () => {}));
    await p.locator("#print-plan").click();
    await p.emulateMedia({ media: "print" });
    if (await p.locator("#app").isVisible()) throw Error("App UI printed");
    if (
      await p
        .locator("#print-root")
        .innerText()
        .then((t) => t.includes("DO NOT PRINT"))
    )
      throw Error("Unchosen item printed");
    await p.pdf({
      path: out + "/" + language + "-weekly-plan.pdf",
      format: "A4",
      printBackground: true,
      preferCSSPageSize: true,
    });
    await p.screenshot({
      path: out + "/" + language + "-print.png",
      fullPage: true,
      animations: "disabled",
    });
    await p.emulateMedia({ media: "screen" });
    await p
      .locator("#save-editable")
      .evaluate((n) => (n.closest("details").open = true));
    const dl = p.waitForEvent("download");
    await p.locator("#save-editable").click();
    await (await dl).saveAs(out + "/" + language + "-editable.json");
    const saved = JSON.parse(
      fs.readFileSync(out + "/" + language + "-editable.json"),
    );
    if (
      saved.plan.items.length !== 3 ||
      saved.plan.items[0].badges.length !== 20
    )
      throw Error("Plan transfer lost data");
    results.push({
      language,
      reviewRequired: true,
      pdf: true,
      editableRoundTrip: true,
    });
    // Exercise pagination, long wrapping and repeated table headings.
    const longPlan = blankPlan(language, language === "de" ? "Unser ausführlicher Wochenplan" : "Our detailed weekly plan");
    longPlan.items = Array.from({ length: 30 }, (_, i) => ({
      ...newItem(i < 22 ? "task" : "reward"),
      name: `${i < 22 ? "TASK" : "REWARD"}-${String(i + 1).padStart(2, "0")} ` +
        (language === "de" ? "Gemeinsam einen kleinen Schritt schaffen" : "Take one small step together"),
      description: (language === "de"
        ? "Wir besprechen gemeinsam, was heute gut passt. Kleine Schritte helfen, und bei Bedarf machen wir eine Pause. "
        : "Agree together what feels manageable today. Small steps help, and we can take a break whenever needed. ").repeat(4),
    }));
    p.once("dialog", (dialog) => dialog.accept());
    await p.locator("nav button").first().click();
    const longChooser = p.waitForEvent("filechooser");
    await p.locator("#open-plan").click();
    await (await longChooser).setFiles({ name: "long-plan.json", mimeType: "application/json", buffer: Buffer.from(exportEditablePlan(longPlan)) });
    await p.locator("#accept-import").click();
    await p.locator("#print-plan").click();
    await p.emulateMedia({ media: "print" });
    if (await p.locator("#print-root tbody tr").count() !== 22 || await p.locator("#print-root li").count() !== 8)
      throw Error("Long print lost selected items");
    await p.pdf({ path: out + "/" + language + "-multipage.pdf", format: "A4", printBackground: true, preferCSSPageSize: true });
    results.push({ language, multipageTasks: 22, multipageRewards: 8 });
    await c.close();
  }
  fs.writeFileSync(
    out + "/results.json",
    JSON.stringify({ pass: true, results }, null, 2),
  );
  console.log(JSON.stringify({ pass: true, results }));
} finally {
  await browser?.close();
  server.kill();
}
