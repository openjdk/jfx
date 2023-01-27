/*
 *  Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package com.sun.javafx.scene.control.behavior;

import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.stage.WindowHelper;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.SceneDecoration;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.WindowEdge;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.Map;

public class SceneDecorationBehaviour extends BehaviorBase<SceneDecoration> {
    private final InputMap<SceneDecoration> inputMap;
    private final Stage stage;
    private final Scene scene;
    private double lastScreenX;
    private double lastScreenY;
    private double lastLocalY;
    private double lastLocalX;

    private Region headerRegion;

    //TODO: Account Shadows
    private static final int RESIZE_THRESHOLD = 23;
    private static final Map<WindowEdge, Cursor> CURSOR_MAP = Map.of(WindowEdge.WEST, Cursor.W_RESIZE,
            WindowEdge.EAST, Cursor.E_RESIZE,
            WindowEdge.SOUTH, Cursor.S_RESIZE,
            WindowEdge.NORTH, Cursor.N_RESIZE,
            WindowEdge.SOUTH_WEST, Cursor.SW_RESIZE,
            WindowEdge.SOUTH_EAST, Cursor.SE_RESIZE,
            WindowEdge.NORTH_WEST, Cursor.NW_RESIZE,
            WindowEdge.NORTH_EAST, Cursor.NE_RESIZE);
    private WindowEdge currentEdge;

    public SceneDecorationBehaviour(SceneDecoration decoration) {
        super(decoration);
        inputMap = createInputMap();
        this.stage = decoration.getStage();
        this.scene = stage.getScene();

        addDefaultMapping(
                new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed),
                new InputMap.MouseMapping(MouseEvent.MOUSE_DRAGGED, this::mouseDragged),
                new InputMap.MouseMapping(MouseEvent.MOUSE_MOVED, this::mouseMoved)
        );
    }

    public void setHeaderRegion(Region headerRegion) {
        this.headerRegion = headerRegion;
    }

    private void mouseDragged(MouseEvent mouseEvent) {
        if (currentEdge != null) {
            WindowHelper.getWindowAccessor().beginResizeDrag(stage, currentEdge, mouseEvent.getButton(),
                                                            lastScreenX, lastScreenY);
        } else if (headerRegion != null) {
            if (mouseInHeader()) {
                WindowHelper.getWindowAccessor().beginMoveDrag(stage, mouseEvent.getButton(), lastScreenX, lastScreenY);
            }
        }
    }

    private boolean mouseInHeader() {
        if (headerRegion == null) {
            return false;
        }

        Bounds boundsInLocal = headerRegion.localToScene(headerRegion.getBoundsInLocal());
        return boundsInLocal.contains(lastLocalX, lastLocalY);
    }

    private void mousePressed(MouseEvent mouseEvent) {
        this.lastScreenX = mouseEvent.getScreenX();
        this.lastScreenY = mouseEvent.getScreenY();
        this.lastLocalX = mouseEvent.getSceneX();
        this.lastLocalY = mouseEvent.getSceneY();
    }

    @Override
    public InputMap<SceneDecoration> getInputMap() {
        return inputMap;
    }

//    private void beginKeyboardResize() {
//        if (scene.getWindow() != null) {
//            double middleX = scene.getWindow().getX() + scene.getX() + (scene.getWidth() / 2);
//            double middleY = scene.getWindow().getY() + scene.getY() + (scene.getHeight() / 2);
//
//            scene.getWindow().beginResizeDrag(WindowEdge.BOTTOM, MouseButton.NONE, middleX, middleY);
//        }
//    }

    private void mouseMoved(MouseEvent e) {
        //mouse is on left side
        if (e.getX() <= RESIZE_THRESHOLD) {
            if (e.getY() >= scene.getHeight() - RESIZE_THRESHOLD) {
                setEdge(WindowEdge.SOUTH_WEST);
                return;
            }

            if (e.getY() <= RESIZE_THRESHOLD) {
                setEdge(WindowEdge.NORTH_WEST);
                return;
            }

            setEdge(WindowEdge.WEST);
        } else if (e.getX() >= scene.getWidth() - RESIZE_THRESHOLD) {
            if (e.getY() >= scene.getHeight() - RESIZE_THRESHOLD) {
                setEdge(WindowEdge.SOUTH_EAST);
                return;
            }

            if (e.getY() <= RESIZE_THRESHOLD) {
                setEdge(WindowEdge.NORTH_EAST);
                return;
            }

            setEdge(WindowEdge.EAST);
        } else if (e.getY() <= RESIZE_THRESHOLD) {
            setEdge(WindowEdge.NORTH);
        } else if (e.getY() >= scene.getHeight() - RESIZE_THRESHOLD) {
            setEdge(WindowEdge.SOUTH);
        } else {
            setEdge(null);
        }
    }

    private void setEdge(WindowEdge edge) {
        currentEdge = edge;
        stage.getScene().setCursor((edge == null) ? null : CURSOR_MAP.get(edge));
    }
}
