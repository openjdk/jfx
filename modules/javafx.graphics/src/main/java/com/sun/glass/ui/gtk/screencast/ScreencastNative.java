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

package com.sun.glass.ui.gtk.screencast;

import com.sun.javafx.font.JniStringCodec;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The screen capture and remote desktop calls of {@link ScreencastHelper} that used to sit behind JNI in
 * {@code libglassgtk3.so}, bound from Java with {@code java.lang.foreign}. Every method here replaces the body of
 * a {@code Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_*} function of commit {@code 033187ad90}, whose C
 * did nothing but convert JNI arguments and call the work: that work - the xdg-desktop-portal, D-Bus and PipeWire
 * code of {@code native-glass/gtk/screencast_pipewire.c} and {@code screencast_portal.c} - stays in the library
 * and is reached through its flat C ABI, {@code native-glass/gtk/screencast_api.h}. Its javadoc names the function
 * each method calls and the native it stands for.
 * <p>
 * A class of its own, next to the helper it serves: {@code com.sun.glass.ui.gtk.GtkGlassNative}, which binds the
 * other side of the GTK glass, is package-private in its own package and cannot be reached from here, and
 * {@code screencast_api.h} is a C ABI of its own, with its own version, status codes and callback table. The
 * shape is that facade's: a lazy holder for the exports of the library, upcall stubs in {@link Arena#global()},
 * and the string codec the JNI's {@code GetStringUTFChars} and {@code NewStringUTF} define.
 *
 * <h2>The library</h2>
 * The {@code sc_*} exports are resolved through {@link SymbolLookup#loaderLookup()} in {@link Lib}, a holder of
 * its own, so that this class can be loaded - and its token callback driven - without {@code libglassgtk3.so}:
 * initializing {@link Lib} needs the library, and {@code sc_abi_version} is checked before anything else of it is
 * bound. The library is in the process because {@code GtkApplication} loaded it at toolkit start, which is the
 * only way {@code ScreencastHelper}'s natives resolved at commit {@code 033187ad90} either.
 *
 * <h2>Threads, blocking and {@code critical}</h2>
 * Every function of {@code screencast_api.h} but the version and {@code sizeof} probes talks D-Bus to the desktop
 * portal, runs a nested GTK main loop or waits on the PipeWire thread loop, so it BLOCKS - for as long as the
 * portal takes, a user's consent dialog included. None of these downcalls is therefore
 * {@link Linker.Option#critical critical}: a critical downcall keeps its thread in Java for the whole call, and
 * every safepoint of the VM would wait for a portal round trip. The pixels of {@code getRGBPixelsImpl} and the
 * screen bounds are copied into native memory for the call and copied back afterwards, in pieces
 * ({@link #CHUNK_BYTES}) so that a safepoint requested during a copy waits for one piece at most.
 * {@code ScreencastHelper} serialises the calls, as it serialised the natives.
 *
 * <h2>The restore token</h2>
 * The one call from the C into Java - {@code TokenStorage.storeTokenFromNative} with the restore token the portal
 * answered - is an upcall stub of this class, installed in the library's token callback table
 * ({@link #installTokenCallbacks}) where the JNI cached a class reference and a method id. The stub lets no
 * {@code Throwable} out - that would terminate the JVM - and reports one exactly as the
 * {@code EXCEPTION_CHECK_DESCRIBE} of {@code screencast_pipewire.c} did: the description on the error stream, and
 * {@code SC_UPCALL_THREW} back to the C, which ignores it as it ignored the exception that macro had cleared.
 * <p>
 * <b>Declared behaviour difference.</b> Commit {@code 033187ad90} asked the JVM for the {@code JNIEnv} of the
 * thread the portal answered on and, when that thread was not attached to the JVM, dropped the token. An upcall
 * stub has no such branch: a call from a thread the JVM has never seen attaches it for the call, so a token that
 * used to be lost is now stored, and {@code TokenStorage.storeTokenFromNative} - which takes a lock and writes a
 * file - can run on a thread the JVM created for the call, whose context class loader is {@code null}.
 */
final class ScreencastNative {

    /** {@code GLASS_SCREENCAST_ABI_VERSION} of {@code screencast_api.h}; {@code sc_abi_version} must answer it. */
    static final int ABI_VERSION = 1;

    /** The label of the symbols of the glass GTK library in {@link #boundSymbols}. */
    static final String LIB_GLASS = "libglassgtk3.so";

    /** The C library, through {@link Linker#defaultLookup()}; the name only labels its symbols. */
    static final String LIB_C = "libc.so.6";

    /** {@code SC_OK}: what the installer of the callback table answers. */
    static final int OK = 0;

    /** {@code SC_UPCALL_OK}: the Java target returned normally. */
    static final int UPCALL_OK = 0;

    /** {@code SC_UPCALL_THREW}: the Java target threw, and the exception was described as the C described it. */
    static final int UPCALL_THREW = 1;

    /** {@code SC_METHOD_SCREENCAST}: {@code ScreencastHelper}'s {@code XDG_METHOD_SCREENCAST}. */
    static final int METHOD_SCREENCAST = 0;

    /** {@code SC_METHOD_REMOTE_DESKTOP}: {@code ScreencastHelper}'s {@code XDG_METHOD_REMOTE_DESKTOP}. */
    static final int METHOD_REMOTE_DESKTOP = 1;

    /** {@code SC_RESULT_OK}: the action was carried out. */
    static final int RESULT_OK = 0;

    /** {@code SC_RESULT_ERROR}: {@code ScreencastHelper}'s {@code ERROR}. */
    static final int RESULT_ERROR = -1;

    /** {@code SC_RESULT_DENIED}: {@code ScreencastHelper}'s {@code DENIED}. */
    static final int RESULT_DENIED = -11;

    /** {@code SC_RESULT_OUT_OF_BOUNDS}: {@code ScreencastHelper}'s {@code OUT_OF_BOUNDS}. */
    static final int RESULT_OUT_OF_BOUNDS = -12;

    /** {@code SC_RESULT_NO_STREAMS}: {@code ScreencastHelper}'s {@code NO_STREAMS}. */
    static final int RESULT_NO_STREAMS = -13;

    /**
     * The most bytes copied between two safepoint polls, as the frame uploads of the GTK glass use: a capture of a
     * large screen is megabytes, and one {@code MemorySegment.copy} of all of it would keep its thread from a
     * safepoint for the whole copy.
     */
    static final long CHUNK_BYTES = 1L << 18;

    /**
     * {@code int32_t (*store_token)(const char *old_token, const char *new_token, const int32_t *bounds,
     * int32_t bounds_len)}, the one slot of {@code ScTokenCallbacks}.
     */
    static final FunctionDescriptor STORE_TOKEN = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS,
            JAVA_INT);

    /** {@code struct ScTokenCallbacks}: one function pointer. */
    static final StructLayout TOKEN_CALLBACKS_LAYOUT = MemoryLayout.structLayout(ADDRESS.withName("store_token"));

    private static final Linker LINKER = Linker.nativeLinker();

    /** {@code library!symbol} for every symbol bound, in binding order, with its address; read by tests. */
    private static final Map<String, Long> BOUND = Collections.synchronizedMap(new LinkedHashMap<>());

    /** {@code size_t strlen(const char *s)}: the length of a C string the library hands a slot. */
    private static final MethodHandle STRLEN = bind(LINKER.defaultLookup(), LIB_C, "strlen",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    /** The stub of {@link #onStoreToken}, created once in {@link Arena#global()}; see {@link #storeTokenStub}. */
    private static MemorySegment storeTokenStub;

    /** Whether the table holding {@link #storeTokenStub} has been handed to the library. */
    private static boolean tokenCallbacksInstalled;

    private ScreencastNative() {
    }

    /**
     * The exports of {@code screencast_api.h}, in {@code libglassgtk3.so} as the class loader loaded it
     * ({@code NativeLibLoader.loadLibrary("glassgtk3")} in {@code GtkApplication}, or a {@code glassgtk3} build
     * loaded as the {@code glass} library itself). A holder of its own: this class is loaded, and its token
     * callback can be driven, without the library. {@code sc_abi_version} is bound and checked before anything
     * else of the library.
     */
    private static final class Lib {

        private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

        /** {@code int32_t sc_abi_version(void)}. */
        static final MethodHandle ABI = bind(LOOKUP, LIB_GLASS, "sc_abi_version",
                FunctionDescriptor.of(JAVA_INT));

        static {
            int actual;
            try {
                actual = (int) ABI.invokeExact();
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (actual != ABI_VERSION) {
                throw new UnsatisfiedLinkError(LIB_GLASS + " screen capture ABI version mismatch: expected "
                        + ABI_VERSION + ", found " + actual);
            }
        }

        /** {@code int32_t sc_sizeof_token_callbacks(void)}. */
        static final MethodHandle SIZEOF_TOKEN_CALLBACKS = bind(LOOKUP, LIB_GLASS, "sc_sizeof_token_callbacks",
                FunctionDescriptor.of(JAVA_INT));
        /** {@code int32_t sc_set_token_callbacks(const ScTokenCallbacks *cb)}. */
        static final MethodHandle SET_TOKEN_CALLBACKS = bind(LOOKUP, LIB_GLASS, "sc_set_token_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /* int32_t sc_load_pipewire(int32_t method, int32_t debug) */
        static final MethodHandle LOAD_PIPEWIRE = bind(LOOKUP, LIB_GLASS, "sc_load_pipewire",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT));
        /* int32_t sc_init_xdg_desktop_portal(void) */
        static final MethodHandle INIT_XDG_DESKTOP_PORTAL = bind(LOOKUP, LIB_GLASS, "sc_init_xdg_desktop_portal",
                FunctionDescriptor.of(JAVA_INT));
        /* void sc_close_session(void) */
        static final MethodHandle CLOSE_SESSION = bind(LOOKUP, LIB_GLASS, "sc_close_session",
                FunctionDescriptor.ofVoid());
        /*
         * int32_t sc_get_rgb_pixels(int32_t x, int32_t y, int32_t width, int32_t height, int32_t *pixels,
         * int32_t pixels_len, const int32_t *bounds, int32_t bounds_len, const char *token,
         * int32_t *out_rejected_region)
         */
        static final MethodHandle GET_RGB_PIXELS = bind(LOOKUP, LIB_GLASS, "sc_get_rgb_pixels",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS,
                        JAVA_INT, ADDRESS, ADDRESS));
        /* int32_t sc_remote_desktop_mouse_move(int32_t x, int32_t y, const char *token) */
        static final MethodHandle REMOTE_DESKTOP_MOUSE_MOVE = bind(LOOKUP, LIB_GLASS,
                "sc_remote_desktop_mouse_move", FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));
        /* int32_t sc_remote_desktop_mouse_button(int32_t is_press, int32_t buttons, const char *token) */
        static final MethodHandle REMOTE_DESKTOP_MOUSE_BUTTON = bind(LOOKUP, LIB_GLASS,
                "sc_remote_desktop_mouse_button", FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));
        /* int32_t sc_remote_desktop_mouse_wheel(int32_t wheel_amt, const char *token) */
        static final MethodHandle REMOTE_DESKTOP_MOUSE_WHEEL = bind(LOOKUP, LIB_GLASS,
                "sc_remote_desktop_mouse_wheel", FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));
        /* int32_t sc_remote_desktop_key(int32_t is_press, int32_t key, const char *token) */
        static final MethodHandle REMOTE_DESKTOP_KEY = bind(LOOKUP, LIB_GLASS, "sc_remote_desktop_key",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));

        private Lib() {
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * The token callback table (screencast_api.h ScTokenCallbacks)
     * ------------------------------------------------------------------------------------------------------- */

    /**
     * Hands the library the token callback table, so that the restore token the portal answers reaches
     * {@code TokenStorage.storeTokenFromNative} - and initializes {@code TokenStorage}, which is what the
     * {@code GetStaticMethodID} of commit {@code 033187ad90} did at this point. That is between the two halves of
     * {@code loadPipewire}: the JNI looked the class and the method up after it had loaded the PipeWire symbols
     * and before it probed the portal, and returned {@code false} - without touching the portal - when a lookup
     * failed. Nothing dials the slot before the portal is probed.
     * <p>
     * The table is copied by the library, so it is built in a confined arena; the stub it points to is in
     * {@link Arena#global()}, because the portal can answer at any later point. A second call installs the same
     * stub again.
     *
     * @throws UnsatisfiedLinkError if the library lacks an export, its ABI version differs from
     *         {@link #ABI_VERSION}, or the {@code sizeof} of the table disagrees with {@link #TOKEN_CALLBACKS_LAYOUT}
     */
    static synchronized void installTokenCallbacks() {
        int size;
        try {
            size = (int) Lib.SIZEOF_TOKEN_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (size != TOKEN_CALLBACKS_LAYOUT.byteSize()) {
            throw new UnsatisfiedLinkError(LIB_GLASS + " ScTokenCallbacks is " + size + " bytes where this build"
                    + " lays it out as " + TOKEN_CALLBACKS_LAYOUT.byteSize());
        }
        initializeTokenStorage();
        MemorySegment stub = storeTokenStub();
        try (Arena call = Arena.ofConfined()) {
            MemorySegment table = call.allocate(TOKEN_CALLBACKS_LAYOUT);
            table.set(ADDRESS, TOKEN_CALLBACKS_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(
                    "store_token")), stub);
            int status = (int) Lib.SET_TOKEN_CALLBACKS.invokeExact(table);
            if (status != OK) {
                throw new UnsatisfiedLinkError(LIB_GLASS + " sc_set_token_callbacks answered " + status);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
        tokenCallbacksInstalled = true;
    }

    /** Whether {@link #installTokenCallbacks} has run in this JVM. */
    static synchronized boolean tokenCallbacksInstalled() {
        return tokenCallbacksInstalled;
    }

    /**
     * The stub of {@link #onStoreToken}, created once in {@link Arena#global()} and never replaced: the library
     * may call a stub it has been given at any later point, so it must outlive every arena but that one.
     */
    static synchronized MemorySegment storeTokenStub() {
        if (storeTokenStub == null) {
            storeTokenStub = upcallStub("onStoreToken", STORE_TOKEN);
        }
        return storeTokenStub;
    }

    /**
     * The {@code store_token} slot: {@code TokenStorage.storeTokenFromNative(String oldToken, String newToken,
     * int[] allowedScreenBounds)}, which the {@code CallStaticVoidMethod} of {@code storeRestoreToken}
     * ({@code screencast_pipewire.c}) reached through a cached class reference and method id.
     * <p>
     * {@code oldToken} is {@code NULL} when the session was started without one, which the JNI passed as
     * {@code null}; both tokens are the bytes it handed to {@code NewStringUTF}. The bounds are the {@code int[]}
     * it built with {@code NewIntArray} and filled with the screens of the session. Nothing escapes: a
     * {@code Throwable} is described where {@code EXCEPTION_CHECK_DESCRIBE} described it and answered with
     * {@link #UPCALL_THREW}.
     */
    private static int onStoreToken(MemorySegment oldToken, MemorySegment newToken, MemorySegment bounds,
                                    int boundsLen) {
        try {
            TokenStorage.storeTokenFromNative(newStringUtf(oldToken), newStringUtf(newToken),
                    intArray(bounds, boundsLen));
            return UPCALL_OK;
        } catch (Throwable t) {
            describe(t);
            return UPCALL_THREW;
        }
    }

    /**
     * What the {@code FindClass} and {@code GetStaticMethodID} of commit {@code 033187ad90} did besides finding
     * the target: a {@code GetStaticMethodID} initializes the class it is asked about, and the failure of that
     * initialization left its {@code Error} pending, which is what the caller of the native saw.
     */
    private static void initializeTokenStorage() {
        try {
            MethodHandles.lookup().ensureInitialized(TokenStorage.class);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * The natives (screencast_api.h)
     * ------------------------------------------------------------------------------------------------------- */

    /**
     * {@code sc_load_pipewire}, the first half of
     * {@code Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_loadPipewire}: the screen capture debug output
     * of the library on or off, then - for a known method only - the remote-desktop flag and the PipeWire library
     * and its symbols. {@code false} when the method is unknown, the library is absent or a symbol is missing.
     */
    static boolean loadPipewire(int method, boolean debug) {
        try {
            return (int) Lib.LOAD_PIPEWIRE.invokeExact(method, debug ? 1 : 0) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code sc_init_xdg_desktop_portal}, the second half of that same native: the active session token, the probe
     * of the xdg-desktop-portal on the session bus and the cleanup of that probe. {@code true} when the portal is
     * usable, which is what {@code loadPipewire} answered and what {@link ScreencastHelper#isAvailable()} reports.
     * Called only after {@link #loadPipewire} answered {@code true}. BLOCKS on D-Bus.
     */
    static boolean initXdgDesktopPortal() {
        try {
            return (int) Lib.INIT_XDG_DESKTOP_PORTAL.invokeExact() != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code sc_close_session} ({@code Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_closeSession}): the
     * PipeWire streams and thread loop, the portal session and its proxies, and the active session token. Called
     * from {@code ScreencastHelper}'s timer thread two seconds after the last use.
     */
    static void closeSession() {
        try {
            Lib.CLOSE_SESSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code sc_get_rgb_pixels}
     * ({@code Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_getRGBPixelsImpl}): the capture of the
     * {@code width} x {@code height} area at ({@code x}, {@code y}) in device pixels, into {@code pixels}.
     * <p>
     * {@code pixels} and {@code bounds} cross as native copies - the call blocks on the portal, so nothing of the
     * JVM's may be held across it - and the captured rows are copied back afterwards, which is what the
     * {@code GetIntArrayElements} of the JNI did around its row-by-row {@code SetIntArrayRegion}. A row that does
     * not fit into {@code pixels} is not copied, the rows that fit still are, and the last rejected one is then
     * thrown as the {@link ArrayIndexOutOfBoundsException} {@code SetIntArrayRegion} threw for it and whose return
     * value the C dropped ({@link #rejectedRegion}). {@code token} is the restore token to start the session with,
     * or {@code null} for none. UPCALLS (the token slot) and BLOCKS.
     *
     * @return 0 or more when the pixels were captured, else {@link #RESULT_ERROR}, {@link #RESULT_DENIED},
     *         {@link #RESULT_OUT_OF_BOUNDS}, {@link #RESULT_NO_STREAMS} or the negative PipeWire file descriptor
     */
    static int getRgbPixels(int x, int y, int width, int height, int[] pixels, int[] bounds, String token) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment pixelBlock = copyToNative(call, pixels);
            MemorySegment boundsBlock = bounds == null ? MemorySegment.NULL : copyToNative(call, bounds);
            MemorySegment rejected = call.allocate(JAVA_INT, 3);
            int result = (int) Lib.GET_RGB_PIXELS.invokeExact(x, y, width, height, pixelBlock, pixels.length,
                    boundsBlock, bounds == null ? 0 : bounds.length, token(call, token), rejected);
            copyFromNative(pixelBlock, pixels);
            ArrayIndexOutOfBoundsException rejectedCopy = rejectedRegion(rejected.get(JAVA_INT, 0),
                    rejected.get(JAVA_INT, JAVA_INT.byteSize()), rejected.get(JAVA_INT, 2 * JAVA_INT.byteSize()),
                    pixels.length);
            if (rejectedCopy != null) {
                throw rejectedCopy;
            }
            return result;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code sc_remote_desktop_mouse_move}
     * ({@code Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_remoteDesktopMouseMoveImpl}): the
     * remote-desktop session started with {@code token} if it is not running, then the pointer moved to the
     * absolute device pixel ({@code x}, {@code y}). UPCALLS and BLOCKS.
     *
     * @return {@link #RESULT_OK} when the move was sent, {@link #RESULT_DENIED} when the portal refused it, else
     *         the session's failure reason
     */
    static int remoteDesktopMouseMove(int x, int y, String token) {
        try (Arena call = Arena.ofConfined()) {
            return (int) Lib.REMOTE_DESKTOP_MOUSE_MOVE.invokeExact(x, y, token(call, token));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code sc_remote_desktop_mouse_button} ({@code Java_..._remoteDesktopMouseButtonImpl}): as
     * {@link #remoteDesktopMouseMove}, with a press or release of the Glass button mask {@code buttons}.
     */
    static int remoteDesktopMouseButton(boolean isPress, int buttons, String token) {
        try (Arena call = Arena.ofConfined()) {
            return (int) Lib.REMOTE_DESKTOP_MOUSE_BUTTON.invokeExact(isPress ? 1 : 0, buttons, token(call, token));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code sc_remote_desktop_mouse_wheel} ({@code Java_..._remoteDesktopMouseWheelImpl}): as
     * {@link #remoteDesktopMouseMove}, with a wheel notch of {@code wheelAmt}.
     */
    static int remoteDesktopMouseWheel(int wheelAmt, String token) {
        try (Arena call = Arena.ofConfined()) {
            return (int) Lib.REMOTE_DESKTOP_MOUSE_WHEEL.invokeExact(wheelAmt, token(call, token));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code sc_remote_desktop_key} ({@code Java_..._remoteDesktopKeyImpl}): as {@link #remoteDesktopMouseMove},
     * with a press or release of the Glass key code {@code key}. The key is resolved before the session is
     * touched, and one that does not resolve answers {@link #RESULT_ERROR} without starting anything.
     */
    static int remoteDesktopKey(boolean isPress, int key, String token) {
        try (Arena call = Arena.ofConfined()) {
            return (int) Lib.REMOTE_DESKTOP_KEY.invokeExact(isPress ? 1 : 0, key, token(call, token));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Copies, strings and exceptions
     * ------------------------------------------------------------------------------------------------------- */

    /**
     * {@code GetStringUTFChars}: the modified UTF-8 bytes of {@code text}, NUL-terminated, or {@code NULL} for a
     * {@code null} string, which is the pointer the JNI left at {@code NULL} for one.
     */
    private static MemorySegment token(Arena call, String text) {
        return text == null ? MemorySegment.NULL : JniStringCodec.allocateModifiedUtf8(call, text);
    }

    /**
     * {@code values} in a block of {@code call}: the native copy the C reads or writes in place of the Java array,
     * copied in {@link #CHUNK_BYTES} pieces. At least one element is allocated, so that an empty array is a
     * pointer the C can tell from the {@code NULL} of a {@code null} array.
     */
    private static MemorySegment copyToNative(Arena call, int[] values) {
        MemorySegment block = call.allocate(JAVA_INT, Math.max(1, values.length));
        for (int done = 0; done < values.length; done += chunkElements()) {
            int count = Math.min(chunkElements(), values.length - done);
            MemorySegment.copy(values, done, block, JAVA_INT, (long) done * JAVA_INT.byteSize(), count);
        }
        return block;
    }

    /** The block of {@link #copyToNative} back into {@code values}, in the same pieces. */
    private static void copyFromNative(MemorySegment block, int[] values) {
        for (int done = 0; done < values.length; done += chunkElements()) {
            int count = Math.min(chunkElements(), values.length - done);
            MemorySegment.copy(block, JAVA_INT, (long) done * JAVA_INT.byteSize(), values, done, count);
        }
    }

    private static int chunkElements() {
        return (int) (CHUNK_BYTES / JAVA_INT.byteSize());
    }

    /**
     * The {@code int[]} of {@code count} ints at {@code values}, or {@code null} for a {@code NULL} pointer -
     * which is what the JNI passed when it had built no array.
     */
    @SuppressWarnings("restricted")
    private static int[] intArray(MemorySegment values, int count) {
        if (values.address() == 0) {
            return null;
        }
        return values.reinterpret((long) count * JAVA_INT.byteSize()).toArray(JAVA_INT);
    }

    /**
     * {@code NewStringUTF}: the modified UTF-8 of a C string, or {@code null} for a {@code NULL} pointer, which is
     * what {@code NewStringUTF} answered for one.
     */
    @SuppressWarnings("restricted")
    private static String newStringUtf(MemorySegment text) throws Throwable {
        if (text.address() == 0) {
            return null;
        }
        long length = (long) STRLEN.invokeExact(text);
        return JniStringCodec.fromNewStringUtf(text.reinterpret(length + 1));
    }

    /**
     * What {@code SetIntArrayRegion} threw for the last row copy the C rejected, and whose return value the C
     * dropped, or {@code null} when it rejected none. The two message forms are HotSpot's own, measured on
     * JDK 25.0.4 and JDK 26.0.2; the copy of a rejected region wrote nothing, and the regions that fitted were
     * copied all the same, which is why this is thrown after the call and after the pixels are back.
     */
    private static ArrayIndexOutOfBoundsException rejectedRegion(int count, int start, int length, int pixelsLen) {
        if (count == 0) {
            return null;
        }
        if (length < 0) {
            return new ArrayIndexOutOfBoundsException("Length " + length + " is negative");
        }
        return new ArrayIndexOutOfBoundsException("Array region " + start + ".." + (start + length)
                + " out of bounds for length " + pixelsLen);
    }

    /**
     * {@code EXCEPTION_CHECK_DESCRIBE} ({@code screencast_pipewire.c}), which is JNI {@code ExceptionDescribe}:
     * {@code Exception in thread "<name>" } on the error stream, then the stack trace, and a {@code Throwable} of
     * the description itself swallowed. The exception is cleared by returning - the C goes on either way.
     */
    private static void describe(Throwable t) {
        try {
            System.err.print("Exception in thread \"" + Thread.currentThread().getName() + "\" ");
            t.printStackTrace();
        } catch (Throwable ignored) {
            // what ExceptionDescribe did with a throw of the description
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
     * Support
     * ------------------------------------------------------------------------------------------------------- */

    /** {@code library!symbol} of every symbol bound, in binding order. */
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

    /** Resolves {@code name} in {@code lookup} and links it as an ordinary - never critical - downcall. */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(SymbolLookup lookup, String library, String name,
                                     FunctionDescriptor descriptor) {
        MemorySegment symbol = lookup.find(name).orElseThrow(
                () -> new UnsatisfiedLinkError(library + " does not export " + name));
        BOUND.put(library + "!" + name, symbol.address());
        return LINKER.downcallHandle(symbol, descriptor);
    }

    /** An upcall stub in {@link Arena#global()} for the static method {@code name} of this class. */
    @SuppressWarnings("restricted")
    private static MemorySegment upcallStub(String name, FunctionDescriptor descriptor) {
        try {
            MethodHandle target = MethodHandles.lookup().findStatic(ScreencastNative.class, name,
                    descriptor.toMethodType());
            return LINKER.upcallStub(target, descriptor, Arena.global());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no " + name + " in " + ScreencastNative.class.getName(), e);
        }
    }

    /**
     * What to throw for a {@code Throwable} caught around the downcalls of this class: a {@link RuntimeException}
     * or an {@link Error} as it is - the {@link ArrayIndexOutOfBoundsException} of {@link #getRgbPixels} among
     * them - anything else wrapped in an {@link IllegalStateException}. Nothing else can come out of one of these
     * downcalls: the C reaches Java only through the token stub, which lets nothing out.
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
