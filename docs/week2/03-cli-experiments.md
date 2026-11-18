# Task B – Command-Line Experiments (Working Toolchain Demonstration)

**Group 3 · MPLab · Week 2 deliverable**

These outputs were captured from a real terminal session. The source files are in [`languages/java/cli-experiments/`](../../languages/java/cli-experiments/). Re-run them on the faculty VM after installation and paste the VM output (with OpenJDK 21 version strings) into the final report.

> Machine used: Windows 11 with a Bash shell, JDK 27 (the VM will run OpenJDK 21; the javac/JVM behaviour shown is the same).

## 1. Version verification

```text
$ javac -version
javac 27
[exit code: 0]

$ java -version
java version "27" 2026-09-15
Java(TM) SE Runtime Environment (build 27+35-2325)
Java HotSpot(TM) 64-Bit Server VM (build 27+35-2325, mixed mode, sharing)
[exit code: 0]
```

## 2. Simple program: compile and run

`Hello.java`

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, MPLab!");
    }
}
```

```text
$ javac Hello.java
[exit code: 0]

$ ls *.class
Hello.class

$ java Hello
Hello, MPLab!
[exit code: 0]
```

**Observation:** compilation is a separate step that produces `Hello.class` (bytecode). `java` takes a **class name**, not a file name.

## 3. Looking inside the bytecode

```text
$ javap -c Hello
Compiled from "Hello.java"
public class Hello {
  public Hello();
    Code:
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return

  public static void main(java.lang.String[]);
    Code:
         0: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
         3: ldc           #13                 // String Hello, MPLab!
         5: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
         8: return
}
```

**Observations:**
* `javac` added a default constructor that calls `Object.<init>`, because every class extends `Object`.
* The JVM is a **stack machine**: `getstatic` pushes `System.out`, `ldc` pushes the string constant, and `invokevirtual` pops both and calls `println`. `invokevirtual` means the method is chosen at run time (dynamic binding).

## 4. Standard input

```text
$ javac Sum.java
$ echo "7 35" | java Sum
7 + 35 = 42
[exit code: 0]
```

## 5. Syntax error (compile time)

`SyntaxError.java` has a missing semicolon on line 3.

```text
$ javac SyntaxError.java
SyntaxError.java:3: error: ';' expected
        int x = 10
                  ^
1 error
[exit code: 1]
```

**Observation:** no `.class` file is produced and nothing runs. The diagnostic format `File.java:LINE: error: message` is what MPLab's error parser uses to highlight the line in the editor.

## 6. Type error (static type checking)

```text
$ javac TypeError.java
TypeError.java:3: error: incompatible types: String cannot be converted to int
        int count = "five";
                    ^
TypeError.java:4: error: incompatible types: int cannot be converted to String
        String s = 42;
                   ^
2 errors
[exit code: 1]
```

**Observation:** `javac` reports **all** type errors in one pass. This is evidence of Java's static, strong typing; in Python the same code would only fail when executed.

## 7. Runtime error (uncaught exception)

```text
$ javac RuntimeError.java
[exit code: 0]

$ java RuntimeError
Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3
	at RuntimeError.main(RuntimeError.java:4)
[exit code: 1]
```

**Observation:** compilation succeeds, because array bounds cannot be checked statically, but the JVM checks every access at run time. The stack trace goes to **stderr** and the exit code is **1**.

## 8. Explicit exit code

```text
$ java ExitCode
Invalid input, exiting
[exit code: 3]
```

`System.exit(3)` sets the process exit code. MPLab reports any non-zero exit as `runtime_error` with `exit_code: 3`.

## 9. Single-file source launcher (JEP 330)

```text
$ java Hello.java
Hello, MPLab!
[exit code: 0]
```

Compiles in memory and runs without creating a `.class` file. **MPLab does not use this mode**, because it would mix compile errors and runtime errors into one step.

## 10. Infinite loop / time limit

```text
$ javac Loop.java
$ timeout 3 java Loop
[exit code: 124]
```

The program never finishes, so an external time limit is essential. `timeout` returns 124. The MPLab module kills the process tree itself and returns `status: "timeout"`.

## 11. Summary of error handling

| Scenario | Stage | Exit code | Output stream | MPLab status |
|---|---|---|---|---|
| Hello world | run | 0 | stdout | `success` |
| Missing `;` | compile | 1 (javac) | stderr | `compile_error` |
| Type mismatch | compile | 1 (javac) | stderr | `compile_error` |
| Array index out of bounds | run | 1 | stderr (stack trace) | `runtime_error` |
| `System.exit(3)` | run | 3 | stderr | `runtime_error` |
| Infinite loop | run | killed | – | `timeout` |
| Heap exhausted | run | 1 | `java.lang.OutOfMemoryError` | `memory_limit_exceeded` |
