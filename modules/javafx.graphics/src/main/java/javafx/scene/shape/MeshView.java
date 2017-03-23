/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.shape.MeshHelper;
import com.sun.javafx.scene.shape.MeshViewHelper;
import com.sun.javafx.sg.prism.NGMeshView;
import com.sun.javafx.sg.prism.NGNode;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Node;

/**
 * The {@code MeshView} class defines a surface with the specified 3D
 * mesh data.
 *
 * @since JavaFX 8.0
 */
public class MeshView extends Shape3D {
    static {
         // This is used by classes in different packages to get access to
         // private and package private methods.
        MeshViewHelper.setMeshViewAccessor(new MeshViewHelper.MeshViewAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((MeshView) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((MeshView) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((MeshView) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((MeshView) node).doComputeContains(localX, localY);
            }

            @Override
            public boolean doComputeIntersects(Node node, PickRay pickRay,
                    PickResultChooser pickResult) {
                return ((MeshView) node).doComputeIntersects(pickRay, pickResult);
            }
        });
    }

    {
        // To initialize the class helper at the begining each constructor of this class
        MeshViewHelper.initHelper(this);
    }

    /**
     * Creates a new instance of {@code MeshView} class.
     */
    public MeshView() {
    }

    /**
     * Creates a new instance of {@code MeshView} class with the specified {@code Mesh}
     * surface.
     * @param mesh the mesh surface
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

    public final ObjectProperty<Mesh> meshProperty() {
        if (mesh == null) {
            mesh = new SimpleObjectProperty<Mesh>(MeshView.this, "mesh") {

                private Mesh old = null;
                private final ChangeListener<Boolean> meshChangeListener =
                        (observable, oldValue, newValue) -> {
                            if (newValue) {
                                NodeHelper.markDirty(MeshView.this, DirtyBits.MESH_GEOM);
                                NodeHelper.geomChanged(MeshView.this);
                            }
                        };
                private final WeakChangeListener<Boolean> weakMeshChangeListener =
                        new WeakChangeListener(meshChangeListener);

                @Override
                protected void invalidated() {
                    if (old != null) {
                        old.dirtyProperty().removeListener(weakMeshChangeListener);
                    }
                    Mesh newMesh = get();
                    if (newMesh != null) {
                        newMesh.dirtyProperty().addListener(weakMeshChangeListener);
                    }
                    NodeHelper.markDirty(MeshView.this, DirtyBits.MESH);
                    NodeHelper.markDirty(MeshView.this, DirtyBits.MESH_GEOM);
                    NodeHelper.geomChanged(MeshView.this);
                    old = newMesh;
                }
            };
        }
        return mesh;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        NGMeshView peer = NodeHelper.getPeer(this);
        if (NodeHelper.isDirty(this, DirtyBits.MESH_GEOM) && getMesh() != null) {
            getMesh().updatePG();
        }
        if (NodeHelper.isDirty(this, DirtyBits.MESH)) {
            peer.setMesh((getMesh() == null) ? null : getMesh().getPGMesh());
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGMeshView();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        if (getMesh() != null) {
            bounds = getMesh().computeBounds(bounds);
            bounds = tx.transform(bounds, bounds);
        } else {
            bounds.makeEmpty();
        }
        return bounds;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeIntersects(PickRay pickRay, PickResultChooser pickResult) {
        return MeshHelper.computeIntersects(getMesh(), pickRay, pickResult, this, getCullFace(), true);
    }

}
