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

import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.win.WinGlassNativeShim;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parity tests for {@code WinPixels} and {@code WinCursor}, the two Glass peers whose natives Java replaced with
 * direct Win32 binds.
 * <p>
 * The {@code _fillDirectByteBuffer} half of this file is the parity oracle and is written to
 * be run <b>twice</b>: once against the JNI implementation ({@code Pixels.cpp:272-297}) and then,
 * unchanged, against the Java one. Every expectation is computed here from the source buffer, so the
 * file keeps its meaning after the native is gone. It covers all four buffer paths a
 * {@code com.sun.glass.ui.Pixels} can hold - direct or heap, bytes or ints - because the JNI reached
 * them through two different mechanisms ({@code GetDirectBufferAddress} versus a pinned array plus an
 * element-counted {@code arrayOffset}, {@code Utils.h:449-483}) that the Java has to agree with.
 * <p>
 * It runs headless: {@code Pixels}' constructors make no thread check ({@code Pixels.java:87-135})
 * and the shim calls {@code _fillDirectByteBuffer} directly, past the
 * {@code Application.checkEventThread()} of {@code Pixels.asByteBuffer} ({@code Pixels.java:228}).
 * Only the happy path was exercised while the JNI half was still in play: an exception thrown anywhere
 * on the JNI path reached {@code CheckAndClearException} ({@code Utils.cpp:50-70} then), which called
 * {@code javaIDs.Application.reportExceptionMID} - {@code NULL} until {@code GlassApplication}'s
 * {@code _initIDs} had run, i.e. a crashed JVM rather than a failed test.
 * <p>
 * The {@code WinCursor} half covers the five OS entry points that replaced
 * {@code Java_com_sun_glass_ui_win_WinCursor__1createCursor} - the symbols themselves, the
 * {@code BITMAPINFOHEADER} and {@code ICONINFO} layouts against the SDK, and then the cursor Windows
 * actually built, read back through {@code GetIconInfo} / {@code GetDIBits}. That read-back is written
 * against the {@code HCURSOR}, not against the implementation, so it is equally the oracle for the C
 * that used to build it. What it cannot cover is how a cursor <em>looks</em> once a window uses it,
 * which stays a manual check.
 * <p>
 * Anything that needs {@code CreateIconIndirect} or moves the real cursor display counter is gated on
 * {@link #probeCursor()} and restores what it changed in a {@code finally} - a unit test that leaves
 * the counter negative leaves the machine running the build without a mouse pointer.
 * <p>
 * Line numbers into the {@code native-glass/win} C++ sources refer to those files at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
@EnabledOnOs(OS.WINDOWS)
public class WinCursorPixelsNativeTest {

    /** A 4x3 BGRA-premultiplied image: 12 pixels, 48 bytes - large enough to catch a row mix-up. */
    private static final int WIDTH = 4;
    private static final int HEIGHT = 3;
    private static final int SIZE = WIDTH * HEIGHT * 4;

    /** The cursor the read-back tests build: the smallest size Windows always accepts. */
    private static final int CURSOR_SIDE = 16;

    /** Why a cursor or {@code ShowCursor} assertion was skipped; see {@link #probeCursor()}. */
    private static final String DESKTOP_REQUIRED =
            "this JVM has no interactive window station, so CreateIconIndirect cannot build a cursor:"
            + " these assertions need a desktop session, not a service or session 0.";

    private static boolean cursorsAvailable;

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(WinCursorPixelsNativeTest.class);

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // Both lazy blocks, in the order WinGlassNativeTest's exact BOUND_SYMBOLS list expects: which
        // test class runs first in this shared JVM is not fixed, the order within each @BeforeAll is.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        cursorsAvailable = probeCursor();
    }

    /**
     * Whether this session can build a cursor at all. {@code CreateIconIndirect} needs a window
     * station with a desktop, which a build running as a service or in session 0 does not have, and
     * failing every cursor assertion there would report a regression that is not one.
     */
    private static boolean probeCursor() {
        long cursor = WinGlassNativeShim.cursorCreateCustom(1, 1, ByteBuffer.allocateDirect(4), 0, 0);
        if (cursor == 0) {
            return false;
        }
        WinGlassNativeShim.destroyCursor(cursor);
        return true;
    }

    /**
     * The gate of every case that builds or shows a cursor: a skip with {@link #DESKTOP_REQUIRED} where
     * {@link #probeCursor()} failed, a failure with the same text under {@code -Djfx.parity.require=true},
     * and either way the body that follows counts as having run on the device ({@link ParityGate}).
     */
    private static void requireCursors() {
        LEDGER.requireOracle(cursorsAvailable, () -> DESKTOP_REQUIRED);
        LEDGER.compared();
    }

    /** If cursors could be built, at least one cursor body ran. */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    // ---------------------------------------------------------------------------------------------
    // WinPixels: the pixel format constant
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code Java_com_sun_glass_ui_win_WinPixels__1initIDs} returned
     * {@code com_sun_glass_ui_Pixels_Format_BYTE_BGRA_PRE} ({@code Pixels.cpp:242}), which is the
     * {@code @Native}-pinned 1 of {@code Pixels.java:55}. Asserting both the constant and the literal
     * is what makes the Java-side replacement provably the same value.
     */
    @Test
    public void nativeFormatIsByteBgraPre() {
        assertEquals(Pixels.Format.BYTE_BGRA_PRE, WinGlassNativeShim.pixelsNativeFormat());
        assertEquals(1, WinGlassNativeShim.pixelsNativeFormat());
    }

    // ---------------------------------------------------------------------------------------------
    // WinPixels._fillDirectByteBuffer: the four source paths
    // ---------------------------------------------------------------------------------------------

    @Test
    public void directByteSourceIsCopiedToTheDestinationBase() {
        byte[] image = byteImage();
        ByteBuffer source = ByteBuffer.allocateDirect(SIZE);
        source.duplicate().put(image);
        ByteBuffer destination = destination(SIZE);

        fill(WinGlassNativeShim.newPixels(WIDTH, HEIGHT, source), destination);

        assertArrayEquals(image, bytesOf(destination));
    }

    /**
     * A heap source is read through its array plus {@code arrayOffset()} ({@code Pixels.java:246},
     * {@code Utils.h:472}), so a slice that does not start at element 0 has to land at the same place
     * as the direct buffer above.
     */
    @Test
    public void heapByteSourceIsReadFromItsArrayOffset() {
        byte[] image = byteImage();
        ByteBuffer backing = ByteBuffer.allocate(SIZE + 7);
        backing.position(7);
        ByteBuffer source = backing.slice();
        source.duplicate().put(image);
        assertEquals(7, source.arrayOffset());
        ByteBuffer destination = destination(SIZE);

        fill(WinGlassNativeShim.newPixels(WIDTH, HEIGHT, source), destination);

        assertArrayEquals(image, bytesOf(destination));
    }

    /**
     * An {@code IntBuffer} source is raw memory to the C ({@code memcpy}, {@code Pixels.cpp:296}), so
     * the bytes that arrive are the ints in the buffer's own byte order - native order here.
     */
    @Test
    public void directIntSourceIsCopiedAsRawNativeOrderBytes() {
        int[] pixels = intImage();
        IntBuffer source = ByteBuffer.allocateDirect(SIZE).order(ByteOrder.nativeOrder()).asIntBuffer();
        source.duplicate().put(pixels);
        ByteBuffer destination = destination(SIZE);

        fill(WinGlassNativeShim.newPixels(WIDTH, HEIGHT, source), destination);

        assertArrayEquals(imageOf(pixels, ByteOrder.nativeOrder()), bytesOf(destination));
    }

    /** {@code arrayOffset()} on an {@code int[]} counts elements, not bytes ({@code Utils.h:472}). */
    @Test
    public void heapIntSourceIsReadFromItsArrayOffsetInElements() {
        int[] pixels = intImage();
        IntBuffer backing = IntBuffer.allocate(pixels.length + 3);
        backing.position(3);
        IntBuffer source = backing.slice();
        source.duplicate().put(pixels);
        assertEquals(3, source.arrayOffset());
        ByteBuffer destination = destination(SIZE);

        fill(WinGlassNativeShim.newPixels(WIDTH, HEIGHT, source), destination);

        assertArrayEquals(imageOf(pixels, ByteOrder.nativeOrder()), bytesOf(destination));
    }

    /**
     * The one case the original test matrix had no entry for: an {@code IntBuffer} viewed
     * big-endian over direct memory. The C copies the bytes that are in memory, so the destination
     * gets big-endian pixels; a Java implementation that read the ints through the view and wrote
     * them back through a native-order view would silently swap every pixel.
     * {@code com.sun.glass.ui.Pixels} makes byte order the caller's business ({@code Pixels.java:44-52}),
     * so raw memory is the contract.
     */
    @Test
    public void bigEndianIntSourceIsCopiedAsTheRawBytesItHoldsInMemory() {
        int[] pixels = intImage();
        IntBuffer source = ByteBuffer.allocateDirect(SIZE).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        source.duplicate().put(pixels);
        assertEquals(ByteOrder.BIG_ENDIAN, source.order());
        ByteBuffer destination = destination(SIZE);

        fill(WinGlassNativeShim.newPixels(WIDTH, HEIGHT, source), destination);

        assertArrayEquals(imageOf(pixels, ByteOrder.BIG_ENDIAN), bytesOf(destination));
    }

    // ---------------------------------------------------------------------------------------------
    // WinPixels._fillDirectByteBuffer: the destination quirks and the silent rejections
    // ---------------------------------------------------------------------------------------------

    /**
     * The C wrote through {@code GetDirectBufferAddress} ({@code Pixels.cpp:292}), which is the base
     * of the buffer, and never touched its position.
     */
    @Test
    public void destinationPositionIsIgnoredAndLeftAlone() {
        byte[] image = byteImage();
        ByteBuffer destination = destination(SIZE);
        destination.position(8);

        fill(bytePixels(image), destination);

        assertArrayEquals(image, bytesOf(destination));
        assertEquals(8, destination.position());
    }

    /** {@code memcpy} is byte-order blind, and so is the destination's declared order. */
    @Test
    public void destinationByteOrderIsIgnoredAndLeftAlone() {
        byte[] image = byteImage();
        ByteBuffer destination = ByteBuffer.allocateDirect(SIZE).order(ByteOrder.BIG_ENDIAN);

        fill(bytePixels(image), destination);

        assertArrayEquals(image, bytesOf(destination));
        assertEquals(ByteOrder.BIG_ENDIAN, destination.order());
    }

    /**
     * {@code Pixels.cpp:287-290} compares against {@code GetDirectBufferCapacity}, not the remaining
     * bytes, and returns silently when it is short: no exception, no partial copy.
     */
    @Test
    public void tooSmallDestinationIsLeftUntouched() {
        ByteBuffer destination = destination(SIZE - 1);

        fill(bytePixels(byteImage()), destination);

        assertArrayEquals(new byte[SIZE - 1], bytesOf(destination));
    }

    /**
     * A heap destination has no direct address ({@code Pixels.cpp:292-295}) and is rejected as
     * silently as a short one.
     */
    @Test
    public void heapDestinationIsLeftUntouched() {
        ByteBuffer destination = ByteBuffer.allocate(SIZE);

        fill(bytePixels(byteImage()), destination);

        assertArrayEquals(new byte[SIZE], bytesOf(destination));
    }

    /** {@code Pixels.cpp:277-279}: a null destination is a no-op, not a {@code NullPointerException}. */
    @Test
    public void nullDestinationIsIgnored() {
        fill(bytePixels(byteImage()), null);
    }

    // ---------------------------------------------------------------------------------------------
    // WinPixels: the two deliberate differences from the JNI, and what is left native
    // ---------------------------------------------------------------------------------------------

    /**
     * A read-only heap source works. It could not under the JNI: {@code Pixels.attachData} calls
     * {@code array()} on a non-direct buffer ({@code Pixels.java:245}), which throws
     * {@code ReadOnlyBufferException} inside the upcall, {@code Pixels::Pixels} swallows it
     * ({@code Pixels.cpp:156}) and the C then reads uninitialised {@code width}/{@code height} and
     * {@code memcpy}s from a {@code NULL} pointer. Reachable: {@code Clipboard.java:234} and
     * {@code QuantumClipboard.java:314} hand {@code createPixels} a caller-supplied {@code slice()}.
     * This is a fix the migration gets for free, and it is asserted so that it stays one.
     */
    @Test
    public void readOnlyHeapSourceIsCopiedInsteadOfCrashing() {
        byte[] image = byteImage();
        ByteBuffer source = ByteBuffer.allocate(SIZE);
        source.duplicate().put(image);
        ByteBuffer destination = destination(SIZE);

        fill(WinGlassNativeShim.newPixels(WIDTH, HEIGHT, source.asReadOnlyBuffer()), destination);

        assertArrayEquals(image, bytesOf(destination));
    }

    /**
     * The other difference, in the other direction: a read-only <em>destination</em> is refused. The
     * C wrote through it regardless, because {@code GetDirectBufferAddress} ({@code Pixels.cpp:292})
     * hands back the address of a read-only view as readily as of a writable one. Nothing in the tree
     * passes one - {@code Pixels.asByteBuffer} allocates the buffer it fills
     * ({@code Pixels.java:214-221}) - and silently defeating {@code asReadOnlyBuffer()} is not
     * behaviour worth preserving.
     */
    @Test
    public void readOnlyDestinationIsRefusedRatherThanWrittenThrough() {
        Pixels pixels = bytePixels(byteImage());
        ByteBuffer destination = destination(SIZE).asReadOnlyBuffer();

        assertThrows(ReadOnlyBufferException.class, () -> fill(pixels, destination));
    }

    /**
     * Nothing is left {@code native} on {@code WinPixels}. {@code _attachInt} and {@code _attachByte}, the
     * return leg of the {@code Pixels.attachData} upcall, are {@code UnsupportedOperationException}
     * throwers now that {@code Pixels::Pixels(JNIEnv*, jobject)}, the only thing
     * that dialled it, has been deleted; asserted as a count rather than left to a grep.
     */
    @Test
    public void winPixelsDeclaresNoNativeMethod() {
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinPixels"));
    }

    // ---------------------------------------------------------------------------------------------
    // WinCursor: symbols, constants and the two SDK layouts
    // ---------------------------------------------------------------------------------------------

    /**
     * The cursor code binds five OS entry points instead of adding a {@code gwin_*} export for them, so
     * "does this symbol exist in this DLL" is the check that the deleted C is really replaceable.
     */
    @Test
    public void everyCursorSymbolResolvesInItsLibrary() {
        for (String symbol : List.of("gdi32!CreateBitmap", "gdi32!CreateDIBSection",
                "gdi32!DeleteObject", "gdi32!GdiFlush", "user32!CreateIconIndirect",
                "user32!ShowCursor", "user32!GetSystemMetrics")) {
            assertTrue(WinGlassNativeShim.resolves(symbol), symbol);
        }
        assertFalse(WinGlassNativeShim.resolves("gdi32!NoSuchFunctionForJfx"));
    }

    /** The winuser.h indices {@code GlassCursor.cpp:188,189} passed to {@code GetSystemMetrics}. */
    @Test
    public void cursorConstantsMatchTheWindowsHeaders() {
        assertEquals(13, WinGlassNativeShim.constant("SM_CXCURSOR"));
        assertEquals(14, WinGlassNativeShim.constant("SM_CYCURSOR"));
        assertEquals(0, WinGlassNativeShim.constant("BI_RGB"));
        assertEquals(0, WinGlassNativeShim.constant("DIB_RGB_COLORS"));
    }

    /** {@code BITMAPINFOHEADER} (wingdi.h): 40 bytes, two {@code WORD}s, no padding anywhere. */
    @Test
    public void bitmapInfoHeaderLayoutIsTheWingdiOne() {
        assertEquals(40, WinGlassNativeShim.layoutByteSize("BITMAPINFOHEADER"));
        assertEquals(0, offset("BITMAPINFOHEADER", "biSize"));
        assertEquals(4, offset("BITMAPINFOHEADER", "biWidth"));
        assertEquals(8, offset("BITMAPINFOHEADER", "biHeight"));
        assertEquals(12, offset("BITMAPINFOHEADER", "biPlanes"));
        assertEquals(14, offset("BITMAPINFOHEADER", "biBitCount"));
        assertEquals(16, offset("BITMAPINFOHEADER", "biCompression"));
        assertEquals(20, offset("BITMAPINFOHEADER", "biSizeImage"));
        assertEquals(24, offset("BITMAPINFOHEADER", "biXPelsPerMeter"));
        assertEquals(28, offset("BITMAPINFOHEADER", "biYPelsPerMeter"));
        assertEquals(32, offset("BITMAPINFOHEADER", "biClrUsed"));
        assertEquals(36, offset("BITMAPINFOHEADER", "biClrImportant"));
    }

    /**
     * {@code ICONINFO} (winuser.h) on x64: 32 bytes, with four bytes of padding after
     * {@code yHotspot} that only the 8-byte handles force. Getting this wrong is the failure mode the
     * layout exists to prevent - a hotspot written at 20 instead of 8 would be silently ignored.
     */
    @Test
    public void iconInfoLayoutIsTheWinuserOneOnX64() {
        assertEquals(32, WinGlassNativeShim.layoutByteSize("ICONINFO"));
        assertEquals(0, offset("ICONINFO", "fIcon"));
        assertEquals(4, offset("ICONINFO", "xHotspot"));
        assertEquals(8, offset("ICONINFO", "yHotspot"));
        assertEquals(16, offset("ICONINFO", "hbmMask"));
        assertEquals(24, offset("ICONINFO", "hbmColor"));
    }

    /** {@code BITMAP} (wingdi.h) on x64, which only the test's read-back fills. */
    @Test
    public void bitmapLayoutIsTheWingdiOneOnX64() {
        assertEquals(32, WinGlassNativeShim.layoutByteSize("BITMAP"));
        assertEquals(4, offset("BITMAP", "bmWidth"));
        assertEquals(8, offset("BITMAP", "bmHeight"));
        assertEquals(18, offset("BITMAP", "bmBitsPixel"));
        assertEquals(24, offset("BITMAP", "bmBits"));
    }

    @Test
    public void winCursorDeclaresNoNativeMethods() {
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinCursor"));
    }

    // ---------------------------------------------------------------------------------------------
    // WinCursor.getBestSize_impl
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code GlassCursor.cpp:183-190} ignored both arguments, and this is the assertion that keeps
     * anyone from "fixing" that: it needs no desktop, because whatever {@code GetSystemMetrics}
     * answers, it has to answer the same thing twice.
     */
    @Test
    public void bestSizeIgnoresBothArguments() {
        assertArrayEquals(WinGlassNativeShim.cursorBestSize(10, 20),
                WinGlassNativeShim.cursorBestSize(999, 1));
    }

    /** ... and it is exactly {@code GetSystemMetrics(SM_CXCURSOR)} by {@code (SM_CYCURSOR)}. */
    @Test
    public void bestSizeIsTheSystemCursorSize() {
        requireCursors();
        int[] best = WinGlassNativeShim.cursorBestSize(10, 20);
        assertArrayEquals(new int[] {
                WinGlassNativeShim.getSystemMetrics(WinGlassNativeShim.constant("SM_CXCURSOR")),
                WinGlassNativeShim.getSystemMetrics(WinGlassNativeShim.constant("SM_CYCURSOR"))},
                best);
        assertTrue(best[0] > 0 && best[1] > 0, "a desktop session has a cursor size: " + best[0]
                + "x" + best[1]);
    }

    // ---------------------------------------------------------------------------------------------
    // WinCursor.setVisible_impl and the ShowCursor display counter
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code ShowCursor} keeps a counter, not a flag: {@code TRUE} adds one, {@code FALSE} takes one
     * away, and the pointer is drawn while the count is not negative. Every assertion here is
     * relative, and the {@code finally} puts the counter back where it was found even when one of
     * them fails - a test that leaves it below zero leaves the machine without a mouse pointer.
     */
    @Test
    public void showCursorIsACounterThatMovesOneStepPerCall() {
        requireCursors();
        int steps = 0;
        try {
            int up = WinGlassNativeShim.showCursor(true);
            steps++;
            int down = WinGlassNativeShim.showCursor(false);
            steps--;
            assertEquals(up - 1, down, "FALSE takes exactly one off what TRUE added");
            int downAgain = WinGlassNativeShim.showCursor(false);
            steps--;
            assertEquals(down - 1, downAgain, "the counter keeps going down, it is not a flag");
            int upAgain = WinGlassNativeShim.showCursor(true);
            steps++;
            assertEquals(down, upAgain, "and comes back up one step at a time");
        } finally {
            restoreCursorCounter(steps);
        }
    }

    /**
     * The latch of {@code GlassCursor.cpp:167-175}, which is what keeps that counter bounded: one
     * {@code ShowCursor} per actual change and none at all for a repeat. Asserted through the counter
     * rather than by reading the field, so it is the behaviour that is pinned.
     */
    @Test
    public void setVisibleCallsShowCursorOnlyOnAChange() {
        requireCursors();
        int resting = restingCursorCounter();
        // The latch starts at true (GlassCursor.cpp:168), so this one is skipped entirely.
        WinGlassNativeShim.cursorSetVisible(true);
        assertEquals(resting, restingCursorCounter(), "setVisible(true) on a visible cursor calls nothing");
        try {
            WinGlassNativeShim.cursorSetVisible(false);
            assertEquals(resting - 1, restingCursorCounter(), "the first hide is one ShowCursor(FALSE)");
            WinGlassNativeShim.cursorSetVisible(false);
            assertEquals(resting - 1, restingCursorCounter(), "the second hide is latched away");
        } finally {
            WinGlassNativeShim.cursorSetVisible(true);
        }
        assertEquals(resting, restingCursorCounter(), "and showing it again puts the counter back");
    }

    // ---------------------------------------------------------------------------------------------
    // WinCursor._createCursor: the ICONINFO sequence, read back from Windows
    // ---------------------------------------------------------------------------------------------

    /**
     * The parity oracle for the sequence that was {@code Pixels::CreateIcon}: build a cursor from a
     * 16x16 pattern whose alpha takes 0x00, 0x80 and 0xFF, then ask Windows what it made of it. The
     * hotspot is passed through unclamped ({@code Pixels.cpp:143-144}), the colour bitmap keeps the
     * size it was given, and the pixels arrive byte for byte - including the alpha byte, which is the
     * one thing about {@code GetDIBits} on an icon bitmap that had to be measured rather than assumed.
     */
    @Test
    public void customCursorKeepsItsHotspotAndEveryPixelByte() {
        requireCursors();
        int[] pattern = cursorPattern();
        long cursor = WinGlassNativeShim.cursorCreateCustom(CURSOR_SIDE, CURSOR_SIDE,
                directInts(pattern), 3, 11);
        try {
            assertNotEquals(0L, cursor);
            int[] readBack = WinGlassNativeShim.readCursor(cursor);
            assertNotNull(readBack);
            assertArrayEquals(new int[] {3, 11, CURSOR_SIDE, CURSOR_SIDE},
                    Arrays.copyOf(readBack, 4));
            assertArrayEquals(pattern, Arrays.copyOfRange(readBack, 4, readBack.length));
        } finally {
            destroy(cursor);
        }
    }

    /**
     * The buffer-crossing table, asserted rather than argued: direct or heap, bytes or ints, with or
     * without an {@code arrayOffset}, the same image has to reach GDI unchanged. The JNI took two
     * quite different routes to these four ({@code GetDirectBufferAddress} versus a pinned array plus
     * an offset in elements); the Java takes one, and this is what proves it is the same one.
     */
    @Test
    public void everyBufferFlavourProducesTheSameCursor() {
        requireCursors();
        int[] pattern = cursorPattern();
        byte[] image = imageOf(pattern, ByteOrder.nativeOrder());
        List<Buffer> sources = List.of(directInts(pattern), directBytes(image),
                heapBytesAtOffset(image, 7), heapIntsAtOffset(pattern, 3));
        for (Buffer source : sources) {
            long cursor = WinGlassNativeShim.cursorCreateCustom(CURSOR_SIDE, CURSOR_SIDE, source,
                    1, 2);
            try {
                assertNotEquals(0L, cursor, source.getClass().getSimpleName());
                int[] readBack = WinGlassNativeShim.readCursor(cursor);
                assertNotNull(readBack, source.getClass().getSimpleName());
                assertArrayEquals(new int[] {1, 2, CURSOR_SIDE, CURSOR_SIDE},
                        Arrays.copyOf(readBack, 4), source.getClass().getSimpleName());
                assertArrayEquals(pattern, Arrays.copyOfRange(readBack, 4, readBack.length),
                        source.getClass().getSimpleName());
            } finally {
                destroy(cursor);
            }
        }
    }

    /**
     * A buffer's position and limit are ignored, as {@code GetDirectBufferAddress} and
     * {@code array()} ignored them ({@code Utils.h:454-466}): the whole buffer from element 0 is what
     * the cursor is built from.
     */
    @Test
    public void sourcePositionAndLimitAreIgnored() {
        requireCursors();
        int[] pattern = cursorPattern();
        IntBuffer source = directInts(pattern);
        source.position(4).limit(8);
        long cursor = WinGlassNativeShim.cursorCreateCustom(CURSOR_SIDE, CURSOR_SIDE, source, 0, 0);
        try {
            assertNotEquals(0L, cursor);
            assertArrayEquals(pattern,
                    Arrays.copyOfRange(WinGlassNativeShim.readCursor(cursor), 4,
                            4 + pattern.length));
            assertEquals(4, source.position(), "the caller's buffer is not disturbed");
            assertEquals(8, source.limit());
        } finally {
            destroy(cursor);
        }
    }

    /**
     * The rejections, all silent and all returning 0, as every failure of
     * {@code Java_com_sun_glass_ui_win_WinCursor__1createCursor} was ({@code Pixels.cpp:146} asserts
     * only in a debug build). The dimension guards are the ones {@code com.sun.glass.ui.Pixels}'
     * constructor used to make on the JNI's behalf ({@code Pixels.java:97-105}); nothing reaches the
     * OS on these paths, so they need no desktop.
     */
    @Test
    public void impossibleArgumentsAreRejectedSilently() {
        ByteBuffer enough = ByteBuffer.allocateDirect(CURSOR_SIDE * CURSOR_SIDE * 4);
        assertEquals(0L, WinGlassNativeShim.cursorCreateCustom(0, CURSOR_SIDE, enough, 0, 0));
        assertEquals(0L, WinGlassNativeShim.cursorCreateCustom(CURSOR_SIDE, 0, enough, 0, 0));
        assertEquals(0L, WinGlassNativeShim.cursorCreateCustom(-1, CURSOR_SIDE, enough, 0, 0));
        assertEquals(0L, WinGlassNativeShim.cursorCreateCustom(CURSOR_SIDE, -1, enough, 0, 0));
        assertEquals(0L, WinGlassNativeShim.cursorCreateCustom(Integer.MAX_VALUE, Integer.MAX_VALUE,
                enough, 0, 0));
        // One pixel short of what width * height * 4 needs.
        assertEquals(0L, WinGlassNativeShim.cursorCreateCustom(CURSOR_SIDE, CURSOR_SIDE,
                ByteBuffer.allocateDirect(CURSOR_SIDE * CURSOR_SIDE * 4 - 1), 0, 0));
    }

    /**
     * A hotspot outside the cursor is passed straight through, unclamped and unvalidated, because
     * that is what {@code Pixels.cpp:143-144} did with the two {@code jint}s it was handed.
     */
    @Test
    public void hotspotIsNotClamped() {
        requireCursors();
        long cursor = WinGlassNativeShim.cursorCreateCustom(CURSOR_SIDE, CURSOR_SIDE,
                directInts(cursorPattern()), CURSOR_SIDE + 40, CURSOR_SIDE + 90);
        try {
            assertNotEquals(0L, cursor);
            int[] readBack = WinGlassNativeShim.readCursor(cursor);
            assertNotNull(readBack);
            assertEquals(CURSOR_SIDE + 40, readBack[0]);
            assertEquals(CURSOR_SIDE + 90, readBack[1]);
        } finally {
            destroy(cursor);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static long offset(String struct, String field) {
        return WinGlassNativeShim.offset(struct, field);
    }

    private static void destroy(long cursor) {
        if (cursor != 0) {
            WinGlassNativeShim.destroyCursor(cursor);
        }
    }

    /**
     * The cursor display counter, sampled without moving it: {@code ShowCursor} has no read-only
     * form, so a {@code TRUE} and a {@code FALSE} are paired and the second one's answer is the
     * resting value.
     */
    private static int restingCursorCounter() {
        int ignoredAfterShow = WinGlassNativeShim.showCursor(true);
        return WinGlassNativeShim.showCursor(false);
    }

    /** Undoes {@code steps} calls of {@code ShowCursor}, whichever way they went. */
    private static void restoreCursorCounter(int steps) {
        for (int i = 0; i < steps; i++) {
            WinGlassNativeShim.showCursor(false);
        }
        for (int i = 0; i > steps; i--) {
            WinGlassNativeShim.showCursor(true);
        }
    }

    /** 256 pixels whose four bytes all differ, with the three alphas an icon bitmap might mangle. */
    private static int[] cursorPattern() {
        int[] pixels = new int[CURSOR_SIDE * CURSOR_SIDE];
        for (int i = 0; i < pixels.length; i++) {
            int alpha = switch (i % 3) {
                case 0 -> 0x00;
                case 1 -> 0x80;
                default -> 0xFF;
            };
            pixels[i] = (alpha << 24) | ((i * 3 & 0xFF) << 16) | ((i * 5 & 0xFF) << 8) | (i * 7 & 0xFF);
        }
        return pixels;
    }

    private static IntBuffer directInts(int[] pixels) {
        IntBuffer buffer =
                ByteBuffer.allocateDirect(pixels.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        buffer.duplicate().put(pixels);
        return buffer;
    }

    private static ByteBuffer directBytes(byte[] image) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.length);
        buffer.duplicate().put(image);
        return buffer;
    }

    private static ByteBuffer heapBytesAtOffset(byte[] image, int offset) {
        ByteBuffer backing = ByteBuffer.allocate(image.length + offset);
        backing.position(offset);
        ByteBuffer buffer = backing.slice();
        buffer.duplicate().put(image);
        return buffer;
    }

    private static IntBuffer heapIntsAtOffset(int[] pixels, int offset) {
        IntBuffer backing = IntBuffer.allocate(pixels.length + offset);
        backing.position(offset);
        IntBuffer buffer = backing.slice();
        buffer.duplicate().put(pixels);
        return buffer;
    }

    private static void fill(Pixels pixels, ByteBuffer destination) {
        WinGlassNativeShim.fillDirectByteBuffer(pixels, destination);
    }

    private static Pixels bytePixels(byte[] image) {
        ByteBuffer source = ByteBuffer.allocateDirect(image.length);
        source.duplicate().put(image);
        return WinGlassNativeShim.newPixels(WIDTH, HEIGHT, source);
    }

    /** 48 distinct, non-zero bytes: every index is told apart from every other and from a zero fill. */
    private static byte[] byteImage() {
        byte[] image = new byte[SIZE];
        for (int i = 0; i < image.length; i++) {
            image[i] = (byte) (0x11 + i * 5);
        }
        return image;
    }

    /** 12 pixels whose four bytes all differ, so a byte-order mistake cannot go unnoticed. */
    private static int[] intImage() {
        int[] pixels = new int[WIDTH * HEIGHT];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0x01020304 + i * 0x01010101;
        }
        return pixels;
    }

    /** The bytes {@code pixels} occupies in memory when written through a view of that byte order. */
    private static byte[] imageOf(int[] pixels, ByteOrder order) {
        ByteBuffer bytes = ByteBuffer.allocate(pixels.length * 4).order(order);
        bytes.asIntBuffer().put(pixels);
        return bytes.array();
    }

    private static ByteBuffer destination(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    private static byte[] bytesOf(ByteBuffer buffer) {
        byte[] copy = new byte[buffer.capacity()];
        buffer.duplicate().clear().get(copy);
        return copy;
    }
}
