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

import com.sun.prism.Graphics;
import com.sun.prism.impl.Disposer;

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

        SceneState viewState = scene.getViewState();
        try {
            valid = validateStageGraphics();

            if (!valid) {
                if (verbose) {
                    System.err.println("PresentingPainter: validateStageGraphics failed");
                }
                return;
            }
            
            if (viewState != null) {
                /*
                 * As Glass is responsible for creating the rendering contexts,
                 * locking should be done prior to the Prism calls.
                 */
                viewState.lock();
                locked = true;
            }

            boolean needsReset = (presentable == null) || (penWidth != viewWidth) || (penHeight != viewHeight);
            if (needsReset) {
                if (presentable == null || presentable.recreateOnResize()) {
                    context = factory.createRenderingContext(viewState);
                }
            }
            
            context.begin();
            
            if (needsReset) {
                if (presentable == null || presentable.recreateOnResize()) {
                    disposePresentable();
                    presentable = factory.createPresentable(viewState);
                    needsReset = false;
                }
                penWidth  = viewWidth;
                penHeight = viewHeight;
            }
            
            if (presentable != null) {
                Graphics g = presentable.createGraphics();

                ViewScene vs = (ViewScene) viewState.getScene();
                if (g != null && vs.getDirty()) {
                    if (needsReset) {
                        g.reset();
                    }
                    paintImpl(g);
                    vs.setDirty(false);
                }
                
                if (!presentable.prepare(null)) {
                    disposePresentable();
                    viewState.getScene().entireSceneNeedsRepaint();
                    return;
                }
                
                /* present for vsync buffer swap */
                if (vs.getDoPresent()) {
                    if (!presentable.present()) {
                        disposePresentable();
                        viewState.getScene().entireSceneNeedsRepaint();
                    }
                }
            }
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            if (valid && context != null) {
                Disposer.cleanUp();
                context.end();
            }
            if (locked) {
                viewState.unlock();
            }

            ViewScene viewScene = (ViewScene)viewState.getScene();
            viewScene.setPainting(false);
            renderLock.unlock();
        }
    }
}
