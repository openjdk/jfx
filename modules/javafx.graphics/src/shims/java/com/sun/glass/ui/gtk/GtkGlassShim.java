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
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * Test access to the GTK glass peers of this package, for the Linux binding tests in
 * {@code test.com.sun.glass.ui.gtk}. Those tests run their scenarios in a child JVM that starts the real
 * GTK toolkit on an X11 display; the child reaches this class through the
 * {@code --add-exports javafx.graphics/com.sun.glass.ui.gtk=ALL-UNNAMED} line of {@code src/test/addExports}.
 * <p>
 * Everything here goes through the same peer methods production uses, so a scenario observes the
 * implementation that is built - the JNI one of commit {@code 033187ad90} or the {@code java.lang.foreign}
 * one that replaced it - without naming either. The few {@code java.lang.foreign} bindings of its own are
 * yardsticks the scenarios measure glass against (GLib sources with a chosen priority, settings and properties
 * read back, the struct layouts {@code GtkGlassNative} hard-codes probed against the libraries); they stay in this
 * class so that the restricted calls stay inside the module {@code --enable-native-access} names.
 */
public final class GtkGlassShim {

    /** Fills the bytes past a struct whose size a layout probe checks. */
    private static final byte CANARY = (byte) 0xA5;

    private GtkGlassShim() {
    }

    private static GtkApplication application() {
        return (GtkApplication) Application.GetApplication();
    }

    /**
     * {@code GtkApplication.submitForLaterInvocation}: hands {@code runnable} straight to the peer, without the
     * {@code InvokeLaterDispatcher} that {@code Application.invokeLater} queues through.
     */
    public static void submitForLaterInvocation(Runnable runnable) {
        application().submitForLaterInvocation(runnable);
    }

    /**
     * {@code library!symbol} of every symbol {@code GtkGlassNative} bound, in binding order; initializes that class.
     * Reached reflectively, so that this shim also compiles against the JNI build of commit {@code 033187ad90},
     * where the class does not exist and the base-build runs of the behaviour tests use the rest of the shim.
     */
    @SuppressWarnings("unchecked")
    public static List<String> boundSymbols() {
        return (List<String>) facade("boundSymbols", new Class<?>[0]);
    }

    /** The address {@code GtkGlassNative} bound {@code library!symbol} at, or 0. */
    public static long boundAddress(String qualifiedName) {
        return (Long) facade("boundAddress", new Class<?>[] {String.class}, qualifiedName);
    }

    private static Object facade(String name, Class<?>[] types, Object... arguments) {
        try {
            Method method = Class.forName("com.sun.glass.ui.gtk.GtkGlassNative").getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The value of the {@code static final long} constant {@code name} of {@code GtkGlassNative}. */
    public static long facadeConstant(String name) {
        return (Long) facadeField(name);
    }

    /**
     * The number of timers {@code GtkGlassNative} still keeps a context for: a timer's entry leaves in the first tick
     * after it was stopped, the tick whose {@code FALSE} removes its GLib source.
     */
    public static int timerRegistrySize() {
        return ((Map<?, ?>) facadeField("TIMERS")).size();
    }

    private static Object facadeField(String name) {
        try {
            Field field = Class.forName("com.sun.glass.ui.gtk.GtkGlassNative").getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@code domain=<matches> code=<code> message=<message>} of a {@code GError} that {@code g_error_new_literal}
     * made, read at {@code GtkGlassNative.G_ERROR_MESSAGE_OFFSET}. Needs no display.
     */
    @SuppressWarnings("restricted")
    public static String gErrorLayout() {
        long messageOffset = facadeConstant("G_ERROR_MESSAGE_OFFSET");
        try (Arena arena = Arena.ofConfined()) {
            int quark = (int) Layout.G_QUARK_FROM_STRING.invokeExact(arena.allocateFrom("jfx-gtk-glass-test"));
            MemorySegment error = (MemorySegment) Layout.G_ERROR_NEW_LITERAL.invokeExact(quark, 42,
                    arena.allocateFrom("layout probe"));
            MemorySegment fields = error.reinterpret(messageOffset + ADDRESS.byteSize());
            MemorySegment message = fields.get(ADDRESS, messageOffset);
            String text = "domain=" + (fields.get(JAVA_INT, 0) == quark) + " code=" + fields.get(JAVA_INT, 4)
                    + " message=" + message.reinterpret(Long.MAX_VALUE).getString(0);
            Layout.G_ERROR_FREE.invokeExact(error);
            return text;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * A {@code GHashTableIter} of {@code GtkGlassNative.G_HASH_TABLE_ITER_SIZE} bytes followed by 32 canary bytes,
     * through {@code g_hash_table_iter_init} and {@code g_hash_table_iter_next} over a one-entry table:
     * {@code entries=<key>:<value>,... tableAtStart=<bool> canary=<intact|overwritten>}. Needs no display.
     */
    public static String hashTableIterLayout() {
        long size = facadeConstant("G_HASH_TABLE_ITER_SIZE");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment iter = arena.allocate(size + 32);
            iter.fill(CANARY);
            MemorySegment table = (MemorySegment) Layout.G_HASH_TABLE_NEW.invokeExact(Layout.G_DIRECT_HASH,
                    Layout.G_DIRECT_EQUAL);
            int added = (int) Layout.G_HASH_TABLE_INSERT.invokeExact(table, MemorySegment.ofAddress(7),
                    MemorySegment.ofAddress(9));
            Layout.G_HASH_TABLE_ITER_INIT.invokeExact(iter, table);
            boolean tableAtStart = iter.get(ADDRESS, 0).address() == table.address();
            MemorySegment key = arena.allocate(ADDRESS);
            MemorySegment value = arena.allocate(ADDRESS);
            List<String> entries = new ArrayList<>();
            while ((int) Layout.G_HASH_TABLE_ITER_NEXT.invokeExact(iter, key, value) != 0) {
                entries.add(key.get(ADDRESS, 0).address() + ":" + value.get(ADDRESS, 0).address());
            }
            Layout.G_HASH_TABLE_UNREF.invokeExact(table);
            return "entries=" + String.join(",", entries) + " tableAtStart=" + tableAtStart + " canary="
                    + canary(iter, size);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code XkbGetState(display, XkbUseCoreKbd, state)} into {@code GtkGlassNative.XKB_STATE_REC_SIZE} bytes followed
     * by 32 canary bytes: {@code status=<status> canary=<intact|overwritten>}; on the FX thread.
     */
    public static String xkbStateRecLayout() {
        long size = facadeConstant("XKB_STATE_REC_SIZE");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment state = arena.allocate(size + 32);
            state.fill(CANARY);
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            int status = (int) Readback.XKB_GET_STATE.invokeExact(display, 0x0100, state);
            return "status=" + status + " canary=" + canary(state, size);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * Every {@code GdkKeymapKey} that {@code gdk_keymap_get_entries_for_keyval} answers for the keyvals 0x20..0x7e
     * and 0xff00..0xffff, read in steps of {@code GtkGlassNative.GDK_KEYMAP_KEY_SIZE} and checked against
     * {@code XkbKeycodeToKeysym(display, keycode, group, level)}:
     * {@code entries=<n> laterEntries=<entries at index 1 or more> mismatches=<keyval:index:keycode/group/level ...>};
     * on the FX thread.
     */
    @SuppressWarnings("restricted")
    public static String keymapKeyLayout() {
        long size = facadeConstant("GDK_KEYMAP_KEY_SIZE");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment keysOut = arena.allocate(ADDRESS);
            MemorySegment countOut = arena.allocate(JAVA_INT);
            MemorySegment gdkDisplay = (MemorySegment) Readback.GDK_DISPLAY_GET_DEFAULT.invokeExact();
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            MemorySegment keymap = (MemorySegment) Readback.GDK_KEYMAP_GET_FOR_DISPLAY.invokeExact(gdkDisplay);
            int entries = 0;
            int laterEntries = 0;
            List<String> mismatches = new ArrayList<>();
            List<Integer> keyvals = new ArrayList<>();
            for (int keyval = 0x20; keyval <= 0x7e; keyval++) {
                keyvals.add(keyval);
            }
            for (int keyval = 0xff00; keyval <= 0xffff; keyval++) {
                keyvals.add(keyval);
            }
            for (int keyval : keyvals) {
                if ((int) Readback.GDK_KEYMAP_GET_ENTRIES_FOR_KEYVAL.invokeExact(keymap, keyval, keysOut,
                        countOut) == 0) {
                    continue;
                }
                int count = countOut.get(JAVA_INT, 0);
                MemorySegment keys = keysOut.get(ADDRESS, 0).reinterpret(count * size);
                for (int i = 0; i < count; i++) {
                    int keycode = keys.get(JAVA_INT, i * size);
                    int group = keys.get(JAVA_INT, i * size + 4);
                    int level = keys.get(JAVA_INT, i * size + 8);
                    long keysym = (long) Readback.XKB_KEYCODE_TO_KEYSYM.invokeExact(display, (byte) keycode, group,
                            level);
                    if (keycode < 8 || keycode > 255 || keysym != keyval) {
                        mismatches.add(Integer.toHexString(keyval) + ":" + i + ":" + keycode + "/" + group + "/"
                                + level);
                    }
                    entries++;
                    if (i > 0) {
                        laterEntries++;
                    }
                }
                Readback.G_FREE.invokeExact(keys);
            }
            return "entries=" + entries + " laterEntries=" + laterEntries + " mismatches="
                    + String.join(" ", mismatches);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private static String canary(MemorySegment segment, long from) {
        for (long i = from; i < segment.byteSize(); i++) {
            if (segment.get(JAVA_BYTE, i) != CANARY) {
                return "overwritten";
            }
        }
        return "intact";
    }

    /** {@code <major>.<minor>.<micro>} of the GTK library the process runs. */
    public static String gtkVersion() {
        try {
            int major = (int) Readback.GTK_GET_MAJOR_VERSION.invokeExact();
            int minor = (int) Readback.GTK_GET_MINOR_VERSION.invokeExact();
            int micro = (int) Readback.GTK_GET_MICRO_VERSION.invokeExact();
            return major + "." + minor + "." + micro;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code gdk_x11_screen_get_window_manager_name(gdk_screen_get_default())}: {@code unknown} when no window
     * manager runs; on the FX thread.
     */
    @SuppressWarnings("restricted")
    public static String windowManagerName() {
        try {
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            MemorySegment name = (MemorySegment) Readback.GDK_X11_SCREEN_GET_WINDOW_MANAGER_NAME.invokeExact(screen);
            return name.address() == 0 ? "null" : name.reinterpret(Long.MAX_VALUE).getString(0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code gdk_visual_get_depth(gdk_screen_get_system_visual(gdk_screen_get_default()))}; on the FX thread. */
    public static int systemVisualDepth() {
        try {
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            MemorySegment visual = (MemorySegment) Readback.GDK_SCREEN_GET_SYSTEM_VISUAL.invokeExact(screen);
            return (int) Readback.GDK_VISUAL_GET_DEPTH.invokeExact(visual);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code XServerVendor} and {@code XVendorRelease} of GDK's X display; on the FX thread. */
    @SuppressWarnings("restricted")
    public static String xServer() {
        try {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            MemorySegment vendor = (MemorySegment) Readback.X_SERVER_VENDOR.invokeExact(display);
            int release = (int) Readback.X_VENDOR_RELEASE.invokeExact(display);
            return (vendor.address() == 0 ? "null" : vendor.reinterpret(Long.MAX_VALUE).getString(0)) + " " + release;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code g_object_set(gtk_settings_get_default(), name, value, NULL)} of a {@code gint} setting; FX thread. */
    public static void setGtkSettingInt(String name, int value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment settings = (MemorySegment) Readback.GTK_SETTINGS_GET_DEFAULT.invokeExact();
            Readback.G_OBJECT_SET_ONE_INT.invokeExact(settings, arena.allocateFrom(name), value, MemorySegment.NULL);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code g_idle_add_full(priority, ...)}: runs {@code runnable} once on the thread that iterates the default
     * main context, at {@code priority}. A {@code Throwable} it throws is printed and swallowed.
     */
    public static void addGlibIdle(int priority, Runnable runnable) {
        long id = Yardstick.register(runnable);
        try {
            int source = (int) Yardstick.G_IDLE_ADD_FULL.invokeExact(priority, Yardstick.CALLBACK,
                    MemorySegment.ofAddress(id), MemorySegment.NULL);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code g_timeout_add_full(priority, interval, ...)}: runs {@code runnable} once, {@code interval}
     * milliseconds from now, at {@code priority}. A {@code Throwable} it throws is printed and swallowed.
     */
    public static void addGlibTimeout(int priority, int interval, Runnable runnable) {
        long id = Yardstick.register(runnable);
        try {
            int source = (int) Yardstick.G_TIMEOUT_ADD_FULL.invokeExact(priority, interval, Yardstick.CALLBACK,
                    MemorySegment.ofAddress(id), MemorySegment.NULL);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code GtkPixels._attachInt} with the given arguments and a fresh {@code GdkPixbuf *} slot (or a {@code 0}
     * pointer when {@code nullPointer}): answers {@link #describePixbuf} of what the slot holds afterwards, and
     * releases that pixbuf. The peer ignores its own fields, so any {@code GtkPixels} can carry the call.
     */
    public static String attachInt(int w, int h, IntBuffer ints, int[] array, int offset, boolean nullPointer) {
        GtkPixels carrier = new GtkPixels(1, 1, IntBuffer.allocate(1));
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment slot = arena.allocate(ADDRESS);
            carrier._attachInt(nullPointer ? 0L : slot.address(), w, h, ints, array, offset);
            return describePixbuf(slot.get(ADDRESS, 0), true);
        }
    }

    /** {@link #attachInt} for {@code GtkPixels._attachByte}. */
    public static String attachByte(int w, int h, ByteBuffer bytes, byte[] array, int offset, boolean nullPointer) {
        GtkPixels carrier = new GtkPixels(1, 1, ByteBuffer.allocate(4));
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment slot = arena.allocate(ADDRESS);
            carrier._attachByte(nullPointer ? 0L : slot.address(), w, h, bytes, array, offset);
            return describePixbuf(slot.get(ADDRESS, 0), true);
        }
    }

    /** {@code Cursor.getNativeCursor()}, the {@code GdkCursor *} of a glass cursor; on the FX thread. */
    public static long nativeCursor(Cursor cursor) {
        try {
            Method method = Cursor.class.getDeclaredMethod("getNativeCursor");
            method.setAccessible(true);
            return (Long) method.invoke(cursor);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@code type=<gdk_cursor_get_cursor_type> defaultDisplay=<bool> image=<describePixbuf of gdk_cursor_get_image>}
     * for a {@code GdkCursor *}, or {@code null} for 0; on the FX thread.
     */
    public static String describeCursor(long cursor) {
        if (cursor == 0L) {
            return "null";
        }
        try {
            MemorySegment c = MemorySegment.ofAddress(cursor);
            int type = (int) Readback.GDK_CURSOR_GET_CURSOR_TYPE.invokeExact(c);
            MemorySegment display = (MemorySegment) Readback.GDK_CURSOR_GET_DISPLAY.invokeExact(c);
            MemorySegment defaultDisplay = (MemorySegment) Readback.GDK_DISPLAY_GET_DEFAULT.invokeExact();
            MemorySegment image = (MemorySegment) Readback.GDK_CURSOR_GET_IMAGE.invokeExact(c);
            return "type=" + type + " defaultDisplay=" + (display.address() == defaultDisplay.address())
                    + " image=" + describePixbuf(image, true);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code g_object_get(gtk_settings_get_default(), name, &value, NULL)} of a {@code gint} setting; FX thread. */
    public static int gtkSettingInt(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment settings = (MemorySegment) Readback.GTK_SETTINGS_GET_DEFAULT.invokeExact();
            MemorySegment value = arena.allocate(JAVA_INT);
            value.set(JAVA_INT, 0, -12345);
            Readback.G_OBJECT_GET_ONE.invokeExact(settings, arena.allocateFrom(name), value, MemorySegment.NULL);
            return value.get(JAVA_INT, 0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code gdk_display_supports_composite(default display)} and {@code gdk_screen_is_composited(default screen)}. */
    public static String compositing() {
        try {
            MemorySegment display = (MemorySegment) Readback.GDK_DISPLAY_GET_DEFAULT.invokeExact();
            int supports = (int) Readback.GDK_DISPLAY_SUPPORTS_COMPOSITE.invokeExact(display);
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            int composited = (int) Readback.GDK_SCREEN_IS_COMPOSITED.invokeExact(screen);
            return "supports=" + supports + " composited=" + composited;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code gdk_screen_get_resolution(gdk_screen_get_default())}; on the FX thread. */
    public static double screenResolution() {
        try {
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            return (double) Readback.GDK_SCREEN_GET_RESOLUTION.invokeExact(screen);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code gdk_display_get_default_cursor_size(gdk_display_get_default())}; on the FX thread. */
    public static int gdkDefaultCursorSize() {
        try {
            MemorySegment display = (MemorySegment) Readback.GDK_DISPLAY_GET_DEFAULT.invokeExact();
            return (int) Readback.GDK_DISPLAY_GET_DEFAULT_CURSOR_SIZE.invokeExact(display);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * The {@code _NET_WM_ICON} property of the X11 window {@code xid} on GDK's display, as
     * {@code items=<n> w=<w> h=<h> sha256=<hex of the items as 32-bit big-endian values>}, or {@code none}; on the
     * FX thread.
     */
    @SuppressWarnings("restricted")
    public static String netWmIcon(long xid) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            long atom = (long) Readback.X_INTERN_ATOM.invokeExact(display, arena.allocateFrom("_NET_WM_ICON"), 0);
            MemorySegment type = arena.allocate(JAVA_LONG);
            MemorySegment format = arena.allocate(JAVA_INT);
            MemorySegment count = arena.allocate(JAVA_LONG);
            MemorySegment after = arena.allocate(JAVA_LONG);
            MemorySegment data = arena.allocate(ADDRESS);
            int status = (int) Readback.X_GET_WINDOW_PROPERTY.invokeExact(display, xid, atom, 0L, 0x7FFFFFFFL, 0,
                    0L, type, format, count, after, data);
            MemorySegment items = data.get(ADDRESS, 0);
            long n = count.get(JAVA_LONG, 0);
            if (status != 0 || items.address() == 0 || n < 2) {
                if (items.address() != 0) {
                    int freed = (int) Readback.X_FREE.invokeExact(items);
                }
                return "none";
            }
            MemorySegment values = items.reinterpret(n * JAVA_LONG.byteSize());
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            for (long i = 0; i < n; i++) {
                int v = (int) values.getAtIndex(JAVA_LONG, i);
                sha.update(new byte[] {(byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v});
            }
            String result = "items=" + n + " w=" + values.getAtIndex(JAVA_LONG, 0) + " h="
                    + values.getAtIndex(JAVA_LONG, 1) + " sha256=" + HexFormat.of().formatHex(sha.digest());
            int freed = (int) Readback.X_FREE.invokeExact(items);
            return result;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code null}, or {@code w= h= stride= channels= alpha= bps= colorspace= bytes=<hex of height * rowstride
     * bytes>} of a {@code GdkPixbuf *}; with {@code unref}, {@code g_object_unref}s it afterwards.
     */
    @SuppressWarnings("restricted")
    static String describePixbuf(MemorySegment pixbuf, boolean unref) {
        if (pixbuf.address() == 0) {
            return "null";
        }
        try {
            int w = (int) Readback.GDK_PIXBUF_GET_WIDTH.invokeExact(pixbuf);
            int h = (int) Readback.GDK_PIXBUF_GET_HEIGHT.invokeExact(pixbuf);
            int stride = (int) Readback.GDK_PIXBUF_GET_ROWSTRIDE.invokeExact(pixbuf);
            int channels = (int) Readback.GDK_PIXBUF_GET_N_CHANNELS.invokeExact(pixbuf);
            int alpha = (int) Readback.GDK_PIXBUF_GET_HAS_ALPHA.invokeExact(pixbuf);
            int bps = (int) Readback.GDK_PIXBUF_GET_BITS_PER_SAMPLE.invokeExact(pixbuf);
            int colorspace = (int) Readback.GDK_PIXBUF_GET_COLORSPACE.invokeExact(pixbuf);
            MemorySegment pixels = (MemorySegment) Readback.GDK_PIXBUF_GET_PIXELS.invokeExact(pixbuf);
            byte[] bytes = pixels.reinterpret((long) h * stride).toArray(JAVA_BYTE);
            String text = "w=" + w + " h=" + h + " stride=" + stride + " channels=" + channels + " alpha=" + alpha
                    + " bps=" + bps + " colorspace=" + colorspace + " bytes=" + HexFormat.of().formatHex(bytes);
            if (unref) {
                Readback.G_OBJECT_UNREF.invokeExact(pixbuf);
            }
            return text;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code find_gdk_keyval_for_glass_keycode} of the {@code glass_key.cpp} that stays in {@code libglassgtk3.so}
     * for the event and screencast C; on the FX thread.
     */
    public static int cFindGdkKeyvalForGlassKeycode(int code) {
        try {
            return (int) Glass.FIND_GDK_KEYVAL_FOR_GLASS_KEYCODE.invokeExact(code);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code find_gdk_keycode_for_keyval} of {@code libglassgtk3.so}; on the FX thread. */
    public static int cFindGdkKeycodeForKeyval(int keyval) {
        try {
            return (int) Glass.FIND_GDK_KEYCODE_FOR_KEYVAL.invokeExact(keyval);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code gdk_keyval_to_glass} of {@code libglassgtk3.so}: its {@code keymap} lookup; on the FX thread. */
    public static int cGdkKeyvalToGlass(int keyval) {
        try {
            return (int) Glass.GDK_KEYVAL_TO_GLASS.invokeExact(keyval);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code getUIScale(gdk_screen_get_default())} of {@code glass_screen.cpp} in {@code libglassgtk3.so}. */
    public static float cGetUIScale() {
        try {
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            return (float) Glass.GET_UI_SCALE.invokeExact(screen);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** Sets both the C global {@code OverrideUIScale} ({@code glass_screen.cpp}) and its Java counterpart. */
    public static void setOverrideUIScale(float scale) {
        Glass.OVERRIDE_UI_SCALE.set(JAVA_FLOAT, 0, scale);
        GtkApplication.overrideUIScale = scale;
    }

    /** {@code GtkApplication.overrideUIScale}. */
    public static float overrideUIScale() {
        return GtkApplication.overrideUIScale;
    }

    /** libc {@code setenv(name, value, 1)}, or {@code unsetenv(name)} for a {@code null} value. */
    public static void setenv(String name, String value) {
        try (Arena arena = Arena.ofConfined()) {
            if (value == null) {
                int rc = (int) Glass.UNSETENV.invokeExact(arena.allocateFrom(name));
            } else {
                int rc = (int) Glass.SETENV.invokeExact(arena.allocateFrom(name), arena.allocateFrom(value), 1);
            }
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * libc {@code getenv(name)}, which answers what the process environment holds now - what a {@code putenv} or
     * {@code setenv} of this process wrote - where {@code System.getenv} answers the copy the JVM took at startup.
     */
    @SuppressWarnings("restricted")
    public static String getenv(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value = (MemorySegment) Libc.GETENV.invokeExact(arena.allocateFrom(name));
            return value.address() == 0 ? null : value.reinterpret(Integer.MAX_VALUE).getString(0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * Initializes {@code GtkApplication}, whose class initializer maps the library named {@code glass} - the
     * deployment that renames a {@code glassgtk3} build to {@code libglass.so} is the one the query answers
     * {@code QUERY_USE_CURRENT} for - and tolerates the absence of that library. Runs the production path
     * itself, so that a scenario needs no toolkit and no display for it.
     */
    public static void initializeGtkApplication() {
        try {
            MethodHandles.lookup().ensureInitialized(GtkApplication.class);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@code GtkGlassNative.Loader.queryLibrary}: which glass GTK library the toolkit must load, the answer of
     * {@code GtkApplication._queryLibrary}.
     */
    public static int queryLibrary(int version, boolean verbose) {
        return GtkGlassNative.Loader.queryLibrary(version, verbose);
    }

    /**
     * {@code GtkGlassNative.Loader.sniffLibs} over the two chains of library names: the decision table of
     * {@code sniffLibs} ({@code launcher.c}), driven with names this machine has.
     */
    public static int sniffLibs(int wantVersion, boolean verbose, String[] gtk3, String[] gtk2) {
        return GtkGlassNative.Loader.sniffLibs(wantVersion, verbose, gtk3, gtk2);
    }

    /** {@code GtkGlassNative.Loader.tryLibrariesNoload}: is {@code soname} loaded in this process already? */
    public static boolean libraryIsLoaded(String soname) {
        return GtkGlassNative.Loader.tryLibrariesNoload(soname);
    }

    /** {@code library!symbol} of every symbol the library query binds, in binding order. */
    public static List<String> loaderBoundSymbols() {
        return GtkGlassNative.Loader.boundSymbols();
    }

    /**
     * The values {@code com.sun.glass.ui.gtk} and the C of {@code native-glass/gtk/glass_gtk_api.h} share: the
     * {@code QUERY_*} the library query answers - which are the ones {@code GtkApplication} branches on, whose
     * fields are initialized from these - and the {@code HT_*} of {@code GtkWindow}.
     */
    public static Map<String, Integer> sharedConstants() {
        Map<String, Integer> values = new LinkedHashMap<>();
        values.put("QUERY_ERROR", GtkGlassNative.Loader.QUERY_ERROR);
        values.put("QUERY_NO_DISPLAY", GtkGlassNative.Loader.QUERY_NO_DISPLAY);
        values.put("QUERY_USE_CURRENT", GtkGlassNative.Loader.QUERY_USE_CURRENT);
        values.put("QUERY_LOAD_GTK3", GtkGlassNative.Loader.QUERY_LOAD_GTK3);
        for (String name : List.of("HT_UNSPECIFIED", "HT_CAPTION", "HT_CLIENT")) {
            values.put(name, constant(GtkWindow.class, name));
        }
        return values;
    }

    private static int constant(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Whether {@code GtkApplication._queryLibrary} is still a {@code native} method - whether the glass GTK
     * library is chosen by the {@code libglass.so} launcher of commit {@code 033187ad90} or in Java.
     */
    public static boolean queryLibraryIsNative() {
        try {
            return Modifier.isNative(GtkApplication.class
                    .getDeclaredMethod("_queryLibrary", int.class, boolean.class).getModifiers());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Whether {@code dlsym(RTLD_DEFAULT, name)} resolves: whether {@code name} is in the global scope of the
     * process, which is what {@code dlopen(..., RTLD_GLOBAL)} puts a library and its dependencies into.
     */
    public static boolean symbolIsInTheGlobalScope(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment symbol = (MemorySegment) Libc.DLSYM.invokeExact(MemorySegment.NULL,
                    arena.allocateFrom(name));
            return symbol.address() != 0;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code GtkGlassNative.findGdkKeyvalForGlassKeycode}. */
    public static int javaFindGdkKeyvalForGlassKeycode(int code) {
        return (Integer) facade("findGdkKeyvalForGlassKeycode", new Class<?>[] {int.class}, code);
    }

    /** {@code GtkGlassNative.findGdkKeycodeForKeyval}. */
    public static int javaFindGdkKeycodeForKeyval(int keyval) {
        return (Integer) facade("findGdkKeycodeForKeyval", new Class<?>[] {int.class}, keyval);
    }

    /** {@code GtkGlassNative.getUIScale(gdk_screen_get_default())}. */
    public static float javaGetUIScale() {
        try {
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            return (Float) facade("getUIScale", new Class<?>[] {MemorySegment.class}, screen);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code GtkApplication.staticScreen_getScreens()}, the peer method itself; on the FX thread. */
    public static Screen[] screens() {
        return application().staticScreen_getScreens();
    }

    /** {@code GtkApplication.getPlatformPreferences()}, the peer method itself; on the FX thread. */
    public static Map<String, Object> platformPreferences() {
        return application().getPlatformPreferences();
    }

    /** {@code GtkApplication.getPlatformKeys()}: the key names and types the peer declares. */
    public static Map<String, Class<?>> platformKeys() {
        return application().getPlatformKeys();
    }

    /**
     * {@code g_object_class_find_property(G_OBJECT_GET_CLASS(gtk_settings_get_default()), name) != NULL}: whether
     * this GTK build has the setting at all; FX thread.
     */
    @SuppressWarnings("restricted")
    public static boolean gtkSettingProperty(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment settings = (MemorySegment) Readback.GTK_SETTINGS_GET_DEFAULT.invokeExact();
            MemorySegment settingsClass = settings.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
            MemorySegment property = (MemorySegment) Readback.G_OBJECT_CLASS_FIND_PROPERTY.invokeExact(
                    settingsClass, arena.allocateFrom(name));
            return property.address() != 0;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code g_object_set(gtk_settings_get_default(), name, value, NULL)} of a string setting; FX thread. */
    public static void setGtkSettingString(String name, String value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment settings = (MemorySegment) Readback.GTK_SETTINGS_GET_DEFAULT.invokeExact();
            Readback.G_OBJECT_SET_ONE.invokeExact(settings, arena.allocateFrom(name), arena.allocateFrom(value),
                    MemorySegment.NULL);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code g_object_get(gtk_settings_get_default(), name, &value, NULL)} of a string setting; FX thread. */
    @SuppressWarnings("restricted")
    public static String gtkSettingString(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment settings = (MemorySegment) Readback.GTK_SETTINGS_GET_DEFAULT.invokeExact();
            MemorySegment out = arena.allocate(ADDRESS);
            Readback.G_OBJECT_GET_ONE.invokeExact(settings, arena.allocateFrom(name), out, MemorySegment.NULL);
            MemorySegment value = out.get(ADDRESS, 0);
            if (value.address() == 0) {
                return null;
            }
            String text = value.reinterpret(Long.MAX_VALUE).getString(0);
            Readback.G_FREE.invokeExact(value);
            return text;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code gtk_style_lookup_color(gtk_style_new(), name, &color)}: {@code {found, red, green, blue}} with the
     * three 16-bit channels read where {@code gdkcolor.h} puts them, after the 32-bit {@code pixel}. The yardstick
     * the platform preference colours are held against; FX thread.
     */
    @SuppressWarnings("restricted")
    public static int[] gtkStyleColor(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment style = (MemorySegment) Readback.GTK_STYLE_NEW.invokeExact();
            try {
                MemorySegment color = arena.allocate(GDK_COLOR_SIZE);
                int found = (int) Readback.GTK_STYLE_LOOKUP_COLOR.invokeExact(style, arena.allocateFrom(name),
                        color);
                return new int[] {found, Short.toUnsignedInt(color.get(JAVA_SHORT, 4)),
                    Short.toUnsignedInt(color.get(JAVA_SHORT, 6)), Short.toUnsignedInt(color.get(JAVA_SHORT, 8))};
            } finally {
                Readback.G_OBJECT_UNREF.invokeExact(style);
            }
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code GdkColor}: {@code guint32 pixel}, then {@code guint16 red, green, blue}. */
    private static final long GDK_COLOR_SIZE = 12;

    /** {@code GTK_STYLE_PROVIDER_PRIORITY_APPLICATION} of {@code gtkstyleprovider.h}. */
    private static final int GTK_STYLE_PROVIDER_PRIORITY_APPLICATION = 600;

    /**
     * Adds a CSS provider to the default screen that redefines the theme colour {@code name} as {@code value},
     * so that a colour off the 1/257 grid of an 8-bit theme palette reaches the preference collector; FX thread.
     * Answers whether the CSS parsed.
     */
    @SuppressWarnings("restricted")
    public static boolean defineThemeColor(String name, String value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment provider = (MemorySegment) Readback.GTK_CSS_PROVIDER_NEW.invokeExact();
            String css = "@define-color " + name + " " + value + ";";
            int loaded = (int) Readback.GTK_CSS_PROVIDER_LOAD_FROM_DATA.invokeExact(provider,
                    arena.allocateFrom(css), -1L, MemorySegment.NULL);
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            Readback.GTK_STYLE_CONTEXT_ADD_PROVIDER_FOR_SCREEN.invokeExact(screen, provider,
                    GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
            return loaded != 0;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code gdk_screen_get_n_monitors(gdk_screen_get_default())}; FX thread. */
    public static int gdkMonitorCount() {
        try {
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            return (int) Readback.GDK_SCREEN_GET_N_MONITORS.invokeExact(screen);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code gdk_screen_get_monitor_geometry} as {@code x,y,width,height}; FX thread. */
    public static int[] gdkMonitorGeometry(int monitor) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            MemorySegment rectangle = arena.allocate(16);
            Readback.GDK_SCREEN_GET_MONITOR_GEOMETRY.invokeExact(screen, monitor, rectangle);
            return rectangle.toArray(JAVA_INT);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code gdk_screen_get_width}, {@code _height}, {@code _width_mm}, {@code _height_mm} of the default screen
     * and {@code gdk_screen_get_monitor_width_mm}, {@code _height_mm} of {@code monitor}; FX thread.
     */
    public static int[] gdkScreenSize(int monitor) {
        try {
            MemorySegment screen = (MemorySegment) Readback.GDK_SCREEN_GET_DEFAULT.invokeExact();
            return new int[] {
                (int) Readback.GDK_SCREEN_GET_WIDTH.invokeExact(screen),
                (int) Readback.GDK_SCREEN_GET_HEIGHT.invokeExact(screen),
                (int) Readback.GDK_SCREEN_GET_WIDTH_MM.invokeExact(screen),
                (int) Readback.GDK_SCREEN_GET_HEIGHT_MM.invokeExact(screen),
                (int) Readback.GDK_SCREEN_GET_MONITOR_WIDTH_MM.invokeExact(screen, monitor),
                (int) Readback.GDK_SCREEN_GET_MONITOR_HEIGHT_MM.invokeExact(screen, monitor)
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * Replaces the 32-bit cardinal property {@code name} of the X11 root window with {@code values}, or deletes it
     * when {@code values} is {@code null}, and waits for the server; FX thread. The GTK glass event code turns the
     * resulting {@code PropertyNotify} of {@code _NET_WORKAREA} or {@code _NET_CURRENT_DESKTOP} into
     * {@code Screen.notifySettingsChanged}.
     */
    @SuppressWarnings("restricted")
    public static void setRootCardinals(String name, long[] values) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            long root = (long) Readback.GDK_X11_GET_DEFAULT_ROOT_XWINDOW.invokeExact();
            long atom = (long) Readback.X_INTERN_ATOM.invokeExact(display, arena.allocateFrom(name), 0);
            if (values == null) {
                int status = (int) Readback.X_DELETE_PROPERTY.invokeExact(display, root, atom);
            } else {
                MemorySegment data = arena.allocateFrom(JAVA_LONG, values);
                int status = (int) Readback.X_CHANGE_PROPERTY.invokeExact(display, root, atom, XA_CARDINAL, 32,
                        PROP_MODE_REPLACE, data, values.length);
            }
            int synced = (int) Readback.X_SYNC.invokeExact(display, 0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * The one {@code GtkSystemClipboard} of the process, which {@code Clipboard.get("SYSTEM")} creates on first
     * use; reached reflectively because that factory is {@code protected} in another package. Everything below
     * calls the peer methods themselves, not {@code SystemClipboard.getData}, which short-circuits to the local
     * data whenever this process owns the clipboard.
     */
    private static GtkSystemClipboard systemClipboard() {
        try {
            Method get = Clipboard.class.getDeclaredMethod("get", String.class);
            get.setAccessible(true);
            return (GtkSystemClipboard) get.invoke(null, Clipboard.SYSTEM);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code GtkSystemClipboard.pushToSystem}, the peer method itself; on the FX thread. */
    public static void clipboardPushToSystem(HashMap<String, Object> data, int supportedActions) {
        systemClipboard().pushToSystem(data, supportedActions);
    }

    /** {@code GtkSystemClipboard.popFromSystem}, the peer method itself; on the FX thread. */
    public static Object clipboardPopFromSystem(String mimeType) {
        return systemClipboard().popFromSystem(mimeType);
    }

    /** {@code GtkSystemClipboard.mimesFromSystem}, the peer method itself; on the FX thread. */
    public static String[] clipboardMimesFromSystem() {
        return systemClipboard().mimesFromSystem();
    }

    /** {@code GtkSystemClipboard.isOwner}, the peer method itself; on the FX thread. */
    public static boolean clipboardIsOwner() {
        return systemClipboard().isOwner();
    }

    /** {@code GtkSystemClipboard.supportedSourceActionsFromSystem}, the peer method itself; on the FX thread. */
    public static int clipboardSupportedSourceActions() {
        return systemClipboard().supportedSourceActionsFromSystem();
    }

    /** {@code GtkSystemClipboard.pushTargetActionToSystem}, the peer method itself; on the FX thread. */
    public static void clipboardPushTargetAction(int actionDone) {
        systemClipboard().pushTargetActionToSystem(actionDone);
    }

    /** {@code GtkSystemClipboard.dispose}, the peer method itself; on the FX thread. */
    public static void clipboardDispose() {
        systemClipboard().dispose();
    }

    /** {@code GtkSystemClipboard.init}, the peer method itself; on the FX thread. */
    public static void clipboardInit() {
        systemClipboard().init();
    }

    /**
     * Ends the one mapped {@code GtkFileChooserDialog} with {@code response}, after setting
     * {@code currentName} as the file name when it is not {@code null}; answers what the dialog offered:
     * {@code <title>|<current folder>|<filter name>...}, or {@code null} when there is no such dialog.
     */
    @SuppressWarnings("restricted")
    public static String respondToFileChooser(int response, String currentName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dialog = fileChooserDialog();
            if (dialog.address() == 0) {
                return null;
            }
            String text = fileChooserState(dialog);
            if (currentName != null) {
                Dialog.GTK_FILE_CHOOSER_SET_CURRENT_NAME.invokeExact(dialog, arena.allocateFrom(currentName));
            }
            Dialog.GTK_DIALOG_RESPONSE.invokeExact(dialog, response);
            return text;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code <title>|<current folder>|<filter name>...} of the one mapped {@code GtkFileChooserDialog}, or
     * {@code null} when there is no such dialog; nothing is changed and no response is sent. A chooser loads its
     * browse folder asynchronously, so the folder is empty until it has.
     */
    public static String fileChooserState() {
        try {
            MemorySegment dialog = fileChooserDialog();
            return dialog.address() == 0 ? null : fileChooserState(dialog);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * The bytes GTK holds for the strings the dialog was given, as
     * {@code <title hex>|<filter name hex>...}: what the caller's string encoder actually produced, without a
     * decoder in between. {@code null} when there is no mapped {@code GtkFileChooserDialog}.
     */
    @SuppressWarnings("restricted")
    public static String fileChooserHex() {
        try {
            MemorySegment dialog = fileChooserDialog();
            if (dialog.address() == 0) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            MemorySegment title = (MemorySegment) Dialog.GTK_WINDOW_GET_TITLE.invokeExact(dialog);
            text.append(cStringHex(title));
            MemorySegment filters = (MemorySegment) Dialog.GTK_FILE_CHOOSER_LIST_FILTERS.invokeExact(dialog);
            for (MemorySegment node = filters; node.address() != 0; node = nextNode(node)) {
                MemorySegment filter = node.reinterpret(GLIST_SIZE).get(ADDRESS, 0);
                MemorySegment name = (MemorySegment) Dialog.GTK_FILE_FILTER_GET_NAME.invokeExact(filter);
                text.append('|').append(cStringHex(name));
            }
            Dialog.G_LIST_FREE.invokeExact(filters);
            return text.toString();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code gtk_file_chooser_select_filename} of every name, on the one mapped {@code GtkFileChooserDialog};
     * answers how many were accepted, or -1 when there is no such dialog.
     */
    @SuppressWarnings("restricted")
    public static int selectInFileChooser(List<String> filenames) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dialog = fileChooserDialog();
            if (dialog.address() == 0) {
                return -1;
            }
            int selected = 0;
            for (String filename : filenames) {
                if ((int) Dialog.GTK_FILE_CHOOSER_SELECT_FILENAME.invokeExact(dialog,
                        arena.allocateFrom(filename)) != 0) {
                    selected++;
                }
            }
            return selected;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code gtk_file_chooser_select_all} of the one mapped {@code GtkFileChooserDialog}: everything in the
     * folder it is showing. Answers the number of files selected afterwards, or -1 when there is no such dialog.
     */
    @SuppressWarnings("restricted")
    public static int selectAllInFileChooser() {
        try {
            MemorySegment dialog = fileChooserDialog();
            if (dialog.address() == 0) {
                return -1;
            }
            Dialog.GTK_FILE_CHOOSER_SELECT_ALL.invokeExact(dialog);
            return fileChooserSelectionCount();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** The {@code select-multiple} property of the one mapped {@code GtkFileChooserDialog}, or -1. */
    @SuppressWarnings("restricted")
    public static int fileChooserSelectMultiple() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dialog = fileChooserDialog();
            if (dialog.address() == 0) {
                return -1;
            }
            MemorySegment value = arena.allocate(JAVA_INT);
            Readback.G_OBJECT_GET_ONE.invokeExact(dialog, arena.allocateFrom("select-multiple"), value,
                    MemorySegment.NULL);
            return value.get(JAVA_INT, 0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * How many files the one mapped {@code GtkFileChooserDialog} has selected, or -1 when there is no such
     * dialog. A chooser fills its file list asynchronously, so a selection asked for right after the folder
     * arrived may not be there yet.
     */
    @SuppressWarnings("restricted")
    public static int fileChooserSelectionCount() {
        try {
            MemorySegment dialog = fileChooserDialog();
            if (dialog.address() == 0) {
                return -1;
            }
            MemorySegment names = (MemorySegment) Dialog.GTK_FILE_CHOOSER_GET_FILENAMES.invokeExact(dialog);
            int count = 0;
            for (MemorySegment node = names; node.address() != 0;
                    node = node.reinterpret(GSLIST_SIZE).get(ADDRESS, ADDRESS.byteSize())) {
                Dialog.G_FREE.invokeExact(node.reinterpret(GSLIST_SIZE).get(ADDRESS, 0));
                count++;
            }
            Dialog.G_SLIST_FREE.invokeExact(names);
            return count;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code GSList}: {@code gpointer data; GSList *next;}. */
    private static final long GSLIST_SIZE = 16;

    /** The one mapped {@code GtkFileChooserDialog} among GTK's toplevels, or {@code MemorySegment.NULL}. */
    @SuppressWarnings("restricted")
    private static MemorySegment fileChooserDialog() throws Throwable {
        MemorySegment dialog = MemorySegment.NULL;
        MemorySegment list = (MemorySegment) Dialog.GTK_WINDOW_LIST_TOPLEVELS.invokeExact();
        for (MemorySegment node = list; node.address() != 0; node = nextNode(node)) {
            MemorySegment window = node.reinterpret(GLIST_SIZE).get(ADDRESS, 0);
            if ((int) Dialog.GTK_WIDGET_GET_MAPPED.invokeExact(window) == 0) {
                continue;
            }
            MemorySegment type = (MemorySegment) Dialog.G_TYPE_NAME_FROM_INSTANCE.invokeExact(window);
            if (type.reinterpret(Long.MAX_VALUE).getString(0).equals("GtkFileChooserDialog")) {
                dialog = window;
            }
        }
        Dialog.G_LIST_FREE.invokeExact(list);
        return dialog;
    }

    @SuppressWarnings("restricted")
    private static String fileChooserState(MemorySegment dialog) throws Throwable {
        StringBuilder text = new StringBuilder();
        MemorySegment title = (MemorySegment) Dialog.GTK_WINDOW_GET_TITLE.invokeExact(dialog);
        text.append(title.address() == 0 ? "" : title.reinterpret(Long.MAX_VALUE).getString(0));
        MemorySegment folder = (MemorySegment) Dialog.GTK_FILE_CHOOSER_GET_CURRENT_FOLDER.invokeExact(dialog);
        text.append('|').append(folder.address() == 0 ? ""
                : folder.reinterpret(Long.MAX_VALUE).getString(0));
        if (folder.address() != 0) {
            Dialog.G_FREE.invokeExact(folder);
        }
        MemorySegment filters = (MemorySegment) Dialog.GTK_FILE_CHOOSER_LIST_FILTERS.invokeExact(dialog);
        for (MemorySegment node = filters; node.address() != 0; node = nextNode(node)) {
            MemorySegment filter = node.reinterpret(GLIST_SIZE).get(ADDRESS, 0);
            MemorySegment name = (MemorySegment) Dialog.GTK_FILE_FILTER_GET_NAME.invokeExact(filter);
            text.append('|').append(name.address() == 0 ? ""
                    : name.reinterpret(Long.MAX_VALUE).getString(0));
        }
        Dialog.G_LIST_FREE.invokeExact(filters);
        return text.toString();
    }

    /** The bytes of a C string before its terminator, in lower-case hex; empty for {@code NULL}. */
    @SuppressWarnings("restricted")
    private static String cStringHex(MemorySegment text) throws Throwable {
        if (text.address() == 0) {
            return "";
        }
        MemorySegment bytes = text.reinterpret(Long.MAX_VALUE);
        long length = 0;
        while (bytes.get(JAVA_BYTE, length) != 0) {
            length++;
        }
        return HexFormat.of().formatHex(bytes.asSlice(0, length).toArray(JAVA_BYTE));
    }

    /** {@code GList}: {@code gpointer data; GList *next; GList *prev;}. */
    private static final long GLIST_SIZE = 24;

    @SuppressWarnings("restricted")
    private static MemorySegment nextNode(MemorySegment node) {
        return node.reinterpret(GLIST_SIZE).get(ADDRESS, ADDRESS.byteSize());
    }

    /** {@code XA_CARDINAL} of {@code Xatom.h}. */
    private static final long XA_CARDINAL = 6;

    /** {@code PropModeReplace} of {@code X.h}. */
    private static final int PROP_MODE_REPLACE = 0;

    /** A copy of the {@code int[]} constant {@code name} of {@code GtkGlassNative}. */
    public static int[] javaTable(String name) {
        try {
            Field field = Class.forName("com.sun.glass.ui.gtk.GtkGlassNative").getDeclaredField(name);
            field.setAccessible(true);
            return ((int[]) field.get(null)).clone();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code ClientMessage} of {@code X.h}. */
    private static final int CLIENT_MESSAGE = 33;

    /** {@code sizeof(XEvent)} on LP64: the union is padded to 24 {@code long}s. */
    private static final long X_EVENT_SIZE = 192;

    /** {@code SubstructureNotifyMask | SubstructureRedirectMask} of {@code X.h}. */
    private static final long SUBSTRUCTURE_MASKS = (1L << 19) | (1L << 20);

    /** {@code XA_STRING} and {@code XA_WM_CLASS} of {@code Xatom.h}. */
    private static final long XA_STRING = 31;
    private static final long XA_WM_CLASS = 67;

    /**
     * Sends the X11 window {@code xid} the {@code WM_PROTOCOLS} / {@code WM_DELETE_WINDOW} client message a window
     * manager sends when the user closes a window, and waits for the server; GDK turns it into {@code GDK_DELETE}.
     * On the FX thread (GDK's display connection).
     */
    public static void sendDeleteRequest(long xid) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            long protocols = (long) Readback.X_INTERN_ATOM.invokeExact(display, arena.allocateFrom("WM_PROTOCOLS"), 0);
            long delete = (long) Readback.X_INTERN_ATOM.invokeExact(display, arena.allocateFrom("WM_DELETE_WINDOW"),
                    0);
            MemorySegment event = clientMessage(arena, display, xid, protocols, delete, 0, 0, 0);
            int status = (int) EventDriver.X_SEND_EVENT.invokeExact(display, xid, 0, 0L, event);
            int synced = (int) Readback.X_SYNC.invokeExact(display, 0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * Asks the window manager, as a pager or the user would, to add or remove the {@code _NET_WM_STATE} atom
     * {@code state} (for example {@code _NET_WM_STATE_ABOVE}) of the X11 window {@code xid}: the EWMH client message
     * to the root window. Without a window manager nothing happens. FX thread.
     */
    public static void requestNetWmState(long xid, boolean add, String state) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            long root = (long) Readback.GDK_X11_GET_DEFAULT_ROOT_XWINDOW.invokeExact();
            long wmState = (long) Readback.X_INTERN_ATOM.invokeExact(display, arena.allocateFrom("_NET_WM_STATE"), 0);
            long atom = (long) Readback.X_INTERN_ATOM.invokeExact(display, arena.allocateFrom(state), 0);
            // EWMH: action, first property, no second property, source indication 1 = an application
            MemorySegment event = clientMessage(arena, display, xid, wmState, add ? 1 : 0, atom, 0, 1);
            int status = (int) EventDriver.X_SEND_EVENT.invokeExact(display, root, 0, SUBSTRUCTURE_MASKS, event);
            int synced = (int) Readback.X_SYNC.invokeExact(display, 0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private static MemorySegment clientMessage(Arena arena, MemorySegment display, long window, long type,
                                               long l0, long l1, long l2, long l3) {
        MemorySegment event = arena.allocate(X_EVENT_SIZE, 8);
        event.set(JAVA_INT, 0, CLIENT_MESSAGE);
        event.set(ADDRESS, 24, display);
        event.set(JAVA_LONG, 32, window);
        event.set(JAVA_LONG, 40, type);
        event.set(JAVA_INT, 48, 32);
        event.set(JAVA_LONG, 56, l0);
        event.set(JAVA_LONG, 64, l1);
        event.set(JAVA_LONG, 72, l2);
        event.set(JAVA_LONG, 80, l3);
        return event;
    }

    /**
     * The atom names of the {@code _NET_WM_STATE} property of the X11 window {@code xid}, joined with {@code |}, or
     * {@code none}: what the window manager says the window's state is; FX thread.
     */
    @SuppressWarnings("restricted")
    public static String netWmState(long xid) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            long property = (long) Readback.X_INTERN_ATOM.invokeExact(display, arena.allocateFrom("_NET_WM_STATE"), 0);
            MemorySegment type = arena.allocate(JAVA_LONG);
            MemorySegment format = arena.allocate(JAVA_INT);
            MemorySegment count = arena.allocate(JAVA_LONG);
            MemorySegment after = arena.allocate(JAVA_LONG);
            MemorySegment data = arena.allocate(ADDRESS);
            int status = (int) Readback.X_GET_WINDOW_PROPERTY.invokeExact(display, xid, property, 0L, 64L, 0,
                    XA_ATOM, type, format, count, after, data);
            MemorySegment items = data.get(ADDRESS, 0);
            long n = count.get(JAVA_LONG, 0);
            List<String> names = new ArrayList<>();
            if (status == 0 && items.address() != 0) {
                MemorySegment atoms = items.reinterpret(n * JAVA_LONG.byteSize());
                for (long i = 0; i < n; i++) {
                    MemorySegment name = (MemorySegment) EventDriver.X_GET_ATOM_NAME.invokeExact(display,
                            atoms.getAtIndex(JAVA_LONG, i));
                    names.add(name.reinterpret(Long.MAX_VALUE).getString(0));
                    int freed = (int) Readback.X_FREE.invokeExact(name);
                }
            }
            if (items.address() != 0) {
                int freed = (int) Readback.X_FREE.invokeExact(items);
            }
            return names.isEmpty() ? "none" : String.join("|", names);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code XA_ATOM} of {@code Xatom.h}. */
    private static final long XA_ATOM = 4;

    /**
     * Where the X11 window {@code xid} starts on the root window, as {@code {x, y}}: {@code XTranslateCoordinates}
     * of its origin on GDK's display. Under a window manager this is the client window inside the frame, whatever the
     * glass geometry says; FX thread.
     */
    public static int[] rootPosition(long xid) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            long root = (long) Readback.GDK_X11_GET_DEFAULT_ROOT_XWINDOW.invokeExact();
            MemorySegment x = arena.allocate(JAVA_INT);
            MemorySegment y = arena.allocate(JAVA_INT);
            MemorySegment child = arena.allocate(JAVA_LONG);
            int ok = (int) EventDriver.X_TRANSLATE_COORDINATES.invokeExact(display, xid, root, 0, 0, x, y, child);
            if (ok == 0) {
                throw new IllegalStateException("XTranslateCoordinates failed for window " + xid);
            }
            return new int[] {x.get(JAVA_INT, 0), y.get(JAVA_INT, 0)};
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code XSetInputFocus(display, xid, RevertToParent, CurrentTime)} and {@code XSync}: gives the X11 window
     * {@code xid} the keyboard focus the way a window manager would, whether or not glass would; FX thread.
     */
    public static void setInputFocus(long xid) {
        try {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            int status = (int) EventDriver.X_SET_INPUT_FOCUS.invokeExact(display, xid, 2, 0L);
            int synced = (int) Readback.X_SYNC.invokeExact(display, 0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code XTestFakeButtonEvent} of an arbitrary X11 button - 6 and 7 are the horizontal wheel, which the glass
     * robot cannot press - and {@code XSync}; FX thread.
     */
    public static void fakeButton(int button, boolean press) {
        try {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            int status = (int) EventDriver.X_TEST_FAKE_BUTTON_EVENT.invokeExact(display, button, press ? 1 : 0, 0L);
            int synced = (int) Readback.X_SYNC.invokeExact(display, 0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code XTestFakeKeyEvent} of the key that {@code XKeysymToKeycode} finds for the keysym {@code name} (for
     * example {@code Menu}), and {@code XSync}; answers the keycode, 0 when the keymap has none. FX thread.
     */
    public static int fakeKeysym(String name, boolean press) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            long keysym = (long) EventDriver.X_STRING_TO_KEYSYM.invokeExact(arena.allocateFrom(name));
            int keycode = Byte.toUnsignedInt((byte) EventDriver.X_KEYSYM_TO_KEYCODE.invokeExact(display, keysym));
            if (keycode != 0) {
                int status = (int) EventDriver.X_TEST_FAKE_KEY_EVENT.invokeExact(display, keycode, press ? 1 : 0, 0L);
                int synced = (int) Readback.X_SYNC.invokeExact(display, 0);
            }
            return keycode;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * {@code XkbSetAutoRepeatRate} of the core keyboard: a key held down repeats first after {@code delay} ms,
     * then every {@code interval} ms. Answers the previous {@code {delay, interval}}. The X server's state, shared by
     * every client of the display: restore it. FX thread.
     */
    public static int[] setAutoRepeatRate(int delay, int interval) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            MemorySegment previousDelay = arena.allocate(JAVA_INT);
            MemorySegment previousInterval = arena.allocate(JAVA_INT);
            int got = (int) EventDriver.XKB_GET_AUTO_REPEAT_RATE.invokeExact(display, XKB_USE_CORE_KBD, previousDelay,
                    previousInterval);
            int set = (int) EventDriver.XKB_SET_AUTO_REPEAT_RATE.invokeExact(display, XKB_USE_CORE_KBD, delay,
                    interval);
            int synced = (int) Readback.X_SYNC.invokeExact(display, 0);
            if (got == 0 || set == 0) {
                throw new IllegalStateException("XkbGetAutoRepeatRate/XkbSetAutoRepeatRate failed: " + got + ", "
                        + set);
            }
            return new int[] {previousDelay.get(JAVA_INT, 0), previousInterval.get(JAVA_INT, 0)};
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code XkbUseCoreKbd} of {@code XKB.h}. */
    private static final int XKB_USE_CORE_KBD = 0x0100;

    /** The {@code WM_CLASS} property of the X11 window {@code xid} as {@code instance|class}, or {@code none}. */
    public static String wmClass(long xid) {
        String text = stringProperty(xid, XA_WM_CLASS, null);
        return text == null ? "none" : text;
    }

    /**
     * The {@code STRING} property {@code name} of the root window - {@code _XKB_RULES_NAMES} names the keyboard
     * model and layout - with its NUL separators written as {@code |}, or {@code none}; FX thread.
     */
    public static String rootStringProperty(String name) {
        try {
            long root = (long) Readback.GDK_X11_GET_DEFAULT_ROOT_XWINDOW.invokeExact();
            String text = stringProperty(root, 0, name);
            return text == null ? "none" : text;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @SuppressWarnings("restricted")
    private static String stringProperty(long xid, long atom, String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            long property = name == null ? atom
                    : (long) Readback.X_INTERN_ATOM.invokeExact(display, arena.allocateFrom(name), 1);
            if (property == 0) {
                return null;
            }
            MemorySegment type = arena.allocate(JAVA_LONG);
            MemorySegment format = arena.allocate(JAVA_INT);
            MemorySegment count = arena.allocate(JAVA_LONG);
            MemorySegment after = arena.allocate(JAVA_LONG);
            MemorySegment data = arena.allocate(ADDRESS);
            int status = (int) Readback.X_GET_WINDOW_PROPERTY.invokeExact(display, xid, property, 0L, 1024L, 0,
                    XA_STRING, type, format, count, after, data);
            MemorySegment items = data.get(ADDRESS, 0);
            long n = count.get(JAVA_LONG, 0);
            String result = null;
            if (status == 0 && items.address() != 0 && format.get(JAVA_INT, 0) == 8) {
                byte[] bytes = items.reinterpret(n).toArray(JAVA_BYTE);
                int end = bytes.length;
                while (end > 0 && bytes[end - 1] == 0) {
                    end--;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < end; i++) {
                    sb.append(bytes[i] == 0 ? '|' : (char) (bytes[i] & 0xFF));
                }
                result = sb.toString();
            }
            if (items.address() != 0) {
                int freed = (int) Readback.X_FREE.invokeExact(items);
            }
            return result;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * A private connection to the X server of {@code DISPLAY} ({@code XOpenDisplay(NULL)}), for injecting input from
     * a thread that is not the FX thread; {@code 0} if it cannot be opened. Used by one thread only, then closed with
     * {@link #closeDisplay}.
     */
    public static long openDisplay() {
        try {
            MemorySegment display = (MemorySegment) EventDriver.X_OPEN_DISPLAY.invokeExact(MemorySegment.NULL);
            return display.address();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code XTestFakeMotionEvent(display, -1, x, y, CurrentTime)} on a connection of {@link #openDisplay}. */
    public static void fakeMotion(long display, int x, int y) {
        try {
            int status = (int) EventDriver.X_TEST_FAKE_MOTION_EVENT.invokeExact(MemorySegment.ofAddress(display), -1,
                    x, y, 0L);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code XFlush} of a connection of {@link #openDisplay}. */
    public static void flushDisplay(long display) {
        try {
            int status = (int) EventDriver.X_FLUSH.invokeExact(MemorySegment.ofAddress(display));
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code XSync(display, False)} of a connection of {@link #openDisplay}. */
    public static void syncDisplay(long display) {
        try {
            int status = (int) Readback.X_SYNC.invokeExact(MemorySegment.ofAddress(display), 0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code XCloseDisplay} of a connection of {@link #openDisplay}. */
    public static void closeDisplay(long display) {
        try {
            int status = (int) EventDriver.X_CLOSE_DISPLAY.invokeExact(MemorySegment.ofAddress(display));
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code XSync} of GDK's own display connection; FX thread. */
    public static void syncGdkDisplay() {
        try {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            int synced = (int) Readback.X_SYNC.invokeExact(display, 0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * Puts {@code before} in front of the {@code Screen.EventHandler} that the toolkit installed, so that a scenario
     * sees every {@code Screen.notifySettingsChanged} the GTK event code makes; {@code before} may throw, which then
     * leaves {@code Screen.notifySettingsChanged} as a throw from the handler would. FX thread.
     */
    public static void wrapScreenEventHandler(Runnable before) {
        try {
            Field field = Screen.class.getDeclaredField("eventHandler");
            field.setAccessible(true);
            Screen.EventHandler previous = (Screen.EventHandler) field.get(null);
            Screen.setEventHandler(new Screen.EventHandler() {
                @Override
                public void handleSettingsChanged() {
                    before.run();
                    if (previous != null) {
                        previous.handleSettingsChanged();
                    }
                }
            });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The drag-and-drop clipboard peer of the process, which {@code Clipboard.get("DND")} creates on first use. The
     * methods below call the peer itself, not {@code SystemClipboard.getData}, which answers from the local data
     * whenever this process is the drag source.
     */
    private static GtkDnDClipboard dndClipboard() {
        try {
            Method get = Clipboard.class.getDeclaredMethod("get", String.class);
            get.setAccessible(true);
            return (GtkDnDClipboard) get.invoke(null, Clipboard.DND);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code GtkDnDClipboard.mimesFromSystem}, the peer method itself; on the FX thread. */
    public static String[] dndMimesFromSystem() {
        return dndClipboard().mimesFromSystem();
    }

    /** {@code GtkDnDClipboard.popFromSystem}, the peer method itself; on the FX thread. */
    public static Object dndPopFromSystem(String mimeType) {
        return dndClipboard().popFromSystem(mimeType);
    }

    /** {@code GtkDnDClipboard.supportedSourceActionsFromSystem}, the peer method itself; on the FX thread. */
    public static int dndSupportedSourceActions() {
        return dndClipboard().supportedSourceActionsFromSystem();
    }

    /** {@code GtkDnDClipboard.isOwner}, the peer method itself; on the FX thread. */
    public static boolean dndIsOwner() {
        return dndClipboard().isOwner();
    }

    /**
     * How many of the JNI entry points of the event and drag-and-drop code that {@code libglassgtk3.so} exported at
     * commit {@code 033187ad90} this process's library still exports, as {@code <found>/<checked>}.
     */
    public static String eventJniEntryPoints() {
        List<String> names = List.of("Java_com_sun_glass_ui_gtk_GtkWindow__1createWindow",
                "Java_com_sun_glass_ui_gtk_GtkWindow__1setBounds", "Java_com_sun_glass_ui_gtk_GtkView__1create",
                "Java_com_sun_glass_ui_gtk_GtkApplication__1init", "Java_com_sun_glass_ui_gtk_GtkApplication__1runLoop",
                "Java_com_sun_glass_ui_gtk_GtkDnDClipboard_pushToSystemImpl");
        int found = 0;
        for (String name : names) {
            if (SymbolLookup.loaderLookup().find(name).isPresent()) {
                found++;
            }
        }
        return found + "/" + names.size();
    }

    // ---------------------------------------------------------------------------------------------
    // Callback tables (glass_gtk_api.h)
    // ---------------------------------------------------------------------------------------------

    /** The status every recording slot of {@link #fireThroughRecordingTables} answers. */
    public static final int RECORDING_STATUS = 7;

    /** The window / view id {@link #fireThroughRecordingTables} passes: bits above 32 set. */
    public static final long RECORDING_ID = 0x1_0000_0002L;

    /** The value every out-parameter is filled with before a slot is fired, per byte. */
    private static final byte SENTINEL = (byte) 0x5A;

    /** The slot numbers of {@code ggtk_test_fire_callback}, table by table in the facade's order. */
    public static final List<Integer> APP_SLOTS = List.of(0, 1);
    public static final List<Integer> WINDOW_SLOTS = List.of(100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
            111);
    public static final List<Integer> VIEW_SLOTS = List.of(200, 201, 202, 203, 204, 205, 206, 207, 208, 209);
    public static final List<Integer> DND_SLOTS = List.of(300, 301, 302, 303, 304);

    /** {@code GLASS_GTK_ABI_VERSION} as {@code GtkGlassNative} expects it. */
    public static int facadeAbiVersion() {
        return GtkGlassNative.ABI_VERSION;
    }

    /** {@code ggtk_abi_version()} as the loaded library answers it. */
    public static int libraryAbiVersion() {
        try {
            return (int) Tables.ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** Whether {@code GtkGlassNative.installCallbacks} has run. */
    public static boolean callbacksInstalled() {
        return GtkGlassNative.callbacksInstalled();
    }

    /** The number of distinct production stubs installed, and whether each table has one per slot. */
    public static String installedStubs() {
        List<List<MemorySegment>> tables = GtkGlassNative.installedCallbackStubs();
        Set<Long> distinct = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (int t = 0; t < tables.size(); t++) {
            for (MemorySegment stub : tables.get(t)) {
                distinct.add(stub.address());
            }
            sb.append(t == 0 ? "" : " ").append(tables.get(t).size());
        }
        return sb.append(" distinct=").append(distinct.size()).toString();
    }

    /**
     * The four callback tables and {@code GgtkDndData} as the facade lays them out, with the {@code sizeof} the C
     * compiler gave each: {@code <struct> size=<java> c=<c>}, then {@code <slot>@<offset>} per member.
     */
    public static List<String> callbackLayouts() {
        List<String> lines = new ArrayList<>();
        int[] sizes = GtkGlassNative.callbackLayoutSizes();
        String[] names = {"GgtkAppCallbacks", "GgtkWindowCallbacks", "GgtkViewCallbacks", "GgtkDndCallbacks"};
        for (int t = 0; t < GtkGlassNative.CALLBACK_TABLES.size(); t++) {
            GtkGlassNative.CallbackTable table = GtkGlassNative.CALLBACK_TABLES.get(t);
            lines.add(names[t] + " size=" + table.layout().byteSize() + " c=" + sizes[t]);
            for (String slot : table.slots()) {
                lines.add(slot + "@" + table.layout().byteOffset(PathElement.groupElement(slot)));
            }
        }
        lines.add("GgtkDndData size=" + GtkGlassNative.GGTK_DND_DATA_LAYOUT.byteSize() + " c=" + sizes[4]);
        for (String field : List.of("kind", "count", "data", "pixbuf")) {
            lines.add(field + "@" + GtkGlassNative.GGTK_DND_DATA_LAYOUT.byteOffset(
                    PathElement.groupElement(field)));
        }
        return lines;
    }

    /**
     * Installs recording tables whose stubs are built from the facade's own slot descriptors, dials every slot
     * through {@code ggtk_test_fire_callback} with the header's fixed patterns, then a table with a {@code NULL} slot
     * and an unknown slot number, and restores the production tables ({@code GtkGlassNative.reinstallCallbacks}). A
     * descriptor that disagrees with the C prototype shows as a garbled pattern. Answers, per slot, the call the
     * recorder saw - {@code <table>.<slot>(<args>)}, pointers decoded where the header says what they hold - and
     * {@code fire <n> -> <status> <outs>}: what the hook returned and what the recorder's writes left in the
     * out-parameters the caller passed. FX thread, so that no event reaches the recording tables.
     */
    public static List<String> fireThroughRecordingTables() {
        List<String> lines = new ArrayList<>();
        List<MemorySegment[]> stubs = new ArrayList<>();
        String[] prefixes = {"app.", "window.", "view.", "dnd."};
        for (int t = 0; t < GtkGlassNative.CALLBACK_TABLES.size(); t++) {
            GtkGlassNative.CallbackTable table = GtkGlassNative.CALLBACK_TABLES.get(t);
            MemorySegment[] tableStubs = new MemorySegment[table.slots().size()];
            for (int i = 0; i < tableStubs.length; i++) {
                tableStubs[i] = Recorder.stub(lines, prefixes[t] + table.slots().get(i),
                        table.descriptors().get(i));
            }
            stubs.add(tableStubs);
        }
        try {
            GtkGlassNative.writeCallbackTables(stubs);
            List<Integer> all = new ArrayList<>(APP_SLOTS);
            all.addAll(WINDOW_SLOTS);
            all.addAll(VIEW_SLOTS);
            all.addAll(DND_SLOTS);
            for (int slot : all) {
                long id = slot == 304 ? GtkGlassNative.DND_AS_RAW : RECORDING_ID;
                lines.add(fireAndRead(slot, id));
            }
            lines.add("fire 999 -> " + fire(999, RECORDING_ID));
            stubs.get(2)[6] = null;
            GtkGlassNative.writeCallbackTables(stubs);
            lines.add("fire 206 with notify_key NULL -> " + fire(206, RECORDING_ID));
        } finally {
            GtkGlassNative.reinstallCallbacks();
        }
        return lines;
    }

    /**
     * Dials slot {@code slot} of the installed tables with {@code id} and out-parameters filled with
     * {@link #SENTINEL}; answers {@code fire <slot> -> <status> <outs>}, the outs as the slot left them. For slot 1
     * a name the production slot allocated is read and freed.
     */
    public static String fireAndRead(int slot, long id) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(GtkGlassNative.GGTK_DND_DATA_LAYOUT.byteSize(), 8);
            MemorySegment out2 = arena.allocate(8, 8);
            out.fill(SENTINEL);
            out2.fill(SENTINEL);
            if (slot == 304) {
                out.fill((byte) 0); // dnd_source_pull zero-fills the GgtkDndData before it dials the slot
            }
            int status = (int) Tables.TEST_FIRE_CALLBACK.invokeExact(slot, id, out, out2);
            return "fire " + slot + " -> " + status + readOuts(slot, out, out2);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private static int fire(int slot, long id) {
        return Integer.parseInt(fireAndRead(slot, id).split(" ")[3]);
    }

    private static String readOuts(int slot, MemorySegment out, MemorySegment out2) throws Throwable {
        int sentinelInt = 0x5A5A5A5A;
        return switch (slot) {
            case 1 -> {
                long address = out.get(JAVA_LONG, 0);
                if (address == 0x5A5A5A5A5A5A5A5AL || address == 0 || address == 0x1234) {
                    yield " out=0x" + Long.toHexString(address);
                }
                String name = cString(MemorySegment.ofAddress(address));
                Tables.G_FREE.invokeExact(MemorySegment.ofAddress(address));
                yield " out=\"" + name + "\"";
            }
            case 100, 111, 300, 301 -> {
                int value = out.get(JAVA_INT, 0);
                yield " out=" + (value == sentinelInt ? "untouched" : Integer.toString(value));
            }
            case 209 -> {
                long x = out.get(JAVA_LONG, 0);
                long valid = Integer.toUnsignedLong(out2.get(JAVA_INT, 0));
                yield x == 0x5A5A5A5A5A5A5A5AL && valid == 0x5A5A5A5AL ? " xy=untouched valid=untouched"
                        : " xy=" + out.get(JAVA_DOUBLE, 0) + "," + out.get(JAVA_DOUBLE, 8) + " valid="
                        + (valid == 0x5A5A5A5AL ? "untouched" : Long.toString(valid));
            }
            case 304 -> " kind=" + out.get(JAVA_INT, 0) + " count=" + out.get(JAVA_INT, 4);
            default -> "";
        };
    }

    /** The bytes of the NUL-terminated C string at {@code s}, as ISO-8859-1 characters. */
    @SuppressWarnings("restricted")
    private static String cString(MemorySegment s) {
        MemorySegment wide = s.reinterpret(Long.MAX_VALUE);
        StringBuilder sb = new StringBuilder();
        for (long i = 0; wide.get(JAVA_BYTE, i) != 0; i++) {
            sb.append((char) Byte.toUnsignedInt(wide.get(JAVA_BYTE, i)));
        }
        return sb.toString();
    }

    /**
     * {@code GtkGlassNative.onSourceGetData} through the installed dnd table ({@code ggtk_test_fire_callback} slot
     * 304, key {@code "text/plain"}) with the drag data map {@code data} in progress and the conversion {@code as};
     * answers {@code status=<s> kind=<k> count=<n> data=<hex>|NULL pixbuf=<w>x<h>|NULL}, and frees what the slot
     * handed over as the C would. FX thread.
     */
    public static String sourceGetData(HashMap<String, Object> data, int as) {
        HashMap<String, Object> previous = GtkDnDClipboard.dragInProgress();
        try (Arena arena = Arena.ofConfined()) {
            Tables.DRAG_IN_PROGRESS.set(data);
            MemorySegment out = arena.allocate(GtkGlassNative.GGTK_DND_DATA_LAYOUT);
            int status = (int) Tables.TEST_FIRE_CALLBACK.invokeExact(304, (long) as, out, MemorySegment.NULL);
            int kind = out.get(JAVA_INT, 0);
            int count = out.get(JAVA_INT, 4);
            MemorySegment block = out.get(ADDRESS, 8);
            MemorySegment pixbuf = out.get(ADDRESS, 16);
            StringBuilder sb = new StringBuilder("status=" + status + " kind=" + kind + " count=" + count);
            if (block.address() == 0) {
                sb.append(" data=NULL");
            } else {
                int length = switch (kind) {
                    case 1 -> count + 1;
                    case 4 -> stringsLength(block, count);
                    default -> count;
                };
                sb.append(" data=").append(HexFormat.of().formatHex(bytes(block, length)));
                Tables.G_FREE.invokeExact(block);
            }
            if (pixbuf.address() == 0) {
                sb.append(" pixbuf=NULL");
            } else {
                sb.append(" pixbuf=").append(describePixbuf(pixbuf, true));
            }
            return sb.toString();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        } finally {
            Tables.DRAG_IN_PROGRESS.set(previous);
        }
    }

    @SuppressWarnings("restricted")
    private static byte[] bytes(MemorySegment block, long length) {
        return block.reinterpret(length).toArray(JAVA_BYTE);
    }

    /** The byte length of {@code count} NUL-terminated strings back to back at {@code block}, terminators included. */
    @SuppressWarnings("restricted")
    private static int stringsLength(MemorySegment block, int count) {
        MemorySegment wide = block.reinterpret(Long.MAX_VALUE);
        int length = 0;
        for (int i = 0; i < count; i++) {
            while (wide.get(JAVA_BYTE, length) != 0) {
                length++;
            }
            length++;
        }
        return length;
    }

    /** The id {@code GtkWindow} registered {@code window} under. */
    public static long windowId(Window window) {
        return ((GtkWindow) window).windowId();
    }

    /** The id {@code GtkView} registered {@code view} under. */
    public static long viewId(View view) {
        return ((GtkView) view).viewId();
    }

    /** {@code ggtk_window_get_id} of the {@code WindowContext} of {@code window}; FX thread. */
    public static long cWindowId(Window window) {
        try {
            return (long) Tables.WINDOW_GET_ID.invokeExact(MemorySegment.ofAddress(window.getNativeHandle()));
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code ggtk_window_get_view_id} of the {@code WindowContext} of {@code window}; FX thread. */
    public static long cWindowViewId(Window window) {
        try {
            return (long) Tables.WINDOW_GET_VIEW_ID.invokeExact(MemorySegment.ofAddress(window.getNativeHandle()));
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code ggtk_view_get_id} of the {@code GlassView} of {@code view}. */
    public static long cViewId(View view) {
        try {
            long ptr = (long) Tables.VIEW_PTR.get(view);
            return (long) Tables.VIEW_GET_ID.invokeExact(MemorySegment.ofAddress(ptr));
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** How many windows {@code GtkWindow}'s registry holds. */
    public static int windowRegistrySize() {
        return GtkWindow.windowRegistrySize();
    }

    /** How many views {@code GtkView}'s registry holds. */
    public static int viewRegistrySize() {
        return GtkView.viewRegistrySize();
    }

    /** Whether {@code GtkView}'s registry holds {@code id}. */
    public static boolean isViewRegistered(long id) {
        return GtkView.isRegistered(id);
    }

    // ---------------------------------------------------------------------------------------------
    // Window natives, and what GTK and the X server made of them
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code GtkWindow._setTitle} itself, not {@code Window.setTitle}, which turns {@code null} into {@code ""} and
     * skips a title equal to the last one; FX thread.
     */
    public static boolean setTitleNative(Window window, String title) {
        return ((GtkWindow) window)._setTitle(window.getNativeHandle(), title);
    }

    /** {@code GtkWindow._setMinimumSize} itself, which {@code Window.setMinimumSize} calls only for sizes >= 0. */
    public static boolean setMinimumSizeNative(Window window, int width, int height) {
        return ((GtkWindow) window)._setMinimumSize(window.getNativeHandle(), width, height);
    }

    /** {@code GtkWindow._setMaximumSize} itself; {@code Window.setMaximumSize} passes -1 for Integer.MAX_VALUE. */
    public static boolean setMaximumSizeNative(Window window, int width, int height) {
        return ((GtkWindow) window)._setMaximumSize(window.getNativeHandle(), width, height);
    }

    /**
     * {@code GtkWindow._setSystemMinimumSize}, which only the header-button overlay of an {@code EXTENDED} window
     * calls; private, so through a private lookup. FX thread.
     */
    public static boolean setSystemMinimumSizeNative(Window window, int width, int height) {
        try {
            MethodHandle native0 = MethodHandles.privateLookupIn(GtkWindow.class, MethodHandles.lookup())
                    .findVirtual(GtkWindow.class, "_setSystemMinimumSize",
                            MethodType.methodType(boolean.class, long.class, int.class, int.class));
            return (boolean) native0.invoke((GtkWindow) window, window.getNativeHandle(), width, height);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * The title {@code gtk_window_get_title} answers for the {@code GtkWindow} of {@code window}, as the hex of its
     * UTF-8 bytes, or {@code null} for a {@code NULL} title. The {@code GtkWindow} is found from the window's X11
     * window: {@code gdk_x11_window_lookup_for_display}, {@code gdk_window_get_user_data},
     * {@code gtk_widget_get_toplevel}. FX thread, the window realized.
     */
    @SuppressWarnings("restricted")
    public static String gtkWindowTitleHex(Window window) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_DISPLAY_GET_DEFAULT.invokeExact();
            MemorySegment gdkWindow = (MemorySegment) Readback.GDK_X11_WINDOW_LOOKUP_FOR_DISPLAY.invokeExact(display,
                    window.getNativeWindow());
            if (gdkWindow.address() == 0) {
                throw new IllegalStateException("no GdkWindow for X11 window " + window.getNativeWindow());
            }
            MemorySegment userData = arena.allocate(ADDRESS);
            Readback.GDK_WINDOW_GET_USER_DATA.invokeExact(gdkWindow, userData);
            MemorySegment toplevel = (MemorySegment) Readback.GTK_WIDGET_GET_TOPLEVEL.invokeExact(
                    userData.get(ADDRESS, 0));
            MemorySegment title = (MemorySegment) Dialog.GTK_WINDOW_GET_TITLE.invokeExact(toplevel);
            return title.address() == 0 ? null : HexFormat.of().formatHex(nulTerminated(title));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** The bytes of the NUL-terminated C string at {@code s}, terminator excluded. */
    @SuppressWarnings("restricted")
    private static byte[] nulTerminated(MemorySegment s) {
        MemorySegment wide = s.reinterpret(Long.MAX_VALUE);
        long length = 0;
        while (wide.get(JAVA_BYTE, length) != 0) {
            length++;
        }
        return wide.asSlice(0, length).toArray(JAVA_BYTE);
    }

    /**
     * The {@code _NET_WM_NAME} property of the X11 window {@code xid} - the title as the window manager reads it,
     * UTF-8 - as hex, or {@code none}; FX thread.
     */
    public static String netWmNameHex(long xid) {
        long[] items = windowProperty(xid, "_NET_WM_NAME", 8);
        if (items == null) {
            return "none";
        }
        byte[] bytes = new byte[items.length];
        for (int i = 0; i < items.length; i++) {
            bytes[i] = (byte) items[i];
        }
        return HexFormat.of().formatHex(bytes);
    }

    /** {@code XSizeHints} flags of {@code Xutil.h}. */
    private static final long P_MIN_SIZE = 1 << 4;
    private static final long P_MAX_SIZE = 1 << 5;

    /**
     * The size limits the {@code WM_NORMAL_HINTS} property of the X11 window {@code xid} gives the window manager:
     * {@code min=<w>,<h>} if {@code PMinSize} is set, {@code max=<w>,<h>} if {@code PMaxSize} is, blank-separated,
     * {@code none} without the property; FX thread.
     */
    public static String wmNormalHints(long xid) {
        long[] items = windowProperty(xid, "WM_NORMAL_HINTS", 32);
        if (items == null || items.length < 9) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        if ((items[0] & P_MIN_SIZE) != 0) {
            parts.add("min=" + items[5] + "," + items[6]);
        }
        if ((items[0] & P_MAX_SIZE) != 0) {
            parts.add("max=" + items[7] + "," + items[8]);
        }
        return parts.isEmpty() ? "none" : String.join(" ", parts);
    }

    /**
     * The items of the property {@code name} of the X11 window {@code xid}, of any type, when its format is
     * {@code format} ({@code 8}: bytes, {@code 32}: the C {@code long}s Xlib hands back for 32-bit items), each as a
     * {@code long}; {@code null} without the property or in another format.
     */
    @SuppressWarnings("restricted")
    private static long[] windowProperty(long xid, String name, int format) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment display = (MemorySegment) Readback.GDK_X11_GET_DEFAULT_XDISPLAY.invokeExact();
            long property = (long) Readback.X_INTERN_ATOM.invokeExact(display, arena.allocateFrom(name), 1);
            if (property == 0) {
                return null;
            }
            MemorySegment type = arena.allocate(JAVA_LONG);
            MemorySegment actualFormat = arena.allocate(JAVA_INT);
            MemorySegment count = arena.allocate(JAVA_LONG);
            MemorySegment after = arena.allocate(JAVA_LONG);
            MemorySegment data = arena.allocate(ADDRESS);
            int status = (int) Readback.X_GET_WINDOW_PROPERTY.invokeExact(display, xid, property, 0L, 1024L, 0,
                    ANY_PROPERTY_TYPE, type, actualFormat, count, after, data);
            MemorySegment items = data.get(ADDRESS, 0);
            try {
                if (status != 0 || items.address() == 0 || actualFormat.get(JAVA_INT, 0) != format) {
                    return null;
                }
                int n = (int) count.get(JAVA_LONG, 0);
                long[] values = new long[n];
                MemorySegment block = items.reinterpret(format == 8 ? n : (long) n * JAVA_LONG.byteSize());
                for (int i = 0; i < n; i++) {
                    values[i] = format == 8 ? Byte.toUnsignedLong(block.get(JAVA_BYTE, i))
                            : block.getAtIndex(JAVA_LONG, i);
                }
                return values;
            } finally {
                if (items.address() != 0) {
                    int freed = (int) Readback.X_FREE.invokeExact(items);
                }
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** {@code AnyPropertyType} of {@code X.h}. */
    private static final long ANY_PROPERTY_TYPE = 0;

    // ---------------------------------------------------------------------------------------------
    // Frame uploads
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code GtkView._uploadPixelsIntArray} itself, past {@code View.uploadPixels} and {@code Pixels}, whose
     * constructor refuses a buffer smaller than its frame; private, so through a private lookup. FX thread.
     */
    public static void uploadPixelsIntArray(View view, int[] pixels, int offset, int width, int height) {
        invokeUpload("_uploadPixelsIntArray", int[].class, view, pixels, offset, width, height);
    }

    /** {@code GtkView._uploadPixelsByteArray} itself, as {@link #uploadPixelsIntArray}. FX thread. */
    public static void uploadPixelsByteArray(View view, byte[] pixels, int offset, int width, int height) {
        invokeUpload("_uploadPixelsByteArray", byte[].class, view, pixels, offset, width, height);
    }

    private static void invokeUpload(String name, Class<?> arrayType, View view, Object pixels, int offset,
                                     int width, int height) {
        try {
            MethodHandle upload = MethodHandles.privateLookupIn(GtkView.class, MethodHandles.lookup())
                    .findVirtual(GtkView.class, name, MethodType.methodType(void.class, long.class, arrayType,
                            int.class, int.class, int.class));
            upload.invoke((GtkView) view, (long) Tables.VIEW_PTR.get(view), pixels, offset, width, height);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** The size of the block {@code view}'s frames are copied into ({@code GtkGlassNative.UploadStaging}). */
    public static long uploadStagingBytes(View view) {
        return ((GtkView) view).uploadStagingBytes();
    }

    /** {@code GtkGlassNative.frameLength}: the frame the upload copies, -1 where the C refuses the frame. */
    public static int frameLength(int length, int offset, int width, int height, int unit) {
        return GtkGlassNative.frameLength(length, offset, width, height, unit);
    }

    // ---------------------------------------------------------------------------------------------
    // Tables with NULL slots
    // ---------------------------------------------------------------------------------------------

    /**
     * Installs tables whose slots are all {@code NULL} except the production stubs of {@code kept}
     * ({@code <table>.<slot>} names, {@code window.is_enabled} for example): from here on the library makes no call
     * for every other slot. Undo with {@link #reinstallProductionTables}. FX thread.
     */
    public static void installNullTablesExcept(Set<String> kept) {
        String[] prefixes = {"app.", "window.", "view.", "dnd."};
        List<List<MemorySegment>> production = GtkGlassNative.installedCallbackStubs();
        List<MemorySegment[]> stubs = new ArrayList<>();
        for (int t = 0; t < GtkGlassNative.CALLBACK_TABLES.size(); t++) {
            List<String> slots = GtkGlassNative.CALLBACK_TABLES.get(t).slots();
            MemorySegment[] tableStubs = new MemorySegment[slots.size()];
            for (int i = 0; i < tableStubs.length; i++) {
                tableStubs[i] = kept.contains(prefixes[t] + slots.get(i)) ? production.get(t).get(i) : null;
            }
            stubs.add(tableStubs);
        }
        GtkGlassNative.writeCallbackTables(stubs);
    }

    /** The production tables again ({@code GtkGlassNative.reinstallCallbacks}). FX thread. */
    public static void reinstallProductionTables() {
        GtkGlassNative.reinstallCallbacks();
    }

    /**
     * The production target of {@code notify_destroy} for the window of {@code windowId}: what the C would have
     * called when it destroyed a window while that slot was {@code NULL}, so that the Java window is closed too.
     * FX thread; answers the slot's status.
     */
    public static int notifyDestroyThroughTheSlotTarget(long windowId) {
        return GtkGlassNative.onNotifyDestroy(windowId);
    }

    /**
     * The production target of {@code notify_key} called as {@code WindowContextBase::process_key} dials it for a
     * key with a character: a {@code PRESS} and then a {@code TYPED}, both with the one code unit {@code c} at the
     * same address. FX thread.
     */
    public static void pressAndTypeThroughTheKeySlot(long viewId, char c) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment key = arena.allocate(JAVA_SHORT);
            key.set(JAVA_SHORT, 0, (short) c);
            int pressed = GtkGlassNative.onNotifyKey(viewId, KeyEvent.PRESS,
                    KeyEvent.VK_Q, key, 1, 0);
            int typed = GtkGlassNative.onNotifyKey(viewId, KeyEvent.TYPED,
                    KeyEvent.VK_UNDEFINED, key, 1, 0);
            if (pressed != 0 || typed != 0) {
                throw new IllegalStateException("notify_key answered " + pressed + ", " + typed);
            }
        }
    }

    /**
     * The production target of {@code notify_move_to_another_screen}, called directly with a monitor this display
     * has ({@code ggtk_test_fire_callback} passes monitor 1001); answers its status. FX thread.
     */
    public static int notifyMoveToAnotherScreen(long windowId, int monitor) {
        return GtkGlassNative.onNotifyMoveToAnotherScreen(windowId, monitor);
    }

    /** A generic recording target for every slot prototype, turned into upcall stubs. */
    private static final class Recorder {

        private static final MethodHandle RECORD;

        static {
            try {
                RECORD = MethodHandles.lookup().findStatic(Recorder.class, "record",
                        MethodType.methodType(int.class, List.class, String.class, Object[].class));
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        /** An upcall stub of {@code descriptor} that records into {@code sink} as {@code name}. */
        @SuppressWarnings("restricted")
        static MemorySegment stub(List<String> sink, String name, FunctionDescriptor descriptor) {
            MethodType type = descriptor.toMethodType();
            MethodHandle target = MethodHandles.insertArguments(RECORD, 0, sink, name)
                    .asCollector(Object[].class, type.parameterCount()).asType(type);
            return Linker.nativeLinker().upcallStub(target, descriptor, Arena.global());
        }

        static int record(List<String> sink, String name, Object[] args) {
            try {
                StringBuilder sb = new StringBuilder(name).append('(');
                for (int i = 0; i < args.length; i++) {
                    sb.append(i == 0 ? "" : ", ").append(args[i] instanceof MemorySegment segment
                            ? pointer(name, i, segment, args) : String.valueOf(args[i]));
                }
                sink.add(sb.append(')').toString());
                writeOuts(name, args);
            } catch (Throwable t) {
                sink.add(name + " recorder failed: " + t);
            }
            return RECORDING_STATUS;
        }

        /** A pointer argument, decoded where the header says what it holds; {@code out} for an out-parameter. */
        @SuppressWarnings("restricted")
        private static String pointer(String name, int index, MemorySegment segment, Object[] args) {
            if (name.equals("view.notify_key") && index == 3) {
                int count = (Integer) args[4];
                MemorySegment units = segment.reinterpret(2L * count);
                StringBuilder sb = new StringBuilder("u[");
                for (int i = 0; i < count; i++) {
                    sb.append(i == 0 ? "" : " ").append(String.format("%04x",
                            (int) units.get(JAVA_CHAR_UNALIGNED, 2L * i)));
                }
                return sb.append(']').toString();
            }
            if ((name.equals("view.notify_input_method_preedit") || name.equals("view.notify_input_method_commit"))
                    && index == 1) {
                return "b[" + HexFormat.of().formatHex(segment.reinterpret((Integer) args[2]).toArray(JAVA_BYTE))
                        + "]";
            }
            if (name.equals("dnd.source_get_data") && index == 0) {
                return "\"" + new String(segment.reinterpret((Integer) args[1]).toArray(JAVA_BYTE),
                        StandardCharsets.ISO_8859_1) + "\"";
            }
            return "out";
        }

        /** What the recorder answers through the out-parameters, per slot. */
        private static void writeOuts(String name, Object[] args) {
            switch (name) {
                case "app.get_application_name" ->
                        ((MemorySegment) args[0]).set(ADDRESS, 0, MemorySegment.ofAddress(0x1234));
                case "window.is_enabled" -> ((MemorySegment) args[1]).set(JAVA_INT, 0, 1);
                case "window.non_client_hit_test" -> ((MemorySegment) args[3]).set(JAVA_INT, 0, 2);
                case "view.notify_input_method_candidate_pos_request" -> {
                    ((MemorySegment) args[2]).set(JAVA_DOUBLE, 0, 3.25);
                    ((MemorySegment) args[2]).set(JAVA_DOUBLE, 8, 4.75);
                    ((MemorySegment) args[3]).set(JAVA_INT, 0, 1);
                }
                case "dnd.notify_drag_enter", "dnd.notify_drag_over" -> ((MemorySegment) args[6]).set(JAVA_INT, 0, 42);
                case "dnd.source_get_data" -> {
                    ((MemorySegment) args[3]).set(JAVA_INT, 0, 4);
                    ((MemorySegment) args[3]).set(JAVA_INT, 4, 9);
                }
                default -> {
                }
            }
        }
    }

    /** The test-only exports of {@code glass_gtk_api.h}, the drag map of {@code GtkDnDClipboard} and GLib's free. */
    private static final class Tables {

        private static final Linker LINKER = Linker.nativeLinker();

        static final MethodHandle ABI_VERSION = glass("ggtk_abi_version", FunctionDescriptor.of(JAVA_INT));
        /** {@code int32_t ggtk_test_fire_callback(int32_t slot, int64_t id, void *out, void *out2)}. */
        static final MethodHandle TEST_FIRE_CALLBACK = glass("ggtk_test_fire_callback",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS, ADDRESS));
        static final MethodHandle WINDOW_GET_ID = glass("ggtk_window_get_id", FunctionDescriptor.of(JAVA_LONG,
                ADDRESS));
        static final MethodHandle WINDOW_GET_VIEW_ID = glass("ggtk_window_get_view_id",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS));
        static final MethodHandle VIEW_GET_ID = glass("ggtk_view_get_id", FunctionDescriptor.of(JAVA_LONG, ADDRESS));
        static final MethodHandle G_FREE = glib("g_free", FunctionDescriptor.ofVoid(ADDRESS));

        static final VarHandle VIEW_PTR = field(View.class, "ptr", long.class,
                false);
        static final VarHandle DRAG_IN_PROGRESS = field(GtkDnDClipboard.class, "dragInProgress",
                HashMap.class, true);

        @SuppressWarnings("restricted")
        private static MethodHandle glass(String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = SymbolLookup.loaderLookup().find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError("libglassgtk3.so does not export " + name));
            return LINKER.downcallHandle(symbol, descriptor);
        }

        @SuppressWarnings("restricted")
        private static MethodHandle glib(String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = SymbolLookup.libraryLookup("libglib-2.0.so.0", Arena.global()).find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError("libglib-2.0.so.0 does not export " + name));
            return LINKER.downcallHandle(symbol, descriptor);
        }

        private static VarHandle field(Class<?> owner, String name, Class<?> type,
                                                        boolean isStatic) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
                return isStatic ? lookup.findStaticVarHandle(owner, name, type) : lookup.findVarHandle(owner, name,
                        type);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    /**
     * libc through the linker's default lookup; bound on first use. Its own holder, because the shim reads the
     * environment and the global scope in processes that have loaded no glass GTK library at all.
     */
    private static final class Libc {

        private static final Linker LINKER = Linker.nativeLinker();

        /** {@code char *getenv(const char *name)}. */
        static final MethodHandle GETENV = libc("getenv", FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code void *dlsym(void *handle, const char *symbol)}, called with {@code RTLD_DEFAULT} (NULL). */
        static final MethodHandle DLSYM = libc("dlsym", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

        @SuppressWarnings("restricted")
        private static MethodHandle libc(String name, FunctionDescriptor descriptor) {
            return LINKER.downcallHandle(LINKER.defaultLookup().find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError("libc does not export " + name)), descriptor);
        }
    }

    /** Bindings to the C that stays in {@code libglassgtk3.so}, and to libc; bound on first use. */
    private static final class Glass {

        private static final Linker LINKER = Linker.nativeLinker();

        static final MethodHandle FIND_GDK_KEYVAL_FOR_GLASS_KEYCODE = glass("find_gdk_keyval_for_glass_keycode",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        static final MethodHandle FIND_GDK_KEYCODE_FOR_KEYVAL = glass("find_gdk_keycode_for_keyval",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        static final MethodHandle GDK_KEYVAL_TO_GLASS = glass("gdk_keyval_to_glass",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        /** {@code jfloat getUIScale(GdkScreen *)}, a C++ function: bound by its mangled name. */
        static final MethodHandle GET_UI_SCALE = glass("_Z10getUIScaleP10_GdkScreen",
                FunctionDescriptor.of(JAVA_FLOAT, ADDRESS));
        static final MemorySegment OVERRIDE_UI_SCALE = variable("OverrideUIScale", JAVA_FLOAT.byteSize());
        static final MethodHandle SETENV = libc("setenv", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
        static final MethodHandle UNSETENV = libc("unsetenv", FunctionDescriptor.of(JAVA_INT, ADDRESS));

        private static MemorySegment symbol(String name) {
            return SymbolLookup.loaderLookup().find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError("libglassgtk3.so does not export " + name));
        }

        @SuppressWarnings("restricted")
        private static MethodHandle glass(String name, FunctionDescriptor descriptor) {
            return LINKER.downcallHandle(symbol(name), descriptor);
        }

        @SuppressWarnings("restricted")
        private static MemorySegment variable(String name, long size) {
            return symbol(name).reinterpret(size);
        }

        @SuppressWarnings("restricted")
        private static MethodHandle libc(String name, FunctionDescriptor descriptor) {
            return LINKER.downcallHandle(LINKER.defaultLookup().find(name).orElseThrow(), descriptor);
        }
    }

    /** Read-back bindings for pixbufs, cursors and X11 window properties; bound on first use. */
    private static final class Readback {

        private static final Linker LINKER = Linker.nativeLinker();

        static final MethodHandle GDK_PIXBUF_GET_WIDTH = pixbufGetter("gdk_pixbuf_get_width");
        static final MethodHandle GDK_PIXBUF_GET_HEIGHT = pixbufGetter("gdk_pixbuf_get_height");
        static final MethodHandle GDK_PIXBUF_GET_ROWSTRIDE = pixbufGetter("gdk_pixbuf_get_rowstride");
        static final MethodHandle GDK_PIXBUF_GET_N_CHANNELS = pixbufGetter("gdk_pixbuf_get_n_channels");
        static final MethodHandle GDK_PIXBUF_GET_HAS_ALPHA = pixbufGetter("gdk_pixbuf_get_has_alpha");
        static final MethodHandle GDK_PIXBUF_GET_BITS_PER_SAMPLE = pixbufGetter("gdk_pixbuf_get_bits_per_sample");
        static final MethodHandle GDK_PIXBUF_GET_COLORSPACE = pixbufGetter("gdk_pixbuf_get_colorspace");
        static final MethodHandle GDK_PIXBUF_GET_PIXELS = downcall("libgdk_pixbuf-2.0.so.0", "gdk_pixbuf_get_pixels",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle G_OBJECT_UNREF = downcall("libgobject-2.0.so.0", "g_object_unref",
                FunctionDescriptor.ofVoid(ADDRESS));
        static final MethodHandle GDK_DISPLAY_GET_DEFAULT = downcall("libgdk-3.so.0", "gdk_display_get_default",
                FunctionDescriptor.of(ADDRESS));
        static final MethodHandle GDK_DISPLAY_GET_DEFAULT_CURSOR_SIZE = downcall("libgdk-3.so.0",
                "gdk_display_get_default_cursor_size", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GDK_CURSOR_GET_CURSOR_TYPE = downcall("libgdk-3.so.0", "gdk_cursor_get_cursor_type",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GDK_CURSOR_GET_DISPLAY = downcall("libgdk-3.so.0", "gdk_cursor_get_display",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GDK_CURSOR_GET_IMAGE = downcall("libgdk-3.so.0", "gdk_cursor_get_image",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GDK_X11_GET_DEFAULT_XDISPLAY = downcall("libgdk-3.so.0",
                "gdk_x11_get_default_xdisplay", FunctionDescriptor.of(ADDRESS));
        static final MethodHandle X_INTERN_ATOM = downcall("libX11.so.6", "XInternAtom",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_INT));
        static final MethodHandle X_GET_WINDOW_PROPERTY = downcall("libX11.so.6", "XGetWindowProperty",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_INT,
                        JAVA_LONG, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        static final MethodHandle X_FREE = downcall("libX11.so.6", "XFree", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GTK_SETTINGS_GET_DEFAULT = downcall("libgtk-3.so.0", "gtk_settings_get_default",
                FunctionDescriptor.of(ADDRESS));
        static final MethodHandle G_OBJECT_GET_ONE = downcall("libgobject-2.0.so.0", "g_object_get",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS, ADDRESS), Linker.Option.firstVariadicArg(2));
        static final MethodHandle GDK_DISPLAY_SUPPORTS_COMPOSITE = downcall("libgdk-3.so.0",
                "gdk_display_supports_composite", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GDK_SCREEN_GET_DEFAULT = downcall("libgdk-3.so.0", "gdk_screen_get_default",
                FunctionDescriptor.of(ADDRESS));
        static final MethodHandle GDK_SCREEN_IS_COMPOSITED = downcall("libgdk-3.so.0", "gdk_screen_is_composited",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GDK_SCREEN_GET_RESOLUTION = downcall("libgdk-3.so.0", "gdk_screen_get_resolution",
                FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS));
        static final MethodHandle GTK_GET_MAJOR_VERSION = downcall("libgtk-3.so.0", "gtk_get_major_version",
                FunctionDescriptor.of(JAVA_INT));
        static final MethodHandle GTK_GET_MINOR_VERSION = downcall("libgtk-3.so.0", "gtk_get_minor_version",
                FunctionDescriptor.of(JAVA_INT));
        static final MethodHandle GTK_GET_MICRO_VERSION = downcall("libgtk-3.so.0", "gtk_get_micro_version",
                FunctionDescriptor.of(JAVA_INT));
        static final MethodHandle GDK_X11_SCREEN_GET_WINDOW_MANAGER_NAME = downcall("libgdk-3.so.0",
                "gdk_x11_screen_get_window_manager_name", FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GDK_SCREEN_GET_SYSTEM_VISUAL = downcall("libgdk-3.so.0",
                "gdk_screen_get_system_visual", FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GDK_VISUAL_GET_DEPTH = downcall("libgdk-3.so.0", "gdk_visual_get_depth",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle X_SERVER_VENDOR = downcall("libX11.so.6", "XServerVendor",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle X_VENDOR_RELEASE = downcall("libX11.so.6", "XVendorRelease",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /** {@code g_object_set(object, name, (gint) value, NULL)}. */
        static final MethodHandle G_OBJECT_SET_ONE_INT = downcall("libgobject-2.0.so.0", "g_object_set",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT, ADDRESS), Linker.Option.firstVariadicArg(2));
        /** {@code g_object_set(object, name, (const gchar *) value, NULL)}. */
        static final MethodHandle G_OBJECT_SET_ONE = downcall("libgobject-2.0.so.0", "g_object_set",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS, ADDRESS), Linker.Option.firstVariadicArg(2));
        static final MethodHandle GDK_SCREEN_GET_N_MONITORS = downcall("libgdk-3.so.0",
                "gdk_screen_get_n_monitors", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GDK_SCREEN_GET_MONITOR_GEOMETRY = downcall("libgdk-3.so.0",
                "gdk_screen_get_monitor_geometry", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS));
        static final MethodHandle GDK_SCREEN_GET_WIDTH = downcall("libgdk-3.so.0", "gdk_screen_get_width",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GDK_SCREEN_GET_HEIGHT = downcall("libgdk-3.so.0", "gdk_screen_get_height",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GDK_SCREEN_GET_WIDTH_MM = downcall("libgdk-3.so.0", "gdk_screen_get_width_mm",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GDK_SCREEN_GET_HEIGHT_MM = downcall("libgdk-3.so.0", "gdk_screen_get_height_mm",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GDK_SCREEN_GET_MONITOR_WIDTH_MM = downcall("libgdk-3.so.0",
                "gdk_screen_get_monitor_width_mm", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        static final MethodHandle GDK_SCREEN_GET_MONITOR_HEIGHT_MM = downcall("libgdk-3.so.0",
                "gdk_screen_get_monitor_height_mm", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        static final MethodHandle GDK_X11_GET_DEFAULT_ROOT_XWINDOW = downcall("libgdk-3.so.0",
                "gdk_x11_get_default_root_xwindow", FunctionDescriptor.of(JAVA_LONG));
        /**
         * {@code int XChangeProperty(Display *, Window, Atom property, Atom type, int format, int mode,
         * const unsigned char *data, int nelements)}.
         */
        static final MethodHandle X_CHANGE_PROPERTY = downcall("libX11.so.6", "XChangeProperty",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT,
                        ADDRESS, JAVA_INT));
        static final MethodHandle X_DELETE_PROPERTY = downcall("libX11.so.6", "XDeleteProperty",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG));
        static final MethodHandle X_SYNC = downcall("libX11.so.6", "XSync",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        /** {@code GtkStyle *gtk_style_new(void)}, the yardstick for the theme colours of the preferences. */
        static final MethodHandle GTK_STYLE_NEW = downcall("libgtk-3.so.0", "gtk_style_new",
                FunctionDescriptor.of(ADDRESS));
        static final MethodHandle GTK_STYLE_LOOKUP_COLOR = downcall("libgtk-3.so.0", "gtk_style_lookup_color",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        static final MethodHandle GTK_CSS_PROVIDER_NEW = downcall("libgtk-3.so.0", "gtk_css_provider_new",
                FunctionDescriptor.of(ADDRESS));
        /**
         * {@code gboolean gtk_css_provider_load_from_data(GtkCssProvider *, const gchar *data, gssize length,
         * GError **error)}.
         */
        static final MethodHandle GTK_CSS_PROVIDER_LOAD_FROM_DATA = downcall("libgtk-3.so.0",
                "gtk_css_provider_load_from_data",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS));
        /**
         * {@code void gtk_style_context_add_provider_for_screen(GdkScreen *, GtkStyleProvider *, guint priority)}.
         */
        static final MethodHandle GTK_STYLE_CONTEXT_ADD_PROVIDER_FOR_SCREEN = downcall("libgtk-3.so.0",
                "gtk_style_context_add_provider_for_screen", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT));
        static final MethodHandle G_OBJECT_CLASS_FIND_PROPERTY = downcall("libgobject-2.0.so.0",
                "g_object_class_find_property", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
        static final MethodHandle XKB_GET_STATE = downcall("libX11.so.6", "XkbGetState",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        static final MethodHandle GDK_KEYMAP_GET_FOR_DISPLAY = downcall("libgdk-3.so.0", "gdk_keymap_get_for_display",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GDK_KEYMAP_GET_ENTRIES_FOR_KEYVAL = downcall("libgdk-3.so.0",
                "gdk_keymap_get_entries_for_keyval", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS,
                        ADDRESS));
        /** {@code KeySym XkbKeycodeToKeysym(Display *, KeyCode, int group, int level)}; {@code KeyCode} is a byte. */
        static final MethodHandle XKB_KEYCODE_TO_KEYSYM = downcall("libX11.so.6", "XkbKeycodeToKeysym",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_BYTE, JAVA_INT, JAVA_INT));
        static final MethodHandle G_FREE = downcall("libglib-2.0.so.0", "g_free", FunctionDescriptor.ofVoid(ADDRESS));
        /** {@code GdkWindow *gdk_x11_window_lookup_for_display(GdkDisplay *, Window)}. */
        static final MethodHandle GDK_X11_WINDOW_LOOKUP_FOR_DISPLAY = downcall("libgdk-3.so.0",
                "gdk_x11_window_lookup_for_display", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));
        /** {@code void gdk_window_get_user_data(GdkWindow *, gpointer *data)}. */
        static final MethodHandle GDK_WINDOW_GET_USER_DATA = downcall("libgdk-3.so.0", "gdk_window_get_user_data",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
        static final MethodHandle GTK_WIDGET_GET_TOPLEVEL = downcall("libgtk-3.so.0", "gtk_widget_get_toplevel",
                FunctionDescriptor.of(ADDRESS, ADDRESS));

        private static MethodHandle pixbufGetter(String name) {
            return downcall("libgdk_pixbuf-2.0.so.0", name, FunctionDescriptor.of(JAVA_INT, ADDRESS));
        }

        @SuppressWarnings("restricted")
        private static MethodHandle downcall(String soname, String name, FunctionDescriptor descriptor,
                                             Linker.Option... options) {
            MemorySegment symbol = SymbolLookup.libraryLookup(soname, Arena.global()).find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError(soname + " does not export " + name));
            return LINKER.downcallHandle(symbol, descriptor, options);
        }
    }

    /** GTK bindings that drive a running {@code GtkFileChooserNative} from inside its own loop. */
    private static final class Dialog {

        private static final Linker LINKER = Linker.nativeLinker();

        static final MethodHandle GTK_WINDOW_LIST_TOPLEVELS = downcall("libgtk-3.so.0",
                "gtk_window_list_toplevels", FunctionDescriptor.of(ADDRESS));
        static final MethodHandle GTK_WIDGET_GET_MAPPED = downcall("libgtk-3.so.0", "gtk_widget_get_mapped",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GTK_WINDOW_GET_TITLE = downcall("libgtk-3.so.0", "gtk_window_get_title",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle G_TYPE_NAME_FROM_INSTANCE = downcall("libgobject-2.0.so.0",
                "g_type_name_from_instance", FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GTK_FILE_CHOOSER_GET_CURRENT_FOLDER = downcall("libgtk-3.so.0",
                "gtk_file_chooser_get_current_folder", FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GTK_FILE_CHOOSER_LIST_FILTERS = downcall("libgtk-3.so.0",
                "gtk_file_chooser_list_filters", FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GTK_FILE_FILTER_GET_NAME = downcall("libgtk-3.so.0",
                "gtk_file_filter_get_name", FunctionDescriptor.of(ADDRESS, ADDRESS));
        /** {@code void gtk_file_chooser_set_current_name(GtkFileChooser *, const gchar *)}; the name is UTF-8. */
        static final MethodHandle GTK_FILE_CHOOSER_SET_CURRENT_NAME = downcall("libgtk-3.so.0",
                "gtk_file_chooser_set_current_name", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
        static final MethodHandle GTK_FILE_CHOOSER_SELECT_FILENAME = downcall("libgtk-3.so.0",
                "gtk_file_chooser_select_filename", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        static final MethodHandle GTK_FILE_CHOOSER_GET_FILENAMES = downcall("libgtk-3.so.0",
                "gtk_file_chooser_get_filenames", FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GTK_FILE_CHOOSER_SELECT_ALL = downcall("libgtk-3.so.0",
                "gtk_file_chooser_select_all", FunctionDescriptor.ofVoid(ADDRESS));
        static final MethodHandle G_SLIST_FREE = downcall("libglib-2.0.so.0", "g_slist_free",
                FunctionDescriptor.ofVoid(ADDRESS));
        static final MethodHandle GTK_DIALOG_RESPONSE = downcall("libgtk-3.so.0", "gtk_dialog_response",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        static final MethodHandle G_LIST_FREE = downcall("libglib-2.0.so.0", "g_list_free",
                FunctionDescriptor.ofVoid(ADDRESS));
        static final MethodHandle G_FREE = downcall("libglib-2.0.so.0", "g_free",
                FunctionDescriptor.ofVoid(ADDRESS));

        @SuppressWarnings("restricted")
        private static MethodHandle downcall(String soname, String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = SymbolLookup.libraryLookup(soname, Arena.global()).find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError(soname + " does not export " + name));
            return LINKER.downcallHandle(symbol, descriptor);
        }
    }

    /** GLib bindings for the layout probes that need no display; bound on first use. */
    private static final class Layout {

        private static final Linker LINKER = Linker.nativeLinker();
        private static final String LIB_GLIB = "libglib-2.0.so.0";

        /** {@code GQuark g_quark_from_string(const gchar *string)}. */
        static final MethodHandle G_QUARK_FROM_STRING = downcall("g_quark_from_string",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /** {@code GError *g_error_new_literal(GQuark domain, gint code, const gchar *message)}. */
        static final MethodHandle G_ERROR_NEW_LITERAL = downcall("g_error_new_literal",
                FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));
        static final MethodHandle G_ERROR_FREE = downcall("g_error_free", FunctionDescriptor.ofVoid(ADDRESS));
        static final MethodHandle G_HASH_TABLE_NEW = downcall("g_hash_table_new",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
        static final MemorySegment G_DIRECT_HASH = symbol("g_direct_hash");
        static final MemorySegment G_DIRECT_EQUAL = symbol("g_direct_equal");
        static final MethodHandle G_HASH_TABLE_INSERT = downcall("g_hash_table_insert",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        static final MethodHandle G_HASH_TABLE_ITER_INIT = downcall("g_hash_table_iter_init",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
        static final MethodHandle G_HASH_TABLE_ITER_NEXT = downcall("g_hash_table_iter_next",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        static final MethodHandle G_HASH_TABLE_UNREF = downcall("g_hash_table_unref",
                FunctionDescriptor.ofVoid(ADDRESS));

        @SuppressWarnings("restricted")
        private static MemorySegment symbol(String name) {
            return SymbolLookup.libraryLookup(LIB_GLIB, Arena.global()).find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError(LIB_GLIB + " does not export " + name));
        }

        @SuppressWarnings("restricted")
        private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
            return LINKER.downcallHandle(symbol(name), descriptor);
        }
    }

    /** One-shot GLib sources with an explicit priority; bound on first use. */
    private static final class Yardstick {

        private static final Linker LINKER = Linker.nativeLinker();
        private static final SymbolLookup GLIB = library("libglib-2.0.so.0");
        private static final Map<Long, Runnable> RUNNABLES = new ConcurrentHashMap<>();
        private static final AtomicLong NEXT_ID = new AtomicLong(1);

        /** {@code guint g_idle_add_full(gint, GSourceFunc, gpointer, GDestroyNotify)}. */
        static final MethodHandle G_IDLE_ADD_FULL = downcall("g_idle_add_full",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

        /** {@code guint g_timeout_add_full(gint, guint, GSourceFunc, gpointer, GDestroyNotify)}. */
        static final MethodHandle G_TIMEOUT_ADD_FULL = downcall("g_timeout_add_full",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

        /** {@code gboolean (*GSourceFunc)(gpointer)}, one stub for every source this class adds. */
        static final MemorySegment CALLBACK = upcall();

        static long register(Runnable runnable) {
            long id = NEXT_ID.getAndIncrement();
            RUNNABLES.put(id, runnable);
            return id;
        }

        /** Runs and forgets the runnable registered under {@code data}; answers {@code G_SOURCE_REMOVE}. */
        static int onSource(MemorySegment data) {
            try {
                Runnable runnable = RUNNABLES.remove(data.address());
                if (runnable != null) {
                    runnable.run();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return 0;
        }

        @SuppressWarnings("restricted")
        private static SymbolLookup library(String soname) {
            return SymbolLookup.libraryLookup(soname, Arena.global());
        }

        @SuppressWarnings("restricted")
        private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = GLIB.find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError("libglib-2.0.so.0 does not export " + name));
            return LINKER.downcallHandle(symbol, descriptor);
        }

        @SuppressWarnings("restricted")
        private static MemorySegment upcall() {
            try {
                FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS);
                MethodHandle target = MethodHandles.lookup().findStatic(Yardstick.class, "onSource",
                        descriptor.toMethodType());
                return LINKER.upcallStub(target, descriptor, Arena.global());
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    /** X11 and XTest calls that inject what a user or a window manager would; bound on first use. */
    private static final class EventDriver {

        private static final Linker LINKER = Linker.nativeLinker();

        /** {@code Status XSendEvent(Display *, Window, Bool propagate, long event_mask, XEvent *)}. */
        static final MethodHandle X_SEND_EVENT = downcall("libX11.so.6", "XSendEvent",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_LONG, ADDRESS));
        /**
         * {@code Bool XTranslateCoordinates(Display *, Window src, Window dest, int src_x, int src_y, int *dest_x,
         * int *dest_y, Window *child)}.
         */
        static final MethodHandle X_TRANSLATE_COORDINATES = downcall("libX11.so.6", "XTranslateCoordinates",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS,
                        ADDRESS));
        /** {@code char *XGetAtomName(Display *, Atom)}; freed with {@code XFree}. */
        static final MethodHandle X_GET_ATOM_NAME = downcall("libX11.so.6", "XGetAtomName",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));
        /** {@code int XSetInputFocus(Display *, Window, int revert_to, Time)}. */
        static final MethodHandle X_SET_INPUT_FOCUS = downcall("libX11.so.6", "XSetInputFocus",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_LONG));
        /** {@code Display *XOpenDisplay(const char *)}. */
        static final MethodHandle X_OPEN_DISPLAY = downcall("libX11.so.6", "XOpenDisplay",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle X_CLOSE_DISPLAY = downcall("libX11.so.6", "XCloseDisplay",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle X_FLUSH = downcall("libX11.so.6", "XFlush", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /** {@code int XTestFakeButtonEvent(Display *, unsigned int button, Bool is_press, unsigned long delay)}. */
        static final MethodHandle X_TEST_FAKE_BUTTON_EVENT = downcall("libXtst.so.6", "XTestFakeButtonEvent",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_LONG));
        /** {@code KeySym XStringToKeysym(const char *)}. */
        static final MethodHandle X_STRING_TO_KEYSYM = downcall("libX11.so.6", "XStringToKeysym",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS));
        /** {@code KeyCode XKeysymToKeycode(Display *, KeySym)}; {@code KeyCode} is an unsigned char. */
        static final MethodHandle X_KEYSYM_TO_KEYCODE = downcall("libX11.so.6", "XKeysymToKeycode",
                FunctionDescriptor.of(JAVA_BYTE, ADDRESS, JAVA_LONG));
        /** {@code int XTestFakeKeyEvent(Display *, unsigned int keycode, Bool is_press, unsigned long delay)}. */
        static final MethodHandle X_TEST_FAKE_KEY_EVENT = downcall("libXtst.so.6", "XTestFakeKeyEvent",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_LONG));
        /** {@code Bool XkbGetAutoRepeatRate(Display *, unsigned int device, unsigned int *delay, unsigned int *)}. */
        static final MethodHandle XKB_GET_AUTO_REPEAT_RATE = downcall("libX11.so.6", "XkbGetAutoRepeatRate",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
        /** {@code Bool XkbSetAutoRepeatRate(Display *, unsigned int device, unsigned int delay, unsigned int)}. */
        static final MethodHandle XKB_SET_AUTO_REPEAT_RATE = downcall("libX11.so.6", "XkbSetAutoRepeatRate",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
        /** {@code int XTestFakeMotionEvent(Display *, int screen, int x, int y, unsigned long delay)}. */
        static final MethodHandle X_TEST_FAKE_MOTION_EVENT = downcall("libXtst.so.6", "XTestFakeMotionEvent",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG));

        @SuppressWarnings("restricted")
        private static MethodHandle downcall(String soname, String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = SymbolLookup.libraryLookup(soname, Arena.global()).find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError(soname + " does not export " + name));
            return LINKER.downcallHandle(symbol, descriptor);
        }
    }
}
