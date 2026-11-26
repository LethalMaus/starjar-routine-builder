import { taskOptions, units } from "./task-options.js";
// All task-only settings live behind the card's existing details disclosure.
export function taskOptionsEditor(
  item,
  { el, field, select, checkbox, button, t, persist, render },
) {
  const box = el("div", { class: "task-options" });
  const apply = (fn, control) => {
    const next = structuredClone(item);
    fn(next);
    try {
      Object.assign(item, taskOptions(next));
      for (const k of [
        "badges",
        "streakBonus",
        "reminder",
        "isUrgent",
        "dueByDate",
        "urgencyBonus",
      ])
        if (!(k in next)) delete item[k];
      control?.setCustomValidity("");
      persist();
      return true;
    } catch {
      control?.setCustomValidity(t("invalid"));
      control?.reportValidity();
      return false;
    }
  };
  const number = (label, value, fn, min = 1, max = 10000) =>
    field(
      label,
      el("input", {
        type: "number",
        min,
        max,
        required: true,
        value,
        "aria-label": label,
        onchange: (e) => {
          e.target.setCustomValidity("");
          if (e.target.reportValidity() && e.target.value !== "")
            apply((x) => fn(x, Number(e.target.value)), e.target);
        },
      }),
    );
  const date = (label, value, fn) =>
    field(
      label,
      el("input", {
        type: "datetime-local",
        value: value == null ? "" : localDate(value),
        "aria-label": label,
        onchange: (e) =>
          apply(
            (x) =>
              fn(x, e.target.value ? new Date(e.target.value).getTime() : null),
            e.target,
          ),
      }),
    );
  box.append(
    el("h4", {}, t("badges")),
    el("p", { class: "muted" }, t("badgeHelp")),
  );
  for (const [i, b] of (item.badges ?? []).entries()) {
    const row = el(
      "details",
      { class: "badge-editor" },
      el(
        "summary",
        {},
        b.title + " · " + b.completionsRequired + " " + t("completions"),
      ),
    );
    row.append(
      field(
        t("badgeTitle"),
        el("input", {
          value: b.title,
          maxLength: 120,
          "aria-label": t("badgeTitle"),
          onchange: (e) =>
            apply((x) => (x.badges[i].title = e.target.value), e.target),
        }),
      ),
      number(
        t("completions"),
        b.completionsRequired,
        (x, v) => (x.badges[i].completionsRequired = v),
        1,
        1000000,
      ),
      number(
        t("bonusStars"),
        b.bonusPoints ?? 0,
        (x, v) => (x.badges[i].bonusPoints = v),
        0,
        1000000,
      ),
      button(
        t("removeBadge"),
        () => {
          apply((x) => x.badges.splice(i, 1));
          render();
        },
        true,
      ),
    );
    box.append(row);
  }
  box.append(
    button(
      t("addBadge"),
      () => {
        const n = (item.badges ?? []).at(-1)?.completionsRequired ?? 0;
        apply((x) => {
          x.badges ??= [];
          x.badges.push({
            completionsRequired: n + 1,
            title: t("newBadge"),
            bonusPoints: 1,
          });
        });
        render();
      },
      true,
      { disabled: (item.badges?.length ?? 0) >= 20 },
    ),
  );
  box.append(
    checkbox(
      t("streaks"),
      !!item.streakBonus,
      (v) => {
        apply((x) => {
          if (v)
            x.streakBonus = {
              bonusPoints: 5,
              everyValue: 1,
              everyUnit: "DAY",
              forValue: 5,
              forUnit: "DAY",
            };
          else delete x.streakBonus;
        });
        render();
      },
      "streak-" + item.id,
    ),
  );
  if (item.streakBonus) {
    const b = item.streakBonus;
    box.append(el("p", { class: "muted" }, t("streakHelp")));
    box.append(
      number(
        t("bonusStars"),
        b.bonusPoints,
        (x, v) => (x.streakBonus.bonusPoints = v),
        1,
        1000000,
      ),
      number(
        t("everyCount"),
        b.everyValue,
        (x, v) => (x.streakBonus.everyValue = v),
      ),
      field(
        t("everyUnit"),
        select(
          b.everyUnit,
          units,
          (v) => apply((x) => (x.streakBonus.everyUnit = v)),
          "every-" + item.id,
          (v) => t("unit_" + v),
        ),
      ),
      number(t("forCount"), b.forValue, (x, v) => (x.streakBonus.forValue = v)),
      field(
        t("forUnit"),
        select(
          b.forUnit,
          units,
          (v) => apply((x) => (x.streakBonus.forUnit = v)),
          "for-" + item.id,
          (v) => t("unit_" + v),
        ),
      ),
    );
  }
  box.append(
    checkbox(
      t("reminders"),
      !!item.reminder,
      (v) => {
        apply((x) => {
          if (v)
            x.reminder = {
              frequencyValue: 1,
              frequencyUnit: "DAY",
              daysOfWeek: [],
              time: null,
              startDate: null,
              endType: "NEVER",
              endDate: null,
              endOccurrences: null,
            };
          else delete x.reminder;
        });
        render();
      },
      "reminder-" + item.id,
    ),
  );
  if (item.reminder) {
    const r = item.reminder;
    box.append(
      number(
        t("everyCount"),
        r.frequencyValue,
        (x, v) => (x.reminder.frequencyValue = v),
      ),
      field(
        t("everyUnit"),
        select(
          r.frequencyUnit,
          ["DAY", "WEEK", "MONTH"],
          (v) => apply((x) => (x.reminder.frequencyUnit = v)),
          "reminder-unit-" + item.id,
          (v) => t("unit_" + v),
        ),
      ),
      field(
        t("reminderTime"),
        el("input", {
          type: "time",
          value: r.time ?? "",
          "aria-label": t("reminderTime"),
          onchange: (e) =>
            apply((x) => (x.reminder.time = e.target.value || null), e.target),
        }),
      ),
      el("p", { class: "muted" }, t("reminderHelp")),
    );
    const days = el("fieldset", {}, el("legend", {}, t("weekdays")));
    for (let d = 1; d <= 7; d++)
      days.append(
        checkbox(
          t("day_" + d),
          r.daysOfWeek.includes(d),
          (v) =>
            apply(
              (x) =>
                (x.reminder.daysOfWeek = v
                  ? [...x.reminder.daysOfWeek, d].sort()
                  : x.reminder.daysOfWeek.filter((n) => n !== d)),
            ),
          "day-" + item.id + "-" + d,
        ),
      );
    box.append(
      days,
      date(t("startDate"), r.startDate, (x, v) => (x.reminder.startDate = v)),
      field(
        t("endType"),
        select(
          r.endType,
          ["NEVER", "ON_DATE", "AFTER_OCCURRENCES"],
          (v) => {
            apply((x) => {
              x.reminder.endType = v;
              x.reminder.endDate =
                v === "ON_DATE" ? (x.reminder.startDate ?? Date.now()) : null;
              x.reminder.endOccurrences = v === "AFTER_OCCURRENCES" ? 10 : null;
            });
            render();
          },
          "end-" + item.id,
          (v) => t("end_" + v),
        ),
      ),
    );
    if (r.endType === "ON_DATE")
      box.append(
        date(t("endDate"), r.endDate, (x, v) => (x.reminder.endDate = v)),
      );
    if (r.endType === "AFTER_OCCURRENCES")
      box.append(
        number(
          t("occurrences"),
          r.endOccurrences,
          (x, v) => (x.reminder.endOccurrences = v),
        ),
      );
  }
  box.append(
    checkbox(
      t("urgent"),
      item.isUrgent ?? false,
      (v) => {
        apply((x) => {
          x.isUrgent = v;
          if (!v) {
            delete x.dueByDate;
            delete x.urgencyBonus;
          }
        });
        render();
      },
      "urgent-" + item.id,
    ),
  );
  if (item.isUrgent)
    box.append(
      date(t("dueDate"), item.dueByDate, (x, v) => {
        if (v == null) delete x.dueByDate;
        else x.dueByDate = v;
      }),
      number(
        t("bonusStars"),
        item.urgencyBonus ?? 0,
        (x, v) => (x.urgencyBonus = v),
        0,
        1000000,
      ),
    );
  return box;
}
function localDate(ms) {
  const d = new Date(ms);
  return new Date(ms - d.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
}
