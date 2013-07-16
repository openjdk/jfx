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

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Pixels;

/**
 * EmbeddedState is intended to provide for a shadow copy the View/Scene state
 * similar to the shadow Graph, providing a static snapshot until the Scene 
 * is rendered.  EmbeddedState captures state that is specific to embedding.
 */
final class EmbeddedState extends SceneState {

    public EmbeddedState(GlassScene vs) {
        super(vs);
    }

    /**
     * Put the pixels on the screen.
     * 
     * @param pixels - the pixels to draw
     * @param uploadCount - the number of uploads (can be null)
     */
    public void uploadPixels(Pixels pixels, AtomicInteger uploadCount) {
        if (isValid()) {
            EmbeddedScene escene = (EmbeddedScene) scene;
            // Pixels are always stored in an IntBuffer for uploading
            escene.textureBits = (IntBuffer) pixels.getPixels();
            escene.host.repaint();
            if (uploadCount != null) {
                uploadCount.decrementAndGet();
            }
        }
    }

    /**
     * Drawing can occur when there is an embedded scene that has a host.
     *
     * @return true if drawing can occur; false otherwise
     *
     * May be called on any thread.
     */
    public boolean isValid() {
        EmbeddedScene escene = (EmbeddedScene) scene;
        return escene != null && escene.host != null && getWidth() > 0 && getHeight() > 0;
    }

    /** Updates the state of this object based on the current state of its
     * nativeWindow.
     *
     * May only be called from the event thread.
     */
    public void update() {
        super.update();
        EmbeddedScene escene = (EmbeddedScene) scene;
        if (scene != null) {
            viewWidth = escene.width;
            viewHeight = escene.height;
            // These variables and others from the super class need be kept up to date to
            // minimize rendering.  For now, claim that the embedded scene is always visible
            // and not minimized so that rendering can occur
            isWindowVisible = true;
            isWindowMinimized = false;
        }
    }
}
