/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.iio;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The golden file: the record of what the JNI JPEG decoder did, captured before any of it was
 * rewritten.
 * <p>
 * Deliberately not {@link java.util.Properties}. Properties applies its own backslash unescaping on
 * read and its own escaping on write, which would fight the escaping this format needs for the
 * recorded callback sequences, and it writes a timestamp comment that makes every regeneration look
 * like a change. This format is one {@code key=value} per line, {@code #} comments, values escaped
 * for backslash, newline, carriage return and the {@code ;} that separates list elements.
 * <p>
 * A golden is evidence, not a fixture. Nothing in this class updates one: the generator refuses to
 * overwrite an existing file unless it is told to, and no test writes at all. If the Java rewrite
 * disagrees with a golden, the disagreement is the finding.
 */
final class JpegGoldens {

    /** Stands in for a null value, which the flat format cannot otherwise distinguish from "". */
    static final String NULL = "#null";

    /** Separates the elements of a recorded callback sequence. */
    private static final char SEPARATOR = ';';

    private final Map<String, String> values;

    private JpegGoldens(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Reads the golden file from the classpath.
     *
     * @throws AssertionError if it is absent; the message carries the capture command, because an
     *         absent golden is a capture that has not been run, never a reason to skip
     */
    static JpegGoldens load() {
        byte[] raw = JpegTestSupport.resourceBytes(JpegTestSupport.GOLDEN_FILE);
        if (raw == null) {
            throw new AssertionError("no JPEG golden file on the classpath ("
                    + JpegTestSupport.RESOURCE_PREFIX + JpegTestSupport.GOLDEN_FILE + ")."
                    + " These tests compare the decoder against values captured from the JNI build;"
                    + " with no capture there is nothing to compare and passing would mean nothing."
                    + " Capture with \"mvn -pl modules/javafx.graphics test -Dtest=JpegCorpusGenerator"
                    + " -Djfx.iio.jpeg.capture=true\" and commit the corpus and the golden file.");
        }
        return new JpegGoldens(parse(new String(raw, StandardCharsets.UTF_8)));
    }

    static JpegGoldens of(Map<String, String> values) {
        return new JpegGoldens(new LinkedHashMap<>(values));
    }

    /**
     * @throws AssertionError if {@code key} was never captured, naming the keys that were - a golden
     *         that does not cover a case cannot be made to cover it by relaxing the test
     */
    String require(String key) {
        String value = values.get(key);
        if (value == null) {
            throw new AssertionError("the golden file has no entry for \"" + key + "\". It was"
                    + " captured from a build that did not produce this case, so there is nothing to"
                    + " compare against. Keys present with the same prefix: " + near(key));
        }
        return value;
    }

    boolean has(String key) {
        return values.containsKey(key);
    }

    /** The value, or {@code null} if absent. */
    String get(String key) {
        return values.get(key);
    }

    /** Keys starting with {@code prefix}, sorted, for exhaustiveness checks. */
    List<String> keysWithPrefix(String prefix) {
        List<String> matching = new ArrayList<>();
        for (String key : new TreeSet<>(values.keySet())) {
            if (key.startsWith(prefix)) {
                matching.add(key);
            }
        }
        return matching;
    }

    private String near(String key) {
        int dot = key.indexOf('.');
        String prefix = dot < 0 ? key : key.substring(0, dot + 1);
        List<String> matching = keysWithPrefix(prefix);
        return matching.isEmpty() ? new TreeSet<>(values.keySet()).toString() : matching.toString();
    }

    // ---------------------------------------------------------------------------------------------
    // Format
    // ---------------------------------------------------------------------------------------------

    static Map<String, String> parse(String text) {
        Map<String, String> parsed = new LinkedHashMap<>();
        int line = 0;
        for (String raw : text.split("\n", -1)) {
            line++;
            String entry = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            if (entry.isBlank() || entry.startsWith("#")) {
                continue;
            }
            int equals = entry.indexOf('=');
            if (equals < 1) {
                throw new IllegalArgumentException("golden file line " + line + " is not key=value: "
                        + entry);
            }
            String key = entry.substring(0, equals);
            if (parsed.put(key, unescape(entry.substring(equals + 1))) != null) {
                throw new IllegalArgumentException("golden file line " + line + " repeats key " + key);
            }
        }
        return parsed;
    }

    static String format(Map<String, String> values, List<String> header) {
        StringBuilder text = new StringBuilder(4096);
        for (String comment : header) {
            text.append('#');
            if (!comment.isEmpty()) {
                text.append(' ').append(comment);
            }
            text.append('\n');
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            text.append(entry.getKey()).append('=').append(escape(entry.getValue())).append('\n');
        }
        return text.toString();
    }

    /** Joins recorded events; elements are escaped, so a message containing {@code ;} survives. */
    static String joinList(List<String> elements) {
        StringBuilder joined = new StringBuilder();
        for (String element : elements) {
            if (joined.length() > 0) {
                joined.append(SEPARATOR);
            }
            joined.append(escapeElement(element == null ? NULL : element));
        }
        return joined.toString();
    }

    static List<String> splitList(String value) {
        List<String> elements = new ArrayList<>();
        if (value.isEmpty()) {
            return elements;
        }
        StringBuilder element = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                element.append(c == 's' ? SEPARATOR : c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == SEPARATOR) {
                elements.add(element.toString());
                element.setLength(0);
            } else {
                element.append(c);
            }
        }
        elements.add(element.toString());
        return elements;
    }

    private static String escapeElement(String element) {
        return element.replace("\\", "\\\\").replace(String.valueOf(SEPARATOR), "\\s");
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private static String unescape(String value) {
        StringBuilder plain = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                plain.append(c);
                continue;
            }
            char next = value.charAt(++i);
            switch (next) {
                case 'n' -> plain.append('\n');
                case 'r' -> plain.append('\r');
                case '\\' -> plain.append('\\');
                default -> plain.append('\\').append(next);
            }
        }
        return plain.toString();
    }
}
