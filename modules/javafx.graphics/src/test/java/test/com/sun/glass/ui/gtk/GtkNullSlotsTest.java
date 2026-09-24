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

import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.glass.ui.gtk.GtkTraceWindow;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NULL SLOTS of {@code glass_gtk_api.h}: a call site of the window, view and drag-and-drop C makes no call for a
 * {@code NULL} slot and goes on - it never crashes. In a child JVM driven by the glass robot on a display without a
 * window manager, the scenario installs tables with {@code NULL} slots at the real call sites, drives a window with
 * a view through them, and restores the production tables:
 * <ol>
 * <li>every slot {@code NULL}: create, bounds, show, focus, a click, a key and close - {@code is_enabled} missing, the
 * window counts as disabled and drops its input;</li>
 * <li>every slot {@code NULL} but {@code is_enabled}: a click and a key reach {@code notify_mouse} and
 * {@code notify_key}, which are {@code NULL};</li>
 * <li>every slot {@code NULL} but {@code is_enabled}, {@code notify_drag_enter} and {@code notify_drag_over}: a drag
 * that Java starts is answered by the window's view and dropped on it - {@code notify_drag_drop} and
 * {@code notify_drag_leave} missing, and the source's {@code source_get_data}, so the drag has no image and serves no
 * data.</li>
 * </ol>
 * The windows and views record every call that reaches them ({@link GtkEventTrace}); in each part only the kept
 * slots may have been called, and the child must end normally. The Java side of the window closed in part 1, which
 * no {@code notify_destroy} reached, is closed afterwards through that slot's production target.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkNullSlotsTest {

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(2 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireRobot();
        GtkTraceGolden.requireNoWindowManager(GtkNullSlotsTest.class);
        run = GtkGlassChildJvm.run(GtkNullSlotsTest.class, "nullSlotsScenario", List.of("-DUSE_ROBOT=true"));
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    /**
     * With every slot {@code NULL}, nothing the C did reached Java: the one call the view saw is the {@code MOVE}
     * that {@code Window.setVisible} makes itself ({@code synthesizeViewMoveEvent}).
     */
    @Test
    public void withEverySlotNullNothingReachesJava() {
        assertEquals("v1.view MOVE at 0,0", value("allNull.calls"), run.describe());
        assertEquals("true", value("allNull.destroyedInJava"));
    }

    /** With only {@code is_enabled} installed, the click and the key called nothing but it. */
    @Test
    public void withOnlyIsEnabledTheInputReachesNothingElse() {
        String calls = value("isEnabledOnly.calls");
        assertTrue(!calls.isEmpty(), "the input reached is_enabled");
        for (String line : calls.split("\n")) {
            assertEquals("w2.isEnabled=true", line, calls);
        }
    }

    /**
     * With {@code notify_drag_enter} and {@code notify_drag_over} installed, the drag is answered and dropped: the
     * drop, the leave and the source's data are {@code NULL} slots, and the drag still ends with the answered
     * action.
     */
    @Test
    public void aDropWithoutItsSlotsStillEnds() {
        String calls = value("dragOnly.calls");
        assertTrue(calls.contains("v2.dragEnter "), calls);
        for (String line : calls.split("\n")) {
            assertTrue(line.equals("w2.isEnabled=true") || line.startsWith("v2.dragEnter ")
                    || line.startsWith("v2.dragOver "), calls);
        }
        assertEquals("COPY", value("dragOnly.performed"), run.describe());
    }

    /** The production tables, restored, deliver again. */
    @Test
    public void theRestoredTablesDeliverAgain() {
        assertTrue(value("restored.calls").contains("w2.notifyDestroy"), value("restored.calls"));
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    private static final int STYLE = Window.TITLED | Window.CLOSABLE;

    /** Runs in {@link GtkGlassChild}. */
    static void nullSlotsScenario(Map<String, String> out) throws Exception {
        GtkEventTrace t = new GtkEventTrace();
        GtkGlassChild.onFx(() -> {
            t.installApplicationRecorders();
            return null;
        });
        GtkEventTrace.ViewHooks copying = new GtkEventTrace.ViewHooks() {
            @Override
            int dragAction(String phase, int recommended) {
                return Clipboard.ACTION_COPY;
            }
        };
        try {
            allNull(t, out, copying);
            GtkTraceWindow w2 = isEnabledOnly(t, out, copying);
            dragOnly(t, out, w2);
        } finally {
            GtkGlassChild.onFx(() -> {
                GtkGlassShim.reinstallProductionTables();
                return null;
            });
        }
        int mark = t.lines().size();
        t.act(() -> {
            for (Window window : List.copyOf(Window.getWindows())) {
                window.close();
            }
        });
        out.put("restored.calls", String.join("\n", t.lines().subList(mark, t.lines().size())));
    }

    private static void allNull(GtkEventTrace t, Map<String, String> out, GtkEventTrace.ViewHooks hooks)
            throws Exception {
        GtkGlassChild.onFx(() -> {
            GtkGlassShim.installNullTablesExcept(Set.of());
            return null;
        });
        int mark = t.lines().size();
        GtkTraceWindow w1 = GtkGlassChild.onFx(() -> t.window("w1", null, STYLE, "v1", hooks));
        long id = GtkGlassChild.onFx(() -> GtkGlassShim.windowId(w1));
        drive(t, w1);
        t.act(w1::close);
        out.put("allNull.calls", String.join("\n", t.lines().subList(mark, t.lines().size())));
        GtkGlassChild.onFx(() -> {
            GtkGlassShim.reinstallProductionTables();
            GtkGlassShim.notifyDestroyThroughTheSlotTarget(id);
            out.put("allNull.destroyedInJava", Boolean.toString(w1.isClosed()));
            return null;
        });
    }

    private static GtkTraceWindow isEnabledOnly(GtkEventTrace t, Map<String, String> out,
                                                GtkEventTrace.ViewHooks hooks) throws Exception {
        GtkTraceWindow w2 = GtkGlassChild.onFx(() -> t.window("w2", null, STYLE, "v2", hooks));
        t.act(() -> w2.setBounds(600, 100, true, true, -1, -1, 300, 200, 0, 0));
        t.act(() -> w2.setVisible(true));
        GtkGlassChild.onFx(() -> {
            GtkGlassShim.installNullTablesExcept(Set.of("window.is_enabled"));
            return null;
        });
        int mark = t.lines().size();
        int[] origin = GtkGlassChild.onFx(() -> GtkGlassShim.rootPosition(w2.getNativeWindow()));
        t.act(w2::requestFocus);
        t.mouseMove(origin[0] + 40, origin[1] + 50);
        t.click(MouseButton.PRIMARY);
        t.type(KeyCode.A);
        out.put("isEnabledOnly.calls", String.join("\n", t.lines().subList(mark, t.lines().size())));
        return w2;
    }

    private static void dragOnly(GtkEventTrace t, Map<String, String> out, GtkTraceWindow w2) throws Exception {
        GtkGlassChild.onFx(() -> {
            GtkGlassShim.installNullTablesExcept(Set.of("window.is_enabled", "dnd.notify_drag_enter",
                    "dnd.notify_drag_over"));
            return null;
        });
        int mark = t.lines().size();
        int[] origin = GtkGlassChild.onFx(() -> GtkGlassShim.rootPosition(w2.getNativeWindow()));
        t.mouseMove(origin[0] + 20, origin[1] + 20);
        t.mousePress(MouseButton.PRIMARY);
        AtomicReference<String> performed = new AtomicReference<>();
        // straight to the peer: Application.invokeLater would hold every later runnable - the robot's included -
        // until this one, which runs the drag loop, has returned
        GtkGlassShim.submitForLaterInvocation(() -> {
            ClipboardAssistance source = new ClipboardAssistance(Clipboard.DND) {
                @Override
                public void actionPerformed(int action) {
                    performed.set(GtkEventTrace.action(action));
                }
            };
            try {
                source.setData("text/plain", "null slots");
                source.setSupportedActions(Clipboard.ACTION_COPY);
                source.flush();
            } catch (RuntimeException e) {
                performed.set("threw " + e);
            } finally {
                source.close();
            }
        });
        t.mouseMove(origin[0] + 40, origin[1] + 40);
        t.mouseMove(origin[0] + 60, origin[1] + 70);
        t.mouseMove(origin[0] + 80, origin[1] + 90);
        t.mouseRelease(MouseButton.PRIMARY);
        if (!GtkGlassChild.waitFor(30_000, () -> performed.get() != null)) {
            throw new IllegalStateException("the drag did not end");
        }
        t.settle();
        out.put("dragOnly.performed", performed.get());
        out.put("dragOnly.calls", String.join("\n", t.lines().subList(mark, t.lines().size())));
    }

    /** Bounds, show, focus, pointer in, a click and a key, for a window of this scenario. */
    private static void drive(GtkEventTrace t, GtkTraceWindow window) throws Exception {
        t.act(() -> window.setBounds(100, 100, true, true, -1, -1, 300, 200, 0, 0));
        t.act(() -> window.setVisible(true));
        t.act(window::requestFocus);
        int[] origin = GtkGlassChild.onFx(() -> GtkGlassShim.rootPosition(window.getNativeWindow()));
        t.mouseMove(origin[0] + 40, origin[1] + 50);
        t.click(MouseButton.PRIMARY);
        t.type(KeyCode.A);
    }
}
