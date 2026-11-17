import { test } from "node:test";
import assert from "node:assert/strict";
import {
  blankPlan,
  newItem,
  parseSuggestions,
  mergeSuggestions,
  discardItem,
  exportPlan,
  savePlan,
  loadPlans,
  cleanPlan,
  contextFor,
  validateParentRequest,
} from "../src/parent-plan.js";
const task = (name = "Choose clothes") => ({
  name,
  description: "Pick one of two outfits together.",
  pointAmount: 5,
  icon: "School",
  color: "BLUE",
  limitType: "DAILY",
  limitValue: 1,
  requireApproval: true,
});
const response = (
  tasks = [task()],
  rewards = [
    {
      ...task("Choose a game"),
      pointAmount: 15,
      icon: "SmartToy",
      limitType: "NONE",
    },
  ],
) =>
  parseSuggestions({
    tasks,
    rewards,
    reason: "A small choice can make the next step clearer.",
  });
const store = () => {
  const m = new Map();
  return { getItem: (k) => m.get(k) ?? null, setItem: (k, v) => m.set(k, v) };
};
test("AI chooses bounded numbers of separate task and reward options", () => {
  const d = response([task("Clothes"), task("Shoes")]);
  assert.equal(d.items.length, 3);
  assert.throws(() => response(Array(7).fill(task())), /TASK_COUNT/);
  assert.throws(() => response([], Array(4).fill(task())), /TASK_COUNT/);
});
test("more suggestions preserve all existing edits and kept state", () => {
  let p = mergeSuggestions(blankPlan("en", "Morning"), response()).plan;
  p.items[0].state = "kept";
  p.items[0].pointAmount = 7;
  p.items[0].description = "An edited description";
  const previous = structuredClone(p.items);
  const next = mergeSuggestions(p, response([task("Clothes"), task()]));
  assert.deepEqual(next.plan.items.slice(0, previous.length), previous);
  assert.equal(next.added, 1);
});
test("discarded original and edited names cannot return via merge", () => {
  let p = mergeSuggestions(blankPlan("en", "Morning"), response()).plan;
  p.items[0].name = "My edited task";
  p = discardItem(p, p.items[0].id);
  const next = mergeSuggestions(p, response([task(), task("MY EDITED TASK")]));
  assert.equal(next.added, 0);
  assert.equal(next.plan.items.length, 1);
  assert.equal(contextFor(p).discarded.length, 2);
});
test("export includes rich fields and only chosen tasks/rewards", () => {
  let p = mergeSuggestions(
    blankPlan("en", "Morning"),
    response([task(), task("Unchosen")]),
  ).plan;
  p.items
    .filter((x) => x.name !== "Unchosen")
    .forEach((x) => (x.state = "kept"));
  p.goal = "private";
  p.items[0].internal = "private";
  const out = JSON.parse(exportPlan(p));
  assert.deepEqual(out.tasks, [task()]);
  assert.equal(out.rewards.length, 1);
  assert.equal(out.tasks[0].limitValue, 1);
  assert.ok(!JSON.stringify(out).includes("private"));
  assert.ok(!JSON.stringify(out).includes("originalName"));
});
test("export rejects no selection, duplicate names, bad values and unsupported enums", () => {
  const p = mergeSuggestions(blankPlan("en", "Morning"), response()).plan;
  assert.throws(() => exportPlan(p), /SELECTION/);
  p.items[0].state = "kept";
  for (const patch of [
    { pointAmount: "1.5" },
    { limitValue: 0 },
    { icon: "Unknown" },
    { color: "ORANGE" },
    { requireApproval: "true" },
  ]) {
    const x = structuredClone(p);
    Object.assign(x.items[0], patch);
    assert.throws(() => exportPlan(x));
  }
  p.items.push({ ...p.items[0], id: "different" });
  assert.throws(() => exportPlan(p), /DUPLICATE/);
});
test("saved choices and discarded exclusions survive reload without prompt context", () => {
  const s = store();
  let p = mergeSuggestions(blankPlan("en", "Morning"), response()).plan;
  p = discardItem(p, p.items[1].id);
  p.items[0].state = "kept";
  p.goal = "private goal";
  p.feedback = "private feedback";
  savePlan(s, p);
  const saved = loadPlans(s)[0];
  assert.equal(saved.items[0].state, "kept");
  assert.equal(saved.discarded.length, 1);
  assert.ok(!s.getItem("routine-library-v2").includes("private"));
});
test("legacy routines migrate without overwriting old storage", () => {
  const s = store();
  const raw = JSON.stringify([
    {
      id: "old",
      title: "Morning",
      language: "en",
      tasks: [{ name: "Brush teeth", pointAmount: 5 }],
      reward: { name: "Choose a game", pointAmount: 20 },
    },
  ]);
  s.setItem("routine-library-v1", raw);
  const p = loadPlans(s)[0];
  assert.equal(p.items.length, 2);
  assert.ok(p.items.every((x) => x.state === "kept"));
  savePlan(s, p);
  assert.equal(s.getItem("routine-library-v1"), raw);
  assert.equal(JSON.parse(exportPlan(p)).rewards[0].pointAmount, 20);
});
test("corrupt storage is not replaced when saving", () => {
  const s = store();
  s.setItem("routine-library-v2", "broken");
  assert.throws(() => savePlan(s, blankPlan("en", "Morning")));
  assert.equal(s.getItem("routine-library-v2"), "broken");
});
test("parent request has no chosen task count and accepts broad optional age", () => {
  const r = {
    goal: "Getting out of bed is difficult",
    language: "en",
    ageBand: "primary",
    availableMinutes: 20,
    feedback: "",
    constraints: [],
    existing: [],
    discarded: [],
  };
  assert.equal(validateParentRequest(r), r);
  assert.throws(
    () => validateParentRequest({ ...r, ageBand: "birthday" }),
    /AGE/,
  );
  assert.throws(
    () => validateParentRequest({ ...r, availableMinutes: 0 }),
    /TIME/,
  );
});
test("no new ideas is valid while unrelated requests require clarification", () => {
  assert.equal(
    parseSuggestions({
      tasks: [],
      rewards: [],
      reason: "There are no useful additions yet.",
    }).items.length,
    0,
  );
  assert.throws(
    () => parseSuggestions({ tasks: [], rewards: [], reason: "" }),
    /NEEDS_INPUT/,
  );
});
