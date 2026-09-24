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

package test.com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.XShim;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The libX11 binding of X against a live X server: the symbol census, a window created, mapped and measured
 * exactly as X11Screen does it (root, x, y and border width passed as null), an atom, a ClientMessage that
 * travels through the 24-long union and back out of XNextEvent on a second thread, the cursor pixmap path, and
 * the pointer query and warp. Skips only without a {@code DISPLAY}; with one, a libX11 that does not bind or an
 * Xlib call that fails is a failure. libX11 1.8 is thread safe from the start, so calling XInitThreads first
 * is harmless whatever ran in this JVM before.
 */
@EnabledOnOs(OS.LINUX)
public class XNativeTest {

    private static final List<String> SYMBOLS = List.of("XInitThreads", "XLockDisplay", "XUnlockDisplay",
            "XOpenDisplay", "XDefaultScreenOfDisplay", "XRootWindowOfScreen", "XWidthOfScreen", "XHeightOfScreen",
            "XCreateWindow", "XMapWindow", "XStoreName", "XSync", "XGetGeometry", "XNextEvent", "XInternAtom",
            "XSendEvent", "XGrabKeyboard", "XWarpPointer", "XFlush", "XQueryPointer", "XCreateBitmapFromData",
            "XCreatePixmapCursor", "XFreePixmap", "XDefineCursor", "XUndefineCursor");

    private static long display;
    private static long screen;
    private static long root;

    @BeforeAll
    public static void openTheDisplay() {
        assumeTrue(System.getenv("DISPLAY") != null, "no DISPLAY: the X11 binding needs an X server");
        XShim.loadLibrary();
        assertTrue(XShim.isLibraryLoaded());
        XShim.XInitThreads();
        display = XShim.XOpenDisplay(null);
        assertNotEquals(0L, display, "XOpenDisplay(" + System.getenv("DISPLAY") + ")");
        screen = XShim.DefaultScreenOfDisplay(display);
        assertNotEquals(0L, screen);
        root = XShim.RootWindowOfScreen(screen);
        assertNotEquals(0L, root);
        assertTrue(XShim.WidthOfScreen(screen) > 0);
        assertTrue(XShim.HeightOfScreen(screen) > 0);
    }

    /** A 1 x 1 override-redirect InputOutput window selecting the pointer events X11Screen selects. */
    private static long createTestWindow() {
        var attrs = new XShim.XSetWindowAttributesShim();
        attrs.setEventMask(XShim.ButtonPressMask | XShim.ButtonReleaseMask | XShim.PointerMotionMask);
        attrs.setOverrideRedirect(true);
        long window = XShim.XCreateWindow(display, root, 0, 0, 1, 1, 0, XShim.CopyFromParent, XShim.InputOutput,
                XShim.CopyFromParent, XShim.CWOverrideRedirect | XShim.CWEventMask, attrs.address());
        assertNotEquals(0L, window, "XCreateWindow");
        XShim.XMapWindow(display, window);
        XShim.XSync(display, false);
        return window;
    }

    @Test
    public void everyXlibSymbolIsBound() {
        List<String> bound = XShim.boundSymbols();
        for (String name : SYMBOLS) {
            assertTrue(bound.contains(XShim.LIB_X11 + "!" + name), () -> name + " is not bound; bound: " + bound);
        }
        assertEquals(SYMBOLS.size(), bound.size(), () -> "unexpected bindings: " + bound);
    }

    @Test
    public void aWindowIsCreatedMappedAndMeasuredAsX11ScreenDoesIt() {
        long window = createTestWindow();
        int[] width = new int[1];
        int[] height = new int[1];
        int[] depth = new int[1];
        XShim.XGetGeometry(display, window, null, null, null, width, height, null, depth);
        assertEquals(1, width[0]);
        assertEquals(1, height[0]);
        assertTrue(depth[0] > 0, "depth " + depth[0]);
        long[] rootOut = new long[1];
        int[] x = new int[1];
        int[] y = new int[1];
        int[] borderWidth = new int[1];
        XShim.XGetGeometry(display, window, rootOut, x, y, width, height, borderWidth, depth);
        assertEquals(root, rootOut[0]);
        assertEquals(0, x[0]);
        assertEquals(0, y[0]);
        assertEquals(0, borderWidth[0]);
        XShim.XStoreName(display, window, "JavaFX framebuffer container");
        XShim.XSync(display, false);
        assertNotEquals(0L, XShim.XInternAtom(display, "_NET_WM_STATE", false));
        XShim.XFlush(display);
    }

    @Test
    public void aClientMessageTravelsThroughTheUnionAndBackOutOfXNextEvent() throws InterruptedException {
        long window = createTestWindow();
        long atom = XShim.XInternAtom(display, "_JFX_MONOCLE_X_TEST", false);
        assertNotEquals(0L, atom);
        var sent = new XShim.XEventShim();
        sent.buffer().order(ByteOrder.nativeOrder()).putInt(0, XShim.ClientMessage);
        sent.setWindow(window);
        sent.setMessageType(atom);
        sent.setFormat(32);
        // A format-32 ClientMessage carries five 32-bit values on the wire: XSendEvent sends the low 32 bits of
        // each data.l[i] and XNextEvent sign-extends them into the C long. The 64-bit slot is the C layout, not
        // the protocol, so the payloads fit in 32 bits and one has bit 31 set to see the sign extension.
        sent.setDataLong(0, 0x1234_5678L);
        sent.setDataLong(1, 0x8000_0001L);
        sent.setDataLong(2, 42L);
        sent.setDataLong(3, -1L);
        var received = new XShim.XEventShim();
        Thread reader = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                XShim.XNextEvent(display, received.address());
                if (received.getType() == XShim.ClientMessage) {
                    return;
                }
            }
        }, "X11 Input (test)");
        reader.setDaemon(true);
        reader.start();
        XShim.XSendEvent(display, window, false, 0L, sent.address());
        XShim.XFlush(display);
        reader.join(15_000);
        assertFalse(reader.isAlive(), "XNextEvent did not deliver the ClientMessage within 15 s");
        assertEquals(XShim.ClientMessage, received.getType());
        assertEquals(window, received.getWindow(), "xany.window");
        ByteBuffer raw = received.buffer().order(ByteOrder.nativeOrder());
        assertEquals(atom, raw.getLong(40), "xclient.message_type");
        assertEquals(32, raw.getInt(48), "xclient.format");
        assertEquals(0x1234_5678L, raw.getLong(56), "xclient.data.l[0]");
        assertEquals(-2147483647L, raw.getLong(64), "xclient.data.l[1]: 0x80000001 sign-extended from the wire");
        assertEquals(42L, raw.getLong(72), "xclient.data.l[2]");
        assertEquals(-1L, raw.getLong(80), "xclient.data.l[3]: 0xffffffff sign-extended from the wire");
    }

    @Test
    public void theTransparentCursorPathRunsWithoutAnXError() {
        long window = createTestWindow();
        ByteBuffer bits = ByteBuffer.allocateDirect(4);
        long pixmap = XShim.XCreateBitmapFromData(display, window, bits, 1, 1);
        assertNotEquals(0L, pixmap, "XCreateBitmapFromData");
        var black = new XShim.XColorShim();
        black.setRed(0);
        black.setGreen(0);
        black.setBlue(0);
        long cursor = XShim.XCreatePixmapCursor(display, pixmap, pixmap, black.address(), black.address(), 0, 0);
        assertNotEquals(0L, cursor, "XCreatePixmapCursor");
        XShim.XFreePixmap(display, pixmap);
        XShim.XDefineCursor(display, window, cursor);
        XShim.XSync(display, false);
        XShim.XUndefineCursor(display, window);
        XShim.XSync(display, false);
    }

    @Test
    public void thePointerIsQueriedAndWarped() {
        long window = createTestWindow();
        int[] position = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        XShim.XQueryPointer(display, window, position);
        assertNotEquals(Integer.MIN_VALUE, position[0], "win_x written");
        assertNotEquals(Integer.MIN_VALUE, position[1], "win_y written");
        XShim.XWarpPointer(display, 0L, 0L, 0, 0, 0, 0, 0, 0);
        XShim.XFlush(display);
        XShim.XLockDisplay(display);
        XShim.XSync(display, false);
        XShim.XUnlockDisplay(display);
    }
}
