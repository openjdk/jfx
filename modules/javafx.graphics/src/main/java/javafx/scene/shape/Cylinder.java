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
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.shape.CylinderHelper;
import com.sun.javafx.scene.shape.MeshHelper;
import com.sun.javafx.sg.prism.NGCylinder;
import com.sun.javafx.sg.prism.NGNode;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.PickResult;
import javafx.scene.transform.Rotate;

/**
 * The {@code Cylinder} class defines a 3 dimensional cylinder with the specified size.
 * A {@code Cylinder} is a 3D geometry primitive created with a given radius and height.
 * It is centered at the origin.
 *
 * @since JavaFX 8.0
 */
public class Cylinder extends Shape3D {
    static {
         // This is used by classes in different packages to get access to
         // private and package private methods.
        CylinderHelper.setCylinderAccessor(new CylinderHelper.CylinderAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((Cylinder) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((Cylinder) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((Cylinder) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((Cylinder) node).doComputeContains(localX, localY);
            }

            @Override
            public boolean doComputeIntersects(Node node, PickRay pickRay,
                    PickResultChooser pickResult) {
                return ((Cylinder) node).doComputeIntersects(pickRay, pickResult);
            }
        });
    }
    static final int DEFAULT_DIVISIONS = 64;
    static final double DEFAULT_RADIUS = 1;
    static final double DEFAULT_HEIGHT = 2;

    private int divisions = DEFAULT_DIVISIONS;
    private TriangleMesh mesh;

    {
        // To initialize the class helper at the begining each constructor of this class
        CylinderHelper.initHelper(this);
    }
    /**
     * Creates a new instance of {@code Cylinder} of radius of 1.0 and height of 2.0.
     * Resolution defaults to 15 divisions along X and Z axis.
     */
    public Cylinder() {
        this(DEFAULT_RADIUS, DEFAULT_HEIGHT, DEFAULT_DIVISIONS);
    }

    /**
     * Creates a new instance of {@code Cylinder} of a given radius and height.
     * Resolution defaults to 15 divisions along X and Z axis.
     *
     * @param radius Radius
     * @param height Height
     */
    public Cylinder (double radius, double height) {
        this(radius, height, DEFAULT_DIVISIONS);
    }

    /**
     * Creates a new instance of {@code Cylinder} of a given radius, height, and
     * divisions. Resolution defaults to 15 divisions along X and Z axis.
     *
     * Note that divisions should be at least 3. Any value less than that will be
     * clamped to 3.
     *
     * @param radius Radius
     * @param height Height
     * @param divisions Divisions
     */
    public Cylinder (double radius, double height, int divisions) {
        this.divisions = divisions < 3 ? 3 : divisions;
        setRadius(radius);
        setHeight(height);
    }

    /**
     * Defines the height or the Y dimension of the Cylinder.
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
            height = new SimpleDoubleProperty(Cylinder.this, "height", DEFAULT_HEIGHT) {
                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Cylinder.this, DirtyBits.MESH_GEOM);
                    manager.invalidateCylinderMesh(key);
                    key = null;
                    NodeHelper.geomChanged(Cylinder.this);
                }
            };
        }
        return height;
    }

    /**
     * Defines the radius in the Z plane of the Cylinder.
     *
     * @defaultValue 1.0
     */
    private DoubleProperty radius;

    public final void setRadius(double value) {
        radiusProperty().set(value);
    }

    public final double getRadius() {
        return radius == null ? 1 : radius.get();
    }

    public final DoubleProperty radiusProperty() {
        if (radius == null) {
            radius = new SimpleDoubleProperty(Cylinder.this, "radius", DEFAULT_RADIUS) {
                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Cylinder.this, DirtyBits.MESH_GEOM);
                    manager.invalidateCylinderMesh(key);
                    key = null;
                    NodeHelper.geomChanged(Cylinder.this);
                }
            };
        }
        return radius;
    }

    /**
     * Retrieves the divisions attribute use to generate this cylinder.
     *
     * @return the divisions attribute.
     */
    public int getDivisions() {
        return divisions;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        if (NodeHelper.isDirty(this, DirtyBits.MESH_GEOM)) {
            final NGCylinder peer = NodeHelper.getPeer(this);
            final float h = (float) getHeight();
            final float r = (float) getRadius();
            if (h < 0 || r < 0) {
                peer.updateMesh(null);
            } else {
                if (key == null) {
                    key = new CylinderKey(h, r, divisions);
                }
                mesh = manager.getCylinderMesh(h, r, divisions, key);
                mesh.updatePG();
                peer.updateMesh(mesh.getPGTriangleMesh());
            }
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGCylinder();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        final float h = (float) getHeight();
        final float r = (float) getRadius();

        if (r < 0 || h < 0) {
            return bounds.makeEmpty();
        }

        final float hh = h * 0.5f;

        bounds = bounds.deriveWithNewBounds(-r, -hh, -r, r, hh, r);
        bounds = tx.transform(bounds, bounds);
        return bounds;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        double w = getRadius();
        double hh = getHeight()*.5f;
        return -w <= localX && localX <= w &&
                -hh <= localY && localY <= hh;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeIntersects(PickRay pickRay, PickResultChooser pickResult) {

        final boolean exactPicking = divisions < DEFAULT_DIVISIONS && mesh != null;

        final double r = getRadius();
        final Vec3d dir = pickRay.getDirectionNoClone();
        final double dirX = dir.x;
        final double dirY = dir.y;
        final double dirZ = dir.z;
        final Vec3d origin = pickRay.getOriginNoClone();
        final double originX = origin.x;
        final double originY = origin.y;
        final double originZ = origin.z;
        final double h = getHeight();
        final double halfHeight = h / 2.0;
        final CullFace cullFace = getCullFace();

        // Check the open cylinder first

        // Coeficients of a quadratic equation desribing intersection with an infinite cylinder
        final double a = dirX * dirX + dirZ * dirZ;
        final double b = 2 * (dirX * originX + dirZ * originZ);
        final double c = originX * originX + originZ * originZ - r * r;

        final double discriminant = b * b - 4 * a * c;

        double t0, t1, t = Double.POSITIVE_INFINITY;
        final double minDistance = pickRay.getNearClip();
        final double maxDistance = pickRay.getFarClip();

        if (discriminant >= 0 && (dirX != 0.0 || dirZ != 0.0)) {
            // the line hits the infinite cylinder

            final double distSqrt = Math.sqrt(discriminant);
            final double q = (b < 0) ? (-b - distSqrt) / 2.0 : (-b + distSqrt) / 2.0;

            t0 = q / a;
            t1 = c / q;

            if (t0 > t1) {
                double temp = t0;
                t0 = t1;
                t1 = temp;
            }

            // let's see if the hit is between clipping planes and within the cylinder's height
            final double y0 = originY + t0 * dirY;
            if (t0 < minDistance || y0 < -halfHeight || y0 > halfHeight || cullFace == CullFace.FRONT) {
                final double y1 = originY + t1 * dirY;
                if (t1 >= minDistance && t1 <= maxDistance && y1 >= -halfHeight && y1 <= halfHeight) {
                    if (cullFace != CullFace.BACK || exactPicking) {
                        // t0 is outside or behind but t1 hits.

                        // We need to do the exact picking even if the back wall
                        // is culled because the front facing triangles may
                        // still be in front of us
                        t = t1;
                    }
                } // else no hit (but we need to check the caps)
            } else if (t0 <= maxDistance) {
                // t0 hits the height between clipping planes
                t = t0;
            } // else no hit (but we need to check the caps)
        }

        // Now check the caps

        // if we already know we are going to do the exact picking,
        // there is no need to check the caps

        boolean topCap = false, bottomCap = false;
        if (t == Double.POSITIVE_INFINITY || !exactPicking) {
            final double tBottom = (-halfHeight - originY) / dirY;
            final double tTop = (halfHeight - originY) / dirY;
            boolean isT0Bottom = false;

            if (tBottom < tTop) {
                t0 = tBottom;
                t1 = tTop;
                isT0Bottom = true;
            } else {
                t0 = tTop;
                t1 = tBottom;
            }

            if (t0 >= minDistance && t0 <= maxDistance && t0 < t && cullFace != CullFace.FRONT) {
                final double tX = originX + dirX * t0;
                final double tZ = originZ + dirZ * t0;
                if (tX * tX + tZ * tZ <= r * r) {
                    bottomCap = isT0Bottom; topCap = !isT0Bottom;
                    t = t0;
                }
            }

            if (t1 >= minDistance && t1 <= maxDistance && t1 < t && (cullFace != CullFace.BACK || exactPicking)) {
                final double tX = originX + dirX * t1;
                final double tZ = originZ + dirZ * t1;
                if (tX * tX + tZ * tZ <= r * r) {
                    topCap = isT0Bottom; bottomCap = !isT0Bottom;
                    t = t1;
                }
            }
        }

        if (Double.isInfinite(t) || Double.isNaN(t)) {
            // no hit
            return false;
        }

        if (exactPicking) {
            return MeshHelper.computeIntersects(mesh, pickRay, pickResult, this, cullFace, false);
        }

        if (pickResult != null && pickResult.isCloser(t)) {
            final Point3D point = PickResultChooser.computePoint(pickRay, t);

            Point2D txCoords;
            if (topCap) {
                txCoords = new Point2D(
                        0.5 + point.getX() / (2 * r),
                        0.5 + point.getZ() / (2 * r));
            } else if (bottomCap) {
                txCoords = new Point2D(
                        0.5 + point.getX() / (2 * r),
                        0.5 - point.getZ() / (2 * r));
            } else {
                final Point3D proj = new Point3D(point.getX(), 0, point.getZ());
                final Point3D cross = proj.crossProduct(Rotate.Z_AXIS);
                double angle = proj.angle(Rotate.Z_AXIS);
                if (cross.getY() > 0) {
                    angle = 360 - angle;
                }
                txCoords = new Point2D(1 - angle / 360, 0.5 + point.getY() / h);
            }

            pickResult.offer(this, t, PickResult.FACE_UNDEFINED, point, txCoords);
        }
        return true;
    }

    static TriangleMesh createMesh(int div, float h, float r) {

        // NOTE: still create mesh for degenerated cylinder
        final int nPonits = div * 2 + 2;
        final int tcCount = (div + 1) * 4 + 1; // 2 cap tex
        final int faceCount = div * 4;

        float textureDelta = 1.f / 256;

        float dA = 1.f / div;
        h *= .5f;

        float points[] = new float[nPonits * 3];
        float tPoints[] = new float[tcCount * 2];
        int faces[] = new int[faceCount * 6];
        int smoothing[] = new int[faceCount];

        int pPos = 0, tPos = 0;

        for (int i = 0; i < div; ++i) {
            double a = dA * i * 2 * Math.PI;

            points[pPos + 0] = (float) (Math.sin(a) * r);
            points[pPos + 2] = (float) (Math.cos(a) * r);
            points[pPos + 1] = h;
            tPoints[tPos + 0] = 1 - dA * i;
            tPoints[tPos + 1] = 1 - textureDelta;
            pPos += 3; tPos += 2;
        }

        // top edge
        tPoints[tPos + 0] = 0;
        tPoints[tPos + 1] = 1 - textureDelta;
        tPos += 2;

        for (int i = 0; i < div; ++i) {
            double a = dA * i * 2 * Math.PI;
            points[pPos + 0] = (float) (Math.sin(a) * r);
            points[pPos + 2] = (float) (Math.cos(a) * r);
            points[pPos + 1] = -h;
            tPoints[tPos + 0] = 1 - dA * i;
            tPoints[tPos + 1] = textureDelta;
            pPos += 3; tPos += 2;
        }

        // bottom edge
        tPoints[tPos + 0] = 0;
        tPoints[tPos + 1] = textureDelta;
        tPos += 2;

        // add cap central points
        points[pPos + 0] = 0;
        points[pPos + 1] = h;
        points[pPos + 2] = 0;
        points[pPos + 3] = 0;
        points[pPos + 4] = -h;
        points[pPos + 5] = 0;
        pPos += 6;

        // add cap central points
        // bottom cap
        for (int i = 0; i <= div; ++i) {
            double a = (i < div) ? (dA * i * 2) * Math.PI: 0;
            tPoints[tPos + 0] = (float) (Math.sin(a) * 0.5f) + 0.5f;
            tPoints[tPos + 1] = (float) (Math.cos(a) * 0.5f) + 0.5f;
            tPos += 2;
        }

        // top cap
        for (int i = 0; i <= div; ++i) {
            double a = (i < div) ? (dA * i * 2) * Math.PI: 0;
            tPoints[tPos + 0] = 0.5f + (float) (Math.sin(a) * 0.5f);
            tPoints[tPos + 1] = 0.5f - (float) (Math.cos(a) * 0.5f);
            tPos += 2;
        }

        tPoints[tPos + 0] = .5f;
        tPoints[tPos + 1] = .5f;
        tPos += 2;

        int fIndex = 0;

        // build body faces
        for (int p0 = 0; p0 < div; ++p0) {
            int p1 = p0 + 1;
            int p2 = p0 + div;
            int p3 = p1 + div;

            // add p0, p1, p2
            faces[fIndex+0] = p0;
            faces[fIndex+1] = p0;
            faces[fIndex+2] = p2;
            faces[fIndex+3] = p2 + 1;
            faces[fIndex+4] = p1 == div ? 0 : p1;
            faces[fIndex+5] = p1;
            fIndex += 6;

            // add p3, p2, p1
            // *faces++ = SmFace(p3,p1,p2, p3,p1,p2, 1);
            faces[fIndex+0] = p3 % div == 0 ? p3 - div : p3;
            faces[fIndex+1] = p3 + 1;
            faces[fIndex+2] = p1 == div ? 0 : p1;
            faces[fIndex+3] = p1;
            faces[fIndex+4] = p2;
            faces[fIndex+5] = p2 + 1;
            fIndex += 6;

        }
        // build cap faces
        int tStart = (div + 1) * 2;
        int t1 = (div + 1) * 4;
        int p1 = div * 2;

        // bottom cap
        for (int p0 = 0; p0 < div; ++p0) {
            int p2 = p0 + 1;
            int t0 = tStart + p0;
            int t2 = t0 + 1;

            // add p0, p1, p2
            faces[fIndex+0] = p0;
            faces[fIndex+1] = t0;
            faces[fIndex+2] = p2 == div ? 0 : p2;
            faces[fIndex+3] = t2;
            faces[fIndex+4] = p1;
            faces[fIndex+5] = t1;
            fIndex += 6;
        }

        p1 = div * 2 + 1;
        tStart = (div + 1) * 3;

        // top cap
        for (int p0 = 0; p0 < div; ++p0) {
            int p2 = p0 + 1 + div;
            int t0 = tStart + p0;
            int t2 = t0 + 1;

            //*faces++ = SmFace(p0+div+1,p1,p2, t0,t1,t2, 2);
            faces[fIndex+0] = p0 + div;
            faces[fIndex+1] = t0;
            faces[fIndex+2] = p1;
            faces[fIndex+3] = t1;
            faces[fIndex+4] = p2 % div == 0 ? p2 - div : p2;
            faces[fIndex+5] = t2;
            fIndex += 6;
        }

        for (int i = 0; i < div * 2; ++i) {
            smoothing[i] = 1;
        }
        for (int i = div * 2; i < div * 4; ++i) {
            smoothing[i] = 2;
        }

        TriangleMesh m = new TriangleMesh(true);
        m.getPoints().setAll(points);
        m.getTexCoords().setAll(tPoints);
        m.getFaces().setAll(faces);
        m.getFaceSmoothingGroups().setAll(smoothing);

        return m;
    }

    private static class CylinderKey extends Key {

        final double radius, height;
        final int divisions;

        private CylinderKey(double radius, double height, int divisions) {
            this.radius = radius;
            this.height = height;
            this.divisions = divisions;
        }

        @Override
        public int hashCode() {
            long bits = 7L;
            bits = 31L * bits + Double.doubleToLongBits(radius);
            bits = 31L * bits + Double.doubleToLongBits(height);
            bits = 31L * bits + divisions;
            return Long.hashCode(bits);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof CylinderKey)) {
                return false;
            }
            CylinderKey other = (CylinderKey) obj;
            if (divisions != other.divisions) {
                return false;
            }
            if (Double.compare(radius, other.radius) != 0) {
                return false;
            }
            if (Double.compare(height, other.height) != 0) {
                return false;
            }
            return true;
        }
    }
}
