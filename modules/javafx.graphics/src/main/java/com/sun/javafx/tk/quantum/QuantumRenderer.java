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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Screen;
import com.sun.javafx.tk.CompletionListener;
import com.sun.javafx.tk.RenderJob;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.GraphicsResource;
import com.sun.prism.Presentable;
import com.sun.prism.ResourceFactory;
import com.sun.prism.impl.PrismSettings;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.prism.PrFilterContext;
import java.util.HashMap;

/*
 * Quantum Renderer
 */
final class QuantumRenderer extends ThreadPoolExecutor  {
    @SuppressWarnings("removal")
    private static boolean usePurgatory = // TODO - deprecate
        AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("decora.purgatory"));


    private static final AtomicReference<QuantumRenderer> instanceReference =
                                    new AtomicReference<>(null);

    private Thread          _renderer;
    private Throwable       _initThrowable = null;
    private CountDownLatch  initLatch = new CountDownLatch(1);

    private QuantumRenderer() {
        super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        setThreadFactory(new QuantumThreadFactory());
    }

    protected Throwable initThrowable() {
        return _initThrowable;
    }

    private void setInitThrowable(Throwable th) {
        _initThrowable = th;
    }

    private class PipelineRunnable implements Runnable {
        private Runnable    work;

        public PipelineRunnable(Runnable runner) {
            work = runner;
        }

        public void init() {
            try {
                if (GraphicsPipeline.createPipeline() == null) {
                    String MSG = "Error initializing QuantumRenderer: no suitable pipeline found";
                    System.err.println(MSG);
                    throw new RuntimeException(MSG);
                } else {
                    Map device = GraphicsPipeline.getPipeline().getDeviceDetails();
                    if (device == null) {
                        device = new HashMap();
                    }
                    device.put(com.sun.glass.ui.View.Capability.kHiDPIAwareKey,
                               PrismSettings.allowHiDPIScaling);
                    Map map =  Application.getDeviceDetails();
                    if (map != null) {
                        device.putAll(map);
                    }
                    Application.setDeviceDetails(device);
                }
            } catch (Throwable th) {
                QuantumRenderer.this.setInitThrowable(th);
            } finally {
                initLatch.countDown();
            }
        }

        public void cleanup() {
            GraphicsPipeline pipeline = GraphicsPipeline.getPipeline();
            if (pipeline != null) {
                pipeline.dispose();
            }
        }

        @Override public void run() {
            try {
                init();
                work.run();
            } finally {
                cleanup();
            }
        }
    }

    private class QuantumThreadFactory implements ThreadFactory {
        final AtomicInteger threadNumber = new AtomicInteger(0);

        @SuppressWarnings("removal")
        @Override public Thread newThread(Runnable r) {
            final PipelineRunnable pipeline = new PipelineRunnable(r);
            _renderer =
                AccessController.doPrivileged((PrivilegedAction<Thread>) () -> {
                    Thread th = new Thread(pipeline);
                    th.setName("QuantumRenderer-" + threadNumber.getAndIncrement());
                    th.setDaemon(true);
                    th.setUncaughtExceptionHandler((t, thr) -> {
                        System.err.println(t.getName() + " uncaught: " + thr.getClass().getName());
                        thr.printStackTrace();
                    });
                    return th;
                });

            assert threadNumber.get() == 1;

            return _renderer;
        }
    }

    protected void createResourceFactory() {
        final CountDownLatch createLatch = new CountDownLatch(1);

        final CompletionListener createDone = job -> createLatch.countDown();

        final Runnable factoryCreator = () -> {
            ResourceFactory factory = GraphicsPipeline.getDefaultResourceFactory();
            assert factory != null;
        };

        final RenderJob job = new RenderJob(factoryCreator, createDone);

        submit(job);

        try {
            createLatch.await();
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }
    }

    /*
     * Dispose the native GraphicsResource of the Presentable on the
     * render thread.  This method can be called from the FX thread
     */
    protected void disposePresentable(final Presentable presentable) {
        assert !Thread.currentThread().equals(_renderer);

        if (presentable instanceof GraphicsResource) {
            final GraphicsResource resource = (GraphicsResource)presentable;

            final Runnable presentableDisposer = () -> resource.dispose();

            final RenderJob job = new RenderJob(presentableDisposer, null);

            submit(job);
        }
    }

    @SuppressWarnings("removal")
    protected void stopRenderer() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            shutdown();
            return null;
        });
        if (PrismSettings.verbose) {
            System.out.println("QuantumRenderer: shutdown");
        }

        assert isShutdown();

        /*
         * ThreadPoolExecutor cannot be restarted once it has been
         * shutdown.  Create a new QuantumRenderer for the next
         * toolkit invocation.
         */
        instanceReference.set(null);
    }

    @Override protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return (RenderJob)runnable;
    }

    protected Future submitRenderJob(RenderJob r) {
        return (submit(r));
    }

    /* java.util.concurrent.ThreadPoolExecutor */

    @Override public void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        /*
         * clean up what we can after every render job
         *
         * we should really be keeping RenderJob/Scene pools
         */
        if (usePurgatory) {
            Screen screen = Screen.getMainScreen();
            Renderer renderer = Renderer.getRenderer(PrFilterContext.getInstance(screen));
            renderer.releasePurgatory();
        }
    }

    void checkRendererIdle() {
        if (PrismSettings.threadCheck) {
            PaintCollector collector = PaintCollector.getInstance();
            final boolean busy = ViewPainter.renderLock.isLocked() &&
                    !ViewPainter.renderLock.isHeldByCurrentThread();

            if (busy) {
                System.err.println("ERROR: PrismPen / FX threads co-running:" +
                                   " DIRTY: " + collector.hasDirty());
                for (StackTraceElement s : QuantumToolkit.getFxUserThread().getStackTrace()) {
                    System.err.println("FX: " + s);
                }
                for (StackTraceElement q : _renderer.getStackTrace()) {
                    System.err.println("QR: " + q);
                }
            }
        }
    }

    public static synchronized QuantumRenderer getInstance() {
        if (instanceReference.get() == null) {
            synchronized (QuantumRenderer.class) {
                QuantumRenderer newTk = null;
                try {
                    newTk = new QuantumRenderer();
                    newTk.prestartCoreThread();

                    newTk.initLatch.await();
                } catch (Throwable t) {
                    if (newTk != null) {
                        newTk.setInitThrowable(t);
                    }
                    if (PrismSettings.verbose) {
                        t.printStackTrace();
                    }
                }
                if (newTk != null && newTk.initThrowable() != null) {
                    if (PrismSettings.noFallback) {
                        System.err.println("Cannot initialize a graphics pipeline, and Prism fallback is disabled");
                        throw new InternalError("Could not initialize prism toolkit, " +
                                                "and the fallback is disabled.");
                    } else {
                        throw new RuntimeException(newTk.initThrowable());
                    }
                }
                instanceReference.set(newTk);
            }
        }
        return instanceReference.get();
    }
}
