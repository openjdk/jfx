/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.stage.Stage;
import java.util.Map;

/**
 * Applets must run within a parent window, so this interface will allow the
 * plugin access to the underlying parent window that all Stages will be created
 * in.
 */
public interface AppletWindow {
    /*
     * topStage must be a primary stage and it's backing window a child window
     * of this AppletWindow or this method will have no effect.
     * The window will do what it can to make sure the given stage is on top of
     * the other primary applet stages. In the future we will allow specifying
     * Z order but that requires low level changes to Glass to do properly.
     */
    public void setStageOnTop(Stage topStage);

    public void setBackgroundColor(int color); // RGB triplet: 0xRRGGBB
    public void setForegroundColor(int color);

    public void setVisible(boolean state);

    public void setSize(int width, int height);
    public int getWidth();
    public int getHeight();

    public void setPosition(int x, int y);
    public int getPositionX();
    public int getPositionY();

    public float getPlatformScaleX();
    public float getPlatformScaleY();

    // returns CARemoteLayer id (only used on Mac)
    public int getRemoteLayerId();
    // dispatchEvent (only used on Mac)
    public void dispatchEvent(Map eventInfo);
}
