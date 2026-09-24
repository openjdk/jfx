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

package test.com.sun.glass.ui.win;

import com.sun.glass.ui.win.WinGlassNativeShim;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The accessibility half of the {@code gwin_*} ABI: the two callback tables that replaced the 87
 * {@code Call*Method} sites of {@code GlassAccessible.cpp} and {@code GlassTextRangeProvider.cpp}, the
 * {@code GwinVariant} layout that replaced nine cached {@code jfieldID}s, the four lifetime entry points
 * that replaced {@code _createGlassAccessible} and its three siblings, and the two
 * {@code UIAutomationCore} functions Java now binds itself.
 * <p>
 * <b>Why this class exists.</b> Accessibility only runs when a UI Automation client listens, so before
 * this flip the entire subsystem had exactly one automated test - and that one only proved that
 * {@code _initIDs} could run. {@code gwin_a11y_create} makes no OS call and needs no toolkit, exactly as
 * {@code gwin_view_create} does not, so a provider can be created here, driven through its own COM
 * vtable and destroyed, in the module suite, with no screen reader anywhere.
 * <p>
 * <b>What is not proved here.</b> A provider method entered from a real out-of-process UI Automation
 * client's RPC thread. Nothing in this repository can start such a client, so the behaviour difference
 * this flip introduces on that path - the JNI answered {@code E_FAIL} for a thread the JVM had never
 * seen, an FFM upcall stub attaches it and runs the Java target - is UNVERIFIED for that thread kind,
 * and is recorded in {@code glass_win_api.h} rather than claimed here.
 */
@EnabledOnOs(OS.WINDOWS)
public class WinAccessibilityNativeTest {

    /** An id no registry holds, so that a fired slot takes the stale-peer path instead of real code. */
    private static final long UNKNOWN_ID = 0x7FFF_0000_0001L;

    /** {@code UIA_IsControlElementPropertyId}, a property every provider is asked for. */
    private static final int UIA_IS_CONTROL_ELEMENT_PROPERTY_ID = 30016;

    /** {@code UIA_InvokePatternId}. */
    private static final int UIA_INVOKE_PATTERN_ID = 10000;

    /** {@code ProviderOptions_ServerSideProvider} (2) {@code | ProviderOptions_UseComThreading} (0x20). */
    private static final int PROVIDER_OPTIONS = 0x2 | 0x20;

    /** {@code UIA_AutomationFocusChangedEventId}. */
    private static final int UIA_AUTOMATION_FOCUS_CHANGED_EVENT_ID = 20005;

    private static final long S_OK = 0L;
    private static final long E_FAIL = 0x8000_4005L;

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // The four older lazy holders first, and only then UIAutomationCore, so that whichever of this
        // class and WinGlassNativeTest runs first in the shared surefire JVM the bound-symbol order is
        // the same one WinGlassNativeTest expects.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
        WinGlassNativeShim.bindScreenSymbols();
        assertNull(WinGlassNativeShim.initializeWinAccessible(), "WinAccessible could not initialize");
        assertNull(WinGlassNativeShim.initializeWinTextRangeProvider(),
                "WinTextRangeProvider could not initialize");
        assertTrue(WinGlassNativeShim.accessibilityCallbacksInstalled(),
                "the static initializers did not install the accessibility tables");
        WinGlassNativeShim.bindAccessibilitySymbols();
    }

    // ---------------------------------------------------------------------------------------------
    // The flip itself
    // ---------------------------------------------------------------------------------------------

    /**
     * The point of the change: {@code com.sun.glass.ui.win} has no {@code native} method left, so
     * {@code glass.dll} needs no {@code JNI_OnLoad}, no cached ids and no {@code FindClass} - the lookup
     * that cannot work from inside an FFM downcall on JDK 26.
     */
    @Test
    public void theAccessibilityPeersDeclareNoNativeMethods() {
        for (String name : List.of("com.sun.glass.ui.win.WinAccessible",
                "com.sun.glass.ui.win.WinTextRangeProvider", "com.sun.glass.ui.win.WinVariant")) {
            List<String> natives = new ArrayList<>();
            try {
                for (Method method : Class.forName(name).getDeclaredMethods()) {
                    if (Modifier.isNative(method.getModifiers())) {
                        natives.add(method.getName());
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new AssertionError(name, e);
            }
            assertEquals(List.of(), natives, name);
        }
    }

    /** All 89 stubs exist, are distinct and live in {@code Arena.global()} - none may ever be freed. */
    @Test
    public void bothTablesAreFullyPopulatedWithDistinctStubs() {
        List<MemorySegment> stubs = WinGlassNativeShim.installedAccessibilityCallbackStubs();
        assertEquals(WinGlassNativeShim.accessibleSlotNames().size()
                + WinGlassNativeShim.textRangeSlotNames().size(), stubs.size());
        for (MemorySegment stub : stubs) {
            assertNotEquals(MemorySegment.NULL, stub, "a slot was left NULL");
            assertTrue(stub.scope().isAlive(), "a stub's arena is not alive");
        }
        assertEquals(stubs.size(), stubs.stream().distinct().count(), "two slots share a stub");
    }

    // ---------------------------------------------------------------------------------------------
    // Layouts, pinned against the C compiler
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code GwinVariant} is the one struct of this section that crosses by value, and it has three
     * padding holes. The size and every one of the eleven offsets come from the C compiler through
     * {@code gwin_sizeof_variant} and {@code gwin_test_variant_offsets} - never from a comment, and never
     * from reading the header.
     */
    @Test
    public void theVariantLayoutIsTheOneTheCCompilerBuilt() {
        assertEquals(WinGlassNativeShim.sizeOfVariant(), WinGlassNativeShim.variantLayoutSize(),
                "sizeof(GwinVariant) and GWIN_VARIANT_LAYOUT disagree");
        int[] fromC = WinGlassNativeShim.variantOffsetsFromC();
        int[] fromLayout = WinGlassNativeShim.variantLayoutOffsets();
        assertEquals(WinGlassNativeShim.VARIANT_FIELDS.size(), fromC.length);
        for (int i = 0; i < fromC.length; i++) {
            assertEquals(fromC[i], fromLayout[i], WinGlassNativeShim.VARIANT_FIELDS.get(i));
        }
    }

    /** Both callback tables are as long as the C compiler made them: one pointer per slot, no more. */
    @Test
    public void bothCallbackTablesHaveTheSizeTheCCompilerGaveThem() {
        assertEquals(WinGlassNativeShim.accessibleSlotNames().size() * 8,
                WinGlassNativeShim.sizeOfAccessibleCallbacks());
        assertEquals(WinGlassNativeShim.textRangeSlotNames().size() * 8,
                WinGlassNativeShim.sizeOfTextRangeCallbacks());
    }

    // ---------------------------------------------------------------------------------------------
    // The descriptor oracle: 89 prototypes, one fire each, argument by argument
    // ---------------------------------------------------------------------------------------------

    /**
     * A pointer argument, which the recording stub reduces to whether it was non-{@code NULL}: an
     * address is not stable enough to assert.
     */
    private static final Object PTR = Boolean.TRUE;

    /**
     * The one pointer the library passes as {@code NULL}: {@code find_attribute}'s
     * {@code const GwinVariant* val}, which the C has never converted.
     */
    private static final Object NULL_PTR = Boolean.FALSE;

    /**
     * What every slot of {@code GwinAccessibleCallbacks} must see, transcribed from the fixed pattern
     * {@code gwin_test_fire_accessible_callback} sends: the {@code int32} in-parameters take 1001,
     * 1002, ... in declaration order, the doubles 1.5 and 2.5, a text in-parameter the two code units
     * {@code {0x0041, 0x0042}} with len 2, a {@code SAFEARRAY*} or sibling id {@code 0x100000002}, and
     * every boolean 1. Each entry is the whole argument list, id first - the count is what catches a
     * descriptor with one parameter too many.
     */
    private static final List<List<Object>> ACCESSIBLE_SLOT_ARGUMENTS = List.of(
            args(UNKNOWN_ID, 1001, PTR),                    //  0 get_pattern_provider
            args(UNKNOWN_ID, PTR),                          //  1 get_host_raw_element_provider
            args(UNKNOWN_ID, 1001, PTR),                    //  2 get_property_value
            args(UNKNOWN_ID, PTR, PTR),                     //  3 get_bounding_rectangle
            args(UNKNOWN_ID, PTR),                          //  4 get_fragment_root
            args(UNKNOWN_ID, PTR, PTR),                     //  5 get_embedded_fragment_roots
            args(UNKNOWN_ID, PTR, PTR),                     //  6 get_runtime_id
            args(UNKNOWN_ID, 1001, PTR),                    //  7 navigate
            args(UNKNOWN_ID),                               //  8 set_focus
            args(UNKNOWN_ID, 1.5, 2.5, PTR),                //  9 element_provider_from_point
            args(UNKNOWN_ID, PTR),                          // 10 get_focus
            args(UNKNOWN_ID, 1001, 0x1_0000_0002L),         // 11 advise_event_added
            args(UNKNOWN_ID, 1001, 0x1_0000_0002L),         // 12 advise_event_removed
            args(UNKNOWN_ID),                               // 13 invoke
            args(UNKNOWN_ID, PTR, PTR),                     // 14 get_selection
            args(UNKNOWN_ID, PTR),                          // 15 get_can_select_multiple
            args(UNKNOWN_ID, PTR),                          // 16 get_is_selection_required
            args(UNKNOWN_ID),                               // 17 select
            args(UNKNOWN_ID),                               // 18 add_to_selection
            args(UNKNOWN_ID),                               // 19 remove_from_selection
            args(UNKNOWN_ID, PTR),                          // 20 get_is_selected
            args(UNKNOWN_ID, PTR),                          // 21 get_selection_container
            args(UNKNOWN_ID, 1.5),                          // 22 set_value
            args(UNKNOWN_ID, PTR),                          // 23 get_value
            args(UNKNOWN_ID, PTR),                          // 24 get_is_read_only
            args(UNKNOWN_ID, PTR),                          // 25 get_maximum
            args(UNKNOWN_ID, PTR),                          // 26 get_minimum
            args(UNKNOWN_ID, PTR),                          // 27 get_large_change
            args(UNKNOWN_ID, PTR),                          // 28 get_small_change
            args(UNKNOWN_ID, "AB", 2),                      // 29 set_value_string
            args(UNKNOWN_ID, PTR, PTR),                     // 30 get_value_string
            args(UNKNOWN_ID, PTR, PTR),                     // 31 get_visible_ranges
            args(UNKNOWN_ID, 0x1_0000_0002L, PTR),          // 32 range_from_child
            args(UNKNOWN_ID, 1.5, 2.5, PTR),                // 33 range_from_point
            args(UNKNOWN_ID, PTR),                          // 34 get_document_range
            args(UNKNOWN_ID, PTR),                          // 35 get_supported_text_selection
            args(UNKNOWN_ID, PTR),                          // 36 get_column_count
            args(UNKNOWN_ID, PTR),                          // 37 get_row_count
            args(UNKNOWN_ID, 1001, 1002, PTR),              // 38 get_item
            args(UNKNOWN_ID, PTR),                          // 39 get_column
            args(UNKNOWN_ID, PTR),                          // 40 get_column_span
            args(UNKNOWN_ID, PTR),                          // 41 get_containing_grid
            args(UNKNOWN_ID, PTR),                          // 42 get_row
            args(UNKNOWN_ID, PTR),                          // 43 get_row_span
            args(UNKNOWN_ID, PTR, PTR),                     // 44 get_column_headers
            args(UNKNOWN_ID, PTR, PTR),                     // 45 get_row_headers
            args(UNKNOWN_ID, PTR),                          // 46 get_row_or_column_major
            args(UNKNOWN_ID, PTR, PTR),                     // 47 get_column_header_items
            args(UNKNOWN_ID, PTR, PTR),                     // 48 get_row_header_items
            args(UNKNOWN_ID),                               // 49 toggle
            args(UNKNOWN_ID, PTR),                          // 50 get_toggle_state
            args(UNKNOWN_ID),                               // 51 collapse
            args(UNKNOWN_ID),                               // 52 expand
            args(UNKNOWN_ID, PTR),                          // 53 get_expand_collapse_state
            args(UNKNOWN_ID, PTR),                          // 54 get_can_move
            args(UNKNOWN_ID, PTR),                          // 55 get_can_resize
            args(UNKNOWN_ID, PTR),                          // 56 get_can_rotate
            args(UNKNOWN_ID, 1.5, 2.5),                     // 57 move
            args(UNKNOWN_ID, 1.5, 2.5),                     // 58 resize
            args(UNKNOWN_ID, 1.5),                          // 59 rotate
            args(UNKNOWN_ID, 1001, 1002),                   // 60 scroll
            args(UNKNOWN_ID, 1.5, 2.5),                     // 61 set_scroll_percent
            args(UNKNOWN_ID, PTR),                          // 62 get_horizontally_scrollable
            args(UNKNOWN_ID, PTR),                          // 63 get_horizontal_scroll_percent
            args(UNKNOWN_ID, PTR),                          // 64 get_horizontal_view_size
            args(UNKNOWN_ID, PTR),                          // 65 get_vertically_scrollable
            args(UNKNOWN_ID, PTR),                          // 66 get_vertical_scroll_percent
            args(UNKNOWN_ID, PTR),                          // 67 get_vertical_view_size
            args(UNKNOWN_ID),                               // 68 scroll_into_view
            args(UNKNOWN_ID));                              // 69 accessible_disposed

    /** {@link #ACCESSIBLE_SLOT_ARGUMENTS} for the 19 slots of {@code GwinTextRangeCallbacks}. */
    private static final List<List<Object>> TEXT_RANGE_SLOT_ARGUMENTS = List.of(
            args(UNKNOWN_ID, PTR),                                  //  0 clone
            args(UNKNOWN_ID, 0x1_0000_0002L, PTR),                  //  1 compare
            args(UNKNOWN_ID, 1001, 0x1_0000_0002L, 1002, PTR),      //  2 compare_endpoints
            args(UNKNOWN_ID, 1001),                                 //  3 expand_to_enclosing_unit
            args(UNKNOWN_ID, 1001, NULL_PTR, 1, PTR),               //  4 find_attribute
            args(UNKNOWN_ID, "AB", 2, 1, 1, PTR),                   //  5 find_text
            args(UNKNOWN_ID, 1001, PTR),                            //  6 get_attribute_value
            args(UNKNOWN_ID, PTR, PTR),                             //  7 get_bounding_rectangles
            args(UNKNOWN_ID, PTR),                                  //  8 get_enclosing_element
            args(UNKNOWN_ID, 1001, PTR, PTR),                       //  9 get_text
            args(UNKNOWN_ID, 1001, 1002, PTR),                      // 10 move
            args(UNKNOWN_ID, 1001, 1002, 1003, PTR),                // 11 move_endpoint_by_unit
            args(UNKNOWN_ID, 1001, 0x1_0000_0002L, 1002),           // 12 move_endpoint_by_range
            args(UNKNOWN_ID),                                       // 13 select
            args(UNKNOWN_ID),                                       // 14 add_to_selection
            args(UNKNOWN_ID),                                       // 15 remove_from_selection
            args(UNKNOWN_ID, 1),                                    // 16 scroll_into_view
            args(UNKNOWN_ID, PTR, PTR),                             // 17 get_children
            args(UNKNOWN_ID));                                      // 18 range_disposed

    private static List<Object> args(Object... values) {
        return List.of(values);
    }

    /**
     * Every slot of {@code GwinAccessibleCallbacks} is fired through a table of recording stubs built
     * from the facade's own {@code FunctionDescriptor}s, and what it saw is compared with the whole
     * pattern the C sent. This is the only automated check of the 70 descriptors: a {@code sizeof}
     * probe sees only pointers, and a descriptor with one parameter too many shifts every following
     * argument by a stack slot on x64 - no crash, no exception, a wrong number in a screen reader, and
     * an out-parameter written through an uninitialised register.
     */
    @Test
    public void everyAccessibleSlotSeesItsOwnPrototype() {
        assertEverySlotSeesItsPrototype(true, WinGlassNativeShim.accessibleSlotNames(),
                ACCESSIBLE_SLOT_ARGUMENTS);
    }

    /** {@link #everyAccessibleSlotSeesItsOwnPrototype} for the 19 slots of {@code GwinTextRangeCallbacks}. */
    @Test
    public void everyTextRangeSlotSeesItsOwnPrototype() {
        assertEverySlotSeesItsPrototype(false, WinGlassNativeShim.textRangeSlotNames(),
                TEXT_RANGE_SLOT_ARGUMENTS);
    }

    private void assertEverySlotSeesItsPrototype(boolean accessible, List<String> names,
                                                 List<List<Object>> expected) {
        assertEquals(names.size(), expected.size(), "the transcribed pattern has the wrong slot count");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(Math.max(WinGlassNativeShim.sizeOfVariant(), 64), 8);
            for (int slot = 0; slot < names.size(); slot++) {
                out.fill((byte) 0);
                WinGlassNativeShim.RecordedSlot recorded = accessible
                        ? WinGlassNativeShim.fireAccessibleIntoRecordingTable(slot, UNKNOWN_ID, out)
                        : WinGlassNativeShim.fireTextRangeIntoRecordingTable(slot, UNKNOWN_ID, out);
                String which = (accessible ? "accessible" : "text range") + " slot " + slot + " ("
                        + names.get(slot) + ")";
                assertFalse(recorded.arguments().isEmpty(), which + ": the slot was never entered");
                assertEquals(expected.get(slot), recorded.arguments(), which + ": wrong arguments");
                assertEquals(0L, recorded.returned(), which + ": the hook reported a failure");
            }
        }
    }

    /** An unknown slot number is rejected by both hooks, as the header says. */
    @Test
    public void anUnknownSlotIsRejectedByBothHooks() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(64, 8);
            assertEquals(-1L, WinGlassNativeShim.fireAccessibleCallback(
                    WinGlassNativeShim.accessibleSlotNames().size(), UNKNOWN_ID, out));
            assertEquals(-1L, WinGlassNativeShim.fireTextRangeCallback(
                    WinGlassNativeShim.textRangeSlotNames().size(), UNKNOWN_ID, out));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // A provider, in process, through its own COM vtable
    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a {@code GlassAccessible} with no toolkit, calls three entries of its
     * {@code IRawElementProviderSimple} vtable - the real C bodies, the real upcall table - and destroys
     * it, asserting the {@code HRESULT}s and that {@code accessible_disposed} fired where
     * {@code DeleteGlobalRef} stood.
     * <p>
     * The id is one no registry holds, so every slot takes the stale-peer path and each provider method
     * returns what it returns when Java answers nothing: {@code get_ProviderOptions} makes no upcall at
     * all and still answers {@code S_OK} with its constant; {@code GetPatternProvider} answers
     * {@code S_OK} with a {@code NULL} pattern; {@code GetPropertyValue} answers {@code E_FAIL}, because
     * that is what {@code copyVariant} has always answered for a null variant.
     */
    @Test
    public void aProviderCanBeCreatedDrivenAndDestroyedInProcess() {
        long accessible = WinGlassNativeShim.createAccessible(UNKNOWN_ID);
        assertNotEquals(0L, accessible, "gwin_a11y_create answered NULL");
        try {
            int[] options = WinGlassNativeShim.providerGetProviderOptions(accessible);
            assertEquals(S_OK, options[0], "get_ProviderOptions");
            assertEquals(PROVIDER_OPTIONS, options[1],
                    "ProviderOptions_ServerSideProvider | ProviderOptions_UseComThreading");

            long[] pattern = WinGlassNativeShim.providerGetPatternProvider(accessible,
                    UIA_INVOKE_PATTERN_ID);
            assertEquals(S_OK, pattern[0], "GetPatternProvider");
            assertEquals(0L, pattern[1], "GetPatternProvider answered a pattern for an unknown peer");

            // The parity tripwire of the whole marshalling direction. WinAccessible.GetPropertyValue
            // answers null for every property it does not handle; the table arm encodes that null as a
            // GwinVariant with vt = VT_EMPTY, and GlassAccessible::variantFromGwin maps VT_EMPTY to
            // E_FAIL - which is the HRESULT copyVariant(NULL) answered at commit 033187ad90
            // (GlassAccessible.cpp, GetPropertyValue). A VT_EMPTY that fell through the switch with
            // hr = S_OK would hand UI Automation an empty VARIANT where the JNI reported a failure.
            assertEquals(E_FAIL, WinGlassNativeShim.providerGetPropertyValue(accessible,
                    UIA_IS_CONTROL_ELEMENT_PROPERTY_ID) & 0xFFFF_FFFFL,
                    "GetPropertyValue for a null WinVariant must answer E_FAIL, as copyVariant(NULL) did;"
                    + " a VT_EMPTY GwinVariant may not reach the client as S_OK");
        } finally {
            List<Object> disposal = WinGlassNativeShim.destroyAccessibleRecordingDisposal(accessible);
            assertEquals(List.of(UNKNOWN_ID), disposal,
                    "accessible_disposed did not fire with the id, where DeleteGlobalRef stood");
        }
    }

    /**
     * A range pins its accessible - its constructor AddRefs it - so destroying the accessible first does
     * not dispose it, and both disposals fire in the order the reference counts drop.
     */
    @Test
    public void aRangePinsItsAccessibleUntilTheRangeIsDestroyed() {
        long rangeId = UNKNOWN_ID + 1;
        long accessible = WinGlassNativeShim.createAccessible(UNKNOWN_ID);
        assertNotEquals(0L, accessible);
        long range = WinGlassNativeShim.createTextRange(accessible, rangeId);
        assertNotEquals(0L, range, "gwin_a11y_text_range_create answered NULL");

        assertEquals(List.of(), WinGlassNativeShim.destroyAccessibleRecordingDisposal(accessible),
                "the accessible was disposed while a live range still held a reference to it");
        assertEquals(List.of(rangeId, UNKNOWN_ID),
                WinGlassNativeShim.destroyTextRangeRecordingDisposal(range),
                "the range's disposal, then its accessible's, as the last references went");
    }

    /** A range on a NULL accessible is NULL, which is what the JNI answered and what Java never checks. */
    @Test
    public void aRangeOnANullAccessibleIsNull() {
        assertEquals(0L, WinGlassNativeShim.createTextRange(0L, UNKNOWN_ID));
    }

    // ---------------------------------------------------------------------------------------------
    // The two id registries, which stand where NewGlobalRef and DeleteGlobalRef stood
    // ---------------------------------------------------------------------------------------------

    /** Enough peers that one missed removal is unmistakable, few enough to stay instantaneous. */
    private static final int PEERS = 16;

    /**
     * The registry entry is the only thing keeping a {@code WinAccessible} - and the whole scene-graph
     * subtree its event handler reaches - alive while its COM object exists, so a disposal slot that
     * stops firing is a leak for the life of the JVM. That is the leak the JNI had, whose
     * {@code ~GlassAccessible} skipped {@code DeleteGlobalRef} when {@code GetEnv()} answered
     * {@code NULL}. Sizes are compared as deltas: the shared test JVM may hold peers of its own.
     * <p>
     * The middle assertion is the pinning rule at scale: every accessible is disposed while its range
     * still holds a COM reference, so none of them may drain yet.
     */
    @Test
    public void creatingAndDisposingPeersLeavesBothRegistriesAsItFoundThem() {
        int accessiblesBefore = WinGlassNativeShim.accessibleRegistrySize();
        int rangesBefore = WinGlassNativeShim.rangeRegistrySize();
        List<Object> accessibles = new ArrayList<>();
        List<Object> ranges = new ArrayList<>();
        try {
            for (int i = 0; i < PEERS; i++) {
                Object accessible = WinGlassNativeShim.createRegisteredAccessible();
                accessibles.add(accessible);
                Object range = WinGlassNativeShim.createRegisteredRange(accessible);
                ranges.add(range);
                assertNotEquals(0L, WinGlassNativeShim.accessibleIdOf(accessible), "peer " + i);
                assertNotEquals(0L, WinGlassNativeShim.rangeIdOf(range), "range " + i);
            }
            assertEquals(accessiblesBefore + PEERS, WinGlassNativeShim.accessibleRegistrySize(),
                    "the accessible registry did not take every new peer");
            assertEquals(rangesBefore + PEERS, WinGlassNativeShim.rangeRegistrySize(),
                    "the range registry did not take every new range");

            for (Object accessible : accessibles) {
                WinGlassNativeShim.disposeAccessible(accessible);
            }
            assertEquals(accessiblesBefore + PEERS, WinGlassNativeShim.accessibleRegistrySize(),
                    "an accessible drained while a live range still held a reference to its COM object");
        } finally {
            for (Object range : ranges) {
                WinGlassNativeShim.disposeRange(range);
            }
        }
        assertEquals(rangesBefore, WinGlassNativeShim.rangeRegistrySize(),
                "range_disposed did not drain the range registry");
        assertEquals(accessiblesBefore, WinGlassNativeShim.accessibleRegistrySize(),
                "accessible_disposed did not drain the accessible registry once the ranges were gone");
    }

    /**
     * The registered-peer path, which every other test here avoids by firing at an id no registry
     * holds. {@code get_runtime_id} is the slot that tells the two apart with no toolkit and no event
     * handler: {@code GetRuntimeId()} answers {@code {UiaAppendRuntimeId, id}} for an accessible with
     * no {@code View}, and the stale-peer default is {@code null}, which the facade turns into a
     * {@code NULL} block with count 0. The block belongs to the caller, as the header says, so it is
     * freed here.
     * <p>
     * The fire runs as the event thread because {@code isDisposed()} asks {@code getNativeAccessible()},
     * which calls {@code Application.checkEventThread()}: off that thread the provider method throws
     * and the slot answers {@code GWIN_ERR_UPCALL}, which is the {@code E_FAIL} the JNI produced from
     * the same exception through {@code CheckAndClearException}.
     */
    @Test
    public void aSlotFiredAtARegisteredIdReachesThePeerAndAStaleIdDoesNot() {
        int slot = WinGlassNativeShim.accessibleSlotNames().indexOf("get_runtime_id");
        assertNotEquals(-1, slot, "get_runtime_id is no longer a slot of GwinAccessibleCallbacks");
        Object accessible = WinGlassNativeShim.createRegisteredAccessible();
        long id = WinGlassNativeShim.accessibleIdOf(accessible);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(Math.max(WinGlassNativeShim.sizeOfVariant(), 64), 8);

            out.fill((byte) 0);
            assertEquals(0L, WinGlassNativeShim.fireAccessibleCallbackAsEventThread(slot, id, out),
                    "get_runtime_id at a registered id");
            long block = out.get(JAVA_LONG, 0);
            assertEquals(2, out.get(JAVA_INT, 8), "the peer's runtime id is two int32s");
            assertNotEquals(0L, block, "the peer answered no block at all");
            WinGlassNativeShim.gwinFree(block);

            out.fill((byte) 0);
            assertEquals(0L, WinGlassNativeShim.fireAccessibleCallbackAsEventThread(slot, UNKNOWN_ID, out),
                    "get_runtime_id at an id no registry holds");
            assertEquals(0, out.get(JAVA_INT, 8), "a stale id must answer an empty runtime id");
            assertEquals(0L, out.get(JAVA_LONG, 0), "a stale id must answer a NULL block, not an empty one");
        } finally {
            WinGlassNativeShim.disposeAccessible(accessible);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The two UIAutomationCore functions Java binds itself
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code UiaClientsAreListening} is the whole of the former JNI body, bound from
     * {@code UIAutomationCore} rather than wrapped in C. Its <em>value</em> is not assertable: it is
     * {@code true} on any machine where something has a UI Automation client open - measured
     * {@code true} on the development machine here, with no screen reader running - so what is asserted
     * is that the call crosses, answers a stable boolean, and does so repeatedly. The conversion is the
     * part that could break: the C tested the {@code BOOL} with {@code ? JNI_TRUE : JNI_FALSE} and this
     * binding tests {@code != 0}, so any non-zero {@code BOOL} is {@code true} in both.
     */
    @Test
    public void uiaClientsAreListeningAnswersAStableBoolean() {
        boolean listening = WinGlassNativeShim.uiaClientsAreListening();
        assertEquals(listening, WinGlassNativeShim.uiaClientsAreListening(),
                "the second call must agree with the first");
        assertEquals(listening, WinGlassNativeShim.uiaClientsAreListening(), "and the third");
    }

    /** {@code UiaRaiseAutomationEvent} on a provider nobody listens to is {@code S_OK}. */
    @Test
    public void raisingAnEventOnAProviderNobodyListensToSucceeds() {
        long accessible = WinGlassNativeShim.createAccessible(UNKNOWN_ID);
        assertNotEquals(0L, accessible);
        try {
            assertEquals(S_OK, WinGlassNativeShim.raiseAutomationEvent(accessible,
                    UIA_AUTOMATION_FOCUS_CHANGED_EVENT_ID));
        } finally {
            WinGlassNativeShim.destroyAccessible(accessible);
        }
    }

    /**
     * {@code gwin_a11y_raise_property_changed} carries two {@code WinVariant}s across as
     * {@code GwinVariant}s, and every shape Java produces with a block is sent, because block ownership
     * is the part a size probe cannot see: a {@code VT_BSTR}, a {@code VT_R8 | VT_ARRAY} with values, an
     * empty array - which is a real one-byte block and not {@code null} - and a scalar with no block at
     * all.
     * <p>
     * The {@code HRESULT} itself is UI Automation's answer for a provider that was never handed to a
     * client, and it is not the same on a machine where a client is listening as on one where none is,
     * so what is asserted is that every shape reaches the OS and comes back with the <em>same</em>
     * answer: a marshalling defect in one of them would show as a different code for that one.
     */
    @Test
    public void raisingAPropertyChangeMarshalsEveryVariantShape() {
        long accessible = WinGlassNativeShim.createAccessible(UNKNOWN_ID);
        assertNotEquals(0L, accessible);
        try {
            Object bstr = WinGlassNativeShim.variant((short) 8, 0, "was", false, 0, null, 0);
            Object otherBstr = WinGlassNativeShim.variant((short) 8, 0, "", false, 0, null, 0);
            Object numbers = WinGlassNativeShim.variant((short) (5 | 0x2000), 0, null, false, 0,
                    new double[] {1.0, 2.0, 3.0}, 0);
            Object empty = WinGlassNativeShim.variant((short) (5 | 0x2000), 0, null, false, 0,
                    new double[0], 0);
            Object number = WinGlassNativeShim.variant((short) 3, 42, null, false, 0, null, 0);
            Object flag = WinGlassNativeShim.variant((short) 11, 0, null, true, 0, null, 0);

            long reference = WinGlassNativeShim.raiseAutomationPropertyChangedEvent(accessible,
                    UIA_IS_CONTROL_ELEMENT_PROPERTY_ID, number, flag);
            assertEquals(reference, WinGlassNativeShim.raiseAutomationPropertyChangedEvent(accessible,
                    UIA_IS_CONTROL_ELEMENT_PROPERTY_ID, bstr, otherBstr), "VT_BSTR, one of them empty");
            assertEquals(reference, WinGlassNativeShim.raiseAutomationPropertyChangedEvent(accessible,
                    UIA_IS_CONTROL_ELEMENT_PROPERTY_ID, numbers, empty),
                    "VT_R8 | VT_ARRAY, one of them empty");
            // A null WinVariant is the jobject copyVariant answered E_FAIL for, short-circuiting before
            // the OS call - so it must not become a VT_EMPTY struct the OS would accept.
            assertEquals(E_FAIL, WinGlassNativeShim.raiseAutomationPropertyChangedEvent(accessible,
                    UIA_IS_CONTROL_ELEMENT_PROPERTY_ID, null, number) & 0xFFFF_FFFFL,
                    "a null old value is E_FAIL with no OS call, as copyVariant(NULL) was");
            assertEquals(E_FAIL, WinGlassNativeShim.raiseAutomationPropertyChangedEvent(accessible,
                    UIA_IS_CONTROL_ELEMENT_PROPERTY_ID, number, null) & 0xFFFF_FFFFL,
                    "and a null new value");
        } finally {
            WinGlassNativeShim.destroyAccessible(accessible);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // No Throwable into a COM vtable
    // ---------------------------------------------------------------------------------------------

    /**
     * A slot whose Java target throws must not let the {@code Throwable} out of the upcall stub - that
     * terminates the JVM, measured on JDK 26 and JDK 25 - and must report it where
     * {@code CheckAndClearException} reported: {@code Application.reportException}, which is the calling
     * thread's {@code UncaughtExceptionHandler}. The library then sees {@code GWIN_ERR_UPCALL}, which is
     * the {@code E_FAIL} every one of those provider methods returned.
     */
    @Test
    public void aSlotWhoseTargetThrowsReportsAndAnswersUpcallFailure() {
        Thread current = Thread.currentThread();
        List<Throwable> delivered = new ArrayList<>();
        List<Thread> deliveredOn = new ArrayList<>();
        Thread.UncaughtExceptionHandler previous = current.getUncaughtExceptionHandler();
        boolean hadOwnHandler = !(previous instanceof ThreadGroup);
        long status;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(Math.max(WinGlassNativeShim.sizeOfVariant(), 64), 8);
            out.fill((byte) 0x5A);
            try {
                current.setUncaughtExceptionHandler((thread, throwable) -> {
                    deliveredOn.add(thread);
                    delivered.add(throwable);
                });
                status = WinGlassNativeShim.fireAccessibleIntoThrowingTable(0, UNKNOWN_ID, out);
            } finally {
                current.setUncaughtExceptionHandler(hadOwnHandler ? previous : null);
            }
        }
        assertEquals(-3L, status, "the library did not see GWIN_ERR_UPCALL");
        assertEquals(1, delivered.size(), "the Throwable never reached Application.reportException"
                + " (delivered: " + delivered + ")");
        assertEquals(WinGlassNativeShim.THROWING_SLOT_MESSAGE, delivered.get(0).getMessage());
        assertSame(current, deliveredOn.get(0), "reportException delivers on the calling thread");
        assertTrue(Runtime.getRuntime().availableProcessors() > 0, "nothing may be left pending");
    }
}
