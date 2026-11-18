# Group 3 – Execution API Proposal (input to Group 1)

**Week 1 deliverable.** The brief (§6) gives a minimal example and requires the class to agree, in Week 1, how **compilation errors, runtime errors, timeouts and unsupported languages** are represented. This is Group 3's proposal, written from the point of view of a language-module owner and of the team building the UI that displays the results. The Java module and the web interface already implement it.

## 1. Execute

`POST /api/execute` · `Content-Type: application/json`

### Request

```json
{
  "language": "java",
  "source_code": "public class Main { public static void main(String[] a) { System.out.println(10 + 20); } }",
  "stdin": ""
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `language` | string | yes | One of `c`, `cpp`, `java`, `python`, `javascript`, `go`, `ruby`, `php`, `haskell`, `prolog` (lowercase) |
| `source_code` | string | yes | UTF-8, max 64 KB |
| `stdin` | string | no | Default `""`, max 64 KB |

### Response (always the same shape)

```json
{
  "status": "success",
  "stdout": "30\n",
  "stderr": "",
  "exit_code": 0,
  "language": "java",
  "version": "javac 21.0.4",
  "stage": "run",
  "message": "Program finished successfully",
  "compile_time_ms": 412,
  "execution_time_ms": 96,
  "truncated": false
}
```

| Field | Type | Required by brief? | Meaning |
|---|---|---|---|
| `status` | string | **yes** | See §2 |
| `stdout` | string | **yes** | Program standard output (line endings normalised to `\n`) |
| `stderr` | string | **yes** | Program standard error **or** compiler diagnostics when `status = compile_error` |
| `exit_code` | int \| null | **yes** | Process exit code; compiler exit code for `compile_error`; `null` if the program was killed (timeout) or never started |
| `language` | string | proposed | Echo of the language that handled the request |
| `version` | string | proposed | Toolchain version, for display and reports |
| `stage` | `"compile"` \| `"run"` \| null | proposed | Where it stopped. Interpreted languages use `"run"` |
| `message` | string | proposed | Short human-readable summary shown next to the status badge |
| `compile_time_ms` / `execution_time_ms` | int | proposed | Timing for the UI and for language comparison (Group 9) |
| `truncated` | bool | proposed | `true` when stdout/stderr was cut at the 64 KB limit |

Unknown extra fields must be ignored by clients, so groups can add fields without breaking others.

## 2. Status values

| `status` | When | HTTP | `exit_code` | Where the details are |
|---|---|---|---|---|
| `success` | Program ran and exited with 0 | 200 | `0` | `stdout` (and possibly warnings in `stderr`) |
| `compile_error` | Compiler/syntax check failed (javac, gcc, g++, go build, ghc; `python -m py_compile` etc. optional) | 200 | compiler's code | `stderr` = diagnostics |
| `runtime_error` | Program exited with non-zero code or uncaught exception | 200 | program's code | `stderr` |
| `timeout` | Wall-clock limit exceeded (compile or run) | 200 | `null` | `message`; partial `stdout` kept |
| `memory_limit_exceeded` | Memory limit hit (e.g. Java `OutOfMemoryError`, container OOM kill) | 200 | code or `null` | `stderr` |
| `unsupported_language` | `language` missing, unknown, or module not installed | 400 | `null` | `message` |
| `invalid_request` | Malformed JSON, missing `source_code`, size limits exceeded | 400 (413 for body too large) | `null` | `message` |
| `internal_error` | Sandbox/toolchain failure that is not the user's fault | 500 | `null` | `message` |

**Why 200 for compile/runtime errors?** The request itself was processed correctly; the *program* failed. That keeps HTTP errors for problems with the request or the server, and the assessment engine (Group 5) can treat every 200 as "a verdict exists".

## 3. Limits (defaults, configurable per module)

| Limit | Default | Java module setting |
|---|---|---|
| Run wall-clock time | 5 s | `MPLAB_JAVA_TIME_LIMIT_MS` |
| Compile time | 20 s | `MPLAB_JAVA_COMPILE_LIMIT_MS` |
| Memory | 256 MB (heap for JVM) | `MPLAB_JAVA_MEMORY_MB` |
| Output per stream | 64 KB | `MPLAB_MAX_OUTPUT_BYTES` |
| Source / stdin size | 64 KB each | fixed |
| Network | none | Docker `--network none` |

## 4. Endpoints needed by the shared web interface

| Endpoint | Purpose | Response |
|---|---|---|
| `GET /api/languages` | Populate the language menu and show which modules are live | `{"languages":[{"id":"java","name":"Java","toolchain":"OpenJDK","group":3,"available":true,"version":"javac 21.0.4"}, …]}` |
| `GET /api/health` | Status dot in the top bar | `{"status":"ok", …}` |
| `GET /api/examples/{language}` *(optional)* | "Examples" menu for Task D demonstrations | `{"language":"java","examples":[{"id","title","concept","source_code"}]}` |

## 5. Worked examples (real output from the Java module)

**Compilation error**

```json
{"status":"compile_error","stdout":"","stderr":"Main.java:3: error: ';' expected\n    int x = 5\n             ^\n1 error\n",
 "exit_code":1,"stage":"compile","message":"1 compilation error; first at line 3: ';' expected"}
```

**Runtime error**

```json
{"status":"runtime_error","stdout":"","stderr":"Exception in thread \"main\" java.lang.ArithmeticException: / by zero\n\tat Main.main(Main.java:4)\n",
 "exit_code":1,"stage":"run","message":"Uncaught java.lang.ArithmeticException: / by zero"}
```

**Timeout**

```json
{"status":"timeout","stdout":"","stderr":"","exit_code":null,"stage":"run",
 "message":"Program exceeded the time limit of 5000 ms"}
```

**Unsupported language** (HTTP 400)

```json
{"status":"unsupported_language","stdout":"","stderr":"","exit_code":null,"language":"cobol",
 "message":"Unknown language 'cobol'. Supported ids: c, cpp, java, python, javascript, go, ruby, php, haskell, prolog"}
```

## 6. Open questions for the Integration Committee

1. Should modules be separate HTTP services (one port per language) or libraries loaded by the controller? (Java supports both.)
2. Do test cases (Group 5) send many `stdin` values in one request (`"test_cases": [...]`) to avoid recompiling? This would cut Java run time a lot. We propose an optional batch field for Week 6.
3. Line endings: we propose that all modules normalise `\r\n` to `\n` in `stdout`/`stderr`.
4. Which sandbox will Faculty IT approve (Docker, nsjail, isolate)? The `Sandbox` interface lets us adapt.
