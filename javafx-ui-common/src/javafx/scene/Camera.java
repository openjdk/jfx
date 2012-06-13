/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

import com.sun.javafx.geom.CameraImpl;


/**
 * Base class for a camera used to render a scene.
 *
 * @since JavaFX 1.3
 */
public abstract class Camera {

    private CameraImpl platformCamera;

    CameraImpl getPlatformCamera() {
        if (platformCamera == null) {
            platformCamera = createPlatformCamera();
        }
        return platformCamera;
    }

    abstract CameraImpl createPlatformCamera();

    abstract void update();
    private BooleanProperty dirty = new SimpleBooleanProperty(this, "dirty");

    final ObservableBooleanValue dirtyProperty() { return dirty; }

    final boolean isDirty() {
        return dirty.get();
    }

    final void markDirty() {
        dirty.set(true);
    }

    final void clearDirty() {
        dirty.set(false);
    }

    Camera copy() {
        return this;
    }
}
