/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import com.sun.javafx.PlatformUtil;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Window;
import com.sun.javafx.tk.CompletionListener;
import com.sun.javafx.tk.RenderJob;

import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import com.sun.javafx.logging.PulseLogger;

/**
 * Manages the collection and rendering of dirty scenes. This class has
 * methods which may be called from one of several threads, depending
 * on the method.
 *
 * <ul>
 *     <li>createInstance: Called by QuantumToolkit once during initialization</li>
 *     <li>getInstance: May be called from any thread</li>
 *     <li>hasDirty: May be called from any thread</li>
 *     <li>addDirtyScene: Called only on the FX Thread</li>
 *     <li>removeDirtyScene: Called only on the FX Thread</li>
 *     <li>getRendered: May be called from any thread</li>
 *     <li>liveRepaintRenderJob: Called only on the FX Thread</li>
 *     <li>renderAll: Called only on the FX Thread</li>
 * </ul>
 *
 * Assertions have been added to each method to verify whether the calling
 * thread is the expected thread.
 */
final class PaintCollector implements CompletionListener {
    /*
        Generally we would prefer to remove this static state and pass the
        collector where it needs to go rather than having code reach into this
        static method to get the instance. IoC (inversion of control) makes
        the code more readable and testable, in general.
    */
    private static volatile PaintCollector collector;

    static PaintCollector createInstance(QuantumToolkit toolkit) {
        return collector = new PaintCollector(toolkit);
    }

    static PaintCollector getInstance() {
        return collector;
    }

    /**
     * Sorts the dirty scenes such that asynchronous scenes come first
     */
    private static final Comparator<GlassScene> DIRTY_SCENE_SORTER = (o1, o2) -> {
        int i1 = o1.isSynchronous() ? 1 : 0;
        int i2 = o2.isSynchronous() ? 1 : 0;
        return i1 - i2;
    };

    /**
     * Contains a list of all of the dirty scenes. This list is populated
     * only from the FX Thread in consequence of a call to addDirtyScene,
     * or cleared from the FX Thread in consequence of a call to renderAll
     * or removeDirtyScene. It is only ever accessed (both read and write!)
     * from the FX thread.
     */
    private final List<GlassScene> dirtyScenes = new ArrayList<>();

    /**
     * Keeps track of the number of scenes which still need to be processed.
     * In the renderAll method, we will await on this latch until all currently
     * pending scenes are completed. Once they are all completed, we will
     * create a new CountDownLatch initialized to the size of the number of
     * scenes to be processed, and then process each scene in turn (or rather,
     * cause them to render on the render thread). As each scene completes,
     * the CompletionListener will be invoked which will decrement the
     * allWorkCompletedLatch.
     */
    private volatile CountDownLatch allWorkCompletedLatch = new CountDownLatch(0);

    /**
     * Indicates whether this PaintCollector has any dirty scenes that
     * need to be processed. This is used by the QuantumToolkit to detect
     * in the postPulse() method whether there are dirty scenes. If there
     * are, then the postPulse will potentially post a new pulse event.
     * Updated from the FX Thread, but may be read from any thread.
     */
    private volatile boolean hasDirty;

    /**
     * A reference to the toolkit. This is supplied in the constructor.
     * Although a Toolkit.getToolkit() call and cast to QuantumToolkit
     * could be used, it is somewhat cleaner to simply supply these
     * parameters in the constructor and not reach out to static state.
     */
    private final QuantumToolkit toolkit;

    /**
     * Indicates whether we should attempt to wait for vsync at
     * the conclusion of rendering all scenes. This is set in the
     * renderAll method if there are any synchronous scenes. If true,
     * then after the last scene is processed we will indicate to the
     * Toolkit that it should exercise the vsync block, and let it
     * decide whether to actually do so or not (based on flags, or
     * what OS we're on, etc).
     *
     * <p>This field will be set from the FX thread and read from
     * the Render thread, hence it is volatile.</p>
     */
    private volatile boolean needsHint;

    /**
     * Singleton constructor.
     *
     * @param qt The QuantumToolkit instance.
     */
    private PaintCollector(QuantumToolkit qt) {
        toolkit  = qt;
    }

    /**
     * Called by renderAll to wait for rendering to complete before
     * continuing.
     */
    void waitForRenderingToComplete() {
        while (true) {
            try {
                // We need to keep waiting until things are done!
                allWorkCompletedLatch.await();
                return;
            } catch (InterruptedException ex) {
                // An interrupted exception at this point is a
                // bad thing. It might have happened during shutdown,
                // perhaps? Or somebody is poking the FX thread and
                // asking it to interrupt. Either way, it means
                // that we have not yet completed rendering some
                // scenes and we're about to make a mess of things.
                // Best thing to do is to retry.
            }
        }
    }

    /**
     * Gets whether there are any dirty scenes that need to be rendered. If
     * true, then a subsequent pulse event and renderAll call is required.
     *
     * @return Whether there are any dirty scenes that need to be rendered.
     */
    final boolean hasDirty() {
        return hasDirty;
    }

    private final void setDirty(boolean value) {
        hasDirty = value;
        if (hasDirty) {
            QuantumToolkit.getToolkit().requestNextPulse();
        }
    }

    /**
     * Adds a dirty scene to the PaintCollector for subsequent processing.
     * This method simply makes the PaintCollector aware of this new
     * scene and ensure it gets processed on the next call to renderAll.
     *
     * The next QuantumToolkit Glass timer generated pulse or PaintCollector
     * rendering vsync hinted pulse will process these dirty scenes.
     *
     * <p>This method must only be called on the FX Thread</p>
     *
     * @param scene    The scene which is dirty. This must not be null.
     */
    final void addDirtyScene(GlassScene scene) {
        // Check that we are on the expected thread.
        assert Thread.currentThread() == QuantumToolkit.getFxUserThread();
        // Scene must not be null (using assert for performance)
        assert scene != null;

        if (QuantumToolkit.verbose) {
            System.err.println("PC.addDirtyScene: " + System.nanoTime() + scene);
        }

        // Because dirtyScenes is ever only accessed from the FX Thread,
        // we don't need any form of concurrent access here. Note also
        // that doing a contains() call here is probably faster than using
        // a HashSet because we are dealing with such a small number of
        // scenes that simple iteration is likely to be much faster
        if (!dirtyScenes.contains(scene)) {
            dirtyScenes.add(scene);
            // Now that we know we have added a scene to dirtyScenes,
            // we should ensure hasDirty is true.
            setDirty(true);
        }
    }

    /**
     * Removes a scene from the dirtyScene list. If the given scene
     * was previously added with a call to addDirtyScene, it will
     * be removed. Potentially this means that after this call the
     * PaintCollector will no longer have any dirty scenes and will
     * no longer require a repaint.
     *
     * <p>This method is typically called when a scene is removed
     * from a stage, or when visible becomes false.
     * </p>
     *
     * <p>This method must only be called on the FX Thread</p>
     *
     * @param scene    The scene which is no longer dirty. Must not be null.
     */
    final void removeDirtyScene(GlassScene scene) {
        // Ensure we're called only from the FX thread
        assert Thread.currentThread() == QuantumToolkit.getFxUserThread();
        assert scene != null;

        // Need to convert to use JavaFX Logging instead.
        if (QuantumToolkit.verbose) {
            System.err.println("PC.removeDirtyScene: " + scene);
        }

        // Remove the scene
        dirtyScenes.remove(scene);
        // Update hasDirty
        setDirty(!dirtyScenes.isEmpty());
    }

    /**
     * Gets the CompletionListener which must be notified when a
     * GlassScene has completed rendering.
     *
     * @return The CompletionListener. Will never be null.
     */
    final CompletionListener getRendered() {
        return this;
    }

    /**
     * This object is a CompletionListener is registered with every GlassScene,
     * such that when the repaint has completed, this method is called.
     * This method will decrement the count on the allWorkCompletedLatch.
     */
    @Override public void done(RenderJob job) {
        // It would be better to have an assertive check that
        // this call is being made on the render thread, rather
        // than on the FXT, but this is easier for now.
        assert Thread.currentThread() != QuantumToolkit.getFxUserThread();

        if (!(job instanceof PaintRenderJob)) {
            throw new IllegalArgumentException("PaintCollector: invalid RenderJob");
        }

        final PaintRenderJob paintjob = (PaintRenderJob)job;
        final GlassScene scene = paintjob.getScene();

        if (scene == null) {
            throw new IllegalArgumentException("PaintCollector: null scene");
        }

        // This callback on Scene only exists to allow the performance
        // counter to be notified when a scene has been rendered. We
        // could reduce the class count and indirection if we had a more
        // direct method for notifying some performance tracker rather
        // than going through this round-about way.
        scene.frameRendered();

        // Work to be done after all rendering is completed. Note that
        // I check against "1" to indicate all rendering is done, and
        // only decrement the allWorkCompletedLatch after wards. This is
        // because as soon as I decrement the allWorkCompletedLatch to 0,
        // then whatever code remains in this method will run concurrently
        // with the FX app thread, and I'd prefer to minimize the number
        // of things here that could be happening in parallel.
        if (allWorkCompletedLatch.getCount() == 1) {
            // In some cases we need to tell the toolkit that
            // now would be a great time to vsync!
            if (needsHint && !toolkit.hasNativeSystemVsync()) {
                toolkit.vsyncHint();
            }

            Application.GetApplication().notifyRenderingFinished();

            // If pulse logging is enabled, then we must call renderEnd now
            // that we know that all of the scene's being rendered are finished
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.renderEnd();
            }
        }

        // Count down the latch, indicating that drawing has
        // completed for some scene.
        allWorkCompletedLatch.countDown();
    }

    /**
     * Run a full pulse and repaint before returning.
     */
    final void liveRepaintRenderJob(final ViewScene scene) {
         ViewPainter viewPainter = scene.getPainter();
         QuantumToolkit quantum = (QuantumToolkit)QuantumToolkit.getToolkit();
         quantum.pulse(false);
         final CountDownLatch latch = new CountDownLatch(1);
         QuantumToolkit.runWithoutRenderLock(() -> {
             quantum.addRenderJob(new RenderJob(viewPainter, rj -> latch.countDown()));
             try {
                 latch.await();
             } catch (InterruptedException e) {
                 //Fail silently.  If interrupted, then proceed with the UI ...
             }
             return null;
         });
     }

    /**
     * Called by QuantumToolkit during a pulse to render whatever dirty scenes
     * we have. This method is only called on the FX thread.
     */
    final void renderAll() {
        // Ensure we're called only from the FX thread
        assert Thread.currentThread() == QuantumToolkit.getFxUserThread();

        // TODO switch to using a logger
        if (QuantumToolkit.pulseDebug) {
            System.err.println("PC.renderAll(" + dirtyScenes.size() + "): " + System.nanoTime());
        }

        // Since hasDirty can only be set to true from the FX Thread,
        // we can do just a simple boolean check here. If we don't
        // have any dirty scenes to process, then we are done.
        if (!hasDirty) {
            return;
        }

        // Because hasDirty is tied to dirtyScenes, it should
        // not be possible that we reach this point if dirtyScenes
        // is empty (since hasDirty was true)
        assert !dirtyScenes.isEmpty();

        // Sort the dirty scenes based on whether they are
        // synchronous or not. If they are not synchronous,
        // then we want to process them first.
        Collections.sort(dirtyScenes, DIRTY_SCENE_SORTER);

        // Reset the fields
        setDirty(false);
        needsHint = false;

        // If pulse logging is enabled, then we must call renderStart
        // BEFORE we actually call repaint on any of the dirty scenes.
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.renderStart();
        }

        // This part needs to be handled a bit differently depending on whether our platform has a native
        // window manager or not.
        // So, check to see if we do (Note: how we determine this need to be improved, this should
        // eventually call down into platform-specific glass code and not rely on
        // a system property, but we will use this for now)
        if (!Application.GetApplication().hasWindowManager()) {
            // No native window manager.  We call repaint on every scene (to make sure it gets recopied
            // to the screen) but we may be able to skip some steps in the repaint.

            // Obtain a z-ordered window list from glass.  For platforms without a native window manager,
            // we need to recopy the all of the window contents to the screen on every frame.
            final List<com.sun.glass.ui.Window> glassWindowList = com.sun.glass.ui.Window.getWindows();
            allWorkCompletedLatch = new CountDownLatch(glassWindowList.size());
            for (int i = 0, n = glassWindowList.size(); i < n; i++) {
                final Window w = glassWindowList.get(i);
                final WindowStage ws = WindowStage.findWindowStage(w);
                if (ws != null) {
                    final ViewScene vs = ws.getViewScene();

                    // Check to see if this scene is in our dirty list.  If so, we will need to render
                    // the scene before we recopy it to the screen.  If not, we can skip this step.
                    if (dirtyScenes.indexOf(vs) != -1) {
                        if (!needsHint) {
                            needsHint = vs.isSynchronous();
                        }
                    }
                    if (!PlatformUtil.useEGL() || i == (n - 1)) {
                        // for platforms without a native window manager, we only want to do the
                        // swap to the screen after the last window has been rendered
                        vs.setDoPresent(true);
                    } else {
                        vs.setDoPresent(false);
                    }
                    try {
                        vs.repaint();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        } else {
            // We have a native window manager.  Only call repaint on the dirty scenes,
            // and swap to the screen on a per-window basis.
            //
            // Now we are ready to repaint each scene. We will first process
            // the uploadScenes, followed by the syncScenes. The reason we
            // want to do this is that when the last syncScene is processed,
            // if needsHint is true, then we will wait for vsync. We clearly
            // don't want to do this until all the dirty scenes have been
            // processed.
            allWorkCompletedLatch = new CountDownLatch(dirtyScenes.size());

            for (final GlassScene gs : dirtyScenes) {
                // Only post the vsync hint if there are synchronous scenes
                if (!needsHint) {
                    needsHint = gs.isSynchronous();
                }
                // On platforms with a window manager, we always set doPresent = true, because
                // we always need to rerender the scene  if it's in the dirty list and we do a
                // swap on a per-window basis
                gs.setDoPresent(true);
                try {
                    gs.repaint();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        dirtyScenes.clear();

        if (toolkit.shouldWaitForRenderingToComplete()) {
            waitForRenderingToComplete();
        }
    }
}
