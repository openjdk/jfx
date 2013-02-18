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
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGPhongMaterial;
import com.sun.javafx.sg.PGShape3D;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.paint.Material;


/**
 * The {@code Shape3D} base class provides definitions of common properties for
 * objects that represent some form of 3D geometric shape.  These properties
 * include:
 * <ul>
 * <li>The {@link Material} to be applied to the fillable interior of the
 * shape or the outline of the shape (see {@link #setMaterial}).
 * <li>The draw model properties that defines how to render its geometry (see {@link #setDrawMode}).
 * <li>The face culling properties that defines which face to cull (see {@link #setCullFace}).
 * </ul>
 *
 * @since JavaFX 8
 */
public abstract class Shape3D extends Node {
    // NOTE: Need a way to specify shape tessellation resolution, may use metric relate to window resolution
    // Will not support dynamic refinement in FX8
    
    // TODO: 3D - May provide user convenient utility to compose images in a single image for shapes such as Box or Cylinder

    protected Shape3D() {
    }

    PredefinedMeshManager manager = PredefinedMeshManager.getInstance();
    int key = 0;

    /**
     * Defines the material this {@code Shape3D}.
     * The default material is null. XXX Info about null Material XXX
     *
     * @defaultValue null
     */
    private ObjectProperty<Material> material;

    public final void setMaterial(Material value) {
        materialProperty().set(value);
    }

    public final Material getMaterial() {
        return material == null ? null : material.get();
    }

    private final ChangeListener<Boolean> materialListener = new ChangeListener<Boolean>() {
        @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue) {
                impl_markDirty(DirtyBits.MATERIAL_PROPERTY);
                //impl_geomChanged();
            }
        }
    };
 
    public final ObjectProperty<Material> materialProperty() {
        if (material == null) {
            material = new SimpleObjectProperty<Material>(Shape3D.this,
                    "material") {

                private Material old = null;
                @Override protected void invalidated() {
                    if (old != null) {
                        old.impl_dirtyProperty().removeListener(materialListener);
                    }
                    Material newMaterial = get();
                    if (newMaterial != null) {
                        newMaterial.impl_dirtyProperty().addListener(materialListener);
                    }
                    impl_markDirty(DirtyBits.MATERIAL);
                    impl_geomChanged();
                    old = newMaterial;
                }
            };
        }
        return material;
    }

    /**
     * Defines the drawMode this {@code Shape3D}.
     *
     * @defaultValue DrawMode.FILL
     */
    private ObjectProperty<DrawMode> drawMode;

    public final void setDrawMode(DrawMode value) {
        drawModeProperty().set(value);
    }

    public final DrawMode getDrawMode() {
        return drawMode == null ? DrawMode.FILL : drawMode.get();
    }

    public final ObjectProperty<DrawMode> drawModeProperty() {
        if (drawMode == null) {
            drawMode = new SimpleObjectProperty<DrawMode>(Shape3D.this,
                    "drawMode", DrawMode.FILL) {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_DRAWMODE);
                }
            };
        }
        return drawMode;
    }  

    /**
     * Defines the drawMode this {@code Shape3D}.
     *
     * @defaultValue CullFace.BACK
     */
    private ObjectProperty<CullFace> cullFace;

    public final void setCullFace(CullFace value) {
        cullFaceProperty().set(value);
    }

    public final CullFace getCullFace() {
        return cullFace == null ? CullFace.BACK : cullFace.get();
    }

    public final ObjectProperty<CullFace> cullFaceProperty() {
        if (cullFace == null) {
            cullFace = new SimpleObjectProperty<CullFace>(Shape3D.this,
                    "cullFace", CullFace.BACK) {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_CULLFACE);
                }
            };
        }
        return cullFace;
    }  

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        // TODO: 3D - Evaluate this logic
        return new BoxBounds(0, 0, 0, 0, 0, 0);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        return false;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();

        // TODO: 3D - Why do we have separate dirty bits for MATERIAL
        // and MATERIAL_PROPERTY ?
        Material material = getMaterial();
        if (impl_isDirty(DirtyBits.MATERIAL_PROPERTY) && material != null) {
            material.impl_updatePG();
        }
        PGShape3D pgShape3D = (PGShape3D) impl_getPGNode();
        if (impl_isDirty(DirtyBits.MATERIAL)) {
            if (material != null) {
                material.impl_updatePG(); // new material should be updated
                pgShape3D.setMaterial((PGPhongMaterial) material.impl_getPGMaterial());
            } else {
                pgShape3D.setMaterial(null);
            }
        }
        if (impl_isDirty(DirtyBits.NODE_DRAWMODE)) {
            pgShape3D.setDrawMode(getDrawMode());
        }
        if (impl_isDirty(DirtyBits.NODE_CULLFACE)) {
            pgShape3D.setCullFace(getCullFace());
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
