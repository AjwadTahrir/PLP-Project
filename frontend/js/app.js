// Entry point of the shared MPLab web interface.
import { registerView, startRouter } from "./router.js";
import { renderPlayground } from "./views/playground.js";
import { placeholderViews } from "./views/placeholders.js";
import { health } from "./api.js";
import { config } from "./config.js";
import { store } from "./dom.js";

registerView({ id: "playground", title: "Playground", owner: "Group 3", render: renderPlayground });
placeholderViews.forEach(registerView);

// Expose the registry so other groups can add pages from their own script files without editing app.js.
window.MPLab = { registerView, config };

startRouter({
  nav: document.getElementById("nav"),
  container: document.getElementById("view"),
  fallback: "playground",
});

// ---- theme toggle -------------------------------------------------------------------------------
document.getElementById("theme-toggle").addEventListener("click", () => {
  const root = document.documentElement;
  const isDark = root.dataset.theme
    ? root.dataset.theme === "dark"
    : matchMedia("(prefers-color-scheme: dark)").matches;
  root.dataset.theme = isDark ? "light" : "dark";
  store.set("theme", root.dataset.theme);
});

// ---- API status indicator -----------------------------------------------------------------------
const statusEl = document.getElementById("api-status");
async function pollHealth() {
  const { up, info } = await health();
  statusEl.classList.toggle("up", up);
  statusEl.classList.toggle("down", !up);
  statusEl.title = up
    ? `API online (${config.apiBase})` + (info && info.sandbox ? ` · sandbox: ${info.sandbox}` : "")
    : `API offline - cannot reach ${config.apiBase}`;
  statusEl.querySelector(".label").textContent = up ? "API online" : "API offline";
}
pollHealth();
setInterval(pollHealth, config.healthPollMs);

if (!window.CodeMirror) {
  console.warn("CodeMirror did not load (offline?). Using the plain-text editor fallback.");
}
