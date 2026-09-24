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
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Frame uploads through the GTK glass view: {@code View.uploadPixels} of a full frame (default 1920x1080, the
 * private display's screen) held in an {@code int[]} ({@code GtkView._uploadPixelsIntArray}), in a {@code byte[]}
 * ({@code _uploadPixelsByteArray}) and in a direct buffer ({@code _uploadPixelsDirect}), into a shown window on the
 * X11 display of {@code DISPLAY}. Each is {@code WindowContextBase::paint} of {@code glass_window.cpp} from the
 * caller's memory; what differs between builds is how Java reaches it and hands over the array - JNI with
 * {@code GetPrimitiveArrayCritical}, or a downcall.
 * <p>
 * Per kind, after one warm-up round, {@code -Dgtk.upload.rounds} rounds (default 9) of {@code -Dgtk.upload.frames}
 * uploads (default 30) on the FX thread, each round ended by an {@code XSync} of GDK's connection so that the X
 * server has taken every frame; recorded are the medians of the wall time and of the FX thread's CPU time per
 * frame. Then one frame of a colour of its own per kind, read back with the robot at the view's centre: a kind
 * whose upload painted nothing would show as a colour that is not its own. A measurement, not a pass/fail test,
 * except that every kind must paint its colour and every round must take time.
 * <p>
 * Then, for the two array kinds, the bounds the C checks ({@code ggtk_view_upload_pixels_int} / {@code _byte} of
 * {@code GlassView.cpp}), through {@code GtkView._uploadPixelsIntArray} and {@code _uploadPixelsByteArray}
 * themselves: a frame at an offset into its array paints from that offset, and a frame that does not fit its array
 * - one element short, one past the end, a negative offset, a width of 0 - paints nothing, so the frame before it
 * stays on the screen. Where the array is copied into a native block before the paint, that block holds the largest
 * frame and goes with the view.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkUploadBenchmarkTest {

    static final String ROUNDS_PROPERTY = "gtk.upload.rounds";
    static final String FRAMES_PROPERTY = "gtk.upload.frames";
    static final String WIDTH_PROPERTY = "gtk.upload.width";
    static final String HEIGHT_PROPERTY = "gtk.upload.height";

    /** The kinds of frame, and the ARGB colour each paints for the read-back. */
    static final List<String> KINDS = List.of("int", "byte", "direct");
    static final int[] CHECK_COLORS = {0xFF20A040, 0xFFC03010, 0xFF1060E0};

    /** The kinds whose frames are Java arrays, and the colour of each one's frame at an offset. */
    static final List<String> ARRAY_KINDS = List.of("int", "byte");
    static final int[] BOUNDS_COLORS = {0xFF3080B0, 0xFFB08030};

    /** What precedes a frame at an offset in its array, and the colour of every frame the C must refuse. */
    static final int PREFIX_COLOR = 0xFFFF00FF;
    static final int REFUSED_COLOR = 0xFF000000;

    /** The offset of the frame in its array, in pixels. */
    static final int OFFSET_PIXELS = 7;

    static final List<String> REFUSED_CASES = List.of("oneShort", "onePastTheEnd", "negativeOffset", "zeroWidth");

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(2 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runUploads() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkUploadBenchmarkTest.class, "uploadScenario", List.of(
                "-D" + ROUNDS_PROPERTY + "=" + Integer.getInteger(ROUNDS_PROPERTY, 9),
                "-D" + FRAMES_PROPERTY + "=" + Integer.getInteger(FRAMES_PROPERTY, 30),
                "-D" + WIDTH_PROPERTY + "=" + Integer.getInteger(WIDTH_PROPERTY, 1920),
                "-D" + HEIGHT_PROPERTY + "=" + Integer.getInteger(HEIGHT_PROPERTY, 1080)));
        System.out.println("GTK upload benchmark (" + System.getProperty("java.vm.name") + " "
                + System.getProperty("java.version") + ")");
        for (Map.Entry<String, String> entry : run.values().entrySet()) {
            if (entry.getKey().startsWith("upload.")) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    @Test
    public void everyKindPaintsItsFrame() {
        for (int k = 0; k < KINDS.size(); k++) {
            String kind = KINDS.get(k);
            assertEquals(String.format(Locale.ROOT, "%06x", CHECK_COLORS[k] & 0xFFFFFF),
                    run.values().get("upload." + kind + ".readBack"), kind + ": " + run.describe());
        }
    }

    /** A frame at an offset into its array is painted from that offset: no pixel of what precedes it shows. */
    @Test
    public void aFrameAtAnOffsetIsPaintedFromThatOffset() {
        for (String kind : ARRAY_KINDS) {
            String colour = hex(BOUNDS_COLORS[ARRAY_KINDS.indexOf(kind)]);
            assertEquals(colour + " " + colour + " " + colour, run.values().get("bounds." + kind + ".atOffset"),
                    kind + ": " + run.describe());
        }
    }

    /**
     * A frame that does not fit its array is refused by the C before it paints: the frame before it stays on the
     * screen.
     */
    @Test
    public void aFrameOutsideItsArrayPaintsNothing() {
        for (String kind : ARRAY_KINDS) {
            String colour = hex(BOUNDS_COLORS[ARRAY_KINDS.indexOf(kind)]);
            for (String refused : REFUSED_CASES) {
                assertEquals(colour, run.values().get("bounds." + kind + "." + refused), kind + " " + refused + ": "
                        + run.describe());
            }
        }
    }

    /**
     * Where the frames of an array are copied into a native block of the view before the paint
     * ({@code GtkGlassNative.UploadStaging}), the block holds exactly the largest frame - not the array it came
     * from - and goes when the view closes.
     */
    @Test
    public void theNativeCopyOfAFrameGoesWithItsView() {
        String[] frame = run.values().get("upload.frame").split("x");
        long bytes = 4L * Integer.parseInt(frame[0]) * Integer.parseInt(frame[1]);
        assertEquals(Long.toString(bytes), run.values().get("upload.staging.afterFrames"), run.describe());
        assertEquals("0", run.values().get("upload.staging.afterClose"), run.describe());
    }

    @Test
    public void everyRoundTakesTime() {
        int rounds = Integer.parseInt(run.values().get("upload.rounds"));
        for (String kind : KINDS) {
            for (int i = 1; i <= rounds; i++) {
                String round = run.values().get("upload." + kind + ".round." + i);
                assertTrue(round != null && round.startsWith("msPerFrame=") && !round.startsWith("msPerFrame=0.000 "),
                        kind + " round " + i + ": " + round);
            }
            assertTrue(Double.parseDouble(run.values().get("upload." + kind + ".median.msPerFrame")) > 0,
                    run.describe());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The measurement (child JVM)
    // ---------------------------------------------------------------------------------------------

    /** Runs in {@link GtkGlassChild}. */
    static void uploadScenario(Map<String, String> out) throws Exception {
        int rounds = Integer.getInteger(ROUNDS_PROPERTY, 9);
        int frames = Integer.getInteger(FRAMES_PROPERTY, 30);
        int width = Integer.getInteger(WIDTH_PROPERTY, 1920);
        int height = Integer.getInteger(HEIGHT_PROPERTY, 1080);
        out.put("upload.rounds", Integer.toString(rounds));
        out.put("upload.frame", width + "x" + height);
        View view = GtkGlassChild.onFx(() -> {
            Window w = Application.GetApplication().createWindow(null, Screen.getMainScreen(), Window.UNTITLED);
            View v = Application.GetApplication().createView();
            w.setView(v);
            w.setBounds(0, 0, true, true, -1, -1, width, height, 0, 0);
            w.setVisible(true);
            return v;
        });
        try {
            // mapped and exposed before the first frame
            GtkGlassChild.onFx(() -> {
                GtkGlassShim.syncGdkDisplay();
                return null;
            });
            Thread.sleep(500);
            ThreadMXBean threads = ManagementFactory.getThreadMXBean();
            for (int k = 0; k < KINDS.size(); k++) {
                String kind = KINDS.get(k);
                Pixels frame = GtkGlassChild.onFx(() -> pixels(kind, width, height, 0xFF336699));
                round(view, frame, frames, threads);
                double[] ms = new double[rounds];
                double[] cpu = new double[rounds];
                for (int i = 0; i < rounds; i++) {
                    double[] round = round(view, frame, frames, threads);
                    ms[i] = round[0];
                    cpu[i] = round[1];
                    out.put("upload." + kind + ".round." + (i + 1), format("msPerFrame=%.3f fxCpuMsPerFrame=%.3f",
                            round[0], round[1]));
                }
                out.put("upload." + kind + ".median.msPerFrame", format("%.3f", median(ms)));
                out.put("upload." + kind + ".median.fxCpuMsPerFrame", format("%.3f", median(cpu)));
                Pixels check = GtkGlassChild.onFx(() -> pixels(kind, width, height, CHECK_COLORS[KINDS.indexOf(kind)]));
                out.put("upload." + kind + ".readBack", GtkGlassChild.onFx(() -> {
                    view.uploadPixels(check);
                    GtkGlassShim.syncGdkDisplay();
                    GlassRobot robot = Application.GetApplication().createRobot();
                    Color color = robot.getPixelColor(view.getWindow().getX() + view.getX() + view.getWidth() / 2,
                            view.getWindow().getY() + view.getY() + view.getHeight() / 2);
                    return String.format(Locale.ROOT, "%02x%02x%02x", channel(color.getRed()),
                            channel(color.getGreen()), channel(color.getBlue()));
                }));
            }
            for (String kind : ARRAY_KINDS) {
                bounds(view, kind, width, height, out);
            }
            out.put("upload.staging.afterFrames", Long.toString(GtkGlassChild.onFx(() ->
                    GtkGlassShim.uploadStagingBytes(view))));
        } finally {
            GtkGlassChild.onFx(() -> {
                view.getWindow().close();
                return null;
            });
        }
        out.put("upload.staging.afterClose", Long.toString(GtkGlassChild.onFx(() ->
                GtkGlassShim.uploadStagingBytes(view))));
    }

    /**
     * The bounds cases of {@code kind} ({@code int} or {@code byte}), through the private upload natives of
     * {@code GtkView}: a frame at {@link #OFFSET_PIXELS} into an array that ends where it ends (the largest frame the
     * C accepts), read back at the view's first pixel, its seventh and its centre; then each frame of
     * {@link #REFUSED_CASES} in {@link #REFUSED_COLOR}, read back at the centre.
     */
    private static void bounds(View view, String kind, int width, int height, Map<String, String> out)
            throws Exception {
        int colour = BOUNDS_COLORS[ARRAY_KINDS.indexOf(kind)];
        int pixels = width * height;
        GtkGlassChild.onFx(() -> {
            upload(view, kind, array(kind, OFFSET_PIXELS, pixels, colour), OFFSET_PIXELS, width, height);
            return null;
        });
        out.put("bounds." + kind + ".atOffset", readBack(view, 0, 0) + " " + readBack(view, OFFSET_PIXELS - 1, 0)
                + " " + readBack(view, width / 2, height / 2));
        for (String refused : REFUSED_CASES) {
            GtkGlassChild.onFx(() -> {
                switch (refused) {
                    case "oneShort" -> upload(view, kind, array(kind, 0, pixels - 1, REFUSED_COLOR), 0, width, height);
                    case "onePastTheEnd" -> upload(view, kind, array(kind, OFFSET_PIXELS, pixels, REFUSED_COLOR),
                            OFFSET_PIXELS + 1, width, height);
                    case "negativeOffset" -> upload(view, kind, array(kind, 0, pixels, REFUSED_COLOR), -1, width,
                            height);
                    case "zeroWidth" -> upload(view, kind, array(kind, 0, pixels, REFUSED_COLOR), 0, 0, height);
                    default -> throw new IllegalArgumentException(refused);
                }
                return null;
            });
            out.put("bounds." + kind + "." + refused, readBack(view, width / 2, height / 2));
        }
    }

    /**
     * An array of {@code kind} holding {@code prefix} pixels of {@link #PREFIX_COLOR}, then {@code pixels} pixels of
     * {@code argb}: {@code int}s, or bytes in the B, G, R, A order of {@code BYTE_BGRA_PRE}.
     */
    private static Object array(String kind, int prefix, int pixels, int argb) {
        int[] ints = new int[prefix + pixels];
        Arrays.fill(ints, 0, prefix, PREFIX_COLOR);
        Arrays.fill(ints, prefix, ints.length, argb);
        if (kind.equals("int")) {
            return ints;
        }
        ByteBuffer bytes = ByteBuffer.allocate(4 * ints.length).order(ByteOrder.LITTLE_ENDIAN);
        bytes.asIntBuffer().put(ints);
        return bytes.array();
    }

    /**
     * {@code GtkView._uploadPixelsIntArray} or {@code _uploadPixelsByteArray} of {@code array} with the offset in
     * pixels turned into the array's elements; FX thread.
     */
    private static void upload(View view, String kind, Object array, int offsetPixels, int width, int height) {
        if (kind.equals("int")) {
            GtkGlassShim.uploadPixelsIntArray(view, (int[]) array, offsetPixels, width, height);
        } else {
            GtkGlassShim.uploadPixelsByteArray(view, (byte[]) array, 4 * offsetPixels, width, height);
        }
    }

    /** The colour at {@code x, y} of the view, read with the robot after an {@code XSync}, as {@code rrggbb}. */
    private static String readBack(View view, int x, int y) throws Exception {
        return GtkGlassChild.onFx(() -> {
            GtkGlassShim.syncGdkDisplay();
            GlassRobot robot = Application.GetApplication().createRobot();
            Color color = robot.getPixelColor(view.getWindow().getX() + view.getX() + x,
                    view.getWindow().getY() + view.getY() + y);
            return String.format(Locale.ROOT, "%02x%02x%02x", channel(color.getRed()), channel(color.getGreen()),
                    channel(color.getBlue()));
        });
    }

    private static String hex(int argb) {
        return String.format(Locale.ROOT, "%06x", argb & 0xFFFFFF);
    }

    /**
     * One round on the FX thread: {@code frames} uploads of {@code frame}, then {@code XSync}; the wall and FX-thread
     * CPU milliseconds per frame.
     */
    private static double[] round(View view, Pixels frame, int frames, ThreadMXBean threads) throws Exception {
        return GtkGlassChild.onFx(() -> {
            long cpu = threads.getCurrentThreadCpuTime();
            long start = System.nanoTime();
            for (int i = 0; i < frames; i++) {
                view.uploadPixels(frame);
            }
            GtkGlassShim.syncGdkDisplay();
            long wall = System.nanoTime() - start;
            long cpuTime = threads.getCurrentThreadCpuTime() - cpu;
            return new double[] {wall / 1e6 / frames, cpuTime / 1e6 / frames};
        });
    }

    /**
     * A frame of {@code argb} (premultiplied, opaque) held as {@code kind} says: an {@code IntBuffer} over an
     * {@code int[]}, a {@code ByteBuffer} over a {@code byte[]} in the B, G, R, A order of {@code BYTE_BGRA_PRE}, or
     * a direct {@code ByteBuffer} in that order.
     */
    private static Pixels pixels(String kind, int width, int height, int argb) {
        Application app = Application.GetApplication();
        return switch (kind) {
            case "int" -> {
                int[] data = new int[width * height];
                Arrays.fill(data, argb);
                yield app.createPixels(width, height, IntBuffer.wrap(data));
            }
            case "byte" -> app.createPixels(width, height, fill(ByteBuffer.wrap(new byte[4 * width * height]), argb));
            case "direct" -> app.createPixels(width, height, fill(ByteBuffer.allocateDirect(4 * width * height), argb));
            default -> throw new IllegalArgumentException(kind);
        };
    }

    private static ByteBuffer fill(ByteBuffer buffer, int argb) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.hasRemaining()) {
            buffer.putInt(argb);
        }
        return buffer.rewind();
    }

    private static int channel(double value) {
        return (int) Math.round(value * 255);
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted.length % 2 == 1 ? sorted[sorted.length / 2]
                : (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2;
    }

    private static String format(String pattern, Object... values) {
        return String.format(Locale.ROOT, pattern, values);
    }
}
