import { test } from "node:test";
import assert from "node:assert/strict";
import {
  exportEditablePlan,
  readPlanFile,
  shareTemplates,
} from "../src/transfer.js";
import { blankPlan, newItem, exportPlan } from "../src/parent-plan.js";
import { fullTask } from "./fixtures.js";
const plan = () => ({
  ...blankPlan("en", "Our week"),
  items: [
    structuredClone(fullTask),
    { ...newItem("reward"), name: "Choose a game" },
  ],
  discarded: [{ kind: "task", name: "Old idea", originalName: "Old idea" }],
});
test("editable transfer preserves decisions and excludes original prompts", () => {
  const p = plan();
  p.items[1].state = "suggested";
  p.goal = "private original";
  const restored = readPlanFile(exportEditablePlan(p));
  assert.equal(restored.items[0].badges.length, 20);
  assert.equal(restored.items[1].state, "suggested");
  assert.equal(restored.discarded[0].name, "Old idea");
  assert.ok(!exportEditablePlan(p).includes("private original"));
});
test("StarJar template import and export preserve all supported fields", () => {
  const p = plan();
  const text = exportPlan(p);
  const restored = readPlanFile(text);
  assert.deepEqual(JSON.parse(exportPlan(restored)), JSON.parse(text));
});
test("malformed or oversized incoming files do not become plans", () => {
  for (const text of [
    "{}",
    "{",
    "x".repeat(200001),
    JSON.stringify({
      format: "starjar-templates",
      version: 2,
      tasks: [],
      rewards: [],
    }),
    JSON.stringify({
      format: "starjar-templates",
      version: 1,
      tasks: [{ name: "Invalid", pointAmount: -1 }],
      rewards: [],
    }),
  ])
    assert.throws(() => readPlanFile(text));
});
test("file sharing handles unsupported browsers and cancellation without fallback writes", async () => {
  assert.equal(await shareTemplates(plan(), {}), "unsupported");
  let count = 0;
  assert.equal(
    await shareTemplates(plan(), {
      canShare: () => true,
      share: async ({ files }) => {
        count++;
        assert.equal(files[0].name, "starjar-templates.json");
        throw new DOMException("cancelled", "AbortError");
      },
    }),
    "cancelled",
  );
  assert.equal(count, 1);
});
