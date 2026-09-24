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

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The toolkit's startup on a machine without {@code libXtst.so.6}, which GTK 3 does not depend on. At commit
 * {@code 033187ad90} {@code libglassgtk3.so} needed that library, so loading {@code glassgtk3} in the
 * {@code GtkApplication} constructor threw {@link UnsatisfiedLinkError} and {@code Platform.startup} failed at once.
 * {@code libglassgtk3.so} no longer needs it, and the robot binds it from Java; the constructor initializes that
 * binding right after the load, so the startup must still fail there, the same way, before any thread of the
 * toolkit has started - not leave the invoke-later thread dead and the startup runnable never run.
 * <p>
 * The child JVM gets a copy of {@code GtkGlassNative} whose {@code libXtst.so.6} reads {@value #ABSENT_XTST}, through
 * {@code --patch-module}; it needs the X11 display of {@code DISPLAY}, which the constructor opens before the load.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkGlassNativeLoadFailureTest {

    /** A soname of the same length as {@code libXtst.so.6} that no system has. */
    static final String ABSENT_XTST = "libXjfx.so.6";

    private static final String FACADE = "com/sun/glass/ui/gtk/GtkGlassNative.class";

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() throws IOException {
        GtkGlassChildJvm.requireDisplay();
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            assertFalse(argument.startsWith("--patch-module"), "this JVM already patches a module: " + argument);
        }
        Path patch = Path.of("target", "gtk-glass-child", "GtkGlassNative-without-libXtst").toAbsolutePath();
        Path classFile = patch.resolve(FACADE);
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, renameUtf8Constant(facadeClassFile(), "libXtst.so.6", ABSENT_XTST));
        run = GtkGlassChildJvm.runWithoutToolkit(GtkGlassNativeLoadFailureTest.class, "startupScenario",
                List.of("--patch-module", "javafx.graphics=" + patch), true);
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    @Test
    public void startupThrowsTheMissingLibraryFromTheApplicationConstructor() {
        assertEquals("java.lang.RuntimeException", value("startup.threw"), run::describe);
        assertEquals("java.lang.UnsatisfiedLinkError", value("startup.cause"));
        assertTrue(value("startup.causeMessage").contains(ABSENT_XTST), value("startup.causeMessage"));
        assertEquals("true", value("startup.causeInApplicationConstructor"), value("startup.causeStack"));
    }

    @Test
    public void noToolkitThreadStartedAndTheRunnableNeverRan() {
        assertEquals("false", value("startup.runnableRan"));
        assertEquals("false", value("threads.invokeLaterDispatcher"));
        String stderr = new String(run.stderrBytes(), StandardCharsets.ISO_8859_1);
        assertFalse(stderr.contains("Exception in thread"), stderr);
    }

    /** Runs in {@link GtkGlassChild} without a toolkit: the scenario starts it. */
    static void startupScenario(Map<String, String> out) throws InterruptedException {
        CountDownLatch ran = new CountDownLatch(1);
        try {
            Platform.startup(ran::countDown);
            out.put("startup.threw", "nothing");
        } catch (Throwable t) {
            out.put("startup.threw", t.getClass().getName());
            Throwable cause = t.getCause();
            out.put("startup.cause", cause == null ? "none" : cause.getClass().getName());
            out.put("startup.causeMessage", cause == null ? "none" : String.valueOf(cause.getMessage()));
            boolean inConstructor = false;
            StringBuilder stack = new StringBuilder();
            for (StackTraceElement frame : cause == null ? new StackTraceElement[0] : cause.getStackTrace()) {
                stack.append(frame).append('\n');
                inConstructor |= frame.getClassName().equals("com.sun.glass.ui.gtk.GtkApplication")
                        && frame.getMethodName().equals("<init>");
            }
            out.put("startup.causeInApplicationConstructor", Boolean.toString(inConstructor));
            out.put("startup.causeStack", stack.toString());
        }
        out.put("startup.runnableRan", Boolean.toString(ran.await(5, TimeUnit.SECONDS)));
        out.put("threads.invokeLaterDispatcher", Boolean.toString(Thread.getAllStackTraces().keySet().stream()
                .anyMatch(thread -> thread.getName().equals("InvokeLaterDispatcher"))));
    }

    private static byte[] facadeClassFile() throws IOException {
        Module graphics = ModuleLayer.boot().findModule("javafx.graphics").orElseThrow();
        try (InputStream in = graphics.getResourceAsStream(FACADE)) {
            assertTrue(in != null, "javafx.graphics has no " + FACADE);
            return in.readAllBytes();
        }
    }

    /**
     * {@code classFile} with its one {@code CONSTANT_Utf8} entry {@code from} replaced by {@code to}, which has the
     * same length, so that no other byte of the class file moves.
     */
    static byte[] renameUtf8Constant(byte[] classFile, String from, String to) {
        byte[] target = utf8Entry(from);
        byte[] replacement = utf8Entry(to);
        assertEquals(target.length, replacement.length, "the replacement must keep the entry's length");
        byte[] result = classFile.clone();
        int found = -1;
        for (int i = 0; i + target.length <= result.length; i++) {
            if (Arrays.equals(result, i, i + target.length, target, 0, target.length)) {
                assertEquals(-1, found, "more than one " + from + " entry");
                found = i;
            }
        }
        assertTrue(found >= 0, "no CONSTANT_Utf8 " + from + " in the class file");
        System.arraycopy(replacement, 0, result, found, replacement.length);
        return result;
    }

    private static byte[] utf8Entry(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        byte[] entry = new byte[3 + bytes.length];
        entry[0] = 1;
        entry[1] = (byte) (bytes.length >> 8);
        entry[2] = (byte) bytes.length;
        System.arraycopy(bytes, 0, entry, 3, bytes.length);
        return entry;
    }
}
