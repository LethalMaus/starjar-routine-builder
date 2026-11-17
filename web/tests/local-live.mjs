// Opt-in real on-device test. Use a dedicated test profile; model download may take several GB.
import { chromium } from "@playwright/test";
import fs from "node:fs";
import { spawn } from "node:child_process";
const out =
  process.env.ROUTINE_EVIDENCE || "/tmp/routine-parent-local-evidence";
fs.mkdirSync(out, { recursive: true });
const server = spawn(
  process.execPath,
  [
    "node_modules/vite/bin/vite.js",
    "preview",
    "--host",
    "127.0.0.1",
    "--port",
    "8806",
    "--strictPort",
  ],
  { stdio: "ignore" },
);
let context;
const result = { at: new Date().toISOString() };
try {
  await new Promise((r) => setTimeout(r, 500));
  context = await chromium.launchPersistentContext(
    process.env.ROUTINE_CHROME_PROFILE ||
      "/tmp/routine-parent-local-test-profile",
    {
      ...(process.env.ROUTINE_CHROME_PATH
        ? { executablePath: process.env.ROUTINE_CHROME_PATH }
        : { channel: "chrome" }),
      headless: true,
      locale: "en",
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
    if (m.text().startsWith("Routine generation code:")) {
      result.errorCode = m.text();
      console.log(m.text());
    }
  });
  await page.goto("http://127.0.0.1:8806");
  await page.evaluate(async () => {
    for (const r of await navigator.serviceWorker.getRegistrations())
      await r.unregister();
    for (const k of await caches.keys()) await caches.delete(k);
    localStorage.clear();
  });
  await page.reload();
  await page.locator("#age").check();
  await page.locator("#start").click();
  await page
    .locator("#goal")
    .fill(
      "Getting out of bed is difficult for a primary-school child. Dressing is fine. They enjoy music and games.",
    );
  await page.locator("#age-band").selectOption("primary");
  await page.locator("#minutes").fill("20");
  await page
    .getByRole("button", { name: "Find tasks and rewards", exact: true })
    .click();
  for (let i = 0; i < 120; i++) {
    await page.waitForTimeout(5000);
    if (await page.locator("#selected-count").count()) break;
    if (i % 6 === 0)
      console.log(
        "Local preparation:",
        await page.locator("#status").innerText(),
      );
    if (
      await page
        .getByRole("button", { name: "Find tasks and rewards", exact: true })
        .count()
    )
      throw Error(await page.locator("#status").innerText());
  }
  await page.locator("#selected-count").waitFor({ timeout: 1000 });
  result.receipt = await page.locator("#receipt").innerText();
  if (!result.receipt.startsWith("Generated on this device"))
    throw Error("Not local");
  result.plan = await page.evaluate(
    () => JSON.parse(localStorage.getItem("routine-draft-v2")).plan,
  );
  await page.screenshot({ path: out + "/local-options.png", fullPage: true });
  await context.setOffline(true);
  await page
    .locator("#feedback")
    .fill("Suggest a different reward involving choosing a shared game.");
  await page.locator("#more-ideas").click();
  await page.waitForFunction(
    () => !document.getElementById("more-ideas").disabled,
    {},
    { timeout: 180000 },
  );
  if ((await page.locator("#status").innerText()).includes("wrong"))
    throw Error(await page.locator("#status").innerText());
  result.offlinePlan = await page.evaluate(
    () => JSON.parse(localStorage.getItem("routine-draft-v2")).plan,
  );
  if (result.offlinePlan.items.length <= result.plan.items.length)
    throw Error("No offline ideas added");
  for (const item of result.plan.items) {
    if (
      JSON.stringify(item) !==
      JSON.stringify(result.offlinePlan.items.find((x) => x.id === item.id))
    )
      throw Error("Existing idea changed offline");
  }
  if (
    !(await page.locator("#receipt").innerText()).startsWith(
      "Generated on this device",
    )
  )
    throw Error("Expected local offline source");
  result.pass = true;
  console.log(
    JSON.stringify({
      pass: true,
      receipt: result.receipt,
      initial: result.plan.items.length,
      offline: result.offlinePlan.items.length,
    }),
  );
} catch (e) {
  result.pass = false;
  result.error = e.message;
  console.log(
    JSON.stringify({ pass: false, error: e.message, code: result.errorCode }),
  );
  process.exitCode = 1;
} finally {
  fs.writeFileSync(
    out + "/local-results.json",
    JSON.stringify(result, null, 2),
  );
  await context?.close();
  server.kill();
}
