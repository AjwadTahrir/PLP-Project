# MPLab – Group 3 (Java · Shared Web Interface & Code Editor)

Group 3's part of **MPLab**, the Multi-Language Programming Lab (Programming Language Paradigms 2026).

* **Language module:** Java (OpenJDK) → [`languages/java/`](languages/java/)
* **Shared-platform responsibility:** web interface and code editor → [`frontend/`](frontend/)

## Run it (≈1 minute)

Requirement: JDK 17+ on your PATH.

```powershell
cd languages\java
.\scripts\test.ps1         # build + 27 tests
.\scripts\run-server.ps1   # open http://127.0.0.1:8080
```

(Linux/macOS: `sh scripts/test.sh` and `sh scripts/run-server.sh`.)

## Deliverables up to Week 3

| Week | Brief requirement | Group 3 deliverable |
|---|---|---|
| **1** | Project plan, architecture and API specification; group contract; leader; Gantt chart | [Project plan](docs/week1/01-project-plan.md) · [Group contract](docs/week1/02-group-contract.md) · [Architecture](docs/week1/03-architecture.md) · [API proposal](docs/week1/04-api-proposal.md) · [Gantt chart](docs/week1/05-gantt-chart.md) |
| **2** | Working toolchain demonstration and installation guide | [Java language investigation (Task A)](docs/week2/01-java-language-investigation.md) · [Installation guide (Task B)](docs/week2/02-installation-guide.md) · [CLI experiments (Task B)](docs/week2/03-cli-experiments.md) · [experiment sources](languages/java/cli-experiments/) |
| **3** | Initial module implementation | [Implementation report + test results](docs/week3/01-initial-implementation.md) · [Java module (Task C)](languages/java/) · [Web interface](frontend/) · [Concept examples (Task D)](languages/java/examples/) |
| 1–3 | Weekly logbook | [Logbook](docs/logbook.md) |

## Repository layout

```
├── docs/
│   ├── week1/   plan, contract, architecture, API proposal, Gantt
│   ├── week2/   investigation, installation guide, CLI experiments
│   ├── week3/   implementation report
│   └── logbook.md
├── languages/java/   Java execution module, tests, examples, scripts
└── frontend/         shared MPLab web interface (HTML/CSS/JS, no build step)
```

In the class-wide repository, `languages/java/` and `frontend/` go in the agreed project structure, and `docs/` goes under `docs/group3/`.

## Still to do before submitting

* Fill in member names, matric numbers, dates and attendance (contract, plan, logbook).
* Replace the placeholder dates in the Gantt chart with the semester's dates.
* Re-run the CLI experiments on the faculty VM (OpenJDK 21) once access is granted, and test `MPLAB_SANDBOX=docker` there.
* Align the API proposal with Group 1's final specification.
* Take screenshots of the UI test cases for the logbook and report.
