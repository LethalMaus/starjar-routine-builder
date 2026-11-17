// Opt-in: two real cloud requests; requires configured Firebase and can incur charges.
import { chromium } from "@playwright/test";
import fs from "node:fs";
import { spawn } from "node:child_process";
const out =
  process.env.ROUTINE_EVIDENCE || "/tmp/routine-parent-cloud-evidence";
fs.mkdirSync(out, { recursive: true });
const server = spawn(
  process.execPath,
  [
    "node_modules/vite/bin/vite.js",
    "preview",
    "--host",
    "127.0.0.1",
    "--port",
    "8805",
    "--strictPort",
  ],
  { stdio: "ignore" },
);
let browser;
const result = { at: new Date().toISOString(), responses: [] };
try {
  await new Promise((r) => setTimeout(r, 500));
  browser = await chromium.launch({
    ...(process.env.ROUTINE_CHROME_PATH
      ? { executablePath: process.env.ROUTINE_CHROME_PATH }
      : { channel: "chrome" }),
    headless: true,
  });
  const context = await browser.newContext({
    locale: "en",
    acceptDownloads: true,
    viewport: { width: 1100, height: 950 },
  });
  const page = await context.newPage();
  page.on("request", (r) => {
    if (new URL(r.url()).hostname === "firebasevertexai.googleapis.com")
      result.appCheck = Boolean(r.headers()["x-firebase-appcheck"]);
  });
  page.on("response", (r) => {
    if (new URL(r.url()).hostname === "firebasevertexai.googleapis.com")
      result.responses.push({ status: r.status() });
  });
  page.on("console", (m) => {
    if (m.text().startsWith("Routine generation code:"))
      result.errorCode = m.text();
  });
  await page.goto("http://127.0.0.1:8805");
  await page.locator("#age").check();
  await page.locator("#start").click();
  await page
    .locator("#goal")
    .fill(
      "Getting out of bed in the morning is difficult. Once up, getting dressed is fine. They enjoy choosing music and games.",
    );
  await page.locator("#age-band").selectOption("primary");
  await page.locator("#minutes").fill("20");
  await page.locator("#cloud").check();
  await page
    .getByRole("button", { name: "Find tasks and rewards", exact: true })
    .click();
  await page.locator("#selected-count").waitFor({ timeout: 90000 });
  result.receipt = await page.locator("#receipt").innerText();
  if (!result.receipt.startsWith("Generated in the cloud") || !result.appCheck)
    throw Error("Expected protected cloud inference");
  result.initial = await page.evaluate(
    () => JSON.parse(localStorage.getItem("routine-draft-v2")).plan,
  );
  if (
    !result.initial.items.some((x) => x.kind === "task") ||
    result.initial.items.filter((x) => x.kind === "reward").length < 2
  )
    throw Error("Missing suggestions");
  await page.locator('[data-kind="task"] [data-action="keep"]').first().click();
  await page
    .locator('[data-kind="reward"] [data-action="keep"]')
    .first()
    .click();
  const discard = page
    .locator(".idea-card")
    .filter({ has: page.locator('[aria-pressed="false"]') })
    .last();
  result.discardedName = await discard.locator("h3").innerText();
  await discard.locator('[data-action="discard"]').click();
  result.before = await page.evaluate(
    () => JSON.parse(localStorage.getItem("routine-draft-v2")).plan,
  );
  await page.screenshot({ path: out + "/choices.png", fullPage: true });
  await page
    .locator("#feedback")
    .fill(
      "Give a different gentle way to leave the warm bed, and a reward involving choosing an activity together. No screens or buying things.",
    );
  await page.locator("#more-ideas").click();
  await page.waitForFunction(
    () => !document.getElementById("more-ideas").disabled,
    {},
    { timeout: 90000 },
  );
  result.after = await page.evaluate(
    () => JSON.parse(localStorage.getItem("routine-draft-v2")).plan,
  );
  if (result.after.items.length <= result.before.items.length)
    throw Error("No new suggestions added");
  for (const item of result.before.items) {
    if (
      JSON.stringify(item) !==
      JSON.stringify(result.after.items.find((x) => x.id === item.id))
    )
      throw Error("Existing choice changed");
  }
  if (result.after.items.some((x) => x.name === result.discardedName))
    throw Error("Discarded item returned");
  const dp = page.waitForEvent("download");
  await page.locator("#export").click();
  await (await dp).saveAs(out + "/selected-rich-export.json");
  result.export = JSON.parse(
    fs.readFileSync(out + "/selected-rich-export.json"),
  );
  if (
    result.export.tasks.length !==
    result.after.items.filter((x) => x.kind === "task" && x.state === "kept")
      .length
  )
    throw Error("Selection mismatch");
  await page.screenshot({ path: out + "/after-more.png", fullPage: true });
  result.pass = true;
  console.log(
    JSON.stringify({
      pass: true,
      initial: result.initial.items.length,
      after: result.after.items.length,
      responses: result.responses,
    }),
  );
} catch (e) {
  result.pass = false;
  result.error = e.message;
  console.log(
    JSON.stringify({
      pass: false,
      error: e.message,
      code: result.errorCode,
      responses: result.responses,
    }),
  );
  process.exitCode = 1;
} finally {
  fs.writeFileSync(out + "/live-results.json", JSON.stringify(result, null, 2));
  await browser?.close();
  server.kill();
}
