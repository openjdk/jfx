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

package test.com.sun.webkit.ffm;

import com.sun.webkit.WebKitNativeShim;
import com.sun.webkit.WkjStubShim;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.PaddingLayout;
import java.lang.foreign.SequenceLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Java struct layouts against the C {@code sizeof} and {@code offsetof} the library exports. This is
 * the cross-platform ABI drift check: it runs everywhere, and it is the one that catches a Java
 * layout that is plausible and wrong.
 * <p>
 * Every layout here is a production one, from {@code com.sun.webkit.WKJLayouts}: the exception slot
 * {@code WebKitNative} reads pending exceptions through, the {@code WKJHost} table it builds, fills
 * and installs, the {@code WKJJSValue} {@code JSObjectNative} marshals through, and the page and
 * bridge callback tables the other facades size themselves from. They were checked against a copy
 * owned by the test shim until production grew tables of its own; they are checked against the
 * production ones now, because a layout that nothing writes through cannot drift in a way that
 * matters.
 * <p>
 * Four things are compared, in increasing order of what they would let through:
 * {@code sizeof(WKJHost)}; the size and offset of each of the nine filled groups, so that a group
 * which grew and one which shrank cannot cancel out; the offset of every one of the 168 callback
 * slots; and the {@link java.lang.foreign.FunctionDescriptor} each filled slot was bound with,
 * against the C prototype the header declares.
 * <p>
 * It has caught two already. The first hand written {@code WKJExceptionSlot} layout computed 528
 * bytes against the C struct's 524, because it carried a trailing four byte padding that three
 * {@code int32_t} followed by {@code uint16_t[256]} do not need; and {@code WKJHost} was modelled as
 * thirteen pointer sized groups totalling 160 bytes while C declared sixteen groups totalling 1352,
 * which {@code wkj_init} rejected outright.
 */
@Tag("ffm")
public class WebKitLayoutTest {

    /** {@code sizeof(WKJExceptionSlot)}: three int32_t and an inline uint16_t[256], no padding. */
    private static final long EXCEPTION_SLOT_SIZE = 524L;

    /**
     * {@code sizeof(WKJHost)}: an {@code int32_t}, four bytes of padding, and sixteen groups - seven
     * one-pointer placeholders and nine real tables.
     */
    private static final long HOST_SIZE = 1352L;

    /** {@code sizeof(WKJHostCore)}: seven function pointers. */
    private static final long HOST_CORE_SIZE = 56L;

    /** The number of callback slots {@code WKJHost} carries, flattened over its sixteen groups. */
    private static final int HOST_SLOT_COUNT = 168;

    /**
     * The slots production deliberately leaves NULL: the seven {@code *.reserved} placeholders,
     * {@code pal.system_beep} and {@code theme.plugin_widget_paint}. They are named and justified in
     * {@code WebKitHostInstallTest}; here only the count matters, because a slot with no stub has no
     * descriptor to compare.
     */
    private static final int DELIBERATELY_NULL_SLOTS = 9;

    /**
     * One filled group of {@code WKJHost}: its member name, the C struct behind it, and the size and
     * offset the C compiler gives it.
     */
    private record Group(String member, String struct, long size, long offset) {
    }

    /**
     * The nine groups that carry real slots, so that a group which gains or loses one fails here
     * naming the group rather than only as a difference in {@code sizeof(WKJHost)} - which two
     * groups changing by equal and opposite amounts would not move at all. The numbers were measured
     * by compiling the current headers.
     */
    private static final List<Group> GROUPS = List.of(
            new Group("core", "WKJHostCore", 56L, 8L),
            new Group("graphics", "WKJHostGraphics", 552L, 120L),
            new Group("network", "WKJHostNetwork", 88L, 672L),
            new Group("media", "WKJHostMedia", 128L, 760L),
            new Group("filesystem", "WKJHostFileSystem", 80L, 888L),
            new Group("theme", "WKJHostTheme", 344L, 968L),
            new Group("wtf", "WKJHostWTF", 8L, 1312L),
            new Group("pal", "WKJHostPAL", 32L, 1320L));

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @Test
    public void exceptionSlotMatchesTheCStruct() {
        MemoryLayout layout = WebKitNativeShim.exceptionSlotLayout();
        assertEquals(WkjStubShim.sizeOf("WKJExceptionSlot"), layout.byteSize(),
                "the Java WKJExceptionSlot layout disagrees with the C sizeof");
        assertEquals(EXCEPTION_SLOT_SIZE, layout.byteSize(),
                "sizeof(WKJExceptionSlot) is no longer 524; the ABI version must be bumped");

        for (String field : List.of("type", "code", "message_length", "message")) {
            assertEquals(WkjStubShim.offsetOf("WKJExceptionSlot", field),
                    layout.byteOffset(PathElement.groupElement(field)),
                    "offsetof(WKJExceptionSlot, " + field + ") disagrees");
        }
    }

    @Test
    public void theExceptionMessageIsAnInlineArrayOfTheSizeCDeclares() {
        MemoryLayout layout = WebKitNativeShim.exceptionSlotLayout();
        MemoryLayout message = layout.select(PathElement.groupElement("message"));
        SequenceLayout sequence = assertInstanceOf(SequenceLayout.class, message,
                "message is an inline uint16_t[WKJ_EXC_MESSAGE_MAX], so it must be a sequenceLayout;"
                        + " a pointer here would read a length as an address");

        int struct = WkjStubShim.findStruct("WKJExceptionSlot");
        int field = WkjStubShim.findStructField(struct, "message");
        assertEquals(WkjStubShim.structFieldElements(struct, field), sequence.elementCount(),
                "the inline message holds a different number of code units in C");
        assertEquals(WkjStubShim.structFieldSize(struct, field), sequence.byteSize(),
                "the inline message is a different size in C");
        assertEquals('h', WkjStubShim.structFieldKind(struct, field),
                "the C message elements are no longer uint16_t");
    }

    @Test
    public void theExceptionSlotHasNoTrailingPadding() {
        GroupLayout layout = (GroupLayout) WebKitNativeShim.exceptionSlotLayout();
        List<MemoryLayout> members = layout.memberLayouts();
        assertFalse(members.get(members.size() - 1) instanceof PaddingLayout,
                "the Java layout ends in padding the C struct does not have, which makes it 528"
                        + " bytes against C's 524 and moves nothing but breaks everything");
    }

    @Test
    public void everyStructTheHeaderDeclaresHasAJavaLayoutOfTheSameShape() {
        Map<String, MemoryLayout> declared = WebKitNativeShim.declaredLayouts();
        Set<String> fromC = new TreeSet<>();
        for (int i = 0, n = WkjStubShim.structCount(); i < n; i++) {
            String name = WkjStubShim.structName(i);
            fromC.add(name);
            MemoryLayout layout = declared.get(name);
            assertNotNull(layout, "the C header declares " + name + " and Java declares no layout"
                    + " for it, so nothing checks it");
            assertEquals(WkjStubShim.structSize(i), layout.byteSize(),
                    "sizeof(" + name + ") disagrees");
            for (int f = 0, fields = WkjStubShim.structFieldCount(i); f < fields; f++) {
                String field = WkjStubShim.structFieldName(i, f);
                assertEquals(WkjStubShim.structFieldOffset(i, f),
                        layout.byteOffset(PathElement.groupElement(field)),
                        "offsetof(" + name + ", " + field + ") disagrees");
                assertEquals(WkjStubShim.structFieldSize(i, f),
                        layout.select(PathElement.groupElement(field)).byteSize(),
                        "sizeof(" + name + "." + field + ") disagrees");
            }
        }
        assertEquals(fromC, new TreeSet<>(declared.keySet()),
                "the set of structs Java declares layouts for and the set the C header declares"
                        + " must be the same, so that a new struct cannot be silently ignored");
    }

    /**
     * The layout checked here is the production one - the {@code WKJHost} layout
     * {@code WebKitNative} builds its table from and hands to {@code wkj_init} - not a copy owned by
     * the test scaffolding. That distinction is the whole value of this check: a layout that is only
     * ever compared against C, and never used to write a table, proves nothing about the table the
     * library will read.
     */
    @Test
    public void hostTableMatchesTheCStruct() {
        assertEquals(WkjStubShim.sizeOf("WKJHost"), WebKitNativeShim.hostLayout().byteSize(),
                "the Java WKJHost layout disagrees with the C sizeof");
        assertEquals(HOST_SIZE, WebKitNativeShim.hostLayout().byteSize(),
                "sizeof(WKJHost) is no longer 1352; the ABI version must be bumped");
        assertEquals(HOST_SIZE, WebKitNativeShim.hostByteSize(),
                "the size production passes to wkj_init must be the size of the layout it built");
        assertEquals(HOST_CORE_SIZE, WkjStubShim.sizeOf("WKJHostCore"),
                "sizeof(WKJHostCore) is no longer 56; the four perf slots were removed after"
                        + " measurement, so a change here means the header grew a slot");
    }

    /**
     * Each filled group, by size and by offset, against both the C compiler and the measured
     * constants above. {@code sizeof(WKJHost)} alone would catch a group that changed size, but it
     * would not say which one, and two groups that changed by equal and opposite amounts would slip
     * through it entirely.
     */
    @Test
    public void everyFilledGroupHasTheSizeAndOffsetTheCCompilerGaveIt() {
        MemoryLayout host = WebKitNativeShim.hostLayout();
        for (Group group : GROUPS) {
            MemoryLayout layout = host.select(PathElement.groupElement(group.member()));
            assertEquals(group.size(), layout.byteSize(),
                    "sizeof(" + group.struct() + ") changed, so that group gained or lost a slot");
            assertEquals(WkjStubShim.sizeOf(group.struct()), layout.byteSize(),
                    "sizeof(" + group.struct() + ") disagrees with the C compiler");
            assertEquals(group.offset(), host.byteOffset(PathElement.groupElement(group.member())),
                    "offsetof(WKJHost, " + group.member()
                            + ") changed, so every later group moved too");
        }
    }

    /**
     * Every callback slot of the whole table, by dotted path, at the offset the C compiler computed.
     * This is the check that makes the Java layout usable rather than merely the right total size:
     * {@code WebKitNative} resolves each stub's destination through exactly these paths.
     */
    @Test
    public void everyHostSlotIsWhereTheCCompilerPutIt() {
        int slots = WkjStubShim.hostSlotCount();
        assertEquals(HOST_SLOT_COUNT, slots,
                "the host table has a different number of callback slots, so a group in the C"
                        + " header has grown or shrunk and WKJLayouts has not followed it");
        for (int i = 0; i < slots; i++) {
            String name = WkjStubShim.hostSlotName(i);
            assertEquals(WkjStubShim.hostSlotOffset(i), WebKitNativeShim.hostSlotOffset(name),
                    "the offset of host slot " + name + " disagrees");
        }
    }

    /**
     * Every filled slot was bound with a {@code FunctionDescriptor} of the shape the C prototype
     * declares. This is the check the offsets do not make: a slot at the right offset whose
     * descriptor has the wrong shape installs a stub the library will call with a mismatched
     * calling convention, which corrupts the stack rather than failing, and no other test in this
     * module would see it.
     * <p>
     * The C side of the comparison is derived from the header by {@code gen-wkjstub.pl}, which reads
     * the same declaration the C++ compiler does, so this is a genuine second source rather than a
     * restatement of what the Java side already says.
     */
    @Test
    public void everyFilledHostSlotWasBoundWithTheShapeTheCPrototypeDeclares() {
        Map<String, String> bound = WebKitNativeShim.hostSlotSignatures();
        Map<String, String> fromC = new TreeMap<>();
        for (int i = 0, n = WkjStubShim.hostSlotCount(); i < n; i++) {
            fromC.put(WkjStubShim.hostSlotName(i), WkjStubShim.hostSlotSignature(i));
        }
        List<String> mismatched = new ArrayList<>();
        for (Map.Entry<String, String> slot : bound.entrySet()) {
            String expected = fromC.get(slot.getKey());
            assertNotNull(expected, "javafx.web filled " + slot.getKey()
                    + ", which the C header does not declare");
            if (!expected.equals(slot.getValue())) {
                mismatched.add(slot.getKey() + ": C says " + expected + ", Java bound "
                        + slot.getValue());
            }
        }
        assertTrue(mismatched.isEmpty(),
                "these callback descriptors disagree with the C prototype: " + mismatched);
        assertEquals(HOST_SLOT_COUNT - DELIBERATELY_NULL_SLOTS, bound.size(),
                "the number of filled slots changed; WebKitHostInstallTest names the ones that are"
                        + " deliberately left NULL and why");
    }
}
