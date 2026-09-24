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
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GtkPixels._attachInt} / {@code _attachByte} and {@code GtkCursor._createCursor} / {@code _getBestSize} on a
 * running toolkit: what {@code GlassPixels.cpp} and {@code GlassCursor.cpp} did at commit {@code 033187ad90}, pinned
 * so that the {@code java.lang.foreign} replacement can be held to it.
 * <ul>
 * <li>the {@code GdkPixbuf} an attach produces, byte for byte: every pixel read as a native-order {@code int} and
 * written as R, G, B, A = {@code >>16}, {@code >>8}, {@code >>0}, {@code >>24} (no unpremultiply), 8-bit RGBA with
 * rowstride {@code w * 4}; heap arrays start at the offset, direct buffers at their base address whatever their
 * position; and every early return of the C (null pointer, no data, negative offset, empty size, the overflow guard,
 * too few elements, a heap buffer passed without its array) leaves the slot untouched;</li>
 * <li>a custom cursor is a pixmap cursor on the default display; when {@code Pixels.attachData} throws (a read-only
 * buffer) or the pixels are {@code null}, the exception is reported, the cursor is 0 and GLib complains about the
 * {@code g_object_unref(NULL)} that followed;</li>
 * <li>the best cursor size is {@code gdk_display_get_default_cursor_size} in both dimensions, whatever was asked;</li>
 * <li>{@code Pixels.attachData} still works when the remaining JNI C calls it: a stage icon reaches the X11 window's
 * {@code _NET_WM_ICON} through {@code GlassWindow.cpp}'s {@code _setIcon}.</li>
 * </ul>
 * All of it runs in {@link GtkGlassChild} on the X11 display of {@code DISPLAY}. The icon digest holds only for the
 * GTK version and scale it was captured with, and is checked only where
 * {@link GtkGlassChildJvm#requireEnvironment} finds them.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkPixelsCursorNativeTest {

    static final int[] INTS = {0x00000000, 0x01020304, 0xFE7F8081, 0xFFFFFFFF, 0x80000000, 0x7FFFFFFF, 0xDEADBEEF,
                               0x12345678};

    static final byte[] BYTES = new byte[20];
    static {
        for (int i = 0; i < BYTES.length; i++) {
            BYTES[i] = (byte) (i * 37 + 11);
        }
    }

    /**
     * The {@code _NET_WM_ICON} the JNI build of commit {@code 033187ad90} produced for {@link #iconImage()} (scaled
     * to 128x128 by Quantum), captured on Xvfb 21.1.22 with GTK 3.24.52.
     */
    static final String NET_WM_ICON =
            "items=16386 w=128 h=128 sha256=591be0f707c129a28b5802577505268d0ed673da676dc91d3e89a6ed5cea6509";

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkPixelsCursorNativeTest.class, "pixelsCursorScenario", List.of());
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    /** The pixbuf description {@code GtkGlassShim.describePixbuf} gives for {@code w x h} pixels. */
    static String expected(int w, int h, int[] pixels) {
        byte[] bytes = new byte[w * h * 4];
        for (int i = 0; i < w * h; i++) {
            int v = pixels[i];
            bytes[4 * i] = (byte) (v >> 16);
            bytes[4 * i + 1] = (byte) (v >> 8);
            bytes[4 * i + 2] = (byte) v;
            bytes[4 * i + 3] = (byte) (v >> 24);
        }
        return "w=" + w + " h=" + h + " stride=" + (w * 4) + " channels=4 alpha=1 bps=8 colorspace=0 bytes="
                + HexFormat.of().formatHex(bytes);
    }

    static int[] ints(int from, int count) {
        int[] result = new int[count];
        System.arraycopy(INTS, from, result, 0, count);
        return result;
    }

    /** {@code count} native-order ints read from {@link #BYTES} at {@code from}. */
    static int[] intsOfBytes(byte[] bytes, int from, int count) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = buffer.getInt(from + 4 * i);
        }
        return result;
    }

    @Test
    public void intAttachProducesThePermutedPixbuf() {
        assertEquals(expected(3, 2, ints(2, 6)), value("attach.int.heap.3x2.offset2"));
        assertEquals(expected(2, 2, ints(4, 4)), value("attach.int.heap.2x2.exactBounds"));
        assertEquals(expected(2, 2, ints(0, 4)), value("attach.int.direct.2x2.positionIgnored"));
    }

    @Test
    public void byteAttachReadsNativeOrderInts() {
        assertEquals(expected(2, 2, intsOfBytes(BYTES, 3, 4)), value("attach.byte.heap.2x2.offset3"));
        assertEquals(expected(2, 2, intsOfBytes(BYTES, 4, 4)), value("attach.byte.heap.2x2.exactBounds"));
        byte[] direct = new byte[8];
        System.arraycopy(BYTES, 0, direct, 0, 8);
        assertEquals(expected(1, 2, intsOfBytes(direct, 0, 2)), value("attach.byte.direct.1x2.positionIgnored"));
    }

    @Test
    public void everyEarlyReturnLeavesTheSlotUntouched() {
        for (String key : List.of("attach.int.heap.tooFew", "attach.int.heapBufferWithoutArray", "attach.int.noData",
                "attach.int.negativeOffset", "attach.int.zeroWidth", "attach.int.zeroHeight", "attach.int.overflow",
                "attach.int.nullPointer", "attach.int.direct.tooFew", "attach.byte.heap.tooFew",
                "attach.byte.heapBufferWithoutArray", "attach.byte.noData", "attach.byte.negativeOffset",
                "attach.byte.overflow", "attach.byte.nullPointer")) {
            assertEquals("null", value(key), key);
        }
    }

    @Test
    public void customCursorsArePixmapCursorsOnTheDefaultDisplay() {
        assertTrue(value("cursor.int").startsWith("type=-1 defaultDisplay=true "), value("cursor.int"));
        assertTrue(value("cursor.byte").startsWith("type=-1 defaultDisplay=true "), value("cursor.byte"));
        assertEquals("0", value("cursor.int.reports"));
        assertEquals("0", value("cursor.byte.reports"));
    }

    @Test
    public void aThrowingAttachIsReportedAndGivesNoCursor() {
        assertEquals("null", value("cursor.readOnly"));
        assertEquals("1", value("cursor.readOnly.reports"));
        assertEquals("java.nio.ReadOnlyBufferException", value("cursor.readOnly.reportedType"));
        assertEquals("null", value("cursor.nullPixels"));
        assertEquals("1", value("cursor.nullPixels.reports"));
        assertEquals("java.lang.NullPointerException", value("cursor.nullPixels.reportedType"));
        assertEquals("null", value("cursor.nullPixels.reportedMessage"));
        // Every GLib critical or warning of the cursor calls: the g_object_unref(NULL) of each failed attach and
        // nothing else - no gdk_cursor_new_from_pixbuf on the NULL pixbuf, which EXCEPTION_OCCURED skipped.
        String unrefCritical = "GLib-GObject-CRITICAL: g_object_unref: assertion 'G_IS_OBJECT (object)' failed";
        assertEquals(List.of(unrefCritical, unrefCritical), glibMessages(run, CURSORS_BEGIN, CURSORS_END));
    }

    /** Written to {@code System.err} on the FX thread around the cursor calls. */
    static final String CURSORS_BEGIN = "-- GtkPixelsCursorNativeTest: cursors begin --";
    static final String CURSORS_END = "-- GtkPixelsCursorNativeTest: cursors end --";

    private static final Pattern GLIB_MESSAGE =
            Pattern.compile("^\\(.*:\\d+\\): ([\\w-]+-(?:CRITICAL|WARNING)) \\*\\*: (?:[\\d:.]+: )?(.*)$");

    /**
     * The GLib log lines of level CRITICAL or WARNING, as {@code <domain>-<LEVEL>: <message>}, that the child wrote
     * to its standard error between the lines {@code begin} and {@code end}.
     */
    static List<String> glibMessages(GtkGlassChildJvm.Run run, String begin, String end) {
        String stderr = new String(run.stderrBytes(), StandardCharsets.ISO_8859_1);
        int from = stderr.indexOf(begin + "\n");
        int to = stderr.indexOf(end + "\n", Math.max(from, 0));
        assertTrue(from >= 0 && to > from, "no " + begin + " ... " + end + " section on stderr:\n" + stderr);
        List<String> messages = new ArrayList<>();
        for (String line : stderr.substring(from, to).split("\n")) {
            Matcher matcher = GLIB_MESSAGE.matcher(line);
            if (matcher.matches()) {
                messages.add(matcher.group(1) + ": " + matcher.group(2));
            }
        }
        return messages;
    }

    @Test
    public void bestSizeIsTheDisplayDefaultCursorSize() {
        String size = value("cursor.defaultSize");
        assertNotEquals("0", size);
        assertEquals(String.join(",", List.of(size, size, size, size, size, size, size, size, size, size)),
                value("cursor.bestSizes"));
    }

    @Test
    public void attachDataStillServesTheRemainingJniWindowIcon() {
        assertTrue(value("icon.netWmIcon").startsWith("items="), value("icon.netWmIcon"));
        GtkGlassChildJvm.requireEnvironment(GtkPixelsCursorNativeTest.class, run, "env.gtk", "env.scale");
        assertEquals(NET_WM_ICON, value("icon.netWmIcon"));
    }

    static WritableImage iconImage() {
        WritableImage image = new WritableImage(2, 2);
        image.getPixelWriter().setColor(0, 0, Color.rgb(200, 30, 10));
        image.getPixelWriter().setColor(1, 0, Color.rgb(0, 128, 255));
        image.getPixelWriter().setColor(0, 1, Color.rgb(250, 250, 5, 0.5));
        image.getPixelWriter().setColor(1, 1, Color.rgb(1, 2, 3, 0.0));
        return image;
    }

    /** Runs in {@link GtkGlassChild}. */
    static void pixelsCursorScenario(Map<String, String> out) throws Exception {
        GtkGlassChild.onFx(() -> {
            // int attach
            IntBuffer offset2 = IntBuffer.wrap(INTS, 2, 6).slice();
            out.put("attach.int.heap.3x2.offset2",
                    GtkGlassShim.attachInt(3, 2, offset2, offset2.array(), offset2.arrayOffset(), false));
            out.put("attach.int.heap.2x2.exactBounds", GtkGlassShim.attachInt(2, 2, null, INTS, 4, false));
            out.put("attach.int.heap.tooFew", GtkGlassShim.attachInt(2, 2, null, INTS, 5, false));
            IntBuffer direct = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asIntBuffer();
            direct.put(ints(0, 4)).position(2);
            out.put("attach.int.direct.2x2.positionIgnored", GtkGlassShim.attachInt(2, 2, direct, null, 0, false));
            out.put("attach.int.direct.tooFew", GtkGlassShim.attachInt(5, 1, direct, null, 0, false));
            out.put("attach.int.heapBufferWithoutArray",
                    GtkGlassShim.attachInt(2, 2, IntBuffer.wrap(INTS), null, 0, false));
            out.put("attach.int.noData", GtkGlassShim.attachInt(2, 2, null, null, 0, false));
            out.put("attach.int.negativeOffset", GtkGlassShim.attachInt(1, 1, null, INTS, -1, false));
            out.put("attach.int.zeroWidth", GtkGlassShim.attachInt(0, 1, null, INTS, 0, false));
            out.put("attach.int.zeroHeight", GtkGlassShim.attachInt(1, 0, null, INTS, 0, false));
            out.put("attach.int.overflow", GtkGlassShim.attachInt(65536, 65536, null, INTS, 0, false));
            out.put("attach.int.nullPointer", GtkGlassShim.attachInt(2, 2, null, INTS, 0, true));

            // byte attach
            out.put("attach.byte.heap.2x2.offset3", GtkGlassShim.attachByte(2, 2, null, BYTES, 3, false));
            out.put("attach.byte.heap.2x2.exactBounds", GtkGlassShim.attachByte(2, 2, null, BYTES, 4, false));
            out.put("attach.byte.heap.tooFew", GtkGlassShim.attachByte(2, 2, null, BYTES, 5, false));
            ByteBuffer directBytes = ByteBuffer.allocateDirect(8);
            directBytes.put(BYTES, 0, 8).position(4);
            out.put("attach.byte.direct.1x2.positionIgnored",
                    GtkGlassShim.attachByte(1, 2, directBytes, null, 0, false));
            out.put("attach.byte.heapBufferWithoutArray",
                    GtkGlassShim.attachByte(1, 1, ByteBuffer.wrap(BYTES), null, 0, false));
            out.put("attach.byte.noData", GtkGlassShim.attachByte(1, 1, null, null, 0, false));
            out.put("attach.byte.negativeOffset", GtkGlassShim.attachByte(1, 1, null, BYTES, -1, false));
            out.put("attach.byte.overflow", GtkGlassShim.attachByte(40000, 40000, null, BYTES, 0, false));
            out.put("attach.byte.nullPointer", GtkGlassShim.attachByte(1, 1, null, BYTES, 0, true));

            // cursors
            System.err.println(CURSORS_BEGIN);
            System.err.flush();
            Application app = Application.GetApplication();
            recordCursor(out, "cursor.int", () -> app.createPixels(2, 2, IntBuffer.wrap(ints(0, 4))));
            ByteBuffer cursorBytes = ByteBuffer.allocateDirect(16);
            cursorBytes.put(BYTES, 0, 16).flip();
            recordCursor(out, "cursor.byte", () -> app.createPixels(2, 2, cursorBytes));
            recordCursor(out, "cursor.readOnly",
                    () -> app.createPixels(2, 2, IntBuffer.wrap(ints(0, 4)).asReadOnlyBuffer()));
            recordCursor(out, "cursor.nullPixels", () -> null);

            out.put("cursor.defaultSize", Integer.toString(GtkGlassShim.gdkDefaultCursorSize()));
            List<String> sizes = new ArrayList<>();
            for (int[] request : new int[][] {{16, 16}, {32, 32}, {0, 0}, {-1, 100}, {1000, 1}}) {
                Size size = Cursor.getBestSize(request[0], request[1]);
                sizes.add(Integer.toString(size.width));
                sizes.add(Integer.toString(size.height));
            }
            out.put("cursor.bestSizes", String.join(",", sizes));
            System.err.println(CURSORS_END);
            System.err.flush();
            return null;
        });
        GtkGlassChild.recordEnvironment(out);

        // A stage icon: Quantum -> Window.setIcon -> JNI _setIcon -> Pixels.attachData -> GtkPixels._attachByte.
        Stage stage = GtkGlassChild.onFx(() -> {
            Stage s = new Stage();
            s.getIcons().add(iconImage());
            s.setScene(new Scene(new Group(), 60, 40));
            s.show();
            return s;
        });
        String icon = "none";
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            icon = GtkGlassChild.onFx(() -> {
                List<Window> windows = Window.getWindows();
                return windows.isEmpty() ? "none" : GtkGlassShim.netWmIcon(windows.get(0).getNativeWindow());
            });
            if (!icon.equals("none")) {
                break;
            }
            Thread.sleep(50);
        }
        out.put("icon.netWmIcon", icon);
        GtkGlassChild.onFx(() -> {
            stage.hide();
            return null;
        });
    }

    private interface PixelsSource {
        Pixels get();
    }

    private static void recordCursor(Map<String, String> out, String key, PixelsSource source) {
        int before = GtkGlassChild.REPORTS.size();
        Cursor cursor = Application.GetApplication().createCursor(1, 1, source.get());
        out.put(key, GtkGlassShim.describeCursor(GtkGlassShim.nativeCursor(cursor)));
        List<GtkGlassChild.Report> all = new ArrayList<>(GtkGlassChild.REPORTS);
        List<GtkGlassChild.Report> reports = all.subList(before, all.size());
        out.put(key + ".reports", Integer.toString(reports.size()));
        if (!reports.isEmpty()) {
            out.put(key + ".reportedType", reports.get(0).throwable().getClass().getName());
            out.put(key + ".reportedMessage", String.valueOf(reports.get(0).throwable().getMessage()));
        }
    }
}
