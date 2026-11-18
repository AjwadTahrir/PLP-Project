// Tiny hash router + view registry. This is the integration point of the shared web interface:
// other groups add their pages by calling registerView() - see frontend/README.md.

const views = new Map();
let current = null;
let navEl = null;
let rerender = null;

/**
 * @param {{ id: string, title: string, owner?: string, nav?: boolean,
 *           render: (container: HTMLElement, params: URLSearchParams) => (void | (() => void)) }} view
 *   render may return a cleanup function that is called when the user leaves the view.
 */
export function registerView(view) {
  views.set(view.id, { nav: true, ...view });
  // Views registered after start-up (window.MPLab.registerView) appear immediately.
  if (navEl) {
    renderNav(navEl);
    rerender();
  }
}

export function startRouter({ nav, container, fallback }) {
  navEl = nav;
  renderNav(nav);
  const go = () => {
    const [path, query = ""] = location.hash.replace(/^#\/?/, "").split("?");
    const id = views.has(path) ? path : fallback;
    if (current && current.cleanup) current.cleanup();
    container.innerHTML = "";
    const view = views.get(id);
    const cleanup = view.render(container, new URLSearchParams(query));
    current = { id, cleanup: typeof cleanup === "function" ? cleanup : null };
    document.title = view.title + " - MPLab";
    for (const a of nav.querySelectorAll("a")) {
      a.classList.toggle("active", a.dataset.view === id);
      if (a.dataset.view === id) a.setAttribute("aria-current", "page");
      else a.removeAttribute("aria-current");
    }
  };
  rerender = go;
  window.addEventListener("hashchange", go);
  go();
}

function renderNav(nav) {
  nav.innerHTML = "";
  for (const v of views.values()) {
    if (!v.nav) continue;
    const a = document.createElement("a");
    a.href = "#/" + v.id;
    a.dataset.view = v.id;
    a.textContent = v.title;
    nav.appendChild(a);
  }
}
