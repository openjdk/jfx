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
package com.sun.glass.ui.win;

import com.sun.glass.ui.*;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.javafx.application.preferences.PreferenceMapping;
import com.sun.prism.impl.PrismSettings;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.paint.Color;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The Windows Glass application peer. It declares no {@code native} method: every call into
 * {@code glass.dll} is an FFM downcall of {@link WinGlassNative}, and every event comes back through the
 * callback tables the static initializer installs. Since the accessibility flip no class of this package
 * declares one either, so the library is loaded through {@code Application.loadNativeLibrary()} only
 * because that is how a Glass library is loaded - nothing in it is reached by name any more.
 * <p>
 * <b>Exceptions reported from the library.</b> They no longer travel through JNI at all. The sink used to
 * be {@code CheckAndClearException} ({@code GlassAccessibleJni.cpp}), which needed
 * {@code com.sun.glass.ui.Application} cached from inside a real JNI native method, because
 * {@code FindClass} cannot see a javafx.graphics class from inside an FFM downcall and the toolkit thread
 * spends its whole life inside the {@code gwin_run_loop} downcall. The last native that did that caching
 * was {@code WinAccessible._initIDs}, and it is gone with the other eight: an accessibility slot that
 * throws is caught in its upcall stub and reported from Java, where the lookup cannot fail, and the
 * library is told {@code GWIN_ERR_UPCALL}. {@code WinDowncallExceptionReportingTest} is the regression
 * net for that delivery.
 * <p>
 * Line numbers into the {@code native-glass/win} C++ sources refer to those files at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
final class WinApplication extends Application implements InvokeLaterDispatcher.InvokeLaterSubmitter {

    static float overrideUIScale;

    /**
     * Was {@code Java_com_sun_glass_ui_win_WinApplication__1getDefaultBrowser}
     * ({@code native-glass/win/GlassApplication.cpp:537-559}), whose body was one
     * {@code shlwapi!AssocQueryStringW} and a {@code CreateJString}: a {@code WRAPPER}, so there is no
     * C left for it and {@code glass.dll} has no reason to link {@code shlwapi} any more.
     * <p>
     * Kept {@code private static} under its old name so that {@link #_showDocument} - which parses
     * the command line, quotes and {@code %1} included - is untouched by the flip.
     * <p>
     * {@code null} still means "no association": the {@code FAILED(hr)} arm of the C (:554-556).
     * What it does <em>not</em> mean is "no browser installed" - Windows answers an unregistered
     * scheme with a failure but an unregistered extension with the shell's own
     * {@code OpenWith.exe "%1"}, and the JNI passed that on just as this does.
     */
    private static String _getDefaultBrowser() {
        return WinGlassNative.defaultBrowser();
    }

    private static boolean getBoolean(String propname, boolean defval, String description) {
        String str = System.getProperty(propname);
        if (str == null) {
            str = System.getenv(propname);
        }
        if (str == null) {
            return defval;
        }
        Boolean ret = Boolean.parseBoolean(str);
        if (PrismSettings.verbose) {
            System.out.println((ret ? "" : "not ")+description);
        }
        return ret;
    }

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

    static {
        verbose = Boolean.getBoolean("javafx.verbose");
        if (PrismSettings.allowHiDPIScaling) {
            overrideUIScale = getFloat("glass.win.uiScale", -1.0f, "Forcing UI scaling factor: ");
            // We only parse these if verbose, to inform the user...
            if (PrismSettings.verbose) {
                getFloat("glass.win.renderScale", -1.0f,
                         "(No longer supported) Rendering scaling factor: ");
                getFloat("glass.win.minHiDPI", 1.5f,
                         "(No longer supported) UI scaling threshold: ");
                getBoolean("glass.win.forceIntegerRenderScale", true,
                           "(No longer supported) force integer rendering scale");
            }
        } else {
            overrideUIScale = 1.0f;
        }
        // Load required Microsoft runtime DLLs on Windows platforms
        Toolkit.loadMSWindowsLibraries();
        // Kept although this class declares no native method: glass.dll is still the JNI library of the
        // accessibility peers (JNI_OnLoad), and Application's loaded flag is set here as on every platform.
        // The initIDs call that followed has no replacement: the UI scale override is read in Java by
        // staticScreen_getScreens, and the method id it cached for CheckAndClearException is cached by
        // WinAccessible (see the class comment).
        Application.loadNativeLibrary();
        // Hands PlatformSupport::updatePreferences (native-glass/win/PlatformSupport.cpp) the callback
        // that replaced its java.util.Map building; since that C was deleted, a change
        // that finds no callback installed is not delivered at all. It belongs here and nowhere
        // else: this initializer runs on the launcher thread, after the library is loaded and before
        // Application.run() creates the toolkit window, so the table is installed before the first
        // WndProc message and is never written from a second thread. Installing it lazily - on the
        // first preference read, say - would drop every notification that arrives during startup;
        // installing it in a change set that did not also wire WinPreferences would drop all of them.
        WinGlassNative.installPreferencesCallback();
        // Hands glass.dll the one callback that replaces all three of GlassApplication.cpp's
        // Runnable.run upcalls - the launchable, _invokeAndWait and _submitForLaterInvocation, which
        // all resolved the same cached method id. It belongs here for the same reason as the line
        // above, and for one more: gwin_run_loop runs the launchable before it starts pumping, so a
        // table installed any later than this would miss the very first thing the toolkit thread does.
        WinGlassNative.installApplicationCallback();
        // Hands glass.dll the two tables that replace the 26 View / WinGestureSupport upcalls of
        // GlassView.cpp, ViewContainer.cpp and FullScreenWindow.cpp (GwinViewCallbacks and
        // GwinGestureCallbacks). Here for the same reason as the two lines above: before the toolkit
        // window exists, so before the first WndProc message, and from one thread. What is NOT done
        // here is initialising WinGestureSupport - its initializer must run on the toolkit thread, and
        // this one does not; see runLoop.
        WinGlassNative.installViewCallbacks();
        // Hands glass.dll the table that replaces the 11 Window / WinWindow upcalls of GlassWindow.cpp
        // and the DeleteGlobalRef of ~GlassWindow (GwinWindowCallbacks). Here for the same reason as
        // the three lines above, and in the same change set as WinWindow's forwarders: an installed
        // table with no registry behind it drops every window event, and JNI-created windows under
        // an installed table deliver an id the registry does not know.
        WinGlassNative.installWindowCallbacks();
        // Hands glass.dll the one-slot table that replaces the Screen.notifySettingsChanged upcall of
        // GlassScreen::HandleDisplayChange (GwinScreenCallbacks), which WM_DISPLAYCHANGE,
        // WM_SETTINGCHANGE(SPI_SETWORKAREA) and WM_DPICHANGED reach. Here for the same reason as the four
        // lines above - before the toolkit window exists, so before the first of those messages, and from
        // one thread - and in the same change set as the Java staticScreen_getScreens: the JNI path it
        // replaces looked Screen up through the class loader _setClassLoader handed the C, and nothing
        // hands the C a class loader any more.
        WinGlassNative.installScreenCallbacks();
    }

    private final InvokeLaterDispatcher invokeLaterDispatcher;

    WinApplication() {
        // Embedded in SWT, with shared event thread
        boolean isEventThread = Boolean.getBoolean("javafx.embed.isEventThread");
        if (!isEventThread) {
            invokeLaterDispatcher = new InvokeLaterDispatcher(this);
            invokeLaterDispatcher.start();
        } else {
            invokeLaterDispatcher = null;
        }
    }

    private static boolean verbose;

    private static final int Process_DPI_Unaware            = 0;
    private static final int Process_System_DPI_Aware       = 1;
    private static final int Process_Per_Monitor_DPI_Aware  = 2;

    private static int getDesiredAwarenesslevel() {
        if (!PrismSettings.allowHiDPIScaling) {
            return Process_DPI_Unaware;
        }
        String awareRequested = System.getProperty("javafx.glass.winDPIawareness");
        if (awareRequested != null) {
            awareRequested = awareRequested.toLowerCase();
            if (awareRequested.equals("aware")) {
                return Process_System_DPI_Aware;
            } else if (awareRequested.equals("permonitor")) {
                return Process_Per_Monitor_DPI_Aware;
            } else {
                if (!awareRequested.equals("unaware")) {
                    System.err.println("unrecognized DPI awareness request, defaulting to unaware: "+awareRequested);
                }
                return Process_DPI_Unaware;
            }
        }
        return Process_Per_Monitor_DPI_Aware;
    }

    /**
     * The SWT-embedded branch and the thread creation are unchanged; what moved is the pump itself,
     * from {@code Java_com_sun_glass_ui_win_WinApplication__1runLoop} to
     * {@link WinGlassNative#runLoop}. {@code setEventThread} is still called on the toolkit thread
     * before it is started, so {@code checkEventThread()} measures exactly the thread it always did -
     * one FFM downcall now sits where a JNI native method used to, on that same thread, and nothing on
     * this path may introduce a helper thread.
     * <p>
     * {@link WinGestureSupport#ensureInitialized()} runs on that thread, in both branches, before the
     * launchable: its static initializer needs the event thread ({@code TouchInputSupport}'s
     * constructor checks it) and must have run before the first touch or gesture event reaches the
     * {@code GwinGestureCallbacks} table. The JNI initialised the class from
     * {@code GlassApplication::ClassForName} inside {@code GlassWindow::Create}, on this same thread;
     * doing it here is earlier - no window exists yet - and never later.
     * <p>
     * {@link #initToolkit} stands where {@code _init} stood, in both branches. {@code _setClassLoader}, which
     * ran first on the launcher thread, is gone without a replacement: its global ref fed
     * {@code GlassApplication::ClassForName}, whose last caller was the JNI screen enumeration.
     */
    @Override
    protected void runLoop(final Runnable launchable) {
        boolean isEventThread = Boolean.getBoolean("javafx.embed.isEventThread");
        int awareness = getDesiredAwarenesslevel();

        if (isEventThread) {
            initToolkit(awareness);
            setEventThread(Thread.currentThread());
            WinGestureSupport.ensureInitialized();
            launchable.run();
            return;
        }
        final Thread toolkitThread =
            new Thread(() -> {
                initToolkit(awareness);
                WinGestureSupport.ensureInitialized();
                WinGlassNative.runLoop(launchable);
            }, "WindowsNativeRunloopThread");
        setEventThread(toolkitThread);
        toolkitThread.start();
    }

    /**
     * Was {@code Java_com_sun_glass_ui_win_WinApplication__1init} ({@code native-glass/win/GlassApplication.cpp}):
     * {@code if (IS_WINVISTA) GlassScreen::LoadDPIFuncs(awarenessRequested);}, then
     * {@code new GlassApplication(_this)}, answering the toolkit HWND. The DPI step is a {@code WRAPPER} and is
     * {@link WinGlassNative#loadDpiFuncs} now - the same one-shot latch the screen enumeration takes, which is why
     * it moved in the same change set as {@link #staticScreen_getScreens}: split, C and Java would each have held
     * a latch of their own. The rest is {@link WinGlassNative#appCreate()}, the same object built without a
     * {@code jobject}, so nothing in C pins this peer any more.
     * <p>
     * <b>The order is the C's and must stay so.</b> The DPI awareness is requested before the toolkit window is
     * created: a window created while the process is DPI-unaware stays unaware, and in a host without a DPI
     * manifest every monitor would then report 96 DPI - a blurry UI and no exception. Under the JDK launchers the
     * request is refused ({@code E_ACCESSDENIED}; their manifest already declares PerMonitorV2), so no test can
     * observe the order and it is kept by reading. {@code loadDpiFuncs} is never called from a static
     * initializer. Both steps run on the calling thread, the one that goes on to pump (or the SWT embedder's),
     * so the {@code RoInitialize} inside {@code appCreate} still precedes that thread's {@code OleInitialize}.
     * <p>
     * The HWND is discarded, as {@code _init}'s {@code jlong} was: {@code gwin_terminate_loop} reads the live
     * instance, and a cached HWND value could name a recycled window.
     */
    private static void initToolkit(int awareness) {
        if (WinGlassNative.isWindowsVersionAtLeast(6, 0)) {    // IS_WINVISTA
            WinGlassNative.loadDpiFuncs(awareness);
        }
        WinGlassNative.appCreate();
    }

    /**
     * {@code DestroyWindow} on the toolkit window, which is what ends the pump in
     * {@link WinGlassNative#runLoop}. Still guarded by {@code getEventThread() != null}, and still
     * called on the toolkit thread, because that is the only thread allowed to destroy it.
     */
    @Override protected void finishTerminating() {
        final Thread toolkitThread = getEventThread();
        if (toolkitThread != null) {
            WinGlassNative.terminateLoop();
            setEventThread(null);
        }
        super.finishTerminating();
    }

    @Override public boolean shouldUpdateWindow() {
        return true;
    }

    /*
     * The value _leaveNestedEventLoop was given, held until the pump it stops has returned.
     *
     * This was GlassApplication::sm_nestedLoopReturnValue, a JGlobalRef in C (GlassApplication.cpp:48);
     * the C's EnterNestedEventLoop(JNIEnv*) read it, cleared it and returned it, which is what the two
     * methods below do in Java. Nothing about the ABI needs it: the value never left Java, it was only
     * parked in C for the duration of a pump.
     *
     * Not volatile, and the reason is thread identity rather than any argument about
     * Application.checkEventThread() - which is switchable off with glass.disableThreadChecks and so
     * proves nothing. One thread writes and reads this: _leaveNestedEventLoop runs inside the nested
     * pump, on the Glass toolkit thread, and _enterNestedEventLoop is that same thread deeper on its
     * own stack, so the write happens-before the read by program order.
     */
    private static Object nestedLoopReturnValue;

    @Override protected Object _enterNestedEventLoop() {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyEnteringNestedEventLoop();
        }
        try {
            WinGlassNative.enterNestedEventLoop();
            Object retValue = nestedLoopReturnValue;
            nestedLoopReturnValue = null;
            return retValue;
        } finally {
            if (invokeLaterDispatcher != null) {
                invokeLaterDispatcher.notifyLeftNestedEventLoop();
            }
        }
    }

    @Override protected void _leaveNestedEventLoop(Object retValue) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyLeavingNestedEventLoop();
        }
        // Same order as the C: store the value, then raise the flag.
        nestedLoopReturnValue = retValue;
        WinGlassNative.leaveNestedEventLoop();
    }

    // FACTORY METHODS

    @Override public Window createWindow(Window owner, Screen screen, int styleMask) {
        return new WinWindow(owner, screen, styleMask);
    }

    @Override public View createView() {
        return new WinView();
    }

    @Override public Cursor createCursor(int type) {
        return new WinCursor(type);
    }

    @Override public Cursor createCursor(int x, int y, Pixels pixels) {
        return new WinCursor(x, y, pixels);
    }

    @Override protected void staticCursor_setVisible(boolean visible) {
        WinCursor.setVisible_impl(visible);
    }

    @Override protected Size staticCursor_getBestSize(int width, int height) {
        return WinCursor.getBestSize_impl(width, height);
    }

    @Override public Pixels createPixels(int width, int height, ByteBuffer data) {
        return new WinPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, ByteBuffer data, float scalex, float scaley) {
        return new WinPixels(width, height, data, scalex, scaley);
    }

    @Override public Pixels createPixels(int width, int height, IntBuffer data) {
        return new WinPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        return new WinPixels(width, height, data, scalex, scaley);
    }

    @Override protected int staticPixels_getNativeFormat() {
        return WinPixels.getNativeFormat_impl();
    }

    @Override public GlassRobot createRobot() {
        return new WinRobot();
    }

    @Override protected double staticScreen_getVideoRefreshPeriod() {
        return 0.0;     // indicate millisecond resolution
    }

    /**
     * Was {@code Java_com_sun_glass_ui_win_WinApplication_staticScreen_1getScreens}, i.e.
     * {@code GlassScreen::CreateJavaScreens}: two {@code EnumDisplayMonitors} passes with
     * {@code GetMonitorSettings} for each monitor ({@code WRAPPER}, now {@link WinGlassNative#collectMonitors()}),
     * the primary pick and the anchoring of the monitors' FX rectangles ({@code PURE}, now
     * {@link WinScreenLayout}), and a {@code Screen} built in C for each ({@code JNI-ONLY}, now built in Java).
     * {@code null} still means that no monitor was enumerated, which {@code Screen.initScreens} reports.
     * <p>
     * {@link #overrideUIScale} is read here, at enumeration time - the value {@code initIDs} used to copy into
     * {@code GlassApplication::overrideUIScale}, whose only reader was this enumeration. It must never be read
     * from a static initializer of {@link WinGlassNative} or {@link WinScreenLayout}: this class's own
     * initializer calls into the facade, which would find it half-initialized.
     * <p>
     * That the two produce the same table rests on three comparisons made while the JNI still existed: the
     * arrangement against the C through a test hook on 2,033 layouts ({@code screen-anchor-golden.txt}); both
     * whole paths on one machine at its own scale and at a forced 1.75, headless and on a started toolkit, every
     * one of the twenty values of every screen equal; and both equal to the table the JNI implementation in commit
     * {@code 8492cb03b0} produced, which the module tests keep ({@code screen-snapshot-golden*.txt}). What stays in C
     * is only the event that the monitors may have changed ({@code GwinScreenCallbacks}, which the static
     * initializer installs).
     */
    @Override protected Screen[] staticScreen_getScreens() {
        return WinScreenLayout.arrange(WinGlassNative.collectMonitors(), overrideUIScale);
    }

    @Override public Timer createTimer(Runnable runnable) {
        return new WinTimer(runnable);
    }

    @Override protected int staticTimer_getMinPeriod() {
        return WinTimer.getMinPeriod_impl();
    }

    @Override protected int staticTimer_getMaxPeriod() {
        return WinTimer.getMaxPeriod_impl();
    }

    @Override public Accessible createAccessible() {
        return new WinAccessible();
    }

    @Override protected FileChooserResult staticCommonDialogs_showFileChooser(Window owner, String folder, String filename, String title, int type,
                                             boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyEnteringNestedEventLoop();
        }
        return WinCommonDialogs.showFileChooser_impl(owner, folder, filename, title, type, multipleMode, extensionFilters, defaultFilterIndex);
    }

    @Override protected File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyEnteringNestedEventLoop();
        }
        return WinCommonDialogs.showFolderChooser_impl(owner, folder, title);
    }

    @Override
    protected void _showDocument(String uri) {
        String browser = _getDefaultBrowser();
        if (browser == null) {
            System.err.println("Could not retrieve default browser");
            return;
        }

        if (browser.contains("%1")) {
            browser = browser.replace("%1", uri);
        } else {
            browser = browser + " " + uri;
        }

        List<String> command = new ArrayList<>();
        int firstIndex = browser.indexOf("\"");
        int secondIndex = browser.indexOf("\"", firstIndex + 1);

        if (firstIndex == 0 && secondIndex != firstIndex) {
            command.add(browser.substring(firstIndex, secondIndex + 1));
            browser = browser.substring(secondIndex + 1).trim();
        }
        command.addAll(Arrays.asList(browser.split(" ")));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.start();
        }  catch (Exception e) {
            System.err.println("Error opening URI : " + uri + " : " +
                    e.getLocalizedMessage());
        }

    }

    @Override protected long staticView_getMultiClickTime() {
        return WinView.getMultiClickTime_impl();
    }

    @Override protected int staticView_getMultiClickMaxX() {
        return WinView.getMultiClickMaxX_impl();
    }

    @Override protected int staticView_getMultiClickMaxY() {
        return WinView.getMultiClickMaxY_impl();
    }

    /**
     * Was {@code Java_com_sun_glass_ui_win_WinApplication__1invokeAndWait}: {@code SendMessage} to the
     * toolkit window, blocking until the runnable has run there. {@code Application.invokeAndWait}
     * still short-circuits the case where the caller is itself the toolkit thread, so this is reached
     * only from another thread.
     */
    @Override protected void _invokeAndWait(Runnable runnable) {
        WinGlassNative.invokeAndWait(runnable);
    }

    /**
     * Was {@code Java_com_sun_glass_ui_win_WinApplication__1submitForLaterInvocation}:
     * {@code PostMessage} to the toolkit window. Callable from any thread, and the usual caller is
     * {@link #invokeLaterDispatcher}'s own thread.
     */
    // InvokeLaterDispatcher.InvokeLaterSubmitter
    @Override public void submitForLaterInvocation(Runnable r) {
        WinGlassNative.submitForLaterInvocation(r);
    }

    @Override protected void _invokeLater(Runnable runnable) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.invokeLater(runnable);
        } else {
            submitForLaterInvocation(runnable);
        }
    }

    @Override
    protected boolean _supportsInputMethods() {
        return true;
    }

    @Override
    protected boolean _supportsTransparentWindows() {
        return true;
    }

    /**
     * Was {@code Java_com_sun_glass_ui_win_WinApplication__1supportsUnifiedWindows}
     * ({@code native-glass/win/GlassApplication.cpp:504-508}), whose entire body was
     * {@code return (IS_WINVISTA);} - i.e. {@code IS_WINVER_ATLEAST(6, 0)}, four lines of byte
     * arithmetic over {@code kernel32!GetVersion} ({@code Utils.h:61-66}). A {@code WRAPPER}, so
     * there is no C left for it.
     * <p>
     * {@link WinGlassNative#isWindowsVersionAtLeast} keeps {@code GetVersion}'s answer including its
     * famous lie: a process whose manifest does not claim a newer Windows is told 6.2 whatever it is
     * running on. That is a property of the process, so Java is told exactly what the C was told.
     */
    @Override
    protected boolean _supportsUnifiedWindows() {
        return WinGlassNative.isWindowsVersionAtLeast(6, 0);
    }

    @Override
    protected boolean _supportsExtendedWindows() {
        return true;
    }

    @Override
    public String getDataDirectory() {
        checkEventThread();
        String baseDirectory = System.getenv("APPDATA");
        if (baseDirectory == null || baseDirectory.length() == 0) {
            return super.getDataDirectory();
        }
        return baseDirectory + File.separator + name + File.separator;
    }

    /**
     * Was {@code Java_com_sun_glass_ui_win_WinApplication__1getKeyCodeForChar}
     * ({@code native-glass/win/KeyTable.cpp}), which was a one-line forwarder to
     * {@code gwin_key_code_for_char} until it was deleted, unreachable, together with this declaration's
     * {@code native} modifier.
     * <p>
     * The body stays in C: it is 40% {@code KeyTable.cpp} table lookup - a table the native
     * {@code WndProc} input path reads directly, so porting it would make two copies that no test
     * could prove agree on an arbitrary keyboard layout - and it needs
     * {@code GlassApplication::GetMainThreadId()}, which is native-only state.
     * <p>
     * The {@code char} is narrowed here rather than in {@link WinGlassNative}: FFM has no {@code char}
     * value layout, the C parameter is a {@code uint16_t}, and the 16 bits are the same either way.
     */
    @Override
    protected int _getKeyCodeForChar(char c, int hint) {
        return WinGlassNative.keyCodeForChar((short) c, hint);
    }

    /**
     * Was {@code Java_com_sun_glass_ui_win_WinApplication__1isKeyLocked}
     * ({@code native-glass/win/KeyTable.cpp:421-439}), which was a two-case switch over
     * {@code user32!GetKeyState} and nothing else. {@link WinGlassNative#keyLockState} is that switch,
     * unchanged: only {@code VK_CAPS_LOCK} and {@code VK_NUM_LOCK} ask Windows anything, and what is
     * tested is bit 0 of the answer.
     */
    @Override
    protected int _isKeyLocked(int keyCode) {
        return WinGlassNative.keyLockState(keyCode);
    }

    /**
     * Was {@code Java_com_sun_glass_ui_win_WinApplication_getPlatformPreferences}, which answered the
     * inline {@code GlassApplication::GetPlatformPreferences()} and so
     * {@code PlatformSupport::collectPreferences(PT_ALL)}: a {@code java.util.HashMap} built in C with
     * {@code NewObject}, five {@code Map.put} upcall sites and two {@code Color.rgb} ones. (All three are
     * cited by symbol; read them with {@code git show 8492cb03b0:} and the path under
     * {@code modules/javafx.graphics/src/main/native-glass/win/} - {@code GlassApplication.cpp},
     * {@code GlassApplication.h}, {@code PlatformSupport.cpp}.) It was the last map
     * {@code PlatformSupport.cpp} built, and a {@code JNI-ONLY} body: every value in it came from the
     * same four sources {@link WinPreferences} already read for every later update, so there is no C
     * left for it.
     * <p>
     * The map is returned as {@code collect} builds it - a plain, mutable {@code HashMap} - because that
     * is what the C returned (the {@code return prefs;} of {@code collectPreferences}); only
     * {@code updatePreferences} wrapped
     * its map in {@code Collections.unmodifiableMap}, and {@link WinPreferences#update} still does. The
     * one caller, {@code QuantumToolkit.runToolkit}, hands it to {@code PlatformPreferences.update},
     * which only reads it.
     * <p>
     * That caller runs on the toolkit thread after {@code runLoop} has initialised the toolkit, which is
     * the only situation in which the two arms were compared, and the only one in which they must agree:
     * {@code tests/system}'s {@code WinPreferencesParityTest} found the same 23 keys, with equal values
     * of the same classes, in both on a started toolkit before the {@code native} was removed. Without a
     * toolkit the C answered null (a null {@code pInstance} in {@code GetPlatformPreferences}) where this
     * answers the {@code user32} keys - a path no caller takes. The WinRT half of the map still needs
     * that thread: the {@code IUISettings} and {@code INetworkInformation} objects live in the
     * single-threaded apartment it created. The state machines of the startup map and of the updates
     * line up as before: {@code collect} does not touch {@code WinPreferences.collected}, just as
     * {@code collectPreferences} never touched the C's former {@code preferences} field, so the first update
     * after startup still counts as a change.
     */
    @Override
    public Map<String, Object> getPlatformPreferences() {
        return WinPreferences.collect(WinGlassNative.GWIN_PT_ALL);
    }

    /**
     * The one upcall of {@code PlatformSupport.cpp} that was an event and not a data call
     * ({@code :188}, {@code env->CallVoidMethod(application, notifyPreferencesChangedMID, ...)}), now
     * reached from {@link WinPreferences#update}.
     * <p>
     * It needs a forwarder rather than a direct call because {@code notifyPreferencesChanged} is
     * {@code protected} ({@code Application.java:258}) and {@code WinPreferences} is not a subclass,
     * so JLS 6.6.2 forbids the call there - a {@code javac} error, where JNI's
     * {@code CallVoidMethod} ignored access control entirely.
     * <p>
     * The instance is the one the C held a global ref to: the peer {@code Application.run} created.
     * Before that, and after {@code Application} clears it, there is nobody to notify - a state the C
     * could not reach, because {@code updatePreferences} only ran while the toolkit window existed.
     */
    static void firePreferencesChanged(Map<String, Object> preferences) {
        if (Application.GetApplication() instanceof WinApplication application) {
            application.notifyPreferencesChanged(preferences);
        }
    }

    // This list needs to be kept in sync with the keys WinPreferences collects.
    @Override
    public Map<String, PreferenceMapping<?, ?>> getPlatformKeyMappings() {
        return Map.of(
            "Windows.UIColor.Foreground", new PreferenceMapping<>("foregroundColor", Color.class),
            "Windows.UIColor.Background", new PreferenceMapping<>("backgroundColor", Color.class),
            "Windows.UIColor.Accent", new PreferenceMapping<>("accentColor", Color.class),
            "Windows.UISettings.AdvancedEffectsEnabled", new PreferenceMapping<>("reducedTransparency", Boolean.class, b -> !b),
            "Windows.UISettings.AutoHideScrollBars", new PreferenceMapping<>("persistentScrollBars", Boolean.class, b -> !b),
            "Windows.SPI.ClientAreaAnimation", new PreferenceMapping<>("reducedMotion", Boolean.class, b -> !b),
            "Windows.NetworkInformation.InternetCostType", new PreferenceMapping<>(
                "reducedData", String.class, v -> switch (v) {
                    case "Unknown", "Unrestricted" -> false;
                    default -> true;
                })
        );
    }

    // This list needs to be kept in sync with the keys WinPreferences collects.
    @Override
    public Map<String, Class<?>> getPlatformKeys() {
        return Map.ofEntries(
            Map.entry("Windows.SPI.HighContrast", Boolean.class),
            Map.entry("Windows.SPI.HighContrastColorScheme", String.class),
            Map.entry("Windows.SPI.ClientAreaAnimation", Boolean.class),
            Map.entry("Windows.SysColor.COLOR_3DFACE", Color.class),
            Map.entry("Windows.SysColor.COLOR_BTNTEXT", Color.class),
            Map.entry("Windows.SysColor.COLOR_GRAYTEXT", Color.class),
            Map.entry("Windows.SysColor.COLOR_HIGHLIGHT", Color.class),
            Map.entry("Windows.SysColor.COLOR_HIGHLIGHTTEXT", Color.class),
            Map.entry("Windows.SysColor.COLOR_HOTLIGHT", Color.class),
            Map.entry("Windows.SysColor.COLOR_WINDOW", Color.class),
            Map.entry("Windows.SysColor.COLOR_WINDOWTEXT", Color.class),
            Map.entry("Windows.UIColor.Background", Color.class),
            Map.entry("Windows.UIColor.Foreground", Color.class),
            Map.entry("Windows.UIColor.AccentDark3", Color.class),
            Map.entry("Windows.UIColor.AccentDark2", Color.class),
            Map.entry("Windows.UIColor.AccentDark1", Color.class),
            Map.entry("Windows.UIColor.Accent", Color.class),
            Map.entry("Windows.UIColor.AccentLight1", Color.class),
            Map.entry("Windows.UIColor.AccentLight2", Color.class),
            Map.entry("Windows.UIColor.AccentLight3", Color.class),
            Map.entry("Windows.UISettings.AdvancedEffectsEnabled", Boolean.class),
            Map.entry("Windows.UISettings.AutoHideScrollBars", Boolean.class),
            Map.entry("Windows.NetworkInformation.InternetCostType", String.class)
        );
    }
}
