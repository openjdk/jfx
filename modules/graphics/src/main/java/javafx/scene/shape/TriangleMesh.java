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

import com.sun.javafx.collections.FloatArraySyncer;
import com.sun.javafx.collections.IntegerArraySyncer;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.sg.prism.NGTriangleMesh;
import javafx.collections.ArrayChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.PickResult;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;

/**
 * Defines a 3D geometric object contains separate arrays of points, 
 * texture coordinates, and faces that describe a triangulated 
 * geometric mesh.
 *<p>
 * Note that the term point, as used in the method names and method
 * descriptions, actually refers to a set of x, y, and z point
 * representing the position of a single vertex. The term points (plural) is
 * used to indicate sets of x, y, and z points for multiple vertices.
 * Similarly, the term texCoord is used to indicate a set of u and v texture
 * coordinates for a single vertex, while the term texCoords (plural) is used
 * to indicate sets of u and v texture coordinates for multiple vertices.
 * Lastly, the term face is used to indicate 3 set of interleaving points
 * and texture coordinates that together represent the geometric topology of a 
 * single triangle, while the term faces (plural) is used to indicate sets of 
 * triangles (each represent by a face).
 * <p>
 * For example, the faces that represent a single textured rectangle, using 2 triangles,
 * has the following data order: [
 * <p>
 * p0, t0, p1, t1, p3, t3,  // First triangle of a textured rectangle
 * <p>
 * p1, t1, p2, t2, p3, t3   // Second triangle of a textured rectangle
 * <p>
 * ]
 * <p>
 * where p0, p1, p2 and p3 are indices into the points array, and t0, t1, t2
 * and t3 are indices into the texCoords array.
 * 
 * <p>
 * The length of {@code points}, {@code texCoords}, and {@code faces} must be
 * divisible by 3, 2, and 6 respectively.
 * The values in the faces array must be within the range of the number of vertices
 * in the points array (0 to points.length / 3 - 1) for the point indices and 
 * within the range of the number of the vertices in 
 * the texCoords array (0 to texCoords.length / 2 - 1) for the texture coordinate indices.
 * 
 * <p> A warning will be recorded to the logger and the mesh will not be rendered
 * (and will have an empty bounds) if any of the array lengths are invalid
 * or if any of the values in the faces array are out of range.
 * 
 * @since JavaFX 8.0
 */
public class TriangleMesh extends Mesh {

    public static final int NUM_COMPONENTS_PER_POINT = 3;
    public static final int NUM_COMPONENTS_PER_TEXCOORD = 2;
    public static final int NUM_COMPONENTS_PER_FACE = 6;

    // TODO: 3D - Need to validate the size and range of these arrays.
    // A warning will be recorded to the logger and the mesh will have an empty
    // bounds if the validation failed. (RT-30451)
    // The values in faces must be within range and the length of points,
    // texCoords and faces must be divisible by 3, 2 and 6 respectively.
    private final ObservableFloatArray points = FXCollections.observableFloatArray();
    private final ObservableFloatArray texCoords = FXCollections.observableFloatArray();
    private final ObservableIntegerArray faces = FXCollections.observableIntegerArray();
    private final ObservableIntegerArray faceSmoothingGroups = FXCollections.observableIntegerArray();
    
    private final Listener pointsSyncer = new Listener(points);
    private final Listener texCoordsSyncer = new Listener(texCoords);
    private final Listener facesSyncer = new Listener(faces);
    private final Listener faceSmoothingGroupsSyncer = new Listener(faceSmoothingGroups);

    private int refCount = 1;

    private BaseBounds cachedBounds;

    /**
     * Creates a new instance of {@code TriangleMesh} class.
     */
    public TriangleMesh() {
    }

    /**
     * Gets the {@code ObservableFloatArray} of points of this {@code TriangleMesh}.
     *
     * @return {@code ObservableFloatArray} of points where each point is
     * represented by 3 float values x, y and z, in that order.
     */    
    public ObservableFloatArray getPoints() {
        return points;
    }

    /**
     * Gets the {@code ObservableFloatArray} of texture coordinates of this {@code TriangleMesh}.
     *
     * @return {@code ObservableFloatArray} array of texture coordinates
     * where each texture coordinate is represented by 2 float values: u and v,
     * in that order
     */    
    public ObservableFloatArray getTexCoords() {
        return texCoords;
    }
 
    /**
     * Gets the {@code ObservableIntegerArray} of faces, indices into the points 
     * and texCoords arrays, of this  {@code TriangleMesh}
     *
     * @return {@code ObservableIntegerArray} of faces where each face is
     * 6 integers p0, t0, p1, t1, p3, t3, where p0, p1 and p2 are indices of 
     * points in points {@code ObservableFloatArray} and t0, t1 and t2 are 
     * indices of texture coordinates in texCoords {@code ObservableFloatArray}.
     * Both indices are in terms of vertices (points or texCoords), not individual
     * floats.
     */    
    public ObservableIntegerArray getFaces() {
        return faces;
    }

    /**
     * Gets the {@code ObservableIntegerArray} of face smoothing groups 
     * of this {@code TriangleMesh}.
     * Smoothing affects how a mesh is rendered but it does not effect its
     * geometry. The face smoothing group value is used to control the smoothing
     * between adjacent faces.
     *
     * <p> The face smoothing group is represented by an array of bits and up to
     * 32 unique groups is possible; (1 << 0) to (1 << 31). The face smoothing
     * group value can range from 0 (no smoothing group) to all 32 groups. A face
     * can belong to zero or more smoothing groups. A face is a member of group
     * N if bit N is set, for example, groups |= (1 << N). A value of 0 implies
     * no smoothing group or hard edges.
     * Smoothing is applied when adjacent pair of faces shared a smoothing group.
     * Otherwise the faces are rendered with a hard edge between them.
     *
     * <p> An empty faceSmoothingGroups implies all faces in this mesh have a
     * smoothing group value of 1.
     *
     * <p> Note: If faceSmoothingGroups is not empty, is size must
     * be equal to number of faces.
     */    
    public ObservableIntegerArray getFaceSmoothingGroups() {
        return faceSmoothingGroups;
    }

    @Override void setDirty(boolean value) {
        super.setDirty(value);
        if (!value) { // false
            pointsSyncer.setDirty(false);
            texCoordsSyncer.setDirty(false);
            facesSyncer.setDirty(false);
            faceSmoothingGroupsSyncer.setDirty(false);
        }
    }

    int getRefCount() {
        return refCount;
    }

    synchronized void incRef() {
        this.refCount += 1;
    }

    synchronized void decRef() {
        this.refCount -= 1;
    }

    private NGTriangleMesh peer;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    /** The peer node created by the graphics Toolkit/Pipeline implementation */
    NGTriangleMesh impl_getPGTriangleMesh() {
        if (peer == null) {
            peer = new NGTriangleMesh();
        }
        return peer;
    }

    @Override
    NGTriangleMesh getPGMesh() {
        return impl_getPGTriangleMesh();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    void impl_updatePG() {
        if (!isDirty()) {
            return;
        }

        final NGTriangleMesh pgTriMesh = impl_getPGTriangleMesh();
        // sync points 
        if (pointsSyncer.dirty) {
            pgTriMesh.syncPoints(pointsSyncer);
        }
        if (texCoordsSyncer.dirty) {
            pgTriMesh.syncTexCoords(texCoordsSyncer);
        }
        if (facesSyncer.dirty) {
            pgTriMesh.syncFaces(facesSyncer);
        }
        if (faceSmoothingGroupsSyncer.dirty) {
            pgTriMesh.syncFaceSmoothingGroups(faceSmoothingGroupsSyncer);
        }
        setDirty(false);
    }

    @Override
    BaseBounds computeBounds(BaseBounds bounds) {
        if (isDirty() || cachedBounds == null) {
            cachedBounds = new BoxBounds();

            final double len = points.size();
            for (int i = 0; i < len; i += NUM_COMPONENTS_PER_POINT) {
                cachedBounds.add(points.get(i), points.get(i + 1), points.get(i + 2));
            }
        }
        return bounds.deriveWithNewBounds(cachedBounds);
    }

    /**
     * Computes the centroid of the given triangle
     * @param v0 vertex of the triangle
     * @param v1 vertex of the triangle
     * @param v2 vertex of the triangle
     * @return the triangle centroid
     */
    private Point3D computeCentroid(Point3D v0, Point3D v1, Point3D v2) {
        Point3D center = v1.midpoint(v2);

        Point3D vec = center.subtract(v0);
        return v0.add(new Point3D(vec.getX() / 3.0, vec.getY() / 3.0, vec.getZ() / 3.0));
    }

    /**
     * Computes the centroid of the given triangle
     * @param v0 vertex of the triangle
     * @param v1 vertex of the triangle
     * @param v2 vertex of the triangle
     * @return the triangle centroid
     */
    private Point2D computeCentroid(Point2D v0, Point2D v1, Point2D v2) {
        Point2D center = v1.midpoint(v2);

        Point2D vec = center.subtract(v0);
        return v0.add(new Point2D(vec.getX() / 3.0, vec.getY() / 3.0));
    }

    /**
     * Computes intersection of a pick ray and a single triangle face.
     *
     * It takes pickRay, origin and dir. The latter two can be of course obtained
     * from the pickRay, but we need them to be converted to Point3D and don't
     * want to do that for all faces. Therefore the conversion is done just once
     * and passed to the method for all the faces.
     *
     * @param pickRay pick ray
     * @param origin pick ray's origin
     * @param dir pick ray's direction
     * @param faceIndex index of the face to test
     * @param cullFace cull face of the Node (and thus the tested face)
     * @param candidate the owner node (for the possible placement to the result)
     * @param reportFace whether or not to report he hit face
     * @param result the pick result to be updated if a closer intersection is found
     * @return true if the pick ray intersects with the face (regardless of whether
     *              the result has been updated)
     */
    private boolean computeIntersectsFace(
            PickRay pickRay, Point3D origin, Point3D dir, int faceIndex,
            CullFace cullFace, Node candidate, boolean reportFace, PickResultChooser result) {

        final int v0Idx = faces.get(faceIndex) * NUM_COMPONENTS_PER_POINT;
        final int v1Idx = faces.get(faceIndex + 2) * NUM_COMPONENTS_PER_POINT;
        final int v2Idx = faces.get(faceIndex + 4) * NUM_COMPONENTS_PER_POINT;

        final Point3D v0 = new Point3D(points.get(v0Idx), points.get(v0Idx + 1), points.get(v0Idx + 2));
        final Point3D v1 = new Point3D(points.get(v1Idx), points.get(v1Idx + 1), points.get(v1Idx + 2));
        final Point3D v2 = new Point3D(points.get(v2Idx), points.get(v2Idx + 1), points.get(v2Idx + 2));

        final Point3D e1 = v1.subtract(v0);
        final Point3D e2 = v2.subtract(v0);

        final Point3D h = dir.crossProduct(e2);

        final double a = e1.dotProduct(h);
        if (a == 0.0) {
            return false;
        }
        final double f = 1.0 / a;

        final Point3D s = origin.subtract(v0);

        final double u = f * (s.dotProduct(h));

        if (u < 0.0 || u > 1.0) {
            return false;
        }

        Point3D q = s.crossProduct(e1);
        double v = f * dir.dotProduct(q);

        if (v < 0.0 || u + v > 1.0) {
            return false;
        }

        final double t = f * e2.dotProduct(q);

        if (t >= pickRay.getNearClip() && t <= pickRay.getFarClip()) {
            if (cullFace != CullFace.NONE) {
                final Point3D normal = e1.crossProduct(e2);
                final double nangle = normal.angle(
                        new Point3D(-dir.getX(), -dir.getY(), -dir.getZ()));
                if ((nangle >= 90 || cullFace != CullFace.BACK) &&
                        (nangle <= 90 || cullFace != CullFace.FRONT)) {
                    // hit culled face
                    return false;
                }
            }

            if (Double.isInfinite(t) || Double.isNaN(t)) {
                // we've got a nonsense pick ray or triangle
                return false;
            }

            if (result == null || !result.isCloser(t)) {
                // it intersects, but we are not interested in the result
                // or we already have a better (closer) result
                // so we can omit the point and texture computation
                return true;
            }

            Point3D point = PickResultChooser.computePoint(pickRay, t);

            // Now compute texture mapping. First rotate the triangle
            // so that we can compute in 2D

            final Point3D centroid = computeCentroid(v0, v1, v2);
            final Point3D cv0 = v0.subtract(centroid);
            final Point3D cv1 = v1.subtract(centroid);
            final Point3D cv2 = v2.subtract(centroid);

            final Point3D ce1 = cv1.subtract(cv0);
            final Point3D ce2 = cv2.subtract(cv0);
            Point3D n = ce1.crossProduct(ce2);
            if (n.getZ() < 0) {
                n = new Point3D(-n.getX(), -n.getY(), -n.getZ());
            }
            final Point3D ax = n.crossProduct(Rotate.Z_AXIS);
            final double angle = Math.atan2(ax.magnitude(), n.dotProduct(Rotate.Z_AXIS));

            Rotate r = new Rotate(Math.toDegrees(angle), ax);
            final Point3D crv0 = r.transform(cv0);
            final Point3D crv1 = r.transform(cv1);
            final Point3D crv2 = r.transform(cv2);
            final Point3D rPoint = r.transform(point.subtract(centroid));

            final Point2D flatV0 = new Point2D(crv0.getX(), crv0.getY());
            final Point2D flatV1 = new Point2D(crv1.getX(), crv1.getY());
            final Point2D flatV2 = new Point2D(crv2.getX(), crv2.getY());
            final Point2D flatPoint = new Point2D(rPoint.getX(), rPoint.getY());

            // Obtain the texture triangle

            final int t0Idx = faces.get(faceIndex + 1) * NUM_COMPONENTS_PER_TEXCOORD;
            final int t1Idx = faces.get(faceIndex + 3) * NUM_COMPONENTS_PER_TEXCOORD;
            final int t2Idx = faces.get(faceIndex + 5) * NUM_COMPONENTS_PER_TEXCOORD;

            final Point2D u0 = new Point2D(texCoords.get(t0Idx), texCoords.get(t0Idx + 1));
            final Point2D u1 = new Point2D(texCoords.get(t1Idx), texCoords.get(t1Idx + 1));
            final Point2D u2 = new Point2D(texCoords.get(t2Idx), texCoords.get(t2Idx + 1));

            final Point2D txCentroid = computeCentroid(u0, u1, u2);

            final Point2D cu0 = u0.subtract(txCentroid);
            final Point2D cu1 = u1.subtract(txCentroid);
            final Point2D cu2 = u2.subtract(txCentroid);

            // Find the transform between the two triangles

            final Affine src = new Affine(
                    flatV0.getX(), flatV1.getX(), flatV2.getX(),
                    flatV0.getY(), flatV1.getY(), flatV2.getY());
            final Affine trg = new Affine(
                    cu0.getX(), cu1.getX(), cu2.getX(),
                    cu0.getY(), cu1.getY(), cu2.getY());

            Point2D txCoords = null;

            try {
                src.invert();
                trg.append(src);
                txCoords = txCentroid.add(trg.transform(flatPoint));
            } catch (NonInvertibleTransformException e) {
                // Can't compute texture mapping, probably the coordinates
                // don't make sense. Ignore it and return null tex coords.
            }

            result.offer(candidate, t, 
                    reportFace ? faceIndex / NUM_COMPONENTS_PER_FACE : PickResult.FACE_UNDEFINED,
                    point, txCoords);
            return true;
        }

        return false;
    }


    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Override
    @Deprecated
    protected boolean impl_computeIntersects(PickRay pickRay, PickResultChooser pickResult, 
            Node candidate, CullFace cullFace, boolean reportFace) {

        boolean found = false;
        final int size = faces.size();

        final Vec3d o = pickRay.getOriginNoClone();
        final Point3D origin = new Point3D(o.x, o.y, o.z);

        final Vec3d d = pickRay.getDirectionNoClone();
        final Point3D dir = new Point3D(d.x, d.y, d.z);

        for (int i = 0; i < size; i += NUM_COMPONENTS_PER_FACE) {
            if (computeIntersectsFace(pickRay, origin, dir, i, cullFace, candidate, 
                    reportFace, pickResult)) {
                found = true;
            }
        }

        return found;
    }

    private class Listener<T extends ObservableArray<T>> implements ArrayChangeListener<T>, FloatArraySyncer, IntegerArraySyncer {
        
        protected final T array;
        protected boolean dirty = true;
        /**
         * Array was replaced
         * @return true if array was replaced; false otherwise
         */
        protected boolean dirtyInFull = true;
        protected int dirtyRangeFrom;
        protected int dirtyRangeLength;

        public Listener(T array) {
            this.array = array;
            array.addListener(this);
        }

        /**
         * Adds a dirty range
         * @param from index of the first modified element
         * @param length length of the modified range
         */
        protected final void addDirtyRange(int from, int length) {
            if (length > 0 && !dirtyInFull) {
                markDirty();
                if (dirtyRangeLength == 0) {
                    dirtyRangeFrom = from;
                    dirtyRangeLength = length;
                } else {
                    int fromIndex = Math.min(dirtyRangeFrom, from);
                    int toIndex = Math.max(dirtyRangeFrom + dirtyRangeLength, from + length);
                    dirtyRangeFrom = fromIndex;
                    dirtyRangeLength = toIndex - fromIndex;
                }
            }
        }

        protected void markDirty() {
            dirty = true;
            TriangleMesh.this.setDirty(true);
        }

        @Override
        public void onChanged(T observableArray, boolean sizeChanged, int from, int to) {
            if (sizeChanged) {
                setDirty(true);
            } else {
                addDirtyRange(from, to - from);
            }
        }

        /**
         * @param dirty if true, the whole collection is marked as dirty;
         * if false, the whole collection is marked as not-dirty
         */
        public final void setDirty(boolean dirty) {
            this.dirtyInFull = dirty;
            if (dirty) {
                markDirty();
                dirtyRangeFrom = 0;
                dirtyRangeLength = array.size();
            } else {
                this.dirty = false;
                dirtyRangeFrom = dirtyRangeLength = 0;
            }
        }

        @Override
        public float[] syncTo(float[] array) {
            ObservableFloatArray floatArray = (ObservableFloatArray) this.array;
            if (dirtyInFull || array == null || array.length != floatArray.size()) {
                // Always allocate a new array when size changes
                return floatArray.toArray(null);
            }
            floatArray.copyTo(dirtyRangeFrom, array, dirtyRangeFrom, dirtyRangeLength);
            return array;
        }

        @Override
        public int[] syncTo(int[] array) {
            ObservableIntegerArray intArray = (ObservableIntegerArray) this.array;
            if (dirtyInFull || array == null || array.length != intArray.size()) {
                // Always allocate a new array when size changes
                return intArray.toArray(null);
            }
            intArray.copyTo(dirtyRangeFrom, array, dirtyRangeFrom, dirtyRangeLength);
            return array;
        }
    }
}
