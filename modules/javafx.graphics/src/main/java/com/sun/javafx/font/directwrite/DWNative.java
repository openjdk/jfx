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

package com.sun.javafx.font.directwrite;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.util.Logging;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * The DirectWrite calls behind {@code DWFactory}, {@code DWFontFile} and {@code DWGlyphLayout}, bound
 * directly from Java: what the {@code Java_com_sun_javafx_font_directwrite_OS_*} bodies of
 * {@code directwrite.cpp} wrap in JNI. It owns the {@link Linker}, the {@code dwrite.dll}
 * {@link SymbolLookup}, every downcall handle, the {@code GUID} layout and the COM vtable dispatch
 * helper, and it is the only class in this package that uses a restricted {@code java.lang.foreign}
 * method.
 *
 * <h2>How a COM method is called</h2>
 * {@code dwrite.dll} exports exactly one of the functions this class needs by name
 * ({@code DWriteCreateFactory}, dwrite.h:5123); everything else is reached through a COM vtable, which
 * <em>is</em> the ABI and is a plain C ABI - {@code this} in RCX, the one Win64 calling convention. A
 * COM interface pointer {@code p} points at a struct whose word 0 is the vtable pointer
 * (unknwnbase.h:222), and slot {@code n} of that vtable holds the function pointer of the {@code n}-th
 * method (unknwnbase.h:198-218 fixes {@code QueryInterface}, {@code AddRef} and {@code Release} as
 * slots 0, 1 and 2 of <em>every</em> interface; an interface's own methods follow its bases' in header
 * declaration order). {@link #slot} reads that function pointer; the call goes through a
 * {@link MethodHandle} produced by the address-less
 * {@link Linker#downcallHandle(FunctionDescriptor, Linker.Option...)} overload, whose handle takes the
 * target address as an extra leading parameter. The <em>signature</em> is therefore linked once and
 * cached while the <em>address</em> is read per call, which is what makes caching virtual dispatch
 * safe: two objects of one interface may come from different implementation classes with different
 * vtables.
 *
 * <h2>Slot numbers</h2>
 * Every slot constant below carries the {@code dwrite.h} line its method is declared on, and the
 * arithmetic is always {@code base + ordinal}: 3 for a direct subinterface of {@code IUnknown}, 6 for
 * {@code IDWriteFontFamily}, whose base {@code IDWriteFontList} adds three methods of its own
 * (dwrite.h:1512, :1548). {@code DWNativeTest} proves the table behaviourally, so a wrong slot fails a
 * test rather than corrupting memory silently.
 *
 * <h2>Reference counting</h2>
 * Every {@code _COM_Outptr_} parameter hands back a pointer that already carries one reference for the
 * caller, so the Java peer built around it ({@code new IDWriteFontFamily(ptr)} and friends) owns
 * exactly that one reference and must {@code Release()} it exactly once. This class never
 * {@code AddRef}s implicitly, never stores a {@link MemorySegment} derived from a COM pointer and
 * never lets an {@link Arena} own a COM object: closing an arena here can only free scratch memory.
 * {@link #addRef} and {@link #release} exist because {@code OS.AddRef} and {@code OS.Release} do, and
 * behave identically - including {@code AddRef} having no null guard, which is unreachable because
 * {@code IUnknown.Release()} zeroes the pointer it releases.
 *
 * <h2>Failure to load</h2>
 * {@code directwrite.cpp:906-916} calls {@code LoadLibrary("dwrite.dll")} then
 * {@code GetProcAddress("DWriteCreateFactory")} and returns {@code NULL} when either fails;
 * {@code DWFactory.getFactory()} depends on that null meaning "DirectWrite unavailable".
 * {@link SymbolLookup#libraryLookup} throws instead, so it is wrapped: a missing library or symbol
 * leaves {@link #dwriteCreateFactory} returning {@code 0}. Nothing in this class's initializer can
 * throw, deliberately - the first thread to touch it may be the "Prism Font Disposer" daemon
 * ({@code Disposer} by way of {@code DWDisposer}), whose loop catches {@code Exception} but not
 * {@code Error}, so an {@code ExceptionInInitializerError} raised there would kill the disposer thread
 * for the life of the process and leak every {@code DisposerRecord} after it.
 * <p>
 * Line numbers into {@code directwrite.cpp}, bare {@code :N} forms included, refer to it at commit
 * {@code 8492cb03b0} ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/directwrite.cpp});
 * line numbers into {@code dwrite.h}, {@code dwrite_2.h}, {@code d2d1.h}, {@code d2dbasetypes.h},
 * {@code dcommon.h}, {@code wincodec.h} and {@code unknwnbase.h} refer to the Windows SDK 10.0.26100.0
 * ({@code Include/10.0.26100.0/um}).
 */
final class DWNative {

    /** {@code winerror.h}; what {@code directwrite.cpp} leaves {@code hr} at when a call never happened. */
    static final int E_FAIL = 0x80004005;

    /** {@code IUnknown} (unknwnbase.h): the one IID COM guarantees pointer identity for. */
    static final String IID_IUNKNOWN = "00000000-0000-0000-c000-000000000046";

    /** {@code IDWriteFactory}: the uuid of the {@code DWRITE_DECLARE_INTERFACE} at dwrite.h:4704. */
    static final String IID_IDWRITE_FACTORY = "b859ee5a-d838-4b5b-a2e8-1adc7d93db48";

    /* IUnknown, unknwnbase.h:198-218: the first three slots of every COM interface, in this order. */
    private static final int IUNKNOWN_QUERY_INTERFACE = 0;             // unknwnbase.h:203
    private static final int IUNKNOWN_ADD_REF = 1;                     // unknwnbase.h:210
    private static final int IUNKNOWN_RELEASE = 2;                     // unknwnbase.h:214

    /* IDWriteFactory : IUnknown, dwrite.h:4704. base 3 + ordinal. */
    private static final int FACTORY_GET_SYSTEM_FONT_COLLECTION = 3;   // ord 0, dwrite.h:4717
    private static final int FACTORY_CREATE_FONT_FILE_REFERENCE = 7;   // ord 4, dwrite.h:4775
    private static final int FACTORY_CREATE_FONT_FACE = 9;             // ord 6, dwrite.h:4821

    /* IDWriteFontCollection : IUnknown, dwrite.h:1459. base 3 + ordinal. */
    private static final int COLLECTION_GET_FONT_FAMILY_COUNT = 3;     // ord 0, dwrite.h:1464
    private static final int COLLECTION_GET_FONT_FAMILY = 4;           // ord 1, dwrite.h:1474
    private static final int COLLECTION_FIND_FAMILY_NAME = 5;          // ord 2, dwrite.h:1488
    private static final int COLLECTION_GET_FONT_FROM_FONT_FACE = 6;   // ord 3, dwrite.h:1503

    /* IDWriteFontList : IUnknown, dwrite.h:1512. base 3 + ordinal; reached through a family pointer. */
    private static final int FONT_LIST_GET_FONT_COUNT = 4;             // ord 1, dwrite.h:1528
    private static final int FONT_LIST_GET_FONT = 5;                   // ord 2, dwrite.h:1538

    /* IDWriteFontFamily : IDWriteFontList, dwrite.h:1548. base 6 = 3 IUnknown + 3 IDWriteFontList. */
    private static final int FONT_FAMILY_GET_FAMILY_NAMES = 6;         // ord 0, dwrite.h:1557
    private static final int FONT_FAMILY_GET_FIRST_MATCHING_FONT = 7;  // ord 1, dwrite.h:1571

    /* IDWriteFont : IUnknown, dwrite.h:1599. base 3 + ordinal. */
    private static final int FONT_GET_FONT_FAMILY = 3;                 // ord 0, dwrite.h:1608
    private static final int FONT_GET_WEIGHT = 4;                      // ord 1, dwrite.h:1615
    private static final int FONT_GET_STRETCH = 5;                     // ord 2, dwrite.h:1620
    private static final int FONT_GET_STYLE = 6;                       // ord 3, dwrite.h:1625
    private static final int FONT_GET_FACE_NAMES = 8;                  // ord 5, dwrite.h:1639
    private static final int FONT_GET_INFORMATIONAL_STRINGS = 9;       // ord 6, dwrite.h:1653
    private static final int FONT_GET_SIMULATIONS = 10;                // ord 7, dwrite.h:1662
    private static final int FONT_CREATE_FONT_FACE = 13;               // ord 10, dwrite.h:1692

    /* IDWriteLocalizedStrings : IUnknown, dwrite.h:1371. base 3 + ordinal. */
    private static final int STRINGS_FIND_LOCALE_NAME = 4;             // ord 1, dwrite.h:1388
    private static final int STRINGS_GET_STRING_LENGTH = 7;            // ord 4, dwrite.h:1431
    private static final int STRINGS_GET_STRING = 8;                   // ord 5, dwrite.h:1446

    /* IDWriteFontFile : IUnknown, dwrite.h:822. base 3 + ordinal. */
    private static final int FONT_FILE_ANALYZE = 5;                    // ord 2, dwrite.h:868

    /* IDWriteFontFace : IUnknown, dwrite.h:1047. base 3 + ordinal. */
    private static final int FONT_FACE_GET_TYPE = 3;                   // ord 0, dwrite.h:1052
    private static final int FONT_FACE_GET_INDEX = 5;                  // ord 2, dwrite.h:1074
    private static final int FONT_FACE_GET_SIMULATIONS = 6;            // ord 3, dwrite.h:1079
    private static final int FONT_FACE_IS_SYMBOL_FONT = 7;             // ord 4, dwrite.h:1084
    private static final int FONT_FACE_GET_METRICS = 8;                // ord 5, dwrite.h:1092
    private static final int FONT_FACE_GET_GLYPH_COUNT = 9;            // ord 6, dwrite.h:1099
    private static final int FONT_FACE_GET_DESIGN_GLYPH_METRICS = 10;  // ord 7, dwrite.h:1115
    private static final int FONT_FACE_GET_GLYPH_INDICES = 11;         // ord 8, dwrite.h:1136
    private static final int FONT_FACE_TRY_GET_FONT_TABLE = 12;        // ord 9, dwrite.h:1176
    private static final int FONT_FACE_RELEASE_FONT_TABLE = 13;        // ord 10, dwrite.h:1188
    private static final int FONT_FACE_GET_GLYPH_RUN_OUTLINE = 14;     // ord 11, dwrite.h:1209

    /* IDWriteGlyphRunAnalysis : IUnknown, dwrite.h:4646. base 3 + ordinal. */
    private static final int ANALYSIS_GET_ALPHA_TEXTURE_BOUNDS = 3;    // ord 0, dwrite.h:4659
    private static final int ANALYSIS_CREATE_ALPHA_TEXTURE = 4;        // ord 1, dwrite.h:4677

    /* IDWriteFactory, dwrite.h:4704, continued. */
    private static final int FACTORY_CREATE_GLYPH_RUN_ANALYSIS = 23;   // ord 20, dwrite.h:5090

    /*
     * ID2D1SimplifiedGeometrySink : IUnknown, d2d1.h:2174 - the one interface this package
     * implements rather than calls. dwrite.h:1041 typedefs IDWriteGeometrySink to it, so one vtable
     * answers both names. These are the slots of the table DWNative builds, not of one it reads.
     */
    private static final int SINK_SET_FILL_MODE = 3;                   // ord 0, d2d1.h:2177
    private static final int SINK_SET_SEGMENT_FLAGS = 4;               // ord 1, d2d1.h:2181
    private static final int SINK_BEGIN_FIGURE = 5;                    // ord 2, d2d1.h:2185
    private static final int SINK_ADD_LINES = 6;                       // ord 3, d2d1.h:2190
    private static final int SINK_ADD_BEZIERS = 7;                     // ord 4, d2d1.h:2195
    private static final int SINK_END_FIGURE = 8;                      // ord 5, d2d1.h:2200
    private static final int SINK_CLOSE = 9;                           // ord 6, d2d1.h:2204
    private static final int SINK_SLOT_COUNT = 10;

    /** {@code GUID} (guiddef.h): 16 bytes, 4-byte aligned. */
    static final StructLayout GUID_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("Data1"),                                    // offset 0
            JAVA_SHORT.withName("Data2"),                                  // offset 4
            JAVA_SHORT.withName("Data3"),                                  // offset 6
            MemoryLayout.sequenceLayout(8, JAVA_BYTE).withName("Data4"));  // offset 8, byteSize 16

    /**
     * {@code DWRITE_GLYPH_METRICS} (dwrite.h:551): seven 32-bit fields, 28 bytes, 4-byte aligned.
     * {@code advanceWidth} and {@code advanceHeight} are {@code UINT32}, the other five {@code INT32};
     * the Java mirror {@code DWRITE_GLYPH_METRICS} holds all seven as {@code int}, which is what the
     * JNI did. Crosses as an out-pointer, never by value.
     */
    static final StructLayout DWRITE_GLYPH_METRICS_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("leftSideBearing"),     // offset 0   INT32
            JAVA_INT.withName("advanceWidth"),        // offset 4   UINT32
            JAVA_INT.withName("rightSideBearing"),    // offset 8   INT32
            JAVA_INT.withName("topSideBearing"),      // offset 12  INT32
            JAVA_INT.withName("advanceHeight"),       // offset 16  UINT32
            JAVA_INT.withName("bottomSideBearing"),   // offset 20  INT32
            JAVA_INT.withName("verticalOriginY"));    // offset 24  INT32, byteSize 28

    /**
     * {@code DWRITE_FONT_METRICS} (dwrite.h:476): ten 16-bit fields, 20 bytes, 2-byte aligned.
     * {@code lineGap}, {@code underlinePosition} and {@code strikethroughPosition} are {@code INT16},
     * the other seven {@code UINT16}. {@code IDWriteFontFace::GetMetrics} (dwrite.h:1092) is declared
     * {@code STDMETHOD_(void, GetMetrics)(DWRITE_FONT_METRICS*)}, so this struct is written through an
     * out-pointer and is <em>not</em> returned by value - the one method in this package whose name
     * invites that assumption.
     */
    static final StructLayout DWRITE_FONT_METRICS_LAYOUT = MemoryLayout.structLayout(
            JAVA_SHORT.withName("designUnitsPerEm"),        // offset 0   UINT16
            JAVA_SHORT.withName("ascent"),                  // offset 2   UINT16
            JAVA_SHORT.withName("descent"),                 // offset 4   UINT16
            JAVA_SHORT.withName("lineGap"),                 // offset 6   INT16
            JAVA_SHORT.withName("capHeight"),               // offset 8   UINT16
            JAVA_SHORT.withName("xHeight"),                 // offset 10  UINT16
            JAVA_SHORT.withName("underlinePosition"),       // offset 12  INT16
            JAVA_SHORT.withName("underlineThickness"),      // offset 14  UINT16
            JAVA_SHORT.withName("strikethroughPosition"),   // offset 16  INT16
            JAVA_SHORT.withName("strikethroughThickness")); // offset 18  UINT16, byteSize 20

    /** {@code DWRITE_GLYPH_OFFSET} (dwrite.h:604): two floats, 8 bytes, 4-byte aligned. */
    static final StructLayout DWRITE_GLYPH_OFFSET_LAYOUT = MemoryLayout.structLayout(
            JAVA_FLOAT.withName("advanceOffset"),     // offset 0
            JAVA_FLOAT.withName("ascenderOffset"));   // offset 4, byteSize 8

    /**
     * {@code DWRITE_GLYPH_RUN} (dwrite.h:3038): 48 bytes, 8-byte aligned. No explicit padding is
     * needed - {@code fontEmSize} and {@code glyphCount} fill 8..15 exactly, and {@code isSideways}
     * and {@code bidiLevel} fill 40..47 exactly. Crosses as a {@code const*}, never by value.
     */
    static final StructLayout DWRITE_GLYPH_RUN_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("fontFace"),             // offset 0   IDWriteFontFace*
            JAVA_FLOAT.withName("fontEmSize"),        // offset 8   FLOAT
            JAVA_INT.withName("glyphCount"),          // offset 12  UINT32
            ADDRESS.withName("glyphIndices"),         // offset 16  UINT16 const*
            ADDRESS.withName("glyphAdvances"),        // offset 24  FLOAT const*
            ADDRESS.withName("glyphOffsets"),         // offset 32  DWRITE_GLYPH_OFFSET const*
            JAVA_INT.withName("isSideways"),          // offset 40  BOOL
            JAVA_INT.withName("bidiLevel"));          // offset 44  UINT32, byteSize 48

    /** {@code DWRITE_MATRIX} (dwrite.h:971): six floats, 24 bytes, 4-byte aligned. By pointer only. */
    static final StructLayout DWRITE_MATRIX_LAYOUT = MemoryLayout.structLayout(
            JAVA_FLOAT.withName("m11"),               // offset 0
            JAVA_FLOAT.withName("m12"),               // offset 4
            JAVA_FLOAT.withName("m21"),               // offset 8
            JAVA_FLOAT.withName("m22"),               // offset 12
            JAVA_FLOAT.withName("dx"),                // offset 16
            JAVA_FLOAT.withName("dy"));               // offset 20, byteSize 24

    /** {@code RECT} (windef.h): four {@code LONG}, 16 bytes, 4-byte aligned. By pointer only. */
    static final StructLayout RECT_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("left"),                // offset 0
            JAVA_INT.withName("top"),                 // offset 4
            JAVA_INT.withName("right"),               // offset 8
            JAVA_INT.withName("bottom"));             // offset 12, byteSize 16

    /**
     * {@code D2D1_POINT_2F} (d2d1.h:283, aliasing {@code D2D_POINT_2F}, dcommon.h:183): two floats,
     * 8 bytes, 4-byte aligned. <b>This is the only aggregate in the package that crosses by value</b>
     * ({@code ID2D1SimplifiedGeometrySink::BeginFigure}, d2d1.h:2185). On Microsoft x64 an aggregate
     * of 1, 2, 4 or 8 bytes is passed as if it were an integer of that size, in the integer register
     * that argument occupies - never split across two registers, and never in an XMM register even
     * though both members are floats. Putting this layout in the {@link FunctionDescriptor} is how
     * that is expressed; flattening it into two {@code JAVA_FLOAT} parameters would put x in XMM1 and
     * y in XMM2 and read garbage without faulting.
     */
    static final StructLayout D2D1_POINT_2F_LAYOUT = MemoryLayout.structLayout(
            JAVA_FLOAT.withName("x"),                 // offset 0
            JAVA_FLOAT.withName("y"));                // offset 4, byteSize 8

    /**
     * {@code D2D1_BEZIER_SEGMENT} (d2d1.h:563): three points, 24 bytes, 4-byte aligned. Larger than
     * 8 bytes, so it could never be passed in a register - and it never is: {@code AddBeziers} takes
     * a pointer to an array of them.
     */
    static final StructLayout D2D1_BEZIER_SEGMENT_LAYOUT = MemoryLayout.structLayout(
            D2D1_POINT_2F_LAYOUT.withName("point1"),  // offset 0
            D2D1_POINT_2F_LAYOUT.withName("point2"),  // offset 8
            D2D1_POINT_2F_LAYOUT.withName("point3")); // offset 16, byteSize 24

    /** The object DirectWrite is handed: {@code { const void* vtbl; uint64_t sessionId; }}, 16 bytes. */
    private static final StructLayout SINK_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("vtbl"),                 // offset 0
            JAVA_LONG.withName("user"));              // offset 8, byteSize 16

    /* DWRITE_GLYPH_RUN is the one struct here that is not a run of equally sized fields. */
    private static final long RUN_FONT_FACE = offsetOf(DWRITE_GLYPH_RUN_LAYOUT, "fontFace");
    private static final long RUN_FONT_EM_SIZE = offsetOf(DWRITE_GLYPH_RUN_LAYOUT, "fontEmSize");
    private static final long RUN_GLYPH_COUNT = offsetOf(DWRITE_GLYPH_RUN_LAYOUT, "glyphCount");
    private static final long RUN_GLYPH_INDICES = offsetOf(DWRITE_GLYPH_RUN_LAYOUT, "glyphIndices");
    private static final long RUN_GLYPH_ADVANCES = offsetOf(DWRITE_GLYPH_RUN_LAYOUT, "glyphAdvances");
    private static final long RUN_GLYPH_OFFSETS = offsetOf(DWRITE_GLYPH_RUN_LAYOUT, "glyphOffsets");
    private static final long RUN_IS_SIDEWAYS = offsetOf(DWRITE_GLYPH_RUN_LAYOUT, "isSideways");
    private static final long RUN_BIDI_LEVEL = offsetOf(DWRITE_GLYPH_RUN_LAYOUT, "bidiLevel");

    private static final Linker LINKER = Linker.nativeLinker();

    /**
     * The symbols bound by name. Copy-on-write because the {@code ole32.dll} and {@code d2d1.dll}
     * holder classes below add to it lazily, on whichever thread first needs COM or Direct2D, while
     * {@link #boundSymbols} may be read from another.
     */
    private static final List<String> BOUND_SYMBOLS = new CopyOnWriteArrayList<>();

    /**
     * One handle per {@link FunctionDescriptor}, never per slot: linkage depends on the signature
     * alone, and the slot only chooses which address that handle is invoked on. The map is consulted
     * when a shape is first bound - class initialization here, class initialization of a lazy holder
     * that reuses a shape - and never on a call path, where every site holds a {@code static final}
     * handle.
     */
    private static final Map<FunctionDescriptor, MethodHandle> HANDLES = new ConcurrentHashMap<>();

    /* The vtable shapes, "this" included and the target address implied by the address-less overload. */

    /** {@code ULONG f(this)}: AddRef, Release, GetWeight, GetStretch, GetStyle, GetSimulations, the counts. */
    private static final MethodHandle U32_THIS = virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code HRESULT f(this, void** out)}. */
    private static final MethodHandle HR_OUT = virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code HRESULT f(this, void** out, BOOL)}: GetSystemFontCollection - the BOOL is 32-bit. */
    private static final MethodHandle HR_OUT_BOOL =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

    /** {@code HRESULT f(this, UINT32, void** out)}: GetFontFamily(index), GetFont, GetStringLength. */
    private static final MethodHandle HR_I_OUT =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));

    /** {@code HRESULT f(this, void*, void** out)}: QueryInterface, GetFontFromFontFace. */
    private static final MethodHandle HR_P_OUT =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /**
     * {@code HRESULT f(this, void*, void*, void*)} - three pointers after {@code this}:
     * FindFamilyName, FindLocaleName, CreateFontFileReference. Kept apart from {@link #HR_4A} because
     * {@code IDWriteFontFile::Analyze} takes a fourth out-pointer that lands on the stack at
     * {@code [RSP+0x20]}; one shared five-pointer shape would leave that slot unwritten.
     */
    private static final MethodHandle HR_3A =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code HRESULT f(this, void*, void*, void*, void*)}: {@code IDWriteFontFile::Analyze} only. */
    private static final MethodHandle HR_4A =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code HRESULT f(this, UINT32, void** out, BOOL* exists)}: GetInformationalStrings. */
    private static final MethodHandle HR_I_OUT_BOOL =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));

    /** {@code HRESULT f(this, UINT32 index, WCHAR* buffer, UINT32 size)}: GetString. */
    private static final MethodHandle HR_I_BUF_I =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));

    /** {@code HRESULT f(this, weight, stretch, style, void** out)}: GetFirstMatchingFont. */
    private static final MethodHandle HR_III_OUT =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));

    /**
     * {@code HRESULT f(this, DWRITE_FONT_FACE_TYPE, UINT32 numberOfFiles, IDWriteFontFile* const*,
     * UINT32 faceIndex, DWRITE_FONT_SIMULATIONS, IDWriteFontFace** out)}: IDWriteFactory::CreateFontFace.
     */
    private static final MethodHandle HR_FONT_FACE = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));

    /**
     * {@code HRESULT f(this, UINT16 const* glyphIndices, UINT32 glyphCount,
     * DWRITE_GLYPH_METRICS* out, BOOL isSideways)}: {@code IDWriteFontFace::GetDesignGlyphMetrics}.
     * The metrics come back through the fourth parameter, not as a return value.
     */
    private static final MethodHandle HR_DESIGN_GLYPH_METRICS = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));

    /**
     * {@code HRESULT f(this, UINT32 const* codePoints, UINT32 count, UINT16* out)}:
     * {@code IDWriteFontFace::GetGlyphIndices}. Distinct from {@link #HR_I_OUT_BOOL}, whose scalar
     * comes first.
     */
    private static final MethodHandle HR_P_I_OUT = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));

    /**
     * {@code void f(this, void*)}: {@code IDWriteFontFace::GetMetrics} (an out-pointer, dwrite.h:1092)
     * and {@code IDWriteFontFace::ReleaseFontTable} (an opaque context, dwrite.h:1188). Both return
     * {@code void}, so neither has an error channel.
     */
    private static final MethodHandle VOID_THIS_P = virtual(FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /**
     * {@code UINT16 f(this)}: {@code IDWriteFontFace::GetGlyphCount} (dwrite.h:1099). Sixteen bits
     * wide, returned in AX with the rest of RAX undefined, so the descriptor must say
     * {@code JAVA_SHORT} and the caller must zero-extend.
     */
    private static final MethodHandle U16_THIS = virtual(FunctionDescriptor.of(JAVA_SHORT, ADDRESS));

    /**
     * {@code HRESULT f(this, UINT32 tag, const void** tableData, UINT32* tableSize, void** context,
     * BOOL* exists)}: {@code IDWriteFontFace::TryGetFontTable} (dwrite.h:1176), five out-parameters
     * after the tag - the last two land on the stack at {@code [RSP+0x20]} and {@code [RSP+0x28]}.
     */
    private static final MethodHandle HR_FONT_TABLE = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /**
     * {@code HRESULT f(this, FLOAT emSize, UINT16 const* glyphIndices, FLOAT const* glyphAdvances,
     * DWRITE_GLYPH_OFFSET const* glyphOffsets, UINT32 glyphCount, BOOL isSideways, BOOL isRightToLeft,
     * IDWriteGeometrySink* sink)}: {@code IDWriteFontFace::GetGlyphRunOutline} (dwrite.h:1209).
     * The only call in this package that upcalls, which is why {@code critical(true)} is impossible
     * for it rather than merely unwise.
     */
    private static final MethodHandle HR_GLYPH_RUN_OUTLINE = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, JAVA_FLOAT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
            ADDRESS));

    /**
     * {@code HRESULT f(this, DWRITE_GLYPH_RUN const*, FLOAT pixelsPerDip, DWRITE_MATRIX const*,
     * DWRITE_RENDERING_MODE, DWRITE_MEASURING_MODE, FLOAT baselineOriginX, FLOAT baselineOriginY,
     * IDWriteGlyphRunAnalysis** out)}: {@code IDWriteFactory::CreateGlyphRunAnalysis} (dwrite.h:5090).
     * Both structs cross as pointers; neither is passed by value.
     */
    private static final MethodHandle HR_GLYPH_RUN_ANALYSIS = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, JAVA_FLOAT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_FLOAT,
            JAVA_FLOAT, ADDRESS));

    /**
     * {@code HRESULT f(this, DWRITE_TEXTURE_TYPE, RECT const*, BYTE* buffer, UINT32 bufferSize)}:
     * {@code IDWriteGlyphRunAnalysis::CreateAlphaTexture} (dwrite.h:4677).
     * {@code GetAlphaTextureBounds} (dwrite.h:4659) has the shape of {@link #HR_I_OUT} and reuses it.
     */
    private static final MethodHandle HR_ALPHA_TEXTURE = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

    /* The nine shapes of the geometry sink, used both to build the upcall stubs and, in the
     * self-test, to call the resulting vtable the way DirectWrite will. */

    /** {@code HRESULT QueryInterface(this, REFIID, void** out)}, sink slot 0. */
    private static final FunctionDescriptor SINK_QUERY_INTERFACE_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS);

    /** {@code ULONG AddRef(this)} / {@code ULONG Release(this)} / {@code HRESULT Close(this)}. */
    private static final FunctionDescriptor SINK_THIS_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS);

    /** {@code void SetFillMode(this, int)} / {@code void SetSegmentFlags(this, int)} / EndFigure. */
    private static final FunctionDescriptor SINK_THIS_INT_FD =
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT);

    /**
     * {@code void BeginFigure(this, D2D1_POINT_2F startPoint, D2D1_FIGURE_BEGIN figureBegin)}, sink
     * slot 5: the one by-value aggregate in the package. See {@link #D2D1_POINT_2F_LAYOUT}.
     */
    private static final FunctionDescriptor SINK_BEGIN_FIGURE_FD =
            FunctionDescriptor.ofVoid(ADDRESS, D2D1_POINT_2F_LAYOUT, JAVA_INT);

    /** {@code void AddLines(this, const D2D1_POINT_2F*, UINT32)} and {@code AddBeziers}, by pointer. */
    private static final FunctionDescriptor SINK_ARRAY_FD =
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT);

    /** {@code dwrite.dll}, or {@code null} where it cannot be loaded - see the class comment. */
    private static final SymbolLookup DWRITE = tryLoad("dwrite.dll");

    /**
     * {@code HRESULT DWriteCreateFactory(DWRITE_FACTORY_TYPE, REFIID, IUnknown**)} (dwrite.h:5123), or
     * {@code null} when {@code dwrite.dll} is absent or does not export it.
     */
    private static final MethodHandle DWRITE_CREATE_FACTORY = bindOptional(DWRITE, "dwrite.dll",
            "DWriteCreateFactory", FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));

    /** The one 16-byte constant this class keeps for the life of the process. */
    private static final MemorySegment IID_IDWRITE_FACTORY_BYTES = guid(Arena.global(), IID_IDWRITE_FACTORY);

    private DWNative() {
    }

    /* ---------------------------------------------------------------------------------------------
     * Linkage
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code LoadLibrary(fileName)}, whose failure the C treated as "DirectWrite unavailable"
     * ({@code directwrite.cpp:906-910}). Never throws: {@link SymbolLookup#libraryLookup} raises
     * {@link IllegalArgumentException} for a library that will not load, and this class must be
     * initializable everywhere.
     */
    @SuppressWarnings("restricted")
    private static SymbolLookup tryLoad(String fileName) {
        try {
            return SymbolLookup.libraryLookup(fileName, Arena.global());
        } catch (IllegalArgumentException | UnsatisfiedLinkError e) {
            return null;
        }
    }

    /** {@code GetProcAddress}: {@code null} rather than an error when the library or the symbol is absent. */
    @SuppressWarnings("restricted")
    private static MethodHandle bindOptional(SymbolLookup library, String libraryName, String name,
                                             FunctionDescriptor descriptor) {
        if (library == null) {
            return null;
        }
        MemorySegment symbol = library.find(name).orElse(null);
        if (symbol == null) {
            return null;
        }
        BOUND_SYMBOLS.add(libraryName + "!" + name);
        return LINKER.downcallHandle(symbol, descriptor);
    }

    /**
     * A handle for one vtable shape. The address-less overload gives a handle whose leading
     * {@link MemorySegment} parameter is the function to call, so one handle serves every object of
     * every interface that has this signature. None of these calls is
     * {@link Linker.Option#critical critical}: DirectWrite allocates, takes locks and can reach the
     * font-cache service.
     */
    @SuppressWarnings("restricted")
    private static MethodHandle virtual(FunctionDescriptor descriptor) {
        return HANDLES.computeIfAbsent(descriptor, shape -> LINKER.downcallHandle(shape));
    }

    /**
     * Slot {@code index} of the vtable {@code self} carries in its first word (unknwnbase.h:222). Two
     * reinterprets and two reads; no linkage, nothing retained.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment slot(MemorySegment self, int index) {
        MemorySegment vtable = self.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
        return vtable.reinterpret((index + 1L) * ADDRESS.byteSize()).getAtIndex(ADDRESS, index);
    }

    /** The COM object at {@code ptr}, as the zero-length segment {@link #slot} bounds per call. */
    private static MemorySegment com(long ptr) {
        return MemorySegment.ofAddress(ptr);
    }

    /**
     * The byte offset of a named field, so the struct layouts above are what the code actually uses
     * rather than a set of literals repeated beside them.
     */
    private static long offsetOf(StructLayout layout, String field) {
        return layout.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    /**
     * A pointer that arrived from native code, given the size it is documented to have. Segments from
     * outside have length zero, and this is the only place in the package allowed to widen one.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment bounded(MemorySegment segment, long byteSize) {
        return segment.reinterpret(byteSize);
    }

    private static Error unexpected(Throwable t) {
        if (t instanceof Error e) {
            return e;
        }
        if (t instanceof RuntimeException e) {
            throw e;
        }
        // Only linkage failures can land here: COM reports failure in the HRESULT, it cannot throw.
        return new AssertionError(t);
    }

    /**
     * {@code guiddef.h} byte order for a {@code REFIID}: {@code Data1}, {@code Data2} and {@code Data3}
     * are little-endian integers, {@code Data4} is eight bytes in writing order. So
     * {@code b859ee5a-d838-4b5b-a2e8-1adc7d93db48} is
     * {@code 5a ee 59 b8 38 d8 5b 4b a2 e8 1a dc 7d 93 db 48}.
     */
    private static MemorySegment guid(Arena arena, String uuid) {
        if (uuid.length() != 36 || uuid.charAt(8) != '-' || uuid.charAt(13) != '-'
                || uuid.charAt(18) != '-' || uuid.charAt(23) != '-') {
            throw new IllegalArgumentException("not a uuid: " + uuid);
        }
        String hex = uuid.substring(0, 8) + uuid.substring(9, 13) + uuid.substring(14, 18)
                + uuid.substring(19, 23) + uuid.substring(24);
        byte[] raw = new byte[16];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        MemorySegment id = arena.allocate(GUID_LAYOUT);
        id.set(JAVA_INT, 0, ((raw[0] & 0xFF) << 24) | ((raw[1] & 0xFF) << 16)
                | ((raw[2] & 0xFF) << 8) | (raw[3] & 0xFF));
        id.set(JAVA_SHORT, 4, (short) (((raw[4] & 0xFF) << 8) | (raw[5] & 0xFF)));
        id.set(JAVA_SHORT, 6, (short) (((raw[6] & 0xFF) << 8) | (raw[7] & 0xFF)));
        MemorySegment.copy(raw, 8, id, JAVA_BYTE, 8, 8);
        return id;
    }

    /**
     * The {@code WCHAR const*} form of {@code chars}, which {@code GetCharArrayElements} handed over
     * verbatim: no terminator is added here, because the peers append one to the string they pass
     * ({@code (name + '\0').toCharArray()}). A {@code null} array becomes {@code NULL}, which is what
     * the C's {@code if (arg)} guards left the pointer at.
     */
    private static MemorySegment wide(Arena arena, char[] chars) {
        return chars == null ? MemorySegment.NULL : arena.allocateFrom(JAVA_CHAR, chars);
    }

    /* ---------------------------------------------------------------------------------------------
     * Test and diagnostic support
     * ------------------------------------------------------------------------------------------- */

    /** Runs the class initializer: loads {@code dwrite.dll} and links every vtable shape. */
    static void ensureLoaded() {
    }

    /** Whether {@code dwrite.dll} loaded and exports {@code DWriteCreateFactory}. */
    static boolean isAvailable() {
        return DWRITE_CREATE_FACTORY != null;
    }

    /** The symbols bound by name, as {@code <dll>!<name>}; the vtable methods have no names to list. */
    static List<String> boundSymbols() {
        return Collections.unmodifiableList(BOUND_SYMBOLS);
    }

    /** Whether {@code fileName} can be loaded at all - the {@link #tryLoad} catch, exercised. */
    static boolean canLoad(String fileName) {
        return tryLoad(fileName) != null;
    }

    /** The 16 bytes {@code uuid} marshals to, for the {@code REFIID} byte-order test. */
    static byte[] guidBytes(String uuid) {
        try (Arena scratch = Arena.ofConfined()) {
            return guid(scratch, uuid).toArray(JAVA_BYTE);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IUnknown - unknwnbase.h:198-218
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code IUnknown::QueryInterface}, slot 0. Not one of the JNI natives: it is here because slot 0
     * is the cheapest self-check that word 0 of a COM object really is its vtable, and because code
     * that needs a derived interface asks for it this way.
     *
     * @return the interface pointer, already carrying a reference for the caller, or {@code 0}
     */
    static long queryInterface(long self, String iid) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment id = guid(scratch, iid);
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_P_OUT.invokeExact(slot(obj, IUNKNOWN_QUERY_INTERFACE), obj, id, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code IUnknown::AddRef}, slot 1 ({@code directwrite.cpp:920-924}); no null guard, as there. */
    static int addRef(long self) {
        MemorySegment obj = com(self);
        try {
            return (int) U32_THIS.invokeExact(slot(obj, IUNKNOWN_ADD_REF), obj);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IUnknown::Release}, slot 2 ({@code directwrite.cpp:926-930}): the new reference count.
     * The null guard and the zeroing of the peer's pointer stay in {@code IUnknown.Release()}, which is
     * what makes a double dispose safe.
     */
    static int release(long self) {
        MemorySegment obj = com(self);
        try {
            return (int) U32_THIS.invokeExact(slot(obj, IUNKNOWN_RELEASE), obj);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFactory - dwrite.h:4704
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code DWriteCreateFactory(factoryType, __uuidof(IDWriteFactory), &factory)}
     * ({@code directwrite.cpp:901-917}): the factory, or {@code 0} when DirectWrite is unavailable or
     * the call failed. The {@code dwrite.dll} handle is never freed, exactly as the C never
     * {@code FreeLibrary}s it.
     */
    static long dwriteCreateFactory(int factoryType) {
        if (DWRITE_CREATE_FACTORY == null) {
            return 0L;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            int hr = (int) DWRITE_CREATE_FACTORY.invokeExact(factoryType, IID_IDWRITE_FACTORY_BYTES, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteFactory::GetSystemFontCollection}, slot 3 (dwrite.h:4717). {@code checkForUpdates}
     * crosses as a 32-bit {@code BOOL}, where the JNI argument was an 8-bit {@code jboolean}.
     */
    static long getSystemFontCollection(long self, boolean checkForUpdates) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_OUT_BOOL.invokeExact(slot(obj, FACTORY_GET_SYSTEM_FONT_COLLECTION), obj, out,
                    checkForUpdates ? 1 : 0);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteFactory::CreateFontFileReference}, slot 7 (dwrite.h:4775). {@code lastWriteTime} is
     * {@code NULL}, as at {@code directwrite.cpp:1870}.
     */
    static long createFontFileReference(long self, char[] filePath) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment path = wide(scratch, filePath);
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_3A.invokeExact(slot(obj, FACTORY_CREATE_FONT_FILE_REFERENCE), obj, path,
                    MemorySegment.NULL, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteFactory::CreateFontFace}, slot 9 (dwrite.h:4821), with the one-element
     * {@code IDWriteFontFile* const*} the C built on the stack ({@code directwrite.cpp:1880-1881}).
     */
    static long createFontFace(long self, int fontFaceType, long fontFile, int faceIndex,
                               int fontFaceSimulationFlags) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment files = scratch.allocate(ADDRESS);
            files.set(ADDRESS, 0, com(fontFile));
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_FONT_FACE.invokeExact(slot(obj, FACTORY_CREATE_FONT_FACE), obj, fontFaceType,
                    1, files, faceIndex, fontFaceSimulationFlags, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFontCollection - dwrite.h:1459
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteFontCollection::GetFontFamilyCount}, slot 3 (dwrite.h:1464); no error channel. */
    static int getFontFamilyCount(long self) {
        MemorySegment obj = com(self);
        try {
            return (int) U32_THIS.invokeExact(slot(obj, COLLECTION_GET_FONT_FAMILY_COUNT), obj);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code IDWriteFontCollection::GetFontFamily}, slot 4 (dwrite.h:1474). */
    static long getFontFamily(long self, int index) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_I_OUT.invokeExact(slot(obj, COLLECTION_GET_FONT_FAMILY), obj, index, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteFontCollection::FindFamilyName}, slot 5 (dwrite.h:1488): the family index, or
     * {@code -1} when the call failed or {@code exists} came back FALSE ({@code directwrite.cpp:2159}).
     * {@code DWFontFile} branches on that {@code -1}, so a {@code 0} here would silently pick family 0.
     */
    static int findFamilyName(long self, char[] familyName) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment name = wide(scratch, familyName);
            MemorySegment index = scratch.allocate(JAVA_INT);
            MemorySegment exists = scratch.allocate(JAVA_INT);
            MemorySegment obj = com(self);
            int hr = (int) HR_3A.invokeExact(slot(obj, COLLECTION_FIND_FAMILY_NAME), obj, name, index, exists);
            return hr >= 0 && exists.get(JAVA_INT, 0) != 0 ? index.get(JAVA_INT, 0) : -1;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code IDWriteFontCollection::GetFontFromFontFace}, slot 6 (dwrite.h:1503). */
    static long getFontFromFontFace(long self, long fontFace) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_P_OUT.invokeExact(slot(obj, COLLECTION_GET_FONT_FROM_FONT_FACE), obj,
                    com(fontFace), out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFontList - dwrite.h:1512, reached through an IDWriteFontFamily pointer
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteFontList::GetFontCount}, slot 4 (dwrite.h:1528); no error channel. */
    static int getFontCount(long self) {
        MemorySegment obj = com(self);
        try {
            return (int) U32_THIS.invokeExact(slot(obj, FONT_LIST_GET_FONT_COUNT), obj);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code IDWriteFontList::GetFont}, slot 5 (dwrite.h:1538). */
    static long getFont(long self, int index) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_I_OUT.invokeExact(slot(obj, FONT_LIST_GET_FONT), obj, index, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFontFamily - dwrite.h:1548 (base IDWriteFontList, so its own methods start at slot 6)
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteFontFamily::GetFamilyNames}, slot 6 (dwrite.h:1557). */
    static long getFamilyNames(long self) {
        return outPointer(self, FONT_FAMILY_GET_FAMILY_NAMES);
    }

    /** {@code IDWriteFontFamily::GetFirstMatchingFont}, slot 7 (dwrite.h:1571). */
    static long getFirstMatchingFont(long self, int weight, int stretch, int style) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_III_OUT.invokeExact(slot(obj, FONT_FAMILY_GET_FIRST_MATCHING_FONT), obj,
                    weight, stretch, style, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFont - dwrite.h:1599
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteFont::CreateFontFace}, slot 13 (dwrite.h:1692). */
    static long createFontFace(long self) {
        return outPointer(self, FONT_CREATE_FONT_FACE);
    }

    /** {@code IDWriteFont::GetFaceNames}, slot 8 (dwrite.h:1639). */
    static long getFaceNames(long self) {
        return outPointer(self, FONT_GET_FACE_NAMES);
    }

    /** {@code IDWriteFont::GetFontFamily}, slot 3 (dwrite.h:1608). */
    static long getFontFamily(long self) {
        return outPointer(self, FONT_GET_FONT_FAMILY);
    }

    /** {@code IDWriteFont::GetStretch}, slot 5 (dwrite.h:1620); a {@code DWRITE_FONT_STRETCH}, no HRESULT. */
    static int getStretch(long self) {
        return u32(self, FONT_GET_STRETCH);
    }

    /** {@code IDWriteFont::GetStyle}, slot 6 (dwrite.h:1625); a {@code DWRITE_FONT_STYLE}, no HRESULT. */
    static int getStyle(long self) {
        return u32(self, FONT_GET_STYLE);
    }

    /** {@code IDWriteFont::GetWeight}, slot 4 (dwrite.h:1615); a {@code DWRITE_FONT_WEIGHT}, no HRESULT. */
    static int getWeight(long self) {
        return u32(self, FONT_GET_WEIGHT);
    }

    /**
     * {@code IDWriteFont::GetInformationalStrings}, slot 9 (dwrite.h:1653): the strings, or {@code 0}
     * when the font does not carry that string. {@code directwrite.cpp:2053} returns NULL whenever
     * {@code exists} is FALSE and releases nothing, because DirectWrite documents the out-pointer as
     * NULL in that case; the callers treat null as normal.
     */
    static long getInformationalStrings(long self, int informationalStringID) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment exists = scratch.allocate(JAVA_INT);
            MemorySegment obj = com(self);
            int hr = (int) HR_I_OUT_BOOL.invokeExact(slot(obj, FONT_GET_INFORMATIONAL_STRINGS), obj,
                    informationalStringID, out, exists);
            return hr >= 0 && exists.get(JAVA_INT, 0) != 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code IDWriteFont::GetSimulations}, slot 10 (dwrite.h:1662); flags, no HRESULT. */
    static int getSimulations(long self) {
        return u32(self, FONT_GET_SIMULATIONS);
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteLocalizedStrings - dwrite.h:1371
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code IDWriteLocalizedStrings::GetString}, slot 8 (dwrite.h:1446): exactly {@code size}
     * characters, the terminating NUL included, or {@code null} when the call failed
     * ({@code directwrite.cpp:2078-2092}). The peer asks for {@code length + 1} and trims.
     */
    static char[] getString(long self, int index, int size) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment buffer = scratch.allocate(JAVA_CHAR, size);
            MemorySegment obj = com(self);
            int hr = (int) HR_I_BUF_I.invokeExact(slot(obj, STRINGS_GET_STRING), obj, index, buffer, size);
            return hr >= 0 ? buffer.toArray(JAVA_CHAR) : null;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteLocalizedStrings::GetStringLength}, slot 7 (dwrite.h:1431): the length in
     * characters without the terminator, or {@code 0} when the call failed.
     */
    static int getStringLength(long self, int index) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment length = scratch.allocate(JAVA_INT);
            MemorySegment obj = com(self);
            int hr = (int) HR_I_OUT.invokeExact(slot(obj, STRINGS_GET_STRING_LENGTH), obj, index, length);
            return hr >= 0 ? length.get(JAVA_INT, 0) : 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteLocalizedStrings::FindLocaleName}, slot 4 (dwrite.h:1388): the index, or {@code -1}
     * when the call failed or the locale is absent ({@code directwrite.cpp:2113}).
     */
    static int findLocaleName(long self, char[] locale) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment name = wide(scratch, locale);
            MemorySegment index = scratch.allocate(JAVA_INT);
            MemorySegment exists = scratch.allocate(JAVA_INT);
            MemorySegment obj = com(self);
            int hr = (int) HR_3A.invokeExact(slot(obj, STRINGS_FIND_LOCALE_NAME), obj, name, index, exists);
            return hr >= 0 && exists.get(JAVA_INT, 0) != 0 ? index.get(JAVA_INT, 0) : -1;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFontFile - dwrite.h:822
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code IDWriteFontFile::Analyze}, slot 5 (dwrite.h:868): the HRESULT straight through, with the
     * four out-parameters copied into the caller's arrays. Four quirks of
     * {@code directwrite.cpp:1958-2001} are reproduced: the {@code self == 0 -> E_FAIL} guard, the
     * "array non-null and length exactly 1" condition on each copy, the copies happening even when the
     * call failed (the C copied uninitialised stack there, this copies the zero-filled scratch, and
     * both callers gate on {@code hr == S_OK} first) and the {@code BOOL} to {@code jboolean}
     * truncation to the low byte.
     */
    static int analyze(long self, boolean[] isSupportedFontType, int[] fontFileType, int[] fontFaceType,
                       int[] numberOfFaces) {
        if (self == 0) {
            return E_FAIL;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment supported = scratch.allocate(JAVA_INT);
            MemorySegment fileType = scratch.allocate(JAVA_INT);
            MemorySegment faceType = scratch.allocate(JAVA_INT);
            MemorySegment faces = scratch.allocate(JAVA_INT);
            MemorySegment obj = com(self);
            int hr = (int) HR_4A.invokeExact(slot(obj, FONT_FILE_ANALYZE), obj, supported, fileType,
                    faceType, faces);
            if (isSupportedFontType != null && isSupportedFontType.length == 1) {
                isSupportedFontType[0] = (supported.get(JAVA_INT, 0) & 0xFF) != 0;
            }
            if (fontFileType != null && fontFileType.length == 1) {
                fontFileType[0] = fileType.get(JAVA_INT, 0);
            }
            if (fontFaceType != null && fontFaceType.length == 1) {
                fontFaceType[0] = faceType.get(JAVA_INT, 0);
            }
            if (numberOfFaces != null && numberOfFaces.length == 1) {
                numberOfFaces[0] = faces.get(JAVA_INT, 0);
            }
            return hr;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFontFace - dwrite.h:1047
     *
     * Slots 3..14 of one interface, of which only 10 (GetDesignGlyphMetrics) and 14
     * (GetGlyphRunOutline) were ever JNI natives. The other six are bound because they are the only
     * way to prove the slot arithmetic around those two <em>behaviourally</em>: each answers a value
     * that is knowable from the font file without asking DirectWrite (the face index, the glyph count
     * in maxp, the units per em in head, the cmap mapping, a whole table byte for byte), so an
     * off-by-one anywhere in 3..14 fails a test instead of silently returning a different method of
     * the same object. They cost about fifteen lines each and no peer calls them.
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteFontFace::GetType}, slot 3 (dwrite.h:1052): a {@code DWRITE_FONT_FACE_TYPE}. */
    static int getFontFaceType(long self) {
        return u32(self, FONT_FACE_GET_TYPE);
    }

    /** {@code IDWriteFontFace::GetIndex}, slot 5 (dwrite.h:1074): the face index within its files. */
    static int getFontFaceIndex(long self) {
        return u32(self, FONT_FACE_GET_INDEX);
    }

    /** {@code IDWriteFontFace::GetSimulations}, slot 6 (dwrite.h:1079): {@code DWRITE_FONT_SIMULATIONS}. */
    static int getFontFaceSimulations(long self) {
        return u32(self, FONT_FACE_GET_SIMULATIONS);
    }

    /** {@code IDWriteFontFace::IsSymbolFont}, slot 7 (dwrite.h:1084): a 4-byte {@code BOOL} in EAX. */
    static boolean isSymbolFont(long self) {
        return u32(self, FONT_FACE_IS_SYMBOL_FONT) != 0;
    }

    /**
     * {@code IDWriteFontFace::GetMetrics}, slot 8 (dwrite.h:1092): the ten fields of
     * {@code DWRITE_FONT_METRICS} in declaration order, sign-extended for the three {@code INT16}
     * fields and zero-extended for the seven {@code UINT16} ones.
     * <p>
     * This method returns {@code void} and writes through an out-pointer; nothing here is returned by
     * value. That matters because Microsoft x64 returns an aggregate that is not 1, 2, 4 or 8 bytes
     * through a hidden pointer supplied by the caller, and this package never has to know whether that
     * hidden pointer precedes or follows {@code this} - no method it binds returns an aggregate at all.
     */
    static int[] getFontFaceMetrics(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(DWRITE_FONT_METRICS_LAYOUT);
            MemorySegment obj = com(self);
            VOID_THIS_P.invokeExact(slot(obj, FONT_FACE_GET_METRICS), obj, out);
            int[] metrics = new int[10];
            for (int i = 0; i < metrics.length; i++) {
                short raw = out.getAtIndex(JAVA_SHORT, i);
                metrics[i] = SIGNED_FONT_METRIC[i] ? raw : Short.toUnsignedInt(raw);
            }
            return metrics;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** Which fields of {@code DWRITE_FONT_METRICS} are {@code INT16}: lineGap, underline, strikethrough. */
    private static final boolean[] SIGNED_FONT_METRIC =
            { false, false, false, true, false, false, true, false, true, false };

    /**
     * {@code IDWriteFontFace::GetGlyphCount}, slot 9 (dwrite.h:1099). The value is a {@code UINT16}
     * returned in AX, so it is bound as {@code JAVA_SHORT} and widened here; a font with more than
     * 32,768 glyphs would otherwise come back negative.
     */
    static int getGlyphCount(long self) {
        MemorySegment obj = com(self);
        try {
            return Short.toUnsignedInt((short) U16_THIS.invokeExact(slot(obj, FONT_FACE_GET_GLYPH_COUNT), obj));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteFontFace::GetDesignGlyphMetrics}, slot 10 (dwrite.h:1115), for the one glyph the
     * JNI native took ({@code directwrite.cpp:1814-1828}): {@code null} on a failing HRESULT, exactly
     * as there, because {@code DWFontFile.createGlyphBoundingBox} and {@code DWGlyph.checkMetrics}
     * both branch on null.
     * <p>
     * The glyph index crosses as a raw {@code JAVA_SHORT}, never widened: {@code DWGlyph} keeps glyph
     * codes in a {@code short} and a CJK font with more than 32,768 glyphs makes it negative, which a
     * {@code JAVA_INT} store would sign-extend into a nonsense {@code UINT16}. The metrics come back
     * through a 28-byte out-parameter, not as a returned struct.
     */
    static DWRITE_GLYPH_METRICS getDesignGlyphMetrics(long self, short glyphIndex, boolean isSideways) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment indices = scratch.allocateFrom(JAVA_SHORT, glyphIndex);
            MemorySegment out = scratch.allocate(DWRITE_GLYPH_METRICS_LAYOUT);
            MemorySegment obj = com(self);
            int hr = (int) HR_DESIGN_GLYPH_METRICS.invokeExact(
                    slot(obj, FONT_FACE_GET_DESIGN_GLYPH_METRICS), obj, indices, 1, out,
                    isSideways ? 1 : 0);
            if (hr < 0) {
                return null;
            }
            DWRITE_GLYPH_METRICS metrics = new DWRITE_GLYPH_METRICS();
            metrics.leftSideBearing = out.getAtIndex(JAVA_INT, 0);
            metrics.advanceWidth = out.getAtIndex(JAVA_INT, 1);
            metrics.rightSideBearing = out.getAtIndex(JAVA_INT, 2);
            metrics.topSideBearing = out.getAtIndex(JAVA_INT, 3);
            metrics.advanceHeight = out.getAtIndex(JAVA_INT, 4);
            metrics.bottomSideBearing = out.getAtIndex(JAVA_INT, 5);
            metrics.verticalOriginY = out.getAtIndex(JAVA_INT, 6);
            return metrics;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteFontFace::GetGlyphIndices}, slot 11 (dwrite.h:1136): the nominal cmap mapping of
     * UTF-32 code points to glyph indices, or {@code null} on a failing HRESULT. Missing code points
     * come back as glyph 0, which is what the cmap says and what {@code OpenTypeGlyphMapper} answers.
     * <p>
     * Both arrays cross off-heap. {@code critical(true)} is not an option: a cmap lookup can fault the
     * font file in through the DirectWrite font cache service, and a pinned Java array across a call
     * that can block is exactly what the option forbids.
     */
    static short[] getGlyphIndices(long self, int[] codePoints) {
        if (codePoints.length == 0) {
            return new short[0];
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment in = scratch.allocateFrom(JAVA_INT, codePoints);
            MemorySegment out = scratch.allocate(JAVA_SHORT, codePoints.length);
            MemorySegment obj = com(self);
            int hr = (int) HR_P_I_OUT.invokeExact(slot(obj, FONT_FACE_GET_GLYPH_INDICES), obj, in,
                    codePoints.length, out);
            return hr >= 0 ? out.toArray(JAVA_SHORT) : null;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteFontFace::TryGetFontTable}, slot 12 (dwrite.h:1176), copied into a Java array and
     * released again through {@code ReleaseFontTable}, slot 13 (dwrite.h:1188): the raw sfnt table
     * bytes, or {@code null} when the face does not carry that table.
     * <p>
     * The pointer DirectWrite hands back addresses its own mapping of the font file and is valid only
     * until {@code ReleaseFontTable}, so it is copied at once and never retained. The {@code finally}
     * makes the release unconditional, which the documented contract requires whenever {@code exists}
     * came back TRUE.
     *
     * @param openTypeTableTag the tag as {@code DWRITE_MAKE_OPENTYPE_TAG} builds it - four bytes in
     *                         file order packed little-endian, so "cmap" is {@code 0x70616D63}
     */
    static byte[] tryGetFontTable(long self, int openTypeTableTag) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment data = scratch.allocate(ADDRESS);
            MemorySegment size = scratch.allocate(JAVA_INT);
            MemorySegment context = scratch.allocate(ADDRESS);
            MemorySegment exists = scratch.allocate(JAVA_INT);
            MemorySegment obj = com(self);
            int hr = (int) HR_FONT_TABLE.invokeExact(slot(obj, FONT_FACE_TRY_GET_FONT_TABLE), obj,
                    openTypeTableTag, data, size, context, exists);
            if (hr < 0 || exists.get(JAVA_INT, 0) == 0) {
                return null;
            }
            MemorySegment table = data.get(ADDRESS, 0);
            MemorySegment tableContext = context.get(ADDRESS, 0);
            try {
                return bounded(table, Integer.toUnsignedLong(size.get(JAVA_INT, 0))).toArray(JAVA_BYTE);
            } finally {
                VOID_THIS_P.invokeExact(slot(obj, FONT_FACE_RELEASE_FONT_TABLE), obj, tableContext);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFactory::CreateGlyphRunAnalysis and IDWriteGlyphRunAnalysis - dwrite.h:5090, :4646
     * ------------------------------------------------------------------------------------------- */

    /** {@code dwrite.h}: {@code DWRITE_TEXTURE_CLEARTYPE_3x1}, the one texture type that is 3 bytes deep. */
    static final int DWRITE_TEXTURE_CLEARTYPE_3x1 = 1;

    /** {@code UINT32_MAX}, as the unsigned comparisons of {@code directwrite.cpp:2182-2185} use it. */
    private static final long UINT32_MAX = 0xFFFFFFFFL;

    /**
     * {@code IDWriteFactory::CreateGlyphRunAnalysis}, slot 23 (dwrite.h:5090)
     * ({@code directwrite.cpp:1926-1955}). Both structs cross as pointers; neither is passed by value,
     * so no aggregate-argument rule applies here beyond "the layout must be right", which the LCD mask
     * bytes prove end to end.
     * <p>
     * Two quirks of the C are kept. A {@code null} {@code DWRITE_GLYPH_RUN} or {@code DWRITE_MATRIX}
     * reaches DirectWrite as {@code NULL} rather than as a zeroed struct ({@code :1939-1940}) -
     * {@code DWGlyph.createAnalysis} really does pass a null {@code strike.matrix} - and the run always
     * describes exactly one glyph, with three one-element arrays ({@code :1933-1936}), which is what
     * the single-valued Java mirror {@code DWRITE_GLYPH_RUN} was built for.
     */
    static long createGlyphRunAnalysis(long self, DWRITE_GLYPH_RUN glyphRun, float pixelsPerDip,
                                       DWRITE_MATRIX transform, int renderingMode, int measuringMode,
                                       float baselineOriginX, float baselineOriginY) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment run = glyphRun == null ? MemorySegment.NULL : encodeGlyphRun(scratch, glyphRun);
            MemorySegment matrix = transform == null ? MemorySegment.NULL : encodeMatrix(scratch, transform);
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_GLYPH_RUN_ANALYSIS.invokeExact(
                    slot(obj, FACTORY_CREATE_GLYPH_RUN_ANALYSIS), obj, run, pixelsPerDip, matrix,
                    renderingMode, measuringMode, baselineOriginX, baselineOriginY, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteGlyphRunAnalysis::GetAlphaTextureBounds}, slot 3 (dwrite.h:4659)
     * ({@code directwrite.cpp:2200-2210}): {@code null} on a failing HRESULT, as there. The
     * {@code RECT} arrives through a 16-byte out-parameter; the caller ({@code DWGlyph.checkBounds})
     * then mutates the returned object, so it stays the plain Java holder it has always been.
     */
    static RECT getAlphaTextureBounds(long self, int textureType) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(RECT_LAYOUT);
            MemorySegment obj = com(self);
            int hr = (int) HR_I_OUT.invokeExact(slot(obj, ANALYSIS_GET_ALPHA_TEXTURE_BOUNDS), obj,
                    textureType, out);
            if (hr < 0) {
                return null;
            }
            RECT rect = new RECT();
            rect.left = out.getAtIndex(JAVA_INT, 0);
            rect.top = out.getAtIndex(JAVA_INT, 1);
            rect.right = out.getAtIndex(JAVA_INT, 2);
            rect.bottom = out.getAtIndex(JAVA_INT, 3);
            return rect;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteGlyphRunAnalysis::CreateAlphaTexture}, slot 4 (dwrite.h:4677)
     * ({@code directwrite.cpp:2171-2198}). The guard ladder of the C is reproduced in its exact order,
     * and its arithmetic is unsigned 32-bit: a null {@code RECT} gives {@code null}; an empty or
     * inverted rectangle in either axis gives {@code null}; the depth is 3 for
     * {@code CLEARTYPE_3x1} and 1 otherwise; and the two overflow guards reject a buffer whose size
     * would not fit a {@code UINT32}.
     * <p>
     * One deliberate divergence, which cannot be made bug-compatible: the C hands the result of
     * {@code new (std::nothrow) BYTE[]} to DirectWrite without a null check ({@code :2188-2189}), so an
     * allocation failure dereferences null. Here the arena raises {@code OutOfMemoryError} instead. The
     * two paths also fail differently above 2 GB, which no glyph mask can reach.
     */
    static byte[] createAlphaTexture(long self, int textureType, RECT textureBounds) {
        if (textureBounds == null) {
            return null;
        }
        if (textureBounds.right <= textureBounds.left) {
            return null;
        }
        if (textureBounds.bottom <= textureBounds.top) {
            return null;
        }
        long width = Integer.toUnsignedLong(textureBounds.right - textureBounds.left);
        long height = Integer.toUnsignedLong(textureBounds.bottom - textureBounds.top);
        long bpp = textureType == DWRITE_TEXTURE_CLEARTYPE_3x1 ? 3 : 1;
        if (height > UINT32_MAX / bpp) {
            return null;
        }
        if (height > 0 && width > UINT32_MAX / (height * bpp)) {
            return null;
        }
        long bufferSize = width * height * bpp;
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment bounds = encodeRect(scratch, textureBounds);
            MemorySegment buffer = scratch.allocate(bufferSize);
            MemorySegment obj = com(self);
            int hr = (int) HR_ALPHA_TEXTURE.invokeExact(slot(obj, ANALYSIS_CREATE_ALPHA_TEXTURE), obj,
                    textureType, bounds, buffer, (int) bufferSize);
            return hr >= 0 ? buffer.toArray(JAVA_BYTE) : null;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The 48-byte {@code DWRITE_GLYPH_RUN} plus the three one-element arrays it points at. */
    private static MemorySegment encodeGlyphRun(Arena arena, DWRITE_GLYPH_RUN holder) {
        MemorySegment indices = arena.allocateFrom(JAVA_SHORT, holder.glyphIndices);
        MemorySegment advances = arena.allocateFrom(JAVA_FLOAT, holder.glyphAdvances);
        MemorySegment offsets = arena.allocate(DWRITE_GLYPH_OFFSET_LAYOUT);
        offsets.setAtIndex(JAVA_FLOAT, 0, holder.advanceOffset);
        offsets.setAtIndex(JAVA_FLOAT, 1, holder.ascenderOffset);
        MemorySegment run = arena.allocate(DWRITE_GLYPH_RUN_LAYOUT);
        run.set(ADDRESS, RUN_FONT_FACE, com(holder.fontFace));
        run.set(JAVA_FLOAT, RUN_FONT_EM_SIZE, holder.fontEmSize);
        run.set(JAVA_INT, RUN_GLYPH_COUNT, 1);
        run.set(ADDRESS, RUN_GLYPH_INDICES, indices);
        run.set(ADDRESS, RUN_GLYPH_ADVANCES, advances);
        run.set(ADDRESS, RUN_GLYPH_OFFSETS, offsets);
        run.set(JAVA_INT, RUN_IS_SIDEWAYS, holder.isSideways ? 1 : 0);
        run.set(JAVA_INT, RUN_BIDI_LEVEL, holder.bidiLevel);
        return run;
    }

    /** The 24-byte {@code DWRITE_MATRIX}: six floats in declaration order. */
    private static MemorySegment encodeMatrix(Arena arena, DWRITE_MATRIX holder) {
        MemorySegment matrix = arena.allocate(DWRITE_MATRIX_LAYOUT);
        matrix.setAtIndex(JAVA_FLOAT, 0, holder.m11);
        matrix.setAtIndex(JAVA_FLOAT, 1, holder.m12);
        matrix.setAtIndex(JAVA_FLOAT, 2, holder.m21);
        matrix.setAtIndex(JAVA_FLOAT, 3, holder.m22);
        matrix.setAtIndex(JAVA_FLOAT, 4, holder.dx);
        matrix.setAtIndex(JAVA_FLOAT, 5, holder.dy);
        return matrix;
    }

    /** The 16-byte {@code RECT}: four {@code LONG} in declaration order. */
    private static MemorySegment encodeRect(Arena arena, RECT holder) {
        MemorySegment rect = arena.allocate(RECT_LAYOUT);
        rect.setAtIndex(JAVA_INT, 0, holder.left);
        rect.setAtIndex(JAVA_INT, 1, holder.top);
        rect.setAtIndex(JAVA_INT, 2, holder.right);
        rect.setAtIndex(JAVA_INT, 3, holder.bottom);
        return rect;
    }

    /* ---------------------------------------------------------------------------------------------
     * The geometry sink: a COM object implemented in Java
     *
     * IDWriteFontFace::GetGlyphRunOutline does not return an outline; it calls one back, into an
     * ID2D1SimplifiedGeometrySink the caller supplies (dwrite.h:1041 typedefs IDWriteGeometrySink to
     * that interface, so one IID answers both names). The C implemented it as the C++ class
     * JFXGeometrySink, directwrite.cpp:1616-1766. Java implements it here, with no C at all: a
     * 16-byte block whose word 0 points at a ten-entry table of Linker.upcallStub segments and whose
     * word 1 is a registry id - never a Java reference, never a pointer DirectWrite could dereference.
     *
     * Everything DirectWrite does with this object happens synchronously, on the calling thread,
     * inside the GetGlyphRunOutline frame; it keeps no pointer afterwards, which the C proves by
     * deleting its sink the moment the call returns (:1810). So the vtable is process-wide (the
     * targets are static methods; one table, built once, in Arena.global()) while the object itself
     * lives in the per-call confined arena.
     * ------------------------------------------------------------------------------------------- */

    /** {@code winerror.h}: what every successful COM call returns. */
    static final int S_OK = 0;

    /**
     * {@code ID2D1SimplifiedGeometrySink} (d2d1.h:2174). {@code IDWriteGeometrySink} is a typedef for
     * the same interface (dwrite.h:1041), so this one IID answers both of the C QueryInterface arms.
     */
    static final String IID_ID2D1_SIMPLIFIED_GEOMETRY_SINK = "2cd9069e-12e2-11dc-9fed-001143a055f9";

    private static final long SINK_USER_OFFSET = offsetOf(SINK_LAYOUT, "user");

    private static final byte[] IID_IUNKNOWN_BYTES = guidBytes(IID_IUNKNOWN);
    private static final byte[] IID_GEOMETRY_SINK_BYTES = guidBytes(IID_ID2D1_SIMPLIFIED_GEOMETRY_SINK);

    /**
     * The sessions of the outline calls currently on some stack, keyed by the id stored in word 1 of
     * the object DirectWrite holds. A registry rather than a {@code ThreadLocal} because the house
     * rule is a registry and because a {@code ThreadLocal} would hide re-entrancy; a miss is always
     * tolerated, so a callback arriving after the entry is gone is a no-op rather than a crash.
     */
    private static final Map<Long, OutlineSession> SINKS = new ConcurrentHashMap<>();

    /** Ids start at 1, so a zeroed or freed word 1 reads as 0 and misses the registry cleanly. */
    private static final AtomicLong NEXT_SINK_ID = new AtomicLong(1);

    /**
     * {@code IDWriteFontFace::GetGlyphRunOutline}, slot 14 (dwrite.h:1209)
     * ({@code directwrite.cpp:1769-1812}), driving a Java-synthesized sink.
     * <p>
     * Four of the eight arguments are fixed exactly as the C fixed them: one glyph, a one-element
     * {@code UINT16} array, {@code NULL} advances and {@code NULL} offsets - {@code MemorySegment.NULL},
     * not zero-filled arrays, because DirectWrite documents NULL as "use the font's own advances" -
     * and {@code isRightToLeft} FALSE. The result is a {@code Path2D} with winding rule
     * {@code WIND_EVEN_ODD}, which is the literal 0 the C passed at {@code :1804}.
     * <p>
     * Failure is {@code null} and nothing else, as it was in JNI: {@code PrismFontStrike} and
     * {@code CompositeStrike} test the result for null and have no catch. An {@code Error} raised in a
     * callback still propagates - the C would have hit {@code std::terminate} there - but a
     * {@code RuntimeException} is logged and turned into {@code null}, so this method throws nothing
     * the JNI version could not.
     */
    static Path2D getGlyphRunOutline(long self, float emSize, short glyphIndex, boolean isSideways) {
        MemorySegment vtable = Sink.VTABLE;
        if (vtable.address() == 0) {
            return null;
        }
        OutlineSession session = new OutlineSession(false);
        Long id = NEXT_SINK_ID.getAndIncrement();
        SINKS.put(id, session);
        int hr;
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment sink = allocateSink(scratch, vtable, id);
            MemorySegment indices = scratch.allocateFrom(JAVA_SHORT, glyphIndex);
            MemorySegment obj = com(self);
            hr = (int) HR_GLYPH_RUN_OUTLINE.invokeExact(slot(obj, FONT_FACE_GET_GLYPH_RUN_OUTLINE),
                    obj, emSize, indices, MemorySegment.NULL, MemorySegment.NULL, 1,
                    isSideways ? 1 : 0, 0, sink);
        } catch (Throwable t) {
            throw unexpected(t);
        } finally {
            SINKS.remove(id);
        }
        Throwable failure = session.pending;
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure != null) {
            PlatformLogger logger = Logging.getJavaFXLogger();
            if (logger.isLoggable(PlatformLogger.Level.SEVERE)) {
                logger.severe("glyph outline callback failed for glyph " + (glyphIndex & 0xFFFF), failure);
            }
            return null;
        }
        return hr >= 0 ? session.toPath() : null;
    }

    /** How many outline calls are on some stack right now: 0 between calls, or a leaked id. */
    static int sinkRegistrySize() {
        return SINKS.size();
    }

    /** The 16-byte object DirectWrite receives: the shared vtable, then this call's registry id. */
    private static MemorySegment allocateSink(Arena arena, MemorySegment vtable, long id) {
        MemorySegment sink = arena.allocate(SINK_LAYOUT);
        sink.set(ADDRESS, 0, vtable);
        sink.set(JAVA_LONG, SINK_USER_OFFSET, id);
        return sink;
    }

    /**
     * The session behind the object at {@code self}, or {@code null} when word 1 names no live call.
     * Every upcall target tolerates that miss and returns its sentinel rather than dereferencing
     * anything: an outline call whose arena has already closed leaves freed bytes there, and a COM
     * object must not fault its caller.
     */
    private static OutlineSession sinkSession(MemorySegment self) {
        try {
            return SINKS.get(bounded(self, SINK_LAYOUT.byteSize()).get(JAVA_LONG, SINK_USER_OFFSET));
        } catch (Throwable t) {
            return null;
        }
    }

    /* The ten upcall targets. None may throw: an exception escaping into DirectWrite would unwind a
     * native frame. Each stashes instead, which also makes every later callback of the same call a
     * no-op, so a half-built path is never handed back. */

    private static int sinkQueryInterface(MemorySegment self, MemorySegment riid, MemorySegment out) {
        MemorySegment target = MemorySegment.NULL;
        try {
            target = bounded(out, ADDRESS.byteSize());
            byte[] id = bounded(riid, GUID_LAYOUT.byteSize()).toArray(JAVA_BYTE);
            if (Arrays.equals(id, IID_GEOMETRY_SINK_BYTES) || Arrays.equals(id, IID_IUNKNOWN_BYTES)) {
                target.set(ADDRESS, 0, self);
                sinkAddRef(self);
                return S_OK;
            }
            target.set(ADDRESS, 0, MemorySegment.NULL);
            // E_FAIL, not E_NOINTERFACE: directwrite.cpp:1762 answers E_FAIL and callers see that.
            return E_FAIL;
        } catch (Throwable t) {
            stash(self, t);
            try {
                if (target.address() != 0) {
                    target.set(ADDRESS, 0, MemorySegment.NULL);
                }
            } catch (Throwable ignored) {
                // the out-pointer itself is unusable; there is nothing further to report to COM
            }
            return E_FAIL;
        }
    }

    private static int sinkAddRef(MemorySegment self) {
        OutlineSession session = sinkSession(self);
        return session == null ? 1 : session.addRef();
    }

    /**
     * Counts down but frees nothing. The C starts its count at 0 ({@code :1666}), frees at 0
     * ({@code :1744-1750}) and is also deleted unconditionally by the entry point ({@code :1810}) -
     * two frees that have never both fired, which is positive evidence that DirectWrite does not drive
     * this count to zero. Here the confined arena is the only owner, so the question cannot arise.
     */
    private static int sinkRelease(MemorySegment self) {
        OutlineSession session = sinkSession(self);
        return session == null ? 0 : session.release();
    }

    /** Ignored, exactly as at {@code directwrite.cpp:1689-1691}: the winding rule is hardcoded. */
    private static void sinkSetFillMode(MemorySegment self, int fillMode) {
    }

    /** Ignored, exactly as at {@code directwrite.cpp:1693-1695}. */
    private static void sinkSetSegmentFlags(MemorySegment self, int vertexFlags) {
    }

    /**
     * The by-value {@code D2D1_POINT_2F} arrives as an 8-byte segment the linker materialises for the
     * duration of this upcall only; its two floats are read here and the segment is never stored.
     * {@code figureBegin} (hollow or filled) is discarded and the point becomes a move-to whatever it
     * says, as at {@code directwrite.cpp:1697-1702}.
     */
    private static void sinkBeginFigure(MemorySegment self, MemorySegment startPoint, int figureBegin) {
        OutlineSession session = sinkSession(self);
        if (session == null || session.failed()) {
            return;
        }
        try {
            session.segment(PathIterator.SEG_MOVETO, startPoint.get(JAVA_FLOAT, 0),
                    startPoint.get(JAVA_FLOAT, 4));
        } catch (Throwable t) {
            session.fail(t);
        }
    }

    /** {@code pointsCount} is a {@code UINT32}; widen it unsigned before sizing the array. */
    private static void sinkAddLines(MemorySegment self, MemorySegment points, int pointsCount) {
        OutlineSession session = sinkSession(self);
        if (session == null || session.failed()) {
            return;
        }
        try {
            long stride = D2D1_POINT_2F_LAYOUT.byteSize();
            long count = Integer.toUnsignedLong(pointsCount);
            MemorySegment array = bounded(points, count * stride);
            for (long i = 0; i < count; i++) {
                long base = i * stride;
                session.segment(PathIterator.SEG_LINETO, array.get(JAVA_FLOAT, base),
                        array.get(JAVA_FLOAT, base + 4));
            }
        } catch (Throwable t) {
            session.fail(t);
        }
    }

    /**
     * Every curve is emitted as a cubic, never a quadratic, even when {@code point1 == point2} - the
     * simplified geometry sink has no quadratic callback and the C said so at
     * {@code directwrite.cpp:1717-1719}. {@code D2D1_BEZIER_SEGMENT} is 24 bytes and crosses by
     * pointer; only {@code BeginFigure} takes an aggregate by value.
     */
    private static void sinkAddBeziers(MemorySegment self, MemorySegment beziers, int beziersCount) {
        OutlineSession session = sinkSession(self);
        if (session == null || session.failed()) {
            return;
        }
        try {
            long stride = D2D1_BEZIER_SEGMENT_LAYOUT.byteSize();
            long count = Integer.toUnsignedLong(beziersCount);
            MemorySegment array = bounded(beziers, count * stride);
            for (long i = 0; i < count; i++) {
                long base = i * stride;
                session.segment(PathIterator.SEG_CUBICTO,
                        array.get(JAVA_FLOAT, base), array.get(JAVA_FLOAT, base + 4),
                        array.get(JAVA_FLOAT, base + 8), array.get(JAVA_FLOAT, base + 12),
                        array.get(JAVA_FLOAT, base + 16), array.get(JAVA_FLOAT, base + 20));
            }
        } catch (Throwable t) {
            session.fail(t);
        }
    }

    /** A close with no coordinates whatever {@code figureEnd} says, as at {@code :1730-1733}. */
    private static void sinkEndFigure(MemorySegment self, int figureEnd) {
        OutlineSession session = sinkSession(self);
        if (session == null || session.failed()) {
            return;
        }
        try {
            session.segment(PathIterator.SEG_CLOSE);
        } catch (Throwable t) {
            session.fail(t);
        }
    }

    /** Always {@code S_OK}, as at {@code directwrite.cpp:1735-1737}. */
    private static int sinkClose(MemorySegment self) {
        return S_OK;
    }

    private static void stash(MemorySegment self, Throwable t) {
        OutlineSession session = sinkSession(self);
        if (session != null) {
            session.fail(t);
        }
    }

    /**
     * The growable buffers behind one outline call - the Java counterpart of the two
     * {@code std::vector}s at {@code directwrite.cpp:1661-1662} - plus the reference count DirectWrite
     * may move and the slot that holds a failure until the native frame has unwound.
     * <p>
     * The arrays handed to {@link Path2D} are always fresh and exactly the right length. That is not
     * tidiness: {@code Path2D(int, byte[], int, float[], int)} stores both arrays <em>by reference</em>
     * and its javadoc says the caller promises to drop every other reference, so a shared or
     * over-long buffer would let one glyph read another glyph's coordinates with no exception
     * anywhere. One session per call, one copy per session.
     */
    private static final class OutlineSession {

        private final boolean failForTesting;
        private byte[] types = new byte[32];
        private float[] coords = new float[64];
        private int numTypes;
        private int numCoords;
        private int refCount;
        private Throwable pending;

        OutlineSession(boolean failForTesting) {
            this.failForTesting = failForTesting;
        }

        int addRef() {
            return ++refCount;
        }

        int release() {
            return --refCount;
        }

        boolean failed() {
            return pending != null;
        }

        void fail(Throwable t) {
            if (pending == null) {
                pending = t;
            }
        }

        void segment(int type, float... values) {
            if (failForTesting) {
                throw new IllegalStateException("injected geometry sink failure");
            }
            if (numTypes == types.length) {
                types = Arrays.copyOf(types, types.length * 2);
            }
            types[numTypes++] = (byte) type;
            if (numCoords + values.length > coords.length) {
                coords = Arrays.copyOf(coords, Math.max(coords.length * 2, numCoords + values.length));
            }
            for (float value : values) {
                coords[numCoords++] = value;
            }
        }

        Path2D toPath() {
            return new Path2D(Path2D.WIND_EVEN_ODD, Arrays.copyOf(types, numTypes), numTypes,
                    Arrays.copyOf(coords, numCoords), numCoords);
        }
    }

    /**
     * The ten upcall stubs, built once and never freed - the one legitimate {@link Arena#global()} in
     * this package, because the targets are static methods and there is exactly one table per process.
     * Held by a nested class so that nothing is created until the first outline is asked for, and
     * {@link #build()} cannot throw, so this initializer cannot either: a table that could not be made
     * is {@link MemorySegment#NULL} and {@link #getGlyphRunOutline} then answers {@code null}, which is
     * what every failure in this area has always looked like to the callers.
     */
    private static final class Sink {

        static final MemorySegment VTABLE = build();

        private static MemorySegment build() {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MemorySegment table = Arena.global()
                        .allocate(ADDRESS.byteSize() * SINK_SLOT_COUNT, ADDRESS.byteSize());
                table.setAtIndex(ADDRESS, IUNKNOWN_QUERY_INTERFACE, sinkStub(lookup, "sinkQueryInterface",
                        SINK_QUERY_INTERFACE_FD, int.class, MemorySegment.class, MemorySegment.class,
                        MemorySegment.class));
                table.setAtIndex(ADDRESS, IUNKNOWN_ADD_REF,
                        sinkStub(lookup, "sinkAddRef", SINK_THIS_FD, int.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, IUNKNOWN_RELEASE,
                        sinkStub(lookup, "sinkRelease", SINK_THIS_FD, int.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, SINK_SET_FILL_MODE, sinkStub(lookup, "sinkSetFillMode",
                        SINK_THIS_INT_FD, void.class, MemorySegment.class, int.class));
                table.setAtIndex(ADDRESS, SINK_SET_SEGMENT_FLAGS, sinkStub(lookup, "sinkSetSegmentFlags",
                        SINK_THIS_INT_FD, void.class, MemorySegment.class, int.class));
                table.setAtIndex(ADDRESS, SINK_BEGIN_FIGURE, sinkStub(lookup, "sinkBeginFigure",
                        SINK_BEGIN_FIGURE_FD, void.class, MemorySegment.class, MemorySegment.class,
                        int.class));
                table.setAtIndex(ADDRESS, SINK_ADD_LINES, sinkStub(lookup, "sinkAddLines",
                        SINK_ARRAY_FD, void.class, MemorySegment.class, MemorySegment.class, int.class));
                table.setAtIndex(ADDRESS, SINK_ADD_BEZIERS, sinkStub(lookup, "sinkAddBeziers",
                        SINK_ARRAY_FD, void.class, MemorySegment.class, MemorySegment.class, int.class));
                table.setAtIndex(ADDRESS, SINK_END_FIGURE, sinkStub(lookup, "sinkEndFigure",
                        SINK_THIS_INT_FD, void.class, MemorySegment.class, int.class));
                table.setAtIndex(ADDRESS, SINK_CLOSE,
                        sinkStub(lookup, "sinkClose", SINK_THIS_FD, int.class, MemorySegment.class));
                return table;
            } catch (Throwable t) {
                return MemorySegment.NULL;
            }
        }
    }

    @SuppressWarnings("restricted")
    private static MemorySegment sinkStub(MethodHandles.Lookup lookup, String name,
                                          FunctionDescriptor descriptor, Class<?> returnType,
                                          Class<?>... parameterTypes) throws ReflectiveOperationException {
        MethodHandle target = lookup.findStatic(DWNative.class, name,
                MethodType.methodType(returnType, parameterTypes));
        return LINKER.upcallStub(target, descriptor, Arena.global());
    }

    /* ---------------------------------------------------------------------------------------------
     * Sink self-test hooks
     *
     * These call the synthesized vtable the way DirectWrite calls it - through the function pointers
     * in the table, with the same FunctionDescriptors - so a test can check the object in isolation
     * before any glyph is involved. They prove the table is well formed and that the by-value point
     * survives a round trip; they cannot prove the Microsoft x64 register assignment, because both
     * ends are the same linker. That proof is the outline parity against the JNI path.
     * ------------------------------------------------------------------------------------------- */

    /**
     * Drives {@code QueryInterface}, {@code AddRef}, {@code Release} and {@code Close} through the
     * sink vtable.
     *
     * @return thirteen values, or {@code null} when the vtable could not be built:
     *         {@code 0} QueryInterface(ID2D1SimplifiedGeometrySink) HRESULT,
     *         {@code 1} 1 if it wrote self, {@code 2} QueryInterface(IUnknown) HRESULT,
     *         {@code 3} 1 if it wrote self, {@code 4} QueryInterface(unrelated IID) HRESULT unsigned,
     *         {@code 5} 1 if it wrote NULL, {@code 6..9} AddRef then three Releases,
     *         {@code 10} Close HRESULT, {@code 11} the registry size during the calls,
     *         {@code 12} AddRef after the count reached zero, which must still answer
     */
    static long[] sinkSelfTest() {
        MemorySegment vtable = Sink.VTABLE;
        if (vtable.address() == 0) {
            return null;
        }
        OutlineSession session = new OutlineSession(false);
        Long id = NEXT_SINK_ID.getAndIncrement();
        SINKS.put(id, session);
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment sink = allocateSink(scratch, vtable, id);
            MethodHandle queryInterface = virtual(SINK_QUERY_INTERFACE_FD);
            MethodHandle counted = virtual(SINK_THIS_FD);
            MemorySegment out = scratch.allocate(ADDRESS);
            long[] result = new long[13];

            MemorySegment sinkIid = guid(scratch, IID_ID2D1_SIMPLIFIED_GEOMETRY_SINK);
            result[0] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                    slot(sink, IUNKNOWN_QUERY_INTERFACE), sink, sinkIid, out));
            result[1] = out.get(ADDRESS, 0).address() == sink.address() ? 1 : 0;

            MemorySegment unknownIid = guid(scratch, IID_IUNKNOWN);
            result[2] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                    slot(sink, IUNKNOWN_QUERY_INTERFACE), sink, unknownIid, out));
            result[3] = out.get(ADDRESS, 0).address() == sink.address() ? 1 : 0;

            MemorySegment otherIid = guid(scratch, IID_IDWRITE_FACTORY);
            result[4] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                    slot(sink, IUNKNOWN_QUERY_INTERFACE), sink, otherIid, out));
            result[5] = out.get(ADDRESS, 0).address() == 0 ? 1 : 0;

            result[6] = (int) counted.invokeExact(slot(sink, IUNKNOWN_ADD_REF), sink);
            result[7] = (int) counted.invokeExact(slot(sink, IUNKNOWN_RELEASE), sink);
            result[8] = (int) counted.invokeExact(slot(sink, IUNKNOWN_RELEASE), sink);
            result[9] = (int) counted.invokeExact(slot(sink, IUNKNOWN_RELEASE), sink);
            result[10] = Integer.toUnsignedLong((int) counted.invokeExact(slot(sink, SINK_CLOSE), sink));
            result[11] = SINKS.size();
            result[12] = (int) counted.invokeExact(slot(sink, IUNKNOWN_ADD_REF), sink);
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        } finally {
            SINKS.remove(id);
        }
    }

    /**
     * Drives one whole figure through the sink vtable with known coordinates and returns the
     * {@code Path2D} the session built: {@code SetFillMode} and {@code SetSegmentFlags} (both ignored),
     * one {@code BeginFigure} carrying the by-value {@code D2D1_POINT_2F}, one {@code AddLines} of two
     * points, one {@code AddBeziers} of one segment and one {@code EndFigure}.
     */
    static Path2D sinkSelfTestOutline() {
        MemorySegment vtable = Sink.VTABLE;
        if (vtable.address() == 0) {
            return null;
        }
        OutlineSession session = new OutlineSession(false);
        Long id = NEXT_SINK_ID.getAndIncrement();
        SINKS.put(id, session);
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment sink = allocateSink(scratch, vtable, id);
            driveOneFigure(scratch, sink);
            return session.pending == null ? session.toPath() : null;
        } catch (Throwable t) {
            throw unexpected(t);
        } finally {
            SINKS.remove(id);
        }
    }

    /**
     * The same figure, but with a session whose first append throws, to show what a failing callback
     * does: the failure is stashed rather than thrown into the caller, every later callback of the
     * same call becomes a no-op, no partial path is built and the registry entry still goes away.
     *
     * @return {@code { 1 if a failure was stashed, segments appended, coordinates appended,
     *         registry size afterwards }}
     */
    static long[] sinkSelfTestFailure() {
        MemorySegment vtable = Sink.VTABLE;
        if (vtable.address() == 0) {
            return null;
        }
        OutlineSession session = new OutlineSession(true);
        Long id = NEXT_SINK_ID.getAndIncrement();
        SINKS.put(id, session);
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment sink = allocateSink(scratch, vtable, id);
            driveOneFigure(scratch, sink);
        } catch (Throwable t) {
            throw unexpected(t);
        } finally {
            SINKS.remove(id);
        }
        return new long[] { session.failed() ? 1 : 0, session.numTypes, session.numCoords, SINKS.size() };
    }

    /** The coordinates {@link #sinkSelfTestOutline} and {@link #sinkSelfTestFailure} feed the sink. */
    static final float[] SINK_SELF_TEST_FIGURE = {
            1.5f, -2.25f,                                   // BeginFigure, by value
            3.5f, 4.5f, -5.5f, 6.5f,                        // AddLines, two points
            7.5f, 8.5f, 9.5f, 10.5f, 11.5f, 12.5f };        // AddBeziers, one cubic

    private static void driveOneFigure(Arena scratch, MemorySegment sink) throws Throwable {
        MethodHandle withInt = virtual(SINK_THIS_INT_FD);
        MethodHandle beginFigure = virtual(SINK_BEGIN_FIGURE_FD);
        MethodHandle withArray = virtual(SINK_ARRAY_FD);
        MethodHandle counted = virtual(SINK_THIS_FD);
        float[] figure = SINK_SELF_TEST_FIGURE;

        MemorySegment start = scratch.allocate(D2D1_POINT_2F_LAYOUT);
        start.setAtIndex(JAVA_FLOAT, 0, figure[0]);
        start.setAtIndex(JAVA_FLOAT, 1, figure[1]);
        MemorySegment lines = scratch.allocate(D2D1_POINT_2F_LAYOUT, 2);
        for (int i = 0; i < 4; i++) {
            lines.setAtIndex(JAVA_FLOAT, i, figure[2 + i]);
        }
        MemorySegment beziers = scratch.allocate(D2D1_BEZIER_SEGMENT_LAYOUT);
        for (int i = 0; i < 6; i++) {
            beziers.setAtIndex(JAVA_FLOAT, i, figure[6 + i]);
        }

        withInt.invokeExact(slot(sink, SINK_SET_FILL_MODE), sink, 1);
        withInt.invokeExact(slot(sink, SINK_SET_SEGMENT_FLAGS), sink, 1);
        beginFigure.invokeExact(slot(sink, SINK_BEGIN_FIGURE), sink, start, 0);
        withArray.invokeExact(slot(sink, SINK_ADD_LINES), sink, lines, 2);
        withArray.invokeExact(slot(sink, SINK_ADD_BEZIERS), sink, beziers, 1);
        withInt.invokeExact(slot(sink, SINK_END_FIGURE), sink, 0);
        int ignored = (int) counted.invokeExact(slot(sink, SINK_CLOSE), sink);
    }

    /* ---------------------------------------------------------------------------------------------
     * Shapes shared by several of the above
     * ------------------------------------------------------------------------------------------- */

    /** {@code HRESULT f(this, void** out)}: the pointer, or {@code 0} on any failing HRESULT. */
    private static long outPointer(long self, int index) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_OUT.invokeExact(slot(obj, index), obj, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code ULONG f(this)}: no out-parameter, no error channel, and therefore no arena. */
    private static int u32(long self, int index) {
        MemorySegment obj = com(self);
        try {
            return (int) U32_THIS.invokeExact(slot(obj, index), obj);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Shaping: IDWriteTextAnalyzer, IDWriteTextFormat, IDWriteTextLayout
     *
     * The shaping half of the port. Every call here is a COM vtable dispatch on a pointer some
     * earlier call produced, so nothing new is bound by name. Slot numbers come from a mechanical
     * count over the Windows SDK header, never from reading: the arithmetic that
     * puts IDWriteTextLayout::Draw at 58 is 3 (IUnknown) + 25 (IDWriteTextFormat's own methods,
     * dwrite.h:2062-2289) + 30 (Draw's ordinal in IDWriteTextLayout), and a reader cannot check that
     * by eye. Both ends of it are pinned behaviourally by DWLayoutTest: the format accessors at
     * slots 20-27 answer the values CreateTextFormat was given, and GetMaxWidth at 42 answers the
     * width CreateTextLayout was given - which can only be true if IDWriteTextFormat really ends at
     * 27 and IDWriteTextLayout really begins at 28.
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteFactory::CreateTextFormat}, ord 12 of 21 (dwrite.h:4938). */
    private static final int FACTORY_CREATE_TEXT_FORMAT = 15;

    /** {@code IDWriteFactory::CreateTextLayout}, ord 15 (dwrite.h:4985). */
    private static final int FACTORY_CREATE_TEXT_LAYOUT = 18;

    /** {@code IDWriteFactory::CreateTextAnalyzer}, ord 18 (dwrite.h:5052). */
    private static final int FACTORY_CREATE_TEXT_ANALYZER = 21;

    /* IDWriteTextAnalyzer (dwrite.h:2738), a direct subinterface of IUnknown, so base 3. */
    private static final int ANALYZER_ANALYZE_SCRIPT = 3;               // ord 0, dwrite.h:2752
    private static final int ANALYZER_ANALYZE_BIDI = 4;                 // ord 1, dwrite.h:2779
    private static final int ANALYZER_ANALYZE_NUMBER_SUBSTITUTION = 5;  // ord 2, dwrite.h:2807
    private static final int ANALYZER_ANALYZE_LINE_BREAKPOINTS = 6;     // ord 3, dwrite.h:2842
    private static final int ANALYZER_GET_GLYPHS = 7;                   // ord 4, dwrite.h:2895
    private static final int ANALYZER_GET_GLYPH_PLACEMENTS = 8;         // ord 5, dwrite.h:2947

    /* IDWriteTextFormat (dwrite.h:2053), base 3, 25 own methods: slots 3..27 inclusive. */
    private static final int TEXT_FORMAT_GET_FONT_FAMILY_NAME_LENGTH = 20;  // ord 17, dwrite.h:2241
    private static final int TEXT_FORMAT_GET_FONT_FAMILY_NAME = 21;         // ord 18, dwrite.h:2251
    private static final int TEXT_FORMAT_GET_FONT_WEIGHT = 22;              // ord 19, dwrite.h:2259
    private static final int TEXT_FORMAT_GET_FONT_STYLE = 23;               // ord 20, dwrite.h:2264
    private static final int TEXT_FORMAT_GET_FONT_STRETCH = 24;             // ord 21, dwrite.h:2269
    private static final int TEXT_FORMAT_GET_FONT_SIZE = 25;                // ord 22, dwrite.h:2274
    private static final int TEXT_FORMAT_GET_LOCALE_NAME_LENGTH = 26;       // ord 23, dwrite.h:2279
    private static final int TEXT_FORMAT_GET_LOCALE_NAME = 27;              // ord 24, dwrite.h:2289

    /* IDWriteTextLayout (dwrite.h:3775), base IDWriteTextFormat = 28. */
    private static final int TEXT_LAYOUT_GET_MAX_WIDTH = 42;                // ord 14, dwrite.h:3967
    private static final int TEXT_LAYOUT_GET_MAX_HEIGHT = 43;               // ord 15, dwrite.h:3972
    private static final int TEXT_LAYOUT_DRAW = 58;                         // ord 30, dwrite.h:4200

    /**
     * {@code DWRITE_SCRIPT_ANALYSIS} (dwrite.h:2351): {@code UINT16 script} then the 4-byte enum
     * {@code DWRITE_SCRIPT_SHAPES shapes}, so two bytes of padding and a size of 8. In and out of
     * {@code GetGlyphs}, {@code GetGlyphPlacements} and {@code SetScriptAnalysis}; the Java mirror
     * {@code DWRITE_SCRIPT_ANALYSIS} keeps {@code short} and {@code int}, which is what the JNI
     * field cache read and wrote.
     */
    static final StructLayout DWRITE_SCRIPT_ANALYSIS_LAYOUT = MemoryLayout.structLayout(
            JAVA_SHORT.withName("script"),            // offset 0   UINT16
            MemoryLayout.paddingLayout(2),
            JAVA_INT.withName("shapes"));             // offset 4   enum, byteSize 8

    /**
     * {@code DWRITE_GLYPH_RUN_DESCRIPTION} (dwrite.h:3092): 40 bytes, 8-byte aligned, with padding
     * after {@code stringLength} and again at the end. Read in the {@code DrawGlyphRun} upcall only.
     * {@code clusterMap} is {@code _Field_size_opt_(stringLength)}, so it may be {@code NULL}.
     */
    static final StructLayout DWRITE_GLYPH_RUN_DESCRIPTION_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("localeName"),           // offset 0   WCHAR const*
            ADDRESS.withName("string"),               // offset 8   WCHAR const*
            JAVA_INT.withName("stringLength"),        // offset 16  UINT32
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("clusterMap"),           // offset 24  UINT16 const*, optional
            JAVA_INT.withName("textPosition"),        // offset 32  UINT32
            MemoryLayout.paddingLayout(4));           // byteSize 40

    private static final long DESCRIPTION_STRING_LENGTH =
            offsetOf(DWRITE_GLYPH_RUN_DESCRIPTION_LAYOUT, "stringLength");
    private static final long DESCRIPTION_CLUSTER_MAP =
            offsetOf(DWRITE_GLYPH_RUN_DESCRIPTION_LAYOUT, "clusterMap");
    private static final long DESCRIPTION_TEXT_POSITION =
            offsetOf(DWRITE_GLYPH_RUN_DESCRIPTION_LAYOUT, "textPosition");

    /**
     * {@code HRESULT f(this, WCHAR const* family, IDWriteFontCollection*, DWRITE_FONT_WEIGHT,
     * DWRITE_FONT_STYLE, DWRITE_FONT_STRETCH, FLOAT size, WCHAR const* locale,
     * IDWriteTextFormat** out)}: {@code IDWriteFactory::CreateTextFormat}. The {@code FLOAT} is the
     * sixth argument, so it travels in XMM5 while the arguments on either side of it use RDX..R9
     * and the stack; naming it {@code JAVA_FLOAT} in the descriptor is what says so.
     */
    private static final MethodHandle HR_CREATE_TEXT_FORMAT = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_FLOAT, ADDRESS,
            ADDRESS));

    /**
     * {@code HRESULT f(this, WCHAR const* string, UINT32 stringLength, IDWriteTextFormat*,
     * FLOAT maxWidth, FLOAT maxHeight, IDWriteTextLayout** out)}:
     * {@code IDWriteFactory::CreateTextLayout}. Note what is <em>not</em> here: a start offset.
     * See {@link #createTextLayout}.
     */
    private static final MethodHandle HR_CREATE_TEXT_LAYOUT = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_FLOAT, JAVA_FLOAT, ADDRESS));

    /**
     * {@code HRESULT f(this, IDWriteTextAnalysisSource*, UINT32 textPosition, UINT32 textLength,
     * IDWriteTextAnalysisSink*)}: all four {@code IDWriteTextAnalyzer::Analyze*} methods share it.
     */
    private static final MethodHandle HR_ANALYZE = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));

    /** {@code IDWriteTextAnalyzer::GetGlyphs} (dwrite.h:2895): {@code this} plus 17 parameters. */
    private static final MethodHandle HR_GET_GLYPHS = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS,
            ADDRESS,        // WCHAR const* textString
            JAVA_INT,       // UINT32 textLength
            ADDRESS,        // IDWriteFontFace*
            JAVA_INT,       // BOOL isSideways
            JAVA_INT,       // BOOL isRightToLeft
            ADDRESS,        // DWRITE_SCRIPT_ANALYSIS const*
            ADDRESS,        // WCHAR const* localeName, optional
            ADDRESS,        // IDWriteNumberSubstitution*, optional
            ADDRESS,        // DWRITE_TYPOGRAPHIC_FEATURES const**, optional
            ADDRESS,        // UINT32 const* featureRangeLengths, optional
            JAVA_INT,       // UINT32 featureRanges
            JAVA_INT,       // UINT32 maxGlyphCount
            ADDRESS,        // UINT16* clusterMap, out
            ADDRESS,        // DWRITE_SHAPING_TEXT_PROPERTIES* textProps, out
            ADDRESS,        // UINT16* glyphIndices, out
            ADDRESS,        // DWRITE_SHAPING_GLYPH_PROPERTIES* glyphProps, out
            ADDRESS));      // UINT32* actualGlyphCount, out

    /**
     * {@code IDWriteTextAnalyzer::GetGlyphPlacements} (dwrite.h:2947): {@code this} plus 18
     * parameters. {@code textProps} is {@code _Inout_updates_}, so it is copied back like a real
     * out-parameter; {@code clusterMap}, {@code glyphIndices} and {@code glyphProps} are
     * {@code const} inputs here even though {@code GetGlyphs} filled them.
     */
    private static final MethodHandle HR_GET_GLYPH_PLACEMENTS = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS,
            ADDRESS,        // WCHAR const* textString
            ADDRESS,        // UINT16 const* clusterMap
            ADDRESS,        // DWRITE_SHAPING_TEXT_PROPERTIES* textProps, in out
            JAVA_INT,       // UINT32 textLength
            ADDRESS,        // UINT16 const* glyphIndices
            ADDRESS,        // DWRITE_SHAPING_GLYPH_PROPERTIES const* glyphProps
            JAVA_INT,       // UINT32 glyphCount
            ADDRESS,        // IDWriteFontFace*
            JAVA_FLOAT,     // FLOAT fontEmSize
            JAVA_INT,       // BOOL isSideways
            JAVA_INT,       // BOOL isRightToLeft
            ADDRESS,        // DWRITE_SCRIPT_ANALYSIS const*
            ADDRESS,        // WCHAR const* localeName, optional
            ADDRESS,        // DWRITE_TYPOGRAPHIC_FEATURES const**, optional
            ADDRESS,        // UINT32 const* featureRangeLengths, optional
            JAVA_INT,       // UINT32 featureRanges
            ADDRESS,        // FLOAT* glyphAdvances, out
            ADDRESS));      // DWRITE_GLYPH_OFFSET* glyphOffsets, out

    /**
     * {@code HRESULT f(this, void* clientDrawingContext, IDWriteTextRenderer*, FLOAT originX,
     * FLOAT originY)}: {@code IDWriteTextLayout::Draw} (dwrite.h:4200). The two floats are the third
     * and fourth arguments, so they are in XMM2 and XMM3 while RCX and RDX hold {@code this} and the
     * context - the positional rule, not "floats fill XMM0 upwards".
     */
    private static final MethodHandle HR_DRAW = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_FLOAT, JAVA_FLOAT));

    /** {@code FLOAT f(this)}: GetFontSize, GetMaxWidth, GetMaxHeight - the only float returns here. */
    private static final MethodHandle F32_THIS = virtual(FunctionDescriptor.of(JAVA_FLOAT, ADDRESS));

    /** {@code HRESULT f(this, WCHAR* buffer, UINT32 nameSize)}: GetFontFamilyName, GetLocaleName. */
    private static final MethodHandle HR_BUF_I = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

    /**
     * {@code IDWriteFactory::CreateTextAnalyzer} ({@code directwrite.cpp:1831-1837}): the analyzer,
     * or {@code 0} on a failing HRESULT, which {@code IDWriteFactory.CreateTextAnalyzer} turns into
     * {@code null} and {@code DWGlyphLayout} into an unshaped run.
     */
    static long createTextAnalyzer(long self) {
        return outPointer(self, FACTORY_CREATE_TEXT_ANALYZER);
    }

    /**
     * {@code IDWriteFactory::CreateTextFormat} ({@code directwrite.cpp:1839-1861}).
     * <p>
     * Both names arrive as {@code char[]} with the terminator the caller appended
     * ({@code IDWriteFactory.java:51,57}) and cross verbatim through {@link #wide}:
     * {@code allocateFrom(String, UTF_16LE)} would run a {@code CharsetEncoder} and replace an
     * unpaired surrogate with U+FFFD, and a family name is arbitrary text out of a font file. A
     * {@code null} name becomes {@code NULL}, as the C's {@code if (arg1)} guard left it -
     * DirectWrite documents both as {@code _In_z_}, so that is a fault in either path and is
     * unreachable from {@code DWGlyphLayout}, which always has a family and always passes
     * {@code "en-us"}.
     */
    static long createTextFormat(long self, char[] fontFamily, long fontCollection, int fontWeight,
                                 int fontStyle, int fontStretch, float fontSize, char[] localeName) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_CREATE_TEXT_FORMAT.invokeExact(
                    slot(obj, FACTORY_CREATE_TEXT_FORMAT), obj, wide(scratch, fontFamily),
                    com(fontCollection), fontWeight, fontStyle, fontStretch, fontSize,
                    wide(scratch, localeName), out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteFactory::CreateTextLayout} ({@code directwrite.cpp:1892-1916}).
     * <p>
     * <b>The layout is built over {@code text[start .. start+count)}, not over {@code text}.</b>
     * DirectWrite's own signature has no start offset - it takes a pointer and a length - so the C
     * added {@code start} to the pinned array before calling ({@code directwrite.cpp:1904}:
     * {@code const WCHAR* text = (const WCHAR*)(lparg1 + start)}). Dropping that addition compiles,
     * links, returns {@code S_OK} and produces glyphs; it just produces the glyphs of the wrong
     * characters, and only for runs that do not start at 0. {@code DWGlyphLayout.renderShape}
     * passes {@code run.getStart()} ({@code DWGlyphLayout.java:359}), which is non-zero for every
     * run after the first, so this is the defect these shaping bindings exist to avoid. It is proved by
     * {@code DWLayoutTest.createTextLayoutHonoursTheStartOffset}, which shapes the same three
     * characters out of two arrays that agree only at the offset, and by every {@code Draw} parity
     * row, all of which use a non-zero start.
     * <p>
     * The three guards are the C's, in the C's order ({@code directwrite.cpp:1900-1902}), and a
     * failure returns {@code 0} with {@code hr} still {@code E_FAIL}, which is what the {@code fail}
     * label did. A {@code null} array returns {@code 0} here; the C reached
     * {@code GetArrayLength(NULL)}, which aborts the JVM, and no caller can reach it.
     */
    static long createTextLayout(long self, char[] text, int start, int count, long textFormat,
                                 float maxWidth, float maxHeight) {
        if (text == null || start < 0 || count < 0) {
            return 0L;
        }
        if (count > Integer.MAX_VALUE - start || start + count > text.length) {
            return 0L;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment buffer = scratch.allocateFrom(JAVA_CHAR, text);
            MemorySegment obj = com(self);
            int hr = (int) HR_CREATE_TEXT_LAYOUT.invokeExact(
                    slot(obj, FACTORY_CREATE_TEXT_LAYOUT), obj,
                    buffer.asSlice((long) start * Character.BYTES), count, com(textFormat),
                    maxWidth, maxHeight, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteTextAnalyzer::AnalyzeScript} ({@code directwrite.cpp:2212-2220}), slot 3.
     * {@code source} and {@code sink} are two interface pointers of one object - the C passed the
     * same {@code JFXTextAnalysisSink*} twice and let the compiler adjust it to each base
     * ({@code directwrite.cpp:936} declares the bases in the order sink, source, so the sink vtable
     * is at offset 0 and the source vtable at offset 8). The Java object of
     * {@link #newAnalysisSink} keeps that shape, so {@link #analysisSinkPointer} and
     * {@link #analysisSourcePointer} differ by 8.
     */
    static int analyzeScript(long self, long source, int textPosition, int textLength, long sink) {
        return analyze(self, ANALYZER_ANALYZE_SCRIPT, source, textPosition, textLength, sink);
    }

    /**
     * {@code IDWriteTextAnalyzer::AnalyzeBidi}, slot 4. Never a JNI native: JavaFX computes bidi
     * levels itself. It is bound because it is the only way to make DirectWrite call
     * {@code SetBidiLevel} - sink slot 5, and the only slot in the package taking {@code UINT8}
     * arguments - which is otherwise a vtable entry no test could reach.
     */
    static int analyzeBidi(long self, long source, int textPosition, int textLength, long sink) {
        return analyze(self, ANALYZER_ANALYZE_BIDI, source, textPosition, textLength, sink);
    }

    /** {@code IDWriteTextAnalyzer::AnalyzeNumberSubstitution}, slot 5; drives sink slot 6. */
    static int analyzeNumberSubstitution(long self, long source, int textPosition, int textLength,
                                         long sink) {
        return analyze(self, ANALYZER_ANALYZE_NUMBER_SUBSTITUTION, source, textPosition, textLength,
                sink);
    }

    /** {@code IDWriteTextAnalyzer::AnalyzeLineBreakpoints}, slot 6; drives sink slot 4. */
    static int analyzeLineBreakpoints(long self, long source, int textPosition, int textLength,
                                      long sink) {
        return analyze(self, ANALYZER_ANALYZE_LINE_BREAKPOINTS, source, textPosition, textLength,
                sink);
    }

    private static int analyze(long self, int index, long source, int textPosition, int textLength,
                               long sink) {
        MemorySegment obj = com(self);
        try {
            return (int) HR_ANALYZE.invokeExact(slot(obj, index), obj, com(source), textPosition,
                    textLength, com(sink));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteTextAnalyzer::GetGlyphs} ({@code directwrite.cpp:2222-2284}), slot 7.
     * <p>
     * The four validations and their order are the C's ({@code directwrite.cpp:2249-2252}); on any
     * of them the initial {@code hr = E_FAIL} is returned and DirectWrite is never called, which
     * {@code DWGlyphLayout} reads as "abandon this run" ({@code DWGlyphLayout.java:131-134}). The
     * text pointer is {@code text + textStart} exactly as at {@code directwrite.cpp:2253}, while
     * every other array is passed from index 0 - {@code clusterMap} and {@code textProps} are sized
     * by {@code textLength}, not by the whole string.
     * <p>
     * Arrays cross through a confined arena and the five out-parameters are copied back after the
     * call, which is what {@code Release*ArrayElements(..., 0)} did ({@code :2274-2282}). The four
     * {@code const} inputs are not copied back: DirectWrite cannot have changed them, so the C's
     * copy-back of those was a no-op. {@code localeName} is optional and {@code DWGlyphLayout}
     * always passes {@code null}, so DirectWrite picks the script default.
     * <p>
     * {@code E_NOT_SUFFICIENT_BUFFER} (0x8007007A) is expected traffic here, not a failure: the
     * caller doubles {@code maxGlyphCount} and calls again ({@code DWGlyphLayout.java:121-129}).
     */
    static int getGlyphs(long self, char[] textString, int textStart, int textLength, long fontFace,
                         boolean isSideways, boolean isRightToLeft,
                         DWRITE_SCRIPT_ANALYSIS scriptAnalysis, char[] localeName,
                         long numberSubstitution, long[] features, int[] featureRangeLengths,
                         int featureRanges, int maxGlyphCount, short[] clusterMap, short[] textProps,
                         short[] glyphIndices, short[] glyphProps, int[] actualGlyphCount) {
        if (textStart < 0 || textString == null) {
            return E_FAIL;
        }
        if (textLength <= 0 || textLength > textString.length) {
            return E_FAIL;
        }
        if (textStart > textString.length - textLength) {
            return E_FAIL;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment text = scratch.allocateFrom(JAVA_CHAR, textString);
            MemorySegment cluster = shorts(scratch, clusterMap);
            MemorySegment props = shorts(scratch, textProps);
            MemorySegment indices = shorts(scratch, glyphIndices);
            MemorySegment glyphProperties = shorts(scratch, glyphProps);
            MemorySegment count = ints(scratch, actualGlyphCount);
            MemorySegment obj = com(self);
            int hr = (int) HR_GET_GLYPHS.invokeExact(slot(obj, ANALYZER_GET_GLYPHS), obj,
                    text.asSlice((long) textStart * Character.BYTES), textLength, com(fontFace),
                    isSideways ? 1 : 0, isRightToLeft ? 1 : 0,
                    scriptAnalysis(scratch, scriptAnalysis), wide(scratch, localeName),
                    com(numberSubstitution), longs(scratch, features),
                    ints(scratch, featureRangeLengths), featureRanges, maxGlyphCount, cluster, props,
                    indices, glyphProperties, count);
            copyBack(cluster, clusterMap);
            copyBack(props, textProps);
            copyBack(indices, glyphIndices);
            copyBack(glyphProperties, glyphProps);
            copyBack(count, actualGlyphCount);
            return hr;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteTextAnalyzer::GetGlyphPlacements} ({@code directwrite.cpp:2286-2355}), slot 8.
     * The same four validations, the same {@code text + textStart}, and three arrays copied back:
     * {@code textProps} (declared {@code _Inout_updates_}), {@code glyphAdvances} and
     * {@code glyphOffsets}. {@code glyphOffsets} is {@code DWRITE_GLYPH_OFFSET*} - two floats per
     * glyph - and the caller's {@code float[]} is twice {@code glyphCount} long, which is why it
     * needs no conversion in either direction.
     */
    static int getGlyphPlacements(long self, char[] textString, short[] clusterMap, short[] textProps,
                                  int textStart, int textLength, short[] glyphIndices,
                                  short[] glyphProps, int glyphCount, long fontFace, float fontEmSize,
                                  boolean isSideways, boolean isRightToLeft,
                                  DWRITE_SCRIPT_ANALYSIS scriptAnalysis, char[] localeName,
                                  long[] features, int[] featureRangeLengths, int featureRanges,
                                  float[] glyphAdvances, float[] glyphOffsets) {
        if (textStart < 0 || textString == null) {
            return E_FAIL;
        }
        if (textLength <= 0 || textLength > textString.length) {
            return E_FAIL;
        }
        if (textStart > textString.length - textLength) {
            return E_FAIL;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment text = scratch.allocateFrom(JAVA_CHAR, textString);
            MemorySegment props = shorts(scratch, textProps);
            MemorySegment advances = floats(scratch, glyphAdvances);
            MemorySegment offsets = floats(scratch, glyphOffsets);
            MemorySegment obj = com(self);
            int hr = (int) HR_GET_GLYPH_PLACEMENTS.invokeExact(
                    slot(obj, ANALYZER_GET_GLYPH_PLACEMENTS), obj,
                    text.asSlice((long) textStart * Character.BYTES), shorts(scratch, clusterMap),
                    props, textLength, shorts(scratch, glyphIndices), shorts(scratch, glyphProps),
                    glyphCount, com(fontFace), fontEmSize, isSideways ? 1 : 0, isRightToLeft ? 1 : 0,
                    scriptAnalysis(scratch, scriptAnalysis), wide(scratch, localeName),
                    longs(scratch, features), ints(scratch, featureRangeLengths), featureRanges,
                    advances, offsets);
            copyBack(props, textProps);
            copyBack(advances, glyphAdvances);
            copyBack(offsets, glyphOffsets);
            return hr;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IDWriteTextLayout::Draw} ({@code directwrite.cpp:2357-2362}), slot 58: the call that
     * drives the renderer. {@code DWGlyphLayout} always passes {@code (0, renderer, 0, 0)}
     * ({@code DWGlyphLayout.java:366}), but the origin is a real parameter here and the tests move
     * it, because the difference between two draws at different origins is the sharpest available
     * proof that the two {@code FLOAT}s reach XMM2 and XMM3 outbound and come back out of XMM2 and
     * XMM3 in the {@code DrawGlyphRun} upcall.
     */
    static int draw(long self, long clientDrawingContext, long renderer, float originX,
                    float originY) {
        MemorySegment obj = com(self);
        try {
            return (int) HR_DRAW.invokeExact(slot(obj, TEXT_LAYOUT_DRAW), obj,
                    com(clientDrawingContext), com(renderer), originX, originY);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* The format and layout accessors below were never JNI natives and have no production caller.
     * They are here for the reason the font-face bindings bind GetGlyphCount and GetMetrics: they are the only
     * behavioural proof of the slot arithmetic around them. IDWriteTextFormat has 25 own methods, so
     * it must end at 27 and IDWriteTextLayout must begin at 28; reading back the six values
     * CreateTextFormat was given, from slots 20-27, and the maximum width CreateTextLayout was
     * given, from slot 42 of the layout, is what makes Draw at 58 more than an assertion. Each is
     * about six lines and none is reachable from main code. */

    /** {@code IDWriteTextFormat::GetFontWeight}, slot 22 (dwrite.h:2259). */
    static int getTextFormatFontWeight(long self) {
        return u32(self, TEXT_FORMAT_GET_FONT_WEIGHT);
    }

    /** {@code IDWriteTextFormat::GetFontStyle}, slot 23 (dwrite.h:2264). */
    static int getTextFormatFontStyle(long self) {
        return u32(self, TEXT_FORMAT_GET_FONT_STYLE);
    }

    /** {@code IDWriteTextFormat::GetFontStretch}, slot 24 (dwrite.h:2269). */
    static int getTextFormatFontStretch(long self) {
        return u32(self, TEXT_FORMAT_GET_FONT_STRETCH);
    }

    /**
     * {@code IDWriteTextFormat::GetFontSize}, slot 25 (dwrite.h:2274): a {@code FLOAT} return, so
     * the value comes back in XMM0 rather than EAX. Reading it through {@link #U32_THIS} would
     * answer a stale integer instead of failing, which is why the descriptor is separate.
     */
    static float getTextFormatFontSize(long self) {
        return f32(self, TEXT_FORMAT_GET_FONT_SIZE);
    }

    /** {@code IDWriteTextFormat::GetFontFamilyNameLength}, slot 20, in characters without the NUL. */
    static int getTextFormatFontFamilyNameLength(long self) {
        return u32(self, TEXT_FORMAT_GET_FONT_FAMILY_NAME_LENGTH);
    }

    /** {@code IDWriteTextFormat::GetLocaleNameLength}, slot 26, in characters without the NUL. */
    static int getTextFormatLocaleNameLength(long self) {
        return u32(self, TEXT_FORMAT_GET_LOCALE_NAME_LENGTH);
    }

    /**
     * {@code IDWriteTextFormat::GetFontFamilyName}, slot 21 (dwrite.h:2251): the family name of the
     * format, or {@code null} on a failing HRESULT. {@code nameSize} counts the terminator.
     */
    static String getTextFormatFontFamilyName(long self) {
        return textFormatString(self, TEXT_FORMAT_GET_FONT_FAMILY_NAME,
                getTextFormatFontFamilyNameLength(self));
    }

    /** {@code IDWriteTextFormat::GetLocaleName}, slot 27 (dwrite.h:2289). */
    static String getTextFormatLocaleName(long self) {
        return textFormatString(self, TEXT_FORMAT_GET_LOCALE_NAME,
                getTextFormatLocaleNameLength(self));
    }

    private static String textFormatString(long self, int index, int length) {
        if (length < 0) {
            return null;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment buffer = scratch.allocate(JAVA_CHAR, length + 1L);
            MemorySegment obj = com(self);
            int hr = (int) HR_BUF_I.invokeExact(slot(obj, index), obj, buffer, length + 1);
            if (hr < 0) {
                return null;
            }
            char[] chars = buffer.toArray(JAVA_CHAR);
            return new String(chars, 0, length);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code IDWriteTextLayout::GetMaxWidth}, slot 42 (dwrite.h:3967), a {@code FLOAT} return. */
    static float getTextLayoutMaxWidth(long self) {
        return f32(self, TEXT_LAYOUT_GET_MAX_WIDTH);
    }

    /** {@code IDWriteTextLayout::GetMaxHeight}, slot 43 (dwrite.h:3972), a {@code FLOAT} return. */
    static float getTextLayoutMaxHeight(long self) {
        return f32(self, TEXT_LAYOUT_GET_MAX_HEIGHT);
    }

    private static float f32(long self, int index) {
        MemorySegment obj = com(self);
        try {
            return (float) F32_THIS.invokeExact(slot(obj, index), obj);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* Array marshalling. Every one of these answers MemorySegment.NULL for a null array, which is
     * what the C's "if (argN)" guards left the pointer at, and allocates a copy otherwise - the
     * behaviour of GetXxxArrayElements with a copying JVM. copyBack is Release with mode 0. */

    private static MemorySegment shorts(Arena arena, short[] values) {
        return values == null ? MemorySegment.NULL : arena.allocateFrom(JAVA_SHORT, values);
    }

    private static MemorySegment ints(Arena arena, int[] values) {
        return values == null ? MemorySegment.NULL : arena.allocateFrom(JAVA_INT, values);
    }

    private static MemorySegment longs(Arena arena, long[] values) {
        return values == null ? MemorySegment.NULL : arena.allocateFrom(JAVA_LONG, values);
    }

    private static MemorySegment floats(Arena arena, float[] values) {
        return values == null ? MemorySegment.NULL : arena.allocateFrom(JAVA_FLOAT, values);
    }

    private static void copyBack(MemorySegment source, short[] values) {
        if (values != null) {
            MemorySegment.copy(source, JAVA_SHORT, 0, values, 0, values.length);
        }
    }

    private static void copyBack(MemorySegment source, int[] values) {
        if (values != null) {
            MemorySegment.copy(source, JAVA_INT, 0, values, 0, values.length);
        }
    }

    private static void copyBack(MemorySegment source, float[] values) {
        if (values != null) {
            MemorySegment.copy(source, JAVA_FLOAT, 0, values, 0, values.length);
        }
    }

    /**
     * The 8-byte {@code DWRITE_SCRIPT_ANALYSIS} the two shaping calls read, or {@code NULL} for a
     * {@code null} mirror - which is what {@code getDWRITE_SCRIPT_ANALYSISFields} returned for a
     * {@code null} object ({@code directwrite.cpp:2240}) and what DirectWrite then rejected.
     * {@code shapes} carries the composite slot number here rather than a
     * {@code DWRITE_SCRIPT_SHAPES}, a reuse of the field that {@code DWGlyphLayout.java:114} makes
     * and this layer must pass through untouched.
     */
    private static MemorySegment scriptAnalysis(Arena arena, DWRITE_SCRIPT_ANALYSIS holder) {
        if (holder == null) {
            return MemorySegment.NULL;
        }
        MemorySegment analysis = arena.allocate(DWRITE_SCRIPT_ANALYSIS_LAYOUT);
        analysis.set(JAVA_SHORT, 0, holder.script);
        analysis.set(JAVA_INT, 4, holder.shapes);
        return analysis;
    }

    /* ---------------------------------------------------------------------------------------------
     * The text analysis sink and source: one COM object with two interfaces, implemented in Java
     *
     * IDWriteTextAnalyzer::AnalyzeScript reports its results by calling back, so it needs an object
     * exposing IDWriteTextAnalysisSink, and it reads its input by calling back, so it needs one
     * exposing IDWriteTextAnalysisSource. JavaFX passes the same object as both
     * (IDWriteTextAnalyzer.java:34). The C did that with C++ multiple inheritance
     * (directwrite.cpp:936: "public IDWriteTextAnalysisSink, public IDWriteTextAnalysisSource"),
     * which gives the object two vtable pointers, the sink's at offset 0 and the source's at offset
     * 8, and makes the compiler adjust "this" by 8 at every source call. Java builds the same shape
     * by hand:
     *
     *     +0   const void* vtblSink     -> 7 upcall stubs, Arena.global()
     *     +8   const void* vtblSource   -> 8 upcall stubs, Arena.global()
     *     +16  int64_t     id           -> registry key, never a Java reference
     *
     * so the sink stub finds its id at self+16 and the source stub at self+8. Nothing DirectWrite
     * receives points at a Java object, and no stub dereferences a pointer it has not bounded.
     *
     * The two vtables are process-wide: their targets are static methods, so there is one table per
     * class, built once, and the per-instance cost is 24 bytes plus the two text copies. The object
     * itself lives in a confined arena for exactly as long as the caller keeps it, and dispose is
     * mandatory - see disposeAnalysisSink.
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteTextAnalysisSink} (dwrite.h:2657). */
    static final String IID_IDWRITE_TEXT_ANALYSIS_SINK = "5810cd44-0ca0-4701-b3fa-bec5182ae4f6";

    /** {@code IDWriteTextAnalysisSource} (dwrite.h:2548). */
    static final String IID_IDWRITE_TEXT_ANALYSIS_SOURCE = "688e1a58-5094-47c8-adc8-fbcea60ae92b";

    /* IDWriteTextAnalysisSink (dwrite.h:2657), base IUnknown = 3, four own methods. */
    private static final int ANALYSIS_SINK_SET_SCRIPT_ANALYSIS = 3;        // ord 0, dwrite.h:2668
    private static final int ANALYSIS_SINK_SET_LINE_BREAKPOINTS = 4;       // ord 1, dwrite.h:2684
    private static final int ANALYSIS_SINK_SET_BIDI_LEVEL = 5;             // ord 2, dwrite.h:2704
    private static final int ANALYSIS_SINK_SET_NUMBER_SUBSTITUTION = 6;    // ord 3, dwrite.h:2728
    private static final int ANALYSIS_SINK_SLOT_COUNT = 7;

    /* IDWriteTextAnalysisSource (dwrite.h:2548), base IUnknown = 3, five own methods. */
    private static final int ANALYSIS_SOURCE_GET_TEXT_AT_POSITION = 3;     // ord 0, dwrite.h:2576
    private static final int ANALYSIS_SOURCE_GET_TEXT_BEFORE_POSITION = 4; // ord 1, dwrite.h:2601
    private static final int ANALYSIS_SOURCE_GET_READING_DIRECTION = 5;    // ord 2, dwrite.h:2610
    private static final int ANALYSIS_SOURCE_GET_LOCALE_NAME = 6;          // ord 3, dwrite.h:2624
    private static final int ANALYSIS_SOURCE_GET_NUMBER_SUBSTITUTION = 7;  // ord 4, dwrite.h:2644
    private static final int ANALYSIS_SOURCE_SLOT_COUNT = 8;

    /** The 24-byte object of the comment above. */
    static final StructLayout ANALYSIS_SINK_OBJECT_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("vtblSink"),             // offset 0
            ADDRESS.withName("vtblSource"),           // offset 8
            JAVA_LONG.withName("id"));                // offset 16, byteSize 24

    private static final long ANALYSIS_SOURCE_OFFSET =
            offsetOf(ANALYSIS_SINK_OBJECT_LAYOUT, "vtblSource");
    private static final long ANALYSIS_SINK_ID_OFFSET = offsetOf(ANALYSIS_SINK_OBJECT_LAYOUT, "id");

    /**
     * {@code HRESULT QueryInterface(this, REFIID, void** ppvObject)}, slot 0 of every COM interface.
     * The same shape as the geometry sink's, repeated here so each object's table reads in one
     * place; {@link FunctionDescriptor} has value equality, so {@link #virtual} and the upcall
     * linker both see one descriptor.
     */
    private static final FunctionDescriptor COM_QUERY_INTERFACE_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS);

    /** {@code ULONG AddRef(this)} and {@code ULONG Release(this)}, slots 1 and 2. */
    private static final FunctionDescriptor COM_REF_COUNT_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS);

    /**
     * {@code HRESULT f(this, UINT32 textPosition, UINT32 textLength, void const*)}: sink slots 3, 4
     * and 6 - {@code SetScriptAnalysis}, {@code SetLineBreakpoints} and
     * {@code SetNumberSubstitution}.
     */
    private static final FunctionDescriptor SINK_RANGE_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS);

    /**
     * {@code HRESULT SetBidiLevel(this, UINT32, UINT32, UINT8 explicitLevel, UINT8 resolvedLevel)},
     * sink slot 5. The two {@code UINT8}s are the only sub-word integer arguments in the package: on
     * Microsoft x64 they occupy whole argument slots (R9B and the first stack slot) with the upper
     * bits undefined, which is exactly what {@code JAVA_BYTE} in a descriptor means. Widening them
     * to {@code JAVA_INT} would read those undefined bits.
     */
    private static final FunctionDescriptor SINK_SET_BIDI_LEVEL_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_BYTE, JAVA_BYTE);

    /**
     * {@code HRESULT f(this, UINT32 textPosition, void** a, void* b)}: source slots 3, 4, 6 and 7.
     * {@code GetTextAtPosition} and {@code GetTextBeforePosition} take {@code (WCHAR const**, UINT32*)}
     * while {@code GetLocaleName} and {@code GetNumberSubstitution} take {@code (UINT32*, ...**)} -
     * the same ABI shape with the two out-parameters in the opposite order, which is the single
     * easiest thing in these bindings to invert. dwrite.h:2624 and :2644 put the length first.
     */
    private static final FunctionDescriptor SOURCE_QUERY_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    /**
     * {@code DWRITE_READING_DIRECTION GetParagraphReadingDirection(this)}, source slot 5: declared
     * {@code STDMETHOD_(DWRITE_READING_DIRECTION, ...)}, so it returns an enum in EAX and
     * <em>not</em> an HRESULT. A stub that returned {@code S_OK} here would silently report
     * left-to-right for every right-to-left run.
     */
    private static final FunctionDescriptor SOURCE_READING_DIRECTION_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS);

    private static final byte[] IID_ANALYSIS_SINK_BYTES = guidBytes(IID_IDWRITE_TEXT_ANALYSIS_SINK);
    private static final byte[] IID_ANALYSIS_SOURCE_BYTES = guidBytes(IID_IDWRITE_TEXT_ANALYSIS_SOURCE);

    /** Live analysis sinks, keyed by the id in word 3 of the block DirectWrite holds. */
    private static final Map<Long, AnalysisSinkSession> ANALYSIS_SINKS = new ConcurrentHashMap<>();

    /** Ids start at 1, so a zeroed or freed word reads as 0 and misses the registry cleanly. */
    private static final AtomicLong NEXT_ANALYSIS_SINK_ID = new AtomicLong(1);

    /**
     * The Java replacement for {@code OS._NewJFXTextAnalysisSink}
     * ({@code directwrite.cpp:1200-1205}): a 24-byte COM object plus copies of the text and the
     * locale, all in one confined arena.
     * <p>
     * {@code text} is copied from {@code start} for exactly {@code length} UTF-16 units and is
     * <b>not</b> NUL-terminated, while {@code locale} is copied whole including the terminator the
     * caller appended - the two different rules at {@code directwrite.cpp:1040-1044}. A
     * {@code null} text or locale answers {@code 0}, which the peer turns into {@code null} and
     * {@code DWGlyphLayout} into an unshaped run ({@code DWGlyphLayout.java:57-59}).
     * <p>
     * The returned value is a <b>registry id, not a pointer</b>. {@link #analysisSinkPointer} and
     * {@link #analysisSourcePointer} produce the two interface pointers to hand to
     * {@link #analyzeScript}. Like the C object it starts with a reference count of 0
     * ({@code directwrite.cpp:1034}); the caller {@code AddRef}s before handing it over and
     * {@code Release}s afterwards, exactly as {@code DWGlyphLayout.java:60,77} does.
     * <p>
     * <b>{@link #disposeAnalysisSink} is mandatory and must run on this thread</b>, in a
     * {@code finally}: the arena is confined, so nothing else can free it, and unlike the C the
     * object holds a registry entry and a run list rather than 24 bytes. There is deliberately no
     * {@code Cleaner} - a cleaning action runs on the cleaner's own thread and
     * {@code Arena.ofConfined().close()} throws {@code WrongThreadException} there, so a
     * {@code Cleaner} would be a safety net that cannot fire.
     */
    static long newAnalysisSink(char[] text, int start, int length, char[] locale, int direction,
                                long numberSubstitution) {
        return newAnalysisSink(text, start, length, locale, direction, numberSubstitution, false);
    }

    /** As {@link #newAnalysisSink}, but the object records every callback for the tests. */
    static long newRecordingAnalysisSink(char[] text, int start, int length, char[] locale,
                                         int direction, long numberSubstitution) {
        return newAnalysisSink(text, start, length, locale, direction, numberSubstitution, true);
    }

    private static long newAnalysisSink(char[] text, int start, int length, char[] locale,
                                        int direction, long numberSubstitution, boolean recording) {
        if (text == null || locale == null || start < 0 || length < 0) {
            return 0L;
        }
        MemorySegment sinkVtable = AnalysisTables.SINK_VTABLE;
        MemorySegment sourceVtable = AnalysisTables.SOURCE_VTABLE;
        if (sinkVtable.address() == 0 || sourceVtable.address() == 0) {
            return 0L;
        }
        Arena arena = Arena.ofConfined();
        try {
            MemorySegment block = arena.allocate(ANALYSIS_SINK_OBJECT_LAYOUT);
            MemorySegment textCopy = arena.allocate(JAVA_CHAR, length);
            MemorySegment.copy(text, start, textCopy, JAVA_CHAR, 0, length);
            MemorySegment localeCopy = arena.allocateFrom(JAVA_CHAR, locale);
            long id = NEXT_ANALYSIS_SINK_ID.getAndIncrement();
            block.set(ADDRESS, 0, sinkVtable);
            block.set(ADDRESS, ANALYSIS_SOURCE_OFFSET, sourceVtable);
            block.set(JAVA_LONG, ANALYSIS_SINK_ID_OFFSET, id);
            if (numberSubstitution != 0) {
                addRef(numberSubstitution);
            }
            ANALYSIS_SINKS.put(id, new AnalysisSinkSession(arena, block, textCopy, localeCopy, length,
                    direction, numberSubstitution, recording));
            return id;
        } catch (Throwable t) {
            arena.close();
            throw t;
        }
    }

    /** The {@code IDWriteTextAnalysisSink*} of {@code id}, or {@code 0} if it is not live. */
    static long analysisSinkPointer(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        return session == null ? 0L : session.block.address();
    }

    /** The {@code IDWriteTextAnalysisSource*} of {@code id}: the block plus 8, or {@code 0}. */
    static long analysisSourcePointer(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        return session == null ? 0L : session.block.address() + ANALYSIS_SOURCE_OFFSET;
    }

    /**
     * The distance between the two interface pointers of one object, which is the size of a vtable
     * pointer. It is not only this class's own choice: the C++ object had the same shape, and the
     * pointer {@code OS.AnalyzeScript} receives is the <em>class</em> pointer, from which the
     * compiler derived the source pointer by adding exactly this ({@code directwrite.cpp:936},
     * :2216-2219). A test that drives the C++ object through this facade has to make the same
     * adjustment by hand, which is why the constant is readable.
     */
    static long analysisSourceOffset() {
        return ANALYSIS_SOURCE_OFFSET;
    }

    /**
     * {@code IDWriteTextAnalysisSource::GetParagraphReadingDirection}, slot 5, called
     * <em>outbound</em> on any analysis source - the Java-synthesized one or the C++ one. Never a
     * JNI native and no production caller: it is the probe that settles, at runtime rather than by
     * assumption, that a given pointer really is a source and not something 8 bytes away from one.
     * Asking a sink instead lands on {@code SetBidiLevel}, which answers {@code S_OK} (0) and
     * touches nothing, so the probe cannot fault even when its answer is wrong.
     */
    static int getParagraphReadingDirection(long source) {
        MemorySegment obj = com(source);
        try {
            return (int) virtual(SOURCE_READING_DIRECTION_FD)
                    .invokeExact(slot(obj, ANALYSIS_SOURCE_GET_READING_DIRECTION), obj);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code JFXTextAnalysisSink::Next} ({@code directwrite.cpp:1151-1154}): pre-increments from -1
     * and never resets, so a second pass over the same sink yields nothing.
     */
    static boolean analysisSinkNext(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        return session != null && session.next();
    }

    /** {@code GetStart} ({@code :1156-1159}): 0 once the cursor is past the last run. */
    static int analysisSinkGetStart(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        return session == null ? 0 : session.field(0);
    }

    /** {@code GetLength} ({@code :1161-1164}): 0 once the cursor is past the last run. */
    static int analysisSinkGetLength(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        return session == null ? 0 : session.field(1);
    }

    /**
     * {@code GetAnalysis} ({@code :1166-1169}). Out of range the C returned {@code NULL}, which
     * {@code newDWRITE_SCRIPT_ANALYSIS(env, NULL)} still turned into an allocated, <b>zero-filled</b>
     * object rather than {@code null} ({@code directwrite.cpp:384-390}); this does the same.
     */
    static DWRITE_SCRIPT_ANALYSIS analysisSinkGetAnalysis(long id) {
        DWRITE_SCRIPT_ANALYSIS analysis = new DWRITE_SCRIPT_ANALYSIS();
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        if (session != null) {
            analysis.script = (short) session.field(2);
            analysis.shapes = session.field(3);
        }
        return analysis;
    }

    /** {@code AddRef} called from Java: the counter DirectWrite shares, never a native call. */
    static int analysisSinkAddRef(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        return session == null ? 1 : session.addRef();
    }

    /**
     * {@code Release} called from Java. Reaching zero marks the object dead and frees nothing: the
     * arena is the owner and {@link #disposeAnalysisSink} is what closes it. The C deleted itself
     * here ({@code directwrite.cpp:1176-1183}), which is why its callers needed no dispose.
     */
    static int analysisSinkRelease(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        return session == null ? 0 : session.release();
    }

    /**
     * Frees the block, the text and the locale, releases the number substitution the constructor
     * referenced and removes the registry entry. Idempotent, and a no-op for an id that is already
     * gone. Must run on the thread that called {@link #newAnalysisSink}.
     */
    static void disposeAnalysisSink(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.remove(id);
        if (session != null) {
            session.close();
        }
    }

    /** How many analysis sinks are live: the leak check. */
    static int analysisSinkRegistrySize() {
        return ANALYSIS_SINKS.size();
    }

    /** Every callback the object received, in order, when it was made by {@link #newRecordingAnalysisSink}. */
    static List<String> analysisSinkCallbacks(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        return session == null ? List.of() : List.copyOf(session.callbacks);
    }

    /**
     * The IIDs DirectWrite asked this object for, each as "uuid S_OK" or "uuid E_FAIL", so that this is
     * measured rather than assumed: an empty list after a real
     * {@code AnalyzeScript} means DirectWrite never queries the object it was handed. It does query
     * the text renderer - see {@link #textRendererQueriedIids}.
     */
    static List<String> analysisSinkQueriedIids(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        return session == null ? List.of() : List.copyOf(session.queriedIids);
    }

    /**
     * The session behind a sink pointer, or {@code null} when word 3 names no live object. Every
     * target tolerates the miss and answers the C's value rather than dereferencing anything: an
     * object DirectWrite kept a reference to past {@link #disposeAnalysisSink} leaves freed bytes
     * there, and a COM object must not fault its caller.
     */
    private static AnalysisSinkSession analysisSinkSession(MemorySegment self) {
        return analysisSession(self, ANALYSIS_SINK_ID_OFFSET);
    }

    /** The same object reached through its source pointer, where the id is 8 bytes closer. */
    private static AnalysisSinkSession sourceSession(MemorySegment self) {
        return analysisSession(self, ANALYSIS_SINK_ID_OFFSET - ANALYSIS_SOURCE_OFFSET);
    }

    private static AnalysisSinkSession analysisSession(MemorySegment self, long idOffset) {
        try {
            return ANALYSIS_SINKS.get(bounded(self, idOffset + JAVA_LONG.byteSize())
                    .get(JAVA_LONG, idOffset));
        } catch (Throwable t) {
            return null;
        }
    }

    /* The fifteen upcall targets of the sink and the source. None may throw: an exception escaping
     * into a COM frame would unwind native code. None returns a failure the C could not return
     * either - every collector and every query in directwrite.cpp:1060-1149 returns S_OK
     * unconditionally, and the callers (DWGlyphLayout.addTextRun L62-78, renderShape L359-408) have
     * no try/finally, so a Java exception surfacing there would leak two or three COM objects per
     * occurrence and turn "no runs" into a crash of text measurement. A failure is therefore logged
     * and swallowed. */

    private static int onSinkQueryInterface(MemorySegment self, MemorySegment riid, MemorySegment out) {
        return comQueryInterface(self, riid, out, self, analysisSinkSession(self));
    }

    /**
     * The source's {@code QueryInterface} answers with the <b>block</b> pointer, not with
     * {@code self}, for all three IIDs. That is not a slip: {@code JFXTextAnalysisSink::
     * QueryInterface} assigns {@code *ppvObject = this} ({@code directwrite.cpp:1186-1192}), and
     * converting the class pointer to {@code void*} applies no base adjustment, so the C hands back
     * the sink vtable whichever interface asked and whichever IID was requested. Reproducing it
     * keeps this port observably identical to the C; correcting it - answering {@code block + 8} for
     * the source IID - belongs in a separate, behaviour-changing commit, and
     * {@link #analysisSinkQueriedIids} exists to say first whether DirectWrite ever asks at all.
     */
    private static int onSourceQueryInterface(MemorySegment self, MemorySegment riid,
                                              MemorySegment out) {
        MemorySegment block = MemorySegment.ofAddress(self.address() - ANALYSIS_SOURCE_OFFSET);
        return comQueryInterface(self, riid, out, block, sourceSession(self));
    }

    /**
     * The shared body: {@code S_OK} plus an {@code AddRef} for the sink IID, the source IID or
     * {@code IUnknown}, and {@code E_FAIL} with {@code *ppvObject = NULL} for anything else.
     * {@code E_FAIL} rather than {@code E_NOINTERFACE} because that is what the C returned
     * ({@code directwrite.cpp:1194}).
     * <p>
     * An object whose session has gone - a COM client holding a reference past
     * {@link #disposeAnalysisSink} - answers {@code E_FAIL} for every IID rather than handing back a
     * pointer to freed memory. The C's behaviour there was {@code delete this} followed by a virtual
     * call, which is undefined; refusing is the only answer that cannot make things worse.
     */
    private static int comQueryInterface(MemorySegment self, MemorySegment riid, MemorySegment out,
                                         MemorySegment answer, AnalysisSinkSession session) {
        MemorySegment target = MemorySegment.NULL;
        try {
            target = bounded(out, ADDRESS.byteSize());
            byte[] id = bounded(riid, GUID_LAYOUT.byteSize()).toArray(JAVA_BYTE);
            boolean known = session != null
                    && (Arrays.equals(id, IID_ANALYSIS_SINK_BYTES)
                    || Arrays.equals(id, IID_ANALYSIS_SOURCE_BYTES)
                    || Arrays.equals(id, IID_IUNKNOWN_BYTES));
            if (session != null) {
                session.queried(id, known);
            }
            if (known) {
                target.set(ADDRESS, 0, answer);
                session.addRef();
                return S_OK;
            }
            target.set(ADDRESS, 0, MemorySegment.NULL);
            return E_FAIL;
        } catch (Throwable t) {
            logCallbackFailure("IDWriteTextAnalysisSink::QueryInterface", t);
            try {
                if (target.address() != 0) {
                    target.set(ADDRESS, 0, MemorySegment.NULL);
                }
            } catch (Throwable ignored) {
                // the out-pointer itself is unusable; there is nothing further to report to COM
            }
            return E_FAIL;
        }
    }

    private static int onSinkAddRef(MemorySegment self) {
        AnalysisSinkSession session = analysisSinkSession(self);
        return session == null ? 1 : session.addRef();
    }

    private static int onSinkRelease(MemorySegment self) {
        AnalysisSinkSession session = analysisSinkSession(self);
        return session == null ? 0 : session.release();
    }

    private static int onSourceAddRef(MemorySegment self) {
        AnalysisSinkSession session = sourceSession(self);
        return session == null ? 1 : session.addRef();
    }

    private static int onSourceRelease(MemorySegment self) {
        AnalysisSinkSession session = sourceSession(self);
        return session == null ? 0 : session.release();
    }

    /**
     * {@code SetScriptAnalysis} ({@code directwrite.cpp:1060-1070}), sink slot 3: appends one run
     * and copies the 8-byte {@code DWRITE_SCRIPT_ANALYSIS} by value, as {@code run.analysis =
     * *scriptAnalysis} did. This is the only sink method that collects anything, and the run list it
     * builds is what {@code DWGlyphLayout.addTextRun} walks.
     */
    private static int onSetScriptAnalysis(MemorySegment self, int textPosition, int textLength,
                                           MemorySegment scriptAnalysis) {
        AnalysisSinkSession session = analysisSinkSession(self);
        if (session == null) {
            return S_OK;
        }
        try {
            MemorySegment analysis = bounded(scriptAnalysis, DWRITE_SCRIPT_ANALYSIS_LAYOUT.byteSize());
            int script = analysis.get(JAVA_SHORT, 0) & 0xFFFF;
            int shapes = analysis.get(JAVA_INT, 4);
            session.addRun(textPosition, textLength, script, shapes);
        } catch (Throwable t) {
            logCallbackFailure("IDWriteTextAnalysisSink::SetScriptAnalysis", t);
        }
        return S_OK;
    }

    /** {@code SetLineBreakpoints} ({@code :1072-1077}), sink slot 4: collects nothing, always S_OK. */
    private static int onSetLineBreakpoints(MemorySegment self, int textPosition, int textLength,
                                            MemorySegment lineBreakpoints) {
        AnalysisSinkSession session = analysisSinkSession(self);
        if (session != null) {
            session.record("SetLineBreakpoints " + textPosition + " " + textLength);
        }
        return S_OK;
    }

    /**
     * {@code SetBidiLevel} ({@code :1079-1085}), sink slot 5: collects nothing, always S_OK. The two
     * levels are {@code UINT8}; they are recorded unsigned so a test can see them.
     */
    private static int onSetBidiLevel(MemorySegment self, int textPosition, int textLength,
                                      byte explicitLevel, byte resolvedLevel) {
        AnalysisSinkSession session = analysisSinkSession(self);
        if (session != null) {
            session.record("SetBidiLevel " + textPosition + " " + textLength + " "
                    + (explicitLevel & 0xFF) + " " + (resolvedLevel & 0xFF));
        }
        return S_OK;
    }

    /** {@code SetNumberSubstitution} ({@code :1087-1092}), sink slot 6: collects nothing. */
    private static int onSetNumberSubstitution(MemorySegment self, int textPosition, int textLength,
                                               MemorySegment numberSubstitution) {
        AnalysisSinkSession session = analysisSinkSession(self);
        if (session != null) {
            session.record("SetNumberSubstitution " + textPosition + " " + textLength + " "
                    + (numberSubstitution.address() != 0 ? 1 : 0));
        }
        return S_OK;
    }

    /**
     * {@code GetTextAtPosition} ({@code :1095-1107}), source slot 3: the text from
     * {@code textPosition} to the end, or {@code NULL}/0 at or past the end. The comparison is
     * unsigned, as it was between two {@code UINT32}s.
     */
    private static int onGetTextAtPosition(MemorySegment self, int textPosition,
                                           MemorySegment textString, MemorySegment textLength) {
        AnalysisSinkSession session = sourceSession(self);
        try {
            MemorySegment outText = bounded(textString, ADDRESS.byteSize());
            MemorySegment outLength = bounded(textLength, JAVA_INT.byteSize());
            if (session == null || Integer.compareUnsigned(textPosition, session.textLength) >= 0) {
                outText.set(ADDRESS, 0, MemorySegment.NULL);
                outLength.set(JAVA_INT, 0, 0);
            } else {
                outText.set(ADDRESS, 0,
                        session.text.asSlice((long) textPosition * Character.BYTES));
                outLength.set(JAVA_INT, 0, session.textLength - textPosition);
                session.record("GetTextAtPosition " + textPosition);
            }
        } catch (Throwable t) {
            logCallbackFailure("IDWriteTextAnalysisSource::GetTextAtPosition", t);
        }
        return S_OK;
    }

    /**
     * {@code GetTextBeforePosition} ({@code :1109-1121}), source slot 4: the whole prefix, or
     * {@code NULL}/0 when the position is 0 or past the end.
     */
    private static int onGetTextBeforePosition(MemorySegment self, int textPosition,
                                               MemorySegment textString, MemorySegment textLength) {
        AnalysisSinkSession session = sourceSession(self);
        try {
            MemorySegment outText = bounded(textString, ADDRESS.byteSize());
            MemorySegment outLength = bounded(textLength, JAVA_INT.byteSize());
            if (session == null || textPosition == 0
                    || Integer.compareUnsigned(textPosition, session.textLength) > 0) {
                outText.set(ADDRESS, 0, MemorySegment.NULL);
                outLength.set(JAVA_INT, 0, 0);
            } else {
                outText.set(ADDRESS, 0, session.text);
                outLength.set(JAVA_INT, 0, textPosition);
                session.record("GetTextBeforePosition " + textPosition);
            }
        } catch (Throwable t) {
            logCallbackFailure("IDWriteTextAnalysisSource::GetTextBeforePosition", t);
        }
        return S_OK;
    }

    /**
     * {@code GetParagraphReadingDirection} ({@code :1123-1125}), source slot 5: the direction the
     * constructor was given, verbatim. Not an HRESULT - see {@link #SOURCE_READING_DIRECTION_FD}.
     * A registry miss answers {@code DWRITE_READING_DIRECTION_LEFT_TO_RIGHT} (0), the value a
     * freshly zeroed object would have carried.
     */
    private static int onGetParagraphReadingDirection(MemorySegment self) {
        AnalysisSinkSession session = sourceSession(self);
        if (session == null) {
            return 0;
        }
        session.record("GetParagraphReadingDirection");
        return session.readingDirection;
    }

    /**
     * {@code GetLocaleName} ({@code :1127-1135}), source slot 6: the locale copy and
     * {@code textLength - textPosition}, with no bounds check - the C had none, and the
     * {@code fflush(stdout)} at its head is dropped because nothing Java-visible went through the C
     * stdio buffer. <b>The length is written first</b> (dwrite.h:2624).
     */
    private static int onGetLocaleName(MemorySegment self, int textPosition, MemorySegment textLength,
                                       MemorySegment localeName) {
        AnalysisSinkSession session = sourceSession(self);
        try {
            MemorySegment outLength = bounded(textLength, JAVA_INT.byteSize());
            MemorySegment outName = bounded(localeName, ADDRESS.byteSize());
            if (session == null) {
                outName.set(ADDRESS, 0, MemorySegment.NULL);
                outLength.set(JAVA_INT, 0, 0);
            } else {
                outName.set(ADDRESS, 0, session.locale);
                outLength.set(JAVA_INT, 0, session.textLength - textPosition);
                session.record("GetLocaleName " + textPosition);
            }
        } catch (Throwable t) {
            logCallbackFailure("IDWriteTextAnalysisSource::GetLocaleName", t);
        }
        return S_OK;
    }

    /**
     * {@code GetNumberSubstitution} ({@code :1137-1149}), source slot 7: the object the constructor
     * was given, {@code AddRef}ed when it is not {@code NULL} - which it always is, because
     * {@link JFXTextAnalysisSink#create} passes 0, as the C helper it replaced did. Length first
     * here too (dwrite.h:2644).
     */
    private static int onGetNumberSubstitution(MemorySegment self, int textPosition,
                                               MemorySegment textLength,
                                               MemorySegment numberSubstitution) {
        AnalysisSinkSession session = sourceSession(self);
        try {
            MemorySegment outLength = bounded(textLength, JAVA_INT.byteSize());
            MemorySegment outSubstitution = bounded(numberSubstitution, ADDRESS.byteSize());
            if (session == null) {
                outSubstitution.set(ADDRESS, 0, MemorySegment.NULL);
                outLength.set(JAVA_INT, 0, 0);
            } else {
                if (session.numberSubstitution != 0) {
                    addRef(session.numberSubstitution);
                }
                outSubstitution.set(ADDRESS, 0, com(session.numberSubstitution));
                outLength.set(JAVA_INT, 0, session.textLength - textPosition);
                session.record("GetNumberSubstitution " + textPosition);
            }
        } catch (Throwable t) {
            logCallbackFailure("IDWriteTextAnalysisSource::GetNumberSubstitution", t);
        }
        return S_OK;
    }

    /**
     * One {@code AnalyzeScript} object: the arena that owns its three segments, the run list the
     * sink half fills, the reference count DirectWrite shares and the recording the tests read.
     * Nothing here is synchronised, exactly as the {@code std::vector}s were not
     * ({@code directwrite.cpp:1064}): DirectWrite drives the object synchronously on the thread
     * inside {@code AnalyzeScript}, which is the thread that created it.
     */
    private static final class AnalysisSinkSession {

        private static final int MAX_RECORDED = 4096;

        private final Arena arena;
        private final MemorySegment block;
        private final MemorySegment text;
        private final MemorySegment locale;
        private final int textLength;
        private final int readingDirection;
        private final long numberSubstitution;
        private final boolean recording;
        private final List<int[]> runs = new ArrayList<>();
        private final List<String> callbacks = new ArrayList<>();
        private final List<String> queriedIids = new ArrayList<>();
        private int position = -1;
        private int refCount;

        AnalysisSinkSession(Arena arena, MemorySegment block, MemorySegment text, MemorySegment locale,
                            int textLength, int readingDirection, long numberSubstitution,
                            boolean recording) {
            this.arena = arena;
            this.block = block;
            this.text = text;
            this.locale = locale;
            this.textLength = textLength;
            this.readingDirection = readingDirection;
            this.numberSubstitution = numberSubstitution;
            this.recording = recording;
        }

        void addRun(int start, int length, int script, int shapes) {
            runs.add(new int[] { start, length, script, shapes });
            record("SetScriptAnalysis " + start + " " + length + " " + script + " " + shapes);
        }

        boolean next() {
            position++;
            return position < runs.size();
        }

        int field(int which) {
            if (position < 0 || position >= runs.size()) {
                return 0;
            }
            return runs.get(position)[which];
        }

        int addRef() {
            return ++refCount;
        }

        int release() {
            return --refCount;
        }

        void record(String callback) {
            if (recording && callbacks.size() < MAX_RECORDED) {
                callbacks.add(callback);
            }
        }

        void queried(byte[] iid, boolean answered) {
            if (queriedIids.size() < 32) {
                queriedIids.add(uuidText(iid) + (answered ? " S_OK" : " E_FAIL"));
            }
        }

        void close() {
            if (numberSubstitution != 0) {
                DWNative.release(numberSubstitution);
            }
            arena.close();
        }
    }

    /**
     * The two analysis vtables, built once in {@link Arena#global()} - legitimate because the
     * targets are static methods and there is exactly one table per process, not one per object.
     * Held by a nested class so that nothing is created until the first sink is asked for, and
     * {@link #build} cannot throw, so this initializer cannot either: a table that could not be
     * built is {@link MemorySegment#NULL} and {@link #newAnalysisSink} then answers {@code 0}, which
     * is what "DirectWrite unavailable" has always looked like to {@code DWGlyphLayout}.
     */
    private static final class AnalysisTables {

        static final MemorySegment SINK_VTABLE = build(true);
        static final MemorySegment SOURCE_VTABLE = build(false);

        private static MemorySegment build(boolean sink) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                int slots = sink ? ANALYSIS_SINK_SLOT_COUNT : ANALYSIS_SOURCE_SLOT_COUNT;
                MemorySegment table = Arena.global()
                        .allocate(ADDRESS.byteSize() * slots, ADDRESS.byteSize());
                table.setAtIndex(ADDRESS, IUNKNOWN_QUERY_INTERFACE, upcall(lookup,
                        sink ? "onSinkQueryInterface" : "onSourceQueryInterface",
                        COM_QUERY_INTERFACE_FD, int.class, MemorySegment.class, MemorySegment.class,
                        MemorySegment.class));
                table.setAtIndex(ADDRESS, IUNKNOWN_ADD_REF, upcall(lookup,
                        sink ? "onSinkAddRef" : "onSourceAddRef", COM_REF_COUNT_FD, int.class,
                        MemorySegment.class));
                table.setAtIndex(ADDRESS, IUNKNOWN_RELEASE, upcall(lookup,
                        sink ? "onSinkRelease" : "onSourceRelease", COM_REF_COUNT_FD, int.class,
                        MemorySegment.class));
                if (sink) {
                    table.setAtIndex(ADDRESS, ANALYSIS_SINK_SET_SCRIPT_ANALYSIS,
                            upcall(lookup, "onSetScriptAnalysis", SINK_RANGE_FD, int.class,
                                    MemorySegment.class, int.class, int.class, MemorySegment.class));
                    table.setAtIndex(ADDRESS, ANALYSIS_SINK_SET_LINE_BREAKPOINTS,
                            upcall(lookup, "onSetLineBreakpoints", SINK_RANGE_FD, int.class,
                                    MemorySegment.class, int.class, int.class, MemorySegment.class));
                    table.setAtIndex(ADDRESS, ANALYSIS_SINK_SET_BIDI_LEVEL,
                            upcall(lookup, "onSetBidiLevel", SINK_SET_BIDI_LEVEL_FD, int.class,
                                    MemorySegment.class, int.class, int.class, byte.class, byte.class));
                    table.setAtIndex(ADDRESS, ANALYSIS_SINK_SET_NUMBER_SUBSTITUTION,
                            upcall(lookup, "onSetNumberSubstitution", SINK_RANGE_FD, int.class,
                                    MemorySegment.class, int.class, int.class, MemorySegment.class));
                } else {
                    table.setAtIndex(ADDRESS, ANALYSIS_SOURCE_GET_TEXT_AT_POSITION,
                            upcall(lookup, "onGetTextAtPosition", SOURCE_QUERY_FD, int.class,
                                    MemorySegment.class, int.class, MemorySegment.class,
                                    MemorySegment.class));
                    table.setAtIndex(ADDRESS, ANALYSIS_SOURCE_GET_TEXT_BEFORE_POSITION,
                            upcall(lookup, "onGetTextBeforePosition", SOURCE_QUERY_FD, int.class,
                                    MemorySegment.class, int.class, MemorySegment.class,
                                    MemorySegment.class));
                    table.setAtIndex(ADDRESS, ANALYSIS_SOURCE_GET_READING_DIRECTION,
                            upcall(lookup, "onGetParagraphReadingDirection",
                                    SOURCE_READING_DIRECTION_FD, int.class, MemorySegment.class));
                    table.setAtIndex(ADDRESS, ANALYSIS_SOURCE_GET_LOCALE_NAME,
                            upcall(lookup, "onGetLocaleName", SOURCE_QUERY_FD, int.class,
                                    MemorySegment.class, int.class, MemorySegment.class,
                                    MemorySegment.class));
                    table.setAtIndex(ADDRESS, ANALYSIS_SOURCE_GET_NUMBER_SUBSTITUTION,
                            upcall(lookup, "onGetNumberSubstitution", SOURCE_QUERY_FD, int.class,
                                    MemorySegment.class, int.class, MemorySegment.class,
                                    MemorySegment.class));
                }
                return table;
            } catch (Throwable t) {
                return MemorySegment.NULL;
            }
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * The text renderer: the second inbound COM object
     *
     * IDWriteTextLayout::Draw does not return glyphs either; it calls an IDWriteTextRenderer, which
     * derives IDWritePixelSnapping, so one interface and one vtable of ten slots
     * (directwrite.cpp:1231 built the same object with single inheritance). The block is therefore
     * the geometry sink's shape - a vtable pointer and a registry id - and the id is at self+8.
     *
     * This is the object that receives DWRITE_GLYPH_RUN. It is worth being precise about what
     * crosses and how, because it is the only place in the shaping bindings where the argument shapes are not
     * all pointers and integers: DrawGlyphRun takes (this, void* context, FLOAT baselineOriginX,
     * FLOAT baselineOriginY, DWRITE_MEASURING_MODE, DWRITE_GLYPH_RUN const*,
     * DWRITE_GLYPH_RUN_DESCRIPTION const*, IUnknown*), so on Microsoft x64 the two floats are
     * arguments 3 and 4 and travel in XMM2 and XMM3 - by position, not by "the first two floats" -
     * while the enum and the three pointers after them land on the stack at [RSP+0x20] upwards.
     * Neither struct is passed by value: both are const pointers, and nothing in these bindings returns
     * an aggregate. That leaves no unknown classification here; the by-value case the fork has
     * already measured is the geometry sink's D2D1_POINT_2F.
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteTextRenderer} (dwrite.h:3637). */
    static final String IID_IDWRITE_TEXT_RENDERER = "ef8a8135-5cc6-45fe-8825-c5a0724eb819";

    /** {@code IDWritePixelSnapping} (dwrite.h:3586), the base interface of the renderer. */
    static final String IID_IDWRITE_PIXEL_SNAPPING = "eaf3a2da-ecf4-4d24-b644-b34f6842024b";

    /* IDWritePixelSnapping (dwrite.h:3586), base IUnknown = 3, three own methods; then
     * IDWriteTextRenderer (dwrite.h:3637), base IDWritePixelSnapping = 6, four own methods. */
    private static final int RENDERER_IS_PIXEL_SNAPPING_DISABLED = 3;  // ord 0, dwrite.h:3597
    private static final int RENDERER_GET_CURRENT_TRANSFORM = 4;       // ord 1, dwrite.h:3611
    private static final int RENDERER_GET_PIXELS_PER_DIP = 5;          // ord 2, dwrite.h:3626
    private static final int RENDERER_DRAW_GLYPH_RUN = 6;              // ord 0, dwrite.h:3662
    private static final int RENDERER_DRAW_UNDERLINE = 7;              // ord 1, dwrite.h:3699
    private static final int RENDERER_DRAW_STRIKETHROUGH = 8;          // ord 2, dwrite.h:3730
    private static final int RENDERER_DRAW_INLINE_OBJECT = 9;          // ord 3, dwrite.h:3758
    private static final int RENDERER_SLOT_COUNT = 10;

    /** {@code { const void* vtbl; uint64_t id; }} - the same 16 bytes as the geometry sink's block. */
    static final StructLayout TEXT_RENDERER_OBJECT_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("vtbl"),                 // offset 0
            JAVA_LONG.withName("id"));                // offset 8, byteSize 16

    private static final long TEXT_RENDERER_ID_OFFSET = offsetOf(TEXT_RENDERER_OBJECT_LAYOUT, "id");

    /** {@code HRESULT f(this, void* context, void* out)}: the three pixel-snapping queries. */
    private static final FunctionDescriptor RENDERER_QUERY_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS);

    /**
     * {@code HRESULT DrawGlyphRun(this, void* context, FLOAT baselineOriginX, FLOAT baselineOriginY,
     * DWRITE_MEASURING_MODE, DWRITE_GLYPH_RUN const*, DWRITE_GLYPH_RUN_DESCRIPTION const*,
     * IUnknown* effect)}, slot 6.
     */
    private static final FunctionDescriptor RENDERER_DRAW_GLYPH_RUN_FD = FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, JAVA_FLOAT, JAVA_FLOAT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS);

    /**
     * {@code HRESULT f(this, void* context, FLOAT x, FLOAT y, void const* decoration,
     * IUnknown* effect)}: {@code DrawUnderline} and {@code DrawStrikethrough}, slots 7 and 8.
     */
    private static final FunctionDescriptor RENDERER_DECORATION_FD = FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, JAVA_FLOAT, JAVA_FLOAT, ADDRESS, ADDRESS);

    /**
     * {@code HRESULT DrawInlineObject(this, void* context, FLOAT x, FLOAT y, IDWriteInlineObject*,
     * BOOL isSideways, BOOL isRightToLeft, IUnknown* effect)}, slot 9.
     */
    private static final FunctionDescriptor RENDERER_INLINE_OBJECT_FD = FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, JAVA_FLOAT, JAVA_FLOAT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS);

    private static final byte[] IID_TEXT_RENDERER_BYTES = guidBytes(IID_IDWRITE_TEXT_RENDERER);
    private static final byte[] IID_PIXEL_SNAPPING_BYTES = guidBytes(IID_IDWRITE_PIXEL_SNAPPING);

    private static final Map<Long, TextRendererSession> TEXT_RENDERERS = new ConcurrentHashMap<>();

    private static final AtomicLong NEXT_TEXT_RENDERER_ID = new AtomicLong(1);

    /**
     * The Java replacement for {@code OS._NewJFXTextRenderer} ({@code directwrite.cpp:1479-1483}):
     * a 16-byte COM object in its own confined arena, with a reference count of 0 as the C++
     * constructor left it ({@code :1317}). The returned value is a registry id;
     * {@link #textRendererPointer} is the {@code IDWriteTextRenderer*} to pass to {@link #draw}.
     * {@link #disposeTextRenderer} is mandatory and must run on this thread - see
     * {@link #newAnalysisSink} for why there is no {@code Cleaner}.
     */
    static long newTextRenderer() {
        return newTextRenderer(false);
    }

    /** As {@link #newTextRenderer}, but the object records every callback for the tests. */
    static long newRecordingTextRenderer() {
        return newTextRenderer(true);
    }

    private static long newTextRenderer(boolean recording) {
        MemorySegment vtable = RendererTable.VTABLE;
        if (vtable.address() == 0) {
            return 0L;
        }
        Arena arena = Arena.ofConfined();
        try {
            MemorySegment block = arena.allocate(TEXT_RENDERER_OBJECT_LAYOUT);
            long id = NEXT_TEXT_RENDERER_ID.getAndIncrement();
            block.set(ADDRESS, 0, vtable);
            block.set(JAVA_LONG, TEXT_RENDERER_ID_OFFSET, id);
            TEXT_RENDERERS.put(id, new TextRendererSession(arena, block, recording));
            return id;
        } catch (Throwable t) {
            arena.close();
            throw t;
        }
    }

    /** The {@code IDWriteTextRenderer*} of {@code id}, or {@code 0} if it is not live. */
    static long textRendererPointer(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        return session == null ? 0L : session.block.address();
    }

    /** {@code JFXTextRenderer::Next} ({@code directwrite.cpp:1430-1433}). */
    static boolean textRendererNext(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        return session != null && session.next();
    }

    /** {@code GetStart} ({@code :1435-1438}): the <b>description's</b> {@code textPosition}. */
    static int textRendererGetStart(long id) {
        GlyphRunRecord run = currentRun(id);
        return run == null ? 0 : run.textPosition;
    }

    /** {@code GetLength} ({@code :1440-1443}): the description's {@code stringLength}. */
    static int textRendererGetLength(long id) {
        GlyphRunRecord run = currentRun(id);
        return run == null ? 0 : run.stringLength;
    }

    /** {@code GetGlyphCount} ({@code :1445-1448}): the glyph run's {@code glyphCount}. */
    static int textRendererGetGlyphCount(long id) {
        GlyphRunRecord run = currentRun(id);
        return run == null ? 0 : run.glyphCount;
    }

    /** {@code GetTotalGlyphCount} ({@code :1450-1452}): the accumulator, valid before any Next. */
    static int textRendererGetTotalGlyphCount(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        return session == null ? 0 : session.totalGlyphCount;
    }

    /**
     * {@code GetFontFace} ({@code :1454-1457}): the run's {@code IDWriteFontFace*}, <b>borrowed</b>.
     * The C did not {@code AddRef} it and {@code DWGlyphLayout.renderShape} never releases it
     * ({@code DWGlyphLayout.java:377}) - its lifetime is the layout's. Out of range it is 0, which
     * the peer turns into {@code null} and {@code getFontSlot} into slot -1.
     */
    static long textRendererGetFontFace(long id) {
        GlyphRunRecord run = currentRun(id);
        return run == null ? 0L : run.fontFace;
    }

    /**
     * {@code GetGlyphIndices} ({@code directwrite.cpp:1515-1536}). The clamp is the C's, computed
     * from the whole array length: {@code min(glyphCount, glyphs.length - start)}. Each index is
     * merged with {@code slot}, which the caller has already shifted left by 24
     * ({@code DWGlyphLayout.java:380}), and the {@code UINT16} is zero-extended.
     * <p>
     * A {@code null} array or a negative {@code start} or {@code slot} answers 0, as the C's four
     * early returns did. Where the C would have overrun the array - {@code start} past its end made
     * its {@code copiedCount} negative and its {@code UINT32} loop counter unbounded - this copies
     * nothing and answers 0.
     */
    static int textRendererGetGlyphIndices(long id, int[] glyphs, int start, int slot) {
        if (glyphs == null || start < 0 || slot < 0) {
            return 0;
        }
        GlyphRunRecord run = currentRun(id);
        if (run == null || run.glyphIndices == null) {
            return 0;
        }
        int copied = clamp(glyphs.length - start, run.glyphCount);
        for (int i = 0; i < copied; i++) {
            glyphs[i + start] = (run.glyphIndices[i] & 0xFFFF) | slot;
        }
        return copied;
    }

    /** {@code GetGlyphAdvances} ({@code :1538-1558}): the same clamp, one float per glyph. */
    static int textRendererGetGlyphAdvances(long id, float[] advances, int start) {
        if (advances == null || start < 0) {
            return 0;
        }
        GlyphRunRecord run = currentRun(id);
        if (run == null || run.glyphAdvances == null) {
            return 0;
        }
        int copied = clamp(advances.length - start, run.glyphCount);
        System.arraycopy(run.glyphAdvances, 0, advances, start, copied);
        return copied;
    }

    /**
     * {@code GetGlyphOffsets} ({@code :1560-1583}): {@code DWRITE_GLYPH_OFFSET} flattened as
     * {@code advanceOffset, ascenderOffset} per glyph, so the count is {@code glyphCount * 2} and an
     * odd clamped count is rejected with 0 - <b>after</b> the clamp ({@code :1572}), so a caller
     * whose array leaves room for half a pair gets nothing rather than half of one.
     */
    static int textRendererGetGlyphOffsets(long id, float[] offsets, int start) {
        if (offsets == null || start < 0) {
            return 0;
        }
        GlyphRunRecord run = currentRun(id);
        if (run == null || run.glyphOffsets == null) {
            return 0;
        }
        int copied = clamp(offsets.length - start, run.glyphCount * 2);
        if (copied % 2 != 0) {
            return 0;
        }
        System.arraycopy(run.glyphOffsets, 0, offsets, start, copied);
        return copied;
    }

    /**
     * {@code GetClusterMap} ({@code :1585-1610}): clamped by {@code GetLength()}, the description's
     * {@code stringLength}, and shifted by {@code glyphStart} so that the map is relative to the
     * start of the JavaFX run rather than to the glyph run. {@code glyphStart} is truncated to a
     * {@code short} <b>before</b> the addition and the sum truncated again on store - both
     * truncations are the C's ({@code :1606}) and both are reproduced.
     */
    static int textRendererGetClusterMap(long id, short[] clusterMap, int textStart, int glyphStart) {
        if (clusterMap == null || textStart < 0 || glyphStart < 0) {
            return 0;
        }
        GlyphRunRecord run = currentRun(id);
        if (run == null || run.clusterMap == null) {
            return 0;
        }
        int copied = clamp(clusterMap.length - textStart, run.stringLength);
        short shift = (short) glyphStart;
        for (int i = 0; i < copied; i++) {
            clusterMap[i + textStart] = (short) ((run.clusterMap[i] & 0xFFFF) + shift);
        }
        return copied;
    }

    /** {@code copiedCount = length - start > available ? available : length - start}, never negative. */
    private static int clamp(int room, int available) {
        return Math.max(0, Math.min(room, available));
    }

    private static GlyphRunRecord currentRun(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        return session == null ? null : session.current();
    }

    /** {@code AddRef} from Java: the counter DirectWrite shares. */
    static int textRendererAddRef(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        return session == null ? 1 : session.addRef();
    }

    /** {@code Release} from Java: reaching zero marks the object dead and frees nothing. */
    static int textRendererRelease(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        return session == null ? 0 : session.release();
    }

    /** Frees the block and removes the registry entry. Idempotent; must run on the creating thread. */
    static void disposeTextRenderer(long id) {
        TextRendererSession session = TEXT_RENDERERS.remove(id);
        if (session != null) {
            session.arena.close();
        }
    }

    /** How many text renderers are live: the leak check. */
    static int textRendererRegistrySize() {
        return TEXT_RENDERERS.size();
    }

    /** Every callback the renderer received, in order, when it was made recording. */
    static List<String> textRendererCallbacks(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        return session == null ? List.of() : List.copyOf(session.callbacks);
    }

    /**
     * The IIDs DirectWrite asked the renderer for, each with the answer it got. Measured, not
     * assumed: {@code dwrite.dll} 10.0.19041 asks a renderer for {@code IDWriteTextRenderer1}
     * (d3e0e934-22a0-427e-aae4-7d9574b59db1, dwrite_2.h:83) before drawing and falls back to the
     * base interface when that is refused - which the C refused too, from the same {@code else}
     * branch ({@code directwrite.cpp:1422-1425}). So this object really is queried, and answering
     * {@code E_FAIL} for an unknown IID is load-bearing rather than defensive.
     */
    static List<String> textRendererQueriedIids(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        return session == null ? List.of() : List.copyOf(session.queriedIids);
    }

    /**
     * The baseline origin and measuring mode of run {@code index}, which the C dropped on the floor
     * and no accessor could reach: {@code { baselineOriginX bits, baselineOriginY bits,
     * measuringMode, isSideways, bidiLevel, fontEmSize bits }}. Float bits, not floats, so the
     * comparison in a test is exact. This is how the tests show that the two {@code FLOAT}
     * parameters of {@code DrawGlyphRun} are decoded from the right registers: draw the same layout
     * at two origins and the difference is exactly the difference of the origins.
     */
    static long[] textRendererRunGeometry(long id, int index) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        if (session == null || index < 0 || index >= session.runs.size()) {
            return null;
        }
        GlyphRunRecord run = session.runs.get(index);
        return new long[] {
                Float.floatToRawIntBits(run.baselineOriginX),
                Float.floatToRawIntBits(run.baselineOriginY),
                run.measuringMode, run.isSideways, run.bidiLevel,
                Float.floatToRawIntBits(run.fontEmSize) };
    }

    /** How many glyph runs this renderer collected, without moving its cursor. */
    static int textRendererRunCount(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        return session == null ? 0 : session.runs.size();
    }

    private static TextRendererSession rendererSession(MemorySegment self) {
        try {
            return TEXT_RENDERERS.get(bounded(self, TEXT_RENDERER_OBJECT_LAYOUT.byteSize())
                    .get(JAVA_LONG, TEXT_RENDERER_ID_OFFSET));
        } catch (Throwable t) {
            return null;
        }
    }

    /* The ten upcall targets of the renderer. Same rules as the sink's: never throw, never return a
     * failure the C could not return. directwrite.cpp:1326-1399 returns S_OK from all seven of the
     * collectors and queries. */

    /**
     * {@code QueryInterface} ({@code directwrite.cpp:1415-1428}): {@code IDWriteTextRenderer},
     * {@code IDWritePixelSnapping} and {@code IUnknown} all answer with {@code self}, which is
     * correct rather than merely faithful - single inheritance means the three interfaces share one
     * vtable pointer at offset 0. Anything else is {@code E_FAIL} with {@code *ppvObject = NULL}.
     */
    private static int onRendererQueryInterface(MemorySegment self, MemorySegment riid,
                                                MemorySegment out) {
        MemorySegment target = MemorySegment.NULL;
        TextRendererSession session = rendererSession(self);
        try {
            target = bounded(out, ADDRESS.byteSize());
            byte[] id = bounded(riid, GUID_LAYOUT.byteSize()).toArray(JAVA_BYTE);
            boolean known = session != null
                    && (Arrays.equals(id, IID_TEXT_RENDERER_BYTES)
                    || Arrays.equals(id, IID_PIXEL_SNAPPING_BYTES)
                    || Arrays.equals(id, IID_IUNKNOWN_BYTES));
            if (session != null) {
                session.queried(id, known);
            }
            if (known) {
                target.set(ADDRESS, 0, self);
                session.addRef();
                return S_OK;
            }
            target.set(ADDRESS, 0, MemorySegment.NULL);
            return E_FAIL;
        } catch (Throwable t) {
            logCallbackFailure("IDWriteTextRenderer::QueryInterface", t);
            try {
                if (target.address() != 0) {
                    target.set(ADDRESS, 0, MemorySegment.NULL);
                }
            } catch (Throwable ignored) {
                // the out-pointer itself is unusable; there is nothing further to report to COM
            }
            return E_FAIL;
        }
    }

    private static int onRendererAddRef(MemorySegment self) {
        TextRendererSession session = rendererSession(self);
        return session == null ? 1 : session.addRef();
    }

    private static int onRendererRelease(MemorySegment self) {
        TextRendererSession session = rendererSession(self);
        return session == null ? 0 : session.release();
    }

    /**
     * {@code IsPixelSnappingDisabled} ({@code :1376-1382}): writes {@code FALSE}. These three
     * queries are behaviour, not boilerplate - together they tell DirectWrite that positions are
     * snapped, that the transform is the identity and that there is one pixel per DIP, which is what
     * decides the glyph positions the golden pins.
     */
    private static int onIsPixelSnappingDisabled(MemorySegment self, MemorySegment context,
                                                 MemorySegment isDisabled) {
        TextRendererSession session = rendererSession(self);
        try {
            bounded(isDisabled, JAVA_INT.byteSize()).set(JAVA_INT, 0, 0);
            if (session != null) {
                session.record("IsPixelSnappingDisabled");
            }
        } catch (Throwable t) {
            logCallbackFailure("IDWritePixelSnapping::IsPixelSnappingDisabled", t);
        }
        return S_OK;
    }

    /** {@code GetCurrentTransform} ({@code :1384-1391}): the identity {@code DWRITE_MATRIX}. */
    private static int onGetCurrentTransform(MemorySegment self, MemorySegment context,
                                             MemorySegment transform) {
        TextRendererSession session = rendererSession(self);
        try {
            MemorySegment matrix = bounded(transform, DWRITE_MATRIX_LAYOUT.byteSize());
            matrix.setAtIndex(JAVA_FLOAT, 0, 1.0f);
            matrix.setAtIndex(JAVA_FLOAT, 1, 0.0f);
            matrix.setAtIndex(JAVA_FLOAT, 2, 0.0f);
            matrix.setAtIndex(JAVA_FLOAT, 3, 1.0f);
            matrix.setAtIndex(JAVA_FLOAT, 4, 0.0f);
            matrix.setAtIndex(JAVA_FLOAT, 5, 0.0f);
            if (session != null) {
                session.record("GetCurrentTransform");
            }
        } catch (Throwable t) {
            logCallbackFailure("IDWritePixelSnapping::GetCurrentTransform", t);
        }
        return S_OK;
    }

    /** {@code GetPixelsPerDip} ({@code :1393-1399}): 1.0f. */
    private static int onGetPixelsPerDip(MemorySegment self, MemorySegment context,
                                         MemorySegment pixelsPerDip) {
        TextRendererSession session = rendererSession(self);
        try {
            bounded(pixelsPerDip, JAVA_FLOAT.byteSize()).set(JAVA_FLOAT, 0, 1.0f);
            if (session != null) {
                session.record("GetPixelsPerDip");
            }
        } catch (Throwable t) {
            logCallbackFailure("IDWritePixelSnapping::GetPixelsPerDip", t);
        }
        return S_OK;
    }

    /**
     * {@code DrawGlyphRun} ({@code directwrite.cpp:1326-1341}), slot 6: the one callback that
     * collects.
     * <p>
     * The C shallow-copied both structs and kept DirectWrite's pointers, reading the glyph arrays
     * after {@code Draw} had returned - sound only because the {@code IDWriteTextLayout} still owns
     * those buffers until it is released. Java copies the arrays here instead, inside the callback,
     * which is strictly safer and yields the same values. {@code fontFace} is copied as a raw
     * pointer <b>without</b> {@code AddRef}, matching {@code :1454} and the fact that
     * {@code DWGlyphLayout} never releases it.
     * <p>
     * {@code glyphAdvances}, {@code glyphOffsets} and {@code clusterMap} are documented optional
     * ({@code _Field_size_opt_}); the C would have dereferenced NULL in its drains, this stores
     * {@code null} and the matching drain answers 0. The description itself is {@code _In_}, so a
     * NULL there is a fault in the C; here it becomes a run with no cluster map.
     */
    private static int onDrawGlyphRun(MemorySegment self, MemorySegment context, float baselineOriginX,
                                      float baselineOriginY, int measuringMode, MemorySegment glyphRun,
                                      MemorySegment glyphRunDescription, MemorySegment effect) {
        TextRendererSession session = rendererSession(self);
        if (session == null) {
            return S_OK;
        }
        try {
            MemorySegment run = bounded(glyphRun, DWRITE_GLYPH_RUN_LAYOUT.byteSize());
            int glyphCount = Math.max(0, run.get(JAVA_INT, RUN_GLYPH_COUNT));
            long fontFace = run.get(ADDRESS, RUN_FONT_FACE).address();
            float fontEmSize = run.get(JAVA_FLOAT, RUN_FONT_EM_SIZE);
            int isSideways = run.get(JAVA_INT, RUN_IS_SIDEWAYS);
            int bidiLevel = run.get(JAVA_INT, RUN_BIDI_LEVEL);
            short[] glyphIndices = readShorts(run.get(ADDRESS, RUN_GLYPH_INDICES), glyphCount);
            float[] glyphAdvances = readFloats(run.get(ADDRESS, RUN_GLYPH_ADVANCES), glyphCount);
            float[] glyphOffsets = readFloats(run.get(ADDRESS, RUN_GLYPH_OFFSETS), glyphCount * 2);

            int textPosition = 0;
            int stringLength = 0;
            short[] clusterMap = null;
            if (glyphRunDescription.address() != 0) {
                MemorySegment description = bounded(glyphRunDescription,
                        DWRITE_GLYPH_RUN_DESCRIPTION_LAYOUT.byteSize());
                textPosition = description.get(JAVA_INT, DESCRIPTION_TEXT_POSITION);
                stringLength = Math.max(0, description.get(JAVA_INT, DESCRIPTION_STRING_LENGTH));
                clusterMap = readShorts(description.get(ADDRESS, DESCRIPTION_CLUSTER_MAP), stringLength);
            }
            session.add(new GlyphRunRecord(fontFace, fontEmSize, glyphCount, glyphIndices,
                    glyphAdvances, glyphOffsets, isSideways, bidiLevel, textPosition, stringLength,
                    clusterMap, baselineOriginX, baselineOriginY, measuringMode));
        } catch (Throwable t) {
            logCallbackFailure("IDWriteTextRenderer::DrawGlyphRun", t);
        }
        return S_OK;
    }

    /**
     * {@code DrawUnderline} and {@code DrawStrikethrough} ({@code :1343-1361}), slots 7 and 8:
     * {@code S_OK} and nothing collected - JavaFX draws decorations itself. They still occupy their
     * slots, because DirectWrite calls what it likes.
     */
    private static int onDrawDecoration(MemorySegment self, MemorySegment context, float originX,
                                        float originY, MemorySegment decoration, MemorySegment effect) {
        TextRendererSession session = rendererSession(self);
        if (session != null) {
            session.record("DrawDecoration " + Float.toString(originX) + " " + Float.toString(originY));
        }
        return S_OK;
    }

    /** {@code DrawInlineObject} ({@code :1363-1373}), slot 9: {@code S_OK}, nothing collected. */
    private static int onDrawInlineObject(MemorySegment self, MemorySegment context, float originX,
                                          float originY, MemorySegment inlineObject, int isSideways,
                                          int isRightToLeft, MemorySegment effect) {
        TextRendererSession session = rendererSession(self);
        if (session != null) {
            session.record("DrawInlineObject " + Float.toString(originX) + " "
                    + Float.toString(originY) + " " + isSideways + " " + isRightToLeft);
        }
        return S_OK;
    }

    /** {@code count} UINT16s from a possibly NULL pointer, or {@code null} for the NULL. */
    private static short[] readShorts(MemorySegment pointer, int count) {
        if (pointer.address() == 0 || count <= 0) {
            return null;
        }
        return bounded(pointer, (long) count * Short.BYTES).toArray(JAVA_SHORT);
    }

    /** {@code count} FLOATs from a possibly NULL pointer, or {@code null} for the NULL. */
    private static float[] readFloats(MemorySegment pointer, int count) {
        if (pointer.address() == 0 || count <= 0) {
            return null;
        }
        return bounded(pointer, (long) count * Float.BYTES).toArray(JAVA_FLOAT);
    }

    /**
     * One glyph run as {@code DrawGlyphRun} saw it. The C kept {@code DWRITE_GLYPH_RUN} and
     * {@code DWRITE_GLYPH_RUN_DESCRIPTION} by value and their buffers by pointer; this keeps the
     * buffers by value and adds the three arguments the C dropped - the baseline origin and the
     * measuring mode - which are what a test needs to see that the float arguments arrived intact.
     */
    private static final class GlyphRunRecord {

        private final long fontFace;
        private final float fontEmSize;
        private final int glyphCount;
        private final short[] glyphIndices;
        private final float[] glyphAdvances;
        private final float[] glyphOffsets;
        private final int isSideways;
        private final int bidiLevel;
        private final int textPosition;
        private final int stringLength;
        private final short[] clusterMap;
        private final float baselineOriginX;
        private final float baselineOriginY;
        private final int measuringMode;

        GlyphRunRecord(long fontFace, float fontEmSize, int glyphCount, short[] glyphIndices,
                       float[] glyphAdvances, float[] glyphOffsets, int isSideways, int bidiLevel,
                       int textPosition, int stringLength, short[] clusterMap, float baselineOriginX,
                       float baselineOriginY, int measuringMode) {
            this.fontFace = fontFace;
            this.fontEmSize = fontEmSize;
            this.glyphCount = glyphCount;
            this.glyphIndices = glyphIndices;
            this.glyphAdvances = glyphAdvances;
            this.glyphOffsets = glyphOffsets;
            this.isSideways = isSideways;
            this.bidiLevel = bidiLevel;
            this.textPosition = textPosition;
            this.stringLength = stringLength;
            this.clusterMap = clusterMap;
            this.baselineOriginX = baselineOriginX;
            this.baselineOriginY = baselineOriginY;
            this.measuringMode = measuringMode;
        }
    }

    /** One {@code Draw} object: the arena, the runs it collected and the cursor the drains move. */
    private static final class TextRendererSession {

        private static final int MAX_RECORDED = 4096;

        private final Arena arena;
        private final MemorySegment block;
        private final boolean recording;
        private final List<GlyphRunRecord> runs = new ArrayList<>();
        private final List<String> callbacks = new ArrayList<>();
        private final List<String> queriedIids = new ArrayList<>();
        private int position = -1;
        private int totalGlyphCount;
        private int refCount;

        TextRendererSession(Arena arena, MemorySegment block, boolean recording) {
            this.arena = arena;
            this.block = block;
            this.recording = recording;
        }

        void add(GlyphRunRecord run) {
            runs.add(run);
            totalGlyphCount += run.glyphCount;
            record("DrawGlyphRun " + Float.toString(run.baselineOriginX) + " "
                    + Float.toString(run.baselineOriginY) + " " + run.measuringMode + " "
                    + run.glyphCount + " " + run.textPosition + " " + run.stringLength);
        }

        boolean next() {
            position++;
            return position < runs.size();
        }

        GlyphRunRecord current() {
            if (position < 0 || position >= runs.size()) {
                return null;
            }
            return runs.get(position);
        }

        int addRef() {
            return ++refCount;
        }

        int release() {
            return --refCount;
        }

        void record(String callback) {
            if (recording && callbacks.size() < MAX_RECORDED) {
                callbacks.add(callback);
            }
        }

        void queried(byte[] iid, boolean answered) {
            if (queriedIids.size() < 32) {
                queriedIids.add(uuidText(iid) + (answered ? " S_OK" : " E_FAIL"));
            }
        }
    }

    /** The renderer vtable: ten stubs, built once in {@link Arena#global()}, never freed. */
    private static final class RendererTable {

        static final MemorySegment VTABLE = build();

        private static MemorySegment build() {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MemorySegment table = Arena.global()
                        .allocate(ADDRESS.byteSize() * RENDERER_SLOT_COUNT, ADDRESS.byteSize());
                table.setAtIndex(ADDRESS, IUNKNOWN_QUERY_INTERFACE, upcall(lookup,
                        "onRendererQueryInterface", COM_QUERY_INTERFACE_FD, int.class,
                        MemorySegment.class, MemorySegment.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, IUNKNOWN_ADD_REF, upcall(lookup, "onRendererAddRef",
                        COM_REF_COUNT_FD, int.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, IUNKNOWN_RELEASE, upcall(lookup, "onRendererRelease",
                        COM_REF_COUNT_FD, int.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, RENDERER_IS_PIXEL_SNAPPING_DISABLED, upcall(lookup,
                        "onIsPixelSnappingDisabled", RENDERER_QUERY_FD, int.class, MemorySegment.class,
                        MemorySegment.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, RENDERER_GET_CURRENT_TRANSFORM, upcall(lookup,
                        "onGetCurrentTransform", RENDERER_QUERY_FD, int.class, MemorySegment.class,
                        MemorySegment.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, RENDERER_GET_PIXELS_PER_DIP, upcall(lookup,
                        "onGetPixelsPerDip", RENDERER_QUERY_FD, int.class, MemorySegment.class,
                        MemorySegment.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, RENDERER_DRAW_GLYPH_RUN, upcall(lookup, "onDrawGlyphRun",
                        RENDERER_DRAW_GLYPH_RUN_FD, int.class, MemorySegment.class, MemorySegment.class,
                        float.class, float.class, int.class, MemorySegment.class, MemorySegment.class,
                        MemorySegment.class));
                table.setAtIndex(ADDRESS, RENDERER_DRAW_UNDERLINE, upcall(lookup, "onDrawDecoration",
                        RENDERER_DECORATION_FD, int.class, MemorySegment.class, MemorySegment.class,
                        float.class, float.class, MemorySegment.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, RENDERER_DRAW_STRIKETHROUGH, upcall(lookup, "onDrawDecoration",
                        RENDERER_DECORATION_FD, int.class, MemorySegment.class, MemorySegment.class,
                        float.class, float.class, MemorySegment.class, MemorySegment.class));
                table.setAtIndex(ADDRESS, RENDERER_DRAW_INLINE_OBJECT, upcall(lookup,
                        "onDrawInlineObject", RENDERER_INLINE_OBJECT_FD, int.class, MemorySegment.class,
                        MemorySegment.class, float.class, float.class, MemorySegment.class, int.class,
                        int.class, MemorySegment.class));
                return table;
            } catch (Throwable t) {
                return MemorySegment.NULL;
            }
        }
    }

    /** One upcall stub in {@link Arena#global()}: legitimate only because the target is static. */
    @SuppressWarnings("restricted")
    private static MemorySegment upcall(MethodHandles.Lookup lookup, String name,
                                        FunctionDescriptor descriptor, Class<?> returnType,
                                        Class<?>... parameterTypes) throws ReflectiveOperationException {
        MethodHandle target = lookup.findStatic(DWNative.class, name,
                MethodType.methodType(returnType, parameterTypes));
        return LINKER.upcallStub(target, descriptor, Arena.global());
    }

    /** A callback failed. The C could not fail here, so this is logged and swallowed, never thrown. */
    private static void logCallbackFailure(String what, Throwable failure) {
        PlatformLogger logger = Logging.getJavaFXLogger();
        if (logger.isLoggable(PlatformLogger.Level.SEVERE)) {
            logger.severe(what + " failed", failure);
        }
    }

    /** The inverse of {@link #guid}: 16 {@code guiddef.h} bytes back to uuid text, for the QI log. */
    private static String uuidText(byte[] raw) {
        if (raw == null || raw.length != 16) {
            return "?";
        }
        StringBuilder text = new StringBuilder(36);
        text.append(hex(raw[3])).append(hex(raw[2])).append(hex(raw[1])).append(hex(raw[0]));
        text.append('-').append(hex(raw[5])).append(hex(raw[4]));
        text.append('-').append(hex(raw[7])).append(hex(raw[6])).append('-');
        for (int i = 8; i < 16; i++) {
            if (i == 10) {
                text.append('-');
            }
            text.append(hex(raw[i]));
        }
        return text.toString();
    }

    private static String hex(byte value) {
        int unsigned = value & 0xFF;
        return "" + Character.forDigit(unsigned >> 4, 16) + Character.forDigit(unsigned & 0xF, 16);
    }

    /* ---------------------------------------------------------------------------------------------
     * Self-test hooks for the two inbound objects
     *
     * These call the synthesized vtables the way DirectWrite calls them - through the function
     * pointers in the tables, with the same FunctionDescriptors - so a test can check both objects
     * in isolation, before any font is involved. They prove the tables are well formed, that every
     * slot holds the method it should and that the out-parameter orders are right. What they cannot
     * prove is the Microsoft x64 register assignment, because both ends are the same linker; that
     * proof is DirectWrite itself calling these objects during AnalyzeScript and Draw, and the
     * parity of the results against the C objects.
     * ------------------------------------------------------------------------------------------- */

    /**
     * Drives {@code QueryInterface}, {@code AddRef} and {@code Release} through both vtables of an
     * analysis sink.
     *
     * @return sixteen values, or {@code null} when the object is not live:
     *         {@code 0,1} QueryInterface(sink IID) on the sink pointer: HRESULT, 1 if it wrote the
     *         block; {@code 2,3} the same for the source IID; {@code 4,5} for {@code IUnknown};
     *         {@code 6,7} for an unrelated IID, where 7 is 1 if it wrote NULL;
     *         {@code 8,9} QueryInterface(source IID) on the <em>source</em> pointer: HRESULT and 1
     *         if it wrote the block - the C's multiple-inheritance behaviour, reproduced;
     *         {@code 10,11} the same for the sink IID on the source pointer;
     *         {@code 12..15} AddRef on the sink, AddRef on the source, Release on the source,
     *         Release on the sink - one counter shared by both vtables, so 1, 2, 1, 0.
     */
    static long[] analysisSinkSelfTest(long id) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        if (session == null) {
            return null;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment sink = session.block;
            MemorySegment source = sink.asSlice(ANALYSIS_SOURCE_OFFSET);
            MethodHandle queryInterface = virtual(COM_QUERY_INTERFACE_FD);
            MethodHandle counted = virtual(COM_REF_COUNT_FD);
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment sinkIid = guid(scratch, IID_IDWRITE_TEXT_ANALYSIS_SINK);
            MemorySegment sourceIid = guid(scratch, IID_IDWRITE_TEXT_ANALYSIS_SOURCE);
            MemorySegment unknownIid = guid(scratch, IID_IUNKNOWN);
            MemorySegment otherIid = guid(scratch, IID_IDWRITE_FACTORY);
            long block = sink.address();
            long[] result = new long[16];

            result[0] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                    slot(sink, IUNKNOWN_QUERY_INTERFACE), sink, sinkIid, out));
            result[1] = out.get(ADDRESS, 0).address() == block ? 1 : 0;
            result[2] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                    slot(sink, IUNKNOWN_QUERY_INTERFACE), sink, sourceIid, out));
            result[3] = out.get(ADDRESS, 0).address() == block ? 1 : 0;
            result[4] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                    slot(sink, IUNKNOWN_QUERY_INTERFACE), sink, unknownIid, out));
            result[5] = out.get(ADDRESS, 0).address() == block ? 1 : 0;
            result[6] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                    slot(sink, IUNKNOWN_QUERY_INTERFACE), sink, otherIid, out));
            result[7] = out.get(ADDRESS, 0).address() == 0 ? 1 : 0;
            result[8] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                    slot(source, IUNKNOWN_QUERY_INTERFACE), source, sourceIid, out));
            result[9] = out.get(ADDRESS, 0).address() == block ? 1 : 0;
            result[10] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                    slot(source, IUNKNOWN_QUERY_INTERFACE), source, sinkIid, out));
            result[11] = out.get(ADDRESS, 0).address() == block ? 1 : 0;

            session.refCount = 0;
            result[12] = (int) counted.invokeExact(slot(sink, IUNKNOWN_ADD_REF), sink);
            result[13] = (int) counted.invokeExact(slot(source, IUNKNOWN_ADD_REF), source);
            result[14] = (int) counted.invokeExact(slot(source, IUNKNOWN_RELEASE), source);
            result[15] = (int) counted.invokeExact(slot(sink, IUNKNOWN_RELEASE), sink);
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Drives all nine collectors and queries of an analysis sink through its two vtables, with
     * arguments a test chooses, and reports what came back.
     *
     * @return twenty-six values, or {@code null} when the object is not live:
     *         {@code 0..3} the HRESULTs of SetScriptAnalysis, SetLineBreakpoints, SetBidiLevel and
     *         SetNumberSubstitution;
     *         {@code 4,5,6} GetTextAtPosition(0): HRESULT, the offset in characters of the pointer
     *         it wrote from the start of the text copy, the length;
     *         {@code 7,8,9} GetTextAtPosition(textLength): HRESULT, 1 if it wrote NULL, the length;
     *         {@code 10,11,12} GetTextBeforePosition(0): HRESULT, 1 if NULL, the length;
     *         {@code 13,14,15} GetTextBeforePosition(2): HRESULT, 1 if it wrote the text base, the
     *         length; {@code 16,17,18} GetTextBeforePosition(textLength + 1): HRESULT, 1 if NULL,
     *         the length; {@code 19} GetParagraphReadingDirection - <em>not</em> an HRESULT;
     *         {@code 20,21,22} GetLocaleName(1): HRESULT, 1 if it wrote the locale base, the length;
     *         {@code 23,24,25} GetNumberSubstitution(1): HRESULT, 1 if it wrote NULL, the length.
     */
    static long[] analysisSinkDriveCallbacks(long id, int position, int length, short script,
                                             int shapes, int explicitLevel, int resolvedLevel) {
        AnalysisSinkSession session = ANALYSIS_SINKS.get(id);
        if (session == null) {
            return null;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment sink = session.block;
            MemorySegment source = sink.asSlice(ANALYSIS_SOURCE_OFFSET);
            MethodHandle range = virtual(SINK_RANGE_FD);
            MethodHandle bidi = virtual(SINK_SET_BIDI_LEVEL_FD);
            MethodHandle query = virtual(SOURCE_QUERY_FD);
            MethodHandle direction = virtual(SOURCE_READING_DIRECTION_FD);
            MemorySegment analysis = scratch.allocate(DWRITE_SCRIPT_ANALYSIS_LAYOUT);
            analysis.set(JAVA_SHORT, 0, script);
            analysis.set(JAVA_INT, 4, shapes);
            MemorySegment breakpoints = scratch.allocate(JAVA_BYTE, Math.max(1, length));
            MemorySegment first = scratch.allocate(ADDRESS);
            MemorySegment second = scratch.allocate(ADDRESS);
            long textBase = session.text.address();
            long localeBase = session.locale.address();
            int textLength = session.textLength;
            long[] result = new long[26];

            result[0] = Integer.toUnsignedLong((int) range.invokeExact(
                    slot(sink, ANALYSIS_SINK_SET_SCRIPT_ANALYSIS), sink, position, length, analysis));
            result[1] = Integer.toUnsignedLong((int) range.invokeExact(
                    slot(sink, ANALYSIS_SINK_SET_LINE_BREAKPOINTS), sink, position, length,
                    breakpoints));
            result[2] = Integer.toUnsignedLong((int) bidi.invokeExact(
                    slot(sink, ANALYSIS_SINK_SET_BIDI_LEVEL), sink, position, length,
                    (byte) explicitLevel, (byte) resolvedLevel));
            result[3] = Integer.toUnsignedLong((int) range.invokeExact(
                    slot(sink, ANALYSIS_SINK_SET_NUMBER_SUBSTITUTION), sink, position, length,
                    MemorySegment.NULL));

            result[4] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(source, ANALYSIS_SOURCE_GET_TEXT_AT_POSITION), source, 0, first, second));
            result[5] = (first.get(ADDRESS, 0).address() - textBase) / Character.BYTES;
            result[6] = second.get(JAVA_INT, 0);
            result[7] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(source, ANALYSIS_SOURCE_GET_TEXT_AT_POSITION), source, textLength, first,
                    second));
            result[8] = first.get(ADDRESS, 0).address() == 0 ? 1 : 0;
            result[9] = second.get(JAVA_INT, 0);

            result[10] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(source, ANALYSIS_SOURCE_GET_TEXT_BEFORE_POSITION), source, 0, first, second));
            result[11] = first.get(ADDRESS, 0).address() == 0 ? 1 : 0;
            result[12] = second.get(JAVA_INT, 0);
            result[13] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(source, ANALYSIS_SOURCE_GET_TEXT_BEFORE_POSITION), source, 2, first, second));
            result[14] = first.get(ADDRESS, 0).address() == textBase ? 1 : 0;
            result[15] = second.get(JAVA_INT, 0);
            result[16] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(source, ANALYSIS_SOURCE_GET_TEXT_BEFORE_POSITION), source, textLength + 1,
                    first, second));
            result[17] = first.get(ADDRESS, 0).address() == 0 ? 1 : 0;
            result[18] = second.get(JAVA_INT, 0);

            result[19] = Integer.toUnsignedLong((int) direction.invokeExact(
                    slot(source, ANALYSIS_SOURCE_GET_READING_DIRECTION), source));

            result[20] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(source, ANALYSIS_SOURCE_GET_LOCALE_NAME), source, 1, first, second));
            result[21] = second.get(ADDRESS, 0).address() == localeBase ? 1 : 0;
            result[22] = first.get(JAVA_INT, 0);
            result[23] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(source, ANALYSIS_SOURCE_GET_NUMBER_SUBSTITUTION), source, 1, first, second));
            result[24] = second.get(ADDRESS, 0).address() == 0 ? 1 : 0;
            result[25] = first.get(JAVA_INT, 0);
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Drives {@code QueryInterface}, {@code AddRef} and {@code Release} through the renderer vtable.
     *
     * @return twelve values, or {@code null} when the object is not live: {@code 0,1} the renderer
     *         IID (HRESULT, 1 if it wrote self), {@code 2,3} the pixel-snapping IID, {@code 4,5}
     *         {@code IUnknown}, {@code 6,7} an unrelated IID where 7 is 1 if it wrote NULL, and
     *         {@code 8..11} AddRef, AddRef, Release, Release.
     */
    static long[] textRendererSelfTest(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        if (session == null) {
            return null;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment renderer = session.block;
            MethodHandle queryInterface = virtual(COM_QUERY_INTERFACE_FD);
            MethodHandle counted = virtual(COM_REF_COUNT_FD);
            MemorySegment out = scratch.allocate(ADDRESS);
            long block = renderer.address();
            long[] result = new long[12];
            String[] iids = { IID_IDWRITE_TEXT_RENDERER, IID_IDWRITE_PIXEL_SNAPPING, IID_IUNKNOWN,
                    IID_IDWRITE_FACTORY };
            for (int i = 0; i < iids.length; i++) {
                MemorySegment iid = guid(scratch, iids[i]);
                result[i * 2] = Integer.toUnsignedLong((int) queryInterface.invokeExact(
                        slot(renderer, IUNKNOWN_QUERY_INTERFACE), renderer, iid, out));
                long written = out.get(ADDRESS, 0).address();
                result[i * 2 + 1] = i == 3 ? (written == 0 ? 1 : 0) : (written == block ? 1 : 0);
            }
            session.refCount = 0;
            result[8] = (int) counted.invokeExact(slot(renderer, IUNKNOWN_ADD_REF), renderer);
            result[9] = (int) counted.invokeExact(slot(renderer, IUNKNOWN_ADD_REF), renderer);
            result[10] = (int) counted.invokeExact(slot(renderer, IUNKNOWN_RELEASE), renderer);
            result[11] = (int) counted.invokeExact(slot(renderer, IUNKNOWN_RELEASE), renderer);
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Drives the three pixel-snapping queries through the renderer vtable and reports what they
     * wrote.
     *
     * @return nine values, or {@code null}: {@code 0,1} IsPixelSnappingDisabled (HRESULT, the BOOL),
     *         {@code 2} GetCurrentTransform HRESULT, {@code 3..8} the six floats of the matrix as
     *         raw bits, {@code 9} GetPixelsPerDip HRESULT and {@code 10} its float as raw bits.
     */
    static long[] textRendererDriveQueries(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        if (session == null) {
            return null;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment renderer = session.block;
            MethodHandle query = virtual(RENDERER_QUERY_FD);
            MemorySegment flag = scratch.allocate(JAVA_INT);
            MemorySegment matrix = scratch.allocate(DWRITE_MATRIX_LAYOUT);
            MemorySegment pixelsPerDip = scratch.allocate(JAVA_FLOAT);
            long[] result = new long[11];
            result[0] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(renderer, RENDERER_IS_PIXEL_SNAPPING_DISABLED), renderer,
                    MemorySegment.NULL, flag));
            result[1] = flag.get(JAVA_INT, 0);
            result[2] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(renderer, RENDERER_GET_CURRENT_TRANSFORM), renderer, MemorySegment.NULL,
                    matrix));
            for (int i = 0; i < 6; i++) {
                result[3 + i] = Float.floatToRawIntBits(matrix.getAtIndex(JAVA_FLOAT, i));
            }
            result[9] = Integer.toUnsignedLong((int) query.invokeExact(
                    slot(renderer, RENDERER_GET_PIXELS_PER_DIP), renderer, MemorySegment.NULL,
                    pixelsPerDip));
            result[10] = Float.floatToRawIntBits(pixelsPerDip.get(JAVA_FLOAT, 0));
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Feeds one synthetic glyph run through the renderer's {@code DrawGlyphRun} stub, so that the
     * clamping, the slot merge and the two truncations of the drains can be tested with values a
     * test chooses rather than with whatever DirectWrite happens to produce. A {@code null} array
     * becomes a {@code NULL} pointer in the struct, which is the optional case the C would have
     * dereferenced.
     *
     * @return the HRESULT the stub returned
     */
    static int textRendererFeedRun(long id, float baselineOriginX, float baselineOriginY,
                                   int measuringMode, long fontFace, float fontEmSize,
                                   short[] glyphIndices, float[] glyphAdvances, float[] glyphOffsets,
                                   short[] clusterMap, int textPosition, int stringLength,
                                   int isSideways, int bidiLevel, boolean withDescription) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        if (session == null) {
            return E_FAIL;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment renderer = session.block;
            MemorySegment run = scratch.allocate(DWRITE_GLYPH_RUN_LAYOUT);
            run.set(ADDRESS, RUN_FONT_FACE, com(fontFace));
            run.set(JAVA_FLOAT, RUN_FONT_EM_SIZE, fontEmSize);
            run.set(JAVA_INT, RUN_GLYPH_COUNT, glyphIndices == null ? 0 : glyphIndices.length);
            run.set(ADDRESS, RUN_GLYPH_INDICES, shorts(scratch, glyphIndices));
            run.set(ADDRESS, RUN_GLYPH_ADVANCES, floats(scratch, glyphAdvances));
            run.set(ADDRESS, RUN_GLYPH_OFFSETS, floats(scratch, glyphOffsets));
            run.set(JAVA_INT, RUN_IS_SIDEWAYS, isSideways);
            run.set(JAVA_INT, RUN_BIDI_LEVEL, bidiLevel);

            MemorySegment description = MemorySegment.NULL;
            if (withDescription) {
                description = scratch.allocate(DWRITE_GLYPH_RUN_DESCRIPTION_LAYOUT);
                description.set(JAVA_INT, DESCRIPTION_STRING_LENGTH, stringLength);
                description.set(JAVA_INT, DESCRIPTION_TEXT_POSITION, textPosition);
                description.set(ADDRESS, DESCRIPTION_CLUSTER_MAP, shorts(scratch, clusterMap));
            }
            MethodHandle drawGlyphRun = virtual(RENDERER_DRAW_GLYPH_RUN_FD);
            return (int) drawGlyphRun.invokeExact(slot(renderer, RENDERER_DRAW_GLYPH_RUN), renderer,
                    MemorySegment.NULL, baselineOriginX, baselineOriginY, measuringMode, run,
                    description, MemorySegment.NULL);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Drives the three slots the renderer ignores - {@code DrawUnderline}, {@code DrawStrikethrough}
     * and {@code DrawInlineObject} - so that they are known to be present, callable and harmless.
     *
     * @return their three HRESULTs, or {@code null} when the object is not live
     */
    static long[] textRendererDriveIgnored(long id) {
        TextRendererSession session = TEXT_RENDERERS.get(id);
        if (session == null) {
            return null;
        }
        try {
            MemorySegment renderer = session.block;
            MethodHandle decoration = virtual(RENDERER_DECORATION_FD);
            MethodHandle inlineObject = virtual(RENDERER_INLINE_OBJECT_FD);
            long[] result = new long[3];
            result[0] = Integer.toUnsignedLong((int) decoration.invokeExact(
                    slot(renderer, RENDERER_DRAW_UNDERLINE), renderer, MemorySegment.NULL, 1.5f,
                    -2.5f, MemorySegment.NULL, MemorySegment.NULL));
            result[1] = Integer.toUnsignedLong((int) decoration.invokeExact(
                    slot(renderer, RENDERER_DRAW_STRIKETHROUGH), renderer, MemorySegment.NULL, 3.5f,
                    -4.5f, MemorySegment.NULL, MemorySegment.NULL));
            result[2] = Integer.toUnsignedLong((int) inlineObject.invokeExact(
                    slot(renderer, RENDERER_DRAW_INLINE_OBJECT), renderer, MemorySegment.NULL, 5.5f,
                    -6.5f, MemorySegment.NULL, 1, 0, MemorySegment.NULL));
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Calls a stub through a pointer whose object has already been disposed, which is what a COM
     * client holding a stale reference would do. Nothing may be recorded and nothing may fault: the
     * id read out of the freed block misses the registry and every target answers its sentinel.
     *
     * @return {@code { QueryInterface HRESULT, AddRef, Release, SetScriptAnalysis HRESULT,
     *         GetParagraphReadingDirection, DrawGlyphRun HRESULT }}
     */
    static long[] staleCallbackProbe(long sinkBlock, long rendererBlock) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment sink = MemorySegment.ofAddress(sinkBlock);
            MemorySegment source = MemorySegment.ofAddress(sinkBlock + ANALYSIS_SOURCE_OFFSET);
            MemorySegment renderer = MemorySegment.ofAddress(rendererBlock);
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment iid = guid(scratch, IID_IDWRITE_TEXT_ANALYSIS_SINK);
            MemorySegment analysis = scratch.allocate(DWRITE_SCRIPT_ANALYSIS_LAYOUT);
            MemorySegment run = scratch.allocate(DWRITE_GLYPH_RUN_LAYOUT);
            long[] result = new long[6];
            result[0] = Integer.toUnsignedLong(onSinkQueryInterface(sink, iid, out));
            result[1] = onSinkAddRef(sink);
            result[2] = onSinkRelease(sink);
            result[3] = Integer.toUnsignedLong(onSetScriptAnalysis(sink, 0, 1, analysis));
            result[4] = onGetParagraphReadingDirection(source);
            result[5] = Integer.toUnsignedLong(onDrawGlyphRun(renderer, MemorySegment.NULL, 0f, 0f, 0,
                    run, MemorySegment.NULL, MemorySegment.NULL));
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The slot numbers this class binds, by name, so a test can pin the table it derived. */
    static int layoutSlot(String name) {
        return switch (name) {
            case "IDWriteFactory::CreateTextFormat" -> FACTORY_CREATE_TEXT_FORMAT;
            case "IDWriteFactory::CreateTextLayout" -> FACTORY_CREATE_TEXT_LAYOUT;
            case "IDWriteFactory::CreateTextAnalyzer" -> FACTORY_CREATE_TEXT_ANALYZER;
            case "IDWriteTextAnalyzer::AnalyzeScript" -> ANALYZER_ANALYZE_SCRIPT;
            case "IDWriteTextAnalyzer::AnalyzeBidi" -> ANALYZER_ANALYZE_BIDI;
            case "IDWriteTextAnalyzer::AnalyzeNumberSubstitution" -> ANALYZER_ANALYZE_NUMBER_SUBSTITUTION;
            case "IDWriteTextAnalyzer::AnalyzeLineBreakpoints" -> ANALYZER_ANALYZE_LINE_BREAKPOINTS;
            case "IDWriteTextAnalyzer::GetGlyphs" -> ANALYZER_GET_GLYPHS;
            case "IDWriteTextAnalyzer::GetGlyphPlacements" -> ANALYZER_GET_GLYPH_PLACEMENTS;
            case "IDWriteTextFormat::GetFontFamilyNameLength" -> TEXT_FORMAT_GET_FONT_FAMILY_NAME_LENGTH;
            case "IDWriteTextFormat::GetFontFamilyName" -> TEXT_FORMAT_GET_FONT_FAMILY_NAME;
            case "IDWriteTextFormat::GetFontWeight" -> TEXT_FORMAT_GET_FONT_WEIGHT;
            case "IDWriteTextFormat::GetFontStyle" -> TEXT_FORMAT_GET_FONT_STYLE;
            case "IDWriteTextFormat::GetFontStretch" -> TEXT_FORMAT_GET_FONT_STRETCH;
            case "IDWriteTextFormat::GetFontSize" -> TEXT_FORMAT_GET_FONT_SIZE;
            case "IDWriteTextFormat::GetLocaleNameLength" -> TEXT_FORMAT_GET_LOCALE_NAME_LENGTH;
            case "IDWriteTextFormat::GetLocaleName" -> TEXT_FORMAT_GET_LOCALE_NAME;
            case "IDWriteTextLayout::GetMaxWidth" -> TEXT_LAYOUT_GET_MAX_WIDTH;
            case "IDWriteTextLayout::GetMaxHeight" -> TEXT_LAYOUT_GET_MAX_HEIGHT;
            case "IDWriteTextLayout::Draw" -> TEXT_LAYOUT_DRAW;
            case "IDWriteTextAnalysisSink::SetScriptAnalysis" -> ANALYSIS_SINK_SET_SCRIPT_ANALYSIS;
            case "IDWriteTextAnalysisSink::SetLineBreakpoints" -> ANALYSIS_SINK_SET_LINE_BREAKPOINTS;
            case "IDWriteTextAnalysisSink::SetBidiLevel" -> ANALYSIS_SINK_SET_BIDI_LEVEL;
            case "IDWriteTextAnalysisSink::SetNumberSubstitution" -> ANALYSIS_SINK_SET_NUMBER_SUBSTITUTION;
            case "IDWriteTextAnalysisSource::GetTextAtPosition" -> ANALYSIS_SOURCE_GET_TEXT_AT_POSITION;
            case "IDWriteTextAnalysisSource::GetTextBeforePosition" -> ANALYSIS_SOURCE_GET_TEXT_BEFORE_POSITION;
            case "IDWriteTextAnalysisSource::GetParagraphReadingDirection" -> ANALYSIS_SOURCE_GET_READING_DIRECTION;
            case "IDWriteTextAnalysisSource::GetLocaleName" -> ANALYSIS_SOURCE_GET_LOCALE_NAME;
            case "IDWriteTextAnalysisSource::GetNumberSubstitution" -> ANALYSIS_SOURCE_GET_NUMBER_SUBSTITUTION;
            case "IDWritePixelSnapping::IsPixelSnappingDisabled" -> RENDERER_IS_PIXEL_SNAPPING_DISABLED;
            case "IDWritePixelSnapping::GetCurrentTransform" -> RENDERER_GET_CURRENT_TRANSFORM;
            case "IDWritePixelSnapping::GetPixelsPerDip" -> RENDERER_GET_PIXELS_PER_DIP;
            case "IDWriteTextRenderer::DrawGlyphRun" -> RENDERER_DRAW_GLYPH_RUN;
            case "IDWriteTextRenderer::DrawUnderline" -> RENDERER_DRAW_UNDERLINE;
            case "IDWriteTextRenderer::DrawStrikethrough" -> RENDERER_DRAW_STRIKETHROUGH;
            case "IDWriteTextRenderer::DrawInlineObject" -> RENDERER_DRAW_INLINE_OBJECT;
            default -> throw new IllegalArgumentException("no such slot: " + name);
        };
    }

    /* =============================================================================================
     * The Direct2D + WIC greyscale mask path
     *
     * The last sixteen natives of directwrite.cpp: CoInitializeEx/CoUninitialize (the COM apartment),
     * the two factory constructors (_WICCreateImagingFactory, _D2D1CreateFactory) and the twelve
     * vtable calls that draw one glyph into a WIC bitmap and read its bytes back.
     * PrismFontFile.getDefaultAAMode() is AA_GREYSCALE, so DWGlyph.getPixelData routes every non-LCD
     * strike through this path: it is the most commonly executed mask path in the product, and the
     * one directwrite-metrics-golden.txt deliberately does not pin.
     * ========================================================================================== */

    /** {@code winerror.h}: COM was already initialized on this thread with a different model. */
    static final int RPC_E_CHANGED_MODE = 0x80010106;

    /** {@code winerror.h}: a success HRESULT that is not {@code S_OK} - what a second CoInitializeEx gives. */
    static final int S_FALSE = 0x1;

    /** {@code wtypesbase.h}: {@code CLSCTX_INPROC_SERVER}, the only class context the C asks for. */
    private static final int CLSCTX_INPROC_SERVER = 0x1;

    /** {@code wincodec.h:385}. Factory1, not Factory2 - see the class comment on {@link #wicCreateImagingFactory}. */
    static final String CLSID_WIC_IMAGING_FACTORY = "cacaf262-9370-4615-a13b-9f5539da4c0a";

    /** {@code wincodec.h:387}: only ever used to prove, in a test, that it is NOT the one being passed. */
    static final String CLSID_WIC_IMAGING_FACTORY2 = "317d06e8-5f24-433d-bdf7-79ce68d8abc2";

    /** {@code wincodec.h:6927}: the uuid of the {@code MIDL_INTERFACE} declaring IWICImagingFactory. */
    static final String IID_IWIC_IMAGING_FACTORY = "ec5ec8a9-c395-4314-9c77-54d7a935ff70";

    /** {@code d2d1.h:3341}: the uuid of the {@code DX_DECLARE_INTERFACE} declaring ID2D1Factory. */
    static final String IID_ID2D1_FACTORY = "06152247-6f50-465a-9245-118bfd3b6007";

    /**
     * The eleven {@code GUID_WICPixelFormat*} values of the switch at {@code directwrite.cpp:2371-2384},
     * indexed by the {@code OS.GUID_WICPixelFormat*} constant minus one (wincodec.h:547-565). Only
     * {@code 32bppPBGRA} ({@code DWGlyph.java:50}) is reachable today; the table stays complete so that
     * the mapping - and its {@code default: return NULL} before any COM call - is reproduced exactly.
     */
    private static final String[] WIC_PIXEL_FORMAT_UUIDS = {
            "6fddc324-4e03-4bfe-b185-3d77768dc908",   //  1 GUID_WICPixelFormat8bppGray,      :547
            "e6cd0116-eeba-4161-aa85-27dd9fb3a895",   //  2 GUID_WICPixelFormat8bppAlpha,     :548
            "6fddc324-4e03-4bfe-b185-3d77768dc90b",   //  3 GUID_WICPixelFormat16bppGray,     :554
            "6fddc324-4e03-4bfe-b185-3d77768dc90d",   //  4 GUID_WICPixelFormat24bppRGB,      :556
            "6fddc324-4e03-4bfe-b185-3d77768dc90c",   //  5 GUID_WICPixelFormat24bppBGR,      :555
            "6fddc324-4e03-4bfe-b185-3d77768dc90e",   //  6 GUID_WICPixelFormat32bppBGR,      :557
            "6fddc324-4e03-4bfe-b185-3d77768dc90f",   //  7 GUID_WICPixelFormat32bppBGRA,     :558
            "6fddc324-4e03-4bfe-b185-3d77768dc910",   //  8 GUID_WICPixelFormat32bppPBGRA,    :559
            "6fddc324-4e03-4bfe-b185-3d77768dc911",   //  9 GUID_WICPixelFormat32bppGrayFloat,:560
            "f5c7ad2d-6a8d-43dd-a7a8-a29935261ae9",   // 10 GUID_WICPixelFormat32bppRGBA,     :564
            "3cc4a650-a527-4d37-a916-3142c7ebedba" }; // 11 GUID_WICPixelFormat32bppPRGBA,    :565

    /* IWICImagingFactory : IUnknown, wincodec.h:6927. base 3 + ordinal. */
    private static final int WIC_FACTORY_CREATE_BITMAP = 17;        // ord 14, wincodec.h:6988

    /*
     * IWICBitmap : IWICBitmapSource, wincodec.h:2330. IWICBitmapSource (wincodec.h:1357) declares five
     * methods, so the base is 8; slots 3 and 4 are its GetSize and GetPixelFormat, reached through an
     * IWICBitmap pointer, which is what pins the base arithmetic behaviourally.
     */
    private static final int WIC_BITMAP_GET_SIZE = 3;               // IWICBitmapSource ord 0, :1361
    private static final int WIC_BITMAP_GET_PIXEL_FORMAT = 4;       // IWICBitmapSource ord 1, :1365
    private static final int WIC_BITMAP_LOCK = 8;                   // ord 0, wincodec.h:2334

    /* IWICBitmapLock : IUnknown, wincodec.h:2209. base 3 + ordinal. */
    private static final int WIC_LOCK_GET_SIZE = 3;                 // ord 0, wincodec.h:2213
    private static final int WIC_LOCK_GET_STRIDE = 4;               // ord 1, wincodec.h:2217
    private static final int WIC_LOCK_GET_DATA_POINTER = 5;         // ord 2, wincodec.h:2220

    /* ID2D1Factory : IUnknown, d2d1.h:3341. base 3 + ordinal. */
    private static final int D2D_FACTORY_CREATE_WIC_TARGET = 13;    // ord 10, d2d1.h:3423

    /*
     * ID2D1RenderTarget : ID2D1Resource, d2d1.h:2392. ID2D1Resource (d2d1.h:1082) declares exactly one
     * method, GetFactory (:1088), so the base is 4 - not 3. Every slot below is 4 + ordinal, and the
     * three accessors at the far end exist only to pin that arithmetic where it matters most.
     */
    private static final int RT_CREATE_SOLID_COLOR_BRUSH = 8;       // ord  4, d2d1.h:2439
    private static final int RT_DRAW_GLYPH_RUN = 29;                // ord 25, d2d1.h:2643
    private static final int RT_SET_TRANSFORM = 30;                 // ord 26, d2d1.h:2650
    private static final int RT_SET_TEXT_ANTIALIAS_MODE = 34;       // ord 30, d2d1.h:2665
    private static final int RT_CLEAR = 47;                         // ord 43, d2d1.h:2754
    private static final int RT_BEGIN_DRAW = 48;                    // ord 44, d2d1.h:2762
    private static final int RT_END_DRAW = 49;                      // ord 45, d2d1.h:2769
    private static final int RT_GET_PIXEL_FORMAT = 50;              // ord 46, d2d1.h:2774
    private static final int RT_GET_SIZE = 53;                      // ord 49, d2d1.h:2799
    private static final int RT_GET_PIXEL_SIZE = 54;                // ord 50, d2d1.h:2805
    private static final int RT_GET_MAXIMUM_BITMAP_SIZE = 55;       // ord 51, d2d1.h:2812

    /** {@code WICRect} (wincodec.h:491): four {@code INT}, 16 bytes. The stack struct of {@code :2395}. */
    static final StructLayout WIC_RECT_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("X"),                   // offset 0
            JAVA_INT.withName("Y"),                   // offset 4
            JAVA_INT.withName("Width"),               // offset 8
            JAVA_INT.withName("Height"));             // offset 12, byteSize 16

    /** {@code D2D1_COLOR_F} (d2d1.h:288 -> d2dbasetypes.h:24, D3DCOLORVALUE): four floats, 16 bytes. By pointer. */
    static final StructLayout D2D1_COLOR_F_LAYOUT = MemoryLayout.structLayout(
            JAVA_FLOAT.withName("r"),                 // offset 0
            JAVA_FLOAT.withName("g"),                 // offset 4
            JAVA_FLOAT.withName("b"),                 // offset 8
            JAVA_FLOAT.withName("a"));                // offset 12, byteSize 16

    /**
     * {@code D2D1_MATRIX_3X2_F} (d2d1.h:289 -> dcommon.h:283, {@code D2D_MATRIX_3X2_F}): six floats, 24 bytes. The
     * SDK declares it as a union of three views of the same 24 bytes; the first is
     * {@code m11, m12, m21, m22, dx, dy}, which is the order the Java mirror uses. By pointer.
     */
    static final StructLayout D2D1_MATRIX_3X2_F_LAYOUT = MemoryLayout.structLayout(
            JAVA_FLOAT.withName("m11"),               // offset 0
            JAVA_FLOAT.withName("m12"),               // offset 4
            JAVA_FLOAT.withName("m21"),               // offset 8
            JAVA_FLOAT.withName("m22"),               // offset 12
            JAVA_FLOAT.withName("dx"),                // offset 16
            JAVA_FLOAT.withName("dy"));               // offset 20, byteSize 24

    /** {@code D2D1_PIXEL_FORMAT} (dcommon.h:163): a DXGI_FORMAT and a D2D1_ALPHA_MODE, 8 bytes. */
    static final StructLayout D2D1_PIXEL_FORMAT_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("format"),              // offset 0
            JAVA_INT.withName("alphaMode"));          // offset 4, byteSize 8

    /**
     * {@code D2D1_RENDER_TARGET_PROPERTIES} (d2d1.h:886): 28 bytes with the nested
     * {@code D2D1_PIXEL_FORMAT} flattened at 4 and 8. Everything is 4-byte aligned, so there is no
     * padding anywhere and the struct does not round up to 32.
     */
    static final StructLayout D2D1_RENDER_TARGET_PROPERTIES_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("type"),                              // offset 0
            D2D1_PIXEL_FORMAT_LAYOUT.withName("pixelFormat"),       // offset 4  (format 4, alphaMode 8)
            JAVA_FLOAT.withName("dpiX"),                            // offset 12
            JAVA_FLOAT.withName("dpiY"),                            // offset 16
            JAVA_INT.withName("usage"),                             // offset 20
            JAVA_INT.withName("minLevel"));                         // offset 24, byteSize 28

    /** {@code D2D1_FACTORY_OPTIONS} (d2d1.h:1009): one {@code D2D1_DEBUG_LEVEL}, 4 bytes. */
    static final StructLayout D2D1_FACTORY_OPTIONS_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("debugLevel"));         // offset 0, byteSize 4

    /**
     * {@code D2D1_SIZE_F} (dcommon.h:260, {@code D2D_SIZE_F}): two floats, 8 bytes. The shape of the
     * buffer {@code ID2D1RenderTarget::GetSize} (d2d1.h:2799) fills - see {@link #STRUCT_RETURN_THIS}
     * for why an aggregate a COM method declares as returned by value never travels in a register.
     */
    static final StructLayout D2D1_SIZE_F_LAYOUT = MemoryLayout.structLayout(
            JAVA_FLOAT.withName("width"),             // offset 0
            JAVA_FLOAT.withName("height"));           // offset 4, byteSize 8

    /** {@code D2D1_SIZE_U} (dcommon.h:272): two {@code UINT32}, 8 bytes; the {@code GetPixelSize} buffer. */
    static final StructLayout D2D1_SIZE_U_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("width"),               // offset 0
            JAVA_INT.withName("height"));             // offset 4, byteSize 8

    private static final long RTP_TYPE = offsetOf(D2D1_RENDER_TARGET_PROPERTIES_LAYOUT, "type");
    private static final long RTP_FORMAT =
            offsetOf(D2D1_RENDER_TARGET_PROPERTIES_LAYOUT, "pixelFormat", "format");
    private static final long RTP_ALPHA_MODE =
            offsetOf(D2D1_RENDER_TARGET_PROPERTIES_LAYOUT, "pixelFormat", "alphaMode");
    private static final long RTP_DPI_X = offsetOf(D2D1_RENDER_TARGET_PROPERTIES_LAYOUT, "dpiX");
    private static final long RTP_DPI_Y = offsetOf(D2D1_RENDER_TARGET_PROPERTIES_LAYOUT, "dpiY");
    private static final long RTP_USAGE = offsetOf(D2D1_RENDER_TARGET_PROPERTIES_LAYOUT, "usage");
    private static final long RTP_MIN_LEVEL = offsetOf(D2D1_RENDER_TARGET_PROPERTIES_LAYOUT, "minLevel");

    /* The vtable shapes the Direct2D + WIC path adds. Shapes already linked above are reused by descriptor. */

    /** {@code void f(this)}: {@code ID2D1RenderTarget::BeginDraw} (d2d1.h:2762). */
    private static final MethodHandle VOID_THIS = virtual(FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code void f(this, int)}: {@code SetTextAntialiasMode} (d2d1.h:2665). */
    private static final MethodHandle VOID_THIS_I = virtual(FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /** {@code HRESULT f(this, void*, void*)}: {@code GetDataPointer}, {@code GetSize}, {@code EndDraw}. */
    private static final MethodHandle HR_2A =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /**
     * {@code HRESULT f(this, UINT w, UINT h, REFWICPixelFormatGUID, WICBitmapCreateCacheOption,
     * IWICBitmap** out)}: {@code IWICImagingFactory::CreateBitmap} (wincodec.h:6988). The pixel format
     * is a {@code const GUID&} (wincodec.h:534), so it travels as a pointer like any other reference.
     */
    private static final MethodHandle HR_CREATE_BITMAP = virtual(FunctionDescriptor.of(
            JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));

    /** {@code HRESULT f(this, CONST WICRect*, DWORD, IWICBitmapLock** out)}: {@code IWICBitmap::Lock}. */
    private static final MethodHandle HR_LOCK =
            virtual(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));

    /**
     * {@code void f(this, D2D1_POINT_2F baselineOrigin, CONST DWRITE_GLYPH_RUN*, ID2D1Brush*,
     * DWRITE_MEASURING_MODE)}: {@code ID2D1RenderTarget::DrawGlyphRun} (d2d1.h:2643). The point is the
     * only aggregate this path passes <b>by value</b>; see {@link #D2D1_POINT_2F_LAYOUT} for why the
     * layout must appear in the descriptor rather than two {@code JAVA_FLOAT} parameters.
     */
    private static final MethodHandle VOID_DRAW_GLYPH_RUN = virtual(FunctionDescriptor.ofVoid(
            ADDRESS, D2D1_POINT_2F_LAYOUT, ADDRESS, ADDRESS, JAVA_INT));

    /**
     * {@code D2D1_SIZE_F f(this)} / {@code D2D1_SIZE_U f(this)} / {@code D2D1_PIXEL_FORMAT f(this)}:
     * the three aggregates {@code ID2D1RenderTarget} declares as returned <b>by value</b>
     * (d2d1.h:2774, :2799, :2805). Not one of the sixteen migrated natives needs them; they are bound
     * because they are the only behavioural pin available for the far end of a 53-method vtable, and
     * because they are the one place in this port where the struct-return convention is exercised at
     * all.
     * <p>
     * <b>They are not returned in a register, even at 8 bytes.</b> Modelling them as
     * {@code FunctionDescriptor.of(D2D1_SIZE_F_LAYOUT, ADDRESS)} - which the Microsoft x64 rule for
     * <em>free</em> functions would justify, since an aggregate of 1, 2, 4 or 8 bytes is a register
     * aggregate and FFM classifies it that way
     * ({@code x64/windows/TypeClass.isRegisterAggregate}) - faults inside {@code d2d1.dll}. The
     * crash names the convention exactly: the callee computes the right answer for the right object
     * (RCX held {@code 0x0000000100000057}, i.e. {@code DXGI_FORMAT_B8G8R8A8_UNORM} with
     * {@code D2D1_ALPHA_MODE_PREMULTIPLIED}, for the render target that was asked) and then stores
     * those 8 bytes through a pointer it took from RDX, which nothing had written.
     * <p>
     * So a COM method that returns an aggregate takes a caller-allocated buffer as a hidden argument
     * <em>after</em> {@code this}: {@code this} in RCX, the buffer in RDX, and the same pointer handed
     * back in RAX. That is the shape every non-MSVC binding of Direct2D uses, and it is why
     * {@code ID2D1RenderTarget::GetSize} is famously uncallable from MinGW. The one descriptor below
     * expresses it, and {@link #renderTargetStructReturnEchoesItsBuffer} checks the RAX half against
     * the object rather than against this comment.
     */
    private static final MethodHandle STRUCT_RETURN_THIS =
            virtual(FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    /**
     * The eleven WIC pixel-format GUIDs in one 176-byte {@link Arena#global() global} block, allocated
     * once. Nothing on the Direct2D + WIC path ever builds a GUID on a call path.
     */
    private static final MemorySegment WIC_PIXEL_FORMAT_GUIDS = wicPixelFormatBlock();

    /** {@code CLSID_WICImagingFactory} and {@code IID_IWICImagingFactory}, allocated once. */
    private static final MemorySegment CLSID_WIC_IMAGING_FACTORY_BYTES =
            guid(Arena.global(), CLSID_WIC_IMAGING_FACTORY);
    private static final MemorySegment IID_IWIC_IMAGING_FACTORY_BYTES =
            guid(Arena.global(), IID_IWIC_IMAGING_FACTORY);

    /** {@code __uuidof(ID2D1Factory)}, the {@code REFIID} of {@code directwrite.cpp:894}. */
    private static final MemorySegment IID_ID2D1_FACTORY_BYTES = guid(Arena.global(), IID_ID2D1_FACTORY);

    private static MemorySegment wicPixelFormatBlock() {
        long size = GUID_LAYOUT.byteSize();
        MemorySegment block = Arena.global()
                .allocate(size * WIC_PIXEL_FORMAT_UUIDS.length, GUID_LAYOUT.byteAlignment());
        try (Arena scratch = Arena.ofConfined()) {
            for (int i = 0; i < WIC_PIXEL_FORMAT_UUIDS.length; i++) {
                MemorySegment.copy(guid(scratch, WIC_PIXEL_FORMAT_UUIDS[i]), 0, block, i * size, size);
            }
        }
        return block;
    }

    /**
     * The 16 bytes of the {@code GUID_WICPixelFormat*} the {@code OS} constant {@code pixelFormat}
     * names, or {@code null} for a value the switch at {@code directwrite.cpp:2371-2384} does not
     * list - which returns {@code NULL} <em>before</em> {@code CreateBitmap} is ever called.
     */
    private static MemorySegment wicPixelFormatGuid(int pixelFormat) {
        if (pixelFormat < 1 || pixelFormat > WIC_PIXEL_FORMAT_UUIDS.length) {
            return null;
        }
        long size = GUID_LAYOUT.byteSize();
        return WIC_PIXEL_FORMAT_GUIDS.asSlice((pixelFormat - 1) * size, size);
    }

    /** The byte offset of a field of a nested struct, so the nesting is expressed once. */
    private static long offsetOf(StructLayout layout, String group, String field) {
        return layout.byteOffset(MemoryLayout.PathElement.groupElement(group),
                MemoryLayout.PathElement.groupElement(field));
    }

    /**
     * {@code ole32.dll}, loaded on <b>first use</b>. The JNI build imported
     * {@code CoInitializeEx}/{@code CoUninitialize}/{@code CoCreateInstance} statically, so ole32 was
     * pulled in the moment {@code javafx_font.dll} loaded; but {@code DWFactory.getWICFactory} is the
     * only caller, and it runs on the D2D/render thread the first time a greyscale or fallback mask is
     * needed. A holder class keeps that timing and that thread, and keeps a JavaFX process that never
     * draws text free of the dependency. A missing library leaves every handle {@code null}, which the
     * facade methods below turn into the same "COM unavailable" answers the C produced.
     */
    private static final class Ole32 {

        private static final SymbolLookup LIBRARY = tryLoad("ole32.dll");

        /** {@code HRESULT CoInitializeEx(LPVOID pvReserved, DWORD dwCoInit)} (combaseapi.h). */
        static final MethodHandle CO_INITIALIZE_EX = bindOptional(LIBRARY, "ole32.dll",
                "CoInitializeEx", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

        /** {@code void CoUninitialize(void)} (combaseapi.h). */
        static final MethodHandle CO_UNINITIALIZE = bindOptional(LIBRARY, "ole32.dll",
                "CoUninitialize", FunctionDescriptor.ofVoid());

        /**
         * {@code HRESULT CoCreateInstance(REFCLSID, LPUNKNOWN, DWORD, REFIID, LPVOID*)}
         * (combaseapi.h). Both GUIDs cross as {@code const GUID&}, i.e. as pointers.
         */
        static final MethodHandle CO_CREATE_INSTANCE = bindOptional(LIBRARY, "ole32.dll",
                "CoCreateInstance",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));

        private Ole32() {
        }
    }

    /**
     * {@code d2d1.dll}, loaded on <b>first use</b>, which is what {@code directwrite.cpp:885} does:
     * {@code LoadLibrary(TEXT("d2d1.dll"))} inside {@code _D2D1CreateFactory}, reached only from
     * {@code DWFactory.getD2DFactory()} behind {@code checkThread()}. Binding it from the
     * {@code DWNative} initializer would pull d2d1.dll and its {@code DllMain} into every JavaFX
     * process - including LCD-only ones and ones that never draw text - at font-factory creation time
     * and on the FX thread instead. {@link Arena#global()} matches the missing {@code FreeLibrary} at
     * {@code directwrite.cpp:885}.
     */
    private static final class D2D1 {

        private static final SymbolLookup LIBRARY = tryLoad("d2d1.dll");

        /**
         * {@code HRESULT WINAPI D2D1CreateFactory(D2D1_FACTORY_TYPE, REFIID,
         * CONST D2D1_FACTORY_OPTIONS*, void**)} (d2d1.h:3666-3672), or {@code null} when the library
         * or the export is missing - the {@code hr = E_FAIL} the C leaves behind at {@code directwrite.cpp:883}.
         */
        static final MethodHandle CREATE_FACTORY = bindOptional(LIBRARY, "d2d1.dll",
                "D2D1CreateFactory",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

        private D2D1() {
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * The COM apartment - directwrite.cpp:848-864
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code CoInitializeEx(NULL, dwCoInit)} ({@code directwrite.cpp:848-858}), returning what the C
     * returned: {@code false} for {@code RPC_E_CHANGED_MODE} and {@code true} for everything else -
     * including {@code S_FALSE}, which is what a second initialization of an already-initialized
     * thread answers, and including a genuine failure HRESULT, which the C also reported as success.
     * <p>
     * The call is per <em>thread</em>, and so is its {@link #coUninitialize} partner:
     * {@code DWFactory.getWICFactory()} makes it on the first thread that asks for a WIC or D2D
     * factory ({@code DWFactory.checkThread()} pins that thread for the life of the process) and the
     * {@code GraphicsPipeline} dispose hook makes the matching {@code CoUninitialize} on the same
     * thread. {@code RPC_E_CHANGED_MODE} does <b>not</b> increment the per-thread initialization count
     * COM keeps, and the C correspondingly does not call {@code CoUninitialize} on that path: it
     * returns {@code null} from {@code getWICFactory} before the dispose hook is ever registered.
     *
     * @return {@code false} only when COM is already initialized on this thread with a different
     *         concurrency model
     */
    static boolean coInitializeEx(int dwCoInit) {
        if (Ole32.CO_INITIALIZE_EX == null) {
            return false;
        }
        try {
            int hr = (int) Ole32.CO_INITIALIZE_EX.invokeExact(MemorySegment.NULL, dwCoInit);
            return hr != RPC_E_CHANGED_MODE;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code CoUninitialize()} ({@code directwrite.cpp:860-864}); a no-op when ole32 is unavailable. */
    static void coUninitialize() {
        if (Ole32.CO_UNINITIALIZE == null) {
            return;
        }
        try {
            Ole32.CO_UNINITIALIZE.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * The two factories - directwrite.cpp:866-899
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code CoCreateInstance(CLSID_WICImagingFactory, NULL, CLSCTX_INPROC_SERVER,
     * IID_IWICImagingFactory, &result)} ({@code directwrite.cpp:866-878}); {@code 0} on any failure.
     * It does not initialize COM itself - {@code DWFactory.getWICFactory} does, and bails out when
     * that fails.
     * <p>
     * <b>Which CLSID.</b> wincodec.h:385 defines {@code CLSID_WICImagingFactory} as factory1 and
     * :388-390 redefines it to factory2 under
     * {@code #if(_WIN32_WINNT >= _WIN32_WINNT_WIN8) || defined(_WIN7_PLATFORM_UPDATE)}.
     * {@code win.cmake:257} compiles the font target with {@code /D_WIN32_WINNT=0x0601} and nothing in
     * {@code native-font} or in the SDK defines {@code _WIN7_PLATFORM_UPDATE}, so the shipped
     * {@code javafx_font.dll} carries factory1 - confirmed by searching the binary for both 16-byte
     * encodings: factory1 is present once, factory2 is absent.
     */
    static long wicCreateImagingFactory() {
        if (Ole32.CO_CREATE_INSTANCE == null) {
            return 0L;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            int hr = (int) Ole32.CO_CREATE_INSTANCE.invokeExact(CLSID_WIC_IMAGING_FACTORY_BYTES,
                    MemorySegment.NULL, CLSCTX_INPROC_SERVER, IID_IWIC_IMAGING_FACTORY_BYTES, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code D2D1CreateFactory(factoryType, __uuidof(ID2D1Factory), &options, &result)}
     * ({@code directwrite.cpp:880-899}); {@code 0} when d2d1.dll is absent, does not export
     * {@code D2D1CreateFactory}, or fails - the three paths the C collapses into a silent
     * {@code hr = E_FAIL}. {@code D2D1_FACTORY_OPTIONS} is four zero bytes
     * ({@code D2D1_DEBUG_LEVEL_NONE}, {@code :891-892}); the C never passes {@code NULL} there, so
     * neither does this.
     */
    static long d2d1CreateFactory(int factoryType) {
        if (D2D1.CREATE_FACTORY == null) {
            return 0L;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment options = scratch.allocate(D2D1_FACTORY_OPTIONS_LAYOUT);
            MemorySegment out = scratch.allocate(ADDRESS);
            int hr = (int) D2D1.CREATE_FACTORY.invokeExact(factoryType, IID_ID2D1_FACTORY_BYTES,
                    options, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IWICImagingFactory / IWICBitmap / IWICBitmapLock - directwrite.cpp:2365-2423
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code IWICImagingFactory::CreateBitmap}, slot 17 (wincodec.h:6988)
     * ({@code directwrite.cpp:2365-2388}). The eleven-way switch over {@code OS.GUID_WICPixelFormat*}
     * becomes {@link #wicPixelFormatGuid}, and its {@code default: return NULL} keeps its position:
     * an unknown value returns {@code 0} <em>before</em> any COM call is made.
     */
    static long createBitmap(long self, int uiWidth, int uiHeight, int pixelFormat, int options) {
        MemorySegment format = wicPixelFormatGuid(pixelFormat);
        if (format == null) {
            return 0L;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_CREATE_BITMAP.invokeExact(slot(obj, WIC_FACTORY_CREATE_BITMAP), obj,
                    uiWidth, uiHeight, format, options, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IWICBitmap::Lock}, slot 8 (wincodec.h:2334) ({@code directwrite.cpp:2390-2398}). The
     * rectangle is always passed, never {@code NULL} - the C builds a stack {@code WICRect} at
     * {@code :2395} whatever the arguments are.
     */
    static long lock(long self, int x, int y, int width, int height, int flags) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment rect = scratch.allocate(WIC_RECT_LAYOUT);
            rect.setAtIndex(JAVA_INT, 0, x);
            rect.setAtIndex(JAVA_INT, 1, y);
            rect.setAtIndex(JAVA_INT, 2, width);
            rect.setAtIndex(JAVA_INT, 3, height);
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_LOCK.invokeExact(slot(obj, WIC_BITMAP_LOCK), obj, rect, flags, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IWICBitmapLock::GetDataPointer}, slot 5 (wincodec.h:2220)
     * ({@code directwrite.cpp:2401-2415}): the whole locked buffer copied into a fresh
     * {@code byte[]} of exactly {@code cbBufferSize}, or {@code null} on a failing HRESULT.
     * {@code DWGlyph.getD2DMask} then walks it row by row using {@link #getStride}, so the copy is
     * reproduced verbatim here; handing the loop a bounded {@code MemorySegment} instead would give
     * identical output and skip up to 256 KB per glyph, but it changes an interface signature and
     * belongs in a separate performance commit, not in a migration.
     * <p>
     * The length is a {@code UINT32}. The C passed it to {@code NewByteArray} as a {@code jsize}, so a
     * value above 2^31 would have raised {@code NegativeArraySizeException}; here it raises
     * {@code IllegalStateException} from {@code toArray} instead. A 256x256 32-bit bitmap locks
     * 256 KB, so neither is reachable.
     */
    static byte[] getDataPointer(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment size = scratch.allocate(JAVA_INT);
            MemorySegment data = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_2A.invokeExact(slot(obj, WIC_LOCK_GET_DATA_POINTER), obj, size, data);
            if (hr < 0) {
                return null;
            }
            long cbBufferSize = Integer.toUnsignedLong(size.get(JAVA_INT, 0));
            return bounded(data.get(ADDRESS, 0), cbBufferSize).toArray(JAVA_BYTE);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IWICBitmapLock::GetStride}, slot 4 (wincodec.h:2217)
     * ({@code directwrite.cpp:2417-2423}). A failure is indistinguishable from a stride of zero, as
     * there ({@code SUCCEEDED(hr) ? result : NULL}); {@code DWGlyph.getD2DMask} would then walk row 0
     * repeatedly. Reproduced rather than improved: an exception here would be a behaviour change.
     */
    static int getStride(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(JAVA_INT);
            MemorySegment obj = com(self);
            int hr = (int) HR_OUT.invokeExact(slot(obj, WIC_LOCK_GET_STRIDE), obj, out);
            return hr >= 0 ? out.get(JAVA_INT, 0) : 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * ID2D1Factory / ID2D1RenderTarget - directwrite.cpp:2427-2502
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code ID2D1Factory::CreateWicBitmapRenderTarget}, slot 13 (d2d1.h:3423)
     * ({@code directwrite.cpp:2427-2437}): the 28-byte properties struct crosses by pointer, and a
     * {@code null} holder reaches Direct2D as {@code NULL} exactly as the C {@code if (arg2)} guard
     * left it.
     */
    static long createWicBitmapRenderTarget(long self, long target,
                                            D2D1_RENDER_TARGET_PROPERTIES properties) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment props = properties == null ? MemorySegment.NULL
                    : encodeRenderTargetProperties(scratch, properties);
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_3A.invokeExact(slot(obj, D2D_FACTORY_CREATE_WIC_TARGET), obj,
                    com(target), props, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code ID2D1RenderTarget::BeginDraw}, slot 48 (d2d1.h:2762) ({@code directwrite.cpp:2440-2444}). */
    static void beginDraw(long self) {
        MemorySegment obj = com(self);
        try {
            VOID_THIS.invokeExact(slot(obj, RT_BEGIN_DRAW), obj);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code ID2D1RenderTarget::EndDraw}, slot 49 (d2d1.h:2769) ({@code directwrite.cpp:2446-2450}).
     * The C calls it through the C++ default arguments, so both {@code D2D1_TAG*} out-parameters are
     * {@code NULL}. {@code DWGlyph.java:203} compares the result with {@code S_OK}, not with
     * {@code SUCCEEDED}, and that comparison stays in {@code DWGlyph}.
     */
    static int endDraw(long self) {
        MemorySegment obj = com(self);
        try {
            return (int) HR_2A.invokeExact(slot(obj, RT_END_DRAW), obj, MemorySegment.NULL,
                    MemorySegment.NULL);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code ID2D1RenderTarget::Clear}, slot 47 (d2d1.h:2754) ({@code directwrite.cpp:2452-2458}).
     * A {@code null} colour still issues the call, with {@code NULL} - which is legal Direct2D and is
     * exactly what the C {@code if (arg1)} guard produced.
     */
    static void clear(long self, D2D1_COLOR_F clearColor) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment color = clearColor == null ? MemorySegment.NULL
                    : encodeColor(scratch, clearColor);
            MemorySegment obj = com(self);
            VOID_THIS_P.invokeExact(slot(obj, RT_CLEAR), obj, color);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code ID2D1RenderTarget::SetTextAntialiasMode}, slot 34 (d2d1.h:2665)
     * ({@code directwrite.cpp:2460-2464}).
     */
    static void setTextAntialiasMode(long self, int textAntialiasMode) {
        MemorySegment obj = com(self);
        try {
            VOID_THIS_I.invokeExact(slot(obj, RT_SET_TEXT_ANTIALIAS_MODE), obj, textAntialiasMode);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code ID2D1RenderTarget::SetTransform}, slot 30 (d2d1.h:2650)
     * ({@code directwrite.cpp:2466-2472}). As with {@link #clear}, a {@code null} holder still issues
     * the call with {@code NULL}; no caller does it, and Direct2D does not document it as legal, but
     * reproducing the C means reproducing that too.
     */
    static void setTransform(long self, D2D1_MATRIX_3X2_F transform) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment matrix = transform == null ? MemorySegment.NULL
                    : encodeMatrix3x2(scratch, transform);
            MemorySegment obj = com(self);
            VOID_THIS_P.invokeExact(slot(obj, RT_SET_TRANSFORM), obj, matrix);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code ID2D1RenderTarget::DrawGlyphRun}, slot 29 (d2d1.h:2643)
     * ({@code directwrite.cpp:2474-2490}) - the one call in the package that passes an aggregate
     * <b>by value</b>. The 8-byte {@code D2D1_POINT_2F} travels in the integer register that argument
     * occupies (RDX here, since {@code this} takes RCX), never in an XMM register and never split.
     * <p>
     * The glyph run is rebuilt per call with {@code glyphCount} forced to 1 and three one-element
     * arrays, which the C frees the instant the call returns ({@code :2487-2489}) - proof that
     * Direct2D copies whatever it keeps, and what licenses the confined arena.
     * <p>
     * One deliberate divergence: when the Java {@code D2D1_POINT_2F} is {@code null} the C passed the
     * uninitialised stack struct, i.e. indeterminate bytes. Java passes zeroes. No caller passes
     * {@code null} ({@code DWGlyph.java:194}, {@code ID2D1RenderTarget.java:53}).
     */
    static void drawGlyphRun(long self, D2D1_POINT_2F baselineOrigin, DWRITE_GLYPH_RUN glyphRun,
                             long foregroundBrush, int measuringMode) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment origin = scratch.allocate(D2D1_POINT_2F_LAYOUT);
            if (baselineOrigin != null) {
                origin.setAtIndex(JAVA_FLOAT, 0, baselineOrigin.x);
                origin.setAtIndex(JAVA_FLOAT, 1, baselineOrigin.y);
            }
            MemorySegment run = glyphRun == null ? MemorySegment.NULL
                    : encodeGlyphRun(scratch, glyphRun);
            MemorySegment obj = com(self);
            VOID_DRAW_GLYPH_RUN.invokeExact(slot(obj, RT_DRAW_GLYPH_RUN), obj, origin, run,
                    com(foregroundBrush), measuringMode);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code ID2D1RenderTarget::CreateSolidColorBrush}, slot 8 (d2d1.h:2439)
     * ({@code directwrite.cpp:2492-2502}).
     * <p>
     * <b>Three arguments, not two.</b> The pure virtual is
     * {@code CreateSolidColorBrush(CONST D2D1_COLOR_F *color, CONST D2D1_BRUSH_PROPERTIES
     * *brushProperties, ID2D1SolidColorBrush **solidColorBrush)}; the by-value call the C makes binds
     * the inline overload at d2d1.h:2922-2929, whose body supplies {@code NULL} for the middle
     * argument - the same trick as the two {@code EndDraw} tags. A two-pointer descriptor would put
     * the out-parameter in R8, where Direct2D reads {@code brushProperties}, and leave R9 - where it
     * writes the new brush - holding whatever the previous call left there.
     */
    static long createSolidColorBrush(long self, D2D1_COLOR_F color) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment rgba = color == null ? MemorySegment.NULL : encodeColor(scratch, color);
            MemorySegment out = scratch.allocate(ADDRESS);
            MemorySegment obj = com(self);
            int hr = (int) HR_3A.invokeExact(slot(obj, RT_CREATE_SOLID_COLOR_BRUSH), obj, rgba,
                    MemorySegment.NULL, out);
            return hr >= 0 ? out.get(ADDRESS, 0).address() : 0L;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The 16-byte {@code D2D1_COLOR_F}: r, g, b, a in declaration order. */
    private static MemorySegment encodeColor(Arena arena, D2D1_COLOR_F holder) {
        MemorySegment color = arena.allocate(D2D1_COLOR_F_LAYOUT);
        color.setAtIndex(JAVA_FLOAT, 0, holder.r);
        color.setAtIndex(JAVA_FLOAT, 1, holder.g);
        color.setAtIndex(JAVA_FLOAT, 2, holder.b);
        color.setAtIndex(JAVA_FLOAT, 3, holder.a);
        return color;
    }

    /** The 24-byte {@code D2D1_MATRIX_3X2_F}: m11, m12, m21, m22, dx, dy. */
    private static MemorySegment encodeMatrix3x2(Arena arena, D2D1_MATRIX_3X2_F holder) {
        MemorySegment matrix = arena.allocate(D2D1_MATRIX_3X2_F_LAYOUT);
        matrix.setAtIndex(JAVA_FLOAT, 0, holder._11);
        matrix.setAtIndex(JAVA_FLOAT, 1, holder._12);
        matrix.setAtIndex(JAVA_FLOAT, 2, holder._21);
        matrix.setAtIndex(JAVA_FLOAT, 3, holder._22);
        matrix.setAtIndex(JAVA_FLOAT, 4, holder._31);
        matrix.setAtIndex(JAVA_FLOAT, 5, holder._32);
        return matrix;
    }

    /** The 28-byte {@code D2D1_RENDER_TARGET_PROPERTIES}, with its nested pixel format flattened. */
    private static MemorySegment encodeRenderTargetProperties(Arena arena,
                                                              D2D1_RENDER_TARGET_PROPERTIES holder) {
        MemorySegment props = arena.allocate(D2D1_RENDER_TARGET_PROPERTIES_LAYOUT);
        props.set(JAVA_INT, RTP_TYPE, holder.type);
        props.set(JAVA_INT, RTP_FORMAT, holder.pixelFormat.format);
        props.set(JAVA_INT, RTP_ALPHA_MODE, holder.pixelFormat.alphaMode);
        props.set(JAVA_FLOAT, RTP_DPI_X, holder.dpiX);
        props.set(JAVA_FLOAT, RTP_DPI_Y, holder.dpiY);
        props.set(JAVA_INT, RTP_USAGE, holder.usage);
        props.set(JAVA_INT, RTP_MIN_LEVEL, holder.minLevel);
        return props;
    }

    /* ---------------------------------------------------------------------------------------------
     * Calls that were never JNI natives: the behavioural pins for the slot arithmetic
     *
     * ID2D1RenderTarget has 53 methods of its own and the mask path reaches six of them, the furthest at
     * slot 49. A slot table can only be proved by asking objects questions whose answers are known
     * independently, so the seven accessors below are bound too - each answers something the test
     * already knows (the bitmap is 256 x 256, its pixel format is the GUID that created it) and each
     * sits next to a slot that matters. They have no caller in main code.
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code ID2D1RenderTarget::GetSize}, slot 53 (d2d1.h:2799), as {@code {width, height}} in DIPs.
     * The {@code D2D1_SIZE_F} arrives through the hidden buffer described on
     * {@link #STRUCT_RETURN_THIS}, not in a register.
     */
    static float[] renderTargetSize(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(D2D1_SIZE_F_LAYOUT);
            MemorySegment obj = com(self);
            MemorySegment ignored = (MemorySegment) STRUCT_RETURN_THIS.invokeExact(
                    slot(obj, RT_GET_SIZE), obj, out);
            return new float[] { out.getAtIndex(JAVA_FLOAT, 0), out.getAtIndex(JAVA_FLOAT, 1) };
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code ID2D1RenderTarget::GetPixelSize}, slot 54 (d2d1.h:2805): two {@code UINT32} in the buffer. */
    static int[] renderTargetPixelSize(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(D2D1_SIZE_U_LAYOUT);
            MemorySegment obj = com(self);
            MemorySegment ignored = (MemorySegment) STRUCT_RETURN_THIS.invokeExact(
                    slot(obj, RT_GET_PIXEL_SIZE), obj, out);
            return new int[] { out.getAtIndex(JAVA_INT, 0), out.getAtIndex(JAVA_INT, 1) };
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code ID2D1RenderTarget::GetPixelFormat}, slot 50 (d2d1.h:2774): {@code {DXGI_FORMAT, alphaMode}}. */
    static int[] renderTargetPixelFormat(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(D2D1_PIXEL_FORMAT_LAYOUT);
            MemorySegment obj = com(self);
            MemorySegment ignored = (MemorySegment) STRUCT_RETURN_THIS.invokeExact(
                    slot(obj, RT_GET_PIXEL_FORMAT), obj, out);
            return new int[] { out.getAtIndex(JAVA_INT, 0), out.getAtIndex(JAVA_INT, 1) };
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The second half of the struct-return convention, measured rather than assumed: the callee hands
     * the caller-allocated buffer back in RAX. {@code true} when the pointer this facade passed as the
     * hidden argument is the pointer that came back.
     */
    static boolean renderTargetStructReturnEchoesItsBuffer(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocate(D2D1_SIZE_U_LAYOUT);
            MemorySegment obj = com(self);
            MemorySegment echoed = (MemorySegment) STRUCT_RETURN_THIS.invokeExact(
                    slot(obj, RT_GET_PIXEL_SIZE), obj, out);
            return echoed.address() == out.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code ID2D1RenderTarget::GetMaximumBitmapSize}, slot 55 (d2d1.h:2812): a plain {@code UINT32}. */
    static int renderTargetMaximumBitmapSize(long self) {
        MemorySegment obj = com(self);
        try {
            return (int) U32_THIS.invokeExact(slot(obj, RT_GET_MAXIMUM_BITMAP_SIZE), obj);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IWICBitmapSource::GetSize}, slot 3 (wincodec.h:1361), reached through an
     * {@code IWICBitmap} pointer: {@code {width, height}}, or {@code null} on a failing HRESULT. This
     * is the base-arithmetic pin - {@code IWICBitmapSource} contributes five methods, which is what
     * puts {@code Lock} at slot 8.
     */
    static int[] wicBitmapSize(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment width = scratch.allocate(JAVA_INT);
            MemorySegment height = scratch.allocate(JAVA_INT);
            MemorySegment obj = com(self);
            int hr = (int) HR_2A.invokeExact(slot(obj, WIC_BITMAP_GET_SIZE), obj, width, height);
            return hr >= 0 ? new int[] { width.get(JAVA_INT, 0), height.get(JAVA_INT, 0) } : null;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IWICBitmapSource::GetPixelFormat}, slot 4 (wincodec.h:1365), as the 16 raw GUID bytes -
     * the independent check that {@link #createBitmap} really handed WIC the GUID the
     * {@code OS.GUID_WICPixelFormat*} constant names.
     */
    static byte[] wicBitmapPixelFormat(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment format = scratch.allocate(GUID_LAYOUT);
            MemorySegment obj = com(self);
            int hr = (int) HR_OUT.invokeExact(slot(obj, WIC_BITMAP_GET_PIXEL_FORMAT), obj, format);
            return hr >= 0 ? format.toArray(JAVA_BYTE) : null;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code IWICBitmapLock::GetSize}, slot 3 (wincodec.h:2213): {@code {width, height}} of the locked
     * rectangle, or {@code null} on a failing HRESULT. Adjacent to {@code GetStride} (4) and
     * {@code GetDataPointer} (5), and it answers something neither of them does, so no permutation of
     * the three passes.
     */
    static int[] wicBitmapLockSize(long self) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment width = scratch.allocate(JAVA_INT);
            MemorySegment height = scratch.allocate(JAVA_INT);
            MemorySegment obj = com(self);
            int hr = (int) HR_2A.invokeExact(slot(obj, WIC_LOCK_GET_SIZE), obj, width, height);
            return hr >= 0 ? new int[] { width.get(JAVA_INT, 0), height.get(JAVA_INT, 0) } : null;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Direct2D + WIC test and diagnostic support
     * ------------------------------------------------------------------------------------------- */

    /** Whether ole32.dll loaded and exports all three COM entry points; loads it if it has not been. */
    static boolean isOle32Available() {
        return Ole32.CO_INITIALIZE_EX != null && Ole32.CO_UNINITIALIZE != null
                && Ole32.CO_CREATE_INSTANCE != null;
    }

    /** Whether d2d1.dll loaded and exports {@code D2D1CreateFactory}; loads it if it has not been. */
    static boolean isD2D1Available() {
        return D2D1.CREATE_FACTORY != null;
    }

    /**
     * The 16 bytes of the {@code GUID_WICPixelFormat*} an {@code OS} constant names, or {@code null}
     * for a value the C switch does not list.
     */
    static byte[] wicPixelFormatGuidBytes(int pixelFormat) {
        MemorySegment guid = wicPixelFormatGuid(pixelFormat);
        return guid == null ? null : guid.toArray(JAVA_BYTE);
    }

    /** How many pixel formats the table carries; the C switch has exactly this many cases. */
    static int wicPixelFormatCount() {
        return WIC_PIXEL_FORMAT_UUIDS.length;
    }

    /** The byte size of a named Direct2D/WIC layout, so a test can pin it without seeing a {@code MemoryLayout}. */
    static long layoutByteSize(String name) {
        return layout(name).byteSize();
    }

    /** The byte offset of a named field of a named Direct2D/WIC layout. */
    static long layoutOffset(String name, String field) {
        return layout(name).byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    private static StructLayout layout(String name) {
        return switch (name) {
            case "WICRect" -> WIC_RECT_LAYOUT;
            case "D2D1_COLOR_F" -> D2D1_COLOR_F_LAYOUT;
            case "D2D1_MATRIX_3X2_F" -> D2D1_MATRIX_3X2_F_LAYOUT;
            case "D2D1_PIXEL_FORMAT" -> D2D1_PIXEL_FORMAT_LAYOUT;
            case "D2D1_RENDER_TARGET_PROPERTIES" -> D2D1_RENDER_TARGET_PROPERTIES_LAYOUT;
            case "D2D1_FACTORY_OPTIONS" -> D2D1_FACTORY_OPTIONS_LAYOUT;
            case "D2D1_SIZE_F" -> D2D1_SIZE_F_LAYOUT;
            case "D2D1_SIZE_U" -> D2D1_SIZE_U_LAYOUT;
            default -> throw new IllegalArgumentException("no such layout: " + name);
        };
    }

    /** The vtable slot a named Direct2D/WIC method occupies, so the table is testable as data. */
    static int renderSlot(String name) {
        return switch (name) {
            case "IWICImagingFactory::CreateBitmap" -> WIC_FACTORY_CREATE_BITMAP;
            case "IWICBitmapSource::GetSize" -> WIC_BITMAP_GET_SIZE;
            case "IWICBitmapSource::GetPixelFormat" -> WIC_BITMAP_GET_PIXEL_FORMAT;
            case "IWICBitmap::Lock" -> WIC_BITMAP_LOCK;
            case "IWICBitmapLock::GetSize" -> WIC_LOCK_GET_SIZE;
            case "IWICBitmapLock::GetStride" -> WIC_LOCK_GET_STRIDE;
            case "IWICBitmapLock::GetDataPointer" -> WIC_LOCK_GET_DATA_POINTER;
            case "ID2D1Factory::CreateWicBitmapRenderTarget" -> D2D_FACTORY_CREATE_WIC_TARGET;
            case "ID2D1RenderTarget::CreateSolidColorBrush" -> RT_CREATE_SOLID_COLOR_BRUSH;
            case "ID2D1RenderTarget::DrawGlyphRun" -> RT_DRAW_GLYPH_RUN;
            case "ID2D1RenderTarget::SetTransform" -> RT_SET_TRANSFORM;
            case "ID2D1RenderTarget::SetTextAntialiasMode" -> RT_SET_TEXT_ANTIALIAS_MODE;
            case "ID2D1RenderTarget::Clear" -> RT_CLEAR;
            case "ID2D1RenderTarget::BeginDraw" -> RT_BEGIN_DRAW;
            case "ID2D1RenderTarget::EndDraw" -> RT_END_DRAW;
            case "ID2D1RenderTarget::GetPixelFormat" -> RT_GET_PIXEL_FORMAT;
            case "ID2D1RenderTarget::GetSize" -> RT_GET_SIZE;
            case "ID2D1RenderTarget::GetPixelSize" -> RT_GET_PIXEL_SIZE;
            case "ID2D1RenderTarget::GetMaximumBitmapSize" -> RT_GET_MAXIMUM_BITMAP_SIZE;
            default -> throw new IllegalArgumentException("no such slot: " + name);
        };
    }
}
