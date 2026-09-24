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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.gtk.GtkGlassShim;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

/**
 * The child JVM of the GTK glass binding tests: starts the real JavaFX toolkit - Quantum over
 * {@code com.sun.glass.ui.gtk} on the X11 display in {@code DISPLAY} - runs one scenario method and writes the
 * values it recorded to a file. It is not a test class and has no test of its own.
 * <p>
 * Usage: {@code GtkGlassChild <class>#<method> <output file>}. The method is a {@code static} method of a test
 * class taking a {@code Map<String, String>}; it runs on the main thread of this JVM with the toolkit up, and
 * reaches the FX thread through {@link #onFx}. The map is written as {@code key=value} lines (see
 * {@link #escape}) even when the scenario throws, and the JVM then exits with status 2. With
 * {@code -Dgtk.glass.child.toolkit=false} ({@link #TOOLKIT_PROPERTY}) the toolkit is not started: the scenario runs
 * without it, or starts it itself.
 */
public final class GtkGlassChild {

    /** {@code false} starts no toolkit before the scenario runs; default {@code true}. */
    static final String TOOLKIT_PROPERTY = "gtk.glass.child.toolkit";

    /** Every {@code Throwable} the FX thread's uncaught-exception handler received, in order. */
    static final List<Report> REPORTS = new CopyOnWriteArrayList<>();

    /** One call of the FX thread's uncaught-exception handler. */
    record Report(Thread thread, Throwable throwable, boolean onEventThread) {
    }

    private GtkGlassChild() {
    }

    public static void main(String[] args) {
        int status = 0;
        Map<String, String> values = new TreeMap<>();
        try {
            String target = args[0];
            int hash = target.indexOf('#');
            Method scenario = Class.forName(target.substring(0, hash))
                    .getDeclaredMethod(target.substring(hash + 1), Map.class);
            scenario.setAccessible(true);
            if (Boolean.parseBoolean(System.getProperty(TOOLKIT_PROPERTY, "true"))) {
                CountDownLatch started = new CountDownLatch(1);
                Platform.startup(started::countDown);
                if (!started.await(60, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("the toolkit did not start within 60 s");
                }
                onFx(() -> {
                    Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) ->
                            REPORTS.add(new Report(thread, throwable, Application.isEventThread())));
                    return null;
                });
            }
            try {
                scenario.invoke(null, values);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            status = 2;
        } finally {
            try {
                write(Path.of(args[1]), values);
            } catch (IOException e) {
                e.printStackTrace();
                status = 3;
            }
        }
        System.out.flush();
        System.err.flush();
        Runtime.getRuntime().halt(status);
    }

    /** Runs {@code body} on the FX thread and returns its result; rethrows what it threw. */
    static <T> T onFx(Callable<T> body) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(body.call());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("the FX thread did not run the task within 30 s");
        }
        Throwable t = failure.get();
        if (t instanceof Exception e) {
            throw e;
        }
        if (t instanceof Error e) {
            throw e;
        }
        return result.get();
    }

    /**
     * Records, under {@code env.*}, what the environment-bound expectations of these tests depend on (see
     * {@link GtkGlassChildJvm#requireEnvironment}): the GTK version, the primary screen's size and output scale, the
     * window manager GDK sees ({@code unknown} for none), compositing, the depth of the system visual, the screen
     * resolution and the X server's vendor and release.
     */
    static void recordEnvironment(Map<String, String> out) throws Exception {
        onFx(() -> {
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getBounds();
            out.put("env.gtk", GtkGlassShim.gtkVersion());
            out.put("env.screen", (int) bounds.getWidth() + "x" + (int) bounds.getHeight());
            out.put("env.scale", screen.getOutputScaleX() + "x" + screen.getOutputScaleY());
            out.put("env.windowManager", GtkGlassShim.windowManagerName());
            out.put("env.compositing", GtkGlassShim.compositing());
            out.put("env.depth", Integer.toString(GtkGlassShim.systemVisualDepth()));
            out.put("env.resolution", Double.toString(GtkGlassShim.screenResolution()));
            out.put("env.xserver", GtkGlassShim.xServer());
            return null;
        });
    }

    /** Polls {@code condition} every 5 ms for at most {@code timeoutMillis}; answers its last value. */
    static boolean waitFor(long timeoutMillis, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                return condition.getAsBoolean();
            }
            Thread.sleep(5);
        }
        return true;
    }

    static void write(Path output, Map<String, String> values) throws IOException {
        StringWriter text = new StringWriter();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            text.append(entry.getKey()).append('=').append(escape(entry.getValue())).append('\n');
        }
        Files.writeString(output, text.toString(), StandardCharsets.UTF_8);
    }

    /** Doubles a backslash, writes a non-printable or non-ASCII char as a hex escape, {@code null} as a lone escape. */
    static String escape(String value) {
        if (value == null) {
            return "\\0";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c >= 0x20 && c < 0x7F) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
    }

    /** The inverse of {@link #escape}. */
    static String unescape(String text) {
        if (text.equals("\\0")) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\') {
                sb.append(c);
            } else if (text.charAt(i + 1) == '\\') {
                sb.append('\\');
                i++;
            } else {
                sb.append((char) Integer.parseInt(text.substring(i + 2, i + 6), 16));
                i += 5;
            }
        }
        return sb.toString();
    }
}
