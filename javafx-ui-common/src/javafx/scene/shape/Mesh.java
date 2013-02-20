/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.sg.PGTriangleMesh;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;

/**
 * Base class for representing a 3D geometric surface.
 * 
 * @since JavaFX 8
 */
public abstract class Mesh {

    protected Mesh() {
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
    abstract PGTriangleMesh getPGMesh();
    abstract void impl_updatePG();

    abstract BaseBounds computeBounds(BaseBounds b);

    /**
     * Picking implementation.
     * @param pickRay The pick ray
     * @param pickResult The pick result to be updated (if a closer intersection is found)
     * @param candidate The Node that owns this mesh to be filled in the pick
     *                  result in case a closer intersection is found
     * @param cullFace The cull face of the node that owns this mesh
     * @param reportFace Whether to report the hit face
     * @return true if the pickRay intersects this mesh (regardless of whether
     *              the pickResult has been updated)
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    abstract protected boolean impl_computeIntersects(PickRay pickRay,
            PickResultChooser pickResult, Node candidate, CullFace cullFace,
            boolean reportFace);
}
