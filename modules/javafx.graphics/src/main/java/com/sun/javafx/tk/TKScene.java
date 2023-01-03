/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessControlContext;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.sg.prism.NGNode;

/**
 * TKScene
 *
 */
public interface TKScene {

    /**
      * This method is called from Scene, when it is being destroyed.
      */
    public void dispose();

    public void waitForRenderingToComplete();

    /**
     * Waits until the render thread is available for synchronization
     * from the scene graph. Once this method returns, the caller has
     * the lock, and will continue to hold the lock until releaseSynchronization
     * is called.
     */
    public void waitForSynchronization();

    /**
     * Releases the synchronization lock previously held. If the updateState
     * flag is set then the glass scene state is updated prior to releasing
     * the lock.
     */
    public void releaseSynchronization(boolean updateState);

    public void setTKSceneListener(TKSceneListener listener);
    public void setTKScenePaintListener(final TKScenePaintListener listener);

    public void setRoot(NGNode root);

    public void markDirty();

    public void setCamera(NGCamera camera);

    NGLightBase[] getLights();
    public void setLights(NGLightBase[] lights);

    /**
     * Set the background fill for the scene
     *
     * @param fillPaint This must be a paint class as returned from Toolkit.createPaint(...)
     */
    public void setFillPaint(Object fillPaint);

    public void setCursor(Object cursor);

    public void enableInputMethodEvents(boolean enable);

    public void finishInputMethodComposition();

    public void entireSceneNeedsRepaint();

    public TKClipboard createDragboard(boolean isDragSource);

    @SuppressWarnings("removal")
    public AccessControlContext getAccessControlContext();
}
