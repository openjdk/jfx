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

package javafx.scene.shape;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.scene.shape.MeshHelper;
import com.sun.javafx.sg.prism.NGTriangleMesh;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import com.sun.javafx.logging.PlatformLogger;

/**
 * Base class for representing a 3D geometric surface.
 *
 * Note that this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
 * for more information.
 *
 * @since JavaFX 8.0
 */
public abstract class Mesh {

    /*
     * Store the singleton instance of the MeshHelper subclass corresponding
     * to the subclass of this instance of Mesh
     */
    private MeshHelper meshHelper = null;

    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        MeshHelper.setMeshAccessor(new MeshHelper.MeshAccessor() {
            @Override
            public MeshHelper getHelper(Mesh mesh) {
                return mesh.meshHelper;
            }

            @Override
            public void setHelper(Mesh mesh, MeshHelper meshHelper) {
                mesh.meshHelper = meshHelper;
            }
        });
    }

    /**
     * A constructor that is called by any {@code Mesh} implementation.
     */
    protected Mesh() {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            String logname = Mesh.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                                                      + "ConditionalFeature.SCENE3D");
        }
    }

    // Mesh isn't a Node. It can't use the standard dirtyBits pattern that is
    // in Node
    // TODO: 3D - Material and Mesh have similar pattern. We should look into creating
    // a "NodeComponent" class if more non-Node classes are needed.

    // Material isn't a Node. It can't use the standard dirtyBits pattern that is
    // in Node
    private final BooleanProperty dirty = new SimpleBooleanProperty(true);

    final boolean isDirty() {
        return dirty.getValue();
    }

    void setDirty(boolean value) {
        dirty.setValue(value);
    }

    final BooleanProperty dirtyProperty() {
        return dirty;
    }

    // We only support one type of mesh for FX 8.
    abstract NGTriangleMesh getPGMesh();
    abstract void updatePG();

    abstract BaseBounds computeBounds(BaseBounds b);

}
