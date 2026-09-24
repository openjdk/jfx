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

package test.com.sun.prism.es2;

import com.sun.glass.ui.monocle.EGLShim;
import com.sun.glass.ui.monocle.LinuxSystemShim;
import com.sun.prism.es2.ES2NativeShim;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.glass.ui.monocle.EglNativeTest;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * {@code es2_context_adopt} through {@code ES2Native.contextAdopt} against a real GLES context: a child JVM on
 * Mesa's surfaceless platform ({@code EGL_PLATFORM=surfaceless LIBGL_ALWAYS_SOFTWARE=1}) creates and makes
 * current an ES 2 context through the Monocle EGL facade, opens {@code libGLESv2} with the {@code dlopen} of
 * AcceleratedScreen, and adopts the context with the {@code dlsym} loader. The driver strings must be those
 * of a GLES context; every entry point of the table must be exactly what the loader answered, NULL included
 * ({@code glTexImage2DMultisample} is not a GLES entry point and stays NULL); a loader that throws yields a
 * context whose entry points are all NULL and no crash; a null loader, or no current context, yields 0; the
 * loader stub's arena is closed once the call has returned; and {@code es2_context_release} frees the result.
 * Run once against the desktop {@code prism_es2} and once against {@code prism_es2_monocle} (the child
 * carries {@code -Dglass.platform=Monocle}, which is what makes {@code ES2Native} bind that library). Each
 * run skips only when its library was not built, when this machine has no libEGL (the gate of EglNativeTest) or
 * when the surfaceless platform yields no display or no surfaceless context; a library that is there and does
 * not bind or fails, fails. The desktop scenario assumes a libGL whose {@code glGetString} dispatches to the
 * context libEGL made current (glvnd's shared libGLdispatch, or Mesa's libglapi): a path no product code takes,
 * since the desktop prism_es2 never adopts; the Monocle scenario is the product path. The class needs longer
 * than the default test timeout: two children of up to 150 s each.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(value = 600, unit = TimeUnit.SECONDS)
public class ES2ContextAdoptTest {

    /** The 50 rows of {@code es2_proc_table} every platform has (the swap-interval rows are platform-only). */
    static final List<String> PROC_NAMES = List.of("glActiveTexture", "glAttachShader", "glBindAttribLocation",
            "glBindFramebuffer", "glBindRenderbuffer", "glCheckFramebufferStatus", "glCompileShader",
            "glCreateProgram", "glCreateShader", "glDeleteBuffers", "glDeleteFramebuffers", "glDeleteProgram",
            "glDeleteShader", "glDeleteRenderbuffers", "glDetachShader", "glDisableVertexAttribArray",
            "glEnableVertexAttribArray", "glFramebufferRenderbuffer", "glFramebufferTexture2D",
            "glGenFramebuffers", "glGenRenderbuffers", "glGetProgramiv", "glGetShaderiv", "glGetUniformLocation",
            "glLinkProgram", "glRenderbufferStorage", "glShaderSource", "glGetShaderInfoLog",
            "glGetProgramInfoLog", "glBufferSubData", "glUniform1f", "glUniform2f", "glUniform3f", "glUniform4f",
            "glUniform4fv", "glUniform1i", "glUniform2i", "glUniform3i", "glUniform4i", "glUniform4iv",
            "glUniformMatrix4fv", "glUseProgram", "glValidateProgram", "glVertexAttribPointer", "glGenBuffers",
            "glBindBuffer", "glBufferData", "glTexImage2DMultisample", "glRenderbufferStorageMultisample",
            "glBlitFramebuffer");

    private static final String NOT_A_GLES_ENTRY_POINT = "glTexImage2DMultisample";
    private static final int EGL_OPENGL_ES_API = 0x30A0;
    private static final int EGL_BAD_MATCH = 0x3009;

    /** The child: an ES 2 context, then the adopt scenarios, written as {@code key=value} lines. */
    public static final class Child {

        private static final List<String> LINES = new ArrayList<>();

        public static void main(String[] args) throws IOException {
            Path output = Path.of(args[0]);
            try {
                run();
            } catch (Throwable t) {
                LINES.add("failure=" + t);
                t.printStackTrace();
                Files.write(output, LINES, StandardCharsets.UTF_8);
                System.exit(2);
            }
            Files.write(output, LINES, StandardCharsets.UTF_8);
        }

        private static void run() {
            ES2NativeShim.loadLibrary();
            LINES.add("library=" + ES2NativeShim.libraryName());
            LINES.add("abi=" + ES2NativeShim.abiVersion());
            EGLShim.loadLibrary();
            long display = EGLShim.eglGetDisplay(0L);
            LINES.add("display=" + display);
            if (display == 0) {
                return;
            }
            int[] major = {0};
            int[] minor = {0};
            require(EGLShim.eglInitialize(display, major, minor), "eglInitialize");
            require(EGLShim.eglBindAPI(EGL_OPENGL_ES_API), "eglBindAPI");
            long[] configs = new long[1];
            int[] count = {0};
            require(EGLShim.eglChooseConfig(display, new int[] {8, 8, 8, 8, 0, 1, 0}, configs, 1, count)
                    && count[0] == 1, "eglChooseConfig(pbuffer 8888)");
            long context = EGLShim.eglCreateContext(display, configs[0], 0L, new int[0]);
            require(context != 0, "eglCreateContext");
            boolean current = EGLShim.eglMakeCurrent(display, 0L, 0L, context);
            LINES.add("makeCurrent=" + current);
            LINES.add("makeCurrent.error=" + EGLShim.eglGetError());
            if (!current) {
                return;
            }
            int flags = LinuxSystemShim.RTLD_LAZY | LinuxSystemShim.RTLD_GLOBAL;
            long gles = LinuxSystemShim.dlopen("libGLESv2.so", flags);
            if (gles == 0) {
                gles = LinuxSystemShim.dlopen("libGLESv2.so.2", flags);
            }
            require(gles != 0, "dlopen libGLESv2: " + LinuxSystemShim.dlerror());

            Map<String, Long> answered = new LinkedHashMap<>();
            long adopted = ES2NativeShim.contextAdopt(gles, (handle, name) -> {
                long address = LinuxSystemShim.dlsym(handle, name);
                answered.put(name, address);
                return address;
            });
            LINES.add("adopted=" + adopted);
            LINES.add("stubAlive=" + ES2NativeShim.loaderStubAlive());
            LINES.add("loaderCalls=" + answered.size());
            LINES.add("loaderNames=" + String.join(";", answered.keySet()));
            require(adopted != 0, "es2_context_adopt");
            LINES.add("version=" + ES2NativeShim.contextGetString(adopted, ES2NativeShim.STR_VERSION));
            LINES.add("vendor=" + ES2NativeShim.contextGetString(adopted, ES2NativeShim.STR_VENDOR));
            LINES.add("renderer=" + ES2NativeShim.contextGetString(adopted, ES2NativeShim.STR_RENDERER));
            String extensions = ES2NativeShim.contextGetString(adopted, ES2NativeShim.STR_EXTENSIONS);
            LINES.add("extensions.length=" + (extensions == null ? -1 : extensions.length()));
            List<String> mismatches = new ArrayList<>();
            int nulls = 0;
            for (String name : PROC_NAMES) {
                Long loaded = answered.get(name);
                long stored = ES2NativeShim.contextGetProcAddress(adopted, name);
                if (loaded == null || loaded != stored) {
                    mismatches.add(name + ":" + loaded + "!=" + stored);
                }
                if (stored == 0) {
                    nulls++;
                }
            }
            LINES.add("mismatches=" + String.join(";", mismatches));
            LINES.add("nulls=" + nulls);
            LINES.add("multisampleNull=" + (ES2NativeShim.contextGetProcAddress(adopted, NOT_A_GLES_ENTRY_POINT) == 0));
            ES2NativeShim.contextRelease(adopted);

            long throwing = ES2NativeShim.contextAdopt(gles, (handle, name) -> {
                throw new IllegalStateException("loader failure for " + name);
            });
            LINES.add("throwing.adopted=" + throwing);
            require(throwing != 0, "adopt with a throwing loader");
            boolean allNull = true;
            for (String name : PROC_NAMES) {
                allNull &= ES2NativeShim.contextGetProcAddress(throwing, name) == 0;
            }
            LINES.add("throwing.allNull=" + allNull);
            ES2NativeShim.contextRelease(throwing);

            LINES.add("nullLoader=" + ES2NativeShim.contextAdopt(gles, null));
            require(EGLShim.eglMakeCurrent(display, 0L, 0L, 0L), "eglMakeCurrent(EGL_NO_CONTEXT)");
            LINES.add("noContext=" + ES2NativeShim.contextAdopt(gles, LinuxSystemShim::dlsym));
            LINES.add("done=true");
        }

        private static void require(boolean condition, String what) {
            if (!condition) {
                throw new IllegalStateException(what + " failed in the child");
            }
        }
    }

    private static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory);
        }
        return directory;
    }

    /** Skips when the build produced no such library; the child then has to bind it. */
    private static void requireBuilt(String libraryName) {
        String file = System.mapLibraryName(libraryName);
        List<Path> entries = ES2Natives.libraryPathEntries();
        boolean built = entries.stream().anyMatch(dir -> Files.isRegularFile(dir.resolve(file)));
        if (!built) {
            abort("this build has no " + file + " on java.library.path " + entries + ": the library is optional"
                    + " (INCLUDE_ES2 / INCLUDE_ES2_MONOCLE, the latter needing libgles-dev) and was not included");
        }
    }

    private static Map<String, String> runChild(String scenario, List<String> jvmOptions)
            throws IOException, InterruptedException {
        Path work = moduleDirectory().resolve("target").resolve("es2-context-adopt").resolve(scenario);
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
                    || argument.startsWith("-XX:ErrorFile") || argument.startsWith("-Dglass.platform")) {
                continue;
            }
            command.add(argument);
        }
        command.add("-XX:ErrorFile=" + errors.resolve("hs_err_pid%p.log"));
        command.addAll(jvmOptions);
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
        if (!process.waitFor(150, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            fail("child JVM " + scenario + " did not finish; log:\n" + tail(log));
        }
        List<Path> errorFiles;
        try (Stream<Path> files = Files.list(errors)) {
            errorFiles = files.sorted().toList();
        }
        if (process.exitValue() != 0 || !Files.isRegularFile(output) || !errorFiles.isEmpty()) {
            fail("child JVM " + scenario + " exited with " + process.exitValue() + ", error files " + errorFiles
                    + "; log:\n" + tail(log) + "\ncommand: " + command);
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(output, StandardCharsets.UTF_8)) {
            int equals = line.indexOf('=');
            if (equals > 0) {
                result.put(line.substring(0, equals), line.substring(equals + 1));
            }
        }
        System.out.println("[ES2ContextAdoptTest] " + scenario + " " + result);
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

    private static void assertAdopted(Map<String, String> result, String library, int expectedLoaderCalls) {
        assertEquals(library, result.get("library"));
        assertEquals("3", result.get("abi"));
        if ("0".equals(result.get("display"))) {
            abort("eglGetDisplay(EGL_DEFAULT_DISPLAY) is EGL_NO_DISPLAY on this machine's surfaceless platform:"
                    + " nothing to adopt");
        }
        if (!"true".equals(result.get("makeCurrent"))) {
            int error = Integer.parseInt(result.get("makeCurrent.error"));
            if (error == EGL_BAD_MATCH) {
                abort("the EGL of this machine cannot make a context current without a surface (no"
                        + " EGL_KHR_surfaceless_context): EGL_BAD_MATCH; nothing to adopt");
            }
            fail("eglMakeCurrent without a surface failed with EGL error 0x" + Integer.toHexString(error));
        }
        assertNotEquals(0L, Long.parseLong(result.get("adopted")), "es2_context_adopt");
        assertEquals("false", result.get("stubAlive"), "the loader stub arena is closed after the call");
        assertEquals(expectedLoaderCalls, Integer.parseInt(result.get("loaderCalls")), "loader calls");
        List<String> names = List.of(result.get("loaderNames").split(";"));
        for (String name : PROC_NAMES) {
            assertTrue(names.contains(name), () -> "the loader was not asked for " + name + ": " + names);
        }
        assertTrue(result.get("version").contains("OpenGL ES"), "GL_VERSION " + result.get("version"));
        assertFalse(result.get("vendor").isEmpty(), "GL_VENDOR");
        assertFalse(result.get("renderer").isEmpty(), "GL_RENDERER");
        assertTrue(Integer.parseInt(result.get("extensions.length")) > 0, "GL_EXTENSIONS");
        assertEquals("", result.get("mismatches"), "every table entry is what the loader answered");
        assertEquals("true", result.get("multisampleNull"), NOT_A_GLES_ENTRY_POINT + " stays NULL on GLES");
        assertTrue(Integer.parseInt(result.get("nulls")) <= 1, "at most that one NULL entry: " + result.get("nulls"));
        assertNotEquals(0L, Long.parseLong(result.get("throwing.adopted")), "a throwing loader still yields a context");
        assertEquals("true", result.get("throwing.allNull"), "and every entry of it is NULL");
        assertEquals("0", result.get("nullLoader"), "a null loader");
        assertEquals("0", result.get("noContext"), "no current context");
        assertEquals("true", result.get("done"));
    }

    @Test
    public void theDesktopLibraryAdoptsAMesaContext() throws IOException, InterruptedException {
        requireBuilt("prism_es2");
        EglNativeTest.requireALibEgl();
        Map<String, String> result = runChild("desktop", List.of());
        // The desktop table has the glXSwapIntervalSGI row as well: 51 loader calls.
        assertAdopted(result, "prism_es2", PROC_NAMES.size() + 1);
    }

    @Test
    public void theMonocleLibraryAdoptsAMesaContext() throws IOException, InterruptedException {
        requireBuilt("prism_es2_monocle");
        EglNativeTest.requireALibEgl();
        Map<String, String> result = runChild("monocle", List.of("-Dglass.platform=Monocle"));
        assertAdopted(result, "prism_es2_monocle", PROC_NAMES.size());
    }
}
