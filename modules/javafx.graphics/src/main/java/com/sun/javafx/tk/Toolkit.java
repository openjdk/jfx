/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk;

import javafx.application.ConditionalFeature;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Dimension2D;
import javafx.scene.effect.BlurType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.runtime.VersionInfo;
import com.sun.javafx.runtime.async.AsyncOperation;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractPrimaryTimer;
import com.sun.scenario.effect.AbstractShadow.ShadowMode;
import com.sun.scenario.effect.Color4f;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import java.util.Optional;


public abstract class Toolkit {
    private static String tk;
    private static Toolkit TOOLKIT;
    private static Thread fxUserThread = null;

    private static final String QUANTUM_TOOLKIT     = "com.sun.javafx.tk.quantum.QuantumToolkit";
    private static final String DEFAULT_TOOLKIT     = QUANTUM_TOOLKIT;

    private static final Map gradientMap = new WeakHashMap();

    @SuppressWarnings("removal")
    private static final boolean verbose = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("javafx.verbose"));

    private static final String[] msLibNames = {
        "api-ms-win-core-console-l1-1-0",
        "api-ms-win-core-console-l1-2-0",
        "api-ms-win-core-datetime-l1-1-0",
        "api-ms-win-core-debug-l1-1-0",
        "api-ms-win-core-errorhandling-l1-1-0",
        "api-ms-win-core-file-l1-1-0",
        "api-ms-win-core-file-l1-2-0",
        "api-ms-win-core-file-l2-1-0",
        "api-ms-win-core-handle-l1-1-0",
        "api-ms-win-core-heap-l1-1-0",
        "api-ms-win-core-interlocked-l1-1-0",
        "api-ms-win-core-libraryloader-l1-1-0",
        "api-ms-win-core-localization-l1-2-0",
        "api-ms-win-core-memory-l1-1-0",
        "api-ms-win-core-namedpipe-l1-1-0",
        "api-ms-win-core-processenvironment-l1-1-0",
        "api-ms-win-core-processthreads-l1-1-0",
        "api-ms-win-core-processthreads-l1-1-1",
        "api-ms-win-core-profile-l1-1-0",
        "api-ms-win-core-rtlsupport-l1-1-0",
        "api-ms-win-core-string-l1-1-0",
        "api-ms-win-core-synch-l1-1-0",
        "api-ms-win-core-synch-l1-2-0",
        "api-ms-win-core-sysinfo-l1-1-0",
        "api-ms-win-core-timezone-l1-1-0",
        "api-ms-win-core-util-l1-1-0",
        "api-ms-win-crt-conio-l1-1-0",
        "api-ms-win-crt-convert-l1-1-0",
        "api-ms-win-crt-environment-l1-1-0",
        "api-ms-win-crt-filesystem-l1-1-0",
        "api-ms-win-crt-heap-l1-1-0",
        "api-ms-win-crt-locale-l1-1-0",
        "api-ms-win-crt-math-l1-1-0",
        "api-ms-win-crt-multibyte-l1-1-0",
        "api-ms-win-crt-private-l1-1-0",
        "api-ms-win-crt-process-l1-1-0",
        "api-ms-win-crt-runtime-l1-1-0",
        "api-ms-win-crt-stdio-l1-1-0",
        "api-ms-win-crt-string-l1-1-0",
        "api-ms-win-crt-time-l1-1-0",
        "api-ms-win-crt-utility-l1-1-0",
        "ucrtbase",

        // Finally load VS 2017 DLLs in the following order
        "vcruntime140",
        "vcruntime140_1",
        "msvcp140",
        "msvcp140_1",
        "msvcp140_2"
};

    private static String lookupToolkitClass(String name) {
        if ("prism".equalsIgnoreCase(name)) {
            return QUANTUM_TOOLKIT;
        } else if ("quantum".equalsIgnoreCase(name)) {
            return QUANTUM_TOOLKIT;
        }
        return name;
    }

    public static synchronized void loadMSWindowsLibraries() {
        for (String libName : msLibNames) {
            try {
                NativeLibLoader.loadLibrary(libName);
            } catch (Throwable t) {
                if (verbose) {
                    System.err.println("Error: failed to load "
                            + libName + ".dll : " + t);
                }
            }
        }
    }

    private static String getDefaultToolkit() {
        if (PlatformUtil.isWindows()) {
            return DEFAULT_TOOLKIT;
        } else if (PlatformUtil.isMac()) {
            return DEFAULT_TOOLKIT;
        } else if (PlatformUtil.isLinux()) {
            return DEFAULT_TOOLKIT;
        } else if (PlatformUtil.isIOS()) {
            return DEFAULT_TOOLKIT;
        } else if (PlatformUtil.isAndroid()) {
           return DEFAULT_TOOLKIT;
        }

        throw new UnsupportedOperationException(System.getProperty("os.name") + " is not supported");
    }

    public static synchronized Toolkit getToolkit() {
        if (TOOLKIT != null) {
            return TOOLKIT;
        }

        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            // Get the javafx.version and javafx.runtime.version from a preconstructed
            // java class, VersionInfo, created at build time.
            VersionInfo.setupSystemProperties();
            return null;
        });

        // Load required Microsoft runtime DLLs on Windows platforms
        if (PlatformUtil.isWindows()) {
            loadMSWindowsLibraries();
        }

        boolean userSpecifiedToolkit = true;

        // Check a system property to see if there is a specific toolkit to use.
        // This is not a doPriviledged check so that applications running
        // with a security manager cannot use this unless they have permission
        // to read system properties.
        String forcedToolkit = null;
        try {
            forcedToolkit = System.getProperty("javafx.toolkit");
        } catch (SecurityException ex) {}

        if (forcedToolkit == null) {
            forcedToolkit = tk;
        }
        if (forcedToolkit == null) {
            userSpecifiedToolkit = false;
            forcedToolkit = getDefaultToolkit();
        }

        if (forcedToolkit.indexOf('.') == -1) {
            // Turn a short name into a fully qualified classname
            forcedToolkit = lookupToolkitClass(forcedToolkit);
        }

        boolean printToolkit = verbose
                || (userSpecifiedToolkit && !forcedToolkit.endsWith("StubToolkit"));

        try {
            Class clz = null;

            try {
                // try our priveledged loader first
                final ClassLoader loader = Toolkit.class.getClassLoader();
                clz = Class.forName(forcedToolkit, false, loader);
            } catch (ClassNotFoundException e) {
                // fall back and try the application class loader
                final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                clz = Class.forName(forcedToolkit, false, loader);
            }

            // Check that clz is a subclass of Toolkit
            if (!Toolkit.class.isAssignableFrom(clz)) {
                throw new IllegalArgumentException("Unrecognized FX Toolkit class: "
                        + forcedToolkit);
            }

            TOOLKIT = (Toolkit)clz.getDeclaredConstructor().newInstance();
            if (TOOLKIT.init()) {
                if (printToolkit) {
                    System.err.println("JavaFX: using " + forcedToolkit);
                }
                return TOOLKIT;
            }
            TOOLKIT = null;
        } catch (Exception any) {
            TOOLKIT = null;
            any.printStackTrace();
        }

        throw new RuntimeException("No toolkit found");
    }

    protected static Thread getFxUserThread() {
        return fxUserThread;
    }

    protected static void setFxUserThread(Thread t) {
        if (fxUserThread != null) {
            throw new IllegalStateException("Error: FX User Thread already initialized");
        }

        fxUserThread = t;
    }

    public void checkFxUserThread() {
        // Throw exception if not on FX user thread
        if (!isFxUserThread()) {
            throw new IllegalStateException("Not on FX application thread; currentThread = "
                    + Thread.currentThread().getName());
        }
    }

    // Toolkit can override this if needed
    public boolean isFxUserThread() {
        return Thread.currentThread() == fxUserThread;
    }

    protected Toolkit() {
    }

    public abstract boolean init();

    /**
     * Indicates whether a nested event loop can be started from the current thread in the current state.
     * A nested event loop can be started from an event handler or from a {@code Runnable} passed to
     * {@code Platform.runLater(Runnable)}.
     * This method must be called on the JavaFX Application thread.
     *
     * @return {@code true} if a nested event loop can be started, and {@code false} otherwise.
     *
     * @throws IllegalStateException if this method is called on a thread other than the JavaFX Application Thread.
     */
    public abstract boolean canStartNestedEventLoop();

    /**
     * Enter a nested event loop and block until the corresponding
     * exitNestedEventLoop call is made.
     * The key passed into this method is used to
     * uniquely identify the matched enter/exit pair. This method creates a
     * new nested event loop and blocks until the corresponding
     * exitNestedEventLoop method is called with the same key.
     * The return value of this method will be the {@code rval}
     * object supplied to the exitNestedEventLoop method call that unblocks it.
     *
     * @param key the Object that identifies the nested event loop, which
     * must not be null
     *
     * @throws IllegalArgumentException if the specified key is associated
     * with a nested event loop that has not yet returned
     *
     * @throws NullPointerException if the key is null
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the FX Application thread
     *
     * @return the value passed into the corresponding call to exitEventLoop
     */
    public abstract Object enterNestedEventLoop(Object key);

    /**
     * Exit a nested event loop and unblock the caller of the
     * corresponding enterNestedEventLoop.
     * The key passed into this method is used to
     * uniquely identify the matched enter/exit pair. This method causes the
     * nested event loop that was previously created with the key to exit and
     * return control to the caller. If the specified nested event loop is not
     * the inner-most loop then it will not return until all other inner loops
     * also exit.
     *
     * @param key the Object that identifies the nested event loop, which
     * must not be null
     *
     * @param rval an Object that is returned to the caller of the
     * corresponding enterNestedEventLoop. This may be null.
     *
     * @throws IllegalArgumentException if the specified key is not associated
     * with an active nested event loop
     *
     * @throws NullPointerException if the key is null
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the FX Application thread
     */
    public abstract void exitNestedEventLoop(Object key, Object rval);

    public abstract void exitAllNestedEventLoops();

    public abstract boolean isNestedLoopRunning();

    public abstract TKStage createTKStage(Window peerWindow, boolean securityDialog, StageStyle stageStyle, boolean primary, Modality modality, TKStage owner, boolean rtl, @SuppressWarnings("removal") AccessControlContext acc);

    public abstract TKStage createTKPopupStage(Window peerWindow, StageStyle popupStyle, TKStage owner, @SuppressWarnings("removal") AccessControlContext acc);
    public abstract TKStage createTKEmbeddedStage(HostInterface host, @SuppressWarnings("removal") AccessControlContext acc);

    @SuppressWarnings("removal")
    private final Map<TKPulseListener,AccessControlContext> stagePulseListeners = new WeakHashMap<>();
    @SuppressWarnings("removal")
    private final Map<TKPulseListener,AccessControlContext> scenePulseListeners = new WeakHashMap<>();
    @SuppressWarnings("removal")
    private final Map<TKPulseListener,AccessControlContext> postScenePulseListeners = new WeakHashMap<>();
    @SuppressWarnings("removal")
    private final Map<TKListener,AccessControlContext> toolkitListeners = new WeakHashMap<>();

    // The set of shutdown hooks is strongly held to avoid premature GC.
    private final Set<Runnable> shutdownHooks = new HashSet<>();

    @SuppressWarnings("removal")
    private void runPulse(final TKPulseListener listener,
            final AccessControlContext acc) {

        if (acc == null) {
            throw new IllegalStateException("Invalid AccessControlContext");
        }

        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            listener.pulse();
            return null;
        }, acc);
    }

    public void firePulse() {
        // Stages need to be notified of pulses before scenes so the Stage can resized
        // and those changes propogated to scene before it gets its pulse to update

        // Copy of listener map
        @SuppressWarnings("removal")
        final Map<TKPulseListener,AccessControlContext> stagePulseList =
                new WeakHashMap<>();
        @SuppressWarnings("removal")
        final Map<TKPulseListener,AccessControlContext> scenePulseList =
                new WeakHashMap<>();
        @SuppressWarnings("removal")
        final Map<TKPulseListener,AccessControlContext> postScenePulseList =
                new WeakHashMap<>();

        synchronized (this) {
            stagePulseList.putAll(stagePulseListeners);
            scenePulseList.putAll(scenePulseListeners);
            postScenePulseList.putAll(postScenePulseListeners);
        }
        for (@SuppressWarnings("removal") Map.Entry<TKPulseListener,AccessControlContext> entry : stagePulseList.entrySet()) {
            runPulse(entry.getKey(), entry.getValue());
        }
        for (@SuppressWarnings("removal") Map.Entry<TKPulseListener,AccessControlContext> entry : scenePulseList.entrySet()) {
            runPulse(entry.getKey(), entry.getValue());
        }
        for (@SuppressWarnings("removal") Map.Entry<TKPulseListener,AccessControlContext> entry : postScenePulseList.entrySet()) {
            runPulse(entry.getKey(), entry.getValue());
        }

        if (lastTkPulseListener != null) {
            runPulse(lastTkPulseListener, lastTkPulseAcc);
        }
    }
    public void addStageTkPulseListener(TKPulseListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            @SuppressWarnings("removal")
            AccessControlContext acc = AccessController.getContext();
            stagePulseListeners.put(listener, acc);
        }
    }
    public void removeStageTkPulseListener(TKPulseListener listener) {
        synchronized (this) {
            stagePulseListeners.remove(listener);
        }
    }
    public void addSceneTkPulseListener(TKPulseListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            @SuppressWarnings("removal")
            AccessControlContext acc = AccessController.getContext();
            scenePulseListeners.put(listener, acc);
        }
    }
    public void removeSceneTkPulseListener(TKPulseListener listener) {
        synchronized (this) {
            scenePulseListeners.remove(listener);
        }
    }
    public void addPostSceneTkPulseListener(TKPulseListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            @SuppressWarnings("removal")
            AccessControlContext acc = AccessController.getContext();
            postScenePulseListeners.put(listener, acc);
        }
    }
    public void removePostSceneTkPulseListener(TKPulseListener listener) {
        synchronized (this) {
            postScenePulseListeners.remove(listener);
        }
    }

    public void addTkListener(TKListener listener) {
        if (listener == null) {
            return;
        }
        @SuppressWarnings("removal")
        AccessControlContext acc = AccessController.getContext();
        toolkitListeners.put(listener, acc);
    }

    public void removeTkListener(TKListener listener) {
        toolkitListeners.remove(listener);
    }

    private TKPulseListener lastTkPulseListener = null;
    @SuppressWarnings("removal")
    private AccessControlContext lastTkPulseAcc = null;
    @SuppressWarnings("removal")
    public void setLastTkPulseListener(TKPulseListener listener) {
        lastTkPulseAcc = AccessController.getContext();
        lastTkPulseListener = listener;
    }

    public void addShutdownHook(Runnable hook) {
        if (hook == null) {
            return;
        }
        synchronized (shutdownHooks) {
            shutdownHooks.add(hook);
        }
    }

    public void removeShutdownHook(Runnable hook) {
        synchronized (shutdownHooks) {
            shutdownHooks.remove(hook);
        }
    }

    protected void notifyShutdownHooks() {
        List<Runnable> hooks;
        synchronized (shutdownHooks) {
            hooks = new ArrayList<>(shutdownHooks);
            shutdownHooks.clear();
        }

        for (Runnable hook : hooks) {
            hook.run();
        }
    }

    @SuppressWarnings("removal")
    public void notifyWindowListeners(final List<TKStage> windows) {
        for (Map.Entry<TKListener,AccessControlContext> entry : toolkitListeners.entrySet()) {
            final TKListener listener = entry.getKey();
            final AccessControlContext acc = entry.getValue();
            if (acc == null) {
                throw new IllegalStateException("Invalid AccessControlContext");
            }

            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                listener.changedTopLevelWindows(windows);
                return null;
            }, acc);
        }
    }

    public void notifyLastNestedLoopExited() {
        for (TKListener listener: toolkitListeners.keySet()) {
            listener.exitedLastNestedLoop();
        }
    }

    // notify the pulse timer code that we need the next pulse to happen
    // this flag is cleared each cycle so subsequent pulses must be requested
    public abstract void requestNextPulse();

    public abstract Future addRenderJob(RenderJob rj);

    public abstract ImageLoader loadImage(String url,
                                          double width, double height,
                                          boolean preserveRatio,
                                          boolean smooth);
    public abstract ImageLoader loadImage(InputStream stream,
                                          double width, double height,
                                          boolean preserveRatio,
                                          boolean smooth);
    public abstract AsyncOperation loadImageAsync(
                                          AsyncOperationListener<? extends ImageLoader> listener,
                                          String url,
                                          double width, double height,
                                          boolean preserveRatio,
                                          boolean smooth);

    /*
     * The loadPlatformImage method supports the following image types:
     *   - an object returned by the renderToImage method
     *   - an instance of com.sun.prism.Image (in case of prism)
     *   - an instance of an external image object, which can be a BufferedImage
     * If JavaFX Image had one more constructor Image(ImageLoader),
     * we could introduce a different method for external image loading support.
     */

    public abstract ImageLoader loadPlatformImage(Object platformImage);

    public abstract PlatformImage createPlatformImage(int w, int h);

    // Indicates the default state of smooth for ImageView and MediaView
    // Subclasses may override this to provide a platform-specific default
    public boolean getDefaultImageSmooth() { return true; }

    public abstract void startup(Runnable runnable);
    public abstract void defer(Runnable runnable);
    public void exit() {
        fxUserThread = null;
    }

    public abstract Map<Object, Object> getContextMap();
    public abstract int getRefreshRate();
    public abstract void setAnimationRunnable(DelayedRunnable animationRunnable);
    public abstract PerformanceTracker getPerformanceTracker();
    public abstract PerformanceTracker createPerformanceTracker();

    //to be used for testing only
    public abstract void waitFor(Task t);

    private Object checkSingleColor(List<Stop> stops) {
        if (stops.size() == 2) {
            Color c = stops.get(0).getColor();
            if (c.equals(stops.get(1).getColor())) {
                return Toolkit.getPaintAccessor().getPlatformPaint(c);
            }
        }
        return null;
    }

    private Object getPaint(LinearGradient paint) {
        Object p = gradientMap.get(paint);
        if (p != null) {
            return p;
        }
        p = checkSingleColor(paint.getStops());
        if (p == null) {
            p = createLinearGradientPaint(paint);
        }
        gradientMap.put(paint, p);
        return p;
    }

    private Object getPaint(RadialGradient paint) {
        Object p = gradientMap.get(paint);
        if (p != null) {
            return p;
        }
        p = checkSingleColor(paint.getStops());
        if (p == null) {
            p = createRadialGradientPaint(paint);
        }
        gradientMap.put(paint, p);
        return p;
    }

    public Object getPaint(Paint paint) {
        if (paint instanceof Color) {
            return createColorPaint((Color) paint);
        }

        if (paint instanceof LinearGradient) {
            return getPaint((LinearGradient) paint);
        }

        if (paint instanceof RadialGradient) {
            return getPaint((RadialGradient) paint);
        }

        if (paint instanceof ImagePattern) {
            return createImagePatternPaint((ImagePattern) paint);
        }

        return null;
    }

    protected static final double clampStopOffset(double offset) {
        return (offset > 1.0) ? 1.0 :
               (offset < 0.0) ? 0.0 : offset;
    }

    protected abstract Object createColorPaint(Color paint);
    protected abstract Object createLinearGradientPaint(LinearGradient paint);
    protected abstract Object createRadialGradientPaint(RadialGradient paint);
    protected abstract Object createImagePatternPaint(ImagePattern paint);

    public abstract void
        accumulateStrokeBounds(com.sun.javafx.geom.Shape shape,
                               float bbox[],
                               StrokeType type,
                               double strokewidth,
                               StrokeLineCap cap,
                               StrokeLineJoin join,
                               float miterLimit,
                               BaseTransform tx);

    public abstract boolean
        strokeContains(com.sun.javafx.geom.Shape shape,
                       double x, double y,
                       StrokeType type,
                       double strokewidth,
                       StrokeLineCap cap,
                       StrokeLineJoin join,
                       float miterLimit);

    public abstract com.sun.javafx.geom.Shape
        createStrokedShape(com.sun.javafx.geom.Shape shape,
                           StrokeType type,
                           double strokewidth,
                           StrokeLineCap cap,
                           StrokeLineJoin join,
                           float miterLimit,
                           float[] dashArray,
                           float dashOffset);

    public abstract int getKeyCodeForChar(String character, int hint);
    public abstract Dimension2D getBestCursorSize(int preferredWidth, int preferredHeight);
    public abstract int getMaximumCursorColors();
    public abstract PathElement[] convertShapeToFXPath(Object shape);

    public abstract Filterable toFilterable(Image img);
    public abstract FilterContext getFilterContext(Object config);

    public abstract boolean isForwardTraversalKey(KeyEvent e);
    public abstract boolean isBackwardTraversalKey(KeyEvent e);

    public abstract AbstractPrimaryTimer getPrimaryTimer();

    public abstract FontLoader getFontLoader();
    public abstract TextLayoutFactory getTextLayoutFactory();

    public abstract Object createSVGPathObject(SVGPath svgpath);
    public abstract Path2D createSVGPath2D(SVGPath svgpath);

    /**
     * Tests whether the pixel on the given coordinates in the given image
     * is non-empty (not fully transparent). Return value is not defined
     * for pixels out of the image bounds.
     */
    public abstract boolean imageContains(Object image, float x, float y);

    public abstract TKClipboard getSystemClipboard();

    public TKClipboard createLocalClipboard() {
        return new LocalClipboard();
    }

    public abstract TKSystemMenu getSystemMenu();

    public abstract TKClipboard getNamedClipboard(String name);

    public boolean isSupported(ConditionalFeature feature) { return false; }

    public boolean isMSAASupported() { return false; }

    public abstract ScreenConfigurationAccessor setScreenConfigurationListener(TKScreenConfigurationListener listener);

    public abstract Object getPrimaryScreen();

    public abstract List<?> getScreens();

    public abstract ScreenConfigurationAccessor getScreenConfigurationAccessor();

    public abstract void registerDragGestureListener(TKScene s, Set<TransferMode> tm, TKDragGestureListener l);

    /**
     * This function is called when a drag originates within a JavaFX application.
     * This means that drags that originate in other applications / from the OS
     * do not call this function.
     * The argument o represents an object used to identify a scene on which
     * the drag has started.
     */
    public abstract void startDrag(TKScene scene, Set<TransferMode> tm, TKDragSourceListener l, Dragboard dragboard);

    // template function which can be implemented by toolkit impls such that they
    // can be informed when a drag and drop operation has completed. This allows
    // for any cleanup that may need to be done.
    public void stopDrag(Dragboard dragboard) {
        // no-op
    }

    public abstract void enableDrop(TKScene s, TKDropTargetListener l);

    public interface Task {
        boolean isFinished();
    }

    public Color4f toColor4f(Color color) {
        return new Color4f((float)color.getRed(), (float)color.getGreen(), (float)color.getBlue(), (float)color.getOpacity());
    }


    public ShadowMode toShadowMode(BlurType blurType) {
        switch (blurType) {
            case ONE_PASS_BOX:
                return ShadowMode.ONE_PASS_BOX;
            case TWO_PASS_BOX:
                return ShadowMode.TWO_PASS_BOX;
            case THREE_PASS_BOX:
                return ShadowMode.THREE_PASS_BOX;
            default:
                return ShadowMode.GAUSSIAN;
        }
    }

    public abstract void installInputMethodRequests(TKScene scene, InputMethodRequests requests);

    /*
     * ImageRenderingContext holds the many parameters passed to
     * the renderToImage method.
     * The use of the parameters is specified by the renderToImage
     * method.
     * @see #renderToImage
     */
    public static class ImageRenderingContext {
        // Node to be rendered
        public NGNode root;

        // Viewport for rendering
        public int x;
        public int y;
        public int width;
        public int height;

        // Initial transform for root node
        public BaseTransform transform;

        // Rendering parameters either from Scene or SnapShotParams
        public boolean depthBuffer;
        public Object platformPaint;
        public NGCamera camera;
        public NGLightBase[] lights;

        // PlatformImage into which to render or null
        public Object platformImage;
    }

    /*
     * This method renders a PG-graph to a platform image object.
     * The returned object can be turned into a useable
     * scene graph image using the appropriate factor of the
     * Image class.
     * The scale specified in the params is used to scale the
     * entire rendering before any transforms in the nodes are
     * applied.
     * The width and height specified in the params represent
     * the user space dimensions to be rendered.  The returned
     * image will be large enough to hold these dimensions
     * scaled by the scale parameter.
     * The depthBuffer specified in the params is used to determine
     * with or without depthBuffer rendering should be performed.
     * The root node is the root of a tree of toolkit-specific
     * scene graph peer nodes to be rendered and should have
     * been previously created by this toolkit.
     * The platformPaint specified in the params must be
     * generated by the appropriate Toolkit.createPaint method
     * and is used to fill the background of the image before
     * rendering the scene graph.
     * The platformImage specified in the params may be non-null
     * and should be a previous return value from this method.
     * If it is non-null then it may be reused as the return value
     * of this method if it is still valid and large enough to
     * hold the requested size.
     *
     * @param context a ImageRenderingContext instance specifying
     *               the various rendering parameters
     * @return a platform specific image object
     * @see Toolkit.getImageAccessor().fromPlatformImage
     */

    public abstract Object renderToImage(ImageRenderingContext context);

    /**
     * Returns the key code for the key which is commonly used on the
     * corresponding platform as a modifier key in shortcuts. For example
     * it is {@code KeyCode.CONTROL} on Windows (Ctrl + C, Ctrl + V ...) and
     * {@code KeyCode.META} on MacOS (Cmd + C, Cmd + V ...).
     *
     * @return the key code for shortcut modifier key
     */
    public KeyCode getPlatformShortcutKey() {
        return PlatformUtil.isMac() ? KeyCode.META : KeyCode.CONTROL;
    }

    /**
     * Returns the lock state for the given keyCode.
     * @param keyCode the keyCode to check
     * @return the lock state for the given keyCode.
     */
    public abstract Optional<Boolean> isKeyLocked(KeyCode keyCode);

    public abstract FileChooserResult showFileChooser(
            TKStage ownerWindow,
            String title,
            File initialDirectory,
            String initialFileName,
            FileChooserType fileChooserType,
            List<ExtensionFilter> extensionFilters,
            ExtensionFilter selectedFilter);

    public abstract File showDirectoryChooser(
            TKStage ownerWindow,
            String title,
            File initialDirectory);

    /*
     * Methods for obtaining "double-click" speed value.
     */
    public abstract long getMultiClickTime();
    public abstract int getMultiClickMaxX();
    public abstract int getMultiClickMaxY();

    private CountDownLatch pauseScenesLatch = null;

    public interface WritableImageAccessor {
        public void loadTkImage(WritableImage wimg, Object loader);
        public Object getTkImageLoader(WritableImage wimg);
    }

    private static WritableImageAccessor writableImageAccessor = null;

    public static void setWritableImageAccessor(WritableImageAccessor accessor) {
        writableImageAccessor = accessor;
    }

    public static WritableImageAccessor getWritableImageAccessor() {
        return writableImageAccessor;
    }

    public interface PaintAccessor {
        public boolean isMutable(Paint paint);
        public Object getPlatformPaint(Paint paint);
        public void addListener(Paint paint, AbstractNotifyListener platformChangeListener);
        public void removeListener(Paint paint, AbstractNotifyListener platformChangeListener);
    }

    private static PaintAccessor paintAccessor = null;

    public static void setPaintAccessor(PaintAccessor accessor) {
        paintAccessor = accessor;
    }

    public static PaintAccessor getPaintAccessor() {
        return paintAccessor;
    }

    public interface ImageAccessor {
        public boolean isAnimation(Image image);
        public ReadOnlyObjectProperty<PlatformImage>getImageProperty(Image image);
        public int[] getPreColors(PixelFormat<ByteBuffer> pf);
        public int[] getNonPreColors(PixelFormat<ByteBuffer> pf);
        public Object getPlatformImage(Image image);
        public Image fromPlatformImage(Object image);
    }

    private static ImageAccessor imageAccessor;

    static {
        // Need to ensure that the Image class is loaded since Toolkit class
        // is the provider of getImageAccessor method and sets the accessor.
        Utils.forceInit(Image.class);
    }

    public static void setImageAccessor(ImageAccessor accessor) {
        imageAccessor = accessor;
    }

    public static ImageAccessor getImageAccessor() {
        return imageAccessor;
    }

    public String getThemeName() {
        return null;
    }

    public abstract GlassRobot createRobot();
}
