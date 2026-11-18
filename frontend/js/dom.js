// Small DOM helpers shared by all views.

/** h("div.card#id", { onclick, title }, child1, "text", ...) */
export function h(tag, attrs, ...children) {
  const [name, ...rest] = tag.split(/(?=[.#])/);
  const el = document.createElement(name || "div");
  for (const part of rest) {
    if (part[0] === ".") el.classList.add(part.slice(1));
    else if (part[0] === "#") el.id = part.slice(1);
  }
  if (attrs && (typeof attrs !== "object" || attrs instanceof Node || Array.isArray(attrs))) {
    children.unshift(attrs);
    attrs = null;
  }
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v == null || v === false) continue;
    if (k.startsWith("on") && typeof v === "function") el.addEventListener(k.slice(2), v);
    else if (k === "dataset") Object.assign(el.dataset, v);
    else if (k === "text") el.textContent = v;
    else el.setAttribute(k, v === true ? "" : v);
  }
  for (const c of children.flat()) {
    if (c == null || c === false) continue;
    el.appendChild(c instanceof Node ? c : document.createTextNode(String(c)));
  }
  return el;
}

export const store = {
  get(key, fallback = null) {
    try {
      const v = localStorage.getItem("mplab." + key);
      return v === null ? fallback : v;
    } catch {
      return fallback;
    }
  },
  set(key, value) {
    try {
      localStorage.setItem("mplab." + key, value);
    } catch {
      /* storage unavailable (private mode) - ignore */
    }
  },
  remove(key) {
    try {
      localStorage.removeItem("mplab." + key);
    } catch {
      /* ignore */
    }
  },
};
