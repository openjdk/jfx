/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.PathElement.sequenceElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * X provides access to Xlib function calls. Except where noted, each
 * method in the X class corresponds to exactly one Xlib call taking
 * parameters in the same order and returning the same result as the
 * corresponding C function.
 * <p>
 * Each call binds the {@code libX11.so.6} function of the same name through {@code java.lang.foreign}, where
 * X11.c of commit 21d5a654f6 wrapped it in a JNI function of the Monocle X11 glass library; the four Xlib
 * macros ({@code DefaultScreenOfDisplay}, {@code RootWindowOfScreen}, {@code WidthOfScreen},
 * {@code HeightOfScreen}) are bound as their function forms. The struct accessors read and write the Xlib
 * layouts below. X is a facade class of this package: every restricted method of the FFM API it uses is
 * called from here. libX11 is bound by the lazy holder {@code Xlib} on {@link #loadLibrary()} (called by
 * X11Platform, where the JNI library used to be loaded) or on the first call, never when this class
 * initialises. The layouts are those of an LP64 libX11: a C {@code long} of any other size is refused with an
 * {@link UnsatisfiedLinkError}, see {@link #requireLp64}. Nothing here is a critical downcall: the "X11 Input"
 * thread blocks in {@link #XNextEvent} while the render thread brackets its calls in {@link #XLockDisplay}.
 */
class X {

    private static X instance = new X();

    /**
     * Obtains the single instance of X.
     *
     */
    static X getX() {
        return instance;
    }

    /** The soname the JNI library was linked against; opened for the life of the process, as that was. */
    static final String LIB_X11 = "libX11.so.6";

    /**
     * {@code sizeof(struct _XDisplay)} of the private {@code Xlibint.h} of libX11 1.8.13 on LP64, which the
     * JNI answered from that header. No public API yields it; it is read only by the
     * {@code monocle.maliSignedStruct} workaround of X11AcceleratedScreen, which copies the Display into a
     * low-address mapping for a Mali EGL that treated the pointer as a signed number. Kept as it was; the
     * follow-up removes the workaround and this constant with it.
     */
    static final int XDISPLAY_PRIVATE_BYTES = 4720;

    /**
     * Binds the Xlib functions behind the calls of this class. An {@link UnsatisfiedLinkError} surfaces here
     * when {@code libX11.so.6} cannot be opened, a symbol does not resolve or the platform is not LP64, as
     * the JNI library loader raised it when the Monocle X11 glass library could not be loaded.
     */
    static void loadLibrary() {
        try {
            Xlib.require();
        } catch (NoClassDefFoundError e) {
            throw unbound(e);
        }
    }

    /** Whether libX11 can be bound: binds it on the first call and answers false instead of throwing. */
    static boolean isLibraryLoaded() {
        try {
            Xlib.require();
            return Xlib.LINKED;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            return false;
        }
    }

    /** {@code libX11.so.6!symbol} of every bound symbol, in binding order. Binds libX11 if that has not happened. */
    static List<String> boundSymbols() {
        synchronized (Xlib.BOUND) {
            return new ArrayList<>(Xlib.BOUND.keySet());
        }
    }

    /**
     * Refuses a C {@code long} that is not 8 bytes: {@code XEvent} is bound as 24 C longs of 8 bytes (192) and
     * XID, Atom, Time and the event masks as 8-byte values, the layouts of an LP64 libX11 (aarch64, x86-64).
     *
     * @param longByteSize the byte size of the canonical {@code long} layout of the linker
     * @throws UnsatisfiedLinkError if it is not 8
     */
    static void requireLp64(long longByteSize) {
        if (longByteSize != 8) {
            throw new UnsatisfiedLinkError("cannot bind " + LIB_X11 + " for Monocle: C long is " + longByteSize
                    + " bytes, but XEvent is bound as 24 C longs of 8 bytes and XID, Atom, Time and the event"
                    + " masks as 8-byte values (LP64 only: aarch64, x86-64)");
        }
    }

    private static UnsatisfiedLinkError unbound(NoClassDefFoundError e) {
        UnsatisfiedLinkError error = new UnsatisfiedLinkError(LIB_X11 + " is not bound: " + e.getMessage());
        error.initCause(e);
        return error;
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException runtime) {
            return runtime;
        }
        if (t instanceof NoClassDefFoundError e) {
            throw unbound(e);
        }
        if (t instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(t);
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
    static final int ClientMessage = 33;

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

    // The Xlib.h layouts of an LP64 libX11 (measured on libX11 1.8.13): Bool is int, XID/Atom/Time/Colormap
    // and the masks are unsigned long, pointers are 8 bytes; padding is explicit.

    /** {@code XSetWindowAttributes}: 112 bytes. */
    static final StructLayout XSET_WINDOW_ATTRIBUTES = MemoryLayout.structLayout(
            JAVA_LONG.withName("background_pixmap"),
            JAVA_LONG.withName("background_pixel"),
            JAVA_LONG.withName("border_pixmap"),
            JAVA_LONG.withName("border_pixel"),
            JAVA_INT.withName("bit_gravity"),
            JAVA_INT.withName("win_gravity"),
            JAVA_INT.withName("backing_store"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("backing_planes"),
            JAVA_LONG.withName("backing_pixel"),
            JAVA_INT.withName("save_under"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("event_mask"),
            JAVA_LONG.withName("do_not_propagate_mask"),
            JAVA_INT.withName("override_redirect"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("colormap"),
            JAVA_LONG.withName("cursor"));

    /** {@code XEvent}: the union is {@code long pad[24]}, 192 bytes; the members below are views of it. */
    static final SequenceLayout XEVENT = MemoryLayout.sequenceLayout(24, JAVA_LONG);

    /** {@code XAnyEvent}: the head every member shares, 40 bytes. */
    static final StructLayout XANY_EVENT = MemoryLayout.structLayout(
            JAVA_INT.withName("type"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("serial"),
            JAVA_INT.withName("send_event"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("display"),
            JAVA_LONG.withName("window"));

    /** {@code XButtonEvent}: 96 bytes. */
    static final StructLayout XBUTTON_EVENT = MemoryLayout.structLayout(
            JAVA_INT.withName("type"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("serial"),
            JAVA_INT.withName("send_event"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("display"),
            JAVA_LONG.withName("window"),
            JAVA_LONG.withName("root"),
            JAVA_LONG.withName("subwindow"),
            JAVA_LONG.withName("time"),
            JAVA_INT.withName("x"),
            JAVA_INT.withName("y"),
            JAVA_INT.withName("x_root"),
            JAVA_INT.withName("y_root"),
            JAVA_INT.withName("state"),
            JAVA_INT.withName("button"),
            JAVA_INT.withName("same_screen"),
            MemoryLayout.paddingLayout(4));

    /** {@code XMotionEvent}: 96 bytes, {@code XButtonEvent} with {@code char is_hint} where the button is. */
    static final StructLayout XMOTION_EVENT = MemoryLayout.structLayout(
            JAVA_INT.withName("type"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("serial"),
            JAVA_INT.withName("send_event"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("display"),
            JAVA_LONG.withName("window"),
            JAVA_LONG.withName("root"),
            JAVA_LONG.withName("subwindow"),
            JAVA_LONG.withName("time"),
            JAVA_INT.withName("x"),
            JAVA_INT.withName("y"),
            JAVA_INT.withName("x_root"),
            JAVA_INT.withName("y_root"),
            JAVA_INT.withName("state"),
            JAVA_BYTE.withName("is_hint"),
            MemoryLayout.paddingLayout(3),
            JAVA_INT.withName("same_screen"),
            MemoryLayout.paddingLayout(4));

    /** {@code XClientMessageEvent}: 96 bytes, its data as the five longs of the union. */
    static final StructLayout XCLIENT_MESSAGE_EVENT = MemoryLayout.structLayout(
            JAVA_INT.withName("type"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("serial"),
            JAVA_INT.withName("send_event"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("display"),
            JAVA_LONG.withName("window"),
            JAVA_LONG.withName("message_type"),
            JAVA_INT.withName("format"),
            MemoryLayout.paddingLayout(4),
            MemoryLayout.sequenceLayout(5, JAVA_LONG).withName("data"));

    /** {@code XColor}: 16 bytes. */
    static final StructLayout XCOLOR = MemoryLayout.structLayout(
            JAVA_LONG.withName("pixel"),
            JAVA_SHORT.withName("red"),
            JAVA_SHORT.withName("green"),
            JAVA_SHORT.withName("blue"),
            JAVA_BYTE.withName("flags"),
            JAVA_BYTE.withName("pad"));

    /** The struct at {@code p} as a segment of the layout's size, the {@code (T *) asPtr(p)} cast of the C. */
    @SuppressWarnings("restricted")
    private static MemorySegment struct(long p, MemoryLayout layout) {
        return MemorySegment.ofAddress(p).reinterpret(layout.byteSize());
    }

    /**
     * XSetWindowAttributes wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XSetWindowAttributes extends C.Structure {

        private static final VarHandle EVENT_MASK = XSET_WINDOW_ATTRIBUTES.varHandle(groupElement("event_mask"));
        private static final VarHandle CURSOR = XSET_WINDOW_ATTRIBUTES.varHandle(groupElement("cursor"));
        private static final VarHandle OVERRIDE_REDIRECT =
                XSET_WINDOW_ATTRIBUTES.varHandle(groupElement("override_redirect"));

        @Override
        int sizeof() {
            return (int) XSET_WINDOW_ATTRIBUTES.byteSize();
        }

        static void setEventMask(long p, long mask) {
            EVENT_MASK.set(struct(p, XSET_WINDOW_ATTRIBUTES), 0L, mask);
        }

        static void setCursor(long p, long cursor) {
            CURSOR.set(struct(p, XSET_WINDOW_ATTRIBUTES), 0L, cursor);
        }

        static void setOverrideRedirect(long p, boolean override) {
            OVERRIDE_REDIRECT.set(struct(p, XSET_WINDOW_ATTRIBUTES), 0L, override ? 1 : 0);
        }
    }

    /**
     * XEvent wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XEvent extends C.Structure {

        private static final VarHandle TYPE = XANY_EVENT.varHandle(groupElement("type"));
        private static final VarHandle WINDOW = XANY_EVENT.varHandle(groupElement("window"));

        XEvent(long p) {
            super(p);
        }

        XEvent() {
            super();
        }

        @Override
        int sizeof() {
            return (int) XEVENT.byteSize();
        }

        static int getType(long p) {
            return (int) TYPE.get(struct(p, XEVENT), 0L);
        }

        static long getWindow(long p) {
            return (long) WINDOW.get(struct(p, XEVENT), 0L);
        }

        static void setWindow(long p, long window) {
            WINDOW.set(struct(p, XEVENT), 0L, window);
        }
    }

    /**
     * XButtonEvent wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XButtonEvent extends XEvent {

        private static final VarHandle BUTTON = XBUTTON_EVENT.varHandle(groupElement("button"));

        /** Creates an XButtonEvent from an existing XEvent */
        XButtonEvent(XEvent event) {
            super(event.p);
        }

        /** The {@code unsigned int} button, as the {@code (jint)} of the C. */
        static int getButton(long p) {
            return (int) BUTTON.get(struct(p, XEVENT), 0L);
        }
    }

    /**
     * XMotionEvent wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XMotionEvent extends XEvent {

        private static final VarHandle X = XMOTION_EVENT.varHandle(groupElement("x"));
        private static final VarHandle Y = XMOTION_EVENT.varHandle(groupElement("y"));

        /** Creates an XMotionEvent from an existing XEvent */
        XMotionEvent(XEvent event) {
            super(event.p);
        }

        static int getX(long p) {
            return (int) X.get(struct(p, XEVENT), 0L);
        }

        static int getY(long p) {
            return (int) Y.get(struct(p, XEVENT), 0L);
        }
    }

    /**
     * XClientMessageEvent wraps the C structure of the same name, defined in
     * Xlib.h
     */
    static class XClientMessageEvent extends XEvent {

        private static final VarHandle MESSAGE_TYPE = XCLIENT_MESSAGE_EVENT.varHandle(groupElement("message_type"));
        private static final VarHandle FORMAT = XCLIENT_MESSAGE_EVENT.varHandle(groupElement("format"));
        private static final VarHandle DATA_L =
                XCLIENT_MESSAGE_EVENT.varHandle(groupElement("data"), sequenceElement());

        /** Creates an XClientMessageEvent from an existing XEvent */
        XClientMessageEvent(XEvent event) {
            super(event.p);
        }

        static void setMessageType(long p, long atom) {
            MESSAGE_TYPE.set(struct(p, XEVENT), 0L, atom);
        }

        /** {@code format} is an {@code int}; the C narrowed the Java long with a cast. */
        static void setFormat(long p, long format) {
            FORMAT.set(struct(p, XEVENT), 0L, (int) format);
        }

        static void setDataLong(long p, int index, long element) {
            DATA_L.set(struct(p, XEVENT), 0L, (long) index, element);
        }
    }

    /**
     * XDisplay wraps the C structure Display.
     */
    static class XDisplay extends C.Structure {
        XDisplay(long p) {
            super(p);
        }

        /** {@link #XDISPLAY_PRIVATE_BYTES}: the private struct size of the libX11 the JNI was built against. */
        @Override
        int sizeof() {
            return XDISPLAY_PRIVATE_BYTES;
        }
    }

    /**
     * XColor wraps the C structure XColor.
     */
    static class XColor extends C.Structure {

        private static final VarHandle RED = XCOLOR.varHandle(groupElement("red"));
        private static final VarHandle GREEN = XCOLOR.varHandle(groupElement("green"));
        private static final VarHandle BLUE = XCOLOR.varHandle(groupElement("blue"));

        /** The {@code unsigned short} components take the low 16 bits, as the {@code (unsigned short)} cast did. */
        void setRed(long p, int red) {
            RED.set(struct(p, XCOLOR), 0L, (short) red);
        }

        void setGreen(long p, int green) {
            GREEN.set(struct(p, XCOLOR), 0L, (short) green);
        }

        void setBlue(long p, int blue) {
            BLUE.set(struct(p, XCOLOR), 0L, (short) blue);
        }

        @Override
        int sizeof() {
            return (int) XCOLOR.byteSize();
        }
    }

    /**
     * libX11, opened once for the process (the JNI library was linked against it and never closed) and bound
     * on first use only, by {@link #loadLibrary()} or the first Xlib call.
     */
    @SuppressWarnings("restricted")
    private static final class Xlib {

        static final Linker LINKER = Linker.nativeLinker();

        /** {@code libX11.so.6!symbol} for every symbol this class binds, in binding order; read by tests. */
        static final Map<String, Long> BOUND = Collections.synchronizedMap(new LinkedHashMap<>());

        static final SymbolLookup LIBRARY = load();

        /** {@code Status XInitThreads(void)}. */
        static final MethodHandle X_INIT_THREADS = bind("XInitThreads", FunctionDescriptor.of(JAVA_INT));

        /** {@code void XLockDisplay(Display *)}. */
        static final MethodHandle X_LOCK_DISPLAY = bind("XLockDisplay", FunctionDescriptor.ofVoid(ADDRESS));

        /** {@code void XUnlockDisplay(Display *)}. */
        static final MethodHandle X_UNLOCK_DISPLAY = bind("XUnlockDisplay", FunctionDescriptor.ofVoid(ADDRESS));

        /** {@code Display *XOpenDisplay(const char *)}. */
        static final MethodHandle X_OPEN_DISPLAY = bind("XOpenDisplay", FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code Screen *XDefaultScreenOfDisplay(Display *)}: the function form of the macro. */
        static final MethodHandle X_DEFAULT_SCREEN_OF_DISPLAY = bind("XDefaultScreenOfDisplay",
                FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code Window XRootWindowOfScreen(Screen *)}: the function form of the macro. */
        static final MethodHandle X_ROOT_WINDOW_OF_SCREEN = bind("XRootWindowOfScreen",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS));

        /** {@code int XWidthOfScreen(Screen *)}: the function form of the macro. */
        static final MethodHandle X_WIDTH_OF_SCREEN = bind("XWidthOfScreen", FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code int XHeightOfScreen(Screen *)}: the function form of the macro. */
        static final MethodHandle X_HEIGHT_OF_SCREEN = bind("XHeightOfScreen",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /**
         * {@code Window XCreateWindow(Display *, Window parent, int x, int y, unsigned width, unsigned height,
         * unsigned border_width, int depth, unsigned class, Visual *, unsigned long valuemask,
         * XSetWindowAttributes *)}.
         */
        static final MethodHandle X_CREATE_WINDOW = bind("XCreateWindow", FunctionDescriptor.of(JAVA_LONG, ADDRESS,
                JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG,
                ADDRESS));

        /** {@code int XMapWindow(Display *, Window)}. */
        static final MethodHandle X_MAP_WINDOW = bind("XMapWindow",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG));

        /** {@code int XStoreName(Display *, Window, const char *)}. */
        static final MethodHandle X_STORE_NAME = bind("XStoreName",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS));

        /** {@code int XSync(Display *, Bool discard)}. */
        static final MethodHandle X_SYNC = bind("XSync", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

        /**
         * {@code Status XGetGeometry(Display *, Drawable, Window *root, int *x, int *y, unsigned *width,
         * unsigned *height, unsigned *border_width, unsigned *depth)}.
         */
        static final MethodHandle X_GET_GEOMETRY = bind("XGetGeometry", FunctionDescriptor.of(JAVA_INT, ADDRESS,
                JAVA_LONG, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        /** {@code int XNextEvent(Display *, XEvent *)}: blocks until an event arrives. */
        static final MethodHandle X_NEXT_EVENT = bind("XNextEvent", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        /** {@code Atom XInternAtom(Display *, const char *, Bool only_if_exists)}. */
        static final MethodHandle X_INTERN_ATOM = bind("XInternAtom",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_INT));

        /** {@code Status XSendEvent(Display *, Window, Bool propagate, long event_mask, XEvent *)}. */
        static final MethodHandle X_SEND_EVENT = bind("XSendEvent",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_LONG, ADDRESS));

        /**
         * {@code int XGrabKeyboard(Display *, Window, Bool owner_events, int pointer_mode, int keyboard_mode, Time)}.
         */
        static final MethodHandle X_GRAB_KEYBOARD = bind("XGrabKeyboard",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG));

        /**
         * {@code int XWarpPointer(Display *, Window src, Window dest, int src_x, int src_y, unsigned src_width,
         * unsigned src_height, int dest_x, int dest_y)}.
         */
        static final MethodHandle X_WARP_POINTER = bind("XWarpPointer", FunctionDescriptor.of(JAVA_INT, ADDRESS,
                JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));

        /** {@code int XFlush(Display *)}. */
        static final MethodHandle X_FLUSH = bind("XFlush", FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /**
         * {@code Bool XQueryPointer(Display *, Window, Window *root, Window *child, int *root_x, int *root_y,
         * int *win_x, int *win_y, unsigned *mask)}.
         */
        static final MethodHandle X_QUERY_POINTER = bind("XQueryPointer", FunctionDescriptor.of(JAVA_INT, ADDRESS,
                JAVA_LONG, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        /**
         * {@code Pixmap XCreateBitmapFromData(Display *, Drawable, const char *data, unsigned width, unsigned height)}.
         */
        static final MethodHandle X_CREATE_BITMAP_FROM_DATA = bind("XCreateBitmapFromData",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT));

        /**
         * {@code Cursor XCreatePixmapCursor(Display *, Pixmap source, Pixmap mask, XColor *fg, XColor *bg,
         * unsigned x, unsigned y)}.
         */
        static final MethodHandle X_CREATE_PIXMAP_CURSOR = bind("XCreatePixmapCursor", FunctionDescriptor.of(JAVA_LONG,
                ADDRESS, JAVA_LONG, JAVA_LONG, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT));

        /** {@code int XFreePixmap(Display *, Pixmap)}. */
        static final MethodHandle X_FREE_PIXMAP = bind("XFreePixmap",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG));

        /** {@code int XDefineCursor(Display *, Window, Cursor)}. */
        static final MethodHandle X_DEFINE_CURSOR = bind("XDefineCursor",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG));

        /** {@code int XUndefineCursor(Display *, Window)}. */
        static final MethodHandle X_UNDEFINE_CURSOR = bind("XUndefineCursor",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG));

        /** True once every handle above is bound; set last, and reading it initialises this class. */
        static final boolean LINKED = !BOUND.isEmpty();

        static void require() {
        }

        private static SymbolLookup load() {
            requireLp64(LINKER.canonicalLayouts().get("long").byteSize());
            try {
                return SymbolLookup.libraryLookup(LIB_X11, Arena.global());
            } catch (IllegalArgumentException e) {
                UnsatisfiedLinkError error = new UnsatisfiedLinkError("cannot load " + LIB_X11 + ": " + e.getMessage());
                error.initCause(e);
                throw error;
            }
        }

        /** Resolves {@code name} in libX11 and links it as an ordinary (never critical) downcall. */
        private static MethodHandle bind(String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = LIBRARY.find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError(LIB_X11 + " does not export " + name));
            BOUND.put(LIB_X11 + "!" + name, symbol.address());
            return LINKER.downcallHandle(symbol, descriptor);
        }
    }

    /** {@code monocle_returnInt} of C.c of commit 21d5a654f6: the first element of a non-null, non-empty array. */
    private static void returnInt(int[] out, MemorySegment value) {
        if (out != null && out.length > 0) {
            out[0] = value.get(JAVA_INT, 0);
        }
    }

    private static void returnLong(long[] out, MemorySegment value) {
        if (out != null && out.length > 0) {
            out[0] = value.get(JAVA_LONG, 0);
        }
    }

    /** The C string of {@code s} in {@code arena}, or NULL for null, as the JNI passed a null jstring. */
    private static MemorySegment cString(Arena arena, String s) {
        return s == null ? MemorySegment.NULL : arena.allocateFrom(s);
    }

    private X() {}

    void XInitThreads() {
        try {
            int ignored = (int) Xlib.X_INIT_THREADS.invokeExact();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XLockDisplay(long display) {
        try {
            Xlib.X_LOCK_DISPLAY.invokeExact(MemorySegment.ofAddress(display));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XUnlockDisplay(long display) {
        try {
            Xlib.X_UNLOCK_DISPLAY.invokeExact(MemorySegment.ofAddress(display));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    long XOpenDisplay(String displayName) {
        try (Arena arena = Arena.ofConfined()) {
            return ((MemorySegment) Xlib.X_OPEN_DISPLAY.invokeExact(cString(arena, displayName))).address();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    long DefaultScreenOfDisplay(long display) {
        try {
            return ((MemorySegment) Xlib.X_DEFAULT_SCREEN_OF_DISPLAY.invokeExact(MemorySegment.ofAddress(display)))
                    .address();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    long RootWindowOfScreen(long screen) {
        try {
            return (long) Xlib.X_ROOT_WINDOW_OF_SCREEN.invokeExact(MemorySegment.ofAddress(screen));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    int WidthOfScreen(long screen) {
        try {
            return (int) Xlib.X_WIDTH_OF_SCREEN.invokeExact(MemorySegment.ofAddress(screen));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    int HeightOfScreen(long screen) {
        try {
            return (int) Xlib.X_HEIGHT_OF_SCREEN.invokeExact(MemorySegment.ofAddress(screen));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    long XCreateWindow(
            long display, long parent,
            int x, int y, int width, int height,
            int borderWidth, int depth, int windowClass,
            long visual, long valueMask,
            long attributes) {
        try {
            return (long) Xlib.X_CREATE_WINDOW.invokeExact(MemorySegment.ofAddress(display), parent, x, y, width,
                    height, borderWidth, depth, windowClass, MemorySegment.ofAddress(visual), valueMask,
                    MemorySegment.ofAddress(attributes));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XMapWindow(long display, long window) {
        try {
            int ignored = (int) Xlib.X_MAP_WINDOW.invokeExact(MemorySegment.ofAddress(display), window);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XStoreName(long display, long window, String name) {
        try (Arena arena = Arena.ofConfined()) {
            int ignored = (int) Xlib.X_STORE_NAME.invokeExact(MemorySegment.ofAddress(display), window,
                    cString(arena, name));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XSync(long display, boolean flush) {
        try {
            int ignored = (int) Xlib.X_SYNC.invokeExact(MemorySegment.ofAddress(display), flush ? 1 : 0);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code XGetGeometry} with every out-parameter answered into scratch memory and copied back only into the
     * arrays that are non-null and non-empty, as the C did through {@code monocle_returnInt/Long}.
     */
    void XGetGeometry(long display, long window,
                                    long[] root,
                                    int[] x, int[] y,
                                    int[] width, int[] height,
                                    int[] borderWidth, int[] depth) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rootOut = arena.allocate(JAVA_LONG);
            MemorySegment xOut = arena.allocate(JAVA_INT);
            MemorySegment yOut = arena.allocate(JAVA_INT);
            MemorySegment widthOut = arena.allocate(JAVA_INT);
            MemorySegment heightOut = arena.allocate(JAVA_INT);
            MemorySegment borderWidthOut = arena.allocate(JAVA_INT);
            MemorySegment depthOut = arena.allocate(JAVA_INT);
            int ignored = (int) Xlib.X_GET_GEOMETRY.invokeExact(MemorySegment.ofAddress(display), window, rootOut,
                    xOut, yOut, widthOut, heightOut, borderWidthOut, depthOut);
            returnLong(root, rootOut);
            returnInt(x, xOut);
            returnInt(y, yOut);
            returnInt(width, widthOut);
            returnInt(height, heightOut);
            returnInt(borderWidth, borderWidthOut);
            returnInt(depth, depthOut);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XNextEvent(long display, long xevent) {
        try {
            int ignored = (int) Xlib.X_NEXT_EVENT.invokeExact(MemorySegment.ofAddress(display),
                    MemorySegment.ofAddress(xevent));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    long XInternAtom(long display, String atomName, boolean onlyIfExists) {
        try (Arena arena = Arena.ofConfined()) {
            return (long) Xlib.X_INTERN_ATOM.invokeExact(MemorySegment.ofAddress(display), cString(arena, atomName),
                    onlyIfExists ? 1 : 0);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XSendEvent(long display, long window, boolean propagate,
                                  long mask, long event) {
        try {
            int ignored = (int) Xlib.X_SEND_EVENT.invokeExact(MemorySegment.ofAddress(display), window,
                    propagate ? 1 : 0, mask, MemorySegment.ofAddress(event));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** The pointer and keyboard modes are {@code int}s; the C narrowed the Java longs with casts. */
    void XGrabKeyboard(long display, long window,
                                     boolean ownerEvents,
                                     long pointerMode,
                                     long keyboardMode,
                                     long time) {
        try {
            int ignored = (int) Xlib.X_GRAB_KEYBOARD.invokeExact(MemorySegment.ofAddress(display), window,
                    ownerEvents ? 1 : 0, (int) pointerMode, (int) keyboardMode, time);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XWarpPointer(long display, long src_window,
                                    long dst_window, int src_x, int src_y,
                                    int src_width, int src_height,
                                    int dest_x, int dest_y) {
        try {
            int ignored = (int) Xlib.X_WARP_POINTER.invokeExact(MemorySegment.ofAddress(display), src_window,
                    dst_window, src_x, src_y, src_width, src_height, dest_x, dest_y);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XFlush(long display) {
        try {
            int ignored = (int) Xlib.X_FLUSH.invokeExact(MemorySegment.ofAddress(display));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code XQueryPointer}, answering the window-relative position into {@code position[0]} and
     * {@code position[1]}; the other six out-parameters are discarded, as the C discarded them.
     */
    void XQueryPointer(long display, long window, int[] position) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment root = arena.allocate(JAVA_LONG);
            MemorySegment child = arena.allocate(JAVA_LONG);
            MemorySegment rootX = arena.allocate(JAVA_INT);
            MemorySegment rootY = arena.allocate(JAVA_INT);
            MemorySegment winX = arena.allocate(JAVA_INT);
            MemorySegment winY = arena.allocate(JAVA_INT);
            MemorySegment mask = arena.allocate(JAVA_INT);
            int ignored = (int) Xlib.X_QUERY_POINTER.invokeExact(MemorySegment.ofAddress(display), window, root,
                    child, rootX, rootY, winX, winY, mask);
            position[0] = winX.get(JAVA_INT, 0);
            position[1] = winY.get(JAVA_INT, 0);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code XCreateBitmapFromData} of a 1 x 1 bitmap: X11.c of commit 21d5a654f6 ignored the width and
     * height arguments and passed 1, 1. The data is the backing memory of a direct buffer from its start,
     * whatever its position, as {@code GetDirectBufferAddress} gave it.
     *
     * @throws IllegalArgumentException if data is not a direct buffer
     */
    long XCreateBitmapFromData(long display, long drawable,
                                            ByteBuffer data, int width, int height) {
        if (!data.isDirect()) {
            throw new IllegalArgumentException("direct ByteBuffer required");
        }
        try {
            MemorySegment bits = MemorySegment.ofBuffer(data.duplicate().clear());
            return (long) Xlib.X_CREATE_BITMAP_FROM_DATA.invokeExact(MemorySegment.ofAddress(display), drawable, bits,
                    1, 1);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code XCreatePixmapCursor} with a black foreground and background at hot spot 0, 0: X11.c of commit
     * 21d5a654f6 ignored the fg, bg, x and y arguments and passed one local XColor with red, green and blue 0
     * for both colours (its pixel and flags were whatever the stack held; here they are 0).
     */
    long XCreatePixmapCursor(long display, long source, long mask,
                                           long fg, long bg, int x, int y) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment black = arena.allocate(XCOLOR);
            return (long) Xlib.X_CREATE_PIXMAP_CURSOR.invokeExact(MemorySegment.ofAddress(display), source, mask,
                    black, black, 0, 0);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XFreePixmap(long display, long pixmap) {
        try {
            int ignored = (int) Xlib.X_FREE_PIXMAP.invokeExact(MemorySegment.ofAddress(display), pixmap);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XDefineCursor(long display, long window, long cursor) {
        try {
            int ignored = (int) Xlib.X_DEFINE_CURSOR.invokeExact(MemorySegment.ofAddress(display), window, cursor);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    void XUndefineCursor(long display, long window) {
        try {
            int ignored = (int) Xlib.X_UNDEFINE_CURSOR.invokeExact(MemorySegment.ofAddress(display), window);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

}
