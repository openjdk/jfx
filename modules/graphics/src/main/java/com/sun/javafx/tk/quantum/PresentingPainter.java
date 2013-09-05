/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.logging.PulseLogger;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.ManagedResource;
import com.sun.prism.impl.PrismSettings;

/**
 * The PresentingPainter is used when we are rendering to the main screen.
 * UploadingPainter is used when we need to render into an offscreen buffer.
 */
final class PresentingPainter extends ViewPainter {

    PresentingPainter(ViewScene view) {
        super(view);
    }

    @Override public void run() {
        renderLock.lock();

        boolean locked = false;
        boolean valid = false;

        try {
            valid = validateStageGraphics();
            if (!valid) {
                if (QuantumToolkit.verbose) {
                    System.err.println("PresentingPainter: validateStageGraphics failed");
                }
                return;
            }
            
            /*
             * As Glass is responsible for creating the rendering contexts,
             * locking should be done prior to the Prism calls.
             */
            sceneState.lock();
            locked = true;
            
            if (factory == null) {
                factory = GraphicsPipeline.getDefaultResourceFactory();
            }
            if (factory == null || !factory.isDeviceReady()) {
                return;
            }

            if (presentable != null && presentable.lockResources(sceneState)) {
                disposePresentable();
            }
            if (presentable == null) {
                presentable = factory.createPresentable(sceneState);
                penWidth  = viewWidth;
                penHeight = viewHeight;
            }
            
            if (presentable != null) {
                Graphics g = presentable.createGraphics();

                ViewScene vs = (ViewScene) sceneState.getScene();
                if (g != null) {
                    paintImpl(g);
                }

                long start = PulseLogger.PULSE_LOGGING_ENABLED ? System.currentTimeMillis() : 0;
                if (!presentable.prepare(null)) {
                    disposePresentable();
                    sceneState.getScene().entireSceneNeedsRepaint();
                    if (PulseLogger.PULSE_LOGGING_ENABLED) {
                        PulseLogger.PULSE_LOGGER.renderMessage(start, System.currentTimeMillis(), "Presentable.prepare");
                    }
                    return;
                }
                
                /* present for vsync buffer swap */
                start = PulseLogger.PULSE_LOGGING_ENABLED ? System.currentTimeMillis() : 0;
                if (vs.getDoPresent()) {
                    if (!presentable.present()) {
                        disposePresentable();
                        sceneState.getScene().entireSceneNeedsRepaint();
                    }
                    if (PulseLogger.PULSE_LOGGING_ENABLED) {
                        PulseLogger.PULSE_LOGGER.renderMessage(start, System.currentTimeMillis(), "Presentable.present");
                    }
                }
            }
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            if (valid) {
                Disposer.cleanUp();
            }
            if (locked) {
                sceneState.unlock();
            }

            ViewScene viewScene = (ViewScene)sceneState.getScene();
            viewScene.setPainting(false);
            if (PrismSettings.poolStats ||
                ManagedResource.anyLockedResources())
            {
                ManagedResource.printSummary();
            }
            renderLock.unlock();
            if (PulseLogger.PULSE_LOGGING_ENABLED) {
                PulseLogger.PULSE_LOGGER.renderMessage(System.currentTimeMillis(), System.currentTimeMillis(), "Finished Presenting Painter");
            }
        }
    }
}
