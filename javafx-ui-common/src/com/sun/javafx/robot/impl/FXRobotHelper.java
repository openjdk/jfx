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
 */
package com.sun.javafx.robot.impl;

import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import com.sun.javafx.robot.FXRobotImage;
import javafx.scene.input.ScrollEvent;

/**
 * Utility class class used for accessing certain implementation-specific
 * runtime functionality.
 *
 */
public class FXRobotHelper {

    static FXRobotInputAccessor inputAccessor;
    static FXRobotSceneAccessor sceneAccessor;
    static FXRobotStageAccessor stageAccessor;
    static FXRobotImageConvertor imageConvertor;

    /**
     * Returns a ObservableList containing this passed parent's children.
     *
     * Note that application must use/reference javafx.scene.Scene class prior to
     * using this method (for example, by creating a scene).
     *
     * @param p Parent subclass to get children for
     * @return ObservableList containing this parent's children
     */
    public static ObservableList<Node> getChildren(Parent p) {
        if (sceneAccessor == null) {
            // TODO: force scene initialization
        }
        return sceneAccessor.getChildren(p);
    }

    /**
     * Returns a ObservableList containing {@code Stage}s created at this point.
     *
     * Note that application must use/reference javafx.stage.Stage class prior to
     * using this method (for example, by creating a Stage).
     *
     * @return ObservableList containing existing stages
     */
    public static ObservableList<Stage> getStages() {
        if (stageAccessor == null) {
            // TODO: force stage initialization
        }
        return stageAccessor.getStages();
    }

    /**
     * Converts passed integer in IntArgb pixel format to Color.
     * @return Color object
     */
    public static Color argbToColor(int argb) {
        int a = argb >> 24;
        a = a & 0xff;
        float aa = ((float)a) / 255f;

        int r = argb >> 16;
        r = r & 0xff;

        int g = argb >> 8;
        g = g & 0xff;

        int b = argb;
        b = b & 0xff;

        return Color.rgb(r, g, b, aa);
    }

    /**
     * @treatAsPrivate implementation detail
     */
    public static void setInputAccessor(FXRobotInputAccessor a) {
        if (inputAccessor != null) {
            System.out.println("Warning: Input accessor is already set: " + inputAccessor);
            Thread.dumpStack();
        }
        inputAccessor = a;
    }

    /**
     * @treatAsPrivate implementation detail
     */
    public static void setSceneAccessor(FXRobotSceneAccessor a) {
        if (sceneAccessor != null) {
            System.out.println("Warning: Scene accessor is already set: " + sceneAccessor);
            Thread.dumpStack();
        }
        sceneAccessor = a;
    }

    /**
     * @treatAsPrivate implementation detail
     */
    public static void setImageConvertor(FXRobotImageConvertor ic) {
        if (imageConvertor != null) {
            System.out.println("Warning: Image convertor is already set: " + imageConvertor);
            Thread.dumpStack();
        }
        imageConvertor = ic;
    }

    /**
     * @treatAsPrivate implementation detail
     */
    public static void setStageAccessor(FXRobotStageAccessor a) {
        if (stageAccessor != null) {
            System.out.println("Warning: Stage accessor already set: " + stageAccessor);
            Thread.dumpStack();
        }
        stageAccessor = a;
    }

    /**
     * @treatAsPrivate implementation detail
     */
    public static abstract class FXRobotStageAccessor {
          public abstract ObservableList<Stage> getStages();
    }

    /**
     * @treatAsPrivate implementation detail
     */
    public static abstract class FXRobotImageConvertor {
          public abstract FXRobotImage
              convertToFXRobotImage(Object platformImage);
    }

    /**
     * @treatAsPrivate implementation detail
     */
    public static abstract class FXRobotInputAccessor {
        public abstract int getCodeForKeyCode(KeyCode keyCode);
        public abstract KeyCode getKeyCodeForCode(int code);
        public abstract KeyEvent createKeyEvent(
                                EventType<? extends KeyEvent> eventType,
                                KeyCode keyCode, String keyChar, String keyText,
                                boolean shiftDown, boolean controlDown,
                                boolean altDown, boolean metaDown);
        public abstract MouseEvent createMouseEvent(
                                EventType<? extends MouseEvent> eventType,
                                int x, int y,
                                int screenX, int screenY,
                                MouseButton button,
                                int clickCount,
                                boolean shiftDown,
                                boolean controlDown,
                                boolean altDown,
                                boolean metaDown,
                                boolean popupTrigger,
                                boolean primaryButtonDown,
                                boolean middleButtonDown,
                                boolean secondaryButtonDown);
        public abstract ScrollEvent createScrollEvent(
                                EventType<? extends ScrollEvent> eventType,
                                int scrollX, int scrollY,
                                ScrollEvent.HorizontalTextScrollUnits xTextUnits,
                                int xText,
                                ScrollEvent.VerticalTextScrollUnits yTextUnits,
                                int yText,
                                int x, int y,
                                int screenX, int screenY,
                                boolean shiftDown,
                                boolean controlDown,
                                boolean altDown,
                                boolean metaDown);
    }

    /**
     * @treatAsPrivate implementation detail
     */
    public static abstract class FXRobotSceneAccessor {
        public abstract void processKeyEvent(Scene scene, KeyEvent keyEvent);
        public abstract void processMouseEvent(Scene scene, MouseEvent mouseEvent);
        public abstract void processScrollEvent(Scene scene, ScrollEvent scrollEvent);
        public abstract ObservableList<Node> getChildren(Parent parent);
        public abstract Object renderToImage(Scene scene, Object platformImage);
    }
}
