// Catalogue of the ten MPLab languages: editor mode, file extension and a starter template.
// The server's GET /api/languages tells the UI which ones are currently available.

export const LANGUAGES = [
  {
    id: "c", name: "C", toolchain: "GCC", group: 1, ext: "c", mode: "text/x-csrc",
    template: `#include <stdio.h>

int main(void) {
    printf("Hello from C!\\n");
    return 0;
}
`,
  },
  {
    id: "cpp", name: "C++", toolchain: "G++", group: 2, ext: "cpp", mode: "text/x-c++src",
    template: `#include <iostream>

int main() {
    std::cout << "Hello from C++!" << std::endl;
    return 0;
}
`,
  },
  {
    id: "java", name: "Java", toolchain: "OpenJDK", group: 3, ext: "java", mode: "text/x-java",
    template: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String name = in.hasNextLine() ? in.nextLine() : "World";
        System.out.println("Hello, " + name + "! Welcome to MPLab.");
    }
}
`,
  },
  {
    id: "python", name: "Python", toolchain: "CPython", group: 4, ext: "py", mode: "python",
    template: `import sys

name = sys.stdin.readline().strip() or "World"
print(f"Hello, {name}! Welcome to MPLab.")
`,
  },
  {
    id: "javascript", name: "JavaScript", toolchain: "Node.js", group: 5, ext: "js", mode: "javascript",
    template: `console.log("Hello from JavaScript!");
`,
  },
  {
    id: "go", name: "Go", toolchain: "Go toolchain", group: 6, ext: "go", mode: "text/x-go",
    template: `package main

import "fmt"

func main() {
\tfmt.Println("Hello from Go!")
}
`,
  },
  {
    id: "ruby", name: "Ruby", toolchain: "Ruby", group: 7, ext: "rb", mode: "ruby",
    template: `puts "Hello from Ruby!"
`,
  },
  {
    id: "php", name: "PHP", toolchain: "PHP CLI", group: 8, ext: "php", mode: "application/x-httpd-php",
    template: `<?php
echo "Hello from PHP!\\n";
`,
  },
  {
    id: "haskell", name: "Haskell", toolchain: "GHC", group: 9, ext: "hs", mode: "haskell",
    template: `main :: IO ()
main = putStrLn "Hello from Haskell!"
`,
  },
  {
    id: "prolog", name: "Prolog", toolchain: "SWI-Prolog", group: 10, ext: "pl", mode: "prolog",
    template: `:- initialization(main).

main :-
    write('Hello from Prolog!'), nl,
    halt.
`,
  },
];

export function languageById(id) {
  return LANGUAGES.find((l) => l.id === id) || LANGUAGES[2];
}

/** File name shown in the editor tab / used for download. Java needs the public class name. */
export function fileNameFor(lang, source) {
  if (lang.id === "java") {
    // Same rule as the server: the public type names the file, else the first declared class.
    const code = (source || "").replace(/\/\*[\s\S]*?\*\/|\/\/.*$/gm, "");
    const m = /\bpublic\s+(?:final\s+|abstract\s+)*(?:class|interface|enum|record)\s+(\w+)/.exec(code)
      || /\b(?:class|record|enum)\s+(\w+)/.exec(code);
    return (m ? m[1] : "Main") + ".java";
  }
  return "main." + lang.ext;
}
