import "./style.css";
import { cancellable } from "./cancellation.js";
import starry from "../../androidApp/src/main/res/drawable/starry.png";
import { strings } from "./strings.js";
import { generate, capability, cloudConfigured } from "./ai.js";
import {
  points,
  exportRoutine,
  readSaved,
  saveRoutine,
  deleteRoutine,
} from "./core.js";
const $ = (id) => document.getElementById(id);
const root = $("app");
let lang = navigator.language.startsWith("de") ? "de" : "en",
  adult = false,
  screen = "create",
  edit = null,
  receipt = null,
  cloudAllowed = false,
  busy = false,
  controller = null,
  status = "",
  availability = "unavailable",
  storageFailed = false;
let input = { goal: "", count: 3, reward: true, constraints: [] };
const t = (k) => strings[lang][k] ?? k;
function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k.startsWith("on")) node.addEventListener(k.slice(2), v);
    else if (k === "class") node.className = v;
    else if (k === "text") node.textContent = v;
    else if (k in node) node[k] = v;
    else node.setAttribute(k, v);
  }
  for (const c of children.flat())
    if (c !== null && c !== undefined)
      node.append(c instanceof Node ? c : document.createTextNode(c));
  return node;
}
const button = (text, fn, secondary = false, extra = {}) =>
  el(
    "button",
    {
      type: "button",
      class: secondary ? "secondary" : "primary",
      onclick: fn,
      ...extra,
    },
    text,
  );
const field = (label, node) =>
  el("label", { class: "field" }, el("span", {}, label), node);
const check = (label, checked, fn, id) =>
  el(
    "label",
    { class: "check" },
    el("input", {
      type: "checkbox",
      checked,
      id,
      onchange: (e) => fn(e.target.checked),
    }),
    el("span", {}, label),
  );
function message(value) {
  status = value;
  const n = $("status");
  if (n) n.textContent = value;
}
function persist() {
  if (!edit) return;
  try {
    localStorage.setItem("routine-draft-v1", JSON.stringify({ edit, receipt }));
  } catch {
    storageFailed = true;
    message(t("storage"));
  }
}
function restore() {
  try {
    adult = localStorage.getItem("routine-adult-v1") === "yes";
    const raw = localStorage.getItem("routine-draft-v1");
    if (raw) {
      const data = JSON.parse(raw);
      const e = data.edit;
      if (
        !e ||
        typeof e.title !== "string" ||
        !Array.isArray(e.tasks) ||
        e.tasks.length > 3 ||
        !["en", "de"].includes(e.language)
      )
        throw Error();
      for (const i of [...e.tasks, ...(e.reward ? [e.reward] : [])])
        if (typeof i.name !== "string" || typeof i.pointAmount !== "string")
          throw Error();
      edit = e;
      receipt = ["ON_DEVICE", "IN_CLOUD", "REVIEWED"].includes(
        data.receipt?.source,
      )
        ? data.receipt
        : null;
      screen = "edit";
      status = t("restore");
    }
  } catch {
    storageFailed = true;
    status = t("storage");
  }
}
function fresh() {
  if (edit && !confirm(t("newConfirm"))) return;
  edit = null;
  receipt = null;
  status = "";
  screen = "create";
  try {
    localStorage.removeItem("routine-draft-v1");
  } catch {
    storageFailed = true;
  }
  render();
}
function editorFrom(draft) {
  edit = {
    id: crypto.randomUUID(),
    title: t("draftTitle"),
    language: lang,
    tasks: (draft?.tasks ?? Array(input.count).fill("")).map((name) => ({
      name,
      pointAmount: "5",
    })),
    reward: input.reward
      ? { name: draft?.reward ?? "", pointAmount: "20" }
      : null,
  };
  screen = "edit";
  persist();
  render();
}
function collect() {
  return {
    ...edit,
    title: edit.title.trim(),
    tasks: edit.tasks.map((x) => ({
      name: x.name.trim(),
      pointAmount: points(x.pointAmount),
    })),
    reward: edit.reward
      ? {
          name: edit.reward.name.trim(),
          pointAmount: points(edit.reward.pointAmount),
        }
      : null,
  };
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
        el(
          "select",
          {
            id: "language",
            disabled: busy,
            onchange: (e) => {
              lang = e.target.value;
              status = "";
              render();
            },
          },
          ...["en", "de"].map((v) =>
            el(
              "option",
              { value: v, selected: lang === v },
              v === "en" ? "English" : "Deutsch",
            ),
          ),
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
        el("h1", {}, t("heading")),
        el("p", {}, t("intro")),
        el("p", { class: "eyebrow" }, t("adult")),
        check(
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
              storageFailed = true;
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
      { "aria-label": lang === "de" ? "Routinen" : "Routines" },
      button(t("create"), fresh, screen !== "create", { disabled: busy }),
      button(
        t("saved"),
        () => {
          screen = "saved";
          status = "";
          render();
        },
        screen !== "saved",
        { disabled: busy },
      ),
    ),
  );
  main.append(
    el(
      "p",
      { id: "status", role: "status", "aria-live": "polite", class: "status" },
      status,
    ),
  );
  if (!navigator.onLine)
    main.append(el("p", { class: "notice" }, t("offline")));
  if (storageFailed) main.append(el("p", { class: "notice" }, t("storage")));
  if (screen === "create") renderCreate(main);
  if (screen === "edit") renderEdit(main);
  if (screen === "saved") renderSaved(main);
  main.append(el("footer", {}, t("adult"), " · ", t("draftSaved")));
}
function renderCreate(main) {
  const card = el("section", { class: "card" });
  main.append(card);
  card.append(
    el(
      "div",
      { class: "hero" },
      el("div", {}, el("h1", {}, t("heading")), el("p", {}, t("intro"))),
      el("img", { src: starry, alt: "", class: "mascot" }),
    ),
  );
  const form = el("form", {
    onsubmit: (e) => {
      e.preventDefault();
      startGeneration();
    },
  });
  card.append(form);
  form.append(
    field(
      t("goal"),
      el("textarea", {
        id: "goal",
        required: true,
        maxLength: 1200,
        rows: 3,
        value: input.goal,
        placeholder: t("placeholder"),
        disabled: busy,
        oninput: (e) => {
          input.goal = e.target.value;
        },
      }),
    ),
    el("p", { class: "muted" }, t("privacy")),
  );
  const options = el("div", { class: "options" });
  form.append(options);
  options.append(
    field(
      t("count"),
      el(
        "select",
        {
          id: "count",
          disabled: busy,
          onchange: (e) => (input.count = Number(e.target.value)),
        },
        ...[1, 2, 3].map((n) =>
          el("option", { value: n, selected: input.count === n }, String(n)),
        ),
      ),
    ),
    check(
      t("reward"),
      input.reward,
      (v) => (input.reward = v),
      "include-reward",
    ),
  );
  for (const c of ["no_purchases", "no_screens", "short_tasks"])
    options.append(
      check(
        t(c),
        input.constraints.includes(c),
        (v) => {
          input.constraints = v
            ? [...input.constraints, c]
            : input.constraints.filter((x) => x !== c);
        },
        c,
      ),
    );
  form.append(
    el(
      "div",
      { class: "privacy" },
      check(
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
        cloudAllowed ? t("cloudInfo") : t("localInfo"),
      ),
    ),
  );
  if (!cloudConfigured) {
    form.querySelector("#cloud").disabled = true;
    form.append(el("p", { class: "muted" }, t("cloudOff")));
  }
  form.append(
    el(
      "p",
      { class: "capability", id: "capability" },
      t(
        availability === "available"
          ? "ready"
          : availability === "downloadable" || availability === "downloading"
            ? "download"
            : "unavailable",
      ),
    ),
    el(
      "details",
      {},
      el("summary", {}, t("settings")),
      el("p", {}, t("setup")),
    ),
  );
  const actions = el(
    "div",
    { class: "actions" },
    el(
      "button",
      { type: "submit", disabled: busy },
      busy ? t("working") : t("generate"),
    ),
    button(
      t("manual"),
      () => {
        receipt = null;
        editorFrom(null);
      },
      true,
      { disabled: busy },
    ),
  );
  if (busy)
    actions.append(
      button(
        t("cancel"),
        () => {
          controller?.abort();
          message(t("cancelled"));
        },
        true,
      ),
    );
  form.append(actions);
  if (busy)
    for (const e of form.querySelectorAll("input,select")) e.disabled = true;
}
async function startGeneration() {
  if (busy) return;
  if (edit && !confirm(t("newConfirm"))) return;
  const r = {
    goal: input.goal,
    language: lang,
    taskCount: input.count,
    includeReward: input.reward,
    constraints: [...input.constraints],
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
            message(t("download") + " · " + Math.round(p * 100) + "%");
        },
      }),
      signal,
      600000,
    );
    if (signal.aborted) return;
    receipt = result.receipt;
    status = "";
    editorFrom(result.draft);
  } catch (e) {
    // Only bounded diagnostic codes; never log descriptions or model output.
    const diagnostic = String(e.code || e.message || "unknown");
    console.warn(
      "Routine generation code:",
      /^[A-Za-z0-9_/-]{1,80}$/.test(diagnostic) ? diagnostic : "unknown",
    );
    if (e.message === "TIMEOUT") controller?.abort();
    if (signal.aborted) message(t("cancelled"));
    else
      message(
        t(
          e.message === "NEEDS_INPUT"
            ? "needs"
            : !cloudAllowed && availability === "unavailable"
              ? "unsupported"
              : "error",
        ),
      );
  } finally {
    busy = false;
    controller = null;
    render();
  }
}
function renderEdit(main) {
  if (!edit) {
    screen = "create";
    render();
    return;
  }
  const card = el("section", { class: "card" });
  main.append(card);
  card.append(
    el("h1", {}, t("review")),
    el("p", {}, t("reviewInfo")),
    el(
      "p",
      { class: "badge", id: "receipt" },
      receipt?.source === "REVIEWED"
        ? t("reviewedLabel")
        : receipt
          ? t(receipt.source === "ON_DEVICE" ? "local" : "remote") +
            " · " +
            receipt.model +
            " · " +
            (receipt.durationMs / 1000).toFixed(1) +
            " s"
          : t("manualLabel"),
    ),
  );
  card.append(
    field(
      t("title"),
      el("input", {
        id: "title",
        maxLength: 120,
        value: edit.title,
        oninput: (e) => {
          edit.title = e.target.value;
          persist();
        },
      }),
    ),
  );
  const row = (item, label, remove) =>
    el(
      "div",
      { class: "item" },
      field(
        label,
        el("input", {
          maxLength: 120,
          value: item.name,
          "aria-label": label,
          oninput: (e) => {
            item.name = e.target.value;
            persist();
          },
        }),
      ),
      field(
        t("stars"),
        el("input", {
          type: "text",
          inputMode: "numeric",
          pattern: "[0-9]*",
          maxLength: 7,
          value: item.pointAmount,
          "aria-label": label + " " + t("stars"),
          oninput: (e) => {
            item.pointAmount = e.target.value;
            persist();
          },
        }),
      ),
      button(t("remove"), remove, true, {
        "aria-label": t("remove") + " " + label,
      }),
    );
  edit.tasks.forEach((item, i) =>
    card.append(
      row(item, t("task") + " " + (i + 1), () => {
        edit.tasks.splice(i, 1);
        persist();
        render();
      }),
    ),
  );
  if (edit.tasks.length < 3)
    card.append(
      button(
        t("add"),
        () => {
          edit.tasks.push({ name: "", pointAmount: "5" });
          persist();
          render();
        },
        true,
      ),
    );
  if (edit.reward)
    card.append(
      row(edit.reward, t("rewardName"), () => {
        edit.reward = null;
        persist();
        render();
      }),
    );
  else
    card.append(
      button(
        "+ " + t("rewardName"),
        () => {
          edit.reward = { name: "", pointAmount: "20" };
          persist();
          render();
        },
        true,
      ),
    );
  card.append(
    el(
      "div",
      { class: "actions" },
      button(t("save"), () => {
        try {
          saveRoutine(localStorage, collect());
          message(t("savedOk"));
        } catch (e) {
          message(
            t(
              ["POINTS", "NAME", "TASK_COUNT"].includes(e.message)
                ? "invalid"
                : "storage",
            ),
          );
        }
      }),
      button(
        t("export"),
        () => {
          let json;
          try {
            json = exportRoutine(collect());
          } catch {
            message(t("invalid"));
            return;
          }
          try {
            const url = URL.createObjectURL(
              new Blob([json], { type: "application/json" }),
            );
            const a = el("a", { href: url, download: "starjar-routine.json" });
            document.body.append(a);
            a.click();
            a.remove();
            setTimeout(() => URL.revokeObjectURL(url), 30000);
            message(t("exported"));
          } catch {
            message(t("exportFail"));
          }
        },
        true,
      ),
    ),
    el("p", { class: "muted" }, t("exportInfo")),
  );
}
function renderSaved(main) {
  const card = el("section", { class: "card" });
  main.append(card);
  card.append(el("h1", {}, t("saved")));
  let values;
  try {
    values = readSaved(localStorage);
  } catch {
    card.append(el("p", {}, t("storage")));
    return;
  }
  if (!values.length) card.append(el("p", {}, t("empty")));
  for (const r of values)
    card.append(
      el(
        "article",
        { class: "saved-item" },
        el("h2", {}, r.title),
        el("p", {}, r.tasks.map((x) => x.name).join(" · ")),
        el(
          "div",
          { class: "actions" },
          button(
            t("open"),
            () => {
              if (edit && edit.id !== r.id && !confirm(t("newConfirm"))) return;
              edit = {
                ...r,
                tasks: r.tasks.map((x) => ({
                  ...x,
                  pointAmount: String(x.pointAmount),
                })),
                reward: r.reward
                  ? { ...r.reward, pointAmount: String(r.reward.pointAmount) }
                  : null,
              };
              receipt = { source: "REVIEWED" };
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
                deleteRoutine(localStorage, r.id);
                status = t("deleted");
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
capability().then((value) => {
  availability = value;
  const label = $("capability");
  if (label)
    label.textContent = t(
      value === "available"
        ? "ready"
        : value === "downloadable" || value === "downloading"
          ? "download"
          : "unavailable",
    );
});
window.addEventListener("online", render);
window.addEventListener("offline", render);
// Build-time generated worker caches only the bundled app, never cloud traffic, tokens or prompts.
if (import.meta.env.PROD && "serviceWorker" in navigator)
  navigator.serviceWorker.register("./sw.js").catch(() => {});
