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
import com.sun.glass.ui.Application;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.glass.ui.gtk.GtkTraceWindow;
import java.io.IOException;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The callback tables of {@code native-glass/gtk/glass_gtk_api.h}, through which the window, view, drag-and-drop
 * and application C of {@code libglassgtk3.so} calls Java since {@code GtkGlassNative.installCallbacks}, checked in
 * a child JVM on the X11 display of {@code DISPLAY}:
 * <ul>
 * <li>the ABI version and the {@code sizeof} of every table and of {@code GgtkDndData} agree with the facade's
 * layouts, and every slot is where its name puts it;</li>
 * <li>every slot, dialled through the C ({@code ggtk_test_fire_callback}) with the header's fixed patterns into a
 * recording table built from the facade's own descriptors, arrives with exactly those arguments and hands its
 * status and out-parameters back - the only check of a descriptor against its C prototype, including the slots no
 * test display reaches;</li>
 * <li>the production slots keep the id contract of the header: 0 is the {@code NullPointerException} HotSpot raised
 * for a {@code NULL} receiver, reported, status 1, out-parameters untouched; an id no longer registered is a no-op;
 * a registered peer receives the call, strings decoded as {@code NewStringUTF} decoded them and key characters as
 * UTF-16 code units;</li>
 * <li>the drag source's data crosses as the C converted it, including the throwing conversions;</li>
 * <li>ids reach the C, and the registries live exactly as long as the JNI global references did - a closed view a
 * window still holds keeps receiving events;</li>
 * <li>real events injected with the robot reach Java through the stubs, and no JNI native method is on the stack of
 * any call the event code makes into Java;</li>
 * <li>no class of the GTK glass declares a JNI native method any more: the window, view, drag-and-drop and
 * application peers call the {@code ggtk_*} functions of the header, and the screen capture helper the
 * {@code sc_*} ones of {@code screencast_api.h}.</li>
 * </ul>
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkCallbackTableTest {

    private static final int STYLE = Window.TITLED | Window.CLOSABLE;

    /** The TEXT pattern of the test hook, {@code 41 C3 A9 F0 9F 98 80 5A}, as {@code NewStringUTF} decodes it. */
    private static final String HOOK_TEXT = "\"A\\u00e9\\u00f0\\u009f\"";

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkCallbackTableTest.class, "tableScenario", List.of());
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    private static List<String> section(String trace, String step) {
        List<String> lines = List.of(trace.split("\n"));
        int start = lines.indexOf("== " + step);
        assertTrue(start >= 0, "no step '" + step + "' in\n" + trace);
        List<String> result = new ArrayList<>();
        for (int i = start + 1; i < lines.size() && !lines.get(i).startsWith("== "); i++) {
            result.add(lines.get(i));
        }
        return result;
    }

    @Test
    public void theTablesAreInstalledWithOneStubPerSlot() {
        assertEquals("1/1", value("abi"));
        assertEquals("true", value("installed"));
        assertEquals("2 12 10 5 distinct=29", value("stubs"));
        assertEquals("com.sun.glass.ui.gtk.GtkWindow", value("createWindow.class"));
    }

    /**
     * No class of {@code com.sun.glass.ui.gtk} or {@code com.sun.glass.ui.gtk.screencast} in the module, nested
     * ones included, declares a JNI native method any more: every call into the glass GTK library goes through
     * {@code glass_gtk_api.h} or {@code screencast_api.h}.
     */
    @Test
    public void theGtkGlassDeclaresNoNativeMethod() throws IOException, ClassNotFoundException {
        assertEquals(List.of(), gtkNativeMethods());
    }

    /** {@code <class>.<method>} of every native method declared by a class of the two GTK glass packages, sorted. */
    private static List<String> gtkNativeMethods() throws IOException, ClassNotFoundException {
        Module module = Application.class.getModule();
        assertTrue(module.isNamed() && module.getLayer() != null, "javafx.graphics is not a named module: " + module);
        ModuleReference reference = module.getLayer().configuration().findModule(module.getName()).orElseThrow()
                .reference();
        Set<String> natives = new TreeSet<>();
        int classes = 0;
        try (ModuleReader reader = reference.open(); Stream<String> names = reader.list()) {
            for (String name : names.filter(n -> n.matches("com/sun/glass/ui/gtk/(screencast/)?[^/]+\\.class"))
                    .toList()) {
                Class<?> c = Class.forName(name.substring(0, name.length() - ".class".length()).replace('/', '.'),
                        false, module.getClassLoader());
                classes++;
                for (Method method : c.getDeclaredMethods()) {
                    if (Modifier.isNative(method.getModifiers())) {
                        natives.add(c.getName() + "." + method.getName());
                    }
                }
            }
        }
        assertTrue(classes > 20, "only " + classes + " classes of the GTK glass packages in " + reference);
        return List.copyOf(natives);
    }

    /** The {@code sizeof} probes and the slot order of {@code glass_gtk_api.h}, on LP64. */
    @Test
    public void layoutsMatchTheHeader() {
        List<String> expected = new ArrayList<>();
        expected.add("GgtkAppCallbacks size=16 c=16");
        addSlots(expected, "notify_screen_settings_changed", "get_application_name");
        expected.add("GgtkWindowCallbacks size=96 c=96");
        addSlots(expected, "is_enabled", "notify_state_changed", "notify_focus", "notify_focus_disabled",
                "notify_focus_ungrab", "notify_destroy", "notify_close", "notify_resize", "notify_move",
                "notify_move_to_another_screen", "notify_level_changed", "non_client_hit_test");
        expected.add("GgtkViewCallbacks size=80 c=80");
        addSlots(expected, "notify_view", "notify_resize", "notify_repaint", "notify_mouse", "notify_menu",
                "notify_scroll", "notify_key", "notify_input_method_preedit", "notify_input_method_commit",
                "notify_input_method_candidate_pos_request");
        expected.add("GgtkDndCallbacks size=40 c=40");
        addSlots(expected, "notify_drag_enter", "notify_drag_over", "notify_drag_drop", "notify_drag_leave",
                "source_get_data");
        expected.addAll(List.of("GgtkDndData size=24 c=24", "kind@0", "count@4", "data@8", "pixbuf@16"));
        assertEquals(String.join("\n", expected), value("layouts"));
    }

    private static void addSlots(List<String> lines, String... slots) {
        for (int i = 0; i < slots.length; i++) {
            lines.add(slots[i] + "@" + (8 * i));
        }
    }

    /** Every slot through the C into a recording table built from the facade's descriptors. */
    @Test
    public void everySlotArrivesThroughTheCWithTheHeaderPattern() {
        String id = Long.toString(GtkGlassShim.RECORDING_ID);
        int status = GtkGlassShim.RECORDING_STATUS;
        List<String> expected = List.of(
                "app.notify_screen_settings_changed()", "fire 0 -> " + status,
                "app.get_application_name(out)", "fire 1 -> " + status + " out=0x1234",
                "window.is_enabled(" + id + ", out)", "fire 100 -> " + status + " out=1",
                "window.notify_state_changed(" + id + ", 1001)", "fire 101 -> " + status,
                "window.notify_focus(" + id + ", 1001)", "fire 102 -> " + status,
                "window.notify_focus_disabled(" + id + ")", "fire 103 -> " + status,
                "window.notify_focus_ungrab(" + id + ")", "fire 104 -> " + status,
                "window.notify_destroy(" + id + ")", "fire 105 -> " + status,
                "window.notify_close(" + id + ")", "fire 106 -> " + status,
                "window.notify_resize(" + id + ", 1001, 1002, 1003)", "fire 107 -> " + status,
                "window.notify_move(" + id + ", 1001, 1002)", "fire 108 -> " + status,
                "window.notify_move_to_another_screen(" + id + ", 1001)", "fire 109 -> " + status,
                "window.notify_level_changed(" + id + ", 1001)", "fire 110 -> " + status,
                "window.non_client_hit_test(" + id + ", 1001, 1002, out)", "fire 111 -> " + status + " out=2",
                "view.notify_view(" + id + ", 1001)", "fire 200 -> " + status,
                "view.notify_resize(" + id + ", 1001, 1002)", "fire 201 -> " + status,
                "view.notify_repaint(" + id + ", 1001, 1002, 1003, 1004)", "fire 202 -> " + status,
                "view.notify_mouse(" + id + ", 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1, 0)",
                "fire 203 -> " + status,
                "view.notify_menu(" + id + ", 1001, 1002, 1003, 1004, 1)", "fire 204 -> " + status,
                "view.notify_scroll(" + id + ", 1001, 1002, 1003, 1004, 1.5, 2.5, 1005, 1006, 1007, 1008, 1009, 3.5,"
                        + " 4.5)", "fire 205 -> " + status,
                "view.notify_key(" + id + ", 1001, 1002, u[0041 d800], 2, 1003)", "fire 206 -> " + status,
                "view.notify_input_method_preedit(" + id + ", b[41c3a9f09f98805a], 8, 1001, 1002)",
                "fire 207 -> " + status,
                "view.notify_input_method_commit(" + id + ", b[41c3a9f09f98805a], 8)", "fire 208 -> " + status,
                "view.notify_input_method_candidate_pos_request(" + id + ", 1001, out, out)",
                "fire 209 -> " + status + " xy=3.25,4.75 valid=1",
                "dnd.notify_drag_enter(" + id + ", 1001, 1002, 1003, 1004, 1005, out)",
                "fire 300 -> " + status + " out=42",
                "dnd.notify_drag_over(" + id + ", 1001, 1002, 1003, 1004, 1005, out)",
                "fire 301 -> " + status + " out=42",
                "dnd.notify_drag_drop(" + id + ", 1001, 1002, 1003, 1004, 1005)", "fire 302 -> " + status,
                "dnd.notify_drag_leave(" + id + ")", "fire 303 -> " + status,
                "dnd.source_get_data(\"text/plain\", 10, 5, out)", "fire 304 -> " + status + " kind=4 count=9",
                "fire 999 -> -1",
                "fire 206 with notify_key NULL -> -2");
        assertEquals(String.join("\n", expected), value("recorded"));
    }

    /** After the recording tables the production stubs are back: the id 0 of a production slot is reported. */
    @Test
    public void theProductionTablesAreRestored() {
        List<String> lines = section(value("trace"), "restored");
        assertEquals(List.of("reported NullPointerException: null", "# fire 100 -> 1 out=untouched"), lines);
    }

    /** The id 0 is the NullPointerException of a NULL JNI receiver: reported once, status 1, nothing written. */
    @Test
    public void idZeroIsAReportedNullPointerException() {
        String trace = value("trace");
        for (int slot : productionSlots()) {
            List<String> lines = section(trace, "id 0 slot " + slot);
            assertEquals(2, lines.size(), "slot " + slot + ": " + lines);
            assertEquals("reported NullPointerException: null", lines.get(0), "slot " + slot);
            assertTrue(lines.get(1).startsWith("# fire " + slot + " -> 1"), "slot " + slot + ": " + lines);
            assertTrue(!lines.get(1).contains("=") || lines.get(1).contains("untouched"),
                    "slot " + slot + " wrote an out-parameter: " + lines);
        }
    }

    /** A non-zero id no peer is registered under any more is a stale peer: nothing happens, status 0. */
    @Test
    public void anUnknownIdIsANoOp() {
        String trace = value("trace");
        for (int slot : productionSlots()) {
            List<String> lines = section(trace, "unknown id slot " + slot);
            assertEquals(1, lines.size(), "slot " + slot + ": " + lines);
            assertTrue(lines.get(0).startsWith("# fire " + slot + " -> 0"), "slot " + slot + ": " + lines);
            assertTrue(!lines.get(0).contains("=") || lines.get(0).contains("untouched"),
                    "slot " + slot + " wrote an out-parameter: " + lines);
        }
    }

    private static List<Integer> productionSlots() {
        List<Integer> slots = new ArrayList<>(GtkGlassShim.WINDOW_SLOTS);
        slots.addAll(GtkGlassShim.VIEW_SLOTS);
        slots.addAll(GtkGlassShim.DND_SLOTS);
        slots.remove(Integer.valueOf(109)); // the hook's monitor 1001 does not exist; see anotherScreen...
        slots.remove(Integer.valueOf(304)); // source_get_data carries no id; see theDragSource...
        return slots;
    }

    /** The registered peer receives every slot, through the virtual Window / View methods. */
    @Test
    public void aRegisteredPeerReceivesTheCall() {
        String trace = value("trace");
        assertEquals(List.of("p.isEnabled=true", "# fire 100 -> 0 out=1"), section(trace, "peer slot 100"));
        assertTrue(section(trace, "peer slot 101").contains("p.notifyStateChanged 1001"));
        assertTrue(section(trace, "peer slot 102").contains("p.notifyFocus 1001"));
        assertTrue(section(trace, "peer slot 103").contains("p.notifyFocusDisabled"));
        assertTrue(section(trace, "peer slot 104").contains("p.notifyFocusUngrab"));
        assertTrue(section(trace, "peer slot 106").contains("p.notifyClose"));
        assertTrue(section(trace, "peer slot 107").contains("p.notifyResize 1001 1002 1003"));
        assertTrue(section(trace, "peer slot 108").contains("p.notifyMove 1001 1002"));
        assertTrue(section(trace, "peer slot 110").contains("p.notifyLevelChanged 1001"));
        // not EXTENDED: GtkWindow.nonClientHitTest answers HT_CLIENT
        assertEquals(List.of("# fire 111 -> 0 out=2"), section(trace, "peer slot 111"));
        assertTrue(section(trace, "peer slot 201").contains("pv.view RESIZE 1001x1002"));
        assertTrue(section(trace, "peer slot 202").contains("pv.view REPAINT"));
        assertTrue(section(trace, "peer slot 203").contains(
                "pv.mouse 1001 1002 1003,1004 1005,1006 mods=0x3ef popup=true synth=false"));
        assertTrue(section(trace, "peer slot 204").contains("pv.menu 1001,1002 1003,1004 keyboard=true"));
        assertTrue(section(trace, "peer slot 205").contains(
                "pv.scroll 1001,1002 1003,1004 d=1.5,2.5 mods=0x3ed lines=1006 chars=1007 default=1008,1009"
                        + " mult=3.5,4.5"));
        assertTrue(section(trace, "peer slot 300").contains("pv.dragEnter 1001,1002 1003,1004 0x3ed"));
        assertTrue(section(trace, "peer slot 300").contains("# fire 300 -> 0 out=1005"));
        assertTrue(section(trace, "peer slot 301").contains("pv.dragOver 1001,1002 1003,1004 0x3ed"));
        assertTrue(section(trace, "peer slot 303").contains("pv.dragLeave"));
        assertTrue(section(trace, "peer slot 302").contains("pv.dragDrop 1001,1002 1003,1004 0x3ed"));
        assertTrue(section(trace, "peer slot 105").contains("d.notifyDestroy"));
        assertEquals("true", value("destroyed.unregistered"));
    }

    /** Key characters cross as UTF-16 code units, a lone surrogate included; never through a charset. */
    @Test
    public void keyCharsAreCodeUnits() {
        List<String> lines = section(value("trace"), "peer slot 206");
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("pv.key ") && line.endsWith(
                " 0x3ea chars=[41,d800] mods=0x3eb")), String.join("\n", lines));
        assertEquals("true", value("key.typedSharesThePressArray"));
    }

    /** IME text crosses as the bytes NewStringUTF decoded: a four-byte UTF-8 sequence is not one character. */
    @Test
    public void imeTextIsDecodedAsNewStringUtfDecodedIt() {
        String trace = value("trace");
        assertTrue(section(trace, "peer slot 207").contains("pv.im text=" + HOOK_TEXT
                + " clause=[0,4] attrBounds=[0,4] attrs=[-22] commit=0 cursor=1001"), trace);
        assertTrue(section(trace, "peer slot 208").contains("pv.im text=" + HOOK_TEXT
                + " clause=null attrBounds=null attrs=null commit=4 cursor=4"), trace);
        List<String> candidate = section(trace, "peer slot 209");
        assertTrue(candidate.contains("pv.imCandidatePos 1001"), String.join("\n", candidate));
        assertTrue(candidate.contains("# fire 209 -> 0 " + value("candidate.expected")),
                String.join("\n", candidate));
    }

    /**
     * {@code ggtk_application_get_key_code_for_char} takes the character as one UTF-16 code unit: {@code a} is
     * {@code KeyEvent.VK_A} on the display's US layout, a lone surrogate {@code VK_UNDEFINED}.
     */
    @Test
    public void theKeyCodeOfACharIsLookedUpForItsCodeUnit() {
        assertEquals(KeyEvent.VK_A + " " + KeyEvent.VK_UNDEFINED, value("keyCodeForChar"));
    }

    /** {@code get_application_name} answers the modified UTF-8 of {@code Application.getName()} in a g_malloc block. */
    @Test
    public void theApplicationNameIsAGMallocBlock() {
        assertEquals("# fire 1 -> 0 out=\"" + value("app.name") + "\"", section(value("trace"), "app slot 1").get(0));
    }

    /** The drag source's value, converted per {@code as} exactly as the JNI converted it. */
    @Test
    public void theDragSourceDataCrossesAsTheCConvertedIt() {
        String trace = value("trace");
        // h, U+00E9, U+0000 and a lone high surrogate: modified UTF-8, U+0000 as C0 80, the surrogate as three bytes
        assertEquals(List.of("# status=0 kind=1 count=8 data=68c3a9c080eda0bd00 pixbuf=NULL"),
                section(trace, "dnd string"));
        assertEquals(List.of("# status=0 kind=0 count=0 data=NULL pixbuf=NULL"), section(trace, "dnd string of bytes"));
        assertEquals(List.of("# status=0 kind=2 count=5 data=0102030405 pixbuf=NULL"), section(trace, "dnd bytes"));
        assertEquals(List.of("# status=0 kind=2 count=0 data= pixbuf=NULL"), section(trace, "dnd empty bytes"));
        assertEquals(List.of("reported ReadOnlyBufferException: null",
                "# status=1 kind=2 count=0 data=NULL pixbuf=NULL"), section(trace, "dnd read-only bytes"));
        List<String> pixbuf = section(trace, "dnd pixels");
        assertEquals(1, pixbuf.size(), String.join("\n", pixbuf));
        assertTrue(pixbuf.get(0).startsWith("# status=0 kind=3 count=0 data=NULL pixbuf=w=2 h=1 "), pixbuf.get(0));
        assertEquals(List.of("reported IllegalStateException: \"attach refused\"",
                "# status=1 kind=3 count=0 data=NULL pixbuf=NULL"), section(trace, "dnd throwing pixels"));
        assertEquals(List.of("# status=0 kind=4 count=2 data=2f746d702f6100622f6300 pixbuf=NULL"),
                section(trace, "dnd strings"));
        assertEquals(List.of("# status=0 kind=4 count=0 data= pixbuf=NULL"), section(trace, "dnd no strings"));
        assertEquals(List.of("reported NullPointerException: null",
                "# status=1 kind=0 count=0 data=NULL pixbuf=NULL"), section(trace, "dnd null string"));
        assertEquals(List.of("# status=0 kind=1 count=3 data=72617700 pixbuf=NULL"), section(trace, "dnd raw string"));
        assertEquals(List.of("# status=0 kind=2 count=2 data=0708 pixbuf=NULL"), section(trace, "dnd raw bytes"));
        assertEquals(List.of("# status=0 kind=0 count=0 data=NULL pixbuf=NULL"), section(trace, "dnd raw other"));
        assertEquals(List.of("# status=0 kind=0 count=0 data=NULL pixbuf=NULL"), section(trace, "dnd absent key"));
        assertEquals(List.of("reported IllegalStateException: \"get refused\"",
                "# status=1 kind=0 count=0 data=NULL pixbuf=NULL"), section(trace, "dnd throwing map"));
        assertEquals(List.of("reported NullPointerException: null",
                "# status=1 kind=0 count=0 data=NULL pixbuf=NULL"), section(trace, "dnd no drag"));
    }

    /** The window and view ids reach the C, and the window's view id follows {@code _setView}. */
    @Test
    public void idsReachTheC() {
        assertEquals("true", value("ids.window"));
        assertEquals("true", value("ids.view"));
        assertEquals("true", value("ids.windowView"));
        assertEquals("0", value("ids.windowViewAfterNull"));
    }

    /**
     * The registries live as the JNI global references did: a view the C window took although {@code setView} threw
     * stays registered after {@code close} and keeps receiving events until the window lets it go.
     */
    @Test
    public void registriesLiveAsTheGlobalReferencesDid() {
        String trace = value("trace");
        assertTrue(section(trace, "swap with the old view's EXIT throwing").contains(
                "# threw ArmedException out of Window.setView: armed h2.mouse EXIT BUTTON_NONE 0,0 0,0 mods=0x0"
                        + " popup=false synth=false"), trace);
        assertEquals("true", value("held.cHoldsNewView"));
        assertEquals("true", value("held.javaKeepsOldView"));
        assertEquals("true", value("held.closedButRegistered"));
        assertTrue(section(trace, "event to the closed held view").contains(
                "h3.mouse 1001 1002 1003,1004 1005,1006 mods=0x3ef popup=true synth=false"), trace);
        assertTrue(section(trace, "close the window").contains("h3.mouse EXIT BUTTON_NONE 0,0 0,0 mods=0x0"
                + " popup=false synth=false"), trace);
        assertEquals("true", value("held.unregisteredAfterClose"));
        assertEquals(value("registry.before"), value("registry.after"));
    }

    /** {@code notify_move_to_another_screen} builds the Screen of the monitor with the facade's screen code. */
    @Test
    public void anotherScreenIsBuiltForTheMonitor() {
        List<String> lines = section(value("trace"), "move to monitor 0");
        assertEquals("# status 0", lines.get(lines.size() - 1), String.join("\n", lines));
        assertTrue(lines.contains("p.notifyMoveToAnotherScreen " + value("screen0")), String.join("\n", lines));
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    /** Runs in {@link GtkGlassChild}. */
    static void tableScenario(Map<String, String> out) throws Exception {
        GtkEventTrace t = new GtkEventTrace();
        try {
            tableScenario(t, out);
        } finally {
            out.put("trace", String.join("\n", t.lines()));
        }
    }

    private static void tableScenario(GtkEventTrace t, Map<String, String> out) throws Exception {
        out.put("abi", GtkGlassShim.facadeAbiVersion() + "/" + GtkGlassShim.libraryAbiVersion());
        out.put("installed", Boolean.toString(GtkGlassShim.callbacksInstalled()));
        out.put("stubs", GtkGlassShim.installedStubs());
        out.put("layouts", String.join("\n", GtkGlassShim.callbackLayouts()));
        out.put("registry.before", GtkGlassChild.onFx(() -> GtkGlassShim.windowRegistrySize() + " windows "
                + GtkGlassShim.viewRegistrySize() + " views"));
        GtkGlassChild.onFx(() -> {
            t.installApplicationRecorders();
            Window probe = Application.GetApplication().createWindow(null, Screen.getMainScreen(), STYLE);
            out.put("createWindow.class", probe.getClass().getName());
            probe.close();
            out.put("app.name", Application.GetApplication().getName());
            // a letter of the display's layout, and a lone surrogate, which g_utf16_to_ucs4 rejects
            out.put("keyCodeForChar", Application.getKeyCodeForChar('a', 0) + " "
                    + Application.getKeyCodeForChar((char) 0xD800, 0));
            return null;
        });

        out.put("recorded", String.join("\n", GtkGlassChild.onFx(GtkGlassShim::fireThroughRecordingTables)));
        t.step("restored");
        t.act(() -> t.note(GtkGlassShim.fireAndRead(100, 0)));
        t.step("app slot 1");
        t.act(() -> t.note(GtkGlassShim.fireAndRead(1, 0)));

        for (int slot : productionSlots()) {
            t.step("id 0 slot " + slot);
            t.act(() -> t.note(GtkGlassShim.fireAndRead(slot, 0)));
            t.step("unknown id slot " + slot);
            t.act(() -> t.note(GtkGlassShim.fireAndRead(slot, Long.MAX_VALUE)));
        }

        GtkEventTrace.ViewHooks hooks = new GtkEventTrace.ViewHooks();
        t.step("create the peer window");
        GtkTraceWindow peer = GtkGlassChild.onFx(() -> t.window("p", null, STYLE, "pv", hooks));
        long windowId = GtkGlassChild.onFx(() -> GtkGlassShim.windowId(peer));
        long viewId = GtkGlassChild.onFx(() -> GtkGlassShim.viewId(peer.getView()));
        checkIds(out, peer);
        for (int slot : List.of(100, 101, 102, 103, 104, 106, 107, 108, 110, 111)) {
            t.step("peer slot " + slot);
            t.act(() -> t.note(GtkGlassShim.fireAndRead(slot, windowId)));
        }
        for (int slot : List.of(201, 202, 203, 204, 205, 207, 208, 300, 301, 303, 300, 302)) {
            t.step("peer slot " + slot);
            t.act(() -> t.note(GtkGlassShim.fireAndRead(slot, viewId)));
        }
        keyChars(t, out, peer, viewId);
        t.step("peer slot 209");
        t.act(() -> {
            View view = peer.getView();
            double x = 420.0 - (peer.getX() + view.getX());
            double y = 260.0 - (peer.getY() + view.getY());
            out.put("candidate.expected", "xy=" + x + "," + y + " valid=1");
            t.note(GtkGlassShim.fireAndRead(209, viewId));
        });
        t.step("move to monitor 0");
        t.act(() -> {
            out.put("screen0", GtkTraceWindow.describe(GtkGlassShim.screens()[0]));
            t.note("status " + GtkGlassShim.notifyMoveToAnotherScreen(windowId, 0));
        });

        t.step("create the window to destroy");
        GtkTraceWindow doomed = GtkGlassChild.onFx(() -> t.window("d", null, STYLE, null, hooks));
        t.step("peer slot 105");
        t.act(() -> {
            long id = GtkGlassShim.windowId(doomed);
            int before = GtkGlassShim.windowRegistrySize();
            t.note(GtkGlassShim.fireAndRead(105, id));
            out.put("destroyed.unregistered", Boolean.toString(GtkGlassShim.windowRegistrySize() == before - 1));
        });

        dragSource(t);
        heldViews(t, out);

        t.act(peer::close);
        out.put("registry.after", GtkGlassChild.onFx(() -> GtkGlassShim.windowRegistrySize() + " windows "
                + GtkGlassShim.viewRegistrySize() + " views"));
    }

    /** The ids of {@code window} and its view as Java registered them and as the C stores them. */
    private static void checkIds(Map<String, String> out, Window window) throws Exception {
        GtkGlassChild.onFx(() -> {
            View view = window.getView();
            out.put("ids.window", Boolean.toString(GtkGlassShim.windowId(window) != 0
                    && GtkGlassShim.windowId(window) == GtkGlassShim.cWindowId(window)));
            out.put("ids.view", Boolean.toString(GtkGlassShim.viewId(view) != 0
                    && GtkGlassShim.viewId(view) == GtkGlassShim.cViewId(view)));
            out.put("ids.windowView", Boolean.toString(
                    GtkGlassShim.cWindowViewId(window) == GtkGlassShim.viewId(view)));
            Window other = Application.GetApplication().createWindow(null, Screen.getMainScreen(), STYLE);
            View spare = Application.GetApplication().createView();
            other.setView(spare);
            other.setView(null);
            out.put("ids.windowViewAfterNull", Long.toString(GtkGlassShim.cWindowViewId(other)));
            spare.close();
            other.close();
            return null;
        });
    }

    /**
     * A key {@code PRESS} with a character and the {@code TYPED} that follows it, dialled as
     * {@code WindowContextBase::process_key} dials them - the same {@code &key} - get the same array.
     */
    private static void keyChars(GtkEventTrace t, Map<String, String> out, GtkTraceWindow peer, long viewId)
            throws Exception {
        t.step("peer slot 206");
        t.act(() -> t.note(GtkGlassShim.fireAndRead(206, viewId)));
        Set<Integer> identities = ConcurrentHashMap.newKeySet();
        List<String> kinds = new ArrayList<>();
        GtkGlassChild.onFx(() -> {
            View view = peer.getView();
            View.EventHandler previous = view.getEventHandler();
            view.setEventHandler(new View.EventHandler() {
                @Override
                public boolean handleKeyEvent(View v, long time, int action, int keyCode, char[] keyChars,
                                              int modifiers) {
                    identities.add(System.identityHashCode(keyChars));
                    kinds.add(action + ":" + new String(keyChars));
                    return false;
                }
            });
            try {
                GtkGlassShim.pressAndTypeThroughTheKeySlot(viewId, 'q');
            } finally {
                view.setEventHandler(previous);
            }
            return null;
        });
        out.put("key.typedSharesThePressArray", Boolean.toString(kinds.size() == 2 && identities.size() == 1));
    }

    /** {@code source_get_data} with the drag data map in progress, every conversion and every failure. */
    private static void dragSource(GtkEventTrace t) throws Exception {
        dnd(t, "dnd string", map("h\u00e9\u0000\ud83d"), 1);
        dnd(t, "dnd string of bytes", map(ByteBuffer.wrap(new byte[] {1})), 1);
        dnd(t, "dnd bytes", map(ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5}, 1, 2)), 2);
        dnd(t, "dnd empty bytes", map(ByteBuffer.allocate(0)), 2);
        dnd(t, "dnd read-only bytes", map(ByteBuffer.allocate(3).asReadOnlyBuffer()), 2);
        HashMap<String, Object> pixels = new HashMap<>();
        GtkGlassChild.onFx(() -> pixels.put("text/plain",
                Application.GetApplication().createPixels(2, 1, IntBuffer.wrap(new int[] {0xFF102030, 0x80405060}))));
        dnd(t, "dnd pixels", pixels, 3);
        dnd(t, "dnd throwing pixels", map(GtkGlassChild.onFx(RefusingPixels::new)), 3);
        dnd(t, "dnd strings", map(new String[] {"/tmp/a", "b/c"}), 4);
        dnd(t, "dnd no strings", map(new String[0]), 4);
        dnd(t, "dnd null string", map(new String[] {"x", null}), 4);
        dnd(t, "dnd raw string", map("raw"), 5);
        dnd(t, "dnd raw bytes", map(ByteBuffer.wrap(new byte[] {7, 8})), 5);
        dnd(t, "dnd raw other", map(Integer.valueOf(3)), 5);
        dnd(t, "dnd absent key", new HashMap<>(Map.of("text/html", "x")), 1);
        HashMap<String, Object> refusing = new HashMap<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object get(Object key) {
                throw new IllegalStateException("get refused");
            }
        };
        dnd(t, "dnd throwing map", refusing, 1);
        dnd(t, "dnd no drag", null, 1);
    }

    private static HashMap<String, Object> map(Object value) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("text/plain", value);
        return map;
    }

    private static void dnd(GtkEventTrace t, String step, HashMap<String, Object> data, int as) throws Exception {
        t.step(step);
        t.act(() -> t.note(GtkGlassShim.sourceGetData(data, as)));
    }

    /** A {@code Pixels} whose {@code attachData} throws. */
    private static final class RefusingPixels extends Pixels {
        RefusingPixels() {
            super(1, 1, IntBuffer.allocate(1));
        }

        @Override
        protected void _fillDirectByteBuffer(ByteBuffer bb) {
        }

        @Override
        protected void _attachInt(long ptr, int w, int h, IntBuffer ints, int[] array, int offset) {
            throw new IllegalStateException("attach refused");
        }

        @Override
        protected void _attachByte(long ptr, int w, int h, ByteBuffer bytes, byte[] array, int offset) {
            throw new IllegalStateException("attach refused");
        }
    }

    /**
     * The view a window's C holds after a {@code setView} whose old-view {@code EXIT} threw: Java keeps the old view,
     * the C the new one; closing the new view does not unregister it while the C holds it, and closing the window
     * lets it go.
     */
    private static void heldViews(GtkEventTrace t, Map<String, String> out) throws Exception {
        GtkEventTrace.ViewHooks hooks = new GtkEventTrace.ViewHooks();
        t.step("create the window holding views");
        GtkTraceWindow window = GtkGlassChild.onFx(() -> t.window("h", null, STYLE, "h2", hooks));
        View held = GtkGlassChild.onFx(() -> {
            View view = Application.GetApplication().createView();
            view.setEventHandler(t.viewHandler("h3", hooks));
            return view;
        });
        // set_view sends its EXIT to whatever view the window holds
        t.step("swap with the old view's EXIT throwing");
        t.arm("h2.mouse EXIT", 1);
        View old = GtkGlassChild.onFx(window::getView);
        t.act(() -> {
            try {
                window.setView(held);
                t.note("setView returned");
            } catch (GtkEventTrace.ArmedException e) {
                t.note("threw ArmedException out of Window.setView: " + e.getMessage());
            }
            out.put("held.cHoldsNewView", Boolean.toString(
                    GtkGlassShim.cWindowViewId(window) == GtkGlassShim.viewId(held)));
            out.put("held.javaKeepsOldView", Boolean.toString(window.getView() == old));
            held.close();
            out.put("held.closedButRegistered", Boolean.toString(GtkGlassShim.isViewRegistered(
                    GtkGlassShim.viewId(held))));
        });
        t.step("event to the closed held view");
        t.act(() -> t.note(GtkGlassShim.fireAndRead(203, GtkGlassShim.cWindowViewId(window))));
        t.step("close the window");
        long heldId = GtkGlassChild.onFx(() -> GtkGlassShim.viewId(held));
        t.act(window::close);
        out.put("held.unregisteredAfterClose", Boolean.toString(
                !GtkGlassChild.onFx(() -> GtkGlassShim.isViewRegistered(heldId))));
    }

    // ---------------------------------------------------------------------------------------------
    // Real events (robot)
    // ---------------------------------------------------------------------------------------------

    /**
     * Mouse, key and window events injected with the robot reach the Java peers through the upcall stubs of
     * {@code GtkGlassNative}: every recorded call has an {@code on*} target of that class on its stack, and none has a
     * native method anywhere on it - neither on the event path nor below it, where the toolkit thread runs the main
     * loop and the scenario's calls into the peers ({@code setView}, {@code setBounds}, {@code setVisible},
     * {@code close}).
     */
    @Test
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    public void realEventsTakeTheTablePath() {
        assumeTrue(Boolean.getBoolean("USE_ROBOT"), "input-injecting GTK glass tests need -DUSE_ROBOT=true");
        GtkGlassChildJvm.Run events = GtkGlassChildJvm.run(GtkCallbackTableTest.class, "eventScenario",
                List.of("-DUSE_ROBOT=true"));
        String kinds = events.values().get("kinds");
        assertTrue(kinds != null && kinds.contains("isEnabled") && kinds.contains("mouse") && kinds.contains("key")
                && kinds.contains("view") && kinds.contains("notifyMove"), events.describe());
        assertEquals("", events.values().get("offTable"), events.describe());
    }

    /** Runs in {@link GtkGlassChild}. */
    static void eventScenario(Map<String, String> out) throws Exception {
        Set<String> kinds = new TreeSet<>();
        List<String> offTable = new ArrayList<>();
        GtkEventTrace t = new GtkEventTrace();
        GtkTraceWindow window = GtkGlassChild.onFx(() -> {
            GtkTraceWindow w = new GtkTraceWindow(null, Screen.getMainScreen(), STYLE, call -> {
                String kind = call.split("[ =]")[0];
                stack(kind, kinds, offTable);
            });
            View view = Application.GetApplication().createView();
            view.setEventHandler(new View.EventHandler() {
                @Override
                public void handleViewEvent(View v, long time, int type) {
                    stack("view", kinds, offTable);
                }

                @Override
                public void handleMouseEvent(View v, long time, int type, int button, int x, int y, int xAbs,
                                             int yAbs, int modifiers, boolean isPopupTrigger,
                                             boolean isSynthesized) {
                    stack("mouse", kinds, offTable);
                }

                @Override
                public boolean handleKeyEvent(View v, long time, int action, int keyCode, char[] keyChars,
                                              int modifiers) {
                    stack("key", kinds, offTable);
                    return false;
                }

                @Override
                public int handleDragEnter(View v, int x, int y, int xAbs, int yAbs, int recommendedDropAction,
                                           ClipboardAssistance dropTargetAssistant) {
                    return recommendedDropAction;
                }
            });
            w.setView(view);
            w.setBounds(100, 100, true, true, -1, -1, 300, 200, 0, 0);
            w.setVisible(true);
            return w;
        });
        t.settle();
        t.act(() -> GtkGlassShim.setInputFocus(window.getNativeWindow()));
        int[] origin = GtkGlassChild.onFx(() -> GtkGlassShim.rootPosition(window.getNativeWindow()));
        GlassRobot robot = t.robot();
        t.act(() -> robot.mouseMove(origin[0] + 50, origin[1] + 50));
        t.act(() -> robot.mouseMove(origin[0] + 60, origin[1] + 55));
        t.act(() -> robot.mousePress(MouseButton.PRIMARY));
        t.act(() -> robot.mouseRelease(MouseButton.PRIMARY));
        t.act(() -> robot.keyPress(KeyCode.A));
        t.act(() -> robot.keyRelease(KeyCode.A));
        t.act(() -> window.setBounds(120, 110, true, true, -1, -1, 310, 210, 0, 0));
        t.act(window::close);
        out.put("kinds", String.join(",", kinds));
        out.put("offTable", String.join("\n", offTable));
    }

    /**
     * How the call being handled came from native code, read from the whole stack of the thread that handles it. A
     * native method anywhere on it is a JNI native - one whose C called Java through JNI, or one the thread was
     * running when a downcall it made called back - and is recorded in {@code offTable} with the frames above it.
     * Otherwise the frame nearest to the call that is an upcall target of {@code GtkGlassNative} decides: a slot
     * target of the callback tables is the table path, recorded in {@code kinds}; another upcall
     * ({@code onInvokeLater}, a timer tick) ran Java code that made the call itself, which is not counted.
     */
    static void stack(String kind, Set<String> kinds, List<String> offTable) {
        List<StackWalker.StackFrame> frames = StackWalker.getInstance().walk(Stream::toList);
        for (StackWalker.StackFrame frame : frames) {
            if (frame.isNativeMethod()) {
                StringBuilder sb = new StringBuilder(kind + " has the JNI native " + frame.getClassName() + "."
                        + frame.getMethodName() + " on its stack:");
                for (StackWalker.StackFrame f : frames.subList(0, frames.indexOf(frame) + 1)) {
                    sb.append(' ').append(f.getClassName()).append('.').append(f.getMethodName());
                }
                offTable.add(sb.toString());
                return;
            }
        }
        frames.stream().filter(GtkCallbackTableTest::isUpcallTarget).findFirst()
                .filter(GtkCallbackTableTest::isSlotTarget).ifPresent(frame -> kinds.add(kind));
    }

    private static boolean isUpcallTarget(StackWalker.StackFrame frame) {
        return frame.getClassName().equals("com.sun.glass.ui.gtk.GtkGlassNative")
                && frame.getMethodName().startsWith("on");
    }

    private static boolean isSlotTarget(StackWalker.StackFrame frame) {
        return frame.getClassName().equals("com.sun.glass.ui.gtk.GtkGlassNative") && frame.getMethodName()
                .matches("on(IsEnabled|Notify[A-Za-z]*|NonClientHitTest|GetApplicationName|SourceGetData)");
    }
}
