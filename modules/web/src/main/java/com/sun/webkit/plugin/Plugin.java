/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.plugin;

import java.io.IOException;

import com.sun.webkit.graphics.WCGraphicsContext;

public interface Plugin {
    public final static int EVENT_BEFOREACTIVATE = -4;
    public final static int EVENT_FOCUSCHANGE = -1;

    /**
     * Sets focus to plugin by webkit
     */
    public void requestFocus();

    /**
     * Sets plugin location in native container
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void setNativeContainerBounds(int x, int y, int width, int height);

    /**
     * Initiates plugin life circle.
     * @param pl
     */
    void activate(Object nativeContainer, PluginListener pl);

    /**
     * Destroys plugin.
     */
    void destroy();

    /**
     * Makes plugin visible/hidden.
     * @param isVisible
     */
    void setVisible(boolean isVisible);

    /**
     * Makes plugin interactive if possible.
     * @param isEnable
     */
    public void setEnabled(boolean enabled);

    /**
     * Sets new position for plugin.
     * @param x
     * @param y
     * @param width
     * @param height
     */
    void setBounds(int x, int y, int width, int height);

    /**
     * Script action over the plugin
     * @param subObjectId
     * @param methodName
     * @param args
     * @return the
     * @throws java.io.IOException
     */
    Object invoke(
            String subObjectId,
            String methodName,
            Object[] args) throws IOException;


    /**
     * Paints plugin by Webkit request in selected bounds
     * @param g
     * @param x
     * @param y
     * @param width
     * @param height
     */
     public void paint(WCGraphicsContext g, int intX, int intY, int intWidth, int intHeight);

     public boolean handleMouseEvent(
            String type,
            int offsetX,
            int offsetY,
            int screenX,
            int screenY,
            int button,
            boolean buttonDown,
            boolean altKey,
            boolean metaKey,
            boolean ctrlKey,
            boolean shiftKey,
            long timeStamp);
}
