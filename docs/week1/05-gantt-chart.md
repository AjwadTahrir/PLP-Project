# Group 3 – Gantt Chart (8 weeks)

Prepared in Week 1 and updated weekly (Brief §5.3). Replace `2026-xx` dates with the real semester dates; the chart below assumes the project starts on a Monday, **Week 1 = day 0**.

> GitHub renders the Mermaid chart below. For the report, export it as an image (mermaid.live → PNG) or rebuild it in Excel using the table in §2.

```mermaid
gantt
    title MPLab – Group 3 (Java + Shared Web Interface)
    dateFormat  YYYY-MM-DD
    axisFormat  W%W
    %% Set START to Monday of Week 1
    section Management
    Group contract, roles, leader          :done,    m1, 2026-01-05, 3d
    Project plan + Gantt                   :done,    m2, 2026-01-05, 5d
    Weekly logbook                         :active,  m3, 2026-01-05, 56d
    Integration committee meetings         :active,  m4, 2026-01-05, 56d

    section Task A – Investigation
    Java language investigation            :done,    a1, 2026-01-07, 10d

    section Task B – Installation
    OpenJDK install (local + VM)           :done,    b1, 2026-01-12, 5d
    CLI experiments + installation guide   :done,    b2, 2026-01-12, 5d

    section Task C – Java module
    API proposal to Group 1                :done,    c0, 2026-01-05, 5d
    Executor, sandbox, HTTP adapter        :done,    c1, 2026-01-19, 5d
    Integrate with controller (Group 2)    :         c2, 2026-01-26, 5d
    Docker/approved sandbox on VM          :         c3, 2026-02-02, 5d

    section Shared web interface
    Architecture + wireframe               :done,    w0, 2026-01-05, 5d
    Shell, router, editor, playground      :done,    w1, 2026-01-12, 12d
    Connect UI to controller               :         w2, 2026-01-26, 5d
    All 10 languages in UI                 :         w3, 2026-02-02, 5d
    Host lecturer/submission pages (4,5,6) :         w4, 2026-02-09, 5d
    Accessibility + polish                 :         w5, 2026-02-16, 5d

    section Task D – Examples
    7 Java concept programs                :done,    d1, 2026-01-14, 8d
    Demonstrate via MPLab                  :         d2, 2026-01-26, 5d

    section Task E – Testing
    Unit + HTTP tests                      :done,    e1, 2026-01-19, 5d
    Cross-group integration tests          :         e2, 2026-02-16, 5d

    section Milestones
    W1 API agreed                          :milestone, ms1, 2026-01-09, 0d
    W3 Initial implementation              :milestone, ms3, 2026-01-23, 0d
    W5 Integrated platform on VM           :milestone, ms5, 2026-02-06, 0d
    W8 Final demo                          :milestone, ms8, 2026-02-27, 0d
```

## 2. Table version

| Task | Owner | W1 | W2 | W3 | W4 | W5 | W6 | W7 | W8 | Status |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|---|
| Group contract, roles, plan, Gantt | Leader | ■ | | | | | | | | Done |
| API proposal / architecture | M2, M4 | ■ | | | | | | | | Done |
| Task A – Java investigation | M6 | ■ | ■ | | | | | | | Done |
| Task B – Install OpenJDK + guide | M3 | | ■ | | | | | | | Done (local); VM pending access |
| Task C – Java module v0.3 | M2 | | | ■ | | | | | | Done |
| Shared UI v0.3 (editor, run, output) | M4, M5 | | ■ | ■ | | | | | | Done |
| Task D – 5+ concept examples | M6 | | ■ | ■ | ■ | | | | | Written; demo W4 |
| Integration with controller (G2) | M2, M4 | | | | ■ | ■ | | | | Planned |
| VM deployment + sandbox | M3 | | | | | ■ | | | | Planned |
| Lecturer/submission pages in shell | M4, M5 | | | | | | ■ | | | Planned |
| Task E – cross-group tests | M3 | | | ■ | ■ | ■ | ■ | ■ | | Unit/HTTP tests done |
| Error display with G8, a11y polish | M5 | | | | | | | ■ | | Planned |
| Technical report + presentation | All | | | | | | | ■ | ■ | Planned |
| Logbook | Leader | ■ | ■ | ■ | ■ | ■ | ■ | ■ | ■ | Ongoing |

Legend: M1–M6 = members as listed in the project plan.
