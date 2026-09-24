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
import com.sun.glass.ui.View;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.javafx.font.JniStringCodec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The application queries of {@code GtkApplication} on a running toolkit: {@code staticView_getMultiClickTime},
 * {@code staticView_getMultiClickMaxX}, {@code staticView_getMultiClickMaxY}, {@code _supportsTransparentWindows}
 * and {@code _openURI}, as {@code GlassApplication.cpp} answered them at commit {@code 033187ad90}, pinned so that
 * the {@code java.lang.foreign} replacement can be held to it.
 * <ul>
 * <li>the multi-click time and distance are GtkSettings' {@code gtk-double-click-time} and
 * {@code gtk-double-click-distance}, the same value in both directions, read once and kept when the settings
 * change later;</li>
 * <li>transparent windows are supported exactly when the display supports compositing and the screen is
 * composited;</li>
 * <li>a URI {@code gtk_show_uri} cannot open prints {@code Error opening URI <uri> : <GError message>} on the
 * process's C {@code stderr}, where {@code <uri>} is the modified UTF-8 {@code GetStringUTFChars} produced: an
 * embedded U+0000 as {@code C0 80}, a supplementary character as two three-byte surrogates.</li>
 * </ul>
 * All of it runs in {@link GtkGlassChild} on the X11 display of {@code DISPLAY}.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkApplicationQueriesNativeTest {

    static final List<String> URIS = List.of(
            "jfxnosuchscheme:plain",
            "jfxnosuchscheme:a" + (char) 0 + "b" + (char) 0xE9 + (char) 0xD83D + (char) 0xDE00 + "z");

    /** Added to both double-click settings after the first read. */
    static final int CHANGE = 7;

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkApplicationQueriesNativeTest.class, "queriesScenario", List.of());
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    @Test
    public void multiClickValuesAreTheGtkSettings() {
        assertEquals(value("oracle.doubleClickTime"), value("multiClick.time"));
        assertEquals(value("oracle.doubleClickDistance"), value("multiClick.maxX"));
        assertEquals(value("oracle.doubleClickDistance"), value("multiClick.maxY"));
        assertEquals(value("multiClick.time") + "," + value("multiClick.maxX") + "," + value("multiClick.maxY"),
                value("multiClick.again"));
    }

    /**
     * {@code staticView_getMultiClickTime} and {@code staticView_getMultiClickMaxX} kept what they read first in a C
     * {@code static}: a GtkSettings change afterwards, which the settings themselves show, does not reach them.
     */
    @Test
    public void multiClickValuesAreReadOnceAndKept() {
        int time = Integer.parseInt(value("oracle.doubleClickTime"));
        int distance = Integer.parseInt(value("oracle.doubleClickDistance"));
        assertEquals((time + CHANGE) + "," + (distance + CHANGE), value("multiClick.changed.oracle"));
        assertEquals(value("multiClick.time") + "," + value("multiClick.maxX") + "," + value("multiClick.maxY"),
                value("multiClick.changed.peer"));
    }

    @Test
    public void transparentWindowsNeedACompositedScreen() {
        String oracle = value("oracle.compositing");
        assertEquals(Boolean.toString(oracle.equals("supports=1 composited=1")), value("transparentWindows"));
    }

    @Test
    public void unopenableUrisAreReportedOnStderrInModifiedUtf8() {
        byte[] stderr = run.stderrBytes();
        List<byte[]> lines = errorLines(stderr);
        assertEquals(URIS.size(), lines.size(), GtkGlassChildJvm.tail(run.stderr(), 40));
        for (int i = 0; i < URIS.size(); i++) {
            byte[] prefix = concat(ascii("Error opening URI "), withoutTerminator(JniStringCodec.toModifiedUtf8(
                    URIS.get(i))), ascii(" : "));
            byte[] line = lines.get(i);
            assertTrue(startsWith(line, prefix), "line " + i + ": " + HexFormat.of().formatHex(line));
            assertTrue(line.length > prefix.length, "no GError message on line " + i);
        }
    }

    /** Runs in {@link GtkGlassChild}. */
    static void queriesScenario(Map<String, String> out) throws Exception {
        GtkGlassChild.onFx(() -> {
            out.put("multiClick.time", Long.toString(View.getMultiClickTime()));
            out.put("multiClick.maxX", Integer.toString(View.getMultiClickMaxX()));
            out.put("multiClick.maxY", Integer.toString(View.getMultiClickMaxY()));
            out.put("multiClick.again", View.getMultiClickTime() + "," + View.getMultiClickMaxX() + ","
                    + View.getMultiClickMaxY());
            out.put("oracle.doubleClickTime", Integer.toString(GtkGlassShim.gtkSettingInt("gtk-double-click-time")));
            out.put("oracle.doubleClickDistance",
                    Integer.toString(GtkGlassShim.gtkSettingInt("gtk-double-click-distance")));
            int time = GtkGlassShim.gtkSettingInt("gtk-double-click-time");
            int distance = GtkGlassShim.gtkSettingInt("gtk-double-click-distance");
            try {
                GtkGlassShim.setGtkSettingInt("gtk-double-click-time", time + CHANGE);
                GtkGlassShim.setGtkSettingInt("gtk-double-click-distance", distance + CHANGE);
                out.put("multiClick.changed.oracle", GtkGlassShim.gtkSettingInt("gtk-double-click-time") + ","
                        + GtkGlassShim.gtkSettingInt("gtk-double-click-distance"));
                out.put("multiClick.changed.peer", View.getMultiClickTime() + "," + View.getMultiClickMaxX() + ","
                        + View.getMultiClickMaxY());
            } finally {
                GtkGlassShim.setGtkSettingInt("gtk-double-click-time", time);
                GtkGlassShim.setGtkSettingInt("gtk-double-click-distance", distance);
            }
            out.put("transparentWindows", Boolean.toString(Application.GetApplication().supportsTransparentWindows()));
            out.put("oracle.compositing", GtkGlassShim.compositing());
            out.put("oracle.screenResolution", Double.toString(GtkGlassShim.screenResolution()));
            return null;
        });
        for (String uri : URIS) {
            GtkGlassChild.onFx(() -> {
                Application.GetApplication().showDocument(uri);
                return null;
            });
        }
        out.put("showDocument.done", "true");
    }

    /** The {@code Error opening URI } lines of {@code stderr}, without their line feed. */
    static List<byte[]> errorLines(byte[] stderr) {
        byte[] marker = ascii("Error opening URI ");
        List<byte[]> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= stderr.length; i++) {
            if (i == stderr.length || stderr[i] == '\n') {
                byte[] line = Arrays.copyOfRange(stderr, start, i);
                if (startsWith(line, marker)) {
                    lines.add(line);
                }
                start = i + 1;
            }
        }
        return lines;
    }

    static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] withoutTerminator(byte[] cString) {
        return Arrays.copyOf(cString, cString.length - 1);
    }

    static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
