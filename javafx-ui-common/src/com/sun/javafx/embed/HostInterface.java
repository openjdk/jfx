/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.embed;

import com.sun.javafx.cursor.CursorFrame;

/*
 * An interface for embedding container. All the methods in this
 * interface are to be used by embedded FX application to request
 * or notify embedding application about various changes, for
 * example, when embedded FX scene changes is painted, it calls
 * HostInterface.repaint() to notify that the container should
 * be eventually repainted to reflect new scene pixels.
 *
 */
public interface HostInterface {

    public void setEmbeddedStage(EmbeddedStageInterface embeddedStage);
    public void setEmbeddedScene(EmbeddedSceneInterface embeddedScene);

    /*
     * Called by embedded FX scene to request focus to this container
     * in an embedding app.
     */
    public boolean requestFocus();

    /*
     * Called by embedded FX scene to traverse focus to a component
     * which is next/previous to this container in an emedding app.
     */
    public boolean traverseFocusOut(boolean forward);

    /*
     * Called by embedded FX scene when its opacity is changed, so
     * embedding container will later draw the scene pixels with
     * a new opacity value.
     */
/*
    public void setOpacity(float opacity);
*/

    /*
     * Called by embedded FX scene when it is repainted, so embedding
     * container will eventually repaint itself to reflect the changes.
     */
    public void repaint();

    /*
     * Called by embedded FX stage when its size is changed, so
     * embedding container will later report the size as the preferred size.
     */
    public void setPreferredSize(int width, int height);

    /*
     * Called by embedded FX stage when FX enables/disables the stage.
     */
    public void setEnabled(boolean enabled);
    
    /*
     * Called by embedded FX scene when its cursor is changed.
     */
    public void setCursor(CursorFrame cursorFrame);
}
