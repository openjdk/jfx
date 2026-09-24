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

import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkSyntheticEvents;
import com.sun.glass.ui.gtk.GtkTraceWindow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static com.sun.glass.ui.gtk.GtkSyntheticEvents.STATE_ABOVE;
import static com.sun.glass.ui.gtk.GtkSyntheticEvents.STATE_ICONIFIED;
import static com.sun.glass.ui.gtk.GtkSyntheticEvents.STATE_MAXIMIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The window-state upcalls of the GTK glass window code, which no X server without a window manager makes GDK
 * report: {@code WindowContextBase::process_state}, {@code notify_state} and {@code WindowContextTop::notify_on_top}
 * of {@code glass_window.cpp} at commit {@code 033187ad90}, reached through {@code GdkEventWindowState}s that the
 * scenario puts on GDK's own event queue ({@link GtkSyntheticEvents#putWindowState}), so that they take the path of
 * the ones GDK makes from a window manager's {@code _NET_WM_STATE}: {@code process_events}, then the window's
 * context. The golden, captured from the JNI build, pins what the event trace cannot reach:
 * <ul>
 * <li>{@code RESTORE} (the iconified or maximized state cleared) repaints the view first and only then calls
 * {@code GtkWindow.notifyStateChanged}; a window without a view goes straight to it; the other states do not
 * repaint;</li>
 * <li>a {@code REPAINT} that throws is reported, and no state change follows ({@code CHECK_JNI_EXCEPTION}'s early
 * return);</li>
 * <li>{@code ABOVE} set or cleared calls {@code Window.notifyLevelChanged} with {@code Level.FLOATING} or
 * {@code Level.NORMAL} - not when the window already has that level, not for an owned window that inherits its
 * owner's, and not when the same event also changes the iconified or maximized state;</li>
 * <li>{@code notifyStateChanged} and {@code notifyLevelChanged} that throw are reported once.</li>
 * </ul>
 * Normalisation: none. The rule of the event trace that moves a {@code REPAINT} to the end of its step would hide
 * the order this scenario exists for; no frame-clock expose reaches these steps, and repeated runs of the JNI build
 * were identical without it.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkWindowStateTraceTest {

    static final String SCENARIO = "gtk-window-state";

    static final List<GtkTraceGolden.Rule> RULES = List.of();

    static final List<String> LEGEND = List.of(
            "Lines as in the event trace; '!throws' marks the recorded call that threw into the C, 'reported' what",
            "Application.reportException delivered. Each 'put' step queues one GdkEventWindowState for the window it",
            "names, changed_mask -> new_window_state as GdkWindowState flags (2 ICONIFIED, 4 MAXIMIZED, 32 ABOVE).");

    private static final int STYLE = Window.TITLED | Window.CLOSABLE | Window.MINIMIZABLE | Window.MAXIMIZABLE;

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(10 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        GtkTraceGolden.requireNoWindowManager(GtkWindowStateTraceTest.class);
        run = GtkTraceGolden.runRepeated(GtkWindowStateTraceTest.class, "windowStateScenario", List.of(), SCENARIO,
                RULES);
    }

    @AfterAll
    static void oracleRan() {
        ParityGate.ledger(GtkWindowStateTraceTest.class).assertOracleRan();
    }

    /** The trace equals the golden captured from the JNI build (no window manager). */
    @Test
    public void traceMatchesTheJniGolden() {
        List<String> raw = GtkTraceGolden.rawTrace(run);
        GtkTraceGolden.verify(GtkWindowStateTraceTest.class, SCENARIO, run, raw, GtkTraceGolden.normalise(raw, RULES),
                LEGEND);
    }

    @Test
    public void everyUpcallArrivesOnTheEventThread() {
        for (String line : GtkTraceGolden.rawTrace(run)) {
            assertTrue(!line.contains("!off-fx-thread"), line);
        }
    }

    /**
     * What {@code notify_state} calls first: for {@code RESTORE} the view's {@code REPAINT} and then the state change -
     * never the other way - and the state change alone for a window without a view and for the other states. (GTK's
     * own handling of the event may queue a redraw, whose {@code REPAINT} comes later in the step.)
     */
    @Test
    public void notifyStateRepaintsFirstOnlyOnRestore() {
        List<List<String>> steps = List.of(
                List.of("put w1 ICONIFIED -> 0 (deiconified)", "v1.view REPAINT", "w1.notifyStateChanged 533"),
                List.of("put w1 MAXIMIZED -> 0 (unmaximized)", "v1.view REPAINT", "w1.notifyStateChanged 533"),
                List.of("put w2 ICONIFIED -> 0 (deiconified; no MINIMIZABLE: the window functions are set again)",
                        "v2.view REPAINT", "w2.notifyStateChanged 533"),
                List.of("put w3 ICONIFIED -> 0 (deiconified; no view)", "w3.notifyStateChanged 533"),
                List.of("put w1 ICONIFIED -> ICONIFIED (iconified)", "w1.notifyStateChanged 531"),
                List.of("put w1 MAXIMIZED -> MAXIMIZED (maximized)", "w1.notifyStateChanged 532"),
                List.of("put w1 ICONIFIED -> MAXIMIZED (deiconified while maximized: MAXIMIZE)",
                        "w1.notifyStateChanged 532"));
        for (List<String> step : steps) {
            List<String> lines = section(step.get(0));
            List<String> expected = step.subList(1, step.size());
            assertEquals(expected, lines.subList(0, Math.min(lines.size(), expected.size())), step.get(0) + ":\n"
                    + String.join("\n", lines));
        }
    }

    /** A throwing {@code REPAINT} of {@code RESTORE} is reported, and {@code notify_state} returns before the state. */
    @Test
    public void aThrowingRepaintSkipsTheStateChange() {
        List<String> lines = section("put w1 ICONIFIED -> 0 with v1's REPAINT throwing");
        assertEquals(List.of("v1.view REPAINT !throws", "reported ArmedException: \"armed v1.view REPAINT\""), lines);
    }

    /** {@code notify_on_top}: the level, and only where the C calls it. */
    @Test
    public void aboveChangesTheLevel() {
        List<String> set = section("put w1 ABOVE -> ABOVE (on top)");
        assertEquals(List.of("w1.notifyLevelChanged " + Window.Level.FLOATING, "w1.handler level "
                + Window.Level.FLOATING), set);
        List<String> cleared = section("put w1 ABOVE -> 0 (no longer on top)");
        assertEquals(List.of("w1.notifyLevelChanged " + Window.Level.NORMAL, "w1.handler level "
                + Window.Level.NORMAL), cleared);
        for (String step : List.of("put w1 ABOVE -> ABOVE again (already on top)",
                "put w3 ABOVE -> 0 (owned by w1, which is on top: kept above)",
                "put w1 ICONIFIED|ABOVE -> ICONIFIED|ABOVE (the state wins)")) {
            List<String> lines = section(step);
            assertTrue(lines.stream().noneMatch(line -> line.contains("notifyLevelChanged")), step + ":\n"
                    + String.join("\n", lines));
        }
    }

    /** A throwing {@code notifyStateChanged} or {@code notifyLevelChanged} is reported once. */
    @Test
    public void throwingStateAndLevelChangesAreReported() {
        List<String> state = section("put w1 ICONIFIED -> ICONIFIED with notifyStateChanged throwing");
        assertTrue(state.contains("w1.notifyStateChanged 531 !throws"), String.join("\n", state));
        assertEquals(1, state.stream().filter(line -> line.startsWith("reported ArmedException: ")).count());
        List<String> level = section("put w1 ABOVE -> ABOVE with notifyLevelChanged throwing");
        assertEquals(List.of("w1.notifyLevelChanged " + Window.Level.FLOATING + " !throws",
                "reported ArmedException: \"armed w1.notifyLevelChanged " + Window.Level.FLOATING + "\""), level);
    }

    private static List<String> section(String step) {
        List<String> raw = GtkTraceGolden.rawTrace(run);
        int start = raw.indexOf("== " + step);
        assertTrue(start >= 0, "no step '" + step + "'");
        List<String> lines = new ArrayList<>();
        for (int i = start + 1; i < raw.size() && !raw.get(i).startsWith("== "); i++) {
            lines.add(raw.get(i));
        }
        return lines;
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    /** Runs in {@link GtkGlassChild}. */
    static void windowStateScenario(Map<String, String> out) throws Exception {
        GtkEventTrace t = new GtkEventTrace();
        try {
            windowStateScenario(t, out);
        } finally {
            out.put(GtkTraceGolden.TRACE_KEY, String.join("\n", t.lines()));
        }
    }

    private static void windowStateScenario(GtkEventTrace t, Map<String, String> out) throws Exception {
        GtkTraceGolden.recordMachine(out);
        GtkGlassChild.onFx(() -> {
            t.installApplicationRecorders();
            return null;
        });
        GtkEventTrace.ViewHooks plain = new GtkEventTrace.ViewHooks();

        t.step("create and show w1 (minimizable, maximizable) with v1 at 100,100 300x200");
        GtkTraceWindow w1 = GtkGlassChild.onFx(() -> t.window("w1", null, STYLE, "v1", plain));
        t.act(() -> w1.setBounds(100, 100, true, true, -1, -1, 300, 200, 0, 0));
        t.act(() -> w1.setVisible(true));
        t.step("create and show w2 (neither minimizable nor maximizable) with v2 at 600,100 300x200");
        GtkTraceWindow w2 = GtkGlassChild.onFx(() -> t.window("w2", null, Window.TITLED | Window.CLOSABLE, "v2",
                plain));
        t.act(() -> w2.setBounds(600, 100, true, true, -1, -1, 300, 200, 0, 0));
        t.act(() -> w2.setVisible(true));
        t.step("create and show w3 (owned by w1, no view) at 100,500 200x100");
        GtkTraceWindow w3 = GtkGlassChild.onFx(() -> t.window("w3", w1, STYLE, null, plain));
        t.act(() -> w3.setBounds(100, 500, true, true, -1, -1, 200, 100, 0, 0));
        t.act(() -> w3.setVisible(true));

        put(t, "w1", w1, "ICONIFIED -> ICONIFIED (iconified)", STATE_ICONIFIED, STATE_ICONIFIED);
        put(t, "w1", w1, "ICONIFIED -> 0 (deiconified)", STATE_ICONIFIED, 0);
        put(t, "w1", w1, "MAXIMIZED -> MAXIMIZED (maximized)", STATE_MAXIMIZED, STATE_MAXIMIZED);
        put(t, "w1", w1, "MAXIMIZED -> 0 (unmaximized)", STATE_MAXIMIZED, 0);
        put(t, "w1", w1, "ICONIFIED|MAXIMIZED -> ICONIFIED|MAXIMIZED (iconified while maximized)",
                STATE_ICONIFIED | STATE_MAXIMIZED, STATE_ICONIFIED | STATE_MAXIMIZED);
        put(t, "w1", w1, "ICONIFIED -> MAXIMIZED (deiconified while maximized: MAXIMIZE)", STATE_ICONIFIED,
                STATE_MAXIMIZED);
        put(t, "w1", w1, "MAXIMIZED -> 0 (unmaximized again)", STATE_MAXIMIZED, 0);
        put(t, "w2", w2, "ICONIFIED -> ICONIFIED (iconified)", STATE_ICONIFIED, STATE_ICONIFIED);
        put(t, "w2", w2, "ICONIFIED -> 0 (deiconified; no MINIMIZABLE: the window functions are set again)",
                STATE_ICONIFIED, 0);
        put(t, "w3", w3, "ICONIFIED -> ICONIFIED (iconified; no view)", STATE_ICONIFIED, STATE_ICONIFIED);
        put(t, "w3", w3, "ICONIFIED -> 0 (deiconified; no view)", STATE_ICONIFIED, 0);

        put(t, "w1", w1, "ABOVE -> ABOVE (on top)", STATE_ABOVE, STATE_ABOVE);
        put(t, "w1", w1, "ABOVE -> ABOVE again (already on top)", STATE_ABOVE, STATE_ABOVE);
        put(t, "w3", w3, "ABOVE -> 0 (owned by w1, which is on top: kept above)", STATE_ABOVE, 0);
        put(t, "w1", w1, "ABOVE -> 0 (no longer on top)", STATE_ABOVE, 0);
        put(t, "w1", w1, "ICONIFIED|ABOVE -> ICONIFIED|ABOVE (the state wins)", STATE_ICONIFIED | STATE_ABOVE,
                STATE_ICONIFIED | STATE_ABOVE);
        put(t, "w1", w1, "ICONIFIED -> ABOVE (deiconified, still above)", STATE_ICONIFIED, STATE_ABOVE);
        put(t, "w1", w1, "ABOVE -> 0 (not above any more, the level never changed)", STATE_ABOVE, 0);

        put(t, "w1", w1, "ICONIFIED -> ICONIFIED (iconified again)", STATE_ICONIFIED, STATE_ICONIFIED);
        t.arm("v1.view REPAINT", 1);
        put(t, "w1", w1, "ICONIFIED -> 0 with v1's REPAINT throwing", STATE_ICONIFIED, 0);
        t.arm("w1.notifyStateChanged", 1);
        put(t, "w1", w1, "ICONIFIED -> ICONIFIED with notifyStateChanged throwing", STATE_ICONIFIED, STATE_ICONIFIED);
        put(t, "w1", w1, "ICONIFIED -> 0 (deiconified after the throws)", STATE_ICONIFIED, 0);
        t.arm("w1.notifyLevelChanged", 1);
        put(t, "w1", w1, "ABOVE -> ABOVE with notifyLevelChanged throwing", STATE_ABOVE, STATE_ABOVE);
        put(t, "w1", w1, "ABOVE -> 0 (after the throw)", STATE_ABOVE, 0);

        t.step("close w3, w2, w1");
        t.act(w3::close);
        t.act(w2::close);
        t.act(w1::close);
    }

    /**
     * A step {@code put <name> <what>}: one {@code GdkEventWindowState} with {@code changed} and {@code state} for
     * {@code window}, queued on the FX thread; then the scenario settles.
     */
    private static void put(GtkEventTrace t, String name, GtkTraceWindow window, String what, int changed,
                            int state) throws Exception {
        t.step("put " + name + " " + what);
        t.act(() -> GtkSyntheticEvents.putWindowState(window.getNativeWindow(), changed, state));
    }
}
