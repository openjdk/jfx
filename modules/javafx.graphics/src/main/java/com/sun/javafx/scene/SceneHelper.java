/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene;

import com.sun.glass.ui.Accessible;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.util.Utils;
import javafx.scene.Camera;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

/**
 * Used to access internal scene methods.
 */
public final class SceneHelper {
    private static SceneAccessor sceneAccessor;

    static {
        Utils.forceInit(Scene.class);
    }

    private SceneHelper() {
    }

    public static void enableInputMethodEvents(Scene scene, boolean enable) {
        sceneAccessor.enableInputMethodEvents(scene, enable);
    }

    public static void processKeyEvent(Scene scene, KeyEvent e) {
        sceneAccessor.processKeyEvent(scene, e);
    }

    public static void processMouseEvent(Scene scene, MouseEvent e) {
        sceneAccessor.processMouseEvent(scene, e);
    }

    public static void preferredSize(Scene scene) {
        sceneAccessor.preferredSize(scene);
    }

    public static void disposePeer(Scene scene) {
        sceneAccessor.disposePeer(scene);
    }

    public static void initPeer(Scene scene) {
        sceneAccessor.initPeer(scene);
    }

    public static void setWindow(Scene scene, Window window) {
        sceneAccessor.setWindow(scene, window);
    }

    public static TKScene getPeer(Scene scene) {
        return sceneAccessor.getPeer(scene);
    }

    public static void setAllowPGAccess(boolean flag) {
        sceneAccessor.setAllowPGAccess(flag);
    }

    public static void parentEffectiveOrientationInvalidated(
            final Scene scene) {
        sceneAccessor.parentEffectiveOrientationInvalidated(scene);
    }

    public static Camera getEffectiveCamera(final Scene scene) {
        return sceneAccessor.getEffectiveCamera(scene);
    }

    public static Scene createPopupScene(final Parent root) {
        return sceneAccessor.createPopupScene(root);
    }

    public static Accessible getAccessible(Scene scene) {
        return sceneAccessor.getAccessible(scene);
    }

    public static void setSceneAccessor(final SceneAccessor newAccessor) {
        if (sceneAccessor != null) {
            throw new IllegalStateException();
        }

        sceneAccessor = newAccessor;
    }

    public static SceneAccessor getSceneAccessor() {
        if (sceneAccessor == null) throw new IllegalStateException();
        return sceneAccessor;
    }

    public interface SceneAccessor {
        void enableInputMethodEvents(Scene scene, boolean enable);

        void processKeyEvent(Scene scene, KeyEvent e);

        void processMouseEvent(Scene scene, MouseEvent e);

        void preferredSize(Scene scene);

        void disposePeer(Scene scene);

        void initPeer(Scene scene);

        void setWindow(Scene scene, Window window);

        TKScene getPeer(Scene scene);

        void setAllowPGAccess(boolean flag);

        void parentEffectiveOrientationInvalidated(Scene scene);

        Camera getEffectiveCamera(Scene scene);

        Scene createPopupScene(Parent root);

        void setTransientFocusContainer(Scene scene, Node node);

        Accessible getAccessible(Scene scene);
    }

}
