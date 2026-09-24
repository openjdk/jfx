/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.InvokeLaterDispatcher;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.javafx.application.preferences.PreferenceMapping;
import com.sun.javafx.util.Logging;
import com.sun.glass.utils.NativeLibLoader;
import com.sun.prism.impl.PrismSettings;
import com.sun.javafx.logging.PlatformLogger;
import javafx.scene.paint.Color;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;


final class GtkApplication extends Application implements
                                    InvokeLaterDispatcher.InvokeLaterSubmitter {
    private static final int forcedGtkVersion;
    private static boolean gtkVersionWarningIssued = false;
    private static final String GTK2_REMOVED_WARNING =
            "WARNING: A command line option tried to select the GTK 2 library, which was removed from JavaFX.";

    private static final String GTK_INVALID_VERSION_WARNING =
            "WARNING: A command line option tried to select an invalid GTK library version.";
    private static final String GTK3_FALLBACK_WARNING = "WARNING: The GTK 3 library will be used instead.";

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1openURI} ({@code GlassApplication.cpp}, commit
     * {@code 033187ad90}), bound directly.
     */
    private static int _openURI(String uri) {
        return GtkGlassNative.openURI(uri);
    }

    static  {
        String gtkVersion = System.getProperty("org.eclipse.swt.internal.gtk.version");
        if (gtkVersion != null && gtkVersion.contains(".")) {
            PlatformLogger logger = Logging.getJavaFXLogger();
            logger.fine(String.format("SWT-GTK library found. Gtk Version = %s.", gtkVersion));
            String[] vers = gtkVersion.split("\\.");
            int ver = Integer.parseInt(vers[0]);

            if (ver != 3) {
                throw new UnsupportedOperationException("SWT-GTK uses unsupported major GTK version " + ver + " .");
            }

            forcedGtkVersion = ver;
        } else {
            forcedGtkVersion = 0;
        }

        // Up to commit 033187ad90 this mapped libglass.so, the launcher library whose _queryLibrary (launcher.c)
        // the query below does in Java. A deployment that renames a glassgtk3 build to libglass.so - the one the
        // copy of _queryLibrary in GlassApplication.cpp answered QUERY_USE_CURRENT for - still has a library of
        // that name, and mapping it here is what lets the query find it. A machine with none is the ordinary case.
        // The catch also covers a libglass.so that is there and cannot be loaded (a wrong architecture, a missing
        // dependency): that deployment then goes on to the glassgtk3 path with this the only record of why. FINE
        // and not WARNING for both, because the Linux build produces no libglass.so at all any more, so a warning
        // here would be on every start.
        try {
            Application.loadNativeLibrary();
        } catch (UnsatisfiedLinkError e) {
            Logging.getJavaFXLogger().fine("no glass library to load ahead of the GTK one", e);
        }
    }

    public static  int screen = -1;
    public static  long display = 0;
    public static  long visualID = 0;

    static float overrideUIScale;

    /**
     * {@code jdk.gtk.verbose}, as {@code _initGTK} stored it in the C global {@code gtk_verbose}; read by the
     * verbose messages {@link GtkGlassNative} reproduces.
     */
    static volatile boolean verbose;

    private final InvokeLaterDispatcher invokeLaterDispatcher;

    private static float getFloat(String propname, float defval, String description) {
        String str = System.getProperty(propname);
        if (str == null) {
            str = System.getenv(propname);
        }
        if (str == null) {
            return defval;
        }
        str = str.trim();
        float val;
        if (str.endsWith("%")) {
            val = Integer.parseInt(str.substring(0, str.length()-1)) / 100.0f;
        } else if (str.endsWith("DPI") || str.endsWith("dpi")) {
            val = Integer.parseInt(str.substring(0, str.length()-3)) / 96.0f;
        } else {
            val = Float.parseFloat(str);
        }
        if (PrismSettings.verbose) {
            System.out.println(description+val);
        }
        return val;
    }

    GtkApplication() {

        int gtkVersion = forcedGtkVersion == 0 ?
            ((Supplier<Integer>) () -> {
                String v = System.getProperty("jdk.gtk.version","3");
                return Character.getNumericValue(v.charAt(0));
            }).get() : forcedGtkVersion;

        if (gtkVersion != 3) {
            if (!gtkVersionWarningIssued) {
                if (gtkVersion == 2) {
                    System.err.println(GTK2_REMOVED_WARNING);
                } else {
                    System.err.println(GTK_INVALID_VERSION_WARNING);
                }
            }

            System.err.println(GTK3_FALLBACK_WARNING);
            gtkVersionWarningIssued = true;
            gtkVersion = 3;
        }

        boolean gtkVersionVerbose = Boolean.getBoolean("jdk.gtk.verbose");
        if (PrismSettings.allowHiDPIScaling) {
            float tmp =
                getFloat("glass.gtk.uiScale", -1.0f, "Forcing UI scaling factor: ");
            overrideUIScale = tmp;
        } else {
            overrideUIScale = -1.0f;
        }

        int libraryToLoad = _queryLibrary(gtkVersion, gtkVersionVerbose);

        if (libraryToLoad == QUERY_NO_DISPLAY) {
            throw new UnsupportedOperationException("Unable to open DISPLAY");
        } else if (libraryToLoad == QUERY_USE_CURRENT) {
            if (gtkVersionVerbose) {
                System.out.println("Glass GTK library to load is already loaded");
            }
        } else if (libraryToLoad == QUERY_LOAD_GTK3) {
            if (gtkVersionVerbose) {
                System.out.println("Glass GTK library to load is glassgtk3");
            }
            NativeLibLoader.loadLibrary("glassgtk3");
        } else {
            throw new UnsupportedOperationException("Unable to load glass GTK library.");
        }

        // Both branches that get here have a glass GTK library loaded: glassgtk3, or - QUERY_USE_CURRENT - a glassgtk3
        // build that was itself loaded as the "glass" library (Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary
        // of GlassApplication.cpp, commit 033187ad90). Bind the system libraries now: a missing one fails the startup
        // here, as loading that library did when it still linked them (GtkGlassNative.link).
        GtkGlassNative.link();
        // The callback tables of glass_gtk_api.h, before _initGTK and _init connect any signal of the library and
        // before the first window exists
        GtkGlassNative.installCallbacks();

        verbose = gtkVersionVerbose;
        _initGTK(gtkVersion, gtkVersionVerbose, overrideUIScale);

        // Embedded in SWT, with shared event thread
        boolean isEventThread = Boolean.getBoolean("javafx.embed.isEventThread");
        if (!isEventThread) {
            invokeLaterDispatcher = new InvokeLaterDispatcher(this);
            invokeLaterDispatcher.start();
        } else {
            invokeLaterDispatcher = null;
        }
    }

    /*
     * The answers of _queryLibrary this class acts on; anything else is a failure to load a GTK library
     * (GtkGlassNative.Loader.QUERY_ERROR). The glass GTK library's C states the same values as GGTK_QUERY_* in
     * native-glass/gtk/glass_gtk_api.h for the loader C of commit 033187ad90 that still carries them.
     */
    private static final int QUERY_NO_DISPLAY = GtkGlassNative.Loader.QUERY_NO_DISPLAY;
    private static final int QUERY_USE_CURRENT = GtkGlassNative.Loader.QUERY_USE_CURRENT;
    private static final int QUERY_LOAD_GTK3 = GtkGlassNative.Loader.QUERY_LOAD_GTK3;

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary} ({@code launcher.c}, the whole of
     * {@code libglass.so}, and its copy in {@code GlassApplication.cpp}; commit {@code 033187ad90}): checks the
     * system and answers which library to load, one of the {@code QUERY_} constants.
     */
    private static int _queryLibrary(int version, boolean verbose) {
        return GtkGlassNative.Loader.queryLibrary(version, verbose);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1initGTK} ({@code GlassApplication.cpp}, commit
     * {@code 033187ad90}), now {@code ggtk_application_init_gtk}: throws the
     * {@code UnsupportedOperationException} the JNI native left pending when GTK is too old.
     */
    private static void _initGTK(int version, boolean verbose, float overrideUIScale) {
        GtkGlassNative.applicationInitGtk(version, verbose, overrideUIScale);
    }

    private void initDisplay() {
        Map ds = getDeviceDetails();
        if (ds != null) {
            Object value;
            value = ds.get("XDisplay");
            if (value != null) {
                display = (Long)value;
            }
            value = ds.get("XVisualID");
            if (value != null) {
                visualID = (Long)value;
            }
            value = ds.get("XScreenID");
            if (value != null) {
                screen = (Integer)value;
            }
        }
    }

    private void init() {
        initDisplay();
        long eventProc = 0;
        Map map = getDeviceDetails();
        if (map != null) {
            Long result = (Long) map.get("javafx.embed.eventProc");
            eventProc = result == null ? 0 : result;
        }

        final boolean disableGrab = (Boolean.getBoolean("sun.awt.disablegrab") ||
               Boolean.getBoolean("glass.disableGrab"));

        _init(eventProc, disableGrab);
        // the "platformSupport = new PlatformSupport(env, obj)" that _init ran last at commit 033187ad90
        GtkGlassNative.platformSupportCreate(this);
    }

    @Override
    protected void runLoop(final Runnable launchable) {
        // Embedded in SWT, with shared event thread
        final boolean isEventThread = Boolean.getBoolean("javafx.embed.isEventThread");

        if (isEventThread) {
            init();
            setEventThread(Thread.currentThread());
            launchable.run();
            return;
        }

        final boolean noErrorTrap = Boolean.getBoolean("glass.noErrorTrap");

        final Thread toolkitThread =
            new Thread(() -> {
                init();
                _runLoop(launchable, noErrorTrap);
            }, "GtkNativeMainLoopThread");
        setEventThread(toolkitThread);
        toolkitThread.start();
    }

    @Override
    protected void finishTerminating() {
        final Thread toolkitThread = getEventThread();
        if (toolkitThread != null) {
            _terminateLoop();
            // the "delete platformSupport" that _terminateLoop ran after gtk_main_quit at commit 033187ad90
            GtkGlassNative.platformSupportDestroy();
            setEventThread(null);
        }
        super.finishTerminating();
    }

    @Override public boolean shouldUpdateWindow() {
        return true;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1terminateLoop} ({@code GlassApplication.cpp}, commit
     * {@code 033187ad90}): {@code gtk_main_quit}, through GTK directly.
     */
    private void _terminateLoop() {
        GtkGlassNative.applicationTerminateLoop();
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1init} ({@code GlassApplication.cpp}, commit
     * {@code 033187ad90}), now {@code ggtk_application_init}.
     */
    private void _init(long eventProc, boolean disableGrab) {
        GtkGlassNative.applicationInit(eventProc, disableGrab);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1runLoop} ({@code GlassApplication.cpp}, commit
     * {@code 033187ad90}). Its first statements are Java: it ran {@code launchable.run()} before anything else and,
     * when that threw, reported the {@code Throwable} as {@code check_and_clear_exception} did
     * ({@code CHECK_JNI_EXCEPTION}) and returned before the loop; a {@code null} launchable was the
     * {@code NullPointerException} {@code CallVoidMethod} raised for it. The rest - the error trap and
     * {@code gtk_main} - is {@code ggtk_application_run_loop}, on this same thread.
     */
    private void _runLoop(Runnable launchable, boolean noErrorTrap) {
        try {
            launchable.run();
        } catch (Throwable t) {
            GtkGlassNative.reportException(t);
            return;
        }
        GtkGlassNative.applicationRunLoop(noErrorTrap);
    }

    @Override
    protected void _invokeAndWait(final Runnable runnable) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.invokeAndWait(runnable);
        } else {
            final CountDownLatch latch = new CountDownLatch(1);
            submitForLaterInvocation(() -> {
                if (runnable != null) runnable.run();
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                //FAIL SILENTLY
            }
        }
    }

    // InvokeLaterDispatcher.InvokeLaterSubmitter
    @Override public void submitForLaterInvocation(Runnable r) {
        GtkGlassNative.submitForLaterInvocation(r);
    }

    @Override protected void _invokeLater(Runnable runnable) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.invokeLater(runnable);
        } else {
            submitForLaterInvocation(runnable);
        }
    }

    private Object eventLoopExitEnterPassValue;

    private void enterNestedEventLoopImpl() {
        GtkGlassNative.enterNestedEventLoop();
    }

    private void leaveNestedEventLoopImpl() {
        GtkGlassNative.leaveNestedEventLoop();
    }

    @Override
    protected Object _enterNestedEventLoop() {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyEnteringNestedEventLoop();
        }
        try {
            enterNestedEventLoopImpl();
            final Object retValue = eventLoopExitEnterPassValue;
            eventLoopExitEnterPassValue = null;
            return retValue;
        } finally {
            if (invokeLaterDispatcher != null) {
                invokeLaterDispatcher.notifyLeftNestedEventLoop();
            }
        }
    }

    @Override
    protected void _leaveNestedEventLoop(Object retValue) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyLeavingNestedEventLoop();
        }
        eventLoopExitEnterPassValue = retValue;
        leaveNestedEventLoopImpl();
    }

    @Override
    public Window createWindow(Window owner, Screen screen, int styleMask) {
        return new GtkWindow(owner, screen, styleMask);
    }

    @Override
    public View createView() {
        return new GtkView();
    }

    @Override
    public Cursor createCursor(int type) {
        return new GtkCursor(type);
    }

    @Override
    public Cursor createCursor(int x, int y, Pixels pixels) {
        return new GtkCursor(x, y, pixels);
    }

    @Override
    protected void staticCursor_setVisible(boolean visible) {
    }

    @Override
    protected Size staticCursor_getBestSize(int width, int height) {
        return GtkCursor._getBestSize(width, height);
    }

    @Override
    public Pixels createPixels(int width, int height, ByteBuffer data) {
        return new GtkPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, ByteBuffer data, float scalex, float scaley) {
        return new GtkPixels(width, height, data, scalex, scaley);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data) {
        return new GtkPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        return new GtkPixels(width, height, data, scalex, scaley);
    }

    @Override
    protected int staticPixels_getNativeFormat() {
        return Pixels.Format.BYTE_BGRA_PRE; // TODO
    }

    @Override
    public GlassRobot createRobot() {
        return new GtkRobot();
    }

    @Override
    public Timer createTimer(Runnable runnable) {
        return new GtkTimer(runnable);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication_staticTimer_1getMinPeriod} ({@code GlassApplication.cpp},
     * commit {@code 033187ad90}) returned this constant: there are no restrictions on the period of a GLib timeout.
     */
    @Override
    protected int staticTimer_getMinPeriod() {
        return 0;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication_staticTimer_1getMaxPeriod} ({@code GlassApplication.cpp},
     * commit {@code 033187ad90}) returned this constant.
     */
    @Override
    protected int staticTimer_getMaxPeriod() {
        return 10000;
    }

    @Override protected double staticScreen_getVideoRefreshPeriod() {
        return 0.0;     // indicate millisecond resolution
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication_staticScreen_1getScreens} ({@code GlassApplication.cpp},
     * commit {@code 033187ad90}), which called {@code rebuild_screens} of {@code glass_screen.cpp} and answered
     * {@code NULL} when building a {@code Screen} threw. Nothing here throws where that C caught, so the array is
     * always built; {@code Screen.initScreens} turned that {@code NULL} into a {@code RuntimeException}.
     */
    @Override
    protected Screen[] staticScreen_getScreens() {
        return GtkGlassNative.screens();
    }

    @Override
    protected FileChooserResult staticCommonDialogs_showFileChooser(
            Window owner, String folder, String filename, String title,
            int type, boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {

        return GtkCommonDialogs.showFileChooser(owner, folder, filename, title,
                type, multipleMode, extensionFilters, defaultFilterIndex);
    }

    @Override
    protected File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
        return GtkCommonDialogs.showFolderChooser(owner, folder, title);
    }

    @Override
    protected void _showDocument(String uri) {
        _openURI(uri);
    }

    @Override
    protected long staticView_getMultiClickTime() {
        return GtkGlassNative.multiClickTime();
    }

    @Override
    protected int staticView_getMultiClickMaxX() {
        return GtkGlassNative.multiClickMaxX();
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication_staticView_1getMultiClickMaxY} ({@code GlassApplication.cpp},
     * commit {@code 033187ad90}) answered the X distance.
     */
    @Override
    protected int staticView_getMultiClickMaxY() {
        return GtkGlassNative.multiClickMaxX();
    }

    @Override
    protected boolean _supportsInputMethods() {
        return true;
    }

    @Override
    protected boolean _supportsTransparentWindows() {
        return GtkGlassNative.supportsTransparentWindows();
    }

    @Override protected boolean _supportsUnifiedWindows() {
        return false;
    }

    @Override
    protected boolean _supportsExtendedWindows() {
        return true;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1getKeyCodeForChar} ({@code glass_key.cpp}, commit
     * {@code 033187ad90}), now {@code ggtk_application_get_key_code_for_char}.
     */
    @Override
    protected int _getKeyCodeForChar(char c, int hint) {
        return GtkGlassNative.applicationKeyCodeForChar(c, hint);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1isKeyLocked} ({@code glass_key.cpp}, commit
     * {@code 033187ad90}), bound directly.
     */
    @Override
    protected int _isKeyLocked(int keyCode) {
        return GtkGlassNative.isKeyLocked(keyCode);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication_getPlatformPreferences} ({@code GlassApplication.cpp},
     * commit {@code 033187ad90}), which called {@code PlatformSupport::collectPreferences} and answered
     * {@code NULL} before {@code _init} had created the {@code PlatformSupport} and after
     * {@code _terminateLoop} had deleted it.
     */
    @Override
    public Map<String, Object> getPlatformPreferences() {
        return GtkGlassNative.platformPreferences();
    }

    /**
     * {@code Application.notifyPreferencesChanged}, which {@code PlatformSupport::updatePreferences}
     * ({@code PlatformSupport.cpp}, commit {@code 033187ad90}) called on this object from the GtkSettings and
     * GNetworkMonitor handlers.
     */
    void notifyPlatformPreferencesChanged(Map<String, Object> preferences) {
        notifyPreferencesChanged(preferences);
    }

    @Override
    public Map<String, PreferenceMapping<?, ?>> getPlatformKeyMappings() {
        return Map.of(
            "GTK.theme_fg_color", new PreferenceMapping<>("foregroundColor", Color.class),
            "GTK.theme_bg_color", new PreferenceMapping<>("backgroundColor", Color.class),
            "GTK.theme_selected_bg_color", new PreferenceMapping<>("accentColor", Color.class),
            "GTK.enable_animations", new PreferenceMapping<>("reducedMotion", Boolean.class, b -> !b),
            "GTK.overlay_scrolling", new PreferenceMapping<>("persistentScrollBars", Boolean.class, b -> !b),
            "GTK.network_metered", new PreferenceMapping<>("reducedData", Boolean.class)
        );
    }

    // This list needs to be kept in sync with PlatformSupport.cpp in the Glass toolkit for GTK.
    @Override
    public Map<String, Class<?>> getPlatformKeys() {
        return Map.ofEntries(
            Map.entry("GTK.theme_name", String.class),
            Map.entry("GTK.theme_fg_color", Color.class),
            Map.entry("GTK.theme_bg_color", Color.class),
            Map.entry("GTK.theme_base_color", Color.class),
            Map.entry("GTK.theme_selected_bg_color", Color.class),
            Map.entry("GTK.theme_selected_fg_color", Color.class),
            Map.entry("GTK.theme_unfocused_fg_color", Color.class),
            Map.entry("GTK.theme_unfocused_bg_color", Color.class),
            Map.entry("GTK.theme_unfocused_base_color", Color.class),
            Map.entry("GTK.theme_unfocused_selected_bg_color", Color.class),
            Map.entry("GTK.theme_unfocused_selected_fg_color", Color.class),
            Map.entry("GTK.insensitive_bg_color", Color.class),
            Map.entry("GTK.insensitive_fg_color", Color.class),
            Map.entry("GTK.insensitive_base_color", Color.class),
            Map.entry("GTK.borders", Color.class),
            Map.entry("GTK.unfocused_borders", Color.class),
            Map.entry("GTK.warning_color", Color.class),
            Map.entry("GTK.error_color", Color.class),
            Map.entry("GTK.success_color", Color.class),
            Map.entry("GTK.enable_animations", Boolean.class),
            Map.entry("GTK.overlay_scrolling", Boolean.class),
            Map.entry("GTK.network_metered", Boolean.class)
        );
    }
}
