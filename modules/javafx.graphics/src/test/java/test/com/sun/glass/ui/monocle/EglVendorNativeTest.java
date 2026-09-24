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

import com.sun.glass.ui.monocle.AcceleratedScreen;
import com.sun.glass.ui.monocle.EglVendorShim;
import com.sun.glass.ui.monocle.GLException;
import com.sun.glass.ui.monocle.NativePlatform;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * The vendor bridge against a stub vendor library: {@code src/test/native/monocle/egl_vendor_stub.c} is
 * compiled with the host C compiler (a skip only when there is none on PATH; a compile failure fails) and a
 * child JVM per scenario, with {@code -Dmonocle.platform=EGL -Dmonocle.egl.lib=<stub>}, drives the EGL
 * platform through the public Monocle API and the shim: {@code NativePlatformFactory.getNativePlatform()}
 * (binds the 23 symbols), {@code createScreens} (two screens of the canned values), the accelerated screen
 * with Prism's seven-int {@code GLPixelFormat.Attributes} array, exactly as MonocleGLFactory passes it (the
 * seven-call sequence of the header), rendering on and off, {@code swapBuffers} under
 * {@code NativeScreen.framebufferSwapLock}, and the hardware cursor. The stub appends every call it sees, one
 * per line, to the file named by the environment variable {@code MONOCLE_EGL_STUB_LOG}, which this test sets
 * for the child; that file must then be exactly the call sequence the header documents, with the arguments
 * the Java passed. The {@code -DSTUB_CHOOSE_CONFIG_FAILS} build makes {@code doEglChooseConfig} answer -1,
 * which must be the IllegalArgumentException of the JNI era; a library file that does not exist and a build
 * with one symbol renamed away on the compiler command line ({@code -DdoGetDpi=...}, no C edit) must be the
 * two UnsatisfiedLinkErrors of {@code EglVendorNative}. The stub returns 1 for every {@code uint8_t}; the
 * reading of a 0 or another non-zero byte is asserted on every platform by EglVendorLayoutTest, the stub
 * having no such variant. The cursor is driven through {@code new EGLCursor()}, not through
 * {@code EGLPlatform.createCursor()}, so the {@code monocle.egl.swcursor} choice is not exercised here. The
 * hardware path (a DRM/GBM vendor library on a board) is not exercised here either. The class needs longer
 * than the default test timeout: a compile of up to 120 s and a child of up to 120 s per test.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(value = 600, unit = TimeUnit.SECONDS)
public class EglVendorNativeTest {

    static final String DISPLAY_ID = "/dev/dri/cardTEST";

    /** Prism's GLPixelFormat.Attributes: RED, GREEN, BLUE, ALPHA, DEPTH, DOUBLEBUFFER, ONSCREEN; no terminator. */
    static final int[] ATTRIBUTES = {8, 8, 8, 8, 0, 1, 1};
    static final int CURSOR_X = 10;
    static final int CURSOR_Y = 20;
    static final int IMAGE_BYTES = 16 * 16 * 4;

    /* The stub's canned handles, as it logs them: decimal. */
    private static final long WINDOW = 0x1234;
    private static final long DISPLAY = 0x5678;
    private static final long CONFIG = 0x9ABC;
    private static final long SURFACE = 0xDEF0;
    private static final long CONTEXT = 0x1357;
    private static final String OMITTED_SYMBOL = "doGetDpi";
    private static final String STUB_LOG_VARIABLE = "MONOCLE_EGL_STUB_LOG";

    private static final List<String> COMPILERS = List.of("cc", "gcc", "clang");

    /** The child: one scenario of the EGL platform against the stub, written as {@code key=value} lines. */
    public static final class Child {

        private static final List<String> LINES = new ArrayList<>();

        public static void main(String[] args) throws IOException {
            Path output = Path.of(args[0]);
            String scenario = args[1];
            try {
                switch (scenario) {
                    case "sequence" -> sequence();
                    case "choose-config-fails" -> chooseConfigFails();
                    case "load-failure" -> loadFailure();
                    default -> throw new IllegalArgumentException(scenario);
                }
            } catch (Throwable t) {
                LINES.add("failure=" + t);
                t.printStackTrace();
                Files.write(output, LINES, StandardCharsets.UTF_8);
                System.exit(2);
            }
            Files.write(output, LINES, StandardCharsets.UTF_8);
        }

        private static void sequence() throws InterruptedException, GLException {
            NativePlatform platform = NativePlatformFactory.getNativePlatform();
            LINES.add("platform=" + platform.getClass().getName());
            LINES.add("bound=" + String.join(";", EglVendorShim.boundSymbols()));
            List<String> screens = EglVendorShim.describeScreens(platform);
            LINES.add("screens=" + screens.size());
            for (int i = 0; i < screens.size(); i++) {
                LINES.add("screen" + i + "=" + screens.get(i));
            }
            AcceleratedScreen screen = platform.getAcceleratedScreen(ATTRIBUTES);
            screen.enableRendering(true);
            screen.enableRendering(false);
            LINES.add("swap=" + screen.swapBuffers());
            Thread swapper = new Thread(screen::swapBuffers, "swapper");
            synchronized (EglVendorShim.framebufferSwapLock()) {
                swapper.start();
                swapper.join(500);
                LINES.add("swap.blockedByLock=" + swapper.isAlive());
            }
            swapper.join(10_000);
            LINES.add("swap.releasedByLock=" + !swapper.isAlive());
            EglVendorShim.Cursor cursor = new EglVendorShim.Cursor();
            int[] size = cursor.bestSize();
            LINES.add("cursor.size=" + size[0] + "x" + size[1]);
            cursor.setVisibility(true);
            cursor.setLocation(CURSOR_X, CURSOR_Y);
            cursor.setImage(image());
            cursor.setVisibility(false);
            LINES.add("done=true");
        }

        private static void chooseConfigFails() throws GLException {
            NativePlatform platform = NativePlatformFactory.getNativePlatform();
            try {
                platform.getAcceleratedScreen(ATTRIBUTES);
                LINES.add("chooseConfig.error=none");
            } catch (RuntimeException e) {
                LINES.add("chooseConfig.error=" + e.getClass().getName() + ": " + e.getMessage());
            }
        }

        private static void loadFailure() {
            try {
                NativePlatformFactory.getNativePlatform();
                LINES.add("platform.error=none");
            } catch (Throwable t) {
                LINES.add("platform.error=" + t.getClass().getName() + ": " + t.getMessage());
            }
        }
    }

    /** The cursor image the child sends: every byte value, so that the stub's byte sum tells them apart. */
    static byte[] image() {
        byte[] image = new byte[IMAGE_BYTES];
        for (int i = 0; i < image.length; i++) {
            image[i] = (byte) (i * 7);
        }
        return image;
    }

    private static long imageSum() {
        long sum = 0;
        for (byte b : image()) {
            sum += b & 0xFF;
        }
        return sum;
    }

    private static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory);
        }
        return directory;
    }

    private static Path workDirectory() throws IOException {
        Path work = moduleDirectory().resolve("target").resolve("egl-vendor-native");
        Files.createDirectories(work);
        return work;
    }

    /** The first C compiler on PATH, or a skip. */
    private static String compiler() {
        String path = System.getenv("PATH");
        for (String directory : (path == null ? "" : path).split(":")) {
            if (directory.isEmpty()) {
                continue;
            }
            for (String name : COMPILERS) {
                if (Files.isExecutable(Path.of(directory, name))) {
                    return Path.of(directory, name).toString();
                }
            }
        }
        abort("no C compiler (" + COMPILERS + ") on PATH: the stub vendor library cannot be built");
        return null;
    }

    /** Compiles the stub with the given extra options into {@code target/egl-vendor-native/<name>.so}. */
    private static Path stub(String name, List<String> options) throws IOException, InterruptedException {
        Path library = workDirectory().resolve("lib" + name + ".so");
        Path source = moduleDirectory().resolve("src").resolve("test").resolve("native").resolve("monocle")
                .resolve("egl_vendor_stub.c");
        assertTrue(Files.isRegularFile(source), "stub source " + source);
        List<String> command = new ArrayList<>(List.of(compiler(), "-shared", "-fPIC"));
        command.addAll(options);
        command.addAll(List.of("-o", library.toString(), source.toString()));
        Path log = workDirectory().resolve(name + "-cc.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile())
                .start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("the C compiler did not finish: " + command);
        }
        if (process.exitValue() != 0 || !Files.isRegularFile(library)) {
            fail("the stub vendor library did not compile (exit " + process.exitValue() + "): " + command
                    + "\n" + tail(log));
        }
        return library;
    }

    /** One scenario in a child JVM: its {@code key=value} result lines, plus the stub's log under {@code stub.log}. */
    private static Map<String, String> runChild(String scenario, Path library)
            throws IOException, InterruptedException {
        Path work = workDirectory().resolve(scenario + (library == null ? "" : "-" + library.getFileName()));
        Files.createDirectories(work);
        Path output = work.resolve("result.txt");
        Path log = work.resolve("child.log");
        Path stubLog = work.resolve("stub.log");
        Path errors = work.resolve("hs_err");
        Files.deleteIfExists(output);
        Files.deleteIfExists(stubLog);
        Files.createDirectories(errors);
        clearStaleErrorFiles(errors);
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument.startsWith("-agentlib") || argument.startsWith("-javaagent")
                    || argument.startsWith("-Xrunjdwp") || argument.startsWith("-Xdebug")
                    || argument.startsWith("-XX:ErrorFile") || argument.startsWith("-Djavafx.toolkit")
                    || argument.startsWith("-Dglass.platform") || argument.startsWith("-Dprism.order")
                    || argument.startsWith("-Dmonocle.")) {
                continue;
            }
            command.add(argument);
        }
        command.add("-XX:ErrorFile=" + errors.resolve("hs_err_pid%p.log"));
        command.add("-Dglass.platform=Monocle");
        command.add("-Dmonocle.platform=EGL");
        command.add("-Dmonocle.egl.lib=" + library);
        command.add("-Degl.displayid=" + DISPLAY_ID);
        command.add("-Dprism.order=sw");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Child.class.getName());
        command.add(output.toString());
        command.add(scenario);
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(moduleDirectory().toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().put(STUB_LOG_VARIABLE, stubLog.toString());
        Process process = builder.start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
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
        System.out.println("[EglVendorNativeTest] " + scenario + " " + result);
        return result;
    }

    /** The calls the stub logged during the last {@link #runChild} of {@code scenario}, one per line. */
    private static List<String> stubLog(String scenario, Path library) throws IOException {
        Path stubLog = workDirectory().resolve(scenario + "-" + library.getFileName()).resolve("stub.log");
        assertTrue(Files.isRegularFile(stubLog), "the stub wrote its log to " + stubLog);
        List<String> calls = new ArrayList<>();
        for (String line : Files.readAllLines(stubLog, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                calls.add(line.strip());
            }
        }
        System.out.println("[EglVendorNativeTest] " + scenario + " stub log " + calls);
        return calls;
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

    /** The screen queries of {@code new EGLScreen(idx)}, in the order of the constructor. */
    private static List<String> screenCalls(int idx) {
        List<String> calls = new ArrayList<>();
        for (String name : List.of("doGetHandle", "doGetDepth", "doGetNativeFormat", "doGetWidth", "doGetHeight",
                "doGetOffsetX", "doGetOffsetY", "doGetDpi", "doGetScale")) {
            calls.add(name + "(" + idx + ")");
        }
        return calls;
    }

    /** The seven calls of the EGLAcceleratedScreen constructor; the stub logs the seven attributes it was given. */
    private static List<String> acceleratedScreenCalls() {
        StringBuilder attributes = new StringBuilder();
        for (int i = 0; i < ATTRIBUTES.length; i++) {
            attributes.append(i == 0 ? "" : ", ").append(ATTRIBUTES[i]);
        }
        return List.of("getNativeWindowHandle(" + DISPLAY_ID + ")", "getEglDisplayHandle()",
                "doEglInitialize(" + DISPLAY + ")", "doEglBindApi(" + EglVendorShim.EGL_OPENGL_ES_API + ")",
                "doEglChooseConfig(" + DISPLAY + ", [" + attributes + "])",
                "doEglCreateWindowSurface(" + DISPLAY + ", " + CONFIG + ", " + WINDOW + ")",
                "doEglCreateContext(" + DISPLAY + ", " + CONFIG + ")");
    }

    @Test
    public void thePlatformDrivesTheStubInTheOrderOfTheHeader() throws IOException, InterruptedException {
        Path library = stub("monocle_egl_stub", List.of());
        Map<String, String> result = runChild("sequence", library);
        assertEquals("com.sun.glass.ui.monocle.EGLPlatform", result.get("platform"));
        assertEquals(String.join(";", EglVendorShim.contract().keySet()), result.get("bound"),
                "the 23 symbols, bound in the order of the header");
        assertEquals("2", result.get("screens"), "doGetNumberOfScreens");
        assertEquals("handle=1 depth=32 format=1 width=800 height=480 offsetX=0 offsetY=0 dpi=96 scale=1.0",
                result.get("screen0"));
        assertEquals("handle=2 depth=32 format=1 width=800 height=480 offsetX=0 offsetY=0 dpi=96 scale=1.0",
                result.get("screen1"));
        assertEquals("true", result.get("swap"), "doEglSwapBuffers answered 1");
        assertEquals("true", result.get("swap.blockedByLock"), "swapBuffers waits for framebufferSwapLock");
        assertEquals("true", result.get("swap.releasedByLock"));
        assertEquals("16x16", result.get("cursor.size"));
        assertEquals("true", result.get("done"));

        List<String> expected = new ArrayList<>();
        expected.add("doGetNumberOfScreens()");
        expected.addAll(screenCalls(0));
        expected.addAll(screenCalls(1));
        expected.addAll(acceleratedScreenCalls());
        expected.add("doEglMakeCurrent(" + DISPLAY + ", " + SURFACE + ", " + SURFACE + ", " + CONTEXT + ")");
        expected.add("doEglMakeCurrent(" + DISPLAY + ", 0, 0, " + CONTEXT + ")");
        expected.add("doEglSwapBuffers(" + DISPLAY + ", " + SURFACE + ")");
        expected.add("doEglSwapBuffers(" + DISPLAY + ", " + SURFACE + ")");
        expected.add("doInitCursor(16, 16)");
        expected.add("doSetCursorVisibility(1)");
        expected.add("doSetLocation(" + CURSOR_X + ", " + CURSOR_Y + ")");
        expected.add("doSetCursorImage(img, " + IMAGE_BYTES + ", sum=" + imageSum() + ")");
        expected.add("doSetCursorVisibility(0)");
        assertEquals(expected, stubLog("sequence", library));
    }

    @Test
    public void aChooseConfigOfMinusOneIsTheIllegalArgumentExceptionOfTheJni()
            throws IOException, InterruptedException {
        Path library = stub("monocle_egl_stub_fail", List.of("-DSTUB_CHOOSE_CONFIG_FAILS"));
        Map<String, String> result = runChild("choose-config-fails", library);
        assertEquals("java.lang.IllegalArgumentException: Could not create an EGLChooseConfig",
                result.get("chooseConfig.error"));
        assertEquals(acceleratedScreenCalls().subList(0, 5), stubLog("choose-config-fails", library),
                "the sequence stops at doEglChooseConfig");
    }

    @Test
    public void aLibraryThatCannotBeOpenedIsTheUnsatisfiedLinkErrorOfEglPlatform()
            throws IOException, InterruptedException {
        Path missing = workDirectory().resolve("libmonocle_egl_missing.so");
        Files.deleteIfExists(missing);
        Map<String, String> result = runChild("load-failure", missing);
        assertEquals("java.lang.UnsatisfiedLinkError: EGLPlatform failed to load the requested library " + missing,
                result.get("platform.error"));
    }

    @Test
    public void aLibraryMissingOneSymbolIsAnUnsatisfiedLinkErrorNamingIt()
            throws IOException, InterruptedException {
        Path library = stub("monocle_egl_stub_omit", List.of("-D" + OMITTED_SYMBOL + "=stubOmitted" + OMITTED_SYMBOL));
        Map<String, String> result = runChild("load-failure", library);
        String error = result.get("platform.error");
        assertTrue(error.startsWith("java.lang.UnsatisfiedLinkError: "), error);
        assertTrue(error.contains(" does not export " + OMITTED_SYMBOL + ","), error);
        assertTrue(error.contains(library.toString()), error);
    }
}
