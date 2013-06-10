/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Pixels;
import com.sun.prism.PresentableState;

/**
 * ViewState is intended to provide for a shadow copy the View/Scene state
 * similar to the shadow Graph, providing a static snapshot until the Scene 
 * is rendered.
 */
class SceneState extends PresentableState {

    GlassScene scene;

    /**
     * Create the View State
     * .
     * May only be called from the event thread.
     */
    public SceneState(GlassScene vs) {
        super();
        scene = vs;
    }

    /**
     * Returns the glass scene for the view state
     * .
     * @return the glass scene 
     *
     * May be called on any thread.
     */
    public GlassScene getScene() {
        return scene;
    }

    /**
     * Drawing can occur in a Glass view if the view exists
     * (is not null), the window exists (is not null) and
     * the view is attached to a window (ie. has not been
     * closed).
     *
     * @return true if drawing can occur; false otherwise
     *
     * May be called on any thread.
     */
    public boolean isValid() {
        return getWindow() != null && getView() != null && !isViewClosed() && getWidth() > 0 && getHeight() > 0;
    }

    /** Updates the state of this object based on the current
     * state of the glass scene.
     *
     * May only be called from the event thread.
     */
    public void update() {
        // When the state is created, the platform view has not yet been
        // created (it is null).  Update the view each time the we ask
        // for the updated state.
        view = scene.getPlatformView();
        super.update();
    }
    
    /**
     * Put the pixels on the screen.
     * 
     * @param pixels - the pixels to draw
     * @param uploadCount - the number of uploads (can be null)
     */
    public void uploadPixels(final Pixels pixels, AtomicInteger uploadCount) {
        Application.invokeLater(new Runnable() {
            @Override public void run() {
                if (isValid()) {
                    SceneState.super.uploadPixels(pixels, uploadCount);
                } else {
                    if (uploadCount != null) {
                        uploadCount.decrementAndGet();
                    }
                }
           }
        });
    }
}
