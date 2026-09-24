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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code com.sun.glass.ui.gtk.GtkGlassNative} without a display, so that every Linux build that has the glass
 * natives - every CI job, where the display tests of this package skip - links it: in a child JVM with no
 * {@code DISPLAY} and no toolkit, the facade initializes and binds exactly
 * {@link GtkGlassNativeBindingTest#SYMBOLS}, and the two struct layouts of GLib it hard-codes hold against the
 * library:
 * <ul>
 * <li>{@code GError.message} is at {@code G_ERROR_MESSAGE_OFFSET}, after {@code domain} and {@code code};</li>
 * <li>{@code g_hash_table_iter_init} and {@code g_hash_table_iter_next} write nothing past
 * {@code G_HASH_TABLE_ITER_SIZE} bytes, and the iterator walks the table.</li>
 * </ul>
 * The layouts are those of LP64 Linux, which is every architecture the GTK glass is built for.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkGlassNativeHeadlessTest {

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        String libraryPath = System.getProperty("java.library.path", "");
        assumeTrue(Stream.of(libraryPath.split(File.pathSeparator)).filter(dir -> !dir.isEmpty())
                .anyMatch(dir -> Files.isRegularFile(Path.of(dir, "libglassgtk3.so"))),
                "this build has no GTK glass natives in " + libraryPath);
        run = GtkGlassChildJvm.runWithoutToolkit(GtkGlassNativeHeadlessTest.class, "headlessScenario", List.of(),
                false);
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    @Test
    public void theChildHadNoDisplay() {
        assertEquals("null", value("display"));
    }

    @Test
    public void facadeBindsExactlyTheseSymbolsWithoutADisplay() {
        assertEquals(String.join(",", GtkGlassNativeBindingTest.SYMBOLS), value("bound"));
    }

    @Test
    public void gErrorMessageIsWhereTheFacadeReadsIt() {
        assertEquals("domain=true code=42 message=layout probe", value("layout.gError"));
    }

    @Test
    public void hashTableIteratorFitsTheFacadeAllocation() {
        assertEquals("entries=7:9 tableAtStart=true canary=intact", value("layout.gHashTableIter"));
    }

    /**
     * The frame an array upload copies before the paint ({@code GtkGlassNative.frameLength}) is the frame the C
     * accepts: over offsets, sizes and array lengths around every boundary, including the overflow of
     * {@code unit * width * height + offset}, it answers -1 exactly where {@code ggtk_view_upload_pixels_int}
     * ({@code unit} 1) or {@code _byte} ({@code unit} 4) returns without painting, and the frame's element count
     * everywhere else.
     */
    @Test
    public void theCopiedFrameIsTheFrameTheCAccepts() {
        assertEquals("", value("frameLength.mismatches"));
        assertTrue(Integer.parseInt(value("frameLength.checked")) > 3000, value("frameLength.checked"));
        assertTrue(Integer.parseInt(value("frameLength.accepted")) > 50, value("frameLength.accepted"));
    }

    /** Runs in {@link GtkGlassChild} without a toolkit and without {@code DISPLAY}. */
    static void headlessScenario(Map<String, String> out) {
        out.put("display", String.valueOf(System.getenv("DISPLAY")));
        out.put("bound", String.join(",", GtkGlassShim.boundSymbols()));
        out.put("layout.gError", GtkGlassShim.gErrorLayout());
        out.put("layout.gHashTableIter", GtkGlassShim.hashTableIterLayout());
        frameLengths(out);
    }

    private static void frameLengths(Map<String, String> out) {
        int[] offsets = {-1, 0, 1, 7, 1_000_000_000, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        int[] widths = {-1, 0, 1, 2, 1920, 46341, Integer.MAX_VALUE};
        int[] heights = {-1, 0, 1, 1080, 46341, Integer.MAX_VALUE};
        int checked = 0;
        int accepted = 0;
        List<String> mismatches = new ArrayList<>();
        for (int unit : new int[] {1, 4}) {
            for (int offset : offsets) {
                for (int width : widths) {
                    for (int height : heights) {
                        long exact = (long) unit * width * height + offset;
                        List<Integer> lengths = new ArrayList<>(List.of(0, 1, 7, 2_073_607, 8_294_428,
                                Integer.MAX_VALUE));
                        if (width > 0 && height > 0 && exact >= 1 && exact <= Integer.MAX_VALUE) {
                            lengths.add((int) exact);
                            lengths.add((int) exact - 1);
                        }
                        for (int length : lengths) {
                            int expected = cAccepts(length, offset, width, height, unit);
                            int actual = GtkGlassShim.frameLength(length, offset, width, height, unit);
                            checked++;
                            if (expected >= 0) {
                                accepted++;
                            }
                            if (actual != expected && mismatches.size() < 20) {
                                mismatches.add("len=" + length + " off=" + offset + " w=" + width + " h=" + height
                                        + " unit=" + unit + ": " + actual + " != " + expected);
                            }
                        }
                    }
                }
            }
        }
        out.put("frameLength.checked", Integer.toString(checked));
        out.put("frameLength.accepted", Integer.toString(accepted));
        out.put("frameLength.mismatches", String.join("; ", mismatches));
    }

    /**
     * The checks of {@code ggtk_view_upload_pixels_int} ({@code unit} 1) and {@code _byte} ({@code unit} 4) of
     * {@code GlassView.cpp}, transcribed in their order with the C's {@code int} arithmetic - which is Java's too,
     * since the order keeps every product in range: -1 where the C returns without painting, else the number of
     * array elements it paints from.
     */
    private static int cAccepts(int pixelsLen, int offset, int width, int height, int unit) {
        if (offset < 0) {
            return -1;
        }
        if (width <= 0 || height <= 0) {
            return -1;
        }
        if (unit == 1) {
            if (width > ((Integer.MAX_VALUE - offset) / height)) {
                return -1;
            }
            if ((width * height + offset) > pixelsLen) {
                return -1;
            }
            return width * height;
        }
        if (width > (((Integer.MAX_VALUE - offset) / 4) / height)) {
            return -1;
        }
        if ((4 * width * height + offset) > pixelsLen) {
            return -1;
        }
        return 4 * width * height;
    }
}
