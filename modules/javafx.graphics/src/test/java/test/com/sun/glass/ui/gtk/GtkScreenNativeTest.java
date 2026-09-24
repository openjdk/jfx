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

import com.sun.glass.ui.Screen;
import com.sun.glass.ui.gtk.GtkGlassShim;
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
 * {@code GtkApplication.staticScreen_getScreens} on a running toolkit, as {@code rebuild_screens} and
 * {@code createJavaScreen} of {@code glass_screen.cpp} answered it at commit {@code 033187ad90}, pinned so that
 * the {@code java.lang.foreign} replacement can be held to it. Every expectation is derived from what the same
 * child JVM read out of GDK and X11 independently, so the test does not depend on the display's size:
 * <ul>
 * <li>one {@code Screen} per GDK monitor, with the monitor's index as its native handle and the system visual's
 * depth;</li>
 * <li>the platform bounds are the monitor's GDK geometry unscaled, the bounds are that geometry divided by the
 * UI scale as {@code float}s and truncated towards zero, and all four scales are that UI scale;</li>
 * <li>the visible bounds are the {@code _NET_WORKAREA} rectangle of the {@code _NET_CURRENT_DESKTOP} intersected
 * with the monitor geometry, scaled the same way - and the whole screen when the property, the atom or the
 * desktop index is missing;</li>
 * <li>the resolution is {@code width * 254 / (width_mm * 10)}, in integer arithmetic, from the monitor's
 * physical size, or the screen's when the monitor reports none and there is exactly one monitor, and 96 when
 * neither is positive.</li>
 * </ul>
 * It runs in {@link GtkGlassChild} on the X11 display of {@code DISPLAY}, and sets and removes the two root
 * window properties itself, which no window manager is there to own.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkScreenNativeTest {

    /** The work area written to {@code _NET_WORKAREA} for desktop 0: x, y, width, height. */
    static final long[] WORKAREA_0 = {13, 29, 1600, 800};

    /** The work area written for desktop 1. */
    static final long[] WORKAREA_1 = {101, 53, 700, 400};

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkScreenNativeTest.class, "screensScenario", List.of());
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    private static int number(String key) {
        return Integer.parseInt(value(key));
    }

    /** One {@code Screen} per GDK monitor, in monitor order, the monitor index as the native handle. */
    @Test
    public void oneScreenPerMonitorInMonitorOrder() {
        int monitors = number("gdk.monitors");
        assertTrue(monitors >= 1, "no monitor");
        assertEquals(monitors, number("screens.count"));
        for (int i = 0; i < monitors; i++) {
            assertEquals(Integer.toString(i), value("screens." + i + ".ptr"));
            assertEquals(value("gdk.visualDepth"), value("screens." + i + ".depth"));
            assertEquals("0", value("screens." + i + ".adapter"));
        }
    }

    /** The platform bounds are the monitor geometry, the bounds are that geometry over the UI scale. */
    @Test
    public void boundsAreTheMonitorGeometryOverTheUiScale() {
        for (int i = 0; i < number("gdk.monitors"); i++) {
            int[] geometry = numbers(value("gdk.monitorGeometry." + i));
            float scale = Float.parseFloat(value("gdk.uiScale"));
            assertEquals(text(geometry), value("screens." + i + ".platform"));
            assertEquals(text(scaled(geometry, scale)), value("screens." + i + ".bounds"));
            assertEquals(scale + "," + scale + "," + scale + "," + scale, value("screens." + i + ".scales"));
        }
    }

    /**
     * With no {@code _NET_WORKAREA} on the root window - the state of a display with no window manager - the
     * visible bounds are the whole screen intersected with the monitor geometry.
     */
    @Test
    public void withoutWorkareaTheVisibleBoundsAreTheWholeScreen() {
        assertVisible("screens", fullScreen());
    }

    /** The {@code _NET_WORKAREA} rectangle of desktop 0, intersected with the monitor geometry. */
    @Test
    public void workareaOfTheCurrentDesktopBecomesTheVisibleBounds() {
        assertVisible("workarea0", rectangle(WORKAREA_0));
    }

    /** {@code _NET_CURRENT_DESKTOP} selects the rectangle: the second four cardinals for desktop 1. */
    @Test
    public void currentDesktopSelectsItsWorkareaRectangle() {
        assertVisible("workarea1", rectangle(WORKAREA_1));
    }

    /** A desktop index past the end of {@code _NET_WORKAREA} leaves the work area at the whole screen. */
    @Test
    public void aDesktopWithoutAWorkareaRectangleFallsBackToTheWholeScreen() {
        assertVisible("workareaOutOfRange", fullScreen());
    }

    /** Removing the properties again puts the visible bounds back where they were. */
    @Test
    public void removingTheWorkareaRestoresTheWholeScreen() {
        assertVisible("restored", fullScreen());
        for (int i = 0; i < number("gdk.monitors"); i++) {
            assertEquals(value("screens." + i + ".visible"), value("restored." + i + ".visible"));
        }
    }

    /** The resolution is the monitor's pixel width over its physical width, in whole dots per inch. */
    @Test
    public void resolutionComesFromThePhysicalSize() {
        for (int i = 0; i < number("gdk.monitors"); i++) {
            int[] geometry = numbers(value("gdk.monitorGeometry." + i));
            int[] scaledGeometry = scaled(geometry, Float.parseFloat(value("gdk.uiScale")));
            int[] size = numbers(value("gdk.screenSize." + i));
            int mmW = size[4];
            int mmH = size[5];
            if ((mmW <= 0 || mmH <= 0) && number("gdk.monitors") == 1) {
                mmW = size[2];
                mmH = size[3];
            }
            String expected = mmW <= 0 || mmH <= 0
                    ? "96,96"
                    : (scaledGeometry[2] * 254) / (mmW * 10) + "," + (scaledGeometry[3] * 254) / (mmH * 10);
            assertEquals(expected, value("screens." + i + ".resolution"));
        }
    }

    /**
     * A {@code PropertyNotify} of {@code _NET_WORKAREA} on the root window reaches
     * {@code Screen.notifySettingsChanged}, which re-reads the screens: the cached list the toolkit hands out
     * carries the new visible bounds without anyone asking the peer again.
     */
    @Test
    public void aWorkareaChangeNotifiesTheScreens() {
        assertEquals(value("workarea0.0.visible"), value("notified.0.visible"),
                "Screen.notifySettingsChanged did not refresh the cached screens");
    }

    private static void assertVisible(String prefix, int[] workarea) {
        for (int i = 0; i < number("gdk.monitors"); i++) {
            int[] geometry = numbers(value("gdk.monitorGeometry." + i));
            int[] visible = intersect(workarea, geometry);
            assertEquals(text(scaled(visible, Float.parseFloat(value("gdk.uiScale")))),
                    value(prefix + "." + i + ".visible"));
        }
    }

    private static int[] fullScreen() {
        int[] size = numbers(value("gdk.screenSize.0"));
        return new int[] {0, 0, size[0], size[1]};
    }

    private static int[] rectangle(long[] cardinals) {
        return new int[] {(int) cardinals[0], (int) cardinals[1], (int) cardinals[2], (int) cardinals[3]};
    }

    /** {@code gdk_rectangle_intersect}, which leaves an empty rectangle at the origin when they do not meet. */
    private static int[] intersect(int[] a, int[] b) {
        int x = Math.max(a[0], b[0]);
        int y = Math.max(a[1], b[1]);
        int right = Math.min(a[0] + a[2], b[0] + b[2]);
        int bottom = Math.min(a[1] + a[3], b[1] + b[3]);
        if (right <= x || bottom <= y) {
            return new int[] {0, 0, 0, 0};
        }
        return new int[] {x, y, right - x, bottom - y};
    }

    /** {@code (jint) (value / uiScale)}: a {@code float} division truncated towards zero. */
    private static int[] scaled(int[] rectangle, float uiScale) {
        int[] result = new int[rectangle.length];
        for (int i = 0; i < rectangle.length; i++) {
            result[i] = (int) (rectangle[i] / uiScale);
        }
        return result;
    }

    private static int[] numbers(String text) {
        String[] parts = text.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }

    private static String text(int[] values) {
        StringBuilder sb = new StringBuilder();
        for (int value : values) {
            sb.append(sb.isEmpty() ? "" : ",").append(value);
        }
        return sb.toString();
    }

    /** Runs in {@link GtkGlassChild}. */
    static void screensScenario(Map<String, String> out) throws Exception {
        GtkGlassChild.recordEnvironment(out);
        GtkGlassChild.onFx(() -> {
            int monitors = GtkGlassShim.gdkMonitorCount();
            out.put("gdk.monitors", Integer.toString(monitors));
            out.put("gdk.uiScale", Float.toString(GtkGlassShim.cGetUIScale()));
            out.put("gdk.visualDepth", Integer.toString(GtkGlassShim.systemVisualDepth()));
            for (int i = 0; i < monitors; i++) {
                out.put("gdk.monitorGeometry." + i, text(GtkGlassShim.gdkMonitorGeometry(i)));
                out.put("gdk.screenSize." + i, text(GtkGlassShim.gdkScreenSize(i)));
            }
            record(out, "screens", GtkGlassShim.screens());
            return null;
        });
        try {
            GtkGlassChild.onFx(() -> {
                GtkGlassShim.setRootCardinals("_NET_CURRENT_DESKTOP", new long[] {0});
                GtkGlassShim.setRootCardinals("_NET_WORKAREA", concat(WORKAREA_0, WORKAREA_1));
                record(out, "workarea0", GtkGlassShim.screens());
                return null;
            });
            // the PropertyNotify of the two writes above is dispatched by the GTK main loop, not by the writer
            String expected = out.get("workarea0.0.visible");
            GtkGlassChild.waitFor(10_000, () -> {
                try {
                    return expected.equals(GtkGlassChild.onFx(() -> visible(Screen.getScreens().get(0))));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            GtkGlassChild.onFx(() -> {
                out.put("notified.0.visible", visible(Screen.getScreens().get(0)));
                GtkGlassShim.setRootCardinals("_NET_CURRENT_DESKTOP", new long[] {1});
                record(out, "workarea1", GtkGlassShim.screens());
                GtkGlassShim.setRootCardinals("_NET_CURRENT_DESKTOP", new long[] {7});
                record(out, "workareaOutOfRange", GtkGlassShim.screens());
                return null;
            });
        } finally {
            GtkGlassChild.onFx(() -> {
                GtkGlassShim.setRootCardinals("_NET_WORKAREA", null);
                GtkGlassShim.setRootCardinals("_NET_CURRENT_DESKTOP", null);
                record(out, "restored", GtkGlassShim.screens());
                return null;
            });
        }
    }

    private static long[] concat(long[] first, long[] second) {
        long[] result = new long[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static void record(Map<String, String> out, String prefix, Screen[] screens) {
        out.put(prefix + ".count", Integer.toString(screens.length));
        for (int i = 0; i < screens.length; i++) {
            Screen screen = screens[i];
            out.put(prefix + "." + i + ".ptr", Long.toString(screen.getNativeScreen()));
            out.put(prefix + "." + i + ".adapter", Integer.toString(screen.getAdapterOrdinal()));
            out.put(prefix + "." + i + ".depth", Integer.toString(screen.getDepth()));
            out.put(prefix + "." + i + ".bounds", screen.getX() + "," + screen.getY() + "," + screen.getWidth()
                    + "," + screen.getHeight());
            out.put(prefix + "." + i + ".platform", screen.getPlatformX() + "," + screen.getPlatformY() + ","
                    + screen.getPlatformWidth() + "," + screen.getPlatformHeight());
            out.put(prefix + "." + i + ".visible", visible(screen));
            out.put(prefix + "." + i + ".resolution", screen.getResolutionX() + "," + screen.getResolutionY());
            out.put(prefix + "." + i + ".scales", screen.getPlatformScaleX() + "," + screen.getPlatformScaleY()
                    + "," + screen.getRecommendedOutputScaleX() + "," + screen.getRecommendedOutputScaleY());
        }
    }

    private static String visible(Screen screen) {
        return screen.getVisibleX() + "," + screen.getVisibleY() + "," + screen.getVisibleWidth() + ","
                + screen.getVisibleHeight();
    }

}
