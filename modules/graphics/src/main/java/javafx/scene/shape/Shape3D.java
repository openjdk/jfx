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

package javafx.scene.shape;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.prism.NGShape3D;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Node;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import sun.util.logging.PlatformLogger;


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
 * Note that this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
 * for more information.
 * 
 * @since JavaFX 8.0
 */
public abstract class Shape3D extends Node {
    // NOTE: Need a way to specify shape tessellation resolution, may use metric relate to window resolution
    // Will not support dynamic refinement in FX8
    
    // TODO: 3D - May provide user convenient utility to compose images in a single image for shapes such as Box or Cylinder

    private static final PhongMaterial DEFAULT_MATERIAL = new PhongMaterial();
    
    protected Shape3D() {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            String logname = Shape3D.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                                                      + "ConditionalFeature.SCENE3D");
        }
    }

    PredefinedMeshManager manager = PredefinedMeshManager.getInstance();
    int key = 0;

    /**
     * Defines the material this {@code Shape3D}.
     * The default material is null. If {@code Material} is null, a PhongMaterial
     * with a diffuse color of Color.LIGHTGRAY is used for rendering.
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
   
    public final ObjectProperty<Material> materialProperty() {
        if (material == null) {
            material = new SimpleObjectProperty<Material>(Shape3D.this,
                    "material") {

                private Material old = null;
                private final ChangeListener<Boolean> materialChangeListener =
                        (observable, oldValue, newValue) -> {
                            if (newValue) {
                                impl_markDirty(DirtyBits.MATERIAL);
                            }
                        };
                private final WeakChangeListener<Boolean> weakMaterialChangeListener =
                        new WeakChangeListener(materialChangeListener);

                @Override protected void invalidated() {
                    if (old != null) {
                        old.impl_dirtyProperty().removeListener(weakMaterialChangeListener);
                    }
                    Material newMaterial = get();
                    if (newMaterial != null) {
                        newMaterial.impl_dirtyProperty().addListener(weakMaterialChangeListener);
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
     * Defines the cullFace this {@code Shape3D}.
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
    public void impl_updatePeer() {
        super.impl_updatePeer();
        final NGShape3D peer = impl_getPeer();
        if (impl_isDirty(DirtyBits.MATERIAL)) {
            Material mat = getMaterial() == null ? DEFAULT_MATERIAL : getMaterial();
            mat.impl_updatePG(); // new material should be updated
            peer.setMaterial(mat.impl_getNGMaterial());
        }
        if (impl_isDirty(DirtyBits.NODE_DRAWMODE)) {
            peer.setDrawMode(getDrawMode() == null ? DrawMode.FILL : getDrawMode());
        }
        if (impl_isDirty(DirtyBits.NODE_CULLFACE)) {
            peer.setCullFace(getCullFace() == null ? CullFace.BACK : getCullFace());
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
