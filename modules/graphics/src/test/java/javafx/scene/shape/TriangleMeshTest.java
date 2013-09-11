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
     * Test of setting faceSmoothingGroups, of class TriangleMesh.
     */
    @Test
    public void testSetFaceSmoothingGroups_intArr() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        instance.getFaceSmoothingGroups().setAll(faceSmoothingGroups);
        assertTrue(instance.getFaceSmoothingGroups().size() == faceSmoothingGroups.length);
        assertArrayEquals(faceSmoothingGroups, instance.getFaceSmoothingGroups().toArray(null));
        assertEquals(instance.getFaces().size() / TriangleMesh.NUM_COMPONENTS_PER_FACE,
                instance.getFaceSmoothingGroups().size());
    }

    /**
     * Test of setting faceSmoothingGroups with arguments, of class TriangleMesh.
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
        instance.getFaceSmoothingGroups().setAll(faceSmoothingGroups);
        instance.getFaceSmoothingGroups().set(index, setterArray, start, length);
        assertArrayEquals(setterArray, instance.getFaceSmoothingGroups().toArray(index, expected, length));
        assertEquals(instance.getFaces().size() / TriangleMesh.NUM_COMPONENTS_PER_FACE,
                instance.getFaceSmoothingGroups().size());
    }

    /**
     * Test of setting faceSmoothingGroups with illegal array length, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaceSmoothingGroups_IllegalLength() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int smLength = divX * divY * 2;
        int[] faceSmoothingGroups = new int[smLength];
        Arrays.fill(faceSmoothingGroups, 1);
        instance.getFaceSmoothingGroups().setAll(faceSmoothingGroups);
        int[] setterArray = new int[smLength + 1]; // IllegalLength
        instance.getFaceSmoothingGroups().setAll(setterArray); // expect IllegalArgumentException
    }

    /**
     * Test of setting faceSmoothingGroups with oversized array, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaceSmoothingGroups_OversizedArr() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int smLength = divX * divY * 2;
        int[] faceSmoothingGroups = new int[smLength];
        Arrays.fill(faceSmoothingGroups, 1);
        instance.getFaceSmoothingGroups().setAll(faceSmoothingGroups);
        int[] setterArray = new int[smLength + 1]; // IllegalLength
        instance.getFaceSmoothingGroups().setAll(setterArray); // expect IllegalArgumentException
    }

    /**
     * Test of setting faces with arguments, of class TriangleMesh.
     */
    @Test (expected = IllegalArgumentException.class)
    public void testSetFaces_4args() {
        int faceSmoothingGroups[] = new int[TriangleMesh.MAX_FACESMOOTHINGGROUPS_LENGTH + 1]; // Oversized
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getFaceSmoothingGroups().setAll(faceSmoothingGroups); // expect IllegalArgumentException
    }

    /**
     * Test of setting faces with illegal array length, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaces_IllegalLength() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int faces[] = new int[instance.getFaces().size() + 1]; // IllegalLength
        int[] expecteds = instance.getFaces().toArray(null);
        instance.getFaces().setAll(faces); // expect IllegalArgumentException
    }

    /**
     * Test of setting faces with oversized array, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFaces_OversizedArr() {
        int faces[] = new int[TriangleMesh.MAX_FACES_LENGTH + 6]; // Oversized
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getFaces().setAll(faces); // expect IllegalArgumentException
    }

    /**
     * Test of setting texCoords with arguments, of class TriangleMesh.
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
        int index = 2;
        int start = 0;
        int length = texCoords.length;
        instance.getTexCoords().set(index, texCoords, start, length);
        assertArrayEquals(instance.getTexCoords().toArray(index, expecteds, length), texCoords, 1e-3f);
    }

    /**
     * Test of setting faces with illegal array length, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testsetTexCoords_IllegalLength() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        float texCoords[] = new float[instance.getTexCoords().size() + 1]; // IllegalLength
        instance.getTexCoords().setAll(texCoords); // expect IllegalArgumentException
    }

    /**
     * Test of setting texCoords with oversized array, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testsetTexCoords_OversizedArr() {
        int texCoords[] = new int[TriangleMesh.MAX_TEXCOORDS_LENGTH + 2]; // Oversized
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getFaces().setAll(texCoords); // expect IllegalArgumentException
    }

    /**
     * Test of setting points with arguments, of class TriangleMesh.
     */
    @Test
    public void testSetPoints_4args() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
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
        int index = 3;
        int start = 0;
        int length = points.length;
        instance.getPoints().set(index, points, start, length);
        assertArrayEquals(instance.getPoints().toArray(index, expecteds, length), points, 1e-3f);
    }

    /**
     * Test of setting points with illegal array length, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPoints_IllegalLength() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        float points[] = new float[instance.getPoints().size() - 2]; // IllegalLength
        float[] expecteds = instance.getPoints().toArray(null);
        instance.getPoints().setAll(points); // expect IllegalArgumentException
    }

    /**
     * Test of setting points with oversized array, of class TriangleMesh.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPoints_OversizedArr() {
        float points[] = new float[TriangleMesh.MAX_POINTS_LENGTH + 3]; // Oversized
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().setAll(points); // expect IllegalArgumentException
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

        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().setAll(points);
        triangleMesh.getTexCoords().setAll(texCoords);
        triangleMesh.getFaces().setAll(faces);

        return triangleMesh;
    }
}
