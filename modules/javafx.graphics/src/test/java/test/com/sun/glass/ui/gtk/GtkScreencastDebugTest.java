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

import com.sun.glass.ui.gtk.screencast.ScreencastHelper;
import com.sun.glass.ui.gtk.screencast.XdgDesktopPortal;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * What the screen capture C prints while {@code ScreencastHelper} initializes with
 * {@code -Djavafx.robot.screenshotDebug=true} and the screen capture method selected, on a machine that has no
 * PipeWire library: the debug lines of {@code native-glass/gtk/screencast_pipewire.c}, in its order, and no
 * others.
 * <p>
 * Up to commit {@code 033187ad90} that path was
 * {@code Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_loadPipewire}, and the debug text is that commit's.
 * The golden was captured while {@code ScreencastHelper} still declared its work as {@code native} methods and
 * that function had become a wrapper around the {@code sc_load_pipewire} of
 * {@code native-glass/gtk/screencast_api.h}, so the {@code __func__} the debug macro prints for a statement that
 * moved into an {@code sc_*} function is that function's name, not the {@code Java_*} name of the commit. It is
 * the whole stdout of the child, with one normalisation: the {@code __LINE__} the debug macro prints after the
 * function name is replaced by {@value #LINE_PLACEHOLDER}, because the C file's line numbers move when C around
 * the function is deleted and that is not a change of what is printed.
 * <p>
 * The portal itself cannot be reached here - this machine has no PipeWire and no xdg-desktop-portal - so this
 * pins the failing load, which is the branch a machine without them takes: the two debug lines, an unavailable
 * helper, and a {@code TokenStorage} that was never even initialized, because the JNI looked its method up only
 * after the PipeWire symbols had loaded and this class installs the callback table at exactly that point.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkScreencastDebugTest {

    /** Writes the golden of this class from the run; refused unless the calls are still JNI natives. */
    static final String CAPTURE_PROPERTY = "jfx.gtk.screencast.capture";

    /** On top of {@link #CAPTURE_PROPERTY}, overwrites a golden that exists. */
    static final String REGENERATE_PROPERTY = "jfx.gtk.screencast.regenerate";

    /** The commit whose debug text the golden records; the capture itself ran on the tree that wrapped it. */
    private static final String CAPTURE_COMMIT = "033187ad90";

    /** What replaces the {@code __LINE__} of the debug macro. */
    static final String LINE_PLACEHOLDER = "<line>";

    private static final String GOLDEN = "gtk-screencast-debug-golden.txt";
    private static final String RESOURCE_DIR = "src/test/resources/test/com/sun/glass/ui/gtk";
    private static final String CLASSES_DIR = "target/test-classes/test/com/sun/glass/ui/gtk";
    private static final String CAPTURE_PREFIX = "capture.";
    private static final String STDOUT_MARKER = "--- stdout";

    /** {@code JFX: <function>:<line> } - the prefix {@code DEBUG_SCREENCAST} puts on every line. */
    private static final Pattern DEBUG_PREFIX = Pattern.compile("^(JFX: [A-Za-z_][A-Za-z0-9_]*):[0-9]+ ");

    /** The line the C prints when {@code dlopen} of the PipeWire library fails; the branch this golden holds for. */
    private static final String NO_PIPEWIRE = "could not load pipewire library";

    private static final List<String> DEBUG = List.of("-Djavafx.robot.screenshotMethod=dbusScreencast",
            "-Djavafx.robot.screenshotDebug=true");

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkScreencastDebugTest.class, "screencastScenario", DEBUG);
    }

    /** The debug output of the failing load, line for line. */
    @Test
    public void theDebugOutputIsWhatTheNativeLoadPrinted() {
        List<String> stdout = normalise(lines(run.stdout()));
        requirePipewireIsAbsent(stdout);
        String implementation = value(CAPTURE_PREFIX + "screencast");
        if (Boolean.getBoolean(CAPTURE_PROPERTY)) {
            if (!"native".equals(implementation)) {
                fail("refusing to capture " + GOLDEN + ": this golden records the stdout of a build whose"
                        + " ScreencastHelper still calls the C through JNI natives, and in this build it is "
                        + implementation);
            }
            write(stdout, implementation);
            abort("golden captured to " + GOLDEN + "; a capture run verifies nothing");
        }
        List<String> golden = load();
        if (golden == null) {
            fail("no golden " + GOLDEN + " on the classpath; capture one with -D" + CAPTURE_PROPERTY + "=true on"
                    + " a build whose ScreencastHelper still declares native methods");
        }
        assertEquals(String.join("\n", golden), String.join("\n", stdout), run::describe);
    }

    /**
     * Without PipeWire the load fails, the helper is not available and the class says so on the error stream -
     * which is what makes {@code GtkRobot} fall back to XTest and GDK.
     */
    @Test
    public void theHelperIsNotAvailableWithoutPipewire() {
        requirePipewireIsAbsent(normalise(lines(run.stdout())));
        assertEquals("dbusScreencast", value("method"), run::describe);
        assertEquals("false", value("available"), run::describe);
        assertTrue(new String(run.stderrBytes(), StandardCharsets.ISO_8859_1)
                .contains("Could not load native libraries for ScreencastHelper"), run::describe);
    }

    /**
     * {@code TokenStorage} is not initialized when the load fails before the point the JNI looked its method up:
     * with the debug output on, an initialized {@code TokenStorage} prints the file it would use.
     */
    @Test
    public void theTokenStorageIsNotInitializedWhenTheLoadFails() {
        requirePipewireIsAbsent(normalise(lines(run.stdout())));
        assertFalse(lines(run.stdout()).stream().anyMatch(line -> line.startsWith("Token storage:")),
                run::describe);
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    /** Runs in {@link GtkGlassChild} with the toolkit started for it, so the glass GTK library is loaded. */
    static void screencastScenario(Map<String, String> out) {
        out.put(CAPTURE_PREFIX + "screencast", helperIsNative() ? "native" : "java");
        out.put("method", XdgDesktopPortal.getMethod());
        out.put("available", Boolean.toString(ScreencastHelper.isAvailable()));
    }

    /** Whether {@code ScreencastHelper} still declares its work as {@code native} methods. */
    private static boolean helperIsNative() {
        for (Method method : ScreencastHelper.class.getDeclaredMethods()) {
            if (Modifier.isNative(method.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // The golden
    // ---------------------------------------------------------------------------------------------

    /**
     * The golden holds for a machine whose {@code dlopen} of {@code libpipewire-0.3.so.0} fails. Where PipeWire is
     * installed the C goes further and prints other lines; that is an environment this expectation was not
     * captured in, not a change of the code.
     */
    private static void requirePipewireIsAbsent(List<String> stdout) {
        ParityGate.requireOracle(GtkScreencastDebugTest.class,
                stdout.stream().anyMatch(line -> line.endsWith(NO_PIPEWIRE)), ParityGate.required(),
                () -> "this machine has a PipeWire library, and the golden of the screen capture debug output"
                        + " was captured where the load fails at dlopen.");
    }

    /** The {@code __LINE__} of the debug prefix replaced by {@value #LINE_PLACEHOLDER}. */
    private static List<String> normalise(List<String> stdout) {
        List<String> normalised = new ArrayList<>();
        for (String line : stdout) {
            normalised.add(DEBUG_PREFIX.matcher(line).replaceFirst("$1:" + LINE_PLACEHOLDER + " "));
        }
        return normalised;
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
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

    /** The stdout lines of the golden on the classpath, or {@code null} if there is none. */
    private static List<String> load() {
        try (InputStream in = GtkScreencastDebugTest.class.getResourceAsStream(GOLDEN)) {
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

    private static void write(List<String> stdout, String implementation) {
        Path module = moduleDirectory();
        Path source = module.resolve(RESOURCE_DIR).resolve(GOLDEN);
        if (Files.exists(source) && !Boolean.getBoolean(REGENERATE_PROPERTY)) {
            fail("a golden already exists at " + source + ". It is the record of what the JNI natives printed;"
                    + " to overwrite it re-run with -D" + REGENERATE_PROPERTY + "=true and review the diff as a"
                    + " behaviour change, not as a test fix.");
        }
        List<String> out = new ArrayList<>();
        out.add("# The stdout of a JVM whose ScreencastHelper initializes with the screen capture method and");
        out.add("# -Djavafx.robot.screenshotDebug=true on a machine with no PipeWire library: the debug lines of");
        out.add("# screencast_pipewire.c, whose text is the text of commit " + CAPTURE_COMMIT + ".");
        out.add("# Captured while ScreencastHelper still declared its work as native methods (the capture is");
        out.add("# refused otherwise) and Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_loadPipewire was");
        out.add("# a wrapper around the sc_load_pipewire of screencast_api.h - not on commit " + CAPTURE_COMMIT
                + " itself.");
        out.add("# DEBUG_SCREENCAST prints __func__, so a debug statement that moved into an sc_* function is");
        out.add("# prefixed with that function here, where at commit " + CAPTURE_COMMIT + " it was prefixed with");
        out.add("# the Java_* one. What follows the prefix is the C's own text and is compared.");
        out.add("# The " + LINE_PLACEHOLDER + " stands for the __LINE__ the debug macro prints: it moves when C is");
        out.add("# deleted around the function and is not compared.");
        out.add("# Capture: -D" + CAPTURE_PROPERTY + "=true on such a build (refused elsewhere);");
        out.add("# overwrite: add -D" + REGENERATE_PROPERTY + "=true.");
        out.add(CAPTURE_PREFIX + "debugTextOfCommit=" + CAPTURE_COMMIT);
        out.add(CAPTURE_PREFIX + "debugFunctionNames=sc_*");
        out.add(CAPTURE_PREFIX + "screencast=" + implementation);
        out.add(STDOUT_MARKER);
        out.addAll(stdout);
        try {
            Files.createDirectories(source.getParent());
            Files.write(source, out, StandardCharsets.UTF_8);
            Path classes = module.resolve(CLASSES_DIR).resolve(GOLDEN);
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
