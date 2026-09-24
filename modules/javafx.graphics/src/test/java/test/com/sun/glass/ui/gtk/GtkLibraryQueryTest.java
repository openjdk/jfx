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
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * What the query for the glass GTK library prints and leaves behind when the toolkit starts with
 * {@code -Djdk.gtk.verbose=true}.
 * <p>
 * Up to commit {@code 033187ad90} that query was {@code Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary}
 * of {@code launcher.c}, the whole of {@code libglass.so}: it put {@code GDK_BACKEND=x11} into the process
 * environment, opened and closed the X11 display, looked for an already loaded GTK library with
 * {@code dlopen(RTLD_NOLOAD)} and otherwise opened {@code libgtk-3.so.0} with {@code RTLD_GLOBAL}, printing one
 * line at each step. The goldens of this class are that build's stdout, captured before the query became Java
 * ({@code capture.queryLibrary=native}), and they hold for both:
 * <ul>
 * <li>a process that has no GTK library yet, which opens one;</li>
 * <li>a process that has mapped {@code libgtk-3.so.0} already, which finds it.</li>
 * </ul>
 * {@code RTLD_GLOBAL} is not decoration: the C that stays in {@code libglassgtk3.so} resolves five GTK, GDK and
 * GIO functions with {@code dlsym(RTLD_DEFAULT, ...)} ({@code wrapped.c}), which only reach the global scope
 * because the query opens the GTK library into it.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkLibraryQueryTest {

    /** Writes the goldens of this class from the run; refused unless the query is still the JNI native. */
    static final String CAPTURE_PROPERTY = "jfx.gtk.loader.capture";

    /** On top of {@link #CAPTURE_PROPERTY}, overwrites a golden that exists. */
    static final String REGENERATE_PROPERTY = "jfx.gtk.loader.regenerate";

    /** The commit whose {@code libglass.so} launcher the goldens record. */
    private static final String CAPTURE_COMMIT = "033187ad90";

    private static final String RESOURCE_DIR = "src/test/resources/test/com/sun/glass/ui/gtk";
    private static final String CLASSES_DIR = "target/test-classes/test/com/sun/glass/ui/gtk";
    private static final String CAPTURE_PREFIX = "capture.";
    private static final String STDOUT_MARKER = "--- stdout";

    private static final List<String> VERBOSE = List.of("-Djdk.gtk.verbose=true");

    /** A toolkit started in a process that has no GTK library mapped. */
    private static GtkGlassChildJvm.Run fresh;

    /** A toolkit started after {@code GtkGlassNative} has mapped {@code libgtk-3.so.0}. */
    private static GtkGlassChildJvm.Run preloaded;

    @BeforeAll
    @Timeout(2 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenarios() {
        GtkGlassChildJvm.requireDisplay();
        fresh = GtkGlassChildJvm.run(GtkLibraryQueryTest.class, "freshScenario", VERBOSE);
        preloaded = GtkGlassChildJvm.runWithoutToolkit(GtkLibraryQueryTest.class, "preloadedScenario", VERBOSE,
                true);
    }

    @Test
    public void theQueryPrintsWhatTheLauncherPrinted() {
        verify("gtk-loader-verbose", fresh);
    }

    @Test
    public void theQueryPrintsWhatTheLauncherPrintedWithTheGtkLibraryAlreadyMapped() {
        verify("gtk-loader-verbose-preloaded", preloaded);
    }

    /**
     * The GTK library the query opens is in the global scope of the process, where the {@code dlsym(RTLD_DEFAULT,
     * ...)} of {@code wrapped.c} finds it - and its GIO and GLib dependencies with it. A process that had the
     * library mapped before the query has none of them there: nothing opened it with {@code RTLD_GLOBAL}.
     */
    @Test
    public void theGtkLibraryIsOpenedIntoTheGlobalScope() {
        assertEquals("true", value(fresh, "globalScope.gtk_main"), fresh::describe);
        assertEquals("true", value(fresh, "globalScope.g_settings_schema_source_get_default"), fresh::describe);
        assertTrue(stderr(fresh).contains("loaded gdk_x11_display_set_window_scale"), fresh::describe);
        assertEquals("false", value(preloaded, "globalScope.gtk_main"), preloaded::describe);
    }

    /**
     * {@code GDK_BACKEND=x11} is in the process environment, read back through libc {@code getenv} - not through
     * {@code System.getenv}, which answers the copy the JVM took before any of this ran.
     */
    @Test
    public void theGdkBackendIsSetInTheProcessEnvironment() {
        assertEquals("x11", value(fresh, "env.GDK_BACKEND"), fresh::describe);
        assertEquals("x11", value(preloaded, "env.GDK_BACKEND"), preloaded::describe);
    }

    // ---------------------------------------------------------------------------------------------
    // The scenarios (child JVMs)
    // ---------------------------------------------------------------------------------------------

    /** Runs in {@link GtkGlassChild} with the toolkit started for it, in a process with no GTK library mapped. */
    static void freshScenario(Map<String, String> out) {
        record(out);
    }

    /**
     * Runs in {@link GtkGlassChild} without its toolkit: maps {@code libgtk-3.so.0} first - initializing
     * {@code GtkGlassNative}, which opens it for its own bindings - and only then starts the toolkit, so that the
     * query finds a GTK library that is already there.
     */
    static void preloadedScenario(Map<String, String> out) throws InterruptedException {
        GtkGlassShim.boundSymbols();
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        if (!started.await(60, TimeUnit.SECONDS)) {
            throw new IllegalStateException("the toolkit did not start within 60 s");
        }
        record(out);
    }

    private static void record(Map<String, String> out) {
        out.put(CAPTURE_PREFIX + "queryLibrary", GtkGlassShim.queryLibraryIsNative() ? "native" : "java");
        out.put("env.GDK_BACKEND", String.valueOf(GtkGlassShim.getenv("GDK_BACKEND")));
        for (String symbol : List.of("gtk_main", "g_settings_schema_source_get_default")) {
            out.put("globalScope." + symbol, Boolean.toString(GtkGlassShim.symbolIsInTheGlobalScope(symbol)));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The goldens
    // ---------------------------------------------------------------------------------------------

    private static String value(GtkGlassChildJvm.Run run, String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    private static String stderr(GtkGlassChildJvm.Run run) {
        return new String(run.stderrBytes(), StandardCharsets.ISO_8859_1);
    }

    /** Compares the child's whole stdout with the golden of {@code scenario}, or captures it. */
    private static void verify(String scenario, GtkGlassChildJvm.Run run) {
        List<String> stdout = lines(run.stdout());
        String file = scenario + "-golden.txt";
        String implementation = value(run, CAPTURE_PREFIX + "queryLibrary");
        if (Boolean.getBoolean(CAPTURE_PROPERTY)) {
            if (!"native".equals(implementation)) {
                fail("refusing to capture " + file + ": these goldens record the stdout of the libglass.so"
                        + " launcher of commit " + CAPTURE_COMMIT + ", and in this build GtkApplication._queryLibrary"
                        + " is " + implementation);
            }
            write(file, scenario, implementation, stdout);
            abort("golden captured to " + file + "; a capture run verifies nothing");
        }
        List<String> golden = load(file);
        if (golden == null) {
            fail("no golden " + file + " on the classpath; capture one with -D" + CAPTURE_PROPERTY + "=true on the"
                    + " JNI build of commit " + CAPTURE_COMMIT);
        }
        assertEquals(String.join("\n", golden), String.join("\n", stdout), run::describe);
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The stdout lines of the golden {@code file} on the classpath, or {@code null} if there is none. */
    private static List<String> load(String file) {
        try (InputStream in = GtkLibraryQueryTest.class.getResourceAsStream(file)) {
            if (in == null) {
                return null;
            }
            List<String> stdout = new ArrayList<>();
            boolean inStdout = false;
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n", -1)) {
                if (inStdout) {
                    stdout.add(line);
                } else if (line.startsWith(STDOUT_MARKER)) {
                    inStdout = true;
                }
            }
            if (!stdout.isEmpty() && stdout.get(stdout.size() - 1).isEmpty()) {
                stdout.remove(stdout.size() - 1);
            }
            return stdout;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(String file, String scenario, String implementation, List<String> stdout) {
        Path module = moduleDirectory();
        Path source = module.resolve(RESOURCE_DIR).resolve(file);
        if (Files.exists(source) && !Boolean.getBoolean(REGENERATE_PROPERTY)) {
            fail("a golden already exists at " + source + ". It is the record of what the JNI launcher printed;"
                    + " to overwrite it re-run with -D" + REGENERATE_PROPERTY + "=true and review the diff as a"
                    + " behaviour change, not as a test fix.");
        }
        // Provenance only, and not compared: these lines are the launcher's own strings and the sonames it tries,
        // so they do not depend on the GTK of the machine the capture ran on.
        Map<String, String> machine = new TreeMap<>();
        machine.put("machine.gtk", GtkGlassChildJvm.CAPTURE_ENVIRONMENT.get("env.gtk"));
        List<String> out = new ArrayList<>();
        out.add("# The stdout of the query for the glass GTK library: " + scenario);
        out.add("# Everything GtkApplication's constructor printed with -Djdk.gtk.verbose=true, captured from the");
        out.add("# JNI build of commit " + CAPTURE_COMMIT + ", where the query was");
        out.add("# Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary of launcher.c in libglass.so. The last");
        out.add("# line is GtkApplication's own; the others are the launcher's printf calls, in its order.");
        out.add("# Capture: -D" + CAPTURE_PROPERTY + "=true on that build (refused elsewhere);");
        out.add("# overwrite: add -D" + REGENERATE_PROPERTY + "=true.");
        out.addAll(machine.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList());
        out.add(CAPTURE_PREFIX + "commit=" + CAPTURE_COMMIT);
        out.add(CAPTURE_PREFIX + "queryLibrary=" + implementation);
        out.add(STDOUT_MARKER);
        out.addAll(stdout);
        try {
            Files.createDirectories(source.getParent());
            Files.write(source, out, StandardCharsets.UTF_8);
            Path classes = module.resolve(CLASSES_DIR).resolve(file);
            Files.createDirectories(classes.getParent());
            Files.write(classes, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.println("captured " + stdout.size() + " stdout lines to " + source);
    }

    private static Path moduleDirectory() {
        Path dir = Path.of("").toAbsolutePath();
        if (Files.isDirectory(dir.resolve(RESOURCE_DIR))) {
            return dir;
        }
        Path module = dir.resolve("modules").resolve("javafx.graphics");
        if (Files.isDirectory(module)) {
            return module;
        }
        throw new IllegalStateException("cannot find the javafx.graphics module directory from " + dir);
    }
}
