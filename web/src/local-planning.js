// German local output is currently below the quality bar; keep cloud opt-in.
export function supportsLocalPlanning(language) {
  return language === "en";
}

import { parseSuggestions } from "./parent-plan.js";

// Keep the small model focused on ideas. Product metadata is editable, deterministic
// and never accepted from the model on this route.
export function localPlanningContext(request) {
  return {
    difficulty: request.goal,
    latestFeedback: request.feedback,
    age: request.ageBand,
    minutes: request.availableMinutes,
    preferences: request.constraints,
    existing: request.existing.map(({ kind, name }) => ({ kind, name })),
    discarded: request.discarded.map(({ kind, name }) => ({ kind, name })),
  };
}

export function localPlanningResult(raw, language, choices) {
  if (!raw || Object.keys(raw).some((k) => !["tasks", "rewards"].includes(k)))
    throw Error("RESPONSE");
  function items(values, kind, max) {
    if (!Array.isArray(values) || values.length > max) throw Error("TASK_COUNT");
    return values.map((value) => {
      if (!value || Object.keys(value).some((k) => !["name", "description"].includes(k)))
        throw Error("RESPONSE");
      if (typeof value.name !== "string" || value.name.length > 60 ||
          typeof value.description !== "string" || value.description.length > 180)
        throw Error("RESPONSE");
      return {
        name: value.name,
        description: value.description,
        pointAmount: kind === "task" ? 5 : 20,
        icon: kind === "task" ? "Check" : "Redeem",
        color: kind === "task" ? "BLUE" : "PURPLE",
        limitType: "NONE",
        limitValue: 1,
        requireApproval: true,
      };
    });
  }
  return parseSuggestions({
    tasks: items(raw.tasks, "task", 6),
    rewards: items(raw.rewards, "reward", 3),
    reason: raw.tasks.length || raw.rewards.length
      ? (language === "de"
        ? "Prüfe die Ideen und passe Sterne und Details an dein Kind an."
        : "Review the ideas and adjust stars and details for your child.")
      : "",
  }, choices);
}
