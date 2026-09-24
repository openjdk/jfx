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

package com.sun.glass.ui.monocle;

import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.nio.ByteBuffer;
import java.util.List;

/** Exposes {@link X}, its layouts, its structures and its Xlib calls to tests. */
public final class XShim {

    public static final String LIB_X11 = X.LIB_X11;
    public static final int XDISPLAY_PRIVATE_BYTES = X.XDISPLAY_PRIVATE_BYTES;

    public static final long None = X.None;
    public static final int CopyFromParent = X.CopyFromParent;
    public static final int InputOutput = X.InputOutput;
    public static final long ButtonPressMask = X.ButtonPressMask;
    public static final long ButtonReleaseMask = X.ButtonReleaseMask;
    public static final long PointerMotionMask = X.PointerMotionMask;
    public static final long CWOverrideRedirect = X.CWOverrideRedirect;
    public static final long CWEventMask = X.CWEventMask;
    public static final long CWCursorMask = X.CWCursorMask;
    public static final int ButtonPress = X.ButtonPress;
    public static final int ButtonRelease = X.ButtonRelease;
    public static final int MotionNotify = X.MotionNotify;
    public static final int ClientMessage = X.ClientMessage;
    public static final long GrabModeAsync = X.GrabModeAsync;
    public static final long CurrentTime = X.CurrentTime;

    private XShim() {
    }

    public static void loadLibrary() {
        X.loadLibrary();
    }

    public static boolean isLibraryLoaded() {
        return X.isLibraryLoaded();
    }

    public static List<String> boundSymbols() {
        return X.boundSymbols();
    }

    public static void requireLp64(long longByteSize) {
        X.requireLp64(longByteSize);
    }

    public static StructLayout xSetWindowAttributesLayout() {
        return X.XSET_WINDOW_ATTRIBUTES;
    }

    public static SequenceLayout xEventLayout() {
        return X.XEVENT;
    }

    public static StructLayout xAnyEventLayout() {
        return X.XANY_EVENT;
    }

    public static StructLayout xButtonEventLayout() {
        return X.XBUTTON_EVENT;
    }

    public static StructLayout xMotionEventLayout() {
        return X.XMOTION_EVENT;
    }

    public static StructLayout xClientMessageEventLayout() {
        return X.XCLIENT_MESSAGE_EVENT;
    }

    public static StructLayout xColorLayout() {
        return X.XCOLOR;
    }

    /** The size {@code X.XDisplay.sizeof()} answers for a Display at any address. */
    public static int xDisplaySizeof() {
        ByteBuffer memory = ByteBuffer.allocateDirect(X.XDISPLAY_PRIVATE_BYTES);
        return new X.XDisplay(C.getC().GetDirectBufferAddress(memory)).sizeof();
    }

    public static void XInitThreads() {
        X.getX().XInitThreads();
    }

    public static void XLockDisplay(long display) {
        X.getX().XLockDisplay(display);
    }

    public static void XUnlockDisplay(long display) {
        X.getX().XUnlockDisplay(display);
    }

    public static long XOpenDisplay(String displayName) {
        return X.getX().XOpenDisplay(displayName);
    }

    public static long DefaultScreenOfDisplay(long display) {
        return X.getX().DefaultScreenOfDisplay(display);
    }

    public static long RootWindowOfScreen(long screen) {
        return X.getX().RootWindowOfScreen(screen);
    }

    public static int WidthOfScreen(long screen) {
        return X.getX().WidthOfScreen(screen);
    }

    public static int HeightOfScreen(long screen) {
        return X.getX().HeightOfScreen(screen);
    }

    public static long XCreateWindow(long display, long parent, int x, int y, int width, int height,
                                     int borderWidth, int depth, int windowClass, long visual, long valueMask,
                                     long attributes) {
        return X.getX().XCreateWindow(display, parent, x, y, width, height, borderWidth, depth, windowClass, visual,
                valueMask, attributes);
    }

    public static void XMapWindow(long display, long window) {
        X.getX().XMapWindow(display, window);
    }

    public static void XStoreName(long display, long window, String name) {
        X.getX().XStoreName(display, window, name);
    }

    public static void XSync(long display, boolean flush) {
        X.getX().XSync(display, flush);
    }

    public static void XGetGeometry(long display, long window, long[] root, int[] x, int[] y, int[] width,
                                    int[] height, int[] borderWidth, int[] depth) {
        X.getX().XGetGeometry(display, window, root, x, y, width, height, borderWidth, depth);
    }

    public static void XNextEvent(long display, long xevent) {
        X.getX().XNextEvent(display, xevent);
    }

    public static long XInternAtom(long display, String atomName, boolean onlyIfExists) {
        return X.getX().XInternAtom(display, atomName, onlyIfExists);
    }

    public static void XSendEvent(long display, long window, boolean propagate, long mask, long event) {
        X.getX().XSendEvent(display, window, propagate, mask, event);
    }

    public static void XGrabKeyboard(long display, long window, boolean ownerEvents, long pointerMode,
                                     long keyboardMode, long time) {
        X.getX().XGrabKeyboard(display, window, ownerEvents, pointerMode, keyboardMode, time);
    }

    public static void XWarpPointer(long display, long srcWindow, long dstWindow, int srcX, int srcY, int srcWidth,
                                    int srcHeight, int destX, int destY) {
        X.getX().XWarpPointer(display, srcWindow, dstWindow, srcX, srcY, srcWidth, srcHeight, destX, destY);
    }

    public static void XFlush(long display) {
        X.getX().XFlush(display);
    }

    public static void XQueryPointer(long display, long window, int[] position) {
        X.getX().XQueryPointer(display, window, position);
    }

    public static long XCreateBitmapFromData(long display, long drawable, ByteBuffer data, int width, int height) {
        return X.getX().XCreateBitmapFromData(display, drawable, data, width, height);
    }

    public static long XCreatePixmapCursor(long display, long source, long mask, long fg, long bg, int x, int y) {
        return X.getX().XCreatePixmapCursor(display, source, mask, fg, bg, x, y);
    }

    public static void XFreePixmap(long display, long pixmap) {
        X.getX().XFreePixmap(display, pixmap);
    }

    public static void XDefineCursor(long display, long window, long cursor) {
        X.getX().XDefineCursor(display, window, cursor);
    }

    public static void XUndefineCursor(long display, long window) {
        X.getX().XUndefineCursor(display, window);
    }

    /** An {@code X.XSetWindowAttributes} and its setters, over its own address. */
    public static final class XSetWindowAttributesShim {

        private final X.XSetWindowAttributes s = new X.XSetWindowAttributes();

        public long address() {
            return s.p;
        }

        public ByteBuffer buffer() {
            return s.b;
        }

        public int sizeof() {
            return s.sizeof();
        }

        public void setEventMask(long mask) {
            X.XSetWindowAttributes.setEventMask(s.p, mask);
        }

        public void setCursor(long cursor) {
            X.XSetWindowAttributes.setCursor(s.p, cursor);
        }

        public void setOverrideRedirect(boolean override) {
            X.XSetWindowAttributes.setOverrideRedirect(s.p, override);
        }
    }

    /** An {@code X.XEvent} and every accessor of it and of its members, over its own address. */
    public static final class XEventShim {

        private final X.XEvent s = new X.XEvent();

        public long address() {
            return s.p;
        }

        public ByteBuffer buffer() {
            return s.b;
        }

        public int sizeof() {
            return s.sizeof();
        }

        public int getType() {
            return X.XEvent.getType(s.p);
        }

        public long getWindow() {
            return X.XEvent.getWindow(s.p);
        }

        public void setWindow(long window) {
            X.XEvent.setWindow(s.p, window);
        }

        public int getButton() {
            return X.XButtonEvent.getButton(new X.XButtonEvent(s).p);
        }

        public int getX() {
            return X.XMotionEvent.getX(new X.XMotionEvent(s).p);
        }

        public int getY() {
            return X.XMotionEvent.getY(new X.XMotionEvent(s).p);
        }

        public void setMessageType(long atom) {
            X.XClientMessageEvent.setMessageType(new X.XClientMessageEvent(s).p, atom);
        }

        public void setFormat(long format) {
            X.XClientMessageEvent.setFormat(new X.XClientMessageEvent(s).p, format);
        }

        public void setDataLong(int index, long element) {
            X.XClientMessageEvent.setDataLong(new X.XClientMessageEvent(s).p, index, element);
        }
    }

    /** An {@code X.XColor} and its setters, over its own address. */
    public static final class XColorShim {

        private final X.XColor s = new X.XColor();

        public long address() {
            return s.p;
        }

        public ByteBuffer buffer() {
            return s.b;
        }

        public int sizeof() {
            return s.sizeof();
        }

        public void setRed(int red) {
            s.setRed(s.p, red);
        }

        public void setGreen(int green) {
            s.setGreen(s.p, green);
        }

        public void setBlue(int blue) {
            s.setBlue(s.p, blue);
        }
    }
}
