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
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.sg.PGBox;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.input.PickResult;

/**
 * The {@code Box} class defines a 3 dimensional box with the specified size.
 * A {@code Box} is a 3D geometry primitive created with a given depth, width,
 * and height. It is centered at the origin.
 *
 * @since JavaFX 8    
 */
public class Box extends Shape3D {

    private TriangleMesh mesh;

    /**
     * Creates a new instance of {@code Box} of dimension 2 by 2 by 2.
     */
    
    public static final double DEFAULT_SIZE = 2;
    
    public Box() {
        this(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    /**
     * Creates a new instance of {@code Box} of dimension width by height 
     * by depth.
     */
    public Box(double width, double height, double depth) {
        setWidth(width);
        setHeight(height);
        setDepth(depth);
    }
    
    /**
     * Defines the depth or the Z dimension of the Box.
     *
     * @defaultValue 2.0
     */
    private DoubleProperty depth;

    public final void setDepth(double value) {
        depthProperty().set(value);
    }

    public final double getDepth() {
        return depth == null ? 2 : depth.get();
    }

    public final DoubleProperty depthProperty() {
        if (depth == null) {
            depth = new SimpleDoubleProperty(Box.this, "depth", DEFAULT_SIZE) {
                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.MESH_GEOM);
                    manager.invalidateBoxMesh(key);
                    key = 0;
                }
            };
        }
        return depth;
    }

    /**
     * Defines the height or the Y dimension of the Box.
     *
     * @defaultValue 2.0
     */
    private DoubleProperty height;

    public final void setHeight(double value) {
        heightProperty().set(value);
    }

    public final double getHeight() {
        return height == null ? 2 : height.get();
    }

    public final DoubleProperty heightProperty() {
        if (height == null) {
            height = new SimpleDoubleProperty(Box.this, "height", DEFAULT_SIZE) {
                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.MESH_GEOM);
                    manager.invalidateBoxMesh(key);
                    key = 0;
                }
            };
        }
        return height;
    }

    /**
     * Defines the width or the X dimension of the Box.
     *
     * @defaultValue 2.0
     */
    private DoubleProperty width;

    public final void setWidth(double value) {
        widthProperty().set(value);
    }

    public final double getWidth() {
        return width == null ? 2 : width.get();
    }

    public final DoubleProperty widthProperty() {
        if (width == null) {
            width = new SimpleDoubleProperty(Box.this, "width", DEFAULT_SIZE) {
                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.MESH_GEOM);
                    manager.invalidateBoxMesh(key);
                    key = 0;
                }
            };
        }
        return width;
    }
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGBox();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_updatePG() {
        super.impl_updatePG();
        if (impl_isDirty(DirtyBits.MESH_GEOM)) {
            PGBox pgBox = (PGBox) impl_getPGNode();
            if (key == 0) {
                key = generateKey((float) getWidth(), 
                                  (float) getHeight(),
                                  (float) getDepth());
            }
            mesh = manager.getBoxMesh((float) getWidth(), 
                                      (float) getHeight(), 
                                      (float) getDepth(),
                                      key);
            mesh.impl_updatePG();
            pgBox.updateMesh(mesh.impl_getPGTriangleMesh());
        }
    }
    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        float hw = (float) getWidth() * 0.5f;
        float hh = (float) getHeight() * 0.5f;
        float hd = (float) getDepth() * 0.5f;
        
        bounds = bounds.deriveWithNewBounds(-hw, -hh, -hd, hw, hh, hd);
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
        double w = getWidth();
        double h = getHeight();
        return -w <= localX && localX <= w && 
                -h <= localY && localY <= h;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_computeIntersects(PickRay pickRay, PickResultChooser pickResult) {

        final double w = getWidth();
        final double h = getHeight();
        final double d = getDepth();
        final double hWidth = w / 2.0;
        final double hHeight = h / 2.0;
        final double hDepth = d / 2.0;
        final Vec3d dir = pickRay.getDirectionNoClone();
        final double invDirX = dir.x == 0 ? Double.POSITIVE_INFINITY : (1.0 / dir.x);
        final double invDirY = dir.y == 0 ? Double.POSITIVE_INFINITY : (1.0 / dir.y);
        final double invDirZ = dir.z == 0 ? Double.POSITIVE_INFINITY : (1.0 / dir.z);
        final Vec3d origin = pickRay.getOriginNoClone();
        final double originX = origin.x;
        final double originY = origin.y;
        final double originZ = origin.z;
        final boolean signX = invDirX < 0.0;
        final boolean signY = invDirY < 0.0;
        final boolean signZ = invDirZ < 0.0;

        double t0 = ((signX ? hWidth : -hWidth) - originX) * invDirX;
        double t1 = ((signX ? -hWidth : hWidth) - originX) * invDirX;
        char side0 = 'x';
        char side1 = 'x';

        final double ty0 = ((signY ? hHeight : -hHeight) - originY) * invDirY;
        final double ty1 = ((signY ? -hHeight : hHeight) - originY) * invDirY;

        if ((t0 > ty1) || (ty0 > t1)) {
            return false;
        }
        if (ty0 > t0) {
            side0 = 'y';
            t0 = ty0;
        }
        if (ty1 < t1) {
            side1 = 'y';
            t1 = ty1;
        }

        double tz0 = ((signZ ? hDepth : -hDepth) - originZ) * invDirZ;
        double tz1 = ((signZ ? -hDepth : hDepth) - originZ) * invDirZ;

        if ((t0 > tz1) || (tz0 > t1)) {
            return false;
        }
        if (tz0 > t0) {
            side0 = signZ ? 'Z' : 'z';
            t0 = tz0;
        }
        if (tz1 < t1) {
            side1 = signZ ? 'z' : 'Z';
            t1 = tz1;
        }

        char side = side0;
        double t = t0;
        final CullFace cullFace = getCullFace();
        if (t0 < 0.0 || cullFace == CullFace.FRONT) {
            if (t1 >= 0.0 && cullFace != CullFace.BACK) {
                side = side1;
                t = t1;
            } else {
                return false;
            }
        }

        if (pickResult.isCloser(t)) {
            Point3D point = PickResultChooser.computePoint(pickRay, t);

            Point2D txtCoords = null;

            if (side == 'x') {
                txtCoords = new Point2D(
                        0.5 - point.getY() / h,
                        0.5 - point.getZ() / d);
            } else if (side == 'y') {
                txtCoords = new Point2D(
                        0.5 - point.getX() / w,
                        0.5 - point.getZ() / d);
            } else if (side == 'z') {
                txtCoords = new Point2D(
                        0.5 - point.getX() / w,
                        0.5 - point.getY() / h);
            } else if (side == 'Z') {
                txtCoords = new Point2D(
                        0.5 - point.getY() / h,
                        0.5 - point.getX() / w);
            }

            pickResult.offer(this, t, PickResult.FACE_UNDEFINED, point, txtCoords);
        }
        
        return true;
    }

    static TriangleMesh createMesh(float w, float h, float d) {

        if (w * h * d == 0) {
            return null;
        }

        float hw = w / 2f;
        float hh = h / 2f;
        float hd = d / 2f;

        float points[] = {
            hw, hh, hd, hw, hh, -hd, hw, -hh, hd, hw, -hh, -hd,
            -hw, hh, hd, -hw, hh, -hd, -hw, -hh, hd, -hw, -hh, -hd };

        float texCoords[] = {0, 0, 0, 1, 1, 0, 1, 1};

        int faceSmoothingGroups[] = {
            1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4
        };

        int faces[] = {
            0, 0, 2, 2, 1, 1, 2, 2, 3, 3, 1, 1, 4, 0, 5, 1, 6, 2, 6, 2, 5, 1, 7, 3,
            0, 0, 1, 1, 4, 2, 4, 2, 1, 1, 5, 3, 2, 0, 6, 2, 3, 1, 3, 1, 6, 2, 7, 3,
            0, 0, 4, 1, 2, 2, 2, 2, 4, 1, 6, 3, 1, 0, 3, 1, 5, 2, 5, 2, 3, 1, 7, 3,};

        TriangleMesh mesh = new TriangleMesh(points, texCoords, faces);
        mesh.setFaceSmoothingGroups(faceSmoothingGroups);

        return mesh;
    }

    private static int generateKey(float w, float h, float d) {
        int hash = 3;
        hash = 97 * hash + Float.floatToIntBits(w);
        hash = 97 * hash + Float.floatToIntBits(h);
        hash = 97 * hash + Float.floatToIntBits(d);
        return hash;
    }
}
