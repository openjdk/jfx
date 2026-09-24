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
import com.sun.glass.ui.gtk.GtkSyntheticEvents;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The goldens of the GTK glass boundary traces: what the JNI build of commit {@code 033187ad90} passed across the
 * C -&gt; Java boundary of the event code for a scripted scenario, recorded by {@link GtkEventTrace} in a child JVM,
 * compared line by line against every later build with the same test code.
 * <p>
 * A trace depends on the X server, GTK, the keyboard layout and the screen, so each golden carries
 * {@code machine.*} keys; where they differ from this machine's, the comparison is skipped - or failed under
 * {@code -Djfx.parity.require=true} ({@link ParityGate}). Goldens exist only for a display without a window manager
 * ({@code <scenario>-nowm-golden.txt}): a window manager places, reparents, focuses and restacks windows
 * asynchronously to the scenario, which makes the trace differ from run to run ({@link #exact}). A scenario may also
 * have a golden for a display with {@code N} monitors ({@code <scenario>-Nmon-nowm-golden.txt}), which a run on such
 * a display is compared with instead ({@link #fileName}).
 * <p>
 * Capture is deliberate and refused on anything but the JNI build: {@code -Djfx.gtk.trace.capture=true} writes a
 * golden from the run, {@code -Djfx.gtk.trace.regenerate=true} on top overwrites an existing one (a behaviour
 * change to be reviewed, not a test fix). Every run writes its raw and normalised trace next to the child's output
 * for diffing.
 */
final class GtkTraceGolden {

    static final String CAPTURE_PROPERTY = "jfx.gtk.trace.capture";
    static final String REGENERATE_PROPERTY = "jfx.gtk.trace.regenerate";

    /** The commit whose JNI libraries the goldens record. */
    static final String CAPTURE_COMMIT = "033187ad90";

    /** What {@code GtkGlassShim.eventJniEntryPoints} answers on the JNI build the goldens are captured from. */
    static final String JNI_ENTRY_POINTS = "6/6";

    /**
     * What {@code GtkSyntheticEvents.upcallPath} answers where the event code's calls into Java take its JNI calls,
     * the build the goldens are captured from - not the callback tables that replace them, which a library exporting
     * those entry points may use as well.
     */
    static final String JNI_UPCALL_PATH = "jni";

    static final String TRACE_KEY = "trace";

    /**
     * {@code -Dgtk.trace.repeat=N} (default 1): how many times the trace tests run their scenario. Every normalised
     * trace must then equal the first - the measurement that decided the normalisation rules.
     */
    static final String REPEAT_PROPERTY = "gtk.trace.repeat";

    private static final String RESOURCE_DIR = "src/test/resources/test/com/sun/glass/ui/gtk";
    private static final String CLASSES_DIR = "target/test-classes/test/com/sun/glass/ui/gtk";
    private static final String MACHINE_PREFIX = "machine.";
    private static final String CAPTURE_PREFIX = "capture.";
    private static final String TRACE_MARKER = "--- trace";
    private static final int MAX_REPORTED = 80;

    /** The normalised trace of the first run of each scenario in this JVM, which later runs must equal. */
    private static final Map<String, List<String>> FIRST_RUNS = new ConcurrentHashMap<>();

    private GtkTraceGolden() {
    }

    // ---------------------------------------------------------------------------------------------
    // Child side
    // ---------------------------------------------------------------------------------------------

    /**
     * Records the machine gate of a trace golden under {@code machine.*} and the provenance under
     * {@code capture.*}: GTK version and the sha256 of the {@code libgtk-3} and {@code libgdk-3} this process mapped,
     * the X server, screen, scale, depth, resolution, monitor count, window manager, compositing, the XKB rules, and
     * whether the JNI entry points of the event code exist; FX thread only where it must be.
     */
    static void recordMachine(Map<String, String> out) throws Exception {
        GtkGlassChild.recordEnvironment(out);
        for (String key : List.of("env.gtk", "env.screen", "env.scale", "env.windowManager", "env.compositing",
                "env.depth", "env.resolution", "env.xserver")) {
            out.put(MACHINE_PREFIX + key.substring(4), out.get(key));
        }
        GtkGlassChild.onFx(() -> {
            out.put(MACHINE_PREFIX + "monitors", Integer.toString(GtkGlassShim.gdkMonitorCount()));
            out.put(MACHINE_PREFIX + "xkb", GtkGlassShim.rootStringProperty("_XKB_RULES_NAMES"));
            out.put(CAPTURE_PREFIX + "jniEntryPoints", GtkGlassShim.eventJniEntryPoints());
            out.put(CAPTURE_PREFIX + "upcallPath", GtkSyntheticEvents.upcallPath());
            return null;
        });
        out.put(MACHINE_PREFIX + "libgtk3", librarySha256("libgtk-3.so.0"));
        out.put(MACHINE_PREFIX + "libgdk3", librarySha256("libgdk-3.so.0"));
        for (String name : List.of("GDK_SCALE", "GDK_DPI_SCALE", "GDK_BACKEND", "GTK_IM_MODULE", "XMODIFIERS")) {
            String value = System.getenv(name);
            out.put(MACHINE_PREFIX + "env." + name, value == null ? "unset" : value);
        }
        out.put(CAPTURE_PREFIX + "java", System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
    }

    /** sha256 of the file this process mapped for the library whose file name starts with {@code soname}. */
    static String librarySha256(String soname) throws IOException {
        for (String line : Files.readAllLines(Path.of("/proc/self/maps"))) {
            int slash = line.indexOf('/');
            if (slash < 0) {
                continue;
            }
            Path path = Path.of(line.substring(slash));
            if (path.getFileName().toString().startsWith(soname)) {
                try {
                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                    return HexFormat.of().formatHex(sha.digest(Files.readAllBytes(path)));
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return "not-mapped";
    }

    /** Runs in {@link GtkGlassChild}: records the environment, including the window manager. */
    static void windowManagerProbe(Map<String, String> out) throws Exception {
        GtkGlassChild.recordEnvironment(out);
    }

    // ---------------------------------------------------------------------------------------------
    // Parent side
    // ---------------------------------------------------------------------------------------------

    /**
     * Skips the calling test class unless the display runs no window manager: for scenarios that script window
     * positions, stacking and focus, which a window manager decides for itself. A plain skip, not a
     * {@link ParityGate} failure: the scenario does not exist there by design.
     */
    static void requireNoWindowManager(Class<?> test) {
        GtkGlassChildJvm.Run probe = GtkGlassChildJvm.run(GtkTraceGolden.class, "windowManagerProbe", List.of());
        String wm = probe.values().get("env.windowManager");
        assumeTrue("unknown".equals(wm), test.getSimpleName() + " scripts window positions, stacking and focus for an"
                + " X server without a window manager, and this display runs " + wm);
    }

    /**
     * Runs {@code scenarioClass#method} {@code -Dgtk.trace.repeat} times in a child JVM and {@link #collect}s each
     * run; answers the first.
     */
    static GtkGlassChildJvm.Run runRepeated(Class<?> scenarioClass, String method, List<String> options,
                                            String scenario, List<Rule> rules) {
        int repeat = Math.max(1, Integer.getInteger(REPEAT_PROPERTY, 1));
        GtkGlassChildJvm.Run first = null;
        for (int i = 1; i <= repeat; i++) {
            GtkGlassChildJvm.Run run = GtkGlassChildJvm.run(scenarioClass, method, options);
            collect(scenario, i, run, rawTrace(run), rules);
            if (first == null) {
                first = run;
            }
        }
        return first;
    }

    /**
     * Keeps run {@code n} of {@code scenario}: copies its raw trace to {@code target/gtk-trace-runs/<scenario>/} and,
     * without a window manager, fails unless its normalised trace equals the first run's.
     */
    static void collect(String scenario, int n, GtkGlassChildJvm.Run run, List<String> raw, List<Rule> rules) {
        try {
            Path runs = Path.of("target", "gtk-trace-runs", scenario).toAbsolutePath();
            Files.createDirectories(runs);
            Files.write(runs.resolve("raw-" + n + ".txt"), raw, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> normalised = normalise(raw, rules);
        if (n == 1) {
            FIRST_RUNS.put(scenario, normalised);
            return;
        }
        if (!exact(run)) {
            return;
        }
        List<String> diff = diff(FIRST_RUNS.get(scenario), normalised);
        if (!diff.isEmpty()) {
            fail("the normalised trace of " + scenario + " is not reproducible: run " + n + " differs from run 1"
                    + " (raw traces in target/gtk-trace-runs/" + scenario + "):\n"
                    + String.join("\n", diff.subList(0, Math.min(MAX_REPORTED, diff.size()))));
        }
    }

    /**
     * Whether the trace of {@code run} has a golden: only without a window manager. Under one, window placement,
     * reparenting into the frame, focus and stacking are the window manager's, asynchronous to the scenario. Measured
     * with openbox on the JNI build: five runs of the event trace gave up to five different normalised traces, and a
     * sixth run missed 52 lines that the first five had in common.
     */
    static boolean exact(GtkGlassChildJvm.Run run) {
        return "nowm".equals(windowManagerKey(run));
    }

    /** The raw trace a child recorded under {@link #TRACE_KEY}. */
    static List<String> rawTrace(GtkGlassChildJvm.Run run) {
        String text = run.values().get(TRACE_KEY);
        if (text == null) {
            throw new AssertionError("the child recorded no trace: " + run.describe());
        }
        return List.of(text.split("\n", -1));
    }

    /** {@code nowm} without a window manager, else its name in lower case without blanks. */
    static String windowManagerKey(GtkGlassChildJvm.Run run) {
        String wm = run.values().getOrDefault(MACHINE_PREFIX + "windowManager", "unknown");
        return "unknown".equals(wm) ? "nowm" : wm.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * The golden of {@code scenario} for a display with {@code monitors} GDK monitors ({@code machine.monitors}):
     * {@code <scenario>-nowm-golden.txt} for one - or an unknown count - and {@code <scenario>-Nmon-nowm-golden.txt}
     * for {@code N} &gt; 1, whose trace differs where the scenario uses the other monitors.
     */
    static String fileName(String scenario, String monitors) {
        return monitors == null || monitors.equals("1") ? scenario + "-nowm-golden.txt"
                : scenario + "-" + monitors + "mon-nowm-golden.txt";
    }

    /**
     * Compares {@code normalised} with the golden of {@code scenario}, or captures it under {@link #CAPTURE_PROPERTY};
     * writes {@code raw} and {@code normalised} beside the child's output either way. Under a window manager there is
     * no golden ({@link #exact}), and the comparison is skipped.
     *
     * @param legend lines that explain the scenario's trace, written into a captured golden's header
     */
    static void verify(Class<?> test, String scenario, GtkGlassChildJvm.Run run, List<String> raw,
                       List<String> normalised, List<String> legend) {
        verify(test, scenario, run, raw, normalised, legend, List.of());
    }

    /**
     * {@link #verify(Class, String, GtkGlassChildJvm.Run, List, List, List)} with {@code goldenRules} applied to the
     * golden before the comparison: for a rule added after the golden was captured, which must then be idempotent
     * on a trace it has already been applied to. A captured golden has every rule of {@code normalised} applied.
     */
    static void verify(Class<?> test, String scenario, GtkGlassChildJvm.Run run, List<String> raw,
                       List<String> normalised, List<String> legend, List<Rule> goldenRules) {
        Path work = run.stdout().getParent();
        try {
            Files.write(work.resolve(scenario + "-raw.txt"), raw, StandardCharsets.UTF_8);
            Files.write(work.resolve(scenario + "-normalised.txt"), normalised, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assumeTrue(exact(run), "the trace golden of " + scenario + " is for a display without a window manager; this"
                + " one runs " + run.values().get(MACHINE_PREFIX + "windowManager") + ", which makes the trace differ"
                + " from run to run");
        Map<String, String> machine = new TreeMap<>();
        Map<String, String> capture = new TreeMap<>();
        for (Map.Entry<String, String> entry : run.values().entrySet()) {
            if (entry.getKey().startsWith(MACHINE_PREFIX)) {
                machine.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().startsWith(CAPTURE_PREFIX)) {
                capture.put(entry.getKey(), entry.getValue());
            }
        }
        // the golden of this display's monitor count; one without it falls back to the one-monitor golden, whose
        // machine keys then say that it is no oracle here
        String file = fileName(scenario, machine.get(MACHINE_PREFIX + "monitors"));
        if (Boolean.getBoolean(CAPTURE_PROPERTY)) {
            if (!JNI_ENTRY_POINTS.equals(capture.get(CAPTURE_PREFIX + "jniEntryPoints"))) {
                fail("refusing to capture " + file + ": a trace golden records the JNI build of commit "
                        + CAPTURE_COMMIT + ", and this build's libglassgtk3.so exports "
                        + capture.get(CAPTURE_PREFIX + "jniEntryPoints") + " of the event code's JNI entry points");
            }
            if (!JNI_UPCALL_PATH.equals(capture.get(CAPTURE_PREFIX + "upcallPath"))) {
                fail("refusing to capture " + file + ": a trace golden records the JNI calls of commit "
                        + CAPTURE_COMMIT + ", and in this build the event code calls Java through "
                        + capture.get(CAPTURE_PREFIX + "upcallPath"));
            }
            write(file, scenario, machine, capture, normalised, legend);
            abort("golden captured to " + file + "; a capture run verifies nothing");
        }
        Golden golden = load(file);
        if (golden == null && !file.equals(fileName(scenario, null))) {
            file = fileName(scenario, null);
            golden = load(file);
        }
        String compared = file;
        ParityGate.Ledger ledger = ParityGate.ledger(test);
        if (golden == null) {
            ledger.requireOracle(false, () -> "no trace golden " + compared + " on the classpath; capture one with -D"
                    + CAPTURE_PROPERTY + "=true on the JNI build of commit " + CAPTURE_COMMIT);
            return;
        }
        List<String> differences = new ArrayList<>();
        TreeSet<String> keys = new TreeSet<>(golden.machine().keySet());
        keys.addAll(machine.keySet());
        for (String key : keys) {
            String expected = golden.machine().get(key);
            String actual = machine.get(key);
            if (expected == null || !expected.equals(actual)) {
                differences.add(key + ": golden=" + expected + " here=" + actual);
            }
        }
        ledger.requireOracle(differences.isEmpty(), () -> "the trace golden " + compared + " was captured on a"
                + " different machine or display, so it is not an oracle here. Differences: " + differences);
        ledger.compared(Math.max(1, golden.trace().size()));
        List<String> diff = diff(normalise(golden.trace(), goldenRules), normalised);
        if (!diff.isEmpty()) {
            fail(diff.size() + " line(s) of the normalised trace differ from " + file + " (captured from the JNI"
                    + " build of commit " + CAPTURE_COMMIT + "; '-' golden, '+' this run; full trace in "
                    + work.resolve(scenario + "-normalised.txt") + "):\n"
                    + String.join("\n", diff.subList(0, Math.min(MAX_REPORTED, diff.size()))));
        }
    }

    /** A loaded golden. */
    record Golden(Map<String, String> machine, List<String> trace) {
    }

    /** The golden {@code file} on the classpath, or {@code null}. */
    static Golden load(String file) {
        try (InputStream in = GtkTraceGolden.class.getResourceAsStream(file)) {
            if (in == null) {
                return null;
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Golden parse(String text) {
        Map<String, String> machine = new LinkedHashMap<>();
        List<String> trace = new ArrayList<>();
        boolean inTrace = false;
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (inTrace) {
                    trace.add(line);
                } else if (line.equals(TRACE_MARKER)) {
                    inTrace = true;
                } else if (line.startsWith(MACHINE_PREFIX)) {
                    int eq = line.indexOf('=');
                    machine.put(line.substring(0, eq), line.substring(eq + 1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Golden(machine, trace);
    }

    private static void write(String file, String scenario, Map<String, String> machine,
                              Map<String, String> capture, List<String> trace, List<String> legend) {
        Path module = moduleDirectory();
        Path source = module.resolve(RESOURCE_DIR).resolve(file);
        if (Files.exists(source) && !Boolean.getBoolean(REGENERATE_PROPERTY)) {
            fail("a trace golden already exists at " + source + ". It is the record of what the JNI build passed"
                    + " across the boundary; to overwrite it re-run with -D" + REGENERATE_PROPERTY + "=true and"
                    + " review the diff as a behaviour change, not as a test fix.");
        }
        List<String> out = new ArrayList<>();
        out.add("# GTK glass boundary trace golden: " + scenario);
        out.add("# Every call the GTK glass event code (glass_window.cpp, glass_window_ime.cpp, glass_dnd.cpp,");
        out.add("# GlassView.cpp, glass_screen.cpp, glass_general.cpp) made across the C -> Java boundary for the");
        out.add("# scripted scenario, captured from the JNI build of commit " + CAPTURE_COMMIT + " and normalised");
        out.add("# by the RULES of the test class. For an X server without a window manager; machine-specific:");
        out.add("# compared only where every machine.* key matches, elsewhere skipped, or failed under");
        out.add("# -Djfx.parity.require=true.");
        out.add("# Capture: -D" + CAPTURE_PROPERTY + "=true on the JNI build (refused elsewhere);");
        out.add("# overwrite: add -D" + REGENERATE_PROPERTY + "=true.");
        for (String line : legend) {
            out.add("# " + line);
        }
        for (Map.Entry<String, String> entry : machine.entrySet()) {
            out.add(entry.getKey() + "=" + entry.getValue());
        }
        for (Map.Entry<String, String> entry : capture.entrySet()) {
            out.add(entry.getKey() + "=" + entry.getValue());
        }
        out.add(TRACE_MARKER);
        out.addAll(trace);
        try {
            Files.createDirectories(source.getParent());
            Files.write(source, out, StandardCharsets.UTF_8);
            Path classes = module.resolve(CLASSES_DIR).resolve(file);
            Files.createDirectories(classes.getParent());
            Files.write(classes, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.println("captured " + trace.size() + " trace lines to " + source);
    }

    private static Path moduleDirectory() {
        Path dir = Path.of("").toAbsolutePath();
        if (Files.isDirectory(dir.resolve(RESOURCE_DIR).getParent().getParent())) {
            return dir;
        }
        Path module = dir.resolve("modules").resolve("javafx.graphics");
        if (Files.isDirectory(module)) {
            return module;
        }
        throw new IllegalStateException("cannot find the javafx.graphics module directory from " + dir);
    }

    // ---------------------------------------------------------------------------------------------
    // Normalisation
    // ---------------------------------------------------------------------------------------------

    /** A normalisation of a whole trace; each one removes a nondeterminism that repeated runs measured. */
    @FunctionalInterface
    interface Rule {
        List<String> apply(List<String> trace);
    }

    /**
     * Sorts every run of lines a scenario recorded inside one of its own nested reads
     * ({@link GtkEventTrace#NESTED_MARK}). A read that spins a nested main loop - {@code mimesFromSystem} and
     * {@code popFromSystem} wait in {@code gtk_main_iteration} for {@code GDK_SELECTION_NOTIFY} - dispatches whatever
     * else the X server delivers meanwhile (a crossing after the drag source's pointer ungrab, a frame-clock expose)
     * at a point that depends on timing: five runs of the in-process drag put those lines at different places among
     * the reads. Sorting keeps every line of the run - every upcall with its arguments - and drops only its order.
     */
    static final Rule SORT_NESTED_READS = trace -> {
        List<String> result = new ArrayList<>();
        List<String> run = new ArrayList<>();
        for (String line : trace) {
            if (line.startsWith(GtkEventTrace.NESTED_MARK)) {
                run.add(line);
                continue;
            }
            run.sort(null);
            result.addAll(run);
            run.clear();
            result.add(line);
        }
        run.sort(null);
        result.addAll(run);
        return result;
    };

    /** A view's {@code REPAINT} as {@link GtkEventTrace} records it, inside a nested read or not. */
    private static final Pattern REPAINT = Pattern.compile("(?:" + Pattern.quote(GtkEventTrace.NESTED_MARK)
            + ")?(v[0-9a-z]+\\.view REPAINT)");

    /**
     * Moves every view {@code REPAINT} of a scenario step to the end of that step, keeping their order and number.
     * A {@code REPAINT} is the C's {@code notifyRepaint} for a {@code GDK_EXPOSE} that GDK's frame clock emits from a
     * timer, while the other notifications of a step come from X events: when one scripted call produces both - a
     * window's opacity is an X property and a redraw - the expose lands before or after the property notification
     * depending on timing (the event trace, 1 run in 5); in a drop target it lands inside or after the target's
     * nested reads, so a repaint loses its nested-read mark too. Nothing else about a repaint varies.
     */
    static final Rule REPAINT_LAST_IN_STEP = trace -> {
        List<String> result = new ArrayList<>();
        List<String> repaints = new ArrayList<>();
        for (String line : trace) {
            Matcher repaint = REPAINT.matcher(line);
            if (line.startsWith("==")) {
                result.addAll(repaints);
                repaints.clear();
                result.add(line);
            } else if (repaint.matches()) {
                repaints.add(repaint.group(1));
            } else {
                result.add(line);
            }
        }
        result.addAll(repaints);
        return result;
    };

    /** Applies {@code rules} in order. */
    static List<String> normalise(List<String> raw, List<Rule> rules) {
        List<String> result = raw;
        for (Rule rule : rules) {
            result = rule.apply(result);
        }
        return List.copyOf(result);
    }

    // ---------------------------------------------------------------------------------------------
    // Diff
    // ---------------------------------------------------------------------------------------------

    /** A line diff ({@code -} golden only, {@code +} this run only, with the preceding step marker as context). */
    static List<String> diff(List<String> expected, List<String> actual) {
        int n = expected.size();
        int m = actual.size();
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = expected.get(i).equals(actual.get(j))
                        ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        List<String> out = new ArrayList<>();
        String step = "(start)";
        String reportedStep = null;
        int i = 0;
        int j = 0;
        while (i < n || j < m) {
            if (i < n && j < m && expected.get(i).equals(actual.get(j))) {
                if (expected.get(i).startsWith("== ")) {
                    step = expected.get(i);
                }
                i++;
                j++;
                continue;
            }
            if (!step.equals(reportedStep)) {
                out.add("  @ " + step);
                reportedStep = step;
            }
            if (j < m && (i == n || lcs[i][j + 1] >= lcs[i + 1][j])) {
                out.add("  + " + actual.get(j++));
            } else {
                out.add("  - " + expected.get(i++));
            }
        }
        return out.stream().filter(line -> !line.startsWith("  @ ")).count() == 0 ? List.of() : out;
    }
}
