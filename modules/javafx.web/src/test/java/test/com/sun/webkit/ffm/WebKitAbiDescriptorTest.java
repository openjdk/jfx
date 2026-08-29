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

package test.com.sun.webkit.ffm;

import com.sun.webkit.WebKitNativeShim;
import com.sun.webkit.WkjStubShim;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks that every {@link java.lang.foreign.FunctionDescriptor} the generated DOM facades bind
 * agrees with {@code buildtools/ffm-web/dom-abi.tsv}, and that both agree with the C library's own
 * view of its function shapes.
 * <p>
 * This is the highest value test available in this repository, for two reasons. It covers all 1796
 * compiled DOM entry points in one pass rather than the handful a round trip test can reach; and a
 * descriptor mismatch is the one binding error that does not fail cleanly. A wrong return layout or
 * a missing argument makes the JVM read the wrong register or the wrong stack slot, which is silent
 * memory corruption rather than an exception, so nothing downstream would report it.
 * <p>
 * The Java side is read out of the compiled facade class files rather than from loaded classes, so
 * this runs whether or not any native library is present: the check that must never be skipped is
 * not allowed to depend on one.
 */
@Tag("ffm")
public class WebKitAbiDescriptorTest {

    /** How many mismatches a failure message lists before it summarises the rest. */
    private static final int REPORT_LIMIT = 50;

    /** The number of {@code BUILT=1} rows the spec is known to carry (contract section 11.2). */
    private static final int EXPECTED_BUILT_ROWS = 1796;

    private record SpecRow(String symbol, String kinds, boolean raises, String file) {
    }

    private static List<SpecRow> builtRows;
    private static int skippedRows;

    @BeforeAll
    static void readSpec() throws IOException {
        String property = System.getProperty("jfx.web.dom.abi.spec");
        assertNotNull(property, "-Djfx.web.dom.abi.spec is not set."
                + " The surefire ffm-binding-test execution passes it; this file is in the"
                + " repository and the test must not be skipped for want of it.");
        Path spec = Path.of(property);
        assertTrue(Files.isRegularFile(spec), "the DOM ABI spec is missing: " + spec.toAbsolutePath());

        List<String> lines = Files.readAllLines(spec, StandardCharsets.UTF_8);
        assertTrue(lines.size() > 1, "the DOM ABI spec is empty: " + spec.toAbsolutePath());

        Map<String, Integer> columns = new HashMap<>();
        String[] header = lines.get(0).split("\t", -1);
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i], i);
        }
        for (String required : List.of("SYMBOL", "RET_LAYOUT", "PARAM_LAYOUTS", "THROWS", "BUILT")) {
            assertTrue(columns.containsKey(required),
                    "the DOM ABI spec has no " + required + " column; its format has changed");
        }

        List<SpecRow> rows = new ArrayList<>();
        int skipped = 0;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] cells = line.split("\t", -1);
            if (!"1".equals(cells[columns.get("BUILT")])) {
                // Its source file is commented out of PlatformJava.cmake, so the library does not
                // export it and no facade may bind it (contract section 11.2).
                skipped++;
                continue;
            }
            rows.add(new SpecRow(cells[columns.get("SYMBOL")],
                    kindsOf(cells[columns.get("RET_LAYOUT")], cells[columns.get("PARAM_LAYOUTS")]),
                    "THROWS".equals(cells[columns.get("THROWS")]),
                    cells[columns.getOrDefault("FILE", 0)]));
        }
        builtRows = List.copyOf(rows);
        skippedRows = skipped;
    }

    /**
     * The Java descriptors against the spec. Every {@code BUILT=1} row must be bound by some facade,
     * with exactly the layouts the row declares.
     */
    @Test
    public void javaDescriptorsMatchTheSpec() {
        List<String> mismatches = new ArrayList<>();
        for (SpecRow row : builtRows) {
            String actual = WebKitNativeShim.descriptorOf(row.symbol());
            if (actual == null) {
                mismatches.add(row.symbol() + ": no facade binds it (spec says " + row.kinds()
                        + ", from " + row.file() + ")");
            } else if (!row.kinds().equals(actual)) {
                mismatches.add(row.symbol() + ": spec says " + row.kinds() + ", the facade binds "
                        + actual);
            }
        }
        assertTrue(mismatches.isEmpty(),
                () -> report("Java descriptor versus dom-abi.tsv", mismatches, builtRows.size()));
    }

    /**
     * The C library's own signature table against the spec. The stub derives its table from the C
     * types of the declarations it compiles, not from the spec's layout columns, so this is an
     * independent third opinion: Java, the spec and C must all three agree.
     */
    @Test
    public void cLibrarySignaturesMatchTheSpec() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
        Map<String, String> library = new HashMap<>();
        for (int i = 0, n = WkjStubShim.symbolCount(); i < n; i++) {
            library.put(WkjStubShim.symbolName(i), WkjStubShim.symbolSignature(i));
        }
        List<String> mismatches = new ArrayList<>();
        for (SpecRow row : builtRows) {
            String actual = library.get(row.symbol());
            if (actual == null) {
                mismatches.add(row.symbol() + ": the library does not export it");
            } else if (!row.kinds().equals(actual)) {
                mismatches.add(row.symbol() + ": spec says " + row.kinds() + ", the C library is "
                        + actual);
            }
        }
        assertTrue(mismatches.isEmpty(),
                () -> report("C signature versus dom-abi.tsv", mismatches, builtRows.size()));
    }

    /**
     * Java against C directly, so that a spec that is wrong in the same way twice cannot hide a
     * disagreement between the two things that actually meet at run time.
     */
    @Test
    public void javaDescriptorsMatchTheCLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
        Map<String, String> library = new HashMap<>();
        for (int i = 0, n = WkjStubShim.symbolCount(); i < n; i++) {
            library.put(WkjStubShim.symbolName(i), WkjStubShim.symbolSignature(i));
        }
        List<String> mismatches = new ArrayList<>();
        int checked = 0;
        for (String symbol : WebKitNativeShim.boundSymbols()) {
            String expected = library.get(symbol);
            if (expected == null) {
                continue;
            }
            checked++;
            String actual = WebKitNativeShim.descriptorOf(symbol);
            if (!expected.equals(actual)) {
                mismatches.add(symbol + ": the C library is " + expected + ", the facade binds "
                        + actual);
            }
        }
        int total = checked;
        assertTrue(mismatches.isEmpty(),
                () -> report("Java descriptor versus the C library", mismatches, total));
        assertTrue(checked >= EXPECTED_BUILT_ROWS,
                "only " + checked + " symbols were compared against the library, expected at least "
                        + EXPECTED_BUILT_ROWS);
    }

    /**
     * A truncated or mis-parsed spec must fail rather than pass vacuously: a run that checks four
     * rows and reports no mismatch is worse than no test at all.
     */
    @Test
    public void theSpecCoversEveryCompiledFunction() {
        assertTrue(builtRows.size() >= EXPECTED_BUILT_ROWS,
                "the spec yielded only " + builtRows.size() + " BUILT=1 rows, expected at least "
                        + EXPECTED_BUILT_ROWS + " (" + skippedRows + " rows were skipped as BUILT=0)");
        assertTrue(skippedRows > 0, "no BUILT=0 rows were found; the spec's BUILT column has"
                + " changed meaning, and symbols the library does not export may now be bound");
    }

    /**
     * Prints the row count actually verified, so that the number appears in the build log rather
     * than only in a reviewer's assumption about it.
     */
    @Test
    public void reportsHowMuchWasChecked() {
        int bound = WebKitNativeShim.boundSymbols().size();
        System.out.println("dom-abi.tsv: " + builtRows.size() + " BUILT=1 rows checked, "
                + skippedRows + " BUILT=0 rows skipped; the facades bind " + bound + " symbols");
        assertTrue(bound >= EXPECTED_BUILT_ROWS,
                "the facades bind only " + bound + " symbols, expected at least "
                        + EXPECTED_BUILT_ROWS);
    }

    private static String report(String what, List<String> mismatches, int checked) {
        StringBuilder text = new StringBuilder(mismatches.size() + " of " + checked
                + " functions disagree (" + what + "):");
        int limit = Math.min(REPORT_LIMIT, mismatches.size());
        for (int i = 0; i < limit; i++) {
            text.append(System.lineSeparator()).append("  ").append(mismatches.get(i));
        }
        if (mismatches.size() > limit) {
            text.append(System.lineSeparator()).append("  ... and ")
                    .append(mismatches.size() - limit).append(" more");
        }
        return text.toString();
    }

    /*
     * The spec names layouts; the descriptor metadata and the C library both speak the one letter
     * kind alphabet of FFM-TEST-PLAN.md section 2.5. This is the only place the two are related.
     */
    private static String kindsOf(String returnLayout, String parameterLayouts) {
        StringBuilder kinds = new StringBuilder();
        kinds.append(kindOf(returnLayout));
        if (!parameterLayouts.isEmpty() && !"-".equals(parameterLayouts)) {
            for (String layout : parameterLayouts.split(",")) {
                kinds.append(kindOf(layout.trim()));
            }
        }
        return kinds.toString();
    }

    private static char kindOf(String layout) {
        return switch (layout) {
            case "void" -> 'v';
            case "JAVA_BYTE" -> 'b';
            case "JAVA_SHORT" -> 'h';
            case "JAVA_INT" -> 'i';
            case "JAVA_LONG" -> 'l';
            case "JAVA_FLOAT" -> 'f';
            case "JAVA_DOUBLE" -> 'd';
            case "ADDRESS" -> 'p';
            default -> throw new IllegalStateException(
                    "dom-abi.tsv names a layout this test does not know: " + layout);
        };
    }
}
