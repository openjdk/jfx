/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.x11;

import com.sun.glass.ui.monocle.util.C;
import com.sun.glass.utils.NativeLibLoader;

public class X {

    static {
        NativeLibLoader.loadLibrary("glass_monocle_x11");
    }

    static final long None = 0l;
    static final int CopyFromParent = 0;
    static final int InputOutput = 1;

    static final long ButtonPressMask = 1l << 2;
    static final long ButtonReleaseMask = 1l << 3;
    static final long PointerMotionMask = 1l << 6;

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

    static class XSetWindowAttributes extends C.Structure {
        @Override
        public native int sizeof();
        static native void setEventMask(long p, long mask);
        static native void setCursor(long p, long cursor);
    }

    static class XEvent extends C.Structure {
        XEvent(long p) {
            super(p);
        }
        XEvent() {
            super();
        }
        @Override
        public native int sizeof();
        static native int getType(long p);
        static native long getWindow(long p);
    }

    static class XButtonEvent extends XEvent {
        XButtonEvent(XEvent event) {
            super(event.p);
        }
        static native int getButton(long p);
    }

    static class XMotionEvent extends XEvent {
        XMotionEvent(XEvent event) {
            super(event.p);
        }
        static native int getX(long p);
        static native int getY(long p);
    }

    static native long XOpenDisplay(String displayName);
    static native long DefaultScreenOfDisplay(long display);
    static native long RootWindowOfScreen(long screen);
    static native int WidthOfScreen(long screen);
    static native int HeightOfScreen(long screen);
    static native long XCreateWindow(
            long display, long parent,
            int x, int y, int width, int height,
            int borderWidth, int depth, int windowClass,
            long visual, long valueMask,
            long attributes);
    static native void XMapWindow(long display, long window);
    static native void XStoreName(long display, long window, String name);
    static native void XSync(long display, boolean flush);
    static native void XGetGeometry(long display, long window,
                                    long[] root,
                                    int[] x, int[] y,
                                    int[] width, int[] height,
                                    int[] borderWidth, int[] depth);
    static native void XNextEvent(long display, long xevent);

}
