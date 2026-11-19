import { exportPlan } from "./parent-plan.js";
const copy = {
  en: {
    week: "Week beginning",
    tasks: "Our tasks",
    stars: "Stars",
    days: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
    agree: "Agree together before starting",
    agreement:
      "Decide what counts as finished and choose the rewards together. A parent checks a task before stars are recorded. You do not have to do every task every day.",
    tick: "Write the number of completions in each day, or tick once for a daily task. Follow any frequency limits you agreed.",
    rewards: "Rewards we can choose",
    review: "At the end of the week",
    reflection: "What worked? What would we change next week?",
    paper:
      "Use this sheet without an app. Paper marks do not sync with StarJar.",
    notes: "Notes and agreements",
    limits:
      "Digital reminders, badges and bonus rules are not calculated by this paper chart.",
  },
  de: {
    week: "Woche ab",
    tasks: "Unsere Aufgaben",
    stars: "Sterne",
    days: ["Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"],
    agree: "Vorher gemeinsam absprechen",
    agreement:
      "Legt fest, wann eine Aufgabe erledigt ist, und wählt die Belohnungen gemeinsam aus. Ein Elternteil prüft die Aufgabe, bevor ihr Sterne eintragt. Nicht jede Aufgabe muss jeden Tag erledigt werden.",
    tick: "Tragt pro Tag die Anzahl der Erledigungen ein oder setzt bei täglichen Aufgaben ein Häkchen. Beachtet eure vereinbarten Häufigkeitsgrenzen.",
    rewards: "Unsere Belohnungen",
    review: "Am Ende der Woche",
    reflection: "Was hat gut geklappt? Was möchten wir nächste Woche ändern?",
    paper:
      "Dieser Plan funktioniert ohne App. Einträge auf Papier werden nicht mit StarJar synchronisiert.",
    notes: "Notizen und Absprachen",
    limits:
      "Digitale Erinnerungen, Abzeichen und Bonusregeln werden auf diesem Papierplan nicht berechnet.",
  },
};
function node(tag, text, cls) {
  const e = document.createElement(tag);
  if (text != null) e.textContent = text;
  if (cls) e.className = cls;
  return e;
}
export function printablePlan(plan, language = "en") {
  const doc = JSON.parse(exportPlan(plan)),
    c = copy[language] ?? copy.en,
    root = node("section", null, "print-plan");
  root.append(
    node("p", "StarJar", "print-brand"),
    node("h1", plan.title),
    node("p", c.week + ": ____________________"),
    node("h2", c.agree),
    node("p", c.agreement),
  );
  if (doc.tasks.length) {
    root.append(node("h2", c.tasks), node("p", c.tick));
    const table = node("table"),
      head = node("thead"),
      hr = node("tr");
    for (const x of [c.tasks, c.stars, ...c.days]) hr.append(node("th", x));
    head.append(hr);
    table.append(head);
    const body = node("tbody");
    for (const task of doc.tasks) {
      const row = node("tr"),
        name = node("td");
      name.append(node("strong", task.name));
      if (task.description) name.append(node("p", task.description));
      row.append(name, node("td", String(task.pointAmount)));
      for (let i = 0; i < 7; i++) row.append(node("td", ""));
      body.append(row);
    }
    table.append(body);
    root.append(table);
  }
  if (doc.rewards.length) {
    root.append(node("h2", c.rewards));
    const ul = node("ul");
    for (const reward of doc.rewards) {
      const li = node("li");
      li.append(
        node(
          "strong",
          reward.name + " — " + reward.pointAmount + " " + c.stars,
        ),
      );
      if (reward.description) li.append(node("p", reward.description));
      ul.append(li);
    }
    root.append(ul);
  }
  root.append(
    node("h2", c.notes),
    node("div", "\u00a0", "writing-lines"),
    node("h2", c.review),
    node("p", c.reflection),
    node("div", "\u00a0", "writing-lines"),
    node("p", c.limits, "print-note"),
    node("p", c.paper, "print-note"),
  );
  return root;
}
export function printPlan(plan, language) {
  document.getElementById("print-root")?.remove();
  const root = printablePlan(plan, language);
  root.id = "print-root";
  document.body.append(root);
  window.print();
}
