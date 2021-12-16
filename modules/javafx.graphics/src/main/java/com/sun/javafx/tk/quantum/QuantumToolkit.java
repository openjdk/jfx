/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import javafx.application.ConditionalFeature;
import javafx.geometry.Dimension2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import java.io.File;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.CommonDialogs;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.EventLoop;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.View;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.runtime.async.AbstractRemoteResource;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.CompletionListener;
import com.sun.javafx.tk.FileChooserType;
import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.ImageLoader;
import com.sun.javafx.tk.PlatformImage;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.ScreenConfigurationAccessor;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.TKDragGestureListener;
import com.sun.javafx.tk.TKDragSourceListener;
import com.sun.javafx.tk.TKDropTargetListener;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKScreenConfigurationListener;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKSystemMenu;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.ResourceFactoryListener;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.PrismSettings;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractPrimaryTimer;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.impl.prism.PrFilterContext;
import com.sun.scenario.effect.impl.prism.PrImage;
import com.sun.javafx.logging.PulseLogger;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import com.sun.javafx.scene.input.DragboardHelper;
import java.util.Optional;

public final class QuantumToolkit extends Toolkit {

    @SuppressWarnings("removal")
    public static final boolean verbose =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("quantum.verbose"));

    @SuppressWarnings("removal")
    public static final boolean pulseDebug =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("quantum.pulse"));

    @SuppressWarnings("removal")
    private static final boolean multithreaded =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                // If it is not specified, or it is true, then it should
                // be true. Otherwise it should be false.
                String value = System.getProperty("quantum.multithreaded");
                if (value == null) return true;
                final boolean result = Boolean.parseBoolean(value);
                if (verbose) {
                    System.out.println(result ? "Multi-Threading Enabled" : "Multi-Threading Disabled");
                }
                return result;
            });

    @SuppressWarnings("removal")
    private static boolean debug =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("quantum.debug"));

    @SuppressWarnings("removal")
    private static Integer pulseHZ =
            AccessController.doPrivileged((PrivilegedAction<Integer>) () -> Integer.getInteger("javafx.animation.pulse"));

    @SuppressWarnings("removal")
    static final boolean liveResize =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                boolean isSWT = "swt".equals(System.getProperty("glass.platform"));
                String result = (PlatformUtil.isMac() || PlatformUtil.isWindows()) && !isSWT ? "true" : "false";
                return "true".equals(System.getProperty("javafx.live.resize", result));
            });

    @SuppressWarnings("removal")
    static final boolean drawInPaint =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                boolean isSWT = "swt".equals(System.getProperty("glass.platform"));
                String result = PlatformUtil.isMac() && isSWT ? "true" : "false";
                return "true".equals(System.getProperty("javafx.draw.in.paint", result));});

    @SuppressWarnings("removal")
    private static boolean singleThreaded =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                Boolean result = Boolean.getBoolean("quantum.singlethreaded");
                if (/*verbose &&*/ result) {
                    System.out.println("Warning: Single GUI Threadiong is enabled, FPS should be slower");
                }
                return result;
            });

    @SuppressWarnings("removal")
    private static boolean noRenderJobs =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                Boolean result = Boolean.getBoolean("quantum.norenderjobs");
                if (/*verbose &&*/ result) {
                    System.out.println("Warning: Quantum will not submit render jobs, nothing should draw");
                }
                return result;
            });

    private class PulseTask {
        private volatile boolean isRunning;
        PulseTask(boolean state) {
            isRunning = state;
        }

        synchronized void set(boolean state) {
            isRunning = state;
            if (isRunning) {
                resumeTimer();
            }
        }

        boolean get() {
            return isRunning;
        }
    }

    private AtomicBoolean           toolkitRunning = new AtomicBoolean(false);
    private PulseTask               animationRunning = new PulseTask(false);
    private PulseTask               nextPulseRequested = new PulseTask(false);
    private AtomicBoolean           pulseRunning = new AtomicBoolean(false);
    private int                     inPulse = 0;
    private CountDownLatch          launchLatch = new CountDownLatch(1);

    final int                       PULSE_INTERVAL = (int)(TimeUnit.SECONDS.toMillis(1L) / getRefreshRate());
    final int                       FULLSPEED_INTERVAL = 1;     // ms
    boolean                         nativeSystemVsync = false;
    private long                    firstPauseRequestTime = 0;
    private boolean                 pauseRequested = false;
    private static final long       PAUSE_THRESHOLD_DURATION = 250;
    private float                   _maxPixelScale;
    private Runnable                pulseRunnable, userRunnable, timerRunnable;
    private Timer                   pulseTimer = null;
    private Thread                  shutdownHook = null;
    private PaintCollector          collector;
    private QuantumRenderer         renderer;
    private GraphicsPipeline        pipeline;

    private ClassLoader             ccl;

    private HashMap<Object,EventLoop> eventLoopMap = null;

    private final PerformanceTracker perfTracker = new PerformanceTrackerImpl();

    @Override public boolean init() {
        /*
         * Glass Mac, X11 need Application.setDeviceDetails to happen prior to Glass Application.Run
         */
        renderer = QuantumRenderer.getInstance();
        collector = PaintCollector.createInstance(this);
        pipeline = GraphicsPipeline.getPipeline();

        /* shutdown the pipeline on System.exit, ^c
         * needed with X11 and Windows, see RT-32501
         */
        shutdownHook = new Thread("Glass/Prism Shutdown Hook") {
            @Override public void run() {
                dispose();
            }
        };
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            return null;
        });
        return true;
    }

    /**
     * This method is invoked by PlatformImpl. It is typically called on the main
     * thread, NOT the JavaFX Application Thread. The userStartupRunnable will
     * be invoked on the JavaFX Application Thread.
     *
     * @param userStartupRunnable A runnable invoked on the JavaFX Application Thread
     *                            that allows the system to perform some startup
     *                            functionality after the toolkit has been initialized.
     */
    @Override public void startup(final Runnable userStartupRunnable) {
        // Save the context class loader of the launcher thread
        ccl = Thread.currentThread().getContextClassLoader();

        try {
            this.userRunnable = userStartupRunnable;

            // Ensure that the toolkit can only be started here
            Application.run(() -> runToolkit());
        } catch (RuntimeException ex) {
            if (verbose) {
                ex.printStackTrace();
            }
            throw ex;
        } catch (Throwable t) {
            if (verbose) {
                t.printStackTrace();
            }
            throw new RuntimeException(t);
        }

        try {
            launchLatch.await();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    // restart the toolkit if previously terminated
    private void assertToolkitRunning() {
        // not implemented
    }

    boolean shouldWaitForRenderingToComplete() {
        return !multithreaded;
    }

    /**
     * Method to initialize the Scene Graph on the JavaFX application thread.
     * Specifically, we will do static initialization for those classes in
     * the javafx.stage, javafx.scene, and javafx.controls packages necessary
     * to allow subsequent construction of the Scene or any Node, including
     * a PopupControl, on a background thread.
     *
     * This method is called on the JavaFX application thread.
     */
    private static void initSceneGraph() {
        // It is both necessary and sufficient to call a static method on the
        // Screen class to allow PopupControl instances to be created on any thread.
        javafx.stage.Screen.getPrimary();
    }

    // Called by Glass from Application.run()
    void runToolkit() {
        Thread user = Thread.currentThread();

        if (!toolkitRunning.getAndSet(true)) {
            user.setName("JavaFX Application Thread");
            // Set context class loader to the same as the thread that called startup
            user.setContextClassLoader(ccl);
            setFxUserThread(user);

            // Glass screens were inited in Application.run(), assign adapters
            assignScreensAdapters();
            /*
             *  Glass Application instance is now valid - create the ResourceFactory
             *  on the render thread
             */
            renderer.createResourceFactory();

            pulseRunnable = () -> QuantumToolkit.this.pulseFromQueue();
            timerRunnable = () -> {
                try {
                    QuantumToolkit.this.postPulse();
                } catch (Throwable th) {
                    th.printStackTrace(System.err);
                }
            };
            pulseTimer = Application.GetApplication().createTimer(timerRunnable);

            Application.GetApplication().setEventHandler(new Application.EventHandler() {
                @Override public void handleQuitAction(Application app, long time) {
                    GlassStage.requestClosingAllWindows();
                }

                @Override public boolean handleThemeChanged(String themeName) {
                    String highContrastSchemeName = Application.GetApplication().getHighContrastScheme(themeName);
                    return PlatformImpl.setAccessibilityTheme(highContrastSchemeName);
                }
            });
        }
        // Initialize JavaFX scene graph
        initSceneGraph();
        launchLatch.countDown();
        try {
            Application.invokeAndWait(this.userRunnable);
            this.userRunnable = null;

            if (getPrimaryTimer().isFullspeed()) {
                /*
                 * FULLSPEED_INTVERVAL workaround
                 *
                 * Application.invokeLater(pulseRunnable);
                 */
                pulseTimer.start(FULLSPEED_INTERVAL);
            } else {
                nativeSystemVsync = Screen.getVideoRefreshPeriod() != 0.0;
                if (nativeSystemVsync) {
                    // system supports vsync
                    pulseTimer.start();
                } else {
                    // rely on millisecond resolution timer to provide
                    // nominal pulse sync and use pulse hinting on
                    // synchronous pipelines to fine tune the interval
                    pulseTimer.start(PULSE_INTERVAL);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            if (PrismSettings.verbose) {
                System.err.println(" vsync: " + PrismSettings.isVsyncEnabled +
                                   " vpipe: " + pipeline.isVsyncSupported());
            }
            PerformanceTracker.logEvent("Toolkit.startup - finished");
        }
    }

    /**
     * Runs the specified supplier, releasing the renderLock if needed.
     * This is called by glass event handlers for Window, View, and
     * Accessible.
     * @param <T> the type of the return value
     * @param supplier the supplier to be run
     * @return the return value from calling supplier.get()
     */
    public static <T> T runWithoutRenderLock(Supplier<T> supplier) {
        final boolean locked = ViewPainter.renderLock.isHeldByCurrentThread();
        try {
            if (locked) {
                ViewPainter.renderLock.unlock();
            }
            return supplier.get();
        } finally {
            if (locked) {
                ViewPainter.renderLock.lock();
            }
        }
    }

    /**
     * Runs the specified supplier, first acquiring the renderLock.
     * The lock is released when done.
     * @param <T> the type of the return value
     * @param supplier the supplier to be run
     * @return the return value from calling supplier.get()
     */
    public static <T> T runWithRenderLock(Supplier<T> supplier) {
        ViewPainter.renderLock.lock();
        try {
            return supplier.get();
        } finally {
            ViewPainter.renderLock.unlock();
        }
    }

    boolean hasNativeSystemVsync() {
        return nativeSystemVsync;
    }

    boolean isVsyncEnabled() {
        return (PrismSettings.isVsyncEnabled &&
                pipeline.isVsyncSupported());
    }

    @Override public void checkFxUserThread() {
        super.checkFxUserThread();
        renderer.checkRendererIdle();
    }

    protected static Thread getFxUserThread() {
        return Toolkit.getFxUserThread();
    }

    @Override public Future addRenderJob(RenderJob r) {
        // Do not run any render jobs (this is for benchmarking only)
        if (noRenderJobs) {
            CompletionListener listener = r.getCompletionListener();
            if (r instanceof PaintRenderJob) {
                ((PaintRenderJob)r).getScene().setPainting(false);
            }
            if (listener != null) {
                try {
                    listener.done(r);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
            return null;
        }
        // Run the render job in the UI thread (this is for benchmarking only)
        if (singleThreaded) {
            r.run();
            return null;
        }
        return (renderer.submitRenderJob(r));
    }

    void postPulse() {
        if (toolkitRunning.get() &&
            (animationRunning.get() || nextPulseRequested.get()) &&
            !setPulseRunning()) {

            Application.invokeLater(pulseRunnable);

            if (debug) {
                System.err.println("QT.postPulse@(" + System.nanoTime() + "): " + pulseString());
            }
        } else if (!animationRunning.get() && !nextPulseRequested.get() && !pulseRunning.get()) {
            pauseTimer();
        } else if (debug) {
            System.err.println("QT.postPulse#(" + System.nanoTime() + "): DROP : " + pulseString());
        }
    }

    private synchronized void pauseTimer() {
        if (!pauseRequested) {
            pauseRequested = true;
            firstPauseRequestTime = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() - firstPauseRequestTime >= PAUSE_THRESHOLD_DURATION) {
            pulseTimer.pause();
            if (debug) {
                System.err.println("QT.pauseTimer#(" + System.nanoTime() + "): Pausing Timer : " + pulseString());
            }
        } else if (debug) {
            System.err.println("QT.pauseTimer#(" + System.nanoTime() + "): Pause Timer : DROP : " + pulseString());
        }
    }

    private synchronized void resumeTimer() {
        pauseRequested = false;
        pulseTimer.resume();
    }

    private String pulseString() {
        return ((toolkitRunning.get() ? "T" : "t") +
                (animationRunning.get() ? "A" : "a") +
                (pulseRunning.get() ? "P" : "p") +
                (nextPulseRequested.get() ? "N" : "n"));
    }

    private boolean setPulseRunning() {
        return (pulseRunning.getAndSet(true));
    }

    private void endPulseRunning() {
        pulseRunning.set(false);
        if (debug) {
            System.err.println("QT.endPulse: " + System.nanoTime());
        }
    }

    void pulseFromQueue() {
        try {
            pulse();
        } finally {
            endPulseRunning();
        }
    }

    protected void pulse() {
        pulse(true);
    }

    void pulse(boolean collect) {
        try {
            inPulse++;
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.pulseStart();
            }

            if (!toolkitRunning.get()) {
                return;
            }
            nextPulseRequested.set(false);
            if (animationRunnable != null) {
                animationRunning.set(true);
                animationRunnable.run();
            } else {
                animationRunning.set(false);
            }
            firePulse();
            if (collect) collector.renderAll();
        } finally {
            inPulse--;
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.pulseEnd();
            }
        }
    }

    void vsyncHint() {
        if (isVsyncEnabled()) {
            if (debug) {
                System.err.println("QT.vsyncHint: postPulse: " + System.nanoTime());
            }
            postPulse();
        }
    }

    @Override public TKStage createTKStage(Window peerWindow, boolean securityDialog, StageStyle stageStyle, boolean primary, Modality modality, TKStage owner, boolean rtl, @SuppressWarnings("removal") AccessControlContext acc) {
        assertToolkitRunning();
        WindowStage stage = new WindowStage(peerWindow, securityDialog, stageStyle, modality, owner);
        stage.setSecurityContext(acc);
        if (primary) {
            stage.setIsPrimary();
        }
        stage.setRTL(rtl);
        stage.init(systemMenu);
        return stage;
    }

    @Override public boolean canStartNestedEventLoop() {
        return inPulse == 0;
    }

    @Override public Object enterNestedEventLoop(Object key) {
        checkFxUserThread();

        if (key == null) {
            throw new NullPointerException();
        }

        if (!canStartNestedEventLoop()) {
            throw new IllegalStateException("Cannot enter nested loop during animation or layout processing");
        }

        if (eventLoopMap == null) {
            eventLoopMap = new HashMap<>();
        }
        if (eventLoopMap.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Key already associated with a running event loop: " + key);
        }
        EventLoop eventLoop = Application.GetApplication().createEventLoop();
        eventLoopMap.put(key, eventLoop);

        Object ret = eventLoop.enter();

        if (!isNestedLoopRunning()) {
            notifyLastNestedLoopExited();
        }

        return ret;
    }

    @Override public void exitNestedEventLoop(Object key, Object rval) {
        checkFxUserThread();

        if (key == null) {
            throw new NullPointerException();
        }
        if (eventLoopMap == null || !eventLoopMap.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Key not associated with a running event loop: " + key);
        }
        EventLoop eventLoop = eventLoopMap.get(key);
        eventLoopMap.remove(key);
        eventLoop.leave(rval);
    }

    @Override public void exitAllNestedEventLoops() {
        checkFxUserThread();
        for (EventLoop eventLoop : eventLoopMap.values()) {
            eventLoop.leave(null);
        }
        eventLoopMap.clear();
        eventLoopMap = null;
    }

    @Override public TKStage createTKPopupStage(Window peerWindow,
                                                StageStyle popupStyle,
                                                TKStage owner,
                                                @SuppressWarnings("removal") AccessControlContext acc) {
        assertToolkitRunning();
        boolean securityDialog = owner instanceof WindowStage ?
                ((WindowStage)owner).isSecurityDialog() : false;
        WindowStage stage = new WindowStage(peerWindow, securityDialog, popupStyle, null, owner);
        stage.setSecurityContext(acc);
        stage.setIsPopup();
        stage.init(systemMenu);
        return stage;
    }

    @Override public TKStage createTKEmbeddedStage(HostInterface host, @SuppressWarnings("removal") AccessControlContext acc) {
        assertToolkitRunning();
        EmbeddedStage stage = new EmbeddedStage(host);
        stage.setSecurityContext(acc);
        return stage;
    }

    private static ScreenConfigurationAccessor screenAccessor =
        new ScreenConfigurationAccessor() {
            @Override public int getMinX(Object obj) {
               return ((Screen)obj).getX();
            }
            @Override public int getMinY(Object obj) {
                return ((Screen)obj).getY();
            }
            @Override public int getWidth(Object obj) {
                return ((Screen)obj).getWidth();
            }
            @Override public int getHeight(Object obj) {
                return ((Screen)obj).getHeight();
            }
            @Override public int getVisualMinX(Object obj) {
                return ((Screen)obj).getVisibleX();
            }
            @Override public int getVisualMinY(Object obj) {
                return ((Screen)obj).getVisibleY();
            }
            @Override public int getVisualWidth(Object obj) {
                return ((Screen)obj).getVisibleWidth();
            }
            @Override public int getVisualHeight(Object obj) {
                return ((Screen)obj).getVisibleHeight();
            }
            @Override public float getDPI(Object obj) {
                return ((Screen)obj).getResolutionX();
            }
            @Override public float getRecommendedOutputScaleX(Object obj) {
                return ((Screen)obj).getRecommendedOutputScaleX();
            }
            @Override public float getRecommendedOutputScaleY(Object obj) {
                return ((Screen)obj).getRecommendedOutputScaleY();
            }
        };

    @Override public ScreenConfigurationAccessor
                    setScreenConfigurationListener(final TKScreenConfigurationListener listener) {
        Screen.setEventHandler(new Screen.EventHandler() {
            @Override public void handleSettingsChanged() {
                notifyScreenListener(listener);
            }
        });
        return screenAccessor;
    }

    private static void assignScreensAdapters() {
        GraphicsPipeline pipeline = GraphicsPipeline.getPipeline();
        for (Screen screen : Screen.getScreens()) {
            screen.setAdapterOrdinal(pipeline.getAdapterOrdinal(screen));
        }
    }

    private static void notifyScreenListener(TKScreenConfigurationListener listener) {
        assignScreensAdapters();
        listener.screenConfigurationChanged();
    }

    @Override public Object getPrimaryScreen() {
        return Screen.getMainScreen();
    }

    @Override public List<?> getScreens() {
        return Screen.getScreens();
    }

    @Override
    public ScreenConfigurationAccessor getScreenConfigurationAccessor() {
        return screenAccessor;
    }

    @Override
    public PerformanceTracker getPerformanceTracker() {
        return perfTracker;
    }

    @Override
    public PerformanceTracker createPerformanceTracker() {
        return new PerformanceTrackerImpl();
    }

    // Only currently called from the loadImage method below.  We do not
    // necessarily know what the worst render scale we will ever see is
    // because the user has control over that, but we should be loading
    // all dpi variants of an image at all times anyway and then using
    // whichever one is needed to respond to a given rendering request
    // rather than predetermining which one to use up front.  If we switch
    // to making that decision at render time then this method can go away.
    private float getMaxRenderScale() {
        if (_maxPixelScale == 0) {
            for (Object o : getScreens()) {
                _maxPixelScale = Math.max(_maxPixelScale, ((Screen) o).getRecommendedOutputScaleX());
                _maxPixelScale = Math.max(_maxPixelScale, ((Screen) o).getRecommendedOutputScaleY());
            }
        }
        return _maxPixelScale;
    }

    @Override public ImageLoader loadImage(String url, double width, double height, boolean preserveRatio, boolean smooth) {
        return new PrismImageLoader2(url, width, height, preserveRatio, getMaxRenderScale(), smooth);
    }

    @Override public ImageLoader loadImage(InputStream stream, double width, double height,
                                           boolean preserveRatio, boolean smooth) {
        return new PrismImageLoader2(stream, width, height, preserveRatio, smooth);
    }

    @Override public AbstractRemoteResource<? extends ImageLoader> loadImageAsync(
            AsyncOperationListener listener, String url,
            double width, double height, boolean preserveRatio, boolean smooth) {
        return new PrismImageLoader2.AsyncImageLoader(listener, url, width, height, preserveRatio, smooth);
    }

    // Note that this method should only be called by PlatformImpl.runLater
    // It should not be called directly by other FX code since the underlying
    // glass invokeLater method is not thread-safe with respect to toolkit
    // shutdown. Calling Platform.runLater *is* thread-safe even when the
    // toolkit is shutting down.
    @Override public void defer(Runnable runnable) {
        if (!toolkitRunning.get()) return;

        Application.invokeLater(runnable);
    }

    @Override public void exit() {
        // This method must run on the FX application thread
        checkFxUserThread();

        // Turn off pulses so no extraneous runnables are submitted
        pulseTimer.stop();

        // We need to wait for the last frame to finish so that the renderer
        // is not running while we are shutting down glass.
        PaintCollector.getInstance().waitForRenderingToComplete();

        notifyShutdownHooks();

        runWithRenderLock(() -> {
            //TODO - should update glass scene view state
            //TODO - doesn't matter because we are exiting
            Application app = Application.GetApplication();
            app.terminate();
            return null;
        });

        dispose();

        super.exit();
    }

    @SuppressWarnings("removal")
    public void dispose() {
        if (toolkitRunning.compareAndSet(true, false)) {
            pulseTimer.stop();
            renderer.stopRenderer();

            try {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                    return null;
                });
            } catch (IllegalStateException ignore) {
                // throw when shutdown hook already removed
            }
        }
    }

    @Override public boolean isForwardTraversalKey(KeyEvent e) {
        return (e.getCode() == KeyCode.TAB)
                   && (e.getEventType() == KeyEvent.KEY_PRESSED)
                   && !e.isShiftDown();
    }

    @Override public boolean isBackwardTraversalKey(KeyEvent e) {
        return (e.getCode() == KeyCode.TAB)
                   && (e.getEventType() == KeyEvent.KEY_PRESSED)
                   && e.isShiftDown();
    }

    private Map<Object, Object> contextMap = Collections.synchronizedMap(new HashMap<>());
    @Override public Map<Object, Object> getContextMap() {
        return contextMap;
    }

    @Override public int getRefreshRate() {
        if (pulseHZ == null) {
            return 60;
        } else {
            return pulseHZ;
        }
    }

    private DelayedRunnable animationRunnable;
    @Override public void setAnimationRunnable(DelayedRunnable animationRunnable) {
        if (animationRunnable != null) {
            animationRunning.set(true);
        }
        this.animationRunnable = animationRunnable;
    }

    @Override public void requestNextPulse() {
        nextPulseRequested.set(true);
    }

    @Override public void waitFor(Task t) {
        if (t.isFinished()) {
            return;
        }
    }

    @Override protected Object createColorPaint(Color color) {
        return new com.sun.prism.paint.Color(
                (float)color.getRed(), (float)color.getGreen(),
                (float)color.getBlue(), (float)color.getOpacity());
    }

    private com.sun.prism.paint.Color toPrismColor(Color color) {
        return (com.sun.prism.paint.Color) Toolkit.getPaintAccessor().getPlatformPaint(color);
    }

    private List<com.sun.prism.paint.Stop> convertStops(List<Stop> paintStops) {
        List<com.sun.prism.paint.Stop> stops =
            new ArrayList<>(paintStops.size());
        for (Stop s : paintStops) {
            stops.add(new com.sun.prism.paint.Stop(toPrismColor(s.getColor()),
                                                   (float) s.getOffset()));
        }
        return stops;
    }

    @Override protected Object createLinearGradientPaint(LinearGradient paint) {
        int cmi = com.sun.prism.paint.Gradient.REPEAT;
        CycleMethod cycleMethod = paint.getCycleMethod();
        if (cycleMethod == CycleMethod.NO_CYCLE) {
            cmi = com.sun.prism.paint.Gradient.PAD;
        } else if (cycleMethod == CycleMethod.REFLECT) {
            cmi = com.sun.prism.paint.Gradient.REFLECT;
        }
        // TODO: extract colors/offsets and pass them in directly...
        List<com.sun.prism.paint.Stop> stops = convertStops(paint.getStops());
        return new com.sun.prism.paint.LinearGradient(
            (float)paint.getStartX(), (float)paint.getStartY(), (float)paint.getEndX(), (float)paint.getEndY(),
            null, paint.isProportional(), cmi, stops);
    }

    @Override
    protected Object createRadialGradientPaint(RadialGradient paint) {
        float cx = (float)paint.getCenterX();
        float cy = (float)paint.getCenterY();
        float fa = (float)paint.getFocusAngle();
        float fd = (float)paint.getFocusDistance();

        int cmi = 0;
        if (paint.getCycleMethod() == CycleMethod.NO_CYCLE) {
            cmi = com.sun.prism.paint.Gradient.PAD;
        } else if (paint.getCycleMethod() == CycleMethod.REFLECT) {
            cmi = com.sun.prism.paint.Gradient.REFLECT;
        } else {
            cmi = com.sun.prism.paint.Gradient.REPEAT;
        }

        // TODO: extract colors/offsets and pass them in directly...
        List<com.sun.prism.paint.Stop> stops = convertStops(paint.getStops());
        return new com.sun.prism.paint.RadialGradient(cx, cy, fa, fd,
                (float)paint.getRadius(), null, paint.isProportional(), cmi, stops);
    }

    @Override
    protected Object createImagePatternPaint(ImagePattern paint) {
        if (paint.getImage() == null) {
            return com.sun.prism.paint.Color.TRANSPARENT;
        } else {
            return new com.sun.prism.paint.ImagePattern(
                    (com.sun.prism.Image) Toolkit.getImageAccessor().getPlatformImage(paint.getImage()),
                    (float)paint.getX(),
                    (float)paint.getY(),
                    (float)paint.getWidth(),
                    (float)paint.getHeight(),
                    paint.isProportional(),
                    Toolkit.getPaintAccessor().isMutable(paint));
        }
    }

    static BasicStroke tmpStroke = new BasicStroke();
    private void initStroke(StrokeType pgtype, double strokewidth,
                            StrokeLineCap pgcap,
                            StrokeLineJoin pgjoin, float miterLimit,
                            float[] dashArray, float dashOffset)
    {
        int type;
        if (pgtype == StrokeType.CENTERED) {
            type = BasicStroke.TYPE_CENTERED;
        } else if (pgtype == StrokeType.INSIDE) {
            type = BasicStroke.TYPE_INNER;
        } else {
            type = BasicStroke.TYPE_OUTER;
        }

        int cap;
        if (pgcap == StrokeLineCap.BUTT) {
            cap = BasicStroke.CAP_BUTT;
        } else if (pgcap == StrokeLineCap.SQUARE) {
            cap = BasicStroke.CAP_SQUARE;
        } else {
            cap = BasicStroke.CAP_ROUND;
        }

        int join;
        if (pgjoin == StrokeLineJoin.BEVEL) {
            join = BasicStroke.JOIN_BEVEL;
        } else if (pgjoin == StrokeLineJoin.MITER) {
            join = BasicStroke.JOIN_MITER;
        } else {
            join = BasicStroke.JOIN_ROUND;
        }

        tmpStroke.set(type, (float) strokewidth, cap, join, miterLimit);
        if ((dashArray != null) && (dashArray.length > 0)) {
            tmpStroke.set(dashArray, dashOffset);
        } else {
            tmpStroke.set((float[])null, 0);
        }
    }

    @Override
    public void accumulateStrokeBounds(Shape shape, float bbox[],
                                       StrokeType pgtype,
                                       double strokewidth,
                                       StrokeLineCap pgcap,
                                       StrokeLineJoin pgjoin,
                                       float miterLimit,
                                       BaseTransform tx)
    {

        initStroke(pgtype, strokewidth, pgcap, pgjoin, miterLimit, null, 0);
        if (tx.isTranslateOrIdentity()) {
            tmpStroke.accumulateShapeBounds(bbox, shape, tx);
        } else {
            Shape.accumulate(bbox, tmpStroke.createStrokedShape(shape), tx);
        }
    }

    @Override
    public boolean strokeContains(Shape shape, double x, double y,
                                  StrokeType pgtype,
                                  double strokewidth,
                                  StrokeLineCap pgcap,
                                  StrokeLineJoin pgjoin,
                                  float miterLimit)
    {
        initStroke(pgtype, strokewidth, pgcap, pgjoin, miterLimit, null, 0);
        // TODO: The contains testing could be done directly without creating a Shape
        return tmpStroke.createStrokedShape(shape).contains((float) x, (float) y);
    }

    @Override
    public Shape createStrokedShape(Shape shape,
                                    StrokeType pgtype,
                                    double strokewidth,
                                    StrokeLineCap pgcap,
                                    StrokeLineJoin pgjoin,
                                    float miterLimit,
                                    float[] dashArray,
                                    float dashOffset) {
        initStroke(pgtype, strokewidth, pgcap, pgjoin, miterLimit,
                   dashArray, dashOffset);
        return tmpStroke.createStrokedShape(shape);
    }

    @Override public Dimension2D getBestCursorSize(int preferredWidth, int preferredHeight) {
        return CursorUtils.getBestCursorSize(preferredWidth, preferredHeight);
    }

    @Override public int getMaximumCursorColors() {
        return 2;
    }

    @Override public int getKeyCodeForChar(String character) {
        return (character.length() == 1)
                ? com.sun.glass.events.KeyEvent.getKeyCodeForChar(
                          character.charAt(0))
                : com.sun.glass.events.KeyEvent.VK_UNDEFINED;
    }

    @Override public PathElement[] convertShapeToFXPath(Object shape) {
        if (shape == null) {
            return new PathElement[0];
        }
        List<PathElement> elements = new ArrayList<>();
        // iterate over the shape and turn it into a series of path
        // elements
        com.sun.javafx.geom.Shape geomShape = (com.sun.javafx.geom.Shape) shape;
        PathIterator itr = geomShape.getPathIterator(null);
        PathIteratorHelper helper = new PathIteratorHelper(itr);
        PathIteratorHelper.Struct struct = new PathIteratorHelper.Struct();

        while (!helper.isDone()) {
            // true if WIND_EVEN_ODD, false if WIND_NON_ZERO
            boolean windEvenOdd = helper.getWindingRule() == PathIterator.WIND_EVEN_ODD;
            int type = helper.currentSegment(struct);
            PathElement el;
            if (type == PathIterator.SEG_MOVETO) {
                el = new MoveTo(struct.f0, struct.f1);
            } else if (type == PathIterator.SEG_LINETO) {
                el = new LineTo(struct.f0, struct.f1);
            } else if (type == PathIterator.SEG_QUADTO) {
                el = new QuadCurveTo(
                    struct.f0,
                    struct.f1,
                    struct.f2,
                    struct.f3);
            } else if (type == PathIterator.SEG_CUBICTO) {
                el = new CubicCurveTo (
                    struct.f0,
                    struct.f1,
                    struct.f2,
                    struct.f3,
                    struct.f4,
                    struct.f5);
            } else if (type == PathIterator.SEG_CLOSE) {
                el = new ClosePath();
            } else {
                throw new IllegalStateException("Invalid element type: " + type);
            }
            helper.next();
            elements.add(el);
        }

        return elements.toArray(new PathElement[elements.size()]);
    }

    @Override public Filterable toFilterable(Image img) {
        return PrImage.create((com.sun.prism.Image) Toolkit.getImageAccessor().getPlatformImage(img));
    }

    @Override public FilterContext getFilterContext(Object config) {
        if (config == null || (!(config instanceof com.sun.glass.ui.Screen))) {
            return PrFilterContext.getDefaultInstance();
        }
        Screen screen = (Screen)config;
        return PrFilterContext.getInstance(screen);
    }

    @Override public AbstractPrimaryTimer getPrimaryTimer() {
        return PrimaryTimer.getInstance();
    }

    @Override public FontLoader getFontLoader() {
        return com.sun.javafx.font.PrismFontLoader.getInstance();
    }

    @Override public TextLayoutFactory getTextLayoutFactory() {
        return com.sun.javafx.text.PrismTextLayoutFactory.getFactory();
    }

    @Override public Object createSVGPathObject(SVGPath svgpath) {
        int windingRule = svgpath.getFillRule() == FillRule.NON_ZERO ? PathIterator.WIND_NON_ZERO : PathIterator.WIND_EVEN_ODD;
        Path2D path = new Path2D(windingRule);
        path.appendSVGPath(svgpath.getContent());
        return path;
    }

    @Override public Path2D createSVGPath2D(SVGPath svgpath) {
        int windingRule = svgpath.getFillRule() == FillRule.NON_ZERO ? PathIterator.WIND_NON_ZERO : PathIterator.WIND_EVEN_ODD;
        Path2D path = new Path2D(windingRule);
        path.appendSVGPath(svgpath.getContent());
        return path;
    }

    @Override public boolean imageContains(Object image, float x, float y) {
        if (image == null) {
            return false;
        }

        com.sun.prism.Image pImage = (com.sun.prism.Image)image;
        int intX = (int)x + pImage.getMinX();
        int intY = (int)y + pImage.getMinY();

        if (pImage.isOpaque()) {
            return true;
        }

        if (pImage.getPixelFormat() == PixelFormat.INT_ARGB_PRE) {
            IntBuffer ib = (IntBuffer) pImage.getPixelBuffer();
            int index = intX + intY * pImage.getRowLength();
            if (index >= ib.limit()) {
                return false;
            } else {
                return (ib.get(index) & 0xff000000) != 0;
            }
        } else if (pImage.getPixelFormat() == PixelFormat.BYTE_BGRA_PRE) {
            ByteBuffer bb = (ByteBuffer) pImage.getPixelBuffer();
            int index = intX * pImage.getBytesPerPixelUnit() + intY * pImage.getScanlineStride() + 3;
            if (index >= bb.limit()) {
                return false;
            } else {
                return (bb.get(index) & 0xff) != 0;
            }
        } else if (pImage.getPixelFormat() == PixelFormat.BYTE_ALPHA) {
            ByteBuffer bb = (ByteBuffer) pImage.getPixelBuffer();
            int index = intX * pImage.getBytesPerPixelUnit() + intY * pImage.getScanlineStride();
            if (index >= bb.limit()) {
                return false;
            } else {
                return (bb.get(index) & 0xff) != 0;
            }
        }
        return true;
    }

    @Override
    public boolean isNestedLoopRunning() {
        return Application.isNestedLoopRunning();
    }

    @Override
    public boolean isSupported(ConditionalFeature feature) {
        switch (feature) {
            case SCENE3D:
                return GraphicsPipeline.getPipeline().is3DSupported();
            case EFFECT:
                return GraphicsPipeline.getPipeline().isEffectSupported();
            case SHAPE_CLIP:
                return true;
            case INPUT_METHOD:
                return Application.GetApplication().supportsInputMethods();
            case TRANSPARENT_WINDOW:
                return Application.GetApplication().supportsTransparentWindows();
            case UNIFIED_WINDOW:
                return Application.GetApplication().supportsUnifiedWindows();
            case TWO_LEVEL_FOCUS:
                return Application.GetApplication().hasTwoLevelFocus();
            case VIRTUAL_KEYBOARD:
                return Application.GetApplication().hasVirtualKeyboard();
            case INPUT_TOUCH:
                return Application.GetApplication().hasTouch();
            case INPUT_MULTITOUCH:
                return Application.GetApplication().hasMultiTouch();
            case INPUT_POINTER:
                return Application.GetApplication().hasPointer();
            default:
                return false;
        }
    }

    @Override
    public boolean isMSAASupported() {
        return  GraphicsPipeline.getPipeline().isMSAASupported();
    }

    // Returns the glass keycode for the given JavaFX KeyCode.
    // This method only converts lock state KeyCode values
    private int toGlassKeyCode(KeyCode keyCode) {
        switch (keyCode) {
            case CAPS:
                return com.sun.glass.events.KeyEvent.VK_CAPS_LOCK;
            case NUM_LOCK:
                return com.sun.glass.events.KeyEvent.VK_NUM_LOCK;
            default:
                return com.sun.glass.events.KeyEvent.VK_UNDEFINED;
        }
    }

    @Override
    public Optional<Boolean> isKeyLocked(KeyCode keyCode) {
        return Application.GetApplication().isKeyLocked(toGlassKeyCode(keyCode));
    }

    static TransferMode clipboardActionToTransferMode(final int action) {
        switch (action) {
            case Clipboard.ACTION_NONE:
                return null;
            case Clipboard.ACTION_COPY:
            //IE drop action for URL copy
            case Clipboard.ACTION_COPY | Clipboard.ACTION_REFERENCE:
                return TransferMode.COPY;
            case Clipboard.ACTION_MOVE:
            //IE drop action for URL move
            case Clipboard.ACTION_MOVE | Clipboard.ACTION_REFERENCE:
                return TransferMode.MOVE;
            case Clipboard.ACTION_REFERENCE:
                return TransferMode.LINK;
            case Clipboard.ACTION_ANY:
                return TransferMode.COPY; // select a reasonable trasnfer mode as workaround until RT-22840
        }
        return null;
    }

    private QuantumClipboard clipboard;
    @Override public TKClipboard getSystemClipboard() {
        if (clipboard == null) {
            clipboard = QuantumClipboard.getClipboardInstance(new ClipboardAssistance(com.sun.glass.ui.Clipboard.SYSTEM));
        }
        return clipboard;
    }

    private GlassSystemMenu systemMenu = new GlassSystemMenu();
    @Override public TKSystemMenu getSystemMenu() {
        return systemMenu;
    }

    @Override public TKClipboard getNamedClipboard(String name) {
        return null;
    }

    @Override public void startDrag(TKScene scene, Set<TransferMode> tm, TKDragSourceListener l, Dragboard dragboard) {
        if (dragboard == null) {
            throw new IllegalArgumentException("dragboard should not be null");
        }

        GlassScene view = (GlassScene)scene;
        view.setTKDragSourceListener(l);

        QuantumClipboard gc = (QuantumClipboard) DragboardHelper.getPeer(dragboard);
        gc.setSupportedTransferMode(tm);
        gc.flush();

        // flush causes a modal DnD event loop, when we return, close the clipboard
        gc.close();
    }

    @Override public void enableDrop(TKScene s, TKDropTargetListener l) {

        assert s instanceof GlassScene;

        GlassScene view = (GlassScene)s;
        view.setTKDropTargetListener(l);
    }

    @Override public void registerDragGestureListener(TKScene s, Set<TransferMode> tm, TKDragGestureListener l) {

        assert s instanceof GlassScene;

        GlassScene view = (GlassScene)s;
        view.setTKDragGestureListener(l);
    }

    @Override
    public void installInputMethodRequests(TKScene scene, InputMethodRequests requests) {

        assert scene instanceof GlassScene;

        GlassScene view = (GlassScene)scene;
        view.setInputMethodRequests(requests);
    }

    static class QuantumImage implements com.sun.javafx.tk.ImageLoader, ResourceFactoryListener {

        // cache rt here
        private com.sun.prism.RTTexture rt;
        private com.sun.prism.Image image;
        private ResourceFactory rf;

        QuantumImage(com.sun.prism.Image image) {
            this.image = image;
        }

        QuantumImage(PixelBuffer<Buffer> pixelBuffer) {
            switch (pixelBuffer.getPixelFormat().getType()) {
                case INT_ARGB_PRE:
                    image = com.sun.prism.Image.fromPixelBufferPreData(PixelFormat.INT_ARGB_PRE,
                            pixelBuffer.getBuffer(), pixelBuffer.getWidth(), pixelBuffer.getHeight());
                    break;

                case BYTE_BGRA_PRE:
                    image = com.sun.prism.Image.fromPixelBufferPreData(PixelFormat.BYTE_BGRA_PRE,
                            pixelBuffer.getBuffer(), pixelBuffer.getWidth(), pixelBuffer.getHeight());
                    break;

                default:
                    throw new InternalError("Unsupported PixelFormat: " + pixelBuffer.getPixelFormat().getType());
            }
        }

        RTTexture getRT(int w, int h, ResourceFactory rfNew) {
            boolean rttOk = rt != null && rf == rfNew &&
                    rt.getContentWidth() == w && rt.getContentHeight() == h;
            if (rttOk) {
                rt.lock();
                if (rt.isSurfaceLost()) {
                    rttOk = false;
                }
            }

            if (!rttOk) {
                if (rt != null) {
                    rt.dispose();
                }
                if (rf != null) {
                    rf.removeFactoryListener(this);
                    rf = null;
                }
                rt = rfNew.createRTTexture(w, h, WrapMode.CLAMP_TO_ZERO);
                if (rt != null) {
                    rf = rfNew;
                    rf.addFactoryListener(this);
                }
            }

            return rt;
        }

        void dispose() {
            if (rt != null) {
                rt.dispose();
                rt = null;
            }
        }

        void setImage(com.sun.prism.Image img) {
            image = img;
        }

        @Override
        public Exception getException() {
            return (image == null)
                    ? new IllegalStateException("Unitialized image")
                    : null;
        }
        @Override
        public int getFrameCount() { return 1; }
        @Override
        public PlatformImage getFrame(int index) { return image; }
        @Override
        public int getFrameDelay(int index) { return 0; }
        @Override
        public int getLoopCount() { return 0; }
        @Override
        public double getWidth() { return image.getWidth(); }
        @Override
        public double getHeight() { return image.getHeight(); }
        @Override
        public void factoryReset() { dispose(); }

        @Override
        public void factoryReleased() {
            dispose();

            // ResourceFactory is being disposed; clear reference to avoid leak
            if (rf != null) {
                rf.removeFactoryListener(this);
                rf = null;
            }
        }
    }

    @Override public ImageLoader loadPlatformImage(Object platformImage) {
        if (platformImage instanceof QuantumImage) {
            return (QuantumImage)platformImage;
        }

        if (platformImage instanceof com.sun.prism.Image) {
            return new QuantumImage((com.sun.prism.Image) platformImage);
        }

        if (platformImage instanceof PixelBuffer) {
            return new QuantumImage((PixelBuffer<Buffer>) platformImage);
        }

        throw new UnsupportedOperationException("unsupported class for loadPlatformImage");
    }

    @Override
    public PlatformImage createPlatformImage(int w, int h) {
        ByteBuffer bytebuf = ByteBuffer.allocate(w * h * 4);
        return com.sun.prism.Image.fromByteBgraPreData(bytebuf, w, h);
    }

    @Override
    public Object renderToImage(ImageRenderingContext p) {
        Object saveImage = p.platformImage;
        final ImageRenderingContext params = p;
        final com.sun.prism.paint.Paint currentPaint = p.platformPaint instanceof com.sun.prism.paint.Paint ?
                (com.sun.prism.paint.Paint)p.platformPaint : null;

        RenderJob re = new RenderJob(new Runnable() {

            private com.sun.prism.paint.Color getClearColor() {
                if (currentPaint == null) {
                    return com.sun.prism.paint.Color.WHITE;
                } else if (currentPaint.getType() == com.sun.prism.paint.Paint.Type.COLOR) {
                    return (com.sun.prism.paint.Color) currentPaint;
                } else if (currentPaint.isOpaque()) {
                    return com.sun.prism.paint.Color.TRANSPARENT;
                } else {
                    return com.sun.prism.paint.Color.WHITE;
                }
            }

            private void draw(Graphics g, int x, int y, int w, int h) {
                g.setLights(params.lights);
                g.setDepthBuffer(params.depthBuffer);

                g.clear(getClearColor());
                if (currentPaint != null &&
                        currentPaint.getType() != com.sun.prism.paint.Paint.Type.COLOR) {
                    g.getRenderTarget().setOpaque(currentPaint.isOpaque());
                    g.setPaint(currentPaint);
                    g.fillQuad(0, 0, w, h);
                }

                // Set up transform
                if (x != 0 || y != 0) {
                    g.translate(-x, -y);
                }
                if (params.transform != null) {
                    g.transform(params.transform);
                }

                if (params.root != null) {
                    if (params.camera != null) {
                        g.setCamera(params.camera);
                    }
                    NGNode ngNode = params.root;
                    ngNode.render(g);
                }

            }

            private void renderTile(int x, int xOffset, int y, int yOffset, int w, int h,
                                    IntBuffer buffer, ResourceFactory rf, QuantumImage tileImg, QuantumImage targetImg) {
                RTTexture rt = tileImg.getRT(w, h, rf);
                if (rt == null) {
                    return;
                }
                Graphics g = rt.createGraphics();
                draw(g, x + xOffset, y + yOffset, w, h);
                int[] pixels = rt.getPixels();
                if (pixels != null) {
                    buffer.put(pixels);
                } else {
                    rt.readPixels(buffer, rt.getContentX(), rt.getContentY(), w, h);
                }
                //Copy tile's pixels into the target image
                targetImg.image.setPixels(xOffset, yOffset, w, h,
                        javafx.scene.image.PixelFormat.getIntArgbPreInstance(), buffer, w);
                rt.unlock();
            }

            private void renderWholeImage(int x, int y, int w, int h, ResourceFactory rf, QuantumImage pImage) {
                RTTexture rt = pImage.getRT(w, h, rf);
                if (rt == null) {
                    return;
                }
                Graphics g = rt.createGraphics();
                draw(g, x, y, w, h);
                int[] pixels = rt.getPixels();
                if (pixels != null) {
                    pImage.setImage(com.sun.prism.Image.fromIntArgbPreData(pixels, w, h));
                } else {
                    IntBuffer ib = IntBuffer.allocate(w * h);
                    if (rt.readPixels(ib, rt.getContentX(), rt.getContentY(), w, h)) {
                        pImage.setImage(com.sun.prism.Image.fromIntArgbPreData(ib, w, h));
                    } else {
                        pImage.dispose();
                        pImage = null;
                    }
                }
                rt.unlock();
            }


            private int computeTileSize(int size, int maxSize) {
                // If 'size' divided by either 2 or 3 produce an exact result
                // and is lesser that the specified maxSize, then use this value
                // as the tile size, as this makes the tiling process more efficient.
                for (int n = 1; n <= 3; n++) {
                    int optimumSize = size / n;
                    if (optimumSize <= maxSize && optimumSize * n == size) {
                        return optimumSize;
                    }
                }
                return maxSize;
            }

            @Override
            public void run() {

                ResourceFactory rf = GraphicsPipeline.getDefaultResourceFactory();

                if (!rf.isDeviceReady()) {
                    return;
                }

                int x = params.x;
                int y = params.y;
                int w = params.width;
                int h = params.height;

                if (w <= 0 || h <= 0) {
                    return;
                }

                boolean errored = false;
                // A temp QuantumImage used only as a RTT cache for rendering tiles.
                QuantumImage tileRttCache = null;
                try {
                    QuantumImage pImage = (params.platformImage instanceof QuantumImage) ?
                            (QuantumImage) params.platformImage : new QuantumImage((com.sun.prism.Image) null);

                    int maxTextureSize = rf.getMaximumTextureSize();
                    if (h > maxTextureSize || w > maxTextureSize) {
                        tileRttCache = new QuantumImage((com.sun.prism.Image) null);
                        // The requested size for the snapshot is too big to fit a single texture,
                        // so we need to take several snapshot tiles and merge them into pImage
                        if (pImage.image == null) {
                            pImage.setImage(com.sun.prism.Image.fromIntArgbPreData(IntBuffer.allocate(w * h), w, h));
                        }

                        // M represents the middle set of tiles each with a size of tileW x tileH.
                        // R is the right hand column of tiles,
                        // B is the bottom row,
                        // C is the corner:
                        // +-----------+-----------+  .  +-------+
                        // |           |           |  .  |       |
                        // |     M     |     M     |  .  |   R   |
                        // |           |           |  .  |       |
                        // +-----------+-----------+  .  +-------+
                        // |           |           |  .  |       |
                        // |     M     |     M     |  .  |   R   |
                        // |           |           |  .  |       |
                        // +-----------+-----------+  .  +-------+
                        //       .           .        .      .
                        // +-----------+-----------+  .  +-------+
                        // |     B     |     B     |  .  |   C   |
                        // +-----------+-----------+  .  +-------+
                        final int mTileWidth = computeTileSize(w, maxTextureSize);
                        final int mTileHeight = computeTileSize(h, maxTextureSize);
                        IntBuffer buffer = IntBuffer.allocate(mTileWidth * mTileHeight);
                        // Walk through all same-size "M" tiles
                        int mTileXOffset = 0;
                        int mTileYOffset = 0;
                        for (mTileXOffset = 0; (mTileXOffset + mTileWidth) <= w; mTileXOffset += mTileWidth) {
                            for (mTileYOffset = 0; (mTileYOffset + mTileHeight) <= h; mTileYOffset += mTileHeight) {
                                renderTile(x, mTileXOffset, y, mTileYOffset, mTileWidth, mTileHeight,
                                        buffer, rf, tileRttCache, pImage);
                            }
                        }
                        // Walk through remaining same-height "R" tiles, if any
                        final int rTileXOffset = mTileXOffset;
                        final int rTileWidth = w - rTileXOffset;
                        if (rTileWidth > 0) {
                            for (int rTileYOffset = 0; (rTileYOffset + mTileHeight) <= h; rTileYOffset += mTileHeight) {
                                renderTile(x, rTileXOffset, y, rTileYOffset, rTileWidth, mTileHeight,
                                        buffer, rf, tileRttCache, pImage);
                            }
                        }
                        // Walk through remaining same-width "B" tiles, if any
                        final int bTileYOffset = mTileYOffset;
                        final int bTileHeight = h - bTileYOffset;
                        if (bTileHeight > 0) {
                            for (int bTileXOffset = 0; (bTileXOffset + mTileWidth) <= w; bTileXOffset += mTileWidth) {
                                renderTile(x, bTileXOffset, y, bTileYOffset, mTileWidth, bTileHeight,
                                        buffer, rf, tileRttCache, pImage);
                            }
                        }
                        // Render corner "C" tile if needed
                        if (rTileWidth > 0 &&  bTileHeight > 0) {
                            renderTile(x, rTileXOffset, y, bTileYOffset, rTileWidth, bTileHeight,
                                    buffer, rf, tileRttCache, pImage);
                        }
                    }
                    else {
                        // The requested size for the snapshot fits max texture size,
                        // so we can directly render it in the target image.
                        renderWholeImage(x, y, w, h, rf, pImage);
                    }
                    params.platformImage = pImage;
                } catch (Throwable t) {
                    errored = true;
                    t.printStackTrace(System.err);
                } finally {
                    if (tileRttCache != null) {
                        tileRttCache.dispose();
                    }
                    Disposer.cleanUp();
                    rf.getTextureResourcePool().freeDisposalRequestedAndCheckResources(errored);
                }
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        re.setCompletionListener(job -> latch.countDown());
        addRenderJob(re);

        do {
            try {
                latch.await();
                break;
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        } while (true);

        Object image = params.platformImage;
        params.platformImage = saveImage;

        return image;
    }

    @Override
    public FileChooserResult showFileChooser(final TKStage ownerWindow,
                                      final String title,
                                      final File initialDirectory,
                                      final String initialFileName,
                                      final FileChooserType fileChooserType,
                                      final List<FileChooser.ExtensionFilter>
                                              extensionFilters,
                                      final FileChooser.ExtensionFilter selectedFilter) {
        WindowStage blockedStage = null;
        try {
            // NOTE: we block the owner of the owner deliberately.
            //       The native system blocks the nearest owner itself.
            //       Otherwise sheets on Mac are unusable.
            blockedStage = blockOwnerStage(ownerWindow);

            return CommonDialogs.showFileChooser(
                    (ownerWindow instanceof WindowStage)
                            ? ((WindowStage) ownerWindow).getPlatformWindow()
                            : null,
                    initialDirectory,
                    initialFileName,
                    title,
                    (fileChooserType == FileChooserType.SAVE)
                            ? CommonDialogs.Type.SAVE
                            : CommonDialogs.Type.OPEN,
                    (fileChooserType == FileChooserType.OPEN_MULTIPLE),
                    convertExtensionFilters(extensionFilters),
                    extensionFilters.indexOf(selectedFilter));
        } finally {
            if (blockedStage != null) {
                blockedStage.setEnabled(true);
            }
        }
    }

    @Override
    public File showDirectoryChooser(final TKStage ownerWindow,
                                     final String title,
                                     final File initialDirectory) {
        WindowStage blockedStage = null;
        try {
            // NOTE: we block the owner of the owner deliberately.
            //       The native system blocks the nearest owner itself.
            //       Otherwise sheets on Mac are unusable.
            blockedStage = blockOwnerStage(ownerWindow);

            return CommonDialogs.showFolderChooser(
                    (ownerWindow instanceof WindowStage)
                            ? ((WindowStage) ownerWindow).getPlatformWindow()
                            : null,
                    initialDirectory, title);
        } finally {
            if (blockedStage != null) {
                blockedStage.setEnabled(true);
            }
        }
    }

    private WindowStage blockOwnerStage(final TKStage stage) {
        if (stage instanceof WindowStage) {
            final TKStage ownerStage = ((WindowStage) stage).getOwner();
            if (ownerStage instanceof WindowStage) {
                final WindowStage ownerWindowStage = (WindowStage) ownerStage;
                ownerWindowStage.setEnabled(false);
                return ownerWindowStage;
            }
        }

        return null;
    }

    private static List<CommonDialogs.ExtensionFilter>
            convertExtensionFilters(final List<FileChooser.ExtensionFilter>
                                            extensionFilters) {
        final CommonDialogs.ExtensionFilter[] glassExtensionFilters =
                new CommonDialogs.ExtensionFilter[extensionFilters.size()];

        int i = 0;
        for (final FileChooser.ExtensionFilter extensionFilter:
                 extensionFilters) {
            glassExtensionFilters[i++] =
                    new CommonDialogs.ExtensionFilter(
                            extensionFilter.getDescription(),
                            extensionFilter.getExtensions());
        }

        return Arrays.asList(glassExtensionFilters);
    }

    @Override
    public long getMultiClickTime() {
        return View.getMultiClickTime();
    }

    @Override
    public int getMultiClickMaxX() {
        return View.getMultiClickMaxX();
    }

    @Override
    public int getMultiClickMaxY() {
        return View.getMultiClickMaxY();
    }

    @Override
    public String getThemeName() {
        return Application.GetApplication().getHighContrastTheme();
    }

    @Override
    public GlassRobot createRobot() {
        return com.sun.glass.ui.Application.GetApplication().createRobot();
    }
}
