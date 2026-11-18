# Group 3 – Project Logbook (Weeks 1–3)

**Group 3** · Java (OpenJDK) · Shared web interface and code editor
Updated weekly by the group leader with contributions from all members (Brief §5.2). Weeks 4–8 use the same template.

> `[Member N]`, dates and attendance are placeholders. Fill them in from your real meetings, and link GitHub commits/PRs as evidence in the *Individual contributions* tables.

---

## Week 1 – Requirements, architecture and API agreement

**Dates:** ____ to ____

### Weekly activities

| Planned | Completed | Notes |
|---|---|---|
| Form group, choose leader, sign contract | ✔ | `docs/week1/02-group-contract.md` |
| Record language/group/members in the Student Information sheet | ✔ / ☐ | |
| Project plan and Gantt chart | ✔ | `01-project-plan.md`, `05-gantt-chart.md` |
| Architecture for Java module + web UI | ✔ | `03-architecture.md` |
| API proposal for Group 1 | ✔ | `04-api-proposal.md` |
| Repository structure agreed (`languages/java`, `frontend/`) | ✔ | |

### Intragroup meeting

| Date / time | Venue | Attendance | Absent (reason) |
|---|---|---|---|
| | | [Member 1–6] | |

**Key decisions**
1. Leader: [Member 1]. Integration committee representative: [Member 1].
2. Java module written in plain Java 17 with no external libraries, so it builds on the VM without internet.
3. Front end: vanilla JS + CodeMirror, no build step, so every group can contribute.
4. Separate `javac` and `java` steps to distinguish compile and runtime errors.

**Assigned actions:** see the project plan's roles table.

### Intergroup collaboration

| Date | With | Topic | Outcome |
|---|---|---|---|
| | Integration Committee | API contract, status values | Group 3 submitted its proposal; final version owned by Group 1 |
| | Group 2 | How the controller calls modules | Java supports both HTTP and in-process; decision pending |
| | Group 10 | VM access, reverse proxy | Waiting for FSKTM approval |

### Individual contributions

| Member | Work done | Evidence |
|---|---|---|
| [Member 1] | Contract, plan, Gantt, committee meeting | commit/PR link |
| [Member 2] | API proposal, module architecture | |
| [Member 3] | Toolchain research | |
| [Member 4] | UI architecture, wireframe | |
| [Member 5] | UI requirements from the brief §5 | |
| [Member 6] | Started Java language investigation | |

### Issues and solutions

| Issue | Action |
|---|---|
| API not yet final | Proposal written so implementation can start, and fields isolated in one class |
| VM access not yet granted | Develop and test locally; scripts work on Windows and Linux |

### Next week's plan

| Task | Responsible | Deliverable |
|---|---|---|
| Java investigation | [Member 6] | Task A summary |
| Install JDK, CLI experiments, guide | [Member 3] | Installation guide + experiment log |
| UI shell and editor prototype | [Member 4], [Member 5] | Running page |

---

## Week 2 – Toolchain installation and command-line experiments

**Dates:** ____ to ____

### Weekly activities

| Planned | Completed | Notes |
|---|---|---|
| Task A – Java language investigation | ✔ | `docs/week2/01-java-language-investigation.md` |
| Install JDK on laptops | ✔ | Temurin 21 / other JDK ≥ 17 |
| Install JDK on faculty VM | ☐ | Waiting for access; commands ready in the guide |
| CLI experiments: success, syntax error, type error, runtime error, exit code, timeout, `javap` | ✔ | `docs/week2/03-cli-experiments.md` |
| Installation guide | ✔ | `docs/week2/02-installation-guide.md` |
| UI shell, router, editor prototype | ✔ | |

### Intragroup meeting

| Date / time | Venue | Attendance | Absent (reason) |
|---|---|---|---|
| | | | |

**Key decisions**
1. Target OpenJDK 21 LTS on the VM, and compile the module with `--release 17`.
2. Docker image `eclipse-temurin:21-jdk` proposed for the sandbox.
3. Use CodeMirror 5 (no bundler needed) with a textarea fallback.

### Intergroup collaboration

| Date | With | Topic | Outcome |
|---|---|---|---|
| | Group 1 | Status values / HTTP codes | |
| | Group 8 | Error display format | Agreed to share `diagnostics.js` |
| | All groups | Language ids and starter templates for the editor | |

### Individual contributions

| Member | Work done | Evidence |
|---|---|---|
| [Member 1] | Logbook, committee | |
| [Member 2] | Prototype executor | |
| [Member 3] | Installation, experiments, guide | |
| [Member 4] | Shell, router, editor component | |
| [Member 5] | Styles, dark mode | |
| [Member 6] | Investigation report | |

### Issues and solutions

| Issue | Action |
|---|---|
| PowerShell adds a BOM when piping stdin, which broke `Scanner` | Documented; test stdin with Bash or MPLab instead |
| Lab machines have different JDK versions | Module compiled for Java 17 bytecode |

### Next week's plan

| Task | Responsible | Deliverable |
|---|---|---|
| Java execution module + tests | [Member 2], [Member 3] | v0.3 |
| Playground page | [Member 4], [Member 5] | v0.3 |
| Concept examples | [Member 6] | ≥ 5 programs |

---

## Week 3 – Initial module implementation

**Dates:** ____ to ____

### Weekly activities

| Planned | Completed | Notes |
|---|---|---|
| `JavaExecutor`, `SourceAnalyzer`, `ProcessRunner`, `Sandbox` | ✔ | |
| HTTP API (`/execute`, `/languages`, `/examples/java`, `/health`) | ✔ | |
| Test suite | ✔ | 27/27 passing |
| Playground UI (editor, stdin, run, output/errors/details) | ✔ | |
| 5+ concept examples | ✔ | 7 programs |
| Docker sandbox test on VM | ☐ | Waiting for VM |

### Intragroup meeting

| Date / time | Venue | Attendance | Absent (reason) |
|---|---|---|---|
| | | | |

**Key decisions**
1. Results with compile/runtime errors return HTTP 200; request problems return 400.
2. Output normalised to `\n` line endings.
3. Other groups add pages through `registerView()`.

### Intergroup collaboration

| Date | With | Topic | Outcome |
|---|---|---|---|
| | Group 2 | Plugging the Java module into the controller | Planned for Week 4 |
| | Groups 4/5/6/9 | How to add pages to the shared UI | Shared `frontend/README.md` |
| | Group 5 | Batch test-case execution | Proposal raised |

### Individual contributions

| Member | Work done | Evidence |
|---|---|---|
| [Member 1] | Reviews, logbook, integration | |
| [Member 2] | Executor, API server | |
| [Member 3] | Tests, CLI experiments in repo | |
| [Member 4] | Editor, router, API client | |
| [Member 5] | Playground UI, responsive fixes | |
| [Member 6] | Examples + README | |

### Issues and solutions

| Issue | Action |
|---|---|
| Error-line highlight hidden by active-line style | CSS specificity fix |
| Footer overlapped output on phones | Flexible height below 980 px |
| Timeout took 3 s longer than the limit | Grace period only for container sandboxes |
| `<=` displayed as `≤` by font ligatures | Ligatures disabled in the editor |

### Next week's plan (Week 4)

| Task | Responsible | Deliverable |
|---|---|---|
| Integrate with execution controller | [Member 2], [Member 4] | Java runs through the shared backend |
| VM deployment test with Docker sandbox | [Member 3] | VM output in the installation guide |
| Language module demonstration | All | Live demo |
| Help other groups add their pages | [Member 4], [Member 5] | First external view merged |
