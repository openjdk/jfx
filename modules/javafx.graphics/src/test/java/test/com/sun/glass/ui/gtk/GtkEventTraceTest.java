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
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.glass.ui.gtk.GtkTraceWindow;
import com.sun.javafx.tk.HeaderAreaType;
import java.nio.ByteBuffer;
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
 * The boundary trace of the GTK glass event code on a running toolkit: every call {@code glass_window.cpp},
 * {@code glass_window_ime.cpp}, {@code GlassView.cpp}, {@code glass_screen.cpp} and {@code glass_general.cpp}
 * made into Java at commit {@code 033187ad90}, with its arguments, for one scripted scenario driven by the glass
 * robot and a few X11 requests a window manager or a pager would send. The golden is that stream, captured from the
 * JNI build; the replacement of the JNI boundary must reproduce it line for line.
 * <p>
 * The scenario covers key press, release and typed text with and without modifiers, a key held down, the menu
 * key, an input-method composition, mouse enter, exit, move, every button, a double click, a drag, both wheel
 * axes, window creation, show, move, resize, focus in and out, a disabled window, a close request, destruction
 * of an owner with an owned window, the level, iconify, maximize and full-screen requests, the icon, a view swap,
 * the screen settings notification and an {@code EXTENDED} window's non-client hit tests; a second monitor is
 * only moved to where the display has one, and a display with two monitors has a golden of its own
 * ({@link GtkTraceGolden#fileName}): there {@code notifyMoveToAnotherScreen} carries the {@code Screen} of the other
 * monitor, which the JNI build made in {@code createJavaScreen} of {@code glass_screen.cpp}. The goldens are for an
 * X server without a window manager: under one the
 * window manager places, reparents, focuses and restacks windows asynchronously and the trace differs from run to
 * run ({@link GtkTraceGolden#exact}), so there only the state changes that need a window manager are checked
 * ({@link #windowManagerUpcallsNeedAWindowManager}).
 * <p>
 * Normalisation ({@link #RULES}): the trace contains no time stamps and no pointers by construction (windows and
 * views are named, screens are described by value), and every scripted step waits until the X server and the GTK
 * main loop are quiet ({@link GtkEventTrace#settle}). Twelve runs of the JNI build were byte-identical; the next
 * ones put a frame-clock {@code REPAINT} before or after an X property notification of the same step, which
 * {@link GtkTraceGolden#REPAINT_LAST_IN_STEP} removes - the only rule. Two settings keep what GDK makes of the input
 * independent of how long the steps take to settle on a loaded machine: {@link #DOUBLE_CLICK_MILLIS} and
 * {@link #AUTOREPEAT_DELAY_MILLIS}.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkEventTraceTest {

    static final String SCENARIO = "gtk-event-trace";

    /** The normalisation of the trace (see the class comment). */
    static final List<GtkTraceGolden.Rule> RULES = List.of(GtkTraceGolden.REPAINT_LAST_IN_STEP);

    static final List<String> LEGEND = List.of(
            "Lines: wN.<call> = a GtkTraceWindow override (the C's call into the window, its arguments);",
            "wN.handler = Window.EventHandler; vN.<event> = View.EventHandler; screen./app. = the toolkit's",
            "handlers; reported = Application.reportException; == step; # value read back by the scenario.");

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(10 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireRobot();
        run = GtkTraceGolden.runRepeated(GtkEventTraceTest.class, "traceScenario", List.of("-DUSE_ROBOT=true"),
                SCENARIO, RULES);
    }

    @AfterAll
    static void oracleRan() {
        ParityGate.ledger(GtkEventTraceTest.class).assertOracleRan();
    }

    static List<String> normalised() {
        return GtkTraceGolden.normalise(GtkTraceGolden.rawTrace(run), RULES);
    }

    /** The normalised trace equals the golden captured from the JNI build (no window manager). */
    @Test
    public void traceMatchesTheJniGolden() {
        GtkTraceGolden.verify(GtkEventTraceTest.class, SCENARIO, run, GtkTraceGolden.rawTrace(run), normalised(),
                LEGEND);
    }

    /** Every upcall of the event code arrives on the FX thread, the GTK main-loop thread. */
    @Test
    public void everyUpcallArrivesOnTheEventThread() {
        for (String line : GtkTraceGolden.rawTrace(run)) {
            assertTrue(!line.contains("!off-fx-thread"), line);
        }
    }

    /**
     * The scenario reached the upcalls it exists for, on any display: a trace that silently lost a whole kind of
     * event would otherwise only show as a golden difference, or not at all where no golden applies.
     */
    @Test
    public void theScenarioReachesEveryKindOfUpcall() {
        String trace = String.join("\n", GtkTraceGolden.rawTrace(run));
        for (String expected : List.of("w1.isEnabled=true", "w1.notifyResize ", "w1.notifyMove ", "w1.notifyFocus ",
                "w1.notifyDestroy", "w2.notifyClose", "w3.notifyDestroy", "w2.notifyFocusUngrab",
                "w1.notifyFocusDisabled", "w4.notifyFocusUngrab",
                "v1.view ADD", "v1.view REPAINT", "v1.view RESIZE", "v1.view MOVE", "v1.mouse ENTER",
                "v1.mouse EXIT", "v1.mouse MOVE", "v1.mouse DOWN BUTTON_LEFT", "v1.mouse UP BUTTON_LEFT",
                "v1.mouse DRAG BUTTON_LEFT", "v1.mouse DOWN BUTTON_RIGHT", "v1.menu ", "v1.scroll ", "v1.key PRESS",
                "v1.key TYPED", "v1.key RELEASE", "v1.key PRESS 0x20d",
                "v1.dragStart ", "v2.view FULLSCREEN_ENTER", "v2.view FULLSCREEN_EXIT", "v4.pickHeaderArea ",
                "screen.handleSettingsChanged", "v1.imCandidatePos ", "v1.im text=")) {
            assertTrue(trace.contains(expected), "the trace has no '" + expected + "'");
        }
    }

    /**
     * The state changes only a window manager makes arrive exactly where one runs: the C's {@code notify_state}
     * reports RESTORE ({@code GtkWindow.notifyStateChanged} 533) after a deiconify or an unmaximize, while
     * {@code GtkWindow._minimize} and {@code _maximize} notify MINIMIZE and MAXIMIZE from Java with or without one.
     * {@code notifyLevelChanged} arrives in neither case: under openbox the window manager sets and clears
     * {@code _NET_WM_STATE_ABOVE} (the trace records the property), but no upcall follows.
     */
    @Test
    public void windowManagerUpcallsNeedAWindowManager() {
        List<String> trace = GtkTraceGolden.rawTrace(run);
        boolean wm = !GtkTraceGolden.exact(run);
        assertEquals(wm, trace.contains("w2.notifyStateChanged 533"), "RESTORE from the C, window manager: " + wm);
        assertTrue(trace.stream().noneMatch(line -> line.startsWith("w2.notifyLevelChanged")), "notifyLevelChanged");
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    private static final int STYLE = Window.TITLED | Window.CLOSABLE | Window.MINIMIZABLE | Window.MAXIMIZABLE;

    /**
     * Where a window's content starts on the screen, as the X server has it: the glass geometry is one of the things
     * the trace records, and under a window manager it is updated asynchronously, so the robot does not aim by it.
     */
    private static int[] origin(Window window) throws Exception {
        return GtkGlassChild.onFx(() -> GtkGlassShim.rootPosition(window.getNativeWindow()));
    }

    /**
     * The double-click time of the scenario, in ms. GDK synthesizes a {@code GDK_3BUTTON_PRESS} - one more
     * {@code isEnabled} in the trace - for the press of the "control click" step only when it comes within twice the
     * double-click time of the first press of the "double click" step; the golden has it, captured with GTK's 400
     * ms and an idle machine, and a loaded one took more than those 800 ms. 10 s makes it certain; no other two
     * presses of the scenario are of the same button in the same place of the same window.
     */
    static final int DOUBLE_CLICK_MILLIS = 10_000;

    /**
     * The X11 autorepeat delay of the scenario, in ms: no key is held that long between its press and its release,
     * however slowly the steps settle, except in the autorepeat step, which sets its own.
     */
    static final int AUTOREPEAT_DELAY_MILLIS = 60_000;

    /** Runs in {@link GtkGlassChild}. */
    static void traceScenario(Map<String, String> out) throws Exception {
        GtkEventTrace t = new GtkEventTrace();
        int[] repeat = GtkGlassChild.onFx(() -> {
            GtkGlassShim.setGtkSettingInt("gtk-double-click-time", DOUBLE_CLICK_MILLIS);
            return GtkGlassShim.setAutoRepeatRate(AUTOREPEAT_DELAY_MILLIS, AUTOREPEAT_DELAY_MILLIS);
        });
        try {
            traceScenario(t, out);
        } finally {
            out.put(GtkTraceGolden.TRACE_KEY, String.join("\n", t.lines()));
            GtkGlassChild.onFx(() -> GtkGlassShim.setAutoRepeatRate(repeat[0], repeat[1]));
        }
    }

    private static void traceScenario(GtkEventTrace t, Map<String, String> out) throws Exception {
        GtkTraceGolden.recordMachine(out);
        // the input-method steps compose with GTK's built-in simple context (Ctrl+Shift+U), which needs no daemon
        GtkGlassShim.setenv("GTK_IM_MODULE", "gtk-im-context-simple");
        GtkGlassChild.onFx(() -> {
            t.installApplicationRecorders();
            return null;
        });
        GtkEventTrace.ViewHooks plain = new GtkEventTrace.ViewHooks();

        t.step("create w1 (TITLED) with v1");
        GtkTraceWindow w1 = GtkGlassChild.onFx(() -> t.window("w1", null, STYLE, "v1", plain));
        t.settle();
        t.step("w1 bounds 100,100 300x200");
        t.act(() -> w1.setBounds(100, 100, true, true, 300, 200, -1, -1, 0, 0));
        t.step("w1 show");
        t.act(() -> w1.setVisible(true));
        t.note("w1 WM_CLASS " + GtkGlassChild.onFx(() -> GtkGlassShim.wmClass(w1.getNativeWindow())));
        t.step("w1 requestFocus");
        t.act(() -> w1.requestFocus());

        t.step("create w2 (UNTITLED) with v2 at 600,100 300x200 and show");
        GtkTraceWindow w2 = GtkGlassChild.onFx(() -> t.window("w2", null,
                Window.UNTITLED | Window.CLOSABLE | Window.MINIMIZABLE | Window.MAXIMIZABLE, "v2", plain));
        t.act(() -> w2.setBounds(600, 100, true, true, -1, -1, 300, 200, 0, 0));
        t.act(() -> w2.setVisible(true));
        t.step("w2 requestFocus");
        t.act(() -> w2.requestFocus());
        t.step("w1 requestFocus again");
        t.act(() -> w1.requestFocus());

        t.step("w1 move to 150,130");
        t.act(() -> w1.setPosition(150, 130));
        t.step("w1 resize to 320x220");
        t.act(() -> w1.setSize(320, 220));
        t.step("w1 content size 300x200");
        t.act(() -> w1.setContentSize(300, 200));
        t.step("w1 bounds with gravity");
        t.act(() -> w1.setBounds(160, 140, true, true, 310, 210, -1, -1, 0.5f, 0.5f));
        t.step("v1 repaint request");
        t.act(() -> w1.getView().scheduleRepaint());

        int[] o1 = origin(w1);
        int[] o2 = origin(w2);
        t.step("pointer into v1, moves inside");
        t.mouseMove(o1[0] + 20, o1[1] + 30);
        t.mouseMove(o1[0] + 40, o1[1] + 50);
        t.mouseMove(o1[0] + 41, o1[1] + 50);
        t.step("pointer from v1 into v2 and back");
        t.mouseMove(o2[0] + 10, o2[1] + 10);
        t.mouseMove(o1[0] + 40, o1[1] + 50);

        t.step("buttons");
        for (MouseButton button : List.of(MouseButton.PRIMARY, MouseButton.MIDDLE, MouseButton.BACK,
                MouseButton.FORWARD)) {
            t.click(button);
        }
        t.step("right button: menu");
        t.click(MouseButton.SECONDARY);
        t.step("double click");
        GlassRobotActions.doubleClick(t);
        t.step("control click");
        t.keyPress(KeyCode.CONTROL);
        t.click(MouseButton.PRIMARY);
        t.keyRelease(KeyCode.CONTROL);

        t.step("drag inside v1, out of w1, release outside");
        t.mousePress(MouseButton.PRIMARY);
        t.mouseMove(o1[0] + 60, o1[1] + 60);
        t.mouseMove(o1[0] + 80, o1[1] + 70);
        t.mouseMove(o2[0] + 50, o2[1] + 50);
        t.mouseRelease(MouseButton.PRIMARY);
        t.mouseMove(o1[0] + 40, o1[1] + 50);

        t.step("wheel: vertical");
        t.wheel(1);
        t.wheel(-1);
        t.step("wheel: horizontal (X11 buttons 6 and 7)");
        GlassRobotActions.button(t, 6);
        GlassRobotActions.button(t, 7);
        t.step("wheel: vertical with shift (the C swaps the axes)");
        t.keyPress(KeyCode.SHIFT);
        t.wheel(1);
        t.keyRelease(KeyCode.SHIFT);

        t.step("keys: a, shift+a, ctrl+a, alt+a");
        t.type(KeyCode.A);
        t.type(KeyCode.SHIFT, KeyCode.A);
        t.type(KeyCode.CONTROL, KeyCode.A);
        t.type(KeyCode.ALT, KeyCode.A);
        t.step("keys: typed text 'Hi 7!'");
        t.type(KeyCode.SHIFT, KeyCode.H);
        t.type(KeyCode.I);
        t.type(KeyCode.SPACE);
        t.type(KeyCode.DIGIT7);
        t.type(KeyCode.SHIFT, KeyCode.DIGIT1);
        t.step("keys: F1, arrows, enter, escape, backspace, tab, delete, home");
        for (KeyCode code : List.of(KeyCode.F1, KeyCode.LEFT, KeyCode.UP, KeyCode.ENTER, KeyCode.ESCAPE,
                KeyCode.BACK_SPACE, KeyCode.TAB, KeyCode.DELETE, KeyCode.HOME)) {
            t.type(code);
        }
        t.step("keys: menu key (keysym Menu)");
        t.act(() -> t.note("Menu keycode " + GtkGlassShim.fakeKeysym("Menu", true)));
        t.act(() -> GtkGlassShim.fakeKeysym("Menu", false));
        t.step("keys: b held down for 3 s, one X11 autorepeat (delay 100 ms, interval 60 s)");
        int[] repeat = GtkGlassChild.onFx(() -> GtkGlassShim.setAutoRepeatRate(100, 60_000));
        try {
            t.keyPress(KeyCode.B);
            Thread.sleep(3000);
            t.keyRelease(KeyCode.B);
        } finally {
            GtkGlassChild.onFx(() -> GtkGlassShim.setAutoRepeatRate(repeat[0], repeat[1]));
        }

        t.step("input method: enable, Ctrl+Shift+U e 9 space commits U+00E9, then a plain key");
        t.act(() -> w1.getView().enableInputMethodEvents(true));
        t.keyPress(KeyCode.CONTROL);
        t.keyPress(KeyCode.SHIFT);
        t.type(KeyCode.U);
        t.keyRelease(KeyCode.SHIFT);
        t.keyRelease(KeyCode.CONTROL);
        t.type(KeyCode.E);
        t.type(KeyCode.DIGIT9);
        t.type(KeyCode.SPACE);
        t.type(KeyCode.C);
        t.step("input method: disable");
        t.act(() -> w1.getView().enableInputMethodEvents(false));

        t.step("w1 view swap: v1 -> v1b");
        t.act(() -> {
            View v1b = Application.GetApplication().createView();
            v1b.setEventHandler(t.viewHandler("v1b", plain));
            try {
                w1.setView(v1b);
            } catch (RuntimeException e) {
                t.note("setView threw " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        t.step("X11 focus to w2, then w1 disabled: a click, the X11 focus and a close request");
        t.act(() -> GtkGlassShim.setInputFocus(w2.getNativeWindow()));
        t.act(() -> w1.setEnabled(false));
        t.mouseMove(o1[0] + 30, o1[1] + 30);
        t.click(MouseButton.PRIMARY);
        t.act(() -> GtkGlassShim.setInputFocus(w1.getNativeWindow()));
        t.act(() -> GtkGlassShim.sendDeleteRequest(w1.getNativeWindow()));
        t.act(() -> w1.setEnabled(true));

        t.step("w2 close request");
        t.act(() -> GtkGlassShim.sendDeleteRequest(w2.getNativeWindow()));

        t.step("w2 X11 focus, grab, a click outside every window ends the grab, ungrab");
        t.act(() -> GtkGlassShim.setInputFocus(w2.getNativeWindow()));
        t.act(() -> {
            try {
                t.note("w2 grabFocus " + w2.grabFocus());
            } catch (IllegalStateException e) {
                t.note("w2 grabFocus threw " + e.getMessage());
            }
        });
        t.mouseMove(1500, 900);
        t.click(MouseButton.PRIMARY);
        t.act(w2::ungrabFocus);

        t.step("w2 toFront, toBack");
        t.act(w2::toFront);
        t.act(w2::toBack);

        t.step("w2 level FLOATING, then above removed by the window manager");
        t.act(() -> w2.setLevel(Window.Level.FLOATING));
        t.note("w2 _NET_WM_STATE " + GtkGlassChild.onFx(() -> GtkGlassShim.netWmState(w2.getNativeWindow())));
        t.act(() -> GtkGlassShim.requestNetWmState(w2.getNativeWindow(), false, "_NET_WM_STATE_ABOVE"));
        t.note("w2 _NET_WM_STATE " + GtkGlassChild.onFx(() -> GtkGlassShim.netWmState(w2.getNativeWindow())));
        t.act(() -> GtkGlassShim.requestNetWmState(w2.getNativeWindow(), true, "_NET_WM_STATE_ABOVE"));
        t.note("w2 _NET_WM_STATE " + GtkGlassChild.onFx(() -> GtkGlassShim.netWmState(w2.getNativeWindow())));
        t.act(() -> w2.setLevel(Window.Level.NORMAL));
        t.note("w2 _NET_WM_STATE " + GtkGlassChild.onFx(() -> GtkGlassShim.netWmState(w2.getNativeWindow())));

        t.step("w2 iconify and deiconify");
        t.act(() -> w2.minimize(true));
        t.act(() -> w2.minimize(false));
        t.step("w2 maximize and restore");
        t.act(() -> w2.maximize(true));
        t.act(() -> w2.maximize(false));
        t.step("v2 fullscreen enter and exit");
        t.act(() -> w2.getView().enterFullscreen(false, false, false));
        t.act(() -> w2.getView().exitFullscreen(false));

        t.step("w2 icon");
        t.act(() -> w2.setIcon(icon()));
        t.note("w2 _NET_WM_ICON " + GtkGlassChild.onFx(() -> GtkGlassShim.netWmIcon(w2.getNativeWindow())));

        // one step each: an expose from the frame clock and the property notifications of another call made in the
        // same step can arrive in either order
        t.step("w2 alpha");
        t.act(() -> w2.setAlpha(0.5f));
        t.step("w2 background");
        t.act(() -> w2.setBackground(0.1f, 0.2f, 0.3f));
        t.step("w2 title");
        t.act(() -> w2.setTitle("w2 " + (char) 0xE9));
        t.step("w2 minimum and maximum size");
        t.act(() -> w2.setMinimumSize(100, 80));
        t.act(() -> w2.setMaximumSize(800, 600));
        t.step("w2 not resizable, resizable");
        t.act(() -> w2.setResizable(false));
        t.act(() -> w2.setResizable(true));

        t.step("screen settings: _NET_WORKAREA set and removed");
        t.act(() -> GtkGlassShim.setRootCardinals("_NET_WORKAREA", new long[] {0, 0, 1800, 1000}));
        t.act(() -> GtkGlassShim.setRootCardinals("_NET_WORKAREA", null));

        t.step("second screen");
        int monitors = GtkGlassChild.onFx(GtkGlassShim::gdkMonitorCount);
        if (monitors < 2) {
            t.note("skipped: the display has " + monitors + " monitor");
        } else {
            int[] geometry = GtkGlassChild.onFx(() -> GtkGlassShim.gdkMonitorGeometry(1));
            t.act(() -> w2.setPosition(geometry[0] + 50, geometry[1] + 50));
            t.act(() -> w2.setPosition(100, 400));
        }

        t.step("w2 hide with the pointer inside, show again");
        int[] o2b = origin(w2);
        t.mouseMove(o2b[0] + 30, o2b[1] + 30);
        t.act(() -> w2.setVisible(false));
        t.act(() -> w2.setVisible(true));

        t.step("create w3 owned by w1 (UTILITY, no view) and show");
        GtkTraceWindow w3 = GtkGlassChild.onFx(() -> t.window("w3", w1, Window.TITLED | Window.UTILITY, null,
                plain));
        t.act(() -> w3.setBounds(200, 500, true, true, 200, 100, -1, -1, 0, 0));
        t.act(() -> w3.setVisible(true));
        t.step("close w1: its view leaves, w3 is destroyed with it");
        t.mouseMove(o1[0] + 30, o1[1] + 30);
        t.act(w1::close);

        extendedWindow(t);

        t.step("close w2");
        t.act(w2::close);
    }

    /** The {@code EXTENDED} window: the resize border, the drag area and the non-client hit tests. */
    private static void extendedWindow(GtkEventTrace t) throws Exception {
        // the drag area is the top-left 150x30; clientFirst makes the next hit tests answer the client area, so that
        // the first press of a double click does not start a move drag that would swallow the rest of it
        int[] clientFirst = {0};
        GtkEventTrace.ViewHooks header = new GtkEventTrace.ViewHooks() {
            @Override
            HeaderAreaType headerArea(double x, double y) {
                if (clientFirst[0] > 0) {
                    clientFirst[0]--;
                    return null;
                }
                return y < 30 && x < 150 ? HeaderAreaType.DRAGBAR : null;
            }
        };
        t.step("create w4 (EXTENDED, resizable) with v4 at 500,400 300x200 and show");
        GtkTraceWindow w4 = GtkGlassChild.onFx(() -> t.window("w4", null, Window.EXTENDED | Window.CLOSABLE
                | Window.MINIMIZABLE | Window.MAXIMIZABLE, "v4", header));
        t.act(() -> w4.setBounds(500, 400, true, true, 300, 200, -1, -1, 0, 0));
        t.act(() -> w4.setResizable(true));
        t.act(() -> w4.setVisible(true));
        int[] o4 = origin(w4);
        t.step("w4 pointer: client area, east border, back to the client area, out of the window");
        t.mouseMove(o4[0] + 100, o4[1] + 100);
        t.mouseMove(o4[0] + 298, o4[1] + 100);
        t.mouseMove(o4[0] + 100, o4[1] + 100);
        t.mouseMove(o4[0] + 100, o4[1] + 2);
        t.mouseMove(o4[0] + 100, o4[1] + 100);
        t.mouseMove(o4[0] - 50, o4[1] + 100);
        t.step("w4 press and release on the east border (resize drag)");
        t.mouseMove(o4[0] + 298, o4[1] + 100);
        t.mousePress(MouseButton.PRIMARY);
        t.mouseRelease(MouseButton.PRIMARY);
        t.step("w4 press and release in the drag area (move drag)");
        t.mouseMove(o4[0] + 50, o4[1] + 15);
        t.mousePress(MouseButton.PRIMARY);
        t.mouseRelease(MouseButton.PRIMARY);
        t.step("w4 press and release in the client area");
        t.mouseMove(o4[0] + 100, o4[1] + 100);
        t.click(MouseButton.PRIMARY);
        t.step("w4 double click in the drag area, both presses answered as client area (maximize toggle)");
        t.mouseMove(o4[0] + 50, o4[1] + 15);
        GtkGlassChild.onFx(() -> clientFirst[0] = 2);
        GlassRobotActions.doubleClick(t);
        t.step("close w4");
        t.act(w4::close);
    }

    /** A 2x2 BGRA icon with four distinct opaque colours. */
    private static Pixels icon() {
        byte[] bytes = new byte[2 * 2 * 4];
        for (int i = 0; i < 4; i++) {
            bytes[i * 4] = (byte) (0x20 * i);
            bytes[i * 4 + 1] = (byte) (0x40 + i);
            bytes[i * 4 + 2] = (byte) (0x80 + i);
            bytes[i * 4 + 3] = (byte) 0xFF;
        }
        return Application.GetApplication().createPixels(2, 2, ByteBuffer.wrap(bytes));
    }

    /** Robot sequences that must reach the X server inside one FX task, so that no settling separates them. */
    static final class GlassRobotActions {

        private GlassRobotActions() {
        }

        /** Press, release, press, release of the primary button in one task: GDK sees a double click. */
        static void doubleClick(GtkEventTrace t) throws Exception {
            var robot = t.robot();
            t.act(() -> {
                robot.mousePress(MouseButton.PRIMARY);
                robot.mouseRelease(MouseButton.PRIMARY);
                robot.mousePress(MouseButton.PRIMARY);
                robot.mouseRelease(MouseButton.PRIMARY);
            });
        }

        /** Press and release of the X11 button {@code button} in one task. */
        static void button(GtkEventTrace t, int button) throws Exception {
            t.act(() -> {
                GtkGlassShim.fakeButton(button, true);
                GtkGlassShim.fakeButton(button, false);
            });
        }
    }
}
