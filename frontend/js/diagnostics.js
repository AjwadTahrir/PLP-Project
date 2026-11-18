// Extracts line numbers from compiler / interpreter output so the editor can highlight them.
// Patterns cover the formats of all ten MPLab toolchains; Group 8 (error reporting) can extend this list.

const FILE_LINE =
  /(?:^|[\s"'(])[\w./\\-]*\.(?:java|c|cc|cpp|cxx|h|hpp|go|rb|php|hs|pl|pro|js|mjs|py)[:(](\d+)(?:[:,](\d+))?/;

const PATTERNS = [
  FILE_LINE,                         // javac, gcc/g++, go, ghc, ruby, node, swipl, java stack traces
  /File "[^"]*", line (\d+)/,        // CPython tracebacks
  /\bon line (\d+)/,                 // PHP
];

/**
 * @param {string} text  stderr of a run
 * @returns {{line:number, column:number|null, text:string, kind:'error'|'warning'|'trace'}[]}
 */
export function parseDiagnostics(text) {
  if (!text) return [];
  const seen = new Set();
  const out = [];
  for (const raw of text.split("\n")) {
    const line = raw.trimEnd();
    for (const re of PATTERNS) {
      const m = re.exec(line);
      if (!m) continue;
      const n = parseInt(m[1], 10);
      if (!Number.isFinite(n) || n <= 0) break;
      // Skip frames from the language runtime itself (e.g. java.base/...Scanner.java:977).
      if (/\bjava\.base\/|\/usr\/lib|node:internal/.test(line)) break;
      const key = n + "|" + line;
      if (!seen.has(key)) {
        seen.add(key);
        const kind = /warning/i.test(line) ? "warning" : /^\s*at\s|^\s*File "/.test(raw) ? "trace" : "error";
        out.push({ line: n, column: m[2] ? parseInt(m[2], 10) : null, text: line.trim(), kind });
      }
      break;
    }
  }
  return out;
}
