// Contract matches core/Routine.kt and TemplateExporter.kt. UTF-16 length limits are intentional.
const check = (ok, code) => {
  if (!ok) throw new Error(code);
};
const name = (s, max = 120) => {
  check(
    typeof s === "string" &&
      s.trim() === s &&
      s.length > 0 &&
      s.length <= max &&
      !/[\u0000-\u001f\u007f-\u009f]/u.test(s) &&
      s.isWellFormed(),
    "NAME",
  );
  return s;
};
export function request(r) {
  check(
    r && typeof r.goal === "string" && r.goal.trim() && r.goal.length <= 1200,
    "REQUEST",
  );
  check(["en", "de"].includes(r.language), "LANGUAGE");
  check(
    Number.isInteger(r.taskCount) && r.taskCount >= 1 && r.taskCount <= 3,
    "TASK_COUNT",
  );
  check(typeof r.includeReward === "boolean", "REWARD");
  check(
    Array.isArray(r.constraints) &&
      new Set(r.constraints).size === r.constraints.length &&
      r.constraints.every((c) =>
        ["no_purchases", "no_screens", "short_tasks"].includes(c),
      ),
    "CONSTRAINT",
  );
  return r;
}
export function draft(raw, r) {
  request(r);
  check(raw && typeof raw === "object" && !Array.isArray(raw), "RESPONSE");
  check(
    Object.keys(raw).every((k) =>
      ["tasks", ...(r.includeReward ? ["reward"] : [])].includes(k),
    ),
    "RESPONSE",
  );
  check(Array.isArray(raw.tasks), "TASK_COUNT");
  if (raw.tasks.length === 0 && (!raw.reward || raw.reward === ""))
    throw Error("NEEDS_INPUT");
  check(raw.tasks.length === r.taskCount, "TASK_COUNT");
  const tasks = raw.tasks.map((s) =>
    name(typeof s === "string" ? s.trim() : s, 80),
  );
  check(
    new Set(tasks.map((s) => s.normalize("NFKC").toUpperCase())).size ===
      tasks.length,
    "DUPLICATE",
  );
  const reward = r.includeReward
    ? name(typeof raw.reward === "string" ? raw.reward.trim() : raw.reward, 80)
    : null;
  return { tasks, reward };
}
export function points(s) {
  check(
    typeof s === "string" &&
      /^\d{1,7}$/.test(s) &&
      Number(s) >= 1 &&
      Number(s) <= 1000000,
    "POINTS",
  );
  return Number(s);
}
export function reviewed(r) {
  check(
    r && typeof r.id === "string" && r.id.trim() && r.id.length <= 128,
    "ROUTINE",
  );
  name(r.title);
  check(["en", "de"].includes(r.language), "LANGUAGE");
  check(Array.isArray(r.tasks), "TASK_COUNT");
  const items = [...r.tasks, ...(r.reward ? [r.reward] : [])];
  check(items.length >= 1 && items.length <= 100, "TASK_COUNT");
  for (const item of items) {
    name(item.name);
    check(
      Number.isInteger(item.pointAmount) &&
        item.pointAmount >= 1 &&
        item.pointAmount <= 1000000,
      "POINTS",
    );
  }
  return r;
}
export function exportRoutine(r) {
  reviewed(r);
  const item = (x) => ({ name: x.name, pointAmount: x.pointAmount });
  const value = JSON.stringify(
    {
      format: "starjar-templates",
      version: 1,
      tasks: r.tasks.map(item),
      rewards: r.reward ? [item(r.reward)] : [],
    },
    null,
    2,
  );
  check(value.length <= 100000, "EXPORT_SIZE");
  return value;
}
export function cleanRoutine(r) {
  reviewed(r);
  return {
    id: r.id,
    title: r.title,
    language: r.language,
    tasks: r.tasks.map((x) => ({ name: x.name, pointAmount: x.pointAmount })),
    reward: r.reward
      ? { name: r.reward.name, pointAmount: r.reward.pointAmount }
      : null,
  };
}
export function readSaved(storage) {
  const raw = storage.getItem("routine-library-v1");
  if (!raw) return [];
  const values = JSON.parse(raw);
  check(Array.isArray(values) && values.length <= 100, "STORAGE");
  return values.map(cleanRoutine);
}
export function saveRoutine(storage, r) {
  const values = readSaved(storage);
  const clean = cleanRoutine(r);
  const index = values.findIndex((x) => x.id === r.id);
  if (index < 0) {
    check(values.length < 100, "STORAGE");
    values.unshift(clean);
  } else values[index] = clean;
  storage.setItem("routine-library-v1", JSON.stringify(values));
  return values;
}
export function deleteRoutine(storage, id) {
  const values = readSaved(storage).filter((x) => x.id !== id);
  storage.setItem("routine-library-v1", JSON.stringify(values));
  return values;
}
