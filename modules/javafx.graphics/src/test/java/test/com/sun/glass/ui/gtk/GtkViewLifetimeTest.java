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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
 * How long the GTK glass keeps a {@code View} reachable, against what the JNI build of commit {@code 033187ad90}
 * did: the C held a view only through the global reference {@code jview} of a {@code WindowContext}
 * ({@code glass_window.cpp}); its {@code GlassView} ({@code GlassView.cpp}) held none. In a child JVM on the X11
 * display of {@code DISPLAY}:
 * <ul>
 * <li>an open view that no window holds and that is dropped without {@code close()} becomes collectable;</li>
 * <li>a view the C holds while Java does not - the window took it in {@code set_view} although
 * {@code Window.setView} threw its old view's {@code EXIT} - stays reachable and receives events through the view
 * table;</li>
 * <li>once the window lets it go, that view, still open, becomes collectable too.</li>
 * </ul>
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkViewLifetimeTest {

    private static final int STYLE = Window.TITLED | Window.CLOSABLE;

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkViewLifetimeTest.class, "lifetimeScenario", List.of());
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    @Test
    public void aDroppedOpenViewNoWindowHoldsIsCollectable() {
        assertEquals("true", value("dropped.registeredWhileReachable"));
        assertEquals("true", value("dropped.collected"), run.describe());
        assertEquals("false", value("dropped.registeredAfterwards"));
    }

    @Test
    public void aViewOnlyTheCHoldsStaysReachableAndReceivesEvents() {
        assertEquals("true", value("held.cHoldsIt"));
        assertEquals("true", value("held.javaWindowDoesNot"));
        assertEquals("true", value("held.alive"), run.describe());
        List<String> event = List.of(value("held.event").split("\n"));
        assertTrue(event.contains("h3.mouse 1001 1002 1003,1004 1005,1006 mods=0x3ef popup=true synth=false"),
                String.join("\n", event));
        assertTrue(event.contains("# fire 203 -> 0"), String.join("\n", event));
    }

    @Test
    public void aViewTheWindowLetGoOfIsCollectable() {
        assertEquals("true", value("released.exitReachedIt"), value("released.trace"));
        assertEquals("true", value("released.collected"), run.describe());
        assertEquals(value("registry.before"), value("registry.after"));
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    /** Runs in {@link GtkGlassChild}. */
    static void lifetimeScenario(Map<String, String> out) throws Exception {
        out.put("registry.before", GtkGlassChild.onFx(GtkViewLifetimeTest::registries));
        droppedView(out);
        GtkEventTrace t = new GtkEventTrace();
        GtkGlassChild.onFx(() -> {
            Thread.currentThread().setUncaughtExceptionHandler(t.reporter());
            return null;
        });
        GtkEventTrace.ViewHooks hooks = new GtkEventTrace.ViewHooks();
        GtkTraceWindow window = GtkGlassChild.onFx(() -> t.window("h", null, STYLE, "h2", hooks));
        WeakReference<View> held = viewOnlyTheCHolds(t, out, window, hooks);
        System.gc();
        out.put("held.alive", Boolean.toString(!GtkPeerRegistryTest.collect(held) && held.get() != null));
        t.step("event to the view only the C holds");
        t.act(() -> t.note(GtkGlassShim.fireAndRead(203, GtkGlassShim.cWindowViewId(window))));
        out.put("held.event", String.join("\n", section(t.lines(), "event to the view only the C holds")));

        t.step("close the window: set_view(NULL) sends the held view its EXIT and lets it go");
        t.act(window::close);
        List<String> closing = section(t.lines(), "close the window: set_view(NULL) sends the held view its EXIT"
                + " and lets it go");
        out.put("released.trace", String.join("\n", closing));
        out.put("released.exitReachedIt", Boolean.toString(closing.stream().anyMatch(line ->
                line.startsWith("h3.mouse EXIT"))));
        out.put("released.collected", Boolean.toString(GtkPeerRegistryTest.collect(held)));
        out.put("registry.after", GtkGlassChild.onFx(GtkViewLifetimeTest::registries));
    }

    /** A view created, registered, and dropped without {@code close()}. */
    private static void droppedView(Map<String, String> out) throws Exception {
        long[] id = new long[1];
        WeakReference<View> dropped = GtkGlassChild.onFx(() -> {
            View view = Application.GetApplication().createView();
            id[0] = GtkGlassShim.viewId(view);
            out.put("dropped.registeredWhileReachable", Boolean.toString(GtkGlassShim.isViewRegistered(id[0])));
            return new WeakReference<>(view);
        });
        out.put("dropped.collected", Boolean.toString(GtkPeerRegistryTest.collect(dropped)));
        out.put("dropped.registeredAfterwards", Boolean.toString(GtkGlassChild.onFx(() ->
                GtkGlassShim.isViewRegistered(id[0]))));
    }

    /**
     * Swaps the view of {@code window} for a new view {@code h3} while the old view's {@code EXIT} throws: the C takes
     * {@code h3}, Java's window keeps its old view, and nothing on the Java side refers to {@code h3} afterwards.
     */
    private static WeakReference<View> viewOnlyTheCHolds(GtkEventTrace t, Map<String, String> out,
                                                         GtkTraceWindow window, GtkEventTrace.ViewHooks hooks)
            throws Exception {
        t.step("swap h2 -> h3 with h2's EXIT throwing");
        t.arm("h2.mouse EXIT", 1);
        return GtkGlassChild.onFx(() -> {
            View old = window.getView();
            View view = Application.GetApplication().createView();
            view.setEventHandler(t.viewHandler("h3", hooks));
            try {
                window.setView(view);
                t.note("setView returned");
            } catch (GtkEventTrace.ArmedException e) {
                t.note("threw ArmedException out of Window.setView");
            }
            out.put("held.cHoldsIt", Boolean.toString(GtkGlassShim.cWindowViewId(window) == GtkGlassShim.viewId(view)));
            out.put("held.javaWindowDoesNot", Boolean.toString(window.getView() == old));
            return new WeakReference<>(view);
        });
    }

    private static List<String> section(List<String> lines, String step) {
        List<String> result = new ArrayList<>();
        int start = lines.indexOf("== " + step);
        for (int i = start + 1; start >= 0 && i < lines.size() && !lines.get(i).startsWith("== "); i++) {
            result.add(lines.get(i));
        }
        return result;
    }

    private static String registries() {
        return GtkGlassShim.windowRegistrySize() + " windows " + GtkGlassShim.viewRegistrySize() + " views";
    }
}
