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

package com.sun.javafx.embed;

import java.nio.IntBuffer;

import com.sun.javafx.scene.traversal.Direction;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.scene.image.PixelFormat;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.InputMethodTextRun;

/**
 * An interface for embedded FX scene peer. It is used by HostInterface
 * object to send various notifications to the scene, for example, when
 * an input event is received in the host application and should be
 * forwarded to FX.
 *
 */
public interface EmbeddedSceneInterface {

    /*
     * A notification about the embedded container is resized.
     */
    public void setSize(int width, int height);

    /*
     * A notification about the scale factor is changed.
     */
    public void setPixelScaleFactors(float scalex, float scaley);

    /*
     * A request to fetch all the FX scene pixels into a offscreen buffer.
     */
    public boolean getPixels(IntBuffer dest, int width, int height);

    /*
     * A request to get the FX scene's pixel format.
     */
    public PixelFormat<?> getPixelFormat();

    /*
     * A notification about mouse event received by host container.
     */
    public void mouseEvent(int type, int button,
                           boolean primaryBtnDown, boolean middleBtnDown, boolean secondaryBtnDown,
                           int x, int y, int xAbs, int yAbs,
                           boolean shift, boolean ctrl, boolean alt, boolean meta,
                           boolean popupTrigger);
    /*
     * A notification about mouse wheel scroll events received by the host container;
     */
    public void scrollEvent(int type, double scrollX, double scrollY,
                            double totalScrollX, double totalScrollY,
                            double xMultiplier, double yMultiplier,
                            double x, double y, double screenX, double screenY,
                            boolean shift, boolean ctrl,
                            boolean alt, boolean meta, boolean inertia);
    /*
     * A notification about key event received by host container.
     */
    public void keyEvent(int type, int key, char[] chars, int modifiers);

    /*
     * A notification about zoom events received by the host container.
     */
    public void zoomEvent(final int type, final double zoomFactor, final double totalZoomFactor,
                          final double x, final double y, final double screenX, final double screenY,
                          boolean shift, boolean ctrl, boolean alt, boolean meta, boolean inertia);

    /*
     * A notification about rotate events received by the host container.
     */
    public void rotateEvent(final int type, final double angle, final double totalAngle,
                          final double x, final double y, final double screenX, final double screenY,
                          boolean shift, boolean ctrl, boolean alt, boolean meta, boolean inertia);

    /*
     * A notification about swipe events received by the host container.
     */
    public void swipeEvent(final int type, final double x, final double y, final double screenX, final double screenY,
                           boolean shift, boolean ctrl, boolean alt, boolean meta);

    /*
     * A notification about menu event received by host container.
     */
    public void menuEvent(int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger);

    public boolean traverseOut(Direction dir);

    public void setDragStartListener(HostDragStartListener l);

    public EmbeddedSceneDTInterface createDropTarget();

    public void inputMethodEvent(EventType<InputMethodEvent> type,
                                 ObservableList<InputMethodTextRun> composed, String committed,
                                 int caretPosition);

    public InputMethodRequests getInputMethodRequests();
}
