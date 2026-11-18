// Thin client for the shared MPLab API. Every function resolves (never rejects) with a plain object,
// so the UI has a single code path. Network failures are reported as status "network_error".
import { config } from "./config.js";

async function request(path, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), options.timeoutMs || config.requestTimeoutMs);
  try {
    const res = await fetch(config.apiBase + path, {
      ...options,
      signal: controller.signal,
      headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    });
    let body = null;
    try {
      body = await res.json();
    } catch {
      body = null;
    }
    return { ok: res.ok, http: res.status, body };
  } catch (err) {
    const aborted = err && err.name === "AbortError";
    return {
      ok: false,
      http: 0,
      body: null,
      networkError: aborted ? "The server did not answer in time" : "Cannot reach the MPLab API at " + config.apiBase,
    };
  } finally {
    clearTimeout(timer);
  }
}

/** POST /execute -> { status, stdout, stderr, exit_code, ... } */
export async function execute({ language, source_code, stdin }) {
  const started = performance.now();
  const r = await request("/execute", {
    method: "POST",
    body: JSON.stringify({ language, source_code, stdin }),
  });
  const roundTripMs = Math.round(performance.now() - started);
  if (r.body && typeof r.body.status === "string") {
    return { ...r.body, round_trip_ms: roundTripMs, http_status: r.http };
  }
  return {
    status: r.networkError ? "network_error" : "internal_error",
    stdout: "",
    stderr: "",
    exit_code: null,
    message: r.networkError || `Unexpected response from server (HTTP ${r.http})`,
    round_trip_ms: roundTripMs,
    http_status: r.http,
  };
}

/** GET /languages -> [{ id, name, available, version }] or null if the endpoint is missing. */
export async function getLanguages() {
  const r = await request("/languages", { method: "GET", timeoutMs: 8000 });
  return r.ok && r.body && Array.isArray(r.body.languages) ? r.body.languages : null;
}

/** GET /examples/{lang} -> [{ id, title, concept, source_code }] (empty when none). */
export async function getExamples(language) {
  const r = await request("/examples/" + encodeURIComponent(language), { method: "GET", timeoutMs: 8000 });
  return r.ok && r.body && Array.isArray(r.body.examples) ? r.body.examples : [];
}

/** GET /health -> true/false */
export async function health() {
  const r = await request("/health", { method: "GET", timeoutMs: 5000 });
  return { up: r.ok, info: r.body };
}
