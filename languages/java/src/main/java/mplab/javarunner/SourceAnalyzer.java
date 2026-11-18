package mplab.javarunner;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight static analysis of submitted Java source, used to decide:
 * <ul>
 *   <li>the file name — Java requires a {@code public} top-level class to live in {@code Name.java};</li>
 *   <li>the class to launch — the class that declares {@code main}.</li>
 * </ul>
 * Comments, string and char literals are blanked out first so text like {@code "class Foo"} inside a
 * string is not mistaken for a declaration.
 */
public final class SourceAnalyzer {

    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "\\bpublic\\s+(?:(?:final|abstract|static|sealed|non-sealed|strictfp)\\s+)*(?:class|interface|enum|record)\\s+(\\w+)");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z_$][\\w$]*|[{}()]");

    private SourceAnalyzer() {
    }

    /** Result of analysis. {@code mainClass} is the binary name passed to {@code java}. */
    public static final class Analysis {
        public final String packageName;
        public final String fileBaseName;
        public final String mainClass;

        Analysis(String packageName, String fileBaseName, String mainClass) {
            this.packageName = packageName;
            this.fileBaseName = fileBaseName;
            this.mainClass = mainClass;
        }
    }

    public static Analysis analyze(String source) {
        String code = stripCommentsAndLiterals(source);

        Matcher pm = PACKAGE.matcher(code);
        String pkg = pm.find() ? pm.group(1) : null;

        Matcher pub = PUBLIC_TYPE.matcher(code);
        String publicType = pub.find() ? pub.group(1) : null;

        String mainOwner = findMainOwner(code);
        String fileBase = publicType != null ? publicType : (mainOwner != null ? topLevelOf(mainOwner) : "Main");
        String simpleMain = mainOwner != null ? mainOwner : fileBase;
        String binary = pkg != null ? pkg + "." + simpleMain : simpleMain;
        return new Analysis(pkg, fileBase, binary);
    }

    /**
     * Walks tokens, tracking which type declaration each brace belongs to, and returns the binary name
     * (Outer$Inner) of the first type that declares a method called {@code main}.
     */
    static String findMainOwner(String code) {
        Matcher m = TOKEN.matcher(code);
        Deque<String> typeStack = new ArrayDeque<>();   // binary names of open types
        Deque<Boolean> braceIsType = new ArrayDeque<>(); // for each open '{': does it open a type body?
        String pendingType = null;
        String prev = null;
        String prev2 = null;
        while (m.find()) {
            String t = m.group();
            switch (t) {
                case "{":
                    if (pendingType != null) {
                        String binary = typeStack.isEmpty() ? pendingType : typeStack.peek() + "$" + pendingType;
                        typeStack.push(binary);
                        braceIsType.push(Boolean.TRUE);
                        pendingType = null;
                    } else {
                        braceIsType.push(Boolean.FALSE);
                    }
                    break;
                case "}":
                    if (!braceIsType.isEmpty() && braceIsType.pop()) {
                        typeStack.pop();
                    }
                    break;
                case "(":
                    // "void main (" -> method declaration named main
                    if ("main".equals(prev) && "void".equals(prev2) && !typeStack.isEmpty()) {
                        return typeStack.peek();
                    }
                    break;
                default:
                    if (("class".equals(prev) || "interface".equals(prev) || "enum".equals(prev)
                            || "record".equals(prev)) && !t.equals("{")) {
                        pendingType = t;
                    }
            }
            prev2 = prev;
            prev = t;
        }
        return null;
    }

    private static String topLevelOf(String binaryName) {
        int idx = binaryName.indexOf('$');
        return idx < 0 ? binaryName : binaryName.substring(0, idx);
    }

    /** Replaces comments and string/char/text-block literals with spaces, preserving newlines. */
    static String stripCommentsAndLiterals(String src) {
        StringBuilder out = new StringBuilder(src.length());
        int n = src.length();
        int i = 0;
        while (i < n) {
            char c = src.charAt(i);
            char next = i + 1 < n ? src.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < n && src.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
            } else if (c == '/' && next == '*') {
                out.append("  ");
                i += 2;
                while (i < n && !(src.charAt(i) == '*' && i + 1 < n && src.charAt(i + 1) == '/')) {
                    out.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append("  ");
                    i += 2;
                }
            } else if (c == '"' && src.startsWith("\"\"\"", i)) {
                out.append("   ");
                i += 3;
                while (i < n && !src.startsWith("\"\"\"", i)) {
                    if (src.charAt(i) == '\\' && i + 1 < n) {
                        out.append("  ");
                        i += 2;
                        continue;
                    }
                    out.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append("   ");
                    i += 3;
                }
            } else if (c == '"' || c == '\'') {
                char quote = c;
                out.append(' ');
                i++;
                while (i < n && src.charAt(i) != quote && src.charAt(i) != '\n') {
                    if (src.charAt(i) == '\\' && i + 1 < n) {
                        out.append(' ');
                        i++;
                    }
                    out.append(' ');
                    i++;
                }
                if (i < n && src.charAt(i) == quote) {
                    out.append(' ');
                    i++;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
