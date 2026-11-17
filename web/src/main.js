import "./style.css";
import starry from "../../androidApp/src/main/res/drawable/starry.png";
import { strings } from "./strings.js";
import { generate, cloudConfigured } from "./ai.js";
import { cancellable } from "./cancellation.js";
import {
  icons,
  colors,
  frequencies,
  blankPlan,
  newItem,
  mergeSuggestions,
  discardItem,
  contextFor,
  exportPlan,
  cleanPlan,
  fromLegacy,
  loadPlans,
  savePlan,
  deletePlan,
} from "./parent-plan.js";
const root = document.getElementById("app"),
  $ = (id) => document.getElementById(id);
let lang = navigator.language.startsWith("de") ? "de" : "en",
  adult = false,
  screen = "create",
  plan = null,
  receipt = null,
  reason = "",
  feedback = "",
  status = "",
  busy = false,
  controller = null,
  cloudAllowed = false,
  storageError = false;
const expanded = new Set();
let input = { goal: "", ageBand: "unspecified", minutes: "", constraints: [] };
const t = (k) => strings[lang][k] ?? k;
function el(tag, attrs = {}, ...children) {
  const n = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k.startsWith("on")) n.addEventListener(k.slice(2), v);
    else if (k === "class") n.className = v;
    else if (k in n) n[k] = v;
    else n.setAttribute(k, v);
  }
  for (const c of children.flat())
    if (c !== null && c !== undefined)
      n.append(c instanceof Node ? c : document.createTextNode(c));
  return n;
}
const button = (label, fn, secondary = false, attrs = {}) =>
  el(
    "button",
    {
      type: "button",
      class: secondary ? "secondary" : "primary",
      onclick: fn,
      ...attrs,
    },
    label,
  );
const field = (label, node) =>
  el("label", { class: "field" }, el("span", {}, label), node);
const checkbox = (label, value, fn, id) =>
  el(
    "label",
    { class: "check" },
    el("input", {
      type: "checkbox",
      id,
      checked: value,
      onchange: (e) => fn(e.target.checked),
    }),
    el("span", {}, label),
  );
function select(value, values, fn, id, labels = (v) => v) {
  return el(
    "select",
    { id, value, onchange: (e) => fn(e.target.value) },
    ...values.map((v) =>
      el("option", { value: v, selected: v === value }, labels(v)),
    ),
  );
}
function message(s) {
  status = s;
  if ($("status")) $("status").textContent = s;
}
function persist() {
  if (!plan) return;
  try {
    localStorage.setItem(
      "routine-draft-v2",
      JSON.stringify({ plan: cleanPlan(plan) }),
    );
  } catch {
    storageError = true;
    message(t("storage"));
  }
}
function restore() {
  try {
    adult = localStorage.getItem("routine-adult-v1") === "yes";
    const raw = localStorage.getItem("routine-draft-v2");
    if (raw) {
      plan = cleanPlan(JSON.parse(raw).plan);
      screen = "edit";
    } else {
      const old = localStorage.getItem("routine-draft-v1");
      if (old) {
        plan = fromLegacy(JSON.parse(old).edit);
        screen = "edit";
      }
    }
  } catch {
    storageError = true;
    status = t("storage");
  }
}
function fresh() {
  if (plan && !confirm(t("newConfirm"))) return;
  plan = null;
  reason = "";
  feedback = "";
  receipt = null;
  status = "";
  screen = "create";
  input = { goal: "", ageBand: "unspecified", minutes: "", constraints: [] };
  try {
    localStorage.removeItem("routine-draft-v2");
    localStorage.removeItem("routine-draft-v1");
  } catch {
    storageError = true;
  }
  render();
}
function manual() {
  plan = blankPlan(lang, t("draftTitle"));
  plan.items.push(newItem("task"));
  receipt = null;
  reason = "";
  screen = "edit";
  persist();
  render();
}
function render() {
  document.documentElement.lang = lang;
  root.replaceChildren();
  root.append(
    el(
      "header",
      {},
      el(
        "div",
        { class: "brand" },
        el("img", { src: starry, alt: "", width: 44, height: 44 }),
        el("strong", {}, "StarJar", el("small", {}, "Routine Builder")),
      ),
      field(
        lang === "de" ? "Sprache" : "Language",
        select(
          lang,
          ["en", "de"],
          (v) => {
            lang = v;
            status = "";
            render();
          },
          "language",
          (v) => (v === "en" ? "English" : "Deutsch"),
        ),
      ),
    ),
  );
  const main = el("main");
  root.append(main);
  if (!adult) {
    main.append(
      el(
        "section",
        { class: "card welcome" },
        el("img", { src: starry, alt: "", class: "mascot" }),
        el("h1", {}, t("parentHeading")),
        el("p", {}, t("parentIntro")),
        el("p", {}, t("adult")),
        checkbox(
          t("age"),
          false,
          (v) => {
            $("start").disabled = !v;
          },
          "age",
        ),
        button(
          t("start"),
          () => {
            adult = true;
            try {
              localStorage.setItem("routine-adult-v1", "yes");
            } catch {
              storageError = true;
            }
            render();
          },
          false,
          { id: "start", disabled: true },
        ),
        el("p", { class: "muted" }, t("privacy")),
      ),
    );
    return;
  }
  main.append(
    el(
      "nav",
      {},
      button(t("create"), fresh, true),
      button(
        t("saved"),
        () => {
          screen = "saved";
          status = "";
          render();
        },
        true,
      ),
    ),
    el(
      "p",
      { role: "status", "aria-live": "polite", class: "status", id: "status" },
      status,
    ),
  );
  if (!navigator.onLine)
    main.append(el("p", { class: "notice" }, t("offline")));
  if (storageError) main.append(el("p", { class: "notice" }, t("storage")));
  if (screen === "create") renderCreate(main);
  else if (screen === "edit" && plan) renderPlan(main);
  else renderSaved(main);
  main.append(el("footer", {}, t("adult"), " · ", t("draftSaved")));
  if (busy)
    for (const n of root.querySelectorAll("input,select,textarea,button"))
      if (n.id !== "cancel") n.disabled = true;
}
function cloudControl() {
  return el(
    "div",
    { class: "privacy" },
    checkbox(
      t("cloud"),
      cloudAllowed,
      (v) => {
        cloudAllowed = v;
        render();
      },
      "cloud",
    ),
    el(
      "p",
      { class: "muted" },
      !cloudConfigured
        ? t("cloudOff")
        : cloudAllowed
          ? t("cloudInfo")
          : t("localInfo"),
    ),
  );
}
function renderCreate(main) {
  const card = el("section", { class: "card" });
  main.append(card);
  card.append(
    el(
      "div",
      { class: "hero" },
      el(
        "div",
        {},
        el("h1", {}, t("parentHeading")),
        el("p", {}, t("parentIntro")),
      ),
      el("img", { src: starry, alt: "", class: "mascot" }),
    ),
  );
  const form = el("form", {
    onsubmit: (e) => {
      e.preventDefault();
      suggest(false);
    },
  });
  card.append(form);
  form.append(
    field(
      t("parentGoal"),
      el("textarea", {
        id: "goal",
        rows: 3,
        required: true,
        maxLength: 1200,
        value: input.goal,
        placeholder: t("parentPlaceholder"),
        oninput: (e) => (input.goal = e.target.value),
      }),
    ),
    el("p", { class: "muted" }, t("privacy")),
    el(
      "div",
      { class: "options" },
      field(
        t("ageBand"),
        select(
          input.ageBand,
          ["unspecified", "preschool", "primary", "teen"],
          (v) => (input.ageBand = v),
          "age-band",
          (v) => t("age_" + v),
        ),
      ),
      field(
        t("minutes"),
        el("input", {
          id: "minutes",
          type: "number",
          min: 1,
          max: 180,
          value: input.minutes,
          placeholder: "20",
          oninput: (e) => (input.minutes = e.target.value),
        }),
      ),
    ),
    el(
      "details",
      {},
      el("summary", {}, t("rewardPreferences")),
      el("p", { class: "muted" }, t("rewardPreferencesInfo")),
      ...["no_purchases", "no_screens"].map((c) =>
        checkbox(
          t(c),
          input.constraints.includes(c),
          (v) =>
            (input.constraints = v
              ? [...input.constraints, c]
              : input.constraints.filter((x) => x !== c)),
          c,
        ),
      ),
    ),
    cloudControl(),
    el(
      "details",
      {},
      el("summary", {}, t("settings")),
      el("p", {}, t("setup")),
    ),
    el(
      "div",
      { class: "actions" },
      el(
        "button",
        { type: "submit", disabled: busy },
        busy ? t("working") : t("suggestIdeas"),
      ),
      button(t("manual"), manual, true),
    ),
  );
  if (!cloudConfigured) form.querySelector("#cloud").disabled = true;
  if (busy)
    form.append(
      button(t("cancel"), () => controller?.abort(), true, { id: "cancel" }),
    );
}
async function suggest(more) {
  if (busy) return;
  if (more && !feedback.trim()) return;
  const current = plan
    ? structuredClone(plan)
    : blankPlan(lang, t("draftTitle"));
  const r = {
    parentPlanning: true,
    goal: input.goal.trim() || (more ? feedback.trim() : ""),
    language: current.language,
    ageBand: input.ageBand,
    availableMinutes: input.minutes === "" ? null : Number(input.minutes),
    constraints: [...input.constraints],
    feedback: more ? feedback.trim() : "",
    ...contextFor(current),
  };
  controller = new AbortController();
  const signal = controller.signal;
  busy = true;
  status = t("working");
  render();
  try {
    const result = await cancellable(
      generate(r, {
        cloudAllowed,
        signal,
        onProgress: (p) => {
          if (!signal.aborted)
            message(t("download") + " " + Math.round(p * 100) + "%");
        },
      }),
      signal,
      600000,
    );
    if (signal.aborted) return;
    const merged = mergeSuggestions(current, result.draft);
    plan = merged.plan;
    receipt = result.receipt;
    reason = result.draft.reason;
    feedback = "";
    screen = "edit";
    status = merged.added ? t("ideasAdded") : t("noNewIdeas");
    persist();
  } catch (e) {
    if (e.message === "TIMEOUT") controller?.abort();
    const diagnostic = String(e.code || e.message || "unknown");
    console.warn(
      "Routine generation code:",
      /^[A-Za-z0-9_/-]{1,80}$/.test(diagnostic) ? diagnostic : "unknown",
    );
    message(
      t(
        signal.aborted
          ? "cancelled"
          : e.message === "NEEDS_INPUT"
            ? "needs"
            : "error",
      ),
    );
  } finally {
    busy = false;
    controller = null;
    render();
  }
}
const emoji = {
  Check: "✓",
  Alarm: "⏰",
  Bed: "🛏",
  Brush: "🪥",
  School: "🎒",
  MenuBook: "📖",
  Redeem: "🎁",
  MusicNote: "♫",
  SmartToy: "🧩",
  Celebration: "🎉",
  SelfImprovement: "☀",
};
const colorHex = {
  GREEN: "#15803d",
  BLUE: "#1d4ed8",
  GRAY: "#64748b",
  WHITE: "#cbd5e1",
  PURPLE: "#7e22ce",
  YELLOW: "#a16207",
  RED: "#b91c1c",
};
function renderPlan(main) {
  const card = el("section", { class: "card" });
  main.append(card);
  card.append(el("h1", {}, t("chooseHeading")), el("p", {}, t("chooseInfo")));
  if (receipt)
    card.append(
      el(
        "p",
        { class: "badge", id: "receipt" },
        t(receipt.source === "ON_DEVICE" ? "local" : "remote") +
          " · " +
          receipt.model,
      ),
    );
  if (reason)
    card.append(el("p", { class: "notice", id: "plan-reason" }, reason));
  card.append(
    field(
      t("title"),
      el("input", {
        id: "title",
        maxLength: 120,
        value: plan.title,
        oninput: (e) => {
          plan.title = e.target.value;
          persist();
        },
      }),
    ),
  );
  for (const kind of ["task", "reward"]) {
    card.append(
      el(
        "h2",
        { class: "section-title" },
        t(kind === "task" ? "taskIdeas" : "rewardIdeas"),
      ),
    );
    for (const item of plan.items.filter((x) => x.kind === kind))
      card.append(itemCard(item));
    card.append(
      button(
        "+ " + t(kind === "task" ? "addOwnTask" : "addOwnReward"),
        () => {
          if (plan.items.length >= 40) {
            message(t("planFull"));
            return;
          }
          plan.items.push(newItem(kind));
          persist();
          render();
        },
        true,
      ),
    );
  }
  const selected = plan.items.filter((x) => x.state === "kept").length;
  card.append(
    el(
      "p",
      { class: "selection-count", id: "selected-count" },
      t("chosenCount").replace("{n}", selected),
    ),
    el(
      "div",
      { class: "actions" },
      button(t("saveIdeas"), () => {
        try {
          savePlan(localStorage, plan);
          message(t("savedOk"));
        } catch {
          message(t("storage"));
        }
      }),
      button(t("exportSelected"), download, true, {
        id: "export",
        disabled: !selected,
      }),
    ),
    el("p", { class: "muted" }, t("richExportInfo")),
  );
  const more = el(
    "section",
    { class: "card more-card" },
    el("h2", {}, t("moreHeading")),
    el("p", {}, t("moreInfo")),
    field(
      t("moreContext"),
      el("textarea", {
        id: "feedback",
        rows: 3,
        maxLength: 600,
        value: feedback,
        placeholder: t("moreHint"),
        oninput: (e) => (feedback = e.target.value),
      }),
    ),
    cloudControl(),
    button(busy ? t("working") : t("moreIdeas"), () => suggest(true), false, {
      id: "more-ideas",
    }),
  );
  main.append(more);
  if (!cloudConfigured) more.querySelector("#cloud").disabled = true;
  if (busy)
    more.append(
      button(t("cancel"), () => controller?.abort(), true, { id: "cancel" }),
    );
}
function itemCard(item) {
  const node = el("article", {
    class: "idea-card " + (item.state === "kept" ? "is-kept" : ""),
    "data-item-id": item.id,
    "data-kind": item.kind,
  });
  node.append(
    el(
      "div",
      { class: "idea-top" },
      el(
        "span",
        {
          class: "idea-icon",
          style: "color:" + colorHex[item.color],
          "aria-hidden": "true",
        },
        emoji[item.icon],
      ),
      el("h3", {}, item.name || t("untitled")),
    ),
  );
  node.append(
    el("p", { class: "idea-description" }, item.description),
    el(
      "p",
      { class: "idea-meta" },
      String(item.pointAmount) +
        " " +
        t("stars") +
        " · " +
        t("freq_" + item.limitType),
    ),
    el(
      "div",
      { class: "actions" },
      button(
        item.state === "kept" ? "✓ " + t("kept") : t("keep"),
        () => {
          item.state = item.state === "kept" ? "suggested" : "kept";
          persist();
          render();
        },
        item.state !== "kept",
        { "aria-pressed": item.state === "kept", "data-action": "keep" },
      ),
      button(
        t("discard"),
        () => {
          try {
            plan = discardItem(plan, item.id);
            persist();
            render();
          } catch {
            message(t("planFull"));
          }
        },
        true,
        { "data-action": "discard" },
      ),
    ),
  );
  const details = el(
    "details",
    {
      open: expanded.has(item.id),
      ontoggle: (e) => {
        if (!e.target.isConnected) return;
        if (e.target.open) expanded.add(item.id);
        else expanded.delete(item.id);
      },
    },
    el("summary", {}, t("editDetails")),
  );
  node.append(details);
  const update = (k, v) => {
    item[k] = v;
    persist();
    node.querySelector("h3").textContent = item.name || t("untitled");
    node.querySelector(".idea-description").textContent = item.description;
    node.querySelector(".idea-meta").textContent =
      String(item.pointAmount) +
      " " +
      t("stars") +
      " · " +
      t("freq_" + item.limitType);
  };
  details.append(
    field(
      t("itemName"),
      el("input", {
        value: item.name,
        maxLength: 120,
        "aria-label": t("itemName"),
        oninput: (e) => update("name", e.target.value),
      }),
    ),
    field(
      t("description"),
      el("textarea", {
        value: item.description,
        rows: 2,
        maxLength: 2000,
        "aria-label": t("description"),
        oninput: (e) => update("description", e.target.value),
      }),
    ),
    el(
      "div",
      { class: "options" },
      field(
        t("stars"),
        el("input", {
          value: item.pointAmount,
          inputMode: "numeric",
          maxLength: 7,
          "aria-label": t("stars"),
          oninput: (e) => update("pointAmount", e.target.value),
        }),
      ),
      field(
        t("icon"),
        select(
          item.icon,
          icons,
          (v) => {
            update("icon", v);
            render();
          },
          "icon-" + item.id,
          (v) => emoji[v] + " " + t("icon_" + v),
        ),
      ),
      field(
        t("color"),
        select(
          item.color,
          colors,
          (v) => {
            update("color", v);
            render();
          },
          "color-" + item.id,
          (v) => t("color_" + v),
        ),
      ),
    ),
    el(
      "div",
      { class: "options" },
      field(
        t("frequency"),
        select(
          item.limitType,
          frequencies,
          (v) => {
            update("limitType", v);
            render();
          },
          "frequency-" + item.id,
          (v) => t("freq_" + v),
        ),
      ),
      field(
        t("frequencyCount"),
        el("input", {
          value: item.limitValue,
          inputMode: "numeric",
          maxLength: 5,
          "aria-label": t("frequencyCount"),
          oninput: (e) => update("limitValue", e.target.value),
        }),
      ),
    ),
    checkbox(
      t("parentApproval"),
      item.requireApproval,
      (v) => update("requireApproval", v),
      "approval-" + item.id,
    ),
  );
  return node;
}
function download() {
  let json;
  try {
    json = exportPlan(plan);
  } catch (e) {
    message(
      t(
        e.message === "DUPLICATE"
          ? "duplicate"
          : e.message === "SELECTION"
            ? "chooseFirst"
            : "invalid",
      ),
    );
    return;
  }
  const url = URL.createObjectURL(
    new Blob([json], { type: "application/json" }),
  );
  const a = el("a", { href: url, download: "starjar-routine.json" });
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 30000);
  message(t("exported"));
}
function renderSaved(main) {
  const card = el("section", { class: "card" }, el("h1", {}, t("saved")));
  main.append(card);
  let values;
  try {
    values = loadPlans(localStorage);
  } catch {
    card.append(el("p", {}, t("storage")));
    return;
  }
  if (!values.length) card.append(el("p", {}, t("empty")));
  for (const p of values)
    card.append(
      el(
        "article",
        { class: "saved-item" },
        el("h2", {}, p.title),
        el(
          "p",
          {},
          t("chosenCount").replace(
            "{n}",
            p.items.filter((x) => x.state === "kept").length,
          ),
        ),
        el(
          "div",
          { class: "actions" },
          button(
            t("open"),
            () => {
              if (plan && plan.id !== p.id && !confirm(t("newConfirm"))) return;
              plan = p;
              input = {
                goal: "",
                ageBand: "unspecified",
                minutes: "",
                constraints: [],
              };
              feedback = "";
              reason = "";
              receipt = null;
              screen = "edit";
              persist();
              render();
            },
            true,
          ),
          button(
            t("delete"),
            () => {
              if (!confirm(t("confirm"))) return;
              try {
                deletePlan(localStorage, p.id);
                if (plan?.id === p.id) {
                  plan = null;
                  localStorage.removeItem("routine-draft-v2");
                  localStorage.removeItem("routine-draft-v1");
                }
                message(t("deleted"));
                render();
              } catch {
                message(t("storage"));
              }
            },
            true,
          ),
        ),
      ),
    );
}
restore();
render();
window.addEventListener("online", render);
window.addEventListener("offline", render);
if (import.meta.env.PROD && "serviceWorker" in navigator)
  navigator.serviceWorker.register("./sw.js").catch(() => {});
