import { points } from "./core.js";
export const icons = [
  "Check",
  "Alarm",
  "Bed",
  "Brush",
  "School",
  "MenuBook",
  "Redeem",
  "MusicNote",
  "SmartToy",
  "Celebration",
  "SelfImprovement",
];
export const colors = [
  "GREEN",
  "BLUE",
  "GRAY",
  "WHITE",
  "PURPLE",
  "YELLOW",
  "RED",
];
export const frequencies = ["NONE", "DAILY", "WEEKLY", "MONTHLY", "TOTAL"];
const valid = (ok, code) => {
  if (!ok) throw Error(code);
};
const text = (s, max, empty = false) => {
  valid(
    typeof s === "string" &&
      s.length <= max &&
      (empty || s.trim().length > 0) &&
      s.isWellFormed() &&
      !/[\u0000-\u001f\u007f-\u009f]/u.test(s),
    "NAME",
  );
  return s.trim();
};
export const key = (s) => s.normalize("NFKC").trim().toUpperCase();
export function validateParentRequest(r) {
  valid(
    typeof r.goal === "string" &&
      r.goal.trim().length > 0 &&
      r.goal.length <= 1200,
    "REQUEST",
  );
  valid(["en", "de"].includes(r.language), "LANGUAGE");
  valid(
    ["unspecified", "preschool", "primary", "teen"].includes(r.ageBand),
    "AGE",
  );
  valid(
    r.availableMinutes == null ||
      (Number.isInteger(r.availableMinutes) &&
        r.availableMinutes >= 1 &&
        r.availableMinutes <= 180),
    "TIME",
  );
  valid(
    Array.isArray(r.constraints) &&
      r.constraints.length <= 2 &&
      r.constraints.every((x) => ["no_purchases", "no_screens"].includes(x)),
    "CONSTRAINT",
  );
  valid(typeof r.feedback === "string" && r.feedback.length <= 600, "FEEDBACK");
  for (const k of ["existing", "discarded"]) {
    valid(Array.isArray(r[k]) && r[k].length <= 80, "CONTEXT");
    for (const x of r[k]) {
      valid(["task", "reward"].includes(x.kind), "CONTEXT");
      text(x.name, 120);
    }
  }
  return r;
}
export function portableItem(item) {
  const name = text(item.name, 120);
  const description = text(
    (item.description ?? "").replace(/\s+/g, " "),
    2000,
    true,
  );
  const pointAmount =
    typeof item.pointAmount === "string"
      ? points(item.pointAmount)
      : item.pointAmount;
  valid(
    Number.isInteger(pointAmount) && pointAmount >= 1 && pointAmount <= 1000000,
    "POINTS",
  );
  valid(icons.includes(item.icon), "ICON");
  valid(colors.includes(item.color), "COLOR");
  valid(frequencies.includes(item.limitType), "FREQUENCY");
  const limitValue =
    typeof item.limitValue === "string"
      ? points(item.limitValue)
      : item.limitValue;
  valid(
    Number.isInteger(limitValue) && limitValue >= 1 && limitValue <= 10000,
    "FREQUENCY",
  );
  valid(typeof item.requireApproval === "boolean", "APPROVAL");
  return {
    name,
    description,
    pointAmount,
    icon: item.icon,
    color: item.color,
    limitType: item.limitType,
    limitValue,
    requireApproval: item.requireApproval,
  };
}
export function parseSuggestions(raw) {
  valid(
    raw &&
      typeof raw === "object" &&
      Object.keys(raw).every((k) => ["tasks", "rewards", "reason"].includes(k)),
    "RESPONSE",
  );
  valid(
    Array.isArray(raw.tasks) &&
      raw.tasks.length <= 6 &&
      Array.isArray(raw.rewards) &&
      raw.rewards.length <= 3,
    "TASK_COUNT",
  );
  if (!raw.tasks.length && !raw.rewards.length && !raw.reason?.trim())
    throw Error("NEEDS_INPUT");
  const convert = (list, kind) =>
    list.map((x) => {
      valid(
        Object.keys(x).every((k) =>
          [
            "name",
            "description",
            "pointAmount",
            "icon",
            "color",
            "limitType",
            "limitValue",
            "requireApproval",
          ].includes(k),
        ),
        "RESPONSE",
      );
      return { ...portableItem(x), kind };
    });
  return {
    items: [...convert(raw.tasks, "task"), ...convert(raw.rewards, "reward")],
    reason: text(raw.reason, 400),
  };
}
export function blankPlan(language, title) {
  return { id: crypto.randomUUID(), title, language, items: [], discarded: [] };
}
export function newItem(kind) {
  return {
    id: crypto.randomUUID(),
    kind,
    state: "kept",
    name: "",
    originalName: "",
    description: "",
    pointAmount: kind === "task" ? 5 : 20,
    icon: kind === "task" ? "Check" : "Redeem",
    color: kind === "task" ? "BLUE" : "PURPLE",
    limitType: "NONE",
    limitValue: 1,
    requireApproval: true,
  };
}
export function mergeSuggestions(plan, result) {
  const next = structuredClone(plan);
  const blocked = new Set(
    [...next.items, ...next.discarded].flatMap((x) =>
      [x.name, x.originalName]
        .filter(Boolean)
        .map((n) => x.kind + ":" + key(n)),
    ),
  );
  let added = 0;
  for (const item of result.items) {
    const k = item.kind + ":" + key(item.name);
    if (blocked.has(k)) continue;
    if (next.items.length >= 40) break;
    blocked.add(k);
    next.items.push({
      ...item,
      id: crypto.randomUUID(),
      state: "suggested",
      originalName: item.name,
    });
    added++;
  }
  return { plan: next, added };
}
export function discardItem(plan, id) {
  const next = structuredClone(plan);
  const item = next.items.find((x) => x.id === id);
  if (!item) return next;
  valid(next.discarded.length < 80, "LIMIT");
  next.items = next.items.filter((x) => x.id !== id);
  next.discarded.push({
    kind: item.kind,
    name: item.name,
    originalName: item.originalName,
  });
  return next;
}
export function contextFor(plan) {
  return {
    existing: plan.items
      .filter((x) => x.name.trim())
      .map((x) => ({
        kind: x.kind,
        name: x.name.trim(),
        pointAmount: Number(x.pointAmount) || 5,
        state: x.state,
      })),
    discarded: plan.discarded
      .flatMap((x) =>
        [...new Set([x.name, x.originalName].filter(Boolean))].map((name) => ({
          kind: x.kind,
          name,
        })),
      )
      .slice(-80),
  };
}
export function exportPlan(plan) {
  const selected = plan.items.filter((x) => x.state === "kept");
  valid(selected.length > 0 && selected.length <= 100, "SELECTION");
  const parts = (kind) => {
    const values = selected.filter((x) => x.kind === kind).map(portableItem);
    valid(
      new Set(values.map((x) => key(x.name))).size === values.length,
      "DUPLICATE",
    );
    return values;
  };
  const json = JSON.stringify(
    {
      format: "starjar-templates",
      version: 1,
      tasks: parts("task"),
      rewards: parts("reward"),
    },
    null,
    2,
  );
  valid(json.length <= 100000, "EXPORT_SIZE");
  return json;
}
// Draft storage permits incomplete edits, but is bounded and allowlisted. No goal, age, feedback or model prompt.
export function cleanPlan(p) {
  valid(
    p &&
      typeof p.id === "string" &&
      p.id.length <= 128 &&
      typeof p.title === "string" &&
      p.title.length <= 120 &&
      ["en", "de"].includes(p.language),
    "STORAGE",
  );
  valid(
    Array.isArray(p.items) &&
      p.items.length <= 40 &&
      Array.isArray(p.discarded) &&
      p.discarded.length <= 80,
    "STORAGE",
  );
  const items = p.items.map((x) => {
    valid(
      ["task", "reward"].includes(x.kind) &&
        ["kept", "suggested"].includes(x.state) &&
        typeof x.id === "string" &&
        x.id.length <= 128,
      "STORAGE",
    );
    const name = text(x.name, 120, true),
      description = text(
        (x.description ?? "").replace(/\s+/g, " "),
        2000,
        true,
      ),
      originalName = text(x.originalName ?? "", 120, true);
    valid(
      ["string", "number"].includes(typeof x.pointAmount) &&
        String(x.pointAmount).length <= 7,
      "POINTS",
    );
    valid(
      ["string", "number"].includes(typeof x.limitValue) &&
        String(x.limitValue).length <= 5,
      "FREQUENCY",
    );
    valid(
      icons.includes(x.icon) &&
        colors.includes(x.color) &&
        frequencies.includes(x.limitType) &&
        typeof x.requireApproval === "boolean",
      "STORAGE",
    );
    return {
      id: x.id,
      kind: x.kind,
      state: x.state,
      name,
      originalName,
      description,
      pointAmount: x.pointAmount,
      icon: x.icon,
      color: x.color,
      limitType: x.limitType,
      limitValue: x.limitValue,
      requireApproval: x.requireApproval,
    };
  });
  valid(new Set(items.map((x) => x.id)).size === items.length, "STORAGE");
  return {
    id: p.id,
    title: p.title,
    language: p.language,
    items,
    discarded: p.discarded.map((x) => {
      valid(["task", "reward"].includes(x.kind), "STORAGE");
      return {
        kind: x.kind,
        name: text(x.name, 120, true),
        originalName: text(x.originalName ?? "", 120, true),
      };
    }),
  };
}
export function fromLegacy(r) {
  const p = blankPlan(r.language, r.title);
  p.id = r.id;
  p.items = [
    ...(r.tasks ?? []).map((x) => ({ ...newItem("task"), ...x })),
    ...(r.reward ? [{ ...newItem("reward"), ...r.reward }] : []),
  ];
  return cleanPlan(p);
}
export function loadPlans(storage) {
  const raw = storage.getItem("routine-library-v2");
  if (raw !== null) {
    const p = JSON.parse(raw);
    valid(Array.isArray(p) && p.length <= 100, "STORAGE");
    return p.map(cleanPlan);
  }
  const legacy = JSON.parse(storage.getItem("routine-library-v1") || "[]");
  valid(Array.isArray(legacy) && legacy.length <= 100, "STORAGE");
  return legacy.map(fromLegacy);
}
export function savePlan(storage, p) {
  const clean = cleanPlan(p);
  text(clean.title, 120);
  const list = loadPlans(storage),
    i = list.findIndex((x) => x.id === p.id);
  if (i >= 0) list[i] = clean;
  else {
    valid(list.length < 100, "LIMIT");
    list.unshift(clean);
  }
  storage.setItem("routine-library-v2", JSON.stringify(list));
}
export function deletePlan(storage, id) {
  storage.setItem(
    "routine-library-v2",
    JSON.stringify(loadPlans(storage).filter((x) => x.id !== id)),
  );
}
