// Opt-in: one real paid cloud request through the UI. Never run in normal tests.
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
    "8797",
    "--strictPort",
  ],
  { stdio: "ignore" },
);
let browser;
const result = {
  at: new Date().toISOString(),
  provider: process.env.ROUTINE_DEBUG_TOKEN_FILE
    ? "private-debug-token"
    : "recaptcha-enterprise",
  responses: [],
};
try {
  await new Promise((r) => setTimeout(r, 500));
  browser = await chromium.launch({
    ...(process.env.ROUTINE_CHROME_PATH
        ? { executablePath: process.env.ROUTINE_CHROME_PATH }
        : { channel: "chrome" }),
    headless: true,
  });
  const context = await browser.newContext({
    locale: "en-GB",
    viewport: { width: 1100, height: 950 },
  });
  if (process.env.ROUTINE_DEBUG_TOKEN_FILE) {
    const token = JSON.parse(
      fs.readFileSync(process.env.ROUTINE_DEBUG_TOKEN_FILE, "utf8"),
    ).token;
    await context.addInitScript((t) => {
      self.FIREBASE_APPCHECK_DEBUG_TOKEN = t;
    }, token);
  }
  const page = await context.newPage();
  page.on("request", (r) => {
    if (new URL(r.url()).hostname === "firebasevertexai.googleapis.com")
      result.cloudRequestHadAppCheck = Boolean(
        r.headers()["x-firebase-appcheck"],
      );
  });
  page.on("response", (r) => {
    const u = new URL(r.url());
    if (
      u.hostname === "firebaseappcheck.googleapis.com" ||
      u.hostname === "firebasevertexai.googleapis.com" ||
      u.hostname === "firebaseml.googleapis.com"
    )
      result.responses.push({
        host: u.hostname,
        operation: u.pathname.split("/").pop(),
        status: r.status(),
      });
  });
  page.on("console", (m) => {
    if (m.text().startsWith("Routine generation code:"))
      result.errorCode = m.text();
  });
  await page.goto("http://127.0.0.1:8797");
  await page.locator("#age").check();
  await page.locator("#start").click();
  result.localAvailability = await page.evaluate(async () =>
    typeof LanguageModel === "undefined"
      ? "unavailable"
      : await Promise.race([
          LanguageModel.availability(),
          new Promise((r) => setTimeout(() => r("unknown"), 3500)),
        ]),
  );
  if (result.localAvailability === "available")
    throw Error(
      "Use a profile without a local model for this cloud fallback test",
    );
  await page
    .locator("#goal")
    .fill("Morning: brush teeth, pack the schoolbag, put on shoes");
  await page.locator("#cloud").check();
  await page
    .getByRole("button", { name: "Suggest a routine", exact: true })
    .click();
  for (let i = 0; i < 30; i++) {
    await page.waitForTimeout(2000);
    if (await page.locator("#receipt").count()) break;
    if (
      !(await page
        .getByRole("button", { name: "Creating suggestions…", exact: true })
        .count())
    )
      throw Error(await page.locator("#status").innerText());
  }
  result.receipt = await page.locator("#receipt").innerText();
  result.stored = await page.evaluate(() =>
    JSON.parse(localStorage.getItem("routine-draft-v1")),
  );
  if (!result.cloudRequestHadAppCheck)
    throw Error("Cloud request missing App Check header");
  if (result.stored.receipt.sdkSource !== "in_cloud")
    throw Error("Expected cloud SDK source");
  await page.screenshot({
    path: out + "/en-cloud-generated.png",
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Create a routine", exact: true })
    .click({ noWaitAfter: true, timeout: 1000 })
    .catch(() => {});
  result.pass = true;
  console.log(
    JSON.stringify({
      pass: true,
      provider: result.provider,
      receipt: result.receipt,
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
  fs.writeFileSync(
    out + "/cloud-live-" + result.provider + ".json",
    JSON.stringify(result, null, 2),
  );
  await browser?.close();
  server.kill();
}
