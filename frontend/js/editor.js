// Code editor component. Uses CodeMirror 5 when it loaded from the CDN, otherwise a plain <textarea>
// with the same API, so the platform keeps working on an offline VM.

let prologDefined = false;

function definePrologMode(CM) {
  if (prologDefined || !CM.defineSimpleMode) return;
  prologDefined = true;
  CM.defineSimpleMode("prolog", {
    start: [
      { regex: /%.*/, token: "comment" },
      { regex: /\/\*/, token: "comment", next: "comment" },
      { regex: /"(?:[^\\"]|\\.)*"/, token: "string" },
      { regex: /'(?:[^\\']|\\.)*'/, token: "string" },
      { regex: /0'.|\d+(?:\.\d+)?/, token: "number" },
      { regex: /:-|-->|\?-|=\.\.|\\\+|->|=:=|=\\=|==|\\==|@<|@>|>=|=<|is\b/, token: "keyword" },
      { regex: /\b(?:true|fail|false|halt|nl|write|writeln|format|findall|member|append|length|initialization)\b/, token: "builtin" },
      { regex: /[A-Z_]\w*/, token: "variable-2" },
      { regex: /[a-z]\w*(?=\()/, token: "def" },
      { regex: /[a-z]\w*/, token: "atom" },
    ],
    comment: [
      { regex: /.*?\*\//, token: "comment", next: "start" },
      { regex: /.*/, token: "comment" },
    ],
    meta: { lineComment: "%" },
  });
}

/**
 * @param {HTMLElement} host
 * @param {{ value?: string, mode?: string, onRun?: () => void, onChange?: () => void,
 *           onCursor?: (line:number, col:number) => void }} opts
 */
export function createEditor(host, opts = {}) {
  const CM = window.CodeMirror;
  if (CM) {
    definePrologMode(CM);
    const runKeys = () => { if (opts.onRun) opts.onRun(); };
    const cm = CM(host, {
      value: opts.value || "",
      mode: opts.mode || "text/x-java",
      theme: "mplab",
      lineNumbers: true,
      indentUnit: 4,
      tabSize: 4,
      indentWithTabs: false,
      matchBrackets: true,
      autoCloseBrackets: true,
      styleActiveLine: true,
      lineWrapping: false,
      viewportMargin: 50,
      extraKeys: {
        "Ctrl-Enter": runKeys,
        "Cmd-Enter": runKeys,
        Tab: (ed) => (ed.somethingSelected() ? ed.indentSelection("add") : ed.replaceSelection(" ".repeat(ed.getOption("indentUnit")))),
        "Shift-Tab": (ed) => ed.indentSelection("subtract"),
      },
    });
    const marked = [];
    cm.on("change", () => opts.onChange && opts.onChange());
    cm.on("cursorActivity", () => {
      const c = cm.getCursor();
      if (opts.onCursor) opts.onCursor(c.line + 1, c.ch + 1);
    });
    return {
      kind: "codemirror",
      getValue: () => cm.getValue(),
      setValue: (v) => { cm.setValue(v); cm.clearHistory(); },
      setMode: (mode) => cm.setOption("mode", mode),
      setFontSize: (px) => { cm.getWrapperElement().style.fontSize = px + "px"; cm.refresh(); },
      focus: () => cm.focus(),
      refresh: () => cm.refresh(),
      markLines(lines) {
        this.clearMarks();
        for (const d of lines) {
          const idx = d.line - 1;
          if (idx < 0 || idx >= cm.lineCount()) continue;
          const cls = d.kind === "warning" ? "cm-warning-line" : "cm-error-line";
          cm.addLineClass(idx, "background", cls);
          marked.push({ idx, cls });
        }
      },
      clearMarks() {
        while (marked.length) {
          const m = marked.pop();
          cm.removeLineClass(m.idx, "background", m.cls);
        }
      },
      goTo(line, col) {
        const pos = { line: Math.max(0, line - 1), ch: Math.max(0, (col || 1) - 1) };
        cm.setCursor(pos);
        cm.scrollIntoView(pos, 80);
        cm.focus();
      },
    };
  }

  // ---------------------------------------------------------------- textarea fallback
  const ta = document.createElement("textarea");
  ta.className = "editor-fallback";
  ta.spellcheck = false;
  ta.setAttribute("aria-label", "Source code");
  ta.value = opts.value || "";
  host.appendChild(ta);
  ta.addEventListener("input", () => opts.onChange && opts.onChange());
  ta.addEventListener("keydown", (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
      e.preventDefault();
      if (opts.onRun) opts.onRun();
    } else if (e.key === "Tab" && !e.shiftKey) {
      e.preventDefault();
      const s = ta.selectionStart;
      ta.setRangeText("    ", s, ta.selectionEnd, "end");
      if (opts.onChange) opts.onChange();
    }
  });
  const reportCursor = () => {
    const before = ta.value.slice(0, ta.selectionStart).split("\n");
    if (opts.onCursor) opts.onCursor(before.length, before[before.length - 1].length + 1);
  };
  ta.addEventListener("keyup", reportCursor);
  ta.addEventListener("click", reportCursor);
  return {
    kind: "textarea",
    getValue: () => ta.value,
    setValue: (v) => { ta.value = v; },
    setMode: () => {},
    setFontSize: (px) => { ta.style.fontSize = px + "px"; },
    focus: () => ta.focus(),
    refresh: () => {},
    markLines: () => {},
    clearMarks: () => {},
    goTo(line) {
      const lines = ta.value.split("\n");
      const offset = lines.slice(0, Math.max(0, line - 1)).reduce((n, l) => n + l.length + 1, 0);
      ta.focus();
      ta.setSelectionRange(offset, offset + (lines[line - 1] || "").length);
    },
  };
}
