/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.shape.ObservableFaceArrayImpl;
import com.sun.javafx.collections.FloatArraySyncer;
import com.sun.javafx.collections.IntegerArraySyncer;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.shape.TriangleMeshHelper;
import com.sun.javafx.sg.prism.NGTriangleMesh;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import com.sun.javafx.logging.PlatformLogger;

/**
 * Defines a 3D triangle mesh that consists of its associated {@code VertexFormat}
 * and a set of separate arrays of vertex components such as points, normals,
 * texture coordinates, and an array of faces that define the individual triangles
 * of the mesh.
 *<p>
 * Note that the term point, as used in the method names and method
 * descriptions, actually refers to a 3D point (x, y, z) in space
 * representing the position of a single vertex. The term points (plural) is
 * used to indicate sets of 3D points for multiple vertices.
 * Similarly, the term normal is used to indicate a 3D vector (nx, ny, nz) in space
 * representing the direction of a single vertex. The term normals (plural) is
 * used to indicate sets of 3D vectors for multiple vertices.
 * The term texCoord is used to indicate a single pair of 2D texture
 * coordinates (u, v) for a single vertex, while the term texCoords (plural) is used
 * to indicate sets of texture coordinates for multiple vertices.
 * Lastly, the term face is used to indicate 3 sets of interleaving points,
 * normals (optional, depending on the associated VertexFormat)
 * and texture coordinates that together represent the geometric topology of a
 * single triangle, while the term faces (plural) is used to indicate sets of
 * triangles (each represent by a face).
 * <p>
 * For example, the faces with {@code VertexFormat.POINT_TEXCOORD} that represent
 * a single textured rectangle, using 2 triangles, have the following data order: [
 * <p>
 * p0, t0, p1, t1, p3, t3,  // First triangle of a textured rectangle
 * <p>
 * p1, t1, p2, t2, p3, t3   // Second triangle of a textured rectangle
 * <p>
 * ]
 * <p>
 * whereas the faces with {@code VertexFormat.POINT_NORMAL_TEXCOORD} that represent
 * a single textured rectangle, using 2 triangles, have the following data order: [
 * <p>
 * p0, n0, t0, p1, n1, t1, p3, n3, t3,  // First triangle of a textured rectangle
 * <p>
 * p1, n1, t1, p2, n2, t2, p3, n3, t3   // Second triangle of a textured rectangle
 * <p>
 * ]
 * <p>
 * where p0, p1, p2 and p3 are indices into the points array, n0, n1, n2 and n3
 * are indices into the normals array, and t0, t1, t2 and t3 are indices into
 * the texCoords array.
 *
 * <p>
 * A triangle has a front and back face. The winding order of a triangle's vertices
 * determines which side is the front face. JavaFX chooses the counter-clockwise
 * (or right-hand rule) winding order as the front face. By default, only the
 * front face of a triangle is rendered. See {@code CullFace} for more
 * information.
 *
 * <p>
 * The length of {@code points}, {@code normals}, and {@code texCoords} must be
 * divisible by 3, 3, and 2 respectively. The length of {@code faces} must be
 * divisible by 6 if it is of {@code VertexFormat.POINT_TEXCOORD} else it must
 * be divisible by 9 if it is of {@code VertexFormat.POINT_NORMAL_TEXCOORD}.
 * The values in the faces array must be within the range of the number of vertices
 * in the points array (0 to points.length / 3 - 1) for the point indices, within
 * the range of the number of vertices in the normals array
 * (0 to normals.length / 3 - 1) for the normal indices, and within the range of
 * the number of the vertices in the texCoords array (0 to texCoords.length / 2 - 1)
 * for the texture coordinate indices.
 *
 * <p> A warning will be recorded to the logger and the mesh will not be rendered
 * (and will have an empty bounds) if any of the array lengths are invalid
 * or if any of the values in the faces array are out of range.
 *
 * @since JavaFX 8.0
 */
public class TriangleMesh extends Mesh {
    static {
        TriangleMeshHelper.setTriangleMeshAccessor(new TriangleMeshHelper.TriangleMeshAccessor() {
            @Override
            public boolean doComputeIntersects(Mesh mesh, PickRay pickRay,
                    PickResultChooser pickResult, Node candidate, CullFace cullFace,
                    boolean reportFace) {
                return ((TriangleMesh) mesh).doComputeIntersects(pickRay,
                        pickResult, candidate, cullFace, reportFace);
            }
        });
    }

    private final ObservableFloatArray points = FXCollections.observableFloatArray();
    private final ObservableFloatArray normals = FXCollections.observableFloatArray();
    private final ObservableFloatArray texCoords = FXCollections.observableFloatArray();
    private final ObservableFaceArray faces = new ObservableFaceArrayImpl();
    private final ObservableIntegerArray faceSmoothingGroups = FXCollections.observableIntegerArray();

    private final Listener pointsSyncer = new Listener(points);
    private final Listener normalsSyncer = new Listener(normals);
    private final Listener texCoordsSyncer = new Listener(texCoords);
    private final Listener facesSyncer = new Listener(faces);
    private final Listener faceSmoothingGroupsSyncer = new Listener(faceSmoothingGroups);
    private final boolean isPredefinedShape;
    private boolean isValidDirty = true;
    private boolean isPointsValid, isNormalsValid, isTexCoordsValid, isFacesValid, isFaceSmoothingGroupValid;
    private int refCount = 1;

    private BaseBounds cachedBounds;

    /**
     * Creates a new instance of {@code TriangleMesh} class with the default
     * {@code VertexFormat.POINT_TEXCOORD} format type.
     */
    public TriangleMesh() {
        this(false);
        TriangleMeshHelper.initHelper(this);
    }

    /**
     * Creates a new instance of {@code TriangleMesh} class with the specified
     * {@code VertexFormat}.
     *
     * @param vertexFormat specifies the vertex format type.
     *
     * @since JavaFX 8u40
     */
    public TriangleMesh(VertexFormat vertexFormat) {
        this(false);
        this.setVertexFormat(vertexFormat);
        TriangleMeshHelper.initHelper(this);
    }

    TriangleMesh(boolean isPredefinedShape) {
        this.isPredefinedShape = isPredefinedShape;
        if (isPredefinedShape) {
            isPointsValid = true;
            isNormalsValid = true;
            isTexCoordsValid = true;
            isFacesValid = true;
            isFaceSmoothingGroupValid = true;
        } else {
            isPointsValid = false;
            isNormalsValid = false;
            isTexCoordsValid = false;
            isFacesValid = false;
            isFaceSmoothingGroupValid = false;
        }
        TriangleMeshHelper.initHelper(this);
    }

    /**
     * Specifies the vertex format of this {@code TriangleMesh}, one of
     * {@code VertexFormat.POINT_TEXCOORD} or {@code VertexFormat.POINT_NORMAL_TEXCOORD}.
     *
     * @defaultValue VertexFormat.POINT_TEXCOORD
     *
     * @since JavaFX 8u40
     */
    private ObjectProperty<VertexFormat> vertexFormat;

    public final void setVertexFormat(VertexFormat value) {
        vertexFormatProperty().set(value);
    }

    public final VertexFormat getVertexFormat() {
        return vertexFormat == null ? VertexFormat.POINT_TEXCOORD : vertexFormat.get();
    }

    public final ObjectProperty<VertexFormat> vertexFormatProperty() {
        if (vertexFormat == null) {
            vertexFormat = new SimpleObjectProperty<>(TriangleMesh.this, "vertexFormat") {

                @Override
                protected void invalidated() {
                    setDirty(true);
                    // Need to mark faces and faceSmoothingGroups dirty too.
                    facesSyncer.setDirty(true);
                    faceSmoothingGroupsSyncer.setDirty(true);
                }
            };
        }
        return vertexFormat;
    }

    /**
     * Returns the number of elements that represents a point.
     *
     * @return number of elements
     */
    public final int getPointElementSize() {
        return getVertexFormat().getPointElementSize();
    }

    /**
     * Returns the number of elements that represents a normal.
     *
     * @return number of elements
     *
     * @since JavaFX 8u40
     */
    public final int getNormalElementSize() {
        return getVertexFormat().getNormalElementSize();
    }

    /**
     * Returns the number of elements that represents a texture coordinates.
     *
     * @return number of elements
     */
    public final int getTexCoordElementSize() {
        return getVertexFormat().getTexCoordElementSize();
    }

    /**
     * Returns the number of elements that represents a face.
     *
     * @return number of elements
     */
    public final int getFaceElementSize() {
        return getVertexFormat().getVertexIndexSize() * 3;
    }

    /**
     * Gets the {@code points} array of this {@code TriangleMesh}.
     *
     * @return {@code points} array where each point is
     * represented by 3 float values x, y and z, in that order.
     */
    public final ObservableFloatArray getPoints() {
        return points;
    }

    /**
     * Gets the {@code normals} array of this {@code TriangleMesh}.
     *
     * @return {@code normals} array where each normal is
     * represented by 3 float values nx, ny and nz, in that order.
     *
     * @since JavaFX 8u40
     */
    public final ObservableFloatArray getNormals() {
        return normals;
    }

    /**
     * Gets the  {@code texCoords} array of this {@code TriangleMesh}.
     * The coordinates are proportional, so texture's top-left corner
     * is at [0, 0] and bottom-right corner is at [1, 1].
     *
     * @return {@code texCoord} array where each texture coordinate is represented
     * by 2 float values: u and v, in that order.
     */
    public final ObservableFloatArray getTexCoords() {
        return texCoords;
    }

    /**
     * Gets the {@code faces} array, indices into the {@code points},
     * {@code normals} (optional, if it is a {@code VertexFormat.POINT_NORMAL_TEXCOORD}
     * mesh) and {@code texCoords} arrays, of this  {@code TriangleMesh}. All
     * indices are in terms of elements in to the points, normals or texCoords
     * arrays not individual floats.
     *
     * @return {@code faces} array where each face is of
     * 3 * {@code VertexFormat.getVertexIndexSize()} integers.
     */
    public final ObservableFaceArray getFaces() {
        return faces;
    }

    /**
     * Gets the {@code faceSmoothingGroups} array of this {@code TriangleMesh}.
     * Smoothing affects how a mesh is rendered but it does not effect its
     * geometry. The face smoothing group value is used to control the smoothing
     * between adjacent faces.
     *
     * <p>
     * {@literal The face smoothing group value is represented by an array of bits and up to
     * 32 unique groups is possible; (1 << 0) to (1 << 31). The face smoothing
     * group value can range from 0 (no smoothing group) to all 32 groups. A face
     * can belong to zero or more smoothing groups. A face is a member of group
     * N if bit N is set, for example, groups |= (1 << N). A value of 0 implies
     * no smoothing group or hard edges.}
     * Smoothing is applied when adjacent pair of faces shared a smoothing group.
     * Otherwise the faces are rendered with a hard edge between them.
     *
     * <p> An empty faceSmoothingGroups implies all faces in this mesh have a
     * smoothing group value of 1.
     *
     * <p> If faceSmoothingGroups is not empty, its size must
     * be equal to number of faces.
     *
     * <p> This faceSmoothingGroups has no effect on its {@code TriangleMesh} if
     * it is of {@code VertexFormat.POINT_NORMAL_TEXCOORD} format.
     * @return the {@code faceSmoothingGroups} array of this {@code TriangleMesh}
     */
    public final ObservableIntegerArray getFaceSmoothingGroups() {
        return faceSmoothingGroups;
    }

    @Override void setDirty(boolean value) {
        super.setDirty(value);
        if (!value) { // false
            pointsSyncer.setDirty(false);
            normalsSyncer.setDirty(false);
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

    /* The peer node created by the graphics Toolkit/Pipeline implementation */
    NGTriangleMesh getPGTriangleMesh() {
        if (peer == null) {
            peer = new NGTriangleMesh();
        }
        return peer;
    }

    @Override
    NGTriangleMesh getPGMesh() {
        return getPGTriangleMesh();
    }

    private boolean validatePoints() {
        if (points.size() == 0) { // Valid but meaningless for picking or rendering.
            return false;
        }

        if ((points.size() % getVertexFormat().getPointElementSize()) != 0) {
            String logname = TriangleMesh.class.getName();
            PlatformLogger.getLogger(logname).warning("points.size() has "
                    + "to be divisible by getPointElementSize(). It is to"
                    + " store multiple x, y, and z coordinates of this mesh");
            return false;
        }
        return true;
    }

    private boolean validateNormals() {
        // Only validate normals if vertex format has normal component
        if (getVertexFormat() != VertexFormat.POINT_NORMAL_TEXCOORD) return true;

        if (normals.size() == 0) { // Valid but meaningless for picking or rendering.
            return false;
        }

        if ((normals.size() % getVertexFormat().getNormalElementSize()) != 0) {
            String logname = TriangleMesh.class.getName();
            PlatformLogger.getLogger(logname).warning("normals.size() has "
                    + "to be divisible by getNormalElementSize(). It is to"
                    + " store multiple nx, ny, and nz coordinates of this mesh");
            return false;
        }
        return true;
    }

    private boolean validateTexCoords() {
        if (texCoords.size() == 0) { // Valid but meaningless for picking or rendering.
            return false;
        }

        if ((texCoords.size() % getVertexFormat().getTexCoordElementSize()) != 0) {
            String logname = TriangleMesh.class.getName();
            PlatformLogger.getLogger(logname).warning("texCoords.size() "
                    + "has to be divisible by getTexCoordElementSize()."
                    + " It is to store multiple u and v texture coordinates"
                    + " of this mesh");
            return false;
        }
        return true;
    }

    private boolean validateFaces() {
        if (faces.size() == 0) { // Valid but meaningless for picking or rendering.
            return false;
        }

        String logname = TriangleMesh.class.getName();
        if ((faces.size() % getFaceElementSize()) != 0) {
            PlatformLogger.getLogger(logname).warning("faces.size() has "
                    + "to be divisible by getFaceElementSize().");
            return false;
        }

        if (getVertexFormat() == VertexFormat.POINT_TEXCOORD) {
            int nVerts = points.size() / getVertexFormat().getPointElementSize();
            int nTVerts = texCoords.size() / getVertexFormat().getTexCoordElementSize();
            for (int i = 0; i < faces.size(); i++) {
                if (i % 2 == 0 && (faces.get(i) >= nVerts || faces.get(i) < 0)
                        || (i % 2 != 0 && (faces.get(i) >= nTVerts || faces.get(i) < 0))) {
                    PlatformLogger.getLogger(logname).warning("The values in the "
                            + "faces array must be within the range of the number "
                            + "of vertices in the points array (0 to points.length / 3 - 1) "
                            + "for the point indices and within the range of the "
                            + "number of the vertices in the texCoords array (0 to "
                            + "texCoords.length / 2 - 1) for the texture coordinate indices.");
                    return false;
                }
            }
        } else if (getVertexFormat() == VertexFormat.POINT_NORMAL_TEXCOORD) {
            int nVerts = points.size() / getVertexFormat().getPointElementSize();
            int nNVerts =  normals.size() / getVertexFormat().getNormalElementSize();
            int nTVerts = texCoords.size() / getVertexFormat().getTexCoordElementSize();
            for (int i = 0; i < faces.size(); i+=3) {
                if ((faces.get(i) >= nVerts || faces.get(i) < 0)
                        || (faces.get(i + 1) >= nNVerts || faces.get(i + 1) < 0)
                        || (faces.get(i + 2) >= nTVerts || faces.get(i + 2) < 0)) {
                    PlatformLogger.getLogger(logname).warning("The values in the "
                            + "faces array must be within the range of the number "
                            + "of vertices in the points array (0 to points.length / 3 - 1) "
                            + "for the point indices, and within the range of the "
                            + "number of the vertices in the normals array (0 to "
                            + "normals.length / 3 - 1) for the normals indices, and "
                            + "number of the vertices in the texCoords array (0 to "
                            + "texCoords.length / 2 - 1) for the texture coordinate indices.");
                    return false;
                }
            }
        } else {
            PlatformLogger.getLogger(logname).warning("Unsupported VertexFormat: " + getVertexFormat().toString());
            return false;
        }
        return true;
    }

    private boolean validateFaceSmoothingGroups() {
        if (faceSmoothingGroups.size() != 0
                && faceSmoothingGroups.size() != (faces.size() / getFaceElementSize())) {
            String logname = TriangleMesh.class.getName();
            PlatformLogger.getLogger(logname).warning("faceSmoothingGroups.size()"
                    + " has to equal to number of faces.");
            return false;
        }
        return true;
    }

    private boolean validate() {
        if (isPredefinedShape) {
            return true;
        }

        if (isValidDirty) {
            if (pointsSyncer.dirtyInFull) {
                isPointsValid = validatePoints();
            }
            if (normalsSyncer.dirtyInFull) {
                isNormalsValid = validateNormals();
            }
            if (texCoordsSyncer.dirtyInFull) {
                isTexCoordsValid = validateTexCoords();
            }
            if (facesSyncer.dirty || pointsSyncer.dirtyInFull
                    || normalsSyncer.dirtyInFull || texCoordsSyncer.dirtyInFull) {
                isFacesValid = isPointsValid && isNormalsValid
                        && isTexCoordsValid && validateFaces();
            }
            if (faceSmoothingGroupsSyncer.dirtyInFull || facesSyncer.dirtyInFull) {
                isFaceSmoothingGroupValid = isFacesValid && validateFaceSmoothingGroups();
            }
            isValidDirty = false;
        }
        return isPointsValid && isNormalsValid && isTexCoordsValid
                && isFaceSmoothingGroupValid && isFacesValid;
    }

    @Override
    void updatePG() {
        if (!isDirty()) {
            return;
        }

        final NGTriangleMesh pgTriMesh = getPGTriangleMesh();
        if (validate()) {
            pgTriMesh.setUserDefinedNormals(getVertexFormat() == VertexFormat.POINT_NORMAL_TEXCOORD);
            pgTriMesh.syncPoints(pointsSyncer);
            pgTriMesh.syncNormals(normalsSyncer);
            pgTriMesh.syncTexCoords(texCoordsSyncer);
            pgTriMesh.syncFaces(facesSyncer);
            pgTriMesh.syncFaceSmoothingGroups(faceSmoothingGroupsSyncer);
        } else {
            pgTriMesh.setUserDefinedNormals(false);
            pgTriMesh.syncPoints(null);
            pgTriMesh.syncNormals(null);
            pgTriMesh.syncTexCoords(null);
            pgTriMesh.syncFaces(null);
            pgTriMesh.syncFaceSmoothingGroups(null);
        }
        setDirty(false);
    }

    @Override
    BaseBounds computeBounds(BaseBounds bounds) {
        if (isDirty() || cachedBounds == null) {
            cachedBounds = new BoxBounds();
            if (validate()) {
                final int len = points.size();
                final int pointElementSize = getVertexFormat().getPointElementSize();
                for (int i = 0; i < len; i += pointElementSize) {
                    cachedBounds.add(points.get(i), points.get(i + 1), points.get(i + 2));
                }
            }
        }
        return bounds.deriveWithNewBounds(cachedBounds);
    }

    /**
     * Computes the centroid of the given triangle
     * @param v0x x coord of first vertex of the triangle
     * @param v0y y coord of first vertex of the triangle
     * @param v0z z coord of first vertex of the triangle
     * @param v1x x coord of second vertex of the triangle
     * @param v1y y coord of second vertex of the triangle
     * @param v1z z coord of second vertex of the triangle
     * @param v2x x coord of third vertex of the triangle
     * @param v2y y coord of third vertex of the triangle
     * @param v2z z coord of third vertex of the triangle
     * @return the triangle centroid
     */
    private Point3D computeCentroid(
            double v0x, double v0y, double v0z,
            double v1x, double v1y, double v1z,
            double v2x, double v2y, double v2z) {

//        Point3D center = v1.midpoint(v2);
//        Point3D vec = center.subtract(v0);
//        return v0.add(new Point3D(vec.getX() / 3.0, vec.getY() / 3.0, vec.getZ() / 3.0));

        return new Point3D(
            v0x + (v2x + (v1x - v2x) / 2.0 - v0x) / 3.0,
            v0y + (v2y + (v1y - v2y) / 2.0 - v0y) / 3.0,
            v0z + (v2z + (v1z - v2z) / 2.0 - v0z) / 3.0);
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
            PickRay pickRay, Vec3d origin, Vec3d dir, int faceIndex,
            CullFace cullFace, Node candidate, boolean reportFace, PickResultChooser result) {//, BoxBounds rayBounds) {

        // This computation was naturally done by Point3D and its operations,
        // but it needs a lot of points and there is often a lot of triangles
        // so it is vital for performance to use only primitive variables
        // and do the computing manually.

        int vertexIndexSize = getVertexFormat().getVertexIndexSize();
        int pointElementSize = getVertexFormat().getPointElementSize();
        final int v0Idx = faces.get(faceIndex) * pointElementSize;
        final int v1Idx = faces.get(faceIndex + vertexIndexSize) * pointElementSize;
        final int v2Idx = faces.get(faceIndex + (2 * vertexIndexSize)) * pointElementSize;

        final float v0x = points.get(v0Idx);
        final float v0y = points.get(v0Idx + 1);
        final float v0z = points.get(v0Idx + 2);
        final float v1x = points.get(v1Idx);
        final float v1y = points.get(v1Idx + 1);
        final float v1z = points.get(v1Idx + 2);
        final float v2x = points.get(v2Idx);
        final float v2y = points.get(v2Idx + 1);
        final float v2z = points.get(v2Idx + 2);

        // e1 = v1.subtract(v0)
        final float e1x = v1x - v0x;
        final float e1y = v1y - v0y;
        final float e1z = v1z - v0z;
        // e2 = v2.subtract(v0)
        final float e2x = v2x - v0x;
        final float e2y = v2y - v0y;
        final float e2z = v2z - v0z;

        // h = dir.crossProduct(e2)
        final double hx = dir.y * e2z - dir.z * e2y;
        final double hy = dir.z * e2x - dir.x * e2z;
        final double hz = dir.x * e2y - dir.y * e2x;

        // a = e1.dotProduct(h)
        final double a = e1x * hx + e1y * hy + e1z * hz;
        if (a == 0.0) {
            return false;
        }
        final double f = 1.0 / a;

        // s = origin.subtract(v0)
        final double sx = origin.x - v0x;
        final double sy = origin.y - v0y;
        final double sz = origin.z - v0z;

        // u = f * (s.dotProduct(h))
        final double u = f * (sx * hx + sy * hy + sz * hz);

        if (u < 0.0 || u > 1.0) {
            return false;
        }

        // q = s.crossProduct(e1)
        final double qx = sy * e1z - sz * e1y;
        final double qy = sz * e1x - sx * e1z;
        final double qz = sx * e1y - sy * e1x;

        // v = f * dir.dotProduct(q)
        double v = f * (dir.x * qx + dir.y * qy + dir.z * qz);

        if (v < 0.0 || u + v > 1.0) {
            return false;
        }

        // t = f * e2.dotProduct(q)
        final double t = f * (e2x * qx + e2y * qy + e2z * qz);

        if (t >= pickRay.getNearClip() && t <= pickRay.getFarClip()) {
            // This branch is entered only for hit triangles (not so often),
            // so we can get smoothly back to the nice code using Point3Ds.

            if (cullFace != CullFace.NONE) {
                // normal = e1.crossProduct(e2)
                final Point3D normal = new Point3D(
                    e1y * e2z - e1z * e2y,
                    e1z * e2x - e1x * e2z,
                    e1x * e2y - e1y * e2x);

                final double nangle = normal.angle(
                        new Point3D(-dir.x, -dir.y, -dir.z));
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

            // centroid = computeCentroid(v0, v1, v2);
            final Point3D centroid = computeCentroid(
                    v0x, v0y, v0z,
                    v1x, v1y, v1z,
                    v2x, v2y, v2z);

            // cv0 = v0.subtract(centroid)
            final Point3D cv0 = new Point3D(
                    v0x - centroid.getX(),
                    v0y - centroid.getY(),
                    v0z - centroid.getZ());
            // cv1 = v1.subtract(centroid)
            final Point3D cv1 = new Point3D(
                    v1x - centroid.getX(),
                    v1y - centroid.getY(),
                    v1z - centroid.getZ());
            // cv2 = v2.subtract(centroid)
            final Point3D cv2 = new Point3D(
                    v2x - centroid.getX(),
                    v2y - centroid.getY(),
                    v2z - centroid.getZ());

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
            int texCoordElementSize = getVertexFormat().getTexCoordElementSize();
            int texCoordOffset = getVertexFormat().getTexCoordIndexOffset();
            final int t0Idx = faces.get(faceIndex + texCoordOffset) * texCoordElementSize;
            final int t1Idx = faces.get(faceIndex + vertexIndexSize + texCoordOffset) * texCoordElementSize;
            final int t2Idx = faces.get(faceIndex + (vertexIndexSize * 2) + texCoordOffset) * texCoordElementSize;

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
                    reportFace ? faceIndex / getFaceElementSize() : PickResult.FACE_UNDEFINED,
                    point, txCoords);
            return true;
        }

        return false;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeIntersects(PickRay pickRay, PickResultChooser pickResult,
            Node candidate, CullFace cullFace, boolean reportFace) {

        boolean found = false;
        if (validate()) {
            final int size = faces.size();

            final Vec3d o = pickRay.getOriginNoClone();

            final Vec3d d = pickRay.getDirectionNoClone();

            for (int i = 0; i < size; i += getFaceElementSize()) {
                if (computeIntersectsFace(pickRay, o, d, i, cullFace, candidate,
                        reportFace, pickResult)) {
                    found = true;
                }
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
            isValidDirty = true;
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
        public float[] syncTo(float[] array, int[] fromAndLengthIndices) {
            assert ((fromAndLengthIndices != null) && (fromAndLengthIndices.length == 2));
            ObservableFloatArray floatArray = (ObservableFloatArray) this.array;
            if (dirtyInFull || array == null || array.length != floatArray.size()) {
                // Always allocate a new array when size changes
                fromAndLengthIndices[0] = 0;
                fromAndLengthIndices[1] = floatArray.size();
                return floatArray.toArray(null);
            }
            fromAndLengthIndices[0] = dirtyRangeFrom;
            fromAndLengthIndices[1] = dirtyRangeLength;
            floatArray.copyTo(dirtyRangeFrom, array, dirtyRangeFrom, dirtyRangeLength);
            return array;
        }

        @Override
        public int[] syncTo(int[] array, int[] fromAndLengthIndices) {
            assert ((fromAndLengthIndices != null) && (fromAndLengthIndices.length == 2));
            ObservableIntegerArray intArray = (ObservableIntegerArray) this.array;
            if (dirtyInFull || array == null || array.length != intArray.size()) {
                fromAndLengthIndices[0] = 0;
                fromAndLengthIndices[1] = intArray.size();
                // Always allocate a new array when size changes
                return intArray.toArray(null);
            }
            fromAndLengthIndices[0] = dirtyRangeFrom;
            fromAndLengthIndices[1] = dirtyRangeLength;
            intArray.copyTo(dirtyRangeFrom, array, dirtyRangeFrom, dirtyRangeLength);
            return array;
        }
    }
}
