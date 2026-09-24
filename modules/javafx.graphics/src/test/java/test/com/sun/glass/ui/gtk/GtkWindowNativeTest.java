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
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What the title and size natives of {@code GtkWindow} hand GTK, read back from GTK and from the X server in a child
 * JVM on the display of {@code DISPLAY}: the bodies of {@code _setTitle}, {@code _setMinimumSize},
 * {@code _setSystemMinimumSize} and {@code _setMaximumSize} of {@code GlassWindow.cpp} at commit
 * {@code 033187ad90}, and {@code WindowContextTop::update_window_constraints} of {@code glass_window.cpp} after them.
 * <ul>
 * <li>{@code _setTitle} converts the title's UTF-16 code units with {@code g_utf16_to_utf8}
 * ({@code jstring_to_utf8} of {@code glass_general.cpp}): the window's GTK title and its {@code _NET_WM_NAME} are the
 * UTF-8 of those units, a supplementary character included; an empty title is an empty GTK title; a {@code null}
 * title, and a title with a lone surrogate, which {@code g_utf16_to_utf8} rejects, leave the GTK title
 * {@code NULL}. The natives are called themselves, past {@code Window.setTitle}, which turns {@code null} into
 * {@code ""}.</li>
 * <li>The size natives refuse a negative minimum and a zero maximum ({@code false}, no change), map a minimum of 0
 * to 1 and a maximum of -1 - what {@code Window.setMaximumSize} passes for {@code Integer.MAX_VALUE} - to
 * {@code G_MAXSHORT}, and the larger of the minimum and the system minimum is the window's minimum: all read back
 * from the {@code WM_NORMAL_HINTS} GTK gives the X server.</li>
 * </ul>
 * No window manager is needed: GTK sets the properties either way.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkWindowNativeTest {

    /** The titles set, in this order, by name. */
    static final Map<String, String> TITLES = titles();

    private static Map<String, String> titles() {
        Map<String, String> titles = new LinkedHashMap<>();
        titles.put("latin1", "w2 " + (char) 0xE9);
        titles.put("bmp", "snow " + (char) 0x2603);
        titles.put("supplementary", "a" + (char) 0xD83D + (char) 0xDE00 + "b");
        titles.put("empty", "");
        titles.put("loneSurrogate", "x" + (char) 0xD800 + "y");
        titles.put("null", null);
        titles.put("again", "w2 " + (char) 0xE9);
        return titles;
    }

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkWindowNativeTest.class, "windowScenario", List.of());
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    /** A title reaches GTK, and the X server, as the UTF-8 of its UTF-16 code units. */
    @Test
    public void aTitleIsTheUtf8OfItsCodeUnits() {
        Map<String, String> utf8 = Map.of("latin1", "773220c3a9", "bmp", "736e6f7720e29883",
                "supplementary", "61f09f988062", "again", "773220c3a9");
        for (Map.Entry<String, String> entry : utf8.entrySet()) {
            String name = entry.getKey();
            assertEquals("true", value("title." + name + ".result"), name);
            assertEquals(entry.getValue(), value("title." + name + ".gtk"), name);
            assertEquals(entry.getValue(), value("title." + name + ".netWmName"), name);
        }
    }

    /** An empty title is an empty GTK title, not a {@code NULL} one. */
    @Test
    public void anEmptyTitleIsAnEmptyGtkTitle() {
        assertEquals("true", value("title.empty.result"));
        assertEquals("", value("title.empty.gtk"));
        assertEquals("", value("title.empty.netWmName"));
    }

    /**
     * A {@code null} title and a title with a lone surrogate, which {@code g_utf16_to_utf8} refuses, both hand GTK a
     * {@code NULL} title; the native still answers {@code true}.
     */
    @Test
    public void aNullTitleAndAnUnconvertibleTitleLeaveNoGtkTitle() {
        for (String name : List.of("loneSurrogate", "null")) {
            assertEquals("true", value("title." + name + ".result"), name);
            assertEquals("null", value("title." + name + ".gtk"), name);
        }
    }

    /** {@code _setMinimumSize}: the width first, a minimum of 0 is 1, a negative one is refused and changes nothing. */
    @Test
    public void theMinimumSizeReachesTheWindowManager() {
        assertEquals("true min=123,45", value("size.min 123x45"));
        assertEquals("false min=123,45", value("size.min -5x10 (native)"));
        assertEquals("false min=123,45", value("size.min 10x-5 (native)"));
        assertEquals("true min=1,1", value("size.min 0x0"));
        assertEquals("true min=150,60", value("size.min 150x60"));
    }

    /**
     * {@code _setMaximumSize}: a zero width or height is refused and changes nothing, and -1 - what
     * {@code Window.setMaximumSize} passes for {@code Integer.MAX_VALUE} - is {@code G_MAXSHORT}.
     */
    @Test
    public void theMaximumSizeReachesTheWindowManager() {
        assertEquals("true max=800,600", value("size.max 800x600"));
        assertEquals("false max=800,600", value("size.max 0x600 (native)"));
        assertEquals("false max=800,600", value("size.max 800x0 (native)"));
        assertEquals("true max=32767,32767", value("size.max MAX_VALUE x MAX_VALUE"));
        assertEquals("true max=32767,600", value("size.max -1x600 (native)"));
    }

    /**
     * {@code _setSystemMinimumSize}: the larger of it and the minimum size is the minimum; a negative one is refused
     * and changes nothing.
     */
    @Test
    public void theSystemMinimumSizeRaisesTheMinimum() {
        assertEquals("true min=300,200", value("size.sysmin 300x200 (native)"));
        assertEquals("false min=300,200", value("size.sysmin -1x5 (native)"));
        assertEquals("true min=300,70", value("size.sysmin 300x10 (native) over min 150x70"));
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    /** Runs in {@link GtkGlassChild}. */
    static void windowScenario(Map<String, String> out) throws Exception {
        GtkEventTrace t = new GtkEventTrace();
        Window window = GtkGlassChild.onFx(() -> {
            Window w = Application.GetApplication().createWindow(null, Screen.getMainScreen(),
                    Window.TITLED | Window.CLOSABLE | Window.MINIMIZABLE | Window.MAXIMIZABLE);
            w.setBounds(200, 200, true, true, 400, 300, -1, -1, 0, 0);
            w.setVisible(true);
            return w;
        });
        try {
            t.settle();
            for (Map.Entry<String, String> entry : TITLES.entrySet()) {
                String key = "title." + entry.getKey();
                GtkGlassChild.onFx(() -> {
                    out.put(key + ".result", Boolean.toString(GtkGlassShim.setTitleNative(window, entry.getValue())));
                    out.put(key + ".gtk", String.valueOf(GtkGlassShim.gtkWindowTitleHex(window)));
                    GtkGlassShim.syncGdkDisplay();
                    out.put(key + ".netWmName", GtkGlassShim.netWmNameHex(window.getNativeWindow()));
                    return null;
                });
            }
            out.put("size.initial", hints(t, window, "min", () -> true));
            size(t, out, window, "min 123x45", "min", () -> {
                window.setMinimumSize(123, 45);
                return window.getMinimumWidth() == 123 && window.getMinimumHeight() == 45;
            });
            size(t, out, window, "min -5x10 (native)", "min", () -> GtkGlassShim.setMinimumSizeNative(window, -5, 10));
            size(t, out, window, "min 10x-5 (native)", "min", () -> GtkGlassShim.setMinimumSizeNative(window, 10, -5));
            size(t, out, window, "min 0x0", "min", () -> {
                window.setMinimumSize(0, 0);
                return window.getMinimumWidth() == 0 && window.getMinimumHeight() == 0;
            });
            size(t, out, window, "min 150x60", "min", () -> GtkGlassShim.setMinimumSizeNative(window, 150, 60));
            size(t, out, window, "max 800x600", "max", () -> {
                window.setMaximumSize(800, 600);
                return window.getMaximumWidth() == 800 && window.getMaximumHeight() == 600;
            });
            size(t, out, window, "max 0x600 (native)", "max", () -> GtkGlassShim.setMaximumSizeNative(window, 0, 600));
            size(t, out, window, "max 800x0 (native)", "max", () -> GtkGlassShim.setMaximumSizeNative(window, 800, 0));
            size(t, out, window, "max MAX_VALUE x MAX_VALUE", "max", () -> {
                window.setMaximumSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
                return window.getMaximumWidth() == Integer.MAX_VALUE;
            });
            size(t, out, window, "max -1x600 (native)", "max", () -> GtkGlassShim.setMaximumSizeNative(window, -1,
                    600));
            size(t, out, window, "sysmin 300x200 (native)", "min", () ->
                    GtkGlassShim.setSystemMinimumSizeNative(window, 300, 200));
            size(t, out, window, "sysmin -1x5 (native)", "min", () ->
                    GtkGlassShim.setSystemMinimumSizeNative(window, -1, 5));
            size(t, out, window, "sysmin 300x10 (native) over min 150x70", "min", () ->
                    GtkGlassShim.setSystemMinimumSizeNative(window, 10, 10)
                            && GtkGlassShim.setMinimumSizeNative(window, 150, 70)
                            && GtkGlassShim.setSystemMinimumSizeNative(window, 300, 10));
        } finally {
            GtkGlassChild.onFx(() -> {
                window.close();
                return null;
            });
        }
    }

    @FunctionalInterface
    private interface Step {
        boolean run() throws Exception;
    }

    /**
     * Runs {@code step} on the FX thread, lets GTK hand the hints to the X server, and records
     * {@code <result> <the min or max part of WM_NORMAL_HINTS>} under {@code size.<name>}.
     */
    private static void size(GtkEventTrace t, Map<String, String> out, Window window, String name, String part,
                             Step step) throws Exception {
        out.put("size." + name, hints(t, window, part, step));
    }

    private static String hints(GtkEventTrace t, Window window, String part, Step step) throws Exception {
        boolean result = GtkGlassChild.onFx(step::run);
        t.settle();
        String hints = GtkGlassChild.onFx(() -> GtkGlassShim.wmNormalHints(window.getNativeWindow()));
        for (String item : hints.split(" ")) {
            if (item.startsWith(part + "=")) {
                return result + " " + item;
            }
        }
        return result + " " + hints;
    }
}
