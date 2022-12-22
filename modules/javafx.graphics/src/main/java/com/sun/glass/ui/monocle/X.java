/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.utils.NativeLibLoader;
import java.nio.ByteBuffer;
import java.security.Permission;

/**
 * X provides access to Xlib function calls. Except where noted, each
 * method in the X class corresponds to exactly one Xlib call taking
 * parameters in the same order and returning the same result as the
 * corresponding C function.
 */
class X {

    static {
        NativeLibLoader.loadLibrary("glass_monocle_x11");
    }

    private static Permission permission = new RuntimePermission("loadLibrary.*");

    private static X instance = new X();

    /**
     * Obtains the single instance of X. Calling this method requires
     * the RuntimePermission "loadLibrary.*".
     *
     */
    static X getX() {
        checkPermissions();
        return instance;
    }

    private static void checkPermissions() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(permission);
        }
    }

    static final long None = 0l;
    static final int CopyFromParent = 0;
    static final int InputOutput = 1;

    static final long ButtonPressMask = 1l << 2;
    static final long ButtonReleaseMask = 1l << 3;
    static final long PointerMotionMask = 1l << 6;
    static final long SubstructureRedirectMask = 1l << 19;
    static final long SubstructureNotifyMask = 1l << 20;

    static final long CWOverrideRedirect = 1l << 9;
    static final long CWEventMask = 1l << 11;
    static final long CWCursorMask = 1l << 14;

    static final int ButtonPress = 4;
    static final int ButtonRelease = 5;
    static final int MotionNotify = 6;

    static final int Button1 = 1;
    static final int Button2 = 2;
    static final int Button3 = 3;
    static final int Button4 = 4;
    static final int Button5 = 5;
    // 4th button (aka browser backward button).
    static final int Button8 = 8;
    // 5th button (aka browser forward button).
    static final int Button9 = 9;

    static final long _NET_WM_STATE_REMOVE = 0;
    static final long _NET_WM_STATE_ADD = 1;
    static final long _NET_WM_STATE_TOGGLE = 2;

    static final long GrabModeSync = 0l;
    static final long GrabModeAsync = 1l;

    static final long CurrentTime = 0l;

    /**
     * XSetWindowAttributes wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XSetWindowAttributes extends C.Structure {
        @Override
        native int sizeof();
        static native void setEventMask(long p, long mask);
        static native void setCursor(long p, long cursor);
        static native void setOverrideRedirect(long p, boolean override);
    }

    /**
     * XEvent wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XEvent extends C.Structure {
        XEvent(long p) {
            super(p);
        }
        XEvent() {
            super();
        }
        @Override
        native int sizeof();
        static native int getType(long p);
        static native long getWindow(long p);
        static native void setWindow(long p, long window);
    }

    /**
     * XButtonEvent wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XButtonEvent extends XEvent {
        /** Creates an XButtonEvent from an existing XEvent */
        XButtonEvent(XEvent event) {
            super(event.p);
        }
        static native int getButton(long p);
    }

    /**
     * XMotionEvent wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XMotionEvent extends XEvent {
        /** Creates an XMotionEvent from an existing XEvent */
        XMotionEvent(XEvent event) {
            super(event.p);
        }
        static native int getX(long p);
        static native int getY(long p);
    }

    /**
     * XClientMessageEvent wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XClientMessageEvent extends XEvent {
        /** Creates an XClientMessageEvent from an existing XEvent */
        XClientMessageEvent(XEvent event) {
            super(event.p);
        }
        static native void setMessageType(long p, long atom);
        static native void setFormat(long p, long format);
        static native void setDataLong(long p, int index, long element);
    }

    /**
     * XDisplay wraps the C structure Display.
     */
    static class XDisplay extends C.Structure {
        XDisplay(long p) {
            super(p);
        }
        @Override
        native int sizeof();
    }

    /**
     * XColor wraps the C structure XColor.
     */
    static class XColor extends C.Structure {
        native void setRed(long p, int red);
        native void setGreen(long p, int green);
        native void setBlue(long p, int blue);
        @Override
        native int sizeof();
    }

    private X() {}
    native void XInitThreads();
    native void XLockDisplay(long display);
    native void XUnlockDisplay(long display);
    native long XOpenDisplay(String displayName);
    native long DefaultScreenOfDisplay(long display);
    native long RootWindowOfScreen(long screen);
    native int WidthOfScreen(long screen);
    native int HeightOfScreen(long screen);
    native long XCreateWindow(
            long display, long parent,
            int x, int y, int width, int height,
            int borderWidth, int depth, int windowClass,
            long visual, long valueMask,
            long attributes);
    native void XMapWindow(long display, long window);
    native void XStoreName(long display, long window, String name);
    native void XSync(long display, boolean flush);
    native void XGetGeometry(long display, long window,
                                    long[] root,
                                    int[] x, int[] y,
                                    int[] width, int[] height,
                                    int[] borderWidth, int[] depth);
    native void XNextEvent(long display, long xevent);
    native long XInternAtom(long display, String atomName, boolean onlyIfExists);
    native void XSendEvent(long display, long window, boolean propagate,
                                  long mask, long event);
    native void XGrabKeyboard(long display, long window,
                                     boolean ownerEvents,
                                     long pointerMode,
                                     long keyboardMode,
                                     long time);
    native void XWarpPointer(long display, long src_window,
                                    long dst_window, int src_x, int src_y,
                                    int src_width, int src_height,
                                    int dest_x, int dest_y);
    native void XFlush(long display);
    native void XQueryPointer(long display, long window, int[] position);
    native long XCreateBitmapFromData(long display, long drawable,
                                            ByteBuffer data, int width, int height);
    native long XCreatePixmapCursor(long display, long source, long mask,
                                           long fg, long bg, int x, int y);
    native void XFreePixmap(long display, long pixmap);
    native void XDefineCursor(long display, long window, long cursor);
    native void XUndefineCursor(long display, long window);

}
