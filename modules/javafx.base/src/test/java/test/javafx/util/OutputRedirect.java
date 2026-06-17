/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.javafx.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/// This facility is used in the tests to redirect stderr output to an in-memory buffer
/// for two reasons:
/// 1. to suppress unrelated output in the logs
/// 2. to check for the presence of expected exceptions and patterns
///
public class OutputRedirect {
    private static PrintStream stderr;
    private static AccumulatingPrintStream stderrCapture;

    /// Redirects the stderr to an internal buffer, for the purpose of avoiding polluting the test logs.
    /// This method is typically placed inside of the `@BeforeEach` block.
    ///
    /// Once the test is finished, the output can be checked for thrown exceptions using {@link #checkStderr(Class...)}.
    /// or {@link #checkAndRestoreStderr(Class...)}.
    ///
    /// The redirection needs to be undone by calling either {@link #restoreStderr()} or
    /// {@link #checkAndRestoreStderr(Class...)} method.
    ///
    public static void suppressStderr() {
        if (stderrCapture == null) {
            stderr = System.err;
            stderrCapture = AccumulatingPrintStream.create();
            System.setErr(stderrCapture);
        }
    }

    /// Restores stderr redirection (typically done inside of a `@AfterEach` block).
    /// It is safe to call this method multiple times.
    public static void restoreStderr() {
        if (stderr != null) {
            System.setErr(stderr);
            stderr = null;
            stderrCapture = null;
        }
    }

    /// Checks the accumulated stderr buffer for the expected exceptions and string patterns.
    ///
    /// This method expects the arguments to contain either instances of `Class<? extends Throwable>`,
    /// or `String` patterns.  For exceptions, multiple instances of the same type are allowed so both the type
    /// and a number of exceptions can be verified.
    ///
    /// For `String` patterns, the check is done via `String.contains()` on the entire captured output.
    ///
    /// When mismatch occurs, the accumulated output is dumped to the actual stderr, and the test `fail()`s.
    ///
    /// @param expected the expected exception classes (duplicates allowed), and/or string patterns
    ///
    public static void checkStderr(Object ... expected) {
        if (stderrCapture != null) {
            boolean err = false;
            String text = stderrCapture.getAccumulatedOutput();

            // exceptions
            Map<String, Integer> errors = findErrors(text);
            Map<String, Integer> exp = toMap(expected);
            if (!errors.equals(exp)) {
                stderr.println("Mismatch in thrown exceptions:\n  expected=" + exp + "\n  observed=" + errors);
                err = true;
            }

            // patterns
            for (Object x : expected) {
                if (x instanceof String s) {
                    if (!text.contains(s)) {
                        stderr.println("Expected pattern not found: " + s);
                        err = true;
                    }
                }
            }

            if (err) {
                stderr.println(text);
                // mismatch fails the test
                fail("Unexpected stderr output");
            }
        }
    }

    /// Checks the accumulated stderr buffer for the expected exceptions and string patterns,
    /// then restores the redirection.
    ///
    /// This method is equivalent to calling {@link #checkStderr(Object...)} followed by
    /// {@link #restoreStderr()}.
    ///
    /// @param expected the expected exception classes (duplicates allowed), and/or string patterns
    ///
    public static void checkAndRestoreStderr(Object ... expected) {
        try {
            checkStderr(expected);
        } finally {
            restoreStderr();
        }
    }

    private static Map<String, Integer> toMap(Object... expected) {
        HashMap<String, Integer> m = new HashMap<>();
        for (Object x : expected) {
            if (x instanceof Class c) {
                if (Throwable.class.isAssignableFrom(c)) {
                    String name = c.getName();
                    Integer v = m.get(name);
                    if (v == null) {
                        m.put(name, Integer.valueOf(1));
                    } else {
                        m.put(name, Integer.valueOf(v + 1));
                    }
                } else {
                    throw new IllegalArgumentException("must specify Class<? extends Throwable>: " + c);
                }
            } else if (x instanceof String) {
                // ok
            } else {
                throw new IllegalArgumentException("must specify either Class<? extends Throwable> or String: " + x);
            }
        }
        return m;
    }

    private static Map<String, Integer> findErrors(String text) {
        HashMap<String, Integer> m = new HashMap<>();
        text.lines().
            map((s) -> findException(s)).
            filter((c) -> c != null).
            forEach((c) -> {
                Integer v = m.get(c);
                if (v == null) {
                    m.put(c, Integer.valueOf(1));
                } else {
                    m.put(c, Integer.valueOf(v + 1));
                }
            });
        return m;
    }

    /// This regex matches either of the two patterns which might appear in the output:
    ///
    /// `Exception in thread "main" java.lang.RuntimeException:`
    ///
    /// or
    ///
    /// `java.lang.NullPointerException: ...`
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
        "(?:" +
            // catches lines starting with things like "Exception in thread "main" java.lang.RuntimeException:"
            "^" + // start of line
            "(?:" + // non-capturing group
                "Exception in thread\s+\"[^\"]*\"\\s+" +
                "(" + // capture group 1
                    "(?:[a-zA-Z_][a-zA-Z0-9_]*\\.)*" +
                    "(?:" +
                        "(?:[A-Z][a-zA-Z0-9]*)*" +
                        "(?:Exception|Error)" +
                    ")" +
                ")" +
            ")" +
        ")" +
        "|" + // or
        "(?:" +
            // catches lines starting with things like "java.lang.NullPointerException: Cannot invoke..."
            "^" +
            "(" + // capture group 2
                "(?:[a-zA-Z_][a-zA-Z0-9_]*\\.)*" +
                "(?:[A-Z][a-zA-Z0-9]*)*" +
                "(?:Exception|Error)" +
            ")" +
        ")");

    private static String findException(String text) {
        Matcher m = EXCEPTION_PATTERN.matcher(text);
        String name;
        if (m.find()) {
            name = m.group(1);
            if (name == null) {
                name = m.group(2);
            }
            return name;
        }
        return null;
    }

    @Test
    public void testFindException() {
        t("Exception in thread \"main\" java.lang.Error", "java.lang.Error");
        t("Exception in thread \"main\" java.lang.RuntimeException: blah blah", "java.lang.RuntimeException");
        t("java.lang.NullPointerException: Cannot invoke \"Object.toString(", "java.lang.NullPointerException");
        t("    at javafx.base/com.sun.javafx.binding.SelectBinding$AsString.computeValue(SelectBinding.java:392)", null);
    }

    private void t(String text, String expected) {
        String s = findException(text);
        assertEquals(expected, s);
    }
}
