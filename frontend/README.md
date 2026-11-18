# MPLab Shared Web Interface (Group 3)

The single front end of MPLab. Plain HTML, CSS and ES modules with **no build step**: any static web server can serve this folder.

## Run locally

The Java module's server also serves this folder:

```powershell
cd languages\java
.\scripts\run-server.ps1      # http://127.0.0.1:8080
```

To use a different backend, add `?api=` to the URL, e.g. `http://127.0.0.1:5500/?api=http://127.0.0.1:8000/api`, or set `window.MPLAB_API_BASE` before `js/app.js` loads.

## Files

| File | Purpose |
|---|---|
| `index.html` | Shell: top bar, navigation, footer; loads CodeMirror from cdnjs |
| `css/styles.css` | Design tokens (light/dark) and shared components |
| `js/app.js` | Entry point: registers views, theme toggle, API status |
| `js/router.js` | Hash router and `registerView()` |
| `js/api.js` | **Only** place that talks to the backend |
| `js/editor.js` | Editor component (CodeMirror or textarea fallback) |
| `js/languages.js` | The 10 languages: ids, editor modes, templates |
| `js/diagnostics.js` | Parses compiler/runtime errors into line numbers |
| `js/dom.js` | `h()` element helper and safe `localStorage` wrapper |
| `js/views/playground.js` | Editor + run page (Group 3) |
| `js/views/placeholders.js` | Empty pages (Exercises, Lecturer, Compare) reserved for Groups 4, 5, 6 and 9 |

## Adding a page (Groups 4, 5, 6, 9)

Create `js/views/<your-page>.js`:

```js
import { h } from "../dom.js";
import { createEditor } from "../editor.js";
import { execute } from "../api.js";

export const exercisesView = {
  id: "exercises",            // same id replaces the placeholder
  title: "Exercises",
  render(container, params) {
    const host = h("div.editor-host");
    container.append(h("section.card", { style: "height:400px;display:flex;flex-direction:column" }, host));
    const editor = createEditor(host, { value: "", mode: "text/x-java" });
    // ... your UI
    return () => { /* optional cleanup when the user leaves the page */ };
  },
};
```

Then register it in `js/app.js` (one line in a PR reviewed by Group 3):

```js
import { exercisesView } from "./views/exercises.js";
registerView(exercisesView);
```

Or, without touching `app.js`, from your own script loaded after it: `window.MPLab.registerView({...})`.

### Shared components

| Class | Use |
|---|---|
| `.card`, `.card-head` | Panels |
| `.btn`, `.btn.primary`, `.btn.ghost` | Buttons |
| `.badge.ok / .warn / .bad / .muted` | Status labels (use for test-case verdicts) |
| `.pill` | Small tags (e.g. language, group) |
| `.console` | Monospace output block |
| `.muted` | Secondary text |

Use the CSS variables (`var(--accent)`, `var(--surface)`, `var(--text)`, …) instead of hard-coded colours so dark mode keeps working.

## Rules for shared files

* `api.js`, `router.js`, `styles.css` tokens: changes need a review from Group 3.
* New backend calls go into `api.js` as a function that **always resolves** (never throws) with an object containing `status`.
* Group 8 (diagnostics) may extend `PATTERNS` in `diagnostics.js`.

## Offline VM

If cdnjs is blocked, the editor falls back to a plain textarea automatically. To keep syntax highlighting, download CodeMirror 5.65.16 into `vendor/codemirror/` and change the URLs in `index.html`.
