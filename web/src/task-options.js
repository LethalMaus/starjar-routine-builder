// StarJar's portable task contract. Rewards never receive these fields.
const check = (ok) => {
  if (!ok) throw Error("TASK_OPTIONS");
};
const int = (v, min = 1, max = 10000) => {
  check(Number.isInteger(v) && v >= min && v <= max);
  return v;
};
const date = (v) => {
  if (v == null) return null;
  return int(v, 0, 253402300799999);
};
const enumeration = (v, values) => {
  check(values.includes(v));
  return v;
};
export const units = ["TIME", "DAY", "WEEK", "MONTH"];
export function featureChoices(f = {}) {
  const result = {
    badgeCount:
      f.badgeCount === "" || f.badgeCount == null ? 0 : Number(f.badgeCount),
    frequency: f.frequency ?? true,
    streaks: f.streaks ?? false,
    reminders: f.reminders ?? false,
    urgency: f.urgency ?? false,
  };
  int(result.badgeCount, 0, 20);
  for (const k of ["frequency", "streaks", "reminders", "urgency"])
    check(typeof result[k] === "boolean");
  return result;
}
export function taskOptions(x) {
  const out = {};
  if (x.badges != null) {
    check(Array.isArray(x.badges) && x.badges.length <= 20);
    let previous = 0;
    out.badges = x.badges.map((b) => {
      const n = int(b.completionsRequired, 1, 1000000);
      check(n > previous);
      previous = n;
      check(
        typeof b.title === "string" &&
          b.title.trim().length > 0 &&
          b.title.length <= 120 &&
          !/[\u0000-\u001f\u007f-\u009f]/u.test(b.title),
      );
      return {
        completionsRequired: n,
        title: b.title.trim(),
        bonusPoints:
          b.bonusPoints == null ? null : int(b.bonusPoints, 0, 1000000),
      };
    });
  }
  if (x.streakBonus != null) {
    const b = x.streakBonus;
    out.streakBonus = {
      bonusPoints: int(b.bonusPoints, 1, 1000000),
      everyValue: int(b.everyValue),
      everyUnit: enumeration(b.everyUnit, units),
      forValue: int(b.forValue),
      forUnit: enumeration(b.forUnit, units),
    };
  }
  if (x.reminder != null) {
    const r = x.reminder;
    check(
      Array.isArray(r.daysOfWeek) &&
        r.daysOfWeek.length <= 7 &&
        new Set(r.daysOfWeek).size === r.daysOfWeek.length,
    );
    r.daysOfWeek.forEach((d) => int(d, 1, 7));
    check(r.time == null || /^([01]\d|2[0-3]):[0-5]\d$/.test(r.time));
    const startDate = date(r.startDate),
      endDate = date(r.endDate),
      endType = enumeration(r.endType, [
        "NEVER",
        "ON_DATE",
        "AFTER_OCCURRENCES",
      ]);
    const endOccurrences =
      r.endOccurrences == null ? null : int(r.endOccurrences);
    check(endType !== "ON_DATE" || endDate !== null);
    check(endType !== "AFTER_OCCURRENCES" || endOccurrences !== null);
    check(startDate == null || endDate == null || endDate >= startDate);
    out.reminder = {
      frequencyValue: int(r.frequencyValue),
      frequencyUnit: enumeration(r.frequencyUnit, ["DAY", "WEEK", "MONTH"]),
      daysOfWeek: [...r.daysOfWeek],
      time: r.time ?? null,
      startDate,
      endType,
      endDate,
      endOccurrences,
    };
  }
  if (x.isUrgent != null) {
    check(typeof x.isUrgent === "boolean");
    out.isUrgent = x.isUrgent;
  }
  if (x.dueByDate != null) out.dueByDate = date(x.dueByDate);
  if (x.urgencyBonus != null)
    out.urgencyBonus = int(x.urgencyBonus, 0, 1000000);
  return out;
}
export function enforceChoices(item, raw) {
  const f = featureChoices(raw);
  // Explicit feature preferences outrank generated content.
  if (!f.frequency) {
    item.limitType = "NONE";
    item.limitValue = 1;
  }
  item.requireApproval = true;
  if (item.kind !== "task") return;
  if (!f.badgeCount) delete item.badges;
  else {
    const count =
      item.limitType === "TOTAL"
        ? Math.min(f.badgeCount, item.limitValue)
        : f.badgeCount;
    check(item.badges?.length === count);
    item.badges[0] = { ...item.badges[0], completionsRequired: 1 };
    if (item.limitType === "TOTAL")
      check(item.badges.every((b) => b.completionsRequired <= item.limitValue));
  }
  if (!f.streaks) delete item.streakBonus;
  if (!f.reminders) delete item.reminder;
  if (!f.urgency) {
    delete item.isUrgent;
    delete item.dueByDate;
    delete item.urgencyBonus;
  }
}
