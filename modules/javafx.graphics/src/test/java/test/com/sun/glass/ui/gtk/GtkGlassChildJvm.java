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

import com.sun.javafx.PlatformUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Parent side of {@link GtkGlassChild}: launches a scenario in a JVM with this module's options and returns what
 * it recorded.
 * <p>
 * The GTK glass tests need the real toolkit on an X11 display, which the module's own test JVM never has
 * ({@code StubToolkit}), so every scenario runs in a child with the parent's module path, exports,
 * {@code java.library.path} (this build's {@code target/native/bin}) and native access, the parent's
 * {@code DISPLAY} and no {@code WAYLAND_DISPLAY}. {@link #requireDisplay()} skips a test class on a host that is not
 * Linux or has no {@code DISPLAY}; {@link #requireRobot()} additionally requires {@code -DUSE_ROBOT=true}, for
 * scenarios that move the pointer or press keys and so must only run on a private display.
 * <p>
 * A child is given at most {@link #CHILD_SECONDS} seconds. JUnit's default timeout for lifecycle methods is
 * shorter, and a class-level {@code @Timeout} does not apply to them, so every {@code @BeforeAll} that launches
 * children carries its own {@code @Timeout}: {@link #BEFORE_ALL_SECONDS} per child. A launch that is interrupted -
 * by that timeout, for one - kills its child before it rethrows.
 */
final class GtkGlassChildJvm {

    /** How long {@link #run} waits for a child JVM. */
    static final long CHILD_SECONDS = 180;

    /** The {@code @Timeout} of an {@code @BeforeAll} per child it launches: {@link #CHILD_SECONDS} and a margin. */
    static final long BEFORE_ALL_SECONDS = 240;

    /**
     * The environment that the environment-bound expectations of these tests were captured in, as
     * {@link GtkGlassChild#recordEnvironment} records it: Xvfb 21.1.22 with no window manager and no compositor, one
     * 1920x1080 screen at 24 bits and 96 dpi, GTK 3.24.52.
     */
    static final Map<String, String> CAPTURE_ENVIRONMENT = Map.of(
            "env.gtk", "3.24.52",
            "env.screen", "1920x1080",
            "env.scale", "1.0x1.0",
            "env.windowManager", "unknown",
            "env.compositing", "supports=1 composited=0",
            "env.depth", "24",
            "env.resolution", "96.0");

    /** What a child JVM left behind. */
    record Run(String scenario, int exitCode, Map<String, String> values, Path stdout, Path stderr,
               List<Path> errorFiles, List<String> command) {

        String describe() {
            return "child JVM '" + scenario + "' exited with " + exitCode + "; hs_err files: " + errorFiles
                    + "\nvalues: " + values + "\nlast stderr lines:\n" + tail(stderr, 60) + "\ncommand: " + command;
        }

        byte[] stderrBytes() {
            try {
                return Files.readAllBytes(stderr);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private GtkGlassChildJvm() {
    }

    /** Skips the calling test class unless this is Linux with an X11 {@code DISPLAY}. */
    static void requireDisplay() {
        assumeTrue(PlatformUtil.isLinux(), "GTK glass runs on Linux only");
        String display = System.getenv("DISPLAY");
        assumeTrue(display != null && !display.isBlank(), "no DISPLAY: the GTK glass tests need an X11 display");
    }

    /** {@link #requireDisplay()}, plus {@code -DUSE_ROBOT=true} for scenarios that inject input. */
    static void requireRobot() {
        requireDisplay();
        assumeTrue(Boolean.getBoolean("USE_ROBOT"), "input-injecting GTK glass tests need -DUSE_ROBOT=true");
    }

    /**
     * For an expectation that holds only on a display like the one it was captured on
     * ({@link #CAPTURE_ENVIRONMENT}), not on a desktop session: skips the calling test - or fails it under
     * {@code -Djfx.parity.require=true} ({@link ParityGate}) - unless every {@code key} that {@code run} recorded
     * ({@link GtkGlassChild#recordEnvironment}) has its captured value.
     */
    static void requireEnvironment(Class<?> testClass, Run run, String... keys) {
        List<String> differences = new ArrayList<>();
        for (String key : keys) {
            String expected = CAPTURE_ENVIRONMENT.get(key);
            if (expected == null) {
                throw new IllegalArgumentException("no captured value for " + key);
            }
            String actual = run.values().get(key);
            if (!expected.equals(actual)) {
                differences.add(key + "=" + actual + " where it was captured with " + expected);
            }
        }
        ParityGate.requireOracle(testClass, differences.isEmpty(), ParityGate.required(),
                () -> "this expectation holds only on a display like the one it was captured on; here "
                        + String.join(", ", differences) + " (X server " + run.values().get("env.xserver") + ").");
    }

    /** A child JVM that is running; {@link #await} collects it. */
    record Pending(String scenario, Process process, Path output, Path stdout, Path stderr, Path errors,
                   List<String> command) {
    }

    /** {@link #launch} that must exit with status 0 and leave no fatal error log. */
    static Run run(Class<?> scenarioClass, String method, List<String> jvmOptions) {
        return checked(await(start(scenarioClass, method, jvmOptions, CHILD_SECONDS, true), CHILD_SECONDS));
    }

    /**
     * {@link #run} for two children that must run at the same time - one owning a selection while the other reads
     * it. Both are started before either is waited for, and both are killed if one of them hangs.
     */
    static List<Run> runTogether(List<Pending> children) {
        List<Run> runs = new ArrayList<>();
        try {
            for (Pending child : children) {
                runs.add(await(child, CHILD_SECONDS));
            }
        } finally {
            for (Pending child : children) {
                if (child.process().isAlive()) {
                    kill(child.process());
                }
            }
        }
        runs.forEach(GtkGlassChildJvm::checked);
        return runs;
    }

    /**
     * {@link #run} with {@code -Dgtk.glass.child.toolkit=false}: the child starts no toolkit before the scenario.
     * Without {@code keepDisplay} the child has no {@code DISPLAY} either.
     */
    static Run runWithoutToolkit(Class<?> scenarioClass, String method, List<String> jvmOptions,
                                 boolean keepDisplay) {
        List<String> options = new ArrayList<>(jvmOptions);
        options.add("-D" + GtkGlassChild.TOOLKIT_PROPERTY + "=false");
        return checked(launch(scenarioClass, method, options, CHILD_SECONDS, keepDisplay));
    }

    /** {@link #start} with this class's own timeout and the parent's display. */
    static Pending start(Class<?> scenarioClass, String method, List<String> jvmOptions) {
        return start(scenarioClass, method, jvmOptions, CHILD_SECONDS, true);
    }

    private static Run checked(Run run) {
        if (run.exitCode() != 0 || !run.errorFiles().isEmpty()) {
            fail(run.describe());
        }
        return run;
    }

    /**
     * Runs {@code scenarioClass#method} in {@link GtkGlassChild}, with or without the parent's {@code DISPLAY}.
     * Output, stdout, stderr and fatal error logs go to {@code target/gtk-glass-child/<class>.<method>/}.
     */
    static Run launch(Class<?> scenarioClass, String method, List<String> jvmOptions, long timeoutSeconds,
                      boolean keepDisplay) {
        return await(start(scenarioClass, method, jvmOptions, timeoutSeconds, keepDisplay), timeoutSeconds);
    }

    /** Starts {@code scenarioClass#method} in a child JVM without waiting for it. */
    static Pending start(Class<?> scenarioClass, String method, List<String> jvmOptions, long timeoutSeconds,
                         boolean keepDisplay) {
        String scenario = scenarioClass.getSimpleName() + "." + method;
        try {
            Path work = Path.of("target", "gtk-glass-child", scenario).toAbsolutePath();
            deleteTree(work);
            Path errors = work.resolve("hs_err");
            Files.createDirectories(errors);
            Path output = work.resolve("values.txt");
            Path stdout = work.resolve("stdout.txt");
            Path stderr = work.resolve("stderr.txt");

            List<String> command = new ArrayList<>();
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (argument.startsWith("-agentlib") || argument.startsWith("-javaagent")
                        || argument.startsWith("-Xrunjdwp") || argument.startsWith("-Xdebug")
                        || argument.startsWith("-XX:ErrorFile")) {
                    continue;
                }
                command.add(argument);
            }
            command.add("-XX:ErrorFile=" + errors.resolve("hs_err_pid%p.log"));
            command.addAll(jvmOptions);
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(GtkGlassChild.class.getName());
            command.add(scenarioClass.getName() + "#" + method);
            command.add(output.toString());

            ProcessBuilder builder = new ProcessBuilder(command)
                    .redirectOutput(stdout.toFile())
                    .redirectError(stderr.toFile());
            builder.environment().remove("WAYLAND_DISPLAY");
            if (!keepDisplay) {
                builder.environment().remove("DISPLAY");
            }
            return new Pending(scenario, builder.start(), output, stdout, stderr, errors, command);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Waits up to {@code timeoutSeconds} for {@code child} and reads what it recorded. */
    static Run await(Pending child, long timeoutSeconds) {
        Process process = child.process();
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                kill(process);
                fail("child JVM '" + child.scenario() + "' did not finish within " + timeoutSeconds + " s; stderr:\n"
                        + tail(child.stderr(), 60));
            }
            List<Path> errorFiles;
            try (Stream<Path> files = Files.list(child.errors())) {
                errorFiles = files.sorted().toList();
            }
            Map<String, String> values = new TreeMap<>();
            if (Files.isRegularFile(child.output())) {
                for (String line : Files.readAllLines(child.output(), StandardCharsets.UTF_8)) {
                    int eq = line.indexOf('=');
                    values.put(line.substring(0, eq), GtkGlassChild.unescape(line.substring(eq + 1)));
                }
            }
            return new Run(child.scenario(), process.exitValue(), values, child.stdout(), child.stderr(),
                    errorFiles, child.command());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            kill(process);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while the child JVM '" + child.scenario()
                    + "' ran; it was killed", e);
        }
    }

    /** Kills {@code process} and waits up to 10 s for it to go, keeping the thread's interrupt status as it was. */
    private static void kill(Process process) {
        process.destroyForcibly();
        boolean interrupted = Thread.interrupted();
        try {
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static String tail(Path file, int lines) {
        try {
            List<String> all = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
            return String.join("\n", all.subList(Math.max(0, all.size() - lines), all.size()));
        } catch (IOException | UncheckedIOException e) {
            return "(unreadable: " + e + ")";
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
