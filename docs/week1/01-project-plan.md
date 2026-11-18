# Group 3 – Project Plan

**Course:** Programming Language Paradigms · **Project:** MPLab (Multi-Language Programming Lab)
**Group:** 3 · **Language:** Java (OpenJDK) · **Shared-platform responsibility:** Shared web interface and code editor
**Document status:** Week 1 deliverable · version 1.0

> Replace every `[Member N]` placeholder with real names. Use the same names in the class *Student Information* Excel sheet.

---

## 1. Scope

Group 3 delivers two compulsory components (Brief §4):

| Component | What we build | Main consumers |
|---|---|---|
| **A. Java language module** | Service that receives `{language, source_code, stdin}`, compiles with `javac`, runs with `java` under limits, and returns `{status, stdout, stderr, exit_code, …}` in the agreed API format | Execution controller (Group 2), assessment engine (Group 5) |
| **B. Shared web interface & code editor** | The single front end of MPLab: layout, navigation, design system, code editor with syntax highlighting for all 10 languages, stdin box, run button, output/error panels, and the view registry that other groups plug their pages into | All students and lecturers; Groups 4, 5, 6, 8, 9 build their pages on it |

**Out of scope** (Brief §5): authentication beyond basics, plagiarism detection, real-time collaborative editing, advanced analytics.

## 2. Objectives

1. Explain how Java source becomes bytecode and runs on the JVM (Task A).
2. Install and verify OpenJDK on the faculty VM and document it (Task B).
3. Implement and test the Java execution module against the shared API (Tasks C, E).
4. Produce at least five Java concept demonstrations runnable in MPLab (Task D). Seven are prepared.
5. Deliver a usable, accessible, responsive web interface that every group integrates into.

## 3. Team structure and roles

| Role | Member | Responsibilities |
|---|---|---|
| Group leader / integration rep | [Member 1] | Weekly planning, logbook, represents Group 3 at the Project Integration Committee, merges PRs |
| Java module lead | [Member 2] | `JavaExecutor`, process/sandbox handling, API conformance |
| Java toolchain & testing | [Member 3] | VM installation, installation guide, test suite, cross-group tests |
| Front-end lead | [Member 4] | App shell, router, editor component, design tokens |
| Front-end developer / UX | [Member 5] | Playground view, output/diagnostics panels, accessibility, responsive layout |
| Documentation & examples | [Member 6] | Java language investigation, concept examples, technical report drafts |

Everyone writes code, reviews at least one PR a week, and records their contributions in the logbook.

## 4. Work breakdown and milestones (Weeks 1–8)

| Week | Brief milestone | Group 3 deliverables | Status |
|---|---|---|---|
| 1 | Requirements, architecture, repo, API agreement | This plan, group contract, Gantt chart, architecture, API proposal to Group 1, UI wireframe | Done |
| 2 | Toolchain installation & CLI experiments | Java language investigation, OpenJDK installation guide, CLI experiment log | Done |
| 3 | Initial module implementation | Java module v0.3 + tests; shared web UI v0.3 (editor, run, output, theme, view registry) | Done |
| 4 | Module testing, first integration with website | Plug Java module into Group 2's controller; UI calls the controller; demo | Planned |
| 5 | All 10 languages integrated, deploy to VM | UI shows all languages; Docker/approved sandbox enabled on VM | Planned |
| 6 | Lecturer assessment workflow | Host Groups 4/5/6 pages in the shell; shared form/table components | Planned |
| 7 | Cross-group testing, security, refinement | Test report, accessibility pass, error-display work with Group 8 | Planned |
| 8 | Final deployment & demo | Group Technical Report, demo, presentation | Planned |

## 5. Dependencies on other groups

| Group | We need from them | They need from us |
|---|---|---|
| 1 – API spec | Final execution API contract (Week 1) | Our API proposal and the UI's endpoint needs (`/languages`, `/examples`) |
| 2 – Execution controller | Endpoint URL and routing rules for `/api/execute` | `LanguageExecutor` contract / HTTP module for Java |
| 4 – Questions | Exercise data model | Page shell, editor component, form styles |
| 5 – Assessment | Test-case result format | Java module that is deterministic and fast enough for many runs |
| 6 – Submissions | Submission endpoints | Editor + submit button integration |
| 8 – Diagnostics | Error format conventions | `diagnostics.js` parser to extend; error panel |
| 9 – Comparison | Comparison page requirements | Reusable editor instances |
| 10 – Deployment | VM layout, reverse proxy config | Static UI bundle (no build step) + Java module start script |

## 6. Technology decisions

| Decision | Choice | Reason |
|---|---|---|
| Java toolchain | OpenJDK 21 LTS (works with 17+) | LTS release, available via `apt` on Ubuntu 22.04/24.04 |
| Module implementation | Plain Java 17, JDK-only (`com.sun.net.httpserver`) | No Maven or internet needed on the VM, and easy for every member to read |
| Compilation strategy | `javac` → `java` as **child processes** | Separates compile errors from runtime errors; submitted code never runs inside the server's JVM (Brief §9) |
| Front end | Vanilla HTML/CSS/ES modules + CodeMirror 5 | No build step; any group can edit it; runs from any static server; falls back to a textarea when offline |
| Sandbox | Pluggable: `local` (development) / `docker` (VM) | The class-wide sandbox decision is still pending, so the interface lets us switch without code changes |

## 7. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| API contract changes late | Medium | High | Fields isolated in `ExecutionResult` / `api.js`; integration tests catch drift |
| VM access delayed | Medium | High | Everything runs locally on Windows/Linux; Docker mode is ready |
| JVM start-up is slow for many test cases | Medium | Medium | Measured ~0.3 s compile + ~0.1 s run; tune JVM flags; later reuse the compiled class across test cases |
| CDN blocked on the VM | Low | Medium | Textarea fallback is built in; CodeMirror can be vendored into `frontend/vendor/` |
| Uneven member contribution | Medium | Medium | Group contract, weekly logbook, GitHub evidence |

## 8. Communication

* Intragroup: weekly meeting (`[day/time]`) plus a WhatsApp/Discord group for daily updates. Minutes go in the logbook.
* Intergroup: leader attends the weekly Project Integration Committee meeting.
* GitHub: feature branches `g3/<feature>`, pull requests reviewed by one other member, and no direct pushes to `main`.
