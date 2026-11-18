// Playground view: language picker, code editor, stdin, run, output/errors/details.
import { LANGUAGES, languageById, fileNameFor } from "../languages.js";
import { createEditor } from "../editor.js";
import { parseDiagnostics } from "../diagnostics.js";
import { execute, getLanguages, getExamples } from "../api.js";
import { config } from "../config.js";
import { h, store } from "../dom.js";

const STATUS_LABELS = {
  success: ["Success", "ok"],
  compile_error: ["Compilation error", "warn"],
  runtime_error: ["Runtime error", "bad"],
  timeout: ["Time limit exceeded", "warn"],
  memory_limit_exceeded: ["Memory limit exceeded", "warn"],
  unsupported_language: ["Language not available", "muted"],
  invalid_request: ["Invalid request", "muted"],
  internal_error: ["Server error", "bad"],
  network_error: ["Server unreachable", "bad"],
};

export function renderPlayground(container, params) {
  let lang = languageById(params.get("lang") || store.get("lang", "java"));
  let availability = new Map(); // id -> { available, version }
  let examples = [];
  let running = false;
  let lastResult = null;
  let activeTab = "output";
  let saveTimer = null;

  // ------------------------------------------------------------------ DOM
  const langSelect = h("select#lang-select", { "aria-label": "Programming language" },
    LANGUAGES.map((l) => h("option", { value: l.id }, l.name)));
  langSelect.value = lang.id;

  const exampleSelect = h("select#example-select", { "aria-label": "Load an example program", hidden: true });
  const resetBtn = h("button.btn.ghost", { type: "button", title: "Replace the code with the starter template" }, "Reset");
  const downloadBtn = h("button.btn.ghost", { type: "button", title: "Download the source file" }, "Download");
  const fontSelect = h("select", { "aria-label": "Editor font size", title: "Editor font size" },
    [12, 13, 14, 15, 16, 18, 20].map((n) => h("option", { value: n }, n + " px")));
  fontSelect.value = store.get("fontSize", "14");
  const runBtn = h("button.btn.primary#run-btn", { type: "button", title: "Run (Ctrl+Enter)" },
    h("span.run-icon", { "aria-hidden": "true" }), h("span.run-label", "Run"));

  const toolbar = h("div.toolbar",
    h("div.toolbar-group", h("label.field-label", { for: "lang-select" }, "Language"), langSelect),
    h("div.toolbar-group", exampleSelect),
    h("div.toolbar-group", resetBtn, downloadBtn, fontSelect),
    h("span.spacer"),
    h("kbd.hint", "Ctrl + Enter"),
    runBtn);

  const fileName = h("span.filename");
  const langMeta = h("span.lang-meta");
  const cursorPos = h("span.cursor-pos", "Ln 1, Col 1");
  const editorHost = h("div.editor-host");
  const exampleNote = h("div.example-note", { hidden: true });
  const editorCard = h("section.card.editor-card", { "aria-label": "Code editor" },
    h("div.card-head", fileName, langMeta, h("span.spacer"), cursorPos),
    exampleNote,
    editorHost);

  const stdinArea = h("textarea.stdin#stdin", {
    spellcheck: "false", placeholder: "Text typed here is sent to the program's standard input",
    "aria-label": "Program input (stdin)",
  });
  const stdinCard = h("section.card.stdin-card",
    h("div.card-head", h("label", { for: "stdin" }, "Input"), h("span.muted", "stdin")),
    stdinArea);

  const badge = h("span.badge.muted", "Ready");
  const resultMsg = h("span.result-msg", "Write some code and press Run.");
  const resultMeta = h("span.result-meta");
  const tabs = {
    output: h("button.tab", { type: "button", role: "tab", dataset: { tab: "output" } }, "Output"),
    errors: h("button.tab", { type: "button", role: "tab", dataset: { tab: "errors" } }, "Errors"),
    details: h("button.tab", { type: "button", role: "tab", dataset: { tab: "details" } }, "Details"),
  };
  const outPre = h("pre.console", { tabindex: "0" });
  const diagList = h("ul.diag-list");
  const panel = h("div.panel", { role: "tabpanel" }, diagList, outPre);
  const outputCard = h("section.card.output-card", { "aria-label": "Program output" },
    h("div.result-bar", { "aria-live": "polite" }, badge, resultMsg, h("span.spacer"), resultMeta),
    h("div.tabs", { role: "tablist" }, Object.values(tabs)),
    panel);

  container.appendChild(h("div.playground", toolbar,
    h("div.workspace", editorCard, h("div.side", stdinCard, outputCard))));

  // ------------------------------------------------------------------ editor
  const editor = createEditor(editorHost, {
    value: loadCode(lang),
    mode: lang.mode,
    onRun: run,
    onChange: () => {
      editor.clearMarks();
      updateFileName();
      clearTimeout(saveTimer);
      saveTimer = setTimeout(() => store.set("code." + lang.id, editor.getValue()), 400);
    },
    onCursor: (l, c) => { cursorPos.textContent = `Ln ${l}, Col ${c}`; },
  });
  editor.setFontSize(Number(fontSelect.value));
  stdinArea.value = store.get("stdin." + lang.id, "");

  // ------------------------------------------------------------------ behaviour
  function loadCode(l) {
    return store.get("code." + l.id, null) ?? l.template;
  }

  function updateFileName() {
    fileName.textContent = fileNameFor(lang, editor.getValue());
  }

  function updateLangMeta() {
    const a = availability.get(lang.id);
    langMeta.innerHTML = "";
    langMeta.append(lang.toolchain);
    if (a && a.version) langMeta.append(" · " + a.version);
    if (a && !a.available) langMeta.append(h("span.pill.muted", "not integrated yet"));
  }

  async function loadExamples() {
    const forLang = lang.id;
    examples = await getExamples(forLang);
    if (forLang !== lang.id) return; // user switched language meanwhile
    exampleSelect.innerHTML = "";
    exampleSelect.hidden = examples.length === 0;
    exampleSelect.append(h("option", { value: "" }, `Examples (${examples.length})`));
    examples.forEach((ex, i) => exampleSelect.append(h("option", { value: String(i) }, ex.title)));
  }

  function switchLanguage(id) {
    store.set("code." + lang.id, editor.getValue());
    store.set("stdin." + lang.id, stdinArea.value);
    lang = languageById(id);
    store.set("lang", lang.id);
    editor.setMode(lang.mode);
    editor.setValue(loadCode(lang));
    stdinArea.value = store.get("stdin." + lang.id, "");
    exampleNote.hidden = true;
    updateFileName();
    updateLangMeta();
    loadExamples();
    history.replaceState(null, "", "#/playground?lang=" + lang.id);
    editor.focus();
  }

  async function run() {
    if (running) return;
    const source = editor.getValue();
    if (!source.trim()) {
      showResult({ status: "invalid_request", message: "The editor is empty.", stdout: "", stderr: "" });
      return;
    }
    if (new Blob([source]).size > config.maxSourceBytes) {
      showResult({ status: "invalid_request", message: `Source is larger than ${config.maxSourceBytes / 1024} KB.`, stdout: "", stderr: "" });
      return;
    }
    running = true;
    store.set("code." + lang.id, source);
    store.set("stdin." + lang.id, stdinArea.value);
    runBtn.disabled = true;
    runBtn.classList.add("busy");
    runBtn.querySelector(".run-label").textContent = "Running";
    badge.className = "badge running";
    badge.textContent = "Running";
    resultMsg.textContent = `Compiling and running ${lang.name}...`;
    resultMeta.textContent = "";
    editor.clearMarks();
    const t0 = performance.now();
    const ticker = setInterval(() => {
      resultMeta.textContent = ((performance.now() - t0) / 1000).toFixed(1) + " s";
    }, 100);
    try {
      const result = await execute({ language: lang.id, source_code: source, stdin: stdinArea.value });
      showResult(result);
    } finally {
      clearInterval(ticker);
      running = false;
      runBtn.disabled = false;
      runBtn.classList.remove("busy");
      runBtn.querySelector(".run-label").textContent = "Run";
    }
  }

  function showResult(r) {
    lastResult = r;
    const [label, tone] = STATUS_LABELS[r.status] || [r.status, "muted"];
    badge.className = "badge " + tone;
    badge.textContent = label;
    resultMsg.textContent = r.message || "";
    const meta = [];
    if (r.exit_code !== undefined && r.exit_code !== null) meta.push("exit " + r.exit_code);
    if (r.execution_time_ms) meta.push("run " + r.execution_time_ms + " ms");
    if (r.compile_time_ms) meta.push("compile " + r.compile_time_ms + " ms");
    if (r.round_trip_ms) meta.push("total " + r.round_trip_ms + " ms");
    resultMeta.textContent = meta.join(" · ");

    const diags = parseDiagnostics(r.stderr || "");
    editor.markLines(diags.filter((d) => d.kind !== "trace" || r.status === "runtime_error"));
    tabs.errors.textContent = "Errors" + (r.stderr ? ` (${Math.max(diags.length, 1)})` : "");
    tabs.errors.classList.toggle("has-errors", !!r.stderr && r.status !== "success");

    const preferErrors = r.status !== "success" && (r.stderr || !r.stdout);
    selectTab(preferErrors ? "errors" : "output");
  }

  function selectTab(name) {
    activeTab = name;
    for (const [k, b] of Object.entries(tabs)) {
      b.classList.toggle("active", k === name);
      b.setAttribute("aria-selected", String(k === name));
    }
    const r = lastResult;
    diagList.innerHTML = "";
    diagList.hidden = true;
    outPre.className = "console";
    if (!r) {
      outPre.textContent = "";
      return;
    }
    if (name === "output") {
      outPre.textContent = r.stdout || "";
      if (!r.stdout) outPre.append(h("span.placeholder", "(no output on stdout)"));
      if (r.truncated) outPre.append(h("span.placeholder", "\n… output truncated by the server limit"));
    } else if (name === "errors") {
      outPre.classList.add("stderr");
      const diags = parseDiagnostics(r.stderr || "");
      if (diags.length) {
        diagList.hidden = false;
        for (const d of diags.slice(0, 20)) {
          diagList.append(h("li", h("button.diag", { type: "button", onclick: () => editor.goTo(d.line, d.column) },
            h("span.diag-line", "Line " + d.line), h("span.diag-text", d.text))));
        }
      }
      outPre.textContent = r.stderr || (r.status === "success" ? "" : r.message || "");
      if (!outPre.textContent) outPre.append(h("span.placeholder", "(no errors)"));
    } else {
      const { round_trip_ms, http_status, ...apiFields } = r;
      outPre.textContent = JSON.stringify(apiFields, null, 2) + `\n\n// HTTP ${http_status ?? "-"}, round trip ${round_trip_ms ?? "-"} ms`;
    }
  }

  // ------------------------------------------------------------------ events
  langSelect.addEventListener("change", () => switchLanguage(langSelect.value));
  exampleSelect.addEventListener("change", () => {
    const ex = examples[Number(exampleSelect.value)];
    exampleSelect.value = "";
    if (!ex) return;
    editor.setValue(ex.source_code);
    exampleNote.hidden = false;
    exampleNote.innerHTML = "";
    exampleNote.append(h("strong", ex.title), ex.concept ? " — " + ex.concept : "");
    updateFileName();
    editor.focus();
  });
  resetBtn.addEventListener("click", () => {
    if (editor.getValue() !== lang.template && !confirm("Replace your code with the starter template?")) return;
    editor.setValue(lang.template);
    store.remove("code." + lang.id);
    exampleNote.hidden = true;
    updateFileName();
  });
  downloadBtn.addEventListener("click", () => {
    const blob = new Blob([editor.getValue()], { type: "text/plain" });
    const a = h("a", { href: URL.createObjectURL(blob), download: fileNameFor(lang, editor.getValue()) });
    document.body.appendChild(a);
    a.click();
    setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 0);
  });
  fontSelect.addEventListener("change", () => {
    editor.setFontSize(Number(fontSelect.value));
    store.set("fontSize", fontSelect.value);
  });
  runBtn.addEventListener("click", run);
  stdinArea.addEventListener("input", () => store.set("stdin." + lang.id, stdinArea.value));
  for (const b of Object.values(tabs)) b.addEventListener("click", () => selectTab(b.dataset.tab));
  const onKey = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter" && !e.defaultPrevented) {
      e.preventDefault();
      run();
    }
  };
  document.addEventListener("keydown", onKey);

  // ------------------------------------------------------------------ init
  updateFileName();
  updateLangMeta();
  selectTab("output");
  loadExamples();
  getLanguages().then((list) => {
    if (!list) return;
    availability = new Map(list.map((l) => [l.id, l]));
    for (const opt of langSelect.options) {
      const a = availability.get(opt.value);
      const base = languageById(opt.value).name;
      opt.textContent = a && !a.available ? base + " (coming soon)" : base;
    }
    updateLangMeta();
  });
  setTimeout(() => { editor.refresh(); editor.focus(); }, 0);

  return () => {
    document.removeEventListener("keydown", onKey);
    store.set("code." + lang.id, editor.getValue());
  };
}
