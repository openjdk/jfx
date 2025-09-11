/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import com.sun.javafx.binding.Logging;
import com.sun.javafx.binding.Logging.ErrorLogger;
import com.sun.javafx.binding.Logging.ErrorLogger.ErrorLogRecord;
import com.sun.javafx.logging.PlatformLogger.Level;
import test.util.AccumulatingPrintStream;

public class ErrorLoggingUtility {

    private static ErrorLogger errorLogger = Logging.getLogger();

    public static void reset() {
        Logging.setKeepException(true);
        errorLogger.setErrorLogRecord(null);
    }

    public static boolean isEmpty() {
        return errorLogger.getErrorLogRecord() == null;
    }

    /**
     * Convenience method for check(Level.FINE, expectedException)
     */
    public static void checkFine(Class<?> expectedException) {
        check(Level.FINE, expectedException);
    }

    /**
     * Convenience method for check(Level.WARNING, expectedException)
     */
    public static void checkWarning(Class<?> expectedException) {
        check(Level.WARNING, expectedException);
    }

    public static void check(Level expectedLevel, Class<?> expectedException) {
        assertTrue(Logging.getKeepException());
        ErrorLogRecord errorLogRecord = errorLogger.getErrorLogRecord();
        assertNotNull(errorLogRecord);
        assertEquals(expectedLevel, errorLogRecord.getLevel());
        assertTrue(expectedException.isAssignableFrom(errorLogRecord.getThrown().getClass()));
        reset();
        checked++;
    }

    private static PrintStream stderr;
    private static AccumulatingPrintStream stderrCapture;
    private static int checked;

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
            checked = 0;
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

    /// Checks the accumulated stderr buffer for the expected exceptions.
    /// Verifies that the number and type of exceptions logged to stderr redirected to an internal buffer
    /// (by a prior call to {@link #suppressStderr()}) corresponds to the `expected` exceptions.
    ///
    /// When mismatch occurs, the accumulated output is dumped to the actual stderr.
    ///
    /// @param expected the expected exceptions (duplicates allowed)
    ///
    public static void checkStderr(Class<? extends Throwable> ... expected) {
        if (stderrCapture != null) {
            String text = stderrCapture.getAccumulatedOutput();
            Map<String,Integer> errors = findErrors(text);
            Map<String,Integer> exp = toMap(expected);
            if(!errors.equals(exp)) {
                stderr.println("Mismatch in thrown exceptions:\n  expected=" + exp + "\n  observed=" + errors);
                stderr.println(text);
            }
        }
    }

    /// Checks the accumulated stderr buffer for the expected exceptions, and restores the redirection.
    ///
    /// Verifies that the number and type of exceptions logged to stderr redirected to an internal buffer
    /// (by a prior call to {@link #suppressStderr()}) corresponds to the `expected` exceptions.
    ///
    /// When mismatch occurs, the accumulated output is dumped to the actual stderr.
    ///
    /// This method is equivalent to calling {@link #checkStderr(Class...)} followed by
    /// {@link #restoreStderr()}.
    ///
    /// @param expected the expected exceptions (duplicates allowed)
    ///
    public static void checkAndRestoreStderr(Class<? extends Throwable> ... expected) {
        checkStderr(expected);
        restoreStderr();
    }

    private static Map<String, Integer> toMap(Class<? extends Throwable> ... expected) {
        HashMap<String, Integer> m = new HashMap<>();
        for (Class<? extends Throwable> c : expected) {
            String name = c.getName();
            Integer v = m.get(name);
            if (v == null) {
                m.put(name, Integer.valueOf(1));
            } else {
                m.put(name, Integer.valueOf(v + 1));
            }
        }
        return m;
    }

    private static Map<String, Integer> findErrors(String text) {
        HashMap<String, Integer> m = new HashMap<>();
        text.
            lines().
            map((s) -> findException(s)).
            forEach((c) -> {
                if (c != null) {
                    Integer v = m.get(c);
                    if (v == null) {
                        m.put(c, Integer.valueOf(1));
                    } else {
                        m.put(c, Integer.valueOf(v + 1));
                    }
                }
            });
        return m;
    }

    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
        "(?:" +
            // catches lines starting with things like "Exception in thread "main" java.lang.RuntimeException:"
            "^" +
            "(?:" +
                "Exception in thread\s+\"[^\"]*\"\\s+" +
                "(" + // capture group 1
                    "(?:[a-zA-Z_][a-zA-Z0-9_]*\\.)*" +
                    "(?:" +
                        "(?:[A-Z][a-zA-Z0-9]*)*" +
                        "(?:Exception|Error)" +
                    ")" +
                ")" +
                //":" +
            ")" +
        ")" +
        "|" +
        "(?:" +
            // // catches lines starting with things like "java.lang.NullPointerException: Cannot invoke..."
            "^" +
            "(" + // capture group 2
                "(?:[a-zA-Z_][a-zA-Z0-9_]*\\.)*" +
                "(?:[A-Z][a-zA-Z0-9]*)*" +
                "(?:Exception|Error)" +
            ")" +
            //":" +
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

    // should I leave this test here?  to test the test?
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
