/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.javafx.tk;

/**
 * TKSceneListener - Listener for the Scene Peer TKScene to pass updates and events back to the scene
 *
 */
public interface TKSceneListener {

    /**
     * The scenes peer's location have changed so we need to update the scene
     *
     * @param x the new X
     * @param y The new Y
     */
    public void changedLocation(float x, float y);

    /**
     * The scenes peer's size have changed so we need to update the scene
     *
     * @param width The new Width
     * @param height The new Height
     */
    public void changedSize(float width, float height);

    /**
     * Pass a mouse event to the scene to handle
     *
     * @param event The event, this should be of type javafx.scene.input.MouseEvent
     */
    public void mouseEvent(Object event);

    /**
     * Pass a key event to the scene to handle
     *
     * @param event The event, this should be of type javafx.scene.input.KeyEvent
     */
    public void keyEvent(Object event);

    /**
     * Pass an input method event to the scene to handle
     *
     * @param event The event, this should be of type 
     *     javafx.scene.input.InputMethodEvent
     */
    public void inputMethodEvent(Object event);

    public void scrollEvent(
            double scrollX, double scrollY,
            double xMultiplier, double yMultiplier,
            int scrollTextX, int scrollTextY,
            int defaultTextX, int defaultTextY,
            double x, double y, double screenX, double screenY,
            boolean _shiftDown, boolean _controlDown,
            boolean _altDown, boolean _metaDown);

    public void menuEvent(double x, double y, double xAbs, double yAbs,
            boolean isKeyboardTrigger);

}
