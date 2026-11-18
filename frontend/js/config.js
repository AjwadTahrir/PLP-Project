// Runtime configuration for the MPLab web interface.
// On the faculty VM the reverse proxy serves the UI and the API from the same origin, so "/api" works.
// For local development against another host, set window.MPLAB_API_BASE before app.js loads,
// or open the page with ?api=http://127.0.0.1:8080/api
const params = new URLSearchParams(location.search);

export const config = {
  apiBase: (params.get("api") || window.MPLAB_API_BASE || "/api").replace(/\/$/, ""),
  requestTimeoutMs: 30000,
  healthPollMs: 15000,
  maxSourceBytes: 64 * 1024,
};
