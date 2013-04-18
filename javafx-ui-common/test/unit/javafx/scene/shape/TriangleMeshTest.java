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

import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;

public class TriangleMeshTest {

    /**
     * Test of setFaceSmoothingGroups method, of class TriangleMesh.
     */
    @Test
    public void testSetFaceSmoothingGroups_intArr() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        instance.setFaceSmoothingGroups(faceSmoothingGroups);
        assertTrue(instance.getFaceSmoothingGroupCount() == faceSmoothingGroups.length);
        assertArrayEquals(faceSmoothingGroups, instance.getFaceSmoothingGroups(null));
    }

    /**
     * Test of setFaceSmoothingGroups method, of class TriangleMesh.
     */
    @Test
    public void testSetFaceSmoothingGroups_4args() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        int[] setterArray = new int[]{2, 4, 8};
        int[] expected = new int[setterArray.length];
        int index = 1;
        int start = 0;
        int length = setterArray.length;
        instance.setFaceSmoothingGroups(faceSmoothingGroups);
        instance.setFaceSmoothingGroups(index, setterArray, start, length);
        assertArrayEquals(setterArray, instance.getFaceSmoothingGroups(index, expected, length));
    }

    /**
     * Test faceSmoothingGroups with illegal value of setFaceSmoothingGroups
     * method, of class TriangleMesh.
     */
    @Test (expected=IllegalArgumentException.class)
    public void testSetFaceSmoothingGroups_4argsValueOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        int[] setterArray = new int[]{2, 0, -1};
        int index = 0;
        int start = 0;
        int length = setterArray.length;
        instance.setFaceSmoothingGroups(index, setterArray, start, length); // expect IllegalArgumentException
        // faceSmoothingGroups should not change
        assertArrayEquals(faceSmoothingGroups, instance.getFaceSmoothingGroups(null));
    }

    /**
     * Test setFaceSmoothingGroups with illegal value of setFaceSmoothingGroups
     * method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaceSmoothingGroups_4argsIllegalArgument() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        instance.setFaceSmoothingGroups(faceSmoothingGroups);
        int[] setterArray = new int[]{2, 0, 1};
        int index = 0;
        int start = 0;
        instance.setFaceSmoothingGroups(index, setterArray, start, -1); // expect IllegalArgumentException
        // faceSmoothingGroups should not change
        assertArrayEquals(faceSmoothingGroups, instance.getFaceSmoothingGroups(null));
    }

    /**
     * Test setFaceSmoothingGroups with illegal value of setFaceSmoothingGroups
     * method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaceSmoothingGroups_4argsIndexOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        int[] setterArray = new int[]{2, 0, 1};
        int start = 0;
        int length = setterArray.length;
        instance.setFaceSmoothingGroups(faceSmoothingGroups);
        instance.setFaceSmoothingGroups(198, setterArray, start, length); // expect IllegalArgumentException
        // faceSmoothingGroups should not change
        assertArrayEquals(faceSmoothingGroups, instance.getFaceSmoothingGroups(null));
    }

    /**
     * Test setFaceSmoothingGroups with illegal value of setFaceSmoothingGroups
     * method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaceSmoothingGroups_4argsStartOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        int[] setterArray = new int[]{2, 0, 1};
        int index = 0;
        int length = setterArray.length;
        instance.setFaceSmoothingGroups(faceSmoothingGroups);
        instance.setFaceSmoothingGroups(index, setterArray, 2, length); // expect IllegalArgumentException
        // faceSmoothingGroups should not change
        assertArrayEquals(faceSmoothingGroups, instance.getFaceSmoothingGroups(null));
    }

    /**
     * Test of setFaces method, of class TriangleMesh.
     */
    @Test
    public void testSetFaces_4args() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 1200 faces
        int faces[] = {
            0, 0, 2, 2, 1, 1,
            2, 2, 3, 3, 1, 1,
            4, 0, 5, 1, 6, 2,
            6, 2, 5, 1, 7, 3,
            0, 0, 1, 1, 4, 2,
            4, 2, 1, 1, 5, 3,
            2, 0, 6, 2, 3, 1,
            3, 1, 6, 2, 7, 3,
            0, 0, 4, 1, 2, 2,
            2, 2, 4, 1, 6, 3,
            1, 0, 3, 1, 5, 2,
            5, 2, 3, 1, 7, 3,};
        int index = 1;
        int start = 0;
        int length = faces.length / 6;
        instance.setFaces(index, faces, start, length);
        int[] expected = new int[faces.length];
        assertArrayEquals(instance.getFaces(index, expected, length), faces);
    }

    /**
     * Test setFaces with illegal value of setFaceSmoothingGroups method, of
     * class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaces_4argsIllegalArgument() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 1200 faces
        int faces[] = {0, 0, 2, 2, 1, 1,};
        int[] expecteds = instance.getFaces(null);
        int length = faces.length / 6;
        instance.setFaces(-1, faces, -1, length);
        // faces should not change
        assertArrayEquals(expecteds, instance.getFaces(null));
    }

    /**
     * Test setFaces with index argument out of range of setFaceSmoothingGroups
     * method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaces_4argsIndexOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 1200 faces
        int faces[] = {0, 0, 2, 2, 1, 1,};
        int[] expecteds = instance.getFaces(null);
        int start = 0;
        int length = faces.length / 6;
        instance.setFaces(200, faces, start, length);
        // faces should not change
        assertArrayEquals(expecteds, instance.getFaces(null));
    }

    /**
     * Test setFaces with start argument out of range of setFaceSmoothingGroups
     * method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaces_4argsStartOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 1200 faces
        int faces[] = {
            0, 0, 2, 2, 1, 1,
            2, 2, 3, 3, 1, 1,};
        int[] expecteds = instance.getFaces(null);
        int index = 1;
        int length = faces.length / 6;
        instance.setFaces(index, faces, 1, length);
        // faces should not change
        assertArrayEquals(expecteds, instance.getFaces(null));
    }

    /**
     * Test of setTexCoords method, of class TriangleMesh.
     */
    @Test
    public void testsetTexCoords_4args() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 242 texCoords
        float texCoords[] = {0, 0,
                             0, 1,
                             1, 0,
                             1, 1};
        float[] expecteds = new float[texCoords.length];
        int index = 1;
        int start = 0;
        int length = texCoords.length / 2;
        instance.setTexCoords(index, texCoords, start, length);
        assertArrayEquals(instance.getTexCoords(index, expecteds, length), texCoords, 1e-3f);
    }

    /**
     * Test setTexCoords with illegal value of setTexCoordsmoothingGroups
     * method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testsetTexCoords_4argsIllegalArgument() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 242 texCoords
        float texCoords[] = {0, 0,
                             0, 1,
                             1, 0,
                             1, 1};
        float[] expecteds = instance.getTexCoords(null);
        int length = texCoords.length / 2;
        instance.setTexCoords(-1, texCoords, -1, length);
        // texCoords should not change
        assertArrayEquals(instance.getTexCoords(null), expecteds, 1e-3f);
    }

    /**
     * Test setTexCoords with index argument out of range of
     * setTexCoordsmoothingGroups method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testsetTexCoords_4argsIndexOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 242 texCoords
        float texCoords[] = {0, 0,
                             0, 1,
                             1, 0,
                             1, 1};
        float[] expecteds = instance.getTexCoords(null);
        int start = 0;
        int length = texCoords.length / 2;
        instance.setTexCoords(120, texCoords, start, length);
        // texCoords should not change
        assertArrayEquals(instance.getTexCoords(null), expecteds, 1e-3f);
    }

    /**
     * Test setTexCoords with start argument out of range of
     * setTexCoordsmoothingGroups method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testsetTexCoords_4argsStartOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 242 texCoords
        float texCoords[] = {0, 0,
                             0, 1,
                             1, 0,
                             1, 1};
        float[] expecteds = instance.getTexCoords(null);
        int index = 1;
        int length = texCoords.length / 2;
        instance.setTexCoords(index, texCoords, 1, length);
        // texCoords should not change
        assertArrayEquals(instance.getTexCoords(null), expecteds, 1e-3f);
    }

    /**
     * Test of setPoints method, of class TriangleMesh.
     */
    @Test
    public void testSetPoints_4args() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 121 points
        float points[] = {
            1, 1, 1,
            1, 1, -1,
            1, -1, 1,
            1, -1, -1,
            -1, 1, 1,
            -1, 1, -1,
            -1, -1, 1,
            -1, -1, -1,};
        float[] expecteds = new float[points.length];
        int index = 1;
        int start = 0;
        int length = points.length / 3;
        instance.setPoints(index, points, start, length);
        assertArrayEquals(instance.getPoints(index, expecteds, length), points, 1e-3f);
    }

    /**
     * Test setPoints with illegal value of setPointsmoothingGroups method, of
     * class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPoints_4argsIllegalArgument() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 121 points
        float points[] = {
            1, 1, 1,
            1, 1, -1,
            1, -1, 1,
            1, -1, -1,
            -1, 1, 1,
            -1, 1, -1,
            -1, -1, 1,
            -1, -1, -1,};
        float[] expecteds = instance.getPoints(null);
        int length = points.length / 3;
        instance.setPoints(-1, points, -1, length);
        // points should not change
        assertArrayEquals(instance.getPoints(null), expecteds, 1e-3f);
    }

    /**
     * Test setPoints with index argument out of range of
     * setPointsmoothingGroups method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPoints_4argsIndexOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 121 points
        float points[] = {
            1, 1, 1,
            1, 1, -1,
            1, -1, 1,
            1, -1, -1,
            -1, 1, 1,
            -1, 1, -1,
            -1, -1, 1,
            -1, -1, -1,};
        float[] expecteds = instance.getPoints(null);
        int start = 0;
        int length = points.length / 3;
        instance.setPoints(120, points, start, length);
        // points should not change
        assertArrayEquals(instance.getPoints(null), expecteds, 1e-3f);
    }

    /**
     * Test setPoints with start argument out of range of
     * setPointsmoothingGroups method, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPoints_4argsStartOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY); // 121 points
        float points[] = {
            1, 1, 1,
            1, 1, -1,
            1, -1, 1,
            1, -1, -1,
            -1, 1, 1,
            -1, 1, -1,
            -1, -1, 1,
            -1, -1, -1,};
        float[] expecteds = instance.getPoints(null);
        int index = 1;
        int length = points.length / 3;
        instance.setPoints(index, points, 1, length);
        // points should not change
        assertArrayEquals(instance.getPoints(null), expecteds, 1e-3f);
    }

    TriangleMesh buildTriangleMesh(int subDivX, int subDivY) {
        final int pointSize = 3;
        final int texCoordSize = 2;
        final int faceSize = 6; // 3 point indices and 3 texCoord indices per triangle
        int numDivX = subDivX + 1;
        int numVerts = (subDivY + 1) * numDivX;
        float points[] = new float[numVerts * pointSize];
        float texCoords[] = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivY * 2;
        int faces[] = new int[faceCount * faceSize];

        TriangleMesh triangleMesh = new TriangleMesh(points, texCoords, faces);

        return triangleMesh;
    }
}
