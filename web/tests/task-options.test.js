import { test } from "node:test";
import assert from "node:assert/strict";
import {
  taskOptions,
  featureChoices,
  enforceChoices,
} from "../src/task-options.js";
import {
  newItem,
  blankPlan,
  cleanPlan,
  exportPlan,
  parseSuggestions,
  icons,
} from "../src/parent-plan.js";
import { fullTask } from "./fixtures.js";

test("all StarJar icons are selectable and full task fields survive storage and export", () => {
  assert.equal(icons.length, 167);
  for (const id of [
    "Flatware",
    "Dishwasher",
    "CleaningServices",
    "StarOutlined",
  ])
    assert.ok(icons.includes(id));
  const plan = blankPlan("en", "Morning");
  plan.items = [fullTask];
  const out = JSON.parse(exportPlan(cleanPlan(plan))).tasks[0];
  assert.deepEqual(taskOptions(out), taskOptions(fullTask));
  assert.equal(out.icon, "CleanHands");
  assert.equal(out.badges.length, 20);
});
test("unsupported task settings never leak onto rewards", () => {
  const plan = blankPlan("en", "Rewards");
  plan.items = [{ ...fullTask, kind: "reward" }];
  const out = JSON.parse(exportPlan(plan)).rewards[0];
  for (const k of [
    "badges",
    "streakBonus",
    "reminder",
    "isUrgent",
    "dueByDate",
    "urgencyBonus",
  ])
    assert.ok(!(k in out));
});
test("invalid badge and schedule settings fail before export", () => {
  for (const change of [
    { badges: Array(21).fill(fullTask.badges[0]) },
    { badges: [fullTask.badges[0], fullTask.badges[0]] },
    { badges: [{ ...fullTask.badges[0], title: "" }] },
    { reminder: { ...fullTask.reminder, time: "25:00" } },
    { reminder: { ...fullTask.reminder, daysOfWeek: [1, 1] } },
    { reminder: { ...fullTask.reminder, endType: "ON_DATE", endDate: null } },
    { streakBonus: { ...fullTask.streakBonus, everyValue: 0 } },
    { dueByDate: -1 },
    { urgencyBonus: 1.5 },
  ])
    assert.throws(() => taskOptions({ ...fullTask, ...change }));
});
test("explicit generation preferences override content and enforce badge count", () => {
  const item = structuredClone(fullTask);
  enforceChoices(item, { badgeCount: "", frequency: false });
  assert.equal(item.limitType, "NONE");
  assert.equal(item.requireApproval, true);
  assert.ok(
    !item.badges && !item.reminder && !item.streakBonus && !item.isUrgent,
  );
  assert.throws(() => featureChoices({ badgeCount: 21 }));
  assert.throws(() =>
    enforceChoices(structuredClone(fullTask), { badgeCount: 10 }),
  );
  const limited = {
    ...structuredClone(fullTask),
    limitType: "TOTAL",
    limitValue: 2,
    badges: fullTask.badges.slice(0, 2),
  };
  enforceChoices(limited, { badgeCount: 10 });
  assert.equal(limited.badges.length, 2);
});
test("AI parser supports full task payload and rejects reward-only violations", () => {
  const { kind, id, state, originalName, ...payload } = fullTask;
  const result = parseSuggestions(
    { tasks: [payload], rewards: [], reason: "Review these options." },
    { badgeCount: 20, streaks: true, reminders: true, urgency: true },
  );
  assert.equal(result.items[0].badges.length, 20);
  assert.throws(() =>
    parseSuggestions({ tasks: [], rewards: [payload], reason: "Review." }),
  );
});
