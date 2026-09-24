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

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.glass.ui.gtk.screencast.ScreencastHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The robot's key tables and UI scale in Java against the C that {@code libglassgtk3.so} keeps for its event and
 * screencast code ({@code glass_key.cpp}, {@code glass_screen.cpp}), compared in one process on the X11 display of
 * {@code DISPLAY}:
 * <ul>
 * <li>{@code find_gdk_keyval_for_glass_keycode} for every glass key code 0..0xFFFF (and three out-of-range ones),
 * with the default screenshot method and with the remote-desktop one, whose extra robot overrides the C adds once
 * {@code ScreencastHelper} has loaded;</li>
 * <li>{@code find_gdk_keycode_for_keyval} for every keyval of the tables, printable Latin-1, the function-key block
 * and a few Unicode keyvals, against the keyboard layout of the display;</li>
 * <li>the {@code keymap} insert table against {@code gdk_keyval_to_glass} for every keyval 0..0xFFFF;</li>
 * <li>{@code getUIScale} for a corpus of {@code GDK_SCALE} values the C reads with {@code atoi}, with and without
 * the {@code glass.gtk.uiScale} override.</li>
 * </ul>
 * The tables in Java are a copy the C does not use; this test is what keeps them equal until the C key code moves.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkRobotParityTest {

    static final List<String> GDK_SCALES = List.of("<unset>", "2", "2x", " 2", "0", "-1", "3.5", "", "+3", "99999");

    static final float[] OVERRIDES = {-1.0f, 1.5f, 0.0f, 2.0f};

    private static GtkGlassChildJvm.Run gtk;
    private static GtkGlassChildJvm.Run remote;

    @BeforeAll
    @Timeout(2 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenarios() {
        GtkGlassChildJvm.requireDisplay();
        gtk = GtkGlassChildJvm.run(GtkRobotParityTest.class, "parityScenario", List.of());
        remote = GtkGlassChildJvm.run(GtkRobotParityTest.class, "remoteDesktopScenario",
                List.of("-Djavafx.robot.screenshotMethod=dbusRemoteDesktop"));
    }

    private static String value(GtkGlassChildJvm.Run run, String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    @Test
    public void keyvalForGlassKeycodeMatchesTheC() {
        assertEquals("", value(gtk, "keyvalForCode.mismatches"));
        assertNotEquals("0", value(gtk, "keyvalForCode.found"));
    }

    @Test
    public void keyvalForGlassKeycodeMatchesTheCWithTheRemoteDesktopOverrides() {
        assertEquals("", value(remote, "keyvalForCode.mismatches"));
        // the remote-desktop overrides really are in force on both sides: VK_NUMPAD0 is KP_Insert, not KP_0
        assertEquals(Integer.toString(0xff9e), value(remote, "keyvalForCode.numpad0"));
        assertEquals(Integer.toString(0xffb0), value(gtk, "keyvalForCode.numpad0"));
    }

    @Test
    public void keycodeForKeyvalMatchesTheC() {
        assertEquals("", value(gtk, "keycodeForKeyval.mismatches"));
        assertNotEquals("0", value(gtk, "keycodeForKeyval.found"));
    }

    @Test
    public void keymapTableMatchesTheC() {
        assertEquals("", value(gtk, "keymap.mismatches"));
        assertEquals("171", value(gtk, "keymap.inserts"));
    }

    @Test
    public void uiScaleMatchesTheC() {
        assertEquals("", value(gtk, "uiScale.mismatches"));
        // only at 96 dpi does getUIScale read GDK_SCALE and GSettings, the branches the corpus is for
        GtkGlassChildJvm.requireEnvironment(GtkRobotParityTest.class, gtk, "env.resolution");
        assertEquals("96.0", value(gtk, "uiScale.resolution"));
    }

    /** Runs in {@link GtkGlassChild}. */
    static void parityScenario(Map<String, String> out) throws Exception {
        GtkGlassChild.recordEnvironment(out);
        GtkGlassChild.onFx(() -> {
            compareKeyvalForCode(out);

            TreeSet<Integer> keyvals = new TreeSet<>();
            int[] keymap = GtkGlassShim.javaTable("KEYMAP");
            for (int i = 0; i < keymap.length; i += 2) {
                keyvals.add(keymap[i]);
            }
            for (String table : List.of("ROBOT_JAVA_TO_KEYVAL", "REMOTE_ROBOT_JAVA_TO_KEYVAL")) {
                int[] pairs = GtkGlassShim.javaTable(table);
                for (int i = 1; i < pairs.length; i += 2) {
                    keyvals.add(pairs[i]);
                }
            }
            for (int k = 0x20; k <= 0xff; k++) {
                keyvals.add(k);
            }
            for (int k = 0xfe00; k <= 0xffff; k++) {
                keyvals.add(k);
            }
            keyvals.addAll(List.of(0, -1, 0x20ac, 0x1000000 | 0x20ac, 0x100263a, 0x7fffffff));
            List<String> mismatches = new ArrayList<>();
            int found = 0;
            for (int keyval : keyvals) {
                int c = GtkGlassShim.cFindGdkKeycodeForKeyval(keyval);
                int java = GtkGlassShim.javaFindGdkKeycodeForKeyval(keyval);
                if (c != java) {
                    mismatches.add(Integer.toHexString(keyval) + ":c=" + c + ",java=" + java);
                }
                if (c >= 0) {
                    found++;
                }
            }
            out.put("keycodeForKeyval.mismatches", String.join(" ", mismatches));
            out.put("keycodeForKeyval.found", Integer.toString(found));

            Map<Integer, Integer> javaKeymap = new HashMap<>();
            for (int i = 0; i < keymap.length; i += 2) {
                javaKeymap.put(keymap[i], keymap[i + 1]);
            }
            List<String> keymapMismatches = new ArrayList<>();
            for (int keyval = 0; keyval <= 0xffff; keyval++) {
                int c = GtkGlassShim.cGdkKeyvalToGlass(keyval);
                int java = javaKeymap.getOrDefault(keyval, 0);
                if (c != java) {
                    keymapMismatches.add(Integer.toHexString(keyval) + ":c=" + c + ",java=" + java);
                }
            }
            out.put("keymap.mismatches", String.join(" ", keymapMismatches));
            out.put("keymap.inserts", Integer.toString(keymap.length / 2));

            out.put("uiScale.resolution", Double.toString(GtkGlassShim.screenResolution()));
            float savedOverride = GtkGlassShim.overrideUIScale();
            List<String> scaleMismatches = new ArrayList<>();
            try {
                for (float override : OVERRIDES) {
                    GtkGlassShim.setOverrideUIScale(override);
                    for (String scale : GDK_SCALES) {
                        GtkGlassShim.setenv("GDK_SCALE", scale.equals("<unset>") ? null : scale);
                        float c = GtkGlassShim.cGetUIScale();
                        float java = GtkGlassShim.javaGetUIScale();
                        if (Float.floatToRawIntBits(c) != Float.floatToRawIntBits(java)) {
                            scaleMismatches.add(override + "/'" + scale + "':c=" + c + ",java=" + java);
                        }
                    }
                }
            } finally {
                GtkGlassShim.setenv("GDK_SCALE", null);
                GtkGlassShim.setOverrideUIScale(savedOverride);
            }
            out.put("uiScale.mismatches", String.join(" ", scaleMismatches));
            return null;
        });
    }

    /** Runs in {@link GtkGlassChild} with {@code -Djavafx.robot.screenshotMethod=dbusRemoteDesktop}. */
    static void remoteDesktopScenario(Map<String, String> out) throws Exception {
        GtkGlassChild.onFx(() -> {
            // what GtkRobot does before its first key: this loads the screen capture C and sets its method
            out.put("screencast.available", Boolean.toString(ScreencastHelper.isAvailable()));
            compareKeyvalForCode(out);
            return null;
        });
    }

    private static void compareKeyvalForCode(Map<String, String> out) {
        List<String> mismatches = new ArrayList<>();
        int found = 0;
        List<Integer> codes = new ArrayList<>();
        for (int code = 0; code <= 0xffff; code++) {
            codes.add(code);
        }
        codes.addAll(List.of(-1, Integer.MIN_VALUE, Integer.MAX_VALUE));
        for (int code : codes) {
            int c = GtkGlassShim.cFindGdkKeyvalForGlassKeycode(code);
            int java = GtkGlassShim.javaFindGdkKeyvalForGlassKeycode(code);
            if (c != java) {
                mismatches.add(Integer.toHexString(code) + ":c=" + c + ",java=" + java);
            }
            if (c != -1) {
                found++;
            }
        }
        out.put("keyvalForCode.mismatches", String.join(" ", mismatches));
        out.put("keyvalForCode.found", Integer.toString(found));
        out.put("keyvalForCode.numpad0", Integer.toString(GtkGlassShim.cFindGdkKeyvalForGlassKeycode(
                KeyEvent.VK_NUMPAD0)));
    }
}
