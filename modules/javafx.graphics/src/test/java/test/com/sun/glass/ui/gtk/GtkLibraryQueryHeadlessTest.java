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

package test.com.sun.glass.ui.gtk;

import com.sun.glass.ui.gtk.GtkGlassShim;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The decision table of the query for the glass GTK library, in a child JVM with no {@code DISPLAY}, so that
 * every Linux build that has the glass natives - every CI job, where the display tests of this package skip -
 * runs it.
 * <p>
 * Up to commit {@code 033187ad90} the table was {@code sniffLibs} of {@code launcher.c}: a GTK 3 library that the
 * process has already is used, a GTK 2 library that the process has already is the one hard failure, a version
 * other than 3 is corrected to 3, and otherwise the GTK 3 names are opened in order with
 * {@code RTLD_LAZY | RTLD_GLOBAL}. This machine has no GTK 2 library to load and every JVM of these tests has
 * GTK 3, so the branches are driven with names that stand in for them - a library every process has
 * ({@value #LOADED}), one this build needs but no JVM has loaded when the scenario starts
 * ({@value #LOADABLE}), and two that no system has - which also pins the two {@code dlopen} flags that carry the
 * behaviour: after the open, {@value #LOADABLE} answers {@code RTLD_NOLOAD} and its symbols are in the global
 * scope, and neither was true before.
 * <p>
 * A second child runs the other arm: a copy of this build's {@code libglassgtk3.so} named {@code libglass.so} on
 * its {@code java.library.path}, which is the deployment the query answers {@code QUERY_USE_CURRENT} for and the
 * one that must leave {@code GDK_BACKEND} alone.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkLibraryQueryHeadlessTest {

    /** {@code glass_gtk_api.h}, relative to the module directory: the C side of the shared constants. */
    private static final String HEADER = "src/main/native-glass/gtk/glass_gtk_api.h";

    /** Names no system has, for the branches that find and load nothing. */
    private static final String[] ABSENT_GTK3 = {"libjfx-no-such-gtk3.so.0", "libjfx-no-such-gtk3.so"};
    private static final String[] ABSENT_GTK2 = {"libjfx-no-such-gtk2.so.0", "libjfx-no-such-gtk2.so"};

    /** A library every Linux process has mapped, standing in for one the query finds already loaded. */
    private static final String LOADED = "libc.so.6";

    /** A library of this build that the child has not loaded when the scenario starts. */
    private static final String LOADABLE = "libXtst.so.6";

    /** A symbol of {@value #LOADABLE}, for the global scope of the process. */
    private static final String LOADABLE_SYMBOL = "XTestFakeMotionEvent";

    /** What {@code sniffLibs} answers for the GTK 3 chain: {@code use_chain[i][0][0]} of {@code launcher.c}. */
    private static final int GTK3 = '3';

    /** What {@code sniffLibs} answers when it found nothing, and {@code QUERY_NO_DISPLAY}. */
    private static final int NOT_FOUND = -1;

    /**
     * Every line the query printed in the child, which is one {@code printf} of {@code sniffLibs}
     * ({@code launcher.c}) per step. The query without a display prints nothing at all - it answers before it
     * looks for a library - and neither does a call that is not verbose, so these are the six verbose
     * {@code sniffLibs} calls of the scenario, in order.
     */
    private static final List<String> VERBOSE_LINES = List.of(
            // nothing loaded and nothing to load
            "checking GTK version 3",
            "trying GTK library libjfx-no-such-gtk3.so.0",
            "trying GTK library libjfx-no-such-gtk3.so",
            // a GTK 2 library in the process
            "checking GTK version 3",
            "found already loaded unsupported GTK library libc.so.6",
            // a GTK 3 library in the process
            "checking GTK version 3",
            "found already loaded GTK library libc.so.6",
            "using GTK library version 3 set libc.so.6",
            // a version other than 0 or 3
            "checking GTK version 2",
            "bad GTK version specified, assuming 3",
            "trying GTK library libjfx-no-such-gtk3.so.0",
            "trying GTK library libjfx-no-such-gtk3.so",
            // the first name of the chain opens
            "checking GTK version 3",
            "trying GTK library libXtst.so.6",
            "using GTK library version 3 set libXtst.so.6",
            // the second name of the chain is the one in the process
            "checking GTK version 3",
            "found already loaded GTK library libXtst.so.6",
            "using GTK library version 3 set libXtst.so.6");

    /** The glass GTK library of this build, and the name a USE_CURRENT deployment gives a copy of it. */
    private static final String GLASSGTK3 = "libglassgtk3.so";
    private static final String GLASS = "libglass.so";

    private static GtkGlassChildJvm.Run run;
    private static GtkGlassChildJvm.Run useCurrent;
    private static Path renamedLibrary;

    @BeforeAll
    @Timeout(2 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenarios() throws IOException {
        String libraryPath = System.getProperty("java.library.path", "");
        Path glassgtk3 = Stream.of(libraryPath.split(File.pathSeparator)).filter(dir -> !dir.isEmpty())
                .map(dir -> Path.of(dir, GLASSGTK3)).filter(Files::isRegularFile).findFirst().orElse(null);
        assumeTrue(glassgtk3 != null, "this build has no GTK glass natives in " + libraryPath);
        run = GtkGlassChildJvm.runWithoutToolkit(GtkLibraryQueryHeadlessTest.class, "tableScenario", List.of(),
                false);
        Path directory = Files.createTempDirectory("gtk-library-query");
        try {
            // /proc/self/maps names a mapped file by its real path
            renamedLibrary = Files.copy(glassgtk3, directory.resolve(GLASS)).toRealPath();
            useCurrent = GtkGlassChildJvm.runWithoutToolkit(GtkLibraryQueryHeadlessTest.class,
                    "useCurrentScenario", List.of("-Djava.library.path=" + directory + File.pathSeparator
                            + libraryPath), false);
        } finally {
            GtkUseCurrentLibraryTest.deleteTree(directory);
        }
    }

    /**
     * Without a display the query answers {@code QUERY_NO_DISPLAY}, and it has done what the launcher did before
     * it looked at the display: {@code GDK_BACKEND=x11} is in the process environment. It looked for no GTK
     * library, which is why the child can go on to drive the table itself.
     */
    @Test
    public void theQueryWithoutADisplayIsTheLaunchersAnswer() {
        assertEquals("null", value("display"));
        assertEquals(Integer.toString(NOT_FOUND), value("query.noDisplay"), run::describe);
        assertEquals("null", value("env.before"), run::describe);
        assertEquals("x11", value("env.after"), run::describe);
        assertEquals("false", value("loaded.gtk3.afterQuery"), run::describe);
    }

    /** Every branch of {@code sniffLibs}, by what it answers. */
    @Test
    public void theDecisionTableIsTheLaunchers() {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("sniff.neither", NOT_FOUND);
        expected.put("sniff.gtk2Loaded", NOT_FOUND);
        expected.put("sniff.gtk3Loaded", GTK3);
        expected.put("sniff.badVersion", NOT_FOUND);
        expected.put("sniff.opened", GTK3);
        expected.put("sniff.secondName", GTK3);
        expected.put("sniff.quiet", NOT_FOUND);
        Map<String, Integer> actual = new LinkedHashMap<>();
        expected.keySet().forEach(key -> actual.put(key, Integer.valueOf(value(key))));
        assertEquals(expected, actual, run::describe);
    }

    /** Every branch by what it printed. */
    @Test
    public void everyBranchPrintsWhatTheLauncherPrinted() {
        assertEquals(String.join("\n", VERBOSE_LINES), String.join("\n", lines(run.stdout())), run::describe);
    }

    /**
     * {@code RTLD_NOLOAD} answers nothing for a library the process has not loaded and a handle once it has, and
     * {@code RTLD_GLOBAL} is what puts that library's symbols where {@code dlsym(RTLD_DEFAULT, ...)} finds them.
     */
    @Test
    public void theLibraryIsOpenedIntoTheGlobalScopeAndFoundAgainWithoutLoadingIt() {
        assertEquals("false", value("loaded.loadable.before"), run::describe);
        assertEquals("false", value("global.loadable.before"), run::describe);
        assertEquals("true", value("loaded.loadable.after"), run::describe);
        assertEquals("true", value("global.loadable.after"), run::describe);
    }

    /**
     * The other arm of the query, the one the copy of
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary} in {@code GlassApplication.cpp} (commit
     * {@code 033187ad90}) was: where the library loaded as {@code glass} is itself a glass GTK library, the query
     * opens the display and answers, and it touches the process environment not at all. That copy set no
     * {@code GDK_BACKEND}, where the launcher's did, and a Wayland session whose GDK would pick its own default
     * keeps it; setting the variable on this path too would be a change of behaviour, not a port. Without a
     * display the arm answers {@code QUERY_NO_DISPLAY} and prints nothing, which is enough to read the variable
     * back afterwards.
     */
    @Test
    public void theUseCurrentArmSetsNoGdkBackend() {
        assertEquals(List.of(renamedLibrary.toString()), List.of(value(useCurrent, "mapped").split("\n")),
                useCurrent::describe);
        assertEquals("null", value(useCurrent, "env.before"), useCurrent::describe);
        assertEquals(Integer.toString(NOT_FOUND), value(useCurrent, "query"), useCurrent::describe);
        assertEquals("null", value(useCurrent, "env.after"), useCurrent::describe);
        assertEquals(List.of(), lines(useCurrent.stdout()), useCurrent::describe);
    }

    /**
     * {@code GtkApplication}'s class initializer maps the library named {@code glass} - which is how the copy
     * above gets into the process - and the Linux build produces none since the launcher went, so the ordinary
     * case is that there is nothing to map: the child initializes that class with no {@code libglass.so} on its
     * {@code java.library.path} and carries on, where an {@code UnsatisfiedLinkError} out of that initializer
     * would fail every toolkit start.
     */
    @Test
    public void theToolkitClassInitializesWhereThereIsNoGlassLibrary() {
        assertEquals("", value("glass.mapped"), run::describe);
        assertEquals("true", value("glass.initialized"), run::describe);
        assertEquals("", value("glass.mappedAfterInit"), run::describe);
    }

    /** The query opens libc and {@code libX11.so.6}, and no GTK library of its own. */
    @Test
    public void theQueryBindsLibcAndX11Only() {
        assertEquals(List.of("libc.so.6!dlopen", "libc.so.6!putenv", "libX11.so.6!XOpenDisplay",
                "libX11.so.6!XCloseDisplay"), List.of(value("bound").split(",")), run::describe);
    }

    /**
     * The constants {@code com.sun.glass.ui.gtk} and the C share: {@code GtkWindow}'s {@code HT_*}, which
     * {@code glass_window.cpp} compares the answer of {@code non_client_hit_test} against, and
     * {@code GtkApplication}'s {@code QUERY_*}, which the loader C of commit {@code 033187ad90} still answers
     * while it is there. Both sides must be changed together, so this reads the C's own {@code GGTK_*} out of
     * {@code glass_gtk_api.h}; a {@code QUERY_*} the C no longer states is not compared, an {@code HT_*} is
     * required.
     */
    @Test
    public void theSharedConstantsAreTheOnesTheCUses() {
        Map<String, Integer> header = headerConstants();
        assumeTrue(!header.isEmpty(), "no " + HEADER + " next to this build");
        Map<String, Integer> java = new LinkedHashMap<>();
        for (String entry : value("constants").split(" ")) {
            int eq = entry.indexOf('=');
            java.put(entry.substring(0, eq), Integer.valueOf(entry.substring(eq + 1)));
        }
        assertEquals(7, java.size(), java::toString);
        Map<String, Integer> expected = new LinkedHashMap<>();
        Map<String, Integer> actual = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> constant : java.entrySet()) {
            String macro = "GGTK_" + constant.getKey();
            if (constant.getKey().startsWith("HT_")) {
                assertTrue(header.containsKey(macro), () -> HEADER + " has no " + macro + ": " + header);
            }
            if (header.containsKey(macro)) {
                expected.put(macro, header.get(macro));
                actual.put(macro, constant.getValue());
            }
        }
        assertEquals(expected, actual, "the Java constants and " + HEADER + " must be changed together");
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    /** Runs in {@link GtkGlassChild} without a toolkit and without {@code DISPLAY}. */
    static void tableScenario(Map<String, String> out) throws IOException {
        out.put("display", String.valueOf(System.getenv("DISPLAY")));
        out.put("env.before", String.valueOf(GtkGlassShim.getenv("GDK_BACKEND")));
        out.put("loaded.loadable.before", Boolean.toString(GtkGlassShim.libraryIsLoaded(LOADABLE)));
        out.put("global.loadable.before", Boolean.toString(GtkGlassShim.symbolIsInTheGlobalScope(LOADABLE_SYMBOL)));

        out.put("query.noDisplay", Integer.toString(GtkGlassShim.queryLibrary(3, true)));
        out.put("env.after", String.valueOf(GtkGlassShim.getenv("GDK_BACKEND")));
        out.put("loaded.gtk3.afterQuery", Boolean.toString(GtkGlassShim.libraryIsLoaded("libgtk-3.so.0")));

        out.put("sniff.neither", sniff(3, true, ABSENT_GTK3, ABSENT_GTK2));
        out.put("sniff.gtk2Loaded", sniff(3, true, ABSENT_GTK3, new String[] {LOADED, ABSENT_GTK2[1]}));
        out.put("sniff.gtk3Loaded", sniff(3, true, new String[] {LOADED, ABSENT_GTK3[1]}, ABSENT_GTK2));
        out.put("sniff.badVersion", sniff(2, true, ABSENT_GTK3, ABSENT_GTK2));
        out.put("sniff.opened", sniff(3, true, new String[] {LOADABLE, ABSENT_GTK3[1]}, ABSENT_GTK2));
        out.put("sniff.secondName", sniff(3, true, new String[] {ABSENT_GTK3[0], LOADABLE}, ABSENT_GTK2));
        out.put("sniff.quiet", sniff(3, false, ABSENT_GTK3, ABSENT_GTK2));

        out.put("loaded.loadable.after", Boolean.toString(GtkGlassShim.libraryIsLoaded(LOADABLE)));
        out.put("global.loadable.after", Boolean.toString(GtkGlassShim.symbolIsInTheGlobalScope(LOADABLE_SYMBOL)));
        out.put("bound", String.join(",", GtkGlassShim.loaderBoundSymbols()));
        out.put("constants", GtkGlassShim.sharedConstants().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + " " + b).orElseThrow());

        // last, so that nothing above it runs with GtkApplication initialized: there is no libglass.so on this
        // child's java.library.path, which is the ordinary case since the launcher library went
        out.put("glass.mapped", String.join("\n", GtkUseCurrentLibraryTest.mappedGlassLibraries()));
        GtkGlassShim.initializeGtkApplication();
        out.put("glass.initialized", "true");
        out.put("glass.mappedAfterInit", String.join("\n", GtkUseCurrentLibraryTest.mappedGlassLibraries()));
    }

    /**
     * Runs in {@link GtkGlassChild} without a toolkit and without {@code DISPLAY}, with a copy of this build's
     * {@code libglassgtk3.so} named {@code libglass.so} first on {@code java.library.path}: initializing
     * {@code GtkApplication} maps that copy, and the query then takes the arm of {@code GlassApplication.cpp}.
     */
    static void useCurrentScenario(Map<String, String> out) throws IOException {
        out.put("env.before", String.valueOf(GtkGlassShim.getenv("GDK_BACKEND")));
        GtkGlassShim.initializeGtkApplication();
        out.put("mapped", String.join("\n", GtkUseCurrentLibraryTest.mappedGlassLibraries()));
        out.put("query", Integer.toString(GtkGlassShim.queryLibrary(3, true)));
        out.put("env.after", String.valueOf(GtkGlassShim.getenv("GDK_BACKEND")));
    }

    private static String sniff(int wantVersion, boolean verbose, String[] gtk3, String[] gtk2) {
        return Integer.toString(GtkGlassShim.sniffLibs(wantVersion, verbose, gtk3, gtk2));
    }

    // ---------------------------------------------------------------------------------------------
    // Support
    // ---------------------------------------------------------------------------------------------

    private static String value(String key) {
        return value(run, key);
    }

    private static String value(GtkGlassChildJvm.Run child, String key) {
        String value = child.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + child.describe());
        }
        return value;
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The {@code GGTK_QUERY_*} and {@code GGTK_HT_*} of {@code glass_gtk_api.h}, empty without the sources. */
    private static Map<String, Integer> headerConstants() {
        Path header = moduleDirectory().resolve(HEADER);
        if (!Files.isRegularFile(header)) {
            return Map.of();
        }
        Pattern define = Pattern.compile("^#define\\s+(GGTK_(?:QUERY|HT)_\\w+)\\s+\\(?(-?\\d+)\\)?\\s*(?:/\\*.*)?$");
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String line : lines(header)) {
            Matcher matcher = define.matcher(line);
            if (matcher.matches()) {
                values.put(matcher.group(1), Integer.valueOf(matcher.group(2)));
            }
        }
        return values;
    }

    private static Path moduleDirectory() {
        Path dir = Path.of("").toAbsolutePath();
        if (Files.isDirectory(dir.resolve("src").resolve("main"))) {
            return dir;
        }
        return dir.resolve("modules").resolve("javafx.graphics");
    }
}
