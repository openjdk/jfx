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
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.sg.PGMeshView;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * The {@code MeshView} class defines a surface with the specified 3D
 * mesh data.
 *
 * @since JavaFX 8    
 */
public class MeshView extends Shape3D {

    /**
     * Creates a new instance of {@code MeshView} class.
     */
    public MeshView() {
    }

    /**
     * Creates a new instance of {@code MeshView} class with the specified {@code Mesh}
     * surface.
     */
    public MeshView(Mesh mesh) {
        setMesh(mesh);
    }

    /**
     * Specifies the 3D mesh data of this {@code MeshView}.
     *
     * @defaultValue null
     */
    private ObjectProperty<Mesh> mesh;

    public final void setMesh(Mesh value) {
        meshProperty().set(value);
    }

    public final Mesh getMesh() {
        return mesh == null ? null : mesh.get();
    }

    private final ChangeListener<Boolean> meshListener = new ChangeListener<Boolean>() {
        @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue) {
                impl_markDirty(DirtyBits.MESH_GEOM);
                impl_geomChanged();
            }
        }
    };

    public final ObjectProperty<Mesh> meshProperty() {
        if (mesh == null) {
            mesh = new SimpleObjectProperty<Mesh>(MeshView.this, "mesh") {
                private Mesh old = null;

                @Override
                protected void invalidated() {
                    if (old != null) {
                        old.dirtyProperty().removeListener(meshListener);
                    }
                    Mesh newMesh = get();
                    if (newMesh != null) {
                        newMesh.dirtyProperty().addListener(meshListener);
                    }
                    impl_markDirty(DirtyBits.MESH);
                    impl_markDirty(DirtyBits.MESH_GEOM);
                    impl_geomChanged();
                    old = newMesh;
                }
            };
        }
        return mesh;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_updatePG() {
        super.impl_updatePG();
        PGMeshView pgMeshView = (PGMeshView)impl_getPGNode();
        if (impl_isDirty(DirtyBits.MESH_GEOM) && getMesh() != null) {
            getMesh().impl_updatePG();
        }
        if (impl_isDirty(DirtyBits.MESH)) {
            pgMeshView.setMesh((getMesh() == null) ? null : getMesh().getPGMesh());
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGMeshView();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        bounds = mesh.get().computeBounds(bounds);
        bounds = tx.transform(bounds, bounds);
        return bounds;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Override
    @Deprecated
    protected boolean impl_computeIntersects(PickRay pickRay, PickResultChooser pickResult) {
        return getMesh().impl_computeIntersects(pickRay, pickResult, this, getCullFace(), true);
    }

}
