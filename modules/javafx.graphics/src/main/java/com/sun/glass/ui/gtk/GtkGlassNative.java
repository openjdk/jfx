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

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.CommonDialogs;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.gtk.screencast.XdgDesktopPortal;
import com.sun.javafx.font.JniStringCodec;
import java.io.ByteArrayOutputStream;
import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javafx.scene.paint.Color;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_CHAR_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * The GTK, GDK and GLib calls of the Linux glass peers that used to sit behind JNI in {@code libglassgtk3.so},
 * bound from Java with {@code java.lang.foreign}. Every method here replaces the body of a
 * {@code Java_com_sun_glass_ui_gtk_*} function of commit {@code 033187ad90} whose C did nothing but marshal
 * around system calls; its javadoc names that function and the file it was in, under
 * {@code modules/javafx.graphics/src/main/native-glass/gtk}. There is no C behind these methods any more: the
 * symbols are the system libraries' own, and this is the only class of the package that uses a restricted
 * {@code java.lang.foreign} method.
 *
 * <h2>Libraries</h2>
 * At commit {@code 033187ad90} {@code libglassgtk3.so} was linked against every soname below, and was loaded
 * only after {@code _queryLibrary} of {@code libglass.so} had {@code dlopen}ed {@code libgtk-3.so.0} with
 * {@code RTLD_GLOBAL}. This class opens the same sonames with {@link SymbolLookup#libraryLookup} in
 * {@link Arena#global()}. For the GTK, GDK, GLib, GIO and X11 libraries, which the remaining C still links, that
 * is a {@code dlopen} of a library that is already mapped: the dynamic linker hands back the instance the process
 * has, so the GTK state these calls see is the state the remaining C sees. {@code libXtst.so.6} is the exception:
 * only the robot C called it, {@code libglassgtk3.so} no longer needs it, and this class is the first to map it.
 * <p>
 * All libraries and symbols are resolved when the class initializes, and a missing one is an
 * {@link UnsatisfiedLinkError} naming the library or the symbol. {@code GtkApplication}'s constructor initializes
 * the class ({@link #link}) right after it has called {@code _queryLibrary} and loaded {@code glassgtk3}, and
 * before {@code _initGTK}: that is where a system library missing from the machine made loading
 * {@code glassgtk3} fail before, and where it still fails the toolkit's startup. The class must not be
 * initialized earlier.
 *
 * <h2>Upcalls</h2>
 * The C passed a {@code malloc}ed {@code RunnableContext} holding a JNI global reference as the {@code gpointer}
 * of a GLib source. Here the {@code gpointer} is an id into a registry of this class, the source function is an
 * upcall stub allocated once in {@link Arena#global()}, and no native code holds a Java reference. An upcall
 * target never lets a {@code Throwable} out - that would terminate the JVM - and where the C called
 * {@code LOG_EXCEPTION} ({@code check_and_clear_exception} of {@code glass_general.cpp}) it does what that did:
 * hand the exception to {@link Application#reportException} on the current thread and swallow anything the
 * report itself throws ({@link #reportException}).
 *
 * <h2>Callback tables and the natives of the event code</h2>
 * The window, view, drag-and-drop and application C that stays in {@code libglassgtk3.so} is reached through its
 * flat C ABI ({@code native-glass/gtk/glass_gtk_api.h}), which is the one part of this class that binds
 * {@code libglassgtk3.so} itself (its {@code ggtk_*} exports, through the class loader's lookup). Java calls that C
 * through one {@code ggtk_*} function per former JNI native of {@code GtkWindow}, {@code GtkView},
 * {@code GtkDnDClipboard} and {@code GtkApplication} (all of them but {@code _queryLibrary}), and the C calls Java
 * through the four callback tables, which {@link #installCallbacks} fills with upcall stubs of this class once, at
 * toolkit start. Windows and views cross as {@code long} ids that {@code GtkWindow} and {@code GtkView} assign and
 * resolve; the stubs report exceptions as above and hand the C a status, on which it takes the branch it took after
 * a pending exception, and where a JNI native threw, its replacement here throws the same exception when the
 * downcall has returned.
 * <p>
 * No downcall is {@link Linker.Option#critical critical}: the sources run Java, most of the {@code ggtk_*} functions
 * and the main-loop calls block and re-enter Java through this class, and the frame uploads of
 * {@code GtkView._uploadPixelsIntArray} and {@code _uploadPixelsByteArray}, which cannot call Java, paint a whole
 * frame to the X server - far longer than a critical downcall may keep its thread from a safepoint. Those two copy
 * the frame out of the Java array into a native block of the view ({@link UploadStaging}) and paint from there,
 * where the JNI painted from the array it held with {@code GetPrimitiveArrayCritical}.
 */
final class GtkGlassNative {

    static final String LIB_GTK = "libgtk-3.so.0";
    static final String LIB_GDK = "libgdk-3.so.0";
    static final String LIB_GDK_PIXBUF = "libgdk_pixbuf-2.0.so.0";
    static final String LIB_GOBJECT = "libgobject-2.0.so.0";
    static final String LIB_GLIB = "libglib-2.0.so.0";
    static final String LIB_GIO = "libgio-2.0.so.0";
    static final String LIB_X11 = "libX11.so.6";
    static final String LIB_XTST = "libXtst.so.6";

    /** The C library, through {@link Linker#defaultLookup()}; the name only labels its symbols. */
    static final String LIB_C = "libc.so.6";

    /** {@code G_PRIORITY_HIGH_IDLE} of {@code gmain.h}. */
    static final int G_PRIORITY_HIGH_IDLE = 100;

    private static final Linker LINKER = Linker.nativeLinker();

    /** {@code library!symbol} for every symbol bound, in binding order, with its address; read by tests. */
    private static final Map<String, Long> BOUND = Collections.synchronizedMap(new LinkedHashMap<>());

    private static final SymbolLookup GTK = library(LIB_GTK);
    private static final SymbolLookup GDK = library(LIB_GDK);
    private static final SymbolLookup GDK_PIXBUF = library(LIB_GDK_PIXBUF);
    private static final SymbolLookup GOBJECT = library(LIB_GOBJECT);
    private static final SymbolLookup GLIB = library(LIB_GLIB);
    private static final SymbolLookup GIO = library(LIB_GIO);
    private static final SymbolLookup X11 = library(LIB_X11);
    private static final SymbolLookup XTST = library(LIB_XTST);
    private static final SymbolLookup LIBC = LINKER.defaultLookup();

    /** {@code gboolean (*GSourceFunc)(gpointer user_data)}. */
    private static final FunctionDescriptor SOURCE_FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS);

    private GtkGlassNative() {
    }

    /**
     * Initializes this class, which opens every library and resolves every symbol it binds. Called by the
     * {@code GtkApplication} constructor once the glass GTK library is loaded - right after
     * {@code NativeLibLoader.loadLibrary("glassgtk3")}, or on {@code QUERY_USE_CURRENT} without it: at commit
     * {@code 033187ad90} {@code libglassgtk3.so} needed {@code libXtst.so.6}, so on a machine without it that load
     * threw and the toolkit's startup failed there. Initialized lazily instead, the class would fail first on the
     * invoke-later thread and in the pulse timer, where nothing reports it, and the startup would never finish.
     *
     * @throws UnsatisfiedLinkError if a library or a symbol is missing
     */
    static void link() {
        // the class initializer has done the work
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Timer: GtkTimer._start, GtkTimer._stop (GlassTimer.cpp)
     * ------------------------------------------------------------------------------------------------------- */

    /**
     * {@code guint gdk_threads_add_timeout_full(gint priority, guint interval, GSourceFunc function,
     * gpointer data, GDestroyNotify notify)}.
     */
    private static final MethodHandle GDK_THREADS_ADD_TIMEOUT_FULL = bind(GDK, LIB_GDK,
            "gdk_threads_add_timeout_full",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /** The {@code call_runnable_in_timer} of {@code GlassTimer.cpp}, shared by every timer. */
    private static final MemorySegment TIMER_CALLBACK = upcallStub("onTimerTick", SOURCE_FUNC);

    /**
     * The {@code RunnableContext} of a started timer: {@code runnable} was a JNI global reference,
     * {@code flag} an {@code int} set by {@code _stop}. The fields are volatile where the C's were plain,
     * which changes nothing on the FX thread that starts, stops and ticks every timer in practice.
     */
    private static final class TimerContext {
        volatile Runnable runnable;
        volatile boolean flag;

        TimerContext(Runnable runnable) {
            this.runnable = runnable;
        }
    }

    /**
     * The contexts of the timers whose GLib source still exists, keyed by the id that crosses as the source's
     * {@code gpointer}. An entry leaves exactly where the C {@code free}d the context: in the first tick after
     * {@link #timerStop}. A timer that is never stopped keeps its entry and its runnable for the life of the
     * process, as it kept its global reference.
     */
    private static final Map<Long, TimerContext> TIMERS = new ConcurrentHashMap<>();

    /** Ids start at 1, so a started timer is never 0, which {@code Timer.start} reads as a failure. */
    private static final AtomicLong NEXT_TIMER_ID = new AtomicLong(1);

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkTimer__1start} ({@code GlassTimer.cpp}): a context holding
     * {@code runnable} with the flag clear, then
     * {@code gdk_threads_add_timeout_full(G_PRIORITY_HIGH_IDLE, period, call_runnable_in_timer, context, NULL)};
     * the source id is ignored. The C returned 0 when {@code malloc} failed; the registry cannot fail that way.
     *
     * @return the timer handle {@code Timer} keeps and passes back to {@link #timerStop}
     */
    static long timerStart(Runnable runnable, int period) {
        long id = NEXT_TIMER_ID.getAndIncrement();
        TIMERS.put(id, new TimerContext(runnable));
        try {
            int source = (int) GDK_THREADS_ADD_TIMEOUT_FULL.invokeExact(G_PRIORITY_HIGH_IDLE, period, TIMER_CALLBACK,
                    MemorySegment.ofAddress(id), MemorySegment.NULL);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        return id;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkTimer__1stop} ({@code GlassTimer.cpp}): sets the flag and drops the
     * runnable ({@code DeleteGlobalRef}, {@code runnable = NULL}). The source is not removed here; the next tick
     * sees the flag, forgets the context and removes itself. A handle that is not a started timer returns
     * silently where the C wrote through whatever pointer it was given; {@code Timer.stop} never passes one.
     */
    static void timerStop(long timer) {
        TimerContext context = TIMERS.get(timer);
        if (context == null) {
            return;
        }
        context.flag = true;
        context.runnable = null;
    }

    /**
     * {@code call_runnable_in_timer} ({@code GlassTimer.cpp}), the {@code GSourceFunc} of every timer, on the thread
     * that iterates the default main context (the FX thread). With the flag set it forgets the context and answers
     * {@code FALSE}, which removes the source; otherwise it runs the runnable, if it still has one, and answers
     * {@code TRUE}. A {@code Throwable} from the runnable is reported ({@code LOG_EXCEPTION}) and the timer keeps
     * firing. The C attached the thread when {@code GetEnv} said it was detached and detached it afterwards; an
     * upcall stub does the same by itself.
     */
    static int onTimerTick(MemorySegment data) {
        try {
            long id = data.address();
            TimerContext context = TIMERS.get(id);
            if (context == null) {
                return 0;
            }
            if (context.flag) {
                TIMERS.remove(id);
                return 0;
            }
            Runnable runnable = context.runnable;
            if (runnable != null) {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    reportException(t);
                }
            }
            return 1;
        } catch (Throwable t) {
            reportException(t);
            return 1;
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Invoke-later and nested event loops: GtkApplication._submitForLaterInvocation, enterNestedEventLoopImpl,
     * leaveNestedEventLoopImpl (GlassApplication.cpp)
     * ------------------------------------------------------------------------------------------------------- */

    /** {@code guint gdk_threads_add_idle_full(gint priority, GSourceFunc function, gpointer data, GDestroyNotify)}. */
    private static final MethodHandle GDK_THREADS_ADD_IDLE_FULL = bind(GDK, LIB_GDK, "gdk_threads_add_idle_full",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /** {@code void gtk_main(void)}. */
    private static final MethodHandle GTK_MAIN = bind(GTK, LIB_GTK, "gtk_main", FunctionDescriptor.ofVoid());

    /** {@code void gtk_main_quit(void)}. */
    private static final MethodHandle GTK_MAIN_QUIT = bind(GTK, LIB_GTK, "gtk_main_quit", FunctionDescriptor.ofVoid());

    /** The {@code call_runnable} of {@code GlassApplication.cpp}, shared by every submitted runnable. */
    private static final MemorySegment INVOKE_LATER_CALLBACK = upcallStub("onInvokeLater", SOURCE_FUNC);

    /**
     * Stands in the registry for a {@code null} runnable, which a {@code ConcurrentHashMap} cannot hold: the C
     * took a global reference to {@code NULL} and its {@code CallVoidMethod} on it left a
     * {@code NullPointerException} that {@code LOG_EXCEPTION} reported.
     */
    private static final Runnable NULL_RUNNABLE = () -> {
        throw new NullPointerException();
    };

    /**
     * The runnables submitted and not yet run, keyed by the id that crosses as the idle source's
     * {@code gpointer}; strong, as the JNI global references were. An entry leaves when its idle runs. A runnable
     * whose idle never runs - the loop quit first - stays, as its global reference and context leaked.
     */
    private static final Map<Long, Runnable> RUNNABLES = new ConcurrentHashMap<>();

    private static final AtomicLong NEXT_RUNNABLE_ID = new AtomicLong(1);

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1submitForLaterInvocation} ({@code GlassApplication.cpp}):
     * {@code gdk_threads_add_idle_full(G_PRIORITY_HIGH_IDLE + 30, call_runnable, context, NULL)}, from whatever
     * thread submits (the {@code InvokeLaterDispatcher} thread, or any thread when embedded in SWT). The C printed
     * {@code malloc failed ...} and dropped the runnable when it could not allocate the context; the registry
     * cannot fail that way.
     */
    static void submitForLaterInvocation(Runnable runnable) {
        long id = NEXT_RUNNABLE_ID.getAndIncrement();
        RUNNABLES.put(id, runnable == null ? NULL_RUNNABLE : runnable);
        try {
            int source = (int) GDK_THREADS_ADD_IDLE_FULL.invokeExact(G_PRIORITY_HIGH_IDLE + 30, INVOKE_LATER_CALLBACK,
                    MemorySegment.ofAddress(id), MemorySegment.NULL);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code call_runnable} ({@code GlassApplication.cpp}), the one-shot {@code GSourceFunc} of
     * {@link #submitForLaterInvocation}, on the thread that iterates the default main context: runs the runnable,
     * reports what it throws ({@code LOG_EXCEPTION}), forgets it ({@code DeleteGlobalRef}, {@code free}) and answers
     * {@code FALSE}.
     */
    static int onInvokeLater(MemorySegment data) {
        try {
            Runnable runnable = RUNNABLES.remove(data.address());
            if (runnable != null) {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    reportException(t);
                }
            }
        } catch (Throwable t) {
            reportException(t);
        }
        return 0;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication_enterNestedEventLoopImpl} ({@code GlassApplication.cpp}):
     * {@code gtk_main()}, which runs until the matching {@link #leaveNestedEventLoop}. Everything the loop
     * dispatches - the event handlers of {@code libglassgtk3.so}, which reach Java through the callback tables
     * ({@link #installCallbacks}), and the other upcalls of this class - runs inside this downcall on the FX thread.
     * The event handlers leave no exception pending and create no JNI local reference: every slot of the tables is
     * installed before the first event, and the stubs report what their Java targets throw. Should anything be
     * pending when the downcall returns, it is rethrown here unchanged, checked or not, as the JNI native method
     * threw it.
     */
    static void enterNestedEventLoop() {
        try {
            GTK_MAIN.invokeExact();
        } catch (Throwable t) {
            throw GtkGlassNative.<RuntimeException>rethrow(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkApplication_leaveNestedEventLoopImpl} ({@code GlassApplication.cpp}). */
    static void leaveNestedEventLoop() {
        try {
            GTK_MAIN_QUIT.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Pixels and cursors: GtkPixels._attachInt, GtkPixels._attachByte (GlassPixels.cpp), GtkCursor._createCursor,
     * GtkCursor._getBestSize (GlassCursor.cpp)
     * ------------------------------------------------------------------------------------------------------- */

    /** {@code GDK_COLORSPACE_RGB} of {@code gdk-pixbuf-core.h}. */
    static final int GDK_COLORSPACE_RGB = 0;

    /**
     * An {@code int} stored least significant byte first: {@code R | G << 8 | B << 16 | A << 24} lays out the bytes
     * R, G, B, A.
     */
    private static final ValueLayout.OfInt RGBA_BYTES = JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    /** {@code gpointer g_malloc(gsize n_bytes)}. */
    private static final MethodHandle G_MALLOC = bind(GLIB, LIB_GLIB, "g_malloc",
            FunctionDescriptor.of(ADDRESS, JAVA_LONG));

    /**
     * {@code void g_free(gpointer mem)}, passed as the {@code GdkPixbufDestroyNotify} of every pixbuf an attach
     * creates. The C passed {@code my_free(guchar *pixels, gpointer data)}, whose whole body was
     * {@code g_free(pixels)}; {@code g_free} ignores the second argument the notify receives.
     */
    private static final MemorySegment G_FREE = address(GLIB, LIB_GLIB, "g_free");

    /**
     * {@code GdkPixbuf *gdk_pixbuf_new_from_data(const guchar *data, GdkColorspace colorspace, gboolean has_alpha,
     * int bits_per_sample, int width, int height, int rowstride, GdkPixbufDestroyNotify destroy_fn,
     * gpointer destroy_fn_data)}.
     */
    private static final MethodHandle GDK_PIXBUF_NEW_FROM_DATA = bind(GDK_PIXBUF, LIB_GDK_PIXBUF,
            "gdk_pixbuf_new_from_data", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));

    /** {@code GdkDisplay *gdk_display_get_default(void)}. */
    private static final MethodHandle GDK_DISPLAY_GET_DEFAULT = bind(GDK, LIB_GDK, "gdk_display_get_default",
            FunctionDescriptor.of(ADDRESS));

    /** {@code GdkCursor *gdk_cursor_new_from_pixbuf(GdkDisplay *display, GdkPixbuf *pixbuf, gint x, gint y)}. */
    private static final MethodHandle GDK_CURSOR_NEW_FROM_PIXBUF = bind(GDK, LIB_GDK, "gdk_cursor_new_from_pixbuf",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code void g_object_unref(gpointer object)}. */
    private static final MethodHandle G_OBJECT_UNREF = bind(GOBJECT, LIB_GOBJECT, "g_object_unref",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code guint gdk_display_get_default_cursor_size(GdkDisplay *display)}. */
    private static final MethodHandle GDK_DISPLAY_GET_DEFAULT_CURSOR_SIZE = bind(GDK, LIB_GDK,
            "gdk_display_get_default_cursor_size", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * The private {@code Pixels.attachData(long)}, which the C reached through the cached {@code jPixelsAttachData}
     * of {@code glass_general.cpp}; JNI ignores access, a private lookup in this module is the Java equivalent.
     */
    private static final MethodHandle PIXELS_ATTACH_DATA = pixelsAttachData();

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkPixels__1attachInt} ({@code GlassPixels.cpp}): the guards of the C in
     * its order, then {@code convert_BGRA_to_RGBA} of {@code w * h} ints starting {@code offset} ints into the
     * array, or at the base address of the direct buffer (its position ignored, as {@code GetDirectBufferAddress}
     * ignored it), and a {@code GdkPixbuf} over the result written to {@code *(GdkPixbuf **) ptr}. A heap buffer
     * passed without its array has the capacity -1 {@code GetDirectBufferCapacity} gave it and fails the bounds
     * check. The C held the array with {@code GetPrimitiveArrayCritical} while it copied; here it is read in place.
     */
    static void pixelsAttachInt(long ptr, int w, int h, IntBuffer ints, int[] array, int offset) {
        if (ptr == 0) {
            return;
        }
        if (array == null && ints == null) {
            return;
        }
        if (offset < 0) {
            return;
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        if (w > (((Integer.MAX_VALUE - offset) / 4) / h)) {
            return;
        }
        int numElem = array == null ? directBufferCapacity(ints) : array.length;
        if ((w * h + offset) > numElem) {
            return;
        }
        MemorySegment data = array == null ? directBufferBase(ints) : MemorySegment.ofArray(array);
        attachPixbuf(ptr, data, 4L * offset, w, h);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkPixels__1attachByte} ({@code GlassPixels.cpp}): {@link #pixelsAttachInt}
     * over bytes, {@code offset} counted in bytes and the bounds check in bytes ({@code w * h * 4 + offset}); the
     * pixels are still read as native-order {@code int}s, which assumes the little-endian layout of
     * {@code BYTE_BGRA_PRE}.
     */
    static void pixelsAttachByte(long ptr, int w, int h, ByteBuffer bytes, byte[] array, int offset) {
        if (ptr == 0) {
            return;
        }
        if (array == null && bytes == null) {
            return;
        }
        if (offset < 0) {
            return;
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        if (w > (((Integer.MAX_VALUE - offset) / 4) / h)) {
            return;
        }
        int numElem = array == null ? directBufferCapacity(bytes) : array.length;
        if ((w * h * 4 + offset) > numElem) {
            return;
        }
        MemorySegment data = array == null ? directBufferBase(bytes) : MemorySegment.ofArray(array);
        attachPixbuf(ptr, data, offset, w, h);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkCursor__1createCursor} ({@code GlassCursor.cpp}): a {@code NULL}
     * {@code GdkPixbuf *}, {@code pixels.attachData(&pixbuf)}; unless that threw ({@code EXCEPTION_OCCURED} reported
     * it), {@code gdk_cursor_new_from_pixbuf(gdk_display_get_default(), pixbuf, x, y)}; then
     * {@code g_object_unref(pixbuf)} in every case - on the exception path that is {@code g_object_unref(NULL)} and
     * GLib's critical warning, as before. A {@code null} {@code pixels} is reported as the
     * {@code NullPointerException} without a message that {@code CallVoidMethod} on {@code NULL} left.
     *
     * @return the {@code GdkCursor *}, or 0
     */
    static long cursorCreate(int x, int y, Pixels pixels) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment slot = call.allocate(ADDRESS);
            boolean threw = false;
            try {
                if (pixels == null) {
                    throw new NullPointerException();
                }
                PIXELS_ATTACH_DATA.invokeExact(pixels, slot.address());
            } catch (Throwable t) {
                reportException(t);
                threw = true;
            }
            MemorySegment pixbuf = slot.get(ADDRESS, 0);
            MemorySegment cursor = MemorySegment.NULL;
            if (!threw) {
                MemorySegment display = (MemorySegment) GDK_DISPLAY_GET_DEFAULT.invokeExact();
                cursor = (MemorySegment) GDK_CURSOR_NEW_FROM_PIXBUF.invokeExact(display, pixbuf, x, y);
            }
            G_OBJECT_UNREF.invokeExact(pixbuf);
            return cursor.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkCursor__1getBestSize} ({@code GlassCursor.cpp}): the display's default
     * cursor size in both dimensions, whatever was asked for. A throwing {@code Size} constructor was reported
     * ({@code EXCEPTION_OCCURED}) and answered {@code null}.
     */
    static Size cursorBestSize(int width, int height) {
        int size;
        try {
            MemorySegment display = (MemorySegment) GDK_DISPLAY_GET_DEFAULT.invokeExact();
            size = (int) GDK_DISPLAY_GET_DEFAULT_CURSOR_SIZE.invokeExact(display);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        try {
            return new Size(size, size);
        } catch (Throwable t) {
            reportException(t);
            return null;
        }
    }

    /**
     * The shared tail of both attaches: {@code convert_BGRA_to_RGBA(data + offset, w * 4, h)} and, when that
     * produced memory, {@code *(GdkPixbuf **) ptr = gdk_pixbuf_new_from_data(rgba, GDK_COLORSPACE_RGB, TRUE, 8, w, h,
     * w * 4, g_free, NULL)}.
     */
    @SuppressWarnings("restricted")
    private static void attachPixbuf(long ptr, MemorySegment data, long offset, int w, int h) {
        MemorySegment rgba = convertBgraToRgba(data, offset, w * 4, h);
        if (rgba.address() != 0) {
            try {
                MemorySegment pixbuf = (MemorySegment) GDK_PIXBUF_NEW_FROM_DATA.invokeExact(rgba, GDK_COLORSPACE_RGB,
                        1, 8, w, h, w * 4, G_FREE, MemorySegment.NULL);
                MemorySegment.ofAddress(ptr).reinterpret(ADDRESS.byteSize()).set(ADDRESS, 0, pixbuf);
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    /**
     * {@code convert_BGRA_to_RGBA} ({@code glass_general.cpp}): {@code NULL} for a non-positive stride or height or
     * an {@code int} overflow of {@code height * stride}; otherwise {@code g_malloc(height * stride)} filled from
     * {@code height * stride / 4} native-order ints of {@code pixels} as the bytes R = {@code >>16}, G = {@code >>8},
     * B = {@code >>0}, A = {@code >>24}. The four bytes of a pixel are written as one little-endian {@code int}, which
     * puts exactly those bytes at exactly those addresses on any byte order, in one access instead of four.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment convertBgraToRgba(MemorySegment pixels, long offset, int stride, int height) {
        if (stride <= 0 || height <= 0 || (height > Integer.MAX_VALUE / stride)) {
            return MemorySegment.NULL;
        }
        int size = height * stride;
        MemorySegment out;
        try {
            out = (MemorySegment) G_MALLOC.invokeExact((long) size);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (out.address() == 0) {
            return MemorySegment.NULL;
        }
        out = out.reinterpret(size);
        for (int i = 0; i < size; i += 4) {
            int pixel = pixels.get(JAVA_INT_UNALIGNED, offset + i);
            out.set(RGBA_BYTES, i, (pixel & 0xFF00FF00) | ((pixel >> 16) & 0xFF) | ((pixel & 0xFF) << 16));
        }
        return out;
    }

    /** {@code GetDirectBufferCapacity}: the capacity of a direct buffer, -1 for any other. */
    private static int directBufferCapacity(Buffer buffer) {
        return buffer.isDirect() ? buffer.capacity() : -1;
    }

    /** {@code GetDirectBufferAddress}: the whole buffer from its base address, whatever its position and limit. */
    private static MemorySegment directBufferBase(Buffer buffer) {
        return MemorySegment.ofBuffer(buffer.duplicate().clear());
    }

    private static MethodHandle pixelsAttachData() {
        try {
            return MethodHandles.privateLookupIn(Pixels.class, MethodHandles.lookup()).findVirtual(Pixels.class,
                    "attachData", MethodType.methodType(void.class, long.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Robot and key lock: GtkRobot (GlassRobot.cpp), GtkApplication._isKeyLocked (glass_key.cpp), and the helpers
     * they called: getUIScale (glass_screen.cpp), glass_settings_get_guint_opt and glass_gdk_display_get_pointer
     * (glass_general.cpp), the wrapped_g_settings_schema_* lookups (wrapped.c), and the robot half of glass_key.cpp
     * ------------------------------------------------------------------------------------------------------- */

    /** {@code DEFAULT_DPI} of {@code glass_screen.cpp}. */
    static final int DEFAULT_DPI = 96;

    /** {@code GDK_MOD2_MASK} (Num Lock) of {@code gdktypes.h}. */
    static final int GDK_MOD2_MASK = 1 << 4;

    /** {@code XkbUseCoreKbd} of {@code XKB.h}. */
    static final int XKB_USE_CORE_KBD = 0x0100;

    /** {@code sizeof(XkbStateRec)} on LP64 Linux; {@code group} is its first byte. */
    static final long XKB_STATE_REC_SIZE = 18;

    /** {@code sizeof(GdkKeymapKey)}: {@code guint keycode; gint group; gint level;}. */
    static final long GDK_KEYMAP_KEY_SIZE = 12;

    /** {@code sizeof(GHashTableIter)} on LP64: three pointers, two ints, one pointer. */
    static final long G_HASH_TABLE_ITER_SIZE = 40;

    static final String XTEST_MISSING = "Glass Robot needs XTest extension to work";

    /** {@code Display *gdk_x11_get_default_xdisplay(void)}. */
    private static final MethodHandle GDK_X11_GET_DEFAULT_XDISPLAY = bind(GDK, LIB_GDK,
            "gdk_x11_get_default_xdisplay", FunctionDescriptor.of(ADDRESS));

    /** {@code gint gdk_x11_get_default_screen(void)}. */
    private static final MethodHandle GDK_X11_GET_DEFAULT_SCREEN = bind(GDK, LIB_GDK, "gdk_x11_get_default_screen",
            FunctionDescriptor.of(JAVA_INT));

    /** {@code Display *gdk_x11_display_get_xdisplay(GdkDisplay *display)}. */
    private static final MethodHandle GDK_X11_DISPLAY_GET_XDISPLAY = bind(GDK, LIB_GDK,
            "gdk_x11_display_get_xdisplay", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code GdkScreen *gdk_screen_get_default(void)}. */
    private static final MethodHandle GDK_SCREEN_GET_DEFAULT = bind(GDK, LIB_GDK, "gdk_screen_get_default",
            FunctionDescriptor.of(ADDRESS));

    /** {@code gdouble gdk_screen_get_resolution(GdkScreen *screen)}. */
    private static final MethodHandle GDK_SCREEN_GET_RESOLUTION = bind(GDK, LIB_GDK, "gdk_screen_get_resolution",
            FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS));

    /** {@code GdkDeviceManager *gdk_display_get_device_manager(GdkDisplay *display)}. */
    private static final MethodHandle GDK_DISPLAY_GET_DEVICE_MANAGER = bind(GDK, LIB_GDK,
            "gdk_display_get_device_manager", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code GdkDevice *gdk_device_manager_get_client_pointer(GdkDeviceManager *device_manager)}. */
    private static final MethodHandle GDK_DEVICE_MANAGER_GET_CLIENT_POINTER = bind(GDK, LIB_GDK,
            "gdk_device_manager_get_client_pointer", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code void gdk_device_get_position(GdkDevice *device, GdkScreen **screen, gint *x, gint *y)}. */
    private static final MethodHandle GDK_DEVICE_GET_POSITION = bind(GDK, LIB_GDK, "gdk_device_get_position",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code GdkWindow *gdk_get_default_root_window(void)}. */
    private static final MethodHandle GDK_GET_DEFAULT_ROOT_WINDOW = bind(GDK, LIB_GDK, "gdk_get_default_root_window",
            FunctionDescriptor.of(ADDRESS));

    /** {@code GdkPixbuf *gdk_pixbuf_get_from_window(GdkWindow *window, gint src_x, gint src_y, gint w, gint h)}. */
    private static final MethodHandle GDK_PIXBUF_GET_FROM_WINDOW = bind(GDK, LIB_GDK, "gdk_pixbuf_get_from_window",
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));

    /** {@code GdkKeymap *gdk_keymap_get_for_display(GdkDisplay *display)}. */
    private static final MethodHandle GDK_KEYMAP_GET_FOR_DISPLAY = bind(GDK, LIB_GDK, "gdk_keymap_get_for_display",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code guint gdk_keyval_to_lower(guint keyval)}. */
    private static final MethodHandle GDK_KEYVAL_TO_LOWER = bind(GDK, LIB_GDK, "gdk_keyval_to_lower",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    /**
     * {@code gboolean gdk_keymap_get_entries_for_keyval(GdkKeymap *keymap, guint keyval, GdkKeymapKey **keys,
     * gint *n_keys)}.
     */
    private static final MethodHandle GDK_KEYMAP_GET_ENTRIES_FOR_KEYVAL = bind(GDK, LIB_GDK,
            "gdk_keymap_get_entries_for_keyval", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));

    /**
     * {@code gboolean gdk_keymap_translate_keyboard_state(GdkKeymap *keymap, guint hardware_keycode,
     * GdkModifierType state, gint group, guint *keyval, gint *effective_group, gint *level,
     * GdkModifierType *consumed_modifiers)}.
     */
    private static final MethodHandle GDK_KEYMAP_TRANSLATE_KEYBOARD_STATE = bind(GDK, LIB_GDK,
            "gdk_keymap_translate_keyboard_state", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT,
                    JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code GdkPixbuf *gdk_pixbuf_add_alpha(const GdkPixbuf *pixbuf, gboolean substitute_color, guchar r, g, b)}. */
    private static final MethodHandle GDK_PIXBUF_ADD_ALPHA = bind(GDK_PIXBUF, LIB_GDK_PIXBUF, "gdk_pixbuf_add_alpha",
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_BYTE, JAVA_BYTE, JAVA_BYTE));

    /** {@code guchar *gdk_pixbuf_get_pixels(const GdkPixbuf *pixbuf)}. */
    private static final MethodHandle GDK_PIXBUF_GET_PIXELS = bind(GDK_PIXBUF, LIB_GDK_PIXBUF, "gdk_pixbuf_get_pixels",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code void g_free(gpointer mem)}, called. */
    private static final MethodHandle G_FREE_CALL = bind(GLIB, LIB_GLIB, "g_free", FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code GHashTable *g_hash_table_new(GHashFunc hash_func, GEqualFunc key_equal_func)}. */
    private static final MethodHandle G_HASH_TABLE_NEW = bind(GLIB, LIB_GLIB, "g_hash_table_new",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    /** {@code guint g_direct_hash(gconstpointer v)}, passed as a function pointer. */
    private static final MemorySegment G_DIRECT_HASH = address(GLIB, LIB_GLIB, "g_direct_hash");

    /** {@code gboolean g_direct_equal(gconstpointer v1, gconstpointer v2)}, passed as a function pointer. */
    private static final MemorySegment G_DIRECT_EQUAL = address(GLIB, LIB_GLIB, "g_direct_equal");

    /** {@code gboolean g_hash_table_insert(GHashTable *hash_table, gpointer key, gpointer value)}. */
    private static final MethodHandle G_HASH_TABLE_INSERT = bind(GLIB, LIB_GLIB, "g_hash_table_insert",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /** {@code void g_hash_table_iter_init(GHashTableIter *iter, GHashTable *hash_table)}. */
    private static final MethodHandle G_HASH_TABLE_ITER_INIT = bind(GLIB, LIB_GLIB, "g_hash_table_iter_init",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /** {@code gboolean g_hash_table_iter_next(GHashTableIter *iter, gpointer *key, gpointer *value)}. */
    private static final MethodHandle G_HASH_TABLE_ITER_NEXT = bind(GLIB, LIB_GLIB, "g_hash_table_iter_next",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /** {@code GSettings *g_settings_new(const gchar *schema_id)}. */
    private static final MethodHandle G_SETTINGS_NEW = bind(GIO, LIB_GIO, "g_settings_new",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code guint g_settings_get_uint(GSettings *settings, const gchar *key)}. */
    private static final MethodHandle G_SETTINGS_GET_UINT = bind(GIO, LIB_GIO, "g_settings_get_uint",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /**
     * The four {@code dlsym(RTLD_DEFAULT, ...)} lookups of {@code wrapped.c}, resolved in {@code libgio-2.0.so.0},
     * the library that defines them; {@code NULL} when it does not, which the callers below treat as the C treated a
     * failed {@code dlsym}. Invoked through address-less handles.
     */
    private static final MemorySegment G_SETTINGS_SCHEMA_SOURCE_GET_DEFAULT = optional(GIO, LIB_GIO,
            "g_settings_schema_source_get_default");
    private static final MemorySegment G_SETTINGS_SCHEMA_SOURCE_LOOKUP = optional(GIO, LIB_GIO,
            "g_settings_schema_source_lookup");
    private static final MemorySegment G_SETTINGS_SCHEMA_HAS_KEY = optional(GIO, LIB_GIO,
            "g_settings_schema_has_key");
    private static final MemorySegment G_SETTINGS_SCHEMA_UNREF = optional(GIO, LIB_GIO, "g_settings_schema_unref");

    /** {@code GSettingsSchemaSource *(*)(void)}. */
    private static final MethodHandle SCHEMA_SOURCE_GET_DEFAULT = addresslessDowncall(
            FunctionDescriptor.of(ADDRESS));

    /** {@code GSettingsSchema *(*)(GSettingsSchemaSource *source, const gchar *schema_id, gboolean recursive)}. */
    private static final MethodHandle SCHEMA_SOURCE_LOOKUP = addresslessDowncall(
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT));

    /** {@code gboolean (*)(GSettingsSchema *schema, const gchar *name)}. */
    private static final MethodHandle SCHEMA_HAS_KEY = addresslessDowncall(
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code void (*)(GSettingsSchema *schema)}. */
    private static final MethodHandle SCHEMA_UNREF = addresslessDowncall(FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code Bool XQueryExtension(Display *, const char *name, int *major, int *first_event, int *first_error)}. */
    private static final MethodHandle X_QUERY_EXTENSION = bind(X11, LIB_X11, "XQueryExtension",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /**
     * {@code int XWarpPointer(Display *, Window src_w, Window dest_w, int src_x, int src_y, unsigned int src_width,
     * unsigned int src_height, int dest_x, int dest_y)}.
     */
    private static final MethodHandle X_WARP_POINTER = bind(X11, LIB_X11, "XWarpPointer",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT));

    /** {@code Window XRootWindow(Display *, int screen_number)}. */
    private static final MethodHandle X_ROOT_WINDOW = bind(X11, LIB_X11, "XRootWindow",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));

    /** {@code int XSync(Display *, Bool discard)}. */
    private static final MethodHandle X_SYNC = bind(X11, LIB_X11, "XSync", FunctionDescriptor.of(JAVA_INT, ADDRESS,
            JAVA_INT));

    /** {@code Atom XInternAtom(Display *, const char *atom_name, Bool only_if_exists)}. */
    private static final MethodHandle X_INTERN_ATOM = bind(X11, LIB_X11, "XInternAtom",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_INT));

    /**
     * {@code Bool XkbQueryExtension(Display *, int *opcode, int *event_base, int *error_base, int *major,
     * int *minor)}.
     */
    private static final MethodHandle XKB_QUERY_EXTENSION = bind(X11, LIB_X11, "XkbQueryExtension",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code Status XkbGetState(Display *, unsigned int device_spec, XkbStatePtr state)}. */
    private static final MethodHandle XKB_GET_STATE = bind(X11, LIB_X11, "XkbGetState",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));

    /**
     * {@code Bool XkbGetNamedIndicator(Display *, Atom name, int *ndx, Bool *state, XkbIndicatorMapPtr map,
     * Bool *real)}.
     */
    private static final MethodHandle XKB_GET_NAMED_INDICATOR = bind(X11, LIB_X11, "XkbGetNamedIndicator",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code Bool XTestQueryExtension(Display *, int *event_base, int *error_base, int *major, int *minor)}. */
    private static final MethodHandle X_TEST_QUERY_EXTENSION = bind(XTST, LIB_XTST, "XTestQueryExtension",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code int XTestGrabControl(Display *, Bool impervious)}. */
    private static final MethodHandle X_TEST_GRAB_CONTROL = bind(XTST, LIB_XTST, "XTestGrabControl",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /** {@code int XTestFakeKeyEvent(Display *, unsigned int keycode, Bool is_press, unsigned long delay)}. */
    private static final MethodHandle X_TEST_FAKE_KEY_EVENT = bind(XTST, LIB_XTST, "XTestFakeKeyEvent",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_LONG));

    /** {@code int XTestFakeButtonEvent(Display *, unsigned int button, Bool is_press, unsigned long delay)}. */
    private static final MethodHandle X_TEST_FAKE_BUTTON_EVENT = bind(XTST, LIB_XTST, "XTestFakeButtonEvent",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_LONG));

    /** {@code char *getenv(const char *name)} of libc: the C environment, not {@link System#getenv}'s snapshot. */
    private static final MethodHandle GETENV = bind(LIBC, LIB_C, "getenv", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code int atoi(const char *nptr)} of libc. */
    private static final MethodHandle ATOI = bind(LIBC, LIB_C, "atoi", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int fputs(const char *s, FILE *stream)} of libc. */
    private static final MethodHandle FPUTS = bind(LIBC, LIB_C, "fputs",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code int fflush(FILE *stream)} of libc. */
    private static final MethodHandle FFLUSH = bind(LIBC, LIB_C, "fflush", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** The {@code FILE *stderr} variable of libc; read at each use, as {@code fprintf(stderr, ...)} reads it. */
    private static final MemorySegment STDERR = stderrVariable();

    /**
     * The {@code keymap} inserts of {@code initialize_key} ({@code glass_key.cpp}), in the C's order, as pairs of GDK
     * keyval ({@code gdkkeysyms.h}, named in the comment) and glass key code. The order matters: the robot answers
     * the first keyval that {@code g_hash_table_iter_next} reaches for a key code, which is why the table is also
     * built as a {@code GHashTable} (see {@link #findGdkKeyvalForGlassKeycode}).
     */
    static final int[] KEYMAP = {
            0xff0d, KeyEvent.VK_ENTER, // Return
            0xff08, KeyEvent.VK_BACKSPACE, // BackSpace
            0xff09, KeyEvent.VK_TAB, // Tab
            0xff0b, KeyEvent.VK_CLEAR, // Clear
            0xff13, KeyEvent.VK_PAUSE, // Pause
            0xff1b, KeyEvent.VK_ESCAPE, // Escape
            0x0020, KeyEvent.VK_SPACE, // space
            0xffff, KeyEvent.VK_DELETE, // Delete
            0xff61, KeyEvent.VK_PRINTSCREEN, // Print
            0xff63, KeyEvent.VK_INSERT, // Insert
            0xff6a, KeyEvent.VK_HELP, // Help
            0xffe1, KeyEvent.VK_SHIFT, // Shift_L
            0xffe2, KeyEvent.VK_SHIFT, // Shift_R
            0xffe3, KeyEvent.VK_CONTROL, // Control_L
            0xffe4, KeyEvent.VK_CONTROL, // Control_R
            0xffe9, KeyEvent.VK_ALT, // Alt_L
            0xffea, KeyEvent.VK_ALT_GRAPH, // Alt_R
            0xffeb, KeyEvent.VK_WINDOWS, // Super_L
            0xffec, KeyEvent.VK_WINDOWS, // Super_R
            0xff67, KeyEvent.VK_CONTEXT_MENU, // Menu
            0xffe7, KeyEvent.VK_WINDOWS, // Meta_L
            0xffe8, KeyEvent.VK_CONTEXT_MENU, // Meta_R
            0xffe5, KeyEvent.VK_CAPS_LOCK, // Caps_Lock
            0xff7f, KeyEvent.VK_NUM_LOCK, // Num_Lock
            0xff14, KeyEvent.VK_SCROLL_LOCK, // Scroll_Lock
            0xff55, KeyEvent.VK_PAGE_UP, // Page_Up
            0xff55, KeyEvent.VK_PAGE_UP, // Prior
            0xff56, KeyEvent.VK_PAGE_DOWN, // Page_Down
            0xff56, KeyEvent.VK_PAGE_DOWN, // Next
            0xff57, KeyEvent.VK_END, // End
            0xff50, KeyEvent.VK_HOME, // Home
            0xff51, KeyEvent.VK_LEFT, // Left
            0xff53, KeyEvent.VK_RIGHT, // Right
            0xff52, KeyEvent.VK_UP, // Up
            0xff54, KeyEvent.VK_DOWN, // Down
            0x002c, KeyEvent.VK_COMMA, // comma
            0x002d, KeyEvent.VK_MINUS, // minus
            0x002e, KeyEvent.VK_PERIOD, // period
            0x002f, KeyEvent.VK_SLASH, // slash
            0x003b, KeyEvent.VK_SEMICOLON, // semicolon
            0x003d, KeyEvent.VK_EQUALS, // equal
            0x005b, KeyEvent.VK_OPEN_BRACKET, // bracketleft
            0x005d, KeyEvent.VK_CLOSE_BRACKET, // bracketright
            0x005c, KeyEvent.VK_BACK_SLASH, // backslash
            0x007c, KeyEvent.VK_BACK_SLASH, // bar
            0xffaa, KeyEvent.VK_MULTIPLY, // KP_Multiply
            0xffab, KeyEvent.VK_ADD, // KP_Add
            0xffac, KeyEvent.VK_SEPARATOR, // KP_Separator
            0xffad, KeyEvent.VK_SUBTRACT, // KP_Subtract
            0xffae, KeyEvent.VK_DECIMAL, // KP_Decimal
            0x0027, KeyEvent.VK_QUOTE, // apostrophe
            0x0060, KeyEvent.VK_BACK_QUOTE, // grave
            0x0026, KeyEvent.VK_AMPERSAND, // ampersand
            0x002a, KeyEvent.VK_ASTERISK, // asterisk
            0x0022, KeyEvent.VK_DOUBLE_QUOTE, // quotedbl
            0x003c, KeyEvent.VK_LESS, // less
            0x003e, KeyEvent.VK_GREATER, // greater
            0x007b, KeyEvent.VK_BRACELEFT, // braceleft
            0x007d, KeyEvent.VK_BRACERIGHT, // braceright
            0x0040, KeyEvent.VK_AT, // at
            0x003a, KeyEvent.VK_COLON, // colon
            0x005e, KeyEvent.VK_CIRCUMFLEX, // asciicircum
            0x0024, KeyEvent.VK_DOLLAR, // dollar
            0x20ac, KeyEvent.VK_EURO_SIGN, // EuroSign
            0x0021, KeyEvent.VK_EXCLAMATION, // exclam
            0x00a1, KeyEvent.VK_INV_EXCLAMATION, // exclamdown
            0x0028, KeyEvent.VK_LEFT_PARENTHESIS, // parenleft
            0x0023, KeyEvent.VK_NUMBER_SIGN, // numbersign
            0x002b, KeyEvent.VK_PLUS, // plus
            0x0029, KeyEvent.VK_RIGHT_PARENTHESIS, // parenright
            0x005f, KeyEvent.VK_UNDERSCORE, // underscore
            0x0030, KeyEvent.VK_0, // 0
            0x0031, KeyEvent.VK_1, // 1
            0x0032, KeyEvent.VK_2, // 2
            0x0033, KeyEvent.VK_3, // 3
            0x0034, KeyEvent.VK_4, // 4
            0x0035, KeyEvent.VK_5, // 5
            0x0036, KeyEvent.VK_6, // 6
            0x0037, KeyEvent.VK_7, // 7
            0x0038, KeyEvent.VK_8, // 8
            0x0039, KeyEvent.VK_9, // 9
            0x0061, KeyEvent.VK_A, // a
            0x0062, KeyEvent.VK_B, // b
            0x0063, KeyEvent.VK_C, // c
            0x0064, KeyEvent.VK_D, // d
            0x0065, KeyEvent.VK_E, // e
            0x0066, KeyEvent.VK_F, // f
            0x0067, KeyEvent.VK_G, // g
            0x0068, KeyEvent.VK_H, // h
            0x0069, KeyEvent.VK_I, // i
            0x006a, KeyEvent.VK_J, // j
            0x006b, KeyEvent.VK_K, // k
            0x006c, KeyEvent.VK_L, // l
            0x006d, KeyEvent.VK_M, // m
            0x006e, KeyEvent.VK_N, // n
            0x006f, KeyEvent.VK_O, // o
            0x0070, KeyEvent.VK_P, // p
            0x0071, KeyEvent.VK_Q, // q
            0x0072, KeyEvent.VK_R, // r
            0x0073, KeyEvent.VK_S, // s
            0x0074, KeyEvent.VK_T, // t
            0x0075, KeyEvent.VK_U, // u
            0x0076, KeyEvent.VK_V, // v
            0x0077, KeyEvent.VK_W, // w
            0x0078, KeyEvent.VK_X, // x
            0x0079, KeyEvent.VK_Y, // y
            0x007a, KeyEvent.VK_Z, // z
            0x0041, KeyEvent.VK_A, // A
            0x0042, KeyEvent.VK_B, // B
            0x0043, KeyEvent.VK_C, // C
            0x0044, KeyEvent.VK_D, // D
            0x0045, KeyEvent.VK_E, // E
            0x0046, KeyEvent.VK_F, // F
            0x0047, KeyEvent.VK_G, // G
            0x0048, KeyEvent.VK_H, // H
            0x0049, KeyEvent.VK_I, // I
            0x004a, KeyEvent.VK_J, // J
            0x004b, KeyEvent.VK_K, // K
            0x004c, KeyEvent.VK_L, // L
            0x004d, KeyEvent.VK_M, // M
            0x004e, KeyEvent.VK_N, // N
            0x004f, KeyEvent.VK_O, // O
            0x0050, KeyEvent.VK_P, // P
            0x0051, KeyEvent.VK_Q, // Q
            0x0052, KeyEvent.VK_R, // R
            0x0053, KeyEvent.VK_S, // S
            0x0054, KeyEvent.VK_T, // T
            0x0055, KeyEvent.VK_U, // U
            0x0056, KeyEvent.VK_V, // V
            0x0057, KeyEvent.VK_W, // W
            0x0058, KeyEvent.VK_X, // X
            0x0059, KeyEvent.VK_Y, // Y
            0x005a, KeyEvent.VK_Z, // Z
            0xffb0, KeyEvent.VK_NUMPAD0, // KP_0
            0xffb1, KeyEvent.VK_NUMPAD1, // KP_1
            0xffb2, KeyEvent.VK_NUMPAD2, // KP_2
            0xffb3, KeyEvent.VK_NUMPAD3, // KP_3
            0xffb4, KeyEvent.VK_NUMPAD4, // KP_4
            0xffb5, KeyEvent.VK_NUMPAD5, // KP_5
            0xffb6, KeyEvent.VK_NUMPAD6, // KP_6
            0xffb7, KeyEvent.VK_NUMPAD7, // KP_7
            0xffb8, KeyEvent.VK_NUMPAD8, // KP_8
            0xffb9, KeyEvent.VK_NUMPAD9, // KP_9
            0xff8d, KeyEvent.VK_ENTER, // KP_Enter
            0xff95, KeyEvent.VK_HOME, // KP_Home
            0xff96, KeyEvent.VK_LEFT, // KP_Left
            0xff97, KeyEvent.VK_UP, // KP_Up
            0xff98, KeyEvent.VK_RIGHT, // KP_Right
            0xff99, KeyEvent.VK_DOWN, // KP_Down
            0xff9a, KeyEvent.VK_PAGE_UP, // KP_Prior
            0xff9a, KeyEvent.VK_PAGE_UP, // KP_Page_Up
            0xff9b, KeyEvent.VK_PAGE_DOWN, // KP_Next
            0xff9b, KeyEvent.VK_PAGE_DOWN, // KP_Page_Down
            0xff9c, KeyEvent.VK_END, // KP_End
            0xff9e, KeyEvent.VK_INSERT, // KP_Insert
            0xff9f, KeyEvent.VK_DELETE, // KP_Delete
            0xffaf, KeyEvent.VK_DIVIDE, // KP_Divide
            0xff9d, KeyEvent.VK_CLEAR, // KP_Begin
            0xffbe, KeyEvent.VK_F1, // F1
            0xffbf, KeyEvent.VK_F2, // F2
            0xffc0, KeyEvent.VK_F3, // F3
            0xffc1, KeyEvent.VK_F4, // F4
            0xffc2, KeyEvent.VK_F5, // F5
            0xffc3, KeyEvent.VK_F6, // F6
            0xffc4, KeyEvent.VK_F7, // F7
            0xffc5, KeyEvent.VK_F8, // F8
            0xffc6, KeyEvent.VK_F9, // F9
            0xffc7, KeyEvent.VK_F10, // F10
            0xffc8, KeyEvent.VK_F11, // F11
            0xffc9, KeyEvent.VK_F12, // F12
            0xfe03, KeyEvent.VK_ALT_GRAPH  // ISO_Level3_Shift
    };

    /** The {@code robot_java_to_keyval} assignments of {@code initialize_key}, as (glass key code, keyval) pairs. */
    static final int[] ROBOT_JAVA_TO_KEYVAL = {
            KeyEvent.VK_ENTER, 0xff0d, // Return
            KeyEvent.VK_CLEAR, 0xff0b, // Clear
            KeyEvent.VK_PAGE_UP, 0xff55, // Page_Up
            KeyEvent.VK_END, 0xff57, // End
            KeyEvent.VK_HOME, 0xff50, // Home
            KeyEvent.VK_LEFT, 0xff51, // Left
            KeyEvent.VK_UP, 0xff52, // Up
            KeyEvent.VK_RIGHT, 0xff53, // Right
            KeyEvent.VK_DOWN, 0xff54, // Down
            KeyEvent.VK_DELETE, 0xffff, // Delete
            KeyEvent.VK_BACK_SLASH, 0x005c, // backslash
            KeyEvent.VK_ALT_GRAPH, 0xfe03  // ISO_Level3_Shift
    };

    /**
     * The {@code robot_java_to_keyval} assignments of {@code initialize_key_remote_desktop}, made once the
     * remote-desktop screenshot method is selected; the {@code keyval_to_scancode} half of that function serves only
     * the screencast C and has no counterpart here.
     */
    static final int[] REMOTE_ROBOT_JAVA_TO_KEYVAL = {
            KeyEvent.VK_NUMPAD0, 0xff9e, // KP_Insert
            KeyEvent.VK_NUMPAD1, 0xff9c, // KP_End
            KeyEvent.VK_NUMPAD2, 0xff99, // KP_Down
            KeyEvent.VK_NUMPAD3, 0xff9b, // KP_Page_Down
            KeyEvent.VK_NUMPAD4, 0xff96, // KP_Left
            KeyEvent.VK_NUMPAD5, 0xff9d, // KP_Begin
            KeyEvent.VK_NUMPAD6, 0xff98, // KP_Right
            KeyEvent.VK_NUMPAD7, 0xff95, // KP_Home
            KeyEvent.VK_NUMPAD8, 0xff97, // KP_Up
            KeyEvent.VK_NUMPAD9, 0xff9a, // KP_Page_Up
            KeyEvent.VK_DECIMAL, 0xff9f, // KP_Delete
            KeyEvent.VK_WINDOWS, 0xffeb, // Super_L
            KeyEvent.VK_CONTEXT_MENU, 0xff67, // Menu
            KeyEvent.VK_CLEAR, 0xff9d, // KP_Begin
            0xE0, 0xff97, // KP_Up
            0xE1, 0xff99, // KP_Down
            0xE2, 0xff96, // KP_Left
            0xE3, 0xff98  // KP_Right
    };

    /** {@code keymap}: the {@code GHashTable} of {@link #KEYMAP}, built by {@link #initKeymap}; never freed. */
    private static MemorySegment keymap = MemorySegment.NULL;

    /** {@code robot_java_to_keyval}, a {@code std::map} in the C. */
    private static final Map<Integer, Integer> ROBOT_KEYVALS = new HashMap<>();

    private static boolean keyInitialized;
    private static boolean keyInitializedRemoteDesktop;

    /** {@code checkXTest}'s statics. */
    private static boolean xtestCheckDone;
    private static int xtestAvailable;

    /** {@code isXkbAvailable}'s statics. */
    private static boolean xkbInitialized;
    private static int xkbAvailable;

    /** The first-use {@code loaded <symbol>} messages of the four {@code wrapped_g_settings_schema_*} functions. */
    private static final boolean[] WRAPPED_LOADED = new boolean[4];

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkRobot__1keyPress} ({@code GlassRobot.cpp}): {@code checkXTest} then
     * {@code keyButton(code, TRUE)}. When XTest is missing the C left an {@code UnsupportedOperationException}
     * pending and carried on into the XTest calls, so the exception reached Java only when the native returned;
     * here it is thrown at the same point, after the calls.
     */
    static void robotKeyPress(int code) {
        boolean missing = checkXTest();
        keyButton(code, true);
        throwIfXTestMissing(missing);
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkRobot__1keyRelease} ({@code GlassRobot.cpp}): see {@link #robotKeyPress}. */
    static void robotKeyRelease(int code) {
        boolean missing = checkXTest();
        keyButton(code, false);
        throwIfXTestMissing(missing);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkRobot__1mouseMove} ({@code GlassRobot.cpp}): the coordinates scaled by
     * {@link #getUIScale} as {@code rint(x * uiScale)} ({@code float} product, {@code double} {@code rint}), then
     * {@code XWarpPointer} to the root window of GDK's default screen and {@code XSync}.
     */
    static void robotMouseMove(int x, int y) {
        try {
            MemorySegment xdisplay = (MemorySegment) GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            boolean missing = checkXTest();
            MemorySegment screen = (MemorySegment) GDK_SCREEN_GET_DEFAULT.invokeExact();
            float uiScale = getUIScale(screen);
            x = (int) Math.rint(x * uiScale);
            y = (int) Math.rint(y * uiScale);
            int screenNumber = (int) GDK_X11_GET_DEFAULT_SCREEN.invokeExact();
            long root = (long) X_ROOT_WINDOW.invokeExact(xdisplay, screenNumber);
            int warped = (int) X_WARP_POINTER.invokeExact(xdisplay, 0L, root, 0, 0, 0, 0, x, y);
            int synced = (int) X_SYNC.invokeExact(xdisplay, 0);
            throwIfXTestMissing(missing);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkRobot__1mousePress} ({@code GlassRobot.cpp}). */
    static void robotMousePress(int buttons) {
        boolean missing = checkXTest();
        mouseButtons(buttons, true);
        throwIfXTestMissing(missing);
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkRobot__1mouseRelease} ({@code GlassRobot.cpp}). */
    static void robotMouseRelease(int buttons) {
        boolean missing = checkXTest();
        mouseButtons(buttons, false);
        throwIfXTestMissing(missing);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkRobot__1mouseWheel} ({@code GlassRobot.cpp}): {@code abs(amt)} press and
     * release pairs of button 4 (negative amounts) or 5, then {@code XSync}.
     */
    static void robotMouseWheel(int amt) {
        try {
            MemorySegment xdisplay = (MemorySegment) GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            int repeat = Math.abs(amt);
            int button = amt < 0 ? 4 : 5;
            boolean missing = checkXTest();
            for (int i = 0; i < repeat; i++) {
                int pressed = (int) X_TEST_FAKE_BUTTON_EVENT.invokeExact(xdisplay, button, 1, 0L);
                int released = (int) X_TEST_FAKE_BUTTON_EVENT.invokeExact(xdisplay, button, 0, 0L);
            }
            int synced = (int) X_SYNC.invokeExact(xdisplay, 0);
            throwIfXTestMissing(missing);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkRobot__1getMouseX} ({@code GlassRobot.cpp}): the client pointer's
     * position ({@code glass_gdk_display_get_pointer}) divided by {@link #getUIScale}, as
     * {@code rint(x / uiScale)}.
     */
    static int robotGetMouseX() {
        return pointerCoordinate(true);
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkRobot__1getMouseY} ({@code GlassRobot.cpp}): see {@link #robotGetMouseX}. */
    static int robotGetMouseY() {
        return pointerCoordinate(false);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkRobot__1getScreenCapture} ({@code GlassRobot.cpp}): after the C's guards,
     * {@code gdk_pixbuf_get_from_window} of the default root window, {@code gdk_pixbuf_add_alpha}, and
     * {@code convert_BGRA_to_RGBA} of {@code width * 4 * height} bytes of its pixels copied into {@code data} as
     * native-order ints (what {@code SetIntArrayRegion} did), then {@code g_free} and {@code g_object_unref}. Like
     * the C, it reads the pixels as {@code width * 4} bytes per row.
     */
    @SuppressWarnings("restricted")
    static void robotGetScreenCapture(int x, int y, int width, int height, int[] data) {
        if (data == null) {
            return;
        }
        if (width <= 0 || height <= 0) {
            return;
        }
        int maxPixels = Integer.MAX_VALUE / 4;
        if (width >= maxPixels / height) {
            return;
        }
        int numPixels = width * height;
        if (numPixels > data.length) {
            return;
        }
        try {
            MemorySegment rootWindow = (MemorySegment) GDK_GET_DEFAULT_ROOT_WINDOW.invokeExact();
            MemorySegment tmp = (MemorySegment) GDK_PIXBUF_GET_FROM_WINDOW.invokeExact(rootWindow, x, y, width,
                    height);
            if (tmp.address() == 0) {
                return;
            }
            MemorySegment screenshot = (MemorySegment) GDK_PIXBUF_ADD_ALPHA.invokeExact(tmp, 0, (byte) 0, (byte) 0,
                    (byte) 0);
            G_OBJECT_UNREF.invokeExact(tmp);
            if (screenshot.address() == 0) {
                return;
            }
            MemorySegment pixels = (MemorySegment) GDK_PIXBUF_GET_PIXELS.invokeExact(screenshot);
            MemorySegment rgba = convertBgraToRgba(pixels.reinterpret((long) width * 4 * height), 0, width * 4,
                    height);
            if (rgba.address() != 0) {
                MemorySegment.copy(rgba, JAVA_INT_UNALIGNED, 0, data, 0, numPixels);
                G_FREE_CALL.invokeExact(rgba);
            }
            G_OBJECT_UNREF.invokeExact(screenshot);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1isKeyLocked} ({@code glass_key.cpp}): UNKNOWN without XKB
     * or for a key other than Caps Lock and Num Lock, whose indicator atoms ({@code "Caps Lock"}, {@code "Num Lock"},
     * only if they exist) are looked up with {@code XkbGetNamedIndicator}.
     */
    static int isKeyLocked(int keyCode) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment gdkDisplay = (MemorySegment) GDK_DISPLAY_GET_DEFAULT.invokeExact();
            MemorySegment display = (MemorySegment) GDK_X11_DISPLAY_GET_XDISPLAY.invokeExact(gdkDisplay);
            if (!isXkbAvailable(display)) {
                return KeyEvent.KEY_LOCK_UNKNOWN;
            }
            long keyCodeAtom = 0;
            switch (keyCode) {
                case KeyEvent.VK_CAPS_LOCK:
                    keyCodeAtom = (long) X_INTERN_ATOM.invokeExact(display, call.allocateFrom("Caps Lock"), 1);
                    break;
                case KeyEvent.VK_NUM_LOCK:
                    keyCodeAtom = (long) X_INTERN_ATOM.invokeExact(display, call.allocateFrom("Num Lock"), 1);
                    break;
                default:
                    break;
            }
            if (keyCodeAtom == 0) {
                return KeyEvent.KEY_LOCK_UNKNOWN;
            }
            MemorySegment isLocked = call.allocate(JAVA_INT);
            int found = (int) XKB_GET_NAMED_INDICATOR.invokeExact(display, keyCodeAtom, MemorySegment.NULL, isLocked,
                    MemorySegment.NULL, MemorySegment.NULL);
            if (found != 0) {
                return isLocked.get(JAVA_INT, 0) != 0 ? KeyEvent.KEY_LOCK_ON : KeyEvent.KEY_LOCK_OFF;
            }
            return KeyEvent.KEY_LOCK_UNKNOWN;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code getUIScale} ({@code glass_screen.cpp}), which the C window and screen code keeps using: the
     * {@code glass.gtk.uiScale} override when positive; otherwise the screen resolution over 96 dpi, and at exactly
     * 96 dpi the integer {@code GDK_SCALE} of the C environment ({@code atoi}) when positive, else GNOME's
     * {@code scaling-factor} when non-zero.
     */
    static float getUIScale(MemorySegment screen) {
        float uiScale;
        if (GtkApplication.overrideUIScale > 0.0f) {
            uiScale = GtkApplication.overrideUIScale;
        } else {
            try (Arena call = Arena.ofConfined()) {
                double resolution = (double) GDK_SCREEN_GET_RESOLUTION.invokeExact(screen);
                uiScale = (float) (resolution / DEFAULT_DPI);
                if ((int) resolution == DEFAULT_DPI) {
                    MemorySegment scaleStr = (MemorySegment) GETENV.invokeExact(call.allocateFrom("GDK_SCALE"));
                    int gdkScale = scaleStr.address() == 0 ? -1 : (int) ATOI.invokeExact(scaleStr);
                    if (gdkScale > 0) {
                        uiScale = (float) gdkScale;
                    } else {
                        int gnomeScale = settingsGetGuintOpt("org.gnome.desktop.interface", "scaling-factor", 0);
                        if (gnomeScale != 0) {
                            uiScale = (float) Integer.toUnsignedLong(gnomeScale);
                        }
                    }
                }
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
        return uiScale;
    }

    /**
     * {@code glass_settings_get_guint_opt} ({@code glass_general.cpp}) over the {@code wrapped_g_settings_schema_*}
     * lookups of {@code wrapped.c}, with the {@code jdk.gtk.verbose} messages of both on the C {@code stderr}. Like
     * the C it leaks the schema when the key is missing and the {@code GSettings} it reads.
     *
     * @return the unsigned setting as an {@code int}, or {@code defval}
     */
    static int settingsGetGuintOpt(String schemaName, String keyName, int defval) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment schemaId = call.allocateFrom(schemaName);
            MemorySegment key = call.allocateFrom(keyName);
            MemorySegment source = MemorySegment.NULL;
            if (wrapped(0, G_SETTINGS_SCHEMA_SOURCE_GET_DEFAULT, "g_settings_schema_source_get_default")) {
                source = (MemorySegment) SCHEMA_SOURCE_GET_DEFAULT.invokeExact(G_SETTINGS_SCHEMA_SOURCE_GET_DEFAULT);
            }
            if (source.address() == 0) {
                if (GtkApplication.verbose) {
                    printStderr("No schema source dir found!\n");
                }
                return defval;
            }
            MemorySegment schema = MemorySegment.NULL;
            if (wrapped(1, G_SETTINGS_SCHEMA_SOURCE_LOOKUP, "g_settings_schema_source_lookup")) {
                schema = (MemorySegment) SCHEMA_SOURCE_LOOKUP.invokeExact(G_SETTINGS_SCHEMA_SOURCE_LOOKUP, source,
                        schemaId, 1);
            }
            if (schema.address() == 0) {
                if (GtkApplication.verbose) {
                    printStderr("schema '" + schemaName + "' not found!\n");
                }
                return defval;
            }
            int hasKey = 0;
            if (wrapped(2, G_SETTINGS_SCHEMA_HAS_KEY, "g_settings_schema_has_key")) {
                hasKey = (int) SCHEMA_HAS_KEY.invokeExact(G_SETTINGS_SCHEMA_HAS_KEY, schema, key);
            }
            if (hasKey == 0) {
                if (GtkApplication.verbose) {
                    printStderr("key '" + keyName + "' not found in schema '" + schemaName + "'!\n");
                }
                return defval;
            }
            if (GtkApplication.verbose) {
                printStderr("found schema '" + schemaName + "' and key '" + keyName + "'\n");
            }
            MemorySegment settings = (MemorySegment) G_SETTINGS_NEW.invokeExact(schemaId);
            if (wrapped(3, G_SETTINGS_SCHEMA_UNREF, "g_settings_schema_unref")) {
                SCHEMA_UNREF.invokeExact(G_SETTINGS_SCHEMA_UNREF, schema);
            }
            return (int) G_SETTINGS_GET_UINT.invokeExact(settings, key);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code find_gdk_keyval_for_glass_keycode} ({@code glass_key.cpp}): the robot override when there is one,
     * otherwise the key of the first {@code keymap} entry {@code g_hash_table_iter_next} reaches whose value is
     * {@code code}, or -1.
     */
    static int findGdkKeyvalForGlassKeycode(int code) {
        initKeymap();
        Integer robot = ROBOT_KEYVALS.get(code);
        if (robot != null) {
            return robot;
        }
        int result = -1;
        try (Arena call = Arena.ofConfined()) {
            MemorySegment iter = call.allocate(G_HASH_TABLE_ITER_SIZE);
            MemorySegment key = call.allocate(ADDRESS);
            MemorySegment value = call.allocate(ADDRESS);
            G_HASH_TABLE_ITER_INIT.invokeExact(iter, keymap);
            while ((int) G_HASH_TABLE_ITER_NEXT.invokeExact(iter, key, value) != 0) {
                if (code == (int) value.get(ADDRESS, 0).address()) {
                    result = (int) key.get(ADDRESS, 0).address();
                    break;
                }
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
        return result;
    }

    /**
     * {@code find_gdk_keycode_for_keyval} ({@code glass_key.cpp}): the hardware keycode that produces the lower-cased
     * {@code keyval} at shift level 0 on the current XKB group (Num Lock applied for keypad digits and operators),
     * falling back to group 0 for a to z; -1 when there is none.
     */
    @SuppressWarnings("restricted")
    static int findGdkKeycodeForKeyval(int keyval) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment keysOut = call.allocate(ADDRESS);
            MemorySegment nKeysOut = call.allocate(JAVA_INT);
            MemorySegment gdkDisplay = (MemorySegment) GDK_DISPLAY_GET_DEFAULT.invokeExact();
            MemorySegment gdkKeymap = (MemorySegment) GDK_KEYMAP_GET_FOR_DISPLAY.invokeExact(gdkDisplay);
            keyval = (int) GDK_KEYVAL_TO_LOWER.invokeExact(keyval);
            boolean requiresNumLock = keyvalRequiresNumlock(keyval);
            if ((int) GDK_KEYMAP_GET_ENTRIES_FOR_KEYVAL.invokeExact(gdkKeymap, keyval, keysOut, nKeysOut) == 0) {
                return -1;
            }
            int nKeys = nKeysOut.get(JAVA_INT, 0);
            MemorySegment keys = keysOut.get(ADDRESS, 0).reinterpret(Math.max(0, nKeys) * GDK_KEYMAP_KEY_SIZE);
            int group = getCurrentKeyboardGroup();
            int result = searchKeys(call, gdkKeymap, keys, nKeys, keyval, group, requiresNumLock);
            if (result < 0 && group != 0) {
                if (keyval >= 'a' && keyval <= 'z') {
                    result = searchKeys(call, gdkKeymap, keys, nKeys, keyval, 0, requiresNumLock);
                }
            }
            G_FREE_CALL.invokeExact(keys);
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code checkXTest} ({@code GlassRobot.cpp}), memoised as the C memoised it; answers whether XTest is missing. */
    private static boolean checkXTest() {
        if (!xtestCheckDone) {
            try (Arena call = Arena.ofConfined()) {
                MemorySegment majorOpcode = call.allocate(JAVA_INT);
                MemorySegment firstEvent = call.allocate(JAVA_INT);
                MemorySegment firstError = call.allocate(JAVA_INT);
                MemorySegment eventBase = call.allocate(JAVA_INT);
                MemorySegment errorBase = call.allocate(JAVA_INT);
                MemorySegment major = call.allocate(JAVA_INT);
                MemorySegment minor = call.allocate(JAVA_INT);
                MemorySegment display = (MemorySegment) GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
                xtestAvailable = (int) X_QUERY_EXTENSION.invokeExact(display, call.allocateFrom("XTEST"), majorOpcode,
                        firstEvent, firstError);
                if (xtestAvailable != 0) {
                    display = (MemorySegment) GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
                    int queried = (int) X_TEST_QUERY_EXTENSION.invokeExact(display, eventBase, errorBase, major,
                            minor);
                    int majorp = major.get(JAVA_INT, 0);
                    int minorp = minor.get(JAVA_INT, 0);
                    if (majorp < 2 || (majorp == 2 && minorp < 2)) {
                        xtestAvailable = 0;
                    } else {
                        display = (MemorySegment) GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
                        int grabbed = (int) X_TEST_GRAB_CONTROL.invokeExact(display, 1);
                    }
                }
            } catch (Throwable t) {
                throw unexpected(t);
            }
            xtestCheckDone = true;
        }
        return xtestAvailable == 0;
    }

    private static void throwIfXTestMissing(boolean missing) {
        if (missing) {
            throw new UnsupportedOperationException(XTEST_MISSING);
        }
    }

    /** {@code keyButton} ({@code GlassRobot.cpp}). */
    private static void keyButton(int code, boolean press) {
        try {
            MemorySegment xdisplay = (MemorySegment) GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            int gdkKeyval = findGdkKeyvalForGlassKeycode(code);
            if (gdkKeyval == -1) {
                return;
            }
            int keycode = findGdkKeycodeForKeyval(gdkKeyval);
            if (keycode == -1) {
                return;
            }
            int faked = (int) X_TEST_FAKE_KEY_EVENT.invokeExact(xdisplay, keycode, press ? 1 : 0, 0L);
            int synced = (int) X_SYNC.invokeExact(xdisplay, 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code mouseButtons} ({@code GlassRobot.cpp}): X buttons 1, 2, 3, 8 and 9, then {@code XSync}. */
    private static void mouseButtons(int buttons, boolean press) {
        try {
            MemorySegment xdisplay = (MemorySegment) GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            int isPress = press ? 1 : 0;
            if ((buttons & GlassRobot.MOUSE_LEFT_BTN) != 0) {
                int faked = (int) X_TEST_FAKE_BUTTON_EVENT.invokeExact(xdisplay, 1, isPress, 0L);
            }
            if ((buttons & GlassRobot.MOUSE_MIDDLE_BTN) != 0) {
                int faked = (int) X_TEST_FAKE_BUTTON_EVENT.invokeExact(xdisplay, 2, isPress, 0L);
            }
            if ((buttons & GlassRobot.MOUSE_RIGHT_BTN) != 0) {
                int faked = (int) X_TEST_FAKE_BUTTON_EVENT.invokeExact(xdisplay, 3, isPress, 0L);
            }
            if ((buttons & GlassRobot.MOUSE_BACK_BTN) != 0) {
                int faked = (int) X_TEST_FAKE_BUTTON_EVENT.invokeExact(xdisplay, 8, isPress, 0L);
            }
            if ((buttons & GlassRobot.MOUSE_FORWARD_BTN) != 0) {
                int faked = (int) X_TEST_FAKE_BUTTON_EVENT.invokeExact(xdisplay, 9, isPress, 0L);
            }
            int synced = (int) X_SYNC.invokeExact(xdisplay, 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The shared body of {@code _getMouseX} and {@code _getMouseY}: {@code gdk_device_get_position} of the client
     * pointer, the other coordinate not requested ({@code NULL}), then {@code rint(coordinate / getUIScale(...))}.
     */
    private static int pointerCoordinate(boolean wantX) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment coordinate = call.allocate(JAVA_INT);
            MemorySegment display = (MemorySegment) GDK_DISPLAY_GET_DEFAULT.invokeExact();
            MemorySegment manager = (MemorySegment) GDK_DISPLAY_GET_DEVICE_MANAGER.invokeExact(display);
            MemorySegment pointer = (MemorySegment) GDK_DEVICE_MANAGER_GET_CLIENT_POINTER.invokeExact(manager);
            MemorySegment x = wantX ? coordinate : MemorySegment.NULL;
            MemorySegment y = wantX ? MemorySegment.NULL : coordinate;
            GDK_DEVICE_GET_POSITION.invokeExact(pointer, MemorySegment.NULL, x, y);
            MemorySegment screen = (MemorySegment) GDK_SCREEN_GET_DEFAULT.invokeExact();
            return (int) Math.rint(coordinate.get(JAVA_INT, 0) / getUIScale(screen));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code init_keymap} ({@code glass_key.cpp}) for the robot: {@code initialize_key} once, and
     * {@code initialize_key_remote_desktop}'s robot overrides once the remote-desktop screenshot method is in use
     * (the C tested {@code isRemoteDesktop}, which the screencast C sets from that same method when
     * {@code ScreencastHelper} loads, and {@code GtkRobot} loads it before a key reaches here). The C also connected
     * {@code keys-changed} handlers that clear the character map only its key-event path reads; that connection
     * stays with the C.
     */
    private static void initKeymap() {
        if (!keyInitialized) {
            try {
                keymap = (MemorySegment) G_HASH_TABLE_NEW.invokeExact(G_DIRECT_HASH, G_DIRECT_EQUAL);
                for (int i = 0; i < KEYMAP.length; i += 2) {
                    int replaced = (int) G_HASH_TABLE_INSERT.invokeExact(keymap, MemorySegment.ofAddress(KEYMAP[i]),
                            MemorySegment.ofAddress(KEYMAP[i + 1]));
                }
            } catch (Throwable t) {
                throw unexpected(t);
            }
            for (int i = 0; i < ROBOT_JAVA_TO_KEYVAL.length; i += 2) {
                ROBOT_KEYVALS.put(ROBOT_JAVA_TO_KEYVAL[i], ROBOT_JAVA_TO_KEYVAL[i + 1]);
            }
            keyInitialized = true;
        }
        if (XdgDesktopPortal.isRemoteDesktop() && !keyInitializedRemoteDesktop) {
            for (int i = 0; i < REMOTE_ROBOT_JAVA_TO_KEYVAL.length; i += 2) {
                ROBOT_KEYVALS.put(REMOTE_ROBOT_JAVA_TO_KEYVAL[i], REMOTE_ROBOT_JAVA_TO_KEYVAL[i + 1]);
            }
            keyInitializedRemoteDesktop = true;
        }
    }

    /** {@code keyval_requires_numlock} ({@code glass_key.cpp}): the keypad digits, operators and {@code KP_Equal}. */
    private static boolean keyvalRequiresNumlock(int keyval) {
        return switch (keyval) {
            case 0xffbd, 0xffaa, 0xffab, 0xffad, 0xffae, 0xffac, 0xffaf,
                 0xffb0, 0xffb1, 0xffb2, 0xffb3, 0xffb4, 0xffb5, 0xffb6, 0xffb7, 0xffb8, 0xffb9 -> true;
            default -> false;
        };
    }

    /** {@code search_keys} ({@code glass_key.cpp}). */
    private static int searchKeys(Arena call, MemorySegment gdkKeymap, MemorySegment keys, int nKeys, int searchKeyval,
                                  int searchGroup, boolean requiresNumLock) throws Throwable {
        int result = -1;
        int state = requiresNumLock ? GDK_MOD2_MASK : 0;
        MemorySegment keyval = call.allocate(JAVA_INT);
        for (int i = 0; i < nKeys; ++i) {
            keyval.set(JAVA_INT, 0, 0);
            int keycode = keys.get(JAVA_INT, i * GDK_KEYMAP_KEY_SIZE);
            if ((int) GDK_KEYMAP_TRANSLATE_KEYBOARD_STATE.invokeExact(gdkKeymap, keycode, state, searchGroup, keyval,
                    MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL) != 0) {
                if (keyval.get(JAVA_INT, 0) == searchKeyval) {
                    result = keycode;
                    break;
                }
            }
        }
        return result;
    }

    /** {@code get_current_keyboard_group} ({@code glass_key.cpp}): the XKB core keyboard's group, or -1 without XKB. */
    private static int getCurrentKeyboardGroup() throws Throwable {
        MemorySegment gdkDisplay = (MemorySegment) GDK_DISPLAY_GET_DEFAULT.invokeExact();
        MemorySegment display = (MemorySegment) GDK_X11_DISPLAY_GET_XDISPLAY.invokeExact(gdkDisplay);
        if (isXkbAvailable(display)) {
            try (Arena call = Arena.ofConfined()) {
                MemorySegment xkbState = call.allocate(XKB_STATE_REC_SIZE);
                int status = (int) XKB_GET_STATE.invokeExact(display, XKB_USE_CORE_KBD, xkbState);
                return xkbState.get(JAVA_BYTE, 0) & 0xFF;
            }
        }
        return -1;
    }

    /** {@code isXkbAvailable} ({@code glass_key.cpp}): {@code XkbQueryExtension} with version 1.0, memoised. */
    private static boolean isXkbAvailable(MemorySegment display) throws Throwable {
        if (!xkbInitialized) {
            try (Arena call = Arena.ofConfined()) {
                MemorySegment major = call.allocate(JAVA_INT);
                MemorySegment minor = call.allocate(JAVA_INT);
                major.set(JAVA_INT, 0, 1);
                minor.set(JAVA_INT, 0, 0);
                xkbAvailable = (int) XKB_QUERY_EXTENSION.invokeExact(display, MemorySegment.NULL, MemorySegment.NULL,
                        MemorySegment.NULL, major, minor);
            }
            xkbInitialized = true;
        }
        return xkbAvailable != 0;
    }

    /**
     * The first-use half of a {@code wrapped_*} function of {@code wrapped.c}: whether its symbol exists, and the
     * {@code loaded <name>} line (flushed) the C printed under {@code jdk.gtk.verbose} the first time it found it.
     */
    private static boolean wrapped(int index, MemorySegment symbol, String name) throws Throwable {
        boolean present = symbol.address() != 0;
        if (!WRAPPED_LOADED[index]) {
            WRAPPED_LOADED[index] = true;
            if (GtkApplication.verbose && present) {
                printStderr("loaded " + name + "\n");
                int flushed = (int) FFLUSH.invokeExact(stderr());
            }
        }
        return present;
    }

    /** {@code fprintf(stderr, ...)} of already-formatted text: one {@code fputs} on libc's {@code stderr}. */
    private static void printStderr(String text) throws Throwable {
        try (Arena call = Arena.ofConfined()) {
            int written = (int) FPUTS.invokeExact(call.allocateFrom(text), stderr());
        }
    }

    private static MemorySegment stderr() {
        return STDERR.get(ADDRESS, 0);
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Application queries: GtkApplication._openURI, staticView_getMultiClickTime, staticView_getMultiClickMaxX,
     * staticView_getMultiClickMaxY, _supportsTransparentWindows (GlassApplication.cpp)
     * ------------------------------------------------------------------------------------------------------- */

    /** {@code gboolean gtk_show_uri(GdkScreen *screen, const gchar *uri, guint32 timestamp, GError **error)}. */
    private static final MethodHandle GTK_SHOW_URI = bind(GTK, LIB_GTK, "gtk_show_uri",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));

    /** {@code void g_error_free(GError *error)}. */
    private static final MethodHandle G_ERROR_FREE = bind(GLIB, LIB_GLIB, "g_error_free",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code GtkSettings *gtk_settings_get_default(void)}. */
    private static final MethodHandle GTK_SETTINGS_GET_DEFAULT = bind(GTK, LIB_GTK, "gtk_settings_get_default",
            FunctionDescriptor.of(ADDRESS));

    /**
     * {@code void g_object_get(gpointer object, const gchar *first_property_name, ...)}, linked for the one shape
     * the C used: one property, the pointer its value is written through, and the {@code NULL} terminator. The
     * same linkage serves a {@code gint *}, a {@code gboolean *} and a {@code gchar **}.
     */
    private static final MethodHandle G_OBJECT_GET_ONE = bind(GOBJECT, LIB_GOBJECT, "g_object_get",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS, ADDRESS), Linker.Option.firstVariadicArg(2));

    /** {@code gboolean gdk_display_supports_composite(GdkDisplay *display)}. */
    private static final MethodHandle GDK_DISPLAY_SUPPORTS_COMPOSITE = bind(GDK, LIB_GDK,
            "gdk_display_supports_composite", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code gboolean gdk_screen_is_composited(GdkScreen *screen)}. */
    private static final MethodHandle GDK_SCREEN_IS_COMPOSITED = bind(GDK, LIB_GDK, "gdk_screen_is_composited",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code size_t strlen(const char *s)} of libc. */
    private static final MethodHandle STRLEN = bind(LIBC, LIB_C, "strlen", FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    /** {@code GError.message}: after {@code GQuark domain} and {@code gint code}. */
    static final long G_ERROR_MESSAGE_OFFSET = 8;

    /** The {@code static gint multi_click_time = -1} of {@code staticView_getMultiClickTime}. */
    private static int multiClickTime = -1;

    /** The {@code static gint multi_click_dist = -1} of {@code staticView_getMultiClickMaxX}. */
    private static int multiClickDist = -1;

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1openURI} ({@code GlassApplication.cpp}):
     * {@code gtk_show_uri(NULL, uri, 0, &error)} with the URI as the modified UTF-8 {@code GetStringUTFChars} gave
     * ({@link JniStringCodec}); on failure {@code Error opening URI <uri> : <message>} (or
     * {@code Unspecified error.} without a {@code GError}) on libc's {@code stderr} and {@code g_error_free}.
     * Called from whatever thread asks, as the C was. Where the C would have crashed on a {@code null} URI this
     * throws {@link NullPointerException}; the C's branch for a failed {@code GetStringUTFChars} (an
     * {@code OutOfMemoryError} left pending and {@code Error: Converted URI string is null} printed) is an
     * {@code OutOfMemoryError} thrown by the conversion here.
     *
     * @return 0 when the URI was shown, -1 otherwise
     */
    @SuppressWarnings("restricted")
    static int openURI(String uri) {
        byte[] uriChars = JniStringCodec.toModifiedUtf8(uri);
        try (Arena call = Arena.ofConfined()) {
            MemorySegment errorOut = call.allocate(ADDRESS);
            MemorySegment uriC = call.allocateFrom(JAVA_BYTE, uriChars);
            int success = (int) GTK_SHOW_URI.invokeExact(MemorySegment.NULL, uriC, 0, errorOut);
            if (success == 0) {
                MemorySegment error = errorOut.get(ADDRESS, 0);
                byte[] message;
                if (error.address() == 0) {
                    message = "Unspecified error.".getBytes(StandardCharsets.US_ASCII);
                } else {
                    MemorySegment text = error.reinterpret(G_ERROR_MESSAGE_OFFSET + ADDRESS.byteSize())
                            .get(ADDRESS, G_ERROR_MESSAGE_OFFSET);
                    message = cStringBytes(text);
                }
                ByteArrayOutputStream line = new ByteArrayOutputStream();
                line.writeBytes("Error opening URI ".getBytes(StandardCharsets.US_ASCII));
                line.write(uriChars, 0, uriChars.length - 1);
                line.writeBytes(" : ".getBytes(StandardCharsets.US_ASCII));
                line.writeBytes(message);
                line.write('\n');
                line.write(0);
                int written = (int) FPUTS.invokeExact(call.allocateFrom(JAVA_BYTE, line.toByteArray()), stderr());
                if (error.address() != 0) {
                    G_ERROR_FREE.invokeExact(error);
                }
            }
            return success != 0 ? 0 : -1;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication_staticView_1getMultiClickTime} ({@code GlassApplication.cpp}):
     * GtkSettings' {@code gtk-double-click-time}, read into a static that starts at -1 the first time it is -1.
     */
    static long multiClickTime() {
        if (multiClickTime == -1) {
            multiClickTime = gtkSettingInt("gtk-double-click-time", multiClickTime);
        }
        return multiClickTime;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication_staticView_1getMultiClickMaxX} ({@code GlassApplication.cpp}):
     * GtkSettings' {@code gtk-double-click-distance}, cached the same way. {@code staticView_getMultiClickMaxY}
     * called it and answered the same value.
     */
    static int multiClickMaxX() {
        if (multiClickDist == -1) {
            multiClickDist = gtkSettingInt("gtk-double-click-distance", multiClickDist);
        }
        return multiClickDist;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1supportsTransparentWindows} ({@code GlassApplication.cpp}):
     * {@code gdk_display_supports_composite(gdk_display_get_default()) && gdk_screen_is_composited(...)}, the second
     * call only made when the first answers true.
     */
    static boolean supportsTransparentWindows() {
        try {
            MemorySegment display = (MemorySegment) GDK_DISPLAY_GET_DEFAULT.invokeExact();
            if ((int) GDK_DISPLAY_SUPPORTS_COMPOSITE.invokeExact(display) == 0) {
                return false;
            }
            MemorySegment screen = (MemorySegment) GDK_SCREEN_GET_DEFAULT.invokeExact();
            return (int) GDK_SCREEN_IS_COMPOSITED.invokeExact(screen) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code g_object_get(gtk_settings_get_default(), name, &value, NULL)} where {@code value} held {@code current}
     * before the call, as the C's static did; answers what it holds afterwards.
     */
    private static int gtkSettingInt(String name, int current) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment value = call.allocate(JAVA_INT);
            value.set(JAVA_INT, 0, current);
            MemorySegment settings = (MemorySegment) GTK_SETTINGS_GET_DEFAULT.invokeExact();
            G_OBJECT_GET_ONE.invokeExact(settings, call.allocateFrom(name), value, MemorySegment.NULL);
            return value.get(JAVA_INT, 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The bytes of a C string before its terminator; {@code (null)} for {@code NULL}, as glibc's {@code %s} prints. */
    private static byte[] cStringBytes(MemorySegment text) throws Throwable {
        if (text.address() == 0) {
            return "(null)".getBytes(StandardCharsets.US_ASCII);
        }
        return cString(text);
    }

    /**
     * The bytes of a C string before its terminator, for every place the C ran {@code strlen} on the pointer: a
     * {@code NULL} is a {@link NullPointerException} here, where the C dereferenced it. Only {@link #cStringBytes}
     * prints {@code (null)}, which is what its {@code fprintf("%s", ...)} callers needed.
     */
    @SuppressWarnings("restricted")
    private static byte[] cString(MemorySegment text) throws Throwable {
        if (text.address() == 0) {
            throw new NullPointerException("NULL C string");
        }
        long length = (long) STRLEN.invokeExact(text);
        return text.reinterpret(length).toArray(JAVA_BYTE);
    }

    /**
     * {@code NewStringUTF}: the modified UTF-8 of a C string, or {@code null} for a {@code NULL} pointer, which is
     * what {@code NewStringUTF} answered for one.
     */
    private static String newStringUtf(MemorySegment text) throws Throwable {
        return text.address() == 0 ? null : JniStringCodec.fromNewStringUtf(cString(text));
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Screen list: GtkApplication.staticScreen_getScreens (GlassApplication.cpp -> rebuild_screens,
     * createJavaScreen, get_screen_workarea, get_current_desktop of glass_screen.cpp)
     * ------------------------------------------------------------------------------------------------------- */

    /** {@code GdkRectangle}: {@code gint x, y, width, height}. */
    private static final long GDK_RECTANGLE_SIZE = 16;

    /** {@code XA_CARDINAL} of {@code Xatom.h}. */
    private static final long XA_CARDINAL = 6;

    /** {@code AnyPropertyType} of {@code X.h}; also {@code None} and {@code Success}. */
    private static final long X_ANY_PROPERTY_TYPE = 0;

    /** {@code gint gdk_screen_get_n_monitors(GdkScreen *screen)}. */
    private static final MethodHandle GDK_SCREEN_GET_N_MONITORS = bind(GDK, LIB_GDK, "gdk_screen_get_n_monitors",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * {@code void gdk_screen_get_monitor_geometry(GdkScreen *screen, gint monitor_num, GdkRectangle *dest)}.
     */
    private static final MethodHandle GDK_SCREEN_GET_MONITOR_GEOMETRY = bind(GDK, LIB_GDK,
            "gdk_screen_get_monitor_geometry", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS));

    /** {@code GdkVisual *gdk_screen_get_system_visual(GdkScreen *screen)}. */
    private static final MethodHandle GDK_SCREEN_GET_SYSTEM_VISUAL = bind(GDK, LIB_GDK,
            "gdk_screen_get_system_visual", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /**
     * {@code gint gdk_visual_get_depth(GdkVisual *visual)}, which the {@code glass_gdk_visual_get_depth} of
     * {@code glass_general.cpp} wrapped at commit {@code 033187ad90}.
     */
    private static final MethodHandle GDK_VISUAL_GET_DEPTH = bind(GDK, LIB_GDK, "gdk_visual_get_depth",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * {@code gboolean gdk_rectangle_intersect(const GdkRectangle *src1, const GdkRectangle *src2,
     * GdkRectangle *dest)}.
     */
    private static final MethodHandle GDK_RECTANGLE_INTERSECT = bind(GDK, LIB_GDK, "gdk_rectangle_intersect",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /** {@code gint gdk_screen_get_width(GdkScreen *screen)}. */
    private static final MethodHandle GDK_SCREEN_GET_WIDTH = bind(GDK, LIB_GDK, "gdk_screen_get_width",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code gint gdk_screen_get_height(GdkScreen *screen)}. */
    private static final MethodHandle GDK_SCREEN_GET_HEIGHT = bind(GDK, LIB_GDK, "gdk_screen_get_height",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code gint gdk_screen_get_width_mm(GdkScreen *screen)}. */
    private static final MethodHandle GDK_SCREEN_GET_WIDTH_MM = bind(GDK, LIB_GDK, "gdk_screen_get_width_mm",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code gint gdk_screen_get_height_mm(GdkScreen *screen)}. */
    private static final MethodHandle GDK_SCREEN_GET_HEIGHT_MM = bind(GDK, LIB_GDK, "gdk_screen_get_height_mm",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code gint gdk_screen_get_monitor_width_mm(GdkScreen *screen, gint monitor_num)}. */
    private static final MethodHandle GDK_SCREEN_GET_MONITOR_WIDTH_MM = bind(GDK, LIB_GDK,
            "gdk_screen_get_monitor_width_mm", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /** {@code gint gdk_screen_get_monitor_height_mm(GdkScreen *screen, gint monitor_num)}. */
    private static final MethodHandle GDK_SCREEN_GET_MONITOR_HEIGHT_MM = bind(GDK, LIB_GDK,
            "gdk_screen_get_monitor_height_mm", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /** {@code GdkWindow *gdk_screen_get_root_window(GdkScreen *screen)}. */
    private static final MethodHandle GDK_SCREEN_GET_ROOT_WINDOW = bind(GDK, LIB_GDK, "gdk_screen_get_root_window",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code Window gdk_x11_window_get_xid(GdkWindow *window)}, which the {@code GDK_WINDOW_XID} macro calls. */
    private static final MethodHandle GDK_X11_WINDOW_GET_XID = bind(GDK, LIB_GDK, "gdk_x11_window_get_xid",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    /**
     * {@code int XGetWindowProperty(Display *, Window, Atom property, long offset, long length, Bool delete,
     * Atom req_type, Atom *type, int *format, unsigned long *nitems, unsigned long *after, unsigned char **prop)}.
     */
    private static final MethodHandle X_GET_WINDOW_PROPERTY = bind(X11, LIB_X11, "XGetWindowProperty",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_INT,
                    JAVA_LONG, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code int XFree(void *data)}. */
    private static final MethodHandle X_FREE = bind(X11, LIB_X11, "XFree", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * {@code rebuild_screens} ({@code glass_screen.cpp}): one {@code Screen} per monitor of the default
     * {@code GdkScreen}, in monitor order, each built by {@link #createScreen}.
     */
    static Screen[] screens() {
        try {
            MemorySegment screen = (MemorySegment) GDK_SCREEN_GET_DEFAULT.invokeExact();
            int monitors = (int) GDK_SCREEN_GET_N_MONITORS.invokeExact(screen);
            Screen[] result = new Screen[monitors];
            for (int i = 0; i < monitors; i++) {
                result[i] = createScreen(screen, i);
            }
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code createJavaScreen} ({@code glass_screen.cpp}): the work area of the screen intersected with the
     * monitor's geometry, both divided by the screen's UI scale as {@code float}s and truncated towards zero as
     * the C's {@code jint} conversions did, and a DPI computed from the monitor's physical size - or the screen's
     * when the monitor reports none and there is exactly one monitor, and 96 when neither is positive.
     */
    private static Screen createScreen(MemorySegment screen, int monitor) throws Throwable {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment workArea = screenWorkarea(call, screen);
            MemorySegment geometry = call.allocate(GDK_RECTANGLE_SIZE);
            GDK_SCREEN_GET_MONITOR_GEOMETRY.invokeExact(screen, monitor, geometry);
            MemorySegment visual = (MemorySegment) GDK_SCREEN_GET_SYSTEM_VISUAL.invokeExact(screen);
            MemorySegment working = call.allocate(GDK_RECTANGLE_SIZE);
            // the C ignored the return too: an empty intersection leaves dest zeroed and is used as it is
            int ignored = (int) GDK_RECTANGLE_INTERSECT.invokeExact(workArea, geometry, working);

            float uiScale = getUIScale(screen);

            int gx = geometry.get(JAVA_INT, 0);
            int gy = geometry.get(JAVA_INT, 4);
            int gw = geometry.get(JAVA_INT, 8);
            int gh = geometry.get(JAVA_INT, 12);
            int mx = (int) (gx / uiScale);
            int my = (int) (gy / uiScale);
            int mw = (int) (gw / uiScale);
            int mh = (int) (gh / uiScale);
            int wx = (int) (working.get(JAVA_INT, 0) / uiScale);
            int wy = (int) (working.get(JAVA_INT, 4) / uiScale);
            int ww = (int) (working.get(JAVA_INT, 8) / uiScale);
            int wh = (int) (working.get(JAVA_INT, 12) / uiScale);

            int mmW = (int) GDK_SCREEN_GET_MONITOR_WIDTH_MM.invokeExact(screen, monitor);
            int mmH = (int) GDK_SCREEN_GET_MONITOR_HEIGHT_MM.invokeExact(screen, monitor);
            if (mmW <= 0 || mmH <= 0) {
                if ((int) GDK_SCREEN_GET_N_MONITORS.invokeExact(screen) == 1) {
                    mmW = (int) GDK_SCREEN_GET_WIDTH_MM.invokeExact(screen);
                    mmH = (int) GDK_SCREEN_GET_HEIGHT_MM.invokeExact(screen);
                }
            }
            int dpiX;
            int dpiY;
            if (mmW <= 0 || mmH <= 0) {
                dpiX = DEFAULT_DPI;
                dpiY = DEFAULT_DPI;
            } else {
                dpiX = (mw * 254) / (mmW * 10);
                dpiY = (mh * 254) / (mmH * 10);
            }
            int depth = visual.address() == 0 ? 0 : (int) GDK_VISUAL_GET_DEPTH.invokeExact(visual);
            return new Screen(monitor, depth, mx, my, mw, mh, gx, gy, gw, gh, wx, wy, ww, wh, dpiX, dpiY,
                    uiScale, uiScale, uiScale, uiScale);
        }
    }

    /**
     * {@code get_screen_workarea} ({@code glass_screen.cpp}): the {@code _NET_WORKAREA} rectangle of the current
     * desktop, or the whole screen when the atom, the property or the desktop index is missing. The returned
     * segment is a {@code GdkRectangle} in {@code call}.
     */
    private static MemorySegment screenWorkarea(Arena call, MemorySegment screen) throws Throwable {
        MemorySegment display = (MemorySegment) GDK_X11_DISPLAY_GET_XDISPLAY.invokeExact(
                (MemorySegment) GDK_DISPLAY_GET_DEFAULT.invokeExact());
        MemorySegment rectangle = call.allocate(GDK_RECTANGLE_SIZE);
        rectangle.set(JAVA_INT, 0, 0);
        rectangle.set(JAVA_INT, 4, 0);
        rectangle.set(JAVA_INT, 8, (int) GDK_SCREEN_GET_WIDTH.invokeExact(screen));
        rectangle.set(JAVA_INT, 12, (int) GDK_SCREEN_GET_HEIGHT.invokeExact(screen));

        long workareaAtom = (long) X_INTERN_ATOM.invokeExact(display, call.allocateFrom("_NET_WORKAREA"), 1);
        if (workareaAtom == X_ANY_PROPERTY_TYPE) {
            return rectangle;
        }
        MemorySegment root = (MemorySegment) GDK_SCREEN_GET_ROOT_WINDOW.invokeExact(screen);
        long xid = (long) GDK_X11_WINDOW_GET_XID.invokeExact(root);
        MemorySegment type = call.allocate(JAVA_LONG);
        MemorySegment format = call.allocate(JAVA_INT);
        MemorySegment num = call.allocate(JAVA_LONG);
        MemorySegment left = call.allocate(JAVA_LONG);
        MemorySegment dataOut = call.allocate(ADDRESS);
        int result = (int) X_GET_WINDOW_PROPERTY.invokeExact(display, xid, workareaAtom, 0L, Long.MAX_VALUE, 0,
                X_ANY_PROPERTY_TYPE, type, format, num, left, dataOut);
        MemorySegment data = dataOut.get(ADDRESS, 0);
        if (result == 0 && data.address() != 0) {
            if (type.get(JAVA_LONG, 0) != X_ANY_PROPERTY_TYPE && format.get(JAVA_INT, 0) == 32) {
                long count = num.get(JAVA_LONG, 0);
                int currentDesktop = currentDesktop(call, display, screen);
                if (Integer.toUnsignedLong(currentDesktop) < Long.divideUnsigned(count, 4)) {
                    long base = Integer.toUnsignedLong(currentDesktop) * 4;
                    MemorySegment values = reinterpretLongs(data, base + 4);
                    rectangle.set(JAVA_INT, 0, (int) values.getAtIndex(JAVA_LONG, base));
                    rectangle.set(JAVA_INT, 4, (int) values.getAtIndex(JAVA_LONG, base + 1));
                    rectangle.set(JAVA_INT, 8, (int) values.getAtIndex(JAVA_LONG, base + 2));
                    rectangle.set(JAVA_INT, 12, (int) values.getAtIndex(JAVA_LONG, base + 3));
                }
            }
            int freed = (int) X_FREE.invokeExact(data);
        }
        return rectangle;
    }

    /**
     * {@code get_current_desktop} ({@code glass_screen.cpp}): the first {@code _NET_CURRENT_DESKTOP} cardinal of
     * the screen's root window, 0 when the atom or the property is missing.
     */
    private static int currentDesktop(Arena call, MemorySegment display, MemorySegment screen) throws Throwable {
        long atom = (long) X_INTERN_ATOM.invokeExact(display, call.allocateFrom("_NET_CURRENT_DESKTOP"), 1);
        if (atom == X_ANY_PROPERTY_TYPE) {
            return 0;
        }
        MemorySegment root = (MemorySegment) GDK_SCREEN_GET_ROOT_WINDOW.invokeExact(screen);
        long xid = (long) GDK_X11_WINDOW_GET_XID.invokeExact(root);
        MemorySegment type = call.allocate(JAVA_LONG);
        MemorySegment format = call.allocate(JAVA_INT);
        MemorySegment num = call.allocate(JAVA_LONG);
        MemorySegment left = call.allocate(JAVA_LONG);
        MemorySegment dataOut = call.allocate(ADDRESS);
        int result = (int) X_GET_WINDOW_PROPERTY.invokeExact(display, xid, atom, 0L, Long.MAX_VALUE, 0,
                XA_CARDINAL, type, format, num, left, dataOut);
        MemorySegment data = dataOut.get(ADDRESS, 0);
        int desktop = 0;
        if (result == 0 && data.address() != 0) {
            if (type.get(JAVA_LONG, 0) == XA_CARDINAL && format.get(JAVA_INT, 0) == 32) {
                desktop = (int) reinterpretLongs(data, 1).get(JAVA_LONG, 0);
            }
            int freed = (int) X_FREE.invokeExact(data);
        }
        return desktop;
    }

    /** {@code count} {@code unsigned long}s of an X property, which {@code XGetWindowProperty} returned unsized. */
    @SuppressWarnings("restricted")
    private static MemorySegment reinterpretLongs(MemorySegment data, long count) {
        return data.reinterpret(count * JAVA_LONG.byteSize());
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Platform preferences: GtkApplication.getPlatformPreferences (GlassApplication.cpp -> PlatformSupport.cpp),
     * and the GtkSettings and GNetworkMonitor handlers that PlatformSupport connected from _init
     * ------------------------------------------------------------------------------------------------------- */

    /** {@code GdkColor}: {@code guint32 pixel}, then {@code guint16 red, green, blue}. */
    private static final long GDK_COLOR_SIZE = 12;

    /** {@code G_CONNECT_AFTER} of {@code gsignal.h}. */
    private static final int G_CONNECT_AFTER = 1;

    /** {@code GtkStyle *gtk_style_new(void)}. */
    private static final MethodHandle GTK_STYLE_NEW = bind(GTK, LIB_GTK, "gtk_style_new",
            FunctionDescriptor.of(ADDRESS));

    /**
     * {@code gboolean gtk_style_lookup_color(GtkStyle *style, const gchar *color_name, GdkColor *color)}.
     */
    private static final MethodHandle GTK_STYLE_LOOKUP_COLOR = bind(GTK, LIB_GTK, "gtk_style_lookup_color",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /**
     * {@code GParamSpec *g_object_class_find_property(GObjectClass *oclass, const gchar *property_name)}; the
     * class comes from the first pointer of the instance, which is what {@code G_OBJECT_GET_CLASS} reads.
     */
    private static final MethodHandle G_OBJECT_CLASS_FIND_PROPERTY = bind(GOBJECT, LIB_GOBJECT,
            "g_object_class_find_property", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    /** {@code GNetworkMonitor *g_network_monitor_get_default(void)}. */
    private static final MethodHandle G_NETWORK_MONITOR_GET_DEFAULT = bind(GIO, LIB_GIO,
            "g_network_monitor_get_default", FunctionDescriptor.of(ADDRESS));

    /** {@code gboolean g_network_monitor_get_network_metered(GNetworkMonitor *monitor)}. */
    private static final MethodHandle G_NETWORK_MONITOR_GET_NETWORK_METERED = bind(GIO, LIB_GIO,
            "g_network_monitor_get_network_metered", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * {@code gulong g_signal_connect_data(gpointer instance, const gchar *detailed_signal, GCallback c_handler,
     * gpointer data, GClosureNotify destroy_data, GConnectFlags connect_flags)}.
     */
    private static final MethodHandle G_SIGNAL_CONNECT_DATA = bind(GOBJECT, LIB_GOBJECT, "g_signal_connect_data",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));

    /** {@code void g_signal_handler_disconnect(gpointer instance, gulong handler_id)}. */
    private static final MethodHandle G_SIGNAL_HANDLER_DISCONNECT = bind(GOBJECT, LIB_GOBJECT,
            "g_signal_handler_disconnect", FunctionDescriptor.ofVoid(ADDRESS, JAVA_LONG));

    /** {@code void (*)(GObject *, GParamSpec *, gpointer)}: the {@code notifySettingChanged} of the C. */
    private static final MemorySegment SETTING_CHANGED_CALLBACK = upcallStub("onSettingChanged",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));

    /** {@code void (*)(GNetworkMonitor *, gboolean, gpointer)}: the {@code notifyNetworkChanged} of the C. */
    private static final MemorySegment NETWORK_CHANGED_CALLBACK = upcallStub("onNetworkChanged",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS));

    /** {@code PlatformSupport::OBSERVED_SETTINGS} ({@code PlatformSupport.h}), in its order. */
    private static final String[] OBSERVED_SETTINGS = {
        "notify::gtk-theme-name",
        "notify::gtk-enable-animations",
        "notify::gtk-overlay-scrolling"
    };

    /** The {@code lookupColorName -> prefColorName} pairs of {@code collectPreferences}, in its order. */
    private static final String[] PREFERENCE_COLORS = {
        "theme_fg_color", "theme_bg_color", "theme_base_color", "theme_selected_bg_color",
        "theme_selected_fg_color", "insensitive_bg_color", "insensitive_fg_color", "insensitive_base_color",
        "theme_unfocused_fg_color", "theme_unfocused_bg_color", "theme_unfocused_base_color",
        "theme_unfocused_selected_bg_color", "theme_unfocused_selected_fg_color", "borders", "unfocused_borders",
        "warning_color", "error_color", "success_color"
    };

    /**
     * The one {@code PlatformSupport} of the process, which {@code _init} created and {@code _terminateLoop}
     * deleted ({@code GlassApplication.cpp}); {@code null} outside that window, where
     * {@code getPlatformPreferences} answered {@code NULL}.
     */
    private static PlatformSupport platformSupport;

    /**
     * {@code PlatformSupport} ({@code PlatformSupport.cpp}): the GtkSettings and GNetworkMonitor signal handlers
     * that report a preference change to the application, and the preferences last reported.
     */
    private static final class PlatformSupport {

        private final GtkApplication application;
        private final MemorySegment settings;
        private final long[] settingChangedHandlers = new long[OBSERVED_SETTINGS.length];
        private final long networkChangedHandler;
        private Map<String, Object> preferences;

        private PlatformSupport(GtkApplication application) throws Throwable {
            this.application = application;
            settings = (MemorySegment) GTK_SETTINGS_GET_DEFAULT.invokeExact();
            try (Arena call = Arena.ofConfined()) {
                if (settings.address() != 0) {
                    for (int i = 0; i < OBSERVED_SETTINGS.length; i++) {
                        settingChangedHandlers[i] = (long) G_SIGNAL_CONNECT_DATA.invokeExact(settings,
                                call.allocateFrom(OBSERVED_SETTINGS[i]), SETTING_CHANGED_CALLBACK,
                                MemorySegment.NULL, MemorySegment.NULL, G_CONNECT_AFTER);
                    }
                }
                MemorySegment monitor = (MemorySegment) G_NETWORK_MONITOR_GET_DEFAULT.invokeExact();
                networkChangedHandler = (long) G_SIGNAL_CONNECT_DATA.invokeExact(monitor,
                        call.allocateFrom("network-changed"), NETWORK_CHANGED_CALLBACK, MemorySegment.NULL,
                        MemorySegment.NULL, G_CONNECT_AFTER);
            }
        }

        /** {@code ~PlatformSupport}: disconnects every handler it connected. */
        private void dispose() throws Throwable {
            MemorySegment current = (MemorySegment) GTK_SETTINGS_GET_DEFAULT.invokeExact();
            for (long handler : settingChangedHandlers) {
                if (handler != 0) {
                    G_SIGNAL_HANDLER_DISCONNECT.invokeExact(current, handler);
                }
            }
            MemorySegment monitor = (MemorySegment) G_NETWORK_MONITOR_GET_DEFAULT.invokeExact();
            G_SIGNAL_HANDLER_DISCONNECT.invokeExact(monitor, networkChangedHandler);
        }
    }

    /**
     * The {@code new PlatformSupport(env, obj)} that {@code Java_com_sun_glass_ui_gtk_GtkApplication__1init}
     * ran last ({@code GlassApplication.cpp}): connects the preference handlers for the application's lifetime.
     */
    static void platformSupportCreate(GtkApplication application) {
        try {
            platformSupport = new PlatformSupport(application);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The {@code delete platformSupport} of
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1terminateLoop} ({@code GlassApplication.cpp}).
     */
    static void platformSupportDestroy() {
        PlatformSupport support = platformSupport;
        if (support != null) {
            platformSupport = null;
            try {
                support.dispose();
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication_getPlatformPreferences} ({@code GlassApplication.cpp}):
     * {@code platformSupport->collectPreferences()}, or {@code null} before {@code _init} and after
     * {@code _terminateLoop}.
     */
    static Map<String, Object> platformPreferences() {
        return platformSupport == null ? null : collectPreferences();
    }

    /**
     * {@code PlatformSupport::collectPreferences} ({@code PlatformSupport.cpp}): the 18 theme colours of a fresh
     * {@code GtkStyle}, the theme name, the animation and overlay-scrolling settings and whether the network is
     * metered, in a {@code HashMap}. A colour the style does not define, a {@code NULL} theme name and an absent
     * {@code gtk-overlay-scrolling} property leave their key out, as the C did.
     */
    private static Map<String, Object> collectPreferences() {
        try (Arena call = Arena.ofConfined()) {
            Map<String, Object> prefs = new HashMap<>();
            MemorySegment style = (MemorySegment) GTK_STYLE_NEW.invokeExact();
            if (style.address() == 0) {
                return null;
            }
            MemorySegment color = call.allocate(GDK_COLOR_SIZE);
            for (String name : PREFERENCE_COLORS) {
                putColor(prefs, style, call.allocateFrom(name), color, "GTK." + name);
            }
            G_OBJECT_UNREF.invokeExact(style);

            MemorySegment settings = (MemorySegment) GTK_SETTINGS_GET_DEFAULT.invokeExact();
            if (settings.address() != 0) {
                MemorySegment out = call.allocate(ADDRESS);
                G_OBJECT_GET_ONE.invokeExact(settings, call.allocateFrom("gtk-theme-name"), out,
                        MemorySegment.NULL);
                MemorySegment themeName = out.get(ADDRESS, 0);
                putString(prefs, "GTK.theme_name", themeName);
                G_FREE_CALL.invokeExact(themeName);

                MemorySegment flag = call.allocate(JAVA_INT);
                flag.set(JAVA_INT, 0, 1);
                G_OBJECT_GET_ONE.invokeExact(settings, call.allocateFrom("gtk-enable-animations"), flag,
                        MemorySegment.NULL);
                putBoolean(prefs, "GTK.enable_animations", flag.get(JAVA_INT, 0) != 0);

                MemorySegment settingsClass = settings.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
                MemorySegment overlay = (MemorySegment) G_OBJECT_CLASS_FIND_PROPERTY.invokeExact(settingsClass,
                        call.allocateFrom("gtk-overlay-scrolling"));
                if (overlay.address() != 0) {
                    flag.set(JAVA_INT, 0, 1);
                    G_OBJECT_GET_ONE.invokeExact(settings, call.allocateFrom("gtk-overlay-scrolling"), flag,
                            MemorySegment.NULL);
                    putBoolean(prefs, "GTK.overlay_scrolling", flag.get(JAVA_INT, 0) != 0);
                }
            }

            MemorySegment monitor = (MemorySegment) G_NETWORK_MONITOR_GET_DEFAULT.invokeExact();
            int metered = (int) G_NETWORK_MONITOR_GET_NETWORK_METERED.invokeExact(monitor);
            putBoolean(prefs, "GTK.network_metered", metered != 0);
            return prefs;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code putColor} ({@code PlatformSupport.cpp}): {@code Color.rgb} of the style colour {@code lookupName},
     * each channel clamped and scaled by {@code (int) (CLAMP(c / 65535.0, 0, 1) * 255.0)}; nothing when the style
     * does not define it. A throw is reported and the key left out, which is what {@code CHECK_JNI_EXCEPTION} did.
     */
    private static void putColor(Map<String, Object> prefs, MemorySegment style, MemorySegment lookupName,
                                 MemorySegment color, String key) throws Throwable {
        if ((int) GTK_STYLE_LOOKUP_COLOR.invokeExact(style, lookupName, color) == 0) {
            return;
        }
        int red = Short.toUnsignedInt(color.get(JAVA_SHORT, 4));
        int green = Short.toUnsignedInt(color.get(JAVA_SHORT, 6));
        int blue = Short.toUnsignedInt(color.get(JAVA_SHORT, 8));
        try {
            prefs.put(key, Color.rgb(channel(red), channel(green), channel(blue), 1.0));
        } catch (Throwable t) {
            reportException(t);
        }
    }

    /** {@code (int) (CLAMP((double) c / 65535.0, 0.0, 1.0) * 255.0)} of {@code putColor}. */
    private static int channel(int value) {
        double scaled = value / 65535.0;
        return (int) (Math.max(0.0, Math.min(1.0, scaled)) * 255.0);
    }

    /**
     * {@code putString} ({@code PlatformSupport.cpp}): the value decoded as {@code NewStringUTF} decoded it, and
     * nothing at all for a {@code NULL} pointer, whose {@code NewStringUTF} answered {@code NULL}.
     */
    private static void putString(Map<String, Object> prefs, String key, MemorySegment value) throws Throwable {
        if (value.address() == 0) {
            return;
        }
        prefs.put(key, JniStringCodec.fromNewStringUtf(cString(value)));
    }

    /** {@code putBoolean} ({@code PlatformSupport.cpp}): {@code Boolean.TRUE} or {@code Boolean.FALSE}. */
    private static void putBoolean(Map<String, Object> prefs, String key, boolean value) {
        prefs.put(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * {@code notifySettingChanged} ({@code PlatformSupport.cpp}), the {@code notify::} handler of the three
     * observed GtkSettings properties.
     */
    static void onSettingChanged(MemorySegment object, MemorySegment paramSpec, MemorySegment data) {
        updatePreferences();
    }

    /** {@code notifyNetworkChanged} ({@code PlatformSupport.cpp}), the GNetworkMonitor handler. */
    static void onNetworkChanged(MemorySegment monitor, int available, MemorySegment data) {
        updatePreferences();
    }

    /**
     * {@code PlatformSupport::updatePreferences} ({@code PlatformSupport.cpp}): collects the preferences again and,
     * when they differ from the ones last reported, keeps them and hands an unmodifiable view to
     * {@code Application.notifyPreferencesChanged}. Nothing may leave this method: it runs in an upcall stub, and
     * the C reported and cleared every exception of this path.
     */
    private static void updatePreferences() {
        try {
            PlatformSupport support = platformSupport;
            if (support == null) {
                return;
            }
            Map<String, Object> newPreferences = collectPreferences();
            boolean preferencesChanged;
            try {
                preferencesChanged = newPreferences != null && !newPreferences.equals(support.preferences);
            } catch (Throwable t) {
                reportException(t);
                return;
            }
            if (preferencesChanged) {
                support.preferences = newPreferences;
                Map<String, Object> unmodifiable = Collections.unmodifiableMap(newPreferences);
                try {
                    support.application.notifyPlatformPreferencesChanged(unmodifiable);
                } catch (Throwable t) {
                    reportException(t);
                }
            }
        } catch (Throwable t) {
            reportException(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * System clipboard: GtkSystemClipboard init, dispose, isOwner, pushToSystem, pushTargetActionToSystem,
     * popFromSystem, supportedSourceActionsFromSystem, mimesFromSystem (GlassSystemClipboard.cpp), and the
     * uris_to_java and glass_gtk_selection_data_get_data_with_length of glass_general.cpp that only they called
     * ------------------------------------------------------------------------------------------------------- */

    /**
     * {@code GDK_SELECTION_CLIPBOARD}, which {@code gdkselection.h} defines as
     * {@code (GdkAtom) GUINT_TO_POINTER(69)}. A {@code GdkAtom} is an opaque pointer; every one of them is passed
     * here as a {@code long}, which is how the System V and AAPCS64 ABIs pass a pointer argument.
     */
    private static final long GDK_SELECTION_CLIPBOARD = 69;

    /** {@code GtkTargetEntry}: {@code gchar *target; guint flags; guint info;}. */
    private static final long GTK_TARGET_ENTRY_SIZE = 16;

    /** {@code FILE_PREFIX} of {@code glass_general.h}. */
    private static final String FILE_PREFIX = "file://";

    /** {@code URI_LIST_COMMENT_PREFIX} of {@code glass_general.h}. */
    private static final String URI_LIST_COMMENT_PREFIX = "#";

    /** {@code URI_LIST_LINE_BREAK} of {@code glass_general.h}. */
    private static final String URI_LIST_LINE_BREAK = "\r\n";

    /** {@code GtkClipboard *gtk_clipboard_get(GdkAtom selection)}. */
    private static final MethodHandle GTK_CLIPBOARD_GET = bind(GTK, LIB_GTK, "gtk_clipboard_get",
            FunctionDescriptor.of(ADDRESS, JAVA_LONG));

    /** {@code GdkAtom gdk_atom_intern_static_string(const gchar *atom_name)}. */
    private static final MethodHandle GDK_ATOM_INTERN_STATIC_STRING = bind(GDK, LIB_GDK,
            "gdk_atom_intern_static_string", FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    /** {@code GdkAtom gdk_atom_intern(const gchar *atom_name, gboolean only_if_exists)}. */
    private static final MethodHandle GDK_ATOM_INTERN = bind(GDK, LIB_GDK, "gdk_atom_intern",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));

    /** {@code gchar *gdk_atom_name(GdkAtom atom)}. */
    private static final MethodHandle GDK_ATOM_NAME = bind(GDK, LIB_GDK, "gdk_atom_name",
            FunctionDescriptor.of(ADDRESS, JAVA_LONG));

    /** {@code GtkTargetList *gtk_target_list_new(const GtkTargetEntry *targets, guint ntargets)}. */
    private static final MethodHandle GTK_TARGET_LIST_NEW = bind(GTK, LIB_GTK, "gtk_target_list_new",
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));

    /** {@code void gtk_target_list_add(GtkTargetList *list, GdkAtom target, guint flags, guint info)}. */
    private static final MethodHandle GTK_TARGET_LIST_ADD = bind(GTK, LIB_GTK, "gtk_target_list_add",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT));

    /** {@code void gtk_target_list_add_text_targets(GtkTargetList *list, guint info)}. */
    private static final MethodHandle GTK_TARGET_LIST_ADD_TEXT_TARGETS = bind(GTK, LIB_GTK,
            "gtk_target_list_add_text_targets", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /**
     * {@code void gtk_target_list_add_image_targets(GtkTargetList *list, guint info, gboolean writable)}.
     */
    private static final MethodHandle GTK_TARGET_LIST_ADD_IMAGE_TARGETS = bind(GTK, LIB_GTK,
            "gtk_target_list_add_image_targets", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code void gtk_target_list_unref(GtkTargetList *list)}. */
    private static final MethodHandle GTK_TARGET_LIST_UNREF = bind(GTK, LIB_GTK, "gtk_target_list_unref",
            FunctionDescriptor.ofVoid(ADDRESS));

    /**
     * {@code GtkTargetEntry *gtk_target_table_new_from_list(GtkTargetList *list, gint *n_targets)}; an empty list
     * answers {@code NULL}, which is what the C read as "clear the clipboard".
     */
    private static final MethodHandle GTK_TARGET_TABLE_NEW_FROM_LIST = bind(GTK, LIB_GTK,
            "gtk_target_table_new_from_list", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    /** {@code void gtk_target_table_free(GtkTargetEntry *targets, gint n_targets)}. */
    private static final MethodHandle GTK_TARGET_TABLE_FREE = bind(GTK, LIB_GTK, "gtk_target_table_free",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /**
     * {@code gboolean gtk_clipboard_set_with_data(GtkClipboard *, const GtkTargetEntry *, guint n_targets,
     * GtkClipboardGetFunc, GtkClipboardClearFunc, gpointer user_data)}; the C ignored the result.
     */
    private static final MethodHandle GTK_CLIPBOARD_SET_WITH_DATA = bind(GTK, LIB_GTK,
            "gtk_clipboard_set_with_data", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS,
                    ADDRESS, ADDRESS));

    /** {@code gchar *gtk_clipboard_wait_for_text(GtkClipboard *)}; iterates the main loop. */
    private static final MethodHandle GTK_CLIPBOARD_WAIT_FOR_TEXT = bind(GTK, LIB_GTK,
            "gtk_clipboard_wait_for_text", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code gchar **gtk_clipboard_wait_for_uris(GtkClipboard *)}; iterates the main loop. */
    private static final MethodHandle GTK_CLIPBOARD_WAIT_FOR_URIS = bind(GTK, LIB_GTK,
            "gtk_clipboard_wait_for_uris", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /**
     * {@code GtkSelectionData *gtk_clipboard_wait_for_contents(GtkClipboard *, GdkAtom target)}; iterates the
     * main loop.
     */
    private static final MethodHandle GTK_CLIPBOARD_WAIT_FOR_CONTENTS = bind(GTK, LIB_GTK,
            "gtk_clipboard_wait_for_contents", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));

    /** {@code GdkPixbuf *gtk_clipboard_wait_for_image(GtkClipboard *)}; iterates the main loop. */
    private static final MethodHandle GTK_CLIPBOARD_WAIT_FOR_IMAGE = bind(GTK, LIB_GTK,
            "gtk_clipboard_wait_for_image", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /**
     * {@code gboolean gtk_clipboard_wait_for_targets(GtkClipboard *, GdkAtom **targets, gint *n_targets)};
     * iterates the main loop, and writes {@code NULL} and 0 when there are none.
     */
    private static final MethodHandle GTK_CLIPBOARD_WAIT_FOR_TARGETS = bind(GTK, LIB_GTK,
            "gtk_clipboard_wait_for_targets", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /** {@code gboolean gtk_targets_include_text(GdkAtom *targets, gint n_targets)}. */
    private static final MethodHandle GTK_TARGETS_INCLUDE_TEXT = bind(GTK, LIB_GTK, "gtk_targets_include_text",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /** {@code gboolean gtk_targets_include_image(GdkAtom *targets, gint n_targets, gboolean writable)}. */
    private static final MethodHandle GTK_TARGETS_INCLUDE_IMAGE = bind(GTK, LIB_GTK, "gtk_targets_include_image",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code GdkAtom gtk_selection_data_get_target(const GtkSelectionData *)}. */
    private static final MethodHandle GTK_SELECTION_DATA_GET_TARGET = bind(GTK, LIB_GTK,
            "gtk_selection_data_get_target", FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    /** {@code gint gtk_selection_data_get_length(const GtkSelectionData *)}. */
    private static final MethodHandle GTK_SELECTION_DATA_GET_LENGTH = bind(GTK, LIB_GTK,
            "gtk_selection_data_get_length", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code const guchar *gtk_selection_data_get_data(const GtkSelectionData *)}. */
    private static final MethodHandle GTK_SELECTION_DATA_GET_DATA = bind(GTK, LIB_GTK,
            "gtk_selection_data_get_data", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code void gtk_selection_data_free(GtkSelectionData *)}. */
    private static final MethodHandle GTK_SELECTION_DATA_FREE = bind(GTK, LIB_GTK, "gtk_selection_data_free",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code gboolean gtk_selection_data_set_text(GtkSelectionData *, const gchar *str, gint len)}. */
    private static final MethodHandle GTK_SELECTION_DATA_SET_TEXT = bind(GTK, LIB_GTK,
            "gtk_selection_data_set_text", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

    /**
     * {@code void gtk_selection_data_set(GtkSelectionData *, GdkAtom type, gint format, const guchar *data,
     * gint length)}.
     */
    private static final MethodHandle GTK_SELECTION_DATA_SET = bind(GTK, LIB_GTK, "gtk_selection_data_set",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT));

    /** {@code gboolean gtk_selection_data_set_uris(GtkSelectionData *, gchar **uris)}. */
    private static final MethodHandle GTK_SELECTION_DATA_SET_URIS = bind(GTK, LIB_GTK,
            "gtk_selection_data_set_uris", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code gboolean gtk_selection_data_set_pixbuf(GtkSelectionData *, GdkPixbuf *)}. */
    private static final MethodHandle GTK_SELECTION_DATA_SET_PIXBUF = bind(GTK, LIB_GTK,
            "gtk_selection_data_set_pixbuf", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code gboolean gdk_pixbuf_get_has_alpha(const GdkPixbuf *)}. */
    private static final MethodHandle GDK_PIXBUF_GET_HAS_ALPHA = bind(GDK_PIXBUF, LIB_GDK_PIXBUF,
            "gdk_pixbuf_get_has_alpha", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int gdk_pixbuf_get_width(const GdkPixbuf *)}. */
    private static final MethodHandle GDK_PIXBUF_GET_WIDTH = bind(GDK_PIXBUF, LIB_GDK_PIXBUF,
            "gdk_pixbuf_get_width", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int gdk_pixbuf_get_height(const GdkPixbuf *)}. */
    private static final MethodHandle GDK_PIXBUF_GET_HEIGHT = bind(GDK_PIXBUF, LIB_GDK_PIXBUF,
            "gdk_pixbuf_get_height", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int gdk_pixbuf_get_rowstride(const GdkPixbuf *)}. */
    private static final MethodHandle GDK_PIXBUF_GET_ROWSTRIDE = bind(GDK_PIXBUF, LIB_GDK_PIXBUF,
            "gdk_pixbuf_get_rowstride", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code guint g_strv_length(gchar **str_array)}. */
    private static final MethodHandle G_STRV_LENGTH = bind(GLIB, LIB_GLIB, "g_strv_length",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code void g_strfreev(gchar **str_array)}. */
    private static final MethodHandle G_STRFREEV = bind(GLIB, LIB_GLIB, "g_strfreev",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code gchar *g_filename_from_uri(const gchar *uri, gchar **hostname, GError **error)}. */
    private static final MethodHandle G_FILENAME_FROM_URI = bind(GLIB, LIB_GLIB, "g_filename_from_uri",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code gchar *g_filename_to_uri(const gchar *filename, const gchar *hostname, GError **error)}. */
    private static final MethodHandle G_FILENAME_TO_URI = bind(GLIB, LIB_GLIB, "g_filename_to_uri",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code void (*GtkClipboardOwnerChange)(GtkClipboard *, GdkEventOwnerChange *, gpointer)}. */
    private static final MemorySegment CLIPBOARD_OWNER_CHANGE_CALLBACK = upcallStub("onClipboardOwnerChanged",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));

    /**
     * {@code void (*GtkClipboardGetFunc)(GtkClipboard *, GtkSelectionData *, guint info, gpointer user_data)}.
     */
    private static final MemorySegment CLIPBOARD_GET_CALLBACK = upcallStub("onClipboardGetData",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT, ADDRESS));

    /** {@code void (*GtkClipboardClearFunc)(GtkClipboard *, gpointer user_data)}. */
    private static final MemorySegment CLIPBOARD_CLEAR_CALLBACK = upcallStub("onClipboardClearData",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /**
     * The content maps handed to GTK, keyed by the id that stands in for the JNI global reference the C passed as
     * the {@code gpointer}. An entry lives exactly as long as the global reference did: from
     * {@link #clipboardPush} until GTK's clear callback.
     */
    private static final Map<Long, Map<String, Object>> CLIPBOARD_DATA = new ConcurrentHashMap<>();

    /**
     * The clipboard peers connected to the {@code owner-change} signal, keyed the same way; the C passed its
     * {@code jclipboard} global reference as the {@code gpointer} of {@code g_signal_connect}.
     */
    private static final Map<Long, Clipboard> CLIPBOARD_PEERS = new ConcurrentHashMap<>();

    private static final AtomicLong NEXT_CLIPBOARD_ID = new AtomicLong(1);

    /** The {@code static GtkClipboard *clipboard} of {@code get_clipboard}. */
    private static MemorySegment clipboard;

    /** The {@code static gboolean is_clipboard_owner}. */
    private static boolean isClipboardOwner;

    /** The {@code static gboolean is_clipboard_updated_by_glass}. */
    private static boolean isClipboardUpdatedByGlass;

    /** The id of the peer in {@link #CLIPBOARD_PEERS}, standing in for the {@code static jobject jclipboard}. */
    private static long clipboardPeerId;

    /** The {@code static gulong owner_change_handler_id}. */
    private static long ownerChangeHandlerId;

    /** The {@code static int initialized} of {@code init_atoms}. */
    private static boolean atomsInitialized;

    private static long mimeTextPlainTarget;
    private static long mimeTextUriListTarget;
    private static long mimeJavaImage;
    private static long mimeFilesTarget;

    /**
     * {@code init_atoms} ({@code GlassSystemClipboard.cpp}): interns the four atoms once. The {@code String}
     * method ids and the {@code "UTF-8"} global reference it also cached went with JNI.
     * <p>
     * {@code gdk_atom_intern_static_string} keeps the pointer it is given instead of copying the name, which is
     * why the C passed string literals; these names are therefore allocated in {@link Arena#global()} and live as
     * long as the process, exactly as the literals in {@code .rodata} did.
     */
    private static void initAtoms() throws Throwable {
        if (atomsInitialized) {
            return;
        }
        Arena forever = Arena.global();
        mimeTextPlainTarget = (long) GDK_ATOM_INTERN_STATIC_STRING.invokeExact(forever.allocateFrom("text/plain"));
        mimeTextUriListTarget = (long) GDK_ATOM_INTERN_STATIC_STRING.invokeExact(
                forever.allocateFrom("text/uri-list"));
        mimeJavaImage = (long) GDK_ATOM_INTERN_STATIC_STRING.invokeExact(
                forever.allocateFrom("application/x-java-rawimage"));
        mimeFilesTarget = (long) GDK_ATOM_INTERN_STATIC_STRING.invokeExact(
                forever.allocateFrom("application/x-java-file-list"));
        atomsInitialized = true;
    }

    /** {@code get_clipboard} ({@code GlassSystemClipboard.cpp}): {@code GDK_SELECTION_CLIPBOARD}, fetched once. */
    private static MemorySegment getClipboard() throws Throwable {
        if (clipboard == null) {
            clipboard = (MemorySegment) GTK_CLIPBOARD_GET.invokeExact(GDK_SELECTION_CLIPBOARD);
        }
        return clipboard;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_init} ({@code GlassSystemClipboard.cpp}): connects
     * {@code owner-change} for {@code peer}. A second {@code init} overwrote the handler id and leaked the first
     * reference, and its {@code ERROR0} message was compiled out of a build without {@code VERBOSE}
     * ({@code glass_general.h} at commit {@code 033187ad90}); the first handler stayed connected to its own peer,
     * which the registry reproduces.
     */
    static void clipboardInit(Clipboard peer) {
        try (Arena call = Arena.ofConfined()) {
            long id = NEXT_CLIPBOARD_ID.getAndIncrement();
            CLIPBOARD_PEERS.put(id, peer);
            clipboardPeerId = id;
            ownerChangeHandlerId = (long) G_SIGNAL_CONNECT_DATA.invokeExact(getClipboard(),
                    call.allocateFrom("owner-change"), CLIPBOARD_OWNER_CHANGE_CALLBACK, MemorySegment.ofAddress(id),
                    MemorySegment.NULL, 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_dispose} ({@code GlassSystemClipboard.cpp}):
     * disconnects the handler and drops the peer.
     */
    static void clipboardDispose() {
        try {
            G_SIGNAL_HANDLER_DISCONNECT.invokeExact(getClipboard(), ownerChangeHandlerId);
            CLIPBOARD_PEERS.remove(clipboardPeerId);
            ownerChangeHandlerId = 0;
            clipboardPeerId = 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_isOwner} ({@code GlassSystemClipboard.cpp}). */
    static boolean clipboardIsOwner() {
        return isClipboardOwner;
    }

    /**
     * {@code clipboard_owner_changed_callback} ({@code GlassSystemClipboard.cpp}): this process owns the
     * clipboard exactly when the change was the one it made itself, and the peer is told either way.
     */
    static void onClipboardOwnerChanged(MemorySegment gtkClipboard, MemorySegment event, MemorySegment data) {
        isClipboardOwner = isClipboardUpdatedByGlass;
        isClipboardUpdatedByGlass = false;
        try {
            // the C called this through a global reference it never checked; a missing one crashed it
            CLIPBOARD_PEERS.get(data.address()).contentChanged();
        } catch (Throwable t) {
            reportException(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_pushToSystem} ({@code GlassSystemClipboard.cpp}): offers
     * one target per key of {@code data}, or an empty target list when it has none, and keeps {@code data} until
     * GTK clears the clipboard. The {@code supportedActions} argument was unused.
     */
    static void clipboardPush(Map<String, Object> data) {
        try (Arena call = Arena.ofConfined()) {
            long id = NEXT_CLIPBOARD_ID.getAndIncrement();
            CLIPBOARD_DATA.put(id, data);
            initAtoms();
            MemorySegment count = call.allocate(JAVA_INT);
            MemorySegment targets = dataToTargets(call, data, count);
            if (targets.address() != 0) {
                int ntargets = count.get(JAVA_INT, 0);
                int set = (int) GTK_CLIPBOARD_SET_WITH_DATA.invokeExact(getClipboard(), targets, ntargets,
                        CLIPBOARD_GET_CALLBACK, CLIPBOARD_CLEAR_CALLBACK, MemorySegment.ofAddress(id));
                GTK_TARGET_TABLE_FREE.invokeExact(targets, ntargets);
            } else {
                // targets == NULL means that we want to clear the clipboard: passing NULL would make
                // gtk_clipboard_set_with_data print a Gtk-CRITICAL assertion, passing 0 as n_targets does not
                MemorySegment dummy = call.allocate(GTK_TARGET_ENTRY_SIZE);
                dummy.set(ADDRESS, 0, call.allocateFrom("MIME_DUMMY_TARGET"));
                dummy.set(JAVA_INT, 8, 0);
                dummy.set(JAVA_INT, 12, 0);
                int set = (int) GTK_CLIPBOARD_SET_WITH_DATA.invokeExact(getClipboard(), dummy, 0,
                        CLIPBOARD_GET_CALLBACK, CLIPBOARD_CLEAR_CALLBACK, MemorySegment.ofAddress(id));
            }
            isClipboardUpdatedByGlass = true;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code data_to_targets} and {@code add_target_from_jstring} ({@code GlassSystemClipboard.cpp}): a target
     * table built from the keys of the map, where {@code text/plain} and {@code application/x-java-rawimage} stand
     * for every text and image target GTK knows, {@code application/x-java-file-list} for {@code text/uri-list},
     * and anything else for the atom of its own name. The table is GTK's; the caller frees it.
     */
    private static MemorySegment dataToTargets(Arena call, Map<String, Object> data, MemorySegment count)
            throws Throwable {
        MemorySegment list = (MemorySegment) GTK_TARGET_LIST_NEW.invokeExact(MemorySegment.NULL, 0);
        for (String key : data.keySet()) {
            byte[] mime = getUtf(key);
            if (cStringEquals(mime, "text/plain")) {
                GTK_TARGET_LIST_ADD_TEXT_TARGETS.invokeExact(list, 0);
            } else if (cStringEquals(mime, "application/x-java-rawimage")) {
                GTK_TARGET_LIST_ADD_IMAGE_TARGETS.invokeExact(list, 0, 1);
            } else if (cStringEquals(mime, "application/x-java-file-list")) {
                GTK_TARGET_LIST_ADD.invokeExact(list, mimeTextUriListTarget, 0, 0);
            } else {
                long atom = (long) GDK_ATOM_INTERN.invokeExact(call.allocateFrom(JAVA_BYTE, mime), 0);
                GTK_TARGET_LIST_ADD.invokeExact(list, atom, 0, 0);
            }
        }
        MemorySegment targets = (MemorySegment) GTK_TARGET_TABLE_NEW_FROM_LIST.invokeExact(list, count);
        GTK_TARGET_LIST_UNREF.invokeExact(list);
        return targets;
    }

    /**
     * {@code set_data_func} and {@code set_data} ({@code GlassSystemClipboard.cpp}): what this process serves when
     * another client asks for one of the targets it offered. Nothing may leave this method: it runs in an upcall
     * stub, and every step of the C reported and cleared its exceptions.
     */
    static void onClipboardGetData(MemorySegment gtkClipboard, MemorySegment selectionData, int info,
                                   MemorySegment user) {
        try (Arena call = Arena.ofConfined()) {
            Map<String, Object> data = CLIPBOARD_DATA.get(user.address());
            long target = (long) GTK_SELECTION_DATA_GET_TARGET.invokeExact(selectionData);
            MemorySegment one = call.allocate(JAVA_LONG);
            one.set(JAVA_LONG, 0, target);
            MemorySegment name = (MemorySegment) GDK_ATOM_NAME.invokeExact(target);
            try {
                if ((int) GTK_TARGETS_INCLUDE_TEXT.invokeExact(one, 1) != 0) {
                    Object result = data.get("text/plain");
                    if (result instanceof String text) {
                        setTextData(call, selectionData, text);
                    }
                } else if ((int) GTK_TARGETS_INCLUDE_IMAGE.invokeExact(one, 1, 1) != 0) {
                    Object result = data.get("application/x-java-rawimage");
                    if (result instanceof GtkPixels pixels) {
                        setImageData(call, selectionData, pixels);
                    }
                } else if (target == mimeTextUriListTarget) {
                    setUriData(call, selectionData, data);
                } else {
                    // NewStringUTF(NULL) answered NULL, and the C looked that key up as it stood
                    Object result = data.get(newStringUtf(name));
                    if (result instanceof String text) {
                        setStringData(call, selectionData, target, text);
                    } else if (result instanceof ByteBuffer buffer) {
                        setByteBufferData(call, selectionData, target, buffer);
                    }
                }
            } finally {
                // the C's set_data reached its g_free(name) whatever the helpers left behind
                G_FREE_CALL.invokeExact(name);
            }
        } catch (Throwable t) {
            reportException(t);
        }
    }

    /** {@code set_text_data}: the UTF-8 bytes up to their first NUL. */
    private static void setTextData(Arena call, MemorySegment selectionData, String text) throws Throwable {
        byte[] bytes = getUtf(text);
        int set = (int) GTK_SELECTION_DATA_SET_TEXT.invokeExact(selectionData, call.allocateFrom(JAVA_BYTE, bytes),
                cStringLength(bytes));
    }

    /** {@code set_jstring_data}: the same bytes, as the requested target with an 8-bit format. */
    private static void setStringData(Arena call, MemorySegment selectionData, long target, String text)
            throws Throwable {
        byte[] bytes = getUtf(text);
        GTK_SELECTION_DATA_SET.invokeExact(selectionData, target, 8, call.allocateFrom(JAVA_BYTE, bytes),
                cStringLength(bytes));
    }

    /** {@code set_bytebuffer_data}: the whole backing array, whatever the buffer's position and limit. */
    private static void setByteBufferData(Arena call, MemorySegment selectionData, long target, ByteBuffer buffer)
            throws Throwable {
        byte[] raw = buffer.array();
        GTK_SELECTION_DATA_SET.invokeExact(selectionData, target, 8, call.allocateFrom(JAVA_BYTE, raw), raw.length);
    }

    /** {@code set_image_data}: the {@code GdkPixbuf} the pixels attach to, unreferenced afterwards either way. */
    private static void setImageData(Arena call, MemorySegment selectionData, Pixels pixels) throws Throwable {
        MemorySegment slot = call.allocate(ADDRESS);
        boolean threw = false;
        try {
            PIXELS_ATTACH_DATA.invokeExact(pixels, slot.address());
        } catch (Throwable t) {
            reportException(t);
            threw = true;
        }
        MemorySegment pixbuf = slot.get(ADDRESS, 0);
        if (!threw) {
            int set = (int) GTK_SELECTION_DATA_SET_PIXBUF.invokeExact(selectionData, pixbuf);
        }
        G_OBJECT_UNREF.invokeExact(pixbuf);
    }

    /**
     * {@code set_uri_data} ({@code GlassSystemClipboard.cpp}): the file list as {@code file://} URIs followed by
     * the URL, in one NUL-terminated array. A file that is not a {@code String} leaves a hole in that array, which
     * GTK reads as its end.
     */
    private static void setUriData(Arena call, MemorySegment selectionData, Map<String, Object> data)
            throws Throwable {
        byte[] url = null;
        if (data.containsKey("text/uri-list")) {
            Object value = data.get("text/uri-list");
            if (value instanceof String text) {
                url = getUtf(text);
            }
        }
        Object[] files = null;
        int filesCount = 0;
        if (data.containsKey("application/x-java-file-list")) {
            Object value = data.get("application/x-java-file-list");
            if (value != null) {
                files = (Object[]) value;
                filesCount = files.length;
            }
        }
        if (url == null && filesCount == 0) {
            return;
        }
        int uriCount = filesCount + (url != null ? 1 : 0);
        // uris must be a NULL-terminated array of strings
        MemorySegment uris = call.allocate(ADDRESS, uriCount + 1);
        int i = 0;
        if (filesCount > 0) {
            for (; i < filesCount; i++) {
                if (files[i] instanceof String file) {
                    MemorySegment path = call.allocateFrom(JAVA_BYTE, getUtf(file));
                    MemorySegment uri = (MemorySegment) G_FILENAME_TO_URI.invokeExact(path, MemorySegment.NULL,
                            MemorySegment.NULL);
                    uris.setAtIndex(ADDRESS, i, uri);
                }
            }
        }
        if (url != null) {
            uris.setAtIndex(ADDRESS, i, call.allocateFrom(JAVA_BYTE, url));
        }
        // http://www.ietf.org/rfc/rfc2483.txt
        int set = (int) GTK_SELECTION_DATA_SET_URIS.invokeExact(selectionData, uris);
        for (int j = 0; j < filesCount; j++) {
            G_FREE_CALL.invokeExact(uris.getAtIndex(ADDRESS, j));
        }
    }

    /** {@code clear_data_func} ({@code GlassSystemClipboard.cpp}): GTK owns the clipboard no more. */
    static void onClipboardClearData(MemorySegment gtkClipboard, MemorySegment user) {
        try {
            CLIPBOARD_DATA.remove(user.address());
        } catch (Throwable t) {
            reportException(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_popFromSystem} ({@code GlassSystemClipboard.cpp}): what
     * the clipboard's owner serves for {@code mime}, decoded by the mime's own rule. The comparisons are made on
     * the modified UTF-8 bytes {@code GetStringUTFChars} produced. Every one of these waits iterates the GTK main
     * loop, so events are dispatched inside this call.
     */
    static Object clipboardPop(String mime) {
        try (Arena call = Arena.ofConfined()) {
            byte[] cmime = JniStringCodec.toModifiedUtf8(mime);
            initAtoms();
            Object result;
            if (cStringEquals(cmime, "text/plain")) {
                result = clipboardText();
            } else if (cStringEquals(cmime, "text/uri-list")) {
                result = urisToJava(clipboardUris(), false);
            } else if (cStringStartsWith(cmime, "text/")) {
                result = clipboardRaw(call, cmime, true);
            } else if (cStringEquals(cmime, "application/x-java-file-list")) {
                result = urisToJava(clipboardUris(), true);
            } else if (cStringEquals(cmime, "application/x-java-rawimage")) {
                result = clipboardImage();
            } else {
                result = clipboardRaw(call, cmime, false);
            }
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code get_data_text}: {@code gtk_clipboard_wait_for_text} decoded as {@code createUTF} decoded it. */
    private static Object clipboardText() throws Throwable {
        MemorySegment data = (MemorySegment) GTK_CLIPBOARD_WAIT_FOR_TEXT.invokeExact(getClipboard());
        if (data.address() == 0) {
            return null;
        }
        try {
            return new String(cString(data), StandardCharsets.UTF_8);
        } finally {
            G_FREE_CALL.invokeExact(data);
        }
    }

    /** {@code gtk_clipboard_wait_for_uris}, whose result {@link #urisToJava} frees. */
    private static MemorySegment clipboardUris() throws Throwable {
        return (MemorySegment) GTK_CLIPBOARD_WAIT_FOR_URIS.invokeExact(getClipboard());
    }

    /**
     * {@code uris_to_java} ({@code glass_general.cpp}), which only the clipboard called with an array GTK owns:
     * with {@code files}, the {@code file://} URIs as filesystem paths in a {@code String[]}; without, the other
     * URIs joined by CR LF in one {@code String}, comment lines left out. The array is freed either way.
     * <p>
     * The n-th file URI is stored at index n, which is what the array is sized for. Commit {@code 033187ad90}
     * indexed it by the position in the whole URI list instead, so a file URI that followed a non-file one was
     * written past the end of the array: the {@code ArrayIndexOutOfBoundsException} was reported and the path
     * lost. The DnD target copy in {@code glass_dnd.cpp} was corrected with it.
     */
    private static Object urisToJava(MemorySegment uris, boolean files) throws Throwable {
        if (uris.address() == 0) {
            return null;
        }
        try {
            int size = (int) G_STRV_LENGTH.invokeExact(uris);
            MemorySegment list = uris.reinterpret(ADDRESS.byteSize() * ((long) size + 1));
            int filesCount = 0;
            for (int i = 0; i < size; i++) {
                if (cStringStartsWith(cString(list.getAtIndex(ADDRESS, i)), FILE_PREFIX)) {
                    filesCount++;
                }
            }
            Object result = null;
            if (files) {
                if (filesCount != 0) {
                    String[] paths = new String[filesCount];
                    int fileIndex = 0;
                    for (int i = 0; i < size; i++) {
                        MemorySegment uri = list.getAtIndex(ADDRESS, i);
                        if (cStringStartsWith(cString(uri), FILE_PREFIX)) {
                            MemorySegment path = (MemorySegment) G_FILENAME_FROM_URI.invokeExact(uri,
                                    MemorySegment.NULL, MemorySegment.NULL);
                            paths[fileIndex++] = newStringUtf(path);
                            G_FREE_CALL.invokeExact(path);
                        }
                    }
                    result = paths;
                }
            } else if (size - filesCount != 0) {
                ByteArrayOutputStream text = new ByteArrayOutputStream();
                for (int i = 0; i < size; i++) {
                    byte[] uri = cString(list.getAtIndex(ADDRESS, i));
                    if (!cStringStartsWith(uri, FILE_PREFIX) && !cStringStartsWith(uri, URI_LIST_COMMENT_PREFIX)) {
                        text.writeBytes(uri);
                        text.writeBytes(URI_LIST_LINE_BREAK.getBytes(StandardCharsets.US_ASCII));
                    }
                }
                byte[] joined = text.toByteArray();
                if (joined.length > 2) {
                    joined = Arrays.copyOf(joined, joined.length - 2);
                }
                result = JniStringCodec.fromNewStringUtf(joined);
            }
            return result;
        } finally {
            G_STRFREEV.invokeExact(uris);
        }
    }

    /**
     * {@code get_data_image} ({@code GlassSystemClipboard.cpp}): the clipboard image with an alpha channel, its
     * rows permuted into the Glass byte order, as a {@code GtkPixels} over a heap {@code ByteBuffer} of
     * {@code rowstride * height} bytes.
     */
    private static Object clipboardImage() throws Throwable {
        MemorySegment pixbuf = (MemorySegment) GTK_CLIPBOARD_WAIT_FOR_IMAGE.invokeExact(getClipboard());
        if (pixbuf.address() == 0) {
            return null;
        }
        if ((int) GDK_PIXBUF_GET_HAS_ALPHA.invokeExact(pixbuf) == 0) {
            MemorySegment withAlpha = (MemorySegment) GDK_PIXBUF_ADD_ALPHA.invokeExact(pixbuf, 0, (byte) 0,
                    (byte) 0, (byte) 0);
            G_OBJECT_UNREF.invokeExact(pixbuf);
            pixbuf = withAlpha;
        }
        int w = (int) GDK_PIXBUF_GET_WIDTH.invokeExact(pixbuf);
        int h = (int) GDK_PIXBUF_GET_HEIGHT.invokeExact(pixbuf);
        int stride = (int) GDK_PIXBUF_GET_ROWSTRIDE.invokeExact(pixbuf);
        if (stride <= 0 || h <= 0 || h > Integer.MAX_VALUE / stride) {
            G_OBJECT_UNREF.invokeExact(pixbuf);
            return null;
        }
        MemorySegment pixels = (MemorySegment) GDK_PIXBUF_GET_PIXELS.invokeExact(pixbuf);
        // the comment of the C calls this RGBA -> BGRA; it is the same permutation
        MemorySegment data = convertBgraToRgba(pixels.reinterpret((long) stride * h), 0, stride, h);
        if (data.address() == 0) {
            G_OBJECT_UNREF.invokeExact(pixbuf);
            return null;
        }
        byte[] bytes = data.toArray(JAVA_BYTE);
        G_FREE_CALL.invokeExact(data);
        G_OBJECT_UNREF.invokeExact(pixbuf);
        return new GtkPixels(w, h, ByteBuffer.wrap(bytes));
    }

    /**
     * {@code get_data_raw} ({@code GlassSystemClipboard.cpp}): the selection for the atom of {@code mime}, either
     * decoded as {@code createUTF} decoded it - to its first NUL - or wrapped whole in a {@code ByteBuffer}.
     */
    private static Object clipboardRaw(Arena call, byte[] mime, boolean stringData) throws Throwable {
        long atom = (long) GDK_ATOM_INTERN.invokeExact(call.allocateFrom(JAVA_BYTE, mime), 0);
        MemorySegment data = (MemorySegment) GTK_CLIPBOARD_WAIT_FOR_CONTENTS.invokeExact(getClipboard(), atom);
        if (data.address() == 0) {
            return null;
        }
        try {
            // glass_gtk_selection_data_get_data_with_length (glass_general.cpp)
            int length = (int) GTK_SELECTION_DATA_GET_LENGTH.invokeExact(data);
            MemorySegment raw = (MemorySegment) GTK_SELECTION_DATA_GET_DATA.invokeExact(data);
            if (stringData) {
                return new String(cString(raw), StandardCharsets.UTF_8);
            }
            return ByteBuffer.wrap(raw.reinterpret(length).toArray(JAVA_BYTE));
        } finally {
            GTK_SELECTION_DATA_FREE.invokeExact(data);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_mimesFromSystem} ({@code GlassSystemClipboard.cpp}): the
     * targets the clipboard offers, with the first text target reported as {@code text/plain}, the first image
     * target as {@code application/x-java-rawimage} and {@code text/uri-list} split into a file list and a URI
     * list according to what the URIs are. An empty clipboard answers {@code null}: the C's
     * {@code glass_try_malloc0_n(0, ...)} was {@code g_try_malloc0(0)}, which is {@code NULL}.
     */
    static String[] clipboardMimes() {
        try (Arena call = Arena.ofConfined()) {
            initAtoms();
            MemorySegment targetsOut = call.allocate(ADDRESS);
            MemorySegment countOut = call.allocate(JAVA_INT);
            int found = (int) GTK_CLIPBOARD_WAIT_FOR_TARGETS.invokeExact(getClipboard(), targetsOut, countOut);
            MemorySegment targets = targetsOut.get(ADDRESS, 0);
            int ntargets = countOut.get(JAVA_INT, 0);
            try {
                return clipboardMimes(call, targets, ntargets);
            } finally {
                // the C freed the target array on every path out of mimesFromSystem
                G_FREE_CALL.invokeExact(targets);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The target-to-mime walk of {@code mimesFromSystem}, over the {@code ntargets} atoms of {@code targets}. */
    @SuppressWarnings("restricted")
    private static String[] clipboardMimes(Arena call, MemorySegment targets, int ntargets) throws Throwable {
        if (ntargets == 0) {
            return null;
        }
        MemorySegment atoms = targets.reinterpret((long) ntargets * JAVA_LONG.byteSize());
        long[] convertible = new long[ntargets * 2];
        int count = 0;
        boolean uriListAdded = false;
        boolean textAdded = false;
        boolean imageAdded = false;
        MemorySegment one = call.allocate(JAVA_LONG);
        for (int i = 0; i < ntargets; i++) {
            long target = atoms.getAtIndex(JAVA_LONG, i);
            one.set(JAVA_LONG, 0, target);
            if ((int) GTK_TARGETS_INCLUDE_TEXT.invokeExact(one, 1) != 0 && !textAdded) {
                convertible[count++] = mimeTextPlainTarget;
                textAdded = true;
            } else if ((int) GTK_TARGETS_INCLUDE_IMAGE.invokeExact(one, 1, 1) != 0 && !imageAdded) {
                convertible[count++] = mimeJavaImage;
                imageAdded = true;
            }
            if (target == mimeTextUriListTarget) {
                if (uriListAdded) {
                    continue;
                }
                MemorySegment uris = clipboardUris();
                if (uris.address() != 0) {
                    try {
                        int size = (int) G_STRV_LENGTH.invokeExact(uris);
                        MemorySegment list = uris.reinterpret(ADDRESS.byteSize() * ((long) size + 1));
                        int filesCount = 0;
                        for (int j = 0; j < size; j++) {
                            if (cStringStartsWith(cString(list.getAtIndex(ADDRESS, j)), FILE_PREFIX)) {
                                filesCount++;
                            }
                        }
                        if (filesCount != 0) {
                            convertible[count++] = mimeFilesTarget;
                        }
                        if (size - filesCount != 0) {
                            convertible[count++] = mimeTextUriListTarget;
                        }
                    } finally {
                        G_STRFREEV.invokeExact(uris);
                    }
                }
                uriListAdded = true;
            } else {
                convertible[count++] = target;
            }
        }
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            MemorySegment name = (MemorySegment) GDK_ATOM_NAME.invokeExact(convertible[i]);
            try {
                result[i] = newStringUtf(name);
            } finally {
                G_FREE_CALL.invokeExact(name);
            }
        }
        return result;
    }

    /**
     * {@code getUTF} ({@code GlassSystemClipboard.cpp}): {@code String.getBytes("UTF-8")} in a buffer one byte
     * longer, NUL-terminated. Standard UTF-8, so a lone surrogate becomes {@code ?} and an embedded U+0000 stays a
     * NUL byte inside the buffer - which every C reader of it stops at.
     */
    private static byte[] getUtf(String text) {
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        return Arrays.copyOf(utf8, utf8.length + 1);
    }

    /**
     * {@code strlen} of a byte buffer: the bytes before its first NUL. {@link #getUtf} keeps the terminator,
     * {@link #cStringBytes} has already cut it off, so the end of the array ends the string too.
     */
    private static int cStringLength(byte[] text) {
        int length = 0;
        while (length < text.length && text[length] != 0) {
            length++;
        }
        return length;
    }

    /** {@code g_strcmp0(text, literal) == 0} for an ASCII literal. */
    private static boolean cStringEquals(byte[] text, String literal) {
        int length = cStringLength(text);
        if (length != literal.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if ((text[i] & 0xFF) != literal.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /** {@code g_str_has_prefix(text, prefix)} for an ASCII prefix. */
    private static boolean cStringStartsWith(byte[] text, String prefix) {
        if (cStringLength(text) < prefix.length()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if ((text[i] & 0xFF) != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Common dialogs: GtkCommonDialogs._showFileChooser, _showFolderChooser (GlassCommonDialogs.cpp)
     * ------------------------------------------------------------------------------------------------------- */

    /** {@code GTK_FILE_CHOOSER_ACTION_OPEN}, {@code _SAVE} and {@code _SELECT_FOLDER} of {@code gtkfilechooser.h}. */
    private static final int GTK_FILE_CHOOSER_ACTION_OPEN = 0;

    private static final int GTK_FILE_CHOOSER_ACTION_SAVE = 1;

    private static final int GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER = 2;

    /** {@code GTK_RESPONSE_ACCEPT} of {@code gtkdialog.h}. */
    private static final int GTK_RESPONSE_ACCEPT = -3;

    /** {@code GSList}: {@code gpointer data; GSList *next;}. */
    private static final long GSLIST_SIZE = 16;

    /**
     * {@code GtkFileChooserNative *gtk_file_chooser_native_new(const gchar *title, GtkWindow *parent,
     * GtkFileChooserAction action, const gchar *accept_label, const gchar *cancel_label)}.
     */
    private static final MethodHandle GTK_FILE_CHOOSER_NATIVE_NEW = bind(GTK, LIB_GTK,
            "gtk_file_chooser_native_new", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS,
                    ADDRESS));

    /** {@code void gtk_file_chooser_set_current_name(GtkFileChooser *, const gchar *name)}. */
    private static final MethodHandle GTK_FILE_CHOOSER_SET_CURRENT_NAME = bind(GTK, LIB_GTK,
            "gtk_file_chooser_set_current_name", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /**
     * {@code void gtk_file_chooser_set_do_overwrite_confirmation(GtkFileChooser *, gboolean)}.
     */
    private static final MethodHandle GTK_FILE_CHOOSER_SET_DO_OVERWRITE_CONFIRMATION = bind(GTK, LIB_GTK,
            "gtk_file_chooser_set_do_overwrite_confirmation", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /** {@code void gtk_file_chooser_set_select_multiple(GtkFileChooser *, gboolean)}. */
    private static final MethodHandle GTK_FILE_CHOOSER_SET_SELECT_MULTIPLE = bind(GTK, LIB_GTK,
            "gtk_file_chooser_set_select_multiple", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /** {@code gboolean gtk_file_chooser_set_current_folder(GtkFileChooser *, const gchar *filename)}. */
    private static final MethodHandle GTK_FILE_CHOOSER_SET_CURRENT_FOLDER = bind(GTK, LIB_GTK,
            "gtk_file_chooser_set_current_folder", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code void gtk_file_chooser_add_filter(GtkFileChooser *, GtkFileFilter *)}. */
    private static final MethodHandle GTK_FILE_CHOOSER_ADD_FILTER = bind(GTK, LIB_GTK,
            "gtk_file_chooser_add_filter", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /** {@code void gtk_file_chooser_set_filter(GtkFileChooser *, GtkFileFilter *)}. */
    private static final MethodHandle GTK_FILE_CHOOSER_SET_FILTER = bind(GTK, LIB_GTK,
            "gtk_file_chooser_set_filter", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /** {@code GtkFileFilter *gtk_file_chooser_get_filter(GtkFileChooser *)}. */
    private static final MethodHandle GTK_FILE_CHOOSER_GET_FILTER = bind(GTK, LIB_GTK,
            "gtk_file_chooser_get_filter", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code GSList *gtk_file_chooser_get_filenames(GtkFileChooser *)}. */
    private static final MethodHandle GTK_FILE_CHOOSER_GET_FILENAMES = bind(GTK, LIB_GTK,
            "gtk_file_chooser_get_filenames", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code gchar *gtk_file_chooser_get_filename(GtkFileChooser *)}. */
    private static final MethodHandle GTK_FILE_CHOOSER_GET_FILENAME = bind(GTK, LIB_GTK,
            "gtk_file_chooser_get_filename", FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code GtkFileFilter *gtk_file_filter_new(void)}. */
    private static final MethodHandle GTK_FILE_FILTER_NEW = bind(GTK, LIB_GTK, "gtk_file_filter_new",
            FunctionDescriptor.of(ADDRESS));

    /** {@code void gtk_file_filter_set_name(GtkFileFilter *, const gchar *name)}. */
    private static final MethodHandle GTK_FILE_FILTER_SET_NAME = bind(GTK, LIB_GTK, "gtk_file_filter_set_name",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /** {@code void gtk_file_filter_add_pattern(GtkFileFilter *, const gchar *pattern)}. */
    private static final MethodHandle GTK_FILE_FILTER_ADD_PATTERN = bind(GTK, LIB_GTK,
            "gtk_file_filter_add_pattern", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /** {@code gint gtk_native_dialog_run(GtkNativeDialog *)}: shows the dialog and runs a nested main loop. */
    private static final MethodHandle GTK_NATIVE_DIALOG_RUN = bind(GTK, LIB_GTK, "gtk_native_dialog_run",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code guint g_slist_length(GSList *)}. */
    private static final MethodHandle G_SLIST_LENGTH = bind(GLIB, LIB_GLIB, "g_slist_length",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code GSList *g_slist_nth(GSList *, guint n)}. */
    private static final MethodHandle G_SLIST_NTH = bind(GLIB, LIB_GLIB, "g_slist_nth",
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));

    /** {@code GSList *g_slist_append(GSList *, gpointer data)}. */
    private static final MethodHandle G_SLIST_APPEND = bind(GLIB, LIB_GLIB, "g_slist_append",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    /** {@code gint g_slist_index(GSList *, gconstpointer data)}. */
    private static final MethodHandle G_SLIST_INDEX = bind(GLIB, LIB_GLIB, "g_slist_index",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code void g_slist_free(GSList *)}. */
    private static final MethodHandle G_SLIST_FREE = bind(GLIB, LIB_GLIB, "g_slist_free",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code GdkWindow *gdk_x11_window_lookup_for_display(GdkDisplay *, Window)}. */
    private static final MethodHandle GDK_X11_WINDOW_LOOKUP_FOR_DISPLAY = bind(GDK, LIB_GDK,
            "gdk_x11_window_lookup_for_display", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));

    /** {@code void gdk_window_get_user_data(GdkWindow *, gpointer *data)}. */
    private static final MethodHandle GDK_WINDOW_GET_USER_DATA = bind(GDK, LIB_GDK, "gdk_window_get_user_data",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /** {@code GtkWidget *gtk_widget_get_toplevel(GtkWidget *)}. */
    private static final MethodHandle GTK_WIDGET_GET_TOPLEVEL = bind(GTK, LIB_GTK, "gtk_widget_get_toplevel",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    /**
     * {@code CommonDialogs.ExtensionFilter.extensionsToArray}, which is private and documented as "called from
     * native" - JNI never saw access control. Reached the same way {@link #PIXELS_ATTACH_DATA} is.
     */
    private static final MethodHandle EXTENSIONS_TO_ARRAY = extensionsToArray();

    private static MethodHandle extensionsToArray() {
        try {
            return MethodHandles.privateLookupIn(CommonDialogs.ExtensionFilter.class, MethodHandles.lookup())
                    .findVirtual(CommonDialogs.ExtensionFilter.class, "extensionsToArray",
                            MethodType.methodType(String[].class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkCommonDialogs__1showFileChooser} ({@code GlassCommonDialogs.cpp}): a
     * {@code GtkFileChooserNative} run in a nested main loop, and the files it was left with.
     * <p>
     * All four strings enter GTK as the modified UTF-8 that {@code GetStringUTFChars} produced. The chosen file
     * names come back as {@code new String(bytes)} with the default charset, which is what the C built them with,
     * and the selected filter as the index of the {@code GtkFileFilter} in the list that was added.
     *
     * @param parent the X11 window id of the owner, or 0
     */
    static CommonDialogs.FileChooserResult showFileChooser(long parent, String folder, String name, String title,
                                                           int type, boolean multiple,
                                                           CommonDialogs.ExtensionFilter[] filters,
                                                           int defaultFilterIndex) {
        try (Arena call = Arena.ofConfined()) {
            int action = type == 0 ? GTK_FILE_CHOOSER_ACTION_OPEN : GTK_FILE_CHOOSER_ACTION_SAVE;
            MemorySegment chooser = (MemorySegment) GTK_FILE_CHOOSER_NATIVE_NEW.invokeExact(
                    modifiedUtf8(call, title), parentWindow(parent), action, MemorySegment.NULL,
                    MemorySegment.NULL);
            MemorySegment filterList = MemorySegment.NULL;
            try {
                if (action == GTK_FILE_CHOOSER_ACTION_SAVE) {
                    GTK_FILE_CHOOSER_SET_CURRENT_NAME.invokeExact(chooser, modifiedUtf8(call, name));
                    GTK_FILE_CHOOSER_SET_DO_OVERWRITE_CONFIRMATION.invokeExact(chooser, 1);
                }
                GTK_FILE_CHOOSER_SET_SELECT_MULTIPLE.invokeExact(chooser, multiple ? 1 : 0);
                int folderSet = (int) GTK_FILE_CHOOSER_SET_CURRENT_FOLDER.invokeExact(chooser,
                        modifiedUtf8(call, folder));
                filterList = setupFileFilters(call, chooser, filters, defaultFilterIndex);

                String[] fileNames = null;
                if ((int) GTK_NATIVE_DIALOG_RUN.invokeExact(chooser) == GTK_RESPONSE_ACCEPT) {
                    MemorySegment names = (MemorySegment) GTK_FILE_CHOOSER_GET_FILENAMES.invokeExact(chooser);
                    int count = (int) G_SLIST_LENGTH.invokeExact(names);
                    if (count > 0) {
                        fileNames = new String[count];
                        for (int i = 0; i < count; i++) {
                            MemorySegment node = (MemorySegment) G_SLIST_NTH.invokeExact(names, i);
                            MemorySegment file = node.reinterpret(GSLIST_SIZE).get(ADDRESS, 0);
                            fileNames[i] = new String(cString(file), Charset.defaultCharset());
                        }
                        for (int i = 0; i < count; i++) {
                            MemorySegment node = (MemorySegment) G_SLIST_NTH.invokeExact(names, i);
                            G_FREE_CALL.invokeExact(node.reinterpret(GSLIST_SIZE).get(ADDRESS, 0));
                        }
                        G_SLIST_FREE.invokeExact(names);
                    }
                }
                if (fileNames == null) {
                    fileNames = new String[0];
                }
                MemorySegment selected = (MemorySegment) GTK_FILE_CHOOSER_GET_FILTER.invokeExact(chooser);
                int index = (int) G_SLIST_INDEX.invokeExact(filterList, selected);
                try {
                    return CommonDialogs.createFileChooserResult(fileNames, filters, index);
                } catch (Throwable t) {
                    // LOG_EXCEPTION: the C reported it and returned the NULL that CallStaticObjectMethod left
                    reportException(t);
                    return null;
                }
            } finally {
                // freed on every path out, as the C freed them: gtk_native_dialog_run dispatches events, whose calls
                // into Java go through the upcall stubs
                G_SLIST_FREE.invokeExact(filterList);
                G_OBJECT_UNREF.invokeExact(chooser);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkCommonDialogs__1showFolderChooser} ({@code GlassCommonDialogs.cpp}): a
     * folder-selecting {@code GtkFileChooserNative}, and the folder it was left with, decoded as
     * {@code NewStringUTF} decoded it - which is not how {@code _showFileChooser} decoded its file names.
     *
     * @param parent the X11 window id of the owner, or 0
     */
    static String showFolderChooser(long parent, String folder, String title) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment chooser = (MemorySegment) GTK_FILE_CHOOSER_NATIVE_NEW.invokeExact(
                    modifiedUtf8(call, title), parentWindow(parent), GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER,
                    MemorySegment.NULL, MemorySegment.NULL);
            try {
                if (folder != null) {
                    int set = (int) GTK_FILE_CHOOSER_SET_CURRENT_FOLDER.invokeExact(chooser,
                            modifiedUtf8(call, folder));
                }
                String result = null;
                if ((int) GTK_NATIVE_DIALOG_RUN.invokeExact(chooser) == GTK_RESPONSE_ACCEPT) {
                    MemorySegment name = (MemorySegment) GTK_FILE_CHOOSER_GET_FILENAME.invokeExact(chooser);
                    try {
                        result = name.address() == 0 ? null : JniStringCodec.fromNewStringUtf(cString(name));
                    } finally {
                        G_FREE_CALL.invokeExact(name);
                    }
                }
                return result;
            } finally {
                // unreferenced on every path out, as the C unreferenced it
                G_OBJECT_UNREF.invokeExact(chooser);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code setup_GtkFileFilters} ({@code GlassCommonDialogs.cpp}): one {@code GtkFileFilter} per
     * {@code ExtensionFilter}, named by its description and matching its patterns, added to the chooser in order
     * and returned as the {@code GSList} whose index {@code showFileChooser} answers. An empty array adds
     * nothing and answers {@code NULL}, as the C did.
     */
    private static MemorySegment setupFileFilters(Arena call, MemorySegment chooser,
                                                  CommonDialogs.ExtensionFilter[] filters, int defaultFilterIndex)
            throws Throwable {
        if (filters.length == 0) {
            return MemorySegment.NULL;
        }
        MemorySegment list = MemorySegment.NULL;
        for (int i = 0; i < filters.length; i++) {
            MemorySegment filter = (MemorySegment) GTK_FILE_FILTER_NEW.invokeExact();
            GTK_FILE_FILTER_SET_NAME.invokeExact(filter, modifiedUtf8(call, filters[i].getDescription()));
            for (String extension : (String[]) EXTENSIONS_TO_ARRAY.invokeExact(filters[i])) {
                GTK_FILE_FILTER_ADD_PATTERN.invokeExact(filter, modifiedUtf8(call, extension));
            }
            GTK_FILE_CHOOSER_ADD_FILTER.invokeExact(chooser, filter);
            if (defaultFilterIndex == i) {
                GTK_FILE_CHOOSER_SET_FILTER.invokeExact(chooser, filter);
            }
            list = (MemorySegment) G_SLIST_APPEND.invokeExact(list, filter);
        }
        return list;
    }

    /**
     * The {@code GtkWindow} of the owner, which the C read from its {@code WindowContext} - here reached from the
     * X11 window id through GDK. An owner whose {@code GdkWindow} does not exist yet has the id 0 and gives no
     * parent, where the C still had its unrealized {@code GtkWindow}: the dialog is then not transient for it.
     */
    private static MemorySegment parentWindow(long xid) throws Throwable {
        if (xid == 0) {
            return MemorySegment.NULL;
        }
        MemorySegment display = (MemorySegment) GDK_DISPLAY_GET_DEFAULT.invokeExact();
        MemorySegment window = (MemorySegment) GDK_X11_WINDOW_LOOKUP_FOR_DISPLAY.invokeExact(display, xid);
        if (window.address() == 0) {
            return MemorySegment.NULL;
        }
        try (Arena call = Arena.ofConfined()) {
            MemorySegment out = call.allocate(ADDRESS);
            GDK_WINDOW_GET_USER_DATA.invokeExact(window, out);
            MemorySegment widget = out.get(ADDRESS, 0);
            if (widget.address() == 0) {
                return MemorySegment.NULL;
            }
            return (MemorySegment) GTK_WIDGET_GET_TOPLEVEL.invokeExact(widget);
        }
    }

    /**
     * {@code GetStringUTFChars}: the modified UTF-8 bytes of {@code text}, NUL-terminated, or {@code NULL} for a
     * {@code null} string, which is what {@code jstring_to_utf_get} left the pointer at.
     */
    private static MemorySegment modifiedUtf8(Arena call, String text) {
        return text == null ? MemorySegment.NULL : call.allocateFrom(JAVA_BYTE, JniStringCodec.toModifiedUtf8(text));
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Callback tables (glass_gtk_api.h): the calls of the window, view, drag-and-drop and application C of
     * libglassgtk3.so into Java - the Call*Method / NewObject sites of glass_window.cpp, glass_window_ime.cpp,
     * GlassView.cpp, glass_dnd.cpp, glass_screen.cpp and glass_general.cpp at commit 033187ad90, which reached Java
     * through the jmethodIDs glass_general.cpp's JNI_OnLoad cached and mainEnv
     * ------------------------------------------------------------------------------------------------------- */

    /**
     * {@code GLASS_GTK_ABI_VERSION} of {@code glass_gtk_api.h}, the revision this class is written against:
     * {@code ggtk_abi_version} must answer it, or the library is refused ({@link #installCallbacks}).
     */
    static final int ABI_VERSION = 1;

    /** The label of the symbols of {@code libglassgtk3.so} in {@link #boundSymbols}. */
    static final String LIB_GLASS = "libglassgtk3.so";

    /**
     * {@code GGTK_ERR_NOT_IN_DRAG}: no drag has entered a Glass window, where the JNI threw
     * {@link #NOT_IN_DRAG_MESSAGE} as an {@code IllegalStateException} ({@code ggtk_dnd_target_*}).
     */
    static final int ERR_NOT_IN_DRAG = -3;

    /**
     * {@code GGTK_ERR_GTK_VERSION}: GTK is older than the build's minimum, where the JNI threw
     * {@code UnsupportedOperationException} ({@code ggtk_application_init_gtk}).
     */
    static final int ERR_GTK_VERSION = -4;

    /** The message of the {@code IllegalStateException} of {@code check_state_in_drag} ({@code glass_dnd.cpp}). */
    static final String NOT_IN_DRAG_MESSAGE =
            "Cannot get supported actions. Drag pointer haven't entered the application window";

    /** {@code GGTK_UPCALL_OK}: the Java target returned normally. */
    static final int UPCALL_OK = 0;

    /** {@code GGTK_UPCALL_THREW}: the Java target threw, and the exception was reported as the C reported it. */
    static final int UPCALL_THREW = 1;

    /** {@code GGTK_DND_DATA_*}, the {@code kind} of a {@code GgtkDndData}. */
    static final int DND_DATA_NONE = 0;
    static final int DND_DATA_STRING = 1;
    static final int DND_DATA_BYTES = 2;
    static final int DND_DATA_PIXBUF = 3;
    static final int DND_DATA_STRINGS = 4;

    /** {@code GGTK_DND_AS_*}, the conversion {@code source_get_data} applies to the value of the key. */
    static final int DND_AS_STRING = 1;
    static final int DND_AS_BYTES = 2;
    static final int DND_AS_PIXBUF = 3;
    static final int DND_AS_STRINGS = 4;
    static final int DND_AS_RAW = 5;

    /** {@code GgtkAppCallbacks}: two function pointers, in declaration order. */
    static final StructLayout GGTK_APP_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("notify_screen_settings_changed"),
            ADDRESS.withName("get_application_name"));

    /** {@code GgtkWindowCallbacks}: twelve function pointers, in declaration order. */
    static final StructLayout GGTK_WINDOW_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("is_enabled"),
            ADDRESS.withName("notify_state_changed"),
            ADDRESS.withName("notify_focus"),
            ADDRESS.withName("notify_focus_disabled"),
            ADDRESS.withName("notify_focus_ungrab"),
            ADDRESS.withName("notify_destroy"),
            ADDRESS.withName("notify_close"),
            ADDRESS.withName("notify_resize"),
            ADDRESS.withName("notify_move"),
            ADDRESS.withName("notify_move_to_another_screen"),
            ADDRESS.withName("notify_level_changed"),
            ADDRESS.withName("non_client_hit_test"));

    /** {@code GgtkViewCallbacks}: ten function pointers, in declaration order. */
    static final StructLayout GGTK_VIEW_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("notify_view"),
            ADDRESS.withName("notify_resize"),
            ADDRESS.withName("notify_repaint"),
            ADDRESS.withName("notify_mouse"),
            ADDRESS.withName("notify_menu"),
            ADDRESS.withName("notify_scroll"),
            ADDRESS.withName("notify_key"),
            ADDRESS.withName("notify_input_method_preedit"),
            ADDRESS.withName("notify_input_method_commit"),
            ADDRESS.withName("notify_input_method_candidate_pos_request"));

    /** {@code GgtkDndCallbacks}: five function pointers, in declaration order. */
    static final StructLayout GGTK_DND_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("notify_drag_enter"),
            ADDRESS.withName("notify_drag_over"),
            ADDRESS.withName("notify_drag_drop"),
            ADDRESS.withName("notify_drag_leave"),
            ADDRESS.withName("source_get_data"));

    /** {@code GgtkDndData}: {@code int32_t kind, count; void *data, *pixbuf} - 24 bytes on LP64. */
    static final StructLayout GGTK_DND_DATA_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("kind"),
            JAVA_INT.withName("count"),
            ADDRESS.withName("data"),
            ADDRESS.withName("pixbuf"));

    private static final long DND_DATA_KIND_OFFSET = dndDataOffset("kind");
    private static final long DND_DATA_COUNT_OFFSET = dndDataOffset("count");
    private static final long DND_DATA_DATA_OFFSET = dndDataOffset("data");
    private static final long DND_DATA_PIXBUF_OFFSET = dndDataOffset("pixbuf");

    /**
     * {@code GgtkDndTargetValue}: {@code int32_t kind, count; void *data; int32_t width, height, records} and the
     * padding to the pointer's alignment - 32 bytes on LP64, which {@link #installCallbacks} checks against the C.
     */
    static final StructLayout GGTK_DND_TARGET_VALUE_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("kind"),
            JAVA_INT.withName("count"),
            ADDRESS.withName("data"),
            JAVA_INT.withName("width"),
            JAVA_INT.withName("height"),
            JAVA_INT.withName("records"),
            MemoryLayout.paddingLayout(4));

    /** {@code GGTK_DND_VALUE_*}, the {@code kind} of a {@code GgtkDndTargetValue}. */
    static final int DND_VALUE_NONE = 0;
    static final int DND_VALUE_STRING = 1;
    static final int DND_VALUE_BYTES = 2;
    static final int DND_VALUE_IMAGE = 3;
    static final int DND_VALUE_FILES = 4;

    /*
     * The out-parameters of the slots, as pointers whose target is sized: the segment an upcall receives for them
     * covers exactly the C object, so a target writes it without widening the pointer itself.
     */
    private static final AddressLayout INT_OUT = pointerTo(JAVA_INT);
    private static final AddressLayout ADDRESS_OUT = pointerTo(ADDRESS);
    private static final AddressLayout XY_OUT = pointerTo(MemoryLayout.sequenceLayout(2, JAVA_DOUBLE));
    private static final AddressLayout DND_DATA_OUT = pointerTo(GGTK_DND_DATA_LAYOUT);

    /* The slot prototypes of glass_gtk_api.h, parameter by parameter; every slot returns the int32_t status. */
    static final FunctionDescriptor NOTIFY_SCREEN_SETTINGS_CHANGED_FD = FunctionDescriptor.of(JAVA_INT);
    static final FunctionDescriptor GET_APPLICATION_NAME_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS_OUT);

    static final FunctionDescriptor IS_ENABLED_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, INT_OUT);
    static final FunctionDescriptor NOTIFY_STATE_CHANGED_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT);
    static final FunctionDescriptor NOTIFY_FOCUS_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT);
    static final FunctionDescriptor NOTIFY_FOCUS_DISABLED_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG);
    static final FunctionDescriptor NOTIFY_FOCUS_UNGRAB_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG);
    static final FunctionDescriptor NOTIFY_DESTROY_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG);
    static final FunctionDescriptor NOTIFY_CLOSE_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG);
    static final FunctionDescriptor NOTIFY_WINDOW_RESIZE_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT,
            JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_MOVE_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_MOVE_TO_ANOTHER_SCREEN_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG,
            JAVA_INT);
    static final FunctionDescriptor NOTIFY_LEVEL_CHANGED_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT);
    static final FunctionDescriptor NON_CLIENT_HIT_TEST_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT,
            JAVA_INT, INT_OUT);

    static final FunctionDescriptor NOTIFY_VIEW_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT);
    static final FunctionDescriptor NOTIFY_VIEW_RESIZE_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT,
            JAVA_INT);
    static final FunctionDescriptor NOTIFY_REPAINT_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT,
            JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_MOUSE_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_MENU_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT,
            JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_SCROLL_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT,
            JAVA_INT, JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
            JAVA_DOUBLE, JAVA_DOUBLE);
    static final FunctionDescriptor NOTIFY_KEY_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT,
            ADDRESS, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_INPUT_METHOD_PREEDIT_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG,
            ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_INPUT_METHOD_COMMIT_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG,
            ADDRESS, JAVA_INT);
    static final FunctionDescriptor NOTIFY_INPUT_METHOD_CANDIDATE_POS_REQUEST_FD = FunctionDescriptor.of(JAVA_INT,
            JAVA_LONG, JAVA_INT, XY_OUT, INT_OUT);

    static final FunctionDescriptor NOTIFY_DRAG_ENTER_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, INT_OUT);
    static final FunctionDescriptor NOTIFY_DRAG_OVER_FD = NOTIFY_DRAG_ENTER_FD;
    static final FunctionDescriptor NOTIFY_DRAG_DROP_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_DRAG_LEAVE_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG);
    static final FunctionDescriptor SOURCE_GET_DATA_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT,
            DND_DATA_OUT);

    /**
     * One callback table: the struct layout, its slot names in declaration order (the order the tables are written
     * in and {@code ggtk_test_fire_callback} numbers them), the target methods of this class and the descriptors,
     * parallel to the names.
     */
    record CallbackTable(StructLayout layout, List<String> slots, List<String> targets,
                         List<FunctionDescriptor> descriptors) {
    }

    static final CallbackTable APP_TABLE = new CallbackTable(GGTK_APP_CALLBACKS_LAYOUT,
            List.of("notify_screen_settings_changed", "get_application_name"),
            List.of("onNotifyScreenSettingsChanged", "onGetApplicationName"),
            List.of(NOTIFY_SCREEN_SETTINGS_CHANGED_FD, GET_APPLICATION_NAME_FD));

    static final CallbackTable WINDOW_TABLE = new CallbackTable(GGTK_WINDOW_CALLBACKS_LAYOUT,
            List.of("is_enabled", "notify_state_changed", "notify_focus", "notify_focus_disabled",
                    "notify_focus_ungrab", "notify_destroy", "notify_close", "notify_resize", "notify_move",
                    "notify_move_to_another_screen", "notify_level_changed", "non_client_hit_test"),
            List.of("onIsEnabled", "onNotifyStateChanged", "onNotifyFocus", "onNotifyFocusDisabled",
                    "onNotifyFocusUngrab", "onNotifyDestroy", "onNotifyClose", "onNotifyWindowResize",
                    "onNotifyMove", "onNotifyMoveToAnotherScreen", "onNotifyLevelChanged", "onNonClientHitTest"),
            List.of(IS_ENABLED_FD, NOTIFY_STATE_CHANGED_FD, NOTIFY_FOCUS_FD, NOTIFY_FOCUS_DISABLED_FD,
                    NOTIFY_FOCUS_UNGRAB_FD, NOTIFY_DESTROY_FD, NOTIFY_CLOSE_FD, NOTIFY_WINDOW_RESIZE_FD,
                    NOTIFY_MOVE_FD, NOTIFY_MOVE_TO_ANOTHER_SCREEN_FD, NOTIFY_LEVEL_CHANGED_FD,
                    NON_CLIENT_HIT_TEST_FD));

    static final CallbackTable VIEW_TABLE = new CallbackTable(GGTK_VIEW_CALLBACKS_LAYOUT,
            List.of("notify_view", "notify_resize", "notify_repaint", "notify_mouse", "notify_menu", "notify_scroll",
                    "notify_key", "notify_input_method_preedit", "notify_input_method_commit",
                    "notify_input_method_candidate_pos_request"),
            List.of("onNotifyView", "onNotifyViewResize", "onNotifyRepaint", "onNotifyMouse", "onNotifyMenu",
                    "onNotifyScroll", "onNotifyKey", "onNotifyInputMethodPreedit", "onNotifyInputMethodCommit",
                    "onNotifyInputMethodCandidatePosRequest"),
            List.of(NOTIFY_VIEW_FD, NOTIFY_VIEW_RESIZE_FD, NOTIFY_REPAINT_FD, NOTIFY_MOUSE_FD, NOTIFY_MENU_FD,
                    NOTIFY_SCROLL_FD, NOTIFY_KEY_FD, NOTIFY_INPUT_METHOD_PREEDIT_FD, NOTIFY_INPUT_METHOD_COMMIT_FD,
                    NOTIFY_INPUT_METHOD_CANDIDATE_POS_REQUEST_FD));

    static final CallbackTable DND_TABLE = new CallbackTable(GGTK_DND_CALLBACKS_LAYOUT,
            List.of("notify_drag_enter", "notify_drag_over", "notify_drag_drop", "notify_drag_leave",
                    "source_get_data"),
            List.of("onNotifyDragEnter", "onNotifyDragOver", "onNotifyDragDrop", "onNotifyDragLeave",
                    "onSourceGetData"),
            List.of(NOTIFY_DRAG_ENTER_FD, NOTIFY_DRAG_OVER_FD, NOTIFY_DRAG_DROP_FD, NOTIFY_DRAG_LEAVE_FD,
                    SOURCE_GET_DATA_FD));

    /** The four tables in the order {@link #installCallbacks} writes them. */
    static final List<CallbackTable> CALLBACK_TABLES = List.of(APP_TABLE, WINDOW_TABLE, VIEW_TABLE, DND_TABLE);

    /**
     * The production stubs, one list per table of {@link #CALLBACK_TABLES}, created once in {@link Arena#global()}
     * by {@link #installCallbacks} and never replaced: the library may call a stub it has been given at any later
     * point, so every one of them must outlive the process.
     */
    private static List<MemorySegment[]> callbackStubs;

    /**
     * The exports of {@code glass_gtk_api.h} this class calls, in {@code libglassgtk3.so} as the class loader loaded
     * it ({@code NativeLibLoader.loadLibrary("glassgtk3")} in {@code GtkApplication}, or - {@code QUERY_USE_CURRENT}
     * - a {@code glassgtk3} build loaded as the {@code glass} library itself). A holder of its own: the
     * rest of this class binds system libraries only and initializes before, or without, {@code glassgtk3} (a
     * headless test does). {@code ggtk_abi_version} is bound and checked before anything else of the library.
     */
    private static final class Glass {

        private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

        /** {@code int32_t ggtk_abi_version(void)}. */
        static final MethodHandle ABI = bind(LOOKUP, LIB_GLASS, "ggtk_abi_version", FunctionDescriptor.of(JAVA_INT));

        static {
            int actual;
            try {
                actual = (int) ABI.invokeExact();
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (actual != ABI_VERSION) {
                throw new UnsatisfiedLinkError(LIB_GLASS + " ABI version mismatch: expected " + ABI_VERSION
                        + ", found " + actual);
            }
        }

        /** {@code int32_t ggtk_sizeof_*(void)}, in the order of {@link #CALLBACK_TABLES}, then the data. */
        static final MethodHandle SIZEOF_APP = bind(LOOKUP, LIB_GLASS, "ggtk_sizeof_app_callbacks",
                FunctionDescriptor.of(JAVA_INT));
        static final MethodHandle SIZEOF_WINDOW = bind(LOOKUP, LIB_GLASS, "ggtk_sizeof_window_callbacks",
                FunctionDescriptor.of(JAVA_INT));
        static final MethodHandle SIZEOF_VIEW = bind(LOOKUP, LIB_GLASS, "ggtk_sizeof_view_callbacks",
                FunctionDescriptor.of(JAVA_INT));
        static final MethodHandle SIZEOF_DND = bind(LOOKUP, LIB_GLASS, "ggtk_sizeof_dnd_callbacks",
                FunctionDescriptor.of(JAVA_INT));
        static final MethodHandle SIZEOF_DND_DATA = bind(LOOKUP, LIB_GLASS, "ggtk_sizeof_dnd_data",
                FunctionDescriptor.of(JAVA_INT));
        static final MethodHandle SIZEOF_DND_TARGET_VALUE = bind(LOOKUP, LIB_GLASS, "ggtk_sizeof_dnd_target_value",
                FunctionDescriptor.of(JAVA_INT));

        /** {@code int32_t ggtk_*_set_callbacks(const Ggtk*Callbacks *cb)}, in the order of {@link #CALLBACK_TABLES}. */
        static final MethodHandle APP_SET_CALLBACKS = bind(LOOKUP, LIB_GLASS, "ggtk_app_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle WINDOW_SET_CALLBACKS = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle VIEW_SET_CALLBACKS = bind(LOOKUP, LIB_GLASS, "ggtk_view_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle DND_SET_CALLBACKS = bind(LOOKUP, LIB_GLASS, "ggtk_dnd_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /*
         * THE NATIVES of glass_gtk_api.h, in its order: one function per former JNI native of GtkApplication,
         * GtkWindow, GtkView and GtkDnDClipboard whose body reads or changes the C's state. Four natives have no
         * function here: _terminateLoop, whose body was gtk_main_quit (bound above), and _getNativeView,
         * _scheduleRepaint and pushTargetActionToSystem, whose bodies were a constant or nothing - their Java
         * callers do that themselves. ggtk_window_t / ggtk_view_t / void* are ADDRESS, int32_t JAVA_INT, int64_t
         * JAVA_LONG, float JAVA_FLOAT, uint16_t JAVA_CHAR.
         */

        /* int32_t ggtk_application_init_gtk(int32_t version, int32_t verbose, float ui_scale, char **out_error) */
        static final MethodHandle APPLICATION_INIT_GTK = bind(LOOKUP, LIB_GLASS, "ggtk_application_init_gtk",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_FLOAT, ADDRESS));
        /* void ggtk_application_init(int64_t event_handler, int32_t disable_grab) */
        static final MethodHandle APPLICATION_INIT = bind(LOOKUP, LIB_GLASS, "ggtk_application_init",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
        /* void ggtk_application_run_loop(int32_t no_error_trap) */
        static final MethodHandle APPLICATION_RUN_LOOP = bind(LOOKUP, LIB_GLASS, "ggtk_application_run_loop",
                FunctionDescriptor.ofVoid(JAVA_INT));
        /* int32_t ggtk_application_get_key_code_for_char(uint16_t character, int32_t hint) */
        static final MethodHandle APPLICATION_GET_KEY_CODE_FOR_CHAR = bind(LOOKUP, LIB_GLASS,
                "ggtk_application_get_key_code_for_char", FunctionDescriptor.of(JAVA_INT, JAVA_CHAR, JAVA_INT));

        /*
         * ggtk_window_t ggtk_window_create(ggtk_window_t owner, int64_t screen, int32_t mask, int64_t visual_id,
         * int64_t window_id)
         */
        static final MethodHandle WINDOW_CREATE = bind(LOOKUP, LIB_GLASS, "ggtk_window_create",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_LONG));
        /* int32_t ggtk_window_close(ggtk_window_t window) */
        static final MethodHandle WINDOW_CLOSE = bind(LOOKUP, LIB_GLASS, "ggtk_window_close",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* int32_t ggtk_window_set_view(ggtk_window_t window, ggtk_view_t view, int64_t *out_view_id) */
        static final MethodHandle WINDOW_SET_VIEW = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_view",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        /* void ggtk_window_update_view_size(ggtk_window_t window) */
        static final MethodHandle WINDOW_UPDATE_VIEW_SIZE = bind(LOOKUP, LIB_GLASS, "ggtk_window_update_view_size",
                FunctionDescriptor.ofVoid(ADDRESS));
        /* void ggtk_window_minimize(ggtk_window_t window, int32_t minimize) */
        static final MethodHandle WINDOW_MINIMIZE = bind(LOOKUP, LIB_GLASS, "ggtk_window_minimize",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* void ggtk_window_maximize(ggtk_window_t window, int32_t maximize, int32_t was_maximized) */
        static final MethodHandle WINDOW_MAXIMIZE = bind(LOOKUP, LIB_GLASS, "ggtk_window_maximize",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));
        /*
         * void ggtk_window_set_bounds(ggtk_window_t window, int32_t x, int32_t y, int32_t x_set, int32_t y_set,
         * int32_t w, int32_t h, int32_t cw, int32_t ch, float x_gravity, float y_gravity)
         */
        static final MethodHandle WINDOW_SET_BOUNDS = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_bounds",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_INT, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT));
        /* void ggtk_window_set_visible(ggtk_window_t window, int32_t visible) */
        static final MethodHandle WINDOW_SET_VISIBLE = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_visible",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* int32_t ggtk_window_set_resizable(ggtk_window_t window, int32_t resizable) */
        static final MethodHandle WINDOW_SET_RESIZABLE = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_resizable",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        /* int32_t ggtk_window_request_focus(ggtk_window_t window, int32_t event) */
        static final MethodHandle WINDOW_REQUEST_FOCUS = bind(LOOKUP, LIB_GLASS, "ggtk_window_request_focus",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        /* void ggtk_window_set_focusable(ggtk_window_t window, int32_t focusable) */
        static final MethodHandle WINDOW_SET_FOCUSABLE = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_focusable",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* int32_t ggtk_window_grab_focus(ggtk_window_t window) */
        static final MethodHandle WINDOW_GRAB_FOCUS = bind(LOOKUP, LIB_GLASS, "ggtk_window_grab_focus",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* void ggtk_window_ungrab_focus(ggtk_window_t window) */
        static final MethodHandle WINDOW_UNGRAB_FOCUS = bind(LOOKUP, LIB_GLASS, "ggtk_window_ungrab_focus",
                FunctionDescriptor.ofVoid(ADDRESS));
        /* int32_t ggtk_window_set_title(ggtk_window_t window, const uint16_t *title, int32_t title_len) */
        static final MethodHandle WINDOW_SET_TITLE = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_title",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
        /* void ggtk_window_set_level(ggtk_window_t window, int32_t level) */
        static final MethodHandle WINDOW_SET_LEVEL = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_level",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* void ggtk_window_set_alpha(ggtk_window_t window, float alpha) */
        static final MethodHandle WINDOW_SET_ALPHA = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_alpha",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_FLOAT));
        /* int32_t ggtk_window_set_background(ggtk_window_t window, float r, float g, float b) */
        static final MethodHandle WINDOW_SET_BACKGROUND = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_background",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT));
        /* void ggtk_window_set_enabled(ggtk_window_t window, int32_t enabled) */
        static final MethodHandle WINDOW_SET_ENABLED = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_enabled",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* int32_t ggtk_window_set_minimum_size(ggtk_window_t window, int32_t w, int32_t h) */
        static final MethodHandle WINDOW_SET_MINIMUM_SIZE = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_minimum_size",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
        /* int32_t ggtk_window_set_system_minimum_size(ggtk_window_t window, int32_t w, int32_t h) */
        static final MethodHandle WINDOW_SET_SYSTEM_MINIMUM_SIZE = bind(LOOKUP, LIB_GLASS,
                "ggtk_window_set_system_minimum_size", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
        /* int32_t ggtk_window_set_maximum_size(ggtk_window_t window, int32_t w, int32_t h) */
        static final MethodHandle WINDOW_SET_MAXIMUM_SIZE = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_maximum_size",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
        /* void ggtk_window_set_icon(ggtk_window_t window, void *pixbuf, int32_t attach_threw) */
        static final MethodHandle WINDOW_SET_ICON = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_icon",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT));
        /* void ggtk_window_to_front(ggtk_window_t window) */
        static final MethodHandle WINDOW_TO_FRONT = bind(LOOKUP, LIB_GLASS, "ggtk_window_to_front",
                FunctionDescriptor.ofVoid(ADDRESS));
        /* void ggtk_window_to_back(ggtk_window_t window) */
        static final MethodHandle WINDOW_TO_BACK = bind(LOOKUP, LIB_GLASS, "ggtk_window_to_back",
                FunctionDescriptor.ofVoid(ADDRESS));
        /* void ggtk_window_set_cursor_type(ggtk_window_t window, int32_t type) */
        static final MethodHandle WINDOW_SET_CURSOR_TYPE = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_cursor_type",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* void ggtk_window_set_cursor(ggtk_window_t window, void *cursor) */
        static final MethodHandle WINDOW_SET_CURSOR = bind(LOOKUP, LIB_GLASS, "ggtk_window_set_cursor",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
        /* void ggtk_window_show_system_menu(ggtk_window_t window, int32_t x, int32_t y) */
        static final MethodHandle WINDOW_SHOW_SYSTEM_MENU = bind(LOOKUP, LIB_GLASS, "ggtk_window_show_system_menu",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));
        /* int32_t ggtk_window_is_visible(ggtk_window_t window) */
        static final MethodHandle WINDOW_IS_VISIBLE = bind(LOOKUP, LIB_GLASS, "ggtk_window_is_visible",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* int64_t ggtk_window_get_native_window(ggtk_window_t window) */
        static final MethodHandle WINDOW_GET_NATIVE_WINDOW = bind(LOOKUP, LIB_GLASS, "ggtk_window_get_native_window",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS));

        /* void ggtk_view_enable_input_method_events(ggtk_view_t view, int32_t enable) */
        static final MethodHandle VIEW_ENABLE_INPUT_METHOD_EVENTS = bind(LOOKUP, LIB_GLASS,
                "ggtk_view_enable_input_method_events", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* ggtk_view_t ggtk_view_create(int64_t view_id) */
        static final MethodHandle VIEW_CREATE = bind(LOOKUP, LIB_GLASS, "ggtk_view_create",
                FunctionDescriptor.of(ADDRESS, JAVA_LONG));
        /* int32_t ggtk_view_get_x(ggtk_view_t view) */
        static final MethodHandle VIEW_GET_X = bind(LOOKUP, LIB_GLASS, "ggtk_view_get_x",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* int32_t ggtk_view_get_y(ggtk_view_t view) */
        static final MethodHandle VIEW_GET_Y = bind(LOOKUP, LIB_GLASS, "ggtk_view_get_y",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* void ggtk_view_set_parent(ggtk_view_t view, ggtk_window_t parent) */
        static final MethodHandle VIEW_SET_PARENT = bind(LOOKUP, LIB_GLASS, "ggtk_view_set_parent",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
        /* int32_t ggtk_view_close(ggtk_view_t view) */
        static final MethodHandle VIEW_CLOSE = bind(LOOKUP, LIB_GLASS, "ggtk_view_close",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* void ggtk_view_upload_pixels_direct(ggtk_view_t view, void *data, int32_t width, int32_t height) */
        static final MethodHandle VIEW_UPLOAD_PIXELS_DIRECT = bind(LOOKUP, LIB_GLASS,
                "ggtk_view_upload_pixels_direct", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT));
        /*
         * void ggtk_view_upload_pixels_int(ggtk_view_t view, const int32_t *pixels, int32_t pixels_len,
         * int32_t offset, int32_t width, int32_t height) - with a native copy of the frame (viewUploadPixelsInt)
         */
        static final MethodHandle VIEW_UPLOAD_PIXELS_INT = bind(LOOKUP, LIB_GLASS, "ggtk_view_upload_pixels_int",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
        /*
         * void ggtk_view_upload_pixels_byte(ggtk_view_t view, const uint8_t *pixels, int32_t pixels_len,
         * int32_t offset, int32_t width, int32_t height) - with a native copy of the frame (viewUploadPixelsByte)
         */
        static final MethodHandle VIEW_UPLOAD_PIXELS_BYTE = bind(LOOKUP, LIB_GLASS, "ggtk_view_upload_pixels_byte",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
        /*
         * int32_t ggtk_view_enter_fullscreen(ggtk_view_t view, int32_t animate, int32_t keep_ratio,
         * int32_t hide_cursor)
         */
        static final MethodHandle VIEW_ENTER_FULLSCREEN = bind(LOOKUP, LIB_GLASS, "ggtk_view_enter_fullscreen",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
        /* void ggtk_view_exit_fullscreen(ggtk_view_t view, int32_t animate) */
        static final MethodHandle VIEW_EXIT_FULLSCREEN = bind(LOOKUP, LIB_GLASS, "ggtk_view_exit_fullscreen",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

        /* int32_t ggtk_dnd_is_owner(void) */
        static final MethodHandle DND_IS_OWNER = bind(LOOKUP, LIB_GLASS, "ggtk_dnd_is_owner",
                FunctionDescriptor.of(JAVA_INT));
        /* int32_t ggtk_dnd_push_to_system(const char *keys, int32_t key_count, int32_t supported) */
        static final MethodHandle DND_PUSH_TO_SYSTEM = bind(LOOKUP, LIB_GLASS, "ggtk_dnd_push_to_system",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
        /* int32_t ggtk_dnd_target_get_supported_actions(int32_t *out_actions) */
        static final MethodHandle DND_TARGET_GET_SUPPORTED_ACTIONS = bind(LOOKUP, LIB_GLASS,
                "ggtk_dnd_target_get_supported_actions", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* int32_t ggtk_dnd_target_get_mimes(char **out_strings, int32_t *out_count, int64_t *out_generation) */
        static final MethodHandle DND_TARGET_GET_MIMES = bind(LOOKUP, LIB_GLASS, "ggtk_dnd_target_get_mimes",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        /* int32_t ggtk_dnd_target_get_data(const char *mime, GgtkDndTargetValue *out) */
        static final MethodHandle DND_TARGET_GET_DATA = bind(LOOKUP, LIB_GLASS, "ggtk_dnd_target_get_data",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        private Glass() {
        }
    }

    /**
     * Hands {@code libglassgtk3.so} the four callback tables: from here on every call site of its window, view,
     * drag-and-drop and application C reaches Java through a stub of this class. Every slot must be installed before
     * the first of the natives' functions ({@link Glass}) runs: the library makes no call for a {@code NULL} slot
     * and goes on as after a normal return, with the out-parameters at the values it initialized them to
     * ({@code glass_gtk_api.h}, NULL SLOTS) - an event would reach no {@code Window} or {@code View}, and a window
     * without {@code is_enabled} counts as disabled and drops its input. Checks {@code ggtk_abi_version} first and
     * the {@code sizeof} of every table, of {@code GgtkDndData} and of {@code GgtkDndTargetValue} against the
     * layouts of this class, and refuses the library with an {@link UnsatisfiedLinkError} on a mismatch.
     * <p>
     * <b>Called once, by the {@code GtkApplication} constructor, right after {@link #link}</b>, whichever glass GTK
     * library {@code _queryLibrary} answered for: on the thread that constructs the application, before
     * {@code _initGTK} and {@code _init} and so before any GLib signal of the library is connected and before the
     * first window exists; {@code _createWindow} already dials {@code get_application_name}. The library copies each
     * table by value and reads the slots on the thread that iterates GLib's default main context, which that
     * constructor has not started yet; the stubs are in {@link Arena#global()}. A second call does nothing.
     *
     * @throws UnsatisfiedLinkError if the library lacks an export, its ABI version differs from
     *         {@link #ABI_VERSION}, or a layout disagrees with the C
     */
    static synchronized void installCallbacks() {
        if (callbackStubs != null) {
            return;
        }
        checkLayouts();
        List<MemorySegment[]> stubs = new ArrayList<>();
        for (CallbackTable table : CALLBACK_TABLES) {
            MemorySegment[] tableStubs = new MemorySegment[table.slots().size()];
            for (int i = 0; i < tableStubs.length; i++) {
                tableStubs[i] = upcallStub(table.targets().get(i), table.descriptors().get(i));
            }
            stubs.add(tableStubs);
        }
        writeCallbackTables(stubs);
        callbackStubs = List.copyOf(stubs);
    }

    /** Whether {@link #installCallbacks} has run in this JVM. */
    static synchronized boolean callbacksInstalled() {
        return callbackStubs != null;
    }

    /**
     * Writes the production tables again, with the stubs {@link #installCallbacks} created; the restore after a test
     * installed tables of its own ({@code ggtk_*_set_callbacks} copies by value, the last call wins). Installs first
     * if nothing has been installed; never creates a second set of stubs.
     */
    static synchronized void reinstallCallbacks() {
        if (callbackStubs == null) {
            installCallbacks();
            return;
        }
        writeCallbackTables(callbackStubs);
    }

    /** The installed production stubs, table by table in the order of {@link #CALLBACK_TABLES}, for tests. */
    static synchronized List<List<MemorySegment>> installedCallbackStubs() {
        List<List<MemorySegment>> result = new ArrayList<>();
        if (callbackStubs != null) {
            for (MemorySegment[] tableStubs : callbackStubs) {
                result.add(List.of(tableStubs));
            }
        }
        return result;
    }

    /**
     * Fills the four tables with {@code stubs} - one array per table of {@link #CALLBACK_TABLES}, in slot order; a
     * {@code null} element leaves its slot {@code NULL}, which the library then never calls (see
     * {@link #installCallbacks}); only tests write such a table - and installs them. Each slot is written at the
     * offset of its name in the layout, never at a computed literal.
     */
    static void writeCallbackTables(List<MemorySegment[]> stubs) {
        MethodHandle[] installers = {Glass.APP_SET_CALLBACKS, Glass.WINDOW_SET_CALLBACKS, Glass.VIEW_SET_CALLBACKS,
                Glass.DND_SET_CALLBACKS};
        try (Arena arena = Arena.ofConfined()) {
            for (int t = 0; t < CALLBACK_TABLES.size(); t++) {
                CallbackTable table = CALLBACK_TABLES.get(t);
                MemorySegment[] tableStubs = stubs.get(t);
                if (tableStubs.length != table.slots().size()) {
                    throw new IllegalArgumentException(table.slots().size() + " slots, " + tableStubs.length
                            + " stubs");
                }
                MemorySegment segment = arena.allocate(table.layout());
                for (int i = 0; i < tableStubs.length; i++) {
                    segment.set(ADDRESS, table.layout().byteOffset(PathElement.groupElement(table.slots().get(i))),
                            tableStubs[i] == null ? MemorySegment.NULL : tableStubs[i]);
                }
                int status = (int) installers[t].invokeExact(segment);
                if (status != 0) {
                    throw new IllegalStateException("ggtk_*_set_callbacks answered " + status);
                }
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code sizeof} of the four tables and of {@code GgtkDndData} as the C compiler saw them, in the order of
     * {@link #CALLBACK_TABLES}, then the data.
     */
    static int[] callbackLayoutSizes() {
        try {
            return new int[] {(int) Glass.SIZEOF_APP.invokeExact(), (int) Glass.SIZEOF_WINDOW.invokeExact(),
                    (int) Glass.SIZEOF_VIEW.invokeExact(), (int) Glass.SIZEOF_DND.invokeExact(),
                    (int) Glass.SIZEOF_DND_DATA.invokeExact()};
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@link #installCallbacks}' check: every C {@code sizeof} equals the byte size of its layout here - the four
     * tables, {@code GgtkDndData}, and the {@code GgtkDndTargetValue} of {@link #dndPopFromSystem}.
     */
    private static void checkLayouts() {
        int[] sizes = callbackLayoutSizes();
        long[] expected = {GGTK_APP_CALLBACKS_LAYOUT.byteSize(), GGTK_WINDOW_CALLBACKS_LAYOUT.byteSize(),
                GGTK_VIEW_CALLBACKS_LAYOUT.byteSize(), GGTK_DND_CALLBACKS_LAYOUT.byteSize(),
                GGTK_DND_DATA_LAYOUT.byteSize()};
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] != expected[i]) {
                throw new UnsatisfiedLinkError(LIB_GLASS + " layout mismatch: sizeof " + i + " is " + sizes[i]
                        + ", the layout has " + expected[i] + " bytes");
            }
        }
        int targetValue;
        try {
            targetValue = (int) Glass.SIZEOF_DND_TARGET_VALUE.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (targetValue != GGTK_DND_TARGET_VALUE_LAYOUT.byteSize()) {
            throw new UnsatisfiedLinkError(LIB_GLASS + " layout mismatch: sizeof GgtkDndTargetValue is " + targetValue
                    + ", the layout has " + GGTK_DND_TARGET_VALUE_LAYOUT.byteSize() + " bytes");
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * The natives of GtkApplication (all but _queryLibrary), GtkWindow, GtkView and GtkDnDClipboard. Each method
     * replaces the Java_com_sun_glass_ui_gtk_* function of commit 033187ad90 it names; the body of that function is
     * the ggtk_* function of glass_gtk_api.h (THE NATIVES) the method calls - for _terminateLoop, gtk_main_quit
     * itself; GtkView._getNativeView, _scheduleRepaint and GtkDnDClipboard.pushTargetActionToSystem answer 0 or do
     * nothing in Java, as their bodies did - and what the JNI function did with its Java arguments and result - the
     * conversions, its calls into Java, the exception it threw or left pending - is done here, in its order, around
     * the downcall. A method runs on the thread that calls it, which is the
     * thread the JNI native ran on. The functions the header marks UPCALLS run Java inside the downcall (the callback
     * tables, nested main loops); their segments live in an arena that closes only when the downcall has returned.
     * ------------------------------------------------------------------------------------------------------- */

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1initGTK} ({@code GlassApplication.cpp}):
     * {@code ggtk_application_init_gtk} on the thread that constructs the application - GTK initialized and the GDK
     * lock entered. When GTK is older than the build's minimum the JNI native returned with an
     * {@code UnsupportedOperationException} pending; the C hands its message back in a {@code g_malloc}'d block, and
     * it is thrown here once the downcall has returned, decoded as {@code ThrowNew} decoded it. The JNI native's
     * first statement, {@code ExceptionClear}, never had anything to clear.
     *
     * @throws UnsupportedOperationException if GTK is older than the build's minimum
     */
    static void applicationInitGtk(int version, boolean verbose, float uiScale) {
        boolean tooOld;
        String message;
        try (Arena call = Arena.ofConfined()) {
            MemorySegment outError = call.allocate(ADDRESS);
            int status = (int) Glass.APPLICATION_INIT_GTK.invokeExact(version, verbose ? 1 : 0, uiScale, outError);
            MemorySegment error = outError.get(ADDRESS, 0);
            tooOld = status == ERR_GTK_VERSION;
            try {
                message = tooOld ? newStringUtf(error) : null;
            } finally {
                G_FREE_CALL.invokeExact(error);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (tooOld) {
            throw new UnsupportedOperationException(message);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1init} ({@code GlassApplication.cpp}):
     * {@code ggtk_application_init} on the toolkit thread - GDK's event handler, the screen signals and the root
     * window's PropertyNotify; from here on events reach the callback tables. The JNI native first stored its
     * {@code JNIEnv} in {@code mainEnv} for the JNI calls of the event code, which calls Java through the tables now.
     */
    static void applicationInit(long eventHandler, boolean disableGrab) {
        try {
            Glass.APPLICATION_INIT.invokeExact(eventHandler, disableGrab ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1runLoop} ({@code GlassApplication.cpp}) after its first two
     * statements, which {@code GtkApplication._runLoop} performs: {@code ggtk_application_run_loop} on the toolkit
     * thread - the GDK error trap unless {@code noErrorTrap}, {@code gtk_main} until
     * {@link #applicationTerminateLoop} quits it, then the GDK lock is left. Every event of the toolkit is dispatched
     * inside this downcall.
     */
    static void applicationRunLoop(boolean noErrorTrap) {
        try {
            Glass.APPLICATION_RUN_LOOP.invokeExact(noErrorTrap ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1terminateLoop} ({@code GlassApplication.cpp}): its first
     * statement, {@code gtk_main_quit()}, bound from GTK directly; the {@code delete platformSupport} that followed
     * it is {@code GtkApplication}'s {@link #platformSupportDestroy} call.
     */
    static void applicationTerminateLoop() {
        try {
            GTK_MAIN_QUIT.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1getKeyCodeForChar} ({@code glass_key.cpp}): the
     * {@code KeyEvent.VK_*} code of the UTF-16 code unit {@code c}; {@code hint} is ignored, as it was.
     */
    static int applicationKeyCodeForChar(char c, int hint) {
        try {
            return (int) Glass.APPLICATION_GET_KEY_CODE_FOR_CHAR.invokeExact(c, hint);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkWindow__1createWindow} ({@code GlassWindow.cpp}): a new
     * {@code WindowContextTop} whose slots receive {@code windowId}. {@code visualId} is
     * {@code GtkApplication.visualID}, which the JNI read inside the constructor. Dials only the application table's
     * {@code get_application_name}.
     *
     * @return the {@code WindowContext *}, never 0
     */
    static long windowCreate(long owner, long screen, int mask, long visualId, long windowId) {
        try {
            MemorySegment window = (MemorySegment) Glass.WINDOW_CREATE.invokeExact(MemorySegment.ofAddress(owner),
                    screen, mask, visualId, windowId);
            return window.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkWindow__1close}: {@code destroy_and_delete_ctx}, whose
     * {@code notify_destroy} runs inside the downcall; {@code true}, the JNI's "return value not used".
     */
    static boolean windowClose(long window) {
        try {
            return (int) Glass.WINDOW_CLOSE.invokeExact(MemorySegment.ofAddress(window)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * What {@code ggtk_window_set_view} answers: {@code set_view}'s result, and the id of the view the window holds
     * now (0 for none), which the C hands back so that nothing reads a {@code WindowContext} after the {@code EXIT}
     * handler inside the call may have destroyed it.
     */
    record SetView(boolean result, long viewId) {
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setView} ({@code GlassWindow.cpp}): {@code ggtk_window_set_view}
     * with {@code view}, the {@code GlassView *} of the {@code View} ({@code View.ptr}, which the JNI read with
     * {@code GetLongField}), 0 for none. The {@code EXIT} that {@code set_view} sends the view the window held reaches
     * Java inside the downcall; what it throws is for {@code GtkWindow._setView} to throw.
     */
    static SetView windowSetView(long window, long view) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment outViewId = call.allocate(JAVA_LONG);
            int result = (int) Glass.WINDOW_SET_VIEW.invokeExact(MemorySegment.ofAddress(window),
                    MemorySegment.ofAddress(view), outViewId);
            return new SetView(result != 0, outViewId.get(JAVA_LONG, 0));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1updateViewSize}: {@code update_view_size}. */
    static void windowUpdateViewSize(long window) {
        try {
            Glass.WINDOW_UPDATE_VIEW_SIZE.invokeExact(MemorySegment.ofAddress(window));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow_minimizeImpl}: {@code set_minimized}. */
    static void windowMinimize(long window, boolean minimize) {
        try {
            Glass.WINDOW_MINIMIZE.invokeExact(MemorySegment.ofAddress(window), minimize ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow_maximizeImpl}: {@code set_maximized}, {@code wasMaximized} unused. */
    static void windowMaximize(long window, boolean maximize, boolean wasMaximized) {
        try {
            Glass.WINDOW_MAXIMIZE.invokeExact(MemorySegment.ofAddress(window), maximize ? 1 : 0,
                    wasMaximized ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setBounds}: {@code set_bounds} and its notifications. */
    static void windowSetBounds(long window, int x, int y, boolean xSet, boolean ySet, int w, int h, int cw, int ch,
                                float xGravity, float yGravity) {
        try {
            Glass.WINDOW_SET_BOUNDS.invokeExact(MemorySegment.ofAddress(window), x, y, xSet ? 1 : 0, ySet ? 1 : 0, w,
                    h, cw, ch, xGravity, yGravity);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow_setVisibleImpl}: {@code WindowContextTop::set_visible}. */
    static void windowSetVisible(long window, boolean visible) {
        try {
            Glass.WINDOW_SET_VISIBLE.invokeExact(MemorySegment.ofAddress(window), visible ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setResizable}: {@code set_resizable}; {@code true}. */
    static boolean windowSetResizable(long window, boolean resizable) {
        try {
            return (int) Glass.WINDOW_SET_RESIZABLE.invokeExact(MemorySegment.ofAddress(window), resizable ? 1 : 0)
                    != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1requestFocus}: {@code gtk_window_present}; {@code true}. */
    static boolean windowRequestFocus(long window, int event) {
        try {
            return (int) Glass.WINDOW_REQUEST_FOCUS.invokeExact(MemorySegment.ofAddress(window), event) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setFocusable}: {@code gtk_window_set_accept_focus}. */
    static void windowSetFocusable(long window, boolean focusable) {
        try {
            Glass.WINDOW_SET_FOCUSABLE.invokeExact(MemorySegment.ofAddress(window), focusable ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1grabFocus}: {@code grab_focus}. */
    static boolean windowGrabFocus(long window) {
        try {
            return (int) Glass.WINDOW_GRAB_FOCUS.invokeExact(MemorySegment.ofAddress(window)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1ungrabFocus}: {@code ungrab_focus} and its upcall. */
    static void windowUngrabFocus(long window) {
        try {
            Glass.WINDOW_UNGRAB_FOCUS.invokeExact(MemorySegment.ofAddress(window));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setTitle} ({@code GlassWindow.cpp}): the title's UTF-16 code units
     * as {@code GetStringChars} gave them - copied as {@code char}s, lone surrogates included, never through a
     * charset - and their count, {@code NULL} for a {@code null} title; the C converts them with
     * {@code g_utf16_to_utf8}. An empty title is a non-{@code NULL} block, as {@code GetStringChars} answered one.
     */
    static boolean windowSetTitle(long window, String title) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment units = MemorySegment.NULL;
            int length = 0;
            if (title != null) {
                char[] chars = title.toCharArray();
                length = chars.length;
                units = call.allocate(JAVA_CHAR, Math.max(1, length));
                MemorySegment.copy(chars, 0, units, JAVA_CHAR, 0, length);
            }
            return (int) Glass.WINDOW_SET_TITLE.invokeExact(MemorySegment.ofAddress(window), units, length) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setLevel}: {@code set_level}. */
    static void windowSetLevel(long window, int level) {
        try {
            Glass.WINDOW_SET_LEVEL.invokeExact(MemorySegment.ofAddress(window), level);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setAlpha}: {@code gtk_window_set_opacity}. */
    static void windowSetAlpha(long window, float alpha) {
        try {
            Glass.WINDOW_SET_ALPHA.invokeExact(MemorySegment.ofAddress(window), alpha);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setBackground}: {@code set_background}; {@code true}. */
    static boolean windowSetBackground(long window, float r, float g, float b) {
        try {
            return (int) Glass.WINDOW_SET_BACKGROUND.invokeExact(MemorySegment.ofAddress(window), r, g, b) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setEnabled}: {@code set_enabled}. */
    static void windowSetEnabled(long window, boolean enabled) {
        try {
            Glass.WINDOW_SET_ENABLED.invokeExact(MemorySegment.ofAddress(window), enabled ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setMinimumSize}: {@code false} for a negative size. */
    static boolean windowSetMinimumSize(long window, int w, int h) {
        try {
            return (int) Glass.WINDOW_SET_MINIMUM_SIZE.invokeExact(MemorySegment.ofAddress(window), w, h) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setSystemMinimumSize}: {@code false} for a negative size. */
    static boolean windowSetSystemMinimumSize(long window, int w, int h) {
        try {
            return (int) Glass.WINDOW_SET_SYSTEM_MINIMUM_SIZE.invokeExact(MemorySegment.ofAddress(window), w, h) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setMaximumSize}: {@code false} for a zero size. */
    static boolean windowSetMaximumSize(long window, int w, int h) {
        try {
            return (int) Glass.WINDOW_SET_MAXIMUM_SIZE.invokeExact(MemorySegment.ofAddress(window), w, h) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setIcon} ({@code GlassWindow.cpp}): {@code pixels.attachData}
     * with the address of a {@code NULL} {@code GdkPixbuf *}, as the JNI passed the address of its local - a throw
     * of it reported as {@code EXCEPTION_OCCURED} reported it - then {@code ggtk_window_set_icon} with whatever was
     * written there, which sets the icon unless that threw and releases the pixbuf in either case.
     */
    static void windowSetIcon(long window, Pixels pixels) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment slot = call.allocate(ADDRESS);
            boolean threw = false;
            if (pixels != null) {
                try {
                    PIXELS_ATTACH_DATA.invokeExact(pixels, slot.address());
                } catch (Throwable t) {
                    reportException(t);
                    threw = true;
                }
            }
            Glass.WINDOW_SET_ICON.invokeExact(MemorySegment.ofAddress(window), slot.get(ADDRESS, 0), threw ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1toFront}: {@code gdk_window_raise}. */
    static void windowToFront(long window) {
        try {
            Glass.WINDOW_TO_FRONT.invokeExact(MemorySegment.ofAddress(window));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1toBack}: {@code gdk_window_lower}. */
    static void windowToBack(long window) {
        try {
            Glass.WINDOW_TO_BACK.invokeExact(MemorySegment.ofAddress(window));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setCursorType}: the {@code GdkCursor} of {@code type}. */
    static void windowSetCursorType(long window, int type) {
        try {
            Glass.WINDOW_SET_CURSOR_TYPE.invokeExact(MemorySegment.ofAddress(window), type);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setCustomCursor}: {@code cursor} is the {@code GdkCursor *} of the
     * {@code Cursor}, which the JNI read from its {@code ptr} field.
     */
    static void windowSetCursor(long window, long cursor) {
        try {
            Glass.WINDOW_SET_CURSOR.invokeExact(MemorySegment.ofAddress(window), MemorySegment.ofAddress(cursor));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1showSystemMenu}: {@code gdk_window_show_window_menu}. */
    static void windowShowSystemMenu(long window, int x, int y) {
        try {
            Glass.WINDOW_SHOW_SYSTEM_MENU.invokeExact(MemorySegment.ofAddress(window), x, y);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow_isVisible}: {@code gtk_widget_get_visible}. */
    static boolean windowIsVisible(long window) {
        try {
            return (int) Glass.WINDOW_IS_VISIBLE.invokeExact(MemorySegment.ofAddress(window)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkWindow__1getNativeWindowImpl}: the X11 window, 0 before it is realized. */
    static long windowNativeWindow(long window) {
        try {
            return (long) Glass.WINDOW_GET_NATIVE_WINDOW.invokeExact(MemorySegment.ofAddress(window));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkView_enableInputMethodEventsImpl}: the IME of the view's window. */
    static void viewEnableInputMethodEvents(long view, boolean enable) {
        try {
            Glass.VIEW_ENABLE_INPUT_METHOD_EVENTS.invokeExact(MemorySegment.ofAddress(view), enable ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkView__1create} ({@code GlassView.cpp}): a new {@code GlassView} whose
     * slots receive {@code viewId}; the JNI ignored the caps map. No upcall.
     *
     * @return the {@code GlassView *}, never 0
     */
    static long viewCreate(long viewId) {
        try {
            return ((MemorySegment) Glass.VIEW_CREATE.invokeExact(viewId)).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkView__1getX}: the content's x in its window, 0 without one. */
    static int viewX(long view) {
        try {
            return (int) Glass.VIEW_GET_X.invokeExact(MemorySegment.ofAddress(view));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkView__1getY}: the content's y in its window, 0 without one. */
    static int viewY(long view) {
        try {
            return (int) Glass.VIEW_GET_Y.invokeExact(MemorySegment.ofAddress(view));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkView__1setParent}: the view's window, then {@code notify_view} with
     * {@code REMOVE} or {@code ADD} for the view's own id - the JNI called {@code notifyView} on the {@code View}
     * the native ran for, which is that id's peer.
     */
    static void viewSetParent(long view, long parent) {
        try {
            Glass.VIEW_SET_PARENT.invokeExact(MemorySegment.ofAddress(view), MemorySegment.ofAddress(parent));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkView__1close}: deletes the {@code GlassView}; {@code true}. */
    static boolean viewClose(long view) {
        try {
            return (int) Glass.VIEW_CLOSE.invokeExact(MemorySegment.ofAddress(view)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkView__1uploadPixelsDirect} ({@code GlassView.cpp}): nothing for a view of
     * 0 or a {@code null} buffer, the JNI's first two returns; otherwise the paint from the buffer's base address -
     * what {@code GetDirectBufferAddress} gave, its position ignored, {@code NULL} for a buffer that is not direct.
     */
    static void viewUploadPixelsDirect(long view, Buffer pixels, int width, int height) {
        if (view == 0 || pixels == null) {
            return;
        }
        MemorySegment data = pixels.isDirect() ? directBufferBase(pixels) : MemorySegment.NULL;
        try {
            Glass.VIEW_UPLOAD_PIXELS_DIRECT.invokeExact(MemorySegment.ofAddress(view), data, width, height);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkView__1uploadPixelsIntArray} ({@code GlassView.cpp}): nothing for a view of
     * 0 or a {@code null} array, the JNI's first two returns; otherwise {@code ggtk_view_upload_pixels_int}, which
     * checks the frame's bounds and paints it ({@code WindowContextBase::paint}: {@code gdk_window_begin_paint_region},
     * cairo, {@code gdk_window_end_paint}).
     * <p>
     * The JNI painted from the array it held with {@code GetPrimitiveArrayCritical}, its thread in native, where the
     * VM does not wait for it: a safepoint requested meanwhile went ahead (G1 pins the array's region). The paint
     * writes a whole frame to the X server, and a {@link Linker.Option#critical critical} downcall with heap access,
     * which would hand the C the array in place, keeps its thread in Java for as long as the call runs - every
     * safepoint of the VM would wait for the paint. So the frame is copied instead: the {@code width * height} ints
     * at {@code offset}, the part the C paints, go into {@code staging}, a native block of the view, and the C gets
     * that block as an array of exactly the frame's length at offset 0, through an ordinary downcall. Its checks
     * then pass exactly when they passed on the array itself: the copy is made only for a frame inside the array
     * ({@link #frameLength}, the C's own test), and a frame outside it is neither copied nor handed over, where the
     * C returned without painting.
     */
    static void viewUploadPixelsInt(long view, UploadStaging staging, int[] pixels, int offset, int width,
                                    int height) {
        if (view == 0 || pixels == null) {
            return;
        }
        int length = frameLength(pixels.length, offset, width, height, 1);
        if (length < 0) {
            return;
        }
        synchronized (staging) {
            MemorySegment frame = staging.copyIn(MemorySegment.ofArray(pixels), (long) offset * Integer.BYTES,
                    (long) length * Integer.BYTES);
            try {
                Glass.VIEW_UPLOAD_PIXELS_INT.invokeExact(MemorySegment.ofAddress(view), frame, length, 0, width,
                        height);
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkView__1uploadPixelsByteArray} ({@code GlassView.cpp}):
     * {@link #viewUploadPixelsInt} over bytes - the {@code 4 * width * height} bytes at {@code offset} - through
     * {@code ggtk_view_upload_pixels_byte}, copied into {@code staging} for the same reason.
     */
    static void viewUploadPixelsByte(long view, UploadStaging staging, byte[] pixels, int offset, int width,
                                     int height) {
        if (view == 0 || pixels == null) {
            return;
        }
        int length = frameLength(pixels.length, offset, width, height, 4);
        if (length < 0) {
            return;
        }
        synchronized (staging) {
            MemorySegment frame = staging.copyIn(MemorySegment.ofArray(pixels), offset, length);
            try {
                Glass.VIEW_UPLOAD_PIXELS_BYTE.invokeExact(MemorySegment.ofAddress(view), frame, length, 0, width,
                        height);
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    /**
     * The number of array elements of a frame of {@code width * height} pixels of {@code unit} elements each, at
     * {@code offset} in an array of {@code length}; -1 where {@code ggtk_view_upload_pixels_int} ({@code unit} 1) or
     * {@code _byte} ({@code unit} 4) refuses the frame: a negative offset, a width or height that is not positive,
     * or {@code unit * width * height + offset} beyond the array - including where that sum overflows an
     * {@code int}, which the C tests first ({@code width > ((INT_MAX - offset) / unit) / height}) and which exceeds
     * any array length.
     */
    static int frameLength(int length, int offset, int width, int height, int unit) {
        if (offset < 0 || width <= 0 || height <= 0) {
            return -1;
        }
        long pixels = (long) width * height;
        if (pixels > ((long) length - offset) / unit) {
            return -1;
        }
        return (int) (pixels * unit);
    }

    /**
     * The native block a view's frames are copied into before {@code ggtk_view_upload_pixels_int} or {@code _byte}
     * paints them ({@link #viewUploadPixelsInt}): allocated at the view's first upload from a Java array, grown - never
     * shrunk - when a frame is larger, freed by {@link #release} when the view closes. The C keeps no pointer to it
     * once the paint has returned, as it kept none to the JNI's critical array. A frame is copied in pieces of at
     * most {@link #CHUNK_BYTES}, so that a safepoint requested during the copy waits for one piece at most.
     * <p>
     * Frames are uploaded on the FX thread ({@code View.uploadPixels} checks it); the uploads hold this object's
     * monitor from the copy until the paint has returned, and so does {@link #release}, so a block is never freed
     * under the C.
     */
    static final class UploadStaging {

        /** The most bytes copied between two safepoint polls. */
        static final long CHUNK_BYTES = 1L << 18;

        /** The alignment of the block: more than cairo and pixman ask of image data. */
        private static final long ALIGNMENT = 64;

        private Arena arena;
        private MemorySegment block = MemorySegment.NULL;

        /**
         * Copies {@code byteSize} bytes of {@code source} from {@code sourceOffset} to the start of the block, grown
         * first if it is smaller, and answers the block. The uploads hold this object's monitor from this call until
         * the paint from the block has returned. Allocating the block can throw {@link OutOfMemoryError}.
         */
        synchronized MemorySegment copyIn(MemorySegment source, long sourceOffset, long byteSize) {
            if (block.byteSize() < byteSize) {
                long capacity = Math.max(byteSize, block.byteSize() + block.byteSize() / 2);
                release();
                arena = Arena.ofShared();
                block = arena.allocate(capacity, ALIGNMENT);
            }
            for (long done = 0; done < byteSize; done += CHUNK_BYTES) {
                MemorySegment.copy(source, sourceOffset + done, block, done, Math.min(CHUNK_BYTES, byteSize - done));
            }
            return block;
        }

        /** Frees the block; the next upload allocates a new one. */
        synchronized void release() {
            if (arena != null) {
                arena.close();
                arena = null;
            }
            block = MemorySegment.NULL;
        }

        /** The size of the block in bytes, 0 when there is none. */
        synchronized long capacity() {
            return block.byteSize();
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkView__1enterFullscreen}: {@code gtk_window_fullscreen} and
     * {@code notify_view(FULLSCREEN_ENTER)}; {@code false} when that threw, as {@code CHECK_JNI_EXCEPTION_RET}
     * answered.
     */
    static boolean viewEnterFullscreen(long view, boolean animate, boolean keepRatio, boolean hideCursor) {
        try {
            return (int) Glass.VIEW_ENTER_FULLSCREEN.invokeExact(MemorySegment.ofAddress(view), animate ? 1 : 0,
                    keepRatio ? 1 : 0, hideCursor ? 1 : 0) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkView__1exitFullscreen}: out of full screen, {@code FULLSCREEN_EXIT}. */
    static void viewExitFullscreen(long view, boolean animate) {
        try {
            Glass.VIEW_EXIT_FULLSCREEN.invokeExact(MemorySegment.ofAddress(view), animate ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkDnDClipboard_isOwner} ({@code GlassDnDClipboard.cpp}). */
    static boolean dndIsOwner() {
        try {
            return (int) Glass.DND_IS_OWNER.invokeExact() != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkDnDClipboard_pushToSystemImpl} ({@code GlassDnDClipboard.cpp},
     * {@code execute_dnd} of {@code glass_dnd.cpp}): the keys of {@code data} in its iteration order, each as the
     * modified UTF-8 {@code GetStringUTFChars} gave and NUL-terminated, back to back, then
     * {@code ggtk_dnd_push_to_system}, which runs the whole drag and answers the performed action. With
     * {@code supported} 0 no drag starts and no key is walked, as before. The values are read through the
     * {@code source_get_data} slot while the drag runs, so {@code data} must be the drag in progress
     * ({@link GtkDnDClipboard#dragInProgress}).
     * <p>
     * The JNI walked the keys after it had created and shown the drag widget. A throw of that walk (only
     * {@code OutOfMemoryError} or {@code ConcurrentModificationException} on a {@code HashMap}) was reported, then
     * {@code jni_exception} asked the already-cleared {@code Throwable} for its message, which left a
     * {@code NullPointerException} pending, and the native queued the widget's destruction and returned with it: the
     * report and that {@code NullPointerException} are reproduced here, but no drag widget is created first. A
     * {@code null} key, on which the JNI's {@code GetStringUTFChars} crashed, takes the same path.
     */
    static int dndPushToSystem(HashMap<String, Object> data, int supported) {
        byte[] keys = new byte[0];
        int count = 0;
        if (supported != 0) {
            try {
                ByteArrayOutputStream block = new ByteArrayOutputStream();
                for (String key : data.keySet()) {
                    block.writeBytes(JniStringCodec.toModifiedUtf8(key));
                    count++;
                }
                keys = block.toByteArray();
            } catch (Throwable t) {
                reportException(t);
                throw new NullPointerException();
            }
        }
        try (Arena call = Arena.ofConfined()) {
            MemorySegment block = call.allocate(Math.max(1, keys.length));
            MemorySegment.copy(keys, 0, block, JAVA_BYTE, 0, keys.length);
            return (int) Glass.DND_PUSH_TO_SYSTEM.invokeExact(block, count, supported);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkDnDClipboard_supportedSourceActionsFromSystem}: the
     * {@code Clipboard.ACTION_*} bits the source of the drag that last entered a Glass window offers.
     *
     * @throws IllegalStateException if no drag has entered one, as {@code check_state_in_drag} threw
     */
    static int dndSupportedSourceActions() {
        int status;
        int actions;
        try (Arena call = Arena.ofConfined()) {
            MemorySegment outActions = call.allocate(JAVA_INT);
            status = (int) Glass.DND_TARGET_GET_SUPPORTED_ACTIONS.invokeExact(outActions);
            actions = outActions.get(JAVA_INT, 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (status == ERR_NOT_IN_DRAG) {
            throw new IllegalStateException(NOT_IN_DRAG_MESSAGE);
        }
        return actions;
    }

    /**
     * The {@code String[]} {@link #dndMimesFromSystem} answered last and the number of the C's mime list it was built
     * from: the JNI returned one array per drag enter ({@code enter_ctx.mimes}), and the C numbers the list it
     * computes once per drag enter. Only touched on the thread that iterates the main context.
     */
    private static String[] dndMimes;
    private static long dndMimesGeneration;

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkDnDClipboard_mimesFromSystem} ({@code dnd_target_get_mimes},
     * {@code glass_dnd.cpp}): the mime types of the drag that last entered a Glass window. The first call of a drag
     * walks its targets and fetches its uri list through a nested main loop, inside the downcall. The
     * {@code OutOfMemoryError}s the JNI reported for event hooks it could not allocate are reported first; then, as
     * the JNI did once per drag enter, every string the C collected is decoded as {@code NewStringUTF} decoded it and
     * added, in order, to a new {@code HashSet}, whose {@code toArray(new String[size])} is the result - or, for a
     * list already answered, the very array answered for it.
     *
     * @throws IllegalStateException if no drag has entered a Glass window, as {@code check_state_in_drag} threw
     */
    static String[] dndMimesFromSystem() {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment outStrings = call.allocate(ADDRESS);
            MemorySegment outCount = call.allocate(JAVA_INT);
            MemorySegment outGeneration = call.allocate(JAVA_LONG);
            int result = (int) Glass.DND_TARGET_GET_MIMES.invokeExact(outStrings, outCount, outGeneration);
            if (result == ERR_NOT_IN_DRAG) {
                throw new IllegalStateException(NOT_IN_DRAG_MESSAGE);
            }
            MemorySegment strings = outStrings.get(ADDRESS, 0);
            try {
                reportHookOutOfMemory(result);
                long generation = outGeneration.get(JAVA_LONG, 0);
                if (dndMimes != null && generation == dndMimesGeneration) {
                    return dndMimes;
                }
                int count = outCount.get(JAVA_INT, 0);
                HashSet<String> set = new HashSet<>();
                long offset = 0;
                for (int i = 0; i < count; i++) {
                    byte[] mime = cString(MemorySegment.ofAddress(strings.address() + offset));
                    set.add(JniStringCodec.fromNewStringUtf(mime));
                    offset += mime.length + 1;
                }
                String[] mimes = set.toArray(new String[set.size()]);
                dndMimes = mimes;
                dndMimesGeneration = generation;
                return mimes;
            } finally {
                G_FREE_CALL.invokeExact(strings);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkDnDClipboard_popFromSystem} ({@code dnd_target_get_data},
     * {@code glass_dnd.cpp}): the drag's data for {@code mimeType}, fetched by {@code ggtk_dnd_target_get_data}
     * through nested main loops inside the downcall. The {@code OutOfMemoryError}s the JNI reported for event hooks
     * it could not allocate are reported first; then the value is built as the JNI built it
     * ({@link #dndTargetValue}), and the C's block is released.
     * <p>
     * A {@code null} {@code mimeType}: the JNI threw the {@code IllegalStateException} when no drag had entered a
     * Glass window and otherwise crashed in {@code GetStringUTFChars}; here the drag is checked the same way and a
     * {@code NullPointerException} is thrown instead of the crash.
     *
     * @throws IllegalStateException if no drag has entered a Glass window, as {@code check_state_in_drag} threw
     */
    static Object dndPopFromSystem(String mimeType) {
        if (mimeType == null) {
            dndSupportedSourceActions();
            throw new NullPointerException();
        }
        try (Arena call = Arena.ofConfined()) {
            MemorySegment mime = modifiedUtf8(call, mimeType);
            MemorySegment value = call.allocate(GGTK_DND_TARGET_VALUE_LAYOUT);
            int result = (int) Glass.DND_TARGET_GET_DATA.invokeExact(mime, value);
            if (result == ERR_NOT_IN_DRAG) {
                throw new IllegalStateException(NOT_IN_DRAG_MESSAGE);
            }
            MemorySegment data = value.get(ADDRESS, targetValueOffset("data"));
            try {
                reportHookOutOfMemory(result);
                return dndTargetValue(value, data);
            } finally {
                G_FREE_CALL.invokeExact(data);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code dnd_target_value_to_java} ({@code glass_dnd.cpp}): the {@code String}, {@code ByteBuffer},
     * {@code GtkPixels} or {@code String[]} a {@code GgtkDndTargetValue} stands for, built as the JNI built it -
     * the bytes as {@code NewStringUTF} decoded them, copies of the C's bytes wrapped. The JNI reported a throw of
     * each of its steps ({@code EXCEPTION_OCCURED}) and answered {@code null}, as this does (only an
     * {@code OutOfMemoryError} can be thrown there). The file list stores each path at its record's index, which
     * {@code dnd_target_uris_to_value} counts over the file URIs alone; an index outside the array is still the
     * {@code ArrayIndexOutOfBoundsException} {@code SetObjectArrayElement} raised, reported, and the element stays
     * {@code null}. Commit {@code 033187ad90} counted that index over all URIs of the list, which put a file URI
     * following a non-file one outside an array sized by the file count.
     */
    @SuppressWarnings("restricted")
    private static Object dndTargetValue(MemorySegment value, MemorySegment data) {
        int kind = value.get(JAVA_INT, targetValueOffset("kind"));
        int count = value.get(JAVA_INT, targetValueOffset("count"));
        try {
            return switch (kind) {
                case DND_VALUE_STRING -> JniStringCodec.fromNewStringUtf(data.reinterpret(count).toArray(JAVA_BYTE));
                case DND_VALUE_BYTES -> ByteBuffer.wrap(data.reinterpret(count).toArray(JAVA_BYTE));
                case DND_VALUE_IMAGE -> new GtkPixels(value.get(JAVA_INT, targetValueOffset("width")),
                        value.get(JAVA_INT, targetValueOffset("height")),
                        ByteBuffer.wrap(data.reinterpret(count).toArray(JAVA_BYTE)));
                case DND_VALUE_FILES -> dndFiles(data, count, value.get(JAVA_INT, targetValueOffset("records")));
                default -> null;
            };
        } catch (Throwable t) {
            reportException(t);
            return null;
        }
    }

    /** The {@code String[count]} of the {@code records} file records at {@code data} ({@code GGTK_DND_VALUE_FILES}). */
    @SuppressWarnings("restricted")
    private static String[] dndFiles(MemorySegment data, int count, int records) {
        String[] files = new String[count];
        long offset = 0;
        for (int r = 0; r < records; r++) {
            MemorySegment header = MemorySegment.ofAddress(data.address() + offset).reinterpret(8);
            int index = header.get(JAVA_INT_UNALIGNED, 0);
            int length = header.get(JAVA_INT_UNALIGNED, 4);
            String path = null;
            if (length >= 0) {
                path = JniStringCodec.fromNewStringUtf(MemorySegment.ofAddress(data.address() + offset + 8)
                        .reinterpret(length).toArray(JAVA_BYTE));
            }
            if (index >= 0 && index < count) {
                files[index] = path;
            } else {
                reportException(new ArrayIndexOutOfBoundsException("Index " + index + " out of bounds for length "
                        + count));
            }
            offset += 8 + (length >= 0 ? ((length + 1 + 3) & ~3) : 0);
        }
        return files;
    }

    /** The {@code OutOfMemoryError}s {@code glass_throw_oom} reported for event hooks the C could not allocate. */
    private static void reportHookOutOfMemory(int reports) {
        for (int i = 0; i < reports; i++) {
            reportException(new OutOfMemoryError("Failed to allocate event hook"));
        }
    }

    /** The offset of the field {@code name} of {@code GgtkDndTargetValue}. */
    private static long targetValueOffset(String name) {
        return GGTK_DND_TARGET_VALUE_LAYOUT.byteOffset(PathElement.groupElement(name));
    }

    /*
     * The targets of the 29 slots. Each is the Call*Method of the C with the peer the id names as the receiver,
     * reached by a virtual call through the GtkWindow / GtkView dispatch methods (View.notify* and Window.notify* are
     * protected in com.sun.glass.ui: only subclass code reaches them, JLS 6.6.2.1), so the overrides of GtkView and
     * of any GtkWindow subclass run exactly as under JNI. Every target:
     * - catches every Throwable, because one that escapes an upcall stub terminates the JVM, and reports it as
     *   check_and_clear_exception (glass_general.cpp) reported it - reportException - then answers UPCALL_THREW, on
     *   which the C takes the early return or the continuation it took after a pending exception;
     * - answers a NullPointerException for the id 0, which the C passes where it called Java on a NULL jwindow or
     *   jview and HotSpot threw that exception for the NULL receiver (glass_gtk_api.h, IDENTITY);
     * - does nothing for an id no peer is registered under any more, and answers UPCALL_OK;
     * - writes its out-parameters only when the Java target returned normally.
     * They run on the thread that iterates GLib's default main context, and are re-entrant: a target may run a
     * nested main loop (the drop target's reads, enterNestedEventLoop) that dials any slot again.
     */

    /** {@code notify_screen_settings_changed}: {@code Screen.notifySettingsChanged()}, {@code LOG_EXCEPTION}. */
    static int onNotifyScreenSettingsChanged() {
        try {
            Screen.notifySettingsChanged();
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code get_application_name} ({@code glass_general.cpp}): {@code Application.GetApplication().getName()} as
     * modified UTF-8 in a {@code g_malloc}'d, NUL-terminated block that the C owns - the {@code GetStringUTFChars}
     * bytes the JNI copied with {@code g_strdup}. A {@code null} name leaves {@code *outName} {@code NULL}; a
     * {@code null} application is the {@code NullPointerException} {@code CallObjectMethod} raised for it.
     */
    static int onGetApplicationName(MemorySegment outName) {
        try {
            Application application = Application.GetApplication();
            if (application == null) {
                throw new NullPointerException();
            }
            String name = application.getName();
            if (name != null) {
                outName.set(ADDRESS, 0, gMallocModifiedUtf8(name));
            }
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code is_enabled}: {@code Window.isEnabled()}, 1 or 0. Hot: every input event asks it before it is
     * dispatched ({@code is_window_enabled_for_event}, {@code GlassApplication.cpp}); scalars only.
     */
    static int onIsEnabled(long windowId, MemorySegment outEnabled) {
        try {
            GtkWindow window = GtkWindow.peer(windowId);
            if (window != null) {
                outEnabled.set(JAVA_INT, 0, window.isEnabled() ? 1 : 0);
            }
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_state_changed}: {@code GtkWindow.notifyStateChanged(int)}. */
    static int onNotifyStateChanged(long windowId, int state) {
        try {
            GtkWindow.dispatchStateChanged(windowId, state);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_focus}: {@code Window.notifyFocus(int)}. */
    static int onNotifyFocus(long windowId, int event) {
        try {
            GtkWindow.dispatchFocus(windowId, event);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_focus_disabled}: {@code Window.notifyFocusDisabled()}. */
    static int onNotifyFocusDisabled(long windowId) {
        try {
            GtkWindow.dispatchFocusDisabled(windowId);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_focus_ungrab}: {@code Window.notifyFocusUngrab()}. */
    static int onNotifyFocusUngrab(long windowId) {
        try {
            GtkWindow.dispatchFocusUngrab(windowId);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code notify_destroy}: {@code Window.notifyDestroy()}, reported as {@code EXCEPTION_OCCURED} reported it, then
     * the registry entries go - where {@code WindowContextBase::process_destroy} deleted the global references of
     * the window and of its view, right after this call.
     */
    static int onNotifyDestroy(long windowId) {
        int status = UPCALL_OK;
        try {
            GtkWindow.dispatchDestroy(windowId);
        } catch (Throwable t) {
            reportException(t);
            status = UPCALL_THREW;
        }
        try {
            GtkWindow.releaseDestroyed(windowId);
        } catch (Throwable t) {
            reportException(t);
        }
        return status;
    }

    /** {@code notify_close}: {@code Window.notifyClose()}. */
    static int onNotifyClose(long windowId) {
        try {
            GtkWindow.dispatchClose(windowId);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_resize} of the window table: {@code Window.notifyResize(int, int, int)}. */
    static int onNotifyWindowResize(long windowId, int type, int width, int height) {
        try {
            GtkWindow.dispatchResize(windowId, type, width, height);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_move}: {@code Window.notifyMove(int, int)}. */
    static int onNotifyMove(long windowId, int x, int y) {
        try {
            GtkWindow.dispatchMove(windowId, x, y);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code notify_move_to_another_screen}: {@code Window.notifyMoveToAnotherScreen(Screen)} with the {@code Screen}
     * of monitor {@code monitorIndex} of the default {@code GdkScreen}, built by {@link #createScreen} - the
     * arithmetic of {@code createJavaScreen} ({@code glass_screen.cpp}), which the JNI ran here before the call.
     */
    static int onNotifyMoveToAnotherScreen(long windowId, int monitorIndex) {
        try {
            Screen screen = createScreen((MemorySegment) GDK_SCREEN_GET_DEFAULT.invokeExact(), monitorIndex);
            GtkWindow.dispatchMoveToAnotherScreen(windowId, screen);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_level_changed}: {@code Window.notifyLevelChanged(int)}. */
    static int onNotifyLevelChanged(long windowId, int level) {
        try {
            GtkWindow.dispatchLevelChanged(windowId, level);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code non_client_hit_test}: {@code GtkWindow.nonClientHitTest(int, int)}, the int unmodified. */
    static int onNonClientHitTest(long windowId, int x, int y, MemorySegment outResult) {
        try {
            GtkWindow window = GtkWindow.peer(windowId);
            if (window != null) {
                outResult.set(JAVA_INT, 0, GtkWindow.nonClientHitTest(window, x, y));
            }
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_view}: {@code View.notifyView(int)}. */
    static int onNotifyView(long viewId, int type) {
        try {
            GtkView.dispatchView(viewId, type);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_resize} of the view table: {@code View.notifyResize(int, int)}. */
    static int onNotifyViewResize(long viewId, int width, int height) {
        try {
            GtkView.dispatchResize(viewId, width, height);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_repaint}: {@code View.notifyRepaint(int, int, int, int)}. */
    static int onNotifyRepaint(long viewId, int x, int y, int width, int height) {
        try {
            GtkView.dispatchRepaint(viewId, x, y, width, height);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code notify_mouse}: {@code View.notifyMouse(int, int, int, int, int, int, int, boolean, boolean)}. Hot: one per
     * motion event; scalars only. The one call site that did not test the exception, the {@code EXIT} of
     * {@code WindowContextBase::set_view}, left it pending and {@code GtkWindow._setView} threw it after the C had
     * swapped the views: that call is recognised by {@link GtkWindow#claimSetViewExit}, and its exception is handed
     * back to the {@code _setView} in progress, which throws it, instead of being reported.
     */
    static int onNotifyMouse(long viewId, int type, int button, int x, int y, int xAbs, int yAbs, int modifiers,
                             int isPopupTrigger, int isSynthesized) {
        GtkWindow.SetViewCall setView = null;
        try {
            setView = GtkWindow.claimSetViewExit(type);
            GtkView.dispatchMouse(viewId, type, button, x, y, xAbs, yAbs, modifiers, isPopupTrigger != 0,
                    isSynthesized != 0);
            return UPCALL_OK;
        } catch (Throwable t) {
            if (setView != null) {
                setView.hold(t);
            } else {
                reportException(t);
            }
            return UPCALL_THREW;
        }
    }

    /** {@code notify_menu}: {@code View.notifyMenu(int, int, int, int, boolean)}, which {@code GtkView} overrides. */
    static int onNotifyMenu(long viewId, int x, int y, int xAbs, int yAbs, int isKeyboardTrigger) {
        try {
            GtkView.dispatchMenu(viewId, x, y, xAbs, yAbs, isKeyboardTrigger != 0);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_scroll}: {@code View.notifyScroll(...)}, the thirteen values as the C passed them. */
    static int onNotifyScroll(long viewId, int x, int y, int xAbs, int yAbs, double deltaX, double deltaY,
                              int modifiers, int lines, int chars, int defaultLines, int defaultChars,
                              double xMultiplier, double yMultiplier) {
        try {
            GtkView.dispatchScroll(viewId, x, y, xAbs, yAbs, deltaX, deltaY, modifiers, lines, chars, defaultLines,
                    defaultChars, xMultiplier, yMultiplier);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * The {@code char[]} {@code notify_key} built for the {@code PRESS} that returned last, and the address of the
     * code unit it was built from: {@code WindowContextBase::process_key} passed the same {@code jcharArray} to the
     * {@code PRESS} and to the {@code TYPED} that follows it, and hands the table the same {@code &key} for both. Only
     * touched on the thread that iterates the main context.
     */
    private static char[] pressedKeyChars;
    private static long pressedKeyAddress;

    /**
     * {@code notify_key}: {@code View.notifyKey(int, int, char[], int)} with the {@code keyCharCount} (0 or 1) UTF-16
     * code units at {@code keyChars} copied as {@code char}s - lone surrogates included, never a charset decode -
     * into a {@code char[]} of that length, never {@code null}. The {@code TYPED} that follows a {@code PRESS} gets
     * the very array that {@code PRESS} got, as under JNI; a nested key event dispatched inside the {@code PRESS}
     * records its own array only while it runs, so the outer pair still shares one.
     */
    static int onNotifyKey(long viewId, int type, int keyCode, MemorySegment keyChars, int keyCharCount,
                           int modifiers) {
        try {
            char[] chars;
            if (type == KeyEvent.TYPED && pressedKeyChars != null && pressedKeyChars.length == keyCharCount
                    && pressedKeyAddress == keyChars.address()) {
                chars = pressedKeyChars;
            } else {
                chars = utf16Units(keyChars, keyCharCount);
            }
            pressedKeyChars = null;
            try {
                GtkView.dispatchKey(viewId, type, keyCode, chars, modifiers);
            } finally {
                if (type == KeyEvent.PRESS) {
                    pressedKeyChars = chars;
                    pressedKeyAddress = keyChars.address();
                }
            }
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code notify_input_method_preedit} ({@code on_preedit_changed}, {@code glass_window_ime.cpp}):
     * {@code GtkView.notifyInputMethodLinux(text, 0, cursorPos, (byte) attr)}, the text decoded from the
     * {@code textLen} bytes as {@code NewStringUTF} decoded them; {@code NULL} is a {@code null} string.
     */
    static int onNotifyInputMethodPreedit(long viewId, MemorySegment text, int textLen, int cursorPos, int attr) {
        try {
            GtkView.dispatchInputMethod(viewId, newStringUtf(text, textLen), 0, cursorPos, (byte) attr);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code notify_input_method_commit} ({@code WindowContextBase::commitIME}): the text decoded as
     * {@code NewStringUTF} decoded it, then {@code GtkView.notifyInputMethodLinux(text, n, n, (byte) 0)} with
     * {@code n} its UTF-16 length, the {@code GetStringLength} of the JNI.
     */
    static int onNotifyInputMethodCommit(long viewId, MemorySegment text, int textLen) {
        try {
            String committed = newStringUtf(text, textLen);
            int length = committed.length();
            GtkView.dispatchInputMethod(viewId, committed, length, length, (byte) 0);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code notify_input_method_candidate_pos_request} ({@code WindowContextBase::updateCaretPos}):
     * {@code GtkView.notifyInputMethodCandidateRelativePosRequest(int)}; elements 0 and 1 of the array it returns
     * go to {@code outXy} and 1 to {@code outValid}. A {@code null} array writes nothing, which makes the C skip
     * {@code gtk_im_context_set_cursor_location}; there, and after a throw, the JNI's
     * {@code GetDoubleArrayElements(NULL)} crashed the JVM (glass_gtk_api.h).
     */
    static int onNotifyInputMethodCandidatePosRequest(long viewId, int offset, MemorySegment outXy,
                                                      MemorySegment outValid) {
        try {
            double[] position = GtkView.dispatchCandidatePosRequest(viewId, offset);
            if (position != null) {
                double x = position[0];
                double y = position[1];
                outXy.setAtIndex(JAVA_DOUBLE, 0, x);
                outXy.setAtIndex(JAVA_DOUBLE, 1, y);
                outValid.set(JAVA_INT, 0, 1);
            }
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_drag_enter}: {@code View.notifyDragEnter(int, int, int, int, int)}, the action it returns. */
    static int onNotifyDragEnter(long viewId, int x, int y, int xAbs, int yAbs, int recommendedAction,
                                 MemorySegment outAction) {
        try {
            Integer action = GtkView.dispatchDragEnter(viewId, x, y, xAbs, yAbs, recommendedAction);
            if (action != null) {
                outAction.set(JAVA_INT, 0, action);
            }
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_drag_over}: {@code View.notifyDragOver(int, int, int, int, int)}, the action it returns. */
    static int onNotifyDragOver(long viewId, int x, int y, int xAbs, int yAbs, int recommendedAction,
                                MemorySegment outAction) {
        try {
            Integer action = GtkView.dispatchDragOver(viewId, x, y, xAbs, yAbs, recommendedAction);
            if (action != null) {
                outAction.set(JAVA_INT, 0, action);
            }
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code notify_drag_drop}: {@code View.notifyDragDrop(int, int, int, int, int)}, its result discarded. The drop
     * target's {@code GtkDnDClipboard} reads run nested main loops inside it.
     */
    static int onNotifyDragDrop(long viewId, int x, int y, int xAbs, int yAbs, int recommendedAction) {
        try {
            GtkView.dispatchDragDrop(viewId, x, y, xAbs, yAbs, recommendedAction);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code notify_drag_leave}: {@code View.notifyDragLeave()}. */
    static int onNotifyDragLeave(long viewId) {
        try {
            GtkView.dispatchDragLeave(viewId);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code source_get_data}: the value the data map of the drag in progress
     * ({@link GtkDnDClipboard#dragInProgress}) holds for the key, converted as {@code as} says - the
     * {@code Map.get} of {@code dnd_source_get_data} ({@code glass_dnd.cpp}) together with what its callers did with
     * the {@code jobject}. The key is decoded from the {@code mimeLen} bytes as {@code NewStringUTF} decoded it. The
     * C zero-filled {@code out}, so "no value" is {@code DND_DATA_NONE} without a write; the blocks written to
     * {@code data} are {@code g_malloc}'d and owned by the C.
     * <ul>
     * <li>{@code Map.get} threw: reported ({@code EXCEPTION_OCCURED}), kind {@code NONE}, {@code UPCALL_THREW};</li>
     * <li>no value, or one the conversion does not take: kind {@code NONE}, {@code UPCALL_OK};</li>
     * <li>the conversion threw ({@code ByteBuffer.array()}, {@code Pixels.attachData}): reported, the value's kind
     * with {@code data} {@code NULL}, {@code UPCALL_THREW}.</li>
     * </ul>
     */
    static int onSourceGetData(MemorySegment mime, int mimeLen, int as, MemorySegment out) {
        try {
            Object value;
            try {
                String key = newStringUtf(mime, mimeLen);
                HashMap<String, Object> data = GtkDnDClipboard.dragInProgress();
                if (data == null) {
                    throw new NullPointerException();
                }
                value = data.get(key);
            } catch (Throwable t) {
                reportException(t);
                return UPCALL_THREW;
            }
            return switch (as) {
                case DND_AS_STRING -> value instanceof String s ? putString(out, s) : UPCALL_OK;
                case DND_AS_BYTES -> value instanceof ByteBuffer b ? putBytes(out, b) : UPCALL_OK;
                case DND_AS_PIXBUF -> value instanceof Pixels p ? putPixbuf(out, p) : UPCALL_OK;
                case DND_AS_STRINGS -> value instanceof String[] a ? putStrings(out, a) : UPCALL_OK;
                case DND_AS_RAW -> value instanceof String s ? putString(out, s)
                        : value instanceof ByteBuffer b ? putBytes(out, b) : UPCALL_OK;
                default -> UPCALL_OK;
            };
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /** {@code GetStringUTFChars}: the modified UTF-8 of {@code s}, NUL-terminated, as a {@code STRING}. */
    private static int putString(MemorySegment out, String s) throws Throwable {
        byte[] bytes = JniStringCodec.toModifiedUtf8(s);
        out.set(ADDRESS, DND_DATA_DATA_OFFSET, gMalloc(bytes));
        out.set(JAVA_INT, DND_DATA_COUNT_OFFSET, bytes.length - 1);
        out.set(JAVA_INT, DND_DATA_KIND_OFFSET, DND_DATA_STRING);
        return UPCALL_OK;
    }

    /**
     * {@code ByteBuffer.array()} + {@code GetByteArrayElements}: the whole backing array, position and limit ignored,
     * as {@code BYTES}. A throw of {@code array()} - a read-only or a direct buffer - is reported and leaves
     * {@code BYTES} with no data.
     */
    private static int putBytes(MemorySegment out, ByteBuffer buffer) throws Throwable {
        out.set(JAVA_INT, DND_DATA_KIND_OFFSET, DND_DATA_BYTES);
        byte[] array;
        try {
            array = buffer.array();
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
        out.set(ADDRESS, DND_DATA_DATA_OFFSET, gMalloc(array));
        out.set(JAVA_INT, DND_DATA_COUNT_OFFSET, array.length);
        return UPCALL_OK;
    }

    /**
     * {@code Pixels.attachData(&pixbuf)}, with the address of {@code out->pixbuf} as the JNI passed the address of
     * its local, so a pixbuf written before a throw is what the C sees; a throw is reported.
     */
    private static int putPixbuf(MemorySegment out, Pixels pixels) {
        out.set(JAVA_INT, DND_DATA_KIND_OFFSET, DND_DATA_PIXBUF);
        try {
            PIXELS_ATTACH_DATA.invokeExact(pixels, out.address() + DND_DATA_PIXBUF_OFFSET);
            return UPCALL_OK;
        } catch (Throwable t) {
            reportException(t);
            return UPCALL_THREW;
        }
    }

    /**
     * {@code GetArrayLength} + {@code GetStringUTFChars} of every element: the strings as modified UTF-8, each
     * NUL-terminated, back to back, as {@code STRINGS}. A {@code null} element, on which the JNI's
     * {@code GetStringUTFChars} crashed, is reported as the {@code NullPointerException} it is here and delivers no
     * strings.
     */
    private static int putStrings(MemorySegment out, String[] strings) throws Throwable {
        ByteArrayOutputStream block = new ByteArrayOutputStream();
        for (String s : strings) {
            if (s == null) {
                reportException(new NullPointerException());
                return UPCALL_THREW;
            }
            block.writeBytes(JniStringCodec.toModifiedUtf8(s));
        }
        out.set(ADDRESS, DND_DATA_DATA_OFFSET, gMalloc(block.toByteArray()));
        out.set(JAVA_INT, DND_DATA_COUNT_OFFSET, strings.length);
        out.set(JAVA_INT, DND_DATA_KIND_OFFSET, DND_DATA_STRINGS);
        return UPCALL_OK;
    }

    /**
     * {@code bytes} in a {@code g_malloc}'d block, which the C owns and {@code g_free}s: never {@code NULL}, one zero
     * byte for an empty array.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment gMalloc(byte[] bytes) throws Throwable {
        int size = Math.max(1, bytes.length);
        MemorySegment block = ((MemorySegment) G_MALLOC.invokeExact((long) size)).reinterpret(size);
        block.set(JAVA_BYTE, 0, (byte) 0);
        MemorySegment.copy(bytes, 0, block, JAVA_BYTE, 0, bytes.length);
        return block;
    }

    /** {@code GetStringUTFChars} + {@code g_strdup}: {@code s} as modified UTF-8 in a {@code g_malloc}'d C string. */
    private static MemorySegment gMallocModifiedUtf8(String s) throws Throwable {
        return gMalloc(JniStringCodec.toModifiedUtf8(s));
    }

    /**
     * {@code NewStringUTF} of the {@code length} bytes at {@code text} (no terminator counted): {@code null} for
     * {@code NULL}, what {@code NewStringUTF} answered for one.
     */
    @SuppressWarnings("restricted")
    private static String newStringUtf(MemorySegment text, int length) {
        if (text.address() == 0) {
            return null;
        }
        return JniStringCodec.fromNewStringUtf(text.reinterpret(length).toArray(JAVA_BYTE));
    }

    /** {@code count} UTF-16 code units at {@code units}, copied as {@code char}s; empty for a count of 0. */
    @SuppressWarnings("restricted")
    private static char[] utf16Units(MemorySegment units, int count) {
        char[] chars = new char[count];
        if (count > 0) {
            MemorySegment.copy(units.reinterpret(count * JAVA_CHAR_UNALIGNED.byteSize()), JAVA_CHAR_UNALIGNED, 0,
                    chars, 0, count);
        }
        return chars;
    }

    /** The offset of the field {@code name} of {@code GgtkDndData}. */
    private static long dndDataOffset(String name) {
        return GGTK_DND_DATA_LAYOUT.byteOffset(PathElement.groupElement(name));
    }

    /** A pointer whose target is {@code target}, for an out-parameter of a slot. */
    @SuppressWarnings("restricted")
    private static AddressLayout pointerTo(MemoryLayout target) {
        return ADDRESS.withTargetLayout(target);
    }

    /* ---------------------------------------------------------------------------------------------------------
     * The query for the glass GTK library: GtkApplication._queryLibrary (launcher.c, the whole of libglass.so)
     * ------------------------------------------------------------------------------------------------------- */

    /**
     * Which glass GTK library the toolkit must load, and what has to happen to the process before it can:
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary} of {@code launcher.c} - the whole of
     * {@code libglass.so} at commit {@code 033187ad90} - and the copy of it in {@code GlassApplication.cpp} that
     * answered when a {@code libglassgtk3.so} build had been renamed to {@code libglass.so}.
     * <p>
     * A holder of its own, initialized by the first query and by nothing else: it opens libc and
     * {@code libX11.so.6} and no more, so that a machine with no display still fails with the
     * {@code UnsupportedOperationException} of a display that will not open, where initializing the enclosing
     * class - which opens the GTK, GDK, GLib, GIO and XTest libraries - would fail first and differently. The
     * enclosing class is initialized where it always was, by {@code GtkApplication} after this query
     * ({@link GtkGlassNative#link}); that is why this one has a linker, lookups and a symbol census of its own and
     * reads no field and calls no method of the enclosing class.
     * <p>
     * {@code dlopen} is called through libc and not through {@link SymbolLookup#libraryLookup}, because the two
     * flags that carry the behaviour cannot be expressed there: {@code RTLD_NOLOAD}, which asks whether a library
     * is already in the process without loading it, and {@code RTLD_GLOBAL}, which puts the GTK library and its
     * dependencies into the global scope - where the {@code dlsym(RTLD_DEFAULT, ...)} of {@code wrapped.c} looks
     * for {@code gdk_x11_display_set_window_scale} and the four GSettings functions.
     */
    static final class Loader {

        /** {@code RTLD_LAZY} of {@code bits/dlfcn.h}: resolve a symbol when it is first called. */
        private static final int RTLD_LAZY = 0x00001;

        /** {@code RTLD_NOLOAD}: answer the handle of a library the process has, and load nothing. */
        private static final int RTLD_NOLOAD = 0x00004;

        /** {@code RTLD_GLOBAL}: add the library and its dependencies to the global scope of the process. */
        private static final int RTLD_GLOBAL = 0x00100;

        /**
         * The answers of {@link #queryLibrary}, the {@code GGTK_QUERY_*} of
         * {@code native-glass/gtk/glass_gtk_api.h}: no GTK 3 library could be loaded, or GTK 2 is already in the
         * process.
         */
        static final int QUERY_ERROR = -2;

        /** {@code XOpenDisplay(NULL)} failed. */
        static final int QUERY_NO_DISPLAY = -1;

        /** The library loaded as {@code glass} is itself the glass GTK library; load none. */
        static final int QUERY_USE_CURRENT = 1;

        /** A GTK 3 library is in the process; load {@code libglassgtk3.so}. */
        static final int QUERY_LOAD_GTK3 = 3;

        /** {@code gtk3_lib_options} of {@code launcher.c}, in its order. */
        private static final String[] GTK3_LIB_OPTIONS = {"libgtk-3.so.0", "libgtk-3.so"};

        /** {@code gtk2_lib_options} of {@code launcher.c}: a GTK 2 already in the process is an error. */
        private static final String[] GTK2_LIB_OPTIONS = {"libgtk-x11-2.0.so.0", "libgtk-x11-2.0.so"};

        /** The version of the GTK 3 chain, {@code gtk3_versioned[0]} of {@code launcher.c}. */
        private static final String GTK3_VERSION = "3";

        private static final Linker LINKER = Linker.nativeLinker();

        /** {@code library!symbol} for every symbol this class binds, in binding order; read by tests. */
        private static final Map<String, Long> BOUND = Collections.synchronizedMap(new LinkedHashMap<>());

        /** {@code void *dlopen(const char *file, int mode)}. */
        private static final MethodHandle DLOPEN = bind(LINKER.defaultLookup(), LIB_C, "dlopen",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));

        /** {@code int putenv(char *string)}, which keeps the pointer it is given. */
        private static final MethodHandle PUTENV = bind(LINKER.defaultLookup(), LIB_C, "putenv",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        private static final SymbolLookup X11 = x11();

        /** {@code Display *XOpenDisplay(const char *display_name)}. */
        private static final MethodHandle X_OPEN_DISPLAY = bind(X11, LIB_X11, "XOpenDisplay",
                FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code int XCloseDisplay(Display *display)}. */
        private static final MethodHandle X_CLOSE_DISPLAY = bind(X11, LIB_X11, "XCloseDisplay",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /**
         * The string of {@code putenv("GDK_BACKEND=x11")}. {@code putenv} keeps the pointer it is given - the C
         * passed a string literal of {@code libglass.so} - so this block is allocated in {@link Arena#global()}
         * and never freed.
         */
        private static final MemorySegment GDK_BACKEND_X11 = Arena.global().allocateFrom("GDK_BACKEND=x11");

        /**
         * Whether the library the class loader loaded as {@code glass} is itself a glass GTK library: the
         * deployment {@code GlassApplication.cpp} described as "renaming the gtk versioned native libraries to be
         * libglass.so", whose {@code _queryLibrary} answered {@code QUERY_USE_CURRENT} because the library that
         * answered was the one to use. Decided once, when this class initializes - which is the toolkit's query,
         * before it loads {@code glassgtk3} - because every process that has loaded a glass GTK library exports
         * these symbols afterwards.
         */
        private static final boolean GLASS_IS_THE_GTK_LIBRARY =
                SymbolLookup.loaderLookup().find("ggtk_abi_version").isPresent();

        private Loader() {
        }

        /**
         * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary}, the two of them: the copy in
         * {@code GlassApplication.cpp} when the glass library the process loaded is a glass GTK library itself -
         * which only opens and closes the display, sets no environment variable and prints nothing - and
         * otherwise the launcher's, which sets {@code GDK_BACKEND=x11} on every system, checks the display and
         * then looks for a GTK library.
         *
         * @param version the GTK version asked for, {@code jdk.gtk.version}
         * @param verbose {@code jdk.gtk.verbose}: print what the launcher printed at each step
         * @return one of {@link #QUERY_ERROR}, {@link #QUERY_NO_DISPLAY}, {@link #QUERY_USE_CURRENT},
         *         {@link #QUERY_LOAD_GTK3}
         */
        static int queryLibrary(int version, boolean verbose) {
            if (GLASS_IS_THE_GTK_LIBRARY) {
                return displayOpens() ? QUERY_USE_CURRENT : QUERY_NO_DISPLAY;
            }
            // Set the gtk backend to x11 on all the systems
            try {
                int ignored = (int) PUTENV.invokeExact(GDK_BACKEND_X11);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            // Before doing anything with GTK the C validated that the DISPLAY can be opened
            if (!displayOpens()) {
                return QUERY_NO_DISPLAY;
            }
            return sniffLibs(version, verbose, GTK3_LIB_OPTIONS, GTK2_LIB_OPTIONS) == '3'
                    ? QUERY_LOAD_GTK3 : QUERY_ERROR;
        }

        /** {@code XOpenDisplay(NULL)} and, if it answered a display, {@code XCloseDisplay}. */
        private static boolean displayOpens() {
            try {
                MemorySegment display = (MemorySegment) X_OPEN_DISPLAY.invokeExact(MemorySegment.NULL);
                if (display.address() == 0) {
                    return false;
                }
                int ignored = (int) X_CLOSE_DISPLAY.invokeExact(display);
                return true;
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }

        /**
         * {@code sniffLibs} of {@code launcher.c}: first whether a GTK 3 library of {@code gtk3} is already in the
         * process, then whether a GTK 2 library of {@code gtk2} is - which is the one failure this answers - and
         * only then an attempt to load the GTK 3 libraries in order.
         * <p>
         * The two chains are parameters, where the C read its two file-static arrays, so that a test can drive
         * every branch of this table on a machine that has no GTK 2 library to load.
         *
         * @return the first character of the version of the chain that was found, {@code '3'}, or -1
         */
        static int sniffLibs(int wantVersion, boolean verbose, String[] gtk3, String[] gtk2) {
            if (verbose) {
                printLine("checking GTK version " + wantVersion);
            }
            boolean found = false;
            int i;
            // at first try to detect already loaded GTK version
            for (i = 0; i < gtk3.length && !found; i++) {
                found = tryLibrariesNoload(gtk3[i]);
                if (found && verbose) {
                    printLine("found already loaded GTK library " + gtk3[i]);
                }
            }
            if (!found) {
                for (i = 0; i < gtk2.length && !found; i++) {
                    found = tryLibrariesNoload(gtk2[i]);
                    if (found && verbose) {
                        printLine("found already loaded unsupported GTK library " + gtk2[i]);
                    }
                }
                if (found) {
                    return -1;
                }
                if (wantVersion != 0 && wantVersion != 3) {
                    // Note, this should never happen, java should be protecting us
                    if (verbose) {
                        printLine("bad GTK version specified, assuming 3");
                    }
                    // the C corrected its own local here and never read it again; kept so the branch reads as it did
                    wantVersion = 3;
                }
                for (i = 0; i < gtk3.length && !found; i++) {
                    if (verbose) {
                        printLine("trying GTK library " + gtk3[i]);
                    }
                    found = tryOpeningLibraries(gtk3[i]);
                }
            }
            if (found) {
                if (verbose) {
                    i--;
                    printLine("using GTK library version " + GTK3_VERSION + " set " + gtk3[i]);
                }
                return GTK3_VERSION.charAt(0);
            }
            return -1;
        }

        /**
         * {@code try_opening_libraries} of {@code launcher.c}: {@code dlopen(name, RTLD_LAZY | RTLD_GLOBAL)}. The
         * handle is dropped without {@code dlclose}, as the C dropped it: the library stays for the process.
         */
        static boolean tryOpeningLibraries(String name) {
            return dlopen(name, RTLD_LAZY | RTLD_GLOBAL);
        }

        /**
         * {@code try_libraries_noload} of {@code launcher.c}: {@code dlopen(name, RTLD_LAZY | RTLD_NOLOAD)},
         * which answers a handle only for a library the process has loaded already.
         */
        static boolean tryLibrariesNoload(String name) {
            return dlopen(name, RTLD_LAZY | RTLD_NOLOAD);
        }

        private static boolean dlopen(String name, int flags) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment handle = (MemorySegment) DLOPEN.invokeExact(arena.allocateFrom(name), flags);
                return handle.address() != 0;
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }

        /**
         * One {@code printf} of {@code launcher.c} under {@code gtk_versionDebug}. The newline is the C's; the
         * stream flushes it, where the C flushed {@code stdout} once {@code sniffLibs} was done. It is
         * {@link System#out}, the field, so a {@code System.setOut} takes these lines where the C always wrote
         * file descriptor 1 - the stream {@code GtkApplication}'s own "Glass GTK library to load is" line has
         * always used.
         */
        private static void printLine(String line) {
            System.out.print(line + "\n");
        }

        /** {@code library!symbol} of every symbol this class bound, in binding order. */
        static List<String> boundSymbols() {
            synchronized (BOUND) {
                return new ArrayList<>(BOUND.keySet());
            }
        }

        @SuppressWarnings("restricted")
        private static SymbolLookup x11() {
            try {
                return SymbolLookup.libraryLookup(LIB_X11, Arena.global());
            } catch (IllegalArgumentException e) {
                UnsatisfiedLinkError error = new UnsatisfiedLinkError("cannot open " + LIB_X11 + ": "
                        + e.getMessage());
                error.initCause(e);
                throw error;
            }
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bind(SymbolLookup lookup, String library, String name,
                                         FunctionDescriptor descriptor) {
            MemorySegment symbol = lookup.find(name).orElseThrow(
                    () -> new UnsatisfiedLinkError(library + " does not export " + name));
            BOUND.put(library + "!" + name, symbol.address());
            return LINKER.downcallHandle(symbol, descriptor);
        }

        private static RuntimeException unexpected(Throwable t) {
            if (t instanceof RuntimeException e) {
                return e;
            }
            if (t instanceof Error e) {
                throw e;
            }
            return new IllegalStateException(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Support
     * ------------------------------------------------------------------------------------------------------- */

    /**
     * {@code check_and_clear_exception} ({@code glass_general.cpp}): {@code Application.reportException(t)} on the
     * current thread, and a second {@code ExceptionClear} that swallowed whatever the report itself threw.
     */
    static void reportException(Throwable t) {
        try {
            Application.reportException(t);
        } catch (Throwable ignored) {
            // what the C's second ExceptionClear did
        }
    }

    /** {@code library!symbol} of every bound symbol, in binding order. */
    static List<String> boundSymbols() {
        synchronized (BOUND) {
            return new ArrayList<>(BOUND.keySet());
        }
    }

    /** The address {@code library!symbol} was bound at, or 0 if this class did not bind it. */
    static long boundAddress(String qualifiedName) {
        Long address = BOUND.get(qualifiedName);
        return address == null ? 0L : address;
    }

    @SuppressWarnings("restricted")
    private static SymbolLookup library(String soname) {
        try {
            return SymbolLookup.libraryLookup(soname, Arena.global());
        } catch (IllegalArgumentException e) {
            UnsatisfiedLinkError error = new UnsatisfiedLinkError("cannot open " + soname + ": " + e.getMessage());
            error.initCause(e);
            throw error;
        }
    }

    /** Resolves {@code name} in {@code lookup} and links it as an ordinary (never critical) downcall. */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(SymbolLookup lookup, String library, String name, FunctionDescriptor descriptor,
                                     Linker.Option... options) {
        MemorySegment symbol = lookup.find(name).orElseThrow(
                () -> new UnsatisfiedLinkError(library + " does not export " + name));
        BOUND.put(library + "!" + name, symbol.address());
        return LINKER.downcallHandle(symbol, descriptor, options);
    }

    /** Resolves {@code name} in {@code lookup} as a plain address, for a function pointer handed to C. */
    private static MemorySegment address(SymbolLookup lookup, String library, String name) {
        MemorySegment symbol = lookup.find(name).orElseThrow(
                () -> new UnsatisfiedLinkError(library + " does not export " + name));
        BOUND.put(library + "!" + name, symbol.address());
        return symbol;
    }

    /** Resolves {@code name} in {@code lookup} as a plain address, or {@code NULL} when the library lacks it. */
    private static MemorySegment optional(SymbolLookup lookup, String library, String name) {
        MemorySegment symbol = lookup.find(name).orElse(MemorySegment.NULL);
        if (symbol.address() != 0) {
            BOUND.put(library + "!" + name, symbol.address());
        }
        return symbol;
    }

    /** The {@code stderr} variable of libc, as an 8-byte segment holding the {@code FILE *}. */
    @SuppressWarnings("restricted")
    private static MemorySegment stderrVariable() {
        MemorySegment symbol = LIBC.find("stderr").orElseThrow(
                () -> new UnsatisfiedLinkError(LIB_C + " does not export stderr"));
        BOUND.put(LIB_C + "!stderr", symbol.address());
        return symbol.reinterpret(ADDRESS.byteSize());
    }

    /** A downcall handle whose target address is its first argument, for symbols that may be absent. */
    @SuppressWarnings("restricted")
    private static MethodHandle addresslessDowncall(FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(descriptor);
    }

    /** An upcall stub in {@link Arena#global()} for the static method {@code name} of this class. */
    @SuppressWarnings("restricted")
    private static MemorySegment upcallStub(String name, FunctionDescriptor descriptor) {
        try {
            MethodHandle target = MethodHandles.lookup().findStatic(GtkGlassNative.class, name,
                    descriptor.toMethodType());
            return LINKER.upcallStub(target, descriptor, Arena.global());
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Throws {@code t} unchanged, checked or not: what a JNI native method did with an exception that native code
     * left pending when it returned.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException rethrow(Throwable t) throws T {
        throw (T) t;
    }

    /**
     * What to throw for a {@code Throwable} caught around the downcalls of this class: a {@link RuntimeException}
     * or an {@link Error} as it is, anything else wrapped in an {@link IllegalStateException}.
     * <p>
     * Such a {@code Throwable} is a failure to link or invoke the handle, or an exception the method itself throws
     * inside its {@code try} block on purpose - the {@code IllegalStateException} of {@link #dndMimesFromSystem} and
     * {@link #dndPopFromSystem} outside a drag - which passes through unchanged. Nothing else can come out of a
     * downcall: the functions that run a nested GTK main loop or call Java otherwise ({@code gtk_main}, the
     * {@code gtk_clipboard_wait_for_*} calls, {@code gtk_native_dialog_run} and the {@code ggtk_*} functions marked
     * UPCALLS) reach Java only through upcall stubs, which let nothing out. Where the C went on to free what it had
     * allocated, the {@code finally} blocks around those calls free the same things if one is thrown all the same.
     */
    private static RuntimeException unexpected(Throwable t) {
        if (t instanceof RuntimeException e) {
            return e;
        }
        if (t instanceof Error e) {
            throw e;
        }
        return new IllegalStateException(t);
    }
}
