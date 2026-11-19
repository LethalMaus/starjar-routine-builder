import { test } from "node:test";
import assert from "node:assert/strict";
import { localPlanningContext, localPlanningResult, supportsLocalPlanning } from "../src/local-planning.js";
import { blankPlan, mergeSuggestions, discardItem, exportPlan } from "../src/parent-plan.js";
import { readPlanFile } from "../src/transfer.js";
const choices = { badgeCount: 0, frequency: false, streaks: false, reminders: false, urgency: false };
const raw = { tasks: [{ name: "Open the workbook", description: "Find the first question together." }], rewards: [{ name: "Choose a board game", description: "Pick a game to play together." }] };

test("simple local ideas receive importable editable defaults without trusting model metadata", () => {
  const result = localPlanningResult(raw, "en", choices);
  assert.equal(result.items[0].pointAmount, 5);
  assert.equal(result.items[1].pointAmount, 20);
  assert.ok(result.items.every(i => i.requireApproval && i.limitType === "NONE"));
  const plan = mergeSuggestions(blankPlan("en", "Homework"), result).plan;
  plan.items.forEach(i => i.state = "kept");
  assert.equal(readPlanFile(exportPlan(plan)).items.length, 2);
  assert.throws(() => localPlanningResult({ ...raw, tasks: [{ ...raw.tasks[0], pointAmount: 100000 }] }, "en", choices), /RESPONSE/);
});

test("malformed, excessive and empty local outputs do not become invented suggestions", () => {
  for (const bad of [null, { ...raw, tasks: Array(7).fill(raw.tasks[0]) }, { ...raw, rewards: null }, { ...raw, tasks: [{name:"",description:"test"}] }, { ...raw, tasks: [{name:"x",description:"x".repeat(181)}] }, { tasks: [], rewards: [] }])
    assert.throws(() => localPlanningResult(bad, "de", choices));
});

test("local follow-ups preserve edits and exclude discarded names", () => {
  const result = localPlanningResult(raw, "en", choices);
  let plan = mergeSuggestions(blankPlan("en", "Homework"), result).plan;
  plan.items[0].pointAmount = 9;
  plan.items[0].state = "kept";
  plan = discardItem(plan, plan.items[1].id);
  const merged = mergeSuggestions(plan, result).plan;
  assert.deepEqual(merged, plan);
});

test("compact context includes current and discarded ideas but omits product metadata", () => {
  const context = localPlanningContext({goal:"Homework",feedback:"Start smaller",ageBand:"primary",availableMinutes:20,constraints:[],existing:[{kind:"task",name:"Open book",pointAmount:9,icon:"Book"}],discarded:[{kind:"reward",name:"Read story"}],iconIds:["Book"]});
  assert.equal(context.difficulty,"Homework");
  assert.deepEqual(context.existing,[{kind:"task",name:"Open book"}]);
  assert.deepEqual(context.discarded,[{kind:"reward",name:"Read story"}]);
  assert.ok(!JSON.stringify(context).includes("pointAmount"));
  assert.ok(!JSON.stringify(context).includes("iconIds"));
});

test("unvalidated local languages are disabled", () => {
  assert.equal(supportsLocalPlanning("en"), true);
  assert.equal(supportsLocalPlanning("de"), false);
  assert.equal(supportsLocalPlanning("fr"), false);
});
