# Week 3 – Initial Module Implementation

**Group 3 · MPLab · Week 3 deliverable** · Java module **v0.3** and shared web interface **v0.3**

## 1. What was delivered

| Component | Location | State |
|---|---|---|
| Java execution module (Task C) | `languages/java/src/main/java/mplab/javarunner/` | Working: compile, run, stdin, limits, all statuses from the API proposal |
| HTTP adapter (module service) | `ApiServer.java` | `/api/execute`, `/api/languages`, `/api/examples/java`, `/api/health`, static UI |
| Sandbox abstraction | `Sandbox.java` | `local` (tested) and `docker` (implemented, to be tested on the VM) |
| Test suite (Task E, part 1) | `languages/java/src/test/java/.../ModuleTests.java` | 27 unit + HTTP tests, **all passing** |
| Concept examples (Task D) | `languages/java/examples/` | 7 programs, loadable from the editor's *Examples* menu |
| Shared web interface | `frontend/` | App shell, navigation, view registry, code editor, stdin, output/errors/details, dark mode, responsive layout |
| Build/run scripts | `languages/java/scripts/` | `build`, `test`, `run-server` for PowerShell and sh |

## 2. Java module

### 2.1 Request flow

1. `ApiServer` reads at most 256 KB, parses the JSON and builds an `ExecutionRequest`.
2. `JavaExecutor.validate` checks the language, that the source is not empty, and the size limits (64 KB).
3. `SourceAnalyzer` blanks out comments and string literals, then finds:
   * the **public type**, which decides the file name (`public class Calculator` → `Calculator.java`);
   * the **class that declares `main`**, including nested classes (`Outer$Inner`) and `package` prefixes.
4. The source is written into a fresh temp directory `mplab-java-*`.
5. **Compile:** `javac -encoding UTF-8 -d . Name.java`, with the working directory set to the temp dir so diagnostics show `Name.java:3:` and never the server's paths.
6. **Run:** `java -Xmx256m -Xss8m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8 … -cp . MainClass`, with stdin piped in, a wall-clock timeout, and stdout/stderr capped at 64 KB each.
7. **Classify:** timeout → `timeout`; `OutOfMemoryError` or container OOM → `memory_limit_exceeded`; exit 0 → `success`; otherwise `runtime_error`, with a summary such as *"Uncaught java.lang.ArithmeticException: / by zero"*.
8. The temp directory is always deleted (`finally`).

### 2.2 Safety measures already in place

| Measure | Where |
|---|---|
| Code runs in a **separate process**, never in the server JVM | `ProcessRunner` |
| Time limit with **process-tree kill** (`descendants().destroyForcibly()`) | `ProcessRunner` |
| Heap and stack limits | `-Xmx`, `-Xss` |
| Output cap; pipes are drained to avoid deadlock | `ProcessRunner.Collector` |
| Request/body/source/stdin size limits | `ApiServer`, `JavaExecutor` |
| Concurrency limit (semaphore, default 4) | `JavaExecutor` |
| Environment cleaned (`JAVA_TOOL_OPTIONS`, `CLASSPATH`…) | `ProcessRunner` |
| Path-traversal guard for static files | `ApiServer.handleStatic` |
| Server binds to `127.0.0.1` by default | `ApiServer.main` |
| Docker mode: no network, read-only FS, `nobody`, capped memory/CPU/PIDs | `Sandbox.Docker` |

The `local` sandbox is **for development only**. On the VM the module must run with `MPLAB_SANDBOX=docker` or the class-approved sandbox (Brief §9).

### 2.3 Test results

Command: `languages/java/scripts/test.ps1` (Windows) or `test.sh` (Linux)

```text
Toolchain: javac 27   sandbox: local

  PASS  json round trip                                             2 ms
  PASS  json rejects malformed body                                 1 ms
  PASS  analyzer: default class name Main                           2 ms
  PASS  analyzer: public class decides file name                    0 ms
  PASS  analyzer: package + main in second class                    1 ms
  PASS  success: hello world                                      346 ms
  PASS  success: reads stdin                                      350 ms
  PASS  success: unicode output                                   309 ms
  PASS  success: custom public class name                         336 ms
  PASS  success: package declaration                              312 ms
  PASS  success: stderr output with exit 0 is still success       333 ms
  PASS  compile error: missing semicolon                          250 ms
  PASS  compile error: type mismatch (static type checking)       268 ms
  PASS  runtime error: uncaught exception                         329 ms
  PASS  runtime error: System.exit(3)                             348 ms
  PASS  runtime error: no main method                             337 ms
  PASS  timeout: infinite loop is killed                         3290 ms
  PASS  memory: huge allocation                                   345 ms
  PASS  output limit: output is truncated                         463 ms
  PASS  invalid request: empty source                               0 ms
  PASS  unsupported language passed to java module                  0 ms
  PASS  http: GET /api/health                                     165 ms
  PASS  http: GET /api/languages lists 10 languages                 3 ms
  PASS  http: POST /api/execute java (brief example)              333 ms
  PASS  http: unsupported language -> 400 unsupported_language      2 ms
  PASS  http: malformed JSON -> 400 invalid_request                 1 ms
  PASS  http: GET /api/execute -> 405                               1 ms

27 passed, 0 failed
```

The tests cover the three scenarios required by Task E: successful execution, invalid code and runtime errors. They also cover timeout, memory, output limits and HTTP error handling. The time limit in the test configuration is 3 s.

**Performance:** a typical small program takes about **0.25–0.35 s** end to end (compile ≈ 0.25 s, run ≈ 0.06 s).

## 3. Shared web interface

### 3.1 Features

* **Top bar:** MPLab brand, navigation (Playground · Exercises · Lecturer · Compare), live API status dot (polls `/api/health`), and a light/dark theme toggle that is remembered.
* **Language menu** with all ten languages. Languages whose module is not deployed yet are labelled *"(coming soon)"* from `/api/languages`.
* **Code editor** (CodeMirror 5): syntax highlighting for C, C++, Java, Python, JavaScript, Go, Ruby, PHP and Haskell, a custom Prolog mode, line numbers, bracket matching, auto-close, active line, 4-space indent, and font-size control. **Textarea fallback** if the CDN is unreachable.
* **Starter template** per language. Code and stdin are **auto-saved per language** in the browser.
* **Examples menu** that loads the Java concept programs from `/api/examples/java`, with a concept banner.
* **Run** button and **Ctrl+Enter**, with a spinner and live elapsed time.
* **Result bar:** coloured status badge, summary message, exit code, run/compile/total time.
* **Output / Errors / Details tabs.** Errors are parsed into clickable *"Line N"* items that jump to the line, and the line is highlighted in red in the editor. *Details* shows the raw API JSON, which is useful for other groups when debugging.
* **Download** the source with the correct file name (`Calculator.java`).
* **Responsive** down to phone width, keyboard accessible, `aria-live` result announcements, skip link.
* **Placeholder pages** reserving space for Groups 4, 5, 6 and 9, with a `window.MPLab.registerView()` hook so they can add pages without editing our files.

### 3.2 Manual UI test log (Week 3)

| # | Test | Expected | Result |
|---|---|---|---|
| 1 | Open `http://127.0.0.1:8080` | Playground loads, API dot green, Java template shown | Pass |
| 2 | stdin `Group 3`, Run | `Hello, Group 3! Welcome to MPLab.`, badge *Success* | Pass |
| 3 | Delete `{` on line 4, Ctrl+Enter | Badge *Compilation error*, Errors tab, clickable *Line 4* item | Pass |
| 4 | Division by zero on line 5 | Badge *Runtime error*, line 5 highlighted, message names `ArithmeticException` | Pass |
| 5 | Examples → *Recursion*, Run | Concept banner, correct output, file tab `Recursion.java` | Pass |
| 6 | Select Python, Run | Badge *Language not available*, message names Group 4 | Pass |
| 7 | Phone width (375 px), light theme | Columns stack, no overlap, nav scrolls | Pass (after fixing footer overlap) |
| 8 | Theme toggle, reload | Theme remembered | Pass |
| 9 | Browser console | No JavaScript errors (only the expected HTTP 400 from test 6) | Pass |

> Take screenshots of tests 2–7 for the logbook and the final report (Brief §13.2 section 4).

### 3.3 Issues found and fixed this week

| Issue | Fix |
|---|---|
| Error-line highlight hidden behind CodeMirror's active-line background | Stronger CSS selector plus a red left border marker |
| Footer overlapped the output panel on narrow screens | Page height is flexible below 980 px |
| Font ligatures displayed `<=` as `≤` (confusing for students) | `font-variant-ligatures: none` in the editor |
| File tab showed `Main.java` for programs whose class is not public | UI now uses the same naming rule as the server |
| Timeout took limit + 3 s in local mode | Host grace period only applies to sandboxes with their own timer |
| Field named `java` shadowed the `java.*` package in `ApiServer` | Used imports instead of fully qualified names |

## 4. How to run (for the lecturer and other groups)

```powershell
cd languages\java
.\scripts\test.ps1          # build + 27 tests
.\scripts\run-server.ps1    # http://127.0.0.1:8080
```

```bash
cd languages/java
sh scripts/test.sh
sh scripts/run-server.sh
```

Requirements: JDK 17+ on PATH. No other dependencies.

## 5. Next steps (Week 4)

1. Agree with Group 2 whether the Java module is called over HTTP (port 8083) or as a library, then integrate.
2. Point the UI's `apiBase` at the controller and test all languages that are ready.
3. Test `MPLAB_SANDBOX=docker` on the VM once access is granted.
4. Demonstrate the 7 concept examples through MPLab (Task D).
5. Share `frontend/README.md` with Groups 4, 5, 6 and 9 so they can start building their pages.
6. Propose the batch test-case API to Groups 1 and 5 (compile once, run many inputs).
