import { test } from "node:test";
import assert from "node:assert/strict";
import {
  request,
  draft,
  reviewed,
  points,
  exportRoutine,
  saveRoutine,
  readSaved,
  deleteRoutine,
} from "../src/core.js";
const req = {
  goal: "teeth, bag, shoes",
  language: "en",
  taskCount: 3,
  includeReward: true,
  constraints: [],
};
const routine = {
  id: "one",
  title: "Morning",
  language: "en",
  tasks: [{ name: "Brush teeth", pointAmount: 5 }],
  reward: { name: "Read together", pointAmount: 20 },
};
test("request bounds and constraint allowlist", () => {
  assert.equal(request(req), req);
  for (const r of [
    { ...req, goal: "" },
    { ...req, taskCount: 4 },
    { ...req, language: "fr" },
    { ...req, constraints: ["anything"] },
    { ...req, constraints: ["no_screens", "no_screens"] },
  ])
    assert.throws(() => request(r));
});
test("draft validates schema, count, language-independent Unicode duplicates", () => {
  assert.deepEqual(
    draft(
      { tasks: ["Brush teeth", "Pack bag", "Shoes on"], reward: "Read" },
      req,
    ),
    { tasks: ["Brush teeth", "Pack bag", "Shoes on"], reward: "Read" },
  );
  assert.throws(
    () => draft({ tasks: ["Straße", "STRASSE", "Shoes"], reward: "Read" }, req),
    /DUPLICATE/,
  );
  assert.throws(
    () => draft({ tasks: ["x", "y", "z"], reward: "r", points: 3 }, req),
    /RESPONSE/,
  );
  assert.throws(() => draft({ tasks: [], reward: "" }, req), /NEEDS_INPUT/);
  assert.throws(() => draft({ tasks: ["x"], reward: "r" }, req), /TASK_COUNT/);
});
test("no-reward schema forbids reward and malformed strings", () => {
  const r = { ...req, includeReward: false, taskCount: 1 };
  assert.deepEqual(draft({ tasks: [" Zähne putzen "] }, r), {
    tasks: ["Zähne putzen"],
    reward: null,
  });
  for (const tasks of [["\ud800"], ["hi\nthere"], ["x".repeat(81)]])
    assert.throws(() => draft({ tasks }, r));
  assert.throws(() => draft({ tasks: ["x"], reward: "r" }, r));
});
test("strict whole star amounts", () => {
  assert.equal(points("1000000"), 1000000);
  for (const x of ["", "0", "-1", "1.5", "1e3", " 5", "1000001", "٥"])
    assert.throws(() => points(x));
});
test("export strips provenance, IDs, title and original prompt", () => {
  const json = JSON.parse(
    exportRoutine({ ...routine, goal: "private", source: "cloud" }),
  );
  assert.deepEqual(Object.keys(json), [
    "format",
    "version",
    "tasks",
    "rewards",
  ]);
  assert.deepEqual(json.tasks, routine.tasks);
  assert.equal(json.format, "starjar-templates");
  assert.ok(!JSON.stringify(json).includes("private"));
  assert.throws(() => reviewed({ ...routine, title: " " }));
});
const store = () => {
  const map = new Map();
  return {
    getItem: (k) => map.get(k) ?? null,
    setItem: (k, v) => map.set(k, v),
  };
};
test("save updates by ID, strips unreviewed data and deletes only intended record", () => {
  const s = store();
  saveRoutine(s, { ...routine, goal: "private" });
  saveRoutine(s, { ...routine, title: "New title" });
  saveRoutine(s, { ...routine, id: "two" });
  assert.equal(readSaved(s).length, 2);
  assert.equal(readSaved(s)[1].title, "New title");
  assert.ok(!s.getItem("routine-library-v1").includes("private"));
  deleteRoutine(s, "one");
  assert.equal(readSaved(s)[0].id, "two");
});
test("corrupt or blocked storage never silently replaces saved data", () => {
  const s = store();
  s.setItem("routine-library-v1", "broken");
  assert.throws(() => saveRoutine(s, routine));
  assert.equal(s.getItem("routine-library-v1"), "broken");
  assert.throws(() =>
    saveRoutine(
      {
        getItem: () => null,
        setItem: () => {
          throw Error("quota");
        },
      },
      routine,
    ),
  );
});
import { cancellable } from "../src/cancellation.js";
test("cancellation returns promptly and ignores late model completion", async () => {
  const c = new AbortController();
  let finish;
  const underlying = new Promise((r) => (finish = r));
  const pending = cancellable(underlying, c.signal);
  c.abort();
  await assert.rejects(pending, /CANCELLED/);
  finish("late draft");
  await Promise.resolve();
});
test("timeout rejects and delayed failures are handled", async () => {
  const c = new AbortController();
  let fail;
  const underlying = new Promise((_, r) => (fail = r));
  await assert.rejects(cancellable(underlying, c.signal, 5), /TIMEOUT/);
  fail(Error("late network error"));
  await Promise.resolve();
});
