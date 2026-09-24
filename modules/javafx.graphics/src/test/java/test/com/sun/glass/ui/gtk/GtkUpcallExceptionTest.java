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
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.glass.ui.gtk.GtkTraceWindow;
import java.util.List;
import java.util.Map;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the GTK glass event code does when the Java target of an upcall throws, as the JNI build of commit
 * {@code 033187ad90} did it: for each group of upcalls that becomes a callback table - window, view, application -
 * a recorder armed to throw once ({@link GtkEventTrace#arm}), and the trace of what reached Java next. The
 * drag-and-drop table's exception paths are in {@link GtkDnDTraceTest}.
 * <p>
 * Where the C tested the exception with {@code CHECK_JNI_EXCEPTION} it reported it through
 * {@code Application.reportException} and returned early; with {@code EXCEPTION_OCCURED} / {@code LOG_EXCEPTION}
 * it reported it and carried on; where it did not test it at all ({@code WindowContextBase::set_view}) the exception
 * stayed pending and was thrown out of the native method that made the upcall. The golden pins all three:
 * <ul>
 * <li>{@code notifyMove} thrown from {@code set_bounds}: reported, and the view's {@code MOVE} that follows it in
 * {@code notify_window_move} never comes;</li>
 * <li>{@code isEnabled} thrown from {@code process_events}: reported, and the event it was asked about is dropped
 * ({@code CallBooleanMethod} answered false);</li>
 * <li>{@code notifyResize} thrown from {@code set_bounds} and then from {@code process_configure}: reported both
 * times, and neither the view's {@code RESIZE} nor - after the second - the geometry update, the window
 * {@code notifyMove} and the screen check run;</li>
 * <li>{@code notifyClose}, {@code notifyFocus} and {@code notifyDestroy}: reported; destruction carries on;</li>
 * <li>the right button's {@code DOWN}: reported, and the {@code notifyMenu} after it never comes; a key's
 * {@code PRESS}: reported, and its {@code TYPED} never comes;</li>
 * <li>the view's {@code ADD} thrown from {@code GtkView._setParent}: reported inside the native method;</li>
 * <li>the old view's {@code EXIT} thrown from {@code set_view}, which does not test it: the exception is thrown
 * out of {@code Window.setView} after the C has taken the new view, while the Java window keeps the old one - so
 * the next swap detaches an already detached view, which {@code GtkView._setParent} reports as {@code ADD}, and
 * the C sends its {@code EXIT} to the view Java never attached;</li>
 * <li>{@code Screen.notifySettingsChanged} thrown from {@code screen_settings_changed}: reported, the C carries
 * on.</li>
 * </ul>
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkUpcallExceptionTest {

    static final String SCENARIO = "gtk-upcall-exceptions";

    /**
     * The normalisation: {@link GtkTraceGolden#REPAINT_LAST_IN_STEP}, the rule of the event trace. Five runs of
     * this scenario on the JNI build recorded identical traces; the rule is applied because the race it removes -
     * a frame-clock expose against an X event of the same step - exists here as well.
     */
    static final List<GtkTraceGolden.Rule> RULES = List.of(GtkTraceGolden.REPAINT_LAST_IN_STEP);

    static final List<String> LEGEND = List.of(
            "Lines as in the event trace; '!throws' marks the recorded call that threw into the C, 'reported'",
            "what Application.reportException delivered to the FX thread's uncaught-exception handler.");

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(10 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireRobot();
        GtkTraceGolden.requireNoWindowManager(GtkUpcallExceptionTest.class);
        run = GtkTraceGolden.runRepeated(GtkUpcallExceptionTest.class, "exceptionScenario",
                List.of("-DUSE_ROBOT=true"), SCENARIO, RULES);
    }

    @AfterAll
    static void oracleRan() {
        ParityGate.ledger(GtkUpcallExceptionTest.class).assertOracleRan();
    }

    /** The normalised trace equals the golden captured from the JNI build (no window manager). */
    @Test
    public void traceMatchesTheJniGolden() {
        List<String> raw = GtkTraceGolden.rawTrace(run);
        GtkTraceGolden.verify(GtkUpcallExceptionTest.class, SCENARIO, run, raw,
                GtkTraceGolden.normalise(raw, RULES), LEGEND);
    }

    /** Every armed call threw, and every exception the C tested was reported exactly once, on the FX thread. */
    @Test
    public void everyCaughtThrowIsReportedOnce() {
        List<String> raw = GtkTraceGolden.rawTrace(run);
        long thrown = raw.stream().filter(line -> line.endsWith(" !throws")).count();
        long reported = raw.stream().filter(line -> line.startsWith("reported ArmedException: ")).count();
        long escaped = raw.stream().filter(line -> line.startsWith("# threw ArmedException")).count();
        assertEquals(12, thrown, "armed calls that threw");
        assertEquals(thrown, reported + escaped, "every throw is either reported or thrown out of the native");
        assertEquals(1, escaped, "only set_view leaves the exception pending");
        for (String line : raw) {
            assertTrue(!line.contains("!off-fx-thread"), line);
        }
    }

    /** A throw from a call the C returns early after hides what the C would have called next. */
    @Test
    public void anEarlyReturnSkipsTheRestOfTheCFunction() {
        List<String> raw = GtkTraceGolden.rawTrace(run);
        String menu = section(raw, "right button with DOWN throwing");
        assertTrue(menu.contains("v1.mouse DOWN BUTTON_RIGHT"), menu);
        assertTrue(!menu.contains("v1.menu "), "notifyMenu after a throwing DOWN:\n" + menu);
        String key = section(raw, "key a with PRESS throwing");
        assertTrue(key.contains("v1.key PRESS 0x41"), key);
        assertTrue(!key.contains("v1.key TYPED"), "TYPED after a throwing PRESS:\n" + key);
        // set_bounds returned after the throwing notifyMove: the next view MOVE comes only after the next
        // notifyMove, which process_configure makes
        List<String> move = List.of(section(raw, "setPosition with notifyMove throwing").split("\n"));
        int thrown = move.indexOf("w1.notifyMove 150 130 !throws");
        assertTrue(thrown >= 0, String.join("\n", move));
        assertEquals("reported ArmedException: \"armed w1.notifyMove 150 130\"", move.get(thrown + 1));
        for (int i = thrown + 1; i < move.size() && !move.get(i).startsWith("w1.notifyMove"); i++) {
            assertTrue(!move.get(i).startsWith("v1.view MOVE"), "view MOVE after a throwing notifyMove:\n"
                    + String.join("\n", move));
        }
    }

    /**
     * Swaps the view of {@code window} for a new recorded view {@code name}; a throw out of {@code Window.setView}
     * is recorded as {@code # threw ...}.
     */
    private static void swapView(GtkEventTrace t, Window window, String name, GtkEventTrace.ViewHooks hooks)
            throws Exception {
        t.act(() -> {
            View view = Application.GetApplication().createView();
            view.setEventHandler(t.viewHandler(name, hooks));
            try {
                window.setView(view);
                t.note("setView returned");
            } catch (GtkEventTrace.ArmedException e) {
                t.note("threw ArmedException out of Window.setView: " + e.getMessage());
            }
            t.note("the window's view is " + name + ": " + (window.getView() == view));
        });
    }

    private static String section(List<String> raw, String step) {
        int start = raw.indexOf("== " + step);
        assertTrue(start >= 0, "no step '" + step + "'");
        StringBuilder sb = new StringBuilder();
        for (int i = start + 1; i < raw.size() && !raw.get(i).startsWith("== "); i++) {
            sb.append(raw.get(i)).append('\n');
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    private static final int STYLE = Window.TITLED | Window.CLOSABLE | Window.MINIMIZABLE | Window.MAXIMIZABLE;

    /** Runs in {@link GtkGlassChild}. */
    static void exceptionScenario(Map<String, String> out) throws Exception {
        GtkEventTrace t = new GtkEventTrace();
        try {
            exceptionScenario(t, out);
        } finally {
            out.put(GtkTraceGolden.TRACE_KEY, String.join("\n", t.lines()));
        }
    }

    private static void exceptionScenario(GtkEventTrace t, Map<String, String> out) throws Exception {
        GtkTraceGolden.recordMachine(out);
        GtkGlassChild.onFx(() -> {
            t.installApplicationRecorders();
            return null;
        });
        GtkEventTrace.ViewHooks plain = new GtkEventTrace.ViewHooks();

        t.step("create and show w1 with v1 at 100,100 300x200, w2 with v2 at 600,100 300x200");
        GtkTraceWindow w1 = GtkGlassChild.onFx(() -> t.window("w1", null, STYLE, "v1", plain));
        t.act(() -> w1.setBounds(100, 100, true, true, -1, -1, 300, 200, 0, 0));
        t.act(() -> w1.setVisible(true));
        GtkTraceWindow w2 = GtkGlassChild.onFx(() -> t.window("w2", null, STYLE, "v2", plain));
        t.act(() -> w2.setBounds(600, 100, true, true, -1, -1, 300, 200, 0, 0));
        t.act(() -> w2.setVisible(true));
        t.act(() -> GtkGlassShim.setInputFocus(w1.getNativeWindow()));

        // window table
        t.step("setPosition with notifyMove throwing");
        t.arm("w1.notifyMove", 1);
        t.act(() -> w1.setPosition(150, 130));
        t.step("pointer into v1 with isEnabled throwing");
        t.arm("w1.isEnabled", 1);
        t.mouseMove(200, 200);
        t.mouseMove(201, 200);
        t.step("setSize with w1.notifyResize throwing twice (set_bounds, then process_configure)");
        t.arm("w1.notifyResize", 2);
        t.act(() -> w1.setSize(320, 220));
        t.step("focus to w2 with w1's notifyFocus throwing");
        t.arm("w1.notifyFocus", 1);
        t.act(() -> GtkGlassShim.setInputFocus(w2.getNativeWindow()));
        t.step("close request with notifyClose throwing");
        t.arm("w1.notifyClose", 1);
        t.act(() -> GtkGlassShim.sendDeleteRequest(w1.getNativeWindow()));

        // view table
        t.step("right button with DOWN throwing");
        t.act(() -> GtkGlassShim.setInputFocus(w1.getNativeWindow()));
        t.arm("v1.mouse DOWN BUTTON_RIGHT", 1);
        t.click(MouseButton.SECONDARY);
        t.step("key a with PRESS throwing");
        t.arm("v1.key PRESS 0x41", 1);
        t.type(KeyCode.A);
        t.step("w2 view swap v2 -> v2b with v2's EXIT throwing (set_view does not test it)");
        t.mouseMove(700, 200);
        t.arm("v2.mouse EXIT", 1);
        swapView(t, w2, "v2b", plain);
        t.step("w2 view swap -> v2c with v2c's ADD throwing (GtkView._setParent tests it)");
        t.arm("v2c.view ADD", 1);
        swapView(t, w2, "v2c", plain);

        // application table
        t.step("_NET_WORKAREA set with Screen.notifySettingsChanged throwing, then removed");
        t.arm("screen.handleSettingsChanged", 1);
        t.act(() -> GtkGlassShim.setRootCardinals("_NET_WORKAREA", new long[] {0, 0, 1800, 1000}));
        t.act(() -> GtkGlassShim.setRootCardinals("_NET_WORKAREA", null));

        t.step("close w2 with notifyDestroy throwing, then w1");
        t.arm("w2.notifyDestroy", 1);
        t.act(w2::close);
        t.act(w1::close);
    }
}
