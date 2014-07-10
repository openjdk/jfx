/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Camera;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Used to access internal scene methods.
 */
public final class SceneHelper {
    private static SceneAccessor sceneAccessor;

    static {
        forceInit(Scene.class);
    }

    private SceneHelper() {
    }

    public static void setPaused(final boolean paused) {
        sceneAccessor.setPaused(paused);
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
        void setPaused(boolean paused);

        void parentEffectiveOrientationInvalidated(Scene scene);

        Camera getEffectiveCamera(Scene scene);

        Scene createPopupScene(Parent root);

        void setTransientFocusContainer(Scene scene, Node node);
    }

    private static void forceInit(final Class<?> classToInit) {
        try {
            Class.forName(classToInit.getName(), true,
                          classToInit.getClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new AssertionError(e);  // Can't happen
        }
    }
}
