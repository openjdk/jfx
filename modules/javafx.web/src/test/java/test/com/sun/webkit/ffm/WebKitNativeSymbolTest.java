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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Symbol resolution: every name a facade binds exists on the C side, and every DOM function the
 * library exports is bound by some facade. A rename on either side, or a missing export macro,
 * fails here rather than at the first use of one DOM type.
 * <p>
 * "Exists on the C side" has two sources, because the two halves of the ABI are verified by
 * different means. The DOM half is implemented by the generated {@code wkjstub} library, so it is
 * checked against the library's own export table. The hand written half is not in the stub - the
 * stub is generated from {@code dom-abi.tsv}, which covers the DOM only - so it is checked against
 * the declarations in {@code Source/WebKitLegacy/java/api/webkit_java_api*.h}, which is the same
 * source of truth the C++ half compiles against. Every header of that directory is read, not only
 * the page one, because the hand written facades now bind the platform, theme, bridge, events and
 * WTF slices as well; the DOM header is skipped, its 1796 functions being covered by the stub.
 */
@Tag("ffm")
public class WebKitNativeSymbolTest {

    /** How many names a failure message lists before it summarises the rest. */
    private static final int REPORT_LIMIT = 40;

    /** {@code WKJ_EXPORT <return type> <name>(} at the start of a line. */
    private static final Pattern DECLARATION = Pattern.compile(
            "(?m)^WKJ_EXPORT\\s+[A-Za-z0-9_]+\\s*\\**\\s*([A-Za-z0-9_]+)\\s*\\(");

    /** The functions the hand written {@code webkit_java_api*.h} headers declare. */
    private static Set<String> handWrittenAbi;

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
        handWrittenAbi = readHandWrittenAbi();
    }

    @Test
    public void everySymbolAFacadeBindsIsExported() {
        List<String> missing = new ArrayList<>();
        for (String symbol : WebKitNativeShim.boundSymbols()) {
            if (!WkjStubShim.exports(symbol) && !handWrittenAbi.contains(symbol)) {
                missing.add(symbol);
            }
        }
        assertTrue(missing.isEmpty(), () -> report(missing.size()
                + " symbols are bound by a facade but neither exported by the library nor declared"
                + " in any webkit_java_api*.h header", missing));
    }


    @Test
    public void everyDomFunctionTheLibraryExportsIsBound() {
        Set<String> bound = new HashSet<>(WebKitNativeShim.boundSymbols());
        List<String> unbound = new ArrayList<>();
        List<String> unboundCore = new ArrayList<>();
        for (int i = 0, n = WkjStubShim.symbolCount(); i < n; i++) {
            String symbol = WkjStubShim.symbolName(i);
            if (bound.contains(symbol)) {
                continue;
            }
            if (symbol.startsWith("wkj_dom_")) {
                unbound.add(symbol);
            } else {
                unboundCore.add(symbol);
            }
        }
        // A core symbol the library exports and Java does not bind yet is expected: the core ABI
        // grows ahead of its callers. A DOM one is not, because the DOM half is generated from the
        // same spec as the library.
        if (!unboundCore.isEmpty()) {
            System.out.println("note: " + unboundCore.size()
                    + " core symbols are exported but not bound yet: " + unboundCore);
        }
        assertTrue(unbound.isEmpty(), () -> report(unbound.size()
                + " DOM functions are exported but no facade binds them", unbound));
    }

    @Test
    public void noSymbolIsBoundTwice() {
        List<String> duplicates = WebKitNativeShim.duplicateBindings();
        assertTrue(duplicates.isEmpty(),
                () -> report(duplicates.size() + " symbols are bound more than once", duplicates));
    }

    @Test
    public void theThreeCoreSymbolsArePresent() {
        for (String symbol : List.of("wkj_init", "wkj_abi_version", "wkj_exception_slot")) {
            assertTrue(WkjStubShim.exports(symbol), symbol + " is not exported by the library");
        }
    }

    @Test
    public void bindingAnAbsentSymbolNamesIt() {
        String absent = "wkj_dom_NoSuchType_noSuchMethod";
        assumeTrue(!WkjStubShim.exports(absent), "the library unexpectedly exports " + absent);
        UnsatisfiedLinkError error = assertThrows(UnsatisfiedLinkError.class,
                () -> WebKitNativeShim.bindSymbol(absent));
        assertTrue(error.getMessage().contains(absent),
                "the failure must name the symbol, but said: " + error.getMessage());
    }

    /**
     * The size of the bound surface: the 1796 compiled DOM functions, {@code wkj_abi_version},
     * {@code wkj_exception_slot}, {@code wkj_init} and the 164 functions of the hand written
     * headers that the facades bind - which now includes {@code wkj_live_connect_init} and the
     * three {@code wkj_bridge_sizeof_*} self-checks. A change to that number is a change to the ABI
     * surface and has to be made here deliberately.
     * <p>
     * A page function the header declares and no facade binds is <em>reported rather than failed</em>,
     * for the reason {@link #everyDomFunctionTheLibraryExportsIsBound} gives in the other direction:
     * the C ABI grows ahead of its callers. What is unbound now is callback tables and entry points
     * whose Java caller does not exist yet, not Java code still on JNI - the module has none left.
     */
    @Test
    public void theFacadesBindTheWholeCompiledDomAbi() {
        Set<String> bound = new HashSet<>(WebKitNativeShim.boundSymbols());
        List<String> unbound = new ArrayList<>(new TreeSet<>(handWrittenAbi));
        unbound.removeAll(bound);
        if (!unbound.isEmpty()) {
            System.out.println("note: " + unbound.size() + " functions of the hand written"
                    + " webkit_java_api*.h headers are declared but no facade binds them: "
                    + unbound);
        }
        assertEquals(1963, WebKitNativeShim.boundSymbols().size(),
                "the facades bind the 1796 compiled DOM functions, wkj_abi_version,"
                        + " wkj_exception_slot, wkj_init and 164 functions of the hand written"
                        + " webkit_java_api*.h headers; a change here is a change to the ABI"
                        + " surface");
    }

    /*
     * The headers are read rather than the stub's export table because the stub is generated from
     * dom-abi.tsv and therefore implements the DOM half only. They are the same declarations the
     * C++ side compiles against, so this is a real second source and not a restatement of what the
     * facades already say.
     *
     * The whole directory is globbed rather than a fixed list being named, so that a header added
     * by a later slice is covered without an edit here. Only webkit_java_api_dom.h is skipped: its
     * 1796 functions are the stub's own export table, which is the stronger check of the two.
     */
    private static Set<String> readHandWrittenAbi() {
        String path = System.getProperty("jfx.web.page.abi.header");
        assertTrue(path != null && !path.isEmpty(),
                "jfx.web.page.abi.header is not set; the surefire argLine must point at"
                        + " webkit_java_api_page.h");
        Path page = Path.of(path);
        assertTrue(Files.isRegularFile(page),
                "the page ABI header is missing: " + page.toAbsolutePath());
        List<Path> headers;
        try (Stream<Path> entries = Files.list(page.getParent())) {
            headers = entries.filter(WebKitNativeSymbolTest::isHandWrittenAbiHeader).sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertTrue(headers.contains(page), "the ABI header directory "
                + page.getParent().toAbsolutePath() + " does not contain " + page.getFileName());
        Set<String> names = new TreeSet<>();
        for (Path header : headers) {
            names.addAll(declarationsIn(header));
        }
        // Aggregate rather than per header: webkit_java_api_pal.h declares callback tables and no
        // entry point at all, which is legitimate and must not read as a stale pattern.
        assertTrue(names.size() > 1, "no WKJ_EXPORT declarations were found in any of the "
                + headers.size() + " headers under " + page.getParent().toAbsolutePath()
                + "; the pattern this test parses them with has gone stale");
        return names;
    }

    private static boolean isHandWrittenAbiHeader(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith("webkit_java_api") && name.endsWith(".h")
                && !name.equals("webkit_java_api_dom.h");
    }

    private static Set<String> declarationsIn(Path header) {
        String text;
        try {
            text = Files.readString(header, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Set<String> names = new TreeSet<>();
        Matcher matcher = DECLARATION.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static String report(String what, List<String> names) {
        StringBuilder text = new StringBuilder(what).append(':');
        int limit = Math.min(REPORT_LIMIT, names.size());
        for (int i = 0; i < limit; i++) {
            text.append(System.lineSeparator()).append("  ").append(names.get(i));
        }
        if (names.size() > limit) {
            text.append(System.lineSeparator()).append("  ... and ")
                    .append(names.size() - limit).append(" more");
        }
        return text.toString();
    }
}
