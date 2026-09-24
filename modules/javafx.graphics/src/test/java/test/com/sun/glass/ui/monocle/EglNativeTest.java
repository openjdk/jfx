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

package test.com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.EGLShim;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * The libEGL binding of EGL on Mesa's surfaceless platform, in a child JVM that carries
 * {@code EGL_PLATFORM=surfaceless LIBGL_ALWAYS_SOFTWARE=1} in its environment: the symbol census, display,
 * initialisation, API binding, configuration choice through the Java {@code setEGLAttrs} for a window, a
 * 16-bit window and a pbuffer, the error path of an invalid attribute, and a context made current without a
 * surface. Skips only when no libEGL exists on the machine; a libEGL that is there and does not bind, or an
 * EGL call that fails, fails the test. {@code eglCreateWindowSurface} and {@code eglSwapBuffers} need a real
 * window and are left to the platform.
 */
@EnabledOnOs(OS.LINUX)
public class EglNativeTest {

    private static final List<String> LIBRARY_DIRS = List.of("/usr/lib/x86_64-linux-gnu",
            "/usr/lib/aarch64-linux-gnu", "/lib/x86_64-linux-gnu", "/lib/aarch64-linux-gnu", "/usr/lib64",
            "/usr/lib", "/lib64", "/lib", "/usr/local/lib");
    private static final int EGL_SUCCESS = 0x3000;
    private static final int EGL_BAD_ATTRIBUTE = 0x3004;
    private static final int EGL_BAD_MATCH = 0x3009;
    private static final int EGL_OPENGL_ES_API = 0x30A0;
    private static final int EGL_SURFACE_TYPE = 0x3033;
    private static final int EGL_PBUFFER_BIT = 0x0001;
    private static final int EGL_NONE = 0x3038;
    private static final int CONFIG_SLOTS = 4;

    /** The child: binds libEGL and runs the surfaceless sequence, writing {@code key=value} lines. */
    public static final class Child {

        private static final List<String> LINES = new ArrayList<>();

        public static void main(String[] args) throws IOException {
            Path output = Path.of(args[0]);
            try {
                EGLShim.loadLibrary();
                LINES.add("library=" + EGLShim.libraryName());
                LINES.add("symbols=" + String.join(";", EGLShim.boundSymbols()));
                long display = EGLShim.eglGetDisplay(0L);
                LINES.add("display=" + display);
                int[] major = {0};
                int[] minor = {0};
                LINES.add("initialized=" + EGLShim.eglInitialize(display, major, minor));
                LINES.add("version=" + major[0] + "." + minor[0]);
                LINES.add("bindApi=" + EGLShim.eglBindAPI(EGL_OPENGL_ES_API));
                choose(display, "window8888", new int[] {8, 8, 8, 8, 0, 1, 1});
                choose(display, "window565", new int[] {5, 6, 5, 0, 0, 1, 1});
                long pbufferConfig = choose(display, "pbuffer8888", new int[] {8, 8, 8, 8, 0, 1, 0});
                long[] configs = new long[CONFIG_SLOTS];
                int[] count = {-1};
                int[] invalid = {EGL_SURFACE_TYPE, EGL_PBUFFER_BIT, 0x7fff, 1, EGL_NONE};
                LINES.add("badAttribute.ok=" + EGLShim.eglChooseConfigList(display, invalid, configs, CONFIG_SLOTS,
                        count));
                LINES.add("badAttribute.error=" + EGLShim.eglGetError());
                long context = EGLShim.eglCreateContext(display, pbufferConfig, 0L, new int[0]);
                LINES.add("context=" + context);
                LINES.add("context.error=" + EGLShim.eglGetError());
                LINES.add("makeCurrent=" + EGLShim.eglMakeCurrent(display, 0L, 0L, context));
                LINES.add("makeCurrent.error=" + EGLShim.eglGetError());
            } catch (Throwable t) {
                LINES.add("failure=" + t);
                t.printStackTrace();
                Files.write(output, LINES, StandardCharsets.UTF_8);
                System.exit(2);
            }
            Files.write(output, LINES, StandardCharsets.UTF_8);
        }

        private static long choose(long display, String name, int[] attributes) {
            long[] configs = new long[CONFIG_SLOTS];
            int[] count = {-1};
            boolean ok = EGLShim.eglChooseConfig(display, attributes, configs, CONFIG_SLOTS, count);
            LINES.add(name + ".ok=" + ok);
            LINES.add(name + ".count=" + count[0]);
            boolean tailZero = true;
            for (int i = Math.max(count[0], 0); i < CONFIG_SLOTS; i++) {
                tailZero &= configs[i] == 0L;
            }
            LINES.add(name + ".tailZero=" + tailZero);
            LINES.add(name + ".error=" + EGLShim.eglGetError());
            return count[0] > 0 ? configs[0] : 0L;
        }
    }

    /**
     * Skips when this machine has no libEGL: first the facade's own lookup (the loader's search path, which
     * covers ld.so.conf.d entries no fixed list can), then the well-known directories and LD_LIBRARY_PATH.
     * Binding libEGL in this JVM is inert: no EGL call is made here, and EGL_PLATFORM matters only at
     * eglGetDisplay, which the child makes in its own environment. Public so that ES2ContextAdoptTest shares
     * the gate.
     */
    @BeforeAll
    public static void requireALibEgl() {
        if (EGLShim.isLibraryLoaded()) {
            System.out.println("[EglNativeTest] libEGL bound as " + EGLShim.libraryName());
            return;
        }
        List<Path> candidates = new ArrayList<>();
        List<String> directories = new ArrayList<>(LIBRARY_DIRS);
        String path = System.getenv("LD_LIBRARY_PATH");
        if (path != null) {
            directories.addAll(Arrays.asList(path.split(":")));
        }
        for (String directory : directories) {
            for (String name : EGLShim.LIBRARY_NAMES) {
                Path candidate = Path.of(directory, name);
                if (Files.exists(candidate)) {
                    candidates.add(candidate);
                }
            }
        }
        if (candidates.isEmpty()) {
            abort("this machine has no libEGL: none of " + EGLShim.LIBRARY_NAMES + " binds by name, and none is in "
                    + directories);
        }
        System.out.println("[EglNativeTest] libEGL candidates: " + candidates);
    }

    private static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory);
        }
        return directory;
    }

    private static Map<String, String> runChild() throws IOException, InterruptedException {
        Path work = moduleDirectory().resolve("target").resolve("egl-native");
        Files.createDirectories(work);
        Path output = work.resolve("result.txt");
        Path log = work.resolve("child.log");
        Path errors = work.resolve("hs_err");
        Files.deleteIfExists(output);
        Files.createDirectories(errors);
        clearStaleErrorFiles(errors);
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
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Child.class.getName());
        command.add(output.toString());
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(moduleDirectory().toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().put("EGL_PLATFORM", "surfaceless");
        builder.environment().put("LIBGL_ALWAYS_SOFTWARE", "1");
        Process process = builder.start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            fail("the EGL child JVM did not finish; log:\n" + tail(log));
        }
        List<Path> errorFiles;
        try (Stream<Path> files = Files.list(errors)) {
            errorFiles = files.sorted().toList();
        }
        if (process.exitValue() != 0 || !Files.isRegularFile(output) || !errorFiles.isEmpty()) {
            fail("the EGL child JVM exited with " + process.exitValue() + ", error files " + errorFiles
                    + "; log:\n" + tail(log) + "\ncommand: " + command);
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(output, StandardCharsets.UTF_8)) {
            int equals = line.indexOf('=');
            if (equals > 0) {
                result.put(line.substring(0, equals), line.substring(equals + 1));
            }
        }
        System.out.println("[EglNativeTest] " + result);
        return result;
    }

    /** Removes the crash files of earlier runs, so that only a crash of this run can fail it. */
    private static void clearStaleErrorFiles(Path errors) throws IOException {
        try (Stream<Path> files = Files.list(errors)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    private static String tail(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return String.join("\n", lines.subList(Math.max(0, lines.size() - 60), lines.size()));
        } catch (IOException e) {
            return "(no log: " + e + ")";
        }
    }

    private static int intOf(Map<String, String> result, String key) {
        String value = result.get(key);
        assertTrue(value != null, () -> key + " missing from " + result);
        return Integer.parseInt(value);
    }

    @Test
    public void theSurfacelessSequenceRunsOnMesa() throws IOException, InterruptedException {
        Map<String, String> result = runChild();
        assertTrue(EGLShim.LIBRARY_NAMES.contains(result.get("library")), result::toString);
        String library = result.get("library");
        List<String> symbols = Arrays.asList(result.get("symbols").split(";"));
        for (String name : List.of("eglGetDisplay", "eglInitialize", "eglBindAPI", "eglChooseConfig",
                "eglCreateWindowSurface", "eglCreateContext", "eglMakeCurrent", "eglSwapBuffers", "eglGetError")) {
            assertTrue(symbols.contains(library + "!" + name), () -> name + " not bound: " + symbols);
        }
        assertEquals(9, symbols.size(), symbols::toString);
        assertNotEquals(0L, Long.parseLong(result.get("display")), "eglGetDisplay(EGL_DEFAULT_DISPLAY)");
        assertEquals("true", result.get("initialized"), "eglInitialize");
        assertTrue(result.get("version").startsWith("1."), "EGL version " + result.get("version"));
        assertEquals("true", result.get("bindApi"), "eglBindAPI(EGL_OPENGL_ES_API)");
        for (String request : List.of("window8888", "window565", "pbuffer8888")) {
            assertEquals("true", result.get(request + ".ok"), request);
            int count = intOf(result, request + ".count");
            assertTrue(count >= 0 && count <= CONFIG_SLOTS, request + ".count=" + count);
            assertEquals("true", result.get(request + ".tailZero"), request + ": slots beyond the count stay 0");
            assertEquals(EGL_SUCCESS, intOf(result, request + ".error"), request + ".error");
        }
        assertTrue(intOf(result, "pbuffer8888.count") >= 1, "a pbuffer configuration on the surfaceless platform");
        assertEquals("false", result.get("badAttribute.ok"), "an invalid attribute is refused");
        assertEquals(EGL_BAD_ATTRIBUTE, intOf(result, "badAttribute.error"), "EGL_BAD_ATTRIBUTE");
        assertNotEquals(0L, Long.parseLong(result.get("context")), "eglCreateContext (ES 2)");
        assertEquals(EGL_SUCCESS, intOf(result, "context.error"));
        if (!"true".equals(result.get("makeCurrent"))) {
            assertEquals(EGL_BAD_MATCH, intOf(result, "makeCurrent.error"),
                    "without EGL_KHR_surfaceless_context a surfaceless eglMakeCurrent is EGL_BAD_MATCH");
        } else {
            assertEquals(EGL_SUCCESS, intOf(result, "makeCurrent.error"));
        }
    }
}
