import { blankPlan, newItem, cleanPlan, exportPlan } from "./parent-plan.js";
const MAX_BYTES = 200000;
export function exportEditablePlan(plan) {
  const text = JSON.stringify(
    { format: "starjar-ideas-plan", version: 1, plan: cleanPlan(plan) },
    null,
    2,
  );
  if (new TextEncoder().encode(text).length > MAX_BYTES)
    throw Error("EXPORT_SIZE");
  return text;
}
export function readPlanFile(text, language = "en", title = "Imported ideas") {
  if (
    typeof text !== "string" ||
    new TextEncoder().encode(text).length > MAX_BYTES
  )
    throw Error("IMPORT_SIZE");
  const doc = JSON.parse(text);
  if (doc.version !== 1) throw Error("IMPORT_VERSION");
  if (doc.format === "starjar-ideas-plan") return cleanPlan(doc.plan);
  if (
    doc.format !== "starjar-templates" ||
    !Array.isArray(doc.tasks) ||
    !Array.isArray(doc.rewards)
  )
    throw Error("IMPORT_FORMAT");
  if (
    doc.tasks.length + doc.rewards.length < 1 ||
    doc.tasks.length + doc.rewards.length > 40
  )
    throw Error("IMPORT_SIZE");
  const plan = blankPlan(language, title);
  for (const kind of ["task", "reward"])
    for (const source of doc[kind === "task" ? "tasks" : "rewards"]) {
      const defaults = newItem(kind);
      // Strip unknown/local identity fields. Incoming templates remain local until reviewed.
      const item = {
        ...defaults,
        name: source.name,
        originalName: source.name,
        pointAmount: source.pointAmount,
        description: source.description ?? "",
        icon: source.icon ?? defaults.icon,
        color: source.color ?? defaults.color,
        limitType: source.limitType ?? "NONE",
        limitValue: source.limitValue ?? 1,
        requireApproval: source.requireApproval ?? true,
      };
      if (kind === "task")
        for (const key of [
          "badges",
          "streakBonus",
          "reminder",
          "isUrgent",
          "dueByDate",
          "urgencyBonus",
        ])
          if (source[key] != null) item[key] = source[key];
      plan.items.push(item);
    }
  exportPlan(plan); // Same strict validation used before sending back to StarJar.
  return cleanPlan(plan);
}
export function downloadFile(text, name) {
  const url = URL.createObjectURL(
    new Blob([text], { type: "application/json" }),
  );
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 30000);
}
export async function shareTemplates(plan, navigatorObject = navigator) {
  const file = new File([exportPlan(plan)], "starjar-templates.json", {
    type: "application/json",
  });
  if (!navigatorObject.share || !navigatorObject.canShare?.({ files: [file] }))
    return "unsupported";
  try {
    await navigatorObject.share({ files: [file], title: plan.title });
    return "shared";
  } catch (e) {
    if (e.name === "AbortError") return "cancelled";
    throw e;
  }
}
