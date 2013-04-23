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
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.sg.PGTriangleMesh;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
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
 * @since JavaFX 8
 */
public class TriangleMesh extends Mesh {

    public static final int NUM_COMPONENTS_PER_POINT = 3;
    public static final int NUM_COMPONENTS_PER_TEXCOORD = 2;
    public static final int NUM_COMPONENTS_PER_FACE = 6;

    private float[] points;
    private float[] texCoords;
    private int[] faces;
    private int[] faceSmoothingGroups;

    private boolean pointsDirty = true;
    private boolean texCoordsDirty = true;
    private boolean facesDirty = true;
    private boolean fsgDirty = true;

    // Partial Update constants and variables
    private static final int RANGE_INDEX = 0;
    private static final int RANGE_LENGTH = 1;
    private static final int MAX_RANGE_SIZE = 2;
    private int refCount = 1;
    private boolean pointUpdateRange = false;   
    private int[] pointRangeInfos;
    private boolean texCoordUpdateRange = false;
    private int[] texCoordRangeInfos;
    private boolean faceUpdateRange = false;
    private int[] faceRangeInfos;
    private boolean fsgUpdateRange = false;
    private int[] fsgRangeInfos;

    private BaseBounds cachedBounds;

    /**
     * Creates a new instance of {@code TriangleMesh} class.
     */
    public TriangleMesh() {
    }
    
    /**
     * Creates a new instance of {@code TriangleMesh} class.
     * TODO: 3D - doc. follows array semantic
     *
     * @param points points array (points.length must be divisible by NUM_COMPONENTS_PER_POINT)
     * @param texCoords texCoords array (texCoords.length must be divisible by
     * NUM_COMPONENTS_PER_TEXCOORD)
     * @param faces faces (or triangles) array (faces.length must be divisible
     * by NUM_COMPONENTS_PER_FACE)
     */
    public TriangleMesh(float[] points, float[] texCoords, int[] faces) {
        setPoints(points);
        setTexCoords(texCoords);
        setFaces(faces);
    }

    /**
     * The total number of points of this {@code TriangleMesh}
     */
    private ReadOnlyIntegerWrapper pointCount;

    final void setPointCount(int value) {
        pointCountPropertyImpl().set(value);
    }

    /**
     * Retrieve total number of points of this {@code TriangleMesh}
     *
     * @return the total number of points
     */
    public final int getPointCount() {
        return pointCount == null ? 0 : pointCount.get();
    }

    public ReadOnlyIntegerProperty pointCountProperty() {
        return pointCountPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper pointCountPropertyImpl() {
        if (pointCount == null) {
            pointCount = new ReadOnlyIntegerWrapper(this, "pointCount");
        }
        return pointCount;
    }

    /**
     * The total number of texture coordinates of this {@code TriangleMesh}
     */
    private ReadOnlyIntegerWrapper texCoordCount;

    final void setTexCoordCount(int value) {
        texCoordCountPropertyImpl().set(value);
    }

    /**
     * Retrieve total number of texture coordinates of this {@code TriangleMesh}
     *
     * @return the total number of texture coordinates
     */
    public final int getTexCoordCount() {
        return texCoordCount == null ? 0 : texCoordCount.get();
    }

    public ReadOnlyIntegerProperty texCoordCountProperty() {
        return texCoordCountPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper texCoordCountPropertyImpl() {
        if (texCoordCount == null) {
            texCoordCount = new ReadOnlyIntegerWrapper(this, "texCoordCount");
        }
        return texCoordCount;
    }

    /**
     * The total number of faces of this {@code TriangleMesh}
     */
    private ReadOnlyIntegerWrapper faceCount;

    final void setFaceCount(int value) {
        faceCountPropertyImpl().set(value);
    }

    /**
     * Retrieve total number of faces of this {@code TriangleMesh}
     *
     * @return the total number of faces
     */
    public final int getFaceCount() {
        return faceCount == null ? 0 : faceCount.get();
    }

    public ReadOnlyIntegerProperty faceCountProperty() {
        return faceCountPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper faceCountPropertyImpl() {
        if (faceCount == null) {
            faceCount = new ReadOnlyIntegerWrapper(this, "faceCount");
        }
        return faceCount;
    }
    
    /**
     * The total number of faceSmoothingGroups of this {@code TriangleMesh}
     */
    private ReadOnlyIntegerWrapper faceSmoothingGroupCount;

    final void setFaceSmoothingGroupCount(int value) {
        faceSmoothingGroupCountPropertyImpl().set(value);
    }

    /**
     * Retrieve total number of faceSmoothingGroups of this {@code TriangleMesh}
     *
     * @return the total number of faceSmoothingGroups
     */
    public final int getFaceSmoothingGroupCount() {
        return faceSmoothingGroupCount == null ? 0 : faceSmoothingGroupCount.get();
    }

    public ReadOnlyIntegerProperty faceSmoothingGroupCountProperty() {
        return faceSmoothingGroupCountPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper faceSmoothingGroupCountPropertyImpl() {
        if (faceSmoothingGroupCount == null) {
            faceSmoothingGroupCount = new ReadOnlyIntegerWrapper(this, "faceSmoothingGroupCount");
        }
        return faceSmoothingGroupCount;
    }

    /**
     * Sets the points of this {@code TriangleMesh}
     * 
     * @param points source array of NUM_COMPONENTS_PER_POINT * n values containing n new points.
     */
    public final void setPoints(float[] points) {
        // Check that points.length is divisible by NUM_COMPONENTS_PER_POINT
        if ((points.length % NUM_COMPONENTS_PER_POINT) != 0) {
            throw new IllegalArgumentException("points.length has to be divisible by NUM_COMPONENTS_PER_POINT." 
                    + " It is to store multiple x, y, and z coordinates of this mesh");
        }

        if ((this.points == null) || (this.points.length < points.length)) {
            this.points = new float[points.length];
        }
        System.arraycopy(points, 0, this.points, 0, points.length);
        // Store the valid point count.
        // Note this.points.length can be bigger than points.length.
        setPointCount(points.length / NUM_COMPONENTS_PER_POINT);

        pointsDirty = true;
        setDirty(true);
    }
    
    /**
     * Sets the points associated with this {@code TriangleMesh}
     * starting at the specified {@code index} using data in {@code points} 
     * starting at index {@code start} for {@code length} number of points.
     * 
     * @param index the starting destination index in this TriangleMesh's points array
     * @param points source array of floats containing the new points
     * @param start starting source index in the points array.
     * @param length number of point elements to be copied.
     */
    public final void setPoints(int index, float[] points,
                      int start, int length) {

        if (index < 0 || start < 0 || length < 0) {
            throw new IllegalArgumentException("index, start and length have to be non-zero");
        }
        int startOffset = start * NUM_COMPONENTS_PER_POINT;
        int lengthInFloatUnit = length * NUM_COMPONENTS_PER_POINT;
        if ((startOffset >= points.length) || ((startOffset + lengthInFloatUnit) > points.length)) {
            throw new IllegalArgumentException("start or (start + length) is out of range for input points");
        }
        int indexOffset = index * NUM_COMPONENTS_PER_POINT;
        int pointCountInFloatUnit = getPointCount() * NUM_COMPONENTS_PER_POINT;
        if ((indexOffset >= pointCountInFloatUnit) || 
                ((indexOffset + lengthInFloatUnit) > pointCountInFloatUnit)) {
            throw new IllegalArgumentException("index or (index + length) is out of range for this triangle mesh's points");
        }
        System.arraycopy(points, startOffset, this.points, indexOffset, lengthInFloatUnit);

        if (pointRangeInfos == null) {
            pointRangeInfos = new int[MAX_RANGE_SIZE];
        }

        if (!pointUpdateRange) {
            pointsDirty = pointUpdateRange = true;
            pointRangeInfos[RANGE_INDEX] = index;
            pointRangeInfos[RANGE_LENGTH] = length;
        } else {
            pointsDirty = true;
            int fromIndex = Math.min(pointRangeInfos[RANGE_INDEX], index);
            int toIndex = Math.max(pointRangeInfos[RANGE_INDEX] + pointRangeInfos[RANGE_LENGTH], index + length);
            pointRangeInfos[RANGE_INDEX] = fromIndex;
            pointRangeInfos[RANGE_LENGTH] = toIndex - fromIndex;
        }
        
        setDirty(true);
    }
    
    /**
     * Gets the points of this {@code TriangleMesh}
     *
     * @param points a float array that will receive the points
     * if it not null and has sufficient capacity.
     * @return a float array of points
     */
    public final float[] getPoints(float[] points) {
        if (this.points == null) {
            return null;
        }
        int pointCountInFloatUnit = getPointCount() * NUM_COMPONENTS_PER_POINT;
        if ((points == null) || (pointCountInFloatUnit > points.length)) {
            points = new float[pointCountInFloatUnit];
        }
        System.arraycopy(this.points, 0, points, 0, pointCountInFloatUnit);
        return points;
    }

    /**
     * Gets the points associated with this {@code TriangleMesh} starting at the
     * specified {@code index} for {@code length} number of points.
     * 
     * @param index starting source points index in this {@code TriangleMesh}
     * @param points destination array that will receive this {@code TriangleMesh}'s points data
     * @param length number of point elements to be copied
     * @return a float array of points
     */
    public final float[] getPoints(int index, float[] points, int length) {
        if (index < 0 || length < 0) {
            throw new IllegalArgumentException("index and length have to be non-zero");
        }

        int lengthInFloatUnit = length * NUM_COMPONENTS_PER_POINT;
        if (lengthInFloatUnit > points.length) {
            throw new IllegalArgumentException("length is out of range for input points");
        }
        int indexOffset = index * NUM_COMPONENTS_PER_POINT;
        int pointCountInFloatUnit = getPointCount() * NUM_COMPONENTS_PER_POINT;
        if ((indexOffset >= pointCountInFloatUnit) || 
                ((indexOffset + lengthInFloatUnit) > pointCountInFloatUnit)) {
            throw new IllegalArgumentException("index or (index + length) is out of range for this triangle mesh's points");
        }

        if (this.points == null) {
            return null;
        }
        System.arraycopy(this.points, indexOffset, points, 0, lengthInFloatUnit);
        return points;
    }
    
    /**
     * Sets the texture coordinates of this {@code TriangleMesh}.
     * 
     * @param texCoords source array of NUM_COMPONENTS_PER_TEXCOORD * n values containing n new texCoords.
     */
    public final void setTexCoords(float[] texCoords) {
        // Check that texCoords.length is divisible by NUM_COMPONENTS_PER_TEXCOORD
        if ((texCoords.length % NUM_COMPONENTS_PER_TEXCOORD) != 0) {
            throw new IllegalArgumentException("texCoords.length has to be divisible by NUM_COMPONENTS_PER_TEXCOORD."
                    +" It is to store multiple u and v texture coordinates of this mesh");
        }

        if ((this.texCoords == null) || (this.texCoords.length < texCoords.length)) {
            this.texCoords = new float[texCoords.length];
        }
        System.arraycopy(texCoords, 0, this.texCoords, 0, texCoords.length);
        // Store the valid texCoords count.
        // Note this.texCoords.length can be bigger than texCoords.length.
        setTexCoordCount(texCoords.length / NUM_COMPONENTS_PER_TEXCOORD);

        texCoordsDirty = true;
        setDirty(true);
    }

    /**
     * Sets the texture coordinates associated with this {@code TriangleMesh}
     * starting at the specified {@code index} using data in {@code texCoords}
     * starting at index {@code start} for {@code length} number of texCoords.
     * 
     * @param index the starting destination index in this TriangleMesh's texCoords array
     * @param texCoords an float array containing the new texture coordinates
     * @param start starting source index in the texture coordinates array
     * @param length number of texCoord elements to be copied.
     */
    public final void setTexCoords(int index, float[] texCoords, int start,
            int length) {
        if (index < 0 || start < 0 || length < 0) {
            throw new IllegalArgumentException("index, start and length have to be non-zero");
        }
        int startOffset = start * NUM_COMPONENTS_PER_TEXCOORD;
        int lengthInFloatUnit = length * NUM_COMPONENTS_PER_TEXCOORD;
        if ((startOffset >= texCoords.length) || ((startOffset + lengthInFloatUnit) > texCoords.length)) {
            throw new IllegalArgumentException("start or (start + length) is out of range for input texCoords");
        }
        int indexOffset = index * NUM_COMPONENTS_PER_TEXCOORD;
        int texCoordCountInFloatUnit = getTexCoordCount() * NUM_COMPONENTS_PER_TEXCOORD;
        if ((indexOffset >= texCoordCountInFloatUnit) || 
                ((indexOffset + lengthInFloatUnit) > texCoordCountInFloatUnit)) {
            throw new IllegalArgumentException("index or (index + length) is out of range for this triangle mesh's texCoords");
        }
        System.arraycopy(texCoords, startOffset, this.texCoords, indexOffset, lengthInFloatUnit);
      
        if (texCoordRangeInfos == null) {
            texCoordRangeInfos = new int[MAX_RANGE_SIZE];
        }
        if (!texCoordUpdateRange) {
            texCoordsDirty = texCoordUpdateRange = true;
            texCoordRangeInfos[RANGE_INDEX] = index;
            texCoordRangeInfos[RANGE_LENGTH] = length;
        } else {
            texCoordsDirty = true;
            int fromIndex = Math.min(texCoordRangeInfos[RANGE_INDEX], index);
            int toIndex = Math.max(texCoordRangeInfos[RANGE_INDEX] + texCoordRangeInfos[RANGE_LENGTH], index + length);
            texCoordRangeInfos[RANGE_INDEX] = fromIndex;
            texCoordRangeInfos[RANGE_LENGTH] = toIndex - fromIndex;            
        }
        setDirty(true);
    }

    /**
     * Gets the texture coordinates of this {@code TriangleMesh}.
     *
     * @param texCoords a float array that will receive the texture coordinates
     * if it not null and has sufficient capacity
     * @return a float array of texture coordinates
     */
    public final float[] getTexCoords(float[] texCoords) {
        if (this.texCoords == null) {
            return null;
        }

        int texCoordCountInFloatUnit = getTexCoordCount() * NUM_COMPONENTS_PER_TEXCOORD;
        if ((texCoords == null) || (texCoordCountInFloatUnit > texCoords.length)) {
            texCoords = new float[texCoordCountInFloatUnit];
        }
        System.arraycopy(this.texCoords, 0, texCoords, 0, texCoordCountInFloatUnit);
        return texCoords;
    }

    /**
     * Gets the texture coordinates associated with this {@code TriangleMesh}
     * starting at the specified {@code index} for {@code length} number of
     * texCoords.
     * 
     * @param index starting source texCoords index in this {@code TriangleMesh}
     * @param texCoords destination array that will receive this {@code TriangleMesh}'s texCoords data 
     * @param length number of texCoord elements to be copied
     * @return a float array of texture coordinates
     */
    public final float[] getTexCoords(int index, float[] texCoords, int length) {
        if (index < 0 || length < 0) {
            throw new IllegalArgumentException("index and length have to be non-zero");
        }

        int lengthInFloatUnit = length * NUM_COMPONENTS_PER_TEXCOORD;
        if (lengthInFloatUnit > texCoords.length) {
            throw new IllegalArgumentException("length is out of range for input texCoords");
        }
        int indexOffset = index * NUM_COMPONENTS_PER_TEXCOORD;
        int texCoordCountInFloatUnit = getTexCoordCount() * NUM_COMPONENTS_PER_TEXCOORD;
        if ((indexOffset >= texCoordCountInFloatUnit) || 
                ((indexOffset + lengthInFloatUnit) > texCoordCountInFloatUnit)) {
            throw new IllegalArgumentException("index or (index + length) is out of range for this triangle mesh's texCoords");
        }

        if (this.texCoords == null) {
            return null;
        }
        System.arraycopy(this.texCoords, indexOffset, texCoords, 0, lengthInFloatUnit);
        return texCoords;
    }

    /**
     * Sets the faces, indices into the points and texCoords arrays,
     * associated with this {@code TriangleMesh}.
     * 
     * @param faces source array of NUM_COMPONENTS_PER_FACE * n indices 
     * (3 point indices and 3 texCood indices) containing n new faces
     */
    public final void setFaces(int[] faces) {
        // Check that faces.length is divisible by NUM_COMPONENTS_PER_FACE
        if ((faces.length % NUM_COMPONENTS_PER_FACE) != 0) {
            throw new IllegalArgumentException("faces.length has to be divisible by NUM_COMPONENTS_PER_FACE.");
        }

        if ((this.faces == null) || (this.faces.length < faces.length)) {
            this.faces = new int[faces.length];
        }
        System.arraycopy(faces, 0, this.faces, 0, faces.length);
        // Store the valid face count.
        // Note this.faces.length can be bigger than faces.length.
        setFaceCount(faces.length / NUM_COMPONENTS_PER_FACE);

        facesDirty = true;
        setDirty(true);
    }

    /**
     * Sets the faces, indices into the points and texCoords arrays, 
     * associated with this {@code TriangleMesh}
     * starting at the specified{@code index} using data in {@code faces} 
     * starting at index {@code start} for {@code length} number of faces.
     * 
     * @param index the starting destination index in this TriangleMesh's faces array
     * @param faces an int array containing the new interleaved vertices
     * @param start starting source index in the faces array.
     * @param length number of interleaved vertex elements to be copied
     */
    public final void setFaces(int index, int[] faces, int start, int length) {
        if (index < 0 || start < 0 || length < 0) {
            throw new IllegalArgumentException("index, start and length have to be non-zero");
        }
        int startOffset = start * NUM_COMPONENTS_PER_FACE;
        int lengthInIntUnit = length * NUM_COMPONENTS_PER_FACE;
        if ((startOffset >= faces.length) || ((startOffset + lengthInIntUnit) > faces.length)) {
            throw new IllegalArgumentException("start or (start + length) is out of range for input faces");
        }
        int indexOffset = index * NUM_COMPONENTS_PER_FACE;
        int faceCountInIntUnit = getFaceCount() * NUM_COMPONENTS_PER_FACE;
        if ((indexOffset >= faceCountInIntUnit) || 
                ((indexOffset + lengthInIntUnit) > faceCountInIntUnit)) {
            throw new IllegalArgumentException("index or (index + length) is out of range for this triangle mesh's faces");
        }
        System.arraycopy(faces, startOffset, this.faces, indexOffset, lengthInIntUnit);

        if (faceRangeInfos == null) {
            faceRangeInfos = new int[MAX_RANGE_SIZE];
        }
        if (!faceUpdateRange) {
            facesDirty = faceUpdateRange = true;
            faceRangeInfos[RANGE_INDEX] = index;
            faceRangeInfos[RANGE_LENGTH] = length;
        } else {
            facesDirty = true;
            int fromIndex = Math.min(faceRangeInfos[RANGE_INDEX], index);
            int toIndex = Math.max(faceRangeInfos[RANGE_INDEX] + faceRangeInfos[RANGE_LENGTH], index + length);
            faceRangeInfos[RANGE_INDEX] = fromIndex;
            faceRangeInfos[RANGE_LENGTH] = toIndex - fromIndex;
        }
        setDirty(true);
    }

    /**
     * Gets the faces, indices into the points and texCoords arrays, of this 
     * {@code TriangleMesh}
     *
     * @param faces an int array that will receive the faces if it not null and 
     * has sufficient capacity.
     * @return an int array of faces
     * 
     */
    public final int[] getFaces(int[] faces) {
        if (this.faces == null) {
            return null;
        }

        int faceCountInIntUnit = getFaceCount() * NUM_COMPONENTS_PER_FACE;
        if ((faces == null) || (faceCountInIntUnit > faces.length)) {
            faces = new int[faceCountInIntUnit];
        }
        System.arraycopy(this.faces, 0, faces, 0, faceCountInIntUnit);
        return faces;
    }

    /**
     * Gets the faces, indices into the points and texCoords arrays,
     * associated with this {@code TriangleMesh} starting at the specified
     * {@code index} for {@code length} number of faces.
     * 
     * @param index starting source faces index in this {@code TriangleMesh}
     * @param faces destination array that will receive this {@code TriangleMesh}'s faces data
     * @param length number of face elements to be copied
     * @return an int array of faces
     */
    public final int[] getFaces(int index, int[] faces, int length) {
        if (index < 0 || length < 0) {
            throw new IllegalArgumentException("index and length have to be non-zero");
        }

        int lengthInIntUnit = length * NUM_COMPONENTS_PER_FACE;
        if (lengthInIntUnit > faces.length) {
            throw new IllegalArgumentException("length is out of range for input faces");
        }
        int indexOffset = index * NUM_COMPONENTS_PER_FACE;
        int faceCountInIntUnit = getFaceCount() * NUM_COMPONENTS_PER_FACE;
        if ((indexOffset >= faceCountInIntUnit) || 
                ((indexOffset + lengthInIntUnit) > faceCountInIntUnit)) {
            throw new IllegalArgumentException("index or (index + length) is out of range for this triangle mesh's faces");
        }

        if (this.faces == null) {
            return null;
        }
        System.arraycopy(this.faces, indexOffset, faces, 0, lengthInIntUnit);
        return faces;
    }
    
    /**
     * Sets the face smoothing group for each face in this {@code TriangleMesh}
     * Smoothing affects how a mesh is rendered but it does not effect its
     * geometry. The face smoothing group value is used to control the smoothing
     * between adjacent faces.
     *
     * The face smoothing group is represented by an array of bits and up to 32
     * unique groups is possible. The face smoothing group value can range from
     * zero to all 32 groups. A face is said to belong to a group is by having
     * the associated bit set. A value of 0 implies no smoothing group or hard
     * edges. A face can have no or more smoothing groups. Smoothing is applied
     * when adjacent pair of faces shared a smoothing group. Otherwise the faces
     * are rendered with a hard edge between them.
     *
     * A null faceSmoothingGroups implies all faces in this mesh have a
     * smoothing group value of 1.
     *
     * Note: If faceSmoothingGroups is not null, faceSmoothingGroups.length must
     * be equal to faces.length/NUM_COMPONENTS_PER_FACE.
     */
    public final void setFaceSmoothingGroups(int[] faceSmoothingGroups) {
        if (faceSmoothingGroups == null) {
            this.faceSmoothingGroups = null;
            setFaceSmoothingGroupCount(0);
        } else {
            // Check that faceSmoothingGroups.length is 1/NUM_COMPONENTS_PER_FACE of faces.length
            if (faceSmoothingGroups.length != (faces.length / NUM_COMPONENTS_PER_FACE)) {
                throw new IllegalArgumentException("faceSmoothingGroups.length has to be equal to (faces.length / NUM_COMPONENTS_PER_FACE).");
            }

            if ((this.faceSmoothingGroups == null)
                    || (this.faceSmoothingGroups.length < faceSmoothingGroups.length)) {
                this.faceSmoothingGroups = new int[faceSmoothingGroups.length];
            }
            System.arraycopy(faceSmoothingGroups, 0, this.faceSmoothingGroups, 0, faceSmoothingGroups.length);
            // Store the valid faceSmoothingGroup count.
            // Note this.faceSmoothingGroups.length can be bigger than faceSmoothingGroups.length.
            setFaceSmoothingGroupCount(faceSmoothingGroups.length);
        }

        fsgDirty = true;
        setDirty(true);
    }

    /**
     * Sets the faceSmoothingGroups associated with this {@code TriangleMesh}
     * starting at the specified {@code index} using data in {@code faceSmoothingGroups} 
     * starting at index {@code start} for {@code length} number of faceSmoothingGroups.
     * The face smoothing group value is used to control the smoothing
     * between adjacent faces.
     *
     * The face smoothing group is represented by an array of bits and up to 32
     * unique groups is possible. The face smoothing group value can range from
     * zero to all 32 groups. A face is said to belong to a group is by having
     * the associated bit set. A value of 0 implies no smoothing group or hard
     * edges. A face can have no or more smoothing groups. Smoothing is applied
     * when adjacent pair of faces shared a smoothing group. Otherwise the faces
     * are rendered with a hard edge between them.
     *
     * @param index the starting destination index in this TriangleMesh's faceSmoothingGroups array
     * @param points source array of floats containing the new faceSmoothingGroups
     * @param start starting source index in the faceSmoothingGroups array.
     * @param length number of faceSmoothingGroup elements to be copied.
     */
    public final void setFaceSmoothingGroups(int index, int[] faceSmoothingGroups,
                      int start, int length) {

        if (index < 0 || start < 0 || length < 0) {
            throw new IllegalArgumentException("index, start and length have to be non-zero");
        }
        
        if ((start >= faceSmoothingGroups.length) || ((start + length) > faceSmoothingGroups.length)) {
            throw new IllegalArgumentException("start or (start + length) is out of range for input faceSmoothingGroups");
        }
        int fsgCount = getFaceSmoothingGroupCount();
        if ((index >= fsgCount) || 
                ((index + length) > fsgCount)) {
            throw new IllegalArgumentException("index or (index + length) is out of range for this triangle mesh's faceSmoothingGroups");
        }

        System.arraycopy(faceSmoothingGroups, start, this.faceSmoothingGroups, index, length);
              
        if (fsgRangeInfos == null) {
            fsgRangeInfos = new int[MAX_RANGE_SIZE];
        }
         if (!fsgUpdateRange) {
            fsgDirty = fsgUpdateRange = true;
            fsgRangeInfos[RANGE_INDEX] = index;
            fsgRangeInfos[RANGE_LENGTH] = length;
        } else {
            fsgDirty = true;
            int fromIndex = Math.min(fsgRangeInfos[RANGE_INDEX], index);
            int toIndex = Math.max(fsgRangeInfos[RANGE_INDEX] + fsgRangeInfos[RANGE_LENGTH], index + length);
            fsgRangeInfos[RANGE_INDEX] = fromIndex;
            fsgRangeInfos[RANGE_LENGTH] = toIndex - fromIndex;
        }
        setDirty(true);
    }

    /**
     * Gets the face smoothing group for each face in this {@code TriangleMesh}
     * @return an int array to smoothing group bits for each face
     */
    public final int[] getFaceSmoothingGroups(int[] faceSmoothingGroups) {
        if (this.faceSmoothingGroups == null) {
            return null;
        }

        int fsgCount = getFaceSmoothingGroupCount();
        if ((faceSmoothingGroups == null) || 
                (fsgCount > faceSmoothingGroups.length)) {
            faceSmoothingGroups = new int[fsgCount];
        }
        System.arraycopy(this.faceSmoothingGroups, 0, faceSmoothingGroups, 0, fsgCount);
        return faceSmoothingGroups;
    }

    /**
     * Gets the face smoothing group for each face in this {@code TriangleMesh}
     * starting at the specified {@code index} for {@code length} number of face
     * smoothing groups.
     * 
     * @param index starting source face smoothing groups index in this {@code TriangleMesh}
     * @param faceSmoothingGroups destination array that will receive this 
     * {@code TriangleMesh}'s faceSmoothingGroups data
     * @param length number of faceSmoothingGroup elements to be copied
     * @return an int array of faceSmoothingGroups
     */
    public final int[] getFaceSmoothingGroups(int index, int[] faceSmoothingGroups, int length) {
        if (index < 0 || length < 0) {
            throw new IllegalArgumentException("index and length have to be non-zero");
        }

        if (length > faceSmoothingGroups.length) {
            throw new IllegalArgumentException("length is out of range for input faceSmoothingGroups");
        }
        
        int fsgCount = getFaceSmoothingGroupCount();
        if ((index >= fsgCount) || ((index + length) > fsgCount)) {
            throw new IllegalArgumentException("index or (index + length) is out of range for this triangle mesh's faceSmoothingGroups");
        }

        if (this.faceSmoothingGroups == null) {
            return null;
        }
        System.arraycopy(this.faceSmoothingGroups, index, faceSmoothingGroups, 0, length);
        return faceSmoothingGroups;
    }

    @Override
    void setDirty(boolean value) {
        super.setDirty(value);
        if (!value) { // false
            pointsDirty = false;
            texCoordsDirty = false;
            facesDirty = false;
            fsgDirty = false;
            pointUpdateRange = false;
            texCoordUpdateRange = false;
            faceUpdateRange = false;
            fsgUpdateRange = false;
            // We don't clear up XXXPartialUpdateInfos array since we will
            // overwrite every element when we update the array.   
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
        if (this.refCount == 0) {
            release();
        }
    }

    void release(){
        // TODO: 3D - release native resoure
    }

    private PGTriangleMesh peer;
    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    /** The peer node created by the graphics Toolkit/Pipeline implementation */
    PGTriangleMesh impl_getPGTriangleMesh() {
        if (peer == null) {
            peer = Toolkit.getToolkit().createPGTriangleMesh();
        }
        return peer;
    }

    @Override
    PGTriangleMesh getPGMesh() {
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

        PGTriangleMesh pgTriMesh = impl_getPGTriangleMesh();
        // sync points 
        if (pointsDirty) {
            if (pointUpdateRange) {
                pgTriMesh.setPoints(points, pointRangeInfos[RANGE_INDEX],
                        pointRangeInfos[RANGE_LENGTH]);
            } else {
                pgTriMesh.setPoints(points);
            }
        }
        // sync texCoords
        if (texCoordsDirty) {
            if (texCoordUpdateRange) {
                pgTriMesh.setTexCoords(texCoords, texCoordRangeInfos[RANGE_INDEX],
                                       texCoordRangeInfos[RANGE_LENGTH]);
            } else {
                pgTriMesh.setTexCoords(texCoords);
            }
        }
        // sync faces
        if (facesDirty) {
            if (faceUpdateRange) {
                pgTriMesh.setFaces(faces, faceRangeInfos[RANGE_INDEX],
                                   faceRangeInfos[RANGE_LENGTH]);
            } else {
                pgTriMesh.setFaces(faces);
            }
        }
        // sync faceSmoothingGroups
        if (fsgDirty) {
            if (fsgUpdateRange) {
                pgTriMesh.setFaceSmoothingGroups(faceSmoothingGroups, fsgRangeInfos[RANGE_INDEX],
                                                 fsgRangeInfos[RANGE_LENGTH]);
            } else {
                pgTriMesh.setFaceSmoothingGroups(faceSmoothingGroups);
            }
        }

        setDirty(false);
    }

    @Override
    BaseBounds computeBounds(BaseBounds bounds) {
        if (isDirty() || cachedBounds == null) {
            cachedBounds = new BoxBounds();

            final double len = points.length;
            for (int i = 0; i < len; i += 3) {
                cachedBounds.add(points[i], points[i + 1], points[i + 2]);
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

        final int v0Idx = faces[faceIndex] * 3;
        final int v1Idx = faces[faceIndex + 2] * 3;
        final int v2Idx = faces[faceIndex + 4] * 3;

        final Point3D v0 = new Point3D(points[v0Idx], points[v0Idx + 1], points[v0Idx + 2]);
        final Point3D v1 = new Point3D(points[v1Idx], points[v1Idx + 1], points[v1Idx + 2]);
        final Point3D v2 = new Point3D(points[v2Idx], points[v2Idx + 1], points[v2Idx + 2]);

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

        final double minDistance = pickRay.isParallel()
                ? Double.NEGATIVE_INFINITY : 0.0;
        if (t >= minDistance) {
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

            final int t0Idx = faces[faceIndex + 1] * 2;
            final int t1Idx = faces[faceIndex + 3] * 2;
            final int t2Idx = faces[faceIndex + 5] * 2;

            final Point2D u0 = new Point2D(texCoords[t0Idx], texCoords[t0Idx + 1]);
            final Point2D u1 = new Point2D(texCoords[t1Idx], texCoords[t1Idx + 1]);
            final Point2D u2 = new Point2D(texCoords[t2Idx], texCoords[t2Idx + 1]);

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
        final int size = faces.length;

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
}
