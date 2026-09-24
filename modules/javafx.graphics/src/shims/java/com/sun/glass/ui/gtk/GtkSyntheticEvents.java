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

package com.sun.glass.ui.gtk;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * GDK events a test puts on GDK's own event queue, for the window C that no X server without a window manager makes
 * GDK report: the main loop dispatches them like the ones GDK translates from X - through the event handler of
 * {@code _init}, {@code process_events} of {@code GlassApplication.cpp}, to the {@code WindowContext} of the window.
 * Its {@code java.lang.foreign} bindings are its own, and {@code GtkGlassNative} is reached reflectively only, so
 * that it compiles against the JNI build of commit {@code 033187ad90} too, where the scenarios that use it record
 * their goldens. Scenarios only; not a production class.
 */
public final class GtkSyntheticEvents {

    /** {@code GDK_WINDOW_STATE} of {@code GdkEventType}. */
    public static final int GDK_WINDOW_STATE = 32;

    /** {@code GdkWindowState} flags. */
    public static final int STATE_ICONIFIED = 1 << 1;
    public static final int STATE_MAXIMIZED = 1 << 2;
    public static final int STATE_ABOVE = 1 << 5;

    /*
     * GdkEventWindowState on LP64: GdkEventType type @0, GdkWindow *window @8, gint8 send_event @16,
     * GdkWindowState changed_mask @20, GdkWindowState new_window_state @24.
     */
    private static final long TYPE_OFFSET = 0;
    private static final long WINDOW_OFFSET = 8;
    private static final long SEND_EVENT_OFFSET = 16;
    private static final long CHANGED_MASK_OFFSET = 20;
    private static final long NEW_WINDOW_STATE_OFFSET = 24;
    private static final long WINDOW_STATE_SIZE = 32;

    private GtkSyntheticEvents() {
    }

    /**
     * Puts a {@code GdkEventWindowState} for the {@code GdkWindow} of the X11 window {@code xid} on GDK's event queue,
     * as GDK makes one when a window manager changes {@code _NET_WM_STATE} or maps / iconifies the window:
     * {@code gdk_event_new(GDK_WINDOW_STATE)}, the window with a reference of its own (which {@code gdk_event_free}
     * drops), {@code send_event} FALSE, {@code changed_mask} and {@code new_window_state}; {@code gdk_event_put} queues
     * a copy. The main loop dispatches it after the task that put it has returned. FX thread.
     *
     * @throws IllegalArgumentException if GDK knows no window for {@code xid}
     */
    public static void putWindowState(long xid, int changedMask, int newWindowState) {
        try {
            MemorySegment display = (MemorySegment) Gdk.DISPLAY_GET_DEFAULT.invokeExact();
            MemorySegment window = (MemorySegment) Gdk.X11_WINDOW_LOOKUP_FOR_DISPLAY.invokeExact(display, xid);
            if (window.address() == 0) {
                throw new IllegalArgumentException("GDK has no window for X11 window " + xid);
            }
            MemorySegment event = eventOf((MemorySegment) Gdk.EVENT_NEW.invokeExact(GDK_WINDOW_STATE));
            try {
                MemorySegment reference = (MemorySegment) Gdk.G_OBJECT_REF.invokeExact(window);
                event.set(ADDRESS, WINDOW_OFFSET, reference);
                event.set(JAVA_BYTE, SEND_EVENT_OFFSET, (byte) 0);
                event.set(JAVA_INT, CHANGED_MASK_OFFSET, changedMask);
                event.set(JAVA_INT, NEW_WINDOW_STATE_OFFSET, newWindowState);
                int type = event.get(JAVA_INT, TYPE_OFFSET);
                if (type != GDK_WINDOW_STATE) {
                    throw new IllegalStateException("gdk_event_new made an event of type " + type);
                }
                Gdk.EVENT_PUT.invokeExact(event);
            } finally {
                Gdk.EVENT_FREE.invokeExact(event);
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @SuppressWarnings("restricted")
    private static MemorySegment eventOf(MemorySegment event) {
        if (event.address() == 0) {
            throw new IllegalStateException("gdk_event_new returned NULL");
        }
        return event.reinterpret(WINDOW_STATE_SIZE);
    }

    /**
     * How the window, view, drag-and-drop and application C of the loaded glass GTK library reaches Java in this
     * process: {@code tables} once {@code GtkGlassNative} has installed the callback tables of
     * {@code glass_gtk_api.h}, else {@code jni} - the JNI calls of commit {@code 033187ad90}, which is the only path
     * where the facade or its callback tables do not exist.
     */
    public static String upcallPath() {
        try {
            Method installed = Class.forName("com.sun.glass.ui.gtk.GtkGlassNative")
                    .getDeclaredMethod("callbacksInstalled");
            installed.setAccessible(true);
            return Boolean.TRUE.equals(installed.invoke(null)) ? "tables" : "jni";
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return "jni";
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@code setenv(name, value, 1)} of the C library: the process environment GTK reads, which
     * {@code System.getenv} does not see. Needs no glass library, so a scenario can call it before the toolkit
     * starts.
     */
    public static void setenv(String name, String value) {
        try (Arena arena = Arena.ofConfined()) {
            int status = (int) Libc.SETENV.invokeExact(arena.allocateFrom(name), arena.allocateFrom(value), 1);
            if (status != 0) {
                throw new IllegalStateException("setenv(" + name + ") failed");
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code int setenv(const char *name, const char *value, int overwrite)}; bound on first use. */
    private static final class Libc {

        @SuppressWarnings("restricted")
        static final MethodHandle SETENV = Linker.nativeLinker().downcallHandle(
                Linker.nativeLinker().defaultLookup().find("setenv").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

        private Libc() {
        }
    }

    /** The GDK and GObject functions this class calls; bound on first use. */
    private static final class Gdk {

        private static final Linker LINKER = Linker.nativeLinker();
        private static final SymbolLookup GDK = library("libgdk-3.so.0");
        private static final SymbolLookup GOBJECT = library("libgobject-2.0.so.0");

        /** {@code GdkDisplay *gdk_display_get_default(void)}. */
        static final MethodHandle DISPLAY_GET_DEFAULT = bind(GDK, "gdk_display_get_default",
                FunctionDescriptor.of(ADDRESS));
        /** {@code GdkWindow *gdk_x11_window_lookup_for_display(GdkDisplay *display, Window window)}. */
        static final MethodHandle X11_WINDOW_LOOKUP_FOR_DISPLAY = bind(GDK, "gdk_x11_window_lookup_for_display",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));
        /** {@code GdkEvent *gdk_event_new(GdkEventType type)}. */
        static final MethodHandle EVENT_NEW = bind(GDK, "gdk_event_new", FunctionDescriptor.of(ADDRESS, JAVA_INT));
        /** {@code void gdk_event_put(const GdkEvent *event)}. */
        static final MethodHandle EVENT_PUT = bind(GDK, "gdk_event_put", FunctionDescriptor.ofVoid(ADDRESS));
        /** {@code void gdk_event_free(GdkEvent *event)}. */
        static final MethodHandle EVENT_FREE = bind(GDK, "gdk_event_free", FunctionDescriptor.ofVoid(ADDRESS));
        /** {@code gpointer g_object_ref(gpointer object)}. */
        static final MethodHandle G_OBJECT_REF = bind(GOBJECT, "g_object_ref",
                FunctionDescriptor.of(ADDRESS, ADDRESS));

        @SuppressWarnings("restricted")
        private static SymbolLookup library(String name) {
            return SymbolLookup.libraryLookup(name, Arena.global());
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bind(SymbolLookup library, String name, FunctionDescriptor descriptor) {
            return LINKER.downcallHandle(library.find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError("no " + name)), descriptor);
        }

        private Gdk() {
        }
    }
}
